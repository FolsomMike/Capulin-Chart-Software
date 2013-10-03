/******************************************************************************
* Title: LoadConfiguration.java
* Author: Mike Schoonover
* Date: 1/02/10
*
* Purpose:
*
* This class displays a window and handles loading a configuration file.
*
* NOTE: The configuration file should not be changed after a job has been
*  used.  If it is necessary to reload the configuration file after it has
*  been damaged, the same file should be used that was selected when the job
*  was created.
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
// class LoadConfiguration
//
// See notes at top of page.
//

class LoadConfiguration extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox configSelect;
    Vector<String> configList;
    Xfer xfer;
    String currentJobPrimaryPath, currentJobBackupPath;
    String jobName;

//-----------------------------------------------------------------------------
// LoadConfiguration::LoadConfiguration (constructor)
//
//

public LoadConfiguration(JFrame pFrame, String pCurrentJobPrimaryPath,
                     String pCurrentJobBackupPath, String pJobName, Xfer pXfer)
{

    super(pFrame, "Load Configuration");

    frame = pFrame;
    currentJobPrimaryPath = pCurrentJobPrimaryPath;
    currentJobBackupPath = pCurrentJobBackupPath;
    jobName = pJobName;
    xfer = pXfer;

    xfer.rBoolean1 = false; //action completed flag - set true if user completes

    setModal(true); //window always on top and has focus until closed

    loadConfigList(); //retrieve a list of available items

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for configurations
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    configSelect = new JComboBox(configList);
    tPanel.add(configSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Load"));
    button.setToolTipText("Load the selected configuration.");
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

}//end of LoadConfiguration::LoadConfiguration (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// NewJob::loadConfigList
//
// Loads a list of the available configurations for selecton by the user.
//

final void loadConfigList()
{

    //directory containing the various configuration files
    File configDir = new File("configurations");
    //get a list of the configuration files in the directory
    String[] configs = configDir.list();

    //create a list to hold the configuration filenames
    configList = new Vector<String>();
    configList.addAll(Arrays.asList(configs));
    //sort the configurations alphabetically
    Collections.sort(configList);

    //after sorting, add the instruction text at the top so it will be displayed
    configList.add(0, "Select a Configuration");

}//end of NewJob::loadConfigList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadConfiguration::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Load")){
        boolean finished = loadSelectedConfiguration();
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
// LoadConfiguration::loadSelectedConfiguration
//
// Loads the selected configuration.
//
// Returns true if the action was successful and the dialog should be closed.
//

boolean loadSelectedConfiguration()
{

    String selectedItemName = (String)configSelect.getSelectedItem();

    boolean configSelected;

    //if the user has not selected a configuration, display an error message
    if (selectedItemName.equalsIgnoreCase("Select a Configuration")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Configuration.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }
    else {
        configSelected = true;
    }

    int n = JOptionPane.showConfirmDialog(
      frame,
      "The selected file must match the one chosen when the job was created.  "
      + "Are you sure you want to load this configuration?",
      "Confirm",
      JOptionPane.YES_NO_OPTION);

    if (n != JOptionPane.YES_OPTION){return(false);} //bail out if user cancels

    String configName = (String)configSelect.getSelectedItem();

    //put a copy of the selected configuration file into the job folder
    //this makes sure that the same configuration file will always be used when
    //the job is loaded
    //note that the "01 - " prefix is to force the file to the top of the
    //explorer window when the files are alphabetized to make it easier to find

    if (!copyFile("configurations" + "/" + configName, currentJobPrimaryPath +
                                      "/01 - " + jobName + " Configuration.ini")
     ||
        !copyFile("configurations" + "/" + configName, currentJobBackupPath +
                                   "/01 - " + jobName + " Configuration.ini")){

            JOptionPane.showMessageDialog(frame,
            "The configuration file could not be copied " +
            "to the primary and/or backup directories.",
            "Error", JOptionPane.ERROR_MESSAGE);
    }

    //signal the class which invoked this window that user has acted and pass
    //back the name of the file/folder acted on

    xfer.rBoolean1 = true; //set action completed flag true
    xfer.rString1 = selectedItemName; //pass back the target file/folder name

    return(true);

}//end of LoadConfiguration::loadSelectedConfiguration
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadConfiguration::copyFile
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
        logSevere(e.getMessage() + " - Error: 266");
        return (false);
    }
    finally {
        try{
            if (in != null) {in.close();}
            if (out != null) {out.close();}
            }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 275");
            return(false);
        }
    }

    return(true);

}//end of LoadConfiguration::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadConfiguration::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of LoadConfiguration::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LoadConfiguration::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of LoadConfiguration::logStackTrace
//-----------------------------------------------------------------------------

}//end of class LoadConfiguration
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
