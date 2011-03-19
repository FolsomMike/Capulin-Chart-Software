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

//wip mks - gate width, height, start, hit count, miss count are now
// sent to the DSP by a thread other than the GUI thread to avoid collisions
//  see setGartStart et. al. for example.
// Same is done for the DAC gates.
// The other values which are user adjustable need to be handled in the same
// way, such as master gain, delay, range, etc..

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Channel
//
// This class handles an input channel.
//

public class Channel extends Object{

IniFile configFile;

Capulin1 channelOwner;
boolean dataChanged;
boolean gateParamsChanged, gateHitMissChanged, gateFlagsChanged;
boolean flags1SetMaskChanged, flags1ClearMaskChanged;

boolean dacGateParamsChanged, dacGateFlagsChanged;

int flags1SetMask, flags1ClearMask;

int chassisAddr, slotAddr, boardChannel;

public UTBoard utBoard;

public int channelIndex;
public Gate[] gates;
public int numberOfGates;

public DACGate[] dacGates;
public int numberOfDACGates;

int scopeMax = 350;

//used by the calibration window to store reference to the channel selectors
public Object calRadioButton;

public String title, shortTitle, type;

boolean channelOn;  //true: pulsed/displayed, off: not pulsed/displayed
boolean channelMasked; //true: pulsed/not display, off: pulsed/displayed

int mode = UTBoard.POSITIVE_HALF;
boolean interfaceTracking = false;
boolean dacEnabled = false;
double aScanDelay = 0;
int hardwareDelay, softwareDelay;
public int delayPix = 0;
double aScanRange = 0;
int hardwareRange;
public int softwareRange;
double softwareGain = 0;
int hardwareGain1, hardwareGain2;
int rejectLevel;
int aScanSmoothing;
int dcOffset;
double nSPerDataPoint;
double uSPerDataPoint;
public double nSPerPixel; //used by outside classes
public double uSPerPixel; //used by outside classes

int firstGateEdgePos, lastGateEdgePos;
boolean isWallChannel = false;

int pulseChannel, pulseBank;

//-----------------------------------------------------------------------------
// Channel::Channel (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
  
public Channel(IniFile pConfigFile, int pChannelIndex, Capulin1 pChannelOwner)
{

configFile = pConfigFile; channelIndex = pChannelIndex;
channelOwner = pChannelOwner;

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

//give the utBoard a link to the gates array
if (utBoard != null) utBoard.linkGates(boardChannel, gates, numberOfGates);

//set FPGA values such as sample start delay, sample size by calling each
//function

//always set range after setting gate parameters, delay, and interface tracking
//as these affect the range

// use this flag to check if any gate is an interface gate

boolean interfaceGatePresent = false;

//detect if both a start and end gate have been set - only if both are found
//is the channel set up for wall as this is the same test the DSPs use
boolean wallStartGateSet = false, wallEndGateSet = false;

//flag if interface and wall gates are present
for (int i = 0; i < numberOfGates; i++){
    if (gates[i].getInterfaceGate()) interfaceGatePresent = true;
    if (gates[i].isWallStartGate) wallStartGateSet = true;
    if (gates[i].isWallEndGate) wallEndGateSet = true;
    }

//if both wall start and end gates, set channel for wall data and
//that data will be appended to peak data packets from the DSPs

if (wallStartGateSet && wallEndGateSet){
    isWallChannel = true;
    if (utBoard != null)
        utBoard.sendWallChannelFlag(boardChannel, isWallChannel);
    }

//force interface tracking to false if no interface gate was set up
//if no interface gate is present, the interface tracking checkbox will not be
//displayed and the user cannot turn it off in case it was stored as on in the
//job file
//NOTE: If some but not all channels in a system have interface tracking, using
// "Copy to All" will copy the interface tracking setting to all channels.  This
// will disable the channels without an interface gate until the program is
// restarted and this piece of code gets executed.

if (!interfaceGatePresent) interfaceTracking = false;

//set bits in flags1 variable
//all flags1 variables should be set at once in the init to avoid conflicts
//due to all flag1 setting functions using the same mask storage variables
int flags1Mask = UTBoard.GATES_ENABLED + UTBoard.ASCAN_ENABLED;
if (dacEnabled) flags1Mask += UTBoard.DAC_ENABLED;
setFlags1(flags1Mask);

//setup various things
setAScanSmoothing(aScanSmoothing, true);
setSoftwareGain(softwareGain, true);
setHardwareGain(hardwareGain1, hardwareGain2, true);
setDCOffset(dcOffset, true);
setMode(mode, true);  //setMode also calls setTransducer
setInterfaceTracking(interfaceTracking, true);
setDelay(aScanDelay, true);

//setRange makes calculations based upon some of the settings above so do last
setRange(aScanRange, true);

//set all the data changed flags so the call to
//  sendDataChangesToRemotes() will send all data to the remotes
//the gates and DAC gates will have set their internal data changed flags true
//upon loading data from the cal file

dataChanged = true;
gateParamsChanged = true; gateHitMissChanged = true; gateFlagsChanged = true;
dacGateParamsChanged = true; dacGateFlagsChanged = true;

//send all the changes made above to the remotes
//call the function in owner because it clears the data changed flag in that
//object - since that function is synchronized, conflicts with other channel
//object setup threads will be avoided - the function sends the data for
//whatever channels have data changed flags set true at the time, so a call from
//one channel may send data for other channels, but will set the flags false so
//when those channels call nothing might be done because their data may already
//have been sent

sendDataChangesToRemotes();

//NOTE: some of the above calls may have invoked
//      channelOwner.setChannelDataChangedFlag(true) so the
// sendDataChangesToRemotes() function in that owner may be called when
// the main thread starts up - nothing will be sent to the remotes because
// all the data changed flags will have been set false by the above call to
// sendDataChangesToRemotes in this class

}//end of Channel::initialize
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
for (i = 0; i < numberOfDACGates; i++)
    if (dacGates[i].getActive())
        if (pX >= (dacGates[i].gatePixStartAdjusted - 5)
                && pX <= (dacGates[i].gatePixStartAdjusted + 5)
                && pY >= (dacGates[i].gatePixLevel - 5)
                && pY <= (dacGates[i].gatePixLevel + 5))
            break;

if (i == numberOfDACGates) i = -1; //no match found

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
for (i = 0; i < numberOfDACGates; i++)
    if (dacGates[i].getActive())
        if (dacGates[i].getSelected())
            break;

if (i == numberOfDACGates) i = -1; //no match found

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

if (pGate < 0 || pGate >= numberOfDACGates) return; //protect against invalid

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
for (int i = 0; i < numberOfDACGates; i++) if (dacGates[i].getActive()) c++;

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
if (getActiveDACGateCount() == numberOfDACGates) return;

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
    setDACGatePixelValues(0, pStart, pEnd, pLevel, true, false); //new values
    return;
    }

//if the new gate is located past the last gate, then insert at the end
if (pStart > dacGates[lastGate].gatePixEnd){
    pStart = dacGates[lastGate].gatePixEnd;
    setDACGatePixelValues(lastGate+1, pStart, pStart+35, pLevel, true, false);
    return;
    }

//if the new gate's location is encompassed by an existing gate, shift
//all later gates down and insert the new gate, adjusting the width of the
//old gate to give space for the new gate

int i;
for (i = 0; i < numberOfDACGates; i++)
    if (dacGates[i].getActive())
        if (pStart >= dacGates[i].gatePixStart
                                           && pStart <= dacGates[i].gatePixEnd)
            break;

//the end of the new gate will equal the start of the following gate
//if there is no following gate, then the end will be the end of the old gate
int pEnd;

if (i < lastGate)
    pEnd = dacGates[i+1].gatePixStart;
else
    pEnd = dacGates[i].gatePixEnd;

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

if (pGate < 0 || pGate >= numberOfDACGates) return; //protect against invalid

int lastGate = getActiveDACGateCount() - 1; //need this in a second

//shift all gates above the one being deleted down one slot
shiftDACGatesDown(pGate+1);

//disable the old last gate slot - that one has been moved down one slot
setDACActive(lastGate, false, false);

//set the end of the gate before the deleted one to the start of the gate
//which was after the deleted one to avoid diagonal lines
//don't do this if the gates involved are at the ends of the array

if (pGate > 0 && pGate < getActiveDACGateCount())
    setDACPixEnd(pGate - 1, dacGates[pGate].gatePixStart, false);

}//end of Channel::deleteDACGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::deleteAllDACGates
//
// Deletes all the DAC gates by setting their active states to false.
//

public void deleteAllDACGates()
{

for (int i = 0; i < numberOfDACGates; i++) setDACActive(i, false, false);

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
if (newFirstGate < 0) return; //protect against shifting out of bounds

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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public void shiftDACGatesUp(int pStart)
{

int newLastGate = getActiveDACGateCount();
if (newLastGate >= numberOfDACGates) return; //protect against full array

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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setDACPixStart(
                                   int pGate, int pStart, boolean pForceUpdate)
{

if (pStart != dacGates[pGate].gatePixStart) pForceUpdate = true;

dacGates[pGate].gatePixStart = pStart;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true); dataChanged = true;
    dacGateParamsChanged = true;  dacGates[pGate].parametersChanged = true;
    }

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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setDACPixEnd(
                                    int pGate, int pEnd, boolean pForceUpdate)
{

if (pEnd != dacGates[pGate].gatePixEnd) pForceUpdate = true;

dacGates[pGate].gatePixEnd = pEnd;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true); dataChanged = true;
    dacGateParamsChanged = true;  dacGates[pGate].parametersChanged = true;
    }

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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setDACPixLevel(
                                   int pGate, int pLevel, boolean pForceUpdate)
{

if (pLevel != dacGates[pGate].gatePixLevel) pForceUpdate = true;

dacGates[pGate].gatePixLevel = pLevel;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true); dataChanged = true;
    dacGateParamsChanged = true;  dacGates[pGate].parametersChanged = true;
    }

}//end of Channel::setDACPixLevel
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

public Gate getGate(int pGate)
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
// Returns true if the channel On and Not Masked.  Returns false otherwise so
// so its data won't be used.
//

public boolean getNewData(int pGate, HardwareVars hdwVs)
{

if (channelOn && !channelMasked){
    gates[pGate].getNewData(hdwVs);
    return(true);
    }
else{
    gates[pGate].getNewData(hdwVs);
    return(false);
    }

}//end of Channel::getNewData
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
// Channel::setFlags1
//
// Sets mask word to set one or more bits in the DSP's flags1 variable.
// To set a particular bit in the flag, the corresponding bit in pSetMask
// should be set to a 1.  Any bit in pSetMask which is a 0 is ignored.
//
// The command is always sent to the DSP regardless of it being a new value as
// it does not require much overhead and is infrequently used.
//
// NOTE: A delay should be inserted between calls to setFlags1 and clearFlags1
// as they are actually sent to the remotes by another thread.  The delay
// should be long enough to ensure that the other thread has had time to send
// the first command.
//
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setFlags1(int pSetMask)
{

flags1SetMask = pSetMask;

channelOwner.setChannelDataChangedFlag(true);
dataChanged = true; flags1SetMaskChanged = true;

}//end of Channel::setFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendSetFlags1
//
// Sends the flags1SetMask to the remotes to set bits in variable flags1.
// See setFlags1 function for more info.
//
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendSetFlags1()
{

utBoard.sendSetFlags1(boardChannel, flags1SetMask);

flags1SetMaskChanged = false;

}//end of Channel::sendSetFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::clearFlags1
//
// Sets mask word to clear one or more bits in the DSP's flags1 variable.
// To clear a particular bit in the flag, the corresponding bit in pSetMask
// should be set to a 0.  Any bit in pSetMask which is a 1 is ignored.
//
// NOTE: A delay should be inserted between calls to setFlags1 and clearFlags1
// as they are actually sent to the remotes by another thread.  The delay
// should be long enough to ensure that the other thread has had time to send
// the first command.
//
// The command is always sent to the DSP regardless of it being a new value as
// it does not require much overhead and is infrequently used.
//
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void clearFlags1(int pClearMask)
{

flags1ClearMask = pClearMask;

channelOwner.setChannelDataChangedFlag(true);
dataChanged = true; flags1ClearMaskChanged = true;

}//end of Channel::clearFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendClearFlags1
//
// Sends the flags1ClearMask to the remotes to clear bits in variable flags1.
// See clearFlags1 function for more info.
//
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendClearFlags1()
{

utBoard.sendClearFlags1(boardChannel, flags1ClearMask);

flags1ClearMaskChanged = false;

}//end of Channel::sendClearFlags1
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
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setDACEnabled(boolean pEnable, boolean pForceUpdate)
{

if (pEnable != dacEnabled) pForceUpdate = true;

dacEnabled = pEnable;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    if (dacEnabled){
        flags1SetMask = UTBoard.DAC_ENABLED;
        flags1SetMaskChanged = true;
        }
    else{
        flags1ClearMask = ~UTBoard.DAC_ENABLED;
        flags1ClearMaskChanged = true;
        }
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true;
    }

}//end of Channel::setDACEnabled
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
    for (int i = 0; i < numberOfGates; i++)
        if (gates[i].gateStart < gates[firstGate].gateStart) firstGate = i;

    //find the latest gate

    int lastGate = 0;
    for (int i = 0; i < numberOfGates; i++)
        if ((gates[i].gateStart + gates[i].gateWidth)
                    > (gates[lastGate].gateStart + gates[lastGate].gateWidth))
            lastGate = i;

    //absolute positioning

    //calculate the position (in samples) of the leading edge of first gate
    firstGateEdgePos = (int)(gates[firstGate].gateStart / uSPerDataPoint);

    //calculate the position in number of samples of trailing edge of last gate
    lastGateEdgePos = (int)((gates[lastGate].gateStart
                                + gates[lastGate].gateWidth)  / uSPerDataPoint);

    }// if (!interfaceTracking)
else{

    int interfaceGateLead = (int)(gates[0].gateStart / uSPerDataPoint);
    int interfaceGateTrail =
            (int)((gates[0].gateStart + gates[0].gateWidth) / uSPerDataPoint);

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
    for (int i = 1; i < numberOfGates; i++)
        if (gates[i].gateStart < gates[firstGate].gateStart) firstGate = i;

    //find the latest gate, not including the interface gate (see note above)

    int lastGate = 1;
    for (int i = 1; i < numberOfGates; i++)
        if ((gates[i].gateStart + gates[i].gateWidth)
                    > (gates[lastGate].gateStart + gates[lastGate].gateWidth))
            lastGate = i;

    //positioning relative to interface gate
    //interface gate is always gate 0

    //calculate the position (in samples) of the leading edge of first gate
    //relative to the leading edge of interface gate (worst case)
    firstGateEdgePos = (int)(
            (gates[0].gateStart + gates[firstGate].gateStart) / uSPerDataPoint);

    //if the interface gate is before all other gates, use its position
    if (interfaceGateLead < firstGateEdgePos)
        firstGateEdgePos = interfaceGateLead;

    //calculate the position in number of samples of trailing edge of last gate
    //relative to the trailing edge of interface gate (worst case)
    lastGateEdgePos = (int)(
            (gates[0].gateStart + gates[0].gateWidth +
            gates[lastGate].gateStart + gates[lastGate].gateWidth)
            / uSPerDataPoint);

    //if the interface gate is after all other gates, use its position
    if (interfaceGateTrail > lastGateEdgePos)
        lastGateEdgePos = interfaceGateTrail;

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
// data is software delay.  The AScan delay equals hardware delay plus
// software delay.
//
// NOTE: This function must be called anytime a gate's position is changed so
// that the sampling start can be adjusted to include the entire gate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setDelay(double pDelay, boolean pForceUpdate)
{

int oldHardwareDelay = hardwareDelay;
int oldSoftwareDelay = softwareDelay;

aScanDelay = pDelay;

//calculate the number of samples to skip based on the delay in microseconds
hardwareDelay = (int)(aScanDelay / uSPerDataPoint);
int totalDelayCount = hardwareDelay;

//the FPGA sample delay CANNOT be later than the delay to the earliest gate
//because the gate cannot be processed if samples aren't taken for it
//if the earliest gate is sooner than the delay, then override the FPGA
//delay - the remaining delay for an AScan will be accounted for by setting
//a the aScanDelay in the DSP

if (firstGateEdgePos < hardwareDelay) hardwareDelay = firstGateEdgePos;

if (hardwareDelay != oldHardwareDelay) pForceUpdate = true;

if (utBoard != null && pForceUpdate)
    utBoard.sendHardwareDelay(boardChannel, hardwareDelay);

//calculate and set the remaining delay left over required to positon the AScan
//correctly after taking into account the FPGA sample delay

softwareDelay = totalDelayCount - hardwareDelay;

if (softwareDelay != oldSoftwareDelay) pForceUpdate = true;

if (utBoard != null && pForceUpdate)
    utBoard.sendSoftwareDelay(boardChannel, softwareDelay, hardwareDelay);

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
// Channel::setRejectLevel
//
// Sets the reject level which is a percentage of the screen height below
// which the signal is attenuated nearly to zero.
//

public void setRejectLevel(int pRejectLevel, boolean pForceUpdate)
{

if (pRejectLevel != rejectLevel) pForceUpdate = true;

rejectLevel = pRejectLevel;

if (utBoard != null && pForceUpdate)
    utBoard.setRejectLevel(boardChannel, rejectLevel * scopeMax / 100);

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

public void setAScanSmoothing(int pAScanSmoothing, boolean pForceUpdate)
{

if (pAScanSmoothing != aScanSmoothing) pForceUpdate = true;

aScanSmoothing = pAScanSmoothing;

if (utBoard != null && pForceUpdate)
    utBoard.setAScanSmoothing(boardChannel, aScanSmoothing);

}//end of Channel::setAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getAScanSmoothing
//
// Returns the amount of smoothing (averaging) for the aScan display.
//

public int getAScanSmoothing()
{

return aScanSmoothing;

}//end of Channel::getAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setDCOffset
//
// Sets the DC offset.
//

public void setDCOffset(int pDCOffset, boolean pForceUpdate)
{

if (pDCOffset != dcOffset) pForceUpdate = true;

dcOffset = pDCOffset;

//divide the input value by 4.857 mv/Count
//AD converter span is 1.2 V, 256 counts
//this will give a value of zero offset until input value is +/-5, then it
//will equal 1 - the value will only change every 5 or so counts because the
//AD resolution is 4+ mV and the input value is mV

if (utBoard != null && pForceUpdate)
    utBoard.sendDCOffset(boardChannel, (int)(dcOffset / 4.6875));

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

if (pMode != mode) pForceUpdate = true;

mode = pMode;

boolean lChannelOn = (mode != UTBoard.CHANNEL_OFF) ? true : false;

//update the tranducer settings as the on/off status is set that way
setTransducer(lChannelOn, pulseBank, pulseChannel, pForceUpdate);

if (utBoard != null && pForceUpdate) utBoard.sendMode(boardChannel, mode);

}//end of Channel::setMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getMode
//
// Returns the signal mode setting: rectification style or off.
//

public int getMode()
{

return mode;

}//end of Channel::getMode
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
// The DSP always returns 400 samples for an AScan dataset.  The softwareRange
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

int oldHardwareRange = hardwareRange;
int oldSoftwareRange = softwareRange;

aScanRange = pRange;

//calculate the position of the left edge of the AScan in sample counts
int leftEdgeAScan = hardwareDelay + softwareDelay;

//calculate the position of the right edge of the AScan in sample counts

int rightEdgeAScan = leftEdgeAScan + (int)(aScanRange / uSPerDataPoint);

int start, stop;

//determine the earliest event for which samples are required - the left edge
//of the AScan or the leading edge of the first gate, which ever is first

if (leftEdgeAScan <= firstGateEdgePos) start = leftEdgeAScan;
else start = firstGateEdgePos;

//determine the latest event for which samples are required - the right edge
//of the AScan or the trailing edge of the last gate, which ever is last

if (rightEdgeAScan >= lastGateEdgePos) stop = rightEdgeAScan;
else stop = lastGateEdgePos;

//calculate the number of required samples to cover the range specified
hardwareRange = stop - start;

//force sample size to be even - see notes at top of UTboard.setHardwareRange
//for more info
if (hardwareRange % 2 != 0) hardwareRange++;

//calculate the compression needed to fit at least the desired number of AScan
//samples into the 400 sample AScan buffer - the scale factor is rounded up
//rather than down to make sure the desired range is collected

softwareRange = (rightEdgeAScan - leftEdgeAScan) / UTBoard.ASCAN_SAMPLE_SIZE;

//if the range is not a perfect integer, round it up
if (((rightEdgeAScan - leftEdgeAScan) % UTBoard.ASCAN_SAMPLE_SIZE) != 0)
    softwareRange++;

if (hardwareRange != oldHardwareRange) pForceUpdate = true;
if (softwareRange != oldSoftwareRange) pForceUpdate = true;

if (utBoard != null && pForceUpdate){

    utBoard.sendHardwareRange(boardChannel, hardwareRange);

    //tell the DSP cores how big the sample set is
    utBoard.sendDSPSampleSize(boardChannel, hardwareRange);

    utBoard.sendSoftwareRange(boardChannel, softwareRange);

    }

}//end of Channel::setRange
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

if (utBoard != null)
    utBoard.sendSampleBufferStart(boardChannel,
                                            UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);

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

if (pSoftwareGain != softwareGain) pForceUpdate = true;

softwareGain = pSoftwareGain;

if (utBoard != null && pForceUpdate)
    utBoard.sendSoftwareGain(boardChannel, pSoftwareGain);

}//end of Channel::setSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getSoftwareGain
//
// Returns the software gain for the DSP.
//

public double getSoftwareGain()
{

return softwareGain;

}//end of Channel::getSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::setHardwareGain
//
// Sets the amount of amplification for the two programmable hardware gain
// stages.  Each value can be between 1 and 16.
//

public void setHardwareGain(int pHardwareGain1, int pHardwareGain2,
                                                           boolean pForceUpdate)
{

if ((pHardwareGain1 != hardwareGain1) || (pHardwareGain2 != hardwareGain2 ))
    pForceUpdate = true;

hardwareGain1 = pHardwareGain1; hardwareGain2 = pHardwareGain2;

if (utBoard != null && pForceUpdate)
    utBoard.sendHardwareGain(boardChannel, hardwareGain1, hardwareGain2);

}//end of Channel::setHardwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getHardwareGain1
//
// Returns the amount of amplification for the first stage programmable
// hardware gain.
//

public int getHardwareGain1()
{

return hardwareGain1;

}//end of Channel::getHardwareGain1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getHardwareGain2
//
// Returns the amount of amplification for the first stage programmable
// hardware gain.
//

public int getHardwareGain2()
{

return hardwareGain2;

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

public void setInterfaceTracking(boolean pOn, boolean pForceUpdate)
{

if (pOn != interfaceTracking) pForceUpdate = true;

interfaceTracking = pOn;

//switch gate start positions to the appropriate values for the current mode
for (int i = 0; i < numberOfGates; i++)
    setGateStart(i, interfaceTracking ?
           gates[i].gateStartTrackingOn : gates[i].gateStartTrackingOff, false);

//determine the span from the earliest gate edge to the latest (in time)
calculateGateSpan();

if (utBoard != null && pForceUpdate){
    for (int i = 0; i < numberOfGates; i++){
        gates[i].setInterfaceTracking(pOn);
        gates[i].flagsChanged = true;
        }

    for (int i = 0; i < numberOfDACGates; i++){
        dacGates[i].setInterfaceTracking(pOn);
        dacGates[i].flagsChanged = true;
        }

    channelOwner.setChannelDataChangedFlag(true); dataChanged = true;
    gateFlagsChanged = true; dacGateFlagsChanged = true;

    }// if (utBoard != null && pForceUpdate)
   
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

if (pChannelOn != channelOn) pForceUpdate = true;
if (pPulseBank != pulseBank) pForceUpdate = true;
if (pPulseChannel != pulseChannel) pForceUpdate = true;

channelOn = pChannelOn; pulseBank = pPulseBank; pulseChannel = pPulseChannel;

channelOn = (mode != UTBoard.CHANNEL_OFF) ? true : false;

if (utBoard != null && pForceUpdate) 
    utBoard.sendTransducer(boardChannel,
                 (byte)(channelOn ? 1:0), (byte)pulseBank, (byte)pulseChannel);

}//end of Channel::setTransducer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::linkTraces
//
// This function is called by traces to link their buffers to specific hardware
// channels/gates and give a link back to variables in the Trace object.
//

public void linkTraces(int pChartGroup, int pChart, int pTrace, int[] pDBuffer,
   int[] pDBuffer2, int[] pFBuffer, Threshold[] pThresholds, int pPlotStyle,
   Trace pTracePtr)
{

for (int i = 0; i < numberOfGates; i++)
        gates[i].linkTraces(pChartGroup, pChart, pTrace, pDBuffer, pDBuffer2,
                         pFBuffer, pThresholds, pPlotStyle, pTracePtr);

}//end of Channel::linkTraces
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

chassisAddr = pConfigFile.readInt(whichChannel, "Chassis", 1);

slotAddr = pConfigFile.readInt(whichChannel, "Slot", 1);

boardChannel = pConfigFile.readInt(whichChannel, "Board Channel", 1) - 1;

pulseChannel = pConfigFile.readInt(whichChannel, "Pulse Channel", 1) - 1;

pulseBank = pConfigFile.readInt(whichChannel, "Pulse Bank", 1) - 1;

type = pConfigFile.readString(whichChannel, "Type", "Other");

numberOfGates = pConfigFile.readInt(whichChannel, "Number Of Gates", 3);

numberOfDACGates = pConfigFile.readInt(whichChannel, "Number Of DAC Gates", 10);

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
    if (numberOfGates > 20) numberOfGates = 20;
    
    gates = new Gate[numberOfGates];

    for (int i = 0; i < numberOfGates; i++)
        gates[i] = new Gate(configFile, channelIndex, i);

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
    if (numberOfDACGates > 20) numberOfDACGates = 20;

    dacGates = new DACGate[numberOfDACGates];

    for (int i = 0; i < numberOfDACGates; i++)
        dacGates[i] = new DACGate(configFile, channelIndex, i);

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

if (utBoard != null)
    utBoard.requestAScan(boardChannel, hardwareDelay);

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

if (utBoard != null)
    return utBoard.getAScan();
else
    return null;

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

if (utBoard != null) utBoard.requestPeakData(boardChannel);

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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setGateStart(
                                int pGate, double pStart, boolean pForceUpdate)
{

if (pStart != gates[pGate].gateStart) pForceUpdate = true;

gates[pGate].gateStart = pStart;

//store the variable as appropriate for the interface tracking mode - this
//allows switching back and forth between modes
if (interfaceTracking)
    gates[pGate].gateStartTrackingOn = pStart;
else
    gates[pGate].gateStartTrackingOff = pStart;

//determine the span from the earliest gate edge to the latest (in time)
calculateGateSpan();

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; gateParamsChanged = true;
    gates[pGate].parametersChanged = true;
    }

}//end of Channel::setGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateStart
//
// Returns the gate start position of pGate.
//

public double getGateStart(int pGate)
{

return gates[pGate].gateStart;

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
// This and all functions which set the gateParamsChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setGateWidth(
                                int pGate, double pWidth, boolean pForceUpdate)
{

if (pWidth != gates[pGate].gateWidth) pForceUpdate = true;

gates[pGate].gateWidth = pWidth;

//determine the span from the earliest gate edge to the latest (in time)
calculateGateSpan();

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; gateParamsChanged = true;
    gates[pGate].parametersChanged = true;
    }

}//end of Channel::setGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateWidth
//
// Gets the gate width of pGate.
//

public double getGateWidth(int pGate)
{

return gates[pGate].gateWidth;

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

return gates[pGate].gateFlags;

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
// This and all functions which set the gateParamsChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setGateLevel(
                                   int pGate, int pLevel, boolean pForceUpdate)
{

if (pLevel != gates[pGate].gateLevel) pForceUpdate = true;

gates[pGate].gateLevel = pLevel;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; gateParamsChanged = true;
    gates[pGate].parametersChanged = true;
    }

}//end of Channel::setGateLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getGateLevel
//
// Gets the gate level of pGate.
//

public int getGateLevel(int pGate)
{

return gates[pGate].gateLevel;

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
// This and all functions which set the gateHitMissChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setGateHitCount(
                                int pGate, int pHitCount, boolean pForceUpdate)
{

if (pHitCount != gates[pGate].gateHitCount) pForceUpdate = true;

gates[pGate].gateHitCount = pHitCount;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; gateHitMissChanged = true;
    gates[pGate].hitMissChanged = true;
    }

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

return gates[pGate].gateHitCount;

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
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setGateMissCount(
                               int pGate, int pMissCount, boolean pForceUpdate)
{

if (pMissCount != gates[pGate].gateMissCount) pForceUpdate = true;

gates[pGate].gateMissCount = pMissCount;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; gateHitMissChanged = true;
    gates[pGate].hitMissChanged = true;
    }

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

return gates[pGate].gateMissCount;

}//end of Channel::getGateMissCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateParameters
//
// Sends gate start, width, and level to the remotes.
//
// This and all functions which set data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendGateParameters()
{

//unknown which gate(s) have changed data, so check them all
//clear the flags even if utBoard is null so they won't be checked again

for (int i = 0; i < numberOfGates; i++){

    if (gates[i].parametersChanged == true){

        if (utBoard != null)
            utBoard.sendGate(boardChannel, i,
                (int)(gates[i].gateStart / uSPerDataPoint),
                (int)(gates[i].gateWidth / uSPerDataPoint),
                gates[i].gateLevel);

        gates[i].parametersChanged = false;

        }// if (gates[i].parametersChanged == true)
    }// for (int i = 0; i < numberOfGates; i++)

//clear the flag
gateParamsChanged = false;

}//end of Channel::sendGateParameters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateFlags
//
// Sends gate flags to the remotes.
//
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendGateFlags()
{

//unknown which gate(s) have changed data, so check them all
//clear the flags even if utBoard is null so they won't be checked again

for (int i = 0; i < numberOfGates; i++){

    if (gates[i].flagsChanged == true){

        if (utBoard != null)
            utBoard.sendGateFlags(boardChannel, i, gates[i].getFlags());

        gates[i].flagsChanged = false;

        }// if (gates[i].parametersChanged == true)
    }// for (int i = 0; i < numberOfGates; i++)

//clear the flag
gateFlagsChanged = false;

}//end of Channel::sendGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendGateHitMiss
//
// Sends gate hit and miss values to the remotes.
//
// This and all functions which set the gateHitMissChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendGateHitMiss()
{

//unknown which gate(s) have changed data, so check them all
//clear the flags even if utBoard is null so they won't be checked again

for (int i = 0; i < numberOfGates; i++){

    if (gates[i].hitMissChanged == true){

        if (utBoard != null)
            utBoard.sendHitMissCounts(boardChannel, i,
                        gates[i].gateHitCount, gates[i].gateMissCount);
        
        gates[i].hitMissChanged = false;

        }// if (gates[i].hitMissChanged == true)
    }// for (int i = 0; i < numberOfGates; i++)

//clear the flag
gateHitMissChanged = false;

}//end of Channel::sendGateHitMiss
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDACGateParameters
//
// Sends DAC gate start, width, and level to the remotes.
//
// This and all functions which set the dacGateParamsChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendDACGateParameters()
{

//unknown which gate(s) have changed data, so check them all
//clear the flags even if utBoard is null so they won't be checked again

for (int i = 0; i < numberOfDACGates; i++){

    if (dacGates[i].parametersChanged == true){

        if (utBoard != null){
        
            //the DAC's gain value depends upon the value of softwareGain and
            //the DAC's level in pixels
            //at 50% scope height, the DAC's gain equals softwareGain
            //it is desirable to have a +/- 20dB adjustment for each DAC gate
            //thus each pixel above or below mid-height will raise or lower gain

            //calculate number of dB per pixel to get +/-20dB total
            double dBPerPix = 20.0 / (scopeMax/2);
            //convert gateLevel from percentage of screen height to pixels
            int pixLevel =
                   (int)Math.round(dacGates[i].gateLevel * scopeMax / 100);
            //calculate distance from the center
            int fromCenter = pixLevel - (scopeMax/2);
            //calculate gain for the DAC section
            double gain = softwareGain + fromCenter * dBPerPix;

            //convert decibels to linear gain: dB = 20 * log10(gain)
            //see notes in UTBoard.sendSoftwareGain for details
            gain = Math.pow(10, gain/20);

            gain *= 6.476;

            int roundedGain = (int)Math.round(gain);

            utBoard.sendDAC(boardChannel, i,
                (int)(dacGates[i].gateStart / uSPerDataPoint),
                (int)(dacGates[i].gateWidth / uSPerDataPoint),
                 roundedGain);

            }

        dacGates[i].parametersChanged = false;

        }// if (dacGates[i].parametersChanged == true)
    }// for (int i = 0; i < numberOfDACGates; i++)

//clear the flag
dacGateParamsChanged = false;

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
// This and all functions which set the data change flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void setDACActive(int pGate, boolean pValue,
                                                          boolean pForceUpdate)
{

if (pValue != dacGates[pGate].getActive()) pForceUpdate = true;

dacGates[pGate].setActive(pValue);

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; dacGateFlagsChanged = true;
    dacGates[pGate].flagsChanged = true;
    }

}//end of Channel::setDACActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDACGateFlags
//
// Sends DAC flags to the remotes.
//
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void sendDACGateFlags()
{

//unknown which gate(s) have changed data, so check them all
//clear the flags even if utBoard is null so they won't be checked again

for (int i = 0; i < numberOfDACGates; i++){

    if (dacGates[i].flagsChanged == true){

        if (utBoard != null)
            utBoard.sendDACGateFlags(boardChannel, i, dacGates[i].getFlags());

        dacGates[i].flagsChanged = false;

        }// if (dacGates[i].parametersChanged == true)
    }// for (int i = 0; i < numberOfDACGates; i++)

//clear the flag
dacGateFlagsChanged = false;

}//end of Channel::sendDACGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::calculateDACGateTimeLocation
//
// Forces the specified DAC gate to update its position, width, and level time
// values by calculating them from its pixel location values.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//
// This and all functions which set the gateParamsChanged flag should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void calculateDACGateTimeLocation(int pDACGate,
       double pUSPerPixel, int pDelayPix, int pCanvasHeight, int pVertOffset,
                                                          boolean pForceUpdate)
{

boolean valueChanged = dacGates[pDACGate].calculateGateTimeLocation(
                           pUSPerPixel, pDelayPix, pCanvasHeight, pVertOffset);

if (valueChanged) pForceUpdate = true;

//flag that new data needs to be sent to remotes
if (pForceUpdate){
    channelOwner.setChannelDataChangedFlag(true);
    dataChanged = true; dacGateParamsChanged = true;
    dacGates[pDACGate].parametersChanged = true;
    }

}//end of Channel::calculateDACGateTimeLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::sendDataChangesToRemotes
//
// If any data has been changed, sends the changes to the remotes.
//
// This and all functions which set the change flags should be synchronized to
// avoid thread conficts.  Typically, one thread changes the data while another
// transmits it to the remotes.
//

public synchronized void sendDataChangesToRemotes()
{

if (!dataChanged) return; //do nothing if not data changed

if (gateParamsChanged) sendGateParameters();

if (gateFlagsChanged) sendGateFlags();

if (gateHitMissChanged) sendGateHitMiss();

if (dacGateParamsChanged) sendDACGateParameters();

if (dacGateFlagsChanged) sendDACGateFlags();

if (flags1SetMaskChanged) sendSetFlags1();

if (flags1ClearMaskChanged) sendClearFlags1();

dataChanged = false;  //clear the flag global flag

}//end of Channel::sendDataChangesToRemotes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateStart
//
// Returns the DAC gate start position of pGate.
//

public double getDACGateStart(int pGate)
{

return dacGates[pGate].gateStart;

}//end of Channel::getDACGateStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateWidth
//
// Gets the DAC gate width of pGate.
//

public double getDACGateWidth(int pGate)
{

return dacGates[pGate].gateWidth;

}//end of Channel::getDACGateWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Channel::getDACGateLevel
//
// Gets the DAC gate level of pGate.
//

public int getDACGateLevel(int pGate)
{

return dacGates[pGate].gateLevel;

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

public synchronized void setAllDACGateDataChangedFlags(boolean pValue)
{

//not used at this time

}//end of Channel::setAllDACGateDataChangedFlags
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
softwareGain = pCalFile.readDouble(section, "Software Gain", 0);
hardwareGain1 = pCalFile.readInt(section, "Hardware Gain Stage 1", 2);
hardwareGain2 = pCalFile.readInt(section, "Hardware Gain Stage 2", 1);
interfaceTracking = pCalFile.readBoolean(section, "Interface Tracking", false);
dacEnabled = pCalFile.readBoolean(section, "DAC Enabled", false);
mode = pCalFile.readInt(section, "Signal Mode", 0);
channelOn = (mode != UTBoard.CHANNEL_OFF) ? true : false;
rejectLevel = pCalFile.readInt(section, "Reject Level", 0);
aScanSmoothing = pCalFile.readInt(section, "AScan Display Smoothing", 1);

// call each gate to load its data
for (int i = 0; i < numberOfGates; i++) gates[i].loadCalFile(pCalFile);

// call each DAC gate to load its data
for (int i = 0; i < numberOfDACGates; i++) dacGates[i].loadCalFile(pCalFile);

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
pCalFile.writeDouble(section, "Software Gain", softwareGain);
pCalFile.writeInt(section, "Hardware Gain Stage 1", hardwareGain1);
pCalFile.writeInt(section, "Hardware Gain Stage 2", hardwareGain2);
pCalFile.writeBoolean(section, "Interface Tracking", interfaceTracking);
pCalFile.writeBoolean(section, "DAC Enabled", dacEnabled);
pCalFile.writeInt(section, "Signal Mode", mode);
pCalFile.writeInt(section, "Reject Level", rejectLevel);
pCalFile.writeInt(section, "AScan Display Smoothing", aScanSmoothing);

// call each gate to save its data        
for (int i = 0; i < numberOfGates; i++)
    gates[i].saveCalFile(pCalFile);

// call each DAC gate to save its data
for (int i = 0; i < numberOfDACGates; i++)
   dacGates[i].saveCalFile(pCalFile);

}//end of Channel::saveCalFile
//-----------------------------------------------------------------------------

}//end of class Channel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
