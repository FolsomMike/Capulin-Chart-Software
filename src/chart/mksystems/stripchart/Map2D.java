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

import chart.Viewer;
import chart.Xfer;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.Color;
import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

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

    int widthOfDataInFile, lengthOfDataInFile;
    
//-----------------------------------------------------------------------------
// Map2D::Map2D (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Map2D(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pMap2DIndex, PlotterGlobals pPlotterGlobals,
            Color pBackgroundColor, Color pGridColor, int pGridXSpacing,
                Threshold[] pThresholds, Hardware pHardware)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup;
    chart = pChart;
    chartIndex = pChartIndex; plotterIndex = pMap2DIndex;
    gridColor = pGridColor;
    plotterGlobals = pPlotterGlobals;

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

    map2DData = new Map2DData(sizeOfDataBuffer, widthOfDataBuffer,
       hdwVs.plotStyle, higherMoreSevere ? PlotterData.MAX : PlotterData.MIN);

    map2DData.init();

    map2DDatum = new Map2DDatum();

}//end of Map2D::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::resetAll
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

    map2DData.resetAll();

}//end of Map2D::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::getDataHandler
//
// Returns a pointer to the data handling object.
//

public Map2DData getDataHandler()
{

    return (map2DData);

}//end of Map2D::getDataHandler
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
        return plotColumn(pG2, plotVs, map2DDatum);
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
// Plots a single column of data points in pMap2DDatum.
//
// All variables are passed via pVars and pMap2DDatum, so different sets can be
// used depending on the context, such as drawing for the first time or
// repainting.
//
// Returns the value last plotted.
//

private int plotColumn(Graphics2D pG2, PlotVars pVars, Map2DDatum pMap2DDatum)
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
        if ((pMap2DDatum.flags & PlotterData.SEGMENT_START_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if segment end flag set, draw a vertical separator bar
        if ((pMap2DDatum.flags & PlotterData.SEGMENT_END_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if end mask flag set and option enabled, draw a vertical separator bar
        if (useVerticalBarToMarkEndMasks
                     && (pMap2DDatum.flags & PlotterData.END_MASK_MARK) != 0){
            pG2.setColor(Color.GREEN);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

    }// if (leadPlotter)

    //save the value plotted so it can be returned on exit
    //it is assumed to be the bottom pixel of the column
    int lastPlotted =
                  pMap2DDatum.newDataColumn[pMap2DDatum.newDataColumn.length-1];

    //if the drawData flag is false, exit having only drawn decorations
    if (!pVars.drawData) {return(lastPlotted);}

    //draw the map data

    int y;

    for (int i = 0; i < pMap2DDatum.newDataColumn.length; i++){

        //translate data value to color -- on program startup the colorMapper
        //hasn't been set yet so just paint the background color

        if(colorMapper != null){
            pG2.setColor(
                colorMapper.mapIntegerToColor(pMap2DDatum.newDataColumn[i]));
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

}//end of Map2D::plotColumn
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
// If the end of valid data is reached in the buffer before the full chart is
// redrawn, the repaintVs.drawData flag is set false so that plotPoint can
// still be called to draw decorations across the entire chart without drawing
// undefined trace data.
//

@Override
public void paintComponent(Graphics2D pG2)
{

    //set starting point to the current buffer offset of the screen display

    map2DData.prepareForRepaint(plotterGlobals.bufOffset);

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

        map2DData.getDataAtRepaintPoint(map2DDatum);

        //stop tracing at end of valid data -- see notes in method header
        if ((map2DDatum.flags & PlotterData.DATA_VALID) == 0) {
            repaintVs.drawData = false;
        }

        plotColumn(pG2, repaintVs, map2DDatum);
    }

}//end of Map2D::paintComponent
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::saveSegment
//
// Saves the data for a segment to the open file pOut.
//
// This function should be called whenever a new segment is completed - each
// segment could represent a piece being monitored, a time period, etc.
//
// This function should be called after the segment end has been marked and
// before the next segment start has been marked so that the end points
// of the data to be saved are known.
//

@Override
public void saveSegment(BufferedWriter pOut) throws IOException
{

    pOut.write("[2D Map]"); pOut.newLine();
    pOut.write("2D Map Index=" + plotterIndex); pOut.newLine();
    pOut.write("2D Map Title=" + title); pOut.newLine();
    pOut.write("2D Map Short Title=" + shortTitle); pOut.newLine();
    //change from insertionPoint to lastSegmentEndIndex after Map2D is updated
    //to handle multiple segments in a circular buffer
    pOut.write("2D Map Data Buffer Length=" + map2DData.insertionPoint);
    pOut.newLine();
    pOut.write("2D Map Data Buffer Width=" + map2DData.widthOfDataBuffer);
    pOut.newLine();

    pOut.newLine();

    map2DData.saveSegment(pOut);

}//end of Map2D::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::loadSegment
//
// Loads the meta data, data points, and flags for a segment from pIn.  It is
// expected that the Map2D section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Map2D section, the [2D Map] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

@Override
public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    //handle entries for the trace itself
    String line = processMap2DMetaData(pIn, pLastLine);

    try{
        //read in trace data points
        line = map2DData.loadSegment(pIn, line);
    }
    catch(IOException e){

        //add identifying details to the error message and pass it on
        throw new IOException(e.getMessage() + " of Chart Group "
              + chartGroup + " Chart " + chartIndex + " Map2D " + plotterIndex);
    }

    return(line);

}//end of Map2D::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2D::processMap2DMetaData
//
// Processes file entries for the map such as the title via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Map2D section, the [2D Map] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processMap2DMetaData(BufferedReader pIn, String pLastLine)
                                                             throws IOException

{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    lengthOfDataInFile = map2DData.sizeOfDataBuffer;
    widthOfDataInFile = map2DData.widthOfDataBuffer;        
    
    //if pLastLine contains the [2D Map] tag, then start loading section
    //immediately else read until "[2D Map]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, "[2D Map]", "",  matchSet)) {
        success = true; //tag already found
    }
    else {
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, "[2D Map]", "",  matchSet)){
                success = true; break;
            }
        }//while
    }//else

    if (!success) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
            + chartGroup + " Chart " + chartIndex + " Map2D " + plotterIndex);
    }

    //set defaults
    int map2DIndexRead = -1;
    String titleRead = "", shortTitleRead = "";

    //scan the first part of the section and parse its entries
    //these entries apply to the chart group itself

    success = false;
    while ((line = pIn.readLine()) != null){

        //stop when next section tag reached (will start with [)
        if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
            success = true; break;
        }

        //read the "Trace Index" entry - if not found, default to -1
        if (Viewer.matchAndParseInt(line, "2D Map Index", -1, matchSet)) {
            map2DIndexRead = matchSet.rInt1;
        }

        //read the "... Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(line, "2D Map Title", "", matchSet)) {
            titleRead = matchSet.rString1;
        }

        //read the "... Short Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(
                                    line, "2D Map Short Title", "", matchSet)) {
            shortTitleRead = matchSet.rString1;
        }
        
        if (Viewer.matchAndParseInt(
              line, "2D Map Data Buffer Length", 
                                       map2DData.sizeOfDataBuffer, matchSet)) {
            lengthOfDataInFile = matchSet.rInt1;
        }        

        if (Viewer.matchAndParseInt(
              line, "2D Map Data Buffer Width",
                                       map2DData.widthOfDataBuffer, matchSet)) {
            widthOfDataInFile = matchSet.rInt1;
        }        
                
    }// while ((line = pIn.readLine()) != null)

    //validate and apply settings
    
    title = titleRead; shortTitle = shortTitleRead;
    
    if (lengthOfDataInFile > map2DData.sizeOfDataBuffer){
        lengthOfDataInFile = map2DData.sizeOfDataBuffer;
    }

    if (widthOfDataInFile > map2DData.widthOfDataBuffer){
        widthOfDataInFile = map2DData.widthOfDataBuffer;
    }
        
    if (!success) {
        throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
              + chartGroup + " Chart " + chartIndex + " Map2D " + plotterIndex);
    }

    //if the index number in the file does not match the index number for this
    //plotter, abort the file read

    if (map2DIndexRead != plotterIndex) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
              + chartGroup + " Chart " + chartIndex + " Map2D " + plotterIndex);
    }

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Map2D::processMap2DMetaData
//-----------------------------------------------------------------------------


}//end of class Map2D
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
