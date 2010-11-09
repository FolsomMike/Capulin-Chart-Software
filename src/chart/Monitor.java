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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Monitor
//
// This class creates a window to display values being monitored for debug
// purposes.
//

class Monitor extends JDialog implements ActionListener {

JPanel panel;

ActionListener actionListener;

JLabel chassisNumLabel, boardNumLabel;
JLabel Encoder1CountLabel, Encoder2CountLabel;

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

configure(pConfigFile);

int panelWidth = 250;
int panelHeight = 500;

setMinimumSize(new Dimension(panelWidth, panelHeight));
setPreferredSize(new Dimension(panelWidth, panelHeight));
setMaximumSize(new Dimension(panelWidth, panelHeight));

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

}//end of Monitor::Monitor (constructor)
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

chassisNumLabel.setText("Chassis Number : " + pMonitorBuffer[10]);

boardNumLabel.setText("Board Number : " + pMonitorBuffer[11]);

if (pMonitorBuffer[0] == 0) input1Label.setText(input1Text + " : Off");
else input1Label.setText(input1Text + " : On");

if (pMonitorBuffer[1] == 0) input2Label.setText(input2Text + " : Off");
else input2Label.setText(input2Text + " : On");

if (pMonitorBuffer[2] == 0) input3Label.setText(input3Text + " : Off");
else input3Label.setText(input3Text + " : On");

if (pMonitorBuffer[3] == 0) input4Label.setText(input4Text + " : Off");
else input4Label.setText(input4Text + " : On");

if (pMonitorBuffer[4] == 0) input5Label.setText(input5Text + " : Off");
else input5Label.setText(input5Text + " : On");

if (pMonitorBuffer[5] == 0) input6Label.setText(input6Text + " : Off");
else input6Label.setText(input6Text + " : On");

if (pMonitorBuffer[6] == 0) input7Label.setText(input7Text + " : Off");
else input7Label.setText(input7Text + " : On");

if (pMonitorBuffer[7] == 0) input8Label.setText(input8Text + " : Off");
else input8Label.setText(input8Text + " : On");

if (pMonitorBuffer[8] == 0) input9Label.setText(input9Text + " : Off");
else input9Label.setText(input9Text + " : On");

if (pMonitorBuffer[9] == 0) input10Label.setText(input10Text + " : Off");
else input10Label.setText(input10Text + " : On");

// combine four bytes each to make the encoder counts

int Encoder1Count = 0, Encoder2Count = 0;

// create integer from four bytes in buffer
Encoder1Count = ((pMonitorBuffer[12] << 24));
Encoder1Count |= (pMonitorBuffer[13] << 16) & 0x00ff0000;
Encoder1Count |= (pMonitorBuffer[14] << 8)  & 0x0000ff00;
Encoder1Count |= (pMonitorBuffer[15])       & 0x000000ff;

Encoder1CountLabel.setText(Encoder1CountText + " : " + Encoder1Count);

// create integer from four bytes in buffer
Encoder2Count = ((pMonitorBuffer[16] << 24));
Encoder2Count |= (pMonitorBuffer[17] << 16) & 0x00ff0000;
Encoder2Count |= (pMonitorBuffer[18] << 8)  & 0x0000ff00;
Encoder2Count |= (pMonitorBuffer[19])       & 0x000000ff;

Encoder2CountLabel.setText(Encoder2CountText + " : " + Encoder2Count);

}//end of Monitor::updateStatus
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
input10Text = pConfigFile.readString("Monitor", "Input 10 Text", "Input 10");

Encoder1CountText = 
    pConfigFile.readString("Monitor", "Encoder 1 Counter Text", "Encoder 1");
Encoder2CountText = 
    pConfigFile.readString("Monitor", "Encoder 2 Counter Text", "Encoder 2");

}//end of Monitor::configure
//-----------------------------------------------------------------------------

}//end of class Monitor
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
