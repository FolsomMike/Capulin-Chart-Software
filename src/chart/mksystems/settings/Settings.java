/******************************************************************************
* Title: Settings.java
* Author: Mike Schoonover
* Date: 11/23/03
*
* Purpose:
*
* This class contains the variables used by various other classes.  It is
* intended to be instantiated by the main() function and a reference to the
* object passed to all other objects which need access to the variables.
*
* This class also handles event calls from the menu on the main form.
*
* This class also loads and saves certain option settings to the options.ini
* file.  These settings are those which affect the program globally and are not
* limited to a specific job/work order setup.  These include such things as
* language selection.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.settings;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.awt.Color;
import java.util.*;
import java.io.FileInputStream;

import chart.CalFileSaver;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.hardware.Hardware;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Settings
//
// See header at top of the page for info.
//

public class Settings extends Object implements ActionListener, ItemListener {

public JFrame mainFrame;

public Hardware hardware;

public String language;

ActionListener actionListener;

private boolean optionsModified, utSettingsModified;

public boolean scanDataModified = false;

Link mainWindow;

public int mainWindowLocationX = 0;
public int mainWindowLocationY = 0;

public int utCalWindowLocationX = 0;
public int utCalWindowLocationY = 0;

public int numberOfChartGroups;
public ChartGroup[] chartGroups;

public CalFileSaver fileSaver = null;

//Constants

public static String SOFTWARE_VERSION = "1.95";

//This is the version of the format used to save the data for a segment which
//holds data for an inspected piece.
//version 1.0 saved with the "Threshold" tag misspelled as "Theshold"
public static String SEGMENT_DATA_VERSION = "1.1";

//This is the format used for non-job files such as those in the root
//program folder, presets, config files, etc. -- older files were in UTF-16LE
//format, new ones in UTF-8.  The UTF-16LE files are now converted to UTF-8
//automatically if they are found on program start up.

public static String mainFileFormat = "UTF-8";

public String currentJobName;
public String currentJobPrimaryPath, currentJobBackupPath, reportsPath;
public String primaryDataPath;
public String backupDataPath;

//this is the format used for files stored in the job folder
//old jobs used UTF-16LE format, newer ones use UTF-8
//the format used is specified in the newer files, defaults to UTF-16LE for
//older jobs

public String jobFileFormat = "UTF-16LE";

//set this to equal the number of UT channels - each channel's values will be
//saved with an identifying number
public static int NUMBER_OF_UT_CHANNELS = 100; //debug mks - read this value from config file?

public static int INCHES = 0, MM = 1;

public static int LECOEUR = 0, ULTRATEK = 1, SOFRATEST = 2;

//wip mks - these phrases need to be loaded from config file
//the phrase used to describe the thing being inspected, i.e.
// "joint", "tube", "plate", "bar", "billet", etc.
public String pieceDescription = "Joint";
//lower case of the above
public String pieceDescriptionLC = "joint";
//lower case of the above
public String pieceDescriptionPlural = "Joints";
//lower case of the above
public String pieceDescriptionPluralLC = "joints";

String[] colorNamesArray = {
    "WHITE",
    "LIGHT_GRAY",
    "GRAY",
    "DARK_GRAY",
    "BLACK",
    "RED",
    "PINK",
    "ORANGE",
    "YELLOW",
    "GREEN",
    "MAGENTA",
    "CYAN",
    "BLUE"
    };

Color[] colorArray = {
    Color.WHITE,
    Color.LIGHT_GRAY,
    Color.GRAY,
    Color.DARK_GRAY,
    Color.BLACK,
    Color.RED,
    Color.PINK,
    Color.ORANGE,
    Color.YELLOW,
    Color.GREEN,
    Color.MAGENTA,
    Color.CYAN,
    Color.BLUE
    };

//end of Constants

//miscellaneous variables

public boolean beginExitProgram = false;
public boolean exitProgram = false;
public boolean restartProgram = false;
public boolean saveOnExit = false;

//if true, program will simulate data for debugging, training and demo
//replaced by simulateMechanical, simulateUT, etc?
public boolean simulationMode = false;
public boolean simulateMechanical = false;
public boolean timerDrivenTracking = false;

public boolean restartNewPieceAtLeftEdge = true;
public boolean showRedPeakLineInGateCenter = false;
public boolean showRedPeakLineAtPeakLocation = false;
public boolean showPseudoPeakAtPeakLocation = true;

public int scanSpeed;

public String graphPrintLayout;
public String userPrintMagnify;

public int printResolutionX, printResolutionY;
public String printQuality;

public boolean inMeasureMode = false;

public double scanXDistance;
public double scanYDistance;

//maximum size allowed for profile scan - this is determined by the size of
//the plot window, the step size, and allowable scaling.
public double maxXScan, maxYScan;

//set true if currently performing a scan
public boolean isScanning = false;

//these variables are used to find or create the root data folders
public String primaryFolderName = "";
public String backupFolderName =  "";

//function of the "Copy to All" button on the A-Scan calibration windows
// 0: copies currently selected channel to all other channels for the currently
//      selected chart
// 1: copies currently selected channel to all other channels for all charts
public int copyToAllMode;

public boolean printMode = false;

public ArrayList<String> configInfo;

//-----------------------------------------------------------------------------
// Settings::Settings (constructor)
//
// Parameter pMainWindow provides a connection back to the MainWindow which
// implements interface Link.  This allows the Settings objects to call functions
// in MainWindow objects.
//

public Settings(Link pMainWindow, ActionListener pActionListener)
{

//store parameters in persistent variables

mainWindow = pMainWindow; actionListener = pActionListener;

optionsModified = false; //no options modified yet
utSettingsModified = false; //no settings modified yet

//language defaults to English if options.ini file cannot be found
language = "English";

loadOptions(); //read option settings from ini file

//load the appropriate language text
//loadLanguage();

}//end of Settings::Settings (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::setOptionsModifiedFlag
//
// Allows optionsModified flag to be set so values will be saved to file
// when the Settings object is destroyed.
//

public void setOptionsModifiedFlag(boolean pModified)
{

optionsModified = pModified;

}//end of Settings::setOptionsModifiedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::setUTSettingsModifiedFlag
//
// Allows outside objects to set the setUTSettingsModifiedFlag to true or false
// so that other objects can know that the values have been modified and any
// necessary calculations should be performed.  This saves processing time
// since the calculations are only made when necessary.
//
// The optionsModified flag is also set so that the values will be saved to
// the ini file.
//
// Note that the objects can set the flag true or false so that it can be
// reset by an object after it processes the changes.  In either case, the
// optionsModified flag is set as it is no error to save the values to the
// ini file regardless if utSettingsModified was set true or false.
//

public void setUTSettingsModifiedFlag(boolean pModified)
{

utSettingsModified = pModified;

optionsModified = true;

}//end of Settings::setUTSettingsModifiedFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::actionPerformed
//
// Catches action events from menu.
//
// If necessary calls actionListener.actionPerformed for the parent form which
// was passed into this class via the constructor so that it can respond to
// events generated in this class.
//

@Override
public void actionPerformed(ActionEvent e)
{

JMenuItem source = (JMenuItem)(e.getSource());

if (source.getToolTipText().equalsIgnoreCase("English")) setLanguage("English");
if (source.getToolTipText().equalsIgnoreCase("Chinese")) setLanguage("Chinese");
if (source.getToolTipText().equalsIgnoreCase("Spanish")) setLanguage("Spanish");

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Job Info")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Job Info"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Save")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Save"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("New Job")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "New Job"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Change to a different job.")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Change Job"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase(
                                       "Copy settings from a different job.")){
    actionListener.actionPerformed(new ActionEvent(
                                             this, 1, "Copy Preset From Job"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase(
                                       "Save current settings as a preset.")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Save Preset"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase(
                                        "Load new settings from a preset.")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Change Preset"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Rename the selected preset.")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Rename Preset"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Delete a preset.")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Delete Preset"));
    return;
    }

//calls function in Main
if (source.getToolTipText().equalsIgnoreCase("Monitor")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Monitor"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Debugger")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Debugger"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Repair Job")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Repair Job"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Update UT Rabbit Code")){
    actionListener.actionPerformed(
                           new ActionEvent(this, 1, source.getActionCommand()));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Update Control Rabbit Code")){
    actionListener.actionPerformed(
                           new ActionEvent(this, 1, source.getActionCommand()));
    return;
    }


//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Set Up System")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Set Up System"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Renew License")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Renew License"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Log")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Log"));
    return;
    }

//calls function in Main
if (source.getActionCommand().equalsIgnoreCase("Status")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Status"));
    return;
    }

//calls function in Main
if (source.getActionCommand().startsWith("View Chart of a Completed")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "Open Viewer"));
    return;
    }

//calls function in Main
if (source.getActionCommand().startsWith("View / Edit Identifier Info")){
    actionListener.actionPerformed(new ActionEvent(
                                               this, 1, "Show ID Info Window"));
    return;
    }

//calls function in Main
if (source.getActionCommand().startsWith("About")){
    actionListener.actionPerformed(new ActionEvent(this, 1, "About"));
    return;
    }

if (source.getActionCommand().startsWith("Display Configuration Info")){
    actionListener.actionPerformed(
                        new ActionEvent(this, 1, "Display Configuration Info"));
    return;
    }

//sets a variable in Settings
if (source.getActionCommand().equalsIgnoreCase(
                            "Restart Each New Piece at Left Edge of Chart")){
    restartNewPieceAtLeftEdge =
                            ((JCheckBoxMenuItem)(e.getSource())).isSelected();
    return;
    }

//sets a variable in Settings
if (source.getActionCommand().equalsIgnoreCase(
                                        "Show Red Peak Line at Gate Center")){
    showRedPeakLineInGateCenter =
                            ((JCheckBoxMenuItem)(e.getSource())).isSelected();
    return;
    }

//sets a variable in Settings
if (source.getActionCommand().equalsIgnoreCase(
                                        "Show Red Peak Line at Peak Location")){
    showRedPeakLineAtPeakLocation =
                            ((JCheckBoxMenuItem)(e.getSource())).isSelected();
    return;
    }

//sets a variable in Settings
if (source.getActionCommand().equalsIgnoreCase(
                                        "Show Peak Symbol at Peak Location")){
    showPseudoPeakAtPeakLocation =
                            ((JCheckBoxMenuItem)(e.getSource())).isSelected();
    return;
    }

//calls function in Main
if (source.getActionCommand().
                    startsWith("Print Flag Report for Last Piece Inspected")){
    actionListener.actionPerformed(
       new ActionEvent(this, 1, "Print Flag Report for Last Piece Inspected"));
    return;
    }

//calls function in Main
if (source.getActionCommand().
                    startsWith("Print Flag Report for User Selection")){
    actionListener.actionPerformed(
       new ActionEvent(this, 1, "Print Flag Report for User Selection"));
    return;
    }

//calls function in Main
if (source.getActionCommand().
                    startsWith("View Calibration Records")){
    actionListener.actionPerformed(
                    new ActionEvent(this, 1, "View Calibration Records"));
    return;
    }

if (source.getToolTipText().equalsIgnoreCase("Exit")){
    //set a flag that lets the mainTimer event close the program - this allows
    //the timer to make sure everything is ok to close - disposing of the
    //main window arbitrarily can crash the program because the timer will
    //still be running and will try to access objects as they are destroyed
    beginExitProgram = true;
    return;
    }

}//end of Settings::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::itemStateChanged
//
// Catches item events from menu.
//
// Items are things such as Radio Buttons and Check Boxes.
//

@Override
public void itemStateChanged(ItemEvent e)
{

JMenuItem source = (JMenuItem)(e.getSource());

String s = "Item event detected."
           + "\n"
           + "    Event source: " + source.getText()
           + "\n"
           + "    New state: "
           + ((e.getStateChange() == ItemEvent.SELECTED) ?
             "selected":"unselected");

System.out.println(s); //debug mks

}//end of Settings::itemStateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::setLanguage
//
// Sets the language variable to pLanguage and sets the class variable
// "optionsModified" to true so the options will be saved.
//
// Calls the mainWindow to allow it to force all objects to update their text
// displays with the new translations.
//

private void setLanguage(String pLanguage)
{

language = pLanguage; optionsModified = true;

mainWindow.changeLanguage(pLanguage);

}//end of Settings::setLanguage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::loadOptions
//
// Loads option settings from the options.ini file.  These are global settings
// for the program.
//

private void loadOptions()
{


}//end of Settings::loadOptions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::saveOptions
//
// Saves option settings to the options.ini file.  These are global settings
// for the program.
//
// If the "optionsModified" variable is false, nothing is saved.
//

public void saveOptions()
{


}//end of Settings::saveOptions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::loadDBColors
//
// Loads the color scheme for the decibel drop.
//

public void loadDBColors(IniFile pIni)
{


}//end of Settings::loadDBColors
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::saveDBColors
//
// Saves the color scheme for the decibel drop.
//

public void saveDBColors(IniFile pIni)
{


}//end of Settings::saveDBColors
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::dBColorsToText
//
// Returns a text color name for pColor.
//

public String dBColorsToText(Color pColor)
{

//scan the color array looking for match
for(int i = 0; i < colorArray.length; i++ )
                       if (pColor == colorArray[i]) return colorNamesArray[i];

//if color not found, return "Undefined"
return("Undefined");

}//end of Settings::dBColorsToText
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::textToDBColors
//
// Returns a color matching pColor.
//

public Color textToDBColors(int pIndex, String pColor)
{

//scan the color array looking for match
for(int i = 0; i < colorNamesArray.length; i++ )
         if (pColor.equalsIgnoreCase(colorNamesArray[i])) return colorArray[i];

//if color not found, return the original default color already in the
//array slot

//return(plotScreenInfo.colorKey[pIndex]);

return Color.BLACK; //not used in this program yet

}//end of Settings::textToDBColors
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//

public void configure(IniFile pConfigFile)
{

String section = "Configuration Info";

//get the number of lines in the description, bail out if illegal value

int lineCount = pConfigFile.readInt(section, "Number of Lines", 0);
if (lineCount == 0 || lineCount > 100) return;

//create a vector to hold the lines of text read from the file
configInfo = new ArrayList<String>(lineCount);

String line, number;

for (int i = 0; i < lineCount; i++){

    //convert the index number to a string to load each line -- in the file,
    //the numbers are all 3 characters long and right justified with blanks
    //prepended for padding -- add the necessary space padding here
    number = Integer.toString(i+1);
    if (number.length() == 1) number = "  " + number;
    else
    if (number.length() == 2) number = " " + number;

    line = pConfigFile.readString(section, "Line " + number, "");
    configInfo.add(line);
    }

section = "Main Configuration";

}//end of Settings::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::displayConfigInfo
//
// Appends the lines of the configuration info to pTextArea.
//

public void displayConfigInfo(JTextArea pTextArea)
{


pTextArea.append("\n");
pTextArea.append(
            "------------------------------------------------------------\n");
pTextArea.append("\n");

ListIterator i;

for (i = configInfo.listIterator(); i.hasNext(); ){
    pTextArea.append((String)i.next() + "\n");
    }

pTextArea.append(
            "------------------------------------------------------------\n");

}//end of Settings::displayConfigInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::prePad
//
// Adds spaces to the beginning of pSource until it is length pLength.
// Returns the new string.
//

public static String prePad(String pSource, int pLength)
{

   while(pSource.length() < pLength)
       pSource = " " + pSource;

   return(pSource);

}//end of Settings::prePad
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::postPad
//
// Adds spaces to the end of pSource until it is length pLength.
// Returns the new string.
//

public static String postPad(String pSource, int pLength)
{

   while(pSource.length() < pLength)
       pSource = pSource + " ";

   return(pSource);

}//end of Settings::postPad
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::truncate
//
// Truncates pSource to pLength.  The truncated string is returned.
//

public static String truncate(String pSource, int pLength)
{

    if(pSource.length() > pLength)
       return (pSource.substring(0, pLength));
    else
       return(pSource);

}//end of Settings::truncate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::appendSecurityHash
//
// Appends a security hash code to the end of file pFilename. Unauthorized
// changes to the file can be detected as the hash code will no longer match
// the data.
//
// The data in the file is summed, the resulting value is scrambled and then
// saved at the end of the file in ASCII hexadecimal format.
//

public static void appendSecurityHash(String pFilename)
{

    long sum = calculateSumOfFileData(pFilename);

    //bail out if there was an error calculating the sum
    if (sum < 0) return;

    //wip mks -- need to add the hashcode to the file here


}//end of Settings::appendSecurityHash
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::calculateSumOfFileData
//
// The data in the file is summed, the resulting value is scrambled and then
// saved at the end of the file in ASCII hexadecimal format.
//
// If not errors encountered, the sum of the data is returned. On error, -1
// is returned.
//
// Note: the sum may experience overflow if the data is too large. This is
// not necessarily an error -- the resulting value can still be used for
// checksums, hashcodes, etc. , so long as the result is the same each time.
//

public static long calculateSumOfFileData(String pFilename)
{

    FileInputStream in = null;

    long sum = 0;

    try {

        in = new FileInputStream(pFilename);

        int c;

        while ((c = in.read()) != -1) {

            //sum all data bytes in the file
            sum += c;

        }
    }
    catch(IOException e){sum = -1;}
    finally{
        try{if (in != null) in.close();}
        catch(IOException e){}

        return(sum);
    }

}//end of Settings::calculateSumOfFileData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Settings::loadLanguage
//
// Sets the text displayed by various controls according to the selected
// language.
//

public void loadLanguage()
{

IniFile ini = null;

//if the ini file cannot be opened and loaded, exit without action
try {ini = new IniFile("language\\Globals - Capulin UT.language",
                                                            mainFileFormat);}
catch(IOException e){return;}

}//end of Settings::loadLanguage
//-----------------------------------------------------------------------------

//debug mks System.out.println(String.valueOf(value)); //debug mks

}//end of class Settings
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
