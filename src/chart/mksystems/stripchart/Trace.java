/******************************************************************************
* Title: Trace.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class handles a single trace.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.stripchart;

import chart.Viewer;
import chart.Xfer;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.*;
import java.io.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Trace
//
// This class creates and controls a trace.
//

public class Trace extends Plotter{

    public TraceData traceData;
    TraceDatum traceDatum;

    boolean invert;

    Threshold[] thresholds;
    int numberOfThresholds;
    int flagThreshold;
    //hardware channel of the last flag
    public int lastFlaggedChannel;
    //clock position of the last flag
    public int lastFlaggedClockPos;

//-----------------------------------------------------------------------------
// Trace::Trace (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Trace(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pTraceIndex, PlotterGlobals pPlotterGlobals,
            Color pBackgroundColor, Color pGridColor, int pGridXSpacing,
                Threshold[] pThresholds, Hardware pHardware)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup;
    chart = pChart;
    chartIndex = pChartIndex; plotterIndex = pTraceIndex;
    gridColor = pGridColor;
    plotterGlobals = pPlotterGlobals;
    gridXSpacing = pGridXSpacing ;
    //some of the code is more efficient with a variable of gridXSpacing-1
    gridXSpacingT = pGridXSpacing - 1;
    backgroundColor = pBackgroundColor; hardware = pHardware;
    thresholds = pThresholds; numberOfThresholds = thresholds.length;

    typeDescriptor = "Trace";

}//end of Trace::Trace (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::init
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

    //link the trace to the appropriate channel as specified in the config file
    //hardware may be null if this object is used for viewing only, so skip this
    //step if so
    if (hardware != null) {
        hardware.linkPlotters(chartGroup, chartIndex, plotterIndex, traceData,
                                            thresholds, hdwVs.plotStyle, this);
    }

}//end of Trace::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::setHeadNum
//
// If headNum is -1, the headNum supplied by the associated gate(s) is used.
//
// See notes for applyValuesFromGate for details on cases where the trace's
// values are overridden by the gate's values.
//

public void setHeadNum(int pHeadNum)
{

    if (headNum == -1){ headNum = pHeadNum; }

}//end of Trace::setHeadNum
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::setDistanceSensorToFrontEdgeOfHead
//
// Uses values from the connected gate instead of those specifically loaded
// for the trace.
//
// If the trace values are default then the supplied values are used.
//
// If multiple gates are tied to the trace, the values from the last gate
// will be used.
//
// When multiple gates are tied to a single trace, some values are loaded
// specifically for the trace from the config file and all gates are considered
// to have the same values such as head number and position.
//
// In cases where single gates are tied to one or more traces, those values are
// set to -1 for the trace in the config file. In that case the associated
// gate supplies the values from settings read for the gate from the config
// file. This makes it simpler when a single channel is tied to multiple traces
// as ajusting the values for that channel (and thus its gates) adjusts it
// for all connected traces. An example is Wall channels which may have gates
// tied to a thickness trace and a gate tied to a lamination trace. The gates
// share the values read for the channel and pass them on to their attached
// traces.
//

public void setDistanceSensorToFrontEdgeOfHead(
                                        double pDistanceSensorToFrontEdgeOfHead)
{        

    if (distanceSensorToFrontEdgeOfHeadOverridden){
        distanceSensorToFrontEdgeOfHead = pDistanceSensorToFrontEdgeOfHead;
    }

}//end of Trace::setDistanceSensorToFrontEdgeOfHead
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::configure
//
// Loads configuration settings from the configuration.ini file.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

    String section = configFileSection;

    invert = pConfigFile.readBoolean(section, "Invert Trace", false);

    int sizeOfDataBuffer = pConfigFile.readInt(
                                        section, "Number of Data Points", 1200);

    //create the arrays to hold data points and flag/decoration info
    if (sizeOfDataBuffer > 100000) {sizeOfDataBuffer = 100000;}

    traceData = new TraceData(sizeOfDataBuffer, hdwVs.plotStyle,
                        higherMoreSevere ? PlotterData.MAX : PlotterData.MIN);
    traceData.init();

    traceDatum = new TraceDatum();

}//end of Trace::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::resetAll
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

    traceData.resetAll();

}//end of Trace::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getDataBufferWidth
//
// Returns the length of the data buffer.
//
// Should be overridden by subclasses.
//

@Override
public int getDataBufferWidth()
{

    return(traceData.sizeOfDataBuffer);

}//end of Trace::getDataBufferWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getDataWidth
//
// Returns the index of the last valid data point.
//
// Returns -1 if no data found.
//

@Override
public int getDataWidth()
{

    return(traceData.getDataWidth());

}//end of Trace::getDataWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::saveSegment
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

    pOut.write("[Trace]"); pOut.newLine();
    pOut.write("Trace Index=" + plotterIndex); pOut.newLine();
    pOut.write("Trace Title=" + title); pOut.newLine();
    pOut.write("Trace Short Title=" + shortTitle); pOut.newLine();
    pOut.newLine();

    traceData.saveSegment(pOut);

}//end of Trace::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::loadSegment
//
// Loads the meta data, data points, and flags for a segment from pIn.  It is
// expected that the Trace section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Trace section, the [Trace] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

@Override
public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    //handle entries for the trace itself
    String line = processTraceMetaData(pIn, pLastLine);

    try{
        //read in trace data points
        line = traceData.loadSegment(pIn, line);
    }
    catch(IOException e){

        //add identifying details to the error message and pass it on
        throw new IOException(e.getMessage() + " of Chart Group "
              + chartGroup + " Chart " + chartIndex + " Trace " + plotterIndex);
    }

    return(line);

}//end of Trace::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::processTraceMetaData
//
// Processes file entries for the trace such as the title via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Trace section, the [Trace] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processTraceMetaData(BufferedReader pIn, String pLastLine)
                                                             throws IOException

{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [Trace] tag, then start loading section
    //immediately else read until "[Trace]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, "[Trace]", "",  matchSet)) {
        success = true; //tag already found
    }
    else {
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, "[Trace]", "",  matchSet)){
                success = true; break;
            }
        }//while
    }//else

    if (!success) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
            + chartGroup + " Chart " + chartIndex + " Trace " + plotterIndex);
    }

    //set defaults
    int traceIndexRead = -1;
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
        if (Viewer.matchAndParseInt(line, "Trace Index", -1, matchSet)) {
            traceIndexRead = matchSet.rInt1;
        }

        //read the "Trace Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(line, "Trace Title", "", matchSet)) {
            titleRead = matchSet.rString1;
        }

        //read the "Trace Short Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(
                                    line, "Trace Short Title", "", matchSet)) {
            shortTitleRead = matchSet.rString1;
        }

    }

    //apply settings
    title = titleRead; shortTitle = shortTitleRead;

    if (!success) {
        throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
              + chartGroup + " Chart " + chartIndex + " Trace " + plotterIndex);
    }

    //if the index number in the file does not match the index number for this
    //plotter, abort the file read

    if (traceIndexRead != plotterIndex) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
              + chartGroup + " Chart " + chartIndex + " Trace " + plotterIndex);
    }

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Trace::processTraceMetaData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::newDataIsReady
//
// Checks to see if any new data is ready to be plotted or erased.
//
// Returns true if new data is ready, false if not.
//

@Override
public boolean newDataIsReady()
{

    return (traceData.newDataIsReady());

}//end of Trace::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::plotNewData
//
// Plots new data in the array.  Assumes new data has been added to the next
// slot in the buffer.
//
// Returns the value last plotted.  If plot style is SPAN, this value will be
// from the second data set.
//

@Override
public int plotNewData(Graphics2D pG2)
{

    int dataReady = traceData.getNewData(traceDatum);

    if (dataReady == PlotterData.NO_NEW_DATA) {return(0);}

    //plot new point
    if (dataReady == PlotterData.FORWARD) {
        return plotPoint(pG2, plotVs, traceDatum);
    }

    if (dataReady == PlotterData.REVERSE) {
        return erasePoint(pG2, plotVs, traceDatum);
    }

    return(0);

}//end of Trace::plotNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::plotPoint
//
// Plots a single data point in pTraceDatum.  Assumes new data has been added to
// the next slot in the buffer.
//
// All variables are passed via pVars and pTraceDatum, so different sets can be
// used depending on the context, such as drawing for the first time or
// repainting.
//
// Returns the value last plotted.  If plot style is SPAN, this value will be
// from the second data set.
//

private int plotPoint(Graphics2D pG2, PlotVars pVars, TraceDatum pTraceDatum)
{

    //increment the pixel pointer until it reaches the right edge, then shift
    //the screen left and keep pointer the same to create scrolling effect
    //the scrolling starts at canvasXLimit-10 to allow room for flags

    if (pVars.pixPtr < canvasXLimit-10) {
        pVars.pixPtr++;
    }
    else{
        //if this is lead Plotter object, shift chart left and erase right slice
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
            if (plotterGlobals.bufOffset == traceData.sizeOfDataBuffer) {
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
        for (int j = 0; j < numberOfThresholds; j++) {
            thresholds[j].drawSlice(pG2, pVars.pixPtr);
        }

        if (pVars.gridCounter++ == gridXSpacingT){
            drawGrid(pG2, pVars.pixPtr, canvasYLimit);
            pVars.gridCounter = 0;
        }

        //if segment start flag set, draw a vertical separator bar
        if ((pTraceDatum.flags & PlotterData.SEGMENT_START_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if segment end flag set, draw a vertical separator bar
        if ((pTraceDatum.flags & PlotterData.SEGMENT_END_SEPARATOR) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if end mask flag set and option enabled, draw a vertical separator bar
        if (useVerticalBarToMarkEndMasks
                      && (pTraceDatum.flags & PlotterData.END_MASK_MARK) != 0){
            pG2.setColor(Color.GREEN);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

    }// if (leadPlotter)

    //apply offset, scaling, limits to y value
    //for span style, the high peaks and low peaks are in separate buffers in
    //the same index position

    if (hdwVs.plotStyle == PlotterHdwVars.POINT_TO_POINT){
        pVars.y1 = pTraceDatum.prevData1;
        pVars.y2 = pTraceDatum.newData1;
    }
    else if (hdwVs.plotStyle == PlotterHdwVars.STICK){
        pVars.y1 = 0;
        pVars.y2 = pTraceDatum.newData1;
    }
    else if (hdwVs.plotStyle == PlotterHdwVars.SPAN){
        pVars.y1 = pTraceDatum.newData1;
        pVars.y2 = pTraceDatum.newData2;
    }

    //save the value plotted so it can be returned on exit
    int lastPlotted = pVars.y2;

    //if the drawData flag is false, exit having only drawn decorations
    if (!pVars.drawData) {return(lastPlotted);}

    if(pVars.y1 == Integer.MIN_VALUE || pVars.y1 == Integer.MAX_VALUE
       || pVars.y2 == Integer.MIN_VALUE || pVars.y2 == Integer.MAX_VALUE){
        return(lastPlotted);
    }

    //prepare to draw the trace

    //apply pixel scaling
    pVars.y1 *= pixelScaling; pVars.y2 *= pixelScaling;

    //apply offset
    pVars.y1 += pixelOffset; pVars.y2 += pixelOffset;

    //apply limits
    if (pVars.y1 > canvasYLimit) {pVars.y1 = canvasYLimit;}
    if (pVars.y2 > canvasYLimit) {pVars.y2 = canvasYLimit;}

    //invert y value if specified
    if (invert) {
        pVars.y1 = canvasYLimit - pVars.y1;
        pVars.y2 = canvasYLimit - pVars.y2;
    }

    //debug mks -- remove this
    //offsets the traces so you can see them clearly for debugging
    //pVars.y1 -= 2 * traceIndex;
    //pVars.y2 -= 2 * traceIndex;
    //debug mks

    //if plotting point-point style, draw line from last height to next height
    //if plotting stick style, draw line from 0 to the signal height
    //if plotting span style, draw line between high and low peak in vertical
    // line

    pG2.setColor(traceColor);

    if (hdwVs.plotStyle == PlotterHdwVars.POINT_TO_POINT) {
        pG2.drawLine(pVars.pixPtr-1, pVars.y1, pVars.pixPtr, pVars.y2);
    }
    else if (hdwVs.plotStyle == PlotterHdwVars.STICK) {
        pG2.drawLine(pVars.pixPtr, pVars.y1, pVars.pixPtr, pVars.y2);
    }
    else if (hdwVs.plotStyle == PlotterHdwVars.SPAN) {
        pG2.drawLine(pVars.pixPtr, pVars.y1, pVars.pixPtr, pVars.y2);
    }

    //if there is a flag set for this data point then draw it - threshold

    //indices are shifted by two as 0 = no flag and 1 = user flag
    if ((flagThreshold =
              ((pTraceDatum.flags & PlotterData.THRESHOLD_MASK) >> 9)-2) >= 0){

        int flagY = pVars.y2; //draw flag at height of peak for non-SPAN styles

        //for span mode, draw flag at min or max depending on bit in flagBuffer
        if (hdwVs.plotStyle == PlotterHdwVars.SPAN){
            if ((pTraceDatum.flags & PlotterData.MIN_MAX_FLAGGED) == 0){
                flagY = pVars.y1;
            }
            else{
                flagY = pVars.y2;
            }
        }//if (hdwVs.plotStyle ==

        thresholds[flagThreshold].drawFlag(pG2, pVars.pixPtr, flagY);
        
        //java.awt.Toolkit.getDefaultToolkit().beep();
/*  //debug mks -- doesn't work very well -- threading issue?       
            javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { 
                    java.awt.Toolkit.getDefaultToolkit().beep(); } });
            
*/
          //  SwissArmyKnife.waitSleep(500);
        
        
        
    }
    else
        if (flagThreshold == -1) {drawUserFlag(pG2, pVars.pixPtr, pVars.y2);}


    //draw square markers if flag has been set for the current location
    if ((pTraceDatum.flags & PlotterData.MARKER_SQUARE) != 0) {
        drawMarkerSquare(pG2, pVars.pixPtr, 
            pVars.y2 <= (canvasYLimit-10) ? pVars.y2:canvasYLimit-10,
            Color.GREEN);
    }

    return(lastPlotted);

}//end of Trace::plotPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::drawMarkerSquare
//
// Draws a pColor square at the specified location.
//

void drawMarkerSquare(Graphics2D pG2, int xPos, int pSigHeight, Color pColor)

{
    //add 1 to xPos so flag is drawn to the right of the peak

    pG2.setColor(pColor);

    pG2.fillRect(xPos+1, pSigHeight, 8, 8);

}//end of Trace::drawMarkerSquare
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::erasesPoint
//
// Erases a single data point in the array.
//
// All variables are passed via pVars and pTraceDatum, so different sets can be
// used depending on the context.
//

public int erasePoint(Graphics2D pG2, PlotVars pVars, TraceDatum pTraceDatum)
{

    //debug mks -- next section for scrolling in reverse NOT tested well!
    // in fact, it probably doesn't work well at all, but for now most
    // reversing doesn't go far enough back to require reverse scroll

    //decrement the pixel pointer until it reaches the left edge, then shift the
    //screen right and keep pointer the same to create scrolling effect

    if (pVars.pixPtr > 0) {
        pVars.pixPtr--;
    }
    else{
        //if this is trailing trace, shift the chart right and erase left slice
        if (trailPlotter){
            //scroll the screen 1 pixel to the left
            pG2.copyArea(0, 0, canvas.getWidth(), canvas.getHeight(), 1, 0);
            //erase the line at the far right
            pG2.setColor(backgroundColor);
            pG2.drawLine(0, 0, 0, canvas.getHeight());

            //shift the buffer location where the display starts - this is done
            //when plotting new data but not when the screen is being repainted
            //as this section is not reached when called by the repaint code;
            //the drawing stops before the right edge of the screen is reached
            //the counter is global for all traces on a chart because they all
            //are shifted when trace 0 shifts the canvas

            plotterGlobals.bufOffset--;
            if (plotterGlobals.bufOffset == -1) {
                plotterGlobals.bufOffset = traceData.getDataBuffer1().length-1;
            }

            //track the number of pixels the chart has been scrolled - this is
            //used to determine the proper location of grid marks and other
            //decorations when the screen is repainted

            plotterGlobals.scrollCount--;

        }//if (traceIndex == 0)

    }//else if (pixPtr <...


    pG2.setColor(backgroundColor);
    pG2.drawLine(pVars.pixPtr+1, 0, pVars.pixPtr+1, canvas.getHeight());

    //save the last value erased so it can be returned on exit
    int lastPlotted = pVars.y2;

    return(lastPlotted);

}//end of Trace::erasePoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::drawUserFlag
//
// Draws a user flag which is a circle using the color of the most severe
// threshold.
//

void drawUserFlag(Graphics2D pG2, int xPos, int pSigHeight)

{

    //add 1 to xPos so flag is drawn to the right of the peak

    pG2.setColor(thresholds[0].thresholdColor);

    pG2.fillOval(xPos+1, pSigHeight, 8, 8);

}//end of Trace::drawUserFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::drawGrid
//
// Draws the grid marks
//

void drawGrid(Graphics2D pG2, int pXPos, int pCanvasYLimit)
{

    //for screen display, zero width/height for grid pixels looks best
    //when rendering for printing, must set width to 1 or pixels disappear
    int width = settings.printMode ?  1 : 0;

    pG2.setColor(gridColor);

    for(int i = 9; i < pCanvasYLimit; i+=10){
        pG2.drawRect(pXPos, i, width, 0);
    }

}//end of Trace::drawGrid
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getDataBuffer1
//
// Returns a reference to dataBuffer1.
//

@Override
public int[] getDataBuffer1()
{

    return(traceData.getDataBuffer1());

}//end of Trace::getDataBuffer1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getFlagBuffer
//
// Returns a reference to flagBuffer.
//

@Override
public int[] getFlagBuffer()
{

    return(traceData.flagBuffer);

}//end of Trace::getFlagBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getPixXY
//
// Translates the data and data position in the circular buffer to pixel
// x,y values.
//

void getPixXY(PlotVars pVs)
{


}//end of Trace::getPixXY
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::findMinValue
//
// Finds the minimum value of data in the buffer.  Search begins at pStart
// position in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and the buffer length
// to avoid errors.
//
// If pFlagLocation is true, a flag will be placed in the data buffer at the
// location of the min value.
//
// Returns the min value and also returns it in pPeakInfo.peak.
// The buffer index of that value is returned in pPeakInfo.index.
//

@Override
public int findMinValue(int pStart, int pEnd, TraceDatum pPeakInfo,
                                                        boolean pFlagLocation)
{

    int peak = (traceData.findMinValue(pStart, pEnd, pPeakInfo));

    if (pFlagLocation && pPeakInfo.index != -1){
        traceData.setFlags(pPeakInfo.index, PlotterData.MARKER_SQUARE);
    }

    return(peak);

}//end of Trace::findMinValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::findMaxValue
//
// Finds the maximum value of data in the buffer.  Search begins at pStart
// position in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and the buffer length
// to avoid errors.
//
// If pFlagLocation is true, a flag will be placed in the data buffer at the
// location of the max value.
//
// Returns the max value and also returns it in pPeakInfo.peak.
// The buffer index of that value is returned in pPeakInfo.index.
//

@Override
public int findMaxValue(int pStart, int pEnd,TraceDatum pPeakInfo,
                                                        boolean pFlagLocation)
{

    int peak = (traceData.findMaxValue(pStart, pEnd, pPeakInfo));

    if (pFlagLocation && pPeakInfo.index != -1){
        traceData.setFlags(pPeakInfo.index, PlotterData.MARKER_SQUARE);
    }

    return(peak);

}//end of Trace::findMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::setLastFlagged
//
// Stores the channel number and clock position which was last flagged.  Also
// stores those values in the chart which owns this trace.
//

public void setLastFlagged(int pChannel, int pClock)
{

    lastFlaggedChannel = pChannel;
    chart.lastFlaggedChannel = lastFlaggedChannel;

    lastFlaggedClockPos = pClock;
    chart.lastFlaggedClockPos = lastFlaggedClockPos;

}//end of Trace::setLastFlagged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::markSegmentStart
//
// Resets the counter which is used to determine if a new segment has begun
// and records the start position.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

@Override
public void markSegmentStart()
{

    traceData.markSegmentStart();

}//end of Trace::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::markSegmentEnd
//
// Marks the buffer location of the end of the current segment.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//
// This function should be called before saving the data so the end points
// of the data to be saved are known.
//

@Override
public void markSegmentEnd()
{

    traceData.markSegmentEnd();

}//end of Trace::markSegmentEnd
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// Plotter::segmentStarted
//
// Returns true if a segment has been started by having had data added to it,
// returns false otherwise.
//

@Override
public boolean segmentStarted()
{

    return(traceData.segmentStarted());

}//end of Plotter::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::placeEndMaskMarker
//
// Marks the current buffer location to signal that an end mask mark should
// be drawn at that position.
//

@Override
public void placeEndMaskMarker()
{

    traceData.placeEndMaskMarker();

}//end of Plotter::placeEndMaskMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// This method should only be called by the producer thread.
//

@Override
public void advanceInsertionPoint()
{

    traceData.advanceInsertionPoint();

}//end of Trace::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::paintComponent
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

    traceData.prepareForRepaint(plotterGlobals.bufOffset);

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

        traceData.getDataAtRepaintPoint(traceDatum);

        //stop tracing at end of valid data -- see notes in method header
        if ((traceDatum.flags & PlotterData.DATA_VALID) == 0) {
            repaintVs.drawData = false;
        }

        plotPoint(pG2, repaintVs, traceDatum);
    }

}//end of Trace::paintComponent
//-----------------------------------------------------------------------------

}//end of class Trace
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------