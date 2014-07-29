import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PShape;
import processing.core.PVector;

// Visualizer that just displays a dot for each person

public class VisualizerIcon extends Visualizer {
	String icons[]={"P1.svg","P2.svg"};
	PShape iconShapes[];
	
	VisualizerIcon(PApplet parent) {
		super();
		iconShapes=new PShape[icons.length];
		for (int i=0;i<iconShapes.length;i++) {
			iconShapes[i]=parent.loadShape(icons[i]);
			assert(iconShapes[i]!=null);
			PApplet.println("Loaded "+icons[i]+" with "+iconShapes[i].getChildCount()+" children, size "+iconShapes[i].width+"x"+iconShapes[i].height);
		}
	}
	
	public void start() {
		super.start();
		Laser.getInstance().setFlag("body",0.0f);
		Laser.getInstance().setFlag("legs",0.0f);
	}
	public void stop() {
		super.stop();
		Laser.getInstance().setFlag("body",1.0f);
		Laser.getInstance().setFlag("legs",1.0f);
	}

	
	public void update(PApplet parent, Positions p) {
		;
	}

	public void draw(PApplet parent, Positions p, PVector wsize) {
		super.draw(parent, p, wsize);
		parent.background(127,127,127);
		parent.shapeMode(PApplet.CENTER);
		final float sz=20;  // Size to make the icon's largest dimension, in pixels
		for (Position ps: p.positions.values()) {  
			int c=ps.getcolor(parent);
			parent.fill(c,255);
			parent.stroke(c,255);
			PShape icon=iconShapes[ps.id%iconShapes.length];
			//icon.translate(-icon.width/2, -icon.height/2);
			//PApplet.println("Display shape "+icon+" at "+ps.origin);
			float scale=Math.min(sz/icon.width,sz/icon.height);
			parent.shape(icon,(ps.origin.x+1)*wsize.x/2, (ps.origin.y+1)*wsize.y/2,icon.width*scale,icon.height*scale);
			//icon.resetMatrix();
		}
	}
	
	public void drawLaser(PApplet parent, Positions p) {
		Laser laser=Laser.getInstance();
		for (Position ps: p.positions.values()) {  
			String icon=icons[ps.id%icons.length];
			laser.cellBegin(ps.id);
			laser.svgfile(icon,0.0f,0.0f,0.7f,0.0f);
			laser.cellEnd(ps.id);
		}
	}
}

