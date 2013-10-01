/******************************************************************************
* Title: Map3DDatum.java
* Author: Mike Schoonover
* Date: 9/30/13
*
* Purpose:
*
* This class handles data from a single buffer position of a 3D map. It includes
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
// class Map3DDatum
//
// This class handles data from a single buffer position.
//

public class Map3DDatum extends PlotterDatum{

    public int [] newDataColumn;
    public int [] prevDataColumn;
    public int flags;

}//end of class Map3DDatum
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------