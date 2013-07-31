/******************************************************************************
* Title: Log.java
* Author: Mike Schoonover
* Date: 4/23/09
*
* Purpose:
*
* This class handles logging messages to a Log class window in a thread
* safe manner.  Each thread needing to log messages should create an object
* of this class, passing it a pointer to the log window which is shared by
* several threads.
*
* Any messages logged in this class are stored in a buffer in case the thread
* logs another message before the main Java thread has a chance to log any
* previous messages.
*
* The invokeLater function is used to trigger the main Java thread to log the
* next message in the buffer when that main thread next runs.  Thus, each thread
* has its own object of this class, each with a buffer of messages, each asking
* the main Java thread to log messages during the next thread run.  Since each
* thread does  nothing more than ask the main Java thread to log the message,
* the logging is thread safe.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.io.*;
import java.util.Date;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ThreadSafeLogger
//

public class ThreadSafeLogger {

    JTextArea log;

    String filenameSuffix;

    String[] messages; //stores messages to be displayed by main thread
    int messagePtr = 0; //tracks message position for new messages
    int mainThreadMessagePtr = 0; //points next message in array to be displayed
    static int NUMBER_THREADSAFE_MESSAGES = 100;

//-----------------------------------------------------------------------------
// ThreadSafeLogger::ThreadSafeLogger (constructor)
//
// Pass the Log window for displaying messages via pLog.
//

public ThreadSafeLogger(JTextArea pLog)
{

    log = pLog;

    messages = new String[NUMBER_THREADSAFE_MESSAGES];

}//end of ThreadSafeLogger::ThreadSafeLogger (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::logMessage
//
// This function allows a thread to add a log entry to the log window.  The
// actual call is passed to the invokeLater function so it will be safely
// executed by the main Java thread.
//
// Messages are stored in a circular buffer so that the calling thead does
// not overwrite the previous message before the main thread can process it.
//

public void logMessage(String pMessage)
{

    //store the message in a buffer where the helper can find it

    messages[messagePtr++] = pMessage;
    if (messagePtr == NUMBER_THREADSAFE_MESSAGES) {messagePtr = 0;}

    //schedule a job for the event-dispatching thread to add the message to log

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { logMessageThreadSafe(); } });

}//end of ThreadSafeLogger::logMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::logMessageThreadSafe
//
// This function is passed to invokeLater by threadSafeLog so that it will be
// run by the main Java thread and display the stored message on the log
// window.
//
//

public void logMessageThreadSafe()
{

    // Since this function will be invoked once for every message placed in the
    // array, no need to check if there is a message available?  Would be a
    // problem if the calling thread began to overwrite the buffer before it
    // could be displayed?

    //display the next message stored in the array
    log.append(messages[mainThreadMessagePtr++]);

    if (mainThreadMessagePtr == NUMBER_THREADSAFE_MESSAGES) {
        mainThreadMessagePtr = 0;
    }

}//end of ThreadSafeLogger::logMessageThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::separate
//
// Write a separator (such as a line of dashes) to the file.
//

public void separate()
{

    logMessage(
      "--------------------------------------------------------------------\n");

}//end of ThreadSafeLogger::separator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::date
//
// Write the date to the file.
//

public void date()
{

    logMessage(new Date().toString() + "\n");

}//end of ThreadSafeLogger::date
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::section
//
// Writes a blank line, a separator, the date, a blank line.
//

public void section()
{

    logMessage("\n");
    separate();
    date();
    logMessage("\n");

}//end of ThreadSafeLogger::section
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::saveToFile
//
// Saves the current log window contents to a disk file in a thread safe manner.
//
// The filename will be the date stamp with pFilenameSuffix appended and .txt
// for the extension.
//
// The file will be saved to the "Log Files" folder in the root program
// folder.
//

public void saveToFile(String pFilenameSuffix)
{

    //store the suffix in a buffer where the helper can find it

    filenameSuffix = pFilenameSuffix;

    //Schedule a job for the event-dispatching thread to do actual saving

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { saveToFileThreadSafe(); } });

}//end of ThreadSafeLogger::saveToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::saveToFileThreadSafe
//
// Saves the current log window contents to a disk file in a thread safe manner.
//
// The filename will be the date stamp with pFilenameSuffix appended and .txt
// for the extension.
//
// The file will be saved to the "Log Files" folder in the root program
// folder.
//

public void saveToFileThreadSafe()
{

    String lineSeparator = System.getProperty("line.separator");

    String filename =
        "Log Files" + File.separator + new Date().toString().replace(":", ".")
         + " ~ " + filenameSuffix + ".txt";

    PrintWriter file = null;

    try{
        file = new PrintWriter(new FileWriter(filename, true));

        file.println(log.getText().replace("\n", lineSeparator));

        if (file != null) {file.close();}

    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 233");
        if (file != null) {file.close();}
    }

}//end of ThreadSafeLogger::saveToFileThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ThreadSafeLogger::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

    JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of ThreadSafeLogger::displayErrorMessage
//-----------------------------------------------------------------------------


}//end of class ThreadSafeLogger
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
