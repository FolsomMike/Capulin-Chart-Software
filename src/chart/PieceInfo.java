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

import javax.swing.*;
import java.awt.*;

import java.awt.event.*;
import java.io.*;

import chart.mksystems.inifile.IniFile;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class PieceInfo
//
//

public class PieceInfo extends JDialog implements ActionListener, 
                                                            WindowListener {

JPanel panel;
String configFilename;
String primaryDataPath, backupDataPath;
String currentWorkOrder;
ActionListener actionListener;

Item[] items;
static int NUMBER_OF_ITEMS = 100;

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
// PieceInfo::PieceInfo (constructor)
//
  
public PieceInfo(JFrame pFrame, String pPrimaryDataPath, String pBackupDataPath,
                      String pCurrentWorkOrder, ActionListener pActionListener)
{

super(pFrame, "Identifier Info");

primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;
currentWorkOrder = pCurrentWorkOrder; actionListener = pActionListener;

addWindowListener(this);

//load the configuration from the primary data folder
configFilename = primaryDataPath + "05 - " + currentWorkOrder
                                    + " Configuration - Piece Info Window.ini";

//create and array to hold 100 items - each item is an data entry object
items = new Item[NUMBER_OF_ITEMS];

//setup the window according to the configuration file
configure(configFilename);

}//end of PieceInfo::PieceInfo (constructor)
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

}//end of PieceInfo::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::configure
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
try {configFile = new IniFile(pConfigFilename);}
    catch(IOException e){return;}

//scan through the array checking to see if an item exists in the configuration
//file for each index positon
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

        if (items[i].width < 1) items[i].width = 1; //range check

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
        itemPanel.add(Box.createHorizontalGlue()); //push components to the left

        //add each panel to the main panel
        panel.add(itemPanel);

        //store the maximum width of any label for use in setting all label
        //widths the same - Java seems to set Min/Preferred/Max to the same so
        //use the Preferred size for this purpose
        if (items[i].label.getPreferredSize().width > maxLabelWidth) 
            maxLabelWidth = items[i].label.getPreferredSize().width;
        
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
        items[i].label.setPreferredSize(new Dimension(maxLabelWidth, height));
        items[i].label.setMaximumSize(new Dimension(maxLabelWidth, height));
        
        }

    }//for (int i=0; i < NUMBER_OF_ITEMS; i++)


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

//clear all items which have been defined and specified for clearing for a new
//job

for (int i=0; i < NUMBER_OF_ITEMS; i++)
    if (items[i] != null && items[i].clearedInNewJob){

        items[i].textField.setText("");

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

for (int i=0; i < NUMBER_OF_ITEMS; i++)
    if (items[i] != null){

        //look in each Item.label for the matching key, return the value
        if (pKey.equals(items[i].labelText))
            return (items[i].textField.getText());

        }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

return(""); //key not found, return empty string

}//end of PieceInfo::getValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PieceInfo::loadData
//
// Loads data into the form from an ini file.
//

public void loadData(String pFilename)
{

IniFile jobInfoFile;

//if the ini file cannot be opened and loaded, exit without action
try {jobInfoFile = new IniFile(pFilename);}
    catch(IOException e){return;}

String section = "Identifying Information";

String test = "empty"; //debug mks

//load all items which have been defined

for (int i=0; i < NUMBER_OF_ITEMS; i++)
    if (items[i] != null){

        //use the label text as the key, the value is the text in the box
        items[i].textField.setText(
                       jobInfoFile.readString(section, items[i].labelText, ""));

        
        test = items[i].textField.getText();
        
        
        }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

}//end of PieceInfo::loadData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobInfo::saveData
//
// Saves data from the form to an ini file.  For quicker and more efficient
// saving during runtime, see saveDataToStream.
//

public void saveData(String pFilename)
{

IniFile jobInfoFile;

//if the ini file cannot be opened and loaded, exit without action
try {jobInfoFile = new IniFile(pFilename);}
    catch(IOException e){return;}

String section = "Identifying Information";

//save all items which have been defined

for (int i=0; i < NUMBER_OF_ITEMS; i++)
    if (items[i] != null){

        //use the label text as the key, the value is the text in the box
        jobInfoFile.writeString(section, items[i].labelText,
                                                items[i].textField.getText());

        }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

jobInfoFile.save();  //save to disk

}//end of JobInfo::saveData
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

for (int i=0; i < NUMBER_OF_ITEMS; i++)
    if (items[i] != null){
 
    pOut.write(items[i].labelText + "=" + items[i].textField.getText());
    pOut.newLine();
      
    }// for (int i=0; i < NUMBER_OF_ITEMS; i++)

}//end of PieceInfo::saveDataToStream
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

//debug mks    
    
//save the file to both data folders
//saveData(primaryDataPath);
//saveData(backupDataPath);

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

}//end of class PieceInfo
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
