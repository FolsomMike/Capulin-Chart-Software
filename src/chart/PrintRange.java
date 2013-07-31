/******************************************************************************
* Title: PrintRange.java
* Author: Mike Schoonover
* Date: 9/14/11
*
* Purpose:
*
* This class displays a dialog window for entering a range of pieces to be
* printed.
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
// class PrintRange
//
//

public class PrintRange extends JDialog implements ActionListener {

    public JTextField startPiece, endPiece;

    public boolean okToPrint = false;

//-----------------------------------------------------------------------------
// PrintRange::PrintRange (constructor)
//

public PrintRange(JFrame pFrame)
{

    super(pFrame, true); //setup as modal window

    setTitle("Print Range");

}//end of PrintRange::PrintRange (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRange::init
//

public void init()
{

    //create a panel to hold the labels and data entry boxes

    JPanel panel;
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setOpaque(true);
    add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    JPanel panel2;
    panel2 = new JPanel();
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
    panel2.setOpaque(true);
    panel.add(panel2);

    startPiece = new JTextField(6);
    panel2.add(startPiece);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    JLabel label = new JLabel("to");
    panel2.add(label);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    endPiece = new JTextField(6);
    panel2.add(endPiece);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    JPanel panel3;
    panel3 = new JPanel();
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.LINE_AXIS));
    panel3.setOpaque(true);
    panel.add(panel3);
    panel.add(panel3);

    JButton print;
    panel3.add(print = new JButton("Print"));
    print.setToolTipText("Print the selected range of pieces.");
    print.setActionCommand("Print");
    print.addActionListener(this);

    panel3.add(Box.createRigidArea(new Dimension(10,0))); //horizontal spacer

    JButton cancel;
    panel3.add(cancel = new JButton("Cancel"));
    cancel.setToolTipText("Cancel");
    cancel.setActionCommand("Cancel");
    cancel.addActionListener(this);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    pack();

}//end of PrintRange::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PrintRange::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Print".equals(e.getActionCommand())) {

        okToPrint = true;
        setVisible(false);

        }

    if ("Cancel".equals(e.getActionCommand())) {

        okToPrint = false;
        setVisible(false);

        }

}//end of PrintRange::actionPerformed
//-----------------------------------------------------------------------------

}//end of class PrintRange
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
