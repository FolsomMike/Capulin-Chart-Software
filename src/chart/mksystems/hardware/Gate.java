/******************************************************************************
* Title: Gate.java
* Author: Mike Schoonover
* Date: 4/26/09
*
* Purpose:
*
* This class handles a gate.
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
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Gate
//
// This class handles an input gate.
//

public class Gate extends BasicGate{

// Variable triggerDirection = 0 if the data is to be flagged if it goes over
// the gate, 1 if data is flagged for going below the gate
int triggerDirection;

// Variable peakDirection = 0 if peak is up, 1 if peak is down.  If the peak
// is up, the worst case values are considered to be the higher ones and vice
// versa.
int peakDirection;

public boolean parametersChanged, hitMissChanged, flagsChanged;

boolean isInterfaceGate = false;
boolean isWallStartGate = false;
boolean isWallEndGate = false;
boolean isFlawGate = false;

// Variables chart and trace specify where the data for this gate will be
// displayed.  If trace is a negative number, the data will not be displayed
// but a flag will be triggered and an event reported for a violation.

public int chartGroup, chart, trace;

Trace tracePtr; //a pointer to the trace attached to this gate

public int[] dBuffer1;
public int[] dBuffer2;
public int[] fBuffer;
public Threshold[] thresholds;
public int plotStyle;
public int clockPos;

public int dataPeak; //used for channels without min and max peaks
public int dataMaxPeak;  //used for channels with min and max peaks
public int dataMinPeak;  //used for channels with minand max peaks

public double dataPeakD; //used for channels without min and max peaks (double)
public double dataMaxPeakD;  //used for channels with min and max peaks (double)
public double dataMinPeakD;  //used for channels with minand max peaks (double)

double wallThickness;

int peakFlags;  //any flags associated with the peak
int peakFlightTime; //the time of flight to the peak (for UT)
int peakTrack; //encoder tracking information

// The encoder1 parameter is the entry encoder or the carriage encoder
// depending on unit type.
// The encoder2 parameter is the exit encoder or the rotational encoder
// depending on unit type.
int encoder1, encoder2;

public int gateHitCount = 0;
public int gateMissCount = 0;

// references to point at the controls used to adjust the values - these
// references are set up by the object which handles the adjusters and are
// only used temporarily

public Object gateHitCountAdjuster;
public Object gateMissCountAdjuster;

//-----------------------------------------------------------------------------
// Gate::Gate (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
  
public Gate(IniFile pConfigFile, int pChannelIndex, int pGateIndex)
{

configFile = pConfigFile; channelIndex = pChannelIndex; gateIndex = pGateIndex;

//read the configuration file and create/setup the charting/control elements
configure(configFile);

//set the gate active flag for each gate
setActive(true);

}//end of Gate::Gate (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::storeNewData
//
// Stores the new data value and sets the newDataReadyFlag true.
//
// pDataPeak is the peak data (either a min or a max) for signals which
// only have one peak.
//
// pDataMaxPeak and pDataMinPeak are for signals which have both a min and a
// max peak, such as a Wall thickness signal. //debug mks - remove this?
//
// The encoder1 parameter is the entry encoder or the carriage encoder
// depending on unit type.
//
// The encoder2 parameter is the exit encoder or the rotational encoder
// depending on unit type.
//

public void storeNewData(int pDataPeak, int pDataMaxPeak, int pDataMinPeak,
        double pDataPeakD, int dPeakFlags, int dPeakFlightTime, int dPeakTrack,
        int pEncoder1, int pEncoder2)
{

dataPeak = pDataPeak;
dataMaxPeak = pDataMaxPeak; //debug mks - remove all references to this?
dataMinPeak = pDataMinPeak; //debug mks - remove all references to this?

dataPeakD = pDataPeakD;

peakFlags = dPeakFlags;
peakFlightTime = dPeakFlightTime;
peakTrack = dPeakTrack;

encoder1 = pEncoder1; encoder2 = pEncoder2;

newDataReady = true;

}//end of Gate::storeNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::storeNewDataD
//
// Stores the new data value and sets the newDataReadyFlag true.
//
// pDataPeakD is the peak data (either a min or a max) for signals which
// only have one peak which is a double.
//

public void storeNewDataD(double pDataPeakD, int dPeakTrack)
{

dataPeakD = pDataPeakD;

peakTrack = dPeakTrack;

newDataReady = true;

}//end of Gate::storeNewDataD
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::getNewData
//
// This function prepares data for access. The data value(s) can be accessed in
// public member variables in this class.  The pointer gatePtr in hdwVs will be
// set to point to this instance so those members can be accessed.
//
// Note: Any calculation in this function should only use a single peak data
//  variable to avoid glitches when another thread is writing to the variable.
//  Multiple calculations are okay, so long as each only depends on a single
//  peak value.
//

public void getNewData(HardwareVars hdwVs)
{

newDataReady = false; // clear the flag until new data is available

hdwVs.gatePtr = this; // pass back a pointer to this instance

//if the gate is a wall gate, convert the data to chart height position
if (isWallStartGate || isWallEndGate){

    //convert nanosecond time span to distance
    //dataPeakD is the only variable possibly changed by peak data updates
    wallThickness = dataPeakD * hdwVs.nSPerDataPoint * hdwVs.velocityNS /
                                                (hdwVs.numberOfMultiples * 2);

    //convert distance to a chart height position
    dataPeak = (int)((wallThickness - hdwVs.nominalWall)
                    / hdwVs.wallChartScale) + hdwVs.nominalWallChartPosition;

    }

}//end of Gate::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::getDefaultData
//
// Returns whatever data is stored.  Use this when the channel is off or masked
// to return a dummy value and set the necessary pointers in hdwVs.
//

public void getDefaultData(HardwareVars hdwVs)
{

hdwVs.gatePtr = this; // pass back a pointer to this instance

}//end of Gate::getDefaultData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::getTrace
//
// Returns a pointer to the trace attached to this gate.
//
//

public Trace getTrace()
{

return tracePtr;

}//end of Gate::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::getFlags
//
// Returns the gate's flags.
//

public int getFlags()
{

return gateFlags;

}//end of Gate::getFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setActive
//
// Turns the gate active flag on or off.
//
// Does not set the flag in the DSP.
//

public final void setActive(boolean pValue)
{

if (pValue)
    gateFlags |= GATE_ACTIVE;
else
    gateFlags &= (~GATE_ACTIVE);

}//end of Gate::setActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setMaxMin
//
// Turns the gate max/min peak search flag on or off.  If pOn is true, the gate
// will be set up as a max peak catching gate.  If pOn is false, the gate
// will be set up as a min peak catching gate.
//
// Does not set the flag in the DSP.
//

public void setMaxMin(boolean pOn)
{

if (pOn)
    gateFlags &= (~GATE_MAX_MIN); //b = 0 for max gate
else
    gateFlags |= GATE_MAX_MIN; //b = 1 for min gate

}//end of Gate::setMaxMin
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setWallStart
//
// Turns the gate wall start flag on or off.  If pOn is true, the gate will be
// used as the first gate for measuring wall thickness.
//
// The GATE_FIND_CROSSING flag should NOT be set along with this flag - the
// DSPs implicitly perform a crossing search for the wall start gate.
//
// Does not set the flag in the DSP.
//

public void setWallStart(boolean pOn)
{

isWallStartGate = pOn;

if (pOn)
    gateFlags |= GATE_WALL_START;
else
    gateFlags &= (~GATE_WALL_START);

}//end of Gate::setWallStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setWallEnd
//
// Turns the gate wall end flag on or off.  If pOn is true, the gate will be
// used as the second gate for measuring wall thickness.
//
// The GATE_FIND_CROSSING flag should NOT be set along with this flag - the
// DSPs implicitly perform a crossing search for the wall end gate.
//
// Does not set the flag in the DSP.
//

public void setWallEnd(boolean pOn)
{

isWallEndGate = pOn;

if (pOn)
    gateFlags |= GATE_WALL_END;
else
    gateFlags &= (~GATE_WALL_END);

}//end of Gate::setWallEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setCrossingSearch
//
// Turns the gate signal crossing search function flag on or off.  If pOn is
// true, the gate will be scanned for the point where the signal exceeds the
// gate in the min or max direction depending on the type of gate.
//
// Does not set the flag in the DSP.
//

public void setCrossingSearch(boolean pOn)
{

if (pOn)
    gateFlags |= GATE_FIND_CROSSING;
else
    gateFlags &= (~GATE_FIND_CROSSING);

}//end of Gate::setCrossingSearch
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setInterfaceGate
//
// Designates the gate for use in tracking the interface and updating the
// positions of the other gates if interface tracking is on.
//
// The GATE_FIND_CROSSING flag should NOT be set along with this flag - the
// DSPs implicitly perform a crossing search for the interface gate.
//
// Does not set the flag in the DSP.
//

public void setInterfaceGate(boolean pOn)
{

isInterfaceGate = pOn;

if (pOn)
    gateFlags |= GATE_FOR_INTERFACE;
else
    gateFlags &= (~GATE_FOR_INTERFACE);

}//end of Gate::setInterfaceGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::getInterfaceGate
//
// Returns the isInterfaceGate flag - true if the gate is designated as the
// interface gate, false if not.
//
// Does not set the flag in the DSP.
//

public boolean getInterfaceGate()
{

return isInterfaceGate;

}//end of Gate::getInterfaceGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setFlawGate
//
// Turns the gate signal peak search function flag on or off.  If pOn is
// true, the gate will be scanned for the greatest signal in the min or max
// direction depending on the type of gate.
//
// Does not set the flag in the DSP.
//

public void setFlawGate(boolean pOn)
{

isFlawGate = pOn;

if (pOn)
    gateFlags |= GATE_FOR_FLAW;
else
    gateFlags &= (~GATE_FOR_FLAW);

}//end of Gate::setFlawGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setReportNonExceeding
//
// Turns the flag on or off for the gate alarm for non-exceeding signals.  If
// pOn is true, the gate will report instances when the signal did not exceed
// the gate threshold in the min or max direction depending on the type of gate.
//
// Does not set the flag in the DSP.
//

public void setReportNonExceeding(boolean pOn)
{

if (pOn)
    gateFlags |= GATE_REPORT_NOT_EXCEED;
else
    gateFlags &= (~GATE_REPORT_NOT_EXCEED);

}//end of Gate::setReportNonExceeding
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::setInterfaceTracking
//
// Turns the interface tracking flag on or off.
//
// Does not set the flag in the DSP.
//

public void setInterfaceTracking(boolean pValue)
{

if (pValue)
    gateFlags |= GATE_USES_TRACKING;
else
    gateFlags &= (~GATE_USES_TRACKING);

}//end of Gate::setInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::linkTraces
//
// This function is called by traces to link their buffers to specific hardware
// channels/gates and give a link back to variables in the Trace object.
//
// The values are only stored if the pChartGroup, pChart, and pTrace parameters
// match those loaded for this gate from the config file.
//

public void linkTraces(int pChartGroup, int pChart, int pTrace, int[] pDBuffer,
   int[] pDBuffer2, int[] pFBuffer, Threshold[] pThresholds, int pPlotStyle,
   Trace pTracePtr)
{

if (pChartGroup == chartGroup && pChart == chart && pTrace == trace){
    
    //store the buffer references in the specified channel to link the trace
    dBuffer1 = pDBuffer; dBuffer2 = pDBuffer2; fBuffer = pFBuffer;

    thresholds = pThresholds;

    plotStyle = pPlotStyle;

    tracePtr = pTracePtr;

    }

}//end of Gate::linkTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//
//

private void configure(IniFile pConfigFile)
{

String whichGate = "Channel " + (channelIndex+1) + " Gate " + (gateIndex+1);

title = 
      pConfigFile.readString(whichGate, "Title", "Gate " + (gateIndex+1));

shortTitle = pConfigFile.readString(
                            whichGate, "Short Title", "G " + (gateIndex+1));

setInterfaceGate(pConfigFile.readBoolean(whichGate, "Interface Gate", false));
setWallStart(pConfigFile.readBoolean(whichGate, "Wall Start Gate", false));
setWallEnd(pConfigFile.readBoolean(whichGate, "Wall End Gate", false));
setFlawGate(pConfigFile.readBoolean(whichGate, "Flaw Gate", false));

triggerDirection = pConfigFile.readInt(whichGate, "Trigger Direction", 0);

peakDirection = pConfigFile.readInt(whichGate, "Peak Direction", 0);
setMaxMin(peakDirection == 0 ? true : false);

chartGroup = pConfigFile.readInt(whichGate, "Chart Group", 0) - 1;

chart = pConfigFile.readInt(whichGate, "Chart", 0) - 1;

trace = pConfigFile.readInt(whichGate, "Trace", 0) -1;

}//end of Gate::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public synchronized void loadCalFile(IniFile pCalFile)
{

String section = "Channel " + (channelIndex + 1) + " Gate " + (gateIndex + 1);

gateStart = pCalFile.readDouble(section, "Gate Start", 50);
gateStartTrackingOn = pCalFile.readDouble(section,
                "Gate Start with Interface Tracking", 50);
gateStartTrackingOff = pCalFile.readDouble(section,
            "Gate Start without Interface Tracking", 50);
gateWidth = pCalFile.readDouble(section, "Gate Width", 2);
gateLevel = pCalFile.readInt(section, "Gate Level", 15);
gateHitCount = pCalFile.readInt(section, "Gate Hit Count", 1);
gateMissCount = pCalFile.readInt(section, "Gate Miss Count", 1);

//set all the data changed flags so data will be sent to remotes
parametersChanged = true; hitMissChanged = true; flagsChanged = true;

}//end of Gate::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Gate::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

String section = "Channel " + (channelIndex + 1) + " Gate " + (gateIndex + 1);

pCalFile.writeDouble(section, "Gate Start", gateStart);
pCalFile.writeDouble(section,
                "Gate Start with Interface Tracking",  gateStartTrackingOn);
pCalFile.writeDouble(section,
            "Gate Start without Interface Tracking",  gateStartTrackingOff);
pCalFile.writeDouble(section, "Gate Width", gateWidth);
pCalFile.writeInt(section, "Gate Level", gateLevel);

pCalFile.writeInt(section, "Gate Hit Count", gateHitCount);
pCalFile.writeInt(section, "Gate Miss Count", gateMissCount);

}//end of Gate::saveCalFile
//-----------------------------------------------------------------------------

}//end of class Gate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
