/******************************************************************************
* Title: X_317_AnalogOutputModule
* Author: Mike Schoonover
* Date: 11/09/15
*
* Purpose:
*
* This class interfaces with X-317 Analog Output modules from
* www.controlbyweb.com / Xytronix Research & Design, Inc.
*
* This series provides five separate analog output channels configurable for
* 0-5V, 0-10V, ±5V, ±10V, 4-20mA modes.
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
import java.text.DecimalFormat;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class X_317_AnalogOutputModule
//

public class X_317_AnalogOutputModule extends EthernetIOModule {

    static final String MODULE_CONTROL_FILEMAME = "state.xml";

    DecimalFormat decimalFormat = new DecimalFormat("0.00");    
    
    static final int NUM_CHANNELS = 4;
    
    static final double MIN_OUTPUT_VALUE = 4.0;
    static final double MAX_OUTPUT_VALUE = 20.0;    
    
//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::X_317_AnalogOutputModule (constructor)
//

public X_317_AnalogOutputModule(int pModuleNumber, IniFile pConfigFile)
{

    super(pModuleNumber, pConfigFile);

    numAnalogOutputChannels = NUM_CHANNELS;
    
    channels = new IOModuleChannel[numAnalogOutputChannels];
    
}//end of X_317_AnalogOutputModule::X_317_AnalogOutputModule (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{
    super.init();

    //allow for rapid data transmission rate of .1 seconds between each
    delayBetweenAccesses = 100;
    
    configure(configFile);

    xmlBaseURL = "raw:XMLNoHeader//" + moduleIPAddress + "/state.xml?";
    
    xmlBaseURLSuffix = "&noReply=1";
  
    setOutput(0, 4.0); //initialize at 4mA
    
}//end of X_317_AnalogOutputModule::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::flipAnalogOutput
//
// Flips channel pWhichOutput from min value to max value and vice versa.
// If value is currently neither min nor max, value is set to min.
//
// Valid values for pWhichOutput are 0-3. These will be converted to 1-4
// before transmitting to the module.
//

@Override
public void flipAnalogOutput(int pWhichOutput)
{
    
    double value = channels[pWhichOutput].outputValue;
    
    if (value == MIN_OUTPUT_VALUE){ value = MAX_OUTPUT_VALUE; }
    else if (value == MAX_OUTPUT_VALUE){ value = MIN_OUTPUT_VALUE; }
    else { value = MIN_OUTPUT_VALUE; }    
    
    setOutput(pWhichOutput, value);    

}//end of X_317_AnalogOutputModule::flipAnalogOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::setOutput
//
// Sets the output pOutputNum's value to pValue.
//
// Valid values for pOutputNum are 0-3. These will be converted to 1-4
// before transmitting to the module.
//
//  "raw:XMLNoHeader//" + moduleIPAddress + "/state.xml?an1State=1";
//

@Override
public void setOutput(int pOutputNum, double pValue)
{
        
    String controlURL;
    
    channels[pOutputNum].outputValue = pValue;
    
    controlURL = xmlBaseURL + "an" + (pOutputNum+1) + "State=" + 
                               decimalFormat.format(pValue) + xmlBaseURLSuffix;

    connectSendGetRequestClose(controlURL, false);

}//end of X_317_AnalogOutputModule::setOutput
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::setOutputWithMinMaxPeakHold
//
// Stores the peak min and max values from pValue so they are not lost if not
// ready to send a new data point.
//
// If ready, alternates between setting output pOutputNum's value to the
// min or the max so both are represented.
//
// Valid values for pOutputNum are 0-3. These will be converted to 1-4
// before transmitting to the module.
//
//  "raw:XMLNoHeader//" + moduleIPAddress + "/state.xml?an1State=1";
//

@Override
public void setOutputWithMinMaxPeakHold(int pOutputNum, double pValue)
{
        
    String controlURL;
    
    IOModuleChannel ch = channels[pOutputNum];
    
    if (pValue > ch.maxValue){ ch.maxValue = pValue; }
    if (pValue < ch.minValue){ ch.minValue = pValue; }

     //do nothing if send recently performed
    if (recentConnectionMade) { return; }
    
    //alternate between sending min and max values
    ch.outputValue = ch.minMaxFlip ? ch.minValue : ch.maxValue;

    controlURL = xmlBaseURL + "an" + (pOutputNum+1) + "State=" + 
                        decimalFormat.format(ch.outputValue)+xmlBaseURLSuffix;

    if (connectSendGetRequestClose(controlURL, false) != null){ 

      //if send successful, clear the min/max value and flip to opposite        
        
      if(ch.minMaxFlip){ ch.minValue = Double.MAX_VALUE; }
      else { ch.maxValue = Double.MIN_VALUE; }
      
      ch.minMaxFlip = !ch.minMaxFlip;
        
    }

}//end of X_317_AnalogOutputModule::setOutputWithMinMaxPeakHold
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::getLinkedChannel
//
// Returns the sensor input channel with which the module channel pChannel is
// linked.
//
// If the linked channel is -1, then no channel is linked.
//

@Override
public int getLinkedChannel(int pChannel)
{
    
    return(channels[pChannel].linkedChannel);

}//end of X_317_AnalogOutputModule::getLinkedChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::configure
//
// Loads configuration settings from pConfigFile.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

    String section = "IO Module " + moduleNumber;
    
    for(int i=0; i<numAnalogOutputChannels; i++){
    
        channels[i] = new IOModuleChannel();
        
        channels[i].outputMode = parseOutputMode(
          pConfigFile.readString(section, "Output " + i + " mode", "4-20mA"));
        
        channels[i].linkedChannel = pConfigFile.readInt(
                        section, "Output " + i + " is Output for Channel", -1);        
    }

}//end of X_317_AnalogOutputModule::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// X_317_AnalogOutputModule::parseOutputMode
//
// Parses pString into one of the allowable modes.
//

int parseOutputMode(String pString)
{

    if (pString.equalsIgnoreCase("4-20mA")) {
        return(IOModuleChannel.CURRENT_LOOP_4_20MA);
    }
    else
    if (pString.equalsIgnoreCase("0-5V")) {
        return(IOModuleChannel.VOLTAGE_0_5V);
    }
    else
    if (pString.equalsIgnoreCase("+/-5V")) {
        return(IOModuleChannel.VOLTAGE_PLUS_MINUS_5V);
    }
    else
    if (pString.equalsIgnoreCase("+/-10V")) {
        return(IOModuleChannel.VOLTAGE_PLUS_MINUS_10V);
    }
    else{        
        return(IOModuleChannel.CURRENT_LOOP_4_20MA);
    }
  
}//end of X_317_AnalogOutputModule::parseOutputMode
//-----------------------------------------------------------------------------

}//end of class X_317_AnalogOutputModule
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
    