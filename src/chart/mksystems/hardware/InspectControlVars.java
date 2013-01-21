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

    public int encoder1, prevEncoder1;
    public int encoder2, prevEncoder2;

    //encoder2 (linear tracking) value at start of piece inspection
    public int encoder2Start;

    public static int INCREASING = 0, DECREASING = 1;

    // specifies if last change of the encoder count was increasing or
    //decreasing
    public int encoder1Dir = INCREASING;
    public int encoder2Dir = INCREASING;

    // specifies if increasing or decreasing encoder count is the forward
    // direction this alternates depending on which end the carriage starts
    // from and is determined by the encoder direction when the inspection of a
    //new piece starts
    public int encoder2FwdDir;

    public boolean onPipeFlag = false;
    public boolean head1Down = false;
    public boolean head2Down = false;
    public boolean tdcFlag = false;

}//end of class InspectControlVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
