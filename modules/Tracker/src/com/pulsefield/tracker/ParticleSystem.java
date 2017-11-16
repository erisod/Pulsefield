package com.pulsefield.tracker;

import java.util.ArrayList;
import java.util.Iterator;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.opengl.PGL;

// An ArrayList is used to manage the list of Particles


class ParticleSystemSettings {
	protected float particleRandomDriftAccel = 0.02f / 300;
	protected float forceRotation = 0.0f;
	protected float particleRotation = 0.0f;
	protected int particleMaxLife = 500;
	
	// blendMode can be -1 (for custom) or PGraphics.BLEND, .ADD, .SUBTRACT, etc.
	protected int blendMode = -1;
	protected float personForce = 0.005f;
	protected float particleScale = 1.0f;

	protected float fadeDeath = 0.1f;
	protected float startOpacity = 1.0f;

	// Net force up/down/left/right.
	protected float tiltx = 0.0f;
	protected float tilty = 0.0f;

	// maxParticles is the hard upper limit on number of particles.
	protected long maxParticles = 50000;
	
	public float getParticleRandomDriftAccel() {
		return particleRandomDriftAccel;
	}

	public void setParticleRandomDriftAccel(float particleRandomDriftAccel) {
		this.particleRandomDriftAccel = particleRandomDriftAccel;
	}

	public float getForceRotation() {
		return forceRotation;
	}

	public void setForceRotation(float forceRotation) {
		this.forceRotation = forceRotation;
	}

	public float getParticleRotation() {
		return particleRotation;
	}

	public void setParticleRotation(float particleRotation) {
		this.particleRotation = particleRotation;
	}

	public int getParticleMaxLife() {
		return particleMaxLife;
	}

	public void setParticleMaxLife(int particleMaxLife) {
		this.particleMaxLife = particleMaxLife;
	}

	public int getBlendMode() {
		return blendMode;
	}

	public void setBlendMode(int blendMode) {
		this.blendMode = blendMode;
	}

	public float getPersonForce() {
		return personForce;
	}

	public void setPersonForce(float personForce) {
		this.personForce = personForce;
	}

	public float getParticleScale() {
		return particleScale;
	}

	public void setParticleScale(float particleScale) {
		this.particleScale = particleScale;
	}

	public float getFadeDeath() {
		return fadeDeath;
	}

	public void setFadeDeath(float fadeDeath) {
		this.fadeDeath = fadeDeath;
	}

	public float getStartOpacity() {
		return startOpacity;
	}

	public void setStartOpacity(float startOpacity) {
		this.startOpacity = startOpacity;
	}

	public float getTiltx() {
		return tiltx;
	}

	public void setTiltx(float tiltx) {
		this.tiltx = tiltx;
	}

	public float getTilty() {
		return tilty;
	}

	public void setTilty(float tilty) {
		this.tilty = tilty;
	}

	public long getMaxParticles() {
		return maxParticles;
	}

	public void setMaxParticles(long maxParticles) {
		this.maxParticles = maxParticles;
	}
}

class ParticleSystemSettingsOSC extends ParticleSystemSettings {
	OSCSettingValue oscParticlesMax = Tracker.theTracker.oscHub.registerValue("/video/particlefield/maxparticles");
	OSCSettingValue oscParticlesMaxLife = Tracker.theTracker.oscHub.registerValue("/video/particlefield/particlemaxlife");
	OSCSettingValue oscParticlesScale = Tracker.theTracker.oscHub.registerValue("/video/particlefield/particlescale");
	OSCSettingValue oscParticlesRotation = Tracker.theTracker.oscHub.registerValue("/video/particlefield/particlerotation");
	OSCSettingValue oscParticlesPersonForce = Tracker.theTracker.oscHub.registerValue("/video/particlefield/persongravity");
	OSCSettingValue oscParticlesForceRotation = Tracker.theTracker.oscHub.registerValue("/video/particlefield/forcerotation");
	OSCSettingValue oscParticlesDispersion = Tracker.theTracker.oscHub.registerValue("/video/particlefield/particledispersion");
	OSCSettingValue oscParticlesOpacity = Tracker.theTracker.oscHub.registerValue("/video/particlefield/particleopacity");
	OSCSettingXYPad oscParticlesTilt = Tracker.theTracker.oscHub.registerXYPad("/video/particlefield/tilt");
	OSCSettingMultiButton oscModes = Tracker.theTracker.oscHub.registerMultiButton("/video/particlefield/blendmode");

	public float getParticleRandomDriftAccel() {
		return oscParticlesDispersion.getValue();
	}

	public void setParticleRandomDriftAccel(float particleRandomDriftAccel) {
		oscParticlesDispersion.setValue(particleRandomDriftAccel);
	}

	public float getForceRotation() {
		return oscParticlesForceRotation.getValue();
	}

	public void setForceRotation(float forceRotation) {
		oscParticlesForceRotation.setValue(forceRotation);
	}

	public float getParticleRotation() {
		return oscParticlesRotation.getValue();
	}

	public void setParticleRotation(float particleRotation) {
		oscParticlesRotation.setValue(particleRotation);
	}

	public int getParticleMaxLife() {
		return oscParticlesMaxLife.getValueInt();
	}

	public void setParticleMaxLife(int particleMaxLife) {
		oscParticlesMaxLife.setValue(particleMaxLife);
	}

	public int getBlendMode() {
		// TODO
		xxx 
		return blendMode;
	}

	public void setBlendMode(int blendMode) {
		this.blendMode = blendMode;
	}

	public float getPersonForce() {
		return oscParticlesPersonForce.getValue();
	}

	public void setPersonForce(float personForce) {
		oscParticlesPersonForce.setValue(personForce);
	}

	public float getParticleScale() {
		return oscParticlesScale.getValue();
	}

	public void setParticleScale(float particleScale) {
		oscParticlesScale.setValue(particleScale);
	}

	public float getTiltx() {
		return oscParticlesTilt.getValue().x;
	}

	public void setTiltx(float tiltx) {
		OSCSettingXYPad.xy setting = oscParticlesTilt.getValue();
		setting.x = tiltx;

		oscParticlesTilt.setValue(setting);
	}

	public float getTilty() {
		return oscParticlesTilt.getValue().y;
	}

	public void setTilty(float tilty) {
		OSCSettingXYPad.xy setting = oscParticlesTilt.getValue();
		setting.y = tilty;

		oscParticlesTilt.setValue(setting);
	}

	public long getMaxParticles() {
		return oscParticlesMax.getValueInt();
	}

	public void setMaxParticles(long maxParticles) {
		oscParticlesMax.setValue(maxParticles);
	}
}


class ParticleSystem {
	// An arraylist for all the particles.
	ArrayList<Particle> particles;
	ParticleSystemSettings settings;
	
	ParticleSystem() {
		particles = new ArrayList<Particle>(); // Initialize the arraylist.
		settings = new ParticleSystemSettings();
	}

	void attractor(PVector c, float force) {
		// Add a gravitational force that acts on all particles.
		for (int i = particles.size() - 1; i >= 0; i--) {
			particles.get(i).attractor(c, force, PApplet.radians(settings.forceRotation));
		}
	}
	
	void applyPeopleGravity(People p) {
		if (settings.personForce != 0.0f) {
			for (Person pos : p.pmap.values()) {
				attractor(pos.getOriginInMeters(), settings.personForce);
			}
		}
	}

	void push(PVector c, PVector spd) {
		for (Particle p : particles) {
			p.push(c, spd);
		}
	}

	// Update particle system.
	void update() {
		Iterator<Particle> i = particles.iterator();
		while (i.hasNext()) {
			Particle p = i.next();
			p.update();
			if (p.isDead()) {
				i.remove();
			}
		}
	}

	// How many particles are left until the max? This allows clients that want
	// to batch particle creation to know when that's acceptable. Can return a negative
	// value if there is a deficit (e.g. if maxParticles is reduced after creation).
	long particlesRemaining() {
		return settings.maxParticles - particles.size();
	}

	// Called before particles are drawn; perform setup here.
	void drawPrep(PGraphics g) {		
		if (settings.blendMode == -1) {
			customBlend(g);
		} else {
			g.blendMode(settings.blendMode);
		}
	}

	void customBlend(PGraphics g) {
		g.resetShader();
		PGL pgl=g.beginPGL();
		pgl.blendFunc(PGL.SRC_ALPHA, PGL.ONE_MINUS_SRC_ALPHA); 
		pgl.blendEquation(PGL.FUNC_ADD);
		g.endPGL();
	}

	void draw(PGraphics g) {
		drawPrep(g);
		for (Particle p : particles) {
			p.draw(g, settings.particleScale);
		}
	}

	void addParticle(Particle p) {
		// Don't allow creation of more than the allowed number of particles.
		if (particlesRemaining() <= 0) {
			return;
		}

		particles.add(p);
	}

	// A method to test if the particle system still has particles
	boolean dead() {
		return particles.isEmpty();
	}
}
