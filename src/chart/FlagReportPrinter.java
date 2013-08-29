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

import chart.mksystems.hardware.Hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Plotter;
import chart.mksystems.stripchart.StripChart;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FlagReportPrinter
//
//

public class FlagReportPrinter extends ViewerReporter
                                                implements ActionListener {

    JDialog dialog;

    Hardware hardware;

    public JTextField startPieceBox, endPieceBox;

    public boolean okToPrint = false;

    int locationX, locationY;

    String pieceDescriptionPlural, pieceDescriptionPluralLC;

    JCheckBox calModeCheckBox;

    IniFile jobInfoFile = null;

    String reportsPath = "";

//-----------------------------------------------------------------------------
// FlagReportPrinter::FlagReportPrinter (constructor)
//

public FlagReportPrinter(JFrame pFrame, Settings pSettings, JobInfo pJobInfo,
        String pJobPrimaryPath, String pJobBackupPath, String pReportsPath,
        String pCurrentJobName, int pLocationX, int pLocationY,
        String pPieceDescriptionPlural, String pPieceDescriptionPluralLC,
        Hardware pHardware, int pPieceToPrint, boolean pIsCalPiece)

{

    super(pSettings, pJobInfo, pJobPrimaryPath, pJobBackupPath,
                                                              pCurrentJobName);

    hardware = pHardware; reportsPath = pReportsPath;

    //if pPieceToPrint is not a negative, number then the report is to be
    //printed for that piece without asking the user to specify a piece
    pieceToPrint = pPieceToPrint; isCalPiece = pIsCalPiece;

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

    //load the job information
    loadJobInfo();

    //create a panel to hold everything

    JPanel panel;
    panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
    panel.setOpaque(true);
    dialog.add(panel);

    //if pieceToPrint is -1, then setup the GUI so user can enter print range
    //otherwise, just print the report for pieceToPrint and display a message
    if (pieceToPrint == -1) {
        setUpForUserEntry(panel);
    }
    else {
        setUpForNoUserEntry(panel);
    }

    dialog.pack();

    dialog.setVisible(true);

    //if the print range already specified without need for user input,
    //print that range
    if (pieceToPrint != -1) {printReportForSinglePiece(pieceToPrint);}

}//end of FlagReportPrinter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::setUpForUserEntry
//
// Sets up the GUI controls for user entry of range to be printed.
//

public void setUpForUserEntry(JPanel pPanel)
{

    //dialog stays on top and focused if user input required
    dialog.setModal(true);

    pPanel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    //panel to hold the "from" and "to" print range numbers

    JPanel panel2;
    panel2 = new JPanel();
    panel2.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel2.setLayout(new BoxLayout(panel2, BoxLayout.LINE_AXIS));
    panel2.setOpaque(true);
    pPanel.add(panel2);

    startPieceBox = new JTextField(6);
    panel2.add(startPieceBox);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    JLabel label = new JLabel("to");
    panel2.add(label);
    panel2.add(Box.createRigidArea(new Dimension(3,0))); //horizontal spacer
    endPieceBox = new JTextField(6);
    panel2.add(endPieceBox);

    pPanel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    //add the Cal piece selection box -- allows reports to be generated for
    //calibration pieces

    calModeCheckBox = new JCheckBox("Report for Cal " + pieceDescriptionPlural);
    calModeCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
    calModeCheckBox.setSelected(false);
    calModeCheckBox.setToolTipText(
            "Check this box to print reports for calibration "
                                                    + pieceDescriptionPluralLC);
    pPanel.add(calModeCheckBox);

    //panel to hold the Print and Cancel buttons

    JPanel panel3;
    panel3 = new JPanel();
    panel3.setAlignmentX(Component.LEFT_ALIGNMENT);
    panel3.setLayout(new BoxLayout(panel3, BoxLayout.LINE_AXIS));
    panel3.setOpaque(true);
    pPanel.add(panel3);
    pPanel.add(panel3);

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

    pPanel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

}//end of FlagReportPrinter::setUpForUserEntry
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::setUpForNoUserEntry
//
// Displays a message in the window telling user that a report is being
// generated.
//

public void setUpForNoUserEntry(JPanel pPanel)
{

    //dialog does not retain focus if only displaying a message -- this allows
    //execution of other code to continue without blocking
    dialog.setModal(false);

    pPanel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

    pPanel.add(new JLabel("  Creating flag report for " + pieceToPrint
                                    + (isCalPiece ? ".cal  " : ".dat  ")));

    pPanel.add(Box.createRigidArea(new Dimension(0,5))); //horizontal spacer

}//end of FlagReportPrinter::setUpForNoUserEntry
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::closeAndDispose
//
// Closes the dialog window and releases its resources.
//

public void closeAndDispose()
{

    dialog.setVisible(false);
    dialog.dispose();

}//end of FlagReportPrinter::closeAndDispose
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
        printReportForEnteredRange();
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
// If the "Cal Mode" checkbox exists, return its state.  If not, return the
// state of isCalPiece.
//

@Override
public boolean isCalSelected()
{

    if (calModeCheckBox != null) {
        return(calModeCheckBox.isSelected());
    }
    else {
        return(isCalPiece);
    }

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
// FlagReportPrinter::printReportForEnteredRange
//
// Prints a report for the user specified range.
//

public void printReportForEnteredRange()
{

    try{
        startPiece = Integer.valueOf(startPieceBox.getText());

        //if no value supplied for the ending piece, set it to the start piece
        if(endPieceBox.getText().isEmpty()) {
            endPiece = startPiece;
        }
        else {
            endPiece = Integer.valueOf(endPieceBox.getText());
        }

        pieceTrack = startPiece;
        startPrint();
    }
    catch(NumberFormatException nfe)  {
        displayErrorMessage("Error in print range.");
    }

}//end of FlagReportPrinter::printReportForEnteredRange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printReportForSinglePiece
//
// Prints a report for pPiece.
//

public void printReportForSinglePiece(int pPiece)
{

    startPiece = pPiece;
    endPiece = startPiece;
    pieceTrack = startPiece;
    startPrint();

}//end of FlagReportPrinter::printReportForSinglePiece
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
        if(!pQuietMode) {displayErrorMessage(result);}
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

    //if it does not already exist, create a folder to hold the report
    String lReportsPath = createFolder();

    //don't print if the path could not be created
    if (lReportsPath.isEmpty()) {return;}

    //print a report for each piece in the range

    for (int i = startPiece; i <= endPiece; i++){

        printReportForPiece(lReportsPath, i);

    }

}//end of FlagReportPrinter::startPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::createFolder
//
// Creates a folder to hold the reports if it does not already exist.
//
// Returns the full path to the folder.
//

public String createFolder()
{

    //remove the folder separator from the end of the reports path if it exists
    //so we can make a new folder name

    String lReportsPath = "";
    File folder;

    if(!reportsPath.isEmpty()){
        lReportsPath = reportsPath;
        if (lReportsPath.endsWith(File.separator)) {
            lReportsPath = lReportsPath.substring(0, lReportsPath.length()-1);
        }

        //create a folder using the job name to hold the reports
        folder = new File(lReportsPath);
        if (!folder.exists()){
            //attempt to create the folder
            if (!folder.mkdirs()){
                displayErrorMessage("The reports folder could not be created.");
                return("");
            }
        }//if(!reportsPath.isEmpty)
    }//if(!reportsPath.isEmpty())

   //remove the folder separator from the end of the job path if it exists
   //so we can make a new folder name

    String lJobPrimaryPath = jobPrimaryPath;
    if (lJobPrimaryPath.endsWith(File.separator)) {
        lJobPrimaryPath =
                    lJobPrimaryPath.substring(0, lJobPrimaryPath.length()-1);
    }

    String finalReportsPath;

    //if a specific folder has been specified to hold reports, then place
    //the new folder there -- if not, then place the folder in the primary
    //data folder
    if (!lReportsPath.isEmpty()){
        finalReportsPath = lReportsPath + File.separator + currentJobName;
    }
    else{
        finalReportsPath = lJobPrimaryPath;
    }

    //create a new folder which will be sorted next to the job data folder
    finalReportsPath = finalReportsPath + " ~ Reports";

    //create a folder using the job name to hold the reports
    folder = new File(finalReportsPath);
    if (!folder.exists()){
        //attempt to create the folder
        if (!folder.mkdirs()){
            displayErrorMessage(
                            "The job's reports folder could not be created.");
            return("");
        }
    }

//if all folders created okay, return the full path
return(finalReportsPath);

}//end of FlagReportPrinter::createFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printReportForPiece
//
// Prints a report for pPiece.
//

public void printReportForPiece(String pReportsPrimaryPath, int pPiece)
{

    //Prepend "Cal" to cal joint reports.  Prepend is used over append so that
    //the files are sorted by group when listed in alphabetical order.

    String prefix = isCalSelected() ? "Cal " : "";

    String filename = pReportsPrimaryPath + File.separator +
       prefix + decimalFormats[0].format(pPiece) + " Flag Report.txt";

    PrintWriter file = null;

    try{
        file = new PrintWriter(new FileWriter(filename, false));
    }
    catch(IOException e){

        //if file cannot be opened, just display an error message
        //no messages will be written to the file -- this is not a super
        //critical error and should happen rarely

        displayErrorMessage("Could not create report file.");
        if (file != null) {file.close();}
        return;

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

    //prepare for printing
    resetTracePreviousFlagVariables();

    //report is printed in linear order, so use outer loop as the trace position
    //index and call each trace with the index

    //use the length of the first trace in the first chart in the first group
    //as all traces should be the same length -- check first to make sure that
    //there is at least one trace and bail out if not

    if( (numberOfChartGroups == 0)
         || (chartGroups[0].getNumberOfStripCharts() == 0)
            || (chartGroups[0].getStripChart(0).getNumberOfPlotters() == 0)){
        return;
    }

    int traceLength =
        chartGroups[0].getStripChart(0).getPlotter(0).getDataBufferWidth();

    for (int i = 0; i < traceLength; i++){
        for (int j = 0; j < numberOfChartGroups; j++){
            ChartGroup cGroup = chartGroups[j];
            for (int k = 0; k < cGroup.getNumberOfStripCharts(); k++){
                StripChart chart = cGroup.getStripChart(k);
                for (int l = 0; l < chart.getNumberOfPlotters(); l++){

                printFlagForPlotter(file, chart, chart.getPlotter(l), i);

                }//for (int l = 0; l < chart.getNumberOfTraces()
            }//for (int k = 0; k < numberOfStripCharts;...
        }//for (int j = 0; j < numberOfChartGroups;...
    }//for (int i = 0; i < traceLength...

    file.close();

}//end of FlagReportPrinter::printReportForPiece
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printFlagForPlotter
//
// Prints the flag at buffer index pDataIndex for plotter pPlotter.
//
// If multiple flags from the same flaw trace are located at the same tenth of a
// foot (or equivalent for metric) and clock position, the first flag is always
// printed but subsequent flags are not printed if they are the same amplitude
// than the flag just before.
//
// For wall traces, subsequent flags are not shown if they have the same wall
// value.
//
// This method is not perfect as multiple flags with differing amplitudes will
// all be printed, but a lot of duplication is of the same amplitude.  To solve
// the problem totally, the grouped flags would need to be preloaded into a
// buffer and scanned for the highest amplitude amongst the group, which would
// then be printed.
//
// For the multiple flag checks, the formatted string version of some of the
// values are used for comparison as this takes into account round off.  This
// does make it difficult to use comparison to find the worst case if that
// option is ever implemented.
//

public void printFlagForPlotter(PrintWriter pFile, StripChart pChart,
                                              Plotter pPlotter, int pDataIndex)
{

    boolean isWallChart = false;
    if (pChart.shortTitle.contains("Wall")) {isWallChart = true;}

    String linearPos;
    String amplitudeText;
    int clockPos;

    //extract the flag threshold -- if greater than 0, then a flag
    //is set at this position (note that threshold 1 denotes a user
    //set flag)
    if (((pPlotter.getFlagBuffer()[pDataIndex] & 0x0000fe00) >> 9) > 0){

        //debug mks -- pixelsPerInch needs to be read from the joint
        //file, not config file as it may change

        //convert the position and amplitude first so they can be
        //used to detect duplicate flags (see notes in function header)

        //convert index to decimal feet, format, and pad to length
        linearPos = prePad(decimalFormats[1].format(pDataIndex /
                                            hdwVs.pixelsPerInch / 12.0), 5);

        //convert and format the amplitude depending on chart type
        int amplitude = pPlotter.getDataBuffer1()[pDataIndex];
        double wall;
        if (isWallChart){
            wall = calculateComputedValue1(amplitude);
            amplitudeText = decimalFormats[2].format(wall);
            amplitudeText = prePad(amplitudeText, 5);
        }
        else{
            if (amplitude > 100) {amplitude = 100;}
            amplitudeText = prePad("" + amplitude, 5);
        }

        //extract the clock position from the flag
        clockPos = pPlotter.getFlagBuffer()[pDataIndex] & 0x1ff;

        //if the Report All Flags option is off, don't print duplicate flags:
        //if the flag is in the same linear and clock position as the
        //previous flag printed for this trace and has the same amplitude, then
        //the flag is not printed (see notes in function header)
        if (!settings.reportAllFlags && linearPos.equals(pPlotter.prevLinearPos)
                && amplitudeText.equals(pPlotter.prevAmplitudeText)
                                    && clockPos == pPlotter.prevClockPos){
            return;
        }

        pPlotter.prevLinearPos = linearPos;
        pPlotter.prevAmplitudeText = amplitudeText;
        pPlotter.prevClockPos = clockPos;

        pFile.print(linearPos + "\t");

        pFile.print(clockPos + "\t"); //radial position

        //print the short title of trace which should have
        //been set up in the config file to describe some or all of
        //orientation, ID/OD designation, and channel number
        pFile.print(postPad(pPlotter.shortTitle, 10) + "\t");

        pFile.print(amplitudeText + "\t"); //amplitude

        //print a line for handwritten notes and initials
        pFile.println("________________________________________");

    }//if (((pTrace.flagBuffer[pDataIndex] & 0x0000fe00) >> 9) > 0)

}//end of FlagReportPrinter::printFlagsForChart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::resetTracePreviousFlagVariables
//
// Resets all the variables used to store the previous flag printed before
// the report is printed.  These variables are used to prevent duplicate flags
// from being printed.
//

public void resetTracePreviousFlagVariables()
{

    for (int j = 0; j < numberOfChartGroups; j++){
        ChartGroup cGroup = chartGroups[j];
        for (int k = 0; k < cGroup.getNumberOfStripCharts(); k++){
            StripChart chart = cGroup.getStripChart(k);
            for (int l = 0; l < chart.getNumberOfPlotters(); l++){

                Plotter plotter = chart.getPlotter(l);
                plotter.prevLinearPos = "";
                plotter.prevAmplitudeText = "";
                plotter.prevClockPos = -1;

            }//for (int l = 0; l < chart.getNumberOfTraces()
        }//for (int k = 0; k < numberOfStripCharts;...
    }//for (int j = 0; j < numberOfChartGroups;...

}//end of FlagReportPrinter::resetTracePreviousFlagVariables
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::printHeader
//
// Prints the report header.
//

public void printHeader(PrintWriter pFile, int pPiece)
{

    //title

    pFile.println("");
    pFile.println(
          "\t\t\tFlag Report for " + settings.pieceDescription + " " + pPiece);
    pFile.println("");

    //job info

    //give work order an entire line as it is sometimes nice to use a long
    //descriptive name
    pFile.println("Work Order: " + truncate(
                    jobInfoFile.readString("Job Info", "Work Order", ""),90));

    pFile.print("Date: " + truncate(
        jobInfoFile.readString("Job Info", "Date Job Started", ""),10) + "  ");

    pFile.print("Customer: " + truncate(
        jobInfoFile.readString("Job Info", "Customer Name", ""),20) + "  ");
    pFile.println("Customer Job #: " + truncate(
        jobInfoFile.readString("Job Info", "Customer PO", ""),10));

    pFile.println("Job Location: " + truncate(
    jobInfoFile.readString("Job Info", "Job Location", ""),70));

    //use the values entered for the Wall chart rather than the job info data
    //as the former is more likely to be accurate
    double wall = hdwVs.nominalWall;

    pFile.print("Diameter: " + truncate(
            jobInfoFile.readString("Job Info", "Pipe Diameter", ""),6) + "  ");

    pFile.print("Nominal Wall: " +
                            truncate(decimalFormats[2].format(wall), 6) + "  ");

    pFile.println("Length: " + decimalFormats[1].format(hdwVs.measuredLength));

    String wallRejectPercentText =
              jobInfoFile.readString("Job Info", "Wall Reject Percentage", "");

    //attempt to extract a wall reject percentage from the user input
    double wallRejectPercent = parseWallRejectPercentage(wallRejectPercentText);

    String wallRejectPercentParsedText = (wallRejectPercent > 0) ?
                        decimalFormats[1].format(wallRejectPercent) : "?";

    pFile.print("Wall Reject Percentage: "
                                + truncate(wallRejectPercentText,6) + "  ");

    double wallMinusReject; String wallMinusRejectText;

    //calculate wall minus the reject percentage
    if (wallRejectPercent > 0){
        //wall reject percent > 0 so no parse error
        wallMinusReject = wall - (wall * (wallRejectPercent/100));
        wallMinusRejectText =
              truncate(decimalFormats[2].format(wallMinusReject), 6);
    }
    else{
        wallMinusRejectText = "?";
    }

    pFile.println("Nominal Wall less " +
      truncate(wallRejectPercentParsedText, 4) + "%: " + wallMinusRejectText);

    pFile.print("Unit Operator: " + truncate(
          jobInfoFile.readString("Job Info", "Unit Operator", ""),30) + "   ");

    pFile.println("Inspection Direction: " + truncate(inspectionDirection,20));

    pFile.println("");
    pFile.println("");

    //prove up person name/signature blank
    pFile.println("Prove up by: _____________________________________________"
                                                         + "________________");
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
        "-----------------------------------------------------------------"
        + "---------------");

}//end of FlagReportPrinter::printSeparator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::prePad
//
// Adds spaces to the beginning of pSource until it is length pLength.
// Returns the new string.
//

String prePad(String pSource, int pLength)
{

   while(pSource.length() < pLength) {
        pSource = " " + pSource;
    }

   return(pSource);

}//end of FlagReportPrinter::prePad
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::postPad
//
// Adds spaces to the end of pSource until it is length pLength.
// Returns the new string.
//

String postPad(String pSource, int pLength)
{

   while(pSource.length() < pLength) {
        pSource = pSource + " ";
    }

   return(pSource);

}//end of FlagReportPrinter::postPad
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::truncate
//
// Truncates pSource to pLength.  The truncated string is returned.
//

String truncate(String pSource, int pLength)
{

    if(pSource.length() > pLength) {
        return (pSource.substring(0, pLength));
    }
    else {
        return(pSource);
    }

}//end of FlagReportPrinter::truncate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::calculateComputedValue1
//
// For this version of Hardware.java, calculates the wall thickness based upon
// the amplitude.
//
// This function is duplicated in multiple objects.  Should make a separate
// class which each of those objects creates to avoid duplication?
//

@Override
public double calculateComputedValue1(int pAmplitude)
{

    double offset = (pAmplitude - hdwVs.nominalWallChartPosition)
                                                        * hdwVs.wallChartScale;

    //calculate wall at cursor y position relative to nominal wall value
    return (hdwVs.nominalWall + offset);

}//end of FlagReportPrinter::calculateComputedValue1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::loadJobInfo
//
// Loads the job information into an iniFile object.  No values are transferred
// out -- they are extracted by other functions as needed.
//

public void loadJobInfo()
{

    //if the ini file cannot be opened and loaded, exit without action and
    //default values will be used

    try {
        jobInfoFile = new IniFile(
            jobPrimaryPath + "03 - " + currentJobName
            + " Job Info.ini", settings.jobFileFormat);
    }
    catch(IOException e){return;}

}//end of FlagReportPrinter::loadJobInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::parseWallRejectPercentage
//
// Converts the user text input pInput for wall reject percentage into a number.
// If the text cannot be parsed, returns -1;
//

public double parseWallRejectPercentage(String pInput)
{

    String s = "";

    //strip out all characters except numbers and decimal points

    for(int i=0; i< pInput.length(); i++){

        if (isNumerical(pInput.charAt(i))) {
            s = s + pInput.charAt(i);
        }

    }

    //convert text to double

    double dValue;

    try{
        dValue = Double.valueOf(s);
    }
    catch(NumberFormatException nfe){
        //return an error code
        return(-1);
    }

    //return the valid value

    return(dValue);

}//end of FlagReportPrinter::parseWallRejectPercentage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FlagReportPrinter::isNumerical
//
// Returns true if pInput is a number or a decimal point.
//

public boolean isNumerical(char pInput)
{

    if(    (pInput == '0')
        || (pInput == '1')
        || (pInput == '2')
        || (pInput == '3')
        || (pInput == '4')
        || (pInput == '5')
        || (pInput == '6')
        || (pInput == '7')
        || (pInput == '8')
        || (pInput == '9')
        || (pInput == '.') ) {return(true);}

    return(false); //not a numerical type character

}//end of FlagReportPrinter::isNumerical
//-----------------------------------------------------------------------------

}//end of class FlagReportPrinter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
