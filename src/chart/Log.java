/******************************************************************************
* Title: Log.java
* Author: Mike Schoonover
* Date: 4/23/09
*
* Purpose:
*
* This class displays a log window.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.awt.*;
import java.util.Date;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Log
//
// This class displays a text area in a window.
//

public class Log extends JDialog{

    public JTextArea textArea;

//-----------------------------------------------------------------------------
// Log::Log (constructor)
//
//

public Log(JFrame frame)
{

    super(frame, "Message Log");

    int panelWidth = 330;
    int panelHeight = 500;

    setMinimumSize(new Dimension(panelWidth, panelHeight));
    setPreferredSize(new Dimension(panelWidth, panelHeight));
    setMaximumSize(new Dimension(panelWidth, panelHeight));

    textArea = new JTextArea();

    JScrollPane areaScrollPane = new JScrollPane(textArea);

    //add(textArea);

    add(areaScrollPane);


}//end of Log::Log (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Log::append
//
// Appends a text string to the text window.
//

public void append(String pText)
{

    textArea.append(pText);

}//end of Log::append
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Log::appendLine
//
// Appends a text string to the text window and moves to the next line
//

public void appendLine(String pText)
{

    textArea.append(pText + "\n");

}//end of Log::appendLine
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Log::separate
//
// Write a separator (such as a line of dashes) to the file.
//

public void separate()
{

    appendLine(
     "---------------------------------------------------------------------");

}//end of Log::separator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Log::date
//
// Write the date to the file.
//

public void date()
{

    appendLine(new Date().toString());

}//end of Log::date
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Log::section
//
// Writes a blank line, a separator, the date, a blank line.
//

public void section()
{

    appendLine("");
    separate(); date();
    appendLine("");

}//end of Log::section
//-----------------------------------------------------------------------------

}//end of class Log
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
