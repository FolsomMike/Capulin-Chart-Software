/******************************************************************************
* Title: TraceDatum.java
* Author: Mike Schoonover
* Date: 7/9/13
*
* Purpose:
*
* This class handles data from a single buffer position. It includes the
* data point itself and other related information such as clock position and
* flags.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.stripchart;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class TraceDatum
//
// This class handles data from a single buffer position.
//

public class TraceDatum extends PlotterDatum{

    public int newData1;
    public int newData2;
    public int prevData1;
    public int prevData2;
    public int flags;

    public int peak;
    public int index;

}//end of class TraceDatum
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------