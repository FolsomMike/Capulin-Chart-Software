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

import java.io.*;
import java.net.*;

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

    int chassisAddr, slotAddr;

    byte status = 0;

    //simulates the default size of a socket created for ethernet access
    // NOTE: If the pipe size is too small, the outside object can fill the
    // buffer and have to wait until the thread on this side catches up.  If
    // the outside object has a timeout, then data will be lost because it will
    // continue on without writing if the timeout occurs.
    // In the future, it would be best if UTBoard object used some flow control
    // to limit overflow in case the default socket size ends up being too
    // small.

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
    //this can be used to provide a unique simulated IP address
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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 112");
    }

    //this end goes to the external object
    inStream = new PipedInputStream(PIPE_SIZE);
    //create an output stream (localOutStream) attached to inStream to read the
    //data sent by the external object
    try{localOutStream = new PipedOutputStream(inStream);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 121");
    }

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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 169");
    }

}//end of Simulator::reSync
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::getChassisSlotAddress
//
// Returns the chassis and slot address for the simulated board.
//

void getChassisSlotAddress()
{

    byte address = (byte)((chassisAddr << 4) & 0xf0);

    address += (byte)(slotAddr & 0x0f);

    sendBytes(address, (byte)0);

}//end of Simulator::getChassisSlotAddress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendBytes
//
// Sends a variable number of bytes (one or more) to the remote device.
//

void sendBytes(byte... pBytes)
{
    System.arraycopy(pBytes, 0, outBuffer, 0, pBytes.length);

    //send packet to remote
    if (byteOut != null) {
        try{
              byteOut.write(outBuffer, 0 /*offset*/, pBytes.length);
              byteOut.flush();
        }
        catch (IOException e) {
            System.err.println(getClass().getName() + " - Error: 220");
        }
    }

}//end of Simulator::sendBytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendShortInt
//
// Sends pShortInt to the host in two bytes, MSB first.
//

void sendShortInt(int pShortInt)
{

    sendBytes((byte)((pShortInt >> 8) & 0xff), (byte)(pShortInt & 0xff));

}//end of Simulator::sendShortInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::sendInteger
//
// Sends pInteger to the host in four bytes, MSB first.
//

void sendInteger(int pInteger)
{

    sendBytes(
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
    if (byteOut != null) {
        try{
            for (int i=0; i<pSize; i++){

                //limit integer to 127 and -128 before converting to byte or the
                //sign can flip

                if (pBuffer[i] > 127) {pBuffer[i] = 127;}
                if (pBuffer[i] < -128) {pBuffer[i] = -128;}

                outBuffer[0] = (byte)pBuffer[i];
                byteOut.write(outBuffer, 0 /*offset*/, 1);
            }

            byteOut.flush();
        }
        catch (IOException e) {
            System.err.println(getClass().getName() + " - Error: 347");
        }
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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 430");
    }

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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 450");
    }

    return (int)((inBuffer[0]<<8) & 0xff00) + (inBuffer[1] & 0xff);

}//end of Simulator::getIntFromSocket
//-----------------------------------------------------------------------------

}//end of class Simulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
