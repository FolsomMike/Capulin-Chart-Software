/******************************************************************************
* Title: RenamePreset.java
* Author: Mike Schoonover
* Date: 1/02/10
*
* Purpose:
*
* This class displays a window and handles renaming a new preset.
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
import java.io.*;
import java.util.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class RenamePreset
//
// See notes at top of page.
//

class RenamePreset extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox presetSelect;
    ArrayList<String> presetList;
    JTextField newNameEntry;
    Xfer xfer;
    String primaryDataPath, backupDataPath;

//-----------------------------------------------------------------------------
// RenamePreset::RenamePreset (constructor)
//
//

public RenamePreset(JFrame pFrame, String pPrimaryDataPath,
                                            String pBackupDataPath, Xfer pXfer)
{

    super(pFrame, "Rename Preset");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;

}//end of RenamePreset::RenamePreset (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RenamePreset::init
//

public void init()
{

    setModal(true); //window always on top and has focus until closed

    xfer.rBoolean1 = false; //action completed flag - set true if user completes

    loadPresetList(); //retrieve a list of available items

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for jobs
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    presetSelect = new JComboBox(presetList.toArray());
    tPanel.add(presetSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,5)));

    JPanel namePanel = new JPanel();
    namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.LINE_AXIS));
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    namePanel.add(new Label("New Name  "));
    namePanel.add(newNameEntry = new JTextField(30));
    namePanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(namePanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Rename"));
    button.setToolTipText("Rename the selected preset.");
    button.setActionCommand("Rename");
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

}//end of RenamePreset::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RenamePreset::loadPresetList
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
    presetList = new ArrayList<String>();
    presetList.addAll(Arrays.asList(configs));
    //sort the items alphabetically
    Collections.sort(presetList);

    //after sorting, add the instruction text at the top so it will be displayed
    presetList.add(0, "Select a Preset to Rename");

}//end of RenamePreset::loadPresetList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RenamePreset::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Rename")){
        boolean finished = renamePreset();
        if (!finished) return;
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
// RenamePreset::renamePreset
//
// Renames the selected preset
//
// Returns true if the action was successful and the dialog should be closed.
//

boolean renamePreset()
{

    String selectedItemName = (String)presetSelect.getSelectedItem();
    String newName = newNameEntry.getText();

    boolean presetSelected;

    //if the user has not selected a configuration, display an error message
    if (selectedItemName.equalsIgnoreCase("Select a Preset to Rename")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Preset.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }
    else
        presetSelected = true;

    //if the user has not entered a job name, display an error
    if (newName.equals("")){
        JOptionPane.showMessageDialog(frame,
        "The New Name entry cannot be blank.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }

    //if the user has entered an illegal character, display an error
    //illegal characters    <>/?:"\|*
    //these cannot be used to create filename or folders in Windows

    if (!validateFilename(newName)){
        JOptionPane.showMessageDialog(frame,
        "The Preset name cannot contain:  <>/?:\"\\|*",
        "Error", JOptionPane.ERROR_MESSAGE);
        return(false);
    }

    int n = JOptionPane.showConfirmDialog(
        frame,
        "Are you sure you want to rename this preset?",
        "Confirm",
        JOptionPane.YES_NO_OPTION);

    if (n != JOptionPane.YES_OPTION) return(false);  //bail out if user cancels

    //if the name to be used for the preset does not end with ".preset" then add
    //it as a suffix to make the presets more identifiable
    if (!newName.toLowerCase().endsWith(".preset")) newName += ".preset";

    File presetFile = new File ("presets/" + selectedItemName);
    File newFile = new File ("presets/" + newName);

    //rename the selected preset to the "New Name" entry

    if (presetSelected && !presetFile.renameTo(newFile)){

            JOptionPane.showMessageDialog(frame,
            "The preset file could not be renamed.",
            "Error", JOptionPane.ERROR_MESSAGE);
            return(false);

    }

    //signal the class which invoked this window that user has acted and pass back
    //the name of the file/folder acted on

    xfer.rBoolean1 = true; //set action completed flag true
    xfer.rString1 = selectedItemName; //pass back the target file/folder name

    return(true);

}//end of RenamePreset::renamePreset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RenamePreset::validateFilename
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

    if (pString.contains("<")  ||
        pString.contains(">")  ||
        pString.contains("/")  ||
        pString.contains("?")  ||
        pString.contains(":")  ||
        pString.contains("\"") ||
        pString.contains("\\") ||
        pString.contains("|")  ||
        pString.contains("*") )

        return false;
    else
        return true;

}//end of RenamePreset::validateFilename
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RenamePreset::copyFile
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
        System.err.println(getClass().getName() + " - Error: 327");
        return (false);
    }
    finally {
        try{
            if (in != null) in.close();
            if (out != null) out.close();
        }
        catch(IOException e){
            System.err.println(getClass().getName() + " - Error: 336");
            return(false);
        }
    }

    return(true);

}//end of RenamePreset::copyFile
//-----------------------------------------------------------------------------

}//end of class RenamePreset
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
