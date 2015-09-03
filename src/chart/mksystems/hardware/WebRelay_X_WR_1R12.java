/******************************************************************************
* Title: WebRelay_X_WR_1R12.java
* Author: Mike Schoonover
* Date: 10/22/13
*
* Purpose:
*
* This is the parent class for classes which interface with I/O modules via
* Ethernet. These modules provide relay outputs, digital inputs, temperature
* readings, etc.
*
* This class interfaces with WebRelay series X-WR-1R12 modules from
* www.controlbyweb.com / Xytronix Research & Design, Inc.
*
* This series provides 1 SPDT relay and 1 digital input.
*
* These modules are accessed via HTML or XTML web protocols -- the modules
* mimic web servers.
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
import java.util.logging.Level;
import java.util.logging.Logger;
import net.www.protocol.raw.RawURLConnection;


public class WebRelay_X_WR_1R12 extends EthernetIOModules {

    static final String MODULE_CONTROL_FILEMAME = "state.xml";

    static String relaySetBaseURL;

    private boolean recentConnectionMade = false;
    
//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::WebRelay_X_WR_1R12 (constructor)
//

public WebRelay_X_WR_1R12(int pModuleNumber, IniFile pConfigFile)
{

    super(pModuleNumber, pConfigFile);

}//end of WebRelay_X_WR_1R12::WebRelay_X_WR_1R12 (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{
    super.init();

    configure(configFile);

    relaySetBaseURL = "raw:XMLNoHeader//" + moduleIPAddress +
                                                    "/state.xml?relayState=";

}//end of WebRelay_X_WR_1R12::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::okayToConnect
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
        try {Thread.sleep(300);} catch(InterruptedException e){}
        recentConnectionMade = false;
    }).start();

    return(true); //okay to make a connection
    
}//end of WebRelay_X_WR_1R12::okayToConnect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::connectSendGetRequestClose
//
// Opens a connection with the module, sends an HTTP GET request to a url,
// then closes the connection.
//
// Returns the received data as a string.
//
// Example URL:
//      raw:XMLNoHeader//169.254.1.3/state.xml?relayState=1
//
// The above will send "GET /state.xml?relayState=1 HTTP/1.1" string to
// host at 169.254.1.3 using a handler which can parse raw text data.
//

public String connectSendGetRequestClose(String pNetURL)
{

    if(!okayToConnect()) { return(""); }
    
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

        //get the response
        o = ( urlConn.getContent() );
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

}//end of WebRelay_X_WR_1R12::connectSendGetRequestClose
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::setRelayState
//
// Sets the relay pWhichRelay's state to pState (RELAY_ON, RELAY_OFF,
// RELAY_PULSE). If RELAY_PULSE is specified, then pPulseDuration specifies the
// duration of the pulse in seconds.
//
// In this class, the module only has one relay so pWhichRelay is ignored.
//
// If pPulseDuration <= 0, no pulse duration is sent to the module in which
// case it will use the preset duration. This preset is set manually via a
// web brower accessing the module's setup page.
//

@Override
public void setRelayState(int pWhichRelay, int pState, String pPulseDuration)
{

    String controlURL;

    if (pState == RELAY_PULSE && Double.parseDouble(pPulseDuration) > 0){
        controlURL = relaySetBaseURL + pState + "&pulseTime=" + pPulseDuration;
    }
    else{
        controlURL = relaySetBaseURL + pState;
    }

    connectSendGetRequestClose(controlURL);

}//end of WebRelay_X_WR_1R12::setRelayState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::pulseRelay
//
// Pulses relay number pWhichRelay for pPulseDuration seconds.
//
// In this class, the module only has one relay so pWhichRelay is ignored.
//
// If pPulseDuration <= 0, no pulse duration is sent to the module in which
// case it will use the preset duration. This preset is set manually via a
// web brower accessing the module's setup page.
//

@Override
public void pulseRelay(int pWhichRelay, String pPulseDuration)
{

    String controlURL;

    if (Double.parseDouble(pPulseDuration) > 0){
        controlURL =
                relaySetBaseURL + RELAY_PULSE + "&pulseTime=" + pPulseDuration;
    }
    else{
        controlURL = relaySetBaseURL + RELAY_PULSE;

    }

    connectSendGetRequestClose(controlURL);

}//end of WebRelay_X_WR_1R12::pulseRelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::configure
//
// Loads configuration settings from pConfigFile.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

}//end of WebRelay_X_WR_1R12::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of WebRelay_X_WR_1R12::logSevere
//-----------------------------------------------------------------------------

}//end of class WebRelay_X_WR_1R12
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
