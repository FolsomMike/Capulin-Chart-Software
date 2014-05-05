/******************************************************************************
* Title: SwissArmyKnife.java
* Author: Mike Schoonover
* Date: 9/25/13
*
* Purpose:
*
* This class handles various functions required by the typical application.
* It consists of static methods and is not meant to be instantiated.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.tools;

//-----------------------------------------------------------------------------

import java.awt.Image;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

//

public class SwissArmyKnife extends Object{


//-----------------------------------------------------------------------------
// SwissArmyKnife::setIconImages
//
// Sets the images to be used by the Operating System as icons for pWindow.
//
// The images are loaded from the "images/application icons" folder located
// in the root folder of the application's jar file. pRootClass should be
// any class in the jar's root folder, such as class Main and is used to
// locate the image folder.
//
// pIconBaseName is the unique name part of each icon -- the name will be
// completed by adding "16x16-32.png", "32x32-32.png", "48x48-32.png",
// "256x256-32.png", etc... Where YxY is the image size and -zz is the color
// depth.
//
// So if the icons are named "IRScan Chart Icon 16x16-32.png" and so forth,
// pIconBaseName should be set to "IRScan Chart Icon" and this method will
// append the appropriate suffixes to load each size of icon.
//

static public void setIconImages(Window pWindow, Class pRootClass,
                                                        String pIconBaseName)
{

    String path = "images/application icons/";
    String fullPath;
    java.net.URL imgURL;

    List <Image> icons = new ArrayList<>();

    //retrieve each size of icon image

    fullPath = path + pIconBaseName + " 16x16-32.png";
    imgURL = pRootClass.getResource(fullPath);
    if (imgURL != null) { icons.add(new ImageIcon(imgURL).getImage()); }

    fullPath = path + pIconBaseName + " 32x32-32.png";
    imgURL = pRootClass.getResource(fullPath);
    if (imgURL != null) { icons.add(new ImageIcon(imgURL).getImage()); }

    fullPath = path + pIconBaseName + " 48x48-32.png";
    imgURL = pRootClass.getResource(fullPath);
    if (imgURL != null) { icons.add(new ImageIcon(imgURL).getImage()); }

    fullPath = path + pIconBaseName + " 256x256-32.png";
    imgURL = pRootClass.getResource(fullPath);
    if (imgURL != null) { icons.add(new ImageIcon(imgURL).getImage()); }

    //set the window's icon set

    pWindow.setIconImages(icons);

}//end of SwissArmyKnife::setIconImages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SwissArmyKnife::waitSleep
//
// Sleeps for pTime milliseconds.
//

public static void waitSleep(int pTime)
{

        try {Thread.sleep(pTime);} catch (InterruptedException e) { }

}//end of SwissArmyKnife::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SwissArmyKnife::stripFileSeparator
//
// Strips off the file separator from the end of pPath is one is found.
//

public static String stripFileSeparator(String pPath)
{

    if (pPath.endsWith(File.separator)) {
        pPath = pPath.substring(0, pPath.length()-1);
    }

    return(pPath);

}//end of SwissArmyKnife::stripFileSeparator
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SwissArmyKnife::createFolderForSpecifiedFileType
//
// Creates a folder to hold a specific type of output files if it does not
// already exist. This folder is used to segregate certain types of files for
// easier viewing and access via outside programs. This also reduces the need
// to access the folders containing the main data files which reduces the
// possibility of accidental file destruction.
//
// If pSpecialPath contains a specific folder to be used (as specified by
// a config file entry or such), the output files will be stored there.
//
// If pSpecialPath is empty, a new folder will be created in the primary
// path using pJobName with pDescriptor appended. Thus, for pDescriptor of
// "Reports" and pJobName of "WO1234", and pBasePath
// of "IR Scan Data Files -  Primary", a sample of the folder created:
//
// IR Scan Data Files -  Primary
//      WO3944
//      WO1234
//      WO1234 ~ Reports
//
// Returns the full path to the folder.
//

public static String createFolderForSpecifiedFileType(String pSpecialPath,
      String pBasePath, String pJobName, String pDescriptor, JFrame pMainFrame)
{

    //remove the folder separator from the end of the reports path if it exists
    //so we can make a new folder name

    String lReportsPath = "";
    File folder;

    //use the specified output folder if path is not empty
    if(!pSpecialPath.isEmpty()){
        
        lReportsPath = SwissArmyKnife.stripFileSeparator(pSpecialPath);

        //create a folder using the job name to hold the reports
        folder = new File(lReportsPath);
        if (!folder.exists()){
            //attempt to create the folder
            if (!folder.mkdirs()){
                displayErrorMessage(
                        pMainFrame, "The output folder could not be created.");
                return("");
            }
        }
    }//if(!pSpecialPath.isEmpty())
    
    String fullReportsPath;

    //if a specific folder has been specified to hold reports, then place
    //the new folder there -- if not, then place the folder in the primary
    //data folder
    if (!lReportsPath.isEmpty()){
        fullReportsPath = lReportsPath + File.separator + pJobName;
    }
    else{
        fullReportsPath = SwissArmyKnife.stripFileSeparator(pBasePath);
    }

    //create a new folder which will be stored next to the job data folder
    fullReportsPath = fullReportsPath + pDescriptor;

    //create a folder using the job name to hold the reports
    folder = new File(fullReportsPath);
    if (!folder.exists()){
        //attempt to create the folder
        if (!folder.mkdirs()){
            displayErrorMessage(
                    pMainFrame, "The output folder could not be created.");
            return("");
        }
    }

//if all folders created okay, return the full path
return(fullReportsPath + File.separator);

}//end of SwissArmyKnife::createFolderForSpecifiedFileType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SwissArmyKnife::formatPath
//
// Append a fwd/backslash if pPath does not already end with one.
// Use File.separator to apply the correct character for the operating system.
//

public static String formatPath (String pPath){

    pPath = pPath.trim();
    
    if (!pPath.equals("") && !pPath.endsWith(File.separator)) {
        pPath += File.separator;
    }

    return(pPath);

}//end of SwissArmyKnife::formatPath
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SwissArmyKnife::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

public static void displayErrorMessage(JFrame pMainFrame, String pMessage)
{

    JOptionPane.showMessageDialog(pMainFrame, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of SwissArmyKnife::displayErrorMessage
//-----------------------------------------------------------------------------


}//end of class SwissArmyKnife
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
