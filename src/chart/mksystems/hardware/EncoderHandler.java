/******************************************************************************
* Title: EncoderHandler.java
* Author: Mike Schoonover
* Date: 11/16/16
*
* Purpose:
*
* This class handles encoder inputs and distance conversions. It is a parent
* class for child classes which handle different encoder arrangements, such
* as single linear encoder / single rotational encoder on a trolley or dual
* linear encoders on a helical or non-helical pass-through unit.
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
// class EncoderHandler
//
//

public class EncoderHandler extends Object{
    
    
    EncoderValues encVals;

    public int encoder1, prevEncoder1;
    public int encoder2, prevEncoder2;

    // specifies if last change of the encoder count was increasing or
    //decreasing
    public int encoder1Dir = INCREASING;
    public int encoder2Dir = INCREASING;

    //debug mks -- needs to be loaded from config file -- specifies if carriage
    //moving away is increasing or decreasing encoder counts
    public int awayDirection = 0;
        
    // specifies if increasing or decreasing encoder count is the forward
    // direction this alternates depending on which end the carriage starts
    // from and is determined by the encoder direction when the inspection of a
    //new piece starts
    public int encoder2FwdDir;

    //encoder values at start of piece inspection
    public int encoder1Start, encoder2Start;

    double measuredLengthFt;

    public final static int INCREASING = 0, DECREASING = 1;    
    
//-----------------------------------------------------------------------------
// EncoderHandler::EncoderHandler (constructor)
//

public EncoderHandler(EncoderValues pEncVals)
{

    encVals = pEncVals;
    
}//end of EncoderHandler::EncoderHandler (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of EncoderHandler::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::calculateTally
//
// Calculates the length of the tube.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public double calculateTally()
{

    measuredLengthFt = 0.0;
    
    return(measuredLengthFt);

}//end of EncoderHandler::calculateTally
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::calculateTruncatedTally
//
// Calculates the length of the tube which has passed by the first sensor
// thus far.
//
// This is NOT the total tally of the tube. See notes for each child class for
// details on this method.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public double calculateTruncatedTally()
{

    measuredLengthFt = 0.0;
    
    return(measuredLengthFt);

}//end of EncoderHandler::calculateTruncatedTally
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::recordLinearStartCount
//
// Records the current count of the encoder tracking the linear position.
//
//(this needs so be changed to store the value with each piece for
// future units which might have multiple pieces in the system at
// once)
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public void recordLinearStartCount()
{

    encoder2Start = encoder2;
    
}//end of EncoderHandler::recordLinearStartCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::getDirectionSetForLinearForward
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
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public int getDirectionSetForLinearFoward()
{

    return(Integer.MAX_VALUE);
    
}//end of EncoderHandler::getDirectionSetForLinearFoward
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::getAbsValueLinearDistanceMovedInches
//
// Returns the absolute value of the total distance moved in inches since the
// linear start count was recorded. Returns absolute value so head moving in
// reverse works the same as forward.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public double getAbsValueLinearDistanceMovedInches()
{
    
    return(0.0);

}//end of EncoderHandler::getAbsValueLinearDistanceMovedInches
//-----------------------------------------------------------------------------

}//end of class EncoderHandler
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
