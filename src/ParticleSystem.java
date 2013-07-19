import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

class ParticleSystem {

// An ArrayList is used to manage the list of Particles

	PApplet parent;
	ArrayList<Particle> particles;    // An arraylist for all the particles
	PVector origin;                   // An origin point for where particles are birthed
	int col;
	PImage img;
	boolean enabled;
	PVector avgspeed; // Average speed in pixels/second
	float lastmovetime;   // Last moved time in seconds
	float averagingTime;   // Averaging time in seconds


	ParticleSystem(PApplet parent, int num, PVector v, int col, PImage img) {
		super(v);
		this.parent=parent;
		particles = new ArrayList<Particle>();   // Initialize the arraylist
		origin = v.get();                        // Store the origin point
		this.col=col;
		this.img=img;
		this.enabled=false;
		this.lastmovetime=0;
		averagingTime=1;  
		avgspeed=new PVector(0.0f, 0.0f);
		for (int i = 0; i < num; i++) {
			addParticle();
		}
	}
	

	void attractor(PVector c, float force) {
		// Add a gravitional force that acts on all particles
		for (int i = particles.size()-1; i >= 0; i--) {
			particles.get(i).attractor(c, force);
		}
	}

	void push(PVector c, PVector spd) {
		// push particles we're moving through
		for (int i = particles.size()-1; i >= 0; i--) {
			particles.get(i).push(c, spd);
		}
	}


	void move(PVector newpos, float elapsed) {
		println("move("+newpos+","+elapsed+"), lastmovetime="+lastmovetime);
		if (lastmovetime!=0.0 && elapsed>lastmovetime) {
			PVector moved=newpos.get();
			moved.sub(origin);
			moved.mult(1.0/(elapsed-lastmovetime)/frameRate);  // Convert to pixels/tick
			// Running average using exponential decay
			float k=averagingTime/frameRate;
			avgspeed.mult(1-k);
			moved.mult(k);
			avgspeed.add(moved);
			println("Speed="+avgspeed);
		}
		origin=newpos;
		lastmovetime=elapsed;
	}

	void run() {
		// Cycle through the ArrayList backwards, because we are deleting while iterating
		for (int i = particles.size()-1; i >= 0; i--) {
			Particle p = particles.get(i);
			p.run();
			if (p.isDead()) {
				particles.remove(i);
			}
		}
	}

	void enable(boolean en) {
		if (en!=enabled) {
			enabled=en;
			lastmovetime=0;
		}
	}

	void addParticle() {
		Particle p;
		int id;
		if (particles.size()==0)
			id=0;
		else
			id=particles.get(particles.size()-1).id+1;
		//println("Add "+id);
		p = new Particle(parent, origin, new PVector(0f,0f), col, img, id);
		particles.add(p);
	}

	void addParticle(Particle p) {
		particles.add(p);
	}

	// A method to test if the particle system still has particles
	boolean dead() {
		return particles.isEmpty();
	}
}

