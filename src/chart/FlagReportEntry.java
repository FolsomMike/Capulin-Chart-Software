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

package chart;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FlagReportEntry
//
// This class encapsulates variables for plotting.
//
// Variable sortOverride is used to force an entry or group of entries into a
// designated position. For instance, setting override for all Wall entries
// to 1000 will force the Wall entries to the top of the sort list.
//

public class FlagReportEntry{

    public double linearPos = 0.0;
    public int clockPos = 0;
    public String chartShortTitle = "";
    public String plotterShortTitle = "";
    public int amplitude = 0;

    public int sortOverride = 0;
    
    public int sortOrder = DESCENDING;
    
    public boolean isWall = false;

    public static final int ASCENDING = 0;
    public static final int DESCENDING = 1;
    
//-----------------------------------------------------------------------------
// FlagReportEntry::FlagReportEntry (constructor)
//

public FlagReportEntry(double pLinearPos, int pClockPos, 
            String pChartShortTitle, String pPlotterShortTitle, int pAmplitude,
            int pSortOverride, int pSortOrder, boolean pIsWall)
{

    linearPos = pLinearPos;
    clockPos = pClockPos;
    chartShortTitle = pChartShortTitle;
    plotterShortTitle = pPlotterShortTitle;
    amplitude = pAmplitude;
    sortOverride = pSortOverride;
    sortOrder = pSortOrder;
    isWall = pIsWall;

}//end of FlagReportEntry::FlagReportEntry (constructor)
//-----------------------------------------------------------------------------
    
}//end of class FlagReportEntry
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
