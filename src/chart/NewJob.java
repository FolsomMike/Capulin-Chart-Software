/******************************************************************************
* Title: NewJob.java
* Author: Mike Schoonover
* Date: 4/23/09
*
* Purpose:
*
* This class displays a window and handles creating a new job.
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
// class NewJob
//
// See notes at top of page.
//

class NewJob extends JDialog implements ActionListener{

    JFrame frame;
    JTextField jobName;
    JComboBox configSelect;
    ArrayList<String> configList;
    JComboBox presetSelect;
    ArrayList<String> presetList;
    Xfer xfer;
    String primaryDataPath, backupDataPath;
    String fileFormat;

//-----------------------------------------------------------------------------
// NewJob::NewJob (constructor)
//
//

public NewJob(JFrame pFrame, String pPrimaryDataPath, String pBackupDataPath,
                                                 Xfer pXfer, String pFileFormat)
{

    super(pFrame, "Create New Job");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;
    fileFormat = pFileFormat;

}//end of NewJob::NewJob (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

protected void init()
{

    xfer.rBoolean1 = false; //new job created flag

    setModal(true); //window always on top and has focus until closed

    loadConfigList(); //retrieve a list of configuration files
    loadPresetList(); //retrieve a list of preset files

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    namePanel.add(new Label("Job Name  "));
    namePanel.add(jobName = new JTextField(30));
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(namePanel);

    add(Box.createRigidArea(new Dimension(0,5)));

    //drop down selection list for configuration
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    configSelect = new JComboBox(configList.toArray());
    tPanel.add(configSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for configuration
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    presetSelect = new JComboBox(presetList.toArray());
    tPanel.add(presetSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Create"));
    button.setToolTipText("Create New Job");
    button.setActionCommand("Create");
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

}//end of NewJob::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::loadConfigList
//
// Loads a list of the available configurations for selecton by the user.
//

private void loadConfigList()
{

    //directory containing the various configuration files
    File configDir = new File("configurations");
    //get a list of the configuration files in the directory
    String[] configs = configDir.list();

    //create a list to hold the configuration filenames
    configList = new ArrayList<>();
    configList.addAll(Arrays.asList(configs));
    //sort the configurations alphabetically
    Collections.sort(configList);

    //after sorting, add the instruction text at the top so it will be displayed
    configList.add(0, "Select a Configuration (required)");

}//end of NewJob::loadConfigList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::loadPresetList
//
// Loads a list of the available presets for selecton by the user.
// The selected preset will become the "Calibration File.ini" for the new job.
// This file stores the user calibration settings - the use of presets save
// time for the user.
//

private void loadPresetList()
{

    //directory containing the various preset files
    File presetDir = new File("presets");
    //get a list of the preset files in the directory
    String[] presets = presetDir.list();

    //create a list to hold the preset filenames
    presetList = new ArrayList<>();
    presetList.addAll(Arrays.asList(presets));
    //sort the presets alphabetically
    Collections.sort(presetList);

    //after sorting, add the instruction text at the top so it will be displayed
    presetList.add(0, "Select a Preset (optional)");

}//end of NewJob::loadPresetList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Create")){
        createJob();
        //if the create was successful, close the window
        if (xfer.rBoolean1){
            setVisible(false);
            dispose();  //destroy the dialog window
        }

        return;
    }

    if (source.getActionCommand().equalsIgnoreCase("Cancel")){
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
    }

}//end of NewJob::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::createJob
//
// Creates the job if all entries are valid and closes the window.  Displays
// error window if invalid entries present.
//

void createJob()
{

    String newJobName = jobName.getText();
    String configName = (String)configSelect.getSelectedItem();
    String presetName = (String)presetSelect.getSelectedItem();

    //if the user has not entered a job name, display an error
    if (newJobName.equals("")){
        JOptionPane.showMessageDialog(frame,
        "The Job Name cannot be blank.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    //if the user has entered an illegal character, display an error
    //illegal characters    <>/?:"\|*
    //these cannot be used to create filename or folders in Windows

    if (!validateFilename(newJobName)){
        JOptionPane.showMessageDialog(frame,
        "The Job Name cannot contain:  <>/?:\"\\|*",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    //if the user has not selected a configuration, display an error message
    if (configName.equalsIgnoreCase("Select a Configuration (required)")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Configuration.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    File primaryFolder = new File (primaryDataPath + newJobName);
    File backupFolder  = new File (backupDataPath + newJobName);

    //display error if a folder with the job name already exists in either
    //directory
    if (primaryFolder.exists() || backupFolder.exists()) {
        JOptionPane.showMessageDialog(frame,
        "A folder with that name already exists " +
        "in the primary and/or backup directories.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    //try to create a folder in both directories, error on fail
    if (!primaryFolder.mkdirs() || !backupFolder.mkdirs()){
        JOptionPane.showMessageDialog(frame,
        "The job folder could not be created in " +
        "in the primary and/or backup directories.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    //put a copy of the Job Info Window configuration file into the job folder
    //this makes sure that the same configuration file will always be used when
    //the job is loaded
    //note that the "04 - " prefix is to force the file near the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (!copyFile("Configuration - Job Info Window.ini",
                    primaryFolder + "/04 - " + newJobName
                    + " Configuration - Job Info Window.ini")
     ||
        !copyFile("Configuration - Job Info Window.ini",
                    backupFolder + "/04 - " + newJobName
                    + " Configuration - Job Info Window.ini")){

            JOptionPane.showMessageDialog(frame,
            "The Job Info Window Configuration file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    //put a copy of the Piece Info Window configuration file into the job folder
    //this makes sure that the same configuration file will always be used when
    //the job is loaded
    //note that the "05 - " prefix is to force the file near the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (!copyFile("Configuration - Piece Info Window.ini",
                    primaryFolder + "/05 - " + newJobName
                    + " Configuration - Piece Info Window.ini")
     ||
        !copyFile("Configuration - Piece Info Window.ini",
                    backupFolder + "/05 - " + newJobName
                    + " Configuration - Piece Info Window.ini")){

            JOptionPane.showMessageDialog(frame,
            "The Piece Info Window Configuration file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    //put a copy of the selected configuration file into the job folder
    //this makes sure that the same configuration file will always be used when
    //the job is loaded
    //note that the "01 - " prefix is to force the file to the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (!copyFile("configurations" + "/" + configName,
                  primaryFolder + "/01 - " + newJobName + " Configuration.ini")
     ||
        !copyFile("configurations" + "/" + configName,
                 backupFolder + "/01 - " + newJobName + " Configuration.ini")){

            JOptionPane.showMessageDialog(frame,
            "The configuration file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    //determine if the user has selected a preset
    boolean presetSelected =
                     !presetName.equalsIgnoreCase("Select a Preset (optional)");

    //put a copy of the selected preset file into the job folder
    //this is the Job Info file and stores user settings
    //note that the "00 - " prefix is to force the file to the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (presetSelected){
       if( (!copyFile("presets" + "/" + presetName,
               primaryFolder + "/00 - " + newJobName + " Calibration File.ini")
     ||
        !copyFile("presets" + "/" + presetName,
              backupFolder + "/00 - " + newJobName + " Calibration File.ini"))){

            JOptionPane.showMessageDialog(frame,
            "The preset file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    else{
        //if no preset selected for copying, then create a blank place holder
        //file so error won't be generated the first time the job is opened
        createPlaceHolderCalFile(
               primaryFolder + "/00 - " + newJobName + " Calibration File.ini");
        createPlaceHolderCalFile(
               backupFolder + "/00 - " + newJobName + " Calibration File.ini");
    }

    //create the "Piece Number File.ini" file in the new folders with starting
    //values of 1 for the next inspection piece and next cal piece numbers

    try{

        ControlPanel.saveSettingsHelper(
                            primaryFolder + "/", newJobName, 1, 1, fileFormat);
        ControlPanel.saveSettingsHelper(
                            backupFolder + "/", newJobName, 1, 1, fileFormat);
    }
    catch(IOException e){
        logSevere(e.getMessage());
    }

    //signal the class which invoked this window that a new job or jobs have
    //been created and pass back the name of the last valid job created

    xfer.rBoolean1 = true; //set new job created flag to true
    xfer.rString1 = newJobName; //pass back the last job name created

}//end of NewJob::createJob
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::validateFilename
//
// Checks pString for characters which are not allowed for file or folder names.
//
// Returns true if the string is valid for use, false otherwise.
//

boolean validateFilename(String pString)
{

    //the matches function for the String class could not be used since it
    //compares the entire string - Internet search suggest using a Whitelist
    //rather than a Blacklist

    if (pString.contains("<")  ||
        pString.contains(">")  ||
        pString.contains("/")  ||
        pString.contains("?")  ||
        pString.contains(":")  ||
        pString.contains("\"") ||
        pString.contains("\\") ||
        pString.contains("|")  ||
        pString.contains("*") ) {

        return false;
    }
    else {
        return true;
    }

}//end of NewJob::validateFilename
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::copyFile
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

        while ((c = in.read()) != -1) {out.write(c); }

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 461");
        return (false);
    }
    finally {
        try{
            if (in != null) {in.close();}
            if (out != null) {out.close();}
            }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 470");
            return(false);
        }
    }

    return(true);

}//end of NewJob::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::createPlaceHolderCalFile
//
// Creates an empty Calibration file in the new job folder.  This done so that
// an error will not be displayed the first time the job is loaded due to a
// missing calibration file.  The empty file will cause defaults to be used
// for the cal settings.  After the job is opened for the first time, a
// complete cal file will be saved.
//
// It is only necessary to place the place holder in the primary folder.
//

void createPlaceHolderCalFile(String pFilename)
{

    PrintWriter file = null;

    try{
            file = new PrintWriter(new FileWriter(pFilename, true));
    }
    catch(IOException e){

            //no need to worry about error here -- an error will be generated
            //when the job is first opened due to the missing cal file, but a
            //new valid file will then be saved for the job

    }

    //don't put any text in the file or it will remain there even when the
    //new proper cal file is created as it appends rather than overwrites

    file.println("\n");

    file.close();

}//end of NewJob::createPlaceHolderCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of NewJob::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of NewJob::logStackTrace
//-----------------------------------------------------------------------------

}//end of class NewJob
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
