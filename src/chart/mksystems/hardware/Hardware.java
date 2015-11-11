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

import chart.Log;
import chart.MessageLink;
import chart.ThreadSafeLogger;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Plotter;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import java.io.BufferedWriter;
import java.io.IOException;
import javax.swing.*;
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

    public boolean startUTRabbitUpdater, startControlRabbitUpdater;

    //debug mks -- needs to be loaded from config file -- specifies if carriage
    //moving away is increasing or decreasing encoder counts
    int awayDirection = 0;

    int prevPixPosition;

    boolean output1On = false;

    public boolean prepareForNewPiece;

    Settings settings;
    ChartGroup chartGroups[];

    public HardwareVars hdwVs;
    IniFile configFile;
    HardwareLink analogDriver;
    HardwareLink digitalDriver;
    int numberOfAnalogChannels;
    JTextArea log;
    int scanRateCounter;

    int flaggingEnableDelay1, flaggingEnableDelay2 = 0;

    InspectControlVars inspectCtrlVars;

    public static int NO_HEAD = 0;
    public static int HEAD_1 = 1;
    public static int HEAD_2 = 2;

    public static int ALL_RABBITS = 0;
    public static int UT_RABBITS = 1;
    public static int CONTROL_RABBITS = 2;

    public boolean connected = false;
    public boolean active = false;
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

    public boolean distanceAdjustedForReturnTrip = false;

    public static int PULSE = 0, CONTINUOUS = 1;
    public int markerMode = PULSE;

//-----------------------------------------------------------------------------
// Hardware::Hardware (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Hardware(IniFile pConfigFile, Settings pSettings, JTextArea pLog)
{

    hdwVs = new HardwareVars(); configFile = pConfigFile; log = pLog;
    settings = pSettings;

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

    analogDriverName = pConfigFile.readString(
                    "Hardware", "Analog Input Driver Name", "PCI-DAS6023");

    digitalDriverName = pConfigFile.readString(
                    "Hardware", "Digital Input Driver Name", "PCI-QUAD04");

    numberOfAnalogChannels = pConfigFile.readInt(
                                "Hardware", "Number of Analog Channels", 50);

    //load the nS per data point value and compute the uS per data point as well
    hdwVs.nSPerDataPoint =
                pConfigFile.readDouble("Hardware", "nS per Data Point", 15.0);
    hdwVs.uSPerDataPoint = hdwVs.nSPerDataPoint / 1000;

    hdwVs.photoEye1DistanceFrontOfHead1 = pConfigFile.readDouble("Hardware",
                          "Photo Eye 1 Distance to Front Edge of Head 1", 8.0);

    hdwVs.photoEye1DistanceFrontOfHead2 = pConfigFile.readDouble("Hardware",
                         "Photo Eye 1 Distance to Front Edge of Head 2", 32.0);

    hdwVs.photoEye2DistanceFrontOfHead1 = pConfigFile.readDouble("Hardware",
                         "Photo Eye 2 Distance to Front Edge of Head 1", 46.0);

    hdwVs.photoEye2DistanceFrontOfHead2 = pConfigFile.readDouble("Hardware",
                         "Photo Eye 2 Distance to Front Edge of Head 2", 22.0);

    hdwVs.photoEyeToPhotoEyeDistance = pConfigFile.readDouble("Hardware",
                         "Distance Between Perpendicular Photo Eyes", 53.4375);

    settings.awayFromHome =
        pConfigFile.readString(
            "Hardware",
            "Description for inspecting in the direction leading away from the"
            + " operator's compartment", "Away From Home");

    settings.towardsHome =
        pConfigFile.readString(
            "Hardware",
            "Description for inspecting in the direction leading toward the"
            + " operator's compartment", "Toward Home");

    //the control board sends packets every so many counts and is susceptible to
    //cumulative round off error, but the values below can be tweaked to give
    //accurate results over the length of the piece -- the packet send trigger
    //counts are often the same as the values below

    hdwVs.encoder1InchesPerCount =
        pConfigFile.readDouble("Hardware", "Encoder 1 Inches Per Count", 0.003);

    hdwVs.encoder2InchesPerCount =
        pConfigFile.readDouble("Hardware", "Encoder 2 Inches Per Count", 0.003);

    hdwVs.pixelsPerInch =
                    pConfigFile.readDouble("Hardware", "Pixels per Inch", 1.0);

    hdwVs.decimalFeetPerPixel = 1/(hdwVs.pixelsPerInch * 12);

    manualInspectControl = pConfigFile.readBoolean(
                    "Hardware", "Manual Inspection Start/Stop Control", false);

    if (numberOfAnalogChannels > 100) {numberOfAnalogChannels = 100;}

    createAnalogDriver(analogDriverName);

}//end of Hardware::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::setChartGroups
//
// Sets the chartGroups variable.
//

public void setChartGroups(ChartGroup pChartGroups [])
{

    chartGroups = pChartGroups;

    analogDriver.setChartGroups(pChartGroups);

}//end of Hardware::setChartGroups
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

    if (pDriverName.equalsIgnoreCase("PCI-DAS6023")) {
        analogDriver =
        new AnalogPCIDAS6023(configFile, true, numberOfAnalogChannels, hdwVs);
        analogDriver.init();
    }

    if (pDriverName.equalsIgnoreCase("Capulin 1")) {
        analogDriver =
        new Capulin1(configFile, settings, true, numberOfAnalogChannels, hdwVs,
                          log, settings.jobFileFormat, Settings.mainFileFormat);
        analogDriver.init();
    }

}//end of Hardware::createAnalogDriver
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::connect
//
// Establishes connections with remote devices.
//

public void connect() throws InterruptedException
{

    logger.section();
    logger.logMessage("Connecting With Chassis and Configuring\n\n");

    connected = true;

    //before attempting to connect with the boards, start a thread to run the
    //simulation functions so the simulated boards can respond

    if (analogDriver.getSimulate()){
        Thread thread = new Thread(this, "Simulator"); thread.start();
    }

    analogDriver.connect();

    //calculate the distance offsets from the point where the photo eye detects
    //the pipe so each plotter can be delayed until its sensors reach the
    //inspection piece
    calculatePlotterOffsetDelays();

    active = true; //allow hardware to be accessed

    logger.logMessage("\nChassis configuration complete.\n");

    logger.saveToFile("Chassis Configuration Log");

}//end of Hardware::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::calculatePlotterOffsetDelays
//
// Calculates appropriate distance offsets for each plotter to align it with
// the leading edge of the test piece.
//

void calculatePlotterOffsetDelays()
{

    calculateTraceOffsetDelays();

    //calculate offset delays for maps not associated with a channel
    analogDriver.calculateMapOffsetDelays();

}//end of Hardware::calculatePlotterOffsetDelays
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

    Plotter plotterPtr;
        for (ChartGroup chartGroup : chartGroups) {
            int nSC = chartGroup.getNumberOfStripCharts();
            for (int sc = 0; sc < nSC; sc++) {
                int nTr = chartGroup.getStripChart(sc).getNumberOfPlotters();
                for (int tr = 0; tr < nTr; tr++) {
                    plotterPtr = chartGroup.getStripChart(sc).getPlotter(tr);
                    if ((plotterPtr != null) && (plotterPtr.head == 1)){
                        plotterPtr.startFwdDelayDistance =
                                hdwVs.photoEye1DistanceFrontOfHead1
                                + plotterPtr.distanceSensorToFrontEdgeOfHead;
                        
                        plotterPtr.startRevDelayDistance =
                                hdwVs.photoEye2DistanceFrontOfHead1 -
                                plotterPtr.distanceSensorToFrontEdgeOfHead;
                        
                    }//if ((tracePtr != null) && (tracePtr.head == 1))
                    if ((plotterPtr != null) && (plotterPtr.head == 2)){
                        plotterPtr.startFwdDelayDistance =
                                hdwVs.photoEye1DistanceFrontOfHead2
                                + plotterPtr.distanceSensorToFrontEdgeOfHead;
                        
                        plotterPtr.startRevDelayDistance =
                                hdwVs.photoEye2DistanceFrontOfHead2 -
                                plotterPtr.distanceSensorToFrontEdgeOfHead;
                        
                    }//if ((tracePtr != null) && (tracePtr.head == 2))
                } //for (int tr = 0; tr < nTr; tr++)
            } //for (int sc = 0; sc < nSC; sc++)
        } //for (int cg = 0; cg < chartGroups.length; cg++)

}//end of Hardware::calculateTraceOffsetDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::saveAllMapDataSetsToFile
//
// Stores the map data stored in Boards set up for mapping to file(s).
// Each board will save its own file.
//
// Any boards without mapping will ignore the request.
//

public void saveAllMapDataSetsToTextFile(
        String pFilename, String pJobFileFormat,
        String pInspectionDirectionDescription)
{

    analogDriver.saveAllMapDataSetsToFile(
                   pFilename, pJobFileFormat, pInspectionDirectionDescription);

}//end of Hardware::saveAllMapDataSetsToFile
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

    distanceAdjustedForReturnTrip = pCalFile.readBoolean("Hardware",
                      "Distance displays adjusted for round trip time", false);

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

    pCalFile.writeDouble(
                        "Hardware", "Velocity (distance/uS)", hdwVs.velocityUS);

    pCalFile.writeDouble("Hardware", "Velocity of Shear Wave (distance/uS)",
                                                         hdwVs.velocityShearUS);

    pCalFile.writeInt("Hardware", "Number of Multiples for Wall",
                                                    hdwVs.numberOfMultiples);

    pCalFile.writeInt("Hardware",
                     "English(" + INCHES + ") / Metric(" + MM + ")", units);

    pCalFile.writeInt("Hardware",
    "Units are in Time(" + TIME + ") or Distance(" + DISTANCE + ")",
                                                            unitsTimeDistance);
    pCalFile.writeBoolean("Hardware",
             "Distance displays adjusted for round trip time",
             distanceAdjustedForReturnTrip);

    pCalFile.writeInt("Hardware",
       "Marker pulses once for each threshold violation (" + PULSE +
       ") or fires continuously during the violation (" + CONTINUOUS + ")",
                                                                   markerMode);

    analogDriver.saveCalFile(pCalFile);

}//end of Hardware::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::saveCalFileHumanReadable
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

    analogDriver.saveCalFileHumanReadable(pOut);

}//end of Hardware::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::getRepRateInHertz
//
// Returns the rep rate in Hertz used for all boards/channels.
//

public int getRepRateInHertz()
{

    return(analogDriver.getRepRateInHertz());

}//end of Hardware::getRepRateInHertz
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

public UTGate getGate(int pChannel, int pGate)
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
// Hardware::pulseAlarmMarker
//
// Pulses the alarm/marker output specified by pChannel.
//

public void pulseAlarmMarker(int pChannel)
{

    analogDriver.pulseAlarmMarker(pChannel);

}//end of Hardware::pulseAlarmMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::flipAnalogOutput
//
// Flips the analog output specified by pChannel from min to max and vice
// versa.
//
// Valid values for pWhichOutput are 0-3.
//

public void flipAnalogOutput(int pChannel)
{

    analogDriver.flipAnalogOutput(pChannel);

}//end of Hardware::flipAnalogOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::turnOnAudibleAlarm
//
// Turns on the audible alarm.
//

public void turnOnAudibleAlarm()
{

    analogDriver.turnOnAudibleAlarm();

    output1On = true;

}//end of Hardware::turnOnAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::turnOffAudibleAlarm
//
// Turns off the audible alarm.
//

public void turnOffAudibleAlarm()
{

    analogDriver.turnOffAudibleAlarm();

    output1On = false;

}//end of Hardware::turnOffAudibleAlarm
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
                                                            || opMode == SCAN) {
        analogDriver.requestPeakData(pChannel);
    }

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

    //handle STOP mode
    if (opMode == Hardware.STOP){
        
        //if the measuredLength is 0 when put in Stop mode, set to the length of
        //tube which as been scanned (if any) so that the tube will have a valid
        //number for length -- necessary for units without photoeyes for which
        //the end of the tube is denoted by going to Stop mode

        //the distance between the vertical eyes is not accounted for
        //when calculating the distance travelled when the STOP mode is
        //entered -- the trailing photo-eye may not even have reached the
        //end of the test piece -- the calculation in this case is merely
        //a rough estimate
        
        if (hdwVs.measuredLength == 0){

            //calculate number counts recorded between start/stop eye triggers
            int pieceLengthEncoderCounts =
             Math.abs(inspectCtrlVars.encoder2 - inspectCtrlVars.encoder2Start);
            
            //convert to inches
            hdwVs.measuredLength =
                 hdwVs.convertEncoder2CountsToFeet(pieceLengthEncoderCounts);
                        
        }
    }//end of if (opMode == Hardware.STOP)

    //handle INSPECT_WITH_TIMER_TRACKING mode
    if (opMode == Hardware.INSPECT_WITH_TIMER_TRACKING){
        //enable flagging always for timer driven mode
        hdwVs.head1Down = true; enableHeadTraceFlagging(1, true);
        hdwVs.head2Down = true; enableHeadTraceFlagging(2, true);
    }
        
    //handle SCAN mode
    if (opMode == Hardware.SCAN){
        //enable flagging always for scan mode
        hdwVs.head1Down = true; enableHeadTraceFlagging(1, true);
        hdwVs.head2Down = true; enableHeadTraceFlagging(2, true);
    }

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
    if (peakDataAvailable) {collectAnalogData();}

    boolean controlDataAvailable = analogDriver.prepareControlData();

    //check if other threads are already accessing data from the remotes
    if (!collectDataEnabled) {return;}

    if (opMode == SCAN || opMode == INSPECT_WITH_TIMER_TRACKING) {
        collectDataForScanOrTimerMode();
    }
    else if (opMode == INSPECT) {
        collectDataForInspectMode();
    }

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

    //scanRateCounter is used to control rate the scan moves across the screen

    //note that the peak data packets are still being requested and stored
    //above, but the trace movement will be slowed down -- some peaks in the
    //buffers will be overwritten by new peaks

    if (scanRateCounter-- == 0){
        scanRateCounter = 10 - settings.scanSpeed;
    }
    else {
        return;
    }

    //retrieve timer driven position updates for scanning or for systems
    //which don't have hardware encoders

    boolean newPositionData = collectEncoderDataTimerMode();

    //call collectAnalogData again if new position data has been received --
    //this makes sure the new position in the buffer is filled with something --
    //the position will usually be overwritten by the next peak data

    if (newPositionData) {collectAnalogData();}

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

    //call collectAnalogData again if new position data has been received --
    //this makes sure the new position in the buffer is filled with something --
    //the position will usually be overwritten by the next peak data
    //wip mks -- not required to call collectAnalogData any more since
    // the new TraceData class fills empty buffer spaces with previous data?

    //also send a request to the remote device(s) for a peak data packet
    //the returned data packet will be processed on subsequent calls to
    //collectData

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
// wipmks --
// Originally, this function only transferred data to the trace buffers if new
// data was available from analogDriver.getNewData - return to this?
//

public void collectAnalogData()
{

    numberOfChannels = analogDriver.getNumberOfChannels();

    //scan through all channels and their gates, processing data from any that
    //have new data available

    for (int ch = 0; ch < numberOfChannels; ch++){

        if(!analogDriver.getChannels()[ch].isEnabled()) {continue;}

        //get the number of gates for the currently selected channel
        numberOfGates = analogDriver.getNumberOfGates(ch);

        for (int g = 0; g < numberOfGates; g++){

            //retrieve data for the gate
            boolean channelActive = analogDriver.getNewData(ch, g, hdwVs);

            if (hdwVs.gatePtr.tracePtr != null) {

                collectAnalogDataMinOrMax(hdwVs.gatePtr, channelActive);

            }// if (hdwVs.gatePtr.tracePtr != null)
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

void collectAnalogDataMinOrMax(UTGate gatePtr, boolean pChannelActive)
{

    Trace trace = gatePtr.tracePtr;
    TraceData traceData = trace.traceData;

    //get the clock and data for this channel
    int clockPos = gatePtr.clockPos;
    int newData = gatePtr.dataPeak;

    boolean dataStored = traceData.storeDataAtInsertionPoint(newData);

    //check for threshold violations and store flags as necessary
    //this must be done in this thread because the flags are used to fire
    //the paint markers in real time and this thread is close to real time
    //whereas the display code is not

    //if the new data point was written into the array, store clock
    //position and check for theshold violation
    //ignore this part for off or masked channels as their data is driven high
    //or low and is not valid for flagging
    if (dataStored && pChannelActive){

        //store the hardware channel from which the data was obtained
        trace.peakChannel = gatePtr.channelIndex;

        //store the wall thickness for display as a number
        trace.wallThickness = gatePtr.wallThickness;

        traceData.storeClockAtInsertionPoint(clockPos);

        //check thresholds and store flag if violation - shift threshold
        //index by 2 as 0 = no flag and 1 = user flag

        for (int j = 0; j < gatePtr.thresholds.length; j++) {
            if (trace.flaggingEnabled &&
                                gatePtr.thresholds[j].checkViolation(newData)){

                traceData.storeThresholdAtInsertionPoint(j);

                //store this channel as the most recent flagged for the trace
                trace.setLastFlagged(gatePtr.channelIndex, clockPos);
                startMarker(gatePtr, j); //handle marking the violation
                break; //stop after first threshold violation found
            }
            else{
                //no flagging, so reset marking system so it can mark again
                endMarker(gatePtr, j);
            }
        }//for (int j = 0; j < gatePtr.thresholds.length; j++)

    }//if (datastored)...

}//end of Hardware::collectAnalogDataMinOrMax
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectEncoderDataTimerMode
//
// Collects and processes encoder and digital inputs.
//
// Returns true if new data has been processed.
//
// This function is used for SCAN or INSPECT_WITH_TIMER_TRACKING mode: the
// data buffer pointer is advanced with each call forcing the traces to
// "free run" across the screen regardless of encoder data input.
//

boolean collectEncoderDataTimerMode()
{

    boolean newPositionData;
    Plotter plotterPtr;

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
    for (int ch = 0; ch < numberOfChannels; ch++) {
        for (int g = 0; g < analogDriver.getNumberOfGates(ch); g++){
            plotterPtr = analogDriver.getTrace(ch,g);
            if (plotterPtr != null) {plotterPtr.positionAdvanced = false;}
        }
    }

    for (int ch = 0; ch < numberOfChannels; ch++){

        //get the number of gates for the currently selected channel
        numberOfGates = analogDriver.getNumberOfGates(ch);

        for (int g = 0; g < numberOfGates; g++){

            plotterPtr = analogDriver.getTrace(ch,g);

            if (plotterPtr != null && plotterPtr.positionAdvanced == false){

                //set flag so this index won't be updated again
                plotterPtr.positionAdvanced = true;

                plotterPtr.advanceInsertionPoint();

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
    if (!analogDriver.getNewInspectPacketReady()) {return false;}


    //debug mks -- could a new packet be received between the above line and the
    //setting of the flag below -- miss a packet? -- packet processing in this
    //same thread then no prob -- different thread, then problem


    //ignore further calls to this function until a new packet is ready
    analogDriver.setNewInspectPacketReady(false);

    //retrieve all the info related to inpection control -- photo eye status,
    //encoder values, etc.
    analogDriver.getInspectControlVars(inspectCtrlVars);

    int recordStopPositionForHead = NO_HEAD;

    //On entering INSPECT mode, the system will wait until signalled that the
    //head is off the pipe or the pipe is out of the system, then it will wait
    //until the head is on the pipe or pipe enters the system before moving the
    //traces

    // manual control option will override signals from the Control Board and
    // begin inspection immediately after the operator presses the Inspect
    // button should manual control option be removed after fixing XXtreme unit?

    //if waiting for piece clear of system, do nothing until flag says true
    if (hdwVs.waitForOffPipe){

        if (manualInspectControl) {inspectCtrlVars.onPipeFlag = false;}

        if (inspectCtrlVars.onPipeFlag) {return false;}
        else {
            //piece has been removed; prepare for it to enter to begin
            hdwVs.waitForOffPipe = false;
            hdwVs.waitForOnPipe = true;
            //assume all heads up if off pipe and disable flagging
            flaggingEnableDelay1 = 0; flaggingEnableDelay2 = 0;
            hdwVs.head1Down = false; enableHeadTraceFlagging(1, false);
            hdwVs.head2Down = false; enableHeadTraceFlagging(2, false);
            }
        }

    if (manualInspectControl) {inspectCtrlVars.onPipeFlag = true;}

    //if waiting for piece to enter the head, do nothing until flag says true
    if (hdwVs.waitForOnPipe){

        if (!inspectCtrlVars.onPipeFlag) {return false;}
        else {
            hdwVs.waitForOnPipe = false; hdwVs.watchForOffPipe = true;
            initializePlotterOffsetDelays(inspectCtrlVars.encoder2Dir);
            //the direction of the linear encoder at the start of the inspection
            //sets the forward direction (increasing or decreasing encoder
            //count)
            inspectCtrlVars.encoder2FwdDir = inspectCtrlVars.encoder2Dir;

            //heads are up, flagging disabled upon start
            flaggingEnableDelay1 = 0; flaggingEnableDelay2 = 0;
            hdwVs.head1Down = false; enableHeadTraceFlagging(1, false);
            hdwVs.head2Down = false; enableHeadTraceFlagging(2, false);

            //set the text description for the direction of inspection
            if (inspectCtrlVars.encoder2FwdDir == awayDirection) {
                settings.inspectionDirectionDescription = settings.awayFromHome;
            }
            else {
                settings.inspectionDirectionDescription = settings.towardsHome;
            }

            //record the value of linear encoder at start of inspection
            //(this needs so be changed to store the value with each piece for
            // future units which might have multiple pieces in the system at
            //once)
            inspectCtrlVars.encoder2Start = inspectCtrlVars.encoder2;
            prevPixPosition = 0;
        }
    }

    if (manualInspectControl){
        inspectCtrlVars.head1Down = true;
        inspectCtrlVars.head2Down = true;
    }

    //if head 1 is up and goes down, enable flagging for all traces on head 1
    //a small distance delay is used to prevent flagging of the initial
    //transition
    if (!hdwVs.head1Down && inspectCtrlVars.head1Down){
        hdwVs.head1Down = true; flaggingEnableDelay1 = 6;
    }

    //if head 2 is up and goes down, enable flagging for all traces on head 2
    //a small distance delay is used to prevent flagging of the initial
    //transition; also enable track sync pulses from Control Board and saving
    //of map data; UT Boards already enabled to send map data
    if (!hdwVs.head2Down && inspectCtrlVars.head2Down){
        hdwVs.head2Down = true; flaggingEnableDelay2 = 6;
        analogDriver.setTrackPulsesEnabledFlag(true);
        analogDriver.setDataBufferIsEnabled(true);
    }

    //if head 1 is down and goes up, disable flagging for all traces on head 1
    if (hdwVs.head1Down && !inspectCtrlVars.head1Down){
        hdwVs.head1Down = false; enableHeadTraceFlagging(1, false);
        recordStopPositionForHead = HEAD_1;
    }

    //if head 2 is down and goes up, disable flagging for all traces on head 2
    //disable saving to the map buffer and disable remote sending of map data
    if (hdwVs.head2Down && !inspectCtrlVars.head2Down){
        hdwVs.head2Down = false; enableHeadTraceFlagging(2, false);
        recordStopPositionForHead = HEAD_2;
        analogDriver.setDataBufferIsEnabled(false);
        analogDriver.enableWallMapPackets(false);
    }

    //watch for piece to exit head
    if (hdwVs.watchForOffPipe){
        if (!inspectCtrlVars.onPipeFlag){

            //use tracking counter to delay after leading photo eye cleared
            //until position where modifier is to be added until the end of
            //the piece
            hdwVs.nearEndOfPieceTracker = hdwVs.nearEndOfPiecePosition;
            //start counting down to near end of piece modifier apply start
            //position
            hdwVs.trackToNearEndofPiece = true;

            analogDriver.requestAllEncoderValues();
            
            //calculate number counts recorded between start/stop eye triggers
            int pieceLengthEncoderCounts =
             Math.abs(inspectCtrlVars.encoder2 - inspectCtrlVars.encoder2Start);

            //convert to inches
            hdwVs.measuredLength =
                  hdwVs.convertEncoder2CountsToInches(pieceLengthEncoderCounts);

            //subtract the distance between the perpendicular eyes -- tracking
            //starts when lead eye hits pipe but ends when trailing eye clears,
            //so the extra distance between the eyes must be accounted for

            hdwVs.measuredLength -= hdwVs.photoEyeToPhotoEyeDistance;

            hdwVs.measuredLength /= 12; //convert to decimal feet

            hdwVs.watchForOffPipe = false;

            //set flag to force preparation for a new piece
            prepareForNewPiece = true;

        }//if (!inspectCtrlVars.onPipeFlag)
    }//if (hdwVs.watchForOffPipe)

    boolean newPositionData = true;  //signal that position has been changed

    moveEncoders(recordStopPositionForHead);

    return(newPositionData);

}//end of Hardware::collectEncoderDataInspectMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveEncoders
//
// Calculates position of the head/test piece to determine if plotters need
// to move and to perform any other actions required by linear motion.
//
// If pRecordStopPositionForHead == HEAD_1, the current position is recorded
// as the stop inspect location for sensors in head 1.
//
// If pRecordStopPositionForHead == HEAD_2, the current position is recorded
// as the stop inspect location for sensors in head 2.
//
// If pRecordStopPositionForHead == NO_HEAD, no location is recorded.
//
// Note 1:
//
// The code handling the pRecordStopPositionForHead must be before the test:
//   if (pixelsMoved == 0) {return;}. That flag is only valid on the first
// time through -- it is transmitted when the Control board sends the linear
// advance data, which does NOT always cause a pixel advance. If it does not,
// the test will bail and any code after will not execute.
//

void moveEncoders(int pRecordStopPositionForHead)
{

    //The control board sends new encoder data after a set number of encoder
    //counts, this set number approximates the distance for one pixel.  It is
    //not exact because there is a round off error which accumulates with each
    //packet sent.
    //To counterract this, the actual encoder count used to compute if a new
    //pixel has been reached.  Sometimes a packet may be sent for which the
    //encoder count does not calculate to the next pixel, so the buffer pointer
    //is not moved and incoming data is still stored in the previous pixel.
    //Sometimes, a packet will arrive which skips a pixel.  In that case, the
    //skipped pixels are filled with data from the previous pixel.

    //calculate the position in inches
    double position = hdwVs.convertEncoder2CountsToInches(
                    inspectCtrlVars.encoder2 - inspectCtrlVars.encoder2Start);

    //take absolute value so head moving in reverse works the same as forward
    position = Math.abs(position);


    //this code must be before if (pixelsMoved == 0) {return;}...see Note 1
    if (pRecordStopPositionForHead == HEAD_1){
        analogDriver.recordStopLocation(1, position);
    }
    else if (pRecordStopPositionForHead == HEAD_2){
        analogDriver.recordStopLocation(2, position);
    }

    //calculate the number of pixels moved since the last check
    int pixPosition = (int)(position * hdwVs.pixelsPerInch);

    //debug mks -- check here for passing zero point -- means pipe has backed
    //out of the system so remove segment

    //calculate the number of pixels moved since the last update
    int pixelsMoved = pixPosition - prevPixPosition;

    //do nothing if encoders haven't moved enough to reach the next pixel
    if (pixelsMoved == 0) {return;}

    prevPixPosition = pixPosition;

    if (flaggingEnableDelay1 != 0 && --flaggingEnableDelay1 == 0){
        enableHeadTraceFlagging(1, true);
        analogDriver.recordStartLocation(1, position);
    }

    if (flaggingEnableDelay2 != 0 && --flaggingEnableDelay2 == 0){
        enableHeadTraceFlagging(2, true);
        analogDriver.recordStartLocation(2, position);
    }

    moveTraces(pixelsMoved, position);

    // advance all maps not linked to specific channels
    moveMaps(pixelsMoved, position);

}//end of Hardware::moveEncoders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveTraces
//

void moveTraces(int pPixelsMoved, double pPosition)
{

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
    for (int ch = 0; ch < numberOfChannels; ch++) {
        for (int g = 0; g < analogDriver.getNumberOfGates(ch); g++){
            tracePtr = analogDriver.getTrace(ch,g);
            if (tracePtr != null) {tracePtr.positionAdvanced = false;}
        }
    }

    for (int ch = 0; ch < numberOfChannels; ch++){

        //get the number of gates for the currently selected channel
        numberOfGates = analogDriver.getNumberOfGates(ch);

        for (int g = 0; g < numberOfGates; g++){

            tracePtr = analogDriver.getTrace(ch,g);

            if (tracePtr != null && tracePtr.positionAdvanced == false){

                if (pPixelsMoved > 0) {
                    moveTracesForward(tracePtr, pPixelsMoved, pPosition);
                }
                else {
                    moveTracesBackward(
                                    tracePtr,Math.abs(pPixelsMoved),pPosition);
                }

            }//if (tracePtr != null...
        }// for (int g = 0; g < numberOfGates; g++)
    }// for (int ch = 0; ch < numberOfChannels; ch++)

}//end of Hardware::moveTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::moveMaps
//
// Advance or retract the map for all Boards with a map.
//
// Any boards without mapping or not in a mode allowing external control of
// advance will ignore the request.
//
// Parameter pPosition is the position of the head or inspection piece as
// measured from the point where the photo eye was blocked.
//

private void moveMaps(int pixelsMoved, double pPosition)
{

    analogDriver.triggerMapAdvance(pPosition);

}//end of Hardware::moveMaps
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
    pTrace.positionAdvanced = true;

    //the trace does not start until its associated sensor(s) have
    //reached the pipe after the photo eye has detected it
    if (pTrace.delayDistance > pPosition ) {return;}

    for (int x = 0; x < pPixelsMoved; x++){

        pTrace.traceData.advanceInsertionPoint();

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
// wip mks -- need to catch when pipe/head has reversed all the way back past
// the start point and exit the inspect mode!
//

void moveTracesBackward(Trace pTrace, int pPixelsMoved, double pPosition)
{

    //set flag so this trace's index won't be updated again by
    //another gate tied to this same trace
    pTrace.positionAdvanced = true;

    //the trace does not start until its associated sensor(s) have
    //reached the pipe after the photo eye has detected it, so don't reverse
    //past that point
    if (pTrace.delayDistance > pPosition ){return;}

    for (int x = 0; x < pPixelsMoved; x++){

        pTrace.traceData.eraseDataAtInsertionPoint();

        //currently, the nearStartOfPiece and nearEndOfPiece conditions are not
        //tracked in reverse -- should probably be fixed just in case reversing
        //occurs in these areas

        //if tracking to the end of the piece after end of piece photo eye
        // signal, reverse this process -- the tracker normally counts down
        // from endOfPiecePosition to zero, so count up when reversing

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
// This method is generally called when the inspection start/stop signals are
// received. In other places in the code there is a distance delay after this
// signal to avoid recording the glitches incurred while the head is settling.
//

void enableHeadTraceFlagging(int pHead, boolean pEnable)
{

    Plotter plotterPtr;
        for (ChartGroup chartGroup : chartGroups) {
            int nSC = chartGroup.getNumberOfStripCharts();
            for (int sc = 0; sc < nSC; sc++) {
                int nTr = chartGroup.getStripChart(sc).getNumberOfPlotters();
                for (int tr = 0; tr < nTr; tr++) {
                    plotterPtr = chartGroup.getStripChart(sc).getPlotter(tr);
                    if ((plotterPtr != null) && (plotterPtr.head == pHead)){
                        
                        plotterPtr.flaggingEnabled = pEnable;
                        
                        //set flag bit to draw a vertical bar to show the mask point
                        plotterPtr.placeEndMaskMarker();
                        
                    }//if ((tracePtr != null) && (tracePtr.head == pHead))
                } //for (int tr = 0; tr < nTr; tr++)
            } //for (int sc = 0; sc < nSC; sc++)
        } //for (int cg = 0; cg < chartGroups.length; cg++)

}//end of Hardware::enableHeadTraceFlagging
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::initializePlotterOffsetDelays
//
// Initializes the plotter start delays for the different types of plotter
// objects. See the notes in each method called for more details.

public void initializePlotterOffsetDelays(int pDirection)
{

    initializeTraceOffsetDelays(pDirection);

    analogDriver.initializeMapOffsetDelays(pDirection, awayDirection);

}//end of Hardware::initializePlotterOffsetDelays
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

    Plotter plotterPtr;

    double leadingTraceCatch, trailingTraceCatch;
    int lead = 0, trail = 0;
        for (ChartGroup chartGroup : chartGroups) {
            int nSC = chartGroup.getNumberOfStripCharts();
            for (int sc = 0; sc < nSC; sc++) {
                int nTr = chartGroup.getStripChart(sc).getNumberOfPlotters();
                //these used to find the leading trace (smallest offset) and the
                //trailing trace (greatest offset) for each chart
                leadingTraceCatch = Double.MAX_VALUE;
                trailingTraceCatch = Double.MIN_VALUE;
                for (int tr = 0; tr < nTr; tr++) {
                    plotterPtr = chartGroup.getStripChart(sc).getPlotter(tr);
                    //if the current direction is the "Away" direction, then set
                    //the offsets properly for the carriage moving away from the
                    //operator otherwise set them for the carriage moving towards
                    //the operator see more notes in this method's header
                    if (plotterPtr != null){
                        
                        //start with all false, one will be set true
                        plotterPtr.leadPlotter = false;
                        
                        if (pDirection == awayDirection) {
                            plotterPtr.delayDistance =
                                    plotterPtr.startFwdDelayDistance;
                        }
                        else {
                            plotterPtr.delayDistance =
                                    plotterPtr.startRevDelayDistance;
                        }
                        
                        //find the leading and trailing traces
                        if (plotterPtr.delayDistance < leadingTraceCatch){
                            lead = tr; leadingTraceCatch = plotterPtr.delayDistance;
                        }
                        if (plotterPtr.delayDistance > trailingTraceCatch){
                            trail = tr; trailingTraceCatch=plotterPtr.delayDistance;
                        }
                        
                    }//if (tracePtr != null)
                } //for (int tr = 0; tr < nTr; tr++)
                chartGroup.getStripChart(sc).getPlotter(lead).leadPlotter = true;
                chartGroup.getStripChart(sc).getPlotter(trail).trailPlotter = true;
                chartGroup.getStripChart(sc).setLeadTrailTraces(lead, trail);
            } //for (int sc = 0; sc < nSC; sc++)
        } //for (int cg = 0; cg < chartGroups.length; cg++)

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

public void startMarker(UTGate pGatePtr, int pWhichThreshold)
{

    if (pGatePtr.thresholds[pWhichThreshold].okToMark
                                                || markerMode == CONTINUOUS){
         //fire the alarm/marker output
        pulseAlarmMarker(pGatePtr.thresholds[pWhichThreshold].alarmChannel);
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

public void endMarker(UTGate pGatePtr, int pWhichThreshold)
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
// Hardware:runBackgroundProcesses
//
// Runs any background processes which are enabled.
//
// Should be called periodically by a thread other than the main GUI thread.
//
// WARNING: Some of the processes may block this thread from continuing until
//      they complete, so some of them should not be called during critical
//      inspection operations.

public void runBackgroundProcesses()
{

    //update the firmware in the UT board rabbits
    if(startUTRabbitUpdater){
        startUTRabbitUpdater = false;
        updateRabbitCode(Hardware.UT_RABBITS);
    }

    //update the firmware in the Control board rabbits
    if(startControlRabbitUpdater){
        startControlRabbitUpdater = false;
        updateRabbitCode(Hardware.CONTROL_RABBITS);
    }

}//end of Hardware::runBackgroundProcesses
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
// Writes various status and error messages to the log window.
//

public void logStatus(Log pLogWindow)
{

    analogDriver.logStatus(pLogWindow);

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

    active = false;

    analogDriver.shutDown();

}//end of Hardware::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::linkPlotters
//
// This function is called by Plotters (Traces, etc.) to link their buffers to
// specific hardware channels/gates and give a link back to variables in the
// Plotter object.
//

public void linkPlotters(int pChartGroup, int pChart, int pTrace,
        TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
                                                            Trace pTracePtr)
{

    analogDriver.linkPlotters(pChartGroup, pChart, pTrace, pTraceData,
                                        pThresholds, pPlotStyle, pTracePtr);

}//end of Hardware::linkPlotters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::run
//

@Override
public void run()
{

    while (true){

        //drive any simulation functions if they are active
        driveSimulation();

        waitSleep(10);

    }//while

}//end of Hardware::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of Hardware::waitSleep
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

//-----------------------------------------------------------------------------
// Hardware::getLinearDecimalFeetPerPixel
//
// Returns the decimal feet represented by each pixel.
//

@Override
public double getLinearDecimalFeetPerPixel()
{

    return(hdwVs.decimalFeetPerPixel);

}//end of Hardware::getLinearDecimalFeetPerPixel
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
