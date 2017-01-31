#include "ofApp.h"

//--------------------------------------------------------------
void ofApp::setup(){
    // Syphon setup

	syphon[0].setName("Main");
	syphon[1].setName("Screen");
    syphon[2].setName("Velocity");
    syphon[3].setName("Temperature");
    syphon[4].setName("Pressure");
    
    // OSC Setup
    int PORT=7713;
    std::cout << "listening for osc messages on port " << PORT << "\n";
    receiver.setup( PORT );
    
    ofSetFrameRate(30); // if vertical sync is off, we can go a bit fast... this caps the framerate
    ofEnableAlphaBlending();
    ofSetCircleResolution(100);
    width = 500;
    height = 500;
    
    // Initial Allocation
    //
    fluid.allocate(width, height, 0.5, true);
    
    // Seting the gravity set up & injecting the background image
    //
    fluid.dissipation = 0.99;
    fluid.velocityDissipation = 0.99;
    
    fluid.setGravity(ofVec2f(0.0,0.0));
    //    fluid.setGravity(ofVec2f(0.0,0.0098));
    
    //  Set obstacle
    //
    fluid.begin();
    ofSetColor(0,0);
    ofSetColor(255);
    ofDrawCircle(width*0.5, height*0.35, 40);
    fluid.end();
    fluid.setUseObstacles(false);
    
    // Adding constant forces
    //
    flameEnable=true;
    flamePosition=ofPoint(0.0,0.7);
    flameVelocity=ofPoint(0.0,-2.0);
    flameColor= ofFloatColor(0.5,0.1,0.0,1.0);
    flameRadius=10.0f;
    flameTemperature=10.0f;
    flameDensity=1.0f;
    updateFlame();
    
    ofSetWindowShape(width, height);
}

void ofApp::updateFlame() {
    if (flameEnable) {
        pendingForces.push_back(punctualForce(ofPoint(width*(flamePosition.x+1)/2,height*(flamePosition.y+1)/2), flameVelocity, flameColor, flameRadius, flameTemperature, flameDensity));
        cout << "Flame enabled, pos=[" << flamePosition << "], vel=[" << flameVelocity << "], color=[" << flameColor << "], radius=" << flameRadius << ", temp=" << flameTemperature << ", den=" << flameDensity << endl;
    } else
        cout << "Flame disabled" << endl;
    
}

//--------------------------------------------------------------
void ofApp::update(){
    // Adding temporal Force
    //
    ofPoint m = ofPoint(mouseX,mouseY);
    ofPoint d = (m - oldM)*10.0;
    oldM = m;
    ofPoint c = ofPoint(640*0.5, 480*0.5) - m;
    c.normalize();
    ofFloatColor col(c.x*sin(ofGetElapsedTimef()),c.y*sin(ofGetElapsedTimef()),0.5f,0.1f);
    cout << "mouse d=" << d << endl;
    fluid.addTemporalForce(m, d, col,10.0f);
    
    //  Update
    //
    fluid.update();
    
    ofSetWindowTitle(ofToString(ofGetFrameRate()));
    
    // Check for OSC messages
    // check for waiting messages
    while( receiver.hasWaitingMessages() ) {
        // get the next message
        ofxOscMessage m;
        receiver.getNextMessage( m );
        if (  m.getAddress()=="/navier/force" ) {
            // both the arguments are int32's
            int cellX = m.getArgAsInt32( 0 );
            int cellY = m.getArgAsInt32( 1 );
            float dX = m.getArgAsFloat(2);
            float dY = m.getArgAsFloat(3);
            float red = m.getArgAsFloat(4);
            float green = m.getArgAsFloat(5);
            float blue = m.getArgAsFloat(6);
            float alpha = m.getArgAsFloat(7);
            float radius = m.getArgAsFloat(8);
            float temp = m.getArgAsFloat(9);
            float den=m.getArgAsFloat(10);
            cout << "(" << cellX << "," << cellY << ") v=(" << dX << "," << dY << ")" << ", col=(" << red << "," << green << "," << blue << "," << alpha << ")" << " radius=" << radius << ", temp=" << temp << ", den=" << den << endl;
            ofPoint m = ofPoint(cellX, cellY);
//        } else if (m.getAddress()=="/navier/scale") {
//            fluid.scale=m.getArgAsFloat(0);
//        } else if (m.getAddress()=="/navier/smokeBuoyancy") {
//            fluid.smokeBuoyancy=m.getArgAsFloat(0);
            ofPoint d = ofPoint(dX, dY)/ofGetFrameRate();  // Velocity in pixels/frame
            pendingForces.push_back(punctualForce(m, d, ofFloatColor(red,green,blue,alpha),radius,temp,den));
        } else if (m.getAddress()=="/navier/updateForces") {
            fluid.setConstantForces(pendingForces);  // Fluid will use these until they are updated again
            pendingForces.clear();
            updateFlame();
        } else if (m.getAddress()=="/navier/viscosity") {
            fluid.viscosity=m.getArgAsFloat(0);
            cout << "viscosity=" << fluid.viscosity << endl;
        } else if (m.getAddress()=="/navier/iterations") {
            fluid.numJacobiIterations=m.getArgAsInt32(0);
            cout << "iterations=" << fluid.numJacobiIterations << endl;
        } else if (m.getAddress()=="/navier/dissipation") {
            fluid.dissipation=m.getArgAsFloat(0);
            cout << "dissipation=" << fluid.dissipation << endl;
        } else if (m.getAddress()=="/navier/velocityDissipation") {
            fluid.velocityDissipation=m.getArgAsFloat(0);
            cout << "velocity dissipation=" << fluid.velocityDissipation << endl;
        } else if (m.getAddress()=="/navier/temperatureDissipation") {
            fluid.temperatureDissipation=m.getArgAsFloat(0);
            cout << "temperature dissipation=" << fluid.temperatureDissipation << endl;
        } else if (m.getAddress()=="/navier/pressureDissipation") {
            fluid.pressureDissipation=m.getArgAsFloat(0);
            cout << "pressure dissipation=" << fluid.pressureDissipation << endl;
        } else if (m.getAddress()=="/navier/gravity") {
            fluid.setGravity(ofVec2f(m.getArgAsFloat(0),m.getArgAsFloat(1)));
        } else if (m.getAddress()=="/navier/capture") {
            saveTexture("/tmp/pingpong.tif",fluid.getTexture());
        } else if (m.getAddress()=="/navier/clear") {
            fluid.clear();
        } else if (m.getAddress()=="/navier/quit") {
            ofExit();
	} else if (m.getAddress()=="/navier/flame") {
	    flameEnable=m.getArgAsFloat(0)>0.5;
	    flamePosition=ofPoint(m.getArgAsFloat(1),m.getArgAsFloat(2));
	    flameVelocity=ofPoint(m.getArgAsFloat(3),m.getArgAsFloat(4));
	    flameDensity=m.getArgAsFloat(5);
	    flameTemperature=m.getArgAsFloat(6);
	    flameRadius=m.getArgAsFloat(7);
	    updateFlame();
    } else {
        cout << "Unexpected OSC message: " << m.getAddress() << endl;
    }
        
    }

    syphon[0].publishTexture(&fluid.getTexture());
    syphon[2].publishTexture(&fluid.getVelocityTexture());
    syphon[3].publishTexture(&fluid.getTemperatureTexture());
    syphon[4].publishTexture(&fluid.getPressureTexture());

}

void ofApp::saveTexture(string filename, const ofTexture &tex) {
    ofPixels pixels;  // Converts to 8-bit from 32F texture format
    tex.readToPixels(pixels);
    ofSaveImage(pixels,filename);
    cout << "Saved texture to " << filename << endl;
}
//--------------------------------------------------------------
void ofApp::draw(){
    ofBackgroundGradient(ofColor::gray, ofColor::black, OF_GRADIENT_LINEAR);
    fluid.draw();
    
    syphon[1].publishScreen();
}

//--------------------------------------------------------------
void ofApp::keyPressed(int key){
    if( key == 'p')
        bPaint = !bPaint;
    if( key == 'o')
        bObstacle = !bObstacle;
    if( key == 'b')
        bBounding = !bBounding;
    if( key == 'c')
        bClear = !bClear;
}

//--------------------------------------------------------------
void ofApp::keyReleased(int key){

}

//--------------------------------------------------------------
void ofApp::mouseMoved(int x, int y ){

}

//--------------------------------------------------------------
void ofApp::mouseDragged(int x, int y, int button){

}

//--------------------------------------------------------------
void ofApp::mousePressed(int x, int y, int button){

}

//--------------------------------------------------------------
void ofApp::mouseReleased(int x, int y, int button){

}

//--------------------------------------------------------------
void ofApp::mouseEntered(int x, int y){

}

//--------------------------------------------------------------
void ofApp::mouseExited(int x, int y){

}

//--------------------------------------------------------------
void ofApp::windowResized(int w, int h){

}

//--------------------------------------------------------------
void ofApp::gotMessage(ofMessage msg){

}

//--------------------------------------------------------------
void ofApp::dragEvent(ofDragInfo dragInfo){ 

}
