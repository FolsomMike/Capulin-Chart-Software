/******************************************************************************
* Title: UTControls.java
* Author: Mike Schoonover
* Date: 4/28/03
*
* Purpose:
*
* This class displays and handles a tabbed pane with controls related to
* ultrasonic equipment.
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
import chart.mksystems.hardware.DACGate;
import chart.mksystems.hardware.Gate;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.hardware.UTBoard;
import chart.mksystems.mswing.MFloatSpinner;
import chart.mksystems.stripchart.StripChart;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SpinnerPanel
//
// Creates a JPanel with a description label, Spinner, and units label.
// The tooltip text for each label is set to the text passed for the label.
//
//

class SpinnerPanel extends JPanel
{

    public JLabel descriptionLabel;
    public MFloatSpinner spinner;
    public JLabel unitsLabel;

//-----------------------------------------------------------------------------
// SpinnerPanel::SpinnerPanel (constructor for doubles)
//
// Creates a MFloatSpinner for displaying float values.
//
// pValue is the initial value to be displayed
// pMin is the minimum allowable value
// pMax is the maximum allowable value
// pIncrement is the amount the value is adjusted by use of the up/down arrows
// pFormatPattern is a format string specifiying how the float value is to be
//  displayed - number of digits before and after the decimal point, forced
//  forced zeroes, etc.  See help from Sun for class DecimalFormat.
//  Example formats: "#,##0.00" and "0.0000"
//
// pWidth is the user specified width for the edit box, set to -1 to use the
// default.
// pHeight is the user specified height for the edit box, set to -1 to use the
// default.
//
// See header notes at the top of the page for more info.
//

public SpinnerPanel(double pValue, double pMin,
                     double pMax, double pIncrement, String pFormatPattern,
                   int pWidth, int pHeight, String pDescription, String pUnits,
                   String pName, MouseListener pMouseListener)
{

    setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

    add(descriptionLabel = new JLabel(pDescription));
    descriptionLabel.setToolTipText(pDescription);

    add (spinner = new MFloatSpinner(
             pValue, pMin, pMax, pIncrement, pFormatPattern, pWidth, pHeight));

    spinner.setName(pName);

    //watch for right mouse clicks
    setSpinnerNameAndMouseListener(spinner, spinner.getName(), pMouseListener);

    add(unitsLabel = new JLabel(pUnits));
    unitsLabel.setToolTipText(pUnits);

}//end of SpinnerPanel::SpinnerPanel (constructor for doubles)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SpinnerPanel::setSpinnerNameAndMouseListener
//
// A mouse listener cannot be added directly to a JSpinner or its sub-classes.
// The listener must be added to the text field inside the spinner to work
// properly.  This function also sets the name of the text field so that the
// mouse listener response method can use it.
//

private void setSpinnerNameAndMouseListener(JSpinner pSpinner, String pName,
                                                   MouseListener pMouseListener)
{

    for (Component child : pSpinner.getComponents()) {
        if (child instanceof JSpinner.NumberEditor) {
            for (Component child2 :
                  ((javax.swing.JSpinner.NumberEditor) child).getComponents()){
                if(pMouseListener != null) {
                          ((javax.swing.JFormattedTextField) child2).
                                               addMouseListener(pMouseListener);
                }
                ((javax.swing.JFormattedTextField) child2).setName(pName);
            }
        }
    }

}//end of SpinnerPanel::setSpinnerNameAndMouseListener
//-----------------------------------------------------------------------------

}//end of class SpinnerPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTControls
//
// This class creates and handles controls related to ultrasonic equipment.
//

public class UTControls extends JTabbedPane
         implements ItemListener, ActionListener, ChangeListener, MouseListener
{

    JFrame frame;
    JPanel oscopeCanvas;
    String language;
    CopyItemSelector copyItemSelector;
    UTCalibrator utCalibrator;

    Hardware hardware;
    Channel currentChannel;
    StripChart chart;
    public boolean updateEnabled = true;


    String inchesPeruSText = "in/uS";
    String cmPeruSText = "cm/uS";

    JCheckBox interfaceTrackingCheckBox;

    JPanel gatesTab, signalTab, wallTab, dacTab, chartTab, processTab;
    JPanel configTab, transducerTab;

    Font blackFont, redFont;

    int gridYCount;

    int pointedDACGate = -1, draggingDACGate = -1;

    //components on Modes tab

    TitledBorder inspectionModeBorder, whichEndBorder;
    JRadioButton flawRadioButton, wallRadioButton;
    JRadioButton boxRadioButton, pinRadioButton;

    //end of components on Modes tab

    //components on Gates tab

    JLabel gateLabel, startLabel, widthLabel, levelLabel;

    //end of components on Gates tab

    //components on Signal tab

    SpinnerPanel delaySpin, rangeSpin, gainSpin, repRateSpin;
    SpinnerPanel hardwareGainSpin1, hardwareGainSpin2, rejectSpin;
    SpinnerPanel smoothingSpin, dcOffsetSpin;
    SpinnerPanel nomWallSpin, nomWallPosSpin, wallScaleSpin, velocitySpin;
    SpinnerPanel numMultiplesSpin;

    TitledBorder unitsBorder;

    TitledBorder rectificationBorder;
    JRadioButton posHalfRadioButton, negHalfRadioButton, fullWaveRadioButton;
    JRadioButton rfRadioButton, offRadioButton;
    //the timeDistMult is used to convert from uS to Distance
    //the time values are always stored as uS - the multiplier is set to 1
    //if set to display in distance, the multiplier is changed to
    //Velocity Shear Wave  (the velocity is entered by the user)
    public double timeDistMult;
    double timeDistMax;
    int displayMode = 0;
    //number of decimal places to display for values which can be displayed as
    //either time or distance - this is set by a string such as "##0.0"
    String timeDistDecimalPlaces;
    double timeDistIncrement; //amount to increment when user clicks arrows

    //end of components on Signal tab

    //components on DAC tab

    JCheckBox dacLocked, dacEnabled;
    JButton deleteDACGate, deleteAllDACGates;

    //end of components on DAC tab

    //components on Chart tab

    JTextField chartTitleTextField;
    JTextField shortTitleTextField;
    JCheckBox hideChartCheckBox;

    //end of components on Chart tab

    //components on Configuration tab

    SpinnerPanel velocityShearSpin;
    JRadioButton inchesRadioButton, mmRadioButton;
    JRadioButton timeRadioButton, distanceRadioButton;
    JRadioButton markerPulseButton, markerContinuousButton;
    JButton resetChannelButton;

    //end of components on Configuration tab

//-----------------------------------------------------------------------------
// UTControls::UTControls (constructor)
//
//
// NOTE: Variable currentChannel is not set here - setChannel must be called to
// finish the setup.
//

public UTControls(JFrame pFrame, JPanel pOscopeCanvas, Hardware pHardware,
                CopyItemSelector pCopyItemSelector, UTCalibrator pUTCalibrator)
{

    frame = pFrame;
    oscopeCanvas = pOscopeCanvas;
    hardware = pHardware;
    copyItemSelector = pCopyItemSelector;
    utCalibrator = pUTCalibrator;

}//end of UTControls::UTControls (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::init
//

public void init()
{

    //create red and black fonts for use with display objects
    HashMap<TextAttribute, Object> map =
                new HashMap<TextAttribute, Object>();
    blackFont = new Font("Dialog", Font.PLAIN, 12);
    map.put(TextAttribute.FOREGROUND, Color.RED);
    redFont = blackFont.deriveFont(map);

    //create the panels blank at this time - they will be filled in later by
    //a call to setChannel

    gatesTab = new JPanel();
    addTab("Gates", null, gatesTab, "Gates");

    signalTab = new JPanel();
    addTab("Signal", null, signalTab, "Signal");

    wallTab = new JPanel();
    addTab("Wall", null, wallTab, "Wall");

    dacTab = new JPanel();
    dacTab.setName("DAC");
    addTab("DAC", null, dacTab, "DAC");
    //watch for right mouse clicks
    dacTab.addMouseListener(this);

    chartTab = new JPanel();
    addTab("Chart", null, chartTab, "Chart");

    processTab = new JPanel();
    addTab("Process", null, processTab, "Gate signal processing methods.");

    configTab = new JPanel();
    addTab("Config", null, configTab, "Configuration");


    setSelectedIndex(0);

}//end of UTControls::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setChannel
//
// Sets the channel for which controls are to be displayed.  Must be called
// to finish setting up the tab panels.
//

public void setChannel(StripChart pChart, Channel pChannel)
{

    chart = pChart;
    currentChannel = pChannel;

    //clear all the tabs first so that if a cal window is opened for a chart
    //with no channels assigned the user will only see blank controls
    gatesTab.removeAll(); signalTab.removeAll(); wallTab.removeAll();
    dacTab.removeAll(); chartTab.removeAll(); processTab.removeAll();
    configTab.removeAll();

    //set multiplier to 1.0 if displaying values in time base,
    //otherwise, set multiplier to speed of shear wave for use in converting
    //back and forth between time and distance
    //the decimal places to be used changes depending on whether time or
    //distance is being displayed
    //works the same for Metric or English units
    if (hardware.unitsTimeDistance == Hardware.TIME){
        timeDistMult = 1.0;
        timeDistDecimalPlaces = "##0.0"; //one decimal place for time values
        timeDistIncrement = .1;
        timeDistMax = 112.8;
    }
    else {
        timeDistMult = hardware.hdwVs.velocityShearUS;
        timeDistDecimalPlaces = "#0.000"; //three decimal places for distances
        timeDistIncrement = .01;
        timeDistMax = 15.0;
    }

    //The grid for the gates and the grid for the threshold cannot use set sizes
    //because both need to grow depending on the number of gates or thresholds.
    //If one grows but not the other, the smaller one will be stretched to fit
    //the window and will look bad.  Make both grids have the same row count so
    //they will be the same size.  If the row count is less than four, make it
    //four to keep the rows from stretching.

    // if current channel is null, then only the chart stuff will be displayed
    // so make the grid large enough to hold the thresholds, igoring gates

    if (currentChannel != null) {
        gridYCount = (
              currentChannel.numberOfGates > chart.getNumberOfThresholds())
                 ? currentChannel.numberOfGates : chart.getNumberOfThresholds();
    }
    else {
        gridYCount = chart.getNumberOfThresholds();
    }

    if (gridYCount < 4) {
        gridYCount = 4;
    }

    //setup the chart controls tab before checking if a valid channel is
    //available so the user can still hide or display the chart

    setupChartTab();

    if (currentChannel == null) {
        return;
    }

    setupGatesTab();

    setupSignalTab();

    setupWallTab();

    setupDACTab();

    //chartTab already setup above

    setupProcessTab();

    setupConfigTab();

    calculateAllUTValues(); //recalculate everything for the new channel

}//end of UTControls::setChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupGatesTab
//
// Sets up a panel for the Gates tab - adds all necessary components to the
// panel.
//

void setupGatesTab()
{

    gatesTab.removeAll();

    //all other objects besides the interfaceTrackingCheckBox get replaced
    //if there is no interface gate for the current channel but there was one
    //for the previously active channel, this checkbox will not get
    //replaced/deleted and will cause problems because the new current channel
    //is not set up to deal with the box
    interfaceTrackingCheckBox = null;

    int numberOfGates = currentChannel.getNumberOfGates();

    //create a row for each gate plus one for the header labels
    //gridYCount equals the number of gates or thresholds, whichever is greater
    //so that both will have the same grid sizes and component sizes
    gatesTab.setLayout(new GridLayout(gridYCount+1, 5, 10, 10));

    gateLabel = new JLabel("Gate"); gateLabel.setToolTipText("Gate");
    gatesTab.add(gateLabel);
    startLabel = new JLabel("Start"); startLabel.setToolTipText("Start");
    gatesTab.add(startLabel);
    widthLabel = new JLabel("Width"); widthLabel.setToolTipText("Width");
    gatesTab.add(widthLabel);
    levelLabel = new JLabel("Level"); levelLabel.setToolTipText("Level");
    gatesTab.add(levelLabel);

    gatesTab.add(new JLabel("")); //fill blank grid spot

    JLabel label;
    MFloatSpinner mfSpinner;

    //add controls for each gate of the current channel

    //there may be more rows than gates because the number of rows may be set
    //higher to prevent them from stretching to fill empty space
    //for those extra rows, fill in dummy labels

    //use gridYCount instead of gridYCount+1 because the header already fills
    //a row

    for (int i=0; i < gridYCount; i++){

        //add controls for each gate
        if (i < numberOfGates){

            label = new JLabel(currentChannel.getGate(i).title);
            label.setToolTipText(currentChannel.getGate(i).title);
            gatesTab.add(label);

            mfSpinner = new MFloatSpinner(
               currentChannel.getGateStart(i) * timeDistMult,
               -273.0, 273.0, timeDistIncrement, timeDistDecimalPlaces, 60, -1);
            mfSpinner.setName(label.getText() + " Gate Start");
            mfSpinner.addChangeListener(this); //monitor changes to value
            //watch for right mouse clicks
            setSpinnerNameAndMouseListener(mfSpinner,mfSpinner.getName(), this);
            gatesTab.add(mfSpinner);
            //save a pointer to this adjuster in the gate object
            currentChannel.getGate(i).gateStartAdjuster = mfSpinner;

            mfSpinner = new MFloatSpinner(
                 currentChannel.getGateWidth(i) * timeDistMult,
                 0, 273.0, timeDistIncrement, timeDistDecimalPlaces, 60, -1);
            mfSpinner.setName(label.getText() + " Gate Width");
            mfSpinner.addChangeListener(this); //monitor changes to value
            //watch for right mouse clicks
            setSpinnerNameAndMouseListener(mfSpinner,mfSpinner.getName(), this);
            gatesTab.add(mfSpinner);
            //save a pointer to this adjuster in the gate object
            currentChannel.getGate(i).gateWidthAdjuster = mfSpinner;

            mfSpinner = new MFloatSpinner(currentChannel.getGateLevel(i),
                                                    0, 100.0, 1, "##0", 60, -1);
            mfSpinner.setName(label.getText() + " Gate Level");
            mfSpinner.addChangeListener(this); //monitor changes to value
            //watch for right mouse clicks
            setSpinnerNameAndMouseListener(mfSpinner,mfSpinner.getName(), this);
            gatesTab.add(mfSpinner);
            //save a pointer to this adjuster in the gate object
            currentChannel.getGate(i).gateLevelAdjuster = mfSpinner;

            //if the gate just added is the interface gate, add a "Track"
            //checkbox to allow selecton of interface tracking, otherwise add
            //an AScan trigger box so user can choose one or more gates to
            //trigger an AScan send

            if (currentChannel.getGate(i).getInterfaceGate()){
                addInterfaceTrackingCheckBox();
            }
            else{
                addAScanTriggerCheckBox(i);
            }
        }// if (i < numberOfGates)
        else{
            //fill extra rows with dummy labels
            gatesTab.add(new JLabel(" "));
            gatesTab.add(new JLabel(" "));
            gatesTab.add(new JLabel(" "));
            gatesTab.add(new JLabel(" "));
            gatesTab.add(new JLabel(" "));
        }//else of if (i < numberOfGates)
    }// for (int i=0; i < gridYCount+1; i++)

}//end of UTControls::setupGatesTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::addInterfaceTrackingCheckBox
//
// Sets up and adds the Interface Tracking Checkbox.
//

void addInterfaceTrackingCheckBox()
{

    interfaceTrackingCheckBox = new JCheckBox("Track");
    interfaceTrackingCheckBox.setSelected(
                            currentChannel.getInterfaceTracking());
    interfaceTrackingCheckBox.setActionCommand("Interface Tracking");
    interfaceTrackingCheckBox.addItemListener(this);
    interfaceTrackingCheckBox.setName("Interface Tracking");
    interfaceTrackingCheckBox.addMouseListener(this);
    interfaceTrackingCheckBox.setToolTipText(
    "Gates Will Follow the Interface Signal if Checked");
    gatesTab.add(interfaceTrackingCheckBox);

}//end of UTControls::addInterfaceTrackingCheckBox
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::addAScanTriggerCheckBox
//
// Sets up and adds the AScan Trigger CheckBox.  This box allows the user to
// select one or more gates to trigger the sending of an AScan. The AScan will
// be sent when the signal exceeds the gate to allow those signals to be
// displayed clearly instead of randomly when the AScan sends are not
// synchronized to the signal.
//

void addAScanTriggerCheckBox(int pWhichGate)
{

    JCheckBox box;
    box = new JCheckBox("Trigger");
    //store with channel so it can be accessed later
    currentChannel.getGate(pWhichGate).aScanTriggerCheckBox = box;
    //always starts unselected
    box.setSelected(false);
    box.setActionCommand("AScan Trigger");
    box.addItemListener(this);
    box.setName("AScan Trigger");
    box.setToolTipText("AScan will lock onto signals which exceed this gate.");
    gatesTab.add(box);

}//end of UTControls::addAScanTriggerCheckBox
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::updateGateControls
//
// Updates the values in the controls with data from the gate variables.  Use
// this if those values are changed programatically and need to be reflected
// back into the display object.
//

void updateGateControls(Channel pChannel)
{

    //disable updates so the controls don't respond while they are being updated
    //the values have already been written to the variables and if the controls
    //respond they may wipe out the values already set

    updateEnabled = false;

    for (int i=0; i < pChannel.getNumberOfGates(); i++){

        ((MFloatSpinner) pChannel.getGate(i).gateStartAdjuster).setValue(
                                      pChannel.getGateStart(i) * timeDistMult);

        ((MFloatSpinner) pChannel.getGate(i).gateWidthAdjuster).setValue(
                                      pChannel.getGateWidth(i) * timeDistMult);
        }

    updateEnabled = true;

}//end of UTControls::updateGateControls
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::updateSignalModeControl
//
// Updates the values in the Signal Mode radio button with data from the
// associated variables.  Use this if that value is changed programatically
// and needs to be reflected back into the display object.
//

void updateSignalModeControl()
{

    //disable updates so the controls don't respond while they are being updated
    //the values have already been written to the variables and if the controls
    //respond they may wipe out the values already set

    updateEnabled = false;

    displayMode = currentChannel.getMode();

    if (displayMode == 0) {
        posHalfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 1) {
        negHalfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 2) {
        fullWaveRadioButton.setSelected(true);
    }
    else
    if (displayMode == 3) {
        rfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 4) {
        offRadioButton.setSelected(true);
    }

    updateEnabled = true;

}//end of UTControls::updateSignalModeControl
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupSignalTab
//
// Sets up a panel for the Signal tab - adds all necessary components to the
// panel.
//

void setupSignalTab()
{

    signalTab.removeAll();

    signalTab.setLayout(new BoxLayout(signalTab, BoxLayout.PAGE_AXIS));

    String timeDistLabel;
    if (hardware.unitsTimeDistance == Hardware.TIME) {
        timeDistLabel = " uS";
    }
    else{
        if (hardware.units == Hardware.INCHES) {
            timeDistLabel = " inches";
        }
        else {
            timeDistLabel = " mm";
        }
        }

    //create a panel to hold top row of panels and components
    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));
    panel1.setAlignmentX(Component.LEFT_ALIGNMENT);

    //create a panel to hold the spinner controls
    JPanel multiSpinnerPanel = new JPanel();
    setSizes(multiSpinnerPanel, 280, 60);

    //choose the layout
    multiSpinnerPanel.setLayout(new GridLayout(2,2));

    //add a panel for Delay control
    multiSpinnerPanel.add(delaySpin =
      new SpinnerPanel( currentChannel.getDelay() * timeDistMult, 0, 273.0,
      timeDistIncrement, timeDistDecimalPlaces, 60, 23, "Delay ", timeDistLabel,
        "Delay", this));
    delaySpin.spinner.addChangeListener(this); //monitor changes to value

    //add a panel for Range control

    //check for out-of-range value or exception will be thrown

    double rangeCheck = currentChannel.getRange() * timeDistMult;
    if (rangeCheck < .1) {
        rangeCheck = .1;
    }
    if (rangeCheck > timeDistMax) {
        rangeCheck = timeDistMax;
    }

    multiSpinnerPanel.add(rangeSpin =
      new SpinnerPanel(rangeCheck, .1, timeDistMax,
      timeDistIncrement, timeDistDecimalPlaces, 60, 23, "Range ", timeDistLabel,
        "Range", this));
    rangeSpin.spinner.addChangeListener(this); //monitor changes to value

    //add a panel for Gain control
    multiSpinnerPanel.add(gainSpin =
                new SpinnerPanel(currentChannel.getSoftwareGain(),
                  0, 80, .1, "#0.0", 60, 23, "Gain ", " dB", "Gain", this));
    gainSpin.spinner.addChangeListener(this); //monitor changes to value

    //add a panel for Rep Rate control
    multiSpinnerPanel.add(repRateSpin =
          new SpinnerPanel(hardware.getRepRateInHertz(),
              0, 2000, 1, "#0", 60, 23, "Rep Rate ", " Hz", "Rep Rate", this));
    repRateSpin.spinner.addChangeListener(this); //monitor changes to value

    //Display/Rectification selection panel and radio buttons

    JPanel rectificationPanel = new JPanel();
    setSizes(rectificationPanel, 400, 60);
    rectificationPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    rectificationPanel.setBorder(
           rectificationBorder = BorderFactory.createTitledBorder("Mode"));
    rectificationPanel.setToolTipText("Rectification");

    posHalfRadioButton = new JRadioButton("+ Half");
    posHalfRadioButton.setToolTipText("View the positive half of the signal.");
    posHalfRadioButton.addActionListener(this);
    posHalfRadioButton.setName("Signal Mode / Off");
    posHalfRadioButton.addMouseListener(this);

    negHalfRadioButton = new JRadioButton("- Half");
    negHalfRadioButton.setToolTipText("View the negative half of the signal.");
    negHalfRadioButton.addActionListener(this);
    negHalfRadioButton.setName("Signal Mode / Off");
    negHalfRadioButton.addMouseListener(this);

    fullWaveRadioButton = new JRadioButton("Full Wave");
    fullWaveRadioButton.setToolTipText(
              "View the positive & negative halves of the signal overlapped.");
    fullWaveRadioButton.addActionListener(this);
    fullWaveRadioButton.setName("Signal Mode / Off");
    fullWaveRadioButton.addMouseListener(this);

    rfRadioButton = new JRadioButton("RF");
    rfRadioButton.setToolTipText("View the full signal.");
    rfRadioButton.addActionListener(this);
    rfRadioButton.setName("Signal Mode / Off");
    rfRadioButton.addMouseListener(this);

    offRadioButton = new JRadioButton("Off");
    offRadioButton.setToolTipText("Turn the channel off.");
    offRadioButton.addActionListener(this);
    offRadioButton.setName("Signal Mode / Off");
    offRadioButton.addMouseListener(this);

    ButtonGroup rectificationGroup = new ButtonGroup();
    rectificationGroup.add(posHalfRadioButton);
    rectificationGroup.add(negHalfRadioButton);
    rectificationGroup.add(fullWaveRadioButton);
    rectificationGroup.add(rfRadioButton);
    rectificationGroup.add(offRadioButton);
    rectificationPanel.add(posHalfRadioButton);
    rectificationPanel.add(negHalfRadioButton);
    rectificationPanel.add(fullWaveRadioButton);
    rectificationPanel.add(rfRadioButton);
    rectificationPanel.add(offRadioButton);

    displayMode = currentChannel.getMode();

    if (displayMode == 0) {
        posHalfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 1) {
        negHalfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 2) {
        fullWaveRadioButton.setSelected(true);
    }
    else
    if (displayMode == 3) {
        rfRadioButton.setSelected(true);
    }
    else
    if (displayMode == 4) {
        offRadioButton.setSelected(true);
    }

    //add the panel with the spinners and the panel with the unit selection
    //to the top panel1
    panel1.add(multiSpinnerPanel);

    //add all panels to the tab
    signalTab.add(panel1);
    signalTab.add(rectificationPanel);

}//end of UTControls::setupSignalTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupWallTab
//
// Sets up a panel for the Wall tab - adds all necessary components to the
// panel.
//

void setupWallTab()
{

    wallTab.removeAll();

    wallTab.setLayout(new BoxLayout(wallTab, BoxLayout.PAGE_AXIS));

    String units;

    if (hardware.units == Hardware.INCHES) {
        units = " inches";
    }
    else {
        units = " mm";
    }

    JPanel topLeftPanel = new JPanel();
    topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.PAGE_AXIS));
    setSizes(topLeftPanel, 250, 140);
    topLeftPanel.setAlignmentY(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer

    //add an entry for nominal wall thickness
    topLeftPanel.add(nomWallSpin = new SpinnerPanel(
            hardware.hdwVs.nominalWall, 0, 99.0,
             .001, "#0.000", 60, 23, "Nominal Wall ", units, "Nominal Wall",
                                                                        null));
    nomWallSpin.spinner.addChangeListener(this); //monitor changes to value
    nomWallSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer

    //add an entry for nominal wall position on the chart
    topLeftPanel.add(nomWallPosSpin = new SpinnerPanel(
            hardware.hdwVs.nominalWallChartPosition, 0, 100, 1, "##0", 40, 23,
           "Nominal Wall Position on Chart ", " %", "Nominal Wall Position",
                                                                        null));
    nomWallPosSpin.spinner.addChangeListener(this); //monitor changes to value
    nomWallPosSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer

    //add an entry for Wall chart scale
    topLeftPanel.add(wallScaleSpin = new SpinnerPanel(
         hardware.hdwVs.wallChartScale,
         0, 99.0, .001, "#0.000", 60, 23, "Wall Chart Scale ", units
                                    + " per 1%", "Wall Chart Scale", null));
    wallScaleSpin.spinner.addChangeListener(this); //monitor changes to value
    wallScaleSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer

    //add an entry for velocity of sound in the test piece
    topLeftPanel.add(velocitySpin = new SpinnerPanel(hardware.hdwVs.velocityUS,
                    0, 5, .0001, "0.0000", 60, 23, "Velocity ", units + "/uS",
                                            "Compression Wave Velocity", null));
    velocitySpin.spinner.addChangeListener(this); //monitor changes to value
    velocitySpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer

    //add an entry for number of multiples between wall gates
    topLeftPanel.add(numMultiplesSpin = new SpinnerPanel(
        hardware.hdwVs.numberOfMultiples, 1, 10, 1, "0", 40, 23,
    "Multiples Between Wall Gates ", "", "Multiples Between Wall Gates", null));
    numMultiplesSpin.spinner.addChangeListener(this); //monitor changes to value
    numMultiplesSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    wallTab.add(topLeftPanel);

}//end of UTControls::setupWallTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupDACTab
//
// Sets up a panel for the DAC tab - adds all necessary components to the
// panel.
//

void setupDACTab()
{

    dacTab.removeAll();

    dacTab.setLayout(new BoxLayout(dacTab, BoxLayout.PAGE_AXIS));

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.LINE_AXIS));

    panel1.add(dacEnabled = new JCheckBox("Enable DAC"));
    dacEnabled.setName("Enable DAC");
    dacEnabled.setToolTipText("Check to view and use the DAC gates.");
    dacEnabled.setSelected(currentChannel.getDACEnabled());
    //set the scope's DAC enabled flag to match the control
    ((OscopeCanvas)oscopeCanvas).setDACEnabled(dacEnabled.isSelected());
    dacEnabled.addItemListener(this); //monitor changes to value

    panel1.add(Box.createRigidArea(new Dimension(40,0)));

    panel1.add(dacLocked = new JCheckBox("Lock DAC"));
    dacLocked.setName("Lock DAC");
    dacLocked.setToolTipText(
        "Uncheck this box to use the mouse to adjust DAC gates on the scope.");
    dacLocked.setSelected(true);
    //set the scope's DAC locked flag to match the control
    ((OscopeCanvas)oscopeCanvas).setDACLocked(dacLocked.isSelected());
    dacLocked.addItemListener(this); //monitor changes to value

    panel1.add(Box.createRigidArea(new Dimension(30,0)));

    deleteDACGate = new JButton("Delete");
    deleteDACGate.setEnabled(false);
    deleteDACGate.setToolTipText(
                       "Use this button to delete a highlighted DAC section.");
    deleteDACGate.setActionCommand("Delete DAC Gate");
    deleteDACGate.addActionListener(this);
    panel1.add(deleteDACGate);

    panel1.setAlignmentX(Component.LEFT_ALIGNMENT);
    dacTab.add(panel1);


    panel1 = new JPanel();
    panel1.setBorder(BorderFactory.createTitledBorder("Instructions"));
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));

    JLabel label1;
    panel1.add(label1 = new JLabel(
                    "Right click on the scope above to add a new DAC point."));
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel1.add(label1 = new JLabel(
                       "Left click on a circle to select or move a section."));
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel1.add(label1 = new JLabel(
                    "Use the Delete button to remove a highlighted section."));
    label1.setAlignmentX(Component.LEFT_ALIGNMENT);

    panel1.setAlignmentX(Component.LEFT_ALIGNMENT);
    dacTab.add(panel1);

    deleteAllDACGates = new JButton("Delete All DAC Sections");
    deleteAllDACGates.setToolTipText(
                                "Use this button to delete all DAC sections.");
    deleteAllDACGates.setActionCommand("Delete All DAC Gates");
    deleteAllDACGates.setEnabled(false);
    deleteAllDACGates.addActionListener(this);
    dacTab.add(deleteAllDACGates);

}//end of UTControls::setupDACTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupChartTab
//
// Sets up a panel for the Chart tab - adds all necessary components to the
// panel.
//

void setupChartTab()
{

    chartTab.removeAll();

    chartTab.setLayout(new BoxLayout(chartTab, BoxLayout.PAGE_AXIS));

    JPanel topPanel = new JPanel();
    topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.LINE_AXIS));

    JPanel panel = new JPanel();
    JPanel panel2;
    JButton button;
    JLabel thresholdLabel,  colorLabel, tlevelLabel;

    int numberOfThresholds = chart.getNumberOfThresholds();

    panel.setAlignmentY(Component.TOP_ALIGNMENT);

    //create a row for each gate plus one for the header labels
    //gridYCount equals the number of gates or thresholds, whichever is greater
    //so that both will have the same grid sizes and component sizes
    panel.setLayout(new GridLayout(gridYCount+1, 3, 10, 10));

    thresholdLabel = new JLabel("Threshold");
    thresholdLabel.setToolTipText("Threshold");
    panel.add(thresholdLabel);
    colorLabel = new JLabel("Color");
    colorLabel.setToolTipText("Threshold Color");
    panel.add(colorLabel);
    tlevelLabel = new JLabel("Level");
    tlevelLabel.setToolTipText("Level");
    panel.add(tlevelLabel);

    JLabel label;
    MFloatSpinner mfSpinner;

    //add controls for each threshold of the chart
    //there may be more rows than thresholds because the number of rows may be
    //set higher to prevent them from stretching to fill empty space
    //for those extra rows, fill in dummy labels

    //use gridYCount instead of gridYCount+1 because the header already fills a
    //row

    for (int i=0; i < gridYCount; i++){

        //add controls for each threshold
        if (i < numberOfThresholds){

            label = new JLabel(chart.getThreshold(i).title);
            label.setToolTipText(chart.getThreshold(i).title);
            panel.add(label);

            //add a color swatch to the color column
            panel.add(
               new JLabel(createColorSwatch(chart.getThresholdColor(i)), LEFT));

            mfSpinner = new MFloatSpinner(chart.getThresholdLevel(i),
                                                     0, 100, 1, "##0", 60, -1);
            //give the spinner a name so the stateChanged function can parse
            mfSpinner.setName("Threshold Spinner " + i);
            mfSpinner.addChangeListener(this); //monitor changes to value
            //watch for right mouse clicks
            setSpinnerNameAndMouseListener(mfSpinner,mfSpinner.getName(), null);
            panel.add(mfSpinner);
            //save a pointer to this adjuster in the threshold object
            chart.getThreshold(i).levelAdjuster = mfSpinner;
        }
        else{
            //fill extra rows with dummy labels
            panel.add(new JLabel(" "));
            panel.add(new JLabel(" "));
            panel.add(new JLabel(" "));
        }
    }// for (int i=0; i < gridYCount+1; i++)

    //add the threshold adjustments
    topPanel.add(panel);

    //separate the threshold adjustments from the stuff on the right
    topPanel.add(Box.createRigidArea(new Dimension(15,0)));

    panel = new JPanel();
    setSizes(panel, 200, 125);
    panel.setAlignmentY(Component.TOP_ALIGNMENT);
    panel.setBorder(BorderFactory.createTitledBorder("Display"));
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.add(label = new JLabel("Chart Name (1-15 letters)"));
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(chartTitleTextField = new JTextField(15));
    chartTitleTextField.setText(chart.getTitle());
    panel.add(label = new JLabel("Short Name (1-4 letters)"));
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(shortTitleTextField = new JTextField(15));
    shortTitleTextField.setText(chart.getShortTitle());

    panel2 = new JPanel();
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
    panel2.add(hideChartCheckBox = new JCheckBox("Hide Chart"));
    hideChartCheckBox.setName("Hide Chart");
    hideChartCheckBox.setSelected(!chart.isChartVisible());
    hideChartCheckBox.addItemListener(this); //monitor changes to value
    //add invisible filler to spread checkbox and button
    panel2.add(Box.createHorizontalGlue());
    //add a button to click on to setting the values
    panel2.add(button = new JButton("Set"));
    button.setToolTipText("Set the chart values.");
    button.setActionCommand("Set Chart Values");
    button.addActionListener(this);

    panel.add(panel2);

    //add the chart names & hide checkbox
    topPanel.add(panel);

    chartTab.add(topPanel);

    //add invisible filler below panel to keep it from expanding to fill
    chartTab.add(Box.createVerticalGlue());

}//end of UTControls::setupChartTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupProcessTab
//
// Sets up a panel for the gate signal processing tab - adds all necessary
// components to the panel.
//

void setupProcessTab()
{

    processTab.removeAll();

    JLabel label;
    JComboBox jcb;
    SpinnerPanel sp;
    Gate gate;

    int numberOfGates = currentChannel.getNumberOfGates();

    //layout for the tab
    processTab.setLayout(new BoxLayout(processTab, BoxLayout.LINE_AXIS));

    //gate label column -- create a row for each gate plus one for the header title
    JPanel gateLabelPanel = new JPanel();
    gateLabelPanel.setLayout(new GridLayout(gridYCount+1, 1, 10, 10));
    //add the row title header
    label = new JLabel("Gate"); label.setToolTipText("Gate");
    gateLabelPanel.add(label);

    for (int i=0; i < gridYCount; i++){
        //add controls for each gate
        if (i < numberOfGates){

            gate = currentChannel.getGate(i);

            label = new JLabel(gate.title);
            label.setToolTipText(gate.title);
            gateLabelPanel.add(label);
        }
        else{gateLabelPanel.add(new JLabel(""));}
    }// for (int i=0; i < gridYCount; i++){

    processTab.add(gateLabelPanel);

    //hit & miss adjusters column -- create a row for each gate plus header title
    JPanel hitMissPanel = new JPanel();
    hitMissPanel.setLayout(new GridLayout(gridYCount+1, 1, 10, 10));
    //add the row title headers
    label = new JLabel("Hits"); label.setToolTipText("Hits required to alarm.");
    hitMissPanel.add(label);
    label = new JLabel("Misses"); label.setToolTipText("Misses required to alarm.");
    hitMissPanel.add(label);

    for (int i=0; i < gridYCount; i++){
        //add controls for each gate
        if (i < numberOfGates){

            gate = currentChannel.getGate(i);

            sp = new SpinnerPanel(currentChannel.getGateHitCount(i), 0, 50, 1,
                  "##0", 40, -1, "", "", gate.title + " Gate Hit Count", this);
            sp.spinner.addChangeListener(this); //monitor changes to value
            hitMissPanel.add(sp);
            //save a pointer to this adjuster in the gate object
            gate.gateHitCountAdjuster = sp.spinner;

            sp = new SpinnerPanel(currentChannel.getGateMissCount(i), 0, 50, 1,
                 "##0", 40, -1, "", "", gate.title + " Gate Miss Count", this);
            sp.spinner.addChangeListener(this); //monitor changes to value
            hitMissPanel.add(sp);
            //save a pointer to this adjuster in the gate object
            gate.gateMissCountAdjuster = sp.spinner;

        }
        else{hitMissPanel.add(new JLabel(""));}
    }// for (int i=0; i < gridYCount; i++){

    processTab.add(hitMissPanel);

    //signal process selections column -- create a row for each gate plus header
    JPanel processPanel = new JPanel();
    processPanel.setLayout(new GridLayout(gridYCount+1, 1, 10, 10));
    //add the row title headers
    label = new JLabel("Signal Processing");
    label.setToolTipText("Signal processing method for the gate.");
    processPanel.add(label);

    for (int i=0; i < gridYCount; i++){
        //add controls for each gate
        if (i < numberOfGates){

            gate = currentChannel.getGate(i);

            //get the signal processing list for the gate type
            ArrayList<String> pl = gate.getSigProcList();

            jcb = new JComboBox(pl.toArray());
            jcb.setSelectedIndex(gate.getSigProcIndex());
            jcb.setActionCommand("Signal Processing");
            jcb.addActionListener(this);
            jcb.setName(gate.title + " Gate Signal Processing Type");
            jcb.addMouseListener(this);
            gate.processSelector = jcb;

            processPanel.add(jcb);

        }
        else{processPanel.add(new JLabel(""));}
    }// for (int i=0; i < gridYCount; i++)

    processTab.add(processPanel);

    //horizontal spacer
    processTab.add(Box.createRigidArea(new Dimension(10,0)));

    //signal process threshold column -- create a row for each gate plus header
    JPanel thresholdPanel = new JPanel();
    thresholdPanel.setLayout(new GridLayout(gridYCount+1, 1, 10, 10));
    //add the row title headers
    label = new JLabel("Threshold");
    label.setToolTipText("Threshold to trigger an event.");
    thresholdPanel.add(label);

    for (int i=0; i < gridYCount; i++){
        //add controls for each gate
        if (i < numberOfGates){

            gate = currentChannel.getGate(i);

            sp = new SpinnerPanel(currentChannel.getSigProcThreshold(i), 0,
                 65535, 1, "##0", 60, -1, "", "", gate.title +
                            " Gate Signal Processing Threshold", this);
            sp.spinner.addChangeListener(this); //monitor changes to value
            thresholdPanel.add(sp);
            //save a pointer to this adjuster in the gate object
            gate.thresholdAdjuster = sp.spinner;
        }
        else{thresholdPanel.add(new JLabel(""));}
    }// for (int i=0; i < gridYCount; i++){

    processTab.add(thresholdPanel);

    //add invisible filler push everything to the left
    processTab.add(Box.createHorizontalGlue());

}//end of UTControls::setupShotCountTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setupConfigTab
//
// Sets up a panel for the Config tab - adds all necessary components to
// the panel.
//

void setupConfigTab()
{

    configTab.removeAll();

    configTab.setLayout(new BoxLayout(configTab, BoxLayout.LINE_AXIS));

    String units;

    if (hardware.units == Hardware.INCHES) {
        units = " inches";
    }
    else {
        units = " mm";
    }

    JPanel topLeftPanel = new JPanel();
    topLeftPanel.setLayout(new BoxLayout(topLeftPanel, BoxLayout.PAGE_AXIS));
    setSizes(topLeftPanel, 170, 180);
    topLeftPanel.setAlignmentY(Component.TOP_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for Reject
    topLeftPanel.add(
            rejectSpin = new SpinnerPanel( currentChannel.getRejectLevel(),
            0, 100.0, 1, "##0", 50, 23, "Reject ", " %", "Reject Level", this));
    rejectSpin.spinner.addChangeListener(this); //monitor changes to value
    rejectSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for AScan Display Smoothing
    topLeftPanel.add(smoothingSpin = new SpinnerPanel(
            currentChannel.getAScanSmoothing(), 1, 25, 1, "##0", 40, 23,
                             "AScan Smoothing ", "", "AScan Smoothing", this));
    smoothingSpin.spinner.addChangeListener(this); //monitor changes to value
    smoothingSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for stage 1 hardware gain
    topLeftPanel.add(hardwareGainSpin1 = new SpinnerPanel(
                  currentChannel.getHardwareGain1(), 1, 16.0, 1, "##0", 40, 23,
                         "Hardware Gain 1 ", " Vo/Vi", "Hardware Gain", this));
    hardwareGainSpin1.spinner.addChangeListener(this); //monitor changes
    hardwareGainSpin1.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for stage 1 hardware gain
    topLeftPanel.add(hardwareGainSpin2 = new SpinnerPanel(
                  currentChannel.getHardwareGain2(), 1, 16.0, 1, "##0", 40, 23,
                         "Hardware Gain 2 ", " Vo/Vi", "Hardware Gain", this));
    hardwareGainSpin2.spinner.addChangeListener(this); //monitor changes
    hardwareGainSpin2.setAlignmentX(Component.LEFT_ALIGNMENT);

    topLeftPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for DC offset
    topLeftPanel.add(dcOffsetSpin = new SpinnerPanel(
                    currentChannel.getDCOffset(), -127, 127, 1, "##0", 60, 23,
                                      "DC Offset ", " mV", "DC Offset", this));
    dcOffsetSpin.spinner.addChangeListener(this); //monitor changes to value
    dcOffsetSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    //create a panel for the right column

    JPanel topRightPanel = new JPanel();
    topRightPanel.setLayout(new BoxLayout(topRightPanel, BoxLayout.PAGE_AXIS));
    setSizes(topRightPanel, 250, 180);
    topRightPanel.setAlignmentY(Component.TOP_ALIGNMENT);

    topRightPanel.add(Box.createRigidArea(new Dimension(0,7)));//vertical spacer

    //Units (inches or mm) selection panel and radio buttons

    JPanel inchesMMPanel = new JPanel();
    setSizes(inchesMMPanel, 130, 23);
    inchesMMPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    inchesRadioButton = new JRadioButton("inches");
    inchesRadioButton.setToolTipText("inches");
    inchesRadioButton.addActionListener(this);
    mmRadioButton = new JRadioButton("mm");
    mmRadioButton.setToolTipText("mm");
    mmRadioButton.addActionListener(this);
    ButtonGroup unitsGroup = new ButtonGroup();
    unitsGroup.add(inchesRadioButton);
    unitsGroup.add(mmRadioButton);
    inchesMMPanel.add(inchesRadioButton);
    inchesMMPanel.add(mmRadioButton);
    topRightPanel.add(inchesMMPanel);

    //set the inches/mm button
    if (hardware.units == Hardware.INCHES) {
        inchesRadioButton.setSelected(true);
    }
    else {
        mmRadioButton.setSelected(true);
    }

    //Time/Distance display (time or distance) selection panel and radio buttons

    JPanel timeDistancePanel = new JPanel();
    setSizes(timeDistancePanel, 140, 23);
    timeDistancePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    timeRadioButton = new JRadioButton("time");
    timeRadioButton.setToolTipText("Values displayed in time.");
    timeRadioButton.addActionListener(this);
    distanceRadioButton = new JRadioButton("distance");
    distanceRadioButton.setToolTipText("Values displayed as distance.");
    distanceRadioButton.addActionListener(this);
    ButtonGroup timeDistanceGroup = new ButtonGroup();
    timeDistanceGroup.add(timeRadioButton);
    timeDistanceGroup.add(distanceRadioButton);
    timeDistancePanel.add(timeRadioButton);
    timeDistancePanel.add(distanceRadioButton);
    topRightPanel.add(timeDistancePanel);

    //set the time/distance button
    if (hardware.unitsTimeDistance == Hardware.TIME) {
        timeRadioButton.setSelected(true);
    }
    else {
        distanceRadioButton.setSelected(true);
    }

    topRightPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    //add an entry for velocity of shear wave sound in steel
    topRightPanel.add(velocityShearSpin = new SpinnerPanel(
                       hardware.hdwVs.velocityShearUS, 0, 5, .0001, "0.0000",
                          60, 23, "Velocity Shear ", units + "/uS", "", null));
    velocityShearSpin.spinner.addChangeListener(this); //monitor changes
    velocityShearSpin.setAlignmentX(Component.LEFT_ALIGNMENT);

    //Marker mode display -
    // one pulse per violation or continuous during violation

    JPanel markerModePanel = new JPanel();
    setSizes(markerModePanel, 220, 24);
    markerModePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    markerPulseButton = new JRadioButton("mark pulse");
    markerPulseButton.setToolTipText(
                            "Pulse marker once for each threshold violation.");
    markerPulseButton.addActionListener(this);
    markerContinuousButton = new JRadioButton("mark continuous");
    markerContinuousButton.setToolTipText(
                     "Fire marker continuously during a threshold violation.");
    markerContinuousButton.addActionListener(this);
    ButtonGroup markerModeGroup = new ButtonGroup();
    markerModeGroup.add(markerPulseButton);
    markerModeGroup.add(markerContinuousButton);
    markerModePanel.add(markerPulseButton);
    markerModePanel.add(markerContinuousButton);
    topRightPanel.add(markerModePanel);

    //set the marker pulse/continuous button
    if (hardware.markerMode == Hardware.PULSE) {
        markerPulseButton.setSelected(true);
    }
    else {
        markerContinuousButton.setSelected(true);
    }

    //vertical spacer
    topRightPanel.add(Box.createRigidArea(new Dimension(0,10)));

    resetChannelButton = new JButton("Reset Channel");
    resetChannelButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    resetChannelButton.setName("Reset Channel");
    resetChannelButton.setToolTipText(
                                "Performs a hardware reset on the channel.");
    resetChannelButton.addActionListener(this);
    resetChannelButton.setActionCommand("Reset Channel");
    topRightPanel.add(resetChannelButton);

    configTab.add(topLeftPanel);
    configTab.add(Box.createRigidArea(new Dimension(7,0))); //horizontal spacer
    configTab.add(topRightPanel);

}//end of UTControls::setupConfigTab
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

public void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of UTControls::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::createColorSwatch
//
// Creates a color swatch for use in displaying colors.
//

public ImageIcon createColorSwatch(Color pColor)
{

    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice gs = ge.getDefaultScreenDevice();
    GraphicsConfiguration gc = gs.getDefaultConfiguration();

    //Java tutorial suggests checking the following cast with "instanceof", to
    //avoid runtime errors but this seems pointless as the cast MUST work for
    //the program to work so it will crash regardless if the cast is bad

    //define the points for a polygon shaped like a paint splotch
    int[] xPoints = {0,  7, 8, 11, 14, 16, 19, 19, 20, 18, 17, 14, 13, 11,
                                                   7,  7,  4,  4,  0,  2, 3, 0};
    int[] yPoints = {5,  4, 0,  3,  0,  4,  2,  8,  9, 12, 16, 14, 17, 19,
                                                  17, 15, 16, 14, 12, 10, 9, 5};


    // have to create a new image buffer for each icon because the icon continues
    // to use the image
    //create an image to store the plot on so it can be copied to the screen
    //during repaint

    BufferedImage imageBuffer;

    imageBuffer = (gc.createCompatibleImage(20, 20, Transparency.OPAQUE));
    Graphics2D g2 = (Graphics2D)imageBuffer.getGraphics();
    //fill the image with same color as menu background
    g2.setColor(new Color(238, 238, 238));
    g2.fillRect(0, 0, imageBuffer.getWidth(), imageBuffer.getHeight());
    g2.setColor(pColor);
    g2.fillPolygon(xPoints, yPoints, 22);

    return new ImageIcon(imageBuffer);

}//end of UTControls::createColorSwatch
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setSpinnerNameAndMouseListener
//
// A mouse listener cannot be added directly to a JSpinner or its sub-classes.
// The listener must be added to the text field inside the spinner to work
// properly.  This function also sets the name of the text field so that the
// mouse listener response method can use it.
//

void setSpinnerNameAndMouseListener(JSpinner pSpinner, String pName,
                                                   MouseListener pMouseListener)
{

    for (Component child : pSpinner.getComponents()) {

        if (child instanceof JSpinner.NumberEditor) {
            for (Component child2 :
                  ((javax.swing.JSpinner.NumberEditor) child).getComponents()){
                ((javax.swing.JFormattedTextField) child2).
                                               addMouseListener(pMouseListener);
                                ((javax.swing.JFormattedTextField) child2).
                                                                setName(pName);
            }
        }
    }

}//end of UTControls::setSpinnerNameAndMouseListener
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::itemStateChanged
//
// Responds to check box changes, etc.
//
// You can tell which item was changed and how by using similar to:
//
// Object source = e.getItemSelectable();
// if (source == scopeOnCheckBox){/*do something*/}
// boolean state = false;
// if (e.getStateChange() == ItemEvent.SELECTED){/*do something*/}
//
// For simplicities sake, the following just updates all controls any time any
// one control is changed.
//

@Override
public void itemStateChanged(ItemEvent e)
{

    //NOTE: ItemEvent does not have an action command, detect component by
    // method getName

    String name;

    //try  casting to a component to use getName
    try{
        name = ((Component)e.getSource()).getName();
    }
    catch (ClassCastException ce){
        //this is an expected exception -- do not print warning to err file
        name = "";
    }

    if (name.equals("Enable DAC")){
        currentChannel.setDACEnabled(dacEnabled.isSelected(), false);
        //set the scope's DAC enabled flag to match the control
        ((OscopeCanvas)oscopeCanvas).setDACEnabled(dacEnabled.isSelected());
    }

    if (name.equals("Lock DAC")){
        if (dacLocked.isSelected()){
            //if the DAC is set to locked, disable buttons and clear selection
            //clear the selected flag for the currently selected gate
            //will do nothing if no gate is selected
            currentChannel.setSelectedDACGate(
                                   currentChannel.getSelectedDACGate(), false);

            //disable the delete buttons each time so it gets disabled when the
            //user clicks in a blank space
            deleteDACGate.setEnabled(false);
            deleteAllDACGates.setEnabled(false);

        }
        else{
            //enable the delete all button if unlocked
            //the delete single gets enabled if a gate is selected
            deleteAllDACGates.setEnabled(true);
        }

        //set the scope's variable to match
        ((OscopeCanvas)oscopeCanvas).setDACLocked(dacLocked.isSelected());
        return;
    }// if (e.getItemSelectable() == dacLocked)

    //handle the "Hide Chart" checkbox
    if (name.equals("Hide Chart")){
        updateChartSettings();
        return;
    }

    //if the Interface Tracking mode changes, switch all gate start values so
    //the reflect the currently active mode

    if (name.equals("Interface Tracking")){

        //call here to switch values (gets called again by updateAllSettings,
        //but is not a problem)
        currentChannel.setInterfaceTracking(
                                 interfaceTrackingCheckBox.isSelected(), false);

        updateGateControls(currentChannel);

        //copy values and states of all display controls to the Global variable
        // set
        updateAllSettings(false);

        return;
    }

    //handle the gate AScan "Trigger" mode checkbox
    if (name.equals("AScan Trigger")){
        updateAllSettings(false);
        return;
    }

}//end of UTControls::itemStateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::actionPerformed
//
// Responds to button and radio button clicks, etc.
//
// You can tell which item was changed and how by using similar to:
//
// Object source = e.getItemSelectable();
// if (source == scopeOnCheckBox){/*do something*/}
//
// For simplicities sake, the following just updates all controls any time any
// one control is changed.
//
// NOTE: You do not need to catch the action command for every control which
//  might trigger this function.  This function will call updateAllSettings
//  each time for uncaught actions -- that function will handle any changes.
//

@Override
public void actionPerformed(ActionEvent e)
{

    //handle the "Set" button for chart info - bail out afterwards instead of
    //calling updateAllSettings because that function will fail if a chart with
    //no assigned channels is modified
    if ("Set Chart Values".equals(e.getActionCommand())){
        updateChartSettings();
        return;
    }

    //handle the "Reset Channel" button - bail out afterwards instead of
    //calling updateAllSettings
    if ("Reset Channel".equals(e.getActionCommand())){
        currentChannel.warmReset();
        return;
    }

    //delete the currently selected DAC gate if user clicks button
    if ("Delete DAC Gate".equals(e.getActionCommand())){
        currentChannel.deleteDACGate(pointedDACGate);
        //recalculate the time locations from the pixel locations for all
        //DAC gates
        calculateDACGateTimeLocation();
        deleteDACGate.setEnabled(false); //disable the button again
    }

    //delete all the DAC gates if user clicks button
    if ("Delete All DAC Gates".equals(e.getActionCommand())){
        currentChannel.deleteAllDACGates();
    }

    //copy values and states of all display controls to the Global variable set
    updateAllSettings(false);

}//end of UTControls::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::stateChanged
//
// Responds to value changes in spinners, etc.
//
// You can tell which item was changed by using similar to:
//
// Object source = e.getSource();
//

@Override
public void stateChanged(ChangeEvent e)
{

    //try casting the source component to a spinner - if valid, then get the
    //name if the component is a threshold control, then update chart
    //settings only

    try{
        MFloatSpinner sp;
        sp = (MFloatSpinner)e.getSource();
        if (sp.getName() != null
                && sp.getName().startsWith("Threshold Spinner")){
            updateChartSettings();
            return;
        }
    }
    catch (ClassCastException ce){
        //this is an expected exception -- do not print warning to err file
    }

    //all other components which fire stateChanged events call to copy their
    //values to the appropriate variables
    updateAllSettings(false);

}//end of UTControls::stateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::updateChartSettings
//
// Updates settings in the chart variables to match values in the user
// controls.
//

void updateChartSettings()
{

    //using the references to each adjust control stored when the panel was
    //setup, scan through each threshold storing the values in the
    //threshold object

    int numberOfThresholds = chart.getNumberOfThresholds();

    for (int i=0; i < numberOfThresholds; i++){
        chart.setThresholdLevel(i,
           ((MFloatSpinner) chart.getThreshold(i).levelAdjuster).getIntValue());
    }

    chart.setTitle(chartTitleTextField.getText().substring(
       0, chartTitleTextField.getText().length() < 15 ?
                            chartTitleTextField.getText().length() : 15));

    chart.setShortTitle(shortTitleTextField.getText().substring(
       0, shortTitleTextField.getText().length() < 4 ?
                            shortTitleTextField.getText().length() : 4));

    //only set the hideChart value if it has changed because the main frame must
    //be packed to show the change and it is inelegant to pack it each time this
    //function is called if the flag has not changed state
    if (hideChartCheckBox.isSelected() != !chart.isChartVisible()){
        chart.setChartVisible(!hideChartCheckBox.isSelected());
        //set a flag so that the main window will know that the program and not
        //the user is resizing the main window and thus should be allowed
        frame.pack();
        //the charts need to allocate resources depending on the size of their
        //sizes - the sizes change when they are set visible or invisible
        chart.handleSizeChanges();
    }

}//end of UTControls::updateChartSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::updateAllSettings
//
// Copies the values and states of all display controls to the corresponding
// variables.
//
// This function should be called when ANY control is modified so that the new
// states and values will be copied to the variable set.
//
// If updateEnabled is false, the function does nothing.  This flag is set to
// false when it is necessary to update the controls without them responding
// to the change.
//
// if pForceUpdate, all values are sent to the DSPs even if they have not
// changed.
//

public void updateAllSettings(boolean pForceUpdate)
{

    if (!updateEnabled) {
        return;
    }

    //using the references to each adjust control stored when the panel was
    //setup, scan through each gate storing the values in the gate object

    Channel ch = currentChannel; //use a short name

    int numberOfGates = ch.getNumberOfGates();
    Gate gate;

    for (int i=0; i < numberOfGates; i++){

        gate = ch.getGate(i);

        ch.setGateStart(i,((MFloatSpinner)
         gate.gateStartAdjuster).getDoubleValue() / timeDistMult, pForceUpdate);
        ch.setGateWidth(i,((MFloatSpinner)
         gate.gateWidthAdjuster).getDoubleValue() / timeDistMult, pForceUpdate);
        ch.setGateLevel(i,((MFloatSpinner)
                           gate.gateLevelAdjuster).getIntValue(), pForceUpdate);

        ch.setGateHitCount(i,((MFloatSpinner)
                        gate.gateHitCountAdjuster).getIntValue(), pForceUpdate);
        ch.setGateMissCount(i,((MFloatSpinner)
                       gate.gateMissCountAdjuster).getIntValue(), pForceUpdate);

        ch.setGateSigProc(i,
            (String)(((JComboBox)gate.processSelector).getSelectedItem()),
                                                                 pForceUpdate);

        ch.setGateSigProcThreshold(i,
          ((MFloatSpinner) gate.thresholdAdjuster).getIntValue(), pForceUpdate);

        //not all gates have a trigger check box (such as interface gates), so
        //check for null before using

        if (((JCheckBox)gate.aScanTriggerCheckBox) != null) {
            ch.setAScanTrigger(i,
             ((JCheckBox)gate.aScanTriggerCheckBox).isSelected(), pForceUpdate);
        }

    }//for (int i=0; i < numberOfGates; i++)

    ch.setDelay(delaySpin.spinner.getDoubleValue() / timeDistMult, pForceUpdate);

    if (interfaceTrackingCheckBox != null) {ch.setInterfaceTracking(
                        interfaceTrackingCheckBox.isSelected(), pForceUpdate);}

    ch.setDACEnabled(dacEnabled.isSelected(), pForceUpdate);
    //set the scope's DAC enabled flag to match the control
    ((OscopeCanvas)oscopeCanvas).setDACEnabled(dacEnabled.isSelected());

    if (posHalfRadioButton.isSelected()) {displayMode = 0;}
    if (negHalfRadioButton.isSelected()) {displayMode = 1;}
    if (fullWaveRadioButton.isSelected()) {displayMode = 2;}
    if (rfRadioButton.isSelected()) {displayMode = 3;}
    if (offRadioButton.isSelected()) {displayMode = 4;}

    ch.setMode(displayMode, pForceUpdate);
    //only set the previous mode if the current mode is not "OFF" -- the
    //previous mode should always be the last "ON" mode so it can be restored
    if (displayMode != UTBoard.CHANNEL_OFF) {ch.previousMode = displayMode;}
    setChannelSelectorColor(ch);

    //always set range after setting gate position or width, delay and interface
    //tracking as these affect the range
    ch.setRange(
              rangeSpin.spinner.getDoubleValue() / timeDistMult, pForceUpdate);
    ch.setSoftwareGain(gainSpin.spinner.getDoubleValue(), pForceUpdate);

    hardware.hdwVs.nominalWall = nomWallSpin.spinner.getDoubleValue();
    hardware.hdwVs.nominalWallChartPosition =
                                          nomWallPosSpin.spinner.getIntValue();
    hardware.hdwVs.wallChartScale = wallScaleSpin.spinner.getDoubleValue();
    hardware.hdwVs.velocityUS = velocitySpin.spinner.getDoubleValue();
    //calculate velocity in distance per NS
    hardware.hdwVs.velocityNS = hardware.hdwVs.velocityUS / 1000;
    hardware.hdwVs.numberOfMultiples = numMultiplesSpin.spinner.getIntValue();

    //the screen will not be updated until the user changes channels or exits
    //the calibrator when inches/mm button changed
    if (inchesRadioButton.isSelected()){hardware.units = Hardware.INCHES;}
    if (mmRadioButton.isSelected()){hardware.units = Hardware.MM;}

    if (timeRadioButton.isSelected()){
        hardware.unitsTimeDistance = Hardware.TIME;
    }
    if (distanceRadioButton.isSelected()){
        hardware.unitsTimeDistance = Hardware.DISTANCE;
    }

    if (markerPulseButton.isSelected()) {
        hardware.markerMode = Hardware.PULSE;
    }
    if (markerContinuousButton.isSelected()) {
        hardware.markerMode = Hardware.CONTINUOUS;
    }

    hardware.hdwVs.velocityShearUS = velocityShearSpin.spinner.getDoubleValue();
    //calculate velocity in distance per NS
    hardware.hdwVs.velocityShearNS = hardware.hdwVs.velocityShearUS / 1000;

    ch.setHardwareGain(hardwareGainSpin1.spinner.getIntValue(),
                        hardwareGainSpin2.spinner.getIntValue(), pForceUpdate);
    ch.setRejectLevel(rejectSpin.spinner.getIntValue(), pForceUpdate);
    ch.setAScanSmoothing(smoothingSpin.spinner.getIntValue(), pForceUpdate);
    ch.setDCOffset(dcOffsetSpin.spinner.getIntValue(), pForceUpdate);

    //recalculate everything that is based on the controls
    calculateAllUTValues();

}//end of UTControls::updateAllSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::setChannelSelectorColor
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
        ((JRadioButton)pChannel.calRadioButton).setToolTipText(pChannel.title
                      + " - This channel is Off - see Signal Tab / Mode Panel");
    }
    else{
        ((JRadioButton)pChannel.calRadioButton).setFont(blackFont);
        ((JRadioButton)pChannel.calRadioButton).setToolTipText(pChannel.detail);
    }

}//end of UTControls::setChannelSelectorColor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::calculateAllUTValues
//
// Calculates all UT values based on user and ini file settings.  Call this
// at startup and any time a user changes an input value.
//

public void calculateAllUTValues()
{

    //for RF mode, shift trace and gates up to center of screen so both positive
    //and negative halves of the signal will be visible
    //for all other modes, do not shift

    if (displayMode == 3) {
        ((OscopeCanvas)oscopeCanvas).
                                  setVertOffset(oscopeCanvas.getHeight() / 2 );
    }
    else {
        ((OscopeCanvas)oscopeCanvas).setVertOffset(0);
    }

    //calculate the uS per pixel using current Range setting and canvas width
    currentChannel.uSPerPixel =
                            currentChannel.getRange() / oscopeCanvas.getWidth();

    //prevent divide by zero errors
    if (currentChannel.uSPerPixel <= 0) {currentChannel.uSPerPixel = .1;}

    //also calculate nS per pixel
    currentChannel.nSPerPixel = currentChannel.uSPerPixel * 1000;

    currentChannel.delayPix =
                  (int)(currentChannel.getDelay() / currentChannel.uSPerPixel);

    calculateGatePixelLocation();

}//end of UTControls::calculateAllUTValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::calculateGatePixelLocation
//
// Calculates the pixel locations for a gate
//

public void calculateGatePixelLocation()
{

    double uSPerPixel = currentChannel.uSPerPixel;
    int delayPix;

    //get the current vertical offset for the scope display
    int vertOffset = ((OscopeCanvas)oscopeCanvas).getVertOffset();

    //if interface tracking is off, the gates must take into effect the delay
    //to the edge of the screen
    //if tracking is on, the gate positions are relative to the interface and
    //do not take into account any delay
    //the exception is the interface gate, which is corrected later in the code

    if (!currentChannel.getInterfaceTracking()) {
        delayPix = currentChannel.delayPix;
    }
    else {
        delayPix = 0;
    }

    //scan through the gates calculating the pixel locations of each setting
    //the pixel values are used by the display function to avoid having to waste
    //time recomputing them each time
    int numberOfGates = currentChannel.getNumberOfGates();
    Gate gate;
    for (int i=0; i < numberOfGates; i++){

        gate = currentChannel.getGate(i);

        gate.calculateGatePixelLocation(
                   uSPerPixel, delayPix, oscopeCanvas.getHeight(), vertOffset);

    }

    //calculate the pixel locations for the DAC gates
    int numberOfDACGates = currentChannel.getNumberOfDACGates();
    DACGate dacGate;

    for (int i=0; i < numberOfDACGates; i++){

        dacGate = currentChannel.getDACGate(i);

        dacGate.calculateGatePixelLocation(
                   uSPerPixel, delayPix, oscopeCanvas.getHeight(), vertOffset);

    }

    //if interface tracking is on, the above code sets the interface gate without
    //concern for the AScan delay - however the interface gate always uses
    //absolute positioning so recompute here for the interface gate (gate 0)

    if (currentChannel.getInterfaceTracking()){

        delayPix = currentChannel.delayPix;

        gate = currentChannel.getGate(0);

        gate.gatePixStart =
                 (int)(currentChannel.getGateStart(0) / uSPerPixel - delayPix);

        gate.gatePixEnd = gate.gatePixStart
                        + (int)(currentChannel.getGateWidth(0) / uSPerPixel);

    }

}//end of UTControls::calculateGatePixelLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::calculateDACGateTimeLocation
//
// Calculates the time locations for a gate from its pixel location and
// various related offsets.
//

public void calculateDACGateTimeLocation()
{

    Channel ch = currentChannel;
    double uSPerPixel = currentChannel.uSPerPixel;
    int delayPix;

    //if interface tracking is off, the gates must take into effect the delay
    //to the edge of the screen
    //if tracking is on, the gate positions are relative to the interface and
    //do not take into account any delay
    //the exception is the interface gate, which is corrected later in the code

    if (!ch.getInterfaceTracking()) {
        delayPix = ch.delayPix;
    }
    else {
        delayPix = 0;
    }

    //get the current vertical offset for the scope display
    int vertOffset = ((OscopeCanvas)oscopeCanvas).getVertOffset();

    //calculate the time locations for the DAC gates
    int numberOfDACGates = ch.getNumberOfDACGates();
    DACGate dacGate;

    for (int i=0; i < numberOfDACGates; i++){

        ch.calculateDACGateTimeLocation(i, uSPerPixel, delayPix,
                                  oscopeCanvas.getHeight(), vertOffset, false);

    }

}//end of UTControls::calculateDACGateTimeLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::mousePressedOnScope
//
// Processes mouse button press events on the scope.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void mousePressedOnScope(MouseEvent e)
{

    //bail out if the selected component has not had the name set
    if (getSelectedComponent().getName() == null) {return;}

    //if the currently selected tab is the DAC, call associated function
    if (getSelectedComponent().getName().equals("DAC")){
        handleMousePressForDAC(e);
    }

}//end of UTControls::mousePressedOnScope
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::mouseReleasedOnScope
//
// Processes mouse button release events on the scope.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void mouseReleasedOnScope(MouseEvent e)
{

    //bail out if the selected component has not had the name set
    if (getSelectedComponent().getName() == null){return;}

    //if the currently selected tab is the DAC, call associated function
    if (getSelectedComponent().getName().equals("DAC")) {
        handleMouseReleaseForDAC(e);
    }

}//end of UTControls::mouseReleasedOnScope
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::handleMousePressForDAC
//
// Processes mouse button press events on the scope when the DAC tab is
// displayed.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void handleMousePressForDAC(MouseEvent e)
{

    //do nothing if the "Enable DAC" checkbox is unchecked or the "Lock DAC"
    //checkbox is checked
    if (!dacEnabled.isSelected() || dacLocked.isSelected()){return;}

    //process left click events for the DAC gates
    if (e.getButton() == MouseEvent.BUTTON1) {handleLeftClickForDAC(e);}

    //process right click events for the DAC gates
    if (e.getButton() == MouseEvent.BUTTON3) {handleRightClickForDAC(e);}

}//end of UTControls::handleMousePressForDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::handleRightClickForDAC
//
// Processes mouse right click events on the scope when the DAC tab is
// displayed.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void handleRightClickForDAC(MouseEvent e)
{

    Channel ch = currentChannel;

    int x = e.getX();

    //if interface tracking is on, adjust the pixel location to account for
    //the interface crossing being the reference point
    if (ch.getInterfaceTracking()){
        x -= ch.gates[0].interfaceCrossingPixAdjusted;
    }

    //clear the selected flag for the currently selected gate
    //will do nothing if no gate is selected
    ch.setSelectedDACGate(ch.getSelectedDACGate(), false);
    deleteDACGate.setEnabled(false); //disable the delete button

    ch.insertDACGate(x, e.getY());

    //calculate the time locations from the pixel locations for all DAC gates
    calculateDACGateTimeLocation();

}//end of UTControls::handleRightClickForDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::handleLeftClickForDAC
//
// Processes mouse left click events on the scope when the DAC tab is
// displayed.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void handleLeftClickForDAC(MouseEvent e)
{

    //clear the selected flag for the currently selected gate
    //will do nothing if no gate is selected
    currentChannel.setSelectedDACGate(currentChannel.getSelectedDACGate(),
                                                                        false);

    //disable the button each time so it gets disabled when the user clicks
    //in a blank space
    deleteDACGate.setEnabled(false);

    //find which DAC gate the start of which the mouse is pointing at
    //if none, then bail out
    if ((pointedDACGate = currentChannel.getPointedDACGate(e.getX(), e.getY()))
            == -1) {
        return;
    }

    //set the selected gate as being dragged - when the user releases the left
    //button draggingDACGate will be set back to -1
    draggingDACGate = pointedDACGate;

    //set the selected flag for the gate pointed by the mouse
    //this will also cause it to be highlighted
    currentChannel.getDACGate(pointedDACGate).setSelected(true);

    //enable the delete button so the user can use it if desired
    deleteDACGate.setEnabled(true);

}//end of UTControls::handleLeftClickForDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::handleMouseReleaseForDAC
//
// Processes mouse button release events on the scope when the DAC tab is
// displayed.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void handleMouseReleaseForDAC(MouseEvent e)
{

    //do nothing if the "Enable DAC" checkbox is unchecked or the "Lock DAC"
    //checkbox is checked
    if (!dacEnabled.isSelected() || dacLocked.isSelected()) {return;}

    //process left click releases for the DAC gates
    if (e.getButton() == MouseEvent.BUTTON1){
        //force recalc of all gate time locations and flag for sending
        //to remotes
        if (draggingDACGate != -1) {
            calculateDACGateTimeLocation();
        }
        draggingDACGate = -1;  //release the gate from being dragged
    }

    //process right click releases for the DAC gates
    if (e.getButton() == MouseEvent.BUTTON3) {}

}//end of UTControls::handleMouseReleaseForDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::mouseDragged
//
// Responds when the mouse is dragged on the scope.  If a gate has been
// selected by clicking on its selection circle, then adjust the gate's
// position.
//
// NOTE: The actual mouse listener for this event is UTCalibrator which owns
// the canvas.  It passes those events here.  This UTControls class serves
// directly as a mouse listener for the controls on its window.
//

public void mouseDraggedOnScope(MouseEvent e)
{

    Channel ch = currentChannel;

    if (draggingDACGate == -1) {return;} //do nothing if not gate selected

    int xPos = e.getX(), yPos = e.getY();

    //if interface tracking is on, adjust the pixel location to account for
    //the interface crossing being the reference point
    if (ch.getInterfaceTracking()){
        xPos -= ch.gates[0].interfaceCrossingPixAdjusted;
    }

    //don't allow positioning off the screen
    if (xPos < 0) {xPos = 0;}

    if (yPos < 0) {yPos = 0;}
    if (yPos > oscopeCanvas.getHeight()) {yPos = oscopeCanvas.getHeight();}

    //don't allow dragging in front of prior gate
    if (draggingDACGate > 0){
        if (xPos < ch.getDACGate(draggingDACGate-1).gatePixStart) {
            xPos = ch.getDACGate(draggingDACGate-1).gatePixStart;
        }
    }

    //don't allow dragging gate start past its own end unless it is the last
    //gate, in which case the end will follow the beginning - this allows the
    //last gate to be stretched as need - it has to be handled differently
    //because there is not a grab point at the end of the last gate

    //the gate can be the last by two ways - being the last spot in the array or
    //being the last active gate in the array

    if ( (draggingDACGate < (ch.getNumberOfDACGates()-1))
                              && ch.getDACGate(draggingDACGate+1).getActive()){

        if (xPos > ch.getDACGate(draggingDACGate).gatePixEnd) {
            xPos = ch.getDACGate(draggingDACGate).gatePixEnd;
        }
    }
    else {
        if (xPos > ch.getDACGate(draggingDACGate).gatePixEnd) {
            ch.getDACGate(draggingDACGate).gatePixEnd = xPos;
        }
    }

    ch.getDACGate(draggingDACGate).gatePixStart = xPos;
    ch.getDACGate(draggingDACGate).gatePixLevel = yPos;

    //if not the first gate, then change the previous gates end point to
    //track the start point of the gate being adjusted
    if (draggingDACGate > 0){
        ch.getDACGate(draggingDACGate-1).gatePixEnd = xPos;
    }

}//end of UTControls::mouseDragged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTCalibrator::mouseClicked
//
// Responds when the mouse button is release while over a component which is
// listening to the mouse.
//
// NOTE: The controls which trigger this event by right click are expected to
// be valid controls for copying between channels.  If other controls trigger
// this event, they should be trapped and processed at the beginning of the
// function to prevent their being added to the copy item list.
//

@Override
public void mouseClicked(MouseEvent e)
{

    int button = e.getButton();

    //catch left clicks
    if (button == MouseEvent.BUTTON1){

        return;

    }

    //catch right clicks
    if (button == MouseEvent.BUTTON3){

        //make sure the "Copy Items" window is displayed to the right of
        //the calibrator window which changes size depending on its contents
        utCalibrator.setCopyItemsWindowLocation();

        //add the item to the list of items to be copied
        copyItemSelector.addItem(e.getComponent().getName());

        //tell UTCalibrator object to display the "Copy" controls
        utCalibrator.actionPerformed(new ActionEvent(this, 1, "Copy"));

    }

}//end of UTCalibrator::mouseClicked
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTControls::(various mouse listener functions)
//
// These functions are implemented per requirements of interface MouseListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void mousePressed(MouseEvent e){}

@Override
public void mouseReleased(MouseEvent e){}

@Override
public void mouseEntered(MouseEvent e){}

@Override
public void mouseExited(MouseEvent e){}

//end of UTControls::(various mouse listener functions)
//-----------------------------------------------------------------------------

//debug mks System.out.println(String.valueOf(value)); //debug mks

}//end of class UTControls
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

