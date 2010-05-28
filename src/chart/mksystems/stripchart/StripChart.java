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

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.Color;
import javax.swing.border.*;
import javax.swing.BorderFactory;
import java.awt.image.BufferedImage;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.hardware.Hardware;
import chart.Viewer;
import chart.Xfer;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ChartCanvas
//
// This panel is used to draw the profile plot.
// This class actually does all the work for the StripChart class.
//

class ChartCanvas extends JPanel {

Globals globals;    
Color backgroundColor;
Color gridColor;
int width, height;

int numberOfTraces;
Trace traces[];
int numberOfThresholds;
Threshold thresholds[];

public BufferedImage imageBuffer;

int peakChannel;
int prevPeakChannelValue = -1;

//-----------------------------------------------------------------------------
// ChartCanvas::ChartCanvas (constructor)
//
//

public ChartCanvas(Globals pGlobals, int pWidth, int pHeight, 
                               Color pBackgroundColor, Color pGridColor, 
                               int pNumberOfTraces, Trace[] pTraces,
                               int pNumberOfThresholds, Threshold[] pThresholds)
{

globals = pGlobals;    
width = pWidth; height = pHeight;

backgroundColor = pBackgroundColor; gridColor = pGridColor;
numberOfTraces = pNumberOfTraces; traces = pTraces;
numberOfThresholds = pNumberOfThresholds; thresholds = pThresholds;

setOpaque(true);

setSizes(width, height);

}//end of ChartCanvas::ChartCanvas (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartCanvas::setSizes
//
// Sets the minimum, preferred, and maximum sizes of the canvas.
//

public void setSizes(int pWidth, int pHeight)
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

for (int i = 0; i < numberOfTraces; i++) traces[i].resetTrace();
    
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
if (!isVisible()) return;

//erase canvas
pG2.setColor(backgroundColor);
pG2.fillRect(0, 0, getWidth(), getHeight());

//draw the thresholds
for (int i = 0; i < numberOfThresholds; i++) 
     thresholds[i].paint(pG2, 0, getWidth()-1);

//paint all the traces
for (int i = 0; i < numberOfTraces; i++) traces[i].paintComponent(pG2);

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
if (!isVisible()) return;

//NOTE: All traces are updated only when new data has been added for trace 0.
// Since trace 0 shifts the screen and draws the decorations, no trace can be
// drawn ahead of it.  The hardware should fill the buffers for all traces on
// a given chart in lockstep, data in the traces other than trace 0 can be
// delayed, but trace 0 data should not be delayed after any other trace.  If
// the sensors are offset in their mounting, trace 0 should always be the lead
// sensor.

//while there is data to be plotted for trace 0, plot data for all traces which
//have data

while (traces[0].newDataReady()){

    //find the hardware channel which produced the worst case value - this
    //requires that the worst case trace be found - it is assumed that all
    //traces on the chart have the same direction of severity - won't work
    //on a chart with a min trace(s) and a max trace(s)
    //use trace 0 to determine which direction is more severe
    int peak;
    peak = (traces[0].higherMoreSevere) ? Integer.MIN_VALUE : Integer.MAX_VALUE;

    int lastValue;

    //plot any new data in the traces
    //check for new data on each trace because delayed traces might not have
    //data at the same time
    for (int i = 0; i < numberOfTraces; i++)
        if (traces[i].newDataReady()){
            lastValue = traces[i].plotNewData((Graphics2D)getGraphics());

            //catch the trace with the worst value and record its channel
            if (traces[0].higherMoreSevere)
                if (lastValue > peak){
                    peak = lastValue; peakChannel = traces[i].peakChannel;
                    }
              else
                if (lastValue < peak){
                    peak = lastValue; peakChannel = traces[i].peakChannel;
                    }

            }// if (traces[i].newDataReady())

    }//while (traces[0].newDataReady())

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

public class StripChart extends JPanel implements MouseListener{

ChartCanvas canvas;
public TitledBorder titledBorder;
Globals globals;
IniFile configFile;
int chartGroup;
int chartIndex;
Hardware hardware;
Color borderColor;
TraceGlobals traceGlobals;
int peakChannelLabelXPos = 250, peakChannelLabelYPos = 23;
int peakChannelXPos = peakChannelLabelXPos + 100, peakChannelYPos = 23;

String title, shortTitle;
int numberOfTraces;
Trace[] traces;
int numberOfThresholds;
Threshold[] thresholds;
Color backgroundColor;
Color gridColor;
int gridXSpacing;
boolean leadingMask;
boolean trailingMask;
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

public StripChart(Globals pGlobals, IniFile pConfigFile, int pChartGroup,
       int pChartIndex, Hardware pHardware, ActionListener pActionListener,
       boolean pChartSizeEqualsBufferSize)
{

globals = pGlobals; configFile = pConfigFile; chartIndex = pChartIndex;
chartGroup = pChartGroup;
hardware = pHardware; actionListener = pActionListener;
traceGlobals = new TraceGlobals();
chartSizeEqualsBufferSize = pChartSizeEqualsBufferSize;

//set up the main panel - this panel does nothing more than provide a title
//border and a spacing border
setOpaque(true);

//listen for mouse events on the strip chart
addMouseListener(this);

//read the configuration file and create/setup the charting/control elements
configure(configFile);

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
                           section, "Short Title", "Cht " + (chartIndex+1));

borderColor = new Color(238,238,238);

//read colors first so they can be passed to other objects

backgroundColor = pConfigFile.readColor(
                      section, "Background Color", new Color(238, 238, 238));

gridColor = pConfigFile.readColor(section, "Grid Color", Color.BLACK);

gridXSpacing = pConfigFile.readInt(section, "Grid X Spacing", 10);

leadingMask = pConfigFile.readBoolean(section, "Leading Mask", true);

trailingMask = pConfigFile.readBoolean(section, "Trailing Mask", true);

maskColor = pConfigFile.readColor(section, "Mask Color", Color.BLACK);

separatorColor = 
        pConfigFile.readColor(section, "Piece Separator Color", Color.BLACK);

numberOfThresholds = pConfigFile.readInt(section, "Number of Thresholds", 1);

//read the configuration file and create/setup the thresholds
configureThresholds(configFile);

numberOfTraces = pConfigFile.readInt(section, "Number of Traces", 1);

//read the configuration file and create/setup the traces
configureTraces(configFile);

setBorder(titledBorder = BorderFactory.createTitledBorder(title));

int cWidth = pConfigFile.readInt(section, "Width", 1000);

int cHeight = pConfigFile.readInt(section, "Height", 100);

//if flag is true, make chart wide enough to hold all data in the trace
// see header notes in constructor for more info

if (chartSizeEqualsBufferSize) cWidth = traces[0].sizeOfDataBuffer;

//create a Canvas object to be placed on the main panel - the Canvas object
//provides a panel and methods for drawing data - all the work is actually
//done by the Canvas object
canvas = new ChartCanvas(globals, cWidth, cHeight, backgroundColor, gridColor,
                                                numberOfTraces, traces,
                                                numberOfThresholds, thresholds);
//listen for mouse events on the canvas
canvas.addMouseListener(this);
add(canvas);    

//give all traces a link to their canvas
for (int i = 0; i < numberOfTraces; i++) traces[i].setCanvas(canvas);

//give all thresholds a link to their canvas
for (int i = 0; i < numberOfThresholds; i++) thresholds[i].setCanvas(canvas); 

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
    if (numberOfTraces > 100) numberOfTraces = 100;
    
    traces = new Trace[numberOfTraces];

    for (int i = 0; i < numberOfTraces; i++) traces[i] = 
       new Trace(globals, configFile, chartGroup, chartIndex, i, traceGlobals,
             backgroundColor, gridColor, gridXSpacing, thresholds, hardware);
    
    }//if (numberOf...
 
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
    if (numberOfThresholds > 100) numberOfThresholds = 100;
    
    thresholds = new Threshold[numberOfThresholds];

    for (int i = 0; i < numberOfThresholds; i++)
        thresholds[i] = new Threshold(globals, configFile, chartGroup,
                                                                chartIndex, i);
    
    }//if (numberOf...
 
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

for (int i = 0; i < numberOfTraces; i++) traces[i].handleSizeChanges();

for (int i = 0; i < numberOfThresholds; i++) thresholds[i].handleSizeChanges();

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

//draw the "Peak Channel" label before calling drawKeyLabel because that
//function changes the font
g2.drawString("Peak Channel:", peakChannelLabelXPos, peakChannelLabelYPos);

//draw the keys for the different traces to show which trace is what - each
//key is a label describing the trace and drawn in the color of the trace
for (int i = 0; i < numberOfTraces; i++) traces[i].drawKeyLabel(g2);

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

if (canvas.peakChannel != canvas.prevPeakChannelValue){
    Graphics2D g2 = (Graphics2D) getGraphics();
    //erase the previous value
    g2.setColor(borderColor);
    //offset channel by 1 to convert from zero base
    g2.drawString(Integer.toString(canvas.prevPeakChannelValue + 1),
                                            peakChannelXPos, peakChannelYPos);
    //draw the new value
    g2.setColor(Color.BLACK);
    //offset channel by 1 to convert from zero base
    g2.drawString(Integer.toString(canvas.peakChannel + 1),
                                            peakChannelXPos, peakChannelYPos);
    canvas.prevPeakChannelValue = canvas.peakChannel;
    }

}//end of StripChart::plotData
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

for (int i = 0; i < numberOfTraces; i++) traces[i].markSegmentStart();

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

for (int i = 0; i < numberOfTraces; i++) traces[i].markSegmentEnd();

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
pOut.write("Note that the Chart Title and Short Title may have been changed");
pOut.newLine();
pOut.write("by the user, so the text displayed on the screen may not match");
pOut.newLine();
pOut.write("the values shown here.");
pOut.newLine(); pOut.newLine();

pOut.write("Chart is Visible=" + isChartVisible()); //save visibility flag
pOut.newLine(); pOut.newLine();

for (int i = 0; i < numberOfThresholds; i++) thresholds[i].saveSegment(pOut);

for (int i = 0; i < numberOfTraces; i++) traces[i].saveSegment(pOut);

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

for (int i = 0; i < numberOfTraces; i++)
    if (traces[i].segmentStarted()) return(true);
    
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

for (int i = 0; i < numberOfThresholds; i++)
   line = thresholds[i].loadSegment(pIn, line);

for (int i = 0; i < numberOfTraces; i++)
   line = traces[i].loadSegment(pIn, line);

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

//if pLastLine contains the [Chart] tag, then skip ahead else read until end of
//file reached or "[Chart]" section tag reached

if (Viewer.matchAndParseString(pLastLine, "[Chart]", "",  matchSet))
    success = true; //tag already found
else
    while ((line = pIn.readLine()) != null){  //search for tag
        if (Viewer.matchAndParseString(line, "[Chart]", "",  matchSet)){
            success = true; break;
            }
        }//while

if (!success) throw new IOException(
       "The file could not be read - section not found for Chart Group "
                                         + chartGroup + " Chart " + chartIndex);

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
    if (Viewer.matchAndParseInt(line, "Chart Index", -1,  matchSet))
        chartIndexRead = matchSet.rInt1;

    //read the "Chart Title" entry - if not found, default to ""
    if (Viewer.matchAndParseString(line, "Chart Title", "",  matchSet))
        titleRead = matchSet.rString1;

    //read the "Chart Short Title" entry - if not found, default to ""
    if (Viewer.matchAndParseString(line, "Chart Short Title", "",  matchSet))
        shortTitleRead = matchSet.rString1;

    //read the "Chart is Visible" entry - if not found, default to true
    if (Viewer.matchAndParseBoolean(line, "Chart is Visible", true,  matchSet))
        visibleRead = matchSet.rBoolean1;
     
    }// while ((line = pIn.readLine()) != null)

//apply settings
title = titleRead; titledBorder.setTitle(title);
shortTitle = shortTitleRead;
setChartVisible(visibleRead);

if (!success) throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
                                         + chartGroup + " Chart " + chartIndex);

//if the index number in the file does not match the index number for this
//strip chart, abort the file read

if (chartIndexRead != chartIndex) throw new IOException(
        "The file could not be read - section not found for Chart Group "
                                         + chartGroup + " Chart " + chartIndex);

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

// a title is loaded from the configuration file but this is replaced when the
// job file is loaded - the user can modify the title inside the program so it
// is saved with the job file info

setTitle(pCalFile.readString(section, "Title", "*"));
setShortTitle(pCalFile.readString(section, "Short Title", "*"));
setChartVisible(pCalFile.readBoolean(section, "Chart is Visible", true));

// call each threshold to load its data
for (int i = 0; i < numberOfThresholds; i++)
                                          thresholds[i].loadCalFile(pCalFile);

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


// a title is loaded from the configuration file but this is replaced when the
// job file is loaded - the user can modify the title inside the program so it
// is saved with the job file info

pCalFile.writeString(section, "Title", getTitle());
pCalFile.writeString(section, "Short Title", getShortTitle());
pCalFile.writeBoolean(section, "Chart is Visible", isChartVisible());

// call each threshold to save its data
for (int i = 0; i < numberOfThresholds; i++) 
    thresholds[i].saveCalFile(pCalFile);

}//end of StripChart::saveCalFile
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
        if (traces[i].keyBounds.contains(x,y)){
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
// StripChart::(various listener functions)
//
// These functions are implemented per requirements of interface MouseListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void mousePressed(MouseEvent e) {};
@Override
public void mouseReleased(MouseEvent e) {};
@Override
public void mouseEntered(MouseEvent e) {};
@Override
public void mouseExited(MouseEvent e) {};

//end of StripChart::(various listener functions)
//-----------------------------------------------------------------------------

}//end of class StripChart
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
