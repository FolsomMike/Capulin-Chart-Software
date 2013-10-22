/******************************************************************************
* Title: ChooseJob.java
* Author: Mike Schoonover
* Date: 1/01/10
*
* Purpose:
*
* This class displays a window and handles switching to a different job.
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
// class ChooseJob
//
// See notes at top of page.
//

class ChooseJob extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox <String> jobSelect;
    ArrayList<String> jobList;
    Xfer xfer;
    String primaryDataPath, backupDataPath;

//-----------------------------------------------------------------------------
// ChooseJob::ChooseJob (constructor)
//
//

public ChooseJob(JFrame pFrame, String pPrimaryDataPath, String pBackupDataPath,
                                                                     Xfer pXfer)
{

    super(pFrame, "Choose Job");

    frame = pFrame;
    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    xfer = pXfer;

}//end of ChooseJob::ChooseJob (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::init
//

public void init()
{

    xfer.rBoolean1 = false; //job switched flag - set true if user switches

    setModal(true); //window always on top and has focus until closed

    loadJobList(); //retrieve a list of available jobs

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for jobs
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    jobSelect = new JComboBox<>(jobList.toArray(new String[jobList.size()]));
    tPanel.add(jobSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Choose"));
    button.setToolTipText("Choose the selected job.");
    button.setActionCommand("Choose");
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

}//end of ChooseJob::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::loadJobList
//
// Loads a list of the available jobs for selection by the user.
//

private void loadJobList()
{

    //directory containing the various pertinent files
    File jobDir = new File(primaryDataPath);
    //get a list of the files/folders in the directory
    String[] configs = jobDir.list();

    //create a list to hold the file/folder names
    jobList = new ArrayList<>(1000);
    jobList.addAll(Arrays.asList(configs));
    //sort the items alphabetically
    Collections.sort(jobList);

    //after sorting, add the instruction text at the top so it will be displayed
    jobList.add(0, "Select a Job");

}//end of ChooseJob::loadJobList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::actionPerformed
//
// Catches action events from buttons, etc.
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Choose")){
        chooseSelectedJob();
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
        }

    if (source.getActionCommand().equalsIgnoreCase("Cancel")){
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
        }

}//end of ChooseJob::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::chooseSelectedJob
//
// Chooses the selected job.
//

void chooseSelectedJob()
{

    String selectedJobName = (String)jobSelect.getSelectedItem();

    //if the user has not selected an item, display an error message
    if (selectedJobName.equalsIgnoreCase("Select a Job")){
        JOptionPane.showMessageDialog(frame,
        "You must select a Job.",
        "Error", JOptionPane.ERROR_MESSAGE);
        return;
        }

    //signal the class which invoked this window that a different job has been
    //selected and pass back the name of that job

    xfer.rBoolean1 = true; //set job selected flag to true
    xfer.rString1 = selectedJobName; //pass back the selected job name

}//end of ChooseJob::chooseSelectedJob
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of ChooseJob::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ChooseJob::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of ChooseJob::logStackTrace
//-----------------------------------------------------------------------------

}//end of class ChooseJob
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
