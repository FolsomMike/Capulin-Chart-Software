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

import javax.swing.*;
import java.awt.Component;
import java.awt.Dimension;
import java.io.*;
import java.text.DecimalFormat;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.util.*;
import java.awt.Rectangle;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.*;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.MediaPrintableArea;
import java.awt.geom.AffineTransform;

import chart.mksystems.inifile.IniFile;
import chart.mksystems.globals.Globals;
import chart.mksystems.stripchart.ChartGroup;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Viewer
//

public class Viewer extends JFrame implements ItemListener, ActionListener,
                                                  ComponentListener, Printable {

Globals globals;
JobInfo jobInfo;

int numberOfChartGroups;
ChartGroup[] chartGroups;

String jobPrimaryPath, currentWorkOrder;

DecimalFormat[] decimalFormats;
int currentSegmentNumber;
String segmentDataVersion;

ViewerControlPanel controlPanel;
JPanel chartGroupPanel;
JScrollPane scrollPane;

SegmentFileFilter segmentFileFilter;

Vector<String> segmentList;

static int FIRST = 0;
static int LAST = 1;

//-----------------------------------------------------------------------------
// Viewer::Viewer (constructor)
//

public Viewer(Globals pGlobals, JobInfo pJobInfo, String pJobPrimaryPath,
                                                    String pCurrentWorkOrder)
{

super("Viewer");

//turn off default bold for Metal look and feel
UIManager.put("swing.boldMetal", Boolean.FALSE);

//force "look and feel" to Java style
try {
    UIManager.setLookAndFeel(
        UIManager.getCrossPlatformLookAndFeelClassName());
    }
catch (Exception e) {}

globals = pGlobals; jobInfo = pJobInfo;
jobPrimaryPath = pJobPrimaryPath; currentWorkOrder = pCurrentWorkOrder;

setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

addComponentListener(this);

segmentFileFilter = new SegmentFileFilter();

//create a list to hold the segment file names
segmentList = new Vector<String>();

//change the layout manager
BoxLayout boxLayout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
//mainFrame.setLayout(new BoxLayout(mainFrame, BoxLayout.Y_AXIS));
getContentPane().setLayout(boxLayout);

//create various decimal formats
decimalFormats = new DecimalFormat[1];
decimalFormats[0] = new  DecimalFormat("0000000");

configure(); //create the charts and other controls

//reset the charts
resetChartGroups();

//pack the window the first time to establish the sizes before the call to
//handleSizeChanges which creates buffers dependent on those sizes
pack();

//allow all objects to update values dependent on display sizes
//must do this before loading data as this resets the buffers
handleSizeChanges();

//load the last file saved - this is the most likely to be viewed
loadFirstOrLastAvailableSegment(LAST);

//pack again to hide charts which are set hidden in the segment data file
pack();

//let's have a look, shall we?
setVisible(true);

}//end of Viewer::Viewer (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::configure
//

public void configure()
{

IniFile configFile = null;

//if the ini file cannot be opened and loaded, exit without action
try {
    configFile = new IniFile(jobPrimaryPath
                         + "01 - " + currentWorkOrder + " Configuration.ini");
    }
    catch(IOException e){return;}

numberOfChartGroups =
          configFile.readInt("Main Configuration", "Number of Chart Groups", 1);


//this panel will be used to hold the chart group so that they can easily
//be added to a scrollpane
chartGroupPanel = new JPanel();

//create an array of chart groups per the config file setting
if (numberOfChartGroups > 0){

    //protect against too many groups
    if (numberOfChartGroups > 10) numberOfChartGroups = 10;

    chartGroups = new ChartGroup[numberOfChartGroups];

    //pass null for the hardware object as that object is not needed for viewing

    for (int i = 0; i < numberOfChartGroups; i++){
        chartGroups[i] =
          new ChartGroup(globals, configFile, i, null /*hardware*/, this, true);
        chartGroupPanel.add(chartGroups[i]);
        }

    }//if (numberOfChartGroups > 0)


//put the chartGroupPanel in a scroll pane so the user can scroll to see
//a chart wider than the screen - this is different from the program's main
//window which does not allow scrolling

//the vertical scrollbar is not needed as all charts should be viewable
//vertically - the horizontal scrollbar should always be shown so the overall
//height of the window doesn't change when the scrollbar appears or disappears

scrollPane = new JScrollPane(chartGroupPanel,
                            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

//force the size of the scrollpane - the charts inside of it will usually be
//wider to hold the entire data segment - the size is set by values in the
//config file, if there are too many charts to fit vertically, a vertical
//scroll bar will be added automatically
//The concept is to have each Chart Group displayed in a separate window during
//inspection.  Thus each viewer window probably needs an index identifier
//passed to it so it knows which group it is displaying.  For now, only one
//group is currently expected, so the window uses the values loaded for group 0.

setSizes(scrollPane,
         chartGroups[0].viewerChartScrollPaneWidth,
         chartGroups[0].viewerChartScrollPaneHeight);

add(scrollPane);

add (controlPanel =
                new ViewerControlPanel(globals, currentWorkOrder, this, this));

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

//put the filenames into the vector in their raw form so that they can be
//sorted using an alphabetical sort
for (int i=0; i<files.length; i++) segmentList.add(files[i]);
//sort the items alphabetically
Collections.sort(segmentList);

//put the array of items into the vector, converting the filenames to numbers
//ignore any names which can't be converted to a number

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
    if (segNum != Integer.MIN_VALUE) segmentList.set(i, "" + segNum);
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

for (int i = 0; i < numberOfChartGroups; i++)
                                            chartGroups[i].handleSizeChanges();

}//end of Viewer::handleSizeChanges
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadFirstOrLastAvailableSegment
//
// Loads the first or last available segment from the folder depending on the
// value of pWhich: LAST or FIRST.

// The segments are sorted alpahbetically but their names are formatted such
// that this places them in numberical order as well.
//
// Does nothing if no valid file is found.
//

void loadFirstOrLastAvailableSegment(int pWhich)
{

//load the list of available segment numbers / files
loadSegmentList();

if (segmentList.size() == 0){
    displayErrorMessage("No valid files in folder.");
    return;
    }

//attempt to convert the first or last entry in the list
try{
    if (pWhich == FIRST)
        currentSegmentNumber = Integer.valueOf(segmentList.firstElement());
    else
        currentSegmentNumber = Integer.valueOf(segmentList.lastElement());
    }
catch(NumberFormatException nfe){
    displayErrorMessage("That file is not valid.");
    return;
    }
finally{
    segmentList.clear(); //clear the list to conserve resources
    }

//load the file
loadSegment();

}//end of Viewer::loadFirstOrLastAvailableSegment
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
if (trimPoint == -1)
    return("");
else
    s = s.substring(trimPoint, s.length());


//look for first numeric character from the end

for (int i = s.length()-1; i >= 0; i--){
    if (isNumeric(s.charAt(i))){trimPoint = i; break;}
    }

//if no numeric character found, return empty string else trim non-numeric
if (trimPoint == -1)
    return("");
else
    s = s.substring(0, trimPoint+1);

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
if (pChar == '0') return true;
if (pChar == '1') return true;
if (pChar == '2') return true;
if (pChar == '3') return true;
if (pChar == '4') return true;
if (pChar == '5') return true;
if (pChar == '6') return true;
if (pChar == '7') return true;
if (pChar == '8') return true;
if (pChar == '9') return true;

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

void startPrint()
{

PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
//some attributes are added by instantiating
PrinterResolution pR = new PrinterResolution(600, 600, PrinterResolution.DPI);
//What if printer doesn't support 600 x 600?  Will probably end up as some
//default.  This shouldn't cause a problem as the print code in this program
//adapts to whatever resolution is actually used.
aset.add(pR);
//some attributes cannot be instantiated as their constructors are protected
//or abstract - these are used by adding their static member variables
aset.add(PrintQuality.NORMAL);
aset.add(OrientationRequested.LANDSCAPE);

//select the paper size according to what the user has selected
if (globals.graphPrintLayout.contains("8-1/2 x 11")){
    aset.add(MediaSizeName.NA_LETTER);
    }
else
if (globals.graphPrintLayout.contains("8-1/2 x 14")){
    aset.add(MediaSizeName.NA_LEGAL);
    }
else
if (globals.graphPrintLayout.contains("A4")){
    aset.add(MediaSizeName.ISO_A4);
    }

PrinterJob job = PrinterJob.getPrinterJob();

//set this object to handle the print calls from the printing system
job.setPrintable(this);

//display dialog allowing user to setup the printer
if (job.printDialog(aset)) {
    try {

        //get the current settings for imageable area - this is the area which
        //can be printed on and depends on the paper size and margins

        MediaPrintableArea mPA;
        try{
            mPA = (MediaPrintableArea)
                aset.get(Class.forName(
                        "javax.print.attribute.standard.MediaPrintableArea"));

            }
        catch(ClassNotFoundException cnfe){
            //if could not be retrieved, default to values for Letter size
            mPA = new MediaPrintableArea(
                   (float)12.7, (float)25.4, (float)177.8, (float)228.6,
                                                        MediaPrintableArea.MM);
            }

        //use the MediaPrintableArea retrieved from the aset, which has the
        //default values set by Java appropriate for the paper size, and adjust
        //it to decrease the margin to allow for a header to be printed - the
        //height must also be adjusted because more space will be available
        //with a decreased margin

        aset.add(new MediaPrintableArea(
                   mPA.getX(MediaPrintableArea.MM) - (float)12.7,
                   mPA.getY(MediaPrintableArea.MM),
                   mPA.getWidth(MediaPrintableArea.MM) + (float)12.7,
                   mPA.getHeight(MediaPrintableArea.MM),
                   MediaPrintableArea.MM));

        //start printing - Java will call the print function of the object
        //specified in the call to job.setPrintable (done above) which must
        //implement the Printable interface
        job.print(aset);
        }
    catch (PrinterException e) {
        displayErrorMessage("Error sending to printer.");
        }
    }

}//end of Viewer::startPrint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::print
//
// Catches calls from the printing system for requests to render images
// for printing.
//

@Override
public int print(Graphics g, PageFormat pPF, int pPage) throws
                                                        PrinterException {

//NOTE: for some reason, the Java print system calls this function twice (or
//perhaps more in some cases?) for each page to be printed.  This is not fully
//explained in the Java documentation, but the consensus seems to be that this
//behavior is correct.

//each chart group is printed on a separate page - use the page number passed
//in by the print system to select the group to be printed

if (pPage <= (numberOfChartGroups-1)){
    printChartGroup(g, pPF, pPage, chartGroups[pPage]);
    // tell the print system that page is printable
    return PAGE_EXISTS;
    }
else
    return NO_SUCH_PAGE; //returning this signals end of printing

}//end of Viewer::print
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::printChartGroup
//
// Prints chart group pChartGroup.
//

public void printChartGroup(Graphics g, PageFormat pPF, int pPage,
                               ChartGroup pChartGroup) throws PrinterException{

Graphics2D g2 = (Graphics2D) g;

//get the width of the chart group so it can be scaled to fit on the printout
//this can be used even if the group has not been made visible, as would be
//the case for printing without first displaying - only need to make sure the
//chart group component has been packed first
int groupWidth = pChartGroup.getWidth();
int groupHeight = pChartGroup.getHeight();

//the margin is set at 32 (1/2"), the header takes up 1/2", this value will
//be used to shift the imageable area down to account for the header
int headerHeight = 32;

//before changing the default scaling set by Java, print the header at the
//upper left corner of the imageable area

//the phrase "Customer Name" should be loaded from the config file instead of
//hard coded so that any value from the jobInfo object could be specified for
//printing on the report - this would also solve the problem where "Customer
//Name" is not one of the valid entries

g2.drawString("Work Order: " + currentWorkOrder
        + "    " + "File: " +
        controlPanel.segmentEntry.getText()
        + "    Customer Name: " + jobInfo.getValue("Customer Name"),
        (int)pPF.getImageableX(), (int)pPF.getImageableY() + 20);

//the transform from the PageFormat object rotates and shifts the final output
//before printing - use the next line to view the transform for debugging
//AffineTransform aT = new AffineTransform(pPF.getMatrix());

//The transform from the Graphics2D object specifies the scaling and rotation
//to be applied when rendering.
//If portrait mode is used, then scaleX and scaleY of the transform matrix will
//be non-zero and shearX and shearY will be zero.  If landscape mode is used,
//the opposite will be true.
//There seems to be no way to retrieve the default DPI.   It can instead be
//calculated from the scale Java has applied to the Graphics2D object.
//Regardless of the printer's DPI, Java applies a scale to the transform so
//that 72 pixels on the screen will be one inch on the paper (72 ppi). If you
//change the DPI, then Java will change the scaling so that the image is still
//printed at the same size. The print texture will look different, but the
//image size will remain the same. To counteract Java's scaling, the scaling
//can be undone as shown a few lines down.

//get a copy of the Graphics2D transform so we can deduce the scale
//if the transform has been rotated, then the scale value will be zero and the
//shear value will be set to the scale - use whichever is non-zero
//the absolute value is used because the values can be negative for certain
//rotations
AffineTransform gAT = g2.getTransform();

double scaleX, scaleY;

if (gAT.getScaleX() != 0) scaleX = gAT.getScaleX();
else scaleX = gAT.getShearX();
scaleX = Math.abs(scaleX);

if (gAT.getScaleY() != 0) scaleY = gAT.getScaleY();
else scaleY = gAT.getShearY();
scaleY = Math.abs(scaleY);

//With no scaling, scaleX or shearX would be 1.0 depending on the paper
//orientation.  Java scales the matrix so that 72 pixels will be one inch on the
//paper.  Thus if the printer DPI is 600, scaleX or shearX will be 8.333
//(600 / 72).  The scale can be removed by scaling by the inverse.  Whichever
//value is non-zero will be the scale.

//get the inverse of the scale already applied by Java
double unscaleX, unscaleY;
unscaleX = 1 / scaleX;
unscaleY = 1 / scaleY;

//parse the printer resolution from the scale knowing that Java sets the
//scale such that 72 pixels is one inch on the paper
int resolutionX = (int)(scaleX * 72);
int resolutionY = (int)(scaleY * 72);

// User (0,0) is typically outside the imageable area, so we must
// translate by the X and Y values in the PageFormat to avoid clipping - this
// will take into account margins, headers, and footers.

// translate before unscaling to position the printout properly on the paper
// shift down to account for the header
g2.translate(pPF.getImageableX(), pPF.getImageableY() + headerHeight);

//remove the scale applied by Java - now the print will be at one pixel per dot
g2.scale(unscaleX, unscaleY);

//determine the scale required to fit the group either to fit vertically or
//horizontally as per the user setting

//parse inches of useable paper width (Java returns this in 1/72 per inch) then
//multiply by the printer resolution to get the number of dots
//shrink height to account for the header
double paperX = (pPF.getImageableWidth() / 72) * resolutionX;
double paperY = ((pPF.getImageableHeight() - headerHeight) / 72) * resolutionY;

//calculate the scale so that either width or length fits the paper per the
//user's setting

if (globals.graphPrintLayout.contains("Fit Width")){
    scaleX = paperX / groupWidth;
    scaleY = scaleX; //fix this - scaleY should reflect possibly different DPI
    }

if (globals.graphPrintLayout.contains("Fit Height")){
    scaleY = paperY / groupHeight;
    scaleX = scaleY;  //fix this - scaleY should reflect possibly different DPI
    }

//apply desired scaling here
g2.scale(scaleX, scaleY);

//disable double buffering to improve scaling and print speed
disableDoubleBuffering(pChartGroup);
pChartGroup.print(g2);
enableDoubleBuffering(pChartGroup);

}//end of Viewer::printChartGroup
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

//NOTE: ItemEvent does not have an action command, so must detect another way.

if (e.getItemSelectable() == controlPanel.calModeCheckBox){

    //if the "View Cal Joints" checkbox is changed, load a new segment
    //to reflect the change

    //parse the user entry and bail if invalid
    if (!parseSegmentNumberEntry()) return;

    //load the specified data file
    loadSegment();

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
    if (!parseSegmentNumberEntry()) return;

    //load the specified data file
    loadSegment();

    }// if ("Load".equals(e.getActionCommand()))

if ("List".equals(e.getActionCommand())) {

    //for receiving info from the SegmentChooser
    Xfer xfer = new Xfer();
    //get a list of available segments
    loadSegmentList();
    //display a list of segments
    new SegmentChooser(this, segmentList, xfer);
    //do nothing if user did not click "Load" button
    if (!xfer.rBoolean1) {segmentList.clear(); return;}
    //place the selection into the user entry box
    controlPanel.segmentEntry.setText(xfer.rString1);
    //clear the list to conserve resources
    segmentList.clear();
    //parse the user entry and bail if invalid
    if (!parseSegmentNumberEntry()) return;
    //load the specified data file
    loadSegment();

    }// if ("List".equals(e.getActionCommand()))

if ("Load First".equals(e.getActionCommand())) {
    //load the first segment available
    loadFirstOrLastAvailableSegment(FIRST);
    }

if ("Load Previous".equals(e.getActionCommand())) {
    //load the segment previous to the current one (numerically)
    currentSegmentNumber--;
    if (currentSegmentNumber < 1) currentSegmentNumber = 1;
    loadSegment();
    }

if ("Load Next".equals(e.getActionCommand())) {
    //load the segment after the current one (numerically)
    currentSegmentNumber++;
    if (currentSegmentNumber > 1000000) currentSegmentNumber = 1000000;
    loadSegment();
    }

if ("Load Last".equals(e.getActionCommand())) {
    //load the last segment available
    loadFirstOrLastAvailableSegment(LAST);
    }

if ("Print".equals(e.getActionCommand())) {
    startPrint();
    }

}//end of Viewer::actionPerformed
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

pack();

}//end of Viewer::componentResized
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadSegment
//
// Loads the data for a segment from the primary job folder.
//
// This function should be called whenever a new segment is loaded for
// viewing or processing - each segment could represent a piece being monitored,
// a time period, etc.
//

private void loadSegment()
{

String segmentFilename;

//reset the charts
resetChartGroups();

//inspected pieces are saved with the prefix 20 while calibration pieces are
//saved with the prefix 30 - this forces them to be grouped together and
//controls the order in which the types are listed when the folder is viewed
//in alphabetical order in an explorer window

String prefix, ext;

prefix = controlPanel.calModeCheckBox.isSelected() ? "30 - " : "20 - ";
ext = controlPanel.calModeCheckBox.isSelected() ? ".cal" : ".dat";

controlPanel.segmentEntry.setText(currentSegmentNumber + ext);

segmentFilename = prefix +
                        decimalFormats[0].format(currentSegmentNumber) + ext;


loadSegmentHelper(jobPrimaryPath + segmentFilename);

repaint();

}//end of Viewer::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::loadSegmentHelper
//
// Loads the data for a segment from the specified file.  See the loadSegment
// function for more info.
//

private void loadSegmentHelper(String pFilename)
{

//create a buffered writer stream

FileInputStream fileInputStream = null;
InputStreamReader inputStreamReader = null;
BufferedReader in = null;

try{

    fileInputStream = new FileInputStream(pFilename);
    inputStreamReader = new InputStreamReader(fileInputStream, "UTF-16LE");
    in = new BufferedReader(inputStreamReader);

    in = new BufferedReader(inputStreamReader);

    processHeader(in); //handle the header section

    String line = "";

    //allow each chart group to load data, pass blank line in first time,
    //thereafter it will contain the last line read from the call to
    //loadSegment and will be passed on to the following call

    for (int i = 0; i < numberOfChartGroups; i++)
        line = chartGroups[i].loadSegment(in, line);

    }// try
catch (FileNotFoundException e){
    displayErrorMessage("Could not find the requested file.");
    }
catch(IOException e){
    displayErrorMessage(e.getMessage());
    }
finally{
    try{if (in != null) in.close();}
    catch(IOException e){}
    try{if (inputStreamReader != null) inputStreamReader.close();}
    catch(IOException e){}
    try{if (fileInputStream != null) fileInputStream.close();}
    catch(IOException e){}
    }

}//end of Viewer::loadSegmentHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::processHeader
//
// Processes the header section of a segment data file via pIn.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//

private String processHeader(BufferedReader pIn) throws IOException

{

String line;
boolean success;
Xfer matchSet = new Xfer(); //for receiving data from function calls

//read until end of file reached or "[Header Start]" section tag reached

success = false;
while ((line = pIn.readLine()) != null){
    if (matchAndParseString(line, "[Header Start]", "", matchSet)){
        success = true; break;
        }
    }

if (!success) throw new IOException(
                               "The file could not be read - missing header.");

//scan the header section and parse its entries

segmentDataVersion = "0.0"; //default in case not found

success = false;
while ((line = pIn.readLine()) != null){

    //stop if the end of the header is reached - header read is successful
    if (matchAndParseString(line, "[Header End]", "", matchSet)){
        success = true;
        break;
        }

    //read the "Segment Data Version" entry - if not found, default to "0.0"  
    if (matchAndParseString(line, "Segment Data Version", "0.0", matchSet))
        segmentDataVersion = matchSet.rString1;
    }

if (!success) throw new IOException(
                         "The file could not be read - missing end of header.");

return(line); //should be "[Header End]" tag on success, unknown value if not

}//end of Viewer::processHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::matchAndParseInt
//
// Determines if the key in pString matches pKey and parses the value to an
// integer.  If the value is invalid, pDefault will be returned.
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseInt(String pString, String pKey,
                                            int pDefault, Xfer pMatchVars)
{

//remove whitespace & force upper case
String ucString = pString.trim().toUpperCase();

//if the string does not start with the key, return default value
if (!ucString.startsWith(pKey.toUpperCase())) {
    pMatchVars.rInt1 = pDefault;
    return(false); //does not match
    }

int indexOfEqual;

//look for '=' symbol, if not found then return default
if ( (indexOfEqual = pString.indexOf("=")) == -1) {
    pMatchVars.rInt1 = pDefault;
    return(true); //key matched but parse was invalid
    }

//return the part of the line after the '=' sign - on error return default
try{
    pMatchVars.rString1 = pString.substring(indexOfEqual + 1);
    pMatchVars.rInt1 = Integer.parseInt(pMatchVars.rString1);
    return(true); //key matched, parse valid
    }
catch(NumberFormatException e){
    pMatchVars.rInt1 = pDefault;
    return(true); //key matched but parse was invalid
    }

}//end of Viewer::matchAndParseInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::matchAndParseString
//
// Determines if the key in pString matches pKey and parses the value to a
// string.  If the value is invalid, pDefault will be returned.  If the
// value is blank, pDefault will be returned.
//
// Note: this function can also be used to determine if a string contains
//  pKey regardless of whether a value exists, thus it can be used to search
//  for a section tag such as "[section]".
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseString(String pString, String pKey,
                                          String pDefault, Xfer pMatchVars)
{

//remove whitespace & force upper case
String ucString = pString.trim().toUpperCase();

//if the string does not start with the key, return default value
if (!ucString.startsWith(pKey.toUpperCase())) {
    pMatchVars.rString1 = pDefault;
    return(false); //does not match
    }

int indexOfEqual;

//look for '=' symbol, if not found then return default
if ( (indexOfEqual = pString.indexOf("=")) == -1) {
    pMatchVars.rString1 = pDefault;
    return(true); //key matched but parse was invalid
    }

//return the part of the line after the '=' sign - on error return default
try{
    pMatchVars.rString1 = pString.substring(indexOfEqual + 1);
    if (pMatchVars.rString1.equals("")) pMatchVars.rString1 = pDefault;
    return(true); //key matched, parse valid
    }
catch(StringIndexOutOfBoundsException e){
    pMatchVars.rString1 = pDefault;
    return(true); //key matched but parse was invalid
    }

}//end of Viewer::matchAndParseString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::matchAndParseString
//
// Determines if the key in pString matches pKey and parses the value to a
// boolean.  If the value is invalid, pDefault will be returned.  If the
// value is blank, pDefault will be returned.
//
// Note: this function can also be used to determine if a string contains
//  pKey regardless of whether a value exists, thus it can be used to search
//  for a section tag such as "[section]".
//
// The function returns true if the value matches pKey.
//
// The value and result flags are returned via pMatchVars.
//

static public boolean matchAndParseBoolean(String pString, String pKey,
                                             Boolean pDefault, Xfer pMatchVars)
{

//remove whitespace & force upper case
String ucString = pString.trim().toUpperCase();

//if the string does not start with the key, return default value
if (!ucString.startsWith(pKey.toUpperCase())) {
    pMatchVars.rBoolean1 = pDefault;
    return(false); //does not match
    }

int indexOfEqual;

//look for '=' symbol, if not found then return default
if ( (indexOfEqual = pString.indexOf("=")) == -1) {
    pMatchVars.rBoolean1 = pDefault;
    return(true); //key matched but parse was invalid
    }

//return the part of the line after the '=' sign - on error return default
try{
    pMatchVars.rString1 = pString.substring(indexOfEqual + 1);

    //return boolean value for the value - default for any invalid value
    if (pMatchVars.rString1.equalsIgnoreCase("true"))
        pMatchVars.rBoolean1 = true;
    else
    if (pMatchVars.rString1.equalsIgnoreCase("false"))
        pMatchVars.rBoolean1 = false;
    else
        pMatchVars.rBoolean1 = pDefault;

    return(true); //key matched, parse valid
    }
catch(StringIndexOutOfBoundsException e){
    pMatchVars.rBoolean1 = pDefault;
    return(true); //key matched but parse was invalid
    }

}//end of Viewer::matchAndParseBoolean
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::resetChartGroups
//
// Erases the chart groups and clears all data.
//

void resetChartGroups()
{

for (int i = 0; i < numberOfChartGroups; i++) chartGroups[i].resetChartGroup();

}//end of Viewer::resetChartGroups
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

JOptionPane.showMessageDialog(this, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of Viewer::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Viewer::setSizes
//
// Sets the min, max, and preferred sizes of pComponent to pX, pY.
//

static public void setSizes(JComponent pComponent, int pX, int pY)
{

pComponent.setMinimumSize(new Dimension(pX, pY));
pComponent.setPreferredSize(new Dimension(pX, pY));
pComponent.setMaximumSize(new Dimension(pX, pY));

}//end of Viewer::setSizes
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

Globals globals;
ItemListener itemListener;
ActionListener actionListener;
JLabel jobValue;
JTextField segmentEntry;
String currentWorkOrder;
JCheckBox calModeCheckBox;

//-----------------------------------------------------------------------------
// ViewerControlPanel::ViewerControlPanel (constructor)
//

public ViewerControlPanel(Globals pGlobals, String pCurrentWorkOrder,
                    ItemListener pItemListener, ActionListener pActionListener)
{

globals = pGlobals; currentWorkOrder = pCurrentWorkOrder;

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
infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));

infoPanel.add(new JLabel(" Job #: "));
jobValue = new JLabel(currentWorkOrder);
infoPanel.add(jobValue);
add(infoPanel);

add(Box.createHorizontalGlue()); //spread the panels

//add panel with controls for navigating between files for display

JPanel fileNavigator = new JPanel();
fileNavigator.setLayout(new BoxLayout(fileNavigator, BoxLayout.X_AXIS));
fileNavigator.setBorder(BorderFactory.createTitledBorder("Navigate"));
fileNavigator.setToolTipText("Select another file to view.");

//"Select " + globals.pieceDescription)

JButton first, previous, next, last;

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

JPanel printControls = new JPanel();
printControls.setLayout(new BoxLayout(printControls, BoxLayout.X_AXIS));
printControls.setBorder(BorderFactory.createTitledBorder("Print"));
printControls.setToolTipText("Printer Controls");

//button to print
JButton print;

printControls.add(print = new JButton(printerIcon));
print.setActionCommand("Print");
print.addActionListener(actionListener);

//combo box to select paper width and scaling
String[] layouts = {"8-1/2 x 11 : Fit Height", "8-1/2 x 11 : Fit Width",
                    "8-1/2 x 14 : Fit Height", "8-1/2 x 14 : Fit Width",
                    "A4 : Fit Height", "A4 : Fit Width"};
JComboBox layoutSelector = new JComboBox(layouts);
Viewer.setSizes(layoutSelector, 150, 25);
layoutSelector.setToolTipText("Select paper size and scaling.");
printControls.add(layoutSelector);
//figure out which string index matches, use first one (0) if no match
int selected = 0;
for (int i = 0; i < layouts.length; i++)
    if (globals.graphPrintLayout.equalsIgnoreCase(layouts[i])) selected = i;
layoutSelector.setSelectedIndex(selected);
layoutSelector.setActionCommand("Select Graph Print Layout");
layoutSelector.addActionListener(this);

add(printControls);

//add a panel allowing user to jump to a specific file

JPanel gotoPanel = new JPanel();
gotoPanel.setBorder(BorderFactory.createTitledBorder(
                                "Choose " + globals.pieceDescription));
gotoPanel.setLayout(new BoxLayout(gotoPanel, BoxLayout.X_AXIS));

calModeCheckBox = new JCheckBox("View Cal " + globals.pieceDescriptionPlural);
calModeCheckBox.setSelected(false);
calModeCheckBox.addItemListener(itemListener);
calModeCheckBox.setToolTipText(
            "Check this box to view calibration " +
                                              globals.pieceDescriptionPluralLC);
gotoPanel.add(calModeCheckBox);

segmentEntry = new JTextField("");
segmentEntry.setToolTipText("Enter the " + globals.pieceDescriptionLC +
                                                      " number to be loaded.");
Viewer.setSizes(segmentEntry, 70, 22);
gotoPanel.add(segmentEntry);

JButton load = new JButton("Load");
load.setActionCommand("Load");
load.addActionListener(actionListener);
load.setToolTipText("Load the " + globals.pieceDescriptionLC +
                                                " number entered in the box.");
gotoPanel.add(load);

JButton list = new JButton("List");
list.setActionCommand("List");
list.addActionListener(actionListener);
list.setToolTipText("List all " + globals.pieceDescriptionPluralLC + ".");
gotoPanel.add(list);

add(gotoPanel);

}//end of ViewerControlPanel::configure
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
    globals.graphPrintLayout = (String)cb.getSelectedItem();
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
if (name.endsWith(extension)) return(true); else return(false);

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

JComboBox fileSelect;
Xfer xfer;

//-----------------------------------------------------------------------------
// SegmentChooser::SegmentChooser (constructor)
//
//

public SegmentChooser(JFrame pFrame, Vector<String> pEntryList, Xfer pXfer)
{

super(pFrame, "Select from List");

//position near top right of parent frame so it's convenient
Rectangle r = pFrame.getBounds();
setLocation(r.x + r.width - 200, r.y);

xfer = pXfer;

xfer.rBoolean1 = false; //selection made flag - set true if user selects

setModal(true); //window always on top and has focus until closed

setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));

JPanel tPanel;

add(Box.createRigidArea(new Dimension(0,15)));

//drop down selection list for jobs
tPanel = new JPanel();
tPanel.setLayout(new BoxLayout(tPanel, BoxLayout.LINE_AXIS));
tPanel.add(Box.createRigidArea(new Dimension(5,0)));
fileSelect = new JComboBox(pEntryList);
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

}//end of SegmentChooser::SegmentChooser (constructor)
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

//signal the class which invoked this window that a file has been selected and
//pass back the number of that file

xfer.rBoolean1 = true; //set file selected flag to true
xfer.rString1 = selectedFile; //pass back the selected file number

setVisible(false);
dispose();  //destroy the dialog window

}//end of SegmentChooser::loadFile
//-----------------------------------------------------------------------------

}//end of class SegmentChooser
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
