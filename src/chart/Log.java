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

import javax.swing.*;
import java.awt.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Log
//
// This class displays a text area in a window.
//

class Log extends JDialog{

public JTextArea textArea;

//-----------------------------------------------------------------------------
// Log::Log (constructor)
//
//
  
public Log(JFrame frame)
{

super(frame, "Message Log");

int panelWidth = 300;
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

}//end of class Log
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
