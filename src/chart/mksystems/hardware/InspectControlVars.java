/******************************************************************************
* Title: InspectControlVars.java
* Author: Mike Schoonover
* Date: 4/18/11
*
* Purpose:
*
* This class encapsulates variables related to the Encoder and inspection
* control signals.
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
// class InspectControlVars
//

public class InspectControlVars extends Object{

    public EncoderHandler encoderHandler;    
    
    public boolean onPipeFlag = false;
    public boolean head1Down = false;
    public boolean head2Down = false;
    public boolean head3Down = false;    
    public boolean tdcFlag = false;

//-----------------------------------------------------------------------------
// InspectControlVars::InspectControlVars (constructor)
//

public InspectControlVars(EncoderHandler pEncoderHandler)
{

    encoderHandler = pEncoderHandler;
    
}//end of InspectControlVars::InspectControlVars (constructor)
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// InspectControlVars::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of InspectControlVars::init
//-----------------------------------------------------------------------------
    
}//end of class InspectControlVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
