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

import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ThreadSafeLogger
//

public class ThreadSafeLogger {
    
JTextArea log;    

String[] messages; //stores messages to be displayed by main thread
int messagePtr = 0; //tracks message position for new messages
int mainThreadMessagePtr = 0; //points next message in the array to be displayed
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
// Board::logMessage
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

messages[messagePtr++] = pMessage;
if (messagePtr == NUMBER_THREADSAFE_MESSAGES) messagePtr = 0;

 //store the message where the helper can find it

//Schedule a job for the event-dispatching thread: 
//creating and showing this application's GUI. 
    
javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() { logMessageThreadSafe(); } }); 

}//end of  Board::logMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::logMessageThreadSafe
//
// This function is passed to invokeLater by threadSafeLog so that it will be
// run by the main Java thread and display the stored message on the log
// window.
// 
//

public void logMessageThreadSafe()
{

// Since this function will be invoked once for every message placed in the
// array, no need to check if there is a message available?  Would be a problem
// if the calling thread began to overwrite the buffer before it could be
// displayed?

//display the next message stored in the array
log.append(messages[mainThreadMessagePtr++]);

if (mainThreadMessagePtr == NUMBER_THREADSAFE_MESSAGES)
    mainThreadMessagePtr = 0;

}//end of  Board::logMessageThreadSafe
//-----------------------------------------------------------------------------


}//end of class ThreadSafeLogger
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
