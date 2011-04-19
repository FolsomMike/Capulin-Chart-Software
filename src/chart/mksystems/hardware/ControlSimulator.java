/******************************************************************************
* Title: ControlSimulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class simulates a TCP/IP connection between the host and UT boards.
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

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlSimulator
//
// This class simulates data from a TCP/IP connection between the host computer
// and Control boards.
//

public class ControlSimulator extends Simulator{

public ControlSimulator() throws SocketException{}; //default constructor - not used

//simulates the default size of a socket created for ethernet access
// NOTE: If the pipe size is too small, the outside object can fill the buffer
// and have to wait until the thread on this side catches up.  If the outside
// object has a timeout, then data will be lost because it will continue on
// without writing if the timeout occurs.
// In the future, it would be best if ControlBoard object used some flow control
// to limit overflow in case the default socket size ends up being too small.

public static int controlBoardCounter = 0;
int controlBoardNumber;

//-----------------------------------------------------------------------------
// ControlSimulator::ControlSimulator (constructor)
//

public ControlSimulator(InetAddress pIPAddress, int pPort)
                                                        throws SocketException
{

//call the parent class constructor
super(pIPAddress, pPort);

//give each Control board a unique number so it can load data from the
//simulation.ini file and such
//this is different than the unique index provided in the parent class Simulator
//as that number is distributed across all sub classes -- UT boards,
//Control boards, etc.
controlBoardNumber = controlBoardCounter++;

//load configuration data from file
configure();

//create an out writer from this class - will be input for some other class
//this writer is only used to send the greeting back to the host

PrintWriter out = new PrintWriter(localOutStream, true);
out.println("Hello from Control Board!");

}//end of ControlSimulator::ControlSimulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::processDataPackets
//
// See processDataPacketsHelper notes for more info.
//

public int processDataPackets(boolean pWaitForPkt)
{

int x = 0;

//process packets until there is no more data available

// if pWaitForPkt is true, only call once or an infinite loop will occur
// because the subsequent call will still have the flag set but no data
// will ever be coming because this same thread which is now blocked is
// sometimes the one requesting data

if (pWaitForPkt)
    return processDataPacketsHelper(pWaitForPkt);
else
    while ((x = processDataPacketsHelper(pWaitForPkt)) != -1){}

return x;

}//end of ControlSimulator::processDataPackets
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::processDataPacketsHelper
//
// Drive the simulation functions.  This function is usually called from a
// thread.
//

public int processDataPacketsHelper(boolean pWaitForPkt)
{

if (byteIn == null) return 0;  //do nothing if the port is closed

try{

    int x;

    //wait until 5 bytes are available - this should be the 4 header bytes, and
    //the packet identifier/command
    if ((x = byteIn.available()) < 5) return -1;

    //read the bytes in one at a time so that if an invalid byte is encountered
    //it won't corrupt the next valid sequence in the case where it occurs
    //within 3 bytes of the invalid byte

    //check each byte to see if the first four create a valid header
    //if not, jump to resync which deletes bytes until a valid first header
    //byte is reached

    //if the reSynced flag is true, the buffer has been resynced and an 0xaa
    //byte has already been read from the buffer so it shouldn't be read again

    //after a resync, the function exits without processing any packets

    if (!reSynced){
        //look for the 0xaa byte unless buffer just resynced
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0xaa) {reSync(); return 0;}
        }
    else reSynced = false;

    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0x55) {reSync(); return 0;}
    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0xbb) {reSync(); return 0;}
    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0x66) {reSync(); return 0;}

    //read the packet ID
    byteIn.read(inBuffer, 0, 1);

    if (inBuffer[0] == UTBoard.GET_STATUS_CMD) getStatus();
//    else
//    if (inBuffer[0] == UTBoard.LOAD_FPGA_CMD) loadFPGA();
//    else
    
    return 0;

    }//try
catch(IOException e){}

return 0;

}//end of ControlSimulator::processDataPacketsHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::getStatus
//
// Simulates returning of the status byte.
//

void getStatus()
{

//send standard packet header
sendPacketHeader(UTBoard.GET_STATUS_CMD, (byte)0, (byte)0);

sendBytes2(status, (byte)0);

}//end of ControlSimulator::getStatus
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// ControlSimulator::sendPacketHeader
//
// Sends via the socket: 0xaa, 0x55, 0xaa, 0x55, packet identifier, DSP chip,
// and DSP core.
//
// Does not flush.
//

void sendPacketHeader(byte pPacketID, byte pDSPChip, byte pDSPCore)
{

outBuffer[0] = (byte)0xaa; outBuffer[1] = (byte)0x55;
outBuffer[2] = (byte)0xbb; outBuffer[3] = (byte)0x66;
outBuffer[4] = (byte)pPacketID; outBuffer[5] = pDSPChip;
outBuffer[6] = pDSPCore;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 7);
        }
    catch (IOException e) {
        System.out.println(e.getMessage());
        }

}//end of ControlSimulator::sendPacketHeader
//----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//
// Each instance must open its own iniFile object because they are created
// simultaneously in different threads.  The iniFile object is not guaranteed
// to be thread safe.
//

private void configure()
{

IniFile configFile;

//if the ini file cannot be opened and loaded, exit without action
try {configFile = new IniFile("Simulation.ini");}
    catch(IOException e){
    return;
    }

String section = "Simulated Control Board " + (controlBoardNumber + 1);

chassisAddr = (byte)configFile.readInt(section, "Chassis Number", 0);

chassisAddr = (byte)(~chassisAddr); //the switches invert the value

boardAddr = (byte)configFile.readInt(section, "Slot Number", 0);

boardAddr = (byte)(~boardAddr); //the switches invert the value

}//end of ControlSimulator::configure
//-----------------------------------------------------------------------------

}//end of class ControlSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
