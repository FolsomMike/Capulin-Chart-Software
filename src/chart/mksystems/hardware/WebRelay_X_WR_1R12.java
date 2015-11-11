/******************************************************************************
* Title: WebRelay_X_WR_1R12.java
* Author: Mike Schoonover
* Date: 10/22/13
*
* Purpose:
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

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class WebRelay_X_WR_1R12
//

public class WebRelay_X_WR_1R12 extends EthernetIOModule {

    static final int NUM_DIGITAL_OUTPUT_CHANNELS = 1;
    static final int NUM_DIGITAL_INPUT_CHANNELS = 1;
    
    static final String MODULE_CONTROL_FILEMAME = "state.xml";

//-----------------------------------------------------------------------------
// WebRelay_X_WR_1R12::WebRelay_X_WR_1R12 (constructor)
//

public WebRelay_X_WR_1R12(int pModuleNumber, IniFile pConfigFile)
{

    super(pModuleNumber, pConfigFile);
    
    numDigitalOutputChannels = NUM_DIGITAL_OUTPUT_CHANNELS;
    numDigitalInputChannels = NUM_DIGITAL_INPUT_CHANNELS;

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

    //allow for data transmission rate of .3 seconds between each
    delayBetweenAccesses = 300;

    configure(configFile);

    xmlBaseURL = "raw:XMLNoHeader//" + moduleIPAddress +
                                                    "/state.xml?relayState=";

}//end of WebRelay_X_WR_1R12::init
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
        controlURL = xmlBaseURL + pState + "&pulseTime=" + pPulseDuration;
    }
    else{
        controlURL = xmlBaseURL + pState;
    }

    connectSendGetRequestClose(controlURL, true);

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
        controlURL = xmlBaseURL + RELAY_PULSE + "&pulseTime=" + pPulseDuration;
    }
    else{
        controlURL = xmlBaseURL + RELAY_PULSE;

    }

    connectSendGetRequestClose(controlURL, true);

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

}//end of class WebRelay_X_WR_1R12
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
