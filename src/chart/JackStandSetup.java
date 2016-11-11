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
    
    private static int MAX_NUM_UNIT_SENSORS;    
    private static int NUM_UNIT_SENSORS;
    private static int MAX_NUM_JACKS_ON_EITHER_END;
    private static int TOTAL_NUM_SENSORS;

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
    MAX_NUM_JACKS_ON_EITHER_END = EncoderCalValues.MAX_NUM_JACKS_ON_EITHER_END;
    TOTAL_NUM_SENSORS = EncoderCalValues.TOTAL_NUM_SENSORS;

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
    
    int numGridRows = numEntryJackStands + numExitJackStands + 2 + 4;
    
    int numGridCols;
    
    if(displayMonitorCB.isSelected()){
        numGridCols = 9;
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
        
        set = new JackStandGUI(i, pPanel, keyAdapter, this);
        
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
        
        set = new JackStandGUI(i, pPanel, keyAdapter, this);
        
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
    
    set = new SensorGUI(-1, pPanel, keyAdapter, this);

    set.init("Entry Sensor", 
     enableEyeDistanceInputsCB.isSelected(), displayMonitorCB.isSelected());

    sensorSetGUIs.add(set);
                
    set = new SensorGUI(-2, pPanel, keyAdapter, this);

    set.init("Exit Sensor", 
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
 
    boolean refreshGUI = false;

    if (pEncoderCalValues.numEntryJackStands > MAX_NUM_JACKS_ON_EITHER_END){
        pEncoderCalValues.numEntryJackStands = MAX_NUM_JACKS_ON_EITHER_END;
    }
    
    if (pEncoderCalValues.numEntryJackStands != numEntryJackStands){
        refreshGUI = true;
        numEntryJackStands = pEncoderCalValues.numEntryJackStands;
    }

    if (pEncoderCalValues.numExitJackStands > MAX_NUM_JACKS_ON_EITHER_END){
        pEncoderCalValues.numExitJackStands = MAX_NUM_JACKS_ON_EITHER_END;
    }
        
    if(pEncoderCalValues.numExitJackStands != numExitJackStands){
        refreshGUI = true;
        numExitJackStands = pEncoderCalValues.numExitJackStands;
    }
    
    if(refreshGUI){ resetDisplayPanel(); }

    //if the GUI was changed or data in pEncoderCalValues has changed, then
    //refresh all labels with data
    
    pEncoderCalValues.sensorTransitionDataChanged = true; //debug mks -- remove this
    
    if (refreshGUI || pEncoderCalValues.sensorTransitionDataChanged){
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
    parseSensorInputs();
    
    return(pEncoderCalValues);
    
}//end of JackStandSetup::getEncoderCalValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseSensorInputs
//
// Transfers sensor data from all input boxes to pEncoderCalValues
//

private void parseSensorInputs()
{
    ArrayList<SensorData> sensorData;
    
    sensorData = encoderCalValues.sensorData;
        
    parseEntryJackInputs(sensorData);
    
    parseUnitSensorInputs(sensorData);    
    
    parseExitJackInputs(sensorData);
    
}//end of JackStandSetup::parseSensorInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseEntryJackInputs
//
// Transfers entry jack stand data from all input boxes to pEncoderCalValues.
//
// See refreshEntryJackDisplays for details on how the jacks and sensors are
// ordered in the lists.
//

private void parseEntryJackInputs(ArrayList<SensorData> pSensorData)
{
    
    int j=9; //first entry jack in sensorData list
    
    for (int i=numEntryJackStands-1; i>=0; i--){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);
        //zeroeth entry is at the end of the first group (entry jacks)
        SensorData sensorDatum = pSensorData.get(j--);
        
        parseSensorDatum(sensorDatum, setGUI);
         
    }

}//end of JackStandSetup::parseEntryJackInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseExitJackInputs
//
// Transfers exit jack stand data from all input boxes to pEncoderCalValues.
//
// See refreshExitJackDisplays for details on how the jacks and sensors are
// ordered in the lists.
//

private void parseExitJackInputs(ArrayList<SensorData> pSensorData)
{
    
    int j=12; //first exit jack in sensorData list
    
    //first exit jack is past all entry jacks and past the two unit sensors
    //subtract one to use as zero-based index
    int firstExitJackPos = numEntryJackStands + 3 - 1;
    int lastExitJackPos = firstExitJackPos + numExitJackStands;
    
    for (int i=firstExitJackPos; i<lastExitJackPos; i++){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);
        //zeroeth entry is at the end of the first group (entry jacks)
        SensorData sensorDatum = pSensorData.get(j++);

        parseSensorDatum(sensorDatum, setGUI);
         
    }

}//end of JackStandSetup::parseExitJackInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::parseUnitSensorInputs
//
// Transfers unit sensor data from all input boxes to pEncoderCalValues.
//
// See refreshUnitSensorDisplays for details on how the jacks and sensors are
// ordered in the lists.
//

private void parseUnitSensorInputs(ArrayList<SensorData> pSensorData)
{
    
    int j=10; //first unit sensor in sensorData list
 
    //unit sensor starts after entry jacks, subtract one to zero-base index
    int firstUnitSensorPos = numEntryJackStands + 1 - 1;
    int lastUnitSensorPos = firstUnitSensorPos + 1;
         
    for (int i=firstUnitSensorPos; i<=lastUnitSensorPos; i++){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);

        SensorData sensorDatum = pSensorData.get(j++);
        
        parseSensorDatum(sensorDatum, setGUI);
         
    }

}//end of JackStandSetup::parseUnitSensorInputs
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

    pSensorDatum.eyeADistToJackCenter = 
                            parseDoubleFromTextField(pSensorSetGUI.eyeADistTF);

    pSensorDatum.eyeBDistToJackCenter = 
                            parseDoubleFromTextField(pSensorSetGUI.eyeBDistTF);
    
    pSensorDatum.jackCenterDistToEye1 = 
                       parseDoubleFromTextField(pSensorSetGUI.jackStandDistTF);
       
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
    ArrayList<SensorData> sensorData;
    
    sensorData = encoderCalValues.sensorData;
        
    refreshEntryJackDisplays(sensorData, pUpdateInputBoxes);
    
    refreshUnitSensorDisplays(sensorData, pUpdateInputBoxes);    
    
    refreshExitJackDisplays(sensorData, pUpdateInputBoxes);
    
}//end of JackStandSetup::refreshSensorDisplays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshEntryJackDisplays
//
// Updates all displays related to the entry jacks from encoderCalValues.
//
// Input boxes are only updated if pUpdateInputBoxes is true. See notes for
// refreshAllGUI for more details.
//
// the entry jack GUIs are in their list in reverse order
// the entry jack SensorSets are in list in reverse order
// starting from the zeroeth jack at the bottom of the first list
// and near the middle of the second list, data is copied from one to the
// other
// the SensorData list contains all the entry jacks, the unit sensors, and
// the exit jacks; the unit sensors are at the middle of the list
// the SensorData list may contain up to 10 entry and 10 exit jacks; if
// the display is set to a lesser number, the extra entries will be ignored
//

private void refreshEntryJackDisplays(ArrayList<SensorData> pSensorData,
                                                    boolean pUpdateInputBoxes)
{
    
    int j=9; //first entry jack in sensorData list
    
    for (int i=numEntryJackStands-1; i>=0; i--){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);
        //zeroeth entry is at the end of the first group (entry jacks)
        SensorData sensorDatum = pSensorData.get(j--);
        
        refreshSensorDatum(sensorDatum, setGUI, pUpdateInputBoxes);
         
    }

}//end of JackStandSetup::refreshEntryJackDisplays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshExitJackDisplays
//
// Updates all displays related to the exit jacks from encoderCalValues.
//
// Input boxes are only updated if pUpdateInputBoxes is true. See notes for
// refreshAllGUI for more details.
//
// the exit jack GUIs are in their list in forward order
// the exit jack SensorSets are in list in forward order
// starting from the zeroeth jack at near the middle of the first list
// and near the middle of the second list, data is copied from one to the
// other
// the SensorData list contains all the entry jacks, the unit sensors, and
// the exit jacks; the unit sensors are at the middle of the list
// the SensorData list may contain up to 10 entry and 10 exit jacks; if
// the display is set to a lesser number, the extra entries will be ignored
//

private void refreshExitJackDisplays(ArrayList<SensorData> pSensorData,
                                                     boolean pUpdateInputBoxes)
{
    
    int j=12; //first exit jack in sensorData list
    
    //first exit jack is past all entry jacks and past the two unit sensors
    //subtract one to use as zero-based index
    int firstExitJackPos = numEntryJackStands + 3 - 1;
    int lastExitJackPos = firstExitJackPos + numExitJackStands;
    
    for (int i=firstExitJackPos; i<lastExitJackPos; i++){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);
        //zeroeth entry is at the end of the first group (entry jacks)
        SensorData sensorDatum = pSensorData.get(j++);

        refreshSensorDatum(sensorDatum, setGUI, pUpdateInputBoxes);
         
    }

}//end of JackStandSetup::refreshExitJackDisplays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JackStandSetup::refreshUnitSensorDisplays
//
// Updates all displays related to the unit sensors from encoderCalValues.
//
// Input boxes are only updated if pUpdateInputBoxes is true. See notes for
// refreshAllGUI for more details.
//
// the SensorData list contains all the entry jacks, the unit sensors, and
// the exit jacks; the unit sensors are at the middle of the list
// the SensorData

private void refreshUnitSensorDisplays(ArrayList<SensorData> pSensorData,
                                                     boolean pUpdateInputBoxes)
{
    
    int j=10; //first unit sensor in sensorData list
 
    //unit sensor starts after entry jacks, subtract one to zero-base index
    int firstUnitSensorPos = numEntryJackStands + 1 - 1;
    int lastUnitSensorPos = firstUnitSensorPos + 1;
     
    
    for (int i=firstUnitSensorPos; i<=lastUnitSensorPos; i++){
        
        SensorSetGUI setGUI = sensorSetGUIs.get(i);

        SensorData sensorDatum = pSensorData.get(j++);
        
        refreshSensorDatum(sensorDatum, setGUI, pUpdateInputBoxes);        
         
    }

}//end of JackStandSetup::refreshUnitSensorDisplays
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

    if (pSensorDatum.sensorState == BLOCKED) { s += "blocked"; }
    else if (pSensorDatum.sensorState == UNBLOCKED) { s += "unblocked"; }
    else{ s = "---"; }

    pSensorSetGUI.stateLbl.setText(s);

    if(pSensorDatum.encoder1Cnt == Integer.MAX_VALUE) { s = "---";}
    else { s = "" + pSensorDatum.encoder1Cnt; }

    pSensorSetGUI.encoder1Lbl.setText(s);

    if(pSensorDatum.encoder2Cnt == Integer.MAX_VALUE) { s = "---";}
    else { s = "" + pSensorDatum.encoder2Cnt; }

    pSensorSetGUI.encoder2Lbl.setText(s);

    if (pSensorDatum.direction == STOPPED) { s = "stopped"; }
    else if (pSensorDatum.direction == FWD) { s = "forward"; }
    else if (pSensorDatum.direction == REV) { s = "reverse"; }
    else{ s = "---"; }

    pSensorSetGUI.directionLbl.setText(s);

    if (pSensorDatum.percentChange == Double.MAX_VALUE) { s = "---"; }
    else{ s = new DecimalFormat("00.0").format(pSensorDatum.percentChange); }

     pSensorSetGUI.percentChangeLbl.setText(s);

    if (pUpdateInputBoxes){

        if (pSensorSetGUI.eyeADistTF != null){
            s = new DecimalFormat("#.####").format(
                                        pSensorDatum.eyeADistToJackCenter);
            pSensorSetGUI.eyeADistTF.setText(s);
        }

        if (pSensorSetGUI.jackStandDistTF != null){
            s = new DecimalFormat("#.####").format(
                                        pSensorDatum.jackCenterDistToEye1);
            pSensorSetGUI.jackStandDistTF.setText(s);
        }

        if (pSensorSetGUI.eyeBDistTF != null){
            s = new DecimalFormat("#.####").format(
                                        pSensorDatum.eyeBDistToJackCenter);
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

    if (val > MAX_NUM_JACKS_ON_EITHER_END){ 
        val = MAX_NUM_JACKS_ON_EITHER_END;
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

    if (val > MAX_NUM_JACKS_ON_EITHER_END){ 
        val = MAX_NUM_JACKS_ON_EITHER_END;
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
    JLabel percentChangeLbl;        
    
//-----------------------------------------------------------------------------
// SensorSetGUI::SensorSetGUI (constructor)
//
//

public SensorSetGUI(
                int pSetNum, JPanel pMainPanel, KeyAdapter pKeyAdapter,
                                                ActionListener pActionListener)
{

    setNum = pSetNum; mainPanel = pMainPanel; keyAdapter = pKeyAdapter;

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
            
    //if the set number is negative, it is not used in the title
    //in such case, the title is expected to be unique without the number
    
    if (setNum >= 0){
        title = pTitle + " " + (setNum + 1);
    }else{
        title = pTitle;
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

public JackStandGUI(int pJackNum, JPanel pMainPanel,KeyAdapter pKeyAdapter,
                                                ActionListener pActionListener)
{

    super(pJackNum, pMainPanel, pKeyAdapter, pActionListener);
        
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

public SensorGUI(int pSensorNum, JPanel pMainPanel, KeyAdapter pKeyAdapter,
                                                ActionListener pActionListener)
{

    super(pSensorNum, pMainPanel, pKeyAdapter, pActionListener);
        
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
        mainPanel.add(percentChangeLbl);        
    }
            
}//end of SensorGUI::init
//-----------------------------------------------------------------------------

}//end of class SensorGUI
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

