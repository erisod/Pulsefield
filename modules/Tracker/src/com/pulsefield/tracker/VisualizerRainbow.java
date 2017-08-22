package com.pulsefield.tracker;

import java.awt.Color;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

public class VisualizerRainbow extends VisualizerParticleSystem {
	private float rainbowPositionDegrees = 0.0f;
	private float rainbowAdvance = 0.0f;
	float rainbowAdvanceIncrement = 0.1f; // rotation of the colors positions
											// per update.

	VisualizerRainbow(PApplet parent) {
		super(parent);
	}

	@Override
	void setUniverseDefaults() {
		universe.maxParticles = 20000;
		universe.particleScale = 0.15f;
		universe.personForce = 0.005f;
		universe.particleRotation = 0.25f;
		universe.forceRotation = 3.0f;
		universe.particleMaxLife = 175;
	}
	
	@Override
	public void start() {
		super.start();


		// Rainbow uses legs for attraction scaling.
		Laser.getInstance().setFlag("legs", 0.0f);
	}

	// Create a particle specified by a distance and angle from a center point.
	private void spewParticle(ParticleSystem universe, int color, float rotation, float angle, float velocity,
			float distanceFromCenter, PVector center) {

		PVector origin = center.copy().add(new PVector(0, distanceFromCenter).rotate(rotation));
		Particle p = new ImageParticle(origin, universe, textures.get("oval.png"));
		p.color = color;

		PVector velocityV = center.copy().sub(origin).normalize().setMag(velocity).rotate(angle);
		p.velocity = velocityV;

		universe.addParticle(p);
	}

	@Override
	public void update(PApplet parent, People p) {
		int particlesPerUpdate = 360;
		rainbowAdvance = (rainbowAdvance + rainbowAdvanceIncrement) % 360;

		// Create some particles unless there are too many already.
		if (universe.particlesRemaining() >= 360) {
			for (int i = 0; i < particlesPerUpdate; i++) {
				rainbowPositionDegrees = (rainbowPositionDegrees + 1) % 360;
				int color = Color.HSBtoRGB((rainbowPositionDegrees + rainbowAdvance) / 360.0f, 1.0f, 1.0f);

				spewParticle(universe, color, (rainbowPositionDegrees / 360.0f) * 2 * (float) Math.PI, 0.0f, 0.0f,
						Tracker.getFloorDimensionMin() / 4, Tracker.getFloorCenter());
			}
		}

		applyPeopleGravityLegScaled(p);
		universe.update();
	}

	@Override
	public void draw(Tracker t, PGraphics g, People p) {
		super.draw(t, g, p);
		drawPeople(g, p);
	}
}
