/******************************************************************************
* Title: UTGate.java
* Author: Mike Schoonover
* Date: 4/26/09
*
* Purpose:
*
* This class handles an ultrasonic gate.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.Xfer;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import chart.mksystems.threadsafe.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTGate
//
// This class handles an input gate.
//

public class UTGate extends BasicGate{

    // Variable triggerDirection = 0 if the data is to be flagged if it goes
    // over the gate, 1 if data is flagged for going below the gate
    int triggerDirection;

    // Variable peakDirection = 0 if peak is up, 1 if peak is down.  If the peak
    // is up, the worst case values are considered to be the higher ones and
    // vice versa.
    int peakDirection;

    //the peak value and the opposite depending on the type of gate
    //for a MAX gate, the peak value is Integer.MAX_VALUE, for a MIN gate
    //the peak value is Integer.MIN_VALUE. These are set when the gates MIN/MAX
    //flag is set.
    int peakValue = 0, antiPeakValue = 0;

    boolean isInterfaceGate = false;
    boolean isWallStartGate = false;
    boolean isWallEndGate = false;
    boolean isFlawGate = false;
    boolean maxMin = false;
    boolean doCrossingSearch = false;
    boolean reportNotExceeding = false;
    boolean doInterfaceTracking = false;
    boolean doFindPeak = false;
    boolean doIntegrateAboveGate = false;
    boolean doQuenchOnOverLimit = false;
    boolean isAScanTriggerGate = false;
    boolean doFindDualPeakCenter = false;
    boolean doSignalAveraging = false;
    
    int aScanSmoothing = 1;

    //if true, then data from this gate is stored where it can be used to modify
    //the wall data -- this allows a flaw gate to produce a kick on the wall
    //trace

    //Note:  need to add flag in the config file to specify whether the gate
    // tweaks the min or max.

    public boolean modifyWall;

    // Variables chart and trace specify where the data for this gate will be
    // displayed.  If trace is a negative number, the data will not be displayed
    // but a flag will be triggered and an event reported for a violation.

    public int chartGroup, chart, trace;

    Trace tracePtr; //a pointer to the trace attached to this gate

    TraceData traceData;
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

    //these peak variables are used to capture peak data for display on or near
    //the AScan display -- they show peaks that occurred between captures of the
    //AScan data -- can be displayed on a peak meter or similar

    //need to add a min peak capture as well -- especially for RF?

    public int aScanPeak = Integer.MIN_VALUE, aScanPeakD = Integer.MIN_VALUE;
    public int aScanPeakFlightTime;

    public ArrayList<String> flawGateProcessList, iFaceProcessList;
    public ArrayList<String> wallGateProcessList;

    // The encoder1 parameter is the entry encoder or the carriage encoder
    // depending on unit type.
    // The encoder2 parameter is the exit encoder or the rotational encoder
    // depending on unit type.
    int encoder1, encoder2;

    SyncedInteger gateHitCount;
    SyncedInteger gateMissCount;
    SyncedInteger sigProcTuning1, sigProcTuning2, sigProcTuning3;
    public String signalProcessing = "undefined";

    // references to point at the controls used to adjust the values - these
    // references are set up by the object which handles the adjusters and are
    // only used temporarily

    public Object gateHitCountAdjuster;
    public Object gateMissCountAdjuster;
    public Object processingSelector;
    public Object tuning1Adjuster, tuning2Adjuster, tuning3Adjuster;
    public Object signalFilterSelector;
  
    //constants

    static int MAX = 0;
    static int MIN = 1;

//-----------------------------------------------------------------------------
// UTGate::UTGate (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
// The constructing class should pass a pointer to a SyncedVariableSet for the
// values in this class which can be changed by the user and are sent to the
// remotes so that they will be managed in a threadsafe manner.
//

public UTGate(IniFile pConfigFile, int pChannelIndex,
                            int pGateIndex, SyncedVariableSet pSyncedVarMgr)
{

    super(pSyncedVarMgr);

    configFile = pConfigFile;
    channelIndex = pChannelIndex; gateIndex = pGateIndex;

}//end of UTGate::UTGate (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    gateHitCount = new SyncedInteger(syncedVarMgr); gateHitCount.init();
    gateMissCount = new SyncedInteger(syncedVarMgr); gateMissCount.init();
    sigProcTuning1 = new SyncedInteger(syncedVarMgr); sigProcTuning1.init();
    sigProcTuning2 = new SyncedInteger(syncedVarMgr); sigProcTuning2.init();
    sigProcTuning3 = new SyncedInteger(syncedVarMgr); sigProcTuning3.init();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

    //create list of process type which can be applied to the gates
    //WARNING: dont' make text entries into the list too long or it will widen
    //         the UT calibrator window and make it look bad.

    //processing options for a normal gate
    flawGateProcessList = new ArrayList<>();
    flawGateProcessList.add("peak");
    flawGateProcessList.add("peak / average");
    //option to integrate signal above the gate
    flawGateProcessList.add("enhance above gate");
    //option to integrate signal above the gate and perform signal averaging
    flawGateProcessList.add("enhance above / average");
    
    //processing options for an interface gate
    iFaceProcessList = new ArrayList<>();
    iFaceProcessList.add("ignore bad interface");
    iFaceProcessList.add("quench on bad interface");

    //processing options for a wall start/end gate
    wallGateProcessList = new ArrayList<>();
    wallGateProcessList.add("first crossing");
    wallGateProcessList.add("peak");
    wallGateProcessList.add("center of dual peaks");
    
    //set the gate active flag for each gate
    setActive(true);

}//end of UTGate::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::storeNewData
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
        int pClockPos, int pEncoder1, int pEncoder2)
{

    dataPeak = pDataPeak;
    dataMaxPeak = pDataMaxPeak; //debug mks - remove all references to this?
    dataMinPeak = pDataMinPeak; //debug mks - remove all references to this?

    dataPeakD = pDataPeakD;

    peakFlags = dPeakFlags;
    peakFlightTime = dPeakFlightTime;
    peakTrack = dPeakTrack;
    clockPos = pClockPos;

    encoder1 = pEncoder1; encoder2 = pEncoder2;

    newDataReady = true;

}//end of UTGate::storeNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::storeNewDataD
//
// Stores the new data value and sets the newDataReadyFlag true.
//
// pDataPeakD is the peak data (either a min or a max) for signals which
// only have one peak which is a double.
//

public void storeNewDataD(double pDataPeakD, int dPeakTrack, int pClockPos)
{

    dataPeakD = pDataPeakD;

    peakTrack = dPeakTrack;
    clockPos = pClockPos;

    newDataReady = true;

}//end of UTGate::storeNewDataD
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::storeNewAScanPeak
//
// If pPeak is higher than the peak value already stored, pPeak will replace
// that value in aScanPeak and the flight time (time after the initial pulse
// at which the peak was detected) will be stored.
//

public void storeNewAScanPeak(int pPeak, int pAScanPeakFlightTime)
{

    if (pPeak > aScanPeak){
        aScanPeak = pPeak;
        aScanPeakFlightTime = pAScanPeakFlightTime;
    }

}//end of UTGate::storeNewAScanPeak
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getAndClearAScanPeak()
//
// Returns aScanPeak and resets it to its minimum so the next peak can be
// captured.  The peak's flight time (time after the initial pulse
// at which the peak was detected) is also returned.
//
// The values are returned in the pAScanPeakInfo object.
//

public void getAndClearAScanPeak(Xfer pAScanPeakInfo)
{

    pAScanPeakInfo.rInt1 = aScanPeak;

    pAScanPeakInfo.rInt2 = aScanPeakFlightTime;

    aScanPeak = Integer.MIN_VALUE;

}//end of UTGate::getAndClearAScanPeak
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setFlags
//
// Sets the various bits in gateFlags to match the gate type (INTERFACE, FLAW,
// ETC.) and current processing modes.
//
// It is not a public method -- it is manipulated by calling other functions
// to select the gate type and processing methods.
//
// Does not send the flag to the DSP, but the data changed flag for the value
// will be set so the change can be sent later.
//

private void setFlags()
{

    int flags = 0;

    //set the bits common to all gate types

    if (gateActive) {
        flags |= GATE_ACTIVE;
    }
    else {
        flags &= (~GATE_ACTIVE);
    }

    if (doFindPeak) {
        flags |= GATE_FIND_PEAK;
    }
    else {
        flags &= (~GATE_FIND_PEAK);
    }

    if (doIntegrateAboveGate) {
        flags |= GATE_INTEGRATE_ABOVE_PEAK;
    }
    else {
        flags &= (~GATE_INTEGRATE_ABOVE_PEAK);
    }

    if (maxMin) {
        flags &= (~GATE_MAX_MIN);
    } //b = 0 for max gate
    else {
        flags |= GATE_MAX_MIN;
    } //b = 1 for min gate

    if (isWallStartGate) {
        flags |= GATE_WALL_START;
    }
    else {
        flags &= (~GATE_WALL_START);
    }

    if (isWallEndGate) {
        flags |= GATE_WALL_END;
    }
    else {
        flags &= (~GATE_WALL_END);
    }

    if (doCrossingSearch) {
        flags |= GATE_FIND_CROSSING;
    }
    else {
        flags &= (~GATE_FIND_CROSSING);
    }

    if (isInterfaceGate) {
        flags |= GATE_FOR_INTERFACE;
    }
    else {
        flags &= (~GATE_FOR_INTERFACE);
    }

    if (reportNotExceeding) {
        flags |= GATE_REPORT_NOT_EXCEED;
    }
    else {
        flags &= (~GATE_REPORT_NOT_EXCEED);
    }

    if (doInterfaceTracking) {
        flags |= GATE_USES_TRACKING;
    }
    else {
        flags &= (~GATE_USES_TRACKING);
    }

    if (doQuenchOnOverLimit) {
        flags |= GATE_QUENCH_IF_OVERLIMIT;
    }
    else {
        flags &= (~GATE_QUENCH_IF_OVERLIMIT);
    }

    if (isAScanTriggerGate) {
        flags |= GATE_TRIGGER_ASCAN_SAVE;
    }
    else {
        flags &= (~GATE_TRIGGER_ASCAN_SAVE);
    }

    if (doFindDualPeakCenter) {
        flags |= GATE_FIND_DUAL_PEAK_CENTER;
    }
    else {
        flags &= (~GATE_FIND_DUAL_PEAK_CENTER);
    }

    if (doSignalAveraging) {
        flags |= GATE_APPLY_SIGNAL_AVERAGING;
    }
    else {
        flags &= (~GATE_APPLY_SIGNAL_AVERAGING);
    }
    
    //insert the AScan averaging value into the flags

    //all gates use the channel's aScanSmoothing value to determine
    //their data averaging depth, but 4 is max allowed even though
    //aScanSmoothing can be higher

    int averaging = aScanSmoothing;
    if (averaging > 4) {averaging = 4;}
    if (averaging < 1) {averaging = 1;}
    //shift down ~ 1-4 -> 0-3
    averaging--;
    //merge into bits 15,14 of flags value
    flags |= (averaging<<14);

    gateFlags.setValue(flags, true);

}//end of UTGate::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getFlags
//
// Returns the gate's flags.
//

public SyncedInteger getFlags()
{

    return (gateFlags);

}//end of UTGate::getFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getNewData
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

}//end of UTGate::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getInactiveData
//
// Sets all data to the opposite of the peak value so that it will be
// overridden by data from any other active channel -- this is because the
// peak data is kept from all channels tied to any one trace.
//
// This method should be called to get data when the owning channel is
// inactive.
//

public void getInactiveData(HardwareVars hdwVs)
{

    newDataReady = false; // clear the flag until new data is available

    hdwVs.gatePtr = this; // pass back a pointer to this instance

    dataPeak = antiPeakValue;
    dataMaxPeak = antiPeakValue;
    dataMinPeak = peakValue;
    dataPeakD = antiPeakValue;

}//end of UTGate::getInactiveData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getDefaultData
//
// Returns whatever data is stored.  Use this when the channel is off or masked
// to return a dummy value and set the necessary pointers in hdwVs.
//

public void getDefaultData(HardwareVars hdwVs)
{

    hdwVs.gatePtr = this; // pass back a pointer to this instance

}//end of UTGate::getDefaultData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getTrace
//
// Returns a pointer to the trace attached to this gate.
//
//

public Trace getTrace()
{

    return tracePtr;

}//end of UTGate::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setSignalProcessing
//
// Sets the signal processing mode.  If the mode is not a valid selection for
// the gate type, the mode is forced to the first valid mode listed for the
// gate type.
//
// Does not set the flags in the DSP.
//

public void setSignalProcessing(String pMode)
{

    //get the signal processing list for the gate type
    ArrayList<String> pl = getSigProcList();

    //if the selection is not in the valid list for the gate type, reset it to
    //the first entry in that valid list
    if (!pl.contains(pMode)) {pMode = pl.get(0);}

    signalProcessing = pMode;

    if (signalProcessing.equals("peak")){
        //option to use unprocessed peak in the gate
        setFindPeak(true);
        setCrossingSearch(false);
        setIntegrateAboveGate(false);
        setFindDualPeakCenter(false);
        setDoSignalAveraging(false);        
        return;
    }

    if (signalProcessing.equals("peak / average")){
        //option to use unprocessed peak in the gate with signal averaging
        setFindPeak(true);
        setCrossingSearch(false);
        setIntegrateAboveGate(false);
        setFindDualPeakCenter(false);
        setDoSignalAveraging(true);
        return;
    }
        
    if (signalProcessing.equals("first crossing")){
        //option to find first crossing point in gate
        setFindPeak(false);
        setCrossingSearch(true);
        setIntegrateAboveGate(false);
        setFindDualPeakCenter(false);
        setDoSignalAveraging(false);        
        return;
    }

    if (signalProcessing.equals("enhance above gate")){
        //option to integrate signal above the gate
        setFindPeak(false);
        setCrossingSearch(false);
        setIntegrateAboveGate(true);
        setFindDualPeakCenter(false);
        setDoSignalAveraging(false);
        return;
    }

    if (signalProcessing.equals("enhance above / average")){
        //option to integrate signal above the gate
        setFindPeak(false);
        setCrossingSearch(false);
        setIntegrateAboveGate(true);
        setFindDualPeakCenter(false);
        setDoSignalAveraging(true);
        return;
    }    
    
    if (signalProcessing.equals("center of dual peaks")){
        setFindPeak(false);
        setCrossingSearch(false);
        setIntegrateAboveGate(false);
        setFindDualPeakCenter(true);
        setDoSignalAveraging(false);
        return;
    }

    if (signalProcessing.equals("ignore bad interface")){
        setQuenchOnOverLimit(false);
        setIntegrateAboveGate(false);
        setDoSignalAveraging(false);
        return;
    }

    // Currently, the "quench on bad interface" option turns on the integrate over
    // gate and the quench on overlimit options.  Thus the quench will be triggered
    // if the integral of the signal above the gate is larger than the trigger
    // value.  As an alternative, the detect peak option (or other future method)
    // could be chosen rather than the integral to use the peak for the trigger
    // value.  The integral makes the most sense for detecting an excessively large
    // interface signal which usually signifies a bad coupling as the energy is
    // reflected at the boundary rather than entering the test piece.
    //
    // If the quench is triggered, all following gates will be ignored.  The
    // quench gate can be the interface gate if it is in use, or any other gate,
    // but will only affect the gates which follow.  Thus, it will be common for
    // the quench gate to be one of the first gates so that it can affect all the
    // following gates.

    if (signalProcessing.equals("quench on bad interface")){
        setQuenchOnOverLimit(true);
        setIntegrateAboveGate(true);
        setDoSignalAveraging(false);        
        return;
    }

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setSignalProcessing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getSignalProcessing
//
// Returns the string containing the current signal processing mode.
// To get the index of the mode in the list appropriate for the gate type, use
// getSigProcIndex.
//

public String getSignalProcessing()
{

    return (signalProcessing);

}//end of UTGate::getSignalProcessing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getSigProcIndex
//
// Returns the index in the appropriate list of the current signal processing
// mode.
//

public int getSigProcIndex()
{

    //get the signal processing list for the gate type
    ArrayList<String> pl = getSigProcList();

    int index = pl.indexOf(signalProcessing);

    //if not found, index will be -1, set to 0 (the first item in the list
    if (index == -1) {index = 0;}

    return(index);

}//end of UTGate::getSigProcIndex
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getSigProcList
//
// Returns the list of signal processing modes which is valid for the gate's
// type.
//

public ArrayList<String> getSigProcList()
{

    //default to flawGateProcessList -- will be reset to proper list
    ArrayList<String> pl = flawGateProcessList;

    //choose the appropriate list for the gate type
    if (isInterfaceGate) {
        pl = iFaceProcessList;
    }
    else if (isFlawGate) {
        pl = flawGateProcessList;
    }
    else if (isWallStartGate) {
        pl = wallGateProcessList;
    }
    else if (isWallEndGate) {
        pl = wallGateProcessList;
    }

    return(pl);

}//end of UTGate::getSigProcList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setActive
//
// Turns the gate active flag on or off.
//
// Does not set the flag in the DSP.
//

public final void setActive(boolean pValue)
{

    gateActive = pValue;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setAScanSmoothing
//
// Sets the AScan smoothing averaging value.  This value is stored in bits
// in the flags.
//
// Does not set the flag in the DSP.
//

public final void setAScanSmoothing(int pAScanSmoothing)
{

    aScanSmoothing = pAScanSmoothing;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setMaxMin
//
// Turns the gate max/min peak search flag on or off.  If pOn is true, the gate
// will be set up as a max peak catching gate.  If pOn is false, the gate
// will be set up as a min peak catching gate.
//
// Sets the values for the maximum peak and its opposite depending on the state
// of maxMin.
//
// Does not set the flag in the DSP.
//

public void setMaxMin(boolean pOn)
{

    maxMin = pOn;

    if (maxMin){
        peakValue = Integer.MAX_VALUE;
        antiPeakValue = Integer.MIN_VALUE;
    }
    else{
        peakValue = Integer.MIN_VALUE;
        antiPeakValue = Integer.MAX_VALUE;
    }

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setMaxMin
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setWallStart
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

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setWallStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getWallStart
//
// Returns the isWallStartGate flag - true if the gate is designated as a wall
// start gate false if not.
//

public boolean getWallStart()
{

    return isWallStartGate;

}//end of UTGate::getWallStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setWallEnd
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

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setWallEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getWallEnd
//
// Returns the isWallEndGate flag - true if the gate is designated as a wall
// start gate false if not.
//

public boolean getWallEnd()
{

    return isWallEndGate;

}//end of UTGate::getWallEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setCrossingSearch
//
// Turns the gate signal crossing search function flag on or off.  If pOn is
// true, the gate will be scanned for the point where the signal exceeds the
// gate in the min or max direction depending on the type of gate.
//
// Does not set the flag in the DSP.
//

public void setCrossingSearch(boolean pOn)
{

    doCrossingSearch = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setCrossingSearch
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setInterfaceGate
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

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setInterfaceGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getInterfaceGate
//
// Returns the isInterfaceGate flag - true if the gate is designated as the
// interface gate, false if not.
//

public boolean getInterfaceGate()
{

    return isInterfaceGate;

}//end of UTGate::getInterfaceGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setFlawGate
//
//
// Turns the gate flaw type flag on or off.
//
// Does not set the flag in the DSP.
//

public void setFlawGate(boolean pOn)
{

    isFlawGate = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setFlawGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getFlawGate
//
// Returns the isFlawGate flag - true if the gate is designated as a flaw gate
// false if not.
//

public boolean getFlawGate()
{

    return isFlawGate;

}//end of UTGate::getFlawGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setFindPeak
//
// Turns the gate signal peak search function flag on or off.  If pOn is
// true, the gate will be scanned for the greatest signal in the min or max
// direction depending on the type of gate.
//
// Does not set the flag in the DSP.
//

public void setFindPeak(boolean pOn)
{

    doFindPeak = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setFindPeak
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setIntegrateAboveGate
//
// Turns the integrate above gate flag on or off.  If pOn is true, the data
// above the gate level will be integrated for the result.
//
// Does not set the flag in the DSP.
//

public void setIntegrateAboveGate(boolean pOn)
{

    doIntegrateAboveGate = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setIntegrateAboveGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setFindDualPeakCenter
//
// Turns the find dual peak center algorithm on or off.
//
// If pState is true, the center (timewise) between the two largest peaks in
// the gate is determined and returned as the location of the peak. At least
// one of the peaks must exceed the gate level.
//
// Does not set the flag in the DSP.
//

public void setFindDualPeakCenter(boolean pState)
{

    doFindDualPeakCenter = pState;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setFindDualPeakCenter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setDoSignalAveraging
//
// Turns the signal averaging algorithm on or off.
//
// If turned on, returned peak is the average of the peaks over a number
// of samples. The number of samples averaged is determined by a value sent
// from the host.
//
// Does not set the flag in the DSP.
//

public void setDoSignalAveraging(boolean pState)
{

    doSignalAveraging = pState;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setDoSignalAveraging
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setQuenchOnOverLimit
//
// Turns the flag on or off for the set quench on over limit.  If pOn is
// true, all gates after this one will be ignored if the value obtained from
// the gate exceeds a trigger level preset by the host.
//
// Does not set the flag in the DSP.
//

public void setQuenchOnOverLimit(boolean pOn)
{

    doQuenchOnOverLimit = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setQuenchOnOverLimit
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setAScanTriggerGate
//
// Turns the isAScanTriggerGate flag on or off.
//
// Does not set the flag in the DSP.
//

public final void setAScanTriggerGate(boolean pValue)
{

    isAScanTriggerGate = pValue;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setAScanTriggerGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::getIsAScanTriggerGate
//
// Returns the isAScanTriggerGate flag - true if the gate is designated as a
// trigger for an AScan capture, false if not.
//

public boolean getIsAScanTriggerGate()
{

    return (isAScanTriggerGate);

}//end of UTGate::getIsAScanTriggerGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::setInterfaceTracking
//
// Turns the interface tracking flag on or off.
//
// Does not set the flag in the DSP.
//

public void setInterfaceTracking(boolean pOn)
{

    doInterfaceTracking = pOn;

    //update the flags to reflect the change
    setFlags();

}//end of UTGate::setInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::linkPlotters
//
// This function is called by Plotters (Traces, etc.) to link their buffers to
// specific hardware channels/gates and give a link back to variables in the
// Plotter object.
//
// The values are only stored if the pChartGroup, pChart, and pTrace parameters
// match those loaded for this gate from the config file.
//

public void linkPlotters(int pChartGroup, int pChart, int pTrace,
        TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
                                                                Trace pTracePtr)
{

    if (pChartGroup == chartGroup && pChart == chart && pTrace == trace){

        //store the buffer references in the specified channel to link the trace
        traceData = pTraceData;

        thresholds = pThresholds;

        plotStyle = pPlotStyle;

        tracePtr = pTracePtr;

    }

}//end of UTGate::linkPlotters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::configure
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

    setInterfaceGate(
                pConfigFile.readBoolean(whichGate, "Interface Gate", false));
    setWallStart(pConfigFile.readBoolean(whichGate, "Wall Start Gate", false));
    setWallEnd(pConfigFile.readBoolean(whichGate, "Wall End Gate", false));
    setFlawGate(pConfigFile.readBoolean(whichGate, "Flaw Gate", false));

    modifyWall = pConfigFile.readBoolean(whichGate, "Modify Wall", false);

    triggerDirection = pConfigFile.readInt(whichGate, "Trigger Direction", 0);

    peakDirection = pConfigFile.readInt(whichGate, "Peak Direction", 0);
    setMaxMin((peakDirection == MAX));

    chartGroup = pConfigFile.readInt(whichGate, "Chart Group", 0) - 1;

    chart = pConfigFile.readInt(whichGate, "Chart", 0) - 1;

    trace = pConfigFile.readInt(whichGate, "Trace", 0) -1;

}//end of UTGate::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

    String section = "Channel " + (channelIndex + 1) + " Gate "
                                                            + (gateIndex + 1);

    gateStart.setValue(pCalFile.readDouble(section, "Gate Start", 50), true);
    gateStartTrackingOn = pCalFile.readDouble(section,
                    "Gate Start with Interface Tracking", 50);
    gateStartTrackingOff = pCalFile.readDouble(section,
                "Gate Start without Interface Tracking", 50);
    gateWidth.setValue(pCalFile.readDouble(section, "Gate Width", 2), true);
    gateLevel.setValue(pCalFile.readInt(section, "Gate Level", 15), true);
    gateHitCount.setValue(pCalFile.readInt(section, "Gate Hit Count", 0), true);
    gateMissCount.setValue(
                        pCalFile.readInt(section, "Gate Miss Count", 0), true);

    sigProcTuning1.setValue(
        pCalFile.readInt(section, "Signal Processing Tuning Value 1", 0), true);

    sigProcTuning2.setValue(
        pCalFile.readInt(section, "Signal Processing Tuning Value 2", 0), true);
    
    sigProcTuning3.setValue(
        pCalFile.readInt(section, "Signal Processing Tuning Value 3", 0), true);

    setSignalProcessing(
       pCalFile.readString(section, "Signal Processing Function", "undefined"));

}//end of UTGate::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

    String section = "Channel " + (channelIndex + 1) + " Gate " +
                                                                (gateIndex + 1);

    pCalFile.writeDouble(section, "Gate Start", gateStart.getValue());
    pCalFile.writeDouble(section,
                    "Gate Start with Interface Tracking",  gateStartTrackingOn);
    pCalFile.writeDouble(section,
                "Gate Start without Interface Tracking",  gateStartTrackingOff);
    pCalFile.writeDouble(section, "Gate Width", gateWidth.getValue());
    pCalFile.writeInt(section, "Gate Level", gateLevel.getValue());

    pCalFile.writeInt(section, "Gate Hit Count", gateHitCount.getValue());
    pCalFile.writeInt(section, "Gate Miss Count", gateMissCount.getValue());

    pCalFile.writeInt(section,
                "Signal Processing Tuning Value 1", sigProcTuning1.getValue());
    pCalFile.writeInt(section,
                "Signal Processing Tuning Value 2", sigProcTuning2.getValue());
    pCalFile.writeInt(section,
                "Signal Processing Tuning Value 3", sigProcTuning3.getValue());
    
    pCalFile.writeString(section, "Signal Processing Function",
                                                        getSignalProcessing());

}//end of UTGate::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTGate::saveCalFileHumanReadable
//
// This saves a subset of the calibration data, the values of which affect
// the inspection process.
//
// The data is saved in a human readable format.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFileHumanReadable(BufferedWriter pOut) throws IOException
{

    pOut.write("    " + Settings.postPad(title, 11));
    pOut.write(Settings.prePad(
                        decimalFormats[3].format(gateStart.getValue()), 8));
    pOut.write(Settings.prePad(
                        decimalFormats[3].format(gateWidth.getValue()), 8));
    pOut.write(Settings.prePad("" + gateLevel.getValue(), 8));
    pOut.write(Settings.prePad("" + gateHitCount.getValue(), 7));
    pOut.write(Settings.prePad("" + gateMissCount.getValue(), 9));
    //pad/truncate to no more or less than 30
    String sigProc = Settings.postPad(getSignalProcessing(), 30);
    sigProc = Settings.truncate(sigProc, 30);
    pOut.write("   " + sigProc);
    
    pOut.write(Settings.prePad("" + sigProcTuning1.getValue(), 12));
    pOut.write(Settings.prePad("" + sigProcTuning2.getValue(), 12));
    pOut.write(Settings.prePad("" + sigProcTuning3.getValue(), 12));    

    pOut.newLine();

}//end of UTGate::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

}//end of class UTGate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
