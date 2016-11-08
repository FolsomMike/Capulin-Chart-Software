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

    boolean reSynced;
    int pktID;
    int reSyncCount;
    int reSyncPktID;
    
    int msgBodyLen;
    
    ThreadSafeLogger logger;
    boolean simulate;

    int messageCount = 0;

    private static final int TIMEOUT = 5;
    private static final int PLC_MESSAGE_LENGTH = 29;
    private static final int PLC_MSG_PACKET_SIZE = 50;
    
    private static final int HEADER_BYTE = '^';
    private static final int ENCODER_EYE_CAL_CMD = '#';
    
//-----------------------------------------------------------------------------
// PLCEthernetController::PLCEthernetController (constructor)
//

public PLCEthernetController(String pPLCIPAddrS, int pPLCPortNum,
     EncoderValues pEncoderValues, ThreadSafeLogger pLogger, boolean pSimulate)
{

    plcIPAddrS = pPLCIPAddrS; plcPortNum = pPLCPortNum;
    encoderValues = pEncoderValues;
    logger = pLogger; simulate = pSimulate;
        
    inBuffer = new byte[PLC_MSG_PACKET_SIZE];
    outBuffer = new byte[PLC_MSG_PACKET_SIZE];

    //body length is message length minus header byte and packet type byte
    msgBodyLen = PLC_MESSAGE_LENGTH - 2;

    
}//end of PLCEthernetController::PLCEthernetController (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    parseIPAddrString();
    
    establishLink();
    
}//end of PLCEthernetController::init
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
// PLCEthernetController::establishLink
//
// Opens a socket with the PLC.
//

private void openSocket()
{

    if (plcIPAddr == null){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
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
        return;
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 176");
        logger.logMessage("Couldn't get I/O for PLC " + plcIPAddrS + "\n");
        logger.logMessage("--" + e.getMessage() + "--\n");
        return;
    }
    

    logger.logMessage("PLC connected.\n");
    
}//end of EthernetIOModule::establishLink
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

        if ( pktID == ENCODER_EYE_CAL_CMD) {return processEncoderEyeCalCmd();}

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
// PLCEthernetController::processEncoderEyeCalCmd
//
// Handles messages from the PLC which indicate that a sensor has changed
// state and includes the encoder counts at the time of change along with the
// current state of the sensor and the conveyor direction.
//

private int processEncoderEyeCalCmd()
{

    int timeOutProcess = 0;
    
    try{
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= msgBodyLen) {break;}
            waitSleep(10);
            }
        if (timeOutProcess < TIMEOUT && byteIn.available() >= msgBodyLen){
            int c = byteIn.read(inBuffer, 0, msgBodyLen);
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

private void parseEncoderEyeCalMsg(int numBytes, byte[] pBuf)
{

    if(numBytes <= 0){ return; }
    
    String msg = new String(pBuf, 0, msgBodyLen);

    encoderValues.setTextMsg(msg);
    
}//end of PLCEthernetController::parseEncoderEyeCalMsg
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
