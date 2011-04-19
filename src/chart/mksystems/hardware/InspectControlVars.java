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

public boolean encoder1FwdDirection = false;
public boolean encoder2FwdDirection = false;

public boolean onPipeFlag = false;
public boolean inspectFlag = false;
public boolean tdcFlag = false;

}//end of class InspectControlVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
