/******************************************************************************
* Title: Map3D.java
* Author: Mike Schoonover
* Date: 9/30/13
*
* Purpose:
*
* This class handles a 3 dimensional map.
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
// class Map3D
//
// This class creates and controls a 3D map.
//

public class Map3D extends Plotter{

    int map3DIndex;

    Map3DData map3DData;
    Map3DDatum map3DDatum;

    int verticalOffset, verticalSize;
    int minY, maxY;

//-----------------------------------------------------------------------------
// Map3D::Map3D (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Map3D(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pMap3DIndex, PlotterGlobals pPlotterGlobals,
            Color pBackgroundColor, Color pGridColor, int pGridXSpacing,
                Threshold[] pThresholds, Hardware pHardware)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup;
    chart = pChart;
    chartIndex = pChartIndex; plotterIndex = pMap3DIndex;
    gridColor = pGridColor;
    plotterGlobals = pPlotterGlobals;

    gridXSpacing = pGridXSpacing ;
    //some of the code is more efficient with a variable of gridXSpacing-1
    gridXSpacingT = pGridXSpacing - 1;
    backgroundColor = pBackgroundColor;
    hardware = pHardware;

    typeDescriptor = "3D Map";

}//end of Map3D::Map3D (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::init
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

}//end of Map3D::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapD::configure
//
// Loads configuration settings from the configuration.ini file.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

    String section = configFileSection;

    dataSourceBoardChassis = pConfigFile.readInt(
                                    section, "Data Source Board Chassis", -1);

    dataSourceBoardSlot = pConfigFile.readInt(
                                    section, "Data Source Board Slot", -1);

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

    map3DData = new Map3DData(sizeOfDataBuffer, widthOfDataBuffer,
       hdwVs.plotStyle, higherMoreSevere ? PlotterData.MAX : PlotterData.MIN);

    map3DData.init();

    map3DDatum = new Map3DDatum();

}//end of Map3D::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::resetAll
//
// Clears all data.
//
// Do not call from the constructor because all data may not be available at
// that time.
//

@Override
public void resetAll()
{

    super.resetAll();

    map3DData.resetAll();

}//end of Map3D::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::getDataHandler
//
// Returns a pointer to the data handling object.
//

public Map3DData getDataHandler()
{

    return (map3DData);

}//end of Map3D::getDataHandler
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::newDataIsReady
//
// Checks to see if any new data is ready to be plotted or erased.
//
// Returns true if new data is ready, false if not.
//

@Override
public boolean newDataIsReady()
{

    return (map3DData.newDataIsReady());

}//end of Map3D::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::plotNewData
//
// Plots new data column in the array.  Assumes new data has been added to the
// next slot in the buffer.
//
// Returns the data point last plotted.
//

@Override
public int plotNewData(Graphics2D pG2)
{

    int dataReady = map3DData.getNewData(map3DDatum);

    if (dataReady == PlotterData.NO_NEW_DATA) {return(0);}

    //plot new point
    if (dataReady == PlotterData.FORWARD) {
        return plotColumn(pG2, plotVs, map3DDatum);
    }

    if (dataReady == PlotterData.REVERSE) {
//debug mks -- add this back in        return erasePoint(pG2, plotVs, map3DDatum);
    }

    return(0);

}//end of Map3D::plotNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::plotColumn
//
// Plots a single column of data points in pMap3DDatum.
//
// All variables are passed via pVars and pMap3DDatum, so different sets can be
// used depending on the context, such as drawing for the first time or
// repainting.
//
// Returns the value last plotted.
//

private int plotColumn(Graphics2D pG2, PlotVars pVars, Map3DDatum pMap3DDatum)
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
            if (plotterGlobals.bufOffset == map3DData.sizeOfDataBuffer) {
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
        if ((pMap3DDatum.flags & PlotterData.SEGMENT_START_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if segment end flag set, draw a vertical separator bar
        if ((pMap3DDatum.flags & PlotterData.SEGMENT_END_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if end mask flag set and option enabled, draw a vertical separator bar
        if (useVerticalBarToMarkEndMasks
                     && (pMap3DDatum.flags & PlotterData.END_MASK_MARK) != 0){
            pG2.setColor(Color.GREEN);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

    }// if (leadPlotter)

    //save the value plotted so it can be returned on exit
    //it is assumed to be the bottom pixel of the column
    int lastPlotted =
                  pMap3DDatum.newDataColumn[pMap3DDatum.newDataColumn.length-1];

    //if the drawData flag is false, exit having only drawn decorations
    if (!pVars.drawData) {return(lastPlotted);}

    //draw the map data

    int y;

    for (int i = 0; i < pMap3DDatum.newDataColumn.length; i++){

        //translate data value to color -- on program startup the colorMapper
        //hasn't been set yet so just paint the background color

        if(colorMapper != null){
            pG2.setColor(
                colorMapper.mapIntegerToColor(pMap3DDatum.newDataColumn[i]));
        }
        else{
            pG2.setColor(backgroundColor);
        }

        //apply vertical offset
        y = i + minY;

        //draw each pixel of the column
        pG2.drawLine(pVars.pixPtr, y, pVars.pixPtr, y);

    }

    return(lastPlotted);

}//end of Map3D::plotColumn
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// This method should only be called by the producer thread.
//

@Override
public void advanceInsertionPoint()
{

    map3DData.advanceInsertionPoint();

}//end of Map3D::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map3D::paintComponent
//
// Refreshes the canvas using the data in the buffers.
//
// If the end of valid data is reached in the buffer before the full chart is
// redrawn, the repaintVs.drawData flag is set false so that plotPoint can
// still be called to draw decorations across the entire chart without drawing
// undefined trace data.
//

@Override
public void paintComponent(Graphics2D pG2)
{

    //set starting point to the current buffer offset of the screen display

    map3DData.prepareForRepaint(plotterGlobals.bufOffset);

    // the repaintVS object is used here to avoid conflict with the
    // plotVs object which tracks plotting of new data

    repaintVs.pixPtr = -1;

    //for repainting, the gridCounter starts at one to sync up with drawing by
    //the plotNewData code

    repaintVs.gridCounter = plotterGlobals.scrollCount % gridXSpacing;

    //start with drawing trace allowed - will be set false by plotPoint when
    //an undefined data point reached which signifies the end of valid data

    repaintVs.drawData = true;

    //stop short of the end of the screen to avoid triggering chart scroll
    //in the plotPoint function

    int stop = canvasXLimit-10;

    for (int i = 0; i < stop; i++){

        map3DData.getDataAtRepaintPoint(map3DDatum);

        //stop tracing at end of valid data -- see notes in method header
        if ((map3DDatum.flags & PlotterData.DATA_VALID) == 0) {
            repaintVs.drawData = false;
        }

        plotColumn(pG2, repaintVs, map3DDatum);
    }

}//end of Map3D::paintComponent
//-----------------------------------------------------------------------------

}//end of class Map3D
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
