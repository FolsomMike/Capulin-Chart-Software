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

import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.font.FontRenderContext;

import chart.mksystems.settings.Settings;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.hardware.Hardware;
import chart.Viewer;
import chart.Xfer;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Trace
//
// This class creates and controls a trace.
//

public class Trace extends Object{

    Settings settings;
    IniFile configFile;
    TraceGlobals traceGlobals;
    int chartGroup;
    public int chartIndex;
    public StripChart chart;
    int traceIndex;
    Hardware hardware;
    JPanel canvas;
    int canvasXLimit;
    int canvasYLimit;
    PlotVars plotVs, repaintVs;
    TraceHdwVars hdwVs;
    int gridXSpacing, gridXSpacingT;
    public boolean leadTrace = false;
    public boolean trailTrace = false;

    String title;
    public String shortTitle;
    public String keyLabel;
    int keyXPosition, keyYPosition;
    public Rectangle2D keyBounds;
    Color traceColor;
    public int head;
    public boolean flaggingEnabled = false;
    public boolean useVerticalBarToMarkEndMasks = false;
    public boolean suppressTraceInEndMasks = false;
    public double distanceSensorToFrontEdgeOfHead;
    public double delayDistance;
    public double startFwdDelayDistance;
    public double startRevDelayDistance;
    public int sizeOfDataBuffer;
    public int dataBuffer1[];
    int dataBuffer2[];
    public int flagBuffer[]; //stores various flags for plotting
                             //0000 0000 0000 0000 | 0000 000 | 0 0000 0000
                             //               |||| | threshold| clock position
                             //               |||> min or max was flagged
                             //               ||> segment start separator
                             //               |> segment end separator
                             //               > end mask marks


    boolean invert;
    int pixelOffset;
    double pixelScaling;
    double preScaling;
    double preOffset;
    boolean higherMoreSevere;

    public String prevLinearPos = "";
    public String prevAmplitudeText = "";
    public int prevClockPos = -1;

    Color backgroundColor;
    Color gridColor;
    Threshold[] thresholds;
    int numberOfThresholds;
    int flagThreshold;
    //hardware channel of the last value to be stored as a peak
    public int peakChannel;
    //hardware channel of the last flag
    public int lastFlaggedChannel;
    //clock position of the last flag
    public int lastFlaggedClockPos;
    //wall thickness value - used by wall traces
    public double wallThickness;

    public int beingFilledSlot; //updated by external class - slot for new data
    public int inProcessSlot; //being filled with data
    //updated by external class - point to last data to plot
    public int endPlotSlot;
    public boolean nextIndexUpdated;  //used by external class

    int segmentStartCounter;
    int lastSegmentStartIndex;
    int lastSegmentEndIndex;

//-----------------------------------------------------------------------------
// Trace::Trace (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Trace(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pTraceIndex, TraceGlobals pTraceGlobals,
            Color pBackgroundColor, Color pGridColor, int pGridXSpacing,
                Threshold[] pThresholds, Hardware pHardware)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup;
    chart = pChart;
    chartIndex = pChartIndex; traceIndex = pTraceIndex; gridColor = pGridColor;
    traceGlobals = pTraceGlobals;
    gridXSpacing = pGridXSpacing ;
    //some of the code is more efficient with a variable of gridXSpacing-1
    gridXSpacingT = pGridXSpacing - 1;
    backgroundColor = pBackgroundColor; hardware = pHardware;
    thresholds = pThresholds; numberOfThresholds = thresholds.length;

    plotVs = new PlotVars(); repaintVs = new PlotVars();
    hdwVs = new TraceHdwVars();

}//end of Trace::Trace (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

    //link the trace to the appropriate channel as specified in the config file
    //hardware may be null if this object is used for viewing only, so skip this
    //step if so
    if (hardware != null)
        hardware.linkTraces(chartGroup, chartIndex, traceIndex, dataBuffer1,
                    dataBuffer2, flagBuffer, thresholds, hdwVs.plotStyle, this);

}//end of Trace::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::configure
//
// Loads configuration settings from the configuration.ini file.
//

private void configure(IniFile pConfigFile)
{

    String section = "Chart Group " + (chartGroup + 1)
             + " Strip Chart " + (chartIndex + 1) + " Trace " + (traceIndex+1);

    title = pConfigFile.readString(section, "Title", "*");

    shortTitle = pConfigFile.readString(section, "Short Title", "*");

    keyLabel = pConfigFile.readString(section, "Key Label", "*");

    keyXPosition = pConfigFile.readInt(section, "Key X Position", 100);

    keyYPosition = pConfigFile.readInt(section, "Key Y Position", 23);

    traceColor = pConfigFile.readColor(section, "Color", Color.BLACK);

    head = pConfigFile.readInt(section, "Head", 1);

    distanceSensorToFrontEdgeOfHead = pConfigFile.readDouble(section,
                             "Distance From Sensor to Front Edge of Head", 0.0);

    sizeOfDataBuffer = pConfigFile.readInt(
                                        section, "Number of Data Points", 1200);

    invert = pConfigFile.readBoolean(section, "Invert Trace", false);

    pixelOffset = pConfigFile.readInt(section, "Pixel Offset", 0);

    pixelScaling = pConfigFile.readDouble(section, "Pixel Scaling", 1.0);

    preScaling = pConfigFile.readDouble(section, "PreScaling", 1.0);

    preOffset = pConfigFile.readDouble(section, "PreOffset", 0.0);

    higherMoreSevere = pConfigFile.readBoolean(
                                 section, "Higher Signal is More Severe", true);

    useVerticalBarToMarkEndMasks = pConfigFile.readBoolean(
                          section, "Use Vertical Bar to Mark End Masks", false);

    suppressTraceInEndMasks = pConfigFile.readBoolean(
                                 section, "Suppress Traces in End Masks", true);

    hdwVs.setPlotStyle(pConfigFile.readInt(section, "Plot Style", 0));

    hdwVs.setSimDataType(
                     pConfigFile.readInt(section, "Simulation Data Type", 0));

    //create the arrays to hold data points and flag/decoration info
    if (sizeOfDataBuffer > 100000) sizeOfDataBuffer = 100000;

    dataBuffer1 = new int[sizeOfDataBuffer];
    flagBuffer = new int[sizeOfDataBuffer];

    //for span mode, a second array is necessary - min/max data is plotted
    if (hdwVs.plotStyle == TraceHdwVars.SPAN)
        dataBuffer2 = new int[sizeOfDataBuffer];

}//end of Trace::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::setCanvas
//
// Stores a pointer to the canvas on which the traces are drawn.
//

public void setCanvas(JPanel pCanvas)
{

    canvas = pCanvas;

}//end of Trace::setCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

    canvasXLimit = canvas.getWidth() - 1;
    canvasYLimit = canvas.getHeight() - 1;

}//end of Trace::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::drawKeyLabel
//
// Draws the key label on the graphics device pG2.  The key label describes
// the trace and is drawn in the color of the trace.
//

public void drawKeyLabel(Graphics2D pG2)
{

    if (keyLabel.compareTo("<not visible>") == 0) return;

    //set the background color for the text to white so that most colors are
    //more visible

    HashMap<TextAttribute, Object> map = new HashMap<TextAttribute, Object>();

    Font font = new Font(Font.SERIF, Font.PLAIN, 12);

    map.put(TextAttribute.BACKGROUND, Color.WHITE);
    font = font.deriveFont(map);
    pG2.setFont(font);

    String keyString = " " + keyLabel + " ";

    pG2.setColor(traceColor);

    //draw the key text, outline it with a black rectangle, and store the
    //dimensions of the text so mouse clicks on the key can be detected

    //draw the text
    FontRenderContext frc = pG2.getFontRenderContext();
    TextLayout layout = new TextLayout(keyString, font, frc);
    layout.draw(pG2, keyXPosition, keyYPosition);

    //get the boundaries of the text
    keyBounds = layout.getBounds();
    keyBounds.setRect(keyBounds.getX() + keyXPosition,
                      keyBounds.getY() + keyYPosition,
                      keyBounds.getWidth(),
                      keyBounds.getHeight());

    //outline the text with a rectangle
    pG2.setColor(Color.BLACK);
    pG2.draw(keyBounds);

}//end of Trace::drawKeyLabel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::resetTrace
//
// Clears all data.
//
// Do not call resetTrace from the constructor because all data may not be
// available at that time.
//

public void resetTrace()
{

    plotVs.gridCounter = 0; //used to place grid marks

    plotVs.drawTrace = true; //draw trace when plotting data

    traceGlobals.bufOffset = 0; //left edge of screen starts at position 0

    traceGlobals.scrollCount = 0; //number of pixels chart has been scrolled

    //reset pointers to the start of the data buffer
    plotVs.prevPtr = 1;
    plotVs.bufPtr = 0;

    //reset the pointers used by the data collector
    //endPlotSlot is always one behind the beingFilledSlot
    beingFilledSlot = 1;
    inProcessSlot = beingFilledSlot;
    endPlotSlot = 0;

    //reset segment end pointers
    lastSegmentStartIndex = -1; lastSegmentEndIndex = -1;

    //pixel position on the screen where data is being plotted
    plotVs.pixPtr = -1;

    //set all data points to the maximum integer value - this value is used to
    //determine if a data point has been filled with data
    if (dataBuffer1 != null)
        for (int i = 0; i < dataBuffer1.length; i++){
            dataBuffer1[i] = Integer.MAX_VALUE;
            flagBuffer[i] = 0;
    }

    //set the position where the pointers start to non-default -- this is the
    //first data that is plotted unless it happens to be replaced by a peak
    //quickly

    dataBuffer1[1] = 0;

    //used in span mode
    if (dataBuffer2 != null)
        for (int i = 0; i < dataBuffer1.length; i++)
            dataBuffer2[i] = Integer.MAX_VALUE;

}//end of Trace::resetTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::markSegmentStart
//
// Resets the segmentStartedCounter and records the current buffer location.
// The counter is used to determine if a new segment has begun and thus may
// have data which needs to be saved.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

public void markSegmentStart()
{

    segmentStartCounter = 0;

    //set the segment separator flag in the flag following the current position

    int index = plotVs.bufPtr + 1;
    //the buffer is circular - start over at beginning
    if (index == sizeOfDataBuffer) index = 0;

    //set flag to display a separator bar at the start of the segment
    flagBuffer[index] |= 1 << 17;

    //record the buffer start position of the last segment
    lastSegmentStartIndex = index;

}//end of Trace::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::markSegmentEnd
//
// Records the current buffer position as the point where the current segment
// ends.  If the segment is to be saved, the save should occur after this
// function is called and before markSegmentStart is called for the next
// segment so the endpoints of the segment to be saved will still be valid.
//
// A separator bar is drawn for cases where the traces might be free running
// between segments, thus leaving a gap.  In that case, a bar at the start and
// end points is necessary to delineate between segment data and useless data
// in the gap.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//

public void markSegmentEnd()
{

    //set the segment separator flag in the flag following the current position

    int index = plotVs.bufPtr + 1;
    //the buffer is circular - start over at beginning
    if (index == sizeOfDataBuffer) index = 0;

    //set flag to display a separator bar at the end of the segment
    flagBuffer[index] |= 1 << 18;

    //record the buffer end position of the last segment
    lastSegmentEndIndex = index;

}//end of Trace::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::markEndMask
//
// Places a vertical bar on the graph to show the start or end flag mask.  This
// indicates the areas in which no flags are registered even if the trace
// breaks a threshold. This is used for the beginning and end of the piece to
// ignore false indications caused by piece entry or head settling.
//
// NOTE: End masks don't work well for UT units in which the head
//  moves across spinning pipe because when the head is dropped each transducer
//  is expected to start flagging at that time even though each is at a
//  different location.  Thus, each trace on a chart would have its own end
//  mask bar which would be confusing.  There is an option in the config file
//  to use trace suppression instead to mark the end mask area.
//
// NOTE: This function might be place the flag prematurely if the thread
//  drawing the trace gets behind data collection.  It is usually better for
//  the data collection / position tracking thread to set the flag bit.
//

public void markEndMask()
{

    int index = plotVs.bufPtr + 1;

    //set flag to display a separator bar at the start of the segment
    flagBuffer[index] |= 1 << 19;

}//end of Trace::markEndMask
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

public void saveSegment(BufferedWriter pOut) throws IOException
{

    pOut.write("[Trace]"); pOut.newLine();
    pOut.write("Trace Index=" + traceIndex); pOut.newLine();
    pOut.write("Trace Title=" + title); pOut.newLine();
    pOut.write("Trace Short Title=" + shortTitle); pOut.newLine();
    pOut.newLine();

    //catch unexpected case where start/stop are invalid and bail
    if (lastSegmentStartIndex < 0 || lastSegmentEndIndex < 0){
        pOut.write("Segment start and/or start invalid - no data saved.");
        pOut.newLine(); pOut.newLine();
        return;
    }

    //save all the data and flags in the segment

    int i = lastSegmentStartIndex;

    pOut.write("[Data Set 1]"); pOut.newLine(); //save the first data set

    while (i != lastSegmentEndIndex){
        pOut.write(Integer.toString(dataBuffer1[i])); //save the data set 1
        pOut.newLine();
        //increment to next buffer slot, wrap around because buffer is circular
        if (++i == sizeOfDataBuffer) i = 0;
    }

    pOut.write("[End of Set]"); pOut.newLine();

    //save the second data set if it exists
    //the second data set is only used for certain styles of plotting
    if (dataBuffer2 != null){

        i = lastSegmentStartIndex;

        pOut.write("[Data Set 2]"); pOut.newLine();

        //save the second data set if it exists
        while (i != lastSegmentEndIndex){
            pOut.write(Integer.toString(dataBuffer2[i])); //save the data set 2
            pOut.newLine();
            //increment to next buffer slot, wrap around as buffer is circular
            if (++i == sizeOfDataBuffer) i = 0;
        }

        pOut.write("[End of Set]"); pOut.newLine();

    }//if (dataBuffer2 != null)

    i = lastSegmentStartIndex;

    pOut.write("[Flags]"); pOut.newLine(); //save the flags

    while (i != lastSegmentEndIndex){
        pOut.write(Integer.toString(flagBuffer[i])); //save the flags
        pOut.newLine();
        //increment to next buffer slot, wrap around because buffer is circular
        if (++i == sizeOfDataBuffer) i = 0;
    }

    pOut.write("[End of Set]"); pOut.newLine();

    pOut.newLine(); //blank line

}//end of Trace::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::segmentStarted
//
// Checks to see if a segment has been started.  If the trace has moved
// a predetermined amount, it is assumed that a segment has been started.
//
// The trace must move more than a few counts to satisfy the start criteria.
// This is to ignore any small errors.
//

public boolean segmentStarted()
{

    if (segmentStartCounter >= 10) return true; else return false;

}//end of Trace::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::loadSegment
//
// Loads the data for a segment from pIn.  It is expected that the Trace
// section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Trace section, the [Trace] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    //handle entries for the trace itself
    String line = processTraceEntries(pIn, pLastLine);

    //read in "Data Set 1"
    line = processDataSeries(pIn, line, "[Data Set 1]", dataBuffer1);

    //if "Data Set 2" is in use, read it in
    if (dataBuffer2 != null)
        line = processDataSeries(pIn, line, "[Data Set 2]", dataBuffer2);

    //read in "Flags"
    line = processDataSeries(pIn, line, "[Flags]", flagBuffer);

    return(line);

}//end of Trace::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::processTraceEntries
//
// Processes the entries for the trace itself via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Trace section, the [Trace] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processTraceEntries(BufferedReader pIn, String pLastLine)
                                                             throws IOException

{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [Trace] tag, then skip ahead else read until
    // end of file reached or "[Trace]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, "[Trace]", "",  matchSet))
        success = true; //tag already found
    else
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, "[Trace]", "",  matchSet)){
                success = true; break;
            }
        }//while

    if (!success) throw new IOException(
           "The file could not be read - section not found for Chart Group "
               + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex);

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
        if (Viewer.matchAndParseInt(line, "Trace Index", -1, matchSet))
            traceIndexRead = matchSet.rInt1;

        //read the "Trace Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(line, "Trace Title", "", matchSet))
            titleRead = matchSet.rString1;

        //read the "Trace Short Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(line, "Trace Short Title", "", matchSet))
            shortTitleRead = matchSet.rString1;

    }

    //apply settings
    title = titleRead; shortTitle = shortTitleRead;

    if (!success) throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
                + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex);

    //if the index number in the file does not match the index number for this
    //threshold, abort the file read

    if (traceIndexRead != traceIndex) throw new IOException(
            "The file could not be read - section not found for Chart Group "
                + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex);

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Trace::processTraceEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::processDataSeries
//
// Processes a data series from pIn.  The series could be "Data Set 1",
// "Data Set 2", or "Flags" depending on the parameters passed in.
//
// The pStartTag string specifies the section start tag for the type of data
// expected and could be: "[Data Set 1]", "[Data Set 2]", or "[Flags]".  The
// pBuffer pointer should be set to the buffer associated with the data type.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For these sections, the [xxx] section start tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processDataSeries(BufferedReader pIn, String pLastLine,
                            String pStartTag, int[] pBuffer) throws IOException
{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [xxx] tag, then skip ahead else read until
    // end of file reached or "[xxx]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, pStartTag, "",  matchSet))
        success = true; //tag already found
    else
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, pStartTag, "",  matchSet)){
                success = true; break;
            }
        }//while

    if (!success) throw new IOException(
           "The file could not be read - section not found for Chart Group "
                + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex
                + " for " + pStartTag);

    //scan the first part of the section and parse its entries
    //these entries apply to the chart group itself

    int i = 0;
    success = false;
    while ((line = pIn.readLine()) != null){

        //stop when next section end tag reached (will start with [)
        if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
            success = true; break;
        }

        try{

            //convert the text to an integer and save in the buffer
            int data = Integer.parseInt(line);
            pBuffer[i++] = data;

            //catch buffer overflow
            if (i == pBuffer.length)
                throw new IOException(
                    "The file could not be read - too much data for Chart Group "
                    + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex
                    + " for " + pStartTag + " at data point " + i);

        }
        catch(NumberFormatException e){
            //catch error translating the text to an integer
            throw new IOException(
             "The file could not be read - corrupt data for Chart Group "
                 + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex
                 + " for " + pStartTag + " at data point " + i);
        }

    }//while ((line = pIn.readLine()) != null)

    if (!success) throw new IOException(
         "The file could not be read - missing end of section for Chart Group "
                 + chartGroup + " Chart " + chartIndex + " Trace " + traceIndex
                    + " for " + pStartTag);

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Trace::processDataSeries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::findMinValue
//
// Finds the minimum value in dataBuffer1.
//
// Finds the minimum value in dataBuffer1.  Search begins at pStart position
// in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and dataBuffer1.length
// to avoid errors.
//

public int findMinValue(int pStart, int pEnd)
{

    if (pStart < 0) pStart = 0;
    if (pStart >= dataBuffer1.length) pStart = dataBuffer1.length - 1;

    if (pEnd < 0) pEnd = 0;
    if (pEnd >= dataBuffer1.length) pEnd = dataBuffer1.length - 1;

    int peak = Integer.MAX_VALUE;

    for (int i = pStart; i < pEnd; i++){

        if (dataBuffer1[i] < peak) peak = dataBuffer1[i];

    }

    return(peak);

}//end of Trace::findMinValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::findMaxValue
//
// Finds the maximum value in dataBuffer1.  Search begins at pStart position
// in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and dataBuffer1.length
// to avoid errors.
//

public int findMaxValue(int pStart, int pEnd)
{

    if (pStart < 0) pStart = 0;
    if (pStart >= dataBuffer1.length) pStart = dataBuffer1.length - 1;

    if (pEnd < 0) pEnd = 0;
    if (pEnd >= dataBuffer1.length) pEnd = dataBuffer1.length - 1;

    int peak = Integer.MIN_VALUE;

    for (int i = pStart; i < pEnd; i++){

        if (dataBuffer1[i] > peak) peak = dataBuffer1[i];

    }

    return(peak);

}//end of Trace::findMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::newDataReady
//
// Checks to see if any new data is ready to be plotted.
//
// Returns true if new data is ready, false if not.
//

public boolean newDataReady()
{

    //if pointer has not been moved, no data ready
    if (plotVs.bufPtr == endPlotSlot) return false;

    //new data is ready
    return true;

}//end of Trace::newDataReady
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

public int plotNewData(Graphics2D pG2)
{

    //this variable is used to determine if a new segment has started and thus
    //has data which can be saved
    //debug mks - this needs to be decremented in cases where the traces are
    // reversed to erase data

    segmentStartCounter++;

    //plot the new point, using the plotVs variable set - this variable set
    //tracks new data to be plotted

    if (endPlotSlot > plotVs.bufPtr) return plotPoint(pG2, plotVs);

    if (endPlotSlot < plotVs.bufPtr) return erasePoint(pG2, plotVs);

    return(0);

}//end of Trace::plotNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::plotPoint
//
// Plots a single data point in the array.  Assumes new data has been added to
// the next slot in the buffer.
//
// All variables are passed via pVars, so different sets can be used depending
// on the context.
//
// Returns the value last plotted.  If plot style is SPAN, this value will be
// from the second data set.
//

public int plotPoint(Graphics2D pG2, PlotVars pVars)
{

    //increment to the next buffer position - it is assumed that new data is
    //ready there, if at the end of the buffer, wrap to the beginning.
    //the data buffer is usually larger than the screen so that multiple screens
    //of data can be recorded

    pVars.bufPtr++;
    if (pVars.bufPtr == dataBuffer1.length) pVars.bufPtr = 0;

    //increment the pixel pointer until it reaches the right edge, then shift
    //the screen left and keep pointer the same to create scrolling effect
    //the scrolling starts at canvasXLimit-10 to allow room for flags

    if (pVars.pixPtr < canvasXLimit-10) pVars.pixPtr++;
    else{
        //if this is the lead trace, shift the chart left and erase right slice
        if (leadTrace){
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

            traceGlobals.bufOffset++;
            if (traceGlobals.bufOffset == dataBuffer1.length)
                traceGlobals.bufOffset = 0;

            //track the number of pixels the chart has been scrolled - this is
            //used to determine the proper location of grid marks and other
            //decorations when the screen is repainted

            traceGlobals.scrollCount++;

        }//if (traceIndex == 0)

    }//else if (pVars.pixPtr...

    //if this is the lead trace draw the decorations
    if (leadTrace){
        for (int j = 0; j < numberOfThresholds; j++)
            thresholds[j].drawSlice(pG2, pVars.pixPtr);

        if (pVars.gridCounter++ == gridXSpacingT){
            drawGrid(pG2, pVars.pixPtr, canvasYLimit);
            pVars.gridCounter = 0;
        }

        //if segment start flag set, draw a vertical separator bar
        if ((flagBuffer[pVars.bufPtr] & 0x00020000) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if segment end flag set, draw a vertical separator bar
        if ((flagBuffer[pVars.bufPtr] & 0x00040000) != 0){
            pG2.setColor(gridColor);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

        //if end mask flag set and option enabled, draw a vertical separator bar
        if (useVerticalBarToMarkEndMasks
                            && (flagBuffer[pVars.bufPtr] & 0x00080000) != 0){
            pG2.setColor(Color.GREEN);
            pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, 0);
        }

    }// if (leadTrace)

    //apply offset, scaling, limits to y value
    //for span style, the high peaks and low peaks are in separate buffers in
    //the same index position

    if (hdwVs.plotStyle == TraceHdwVars.POINT_TO_POINT){
        pVars.y1 = dataBuffer1[pVars.prevPtr];
        pVars.y2 = dataBuffer1[pVars.bufPtr];
    }
    else if (hdwVs.plotStyle == TraceHdwVars.STICK){
        pVars.y1 = dataBuffer1[pVars.bufPtr];
        pVars.y2 = dataBuffer1[pVars.bufPtr]; //debug mks -- should one of these be zero to draw from the bottom?
    }
    else if (hdwVs.plotStyle == TraceHdwVars.SPAN){
        pVars.y1 = dataBuffer1[pVars.bufPtr];
        pVars.y2 = dataBuffer2[pVars.bufPtr];
    }

    //save current pointers for use during next loop
    pVars.prevPtr = pVars.bufPtr;
    //save the value plotted so it can be returned on exit
    int lastPlotted = pVars.y2;

    //set flag to stop drawing traces if a data point is encountered equal to
    //MAX_VALUE - this is the end of the valid data - further calls to this
    //function will not draw a trace until the flag is set back to true - this
    //allows the decorations to be drawn across the full chart even though the
    //data is not defined

    if (dataBuffer1[pVars.bufPtr] == Integer.MAX_VALUE) pVars.drawTrace = false;

    //if the drawTrace flag is false, exit without drawing them - this allows
    //the decorations to be drawn before data is available so the grid and
    //thresholds are displayed

    if (!pVars.drawTrace) return(lastPlotted);

    //prepare to draw the trace

    //apply pixel scaling
    pVars.y1 *= pixelScaling; pVars.y2 *= pixelScaling;

    //apply offset
    pVars.y1 += pixelOffset; pVars.y2 += pixelOffset;

    //apply limits
    if (pVars.y1 > canvasYLimit) pVars.y1 = canvasYLimit;
    if (pVars.y2 > canvasYLimit) pVars.y2 = canvasYLimit;

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

    if (hdwVs.plotStyle == TraceHdwVars.POINT_TO_POINT)
        pG2.drawLine(pVars.pixPtr-1, pVars.y1, pVars.pixPtr, pVars.y2);
    else
    if (hdwVs.plotStyle == TraceHdwVars.STICK)
        pG2.drawLine(pVars.pixPtr, canvasYLimit, pVars.pixPtr, pVars.y2);
    else
    if (hdwVs.plotStyle == TraceHdwVars.SPAN)
        pG2.drawLine(pVars.pixPtr, pVars.y1, pVars.pixPtr, pVars.y2);

    //if there is a flag set for this data point then draw it - threshold
    //indices are shifted by two as 0 = no flag and 1 = user flag
    if ((flagThreshold =
                       ((flagBuffer[pVars.bufPtr] & 0x0000fe00) >> 9)-2) >= 0){

        int flagY = pVars.y2; //draw flag at height of peak for non-SPAN styles

        //for span mode, draw flag at min or max depending on bit in flagBuffer
        if (hdwVs.plotStyle == TraceHdwVars.SPAN){
            if ((flagBuffer[pVars.bufPtr] & 0x00010000) == 0) flagY = pVars.y1;
        }//if (hdwVs.plotStyle ==

        thresholds[flagThreshold].drawFlag(pG2, pVars.pixPtr, flagY);
    }
    else
        if (flagThreshold == -1) drawUserFlag(pG2, pVars.pixPtr, pVars.y2);

    return(lastPlotted);

}//end of Trace::plotPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::erasesPoint
//
// Erases a single data point in the array.
//
// All variables are passed via pVars, so different sets can be used depending
// on the context.
//

public int erasePoint(Graphics2D pG2, PlotVars pVars)
{

    //decrement to the previous buffer position
    //if at the beginning of the buffer, wrap to the end

    pVars.bufPtr--;
    if (pVars.bufPtr == -1) pVars.bufPtr = dataBuffer1.length-1;

    //debug mks -- next section for scrolling in reverse NOT tested well!

    //decrement the pixel pointer until it reaches the left edge, then shift the
    //screen right and keep pointer the same to create scrolling effect

    if (pVars.pixPtr > 0) pVars.pixPtr--;
    else{
        //if this is trailing trace, shift the chart right and erase left slice
        if (trailTrace){
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

            traceGlobals.bufOffset--;
            if (traceGlobals.bufOffset == -1)
                traceGlobals.bufOffset = dataBuffer1.length-1;

            //track the number of pixels the chart has been scrolled - this is
            //used to determine the proper location of grid marks and other
            //decorations when the screen is repainted

            traceGlobals.scrollCount--;

        }//if (traceIndex == 0)

    }//else if (pixPtr <...


    pG2.setColor(backgroundColor);
    pG2.drawLine(pVars.pixPtr+1, 0, pVars.pixPtr+1, canvas.getHeight());

    //debug mks -- next values necessary? need to return lastPlotted?

    //save current pointers for use during next loop
    pVars.prevPtr = pVars.bufPtr;
    //save the value plotted so it can be returned on exit
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
// Trace::getTitle
//
// Returns the trace title.
//

public String getTitle()
{

    return (title);

}//end of Trace::getTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getDataBuffer1
//
// Returns a reference to dataBuffer1.
//

public int[] getDataBuffer1()
{

    return(dataBuffer1);

}//end of Trace::getDataBuffer1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::getDataWidth
//
// Returns the index of the last valid data point.
//
// Returns -1 if no data found.
//

public int getDataWidth()
{

    int endOfData = -1;

    //NOTE: Start at dataBuffer1.length - 2 as the last element seems to be
    // filled with zero -- why is this? -- fix?

    for (int i = (dataBuffer1.length - 2); i > 0; i--){
        if (dataBuffer1[i] != Integer.MAX_VALUE){
            endOfData = i;
            break;
        }
    }//for (int i = (pBuffer.length - 1); i <= 0; i--){

    return(endOfData);

}//end of Trace::getDataWidth
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
// Trace::paintComponent
//
// Refreshes the canvas using the data in the buffers.
//

public void paintComponent(Graphics2D pG2)
{

    // the repaintVS variable set is used here to avoid conflict with the
    // plotVs set which tracks plotting of new data

    //use bufOffset from plotVs as that value is adjusted when the chart is
    //scrolled by the addNewData code
    //bufPtr gets incremented on entry to plotPoint so it will point to the next
    //location first time through
    repaintVs.bufPtr = traceGlobals.bufOffset;

    repaintVs.prevPtr = traceGlobals.bufOffset;

    repaintVs.pixPtr = -1;

    //for repainting, the gridCounter starts at one to sync up with drawing by
    //the plotNewData code

    repaintVs.gridCounter = traceGlobals.scrollCount % gridXSpacing;

    //start with drawing trace allowed - will be set false by plotPoint if
    //a data point is reached which is set to MAX_VALUE which denotes the end of
    //the valid data

    repaintVs.drawTrace = true;

    //stop short of the end of the screen to avoid triggering chart scroll
    //in the plotPoint function

    int stop = canvasXLimit-10;

    for (int i = 0; i < stop; i++){
        //plot each point, the plotPoint function increments all pointers
        plotPoint(pG2, repaintVs);
    }

}//end of Trace::paintComponent
//-----------------------------------------------------------------------------

}//end of class Trace
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------