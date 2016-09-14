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

    int messageCount = 0;

    private static final int PLC_MESSAGE_LENGTH = 28;
    
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

    sendString("@Hello from VScan!        ");

    //sendTestMessages(); //debug mks

}//end of EthernetIOModule::establishLink
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::sendTestMessages
//
// Sends a batch of messages for testing purposes.
//

public void sendTestMessages()
{

    sendString("@Block of test messages:  ");

    sendString("#001|                     ");

    sendString("#002|                     ");

    sendString("#003|                     ");
    
    sendString("*LL |03|009|0100|00090|001");

    sendString("*TT |02|023|3422|01240|002");

    sendString("*WT |01|023|3422|01240|004");

    sendString("*WL |01|009|0100|00090|003");

    sendString("*TL |02|023|3422|01240|001");

    sendString("*LT |02|023|3422|01240|002");

    sendString("*22L|02|023|3422|01240|001");

    sendString("*22T|02|023|3422|01240|003");

    sendString("*12L|03|009|0100|00090|001");

    sendString("*12T|02|023|3422|01240|002");

    sendString("*12L|02|023|3422|01240|003");

    sendString("*45L|02|023|3422|01240|001");

    sendString("*45T|02|023|3422|01240|003");

    sendString("*LL |03|009|0100|00090|002");

}//end of EthernetIOModule::sendTestMessages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::establishLink
//
// Opens a socket with the PLC.
//

private void openSocket()
{

    if (plcIPAddr == null){
        logger.logMessage("Error: PLC IP Address is not valid.\n");
        return;
    }

    try {

        //displays message on bottom panel of IDE
        logger.logMessage("Connecting to PLC at: " + plcIPAddrS + "...\n");

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
    

    logger.logMessage("PLC connected.\n");
    
}//end of EthernetIOModule::establishLink
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::sendString
//
// Sends a string via the socket. A | symbol and single digit alpha message
// count will be appended to the message.
//

public void sendString(String pValue)
{

    if (byteOut == null){ return; }

    pValue = pValue + "|" + getAndIncrementMessageCount();
  
    assert(pValue.length() == PLC_MESSAGE_LENGTH);
    
    try{
        for(int i = 0; i < pValue.length(); i++){
          byteOut.writeByte((byte) pValue.charAt(i));
        }
        byteOut.flush();
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 255");
    }

}//end of EthernetIOModule::sendString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::getAndIncrementMessageCount
//
// Returns the current message count value as a string and increments it for
// the next use. Rolls back to 0 when value of 10 reached; range is 0~9
//

private String getAndIncrementMessageCount()
{

    int value = messageCount;

    messageCount++;
    
    if (messageCount == 10){ messageCount = 0; }

    return(Integer.toString(value));

}//end of EthernetIOModule::getAndIncrementMessageCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

public void shutDown()
{

    //close everything - the order of closing may be important

    try{
        if (byteOut != null) {byteOut.close();}
        if (byteIn != null) {byteIn.close();}
        if (out != null) {out.close();}
        if (in != null) {in.close();}
        if (socket != null) {socket.close();}
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 267");
    }

}//end of PLCEthernetController::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::waitSleep
//
// Sleeps for pTime milliseconds.
//

private void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of PLCEthernetController::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PLCEthernetController::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

private void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Board::logSevere
//-----------------------------------------------------------------------------
    
}//end of class PLCEthernetController
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
