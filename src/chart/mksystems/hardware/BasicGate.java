/******************************************************************************
* Title: BasicGate.java
* Author: Mike Schoonover
* Date: 11/19/09
*
* Purpose:
*
* This is the root class for gates.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class BasicGate
//
// This is the base class for gates.
//

public class BasicGate extends Object{

IniFile configFile;

int channelIndex, gateIndex;

public String title, shortTitle;

boolean selectedFlag = false;

boolean newDataReady;

double gateStart = 0;
double gateStartTrackingOn = 0;
double gateStartTrackingOff = 0;
public int gatePixStart = 0;
public int gatePixStartAdjusted = 0;
public int gatePixEnd = 0;
public int gatePixEndAdjusted = 0;
double gateWidth = 0;
int gateLevel = 0;
public int gatePixLevel = 0;
int gateFlags = 0;

double previousGateStart;
double previousGateWidth;

public int InterfaceCrossingPixAdjusted = 0;

static int GATE_ACTIVE = 0x01;
static int GATE_REPORT_NOT_EXCEED = 0x02;
static int GATE_MAX_MIN = 0x04;
static int GATE_WALL_START = 0x08;
static int GATE_WALL_END = 0x10;
static int GATE_FIND_CROSSING = 0x20;
static int GATE_USES_TRACKING = 0x40;
static int GATE_FOR_FLAW = 0x80;
static int GATE_FOR_INTERFACE = 0x0100;

// references to point at the controls used to adjust the values - these
// references are set up by the object which handles the adjusters and are
// only used temporarily

public Object gateStartAdjuster;
public Object gateWidthAdjuster;
public Object gateLevelAdjuster;


//-----------------------------------------------------------------------------
// BasicGate::calculateGatePixelLocation
//
// Calculates the pixel locations for a gate from its time locations and
// various related offsets.
//
// Returns true if any value was changed.  This return value can then be used
// to decide if the gate data needs to be sent to a remote.
//
// This function performs the opposite of calculateGateTimeLocation.
//

public boolean calculateGatePixelLocation(double pUSPerPixel, int pDelayPix,
                                             int pCanvasHeight, int pVertOffset)
{

boolean valueChanged = false;

int nGatePixStart = (int)Math.round(gateStart / pUSPerPixel - pDelayPix);
if (gatePixStart != nGatePixStart){
    gatePixStart = nGatePixStart; valueChanged = true;
    }

int nGatePixEnd = gatePixStart + (int)Math.round(gateWidth / pUSPerPixel);
if (gatePixEnd != nGatePixEnd){
    gatePixEnd = nGatePixEnd; valueChanged = true;
    }

//make first calculation from the percentage value of gateLevel
int nGatePixLevel = (int)Math.round(gateLevel * pCanvasHeight / 100);

//add in the current vertical offset
nGatePixLevel += pVertOffset;

//invert level so 0,0 is at bottom left
if (nGatePixLevel < 0) nGatePixLevel = 0;
if (nGatePixLevel > pCanvasHeight) nGatePixLevel = pCanvasHeight;
nGatePixLevel = pCanvasHeight - nGatePixLevel;

if (gatePixLevel != nGatePixLevel){
    gatePixLevel = nGatePixLevel; valueChanged = true;
    }

return(valueChanged);

}//end of BasicGate::calculateGatePixelLocations
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::calculateGateTimeLocation
//
// Calculates the time locations for a gate from its pixel location and
// various related offsets.
//
// Returns true if any value was changed.  This return value can then be used
// to decide if the gate data needs to be sent to a remote.
//
// This function performs the opposite of calculateGatePixelLocation.
//

public boolean calculateGateTimeLocation(double pUSPerPixel, int pDelayPix,
                                             int pCanvasHeight, int pVertOffset)
{

boolean valueChanged = false;

double nGateStart =  (gatePixStart + pDelayPix) * pUSPerPixel;
if (gateStart != nGateStart){gateStart = nGateStart; valueChanged = true;}

double nGateWidth = (gatePixEnd - gatePixStart) * pUSPerPixel;
if (gateWidth != nGateWidth){gateWidth = nGateWidth; valueChanged = true;}

int nPixLevel = gatePixLevel; //use a temp variable - don't modify original

//invert level so 0,0 is back at top left
if (nPixLevel < 0) nPixLevel = 0;
if (nPixLevel > pCanvasHeight) nPixLevel = pCanvasHeight;
nPixLevel = pCanvasHeight - nPixLevel;

//remove the current vertical offset
nPixLevel = nPixLevel - pVertOffset;

int nGateLevel =
            (int)Math.round(((double)nPixLevel / (double)pCanvasHeight * 100));

if (gateLevel != nGateLevel){gateLevel = nGateLevel; valueChanged = true;}

return(valueChanged);

}//end of BasicGate::calculateGateTimeLocation
//-----------------------------------------------------------------------------

}//end of class BasicGate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
