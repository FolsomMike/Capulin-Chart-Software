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

import chart.mksystems.inifile.IniFile;

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

boolean setupComplete = false; //set true if set was completed
boolean ready = false; //set true if board is successfully setup

boolean simulate;

public InetAddress ipAddr;
String ipAddrS;
public int chassisAddr, slotAddr;
String chassisSlotAddr;

Socket socket = null;
PrintWriter out = null;
BufferedReader in = null;
byte[] inBuffer;
byte[] outBuffer;
DataOutputStream byteOut = null;
DataInputStream byteIn = null;

String[] threadSafeMessage; //stores messages to be displayed by main thread
int threadSafeMessagePtr = 0; //points next message in the array for saving
int mainThreadMessagePtr = 0; //points next message in the array to be displayed
static int NUMBER_THREADSAFE_MESSAGES = 100;

int TIMEOUT = 50;

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
byte[] inBuffer; 
inBuffer = new byte[IN_BUFFER_SIZE];

try{
    while(true){

        if (byteIn.available() >= IN_BUFFER_SIZE){

            byteIn.read(inBuffer, 0, IN_BUFFER_SIZE);
            break;
            
            }// if
        }// while...
    }// try
catch(IOException e){}

return inBuffer[0];

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
// UTBoard::processDataPackets
//
// This function should be called often to allow processing of data packets
// received from the remotes and stored in the socket buffer.
//
// The amount of time the function is to wait for a packet is specified by
// pTimeOut.  Each count of pTimeOut equals 10 ms.
//
// If pWaitForPkt is true, the function will wait until data is available.
//
// Returns number of bytes retrieved from the socket, not including the
// 4 header bytes, the packet ID, the DSP chip ID, and the DSP core ID.
//

public int processDataPackets(boolean pWaitForPkt, int pTimeOut)
{

return 0;

}//end of Board::processDataPackets
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
// Board::threadSafeLog
//
// This function allows a thread to add a log entry to the log window.  The
// actual call is passed to the invokeLater function so it will be safely
// executed by the main Java thread.
// 
// Messages are stored in a circular buffer so that the calling thead does
// not overwrite the previous message before the main thread can process it.
//

public void threadSafeLog(String pMessage)
{

threadSafeMessage[threadSafeMessagePtr++] = pMessage;
if (threadSafeMessagePtr == NUMBER_THREADSAFE_MESSAGES)
    threadSafeMessagePtr = 0;

 //store the message where the helper can find it

//Schedule a job for the event-dispatching thread: 
//creating and showing this application's GUI. 
    
javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() { threadSafeLogHelper(); } }); 

}//end of  Board::threadSafeLog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::threadSafeLogHelper
//
// This function is passed to invokeLater by threadSafeLog so that it will be
// run by the main Java thread and display the stored message on the log
// window.
// 
//

public void threadSafeLogHelper()
{

// Since this function will be invoked once for every message placed in the
// array, no need to check if there is a message available?  Would be a problem
// if the calling thread began to overwrite the buffer before it coulde be
// displayed?

//display the next message stored in the array
log.append(threadSafeMessage[mainThreadMessagePtr++]);

if (mainThreadMessagePtr == NUMBER_THREADSAFE_MESSAGES)
    mainThreadMessagePtr = 0;

}//end of  Board::threadSafeLogHelper
//-----------------------------------------------------------------------------

}//end of class Board
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------    
