/******************************************************************************
* Title: Help.java
* Author: Mike Schoonover
* Date: 8/5/12
*
* Purpose:
*
* This class displays a window for displaying help information specified
* by pFilename.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.awt.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Help
//
// This class displays a text area in a window.
//

class Help extends JDialog{

    public JTextArea textArea;

    String filename;

//-----------------------------------------------------------------------------
// Help::Help (constructor)
//

public Help(JFrame frame, String pFilename)
{

    super(frame, "Help");

    filename = pFilename;

}//end of Help::Help (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Help::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

   //release the window's resources when it is closed
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    int panelWidth = 400;
    int panelHeight = 500;

    setMinimumSize(new Dimension(panelWidth, panelHeight));
    setPreferredSize(new Dimension(panelWidth, panelHeight));
    setMaximumSize(new Dimension(panelWidth, panelHeight));

    textArea = new JTextArea();

    JScrollPane areaScrollPane = new JScrollPane(textArea);

    add(areaScrollPane);

    loadAndDisplayInfo();

    setVisible(true);

}//end of Help::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Help::loadAndDisplayInfo
//
// Loads the help info from file "Temperature Monitor Help.txt" and displays it
// in the window.
//

private void loadAndDisplayInfo()
{

    String filepath = "Help Files" + File.separator + filename;

    String line;

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader in = null;

    textArea.append("\n"); //blank line at the top

    try{

        fileInputStream = new FileInputStream(filepath);
        inputStreamReader = new InputStreamReader(fileInputStream);
        in = new BufferedReader(inputStreamReader);

            //read each line and display it
            while ((line = in.readLine()) != null){
                textArea.append(line);
                textArea.append("\n");
            }

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 103");
        displayErrorMessage("Could not open Help file: " + filepath);
    }
    finally{
        try{if (in != null){in.close();}}
        catch(IOException e){}
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){}
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){}
    }

}//end of Help::loadAndDisplayInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Help::displayErrorMessage
//
// Displays an error dialog with message pMessage.
//

private void displayErrorMessage(String pMessage)
{

    JOptionPane.showMessageDialog(null, pMessage,
                                            "Error", JOptionPane.ERROR_MESSAGE);

}//end of Help::displayErrorMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Help::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Help::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Help::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of Help::logStackTrace
//-----------------------------------------------------------------------------

}//end of class Help
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
