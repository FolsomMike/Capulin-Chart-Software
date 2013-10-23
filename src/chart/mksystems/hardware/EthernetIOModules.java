/******************************************************************************
* Title: EthernetIOModules.java
* Author: Mike Schoonover
* Date: 10/22/13
*
* Purpose:
*
* This is the parent class for classes which interface with I/O modules via
* Ethernet. These modules provide relay outputs, digital inputs, temperature
* readings, etc. These modules are accessed via HTML or XTML web protocols --
* the modules mimic web servers.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import chart.mksystems.inifile.IniFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import net.www.protocol.raw.RawURLStreamHandlerFactory;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EthernetIOModules
//

public class EthernetIOModules extends Object
                                        implements AudibleAlarmController {

    IniFile configFile;

    boolean audibleAlarmController;
    int audibleAlarmOutputChannel;
    String audibleAlarmPulseDuration;

    protected String moduleType;
    protected int moduleNumber;
    protected String moduleIPAddress;

    protected URL url;
    protected URLConnection urlConn = null;
    protected BufferedReader rd = null;
    protected InputStreamReader isr = null;

    static final int RELAY_OFF = 0;
    static final int RELAY_ON = 1;
    static final int RELAY_PULSE = 2;


//-----------------------------------------------------------------------------
// EthernetIOModules::EthernetIOModules (constructor)
//
// Each module should have a unique pModuleNumber.
//

public EthernetIOModules(int pModuleNumber, IniFile pConfigFile)
{

    moduleNumber = pModuleNumber;
    configFile = pConfigFile;

}//end of EthernetIOModules::EthernetIOModules (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//
// The stream handler factory can only be called once per Java Virtual Machine
// instance. When the program tries to restart after a job or preset load,
// the attempt to set the factory again will cause an exception which can be
// caught and ignored. Oddly, the attempt throws an Error rather than an
// Exception.
//

public void init()
{

    //set this so our custom version of HTTP stream can be used -- the
    //WebRelay modules have some non-standard response formats which won't
    //work with the default handlers

    try{
        URL.setURLStreamHandlerFactory(new RawURLStreamHandlerFactory());
    }
    catch(Error e){
        //see notes in method header about only setting factory once
    }

}//end of EthernetIOModules::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of EthernetIOModules::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::setRelayState
//
// Sets the relay pWhichRelay's state to pState (RELAY_ON, RELAY_OFF,
// RELAY_PULSE). If RELAY_PULSE is specified, then pPulseDuration specifies the
// duration of the pulse in seconds.
//

public void setRelayState(int pWhichRelay, int pState, String pPulseDuration)
{

}//end of EthernetIOModules::setRelayState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::pulseRelay
//
// Pulses relay number pWhichRelay for pPulseDuration seconds.
//

public void pulseRelay(int pWhichRelay, String pPulseDuration)
{

}//end of EthernetIOModules::pulseRelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::turnOnAudibleAlarm
//
// Turns on the relay which fires the audible alarm for one second.
//

@Override
public void turnOnAudibleAlarm()
{

    setRelayState(audibleAlarmOutputChannel, RELAY_ON,
                                                    audibleAlarmPulseDuration);

}//end of EthernetIOModules::turnOnAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::turnOffAudibleAlarm
//
// Turns off the relay which fires the audible alarm for one second.
//

@Override
public void turnOffAudibleAlarm()
{

    setRelayState(audibleAlarmOutputChannel, RELAY_OFF,
                                                    audibleAlarmPulseDuration);

}//end of EthernetIOModules::turnOffAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::pulseAudibleAlarm
//
// Pulses the relay which fires the audible alarm for one second.
//

@Override
public void pulseAudibleAlarm()
{

    pulseRelay(audibleAlarmOutputChannel, audibleAlarmPulseDuration);

}//end of EthernetIOModules::pulseAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::isAudibleAlarmController
//
// Returns audibleAlarmController.
//

@Override
public boolean isAudibleAlarmController()
{

    return(audibleAlarmController);

}//end of EthernetIOModules::isAudibleAlarmController
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModules::configure
//
// Loads configuration settings from pConfigFile.
//

void configure(IniFile pConfigFile)
{

    String section = "IO Module " + moduleNumber;

    moduleType = pConfigFile.readString(section, "Module Type", "unknown");

    moduleIPAddress =
                pConfigFile.readString(section, "Module IP Address", "unknown");

    audibleAlarmController =
              pConfigFile.readBoolean(section, "Audible Alarm Module", false);

    audibleAlarmOutputChannel =
            pConfigFile.readInt(section, "Audible Alarm Output Channel", 0);

    audibleAlarmPulseDuration =
          pConfigFile.readString(section, "Audible Alarm Pulse Duration", "1");

}//end of EthernetIOModules::configure
//-----------------------------------------------------------------------------

}//end of class EthernetIOModules
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
