/******************************************************************************
* Title: Plotter.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This is the parent class for objects which plot data in a StripChart, such
* as Trace, Map2D, Map3D, etc.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.stripchart;

import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import javax.swing.JPanel;

//-----------------------------------------------------------------------------
// class Plotter
//
// This is the parent class for objects which plot data in a StripChart, such
// as Trace, Map2D, Map3D, etc.
//

public class Plotter extends Object{

    IniFile configFile;
    Settings settings;
    Hardware hardware;

    int dataSourceBoardChassis = -1, dataSourceBoardSlot = -1;

    String typeDescriptor;
    String configFileSection;

    int chartGroup;
    public int chartIndex;
    public StripChart chart;
    int plotterIndex;
    String title;
    public String shortTitle;
    Color traceColor;
    public String keyLabel;
    int keyXPosition, keyYPosition;
    public Rectangle2D keyBounds;
    public int headNum;
    public double distanceSensorToFrontEdgeOfHead;
    boolean distanceSensorToFrontEdgeOfHeadOverridden = false;
    int pixelOffset;
    double pixelScaling;
    double preScaling;
    double preOffset;
    boolean higherMoreSevere;
    public boolean flaggingEnabled = false;
    public boolean useVerticalBarToMarkEndMasks = false;
    public boolean suppressTraceInEndMasks = false;
    public double delayDistance;
    public double startFwdDelayDistance;
    public double startRevDelayDistance;

    public boolean leadPlotter = false;
    public boolean trailPlotter = false;

    public double prevLinearPos = Double.MIN_VALUE;
    public int prevAmplitude = Integer.MIN_VALUE;
    public int prevClockPos = -1;

    PlotterHdwVars hdwVs;
    PlotterGlobals plotterGlobals;
    PlotVars plotVs, repaintVs;

    //wall thickness value - used by wall traces
    public double wallThickness;
    //hardware channel of the last value to be stored as a peak
    public int peakChannel;

    JPanel canvas;
    int canvasXLimit;
    int canvasYLimit;
    double inchesPerPixel;
    Color backgroundColor;
    Color gridColor;

    ColorMapper colorMapper = null;

    public boolean positionAdvanced;  //used by external class

    //types of plotter objects

    static final int TRACE = 0;
    static final int MAP_2D = 1;
    static final int MAP_3D = 2;
    static final int X_AXIS_ANNOTATION = 3;    

    static final int MINOR_GRID_SPACING_INCHES = 6;
    static final int MAJOR_GRID_SPACING_INCHES = 12;
    static final int X_AXIS_ANNOTATION_SPACING_INCHES = 24;
    
//-----------------------------------------------------------------------------
// Plotter::Plotter (constructor)
//
// This constructor is not normally used. Each subclass should provide a
// custom constructor.
//

public Plotter()
{


}//end of Plotter::Plotter (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

    configFileSection = "Chart Group " + (chartGroup + 1)
            + " Strip Chart " + (chartIndex + 1) + " " + typeDescriptor + " "
            + (plotterIndex + 1);

}//end of Plotter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::configure
//
// Loads configuration settings from the configuration.ini file.
//

void configure(IniFile pConfigFile)
{

    String section = configFileSection;

    title = pConfigFile.readString(section, "Title", "*");

    shortTitle = pConfigFile.readString(section, "Short Title", "*");

    traceColor = pConfigFile.readColor(section, "Color", Color.BLACK);

    keyLabel = pConfigFile.readString(section, "Key Label", "*");

    keyXPosition = pConfigFile.readInt(section, "Key X Position", 100);

    keyYPosition = pConfigFile.readInt(section, "Key Y Position", 23);

    headNum = pConfigFile.readInt(section, "Head", 1);

    distanceSensorToFrontEdgeOfHead = pConfigFile.readDouble(section,
                             "Distance From Sensor to Front Edge of Head", 0.0);

    if (distanceSensorToFrontEdgeOfHead == -1){
        distanceSensorToFrontEdgeOfHeadOverridden = true;
    }
    
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

    if (hdwVs != null){
        hdwVs.setPlotStyle(pConfigFile.readInt(section, "Plot Style", 0));
    }
    
    if (hdwVs != null){
        hdwVs.setSimDataType(
                     pConfigFile.readInt(section, "Simulation Data Type", 0));
    }
    
}//end of Plotter::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::resetAll
//
// Clears all data.
//
// Do not call from the constructor because all data may not be available at
// that time.
//

public void resetAll()
{

    if (plotVs != null){
        
        plotVs.gridCounter = 0; //used to place grid marks

        plotVs.nextMinorGridLoc = MINOR_GRID_SPACING_INCHES;
        plotVs.nextMajorGridLoc = MAJOR_GRID_SPACING_INCHES;

        plotVs.drawData = true; //draw trace when plotting data

        //pixel position on the screen where data is being plotted
        plotVs.pixPtr = -1;
    }
    
    plotterGlobals.bufOffset = 0; //left edge of screen starts at position 0

    plotterGlobals.scrollCount = 0; //number of pixels chart has been scrolled

}//end of Plotter::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::setCanvas
//
// Stores a pointer to the canvas on which the data is drawn.
//

public void setCanvas(JPanel pCanvas)
{

    canvas = pCanvas;

}//end of Plotter::setCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataSourceBoardChassis
//
// Returns the chassis number of the board providing data.
//

public int getDataSourceBoardChassis()
{

    return(dataSourceBoardChassis);

}//end of Plotter::getDataSourceBoardChassis
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataSourceBoardSlot
//
// Returns the slot number of the board providing data.
//

public int getDataSourceBoardSlot()
{

    return(dataSourceBoardSlot);

}//end of Plotter::getDataSourceBoardSlot
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::setColorMapper
//
// Stores a pointer to the ColorMapper which is used by some subclasses to
// translate data values to different colors.
//

public void setColorMapper(ColorMapper pColorMapper)
{

    colorMapper = pColorMapper;

}//end of Plotter::setColorMapper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

    canvasXLimit = canvas.getWidth() - 1;
    canvasYLimit = canvas.getHeight() - 1;

}//end of Plotter::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::newDataIsReady
//
// Checks to see if any new data is ready to be plotted or erased.
//
// Should be overridden by subclasses.
//
// Returns true if new data is ready, false if not.
//

public boolean newDataIsReady()
{

    return(false);

}//end of Plotter::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::plotNewData
//
// Plots new data.
//
// Should be overridden by subclasses.
//
// Returns the value last plotted. See subclass for details.
//

public int plotNewData(Graphics2D pG2)
{

    return(0);

}//end of Plotter::plotNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataBuffer1
//
// Returns a reference to dataBuffer1.
//
// Should be overridden by subclasses.
//

public int[] getDataBuffer1()
{

    return(null);

}//end of Plotter::getDataBuffer1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getFlagBuffer
//
// Returns a reference to flagBuffer.
//
// Should be overridden by subclasses.
//

public int[] getFlagBuffer()
{

    return(null);

}//end of Plotter::getFlagBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getTitle
//
// Returns the plotter object's title.
//

public String getTitle()
{

    return (title);

}//end of Plotter::getTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataBufferWidth
//
// Returns the width of the data buffer. For a single dimension array, this
// is simply the length of the array. For a multi-dimension array, it is the
// array length in the dimension that corresponds with the linear length of
// the chart and test piece.
//
// Should be overridden by subclasses.
//

public int getDataBufferWidth()
{

    return(0);

}//end of Plotter::getDataBufferWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataWidth
//
// Returns the index of the last valid data point.
//
// Returns -1 if no data found.
//
// Should be overridden by subclasses.
//

public int getDataWidth()
{

    return(0);

}//end of Plotter::getDataWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::loadSegment
//
// Loads the meta data, data points, and flags for a segment from pIn.
//
// Should be overridden by subclasses.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    return("");

}//end of Plotter::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::saveSegment
//
// Saves the data for a segment to the open file pOut.
//
// Should be overridden by subclasses.
//

public void saveSegment(BufferedWriter pOut) throws IOException
{

}//end of Plotter::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::drawKeyLabel
//
// Draws the key label on the graphics device pG2.  The key label describes
// the trace/map and is drawn in the color of the trace/map.
//
// In the case of a map using many colors, the "Trace Color" entry in the
// config file is still used for the key label color.
//

public void drawKeyLabel(Graphics2D pG2)
{

    if (keyLabel.compareTo("<not visible>") == 0) {return;}

    //set the background color for the text to white so that most colors are
    //more visible

    HashMap<TextAttribute, Object> map = new HashMap<>();

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

}//end of Plotter::drawKeyLabel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::markSegmentStart
//
// Resets the counter which is used to determine if a new segment has begun
// and records the start position.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//
// Should be overridden by subclasses.
//

public void markSegmentStart()
{


}//end of Plotter::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::markSegmentEnd
//
// Marks the buffer location of the end of the current segment.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//
// This function should be called before saving the data so the end points
// of the data to be saved are known.
//
// Should be overridden by subclasses.
//

public void markSegmentEnd()
{

}//end of Plotter::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::segmentStarted
//
// Returns true if a segment has been started by having had data added to it,
// returns false otherwise.
//
// Should be overridden by subclasses.
//

public boolean segmentStarted()
{

    return(false);

}//end of Plotter::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::findMinValue
//
// Finds the minimum value of data in the buffer.  Search begins at pStart
// position in the array and ends at pEnd.
//
// Should be overridden by subclasses.
//
// If pFlagLocation is true, a flag will be placed in the data buffer at the
// location of the min value.
//
// Returns the min value and also returns it in pPeakInfo.peak.
// The buffer index of that value is returned in pPeakInfo.index.
//

public int findMinValue(int pStart, int pEnd,TraceDatum pPeakInfo,
                                                        boolean pFlagLocation)
{

    return(0);

}//end of Plotter::findMinValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::findMaxValue
//
// Finds the maximum value of data in the buffer.  Search begins at pStart
// position in the array and ends at pEnd.
//
// Should be overridden by subclasses.
//
// If pFlagLocation is true, a flag will be placed in the data buffer at the
// location of the max value.
//
// Returns the max value and also returns it in pPeakInfo.peak.
// The buffer index of that value is returned in pPeakInfo.index.
//


public int findMaxValue(int pStart, int pEnd,TraceDatum pPeakInfo,
                                                        boolean pFlagLocation)
{

    return(0);

}//end of Plotter::findMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::placeEndMaskMarker
//
// Marks the current buffer location to signal that an end mask mark should
// be drawn at that position.
//
// Should be overridden by subclasses.
//

public void placeEndMaskMarker()
{

}//end of Plotter::placeEndMaskMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// This method should only be called by the producer thread.
//
// Should be overridden by subclasses.
//

public void advanceInsertionPoint()
{

}//end of Plotter::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::isMaxCaptureMode
//
// Returns true if this plotter is configured to capture maximum peak values;
// return false if configured to capture minimum peak values.
//

public boolean isMaxCaptureMode()
{

    return(higherMoreSevere);

}//end of Plotter::isMaxCaptureMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::paintComponent
//
// Refreshes the canvas using the data in the buffers.
//
// Should be overridden by subclasses.
//

public void paintComponent(Graphics2D pG2)
{



}//end of Plotter::paintComponent
//-----------------------------------------------------------------------------

}//end of class Plotter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
