/******************************************************************************
* Title: CalFileSaver.java
* Author: Mike Schoonover
* Date: 5/8/10
*
* Purpose:
*
* Saves all user calibration data using a background thread. A more human
* readable version can also be saved. Note that the standard format is also
* in text format and can be viewed in any text editor, but it is not organized
* or titled for clarity of viewing.
*
* This class implements Runnable so it can execute as a thread.  It is used to
* solve a problem frequently encountered in Java - displaying update messages
* while performing a lengthy task.
*
* When saving a file in direct response to a user input in the main GUI thread,
* it is not possible to display updated error messages to track the progress.
* This is because the main GUI thread handles the actual painting of the
* message component.  If the GUI thread is busy saving the file, it will not
* display the message until after the save is done because the save has the
* thread busy.  Even if the message is displayed first and the file saving
* started afterwards - both using invokeLater to force the GUI thread to
* perform each action - the GUI will usually begin the file save before
* displaying the message.
*
* This class solves the problem by using invokeLater to cause the main GUI
* thread to display the message at its convenience.  Meanwhile, this class
* begins the save process in a separate thread. Thus the GUI thread is never
* tied up saving data.  This leaves the GUI thread free to do the necessary
* message painting.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.hardware.Hardware;
import chart.mksystems.hardware.HardwareVars;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlBoard
//
// See notes at top of page.
//

public class CalFileSaver extends Thread{

    Settings settings;
    Hardware hardware;
    HardwareVars hdwVs;
    boolean humanReadable;

    static JFrame mainFrame;
    static MessageWindow messageWindow;

    DecimalFormat[] decimalFormats;

//-----------------------------------------------------------------------------
// CalFileSaver::CalFileSaver (constructor)
//

public CalFileSaver(Settings pSettings, Hardware pHardware,
                                                        boolean pHumanReadable)
{

    settings = pSettings;
    hardware = pHardware;
    hdwVs = hardware.hdwVs;
    humanReadable = pHumanReadable;

    //store in a static variable so it can be used from static methods
    mainFrame = settings.mainFrame;

}//end of CalFileSaver::CalFileSaver (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //create various decimal formats
    decimalFormats = new DecimalFormat[4];
    decimalFormats[0] = new  DecimalFormat("0000000");
    decimalFormats[1] = new  DecimalFormat("0.0");
    decimalFormats[2] = new  DecimalFormat("0.00");
    decimalFormats[3] = new  DecimalFormat("0.000");

}//end of CalFileSaver::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::run
//
// This thread loads the board with FPGA and DSP code.  Using a thread allows
// multiple boards to be loaded simultaneously.
//

@Override
public void run() {

    //tell the GUI to update the status message
    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
        @Override
        public void run() {
            createMessageWindow("Saving data to primary folder...");}});

    //begin saving the data to the primary path
    if (!humanReadable) {
        saveFile(settings.currentJobPrimaryPath);
    }
    else {
        saveFileHumanReadable(settings.currentJobPrimaryPath);
    }

    //tell the GUI to update the status message
    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
        @Override
        public void run() {
           messageWindow.message.setText("Saving data to backup folder...");}});

    //begin saving the data to the backup path
    if (!humanReadable) {
        saveFile(settings.currentJobBackupPath);
    }
    else {
        saveFileHumanReadable(settings.currentJobBackupPath);
    }

    //tell the GUI to dispose of the message window
    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
        @Override
        public void run() {disposeOfMessageWindow();}});

    //this thread is done - set the pointer to this object null to dispose it
    settings.fileSaver = null;

}//end of CalFileSaver::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::saveFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// The format of this file is text, but it is not formatted or titled for
// clarity.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

protected void saveFile(String pJobPath)
{

    //if the job path has not been set, don't save anything or it will be saved in
    //the program root folder -- this occurs when the current job path specified in
    //the Main Settings.ini is blank

    if (pJobPath.equals("")) {return;}

    IniFile calFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        calFile = new IniFile(pJobPath + "00 - " + settings.currentJobName +
                              " Calibration File.ini", settings.jobFileFormat);
    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 190");
        return;
    }

    //if true, traces will restart at left edge of chart for each new piece
    //if false, new piece will be added to end of traces while chart scrolls
    calFile.writeBoolean("General",
                         "Restart Each New Piece at Left Edge of Chart",
                                           settings.restartNewPieceAtLeftEdge);

    //settings which control peak hold display on the A Scan
    calFile.writeBoolean("General", "Show Red Peak Line at Gate Center",
                                         settings.showRedPeakLineInGateCenter);
    calFile.writeBoolean("General", "Show Red Peak Line at Peak Location",
                                       settings.showRedPeakLineAtPeakLocation);
    calFile.writeBoolean("General", "Show Peak Symbol at Peak Location",
                                        settings.showPseudoPeakAtPeakLocation);
    calFile.writeBoolean("General",
            "Report all flags (do not skip duplicates at the same location)",
                                                      settings.reportAllFlags);

    calFile.writeInt(
               "General", "Scanning and Inspecting Speed", settings.scanSpeed);

    calFile.writeString("General", "Graph Print Layout",
                                                    settings.graphPrintLayout);

    calFile.writeString(
                      "General", "Graph Print Magnify",
                                                    settings.userPrintMagnify);

    //save info for all charts
    for (int i=0; i < settings.numberOfChartGroups; i++) {
        settings.chartGroups[i].saveCalFile(calFile);
    }

    hardware.saveCalFile(calFile);

    //force save buffer to disk
    calFile.save();

}//end of MainWindow::saveFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::saveFileHumanReadable
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// The format of this file is text and is formatted specifically to be easier
// to read by humans.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

protected void saveFileHumanReadable(String pJobPath)
{

    //if the job path has not been set, don't save anything or it will be saved
    //in the program root folder -- this occurs when the current job path
    //specified in the Main Settings.ini is blank

    if (pJobPath.equals("")) {return;}

    String date = new Date().toString();

    String filename = pJobPath + "00 - " + settings.currentJobName
                 + " Calibration File ~ " + date.replace(":", "-") + ".txt";

    //create a buffered writer stream
    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter out = null;

    try{

        fileOutputStream = new FileOutputStream(filename);
        outputStreamWriter =
               new OutputStreamWriter(fileOutputStream, settings.jobFileFormat);
        out = new BufferedWriter(outputStreamWriter);

        //write data to the file
        saveFileHumanReadableHelper(out);

        //Note! You MUST flush to make sure everything is written.
        out.flush();

    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 277");
    }
    finally{

        try{if (out != null) {out.close();}}
        catch(IOException e){}
        try{if (outputStreamWriter != null) {outputStreamWriter.close();}}
        catch(IOException e){}
        try{if (fileOutputStream != null) {fileOutputStream.close();}}
        catch(IOException e){}
    }

    //calculate a security hash code and append it to the file
    Settings.appendSecurityHash(filename);

}//end of MainWindow::saveFileHumanReadable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::saveFileHumanReadableHelper
//
// Saves data to the open file pOut.
//

protected void saveFileHumanReadableHelper(BufferedWriter pOut)
                                                            throws IOException
{

    pOut.newLine();
    pOut.write("Calibration Record");
    pOut.newLine();
    pOut.write("Time & Date: " + new Date().toString());
    pOut.newLine(); pOut.newLine();
    pOut.write("----- General Settings -----");
    pOut.newLine(); pOut.newLine();
    pOut.write("Job Name : " + settings.currentJobName); pOut.newLine();
    pOut.write("Calibration settings used for joint(s) : 23 to 46");
    pOut.newLine();
    pOut.write("Rotational Speed (RPM) : 179.4");
    pOut.newLine();
    pOut.write("Surface Speed (inches/second) : 40");
    pOut.newLine();
    pOut.write("Helix (inches/revolution) : .048");
    pOut.newLine();
    pOut.write("Nominal Wall Thickness : " +
                                decimalFormats[3].format(hdwVs.nominalWall));
    pOut.newLine();
    pOut.write("Nominal Wall Chart Position : " +
                                            hdwVs.nominalWallChartPosition);
    pOut.newLine();
    pOut.write("Wall Chart Scale inches/100th : " +
                            decimalFormats[3].format(hdwVs.wallChartScale));
    pOut.newLine();
    pOut.write("Sound Velocity inches/us (compression) : " +
                                decimalFormats[3].format(hdwVs.velocityUS));
    pOut.newLine();
    pOut.write("Sound Velocity inches/us (shear) : " +
                              decimalFormats[3].format(hdwVs.velocityShearUS));
    pOut.newLine();
    pOut.write("Number of Multiples between Wall Echoes : " +
                                                      hdwVs.numberOfMultiples);
    pOut.newLine();
    pOut.write("Ultrasonic Repetition Rate in Hertz : " + hdwVs.repRate);
    pOut.newLine();

    pOut.newLine();
    pOut.write("----- Chart Settings -----");
    pOut.newLine(); pOut.newLine();

    //save info for all charts
    for (int i=0; i < settings.numberOfChartGroups; i++) {
        settings.chartGroups[i].saveCalFileHumanReadable(pOut);
    }

    //save all channel settings
    hardware.saveCalFileHumanReadable(pOut);

}//end of MainWindow::saveFileHumanReadableHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::createMessageWindow
//
// Create the message window. For thread safety, this method should be invoked
// from the event-dispatching thread.  This is usually done by using
// invokeLater to schedule this funtion to be called from inside the event-
// dispatching thread.  This is necessary because this thread object is not
// operating in the event-dispatching thread.
//

private static void createMessageWindow(String pMessage)
{

    messageWindow = new MessageWindow(mainFrame, pMessage);

}//end of CalFileSaver::createMessageWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::disposeOfMessageWindow
//
// Disposes of the message window. For thread safety, this method should be
// invoked from the event-dispatching thread.  This is usually done by using
// invokeLater to schedule this funtion to be called from inside the event-
// dispatching thread.  This is necessary because this thread object is not
// operating in the event-dispatching thread.
//

private static void disposeOfMessageWindow()
{

    messageWindow.dispose();
    messageWindow = null;

}//end of CalFileSaver::disposeOfMessageWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of CalFileSaver::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CalFileSaver::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of CalFileSaver::logStackTrace
//-----------------------------------------------------------------------------

}//end of class CalFileSaver
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------