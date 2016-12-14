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
    public int sensorGroup;
    public String pTitle = "";

    public static double eye1DistToEntryJackMeasurePoint;
    public static double eye1DistToExitJackMeasurePoint;    
    
    //Eye1 is the entry/start inspection eye
    private double jackCenterDistToMeasurePoint;
    public double getJackCenterDistToMeasurePoint(){
                                        return (jackCenterDistToMeasurePoint);}
    
    private double jackCenterDistToEye1 = 0;
    private double eyeADistToJackCenter = 0;
    public double getEyeADistToJackCenter(){ return eyeADistToJackCenter; }
    private double eyeADistToEye1 = 0;
    private double eyeBDistToJackCenter = 0;
    public double getEyeBDistToJackCenter(){ return eyeBDistToJackCenter; }    
    private double eyeBDistToEye1 = 0;
    
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
    public static final int ENTRY_GROUP = 0;
    public static final int EXIT_GROUP = 1;
    public static final int UNIT_GROUP = 2;

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

public SensorData(int pSensorDataNum, int pSensorGroup)
{

    sensorDataNum = pSensorDataNum; sensorGroup = pSensorGroup;
    
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

//-----------------------------------------------------------------------------
// SensorData::setJackCenterDistToMeasurePoint
//
// Sets the jackCenterDistToMeasurePoint variable and uses it to calculate
// the jackCenterDistToEye1 distance.
//

public void setJackCenterDistToMeasurePoint(double pValue)
{

    jackCenterDistToMeasurePoint = pValue;
    
    updatedAllDistancesToEye1();
    
}//end of SensorData::setJackCenterDistToMeasurePoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::setEyeADistToJackCenter
//
// Sets the eyeADistToJackCenter and recalculates all distances to Eye 1.
//

public void setEyeADistToJackCenter(double pValue)
{

    eyeADistToJackCenter = pValue;
    
    updatedAllDistancesToEye1();
    
}//end of SensorData::setEyeADistToJackCenter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::setEyeBDistToJackCenter
//
// Sets the eyeBDistToJackCenter and recalculates all distances to Eye 1.
//

public void setEyeBDistToJackCenter(double pValue)
{

    eyeBDistToJackCenter = pValue;
    
    updatedAllDistancesToEye1();
    
}//end of SensorData::setEyeBDistToJackCenter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::updatedAllDistancesToEye1
//
// Calculates the distance from each jack center and eye to eye1.
//

public void updatedAllDistancesToEye1()
{

    double eye1DistToMeasurePoint;
    double eyeADistToJackCenterSign;
    double eyeBDistToJackCenterSign;    
    
    switch(sensorGroup){
        case ENTRY_GROUP:
            eye1DistToMeasurePoint = eye1DistToEntryJackMeasurePoint;
            eyeADistToJackCenterSign = 1; eyeBDistToJackCenterSign = -1;
        break;
        case EXIT_GROUP:
            eye1DistToMeasurePoint = eye1DistToExitJackMeasurePoint;
            eyeADistToJackCenterSign = -1; eyeBDistToJackCenterSign = 1;
        break;
        case UNIT_GROUP:
            eye1DistToMeasurePoint = 0;
            eyeADistToJackCenterSign = 0; eyeBDistToJackCenterSign = 0;
            //debug mks -- this value should be different for each eye in
            //the Unit group...entry eye is 0, etc. (load from config?)
            jackCenterDistToEye1 = 56.5;
            return;

        default:
            eye1DistToMeasurePoint = 0;
            eyeADistToJackCenterSign = 0; eyeBDistToJackCenterSign = 0;            
        break;
    }
    
    jackCenterDistToEye1 = 
                        jackCenterDistToMeasurePoint + eye1DistToMeasurePoint;

    eyeADistToEye1 = jackCenterDistToEye1 +
                            (eyeADistToJackCenter * eyeADistToJackCenterSign);

    eyeBDistToEye1 = jackCenterDistToEye1 +
                            (eyeBDistToJackCenter * eyeBDistToJackCenterSign);

}//end of SensorData::updatedAllDistancesToEye1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorData::getEyeDistToEye1
//
// Returns the distance to eye 1 for the sensor set eye specified by pEye.
//
// Eye 1 is the entry/start inspection eye.
//
// Returns Integer.MAX_VALUE if EYE_A or EYE_B not specified.
//

public double getEyeDistToEye1(int pEye)
{        
 
    switch(pEye){
        
        case EYE_A:
            return(eyeADistToEye1);
            
        case EYE_B:
            return(eyeBDistToEye1);

        case SELF:
            return(jackCenterDistToEye1);
            
        default:
            return(Integer.MAX_VALUE);
    }
    
}//end of SensorData::getEyeDistToEye1
//-----------------------------------------------------------------------------
        
        
}//end of class SensorData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
