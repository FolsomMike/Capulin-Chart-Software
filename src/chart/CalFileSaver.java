/******************************************************************************
* Title: CalFileSaver.java
* Author: Mike Schoonover
* Date: 5/8/10
*
* Purpose:
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

import javax.swing.JFrame;
import java.io.IOException;

import chart.mksystems.settings.Settings;
import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlBoard
//
// See notes at top of page.
//

public class CalFileSaver extends Thread{

Settings settings;
Hardware hardware;

static JFrame mainFrame;
static MessageWindow messageWindow;

//-----------------------------------------------------------------------------
// CalFileSaver::CalFileSaver (constructor)
//

public CalFileSaver(Settings pSettings, Hardware pHardware)
{

settings = pSettings;
hardware = pHardware;

//store in a static variable so it can be used from static methods
mainFrame = settings.mainFrame;

}//end of CalFileSaver::CalFileSaver (constructor)
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
saveFile(settings.currentJobPrimaryPath);

//tell the GUI to update the status message
javax.swing.SwingUtilities.invokeLater(
    new Runnable() {
    @Override
    public void run() {
        messageWindow.message.setText("Saving data to backup folder...");}});

//begin saving the data to the backup path
saveFile(settings.currentJobBackupPath);

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
// Each object is passed a pointer to the file so that they may save their
// own data.
//

protected void saveFile(String pJobPath)
{

//if the job path has not been set, don't save anything or it will be saved in
//the program root folder -- this occurs when the current job path specified in
//the Main Settings.ini

if (pJobPath.equals("")) return;

IniFile calFile = null;

//if the ini file cannot be opened and loaded, exit without action
try {
    calFile = new IniFile(pJobPath + "00 - " + settings.currentJobName +
                            " Calibration File.ini", settings.jobFileFormat);
    }
    catch(IOException e){return;}

//if true, traces will restart at left edge of chart for each new piece
//if false, new piece will be added to end of traces while chart scrolls
calFile.writeBoolean("General", "Restart Each New Piece at Left Edge of Chart",
                                           settings.restartNewPieceAtLeftEdge);

//settings which control peak hold display on the A Scan
calFile.writeBoolean("General", "Show Red Peak Line at Gate Center",
                                        settings.showRedPeakLineInGateCenter);
calFile.writeBoolean("General", "Show Red Peak Line at Peak Location",
                                       settings.showRedPeakLineAtPeakLocation);
calFile.writeBoolean("General", "Show Peak Symbol at Peak Location",
                                        settings.showPseudoPeakAtPeakLocation);

calFile.writeInt(
               "General", "Scanning and Inspecting Speed", settings.scanSpeed);

calFile.writeString("General", "Graph Print Layout", settings.graphPrintLayout);

calFile.writeString(
                  "General", "Graph Print Magnify", settings.userPrintMagnify);

//save info for all charts
for (int i=0; i < settings.numberOfChartGroups; i++)
    settings.chartGroups[i].saveCalFile(calFile);

hardware.saveCalFile(calFile);

//force save
calFile.save();

}//end of MainWindow::saveFile
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

}//end of class CalFileSaver
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------