/******************************************************************************
* Title: Debugger.java
* Author: Mike Schoonover
* Date: 5/16/09
*
* Purpose:
*
* This class displays a Debugger window.
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
import javax.swing.BorderFactory;
import java.awt.event.*;
import java.text.DecimalFormat;

import chart.mksystems.hardware.Hardware;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Debugger
//
// This class displays debugging tools in a window.
//

class Debugger extends JDialog implements ActionListener, WindowListener {

JTextArea textArea;
Hardware hardware;
ButtonGroup bgDSPChip, bgDSPCore, bgRamType, bgRamPage;
TextField displayAddress, modifyAddress, modifyValue;
TextField fillSize, chassisNumber, slotNumber;
DecimalFormat fourDigit;

JCheckBox samplingEnabled, dspRunEnabled, testDataEnabled;

static int FALSE = 0;
static int TRUE = 1;

int whichDSPChip = 1;
int whichDSPCore = 1;
int ramType = 0;
int ramPage = 1;
int displayAddr, modifyAddr, modifyVal, fillSz;
int chassisNum=0, slotNum=0;

JRadioButton dspChip1, dspChip2;
JRadioButton dspCoreA, dspCoreB, dspCoreC, dspCoreD;
JRadioButton ramLocal, ramShared;
JRadioButton ramPage0, ramPage1, ramPage2, ramPage3;


static int DATABLOCK_SIZE = 128;
byte[]dataBlock;

//-----------------------------------------------------------------------------
// Debugger::Debugger (constructor)
//
//

public Debugger(JFrame frame, Hardware pHardware)
{

super(frame, "Debugger");

hardware = pHardware;

dataBlock = new byte[DATABLOCK_SIZE];

JPanel p, p2;
JButton b;

int tWidth = 370;
int tHeight = 300;

addWindowListener(this);

getContentPane().
            setLayout(new BoxLayout(getContentPane(), BoxLayout.LINE_AXIS));

JPanel ramEdit = new JPanel();
ramEdit.setBorder(BorderFactory.createTitledBorder("DSP Memory"));
ramEdit.setLayout(new BoxLayout(ramEdit, BoxLayout.PAGE_AXIS));
ramEdit.setAlignmentY(Component.TOP_ALIGNMENT);

//add a text area to display the memory values
textArea = new JTextArea();
JScrollPane areaScrollPane = new JScrollPane(textArea);
areaScrollPane.setMinimumSize(new Dimension(tWidth, tHeight));
areaScrollPane.setPreferredSize(new Dimension(tWidth, tHeight));
areaScrollPane.setMaximumSize(new Dimension(tWidth, tHeight));
textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
ramEdit.add(areaScrollPane);

bgDSPChip = new ButtonGroup(); bgDSPCore = new ButtonGroup();
bgRamType = new ButtonGroup(); bgRamPage = new ButtonGroup();

//add a panel for entering starting address of range to be displayed
p = new JPanel();
p.setBorder(BorderFactory.createTitledBorder("View"));
p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

p.add(new JLabel("Address"));
displayAddress = new TextField(6);
p.add(displayAddress);

b = new JButton("Display");
b.setActionCommand("Display");
b.addActionListener(this);
b.setToolTipText("Display memory contents.");
p.add(b);

b = new JButton("Next");
b.setActionCommand("Next");
b.addActionListener(this);
b.setToolTipText("Display next block of memory contents.");
p.add(b);

b = new JButton("Previous");
b.setActionCommand("Previous");
b.addActionListener(this);
b.setToolTipText("Display previous block of memory contents.");
p.add(b);

ramEdit.add(p);

//add a panel for writing a value to a memory location

p = new JPanel();
p.setBorder(BorderFactory.createTitledBorder("Modify"));
p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

p.add(new JLabel("Address"));
modifyAddress = new TextField(6);
p.add(modifyAddress);

p.add(new JLabel("Value"));
modifyValue = new TextField(6);
p.add(modifyValue);

b = new JButton("Modify");
b.setActionCommand("Modify");
b.addActionListener(this);
b.setToolTipText("Modify memory contents.");
p.add(b);

ramEdit.add(p);

//add a panel for filling a block of memory

p = new JPanel();
p.setBorder(BorderFactory.createTitledBorder("Fill"));
p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

p.add(new JLabel("Block Size"));
fillSize = new TextField(6);
p.add(fillSize);

b = new JButton("Fill");
b.setActionCommand("Fill");
b.addActionListener(this);
b.setToolTipText("Fills a block of memory.");
p.add(b);

p.add(Box.createHorizontalGlue()); //force components to the left

ramEdit.add(p);

//add a panel for selecting the DSP chip, core, and local/shared RAM

p = new JPanel();
p.setBorder(BorderFactory.createTitledBorder("Select"));
p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

//radio button panel for selecting between DSP chips

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Chip"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentY(Component.TOP_ALIGNMENT);

dspChip1 = new JRadioButton("DSP1");
dspChip1.setActionCommand("DSP1");
dspChip1.addActionListener(this);
dspChip1.setToolTipText("Select chip DSP1.");
dspChip1.setSelected(true);
bgDSPChip.add(dspChip1);
p2.add(dspChip1);

dspChip2 = new JRadioButton("DSP2");
dspChip2.setActionCommand("DSP2");
dspChip2.addActionListener(this);
dspChip2.setToolTipText("Select chip DSP2.");
dspChip2.setSelected(true);
bgDSPChip.add(dspChip2);
p2.add(dspChip2);

p.add(p2);

//radio button panel for selecting between DSP cores

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Core"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentY(Component.TOP_ALIGNMENT);

dspCoreA = new JRadioButton("A");
dspCoreA.setActionCommand("Core A");
dspCoreA.addActionListener(this);
dspCoreA.setToolTipText("Select DSP core A.");
dspCoreA.setSelected(true);
bgDSPCore.add(dspCoreA);
p2.add(dspCoreA);

dspCoreB = new JRadioButton("B");
dspCoreB.setActionCommand("Core B");
dspCoreB.addActionListener(this);
dspCoreB.setToolTipText("Select DSP core B.");
bgDSPCore.add(dspCoreB);
p2.add(dspCoreB);

dspCoreC = new JRadioButton("C");
dspCoreC.setActionCommand("Core C");
dspCoreC.addActionListener(this);
dspCoreC.setToolTipText("Select DSP core C.");
bgDSPCore.add(dspCoreC);
p2.add(dspCoreC);

dspCoreD = new JRadioButton("D");
dspCoreD.setActionCommand("Core D");
dspCoreD.addActionListener(this);
dspCoreD.setToolTipText("Select DSP core D.");
bgDSPCore.add(dspCoreD);
p2.add(dspCoreD);

p.add(p2);

//radio button panel for selecting local/shared memory

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Type"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentY(Component.TOP_ALIGNMENT);

ramLocal = new JRadioButton("Local");
ramLocal.setActionCommand("Local");
ramLocal.addActionListener(this);
ramLocal.setToolTipText("Display DSP local memory contents.");
ramLocal.setSelected(true);
bgRamType.add(ramLocal);
p2.add(ramLocal);

ramShared = new JRadioButton("Shared");
ramShared.setActionCommand("Shared");
ramShared.addActionListener(this);
ramShared.setToolTipText("Display DSP shared memory contents.");
bgRamType.add(ramShared);
p2.add(ramShared);

p.add(p2);

//radio button panel for selecting RAM page

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Page"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentY(Component.TOP_ALIGNMENT);

ramPage0 = new JRadioButton("0");
ramPage0.setActionCommand("RAM Page 0");
ramPage0.addActionListener(this);
ramPage0.setToolTipText("Display DSP memory page 0.");
ramPage0.setSelected(true);
bgRamPage.add(ramPage0);
p2.add(ramPage0);

ramPage1 = new JRadioButton("1");
ramPage1.setActionCommand("RAM Page 1");
ramPage1.addActionListener(this);
ramPage1.setToolTipText("Display DSP memory page 1.");
ramPage1.setSelected(true);
bgRamPage.add(ramPage1);
p2.add(ramPage1);

ramPage2 = new JRadioButton("2");
ramPage2.setActionCommand("RAM Page 2");
ramPage2.addActionListener(this);
ramPage2.setToolTipText("Display DSP memory page 2.");
ramPage2.setSelected(true);
bgRamPage.add(ramPage2);
p2.add(ramPage2);

ramPage3 = new JRadioButton("3");
ramPage3.setActionCommand("RAM Page 3");
ramPage3.addActionListener(this);
ramPage3.setToolTipText("Display DSP memory page 3.");
ramPage3.setSelected(true);
bgRamPage.add(ramPage3);
p2.add(ramPage3);

p.add(p2);

//force the panels to the left

p.add(Box.createHorizontalGlue());

ramEdit.add(p);

//add a button to clear the window
b = new JButton("Clear");
p2.setAlignmentX(Component.CENTER_ALIGNMENT);
b.setActionCommand("Clear");
b.addActionListener(this);
b.setToolTipText("Clear the display.");
b.setSelected(true);
ramEdit.add(b);

//add the DSP Memory editor panel to the window
add(ramEdit);

//create a panel to hold various controls

JPanel controlPanel = new JPanel();
controlPanel.setBorder(BorderFactory.createTitledBorder("DSP Controls"));
controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.PAGE_AXIS));
controlPanel.setAlignmentY(Component.TOP_ALIGNMENT);

//panel for selecting chassis and board

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Selector"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentX(Component.LEFT_ALIGNMENT);
p2.setMaximumSize(new Dimension(120, 60));

p2.add(new Label("Chassis Number"));
chassisNumber = new TextField(1);
chassisNumber.setText("0");
p2.add(chassisNumber);

p2.add(new Label("Slot Number"));
slotNumber = new TextField(1);
slotNumber.setText("0");
p2.add(slotNumber);

controlPanel.add(p2);

//check box panel for selecting modes

p2 = new JPanel();
p2.setBorder(BorderFactory.createTitledBorder("Modes"));
p2.setLayout(new BoxLayout(p2, BoxLayout.PAGE_AXIS));
p2.setAlignmentX(Component.LEFT_ALIGNMENT);

samplingEnabled = new JCheckBox("Sampling Enabled");
samplingEnabled.setActionCommand("Sampling Enabled");
samplingEnabled.addActionListener(this);
samplingEnabled.setToolTipText("Select sampling or debug mode.");
samplingEnabled.setSelected(false);
p2.add(samplingEnabled);

dspRunEnabled = new JCheckBox("DSP's Running");
dspRunEnabled.setActionCommand("DSP's Running");
dspRunEnabled.addActionListener(this);
dspRunEnabled.setToolTipText("Enable or disable DSP program running.");
dspRunEnabled.setSelected(true);
p2.add(dspRunEnabled);

testDataEnabled = new JCheckBox("Use Test Data");
testDataEnabled.setActionCommand("Use Test Data");
testDataEnabled.addActionListener(this);
testDataEnabled.setToolTipText("Choose between real data and test data.");
testDataEnabled.setSelected(true);
p2.add(testDataEnabled);

controlPanel.add(p2);

b = new JButton("Verify DSP Code");
b.setActionCommand("Verify DSP Code");
b.addActionListener(this);
b.setToolTipText("Verifies that code in each DSP matches the file.");
controlPanel.add(b);

controlPanel.add(Box.createVerticalGlue()); //force components to the top

//add the DSP Control panel to the window
add(controlPanel);

pack();

}//end of Debugger::Debugger (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::actionPerformed
//
// Catches action events from buttons, etc.
//

@Override
public void actionPerformed(ActionEvent e)
{

//get the latest settings input by the user
refreshVarsFromUserInputs();

//clear the display - exit without displaying
if (e.getActionCommand().equalsIgnoreCase("Verify DSP Code")){
    //this is the slow verify method
    //NOTE: since the DSP code is modified depending on the type of signal
    //rectification, the verify will return errors unless rectification is
    //set to +half or RF which uses the default opcodes
    //wip mks - this function will enable the sampling function in the remote
    //  the state of the checkbox for this function should be set to on
    hardware.verifyAllDSPCode2();
    return;
    }

//clear the display - exit without displaying
if (e.getActionCommand().equalsIgnoreCase("Clear")){
    textArea.setText(null);
    return;
    }

//DSP cores running or in reset
if (e.getActionCommand().equalsIgnoreCase("DSP's Running")){
    hardware.setState(
                    chassisNum, slotNum, 1, dspRunEnabled.isSelected() ? 1:0);
    if (!dspRunEnabled.isSelected()){
        textArea.append("\nSample tranfer into DSP's may have errors when\n");
        textArea.append("they are not running due to the internal DSP\n");
        textArea.append("clocks running slower while in reset.\n");
        }
    }

//choose between actual A/D convertor data and test sequence data
if (e.getActionCommand().equalsIgnoreCase("Use Test Data")){
    hardware.setState(
                chassisNum, slotNum, 2, testDataEnabled.isSelected() ? 1:0);
    return;
    }

//check for sampling state before updating the display - if sampling is enabled
//then do not update any other display to avoid collisions on the HPI bus

//sampling enabled or disabled (memory can only be viewed with the debugger
//if sampling is disabled
hardware.setState(chassisNum, slotNum, 0, samplingEnabled.isSelected() ? 1:0);

if (samplingEnabled.isSelected()) {
    textArea.append("\nCannot update display when sampling is enabled.\n");
    return;
    }

//adjust the address to the next block
if (e.getActionCommand().equalsIgnoreCase("Next")){

    displayAddress.setText(
            toHexString(Integer.parseInt(displayAddress.getText(), 16) + 64));

    }

//adjust the address to the previous block
if (e.getActionCommand().equalsIgnoreCase("Previous")){

    displayAddress.setText(
            toHexString(Integer.parseInt(displayAddress.getText(), 16) - 64));

    }

//parse the user settings

try{
    displayAddr = Integer.parseInt(displayAddress.getText(), 16);
    }
catch(NumberFormatException ec){
    displayAddr = 0;
    displayAddress.setText("0000");
    }

try{
    modifyAddr = Integer.parseInt(modifyAddress.getText(), 16);
    }
catch(NumberFormatException ec){
    modifyAddr = 0;
    modifyAddress.setText("0000");
    }


try{
    modifyAddr = Integer.parseInt(modifyAddress.getText(), 16);
    }
catch(NumberFormatException ec){
    modifyAddr = 0;
    modifyAddress.setText("0000");
    }

try{
    modifyVal = Integer.parseInt(modifyValue.getText(), 16);
    }
catch(NumberFormatException ec){
    modifyVal = 0;
    modifyValue.setText("0000");
    }

try{
    fillSz = Integer.parseInt(fillSize.getText(), 16);
    }
catch(NumberFormatException ec){
    fillSz = 0;
    fillSize.setText("0000");
    }

if (dspChip1.isSelected()) whichDSPChip = 1;
else
if (dspChip2.isSelected()) whichDSPChip = 2;

if (dspCoreA.isSelected()) whichDSPCore = 1;
else
if (dspCoreB.isSelected()) whichDSPCore = 2;
else
if (dspCoreC.isSelected()) whichDSPCore = 3;
else
if (dspCoreD.isSelected()) whichDSPCore = 4;

if (ramLocal.isSelected()) ramType = 0;
else
if (ramShared.isSelected()) ramType = 1;

if (ramPage0.isSelected()) ramPage = 0;
else
if (ramPage1.isSelected()) ramPage = 1;
else
if (ramPage2.isSelected()) ramPage = 2;
else
if (ramPage3.isSelected()) ramPage = 3;

//write the data to the specified address
if (e.getActionCommand().equalsIgnoreCase("Modify")){
    modifyData();
    }

//fill a block of memory with the specified address and data
if (e.getActionCommand().equalsIgnoreCase("Fill")){
    fillData();
    }

//any button clicked or radio button change causes a screen refresh to display
//the data from the selected range
displayData();

}//end of Debugger::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::refreshVarsFromUserInputs
//
// Refreshes variables from the user inputs.
//
// If the chassis or slot number has changed, the displayed state values are
// updated to match what the states actually are for the new chassis and slot.
//

void refreshVarsFromUserInputs()
{

boolean changed = false;

int newChassisNum = Integer.valueOf(chassisNumber.getText());
if (newChassisNum != chassisNum) changed = true;
chassisNum = newChassisNum;

int newSlotNum = Integer.valueOf(slotNumber.getText());
if (newSlotNum != slotNum) changed = true;
slotNum = newSlotNum;

if (changed) refreshDisplayedStates();

}//end of Debugger::refreshVarsFromUserInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::toHexString
//
// Converts an integer to a 4 character hex string.
//

String toHexString(int pValue)
{

String s = Integer.toString(pValue, 16);

//force length to be four characters
if (s.length() == 0) return "0000" + s;
else
if (s.length() == 1) return "000" + s;
else
if (s.length() == 2) return "00" + s;
else
if (s.length() == 3) return "0" + s;
else
return s;

}//end of Debugger::toHexString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::modifyData
//
// Writes the user entered byte to the specified address.
//

void modifyData()
{

hardware.writeRAM(chassisNum, slotNum, whichDSPChip, whichDSPCore, ramType,
                                               ramPage, modifyAddr, modifyVal);

}//end of Debugger::modifyData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::fillData
//
// Fills a block of memory with the specified address, size, and value.
//

void fillData()
{

hardware.fillRAM(chassisNum, slotNum, whichDSPChip, whichDSPCore, ramType,
                                       ramPage, modifyAddr, fillSz, modifyVal);

}//end of Debugger::fillData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::displayData
//
// Displays 64 words of data from the selected memory location.
//

void displayData()
{

textArea.append("\n");

//readRAM expects count to be in words, so divide number of bytes by 2

hardware.readRAM(chassisNum, slotNum, whichDSPChip, whichDSPCore, ramType,
                             ramPage, displayAddr, DATABLOCK_SIZE/2, dataBlock);

int dbIndex = 0;

for (int line = 0; line < 8; line++){

    //display the address for each line
    textArea.append(" " + toHexString(displayAddr) + ":  ");
    displayAddr += 8;

    for (int col = 0; col < 8; col++){

        textArea.append(
            toHexString(
                (int)((dataBlock[dbIndex++]<<8) & 0xff00) +
                (int)(dataBlock[dbIndex++] & 0x00ff)
                )
             + " ");

        }//for (col = 0; col < 8; col++)

    textArea.append("\n");

    }// for (line = 0; line < 8; line++)

}//end of Debugger::displayData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::refreshDisplayedStates
//

void refreshDisplayedStates()
{

//some settings may be changed when the window is not active, so make sure
//that all screen settings match the actual settings

samplingEnabled.setSelected(
                hardware.getState(chassisNum, slotNum, 0) == 0 ? false:true);

dspRunEnabled.setSelected(
                hardware.getState(chassisNum, slotNum, 1) == 0 ? false:true);

testDataEnabled.setSelected(
                hardware.getState(chassisNum, slotNum, 2) == 0 ? false:true);

}//end of Debugger::refreshDisplayedStates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::windowActivated
//
// Handles actions necessary when the window becomes active.
//

@Override
public void windowActivated(WindowEvent e)

{

//make sure variables reflect user inputs
refreshVarsFromUserInputs();

//some settings may be changed when the window is not active, so make sure
//that all screen settings match the actual settings
refreshDisplayedStates();

}//end of Debugger::windowActivated
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::(various window listener functions)
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
public void windowOpened(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}
@Override
public void windowDeactivated(WindowEvent e){}

//end of Debugger::(various window listener functions)
//-----------------------------------------------------------------------------

}//end of class Debugger
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
