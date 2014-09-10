/******************************************************************************
* Title: UDPSimulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class simulates a UDP connection between the host and the remote
* devices (Control Boards, UT Boards, etc.)
*
* This is a subclass of MulticastSocket and can be substituted for an instance
* of that class when simulated data is needed.
* 
* This class uses a crude method to simulate the socket...it overrides the
* MulticastSocket.receive method and simply returns a data packet when the
* main program calls that method to receive data.
* 
* The Ethernet socket simulator used by the Simulator class is a more robust
* simulation and actually reads and writes data to the underlying sockets
* to communicate with the main program.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.inifile.IniFile;
import java.io.IOException;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UDPSimulator
//
// This class simulates data from a UDP connection between the host computer
// and remote devices such as Control Boards, UT Boards, etc.
//

public class UDPSimulator extends MulticastSocket{

    int port;

    int responseCount = 0;

    String announcement;

    IniFile configFile;

//default constructor - not used    
public UDPSimulator() throws SocketException, IOException{};


//-----------------------------------------------------------------------------
// UDPSimulator::UDPSimulator (constructor)
//

public UDPSimulator(int pPort, String pAnnouncement)
        throws SocketException, IOException
{

    super(pPort);

    port = pPort;

    announcement = pAnnouncement;

}//end of UDPSimulator::UDPSimulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UDPSimulator::send
//
//

@Override
public void send(DatagramPacket p)
{

}//end of UDPSimulator::send
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UDPSimulator::receive
//
// This method gets triggered when the program sends a UDP packet.
//
// See notes in header regarding the methods used in this class compared to
// the socket simulator in the Simulator class.
//

@Override
public void receive(DatagramPacket p)
{

    p.setData(announcement.getBytes());

    //each simulated UT board sends a response packet which will have its IP
    //address - for each instance of this class created, use the next sequential
    //IP address

    String ip = "169.254.1." + (++responseCount);

    try{p.setAddress(InetAddress.getByName(ip));}
    catch(UnknownHostException e){
        logSevere(e.getMessage() + " - Error: 96");
    }

}//end of UDPSimulator::receive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UDPSimulator::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of UDPSimulator::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UDPSimulator::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of UDPSimulator::logStackTrace
//-----------------------------------------------------------------------------

}//end of class UDPSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
