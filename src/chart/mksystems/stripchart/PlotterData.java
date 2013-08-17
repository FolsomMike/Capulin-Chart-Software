/******************************************************************************
* Title: PlotterData.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This class handles data for a Plotter object such as a Trace, Map2D, Map3D,
* etc. It has synchronized functions (or other protection mechanisms) to allow
* data to be inserted by one thread and read for display by a different thread.
*
* Only two threads should access this class -- a producer thread which adds
* or removes data and a consumer thread which reads data.
*
* All methods which alter the flagBuffer should be synchronized. Those that
* only read it do not have to be synchronized.
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
// class PlotterData
//
// This is the parent class for classes which create and control a data set for
// a Plotter object.
//

public class PlotterData extends Object{



//-----------------------------------------------------------------------------
// PlotterData::PlotterData (constructor)
//
//

public PlotterData()
{


}//end of PlotterData::PlotterData (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of PlotterData::init
//-----------------------------------------------------------------------------

}//end of class PlotterData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
