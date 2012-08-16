/******************************************************************************
* Title: Hardware.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class handles the hardware interfaces.  It will create and use the
* appropriate interface based on information loaded from the configuration file.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import javax.swing.*;

import chart.MessageLink;
import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;
import chart.ThreadSafeLogger;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.ChartGroup;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Hardware
//
// This class creates and handles the hardware interface.
//

public class Hardware extends Object implements TraceValueCalculator, Runnable,
                                                                   MessageLink {

static public int SCAN = 0, INSPECT = 1, STOPPED = 2;
static public int INSPECT_WITH_TIMER_TRACKING = 3, PAUSED = 4;
int opMode = STOPPED;

ThreadSafeLogger logger;

//debug mks -- needs to be loaded from config file -- specifies if carriage
//moving away is increasing or decreasing encoder counts
int AwayDirection;

double encoder1InchesPerCount;
double encoder2InchesPerCount;
public double pixelsPerInch;

int prevPixPosition;

boolean output1On = false;

Globals globals;
public ChartGroup chartGroups[];

public HardwareVars hdwVs;
IniFile configFile;
HardwareLink analogDriver;
HardwareLink digitalDriver;
int numberOfAnalogChannels;
JTextArea log;
int scanRateCounter;

InspectControlVars inspectCtrlVars;

public static int ALL_RABBITS = 0;
public static int UT_RABBITS = 1;
public static int CONTROL_RABBITS = 2;

public boolean connected = false;
boolean collectDataEnabled = true;

//variables used by functions - declared here to avoid garbage collection
int numberOfChannels;
int numberOfGates;

String analogDriverName;
String digitalDriverName;

boolean manualInspectControl = false;

public static int INCHES = 0, MM = 1;
public int units = INCHES;

public static int TIME = 0, DISTANCE = 1;
public int unitsTimeDistance = TIME;

public static int PULSE = 0, CONTINUOUS = 1;
public int markerMode = PULSE;

double photoEye1DistanceFrontOfHead1;
double photoEye1DistanceFrontOfHead2;

double photoEye2DistanceFrontOfHead1;
double photoEye2DistanceFrontOfHead2;

//-----------------------------------------------------------------------------
// Hardware::Hardware (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Hardware(IniFile pConfigFile, Globals pGlobals, JTextArea pLog)
{

hdwVs = new HardwareVars(); configFile = pConfigFile; log = pLog;
globals = pGlobals;

logger = new ThreadSafeLogger(pLog);

inspectCtrlVars = new InspectControlVars();

//load configuration settings
configure(configFile);

}//end of Hardware::Hardware (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::configure
//
// Loads configuration settings from the configuration.ini file.
//
// Only configuration data for this class are loaded here.  Each
// child object should be allowed to load its own data.
//

private void configure(IniFile pConfigFile)
{

analogDriverName =
  pConfigFile.readString("Hardware", "Analog Input Driver Name", "PCI-DAS6023");

digitalDriverName =
  pConfigFile.readString("Hardware", "Digital Input Driver Name", "PCI-QUAD04");

numberOfAnalogChannels =
  pConfigFile.readInt("Hardware", "Number of Analog Channels", 50);

//load the nS per data point value and compute the uS per data point as well
hdwVs.nSPerDataPoint =
  pConfigFile.readDouble("Hardware", "nS per Data Point", 15.0);
hdwVs.uSPerDataPoint = hdwVs.nSPerDataPoint / 1000;

photoEye1DistanceFrontOfHead1 = pConfigFile.readDouble("Hardware",
                        "Photo Eye 1 Distance to Front Edge of Head 1", 22.0);

photoEye1DistanceFrontOfHead2 = pConfigFile.readDouble("Hardware",
                        "Photo Eye 1 Distance to Front Edge of Head 2", 46.0);

photoEye2DistanceFrontOfHead1 = pConfigFile.readDouble("Hardware",
                        "Photo Eye 2 Distance to Front Edge of Head 1", 58.0);

photoEye2DistanceFrontOfHead2 = pConfigFile.readDouble("Hardware",
                        "Photo Eye 2 Distance to Front Edge of Head 2", 35.0);

//the control board sends packets every so many counts and is susceptible to
//cumulative round off error, but the values below can be tweaked to give
//accurate results over the length of the piece -- the packet send trigger
//counts are often the same as the values below

encoder1InchesPerCount =
    pConfigFile.readDouble("Hardware", "Encoder 1 Inches Per Count", 0.003);

encoder2InchesPerCount =
    pConfigFile.readDouble("Hardware", "Encoder 2 Inches Per Count", 0.003);

pixelsPerInch = pConfigFile.readDouble("Hardware", "Pixels per Inch", 1.0);

manualInspectControl = pConfigFile.readBoolean(
        "Hardware", "Manual Inspection Start/Stop Control", false);

if (numberOfAnalogChannels > 100) numberOfAnalogChannels = 100;

createAnalogDriver(analogDriverName);

}//end of Hardware::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::createAnalogDriver
//
// Creates a driver to handle the analog input board.  This board may also
// handle the digital I/O, in which case digitalDriver should be set equal to
// analogDriver to that the same driver gets called for both types of input.
//

private void createAnalogDriver(String pDriverName)
{

if (pDriverName.equalsIgnoreCase("PCI-DAS6023")) analogDriver =
          new AnalogPCIDAS6023(configFile, true, numberOfAnalogChannels, hdwVs);

if (pDriverName.equalsIgnoreCase("Capulin 1")) analogDriver =
    new Capulin1(configFile, true, numberOfAnalogChannels, hdwVs, log,
                                 globals.jobFileFormat, Globals.mainFileFormat);

}//end of Hardware::createAnalogDriver
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::connect
//
// Establishes connections with remote devices.
//

public void connect()
{

logger.section();
logger.logMessage("Connecting With Chassis and Configuring\n\n");

connected = true;

//before attempting to connect with the boards, start a thread to run the
//simulation functions so the simulated boards can respond

if (analogDriver.getSimulate()){
    Thread thread = new Thread(this); thread.start();
    }

analogDriver.connect();

//calculate the trace offsets from the point where the photo eye detects the
//pipe so the traces can be delayed until their sensors reach the inspection
//piece
calculateTraceOffsetDelays();

logger.logMessage("\nChassis configuration complete.\n");

logger.saveToFile("Chassis Configuration Log");

}//end of Hardware::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::calculateTraceOffsetDelays
//
// Adds the appropriate photo eye distance to the front of each head to each
// trace's distance from the front edge of its head.
//
// These offsets are used to delay the trace after the photo eye detects the
// pipe until the sensor(s) associated to that trace reach the pipe.
//

void calculateTraceOffsetDelays()
{

Trace tracePtr;

for (int cg = 0; cg < chartGroups.length; cg++){

    int nSC = chartGroups[cg].getNumberOfStripCharts();

    for (int sc = 0; sc < nSC; sc++){

        int nTr = chartGroups[cg].getStripChart(sc).getNumberOfTraces();

        for (int tr = 0; tr < nTr; tr++){

            tracePtr = chartGroups[cg].getStripChart(sc).getTrace(tr);


            if ((tracePtr != null) && (tracePtr.head == 1)){
                tracePtr.startFwdDelayDistance =
                        photoEye1DistanceFrontOfHead1
                                    + tracePtr.distanceSensorToFrontEdgeOfHead;

                tracePtr.startRevDelayDistance =
                        photoEye2DistanceFrontOfHead1 -
                                    tracePtr.distanceSensorToFrontEdgeOfHead;

            }//if ((tracePtr != null) && (tracePtr.head == 1))

            if ((tracePtr != null) && (tracePtr.head == 2)){
                tracePtr.startFwdDelayDistance =
                        photoEye1DistanceFrontOfHead2
                                    + tracePtr.distanceSensorToFrontEdgeOfHead;

                tracePtr.startRevDelayDistance =
                        photoEye2DistanceFrontOfHead2 -
                                    tracePtr.distanceSensorToFrontEdgeOfHead;

            }//if ((tracePtr != null) && (tracePtr.head == 2))

        }//for (int tr = 0; tr < nTr; tr++)
    }//for (int sc = 0; sc < nSC; sc++)
}//for (int cg = 0; cg < chartGroups.length; cg++)

}//end of Hardware::calculateTraceOffsetDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

hdwVs.nominalWall = pCalFile.readDouble("Hardware", "Nominal Wall", 0.250);

hdwVs.nominalWallChartPosition =
               pCalFile.readInt("Hardware", "Nominal Wall Chart Position", 50);

hdwVs.wallChartScale =
                    pCalFile.readDouble("Hardware", "Wall Chart Scale", 0.002);

hdwVs.velocityUS =
            pCalFile.readDouble("Hardware", "Velocity (distance/uS)", 0.233);

//calculate velocity in distance per NS
hdwVs.velocityNS = hdwVs.velocityUS / 1000;

hdwVs.velocityShearUS =
  pCalFile.readDouble(
                    "Hardware", "Velocity of Shear Wave (distance/uS)", 0.133);

//calculate velocity in distance per NS
hdwVs.velocityShearNS = hdwVs.velocityShearUS / 1000;

hdwVs.numberOfMultiples =
        pCalFile.readInt("Hardware", "Number of Multiples for Wall", 1);

units = pCalFile.readInt("Hardware",
                             "English(" + INCHES + ") / Metric(" + MM + ")", 0);

unitsTimeDistance = pCalFile.readInt("Hardware",
           "Units are in Time(" + TIME + ") or Distance(" + DISTANCE + ")", 0);

markerMode = pCalFile.readInt("Hardware",
       "Marker pulses once for each threshold violation (" + PULSE +
       ") or fires continuously during the violation (" + CONTINUOUS + ")", 0);

analogDriver.loadCalFile(pCalFile);

}//end of Hardware::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

pCalFile.writeDouble("Hardware", "Nominal Wall", hdwVs.nominalWall);

pCalFile.writeInt("Hardware", "Nominal Wall Chart Position",
                                                hdwVs.nominalWallChartPosition);

pCalFile.writeDouble("Hardware", "Wall Chart Scale", hdwVs.wallChartScale);

pCalFile.writeDouble("Hardware", "Velocity (distance/uS)", hdwVs.velocityUS);

pCalFile.writeDouble("Hardware", "Velocity of Shear Wave (distance/uS)",
                                                         hdwVs.velocityShearUS);

pCalFile.writeInt("Hardware", "Number of Multiples for Wall",
                                                    hdwVs.numberOfMultiples);

pCalFile.writeInt("Hardware",
                     "English(" + INCHES + ") / Metric(" + MM + ")", units);

pCalFile.writeInt("Hardware",
    "Units are in Time(" + TIME + ") or Distance(" + DISTANCE + ")",
                                                            unitsTimeDistance);

pCalFile.writeInt("Hardware",
       "Marker pulses once for each threshold violation (" + PULSE +
       ") or fires continuously during the violation (" + CONTINUOUS + ")",
                                                                    markerMode);

analogDriver.saveCalFile(pCalFile);

}//end of Hardware::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getUSPerDataPoint
//
// Returns the current setting for uS per data point for the hardware.
//

public double getUSPerDataPoint()
{

return hdwVs.uSPerDataPoint;

}//end of Hardware::getUSPerDataPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getNumberOfChannels
//
// Returns the number of channels.
//

public int getNumberOfChannels()
{

return analogDriver.getNumberOfChannels();

}//end of Hardware::getNumberOfChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getChannels
//
// Returns a reference to the array of channels.
//

public Channel[] getChannels()
{

return analogDriver.getChannels();

}//end of Hardware::getChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getGate
//
// Returns a reference to the specified gate.
//

public Gate getGate(int pChannel, int pGate)
{

return analogDriver.getGate(pChannel, pGate);

}//end of Hardware::getGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getNumberOfGates
//
// Returns the number of gates for the specified channel.

public int getNumberOfGates(int pChannel)
{

return analogDriver.getNumberOfGates(pChannel);

}//end of Hardware::getNumberOfGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::startMonitor
//
// Commands the hardware to enter the status monitor mode.
//

public void startMonitor()
{

analogDriver.startMonitor();

}//end of Hardware::startMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::stopMonitor
//
// Commands the hardware to exit the status monitor mode.
//

public void stopMonitor()
{

analogDriver.stopMonitor();

}//end of Hardware::stopMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getMonitorPacket
//
// Retrieves a data packet containing monitor data.
//

public byte[] getMonitorPacket(boolean pRequestPacket)
{

return analogDriver.getMonitorPacket(pRequestPacket);

}//end of Hardware::getMonitorPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::zeroEncoderCounts
//
// Zeroes the encoder counts.
//

public void zeroEncoderCounts()
{

analogDriver.zeroEncoderCounts();

}//end of Hardware::zeroEncoderCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::pulseOutput1
//
// Pulses output 1 on the Control board.
//

public void pulseOutput1()
{

analogDriver.pulseOutput1();

}//end of Hardware::pulseOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::turnOnOutput1
//
// Turns on output 1 on the Control board.
//

public void turnOnOutput1()
{

analogDriver.turnOnOutput1();

output1On = true;

}//end of Hardware::turnOnOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::turnOffOutput1
//
// Turns off output 1 on the Control board.
//

public void turnOffOutput1()
{

analogDriver.turnOffOutput1();

output1On = false;

}//end of Hardware::turnOffOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::requestAScan
//
// Requests an AScan dataset for the specified channel from the appropriate
// remote device.
//

public void requestAScan(int pChannel)
{

analogDriver.requestAScan(pChannel);

}//end of Hardware::requestAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getAScan
//
// Retrieves an AScan dataset for the specified channel.
//

public AScan getAScan(int pChannel)
{

return analogDriver.getAScan(pChannel);

}//end of Hardware::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::requestPeakData
//
// Sends a request to the remote device for a peak data packet for the
// specified channel.
//

public void requestPeakData(int pChannel)
{

if (opMode == INSPECT || opMode == INSPECT_WITH_TIMER_TRACKING
                                                            || opMode == SCAN)
    analogDriver.requestPeakData(pChannel);

}//end of Hardware::requestPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::setMode
//
// Sets the mode to INSPECT, SCAN, STOPPED, etc.
//
// In scan mode, the data is transferred to the trace buffers in "free run"
// mode, regardless of the movement of the test piece.  This mode is used for
// calibration, troubleshooting, etc. and utilizes the freeRun method.
//
// Note that this not the same as the "free run" used when simulating encoders
// for systems that don't have any.  That is done in the "run" mode with
// data simulating encoder movement.  In that mode, test pieces are tracked,
// segregated, and data is saved.
//

public void setMode(int pOpMode)
{

opMode = pOpMode;

analogDriver.setMode(pOpMode);

}//end of Hardware::setMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getMode
//
// Returns the operation mode flag
//

public int getMode()
{

return(opMode);

}//end of Hardware::getMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectData
//
// Collects analog data from all channels and stores it in the appropriate trace
// buffers, collects encoder and digital I/O inputs and processes as necessary.
//
// Flow:
//
// A thread should call this function periodically to allow it to transfer data
// from the incoming buffers to the traces.  It is expected that the incoming
// buffers are large enough to hold the necessary data between calls.
//
// The buffering must be handled autonomously - by the Ethernet driver, the
// serial port driver, or a driver in the remote device.  If there is not an
// autonomous driver, a high speed timer call or thread needs to be added to
// the program to buffer the data between the calls to this function.
//
// This function will call prepareData() which can be used to transfer data
// from the input buffers to the gate variables.  If no data is ready,
// prepareData should return false and this function will exit immediately.  If
// data is ready, each channel will be called and each channel should call each
// of its gates to transfer data.  Any gate which does not have data ready
// should return false.
//
// A separate timer or thread is used to trigger the traces to display the data.
//

public void collectData()
{

//process all available packets - this is done with every call so that all
//types of packets get handled for functions that need them
//if analogDriver.prepareData() returns true, then peak data is ready to be
//processed

boolean peakDataAvailable = analogDriver.prepareAnalogData();

//collect analog data if new peak data is available
//This MUST be done every time prepareAnalogData is called as this function
//records the peaks.
if (peakDataAvailable) collectAnalogData();

boolean controlDataAvailable = analogDriver.prepareControlData();

//check if other threads are already accessing data from the remotes
if (!collectDataEnabled) return;

if (opMode == SCAN || opMode == INSPECT_WITH_TIMER_TRACKING)
    collectDataForScanOrTimerMode();
else
if (opMode == INSPECT)
    collectDataForInspectMode();

}//end of Hardware::collectData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectDataForScanOrTimerMode
//
// Collects analog data from all channels and stores it in the appropriate trace
// buffers, collects encoder and digital I/O inputs and processes as necessary.
//
// This function is specifically for SCAN and INSPECT_WITH_TIMER_TRACKING modes
// which use a timer to drive the traces rather than hardware encoder inputs.
//
// Peak data is requested periodically rather than being requested when the
// encoder position dictates such.
//

public void collectDataForScanOrTimerMode()
{

//send a request to the remote device(s) for a peak data packet
//the returned data packet will not be returned immediately, so the call to
//collectAnalogData later in this function will usually process packet(s)
//returned from the request sent on the previous pass

analogDriver.requestPeakDataForAllBoards();

//scanRateCounter is used to control the rate the scan moves across the screen

//note that the peak data packets are still being requested and stored above,
//but the trace movement will be slowed down -- some peaks in the buffers will
//be overwritten by new peaks

if (scanRateCounter-- == 0){
    scanRateCounter = 10 - globals.scanSpeed;
    }
else return;

//retrieve timer driven position updates for scanning or for systems
//which don't have hardware encoders

boolean newPositionData = collectEncoderDataTimerMode();

//call collectAnalogData again if new position data has been received -- this
//makes sure the new position in the buffer is filled with something -- the
//position will usually be overwritten by the next peak data

if (newPositionData) collectAnalogData();

}//end of Hardware::collectDataForScanOrTimerMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectDataForInspectMode
//
// Collects analog data from all channels and stores it in the appropriate trace
// buffers, collects encoder and digital I/O inputs and processes as necessary.
//
// This function is specifically for INSPECT mode which uses encoder data to
// drive the traces rather than a timer.
//
// Peak data is requested each time the encoder moves the specified tigger
// amount.
//

public void collectDataForInspectMode()
{

//process position information from whatever device is handling the encoder
//inputs

boolean newPositionData = collectEncoderDataInspectMode();

//call collectAnalogData again if new position data has been received -- this
//makes sure the new position in the buffer is filled with something -- the
//position will usually be overwritten by the next peak data

//also send a request to the remote device(s) for a peak data packet
//the returned data packet will be processed on subsequent calls to collectData

if (newPositionData){

    collectAnalogData();

    analogDriver.requestPeakDataForAllBoards();

    }

}//end of Hardware::collectDataForInspectMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectAnalogData
//
// Collects analog data from all channels and stores it in the appropriate trace
// buffers.
//
// Note:
// Originally, this function only transferred data to the trace buffers if new
// data was available from analogDriver.getNewData - this caused a problem
// because the trace pointer might be updated twice or more before new data
// became available.  If new data was not available, then the data for that
// slot would be undefined, left as the default.  This could cause a glitch in
// the trace or other problems, especially if the tracing code uses the default
// values to determine where the new trace data ends. Now, the old data is used
// if no new data has replaced it: if the trace pointer has not moved then the
// slot will be overwritten with the same value, if it has moved then the new
// slot will be written with the same data as the previous slot.
//

public void collectAnalogData()
{

numberOfChannels = analogDriver.getNumberOfChannels();

//scan through all channels and their gates, processing data from any that
//have new data available

for (int ch = 0; ch < numberOfChannels; ch++){

    //get the number of gates for the currently selected channel
    numberOfGates = analogDriver.getNumberOfGates(ch);

    for (int g = 0; g < numberOfGates; g++){

        //retrieve data for the gate
        boolean channelActive = analogDriver.getNewData(ch, g, hdwVs);

        if (hdwVs.gatePtr.tracePtr != null)
            do{
                //debug mks -- following code won't work if data starts over
                //in circular buffer -- check of filledSlot > inProcessSlot
                //will fail even though the former is ahead of the latter.
                //need to track data position with a variable which never
                //restarts even when end of buffer is reached -- buffer position
                //would then be calculated by dividing that variable by the
                //size of the buffer

                //NOTE: The check for SCAN mode is a quick fix to make this
                //work.  Since SCAN is the only mode which wraps around in the
                //buffer and also never reverses, this fix works.  However,
                //some inspection modes will use wrap around and won't work
                //because they do reverse.
                //FIX THIS!!!!


                //collect new data and move trace forward or back it up as
                //required by movement of inspection head -- use >= because
                //collectAnalogDataMinOrMax must be called even if the pointers
                //haven't moved to collect peak data which will then be written
                //over old data in the pointer positions
                if (opMode == SCAN ||
                        (hdwVs.gatePtr.tracePtr.beingFilledSlot >=
                                    hdwVs.gatePtr.tracePtr.inProcessSlot))
                    collectAnalogDataMinOrMax(hdwVs.gatePtr, channelActive);
                else
                    backUpTraces(hdwVs.gatePtr, channelActive);

            }while(hdwVs.gatePtr.tracePtr.inProcessSlot !=
                                        hdwVs.gatePtr.tracePtr.beingFilledSlot);
        }// for (int g = 0; g < numberOfGates; g++)

    }// for (int ch = 0; ch < numberOfChannels; ch++)

}//end of Hardware::collectAnalogData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectAnalogDataMinOrMax
//
// Collects analog data from channel pCh and stores it in the appropriate trace
// buffers.  This function collects data when configured for a single stream
// of min or max peaks.
//
// If chInfo[pCh].nextIndex has not been moved by collectEncoderData since the
// last data was collected, the new data will be compared with the data already
// in the current array location and the worst case data will be kept.
//
// If pChannelActive is true, the data for the channel will be stored if it is
// a new peak and the trace pointers moved.  If false, the data will be set
// such that it cannot overwrite an existing peak so it will not affect peak
// data already recorded from an active channel.  It still overrides the default
// buffer value if it hasn't been changed so that the trace will be drawn even
// if all channels are off (in which case none would overwrite the default with
// a peak).
//
// If the buffer pointer being used by this function to insert data matches
// the pointer from the position data, the function returns true.
// The pointer here only moves one position at at time.  If the position
// pointer has moved more than one, then they won't match after this function
// completes and it will turn false.  The calling function can then repeat
// the call until this function has caught up.  This should be done whenever
// the position data has moved its pointer if there is a chance it could move
// more than one position.
//

void collectAnalogDataMinOrMax(Gate gatePtr, boolean pChannelActive)
{

Trace trace = gatePtr.tracePtr;

//only move forward if the position pointer has moved
if(trace.inProcessSlot != trace.beingFilledSlot) trace.inProcessSlot++;

//the buffer is circular - start over at beginning
if (trace.inProcessSlot == trace.sizeOfDataBuffer) trace.inProcessSlot = 0;

int nextIndex = trace.inProcessSlot; //use shorter name

boolean dataStored = false;

//get the clock and data for this channel
int clockPos = gatePtr.clockPos;
int newData = gatePtr.dataPeak;

//if the channel is off or masked, set the newData value such that it will not
//override any existing data and thus will be overwritten by an active channel's
//peak except when no channel is active
//without changing the data, a trace won't move if all channels for that trace
//are turned off -- when a channel is turned back on the trace will never
//start again or takes a long time
if (!pChannelActive){
    if (gatePtr.peakDirection == 0) //0 means higher data more severe
        newData = Integer.MIN_VALUE;
    else
        newData = Integer.MAX_VALUE-1; //can't use MAX_VALUE -- that is default
    }

//if the array value is still default, replace with the new data
if (gatePtr.dBuffer1[nextIndex] == Integer.MAX_VALUE){
    gatePtr.dBuffer1[nextIndex] = newData;
    dataStored = true;
    }
else{
    //if the array value already has data, only overwrite it if the new
    //data is more severe than what is already stored there - whether
    //more severe is greater or smaller is decided by an option in the
    //configuration file and passed in via the method linkTraces

    // peakDirection == 0 means higher data is more severe
    if (gatePtr.peakDirection == 0){
        //higher values are more severe - keep highest value
        if (newData > gatePtr.dBuffer1[nextIndex]){
            gatePtr.dBuffer1[nextIndex] = newData;
            dataStored = true;
            }//if (newData >...
        }//gatePtr.peakDirection...
    else{
        //lower values are more severe - keep lowest value
        if (newData < gatePtr.dBuffer1[nextIndex]){
            gatePtr.dBuffer1[nextIndex] = newData;
            dataStored = true;
            }//if (newData <...
        }//else if (gatePtr.peakDirection...
    }//else if (gatePtr.dBuffer1[nextIndex]...


//check for threshold violations and store flags as necessary
//this must be done in this thread because the flags are used to fire
//the paint markers in real time and this thread is close to real time
//whereas the display code is not

//if the new data point was written into the array, store clock
//position and check for theshold violation
//ignore this part for off or masked channels as their data is driven high or
//low and is not valid for flagging
if (dataStored && pChannelActive){

    //store the hardware channel from which the data was obtained
    trace.peakChannel = gatePtr.channelIndex;

    //store the wall thickness for display as a number
    trace.wallThickness = gatePtr.wallThickness;

    //store the clock position for the data in bits 8-0
    gatePtr.fBuffer[nextIndex] &= 0xfffffe00; //erase old
    gatePtr.fBuffer[nextIndex] += clockPos; //store new

    //check thresholds and store flag if violation - shift threshold
    //index by 2 as 0 = no flag and 1 = user flag

    for (int j = 0; j < gatePtr.thresholds.length; j++)
        if (trace.flaggingEnabled &&
                            gatePtr.thresholds[j].checkViolation(newData)){
            //store the index of threshold violated in byte 1
            gatePtr.fBuffer[nextIndex] &= 0xffff01ff; //erase old
            gatePtr.fBuffer[nextIndex] += (j+2) << 9; //store new flag
            startMarker(gatePtr, j); //handle marking the violation
            break; //stop after first threshold violation found
            }
        else{
            endMarker(gatePtr, j);
            }//if (chInfo[pCh].thresholds[j]...

    }//if (datastored)...

//reset the data point just ahead - wrap around to beginning
if (nextIndex < gatePtr.dBuffer1.length-1){
            gatePtr.dBuffer1[nextIndex+1] = Integer.MAX_VALUE;
            gatePtr.fBuffer[nextIndex+1] = 0;
            }
        else{
            gatePtr.dBuffer1[0] = Integer.MAX_VALUE;
            gatePtr.fBuffer[0] = 0;
            }

//endPlotSlot is always one spot behind the one being filled with data
//adjusting it allows the plot functions to plot up to that point

if (trace.inProcessSlot == 0)
    trace.endPlotSlot = trace.sizeOfDataBuffer-1;
else
    trace.endPlotSlot = trace.inProcessSlot-1;

}//end of Hardware::collectAnalogDataMinOrMax
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::backUpTraces
//
// Handles case where system has been reversed to re-inspect a segment.
// Trace pointers are decremented and buffers set back to default value.
//
// Only call this function if beingFilledSlot is less than inProcessSlot.
//

void backUpTraces(Gate gatePtr, boolean pChannelActive)
{

Trace trace = gatePtr.tracePtr;

//erase data stored in the current position
gatePtr.dBuffer1[trace.inProcessSlot] = Integer.MAX_VALUE;
gatePtr.fBuffer[trace.inProcessSlot] = 0;

//move backward one slot
trace.inProcessSlot--;

//the buffer is circular - start over at beginning
if (trace.inProcessSlot == -1)
    trace.inProcessSlot = trace.sizeOfDataBuffer-1;

//endPlotSlot is always one spot behind the one being filled with data
//adjusting it allows the plot functions to plot up to that point

if (trace.inProcessSlot == 0)
    trace.endPlotSlot = trace.sizeOfDataBuffer-1;
else
    trace.endPlotSlot = trace.inProcessSlot-1;

}//end of Hardware::backUpTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectAnalogDataMinAndMax
//
// Collects analog data from channel pCh and stores it in the appropriate trace
// buffers.  This function collects data when configured for a dual stream
// of min and max peaks - often used to display a span between the two.
//
// If chInfo[pCh].nextIndex has not been moved by collectEncoderData since the
// last data was collected, the new data will be compared with the data already
// in the current array location and the worst case data will be kept.
//
// If pChannelActive is true, the data for the channel will be stored if it is
// a new peak and the trace pointers moved.  If false, the data will be set
// such that it cannot overwrite an existing peak so it will be hidden.  It
// still overrides the default value so that the trace will be drawn.
//

public void collectAnalogDataMinAndMax(Gate gatePtr, boolean pChannelActive)
{

int nextIndex = gatePtr.tracePtr.beingFilledSlot;

boolean dataStored = false;

//get the clock and data for this channel

//get the clock and data for this channel
int clockPos = gatePtr.clockPos;
int newMaxData = gatePtr.dataMaxPeak; //set by getChannelData
int newMinData = gatePtr.dataMinPeak; //set by getChannelData

//if the channel is off or masked, set the newData value such that it will not
//override any existing data and thus will be hidden except when it replaces
//the default value in order to make the trace move
if (!pChannelActive){
    newMaxData = Integer.MIN_VALUE;
    newMinData = Integer.MAX_VALUE;
    }

//newMaxData stored in dBuffer, newMinData stored in dBuffer2

//process the Max Data ----------------------------------------------------

//if the array value is still default, replace with the new data
if (gatePtr.dBuffer1[nextIndex] == Integer.MAX_VALUE){
    gatePtr.dBuffer1[nextIndex] = newMaxData;
    dataStored = true;
    }
else{
    //if the array value already has data, only overwrite it if the new
    //data is more severe than what is already stored there
    //more severe is always greater for the peakMax in span mode

    //higher values are more severe - keep highest value
    if (newMaxData > gatePtr.dBuffer1[nextIndex]){
        gatePtr.dBuffer1[nextIndex] = newMaxData;
        dataStored = true;
        }//if (newMaxData >...

    }//else if gatePtr.dBuffer1[nextIndex]

//check for threshold violations and store flags as necessary
//this must be done in this thread because the flags are used to fire
//the paint markers in real time and this thread is close to real time
//whereas the display code is not

//if the new data point was written into the array, store clock
//position and check for theshold violation
if (dataStored){
    //store the clock position for the data in byte 0
    gatePtr.fBuffer[nextIndex] &= 0xfffffe00; //erase old
    gatePtr.fBuffer[nextIndex] += clockPos; //store new

    //check thresholds and store flag if violation - shift threshold
    //index by 2 as 0 = no flag and 1 = user flag

    for (int j = 0; j < gatePtr.thresholds.length; j++)
        if (gatePtr.tracePtr.flaggingEnabled &&
                gatePtr.thresholds[j].checkViolation(newMaxData)){
            //store the index of threshold violated in byte 1
            gatePtr.fBuffer[nextIndex] &= 0xffff01ff; //erase old
            gatePtr.fBuffer[nextIndex] += (j+2) << 9; //store new flag
            //specify that max value was flagged - set bit 16 to 0
            gatePtr.fBuffer[nextIndex] &= 0xfffeffff; //erase old
            gatePtr.fBuffer[nextIndex] += 0 << 16; //store new flag
            startMarker(gatePtr, j); //handle marking the violation
            break; //stop after first threshold violation found
            }
        else{
            endMarker(gatePtr, j);
            }//if (chInfo[pCh].thresholds[j]...


    }//if (datastored)...

//process the Min Data ----------------------------------------------------

dataStored = false;

//if the array value is still default, replace with the new data
//(note that MAX_VALUE is still used as the default value for MinData buffer
if (gatePtr.dBuffer2[nextIndex] == Integer.MAX_VALUE){
    gatePtr.dBuffer2[nextIndex] = newMinData;
    dataStored = true;
    }
else{
    //if the array value already has data, only overwrite it if the new
    //data is more severe than what is already stored there
    //more severe is always less than for the peakMin in span mode

    //lower values are more severe - keep lowest value
    if (newMinData < gatePtr.dBuffer2[nextIndex]){
        gatePtr.dBuffer2[nextIndex] = newMinData;
        dataStored = true;
        }//if (newMinData <...

    }//else if (chInfo[i].dBuffer2...

//check for threshold violations and store flags as necessary
//this must be done in this thread because the flags are used to fire
//the paint markers in real time and this thread is close to real time
//whereas the display code is not

//if the new data point was written into the array, store clock
//position and check for theshold violation
if (dataStored){

    //store the hardware channel from which the data was obtained
    gatePtr.tracePtr.peakChannel = gatePtr.channelIndex;

    //store the wall thickness for display as a number
    gatePtr.tracePtr.wallThickness = gatePtr.wallThickness;

    //store the clock position for the data in byte 0
    gatePtr.fBuffer[nextIndex] &= 0xfffffe00; //erase old
    gatePtr.fBuffer[nextIndex] += clockPos; //store new

    //check thresholds and store flag if violation - shift threshold
    //index by 2 as 0 = no flag and 1 = user flag

    for (int j = 0; j < gatePtr.thresholds.length; j++)
        if (gatePtr.tracePtr.flaggingEnabled &&
                gatePtr.thresholds[j].checkViolation(newMinData)){
            //store the index of threshold violated in byte 1
            gatePtr.fBuffer[nextIndex] &= 0xffff01ff; //erase old
            gatePtr.fBuffer[nextIndex] += (j+2) << 9; //store new
            //specify that min value was flagged - set bit 16 to 1
            gatePtr.fBuffer[nextIndex] &= 0xfffeffff; //erase old
            gatePtr.fBuffer[nextIndex] += 1 << 16; //store new flag
            //stop after first threshold violation found
            break;
            }//if (chInfo[pCh].thresholds[j]...

    }//if (datastored)...


//-----------------------------------------------------------------

//reset the data point just ahead - wrap around to beginning
if (nextIndex < gatePtr.dBuffer2.length-1){
            gatePtr.dBuffer2[nextIndex+1] = Integer.MAX_VALUE;
            gatePtr.fBuffer[nextIndex+1] = 0;
            }
        else{
            gatePtr.dBuffer2[0] = Integer.MAX_VALUE;
            gatePtr.fBuffer[0] = 0;
            }

}//end of Hardware::collectAnalogDataMinAndMax
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectEncoderDataTimerMode
//
// Collects and processes encoder and digital inputs.
//
// Returns true if new data has been processed.
//
// If the encoder has reached the next array position, chInfo[?].nextIndex++
// is updated so that data will be placed in the next position.  It takes
// a set amount of encoder counts to reach the next array position as specified
// by the calibration value in the configuration file.
//
// This function is used for SCAN or INSPECT_WITH_TIMER_TRACKING mode: the
// nextIndex pointer is incremented with each call forcing the traces to
// "free run" across the screen regardless of encoder data input.
//

boolean collectEncoderDataTimerMode()
{

boolean newPositionData = false;
Trace tracePtr;

newPositionData = true; //signal that position has been changed

numberOfChannels = analogDriver.getNumberOfChannels();

//Scan through all channels and their gates, updating the buffer pointer
//for the traces connected to each.

//Since more than one gate can be attached to the same trace, a trace may
//may be encountered multiple times.  Too avoid multiple increments of
//such a trace's pointer, each trace is flagged when it is updated so it
//can be ignored the next time it is encountered.

//NOTE: you must check for NULL trace references because some channels
//are tied to flags but not traces.

//set all flags to false before starting
for (int ch = 0; ch < numberOfChannels; ch++)
    for (int g = 0; g < analogDriver.getNumberOfGates(ch); g++){
        tracePtr = analogDriver.getTrace(ch,g);
        if (tracePtr != null) tracePtr.nextIndexUpdated = false;
        }

for (int ch = 0; ch < numberOfChannels; ch++){

    //get the number of gates for the currently selected channel
    numberOfGates = analogDriver.getNumberOfGates(ch);

    for (int g = 0; g < numberOfGates; g++){

        tracePtr = analogDriver.getTrace(ch,g);

        if (tracePtr != null && tracePtr.nextIndexUpdated == false){

            //set flag so this index won't be updated again
            tracePtr.nextIndexUpdated = true;

            tracePtr.beingFilledSlot++;

            //the buffer is circular - start over at beginning
            if (tracePtr.beingFilledSlot == tracePtr.sizeOfDataBuffer)
                tracePtr.beingFilledSlot = 0;

            }
        }// for (int g = 0; g < numberOfGates; g++)
    }// for (int ch = 0; ch < numberOfChannels; ch++)

return(newPositionData);

}//end of Hardware::collectEncoderDataTimerMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectEncoderDataInspectMode
//
// Collects and processes encoder and digital inputs.
//
// Returns true if new data has been processed.
//
// If the encoder has reached the next array position, chInfo[?].nextIndex++
// is updated so that data will be placed in the next position.  It takes
// a set amount of encoder counts to reach the next array position as specified
// by the calibration value in the configuration file.
//

boolean collectEncoderDataInspectMode()
{

//do nothing until a new Inspect packet is ready
if (!analogDriver.getNewInspectPacketReady()) return false;


//debug mks -- could a new packet be received between the above line and the
//setting of the flag below -- miss a packet? -- packet processing in this
//same thread then no prob -- different thread, then problem


//ignore further calls to this function until a new packet is ready
analogDriver.setNewInspectPacketReady(false);

//retrieve all the info related to inpection control -- photo eye status,
//encoder values, etc.
analogDriver.getInspectControlVars(inspectCtrlVars);

//On entering INSPECT mode, the system will wait until signalled that the
//head is off the pipe or the pipe is out of the system, then it will wait
//until the head is on the pipe or pipe enters the system before moving the
//traces

// manual control option will override signals from the Control Board and
// begin inspection immediately after the operator presses the Inspect button
// should manual control option be removed after fixing XXtreme unit?

//if waiting for piece clear of system, do nothing until flag says true
if (hdwVs.waitForOffPipe){

    if (manualInspectControl) inspectCtrlVars.onPipeFlag = false;

    if (inspectCtrlVars.onPipeFlag) return false;
    else {
        hdwVs.waitForOffPipe = false;
        hdwVs.waitForOnPipe = true;
        //assume all heads up if off pipe and disable flagging
        hdwVs.head1Down = false; enableHeadTraceFlagging(1, false);
        hdwVs.head2Down = false; enableHeadTraceFlagging(2, false);
        }
    }

if (manualInspectControl) inspectCtrlVars.onPipeFlag = true;

//if waiting for piece to enter the head, do nothing until flag says true
if (hdwVs.waitForOnPipe){

    if (!inspectCtrlVars.onPipeFlag) return false;
    else {
        hdwVs.waitForOnPipe = false; hdwVs.watchForOffPipe = true;
        initializeTraceOffsetDelays(inspectCtrlVars.encoder2Dir);
        //the direction of the linear encoder at the start of the inspection
        //sets the forward direction (increasing or decreasing encoder count)
        inspectCtrlVars.encoder2FwdDir = inspectCtrlVars.encoder2Dir;
        //record the value of linear encoder at start of inspection
        //(this needs so be changed to store the value with each piece for
        // future units which might have multiple pieces in the system at once)
        inspectCtrlVars.encoder2Start = inspectCtrlVars.encoder2;
        prevPixPosition = 0;
        }
    }

if (manualInspectControl){
    inspectCtrlVars.head1Down = true;
    inspectCtrlVars.head2Down = true;
}

//if head 1 is up and goes down, enable flagging for all traces on head 1
if (!hdwVs.head1Down && inspectCtrlVars.head1Down){
    hdwVs.head1Down = true; enableHeadTraceFlagging(1, true);
    }

//if head 2 is up and goes down, enable flagging for all traces on head 2
if (!hdwVs.head2Down && inspectCtrlVars.head2Down){
    hdwVs.head2Down = true; enableHeadTraceFlagging(2, true);
    }

//if head 1 is down and goes up, disable flagging for all traces on head 1
if (hdwVs.head1Down && !inspectCtrlVars.head1Down){
    hdwVs.head1Down = false; enableHeadTraceFlagging(1, false);
    }

//if head 2 is down and goes up, disable flagging for all traces on head 2
if (hdwVs.head2Down && !inspectCtrlVars.head2Down){
    hdwVs.head2Down = false; enableHeadTraceFlagging(2, false);
    }

//watch for piece to exit head
if (hdwVs.watchForOffPipe){
    if (!inspectCtrlVars.onPipeFlag){

        //use tracking counter to delay after leading photo eye cleared until
        //position where modifier is to be added until the end of the piece
        hdwVs.nearEndOfPieceTracker = hdwVs.nearEndOfPiecePosition;
        //start counting down to near end of piece modifier apply start position
        hdwVs.trackToNearEndofPiece = true;

        //use tracking counter to delay after leading photo eye cleared until
        //end of piece
        hdwVs.trackToEndOfPiece = true;
        hdwVs.endOfPieceTracker = hdwVs.endOfPiecePosition;

        hdwVs.watchForOffPipe = false;
        }
    }

boolean newPositionData = true;  //signal that position has been changed

moveEncoders();

return(newPositionData);

}//end of Hardware::collectEncoderDataInspectMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveEncoders
//

void moveEncoders()
{

//The control board sends new encoder data after a set number of encoder counts,
//this set number approximates the distance for one pixel.  It is not exact
//because there is a round off error which accumulates with each packet sent.
//To counterract this, the actual encoder count used to compute if a new pixel
//has been reached.  Sometimes a packet may be sent for which the encoder count
//does not calculate to the next pixel, so the buffer pointer is not moved and
//incoming data is still stored in the previous pixel.  Sometimes, a packet
//will arrive which skips a pixel.  In that case, the skipped pixels are filled
//with data from the previous pixel.

//calculate the position in inches
double position = encoder2InchesPerCount *
        (inspectCtrlVars.encoder2 - inspectCtrlVars.encoder2Start);

//take absolute value so head moving in reverse works the same as forward
position = Math.abs(position);

//calculate the number of pixels moved since the last check
int pixPosition = (int)(position * pixelsPerInch);

//debug mks -- check here for passing zero point -- means pipe has backed out of
//the system so remove segment

//calculate the number of pixels moved since the last update
int pixelsMoved = pixPosition - prevPixPosition;

//do nothing if encoders haven't moved enough to reach the next pixel
if (pixelsMoved == 0) return;

prevPixPosition = pixPosition;

Trace tracePtr;

numberOfChannels = analogDriver.getNumberOfChannels();

//Scan through all channels and their gates, updating the buffer pointer
//for the traces connected to each.

//Since more than one gate can be attached to the same trace, a trace may
//may be encountered multiple times.  Too avoid multiple increments of
//such a trace's pointer, each trace is flagged when it is updated so it
//can be ignored the next time it is encountered.

//NOTE: you must check for NULL trace references because some channels
//are tied to flags but not traces.

//set all flags to false before starting
for (int ch = 0; ch < numberOfChannels; ch++)
    for (int g = 0; g < analogDriver.getNumberOfGates(ch); g++){
        tracePtr = analogDriver.getTrace(ch,g);
        if (tracePtr != null) tracePtr.nextIndexUpdated = false;
        }

for (int ch = 0; ch < numberOfChannels; ch++){

    //get the number of gates for the currently selected channel
    numberOfGates = analogDriver.getNumberOfGates(ch);

    for (int g = 0; g < numberOfGates; g++){

        tracePtr = analogDriver.getTrace(ch,g);

        if (tracePtr != null && tracePtr.nextIndexUpdated == false){

            if (pixelsMoved > 0)
                moveTracesForward(tracePtr, pixelsMoved, position);
            else
                moveTracesBackward(tracePtr, Math.abs(pixelsMoved), position);

            }//if (tracePtr != null...
        }// for (int g = 0; g < numberOfGates; g++)
    }// for (int ch = 0; ch < numberOfChannels; ch++)

}//end of Hardware::moveEncoders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveTracesForward
//
// Moves the trace pointers forward to respond to forward inspection direction.
//
// Parameter pTrace is the trace to be updated.
//
// Parameter pPixelsMoved is the number of pixels the trace is to be moved.
//
// Parameter pPosition is the position of the head or inspection piece as
// measured from the point where the photo eye was blocked.
//

void moveTracesForward(Trace pTrace, int pPixelsMoved, double pPosition)
{

//set flag so this trace's index won't be updated again by
//another gate tied to this same trace
pTrace.nextIndexUpdated = true;

//the trace does not start until its associated sensor(s) have
//reached the pipe after the photo eye has detected it
if (pTrace.delayDistance > pPosition )return;

for (int x = 0; x < pPixelsMoved; x++){

    pTrace.beingFilledSlot++;

    //the buffer is circular - start over at beginning
    if (pTrace.beingFilledSlot == pTrace.sizeOfDataBuffer)
        pTrace.beingFilledSlot = 0;

    //debug mks
    //the end of piece, near start of piece, and near endof piece
    //tracking needs to be done separately for each trace, trigger
    //distances need to be loaded from config, track counts (which
    //is in pixels) needs to be converted from the inch distances
    //for the desired effect
    //see HardwareVars notes for more details

    //track position to find end of section at start of pipe where
    //modifier is to be applied
    if (hdwVs.nearStartOfPieceTracker != 0){
        hdwVs.nearStartOfPieceTracker--;
        }
    else{
        hdwVs.nearStartOfPiece = false;
        }

    if (hdwVs.trackToNearEndofPiece){
        if (hdwVs.nearEndOfPieceTracker != 0){
            hdwVs.nearEndOfPieceTracker--;
            }
        else{
            hdwVs.nearEndOfPiece = true;
            }
        }

    if (hdwVs.trackToEndOfPiece){
        if (hdwVs.endOfPieceTracker != 0){
            hdwVs.endOfPieceTracker--;
            }
        else{
        //set flag to force preparation for a new piece
        globals.prepareForNewPiece = true;
            }
        }

    }//for (int x = 0; x < pixelsMoved; x++){

}//end of Hardware::moveTracesForward
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveTracesBackward
//
// Moves the trace pointers backward to respond to system being reversed.
// This is not to be confused with inspecting in the reverse direction.
// Inspection can occur in either directon.  This code handles cases where the
// head or piece is backed up for the current inspection forward direction to
// re-inspect a segment.
//
// Parameter pTrace is the trace to be updated.
//
// Parameter pPixelsMoved is the number of pixels the trace is to be moved and
// should always be positive.
//
// Parameter pPosition is the position of the head or inspection piece as
// measured from the point where the photo eye was blocked.
//

void moveTracesBackward(Trace pTrace, int pPixelsMoved, double pPosition)
{

//set flag so this trace's index won't be updated again by
//another gate tied to this same trace
pTrace.nextIndexUpdated = true;

//the trace does not start until its associated sensor(s) have
//reached the pipe after the photo eye has detected it
if (pTrace.delayDistance > pPosition )return;

for (int x = 0; x < pPixelsMoved; x++){

    pTrace.beingFilledSlot--;

    //the buffer is circular - start over at end
    if (pTrace.beingFilledSlot == -1)
        pTrace.beingFilledSlot = pTrace.sizeOfDataBuffer - 1;

    //currently, the nearStartOfPiece and nearEndOfPiece conditions are not
    //tracked in reverse -- should probably be fixed just in case reversing
    //occurs in these areas

    //if tracking to the end of the piece after end of piece photo eye signal,
    //reverse this process

    if (hdwVs.trackToEndOfPiece){
        if (hdwVs.endOfPieceTracker != hdwVs.endOfPiecePosition){
            hdwVs.endOfPieceTracker++;
            }
        else{
            //original trigger point passed, so no longer near end
            hdwVs.trackToEndOfPiece = false;
            }
        }

    }//for (int x = 0; x < pixelsMoved; x++){

}//end of Hardware::moveTracesBackward
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::enableHeadTraceFlagging
//
// Enables or disables all traces for the specified head.
//

void enableHeadTraceFlagging(int pHead, boolean pEnable)
{

Trace tracePtr;

for (int cg = 0; cg < chartGroups.length; cg++){

    int nSC = chartGroups[cg].getNumberOfStripCharts();

    for (int sc = 0; sc < nSC; sc++){

        int nTr = chartGroups[cg].getStripChart(sc).getNumberOfTraces();

        for (int tr = 0; tr < nTr; tr++){

            tracePtr = chartGroups[cg].getStripChart(sc).getTrace(tr);

            if ((tracePtr != null) && (tracePtr.head == pHead))

                tracePtr.flaggingEnabled = pEnable;

            }//for (int tr = 0; tr < nTr; tr++)
        }//for (int sc = 0; sc < nSC; sc++)
    }//for (int cg = 0; cg < chartGroups.length; cg++)

}//end of Hardware::enableHeadTraceFlagging
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::initializeTraceOffsetDelays
//
// Sets the trace start delays so the traces don't start until their associated
// sensors reach the pipe.
//
// The distance is set depending on the direction of inspection.  Some systems
// have different photo eye to sensor distances depending on the direction
// of travel.
//
// The delay is necessary because each sensor may be a different distance from
// the photo-eye which detects the start of the pipe.
//
// Two sets of values are stored:
//
// The distance of each sensor from the front edge of its head.
// The front edge of the head is the edge which reaches the inspection piece
// first when the carriage is moving away from the operator's station
// (the "forward" direction).
//
// The distances of Photo Eye 1 and Photo Eye 2 to the front edge of each
// head.
//
// Photo Eye 1 is the photo eye which reaches the inspection piece first when
// the carriage is moving away from the operator's station (the "forward"
// direction).
//

public void initializeTraceOffsetDelays(int pDirection)
{

Trace tracePtr;

double leadingTraceCatch, trailingTraceCatch;
int lead = 0, trail = 0;

for (int cg = 0; cg < chartGroups.length; cg++){

    int nSC = chartGroups[cg].getNumberOfStripCharts();

    for (int sc = 0; sc < nSC; sc++){

        int nTr = chartGroups[cg].getStripChart(sc).getNumberOfTraces();

        //these used to find the leading trace (smallest offset) and the
        //trailing trace (greatest offset) for each chart
        leadingTraceCatch = Double.MAX_VALUE;
        trailingTraceCatch = Double.MIN_VALUE;

        for (int tr = 0; tr < nTr; tr++){

            tracePtr = chartGroups[cg].getStripChart(sc).getTrace(tr);

            //if the current direction is the "Away" direction, then set the
            //offsets properly for the carriage moving away from the operator
            //otherwise set them for the carriage moving towards the operator
            //see more notes in this method's header

            if (tracePtr != null){

                //start with all false, one will be set true
                tracePtr.leadTrace = false;

                if (pDirection == AwayDirection)
                    tracePtr.delayDistance = tracePtr.startFwdDelayDistance;
                else
                    tracePtr.delayDistance = tracePtr.startRevDelayDistance;

                //find the leading and trailing traces
                if (tracePtr.delayDistance < leadingTraceCatch)
                    {lead = tr; leadingTraceCatch = tracePtr.delayDistance;}
                if (tracePtr.delayDistance > trailingTraceCatch)
                    {trail = tr; trailingTraceCatch = tracePtr.delayDistance;}

                }//if (tracePtr != null)

            }//for (int tr = 0; tr < nTr; tr++)

            chartGroups[cg].getStripChart(sc).getTrace(lead).leadTrace = true;
            chartGroups[cg].getStripChart(sc).getTrace(trail).trailTrace = true;
            chartGroups[cg].getStripChart(sc).setLeadTrailTraces(lead, trail);

        }//for (int sc = 0; sc < nSC; sc++)
    }//for (int cg = 0; cg < chartGroups.length; cg++)

}//end of Hardware::initializeTraceOffsetDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::startMarker
//
// If the marker mode is pulsed, then a pulse will be fired only at the
// first threshold violation and not fired again until the violation is cleared
// and then occurs again.
//
// If the marker mode is continuous, a pulse will be fired for every violation
// which will effectively turn the marker on continuously if the violations
// are closely spaced.
//

public void startMarker(Gate pGatePtr, int pWhichThreshold)
{

if (pGatePtr.thresholds[pWhichThreshold].okToMark || markerMode == CONTINUOUS){
    pulseOutput1(); //fire the marker
    }

pGatePtr.thresholds[pWhichThreshold].okToMark = false;

}//end of Hardware::startMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::endMarker
//
// For pulse mode, the flag which allows another marker pulse is enabled.
// For continuous mode, the marker is turned off if it was on.
//

public void endMarker(Gate pGatePtr, int pWhichThreshold)
{

pGatePtr.thresholds[pWhichThreshold].okToMark = true;

}//end of Hardware::endMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::displayMessages
//
// Displays any messages received from the remote.
//
// NOTE: If a message needs to be displayed by a thread other than the main
// Java thread, use threadSafeLog instead.
//

public void displayMessages()
{

analogDriver.displayMessages();

}//end of  Hardware::displayMessages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::updateRabbitCode
//
// Installs new firmware on the Rabbit micro-controllers.
//

public void updateRabbitCode(int pWhichRabbits)
{

    analogDriver.updateRabbitCode(pWhichRabbits);

}//end of Hardware::updateRabbitCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::readRAM
//
// Fills array with the contents of the RAM on the specified chassis, slot,
// DSP chip, DSP core, shared or local memory, page, and starting address.
//
// pCount bytes are returned.
//

public void readRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pCount, byte[] dataBlock)
{

analogDriver.readRAM(pChassis, pSlot, pDSPChip, pDSPCore, pRAMType,
                                          pPage, pAddress, pCount, dataBlock);

}//end of Hardware::readRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::writeRAM
//
// Writes pValue to the RAM at pAddress on the specified chassis, slot,
// DSP chip, DSP core, shared or local memory, and page.
//

public void writeRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pValue)
{

analogDriver.writeRAM(pChassis, pSlot, pDSPChip, pDSPCore,
                                            pRAMType, pPage, pAddress, pValue);

}//end of Hardware::writeRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::fillRAM
//
// Fills a block of memory with the specified address, size, and value.
//

public void fillRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pBlockSize, int pValue)
{

analogDriver.fillRAM(pChassis, pSlot, pDSPChip, pDSPCore,
                                pRAMType, pPage, pAddress, pBlockSize, pValue);

}//end of Hardware::fillRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getState
//
// Returns the state of various flags or values as selected by pWhich.
// If a flag is being requested, returns 0 for false and not 0 for true.
// If a value is being requested, returns the value.
//
//

public int getState(int pChassis, int pSlot, int pWhich)
{

return analogDriver.getState(pChassis, pSlot, pWhich);

}//end of Hardware::getState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::setState
//
// Sets the state of various flags or values as selected by pWhich.
// If a flag is being specified, pValue 0 for false and not 0 for true.
// If a value is being specified, it will be set to pValue.
//

public void setState(int pChassis, int pSlot, int pWhich, int pValue)
{

analogDriver.setState(pChassis, pSlot, pWhich, pValue);

}//end of Hardware::setState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware:sendDataChangesToRemotes
//
// If any data has been changed, sends the changes to the remotes.
//

public void sendDataChangesToRemotes()
{

analogDriver.sendDataChangesToRemotes();

}//end of Hardware::sendDataChangesToRemotes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware:verifyAllDSPCode2
//
// Verifies that the code in each DSP matches the file.  Used to check for
// transmission or corruption errors.
//
// This function checks byte by byte and is VERY slow.
//

public void verifyAllDSPCode2()
{

//disable the data collection thread so it doesn't collide with access by
//the verifyAllDSPCode function

collectDataEnabled = false;

analogDriver.verifyAllDSPCode2();

collectDataEnabled = true;

}//end of Hardware::verifyAllDSPCode2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::doTasks
//
// Should be called by a timer so that various tasks can be performed as
// necessary.  Since Java doesn't update the screen during calls to the user
// software, it is necessary to execute tasks in a segmented fashion if it
// is necessary to display status messages along the way.
//

public void doTasks()
{

analogDriver.doTasks();

}//end of Hardware::doTasks
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::driveSimulation
//
// Drive any simulation functions if they are active.
//

public void driveSimulation()
{

analogDriver.driveSimulation();

}//end of Hardware::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::logStatus
//
// Writes various status messages to the log window.
//

public void logStatus(JTextArea pTextArea)
{

analogDriver.logStatus(pTextArea);

}//end of Hardware::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

public void shutDown()
{

analogDriver.shutDown();

}//end of Hardware::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::linkTraces
//
// This function is called by traces to link their buffers to specific hardware
// channels/gates and give a link back to variables in the Trace object.
//

public void linkTraces(int pChartGroup, int pChart, int pTrace, int[] pDBuffer,
  int[] pDBuffer2, int[] pFBuffer, Threshold[] pThresholds, int pPlotStyle,
                                                            Trace pTracePtr)
{

analogDriver.linkTraces(pChartGroup, pChart, pTrace, pDBuffer, pDBuffer2,
                         pFBuffer, pThresholds, pPlotStyle, pTracePtr);

}//end of Hardware::linkTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::run
//

@Override
public void run()
{

try{
    while (true){

        //drive any simulation functions if they are active
        driveSimulation();

        Thread.sleep(10);

        }//while

    }//try

catch (InterruptedException e) {
    }

}//end of Hardware::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::calculateComputedValue1
//
// For this version of Hardware.java, calculates the wall thickness based upon
// the cursor Y position.
//
// This function is duplicated in multiple objects.  Should make a separate
// class which each of those objects creates to avoid duplication?
//

@Override
public double calculateComputedValue1(int pCursorY)
{

double offset = (hdwVs.nominalWallChartPosition - pCursorY)
                                                        * hdwVs.wallChartScale;

//calculate wall at cursor y position relative to nominal wall value
return (hdwVs.nominalWall + offset);

}//end of Hardware::calculateComputedValue1
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// Hardware::xmtMessage
//
// This method allows an outside class to send a message and a value to this
// class and receive a status value back.
//
// In this class, this is mainly used to pass messages on to the simulator
// object(s) so that they can be controlled via messages.
//

@Override
public int xmtMessage(int pMessage, int pValue)
{

//pass the message on to the mechanical simulation object
return analogDriver.xmtMessage(pMessage, pValue);

}//end of Hardware::xmtMessage
//----------------------------------------------------------------------------

}//end of class Hardware
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
