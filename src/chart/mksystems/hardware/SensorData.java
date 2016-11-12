/******************************************************************************
* Title: SensorData.java
* Author: Mike Schoonover
* Date: 11/08/16
*
* Purpose:
*
* This class handles variables related to a sensor set. It provides variables
* for distance to a jack stand, its two sensors, and various related data.
* It can also be used to hold data for a single sensor, in which case the
* variables for the jack stand and the second sensor are ignored.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------
// class SensorData
//
//

public class SensorData extends Object{
    
    public int sensorDataNum = 0;
    public String pTitle = "";
    
    //Eye1 is the entry/start inspection eye
    public double jackCenterDistToEye1 = 0; 
    public double eyeADistToJackCenter = 0;
    public double eyeBDistToJackCenter = 0;

    public int lastEyeChanged=UNDEFINED_EYE;
    
    public int lastState = UNDEFINED_STATE;
    public int eyeAState = UNDEFINED_STATE;
    public int eyeBState = UNDEFINED_STATE;    
    
    //last encoder count reported for either eye
    public int lastEncoder1Cnt = Integer.MAX_VALUE;
    public int lastEncoder2Cnt = Integer.MAX_VALUE;        
    
    public int eyeAEncoder1Cnt = Integer.MAX_VALUE;
    public int eyeAEncoder2Cnt = Integer.MAX_VALUE;    
    public int eyeBEncoder1Cnt = Integer.MAX_VALUE;    
    public int eyeBEncoder2Cnt = Integer.MAX_VALUE;
 
    public int direction = UNDEFINED_DIR;
    public int sensorNum = Integer.MAX_VALUE;
    public double countsPerInch = Double.MAX_VALUE;
    public double percentChange = Double.MAX_VALUE;

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
// SensorData::SensorData (constructor)
//

public SensorData(int pSensorDataNum)
{

    sensorDataNum = pSensorDataNum;
    
}//end of SensorData::SensorData (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

     resetAll();

}//end of SensorData::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::resetAll
//
// Resets all values to default.
//

public void resetAll()
{

    lastEyeChanged=UNDEFINED_EYE;
    lastState = UNDEFINED_STATE;
    eyeAState = UNDEFINED_STATE;
    eyeBState = UNDEFINED_STATE;    

    lastEncoder1Cnt = Integer.MAX_VALUE;
    lastEncoder2Cnt = Integer.MAX_VALUE;        

    eyeAEncoder1Cnt = Integer.MAX_VALUE;
    eyeAEncoder2Cnt = Integer.MAX_VALUE;    
    eyeBEncoder1Cnt = Integer.MAX_VALUE;    
    eyeBEncoder2Cnt = Integer.MAX_VALUE;

    direction = UNDEFINED_DIR;
    sensorNum = Integer.MAX_VALUE;
    countsPerInch = Double.MAX_VALUE;
    percentChange = Double.MAX_VALUE;

}//end of SensorData::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::setStateAndEncoderCounts
//
// Stores the pState and pEncoder*Cnt values in the lastState and 
// lastEncoder*Cnt variables and in the eye*State and eye*Encoder*Cnt variables
// specific to the value in lastEyeChanged.
//
// Variable lastEyeChanged should already be set before calling.
//

public void setEncoderCounts(int pState, int pEncoder1Cnt, int pEncoder2Cnt)
{

    lastState = pState;
    lastEncoder1Cnt = pEncoder1Cnt; lastEncoder2Cnt = pEncoder2Cnt;
    
        switch (lastEyeChanged) {
        case EYE_A :
                eyeAState = pState;
                eyeAEncoder1Cnt = pEncoder1Cnt; eyeAEncoder2Cnt = pEncoder2Cnt;
            break;
        case EYE_B:
                eyeBState = pState;            
                eyeBEncoder1Cnt = pEncoder1Cnt; eyeBEncoder2Cnt = pEncoder2Cnt;            
            break;
    }

}//end of SensorData::setEncoderCounts
//-----------------------------------------------------------------------------

}//end of class SensorData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
