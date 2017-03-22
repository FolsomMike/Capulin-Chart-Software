/******************************************************************************
* Title: FlagReportEntry.java
* Author: Mike Schoonover
* Date: 3/21/17
*
* Purpose:
*
* This class encapsulates a single entry on the flag report.
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
// class FlagReportEntry
//
// This class encapsulates variables for plotting.
//

public class FlagReportEntry{

    public double linearPos = 0.0;
    public int clockPos = 0;
    public String title = "";
    public int amplitude = 0;

//-----------------------------------------------------------------------------
// FlagReportEntry::FlagReportEntry (constructor)
//

public FlagReportEntry(double pLinearPos, int pClockPos, String pTitle,
                                                                int pAmplitude)
{

    linearPos = pLinearPos;
    clockPos = pClockPos;
    title = pTitle;
    amplitude = pAmplitude;

}//end of FlagReportEntry::FlagReportEntry (constructor)
//-----------------------------------------------------------------------------
    
}//end of class FlagReportEntry
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
