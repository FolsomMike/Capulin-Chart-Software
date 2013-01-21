/******************************************************************************
* Title: LogFile.java
* Author: Mike Schoonover
* Date: 6/14/12
*
* Purpose:
*
* This class manages a file meant for logging progress, error, or other types
* of messages.  If the specified file already exists, then new messages are
* appended to the existing file.
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
import javax.swing.JOptionPane;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class LogFile
//
// See notes at top of page.
//

public class LogFile extends Object {

    PrintWriter file = null;
    String filename;

//-----------------------------------------------------------------------------
// LogFile::LogFile (constructor)
//
//

public LogFile()
{

}//end of LogFile::LogFile (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::init
//
// Creates or opens for appending the file pFilename.
//

public void init(String pFilename)
{

    filename = pFilename;

    try{
        file = new PrintWriter(new FileWriter(pFilename, true));
    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 66");
        if (file != null) file.close();
    }

}//end of LogFile::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::close
//
// Closes the file.
//

public void close()
{

    if (file != null) file.close();

}//end of LogFile::close
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::log
//
// Writes pLine to the file.
//

public void log(String pLine)
{

    if (file != null) file.println(pLine);

}//end of LogFile::log
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::separate
//
// Write a separator (such as a line of dashes) to the file.
//

public void separate()
{

    if (file != null) file.println(
     "---------------------------------------------------------------------");

}//end of LogFile::separator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::date
//
// Write the date to the file.
//

public void date()
{

    if (file != null) file.println(new Date().toString());

}//end of LogFile::date
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::section
//
// Writes a blank line, a separator, the date, a blank line.
//

public void section()
{

    if (file != null){
        file.println("");
        separate(); date();
        file.println("");
    }

}//end of LogFile::section
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LogFile::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of LogFile::displayErrorMessage
//-----------------------------------------------------------------------------

}//end of class LogFile
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------