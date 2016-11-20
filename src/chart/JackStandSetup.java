/******************************************************************************
* Title: JackStandSetup.java
* Author: Mike Schoonover
* Date: 11/06/16
*
* Purpose:
*
* This class displays a window for entering distances for the movable jack
* stands and their sensors.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.hardware.EncoderCalValues;
import chart.mksystems.hardware.SensorData;
import chart.mksystems.inifile.IniFile;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class JackStandSetup
//
// This class displays a window for calibrating the encoder counts per distance
// values.
//

class JackStandSetup extends JDialog implements ActionListener {

        
    private EncoderCalValues encoderCalValues;

    private JPanel mainPanel;

    private JPanel displayPanel;

    private JLabel calMessageLbl;

    JCheckBox displayMonitorCB, enableEyeDistanceInputsCB;
    
    JButton applyBtn;
    
    private Font blackFont, redFont;

    private final ActionListener actionListener;
    private final IniFile configFile;

    int numEntryJackStands, numExitJackStands;
    
    JTextField numEntryJackStandsTF, numExitJackStandsTF;
    
    ArrayList<SensorSetGUI> sensorSetGUIs = new ArrayList<>(1);

    KeyAdapter keyAdapter;    

    private final static int ENTRY_JACK_GROUP = 0;
    private final static int EXIT_JACK_GROUP = 1;
    static final int UNIT_SENSOR_GROUP = 2;    
    
    private static int MAX_NUM_UNIT_SENSORS;    
    private static int NUM_UNIT_SENSORS;
    private static int MAX_NUM_JACKS_ANY_GROUP;
    private static int MAX_TOTAL_NUM_SENSORS;

    private static int UNDEFINED_GROUP;
    private static int INCOMING;
    private static int OUTGOING;
    private static int UNIT;

    private static int UNDEFINED_EYE;
    private static int EYE_A;
    private static int EYE_B;
    private static int SELF;
    
    private static int UNDEFINED_DIR;
    private static int STOPPED;
    private static int FWD;
    private static int REV;

    private static int UNDEFINED_STATE;
    private static int UNBLOCKED;
    private static int BLOCKED;
        
//-----------------------------------------------------------------------------
// JackStandSetup::JackStandSetup (constructor)
//
//

public JackStandSetup(JFrame frame, IniFile pConfigFile,
                                                ActionListener pActionListener)
{

    super(frame, "Jack Stand Setup");

    //get a local copy of constants for easier use
    
    MAX_NUM_UNIT_SENSORS = EncoderCalValues.MAX_NUM_UNIT_SENSORS;
    NUM_UNIT_SENSORS = EncoderCalValues.NUM_UNIT_SENSORS;
    MAX_NUM_JACKS_ANY_GROUP = EncoderCalValues.MAX_NUM_JACKS_ANY_GROUP;
    MAX_TOTAL_NUM_SENSORS = EncoderCalValues.MAX_TOTAL_NUM_SENSORS;

    UNDEFINED_GROUP = EncoderCalValues.UNDEFINED_GROUP;
    INCOMING = EncoderCalValues.INCOMING;
    OUTGOING = EncoderCalValues.OUTGOING;
    UNIT = EncoderCalValues.UNIT;

    UNDEFINED_EYE = EncoderCalValues.UNDEFINED_EYE;
    EYE_A = EncoderCalValues.EYE_A;
    EYE_B = EncoderCalValues.EYE_B;
    SELF = EncoderCalValues.SELF;

    UNDEFINED_DIR = EncoderCalValues.UNDEFINED_DIR;
    STOPPED = EncoderCalValues.STOPPED;
    FWD = EncoderCalValues.FWD;
    REV = EncoderCalValues.REV;

    UNDEFINED_STATE = EncoderCalValues.UNDEFINED_STATE;
    UNBLOCKED = EncoderCalValues.UNBLOCKED;
    BLOCKED = EncoderCalValues.BLOCKED;
    
    actionListener = pActionListener;
    configFile = pConfigFile;

    createKeyAdapter();
    
}//end of JackStandSetup::JackStandSetup (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::init
//
// Initializes the object.
//

public void init()
{
        
    setupGUI();
    
    pack();

}//end of JackStandSetup::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupGUI
//

private void setupGUI()
{

    mainPanel = new JPanel();
    mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

    add(mainPanel);
  
    displayMonitorCB = new JCheckBox("Display Monitor");
    displayMonitorCB.setActionCommand("Display Monitor");
    displayMonitorCB.addActionListener(this);
    
    enableEyeDistanceInputsCB = new JCheckBox("Enable Eye Distance Editing");
    enableEyeDistanceInputsCB.setActionCommand("Enable Eye Distance Editing");
    enableEyeDistanceInputsCB.addActionListener(this);
     
    displayPanel = new JPanel();
    mainPanel.add(displayPanel);
    
    setupDisplayPanel(displayPanel);
        
    mainPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer    
    
    setupNumJacksInputPanel(mainPanel);
    
    setupControlsPanel(mainPanel);
    
}//end of JackStandSetup::setupGUI
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::resetDisplayPanel
//
// Removes all components from the displayPanel and sets it up again. Useful
// when switching between displaying Monitor values and not displaying them.
//
// Garbage collection run afterwards to clean up all the objects orphaned by
// this operation.
//

private void resetDisplayPanel()
{

    displayPanel.removeAll();
  
    setupDisplayPanel(displayPanel);

    refreshAllGUI(true);
    
    pack();
    
    repaint();
    
    System.gc();

}//end of JackStandSetup::resetDisplayPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setEyeDistanceEditingEnabled
//
// Sets the enable state of the eye distance inputs text fields.
//

private void setEyeDistanceEditingEnabled()
{

    setJackStandsEyeDistanceEditingEnabled(
                                        enableEyeDistanceInputsCB.isSelected());
    
}//end of JackStandSetup::setEyeDistanceEditingEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupDisplayPanel
//
// Adds various inputs and displays to pPanel. Calling function should
// add the panel to its parent.
//
//

private void setupDisplayPanel(JPanel pPanel)
{

    pPanel.setLayout(new BoxLayout(pPanel, BoxLayout.Y_AXIS));    
    
    setupDisplayGrid(pPanel);
  
    pPanel.add(Box.createRigidArea(new Dimension(0,7))); //vertical spacer

    setupMsgDisplayPanel(pPanel);
        
}//end of JackStandSetup::setupDisplayPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupDisplayGrid
//
// Creates a grid of labels, inputs, and displays for the various sensors.
//

private void setupDisplayGrid(JPanel pParentPanel)
{
    
    JPanel panel = new JPanel();
    
    panel.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    //use grid layout with enough rows to display all entry and exit
    //jackstands, unit entry eye, unit exit eye, and two rows for a header
    //and blank separator row, and two separator rows between groups
    
    int numGridRows = 
                numEntryJackStands + numExitJackStands + NUM_UNIT_SENSORS + 4;
    
    int numGridCols;
    
    if(displayMonitorCB.isSelected()){
        numGridCols = 10;
    }else{
        numGridCols = 4;
    }
    
    panel.setLayout(new GridLayout(numGridRows, numGridCols, 10, 10));

    panel.setOpaque(true);
    pParentPanel.add(panel);
        
    addLabels(panel, 1,"");
    addLabel(panel, "  Eye A", "distance to jack center");
    addLabel(panel, "Jack Stand", "distance to unit");
    addLabel(panel, "  Eye B", "distance to jack center");

        //these only visible if Display Monitor checkbox is checked
    if (displayMonitorCB.isSelected()){
        addLabel(panel, "State", "indicates which sensor last changed and "
                                        + "whether it is blocked or unblocked");
        addLabel(panel, "Encoder 1", "counts at time of state change");
        addLabel(panel, "Encoder 2", "counts at time of state change");
        addLabel(panel, "Direction",
                                "conveyor direction at time of state change");
        addLabel(panel, "Counts/Inch",
            "counts per inch calculated between last two sensor state changes");
        addLabel(panel, "% change",
                                "% calibration change since last calculation");        
    }
    
    
    sensorSetGUIs.clear(); //remove all entries
    
    addLabels(panel, numGridCols, "");
    
    setupEntryJackStandGUIs(panel);

    addLabels(panel, numGridCols, "");
    
    setUpUnitSensorGUIs(panel);
        
    addLabels(panel, numGridCols, "");
    
    setupExitJackStandGUIs(panel);

}//end of JackStandSetup::setupDisplayGrid
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupMsgDisplayPanel
//
// Creates a panel with a label for display eye-encoder matching messages
// from the PLC.
//

private void setupMsgDisplayPanel(JPanel pParentPanel)
{
    
    //do not display if monitor display is not selected
    if(!displayMonitorCB.isSelected()) { return; }
    
    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder(
                                        "Sensor-Encoder Calibration Messages"));
    JackStandSetup.setSizes(panel, 700, 50);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.CENTER_ALIGNMENT);
    pParentPanel.add(panel);
        
    calMessageLbl = new JLabel("waiting for message...");
    calMessageLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(calMessageLbl);
    
}//end of JackStandSetup::setupMsgDisplayPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupNumJacksInputPanel
//
// Creates a panel with inputs for the number of entry and exit jack stands.
//

private void setupNumJacksInputPanel(JPanel pParentPanel)
{
    
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.CENTER_ALIGNMENT);
    pParentPanel.add(panel);
        
    JPanel leftPanel = new JPanel();
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.X_AXIS));
    setSizes(leftPanel, 250, 25);
    leftPanel.add(new JLabel("Number of entry jacks: "));
    numEntryJackStandsTF = new JTextField(3);
    numEntryJackStandsTF.addKeyListener(keyAdapter);
    leftPanel.add(numEntryJackStandsTF);
    leftPanel.add(new JLabel("exit jacks: "));
    numExitJackStandsTF = new JTextField(3);
    numExitJackStandsTF.addKeyListener(keyAdapter);
    leftPanel.add(numExitJackStandsTF);
    panel.add(leftPanel);
    
    panel.add(Box.createHorizontalGlue());
    

}//end of JackStandSetup::setupNumJacksInputPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupControlsPanel
//
// Creates a panel with various controls.
//

private void setupControlsPanel(JPanel pParentPanel)
{
    
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.CENTER_ALIGNMENT);
    pParentPanel.add(panel);
        
    panel.add(displayMonitorCB);
    
    panel.add(enableEyeDistanceInputsCB);

    panel.add(Box.createHorizontalGlue());
    
    applyBtn = new JButton("Apply");
    applyBtn.setActionCommand("Apply");
    applyBtn.addActionListener(this);
    applyBtn.setEnabled(false);
    panel.add(applyBtn);

}//end of JackStandSetup::setupControlsPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::addLabels
//
// Adds label to pPanel with pText and pToolTip.
//

private void addLabel(JPanel pPanel, String pText, String pToolTip)
{
    
    JLabel label;
    pPanel.add(label = new JLabel(pText));
    label.setToolTipText(pToolTip);

}//end of JackStandSetup::addLabelWithToolTip
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::addLabels
//
// Adds pNumLabels labels to mainPanel with pText.
//

private void addLabels(JPanel pPanel, int pNumLabels, String pText)
{
    
    for(int i=0; i<pNumLabels; i++){
    
        pPanel.add(new JLabel(pText));
    
    }
    
}//end of JackStandSetup::addLabels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupEntryJackStandGUIs
//
// Adds a GUI panel for each entry jack stand.
//

private void setupEntryJackStandGUIs(JPanel pPanel)
{
    
    //display entry jacks in reverse order to depict physical layout
    
    for (int i=numEntryJackStands-1; i>=0; i--){

        SensorSetGUI set;
        
        set = new JackStandGUI(ENTRY_JACK_GROUP, i, pPanel, keyAdapter, this);
        
        set.init("Entry Jack", 
         enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());
        
        sensorSetGUIs.add(set);
        
    }
    
}//end of JackStandSetup::setupEntryJackStandGUIs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setupExitJackStandGUIs
//
// Adds a GUI panel for each exit jack stand.
//

private void setupExitJackStandGUIs(JPanel pPanel)
{

    //display exit jacks in forward order to depict physical layout
    
    for (int i=0; i<numExitJackStands; i++){

        SensorSetGUI set;
        
        set = new JackStandGUI(EXIT_JACK_GROUP, i, pPanel, keyAdapter, this);
        
        set.init("Exit Jack", 
         enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());
        
        sensorSetGUIs.add(set);
                
    }
    
}//end of JackStandSetup::setupExitJackStandGUIs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setUpUnitSensorGUIs
//
// Adds a GUI panel for each unit sensor, such as the exit (inspection start)
// and exit (inspection end).
//

private void setUpUnitSensorGUIs(JPanel pPanel)
{
    
    SensorSetGUI set;

    //use negative set numbers so the numbers are not displayed or used to
    //load cal data...the sensor title is explicit and does need a number
    
    set = new SensorGUI(UNIT_SENSOR_GROUP, 0, pPanel, keyAdapter, this);

    set.init("Entry Sensor", 
     enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());

    sensorSetGUIs.add(set);
                
    set = new SensorGUI(UNIT_SENSOR_GROUP, 1, pPanel, keyAdapter, this);

    set.init("Exit Sensor", 
     enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());

    sensorSetGUIs.add(set);

    set = new SensorGUI(UNIT_SENSOR_GROUP, 2, pPanel, keyAdapter, this);

    set.init("After Drive", 
     enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());

    sensorSetGUIs.add(set);

}//end of JackStandSetup::setUpUnitSensorGUIs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setJackStandsEyeDistanceEditingEnabled
//
// Sets the enabled flag for all eye distance text fields.
//

private void setJackStandsEyeDistanceEditingEnabled(boolean pEnabled)
{

    sensorSetGUIs.stream().forEach((set) -> {
        set.setEyeDistanceEditingEnabled(pEnabled);
    });
        
}//end of JackStandSetup::setJackStandsEyeDistanceEditingEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setEncoderCalValues
//
// Sets all encoder calibrations via pEncoderCalValues and updates labels to
// reflect the data.
//
// If required, the GUI is reset to reflect changes such as the number of
// entry or exit jacks.
//

public void setEncoderCalValues(EncoderCalValues pEncoderCalValues)
{
    
    encoderCalValues = pEncoderCalValues;
 
    boolean relayoutGUI = false;

    if (pEncoderCalValues.numEntryJackStands > MAX_NUM_JACKS_ANY_GROUP){
        pEncoderCalValues.numEntryJackStands = MAX_NUM_JACKS_ANY_GROUP;
    }
    
    if (pEncoderCalValues.numEntryJackStands != numEntryJackStands){
        relayoutGUI = true;
        numEntryJackStands = pEncoderCalValues.numEntryJackStands;
    }

    if (pEncoderCalValues.numExitJackStands > MAX_NUM_JACKS_ANY_GROUP){
        pEncoderCalValues.numExitJackStands = MAX_NUM_JACKS_ANY_GROUP;
    }
        
    if(pEncoderCalValues.numExitJackStands != numExitJackStands){
        relayoutGUI = true;
        numExitJackStands = pEncoderCalValues.numExitJackStands;
    }
    
    if(relayoutGUI){ resetDisplayPanel(); }

    //if the GUI was changed or data in pEncoderCalValues has changed, then
    //refresh all labels with data
    
    pEncoderCalValues.sensorTransitionDataChanged = true; //debug mks -- remove this
    
    if (relayoutGUI || pEncoderCalValues.sensorTransitionDataChanged){
        refreshAllGUI(false);
    }
    
}//end of JackStandSetup::setEncoderCalValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::getEncoderCalValues
//
// Returns all jack stand calibrations via pEncoderCalValues. The function
// itself returns a reference to pEncoderValues.
//

public EncoderCalValues getEncoderCalValues(EncoderCalValues pEncoderCalValues)
{
 
    pEncoderCalValues.numEntryJackStands = numEntryJackStands;
    pEncoderCalValues.numExitJackStands = numExitJackStands;    

    //transfer all user inputs to pEncoderCalValues
    parseAllSensorInputs();
    
    return(pEncoderCalValues);
    
}//end of JackStandSetup::getEncoderCalValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseAllSensorInputs
//
// Transfers sensor data from all input boxes of all sensors of all groups
// to pEncoderCalValues.
//

private void parseAllSensorInputs()
{

    parseSensorSetGroupInputs(ENTRY_JACK_GROUP);
    
    parseSensorSetGroupInputs(EXIT_JACK_GROUP);
    
    parseSensorSetGroupInputs(UNIT_SENSOR_GROUP);
    
}//end of JackStandSetup::parseAllSensorInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseSensorSetGroupInputs
//
// Transfers sensor data from  all input boxes for all sensor sets in the group
// specified by pGroupNum to pEncoderCalValues.
//

private void parseSensorSetGroupInputs(int pGroupNum)
{
    
    int numSensors = 0;

    if (pGroupNum == ENTRY_JACK_GROUP){ numSensors = numEntryJackStands; }
    else if (pGroupNum == EXIT_JACK_GROUP){ numSensors = numExitJackStands; }
    else if (pGroupNum == UNIT_SENSOR_GROUP){ numSensors = NUM_UNIT_SENSORS; }    

    for (int i=0; i<numSensors; i++){
        //get object which handles GUI
        SensorSetGUI setGUI = getSensorSetFromGUIList(pGroupNum, i);
        //get object with sensor data
        SensorData sensorDatum = getSensorDatumFromSensorDataList(pGroupNum, i);
        //transfer data from inputs to encoderCalValue
        parseSensorDatum(sensorDatum, setGUI);
    }

}//end of JackStandSetup::parseSensorSetGroupInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseSensorDatum
//
// Transfers unit sensor data from inputs boxes of pSensorDatum to
// pEncoderCalValues.
//

private void parseSensorDatum(SensorData pSensorDatum,
                                                     SensorSetGUI pSensorSetGUI)
{

    pSensorDatum.setJackCenterDistToMeasurePoint(
                       parseDoubleFromTextField(pSensorSetGUI.jackStandDistTF));
        
    pSensorDatum.setEyeADistToJackCenter( 
                            parseDoubleFromTextField(pSensorSetGUI.eyeADistTF));

    pSensorDatum.setEyeBDistToJackCenter(
                            parseDoubleFromTextField(pSensorSetGUI.eyeBDistTF));
           
}//end of JackStandSetup::parseSensorDatum
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseDoubleFromTextField
//
// Converts the text in pTextField to a double and returns the value. If the
// parsing fails, returns 0.
//

private double parseDoubleFromTextField(JTextField pTextField)
{
 
    if(pTextField == null){ return(0.0); }
    
    double val;

    try{
        val = Double.valueOf(pTextField.getText().trim());
    }
    catch(NumberFormatException nfe){
        val = 0.0;
    }

    return(val);

}//end of JackStandSetup::parseDoubleFromTextField
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::createKeyAdapter
//
// Creates a KeyAdapter which can be used to monitor when the user makes
// changes in a Text Field.
//

private void createKeyAdapter()
{
    
    keyAdapter = new KeyAdapter(){
        
        @Override
        public void keyPressed(KeyEvent ke)
        {
            //this section will execute only when user is editing the JTextField
            if(!(ke.getKeyChar()==27||ke.getKeyChar()==65535)){
                actionPerformed(new ActionEvent(this, 1, "Text Field Changed"));
            }
        }
    };

}//end of JackStandSetup::createKeyAdapter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setLabelWithFormattedValue
//
//

private void setLabelWithFormattedValue(JLabel pLabel, String pText, 
                                                                 double pValue)
{
    
    pLabel.setText(pText + new DecimalFormat("#.####").format(pValue));

}//end of JackStandSetup::setLabelWithFormattedValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshAllGUI
//
// Updates all GUI components with their associated variable values.
//
// Input boxes are only updated if pUpdateInputBoxes is true. This function
// is called constantly by setEncoderCalValues during normal operation...if it
// updates the input boxes each time it will overwrite whatever the user has
// entered. They should be updated whenever the display panel is reset as that
// destroys and recreates all the boxes and they must be refreshed. This also
// serves to populate the boxes the first time setEncoderCalValues is called
// as that call will force a reset of the display panel due to the number of
// jacks being changed with the data loaded from cal file.
//

public void refreshAllGUI(boolean pUpdateInputBoxes)
{

    if (calMessageLbl != null){ 
        calMessageLbl.setText(encoderCalValues.textMsg);
    }
        
    if(pUpdateInputBoxes){
        
        numEntryJackStandsTF.setText("" + numEntryJackStands);
        numExitJackStandsTF.setText("" + numExitJackStands);
        
    }
    
    refreshSensorDisplays(pUpdateInputBoxes);
    
}//end of JackStandSetup::refreshAllGUI
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshSensorDisplays
//
// Updates all displays related to the sensors with values from     
// encoderCalValues.
//
// Input boxes are only updated if pUpdateInputBoxes is true. See notes for
// refreshAllGUI for more details.
//
// NOTE: the SensorData ArrayList is a reference back to the copy being
// modified by the PLCEthernetController object on a continuing basis. That
// updating may be occurring in a different thread...may have occasional
// glitches in the displayed data.
//

private void refreshSensorDisplays(boolean pUpdateInputBoxes)
{
    
    refreshSensorSetGroupDisplay(ENTRY_JACK_GROUP, pUpdateInputBoxes);
    
    refreshSensorSetGroupDisplay(EXIT_JACK_GROUP, pUpdateInputBoxes);    
    
    refreshSensorSetGroupDisplay(UNIT_SENSOR_GROUP, pUpdateInputBoxes);
    
}//end of JackStandSetup::refreshSensorDisplays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshSensorSetGroupDisplay
//
// Updates all displays for all sensor sets in the group specified by pGroupNum.
//
// Input boxes are only updated if pUpdateInputBoxes is true. See notes for
// refreshAllGUI for more details.
//

private void refreshSensorSetGroupDisplay(
                                    int pGroupNum, boolean pUpdateInputBoxes)
{
    
    int numSensors = 0;

    if (pGroupNum == ENTRY_JACK_GROUP){ numSensors = numEntryJackStands; }
    else if (pGroupNum == EXIT_JACK_GROUP){ numSensors = numExitJackStands; }
    else if (pGroupNum == UNIT_SENSOR_GROUP){ numSensors = NUM_UNIT_SENSORS; }    

    for (int i=0; i<numSensors; i++){
        //get object which handles GUI
        SensorSetGUI setGUI = getSensorSetFromGUIList(pGroupNum, i);
        //get object with sensor data
        SensorData sensorDatum = getSensorDatumFromSensorDataList(pGroupNum, i);
        //display data on GUI
        refreshSensorDatum(sensorDatum, setGUI, pUpdateInputBoxes);
    }

}//end of JackStandSetup::refreshEntryJackDisplays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::getSensorSetFromGUIList
//
// Retrieves a sensor set GUI object from the sensorSetGUIs list based on
// the sensor group number and the sensor's number in that group. The object
// contains references to GUI components for displaying data related to the
// sensor.
//
// There are three groups, all stored in the same sensorSetGUIs list:
//      entry jacks, unit sensors, exit jacks.
//
// Each jack is represented by a sensor set GUI.
// Each unit sensor is also represented by a sensor set GUI even though it is
// only a single sensor; some of the variables are not used in that case.
//
// List Ordering:
//
// the entry jack GUIs are at the top of the list in reverse order
// the unit sensors follow the entry jacks and are in forward order
// the exit jack SensorSets follow the unit sensors and are in forward order
//

SensorSetGUI getSensorSetFromGUIList(int pGroupNum, int pSensorNum)
{
        
    if(pGroupNum == ENTRY_JACK_GROUP){
        //stored in reverse order at top of list
        int firstEntryJack = numEntryJackStands - 1;
        int jackPos = firstEntryJack - pSensorNum;
        return(sensorSetGUIs.get(jackPos));
    }
    
    if(pGroupNum == EXIT_JACK_GROUP){
        //stored in forward order after entry jacks and unit sensors
        int firstExitJack = numEntryJackStands + NUM_UNIT_SENSORS;
        int jackPos = firstExitJack + pSensorNum;
        return(sensorSetGUIs.get(jackPos));
    }

    if(pGroupNum == UNIT_SENSOR_GROUP){
        //stored in forward order after entry jacks
        int firstUnitSensor = numEntryJackStands;
        int jackPos = firstUnitSensor + pSensorNum;
        return(sensorSetGUIs.get(jackPos));
    }

    return(null);
    
}//end of JackStandSetup::getSensorSetFromGUIList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::getSensorDatumFromSensorDataList
//
// Retrieves a sensor set data object from the sensorData list based on
// the sensor group number and the sensor's number in that group. The object
// contains values for the sensor set and jack stand.
//
// There are three groups, all stored in the same sensorData list:
//      entry jacks, unit sensors, exit jacks.
//
// Each jack is represented by a sensor data object.
// Each unit sensor is also represented by a sensor data object even though it
// is only a single sensor; some of the variables are not used in that case.
//
// DIFFERENCE BETWEEN THE ORDERING IN sensorSetGUIs LIST AND sensorData LIST:
//
// The sensorSetGUIs list has exactly the number of entries as needed for each
// group. The sensorData list has enough entries to contain the maximum number
// allowed for each group. Thus there are often more entries in the latter than
// the former.
//
// List Ordering:
//
// the entry jack GUIs are at the top of the list in reverse order, extra
//  positions at the top
// the unit sensors follw the entry jacks and are in forward order
// the exit jack SensorSets follow the unit sensors and are in forward order,
//  extra positions at the bottom
//

SensorData getSensorDatumFromSensorDataList(int pGroupNum, int pSensorNum)
{
        
    if(pGroupNum == ENTRY_JACK_GROUP){
        //stored in reverse order at top of list
        int firstEntryJack = MAX_NUM_JACKS_ANY_GROUP - 1;
        int jackPos = firstEntryJack - pSensorNum;
        return(encoderCalValues.sensorData.get(jackPos));
    }
    
    if(pGroupNum == EXIT_JACK_GROUP){
        //stored in forward order after entry jacks and unit sensors
        int firstExitJack = MAX_NUM_JACKS_ANY_GROUP + MAX_NUM_UNIT_SENSORS;
        int jackPos = firstExitJack + pSensorNum;
        return(encoderCalValues.sensorData.get(jackPos));
    }

    if(pGroupNum == UNIT_SENSOR_GROUP){
        //stored in forward order after entry jacks
        int firstUnitSensor = MAX_NUM_JACKS_ANY_GROUP;
        int jackPos = firstUnitSensor + pSensorNum;
        return(encoderCalValues.sensorData.get(jackPos));
    }

    return(null);
    
}//end of JackStandSetup::getSensorDatumFromSensorDataList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshSensorDatum
//
// Updates the displays for a sensor, transferring the data from pSensorDatum
// to the components of pSensorSetGUI.
//

private void refreshSensorDatum(SensorData pSensorDatum,
                         SensorSetGUI pSensorSetGUI, boolean pUpdateInputBoxes)
{

    String s;       

    //the last eye changed for the jack is prepended to the state

    if (pSensorDatum.lastEyeChanged == EYE_A) { s = "A - "; }
    else if (pSensorDatum.lastEyeChanged == EYE_B) { s = "B - "; }
    else{ s = ""; }

    //state is appended to last eye changed

    if (pSensorDatum.lastState == BLOCKED) { s += "blocked"; }
    else if (pSensorDatum.lastState == UNBLOCKED) { s += "unblocked"; }
    else{ s = "---"; }

    pSensorSetGUI.stateLbl.setText(s);

    if(pSensorDatum.lastEncoder1Cnt == Integer.MAX_VALUE) { s = "---";}
    else { s = "" + pSensorDatum.lastEncoder1Cnt; }

    pSensorSetGUI.encoder1Lbl.setText(s);

    if(pSensorDatum.lastEncoder2Cnt == Integer.MAX_VALUE) { s = "---";}
    else { s = "" + pSensorDatum.lastEncoder2Cnt; }

    pSensorSetGUI.encoder2Lbl.setText(s);

    if (pSensorDatum.direction == STOPPED) { s = "stopped"; }
    else if (pSensorDatum.direction == FWD) { s = "forward"; }
    else if (pSensorDatum.direction == REV) { s = "reverse"; }
    else{ s = "---"; }

    pSensorSetGUI.directionLbl.setText(s);

    if (pSensorDatum.countsPerInch == Double.MAX_VALUE) { s = "---"; }
    else{ s = new DecimalFormat("00.0").format(pSensorDatum.countsPerInch); }

     pSensorSetGUI.countsPerInchLbl.setText(s);
        
    if (pSensorDatum.percentChange == Double.MAX_VALUE) { s = "---"; }
    else{ s = new DecimalFormat("00.0").format(pSensorDatum.percentChange); }

     pSensorSetGUI.percentChangeLbl.setText(s);

    if (pUpdateInputBoxes){

        if (pSensorSetGUI.eyeADistTF != null){
            s = new DecimalFormat("#.####").format(
                                        pSensorDatum.getEyeADistToJackCenter());
            pSensorSetGUI.eyeADistTF.setText(s);
        }

        if (pSensorSetGUI.jackStandDistTF != null){
            s = new DecimalFormat("#.####").format(
                               pSensorDatum.getJackCenterDistToMeasurePoint());
            pSensorSetGUI.jackStandDistTF.setText(s);
        }

        if (pSensorSetGUI.eyeBDistTF != null){
            s = new DecimalFormat("#.####").format(
                                        pSensorDatum.getEyeBDistToJackCenter());
            pSensorSetGUI.eyeBDistTF.setText(s);
        }

    }
       
}//end of JackStandSetup::refreshSensorDatum
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::actionPerformed
//
// Catches action events from buttons, etc.
//
// Passes some of them on to the specified actionListener.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Display Monitor".equals(e.getActionCommand())) {
        resetDisplayPanel();
        return;
    }
    
    if ("Enable Eye Distance Editing".equals(e.getActionCommand())) {
        setEyeDistanceEditingEnabled();
        return;
    }

    if ("Text Field Changed".equals(e.getActionCommand())) {
        applyBtn.setEnabled(true);
        return;
    }

    if ("Apply".equals(e.getActionCommand())) {
        handleApplyButton();
        return;
    }
        
}//end of JackStandSetup::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::updateStatus
//
// Updates the display to show the current state of the I/O.
//

public void updateStatus()
{

    
}//end of JackStandSetup::updateStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::handleApplyButton
//
// Handles clicks of the Apply button.
//

private void handleApplyButton()
{

    applyBtn.setEnabled(false);
  
    handleNumJacksEntries();
    
    handleDistanceEntries();
        
    //transfer all data to the Hardware object
    actionListener.actionPerformed(
                  new ActionEvent(this, 1, "Transfer Jack Stand Setup Data"));
    
}//end of JackStandSetup::handleApplyButton
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::handleNumJacksEntries
//
// Parses the text in the text fields and transfers to the variables.
// If the value of either has changed, then the GUI is reset to reflect the
// change.
//

private void handleNumJacksEntries()
{

    boolean refreshGUI = false;
    int val;
    
    //handle entry jacks
    
    try{
        val = Integer.valueOf(numEntryJackStandsTF.getText().trim());
    }
    catch(NumberFormatException nfe){
        val = numEntryJackStands; //do not change value if invalid entry
    }

    if (val > MAX_NUM_JACKS_ANY_GROUP){ 
        val = MAX_NUM_JACKS_ANY_GROUP;
        numEntryJackStandsTF.setText("" + val);
    }

    //number of jacks changed so GUI must be updated
    if (val != numEntryJackStands){ refreshGUI = true; }
    
    numEntryJackStands = val;

    //handle exit jacks
    
    try{
        val = Integer.valueOf(numExitJackStandsTF.getText().trim());
    }
    catch(NumberFormatException nfe){
        val = numExitJackStands; //do not change value if invalid entry
    }

    if (val > MAX_NUM_JACKS_ANY_GROUP){ 
        val = MAX_NUM_JACKS_ANY_GROUP;
        numExitJackStandsTF.setText("" + val);
    }
    
    //number of jacks changed so GUI must be updated
    if (val != numExitJackStands){ refreshGUI = true; }
    
    numExitJackStands = val;
    
    //if number of entry or exit jacks changed, refresh the display
    if(refreshGUI){ resetDisplayPanel(); }
    
}//end of JackStandSetup::handleNumJacksEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::handleDistanceEntries
//
// Parses the text in the text fields and transfers to the variables.
// Reformats as a formatted double value and overwrites the entry in the
// text field.
//

private void handleDistanceEntries()
{

    for(SensorSetGUI sensorSetGUI : sensorSetGUIs){

        formatDistanceEntry(sensorSetGUI.eyeADistTF);
        formatDistanceEntry(sensorSetGUI.jackStandDistTF);        
        formatDistanceEntry(sensorSetGUI.eyeBDistTF);
          
    }

}//end of JackStandSetup::handleDistanceEntries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::formatDistanceEntry
//
// Converts the text in pTextField to a double, formats it, and overwrites
// the original text with the formatted version.
//

private void formatDistanceEntry(JTextField pTextField)
{
 
    if(pTextField == null){ return; }
    
    double val;

    try{
        val = Double.valueOf(pTextField.getText().trim());
    }
    catch(NumberFormatException nfe){
        val = 0.0;
    }

    pTextField.setText(new DecimalFormat("#.####").format(val));

}//end of JackStandSetup::formatDistanceEntry
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

static void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of JackStandSetup::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::setNumJackStands
//
// Sets the number of entry and exit jack stands. The GUI is reset to reflect
// the change.
//

public void setNumJackStands(int pNumEntryJackStands, int pNumExitJackStands)
{

    numEntryJackStands = pNumEntryJackStands;

    numExitJackStands = pNumExitJackStands;
    
}//end of JackStandSetup::setNumJackStands
//-----------------------------------------------------------------------------

}//end of class JackStandSetup
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SensorSetGUI
//
// This is a parent class which handles one or more sensors as a set.
// It handles GUI components for interfacing with the sensor values.
//

class SensorSetGUI{

    ActionListener actionListener;

    KeyAdapter keyAdapter;
    
    JPanel mainPanel;

    int groupNum;
    int setNum;
    String title;
    
    JLabel rowLabel;
    JTextField eyeADistTF;
    JTextField jackStandDistTF;
    JTextField eyeBDistTF;
    JLabel stateLbl;
    JLabel encoder1Lbl;
    JLabel encoder2Lbl;
    JLabel directionLbl;
    JLabel countsPerInchLbl;    
    JLabel percentChangeLbl;        
    
//-----------------------------------------------------------------------------
// SensorSetGUI::SensorSetGUI (constructor)
//
//

public SensorSetGUI(int pGroupNum, int pSetNum, JPanel pMainPanel,
                        KeyAdapter pKeyAdapter, ActionListener pActionListener)
{

    groupNum = pGroupNum; setNum = pSetNum; mainPanel = pMainPanel;
    keyAdapter = pKeyAdapter;

    actionListener = pActionListener;

}//end of SensorSetGUI::SensorSetGUI (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorSetGUI::init
//
// Initializes the object.
//

public void init(String pTitle, boolean pEnableEyeDistanceInputs,
                                                       boolean pDisplayMonitor)
{
            
    //if unit sensor, the set number is not used in the title
    //in such case, the title is expected to be unique without the number
    
    if (groupNum == JackStandSetup.UNIT_SENSOR_GROUP){
        title = pTitle;
    }else{
        title = pTitle + " " + (setNum + 1);
    }
    
}//end of SensorSetGUI::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorSetGUI::setEyeDistanceEditingEnabled
//
// Sets the enabled flag for all eye distance text fields.
//

void setEyeDistanceEditingEnabled(boolean pEnabled)
{

    if(eyeADistTF != null) { eyeADistTF.setEnabled(pEnabled); }
    if(eyeBDistTF != null) { eyeBDistTF.setEnabled(pEnabled); }
        
}//end of SensorSetGUI::setEyeDistanceEditingEnabled
//-----------------------------------------------------------------------------

}//end of class SensorSetGUI
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class JackStandGUI
//
// This class a set of labels, inputs, and displays for a single jack stand.
//

class JackStandGUI extends SensorSetGUI{
    
//-----------------------------------------------------------------------------
// JackStandGUI::JackStandGUI (constructor)
//
//

public JackStandGUI(int pGroupNum, int pJackNum, JPanel pMainPanel,
                        KeyAdapter pKeyAdapter, ActionListener pActionListener)
{

    super(pGroupNum, pJackNum, pMainPanel, pKeyAdapter, pActionListener);
        
}//end of JackStandGUI::JackStandGUI (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandGUI::init
//
// Initializes the object.
//

@Override
public void init(String pTitle, boolean pEnableEyeDistanceInputs,
                                                       boolean pDisplayMonitor)
{

    super.init(pTitle, pEnableEyeDistanceInputs, pDisplayMonitor);
    
    rowLabel = new JLabel(title);
    eyeADistTF = new JTextField(4); eyeADistTF.addKeyListener(keyAdapter);
    jackStandDistTF = new JTextField(4); 
    jackStandDistTF.addKeyListener(keyAdapter);
    eyeBDistTF = new JTextField(4); eyeBDistTF.addKeyListener(keyAdapter);
    stateLbl = new JLabel("?");
    encoder1Lbl = new JLabel("?");
    encoder2Lbl = new JLabel("?");
    directionLbl = new JLabel("?");
    countsPerInchLbl = new JLabel("?");
    percentChangeLbl = new JLabel("?");
    
    setEyeDistanceEditingEnabled(pEnableEyeDistanceInputs);
    
    mainPanel.add(rowLabel);
    mainPanel.add(eyeADistTF);
    mainPanel.add(jackStandDistTF);
    mainPanel.add(eyeBDistTF);

    //these only visible if Display Monitor checkbox is checked
    if (pDisplayMonitor){
    
        mainPanel.add(stateLbl);
        mainPanel.add(encoder1Lbl);
        mainPanel.add(encoder2Lbl);    
        mainPanel.add(directionLbl);
        mainPanel.add(countsPerInchLbl);        
        mainPanel.add(percentChangeLbl);        
    }
            
}//end of JackStandGUI::init
//-----------------------------------------------------------------------------

}//end of class JackStandGUI
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SensorGUI
//
// This class a set of labels, inputs, and displays for a single jack stand.
//

class SensorGUI extends SensorSetGUI{
    
//-----------------------------------------------------------------------------
// SensorGUI::SensorGUI (constructor)
//
//

public SensorGUI(int pGroupNum, int pSensorNum, JPanel pMainPanel,
                        KeyAdapter pKeyAdapter, ActionListener pActionListener)
{

    super(pGroupNum, pSensorNum, pMainPanel, pKeyAdapter, pActionListener);
        
}//end of SensorGUI::SensorGUI (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SensorGUI::init
//
// Initializes the object.
//

@Override
public void init(String pTitle, boolean pEnableEyeDistanceInputs,
                                                       boolean pDisplayMonitor)
{
    
    super.init(pTitle, pEnableEyeDistanceInputs, pDisplayMonitor);    

    rowLabel = new JLabel(title);
    stateLbl = new JLabel("?");
    encoder1Lbl = new JLabel("?");
    encoder2Lbl = new JLabel("?");
    directionLbl = new JLabel("?");
    countsPerInchLbl = new JLabel("?");
    percentChangeLbl = new JLabel("?");
    
    setEyeDistanceEditingEnabled(pEnableEyeDistanceInputs);
    
    mainPanel.add(rowLabel);
    mainPanel.add(new JLabel(""));
    mainPanel.add(new JLabel(""));
    mainPanel.add(new JLabel(""));

    //these only visible if Display Monitor checkbox is checked
    if (pDisplayMonitor){
    
        mainPanel.add(stateLbl);
        mainPanel.add(encoder1Lbl);
        mainPanel.add(encoder2Lbl);    
        mainPanel.add(directionLbl);
        mainPanel.add(countsPerInchLbl);
        mainPanel.add(percentChangeLbl);        
    }
            
}//end of SensorGUI::init
//-----------------------------------------------------------------------------

}//end of class SensorGUI
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

