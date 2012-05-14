/******************************************************************************
* Title: HardwareVars.java
* Author: Mike Schoonover
* Date: 4/26/08
*
* Purpose:
*
* This class encapsulates variables related to the Hardware class.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.stripchart.TraceHdwVars;
import chart.mksystems.stripchart.Threshold;

//-----------------------------------------------------------------------------
// class HardwareVars
//
//

public class HardwareVars extends Object{

int nextIndex;
TraceHdwVars hdwVs;
Threshold[] thresholds;

Gate gatePtr;

double nSPerDataPoint, uSPerDataPoint;

public double velocityUS, velocityNS, nominalWall, wallChartScale;
public int nominalWallChartPosition, numberOfMultiples;

public double velocityShearUS, velocityShearNS;

boolean waitForOffPipe = false;
boolean waitForOnPipe = false;
boolean waitForInspectStart = false;
boolean watchForOffPipe = false;
boolean head1Down = false;
boolean head2Down = false;

//used to track count from photo eye clear to end of piece
public int endOfPieceTracker;
public boolean trackToEndOfPiece;
public int endOfPiecePosition = 210; //NOTE: load this from config file


public boolean nearStartOfPiece;
public int nearStartOfPieceTracker;
//position is distance from start of piece for which modifier is to be applied
public int nearStartOfPiecePosition = 350; //NOTE: load this from config file

public boolean nearEndOfPiece;
public int nearEndOfPieceTracker;
//position is distance from photo eye clear signal to location where modifier
//is to be applied until the end of the pipe
public int nearEndOfPiecePosition = 1; //NOTE: load this from config file
public boolean trackToNearEndofPiece;


//these are used to transfer values from gates specified in the configuration
//file -- the values are used to modify the wall traces so that a flaw gate
//can create a spike on the wall trace(s)
public int wallMaxModifier;
public int wallMinModifier;

}//end of class HardwareVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------    
