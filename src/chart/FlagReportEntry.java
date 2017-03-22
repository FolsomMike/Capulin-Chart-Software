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
// Variable sortValue is the value actually used for group sorting. It can be
// manipulated to force a group to the top or bottom and can affect the
// sorting order of the groups. Order within the groups is sorted by
// the amplitude and sortOrder variables.
//

public class FlagReportEntry{

    public double linearPos = 0.0;
    public int clockPos = 0;
    public String chartShortTitle = "";
    public String plotterShortTitle = "";
    public int amplitude = 0;

    public int groupSortValue = 0;
    
    public int sortOrder = DESCENDING;
    
    public boolean isWall = false;

    public static final int ASCENDING = 0;
    public static final int DESCENDING = 1;
    
//-----------------------------------------------------------------------------
// FlagReportEntry::FlagReportEntry (constructor)
//

public FlagReportEntry(double pLinearPos, int pClockPos, 
            String pChartShortTitle, String pPlotterShortTitle, int pAmplitude,
            int pGroupSortValue, int pSortOrder, boolean pIsWall)
{

    linearPos = pLinearPos;
    clockPos = pClockPos;
    chartShortTitle = pChartShortTitle;
    plotterShortTitle = pPlotterShortTitle;
    amplitude = pAmplitude;
    groupSortValue = pGroupSortValue;
    sortOrder = pSortOrder;
    isWall = pIsWall;

}//end of FlagReportEntry::FlagReportEntry (constructor)
//-----------------------------------------------------------------------------
    
}//end of class FlagReportEntry
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
