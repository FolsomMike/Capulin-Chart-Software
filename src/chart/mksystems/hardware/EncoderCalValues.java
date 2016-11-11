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
    
    public boolean sensorTransitionDataChanged = false;
  
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
    
    public ArrayList<SensorData> sensorData;

    public static final int MAX_NUM_UNIT_SENSORS = 3;
    
    public static final int NUM_UNIT_SENSORS = 3;
    
    public static final int MAX_NUM_JACKS_ON_EITHER_END = 10;
    
    public static final int TOTAL_NUM_SENSORS = 
                     MAX_NUM_JACKS_ON_EITHER_END * 2 + MAX_NUM_UNIT_SENSORS;

    public static final int UNDEFINED_GROUP = -1;
    public static final int INCOMING = 0;
    public static final int OUTGOING = 1;
    public static final int UNIT = 2;

    public static final int UNDEFINED_EYE = -1;
    public static final int EYE_A = 0;
    public static final int EYE_B = 1;
    public static final int SELF = 2;
    
    public static final int UNDEFINED_DIR = -1;
    public static final int STOPPED = 0;
    public static final int FWD = 1;
    public static final int REV = 2;

    public static final int UNDEFINED_STATE = -1;    
    public static final int UNBLOCKED = 0;
    public static final int BLOCKED = 1;
                
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
