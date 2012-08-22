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
JMenuItem loadFromAnotherJobMenuItem;
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
JMenuItem viewSegmentMenuItem, viewIDInfoMenuItem;

JMenu optionsMenu, chartMenu, aScanMenu;
JCheckBoxMenuItem restartNewPieceAtLeftEdgeMenuCheck;
JCheckBoxMenuItem showRedPeakLineInGateCenter;
JCheckBoxMenuItem showRedPeakLineAtPeakLocation;
JCheckBoxMenuItem showPseudoPeakAtPeakLocation;

//JMenuItem languageMenu;
//ButtonGroup languageGroup;
//JRadioButtonMenuItem englishRadioButton, chineseRadioButton, spanishRadioButton;
JMenu helpMenu;
JMenuItem aboutMenuItem, monitorMenuItem, debuggerMenuItem, repairJobMenuItem;
JMenuItem updateUTRabbitCodeMenuItem, updateControlRabbitCodeMenuItem;
JMenuItem setupSystemMenuItem;
JMenuItem renewalMenuItem, logMenuItem, configInfoItem, statusMenuItem;

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
jobInfoMenuItem.addActionListener(globals);
fileMenu.add(jobInfoMenuItem);

//File/Save menu item
saveMenuItem = new JMenuItem("Save");
saveMenuItem.setMnemonic(KeyEvent.VK_S);
saveMenuItem.setToolTipText("Save");
saveMenuItem.addActionListener(globals);
fileMenu.add(saveMenuItem);

//Change Job menu item
saveMenuItem = new JMenuItem("Change Job");
saveMenuItem.setMnemonic(KeyEvent.VK_C);
saveMenuItem.setToolTipText("Change to a different job.");
saveMenuItem.addActionListener(globals);
fileMenu.add(saveMenuItem);

//File/New Job menu item
newJobMenuItem = new JMenuItem("New Job");
newJobMenuItem.setMnemonic(KeyEvent.VK_N);
newJobMenuItem.setToolTipText("New Job");
newJobMenuItem.addActionListener(globals);
fileMenu.add(newJobMenuItem);

//File/Manage Presets menu item
managePresetsMenuItem = new JMenu("Manage Presets");
managePresetsMenuItem.setMnemonic(KeyEvent.VK_M);
managePresetsMenuItem.setToolTipText("Manage Presets");
//managePresetsMenuItem.addActionListener(globals);
fileMenu.add(managePresetsMenuItem);

//File/Manage Presets/Load From Another Job menu item
loadFromAnotherJobMenuItem = new JMenuItem("Copy From Another Job");
loadFromAnotherJobMenuItem.setMnemonic(KeyEvent.VK_C);
loadFromAnotherJobMenuItem.setToolTipText("Copy settings from a different job.");
loadFromAnotherJobMenuItem.addActionListener(globals);
managePresetsMenuItem.add(loadFromAnotherJobMenuItem);

//File/Manage Presets/Save Preset menu item
savePresetMenuItem = new JMenuItem("Save Preset");
savePresetMenuItem.setMnemonic(KeyEvent.VK_S);
savePresetMenuItem.setToolTipText("Save current settings as a preset.");
savePresetMenuItem.addActionListener(globals);
managePresetsMenuItem.add(savePresetMenuItem);

//File/Manage Presets/Load Preset menu item
loadPresetMenuItem = new JMenuItem("Load Preset");
loadPresetMenuItem.setMnemonic(KeyEvent.VK_L);
loadPresetMenuItem.setToolTipText("Load new settings from a preset.");
loadPresetMenuItem.addActionListener(globals);
managePresetsMenuItem.add(loadPresetMenuItem);

//File/Manage Presets/Rename Preset menu item
renamePresetMenuItem = new JMenuItem("Rename Preset");
renamePresetMenuItem.setMnemonic(KeyEvent.VK_R);
renamePresetMenuItem.setToolTipText("Rename the selected preset.");
renamePresetMenuItem.addActionListener(globals);
managePresetsMenuItem.add(renamePresetMenuItem);

//File/Manage Presets/Delete Preset menu item
deletePresetMenuItem = new JMenuItem("Delete Preset");
deletePresetMenuItem.setMnemonic(KeyEvent.VK_D);
deletePresetMenuItem.setToolTipText("Delete a preset.");
deletePresetMenuItem.addActionListener(globals);
managePresetsMenuItem.add(deletePresetMenuItem);

//File/Exit menu item
exitMenuItem = new JMenuItem("Exit");
exitMenuItem.setMnemonic(KeyEvent.VK_X);
exitMenuItem.setToolTipText("Exit");
exitMenuItem.addActionListener(globals);
fileMenu.add(exitMenuItem);

//Print menu
printMenu = new JMenu("Print");
printMenu.setMnemonic(KeyEvent.VK_P);
printMenu.setToolTipText("Print");
//add(printMenu); DISABLED UNTIL FUNCTIONAL

//Print/Current Joint
printCurrentMenuItem = new JMenuItem("Chart for " +
                                globals.pieceDescription + " Being Inspected");
printCurrentMenuItem.setMnemonic(KeyEvent.VK_I);
printCurrentMenuItem.setToolTipText("Print the chart for " +
                    globals.pieceDescriptionLC + " currently being inspected.");
printCurrentMenuItem.addActionListener(globals);
printMenu.add(printCurrentMenuItem);

//Print/Last Joint completed
printLastMenuItem = new JMenuItem("Chart for Last " +
                                     globals.pieceDescription + " Completed");
printLastMenuItem.setMnemonic(KeyEvent.VK_L);
printLastMenuItem.setToolTipText("Print the chart for last " +
                                 globals.pieceDescriptionLC + " completed.");
printLastMenuItem.addActionListener(globals);
printMenu.add(printLastMenuItem);

//Print/Any Joint
printAnyMenuItem = new JMenuItem("Chart(s) for selected " +
                                                globals.pieceDescriptionPlural);
printAnyMenuItem.setMnemonic(KeyEvent.VK_S);
printAnyMenuItem.setToolTipText("Print the chart(s) for Selected " +
                                              globals.pieceDescriptionPluralLC);
printAnyMenuItem.addActionListener(globals);
printMenu.add(printAnyMenuItem);

printMenu.addSeparator();

//Print/Auto Print
autoPrintMenuCheck = new JCheckBoxMenuItem("Auto Print Chart After Each " +
                                    globals.pieceDescription + " Inspected");
autoPrintMenuCheck.setMnemonic(KeyEvent.VK_A);
autoPrintMenuCheck.setToolTipText("Prints chart automatically after each "
        + globals.pieceDescriptionLC + " is inspected.");
autoPrintMenuCheck.addActionListener(globals);
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
reportCurrentMenuItem.addActionListener(globals);
//reportMenu.add(reportCurrentMenuItem);  DISABLED UNTIL FUNCTIONAL
//NOTE:  This option might not make sense and might should be removed.

//Report/Last Joint completed
reportLastMenuItem = new JMenuItem("Flag Report for Last " +
                                     globals.pieceDescription + " Completed");
reportLastMenuItem.setMnemonic(KeyEvent.VK_L);
reportLastMenuItem.setToolTipText("Print the flag report for last " +
                                 globals.pieceDescriptionLC + " completed.");
reportLastMenuItem.addActionListener(globals);
reportMenu.add(reportLastMenuItem);

//Report/Any Joint
reportAnyMenuItem = new JMenuItem("Flag report(s) for Selected " +
                                                globals.pieceDescriptionPlural);
reportAnyMenuItem.setMnemonic(KeyEvent.VK_S);
reportAnyMenuItem.setToolTipText("Print the flag report(s) for selected " +
                                              globals.pieceDescriptionPluralLC);
reportAnyMenuItem.setActionCommand("Print Flag Report");
reportAnyMenuItem.addActionListener(globals);
reportMenu.add(reportAnyMenuItem);

//reportMenu.addSeparator();  DISABLED UNTIL FUNCTIONAL

//Report/Auto Report
autoReportMenuCheck = new JCheckBoxMenuItem("Auto Print Flag Report After Each "
                                    + globals.pieceDescription + " Inspected");
autoReportMenuCheck.setMnemonic(KeyEvent.VK_A);
autoReportMenuCheck.setToolTipText(
    "Prints flag report automatically after each "
                            + globals.pieceDescriptionLC + " is inspected.");
autoReportMenuCheck.addActionListener(globals);
//reportMenu.add(autoReportMenuCheck);  DISABLED UNTIL FUNCTIONAL

//reportMenu.addSeparator();  DISABLED UNTIL FUNCTIONAL

//Report/Tally Report
reportTallyMenuItem = new JMenuItem("Print Tally Report");
reportTallyMenuItem.setMnemonic(KeyEvent.VK_T);
reportTallyMenuItem.setToolTipText(
                            "Print the tally report.");
reportTallyMenuItem.addActionListener(globals);
//reportMenu.add(reportTallyMenuItem);  DISABLED UNTIL FUNCTIONAL


//Report/Final Report
reportFinalMenuItem = new JMenuItem("Prepare/Print Final Inspection Report");
reportFinalMenuItem.setMnemonic(KeyEvent.VK_F);
reportFinalMenuItem.setToolTipText(
                            "Prepare and print the final inspection report.");
reportFinalMenuItem.addActionListener(globals);
//reportMenu.add(reportFinalMenuItem);  DISABLED UNTIL FUNCTIONAL

//View menu
viewMenu = new JMenu("View");
viewMenu.setMnemonic(KeyEvent.VK_V);
viewMenu.setToolTipText("View");
add(viewMenu);

//View/View Saved Segment
viewSegmentMenuItem = new JMenuItem("View Chart of a Completed " +
                                                    globals.pieceDescription);
viewSegmentMenuItem.setMnemonic(KeyEvent.VK_C);
viewSegmentMenuItem.setToolTipText("View chart of a completed " +
                                            globals.pieceDescriptionLC + ".");
viewSegmentMenuItem.addActionListener(globals);
viewMenu.add(viewSegmentMenuItem);

//View/View/Edit Identifier Info
viewIDInfoMenuItem = new JMenuItem("View / Edit Identifier Info");
viewIDInfoMenuItem.setMnemonic(KeyEvent.VK_I);
viewIDInfoMenuItem.setToolTipText("View and edit the identifier info for each "
                                            + globals.pieceDescriptionLC + ".");
viewIDInfoMenuItem.addActionListener(globals);
viewMenu.add(viewIDInfoMenuItem);

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
//englishRadioButton.addActionListener(globals);

//chineseRadioButton = new JRadioButtonMenuItem("Chinese");
//chineseRadioButton.setMnemonic(KeyEvent.VK_C);
//chineseRadioButton.setToolTipText("Chinese");
//if(globals.language.compareTo("Chinese") == 0)
//    chineseRadioButton.setSelected(true);
//languageGroup.add(chineseRadioButton); languageMenu.add(chineseRadioButton);
//chineseRadioButton.addActionListener(globals);

//spanishRadioButton = new JRadioButtonMenuItem("Spanish");
//spanishRadioButton.setMnemonic(KeyEvent.VK_S);
//spanishRadioButton.setToolTipText("Spanish");
//if(globals.language.compareTo("Spanish") == 0)
//    spanishRadioButton.setSelected(true);
//languageGroup.add(spanishRadioButton); languageMenu.add(spanishRadioButton);
//spanishRadioButton.addActionListener(globals);

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
                             "Restart each new piece at left edge of chart.");
restartNewPieceAtLeftEdgeMenuCheck.addActionListener(globals);
chartMenu.add(restartNewPieceAtLeftEdgeMenuCheck);

//Options/A Scan menu
aScanMenu = new JMenu("A Scan");
aScanMenu.setMnemonic(KeyEvent.VK_A);
aScanMenu.setToolTipText("A Scan Display Options");
optionsMenu.add(aScanMenu);

//Options/A Scan/Show Red Peak Line at Gate Center
showRedPeakLineInGateCenter = new JCheckBoxMenuItem(
                            "Show Red Peak Line at Gate Center");
showRedPeakLineInGateCenter.setToolTipText(
                             "Show red peak line at gate center.");
showRedPeakLineInGateCenter.addActionListener(globals);
aScanMenu.add(showRedPeakLineInGateCenter);

//Options/A Scan/Show Red Peak Line at Peak Location
showRedPeakLineAtPeakLocation = new JCheckBoxMenuItem(
                            "Show Red Peak Line at Peak Location");
showRedPeakLineAtPeakLocation.setToolTipText(
                "Show red peak line at the location of the peak in the gate.");
showRedPeakLineAtPeakLocation.addActionListener(globals);
aScanMenu.add(showRedPeakLineAtPeakLocation);

//Options/A Scan/Show Pseudo Peak at Peak Location
showPseudoPeakAtPeakLocation = new JCheckBoxMenuItem(
                            "Show Peak Symbol at Peak Location");
showPseudoPeakAtPeakLocation.setToolTipText(
"Show a symbol representing the peak at the location of the peak in the gate.");
showPseudoPeakAtPeakLocation.addActionListener(globals);
aScanMenu.add(showPseudoPeakAtPeakLocation);

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
logMenuItem.addActionListener(globals);
helpMenu.add(logMenuItem);

configInfoItem = new JMenuItem("Display Configuration Info");
configInfoItem.setMnemonic(KeyEvent.VK_C);
configInfoItem.setToolTipText("Display Configuration Info");
configInfoItem.setActionCommand("Display Configuration Info");
configInfoItem.addActionListener(globals);
helpMenu.add(configInfoItem);

statusMenuItem = new JMenuItem("Status");
statusMenuItem.setMnemonic(KeyEvent.VK_S);
statusMenuItem.setToolTipText("Status");
statusMenuItem.setActionCommand("Status");
statusMenuItem.addActionListener(globals);
helpMenu.add(statusMenuItem);

monitorMenuItem = new JMenuItem("Monitor");
monitorMenuItem.setMnemonic(KeyEvent.VK_M);
monitorMenuItem.setToolTipText("Monitor");
monitorMenuItem.addActionListener(globals);
helpMenu.add(monitorMenuItem);

debuggerMenuItem = new JMenuItem("Debugger");
debuggerMenuItem.setMnemonic(KeyEvent.VK_D);
debuggerMenuItem.setToolTipText("Debugger");
debuggerMenuItem.setActionCommand("Debugger");
debuggerMenuItem.addActionListener(globals);
helpMenu.add(debuggerMenuItem);

//this is actually the "License Renewal" function but is not referred to as
//such to obfuscate its purpose
renewalMenuItem = new JMenuItem("Renew");
renewalMenuItem.setMnemonic(KeyEvent.VK_R);
renewalMenuItem.setToolTipText("Renew");
renewalMenuItem.setActionCommand("Renew License");
renewalMenuItem.addActionListener(globals);
helpMenu.add(renewalMenuItem);

//option to validate and repair job files
repairJobMenuItem = new JMenuItem("Repair Job");
repairJobMenuItem.setMnemonic(KeyEvent.VK_J);
repairJobMenuItem.setToolTipText("Repair Job Files");
repairJobMenuItem.setActionCommand("Repair Job");
repairJobMenuItem.addActionListener(globals);
helpMenu.add(repairJobMenuItem);

//option to install new Rabbit firmware in the UT boards
updateUTRabbitCodeMenuItem = new JMenuItem("Update UT Rabbit Code");
updateUTRabbitCodeMenuItem.setMnemonic(KeyEvent.VK_U);
updateUTRabbitCodeMenuItem.setToolTipText(""
   + "Installs new software in the Rabbit micro-controllers on all UT boards.");
updateUTRabbitCodeMenuItem.setActionCommand("Update UT Rabbit Code");
updateUTRabbitCodeMenuItem.addActionListener(globals);
helpMenu.add(updateUTRabbitCodeMenuItem);

//option to install new Rabbit firmware in the Control board
updateControlRabbitCodeMenuItem = new JMenuItem("Update Control Rabbit Code");
updateControlRabbitCodeMenuItem.setMnemonic(KeyEvent.VK_U);
updateControlRabbitCodeMenuItem.setToolTipText(""
+ "Installs new software in the Rabbit micro-controllers on all Control boards.");
updateControlRabbitCodeMenuItem.setActionCommand("Update Control Rabbit Code");
updateControlRabbitCodeMenuItem.addActionListener(globals);
helpMenu.add(updateControlRabbitCodeMenuItem);

//option to setup the system
setupSystemMenuItem = new JMenuItem("Setup System");
setupSystemMenuItem.setMnemonic(KeyEvent.VK_S);
setupSystemMenuItem.setToolTipText("Setup System");
setupSystemMenuItem.setActionCommand("Setup System");
setupSystemMenuItem.addActionListener(globals);
helpMenu.add(setupSystemMenuItem);

//option to display the "About" window
aboutMenuItem = new JMenuItem("About");
aboutMenuItem.setMnemonic(KeyEvent.VK_A);
aboutMenuItem.setToolTipText("Display the About window.");
aboutMenuItem.setActionCommand("About");
aboutMenuItem.addActionListener(globals);
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
showRedPeakLineInGateCenter.setSelected(
                                        globals.showRedPeakLineInGateCenter);
showRedPeakLineAtPeakLocation.setSelected(
                                        globals.showRedPeakLineAtPeakLocation);
showPseudoPeakAtPeakLocation.setSelected(globals.showPseudoPeakAtPeakLocation);

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

public final void loadLanguage(String pLanguage)
{

language = pLanguage;

IniFile ini = null;

//if the ini file cannot be opened and loaded, exit without action
try {ini = new IniFile("language\\Main Menu - Capulin UT.language",
                                                        globals.jobFileFormat);}
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