/******************************************************************************
* Title: DeletePreset.java
* Author: Mike Schoonover
* Date: 1/02/10
*
* Purpose:
*
* This class displays a window and handles deleting of presets.
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
// class DeletePreset
//
// See notes at top of page.
//

class DeletePreset extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox <String>presetSelect;
    ArrayList<String> presetList;
    Xfer xfer;
    String primaryDataPath, backupDataPath;

//-----------------------------------------------------------------------------
// DeletePreset::DeletePreset (constructor)
//
//

public DeletePreset(JFrame pFrame, String pPrimaryDataPath,
                                            String pBackupDataPath, Xfer pXfer)
{

    super(pFrame, "Delete Preset");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;

}//end of DeletePreset::DeletePreset (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DeletePreset::init
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
    tPanel.add(presetSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Delete"));
    button.setToolTipText("Delete the selected preset.");
    button.setActionCommand("Delete");
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

}//end of DeletePreset::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DeletePreset::loadPresetList
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
    presetList.add(0, "Select a Preset");

}//end of DeletePreset::loadPresetList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DeletePreset::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Delete")){
        boolean finished = deletePreset();
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
// DeletePreset::deletePreset
//
// Deletes the selected preset.
//
// Returns true if the action was successful and the dialog should be closed
//

boolean deletePreset()
{

    String selectedItemName = (String)presetSelect.getSelectedItem();

    //if the user has not selected a configuration, display an error message
    if (selectedItemName.equalsIgnoreCase("Select a Preset")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Preset.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }

    int n = JOptionPane.showConfirmDialog(
        frame,
        "Are you sure you want to delete this preset?",
        "Confirm",
        JOptionPane.YES_NO_OPTION);

    if (n != JOptionPane.YES_OPTION) {return(false);}//bail out if user cancels

    String presetName = (String)presetSelect.getSelectedItem();

    File presetFile = new File ("presets/" + presetName);

    presetFile.delete();

    //signal the class which invoked this window that user has acted and pass back
    //the name of the file/folder acted on

    xfer.rBoolean1 = true; //set action completed flag true
    xfer.rString1 = selectedItemName; //pass back the target file/folder name

    return(true);

}//end of DeletePreset::deletePreset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DeletePreset::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of DeletePreset::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DeletePreset::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of DeletePreset::logStackTrace
//-----------------------------------------------------------------------------

}//end of class DeletePreset
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
