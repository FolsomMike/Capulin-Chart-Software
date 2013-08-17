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
import chart.mksystems.hardware.WallMap2DColorMapper;
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

    int verticalOffset, verticalSize;
    int minY, maxY;

//-----------------------------------------------------------------------------
// Map2D::Map2D (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Map2D(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pMap2DIndex, PlotterGlobals pTraceGlobals,
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

    plotVs = new PlotVars(); repaintVs = new PlotVars();
    hdwVs = new PlotterHdwVars();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

    //debug mks

    //this should be done in Main, Hardware, Capulin 1 based on settings
    //read from config file so the colorMapper is customized to the config

    //test pipe .707 wall, 13-3/4 dia.

    // .707 wall, use range of +20% to -50%

    // hue base of 0.0 is Red
    // hue base of .6666666667 is Blue

    //for simulation using .250 wall:
    // +20% = .30    -50% = .125, range is .175

    //public WallMap2DColorMapper(int pValueBase, int pValueRange, float pHueBase,
    //       float pHueRange, float pHue, float pSaturation, float pBrightness,
    //      boolean pInvertHue)


    setColorMapper(new WallMap2DColorMapper(
            125, 175, (float)0.0, (float).6666666667,
            (float)1.0, (float)1.0, (float)1.0, false));


    //debug mks

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

    verticalSize = pConfigFile.readInt(section, "Vertical Size", 100);

    verticalOffset = pConfigFile.readInt(section, "Vertical Offset", 5);

    minY = verticalOffset; maxY = minY + verticalSize;

    int sizeOfDataBuffer = pConfigFile.readInt(
                                       section, "Length of Data Buffer", 1200);

    if (sizeOfDataBuffer > 100000) {sizeOfDataBuffer = 100000;}

    int widthOfDataBuffer = pConfigFile.readInt(
                                       section, "Width of Data Buffer", 100);


    if (widthOfDataBuffer > 100000) {widthOfDataBuffer = 100000;}

    //create the arrays to hold data points and flag/decoration info

    map2DData = new Map2DData(sizeOfDataBuffer, widthOfDataBuffer,
            hdwVs.plotStyle, higherMoreSevere ? TraceData.MAX : TraceData.MIN);

    map2DData.init();

    map2DDatum = new Map2DDatum();

}//end of Map2D::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::newDataIsReady
//
// Checks to see if any new data is ready to be plotted or erased.
//
// Returns true if new data is ready, false if not.
//

@Override
public boolean newDataIsReady()
{

    return (map2DData.newDataIsReady());

}//end of Map2D::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::plotNewData
//
// Plots new data column in the array.  Assumes new data has been added to the
// next slot in the buffer.
//
// Returns the data point last plotted.
//

@Override
public int plotNewData(Graphics2D pG2)
{

    int dataReady = map2DData.getNewData(map2DDatum);

    if (dataReady == PlotterData.NO_NEW_DATA) {return(0);}

    //plot new point
    if (dataReady == PlotterData.FORWARD) {
        return plotPoint(pG2, plotVs, map2DDatum);
    }

    if (dataReady == PlotterData.REVERSE) {
//debug mks -- add this back in        return erasePoint(pG2, plotVs, map2DDatum);
    }

    return(0);

}//end of Map2D::plotNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::plotColumn
//
// Plots a single column of data points in the array.  Assumes new data has
// been added to the next column slot in the buffer.
//
// All variables are passed via pVars and pTraceDatum, so different sets can be
// used depending on the context.
//
// Returns the value last plotted.
//

public int plotPoint(Graphics2D pG2, PlotVars pVars, Map2DDatum pMap2DDatum)
{

    //increment the pixel pointer until it reaches the right edge, then shift
    //the screen left and keep pointer the same to create scrolling effect
    //the scrolling starts at canvasXLimit-10 to allow room for flags

    if (pVars.pixPtr < canvasXLimit-10) {
        pVars.pixPtr++;
    }
    else{
        //if this is the lead plotter, shift chart left and erase right slice
        if (leadPlotter){
            //scroll the screen 1 pixel to the left
            pG2.copyArea(1, 0, canvas.getWidth(), canvas.getHeight(), -1, 0);
            //erase the line at the far right
            pG2.setColor(backgroundColor);
            pG2.drawLine(canvasXLimit, 0, canvasXLimit, canvas.getHeight());

            //shift the buffer location where the display starts - this is done
            //when plotting new data but not when the screen is being repainted
            //as this section is not reached when called by the repaint code;
            //the drawing stops before the right edge of the screen is reached
            //the counter is global for all traces on a chart because they all
            //are shifted when trace 0 shifts the canvas

            plotterGlobals.bufOffset++;
            if (plotterGlobals.bufOffset == map2DData.sizeOfDataBuffer) {
                plotterGlobals.bufOffset = 0;
            }

            //track the number of pixels the chart has been scrolled - this is
            //used to determine the proper location of grid marks and other
            //decorations when the screen is repainted

            plotterGlobals.scrollCount++;

        }//if (traceIndex == 0)

    }//else if (pVars.pixPtr...

    //if this is the lead Plotter object draw the decorations
    if (leadPlotter){

        //if segment start flag set, draw a vertical separator bar
        if ((pMap2DDatum.flags & TraceData.SEGMENT_START_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if segment end flag set, draw a vertical separator bar
        if ((pMap2DDatum.flags & TraceData.SEGMENT_END_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if end mask flag set and option enabled, draw a vertical separator bar
        if (useVerticalBarToMarkEndMasks
                        && (pMap2DDatum.flags & TraceData.END_MASK_MARK) != 0){
            pG2.setColor(Color.GREEN);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

    }// if (leadPlotter)

    int y;

    for (int i = 0; i < pMap2DDatum.newDataColumn.length; i++){

        //translate data value to color
        pG2.setColor(
                colorMapper.mapIntegerToColor(pMap2DDatum.newDataColumn[i]));

        //apply vertical offset
        y = i + minY;

        //draw each pixel of the column
        pG2.drawLine(pVars.pixPtr, y, pVars.pixPtr, y);

    }

    //save the value plotted so it can be returned on exit
    //it is assumed to be the bottom pixel of the column
    int lastPlotted =
                  pMap2DDatum.newDataColumn[pMap2DDatum.newDataColumn.length-1];

    //if the drawData flag is false, exit having only drawn decorations
    if (!pVars.drawData) {return(lastPlotted);}

    // can draw decorations here -- see Trace object for example

    return(lastPlotted);

}//end of Map2D::plotPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// This method should only be called by the producer thread.
//

@Override
public void advanceInsertionPoint()
{

    map2DData.advanceInsertionPoint();

}//end of Map2D::advanceInsertionPoint
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
