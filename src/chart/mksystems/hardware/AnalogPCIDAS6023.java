/******************************************************************************
* Title: AnalogPCIDAS6023.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class handles the hardware interface to a Computer Board Inc
* AnalogPCIDAS6023 analog input board.
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
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import java.io.BufferedWriter;
import java.io.IOException;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class AnalogPCIDAS6023
//
// This class creates and handles the hardware interface.
//

public class AnalogPCIDAS6023 extends Object implements HardwareLink{

    String boardName = "CBIncPCIBoard";
    IniFile configFile;
    int numberOfAnalogChannels;

    boolean simulationMode = false;
    int[] simData;
    HardwareVars hdwVs;

    public int dataPeak; //used for channels without min and max peaks
    public int dataMaxPeak;  //used for channels with min and max peaks
    public int dataMinPeak;  //used for channels with minand max peaks

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::AnalogPCIDAS6023 (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
// If pSimulationMode is true, signals will be simulated.

AnalogPCIDAS6023(IniFile pConfigFile, boolean pSimulationMode,
                              int pNumberOfAnalogChannels, HardwareVars pHdwVs)
{

    configFile = pConfigFile; simulationMode = pSimulationMode;
    numberOfAnalogChannels = pNumberOfAnalogChannels;
    hdwVs = pHdwVs;

    simData = new int[numberOfAnalogChannels];
    for(int i=0; i<simData.length; i++) {simData[i] = -1;}

    //load configuration settings
    configure(configFile);

}//end of AnalogPCIDAS6023::AnalogPCIDAS6023 (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{


}//end of AnalogPCIDAS6023::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::connect
//
// Initializes the hardware.
//

@Override
public void connect()
{


}//end of AnalogPCIDAS6023::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

@Override
public void loadCalFile(IniFile pCalFile)
{

}//end of AnalogPCIDAS6023::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

@Override
public void saveCalFile(IniFile pCalFile)
{

}//end of AnalogPCIDAS6023::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::saveCalFileHumanReadable
//
// This saves a subset of the calibration data, the values of which affect
// the inspection process.
//
// The data is saved in a human readable format.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

@Override
public void saveCalFileHumanReadable(BufferedWriter pOut) throws IOException
{

}//end of AnalogPCIDAS6023::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::setMode
//
// Sets the mode to INSPECT, SCAN, or STOPPED.
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

@Override
public void setMode(int pOpMode)
{


}//end of AnalogPCIDAS6023::setMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::startMonitor
//
// Commands the hardware to enter the status monitor mode.
//

@Override
public void startMonitor()
{


}//end of AnalogPCIDAS6023::startMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::stopMonitor
//
// Commands the hardware to exit the status monitor mode.
//

@Override
public void stopMonitor()
{

}//end of AnalogPCIDAS6023::stopMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getMonitorPacket
//
// Retrieves a data packet containing monitor data.
//

@Override
public byte[] getMonitorPacket(boolean pRequestPacket)
{

return(null);

}//end of AnalogPCIDAS6023::getMonitorPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::zeroEncoderCounts
//
// Zeroes the encoder counts.
//

@Override
public void zeroEncoderCounts()
{

}//end of AnalogPCIDAS6023::zeroEncoderCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::pulseOutput1
//
// Pulses output 1.
//

@Override
public void pulseOutput1()
{

}//end of AnalogPCIDAS6023::pulseOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::turnOnOutput1
//
// Turn on output 1.
//

@Override
public void turnOnOutput1()
{

}//end of AnalogPCIDAS6023::turnOnOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::turnOffOutput1
//
// Turn off output 1.
//

@Override
public void turnOffOutput1()
{

}//end of AnalogPCIDAS6023::turnOffOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::requestAScan
//
// Requests an AScan dataset for the specified channel from the appropriate
// remote device.
//

@Override
public void requestAScan(int pChannel)
{

}//end of AnalogPCIDAS6023::requestAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getAScan
//
// Retrieves an AScan dataset for the specified channel.
//

@Override
public AScan getAScan(int pChannel)
{

return null;

}//end of AnalogPCIDAS6023::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::requestPeakData
//
// Sends a request to the remote device for a peak data packet for the
// specified channel.
//

@Override
public void requestPeakData(int pChannel)
{

}//end of AnalogPCIDAS6023::requestPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::requestPeakDataForAllBoards
//
// Requests peak data for all channels on all UT boards.
//
// The channel numbers sent to requestPeakData4 refer to the four analog
// channels on each board.  The utBoard objects have links back to the logical
// channels for each analog channel.
//

@Override
public void requestPeakDataForAllBoards()
{

}//end of AnalogPCIDAS6023::requestPeakDataForAllBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::prepareAnalogData
//
// Retrieves a data packet from the incoming data buffer from the analog board
// and distributes it to the newData variables in each gate.
//
// Returns true if new data is available, false if not.
//

@Override
public boolean prepareAnalogData()
{

return true;

}//end of AnalogPCIDAS6023::prepareAnalogData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::prepareControlData
//
// Retrieves a data packet from the incoming data buffer from the control board
// and distributes it to the newData variables in each gate.
//
// Returns true if new data is available, false if not.
//

@Override
public boolean prepareControlData()
{

return true;

}//end of AnalogPCIDAS6023::prepareControlData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::linkPlotters
//
// This function is called by Plotters (Traces, etc.) to link their buffers to
// specific hardware channels/gates and give a link back to variables in the
// Plotter object.
//

@Override
public void linkPlotters(int pChartGroup, int pChart, int pTrace,
            TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
                                                                Trace pTracePtr)
{

}//end of AnalogPCIDAS6023::linkPlotters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::configure
//
// Loads configuration settings from the configuration.ini file.
//
// Only configuration data for the ChartGroup itself are loaded here.  Each
// child object should be allowed to load its own data.
//

private void configure(IniFile pConfigFile)
{

}//end of AnalogPCIDAS6023::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getNumberOfChannels
//
// Returns the number of channels.
//

@Override
public int getNumberOfChannels()
{

    return 0;

}//end of AnalogPCIDAS6023::getNumberOfChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getChannels
//
// Returns a reference to the array of channels.
//

@Override
public Channel[] getChannels()
{

    Channel[] channels = null;
    return channels;

}//end of AnalogPCIDAS6023::getChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getGate
//
// Returns a reference to the specified gate.
//

@Override
public UTGate getGate(int pChannel, int pGate)
{

return null;

}//end of AnalogPCIDAS6023::getGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getNumberOfGates
//
// Returns the number of gates for the specified channel.

@Override
public int getNumberOfGates(int pChannel)
{

    return 0;

}//end of AnalogPCIDAS6023::getNumberOfGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getTrace
//
// Calls the getTrace function for the specified channel and gate.  See the
// channel and gate classes for more info.
//

@Override
public Trace getTrace(int pChannel, int pGate)
{

    return null;

}//end of AnalogPCIDAS6023::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getNewData
//

@Override
public boolean getNewData(int ch, int g, HardwareVars hdwVs)
{

    return true;

}//end of AnalogPCIDAS6023::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getChannelData
//
// Returns data for channel specified by pChannel.
//
//

@Override
public int getChannelData(int pChannel, int pSimDataType)
{

    //if in simulation mode, return simulated data
    if(simulationMode) {return(simulateChannelData(pChannel, pSimDataType));}

    return(50);

}//end of AnalogPCIDAS6023::getChannelData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::simulateChannelData
//
// Returns simulated data for channel specified by pChannel.
//

public int simulateChannelData(int pChannel, int pSimDataType)
{

    int clockPos;

    //return a random signal with low noise baseline and intermittent spikes

    if (pSimDataType == 0){

        double random = Math.random() * 5;

        simData[pChannel] = 3 + (int)random; //baseline noise

        //add occasional spikes
        if ( (int)(Math.random() * 80) == 1) {
            simData[pChannel] += (int)(Math.random() * 100);
        }

        //set a random clock position for this data point
        clockPos = (int)(Math.random() * 12) + 1; //1 to 12

        dataPeak = simData[pChannel];

        return(clockPos);

    }

    //return a sawtooth signal

    if (pSimDataType == 1){

        if (simData[pChannel] == 100) {simData[pChannel] = -1;}

        //set a random clock position for this data point
        clockPos = (int)(Math.random() * 12) + 1; //1 to 12

        //add in an offset calculated from channel number so multiple traces are
        //not overlayed on a chart
        dataPeak = ++simData[pChannel] + pChannel * 5;

        return(clockPos);

    }

    //return a gamma signal

    if (pSimDataType == 2){

        //simulate the minimum peak value of the signal

        double random = Math.random() * 5;

        simData[pChannel] = 47 + (int)random; //baseline noise

        //add occasional downward spikes
        if ( (int)(Math.random() * 80) == 1) {
            simData[pChannel] -= (int)(Math.random() * 40);
        }

        dataMinPeak = simData[pChannel];

        //simulate the maximum peak value of the signal

        random = Math.random() * 5;

        simData[pChannel] = 53 + (int)random; //baseline noise

        //add occasional upward spikes
        if ( (int)(Math.random() * 80) == 1) {
            simData[pChannel] += (int)(Math.random() * 40);
        }

        dataMaxPeak = simData[pChannel];

        //set a random clock position for this data point
        clockPos = (int)(Math.random() * 12) + 1; //1 to 12

        return(clockPos);

    }

    //if simulation data type not found, return default
    return(0);

}//end of AnalogPCIDAS6023::simulateChannelData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023:displayMessages
//
// Displays any messages received from the remote.
//

@Override
public void displayMessages()
{


}//end of AnalogPCIDAS6023::displayMessages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::updateRabbitCode
//
// Installs new firmware on the Rabbit micro-controllers.
//

@Override
public void updateRabbitCode(int pWhichRabbits)
{

}//end of AnalogPCIDAS6023::updateRabbitCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023:readRAM
//

@Override
public void readRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pCount, byte[] dataBlock)
{


}//end of AnalogPCIDAS6023::readRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::writeRAM
//
// Writes pValue to the RAM at pAddress on the specified chassis, board,
// DSP chip, DSP core, shared or local memory, and page.
//

@Override
public void writeRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pValue)
{

}//end of AnalogPCIDAS6023::writeRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::fillRAM
//
// Fills a block of memory with the specified address, size, and value.
//

@Override
public void fillRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pBlockSize, int pValue)
{


}//end of AnalogPCIDAS6023::fillRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getState
//
// Returns the state of various flags or values as selected by pWhich.
// If a flag is being requested, returns 0 for false and not 0 for true.
// If a value is being requested, returns the value.
//
//

@Override
public int getState(int pChassis, int pSlot, int pWhich)
{

    return 0;

}//end of AnalogPCIDAS6023::getState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::setState
//
// Sets the state of various flags or values as selected by pWhich.
// If a flag is being specified, pValue 0 for false and not 0 for true.
// If a value is being specified, it will be set to pValue.
//

@Override
public void setState(int pChassis, int pSlot, int pWhich, int pValue)
{

}//end of AnalogPCIDAS6023::setState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023:sendDataChangesToRemotes
//
// If any data has been changed, sends the changes to the remotes.
//
// This and all functions which set the change flags should be synchronized to
// avoid thread conficts.  Typically, one thread changes the data while another
// transmits it to the remotes.
//

@Override
public synchronized void sendDataChangesToRemotes()
{

}//end of AnalogPCIDAS6023::sendDataChangesToRemotes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::doTasks
//
// Should be called by a timer so that various tasks can be performed as
// necessary.  Since Java doesn't update the screen during calls to the user
// software, it is necessary to execute tasks in a segmented fashion if it
// is necessary to display status messages along the way.
//

@Override
public void doTasks()
{

    displayMessages();

}//end of AnalogPCIDAS6023:doTasks
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::driveSimulation
//
// Drive any simulation functions if they are active.
//

@Override
public void driveSimulation()
{

}//end of AnalogPCIDAS6023::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getSimulate
//
// Returns the simulate flag.  This flag is set if any simulation is being
// performed so that outside classes can adjust accordingly, such as by
// starting a thread to drive the simulation functions.
//

@Override
public boolean getSimulate()
{

    return (false);

}//end of AnalogPCIDAS6023::getSimulate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::logStatus
//
// Writes various status messages to the log window.
//

@Override
public void logStatus(Log pLogWindow)
{

}//end of AnalogPCIDAS6023::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023:verifyAllDSPCode2
//
// Verifies that the code in each DSP matches the file.  Used to check for
// transmission or corruption errors.
//

@Override
public void verifyAllDSPCode2()
{

}//end of AnalogPCIDAS6023::verifyAllDSPCode2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getInspectControlVars
//
// Transfers local variables related to inspection control signals and encoder
// counts.
//

@Override
public void getInspectControlVars(InspectControlVars pICVars)
{

}//end of AnalogPCIDAS6023::getInspectControlVars
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::various get/set functions
//

@Override
public boolean getOnPipeFlag(){return false;}
@Override
public boolean getInspectFlag(){return false;}
@Override
public boolean getNewInspectPacketReady(){return false;}
@Override
public void setNewInspectPacketReady(boolean pValue){}

//end of AnalogPCIDAS6023::various get/set functions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::xmtMessage
//

@Override
public int xmtMessage(int pMessage, int pValue)
{

    return(0);

}//end of AnalogPCIDAS6023::xmtMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getRepRateInHertz
//
// Returns the rep rate in Hertz used for all boards/channels.
//

@Override
public int getRepRateInHertz()
{

    return(0);

}//end of AnalogPCIDAS6023::getRepRateInHertz
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::setChartGroups
//
// Sets the chartGroups variable.
//

@Override
public void setChartGroups(ChartGroup pChartGroups [])
{

}//end of AnalogPCIDAS6023::setChartGroups
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::triggerMapAdvance
//
// Advance the map for board with index pBoard.
//
// Any board without mapping or not in a mode allowing external control of
// advance will ignore the request.
//
// Parameter pPosition is the position of the head or inspection piece as
// measured from the point where the photo eye was blocked.
//

@Override
public void triggerMapAdvance(double pPosition)
{


}//end of AnalogPCIDAS6023::triggerMapAdvance
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getNumberOfUTBoards
//
// Returns the number of UTBoards.
//

@Override
public int getNumberOfUTBoards()
{

    return(0);

}//end of AnalogPCIDAS6023::getNumberOfUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::getNumberOfUTBoards
//

@Override
public void calculateMapOffsetDelays(
        double pPhotoEye1DistanceFrontOfHead1,
        double pPhotoEye1DistanceFrontOfHead2,
        double pPhotoEye2DistanceFrontOfHead1,
        double pPhotoEye2DistanceFrontOfHead2
        )
{

}//end of AnalogPCIDAS6023::getNumberOfUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::initializeMapOffsetDelays
//

@Override
public void initializeMapOffsetDelays(int pDirection, int pAwayDirection)
{


}//end of AnalogPCIDAS6023::initializeMapOffsetDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::saveAllMapDataSetsToFile
//
// Stores the map data stored in Boards set up for mapping to file(s).
// Each board will save its own file.
//
// Any boards without mapping will ignore the request.
//

@Override
public void saveAllMapDataSetsToFile(
        String pFilename, String pJobFileFormat,
        String pInspectionDirectionDescription)
{

}//end of AnalogPCIDAS6023::saveAllMapDataSetsToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::recordStartLocation
//
// Records the linear position of the head/test piece when the start
// inspection signal is received for pHead.
//

@Override
public void recordStartLocation(int pHead, double pPosition)
{

}//end of AnalogPCIDAS6023::recordStartLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::recordStopLocation
//
// Records the linear position of the head/test piece when the stop
// inspection signal is received for pHead.
//

@Override
public void recordStopLocation(int pHead, double pPosition)
{

}//end of AnalogPCIDAS6023::recordStopLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// AnalogPCIDAS6023::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

@Override
public void shutDown()
{

}//end of AnalogPCIDAS6023::shutDown
//-----------------------------------------------------------------------------


}//end of class AnalogPCIDAS6023
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
