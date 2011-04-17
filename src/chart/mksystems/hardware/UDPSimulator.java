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
* This is a subclass of DatagramPacket and can be substituted for an instance
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

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UDPSimulator
//
// This class simulates data from a UDP connection between the host computer
// and remote devices such as Control Boards, UT Boards, etc.
//

public class UDPSimulator extends DatagramSocket{

public UDPSimulator() throws SocketException{}; //default constructor - not used

int port;

int responseCount = 0;

IniFile configFile;

//-----------------------------------------------------------------------------
// UDPSimulator::UDPSimulator (constructor)
//
  
public UDPSimulator(int pPort) throws SocketException
{

port = pPort;

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
//
  
@Override
public void receive(DatagramPacket p)
{

String s = "UT board present...";

p.setData(s.getBytes());

//each simulated UT board sends a response packet which will have its IP
//address - for each instance of this class created, use the next sequential
//IP address

String ip = "169.254.1." + (++responseCount);

try{p.setAddress(InetAddress.getByName(ip));}
catch(UnknownHostException e){}

}//end of UDPSimulator::receive
//-----------------------------------------------------------------------------


}//end of class UDPSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
