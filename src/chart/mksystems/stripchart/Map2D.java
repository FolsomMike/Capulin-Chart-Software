/******************************************************************************
* Title: Map2D.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This class handles a 2 dimensional map.
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

import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.Color;
import java.awt.Graphics2D;

//-----------------------------------------------------------------------------
// class Map2D
//
// This class creates and controls a 2D map.
//

public class Map2D extends Plotter{

    int map2DIndex;

    Map2DData map2DData;
    Map2DDatum map2DDatum;

//-----------------------------------------------------------------------------
// Map2D::Map2D (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Map2D(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pMap2DIndex, TraceGlobals pTraceGlobals,
            Color pBackgroundColor, Color pGridColor, int pGridXSpacing,
                Threshold[] pThresholds, Hardware pHardware)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup;
    chart = pChart;
    chartIndex = pChartIndex; plotterIndex = pMap2DIndex;
    gridColor = pGridColor;

    gridXSpacing = pGridXSpacing ;
    //some of the code is more efficient with a variable of gridXSpacing-1
    gridXSpacingT = pGridXSpacing - 1;
    backgroundColor = pBackgroundColor;
    hardware = pHardware;

    typeDescriptor = "2D Map";

}//end of Map2D::Map2D (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    super.init();

//    plotVs = new PlotVars(); repaintVs = new PlotVars();
    hdwVs = new TraceHdwVars();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

}//end of Map2D::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::configure
//
// Loads configuration settings from the configuration.ini file.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

    String section = configFileSection;

    int sizeOfDataBuffer = pConfigFile.readInt(
                                        section, "Number of Data Points", 1200);

    //create the arrays to hold data points and flag/decoration info
    if (sizeOfDataBuffer > 100000) {sizeOfDataBuffer = 100000;}

    map2DData = new Map2DData(sizeOfDataBuffer, hdwVs.plotStyle,
                        higherMoreSevere ? TraceData.MAX : TraceData.MIN);
    map2DData.init();

    map2DDatum = new Map2DDatum();

}//end of Map2D::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::paintComponent
//
// Refreshes the canvas using the data in the buffers.
//

@Override
public void paintComponent(Graphics2D pG2)
{

/*

    //set starting point to the current buffer offset of the screen display

    traceData.prepareForRepaint(traceGlobals.bufOffset);

    // the repaintVS object is used here to avoid conflict with the
    // plotVs object which tracks plotting of new data

    repaintVs.pixPtr = -1;

    //for repainting, the gridCounter starts at one to sync up with drawing by
    //the plotNewData code

    repaintVs.gridCounter = traceGlobals.scrollCount % gridXSpacing;

    //start with drawing trace allowed - will be set false by plotPoint when
    //an undefined data point reached which signifies the end of valid data

    repaintVs.drawTrace = true;

    //stop short of the end of the screen to avoid triggering chart scroll
    //in the plotPoint function

    int stop = canvasXLimit-10;

    for (int i = 0; i < stop; i++){

        traceData.getDataAtRepaintPoint(traceDatum);

        //stop tracing at end of valid data -- see notes in method header
        if ((traceDatum.flags & TraceData.DATA_VALID) == 0) {
            repaintVs.drawTrace = false;
        }

        plotPoint(pG2, repaintVs, traceDatum);
    }

*
*/


}//end of Map2D::paintComponent
//-----------------------------------------------------------------------------

}//end of class Map2D
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
