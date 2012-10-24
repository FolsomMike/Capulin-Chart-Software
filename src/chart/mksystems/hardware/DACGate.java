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
import chart.mksystems.threadsafe.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class DACGate
//
// This class handles a DAC gate.
//

public class DACGate extends BasicGate{


boolean doInterfaceTracking = false;

int scopeMax;
double softwareGain;
SyncedInteger gainForRemote;

//-----------------------------------------------------------------------------
// DACGate::DACGate (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

public DACGate(IniFile pConfigFile, int pChannelIndex, int pGateIndex,
                                 SyncedVariableSet pSyncedVarMgr, int pScopeMax)
{

super(pSyncedVarMgr);

configFile = pConfigFile; channelIndex = pChannelIndex; gateIndex = pGateIndex;
scopeMax = pScopeMax;

gainForRemote = new SyncedInteger(syncedVarMgr); gainForRemote.init();

//read the configuration file and create/setup the charting/control elements
configure(configFile);

//unlike flaw/wall gates, DAC gates are not set active until the user explicitly
//sets them active

}//end of DACGate::DACGate (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setFlags
//
// Sets the various bits in gateFlags to according to various settings.
//
// It is not a public method -- it is manipulated by calling other functions
// to select the gate type and processing methods.
//
// Does not set the flag in the DSP.
//

void setFlags()
{

    int flags = 0;

    //set the bits common to all gate types

    if (gateActive)
        flags |= GATE_ACTIVE;
    else
        flags &= (~GATE_ACTIVE);

    if (doInterfaceTracking)
        flags |= GATE_USES_TRACKING;
    else
        flags &= (~GATE_USES_TRACKING);

    gateFlags.setValue(flags);

}//end of DACGate::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::getFlags
//
// Returns the gate's flags.
//

public SyncedInteger getFlags()
{

    return gateFlags;

}//end of DACGate::getFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setSoftwareGain
//
// Sets the software gain and calculates the gain for the DAC gate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setSoftwareGain(double pSoftwareGain, boolean pForceUpdate)
{

    softwareGain = pSoftwareGain;

    //recalculate the gain to be sent to the DSP
    calculateDACGain(pForceUpdate);

}//end of DACGate::setSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::calculateGateTimeLocation
//
// Calculates the time locations for a gate from its pixel location and
// various related offsets.
//
// This function performs the opposite of calculateGatePixelLocation.
//
// Also calculates the gain value to send to the remotes.
//

@Override
public void calculateGateTimeLocation(double pUSPerPixel, int pDelayPix,
                                             int pCanvasHeight, int pVertOffset)
{

super.calculateGateTimeLocation(
                        pUSPerPixel, pDelayPix, pCanvasHeight, pVertOffset);

calculateDACGain(false);

}//end of BasicGate::calculateGateTimeLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setLevel
//
// Sets the gate level and calculates the gain for the DAC gate.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void setLevel(int pLevel, boolean pForceUpdate)
{

    gateLevel.setValue(pLevel);

    //recalculate the gain to be sent to the DSP
    calculateDACGain(pForceUpdate);

}//end of DACGate::setLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::calculateDACGain
//
// Calculates the gain for the gate to be sent to the DSP.  Should be called
// when any of the variables used in the calculation are changed.
//
// If pForceUpdate is true, the value(s) will always be sent to the DSP.  If
// the flag is false, the value(s) will be sent only if they have changed.
//

public void calculateDACGain(boolean pForceUpdate)
{

    //the DAC's gain value depends upon the value of softwareGain and
    //and the DAC's level in pixels
    //at 50% scope height, the DAC's gain equals softwareGain
    //it is desirable to have a +/- 20dB adjustment for each DAC
    //gate thus each pixel above or below mid-height will raise or
    //lower gain

    //calculate number of dB per pixel to get +/-20dB total
    double dBPerPix = 20.0 / (scopeMax/2);

    //convert gateLevel from percentage of screen height to pixels
    //use applyValue on gateLevel here because the value's changed status
    //will be transferred to gainForRemote, which is the value actually sent
    //to the remotes, thus the gateLevel value's change is applied here while
    //the gainForRemote's value change will be applied when it is transmitted

    int pixLevel = (int)Math.round(gateLevel.applyValue() * scopeMax / 100);
    //calculate distance from the center
    int fromCenter = pixLevel - (scopeMax/2);
    //calculate gain for the DAC section
    double gain = softwareGain + fromCenter * dBPerPix;

    //convert decibels to linear gain: dB = 20 * log10(gain)
    //see notes in UTBoard.sendSoftwareGain for details
    gain = Math.pow(10, gain/20);

    gain *= 6.476;

    int roundedGain = (int)Math.round(gain);

    if (roundedGain != gainForRemote.getValue()) pForceUpdate = true;

    if (!pForceUpdate) return; //do nothing unless value change or forced

    gainForRemote.setValue(roundedGain);

}//end of DACGate::calculateDACGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::setActive
//
// Turns the gate active flag on or off.
//
// Does not set the flag in the DSP.
//

public void setActive(boolean pOn)
{

gateActive = pOn;

//update the flags to reflect the change
setFlags();

}//end of DACGate::setActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::getActive
//
// Returns the state of the gateActive.
//

public boolean getActive()
{

return gateActive;

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

previousGateStart = pSourceGate.previousGateStart;
previousGateWidth = pSourceGate.previousGateWidth;

interfaceCrossingPixAdjusted = pSourceGate.interfaceCrossingPixAdjusted;

gateActive = pSourceGate.gateActive;
doInterfaceTracking = pSourceGate.doInterfaceTracking;

//update the flags to reflect any changes
setFlags();

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

doInterfaceTracking = pOn;

//update the flags to reflect the change
setFlags();

}//end of DACGate::setInterfaceTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DACGate::isPositionChanged
//
// Returns true if the start, width, or level of the gate have been changed.
//
// Overrides the BasicGate::isPositionChanged to return the gain for the
// channel rather than the level.
//

@Override
public boolean isPositionChanged()
{

if (     gateStart.getDataChangedFlag()
      || gateWidth.getDataChangedFlag()
      || gainForRemote.getDataChangedFlag())

    return(true);
else
    return(false);

}//end of DACGate::isPositionChanged
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
// This and all functions which set the data changed flag(s) should be
// synchronized to avoid thread conficts.  Typically, one thread changes the
// data while another transmits it to the remotes.
//

public synchronized void loadCalFile(IniFile pCalFile)
{

String section = "Channel " + (channelIndex+1) + " DAC Gate " + (gateIndex+1);


setActive(pCalFile.readBoolean(section, "Gate is Active", false));
gateStart.setValue(pCalFile.readDouble(section, "Gate Start", 50));
gateStartTrackingOn = pCalFile.readDouble(section,
                "Gate Start with Interface Tracking", 50);
gateStartTrackingOff = pCalFile.readDouble(section,
            "Gate Start without Interface Tracking", 50);
gateWidth.setValue(pCalFile.readDouble(section, "Gate Width", 2));
setLevel(pCalFile.readInt(section, "Gate Level", 15), true);

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
pCalFile.writeDouble(section, "Gate Start", gateStart.getValue());
pCalFile.writeDouble(section,
                "Gate Start with Interface Tracking",  gateStartTrackingOn);
pCalFile.writeDouble(section,
            "Gate Start without Interface Tracking",  gateStartTrackingOff);
pCalFile.writeDouble(section, "Gate Width", gateWidth.getValue());
pCalFile.writeInt(section, "Gate Level", gateLevel.getValue());

}//end of DACGate::saveCalFile
//-----------------------------------------------------------------------------

}//end of class DACGate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
