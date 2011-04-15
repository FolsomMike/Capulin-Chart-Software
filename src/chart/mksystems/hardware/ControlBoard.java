/******************************************************************************
* Title: ControlBoard.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class interfaces with a Capulin Control board.
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
// class ControlBoard
//
// This class creates and handles an interface to a Control board.
//

public class ControlBoard extends Board{


int monitorPacketSize;

int packetRequestTimer = 0;

int runtimePacketSize;

//Commands for Control boards
//These should match the values in the code for those boards.

static byte NO_ACTION = 0;
static byte UNUSED1_CMD = 1;
static byte ZERO_ENCODERS_CMD = 2;
static byte GET_MONITOR_PACKET_CMD = 3;
static byte PULSE_OUTPUT_CMD = 4;
static byte TURN_ON_OUTPUT_CMD = 5;
static byte TURN_OFF_OUTPUT_CMD = 6;
static byte SET_ENCODERS_DELTA_TRIGGER_CMD = 7;
static byte START_INSPECT_CMD = 8;
static byte STOP_INSPECT_CMD = 9;
static byte START_MONITOR_CMD = 10;
static byte STOP_MONITOR_CMD = 11;
static byte DEBUG_CMD = 126;
static byte EXIT_CMD = 127;

//Status Codes for Control boards
//These should match the values in the code for those boards.

static byte NO_STATUS = 0;

static int RUNTIME_PACKET_SIZE = 2048;

//-----------------------------------------------------------------------------
// UTBoard::UTBoard (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public ControlBoard(IniFile pConfigFile, String pBoardName, int pBoardIndex,
                      int pRuntimePacketSize, boolean pSimulate, JTextArea pLog)
{

configFile = pConfigFile; 
boardName = pBoardName;
boardIndex = pBoardIndex;
runtimePacketSize = pRuntimePacketSize;
simulate = pSimulate;

log = pLog;
threadSafeMessage = new String[NUMBER_THREADSAFE_MESSAGES];

//read the configuration file and create/setup the charting/control elements
configure(configFile);

}//end of UTBoard::UTBoard (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::run
//
// This thread loads the board with FPGA and DSP code.  Using a thread allows
// multiple boards to be loaded simultaneously.
//

@Override
public void run() { 
         
try{
    
    connect();
    
    Thread.sleep(10); //here just to avoid exception not thrown error
          
    }//try
    
catch (InterruptedException e) {    
    }

}//end of ControlBoard::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::connect
//
// Opens a TCP/IP connection with the Control Board.
//

public void connect()
{

//displays message on bottom panel of IDE
threadSafeLog("Opening connection with Control board...\n"); 
  
try {

    threadSafeLog("Control Board IP Address: " + ipAddr.toString() + "\n");

    if (!simulate) socket = new Socket(ipAddr, 23);
    else socket = new ControlSimulator(ipAddr, 23);

    out = new PrintWriter(socket.getOutputStream(), true);

    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    byteOut = new DataOutputStream(socket.getOutputStream());
    byteIn =  new DataInputStream(socket.getInputStream());

    }//try
    catch (IOException e) {
        threadSafeLog("Couldn't get I/O for " + ipAddrS + "\n");
        return;
        }

try {
    //display the greeting message sent by the remote
    threadSafeLog(ipAddrS + " says " + in.readLine() + "\n");
    }
catch(IOException e){}

//flag that board setup has been completed - whether it failed or not
setupComplete = true;

//flag that setup was successful and board is ready for use
ready = true;

}//end of ControlBoard::connectControlBoard
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard:initialize
//
// Sets up various settings on the board.
//

public void initialize()
{

setEncodersDeltaTrigger();

}//end of ControlBoard::initialize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::startMonitor
//
// Places the Control board in Monitor status and displays the status of
// various I/O as sent back from the Control board.
//
// The parameter dMonitorPacketSize specifies the size of the packet buffer.

public void startMonitor(int dMonitorPacketSize)
{

monitorPacketSize = dMonitorPacketSize;

sendBytes2(START_MONITOR_CMD, (byte) 0);

}//end of ControlBoard::startMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::stopMonitor
//
// Takes the Control board out of monitor mode.
//

public void stopMonitor()
{
    
sendBytes2(STOP_MONITOR_CMD, (byte) 0);

}//end of ControlBoard::stopMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::getMonitorPacket
//
// Stuffs I/O status received from the remote into an array.
// If pRequestPacket is true, then a packet is requested every so often.
// If false, then packets are only received when the remote computer sends
// them.
//

public void getMonitorPacket(byte[] pMonitorBuffer, boolean pRequestPacket)
{

int c;

if (byteIn != null)
    try {
 
        c = byteIn.available();
        
        if (c >= monitorPacketSize) 
            byteIn.read(pMonitorBuffer, 0, monitorPacketSize);
            
        }
    catch(EOFException eof){log.append("End of stream.\n");}
    catch(IOException e){}

if (pRequestPacket)
        //request a packet be sent if the counter has timed out
        if (packetRequestTimer++ == 50){
            packetRequestTimer = 0;
            sendBytes2(GET_MONITOR_PACKET_CMD, (byte) 0);
            }

}//end of ControlBoard::getMonitorPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::zeroEncoderCounts
//
// Sends command to zero the encoder counts.
//

public void zeroEncoderCounts()
{
    
sendBytes2(ZERO_ENCODERS_CMD, (byte) 0);

}//end of ControlBoard::zeroEncoderCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::pulseOutput
//
// Pulses the specified output(s).
//
// WIP MKS - need to add parameter to pass in which output to be fired.
// Currently the Rabbit code simply fires output 1 with this call.
//

public void pulseOutput()
{

sendBytes2(PULSE_OUTPUT_CMD, (byte) 0);

}//end of ControlBoard::pulseOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::turnOnOutput
//
// Turns on the specified output(s).
//
// WIP MKS - need to add parameter to pass in which output to be fired.
// Currently the Rabbit code simply fires output 1 with this call.
//

public void turnOnOutput()
{

sendBytes2(TURN_ON_OUTPUT_CMD, (byte) 0);

}//end of ControlBoard::turnOnOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::turnOffOutput
//
// Turns off the specified output(s).
//
// WIP MKS - need to add parameter to pass in which output to be fired.
// Currently the Rabbit code simply fires output 1 with this call.
//

public void turnOffOutput()
{

sendBytes2(TURN_OFF_OUTPUT_CMD, (byte) 0);

}//end of ControlBoard::turnOffOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::setEncodersDeltaTrigger
//
// Tells the Control board how many encoder counts to wait before sending
// an encoder value update.  The trigger value for each encoder is sent.
//
// Normally, this value will be set to something reasonable like 1 inch of
// travel of the piece being inspected.
//

public void setEncodersDeltaTrigger()
{

//debug mks -- read these values from config file

int encoder1DeltaTrigger = 1000;
int encoder2DeltaTrigger = 2000;

//debug mks end

sendBytes5(SET_ENCODERS_DELTA_TRIGGER_CMD,
            (byte)((encoder1DeltaTrigger >> 8) & 0xff),
            (byte)(encoder1DeltaTrigger & 0xff),
            (byte)((encoder2DeltaTrigger >> 8) & 0xff),
            (byte)(encoder2DeltaTrigger & 0xff)
            );

}//end of ControlBoard::setEncodersDeltaTrigger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::startInspect
//
// Puts Control board in the inspect mode.  In this mode the Control board
// will monitor encoder and status signals and return position information to
// the host.
//

public void startInspect()
{

sendBytes2(START_INSPECT_CMD, (byte) 0);

}//end of ControlBoard::startInspect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::stopInspect
//
// Takes Control board out of the inspect mode.
//

public void stopInspect()
{

sendBytes2(STOP_INSPECT_CMD, (byte) 0);

}//end of ControlBoard::stopInspect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::prepareData
//
// Retrieves a data packet from the incoming data buffer and distributes it
// to the newData variables in each gate.
//
// Returns true if new data is available, false if not.
//

public boolean prepareData()
{

if (byteIn != null)
    try {
 
        int c = byteIn.available();

        //if a full packet is not ready, return false
        if (c < runtimePacketSize) return false;
        
        byteIn.read(inBuffer, 0, runtimePacketSize);

        //wip mks - distribute the data to the gate's newData variables here
        
        }
    catch(EOFException eof){log.append("End of stream.\n"); return false;}
    catch(IOException e){return false;}

return true;

}//end of ControlBoard::prepareData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlBoard::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//

private void configure(IniFile pConfigFile)
{

inBuffer = new byte[RUNTIME_PACKET_SIZE];
outBuffer = new byte[RUNTIME_PACKET_SIZE];

}//end of ControlBoard::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

protected void shutDown()
{

//close everything - the order of closing may be important

try{

    if (byteOut != null) byteOut.close();
    if (byteIn != null) byteIn.close();
    if (out != null) out.close();
    if (in != null) in.close();
    if (socket != null) socket.close();
    
    }
catch(IOException e){}

}//end of ControlBoard::shutDown
//-----------------------------------------------------------------------------

}//end of class ControlBoard
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------    
