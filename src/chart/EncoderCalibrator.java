/******************************************************************************
* Title: EncoderCalibrator.java
* Author: Mike Schoonover
* Date: 9/12/16
*
* Purpose:
*
* This class displays a window for calibrating the encoder counts per distance
* values.
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
import chart.mksystems.inifile.IniFile;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class EncoderCalibrator
//
// This class displays a window for calibrating the encoder counts per distance
// values.
//

class EncoderCalibrator extends JDialog implements ActionListener {

    private JPanel mainPanel;

    private Font blackFont, redFont;

    private final ActionListener actionListener;
    private final IniFile configFile;

    private int encoder1Count, encoder2Count;
    

    private double encoder1CountsPerInch, encoder2CountsPerInch;
    private double encoder1InchesPerCount, encoder2InchesPerCount;
    private double encoder1CountsPerSecInput, encoder2CountsPerSecInput;
    private double encoder1CountsPerSec, encoder2CountsPerSec;
    private double encoder1Helix;
    
    public double getEncoder1CountsPerInch(){return encoder1CountsPerInch; }
    public double getEncoder2CountsPerInch(){return encoder2CountsPerInch; }    
    public double getEncoder1InchesPerCount(){return encoder1InchesPerCount; }
    public double getEncoder2InchesPerCount(){return encoder2InchesPerCount; }    
    public double getEncoder1CountsPerSec(){return encoder1CountsPerSec; }
    public double getEncoder2CountsPerSec(){return encoder2CountsPerSec; }
    private double encoder2Helix;    

    private double encoder1CountsPerRev, encoder2CountsPerRev;
    
    private JTextField distanceEntryBox, revolutionsEntryBox;
    
    private String encoder1CountsPerInchText, encoder2CountsPerInchText;
    private JLabel encoder1CountsPerInchLbl, encoder2CountsPerInchLbl;
    
    private String encoder1CountsPerRevText, encoder2CountsPerRevText;
    private JLabel encoder1CountsPerRevLbl, encoder2CountsPerRevLbl;
        
    private JLabel encoder1CountLabel, encoder2CountLabel;
    private String encoder1CountText, encoder2CountText;
    
    JLabel encoder1CountsPerSecInputLbl, encoder2CountsPerSecInputLbl;
    JLabel encoder1CountsPerSecLbl, encoder2CountsPerSecLbl;
    String encoder1CountsPerSecText, encoder2CountsPerSecText;

    JLabel helixLbl;
    String helixLblText = "Helix inches/rev: ";
    
    private JButton linearCalStartBtn, linearCalFinishBtn;
    private JButton rotaryCalStartBtn, rotaryCalFinishBtn;
    
    JLabel countsPerSecLabel;
    
//-----------------------------------------------------------------------------
// EncoderCalibrator::EncoderCalibrator (constructor)
//
//

public EncoderCalibrator(JFrame frame, IniFile pConfigFile,
                                                ActionListener pActionListener)
{

    super(frame, "Encoder Calibration");

    actionListener = pActionListener;
    configFile = pConfigFile;

}//end of EncoderCalibrator::EncoderCalibrator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::init
//
// Initializes the object.
//

public void init()
{

    configure(configFile);

    int panelWidth = 250;
    int panelHeight = 500;

    //create red and black fonts for use with display objects
    HashMap<TextAttribute, Object> map = new HashMap<>();
    blackFont = new Font("Dialog", Font.PLAIN, 12);
    map.put(TextAttribute.FOREGROUND, Color.RED);
    redFont = blackFont.deriveFont(map);

    encoder1CountsPerInchText = encoder1CountText + " counts per inch:  ";
    encoder2CountsPerInchText = encoder2CountText + " counts per inch:  ";
    
    encoder1CountsPerRevText = encoder1CountText + " counts per revolution:  ";
    encoder2CountsPerRevText = encoder2CountText + " counts per revolution:  ";

    encoder1CountsPerSecText = encoder1CountText + " counts per second:  ";
    encoder2CountsPerSecText = encoder2CountText + " counts per second:  ";
        
    setupGUI();
    
    pack();

}//end of EncoderCalibrator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupGUI
//

private void setupGUI()
{

    mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setOpaque(true);
    add(mainPanel);

    setupEncodersPanel(mainPanel);
    
    setupOutputPanel(mainPanel);
    
    setupLinearCalPanel(mainPanel);
    
    setupRotaryCalPanel(mainPanel);
    
    setupRateCalPanel(mainPanel);
    
}//end of EncoderCalibrator::setupGUI
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupEncodersPanel
//

private void setupEncodersPanel(JPanel pPanel)
{

    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);    
    mainPanel.add(panel);
        
    setupEncoderPanel(panel, encoder1CountText,
                    encoder1CountLabel = new JLabel("Counts : "),
                    encoder1CountsPerSecInputLbl = new JLabel("Counts/sec : "));
    
    panel.add(Box.createHorizontalGlue()); //force space between buttons
    
    setupEncoderPanel(panel, encoder2CountText,
                    encoder2CountLabel = new JLabel("Counts : "),
                    encoder2CountsPerSecInputLbl = new JLabel("Counts/sec : "));
    
}//end of EncoderCalibrator::setupEncodersPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupOutputPanel
//
// Displays the results of the math performed such as counts/inch, helix, etc.
//

private void setupOutputPanel(JPanel pPanel)
{

    JPanel panel = new JPanel();
    panel.setBorder(BorderFactory.createTitledBorder("Calculations"));
    setSizes(panel, 300, 185);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainPanel.add(panel);
    
    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer
    
    encoder1CountsPerInchLbl = new JLabel(encoder1CountsPerInchText);
    encoder1CountsPerInchLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder1CountsPerInchLbl);
    
    encoder2CountsPerInchLbl = new JLabel(encoder2CountsPerInchText);
    encoder2CountsPerInchLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder2CountsPerInchLbl);
        
    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer

    encoder1CountsPerRevLbl = new JLabel(encoder1CountsPerRevText);
    encoder1CountsPerRevLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder1CountsPerRevLbl);
    
    encoder2CountsPerRevLbl = new JLabel(encoder2CountsPerRevText);
    encoder2CountsPerRevLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder2CountsPerRevLbl);
    
    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer

    encoder1CountsPerSecLbl = new JLabel(encoder1CountsPerSecText);
    encoder1CountsPerSecLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder1CountsPerSecLbl);
        
    encoder2CountsPerSecLbl = new JLabel(encoder2CountsPerSecText);
    encoder2CountsPerSecLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(encoder2CountsPerSecLbl);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer

    helixLbl = new JLabel(helixLblText);
    helixLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel.add(helixLbl);
        
    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer
        
}//end of EncoderCalibrator::setupOutputPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupLinearCalPanel
//
// Displays the controls for performing the linear calibration.
//

private void setupLinearCalPanel(JPanel pPanel)
{

    JPanel panel = new JPanel();
    panel.setBorder(
               BorderFactory.createTitledBorder("Linear Distance Calibration"));
    setSizes(panel, 300, 145);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainPanel.add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer

    linearCalStartBtn = new JButton("Start Linear Calibration");
    linearCalStartBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
    linearCalStartBtn.setActionCommand("Start Linear Cal");
    linearCalStartBtn.addActionListener(this);
    linearCalStartBtn.setToolTipText(
                        "After clicking, move tube approximately 10 feet.");
    panel.add(linearCalStartBtn);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer    
    
    JPanel entryPanel = new JPanel();
    entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.X_AXIS));
    entryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel entryLabel = new JLabel("distance moved in inches: ");
    entryPanel.add(entryLabel);
    
    distanceEntryBox = new JTextField(20);
    entryPanel.add(distanceEntryBox);
    
    panel.add(entryPanel);        

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer                

    linearCalFinishBtn = new JButton("Finish");
    linearCalFinishBtn.setAlignmentX(Component.LEFT_ALIGNMENT);    
    linearCalFinishBtn.setActionCommand("Finish Linear Cal");
    linearCalFinishBtn.addActionListener(this);
    linearCalFinishBtn.setToolTipText(
               "Enter distance moved above in inches, then click this button.");
    linearCalFinishBtn.setEnabled(false);
    panel.add(linearCalFinishBtn);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer    
    
}//end of EncoderCalibrator::setupLinearCalPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupRotaryCalPanel
//
// Displays the controls for performing the rotary calibration.
//

private void setupRotaryCalPanel(JPanel pPanel)
{

    JPanel panel = new JPanel();
    panel.setBorder(
          BorderFactory.createTitledBorder("Rotational Distance Calibration"));
    setSizes(panel, 300, 145);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainPanel.add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer

    rotaryCalStartBtn = new JButton("Start Rotational Calibration");
    rotaryCalStartBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
    rotaryCalStartBtn.setActionCommand("Start Rotary Cal");
    rotaryCalStartBtn.addActionListener(this);
    rotaryCalStartBtn.setToolTipText(
                                "After clicking, rotate tube 10 revolutions.");
    panel.add(rotaryCalStartBtn);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer    
    
    JPanel entryPanel = new JPanel();
    entryPanel.setLayout(new BoxLayout(entryPanel, BoxLayout.X_AXIS));
    entryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel entryLabel = new JLabel("number of rotations: ");
    entryPanel.add(entryLabel);
    
    revolutionsEntryBox = new JTextField(20);
    revolutionsEntryBox.setText("10");
    entryPanel.add(revolutionsEntryBox);
    
    panel.add(entryPanel);        

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer                

    rotaryCalFinishBtn = new JButton("Finish");
    rotaryCalFinishBtn.setAlignmentX(Component.LEFT_ALIGNMENT);    
    rotaryCalFinishBtn.setActionCommand("Finish Rotary Cal");
    rotaryCalFinishBtn.addActionListener(this);
    rotaryCalFinishBtn.setToolTipText(
             "Enter number of revolutions completed, then click this button.");
    rotaryCalFinishBtn.setEnabled(false);
    panel.add(rotaryCalFinishBtn);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer    
    
}//end of EncoderCalibrator::setupRotaryCalPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupRateCalPanel
//
// Displays the controls for performing the count/sec rate calibration.
//

private void setupRateCalPanel(JPanel pPanel)
{

    JPanel panel = new JPanel();
    panel.setBorder(
          BorderFactory.createTitledBorder("Encoder Rate Calibration"));
    setSizes(panel, 300, 75);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    panel.setAlignmentX(Component.LEFT_ALIGNMENT);
    mainPanel.add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer
        
    JButton setBtn = new JButton("Set Counts/Inch");
    setBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
    setBtn.setActionCommand("Set Counts per Second");
    setBtn.addActionListener(this);
    rotaryCalStartBtn.setToolTipText(
             "Start tube running at inspection speed, then click this button.");
    panel.add(setBtn);
        
    panel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer                
    
}//end of EncoderCalibrator::setupRateCalPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setupEncoderPanel
//

private void setupEncoderPanel(JPanel pPanel, String pPanelTitle, 
                JLabel pEncoderCountsLabel, JLabel pEncoderCountsPerSecLabel)
{

    JPanel subPanel = new JPanel();
    subPanel.setLayout(new BoxLayout(subPanel, BoxLayout.Y_AXIS));
    subPanel.setBorder(BorderFactory.createTitledBorder(pPanelTitle));
    setSizes(subPanel, 140, 85);
    
    subPanel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer
        
    subPanel.add(pEncoderCountsLabel);
    pEncoderCountsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    subPanel.add(Box.createRigidArea(new Dimension(0,7)));//vertical spacer    

    subPanel.add(pEncoderCountsPerSecLabel);
    pEncoderCountsPerSecLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    subPanel.add(Box.createRigidArea(new Dimension(0,10)));//vertical spacer    
        
    pPanel.add(subPanel);

}//end of EncoderCalibrator::setupEncoderPanel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setLabelWithFormattedValue
//
//

public void setLabelWithFormattedValue(JLabel pLabel, String pText, 
                                                                 double pValue)
{
    
    pLabel.setText(pText + new  DecimalFormat("#.####").format(pValue));

}//end of EncoderCalibrator::setLabelWithFormattedValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::startLinearCal
//
// Starts the linear encoder calibration process.
//

private void startLinearCal()
{

    //zero the encoder counts
    actionListener.actionPerformed(
                            new ActionEvent(this, 1, "Zero Encoder Counts"));
    
    linearCalStartBtn.setEnabled(false);
    
    linearCalFinishBtn.setEnabled(true);
    
}//end of EncoderCalibrator::startLinearCal
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::finishLinearCal
//
// Finishes the linear calibration process, calculates, and stores all values.
//

private void finishLinearCal()
{

    if (distanceEntryBox.getText().trim().isEmpty()) { return; }
    
    double distanceMoved;
    
    try{
        distanceMoved = Double.parseDouble(distanceEntryBox.getText().trim());
            
    }catch(NumberFormatException ec){
        distanceMoved = Double.MIN_VALUE;
    }
    
    encoder1CountsPerInch = encoder1Count / distanceMoved;
    encoder2CountsPerInch = encoder2Count / distanceMoved;        
    
    encoder1InchesPerCount = 0;
    if (encoder1Count > 0){
        encoder1InchesPerCount = distanceMoved / encoder1Count;
    }

    encoder2InchesPerCount = 0;
    if (encoder2Count > 0){    
        encoder2InchesPerCount = distanceMoved / encoder2Count;
    }
    
    if(distanceMoved > 0){

        refreshAllLabels(); //update all labels with current values
                        
    }else{
        encoder1CountsPerInchLbl.setText(
                          encoder1CountsPerInchText + "*** invalid entry ***");
        
        encoder2CountsPerInchLbl.setText(
                          encoder1CountsPerInchText + "*** invalid entry ***");
        
    }
    
    
    linearCalStartBtn.setEnabled(true);
    
    linearCalFinishBtn.setEnabled(false);
    
    actionListener.actionPerformed(
                new ActionEvent(this, 1, "Transfer Encoder Calibration Data"));

}//end of EncoderCalibrator::finishLinearCal
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::startRotaryCal
//
// Starts the rotary encoder calibration process.
//

private void startRotaryCal()
{

    //zero the encoder counts
    actionListener.actionPerformed(
                            new ActionEvent(this, 1, "Zero Encoder Counts"));
    
    rotaryCalStartBtn.setEnabled(false);
    
    rotaryCalFinishBtn.setEnabled(true);
    
}//end of EncoderCalibrator::startRotaryCal
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::finishRotaryCal
//
// Finishes the rotary calibration process, calculates, and stores all values.
//

private void finishRotaryCal()
{

    if (revolutionsEntryBox.getText().trim().isEmpty()) { return; }    
    
    int revsCompleted;
    
    try{
        revsCompleted = Integer.parseInt(revolutionsEntryBox.getText().trim());
    }catch(NumberFormatException ec){
        revsCompleted = Integer.MIN_VALUE;
    }

    encoder1CountsPerRev = (double)encoder1Count / (double)revsCompleted;
    encoder2CountsPerRev = (double)encoder2Count / (double)revsCompleted;
    
    if(revsCompleted > 0){
        
        refreshAllLabels(); //update all labels with current values        
                        
    }else{
        encoder1CountsPerRevLbl.setText(
                          encoder1CountsPerRevText + "*** invalid entry ***");
        
        encoder2CountsPerRevLbl.setText(
                          encoder1CountsPerRevText + "*** invalid entry ***");
        
    }
        
    rotaryCalStartBtn.setEnabled(true);
    
    rotaryCalFinishBtn.setEnabled(false);

    actionListener.actionPerformed(
                new ActionEvent(this, 1, "Transfer Encoder Calibration Data"));
        
}//end of EncoderCalibrator::finishRotaryCal
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setCountsPerSecond
//
// Stores the current counts per second for both encoders.
//

private void setCountsPerSecond()
{

    encoder1CountsPerSec = encoder1CountsPerSecInput;    
    encoder2CountsPerSec = encoder2CountsPerSecInput;
    
    refreshAllLabels(); //update all labels with current values    

    actionListener.actionPerformed(
                new ActionEvent(this, 1, "Transfer Encoder Calibration Data"));
        
}//end of EncoderCalibrator::setCountsPerSecond
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::getEncoderCalValues
//
// Returns all encoder calibrations via pEncoderCalValues. The function itself
// returns a reference to pEncoderValues.
//

public EncoderCalValues getEncoderCalValues(EncoderCalValues pEncoderCalValues)
{
 
    pEncoderCalValues.encoder1CountsPerInch = encoder1CountsPerInch;
    pEncoderCalValues.encoder1InchesPerCount = encoder1InchesPerCount;
    pEncoderCalValues.encoder1CountsPerRev = encoder1CountsPerRev;        
    pEncoderCalValues.encoder1CountsPerSec = encoder1CountsPerSec;
    pEncoderCalValues.encoder1Helix = encoder1Helix;
    
    pEncoderCalValues.encoder2CountsPerInch = encoder2CountsPerInch;
    pEncoderCalValues.encoder2InchesPerCount = encoder2InchesPerCount;
    pEncoderCalValues.encoder2CountsPerRev = encoder2CountsPerRev;        
    pEncoderCalValues.encoder2CountsPerSec = encoder2CountsPerSec;
    pEncoderCalValues.encoder2Helix = encoder2Helix;            
    
    return(pEncoderCalValues);
    
}//end of EncoderCalibrator::getEncoderCalValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setEncoderCalValues
//
// Sets all encoder calibrations via pEncoderCalValues and updates labels to
// reflect the data.
//

public void setEncoderCalValues(EncoderCalValues pEncoderCalValues)
{
 
    encoder1CountsPerInch = pEncoderCalValues.encoder1CountsPerInch;    
    encoder1InchesPerCount = pEncoderCalValues.encoder1InchesPerCount;
    encoder1CountsPerRev = pEncoderCalValues.encoder1CountsPerRev;    
    encoder1CountsPerSec = pEncoderCalValues.encoder1CountsPerSec;

    encoder2CountsPerInch = pEncoderCalValues.encoder2CountsPerInch;    
    encoder2InchesPerCount = pEncoderCalValues.encoder2InchesPerCount;
    encoder2CountsPerRev = pEncoderCalValues.encoder2CountsPerRev;        
    encoder2CountsPerSec = pEncoderCalValues.encoder2CountsPerSec;
        
    refreshAllLabels();
    
}//end of EncoderCalibrator::setEncoderCalValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::calculateHelix
//
// Calculates the helical advance rate in inches/rev and returns the values as
// a string.
//
// The helix using both encoder 1 and encoder 2 are calculated, but the only
// the value for encoder 1 is returned.
//

private String calculateHelix()
{

    if (encoder2CountsPerInch > 0 && encoder2CountsPerRev >= 0){  
        encoder2Helix = encoder2CountsPerRev / encoder2CountsPerInch;
    }else{
        encoder2Helix = 0;
    }

    if (encoder1CountsPerInch > 0 && encoder1CountsPerRev >= 0){
        encoder1Helix = encoder1CountsPerRev / encoder1CountsPerInch;
        return(new  DecimalFormat("#.##").format(encoder1Helix));
    }else{
        encoder1Helix = 0;
        return("invalid");
    }
        
}//end of EncoderCalibrator::calculateHelix
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::refreshAllLabels
//
// Updates all labels with their associated variable values.
//

public void refreshAllLabels()
{

    setLabelWithFormattedValue(encoder1CountsPerInchLbl,
                        encoder1CountsPerInchText, encoder1CountsPerInch);

    setLabelWithFormattedValue(encoder2CountsPerInchLbl,
                        encoder2CountsPerInchText, encoder2CountsPerInch);

    setLabelWithFormattedValue(encoder1CountsPerRevLbl,
                        encoder1CountsPerRevText, encoder1CountsPerRev);

    setLabelWithFormattedValue(encoder2CountsPerRevLbl,
                        encoder2CountsPerRevText, encoder2CountsPerRev);

    setLabelWithFormattedValue(encoder1CountsPerSecLbl,
                            encoder1CountsPerSecText, encoder1CountsPerSec);
        
    setLabelWithFormattedValue(encoder2CountsPerSecLbl,
                            encoder2CountsPerSecText, encoder2CountsPerSec);
                
    helixLbl.setText(helixLblText + calculateHelix());
    
}//end of EncoderCalibrator::refreshAllLabels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::actionPerformed
//
// Catches action events from buttons, etc.
//
// Passes some of them on to the specified actionListener.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Start Linear Cal".equals(e.getActionCommand())) {
        startLinearCal();
        return;
    }
    
    if ("Finish Linear Cal".equals(e.getActionCommand())) {
        finishLinearCal();
        return;
    }
    
    if ("Start Rotary Cal".equals(e.getActionCommand())) {
        startRotaryCal();
        return;
    }
    
    if ("Finish Rotary Cal".equals(e.getActionCommand())) {
        finishRotaryCal();
        return;
    }

    if ("Set Counts per Second".equals(e.getActionCommand())) {
        setCountsPerSecond();
        return;
    }

}//end of EncoderCalibrator::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::updateStatus
//
// Updates the display to show the current state of the I/O.
//

public void updateStatus(byte[] pMonitorBuffer)
{

    int x = 17; //start at buffer location of encoder counts

    // combine four bytes each to make the encoder counts

    // create integer from four bytes in buffer
    encoder1Count = ((pMonitorBuffer[x++] << 24));
    encoder1Count |= (pMonitorBuffer[x++] << 16) & 0x00ff0000;
    encoder1Count |= (pMonitorBuffer[x++] << 8)  & 0x0000ff00;
    encoder1Count |= (pMonitorBuffer[x++])       & 0x000000ff;

    encoder1CountLabel.setText("Counts : " + encoder1Count);

    // create integer from four bytes in buffer
    encoder2Count = ((pMonitorBuffer[x++] << 24));
    encoder2Count |= (pMonitorBuffer[x++] << 16) & 0x00ff0000;
    encoder2Count |= (pMonitorBuffer[x++] << 8)  & 0x0000ff00;
    encoder2Count |= (pMonitorBuffer[x++])       & 0x000000ff;

    encoder2CountLabel.setText("Counts : " + encoder2Count);

    encoder1CountsPerSecInput =  //cast to short to force sign extension
    (short)((pMonitorBuffer[x++]<<8) & 0xff00) + (pMonitorBuffer[x++] & 0xff);

    encoder1CountsPerSecInputLbl.setText(
                                  "Counts/sec : " + encoder1CountsPerSecInput);
    
    encoder2CountsPerSecInput =  //cast to short to force sign extension
    (short)((pMonitorBuffer[x++]<<8) & 0xff00) + (pMonitorBuffer[x++] & 0xff);
    
    encoder2CountsPerSecInputLbl.setText(
                                "Counts/sec : " + encoder2CountsPerSecInput);    
    
}//end of EncoderCalibrator::updateStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setLabelOnOff
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

}//end of EncoderCalibrator::setLabelOnOff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pWidth and pHeight.
//

public void setSizes(Component pComponent, int pWidth, int pHeight)
{

    pComponent.setMinimumSize(new Dimension(pWidth, pHeight));
    pComponent.setPreferredSize(new Dimension(pWidth, pHeight));
    pComponent.setMaximumSize(new Dimension(pWidth, pHeight));

}//end of EncoderCalibrator::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderCalibrator::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//

private void configure(IniFile pConfigFile)
{

    //load the names for the input display labels - this allows each type of
    //equipment to have different uses for the inputs and the labels on the
    //screen can be set to match

    encoder1CountText =
      pConfigFile.readString("Monitor", "Encoder 1 Counter Text", "Encoder 1");
    encoder2CountText =
      pConfigFile.readString("Monitor", "Encoder 2 Counter Text", "Encoder 2");

}//end of EncoderCalibrator::configure
//-----------------------------------------------------------------------------

}//end of class EncoderCalibrator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
