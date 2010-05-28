/******************************************************************************
* Title: MessageWindow.java
* Author: Mike Schoonover
* Date: 5/7/10
*
* Purpose:
*
* This class is used to display messages in a dialog window.  No buttons are
* displayed - the window should be closed by the calling function.
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
import java.awt.Rectangle;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MessageWindow
//
// See notes at top of page for info.
//

class MessageWindow extends JDialog{

public JLabel message;

//-----------------------------------------------------------------------------
// MessageWindow::MessageWindow (constructor)
//
//
  
public MessageWindow(JFrame pFrame, String pMessage)
{

super(pFrame, "Info");

setDefaultCloseOperation(DISPOSE_ON_CLOSE);

JPanel panel = new JPanel();
panel.setOpaque(true);

message = new JLabel(pMessage);
panel.add(message);

//have to use getContentPane to add a panel - seems though some other components
//can be added directly to the object but not panels
getContentPane().add(panel);

pack();

//position in center of parent frame
Rectangle r = pFrame.getBounds();
setLocation(((r.x + r.width)/2) - (getWidth()/2),
        ((r.y + r.height)/2) - (getHeight()/2));

setVisible(true);

}//end of MessageWindow::MessageWindow (constructor)
//-----------------------------------------------------------------------------    

}//end of class MessageWindow
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
