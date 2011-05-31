/******************************************************************************
* Title: UTCalibrator.java
* Author: Mike Schoonover
* Date: 4/27/09
*
* Purpose:
*
* This class displays a window for calibrating charts and hardware.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.awt.font.TextAttribute;

import chart.mksystems.globals.Globals;
import chart.mksystems.stripchart.StripChart;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.hardware.Channel;
import chart.mksystems.hardware.Gate;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTCalibrator
//
// This class displays a text area in a window.
//

public class UTCalibrator extends JDialog implements ActionListener, 
         WindowListener, MouseListener, MouseMotionListener, ComponentListener{

public Oscilloscope scope1;
JPanel channelSelector;
JButton minMax, viewIP;
Globals globals;

public int currentChannelIndex=0;
int numberOfChannels; //number of channels in the current group (chart)
int numberOfChannelsInSystem; //number of all channels in the system
StripChart chart;
Hardware hardware;
public Channel[] channels; //channels in the current group (chart)
public Channel[] allChannels; //all channels in the system
Gate gate;
UTControls utControls;

double previousDelay;

ButtonGroup channelSelectorGroup;

Font blackFont, redFont;

//-----------------------------------------------------------------------------
// UTCalibrator::UTCalibrator (constructor)
//
//
  
public UTCalibrator(JFrame pFrame, Hardware pHardware, Globals pGlobals)
{

super(pFrame, "Calibration");

addWindowListener(this);
addComponentListener(this);

hardware = pHardware; globals = pGlobals;

//create red and black fonts for use with display objects
Hashtable<TextAttribute, Object> map =
            new Hashtable<TextAttribute, Object>();
blackFont = new Font("Dialog", Font.PLAIN, 12);
map.put(TextAttribute.FOREGROUND, Color.RED);
redFont = blackFont.deriveFont(map);

Container contentPane = getContentPane();
contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

//create the channel selector panel with a dummy label
channelSelector = new JPanel();
channelSelector.setLayout(new BoxLayout(channelSelector, BoxLayout.Y_AXIS));
channelSelector.setOpaque(true);
channelSelector.setBorder(BorderFactory.createTitledBorder("Channels"));
channelSelector.setAlignmentY(Component.TOP_ALIGNMENT);
contentPane.add(channelSelector);

//create a panel to hold the scope and control panels
JPanel panel = new JPanel();
panel.setAlignmentY(Component.TOP_ALIGNMENT);
panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

//create a panel to hold the scope and alarm panel
JPanel scopeAndAlarms = new JPanel();
scopeAndAlarms.setAlignmentX(Component.LEFT_ALIGNMENT);
scopeAndAlarms.setLayout(new BoxLayout(scopeAndAlarms, BoxLayout.X_AXIS));

//create the oscilloscope
scope1 = new Oscilloscope("Scope", hardware.getUSPerDataPoint(), this, this);
scope1.setAlignmentY(Component.TOP_ALIGNMENT);
scopeAndAlarms.add(scope1);

//create a panel to hold alarm flags and various controls
JPanel alarms = new JPanel();
alarms.setLayout(new BoxLayout(alarms, BoxLayout.Y_AXIS));
alarms.setOpaque(true);
alarms.setBorder(BorderFactory.createTitledBorder("Alarms"));
alarms.setAlignmentY(Component.TOP_ALIGNMENT);

minMax = new JButton("Minimize");
minMax.setAlignmentX(Component.CENTER_ALIGNMENT);
minMax.addActionListener(this);
minMax.setActionCommand("Min / Max");
minMax.setToolTipText("Minimize or Maximize the scope display.");
alarms.add(minMax);

//add invisible filler to spread buttons
alarms.add(Box.createVerticalGlue());

viewIP = new JButton("View IP");
viewIP.setAlignmentX(Component.CENTER_ALIGNMENT);
viewIP.addMouseListener(this);
viewIP.setName("View IP"); //used by the mouse listener functions
viewIP.setToolTipText("Temporarily set delay to zero to view initial pulse.");
alarms.add(viewIP);

scopeAndAlarms.add(alarms);
panel.add(scopeAndAlarms);

utControls = new UTControls(pFrame, scope1.getOscopeCanvas(), hardware);
utControls.setAlignmentX(Component.LEFT_ALIGNMENT);
panel.add(utControls);

contentPane.add(panel);

pack();

}//end of UTCalibrator::UTCalibrator (constructor)
//-----------------------------------------------------------------------------    

//-----------------------------------------------------------------------------
// UTCalibrator::displayData
//
// Redraws the scope.
//

void displayData(int pRange, int pInterfaceCrossingPosition, int[] pUTAscanData)
{

Channel ch = channels[currentChannelIndex]; //use shorter name

// if pInterfaceCrossingPosition is -1, then tracking is off
// if greater than -1, then tracking is on

//adjust the gate start and end positions
//if interface tracking is off, the positions are transferred as is
//if interface tracking is on, the positions are added to the interface
//crossing point so that they are relative to the interface

if (!ch.getInterfaceTracking()){

    for (int i = 0; i < ch.getNumberOfGates(); i++){
        ch.gates[i].adjustPositionsNoTracking();
        //set to -1 to flag the interface tracking is off
        ch.gates[0].interfaceCrossingPixAdjusted = -1;
        }

    for (int i=0; i < ch.getNumberOfDACGates(); i++)
        if (ch.dacGates[i].getActive())
            ch.dacGates[i].adjustPositionsNoTracking();

    }//if (!channels[currentChannelIndex].getInterfaceTracking())
else{

    //translate the interface crossing position from relative to the initial
    //pulse to relative to the left edge of the scope display
    //multiply pInterfaceCrossingPosition by .015 to convert from samples
    //of 15 ns each to total us
    //interface gate MUST be gate 0 for compatibility with DSP code
    
    ch.gates[0].interfaceCrossingPixAdjusted =
     (int)((double)((pInterfaceCrossingPosition * 0.015)
        / ch.uSPerPixel) - ch.delayPix);

    int offset = ch.gates[0].interfaceCrossingPixAdjusted;

    //compute all gate start positions relative to the interface crossing
    // do not do this for gate 0 - the interface gate is always positioned
    // absolutely
    //interface gate MUST be gate 0 for compatibility with DSP code

    for (int i = 1; i < ch.getNumberOfGates(); i++)
        ch.gates[i].adjustPositionsWithTracking(offset);
        
    //interface gate does not track the interface
    ch.gates[0].adjustPositionsNoTracking();

    //translate the positions for the DAC gates
    for (int i=0; i < ch.getNumberOfDACGates(); i++)
        if (ch.dacGates[i].getActive())
            ch.dacGates[i].adjustPositionsWithTracking(offset);

    }// else of if (!ch.getInterfaceTracking())

if (pUTAscanData != null && scope1 != null)
    scope1.displayData(pRange, ch.gates[0].interfaceCrossingPixAdjusted,
                                                                pUTAscanData);

}//end of UTCalibrator::displayData
//-----------------------------------------------------------------------------    

//-----------------------------------------------------------------------------
// UTCalibrator::setChannels
//
// Sets the reference to the array of channels to be accessed in the calibration
// window.  Also sets the chart which contains the trace connected to the
// channel.
//
// This must be called before the window is made visible or the scope is allowed
// to update.
//

public void setChannels(int pNumberOfChannels, Channel[] pChannels,
      StripChart pChart, int pNumberOfChannelsInSystem, Channel[] pAllChannels)
{

numberOfChannelsInSystem = pNumberOfChannelsInSystem;
allChannels = pAllChannels;

numberOfChannels = pNumberOfChannels; channels = pChannels;
chart = pChart;

//set the current channel to that last viewed for the current chart
//the first time, through the value will be 0, the first channel

currentChannelIndex = chart.lastAScanChannel;

scope1.setChannel(channels[currentChannelIndex], chart.getTitle());

//this also recalculates all UT values
utControls.setChannel(chart, channels[currentChannelIndex]);

setupChannelSelectorPanel();

pack();
repaint(); //force update of label on scope

}//end of UTCalibrator::setChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::setupChannelSelectorPanel
//
// Sets up the panel for selecting which channel to display so that it
// reflects the number and names of the available channels.
//

public void setupChannelSelectorPanel()
{

//if channelSelectorGroup is not null, then this function has already been
//called so remove previous components before installing new ones
if (channelSelectorGroup != null)
    channelSelector.removeAll();

//setting the pointer to a new object will release the old one if this
//function has been called before
channelSelectorGroup = new ButtonGroup();

JRadioButton rb;

for (int i=0; i < numberOfChannels; i++){

    rb = new JRadioButton(channels[i].shortTitle);

    channels[i].calRadioButton = rb; //store a reference to the button

    //if the channel is off, display in red and change tool tip
    setChannelSelectorColor(channels[i]);

    rb.addActionListener(this);

    rb.setActionCommand(Integer.toString(i)); //use index as the action string

    // select the currently active channel
    if (i == currentChannelIndex) rb.setSelected(true);

    channelSelectorGroup.add(rb); //group the radio buttons

    channelSelector.add(rb); //add the button to the panel

    }


channelSelector.add(Box.createRigidArea(new Dimension(0,15))); //vertical spacer

//add a button for copying the selected channel settings to all channels
JButton b = new JButton("Copy to All");
b.addActionListener(this);
b.setActionCommand("Copy to All");
b.setToolTipText("Copy settings for selected channel to all other channels.");
channelSelector.add(b);

}//end of UTCalibrator::setupChannelSelectorPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::setChannelSelectorColor
//
// Sets the selector radio button to black if the channel is on or red if the
// channel is off.  Also sets the tooltip to an appropriate message for the
// state.
//

void setChannelSelectorColor(Channel pChannel)
{

//if the channel is off, display selector button in red and change tool tip
//else display in black and use normal tool tip

if (pChannel.getMode() == 4){
    ((JRadioButton)pChannel.calRadioButton).setFont(redFont);
    ((JRadioButton)pChannel.calRadioButton).setToolTipText(
     pChannel.title + " - This channel is Off - see Signal Tab / Mode Panel");
    }
else{
    ((JRadioButton)pChannel.calRadioButton).setFont(blackFont);
    ((JRadioButton)pChannel.calRadioButton).setToolTipText(pChannel.title);
    }

}//end of UTCalibrator::setChannelSelectorColor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::copyToAllChannelsForCurrentChart
//
// Copies the values for the currently selected channel to all the other
// channels for the currently selected chart.
//
// WARNING:  The source and destination channels must have the same number and
// types of gates.
//

public void copyToAllChannelsForCurrentChart()
{

//copy from currently selected channel to all channels associated with the
//same group (chart)

copyToAllHelper(channels[currentChannelIndex], channels, numberOfChannels);

}//end of UTCalibrator::copyToAllChannelsForCurrentChart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::copyToAllChannelsForAllCharts
//
// Copies the values for the currently selected channel to all the other
// channels for all other charts.
//
// WARNING:  The source and destination channels must have the same number and
// types of gates.
//

public void copyToAllChannelsForAllCharts()
{

//copy from currently selected channel to all channels associated with all
//same groups (charts)

copyToAllHelper(channels[currentChannelIndex], allChannels,
                                                     numberOfChannelsInSystem);

}//end of UTCalibrator::copyToAllChannelsForAllCharts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::copyToAllHelper
//
// Copies the info from channel in pSource to all the channels in
// pDestChannels.
//
// WARNING:  The source and destination channels must have the same number and
// types of gates.
//

public void copyToAllHelper(Channel pSource, Channel[] pDestChannels,
                                                          int pNumDestChannels)
{

int numberOfGates, numberOfDACGates;

//get the number of gates for the channel - note that all channels in the
//destination group should have the same number of channels for this to work
//properly

numberOfGates = pSource.getNumberOfGates();
numberOfDACGates = pSource.getNumberOfDACGates();

//scan through all channels and copy info from currently selected channel

// each call to set a value will set the trigger flag for the sending thread
// to send the value next time it runs -- if it runs between setting of two
// values tied to the same flag, a duplicate call might be made but causes no
// harm
// all setting / sending functions are synchronized so the value setting and
// flag setting/clearing by different threads is protected against collision

for (int ch = 0; ch < pNumDestChannels; ch++){

    //copy the gate info for all gates of the channels
    for (int g = 0; g < numberOfGates; g++){

        pDestChannels[ch].setGateStart(g, pSource.getGateStart(g), false);
        pDestChannels[ch].setGateStartTrackingOn(
                                    g, pSource.getGateStartTrackingOn(g));
        pDestChannels[ch].setGateStartTrackingOff(
                                    g, pSource.getGateStartTrackingOff(g));
        pDestChannels[ch].setGateWidth(g, pSource.getGateWidth(g), false);
        pDestChannels[ch].setGateLevel(g, pSource.getGateLevel(g), false);
        pDestChannels[ch].setGateHitCount(g, pSource.getGateHitCount(g), false);
        pDestChannels[ch].setGateMissCount(g, pSource.getGateMissCount(g),
                                                                         false);
        }

    //copy the DAC gate info
    for (int dg = 0; dg < numberOfDACGates; dg++){
        pDestChannels[ch].copyGate(dg, pSource.dacGates[dg]);
        }

    //copy the non-gate info for the channels

    //always set range after setting gate position or width, delay and interface
    //tracking as these affect the range

    //set the pForceUpdate flags false so that the values are only sent to the
    //DSPs if they have changed

    //wip mks -- need to convert into synced functions

    pDestChannels[ch].setSoftwareGain(pSource.getSoftwareGain(), false);
    pDestChannels[ch].setDelay(pSource.getDelay(), false);
    pDestChannels[ch].setInterfaceTracking(pSource.getInterfaceTracking(),
                                                                        false);
    pDestChannels[ch].setRange(pSource.getRange(), false);
    pDestChannels[ch].setMode(pSource.getMode(), false);
    pDestChannels[ch].setHardwareGain(pSource.getHardwareGain1(),
                                            pSource.getHardwareGain2(), false);
    pDestChannels[ch].setRejectLevel(pSource.getRejectLevel(), false);
    pDestChannels[ch].setAScanSmoothing(pSource.getAScanSmoothing(), false);
    pDestChannels[ch].setDCOffset(pSource.getDCOffset(), false);

    pDestChannels[ch].setDACEnabled(pSource.getDACEnabled(), false);

    //updates the channel number color to match the channel's on/off state
    //if all system channels are being copied, not all of those channels will
    //be in the group currently being displayed by the Calibrator window, those
    //not in the group will not have a radio button to set so skip them
    if (pDestChannels[ch].calRadioButton != null)
        setChannelSelectorColor(pDestChannels[ch]);

    }// for (int ch = 0; ch < pNumDestChannels; ch++)

}//end of UTCalibrator::copyToAllHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::viewIP
//
// Stores the current delay value and then sets the delay to zero so the
// initial pulse can be viewed.
//
// A call to unViewIP restores the delay value to its state before calling this
// function.
//

public void viewIP()
{

Channel currentCh = channels[currentChannelIndex];

//catch null - this can happen if not enough channels are installed in the
//chassis or a mistake is made in the config file assigning channels to
//traces
if (currentCh == null) return;

//store the current delay value so it can be restored
previousDelay = currentCh.getDelay();

//set the delay to zero so the initial pulse is displayed
currentCh.setDelay(0.0, false);

// the FPGA is always set up to capture data from the earliest point viewed or
// gated until the last point viewed or gated - this may be too much data,
// especially if there is a large water path, so the gates are temporarily set
// to start of zero and width of zero

// the gate width of zero might still cause the DSPs to scan one point of the
// gate and return a value, but shouldn't really cause a problem - trace or
// signal might change during View IP, but will return to normal afterwards

for (int g = 0; g < currentCh.getNumberOfGates(); g++){
    currentCh.setPreviousGateStart(g, currentCh.getGateStart(g));
    currentCh.setGateStart(g, 0, false);
    currentCh.setPreviousGateWidth(g, currentCh.getGateWidth(g));
    currentCh.setGateWidth(g, 0, false);
    }

//disable updates so the controls don't respond while they are being updated
//the values have already been written to the variables and if the controls
//respond they may wipe out the values already set

utControls.updateEnabled = false;

// update the display so user can see that delay is set to zero
// must cast the constant to a double or it somehow stores as an integer in
// the MFloatSpinner and causes a casting crash when using getDoubleValue()
utControls.delaySpin.spinner.setValue((double)0);

//must update the screen controls before calling updateAllSettings or that
//function will overwrite the variables just changed with the values in the
//display
utControls.updateGateControls(currentCh);

utControls.updateEnabled = true;

// call updateAllSettings to make sure everything is recalculated and set
utControls.updateAllSettings(false);

}//end of UTCalibrator::viewIP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::unViewIP
//
// Restores the current delay value with the value saved by viewIP.
//

public void unViewIP()
{

Channel currentCh = channels[currentChannelIndex];

//catch null - this can happen if not enough channels are installed in the
//chassis or a mistake is made in the config file assigning channels to
//traces
if (currentCh == null) return;

//set the delay back to the value before viewIP
currentCh.setDelay(previousDelay, false);

//set the gates back to their settings before viewIP
for (int g = 0; g < currentCh.getNumberOfGates(); g++){
    currentCh.setGateStart(g, currentCh.getPreviousGateStart(g), false);
    currentCh.setGateWidth(g, currentCh.getPreviousGateWidth(g), false);
    }

//disable updates so the controls don't respond while they are being updated
//the values have already been written to the variables and if the controls
//respond they may wipe out the values already set

utControls.updateEnabled = false;

//update the display
utControls.delaySpin.spinner.setValue(previousDelay * utControls.timeDistMult);

//must update the screen controls to use updateAllSettings or that function
//will overwrite the variables just changed with the values in the display
utControls.updateGateControls(currentCh);

utControls.updateEnabled = true;

// call updateAllSettings to make sure everything is recalculated and set
utControls.updateAllSettings(false);

}//end of UTCalibrator::unViewIP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{ 

//trap "Copy to All" button
if (e.getActionCommand().equals("Copy to All")){

    if (globals.copyToAllMode == 0) copyToAllChannelsForCurrentChart();
    else
    if (globals.copyToAllMode == 1) copyToAllChannelsForAllCharts();

    return;
    }

//trap "All" radio button
if (e.getActionCommand().equals("All")){
    return;
    }

//trap "Minimize" / "Maximize" button to shrink or expand the scope display
if (e.getActionCommand().equals("Min / Max")){

    if (channelSelector.isVisible()){ //hide stuff to shrink the window
        channelSelector.setVisible(false);
        utControls.setVisible(false);
        minMax.setText("Maximize");
        }
    else{ //show stuff and expand the window
        channelSelector.setVisible(true);
        utControls.setVisible(true);
        minMax.setText("Minimize");
        }

    pack(); //force resizing of the window
    repaint();
    return;
    }

//if not trapped above, action command is the number of the selected channel

//disable fast AScan processing in the DSP for most efficient operation
channels[currentChannelIndex].setAScanFastEnabled(false, false);

currentChannelIndex = Integer.valueOf(e.getActionCommand());

//enable fast AScan processing in the DSP for the newly selected channel
channels[currentChannelIndex].setAScanFastEnabled(true, false);

//store current channel so it can be used when the window is closed & reopened
chart.lastAScanChannel = currentChannelIndex;

scope1.setChannel(channels[currentChannelIndex], chart.getTitle());

utControls.setChannel(chart, channels[Integer.valueOf(e.getActionCommand())]);

//mask all channels except the selected one when the window is active so only
//the data from the selected channel is applied to the trace(s)

for (int i=0; i<numberOfChannels; i++)
    if (channels[i] != null) channels[i].setMasked(true);

if (channels[currentChannelIndex] != null)
    channels[currentChannelIndex].setMasked(false);

pack();

repaint(); //force update of label on scope

}//end of UTCalibrator::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::windowOpened
//
// Handles actions necessary when the window is opened.  This only gets called
// when the window is first created - closing and re-opening it afterwards will
// not invoke this function.
//

@Override
public void windowOpened(WindowEvent e)

{

}//end of UTCalibrator::windowOpened
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::windowActivated
//
// Handles actions necessary when the window is activated.
//

@Override
public void windowActivated(WindowEvent e)

{

//mask all channels except the selected one when the window is active so only
//the data from the selected channel is applied to the trace(s)
    
for (int i=0; i<numberOfChannels; i++)
    if (channels[i] != null) channels[i].setMasked(true);

if (channels[currentChannelIndex] != null)
    channels[currentChannelIndex].setMasked(false);

//enable Ascan fast buffer processing in the DSP
//this will result in missed data sets and missed peaks due to the extensive
//processing required, so should only be used for setup, not inspection
channels[currentChannelIndex].setAScanFastEnabled(true, false);

}//end of UTCalibrator::windowActivated
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::windowDeactivated
//
// Handles actions necessary when the window is deactivated.
//

@Override
public void windowDeactivated(WindowEvent e)
{

//unmask all channels when the window is not active so all their data is
//applied to the trace(s)

for (int i=0; i<numberOfChannels; i++)
    if (channels[i] != null) channels[i].setMasked(false);

//disable Ascan processing in the DSP
// This processing takes too much time and some samples will be missed when
// it is running.  This is okay for setting up in a static situation, but
// the AScan must be disabled during inspection.  The OScope screen will thus
// be frozen during inspection.
channels[currentChannelIndex].setAScanFastEnabled(false, false);

}//end of UTCalibrator::windowDeactivated
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::(various window listener functions)
//
// These functions are implemented per requirements of interface WindowListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void windowClosing(WindowEvent e){}
@Override
public void windowClosed(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}

//end of UTCalibrator::(various window listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mousePressed
//
// Responds when the mouse button is pressed while over a component which is
// listening to the mouse.
//

@Override
public void mousePressed(MouseEvent e)
{

if (e.getComponent().getName().equals("View IP")) viewIP();

if (e.getComponent().getName().equals("Oscope Canvas")) 
    utControls.mousePressedOnScope(e);

}//end of UTCalibrator::mousePressed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mouseRelease
//
// Responds when the mouse button is release while over a component which is
// listening to the mouse.
//

@Override
public void mouseReleased(MouseEvent e)
{

if (e.getComponent().getName().equals("View IP")) unViewIP();

if (e.getComponent().getName().equals("Oscope Canvas"))
    utControls.mouseReleasedOnScope(e);

}//end of UTCalibrator::mouseReleased
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::(various mouse listener functions)
//
// These functions are implemented per requirements of interface MouseListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void mouseEntered(MouseEvent e){}

@Override
public void mouseExited(MouseEvent e){}

@Override
public void mouseClicked(MouseEvent e){}

//end of UTCalibrator::(various mouse listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mouseMoved
//
// Responds when the mouse is moved.
//

@Override
public void mouseMoved(MouseEvent e)
{


}//end of UTCalibrator::mouseMoved
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mouseDragged
//
// Responds when the mouse is dragged.
//

@Override
public void mouseDragged(MouseEvent e)
{

utControls.mouseDraggedOnScope(e);

}//end of UTCalibrator::mouseDragged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::componentResized
//
// Handles actions necessary when the window is resized.
//

@Override
public void componentResized(ComponentEvent e)
{

pack();

}//end of UTCalibrator::componentResized
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::(various component listener functions)
//
// These functions are implemented per requirements of interface
// ComponentListener but do nothing at the present time.  As code is added to
// each function, it should be moved from this section and formatted properly.
//

@Override
public void componentHidden(ComponentEvent e){}
@Override
public void componentShown(ComponentEvent e){}
@Override
public void componentMoved(ComponentEvent e){}

//end of UTCalibrator::(various component listener functions)
//-----------------------------------------------------------------------------

}//end of class UTCalibrator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
