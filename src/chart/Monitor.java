/******************************************************************************
* Title: Monitor.java
* Author: Mike Schoonover
* Date: 4/23/09
*
* Purpose:
*
* This class monitors various inputs for debugging purposes.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.inifile.IniFile;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Monitor
//
// This class creates a window to display values being monitored for debug
// purposes.
//

class Monitor extends JDialog implements ActionListener {

    JPanel panel;

    Font blackFont, redFont;

    ActionListener actionListener;
    IniFile configFile;

    JLabel chassisNumLabel, boardNumLabel;
    JLabel Encoder1CountLabel, Encoder2CountLabel;

    JLabel inspectionStatusLabel, rpmLabel, rpmVarianceLabel;

    JLabel input1Label, input2Label, input3Label, input4Label;
    JLabel input5Label, input6Label, input7Label, input8Label;
    JLabel input9Label, input10Label;

    String input1Text, input2Text, input3Text, input4Text, input5Text;
    String input6Text, input7Text, input8Text, input9Text, input10Text;
    String Encoder1CountText, Encoder2CountText;

//-----------------------------------------------------------------------------
// Monitor::Monitor (constructor)
//
//

public Monitor(JFrame frame, IniFile pConfigFile,
                                                ActionListener pActionListener)
{

    super(frame, "System Monitor");

    actionListener = pActionListener;
    configFile = pConfigFile;

}//end of Monitor::Monitor (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Monitor::init
//
// Initializes the object.
//

public void init()
{

    configure(configFile);

    int panelWidth = 250;
    int panelHeight = 500;

    setMinimumSize(new Dimension(panelWidth, panelHeight));
    setPreferredSize(new Dimension(panelWidth, panelHeight));
    setMaximumSize(new Dimension(panelWidth, panelHeight));

    //create red and black fonts for use with display objects
    Hashtable<TextAttribute, Object> map =
                new Hashtable<TextAttribute, Object>();
    blackFont = new Font("Dialog", Font.PLAIN, 12);
    map.put(TextAttribute.FOREGROUND, Color.RED);
    redFont = blackFont.deriveFont(map);

    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    add(panel);

    chassisNumLabel = new JLabel("Chassis Number : ");
    panel.add(chassisNumLabel);

    boardNumLabel = new JLabel("Board Number : ");
    panel.add(boardNumLabel);

    input1Label = new JLabel(input1Text + " : ");
    panel.add(input1Label);

    input2Label = new JLabel(input2Text + " : ");
    panel.add(input2Label);

    input3Label = new JLabel(input3Text + " : ");
    panel.add(input3Label);

    input4Label = new JLabel(input4Text + " : ");
    panel.add(input4Label);

    input5Label = new JLabel(input5Text + " : ");
    panel.add(input5Label);

    input6Label = new JLabel(input6Text + " : ");
    panel.add(input6Label);

    input7Label = new JLabel(input7Text + " : ");
    panel.add(input7Label);

    input8Label = new JLabel(input8Text + " : ");
    panel.add(input8Label);

    input9Label = new JLabel(input9Text + " : ");
    panel.add(input9Label);

    input10Label = new JLabel(input10Text + " : ");
    panel.add(input10Label);

    Encoder1CountLabel = new JLabel(Encoder1CountText + " : ");
    panel.add(Encoder1CountLabel);

    Encoder2CountLabel = new JLabel(Encoder2CountText + " : ");
    panel.add(Encoder2CountLabel);

    inspectionStatusLabel = new JLabel("Last Inspection Control Command : ");
    panel.add(inspectionStatusLabel);

    rpmLabel = new JLabel("RPM : ");
    panel.add(rpmLabel);

    rpmVarianceLabel = new JLabel("RPM Variance : ");
    panel.add(rpmVarianceLabel);

    JButton zeroEncoderCounts = new JButton("Zero Encoder Counts");
    zeroEncoderCounts.setActionCommand("Zero Encoder Counts");
    zeroEncoderCounts.addActionListener(this);
    zeroEncoderCounts.setToolTipText("Zero Encoder Counts");
    panel.add(zeroEncoderCounts);

    JButton pulseOutput1 = new JButton("Pulse Output 1");
    pulseOutput1.setActionCommand("Pulse Output 1");
    pulseOutput1.addActionListener(this);
    pulseOutput1.setToolTipText("Pulse Output 1");
    panel.add(pulseOutput1);

    pack();

}//end of Monitor::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Monitor::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    //tells the Control board to zero the encoder counts
    if (source.getToolTipText().equalsIgnoreCase("Zero Encoder Counts")){
        //pass the command back to the main function
        actionListener.actionPerformed(
                              new ActionEvent(this, 1, "Zero Encoder Counts"));
        return;
    }

    //tells the Control board to pulse output 1
    if (source.getToolTipText().equalsIgnoreCase("Pulse Output 1")){
        //pass the command back to the main function
        actionListener.actionPerformed(
                              new ActionEvent(this, 1, "Pulse Output 1"));
        return;
    }

}//end of Monitor::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Monitor::updateStatus
//
// Updates the display to show the current state of the I/O.
//

public void updateStatus(byte[] pMonitorBuffer)
{

    int x = 0;

    if (pMonitorBuffer[x++] == 0) {input1Label.setText(input1Text + " : Off");}
    else {input1Label.setText(input1Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input2Label.setText(input2Text + " : Off");}
    else {input2Label.setText(input2Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input3Label.setText(input3Text + " : Off");}
    else {input3Label.setText(input3Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input4Label.setText(input4Text + " : Off");}
    else {input4Label.setText(input4Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input5Label.setText(input5Text + " : Off");}
    else {input5Label.setText(input5Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input6Label.setText(input6Text + " : Off");}
    else {input6Label.setText(input6Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input7Label.setText(input7Text + " : Off");}
    else {input7Label.setText(input7Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {input8Label.setText(input8Text + " : Off");}
    else {input8Label.setText(input8Text + " : On");}

    if (pMonitorBuffer[x++] == 0) {
        setLabelOnOff(input9Label, input9Text, false);}
    else {setLabelOnOff(input9Label, input9Text, true);}

    if (pMonitorBuffer[x++] == 0) {
        input10Label.setText(input10Text + " : Off");}
    else {input10Label.setText(input10Text + " : On");}

    chassisNumLabel.setText("Chassis Number : " + pMonitorBuffer[x++]);

    boardNumLabel.setText("Board Number : " + pMonitorBuffer[x++]);

    inspectionStatusLabel.setText(
                   "Last Inspection Control Command : " + pMonitorBuffer[x++]);

    int rpm = (int)((pMonitorBuffer[x++]<<8) & 0xff00) +
                                             (int)(pMonitorBuffer[x++] & 0xff);

    rpmLabel.setText("RPM : " +  rpm);

    int rpmVariation =  //cast to short to force sign extension
      (short)((pMonitorBuffer[x++]<<8) & 0xff00) + (pMonitorBuffer[x++] & 0xff);

    rpmVarianceLabel.setText("RPM Variance : " + rpmVariation);

    // combine four bytes each to make the encoder counts

    int Encoder1Count, Encoder2Count;

    // create integer from four bytes in buffer
    Encoder1Count = ((pMonitorBuffer[x++] << 24));
    Encoder1Count |= (pMonitorBuffer[x++] << 16) & 0x00ff0000;
    Encoder1Count |= (pMonitorBuffer[x++] << 8)  & 0x0000ff00;
    Encoder1Count |= (pMonitorBuffer[x++])       & 0x000000ff;

    Encoder1CountLabel.setText(Encoder1CountText + " : " + Encoder1Count);

    // create integer from four bytes in buffer
    Encoder2Count = ((pMonitorBuffer[x++] << 24));
    Encoder2Count |= (pMonitorBuffer[x++] << 16) & 0x00ff0000;
    Encoder2Count |= (pMonitorBuffer[x++] << 8)  & 0x0000ff00;
    Encoder2Count |= (pMonitorBuffer[x++])       & 0x000000ff;

    Encoder2CountLabel.setText(Encoder2CountText + " : " + Encoder2Count);

}//end of Monitor::updateStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Monitor::setLabelOnOff
//
// If pOnOff is true, sets pLabel text to pDesc + " : On" and color to red.
// If pOnOff is false, sets pLabel text to pDesc + " : Off" and color to black.
//

private void setLabelOnOff(JLabel pLabel, String pDesc, Boolean pOnOff)
{

    if (pOnOff){

        pLabel.setFont(redFont);
        pLabel.setText(pDesc + " : On");

    }
    else{

        pLabel.setFont(blackFont);
        pLabel.setText(pDesc + " : Off");

    }

}//end of Monitor::setLabelOnOff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Monitor::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//

private void configure(IniFile pConfigFile)
{

    //load the names for the input display labels - this allows each type of
    //equipment to have different uses for the inputs and the labels on the
    //screen can be set to match

    input1Text = pConfigFile.readString("Monitor", "Input 1 Text", "Input 1");
    input2Text = pConfigFile.readString("Monitor", "Input 2 Text", "Input 2");
    input3Text = pConfigFile.readString("Monitor", "Input 3 Text", "Input 3");
    input4Text = pConfigFile.readString("Monitor", "Input 4 Text", "Input 4");
    input5Text = pConfigFile.readString("Monitor", "Input 5 Text", "Input 5");
    input6Text = pConfigFile.readString("Monitor", "Input 6 Text", "Input 6");
    input7Text = pConfigFile.readString("Monitor", "Input 7 Text", "Input 7");
    input8Text = pConfigFile.readString("Monitor", "Input 8 Text", "Input 8");
    input9Text = pConfigFile.readString("Monitor", "Input 9 Text", "Input 9");
    input10Text =
                pConfigFile.readString("Monitor", "Input 10 Text", "Input 10");

    Encoder1CountText =
      pConfigFile.readString("Monitor", "Encoder 1 Counter Text", "Encoder 1");
    Encoder2CountText =
      pConfigFile.readString("Monitor", "Encoder 2 Counter Text", "Encoder 2");

}//end of Monitor::configure
//-----------------------------------------------------------------------------

}//end of class Monitor
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
