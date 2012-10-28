/******************************************************************************
* Title: PieceInfoItem.java
* Author: Mike Schoonover
* Date: 10/26/12
*
* Purpose:
*
* This class handles items for display in the PieceInfo window. It is in its
* own file so that it can be public and exported by the PieceInfo class to
* outside classes.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class PieceInfoItem
//
// Holds the info for one entry item which is an input box with a label.
//

public class PieceInfoItem extends Object{

public String labelText;
public int width;
public int height;
public int numberCharacters;
public boolean editable;
public boolean clearedInNewJob;
public JLabel label;
public JTextField textField;
public boolean printInFooter;
public int printOrder;
boolean printed;

//-----------------------------------------------------------------------------
// PieceInfoItem::createTextField
//

void createTextField(FocusListener focusListener)
{

textField = new JTextField(numberCharacters);

//all text fields have the same name so they all trigger the focus listener in
//the same way
textField.setName("Value Text Field");
textField.addFocusListener(focusListener);

int dHeight = textField.getPreferredSize().height;

//set the width to 1 pixel - Java will override this to make the field large
//enough to hold the specified number of characters but prevents it from
//enlarging the field to fill its container

textField.setMinimumSize(new Dimension(1, dHeight));
textField.setPreferredSize(new Dimension(1, dHeight));
textField.setMaximumSize(new Dimension(1, dHeight));

}//end of PieceInfoItem::createTextField
//-----------------------------------------------------------------------------

}//end of class PieceInfoItem
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
