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

import chart.MessageLink;
import chart.mksystems.inifile.IniFile;
import java.io.*;
import java.net.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlSimulator
//
// This class simulates data from a TCP/IP connection between the host computer
// and Control boards.
//

public class ControlSimulator extends Simulator implements MessageLink{

    //default constructor - not used
    public ControlSimulator() throws SocketException{};

    //simulates the default size of a socket created for ethernet access
    // NOTE: If the pipe size is too small, the outside object can fill the
    // buffer and have to wait until the thread on this side catches up.  If the
    // outside object has a timeout, then data will be lost because it will
    // continue on without writing if the timeout occurs.
    // In the future, it would be best if ControlBoard object used some flow
    // control to limit overflow in case the default socket size ends up being
    // too small.

    public static int controlBoardCounter = 0;
    int controlBoardNumber;
    String mainFileFormat;

    boolean onPipeFlag = false;
    boolean head1Down = false;
    boolean head2Down = false;
    boolean inspectMode = false;
    int simulationMode = MessageLink.STOP;
    int encoder1 = 0, encoder2 = 0;
    int encoder1DeltaTrigger = 1000, encoder2DeltaTrigger = 1000;
    int inspectPacketCount = 0;

    byte controlFlags = 0, portE = 0;

    int positionTrack; // this is the number of packets sent, not the encoder
                       // value

    public static int DELAY_BETWEEN_INSPECT_PACKETS = 1;
    int delayBetweenPackets = DELAY_BETWEEN_INSPECT_PACKETS;

    //This mimics the 7-5/8 IRNDT test joint used at Tubo Belle Chasse
    //Number of encoder counts (using leading eye for both ends): 90107
    //The PLC actually gives pipe-on when the lead eye hits the pipe and
    //pipe-off when the trailing eye leaves the pipe:
    // eye to eye distance is 53.4375", or 17,653 encoder counts.
    //Number of encoder counts for simulated joint: 90107 + 17,653 = 107,760
    // (this assumes starting with lead eye and ending with trail eye)
    //Number of counts per packet: 83
    //Number of packets for complete simulated joint: 107,760 / 83 = 1298

    //number of packets to skip before simulating lead eye reaching pipe
    //this simulates the head moving from the off-pipe position to on-pipe
    public final static int START_DELAY_IN_PACKETS = 10;

    //number of packets for length of tube -- take into account the start delay
    //packets as inspection does not occur during that time
    public static int LENGTH_OF_JOINT_IN_PACKETS =
                                                1298 + START_DELAY_IN_PACKETS;

//-----------------------------------------------------------------------------
// ControlSimulator::ControlSimulator (constructor)
//

public ControlSimulator(InetAddress pIPAddress, int pPort,
                                String pMainFileFormat) throws SocketException
{

    //call the parent class constructor
    super(pIPAddress, pPort);

    mainFileFormat = pMainFileFormat;

    //give each Control board a unique number so it can load data from the
    //simulation.ini file and such
    //this is different than the unique index provided in the parent class
    //Simulator as that number is distributed across all sub classes -- UT
    //boards, Control boards, etc.
    controlBoardNumber = controlBoardCounter++;

    //load configuration data from file
    configure();

    //create an out writer from this class - will be input for some other class
    //this writer is only used to send the greeting back to the host

    PrintWriter out = new PrintWriter(localOutStream, true);
    out.println("Hello from Control Board Simulator!");

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

    if (pWaitForPkt) {
        return processDataPacketsHelper(pWaitForPkt);
    }
    else {
        while ((x = processDataPacketsHelper(pWaitForPkt)) != -1){}
    }

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

    if (byteIn == null) {return 0;}  //do nothing if the port is closed

    //simulate the inspection signals and send back packets to the host
    if (inspectMode == true) {simulateInspection();}

    try{

        int x;

        //wait until 5 bytes are available - this should be the 4 header bytes,
        //and the packet identifier/command
        if ((x = byteIn.available()) < 5) {return -1;}

        //read the bytes in one at a time so that if an invalid byte is
        //encountered it won't corrupt the next valid sequence in the case
        //where it occurs within 3 bytes of the invalid byte

        //check each byte to see if the first four create a valid header
        //if not, jump to resync which deletes bytes until a valid first header
        //byte is reached

        //if the reSynced flag is true, the buffer has been resynced and an 0xaa
        //byte has already been read from buffer so it shouldn't be read again

        //after a resync, the function exits without processing any packets

        if (!reSynced){
            //look for the 0xaa byte unless buffer just resynced
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] != (byte)0xaa) {reSync(); return 0;}
        }
        else {
            reSynced = false;
        }

        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0x55) {reSync(); return 0;}
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0xbb) {reSync(); return 0;}
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0x66) {reSync(); return 0;}

        //read the packet ID
        byteIn.read(inBuffer, 0, 1);

        byte pktID = inBuffer[0];

        if (pktID == ControlBoard.GET_STATUS_CMD) {getStatus();}
        else
        if (pktID == ControlBoard.SET_ENCODERS_DELTA_TRIGGER_CMD)
            {setEncodersDeltaTrigger(pktID);}
        else
        if (pktID == ControlBoard.START_INSPECT_CMD) {startInspect(pktID);}
        else
        if (pktID == ControlBoard.STOP_INSPECT_CMD) {stopInspect(pktID);}
        else
        if (pktID == ControlBoard.GET_CHASSIS_SLOT_ADDRESS_CMD)
            {getChassisSlotAddress();}

        return 0;

    }//try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 221");
    }

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

    sendBytes(status, (byte)0);

}//end of ControlSimulator::getStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::setEncodersDeltaTrigger
//
// Tells the Control board how many encoder counts to wait before sending
// an encoder value update.  The trigger value for each encoder is sent.
//
// Normally, this value will be set to something reasonable like .25 to 1.0
// inch of travel of the piece being inspected. Should be no larger than the
// distance represented by a single pixel.
//

int setEncodersDeltaTrigger(byte pPktID)
{

    //read the databytes and checksum
    int bytesRead = readBlockAndVerify(5, pPktID);

    if (bytesRead < 0) {return(bytesRead);} //bail out if error

    encoder1DeltaTrigger =
                   (int)((inBuffer[0]<<8) & 0xff00) + (int)(inBuffer[1] & 0xff);

    encoder2DeltaTrigger =
                   (int)((inBuffer[2]<<8) & 0xff00) + (int)(inBuffer[3] & 0xff);

    return(bytesRead);

}//end of ControlSimulator::setEncodersDeltaTrigger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::readBlockAndVerify
//
// Reads pNumberOfBytes from byteIn into inBuffer. The bytes (including the last
// one which is the checksum) are summed with pPktID and then compared with
// 0x00.
//
// The value pNumberOfBytes should be equal to the number of data bytes
// remaining in the packet plus one for the checksum.
//
// Returns the number of bytes read if specified number of bytes were read and
// the checksum verified. Returns -1 otherwise.
//

int readBlockAndVerify(int pNumberOfBytes, byte pPktID)
{

    int bytesRead;

    try{
        bytesRead = byteIn.read(inBuffer, 0, pNumberOfBytes);
    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 299");
        return(-1);
    }

    if (bytesRead == pNumberOfBytes){

        byte sum = 0;
        for(int i = 0; i < pNumberOfBytes; i++) {sum += inBuffer[i];}

        //calculate checksum to check validity of the packet
        if ( (pPktID + sum & (byte)0xff) != 0) {return(-1);}
    }
    else{
        //error -- not enough bytes could be read
        return(-1);
    }

    return(bytesRead);

}//end of ControlSimulator::readBlockAndVerify
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::getChassisSlotAddress
//
// Simulates returning of the chassis and slot address byte.
// Also returns the status byte.
//

@Override
void getChassisSlotAddress()
{

    byte address =
            (byte)(((byte)chassisAddr<<4 & 0xf0) + ((byte)slotAddr & 0xf));

    //send standard packet header
    sendPacketHeader(ControlBoard.GET_CHASSIS_SLOT_ADDRESS_CMD);

    sendBytes(address, status);

}//end of ControlSimulator::getChassisSlotAddress
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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 358");
    }

    if (bytesRead == 2){

        //calculate checksum to check validity of the packet
        if ( (pPktID + inBuffer[0] + inBuffer[1] & (byte)0xff) != 0) {
            return(-1);
        }
    }
    else{
        //("Error - Wrong sized packet header for startInspect!\n");
        return(-1);
    }

    resetAll();

    inspectMode = true;

    return(bytesRead);

}//end of ControlSimulator::startInspect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlSimulator::resetAll
//
// Resets all variables in preparation to simulate a new piece.
//

void resetAll()
{

    positionTrack = 0;
    onPipeFlag = false;
    head1Down = false;
    head2Down = false;
    encoder1 = 0; encoder2 = 0;
    inspectPacketCount = 0;
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
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 415");
    }

    if (bytesRead == 2){

        //calculate checksum to check validity of the packet
        if ( (pPktID + inBuffer[0] + inBuffer[1] & (byte)0xff) != 0) {
            return(-1);
        }
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

    //do nothing if in STOP mode
    if (simulationMode == MessageLink.STOP) {return;}

    //delay between sending inspect packets to the host
    if (delayBetweenPackets-- != 0) {return;}
    //restart time for next packet send
    delayBetweenPackets = DELAY_BETWEEN_INSPECT_PACKETS;

    inspectPacketCount++; //count packets sent to host

    //track distance moved by number of packets sent
    if (simulationMode == MessageLink.FORWARD){
        positionTrack++;
    }
    else
    if (simulationMode == MessageLink.REVERSE){
        positionTrack--;
    }

    //the position track will run negative if inspection is occurring in the
    //reverse direction -- use absolute value to find trigger points for both
    //directions

    int triggerTrack = Math.abs(positionTrack);

    //after photo eye reaches piece, give "on pipe" signal
    if (triggerTrack >= 10) {onPipeFlag = true;} else {onPipeFlag = false;}

    //after head 1 reaches position, give head 1 down signal
    if (triggerTrack >= 200) {head1Down = true;} else {head1Down = false;}

    //after head 2 reaches position, give head 2 down signal
    if (triggerTrack >= 250) {head2Down = true;} else {head2Down = false;}

    //after head 1 reaches pick up position, give head 1 up signal
    if (triggerTrack >= LENGTH_OF_JOINT_IN_PACKETS-100) {head1Down = false;}

    //after head 2 reaches pick up position, give head 2 up signal
    if (triggerTrack >= LENGTH_OF_JOINT_IN_PACKETS-50) {head2Down = false;}

    //after photo eye reaches end of piece, turn off "on pipe" signal
    if (triggerTrack >= LENGTH_OF_JOINT_IN_PACKETS) {onPipeFlag = false;}

    //wait a bit after head has passed the end and prepare for the next piece
    if (triggerTrack >= LENGTH_OF_JOINT_IN_PACKETS + 10) {resetAll();}

    //start with all control flags set to 0
    controlFlags = (byte)0x00;
    //start with portE bits = 1, they are changed to zero if input is active
    portE = (byte)0xff;

    //set appropriate bit high for each flag which is active low
    if (onPipeFlag)
        {controlFlags = (byte)(controlFlags | ControlBoard.ON_PIPE_CTRL);}
    if (head1Down)
        {controlFlags = (byte)(controlFlags | ControlBoard.HEAD1_DOWN_CTRL);}
    if (head2Down)
        {controlFlags = (byte)(controlFlags | ControlBoard.HEAD2_DOWN_CTRL);}

    //move the encoders the forward or backward the amount expected by the host
    if (simulationMode == MessageLink.FORWARD){
        encoder1 += encoder1DeltaTrigger;
        encoder2 += encoder2DeltaTrigger;
    }
    else
    if (simulationMode == MessageLink.REVERSE){
        encoder1 -= encoder1DeltaTrigger;
        encoder2 -= encoder2DeltaTrigger;
    }

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


    outBuffer[x++] = controlFlags;
    outBuffer[x++] = portE;

    //send packet to the host
    if (byteOut != null) {
        try{
            byteOut.write(outBuffer, 0 /*offset*/, pktSize);
        }
        catch (IOException e) {
            System.err.println(getClass().getName() + " - Error: 546");
        }
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
    if (byteOut != null) {
        try{
            byteOut.write(outBuffer, 0 /*offset*/, 5);
        }
        catch (IOException e) {
            System.err.println(getClass().getName() + " - Error: 573");
        }
    }

}//end of ControlSimulator::sendPacketHeader
//----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// ControlSimulator::xmtMessage
//
// This method allows an outside class to send a message and a value to this
// class and receive a status value back.
//

@Override
public int xmtMessage(int pMessage, int pValue)
{

    if (pMessage == MessageLink.SET_MODE){

        simulationMode = pValue;

        return(MessageLink.NULL);

    }//if (pMessage == MessageLink.SET_MODE)

    return(MessageLink.NULL);

}//end of ControlSimulator::xmtMessage
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
    try {configFile = new IniFile("Simulation.ini", mainFileFormat);}
        catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 622");
        return;
    }

    String section = "Simulated Control Board " + (controlBoardNumber + 1);

    chassisAddr = (byte)configFile.readInt(section, "Chassis Number", 0);

    slotAddr = (byte)configFile.readInt(section, "Slot Number", 0);

}//end of ControlSimulator::configure
//-----------------------------------------------------------------------------

}//end of class ControlSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
