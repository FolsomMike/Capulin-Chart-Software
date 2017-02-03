/******************************************************************************
* Title: ViewerReporter.java
* Author: Mike Schoonover
* Date: 8/15/12
*
* Purpose:
*
* This class provides common functionality for viewing a piece, printing its
* graphs, and printing reports.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.hardware.HardwareVars;
import chart.mksystems.hardware.TraceValueCalculator;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.ChartGroup;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.*;



//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ViewerReporter
//

public class ViewerReporter implements ActionListener, TraceValueCalculator {

    Settings settings;
    JobInfo jobInfo;
    public HardwareVars hdwVs;
    String jobPrimaryPath, jobBackupPath, currentJobName;
    JScrollPane scrollPane;
    JPanel chartGroupPanel;

    int pieceToPrint;
    boolean isCalPiece;

    JFrame mainFrame;
    int loadSegmentError;
    String segmentDataVersion;

    String inspectionDirection = "";

    int numberOfChartGroups;
    ChartGroup[] chartGroups;

    DecimalFormat[] decimalFormats;
    int currentSegmentNumber;

    PieceInfo pieceIDInfo;

    ViewerControlPanel controlPanel;

    PrintProgress printProgress;

    PrintRequestAttributeSet aset;
    PrinterJob job;

    String fileCreationTimeStamp = "";

    PrintRunnable printRunnable;

    int startPiece = 0, endPiece = 0, pieceTrack = 0;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class PrintRunnable
//
// This is run by the thread which handles printing.
//

class PrintRunnable implements Runnable {

    PrintRequestAttributeSet aset;

    boolean inJobPrintMethod = false;
    boolean startPrint = false;
    boolean pauseThreadFlag = false;

//-----------------------------------------------------------------------------
// PrintRunnable::PrintRunnable (constructor)
//

public PrintRunnable(PrintRequestAttributeSet pAset)
{

    aset = pAset;

}//end of PrintRunnable::PrintRunnable (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::run
//

@Override
public void run() {

    while (true){

        try{
            waitForPrintTrigger();
            }
        catch (InterruptedException e) {
            //kill the thread if interrupted from another object during wait
            return;
            }

        //start printing - Java will call the print function of the object
        //specified in the call to job.setPrintable (done above) which must
        //implement the Printable interface

        try{

            //disable the print buttons using thread safe code
            controlPanel.setEnabledButtonsThreadSafe(false);

            inJobPrintMethod = true;
            job.print(aset);
            inJobPrintMethod = false;

            //if the thread was interrupted while in the job.print method, kill
            //the thread immediately
            if (Thread.interrupted()) {return;}

            //set label to default and close the printProgress window
            printProgress.setLabel("Printing...");
            printProgress.setVisible(false);

            //enable the print buttons using thread safe code
            controlPanel.setEnabledButtonsThreadSafe(true);

            }
        catch (PrinterException e) {
            displayErrorMessage("Error sending to printer."); //debug mks wrap this in thread safe code
            }

        }//while(true)


}//end of PrintRunnable::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::waitForPrintTrigger
//
// Enters a wait state until another thread calls triggerPrint which sets
// startPrint true and uses notifyAll to wake this thread up.
//

public synchronized void waitForPrintTrigger() throws InterruptedException {

    startPrint = false;

    while (!startPrint) {
        try {
            wait();
        }
        catch (InterruptedException e) {
            throw new InterruptedException();
        }

    }//while (!printTrigger)

}//end of PrintRunnable::waitForPrintTrigger
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::triggerPrint
//
// Sets startPrint true and calls notifyAll to wake up the print thread.
//

public synchronized void triggerPrint() {

    startPrint = true;

    notifyAll();

}//end of PrintRunnable::triggerPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::pauseThread
//
// Enters a wait state until another thread calls unPauseThread which sets
// pauseThreadFlag false and uses notifyAll to wake this thread up.
//
// During printing, this thread must call loadSegment to load and display each
// segment and then printChartGroup to print them.  As these functions are not
// thread safe (partly because of Swing usage), they should be called in the
// main event thread using invokeLater.  Since invokeLater results in a delayed
// call to load the segment, this thread must wait until that operation is
// completed.  Using this method to pause the thread and unPauseThread to
// release it allows the thread to wait for the functions to complete.
//

public synchronized void pauseThread() throws InterruptedException {

    pauseThreadFlag = true;

    while (pauseThreadFlag) {
        try {
            wait();
        }
        catch (InterruptedException e) {
            throw new InterruptedException();
        }
    }//while (pausePrintThread)

}//end of PrintRunnable::pauseThread
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::unPauseThread
//
// Sets pauseThreadFlag false and calls notifyAll to wake up the print thread.
//

public synchronized void unPauseThread() {

    pauseThreadFlag = false;

    notifyAll();

}//end of PrintRunnable::unPauseThread
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRunnable::enablePrintButtonsThreadSafe
//
// Enables the "Print" and "Print Multiple" buttons.
//

public synchronized void enablePrintButtonsThreadSafe()
{

    controlPanel.setEnabledButtonsThreadSafe(true);

}//end of PrintRunnable::enablePrintButtonsThreadSafe
//-----------------------------------------------------------------------------


}//end of class PrintRunnable
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------


//-----------------------------------------------------------------------------
// ViewerReporter::ViewerReporter (constructor)
//

public ViewerReporter(Settings pSettings, JobInfo pJobInfo,
        String pJobPrimaryPath, String pJobBackupPath, String pCurrentJobName)
{

    hdwVs = new HardwareVars();

    settings = pSettings; jobInfo = pJobInfo;
    jobPrimaryPath = pJobPrimaryPath; jobBackupPath = pJobBackupPath;
    currentJobName = pCurrentJobName;

    //create various decimal formats
    decimalFormats = new DecimalFormat[3];
    decimalFormats[0] = new  DecimalFormat("0000000");
    decimalFormats[1] = new  DecimalFormat("0.0");
    decimalFormats[2] = new  DecimalFormat("0.000");

}//end of Viewer::Viewer (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::init
//
//

public void init()
{

    //create an object to hold info about each piece
    pieceIDInfo = new PieceInfo(mainFrame, jobPrimaryPath, jobBackupPath,
                           currentJobName, this, true, settings.jobFileFormat);
    pieceIDInfo.init();

}//end of ViewerReporter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::resetChartGroups
//
// Erases the chart groups and clears all data.
//

void resetChartGroups()
{

    for (int i = 0; i < numberOfChartGroups; i++) {
        chartGroups[i].resetAll();
    }

}//end of ViewerReporter::resetChartGroups
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::loadSegment
//
// Loads the data for a segment from the primary job folder.  The calibration
// and piece info are also loaded from the associated info file.
//
// This function should be called whenever a new segment is loaded for
// viewing or processing - each segment could represent a piece being monitored,
// a time period, etc.
//
// If pQuietMode is true, no error message is displayed if a file cannot be
// loaded.  This is useful for the print function which can then continue on
// to the next piece instead of freezing until the user clears the dialog
// window.
//
// If no error, returns the filename extension.
// On error loading the chart data, returns "Error: <message>".
// Error on loading piece id info or calibration data returns empty string.
//

String loadSegment(boolean pQuietMode)
{

    String segmentFilename;

    //reset the charts
    resetChartGroups();

    //inspected pieces are saved with the prefix 20 while calibration pieces are
    //saved with the prefix 30 - this forces them to be grouped together and
    //controls the order in which the types are listed when the folder is viewed
    //in alphabetical order in an explorer window

    String prefix, ext, infoExt;

    prefix = isCalSelected() ? "30 - " : "20 - ";
    ext = isCalSelected() ? ".cal" : ".dat";
    infoExt = isCalSelected() ? ".cal info" : ".info";

    segmentFilename = prefix +
                            decimalFormats[0].format(currentSegmentNumber);

    //load the cal file first so its settings can be overridden by any
    //settings in the data file which might have been different at the time
    //the data file was saved
    loadCalFile(); //load calibration settings needed for viewing

    String fullPath = jobPrimaryPath + segmentFilename + ext;

    fileCreationTimeStamp = getFileCreationDateTimeString(fullPath);

    //load the graph data
    String errorMsg = loadSegmentHelper(fullPath);

    //on error, display the message, repaint with empty chart, and exit
    if (!errorMsg.isEmpty()){
        return("Error: " + errorMsg);
        }

    //load piece info
    loadInfoHelper(jobPrimaryPath + segmentFilename + infoExt);

    return(ext);

}//end of ViewerReporter::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::loadSegmentHelper
//
// Loads the data for a segment from the specified file.  See the loadSegment
// function for more info.
//
// If there is no error, returns empty String ""
// ON error, returns the appropriate error message
//

private String loadSegmentHelper(String pFilename)
{

    //create a buffered writer stream

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader in = null;

    try{

        fileInputStream = new FileInputStream(pFilename);
        inputStreamReader = new InputStreamReader(fileInputStream,
                                                       settings.jobFileFormat);

        in = new BufferedReader(inputStreamReader);

        processHeader(in); //handle the header section

        String line = "";

        //allow each chart group to load data, pass blank line in first time,
        //thereafter it will contain the last line read from the call to
        //loadSegment and will be passed on to the following call

        for (int i = 0; i < numberOfChartGroups; i++) {
            line = chartGroups[i].loadSegment(in, line);
        }

        }// try
    catch (FileNotFoundException e){
        return("Could not find the requested file.");
        }
    catch(IOException e){
        return(e.getMessage());
        }
    finally{
        try{if (in != null) {in.close();}}
        catch(IOException e){}
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){}
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){}
        }

    return("");

}//end of ViewerReporter::loadSegmentHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::getFileCreationDateTimeString
//
// Returns the file creation timestamp for pFilename as a formatted String.
//
// On Linux systems, this returns the last modified date.
//

private String getFileCreationDateTimeString(String pFilename)
{

    Path path = Paths.get(pFilename);

    BasicFileAttributes attr;

    try{
        attr = Files.readAttributes(path, BasicFileAttributes.class);
    }catch (IOException e){
        return("");
    }

    Date date = new Date(attr.creationTime().toMillis());

    SimpleDateFormat simpleDateFormat =
                                   new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");

    String timeStamp = simpleDateFormat.format(date);

    return(timeStamp);

}//end of ViewerReporter::getFileCreationDateTimeString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::loadInfoHelper
//
// Loads the info for a segment from the specified file.  See the loadSegment
// function for more info.
//

private void loadInfoHelper(String pFilename)
{

    pieceIDInfo.loadData(pFilename);

}//end of ViewerReporter::loadInfoHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::processHeader
//
// Processes the header section of a segment data file via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//

private String processHeader(BufferedReader pIn) throws IOException

{

    String line;
    boolean success;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //read until end of file reached or "[Header Start]" section tag reached

    success = false;
    while ((line = pIn.readLine()) != null){
        if (matchAndParseString(line, "[Header Start]", "", matchSet)){
            success = true; break;
            }
        }

    if (!success) {
        throw new IOException(
                     "The file could not be read - missing header.");
    }

    //scan the header section and parse its entries

    //defaults in case not found
    segmentDataVersion = "0.0";
    hdwVs.measuredLengthText = "";
    inspectionDirection = "Unknown";

    success = false;
    while ((line = pIn.readLine()) != null){

        //stop if the end of the header is reached - header read is successful
        if (matchAndParseString(line, "[Header End]", "", matchSet)){
            success = true;
            break;
            }

        //this code is SUSPECT -- entries probably have to be in same order
        //for this to work -- needs to only set variable if line matches

        //read the "Segment Data Version" entry
        if (matchAndParseString(line, "Segment Data Version", "0.0", matchSet)) {
            segmentDataVersion = matchSet.rString1;
        }

        //read the "Measured Length" entry
        if (matchAndParseString(line, "Measured Length", "0.0", matchSet)){
            hdwVs.measuredLengthText = matchSet.rString1;
            try{hdwVs.measuredLength =
                                    Double.valueOf(hdwVs.measuredLengthText);}
            catch(NumberFormatException nfe){hdwVs.measuredLength = 0;}
        }

        //read the "Inspection Direction" entry
        if (matchAndParseString(
                           line, "Inspection Direction", "Unknown", matchSet)) {
            inspectionDirection = matchSet.rString1;
        }

    }//while ((line = pIn.readLine()) != null)


    if (!success) {
        throw new IOException(
              "The file could not be read - missing end of header.");
    }

    return(line); //should be "[Header End]" tag on success, unknown value if not

}//end of ViewerReporter::processHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::matchAndParseInt
//
// Determines if the key in pString matches pKey and parses the value to an
// integer.  If the value is invalid, pDefault will be returned.
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseInt(String pString, String pKey,
                                            int pDefault, Xfer pMatchVars)
{

    //remove whitespace & force upper case
    String ucString = pString.trim().toUpperCase();

    //if the string does not start with the key, return default value
    if (!ucString.startsWith(pKey.toUpperCase())) {
        pMatchVars.rInt1 = pDefault;
        return(false); //does not match
        }

    int indexOfEqual;

    //look for '=' symbol, if not found then return default
    if ( (indexOfEqual = pString.indexOf("=")) == -1) {
        pMatchVars.rInt1 = pDefault;
        return(true); //key matched but parse was invalid
        }

    //return the part of the line after the '=' sign - on error return default
    try{
        pMatchVars.rString1 = pString.substring(indexOfEqual + 1);
        pMatchVars.rInt1 = Integer.parseInt(pMatchVars.rString1);
        return(true); //key matched, parse valid
        }
    catch(NumberFormatException e){
        pMatchVars.rInt1 = pDefault;
        return(true); //key matched but parse was invalid
        }

}//end of ViewerReporter::matchAndParseInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::matchAndParseString
//
// Determines if the key in pString matches pKey and parses the value to a
// string.  If the value is invalid, pDefault will be returned.  If the
// value is blank, pDefault will be returned.
//
// Note: this function can also be used to determine if a string contains
//  pKey regardless of whether a value exists, thus it can be used to search
//  for a section tag such as "[section]".
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseString(String pString, String pKey,
                                          String pDefault, Xfer pMatchVars)
{

    //remove whitespace & force upper case
    String ucString = pString.trim().toUpperCase();

    //if the string does not start with the key, return default value
    if (!ucString.startsWith(pKey.toUpperCase())) {
        pMatchVars.rString1 = pDefault;
        return(false); //does not match
        }

    int indexOfEqual;

    //look for '=' symbol, if not found then return default
    if ( (indexOfEqual = pString.indexOf("=")) == -1) {
        pMatchVars.rString1 = pDefault;
        return(true); //key matched but parse was invalid
        }

    //return the part of the line after the '=' sign - on error return default
    try{
        pMatchVars.rString1 = pString.substring(indexOfEqual + 1);
        if (pMatchVars.rString1.equals("")) {pMatchVars.rString1 = pDefault;}
        return(true); //key matched, parse valid
        }
    catch(StringIndexOutOfBoundsException e){
        pMatchVars.rString1 = pDefault;
        return(true); //key matched but parse was invalid
        }

}//end of ViewerReporter::matchAndParseString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::matchAndParseString
//
// Determines if the key in pString matches pKey and parses the value to a
// boolean.  If the value is invalid, pDefault will be returned.  If the
// value is blank, pDefault will be returned.
//
// Note: this function can also be used to determine if a string contains
//  pKey regardless of whether a value exists, thus it can be used to search
//  for a section tag such as "[section]".
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseBoolean(String pString, String pKey,
                                             Boolean pDefault, Xfer pMatchVars)
{

    //remove whitespace & force upper case
    String ucString = pString.trim().toUpperCase();

    //if the string does not start with the key, return default value
    if (!ucString.startsWith(pKey.toUpperCase())) {
        pMatchVars.rBoolean1 = pDefault;
        return(false); //does not match
        }

    int indexOfEqual;

    //look for '=' symbol, if not found then return default
    if ( (indexOfEqual = pString.indexOf("=")) == -1) {
        pMatchVars.rBoolean1 = pDefault;
        return(true); //key matched but parse was invalid
        }

    //return the part of the line after the '=' sign - on error return default
    try{
        pMatchVars.rString1 = pString.substring(indexOfEqual + 1);

        //return boolean value for the value - default for any invalid value
        if (pMatchVars.rString1.equalsIgnoreCase("true")) {
            pMatchVars.rBoolean1 = true;
        }
        else if (pMatchVars.rString1.equalsIgnoreCase("false")) {
            pMatchVars.rBoolean1 = false;
        }
        else {
            pMatchVars.rBoolean1 = pDefault;
        }

        return(true); //key matched, parse valid
        }
    catch(StringIndexOutOfBoundsException e){
        pMatchVars.rBoolean1 = pDefault;
        return(true); //key matched but parse was invalid
        }

}//end of ViewerReporter::matchAndParseBoolean
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile()
{

    IniFile calFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {

        calFile = new IniFile(jobPrimaryPath + "00 - "
            + currentJobName + " Calibration File.ini", settings.jobFileFormat);
        calFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 712");
        return;
    }

    //since the Viewer does not create a Hardware object, load in any values
    //which are needed for viewing which would normally be loaded by the
    //Hardware class

    //NOTE -- debug MKS
    // THESE VALUES NEED TO BE SAVED WITH THE JOINT DATA FILE
    // AND READ FROM THERE INSTEAD OF THE CALIBRATION FILE
    // Each chart needs to store the variables in case there are multiple wall
    // charts.  If a chart doesn't use these values, then they can save random
    // values -- will be ignored when loaded for viewing/reporting.

    hdwVs.nominalWall = calFile.readDouble("Hardware", "Nominal Wall", 0.250);

    hdwVs.nominalWallChartPosition =
                calFile.readInt("Hardware", "Nominal Wall Chart Position", 50);

    hdwVs.wallChartScale =
                     calFile.readDouble("Hardware", "Wall Chart Scale", 0.002);


    //don't load settings -- a pointer to the settings is passed to the Viewer
    // and these values are shared with the main program and other viewers --
    // the settings are already loaded by the main program

    //load info for all charts
    for (int i=0; i < numberOfChartGroups; i++) {
        chartGroups[i].loadCalFile(calFile);
    }

}//end of ViewerReporter::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pX, pY.
//

static public void setSizes(JComponent pComponent, int pX, int pY)
{

    pComponent.setMinimumSize(new Dimension(pX, pY));
    pComponent.setPreferredSize(new Dimension(pX, pY));
    pComponent.setMaximumSize(new Dimension(pX, pY));

}//end of ViewerReporter::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::setSizes
//
// Sets the min, max, and preferred sizes of pJFrame to pX, pY.
//

static public void setSizes(JFrame pJFrame, int pX, int pY)
{

    pJFrame.setMinimumSize(new Dimension(pX, pY));
    pJFrame.setPreferredSize(new Dimension(pX, pY));
    pJFrame.setMaximumSize(new Dimension(pX, pY));

}//end of ViewerReporter::setSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//
// Should be overridden by subclasses to provide custom message handling.
//

public void displayErrorMessage(String pMessage)
{

}//end of ViewerReporter::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::configure
//
// Creates a panel containing the appropriate chart groups to hold the data
// from a saved file.
//
// The panel is not added to any container -- the chart groups and their
// associated charts and traces can thus be used stand alone for printing
// or analyizing their data.
//

public void configure()
{

    IniFile configFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        configFile = new IniFile(jobPrimaryPath + "01 - " + currentJobName
                                + " Configuration.ini", settings.jobFileFormat);
        configFile.init();
        }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 815");
            return;
        }

    numberOfChartGroups =
         configFile.readInt("Main Configuration", "Number of Chart Groups", 1);


    //this panel will be used to hold the chart group so that they can easily
    //be added to a scrollpane
    chartGroupPanel = new JPanel();

    //create an array of chart groups per the config file setting
    if (numberOfChartGroups > 0){

        //protect against too many groups
        if (numberOfChartGroups > 10) {numberOfChartGroups = 10;}

        chartGroups = new ChartGroup[numberOfChartGroups];

        //pass null for the hardware object as that object is not needed for
        //viewing

        for (int i = 0; i < numberOfChartGroups; i++){
            chartGroups[i] = new ChartGroup(settings, mainFrame,
                            configFile, i, null /*hardware*/, this, true, this);
            chartGroupPanel.add(chartGroups[i]);
            }

        }//if (numberOfChartGroups > 0)

    //NOTE -- debug MKS
    // pixelsPerInch VALUES NEED TO BE SAVED WITH THE JOINT DATA FILE
    // AND READ FROM THERE INSTEAD OF THE CONFIG FILE

    hdwVs.pixelsPerInch =
                    configFile.readDouble("Hardware", "Pixels per Inch", 1.0);

    hdwVs.decimalFeetPerPixel = 1/(hdwVs.pixelsPerInch * 12);

}//end of ViewerReporter::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{


}//end of ViewerReporter::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::calculateComputedValue1
//
// For this version, calculates the wall thickness based upon the cursor Y
// position.
//
// This function is duplicated in multiple objects.  Should make a separate
// class which each of those objects creates to avoid duplication?
//

@Override
public double calculateComputedValue1(int pCursorY)
{

    double offset = (hdwVs.nominalWallChartPosition - pCursorY)
                                                        * hdwVs.wallChartScale;

    //calculate wall at cursor y position relative to nominal wall value
    return (hdwVs.nominalWall + offset);

}//end of ViewerReporter::calculateComputedValue1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::getLinearDecimalFeetPerPixel
//
// Returns the decimal feet represented by each pixel in the linear axis.
//

@Override
public double getLinearDecimalFeetPerPixel()
{

    return(hdwVs.decimalFeetPerPixel);

}//end of ViewerReporter::getLinearDecimalFeetPerPixel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::isCalSelected
//
// Gets the state of the Cal joint switch.  This function should be overridden
// by subclasses to return the value of whatever object they are using to
// select the Cal state.
//

public boolean isCalSelected()
{

    return(false);

}//end of ViewerReporter::isCalSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of ViewerReporter::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerReporter::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of ViewerReporter::logStackTrace
//-----------------------------------------------------------------------------

}//end of class ViewerReporter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
