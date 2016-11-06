/******************************************************************************
* Title: Marker.java
* Author: Mike Schoonover
* Date: 10/29/16
*
* Purpose:
*
* This class handles variables and actions related to markers which mark the
* tube at the position of detected anomalies.
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
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
// class Marker
//

public class Marker extends Object{


    int markerNum;
    
    double timeAdjustSpeedRatio = 0;

    double photoEye1DistanceInInches = 0;
    int angularPositionInDegrees = 0;

public double getPhotoEye1DistanceInInches(){return photoEye1DistanceInInches;}
public int getAngularPositionInDegrees(){return angularPositionInDegrees;}    

//-----------------------------------------------------------------------------
// Marker::Marker (constructor)
//

public Marker(int pMarkerNum)
{

    markerNum = pMarkerNum;

}//end of Marker::Marker (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Marker::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{
    
}//end of Marker::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Marker::configure
//
// Loads configuration settings from the configuration.ini file.
//
// Only configuration data for this class are loaded here.  Each
// child object should be allowed to load its own data.
//

public void configure(IniFile pConfigFile)
{
    
    String markerNumText = "Marker " + (markerNum+1);
    
    timeAdjustSpeedRatio = pConfigFile.readDouble(
                   "Markers" , markerNumText+ " Time Adjust Speed Ratio", 0.0);
    
    photoEye1DistanceInInches = pConfigFile.readDouble(
          "Markers", markerNumText + " to Photo Eye 1 Distance in Inches", 0);

    angularPositionInDegrees = pConfigFile.readInt(
        "Markers", markerNumText + " Angular Position From TDC in Degrees", 0);    
    
}//end of Marker::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Marker::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Marker::logSevere
//-----------------------------------------------------------------------------

}//end of class Marker
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
