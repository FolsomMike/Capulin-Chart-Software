/******************************************************************************
* Title: Oscilloscope.java
* Author: Mike Schoonover
* Date: 11/14/03
*
* Purpose:
*
* This class displays signals on a component which mimics an oscilloscope.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.hardware.Channel;
import chart.mksystems.settings.Settings;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.border.*;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class OscopeCanvas
//
// This panel is used to draw the oscilloscope data traces and decorations.
// This class actually does all the work for the Oscilloscope class.
//

class OscopeCanvas extends JPanel {

    Settings settings;
    public BufferedImage imageBuffer;
    Channel channel;
    boolean persistMode = false;
    double uSPerDataPoint;
    boolean dacEnabled = false;
    boolean dacLocked = true;
    int maxX, maxY;
    Xfer aScanPeakInfo;
    Color bgColor;

    int vertOffset = 0; //vertical offset for the trace and gates

//-----------------------------------------------------------------------------
// OscopeCanvas::OscopeCanvas (constructor)
//
// NOTE: Variable channel is not set here - setChannel must be called before
// the scope is allowed to update.
//

public OscopeCanvas(int pMaxX, int pMaxY, double pUSPerDataPoint,
        MouseListener pMouseListener, MouseMotionListener pMouseMotionListener,
                                                             Settings pSettings)
{

    maxX = pMaxX; maxY = pMaxY;
    uSPerDataPoint = pUSPerDataPoint; settings = pSettings;

    bgColor = new Color(153, 204, 0);

    aScanPeakInfo = new Xfer();

    //we will handle drawing the background
    setOpaque(false);

    setMinimumSize(new Dimension(maxX,maxY));
    setPreferredSize(new Dimension(maxX,maxY));
    setMaximumSize(new Dimension(maxX,maxY));

    setBackground(bgColor);

    setName("Oscope Canvas"); //used by the mouse listener
    addMouseListener(pMouseListener);
    addMouseMotionListener(pMouseMotionListener);

}//end of OscopeCanvas::OscopeCanvas (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::setChannel
//
// Sets the channel which is to be displayed on the scope.  This must be
// called before the scope is allowed to update.
//

public void setChannel(Channel pChannel)
{

    channel = pChannel;

}//end of OscopeCanvas::setChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::setPersistMode
//
// Sets the persistence mode -- if false, the scope display is erased
// constantly. If true, the display is not erased between refresh cycles so
// old data is left in place to show the signal history.
//

public void setPersistMode(boolean pState)
{

    persistMode = pState;

}//end of OscopeCanvas::setPersistMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::setDACEnabled
//
// Sets the dacEnabled flag.  If true, the DAC gates will be displayed.
//

public void setDACEnabled(boolean pEnabled)
{

    dacEnabled = pEnabled;

}//end of OscopeCanvas::setDACEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::setDACLocked
//
// Sets the dacLocked flag.  If true, the selection circles for the gates
// will not be drawn.
//

public void setDACLocked(boolean pEnabled)
{

    dacLocked = pEnabled;

}//end of OscopeCanvas::setDACLocked
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::setVertOffset
//
// Sets the vertical offset for the trace(s) and gate(s) to shift them up or
// down.
//
// A larger number for pVertOffset shifts upwards.  The value is not inverted
// here to account for 0,0 being at the top left of the screen because the
// offset is later applied before inversion of the data point.
//

public void setVertOffset(int pVertOffset)
{

    vertOffset = pVertOffset;

}//end of OscopeCanvas::setVertOffset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::getVertOffset
//
// Returns the vertical offset for the trace(s) and gate(s) to shift them up or
// down.  See setVertOffset for more info.
//

public int getVertOffset()
{

    return vertOffset;

}//end of OscopeCanvas::getVertOffset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::createImageBuffer
//
// This function must be called after the GUI has been created and packed as
// the width and height of the canvas is not set in stone until that time
// and will show as 0.
//

public void createImageBuffer()
{

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gs = ge.getDefaultScreenDevice();
    GraphicsConfiguration gc = gs.getDefaultConfiguration();

    //Java tutorial suggests checking the following cast with "instanceof", to
    //avoid runtime errors but this seems pointless as the cast MUST work for
    //the program to work so it will crash regardless if the cast is bad

    int width = getWidth(); if (width == 0) {width = 1;}
    int height = getHeight(); if (height == 0) {height = 1;}

    //create an image to store the plot on so it can be copied to the screen
    //during repaint
    imageBuffer =
                (gc.createCompatibleImage(width, height, Transparency.OPAQUE));

}//end of OscopeCanvas::createImageBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::paintComponent
//

@Override
public void paintComponent(Graphics g)

{

    Graphics2D g2 = (Graphics2D) g;

    super.paintComponent(g2); //paint background

    g2.drawImage(imageBuffer, 0, 0, null);

}//end of OscopeCanvas::paintComponent
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::displayData
//
// Displays the data in pData.  The scope display will be redrawn.
//

public void displayData(int pRange, int pInterfaceCrossingPosition,
                                                int[]pData, Channel pChannel)

{

    //draw on the image buffer
    Graphics2D g2 = (Graphics2D) imageBuffer.getGraphics();

    //if not in Persistence Mode, erase the old data before drawing the new,
    //else leave old data in place to show signal history
    if(!persistMode){
        g2.setColor(bgColor);
        g2.fillRect(0, 0, getWidth(), getWidth());
    }

    //draw the grid lines on the screen before anything else
    drawGrid(g2);

    g2.setColor(Color.BLACK);

    int yPos, scaledI;
    int lastX = 0, lastY = maxY; //start at bottom left corner

    for(int i = 0; i < pData.length; i++){

         //apply the offset before inverting
         yPos = pData[i] + vertOffset;

        //limit y before inverse to prevent problems
         if (yPos < 0) {yPos = 0;}
         else
         if (yPos > maxY) {yPos = maxY;}

         //invert y value so 0,0 is at bottom left, then shift by vertOffset
         int yInv = maxY - yPos;

        //calculate X position to reflect range setting - the time per pixel
        //varies with range and will not match the time per data point
        scaledI = (int)((i * uSPerDataPoint * pRange) / pChannel.uSPerPixel);

        //stop drawing when right edge of canvas reached
        //pData.length will often be larger than this because it may be
        //compressed, but no need to display past the edge of the canvas
        if (scaledI > getWidth()-3) {break;}

        //debug mks
        // The getWidth()-3 is used above as a quick fix to get rid of the
        // random spike sometimes seen at the right edge of the screen.  This is
        // due to the compressed or expanded data returned by the DSP not
        // filling the transmit buffer exactly due to round off errors or not
        // quite enough data being collected by the FPGA to perfectly fill the
        // buffer.
        // Need to set the entire buffer to a NULL value (such as max int) when
        // the scale ratio is set in the DSP.  The UTBoard code could then
        // detect this value and pass the size of the valid data to be scanned
        // to this function instead of it using pData.length.
        //end debug mks

        g2.drawLine(lastX, lastY, scaledI, yInv);

        lastX = scaledI; lastY = yInv;

    }//for(int i = 0; i

    //draw the gates on the scope, use g2 graphics object as it is for buffered
    //image
    drawGates(channel, g2, pInterfaceCrossingPosition);

    //draw the DAC gates on the scope, use g2 graphics object as it is for
    //buffered image
    if (dacEnabled) {drawDACGates(channel, g2, pInterfaceCrossingPosition);}

    //display the image buffer on the screen
    //NOTE - need to get a graphics object for the canvas - don't use g2 from
    //this function as that is for the buffered image
    paintComponent(getGraphics());

}//end of OscopeCanvas::displayData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::drawGates
//
// Draws the gates on the scope.
//

public void drawGates(Channel pChannel, Graphics2D pG2,
                                                int pInterfaceCrossingPosition)
{

    // draw each gate and peak indicator for the channel

    for (int i = 0; i < pChannel.numberOfGates; i++){

        //draw each gate
        pG2.setColor(Color.RED);
        pG2.drawLine(
                pChannel.gates[i].gatePixStartAdjusted,
                pChannel.gates[i].gatePixLevel,
                pChannel.gates[i].gatePixEndAdjusted,
                pChannel.gates[i].gatePixLevel
                );

        //if peak capture display is on, draw the peak recorded for that gate
        //since the last draw

        pChannel.gates[i].getAndClearAScanPeak(aScanPeakInfo);

        int peak = aScanPeakInfo.rInt1;
        int flightTime = aScanPeakInfo.rInt2;

        //invert level so 0,0 is at bottom left

        if (peak < 0) {peak = 0;} if (peak > maxY) {peak = maxY;}
        peak = maxY - peak;

        flightTime -= pChannel.getSoftwareDelay();
        int peakPixelLoc =
          (int) ((flightTime * pChannel.nSPerDataPoint) / pChannel.nSPerPixel);

        //display a red line in the center of the gate which shows the height of
        //the peak

        if(settings.showRedPeakLineInGateCenter){
            pG2.setColor(Color.RED);
            pG2.drawLine(
                    pChannel.gates[i].gatePixMidPointAdjusted, maxY,
                    pChannel.gates[i].gatePixMidPointAdjusted, peak
                    );
        }

        //display a red line at the peak's location in the gate which shows the
        //height of the peak

        if(settings.showRedPeakLineAtPeakLocation){
            pG2.setColor(Color.RED);
            pG2.drawLine(peakPixelLoc, maxY, peakPixelLoc, peak);
        }

        //draw a pseudo signal peak where it would have been displayed had
        //it been caught in the aScan sample set -- draw a triangle to mimic
        //an actual peak (somewhat)

        if(settings.showPseudoPeakAtPeakLocation){
            pG2.setColor(Color.BLACK);
            pG2.drawLine(peakPixelLoc - 5, maxY, peakPixelLoc, peak);
            pG2.drawLine(peakPixelLoc + 5, maxY, peakPixelLoc, peak);
        }

    }// for (int i = 0; i < pChannel.numberOfGates; i++)

    //if interface tracking is on, mark the point where the interface has
    //crossed the interface gate - the interface gate is always gate 0
    if (pInterfaceCrossingPosition > -1){
        pG2.drawLine(
                pInterfaceCrossingPosition,
                pChannel.gates[0].gatePixLevel - 10,
                pInterfaceCrossingPosition,
                pChannel.gates[0].gatePixLevel + 10
                );
    }

}//end of OscopeCanvas::drawGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::drawDACGates
//
// Draws the DAC gates on the scope.
//

public void drawDACGates(Channel pChannel, Graphics2D pG2,
                                                int pInterfaceCrossingPosition)
{

    // draw each DAC gate for the channel

    for (int i = 0; i < pChannel.numberOfDACGates; i++){

        if (pChannel.dacGates[i].getActive()){

            if (pChannel.dacGates[i].getSelected()) {
                pG2.setColor(Color.WHITE);
            }
            else {
                pG2.setColor(Color.BLUE);
            }

            //when DAC is unlocked and can be modified, draw selection circles
            if (!dacLocked) {
                pG2.drawOval(pChannel.dacGates[i].gatePixStartAdjusted - 5,
                             pChannel.dacGates[i].gatePixLevel - 5,
                             10, 10);
            }

            //draw the gate
            pG2.drawLine(
                pChannel.dacGates[i].gatePixStartAdjusted,
                pChannel.dacGates[i].gatePixLevel,
                pChannel.dacGates[i].gatePixEndAdjusted,
                pChannel.dacGates[i].gatePixLevel
                );

            //if the subsequent gate is also active, draw a line connecting the
            //two so you get a profile line instead of gates
            //draw the gate

            if ( (i+1) < pChannel.numberOfDACGates
                     && pChannel.dacGates[i+1].getActive()) {

                pG2.drawLine(
                    pChannel.dacGates[i].gatePixEndAdjusted,
                    pChannel.dacGates[i].gatePixLevel,
                    pChannel.dacGates[i+1].gatePixStartAdjusted,
                    pChannel.dacGates[i+1].gatePixLevel
                    );
            }
        }
    }// for (int i = 0; i < pChannel.numberOfDACGates; i++)

}//end of OscopeCanvas::drawDACGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// OscopeCanvas::drawGrid
//
// Draws the grid lines on the scope.
//

public void drawGrid(Graphics2D pG2)

{

    pG2.setColor(Color.LIGHT_GRAY);

    int width = getWidth(), height = getHeight();
    int interval = width / 10;

    for (int i = interval; i < width; i+=interval){
        //draw the vertical lines
        pG2.drawLine(i, 0, i, height  );
    }

    interval = height / 10;

    for (int i = interval; i < height; i+=interval){
        //draw the horizontal lines
        pG2.drawLine(0, i, width, i);
    }

}//end of OscopeCanvas::drawGrid
//-----------------------------------------------------------------------------


}//end of class OScopeCanvas
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class HorizontalGridLabels
//
// This panel draws grid labels for the horizontal axis of the scope.
//

class HorizontalGridLabels extends JPanel {

    int width, height;

//-----------------------------------------------------------------------------
// HorizontalGridLabels::HorizontalGridLabels (constructor)
//
//

public HorizontalGridLabels(int pWidth)
{

    width = pWidth; height = 13;

}//end of HorizontalGridLabels::HorizontalGridLabels (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// HorizontalGridLabels::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //we will handle drawing the background
    setOpaque(false);

    setMinimumSize(new Dimension(width, height));
    setPreferredSize(new Dimension(width, height));
    setMaximumSize(new Dimension(width, height));

}//end of HorizontalGridLabels::HorizontalGridLabels (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// HorizontalGridLabels::paintComponent
//

@Override
public void paintComponent(Graphics g)

{

    Graphics2D g2 = (Graphics2D) g;

    super.paintComponent(g2); //paint background

    //calculate spacing between grids
    int division = width / 10;

    //draw number under each vertical grid line

    for (int i = 1; i < 10; i++){
        g2.drawString("" + i, (i*division)-3, 12);
    }

}//end of HorizontalGridLabels::paintComponent
//-----------------------------------------------------------------------------

}//end of class HorizontalGridLabels
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Oscilloscope
//
// This class creates and controls an oscilloscope display.
//

public class Oscilloscope extends JPanel {

    Channel channel;
    OscopeCanvas canvas;
    Settings settings;
    Color bgColor;

    HorizontalGridLabels horizontalGridLabels;
    boolean displayHorizontalGridlabels = true;

//-----------------------------------------------------------------------------
// Oscilloscope::Oscilloscope (constructor)
//
// NOTE: Variable channel is not set here - setChannel must be called before
// the scope is allowed to update.
//

public Oscilloscope(String pTitle, double pUSPerDataPoint,
        MouseListener pMouseListener, MouseMotionListener pMouseMotionListener,
        Settings pSettings)
{

    settings = pSettings;

    bgColor = new Color(153, 204, 0);

    //set up the main panel - this panel does nothing more than provide a title
    //border and a spacing border
    setOpaque(true);

    //change the layout manager
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    setBorder(BorderFactory.createTitledBorder(pTitle));

    int scopeWidth = 350, scopeHeight = 350;

    //create a Canvas object to be placed on the main panel - the Canvas object
    //provides a panel and methods for drawing data - all the work is actually
    //done by the Canvas object
    canvas = new OscopeCanvas(scopeWidth, scopeHeight, pUSPerDataPoint,
                                pMouseListener, pMouseMotionListener, settings);

    add(canvas);

    //if enabled, add a panel with labels for the horizontal axis
    if (displayHorizontalGridlabels){
        horizontalGridLabels = new HorizontalGridLabels(scopeWidth);
        horizontalGridLabels.init();
        add(horizontalGridLabels);
    }

}//end of Oscilloscope::Oscilloscope (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::setChannel
//
// Sets the channel which is to be displayed on the scope.  This must be
// called before the scope is allowed to update.
//

public void setChannel(Channel pChannel, String pGroupTitle)
{

    channel = pChannel;

    if (channel == null) {return;}

    canvas.setChannel(channel);

    //set the title of the border for the main panel to the channel's title
    ((TitledBorder)getBorder()).setTitle(channel.title + " " + pGroupTitle);

}//end of Oscilloscope::setChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::getOscopeCanvas
//
// Returns a reference to the canvas used by the Oscope as a JPanel.
//

public JPanel getOscopeCanvas()
{

    return canvas;

}//end of Oscilloscope::getOscopeCanvas
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::setPersistMode
//
// Sets the persistence mode -- if false, the scope display is erased
// constantly. If true, the display is not erased between refresh cycles so
// old data is left in place to show the signal history.
//

public void setPersistMode(boolean pState)
{

    canvas.setPersistMode(pState);

}//end of Oscilloscope::setPersistMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::setTitle
//
// Sets the title of the scope.
//

public void setTitle(String pTitle)
{

    //set the title of the border for the main panel
    ((TitledBorder)getBorder()).setTitle(pTitle);

}//end of Oscilloscope::setTitle
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::displayData
//
// Displays the data in pData.  The scope display will be redrawn.
//

public void displayData(int pRange, int pInterfaceCrossingPosition, int[]pData)

{

    canvas.displayData(pRange, pInterfaceCrossingPosition, pData, channel);

}//end of Oscilloscope::displayData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::createImageBuffer
//
// Creates an image buffer to hold the profile plot.
//

public void createImageBuffer()
{

    canvas.createImageBuffer();

}//end of Oscilloscope::createImageBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Oscilloscope::clearPlot
//
// Erases the plot area.
//

public void clearPlot()
{

    Graphics gb = canvas.imageBuffer.getGraphics();

    gb.setColor(bgColor);
    gb.fillRect(0, 0, canvas.getWidth(), canvas.getWidth());

    canvas.paintComponent(canvas.getGraphics());

}//end of Oscilloscope::clearPlot
//-----------------------------------------------------------------------------

}//end of class Oscilloscope
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
