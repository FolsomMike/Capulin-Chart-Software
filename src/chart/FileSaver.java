/******************************************************************************
* Title: FileSaver.java
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

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ControlBoard
//
// See notes at top of page.
//

class FileSaver extends Thread{

static MainWindow mainWindow;
static MessageWindow messageWindow;

//-----------------------------------------------------------------------------
// FileSaver::FileSaver (constructor)
//

public FileSaver(MainWindow pMainWindow)
{

mainWindow = pMainWindow;

}//end of FileSaver::FileSaver (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileSaver::run
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
mainWindow.saveCalFileHelper(mainWindow.currentJobPrimaryPath);

//tell the GUI to update the status message
javax.swing.SwingUtilities.invokeLater(
    new Runnable() {
    @Override
    public void run() {
        messageWindow.message.setText("Saving data to backup folder...");}});

//begin saving the data to the backup path
mainWindow.saveCalFileHelper(mainWindow.currentJobBackupPath);

//tell the GUI to dispose of the message window
javax.swing.SwingUtilities.invokeLater(
    new Runnable() {
    @Override
    public void run() {disposeOfMessageWindow();}});

//this thread is done - set the pointer to this object in mainWindow to null
//so this object will be disposed of
mainWindow.fileSaver = null;

}//end of FileSaver::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileSaver::createMessageWindow
//
// Create the message window. For thread safety, this method should be invoked
// from the event-dispatching thread.  This is usually done by using
// invokeLater to schedule this funtion to be called from inside the event-
// dispatching thread.  This is necessary because this thread object is not
// operating in the event-dispatching thread.
//

private static void createMessageWindow(String pMessage)
{

messageWindow = new MessageWindow(mainWindow.mainFrame, pMessage);

}//end of FileSaver::createMessageWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileSaver::disposeOfMessageWindow
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

}//end of FileSaver::disposeOfMessageWindow
//-----------------------------------------------------------------------------

}//end of class FileSaver
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------