/******************************************************************************
* Title: JobValidator.java
* Author: Mike Schoonover
* Date: 3/11/10
*
* Purpose:
*
* This class verifies that the job files (excluding the inspection data files)
* are valid and repairs them if possible.
*
* The class has two different modes: robust and non-robust as specified by the
* parameter pRobust.  If pRobust is false, the root data folders are not
* repaired.  This mode is to be used each time a job is loaded.  If pRobust is
* true, the root data folders are repaired.  This mode is to be used explicitly
* for a more extensive repair invoked at the behest of a technician.
*
* Operation for Non-Robust Mode:
*
* Either root path is empty: error msg, no repair, xfer.rBoolean2 = true
* Either root path is missing:  error msg, no repair, xfer.rBoolean2 = true
* Job name is empty: no msg, no repair, xfer.rBoolean3 = true
* Job folder missing in both roots: no msg, no repair, xfer.rBoolean3 = true
* Primary job folder missing:
*   Folder recreated: repaired message
*   All config files replaced: repaired/failed message for each file
*       (the files are copied from the opposite root data folder if possible,
*           reloaded from the default configs necessary)
*   If folder cannot be recreated: error message, xfer.rBoolean3 = true
* Backup job folder missing: same as for Primary job folder
*
* Operation for Robust Mode:
*
* Same as Non-Robust mode except:
* Either root path is missing:
*   Folder recreated: repaired message
*   If folder cannot be recreated: error message, xfer.rBoolean3 = true
*
* It is intended that this class be called upon job loading with pRobust
* set false so that the root folders are not recreated if missing.  The class
* can be called explicitly from a menu option with pRobust set true to correct
* for missing root paths.  Since a missing root path is a major error but might
* be caused by a network connection problem, the repair of such should only be
* done as instructed by a technician after verifying that all network
* connections are valid.
*
* All repair actions are logged in a text file in the primary job folder:
*   "15 - [the job name] Repair Log.txt"
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.io.*;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class JobValidator
//
// See notes at top of page.
//

class JobValidator extends Object {

    Xfer xfer;
    boolean robust;
    String primaryDataPath, backupDataPath;
    String currentJobPrimaryPath, currentJobBackupPath;
    String jobName;
    PrintWriter logFile = null;
    boolean pathRecreated = false;

    //file type enumerators
    //used to specify which type of file is being processed
    static int CAL_FILE = 0;
    static int CONFIG_FILE = 1;
    static int PIECE_NUMBER_FILE = 2;
    static int JOB_INFO_FILE = 3;
    static int JOB_INFO_CONFIG_FILE = 4;

    //used to specify which folder is being repaired
    static int PRIMARY = 0;
    static int BACKUP = 1;

    //various success message strings tied to the file type enumerators
    String SuccessMsgs[] = {

        "The calibration file was restored from the backup version."
            + " Please check all calibration settings.",

        "The configuration file was restored from the backup version."
            + " Please check all calibration settings.",

        "The file containing the number of the next piece to be inspected"
            + " was restored from the backup version."
            + " Please check that the number is correct.",

        "The job information file was restored from the backup version."
            + " Please check that the job information is correct.",

        "The job information configuration file"
            + " was restored from the backup version."
            + " Please check that the job information is correct."
        };

    //various failure message strings tied to the file type enumerators
    String FailureMsgs[] = {

      "The calibration file was damaged or missing and could not be restored."
            + " Please reset all calibration settings.",

      "The configuration file was damaged or missing and could not be restored."
            + " Please select the proper configuration file.",

      "The file containing the number of the next piece to be inspected"
            + " was damaged or missing and could not be restored."
            + " Please enter the correct number.",

      "The job information file"
            + " was damaged or missing and could not be restored."
            + " Please re-enter the job information.",

      "" //no failure message here for JOB_INFO_CONFIG_FILE
      };

//-----------------------------------------------------------------------------
// JobValidator::JobValidator (constructor)
//

public JobValidator(String pPrimaryDataPath, String pBackupDataPath,
                                   String pJobName, boolean pRobust, Xfer pXfer)
{

    jobName = pJobName; robust = pRobust; xfer = pXfer;

    primaryDataPath = pPrimaryDataPath; backupDataPath = pBackupDataPath;

    currentJobPrimaryPath = pPrimaryDataPath + jobName + File.separator;
    currentJobBackupPath = pBackupDataPath + jobName + File.separator;

    try{

        xfer.rBoolean2 = xfer.rBoolean3 = false;

        //check to see if the root data paths exist or can be repaired if robust
        if (!validatePathAndRepair(pPrimaryDataPath, "root primary", robust)
            || !validatePathAndRepair(pBackupDataPath, "root backup", robust)
            ) {

            xfer.rBoolean2 = true; return;

        }

        //if no path repair option and both job folders are missing, assume
        //that the job does not exist at all - only do this check if robust if
        //false
        if (!robust && !validateEitherPath()){xfer.rBoolean3 = true; return;}

        //if the primary job folder could not be verified or repaired, bail out
        if (!validatePathAndRepair(currentJobPrimaryPath, "primary job", true)){
            xfer.rBoolean3 = true; return;
        }

        //open the repair log file in case it is needed - data is appended to
        //the log file if it already exists
        //the log file is created after verifying the existence of the primary
        //directory because it is stored there
        logFile = new PrintWriter(new FileWriter(
          currentJobPrimaryPath + "15 - " + jobName + " Repair Log.txt", true));

        //if the call to validatePath for the primary folder caused the primary
        //folder to be recreated, log that error
        if(pathRecreated){
            logMessage("Error - primary path does not exist: " +
                                                  currentJobPrimaryPath, true);
            logMessage("Action - creating primary path.");
            pathRecreated = false;
        }

        //if the backup job folder could not be verified or repaired, bail out
        if (!validatePathAndRepair(currentJobBackupPath, "backup job", true)){
            xfer.rBoolean3 = true; return;
        }

        //if the call to validatePath for the backup folder caused the backup
        //folder to be recreated, log that error
        if(pathRecreated){
            logMessage("Error - backup path does not exist: " +
                                                    currentJobBackupPath, true);
            logMessage("Action - creating backup path.");
            pathRecreated = false;
        }

        //validate/repair the primary folder files
        validate("00 - ", " Calibration File.ini", CAL_FILE, PRIMARY);
        validate("01 - ", " Configuration.ini", CONFIG_FILE, PRIMARY);
        validate("02 - ", " Piece Number File.ini", PIECE_NUMBER_FILE, PRIMARY);
        validate("03 - ", " Job Info.ini", JOB_INFO_FILE, PRIMARY);
        validate("04 - ", " Configuration - Job Info Window.ini",
                                                 JOB_INFO_CONFIG_FILE, PRIMARY);

        //validate/repair the backup folder files
        validate("00 - ", " Calibration File.ini", CAL_FILE, BACKUP);
        validate("01 - ", " Configuration.ini", CONFIG_FILE, BACKUP);
        validate("02 - ", " Piece Number File.ini", PIECE_NUMBER_FILE, BACKUP);
        validate("03 - ", " Job Info.ini", JOB_INFO_FILE, BACKUP);
        validate("04 - ", " Configuration - Job Info Window.ini",
                                                  JOB_INFO_CONFIG_FILE, BACKUP);

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 213");
    }
    finally{
        if (logFile != null) {logFile.close();}
    }

}//end of JobValidator::JobValidator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::validateEitherPath
//
// Verifies that both root data paths and the job name are not empty and that
// the job folder exists in at least one path.  No attempt is made for repair.
//
// If at least one path is good, this method returns true.
// If both paths were bad, returns false.
//
// This can be used to catch cases where the job does not exist in either
// path and may be assumed to have been removed - in this case, it may not be
// an error.
//

private boolean validateEitherPath()
{

    //if the jobName is empty, validation fails
    if (jobName.equals("")) {return(false);}

    //verify that the job folder exists in at least one path
    File folder1 = new File(currentJobPrimaryPath);
    File folder2 = new File(currentJobBackupPath);
    if (folder1.exists() || folder2.exists()) {return(true);}

    return(false);

}//end of JobValidator::validateEitherPath
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::validatePathAndRepair
//
// Verifies that pPath exists and attempts to repair it if pRepair is true.
//
// If the pPath is empty, this method returns false.
// If the path is good or was repaired, this method returns true.
// If the path was bad and unrepairable or pRepair is false, returns false.
//

private boolean validatePathAndRepair(String pPath, String pIdentifier,
                                                               boolean pRepair)
{

    //if the specified path is blank, bail out with error
    if (pPath.equals("")){
        displayErrorMessage("The " + pIdentifier +
                           " folder specified is blank - cannot be repaired.");
        return(false);
    }

    File folder = new File(pPath);

    //all good
    if (folder.exists()) {return(true);}

    //path bad, don't repair if pRepair is false
    if (!pRepair) {
        displayErrorMessage("The " + pIdentifier +
                                  " folder was missing and was not repaired.");
        return(false);
    }

    //try to create the path
    if (folder.mkdirs()){
        displayErrorMessage("The " + pIdentifier +
                                      " folder was missing and was repaired.");
        pathRecreated = true;
        return(true);
    }
    else{
        displayErrorMessage("The " + pIdentifier +
                             " folder was missing and could not be repaired.");
        return(false);
    }

}//end of JobValidator::validatePathAndRepair
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::validate
//
// Verifies that pFilename exists in the Primary or Backup folder and replaces
// it from the opposite folder if necessary.  If the copy fails, certain files
// may be replaced from the default set.
//
// The Primary/Backup folder is specified by pWhichFolder: PRIMARY or BACKUP.
//
// All repair actions are logged in a text file in the primary folder.
//

private void validate(String pPrefix, String pFilename, int pFileType,
                                                               int pWhichFolder)
{

    String toFix = "", backup = "";
    //select the folder to be repaired and the folder to provide backup copies
    if  (pWhichFolder == PRIMARY){
        toFix = currentJobPrimaryPath;
        backup = currentJobBackupPath;
    }

    if  (pWhichFolder == BACKUP){
        toFix = currentJobBackupPath;
        backup = currentJobPrimaryPath;
    }

    String filename = toFix + pPrefix + jobName + pFilename;
    String filenameBackup = backup + pPrefix + jobName + pFilename;

    File file = new File(filename);

    if (!file.exists()) {
        handleMissingFile(filenameBackup, filename, pFileType);
    }

}//end of JobValidator::validate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::handleMissingFile
//
// Used to copy the backup version of a file to the primary folder.
// Actually copies whatever file is specified in pSource to file pDestination.
// If the repair succeeds, a success message is displayed for the user.
// If the repair fails, a failure message is displayed for the user.
// The success and failure messages are selected depending on the value
// of pFileType.
//
// Returns true if the action was successful, false if not.
//
// All repair actions are logged in a text file in the primary folder.
//

public boolean handleMissingFile(String pSource, String pDestination,
                                                                int pFileType)
{

    logMessage("Error - file does not exist: " + pDestination, true);

    boolean success;

    success = copyFileFromBackup(pSource, pDestination);

    //display the appropriate success or failure message for the file type

    if (success) {
        displayErrorMessage(SuccessMsgs[pFileType]);
    }
    else{
        //display the error message for the file type if message is not blank
        if (!FailureMsgs[pFileType].equals("")) {
            displayErrorMessage(FailureMsgs[pFileType]);
        }
    }

    //if the job info configuration file could not be restored from the backup
    //version, copy the default version to both directories
    if (!success && pFileType == JOB_INFO_CONFIG_FILE) {
        success =
             handleJobInfoConfigFileBackupRestoreFailure(pSource, pDestination);
    }

    //if the job configuration file could not be restored from the backup
    //version, allow the user to select appropriate file from list of defaults
    if (!success && pFileType == CONFIG_FILE) {
        success = handleConfigFileBackupRestoreFailure();
    }

    return success;

}//end of JobValidator::handleMissingFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::handleConfigFileBackupRestoreFailure
//
// Allows the user to select the proper configuration file to be used for
// the job from a list.
//
// NOTE: The user should normally select the same configuration file as was
// first selected when the job was created.
//

public boolean handleConfigFileBackupRestoreFailure()

{

    LoadConfiguration loadConfiguration =
       new LoadConfiguration(null, currentJobPrimaryPath, currentJobBackupPath,
                                                                 jobName, xfer);

    return true;

}//end of JobValidator::handleConfigFileBackupRestoreFailure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::handleJobInfoConfigFileBackupRestoreFailure
//
// Copies the default file for the job info window configuration.  This is the
// same file that would be copied to the primary and backup directories when
// a job is first created.
//

public boolean handleJobInfoConfigFileBackupRestoreFailure(
                                String pFilenamePrimary, String pFilenameBackup)

{

    //put a copy of the Job Info Window configuration file into the job folder
    //this makes sure that the same configuration file will always be used when
    //the job is loaded
    //note that the "04 - " prefix is to force the file near the top of the
    //explorer window when the files are alphabetized to make it easier to find

    boolean successP, successB = false;

    if (!(successP =
             copyFile("Configuration - Job Info Window.ini", pFilenamePrimary))
        ||
        !(successB =
             copyFile("Configuration - Job Info Window.ini", pFilenameBackup))){

        displayErrorMessage("The job information configuration file "
            + " was damaged or missing and could not be repaired."
            + " Please contact technical support.");

    }
    else{
        displayErrorMessage("The job information configuration file"
            + " was damaged or missing and replaced with the default version.");
    }

    //if file could not be copied to either directory, return error
    if (!successP || !successB) {
        return false;
    }
    else {
        return true;
    }

}//end of JobValidator::handleJobInfoConfigFileBackupRestoreFailure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::copyFileFromBackup
//
// Used to copy the backup version of a file to the primary folder.
// Actually copies whatever file is specified in pSource to file pDestination.
//
// Returns true if the action was successful, false if not.
//
// All repair actions are logged in a text file in the primary folder.
//

public boolean copyFileFromBackup(String pSource, String pDestination)
{

    logMessage("Action - copying from backup folder: " + pSource);

    boolean success;

    if (!(success = copyFile(pSource, pDestination))) {
        logMessage("Error - cannot copy from backup folder: " + pSource);
    }

    return success;

}//end of JobValidator::copyFileFromBackup
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::logMessage
//
// Appends pMessage to the text file logFile.  If logFile is null, does nothing.
// If pLogDate is true, the current date and time are logged on a separate
// line before the message.
//
// See logMessage method without the pLogDate parameter for a shorthand
// way of logging a message with no date.
//

private void logMessage(String pMessage, boolean pLogDate)
{

    if (pLogDate) {logFile.println(new Date().toString());}

    logFile.println(pMessage);

}//end of JobValidator::logMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::logMessage
//
// Appends pMessage to the text file logFile.  If logFile is null, does nothing.
//
// Use this method to print a message without logging the date.
//

private void logMessage(String pMessage)
{

    logMessage(pMessage, false);

}//end of JobValidator::logMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::copyFile
//
// Copies file pSource to pDest.
//
// Returns true if the copy was successful, false otherwise.
//

boolean copyFile(String pSource, String pDest)
{

    FileInputStream in = null;
    FileOutputStream out = null;

    try {

        in = new FileInputStream(pSource);
        out = new FileOutputStream(pDest);

        int c;

        while ((c = in.read()) != -1) {out.write(c); }

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 545");
        return (false);
    }
    finally {
        try{
            if (in != null) {in.close();}
            if (out != null) {out.close();}
        }
        catch(IOException e){
            logSevere(e.getMessage() + " - Error: 554");
            return(false);
        }
    }

    return(true);

}//end of JobValidator::copyFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

    JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of JobValidator::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of JobValidator::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of JobValidator::logStackTrace
//-----------------------------------------------------------------------------

}//end of class JobValidator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------