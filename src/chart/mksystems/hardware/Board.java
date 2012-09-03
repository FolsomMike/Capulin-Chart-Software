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

import java.io.*;
import java.net.*;
import javax.swing.*;

import chart.ThreadSafeLogger;
import chart.mksystems.inifile.IniFile;

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
public int chassisAddr, slotAddr;
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
// Board::sendByte
//
// Sends pByte to the remote device, prepending a valid header and appending
// the appropriate checksum.
//

void sendByte(byte pByte)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte;

//calculate the checksum
outBuffer[1] = (byte)( (0x100 - outBuffer[0]) & 0xff  );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 2);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendByte
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes2
//
// Sends two bytes to the remote device, prepending a valid header and appending
// the appropriate checksum.
//

void sendBytes2(byte pByte1, byte pByte2)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;

//calculate the checksum
outBuffer[2] = (byte)( (0x100 - outBuffer[0] - outBuffer[1]) & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 3);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes3
//
// Sends three bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes3(byte pByte1, byte pByte2, byte pByte3)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2; outBuffer[2] = pByte3;

//calculate the checksum
outBuffer[3] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 4);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes3
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes4
//
// Sends four bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes4(byte pByte1, byte pByte2, byte pByte3, byte pByte4)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;

//calculate the checksum
outBuffer[4] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 5);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes4
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes5
//
// Sends five bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes5(byte pByte1, byte pByte2, byte pByte3, byte pByte4, byte pByte5)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5;

//calculate the checksum
outBuffer[5] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 6);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes5
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes6
//
// Sends six bytes to the remote device, prepending a valid header and appending
// the appropriate checksum.
//

void sendBytes6(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                                                    byte pByte5, byte pByte6)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;

//calculate the checksum
outBuffer[6] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 7);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes6
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes7
//
// Sends seven bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes7(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                     byte pByte5, byte pByte6, byte pByte7)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7;

//calculate the checksum
outBuffer[7] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 8);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes7
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes8
//
// Sends eight bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes8(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                     byte pByte5, byte pByte6, byte pByte7, byte pByte8)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7; outBuffer[7] = pByte8;

//calculate the checksum
outBuffer[8] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6] - outBuffer[7])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 9);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes8
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes9
//
// Sends nine bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes9(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
               byte pByte5, byte pByte6, byte pByte7, byte pByte8, byte pByte9)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7; outBuffer[7] = pByte8;
outBuffer[8] = pByte9;

//calculate the checksum
outBuffer[9] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6] - outBuffer[7]
         - outBuffer[8])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 10);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes9
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes10
//
// Sends ten bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes10(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
  byte pByte5, byte pByte6, byte pByte7, byte pByte8, byte pByte9, byte pByte10)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7; outBuffer[7] = pByte8;
outBuffer[8] = pByte9; outBuffer[9] = pByte10;

//calculate the checksum
outBuffer[10] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6] - outBuffer[7]
         - outBuffer[8] - outBuffer[9])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 11);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes10
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes11
//
// Sends eleven bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes11(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                 byte pByte5, byte pByte6, byte pByte7, byte pByte8,
                 byte pByte9, byte pByte10, byte pByte11)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7; outBuffer[7] = pByte8;
outBuffer[8] = pByte9; outBuffer[9] = pByte10;
outBuffer[10] = pByte11;

//calculate the checksum
outBuffer[11] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6] - outBuffer[7]
         - outBuffer[8] - outBuffer[9] - outBuffer[10])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 12);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes11
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendBytes15
//
// Sends fifteen bytes to the remote device, prepending a valid header and
// appending the appropriate checksum.
//

void sendBytes15(byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                 byte pByte5, byte pByte6, byte pByte7, byte pByte8,
                 byte pByte9, byte pByte10, byte pByte11, byte pByte12,
                 byte pByte13, byte pByte14, byte pByte15)
{

sendHeader(); //send the packet header

outBuffer[0] = pByte1; outBuffer[1] = pByte2;
outBuffer[2] = pByte3; outBuffer[3] = pByte4;
outBuffer[4] = pByte5; outBuffer[5] = pByte6;
outBuffer[6] = pByte7; outBuffer[7] = pByte8;
outBuffer[8] = pByte9; outBuffer[9] = pByte10;
outBuffer[10] = pByte11; outBuffer[11] = pByte12;
outBuffer[12] = pByte13; outBuffer[13] = pByte14;
outBuffer[14] = pByte15;

//calculate the checksum
outBuffer[15] = (byte)(
        (0x100 - outBuffer[0] - outBuffer[1] - outBuffer[2] - outBuffer[3]
        - outBuffer[4] - outBuffer[5] - outBuffer[6] - outBuffer[7]
         - outBuffer[8] - outBuffer[9] - outBuffer[10] - outBuffer[11] -
         outBuffer[12] - outBuffer[13] - outBuffer[14])
         & 0xff );

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 16);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of Board::sendBytes15
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
if (byteOut != null)
    try{byteOut.write(outBuffer, 0 /*offset*/, 4);}
    catch (IOException e) {}

}//end of Board::sendHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::process2BytePacket
//
// Retrieves two data bytes from the packet and stores them in inBuffer.
//
// Returns number of bytes retrieved from the socket.
//

public int process2BytePacket()
{

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 2) break;
        waitSleep(10);
        }
    if (byteIn.available() >= 2) return byteIn.read(inBuffer, 0, 2);
    }// try
catch(IOException e){}

return 0;

}//end of Board::process2BytePacket
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

if (byteIn == null) return(0);

sendByte(pCommand); //request the data from the remote

//force waiting for and processing of receive packets
if (pForceProcessDataPackets) processDataPackets(true, TIMEOUT);

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

if (byteIn == null) return(0);

sendBytes2(pCommand, pSendData);

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
catch(IOException e){}

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

if (pWaitForPkt)
    return processOneDataPacket(pWaitForPkt, pTimeOut);
else
    while ((x = processOneDataPacket(pWaitForPkt, pTimeOut)) != -1){}

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
// Board::logStatus
//
// Writes various status messages to the log window.
//

public void logStatus(JTextArea pTextArea)
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

boolean fileDone = false;

FileInputStream inFile = null;

try {

    sendByte(pS.loadFirmwareCmd); //send command to initiate loading

    logger.logMessage(pBoardType + " " + ipAddrS +
                                        " loading Rabbit firmware..." + "\n");

    timeOutRead = 0;
    inFile = new FileInputStream(pFilename);
    int c, inCount;

    while(timeOutRead < FIRMWARE_LOAD_TIMEOUT){

        inBuffer[0] = pS.noAction; //clear request byte from host
        inBuffer[1] = pS.noAction; //clear status word (upper byte) from host
        inBuffer[2] = pS.noAction; //clear status word (lower byte) from host

        remoteStatus = 0;

        //check for a request from the remote if connected
        if (byteIn != null){
            inCount = byteIn.available();
            //0 = buffer offset, 2 = number of bytes to read
            if (inCount >= 3)
                byteIn.read(inBuffer, 0, 3);
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
            while (bufPtr < CODE_BUFFER_SIZE && (c = inFile.read()) != -1 ) {

                //stuff the bytes into the buffer after the command byte
                codeBuffer[bufPtr++] = (byte)c;

                //reset timer in this loop so it only gets reset when
                //a request has been received AND not at end of file
                timeOutRead = 0;

                }

            if (c == -1) fileDone = true;

            //send packet to remote -- at the end of the file, this may have
            //random values as padding to fill out the buffer
            byteOut.write(codeBuffer, 0 /*offset*/, CODE_BUFFER_SIZE);

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

    if (fileDone)
        logger.logMessage(
           pBoardType + " " + ipAddrS + " firmware installed." + "\n");
    else
    //remote has not responded if this part reached
        logger.logMessage(pBoardType + " " + ipAddrS
                        + " error loading firmware - contact lost." + "\n");

    }//try
catch(IOException e){
    logger.logMessage(
            pBoardType + " " + ipAddrS + " error loading firmware!" + "\n");
}
finally {
    if (inFile != null) try {inFile.close();}catch(IOException e){}
    }//finally

}//end of Board::installNewRabbitFirmware
//-----------------------------------------------------------------------------

}//end of class Board
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
