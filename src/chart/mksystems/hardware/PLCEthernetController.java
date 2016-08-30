/******************************************************************************
* Title: PLCEthernetController.java
* Author: Mike Schoonover
* Date: 08/29/16
*
* Purpose:
*
* This is the parent class for modules which handle communications with the PLC
* via Ethernet.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import chart.ThreadSafeLogger;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
// class PLCEthernetController
//

public class PLCEthernetController {


    public InetAddress plcIPAddr = null;
    String plcIPAddrS = null;
    int plcPortNum;
    Socket socket = null;
    PrintWriter out = null;
    BufferedReader in = null;
    byte[] inBuffer;
    byte[] outBuffer;
    DataOutputStream byteOut = null;
    DataInputStream byteIn = null;

    ThreadSafeLogger logger;
    boolean simulate;
    
//-----------------------------------------------------------------------------
// PLCEthernetController::PLCEthernetController (constructor)
//

public PLCEthernetController(String pPLCIPAddrS, int pPLCPortNum,
                                    ThreadSafeLogger pLogger, boolean pSimulate)
{

    plcIPAddrS = pPLCIPAddrS; plcPortNum = pPLCPortNum;
    logger = pLogger; simulate = pSimulate;
        
}//end of PLCEthernetController::PLCEthernetController (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    parseIPAddrString();
    
    establishLink();
    
}//end of PLCEthernetController::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::parseIPAddrString
//
// Creates an InetAddress object from the IP address string plcIPAddrS.
//

public void parseIPAddrString()
{

    try{
        plcIPAddr = InetAddress.getByName(plcIPAddrS);
    }
    catch(UnknownHostException e){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
    }
    
}//end of PLCEthernetController::parseIPAddrString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::establishLink
//
// Opens a socket with the PLC and sends a greeting message.
//

private void establishLink()
{

    openSocket();

    sendString("$Hello from VScan!         ");

//debug mks

    sendString("*LL:03:009:0100:00090:001:0");

    sendString("*TT:02:023:3422:01240:002:1");

    sendString("*WT:01:023:3422:01240:004:2");

    sendString("*WL:01:009:0100:00090:003:3");

    sendString("*TT:02:023:3422:01240:001:4");

    sendString("*TT:02:023:3422:01240:002:5");

    sendString("*TT:02:023:3422:01240:001:6");

    sendString("*TT:02:023:3422:01240:003:7");

    sendString("*LL:03:009:0100:00090:001:8");

    sendString("*TT:02:023:3422:01240:002:9");

    sendString("*TT:02:023:3422:01240:003:0");

    sendString("*TT:02:023:3422:01240:001:1");

    sendString("*TT:02:023:3422:01240:003:2");

    sendString("*LL:03:009:0100:00090:002:3");

//debug mks end

    
}//end of EthernetIOModule::establishLink
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::establishLink
//
// Opens a socket with the PLC.
//

public void openSocket()
{

    if (plcIPAddr == null){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
        return;
    }

    try {

        //displays message on bottom panel of IDE
        logger.logMessage("Connecting to PLC at" + plcIPAddrS + "...\n");

        if (!simulate) {socket = new Socket(plcIPAddr, plcPortNum);}
        else {
            return;
        }

        //set amount of time in milliseconds that a read from the socket will
        //wait for data - this prevents program lock up when no data is ready
        socket.setSoTimeout(250);

        // the buffer size is not changed here as the default ends up being
        // large enough - use this code if it needs to be increased
        //socket.setReceiveBufferSize(10240 or as needed);

        //allow verification that the hinted size is actually used
        logger.logMessage("PLC receive buffer size: " +
                                      socket.getReceiveBufferSize() + "...\n");

        //allow verification that the hinted size is actually used
        logger.logMessage("PLC send buffer size: " +
                                        socket.getSendBufferSize() + "...\n");

        out = new PrintWriter(socket.getOutputStream(), true);

        in = new BufferedReader(new InputStreamReader(
                                            socket.getInputStream()));

        byteOut = new DataOutputStream(socket.getOutputStream());
        byteIn = new DataInputStream(socket.getInputStream());

    }
    catch (UnknownHostException e) {
        logSevere(e.getMessage() + " - Error: 171");
        logger.logMessage("Unknown host: PLC " + plcIPAddrS + ".\n");
        return;
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 176");
        logger.logMessage("Couldn't get I/O for PLC " + plcIPAddrS + "\n");
        logger.logMessage("--" + e.getMessage() + "--\n");
        return;
    }
    

}//end of EthernetIOModule::establishLink
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::sendString
//
// Sends a string via the socket.
//

void sendString(String pValue)
{

    if (byteOut == null){ return; }
    
//    try{byteOut.writeByte((byte)'*');}catch(IOException e){} //debug mks
    
    try{
        for(int i = 0; i < pValue.length(); i++){
          byteOut.writeByte((byte) pValue.charAt(i));
        }
//        byteOut.writeByte((byte)'2'); //debug mks
        byteOut.flush();
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 206");
    }

}//end of EthernetIOModule::sendString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of PLCEthernetController::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Board::logSevere
//-----------------------------------------------------------------------------
    
}//end of class PLCEthernetController
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
