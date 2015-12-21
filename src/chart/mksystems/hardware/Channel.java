/******************************************************************************
* Title: Channel.java
* Author: Mike Schoonover
* Date: 4/26/09
*
* Purpose:
*
* This class handles an input channel.
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
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import chart.mksystems.threadsafe.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.ListIterator;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Channel
//
// This class handles an input channel.
//

public class Channel extends Object{

    IniFile configFile;
    Settings settings;

    SyncedVariableSet syncedVarMgr;

    public boolean gateSigProcThresholdChanged;

    SyncedShortInt dspControlFlags;

    int chassisAddr, slotAddr, boardChannel;

    DecimalFormat[] decimalFormats;

    public UTBoard utBoard;

    public int channelIndex;
    public UTGate[] gates;
    public int numberOfGates;

    public DACGate[] dacGates;
    public int numberOfDACGates;

    public boolean freezeScopeWhenNotInFocus;

    AnalogOutputController analogOutputController;
    int analogOutputControllerChannel;
    
    int scopeMax = 350;

    //used by the calibration window to store reference to the channel selectors
    //and their accompanying copy buttons
    public Object calRadioButton, copyButton;

    public String title, shortTitle, detail, type;

    boolean channelOn;  //true: pulsed/displayed, off: not pulsed/displayed
    boolean channelMasked; //true: pulsed/not display, off: pulsed/displayed

    boolean enabled; //overrides mode -- channel always off if false
    SyncedInteger mode;
    public int previousMode;
    boolean interfaceTracking = false;
    boolean dacEnabled = false, aScanSlowEnabled = false;
    boolean aScanFastEnabled = false, aScanFreeRun = true;
    double aScanDelay = 0;
    public SyncedInteger hardwareDelayFPGA, hardwareDelayDSP;
    public SyncedInteger softwareDelay;
    public int delayPix = 0;
    double aScanRange = 0;
    public SyncedInteger hardwareRange;
    public SyncedInteger aScanScale;
    SyncedDouble softwareGain;
    SyncedInteger hardwareGain1, hardwareGain2;
    SyncedIntArray filter;
    int rejectLevel;
    SyncedInteger aScanSmoothing;
    int dcOffset;
    public double nSPerDataPoint;
    public double uSPerDataPoint;
    public double nSPerPixel; //used by outside classes
    public double uSPerPixel; //used by outside classes
    String filterName = "";
    
    int firstGateEdgePos, lastGateEdgePos;
    boolean isWallChannel = false;

    int pulseChannel, pulseBank;

    private String dataVersion = "1.0";
    private FileInputStream fileInputStream = null;
    private InputStreamReader inputStreamReader = null;
    private BufferedReader in = null;

    static final int MAX_NUM_COEFFICIENTS = 31;
    
//-----------------------------------------------------------------------------
// Channel::Channel (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
// The constructing class should pass a pointer to a SyncedVariableSet for the
// values in this class which can be changed by the user and are sent to the
// remotes so that they will be managed in a threadsafe manner.
//

public Channel(IniFile pConfigFile, Settings pSettings, int pChannelIndex,
                                              SyncedVariableSet pSyncedVarMgr)
{

    configFile = pConfigFile; settings = pSettings;
    channelIndex = pChannelIndex;

    //if a SyncedVariableSet manager is provided use it, if not then create one

    if (pSyncedVarMgr != null) {
        syncedVarMgr = pSyncedVarMgr;
    }
    else {
        syncedVarMgr = new SyncedVariableSet();
    }

    softwareGain = new SyncedDouble(syncedVarMgr); softwareGain.init();
    hardwareGain1 = new SyncedInteger(syncedVarMgr); hardwareGain1.init();
    hardwareGain2 = new SyncedInteger(syncedVarMgr); hardwareGain2.init();
    aScanSmoothing = new SyncedInteger(syncedVarMgr); aScanSmoothing.init();
    dspControlFlags = new SyncedShortInt(syncedVarMgr); dspControlFlags.init();
    mode = new SyncedInteger(syncedVarMgr); mode.init();
    mode.setValue((int)UTBoard.POSITIVE_HALF, true);
    filter = new SyncedIntArray(syncedVarMgr); filter.init();
    hardwareDelayFPGA =
                new SyncedInteger(syncedVarMgr); hardwareDelayFPGA.init();
    hardwareDelayDSP =
                new SyncedInteger(syncedVarMgr); hardwareDelayDSP.init();
    softwareDelay = new SyncedInteger(syncedVarMgr); softwareDelay.init();
    hardwareRange = new SyncedInteger(syncedVarMgr); hardwareRange.init();
    aScanScale = new SyncedInteger(syncedVarMgr); aScanScale.init();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

}//end of Channel::Channel (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel:initialize
//
// Prepares the channel for operation.
//

public void initialize()
{

    //create various decimal formats
    decimalFormats = new DecimalFormat[4];
    decimalFormats[0] = new  DecimalFormat("0000000");
    decimalFormats[1] = new  DecimalFormat("0.0");
    decimalFormats[2] = new  DecimalFormat("0.00");
    decimalFormats[3] = new  DecimalFormat("0.000");

    //give the utBoard a link to the gates array
    if (utBoard != null){utBoard.linkGates(boardChannel, gates, numberOfGates);}

    //set FPGA values such as sample start delay, sample size by calling each
    //function

    //always set range after setting gate parameters, delay, and interface
    //tracking as these affect the range

    // use this flag to check if any gate is an interface gate

    boolean interfaceGatePresent = false;

    //detect if both a start and end gate have been set - only if both are found
    //is the channel set up for wall as this is the same test the DSPs use
    boolean wallStartGateSet = false, wallEndGateSet = false;

    //flag if interface and wall gates are present
    for (int i = 0; i < numberOfGates; i++){
        if (gates[i].getInterfaceGate()) {interfaceGatePresent = true;}
        if (gates[i].isWallStartGate) {wallStartGateSet = true;}
        if (gates[i].isWallEndGate) {wallEndGateSet = true;}
    }

    //if both wall start and end gates, set channel for wall data and
    //that data will be appended to peak data packets from the DSPs

    if (wallStartGateSet && wallEndGateSet){
        isWallChannel = true;
        if (utBoard != null) {
            utBoard.setWallChannelFlag(boardChannel, isWallChannel);
        }
    }

    //force interface tracking to false if no interface gate was set up
    //if no interface gate is present, the interface tracking checkbox will not
    //be displayed and the user cannot turn it off in case it was stored as on
    //in the job file
    //NOTE: If some but not all channels in a system have interface tracking,
    // using "Copy to All" will copy the interface tracking setting to all
    // channels. This will disable the channels without an interface gate
    // the progam is restarted and this piece of code gets executed.

    if (!interfaceGatePresent) {interfaceTracking = false;}

    //set bits in dspControlFlags

    short lDSPControlFlags = 0;

    lDSPControlFlags |= UTBoard.GATES_ENABLED;
    if (dacEnabled) {lDSPControlFlags |= UTBoard.DAC_ENABLED;}
    lDSPControlFlags |= UTBoard.ASCAN_FREE_RUN;

    if (utBoard != null && utBoard.getType() == UTBoard.RABBIT_FLAW_WALL_MODE){
        lDSPControlFlags |= UTBoard.DSP_FLAW_WALL_MODE;
    }
    else if (
       utBoard != null && utBoard.getType() == UTBoard.RABBIT_WALL_MAP_MODE){
        lDSPControlFlags |= UTBoard.DSP_WALL_MAP_MODE;
    }

    dspControlFlags.setValue(lDSPControlFlags, true);

    //setup various things
    setAScanSmoothing(aScanSmoothing.getValue(), true);
    setRejectLevel(rejectLevel, true);
    setDCOffset(dcOffset, true);
    setMode(mode.getValue(), true);  //setMode also calls setTransducer
    setInterfaceTracking(interfaceTracking, true);
    setDelay(aScanDelay, true);

    //setRange calculates based upon some of the settings above so do last
    setRange(aScanRange, true);

    //send all the changes made above to the remotes
    sendDataChangesToRemotes();

    //enable sample processing after all values initialized
    lDSPControlFlags |= UTBoard.PROCESSING_ENABLED;
    dspControlFlags.setValue(lDSPControlFlags, true);
    //send new flag value to remotes
    sendDataChangesToRemotes();

}//end of Channel::initialize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAnalogOutputController
//
// Sets the analog output controller and channel for this UT channel.
// The values reported for this channel will be output on the device.
//

public void setAnalogOutputController(
                                AnalogOutputController pAnalogOutputController,
                                int pOutputChannel)
{

    if(utBoard == null){ return; }
    
    analogOutputController = pAnalogOutputController;
    analogOutputControllerChannel = pOutputChannel;
    
    utBoard.setAnalogOutputController(boardChannel, pAnalogOutputController,
                                                                pOutputChannel);
    
}//end of Channel::setAnalogOutputController
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isEnabled
//
// Returns true if the channel is enabled, false otherwise.
//

public boolean isEnabled()
{

    return(enabled);

}//end of Channel::isEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setMasked
//
// Sets the channelMasked flag.
//

public void setMasked(boolean pMasked)
{

    channelMasked = pMasked;

}//end of Channel::setMasked
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getNumberOfDACGates
//
// Returns the number of DAC gates.
//

public int getNumberOfDACGates()
{

    return numberOfDACGates;

}//end of Channel::getNumberOfDACGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGate
//
// Returns a reference to the specified DAC gate.
//

public DACGate getDACGate(int pGate)
{

    return dacGates[pGate];

}//end of Channel::getDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getPointedDACGate
//
// Returns the DAC gate for which pX,pY is within 5 pixels of the adjusted
// start location.  The adjusted location is used because it takes into
// account shifting to track the interface if tracking is enabled.
//
// If no gate satisfies this criteria, -1 is returned.
//

public int getPointedDACGate(int pX, int pY)
{

    int i;

    //scan all gates for start which is near pX,pY
    for (i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].getActive()) {
            if (pX >= (dacGates[i].gatePixStartAdjusted - 5)
                    && pX <= (dacGates[i].gatePixStartAdjusted + 5)
                    && pY >= (dacGates[i].gatePixLevel - 5)
                    && pY <= (dacGates[i].gatePixLevel + 5)) {
                break;
            }
        }
    }

    if (i == numberOfDACGates) {i = -1;} //no match found

    return i;

}//end of Channel::getPointedDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getSelectedDACGate
//
// Returns the index of the first DAC gate which has a selectedFlag set true.
//
// If no gate satisfies this criteria, -1 is returned.
//

public int getSelectedDACGate()
{

    int i;

    //scan all gates to find the first selected one
    for (i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].getActive()) {
            if (dacGates[i].getSelected()) {break;}
        }
    }

    if (i == numberOfDACGates) {i = -1;} //no match found

    return i;

}//end of Channel::getSelectedDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setSelectedDACGate
//
// Sets the selectedFlag for the specified gate.  If pGate is invalid, does
// nothing.
//

public void setSelectedDACGate(int pGate, boolean pState)
{

    if (pGate < 0 || pGate >= numberOfDACGates) {return;}

    dacGates[pGate].setSelected(pState);

}//end of Channel::setSelectedDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getActiveDACGateCount
//
// Returns the number of gates which have been set active and thus are in use.
//

public int getActiveDACGateCount()
{

    int c = 0;

    //scan all gates to count the active ones
    for (int i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].getActive()) {c++;}
    }

    return c;

}//end of Channel::getActiveDACGateCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::insertDACGate
//
// Inserts a new DAC gate with the specified values into the appropriate
// place in the DAC gate array.
//
// If no gates have yet been set, insertion will be at gate 0.
// If location is before first gate, gates will be shifted to make space at 0.
// If location is after the last gate, insertion will occur at the end.
// If location is encompassed by an existing gate, the gates after that one
//   will be shifted to make space for insertion.
// If all gates are already taken, no insertion will be made.
//
// Gates inserted at the end will be given width of 35.
// Gates inserted at the beginning will have width from the pStart value to
//   the beginning of the first gate.
// Gates inserted between gates will have a width from the pStart value to the
//  end of the gate which encompasses the new location.  That gate will have
//  its width shortened by that amount to make room for the new gate.
//
// The start location of the gate will be adjusted to match the end location
// of the previous gate.
//

public void insertDACGate(int pStart, int pLevel)
{

    //do not allow insertion if gate array is full
    if (getActiveDACGateCount() == numberOfDACGates) {return;}

    int lastGate = getActiveDACGateCount() - 1;

    //if this is the first gate to be set, insert at gate 0
    if (getActiveDACGateCount() == 0){
        setDACGatePixelValues(0, pStart, pStart+35, pLevel, true, false);
        return;
    }

    //if the new gate is located before the first gate, then insert at the
    //beginning
    if (pStart < dacGates[0].gatePixStart){
        int pEnd = dacGates[0].gatePixStart; //ends where first gate starts
        shiftDACGatesUp(0); //shift gates to make room
        setDACGatePixelValues(0, pStart, pEnd, pLevel, true, false);
        return;
    }

    //if the new gate is located past the last gate, then insert at the end
    if (pStart > dacGates[lastGate].gatePixEnd){
        pStart = dacGates[lastGate].gatePixEnd;
        setDACGatePixelValues(
                        lastGate+1, pStart, pStart+35, pLevel, true, false);
        return;
    }

    //if the new gate's location is encompassed by an existing gate, shift
    //all later gates down and insert the new gate, adjusting the width of the
    //old gate to give space for the new gate

    int i;
    for (i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].getActive()) {
            if (pStart >= dacGates[i].gatePixStart
                                           && pStart <= dacGates[i].gatePixEnd) {
                break;
            }
        }
    }

    //the end of the new gate will equal the start of the following gate
    //if there is no following gate, then the end will be the end of old gate
    int pEnd;

    if (i < lastGate) {
        pEnd = dacGates[i+1].gatePixStart;
    }
    else {
        pEnd = dacGates[i].gatePixEnd;
    }

    shiftDACGatesUp(i+1); //shift gates to make room

    //adjust the end of the old gate to match the start of the new gate
    setDACPixEnd(i, pStart, false);

    //set up the new gate
    setDACGatePixelValues(i+1, pStart, pEnd, pLevel, true, false);

}//end of Channel::insertDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::deleteDACGate
//
// Deletes the DAC gate specified by pGate.
//

public void deleteDACGate(int pGate)
{

    if (pGate < 0 || pGate >= numberOfDACGates) {return;}

    int lastGate = getActiveDACGateCount() - 1; //need this in a second

    //shift all gates above the one being deleted down one slot
    shiftDACGatesDown(pGate+1);

    //disable the old last gate slot - that one has been moved down one slot
    setDACActive(lastGate, false, false);

    //set the end of the gate before the deleted one to the start of the gate
    //which was after the deleted one to avoid diagonal lines
    //don't do this if the gates involved are at the ends of the array

    if (pGate > 0 && pGate < getActiveDACGateCount()) {
        setDACPixEnd(pGate - 1, dacGates[pGate].gatePixStart, false);
    }

}//end of Channel::deleteDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::deleteAllDACGates
//
// Deletes all the DAC gates by setting their active states to false.
//

public void deleteAllDACGates()
{

    for (int i = 0; i < numberOfDACGates; i++) {setDACActive(i, false, false);}

}//end of Channel::deleteAllDACGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::shiftDACGatesDown
//
// Shifts all gates beginning with index pStart down one slot.
//

public void shiftDACGatesDown(int pStart)
{

    int newFirstGate = pStart - 1;
    if (newFirstGate < 0) {return;} //protect against shifting out of bounds

    int stop = getActiveDACGateCount() - 1;

    //copy gates to shift them
    for (int i = newFirstGate; i < stop; i++){
        setDACPixStart(i, dacGates[i+1].gatePixStart, false);
        setDACPixEnd(i, dacGates[i+1].gatePixEnd, false);
        setDACPixLevel(i, dacGates[i+1].gatePixLevel, false);
        setDACActive(i, dacGates[i+1].getActive(), false);
        dacGates[i].setSelected(dacGates[i+1].getSelected());
    }

}//end of Channel::shiftDACGatesDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::shiftDACGatesUp
//
// Shifts all gates beginning with index pStart up one slot.
//

public void shiftDACGatesUp(int pStart)
{

    int newLastGate = getActiveDACGateCount();
    if (newLastGate >= numberOfDACGates) {return;} //protect against full array

    //copy gates to shift them
    for (int i = newLastGate; i > pStart; i--){
        setDACPixStart(i, dacGates[i-1].gatePixStart, false);
        setDACPixEnd(i, dacGates[i-1].gatePixEnd, false);
        setDACPixLevel(i, dacGates[i-1].gatePixLevel, false);
        setDACActive(i, dacGates[i-1].getActive(), false);
        dacGates[i].setSelected(dacGates[i-1].getSelected());
    }

}//end of Channel::shiftDACGatesUp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACGatePixelValues
//
// Sets the pixel location values and active flag of the specified DAC gate.
//

public void setDACGatePixelValues(int pGate, int pStart, int pEnd,
                                 int pLevel, boolean pActive, boolean pSelected)
{

    setDACPixStart(pGate, pStart, false);
    setDACPixEnd(pGate, pEnd, false);
    setDACPixLevel(pGate, pLevel, false);
    setDACActive(pGate, pActive, false);

    dacGates[pGate].setSelected(pSelected);

}//end of Channel::setDACGatePixelValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACPixStart
//
// Sets the DAC gate start pixel position of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDACPixStart(int pGate, int pStart, boolean pForceUpdate)
{

    if (pStart != dacGates[pGate].gatePixStart) {pForceUpdate = true;}

    if (pForceUpdate) {dacGates[pGate].gatePixStart = pStart;}

}//end of Channel::setDACPixStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACPixEnd
//
// Sets the DAC gate end pixel position of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDACPixEnd(int pGate, int pEnd, boolean pForceUpdate)
{

    if (pEnd != dacGates[pGate].gatePixEnd) {pForceUpdate = true;}

    if (pForceUpdate) {dacGates[pGate].gatePixEnd = pEnd;}

}//end of Channel::setDACPixEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACPixLevel
//
// Sets the gate level pixel position of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDACPixLevel(int pGate, int pLevel, boolean pForceUpdate)
{

    if (pLevel != dacGates[pGate].gatePixLevel) {pForceUpdate = true;}

    if (pForceUpdate) {dacGates[pGate].gatePixLevel = pLevel;}

}//end of Channel::setDACPixLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getNumberOfGates
//
// Returns the number of gates.
//

public int getNumberOfGates()
{

    return numberOfGates;

}//end of Channel::getNumberOfGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGate
//
// Returns a reference to the specified gate.
//

public UTGate getGate(int pGate)
{

    return gates[pGate];

}//end of Channel::getGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getNewData
//
// Calls the getNewData function for the specified gate.  See the gate
// class for more info.
//
// Returns true if the channel On and Not Masked.  Returns false otherwise and
// data which will always be overridden by other channels which are active.
//
// Thus, for an inactive gate for which higher values are the peaks, the data
// returned will be a minimum value and vice versa.
//

public boolean getNewData(int pGate, HardwareVars hdwVs)
{

    if (channelOn && !channelMasked){
        gates[pGate].getNewData(hdwVs);
        return(true);
    }
    else{
        gates[pGate].getInactiveData(hdwVs);
        return(false);
    }

}//end of Channel::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getPeakGate
//
// Returns the index of the gate with the current highest stored peak value.
//
// NOTE: This method assumes that all the gates are max peak catching.
//

public int getPeakGate()
{

    int data, peak = Integer.MIN_VALUE;
    int peakGate = -1;
    
    for(int i=0; i<gates.length; i++){        
        data = gates[i].getCurrentDataPeak();
        if (data >= peak){
            peak = data; peakGate = i;
        }
    }
    
    return(peakGate);
    
}//end of Channel::getPeakGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getTrace
//
// Calls the getTrace function for the specified gate.  See the gate
// class for more info.
//
//

public Trace getTrace(int pGate)
{

    return gates[pGate].getTrace();

}//end of Channel::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDSPControlFlags
//
// Sends the dspControlFlags to the remotes.
//

public void sendDSPControlFlags()
{

    if (utBoard != null) {
        utBoard.sendDSPControlFlags(boardChannel, dspControlFlags.applyValue());
    }

}//end of Channel::sendDSPControlFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACEnabled
//
// Sets appropriate mask word to set or clear the DAC_ENABLED bit in the DSP's
// flags1 variable.
//
// NOTE: A delay should be inserted between calls to this function and setFlags1
// and clearFlags1 as they are actually sent to the remotes by another thread.
// The delay should be long enough to ensure that the other thread has had time
// to send the first command.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDACEnabled(boolean pEnable, boolean pForceUpdate)
{

    if (pEnable != dacEnabled) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    dacEnabled = pEnable;

    if (dacEnabled) {
        dspControlFlags.setValue(
         (short)(dspControlFlags.getValue() | (short)UTBoard.DAC_ENABLED),
         pForceUpdate);
    }
    else {
        dspControlFlags.setValue(
         (short)(dspControlFlags.getValue() & (short)~UTBoard.DAC_ENABLED),
         pForceUpdate);
    }

}//end of Channel::setDACEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACEnabled
//
// Returns the dacEnabled flag.
//

public boolean getDACEnabled()
{

    return dacEnabled;

}//end of Channel::getDACEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAScanFastEnabled
//
// Sets appropriate mask word to set or clear the ASCAN_FAST_ENABLED bit in the
// DSP's flags1 variable.  This allows Ascan data to be retrieved.
//
// NOTE: The fast AScan function in the DSP takes too much time and will
// generally cause datasets to be skipped because it processes the entire AScan
// buffer in one chunk -- effectively lowering the rep rate.  The function
// should ONLY be enabled when using an AScan display for setup.
//
// NOTE: See setAScanSlowEnabled for a version which can be used during
// inspection mode because it processes the AScan buffer a piece at a time.
//
// NOTE: A delay should be inserted between calls to this function and setFlags1
// and clearFlags1 as they are actually sent to the remotes by another thread.
// The delay should be long enough to ensure that the other thread has had time
// to send the first command.
//
// WARNING: the fast AScan and slow AScan options should never be enabled
// at the same time.  They share some of the same variables in the DSP.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setAScanFastEnabled(boolean pEnable,
                                                           boolean pForceUpdate)
{

    if (pEnable != aScanFastEnabled) {pForceUpdate = true;}

    aScanFastEnabled = pEnable;

    //flag that new data needs to be sent to remotes
    if (pForceUpdate){
        if (aScanFastEnabled){
            dspControlFlags.setValue(
             (short)(dspControlFlags.getValue() |
                            (short)UTBoard.ASCAN_FAST_ENABLED), pForceUpdate);
        }
        else{
            dspControlFlags.setValue(
             (short)(dspControlFlags.getValue() &
                            (short)~UTBoard.ASCAN_FAST_ENABLED), pForceUpdate);
        }
    }//if (pForceUpdate)

}//end of Channel::setAScanFastEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScanFastEnabled
//
// Returns the aScanFastEnabled flag.
//

public boolean getAScanFastEnabled()
{

    return aScanFastEnabled;

}//end of Channel::getAScanFastEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAScanSlowEnabled
//
// Sets appropriate mask word to set or clear the ASCAN_SLOW_ENABLED bit in the
// DSP's flags1 variable.  This allows Ascan data to be retrieved.
//
// NOTE: This function can be enabled during inspection because it processes
// only a small chunk of the AScan buffer during each pulse cycle.
//
// NOTE: See setAScanFastEnabled for a version which might give faster visual
// response but can only be used during calibration as it might cause data
// peaks to be missed occasionally.
//
// NOTE: A delay should be inserted between calls to this function and setFlags1
// and clearFlags1 as they are actually sent to the remotes by another thread.
// The delay should be long enough to ensure that the other thread has had time
// to send the first command.
//
// WARNING: the fast AScan and slow AScan options should never be enabled
// at the same time.  They share some of the same variables in the DSP.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setAScanSlowEnabled(boolean pEnable,
                                                           boolean pForceUpdate)
{

    if (pEnable != aScanSlowEnabled) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    aScanSlowEnabled = pEnable;

    if (aScanSlowEnabled) {
        dspControlFlags.setValue((short)(dspControlFlags.getValue() |
                             (short)UTBoard.ASCAN_SLOW_ENABLED), pForceUpdate);
    }
    else {
        dspControlFlags.setValue((short)(dspControlFlags.getValue() &
                            (short)~UTBoard.ASCAN_SLOW_ENABLED), pForceUpdate);
    }

}//end of Channel::setAScanSlowEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScanSlowEnabled
//
// Returns the aScanSlowEnabled flag.
//

public boolean getAScanSlowEnabled()
{

    return aScanSlowEnabled;

}//end of Channel::getAScanSlowEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAScanFreeRun
//
// Sets appropriate mask word to set or clear the ASCAN_FREE_RUN bit in the
// DSP's flags1 variable.  This flag controls whether AScan data is saved
// immediately upon a request or if it is saved when an AScan trigger gate is
// exceeded.  The latter allows the AScan display to be synchronized with a
// signal peak in the gate.
//
// NOTE: A delay should be inserted between calls to this function and setFlags1
// and clearFlags1 as they are actually sent to the remotes by another thread.
// The delay should be long enough to ensure that the other thread has had time
// to send the first command.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setAScanFreeRun(boolean pEnable, boolean pForceUpdate)
{

    if (pEnable != aScanFreeRun) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    aScanFreeRun = pEnable;

    if (aScanFreeRun) {
        dspControlFlags.setValue((short)(dspControlFlags.getValue() |
                                 (short)UTBoard.ASCAN_FREE_RUN), pForceUpdate);
    }
    else {
        dspControlFlags.setValue((short)(dspControlFlags.getValue() &
                                (short)~UTBoard.ASCAN_FREE_RUN), pForceUpdate);
    }

}//end of Channel::setAScanFreeRun
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScanFreeRun
//
// Returns the aScanFreeRun flag.
//

public boolean getAScanFreeRun()
{

    return aScanFreeRun;

}//end of Channel::getAScanFreeRun
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::calculateGateSpan
//
// Records the position in sample counts of the leading edge of the first
// gate (first in time) and the trailing edge of the last gate (last in time).
// The span between these two values represents the number of samples which
// must be recorded by the FPGA in order to allow processing of data in those
// gates.  The leading point is stored in firstGateEdgePos while the trailing
// point is stored in lastGateEdgePos.
//
// If interface tracking is off, the calculation is made using the gate
// start positions and widths as set.
//
// If interface tracking is on, the gate positions are based on the position
// where the signal exceeds the interface gate.  This is often not known, so
// the worst possible case is computed.  If the gate position is negative
// (gate is located before the interface crossing), then that gate's position
// is calculated from the leading edge of the interface gate.  This is the
// worst case as that is the earliest time the signal could cross the interface
// gate.  If the gate position is positive (gate is located after the
// interface crossing), the gate's position is calculated from the trailing
// edge of the interface gate as that is the worst case.
//
// NOTE: Call this function any time a gate position or width is modified.
//

void calculateGateSpan()
{

    //calculate the gate positions as absolutes - see notes at top of function
    //or as worst case distances from the interface gate

    if (!interfaceTracking){

        //find the earliest gate

        int firstGate = 0;
        for (int i = 0; i < numberOfGates; i++) {
            if (gates[i].gateStart.getValue()
                                        < gates[firstGate].gateStart.getValue()) {
                firstGate = i;
            }
        }

        //find the latest gate

        int lastGate = 0;
        for (int i = 0; i < numberOfGates; i++) {
            if ((gates[i].gateStart.getValue() + gates[i].gateWidth.getValue())
                            > (gates[lastGate].gateStart.getValue()
                                       + gates[lastGate].gateWidth.getValue())){
                lastGate = i;
            }
        }

        //absolute positioning

        //calculate the position (in samples) of the leading edge of first gate
        firstGateEdgePos =
                 (int)(gates[firstGate].gateStart.getValue() / uSPerDataPoint);

        //calculate the position in number of samples of trailing edge of
        //last gate
        lastGateEdgePos = (int)((gates[lastGate].gateStart.getValue()
                    + gates[lastGate].gateWidth.getValue())  / uSPerDataPoint);

    }// if (!interfaceTracking)
    else{

        int interfaceGateLead =
                        (int)(gates[0].gateStart.getValue() / uSPerDataPoint);
        int interfaceGateTrail = (int)((gates[0].gateStart.getValue() +
                             gates[0].gateWidth.getValue()) / uSPerDataPoint);

        //if there is only one gate, it must be the interface gate so use its
        //position and return
        if (numberOfGates == 1){
            firstGateEdgePos = interfaceGateLead;
            lastGateEdgePos = interfaceGateTrail;
            return;
        }

        //find the earliest gate, not including the interface gate
        //the interface gate always uses absolute positioning whereas the other
        //gates will be relative to the interface crossing if tracking is on

        int firstGate = 1;
        for (int i = 1; i < numberOfGates; i++) {
            if (gates[i].gateStart.getValue() <
                                        gates[firstGate].gateStart.getValue()) {
                firstGate = i;
            }
        }

        //find the latest gate, not including the interface gate (see note above)

        int lastGate = 1;
        for (int i = 1; i < numberOfGates; i++) {
            if ((gates[i].gateStart.getValue() + gates[i].gateWidth.getValue())
                           > (gates[lastGate].gateStart.getValue()
                                     + gates[lastGate].gateWidth.getValue())) {
                lastGate = i;
            }
        }

        //positioning relative to interface gate
        //interface gate is always gate 0

        //calculate the position (in samples) of the leading edge of first gate
        //relative to the leading edge of interface gate (worst case)
        firstGateEdgePos = (int)((gates[0].gateStart.getValue()
                + gates[firstGate].gateStart.getValue()) / uSPerDataPoint);

        //if the interface gate is before all other gates, use its position
        if (interfaceGateLead < firstGateEdgePos) {
            firstGateEdgePos = interfaceGateLead;
        }

        //calculate the position in number of samples of trailing edge of last
        //gate relative to the trailing edge of interface gate (worst case)
        lastGateEdgePos = (int)(
                (gates[0].gateStart.getValue() + gates[0].gateWidth.getValue() +
                gates[lastGate].gateStart.getValue()
                + gates[lastGate].gateWidth.getValue()) / uSPerDataPoint);

        //if the interface gate is after all other gates, use its position
        if (interfaceGateTrail > lastGateEdgePos) {
            lastGateEdgePos = interfaceGateTrail;
        }

    }// else of if (!interfaceTracking)

}//end of Channel::calculateGateSpan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDelay
//
// Sets the delay for the AScan.  This is the amount of time skipped between
// the initial pulse and the start of the data collection.
//
// Note that the delay sent to the FPGA must not delay past the start of the
// earliest gate. Data still needs to be collected for all gates even if the
// user is not viewing that portion of the data.
//
// Thus the delay for the AScan display is comprised of two parts - the delay
// already introduced by the FPGA skipping a specified number of samples after
// the initial pulse and the DSP skipping a specified number of samples when
// it transmits the AScan data set.  This is because the FPGA must start
// recording data at the start of the earliest gate while the AScan may be
// displaying data from a point later than that.
//
// The delay in the FPGA before samples are stored is hardwareDelay while
// the delay in the DSP software for the positioning of the start of the AScan
// data is softwareDelay.  The AScan delay equals hardware delay plus
// software delay.
//
// The hardwareDelay and softwareDelay values represent one sample period for
// each count.  For a 66.666 MHz sample rate, this equates to 15nS per count.
//
// NOTE: This function must be called anytime a gate's position is changed so
// that the sampling start can be adjusted to include the entire gate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDelay(double pDelay, boolean pForceUpdate)
{

    int newHardwareDelay, newSoftwareDelay;

    aScanDelay = pDelay;

    //calculate the number of samples to skip based on the delay in microseconds
    newHardwareDelay = (int)(aScanDelay / uSPerDataPoint);
    int totalDelayCount = newHardwareDelay;

    //the FPGA sample delay CANNOT be later than the delay to the earliest gate
    //because the gate cannot be processed if samples aren't taken for it
    //if the earliest gate is sooner than the delay, then override the FPGA
    //delay - the remaining delay for an AScan will be accounted for by setting
    //a the aScanDelay in the DSP

    if (firstGateEdgePos < newHardwareDelay) {
        newHardwareDelay = firstGateEdgePos;
    }

    //one copy is sent to the FPGA, the other to the DSP
    //separate objects used for easy thread synchronizing
    hardwareDelayFPGA.setValue(newHardwareDelay, pForceUpdate);
    hardwareDelayDSP.setValue(newHardwareDelay, pForceUpdate);

    //calculate and set the remaining delay left over required to position the
    //AScan correctly after taking into account the FPGA sample delay

    newSoftwareDelay = totalDelayCount - newHardwareDelay;

    softwareDelay.setValue(newSoftwareDelay, pForceUpdate);

}//end of Channel::setDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDelay
//
// Returns the AScan signal delay for the DSP.
//

public double getDelay()
{

    return aScanDelay;

}//end of Channel::getDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDelayInSamplePeriods
//
// Returns the delay time between the initial pulse and when the left edge
// of the AScan should start.  This is essentially the same value as aScanDelay,
// but that value is in uSec whereas the value returned by this function is the
// actual number number of samples which were skipped, each sample representing
// 15nS if using a 66.6666MHz sample clock.
//
// This value may be a bit more accurate than converting aScanDelay to sample
// periods.
//
// The returned value represents one sample period for each count.  For a
// 66.666 MHz sample rate, this equates to 15nS per count.
//
// The value is obtained by summing hardwareDelay and softwareDelay.  The
// hardwareDelay is the value fed into the FPGA to set the number of samples
// it ignores before it starts recording.  This sample start point begins at
// the left edge of the aScan OR EARLIER if a gate is set before the aScan
// starts (sampling must occur at the start of the earliest gate in order for
// the gate to be useful).  The software delay is used to delay the sample
// window used for displaying the aScan after the start of sampling.
//
// If the location of a gate or signal is known by the number of sample periods
// after the initial pulse, it can be positioned properly on the aScan display
// by subtracting the value returned returned by this function and scaling
// from sample periods to pixels.
//
// More often, the location is known from the start of the DSP sample buffer.
// In this case, only subtract the softwareDelay from the location -- use
// getSoftwareDelay instead fo this function.
//
// Example:
//
// int peakPixelLoc =
//        (int) ((flightTime * pChannel.nSPerDataPoint) / pChannel.nSPerPixel);
//

public int getDelayInSamplePeriods()
{

    return hardwareDelayFPGA.getValue() + softwareDelay.getValue();

}//end of Channel::getDelayInSamplePeriods
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getSoftwareDelay
//
// Returns the delay time between the initial pulse and the start of the DSP
// sample buffer.  Each count represents one sample (or 15nS) if using a
// 66.6666MHz sample clock.
//
// See notes for getDelayInSamplePeriods for more info.
//

public int getSoftwareDelay()
{

    return softwareDelay.getValue();

}//end of Channel::getSoftwareDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setRejectLevel
//
// Sets the reject level which is a percentage of the screen height below
// which the signal is attenuated nearly to zero.
//

public void setRejectLevel(int pRejectLevel, boolean pForceUpdate)
{

    if (pRejectLevel != rejectLevel) {pForceUpdate = true;}

    rejectLevel = pRejectLevel;

    if (utBoard != null && pForceUpdate) {
        utBoard.setRejectLevel(boardChannel, pRejectLevel);
    }

}//end of Channel::setRejectLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getRejectLevel
//
// Returns the reject level which is a percentage.
//

public int getRejectLevel()
{

    return rejectLevel;

}//end of Channel::getRejectLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAScanSmoothing
//
// Sets the amount of smoothing (averaging) for the aScan display.
//
// The value is also sent to the remotes to specify the amount of averaging
// to use for the data samples.  The AScan data is not averaged by the remotes,
// only the peak data.  Averaging (smoothing) of the AScan data is done in
// the host.  Thus, AScan data can be averaged, peak data can be averaged, or
// both together.
//

public void setAScanSmoothing(int pAScanSmoothing, boolean pForceUpdate)
{

    if (pAScanSmoothing != aScanSmoothing.getValue()) {pForceUpdate = true;}

    if (pForceUpdate){

        aScanSmoothing.setValue(pAScanSmoothing, pForceUpdate);

        //update all gates with new averaging value
        for (int i = 0; i < numberOfGates; i++) {
            gates[i].setAScanSmoothing(pAScanSmoothing);
        }

    }

}//end of Channel::setAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScanSmoothing
//
// Returns the amount of smoothing (averaging) for the aScan display.
//

public int getAScanSmoothing()
{

    return aScanSmoothing.getValue();

}//end of Channel::getAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendAScanSmoothing
//
// Sends the AScanSmoothing value to the remotes.
//

public void sendAScanSmoothing()
{

    if (utBoard != null) {
        utBoard.setAScanSmoothing(boardChannel, aScanSmoothing.applyValue());
    }

}//end of Channel::sendAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDCOffset
//
// Sets the DC offset.
//

public void setDCOffset(int pDCOffset, boolean pForceUpdate)
{

    if (pDCOffset != dcOffset) {pForceUpdate = true;}

    dcOffset = pDCOffset;

    //divide the input value by 4.857 mv/Count
    //AD converter span is 1.2 V, 256 counts
    //this will give a value of zero offset until input value is +/-5, then it
    //will equal 1 - the value will only change every 5 or so counts because the
    //AD resolution is 4+ mV and the input value is mV

    if (utBoard != null && pForceUpdate) {
        utBoard.sendDCOffset(boardChannel, (int)(dcOffset / 4.6875));
    }

}//end of Channel::setDCOffset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDCOffset
//
// Returns the DC offset value.
//

public int getDCOffset()
{

    return dcOffset;

}//end of Channel::getDCOffset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setMode
//
// Sets the signal mode setting: rectification style or channel off.
//
// Also sets the channelOn switch true if mode is not CHANNEL_OFF, false if
// it is.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setMode(int pMode, boolean pForceUpdate)
{

    //if the channel is disabled in the configuration file, mode is always off
    if (!enabled) {pMode = UTBoard.CHANNEL_OFF;}

    if (pMode != mode.getValue()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    mode.setValue(pMode, pForceUpdate);

    boolean lChannelOn = (mode.getValue() != UTBoard.CHANNEL_OFF);

    //update the tranducer settings as the on/off status is set that way
    setTransducer(lChannelOn, pulseBank, pulseChannel, pForceUpdate);

}//end of Channel::setMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendMode
//
// Sends the signal mode to the remote.
//

public void sendMode()
{

    if (utBoard != null) {utBoard.sendMode(boardChannel, mode.applyValue());}

}//end of Channel::sendMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getMode
//
// Returns the signal mode setting: rectification style or off.
//

public int getMode()
{

    return mode.getValue();

}//end of Channel::getMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendHardwareDelayToFPGA
//
// Sends the hardware delay used in the FPGA to the remote so it can be
// stored in the FPGA registers.
//

private void sendHardwareDelayToFPGA()
{

    if (utBoard != null) {
        utBoard.sendHardwareDelay(boardChannel, hardwareDelayFPGA.applyValue());
    }

}//end of Channel::sendHardwareDelayToFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendSoftwareDelay
//
// Sends the software delay to the DSP. A copy of the hardware delay being
// used in the FPGA is also sent so that the DSP can use it in calculations.
//

private void sendSoftwareDelay()
{

    if (utBoard != null) {
        utBoard.sendSoftwareDelay(boardChannel,
                    softwareDelay.applyValue(), hardwareDelayDSP.applyValue());
    }

}//end of Channel::sendSoftwareDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setRange
//
// Sets the range for the AScan. This is the amount of signal displayed in one
// Ascan.
//
// Note that the data collected amount must be enough to stretch from the
// start of the earliest gate to the end of the latest gate, even if the range
// being displayed on the scope is smaller than this.  Even if the user is
// looking at a smaller area, data must still be collected so that it can be
// processed for each gate. If the displayed AScan range is larger than the
// earliest and latest gates, the range must be set to encompass this as well.
//
// The number of samples stored by the FPGA to ensure coverage from the first
// point of interest (left edge of AScan or earliest gate) to the last point
// of interest (right edge of AScan or latest gate) is stored in hardwareRange.
// This value is the number of samples to be stored on each transducer firing.
//
// The DSP always returns 400 samples for an AScan dataset.  The aScanScale
// value tells the DSP how many samples to compress for each sample placed in
// the AScan buffer.  The DSP stores the peaks from the compressed samples
// and transfers those peaks to the AScan buffer.
//
// When displaying the AScan, the host takes into account the compression
// performed by the DSP (which is always an integer multiple for simplicity)
// and factors in a further fractional scaling to arrive at the desired
// range scale for the AScan.  The DSP's software range is always set to the
// smallest possible integer which will compress the desired number of samples
// (the range) into the 400 sample AScan buffer.  This program will then
// further shrink or stretch the 400 samples as needed to display the desired
// range.
//
// It is desirable to have more compression done by the DSP than is needed
// rather than less.  The AScan buffer can be stretched or squeezed as
// necessary if it is full of data - any extra simply won't be displayed.  For
// this to work, the range of samples to be viewed must be present in the
// AScan buffer even if they are over compressed.
//
// NOTE: On startup, always call setDelay before calling setRange as this
//    function uses data calculated in setDelay.  Thereafter each function only
//    needs to be called if its respective data is modified.
//
// NOTE: This function must be called anytime a gate's position or width is
// changed so that the sample size can be adjusted to include the entire gate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setRange(double pRange, boolean pForceUpdate)
{

    aScanRange = pRange;

    //calculate the position of the left edge of the AScan in sample counts
    int leftEdgeAScan = hardwareDelayFPGA.getValue() + softwareDelay.getValue();

    //calculate the position of the right edge of the AScan in sample counts

    int rightEdgeAScan = leftEdgeAScan + (int)(aScanRange / uSPerDataPoint);

    int start, stop;

    //determine the earliest event for which samples are required - the left
    //edge of the AScan or the leading edge of the first gate, which ever is
    //first

    if (leftEdgeAScan <= firstGateEdgePos) {
        start = leftEdgeAScan;
    }
    else {
        start = firstGateEdgePos;
    }

    //determine the latest event for which samples are required - the right edge
    //of the AScan or the trailing edge of the last gate, which ever is last

    if (rightEdgeAScan >= lastGateEdgePos) {
        stop = rightEdgeAScan;
    }
    else {
        stop = lastGateEdgePos;
    }

    //calculate the number of required samples to cover the range specified
    hardwareRange.setValue(stop - start, pForceUpdate);

    //force sample size to be even - see notes at top of
    //UTboard.setHardwareRange for more info
    if (hardwareRange.getValue() % 2 != 0) {
        hardwareRange.setValue(hardwareRange.getValue() + 1, pForceUpdate);
    }

    //calculate the compression needed to fit at least the desired number of
    //AScan samples into the 400 sample AScan buffer - the scale factor is
    //rounded up rather than down to make sure the desired range is collected

    aScanScale.setValue(
    (rightEdgeAScan - leftEdgeAScan) / UTBoard.ASCAN_SAMPLE_SIZE, pForceUpdate);

    //if the range is not a perfect integer, round it up
    if (((rightEdgeAScan - leftEdgeAScan) % UTBoard.ASCAN_SAMPLE_SIZE) != 0) {
        aScanScale.setValue(aScanScale.getValue() + 1, pForceUpdate);
    }

}//end of Channel::setRange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendRange
//
// Sends the necessary information regarding the range to the remotes.  Note
// that aScanRange itself is not sent, so it is not a threadsafe variable.
//
// The values which are calculated and sent are threadsafe.
//

private void sendRange()
{

    if (utBoard != null){

        //lock in the synced value since it is used twice here
        int lHardwareRange = hardwareRange.applyValue();

        utBoard.sendHardwareRange(boardChannel, lHardwareRange);

        //tell the DSP cores how big the sample set is
        utBoard.sendDSPSampleSize(boardChannel, lHardwareRange);

        utBoard.sendAScanScale(boardChannel, aScanScale.applyValue());

    }

}//end of Channel::sendRange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getRange
//
// Returns the signal range.  The range is how much of the signal is displayed.
//

public double getRange()
{

    return aScanRange;

}//end of Channel::getRange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setSampleBufferStart
//
// Sets the registers in the FPGA which hold the starting address in the
// DSP for the sample buffer.
//

public void setSampleBufferStart()
{

    if (utBoard != null) {
        utBoard.sendSampleBufferStart(boardChannel,
                                            UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);
    }

}//end of Channel::setSampleBufferStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setSoftwareGain
//
// Sets the software gain for the DSP.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setSoftwareGain(double pSoftwareGain, boolean pForceUpdate)
{

    if (pSoftwareGain != softwareGain.getValue()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    softwareGain.setValue(pSoftwareGain, pForceUpdate);

    for (int i = 0; i < numberOfDACGates; i++) {
        dacGates[i].setSoftwareGain(pSoftwareGain, pForceUpdate);
    }

}//end of Channel::setSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendSoftwareGain
//
// Sends the software gain to the DSP.
//

public void sendSoftwareGain()
{

    if (utBoard != null) {
        utBoard.sendSoftwareGain(boardChannel, softwareGain.applyValue());
    }

}//end of Channel::sendSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getSoftwareGain
//
// Returns the software gain for the DSP.
//

public double getSoftwareGain()
{

    return (softwareGain.getValue());

}//end of Channel::getSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setHardwareGain
//
// Sets the hardware gains for the DSP.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setHardwareGain(int pHardwareGain1, int pHardwareGain2,
                                                           boolean pForceUpdate)
{

    hardwareGain1.setValue(pHardwareGain1, pForceUpdate);
    hardwareGain2.setValue(pHardwareGain2, pForceUpdate);

}//end of Channel::setHardwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendHardwareGain
//
// Sends the hardware gain to the DSP.
//

public void sendHardwareGain()
{

    if (utBoard != null) {
        utBoard.sendHardwareGain(
         boardChannel, hardwareGain1.applyValue(), hardwareGain2.applyValue());
    }

}//end of Channel::sendHardwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getHardwareGain1
//
// Returns the hardware gain 1 for the DSP.
//

public int getHardwareGain1()
{

    return hardwareGain1.getValue();

}//end of Channel::getHardwareGain1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getHardwareGain2
//
// Returns the hardware gain 2 for the DSP.
//

public int getHardwareGain2()
{

    return hardwareGain2.getValue();

}//end of Channel::getHardwareGain2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setInterfaceTracking
//
// Sets the interface tracking on/off flag for pChannel in the DSPs.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setInterfaceTracking(boolean pState, boolean pForceUpdate)
{

    if (pState != interfaceTracking) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    interfaceTracking = pState;

    //switch gate start positions to the appropriate values for the current mode
    for (int i = 0; i < numberOfGates; i++) {
        setGateStart(i, interfaceTracking ?
           gates[i].gateStartTrackingOn : gates[i].gateStartTrackingOff, false);
    }

    //determine the span from the earliest gate edge to the latest (in time)
    calculateGateSpan();

    for (int i = 0; i < numberOfGates; i++) {
        gates[i].setInterfaceTracking(pState);
    }

    for (int i = 0; i < numberOfDACGates; i++) {
        dacGates[i].setInterfaceTracking(pState);
    }

}//end of Channel::setInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getInterfaceTracking
//
// Returns the getInterfaceTracking flag.
//

public boolean getInterfaceTracking()
{

    return interfaceTracking;

}//end of Channel::getInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAScanTrigger
//
// Sets the AScan trigger on/off flag for pChannel in the DSPs.  If set, the
// gate will trigger an AScan save when the signal exceeds the gate level.
// This allows the display to be synchronized with signals of interest so they
// are displayed clearly rather than as brief flashes.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setAScanTrigger(int pGate, boolean pState, boolean pForceUpdate)
{

    if (pState != gates[pGate].getIsAScanTriggerGate()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    //store the AScan trigger gate setting for the specified gate
    gates[pGate].setAScanTriggerGate(pState);

    //Look at all gates' AScan trigger settings -- if any gate(s) are set as
    //trigger gates, then the DSP's flags are set to enable the triggered AScan
    //mode.  If no gate is a trigger gate, then the DSP's AScan mode is set to
    //free run.

    boolean triggerGateFound = false;
    for(int i = 0; i < numberOfGates; i++){
        if(gates[i].getIsAScanTriggerGate()){
            triggerGateFound = true;
            setAScanFreeRun(false, false);
            break;
        }
    }//for(int i = 0; i < numberOfGates; i++)

    //no gate was a trigger gate so set DSP's AScan mode to free-run
    if (!triggerGateFound) {setAScanFreeRun(true, false);}

}//end of Channel::setAScanTrigger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setTransducer
//
// Sets the transducer's pulse on/off state, pulse bank, and pulse channel
// and transmits these values to the DSP.
//
// Note that the transucer can be on one channel while another is fired for
// pitch/catch configurations.
//
// pBank is the pulser bank to which the transducer is to be assigned.
// pPulsedChannel is the pulser fired for the specified channel.
// pOnOff is 0 if the channel is to be pulsed and 1 if not.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

void setTransducer(boolean pChannelOn, int pPulseBank, int pPulseChannel,
                                                        boolean pForceUpdate)
{

    if (pChannelOn != channelOn) {pForceUpdate = true;}
    if (pPulseBank != pulseBank) {pForceUpdate = true;}
    if (pPulseChannel != pulseChannel) {pForceUpdate = true;}

    channelOn =
            pChannelOn; pulseBank = pPulseBank; pulseChannel = pPulseChannel;

    channelOn = (mode.getValue() != UTBoard.CHANNEL_OFF);

    if (utBoard != null && pForceUpdate) {
        utBoard.sendTransducer(boardChannel,
                 (byte)(channelOn ? 1:0), (byte)pulseBank, (byte)pulseChannel);
    }

}//end of Channel::setTransducer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::linkPlotters
//
// This function is called by Plotters (Traces, etc.) to link their buffers to
// specific hardware channels/gates and give a link back to variables in the
// Plotter object.
//

public void linkPlotters(int pChartGroup, int pChart, int pTrace,
        TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
                                                            Trace pTracePtr)
{

    for (int i = 0; i < numberOfGates; i++) {
        gates[i].linkPlotters(pChartGroup, pChart, pTrace, pTraceData,
                                        pThresholds, pPlotStyle, pTracePtr);
    }

}//end of Channel::linkPlotters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//

private void configure(IniFile pConfigFile)
{

    //load the nS per data point value and compute the uS per data point as well

    nSPerDataPoint =
      pConfigFile.readDouble("Hardware", "nS per Data Point", 15.0);
    uSPerDataPoint = nSPerDataPoint / 1000;

    String whichChannel = "Channel " + (channelIndex + 1);

    title =
          pConfigFile.readString(
                         whichChannel, "Title", "Channel " + (channelIndex+1));

    shortTitle = pConfigFile.readString(
                        whichChannel, "Short Title", "Ch " + (channelIndex+1));

    detail = pConfigFile.readString(whichChannel, "Detail", title);

    chassisAddr = pConfigFile.readInt(whichChannel, "Chassis", 1);

    slotAddr = pConfigFile.readInt(whichChannel, "Slot", 1);

    boardChannel = pConfigFile.readInt(whichChannel, "Board Channel", 1) - 1;

    pulseChannel = pConfigFile.readInt(whichChannel, "Pulse Channel", 1) - 1;

    pulseBank = pConfigFile.readInt(whichChannel, "Pulse Bank", 1) - 1;

    enabled = pConfigFile.readBoolean(whichChannel, "Enabled", true);

    type = pConfigFile.readString(whichChannel, "Type", "Other");

    numberOfGates = pConfigFile.readInt(whichChannel, "Number Of Gates", 3);

    numberOfDACGates =
            pConfigFile.readInt(whichChannel, "Number Of DAC Gates", 10);

    //This option controls whether the scope (AScan, etc.) is frozen when
    //the Calibration Window is not in focus. For most channels, this should
    //be true as the scope display requires excessive processing by the DSPs
    //for the selected channel which will cause reduced rep rate. Some channels
    //however are not as critical and it may be desired to monitor their scope
    //signal at all times.
    
    freezeScopeWhenNotInFocus = pConfigFile.readBoolean(whichChannel, 
                                    "Freeze Scope When Not in Focus", true);
    
    //read the configuration file and create/setup the gates
    configureGates();

    //read the configuration file and create/setup the DAC gates
    configureDACGates();

}//end of Channel::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::configureGates
//
// Loads configuration settings from the configuration.ini file relating to
// the gates and creates/sets them up.
//

private void configureGates()
{

    //create an array of gates per the config file setting
    if (numberOfGates > 0){

        //protect against too many
        if (numberOfGates > 20) {numberOfGates = 20;}

        gates = new UTGate[numberOfGates];

        for (int i = 0; i < numberOfGates; i++) {
            gates[i] =
                    new UTGate(configFile, channelIndex, i, syncedVarMgr);
            gates[i].init();
        }

    }//if (numberOfGates > 0)

}//end of Channel::configureGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::configureDACGates
//
// Loads configuration settings from the configuration.ini file relating to
// the DAC gates and creates/sets them up.
//

private void configureDACGates()
{

    //create an array of gates per the config file setting
    if (numberOfDACGates > 0){

        //protect against too many
        if (numberOfDACGates > 20) {numberOfDACGates = 20;}

        dacGates = new DACGate[numberOfDACGates];

        for (int i = 0; i < numberOfDACGates; i++) {
            dacGates[i] = new DACGate(configFile, channelIndex, i,
                                                    syncedVarMgr, scopeMax);
            dacGates[i].init();
        }

    }//if (numberOfDACGates > 0)

}//end of Channel::configureDACGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::requestAScan
//
// Requests an AScan dataset for this channel from the appropriate remote
// device.
//

public void requestAScan()
{

    //boardChannel specifies which analog channel on the UT board is associated
    //with this channel object - it is read from the configuration file

    if (utBoard != null) {
        utBoard.requestAScan(boardChannel, hardwareDelayFPGA.getValue());
    }

}//end of Channel::requestAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScan
//
// Retrieves an AScan dataset for the specified channel.
//

public AScan getAScan()
{

    //boardChannel specifies which analog channel on the UT board is associated
    //with this channel object - it is read from the configuration file

    if (utBoard != null) {
        return utBoard.getAScan();
    }
    else {
        return null;
    }

}//end of Channel::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::requestPeakData
//
// Sends a request to the remote device for a peak data packet for the
// specified channel.
//
// The channel number sent refers to one of the four analog channels on the
// board.  The UTBoard object has a connection between the board channel
// and the logical channel so it can store the peak from any board channel
// with its associated logical channel.
//

public void requestPeakData()
{

    if (utBoard != null) {utBoard.requestPeakData(boardChannel);}

}//end of Channel::requestPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateStart
//
// Sets the gate start position of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateStart(int pGate, double pStart, boolean pForceUpdate)
{

    if (pStart != gates[pGate].gateStart.getValue()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    gates[pGate].gateStart.setValue(pStart, pForceUpdate);

    //store the variable as appropriate for the interface tracking mode - this
    //allows switching back and forth between modes
    if (interfaceTracking) {
        gates[pGate].gateStartTrackingOn = pStart;
    }
    else {
        gates[pGate].gateStartTrackingOff = pStart;
    }

    //determine the span from the earliest gate edge to the latest (in time)
    calculateGateSpan();

}//end of Channel::setGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateStart
//
// Returns the gate start position of pGate.
//

public double getGateStart(int pGate)
{

    return gates[pGate].gateStart.getValue();

}//end of Channel::getGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateStartTrackingOn
//
// Sets the gate start position for pGate stored for use with
// Interface Tracking.
//
// No force update or call to calculateGateSpan is needed here because the
// value is only stored and not applied.
//

public void setGateStartTrackingOn(int pGate, double pStart)
{

    gates[pGate].gateStartTrackingOn = pStart;

}//end of Channel::setGateStartTrackingOn
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateStartTrackingOn
//
// Returns the gate start position for pGate stored for use with
// Interface Tracking.
//

public double getGateStartTrackingOn(int pGate)
{

    return gates[pGate].gateStartTrackingOn;

}//end of Channel::getGateStartTrackingOn
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateStartTrackingOff
//
// Sets the gate start position for pGate stored for use without
// Interface Tracking.
//
// No force update or call to calculateGateSpan is needed here because the
// value is only stored and not applied.
//

public void setGateStartTrackingOff(int pGate, double pStart)
{

    gates[pGate].gateStartTrackingOff = pStart;

}//end of Channel::setGateStartTrackingOff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateStartTrackingOff
//
// Returns the gate start position for pGate stored for use with
// Interface Tracking.
//

public double getGateStartTrackingOff(int pGate)
{

    return gates[pGate].gateStartTrackingOff;

}//end of Channel::getGateStartTrackingOff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setPreviousGateStart
//
// Stores pStart in the previousGateStart variable for gate pGate.
//

public void setPreviousGateStart(int pGate, double pStart)
{

    gates[pGate].previousGateStart = pStart;

}//end of Channel::setPreviousGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getPreviousGateStart
//
// Returns the previousGateStart variable for gate pGate.
//

public double getPreviousGateStart(int pGate)
{

    return gates[pGate].previousGateStart;

}//end of Channel::getPreviousGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateWidth
//
// Sets the gate width of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateWidth(int pGate, double pWidth, boolean pForceUpdate)
{

    if (pWidth != gates[pGate].gateWidth.getValue()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    gates[pGate].gateWidth.setValue(pWidth, pForceUpdate);

    //determine the span from the earliest gate edge to the latest (in time)
    calculateGateSpan();

}//end of Channel::setGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateWidth
//
// Gets the gate width of pGate.
//

public double getGateWidth(int pGate)
{

    return gates[pGate].gateWidth.getValue();

}//end of Channel::getGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setPreviousGateWidth
//
// Stores pStart in the previousGateWidth variable for gate pGate.
//

public void setPreviousGateWidth(int pGate, double pWidth)
{

    gates[pGate].previousGateWidth = pWidth;

}//end of Channel::setPreviousGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getPreviousGateWidth
//
// Returns the previousGateWidth variable for gate pGate.
//

public double getPreviousGateWidth(int pGate)
{

    return gates[pGate].previousGateWidth;

}//end of Channel::getPreviousGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateFlags
//
// Gets the gate flags.
//

public int getGateFlags(int pGate)
{

    return gates[pGate].gateFlags.getValue();

}//end of Channel::getGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateLevel
//
// Sets the gate level of pGate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateLevel(int pGate, int pLevel, boolean pForceUpdate)
{

    if (pLevel != gates[pGate].gateLevel.getValue()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    gates[pGate].gateLevel.setValue(pLevel, pForceUpdate);

}//end of Channel::setGateLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateLevel
//
// Gets the gate level of pGate.
//

public int getGateLevel(int pGate)
{

    return gates[pGate].gateLevel.getValue();

}//end of Channel::getGateLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateHitCount
//
// Sets the hit count value for the gate.  This is the number of consecutive
// times the signal must exceed the gate level before it will cause an alarm.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateHitCount(int pGate, int pHitCount, boolean pForceUpdate)
{

    gates[pGate].gateHitCount.setValue(pHitCount, pForceUpdate);

}//end of Channel::setGateHitCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateHitCount
//
// Returns the hit count value for the gate.  See setGateHitCount for more
// info.
//

public int getGateHitCount(int pGate)
{

    return gates[pGate].gateHitCount.getValue();

}//end of Channel::getGateHitCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateMissCount
//
// Sets the miss count value for the gate.  This is the number of consecutive
// times the signal must fail to exceed the gate level before it will cause an
// alarm.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateMissCount(int pGate, int pMissCount, boolean pForceUpdate)
{

    gates[pGate].gateMissCount.setValue(pMissCount, pForceUpdate);

}//end of Channel::setGateMissCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateMissCount
//
// Returns the miss count value for the gate.  See setGateMissCount for more
// info.
//

public int getGateMissCount(int pGate)
{

    return gates[pGate].gateMissCount.getValue();

}//end of Channel::getGateMissCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateSigProcTuningValue
//
// Sets the signal processing tuning value specified by pWhichTuningValue for
// pGate.  These values are used by various signal processing methods to
// trigger events.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//
// Note: pWhichTuningValue is zero based -- 0 returns tuning value 1
//

public void setGateSigProcTuningValue(int pGate, int pWhichTuningValue,
                                              int pValue, boolean pForceUpdate)
{

    switch (pWhichTuningValue){    
        case 0: gates[pGate].sigProcTuning1.setValue(pValue, pForceUpdate);
        case 1: gates[pGate].sigProcTuning2.setValue(pValue, pForceUpdate);
        case 2: gates[pGate].sigProcTuning3.setValue(pValue, pForceUpdate);        
    }
    
}//end of Channel::setGateSigProcTuningValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateSigProcTuningValue
//
// Returns the signal processing tuning value specified by pWhichTuningValue
// for pGate.  See setSigProcTuningValue for more info.
//
// Note: pWhichTuningValue is zero based -- 0 returns tuning value 1
//

public int getGateSigProcTuningValue(int pGate, int pWhichTuningValue)
{

    switch (pWhichTuningValue){
    
        case 0: return gates[pGate].sigProcTuning1.getValue();
        case 1: return gates[pGate].sigProcTuning2.getValue();
        case 2: return gates[pGate].sigProcTuning3.getValue();
        default: return(0);
            
    }
            
}//end of Channel::getGateSigProcTuningValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setGateSigProc
//
// Sets the signal processing mode for pGate to pMode.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setGateSigProc(int pGate, String pMode, boolean pForceUpdate)
{

    if (!pMode.equals(gates[pGate].getSignalProcessing())){pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    gates[pGate].setSignalProcessing(pMode);

}//end of Channel::setGateSigProc
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateSigProc
//
// Returns the signal processing mode for pGate to pMode.
//

public String getGateSigProc(int pGate)
{

    return(gates[pGate].getSignalProcessing());

}//end of Channel::getGateSigProc
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyGatePositionChanged
//
// Checks if any gate start, width, or level values have been changed.
//

private boolean isAnyGatePositionChanged()
{

    for (int i = 0; i < numberOfGates; i++) {
        if (gates[i].isPositionChanged()) {return(true);}
    }

   return(false);

}//end of Channel::isAnyGatePositionChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyGateSigProcTuningChanged
//
// Checks if any gate's signal processing tuning values have changed.
//

private boolean isAnyGateSigProcTuningChanged()
{

    for (int i = 0; i < numberOfGates; i++) {
        if (gates[i].sigProcTuning1.getDataChangedFlag()
            ||gates[i].sigProcTuning2.getDataChangedFlag()
            ||gates[i].sigProcTuning3.getDataChangedFlag()) {
            return(true);}
    }

   return(false);

}//end of Channel::isAnyGateSigProcTuningChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyGateFlagsChanged
//
// Checks if any gate flags have changed.
//

private boolean isAnyGateFlagsChanged()
{

    for (int i = 0; i < numberOfGates; i++) {
        if (gates[i].isFlagsChanged()) {return(true);}
    }

   return(false);

}//end of Channel::isAnyGateFlagsChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyDACGatePositionChanged
//
// Checks if any DAC gate start, width, or gain values have been changed.
//

private boolean isAnyDACGatePositionChanged()
{

    for (int i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].isPositionChanged()) {return(true);}
    }

   return(false);

}//end of Channel::isAnyDACGatePositionChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyDACGateFlagsChanged
//
// Checks if any DAC gate flags have changed.
//

private boolean isAnyDACGateFlagsChanged()
{

    for (int i = 0; i < numberOfDACGates; i++) {
        if (dacGates[i].isFlagsChanged()) {return(true);}
    }
   return(false);

}//end of Channel::isAnyDACGateFlagsChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::isAnyGateHitMissChanged
//
// Checks if any gate Hit/Miss count values have changed.
//

private boolean isAnyGateHitMissChanged()
{

    for (int i = 0; i < numberOfGates; i++) {
        if (gates[i].gateHitCount.getDataChangedFlag()
            || gates[i].gateMissCount.getDataChangedFlag()) {return(true);}
    }

   return(false);

}//end of Channel::isAnyGateHitMissChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateParameters
//
// Sends gate start, width, and level to the remotes.
//

public void sendGateParameters()
{

    //unknown which gate(s) have changed data, so check them all

    for (int i = 0; i < numberOfGates; i++){

        if (gates[i].isPositionChanged()){

            if (utBoard != null) {
                utBoard.sendGate(boardChannel, i,
                    (int)(gates[i].gateStart.applyValue() / uSPerDataPoint),
                    (int)(gates[i].gateWidth.applyValue() / uSPerDataPoint),
                    gates[i].gateLevel.applyValue());
            }

        }//gates[i].gateStart.getDataChanged()...
    }// for (int i = 0; i < numberOfGates; i++)

}//end of Channel::sendGateParameters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateFlags
//
// Sends gate flags to the remotes.
//

public void sendGateFlags()
{

    //unknown which gate(s) have changed data, so check them all

    for (int i = 0; i < numberOfGates; i++){
        if (gates[i].getFlags().getDataChangedFlag()){

            if (utBoard != null) {
                utBoard.sendGateFlags(
                boardChannel, i, gates[i].getFlags().applyValue());
            }

        }//if (gates[i].getFlags().getDataChanged() == true)
    }// for (int i = 0; i < numberOfGates; i++)

}//end of Channel::sendGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateSigProcTuningValues
//
// Sends gate signal processing tuning values to the remotes.
//

public void sendGateSigProcTuningValues()
{

    //unknown which gate(s) have changed data, so check them all

    for (int i = 0; i < numberOfGates; i++){

        if (gates[i].sigProcTuning1.getDataChangedFlag()
             || gates[i].sigProcTuning2.getDataChangedFlag()
             || gates[i].sigProcTuning3.getDataChangedFlag()){

            if (utBoard != null) {
                utBoard.sendGateSigProcTuningValues(boardChannel, i,
                        gates[i].sigProcTuning1.applyValue(),
                        gates[i].sigProcTuning2.applyValue(),
                        gates[i].sigProcTuning3.applyValue());
            }

        }//if (gates[i].sigProcTuningValue1.getDataChanged())
    }// for (int i = 0; i < numberOfGates; i++)

}//end of Channel::sendGateSigProcTuningValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateHitMiss
//
// Sends gate hit and miss values to the remotes.
//

public void sendGateHitMiss()
{
    //unknown which gate(s) have changed data, so check them all

    for (int i = 0; i < numberOfGates; i++){

        if (gates[i].gateHitCount.getDataChangedFlag()
             || gates[i].gateMissCount.getDataChangedFlag()){

            if (utBoard != null) {
                     utBoard.sendHitMissCounts(boardChannel, i,
                     gates[i].gateHitCount.applyValue(),
                     gates[i].gateMissCount.applyValue());
            }

        }//if (gates[i].gateHitCount.getDataChanged()...
    }// for (int i = 0; i < numberOfGates; i++)

}//end of Channel::sendGateHitMiss
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDACGateParameters
//
// Sends DAC gate start, width, and level to the remotes.
//

public void sendDACGateParameters()
{

    //unknown which gate(s) have changed data, so check them all
    //clear the flags even if utBoard is null so they won't be checked again

    for (int i = 0; i < numberOfDACGates; i++){

        if (dacGates[i].isPositionChanged()){

            if (utBoard != null){

                utBoard.sendDAC(boardChannel, i,
                    (int)(dacGates[i].gateStart.applyValue() / uSPerDataPoint),
                    (int)(dacGates[i].gateWidth.applyValue() / uSPerDataPoint),
                    dacGates[i].gainForRemote.applyValue());

            }//if (utBoard != null)
        }//if (dacGates[i].isPositionChanged())
    }// for (int i = 0; i < numberOfDACGates; i++)

}//end of Channel::sendDACGateParameters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDACActive
//
// Sets the DAC gate active flag.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDACActive(int pGate, boolean pValue, boolean pForceUpdate)
{

    if (pValue != dacGates[pGate].getActive()) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    dacGates[pGate].setActive(pValue);

}//end of Channel::setDACActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::copyGate
//
// Copies the appropriate values from source gate pSourceGate to gate indexed
// by gate pDestGate.
//

public void copyGate(int pDestGate, DACGate pSourceGate)
{

    //copy pSourceGate to pDestGate
    dacGates[pDestGate].copyFromGate(pSourceGate);

}//end of DACGate::copyGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDACGateFlags
//
// Sends DAC flags to the remotes.
//

public void sendDACGateFlags()
{

    //unknown which gate(s) have changed data, so check them all

    for (int i = 0; i < numberOfDACGates; i++){
        if (dacGates[i].gateFlags.getDataChangedFlag()){

            if (utBoard != null) {
                utBoard.sendDACGateFlags(
                    boardChannel, i, dacGates[i].getFlags().applyValue());
            }

        }//if (dacGates[i].gateFlags.getDataChanged())
    }// for (int i = 0; i < numberOfDACGates; i++)

}//end of Channel::sendDACGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::calculateDACGateTimeLocation
//
// Forces the specified DAC gate to update its position, width, and level time
// values by calculating them from its pixel location values.
//

public void calculateDACGateTimeLocation(int pDACGate,
       double pUSPerPixel, int pDelayPix, int pCanvasHeight, int pVertOffset,
                                                          boolean pForceUpdate)
{

    dacGates[pDACGate].calculateGateTimeLocation(
                            pUSPerPixel, pDelayPix, pCanvasHeight, pVertOffset);

}//end of Channel::calculateDACGateTimeLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDataChangesToRemotes
//
// If any data has been changed, sends the changes to the remotes.
//
// These values are often changed by the user clicking on controls which are
// handled in the main GUI thread. Synced variables are used to avoid
// collisions with the thread handling the UTBoards which also calls this
// function.
//
// If pWaitForAcks is true, this method will force the processing of any Ack
// packets returned by the DSPs and check to see if each message was
// acknowledged. This is usually done when the channels are first set up or
// copied as a lot of data is sent at that time and it is good to verify
// receipt.
//

public void sendDataChangesToRemotes()
{

    //do nothing if no data changed for any synced variables
    if (!syncedVarMgr.getDataChangedMaster()) {return;}

    if (utBoard != null) {utBoard.clearDSPMessageAndAckCounters();}

    if (dspControlFlags.getDataChangedFlag()) {sendDSPControlFlags();}

    if (softwareGain.getDataChangedFlag()) {sendSoftwareGain();}

    if (hardwareGain1.getDataChangedFlag()) {sendHardwareGain();}

    if (hardwareGain2.getDataChangedFlag()) {sendHardwareGain();}

    if (aScanSmoothing.getDataChangedFlag()) {sendAScanSmoothing();}

    if (mode.getDataChangedFlag()) {sendMode();}

    if (filter.getDataChangedFlag()) {sendFilter();}    
    
    if (hardwareDelayFPGA.getDataChangedFlag()) {sendHardwareDelayToFPGA();}

    if (softwareDelay.getDataChangedFlag() ||
                hardwareDelayDSP.getDataChangedFlag()) {sendSoftwareDelay();}

    if (hardwareRange.getDataChangedFlag() || aScanScale.getDataChangedFlag()) {
        sendRange();
    }

    if (isAnyGatePositionChanged()) {sendGateParameters();}

    if (isAnyGateFlagsChanged()) {sendGateFlags();}

    if (isAnyGateHitMissChanged()) {sendGateHitMiss();}

    if (isAnyGateSigProcTuningChanged()) {sendGateSigProcTuningValues();}

    if (isAnyDACGatePositionChanged()) {sendDACGateParameters();}

    if (isAnyDACGateFlagsChanged()) {sendDACGateFlags();}

    //wait one time to allow the DSPs to send the response ACK packets rather
    //than having processAllAvailableDataPackets wait between each packet as
    //this way is much faster

    if (utBoard != null) {utBoard.waitSleep(300);}

    //process the expected DSP ACK packets
    if (utBoard != null) {utBoard.processAllAvailableDataPackets(false);}

    //check to see if each message received and ACK
    if (utBoard != null) {utBoard.compareDSPAckCountToMessageCount();}

}//end of Channel::sendDataChangesToRemotes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateStart
//
// Returns the DAC gate start position of pGate.
//

public double getDACGateStart(int pGate)
{

    return dacGates[pGate].gateStart.getValue();

}//end of Channel::getDACGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateWidth
//
// Gets the DAC gate width of pGate.
//

public double getDACGateWidth(int pGate)
{

    return dacGates[pGate].gateWidth.getValue();

}//end of Channel::getDACGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateLevel
//
// Gets the DAC gate level of pGate.
//

public int getDACGateLevel(int pGate)
{

    return dacGates[pGate].gateLevel.getValue();

}//end of Channel::getDACGateLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateActive
//
// Returns the DAC gate active flag.
//

public boolean getDACGateActive(int pGate)
{

    return dacGates[pGate].getActive();

}//end of Channel::getDACGateActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateSelected
//
// Returns the DAC gate selected flag.
//

public boolean getDACGateSelected(int pGate)
{

    return dacGates[pGate].getSelected();

}//end of Channel::getDACGateSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setAllDACGateDataChangedFlags
//
// Sets all data changed flags related to the DAC gates.  Setting them true
// will force all data to be sent to the remotes.
//

public void setAllDACGateDataChangedFlags(boolean pValue)
{

    //not used at this time

}//end of Channel::setAllDACGateDataChangedFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setFilter
//
// Sets the digital signal filter to pFilterName. The values for the filter are
// loaded from the text file with that name and are transmitted to the DSP.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setFilter(String pFilterName, boolean pForceUpdate)
{
    
    if (!filterName.equals(pFilterName)) {pForceUpdate = true;}

    if (!pForceUpdate) {return;} //do nothing unless value change or forced

    filterName = pFilterName;
    
    int []newValues = loadFilter(pFilterName);

    filter.setValues(newValues, pForceUpdate);
    
}//end of Channel::setFilter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getFilter
//
// Sets the digital signal filter name filterName.
//

public String getFilter()
{

    return(filterName);
        
}//end of Channel::getFilter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::createOneElementArrayContainingZero
//
// Returns an in array with a single element set to zero.
//

private int[] createOneElementArrayContainingZero()
{
    
    int[] array = new int[1]; array[0] = 0;

    return(array);

}//end of Channel::createOneElementArrayContainingZero
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::loadFilter
//
// Loads the values from the text file pFilterName and returns them in an
// array.
//
// The first value in the array is the number of filter coefficients. If the
// file cannot be loaded or does not contain valid numbers, a single element
// array will be returned containing the value of 0 to denote no filtering.
//

private int[] loadFilter(String pFilterName)
{

    ArrayList<String> dataFromFile = new ArrayList<>();
    
    loadFromTextFile("filters\\"+ filterName + ".txt", dataFromFile);
    
    return(parseFilterFileData(dataFromFile));
    
}//end of Channel::loadFilter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::parseFilterFileData
//
// Parses the data loaded from the filter file in pData to retrieve filter
// coefficients and right shift amount.
//
// If no valid integer values are found, a one element array containing the
// value 0 is returned.
//
// If invalid entries are encountered when expecting an integer, the value
// will set to zero.
//
// The format of the returned array:
//  byte 0 = number of filter coefficients
//  byte 1 = number of bits to right shift during FIR processing
//  byte 2~last = FIR filter coefficients
//

private int[] parseFilterFileData(ArrayList<String> pData)
{

    boolean result; ArrayList<Integer>entryList = new ArrayList<>();
    int[] filterValues;
    
    String line;
    
    ListIterator iter = pData.listIterator();

    //parse the coefficient scaling vaue -- the filters often have a LOT of gain
    //when used as is, so scaling down the coefficients is a simple method of
    //allowing the user to adjust the gain by setting a value in the file
        
    result = searchListForString("<coefficient scaling start>" , iter);
    
    double coeffScaling = 1.0;
    
    if (result) {
    
        line = getNextNonBlankLineInList(iter);
        if(!line.isEmpty()) { coeffScaling = parseDouble(line, 1.0); }
        
    }
    
    //parse the number of bits each multiplication result is shifted during the
    //FIR filter math to prevent overflow during the process
        
    result = searchListForString("<FIR filter shift bits amount start>" , iter);
    
    if (!result) { return(createOneElementArrayContainingZero()); }
    
    line = getNextNonBlankLineInList(iter);
    
    if(line.isEmpty()) { return(createOneElementArrayContainingZero()); }
    
    int numFIRFilterBitRightShifts;
        
    numFIRFilterBitRightShifts = parseInt(line, -5);
    
    //parse the filter coefficients
    
    result = searchListForString("<start of coefficients>" , iter);
    
    if (!result) { return(createOneElementArrayContainingZero()); }
    
    boolean done = false;
    
    while(!done){
        
        int coeff;
        
        line = getNextNonBlankLineInList(iter);
        if(line.isEmpty()) { return(createOneElementArrayContainingZero()); }
        coeff = parseInt(line, Integer.MAX_VALUE);        
        if (coeff == Integer.MAX_VALUE){ break; } //stop if non-integer
    
        coeff = (int)Math.round(coeff * coeffScaling); //scale to adjust gain
        
        entryList.add(coeff); //add each valid value to the list
        
    }
    
    //if number of coefficients exceeds max allowed, return single element
    //set to zero array as the filter values cannot be trusted
    
    if(entryList.size() > MAX_NUM_COEFFICIENTS) {
        return(createOneElementArrayContainingZero());
    }
        
    //the size of the entry list is the number of coefficients read, add this
    //count as the first entry in the list
    entryList.add(0, entryList.size());
    //second entry is the FIR filter bit shift amount
    entryList.add(1, numFIRFilterBitRightShifts);
    
    filterValues = new int[entryList.size()];
    
    for(int i=0; i<filterValues.length; i++){
        filterValues[i] = entryList.get(i);
    }
    
    return(filterValues);

}//end of Channel::parseFilterFileData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::parseInt
//
// Returns an integer parsed from String pString.
// If an error occurs during parsing, pDefault is returned.
//

private int parseInt(String pString, int pDefault)
{
   
    int value;
    
    try{
        value = Integer.parseInt(pString.trim());
    }
    catch(NumberFormatException e){
        value = pDefault;
    }
    
    return(value);
    
}// end of Channel::parseInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::parseDouble
//
// Returns a double parsed from String pString.
// If an error occurs during parsing, pDefault is returned.
//

private double parseDouble(String pString, double pDefault)
{
   
    double value;
    
    try{
        value = Double.parseDouble(pString.trim());
    }
    catch(NumberFormatException e){
        value = pDefault;
    }
    
    return(value);
    
}// end of Channel::parseDouble
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::searchListForString
//
// Searches an ArrayList for pTarget iterating with pIter.
//
// Returns true if the target is found, leaving pIter ready to load next line.
// Returns false if target not found, pIter will be at end of list.
//

private boolean searchListForString(String pTarget, ListIterator pIter)
{
    
    while(pIter.hasNext()){

        String input = (String)pIter.next();        
        if (input.equals(pTarget)){ return(true); }

    }
    
    return(false);

}//end of Channel::searchListForString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getNextNonBlankLineInList
//
// Skips past any blank lines in an ArrayList iterating with pIter.
//
// Returns the contents of the next non-blank line.
// Returns an empty string if no non-blank line is found.
//

private String getNextNonBlankLineInList(ListIterator pIter)
{
    
    while(pIter.hasNext()){

        String input = (String)pIter.next();        
        if (!input.isEmpty()){ return(input); }

    }
    
    return("");
    
}//end of Channel::getNextNonBlankLineInList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::loadFromTextFile
//
// Loads all text strings from  text file pFilename into pList.
//

public void loadFromTextFile(String pFilename, ArrayList<String> pList)
{

    try{

        openTextInFile(pFilename);

        readDataFromTextFile(pList);

    }
    catch (IOException e){
        //display an error message and/or log the message
    }
    finally{
        closeTextInFile();
    }

}//end of Channel::loadFromTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::openTextInFile
//
// Opens text file pFilename for reading.
//

private void openTextInFile(String pFilename) throws IOException
{

    fileInputStream = new FileInputStream(pFilename);
    inputStreamReader = new InputStreamReader(fileInputStream);
    in = new BufferedReader(inputStreamReader);

}//end of Channel::openTextInFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::readDataFromTextFile
//
// Reads all strings from the text file into pList.
//

private void readDataFromTextFile(ArrayList<String> pList) throws IOException
{

    //read each data line or until end of file reached

    String line;

    if ((line = in.readLine()) != null){
        dataVersion = line;
    }
    else{
        return;
    }

    while((line = in.readLine()) != null){
        pList.add(line);
    }

}//end of Channel::readDataFromTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::closeTextInFile
//
// Closes the text input file.
//

private void closeTextInFile()
{

    try{

        if (in != null) {in.close();}
        if (inputStreamReader != null) {inputStreamReader.close();}
        if (fileInputStream != null) {fileInputStream.close();}

    }
    catch(IOException e){

        //ignore error while trying to close the file
        //could log the error message in the future

    }

}//end of Channel::closeTextInFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getFilterName
//
// Returns the name of the filter in use.
//

public String getFilterName()
{

    return(filterName);

}//end of Channel::getFilterName
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendFilter
//
// Sends the filter values to the DSP.
//

public void sendFilter()
{

    if (utBoard != null) {
        utBoard.sendFilter(boardChannel, filter.applyValues());
    }

}//end of Channel::sendFilter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::warmReset
//
// Resets the UT board for this channel as well as the other channels on that
// board. The channel settings, including FPGA register values will be
// reloaded.
//

public void warmReset()
{

    if (utBoard != null) {utBoard.warmReset();}

}//end of Channel::warmReset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

    String section = "Channel " + (channelIndex + 1);

    aScanDelay = pCalFile.readDouble(section, "Sample Delay", 0);
    aScanRange = pCalFile.readDouble(section, "Range", 53.0);
    setSoftwareGain(pCalFile.readDouble(section, "Software Gain", 0), true);
    hardwareGain1.setValue(
                pCalFile.readInt(section, "Hardware Gain Stage 1", 2), true);
    hardwareGain2.setValue(
                  pCalFile.readInt(section, "Hardware Gain Stage 2", 1), true);
    interfaceTracking = pCalFile.readBoolean(
                                        section, "Interface Tracking", false);
    dacEnabled = pCalFile.readBoolean(section, "DAC Enabled", false);
    mode.setValue(pCalFile.readInt(section, "Signal Mode", 0), true);
    
    filterName = pCalFile.readString(section,
                                            "Signal Filter Name", "No Filter");
    
    filter.setValuesFromString(
              pCalFile.readString(section, "Signal Filter Values", "0"), true);

    //default previousMode to mode if previousMode has never been saved
    previousMode =
            pCalFile.readInt(section, "Previous Signal Mode", mode.getValue());
    channelOn = (mode.getValue() != UTBoard.CHANNEL_OFF);
    rejectLevel = pCalFile.readInt(section, "Reject Level", 0);
    aScanSmoothing.setValue(
                 pCalFile.readInt(section, "AScan Display Smoothing", 1), true);

    // call each gate to load its data
    for (int i = 0; i < numberOfGates; i++) {gates[i].loadCalFile(pCalFile);}

    // call each DAC gate to load its data
    for (int i = 0; i < numberOfDACGates; i++) {
        dacGates[i].loadCalFile(pCalFile);
    }

    //determine the span from the earliest gate edge to the latest (in time)
    calculateGateSpan();

}//end of Channel::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

    String section = "Channel " + (channelIndex + 1);

    pCalFile.writeDouble(section, "Sample Delay", aScanDelay);
    pCalFile.writeDouble(section, "Range", aScanRange);
    pCalFile.writeDouble(section, "Software Gain", softwareGain.getValue());
    pCalFile.writeInt(
                    section, "Hardware Gain Stage 1", hardwareGain1.getValue());
    pCalFile.writeInt(
                    section, "Hardware Gain Stage 2", hardwareGain2.getValue());
    pCalFile.writeBoolean(section, "Interface Tracking", interfaceTracking);
    pCalFile.writeBoolean(section, "DAC Enabled", dacEnabled);
    pCalFile.writeInt(section, "Signal Mode", mode.getValue());
    
    pCalFile.writeString(section, "Signal Filter Name", filterName);
    pCalFile.writeString(section, "Signal Filter Values", filter.toString());
    
    pCalFile.writeInt(section, "Previous Signal Mode", previousMode);
    pCalFile.writeInt(section, "Reject Level", rejectLevel);
    pCalFile.writeInt(
                section, "AScan Display Smoothing", aScanSmoothing.getValue());

    // call each gate to save its data
    for (int i = 0; i < numberOfGates; i++) {
        gates[i].saveCalFile(pCalFile);
    }

    // call each DAC gate to save its data
    for (int i = 0; i < numberOfDACGates; i++) {
        dacGates[i].saveCalFile(pCalFile);
    }

}//end of Channel::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::saveCalFileHumanReadable
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

    pOut.write("Channel | Main Gain | Hdw Gain 1 | Hdw Gain 2 | DC Offset |");
    pOut.write(" Delay | Range | Mode | Reject");
    pOut.newLine();

    pOut.write(Settings.postPad(" " + (channelIndex + 1), 7));

    pOut.write(
        Settings.prePad(decimalFormats[2].format(softwareGain.getValue()), 12));

    pOut.write(Settings.prePad("" + hardwareGain1.getValue(), 13));

    pOut.write(Settings.prePad("" + hardwareGain2.getValue(), 13));

    pOut.write(Settings.prePad("" + 0, 12)); //wip - need to read actual value

    pOut.write(Settings.prePad(decimalFormats[3].format(aScanDelay), 8));

    pOut.write(Settings.prePad(decimalFormats[3].format(aScanRange), 8));

    pOut.write(Settings.prePad("" + mode.getValue(), 7)); //wip - print name rather than number

    pOut.write(Settings.prePad("" + rejectLevel, 9));

    pOut.newLine();

    //gate data header
    pOut.write("   Gate         | Start | Width | Level | Hits | Misses |");
    pOut.write(" Process                        | Threshold");
    pOut.newLine();

    // call each gate to save its data
    for (int i = 0; i < numberOfGates; i++) {
        gates[i].saveCalFileHumanReadable(pOut);
    }


    if(dacEnabled){
        //DAC gate data header
        pOut.write("   DAC Gate     | Start | Width | Level");
        pOut.newLine();

        // call each gate to save its data
        for (int i = 0; i < numberOfDACGates; i++) {
            dacGates[i].saveCalFileHumanReadable(pOut);
        }
    }
    else{
        pOut.write("   DAC not enabled.");
        pOut.newLine();
    }

}//end of Channel::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

}//end of class Channel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
