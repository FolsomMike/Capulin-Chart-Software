/******************************************************************************
* Title: ViolationInfo.java
* Author: Mike Schoonover
* Date: 9/11/16
*
* Purpose:
*
* This class encapsulates variables related to violation events, such as
* exceeding a threshold or other cases which require alarm or marker firing.
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

import chart.mksystems.inifile.IniFile;

// class ViolationInfo
//

public class ViolationInfo extends Object{

    String orientation = "";
    int headNum = -1;
    int channelNum = -1;
    double distanceToMarker = -1;
    int encoderCountsToMarker = -1;
    
    //string loaded from config file specifying which markers are to be fired
    //when a signal in the gate exceeds a threshold on the chart
    //byte parsed from markersTriggeredS which specifies which markers are
    //fired...each bit controls a separate marker bit0:marker1,1:2,2:3, etc.

    String markersTriggeredList = "";
    byte markersTriggered = 0;
    
//-----------------------------------------------------------------------------
// ViolationInfo::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{        

          
}//end of ViolationInfo::init
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// ViolationInfo::configure
//
// Loads configuration settings from section pSection in the file pConfigFile.
//
// The values pHeadNum, pChannelNum, and pEncoderCountsToMarker are passed in
// by the owner object to be stored as additional configuration info.
// 

public void configure(IniFile pConfigFile, String pSection, int pHeadNum,
                                int pChannelNum, int pEncoderCountsToMarker)
{

    headNum = pHeadNum;
    channelNum = pChannelNum;
    encoderCountsToMarker = pEncoderCountsToMarker;
        
    orientation = pConfigFile.readString(pSection, "Orientation", "???");

    markersTriggeredList = pConfigFile.readString(
                                          pSection, "Markers Triggered", "0");
    
    // parse string to set the bits in a byte to specify which markers are to
    //be fired when the signal in the gate exceeds a chart threshold

    markersTriggered = parseListToBits(markersTriggeredList);
    
}//end of ViolationInfo::configure
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// ViolationInfo::parseListToBits
//
// Parses the comma delimited list in string pText to set the bits in the
// return byte.
//
// Input text format examples (up to 8 bits: 1-8):
//
//  0               (no bits set)
//  1               (bit 1 set)
//  1,2             (bits 1&2 set)
//  1,2,3,4,5,6,7,8 (all bits set)
//
// Return byte format:
//
// bit 0: 1 if 1 listed in input text
// bit 1: 1 if 2 listed in input text
// ...
// bit 7: 1 if 8 listed in input text
//

private byte parseListToBits(String pText)
{

    byte result = 0;
    
    String[] split = pText.split(",");
    
    if(split.length > 0){
     
        for (String bitNumS : split) {
            try {
                
                int bitNum = Integer.parseInt(bitNumS.trim());
                
                if (bitNum > 0){
                    result += Math.pow(2, bitNum - 1);
                }
            }catch(NumberFormatException e){
                //ignore value if invalid
            }
        }
    }

    return(result);
    
}//end of ViolationInfo::parseListToBits
//-----------------------------------------------------------------------------


}//end of class ViolationInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
