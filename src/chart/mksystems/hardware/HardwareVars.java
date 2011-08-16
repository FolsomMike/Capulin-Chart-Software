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

}//end of class HardwareVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------    
