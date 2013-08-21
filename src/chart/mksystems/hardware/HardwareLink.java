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

import chart.Log;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Map2D;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import java.io.BufferedWriter;
import java.io.IOException;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// interface HardwareLink
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

UTGate getGate(int pChannel, int pGate);

void connect();

void loadCalFile(IniFile pCalFile);

void saveCalFile(IniFile pCalFile);

public void saveCalFileHumanReadable(BufferedWriter pOut) throws IOException;

public void setMode(int pOpMode);

void startMonitor();

void stopMonitor();

byte[] getMonitorPacket(boolean pRequestPacket);

void zeroEncoderCounts();

void pulseOutput1();

void turnOnOutput1();

void turnOffOutput1();

public void requestAScan(int pChannel);

AScan getAScan(int pChannel);

public void requestPeakData(int pChannel);

public void requestPeakDataForAllBoards();

public void linkPlotters(int pChartGroup, int pChart, int pTrace,
        TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
        Trace pTracePtr);

boolean prepareAnalogData();

boolean prepareControlData();

public void displayMessages();

public void updateRabbitCode(int pWhichRabbits);

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

public void logStatus(Log pLogWindow);

public void verifyAllDSPCode2();

public void shutDown();

public void getInspectControlVars(InspectControlVars pICVars);

public boolean getOnPipeFlag();

public boolean getInspectFlag();

public boolean getNewInspectPacketReady();

public void setNewInspectPacketReady(boolean pValue);

public int xmtMessage(int pMessage, int pValue);

public int getRepRateInHertz();

public void setChartGroups(ChartGroup pChartGroups []);

}//end of interface HardwareLink
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
