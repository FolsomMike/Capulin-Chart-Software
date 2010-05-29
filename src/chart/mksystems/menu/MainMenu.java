/******************************************************************************
* Title: MainMenu.java
* Author: Mike Schoonover
* Date: 11/23/03
*
* Purpose:
*
* This class creates the main menu and submenus for the main form.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.menu;

import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.BufferedImage;

import chart.mksystems.globals.Globals;
import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MainMenu
//
// This class creates the main menu and sub menus for the main form.
//

public class MainMenu extends JMenuBar{

Globals globals;
JMenu fileMenu;
JMenuItem exitMenuItem;
JMenuItem jobInfoMenuItem;
JMenuItem saveMenuItem;
JMenuItem newJobMenuItem;
JMenu managePresetsMenuItem;
JMenuItem savePresetMenuItem;
JMenuItem loadPresetMenuItem;
JMenuItem renamePresetMenuItem;
JMenuItem deletePresetMenuItem;

JMenu printMenu;
JMenuItem printCurrentMenuItem;
JMenuItem printLastMenuItem;
JMenuItem printAnyMenuItem;
JCheckBoxMenuItem autoPrintMenuCheck;

JMenu reportMenu;
JMenuItem reportCurrentMenuItem;
JMenuItem reportLastMenuItem;
JMenuItem reportAnyMenuItem;
JCheckBoxMenuItem autoReportMenuCheck;
JMenuItem reportTallyMenuItem;
JMenuItem reportFinalMenuItem;

JMenu viewMenu;
JMenuItem viewSegmentMenuItem;

JMenu optionsMenu, chartMenu;
JCheckBoxMenuItem restartNewPieceAtLeftEdgeMenuCheck;

//JMenuItem languageMenu;
//ButtonGroup languageGroup;
//JRadioButtonMenuItem englishRadioButton, chineseRadioButton, spanishRadioButton;
JMenu helpMenu;
JMenuItem aboutMenuItem, monitorMenuItem, debuggerMenuItem, repairJobMenuItem;
JMenuItem setupSystemMenuItem;
JMenuItem renewalMenuItem, logMenuItem, statusMenuItem;

public BufferedImage imageBuffer;

//-----------------------------------------------------------------------------
// MainMenu::MainMenu (constructor)
//

String language;

public MainMenu(Globals pGlobals)
{

globals = pGlobals;

//File menu
fileMenu = new JMenu("File");
fileMenu.setMnemonic(KeyEvent.VK_F);
fileMenu.setToolTipText("File");
add(fileMenu);

//File/Job Info menu item
jobInfoMenuItem = new JMenuItem("Job Info");
jobInfoMenuItem.setMnemonic(KeyEvent.VK_J);
jobInfoMenuItem.setToolTipText("Job Info");
jobInfoMenuItem.addActionListener(pGlobals);
fileMenu.add(jobInfoMenuItem);

//File/Save menu item
saveMenuItem = new JMenuItem("Save");
saveMenuItem.setMnemonic(KeyEvent.VK_S);
saveMenuItem.setToolTipText("Save");
saveMenuItem.addActionListener(pGlobals);
fileMenu.add(saveMenuItem);

//Change Job menu item
saveMenuItem = new JMenuItem("Change Job");
saveMenuItem.setMnemonic(KeyEvent.VK_C);
saveMenuItem.setToolTipText("Change to a different job.");
saveMenuItem.addActionListener(pGlobals);
fileMenu.add(saveMenuItem);

//File/New Job menu item
newJobMenuItem = new JMenuItem("New Job");
newJobMenuItem.setMnemonic(KeyEvent.VK_N);
newJobMenuItem.setToolTipText("New Job");
newJobMenuItem.addActionListener(pGlobals);
fileMenu.add(newJobMenuItem);

//File/Manage Presets menu item
managePresetsMenuItem = new JMenu("Manage Presets");
managePresetsMenuItem.setMnemonic(KeyEvent.VK_M);
managePresetsMenuItem.setToolTipText("Manage Presets");
//managePresetsMenuItem.addActionListener(pGlobals);
fileMenu.add(managePresetsMenuItem);

//File/Manage Presets/Save Preset menu item
savePresetMenuItem = new JMenuItem("Save Preset");
savePresetMenuItem.setMnemonic(KeyEvent.VK_S);
savePresetMenuItem.setToolTipText("Save current settings as a preset.");
savePresetMenuItem.addActionListener(pGlobals);
managePresetsMenuItem.add(savePresetMenuItem);

//File/Manage Presets/Load Preset menu item
loadPresetMenuItem = new JMenuItem("Load Preset");
loadPresetMenuItem.setMnemonic(KeyEvent.VK_L);
loadPresetMenuItem.setToolTipText("Load new settings from a preset.");
loadPresetMenuItem.addActionListener(pGlobals);
managePresetsMenuItem.add(loadPresetMenuItem);

//File/Manage Presets/Rename Preset menu item
renamePresetMenuItem = new JMenuItem("Rename Preset");
renamePresetMenuItem.setMnemonic(KeyEvent.VK_R);
renamePresetMenuItem.setToolTipText("Rename the selected preset.");
renamePresetMenuItem.addActionListener(pGlobals);
managePresetsMenuItem.add(renamePresetMenuItem);

//File/Manage Presets/Delete Preset menu item
deletePresetMenuItem = new JMenuItem("Delete Preset");
deletePresetMenuItem.setMnemonic(KeyEvent.VK_D);
deletePresetMenuItem.setToolTipText("Delete a preset.");
deletePresetMenuItem.addActionListener(pGlobals);
managePresetsMenuItem.add(deletePresetMenuItem);

//File/Exit menu item
exitMenuItem = new JMenuItem("Exit");
exitMenuItem.setMnemonic(KeyEvent.VK_X);
exitMenuItem.setToolTipText("Exit");
exitMenuItem.addActionListener(pGlobals);
fileMenu.add(exitMenuItem);

//Print menu
printMenu = new JMenu("Print");
printMenu.setMnemonic(KeyEvent.VK_P);
printMenu.setToolTipText("Print");
add(printMenu);

//Print/Current Joint
printCurrentMenuItem = new JMenuItem("Chart for " +
                                globals.pieceDescription + " Being Inspected");
printCurrentMenuItem.setMnemonic(KeyEvent.VK_I);
printCurrentMenuItem.setToolTipText("Print the chart for " +
                    globals.pieceDescriptionLC + " currently being inspected.");
printCurrentMenuItem.addActionListener(pGlobals);
printMenu.add(printCurrentMenuItem);

//Print/Last Joint completed
printLastMenuItem = new JMenuItem("Chart for Last " +
                                     globals.pieceDescription + " Completed");
printLastMenuItem.setMnemonic(KeyEvent.VK_L);
printLastMenuItem.setToolTipText("Print the chart for last " +
                                 globals.pieceDescriptionLC + " completed.");
printLastMenuItem.addActionListener(pGlobals);
printMenu.add(printLastMenuItem);

//Print/Any Joint
printAnyMenuItem = new JMenuItem("Chart(s) for selected " +
                                                globals.pieceDescriptionPlural);
printAnyMenuItem.setMnemonic(KeyEvent.VK_S);
printAnyMenuItem.setToolTipText("Print the chart(s) for Selected " +
                                               globals.pieceDescriptionPluralLC);
printAnyMenuItem.addActionListener(pGlobals);
printMenu.add(printAnyMenuItem);

printMenu.addSeparator();

//Print/Auto Print
autoPrintMenuCheck = new JCheckBoxMenuItem("Auto Print Chart After Each " +
                                    globals.pieceDescription + " Inspected");
autoPrintMenuCheck.setMnemonic(KeyEvent.VK_A);
autoPrintMenuCheck.setToolTipText("Prints chart automatically after each "
        + globals.pieceDescriptionLC + " is inspected.");
autoPrintMenuCheck.addActionListener(pGlobals);
printMenu.add(autoPrintMenuCheck);


//Report menu
reportMenu = new JMenu("Report");
reportMenu.setMnemonic(KeyEvent.VK_R);
reportMenu.setToolTipText("Report");
add(reportMenu);

//Report/Current Joint
reportCurrentMenuItem = new JMenuItem("Flag Report for " +
                                globals.pieceDescription + " Being Inspected");
reportCurrentMenuItem.setMnemonic(KeyEvent.VK_I);
reportCurrentMenuItem.setToolTipText("Print the flag report for " +
                    globals.pieceDescriptionLC + " currently being inspected.");
reportCurrentMenuItem.addActionListener(pGlobals);
reportMenu.add(reportCurrentMenuItem);

//Report/Last Joint completed
reportLastMenuItem = new JMenuItem("Flag Report for Last " +
                                     globals.pieceDescription + " Completed");
reportLastMenuItem.setMnemonic(KeyEvent.VK_L);
reportLastMenuItem.setToolTipText("Print the flag report for last " +
                                 globals.pieceDescriptionLC + " completed.");
reportLastMenuItem.addActionListener(pGlobals);
reportMenu.add(reportLastMenuItem);

//Report/Any Joint
reportAnyMenuItem = new JMenuItem("Flag report(s) for Selected " +
                                                globals.pieceDescriptionPlural);
reportAnyMenuItem.setMnemonic(KeyEvent.VK_S);
reportAnyMenuItem.setToolTipText("Print the flag report(s) for selected " +
                                               globals.pieceDescriptionPluralLC);
reportAnyMenuItem.addActionListener(pGlobals);
reportMenu.add(reportAnyMenuItem);

reportMenu.addSeparator();

//Report/Auto Report
autoReportMenuCheck = new JCheckBoxMenuItem("Auto Print Flag Report After Each "
                                    + globals.pieceDescription + " Inspected");
autoReportMenuCheck.setMnemonic(KeyEvent.VK_A);
autoReportMenuCheck.setToolTipText(
    "Prints flag report automatically after each "
                            + globals.pieceDescriptionLC + " is inspected.");
autoReportMenuCheck.addActionListener(pGlobals);
reportMenu.add(autoReportMenuCheck);

reportMenu.addSeparator();

//Report/Tally Report
reportTallyMenuItem = new JMenuItem("Print Tally Report");
reportTallyMenuItem.setMnemonic(KeyEvent.VK_T);
reportTallyMenuItem.setToolTipText(
                            "Print the tally report.");
reportTallyMenuItem.addActionListener(pGlobals);
reportMenu.add(reportTallyMenuItem);


//Report/Final Report
reportFinalMenuItem = new JMenuItem("Prepare/Print Final Inspection Report");
reportFinalMenuItem.setMnemonic(KeyEvent.VK_F);
reportFinalMenuItem.setToolTipText(
                            "Prepare and print the final inspection report.");
reportFinalMenuItem.addActionListener(pGlobals);
reportMenu.add(reportFinalMenuItem);

//View menu
viewMenu = new JMenu("View");
viewMenu.setMnemonic(KeyEvent.VK_V);
viewMenu.setToolTipText("View");
add(viewMenu);

//View/View Saved segment
viewSegmentMenuItem = new JMenuItem("View Chart of a Completed " +
                                                    globals.pieceDescription);
viewSegmentMenuItem.setMnemonic(KeyEvent.VK_C);
viewSegmentMenuItem.setToolTipText("View chart of a completed " +
                                            globals.pieceDescriptionLC + ".");
viewSegmentMenuItem.addActionListener(pGlobals);
viewMenu.add(viewSegmentMenuItem);

//Options\Language submenu and items
//languageMenu = new JMenu("Language");
//languageMenu.setMnemonic(KeyEvent.VK_L);
//languageMenu.setToolTipText("Language");
//optionsMenu.add(languageMenu);

//a group of radio button menu items
//languageGroup = new ButtonGroup();

//englishRadioButton = new JRadioButtonMenuItem("English");
//englishRadioButton.setMnemonic(KeyEvent.VK_E);
//englishRadioButton.setToolTipText("English");
//if(globals.language.compareTo("English") == 0)
//    englishRadioButton.setSelected(true);
//languageGroup.add(englishRadioButton); languageMenu.add(englishRadioButton);
//englishRadioButton.addActionListener(pGlobals);

//chineseRadioButton = new JRadioButtonMenuItem("Chinese");
//chineseRadioButton.setMnemonic(KeyEvent.VK_C);
//chineseRadioButton.setToolTipText("Chinese");
//if(globals.language.compareTo("Chinese") == 0)
//    chineseRadioButton.setSelected(true);
//languageGroup.add(chineseRadioButton); languageMenu.add(chineseRadioButton);
//chineseRadioButton.addActionListener(pGlobals);

//spanishRadioButton = new JRadioButtonMenuItem("Spanish");
//spanishRadioButton.setMnemonic(KeyEvent.VK_S);
//spanishRadioButton.setToolTipText("Spanish");
//if(globals.language.compareTo("Spanish") == 0)
//    spanishRadioButton.setSelected(true);
//languageGroup.add(spanishRadioButton); languageMenu.add(spanishRadioButton);
//spanishRadioButton.addActionListener(pGlobals);

//Options menu
optionsMenu = new JMenu("Options");
optionsMenu.setMnemonic(KeyEvent.VK_O);
optionsMenu.setToolTipText("Options");
add(optionsMenu);

//Options/Chart menu
chartMenu = new JMenu("Chart");
chartMenu.setMnemonic(KeyEvent.VK_C);
chartMenu.setToolTipText("Chart Options");
optionsMenu.add(chartMenu);


//Options/Chart/Restart Each New Piece at Left Edge of Chart
restartNewPieceAtLeftEdgeMenuCheck = new JCheckBoxMenuItem(
                            "Restart Each New Piece at Left Edge of Chart");
restartNewPieceAtLeftEdgeMenuCheck.setToolTipText(
                             "Restart Each New Piece at Left Edge of Chart.");
restartNewPieceAtLeftEdgeMenuCheck.addActionListener(pGlobals);
chartMenu.add(restartNewPieceAtLeftEdgeMenuCheck);

//Help menu
helpMenu = new JMenu("Help");
helpMenu.setMnemonic(KeyEvent.VK_H);
helpMenu.setToolTipText("Help");
add(helpMenu);

//Help menu items and submenus

//View menu items and submenus
logMenuItem = new JMenuItem("Log");
logMenuItem.setMnemonic(KeyEvent.VK_L);
logMenuItem.setToolTipText("Log");
logMenuItem.setActionCommand("Log");
logMenuItem.addActionListener(pGlobals);
helpMenu.add(logMenuItem);

statusMenuItem = new JMenuItem("Status");
statusMenuItem.setMnemonic(KeyEvent.VK_S);
statusMenuItem.setToolTipText("Status");
statusMenuItem.setActionCommand("Status");
statusMenuItem.addActionListener(pGlobals);
helpMenu.add(statusMenuItem);

monitorMenuItem = new JMenuItem("Monitor");
monitorMenuItem.setMnemonic(KeyEvent.VK_M);
monitorMenuItem.setToolTipText("Monitor");
monitorMenuItem.addActionListener(pGlobals);
helpMenu.add(monitorMenuItem);

debuggerMenuItem = new JMenuItem("Debugger");
debuggerMenuItem.setMnemonic(KeyEvent.VK_D);
debuggerMenuItem.setToolTipText("Debugger");
debuggerMenuItem.setActionCommand("Debugger");
debuggerMenuItem.addActionListener(pGlobals);
helpMenu.add(debuggerMenuItem);

//this is actually the "License Renewal" function but is not referred to as
//such to obfuscate its purpose
renewalMenuItem = new JMenuItem("Renew");
renewalMenuItem.setMnemonic(KeyEvent.VK_R);
renewalMenuItem.setToolTipText("Renew");
renewalMenuItem.setActionCommand("Renew License");
renewalMenuItem.addActionListener(pGlobals);
helpMenu.add(renewalMenuItem);

//option to validate and repair job files
repairJobMenuItem = new JMenuItem("Repair Job");
repairJobMenuItem.setMnemonic(KeyEvent.VK_J);
repairJobMenuItem.setToolTipText("Repair Job Files");
repairJobMenuItem.setActionCommand("Repair Job");
repairJobMenuItem.addActionListener(pGlobals);
helpMenu.add(repairJobMenuItem);

//option to setup the system
setupSystemMenuItem = new JMenuItem("Setup System");
setupSystemMenuItem.setMnemonic(KeyEvent.VK_S);
setupSystemMenuItem.setToolTipText("Setup System");
setupSystemMenuItem.setActionCommand("Setup System");
setupSystemMenuItem.addActionListener(pGlobals);
helpMenu.add(setupSystemMenuItem);

//option to display the "About" window
aboutMenuItem = new JMenuItem("About");
aboutMenuItem.setMnemonic(KeyEvent.VK_A);
aboutMenuItem.setToolTipText("Display the About window.");
aboutMenuItem.setActionCommand("About");
aboutMenuItem.addActionListener(pGlobals);
helpMenu.add(aboutMenuItem);

//load the appropriate language text
loadLanguage(globals.language);

}//end of MainMenu::MainMenu (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainMenu::refreshMenuSettings
//
// Sets menu items such as checkboxes and radio buttons to match their
// associated variable values.  This function can be called after the variables
// have been loaded to force the menu items to match.
//

public void refreshMenuSettings()
{

restartNewPieceAtLeftEdgeMenuCheck.setSelected(
                                            globals.restartNewPieceAtLeftEdge);

}//end of MainMenu::refreshMenuSettings
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MainMenu::isSelected
//
// Returns true is any of the top level menu items are selected.
//
// NOTE: this is a workaround for JMenuBar.isSelected which once true never
// seems to go back false when the menu is no longer selected.
//

@Override
public boolean isSelected()
{

//return true if any top level menu item is selected    
    
if (fileMenu.isSelected() || printMenu.isSelected()
    || helpMenu.isSelected())
    return(true);
    
return false;
    
}//end of MainMenu::isSelected
//-----------------------------------------------------------------------------
        
//-----------------------------------------------------------------------------
// MainMenu::loadLanguage
//
// Sets the text displayed by various controls according to the selected
// language.
//
// Sets class variable "language" to "pLanguage" so outside classes can call
// this function to both set the variable and load new translations.
//

public void loadLanguage(String pLanguage)
{

language = pLanguage;

IniFile ini = null;

//if the ini file cannot be opened and loaded, exit without action
try {ini = new IniFile("language\\Main Menu - Capulin UT.language");}
catch(IOException e){return;}


//set the titles for the tab panels

//fileMenu.setText(ini.readString("File", language, "File"));
//exitMenuItem.setText(ini.readString("Exit", language, "Exit"));
//optionsMenu.setText(ini.readString("Options", language, "Options"));
//languageMenu.setText(ini.readString("Language", language, "Language"));
//englishRadioButton.setText(ini.readString("English", language, "English"));
//chineseRadioButton.setText(ini.readString("Chinese", language, "Chinese"));
//spanishRadioButton.setText(ini.readString("Spanish", language, "Spanish"));
helpMenu.setText(ini.readString("Help", language, "Help"));
aboutMenuItem.setText(ini.readString("About", language, "About"));

}// MainMenu::loadLanguage
//-----------------------------------------------------------------------------

}//end of class MainMenu
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------