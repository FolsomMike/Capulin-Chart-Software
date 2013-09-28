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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.ListIterator;
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
            //displayWarningMessage is threadsafe
            displayWarningMessage(
            "One or more errors occurred. Last error encountered: "
            + errorMessage);
        }

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

    }//try
    catch (InterruptedException e) {
        return;
    }
    finally{

        disposeOfStatusWindow();

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

    if(!createBasicInstructionFile()) { return; }

    if(!createViewerStarterCmdFile()) { return; }

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
        success = false;
        errorMessage = e.getMessage() + "; Cannot create folder: " + pNewFolder;
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

    if (!createAppFolders()) { return(false); }

    if (!copyAppFiles()) { return(false); }

    if (!createViewerModeConfigFile()) { return(false); }

    if (!createMainSettingsConfigFile()) { return(false); }

    if (!createMainStaticSettingsConfigFile()) { return(false); }

    return(true);

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

    if (!copyFolderToAppFolder("Help Files")) { return(false); }

    if (!createFolderInAppFolder("Log Files")) { return(false); }

    if (!copyFolderToAppFolder("Language")) { return(false); }

    return(true);

}//end of ViewerCreator::createsAppFolders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyAppFiles
//
// Copies files required by the application to the target folder.
//
// Returns true if successful, false if not.
//

private boolean copyAppFiles()
{

    if (!copyFileToTargetAppFolder("Chart.jar")) { return(false); }


    if (!copyFileToTargetAppFolder(
                        "Configuration - General.ini")) { return(false); }

    if (!copyFileToTargetAppFolder(
                "Configuration - Job Info Window.ini")) { return(false); }

    if (!copyFileToTargetAppFolder(
                "Configuration - Piece Info Window.ini")) { return(false); }

    if (!copyFileIfExistsToTargetAppFolder(
                "Configs Have Been Converted To UTF-8")) { return(false); }

    return(true);

}//end of ViewerCreator::copyAppFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyFileToTargetAppFolder
//
// Copies pFile to the target application folder.
//
// Returns true if successful, false if not.
//

private boolean copyFileToTargetAppFolder(String pFile)
{

    String destination = appFolderName + sep + pFile;

    //copy without prompting user to overwrite; preserve file attributes

    boolean result = CopyTools.copyFile(mainFrame,
                    Paths.get(pFile), Paths.get(destination), false, true);

    if (!result) {
        success = false;
        errorMessage = CopyTools.getErrorMessage();
    }

    return(result);

}//end of ViewerCreator::copyFileToTargetAppFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyFileIfExistsToTargetAppFolder
//
// Copies pFile if it exists to the target application folder. If it does not
// exist, nothing is done and no error is recorded.
//
// Returns true if successful, false if not.
//

private boolean copyFileIfExistsToTargetAppFolder(String pFile)
{

    //do nothing if file does not exist -- not an error
    if (Files.notExists(Paths.get(pFile))) { return(true); }

    boolean result = copyFileToTargetAppFolder(pFile);

    //success flag and error message already set by copyFileToTargetAppFolder

    return(result);

}//end of ViewerCreator::copyFileIfExistsToTargetAppFolder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::copyFolderToAppFolder
//
// Copies the folder pSource and all of its contents from the app's root folder
// to the target app folder.
//
// Returns true if successful, false if not.
//

private boolean copyFolderToAppFolder(String pSource)
{

    //get actual path to the file in the current folder, which in this case
    //is the application's root folder

    Path sourcePath = Paths.get(pSource).toAbsolutePath();

    //copy the folder and its contents to the target folder
    boolean result = copyFileTree(sourcePath.toString(), appFolderName);

    //success flag and error message already set by copyFileTree

    return(result);

}//end of ViewerCreator::copyFolderToAppFolder
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
// ViewerCreator::createViewerModeConfigFile
//
// Creates file "Viewer Mode.ini" in the target app root folder with entries
// appropriate for the Viewer package.
//
// The presence of this file will cause the application into "Viewer Mode". In
// this mode, the hardware will be ignored, no license key is needed, help
// messages are displayed appropriate for use as a viewer.
//
// Returns true if successful, false if not.
//

private boolean createViewerModeConfigFile()
{

    String filename = appFolderName + sep + "Viewer Mode.ini";

    ArrayList<String> contents = new ArrayList<>();

    // add explanation comments

    contents.add("");
    contents.add(
            "; When this file is present in the same folder as Chart.jar,");
    contents.add(
            "; the program will start in \"Viewer Mode\". The hardware will");
    contents.add(
            "; be ignored, no license key is required, help messages related");
    contents.add(
            "; to viewing will be displayed, and various other tweaks will");
    contents.add(
            "; be implemented to streamline the viewing operations.");
    contents.add("");
    contents.add(
            "; Rename this file to turn off \"Viewer Mode\".");
    contents.add("");

    contents.add("[Main Configuration]");
    contents.add("");
    contents.add("Width=full screen");

    boolean result = createFileContainingContents(filename, contents);

    return(result);

}//end of ViewerCreator::createViewerModeConfigFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createMainSettingsConfigFile
//
// Creates file "Main Settings.ini" in the target app root folder with entries
// appropriate for the Viewer package.
//
// The Viewer will be set up to load jobName (the current job passed to this
// creator) on startup.
//
// Returns true if successful, false if not.
//

private boolean createMainSettingsConfigFile()
{

    String filename = appFolderName + sep + "Main Settings.ini";

    ArrayList<String> contents = new ArrayList<>();

    contents.add("[Main Configuration]");
    contents.add("");
    contents.add("Current Work Order=" + jobName);

    boolean result = createFileContainingContents(filename, contents);

    return(result);

}//end of ViewerCreator::createMainSettingsConfigFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createMainStaticSettingsConfigFile
//
// Creates file "Main Static Settings.ini" in the target app root folder with
// entries appropriate for the Viewer package.
//
// The Viewer will be set up to load jobs from folders specific to the
// viewer and contained in the same root folder.
//
// Returns true if successful, false if not.
//

private boolean createMainStaticSettingsConfigFile()
{

    String filename = appFolderName + sep + "Main Static Settings.ini";

    ArrayList<String> contents = new ArrayList<>();

    contents.add("[Main Configuration]");
    contents.add("");

    contents.add("Primary Data Path=.." + sep + primaryFolderName + sep);
    contents.add("Backup Data Path=.." + sep + backupFolderName + sep);

    boolean result = createFileContainingContents(filename, contents);

    return(result);

}//end of ViewerCreator::createMainStaticSettingsConfigFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createBasicInstructionFile
//
// Creates file "The latest version of Java...." in the viewer package's root
// folder containing some basic instructions.
//
// Returns true if successful, false if not.
//

private boolean createBasicInstructionFile()
{

    String name =
            "The latest version of Java must be installed on your computer to "
            + "use this viewer. (visit www.java.com to install).txt";

    String filename = rootFolderName + sep + name;

    ArrayList<String> contents = new ArrayList<>();

    contents.add("");

    contents.add(
      "The latest version of Java must be installed on your computer to use");
    contents.add("this viewer program.");
    contents.add("");
    contents.add("Java can be downloaded and installed by visiting:");
    contents.add("");
    contents.add("    www.java.com");

    boolean result = createFileContainingContents(filename, contents);

    return(result);

}//end of ViewerCreator::createBasicInstructionFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createViewerStarterCmdFile
//
// Creates a Windows CMD file in the viewer package root folder to provide
// a simple method for the user to start the program.
//
// Returns true if successful, false if not.
//

private boolean createViewerStarterCmdFile()
{

    String name = "Click This to Run the IR Scan Viewer Program.cmd";

    String filename = rootFolderName + sep + name;

    ArrayList<String> contents = new ArrayList<>();

    contents.add("");

    contents.add("cd \"contents\\IRNDT IRScan Program\"");

    //use the start command to start the program so the command prompt window
    //does not stay open -- start begins a new session other than the one the
    //cmd file is running in so it runs independently; the first "" is used
    //as the name of the session window and is left blank here

    contents.add("start \"\" \"Chart.jar\"");

    boolean result = createFileContainingContents(filename, contents);

    return(result);

}//end of ViewerCreator::createViewerStarterCmdFile
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
        //success flag and error message already set by copyFileTree
        return(false);
    }

}//end of ViewerCreator::copyJobFolders
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::createFileContainingContents
//
// Creates text file pFilename containing the lines of text in pContents.
//
// Returns true if successful, false if not.
//

private boolean createFileContainingContents(String pFilename,
                                                ArrayList <String> pContents)
{

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter out = null;

    try{

        fileOutputStream = new FileOutputStream(pFilename);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
        out = new BufferedWriter(outputStreamWriter);

        ListIterator i;

        //write each line in the buffer to the file

        for (i = pContents.listIterator(); i.hasNext(); ){
            out.write((String)i.next());
            out.newLine();
            }

        //Note! You MUST flush to make sure everything is written.

        out.flush();

    }
    catch(IOException e){
        success = false;
        errorMessage = e.getMessage() + "; Cannot create file: " + pFilename;
        return(false); //error on any other exception
    }
    finally{
        try{
            if (out != null) {out.close();}
            if (outputStreamWriter != null) {outputStreamWriter.close();}
            if (fileOutputStream != null) {fileOutputStream.close();}
        }
        catch(IOException e){
        success = false;
        errorMessage = e.getMessage() + "; Cannot create file: " + pFilename;
        return(false); //error on any other exception}
        }
    }

    return(true);

}//end of ViewerCreator::copyFileIfExistsToTargetAppFolder
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

    boolean result = CopyTools.copyTree(mainFrame, Paths.get(pSource),
                                    Paths.get(pDestination), false, true);

    if (!result) {
        success = false;
        errorMessage = CopyTools.getErrorMessage();
    }

    return(result);

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

    //schedule a job for the event-dispatching thread to do the work

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

    //schedule a job for the event-dispatching thread to do the work

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

//-----------------------------------------------------------------------------
// ViewerCreator::disposeOfStatusWindow
//
// Closes the status window and releases its resources in a threadsafe
// manner.
//

private void disposeOfStatusWindow()
{

    //schedule a job for the event-dispatching thread to do the work

    javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                @Override
                public void run() { disposeOfStatusWindowThreadSafe(); } });

}//end of ViewerCreator::disposeOfStatusWindow
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ViewerCreator::disposeOfStatusWindowThreadSafe
//
// Closes the status window and releases its resources.
//
// Designed to be called from GUI thread by use of invokeLater.
//

private void disposeOfStatusWindowThreadSafe()
{

    statusWindow.dispose();
    statusWindow = null;

}//end of ViewerCreator::disposeOfStatusWindowThreadSafe
//-----------------------------------------------------------------------------


}//end of class ViewerCreator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
