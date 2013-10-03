/******************************************************************************
* Title: Board.java
* Author: Mike Schoonover
* Date: 5/7/09
*
* Purpose:
*
* This class is the parent class for those which handle various boards via
* Ethernet.
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
import chart.ThreadSafeLogger;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.Map2D;
import chart.mksystems.stripchart.Map2DData;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class InstallFirmwareSettings
//
// This class is used to pass in all necessary settings to the
// installNewRabbitFirmware function.
//

class InstallFirmwareSettings extends Object{

    public byte loadFirmwareCmd;
    public byte noAction;
    public byte error;
    public byte sendDataCmd;
    public byte dataCmd;
    public byte exitCmd;

}//end of class InstallFirmwareSettings
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Board
//
//

public abstract class Board extends Object implements Runnable{


    short rabbitControlFlags = 0;
    boolean enabled = false;
    int type;
    int targetMapChannel;
    int boardChannelForMapDataSource;
    int headForMapDataSensor;
    double distanceMapSensorToFrontEdgeOfHead;
    double mapSensorDelayDistance;
    double startFwdDelayDistance = 0;
    double startRevDelayDistance = 0;

    int controlFlags = 0;
    String configFilename;
    IniFile configFile;
    String boardName;
    int boardIndex;
    JTextArea log;
    ThreadSafeLogger logger;

    boolean setupComplete = false; //set true if set was completed
    boolean ready = false; //set true if board is successfully setup

    boolean simulate;

    public InetAddress ipAddr;
    String ipAddrS;
    public int chassisAddr = -1, slotAddr = -1;
    String chassisSlotAddr;
    static int FIRMWARE_LOAD_TIMEOUT = 999999;

    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    byte[] inBuffer;
    byte[] outBuffer;
    DataOutputStream byteOut = null;
    DataInputStream byteIn = null;

    int TIMEOUT = 50;
    int timeOutProcess = 0; //use this one in the packet process functions

    final static int ADVANCE_NEVER = 0;
    final static int ADVANCE_ON_TDC_CODE = 1;
    final static int ADVANCE_BY_CONTROLLER = 2;
    final static int ADVANCE_ON_ENCODER_CODE = 3;

    int mapAdvanceMode = ADVANCE_NEVER;

    Map2D map2D = null;
    Map2DData map2DData = null;
    int map2DDataColumn[] = null;


//-----------------------------------------------------------------------------
// Board::Board (constructor)
//

public Board(JTextArea pLog)
{

    log = pLog;

    //create an object to log messages to the log window in thread safe manner
    logger = new ThreadSafeLogger(log);

}//end of Board::Board (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::configure
//
// Loads configuration settings from the configuration.ini file.
//

void configure(IniFile pConfigFile)
{


}//end of Board::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::configureExtended
//
// Loads further configuration settings from the configuration.ini file.
// These settings are stored using the boards chassis and slot addresses, so
// they cannot be loaded until after the host has uploaded the FPGA code to the
// board.
//

void configureExtended(IniFile pConfigFile)
{

    String section = "UT Board in Chassis " + chassisAddr + " Slot " + slotAddr;

    enabled = pConfigFile.readBoolean(section, "Enabled", true);

}//end of Board::configureExtended
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendRabbitControlFlags
//
// Sends the rabbitControlFlags value to the remotes. These flags control
// the functionality of the remotes.
//
// The paramater pCommand is the command specific to the subclass for its
// Rabbit remote.
//

public void sendRabbitControlFlags(final byte pCommand)
{

    sendBytes(pCommand,
                (byte) ((rabbitControlFlags >> 8) & 0xff),
                (byte) (rabbitControlFlags & 0xff)
                );

}//end of Board::sendRabbitControlFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getChassisAddress
//

public int getChassisAddress()
{

    return(chassisAddr);

}//end of Board::getChassisAddress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getSlotAddress
//

public int getSlotAddress()
{

    return(slotAddr);

}//end of Board::getSlotAddress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::compareChassisAndSlot
//
// Returns true if pChassis and pSlot match the boards chassis and slot address.
//

public boolean compareChassisAndSlot(int pChassis, int pSlot)
{


    if (pChassis == chassisAddr && pSlot == slotAddr){
        return(true);
    }
    else{
        return(false);
    }

}//end of Board::compareChassisAndSlot
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::isEnabled
//
// Returns true if the channel is enabled, false otherwise.
//

public boolean isEnabled()
{

    return(enabled);

}//end of Board::isEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::setIPAddr
//
// Sets the IP address for this board.
//

public void setIPAddr(InetAddress pIPAddr)
{

    ipAddr = pIPAddr;

    ipAddrS = pIPAddr.toString();

}//end of Board::setIPAddr
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes
//
// Sends a variable number of bytes (one or more) to the remote device,
// prepending a valid header and appending the appropriate checksum.
//

void sendBytes(byte... pBytes)
{

    int checksum = 0;

    sendHeader(); //send the packet header

    for(int i=0; i<pBytes.length; i++){
        outBuffer[i] = pBytes[i];
        checksum += pBytes[i];
    }

    //calculate checksum and put at end of buffer
    outBuffer[pBytes.length] = (byte)(0x100 - (byte)(checksum & 0xff));

    //send packet to remote
    if (byteOut != null) {
        try{
              byteOut.write(outBuffer, 0 /*offset*/, pBytes.length + 1);
              byteOut.flush();
        }
        catch (IOException e) {
            logSevere(e.getMessage() + " - Error: 422");
        }
    }

}//end of Board::sendBytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendHeader
//
// Sends a valid packet header without flushing.
//

void sendHeader()
{

    outBuffer[0] = (byte)0xaa; outBuffer[1] = (byte)0x55;
    outBuffer[2] = (byte)0xbb; outBuffer[3] = (byte)0x66;

    //send packet to remote
    if (byteOut != null) {
        try{
            byteOut.write(outBuffer, 0 /*offset*/, 4);
        }
        catch (IOException e){
            logSevere(e.getMessage() + " - Error: 569");
        }
    }

}//end of Board::sendHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::waitForNumberOfBytes
//
// Waits until pNumBytes number of data bytes are available in the socket.
//
// Returns true if the bytes become available before timing out, false
// otherwise.
//

boolean waitForNumberOfBytes(int pNumBytes)
{

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= pNumBytes) {return(true);}
            waitSleep(10);
        }
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 595");
        return(false);
    }

    return(false);

}//end of Board::readBytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::readBytes
//
// Retrieves pNumBytes number of data bytes from the packet and stores them
// in inBuffer.
//
// Returns number of bytes retrieved from the socket.
//
// If the attempt times out, returns 0.
//

public int readBytes(int pNumBytes)
{

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= pNumBytes) {break;}
            waitSleep(10);
        }
        if (byteIn.available() >= pNumBytes){
            return byteIn.read(inBuffer, 0, pNumBytes);
        }
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 595");
    }

    return 0;

}//end of Board::readBytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getRemoteData
//
// Retrieves two data bytes from the remote device, using the command specified
// by pCommand.
//
// The first byte is returned.
//
// If pForceProcessDataPackets is true, the processDataPackets function will
// be called.  This is for use when that function is not already being called
// by another thread.
//
// IMPORTANT NOTE: For this function to work, the sub-class must catch
// the return packet type in its processOneDataPacket method and then read in
// the necessary data -- a simple way is to call process2BytePacket after
// catching the return packet.
// Search for GET_STATUS_CMD in UTBoard to see an example.
//

byte getRemoteData(byte pCommand, boolean pForceProcessDataPackets)
{

    if (byteIn == null) {return(0);}

    sendBytes(pCommand); //request the data from the remote

    //force waiting for and processing of receive packets
    if (pForceProcessDataPackets) {processDataPackets(true, TIMEOUT);}

    return inBuffer[0];

}//end of Board::getRemoteData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getRemoteAddressedData
//
// Retrieves a data byte from the remote device, using the command specified
// by pCommand and the value pData which can be used as an address or other
// specifier.
//

byte getRemoteAddressedData(byte pCommand, byte pSendData)
{

    if (byteIn == null) {return(0);}

    sendBytes(pCommand, pSendData);

    int IN_BUFFER_SIZE = 2;
    byte[] inBuf;
    inBuf = new byte[IN_BUFFER_SIZE];

    try{
        while(true){

            if (byteIn.available() >= IN_BUFFER_SIZE){

                byteIn.read(inBuf, 0, IN_BUFFER_SIZE);
                break;

            }// if
        }// while...
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 668");
    }

    return inBuf[0];

}//end of Board::getRemoteAddressedData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getRemoteDataBlock
//
// Retrieves a data block from the remote device, using the command specified
// by pCommand, a command qualifier specified by pQualifier, and the block
// size specified by pSize.  The data is returned via pBuffer.
//

void getRemoteDataBlock(byte pCommand, byte pQualifier, int pSize,
                                                                 int[] pBuffer)
{

    return; //debug mks - remove this line - reinsert next block

    /*

    if (byteIn == null) return;

    sendBytes4(pCommand, pQualifier,
              (byte) ((pSize >> 8) & 0xff), (byte) (pSize & 0xff));

    try{
        while(true){

            if (byteIn.available() >= pSize){

                byteIn.read(pktBuffer, 0, pSize);
                break;

                }// if
            }// while...
        }// try
    catch(IOException e){}

    //transfer the bytes to the int array - allow for sign extension
    for (int i=0; i<pSize; i++) pBuffer[i] = (int)pktBuffer[i];

    //use this line to prevent sign extension
    //for (int i=0; i<pSize; i++) pBuffer[i] = ((int)pktBuffer[i]) & 0xff;

    */

}//end of Board::getRemoteDataBlock
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::processDataPackets
//
// The amount of time the function is to wait for a packet is specified by
// pTimeOut.  Each count of pTimeOut equals 10 ms.
//
// See processOneDataPacket notes for more info.
//

public int processDataPackets(boolean pWaitForPkt, int pTimeOut)
{

    int x = 0;

    //process packets until there is no more data available

    // if pWaitForPkt is true, only call once or an infinite loop will occur
    // because the subsequent call will still have the flag set but no data
    // will ever be coming because this same thread which is now blocked is
    // sometimes the one requesting data

    if (pWaitForPkt) {
        return processOneDataPacket(pWaitForPkt, pTimeOut);
    }
    else {
        while ((x = processOneDataPacket(pWaitForPkt, pTimeOut)) != -1){}
    }

    return x;

}//end of Board::processDataPackets
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::processOneDataPacket
//
// This function processes a single data packet if it is available.  If
// pWaitForPkt is true, the function will wait until data is available.
//
// This function should be overridden by sub-classes to provide specialized
// functionality.
//

public int processOneDataPacket(boolean pWaitForPkt, int pTimeOut)
{

    return(0);

}//end of Board::processOneDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of Board::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::waitForConnectCompletion
//
// Waits until the setupComplete flag is true.
//

public synchronized void waitForConnectCompletion()
{

    while(!setupComplete){
        try {wait(); } catch (InterruptedException e) { }
    }

}//end of Board::waitForConnectCompletion
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::setMapAdvanceMode
//
// Sets the mapAdvanceMode which controls what triggers the map trace to
// advance.
//
// Some options are:
//
//  0: no advance at all
//  1: advance with every TDC control code in the DSP data (DSP control)
//  2: advance on command from outside controller object (Encoder control)
//  3: advance on encoder advance control code in the DSP data (DSP control)
//
// Option 4 was never finished as the outside controller object can more
//  accurately monitor the encoder. See tag "setMapAdvanceMode Option 3 removed"
//  in Git history. The code was entirely removed from the Java for simplicity.
//  The code was never added to the Control Board Rabbit. The DSP code can
//  handle the option fine as it simple transfers any change to the tracker
//  variable to the data as a control code.
//
// For Scan mode, option 1 is used as it will cause the map to advance with
// each revolution even if the encoders aren't moving.
//

public void setMapAdvanceMode(int pMode)
{

    mapAdvanceMode = pMode;

}//end of Board::setMapAdvanceMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::setMap2D
//
// Sets pointer to a Map2D object in which the board can send mapping data.
//
// Subclasses should override this method but always call this method.
//

public void setMap2D(Map2D pMap2D)
{

    map2D = pMap2D;
    map2DData = map2D.getDataHandler();

    map2DDataColumn = new int[map2DData.getDataBufferWidth()];

}//end of Board::setMap2D
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::getMap2D
//
// Returns pointer to the map2D object.
//

public Map2D getMap2D()
{

    return(map2D);

}//end of Board::getMap2D
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::triggerMapAdvance
//
// Advances the map if it exists and the current mode allows external control.
//
// This function should be overridden by sub-classes to provide specialized
// functionality.
//

public void triggerMapAdvance()
{


}//end of Board::triggerMapAdvance
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::logStatus
//
// Writes various status and error messages to the log window.
//

public void logStatus(Log pLogWindow)
{

}//end of Board::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::installNewRabbitFirmware
//
// Transmits the Rabbit firmware code to the specified board to replace the
// existing code.
//
// The firmware in the Rabbit is stored in flash memory.  There is a slight
// danger in installing new firmware because the Rabbit may become inoperable\
// if a power glitch occurs during the process -- a reload via serial cable
// would then be required.
//
// Since there is a danger of locking up the Rabbit and the flash memory can be
// written to a finite number of times, the code is not sent each time the
// system starts as is done with the FPGA code.  Rather, it is only updated
// by explicit command.
//
// This function uses TCP/IP and transmits to the single board handled by
// this Board object.  If multiple boards are being loaded simultaneously,
// the load time increases significantly.
//
// This function uses the "binary file" (*.bin) produced by the Dynamic C
// compiler.
//
// The file is transmitted in 1025 byte blocks: one command byte followed by
// 1024 data bytes. The last block is truncated as necessary.
//
// The remote should send the command SEND_DATA when it is ready for each
// block, including the first one.
//

void installNewRabbitFirmware(String pBoardType, String pFilename,
                                                InstallFirmwareSettings pS)
{

    int CODE_BUFFER_SIZE = 1025; //transfer command word and 1024 data bytes
    byte[] codeBuffer;
    codeBuffer = new byte[CODE_BUFFER_SIZE];
    int remoteStatus;
    int timeOutRead;
    int bufPtr;
    int pktCounter = 0;

    boolean fileDone = false;

    FileInputStream inFile = null;

    try {

        sendBytes(pS.loadFirmwareCmd); //send command to initiate loading

        logger.logMessage(pBoardType + " " + ipAddrS +
                                         " loading Rabbit firmware..." + "\n");

        timeOutRead = 0;
        inFile = new FileInputStream(pFilename);
        int c, inCount;

        while(timeOutRead < FIRMWARE_LOAD_TIMEOUT){

            inBuffer[0] = pS.noAction; //clear request byte from host
            //clear status word (upper byte) from host
            inBuffer[1] = pS.noAction;
            //clear status word (lower byte) from host
            inBuffer[2] = pS.noAction;

            remoteStatus = 0;

            //check for a request from the remote if connected
            if (byteIn != null){
                inCount = byteIn.available();
                //0 = buffer offset, 2 = number of bytes to read
                if (inCount >= 3) {byteIn.read(inBuffer, 0, 3);}
                remoteStatus = (int)((inBuffer[0]<<8) & 0xff00)
                                                   + (int)(inBuffer[1] & 0xff);
            }

            //trap and respond to messages from the remote
            if (inBuffer[0] == pS.error){
                logger.logMessage(pBoardType + " " + ipAddrS +
                 " error loading firmware, error code: " + remoteStatus + "\n");
                return;
            }

            //send data packet when requested by remote
            if (inBuffer[0] == pS.sendDataCmd && !fileDone){

                bufPtr = 0; c = 0;
                codeBuffer[bufPtr++] = pS.dataCmd; // command byte = data packet

                //be sure to check bufPtr on left side or a byte will get read
                //and ignored every time bufPtr test fails
                while (bufPtr<CODE_BUFFER_SIZE && (c = inFile.read()) != -1 ) {

                    //stuff the bytes into the buffer after the command byte
                    codeBuffer[bufPtr++] = (byte)c;

                    //reset timer in this loop so it only gets reset when
                    //a request has been received AND not at end of file
                    timeOutRead = 0;

                }//while (bufPtr<CODE_BUFFER_SIZE...

                if (c == -1) {fileDone = true;}

                //send packet to remote -- at the end of the file, this may have
                //random values as padding to fill out the buffer
                byteOut.write(codeBuffer, 0 /*offset*/, CODE_BUFFER_SIZE);

                pktCounter++; //track number of packets sent

                //send the exit command when the file is done
                if (fileDone){
                    codeBuffer[0] = pS.exitCmd;
                    byteOut.write(codeBuffer, 0 /*offset*/, 1);
                    break;
                }//if (fileDone)

            }//if (inBuffer[0] == SEND_DATA)

            //count loops - will exit when max reached
            //this is reset whenever a packet request is received and the end of
            //file not reached - when end of file reached, loop will wait until
            //timeout reached again before exiting in order to catch success/error
            //messages from the remote

            timeOutRead++;

            }// while(timeOutGet <...

        if (fileDone) {
            logger.logMessage(
               pBoardType + " " + ipAddrS + " firmware installed." + "\n");
        }
        else {
            logger.logMessage(pBoardType + " " + ipAddrS +
               " error loading firmware - contact lost after " + pktCounter +
               " packets." + "\n");
        }

    }//try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 947");
        logger.logMessage(
                pBoardType + " " + ipAddrS + " error loading firmware!" + "\n");
    }
    finally {
        if (inFile != null) {try {inFile.close();}catch(IOException e){}}
    }//finally


    //display status message sent back from remote

    String msg = null;
    int c = 0;

    //loop for a bit looking for messages from the remote
    do{
        try{msg = in.readLine();} catch(IOException e){}
        if (msg != null){
            logger.logMessage("UT " + ipAddrS + " says " + msg + "\n");
            msg = null;
        }
    }while(c++ < 50);

}//end of Board::installNewRabbitFirmware
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Board::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of Board::logStackTrace
//-----------------------------------------------------------------------------

}//end of class Board
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
