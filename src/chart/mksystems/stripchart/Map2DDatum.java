/******************************************************************************
* Title: Map2DDatum.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This class handles data from a single buffer position of a 2D map. It includes
* the data points and other related information such as clock position and
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
// class Map2DDatum
//
// This class handles data from a single buffer position.
//

public class Map2DDatum extends PlotterDatum{

    public int newData1;
    public int newData2;
    public int prevData1;
    public int prevData2;
    public int flags;

}//end of class Map2DDatum
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------