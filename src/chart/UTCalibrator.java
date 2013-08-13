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

import chart.mksystems.hardware.Channel;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.hardware.UTBoard;
import chart.mksystems.hardware.UTGate;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.StripChart;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTCalibrator
//
// This class displays a text area in a window.
//

public class UTCalibrator extends JDialog implements ActionListener,
         WindowListener, MouseListener, MouseMotionListener, ComponentListener{

    JFrame frame;
    public Oscilloscope scope1;
    JPanel channelSelector, copyPanel;
    JButton minMax, viewIP;
    JToggleButton persist;
    Settings settings;

    CopyItemSelector copyItemSelector;
    public int currentChannelIndex=0;
    int numberOfChannels; //number of channels in the current group (chart)
    int numberOfChannelsInSystem; //number of all channels in the system
    StripChart chart;
    Hardware hardware;
    public Channel[] channels; //channels in the current group (chart)
    public Channel[] allChannels; //all channels in the system
    UTGate gate;
    UTControls utControls;
    JButton copyButton, copyToAllButton;
    JButton allOnButton, allOffButton;
    Component rigidArea, rigidAreaDynamic;

    double previousDelay;

    ButtonGroup channelSelectorGroup;

    Font blackFont, redFont;

//-----------------------------------------------------------------------------
// UTCalibrator::UTCalibrator (constructor)
//
//

public UTCalibrator(JFrame pFrame, Hardware pHardware, Settings pSettings)
{

    super(pFrame, "Calibration");

    frame = pFrame; hardware = pHardware; settings = pSettings;

}//end of UTCalibrator::UTCalibrator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::init
//

public void init()
{

    addWindowListener(this);
    addComponentListener(this);

    //create red and black fonts for use with display objects
    HashMap<TextAttribute, Object> map =
                new HashMap<TextAttribute, Object>();
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
    scope1 = new Oscilloscope("Scope", hardware.getUSPerDataPoint(), this, this,
                                                                      settings);
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

    persist = new JToggleButton("Persist");
    persist.setAlignmentX(Component.CENTER_ALIGNMENT);
    persist.addActionListener(this);
    persist.setActionCommand("Persist");
    persist.setToolTipText("Turn persistence mode on/off for the scope.");
    alarms.add(persist);

    alarms.add(Box.createRigidArea(new Dimension(0,3))); //vertical spacer

    viewIP = new JButton("View IP");
    viewIP.setAlignmentX(Component.CENTER_ALIGNMENT);
    viewIP.addMouseListener(this);
    viewIP.setName("View IP"); //used by the mouse listener functions
    viewIP.setToolTipText(
                        "Temporarily set delay to zero to view initial pulse.");
    alarms.add(viewIP);

    scopeAndAlarms.add(alarms);
    panel.add(scopeAndAlarms);

    //create the window used to display items selected for copying
    copyItemSelector = new CopyItemSelector(frame);
    copyItemSelector.init();

    utControls = new UTControls(frame, scope1.getOscopeCanvas(), hardware,
                                                       copyItemSelector, this);
    utControls.init();
    utControls.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(utControls);

    contentPane.add(panel);

    setLocation(settings.utCalWindowLocationX, settings.utCalWindowLocationY);

    pack();

}//end of UTCalibrator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::setCopyItemsWindowLocation
//
// This method sets the screen position of the window.  This must be done
// each time it is displayed because it is best placed to the right of the
// calibrator window which changes size depending on its contents.
//

public void setCopyItemsWindowLocation()
{

    //set the position of the "Copy Items" window so that it is to the right of
    //the calibrator window

    copyItemSelector.setLocation(getWidth() + (int)getLocation().getX(),
                                                (int)getLocation().getY());

}//end of UTCalibrator::setCopyItemsWindowLocation
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

        for (int i=0; i < ch.getNumberOfDACGates(); i++) {
            if (ch.dacGates[i].getActive()) {
                ch.dacGates[i].adjustPositionsNoTracking();
            }
        }

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

        for (int i = 1; i < ch.getNumberOfGates(); i++) {
            ch.gates[i].adjustPositionsWithTracking(offset);
        }

        //interface gate does not track the interface
        ch.gates[0].adjustPositionsNoTracking();

        //translate the positions for the DAC gates
        for (int i=0; i < ch.getNumberOfDACGates(); i++) {
            if (ch.dacGates[i].getActive()) {
                ch.dacGates[i].adjustPositionsWithTracking(offset);
            }
        }

    }// else of if (!ch.getInterfaceTracking())

    if (pUTAscanData != null && scope1 != null) {
        scope1.displayData(pRange, ch.gates[0].interfaceCrossingPixAdjusted,
                                                                 pUTAscanData);
    }

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
    //the first time the value will be 0, the first channel

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
    if (channelSelectorGroup != null) {
        channelSelector.removeAll();
    }

    //setting the pointer to a new object will release the old one if this
    //function has been called before
    channelSelectorGroup = new ButtonGroup();

    JPanel allSelectors, panel;
    JRadioButton rb;

    //put all channel selectors in a parent panel so they can be aligned
    allSelectors = new JPanel();
    allSelectors.setLayout(new BoxLayout(allSelectors, BoxLayout.Y_AXIS));
    allSelectors.setAlignmentX(Component.LEFT_ALIGNMENT);
    channelSelector.add(allSelectors); //add the sub-panel to the parent




    for (int i=0; i < numberOfChannels; i++){

        //create a panel to hold each radio button with its accompanying copy
        //button
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rb = new JRadioButton(channels[i].shortTitle);

        channels[i].calRadioButton = rb; //store a reference to the radio button

        //if the channel is off, display in red and change tool tip
        setChannelSelectorColor(channels[i]);

        rb.addActionListener(this);

        //use index as the action string
        rb.setActionCommand(Integer.toString(i));

        // select the currently active channel
        if (i == currentChannelIndex) {rb.setSelected(true);}

        channelSelectorGroup.add(rb); //group the radio buttons

        panel.add(rb); //add the radio button to the sub-panel

        //add the copy button to the sub-panel so it displays to the right
        JButton b = new JButton("<");
        //set the buttons name to the index number of its owner channel so when
        //the button is clicked the associated channel can be discerned
        b.setName(Integer.toString(i));
        //get rid of the blank space around the text so button can be smaller
        b.setMargin(new Insets(0, 0, 0, 0));
        setSizes(b, 15, 18); //make button small
        b.addActionListener(this);
        b.setActionCommand("Copy to This Channel");
        b.setToolTipText("Copy settings to this channel.");
        b.setVisible(false);
        channels[i].copyButton = b; //store a reference to the copy button
        panel.add(b);

        allSelectors.add(panel); //add the sub-panel to the parent

        }

    //vertical spacer
    channelSelector.add(Box.createRigidArea(new Dimension(0,15)));

    //create a panel to hold the "Copy", "Cancel", and other buttons so that
    //they can be centered
    copyPanel = new JPanel();
    copyPanel.setLayout(new BoxLayout(copyPanel, BoxLayout.Y_AXIS));
    copyPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    //add a button for turning all channels on
    allOnButton = new JButton("All On");
    allOnButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    allOnButton.addActionListener(this);
    allOnButton.setActionCommand("All On");
    allOnButton.setToolTipText("Turn all channels on.");
    copyPanel.add(allOnButton);

    copyPanel.add(Box.createRigidArea(new Dimension(0,4)));

    //add a button for turning all channels off
    allOffButton = new JButton("All Off");
    allOffButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    allOffButton.addActionListener(this);
    allOffButton.setActionCommand("All Off");
    allOffButton.setToolTipText("Turn all channels off.");
    copyPanel.add(allOffButton);

    copyPanel.add(Box.createRigidArea(new Dimension(0,4)));

    //add a button for copying the selected channel settings to other channel(s)
    copyButton = new JButton("Copy");
    copyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    copyButton.addActionListener(this);
    copyButton.setActionCommand("Copy");
    copyButton.setToolTipText(
                    "Copy settings for selected channel to other channel(s).");
    copyPanel.add(copyButton);

    //create a vertical spacer between the "Exit Copy Mode" and "Copy to All"
    //buttons to help prevent accidental clicking of the latter -- make the
    //spacer invisible (it's not really visible anyway) so that it doesn't
    //create a space until the "Copy" button is clicked
    rigidAreaDynamic = Box.createRigidArea(new Dimension(0,60));
    rigidAreaDynamic.setVisible(false);
    copyPanel.add(rigidAreaDynamic);

    //add a button for copying the selected channel settings to all channels
    //this button is hidden until the "Copy" button is clicked
    copyToAllButton = new JButton("Copy to All");
    copyToAllButton.setAlignmentX(Component.CENTER_ALIGNMENT);
    copyToAllButton.setVisible(false);
    copyToAllButton.addActionListener(this);
    copyToAllButton.setActionCommand("Copy to All");
    copyToAllButton.setToolTipText(
                  "Copy settings for selected channel to all other channels.");
    copyPanel.add(copyToAllButton);

    //set the panel's max size to unlimited so it will fill the width of its
    //parent panel, setting max height to it's preferred height keeps it from
    //growing vertically -- no way to only set max width only
    copyPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
            copyPanel.getPreferredSize().height));

    channelSelector.add(copyPanel);

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
    ((JRadioButton)pChannel.calRadioButton).setToolTipText(pChannel.detail);
    }

}//end of UTCalibrator::setChannelSelectorColor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::setCopyToChannelButtonsVisibity
//
// Sets the visibility of all the "Copy to This Channel" buttons (which
// actually have "<" as the text).
//
// The button for the currently selected channel is also set visible because
// the user can change the selection at any time when copy mode is active.
//

public void setCopyToChannelButtonsVisibity(boolean pVisible)
{

for (int ch = 0; ch < numberOfChannels; ch++){

    ((JButton)channels[ch].copyButton).setVisible(pVisible);

}//for (int ch = 0; ch < numberOfChannels; ch++)

}//end of UTCalibrator::setCopyToChannelButtonsVisibity
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
// UTCalibrator::copyChannel
//
// Copies the info from channel in pSource to the channel in pDestination.
//
// If pCopyAll is true, all settings are copied.  If false, only the values
// from the controls whose Name variable is included in pCopyList will be
// copied.
//
// If the "Copy All Parameters" item is checked in the CopyItemSelector
// window, pCopyAll will be forced true.
//
// WARNING:  The source and destination channels must have the same number and
// types of gates.
//

public void copyChannel(Channel pSource, Channel pDestination,
                                                            boolean pCopyAll)
{

//if the user has selected to copy everything, then force pCopyAll to true
if(copyItemSelector.getItemState("Copy All Parameters")) {
        pCopyAll = true;
    }

int numberOfGates, numberOfDACGates;

//set the pForceUpdate flags false in each call to copy values so that the
//values are only sent to the DSPs if they are altered

//get the number of gates for the channel - note that all channels in the
//destination group should have the same number of channels for this to work
//properly

numberOfGates = pSource.getNumberOfGates();
numberOfDACGates = pSource.getNumberOfDACGates();

// copy the regular gates
// each call to set a value will set the trigger flag for the sending thread
// to send the value next time it runs -- if it runs between setting of two
// values tied to the same flag, a duplicate call might be made but causes no
// harm
// all setting / sending functions are synchronized so the value setting and
// flag setting/clearing by different threads is protected against collision

// since each part of a gate can be selected for copying, each part is
// copied seperately here

UTGate sGate;

//copy the gate info for all gates of the channels
for (int g = 0; g < numberOfGates; g++){

    sGate = pSource.getGate(g);

    if(pCopyAll || itemCopySelected(sGate.gateStartAdjuster)){
        pDestination.setGateStart(g, pSource.getGateStart(g), false);
        pDestination.setGateStartTrackingOn(
                                        g, pSource.getGateStartTrackingOn(g));
        pDestination.setGateStartTrackingOff(
                                        g, pSource.getGateStartTrackingOff(g));
    }

    if(pCopyAll || itemCopySelected(sGate.gateWidthAdjuster)) {
        pDestination.setGateWidth(g, pSource.getGateWidth(g), false);
    }
    if(pCopyAll || itemCopySelected(sGate.gateLevelAdjuster)) {
        pDestination.setGateLevel(g, pSource.getGateLevel(g), false);
    }
    if(pCopyAll || itemCopySelected(sGate.gateHitCountAdjuster)) {
        pDestination.setGateHitCount(g, pSource.getGateHitCount(g), false);
    }
    if(pCopyAll || itemCopySelected(sGate.gateMissCountAdjuster)) {
        pDestination.setGateMissCount(g, pSource.getGateMissCount(g), false);
    }
    }

//copy the DAC gate info

//since the entire DAC gate group is copied without the user being able to
//select individual parts, the copyGate function can be used here unlike the
//copy section used above for the regular gates

if(pCopyAll || copyItemSelector.getItemState("DAC")){
    for (int dg = 0; dg < numberOfDACGates; dg++) {
        pDestination.copyGate(dg, pSource.dacGates[dg]);
    }
    pDestination.setDACEnabled(pSource.getDACEnabled(), false);
    }

//copy the non-gate info for the channels

//always set range after setting gate position or width, delay and interface
//tracking as these affect the range

//wip mks -- need to convert into synced functions

if(pCopyAll || copyItemSelector.getItemState("Gain")) {
        pDestination.setSoftwareGain(pSource.getSoftwareGain(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("Delay")) {
        pDestination.setDelay(pSource.getDelay(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("Range")) {
        pDestination.setRange(pSource.getRange(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("Interface Tracking")) {
        pDestination.setInterfaceTracking(pSource.getInterfaceTracking(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("Signal Mode / Off")) {
        pDestination.setMode(pSource.getMode(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("Hardware Gain")) {
        pDestination.setHardwareGain(pSource.getHardwareGain1(),
                                            pSource.getHardwareGain2(), false);
    }

if(pCopyAll || copyItemSelector.getItemState("Reject Level")) {
        pDestination.setRejectLevel(pSource.getRejectLevel(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("AScan Smoothing")) {
        pDestination.setAScanSmoothing(pSource.getAScanSmoothing(), false);
    }
if(pCopyAll || copyItemSelector.getItemState("DC Offset")) {
        pDestination.setDCOffset(pSource.getDCOffset(), false);
    }

//updates the channel number color to match the channel's on/off state
//if all system channels are being copied, not all of those channels will
//be in the group currently being displayed by the Calibrator window, those
//not in the group will not have a radio button to set so skip them
if (pDestination.calRadioButton != null) {
        setChannelSelectorColor(pDestination);
    }

}//end of UTCalibrator::copyChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::itemCopySelected
//
// Returns true if the item has been selected for copying, false if not.
// An item has been selected for copying if it has been added to the
// CopyItemSelector window.
//
// Also returns false if pObject is null.
//

boolean itemCopySelected(Object pObject)
{

if ((pObject != null) &&
        copyItemSelector.getItemState(((Component)pObject).getName())) {
        return(true);
    }
else {
        return(false);
    }

}//end of UTCalibrator::itemCopySelected
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

    //scan through all channels and copy info from currently selected channel
    //to all other channels

    for (int ch = 0; ch < pNumDestChannels; ch++){

        if (pDestChannels[ch].isEnabled()){
            copyChannel(pSource, pDestChannels[ch], false);
        }

    }// for (int ch = 0; ch < pNumDestChannels; ch++)

}//end of UTCalibrator::copyToAllHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::turnAllChannelsOn
//
// Turns all channels on if pOn is true.
// Turns all channels off if pOn is false.
//
// The current mode is stored so that if the channels are turned off and back
// on a again later, they return to their original settings.
//

public void turnAllChannelsOn(boolean pOn)
{

//scan through all channels and copy info from currently selected channel
//to all other channels

for (int ch = 0; ch < numberOfChannels; ch++){

    if(pOn){
        channels[ch].setMode(channels[ch].previousMode, false);
    }

    if(!pOn){
        //store current mode, but only if it is not the "Off" mode or this will
        //set the previous mode to "Off" also if the "All Off" button is hit
        //twice and the real previous mode will be lost
        if (channels[ch].getMode() != UTBoard.CHANNEL_OFF) {
            channels[ch].previousMode = channels[ch].getMode();
        }
        //turn the channel off
        channels[ch].setMode(UTBoard.CHANNEL_OFF, false);
    }

    //updates the channel number color to match the channel's on/off state
    //if all system channels are being copied, not all of those channels will
    //be in the group currently being displayed by the Calibrator window, those
    //not in the group will not have a radio button to set so skip them
    if (channels[ch].calRadioButton != null) {
        setChannelSelectorColor(channels[ch]);
    }

}// for (int ch = 0; ch < pNumDestChannels; ch++)

//force the radio button to display the newly changed setting
utControls.updateSignalModeControl();

}//end of UTCalibrator::turnAllChannelsOn
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
if (currentCh == null) {return;}

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
if (currentCh == null) {return;}

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

//trap "All On" button
if (e.getActionCommand().equals("All On")){ turnAllChannelsOn(true); return;}

//trap "All Off" button
if (e.getActionCommand().equals("All Off")){ turnAllChannelsOn(false); return;}

//trap "Copy" button
if (e.getActionCommand().equals("Copy")){

    //hide the all on/off buttons to reduce clutter during copy mode
    allOnButton.setVisible(false);
    allOffButton.setVisible(false);

    //display "<" buttons next to each channel so selected channel can be
    //copied to any channel for which the button is clicked
    setCopyToChannelButtonsVisibity(true);
    //display the vertical spacer between the "Copy Cancel" and "Copy to All"
    rigidAreaDynamic.setVisible(true);
    //display the "Copy to All" button so it can be clicked
    copyToAllButton.setVisible(true);
    //"Copy" button becomes "Exit Copy Mode" button
    copyButton.setText("Exit Copy Mode");
    copyButton.setActionCommand("Exit Copy Mode");
    copyButton.setToolTipText("Exit the copy mode.");
    //set new max height to account for the now visible button and spacer
    copyPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
        copyPanel.getPreferredSize().height));
    pack(); //resize the window
    //adjust the size of the "Copy Items" window
    setCopyItemsWindowLocation();
    //call with blank string so window is opened and "Copy All Parameters"
    //option is displayed
    copyItemSelector.addItem("");
    return;
    }

//trap "Exit Copy Mode" button
if (e.getActionCommand().equals("Exit Copy Mode")){

    //unhide the all on/off buttons
    allOnButton.setVisible(true);
    allOffButton.setVisible(true);

    setCopyToChannelButtonsVisibity(false);
    //hide the vertical spacer between the "Copy Cancel" and "Copy to All"
    rigidAreaDynamic.setVisible(false);
    copyToAllButton.setVisible(false);
    //"Cancel" button becomes "Copy" button
    copyButton.setText("Copy");
    copyButton.setActionCommand("Copy");
    copyButton.setToolTipText(
                    "Copy settings for selected channel to other channel(s).");
    //set new max height to account for the now hidden button and spacer
    copyPanel.setMaximumSize(new Dimension(Short.MAX_VALUE,
        copyPanel.getPreferredSize().height));

    //remove all items selected for copying and hide the window
    copyItemSelector.removeAll();
    copyItemSelector.setVisible(false);

    pack(); //resize the window

    return;
    }

//trap "Copy to This Channel" buttons
if (e.getActionCommand().equals("Copy to This Channel")){

    //each button has the index number of the channel to which it is associated
    //stored in its name -- copy from the currently selected (by radio button)
    //channel to the channel for which the "<" button was clicked
    copyChannel(channels[currentChannelIndex],
        channels[Integer.valueOf(((Component)e.getSource()).getName())], false);

    return;
    }

//trap "Copy to All" button
if (e.getActionCommand().equals("Copy to All")){

    //set the items to copy window visible in case user has closed it
    copyItemSelector.setVisible(true);

    int n = JOptionPane.showConfirmDialog(this,
    "Are you sure you want to copy to all channels?",
    "Confirm",
    JOptionPane.YES_NO_OPTION);
    if (n != JOptionPane.YES_OPTION) {return;}  //bail out if user cancels

    if (settings.copyToAllMode == 0) {
        copyToAllChannelsForCurrentChart();
    }
    else
    if (settings.copyToAllMode == 1) {
        copyToAllChannelsForAllCharts();
    }

    return;
    }

//trap "Persist" toggle button and apply setting to the scope display
if (e.getActionCommand().equals("Persist")){
    scope1.setPersistMode(persist.isSelected());
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
    //adjust the size of the "Copy Items" window
    setCopyItemsWindowLocation();
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

for (int i=0; i<numberOfChannels; i++) {
    if (channels[i] != null) {channels[i].setMasked(true);}
}

if (channels[currentChannelIndex] != null) {
        channels[currentChannelIndex].setMasked(false);
}

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

for (int i=0; i<numberOfChannels; i++) {
        if (channels[i] != null) {
            channels[i].setMasked(true);
        }
    }

if (channels[currentChannelIndex] != null) {
        channels[currentChannelIndex].setMasked(false);
    }

//enable Ascan fast buffer processing in the DSP
//this will result in missed data sets and missed peaks due to the extensive
//processing required, so should only be used for setup, not inspection

if (channels[currentChannelIndex] != null){
    channels[currentChannelIndex].setAScanFastEnabled(true, false);
}

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

for (int i=0; i<numberOfChannels; i++) {
        if (channels[i] != null) {
            channels[i].setMasked(false);
        }
    }

//disable Ascan processing in the DSP
// This processing takes too much time and some samples will be missed when
// it is running.  This is okay for setting up in a static situation, but
// the AScan must be disabled during inspection.  The OScope screen will thus
// be frozen during inspection.

if (channels[currentChannelIndex] != null){
    channels[currentChannelIndex].setAScanFastEnabled(false, false);
}

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

if (e.getComponent().getName().equals("View IP")) {viewIP();}

if (e.getComponent().getName().equals("Oscope Canvas")) {
        utControls.mousePressedOnScope(e);
    }

}//end of UTCalibrator::mousePressed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mouseReleased
//
// Responds when the mouse button is release while over a component which is
// listening to the mouse.
//

@Override
public void mouseReleased(MouseEvent e)
{

if (e.getComponent().getName().equals("View IP")) {unViewIP();}

if (e.getComponent().getName().equals("Oscope Canvas")) {
        utControls.mouseReleasedOnScope(e);
    }

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
// UTCalibrator::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

public void setSizes(Component pComponent, int pWidth, int pHeight)
{

pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of UTCalibrator::setSizes
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
// UTCalibrator::componentHidden
//
// Handles actions necessary when the window is hidden.
//

@Override
public void componentHidden(ComponentEvent e)
{

//close the copy item selector window in case is was open
if (copyItemSelector != null) {copyItemSelector.setVisible(false);}

}//end of UTCalibrator::componentHidden
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::(various component listener functions)
//
// These functions are implemented per requirements of interface
// ComponentListener but do nothing at the present time.  As code is added to
// each function, it should be moved from this section and formatted properly.
//

@Override
public void componentShown(ComponentEvent e){}
@Override
public void componentMoved(ComponentEvent e){}

//end of UTCalibrator::(various component listener functions)
//-----------------------------------------------------------------------------

}//end of class UTCalibrator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
