/******************************************************************************
* Title: ViewerCreator.java
* Author: Mike Schoonover
* Date: 9/25/13
*
* Purpose:
*
* This class creates a packaged Viewer setup by copying the current job
* and the necessary program elements for viewing the job data. The user is
* able to select any folder (such as a USB memory stick) to contain the setup.
*
* The resulting folder (or memory stick) can then be used anywhere to view the
* job.
*
* The copy operation is performed in a background thread so that progress can
* be displaced in GUI components.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart;

//-----------------------------------------------------------------------------

import chart.mksystems.tools.CopyTools;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

//

public class ViewerCreator extends Thread{

    JFrame mainFrame;

    String sep;
    String targetFolder;
    String rootFolderBaseName;
    String rootFolderName;
    String rootSubFolderName;
    String appFolderName;
    String jobName;
    String primaryJobFolder;
    String backupJobFolder;
    String primaryTargetFolderName;
    String backupTargetFolderName;
    String primaryFolderName;
    String backupFolderName;

    JDialog statusWindow;
    JPanel statusPanel;
    JLabel status, progress;

    String statusText = "";
    String progressText = "";

    boolean success;
    String errorMessage;

    String warningMessage = "";

//-----------------------------------------------------------------------------
// ViewerCreator::ViewerCreator (constructor)
//

public ViewerCreator(JFrame pMainFrame, String pRootFolderBaseName,
        String pJobName,
        String pPrimaryJobFolder, String pBackupJobFolder,
        String pPrimaryFolderName, String pBackupFolderName)
{

    mainFrame = pMainFrame;

    sep = File.separator; //use a short name for the separator

    rootFolderBaseName = pRootFolderBaseName;
    jobName = pJobName;
    primaryJobFolder = stripTrailingSeparator(pPrimaryJobFolder);
    backupJobFolder = stripTrailingSeparator(pBackupJobFolder);
    primaryFolderName = pPrimaryFolderName;
    backupFolderName = pBackupFolderName;

}//end of ViewerCreator::ViewerCreator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

    if (!displayConfirmationToProceedDialog()) { return; }

    if (!letUserBrowseToTargetFolder()) { return; }

    createStatusWindow();

    this.start(); //start the thread to perform the copying

}//end of ViewerCreator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::displayConfirmationToProceedDialog
//
// Gives user instructions and a chance to continue or bail out.
//
// Returns true if user wishes to continue, false if not.
//

private boolean displayConfirmationToProceedDialog()
{

    int n = JOptionPane.showConfirmDialog( mainFrame,
        "Click OK to create a Job Viewer package for viewing job data on any"
          + " computer. After clicking OK, browse to the desired folder or"
          + " memory stick in which the Viewer is to be created.",
        "Job Viewer Creator", JOptionPane.OK_CANCEL_OPTION);

    if (n == JOptionPane.OK_OPTION) {
        return(true);
    }
    else{
        return(false);
    }

}//end of ViewerCreator::displayConfirmationToProceedDialog
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::letUserBrowseToTargetFolder
//
// Allows the user to browse to the folder or memory stick in which the
// Viewer Program package is to be created.
//
// Returns true if user successfully selected a folder, false on any error.
//

private boolean letUserBrowseToTargetFolder()
{

    //create and display a file chooser
    final JFileChooser fc = new JFileChooser();
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    int returnVal = fc.showOpenDialog(mainFrame);

    if (returnVal != JFileChooser.APPROVE_OPTION){
        displayWarningMessage("Setup cancelled - nothing done.");
        return(false);
    }

    //selected file will actually be the selected directory because mode is
    //JFileChooser.DIRECTORIES_ONLY
    targetFolder = fc.getSelectedFile().toString();

    return(true);

}//end of ViewerCreator::letUserBrowseToTargetFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createStatusWindow
//
// Sets up and displays a window for displaying status and progress info
// regarding the copying process.
//

private void createStatusWindow()
{

    statusWindow = new JDialog(mainFrame, "Creating Viewer Package");

    JDialog sw = statusWindow; //short name

    //use line axis so we can add side padding
    sw.setLayout(new BoxLayout(sw.getContentPane(), BoxLayout.LINE_AXIS));

    int panelWidth = 400;
    int panelHeight = 110;

    sw.setMinimumSize(new Dimension(panelWidth, panelHeight));
    sw.setPreferredSize(new Dimension(panelWidth, panelHeight));
    sw.setMaximumSize(new Dimension(panelWidth, panelHeight));

    //use a panel to get some padding from edge of JDialog window
    statusPanel = new JPanel();

    JPanel jp = statusPanel; //short name

    jp.setLayout(new BoxLayout(jp, BoxLayout.PAGE_AXIS));

    jp.add(Box.createRigidArea(new Dimension(0,10))); //vertical spacer

    status = new JLabel("status");
    jp.add(status);

    jp.add(Box.createRigidArea(new Dimension(0,20))); //vertical spacer

    progress = new JLabel("progress");
    jp.add(progress);

    jp.add(Box.createRigidArea(new Dimension(0,10))); //vertical spacer

    //add the panel with padding spacers to each side

    sw.add(Box.createRigidArea(new Dimension(5,0))); //horizontal spacer
    sw.add(jp); //add panel to the window
    sw.add(Box.createRigidArea(new Dimension(5,0))); //horizontal spacer

    sw.pack();

    sw.setVisible(true);

}//end of ViewerCreator::createStatusWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::run
//
// The actual copying operation is performed here so that the progress can be
// displayed in GUI components.
//

@Override
public void run() {

    try{

        success = true;
        errorMessage = "";

        createViewerPackage();

        if(!success){
            displayWarningMessage(
            "One or more errors occurred. Last error encounter: "
            + errorMessage);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

    }//try
    catch (InterruptedException e) {
        return;
    }

}//end of ViewerCreator::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createViewerPackage
//
// Copies the current job and a copy of the program to the target directory.
//

private void createViewerPackage()
{

    if(!createRootFolder()) { return; }

    if(!createRootSubFolder()) { return; }

    if(!copyApplication()) { return; }

    if(!createFolderInRootSubFolder(primaryFolderName)) { return; }

    primaryTargetFolderName = rootSubFolderName + sep + primaryFolderName;

    if(!createFolderInRootSubFolder(backupFolderName)) { return; }

    backupTargetFolderName = rootSubFolderName + sep + backupFolderName;

    if (!copyJobFolders()) { return; }

}//end of ViewerCreator::createViewerPackage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createRootFolder
//
// Creates the root folder to hold everything -- the job and the program.
//
// Returns true if no error, false on failure.
//
// Also returns true if the folder already exists -- the existing folder will
// be used in that case.
//

private boolean createRootFolder()
{

    setStatus("Creating root folder...");

    rootFolderName = targetFolder + sep + rootFolderBaseName;

    setProgress(rootFolderName);

    return(createFolder(rootFolderName));

}//end of ViewerCreator::createRootFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createRootSubFolder
//
// Creates the root sub folder to hold the job and the program.
//
// Returns true if no error, false on failure.
//
// Also returns true if the folder already exists -- the existing folder will
// be used in that case.
//

private boolean createRootSubFolder()
{

    setStatus("Creating root sub folder...");

    rootSubFolderName = rootFolderName + sep + "contents";

    setProgress(rootSubFolderName);

    return(createFolder(rootSubFolderName));

}//end of ViewerCreator::createRootSubFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createFolderInRootSubFolder
//
// Creates folder pFolderName in the root sub folder.
//
// Returns true if no error, false on failure.
//
// Also returns true if the folder already exists -- the existing folder will
// be used in that case.
//

private boolean createFolderInRootSubFolder(String pFolderName)
{

    setStatus("Creating folder in root sub folder...");

    setProgress(pFolderName);

    String newFolder = rootSubFolderName + sep + pFolderName;

    return(createFolder(newFolder));

}//end of ViewerCreator::createFolderInRootSubFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createFolder
//
// Creates folder pNewFolder.
//
// Returns true if no error, false on failure.
//
// Also returns true if the folder already exists -- the existing folder will
// be used in that case.
//

private boolean createFolder(String pNewFolder)
{

    try{

        Files.createDirectory(Paths.get(pNewFolder));

    }
    catch(FileAlreadyExistsException e){
        //okay if already exists -- existing folder will be used
        return(true);
    }
    catch(Exception e){
        return(false); //error on any other exception
    }

    return(true);

}//end of ViewerCreator::createFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyApplication
//
// Copies the application and all necessary support files to the target
// folder.
//
// Returns true if successful, false if not.
//

private boolean copyApplication()
{

    appFolderName = rootSubFolderName + sep + "IRNDT IRScan Program";

    return(createAppFolders());

}//end of ViewerCreator::copyApplication
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createAppFolders
//
// Creates folders required by the application in folder appFolderName.
//
// Returns true if successful, false if not.
//

private boolean createAppFolders()
{

    if (!createFolder(appFolderName)) { return(false); }

    if (!createFolderInAppFolder("Help Files")) { return(false); }

    if (!createFolderInAppFolder("Log Files")) { return(false); }

    if (!createFolderInAppFolder("Language")) { return(false); }

    return(true);

}//end of ViewerCreator::createsAppFolders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createFolderInAppFolder
//
// Creates folder pFolderName in the application folder
//
// Returns true if no error, false on failure.
//
// Also returns true if the folder already exists -- the existing folder will
// be used in that case.
//

private boolean createFolderInAppFolder(String pFolderName)
{

    setStatus("Creating folder in application folder...");

    setProgress(pFolderName);

    String newFolder = appFolderName + sep + pFolderName;

    return(createFolder(newFolder));

}//end of ViewerCreator::createFolderInRootSubFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyJobFolders
//
// Copies the primary and backup folders to the target folder.
//
// Returns true if successful, false if not.
//

private boolean copyJobFolders()
{

    boolean primaryOK = copyFileTree(primaryJobFolder, primaryTargetFolderName);

    boolean secondaryOK = copyFileTree(backupJobFolder, backupTargetFolderName);

    if (primaryOK && secondaryOK){
        return(true);
    }
    else{
        errorMessage = CopyTools.getErrorMessage();
        return(false);
    }

}//end of ViewerCreator::copyJobFolders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyFileTree
//
// Copies folder pSource to pDestination folder. If the target folder exists,
// it will be overwritten. All folders and files and the contents therein are
// also copied.
//
// Returns true if successful, false if not.
//

private boolean copyFileTree(String pSource, String pDestination)
{

    //copy the folders without prompting for overwrite and copying the file
    //attributes as well

    return (CopyTools.copyTree(mainFrame, Paths.get(pSource),
                                    Paths.get(pDestination), false, true));

}//end of ViewerCreator::copyFileTree
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::setStatus
//
// Sets the status label text to pText in a threadsafe manner.
//

private void setStatus(String pText)
{

    //store in class variable so thread safe code can find it
    statusText = pText;

    //schedule a job for the event-dispatching thread to add the message to log

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { setStatusThreadSafe(); } });

}//end of ViewerCreator::setStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::setStatusThreadSafe
//
// Sets the status label text to statusText.
//
// Designed to be called from GUI thread by use of invokeLater.
//

private void setStatusThreadSafe()
{

    status.setText(statusText);

}//end of ViewerCreator::setStatusThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::setProgress
//
// Sets the progress label text to pText in a threadsafe manner.
//

private void setProgress(String pText)
{

    //store in class variable so thread safe code can find it
    progressText = pText;

    //schedule a job for the event-dispatching thread to add the message to log

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { setProgressThreadSafe(); } });

}//end of ViewerCreator::setProgress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::setProgressThreadSafe
//
// Sets the progress label text to statusText.
//
// Designed to be called from GUI thread by use of invokeLater.
//

private void setProgressThreadSafe()
{

    progress.setText(progressText);

}//end of ViewerCreator::setProgressThreadSafe
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::stripTrailingSeparator
//
// If pString ends with a file path separator, it is stripped from the string.
//
// Returns pString without a trailing separator.
//

private String stripTrailingSeparator(String pString)
{

    String result = pString;

    if (result.endsWith(sep)){
        result = result.substring(0, result.length()-1);
    }

    return(result);

}//end of ViewerCreator::stripTrailingSeparator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::displayWarningMessage
//
// Displays a warning dialog with message pMessage in a threadsafe manner.
//

private void displayWarningMessage(String pMessage)
{

    //store in class variable so thread safe code can find it
    warningMessage = pMessage;

    //schedule a job for the event-dispatching thread to add the message to log

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { displayWarningMessageThreadSafe(); } });

}//end of ViewerCreator::displayWarningMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::displayWarningMessageThreadSafe
//
// Displays warning message warningMessage.
//
// Designed to be called from GUI thread by use of invokeLater.
//

private void displayWarningMessageThreadSafe()
{

    JOptionPane.showMessageDialog(mainFrame, warningMessage,
                                       "Warning", JOptionPane.WARNING_MESSAGE);

}//end of ViewerCreator::displayWarningMessageThreadSafe
//-----------------------------------------------------------------------------

}//end of class ViewerCreator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
