import oscP5.OscMessage;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

public abstract class Visualizer {
	Visualizer() {
	}

	public void draw(PApplet parent, Positions p, PVector wsize) {
		if (p.positions.isEmpty()) {
			parent.fill(50, 255, 255);
			parent.textAlign(PConstants.CENTER,PConstants.CENTER);
			parent.textSize(32);
			parent.stroke(255);
			parent.text("Waiting for users...", wsize.x/2,wsize.y/2);
		}
	}

	abstract public void update(PApplet parent, Positions p);
	public void start() {;}
	public void stop() {;}

	public void stats() { }

	public void drawBorders(PApplet parent, boolean octagon, PVector wsize) {
		if (octagon) {
			parent.beginShape();
			float gapAngle=(float)(10f*Math.PI /180);
			for (float angle=gapAngle/2;angle<2*Math.PI;angle+=(2*Math.PI-gapAngle)/8)
				parent.vertex((float)((Math.sin(angle+Math.PI)+1)*wsize.x/2),(float)((Math.cos(angle+Math.PI)+1)*wsize.y/2));
			parent.endShape(PConstants.OPEN);
		} else {
			parent.line(0, 0, wsize.x-1, 0);
			parent.line(0, 0, 0, wsize.y-1);
			parent.line(wsize.x-1, 0, wsize.x-1, wsize.y-1);
			parent.line(0, wsize.y-1, wsize.x-1, wsize.y-1);
		}
	}

	public void handleMessage(OscMessage theOscMessage) {
		PApplet.println("Unhanled OSC Message: "+theOscMessage.toString());
	}

}
