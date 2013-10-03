/******************************************************************************
* Title: Viewer.java
* Author: Mike Schoonover
* Date: 1/16/09
*
* Purpose:
*
* This class displays a window for viewing saved chart segments.
* Depending on the configuration, each segment could represent a piece being
* monitored or inspected, a time period, etc.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.ChartGroup;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.AffineTransform;
import java.awt.print.*;
import java.io.*;
import java.util.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterResolution;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Viewer
//

public class Viewer extends ViewerReporter implements WindowListener,
        ItemListener, ComponentListener, Printable {

    Thread printThread;

    SegmentFileFilter segmentFileFilter;

    ArrayList<String> segmentList;

    static int FIRST = 0;
    static int LAST = 1;

    int startPage = 0, endPage = 0, pageTrack = 0;
    int printCallPageTrack;

    PrintRange printRange, printCalsRange;


//-----------------------------------------------------------------------------
// Viewer::Viewer (constructor)
//

public Viewer(Settings pSettings, JobInfo pJobInfo, String pJobPrimaryPath,
                            String pJobBackupPath, String pCurrentJobName)
{

    super(
         pSettings, pJobInfo, pJobPrimaryPath, pJobBackupPath, pCurrentJobName);

}//end of Viewer::Viewer (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::init
//

@Override
public void init()
{

    super.init();

    mainFrame = new JFrame("Viewer");

    //turn off default bold for Metal look and feel
    UIManager.put("swing.boldMetal", Boolean.FALSE);

    //force "look and feel" to Java style
    try {
        UIManager.setLookAndFeel(
            UIManager.getCrossPlatformLookAndFeelClassName());
        }
    catch (ClassNotFoundException | InstantiationException |
            IllegalAccessException | UnsupportedLookAndFeelException e) {
        logSevere(e.getMessage() + " - Error: 112");
    }

    mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    mainFrame.addWindowListener(this);

    mainFrame.addComponentListener(this);

    segmentFileFilter = new SegmentFileFilter();

    //create a list to hold the segment file names
    segmentList = new ArrayList<>();

    //change the layout manager
    BoxLayout boxLayout = new BoxLayout(
                                 mainFrame.getContentPane(), BoxLayout.Y_AXIS);
    //mainFrame.setLayout(new BoxLayout(mainFrame, BoxLayout.Y_AXIS));
    mainFrame.getContentPane().setLayout(boxLayout);

    //create the charts and other controls -- this will display empty charts
    //which will be replaced with actual data by calling method
    //loadFirstOrLastAvailableSegment
    configure();

    //reset the charts
    resetChartGroups();

    //load the last file saved - this is the most likely to be viewed
    loadFirstOrLastAvailableSegment(LAST);

    //let's have a look, shall we?
    mainFrame.setVisible(true);

}//end of Viewer::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::resetMainFrameAndSetUp
//
// Clears all children from the main JFrame and adds them back.  The clearing
// is necessary to force the frame to size properly to the contents.
//

public void resetMainFrameAndSetUp()
{

    //remove all children components
    //cannot use removeAll on a JFrame as it causes strange behavior, probably
    //because this may remove contentPane, which is an illegal action
    if(scrollPane != null) {mainFrame.remove(scrollPane);}
    if(controlPanel != null) {mainFrame.remove(controlPanel);}

    //clear out any presets so the JFrame will size itself to its contents
    mainFrame.setMinimumSize(null);
    mainFrame.setPreferredSize(null);
    mainFrame.setMaximumSize(null);

    mainFrame.add(scrollPane);

    mainFrame.add(controlPanel);

    //pack the window the first time to establish the sizes before the call to
    //handleSizeChanges which creates buffers dependent on those sizes
    mainFrame.pack();
    //in this case, Java needs two pack calls to properly size everything
    //this is required for the scroll pane to pack around its contents and the
    //main window to pack around the scroll pane
    mainFrame.pack();

    //if a size has been specified for the window, then use that size
    //this is useful for preventing the viewer window from filling the screen

    double w = mainFrame.getWidth();
    double h = mainFrame.getHeight();

    if(chartGroups[0].viewerWindowWidth != -1) {
        w = chartGroups[0].viewerWindowWidth;
    }

    if(chartGroups[0].viewerWindowHeight != -1) {
        h = chartGroups[0].viewerWindowHeight;
    }

    setSizes(mainFrame, (int)w, (int)h);
    mainFrame.pack();

    //At this point, the main window will be sized large enough to view the
    //graphs vertically with only a horizontal scroll bar displayed
    //automatically.
    //If the window is too large for the screen, then everything is shrunken to
    //fit with a vertical scroll bar then being displayed automatically.

    //get the actual screen size
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

    w = mainFrame.getWidth();
    h = mainFrame.getHeight();

    double adjustedWAmount = 0;
    double adjustedHAmount = 0;

    if (w > screenSize.getWidth()){
        adjustedWAmount = screenSize.getWidth() - w;
        w = screenSize.getWidth();
    }

    //shrink vertical a bit extra to account for the Windows task bar

    // tried using this code to avoid guessing at the height of the task bar,
    // but it didn't work -- windows doesn't support maximimizing in vertical
    // direction only?
    // if (h > screenSize.getHeight())
    //    mainFrame.setExtendedState(JFrame.MAXIMIZED_VERT);

    if (h > screenSize.getHeight()){
        adjustedHAmount = screenSize.getHeight() - h;
        h = screenSize.getHeight() - 30;
    }

    //set the new size for the main window
    setSizes(mainFrame, (int)w, (int)h);

    mainFrame.pack();

    //allow all objects to update values dependent on display sizes
    //must do this before loading data as this resets the buffers
    handleSizeChanges();

}//end of Viewer::resetMainFrameAndSetUp
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::configure
//
// Calls the parent component to create a panel containing the appropriate
// chart groups, then adds the panel to the main window for display.
//

@Override
public void configure()
{

    super.configure();

    controlPanel = new ViewerControlPanel(settings, currentJobName, this, this);

    //put the chartGroupPanel in a scroll pane so the user can scroll to see
    //a chart wider than the screen - this is different from the program's main
    //window which does not allow scrolling

    //the vertical scrollbar is not needed as all charts should be viewable
    //vertically - the horizontal scrollbar should always be shown so the
    //overall height of the window doesn't change when the scrollbar appears
    //or disappears

    scrollPane = new JScrollPane(chartGroupPanel,
                                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    //load the current cal file so the default display will reflect which
    //charts are hidden and any other settings -- these will be overridden
    //when a valid data file is loaded to reflect the settings stored therein
    loadCalFile();

    //set up the main frame with its components
    resetMainFrameAndSetUp();

    //this is used for printing
    aset = new HashPrintRequestAttributeSet();

    //create two separate print range dialog windows -- one for regular pieces
    //and one for calibration pieces -- this allows each to remember different
    //ranges when the user switches back and forth from regular to cal pieces
    printRange = new PrintRange(mainFrame);
    printRange.init();
    printCalsRange = new PrintRange(mainFrame);
    printCalsRange.init();

    printProgress = new PrintProgress(mainFrame);
    printProgress.init();

    //create a thread to handle printing in the background
    printRunnable = new PrintRunnable(aset);
    printThread = new Thread(printRunnable, "Print Thread");

    printThread.start();

}//end of Viewer::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadSegmentList
//
// Loads a list of the available segment files from the folder.  The filenames
// are reduced to the number without preceding zeroes or other non-numeric
// characters.
//
// Either ".dat" or ".cal" files are loaded depending on the checkbox setting.
//

public void loadSegmentList()
{

    //specify the type of files to load
    segmentFileFilter.extension
                = controlPanel.calModeCheckBox.isSelected() ? ".cal" : ".dat";

    //directory containing the various pertinent files
    File jobDir = new File(jobPrimaryPath);
    //get a list of the files/folders in the directory
    String[] files = jobDir.list(segmentFileFilter);

    //clear the list to hold the file/folder names
    segmentList.clear();
    segmentList.addAll(Arrays.asList(files));
    //sort the items alphabetically
    Collections.sort(segmentList);

    //put the array of items into the vector, converting the filenames to
    //numbers ignore any names which can't be converted to a number

    int segNum;
    String s;

    //scan through the sorted list converting the filenames to segment numbers
    //by stripping off the prefix and suffix and converting the remaining
    //characters to an integer
    //if any filename cannot be converted, leave it as is so the user can see
    //that a file is there but is invalid

    for (int i=0; i<segmentList.size(); i++){
        s = (String)segmentList.get(i);
        segNum = parseFilenameToSegmentNumber(s);
        if (segNum != Integer.MIN_VALUE) {segmentList.set(i, "" + segNum);}
    }

}//end of Viewer::loadSegmentList
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::handleSizeChanges
//
// Updates any values related to the size of display objects.  Called after
// the display has been set and any time a size may have changed.
//

public void handleSizeChanges()
{

    for (int i = 0; i < numberOfChartGroups; i++) {
        chartGroups[i].handleSizeChanges();
    }

}//end of Viewer::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadFirstOrLastAvailableSegment
//
// Loads the first or last available segment from the folder depending on the
// value of pWhich: LAST or FIRST.
//
// Does nothing if no valid file is found.
//

void loadFirstOrLastAvailableSegment(int pWhich)
{

    int segNumber = getFirstOrLastAvailableSegmentNumber(pWhich);

    if (currentSegmentNumber == -1){
        displayErrorMessage("Error with name in file list.");
        return;
    }

    if (currentSegmentNumber == -2){
        displayErrorMessage("No valid files in folder.");
        return;
    }

    currentSegmentNumber = segNumber;

    //load the file
    loadSegment(false);

}//end of Viewer::loadFirstOrLastAvailableSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::getFirstOrLastAvailableSegmentNumber
//
// Returns the number of the first or last available segment from the folder
// depending on the value of pWhich: LAST or FIRST.

// The segments are sorted alphabetically but their names are formatted such
// that this places them in numberical order as well.
//
// Returns -1 if there is an error parsing the number.
// Returns -2 if there are no files.
//

int getFirstOrLastAvailableSegmentNumber(int pWhich)
{

    //load the list of available segment numbers / files
    loadSegmentList();

    if (segmentList.isEmpty()) {return(-2);}  //no files found

    //attempt to convert the first or last entry in the list
    try{
        if (pWhich == FIRST) {
            return(Integer.valueOf(segmentList.get(0)));
        }
        else {
            return(Integer.valueOf(segmentList.get(segmentList.size()-1)));
        }
    }
    catch(NumberFormatException nfe){
        return(-1);
    }
    finally{
        segmentList.clear(); //clear the list to conserve resources
    }

}//end of Viewer::getFirstOrLastAvailableSegmentNumber
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::trimNonNumeric
//
// Trims all non-numeric characters from the beginning and end of pString.
// Non-numeric characters in between numeric characters will not be stripped.
//
// Returns the resulting string.
//

String trimNonNumeric(String pString)
{

    String s = pString;

    int trimPoint = -1;

    //look for first numeric character from the beginning

    for (int i = 0; i < s.length(); i++){
        if (isNumeric(s.charAt(i))){trimPoint = i; break;}
    }

    //if no numeric character found, return empty string else trim non-numeric
    if (trimPoint == -1) {
        return("");
    }
    else {
        s = s.substring(trimPoint, s.length());
    }


    //look for first numeric character from the end

    for (int i = s.length()-1; i >= 0; i--){
        if (isNumeric(s.charAt(i))){trimPoint = i; break;}
    }

    //if no numeric character found, return empty string else trim non-numeric
    if (trimPoint == -1) {
        return("");
    }
    else {
        s = s.substring(0, trimPoint+1);
    }

    return(s);

}//end of Viewer::trimNonNumeric
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::isNumeric
//
// Returns true if pChar is numeric, false if not.
//

boolean isNumeric(char pChar)
{

    //check each possible number
    if (pChar == '0') {return true;}
    if (pChar == '1') {return true;}
    if (pChar == '2') {return true;}
    if (pChar == '3') {return true;}
    if (pChar == '4') {return true;}
    if (pChar == '5') {return true;}
    if (pChar == '6') {return true;}
    if (pChar == '7') {return true;}
    if (pChar == '8') {return true;}
    if (pChar == '9') {return true;}

    return false; //not numeric

}//end of Viewer::isNumeric
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::parseSegmentNumberEntry
//
// Converts the user entry for the segment number into an integer and stores
// it in currentSegmentNumber.
//
// Returns true on success.  Displays an error message and returns false
// if the entry is invalid.
//

boolean parseSegmentNumberEntry()
{

    String sn = controlPanel.segmentEntry.getText();
    //strip all non-numeric characters from beginning and end of the entry
    sn = trimNonNumeric(sn);

    try{
        currentSegmentNumber = Integer.valueOf(sn);
    }
    catch(NumberFormatException nfe){
        displayErrorMessage("Illegal entry.");
        return(false);
    }

    return(true);

}//end of Viewer::parseSegmentNumberEntry
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::parseFilenameToSegmentNumber
//
// Converts the segment filename pFilename to the segment number.
//
// The "20 - "/".dat" or "30 - "/".cal" prefix/suffix will be removed and
// the remaining text converted to a number.  Preceding zeroes will be
// discarded.
//
// The number will be returned if valid.  If not valid, Integer.MIN_VALUE
// will be returned.
//

int parseFilenameToSegmentNumber(String pFilename)
{

    //strip off the prefix and suffix
    String sn = pFilename.substring(5, (pFilename.length()-4));

    //strip all non-numeric characters from beginning and end of the entry
    sn = trimNonNumeric(sn);

    int value;

    try{
        value = Integer.valueOf(sn);
    }
    catch(NumberFormatException nfe){
        return(Integer.MIN_VALUE);
    }

    return(value);

}//end of Viewer::parseFilenameToSegmentNumber
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::startPrint
//
// Begins printing the currently displayed file.
//
// This method uses the cross platform print dialog as it works better than
// the native print dialog which does not accurately pass all user changes back
// to the Java print system.
//
// Printer Resolution Issues:
//
// As of 9/8/11, there is no apparent way to determine exactly what resolution
// the printer is using.  Java code can be used to request a change, but there
// is no guarantee that the change will actually occur.  Since Java doesn't
// have any way of knowing that its request has been honored by the printer,
// it sets up scaling based on what it believes is the resolution rather than
// what the actual resolution might be.  This can result in improper scaling
// or an image printed off the paper.
//
// One example is the HP Deskjet 1000.  It ignores requests to change the
// resolution and the print quality via programming.  It seems that the only
// way to change the resolution is to use the printer preferences in Windows
// to choose Draft/Normal/Best.  If the config file is set up to use 600 DPI,
// then the user must manually set the printer preferences to "Best" so the
// printer will actually use 600 DPI.  The print quality cannot be set via
// programming as the printer seems to ignore the request.
//
// To accommodate the many printers and their problems, the print resolution
// and quality are now set in the "Configuration - General.ini" file so that
// working values can be selected for any printer.
//
// NOTE: If the entries have not been made in the ini file, the program defaults
// to 300 DPI and Normal quality.
//

void startPrint()
{

    //clear all attributes from the set
    aset.clear();

    //*** see notes regarding resolution problems in the header notes above ***

    //some attributes are added by instantiating
    PrinterResolution pR = new PrinterResolution(settings.printResolutionX,
                            settings.printResolutionY, PrinterResolution.DPI);
    aset.add(pR);

    //some attributes cannot be instantiated as their constructors are protected
    //or abstract - these are used by adding their static member variables

    if(settings.printQuality.contains("Draft")) {aset.add(PrintQuality.DRAFT);}
    if(settings.printQuality.contains("Normal")){aset.add(PrintQuality.NORMAL);}
    if(settings.printQuality.contains("High")) {aset.add(PrintQuality.HIGH);}

    aset.add(OrientationRequested.LANDSCAPE);

    //select the paper size according to what the user has selected
    if (settings.graphPrintLayout.contains("8-1/2 x 11")){
        aset.add(MediaSizeName.NA_LETTER);
    }
    else
    if (settings.graphPrintLayout.contains("8-1/2 x 14")){
        aset.add(MediaSizeName.NA_LEGAL);
    }
    else
    if (settings.graphPrintLayout.contains("A4")){
        aset.add(MediaSizeName.ISO_A4);
    }

    job = PrinterJob.getPrinterJob();

    //set this object to handle the print calls from the printing system
    job.setPrintable(this);

    //display dialog allowing user to setup the printer
    if (job.printDialog(aset)) {

        //get the current settings for imageable area - this is the area which
        //can be printed on and depends on the paper size and margins

        MediaPrintableArea mPA;
        try{
            mPA = (MediaPrintableArea) aset.get(Class.forName(
                        "javax.print.attribute.standard.MediaPrintableArea"));
        }
        catch(ClassNotFoundException cnfe){
            //if could not be retrieved, default to reasonable values for
            //Letter size with 1" margins (adjusted to 1/2" margins below)
            mPA = new MediaPrintableArea(
                   (float)25.4, (float)25.4, (float)165.1, (float)228.6,
                                                        MediaPrintableArea.MM);
        }

        //use the MediaPrintableArea retrieved from the aset, which has the
        //default values set by Java appropriate for the paper size, and adjust
        //it to decrease the margins to 1/2"

        aset.add(new MediaPrintableArea(
                   mPA.getX(MediaPrintableArea.MM) - (float)12.7,
                   mPA.getY(MediaPrintableArea.MM) - (float)12.7,
                   mPA.getWidth(MediaPrintableArea.MM) + (float)25.4,
                   mPA.getHeight(MediaPrintableArea.MM) + (float)25.4,
                   MediaPrintableArea.MM));

        //get the page range from the print dialog (adjustable by user)
        //(not used in this program, only included for educational purposes)

        //See notes at the top of the print method for details on how the
        //program uses piece ranges and page ranges specified by the user.

        PageRanges pageRanges;
        try{
            pageRanges = (PageRanges) aset.get(Class.forName(
                                "javax.print.attribute.standard.PageRanges"));
        }
        catch(ClassNotFoundException cnfe){
            //if cannot be retrieved, set to null which equates to "print all"
            pageRanges = null;
        }

        if (pageRanges == null){
            //If pageRanges is returned null, then no range was specified.
            //For the current version of the print dialog, this means "Print
            //All" was selected, so set values accordingly.
            //(not used in this program, only included for educational purposes)
            startPage = 0; endPage = 9999; pageTrack = 0;
        }
        else{
            //get the first range to be printed
            //(not used in this program, only included for educational purposes)
            int[][]rangeMembers = pageRanges.getMembers();
            startPage = rangeMembers[0][0];
            endPage= rangeMembers[0][1];
            pageTrack = startPage;
        }

        //see notes in print method for details on this variable
        printCallPageTrack = -1;

        //display a dialog window to track the progress of the print preparation
        displayPrintProgressDialog();

        //tell the print thread to start printing
        printRunnable.triggerPrint();

    }//if (job.printDialog(aset))

}//end of Viewer::startPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::print
//
// Catches calls from the printing system for requests to render images
// for printing.
//
// The user can choose a range to print on the Print Dialog. When Java
// calls the Printable object's print method, it will pass the page
// numbers to be printed, translating the user entry to zero based
// values: range 1-5 => page 0-4
//
// The print function could be called with various page numbers as some print
// dialogs allow different ranges or individual pages to be specified.  This
// doesn't work very well for this program as missing pieces could change the
// actual number of pages as could pieces which require multiple pages to
// print.
//
// The problem is handled by ignoring the page number passed in by Java as it
// would take a lot of work to match the requested pages to the appropriate
// piece files.  Instead, a dialog window is presented to the user allowing
// them to choose the range of pieces to be printed.
//
// If the Java print dialog is left on "Print All", it will continue to call
// the print method until that method returns NO_SUCH_PAGE.  This works best
// and is preferred.
//
// If the user specifies a print range, Java will only call the print method
// as many times as it deems necessary to print the specified range.  This
// may truncate the printout depending on the number of pages printed per
// piece and/or the number of missing pieces.  If the user specifies a range
// or individual pieces with starting points other than 1, the program will
// print the range specified in the piece range dialog window instead.  The
// only effect the page range specified on the print dialog will have is to
// truncate the print if that range specifies fewer pages than what will be
// required.
//
// The best method therefore is to leave the range in the Print Dialog set on
// "Print All" to ensure that all pages are printed.
//

@Override
public int print(Graphics g, PageFormat pPF, int pPage) throws
                                                            PrinterException {

    //cease printing if user has pressed Cancel button on print progress dialog
    if (printProgress.userCancel) {return(NO_SUCH_PAGE);}

    //NOTE: for some reason, the Java print system calls this function twice
    //(or perhaps more in some cases?) for each page to be printed.  This is not
    //fully explained in the Java documentation, but the consensus seems to be
    //that this behavior is correct.

    //if only the piece being viewed is to be printed, the variables should be
    // startPiece = -1 endPiece = -1 pieceTrack = -1  before starting printing
    //this way, incrementing pieceTrack once will trigger the end of printing
    //while the -1 values will suppress loading of different pieces for printing

    //if multiple pieces are to be printed, startPiece, endPiece should be set
    //to the first and last pieces and pieceTrack should be set to startPiece
    //before starting printing

    //since Java calls this function twice with the same page number to print a
    //single page, it is necessary to note that each piece has been processed
    //twice before moving to the next piece -- as we are not directly using the
    //page number supplied by Java to map directly to piece numbers, we will use
    //it to track the number of times this function has been called for each
    //page with the variable printCallPageTrack, only loading a new piece if
    //the page number has changed
    //printCallPageTrack should be set to -1 before starting printing

    //compare each page number from Java print engine with previous value --
    //each time it changes, load the next piece file as Java is expecting
    //a different page -- because it is set to -1 before starting printing, it
    //will always trigger the first time through

    if (pPage != printCallPageTrack){

        printCallPageTrack = pPage; //store for next comparison

        //only check if at end of print if the page number has changed -- this
        //ensures that the second call for that last piece has been made

        if (pieceTrack > endPiece) {return(NO_SUCH_PAGE);}

        //if startPiece is -1, don't load a new piece as the user only wants
        //to print the currently displayed piece

        if (startPiece != -1) {
            currentSegmentNumber = pieceTrack;

            //try loading segments until an existing one is found
            try{
                while (pieceTrack <= endPiece && loadSegmentThreadSafe() != 0){
                    pieceTrack++;
                    currentSegmentNumber = pieceTrack;
                }
            }
            catch(InterruptedException e){
                //if an interruption has been caught, generate a new interrupt
                //and force the exit from the job.print method which called this
                //print method so the printThread can catch the interrupt and exit
                printThread.interrupt();
                return(NO_SUCH_PAGE);
            }

            //if missing segments skipped until endPiece reached, halt printing
            if (pieceTrack > endPiece) {return(NO_SUCH_PAGE);}

            //update the progress window to show the current piece
            printProgress.setLabel(currentSegmentNumber);

        }//if (startPiece != -1)

        //next time, load the next piece or will cause the end of printing
        //if only printing the currently viewed piece
        pieceTrack++;

    }//if (pPage != printCallPageTrack)

    //print the chart to the graphics object using the main event thread
    //NOTE: need to modify so this method prints all chartGroups
    try{
        printChartGroupThreadSafe(g, pPF, pPage, chartGroups[0]);
    }
    catch(InterruptedException e){
        //if an interruption has been caught, generate a new interrupt
        //and force the exit from the job.print method which called this
        //print method so the printThread can catch the interrupt and exit
        printThread.interrupt();
        return(NO_SUCH_PAGE);
    }

    // tell the print system that page is printable
    return PAGE_EXISTS;

}//end of Viewer::print
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadSegmentThreadSafe
//
// Loads a segment using invokeLater so that it runs in the main event thread
// as the function is not thread safe.
//
// The current thread is paused until the main thread finishes loading the
// segment.
//
// The error state of the loadSegment operation is returned.
//

public int loadSegmentThreadSafe() throws InterruptedException
{

    //invoke the main event thread to load the segment

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            loadSegment(true); //load segment without dialogs

            }});

    //halt here (this is running in printThread) and wait for the main event
    //thread to complete the segment load

    printRunnable.pauseThread();

    return(loadSegmentError);

}//end of Viewer::loadSegmentThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::printChartGroupThreadSafe
//
// Prints a chart group using invokeLater so that it runs in the main event
// thread as the function is not thread safe.
//
// The current thread is paused until the main thread finishes printing.
//

public void printChartGroupThreadSafe(final Graphics pG,
          final PageFormat pPF, final int pPage, final ChartGroup pChartGroup)
                                                    throws InterruptedException
{

    //invoke the main event thread to print the chart group

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            printChartGroup(pG, pPF, pPage, pChartGroup);

            }});

    //halt here (this is running in printThread) and wait for the main event
    //thread to complete the print

    printRunnable.pauseThread();

}//end of Viewer::printChartGroupThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::displayPrintProgressDialog
//
// Displays a dialog to show the user the progress of the print preparation.
// Our code handles the print preparation before and during the Java print
// engine sending processed data on to the printer.  This dialog gives the user
// the opportunity to halt the preparation routine which can be very lengthy
// for a large print job.
//
// Any data already being sent to the printer must be halted via the Windows
// printer control panel.
//

public void displayPrintProgressDialog()
{

    //center the dialog on the main window
    int x = mainFrame.getX() + mainFrame.getWidth()/2;
    int y = mainFrame.getY() + mainFrame.getHeight()/2;

    printProgress.userCancel = false;
    printProgress.setLocation(x,y);
    printProgress.setLabel("Printing...");
    printProgress.setVisible(true);

}//end of Viewer::displayPrintProgressDialog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::printChartGroup
//
// Prints chart group pChartGroup.
//

public void printChartGroup(Graphics g, PageFormat pPF, int pPage,
                                                   ChartGroup pChartGroup){

    Graphics2D g2 = (Graphics2D) g;

    //get the width of the chart group so it can be scaled to fit on the
    //printout this can be used even if the group has not been made visible, as
    //would be the case for printing without first displaying - only need to
    //make sure the chart group component has been packed first
    int groupWidth = pChartGroup.getWidth();
    int groupHeight = pChartGroup.getHeight();

    //the margin is set at 32 (1/2"), the header takes up 1/2", this value will
    //be used to shift the imageable area down to account for the header
    int headerHeight = 15;
    int footerHeight = 15;

    //Before changing the default scaling set by Java, print the header and
    //footer at the upper left and lower left corner of the imageable area.
    //This is done before changing the scaling because the new scaling used will
    //vary depending on the width of the chart group in order to make sure it
    //fits. If the new scaling is used, the header and footer would vary in size
    //also which could result in an undesirable text size.

    //the phrase "Customer Name" should be loaded from the config file instead
    //of hard coded so that any value from the jobInfo object could be specified
    //for printing on the report - this would also solve the problem where
    //"Customer Name" is not one of the valid entries

    //g2.setColor(Color.RED); //debug mks -- remove this

    //move drawing area to the part which can actually be printed on -- inside
    //the margins
    g2.translate(pPF.getImageableX(), pPF.getImageableY());

    //printing here will always be the right size regardless of the DPI (provide
    //the printer's DPI actually matches what Java thinks it is -- see notes
    //above) because Java adjusts the scale such that 72 pixels equals one inch
    //for whatever DPI is in effect

    g2.drawString("Work Order: " + currentJobName
            + "    " + "File: " + controlPanel.segmentEntry.getText()
            + "    Customer Name: " + jobInfo.getValue("Customer Name"), 0, 10);

    String footerString;

    //get a string with entries from the Piece ID object for printing
    footerString = formatPieceIDEntriesForPrinting();

    //add Wall max/min values to the footer string
    footerString += formatAndLabelWallMinMaxForPrinting();

    //print the finished string
    g2.drawString(footerString, 0, (int)pPF.getImageableHeight());


    // *** see notes in the header above regarding Java resolution issues ***
    //
    //When this code was written, there was no easy way to get the default DPI
    //for the printer.  The default DPI is used because this DPI is guaranteed
    //to be supported by the printer.  It is necessary to know the DPI in order
    //to scale the graph to fit on the paper.  The "transform" is used by Java
    //to shift and scale the printing.  This can be retrieved and used to
    //determine the default DPI as explained below:

    //the transform from the PageFormat object rotates and shifts the final
    //output before printing - use the next line to view the transform for
    //debugging AffineTransform aT = new AffineTransform(pPF.getMatrix());

    //The transform from the Graphics2D object specifies the scaling and
    //rotation to be applied when rendering.
    //If portrait mode is used, then scaleX and scaleY of the transform matrix
    //will be non-zero and shearX and shearY will be zero.  If landscape mode
    //is used, the opposite will be true.
    //There seems to be no way to retrieve the default DPI.   It can instead be
    //calculated from the scale Java has applied to the Graphics2D object.
    //Regardless of the printer's DPI, Java applies a scale to the transform so
    //that 72 pixels on the screen will be one inch on the paper (72 ppi). If
    //you change the DPI, then Java will change the scaling so that the image is
    //still printed at the same size. The print texture will look different, but
    //the image size will remain the same. To counteract Java's scaling, the
    //scaling can be undone as shown a few lines down.

    //get a copy of the Graphics2D transform so we can deduce the scale
    //if the transform has been rotated, then the scale value will be zero and
    //the shear value will be set to the scale - use whichever is non-zero
    //the absolute value is used because the values can be negative for certain
    //rotations

    AffineTransform gAT = g2.getTransform();

    double scaleX, scaleY;

    if (gAT.getScaleX() != 0) {
        scaleX = gAT.getScaleX();
    }
    else {
        scaleX = gAT.getShearX();
    }
    scaleX = Math.abs(scaleX);

    if (gAT.getScaleY() != 0) {
        scaleY = gAT.getScaleY();
    }
    else {
        scaleY = gAT.getShearY();
    }
    scaleY = Math.abs(scaleY);

    //With no scaling, scaleX or shearX would be 1.0 depending on the paper
    //orientation.  Java scales the matrix so that 72 pixels will be one inch
    //on the paper.  Thus if the printer DPI is 600, scaleX or shearX will be
    //8.333 (600 / 72).  The scale can be removed by scaling by the inverse.
    //Whichever value is non-zero will be the scale.

    //get the inverse of the scale already applied by Java
    double unscaleX, unscaleY;
    unscaleX = 1 / scaleX;
    unscaleY = 1 / scaleY;

    //parse the printer resolution from the scale knowing that Java sets the
    //scale such that 72 pixels is one inch on the paper
    int resolutionX = (int)(scaleX * 72);
    int resolutionY = (int)(scaleY * 72);

    // User (0,0) is typically outside the imageable area, so we must
    // translate by the X and Y values in the PageFormat to avoid clipping -
    //this will take into account margins, headers, and footers.

    // translate before unscaling to position the printout properly on the paper
    // shift down to account for the header
    g2.translate(0, headerHeight);

    //remove the scale applied by Java - now the print will be at one pixel/dot
    g2.scale(unscaleX, unscaleY);

    //determine the scale required to fit the group either to fit vertically or
    //horizontally as per the user setting

    //parse inches of useable paper width (Java returns this in 1/72 per inch)
    //then multiply by the printer resolution to get the number of dots
    //reduce available height to account for the header and the footer -- scale
    //the output to fit in the leftover space
    double paperX = (pPF.getImageableWidth() / 72) * resolutionX;
    double paperY =
        ((pPF.getImageableHeight() - headerHeight - footerHeight) / 72)
                                                                * resolutionY;

    //calculate the scale so that either width or length fits the paper per the
    //user's setting

    if (settings.graphPrintLayout.contains("Fit Width")){

        //if the user does not choose "Fit to Data" for the magnification, then
        //calculate the scale to fit the entire chart width on the paper
        if (!settings.userPrintMagnify.contains("Fit to Data")){
            scaleX = paperX / groupWidth;
            scaleY = scaleX; //fix this - scaleY reflect possibly different DPI?
            }
        else{

            //if the user chooses "Fit to Data", then calcualate the width
            //required to display all the data, chopping off the unused chart
            //portion this option is not used for the "Fit Height" layouts
            //all traces should have the same amount of data, so use one trace
            //to determine the width for all

            int dataWidth = getDataWidth();

            //if no data was found, use the entire group width
            if (dataWidth == -1) {
                dataWidth = groupWidth;
            }
            else {
                dataWidth += 30;
            } //add room for the chart border

            //prevent short data widths from blowing the vertical up too much
            //allow width to be no less than half the group width
            if (((double)groupWidth / ((double)dataWidth)) > 2) {
                dataWidth = groupWidth / 2;
            }

            scaleX = paperX / dataWidth;
            scaleY = scaleX; //fix this - scaleY reflect possibly different DPI?

            }
        }//if (settings.graphPrintLayout.contains("Fit Width"))

    if (settings.graphPrintLayout.contains("Fit Height")){
        scaleY = paperY / groupHeight;
        //fix this - scaleY should reflect possibly different DPI
        scaleX = scaleY;
        }

    //apply the user's scale modification if user has not selected
    //"Fit to Data", the magnification for that case is already applied above

    if (!settings.userPrintMagnify.contains("Fit to Data")){

        //get the numeric portion of the magnify string (skip the label)
        Double magnify = Double.valueOf(settings.userPrintMagnify.substring(8));

        scaleX *= magnify;
        scaleY *= magnify;
        }

    //apply desired scaling here
    g2.scale(scaleX, scaleY);

    //disable double buffering to improve scaling and print speed
    disableDoubleBuffering(pChartGroup);
    settings.printMode = true;
    pChartGroup.print(g2);
    settings.printMode = false;
    enableDoubleBuffering(pChartGroup);

    printRunnable.unPauseThread(); //release the print thread if it is waiting

}//end of Viewer::printChartGroup
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::getDataWidth
//
// Returns the index of the last valid data point in the buffer.
//
// Returns -1 if no data found.
//

public int getDataWidth()
{

    return(
         chartGroups[0].getStripChart(0).getPlotter(0).getDataWidth());

}//end of Viewer::getDataWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::disableDoubleBuffering
//
// Disables double buffering for Component pC and all components it might
// contain.
//
// This should be done before rendering to a print graphic object so that
// scaling works properly and unnecessary data is not sent to the print spooler.
//
// This concept by Marty Hall and Bob Evans.
// http://www.apl.jhu.edu/~hall/java/Swing-Tutorial/Swing-Tutorial-Printing.html
//
// See also Viewer::disableDoubleBuffering
//

public static void disableDoubleBuffering(Component c) {

    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(false);

}//end of Viewer::disableDoubleBuffering
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::enableDoubleBuffering
//
// Enables double buffering for Component pC and all components it might
// contain.
//
// This should be done after rendering to a printer graphics object.
//
// This concept by Marty Hall and Bob Evans.
// http://www.apl.jhu.edu/~hall/java/Swing-Tutorial/Swing-Tutorial-Printing.html
//
// See also Viewer::disableDoubleBuffering
//

public static void enableDoubleBuffering(Component c)
{

    RepaintManager currentManager = RepaintManager.currentManager(c);
    currentManager.setDoubleBufferingEnabled(true);

}//end of Viewer::enableDoubleBuffering
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::formatPieceIDEntriesForPrinting
//
// Adds entries in the pieceIDInfo object to a string which can be printed.
// The resulting string is returned.
//
// Entries to be printed and the order in which they are to be printed are
// specified in the "Configuration - Piece Info Window" file.
//

public String formatPieceIDEntriesForPrinting()
{

    String result = "";

    KeyValue keyValue = new KeyValue();

    //if nothing to print, bail out
    if (!pieceIDInfo.getFirstToPrint(keyValue)) {return(result);}

    //add first entry to the footer string
    result = keyValue.keyString + ": " + keyValue.valueString + "    ";

    //add remaining printable entries to the string
    while(pieceIDInfo.getNextToPrint(keyValue)) {
        result =
            result + keyValue.keyString + ": " + keyValue.valueString + "    ";
    }

    return(result);

}//end of Viewer::printPieceIDEntriesInFooter
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::formatAndLabelWallMinMaxForPrinting
//
// Finds the min and max for the Wall chart and creates a string with labels
// which can be printed.
//
// The resulting string is returned.
//

public String formatAndLabelWallMinMaxForPrinting()
{

    String result, wallText;


    //check all chart groups for a Wall Max trace -- if there are more than one,
    //the value from the first one found will be used

    result = "Max Wall: ";

    for (int i = 0; i < numberOfChartGroups; i++){
        wallText = chartGroups[i].getWallMinOrMaxText(false, hdwVs);
        if (!wallText.isEmpty()){
            result = result + wallText;
            break;
        }
    }//for (int i = 0; i < numberOfChartGroups; i++)

    //add space between max and min
    result = result + "    Min Wall: ";

    //check all chart groups for a Wall Max trace -- if there are more than one,
    //the value from the last one found will be used

    for (int i = 0; i < numberOfChartGroups; i++){
        wallText = chartGroups[i].getWallMinOrMaxText(true, hdwVs);
        if (!wallText.isEmpty()){
            result = result + wallText;
            break;
        }

    }//for (int i = 0; i < numberOfChartGroups; i++)

    //following to display wall string in the piece ID info window:
    //pieceIDInfo.items[2].textField.setText(result);

    return(result);

}//end of Viewer::formatAndLabelWallMinMaxForPrinting
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::itemStateChanged
//
// Responds to check box changes, etc.
//
// You can tell which item was changed and how by using similar to:
//
// Object source = e.getItemSelectable();
// if (source == scopeOnCheckBox){/*do something*/}
// boolean state = false;
// if (e.getStateChange() == ItemEvent.SELECTED){/*do something*/}
//
// For simplicities sake, the following just updates all controls any time any
// one control is changed.
//

@Override
public void itemStateChanged(ItemEvent e)
{

    //NOTE: ItemEvent does not have an action command, so must detect another
    //way.

    if (e.getItemSelectable() == controlPanel.calModeCheckBox){

        //load the last file saved -- either info or cal
        loadFirstOrLastAvailableSegment(LAST);

    }// if (e.getItemSelectable() == statusPanel.calModeCheckBox)

}//end of ViewerPanel::itemStateChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Load".equals(e.getActionCommand())) {

        //parse the user entry and bail if invalid
        if (!parseSegmentNumberEntry()) {return;}

        //load the specified data file
        loadSegment(false);

    }// if ("Load".equals(e.getActionCommand()))

    if ("List".equals(e.getActionCommand())) {

        //for receiving info from the SegmentChooser
        Xfer xfer = new Xfer();
        //get a list of available segments
        loadSegmentList();
        //display a list of segments
        SegmentChooser segmentChooser =
                                new SegmentChooser(mainFrame, segmentList, xfer);
        segmentChooser.init();
        //do nothing if user did not click "Load" button
        if (!xfer.rBoolean1) {segmentList.clear(); return;}
        //place the selection into the user entry box
        controlPanel.segmentEntry.setText(xfer.rString1);
        //clear the list to conserve resources
        segmentList.clear();
        //parse the user entry and bail if invalid
        if (!parseSegmentNumberEntry()) {return;}
        //load the specified data file
        loadSegment(false);

    }// if ("List".equals(e.getActionCommand()))

    if ("Load First".equals(e.getActionCommand())) {
        //load the first segment available
        loadFirstOrLastAvailableSegment(FIRST);
    }

    if ("Load Previous".equals(e.getActionCommand())) {
        //load the segment previous to the current one (numerically)
        currentSegmentNumber--;
        if (currentSegmentNumber < 1) {currentSegmentNumber = 1;}
        loadSegment(false);
    }

    if ("Load Next".equals(e.getActionCommand())) {
        //load the segment after the current one (numerically)
        currentSegmentNumber++;
        if (currentSegmentNumber > 1000000) {currentSegmentNumber = 1000000;}
        loadSegment(false);
    }

    if ("Load Last".equals(e.getActionCommand())) {
        //load the last segment available
        loadFirstOrLastAvailableSegment(LAST);
    }

    if ("Print".equals(e.getActionCommand())) {
        //setup to print the currrently displayed piece
        startPiece = -1; endPiece = -1; pieceTrack = -1;
        startPrint();
    }

    if ("Print Multiple".equals(e.getActionCommand())) {
        //display the regular or cal print range window
        displayPrintRangeWindow();
    }

    if ("Show Info Details".equals(e.getActionCommand())){
        pieceIDInfo.setVisible(true);
    }

}//end of Viewer::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::displayPrintRangeWindow
//
// Displays one of two print range dialog windows -- one for printing regular
// pieces and one for printing calibration pieces.
//
// Two separate windows are used so each can track a separate set of ranges
// for the two types of pieces to be printed.
//

public void displayPrintRangeWindow()
{

    PrintRange dialog;

    //choose the proper dialog window depending on the state of the Cal switch
    if (!controlPanel.calModeCheckBox.isSelected()) {
        dialog = printRange;
    }
    else {
        dialog = printCalsRange;
    }

    //position the dialog near the "Print Multiple" button
    int x = mainFrame.getX() + controlPanel.getX()
                                        + controlPanel.printControls.getX();
    int y = mainFrame.getY() + controlPanel.getY();

    //display the print range window near the "Print Multiple" button
    dialog.setLocation(x, y);

    //if both the start and end piece range entries are blank, then fill in with
    //values which would print all the pieces

    if (dialog.startPiece.getText().isEmpty()
            && dialog.endPiece.getText().isEmpty() ){
        //get the first piece file in the folder -- on error default to 1
        int first = getFirstOrLastAvailableSegmentNumber(FIRST);
        if (first < 1) {first = 1;}
        //get the last piece file in the folder -- on error default to 1
        int last = getFirstOrLastAvailableSegmentNumber(LAST);
        if (last < 1) {last = 1;}

        //preset with a range including all non-calibration pieces
        dialog.startPiece.setText(first + "");
        dialog.endPiece.setText(last + "");
    }

    dialog.setVisible(true);

    //if user clicked print, then begin printing else do nothing
    if (dialog.okToPrint) {

        try{
            startPiece = Integer.valueOf(dialog.startPiece.getText());
            endPiece = Integer.valueOf(dialog.endPiece.getText());
            pieceTrack = startPiece;
            startPrint();
        }
        catch(NumberFormatException nfe)  {
            displayErrorMessage("Error in print range.");
        }

    }//if (dialog.okToPrint)

}//end of Viewer::displayPrintRangeWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::(various component listener functions)
//
// These functions are implemented per requirements of interface
// ComponentListener but do nothing at the present time.  As code is added to
// each function, it should be moved from this section and formatted properly.
//

@Override
public void componentHidden(ComponentEvent e){}
@Override
public void componentShown(ComponentEvent e){}
@Override
public void componentMoved(ComponentEvent e){}

//end of Viewer::(various component listener functions)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::componentResized
//
// Handles actions necessary when the window is resized by the user.
//

@Override
public void componentResized(ComponentEvent e)
{

    //pack the window back to its smallest size again, effectively preventing
    //the resize attempt

    mainFrame.pack();

}//end of Viewer::componentResized
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

@Override
public void displayErrorMessage(String pMessage)
{

    JOptionPane.showMessageDialog(mainFrame, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of Viewer::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::isCalSelected
//
// Gets the state of the Cal joint switch.
//

@Override
public boolean isCalSelected()
{

    return(controlPanel.calModeCheckBox.isSelected());

}//end of Viewer::isCalSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::setScrollPaneSizes
//
// Sets the size of the scroll pane by adjusting the size of the content
// pane it controls.
//
// The chartGroup values viewerChartScrollPaneWidth and
// viewerChartScrollPaneHeight should never be changed after being loaded
// from the config file as they are used as a starting point each time a new
// piece is loaded for viewing.
//

public void setScrollPaneSizes(int pWidth, int pHeight)
{

    setSizes(scrollPane, pWidth, pHeight);

}//end of Viewer::setScrollPaneSizes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadSegment
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
        if(!pQuietMode){displayErrorMessage(result);}
        mainFrame.repaint();
        printRunnable.unPauseThread(); //release the print thread if waiting
        return(result);
        }

    controlPanel.segmentEntry.setText(currentSegmentNumber + result);

    //set up the main JFrame with its children components
    resetMainFrameAndSetUp();

    printRunnable.unPauseThread(); //release the print thread if it is waiting

    return(result);

}//end of Viewer::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::windowClosing
//
// Handles actions necessary when the window is closed.
//

@Override
public void windowClosing(WindowEvent e)
{

    //probably don't have to worry about the print thread accessing a deleted
    //object as most of that thread is run in the main event thread -- as is
    //this method -- so the print thread can't be accessing anything if this
    //method is being called because both accesses are in the same thread

    //signal the thread to cancel printing so job.print will exit in the
    //printThread as soon as possible
    printProgress.userCancel = true;

    //kill the thread
    printThread.interrupt();

}//end of Viewer::windowClosing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::(various window listener functions)
//
// These functions are implemented per requirements of interface WindowListener
// but do nothing at the present time.  As code is added to each function, it
// should be moved from this section and formatted properly.
//

@Override
public void windowClosed(WindowEvent e){}
@Override
public void windowOpened(WindowEvent e){}
@Override
public void windowIconified(WindowEvent e){}
@Override
public void windowDeiconified(WindowEvent e){}
@Override
public void windowActivated(WindowEvent e){}
@Override
public void windowDeactivated(WindowEvent e){}

//end of Viewer::(various window listener functions)
//-----------------------------------------------------------------------------

}//end of class Viewer
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class ViewerControlPanel
//
// This class creates a panel for controls.
//

class ViewerControlPanel extends JPanel implements ActionListener
{

    Settings settings;
    ItemListener itemListener;
    ActionListener actionListener;
    JTextField segmentEntry;
    String currentJobName;
    JCheckBox calModeCheckBox;
    public JPanel printControls;

    JButton first, previous, next, last;
    JButton infoDetails;
    JButton print, printMultiple;
    JComboBox <String>layoutSelector;
    JComboBox <String>userMagnifySelector;
    JButton load, list;

//-----------------------------------------------------------------------------
// ViewerControlPanel::ViewerControlPanel (constructor)
//

public ViewerControlPanel(Settings pSettings, String pCurrentJobName,
                    ItemListener pItemListener, ActionListener pActionListener)
{

    settings = pSettings; currentJobName = pCurrentJobName;

    itemListener = pItemListener;
    actionListener = pActionListener;

    configure();

}//end of ViewerControlPanel::ViewerControlPanel (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerControlPanel::configure
//
// Loads configuration settings from the configuration.ini file and configures
// the object.
//

private void configure(/*IniFile pConfigFile*/)
{

    //create image icons for the controls

    //NOTE: You must use forward slashes in the path names for the resource
    //loader to find the image files in the JAR package.

    ImageIcon firstIcon = createImageIcon("images/FirstIconSmall.gif");
    ImageIcon firstIconHighlighted =
                       createImageIcon("images/FirstIconHighlightedSmall.gif");

    ImageIcon previousIcon = createImageIcon("images/PreviousIconSmall.gif");
    ImageIcon previousIconHighlighted =
                    createImageIcon("images/PreviousIconHighlightedSmall.gif");

    ImageIcon nextIcon = createImageIcon("images/NextIconSmall.gif");
    ImageIcon nextIconHighlighted =
                        createImageIcon("images/NextIconHighlightedSmall.gif");

    ImageIcon lastIcon = createImageIcon("images/LastIconSmall.gif");
    ImageIcon lastIconHighlighted =
                       createImageIcon("images/LastIconHighlightedSmall.gif");

    ImageIcon printerIcon = createImageIcon("images/PrinterOn32H.png");

    //layout for the big panel
    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

    //add panel to display info such as the job name

    JPanel infoPanel = new JPanel();
    infoPanel.setBorder(BorderFactory.createTitledBorder("Info"));
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.LINE_AXIS));

    //create a panel to hold the job number/name -- the name is broken up into
    //two lines if it is too long

    JPanel jobNamePanel = new JPanel();
    jobNamePanel.setLayout(new BoxLayout(jobNamePanel, BoxLayout.PAGE_AXIS));

    String line, line2;
    if (currentJobName.length() <= 35){
        line = currentJobName; line2 = "";
    }
    else {
        line = currentJobName.substring(0, 35);
        line2 = currentJobName.substring(35, currentJobName.length());
    }

    JLabel jNameLine = new JLabel(" Job #: " + line);
    jobNamePanel.add(jNameLine);

    if (!line2.isEmpty()){
        jNameLine = new JLabel("          " + line2);
        jobNamePanel.add(jNameLine);
    }

    infoPanel.add(jobNamePanel);

    //add a spacer to separate
    infoPanel.add(Box.createRigidArea(new Dimension(15,0))); //horizontal spacer

    infoPanel.add(infoDetails = new JButton("Details"));
    infoDetails.setActionCommand("Show Info Details");
    infoDetails.addActionListener(actionListener);

    add(infoPanel);


    add(Box.createHorizontalGlue()); //spread the panels

    //add panel with controls for navigating between files for display

    JPanel fileNavigator = new JPanel();
    fileNavigator.setLayout(new BoxLayout(fileNavigator, BoxLayout.X_AXIS));
    fileNavigator.setBorder(BorderFactory.createTitledBorder("Navigate"));
    fileNavigator.setToolTipText("Select another file to view.");

    //"Select " + settings.pieceDescription)

    fileNavigator.add(first = new JButton(firstIcon));
    first.setRolloverIcon(firstIconHighlighted);
    first.setRolloverEnabled(true);
    first.setActionCommand("Load First");
    first.addActionListener(actionListener);

    fileNavigator.add(previous = new JButton(previousIcon));
    previous.setRolloverIcon(previousIconHighlighted);
    previous.setRolloverEnabled(true);
    previous.setActionCommand("Load Previous");
    previous.addActionListener(actionListener);

    fileNavigator.add(next = new JButton(nextIcon));
    next.setRolloverIcon(nextIconHighlighted);
    next.setRolloverEnabled(true);
    next.setActionCommand("Load Next");
    next.addActionListener(actionListener);

    fileNavigator.add(last = new JButton(lastIcon));
    last.setRolloverIcon(lastIconHighlighted);
    last.setRolloverEnabled(true);
    last.setActionCommand("Load Last");
    last.addActionListener(actionListener);

    add(fileNavigator);

    add(Box.createHorizontalGlue()); //spread the panels

    //add a print control panel

    printControls = new JPanel();
    printControls.setLayout(new BoxLayout(printControls, BoxLayout.X_AXIS));
    printControls.setBorder(BorderFactory.createTitledBorder("Print"));
    printControls.setToolTipText("Printer Controls");

    //button to print the piece being viewed
    printControls.add(print = new JButton(printerIcon));
    print.setActionCommand("Print");
    print.addActionListener(actionListener);
    //horizontal spacer
    printControls.add(Box.createRigidArea(new Dimension(3,0)));

    //button to print multiple pieces

    //using HTML <center> makes the text shift to one side when the button size
    //is limited, so a non-breaking space is used to center "Print" above
    //"Multiple" and centering is not used
    String buttonText =
                    "<html>"+"&nbsp Print &nbsp"+"<br>"+"Multiple"+"</html>";
    printControls.add(printMultiple = new JButton(buttonText));
    //using HTML text makes button use too much space, so limit its size
    Viewer.setSizes(printMultiple, 65, 43);
    printMultiple.setActionCommand("Print Multiple");
    printMultiple.addActionListener(actionListener);
    //horizontal spacer
    printControls.add(Box.createRigidArea(new Dimension(3,0)));

    JPanel panel1 = new JPanel();
    panel1.setLayout(new BoxLayout(panel1, BoxLayout.PAGE_AXIS));
    printControls.setToolTipText("Layout & Magnification");

    //combo box to select paper width and scaling
    String[] layouts = {"8-1/2 x 11 : Fit Height", "8-1/2 x 11 : Fit Width",
                        "8-1/2 x 14 : Fit Height", "8-1/2 x 14 : Fit Width",
                        "A4 : Fit Height", "A4 : Fit Width"};
    layoutSelector = new JComboBox<>(layouts);
    Viewer.setSizes(layoutSelector, 150, 25);
    layoutSelector.setToolTipText("Select paper size and scaling.");
    panel1.add(layoutSelector);
    //figure out which string index matches, use first one (0) if no match
    int selected = 0;
    for (int i = 0; i < layouts.length; i++) {
        if (settings.graphPrintLayout.equalsIgnoreCase(layouts[i])) {
            selected = i;
        }
    }
    layoutSelector.setSelectedIndex(selected);
    layoutSelector.setActionCommand("Select Graph Print Layout");
    layoutSelector.addActionListener(this);

    String[] magnifyValues = {"Magnify 1.0", "Fit to Data", "Magnify 1.1",
        "Magnify 1.2", "Magnify 1.3", "Magnify 1.4", "Magnify 1.5",
        "Magnify 1.6", "Magnify 1.7", "Magnify 1.8", "Magnify 1.9",
        "Magnify 2.0"};

    userMagnifySelector = new JComboBox<>(magnifyValues);
    Viewer.setSizes(userMagnifySelector, 150, 25);
    layoutSelector.setToolTipText("Select magnification.");
    panel1.add(userMagnifySelector);
    selected = 0;
    for (int i = 0; i < magnifyValues.length; i++) {
        if (settings.userPrintMagnify.equalsIgnoreCase(magnifyValues[i])) {
            selected = i;
        }
    }
    userMagnifySelector.setSelectedIndex(selected);
    userMagnifySelector.setActionCommand("Select Magnification");
    userMagnifySelector.addActionListener(this);

    printControls.add(panel1);

    add(printControls);

    //add a panel allowing user to jump to a specific file

    JPanel gotoPanel = new JPanel();
    gotoPanel.setBorder(BorderFactory.createTitledBorder(
                                    "Choose " + settings.pieceDescription));
    gotoPanel.setLayout(new BoxLayout(gotoPanel, BoxLayout.X_AXIS));

    calModeCheckBox = new JCheckBox("View Cal "
                                            + settings.pieceDescriptionPlural);
    calModeCheckBox.setSelected(false);
    calModeCheckBox.addItemListener(itemListener);
    calModeCheckBox.setToolTipText(
                "Check this box to view calibration " +
                                            settings.pieceDescriptionPluralLC);
    gotoPanel.add(calModeCheckBox);

    segmentEntry = new JTextField("");
    segmentEntry.setToolTipText("Enter the " + settings.pieceDescriptionLC +
                                                      " number to be loaded.");
    Viewer.setSizes(segmentEntry, 70, 22);
    gotoPanel.add(segmentEntry);

    load = new JButton("Load");
    load.setActionCommand("Load");
    load.addActionListener(actionListener);
    load.setToolTipText("Load the " + settings.pieceDescriptionLC +
                                                " number entered in the box.");
    gotoPanel.add(load);

    list = new JButton("List");
    list.setActionCommand("List");
    list.addActionListener(actionListener);
    list.setToolTipText("List all " + settings.pieceDescriptionPluralLC + ".");
    gotoPanel.add(list);

    add(gotoPanel);

}//end of ViewerControlPanel::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerControlPanel::setEnabledButtonsThreadSafe
//
// Sets the enable state for the all the buttons and controls which could
// cause problems if changed or clicked while printing.
//
// Uses invokeLater to set the enabled states in order to be thread safe
// when called from any thread.
//

public void setEnabledButtonsThreadSafe(final boolean pState)
{

    javax.swing.SwingUtilities.invokeLater(
        new Runnable() {
            @Override
            public void run() {

            infoDetails.setEnabled(pState);
            first.setEnabled(pState); previous.setEnabled(pState);
            next.setEnabled(pState); last.setEnabled(pState);
            print.setEnabled(pState); printMultiple.setEnabled(pState);
            layoutSelector.setEnabled(pState);
            userMagnifySelector.setEnabled(pState);
            calModeCheckBox.setEnabled(pState);
            load.setEnabled(pState); list.setEnabled(pState);

            }});

}//end of ViewerControlPanel::setEnabledButtonsThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerControlPanel::createImageIcon
//
// Returns an ImageIcon, or null if the path was invalid.
//
// ***************************************************************************
// NOTE: You must use forward slashes in the path names for the resource
// loader to find the image files in the JAR package.
// ***************************************************************************
//

protected static ImageIcon createImageIcon(String path)
{

    //have to use the ControlPanel class since it is the one which matches the
    //filename holding this class

    java.net.URL imgURL = ControlPanel.class.getResource(path);

    if (imgURL != null) {
        return new ImageIcon(imgURL);
        }
    else {return null;}

}//end of ViewerControlPanel::createImageIcon
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerControlPanel::actionPerformed
//
// Responds to button events.
//

@Override
public void actionPerformed(ActionEvent e)
{

    if ("Select Graph Print Layout".equals(e.getActionCommand())) {
        JComboBox cb = (JComboBox)e.getSource();
        settings.graphPrintLayout = (String)cb.getSelectedItem();
        }

    if ("Select Magnification".equals(e.getActionCommand())) {
        JComboBox cb = (JComboBox)e.getSource();
        settings.userPrintMagnify = (String)cb.getSelectedItem();
        }

}//end of ViewerControlPanel::actionPerformed
//-----------------------------------------------------------------------------

}//end of class ViewerControlPanel
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SegmentFileFilter
//
// This class is used to filter files when loading them with File.list()
//

class SegmentFileFilter implements FilenameFilter
{

    public String extension = "";

//-----------------------------------------------------------------------------
// SegmentFileFilter::accept
//
// Returns true if the name parameter meets the filter requirements, false
// otherwise.
//

@Override
public boolean accept(File dir, String name)
{

    //the file satisfies the filter if it ends with the extension value
    if (name.endsWith(extension)) {
        return(true);
    } else {
        return(false);
    }

}//end of SegmentFileFilter::accept
//-----------------------------------------------------------------------------

}//end of class SegmentFileFilter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SegmentChooser
//
// Displays a window allowing user to select from a list of entries.  The
// entries are passed in to the constructor via pEntryList.
//
// The selected entry and a flag denoting that a selection was made is passed
// back via pXfer.
//

class SegmentChooser extends JDialog implements ActionListener{

    JFrame frame;
    JComboBox fileSelect;
    ArrayList<String> entryList;
    Xfer xfer;

//-----------------------------------------------------------------------------
// SegmentChooser::SegmentChooser (constructor)
//
//

public SegmentChooser(JFrame pFrame, ArrayList<String> pEntryList, Xfer pXfer)
{

    super(pFrame, "Select from List");

    frame = pFrame; entryList = pEntryList; xfer = pXfer;

}//end of SegmentChooser::SegmentChooser (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SegmentChooser::init
//
//

public void init()
{

    //position near top right of parent frame so it's convenient
    Rectangle r = frame.getBounds();
    setLocation(r.x + r.width - 200, r.y);

    xfer.rBoolean1 = false; //selection made flag - set true if user selects

    setModal(true); //window always on top and has focus until closed

    setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

    JPanel tPanel;

    add(Box.createRigidArea(new Dimension(0,15)));

    //drop down selection list for jobs
    tPanel = new JPanel();
    tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    fileSelect = new JComboBox<>((String [])entryList.toArray());
    tPanel.add(fileSelect);
    tPanel.add(Box.createRigidArea(new Dimension(5,0)));
    add(tPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    JButton button;

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));
    buttonPanel.add(button = new JButton("Load"));
    button.setToolTipText("Load the selected item.");
    button.setActionCommand("Load");
    button.addActionListener(this);

    //force wider to allow room for displaying window title
    buttonPanel.add(Box.createRigidArea(new Dimension(20,0)));

    buttonPanel.add(button = new JButton("Cancel"));
    button.setToolTipText("Cancel");
    button.setActionCommand("Cancel");
    button.addActionListener(this);
    buttonPanel.add(Box.createRigidArea(new Dimension(5,0)));

    add(buttonPanel);

    add(Box.createRigidArea(new Dimension(0,15)));

    pack();

    setVisible(true);

}//end of SegmentChooser::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SegmentChooser::actionPerformed
//
// Catches action events from buttons, etc.
//
//

@Override
public void actionPerformed(ActionEvent e)
{

    JButton source = (JButton)(e.getSource());

    if (source.getActionCommand().equalsIgnoreCase("Load")){
        loadFile();
        return;
    }

    if (source.getActionCommand().equalsIgnoreCase("Cancel")){
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
    }

}//end of SegmentChooser::actionPerformed
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SegmentChooser::loadFile
//
// Signals the calling function that the user has selected a file to load.
//

void loadFile()
{

    String selectedFile = (String)fileSelect.getSelectedItem();

    //if the user has not selected an item, close window without action
    if (selectedFile.equals("")){
        setVisible(false);
        dispose();  //destroy the dialog window
        return;
        }

    //signal the class which invoked this window that a file has been selected
    //and pass back the number of that file

    xfer.rBoolean1 = true; //set file selected flag to true
    xfer.rString1 = selectedFile; //pass back the selected file number

    setVisible(false);
    dispose();  //destroy the dialog window

}//end of SegmentChooser::loadFile
//-----------------------------------------------------------------------------

}//end of class SegmentChooser
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
