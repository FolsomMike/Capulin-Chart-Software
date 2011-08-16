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


boolean onPipeFlag = false;
boolean delayingToOnPipe = true;
boolean delayingToInspect = true;
boolean inspectFlag = false;
boolean inspectMode = false;
int encoder1 = 0, encoder2 = 0;
int encoder1DeltaTrigger, encoder2DeltaTrigger;
int inspectPacketCount = 0;

byte portA = 0, portE = 0;

int delayToOnPipe = 1000;
int delayToInspect = 1000;
int lengthTrack;

public static int DELAY_BETWEEN_INSPECT_PACKETS = 10;
int delayBetweenPackets = DELAY_BETWEEN_INSPECT_PACKETS;

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

//simulate the inspection signals and send back packets to the host
if (inspectMode == true) simulateInspection();

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

    byte pktID = inBuffer[0];

    if (pktID == ControlBoard.GET_STATUS_CMD) getStatus();
    else
    if (pktID == ControlBoard.START_INSPECT_CMD) startInspect(pktID);
    else
    if (pktID == ControlBoard.STOP_INSPECT_CMD) stopInspect(pktID);

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
sendPacketHeader(ControlBoard.GET_STATUS_CMD);

sendBytes2(status, (byte)0);

}//end of ControlSimulator::getStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::startInspect
//
// Starts the inspect mode -- simulated encoder and inspection control flag
// packets will be sent to the host.
//

int startInspect(byte pPktID)
{

int bytesRead = 0;

try{
    bytesRead = byteIn.read(inBuffer, 0, 2);
    }
catch(IOException e){}

if (bytesRead == 2){

	//calculate checksum to check validity of the packet
	if ( (pPktID + inBuffer[0] + inBuffer[1] & (byte)0xff) != 0) return(-1);
	}
else{
	//("Error - Wrong sized packet header for startInspect!\n");
	return(-1);
	}

resetAll();

inspectMode = true;

return(bytesRead);

}//end of ControlSimulator::startInpsect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::resetAll
//
// Resets all variables in preparation to simulate a new piece.
//

void resetAll()
{

onPipeFlag = false;
delayingToOnPipe = true;
delayingToInspect = false;
inspectFlag = false;
encoder1 = 0; encoder2 = 0;
inspectPacketCount = 0;

delayToOnPipe = 10;
delayBetweenPackets = DELAY_BETWEEN_INSPECT_PACKETS;

}//end of ControlSimulator::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::stopInspect
//
// Stops the inspect mode.
//

int stopInspect(byte pPktID)
{

int bytesRead = 0;

try{
    bytesRead = byteIn.read(inBuffer, 0, 2);
    }
catch(IOException e){}

if (bytesRead == 2){

	//calculate checksum to check validity of the packet
	if ( (pPktID + inBuffer[0] + inBuffer[1] & (byte)0xff) != 0) return(-1);
	}
else{
	//("Error - Wrong sized packet header for startInspect!\n");
	return(-1);
	}

inspectMode = false;

return(bytesRead);

}//end of ControlSimulator::stopInspect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::simulateInspection
//
// Simulates signals expected by the host in inspect mode.
//

void simulateInspection()
{

//delay between sending inspect packets to the host
if (delayBetweenPackets-- != 0) return;
//restart time for next packet send
delayBetweenPackets = DELAY_BETWEEN_INSPECT_PACKETS;

inspectPacketCount++; //count packets sent to host

//after timeout, simulate head reaching beginning of tube by setting flag
if (delayingToOnPipe){
    if (delayToOnPipe != 0){delayToOnPipe--;}
    else {
        onPipeFlag = true;
        delayingToOnPipe = false;
        delayingToInspect = true;
        delayToInspect = 10;
        lengthTrack = 300;
        }
    }

//after timeout, simulate head reaching drop down point of tube by setting flag
//this is when inspection actually begins
if (delayingToInspect){
    if (delayToInspect != 0){delayToInspect--;}
    else {
        inspectFlag = true;
        delayingToInspect = false;
        }
    }

//if on pipe, use counter to trigger a simulated end of the piece
if (onPipeFlag){
    if (lengthTrack != 0){lengthTrack--;}
    else {
        resetAll();
        } 
    }


//start with all port inputs set to 1
portA = (byte)0xff; portE = (byte)0xff;

//set appropriate bit low for each flag which is active low
if (onPipeFlag)
    portA = (byte)(portA & ~ControlBoard.ON_PIPE_MASK);
if (inspectFlag) portA = (byte)(portA & ~ControlBoard.INSPECT_MASK);

//move the encoders the amount expected by the host
encoder1 += encoder1DeltaTrigger;
encoder2 += encoder2DeltaTrigger;

int pktSize = 12;
int x = 0;

sendPacketHeader(ControlBoard.GET_INSPECT_PACKET_CMD);

//send the packet count back to the host, MSB followed by LSB
outBuffer[x++] = (byte)((inspectPacketCount >> 8) & 0xff);
outBuffer[x++] = (byte)(inspectPacketCount++ & 0xff);

//place the encoder 1 values into the buffer by byte, MSB first
outBuffer[x++] = (byte)((encoder1 >> 24) & 0xff);
outBuffer[x++] = (byte)((encoder1 >> 16) & 0xff);
outBuffer[x++] = (byte)((encoder1 >> 8) & 0xff);
outBuffer[x++] = (byte)(encoder1 & 0xff);

//place the encoder 2 values into the buffer by byte, MSB first
//place the encoder 1 values into the buffer by byte, MSB first
outBuffer[x++] = (byte)((encoder2 >> 24) & 0xff);
outBuffer[x++] = (byte)((encoder2 >> 16) & 0xff);
outBuffer[x++] = (byte)((encoder2 >> 8) & 0xff);
outBuffer[x++] = (byte)(encoder2 & 0xff);


outBuffer[x++] = portA;
outBuffer[x++] = portE;

//send packet to the host
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 12);
        }
    catch (IOException e) {
        System.out.println(
                        "Control Board simulateInspection: " + e.getMessage());
        }

}//end of ControlSimulator::simulateInspection
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// ControlSimulator::sendPacketHeader
//
// Sends via the socket: 0xaa, 0x55, 0xaa, 0x55, packet identifier.
//
// Does not flush.
//

void sendPacketHeader(byte pPacketID)
{

outBuffer[0] = (byte)0xaa; outBuffer[1] = (byte)0x55;
outBuffer[2] = (byte)0xbb; outBuffer[3] = (byte)0x66;
outBuffer[4] = (byte)pPacketID;

//send packet to remote
if (byteOut != null)
    try{
        byteOut.write(outBuffer, 0 /*offset*/, 5);
        }
    catch (IOException e) {
        System.out.println("Control Board sendPacketHeader" + e.getMessage());
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
