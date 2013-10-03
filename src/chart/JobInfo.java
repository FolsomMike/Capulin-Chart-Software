/******************************************************************************
* Title: JobInfo.java
* Author: Mike Schoonover
* Date: 5/10/09
*
* Purpose:
*
* This class displays a dialog window for entering and editing information
* for a job.
*
* The screen is totally configurable with the labels for each entry box and
* the width of the box being loaded from a file.
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
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Item
//
// Holds the info for one entry item which is an input box with a label.
//

class Item extends Object{

    public String labelText;
    public int width;
    public int height;
    public int numberCharacters;
    public boolean editable;
    public boolean clearedInNewJob;
    public JLabel label;
    public JTextField textField;

//-----------------------------------------------------------------------------
// Item::createTextField
//

void createTextField()
{

    textField = new JTextField(numberCharacters);

    int dHeight = textField.getPreferredSize().height;

    //set the width to 1 pixel - Java will override this to make the field large
    //enough to hold the specified number of characters but prevents it from
    //enlarging the field to fill its container

    textField.setMinimumSize(new Dimension(1, dHeight));
    textField.setPreferredSize(new Dimension(1, dHeight));
    textField.setMaximumSize(new Dimension(1, dHeight));

}//end of Item::createTextField
//-----------------------------------------------------------------------------


}//end of class Item
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class JobInfo
//
//

public class JobInfo extends JDialog implements ActionListener, WindowListener {

    JPanel panel;
    String configFilename;
    String primaryDataPath, backupDataPath;
    String currentWorkOrder;
    ActionListener actionListener;
    String fileFormat;

    Item[] items;
    static int NUMBER_OF_ITEMS = 100;

//-----------------------------------------------------------------------------
// JobInfo::JobInfo (constructor)
//

public JobInfo(JFrame pFrame, String pPrimaryDataPath, String pBackupDataPath,
                      String pCurrentWorkOrder, ActionListener pActionListener,
                      String pFileFormat)
{

    super(pFrame, "Job Info");

    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    currentWorkOrder = pCurrentWorkOrder; actionListener = pActionListener;
    fileFormat = pFileFormat;

}//end of JobInfo::JobInfo (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::init
//
// Initializes new objects. Should be called immediately after instantiation.
//
//

public void init()
{

    addWindowListener(this);

    //load the configuration from the primary data folder
    configFilename = primaryDataPath + "04 - " + currentWorkOrder
                                      + " Configuration - Job Info Window.ini";

    //create and array to hold 100 items - each item is an data entry object
    items = new Item[NUMBER_OF_ITEMS];

    //setup the window according to the configuration file
    configure(configFilename);
    //load the data
    loadData();

}//end of JobInfo::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

}//end of JobInfo::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//

private void configure(String pConfigFilename)
{

    //create a panel to hold the labels and data entry boxes
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setOpaque(true);
    add(panel);

    JPanel itemPanel; //panels to hold label and text field
    int maxLabelWidth = 0;

    IniFile configFile;
    String section, text;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        configFile = new IniFile(pConfigFilename, fileFormat);
        configFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 180");
        return;
    }

    //scan through the array checking to see if an item exists in the
    //configuration file for each index positon
    for (int i=0; i < NUMBER_OF_ITEMS; i++){

        section = "Item " + (i+1);

        //see if a label exists for the index
        text = configFile.readString(section, "Label", "blank");

        //if a label exists, create the item and load the other data for it
        if (!text.equalsIgnoreCase("blank")) {

            items[i] = new Item();
            items[i].labelText = text;
            items[i].numberCharacters =
                        configFile.readInt(section, "Number of Characters", 20);
            items[i].editable =
                        configFile.readBoolean(section, "Editable", true);
            items[i].clearedInNewJob =
                  configFile.readBoolean(section, "Cleared in a New Job", true);

            if (items[i].width < 1) {items[i].width = 1;} //range check

            //add each label/field pair to a panel
            itemPanel = new JPanel();
            itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
            items[i].createTextField();
            items[i].label = new JLabel(items[i].labelText);
            //space at left edge
            itemPanel.add(Box.createRigidArea(new Dimension(5,0)));
            itemPanel.add(items[i].label);
            //space between label and field
            itemPanel.add(Box.createRigidArea(new Dimension(5,0)));
            itemPanel.add(items[i].textField);
            //push components to the left
            itemPanel.add(Box.createHorizontalGlue());

            //add each panel to the main panel
            panel.add(itemPanel);

            //store the maximum width of any label for use in setting all label
            //widths the same - Java seems to set Min/Preferred/Max to the same
            //so use the Preferred size for this purpose
            if (items[i].label.getPreferredSize().width > maxLabelWidth) {
                maxLabelWidth = items[i].label.getPreferredSize().width;
            }

        }// if (text.equalsIgnoreCase("blank"))

    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

    int height;

    //set all label widths to that of the widest label to align the fields
    for (int i=0; i < NUMBER_OF_ITEMS; i++){

        if (items[i] != null){

            //get the default height of the label
            height = items[i].label.getPreferredSize().height;

            //set all dimensions to match the widest label
            items[i].label.setMinimumSize(new Dimension(maxLabelWidth, height));
            items[i].label.setPreferredSize(
                                         new Dimension(maxLabelWidth, height));
            items[i].label.setMaximumSize(new Dimension(maxLabelWidth, height));

        }

    }//for (int i=0; i < NUMBER_OF_ITEMS; i++)

    pack();

}//end of JobInfo::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::prepareForNewJob
//
// Prepares the job info for use with a new job.  Certain entries are blanked
// as they should always be filled in for each job.  The entries to be blanked
// are specified in the configuration file.
//
// Also, the data paths and current work order are updated.
//

public void prepareForNewJob(String pPrimaryDataPath, String pBackupDataPath,
                                                      String pCurrentWorkOrder)
{

    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    currentWorkOrder = pCurrentWorkOrder;

    //clear all items which have been defined and specified for clearing for a
    //new job

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null && items[i].clearedInNewJob){
            items[i].textField.setText("");
        }
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

    //save the file to both data folders
    saveData(primaryDataPath);
    saveData(backupDataPath);

}//end of JobInfo::prepareForNewJob
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::getValue
//
// Returns the value in the Item array associated with the pKey.  The key is
// stored in the Item.label member while the value is stored in Item.textField
// member.
//
// Returns an empty string if key cannot be found.
//

public String getValue(String pKey)
{

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

            //look in each Item.label for the matching key, return the value
            if (pKey.equals(items[i].labelText)) {
                return (items[i].textField.getText());
            }

        }
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

    return(""); //key not found, return empty string

}//end of JobInfo::getValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::loadData
//
// Loads data into the form.
//

public void loadData()
{

    String jobInfoFilename = primaryDataPath + "03 - " + currentWorkOrder
                                                             + " Job Info.ini";

    IniFile jobInfoFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        jobInfoFile = new IniFile(jobInfoFilename, fileFormat);
        jobInfoFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 339");
        return;
    }

    String section = "Job Info";

    //load all items which have been defined

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

            //use the label text as the key, the value is the text in the box
            items[i].textField.setText(
                      jobInfoFile.readString(section, items[i].labelText, ""));

        }
    }// if (items[i] != null)

}//end of JobInfo::loadData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::saveData
//
// Saves data from the form.
//

public void saveData(String pDataPath)
{

    String jobInfoFilename = pDataPath + "03 - " + currentWorkOrder
                                                              + " Job Info.ini";

    IniFile jobInfoFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        jobInfoFile = new IniFile(jobInfoFilename, fileFormat);
        jobInfoFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 376");
        return;
    }

    String section = "Job Info";

    //save all items which have been defined

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

            //use the label text as the key, the value is the text in the box
            jobInfoFile.writeString(section, items[i].labelText,
                                                  items[i].textField.getText());

            }
    }// if (items[i] != null)

    jobInfoFile.save();  //save to disk

}//end of JobInfo::saveData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::windowClosing
//
// Handles actions necessary when the window is closed by clicking on the "X"
// icon or by dispatching a WINDOW_CLOSING event from code triggered by the
// "File/Exit" option.
//

@Override
public void windowClosing(WindowEvent e)
{

    //save the file to both data folders
    saveData(primaryDataPath);
    saveData(backupDataPath);

}//end of JobInfo::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::(various window listener functions)
//
// These functions are implemented per requirements of interface WindowListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void windowClosed(WindowEvent e){}
@Override
public void windowOpened(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}
@Override
public void windowActivated(WindowEvent e){}
@Override
public void windowDeactivated(WindowEvent e){}

//end of JobInfo::(various window listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of JobInfo::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of JobInfo::logStackTrace
//-----------------------------------------------------------------------------

}//end of class JobInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
