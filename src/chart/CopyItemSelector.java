/******************************************************************************
* Title: CopyItemSelector.java
* Author: Mike Schoonover
* Date: 5/15/12
*
* Purpose:
*
* This class displays a window containing items the user has selected to be
* copied from one channel to others.  The items are displayed as check box
* controls so the can easily be deselected from the copy operation.
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
import java.awt.event.*;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class CopyItemSelector
//
// See notes a top of page for info on this class.
//

public class CopyItemSelector extends JDialog implements ActionListener{

JPanel panel;

//-----------------------------------------------------------------------------
// CopyItemSelector::CopyItemSelector (constructor)
//
//

public CopyItemSelector(JFrame pFrame)
{

super(pFrame, "Copy Items");

}//end of CopyItemSelector::CopyItemSelector (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::init
//
// Initializes the object.  Must be called immediately after instantiation.
//

public void init()
{

//don't allow user to close the window with the x in the corner
setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

//add panel to hold everything
JPanel mainPanel = new JPanel();
mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
mainPanel.setOpaque(true);
add(mainPanel);

//add panel to hold list of items as check boxes
panel = new JPanel();
panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
panel.setOpaque(true);
panel.setAlignmentX(Component.CENTER_ALIGNMENT);
mainPanel.add(panel);

//add panel to hold "Help" button
JPanel helpPanel = new JPanel();
helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));
helpPanel.setOpaque(true);
JButton b = new JButton("Help");
b.addActionListener(this);
b.setActionCommand("Help");
helpPanel.add(b);
helpPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
mainPanel.add(helpPanel);

}//end of CopyItemSelector::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::getItemState
//
// Returns the checked state of the checkbox with the text string matching
// pItemName.
//
// If the item is not found, returns false.  If it is found, returns the state
// of the check box.
//

public boolean getItemState(String pItemName)
{

JCheckBox cb = getItemWithName(pItemName);

if (cb != null)
    return cb.isSelected();
else
    return(false);

}//end of CopyItemSelector::getItemState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::addItem
//
// Adds the name of an item to be added to the group of items which are to be
// copied and sets the window visible.
//
// The name is added to the window as a checkbox with the text set to that
// name.
//
// If the panel currently contains no items, the special item
// "Copy All Parameters" is added so it shows up at the top of the list.  If
// the user checks this box, then all parameters will be copied.
//
// The window is made visible.
//
// If pItemName is empty, the "Copy All Parameters" option will still be
// added and the window displayed, but no other item will be added.
//

public void addItem(String pItemName)
{

JCheckBox cb;

//add special item so user can choose to select all parameters for copy
if (getItemWithName("Copy All Parameters") == null){
    cb = new JCheckBox("Copy All Parameters");
    cb.setSelected(false);
    panel.add(cb);
}

//only add item if it is not already present
if (!pItemName.equals("") && getItemWithName(pItemName) == null){
    //add a new checkbox for the item
    cb = new JCheckBox(pItemName);
    cb.setSelected(true);
    panel.add(cb);
}

setVisible(true); //display the window

pack();

}//end of CopyItemSelector::addItem
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::getItemWithName
//
// Looks for a checkbox on the panel with the name PItemName.  Returns a pointer
// to the checkbox if found, null if not.
//

JCheckBox getItemWithName(String pItemName)
{

for (Component child : panel.getComponents())
    if (child instanceof JCheckBox)
        if (((JCheckBox)child).getText().equals(pItemName))
            return((JCheckBox)child);

return(null);

}//end of CopyItemSelector::getItemWithName
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::removeAll
//
// Removes all check box items from the panel.
//

@Override
public void removeAll()
{

panel.removeAll();

}//end of CopyItemSelector::removeAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::displayHelp
//
// Displays help info related to copying parameters.
//

public void displayHelp()
{

JOptionPane.showMessageDialog(this,
     "   Copying Parameters Between Channels\n\n"
     + "Right click on the controls in the Calibration window to add them"
     + " to the Copy Items window.\n"
     + "Any item listed there will be copied if checked. To copy all"
     + " settings for a channel, check the\n"
     + "Copy All Parameters entry.\n"
     + "To add the DAC settings to the list, right click anywhere on the"
     + " DAC tab.\n"
     + "To copy the chosen items from the currently selected channel to"
     + " another channel, click the <\n"
     + "button to the right of the destination channel.  To copy"
     + " from the selected channel to all other\n"
     + "channels, click the Copy to All button.\n"
     + "To close the Copy Items window and clear all items, click the"
     + " Cancel Copy button");

}//end of CopyItemSelector::displayHelp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CopyItemSelector::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

//trap "Help" button
if (e.getActionCommand().equals("Help")) displayHelp();

}//end of CopyItemSelector::actionPerformed
//-----------------------------------------------------------------------------


}//end of class CopyItemSelector
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
