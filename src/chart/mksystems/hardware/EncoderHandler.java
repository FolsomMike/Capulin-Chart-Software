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

import javax.swing.JLabel;



//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EncoderHandler
//
//

public class EncoderHandler extends Object{
    
    
    EncoderValues encVals;

    public int encoder1 = 0, prevEncoder1 = 0;
    public int encoder2 = 0, prevEncoder2 = 0;

    double linearDistanceMovedInches;
    double linearDistanceMovedInchesCorrected;
        
    // specifies if last change of the encoder count was increasing or
    //decreasing
    public int encoder1Dir = INCREASING;
    public int encoder2Dir = INCREASING;

    JLabel msgLabel;

    String msg = "";
    
    //debug mks -- needs to be loaded from config file -- specifies if carriage
    //moving away from home is increasing or decreasing encoder counts
    private final int awayDirection = INCREASING;
        
    public int getAwayDirection(){ return(awayDirection); }
    
    // specifies if increasing or decreasing encoder count is the forward
    // direction this alternates depending on which end the carriage starts
    // from and is determined by the encoder direction when the inspection of a
    //new piece starts
    int encoder1FwdDir, encoder2FwdDir;

    //encoder values at start of piece inspection
    public int encoder1Start, encoder2Start;

    double measuredLengthFt;

    double linearPositionCorrection;
    
    public final static int INCREASING = 0, DECREASING = 1;    
    
//-----------------------------------------------------------------------------
// EncoderHandler::EncoderHandler (constructor)
//

public EncoderHandler(EncoderValues pEncVals, JLabel pMsgLabel)
{

    encVals = pEncVals; msgLabel = pMsgLabel;
    
}//end of EncoderHandler::EncoderHandler (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public void init()
{


}//end of EncoderHandler::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::displayMsg
//
// Displays a message on the msgLabel using a threadsafe method.
//
// There is no bufferering, so if this function is called again before
// invokeLater calls displayMsgThreadSafe, the prior message will be
// overwritten.
//

public void displayMsg(String pMessage)
{

    msg = pMessage;

    javax.swing.SwingUtilities.invokeLater(this::displayMsgThreadSafe);    

}//end of EncoderHandler::displayMsg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::displayMsgThreadSafe
//
// Displays a message on the msgLabel and should only be called from
// invokeLater.
//

public void displayMsgThreadSafe()
{

    msgLabel.setText(msg);
    
}//end of EncoderHandler::displayMsgThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::resetAll
//
// Resets all values to default.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//
// NOTE: child classes should call this function.
//

public void resetAll()
{

    //do NOT reset encoder1, prevEncoder1, encoder2, prevEncoder2 as they
    //contain values matching those in the Control Board
    
    encoder1Dir = INCREASING; encoder2Dir = INCREASING;
    encoder1Start = 0; encoder2Start = 0;
    measuredLengthFt = 0.0;
    linearPositionCorrection = 0.0;

    linearDistanceMovedInches = 0.0;
    linearDistanceMovedInchesCorrected = 0.0;

}//end of EncoderHandler::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::configure
//
// Configures values which depend on data in other objects which must first
// load settings from the config file.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public void configure()
{

}//end of EncoderHandler::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::setCurrentLinearDirectionAsFoward
//
// Sets the direction last detected as Forward for whichever encoder(s) are
// used for tracking linear position.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public void setCurrentLinearDirectionAsFoward()
{

}//end of EncoderHandler::setCurrentLinearDirectionAsFoward
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

//-----------------------------------------------------------------------------
// EncoderHandler::handleEncoderSwitchOver
//
// For systems with multiple linear tracking encoders, this function handles
// switching from the first encoder to the second encoder once the tube has
// passed the second encoder. It also handles switching back to the first
// encoder in case the tube is reversed.
//
// Should be overridden by child classes to provide custom handling based on
// the encoder configuration.
//

public void handleEncoderSwitchOver()
{

}//end of EncoderHandler::handleEncoderSwitchOver
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::handleLinearPositionOverride
//

public void handleLinearPositionOverride(double pOverride)
{
  
}//end of EncoderHandler::handleLinearPositionOverride
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderHandler::allowTraceUpdate
//
// Determines if the trace should be updated based on the number and sign
// of the pixels moved value.
//
// Each subclass may handle differently: some may disallow trace reversal for
// instance.
//

public boolean allowTraceUpdate(int pPixelsMoved)
{

    return (true);

}//end of EncoderHandler::handleLinearPositionOverride
//-----------------------------------------------------------------------------

}//end of class EncoderHandler
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
