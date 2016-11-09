/******************************************************************************
* Title: EncoderCalValues.java
* Author: Mike Schoonover
* Date: 9/16/16
*
* Purpose:
*
* This class handles variables related calibrating the encoders.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import java.util.ArrayList;

// class EncoderCalValues
//
//

public class EncoderCalValues extends Object{
  
    public double encoder1CountsPerInch = 0; 
    public double encoder1InchesPerCount = 0;
    public double encoder1CountsPerRev = 0;    
    public double encoder1CountsPerSec = 0;
    public double encoder1Helix = 0;

    public double encoder2CountsPerInch = 0; 
    public double encoder2InchesPerCount = 0;
    public double encoder2CountsPerRev = 0;        
    public double encoder2CountsPerSec = 0;
    public double encoder2Helix = 0;    

    public int numEntryJackStands = 0;
    public int numExitJackStands = 0;    
    public String textMsg = "";
    
    ArrayList<SensorData> sensorData;

//-----------------------------------------------------------------------------
// EncoderCaValues::EncoderCalValues (constructor)
//

public EncoderCalValues()
{


}//end of EncoderCalValues::EncoderCalValues (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalValues::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of EncoderCalValues::init
//-----------------------------------------------------------------------------

}//end of class EncoderCalValues
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
