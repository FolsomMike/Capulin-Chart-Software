/******************************************************************************
* Title: PieceInfo.java
* Author: Mike Schoonover
* Date: 8/16/11
*
* Purpose:
*
* This class displays a dialog window for entering and editing information
* for an inspected piece.
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
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class PieceInfo
//
//

public class PieceInfo extends JDialog implements ActionListener,
                                                WindowListener, FocusListener {

    JPanel panel;
    String configFilename;
    String primaryDataPath, backupDataPath;
    String currentWorkOrder;
    ActionListener actionListener;
    int prevOrderedLow;
    int prevPrintedPosition;
    boolean displayUpdateButton;
    JButton updateButton;
    String filename;
    String fileFormat;

    PieceInfoItem[] items;
    static int NUMBER_OF_ITEMS = 100;

//-----------------------------------------------------------------------------
// PieceInfo::PieceInfo (constructor)
//

public PieceInfo(JFrame pFrame, String pPrimaryDataPath, String pBackupDataPath,
                      String pCurrentWorkOrder, ActionListener pActionListener,
                      boolean pDisplayUpdateButton, String pFileFormat)
{

    super(pFrame, "Identifier Info");

    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
    currentWorkOrder = pCurrentWorkOrder; actionListener = pActionListener;
    displayUpdateButton = pDisplayUpdateButton;
    fileFormat = pFileFormat;

}//end of PieceInfo::PieceInfo (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::init
//

public void init()
{

    addWindowListener(this);

    //load the configuration from the primary data folder
    configFilename = primaryDataPath + "05 - " + currentWorkOrder
                                     + " Configuration - Piece Info Window.ini";

    //create and array to hold 100 items - each item is an data entry object
    items = new PieceInfoItem[NUMBER_OF_ITEMS];

    //setup the window according to the configuration file
    configure(configFilename);

}//end of PieceInfo::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//
// Notes regarding setting labels to focusable:
//
// When displayed, the Update button is disabled until the user clicks in any
// text box. It disabled again when the user clicks it to save the data.
// Upon window display, the first text box would normally gain focus as the
// labels default to non-focusable. Thus, when the text box gained focus it
// would automatically enable the Update button. Also, when the Update button
// was disabled, focus would pass back to the first text box and that act
// would immediately re-enable the Update button. By making the labels
// focusable, the first label can take the focus without enabling the update
// button. The user must explicitly click in the first text box to set the
// focus there and cause the Update button to be enabled.
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

    //the "Configuration - Piece Info Window.ini" files were saved in the
    //default character set for Windows, so this must be used to load them
    String defaultCS = Charset.defaultCharset().displayName();

    //if the ini file cannot be opened and loaded, exit without action
    try {
        configFile = new IniFile(pConfigFilename, defaultCS /*fileFormat*/);
        configFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 134");
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

            items[i] = new PieceInfoItem();
            items[i].labelText = text;
            items[i].numberCharacters =
                       configFile.readInt(section, "Number of Characters", 20);
            items[i].editable =
                        configFile.readBoolean(section, "Editable", true);
            items[i].clearedInNewJob =
                 configFile.readBoolean(section, "Cleared in a New Job", true);

            items[i].printInFooter =
                      configFile.readBoolean(section, "Print in Footer", true);
            items[i].printOrder =
                                configFile.readInt(section, "Print Order", -1);

            //range check -- is this used any more?
            if (items[i].width < 1) {items[i].width = 1;}

            //add each label/field pair to a panel
            itemPanel = new JPanel();
            itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
            items[i].createTextField(this);
            items[i].label = new JLabel(items[i].labelText);
            items[i].label.setFocusable(true); //see notes in method header
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
            items[i].label.setMinimumSize(
                                    new Dimension(maxLabelWidth, height));
            items[i].label.setPreferredSize(
                                    new Dimension(maxLabelWidth, height));
            items[i].label.setMaximumSize(
                                    new Dimension(maxLabelWidth, height));

        }

    }//for (int i=0; i < NUMBER_OF_ITEMS; i++)


    //display Update button if specified -- this allows the user to save changes
    //made to the data

    if(displayUpdateButton){

        itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));

        updateButton = new JButton("Update");
        updateButton.setEnabled(false);
        updateButton.setToolTipText("Update/Save info.");
        updateButton.setActionCommand("Update Info");
        updateButton.addActionListener(this);
        itemPanel.add(updateButton);
        panel.add(itemPanel);

    }//if(displayUpdateButton)

    pack();

}//end of PieceInfo::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::prepareForNewJob
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

}//end of PieceInfo::prepareForNewJob
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::getValue
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

}//end of PieceInfo::getValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::getFirstToPrint
//
// To begin printing items in the list, call this function first to retrieve
// the first item to be printed.  Subsequent calls to getNextToPrint will
// return further items to be printed in the proper order.
//
// Returns the key and value in the Item array which is to be printed first
// for hardcopy printouts.  The key is the label to be used and the value
// is the value for that label, an example would be "Heat #" for the key and
// "X2302" for the value.  These are typically printed together.
//
// To be printed, an entry must have its printInFooter value set true in the
// configuration file.  If the entry does not exist in the file, then the
// value will be defaulted to true for compatibility with older job files.
//
// The order in which the entries are to be printed is specified by the
// printOrder variable.  If this is >= 0 for one or more entries with
// printInFooter true, then those entries will be printed first in the order
// specified by printOrder, lowest value first.  The value printOrder can be
// 0 to max int.
//
// After the above have been printed, all entries with printInFooter true and
// printOrder of -1 will be printed in the order in which they were loaded from
// the config file.
//
// Returns true if there is a value to be printed, false if not.  The key
// value pair are returned in pKeyValue.
//

public boolean getFirstToPrint(KeyValue pKeyValue)
{

    //preset values and flags to begin printing

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null) {items[i].printed = false;}
    }

    //make first call to getNextToPrint
    // subsequent calls should call that function directly instead of this one

    return(getNextToPrint(pKeyValue));

}//end of PieceInfo::getFirstToPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::getNextToPrint
//
// To begin printing items in the list, call this function first to retrieve
// the first item to be printed.  Subsequent calls to getNextToPrint will
// return further items to be printed in the proper order.
//
// Returns the key and value in the Item array which is to be printed first
// for hardcopy printouts.  The key is the label to be used and the value
// is the value for that label, an example would be "Heat #" for the key and
// "X2302" for the value.  These are typically printed together.
//
// To be printed, an entry must have its printInFooter value set true in the
// configuration file.  If the entry does not exist in the file, then the
// value will be defaulted to true for compatibility with older job files.
//
// The order in which the entries are to be printed is specified by the
// printOrder variable.  If this is >= 0 for one or more entries with
// printInFooter true, then those entries will be printed first in the order
// specified by printOrder, lowest value first.  The value printOrder can be
// 0 to max int.
//
// After the above have been printed, all entries with printInFooter true and
// printOrder of -1 will be printed in the order in which they were loaded from
// the config file.
//
// Returns true if there is a value to be printed, false if not.  The key
// value pair are returned in pKeyValue.
//

public boolean getNextToPrint(KeyValue pKeyValue)
{

    int order;
    prevPrintedPosition = -1;
    prevOrderedLow = Integer.MAX_VALUE;

    //find any printable items which have printOrder >=0, these are printed
    //first in numerical order based on their printOrder values -- find the
    //unprinted item with the lowest print order

    for (int i=0; i < NUMBER_OF_ITEMS; i++){

        if (items[i] != null && items[i].printInFooter && !items[i].printed){

            order = items[i].printOrder;

            //catch the lowest order number which is not -1, this is the first
            //to be printed -- items with -1 are not ordered
            if ((order > -1) && (order < prevOrderedLow)){
                prevOrderedLow = order; //store to catch next larger in future
                prevPrintedPosition = i; //store position
                //store the key/value for the entry to be printed
                pKeyValue.keyString = items[i].labelText;
                pKeyValue.valueString = items[i].textField.getText();
                }

        }//if (items[i] != null && items[i].printInFooter)
    }//for (int i=0; i < NUMBER_OF_ITEMS; i++)

    //if an ordered entry was found, flag it as already printed and return true
    if (prevPrintedPosition != -1) {
        items[prevPrintedPosition].printed = true;
        return(true);
    }

    //if no ordered items were found, return the first item in the list which has
    //its printInFooter flag set true

    for (int i=0; i < NUMBER_OF_ITEMS; i++){
        if (items[i] != null && items[i].printInFooter){

            //catch the first with print flag set true
            if (!items[i].printed){
                prevPrintedPosition = i; //store position
                //store the key/value for the entry to be printed
                pKeyValue.keyString = items[i].labelText;
                pKeyValue.valueString = items[i].textField.getText();
                break;
            }
        }//if (items[i] != null && items[i].printInFooter)
    }//for (int i=0; i < NUMBER_OF_ITEMS; i++)

    //if an ordered entry was found, flag it as already printed and return true
    if (prevPrintedPosition != -1) {
        items[prevPrintedPosition].printed = true;
        return(true);
    }

    //return false if no printable entry found
    return(false);

}//end of PieceInfo::getNextToPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::loadData
//
// Loads data into the form from an ini file.
//

public void loadData(String pFilename)
{

    filename = pFilename;

    IniFile jobInfoFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        jobInfoFile = new IniFile(pFilename, fileFormat);
        jobInfoFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 452");
        return;
    }

    String section = "Identifying Information";

    //load all items which have been defined

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

            //use the label text as the key, the value is the text in the box
            items[i].textField.setText(
                      jobInfoFile.readString(section, items[i].labelText, ""));

            }
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

    //disable the update button -- will be re-enabled when user clicks in a box
    if (updateButton != null) {updateButton.setEnabled(false);}

}//end of PieceInfo::loadData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::getItems
//
//
//

public PieceInfoItem[] getItems()
{

    return(items);

}//end of PieceInfo::getItems
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::saveData
//
// Saves data from the form to an ini file.  For quicker and more efficient
// saving during runtime, see saveDataToStream.
//

public void saveData(String pFilename)
{

    IniFile jobInfoFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        jobInfoFile = new IniFile(pFilename, fileFormat);
        jobInfoFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 504");
        return;
    }

    String section = "Identifying Information";

    //save all items which have been defined

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

            //use the label text as the key, the value is the text in the box
            jobInfoFile.writeString(section, items[i].labelText,
                                                 items[i].textField.getText());

        }
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

    jobInfoFile.save();  //save to disk

}//end of PieceInfo::saveData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::saveDataToStream
//
// Saves data from the form to an open stream.  This is a quick and efficient
// way to save the data.  See saveData to save the data to an iniFile instead.
//

public void saveDataToStream(BufferedWriter pOut) throws IOException
{

    //save section name

    pOut.write("[Identifying Information]"); pOut.newLine();
    pOut.newLine();

    //save all items which have been defined

    for (int i=0; i < NUMBER_OF_ITEMS; i++) {
        if (items[i] != null){

        pOut.write(items[i].labelText + "=" + items[i].textField.getText());
        pOut.newLine();

    }
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

}//end of PieceInfo::saveDataToStream
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Update Info".equals(e.getActionCommand())) {
        if (updateButton != null) {updateButton.setEnabled(false);}
        saveData(filename);
        }

}//end of PieceInfo::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::focusGained
//
// Catches when a component gets keyboard focus.
//
//

@Override
public void focusGained(FocusEvent e)
{

    //if the Update button is in place (such as when window is displayed from
    //the viewer), it gets enabled whenever the user clicks in any of the
    //textfields so the data can be saved -- the enabled button also serves as
    //a visual clue that data has been modified

    if (e.getComponent().getName().equals("Value Text Field")){
        if (updateButton != null) {updateButton.setEnabled(true);}
    }

}//end of PieceInfo::focusGained
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::focusLost
//
// Catches when a compenent loses keyboard focus.
//
//

@Override
public void focusLost(FocusEvent e)
{

}//end of PieceInfo::focusLost
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::windowClosing
//
// Handles actions necessary when the window is closed by clicking on the "X"
// icon or by dispatching a WINDOW_CLOSING event from code triggered by the
// "File/Exit" option.
//

@Override
public void windowClosing(WindowEvent e)
{

}//end of PieceInfo::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::(various window listener functions)
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

//end of PieceInfo::(various window listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of PieceInfo::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of PieceInfo::logStackTrace
//-----------------------------------------------------------------------------

}//end of class PieceInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
