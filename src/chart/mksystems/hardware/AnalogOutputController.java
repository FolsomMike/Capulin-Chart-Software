/******************************************************************************
* Title: AnalogOutputController.java
* Author: Mike Schoonover
* Date: 11/10/15
*
* Purpose:
*
* This file contains the interface definition for AnalogOutputController.  This
* interface provides functions required by all devices which handle analog
* outputs.
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
//-----------------------------------------------------------------------------
// interface AnalogOutputController
//

public interface AnalogOutputController {

    public boolean isAnalogOutputController();
    
    public void setOutput(int pWhichOutput, double pValue);
    
    public void setOutputWithMinMaxPeakHold(int pWhichOutput, double pValue);

}//end of interface AnalogOutputController
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
