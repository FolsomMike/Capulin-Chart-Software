/******************************************************************************
* Title: JobValidator.java
* Author: Mike Schoonover
* Date: 3/11/10
*
* Purpose:
*
* This class verifies that the job files (excluding the inspection data files)
* are valid.
*
* If constructor parameter pRobust is passed in true, the primary and backup
* folders will be re-created if they are missing.  If false, the repair
* will be aborted if either folder is missing.
*
* It is intended that this class be called upon program restart with pRobust
* set false so that folders are not recreated if missing.  The class can be
* called explicitly from a menu option with pRobust set true to correct for
* missing paths.  Since a missing path is a major error but might be caused by
* a network connection problem, the repair of such should only be done as
* instructed by a technician after verifying that all network connections are
* valid.
*
* If a file is missing or corrupt in the primary folder, a copy is made from
* the file in the backup directory.  If the file is missing or corrupt in the
* backup folder, an error message is displayed.
*
* All repair actions are logged in a text file named:
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

public JobValidator(String pCurrentJobPrimaryPath, String pCurrentJobBackupPath,
                                   String pJobName, boolean pRobust, Xfer pXfer)
{

currentJobPrimaryPath = pCurrentJobPrimaryPath;
currentJobBackupPath = pCurrentJobBackupPath;
jobName = pJobName; robust = pRobust; xfer = pXfer;

try{

    //if the primary path could not be verified or repaired, bail out
    if (!validatePath(currentJobPrimaryPath, "primary")) return;

    //open the repair log file in case it is needed - data is appended to the
    //log file if it already exists
    //the log file is created after verifying the existence of the primary
    //directory because it is stored there
    logFile = new PrintWriter(new FileWriter(
      currentJobPrimaryPath + "15 - " + jobName + " Repair Log.txt", true));

    //if the call to validatePath for the primary folder caused the primary
    //folder to be recreated, log that error
    if(pathRecreated){
        logMessage("Error - primary path does not exist: " +
                                                currentJobPrimaryPath, true);
        logMessage("Action - re-creating primary path.");
        pathRecreated = false;
        }

    //if the secondary path could not be verified or repaired, bail out
    if (!validatePath(currentJobBackupPath, "backup")) return;

    //if the call to validatePath for the backup folder caused the backup
    //folder to be recreated, log that error
    if(pathRecreated){
        logMessage("Error - backup path does not exist: " +
                                                currentJobBackupPath, true);
        logMessage("Action - re-creating backup path.");
        pathRecreated = false;
        }
    
    validate("00 - ", " Calibration File.ini", CAL_FILE);
    validate("01 - ", " Configuration.ini", CONFIG_FILE);
    validate("02 - ", " Piece Number File.ini", PIECE_NUMBER_FILE);
    validate("03 - ", " Job Info.ini", JOB_INFO_FILE);
    validate("04 - ", " Configuration - Job Info Window.ini",
                                                        JOB_INFO_CONFIG_FILE);

    }
catch(IOException e){
    }
finally{
    if (logFile != null) logFile.close();
    }
    
}//end of JobValidator::JobValidator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::validatePath
//
// Verifies that pPath exists.
//
// If it does not exist and class member robust is true, then an attempt is
// made to re-create the path.  If robust if false, no repair attempt is made.
//
// If the path is good or was repaired, this method returns true.
// If the path was bad and unrepairable or robust is false, returns false.
//
//

private boolean validatePath(String pPath, String pIdentifier)
{

File folder = new File (pPath);

//all good
if (folder.exists()) return(true); 

//path bad, but don't repair because robust is false
if (!robust) {
    displayErrorMessage("The " + pIdentifier +
                                " folder was missing and was not re-created.");
    return(false);
    }

//all good if folder created, all bad if not
if (folder.mkdirs()){
    displayErrorMessage("The " + pIdentifier +
                                     " folder was missing and was re-created.");
    pathRecreated = true;
    return(true);
    }
else{
    displayErrorMessage("The " + pIdentifier +
                            " folder was missing and could not be re-created.");
    return(false);
    }

}//end of JobValidator::validatePath
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::validate
//
// Verifies that pFilename exists in the primary folder and is not corrupt.
// If missing or corrupt, a copy is made from the backup folder.  If the
// backup file is missing or corrupt, and error message is displayed.
//
// All repair actions are logged in a text file in the primary folder.
//

public void validate(String pPrefix, String pFilename, int pFileType)
{

String filename = currentJobPrimaryPath + pPrefix + jobName + pFilename;
String filenameBackup = currentJobBackupPath + pPrefix + jobName + pFilename;

File file = new File(filename);

if (!file.exists()) handleMissingFile(filenameBackup, filename, pFileType);

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

if (success)
    displayErrorMessage(SuccessMsgs[pFileType]);
else{
    //display the error message for the file type if the message is not blank
    if (!FailureMsgs[pFileType].equals(""))
        displayErrorMessage(FailureMsgs[pFileType]);
    }

//if the job info configuration file could not be restored from the backup
//version, copy the default version to both directories
if (!success && pFileType == JOB_INFO_CONFIG_FILE)
    success =
            handleJobInfoConfigFileBackupRestoreFailure(pSource, pDestination);

//if the job configuration file could not be restored from the backup version,
//allow the user to select the appropriate file from the list of defaults
if (!success && pFileType == CONFIG_FILE)
    success = handleConfigFileBackupRestoreFailure();

return success;

}//end of JobValidator::handleMissingFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// JobValidator::handleConfigFileBackupRestoreFailure
//
// Allows the user to select the proper configuration file to be used for
// the job from a list.
//
// NOTE: The same configuration file should be selected as was first selected
// when the job was created.
//

public boolean handleConfigFileBackupRestoreFailure()

{

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

boolean successP = false, successB = false;

if (!(successP = 
            copyFile("Configuration - Job Info Window.ini", pFilenamePrimary))
    ||
    !(successB = 
            copyFile("Configuration - Job Info Window.ini", pFilenameBackup))){

    displayErrorMessage("The job information configuration file "
        + " was damaged or missing and could not be repaired."
        + " Please contact technical support.");

    }
else
    {
    displayErrorMessage("The job information configuration file"
        + " was damaged or missing and replaced with the default version.");
    }

//if file could not be copied to either directory, return error
if (!successP || !successB) return false;
else return true;

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

if (!(success = copyFile(pSource, pDestination)))
    logMessage("Error - cannot copy from backup folder: " + pSource);

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

public void logMessage(String pMessage, boolean pLogDate)
{

if (pLogDate) logFile.println(new Date().toString());

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

public void logMessage(String pMessage)
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
catch(IOException e){return (false);}
finally {
    try{
        if (in != null) in.close();
        if (out != null) out.close();
        }
    catch(IOException e){return(false);}
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

}//end of class JobValidator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------