/******************************************************************************
* Title: XScaleAnnotator.java
* Author: Mike Schoonover
* Date: 3/22/17
*
* Purpose:
*
* This class draws annotations for the X axis such as grid location numers.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.stripchart;

import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.awt.*;
import java.text.DecimalFormat;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class XScaleAnnotator
//

public class XScaleAnnotator extends Plotter{

    DecimalFormat decimalFormat;    
    
//-----------------------------------------------------------------------------
// XScaleAnnotator::XScaleAnnotator (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public XScaleAnnotator(Settings pSettings, IniFile pConfigFile, int pChartGroup,
            StripChart pChart,
            int pChartIndex, int pPlotterIndex, PlotterGlobals pPlotterGlobals,
            Color pBackgroundColor, Color pGridColor, double pInchesPerPixel)
{

    settings = pSettings; configFile = pConfigFile;
    chartGroup = pChartGroup; chart = pChart;
    chartIndex = pChartIndex; plotterIndex = pPlotterIndex;
    gridColor = pGridColor;
    plotterGlobals = pPlotterGlobals;
    inchesPerPixel = pInchesPerPixel;
    backgroundColor = pBackgroundColor;

    typeDescriptor = "X Axis Annotator";

}//end of XScaleAnnotator::XScaleAnnotator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// XScaleAnnotator::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    super.init();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

    decimalFormat = new  DecimalFormat("0.0");
   
}//end of XScaleAnnotator::init
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

}//end of Trace::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// XScaleAnnotator::resetAll
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

}//end of XScaleAnnotator::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Trace::drawGrid
//
// Draws the grid marks
//

void drawGrid(Graphics2D pG2, int pXPos, int pVertSpacing, int pCanvasYLimit)
{

    //for screen display, zero width/height for grid pixels looks best
    //when rendering for printing, must set width to 1 or pixels disappear
    int width = settings.printMode ?  1 : 0;

    pG2.setColor(gridColor);

    for(int i = (pVertSpacing-1); i < pCanvasYLimit; i+=pVertSpacing){
        pG2.drawRect(pXPos, i, width, 0);
    }

}//end of Trace::drawGrid
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// XScaleAnnotator::paintComponent
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

    //for repainting, the gridCounter starts at one to sync up with drawing by
    //the plotNewData code

    int gridCounter = plotterGlobals.scrollCount;
    
    double locLeftEdgeInInches = gridCounter * inchesPerPixel;
    
    double numMajorGridLines = 
        (int)Math.ceil(locLeftEdgeInInches  / X_AXIS_ANNOTATION_SPACING_INCHES);

    double nextMajorGridLoc =
                        (numMajorGridLines+1)*X_AXIS_ANNOTATION_SPACING_INCHES;

    pG2.setColor(gridColor);
    
    for(int i = 0; i < canvas.getWidth(); i++){
        
        double locInInches = gridCounter++ * inchesPerPixel;

        if (locInInches >= nextMajorGridLoc){ 

            pG2.drawString(decimalFormat.format(locInInches/12), i, 10);
            
            nextMajorGridLoc += X_AXIS_ANNOTATION_SPACING_INCHES;;
        }
    }

}//end of XScaleAnnotator::paintComponent
//-----------------------------------------------------------------------------

}//end of class XScaleAnnotator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------