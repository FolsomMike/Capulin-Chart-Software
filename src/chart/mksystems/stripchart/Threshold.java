/******************************************************************************
* Title: Threshold.java
* Author: Mike Schoonover
* Date: 3/17/08
*
* Purpose:
*
* This class handles a single threshold.
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

import chart.mksystems.settings.Settings;
import chart.mksystems.inifile.IniFile;
import chart.Viewer;
import chart.Xfer;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Threshold
//
// This class creates and controls a trace.
//

public class Threshold extends Object{

Settings settings;
IniFile configFile;
int chartGroup;
int chartIndex;
int thresholdIndex;
JPanel canvas;
int canvasXLimit;
int canvasYLimit;

public static int flagWidth = 5;
public static int flagHeight = 7;

public boolean okToMark = true;

public String title;
String shortTitle;
boolean doNotFlag, flagOnOver;
public Color thresholdColor;

public int thresholdLevel;
int plotThresholdLevel;
boolean invert;

// references to point at the controls used to adjust the values - these
// references are set up by the object which handles the adjusters and are
// only used temporarily

public Object levelAdjuster;


//-----------------------------------------------------------------------------
// Threshold::Threshold (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public Threshold(Settings pSettings, IniFile pConfigFile, int pChartGroup,
                                        int pChartIndex, int pThresholdIndex)
{

settings = pSettings; configFile = pConfigFile;
chartGroup = pChartGroup;
chartIndex = pChartIndex; thresholdIndex = pThresholdIndex;

//read the configuration file and create/setup the charting/control elements
configure(configFile);

}//end of Threshold::Threshold (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::configure
//
// Loads configuration settings from the configuration.ini file.
//

private void configure(IniFile pConfigFile)
{

String section = "Chart Group " + (chartGroup + 1)
     + " Strip Chart " + (chartIndex + 1) + " Threshold " + (thresholdIndex+1);

title = pConfigFile.readString(section, "Title", "*");

shortTitle = pConfigFile.readString(section, "Short Title", "*");

doNotFlag =
    pConfigFile.readBoolean(section, "Do Not Flag - For Reference Only", false);

flagOnOver = pConfigFile.readBoolean(section, "Flag On Over", true);

thresholdColor = pConfigFile.readColor(section, "Color", Color.RED);

invert = pConfigFile.readBoolean(section, "Invert Threshold", true);

thresholdLevel = pConfigFile.readInt(section, "Default Level", 50);

}//end of Threshold::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

String section = "Chart Group " + (chartGroup + 1) + " Strip Chart "
        + (chartIndex + 1) + " Threshold " + (thresholdIndex + 1);

thresholdLevel = pCalFile.readInt(section, "Threshold Level", 10);

}//end of Threshold::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

String section = "Chart Group " + (chartGroup + 1) + " Strip Chart "
        + (chartIndex + 1) + " Threshold " + (thresholdIndex + 1);

pCalFile.writeInt(section, "Threshold Level", thresholdLevel);

}//end of Threshold::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::setCanvas
//
// Stores a pointer to the canvas on which the traces are drawn.
//

public void setCanvas(JPanel pCanvas)
{

canvas = pCanvas;

}//end of Threshold::setCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

canvasXLimit = canvas.getWidth() - 1;
canvasYLimit = canvas.getHeight() - 1;

//force recalculation of values associated with the level
setThresholdLevel(thresholdLevel);

}//end of Threshold::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::setThresholdLevel
//
// Sets the level for the threshold indexed by pWhich.
//

public void setThresholdLevel( int pLevel)
{

thresholdLevel = pLevel;

plotThresholdLevel = thresholdLevel;
if(plotThresholdLevel < 0) plotThresholdLevel = 0;
if(plotThresholdLevel > canvasYLimit) plotThresholdLevel = canvasYLimit;

//invert the y position if specified
if (invert){
    plotThresholdLevel = canvasYLimit - plotThresholdLevel;
    }

}//end of Threshold::setThresholdLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Thresholds::paint
//
// Draws the threshold from pixel pStart to pixel pEnd.

public void paint(Graphics2D pG2, int pStart, int pEnd)

{

pG2.setColor(thresholdColor);
pG2.drawLine(pStart, plotThresholdLevel, pEnd, plotThresholdLevel);

}//end of Threshold::paint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Thresholds::drawSlice
//
// Draws one dot of the threshold.
//

public void drawSlice(Graphics2D pG2, int xPos)

{

pG2.setColor(thresholdColor);
pG2.drawRect(xPos, plotThresholdLevel, 0, 0); //draw a dot to make the threshold

}//end of Threshold::drawSlice
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Thresholds::checkViolation
//
// Returns true signal exceeds the threshold level.  Whether this is above or
// below the threshold is determined by flagOnOver.
//
// The threshold with the lowest (0) thresholdIndex is the highest severity
// threshold, highest index is lowest.  This function should be called for the
// thresholds in order of their index which happens automatically if they are
// stored in an array in this order.  If called in this order, no more
// thresholds should be checked after one returns true because lower severity
// thresholds should not override higher ones.
//
// If doNotFlag is true, the threshold is for reference purposes only and no
// violations will ever be recorded.
//
// NOTE: For this function, the threshold is not inverted and the pSigHeight
// should not be inverted as well.
//

public boolean checkViolation(int pSigHeight)

{

//if the threshold is non-flagging, return without action
if (doNotFlag) return(false);

//if the signal level exceeds the threshold, draw a flag - if flagOnOver is
//true check for signal above, if false check for signal below
if (flagOnOver){
    if (pSigHeight >= thresholdLevel) return(true);
    }
else{
    if (pSigHeight <= thresholdLevel) return(true);
    }

return(false); //no flag set

}//end of Threshold::checkViolation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Thresholds::drawFlag
//
// Draws a flag with the threshold color at location xPos,pSigHeight.
//

public void drawFlag(Graphics2D pPG2, int pXPos, int pYPos)
{

    //if flag would be drawn above or below the screen, force on screen
    if (pYPos < 0) pYPos = 0;
    if (pYPos > canvasYLimit) pYPos = canvasYLimit - flagHeight;

    //add 1 to xPos so flag is drawn to the right of the peak

    pPG2.setColor(thresholdColor);
    pPG2.fillRect(pXPos+1, pYPos, flagWidth, flagHeight);

}//end of Threshold::drawFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::saveSegment
//
// Saves the thresholds settings to the open file pOut.
//

public void saveSegment(BufferedWriter pOut) throws IOException
{

pOut.write("[Threshold]"); pOut.newLine();
pOut.write("Threshold Index=" + thresholdIndex); pOut.newLine();
pOut.write("Threshold Title=" + title); pOut.newLine();
pOut.write("Threshold Short Title=" + shortTitle); pOut.newLine();
pOut.newLine();

pOut.write("Threshold Level=" + thresholdLevel); //save the threshold level
pOut.newLine(); pOut.newLine();

}//end of Threshold::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::loadSegment
//
// Loads the data for a segment from pIn.  It is expected that the Threshold
// section is next in the file.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Threshold section, the [Threshold] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                             throws IOException
{

//handle entries for the threshold itself
String line = processThresholdEntries(pIn, pLastLine);

return(line);

}//end of Threshold::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Threshold::processThresholdEntries
//
// Processes the entries for the threshold itself via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For the Threshold section, the [Threshold] tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processThresholdEntries(BufferedReader pIn, String pLastLine)
                                                             throws IOException

{

String line;
boolean success = false;
Xfer matchSet = new Xfer(); //for receiving data from function calls

//if pLastLine contains the [Threshold] tag, then skip ahead else read until
// end of file reached or "[Threshold]" section tag reached

if (Viewer.matchAndParseString(pLastLine, "[Threshold]", "",  matchSet))
    success = true; //tag already found
else
    while ((line = pIn.readLine()) != null){  //search for tag
        if (Viewer.matchAndParseString(line, "[Threshold]", "",  matchSet)){
            success = true; break;
            }
        }//while

if (!success) throw new IOException(
       "The file could not be read - section not found for Chart Group "
        + chartGroup + " Chart " + chartIndex + " Threshold " + thresholdIndex);

//set defaults
int thresholdIndexRead = -1;
String titleRead = "", shortTitleRead = "";
int levelRead = 100;

//scan the first part of the section and parse its entries
//these entries apply to the chart group itself

success = false;
while ((line = pIn.readLine()) != null){

    //stop when next section tag reached (will start with [)
    if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
        success = true; break;
        }

    //read the "Threshold Index" entry - if not found, default to -1
    if (Viewer.matchAndParseInt(line, "Threshold Index", -1, matchSet))
        thresholdIndexRead = matchSet.rInt1;

    //NOTE: this match is due to a bug in segments saved under
    // Segment Data Version 1.0 - the tag was misspelled - can be removed
    // eventually - only one job run with that version
    //read the "Theshold Index" entry - if not found, default to -1
    if (Viewer.matchAndParseInt(line, "Theshold Index", -1, matchSet))
        thresholdIndexRead = matchSet.rInt1;

    //read the "Threshold Title" entry - if not found, default to ""
    if (Viewer.matchAndParseString(line, "Threshold Title", "", matchSet))
        titleRead = matchSet.rString1;

    //read the "Threshold Short Title" entry - if not found, default to ""
    if (Viewer.matchAndParseString(line, "Threshold Short Title", "", matchSet))
        shortTitleRead = matchSet.rString1;

    //read the "Threshold Level" entry - if not found, default to 100
    if (Viewer.matchAndParseInt(line, "Threshold Level", 100, matchSet))
        levelRead = matchSet.rInt1;

    }

//apply settings
title = titleRead; shortTitle = shortTitleRead;
setThresholdLevel(levelRead);

if (!success) throw new IOException(
        "The file could not be read - missing end of section for Chart Group "
        + chartGroup + " Chart " + chartIndex + " Threshold " + thresholdIndex);

//if the index number in the file does not match the index number for this
//threshold, abort the file read

if (thresholdIndexRead != thresholdIndex) throw new IOException(
        "The file could not be read - section not found for Chart Group "
        + chartGroup + " Chart " + chartIndex + " Threshold " + thresholdIndex);

return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Threshold::processThresholdEntries
//-----------------------------------------------------------------------------

}//end of class Threshold
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------