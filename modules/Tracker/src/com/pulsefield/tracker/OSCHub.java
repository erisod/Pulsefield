package com.pulsefield.tracker;

import java.util.HashMap;
import java.util.logging.Logger;

import netP5.NetAddress;
import oscP5.OscMessage;
import oscP5.OscP5;
import processing.core.PApplet;

// OSCHub is intended to support shared settings across several visualizers and is responsible for:
//   (1) Handling incoming OSC messages from the control (e.g. ipad setting framerate=40).
//   (2) Distribution of the message to modules that have registered for a push notification on change.
//   (3) Storage of setting state (store framerate=40)
//   (4) Support queries of the stored value (e.g. get framerate setting)
//   (5) Support setting of stored value from code (e.g. set framerate=35), with push to ipad for displayed value.
//   (6) General registration of value settings to identify bugs (e.g. 'framrate' doesn't exist; maxx is not an led).
public class OSCHub {
	protected final static Logger logger = Logger.getLogger(OSCHub.class.getName());

	TouchOSC touchOSC;
	static OSCHub theOSCHub;
	HashMap<String, OSCSetting> hubmap = new HashMap<String, OSCSetting>();

	OSCHub(TouchOSC to) {
		touchOSC = to;
		theOSCHub = this;
	}

	class OSCTypeMismatchException extends RuntimeException {
	}

	class OSCSettingDoesNotExistException extends RuntimeException {

	}

	public OSCSettingValue registerValue(String oscPath) {
		return (OSCSettingValue) register(oscPath, OSCType.Value);
	}

	public OSCSettingButton registerButton(String oscPath) {
		return (OSCSettingButton) register(oscPath, OSCType.Button);
	}

	public OSCSettingMultiButton registerMultiButton(String oscPath) {
		return (OSCSettingMultiButton) register(oscPath, OSCType.MultiButton);
	}

	public OSCSettingLabel registerLabel(String oscPath) {
		return (OSCSettingLabel) register(oscPath, OSCType.Label);
	}

	public OSCSettingXYPad registerXYPad(String oscPath) {
		return (OSCSettingXYPad) register(oscPath, OSCType.XYPad);
	}

	public OSCSetting register(String oscPath, OSCType oscType) {
		OSCSetting myOSCSetting;
		
		// Does this not already exist?
		if (!hubmap.containsKey(oscPath)) {
			// Create it.
			myOSCSetting = OSCSetting.makeOSCSetting(oscPath, oscType);
			hubmap.put(oscPath, myOSCSetting);
		} else {
			// Check that the type is correct.
			myOSCSetting = hubmap.get(oscPath);

			if (myOSCSetting.type != oscType) {
				logger.severe("register called for existing setting (" + oscPath + ") but a different type.  Existing type: " + myOSCSetting.type + " vs new "
						+ oscType);
				throw new OSCTypeMismatchException();
			}
		}
		
		return myOSCSetting;

		// TODO: Register caller's callback (at the setting level or maybe just a global notification?)
	}

	public OSCSetting get(String oscPath) {
		if (!hubmap.containsKey(oscPath)) {
			throw new OSCSettingDoesNotExistException();
		} else {
			return hubmap.get(oscPath);
		}
	}

	public void sendMessage(OscMessage msg) {
		touchOSC.sendMessage(msg);
	}

	public static OSCHub getInstance() {
		return theOSCHub;
	}

	public void handleMessage(OscMessage msg) {
		if (hubmap.containsKey(msg.addrPattern())) {
			OSCSetting myOSCSetting = hubmap.get(msg.addrPattern());
			myOSCSetting.handle(msg);
			logger.warning("hubmap handling " + msg + " " + msg.get(0).floatValue()); // TODO: Move log level to debug
																						// or
																					// info.
		} else {
			// TODO: Move to warn logging.
			logger.warning("Warning: OSC Message received with unregistered path : " + msg.addrPattern() + " and payload " + msg.get(0).floatValue());
		}
	}
}
