/******************************************************************************
* Title: HardwareLink.java
* Author: Mike Schoonover
* Date: 3/18/08
*
* Purpose:
*
* This file contains the interface definition for HardwareLink.  This interface
* provides functions required by all hardware drivers.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import javax.swing.*;

import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// interface Link
//
// Defines functions to allow different objects to call functions in other
// objects.
//

public interface HardwareLink {

int getNumberOfChannels();

int getNumberOfGates(int pChannel);

public Trace getTrace(int pChannel, int pGate);

boolean getNewData(int ch, int g, HardwareVars hdwVs);

int getChannelData(int _pChannel, int pSimDataType);

Channel[] getChannels();

Gate getGate(int pChannel, int pGate);

void connect();

void loadCalFile(IniFile pCalFile);

void saveCalFile(IniFile pCalFile);

public void setMode(int pOpMode);

void startMonitor(int dMonitorPacketSize);

void stopMonitor();

void getMonitorPacket(byte[] pMonitorBuffer, boolean pRequestPacket);

void zeroEncoderCounts();

public void requestAScan(int pChannel);

AScan getAScan(int pChannel);

public void requestPeakData(int pChannel);

public void requestPeakDataForAllBoards();

public void linkTraces(int pChartGroup, int pChart, int pTrace, int[] pDBuffer,
   int[] pDBuffer2, int[] pFBuffer, Threshold[] pThresholds, int pPlotStyle,
   Trace pTracePtr);

boolean prepareData();

public void displayMessages();

public void doTasks();

public void readRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
         int pRAMType, int pPage, int pAddress, int pCount, byte[] dataBlock);

public void writeRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
                            int pRAMType, int pPage, int pAddress, int pValue);

public void fillRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pBlockSize, int pValue);

public int getState(int pChassis, int pSlot, int pWhich);

public void setState(int pChassis, int pSlot, int pWhich, int pValue);

public void sendDataChangesToRemotes();

public void driveSimulation();

public boolean getSimulate();

public void logStatus(JTextArea pTextArea);

public void verifyAllDSPCode2();

public void shutDown();

}//end of interface HardwareLink
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
