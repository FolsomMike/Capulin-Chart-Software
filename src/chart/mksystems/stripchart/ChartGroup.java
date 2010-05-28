/******************************************************************************
* Title: ChartGroup.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class displays a group of charts contained on a JPanel.  A status bar
* is inserted between two of the charts.  Positions, colors, etc. of the charts
* and status bar are read from a configuration file or transferred in from
* variables.
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
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;

import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.hardware.Hardware;
import chart.Viewer;
import chart.Xfer;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ChartGroup
//
// This class creates and controls a plot display.
//

public class ChartGroup extends JPanel implements MouseListener{

Globals globals;    
IniFile configFile;
int chartGroupIndex;
Hardware hardware;

int windowXPos, windowYPos;

int numberOfStripCharts;
StripChart[] stripCharts;
String pieceName;
Color jointLabelColor;
boolean singleColumn;
boolean chartSizeEqualsBufferSize;

public int viewerChartScrollPaneWidth;
public int viewerChartScrollPaneHeight;

ActionListener actionListener;

//-----------------------------------------------------------------------------
// ChartGroup::ChartGroup (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
  
public ChartGroup(Globals pGlobals, IniFile pConfigFile, int pChartGroupIndex,
                        Hardware pHardware, ActionListener pActionListener,
                                        boolean pChartSizeEqualsBufferSize)
{

globals = pGlobals; configFile = pConfigFile; 
chartGroupIndex = pChartGroupIndex;
hardware = pHardware;
actionListener = pActionListener;
chartSizeEqualsBufferSize = pChartSizeEqualsBufferSize;

//set up the main panel - this panel does nothing more than provide a title
//border and a spacing border
setOpaque(true);

//read the configuration file and create/setup the charting/control elements
configure(configFile);

}//end of ChartGroup::ChartGroup (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::configure
//
// Loads configuration settings from the configuration.ini file.  These set
// the number of charts, traces, thresholds, channels, positions, colors, etc.
// The various child objects are then created as specified by the config data.
//
// Only configuration data for the ChartGroup itself are loaded here.  Each
// child object should be allowed to load its own data.
//

private void configure(IniFile pConfigFile)
{

String section = "Chart Group " + (chartGroupIndex + 1);

numberOfStripCharts = 
         pConfigFile.readInt(section, "Number of Strip Charts", 1);

pieceName = pConfigFile.readString(section, "Name Of Pieces", "Piece");

jointLabelColor = pConfigFile.readColor(
                                    section, "Joint Label Color", Color.BLACK);

singleColumn = pConfigFile.readBoolean(section, "Single Column", true);


//if config file specifies a single column, use BoxLayout to force a single
//column else use FlowLayout to allow multiple columns
if (singleColumn)
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
else
    setLayout(new FlowLayout());

//read values used by the Viewer window to set the size of the scroll pane
//this pane contains the charts to be viewed - where possible, the height
//can be set to show all of the charts at once, if there are too many then
//a convenient height can be set and a vertical scroll bar will automatically
//be shown to allow scrolling to view all charts

viewerChartScrollPaneWidth = pConfigFile.readInt(
                            section, "Viewer Chart Scroll Pane Width", 1233);
viewerChartScrollPaneHeight = pConfigFile.readInt(
                            section, "Viewer Chart Scroll Pane Height", 308);

//create an array of strip charts per the config file setting
if (numberOfStripCharts > 0){
    
    //protect against too many groups
    if (numberOfStripCharts > 100) numberOfStripCharts = 100;
    
    stripCharts = new StripChart[numberOfStripCharts];

    for (int i = 0; i < numberOfStripCharts; i++){
       stripCharts[i] = new StripChart(globals, configFile, chartGroupIndex, i,
                        hardware, actionListener, chartSizeEqualsBufferSize);
        add(stripCharts[i]);
        }
    
    }//if (numberOfChartGroups > 0)

}//end of ChartGroup::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

String section = "Chart Group " + (chartGroupIndex + 1);

windowXPos = pCalFile.readInt(section, "Window X Position", 0);
windowYPos = pCalFile.readInt(section, "Window Y Position", 0);

// call each chart to load its data
for (int i = 0; i < numberOfStripCharts; i++)
                                          stripCharts[i].loadCalFile(pCalFile);

}//end of ChartGroup::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

String section = "Chart Group " + (chartGroupIndex + 1);

pCalFile.writeInt(section, "Window X Position", windowXPos);
pCalFile.writeInt(section, "Window Y Position", windowYPos);

// call each chart to load its data
for (int i = 0; i < numberOfStripCharts; i++)
                                          stripCharts[i].saveCalFile(pCalFile);

}//end of ChartGroup::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::getStripChart
//
// Returns a pointer to the chart specified by pWhich.
//

public StripChart getStripChart(int pWhich)
{

return stripCharts[pWhich];

}//end of ChartGroup::getStripChart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::resetChartGroup
//
// Erases the charts and clears all data.
//

public void resetChartGroup()
{

for (int i = 0; i < numberOfStripCharts; i++) stripCharts[i].resetChart();
    
}//end of ChartGroup::resetChartGroup
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::markSegmentStart
//
// Resets the counter which is used to determine if a new segment has begun
// and records the start position.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

public void markSegmentStart()
{

for (int i = 0; i < numberOfStripCharts; i++)
    stripCharts[i].markSegmentStart();

}//end of ChartGroup::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::markSegmentEnd
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

for (int i = 0; i < numberOfStripCharts; i++)
    stripCharts[i].markSegmentEnd();

}//end of ChartGroup::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::saveSegment
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

pOut.write("[Chart Group]"); pOut.newLine();
pOut.write("Chart Group Index=" + chartGroupIndex); pOut.newLine();
pOut.newLine();

for (int i = 0; i < numberOfStripCharts; i++)
    stripCharts[i].saveSegment(pOut);

}//end of ChartGroup::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::segmentStarted
//
// Checks to see if a segment has been started and thus may have data which
// needs to be saved.
//

public boolean segmentStarted()
{

for (int i = 0; i < numberOfStripCharts; i++)
    if (stripCharts[i].segmentStarted()) return(true);

return(false);

}//end of ChartGroup::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::loadSegment
//
// Loads the data for a segment from pIn.  It is expected that the ChartGroup
// section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// The [ChartGroup] tag may or may not have already been read from the file by
// the code handling the previous section.  If it has been read, the line
// containing the tag should be passed in via pLastLine.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

//handle entries for the chart group itself
String line = processChartGroupEntries(pIn, pLastLine);

//allow each strip chart to load data, passing the last line read to the next
//call each time

for (int i = 0; i < numberOfStripCharts; i++)
    line = stripCharts[i].loadSegment(pIn, line);

return(line);

}//end of ChartGroup::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::processChartGroupEntries
//
// Processes the entries for the chart group itself via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// The [ChartGroup] tag may or may not have already been read from the file by
// the code handling the previous section.  If it has been read, the line
// containing the tag should be passed in via pLastLine.
//

private String processChartGroupEntries(BufferedReader pIn, String pLastLine)
                                                            throws IOException

{

String line;
boolean success = false;
Xfer matchSet = new Xfer(); //for receiving data from function calls

//if pLastLine contains the [Chart Group] tag, then skip ahead else read until
// end of file reached or "[Chart Group]" section tag reached

if (Viewer.matchAndParseString(pLastLine, "[Chart Group]", "", matchSet))
    success = true; //tag already found
else
    while ((line = pIn.readLine()) != null){  //search for tag
        if (Viewer.matchAndParseString(line, "[Chart Group]", "", matchSet)){
            success = true; break;
            }
        }//while

if (!success) throw new IOException(
       "The file could not be read - section not found for Chart Group "
                                                            + chartGroupIndex);

//set defaults
int chartGroupIndexRead = -1;

//scan the first part of the section and parse its entries
//these entries apply to the chart group itself

success = false;
while ((line = pIn.readLine()) != null){

    //stop when next section tag reached (will start with [)
    if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
        success = true; break;
        }

    //catch the "Chart Group Index" entry - if not found, default to -1
    if (Viewer.matchAndParseInt(line, "Chart Group Index", -1,  matchSet))
        chartGroupIndexRead = matchSet.rInt1;

    }// while ((line = pIn.readLine()) != null)

if (!success) throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
                                                            + chartGroupIndex);

//if the index number in the file does not match the index number for this
//chart group, abort the file read

if (chartGroupIndexRead != chartGroupIndex) throw new IOException(
        "The file could not be read - section not found for Chart Group "
                                                            + chartGroupIndex);

return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Viewer::processChartGroupEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

for (int i = 0; i < numberOfStripCharts; i++) 
                                             stripCharts[i].handleSizeChanges();

}//end of ChartGroup::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::plotData
//
// Plots any new data in the trace buffers.
//

public void plotData()
{

for (int i = 0; i < numberOfStripCharts; i++) stripCharts[i].plotData();
    
}//end of ChartGroup::plotData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::getStripChartWidth
//
// Returns the width of the strip charts in the group.  It is assumed that
// all the charts are the same width, so the width of the first one is returned.
//
// Note: this is only accurate after the group has been packed.
//

public int getStripChartWidth()

{

return stripCharts[0].getWidth();

}//end of ChartGroup::getStripChartWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::getStripChartHeight
//
// Returns the height of the strip charts in the group.  It is assumed that
// all the charts are the same height, so the height of the first one is
// returned.
//
// Note: this is only accurate after the group has been packed.
//

public int getStripChartHeight()

{

return stripCharts[0].getHeight();

}//end of ChartGroup::getStripChartHeight
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChartGroup::(various listener functions)
//
// These functions are implemented per requirements of interface MouseListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

    
@Override
public void mouseClicked(MouseEvent e) {};    
@Override
public void mousePressed(MouseEvent e) {};
@Override
public void mouseReleased(MouseEvent e) {};
@Override
public void mouseEntered(MouseEvent e) {};
@Override
public void mouseExited(MouseEvent e) {};

//end of ProfilePlot::(various listener functions)
//-----------------------------------------------------------------------------

}//end of class ChartGroup
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
