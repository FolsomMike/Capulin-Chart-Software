/******************************************************************************
* Title: SavePreset.java
* Author: Mike Schoonover
* Date: 1/02/10
*
* Purpose:
*
* This class displays a window and handles saving to a preset.
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
// class SavePreset
//
// See notes at top of page.
//

class SavePreset extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox<String> presetSelect;
    ArrayList<String> presetList;
    Xfer xfer;
    String primaryDataPath, backupDataPath;
    String currentJobName;

//-----------------------------------------------------------------------------
// SavePreset::SavePreset (constructor)
//
//

public SavePreset(JFrame pFrame, String pPrimaryDataPath,
                     String pBackupDataPath, Xfer pXfer, String pCurrentJobName)
{

    super(pFrame, "Save Preset");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;
    currentJobName = pCurrentJobName;

}//end of SavePreset::SavePreset (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::init
//

public void init()
{

    setModal(true); //window always on top and has focus until closed

    xfer.rBoolean1 = false; //action completed flag - set true if user completes

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
    presetSelect.setEditable(true);
    tPanel.add(presetSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Save"));
    button.setToolTipText("Save current settings to the selected preset.");
    button.setActionCommand("Save");
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

}//end of SavePreset::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::loadPresetList
//
// Loads a list of the available presets for selecton by the user.
//

void loadPresetList()
{

    //directory containing the pertinent files
    File jobDir = new File("presets");
    //get a list of the files/folders in the directory
    String[] configs = jobDir.list();

    //create a list to hold the items
    presetList = new ArrayList<>();
    presetList.addAll(Arrays.asList(configs));
    //sort the items alphabetically
    Collections.sort(presetList);

    //after sorting, add the instruction text at the top so it will be displayed
    presetList.add(0, "Type a Preset Name or Select One");

}//end of SavePreset::loadPresetList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Save")){
        boolean finished = savePreset();
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
// SavePreset::savePreset
//
// Saves the current settings to the selected preset.
//
// Returns true if the action was successful and the dialog should be closed.
//

boolean savePreset()
{

    String selectedItemName = (String)presetSelect.getSelectedItem();

    boolean presetSelected;

    //if the user has not selected a configuration, display an error message
    if (selectedItemName.equalsIgnoreCase("Type a Preset Name or Select One")){
        JOptionPane.showMessageDialog(frame,
        "You must enter a Preset name.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }
    else {
        presetSelected = true;
    }

    //if the user has entered an illegal character, display an error
    //illegal characters    <>/?:"\|*
    //these cannot be used to create filename or folders in Windows

    if (!validateFilename(selectedItemName)){
        JOptionPane.showMessageDialog(frame,
        "The Preset name cannot contain:  <>/?:\"\\|*",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }

    //if the name to be used for the preset does not end with ".preset" then add it
    //as a suffix to make the presets more identifiable
    if (!selectedItemName.toLowerCase().endsWith(".preset")) {
        selectedItemName += ".preset";
    }

    File presetFile = new File ("presets/" + selectedItemName);

    //request confirmation to overwrite if a folder with the job name already
    //exists in either directory
    if (presetFile.exists()) {

        int n = JOptionPane.showConfirmDialog(
            frame,
            "A Preset with that name already exists. Do you want to overwrite it?",
            "Confirm Overwrite",
            JOptionPane.YES_NO_OPTION);

        if (n != JOptionPane.YES_OPTION){return(false);} //bail if user cancels

    }

    File primaryFolder = new File (primaryDataPath + currentJobName);

    //copy the selected preset file from the job folder to the Presets folder
    //this is the Job Calibration file and stores user settings
    //note that the "00 - " prefix is to force the file to the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (presetSelected &&
        (!copyFile(primaryFolder + "/00 - "
                          + currentJobName + " Calibration File.ini",
                                         "presets" + "/" + selectedItemName))){

            JOptionPane.showMessageDialog(frame,
            "The preset file could not be copied " +
            "to the Preset folder.",
            "Error", JOptionPane.ERROR_MESSAGE);
            return(false);

    }

    //signal the class which invoked this window that user has acted and pass
    //back the name of the file/folder acted on

    xfer.rBoolean1 = true; //set action completed flag true
    xfer.rString1 = selectedItemName; //pass back the target file/folder name

    return(true);

}//end of SavePreset::savePreset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::validateFilename
//
// Checks pString for characters which are not allowed for file or folder names.
//
// Returns true if the string is valid for use, false otherwise.
//

boolean validateFilename(String pString)
{

//the matches function for the String class could not be used since it compares
//the entire string - Internet search suggest using a Whitelist rather than a
//Blacklist
    
    
    return( 
            !pString.contains("<") && 
            !pString.contains(">") && 
            !pString.contains("/") && 
            !pString.contains("?") && 
            !pString.contains(":") && 
            !pString.contains("\"") && 
            !pString.contains("\\") && 
            !pString.contains("|") && 
            !pString.contains("*"));

}//end of SavePreset::validateFilename
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::copyFile
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
        logSevere(e.getMessage() + " - Error: 325");
        return (false);
    }
    finally {
        try{
            if (in != null) {in.close();}
            if (out != null) {out.close();}
        }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 334");
            return(false);
        }
    }

    return(true);

}//end of SavePreset::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of SavePreset::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SavePreset::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of SavePreset::logStackTrace
//-----------------------------------------------------------------------------

}//end of class SavePreset
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
