/******************************************************************************
* Title: Simulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This is the super class for various simulator classes which simulate a TCP/IP
* connection between the host and various types of hardware.
*
* This is a subclass of Socket and can be substituted for an instance
* of that class when simulated data is needed.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import java.net.*;
import java.io.*;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Simulator
//

public class Simulator extends Socket{

public Simulator() throws SocketException{}; //default constructor - not used

public InetAddress ipAddr;
int port;

int index;

boolean reSynced;
int reSyncCount = 0;

public static int instanceCounter = 0;

byte chassisAddr, boardAddr;

byte status = 0;

//simulates the default size of a socket created for ethernet access
// NOTE: If the pipe size is too small, the outside object can fill the buffer
// and have to wait until the thread on this side catches up.  If the outside
// object has a timeout, then data will be lost because it will continue on
// without writing if the timeout occurs.
// In the future, it would be best if UTBoard object used some flow control
// to limit overflow in case the default socket size ends up being too small.

static int PIPE_SIZE = 8192;

PipedOutputStream outStream;
PipedInputStream  localInStream;

PipedInputStream  inStream;
PipedOutputStream localOutStream;

DataOutputStream byteOut = null;
DataInputStream byteIn = null;

int IN_BUFFER_SIZE = 512;
byte[] inBuffer;

int OUT_BUFFER_SIZE = 512;
byte[] outBuffer;

//-----------------------------------------------------------------------------
// Simulator::Simulator (constructor)
//

public Simulator(InetAddress pIPAddress, int pPort) throws SocketException
{

port = pPort; ipAddr = pIPAddress;

//give each instance of the class a unique number
index = instanceCounter++;


//create an input and output stream to simulate those attached to a real
//Socket connected to a hardware board

// four steams are used - two connected pairs
// an ouptut and an input stream are created to hand to the outside object
// (outStream & inStream) - the outside object writes to outStream and reads
// from inStream
// an input stream is then created using the outStream as it's connection -
// this object reads from that input stream to receive bytes sent by the
// external object via the attached outStream
// an output stream is then created using the inStream as it's connection -
// this object writes to that output stream to send bytes to be read by the
// external object via the attached inStream

//this end goes to the external object
outStream = new PipedOutputStream();
//create an input stream (localInStream) attached to outStream to read the
//data sent by the external object
try{localInStream = new PipedInputStream(outStream, PIPE_SIZE);}
catch(IOException e){}

//this end goes to the external object
inStream = new PipedInputStream(PIPE_SIZE);
//create an output stream (localOutStream) attached to inStream to read the
//data sent by the external object
try{localOutStream = new PipedOutputStream(inStream);}
catch(IOException e){}

inBuffer = new byte[IN_BUFFER_SIZE]; //used by various functions
outBuffer = new byte[OUT_BUFFER_SIZE]; //used by various functions

//create an output and input byte stream
//out for this class is in for the outside classes and vice versa

byteOut = new DataOutputStream(localOutStream);
byteIn = new DataInputStream(localInStream);

}//end of Simulator::Simulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::reSync
//
// Clears bytes from the socket buffer until 0xaa byte reached which signals
// the *possible* start of a new valid packet header or until the buffer is
// empty.
//
// If an 0xaa byte is found, the flag reSynced is set true to that other
// functions will know that an 0xaa byte has already been removed from the
// stream, signalling the possible start of a new packet header.
//
// There is a special case where a 0xaa is found just before the valid 0xaa
// which starts a new packet - the first 0xaa is the last byte of the previous
// packet (usually the checksum).  In this case, the next packet will be lost
// as well.  This should happen rarely.
//

public void reSync()
{

reSynced = false;

//track the number of time this function is called, even if a resync is not
//successful - this will track the number of sync errors
reSyncCount++;

try{
    while (byteIn.available() > 0) {
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] == (byte)0xaa) {reSynced = true; break;}
        }
    }
catch(IOException e){}

}//end of Simulator::reSync
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getAddress
//
// Returns the chassis and board address.
//

void getAddress()
{

byte address = (byte)((chassisAddr << 4) & 0xf0);

address += (byte)(boardAddr & 0x0f);

sendBytes2(address, (byte)0);

}//end of Simulator::getAddress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendByte
//
// Sends pByte back to the host.
//

void sendByte(byte pByte)
{

outBuffer[0] = pByte;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 1);
        byteOut.flush();
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of Simulator::sendByte
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendBytes2
//
// Sends two bytes to the host.
//

void sendBytes2(byte pByte1, byte pByte2)
{

outBuffer[0] = pByte1; outBuffer[1] = pByte2;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 2);
        byteOut.flush();
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of Simulator::sendBytes2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendBytes3
//
// Sends three bytes to the host.
//

void sendBytes3(byte pByte1, byte pByte2, byte pByte3)
{

outBuffer[0] = pByte1; outBuffer[1] = pByte2;  outBuffer[2] = pByte3;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 3);
        byteOut.flush();
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of Simulator::sendBytes3
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendBytes4
//
// Sends four bytes to the host.
//

void sendBytes4(byte pByte1, byte pByte2, byte pByte3, byte pByte4)
{

outBuffer[0] = pByte1; outBuffer[1] = pByte2;  outBuffer[2] = pByte3;
outBuffer[3] = pByte4;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 4);
        byteOut.flush();
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of Simulator::sendBytes4
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendShortInt
//
// Sends pShortInt to the host in two bytes, MSB first.
//

void sendShortInt(int pShortInt)
{

sendBytes2((byte)((pShortInt >> 8) & 0xff), (byte)(pShortInt & 0xff));

}//end of Simulator::sendShortInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendInteger
//
// Sends pInteger to the host in four bytes, MSB first.
//

void sendInteger(int pInteger)
{

sendBytes4(
        (byte)((pInteger >> 24) & 0xff), (byte)(pInteger >> 16 & 0xff),
        (byte)((pInteger >> 8) & 0xff), (byte)(pInteger & 0xff));

}//end of Simulator::sendInteger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendDataBlock
//
// Sends pSize number of data points in pBuffer back to the host.
//

void sendDataBlock(int pSize, int[] pBuffer)
{

//send packet to remote
if (byteOut != null)
    try{
        for (int i=0; i<pSize; i++){

            //limit integer to 127 and -128 before converting to byte or the
            //sign can flip

            if (pBuffer[i] > 127) pBuffer[i] = 127;
            if (pBuffer[i] < -128) pBuffer[i] = -128;

            outBuffer[0] = (byte)pBuffer[i];
            byteOut.write(outBuffer, 0 /*offset*/, 1);
            }

        byteOut.flush();
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of Simulator::sendDataBlock
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getInputStream()
//
// Returns an input stream for the calling object - it is an input to that
// object.
//

@Override
public InputStream getInputStream()
{

return (inStream);

}//end of Simulator::getInputStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getOutputStream()
//
// Returns an output stream for the calling object - it is an output from that
// object.
//

@Override
public OutputStream getOutputStream()
{

return (outStream);

}//end of Simulator::getOutputStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getReceiveBufferSize
//
// Returns the buffer size for the pipe(s) being used to simulate a socket
// connection.  The size of the input stream defines the size for both itself
// and the attached output stream.
//

@Override
public int getReceiveBufferSize()
{

return (PIPE_SIZE);

}//end of Simulator::getReceiveBufferSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getSendBufferSize
//
// Returns the buffer size for the pipe(s) being used to simulate a socket
// connection.  The size of the input stream defines the size for both itself
// and the attached output stream.
//

@Override
public int getSendBufferSize()
{

return (PIPE_SIZE);

}//end of Simulator::getSendBufferSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getByteFromSocket()
//
// Reads one byte from the socket and returns it as an integer.
//

public int getByteFromSocket()
{

try{byteIn.read(inBuffer, 0, 1);}
catch(IOException e){}

return (int)(inBuffer[0] & 0xff);

}//end of Simulator::getByteFromSocket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getIntFromSocket()
//
// Reads two bytes from the socket and converts them to an integer.  The format
// expected from the socket is MSB/LSB unsigned integer.
//

public int getIntFromSocket()
{

try{byteIn.read(inBuffer, 0, 2);}
catch(IOException e){}

return (int)((inBuffer[0]<<8) & 0xff00) + (inBuffer[1] & 0xff);

}//end of Simulator::getIntFromSocket
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
// Simulator::finalize
//
// This function is inherited from the object class and is called by the Java
// VM before the object is discarded.
//

@Override
protected void finalize() throws Throwable
{

//close everything - the order of closing may be important

localInStream.close();
outStream.close();

inStream.close();
localOutStream.close();

//allow the parent classes to finalize
super.finalize();

}//end of Simulator::finalize
//-----------------------------------------------------------------------------

}//end of class Simulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

