/******************************************************************************
* Title: EncoderLinearAndRotational.java
* Author: Mike Schoonover
* Date: 11/16/16
*
* Purpose:
*
* This class handles encoder inputs and distance conversions. It inherits from
* EncoderHandler. This child class handles units with a rotational encoder
* for tracking spinning pipe and a linear encoder tracking a trolley.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EncoderLinearAndRotational
//
//

public class EncoderLinearAndRotational extends EncoderHandler{
    
                
//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::EncoderLinearAndRotational (constructor)
//

public EncoderLinearAndRotational(EncoderValues pEncVals)
{

    super(pEncVals);
    
}//end of EncoderLinearAndRotational::EncoderLinearAndRotational (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    super.init();

}//end of EncoderLinearAndRotational::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::resetAll
//
// Resets all values to default.
//
// NOTE: child classes should call this function.
//

@Override
public void resetAll()
{

    super.resetAll();
    
}//end of EncoderLinearAndRotational::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::setCurrentLinearDirectionAsFoward
//
// Sets the direction designated as Forward for whichever encoder(s) are
// used for tracking linear position
//

@Override
public void setCurrentLinearDirectionAsFoward()
{
    
    encoder2FwdDir = encoder2Dir;
    
}//end of EncoderLinearAndRotational::setCurrentLinearDirectionAsFoward
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::calculateTally
//
// Calculates the length of the tube.
//

@Override
public double calculateTally()
{

    super.calculateTally();

    //calculate number counts recorded between start/stop eye triggers
    int pieceLengthEncoderCounts =
     Math.abs(encoder2 - encoder2Start);

    //convert to inches
    measuredLengthFt =
              encVals.convertEncoder2CountsToInches(pieceLengthEncoderCounts);

    //subtract the distance between the perpendicular eyes -- tracking
    //starts when lead eye hits pipe but ends when trailing eye clears,
    //so the extra distance between the eyes must be accounted for...
    //also subtract the length of the end stop which will be non-zero
    //for systems where the away laser triggers on that instead of the
    //end of the tube

    measuredLengthFt = measuredLengthFt
        - encVals.photoEyeToPhotoEyeDistance - encVals.endStopLength;

    measuredLengthFt /= 12.0; //convert to decimal feet

    return(measuredLengthFt);
    
}//end of EncoderLinearAndRotational::calculateTally
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::calculateTruncatedTally
//
// Calculates the length of the tube which has passed by the first sensor
// thus far.
//
// Uses encoder 2 which tracks the linear position of the trolley.
//
// This is NOT the total tally of the tube.
//
// The distance between the vertical eyes is not accounted for when calculating
// the distance traveled thus far -- the trailing photo-eye may not even have
// reached the end of the test piece.
//

@Override
public double calculateTruncatedTally()
{
    
    //calculate number counts recorded between start/stop eye triggers
    int pieceLengthEncoderCounts = Math.abs(encoder2 - encoder2Start);

    //convert to inches
    measuredLengthFt =
                encVals.convertEncoder2CountsToFeet(pieceLengthEncoderCounts);

    return(measuredLengthFt);

}//end of EncoderLinearAndRotational::calculateTruncatedTally
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::getDirectionSetForLinearForward
//
// Returns the direction of linear travel which is currently defined as 
// "forward" or "reverse" for whichever encoder(s) is handling linear tracking.
//
// Trolley units can inspect in both directions, so the concept of "forward"
// and "reverse" are flipped depending on the end from which the trolley
// started.
//
// Pass through units generally have the same direction for both encoders.
//

@Override
public int getDirectionSetForLinearFoward()
{

    return(encoder2FwdDir);
    
}//end of EncoderLinearAndRotational::getDirectionSetForLinearFoward
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::recordLinearStartCount
//
// Records the current count of the encoder tracking the linear position.
//

@Override
public void recordLinearStartCount()
{

    encoder2Start = encoder2;
    
}//end of EncoderLinearAndRotational::recordLinearStartCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderLinearAndRotational::getAbsValueLinearDistanceMovedInches
//
// Returns the absolute value of the total distance moved in inches since the
// linear start count was recorded. Returns absolute value so head moving in
// reverse works the same as forward. 
//

@Override
public double getAbsValueLinearDistanceMovedInches()
{
    
    double position = 
               encVals.convertEncoder2CountsToInches(encoder2 - encoder2Start);

    return(Math.abs(position));

}//end of EncoderLinearAndRotational::getAbsValueLinearDistanceMovedInches
//-----------------------------------------------------------------------------


}//end of class EncoderLinearAndRotational
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
