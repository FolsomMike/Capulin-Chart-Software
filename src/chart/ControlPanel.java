/******************************************************************************
* Title: ControlPanel.java
* Author: Mike Schoonover
* Date: 3/23/08
*
* Purpose:
*
* This class displays a group of controls contained on a JPanel.
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
import java.io.*;
import javax.swing.border.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;

import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.ImageIcon;

import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.mswing.MFloatSpinner;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlPanel
//
// This class creates a panel for controls.
//

public class ControlPanel extends JPanel 
                       implements ActionListener, ChangeListener, ItemListener{


Globals globals;
IniFile configFile;
Hardware hardware;
JFrame mainFrame;
ActionListener parentActionListener;
String currentJobPrimaryPath, currentJobBackupPath;
MessageLink mechSimulator;

ModePanel modePanel;
StatusPanel statusPanel;
InfoPanel infoPanel;
ScanSpeedPanel scanSpeedPanel;
DemoPanel demoPanel;
ManualControlPanel manualControlPanel;
String jobName;
boolean simulateMechanical;
boolean timerDrivenTracking;
int previousMode = -1;
ImageIcon warningSymbol;
int panelWidth, panelHeight;

public boolean calMode = false;
int nextPieceNumber, nextCalPieceNumber;

//-----------------------------------------------------------------------------
// ControlPanel::ControlPanel (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//
  
public ControlPanel(IniFile pConfigFile, String pCurrentJobPrimaryPath,
    String pCurrentJobBackupPath, Hardware pHardware, JFrame pFrame,
     ActionListener pParentActionListener, String pJobName, Globals pGlobals,
                                                    MessageLink pMechSimulator)
{

configFile = pConfigFile; hardware = pHardware; mainFrame = pFrame;
parentActionListener = pParentActionListener;
currentJobPrimaryPath = pCurrentJobPrimaryPath;
currentJobBackupPath = pCurrentJobBackupPath;
jobName = pJobName;
globals = pGlobals;
mechSimulator = pMechSimulator;
simulateMechanical = globals.simulateMechanical;
timerDrivenTracking = globals.timerDrivenTracking;

//set up the main panel - this panel does nothing more than provide a title
//border and a spacing border
setOpaque(true);

//read the configuration file and create/setup the charting/control elements
configure(configFile);

//load settings such as the next inspection and cal piece numbers
loadSettings();

//set the piece number editor to the value loaded from disk
//cast to a double or the float spinner will switch its internal value to an
//integer which will cause problems later when using getIntValue()
statusPanel.pieceNumberEditor.setValue((double)nextPieceNumber);

}//end of ControlPanel::ControlPanel (constructor)
//-----------------------------------------------------------------------------    

//-----------------------------------------------------------------------------
// ControlPanel::refreshControls
//
// Updates various controls with the values from the matching variables.
//

public void refreshControls()
{

//set the scan speed editor to the value loaded from disk
//cast to a double or the float spinner will switch its internal value to an
//integer which will cause problems later when using getIntValue()
scanSpeedPanel.scanSpeedEditor.setValue((double)globals.scanSpeed);

}//end of ControlPanel::refreshControls
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//

private void configure(IniFile pConfigFile)
{

panelWidth = pConfigFile.readInt("Control Panel", "Width", 1200);
panelHeight = pConfigFile.readInt("Control Panel", "Height", 50);

setMinimumSize(new Dimension(panelWidth, panelHeight));
setPreferredSize(new Dimension(panelWidth, panelHeight));
setMaximumSize(new Dimension(panelWidth, panelHeight));

//NOTE: You must use forward slashes in the path names for the resource
//loader to find the image files in the JAR package.

warningSymbol = createImageIcon("images/windows-warning.gif");

setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

add(modePanel = new ModePanel(this));
modePanel.init();
add(statusPanel = new StatusPanel(globals, this, this));
add(infoPanel = new InfoPanel(this, jobName));
add(scanSpeedPanel = new ScanSpeedPanel(globals, this, this));
if (simulateMechanical) add(demoPanel = new DemoPanel(mechSimulator));
if (demoPanel != null) demoPanel.init();
add(Box.createHorizontalGlue()); //force manual control to the right side
if (timerDrivenTracking){ 
    add(manualControlPanel = new ManualControlPanel(this, this, warningSymbol));
    manualControlPanel.init();
    }

}//end of ControlPanel::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{ 

//force garbage collection before beginning any time sensitive tasks
System.gc();    
    
if ("Scan".equals(e.getActionCommand())) {

    modePanel.scanButton.setEnabled(false);
    modePanel.inspectButton.setEnabled(false);
    
    //start the scan mode
    hardware.setMode(Hardware.SCAN);

    previousMode = -1;
    
    }

if ("Inspect".equals(e.getActionCommand())) {

    //reset everything to inspect the next piece - do this before entering
    //the inspect mode
    prepareForNextPiece();

    modePanel.scanButton.setEnabled(false);
    modePanel.inspectButton.setEnabled(false);
  
    //allow user to control inspection if manual tracking is enabled
    if (timerDrivenTracking) {
        manualControlPanel.pauseResumeButton.setEnabled(true);
        manualControlPanel.nextPieceButton.setEnabled(true);

        //force the paused mode so the user can start inspection at will
        manualControlPanel.pauseResumeButton.setText("Resume");
        hardware.setMode(Hardware.PAUSED); //pause the traces
        
        //set previousMode to desired inspection mode so when the user
        //uses the "Resume" button, inspection will be resumed
        previousMode = Hardware.INSPECT_WITH_TIMER_TRACKING;

        }
    else{
        //start inspection mode using encoder inputs to track position
        hardware.setMode(Hardware.INSPECT);
        previousMode = -1;
        }

    }// if ("Inspect".equals(e.getActionCommand()))

if ("Stop".equals(e.getActionCommand())) {

    previousMode = hardware.getMode(); //store the current mode

    hardware.setMode(Hardware.STOPPED); //stop the scan mode

    modePanel.scanButton.setEnabled(true);
    modePanel.inspectButton.setEnabled(true);

    //disallow user controls if inspection if manual tracking is enabled
    if (timerDrivenTracking) {
        manualControlPanel.pauseResumeButton.setText("Pause");
        manualControlPanel.pauseResumeButton.setEnabled(false);
        manualControlPanel.nextPieceButton.setEnabled(false);
        }

    if (previousMode != Hardware.STOPPED  && previousMode != Hardware.SCAN){

        finishPiece();

        }// if (hardware.getMode() != Hardware.STOPPED...

    previousMode = -1;

    }// if ("Stop".equals(e.getActionCommand()))

if ("Pause or Resume".equals(e.getActionCommand())) {

    JButton b = manualControlPanel.pauseResumeButton;

    if (b.getText().equals("Pause")){
        b.setText("Resume");
        previousMode = hardware.getMode(); //store the current mode
        hardware.setMode(Hardware.PAUSED); //pause the traces
        //don't set previousMode to -1 here ~ it's used to resume
        }
    else{
        b.setText("Pause");
        //resume whatever mode was in effect when paused
        hardware.setMode(previousMode);
        previousMode = -1;
        }

    }//if ("Pause or Resume".equals(e.getActionCommand()))

if ("Next Run".equals(e.getActionCommand())) {

    //if the screen is paused, then don't update previousMode as it will
    //hold the proper mode ~ if it is -1, then store the mode for later use
    if (previousMode == -1) previousMode = hardware.getMode();

    hardware.setMode(Hardware.STOPPED); //stop the traces
    finishPiece();
    prepareForNextPiece();

    //force the paused mode, leaving the previousMode value set to the last
    //inspection mode value so that when the user hits "Resume" the inspection
    //mode will be entered again
    manualControlPanel.pauseResumeButton.setText("Resume");
    hardware.setMode(Hardware.PAUSED); //pause the traces

    }// if ("Next Run".equals(e.getActionCommand()))

}//end of ControlPanel::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::itemStateChanged
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

//NOTE: ItemEvent does not have an action command, so must detect another way.

if (e.getItemSelectable() == statusPanel.calModeCheckBox){

    calMode = statusPanel.calModeCheckBox.isSelected();

    if (calMode){

        //copy the current number in the control to the matching variable
        nextPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
        //copy the opposite variable to the control (must cast to double)
        statusPanel.pieceNumberEditor.setValue((double)nextCalPieceNumber);

        if (timerDrivenTracking)
            manualControlPanel.calModeWarning.setVisible(true);
           //add an else here - add warning label to info panel, set it
           //visible if there is no manualControlPanel

        }
    else{

        //copy the current number in the control to the matching variable
        nextCalPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
        //copy the opposite variable to the control (must cast to double)
        statusPanel.pieceNumberEditor.setValue((double)nextPieceNumber);

        if (timerDrivenTracking)
            manualControlPanel.calModeWarning.setVisible(false);
        //add an else here - add warning label to info panel, set it
        //visible if there is no manualControlPanel

        }//else of if (calMode)
                
    }// if (e.getItemSelectable() == statusPanel.calModeCheckBox)

}//end of ControlPanel::itemStateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::stateChanged
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

if (e.getSource() == statusPanel.pieceNumberEditor){

    calMode = statusPanel.calModeCheckBox.isSelected();

    if (calMode){
        //copy the current number in the control to the matching variable
        nextCalPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
        }
    else{
        //copy the current number in the control to the matching variable
        nextPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
        }//else of if (calMode)

    }// if (e.getSource() == statusPanel.pieceNumberEditor)

if (e.getSource() == scanSpeedPanel.scanSpeedEditor){

    globals.scanSpeed = scanSpeedPanel.scanSpeedEditor.getIntValue();

    }// if (e.getSource() == scanSpeedPanel.scanSpeedEditor)

}//end of ControlPanel::stateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::finishPiece
//
// Saves the data for the piece just inspected and increments the piece number
// for the next run.
//

void finishPiece()
{

//call the parent to prepare for inspecting the next piece

parentActionListener.actionPerformed(
                            new ActionEvent(this, 1, "Process finished Piece"));

}//end of ControlPanel::finishPiece
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::prepareForNextPiece
//
// Prepares the system to inspect the next piece.
//

void prepareForNextPiece()
{

//call the parent to prepare for inspecting the next piece

parentActionListener.actionPerformed(
                            new ActionEvent(this, 1, "Prepare for Next Piece"));

}//end of ControlPanel::prepareForNextPiece
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::incrementPieceNumber
//
// Increments the piece number or cal piece number depending on the value of
// calMode.  The spinner control is updated with the new number.
//

public void incrementPieceNumber()
{

//depending on the mode, increment the appropriate variable and control
if (!statusPanel.calModeCheckBox.isSelected()){
    //get the latest value in the control in case user changed it
    nextPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
    nextPieceNumber++; //increment
    //stuff new value back to the control
    //cast to a double or the float spinner will switch its internal value to an
    //integer which will cause problems later when using getIntValue()
    statusPanel.pieceNumberEditor.setValue((double)nextPieceNumber);
    }
else{
    //get the latest value in the control in case user changed it
    nextCalPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
    nextCalPieceNumber++; //increment
    //stuff new value back to the control
    //cast to a double or the float spinner will switch its internal value to an
    //integer which will cause problems later when using getIntValue()
    statusPanel.pieceNumberEditor.setValue((double)nextCalPieceNumber);
    }

saveSettings(); //save the numbers after each change in case of crash

}//end of ControlPanel::incrementPieceNumber
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::addButtonToJPanel (static)
//
// Adds a button to a JPanel with associated settings and values.
//

static public JButton addButtonToJPanel(JPanel pPanel, String pLabel, 
        String pActionCommand, ActionListener pActionListener, String pToolTip)
{

JButton button = new JButton(pLabel);
button.setActionCommand(pActionCommand);
button.addActionListener(pActionListener);
button.setToolTipText(pToolTip);
pPanel.add(button);
    
return (button);    

}//end of ControlPanel::addButtonToJPanel (static)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::loadSettings
//
// Saves settings such as the next piece and calibration piece numbers.
// These values often changed as part of normal operation.
//

private void loadSettings()
{

IniFile settingsFile = null;

//if the ini file cannot be opened and loaded, exit without action
try {
    settingsFile = new IniFile(currentJobPrimaryPath + "02 - "
                                + jobName + " Piece Number File.ini");
    }
    catch(IOException e){return;}

nextPieceNumber = settingsFile.readInt(
                                "General", "Next Inspection Piece Number", 1);

if (nextPieceNumber < 1) nextPieceNumber = 1;

nextCalPieceNumber = settingsFile.readInt(
                               "General", "Next Calibration Piece Number", 1);

if (nextCalPieceNumber < 1) nextCalPieceNumber = 1;

}//end of ControlPanel::loadSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::saveSettings
//
// Saves settings such as the next piece and calibration piece numbers.
// The file is saved to both the primary and secondary job folders.
// These values often changed as part of normal operation.
//

public void saveSettings()
{

//copy the control value to the appropriate variable depending on the cal mode
//setting - this makes sure the value gets saved in case it was changed
//manually by the user

if (statusPanel.calModeCheckBox.isSelected())
    nextCalPieceNumber = statusPanel.pieceNumberEditor.getIntValue();
else
    nextPieceNumber = statusPanel.pieceNumberEditor.getIntValue();

saveSettingsHelper(currentJobPrimaryPath, jobName,
                                        nextPieceNumber, nextCalPieceNumber);
saveSettingsHelper(currentJobBackupPath, jobName,
                                        nextPieceNumber, nextCalPieceNumber);
        
}//end of ControlPanel::saveSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::saveSettingsHelper
//
// Saves settings such as the next piece and calibration piece numbers.
// The file is saved to the folder specified by pJobPath using the name
// pJobName.
//
// These settings often change as part of normal operation and so are saved
// separately from other settings which are rarely changed.
//

static void saveSettingsHelper(String pJobPath, String pJobName,
                                int pNextPieceNumber, int pNextCalPieceNumber)
{

IniFile settingsFile = null;

//if the ini file cannot be opened and loaded, exit without action
try {
    settingsFile = new IniFile(pJobPath + "02 - "
                                    + pJobName + " Piece Number File.ini");
    }
    catch(IOException e){return;}

settingsFile.writeInt(
                   "General", "Next Inspection Piece Number", pNextPieceNumber);

settingsFile.writeInt(
               "General", "Next Calibration Piece Number", pNextCalPieceNumber);

settingsFile.save(); //force save

}//end of ControlPanel::saveSettingsHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ControlPanel::createImageIcon
//
// Returns an ImageIcon, or null if the path was invalid.
//
// ***************************************************************************
// NOTE: You must use forward slashes in the path names for the resource
// loader to find the image files in the JAR package.
// ***************************************************************************
//

protected static ImageIcon createImageIcon(String path)
{

//have to use the ControlPanel class since it is the one which matches the
//filename holding this class

java.net.URL imgURL = ControlPanel.class.getResource(path);

if (imgURL != null) {
    return new ImageIcon(imgURL);
    }
else {return null;}

}//end of ControlPanel::createImageIcon
//-----------------------------------------------------------------------------

}//end of class ControlPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ModePanel
//
// This class creates and controls a panel with buttons for the 
// Inspect/Scan/Stop functions.
//

class ModePanel extends JPanel{

public TitledBorder titledBorder;    
JButton inspectButton, scanButton, stopButton;
ActionListener actionListener;
    
//-----------------------------------------------------------------------------
// ModePanel::ModePanel (constructor)
//
//
  
public ModePanel(ActionListener pActionListener)
{

actionListener = pActionListener;
        
}//end of ModePanel::ModePanel (constructor)
//-----------------------------------------------------------------------------    

//-----------------------------------------------------------------------------
// ModePanel::init
//
// Initializes the object.
//
  
public void init()
{

setBorder(titledBorder = BorderFactory.createTitledBorder("Mode"));    

setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

inspectButton = ControlPanel.addButtonToJPanel(this, "Inspect", 
                            "Inspect", actionListener, "Prepares to inspect.");

scanButton = ControlPanel.addButtonToJPanel(this, "Scan", 
        "Scan", actionListener, "Begins scanning data - use for calibration.");

stopButton = ControlPanel.addButtonToJPanel(this, "Stop", 
                     "Stop", actionListener, "Places system in standby mode.");
    
}//end of ModePanel::init
//-----------------------------------------------------------------------------    

}//end of class ModePanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class StatusPanel
//
// This class creates and controls a panel displaying status information.
//

class StatusPanel extends JPanel{

ChangeListener changeListener;    
ItemListener itemListener;

public TitledBorder titledBorder;    

MFloatSpinner pieceNumberEditor;
JLabel speedLabel, speedValue;
JLabel rpmLabel, rpmValue;

JCheckBox calModeCheckBox;

//-----------------------------------------------------------------------------
// StatusPanel::StatusPanel (constructor)
//
//
  
public StatusPanel(Globals pGlobals, ChangeListener pChangeListener,
                                                    ItemListener pItemListener)
{

changeListener = pChangeListener; itemListener = pItemListener;
    
setBorder(titledBorder = BorderFactory.createTitledBorder("Status"));
setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

pieceNumberEditor = new MFloatSpinner(1, 1, 100000, 1, "##0", 60, -1);
pieceNumberEditor.addChangeListener(changeListener);
pieceNumberEditor.setToolTipText("The next "
                            + pGlobals.pieceDescriptionLC + " number.");
add(pieceNumberEditor);

calModeCheckBox = new JCheckBox("Cal Mode");
calModeCheckBox.setSelected(false);
calModeCheckBox.setActionCommand("Calibration Mode");
calModeCheckBox.addItemListener(itemListener);
calModeCheckBox.setToolTipText(
            "Check this box to run and save calibration " +
                                            pGlobals.pieceDescriptionPluralLC);
add(calModeCheckBox);

speedLabel = new JLabel(" Speed: ");
add(speedLabel);
speedValue = new JLabel("000");
add(speedValue);

rpmLabel = new JLabel(" RPM: ");
add(rpmLabel);
rpmValue = new JLabel("000");
add(rpmValue);

}//end of StatusPanel::StatusPanel (constructor)
//-----------------------------------------------------------------------------    

}//end of class StatusPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class InfoPanel
//
// This class creates and controls a panel displaying misc information.
//

class InfoPanel extends JPanel{

JPanel parent;    
public TitledBorder titledBorder;    

JLabel jobLabel, jobValue;

//-----------------------------------------------------------------------------
// InfoPanel::InfoPanel (constructor)
//
//
  
public InfoPanel(JPanel pParent, String pCurrentWorkOrder)
{

parent = pParent;
    
setBorder(titledBorder = BorderFactory.createTitledBorder("Info"));
setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

jobLabel = new JLabel(" Job #: ");
add(jobLabel);
jobValue = new JLabel(pCurrentWorkOrder);
add(jobValue);

}//end of InfoPanel::InfoPanel (constructor)
//-----------------------------------------------------------------------------    

}//end of class InfoPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ScanSpeedPanel
//
// This class creates and controls a panel allowing adjustment of the chart scan
// speed.
//

class ScanSpeedPanel extends JPanel{

JPanel parent;
public TitledBorder titledBorder;
MFloatSpinner scanSpeedEditor;

//-----------------------------------------------------------------------------
// ScanSpeedPanel::ScanSpeedPanel (constructor)
//

public ScanSpeedPanel(Globals pGlobals, JPanel pParent,
                                                ChangeListener pChangeListener)
{

parent = pParent;

setBorder(titledBorder = BorderFactory.createTitledBorder("Scan Speed"));
setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

scanSpeedEditor = new MFloatSpinner(1, 1, 10, 1, "##0", 60, -1);
scanSpeedEditor.addChangeListener(pChangeListener);
scanSpeedEditor.setToolTipText("Scanning & Inspecting Speed");
add(scanSpeedEditor);

//add a spacer to force the box to be large enough for its title
add(Box.createRigidArea(new Dimension(15,0))); //horizontal spacer

}//end of ScanSpeedPanel::ScanSpeedPanel (constructor)
//-----------------------------------------------------------------------------

}//end of class ScanSpeedPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class DemoPanel
//
// This class creates and controls a panel with controls and displays for
// the demonstration function which simulates actual inspection.
//
// It accepts a MessageLink interface which is the simulator object which is
// controlled by this class.
//

class DemoPanel extends JPanel implements ActionListener{

public TitledBorder titledBorder;    
JButton forwardButton, reverseButton, stopButton, resetButton;
MessageLink mechSimulator;
    
//-----------------------------------------------------------------------------
// DemoPanel::DemoPanel (constructor)
//
//
  
public DemoPanel(MessageLink pMechSimulator)
{

mechSimulator = pMechSimulator;
    
}//end of DemoPanel::DemoPanel (constructor)
//-----------------------------------------------------------------------------    

//-----------------------------------------------------------------------------
// DemoPanel::init
//
// Initializes the object.
//

public void init()
{ 

setBorder(titledBorder = BorderFactory.createTitledBorder("Demo Simulation"));

setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

forwardButton = ControlPanel.addButtonToJPanel(this, "Fwd", 
     "Demo Forward", this, "Simulates a test piece moving forward.");

stopButton = ControlPanel.addButtonToJPanel(this, "Stop", 
               "Demo Stop", this, "Simulates a test piece stopped.");

stopButton.setEnabled(false);

reverseButton = ControlPanel.addButtonToJPanel(this, "Reverse", 
  "Demo Reverse", this, "Simulates a test piece moving in reverse.");

resetButton = ControlPanel.addButtonToJPanel(this, "Reset", 
           "Demo Reset", this, "Resets to no test piece in system.");
    
}//end of DemoPanel::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DemoPanel::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{ 

if ("Demo Forward".equals(e.getActionCommand())) {
    forwardButton.setEnabled(false);
    reverseButton.setEnabled(true);
    stopButton.setEnabled(true);
    //set the mode in the mechanical simulator object
    mechSimulator.xmtMessage(MessageLink.SET_MODE, MessageLink.FORWARD);
    }

if ("Demo Stop".equals(e.getActionCommand())) {
    stopButton.setEnabled(false);
    reverseButton.setEnabled(true);
    forwardButton.setEnabled(true);
    //set the mode in the mechanical simulator object
    mechSimulator.xmtMessage(MessageLink.SET_MODE, MessageLink.STOP);
    }

if ("Demo Reverse".equals(e.getActionCommand())) {
    reverseButton.setEnabled(false);
    stopButton.setEnabled(true);
    forwardButton.setEnabled(true);
    //set the mode in the mechanical simulator object
    mechSimulator.xmtMessage(MessageLink.SET_MODE, MessageLink.REVERSE);    
    }

if ("Demo Reset".equals(e.getActionCommand())) {
    
    //reset the mechanical simulator object
    mechSimulator.xmtMessage(MessageLink.SET_MODE, MessageLink.RESET);

    }

}//end of DemoPanel::actionPerformed
//-----------------------------------------------------------------------------

}//end of class DemoPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ManualControlPanel
//
// This class creates and controls a panel with controls and displays for
// manual control of the inspection process.  This is mainly used with
// handheld crabs and other systems without encoders or photoeyes.
//

class ManualControlPanel extends JPanel{

public TitledBorder titledBorder;
JButton pauseResumeButton, nextPieceButton;
JLabel calModeWarning;
ChangeListener changeListener;
ActionListener actionListener;
ImageIcon warningSymbol;

//-----------------------------------------------------------------------------
// ManualControl::ManualControl (constructor)
//
//

public ManualControlPanel(ChangeListener pChangeListener,
                    ActionListener pActionListener, ImageIcon pWarningSymbol)
{

changeListener = pChangeListener;
actionListener = pActionListener;
warningSymbol = pWarningSymbol;

}//end of ManualControl::ManualControl (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ManualControl::init
//
// Initializes the object.
//
  
public void init()
{

setBorder(titledBorder = BorderFactory.createTitledBorder(
                                                 "Manual Inspection Control"));

setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

nextPieceButton = ControlPanel.addButtonToJPanel(this, "Next Run",
              "Next Run", actionListener, "Begins inspection of next piece.");

nextPieceButton.setEnabled(false);

//space between button and label
add(Box.createRigidArea(new Dimension(5,0))); //horizontal spacer

add(calModeWarning = new JLabel("Cal Mode", warningSymbol, JLabel.LEADING));

calModeWarning.setVisible(false); //starts out invisible

//space between button and label
add(Box.createRigidArea(new Dimension(5,0))); //horizontal spacer

pauseResumeButton = ControlPanel.addButtonToJPanel(
    this, "Pause", "Pause or Resume",
       actionListener, "Pauses the inspection without moving to next piece.");

pauseResumeButton.setEnabled(false);

}//end of ManualControlPanel::init
//-----------------------------------------------------------------------------    


}//end of class ManualControlPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
