/******************************************************************************
* Title: PLCEthernetController.java
* Author: Mike Schoonover
* Date: 08/29/16
*
* Purpose:
*
* This is the parent class for modules which handle communications with the PLC
* via Ethernet.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import chart.ThreadSafeLogger;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;

//-----------------------------------------------------------------------------
// class PLCEthernetController
//

public class PLCEthernetController {


    public InetAddress plcIPAddr = null;
    String plcIPAddrS = null;
    int plcPortNum;
    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    byte[] inBuffer;
    byte[] outBuffer;
    DataOutputStream byteOut = null;
    DataInputStream byteIn = null;
    EncoderValues encoderValues;
    JLabel msgLabel;

    private double linearPositionOverride = Integer.MAX_VALUE;
    
    int machineState;
    boolean tubeTallied;
    
    SensorData datumByNum = null;
    int eyeByNum = UNDEFINED_EYE;    
    
    boolean reSynced;
    int pktID;
    int reSyncCount;
    int reSyncPktID;
    
    ThreadSafeLogger logger;
    boolean simulate;

    int messageCount = 0;

    private static final int TIMEOUT = 5;
    private static final int PLC_MESSAGE_LENGTH = 29;
    private static final int MSG_BODY_LEN = PLC_MESSAGE_LENGTH - 2;
    private static final int PLC_MSG_PACKET_SIZE = 50;
    
    private static final int HEADER_BYTE = '^';
    private static final int ENCODER_EYE_CAL_CMD = '#';

    private static int MAX_NUM_UNIT_SENSORS;    
    private static int NUM_UNIT_SENSORS;
    private static int MAX_NUM_JACKS_ANY_GROUP;
    private static int MAX_TOTAL_NUM_SENSORS;

    private static int UNDEFINED_GROUP;
    private static int INCOMING;
    private static int OUTGOING;
    private static int UNIT;

    private static int UNDEFINED_EYE;
    private static int EYE_A;
    private static int EYE_B;
    private static int SELF;
    
    private static int UNDEFINED_DIR;
    private static int STOPPED;
    private static int FWD;
    private static int REV;

    private static int UNDEFINED_STATE;
    private static int UNBLOCKED;
    private static int BLOCKED;
        
    private static int UNIT_SENSOR_INDEX;
    
    public static final int STATE_CHAR_POS = 5;
    public static final int DIR_CHAR_POS = 24;
        
    private static final int MS_UNIT_CLEAR = 0;
    private static final int MS_ENTRY_EYE_PASSED = 1;
    private static final int MS_EXIT_EYE_PASSED = 2;
    private static final int MS_TRAILING_EYE_PASSED = 3;
    

//-----------------------------------------------------------------------------
// PLCEthernetController::PLCEthernetController (constructor)
//

public PLCEthernetController(String pPLCIPAddrS, int pPLCPortNum,
     EncoderValues pEncoderValues, ThreadSafeLogger pLogger, JLabel pMsgLabel,
     boolean pSimulate)
{

    plcIPAddrS = pPLCIPAddrS; plcPortNum = pPLCPortNum;
    encoderValues = pEncoderValues;
    logger = pLogger; msgLabel = pMsgLabel; simulate = pSimulate;
    
    machineState = MS_UNIT_CLEAR;
    
    tubeTallied = false;
    
    inBuffer = new byte[PLC_MSG_PACKET_SIZE];
    outBuffer = new byte[PLC_MSG_PACKET_SIZE];

    //get a local copy of constants for easier use
    
    MAX_NUM_UNIT_SENSORS = EncoderCalValues.MAX_NUM_UNIT_SENSORS;
    NUM_UNIT_SENSORS = EncoderCalValues.NUM_UNIT_SENSORS;
    MAX_NUM_JACKS_ANY_GROUP = EncoderCalValues.MAX_NUM_JACKS_ANY_GROUP;
    MAX_TOTAL_NUM_SENSORS = EncoderCalValues.MAX_TOTAL_NUM_SENSORS;

    UNDEFINED_GROUP = EncoderCalValues.UNDEFINED_GROUP;
    INCOMING = EncoderCalValues.INCOMING;
    OUTGOING = EncoderCalValues.OUTGOING;
    UNIT = EncoderCalValues.UNIT;

    UNDEFINED_EYE = EncoderCalValues.UNDEFINED_EYE;
    EYE_A = EncoderCalValues.EYE_A;
    EYE_B = EncoderCalValues.EYE_B;
    SELF = EncoderCalValues.SELF;

    UNDEFINED_DIR = EncoderCalValues.UNDEFINED_DIR;
    STOPPED = EncoderCalValues.STOPPED;
    FWD = EncoderCalValues.FWD;
    REV = EncoderCalValues.REV;

    UNDEFINED_STATE = EncoderCalValues.UNDEFINED_STATE;
    UNBLOCKED = EncoderCalValues.UNBLOCKED;
    BLOCKED = EncoderCalValues.BLOCKED;
        
    UNIT_SENSOR_INDEX = EncoderCalValues.UNIT_SENSOR_INDEX;
    
}//end of PLCEthernetController::PLCEthernetController (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    resetAll();
    
    parseIPAddrString();
    
    establishLink();
    
}//end of PLCEthernetController::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::resetAll
//
// Resets all values to default ready for next run.
//

public void resetAll()
{
        
    linearPositionOverride = Integer.MAX_VALUE;        

    for(SensorData datum : encoderValues.getSensorData()){
        datum.resetAll();
    }
    
}//end of PLCEthernetController::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::parseIPAddrString
//
// Creates an InetAddress object from the IP address string plcIPAddrS.
//

public void parseIPAddrString()
{

    try{
        plcIPAddr = InetAddress.getByName(plcIPAddrS);
    }
    catch(UnknownHostException e){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
    }
    
}//end of PLCEthernetController::parseIPAddrString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::establishLink
//
// Opens a socket with the PLC and sends a greeting message.
//

private void establishLink()
{

    openSocket();

    sendString("^@Hello from VScan!        ");
    
//    sendTestMessages(); //debug mks
    
    //debug mks
/*    
    sendString("^@Line 2                  !"); //debug mks

    sendString("^*Line 2.1                !"); //debug mks

    sendString("^*Line 2.1a               !"); //debug mks    
    
    sendString("^#Line 2.2                !"); //debug mks
    
    sendString("^@Line 3       !"); //debug mks
    
    sendString("^@Line 4                  !"); //debug mks
    
    sendString("^@Line 5       !"); //debug mks
    
    sendString("^@Line 6       !"); //debug mks
    
    sendString("^@Line 7                  !"); //debug mks    

    sendString("^@Line 8                       !"); //debug mks    
    
    sendString("^@Line 9                  !"); //debug mks    
        
    //sendTestMessages(); //debug mks
  */
        
}//end of EthernetIOModule::establishLink
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::sendTestMessages
//
// Sends a batch of messages for testing purposes.
//

public void sendTestMessages()
{

    sendString("^@Block of test messages:  ");

    sendString("^#001|                     ");

    sendString("^#002|                     ");

    sendString("^#003|                     ");
    
    sendString("^*LL |03|009|0100|00090|001");

    sendString("^*TT |02|023|3422|00120|002");

    sendString("^*WT |01|023|3422|00150|001");

    sendString("^*WL |01|009|0100|00180|002");

    sendString("^*TL |02|023|3422|00210|001");

    sendString("^*LT |02|023|3422|00240|002");

    sendString("^*22L|02|023|3422|00270|001");

    sendString("^*22T|02|023|3422|01240|003");

    sendString("^*12L|03|009|0100|00090|001");

    sendString("^*12T|02|023|3422|01240|002");

    sendString("^*12L|02|023|3422|01240|003");

    sendString("^*45L|02|023|3422|01240|001");

    sendString("^*45T|02|023|3422|01240|003");

    sendString("^*LL |03|009|0100|00090|002");

}//end of EthernetIOModule::sendTestMessages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::openSocket
//
// Opens a socket with the PLC.
//

private void openSocket()
{

    if (plcIPAddr == null){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
        displayMsg("ERROR! PLC not connected!");        
        return;
    }

    try {

        //displays message on bottom panel of IDE
        logger.logMessage("Connecting to PLC at: " + plcIPAddrS + "...\n");

        if (!simulate) {socket = new Socket(plcIPAddr, plcPortNum);}
        else {
            return;
        }

        //set amount of time in milliseconds that a read from the socket will
        //wait for data - this prevents program lock up when no data is ready
        socket.setSoTimeout(250);

        // the buffer size is not changed here as the default ends up being
        // large enough - use this code if it needs to be increased
        //socket.setReceiveBufferSize(10240 or as needed);

        out = new PrintWriter(socket.getOutputStream(), true);

        in = new BufferedReader(new InputStreamReader(
                                            socket.getInputStream()));

        byteOut = new DataOutputStream(socket.getOutputStream());
        byteIn = new DataInputStream(socket.getInputStream());

    }
    catch (UnknownHostException e) {
        logSevere(e.getMessage() + " - Error: 171");
        logger.logMessage("Unknown host: PLC " + plcIPAddrS + ".\n");
        displayMsg("ERROR! PLC not connected!");        
        return;
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 176");
        logger.logMessage("Couldn't get I/O for PLC " + plcIPAddrS + "\n");
        logger.logMessage("--" + e.getMessage() + "--\n");
        displayMsg("ERROR! PLC not connected!");
        return;
    }
    

    logger.logMessage("PLC connected.\n");
    
    displayMsg("PLC connected...");
    
}//end of EthernetIOModule::openSocket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::displayMsg
//
// Displays pMsg on the msgLabel.
//
// No need for threadsafe code as this class runs in the Java main GUI thread.
//

public void displayMsg(String pMsg)
{

    msgLabel.setText(pMsg);

}//end of EthernetIOModule::displayMsg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::sendString
//
// Sends a string via the socket. A | symbol and single digit alpha message
// count will be appended to the message.
//

public void sendString(String pValue)
{

    if (byteOut == null){ return; }

    pValue = pValue + "|" + getAndIncrementMessageCount();
  
    assert(pValue.length() == PLC_MESSAGE_LENGTH);
    
    try{
        for(int i = 0; i < pValue.length(); i++){
          byteOut.writeByte((byte) pValue.charAt(i));
        }
        byteOut.flush();
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 255");
    }

}//end of EthernetIOModule::sendString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::getAndIncrementMessageCount
//
// Returns the current message count value as a string and increments it for
// the next use. Rolls back to 0 when value of 10 reached; range is 0~9
//

private String getAndIncrementMessageCount()
{

    int value = messageCount;

    messageCount++;
    
    if (messageCount == 10){ messageCount = 0; }

    return(Integer.toString(value));

}//end of EthernetIOModule::getAndIncrementMessageCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::processOneDataPacket
//
// This function processes a single data packet if it is available.  If
// pWaitForPkt is true, the function will wait until data is available.
//
// The amount of time the function is to wait for a packet is specified by
// pTimeOut.  Each count of pTimeOut equals 10 ms.
//
// This function should be called often to allow processing of data packets
// received from the remotes and stored in the socket buffer.
//
// All packets received from the remote devices should begin with the value
// stored in the constant HEADER_BYTE, followed by the packet type identifier.
//
// Returns number of bytes retrieved from the socket, not including the
// header byte and the packet type identifier.
//
// Thus, if a non-zero value is returned, a packet was processed.  If zero
// is returned, some bytes may have been read but a packet was not successfully
// processed due to missing bytes or header corruption.
// A return value of -1 means that the buffer does not contain a packet.
//
// Currently, incoming packets do not have checksums. Data integrity is left to
// the TCP/IP protocol.
//

public int processOneDataPacket(boolean pWaitForPkt, int pTimeOut)
{

    if (byteIn == null) {return -1;}  //do nothing if the port is closed

    try{

        int timeOutWFP;
        
        //wait a while for a packet if parameter is true
        if (pWaitForPkt){
            timeOutWFP = 0;
            while(byteIn.available() < 7 && timeOutWFP++ < pTimeOut){
                waitSleep(10);
            }
        }

        //wait until 2 bytes are available - this should be the header byte
        //and the packet identifier
        if (byteIn.available() < 2) {return -1;}

        //read the bytes in one at a time so that if an invalid byte is
        //encountered it won't corrupt the next valid sequence in the case
        //where it occurs within 3 bytes of the invalid byte

        //check the byte to see if it is a valid header byte
        //if not, jump to resync which deletes bytes until a valid first header
        //byte is reached

        //if the reSynced flag is true, the buffer has been resynced and a
        //header byte has already been read from the buffer so it shouldn't be
        //read again

        //after a resync, the function exits without processing any packets

        if (!reSynced){
            //look for the 0xaa byte unless buffer just resynced
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] != (byte)HEADER_BYTE) {reSync(); return 0;}
        }
        else {reSynced = false;}

        //read in the packet identifier
        byteIn.read(inBuffer, 0, 1);

        //store the ID of the packet (the packet type)
        pktID = inBuffer[0];

        if ( pktID == ENCODER_EYE_CAL_CMD) {return handleEncoderEyeCalCmd();}

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3453");
    }

    return 0;

}//end of PLCEthernetController::processOneDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::reSync
//
// Clears bytes from the socket buffer until header byte reached which signals
// the *possible* start of a new valid packet header or until the buffer is
// empty.
//
// If a header byte is found, the flag reSynced is set true so that other
// functions will know that the header byte has already been removed from the
// stream, signalling the possible start of a new packet header.
//
// There is a special case where an erroneous header byte is found just before
// the valid header byte which starts a new packet - the first header byte is
// the last byte of the previous packet.  In this case, the next packet will be
// lost as well.  This should happen rarely.
//

public void reSync()
{

    reSynced = false;

    //track the number of times this function is called, even if a resync is not
    //successful - this will track the number of sync errors
    reSyncCount++;

    //store info pertaining to what preceded the reSync - these values will be
    //overwritten by the next reSync, so they only reflect the last error
    //NOTE: when a reSync occurs, these values are left over from the PREVIOUS
    // good packet, so they indicate what PRECEDED the sync error.

    reSyncPktID = pktID;

    try{
        while (byteIn.available() > 0) {
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] == (byte)HEADER_BYTE) {reSynced = true; break;}
            }
        }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 448");
    }

}//end of PLCEthernetController::reSync
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::handleEncoderEyeCalCmd
//

private int handleEncoderEyeCalCmd()
{

    int status = processEncoderEyeCalCmd();

    if (status == 0){ return(status); }
    
    handleEncoderEyeValueReset();
    
    processStateMachine();
    
    return(status);
    
}//end of PLCEthernetController::handleEncoderEyeCalCmd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::handleEncoderEyeValueReset
//
// Any time the entry start inspection eye is unblocked or in unknown state,
// reset all values for all sensors.
//

private void handleEncoderEyeValueReset()
{
    
    //debug mks
    //can't do this!
    //When inspection eye blocked, PLC sends message and flag set blocked
    //then code resets all flags and this gets executed repeatedly because
    //the eye flag is undefined.
    //Also, when trailing end of pipe clears start eye, this causes constant
    //reset while pipe is still being tracked
  
//if(encoderValues.getSensorData().get(UNIT_SENSOR_INDEX).lastState != BLOCKED){    
//    resetAll();
//}
    
    //debug mks end
    
      
}//end of PLCEthernetController::handleEncoderEyeValueReset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::processStateMachine
//
// Processes the state machine which controls sensor state changes and
// encoder calibrations.
//

public void processStateMachine() //debug mks -- make this private
{

    
/* debug mks -- test code 
    for(int i=0; i<encoderValues.getTotalNumSensors(); i++){
        
        getSensorByNumber(i);
        
    }
    
debug mks end    
    
*/
    
}//end of PLCEthernetController::processStateMachine
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::getSensorByNumber
//
// Sets class variables SensorData datumByNum and eyeByNum based on the
// value of pSensorNum. pSensorNum equal to 0 is the first entry jack while
// a value of TOTAL_NUM_SENSORS - 1 is the last exit jack.
//
// The sensors are treated as a single series starting at number 0 (the first
// entry jack), running through all the entry jacks, through the unit sensors,
// through the exit jacks and ending with the last exit jack.
//
// If pSensorNum refers to one of the unit sensors, eyeByNum is set to SELF
// as there is only a single eye.
//
// If pSensorNum is less than zero or greater than the index of the last
// sensor, datumByNum is set null and eyeByNum is set to UNDEFINED_EYE.

private void getSensorByNumber(int pSensorNum)
{

    int numEntryJackStands = encoderValues.getNumEntryJackStands();
    int totalNumSensors = encoderValues.getTotalNumSensors();                    
    
    if(pSensorNum < 0 || pSensorNum >= totalNumSensors){ 
        datumByNum = null; eyeByNum = UNDEFINED_EYE; return;
    }
    
    int index;
    
    //handle references to entry jack group at first of list
    
    if(pSensorNum < numEntryJackStands * 2){
        index = UNIT_SENSOR_INDEX - (numEntryJackStands - pSensorNum / 2);
        datumByNum = encoderValues.getSensorData().get(index);
        eyeByNum = pSensorNum % 2 == 0 ? EYE_A : EYE_B;
        return;
    }
    
    //handle references to exit jack group at end of list    

    if(pSensorNum >= numEntryJackStands * 2 + NUM_UNIT_SENSORS){
        index = UNIT_SENSOR_INDEX + NUM_UNIT_SENSORS +
                ((pSensorNum - numEntryJackStands * 2 - NUM_UNIT_SENSORS) / 2); 
        datumByNum = encoderValues.getSensorData().get(index);
        eyeByNum = (pSensorNum+NUM_UNIT_SENSORS) % 2 == 0 ? EYE_A : EYE_B;
        return;
    }
    
    //handle references to unit sensor group at end of list        
    //anything not caught above must be a unit sensor in the middle of the list
    
    index = UNIT_SENSOR_INDEX + (pSensorNum - numEntryJackStands * 2);
    datumByNum = encoderValues.getSensorData().get(index);
    eyeByNum = SELF;
    
}//end of PLCEthernetController::getSensorByNumber
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::processEncoderEyeCalCmd
//
// Handles messages from the PLC which indicate that a sensor has changed
// state and includes the encoder counts at the time of change along with the
// current state of the sensor and the conveyor direction.
//
// Returns the number of bytes read from the buffer on success.
// Returns 0 on failure.
//

private int processEncoderEyeCalCmd()
{

    int timeOutProcess = 0;
    
    try{
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= MSG_BODY_LEN) {break;}
            waitSleep(10);
            }
        if (timeOutProcess < TIMEOUT && byteIn.available() >= MSG_BODY_LEN){
            int c = byteIn.read(inBuffer, 0, MSG_BODY_LEN);
            encoderValues.setSensorTransitionDataChanged(true);
            parseEncoderEyeCalMsg(c, inBuffer);
            return(c);
            }
        else {
            
            }
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 486");
    }

    return 0;

}//end of PLCEthernetController::processEncoderEyeCalCmd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::parseEncoderEyeCalMsg
//

public void parseEncoderEyeCalMsg(int numBytes, byte[] pBuf) //debug mks -- set this to private
{

    if(numBytes <= 0){ return; }
    
    String msg = new String(pBuf, 0, MSG_BODY_LEN);

    encoderValues.setTextMsg(msg);

    SensorData sensor = parseSensorID(msg.substring(0,4));
    
    //if sensor ID invalid, then ignore message
    if(sensor == null){ return; }

    //parse direction
    
    if (msg.charAt(DIR_CHAR_POS) == 'F'){ sensor.direction = FWD; }
    else if (msg.charAt(DIR_CHAR_POS) == 'R'){ sensor.direction = REV; }
    else if (msg.charAt(DIR_CHAR_POS) == 'S'){ sensor.direction = STOPPED; }
    else{  sensor.direction = UNDEFINED_DIR; }
 
   //parse state
    
    int sensorState;
    
    if (msg.charAt(STATE_CHAR_POS) == 'U'){ sensorState = UNBLOCKED; }
    else if (msg.charAt(STATE_CHAR_POS) == 'B'){ sensorState = BLOCKED; }
    else{ sensorState = UNDEFINED_STATE; }
    
    //parse and store encoder counts along with the sensor state

    sensor.setEncoderCounts(sensorState,
                            parseEncoderCount(msg, 10),
                            parseEncoderCount(msg, 17));

    handleLinearPositionUpdateFlagging(sensor);
    
}//end of PLCEthernetController::parseEncoderEyeCalMsg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::handleLinearPositionUpdateFlagging
//
// This function handles sensor changes which trigger corrections to the
// current linear position.
//
// Currently, when any exit sensor changes to blocked state while conveyor
// is moving forward, the position of that sensor is stored so other code can
// use it to correct the current linear position.
//
// If a valid transition has occurred, linearPositionOverride will be set to
// the linear position of the transitioning sensor. Otherwise, the variable
// will be set to Integer.MAX_VALUE.
//

private void handleLinearPositionUpdateFlagging(SensorData pSensor)
{
    
    //if this sensor already used for correction, ignore it to prevent chatter
    if (pSensor.appliedAsCorrectionFwd) { return; }
    
    //if sensor is not the unit trailing or any of the exit jacks, then
    //do not use for linear position adjustment
    
    if(
       !(pSensor.sensorGroup == SensorData.UNIT_GROUP && pSensor.sensorNum == 2)
            &&
       pSensor.sensorGroup != SensorData.EXIT_GROUP) { return; }
    
    if(pSensor.direction != FWD) { return; }
    
    if (pSensor.lastState != BLOCKED) { return; }

    //only use each sensor once for correction while in forward motion to
    //prevent false triggering due to sensor chatter
    //need to add logic to clear this if cleared in reverse direction and
    //to only use sensor once when reversing
    
    pSensor.appliedAsCorrectionFwd = true;
    
    linearPositionOverride = pSensor.getEyeDistToEye1(pSensor.lastEyeChanged);
    
}//end of PLCEthernetController::handleLinearPositionUpdateFlagging
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::getAndClearLinearPositionOverride
//
// Returns linearPositionOverride and sets it to Integer.MAX_VALUE.
//

public double getAndClearLinearPositionOverride(){ 

    double lpo = linearPositionOverride;
    
    linearPositionOverride = Integer.MAX_VALUE;
    
    return(lpo);

}//end of PLCEthernetController::getAndClearLinearPositionOverride
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::parseEncoderCount
//
// Parses the 6 digit substring in pMsg starting at pStart location. Returns
// the value as an integer.
//
// On error, returns Integer.MAX_VALUE.

private int parseEncoderCount(String pMsg, int pStart)
{        

    String s = pMsg.substring(pStart, pStart + 6); //debug mks -- remove this
    
    try{
        return(Integer.valueOf(pMsg.substring(pStart, pStart + 6).trim()));
    }
    catch(NumberFormatException nfe){
        return(Integer.MAX_VALUE); //error parsing
    }
    
}//end of PLCEthernetController::parseEncoderCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::parseSensorID
//
// Parses the phrase in pID to determine which SensorData object in the
// sensorData ArrayList is addressed -- a reference to that SensorData object
// is returned.
//
// The format of the ID string is: g|nn
// Where g is:
//  I -> entry jack sensor
//  O -> exit jack sensor; 
//  U -> unit sensor (mounted on center-section or trailer)
//
// and nn is a two digit number which IDs the sensor in the group.
// Each jack has two sensors 0:1, 2:3, 4:5, etc.
//
// As each jack has two eyes, lastEyeChanged is set to show which eye last
// changed state (EYE_A OR EYE_B). For cases such as a Unit eye which represents
// a single sensor, lastEyeChanged is set to SELF.
//
// The entry sensor is that used to trigger start of inspection.
// The exit sensor is that used to trigger end of inspection.
// The entry, exit, sensors are stored in the SensorData object in the middle
// of the list, the entry jack sensors fill the list before the middle and
// the exit jack sensors fill the list after the middle:
//
//  0: entry jack sensor 18 & 19  
//     ...
//  8: entry jack sensor 02 & 03
//  9: entry jack sensor 00 & 01
// 10: unit sensor 00 (entry inspection start sensor)
// 11: exit sensor 01 (exit inspection end sensor)
// 12: trailing unit sensor 02 (sensor after second clamp roller) 
// 13: exit jack sensor 00 & 01
// 14: exit jack sensor 02 & 03
//      ...
// 22: exit jack sensor 18 & 19
//

private SensorData parseSensorID(String pID)
{

    int sensorNum;
            
    try{
        sensorNum = Integer.valueOf(pID.substring(2,4).trim());
        if ((sensorNum < 0) || (sensorNum >= MAX_NUM_JACKS_ANY_GROUP * 2)){
            return(null);
        }
    }
    catch(NumberFormatException nfe){
        return(null); //do nothing if number is invalid
    }

    SensorData sensorData = null;
    
    //sensors on unit
    if (pID.charAt(0) == 'U'){
        sensorData = encoderValues.getSensorData().get(sensorNum + 10);
        sensorData.sensorNum = sensorNum; sensorData.lastEyeChanged = SELF;
        return(sensorData);
    }
    
    // for jacks, EyeA is always towards the incoming side
    // thus for the entry jacks, sensor 0 starts at the unit and will be
    // an EyeB and then alternate outwards; for outgoing jacks, sensor 0
    // also starts at the unit but sensor 0 for that group is an EyeA and
    // alternates outward away from the unit
    
    //handle entry jack sensors which are numbered 0-10 starting from unit
    if (pID.charAt(0) == 'I'){
        int i = (MAX_NUM_JACKS_ANY_GROUP-1) - sensorNum / 2;
        sensorData = encoderValues.getSensorData().get(i);
        sensorData.sensorNum = sensorNum; 
        sensorData.lastEyeChanged = sensorNum % 2 == 0 ? EYE_B : EYE_A;
        return(sensorData);
    }

    //handle exit jack sensors which are numbered 0-10 starting from unit
    if (pID.charAt(0) == 'O'){
        int i = (MAX_NUM_JACKS_ANY_GROUP+MAX_NUM_UNIT_SENSORS) + sensorNum / 2;
        sensorData = encoderValues.getSensorData().get(i);
        sensorData.sensorNum = sensorNum;        
        sensorData.lastEyeChanged = sensorNum % 2 == 0 ? EYE_A : EYE_B;
        return(sensorData);
    }
        
    return(sensorData);

}//end of PLCEthernetController::parseSensorID
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// PLCEthernetController::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

public void shutDown()
{

    //close everything - the order of closing may be important

    try{
        if (byteOut != null) {byteOut.close();}
        if (byteIn != null) {byteIn.close();}
        if (out != null) {out.close();}
        if (in != null) {in.close();}
        if (socket != null) {socket.close();}
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 267");
    }

}//end of PLCEthernetController::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::waitSleep
//
// Sleeps for pTime milliseconds.
//

private void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of PLCEthernetController::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

private void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Board::logSevere
//-----------------------------------------------------------------------------
    
}//end of class PLCEthernetController
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
