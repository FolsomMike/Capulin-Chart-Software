/******************************************************************************
* Title: TraceHdwVars.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class encapsulates variables passed between the Hardware class and the
* Trace class.
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
// class TraceHdwVars
//
// This class encapsulates variables passed between the Hardware class and the
// Trace class.
//

public class TraceHdwVars{

    //public int bufPtr;
    public int separatorPos;
    public int plotStyle;
    public int simDataType;

    //constants

    static int MAX_PLOT_STYLE = 2;
    static public int POINT_TO_POINT = 0;
    static public int STICK = 1;
    static public int SPAN = 2;

    static int MAX_SIM_TYPE = 2;
    static public int RANDOM_SPIKE = 0;
    static public int SAWTOOTH = 1;
    static public int GAMMA = 2;


//-----------------------------------------------------------------------------
// TraceHdwVars::setSimDataType
//
// Sets the simulation data type to pType.  If pType is illegal, type is set
// to 0.
//

public void setSimDataType(int pType)
{

    if (pType > MAX_SIM_TYPE) {pType = 0;}

    simDataType = pType;

}//end of class TraceHdwVars::setSimDataType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceHdwVars::setPlotStyle
//
// Sets the plot style to pStyle.  If pType is illegal, style is set to
// to 0.
//

public void setPlotStyle(int pStyle)
{

    if (pStyle > MAX_PLOT_STYLE) {pStyle = 0;}

    plotStyle = pStyle;

}//end of class TraceHdwVars::setPlotStyle
//-----------------------------------------------------------------------------


}//end of class TraceHdwVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
