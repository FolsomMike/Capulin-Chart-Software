/******************************************************************************
* Title: ControlSimulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class simulates a TCP/IP connection between the host and Control boards.
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
// class ControlSimulator
//
// This class simulates data from a TCP/IP connection between the host computer
// and UT boards.
//

public class ControlSimulator extends Socket{

public ControlSimulator() throws SocketException{}; //default constr - not used

public InetAddress ipAddr;
int port;

byte status = FPGA_LOADED_FLAG;

PipedInputStream  outStreamRead;
PipedOutputStream outStream;

PipedInputStream  inStream;
PipedOutputStream inStreamWrite;

DataOutputStream byteOut = null;
DataInputStream byteIn = null;

int IN_BUFFER_SIZE = 512;
byte[] inBuffer; 

//Commands for UT boards
//These should match the values in the code for those boards.

static byte NO_ACTION = 0;
static byte MONITOR_CMD = 1;
static byte ZERO_ENCODERS_CMD = 2;
static byte REFRESH_CMD = 3;
static byte LOAD_FPGA_CMD = 4;
static byte SEND_DATA_CMD = 5;
static byte DATA_CMD = 6;
static byte WRITE_FPGA_CMD = 7;
static byte READ_FPGA_CMD = 8;
static byte GET_STATUS_CMD = 9;
static byte SET_HDW_GAIN_CMD = 10;
static byte WRITE_DSP_CMD = 11;
static byte WRITE_NEXT_DSP_CMD = 12;
static byte READ_DSP_CMD = 13;
static byte READ_NEXT_DSP_CMD = 14;
static byte DEBUG_CMD = 126;
static byte EXIT_CMD = 127;

//FPGA Register Addresses for the UT Board

static byte CHASSIS_BOARD_ADDRESS = 0x08;

//Status Codes for UT boards
//These should match the values in the code for those boards.

static byte NO_STATUS = 0;
static byte FPGA_INITB_ERROR = 1;
static byte FPGA_DONE_ERROR = 2;
static byte FPGA_CONFIG_CRC_ERROR = 3;
static byte FPGA_CONFIG_GOOD = 4;

// UT Board status flag bit masks

static byte FPGA_LOADED_FLAG = 0x01;


//-----------------------------------------------------------------------------
// ControlSimulator::ControlSimulator (constructor)
//
  
public ControlSimulator(InetAddress pIPAddress, int pPort)
                                                         throws SocketException
{

port = pPort; ipAddr = pIPAddress;

//create an input and output stream to simulate those attached to a real
//Socket connected to a UT board

//each stream has a mate - this class writes to inStreamWrite to send data
//via inStream and reads from outStreamRead to receive data from outStream

outStream = new PipedOutputStream();
try{outStreamRead = new PipedInputStream(outStream);}
catch(IOException e){}

inStream = new PipedInputStream();
try{inStreamWrite = new PipedOutputStream(inStream);}
catch(IOException e){}

inBuffer = new byte[IN_BUFFER_SIZE]; //used by various functions

//create an output and input byte stream
//out for this class is in for the outside classes and vice versa

byteOut = new DataOutputStream(inStreamWrite);
byteIn = new DataInputStream(outStreamRead);

//create an out writer from this class - will be input for some other class
//this writer is only used to send the greeting back to the host

PrintWriter out = new PrintWriter(inStreamWrite, true);
out.println("Hello from Control Board!");

}//end of ControlSimulator::ControlSimulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::driveSimulation
//
// Drive the simulation functions.  This function is usually called from a
// thread.
//

public void driveSimulation()
{

int inCount;

try{
    inCount = byteIn.available();

    //if data is available, read the command byte

    //0 = buffer offset, 1 = number of bytes to read
    if (inCount >= 1) byteIn.read(inBuffer, 0, 1);
    else return;

    if (inBuffer[0] == MONITOR_CMD) startMonitor();

    if (inBuffer[0] == EXIT_CMD) stopMonitor();
   
    }//try
catch(IOException e){}

}//end of UTSimulator::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::sendByte
//
// Sends pByte back to the host.
//

void sendByte(byte pByte)
{

byte[] buffer = new byte[1];

buffer[0] = pByte;

//send packet to remote
if (byteOut != null) 
    try{
        byteOut.write(buffer, 0 /*offset*/, 1);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of ControlSimulator::sendByte
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::sendBytes2
//
// Sends two bytes to the host.
//

void sendBytes2(byte pByte1, byte pByte2)
{

byte[] buffer = new byte[2];

buffer[0] = pByte1; buffer[1] = pByte2;

//send packet to remote
if (byteOut != null) 
    try{
        byteOut.write(buffer, 0 /*offset*/, 2);
        byteOut.flush();
        }
    catch (IOException e) {}

}//end of ControlSimulator::sendBytes2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::startMonitor
//
// Starts the monitor function.
//
  
void startMonitor()
{

try{byteIn.read(inBuffer, 0, 2);}
catch(IOException e){}

}//end of ControlSimulator::startMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::stopMonitor
//
// Stops the monitor function.
//
  
void stopMonitor()
{

try{byteIn.read(inBuffer, 0, 1);}
catch(IOException e){}

if (inBuffer[0] == CHASSIS_BOARD_ADDRESS) sendBytes2((byte)0xee, (byte)0);

}//end of ControlSimulator::stopMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::getInputStream()
//
  
@Override
public InputStream getInputStream()
{

return (inStream);

}//end of ControlSimulator::getInputStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::getOutputStream()
//
  
@Override
public OutputStream getOutputStream()
{

return (outStream);

}//end of ControlSimulator::getOutputStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::finalize
//
// This function is inherited from the object class and is called by the Java
// VM before the object is discarded.
//

@Override
protected void finalize() throws Throwable
{

//close everything - the order of closing may be important

outStreamRead.close();
outStream.close();

inStream.close();
inStreamWrite.close();

//allow the parent classes to finalize
super.finalize();

}//end of ControlSimulator::finalize
//-----------------------------------------------------------------------------

}//end of class ControlSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
