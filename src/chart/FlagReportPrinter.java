/******************************************************************************
* Title: FlagReportPrinter.java
* Author: Mike Schoonover
* Date: 8/14/12
*
* Purpose:
*
* This class displays a dialog window for entering a range of pieces and then
* prints a flag report for those pieces.
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
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;

import chart.mksystems.globals.Globals;
import chart.mksystems.stripchart.StripChart;
import chart.mksystems.stripchart.Trace;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FlagReportPrinter
//
//

public class FlagReportPrinter extends ViewerReporter
                                                implements ActionListener {

    JDialog dialog;

    public JTextField startPieceBox, endPieceBox;

    public boolean okToPrint = false;

    int locationX, locationY;

    String pieceDescriptionPlural, pieceDescriptionPluralLC;

    JCheckBox calModeCheckBox;

//-----------------------------------------------------------------------------
// FlagReportPrinter::FlagReportPrinter (constructor)
//

public FlagReportPrinter(JFrame pFrame, Globals pGlobals, JobInfo pJobInfo,
        String pJobPrimaryPath, String pJobBackupPath, String pCurrentJobName,
        int pLocationX, int pLocationY, String pPieceDescriptionPlural,
        String pPieceDescriptionPluralLC)

{

    super(pGlobals, pJobInfo, pJobPrimaryPath, pJobBackupPath, pCurrentJobName);

    //set up as modal window
    dialog = new JDialog(pFrame, true);

    dialog.setTitle("Flag Report");

    locationX = pLocationX; locationY = pLocationY;

    pieceDescriptionPlural = pPieceDescriptionPlural;

    pieceDescriptionPluralLC = pPieceDescriptionPluralLC;

}//end of FlagReportPrinter::FlagReportPrinter (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::init
//

@Override
public void init()
{

    super.init();

    //release resources when closed
    dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    dialog.setLocation(locationX, locationY);

    //create the chart groups to hold data for the reports
    configure();

    //create a panel to hold everything

    JPanel panel;
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setOpaque(true);
    dialog.add(panel);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    //panel to hold the "from" and "to" print range numbers

    JPanel panel2;
    panel2 = new JPanel();
    panel2.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
    panel2.setOpaque(true);
    panel.add(panel2);

    startPieceBox = new JTextField(6);
    panel2.add(startPieceBox);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    JLabel label = new JLabel("to");
    panel2.add(label);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    endPieceBox = new JTextField(6);
    panel2.add(endPieceBox);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    //add the Cal piece selection box -- allows reports to be generated for
    //calibration pieces

    calModeCheckBox = new JCheckBox("Report for Cal " + pieceDescriptionPlural);
    calModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    calModeCheckBox.setSelected(false);
    calModeCheckBox.setToolTipText(
            "Check this box to print reports for calibration "
                                                    + pieceDescriptionPluralLC);
    panel.add(calModeCheckBox);

    //panel to hold the Print and Cancel buttons

    JPanel panel3;
    panel3 = new JPanel();
    panel3.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.LINE_AXIS));
    panel3.setOpaque(true);
    panel.add(panel3);
    panel.add(panel3);

    JButton print;
    panel3.add(print = new JButton("Print"));
    print.setToolTipText("Print the report for the selected range of pieces.");
    print.setActionCommand("Print");
    print.addActionListener(this);

    panel3.add(Box.createRigidArea(new Dimension(10,0))); //horizontal spacer

    JButton cancel;
    panel3.add(cancel = new JButton("Cancel"));
    cancel.setToolTipText("Cancel");
    cancel.setActionCommand("Cancel");
    cancel.addActionListener(this);

    panel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    dialog.pack();

    dialog.setVisible(true);

}//end of FlagReportPrinter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Print".equals(e.getActionCommand())) {
        okToPrint = true;
        printReport();
        dialog.dispose();
    }

    if ("Cancel".equals(e.getActionCommand())) {
        dialog.dispose(); //release window resources
        return;
    }

}//end of FlagReportPrinter::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::configure
//
// Calls the parent component to create a panel containing the appropriate
// chart groups.
//
// The panel is not added to a window as the chart group data is only used
// for printing

@Override
public void configure()
{

    super.configure();


}//end of FlagReportPrinter::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::isCalSelected
//
// Gets the state of the Cal joint switch.
//

@Override
public boolean isCalSelected()
{

    return(calModeCheckBox.isSelected());

}//end of FlagReportPrinter::isCalSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

@Override
public void displayErrorMessage(String pMessage)
{

JOptionPane.showMessageDialog(mainFrame, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of FlagReportPrinter::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printReport
//
// Prints a report for the selected pieces.
//

public void printReport()
{

    try{
        startPiece = Integer.valueOf(startPieceBox.getText());

        //if no value supplied for the ending piece, set it to the start piece
        if(endPieceBox.getText().isEmpty())
            endPiece = startPiece;
        else
            endPiece = Integer.valueOf(endPieceBox.getText());

        pieceTrack = startPiece;
        startPrint();
        }
    catch(NumberFormatException nfe)  {
        displayErrorMessage("Error in print range.");
        }

}//end of FlagReportPrinter::printReport
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::loadSegment
//
// Loads the data for a segment from the primary job folder.  The calibration
// and piece info are also loaded from the associated info file.
//
// This function should be called whenever a new segment is loaded for
// viewing or processing - each segment could represent a piece being monitored,
// a time period, etc.
//
// If pQuietMode is true, no error message is displayed if a file cannot be
// loaded.  This is useful for the print function which can then continue on
// to the next piece instead of freezing until the user clears the dialog
// window.
//
// If no error, returns the filename extension.
// On error loading the chart data, returns "Error: <message>".
// Error on loading piece id info or calibration data returns empty string.
//

@Override
String loadSegment(boolean pQuietMode)
{

    String result = super.loadSegment(pQuietMode);

    //on error, display the message, repaint with empty chart, and exit
    if (result.startsWith("Error")){
        if(!pQuietMode)displayErrorMessage(result);
        }

    return(result);

}//end of FlagReportPrinter::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::startPrint
//
// Begins the report process.
//

public void startPrint()
{

    //remove the folder separator from the end of the job path if it exists
    //so we can make a new folder name

    String stripped = jobPrimaryPath;
    if (stripped.endsWith(File.separator))
    stripped = stripped.substring(0, stripped.length()-1);

    //create a new folder which will be sorted next to the job data folder
    String reportsPrimaryPath = stripped + " ~ Reports";

    //create a folder using the job name to hold the reports
    File folder = new File(reportsPrimaryPath);
    if (!folder.exists()){
        //attempt to create the folder
        if (!folder.mkdirs()){
            displayErrorMessage("The reports folder could not be created.");
            return;
            }
    }

    //print a report for each piece in the range

    for (int i = startPiece; i <= endPiece; i++){

        printReportForPiece(reportsPrimaryPath, i);

    }

}//end of FlagReportPrinter::startPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printReportForPiece
//
// Prints a report for pPiece.
//

public void printReportForPiece(String pReportsPrimaryPath, int pPiece)
{

    String filename = pReportsPrimaryPath + File.separator +
           decimalFormats[0].format(pPiece)+ " Flag Report.txt";

    PrintWriter file = null;

    try{
        file = new PrintWriter(new FileWriter(filename, false));
    }
    catch(IOException e){

        //if file cannot be opened, just display an error message
        //no messages will be written to the file -- this is not a super
        //critical error and should happen rarely

        displayErrorMessage("Could not create report file.");
        if (file != null) file.close();

    }

    //loadSegment uses currentSegmentNumber to load the desired piece
    currentSegmentNumber = pPiece;

    //load the data for the piece
    String result = loadSegment(true);

    //print the header information at the top of the file
    printHeader(file, pPiece);

    //print an error message if the piece could not be loaded
    if (result.startsWith("Error")){
        file.println("");
        file.println("Error - no file found.");
        file.close();
        return;
    }


    for (int i = 0; i < numberOfChartGroups; i++){
        for (int j = 0; j < chartGroups[i].getNumberOfStripCharts(); j++){

            printFlagsForChart(file, chartGroups[i].getStripChart(j));

        }//for (int j = 0; j < numberOfStripCharts;...
    }//for (int i = 0; i < numberOfChartGroups;...

    file.close();

}//end of FlagReportPrinter::printReportForPiece
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printFlagsForChart
//
// Prints all the flags for pChart.
//

public void printFlagsForChart(PrintWriter pFile, StripChart pChart)
{

    for (int i = 0; i < pChart.getNumberOfTraces(); i++){

        Trace trace = pChart.getTrace(i);

        int flagThreshold;

        for (int j=0; j<trace.flagBuffer.length; j++){

            //extract the flag threshold -- if greater than 0, then a flag
            //is set at this position (note that threshold 1 denotes a user
            //set flag)
            if ((flagThreshold =
                        (trace.flagBuffer[j] & 0x0000fe00) >> 9) > 0){

                pFile.print(j+"\t"); //linear location

                //extract and print the clock position
                int clockPos = trace.flagBuffer[j] & 0x1ff;
                pFile.print(clockPos+"\t"); //radial position

                //print the short title of the trace which should have been
                //set up in the config file to describe some or all of
                //orientation, ID/OD designation, and channel number
                pFile.print(addPadding(trace.shortTitle, 10)+"\t");

                int amplitude = trace.dataBuffer1[j];
                if (amplitude > 100) amplitude = 100;
                pFile.print(addPadding(""+amplitude, 3)+"\t"); //amplitude

                //print a line for handwritten notes and initials
                pFile.println("________________________________________");


            }//if ((flagThreshold =
        }//for (int j=0; j<trace.flagBuffer.length; j++)
    }//for (int I = 0; i < stripChart.getNumberOfTraces();...

}//end of FlagReportPrinter::printFlagsForChart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printHeader
//
// Prints the report header.
//

public void printHeader(PrintWriter pFile, int pPiece)
{

    //title

    pFile.println(
            "\t\t\tFlag Report for " + globals.pieceDescription + " " + pPiece);
    pFile.println("");

    //job info

    pFile.print("Customer: " + "   ");
    pFile.print("Date: " + "   ");
    pFile.print("Customer Job #: " + "   ");
    pFile.println("Work Order: " + "   ");
    pFile.print("Length: " + "   ");
    pFile.print("Wall: " + "   ");
    pFile.println("Wall less 5%: " + "   ");
    pFile.println("");
    pFile.println("");

    //prove up person signature
    pFile.println("Prove up by: _____________________________________________");
    pFile.println("");

    //column headers

    pFile.println("Linear\tRadial\tTransducer\tAmplitude\t"
                                                    + "Notes & Initials");
    pFile.println("(feet)\t(clock)");

    printSeparator(pFile);


}//end of FlagReportPrinter::printHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printSeparator
//
// Prints a separator line to pFile
//

public void printSeparator(PrintWriter pFile)
{

pFile.println(
  "--------------------------------------------------------------------------"
        + "------");

}//end of FlagReportPrinter::printSeparator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::addPadding
//
// Adds spaces to pSource until it is length pLength.  Returns the new string.
//

String addPadding(String pSource, int pLength)
{

   while(pSource.length() < pLength)
       pSource = pSource + " ";

   return(pSource);

}//end of FlagReportPrinter::addPadding
//-----------------------------------------------------------------------------


}//end of class FlagReportPrinter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
