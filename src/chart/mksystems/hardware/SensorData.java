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

    public int lastEyeChanged=0;
    public int sensorState = UNDEFINED_STATE;
    public int encoder1Cnt = Integer.MAX_VALUE, encoder2Cnt = Integer.MAX_VALUE;
    public int direction = UNDEFINED_DIR;
    public int sensorNum = Integer.MAX_VALUE;
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


}//end of SensorData::init
//-----------------------------------------------------------------------------




}//end of class SensorData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
