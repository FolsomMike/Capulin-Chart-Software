/******************************************************************************
* Title: MultipleInstancePreventer.java
* Author: Mike Schoonover
* Date: 9/23/13
*
* Purpose:
*
* This class has a static function which checks to see if an instance of the
* application is already running.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.tools;

//-----------------------------------------------------------------------------

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import javax.swing.JOptionPane;

//

public class MultipleInstancePreventer extends Object{

    private static File lockFile;
    private static FileChannel lockChannel;
    private static FileLock fileLock;



//-----------------------------------------------------------------------------
// MultipleInstancePreventer::MultipleInstancePreventer (constructor)
//

public MultipleInstancePreventer()
{


}//end of MultipleInstancePreventer::MultipleInstancePreventer (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MultipleInstancePreventer::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of MultipleInstancePreventer::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MultipleInstancePreventer::checkForInstanceAlreadyRunning
//
// This method creates a file with name pProgramName and attempts to lock it. If
// a file with that name has already been locked, then another instance of
// the application is already running and an warning message is displayed and
// the program exits.
//
// The user is given the option to start the new instance anyway for cases
// where a file lock gets stuck in place do to a program crash. This avoids
// having to reboot the computer immediately.
//
// NOTE: Some websites suggested trying to bind a ServerSocket. As only one
// can be bound on the system, only one app can bind the socket -- those that
// fail can assume that another instance is running and exit. This is
// problematic in that no two programs -- even if different programs -- can
// run at the same time as the ServerSocket doesn't differentiate as to the
// type of program binding it. Thus, if you wanted only a single Java charting
// program and a single Java editing program and a single Java monitor program
// to be allowed to run together, it would not be allowed as each would try to
// bind the ServerSocket and only one would succeed.
//
// Because of this, the locking file idea was used instead as each program
// looks for the file in its own root folder and uses a different filename. Thus
// there is no limit to the different programs run -- just a limit on the number
// of each running.
//

static public void checkForInstanceAlreadyRunning(String pProgramName)
{

    try {

        lockFile = new File(pProgramName + ".lock");
        if (lockFile.exists()) { lockFile.delete(); }
        FileOutputStream lockFileOS = new FileOutputStream(lockFile);
        lockFileOS.close();

        lockChannel = new RandomAccessFile(lockFile,"rw").getChannel();
        fileLock = lockChannel.tryLock();
        if (fileLock == null) { throw new Exception("Unable to obtain lock"); }
    }
    catch (Exception e) {

        int response = JOptionPane.showConfirmDialog(null,
        "An instance of " + pProgramName + " is already running. "
        + "Look for it in the taskbar at the bottom of the screen. "
        + "If you are sure no other instance is running, click 'Yes' to "
        + "start the program.",
        pProgramName + " is already running...",
        JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.NO_OPTION){
            System.exit(0);
        }

    }

}//end of MultipleInstancePreventer::checkForInstanceAlreadyRunning
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MultipleInstancePreventer::removeLock
//
// Removes the lock from the lock file. Call this upon exiting the program to
// make sure the lock is removed.
//

static public void removeLock()
{

    try {
          fileLock.release();
          lockChannel.close();
          lockFile.delete();
        } catch (IOException e) {
          System.out.println(
                  "Error while closing the lock socket: " + e.getMessage());
        }

}//end of MultipleInstancePreventer::removeLock
//-----------------------------------------------------------------------------


}//end of class MultipleInstancePreventer
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
