/******************************************************************************
* Title: LoadPreset.java
* Author: Mike Schoonover
* Date: 1/02/10
*
* Purpose:
*
* This class displays a window and handles switching to a new preset.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class LoadPreset
//
// See notes at top of page.
//

class LoadPreset extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox <String>presetSelect;
    ArrayList<String> presetList;
    Xfer xfer;
    String primaryDataPath, backupDataPath;
    String currentJobName;

//-----------------------------------------------------------------------------
// LoadPreset::LoadPreset (constructor)
//
//

public LoadPreset(JFrame pFrame, String pPrimaryDataPath,
                     String pBackupDataPath, Xfer pXfer, String pCurrentJobName)
{

    super(pFrame, "Load Preset");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;
    currentJobName = pCurrentJobName;

}//end of LoadPreset::LoadPreset (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::init
//

public void init()
{

    xfer.rBoolean1 = false; //action completed flag - set true if user completes

    setModal(true); //window always on top and has focus until closed

    loadPresetList(); //retrieve a list of available items

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for presets
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    presetSelect =
            new JComboBox<>(presetList.toArray(new String[presetList.size()]));
    tPanel.add(presetSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Load"));
    button.setToolTipText("Load the selected preset.");
    button.setActionCommand("Load");
    button.addActionListener(this);

    buttonPanel.add(Box.createHorizontalGlue()); //force space between buttons

    buttonPanel.add(button = new JButton("Cancel"));
    button.setToolTipText("Cancel");
    button.setActionCommand("Cancel");
    button.addActionListener(this);
    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));

    add(buttonPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    pack();

    setVisible(true);

}//end of LoadPreset::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::loadPresetList
//
// Loads a list of the available presets for selecton by the user.
//

final void loadPresetList()
{

    //directory containing the pertinent files
    File jobDir = new File("presets");
    //get a list of the files/folders in the directory
    String[] configs = jobDir.list();

    //create a list to hold the items
    presetList = new ArrayList<>(1000);
    presetList.addAll(Arrays.asList(configs));
    //sort the items alphabetically
    Collections.sort(presetList);

    //after sorting, add the instruction text at the top so it will be displayed
    presetList.add(0, "Select a Preset");

}//end of LoadPreset::loadPresetList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Load")){
        boolean finished = loadSelectedPreset();
        if (!finished) {return;}
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
        }

    if (source.getActionCommand().equalsIgnoreCase("Cancel")){
         setVisible(false);
         dispose();  //destroy the dialog window
        return;
        }

}//end of Change::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::loadSelectedPreset
//
// Loads the selected preset.
//
// Returns true if the action was successful and the dialog should be closed.
//

boolean loadSelectedPreset()
{

    String selectedItemName = (String)presetSelect.getSelectedItem();

    boolean presetSelected;

    //if the user has not selected a configuration, display an error message
    if (selectedItemName.equalsIgnoreCase("Select a Preset")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Preset.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
        }
    else {
        presetSelected = true;
    }

    int n = JOptionPane.showConfirmDialog(
        frame,
        "All of your current settings will be lost!  " +
        "Are you sure you want to load this preset?",
        "Confirm",
        JOptionPane.YES_NO_OPTION);

    if (n != JOptionPane.YES_OPTION){return(false);} //bail out if user cancels

    File primaryFolder = new File (primaryDataPath + currentJobName);
    File backupFolder  = new File (backupDataPath + currentJobName);

    //copy the selected preset file into the job folder
    //this is the Job Info file and stores user settings
    //note that the "00 - " prefix is to force the file to the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (presetSelected &&
        (!copyFile("presets" + "/" + selectedItemName, primaryFolder +
                           "/00 - " + currentJobName + " Calibration File.ini")
     ||
        !copyFile("presets" + "/" + selectedItemName, backupFolder +
                    "/00 - " + currentJobName + " Calibration File.ini"))){

            JOptionPane.showMessageDialog(frame,
            "The preset file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
            return(false);

        }

    //signal the class which invoked this window that user has acted and pass
    //back the name of the file/folder acted on

    xfer.rBoolean1 = true; //set action completed flag true
    xfer.rString1 = selectedItemName; //pass back the target file/folder name

    return(true);

}//end of LoadPreset::LoadSelectedPreset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::copyFile
//
// Copies file pSource to pDest.
//
// Returns true if the copy was successful, false otherwise.
//

boolean copyFile(String pSource, String pDest)
{

    FileInputStream in = null;
    FileOutputStream out = null;

    try {

        in = new FileInputStream(pSource);
        out = new FileOutputStream(pDest);

        int c;

        while ((c = in.read()) != -1) {
            out.write(c); }

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 272");
        return (false);
    }
    finally {
        try{
            if (in != null) {in.close();}
            if (out != null) {out.close();}
        }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 280");
            return(false);
        }
    }

    return(true);

}//end of LoadPreset::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of LoadPreset::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadPreset::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of LoadPreset::logStackTrace
//-----------------------------------------------------------------------------

}//end of class LoadPreset
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
