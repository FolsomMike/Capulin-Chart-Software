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

}//end of class SwissArmyKnife
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
