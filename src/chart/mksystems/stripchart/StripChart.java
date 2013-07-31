/******************************************************************************
* Title: StripChart.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class creates and handles a strip chart.
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
import chart.mksystems.hardware.TraceValueCalculator;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.border.*;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ValueDisplay

class ValueDisplay extends Object {

    int labelXPos = 500, labelYPos = 23;
    int xPos = labelXPos + 100;
    int yPos = labelYPos;
    String label = "";
    String decPlaces = "0.000";
    DecimalFormat decimalFormat;
    Color textColor, backgroundColor;

    public int iValue = 0;
    int iPrevValue = -1;

    public double dValue = 0;
    double dPrevValue = -1;

    public String sValue = "";
    String sPrevValue = "";

    static int UNDEFINED = 0;
    static int INTEGER = 1;
    static int DOUBLE = 2;
    static int STRING = 3;

    int valueType = UNDEFINED;

//-----------------------------------------------------------------------------
// ValueDisplay::ValueDisplay (constructor)
//
// This class handles drawing a value with a label.  The value is only drawn
// if it has changed.  The previous value is erased before drawing the new
// value.
//
// Multiple update functions are provided, one for integers, one for doubles,
// one for Strings, etc.
//
// After one of these is called, a flag is set to specify which type of
// variable is being handled by this object.  This can be changed at any time
// by calling one of the other update function.
//

public ValueDisplay(int pLabelXPos, int pLabelYPos, int pXPos, int pYPos,
                    String pLabel, String pDecPlaces, Color pTextColor,
                    Color pBackgroundColor)
{


    labelXPos = pLabelXPos; labelYPos = pLabelYPos;
    xPos = pXPos; yPos = pYPos;
    label = pLabel; decPlaces = pDecPlaces;
    textColor = pTextColor; backgroundColor = pBackgroundColor;

    decimalFormat = new DecimalFormat(decPlaces);

}//end of ValueDisplay::ValueDisplay (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ValueDisplay::paint
//
// Draws the label and the value regardless of whether or not the value has
// changed.  The value type which is redrawn is determined by the current
// values of valueIsInt, valueIsDouble, etc.
//

void paint(Graphics2D pG2)
{

    pG2.setColor(textColor);
    pG2.drawString(label, labelXPos, labelYPos);

    //draw the appropriate value
    if (valueType == INTEGER) {updateInt(pG2, iValue, true);}
    if (valueType == DOUBLE)  {updateDouble(pG2, dValue, true);}
    if (valueType == STRING)  {updateString(pG2, sValue, true);}

}//end of ValueDisplay::paint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ValueDisplay::updateInt
//
// Draws the int value if it has changed.  The previous value is erased first.
//
// If pForceUPdate is true, the value will be drawn even if it has not changed.
//

void updateInt(Graphics2D pG2, int pNewValue, boolean pForceUpdate)
{

    valueType = INTEGER; //variable type for this object is now int

    iValue = pNewValue;

    if ((iValue != iPrevValue) || pForceUpdate){
        //erase the previous value
        pG2.setColor(backgroundColor);
        pG2.drawString(Integer.toString(iPrevValue), xPos, yPos);
        //draw the new value
        pG2.setColor(textColor);
        pG2.drawString(Integer.toString(iValue), xPos, yPos);
        iPrevValue = iValue;
    }

}//end of ValueDisplay::updateInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ValueDisplay::updateDouble
//
// Draws the double value if it has changed. The previous value is erased first.
//
// If pForceUPdate is true, the value will be drawn even if it has not changed.
//

void updateDouble(Graphics2D pG2, double pNewValue, boolean pForceUpdate)
{

    valueType = DOUBLE; //variable type for this object is now double

    dValue = pNewValue;

    if ((dValue != dPrevValue) || pForceUpdate){
        //erase the previous value
        pG2.setColor(backgroundColor);
        pG2.drawString(decimalFormat.format(dPrevValue), xPos, yPos);
        //draw the new value
        pG2.setColor(textColor);
        pG2.drawString(decimalFormat.format(dValue), xPos, yPos);
        dPrevValue = dValue;
    }

}//end of ValueDisplay::updateDouble
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ValueDisplay::updateString
//
// Draws the string value if it has changed. The previous value is erased first.
//
// If pForceUPdate is true, the value will be drawn even if it has not changed.
//

void updateString(Graphics2D pG2, String pNewValue, boolean pForceUpdate)
{

    valueType = STRING; //variable type for this object is now string

    sValue = pNewValue;

    if (!sValue.equals(sPrevValue) || pForceUpdate){
        //erase the previous value
        pG2.setColor(backgroundColor);
        pG2.drawString(sPrevValue, xPos, yPos);
        //draw the new value
        pG2.setColor(textColor);
        pG2.drawString(sValue, xPos, yPos);
        sPrevValue = sValue;
    }

}//end of ValueDisplay::updateString
//-----------------------------------------------------------------------------


}//end of class ValueDisplay
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ChartCanvas
//
// This panel is used to draw the profile plot.
// This class actually does all the work for the StripChart class.
//

class ChartCanvas extends JPanel {

    Settings settings;
    Color backgroundColor;
    Color gridColor;
    int width, height;

    int numberOfTraces;
    Trace traces[];
    public int leadingTrace, trailingTrace;
    int numberOfThresholds;
    Threshold thresholds[];

    MouseMotionListener mouseMotionListener;

    public BufferedImage imageBuffer;

    int peakChannel;
    double runningValue;

//-----------------------------------------------------------------------------
// ChartCanvas::ChartCanvas (constructor)
//

public ChartCanvas(Settings pSettings, int pWidth, int pHeight,
                               Color pBackgroundColor, Color pGridColor,
                               int pNumberOfTraces, Trace[] pTraces,
                               int pNumberOfThresholds, Threshold[] pThresholds,
                               MouseMotionListener pMouseMotionListener)
{

    settings = pSettings;
    width = pWidth; height = pHeight;

    backgroundColor = pBackgroundColor; gridColor = pGridColor;
    numberOfTraces = pNumberOfTraces; traces = pTraces;
    numberOfThresholds = pNumberOfThresholds; thresholds = pThresholds;
    mouseMotionListener = pMouseMotionListener;
    addMouseMotionListener(mouseMotionListener);

    setOpaque(true);

    //set the min, preferred, and max sizes
    setSizes(width, height);

}//end of ChartCanvas::ChartCanvas (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::setSizes
//
// Sets the minimum, preferred, and maximum sizes of the canvas.
//

private void setSizes(int pWidth, int pHeight)
{

    setMinimumSize(new Dimension(pWidth, pHeight));
    setPreferredSize(new Dimension(pWidth, pHeight));
    setMaximumSize(new Dimension(pWidth, pHeight));

}//end of ChartCanvas::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::resetCanvas
//
// Erases the chart area and clears all data in the trace buffers.
//

public void resetCanvas()
{

    for (int i = 0; i < numberOfTraces; i++) {traces[i].resetTrace();}

}//end of ChartCanvas::resetCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::paintCanvas
//
// Paints the canvas with traces, thresholds, masks, flags etc.
//

public void paintCanvas(Graphics2D pG2)
{

    //if the canvas is hidden, don't try to repaint it
    if (!isVisible()) {return;}

    //erase canvas
    pG2.setColor(backgroundColor);
    pG2.fillRect(0, 0, getWidth(), getHeight());

    //draw the thresholds
    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].paint(pG2, 0, getWidth()-1);
    }

    //paint all the traces
    for (int i = 0; i < numberOfTraces; i++) {traces[i].paintComponent(pG2);}

}//end of ChartCanvas::paintCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::plotData
//
// Plots any new data in the trace arrays
//

public void plotData()
{

    //if the canvas is hidden, don't try to plot the data
    if (!isVisible()) {return;}

    //NOTE: All traces are updated only when new data has been added for the
    // leading trace. Since the leading trace shifts the screen and draws the
    // decorations, no trace can be drawn ahead of it.  The hardware should
    // fill the buffers for all traces on a given chart in lockstep, data in
    // the traces other than the leading trace can be delayed.  If the sensors
    // are offset in their mounting, the leading trace should always be the
    // lead sensor.

    //while there is data to be plotted for the leading trace, plot data for all
    //traces which have data

    while (traces[leadingTrace].newDataIsReady()){

        //find the hardware channel which produced the worst case value - this
        //requires that the worst case trace be found - it is assumed that all
        //traces on the chart have the same direction of severity - won't work
        //on a chart with a min trace(s) and a max trace(s)
        //look at trace 0 setting to determine which direction is more severe
        int peak;
        peak =
           (traces[0].higherMoreSevere) ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        int lastValue;

        //plot any new data in the traces
        //check for new data on each trace because delayed traces might not have
        //data at the same time
        for (int i = 0; i < numberOfTraces; i++) {
            if (traces[i].newDataIsReady()){
                lastValue = traces[i].plotNewData((Graphics2D)getGraphics());

                //catch the trace with the worst value and record its channel
                if (traces[0].higherMoreSevere) {
                    if (lastValue > peak){
                        peak = lastValue; peakChannel = traces[i].peakChannel;
                    }
                    else
                    if (lastValue < peak){
                        peak = lastValue; peakChannel = traces[i].peakChannel;
                    }
                }

                //store the wall thickness reading
                runningValue = traces[i].wallThickness;

            }// if (traces[i].newDataReady())
        }// for (int i = 0; i < numberOfTraces; i++)
    }//while (traces[leadingTrace].newDataReady())

}//end of ChartCanvas::plotData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::paintComponent
//

@Override
public void paintComponent(Graphics g)

{

    Graphics2D g2 = (Graphics2D) g;

    super.paintComponent(g2); //paint background

    paintCanvas(g2);

}//end of ChartCanvas::paintComponent
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::print
//
// Renders the canvas on pG which should be a graphics context for printing.
//
// The canvas is rendered at the position on the paper as it would appear
// on the screen.  The screen positions are determined by things such as
// the layout manager and the containers such as panels.  The print context
// does not understand these positioning devices, so each canvas must be
// explicitly positioned.  Using their positions on the screen provides a good
// arrangment as it allows the views to be similar.
//

public void print(Graphics2D pG2)

{

    paintCanvas(pG2);

}//end of ChartCanvas::print
//-----------------------------------------------------------------------------

}//end of class ChartCanvas
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class StripChart
//
// This class creates and controls a plot display.
//

public class StripChart extends JPanel implements MouseListener,
                                                           MouseMotionListener{

    ChartCanvas canvas;
    public TitledBorder titledBorder;
    Settings settings;
    IniFile configFile;
    int chartGroup;
    int chartIndex;
    TraceValueCalculator traceValueCalculator;
    Hardware hardware;
    Color borderColor;
    TraceGlobals traceGlobals;
    public int chartHeight;

    boolean displayPeakChannel;
    ValueDisplay peakChannelDisplay;
    StringBuilder lastFlaggedText;
    boolean displayLastFlaggedChannel;
    ValueDisplay lastFlaggedChannelDisplay;
    public int lastFlaggedChannel = -1;
    int prevLastFlaggedChannel = -1;
    public int lastFlaggedClockPos = 0;
    int prevLastFlaggedClockPos = 0;
    boolean displayPrevMinWall;
    ValueDisplay prevMinWallDisplay;
    boolean displayRunningValue;
    ValueDisplay runningValueDisplay;
    boolean displayComputedAtCursor;
    ValueDisplay computedAtCursorDisplay;
    boolean displayChartHeightAtCursor;
    ValueDisplay chartHeightAtCursorDisplay;
    boolean displayLinearPositionAtCursor;
    ValueDisplay linearPositionAtCursorDisplay;

    public String title, shortTitle;
    int numberOfTraces;
    Trace[] traces;
    int numberOfThresholds;
    Threshold[] thresholds;
    Color backgroundColor;
    Color gridColor;
    int gridXSpacing;
    //why do we have mask boolean values?
    boolean leadMask, trailMask;
    double leadMaskPos, trailMaskPos;
    Color maskColor;
    Color separatorColor;
    public int lastAScanChannel = 0;
    boolean chartSizeEqualsBufferSize;

    ActionListener actionListener;

//-----------------------------------------------------------------------------
// StripChart::StripChart (constructor)
//
// If chartSizeEqualsBufferSize is false, the chart wdith will be set by
// value read from the config file.  This is normal for the main window
// which may not have scrolling panes to view wide charts.
//
// If chartSizeEqualsBufferSize is true, the chart width will be set to match
// the buffer size in trace 0.  It is assumed that all traces will have the
// same buffer size.
//

public StripChart(Settings pSettings, IniFile pConfigFile, int pChartGroup,
       int pChartIndex, Hardware pHardware, ActionListener pActionListener,
       boolean pChartSizeEqualsBufferSize,
        TraceValueCalculator pTraceValueCalculator)
{

    settings = pSettings; configFile = pConfigFile; chartIndex = pChartIndex;
    chartGroup = pChartGroup;
    hardware = pHardware; actionListener = pActionListener;
    traceValueCalculator = pTraceValueCalculator;
    traceGlobals = new TraceGlobals();
    chartSizeEqualsBufferSize = pChartSizeEqualsBufferSize;

    //set up the main panel - this panel does nothing more than provide a title
    //border and a spacing border
    setOpaque(true);

    //listen for mouse events on the strip chart
    addMouseListener(this);

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

    // if enabled by the config file, create object to display values related to
    // the traces

    // the positions, labels, and decimal formats could be loaded from the
    // config file in the future

    if (displayPeakChannel) {
        peakChannelDisplay =  new ValueDisplay(250, 23, 350, 23,
                           "Peak Channel:", "0.000", Color.BLACK, borderColor);
    }

    if (displayLastFlaggedChannel){
        lastFlaggedChannelDisplay =  new ValueDisplay(
            250, 23, 380, 23, "Last Flagged Channel:", "0.000",
                                                     Color.BLACK, borderColor);
        //a StringBuilder is used to avoid creating new strings during time
        //critical code and thus creating excess garbage collection
        lastFlaggedText = new StringBuilder(50);
    }

    if (displayPrevMinWall) {
        prevMinWallDisplay =  new ValueDisplay(250, 23, 355, 23,
                     "Previous Wall Min:", "0.000", Color.BLACK, borderColor);
    }

    if (displayRunningValue) {
        runningValueDisplay = new ValueDisplay( 500, 23, 600, 23,
                       "Wall Thickness:", "0.000", Color.BLACK, borderColor);
    }

    if (displayComputedAtCursor) {
        computedAtCursorDisplay = new ValueDisplay(
           650, 23, 720, 23,  "At Cursor:", "0.000", Color.BLACK, borderColor);
    }

    if (displayChartHeightAtCursor) {
        chartHeightAtCursorDisplay  = new ValueDisplay(
              800, 23, 865, 23,  "Amplitude:", "0", Color.BLACK, borderColor);
    }

    if (displayLinearPositionAtCursor) {
        linearPositionAtCursorDisplay  = new ValueDisplay( 950, 23, 1050, 23,
                        "Linear Position:", "0.0", Color.BLACK, borderColor);
    }

}//end of StripChart::StripChart (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::configure
//
// Loads configuration settings from the configuration.ini file.  These set
// the number of charts, traces, thresholds, channels, positions, colors, etc.
// The various child objects are then created as specified by the config data.
//
// Only configuration data for the StripChart itself is loaded here.  Each
// child object should be allowed to load its own data.
//

private void configure(IniFile pConfigFile)
{

    String section = "Chart Group " + (chartGroup + 1)
                                          + " Strip Chart " + (chartIndex + 1);

    title = pConfigFile.readString(section, "Title", "Chart " + (chartIndex+1));

    shortTitle = pConfigFile.readString(
                               section, "Short Title", "Ch " + (chartIndex+1));

    borderColor = new Color(238,238,238);

    //read colors first so they can be passed to other objects

    backgroundColor = pConfigFile.readColor(
                        section, "Background Color", new Color(238, 238, 238));

    gridColor = pConfigFile.readColor(section, "Grid Color", Color.BLACK);

    gridXSpacing = pConfigFile.readInt(section, "Grid X Spacing", 10);

    leadMask = pConfigFile.readBoolean(section, "Leading Mask", true);

    trailMask = pConfigFile.readBoolean(section, "Trailing Mask", true);

    maskColor = pConfigFile.readColor(section, "Mask Color", Color.BLACK);

    separatorColor =
          pConfigFile.readColor(section, "Piece Separator Color", Color.BLACK);

    displayPeakChannel =
               pConfigFile.readBoolean(section, "Display Peak Channel", false);

    displayLastFlaggedChannel =
       pConfigFile.readBoolean(section, "Display Last Flagged Channel", false);

    displayPrevMinWall = pConfigFile.readBoolean(
             section, "Display Minimum Wall From Last Finished Piece", false);

    displayRunningValue =
              pConfigFile.readBoolean(section, "Display Running Value", false);

    displayComputedAtCursor = pConfigFile.readBoolean(section,
               "Display Computed Value Represented by Cursor Position", false);

    displayChartHeightAtCursor = pConfigFile.readBoolean(section,
      "Display Chart Height Percentage Represented by Cursor Position", false);

    displayLinearPositionAtCursor = pConfigFile.readBoolean(section,
        "Display Chart Linear Position Represented by Cursor Position", false);

    numberOfThresholds =
                       pConfigFile.readInt(section, "Number of Thresholds", 1);

    //read the configuration file and create/setup the thresholds
    configureThresholds(configFile);

    numberOfTraces = pConfigFile.readInt(section, "Number of Traces", 1);

    //read the configuration file and create/setup the traces
    configureTraces(configFile);

    setBorder(titledBorder = BorderFactory.createTitledBorder(title));

    int chartWidth = pConfigFile.readInt(section, "Width", 1000);

    chartHeight = pConfigFile.readInt(section, "Height", 100);

    //if flag is true, make chart wide enough to hold all data in the trace
    // see header notes in constructor for more info

    if (chartSizeEqualsBufferSize) {
        chartWidth = traces[0].traceData.sizeOfDataBuffer;
    }

    //create a Canvas object to be placed on the main panel - the Canvas object
    //provides a panel and methods for drawing data - all the work is actually
    //done by the Canvas object
    canvas = new ChartCanvas(settings, chartWidth, chartHeight,
                     backgroundColor, gridColor, numberOfTraces, traces,
                     numberOfThresholds, thresholds, this);

    //default to trace 0 as the leading trace for now so the chart decorations
    //will be drawn
    canvas.leadingTrace = 0;

    //listen for mouse events on the canvas
    canvas.addMouseListener(this);
    add(canvas);

    //give all traces a link to their canvas
    for (int i = 0; i < numberOfTraces; i++) {traces[i].setCanvas(canvas);}

    //give all thresholds a link to their canvas
    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].setCanvas(canvas);
    }

}//end of StripChart::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::configureTraces
//
// Loads configuration settings from the configuration.ini file relating to
// the traces and creates/sets them up.
//

private void configureTraces(IniFile pConfigFile)
{

    //create an array of traces per the config file setting
    if (numberOfTraces > 0){

        //protect against too many items
        if (numberOfTraces > 100) {numberOfTraces = 100;}

        traces = new Trace[numberOfTraces];

        for (int i = 0; i < numberOfTraces; i++){ traces[i] =
           new Trace(settings, configFile, chartGroup, this, chartIndex, i,
                 traceGlobals, backgroundColor, gridColor, gridXSpacing,
                                                        thresholds, hardware);
           traces[i].init();
        }

        //default to trace 0 as the leading trace for now so the chart
        //decorations will be drawn
        traces[0].leadTrace = true;

    }//if (numberOfTraces > 0)

}//end of StripChart::configureTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::configureThresholds
//
// Loads configuration settings from the configuration.ini file relating to
// the thresholds and creates/sets them up.
//

private void configureThresholds(IniFile pConfigFile)
{

    //create an array of thresholds per the config file setting
    if (numberOfThresholds > 0){

        //protect against too many items
        if (numberOfThresholds > 100) {numberOfThresholds = 100;}

        thresholds = new Threshold[numberOfThresholds];

        //wip mks -- change configFile below to pConfigFile

        for (int i = 0; i < numberOfThresholds; i++) {
            thresholds[i] = new Threshold(settings, configFile, chartGroup,
                                                                chartIndex, i);
        }

    }//if (numberOfThresholds > 0)

}//end of StripChart::configureThresholds
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getNumberOfThresholds
//
// Returns the number of thresholds.
//

public int getNumberOfThresholds()
{

    return numberOfThresholds;

}//end of StripChart::getNumberOfThresholds
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getThreshold
//
// Returns the threshold indexed by pWhich.
//

public Threshold getThreshold(int pWhich)
{

    return thresholds[pWhich];

}//end of StripChart::getThreshold
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::setThresholdLevel
//
// Sets the level for the threshold indexed by pWhich.
//

public void setThresholdLevel(int pWhich, int pLevel)
{

    thresholds[pWhich].setThresholdLevel(pLevel);

}//end of StripChart::setThresholdLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getThresholdLevel
//
// Returns the level value for the threshold indexed by pWhich.
//

public int getThresholdLevel(int pWhich)
{

    return thresholds[pWhich].thresholdLevel;

}//end of StripChart::getThresholdLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getThresholdColor
//
// Returns the color for the threshold indexed by pWhich.
//

public Color getThresholdColor(int pWhich)
{

    return thresholds[pWhich].thresholdColor;

}//end of StripChart::getThresholdColor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

    for (int i = 0; i < numberOfTraces; i++) {traces[i].handleSizeChanges();}

    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].handleSizeChanges();
    }

}//end of StripChart::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::paintComponent
//

@Override
public void paintComponent(Graphics g)
{

    Graphics2D g2 = (Graphics2D) g;

    super.paintComponent(g2); //paint background

    //draw these labels before calling drawKeyLabel because that function
    //changes the font

    //add one to the peak channel to switch from 0 based counting
    if (displayPeakChannel) {peakChannelDisplay.paint((Graphics2D) g2);}

    if (displayLastFlaggedChannel) {
        lastFlaggedChannelDisplay.paint((Graphics2D) g2);
    }

    if (displayPrevMinWall) {prevMinWallDisplay.paint((Graphics2D) g2);}

    if (displayRunningValue) {runningValueDisplay.paint((Graphics2D) g2);}

    if (displayComputedAtCursor) {
        computedAtCursorDisplay.paint((Graphics2D) g2);
    }

    if (displayChartHeightAtCursor) {
        chartHeightAtCursorDisplay.paint((Graphics2D) g2);
    }

    if (displayLinearPositionAtCursor) {
        linearPositionAtCursorDisplay.paint((Graphics2D) g2);
    }

    //draw the keys for the different traces to show which trace is what - each
    //key is a label describing the trace and drawn in the color of the trace
    for (int i = 0; i < numberOfTraces; i++) {traces[i].drawKeyLabel(g2);}

}//end of StripChart::paintComponent
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::repaintCanvas
//
// Repaints the canvas.
//

public void repaintCanvas()
{

    canvas.paintComponent(canvas.getGraphics());

}//end of StripChart::repaintCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::plotData
//
// Plots any new data in the trace arrays
//

public void plotData()
{

    canvas.plotData();

    //if enabled, display the channel which is supplying the peak value
    if (displayPeakChannel) {
        peakChannelDisplay.updateString((Graphics2D) getGraphics(),
                      hardware.getChannels()[canvas.peakChannel].title, false);
    }

    //if enabled and the channel or clock has changed, update the display for
    //the channel which was last flagged

    if (displayLastFlaggedChannel
        && (lastFlaggedChannel != prevLastFlaggedChannel
                    || lastFlaggedClockPos != prevLastFlaggedClockPos)){

        prevLastFlaggedChannel = lastFlaggedChannel;
        prevLastFlaggedClockPos = lastFlaggedClockPos;

        if (lastFlaggedChannel == -1) {
            lastFlaggedText.setLength(0);
        } //display nothing if channel not set
        else{
            //use multiple appends rather than the + operator to combine the
            //strings as it is faster
            lastFlaggedText.setLength(0);
            lastFlaggedText.append(
                  hardware.getChannels()[lastFlaggedChannel].title);
            lastFlaggedText.append(" ~ ");
            lastFlaggedText.append(lastFlaggedClockPos);
        }

        lastFlaggedChannelDisplay.updateString((Graphics2D) getGraphics(),
                                            lastFlaggedText.toString(), false);
    }

    //if enabled, display the current value of the trace height
    if (displayRunningValue) {
        runningValueDisplay.updateDouble(
                (Graphics2D) getGraphics(), canvas.runningValue, false);
    }

}//end of StripChart::plotData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::updatePreviousMinWallValue
//
// Updates the display which shows the minimum wall for the previous joint.
// If that display is not enabled then does nothing.
//

public void updatePreviousMinWallValue(double pMinWall)
{

    if (displayPrevMinWall) {
        prevMinWallDisplay.updateDouble(
                                (Graphics2D) getGraphics(), pMinWall, false);
    }

}//end of StripChart::updatePreviousMinWallValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::resetChart
//
// Erases the chart area and clears all data.
//

public void resetChart()
{

    Graphics2D g2 = (Graphics2D) canvas.getGraphics();

    //reset all chart and trace data
    canvas.resetCanvas();

    repaint();

}//end of StripChart::resetChart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::markSegmentStart
//
// Resets the counter which is used to determine if a new segment has begun
// and records the start position.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

public void markSegmentStart()
{

    for (int i = 0; i < numberOfTraces; i++) {
        traces[i].traceData.markSegmentStart();
    }

}//end of StripChart::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::markSegmentEnd
//
// Marks the buffer location of the end of the current segment.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//
// This function should be called before saving the data so the end points
// of the data to be saved are known.
//

public void markSegmentEnd()
{

    for (int i = 0; i < numberOfTraces; i++) {
        traces[i].traceData.markSegmentEnd();
    }

}//end of StripChart::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::saveSegment
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

    pOut.write("[Chart]"); pOut.newLine();
    pOut.write("Chart Index=" + chartIndex); pOut.newLine();
    pOut.write("Chart Title=" + title); pOut.newLine();
    pOut.write("Chart Short Title=" + shortTitle); pOut.newLine();
    pOut.newLine();
    pOut.write(
            "Note that the Chart Title and Short Title may have been changed");
    pOut.newLine();
    pOut.write(
            "by the user, so the text displayed on the screen may not match");
    pOut.newLine();
    pOut.write("the values shown here.");
    pOut.newLine(); pOut.newLine();

    pOut.write("Chart is Visible=" + isChartVisible()); //save visibility flag
    pOut.newLine(); pOut.newLine();

    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].saveSegment(pOut);
    }

    for (int i = 0; i < numberOfTraces; i++) {traces[i].saveSegment(pOut);}

}//end of StripChart::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::segmentStarted
//
// Checks to see if a segment has been started and thus may have data which
// needs to be saved.
//

public boolean segmentStarted()
{

    for (int i = 0; i < numberOfTraces; i++) {
        if (traces[i].traceData.segmentStarted()) {
            return(true);
        }
    }

    return(false);

}//end of StripChart::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::loadSegment
//
// Loads the data for a segment from pIn.  It is expected that the StripChart
// section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// The [Chart] tag may or may not have already been read from the file by the
// code handling the previous section.  If it has been read, the line containing
// the tag should be passed in via pLastLine.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    //handle entries for the strip chart itself
    String line = processStripChartEntries(pIn, pLastLine);

    //allow each threshold to load data, passing the last line read to the next
    //call each time

    for (int i = 0; i < numberOfThresholds; i++) {
        line = thresholds[i].loadSegment(pIn, line);
    }

    for (int i = 0; i < numberOfTraces; i++) {
        line = traces[i].loadSegment(pIn, line);
    }

    return(line);

}//end of StripChart::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::processStripChartEntries
//
// Processes the entries for the strip chart itself via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the StripChart section, the [Chart] tag may or may not have already been
// read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processStripChartEntries(BufferedReader pIn, String pLastLine)
                                                             throws IOException
{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [Chart] tag, then skip ahead else read until
    //end of file reached or "[Chart]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, "[Chart]", "",  matchSet)) {
        success = true;
    } //tag already found
    else {
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, "[Chart]", "",  matchSet)){
                success = true; break;
            }
        }//while
    }//else

    if (!success) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
                                       + chartGroup + " Chart " + chartIndex);
    }

    //set defaults
    int chartIndexRead = -1;
    String titleRead = "", shortTitleRead = "";
    boolean visibleRead = true;

    //scan the first part of the section and parse its entries
    //these entries apply to the chart group itself

    success = false;
    while ((line = pIn.readLine()) != null){

        //stop when next section tag reached (will start with [)
        if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
            success = true; break;
        }

        //read the "Chart Index" entry - if not found, default to -1
        if (Viewer.matchAndParseInt(line, "Chart Index", -1,  matchSet)) {
            chartIndexRead = matchSet.rInt1;
        }

        //read the "Chart Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(line, "Chart Title", "",  matchSet)) {
            titleRead = matchSet.rString1;
        }

        //read the "Chart Short Title" entry - if not found, default to ""
        if (Viewer.matchAndParseString(
                                   line, "Chart Short Title", "",  matchSet)) {
            shortTitleRead = matchSet.rString1;
        }

        //read the "Chart is Visible" entry - if not found, default to true
        if (Viewer.matchAndParseBoolean(
                                  line, "Chart is Visible", true,  matchSet)) {
            visibleRead = matchSet.rBoolean1;
        }

    }// while ((line = pIn.readLine()) != null)

    //apply settings
    title = titleRead; titledBorder.setTitle(title);
    shortTitle = shortTitleRead;
    setChartVisible(visibleRead);

    if (!success) {
        throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
                                         + chartGroup + " Chart " + chartIndex);
    }

    //if the index number in the file does not match the index number for this
    //strip chart, abort the file read

    if (chartIndexRead != chartIndex) {
        throw new IOException(
            "The file could not be read - section not found for Chart Group "
                                        + chartGroup + " Chart " + chartIndex);
    }

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of StripChart::processStripChartEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::convertToXPixelLocation
//
// Calculates the horizontal pixel location of the absolute value pAbsoluteX
// which is in inches or mm depending on the units currently in use.
//

public int convertToXPixelLocation(double pAbsoluteX)
{

    return(0);

}//end of StripChart::convertToXPixelLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::convertToYPixelLocation
//
// Calculates the vertical pixel location of the absolute value pAbsoluteY
// which is in inches or mm depending on the units currently in use.
//

public int convertToYPixelLocation(double pAbsoluteY)
{

    return(0);

}//end of StripChart::convertToYPixelLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::convertToPixels
//
// Calculates the pixels equal to the absolute distance pAbsolute which is in
// inches or mm depending on the units currently in use.
//

public int convertToPixels(double pAbsoluteX)
{

    return(0);

}//end of StripChart::convertToPixels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::convertToXAbsLocation
//
// Calculates the absolute horizontal value in inches or mm depending on the
// units currently in use from pPixX.
//

public double convertToXAbsLocation(double pPixX)
{

    return(0);

}//end of StripChart::convertToXAbsLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::setTitle
//
// Sets the chart title.
//

public void setTitle(String pTitle)
{

    title = pTitle;

    titledBorder.setTitle(title);

    repaint(); //force display update of title

}//end of StripChart::setTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getTitle
//
// Returns the chart title.
//

public String getTitle()
{

    return title;

}//end of StripChart::getTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::setShortTitle
//
// Sets the chart's short (abbreviated) title.
//

public void setShortTitle(String pShortTitle)
{

    shortTitle = pShortTitle;

}//end of StripChart::setShortTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getShortTitle
//
// Returns the chart's short (abbreviation) title.
//

public String getShortTitle()
{

    return shortTitle;

}//end of StripChart::getShortTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::setChartVisible
//
// Sets the visible status for the chart.
//

public void setChartVisible(Boolean pVisible)
{

    canvas.setVisible(pVisible);

}//end of StripChart::setChartVisible
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::isChartVisible
//
// Returns the chart's visible status.
//

public Boolean isChartVisible()
{

    return canvas.isVisible();

}//end of StripChart::isChartVisible
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

    String section = "Chart Group " + (chartGroup + 1)
                                         + " Strip Chart " + (chartIndex + 1);

    // a title is loaded from the configuration file but this is replaced when
    // the job file is loaded - the user can modify the title inside the
    // program so it is saved with the job file info

    //only override the titles if the cal file strings are not empty and are not
    //the default asterisk -- this allows the titles from the config file to be
    //displayed if the user has not explictly set the titles

    String s;
    s = pCalFile.readString(section, "Title", "*");
    if (!s.equalsIgnoreCase("*")) {setTitle(s);}
    s = pCalFile.readString(section, "Short Title", "*");
    if (!s.equalsIgnoreCase("*")) {setShortTitle(s);}

    setChartVisible(pCalFile.readBoolean(section, "Chart is Visible", true));

    // call each threshold to load its data
    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].loadCalFile(pCalFile);
    }

    leadMaskPos = pCalFile.readDouble(
                             section, "Leading Mask Position (inches)", 24.0);
    trailMaskPos = pCalFile.readDouble(
                             section, "Trailing Mask Position (inches)", 24.0);

}//end of StripChart::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

    String section = "Chart Group " + (chartGroup + 1)
                                         + " Strip Chart " + (chartIndex + 1);


    // a title is loaded from the configuration file but this is replaced when
    // the job file is loaded - the user can modify the title inside the
    // program so it is saved with the job file info

    pCalFile.writeString(section, "Title", getTitle());
    pCalFile.writeString(section, "Short Title", getShortTitle());
    pCalFile.writeBoolean(section, "Chart is Visible", isChartVisible());

    // call each threshold to save its data
    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].saveCalFile(pCalFile);
    }

    pCalFile.writeDouble(
                     section, "Leading Mask Position (inches)", leadMaskPos);
    pCalFile.writeDouble(
                     section, "Trailing Mask Position (inches)", trailMaskPos);

}//end of StripChart::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::saveCalFileHumanReadable
//
// This saves a subset of the calibration data, the values of which affect
// the inspection process.
//
// The data is saved in a human readable format.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFileHumanReadable(BufferedWriter pOut) throws IOException
{

    pOut.write(Settings.postPad(getTitle(), 15));

    //call each threshold to save its data
    for (int i = 0; i < numberOfThresholds; i++) {
        thresholds[i].saveCalFileHumanReadable(pOut);
    }

    pOut.write( " Lead Mask  " + leadMaskPos);
    pOut.write( " Trail Mask " + trailMaskPos);

}//end of StripChart::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::convertToYAbsLocation
//
// Calculates the absolute vertical value in inches or mm depending on the
// units currently in use from pPixY.
//

public double convertToYAbsLocation(double pPixY)
{

    return(0);

}//end of StripChart::convertToYAbsLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getTrace
//
// Returns a pointer to the trace specified by pWhich.
//

public Trace getTrace(int pWhich)
{

    return traces[pWhich];

}//end of StripChart::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::findMinOrMaxValueOfTrace
//
// Finds the minimum or maximum value in dataBuffer1 of trace specified by
// pTrace.  If pFindMin is true, then the minimum is found else the maximum
// is found.
//
// Search begins at pStart position in the array and ends at pEnd.
//

public int findMinOrMaxValueOfTrace(Trace pTrace, boolean pFindMin,
                                                        int pStart, int pEnd)
{

    if(pFindMin) {
        return(pTrace.findMinValue(pStart, pEnd));
    }
    else {
        return(pTrace.findMaxValue(pStart, pEnd));
    }

}//end of StripChart::findMinOrMaxValueOfTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::createArtificialLeadMask
//
// Attempts to detect the point where good inspection data starts.  Traces 0 & 1
// are used for sampling.  Useful in cases where a mask was not set.
//
// This currently only works well for a Wall chart.
//

public int createArtificialLeadMask()
{

    //first look for a section at the beginning where at least one of the traces
    //is the same value for several consecutive points -- for Wall data, this
    //usually signifies that the head is up and flat line data is being received

    //if a straight line section is found, start at the end of it plus 30 points

    int buffer0[] = traces[0].getDataBuffer1();
    int buffer1[] = traces[1].getDataBuffer1();

    //look for end of flat line sections near the beginning of both traces
    int endOfFlatline0 = findEndOfFlatlineSectionNearTraceStart(buffer0);
    int endOfFlatline1 = findEndOfFlatlineSectionNearTraceStart(buffer1);

    int maskStart;

    //use the larger of the two to extend past the longest flatline section
    maskStart = Math.max(endOfFlatline0, endOfFlatline1);

    //if both were -1, then no flat line sections were found so just
    //start from 0
    if (maskStart == -1) {maskStart = 0;}

    //if flat line section not found, start at position 80 -- if section found,
    //start at end of section plus 80
    maskStart += 80;

    return(maskStart);

}//end of StripChart::createArtificialLeadMask
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::findEndOfFlatlineSectionNearTraceStart
//
// Looks for a section at the beginning of the buffer where the data is the
// same value for 5 consecutive points -- for Wall data, this usually
// signifies that the head is up and flat line data is being received.
//
// If such a section is found, the end of the section is returned.
// If no such section is found, -1 is returned.
//

public int findEndOfFlatlineSectionNearTraceStart(int[] pBuffer)
{

    int startOfFlatline = -1;
    int sample = 0;
    int end = 100;

    //return no find code if buffer is too small for the test
    if (end > (pBuffer.length - 5)) {return(-1);}

    //look for flat line section
    for (int i = 0; i <= end; i++){

        //check to see if 5 consecutive points match
        sample = pBuffer[i];
        if (pBuffer[i+1] == sample && pBuffer[i+2] == sample
              && pBuffer[i+3] == sample && pBuffer[i+4] == sample){

            startOfFlatline = i;
            break;

        }
    }//for (int i = 0; i < 100; i++)

    //return no find code if no flatline section found
    if (startOfFlatline == -1) {return(-1);}

    //find the end of the flatline

    int endOfFlatline = -1;
    int endSearchEnd = 100; //only search first several inches

    if(endSearchEnd >= pBuffer.length) {endSearchEnd = pBuffer.length - 1;}

    //find first position which is not a match -- this is the end of
    //the flatline
    for (int i = startOfFlatline; i <= end; i++){

        if (sample != pBuffer[i]){
            endOfFlatline = i;
            break;
        }

    }//for (int i = startOfFlatline; i <= end; i++){

    //returned value will be the end of the flat line section or -1 if none found
    return(endOfFlatline);

}//end of StripChart::findEndOfFlatlineSectionNearTraceStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::createArtificialTrailMask
//
// Attempts to detect the point where good inspection data ends.  Traces 0 & 1
// are used for sampling.  Useful in cases where a mask was not set.
//
// This currently only works well for a Wall chart.
//

public int createArtificialTrailMask()
{

    //first look for a section at the end where at least one of the traces
    //is the same value for several consecutive points -- for Wall data, this
    //usually signifies that the head is up and flat line data is being received

    //if a straight line section is found, end at start of it minus 30 points

    int buffer0[] = traces[0].getDataBuffer1();
    int buffer1[] = traces[1].getDataBuffer1();

    //look for end of flat line sections near the end of both traces
    int endOfFlatline0 = findEndOfFlatlineSectionNearTraceEnd(buffer0);
    int endOfFlatline1 = findEndOfFlatlineSectionNearTraceEnd(buffer1);

    int maskStart;

    //use the smaller of the two to stop before the longest flatline section
    maskStart = Math.min(endOfFlatline0, endOfFlatline1);

    //if both were -1, then no flat line sections were found so just start from
    //end of trace 0 (which should match end of other traces as well)
    if (maskStart == -1) {maskStart = findEndOfData(buffer0);}

    //if flat line section not found, start at end minus 80 -- if section found,
    //start at end of section minus 80
    maskStart -= 80;

    return(maskStart);

}//end of StripChart::createArtificialTrailMask
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::findEndOfFlatlineSectionNearTraceEnd
//
// Looks for a section at the end of the buffer where the data is the
// same value for 5 consecutive points -- for Wall data, this usually
// signifies that the head is up and flat line data is being received.
//
// If such a section is found, the end of the section is returned.
// If no such section is found, -1 is returned.
//
// NOTE: This function is the opposite of the function
//     findEndOfFlatlineSectionNearTraceStart
// For this function, the term startOfFlatline means the start of the section
// when scanning backwards from the end.  The term endOfFlatline means the
// end of the flat line section when scanning backwards.  Thus the end of the
// section is nearest to the beginning of the buffer and the start is nearest
// to the end of the buffer -- opposite meaning from the aforementioned
// function.
//

public int findEndOfFlatlineSectionNearTraceEnd(int[] pBuffer)
{

    int startOfFlatline = -1;
    int sample = 0;
    int endOfData;

    //when searching from the end of the buffer, the last data point must be found
    //to serve as the starting point else the default unfilled values will trigger
    //the flat line section detector

    endOfData = findEndOfData(pBuffer);

    //return no find code if no data found
    if (endOfData == -1) {return(-1);}

    //search from end of data to 100 points prior
    int end = endOfData - 100;

    //return no find code if buffer is too small for the test
    if (end < 5) {return(-1);}

    //look for flat line section
    for (int i = endOfData; i >= end; i--){

        //check to see if 5 consecutive points match
        sample = pBuffer[i];

        if (pBuffer[i-1] == sample && pBuffer[i-2] == sample
              && pBuffer[i-3] == sample && pBuffer[i-4] == sample){

            startOfFlatline = i;
            break;

        }

    }//for (int i = 0; i < 100; i++)

    //return no find code if no flatline section found
    if (startOfFlatline == -1) {return(-1);}

    //find the end of the flatline

    int endOfFlatline = -1;
    int endSearchEnd = endOfData - 100; //only search last several inches

    //return no find code if buffer too small for the test
    if(endSearchEnd < 5) {return(-1);}

    //find first position which is not a match -- this is the end of the flatline
    for (int i = startOfFlatline; i >= endSearchEnd; i--){

        if (sample != pBuffer[i]){
            endOfFlatline = i;
            break;
        }

    }//for (int i = startOfFlatline; i <= end; i++){

    //returned value will be the end of the flat line section or -1 if none found
    return(endOfFlatline);

}//end of StripChart::findEndOfFlatlineSectionNearTraceEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::findEndOfData
//
// Finds the end of the data in pBuffer and returns the index position.
//
// Returns -1 if no data found.
//

public int findEndOfData(int[] pBuffer)
{

    int endOfData = -1;

    //NOTE: Start at pBuffer.length - 2 as the last element seems to be filled
    // with zero -- why is this? -- fix?

    for (int i = (pBuffer.length - 2); i > 0; i--){
        if (pBuffer[i] != Integer.MAX_VALUE){
            endOfData = i;
            break;
        }
    }//for (int i = (pBuffer.length - 1); i <= 0; i--){

    return(endOfData);

}//end of StripChart::findEndOfData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::getNumberOfTraces
//
// Returns the number of traces in the strip chart.
//

public int getNumberOfTraces()
{

    return numberOfTraces;

}//end of StripChart::getNumberOfTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::setLeadTrailTraces
//
// Specifies the leading and trailing traces.  The leading trace is used to
// clear the screen and decorate.
//

public void setLeadTrailTraces(int pLead, int pTrail)
{

    canvas.leadingTrace = pLead; canvas.trailingTrace = pTrail;

}//end of StripChart::setLeadTrailTraces
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::mouseClicked
//

@Override
public void mouseClicked(MouseEvent e)
{


    //get button pressed
    int b = e.getButton();

    //get mouse x,y
    Integer x = e.getX(); Integer y = e.getY();

    //do nothing if the click is outside the plot area
    //if (x < dx1 || x > dx2) return; if (y < dy1 || y > dy2) return;

    //For UT, clicking either trace labeled ID or OD brings up all channels
    //for the chart regardless of trace - this is because ID/OD is tied to all
    //traces.  In the future, perhaps add switch for other types of systems so
    //that clicking on one trace only brings up the channels tied to that trace.

    //left mouse button, set width
    if (b == MouseEvent.BUTTON1){

        //check if click in on a trace's key label
        //if clicked, call the parent listener with the command string and the
        //chart's group and index numbers appended
        for (int i = 0; i < numberOfTraces; i++) {
            if ((traces[i].keyBounds != null) && traces[i].keyBounds.contains(x,y)){
                // trigger event to open the calibration window
                actionListener.actionPerformed(new ActionEvent(this,
                                         ActionEvent.ACTION_PERFORMED,
                    "Open Calibration Window ~" + chartGroup + "~" + chartIndex));
                break;
            }
        }//for (int i = 0;...
    }//if (b == MouseEvent.BUTTON1)

    //right button, set height
    if (b == MouseEvent.BUTTON3){

    }//if (b == MouseEvent.BUTTON3)

}//end of StripChart::mouseClicked
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::mouseMoved
//
// Responds to mouse movements.
//

@Override
public void mouseMoved(MouseEvent e)
{

    //when the cursor moves, let the hardware object provide the appropriate
    //calculated value for the cursor's y position

    if (displayChartHeightAtCursor) {
        chartHeightAtCursorDisplay.updateInt((Graphics2D)getGraphics(),
                                                chartHeight - e.getY(), false);
    }

    //does extra calculation based on the Y position, such as computing wall
    //thickness
    if (displayComputedAtCursor) {
        computedAtCursorDisplay.updateDouble((Graphics2D)getGraphics(),
                traceValueCalculator.calculateComputedValue1(e.getY()), false);
    }

    //convert linear x cursor position to really world feet & inches
    //wip mks -- need to determine scale from user cal settings rather than
    //hard coding

    if (displayLinearPositionAtCursor) {
        linearPositionAtCursorDisplay.updateDouble((Graphics2D)getGraphics(),
       (e.getX() * traceValueCalculator.getLinearDecimalFeetPerPixel()), false);
    }

}//end of StripChart::mouseMoved
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// StripChart::(various listener functions)
//
// These functions are implemented per requirements of interface MouseListener
// and MouseMotionListener but do nothing at the present time.  As code is
// added to each function, it should be moved from this section and formatted
// properly.
//

@Override
public void mousePressed(MouseEvent e) {};
@Override
public void mouseReleased(MouseEvent e) {};
@Override
public void mouseEntered(MouseEvent e) {};
@Override
public void mouseExited(MouseEvent e) {};
@Override
public void mouseDragged(MouseEvent e) {};

//end of StripChart::(various listener functions)
//-----------------------------------------------------------------------------

}//end of class StripChart
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
