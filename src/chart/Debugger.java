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

import chart.mksystems.hardware.Hardware;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.*;

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

    ArrayList<JTextField> registerFields;
    
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
    
    //this is the address of the buffer holding the DSP register values
    //which are saved by the code using the debug routines
    //it should be adjusted whenever the buffer moves in memory due to adding
    //more variables
    
    private static final int DSP_REGISTER_BUFFER_ADDR = 0x02c7;

//-----------------------------------------------------------------------------
// Debugger::Debugger (constructor)
//
//

public Debugger(JFrame frame, Hardware pHardware)
{

    super(frame, "Debugger");

    hardware = pHardware;

}//end of Debugger::Debugger (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

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

    ramPage0 = new JRadioButton("0    "); //force wide so room for "Page" title
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

    //add a panel to hold miscellanous buttons
    p = new JPanel();
    p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));

    //add a button to clear the window
    b = new JButton("Clear");
    b.setAlignmentX(Component.CENTER_ALIGNMENT);
    b.setActionCommand("Clear");
    b.addActionListener(this);
    b.setToolTipText("Clear the display.");
    p.add(b);

    //add a Help button
    b = new JButton("Help");
    b.setAlignmentX(Component.CENTER_ALIGNMENT);
    b.setActionCommand("Help");
    b.addActionListener(this);
    b.setToolTipText("Display help information.");
    p.add(b);

    ramEdit.add(p);

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

    add(createMonitorPanel());
    
    pack();

}//end of Debugger::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::createMonitorPanel
//
// Creates a panel for viewing and editing the DSP registers and controlling
// breakpoints.
//
// The register values are read from a known buffer in DSP memory when the
// "Refresh" button is clicked. It is expected that the DSP code saves the
// registers to that buffer periodically or when performing a debug halt.
//
// If the DPS is in a debug halt, it will be restarted when the "Continue"
// button is clicked.
//
// Returns reference to the JPanel.
//

private JPanel createMonitorPanel(){
    
    JPanel monitorPanel;
    
    monitorPanel = new JPanel();
    
    monitorPanel.setBorder(BorderFactory.createTitledBorder("Monitor"));
    monitorPanel.setLayout(new BoxLayout(monitorPanel, BoxLayout.PAGE_AXIS));
    monitorPanel.setAlignmentY(Component.TOP_ALIGNMENT);

    JButton b;
    
    b = new JButton("Refresh");
    b.setActionCommand("Refresh Monitor Panel");
    b.addActionListener(this);
    b.setToolTipText("Updates values on the monitor panel.");
    b.setAlignmentX(Component.LEFT_ALIGNMENT);
    monitorPanel.add(b);
    
    monitorPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer
    
    monitorPanel.add(createRegisterPanel());
    
    monitorPanel.add(Box.createRigidArea(new Dimension(0,5))); //vertical spacer    

    b = new JButton("Continue");
    b.setActionCommand("Continue DSP From Debug Halt");
    b.addActionListener(this);
    b.setToolTipText("Restarts DSP execution if in Debug Halt.");
    b.setAlignmentX(Component.LEFT_ALIGNMENT);    
    monitorPanel.add(b);
        
    monitorPanel.add(Box.createVerticalGlue()); //force components to the top

    return(monitorPanel);

}//end of Debugger::createMonitorPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::createRegisterPanel
//
// Creates a panel containing a grid to display the DSP register values.
//
// Returns reference to the JPanel.
//

private JPanel createRegisterPanel()
{

    JPanel registerPanel;
    
    registerPanel = new JPanel();
    
    setSizes(registerPanel, 200, 400);
    registerPanel.setBorder(BorderFactory.createTitledBorder("DSP Registers"));
    registerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    registerFields = new ArrayList<>();
    ArrayList<JTextField> rf = registerFields; //use short name
    
    GridLayout gridLayout = new GridLayout(0,2);

    gridLayout.setHgap(2);
    gridLayout.setVgap(3);
    
    registerPanel.setLayout(gridLayout);

    JTextField tf;
    
    //two empty labels to create a spacer
    registerPanel.add(new JLabel(""));
    registerPanel.add(new JLabel(""));
    
    registerPanel.add(new JLabel("Debug"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("A"));
    registerPanel.add(tf = new JTextField("0x4ffff")); rf.add(tf);
    registerPanel.add(new JLabel("B"));
    registerPanel.add(tf = new JTextField("0x4ffff")); rf.add(tf);
    registerPanel.add(new JLabel("T"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("ST0"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("ST1"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("PMST"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("BRC"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);    
    registerPanel.add(new JLabel("SP"));    
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);    
    registerPanel.add(new JLabel("AR0"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR1"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR2"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR3"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR4"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR5"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR6"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    registerPanel.add(new JLabel("AR7"));
    registerPanel.add(tf = new JTextField("0xffff")); rf.add(tf);
    
    return(registerPanel);
    
}//end of Debugger::createRegisterGrid
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

    //display the help info window
    if (e.getActionCommand().equalsIgnoreCase("Help")){
        Help help = new Help(null, "Debugger Help.txt");
        help.init();
        return;
    }
        
    //DSP cores running or in reset
    if (e.getActionCommand().equalsIgnoreCase("DSP's Running")){
        hardware.setState(
                     chassisNum, slotNum, 1, dspRunEnabled.isSelected() ? 1:0);
        if (!dspRunEnabled.isSelected()){
            textArea.append(
                        "\nSample tranfer into DSP's may have errors when\n");
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

    parseUserInputs();

    if (e.getActionCommand().equalsIgnoreCase("Refresh Monitor Panel")){
        refreshMonitorPanel();
        return;
    }

    if (e.getActionCommand().equalsIgnoreCase("Continue DSP From Debug Halt")){
        restartDSPFromDebugHalt();
        refreshMonitorPanel();        
        return;
    }
        
    //write the data to the specified address
    if (e.getActionCommand().equalsIgnoreCase("Modify")){
        modifyData();
    }

    //fill a block of memory with the specified address and data
    if (e.getActionCommand().equalsIgnoreCase("Fill")){
        fillData();
    }

    //any button clicked or radio button change causes a screen refresh to
    //display the data from the selected range
    displayData();

}//end of Debugger::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::parseUserInputs
//
// Read user inputs and store in variables.
//

private void parseUserInputs()
{        
        
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

    if (dspChip1.isSelected()) {whichDSPChip = 1;}
    else if (dspChip2.isSelected()) {whichDSPChip = 2;}

    if (dspCoreA.isSelected()) {whichDSPCore = 1;}
    else if (dspCoreB.isSelected()) {whichDSPCore = 2;}
    else if (dspCoreC.isSelected()) {whichDSPCore = 3;}
    else if (dspCoreD.isSelected()) {whichDSPCore = 4;}

    if (ramLocal.isSelected()) {ramType = 0;}
    else if (ramShared.isSelected()) {ramType = 1;}

    if (ramPage0.isSelected()) {ramPage = 0;}
    else if (ramPage1.isSelected()) {ramPage = 1;}
    else if (ramPage2.isSelected()) {ramPage = 2;}
    else if (ramPage3.isSelected()) {ramPage = 3;}
    
}//end of Debugger::parseUserInputs
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
    if (newChassisNum != chassisNum) {changed = true;}
    chassisNum = newChassisNum;

    int newSlotNum = Integer.valueOf(slotNumber.getText());
    if (newSlotNum != slotNum) {changed = true;}
    slotNum = newSlotNum;

    if (changed) {refreshDisplayedStates();}

}//end of Debugger::refreshVarsFromUserInputs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::toHexString
//
// Converts an integer to a 4 character hex string.
//

String toHexString(int pValue)
{

    String s = Integer.toString(pValue & 0xffff, 16);

    //force length to be four characters
    if (s.length() == 0) {return("0000" + s);}
    else
    if (s.length() == 1) {return("000" + s);}
    else
    if (s.length() == 2) {return("00" + s);}
    else
    if (s.length() == 3) {return("0" + s);}
    else
    {return (s);}

}//end of Debugger::toHexString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::toHexString
//
// Converts a long to a 9 character hex string.
//

String toHexString(long pValue)
{

    String s = Long.toString(pValue, 16);

    //force length to be four characters
    if (s.length() == 0) {return("0000" + s);}
    else
    if (s.length() == 1) {return("000" + s);}
    else
    if (s.length() == 2) {return("00" + s);}
    else
    if (s.length() == 3) {return("0" + s);}
    else
    {return (s);}

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
// Debugger::refreshMonitorPanel
//
// Refreshes the data in the Monitor panel such as DSP registers.
//
// Values read are from the currently selected DSP core in the currently
// selected chip.
//
// The A&B registers are actually only 40 bits, but the transfer occurs in
// words so they take up 48 bits (6 bytes) in the buffer. The top byte is
// always zero.
//

void refreshMonitorPanel()
{

    //readRAM expects count to be in words, so divide number of bytes by 2
    
    //the register variables are always on Page 0 of Local memory at address
    //specified by DSP_REGISTER_BUFFER_ADDR

    hardware.readRAM(chassisNum, slotNum, whichDSPChip, whichDSPCore,
            0 /* local memory */, 0 /* page*/,
            DSP_REGISTER_BUFFER_ADDR, DATABLOCK_SIZE/2, dataBlock);

    ArrayList<JTextField> rf = registerFields; //use short name
    
    int dbIndex = 0; int i = 0;
    
    //extract bytes from the buffer and display the values in text fields
    
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //status
    dbIndex = setTextFieldTo6Bytes(rf.get(i++), dataBlock, dbIndex); //A reg
    dbIndex = setTextFieldTo6Bytes(rf.get(i++), dataBlock, dbIndex); //B reg
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //T reg    
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //ST0
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //ST1    
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //PMST
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //BRC
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //SP    
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR0
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR1
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR2
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR3
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR4
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR5
    dbIndex = setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR6
    setTextFieldToInt(rf.get(i++), dataBlock, dbIndex); //AR7
    
}//end of Debugger::refreshMonitorPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::setTextFieldToInt
//
// Creates a word from the two bytes stored in pBuffer at pIndex, converts
// it to a hexadecimal string and applies it to pField.
//
// pIndex is updated to point to the following buffer element and the new
// value is returned.
//
// NOTE: Each byte is ANDed with 0xff variant to remove sign extension.
//

private int setTextFieldToInt(JTextField pField, byte[] pBuffer, int pIndex)
{

    pField.setText(
        separateIntoPairs(
            toHexString(
                ((int)dataBlock[pIndex++]<<8) +
                (dataBlock[pIndex++] & 0xff)))
    );
    
    return(pIndex);
    
}//end of Debugger::setTextFieldToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::setTextFieldTo6Bytes
//
// Creates a 48 bit value from the six bytes stored in pBuffer at pIndex,
// converts it to a hexadecimal string and applies it to pField.
//
// pIndex is updated to point to the following buffer element and the new
// value is returned.
//
// NOTE: Each byte is ANDed with 0xff variant to remove sign extension.
//

private int setTextFieldTo6Bytes(JTextField pField, byte[] pBuffer, int pIndex)
{
            
    long r = ((long)dataBlock[pIndex++]<<40 & 0xff0000000000L) +
             ((long)dataBlock[pIndex++]<<32 & 0xff00000000L) +                
             ((long)dataBlock[pIndex++]<<24 & 0xff000000L) +                
             ((long)dataBlock[pIndex++]<<16 & 0xff0000L) +                
             ((long)dataBlock[pIndex++]<<8 & 0xff00L)  +
             ((long)dataBlock[pIndex++] & 0xffL);

    pField.setText(separateIntoPairs(toHexString(r)));
    
    return(pIndex);
    
}//end of Debugger::setTextFieldTo6Bytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::separateIntoPairs
//
// Starting from the right end of pIn, inserts a space after every two
// two characters to separate the string into pairs.
//

private String separateIntoPairs(String pIn)
{

    String o = "";
    
    int j = 0;
    
    for(int i=pIn.length(); i>0; i--){
        
        o = pIn.substring(i-1,i) + o;
        
        if(j++ == 1){ o = " " + o; j = 0; }
        
    }
    
    return(o);
        
}//end of Debugger::separateIntoPairs
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::restartDSPFromDebugHalt
//
// Restarts the DSP if it is in Debug Halt.
//
// This only restarts the currently selected DSP core in the currently
// selected chip.
//

void restartDSPFromDebugHalt()
{

    //clear the Debug Status word, the first word in the DSP register buffer
    //the LSB controls the halt state
    
    hardware.writeRAM(chassisNum, slotNum, whichDSPChip, whichDSPCore, ramType,
                                  ramPage, DSP_REGISTER_BUFFER_ADDR, 0x00);

}//end of Debugger::restartDSPFromDebugHalt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

public void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of Debugger::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debugger::refreshDisplayedStates
//

void refreshDisplayedStates()
{

    //some settings may be changed when the window is not active, so make sure
    //that all screen settings match the actual settings

    samplingEnabled.setSelected(
                (hardware.getState(chassisNum, slotNum, 0) != 0));

    dspRunEnabled.setSelected(
                (hardware.getState(chassisNum, slotNum, 1) != 0));

    testDataEnabled.setSelected(
                (hardware.getState(chassisNum, slotNum, 2) != 0));

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
