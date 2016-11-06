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
* It provides a text marker message for sending to a remote marker control
* device which instructs which marker to fire and the distance to the marker.
* Each violation object can fire multiple markers, but the marker message only
* holds distance information for the marker designated as the Primary Marker.
* If multiple markers are fired, they will fire at the same time as the Primary
* Marker.
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
    double distanceToMarkerInInches = -1;
    int encoderCountsToMarker = -1;
    
    //string loaded from config file specifying which markers are to be fired
    //when a signal in the gate exceeds a threshold on the chart
    //byte parsed from markersTriggeredS which specifies which markers are
    //fired...each bit controls a separate marker bit0:marker1,1:2,2:3, etc.

    String markersTriggeredList = "";
    byte markersTriggered = 0;
    
    int primaryMarker = 0;
    
    public int getMarkersTriggered(){ return markersTriggered; }
    public int getPrimaryMarker(){ return primaryMarker; }
        
    String markerMessage = "";
    
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
// The values pHeadNum and pChannelNum are passed in by the owner object to be
// stored as additional configuration info.
// 

public void configure(IniFile pConfigFile, String pSection, int pHeadNum,
                                                                int pChannelNum)
{

    headNum = pHeadNum;
    channelNum = pChannelNum;
    
    orientation = pConfigFile.readString(pSection, "Orientation", "???");

    markersTriggeredList = pConfigFile.readString(
                                          pSection, "Markers Triggered", "0");
    
    // parse string to set the bits in a byte to specify which markers are to
    //be fired when the signal in the gate exceeds a chart threshold

    markersTriggered = parseListToBits(markersTriggeredList);

    primaryMarker = pConfigFile.readInt(pSection, "Primary Marker", 0) - 1;
    
}//end of ViolationInfo::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViolationInfo::createMarkerMessage
//
// Creates a string formatted for transmission to a remote device such as a PLC
// which contains all information related to the marking of an anomaly.
//
// Creating the string in advance saves time over creating it each time it is
// needed and all the values are unchanging.
//
// Note that the string only specifies the distance to the primary marker.
// If multiple markers are fired, they will all be fired at the same time as
// the primary marker.
//

public void createMarkerMessage(int pEncoderCountsToMarker, 
                                               double pDistanceToMarkerInInches)
{
    
    encoderCountsToMarker = pEncoderCountsToMarker;
    
    distanceToMarkerInInches = pDistanceToMarkerInInches;
   
    markerMessage = "^*" + postPad(orientation, 3, " ");
            
    markerMessage = markerMessage + "|" + prePad("" + headNum, 2, "0");
    
    markerMessage = markerMessage + "|" + prePad("" + channelNum, 3, "0");
 
    //convert distance to tenths of an inch
    int tenths = (int) Math.round(distanceToMarkerInInches * 10);
    
    markerMessage = markerMessage + "|" + prePad("" + tenths, 4, "0");
    
    markerMessage = markerMessage + "|" + 
                                  prePad("" + encoderCountsToMarker, 5, "0");
    
    markerMessage = markerMessage + "|" + 
                                  prePad("" + markersTriggered, 3, "0");
    
}//end of ViolationInfo::createMarkerMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViolationInfo::getMarkerMessage
//
// Returns a string containing info related to marking anomalies.
//

public String getMarkerMessage()
{
    
    return(markerMessage);

}//end of ViolationInfo::getMarkerMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViolationInfo::prePad
//
// Pads pString to be at least pLength by prepending the required number of
// characters specified by pPadChar.
// 
// Returns the padded string.
//

private String prePad(String pString, int pLength, String pPadChar)
{
        
    while(pString.length() < pLength){ pString = pPadChar + pString; }
    
    return(pString);
        
}//end of ViolationInfo::prePad
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViolationInfo::postPad
//
// Pads pString to be at least pLength by appending the required number of
// characters specified by pPadChar.
// 
// Returns the padded string.
//

private String postPad(String pString, int pLength, String pPadChar)
{
        
    while(pString.length() < pLength){ pString = pString + pPadChar; }
    
    return(pString);
        
}//end of ViolationInfo::postPad
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
