package com.pulsefield.tracker;
import java.util.HashMap;
import java.util.Iterator;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

class GridData {
	int id;
	int prevgrid, nextgrid;
	int exploding;
	GridData() { id=-1; prevgrid=-1; nextgrid=-1; exploding=-1; }
	void set(int id, int prevgrid) {
		assert(this.id==-1 || this.exploding>0);
		this.id=id;
		this.prevgrid=prevgrid;
		this.nextgrid=-1;
		this.exploding=-1;
	}
}

class Cursor {
	int grid;
	boolean fwd;
	int pitch;
	Cursor(int grid, boolean fwd, int pitch) {
		this.grid=grid;
		this.fwd=fwd;
		this.pitch=pitch;
	}
}

public class VisualizerTron extends Visualizer {
	final int gridWidth = 20;
	final int gridHeight = 20;
	float lastbeat=0;
	float notedur=0.25f;
	GridData grid[];
	HashMap<Integer,Integer> currentgrid;
	HashMap<Integer,Cursor> playgrid;  // Current playing grid;  -ve is backing up
	Scale scale;
	Synth synth;
	TrackSet trackSet;
	
	VisualizerTron(PApplet parent, Scale scale, Synth synth) {
		super();
		grid=new GridData[gridWidth*gridHeight];
		for (int i=0;i<grid.length;i++)
			grid[i]=new GridData();
		currentgrid=new HashMap<Integer,Integer>();
		playgrid=new HashMap<Integer,Cursor>();
		this.scale=scale;
		this.synth=synth;
	}

	@Override
	public void start() {
		super.start();
		Laser.getInstance().setFlag("body",0.0f);
		Laser.getInstance().setFlag("legs",0.0f);
		trackSet=Ableton.getInstance().setTrackSet("Tron");
	}

	@Override
	public void stop() {
		super.stop();
	}

	int postogrid(PVector p) {
		int gpos=postogridx(p.x)*gridHeight+postogridy(p.y);
		return gpos;
	}

	int postogridx(float x) {
		int gx=(int)((x+1)/2*gridWidth);
		if (gx<0) gx=0;
		if (gx>=gridWidth) gx=gridWidth-1;
		return gx;
	}
	int postogridy(float y) {
		int gy=(int)((y+1)/2*gridHeight);
		if (gy<0) gy=0;
		if (gy>=gridHeight) gy=gridHeight-1;
		return gy;

	}

	String gdisp(int grid) {
		int gx=grid/gridHeight;
		int gy=grid-gx*gridHeight;
		return "("+gx+","+gy+")";
	}

	void explode(int id) {
		for (int i=0;i<grid.length;i++)
			if ((grid[i].id==id || id==-1)  && grid[i].exploding==-1) {
				grid[i].exploding=1;
			}
	}

	void clear(int id) {
		for (int i=0;i<grid.length;i++)
			if (grid[i].id==id || id==-1) {
				grid[i].id=-1;
			}
	}


	int findstep(int oldgpos,int gpos) {
		int dx=(int)(gpos/gridHeight)-(int)(oldgpos/gridHeight);
		int dy=gpos-oldgpos-dx*gridHeight;
		assert(dx!=0 || dy!=0);
		if (Math.abs(dx)>Math.abs(dy))
			return oldgpos+((dx>0)?gridHeight:-gridHeight);
		else
			return oldgpos+((dy>0)?1:-1);
	}

	public void update(PApplet parent, People positions) {
		float beat=MasterClock.getBeat();
		if ((int)(beat/notedur) != (int)(lastbeat/notedur)) {
			for (int id: playgrid.keySet()) {
				Cursor pg=playgrid.get(id);
				int p=pg.grid;
				boolean fwd=pg.fwd;

				// Check for end bounces
				if (fwd && grid[p].nextgrid==-1)
					fwd=false;
				if (!fwd && grid[p].prevgrid==-1)
					fwd=true;
				int n=fwd?grid[p].nextgrid:grid[p].prevgrid;
				if (n!=-1) {
					// Make a delta such that the return path undoes all the pitch shifts
					int delta;
					if (grid[p].nextgrid!=-1 && grid[p].prevgrid!=-1) {
						delta=(grid[p].nextgrid-grid[p].prevgrid);
						if (!fwd) delta=-delta;
					} else
						delta=fwd?1:-1;  // Opposite signs at ends
					
					//logger.fine("delta="+delta+"("+grid[p].nextgrid+"-"+grid[p].prevgrid+") @"+p);
					if (delta!=2 && delta!=-2 && delta!=gridHeight*2 && delta!=-gridHeight*2) {
						// Corner
						pg.pitch+=(delta>0)?1:-1;
						//logger.fine("ID "+id+": "+pg.pitch);
						if (positions.get(id)!=null)
							synth.play(id, pg.pitch, 127, (int)(notedur*480*2), positions.get(id).channel);
					}
					pg.fwd=fwd;
					pg.grid=n;
				}	
			}
			lastbeat=beat;
		}


		for (int id: positions.pmap.keySet()) {
			Person ps=positions.get(id);
			PVector newpos=ps.getNormalizedPosition();
			int gpos=postogrid(newpos);
			if (currentgrid.containsKey(id)  && currentgrid.get(id)!=-1) {
				int oldgpos=currentgrid.get(id);
				if (grid[oldgpos].id != id) {
					logger.warning("grid["+oldgpos+"]="+grid[oldgpos].id+", but expected id "+id);
				}
				int priorgpos=grid[oldgpos].prevgrid;
				if (gpos==priorgpos && grid[oldgpos].id==id) {
					// Retracing prior step, undo last step
					grid[oldgpos].id=-1;
					grid[priorgpos].nextgrid=-1;
					currentgrid.put(id, gpos);
				} else if (priorgpos!=-1 && gpos==grid[priorgpos].prevgrid) {
					// Back up 2 steps, probably a diagonal
					grid[oldgpos].id=-1;
					grid[priorgpos].nextgrid=-1;
					grid[priorgpos].id=-1;
					grid[grid[priorgpos].prevgrid].nextgrid=-1;
					currentgrid.put(id, gpos);
				} else
					while (oldgpos!=gpos) {
						// Move 1 grid step at a time, always manhattan type move (no diagonals)
						int stepgpos=findstep(oldgpos,gpos);
						//logger.fine("findstep("+oldgpos+","+gpos+") -> "+stepgpos);
						GridData g=grid[stepgpos];
						if (g.id==-1 || g.exploding>0) {
							//logger.fine("Set grid "+gdisp(stepgpos)+" to "+id+" prev="+gdisp(oldgpos));
							grid[stepgpos].set(id,oldgpos);
							grid[oldgpos].nextgrid=stepgpos;
							currentgrid.put(id, stepgpos);
						} else {
							// Collision, clear out this ID
							logger.fine("Collision of ID "+id+" with "+g.id+" at pos="+gdisp(stepgpos)+", prev="+gdisp(g.prevgrid));						
							explode(id);
							currentgrid.put(id,-1);
							break;
						}
						oldgpos=stepgpos;
					}
			} else if (grid[gpos].id==-1 || grid[gpos].exploding>0) {
				// Not on map, just put us there
				grid[gpos].set(id,-1);
				currentgrid.put(id, gpos);
				playgrid.put(id,new Cursor(gpos,true,scale.map2note(id%scale.length(), 0, scale.length()-1, 0, 1)));
			} else {
				// On top of someone
				//logger.fine("ID "+id+" is on top of "+grid[gpos].id);
				currentgrid.put(id,-1);
			}
		}
		for (Iterator<Integer> iter = currentgrid.keySet().iterator();iter.hasNext();) {
			int id=iter.next().intValue();
			if (!positions.pmap.containsKey(id)) {
				logger.fine("Removing ID "+id);
				playgrid.remove(id);
				clear(id);
				iter.remove();
			}
		}
	}

	public void draw(Tracker t, PGraphics g, People p) {
		super.draw(t,g,p);

		for (int i=0;i<gridWidth;i++)
			for (int j=0;j<gridHeight;j++) {
				GridData gd=grid[i*gridHeight+j];
				int gid=gd.id;
				if (gid!=-1) {
					if (p.get(gid)== null) {
						// Person deleted
						logger.fine("draw: missing person in grid: id= "+gid);
						grid[i*gridHeight+j].id=-1;
						continue;
					}
					PVector c1=Tracker.normalizedToFloor(new PVector(i*2.0f/gridWidth-1,(j+1)*2.0f/gridHeight-1));
					PVector c2=Tracker.normalizedToFloor(new PVector((i+1)*2.0f/gridWidth-1,j*2.0f/gridHeight-1));	

					if (gd.exploding>0) {
						final int explosionFrames = 300;
						assert(p.get(gid)!=null);
						g.fill(p.get(gid).getcolor());
						float w = (c2.x-c1.x)*(explosionFrames-gd.exploding)/explosionFrames;
						float h = (c2.y-c1.y)*(explosionFrames-gd.exploding)/explosionFrames;
						PVector sz=Tracker.getFloorSize();
						float disp=sz.x*gd.exploding/explosionFrames;
						g.rect(c1.x,c1.y+disp,w,h);
						g.rect(c1.x,c1.y-disp,w,h);
						g.rect(c1.x+disp,c1.y,w,h);
						g.rect(c1.x-disp,c1.y,w,h);
						if (gd.exploding<explosionFrames)
							grid[i*gridHeight+j].exploding+=10;
						else {
							grid[i*gridHeight+j].exploding=-1;
							grid[i*gridHeight+j].id=-1;
						}
					} else {
						float inset=0.05f;
						g.stroke(0);
						if (currentgrid.containsKey(gid) && currentgrid.get(gid)==i*gridHeight+j) {
							g.fill(g.color(255,255,255));
							g.rect(c1.x,c1.y,c2.x-c1.x,c2.y-c1.y);
							inset=0;
						}
						if (playgrid.get(gd.id).grid == i*gridHeight+j)
							g.fill(p.get(gd.id).getcolor(),100);
						else
							g.fill(p.get(gd.id).getcolor());
						g.rect(c1.x+inset*(c2.x-c1.x),c1.y+inset*(c2.y-c1.y),c2.x-c1.x-2*inset*(c2.x-c1.x),c2.y-c1.y-2*inset*(c2.y-c1.y));
					}
				}
			}	
	}
	
	public void drawLaser(PApplet parent, People p) {
		super.drawLaser(parent,p);
		Laser laser=Laser.getInstance();
		laser.bgBegin();
		for (int i=0;i<gridWidth;i++)
			for (int j=0;j<gridHeight;j++) {
				GridData g=grid[i*gridHeight+j];
				int gid=g.id;
				if (gid!=-1) {
					if (p.get(gid)== null) {
						// Person deleted
						logger.fine("draw: missing person in grid: id= "+gid);
						grid[i*gridHeight+j].id=-1;
						continue;
					}
					PVector c1=Tracker.normalizedToFloor(new PVector(i*2.0f/gridWidth-1,(j+1)*2.0f/gridHeight-1));
					PVector c2=Tracker.normalizedToFloor(new PVector((i+1)*2.0f/gridWidth-1,j*2.0f/gridHeight-1));	
					laser.shapeBegin("Grid"+(i*gridHeight+j));
					
					if (g.exploding>0) {
						final int explosionFrames = 400;
						assert(p.get(gid)!=null);
						float w = (c2.x-c1.x)*(explosionFrames-g.exploding)/explosionFrames;
						float h = (c2.y-c1.y)*(explosionFrames-g.exploding)/explosionFrames;
						float disp=(Tracker.maxx-Tracker.minx)*g.exploding/explosionFrames;
//						logger.fine("exploding="+g.exploding+", w="+w+", h="+h+", disp="+disp);
						laser.rect(c1.x,c1.y+disp,w,h);
						laser.rect(c1.x,c1.y-disp,w,h);
						laser.rect(c1.x+disp,c1.y,w,h);
						laser.rect(c1.x-disp,c1.y,w,h);
					} else {
						float inset=0.1f;
//						if (currentgrid.containsKey(gid) /*&& currentgrid.get(gid)==i*gridHeight+j*/) {
//							logger.fine("c1="+c1+", c2="+c2);
							laser.rect(c1.x,c1.y,c2.x-c1.x,c2.y-c1.y);
//						}
						if (playgrid.get(g.id).grid == i*gridHeight+j) {
							laser.rect(c1.x+inset*(c2.x-c1.x),c1.y+inset*(c2.y-c1.y),c2.x-c1.x-2*inset*(c2.x-c1.x),c2.y-c1.y-2*inset*(c2.y-c1.y));
						}

					}
					laser.shapeEnd("Grid"+(i*gridHeight+j));
				}
			}	
		laser.bgEnd();
	}
}

