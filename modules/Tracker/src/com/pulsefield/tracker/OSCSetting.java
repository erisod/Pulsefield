package com.pulsefield.tracker;

import java.util.logging.Logger;

import oscP5.OscMessage;

// Map the variety of TouchOSC controls into classes to describe the values they carry.
enum OSCType {
	Button, // PushButton, ToggleButton (LED can be a button).
	Label, // Text display.
	Value, // One dimension value aka Slider Fader, Rotary, or LED (can display a range by brightness).
	XYPad, // Two values.
	MultiButton, // MultiPush, MultiToggle.
	MultiXY, // Multiple xy values (limit uncertain).
	MultiFader // Set of values (up to 64)
}

abstract class OSCSetting {
	protected final static Logger logger = Logger.getLogger(OSCSetting.class.getName());
	String name;
	String path;
	OSCType type;
	// TODO: List of callbacks on change ?

	OSCHub hub = OSCHub.getInstance();

	public static OSCSetting makeOSCSetting(String oscPath, OSCType oscType) {
		OSCSetting newOSCSetting = null;

		switch (oscType) {
		case Button:
			newOSCSetting = new OSCSettingButton();
			break;
		case Label:
			newOSCSetting = new OSCSettingLabel();
			break;
		case MultiButton:
			newOSCSetting = new OSCSettingMultiButton();
			break;
		case MultiFader:
			break;
		case MultiXY:
			break;
		case Value:
			newOSCSetting = new OSCSettingValue();
			break;
		case XYPad:
			newOSCSetting = new OSCSettingXYPad();
			break;
		default:
			break;
		}

		newOSCSetting.path = oscPath;
		newOSCSetting.type = oscType;
		return newOSCSetting;
	}

	public void handle(OscMessage msg) {
	}

	// Push data to TouchOSC (for initialization typically).
	public void push() {
	}
}

enum OSCState {
	On, Off
}

class OSCSettingButton extends OSCSetting {
	OSCState state = OSCState.Off;

	public OSCState getState() {
		return state;
	}

	public void handle(OscMessage msg) {
		logger.warning("BUTTON handling : " + msg + " VALUE: " + msg.get(0).floatValue());
		setState(msg.get(0).floatValue() == 0.0f ? OSCState.Off : OSCState.On);
	}

	public void setState(OSCState o) {
		// Do nothing if there isn't a change.
		if (state == o) {
			return;
		}

		state = o;

		push();
	}

	@Override
	public void push() {
		OscMessage set = new OscMessage(path);

		switch (state) {
		case On:
			set.add(1.0f);
			break;
		case Off:
			set.add(0.0f);
			break;
		}

		OSCHub.theOSCHub.sendMessage(set);
	}
}

// Multibutton is a max of 16x16 grid of push or stateful buttons.
class OSCSettingMultiButton extends OSCSetting {
	int MaxGridDimensionSize = 16;
	OSCState[][] stateGrid = new OSCState[MaxGridDimensionSize][MaxGridDimensionSize];

	public OSCState getStateAt(int x, int y) {
		// Note, bound check unnecessary; java will throw an ArrayIndexOutOfBoundsException.
		return stateGrid[x][y];
	}

	@Override
	public void handle(OscMessage msg) {
		// Message looks like : /test/multipush/9/6 and payload 1.0
		String components[] = msg.addrPattern().split("/");

		int x = Integer.parseInt(components[-2]);
		int y = Integer.parseInt(components[-1]);

		if (x < 0 || x > MaxGridDimensionSize - 1 || y < 0 || y > MaxGridDimensionSize - 1) {
			logger.warning("Received out of range multibutton address message : " + msg.addrPattern() + " [ignored]");
			return;
		}
		
		setStateAt(x, y, msg.get(0).floatValue() == 0.0f ? OSCState.Off : OSCState.On);
	}

	public void setStateAt(int x, int y, OSCState o) {
		// Do nothing if there isn't a change.
		if (stateGrid[x][y] == o) {
			return;
		}

		stateGrid[x][y] = o;

		pushAt(x, y);
	}

	public void pushAt(int x, int y) {
		OscMessage set = new OscMessage(path + "/" + x + "/" + y);

		switch (stateGrid[x][y]) {
		case On:
			set.add(true);
			break;
		case Off:
			set.add(false);
			break;
		}

		OSCHub.theOSCHub.sendMessage(set);
	}

	@Override
	public void push() {
		for (int x = 0; x < MaxGridDimensionSize; x++) {
			for (int y = 0; y < MaxGridDimensionSize; y++) {
				pushAt(x, y);
			}
		}
	}
}

class OSCSettingValue extends OSCSetting {
	// TODO Scale modes?
	float value;

	public float getValue() {
		return value;
	}

	public int getValueInt() {
		return (int) value;
	}

	public void setValue(int v) {
		setValue((float) v);
	}

	@Override
	public void handle(OscMessage msg) {
		setValue(msg.get(0).floatValue());
	}

	public void setValue(float v) {
		// Do nothing if there isn't a change.
		if (value == v) {
			return;
		}

		value = v;

		push();
	}

	@Override
	public void push() {
		OscMessage set = new OscMessage(this.path);
		set.add(value);
		OSCHub.theOSCHub.sendMessage(set);
	}

}

class OSCSettingLabel extends OSCSetting {
	String label;

	public String getValue() {
		return label;
	}

	@Override
	public void handle(OscMessage msg) {
		// This will probably never happen as touchOSC doesn't support
		// modifying a label in the iPad interface.
		setValue(msg.get(0).stringValue());
	}

	public void setValue(String l) {
		// Do nothing if there isn't a change.
		if (label == l) {
			return;
		}

		label = l;

		push();
	}

	@Override
	public void push() {
		OscMessage set = new OscMessage(path);
		set.add(label);
		OSCHub.theOSCHub.sendMessage(set);
	}
}

class OSCSettingXYPad extends OSCSetting {
	xy value = new xy();

	public static class xy {
		float x;
		float y;
	}

	public xy getValue() {
		return value;
	}

	@Override
	public void handle(OscMessage msg) {
		xy newxy = new xy();
		newxy.x = msg.get(0).floatValue();
		newxy.y = msg.get(1).floatValue();
		setValue(newxy);
	}

	public void setValue(xy v) {
		// Do nothing if there isn't a change.
		if (value.x == v.x && value.y == v.y) {
			return;
		}

		value.x = v.x;
		value.y = v.y;
		
		push();
	}

	@Override
	public void push() {
		OscMessage set = new OscMessage(this.path);
		set.add("set");
		set.add(value.x);
		set.add(value.y);
		OSCHub.theOSCHub.sendMessage(set);
	}
}