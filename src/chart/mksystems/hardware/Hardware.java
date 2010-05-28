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

import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.TraceHdwVars;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Hardware
//
// This class creates and handles the hardware interface.
//

public class Hardware extends Object implements Runnable{

static public int SCAN = 0, INSPECT = 1, STOPPED = 2;
static public int INSPECT_WITH_TIMER_TRACKING = 3, PAUSED = 4;
int opMode = STOPPED;

Globals globals;
    
public HardwareVars hdwVs;
IniFile configFile;
HardwareLink analogDriver;
HardwareLink digitalDriver;
int numberOfAnalogChannels;
JTextArea log;

public boolean connected = false;
boolean collectDataEnabled = true;

//variables used by functions - declared here to avoid garbage collection
int numberOfChannels;
int numberOfGates;

String analogDriverName;
String digitalDriverName;

public static int INCHES = 0, MM = 1;

public int units = 0;

String threadSafeMessage; //this needs to be an array
int threadSafeMessagePtr; //points to next message in the array

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
    new Capulin1(configFile, true, numberOfAnalogChannels, hdwVs, log);
  
}//end of Hardware::createAnalogDriver
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::connect
//
// Establishes connections with remote devices.
//

public void connect()
{

connected = true;

//before attempting to connect with the boards, start a thread to run the
//simulation functions so the simulated boards can respond

if (analogDriver.getSimulate()){
    Thread thread = new Thread(this); thread.start();
    }

analogDriver.connect();

}//end of Hardware::connect
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

hdwVs.numberOfMultiples =
        pCalFile.readInt("Hardware", "Number of Multiples for Wall", 1);

units = pCalFile.readInt("Hardware",
                             "English(" + INCHES + ") / Metric(" + MM + ")", 0);
        
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

pCalFile.writeInt("Hardware", "Number of Multiples for Wall",
                                                    hdwVs.numberOfMultiples);

pCalFile.writeInt("Hardware",
                     "English(" + INCHES + ") / Metric(" + MM + ")", units);

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

public void startMonitor(int dMonitorPacketSize)
{

analogDriver.startMonitor(dMonitorPacketSize);

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

public void getMonitorPacket(byte[] pMonitorBuffer, boolean pRequestPacket)
{

analogDriver.getMonitorPacket(pMonitorBuffer, pRequestPacket);

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
// Hardware::requestPeakDataForAllBoards
//
// Sends requests to all boards for peak data from all channels.
//

public void requestPeakDataForAllBoards()
{

if (opMode == INSPECT || opMode == INSPECT_WITH_TIMER_TRACKING
                                                            || opMode == SCAN)
    analogDriver.requestPeakDataForAllBoards();

}//end of Hardware::requestPeakDataForAllBoards
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
// A thread will call this function periodically to allow it to transfer data
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

boolean peakDataAvailable = analogDriver.prepareData();

//do nothing if in stopped mode
if (opMode == STOPPED || opMode == PAUSED) return;

if (!collectDataEnabled) return;

//send a request to the remote device for a peak data packet
//the returned data packet will not be returned immediately, so the call to
//collectAnalogData later in this function will usually process packet(s)
//returned from the request sent on the previous pass

requestPeakDataForAllBoards();

//process position information from whatever device is handling the encoder
//inputs, or retrieve timer driven position updates for scanning or for systems
//which don't have hardware encoders

boolean newPositionData = collectEncoderData();

//collect analog data if new position or peak data is available
//if new position data is available, the function will fill in the trace buffer
// with either new data or repeated old data
//if new data is available but the encoder hasn't moved, the new data will
// be transferred to the trace buffer at the same position if the new data is
// a worse case than the data already there

if (newPositionData || peakDataAvailable) collectAnalogData();

}//end of Hardware::collectData
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
// if no new data has replaced it - if the trace pointer has not moved then the
// slot will be overwritten with the same value, if it has moved then the slot
// will be written with the same data as the previous slot.
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

        //retrieve data for the gate - process it whether it is new data
        //or old data - see note at top of this function
        analogDriver.getNewData(ch, g, hdwVs);

        if (hdwVs.gatePtr.tracePtr != null)
            if (hdwVs.gatePtr.plotStyle == TraceHdwVars.SPAN)
                collectAnalogDataMinAndMax(hdwVs.gatePtr);
            else           
                collectAnalogDataMinOrMax(hdwVs.gatePtr);

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

public void collectAnalogDataMinOrMax(Gate gatePtr)
{
        
int nextIndex = gatePtr.tracePtr.nextEmptySlot;

boolean dataStored = false;
        
//get the clock and data for this channel
int clockPos = gatePtr.clockPos;
int newData = gatePtr.dataPeak;
        
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
if (dataStored){

    //store the hardware channel for which the data was stored
    gatePtr.tracePtr.peakChannel = gatePtr.channelIndex;

    //store the clock position for the data in bits 8-0
    gatePtr.fBuffer[nextIndex] &= 0xfffffe00; //erase old
    gatePtr.fBuffer[nextIndex] += clockPos; //store new

    //check thresholds and store flag if violation - shift threshold
    //index by 2 as 0 = no flag and 1 = user flag
            
    for (int j = 0; j < gatePtr.thresholds.length; j++)
        if (gatePtr.thresholds[j].checkViolation(newData)){
            //store the index of threshold violated in byte 1
            gatePtr.fBuffer[nextIndex] &= 0xffff01ff; //erase old
            gatePtr.fBuffer[nextIndex] += (j+2) << 9; //store new
            //stop after first threshold violation found
            break;
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

//after the next empty slot has been filled with data, copy pointer to nextSlot
//so the data will be plotted
gatePtr.tracePtr.nextSlot = gatePtr.tracePtr.nextEmptySlot;

}//end of Hardware::collectAnalogDataMinOrMax
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

public void collectAnalogDataMinAndMax(Gate gatePtr)
{
        
int nextIndex = gatePtr.tracePtr.nextEmptySlot;

boolean dataStored = false;
        
//get the clock and data for this channel

//get the clock and data for this channel
int clockPos = gatePtr.clockPos;
int newMaxData = gatePtr.dataMaxPeak; //set by getChannelData
int newMinData = gatePtr.dataMinPeak; //set by getChannelData

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
        if (gatePtr.thresholds[j].checkViolation(newMaxData)){
            //store the index of threshold violated in byte 1
            gatePtr.fBuffer[nextIndex] &= 0xffff01ff; //erase old
            gatePtr.fBuffer[nextIndex] += (j+2) << 9; //store new
            //specify that max value was flagged - set bit 16 to 0
            gatePtr.fBuffer[nextIndex] &= 0xfffeffff; //erase old
            gatePtr.fBuffer[nextIndex] += 0 << 16; //store new flag
            //stop after first threshold violation found
            break;
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

    //store the hardware channel for which the data was stored
    gatePtr.tracePtr.peakChannel = gatePtr.channelIndex;

    //store the clock position for the data in byte 0
    gatePtr.fBuffer[nextIndex] &= 0xfffffe00; //erase old
    gatePtr.fBuffer[nextIndex] += clockPos; //store new

    //check thresholds and store flag if violation - shift threshold
    //index by 2 as 0 = no flag and 1 = user flag
            
    for (int j = 0; j < gatePtr.thresholds.length; j++)
        if (gatePtr.thresholds[j].checkViolation(newMinData)){
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

//after the next empty slot has been filled with data, copy pointer to nextSlot
//so the data will be plotted
gatePtr.tracePtr.nextSlot = gatePtr.tracePtr.nextEmptySlot;

}//end of Hardware::collectAnalogDataMinAndMax
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::collectEncoderData
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
// In "scan" or "timer tracking" mode, the nextIndex pointer is incremented
// with each call forcing the traces to "free run" across the screen regardless
// of encoder motion.
//

boolean collectEncoderData()
{

boolean newPositionData = false;
Trace tracePtr;

//if in scan mode or inspecting while using a timer to track the trace position,
//increment the array position with each call
if (opMode == SCAN || opMode == INSPECT_WITH_TIMER_TRACKING){

    newPositionData = true; //always new position data in this mode

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

                tracePtr.nextEmptySlot++;

                //the buffer is circular - start over at beginning
                if (tracePtr.nextEmptySlot == tracePtr.sizeOfDataBuffer)
                    tracePtr.nextEmptySlot = 0;
                }

            }// for (int g = 0; g < numberOfGates; g++)
        }// for (int ch = 0; ch < numberOfChannels; ch++)

    }//if (opMode == SCAN)...


return(newPositionData);

}//end of Hardware::collectEncoderData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//  Hardware:displayMessages
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
// Hardware::threadSafeLog
//
// This function allows a thread to add a log entry to the log window.  The
// actual call is passed to the invokeLater function so it will be safely
// executed by the main Java thread.
// 
//

public void threadSafeLog(String pMessage)
{

threadSafeMessage = pMessage; //store the message where the helper can find it

//Schedule a job for the event-dispatching thread: 
//creating and showing this application's GUI. 
    
javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() { threadSafeLogHelper(); } }); 

}//end of  Hardware::threadSafeLog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Hardware::threadSafeLogHelper
//
// This function is passed to invokeLater by threadSafeLog so that it will be
// run by the main Java thread and display the stored message on the log
// window.
// 
//

public void threadSafeLogHelper()
{

//display the stored message
log.append(threadSafeMessage);
    
}//end of  Hardware::threadSafeLogHelper
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

}//end of Capulin1::verifyAllDSPCode2
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
public void run() { 
         
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

}//end of class Hardware
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------    
    