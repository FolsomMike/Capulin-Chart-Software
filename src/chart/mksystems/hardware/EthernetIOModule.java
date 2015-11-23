/******************************************************************************
* Title: EthernetIOModule.java
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.www.protocol.raw.RawURLConnection;
import net.www.protocol.raw.RawURLStreamHandlerFactory;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EthernetIOModule
//

public class EthernetIOModule extends Object
                    implements AudibleAlarmController, AnalogOutputController {

    IniFile configFile;

    boolean audibleAlarmController;
    int audibleAlarmOutputChannel;
    String audibleAlarmPulseDuration;

    IOModuleChannel[] channels;    
    
    boolean analogOutputController;

    int numAnalogOutputChannels = 0;
    int numAnalogInputChannels = 0;
    int numDigitalOutputChannels = 0;
    int numDigitalInputChannels = 0;
    
    protected String moduleType;
    protected String moduleFunction;
    protected int moduleNumber;
    protected String moduleIPAddress;

    protected URL url;
    protected URLConnection urlConn = null;
    protected BufferedReader rd = null;
    protected InputStreamReader isr = null;

    static final int RELAY_OFF = 0;
    static final int RELAY_ON = 1;
    static final int RELAY_PULSE = 2;

    boolean recentConnectionMade = false;
    
    int delayBetweenAccesses; //time delay in ms between consecutive accesses
    
    static String xmlBaseURL = "";
    static String xmlBaseURLSuffix = "";
    
//-----------------------------------------------------------------------------
// EthernetIOModule::EthernetIOModule (constructor)
//
// Each module should have a unique pModuleNumber.
//

public EthernetIOModule(int pModuleNumber, IniFile pConfigFile)
{

    moduleNumber = pModuleNumber;
    configFile = pConfigFile;
    
    delayBetweenAccesses = 300; //default to 300 milliseconds

}//end of EthernetIOModule::EthernetIOModule (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::init
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

}//end of EthernetIOModule::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::okayToConnect
//
// If too many connections are attempted too rapidly, the system will stutter
// as the buffer fills or responses are delayed.
//
// Connection requests are limited to one every 0.3 seconds. If this function
// has not been called within the past 0.3 seconds, it returns true to signal
// that it is okay to connect.
//
// If a connection has been made in the last 0.3 seconds, this function returns
// false to signal that no connection attempt should be made.
//
// This function sets a flag and starts a new thread to clear that flag in
// 0.3 seconds as the control mechanism.
//

public boolean okayToConnect()
{

    if (recentConnectionMade) { return(false); }
    
    recentConnectionMade = true;
    
    //start thread to clear the flag after a delay
    
    new Thread(() -> {
        try {Thread.sleep(delayBetweenAccesses);}catch(InterruptedException e){}
        recentConnectionMade = false;
    }).start();

    return(true); //okay to make a connection
    
}//end of EthernetIOModule::okayToConnect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::connectSendGetRequestClose
//
// Opens a connection with the module, sends an HTTP GET request to a url,
// then closes the connection.
//
// If pWaitForReply is true, waits for data and returns the received data as a
// string.
// 
// If the connection is not made because a previous access is still in play,
// returns null.
//
// Example URL:
//      raw:XMLNoHeader//169.254.1.3/state.xml?relayState=1
//
// The above will send "GET /state.xml?relayState=1 HTTP/1.1" string to
// host at 169.254.1.3 using a handler which can parse raw text data.
//

public String connectSendGetRequestClose(String pNetURL, boolean pWaitForReply)
{

    if(!okayToConnect()) { return(null); }
    
    String result = "";

    StringBuilder sb = null;
    Object o = null;

    try{

        url = new URL(pNetURL);

        urlConn = url.openConnection();

        //allow ten seconds for a connection attempt
        //(note that 0 actually defaults to about 22 seconds, not infinity
        // as described in the document for URLConnection class)
        urlConn.setConnectTimeout(10000);

        //set amount of time in milliseconds that a read from the socket will
        //wait for data - this prevents program lock up when no data is ready
        //or less data than will fill the buffer has been sent
        urlConn.setReadTimeout(3000);

        //send the URL with the command and state info
        o = ( urlConn.getContent() );
        
        if(!pWaitForReply) { return(""); }
        
        isr = new InputStreamReader((InputStream)o);
        rd = new BufferedReader(isr);

        sb = new StringBuilder();
        String line;

        while ((line = rd.readLine()) != null){
            sb.append(line);
        }

        result = sb.toString();

    }
    catch(MalformedURLException me){

        logSevere(me.getMessage() + " - Error: 138");

    }//catch
    catch (SocketTimeoutException e){

        //this is not usually an error -- it will be thrown when the host
        //has finished sending data -- the calling code can sort out if it was
        //a true error by whether or not data was received

        //data will often have been read before the timeout, so return it

        if (sb != null) {result = sb.toString();}

    }
    catch (ConnectException e){

        //this can get thrown when the host has not closed down the connection
        //from a previous attempt -- wait for 5 seconds before trying again
        waitSleep(5000);

    }
    catch (IOException e){
        logSevere(e.getMessage() + " - Error: 161");
    }//catch
    finally{

        try{
            if (rd!=null) {rd.close();}
            if (isr!=null) {isr.close();}
            if (o!=null) {((InputStream)o).close();}
            if (urlConn!=null){((RawURLConnection)urlConn).disconnect();}
            }
        catch(IOException e){}
    }

    return(result);

}//end of EthernetIOModule::connectSendGetRequestClose
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::waitSleep
//
// Sleeps for pTime milliseconds.
//

public void waitSleep(int pTime)
{

    try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of EthernetIOModule::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::flipAnalogOutput
//

public void flipAnalogOutput(int pWhichOutput)
{

}//end of EthernetIOModule::flipAnalogOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::setOutPut
//
// Sets the output pWhichOutput's value to pValue.
//

public void setOutput(int pWhichOutput, double pValue)
{

}//end of EthernetIOModule::setOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::setOutputWithMinMaxPeakHold
//
// Stores the peak min and max values from pValue so they are not lost if not
// ready to send a new data point.
//
// If ready, alternates between setting output pWhichOutput's value to the
// min or the max so both are transmitted.
//

public void setOutputWithMinMaxPeakHold(int pWhichOutput, double pValue)
{
        
}//end of EthernetIOModule::setOutputWithMinMaxPeakHold
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::setRelayState
//
// Sets the relay pWhichRelay's state to pState (RELAY_ON, RELAY_OFF,
// RELAY_PULSE). If RELAY_PULSE is specified, then pPulseDuration specifies the
// duration of the pulse in seconds.
//

public void setRelayState(int pWhichRelay, int pState, String pPulseDuration)
{

}//end of EthernetIOModule::setRelayState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::pulseRelay
//
// Pulses relay number pWhichRelay for pPulseDuration seconds.
//

public void pulseRelay(int pWhichRelay, String pPulseDuration)
{

}//end of EthernetIOModule::pulseRelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::turnOnAudibleAlarm
//
// Turns on the relay which fires the audible alarm for one second.
//

@Override
public void turnOnAudibleAlarm()
{

    setRelayState(audibleAlarmOutputChannel, RELAY_ON,
                                                    audibleAlarmPulseDuration);

}//end of EthernetIOModule::turnOnAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::turnOffAudibleAlarm
//
// Turns off the relay which fires the audible alarm for one second.
//

@Override
public void turnOffAudibleAlarm()
{

    setRelayState(audibleAlarmOutputChannel, RELAY_OFF,
                                                    audibleAlarmPulseDuration);

}//end of EthernetIOModule::turnOffAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::pulseAudibleAlarm
//
// Pulses the relay which fires the audible alarm for one second.
//

@Override
public void pulseAudibleAlarm()
{

    pulseRelay(audibleAlarmOutputChannel, audibleAlarmPulseDuration);

}//end of EthernetIOModule::pulseAudibleAlarm
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::pulseAlarmMarker
//
// Pulses the relay which fires the alarm/marker specified by pChannel for one
/// second.
//

@Override
public void pulseAlarmMarker(int pChannel)
{
    
    pulseRelay(pChannel, audibleAlarmPulseDuration);

}//end of EthernetIOModule::pulseAlarmMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::isAudibleAlarmController
//
// Returns audibleAlarmController.
//

@Override
public boolean isAudibleAlarmController()
{

    return(audibleAlarmController);

}//end of EthernetIOModule::isAudibleAlarmController
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::isAnalogOutputController
//
// Returns analogOutputController.
//

@Override
public boolean isAnalogOutputController()
{

    return(analogOutputController);

}//end of EthernetIOModule::isAnalogOutputController
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::getLinkedChannel
//
// Returns the sensor input channel with which the module channel pChannel is
// linked.
//
// If the linked channel is -1, then no channel is linked.
//

public int getLinkedChannel(int pChannel)
{
    
    return(-1);

}//end of EthernetIOModule::getLinkedChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::configure
//
// Loads configuration settings from pConfigFile.
//

void configure(IniFile pConfigFile)
{

    String section = "IO Module " + moduleNumber;

    moduleType = pConfigFile.readString(section, "Module Type", "unknown");

    moduleFunction =pConfigFile.readString(section,"Module Function","unknown");

    moduleIPAddress =
                pConfigFile.readString(section, "Module IP Address", "unknown");

    audibleAlarmController =
              pConfigFile.readBoolean(section, "Audible Alarm Module", false);

    audibleAlarmOutputChannel =
            pConfigFile.readInt(section, "Audible Alarm Output Channel", 0);

    audibleAlarmPulseDuration =
          pConfigFile.readString(section, "Audible Alarm Pulse Duration", "1");

}//end of EthernetIOModule::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EthernetIOModule::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of EthernetIOModule::logSevere
//-----------------------------------------------------------------------------


}//end of class EthernetIOModule
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class IOModuleChannel
//

class IOModuleChannel {

    int outputMode;
    int linkedChannel;
    double outputValue;
    double minValue = Double.MAX_VALUE, maxValue = Double.MIN_VALUE;
    boolean minMaxFlip = true;
    
    final static int CURRENT_LOOP_4_20MA = 0;
    final static int VOLTAGE_0_5V = 1;
    final static int VOLTAGE_0_10V = 2;
    final static int VOLTAGE_PLUS_MINUS_5V = 3;    
    final static int VOLTAGE_PLUS_MINUS_10V = 4;
     
}//end of class IOModuleChannel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
