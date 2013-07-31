/******************************************************************************
* Title: PrintProgress.java
* Author: Mike Schoonover
* Date: 9/14/11
*
* Purpose:
*
* This class displays a dialog window to show progress as a job is printed and
* to allow the user to cancel the process.
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class PrintProgress
//
//

public class PrintProgress extends JDialog implements ActionListener {

    public boolean userCancel = false;
    JLabel label;

//-----------------------------------------------------------------------------
// PrintProgress::PrintProgress (constructor)
//

public PrintProgress(JFrame pFrame)
{

    super(pFrame, false); //setup as non-modal window

    setTitle("Printing Status");

}//end of PrintProgress::PrintProgress (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintProgress::init
//

public void init()
{

    //create a panel to hold the labels and data entry boxes

    JPanel panel;
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setOpaque(true);
    add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,20))); //horizontal spacer

    label = new JLabel("Printing...");
    label.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(label);

    panel.add(Box.createRigidArea(new Dimension(0,20))); //horizontal spacer

    JButton cancel;
    cancel = new JButton("Cancel");
    cancel.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(cancel);
    cancel.setToolTipText("Cancel");
    cancel.setActionCommand("Cancel");
    cancel.addActionListener(this);

    panel.add(Box.createRigidArea(new Dimension(200,5))); //horizontal spacer

    pack();

}//end of PrintProgress::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintProgress::setLabel
//
// Sets the label to pLabel.
//
// Uses invokeLater to set the label so it is thread safe and can be called
// by a thread other than the main Java gui thread.
//

public void setLabel(final String pLabel)
{

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            label.setText(pLabel);

            }});

}//end of PrintProgress::setLabel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintProgress::setLabel
//
// Sets the label to show that piece number pNumberBeingProcessed is currently
// being prepared for printing.
//
// Uses invokeLater to set the label so it is thread safe and can be called
// by a thread other than the main Java gui thread.
//

public void setLabel(final int pNumberBeingProcessed)
{

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            label.setText("Preparing file " + pNumberBeingProcessed + "...");

            }});

}//end of PrintProgress::setLabel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintProgress::threadSafeSetVisible
//
// Sets the window's label to pLabel and the visiblity to pVisible.
//
// Uses invokeLater to set the state so it is thread safe and can be called
// by a thread other than the main Java gui thread.
//

public void threadSafeSetVisible(final boolean pVisible, final String pLabel)
{

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            setLabel(pLabel);
            setVisible(pVisible);

            }});

}//end of PrintProgress::threadSafeSetVisible
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintProgress::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Cancel".equals(e.getActionCommand())) {

        userCancel = true;
        setVisible(false);

        }

}//end of PrintProgress::actionPerformed
//-----------------------------------------------------------------------------

}//end of class PrintProgress
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
