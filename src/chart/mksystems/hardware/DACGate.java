/******************************************************************************
* Title: DACGate.java
* Author: Mike Schoonover
* Date: 11/19/09
*
* Purpose:
*
* This class handles a DAC gate.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class DACGate
//
// This class handles a DAC gate.
//

public class DACGate extends BasicGate{


public boolean parametersChanged, flagsChanged;

//-----------------------------------------------------------------------------
// DACGate::DACGate (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public DACGate(IniFile pConfigFile, int pChannelIndex, int pGateIndex)
{

configFile = pConfigFile; channelIndex = pChannelIndex; gateIndex = pGateIndex;

//read the configuration file and create/setup the charting/control elements
configure(configFile);

//unlike flaw/wall gates, DAC gates are not set active until the user explicitly
//sets them active

}//end of DACGate::DACGate (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::getFlags
//
// Returns the gate's flags.
//

public int getFlags()
{

return gateFlags;

}//end of DACGate::getFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setActive
//
// Turns the gate active flag on or off.
//
// Does not set the flag in the DSP.
//

public void setActive(boolean pValue)
{

if (pValue)
    gateFlags |= GATE_ACTIVE;
else
    gateFlags &= (~GATE_ACTIVE);

}//end of DACGate::setActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::getActive
//
// Returns the state of the GATE_ACTIVE bit in the gateFlags variable.
//

public boolean getActive()
{

return ((gateFlags & GATE_ACTIVE) != 0) ? true : false;

}//end of DACGate::getActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setSelected
//
// Turns the gate selected flag on or off.
//

public void setSelected(boolean pValue)
{

selectedFlag = pValue;

}//end of DACGate::setSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::getSelected
//
// Returns the state of the selectedFlag.
//

public boolean getSelected()
{

return selectedFlag;

}//end of DACGate::getSelected
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::copyFromGate
//
// Copies the appropriate values from source gate pSourceGate.
// Does not set any data changed flags.
//

public void copyFromGate(DACGate pSourceGate)
{

gateStart = pSourceGate.gateStart;
gateStartTrackingOn = pSourceGate.gateStartTrackingOn;
gateStartTrackingOff = pSourceGate.gateStartTrackingOff;
gatePixStart = pSourceGate.gatePixStart;
gatePixStartAdjusted = pSourceGate.gatePixStartAdjusted;
gatePixEnd = pSourceGate.gatePixEnd;
gatePixEndAdjusted = pSourceGate.gatePixEndAdjusted;
gateWidth = pSourceGate.gateWidth;
gateLevel = pSourceGate.gateLevel;
gatePixLevel = pSourceGate.gatePixLevel;
gateFlags = pSourceGate.gateFlags;

previousGateStart = pSourceGate.previousGateStart;
previousGateWidth = pSourceGate.previousGateWidth;

interfaceCrossingPixAdjusted = pSourceGate.interfaceCrossingPixAdjusted;

}//end of DACGate::copyFromGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setInterfaceTracking
//
// Turns the interface tracking flag on or off.
//
// Does not set the flag in the DSP.
//

public void setInterfaceTracking(boolean pOn)
{

if (pOn)
    gateFlags |= GATE_USES_TRACKING;
else
    gateFlags &= (~GATE_USES_TRACKING);

}//end of DACGate::setInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//
//

private void configure(IniFile pConfigFile)
{

String whichGate = "Channel " + (channelIndex+1) + " DAC Gate " + (gateIndex+1);

title =
      pConfigFile.readString(whichGate, "Title", "DAC Gate " + (gateIndex+1));

shortTitle = pConfigFile.readString(
                            whichGate, "Short Title", "DG " + (gateIndex+1));

}//end of DACGate::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

public void loadCalFile(IniFile pCalFile)
{

String section = "Channel " + (channelIndex+1) + " DAC Gate " + (gateIndex+1);


setActive(pCalFile.readBoolean(section, "Gate is Active", false));
gateStart = pCalFile.readDouble(section, "Gate Start", 50);
gateStartTrackingOn = pCalFile.readDouble(section,
                "Gate Start with Interface Tracking", 50);
gateStartTrackingOff = pCalFile.readDouble(section,
            "Gate Start without Interface Tracking", 50);
gateWidth = pCalFile.readDouble(section, "Gate Width", 2);
gateLevel = pCalFile.readInt(section, "Gate Level", 15);

//set all the data changed flags so data will be sent to remotes
parametersChanged = true; flagsChanged = true;

}//end of DACGate::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

public void saveCalFile(IniFile pCalFile)
{

String section = "Channel " + (channelIndex+1) + " DAC Gate " + (gateIndex+1);

pCalFile.writeBoolean(section, "Gate is Active", getActive());
pCalFile.writeDouble(section, "Gate Start", gateStart);
pCalFile.writeDouble(section,
                "Gate Start with Interface Tracking",  gateStartTrackingOn);
pCalFile.writeDouble(section,
            "Gate Start without Interface Tracking",  gateStartTrackingOff);
pCalFile.writeDouble(section, "Gate Width", gateWidth);
pCalFile.writeInt(section, "Gate Level", gateLevel);

}//end of DACGate::saveCalFile
//-----------------------------------------------------------------------------

}//end of class DACGate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
