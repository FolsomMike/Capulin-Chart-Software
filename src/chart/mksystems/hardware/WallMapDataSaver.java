/******************************************************************************
* Title: WallMapDataSaver.java
* Author: Mike Schoonover
* Date: 8/22/13
*
* Purpose:
*
* This class saves wall data collected by a UTBoard in a format compatible
* with Tuboscope's wall  map viewer.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import MKSTools.CharBuf;
import MKSTools.DWORD;
import MKSTools.LittleEndianTool;
import MKSTools.WORD;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;


//-----------------------------------------------------------------------------
// class WallMapDataSaver
//

public class WallMapDataSaver extends Object{

    final static int NUMBER_OF_SOURCE_BOARDS = 4;

    UTBoard utBoards[];
    short dataBuffers[][];

    DataOutputStream outFile;

    static final int NUM_WALL_CHANNEL = 4;
    static final int NUM_MAX_ASCAN = 2000;   // maximum number of Ascans per revolution
    static final int NUM_MAX_REVOL = 2000;   // maximum number of revolutions per joint


    //--- variables contained in the Tubo struct JOB_REC ---

    CharBuf cfgFile;        // configuration file name for the current calibration
    CharBuf WO;             // work order number
    CharBuf grade;          // grade of the pipe
    CharBuf lotNum;         // Lot number
    CharBuf heat;           // Heat number
    CharBuf customer;       //customer name
    CharBuf operator;       // operator name
    CharBuf busUnit;        // which Tubo business unit
    CharBuf comment;
    CharBuf version;        // software version number
    WORD verNum;            // version num * 100
    float nominalWall;      // nominal wall in inches
    float OD;               // nominal OD in inches
    CharBuf location;
    CharBuf wellName;
    CharBuf date;           // date of inspection
    CharBuf range;
    CharBuf wellFoot;       // not used
    byte wallStatFlag;      // not used
    CharBuf rbNum;          // not used
    CharBuf spare;
    float fMotionPulseLen;  // inches per motion pulse, needs to be 0.5
    float fChnlOffset[];    // channel offsets in inches
    short nHomeXOffset;     // linear location offset when the heads run from the home end (end stop
                            // end) to the far end, in motion pulses
    short nAwayXOffset;     // linear location offset when the heads run from the far end to the home
                            // end, in motion pulses
    int   nStopXloc;        // The linear location where the wall head is when the inspection ends
                            //    while running from the far end to the home end, in motion pulses.

    //--- end of variables contained in the Tubo struct JOB_REC ---

    //--- variables contained in the Tubo struct RAW_WALL_HEAD ---


    // JOB_REC JobRec;      // can't actually use a struct in Java
    DWORD nJointNum;        // joint number
    float fWall;            // nominal pipe wall thickness in inches
    float fOD;              // nominal pipe OD in inches
    WORD nNumRev;           // actual number of revolutions stored in this file
                            // no greater than NUM_MAX_REVOL
    WORD nMotionBus;        // Bit 14 (zero-based) is the home or away bit.
                            // 1: Home, the heads run from the home end to the far end.
                            // 0: the heads run from the far end to the home end.

    //--- end of variables contained in the Tubo struct RAW_WALL_HEAD ---

    //--- variables contained in the Tubo struct WALL_ASCAN ---

    // wall readings from all channels for a single simultaneous pulse

    WORD wall[]; //wall readings for all channels, in thousandth of an inch

    //--- end of variables contained in the Tubo struct WALL_ASCAN ---


    //--- variables contained in the Tubo struct WALL_ASCAN ---

    // wall readings from all channels for a single revolution

    WORD nNumAscan[];           // actual number of readings for this (this is a WORD -- unsigned short -- save only lower two bytes)
                                // Revolution for each channel; <= NUM_MAX_ASCAN
    short       nXloc;          // linear location for this revolution, in motion pulses
    WORD   nMotionBusNotUsed;   // not used
    float   fCrossArea[];       // not used

    // WALL_ASCAN    WallAscan[NUM_MAX_ASCAN];	// wall readings for this revolution all wall channels
                                                // (can't use structs in Java

    //--- end of variables contained in the Tubo struct WALL_REVOLUTION ---

//-----------------------------------------------------------------------------
// WallMapDataSaver::WallMapDataSaver (constructor)
//

public WallMapDataSaver()
{

}//end of WallMapDataSaver::WallMapDataSaver (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init(UTBoard pBoard0, UTBoard pBoard1, UTBoard pBoard2,
                                                            UTBoard pBoard3)
{

    utBoards = new UTBoard[NUMBER_OF_SOURCE_BOARDS];
    utBoards[0] = pBoard0; utBoards[1] = pBoard1;
    utBoards[2] = pBoard2; utBoards[3] = pBoard3;

    dataBuffers = new short[NUMBER_OF_SOURCE_BOARDS][];

    for(int i = 0; i < NUMBER_OF_SOURCE_BOARDS; i++){
        dataBuffers[i] = utBoards[i].getDataBuffer();
    }


    cfgFile = new CharBuf("", 128);
    WO = new CharBuf("", 10);
    grade = new CharBuf("", 10);
    lotNum = new CharBuf("", 10);
    heat = new CharBuf("", 34);
    customer = new CharBuf("", 32);
    operator = new CharBuf("", 10);
    busUnit = new CharBuf("", 32);
    comment = new CharBuf("", 80);
    version = new CharBuf("", 16);
    verNum = new WORD(0);
    location = new CharBuf("", 40);
    wellName = new CharBuf("", 40);
    date = new CharBuf("", 10);
    range = new CharBuf("", 8);
    wellFoot = new CharBuf("", 12);
    rbNum = new CharBuf("", 10);
    spare = new CharBuf("", 75);
    fChnlOffset = new float[4];

    nNumRev = new WORD(0);

    wall = new WORD[NUM_WALL_CHANNEL];
    for(int i = 0; i < NUM_WALL_CHANNEL; i++){wall[i] = new WORD(0);}

    nNumAscan = new WORD[NUM_WALL_CHANNEL];
    for(int i = 0; i < NUM_WALL_CHANNEL; i++){nNumAscan[i] = new WORD(0);}
    fCrossArea = new float[NUM_WALL_CHANNEL];


    nMotionBus = new WORD(0);
    nMotionBusNotUsed = new WORD(0);
    nJointNum = new DWORD(0);

    setUpTestData(); //debug mks -- remove this
    saveToFile("test1.data"); //debug mks -- remove this

}//end of WallMapDataSaver::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::saveToFile
//
// Writes all header data and the data in the UTBoards to file pFilename.
//

public void saveToFile(String pFilename)
{

    try{
        outFile =
              new DataOutputStream(new BufferedOutputStream(
              new FileOutputStream(pFilename)));

        saveHeader();

        saveRevolution(291, 291, 291, 291);

    }
    catch(IOException e){

    }
    finally{
        try{if (outFile != null) {outFile.close();}}
        catch(IOException e){}
    }

}//end of WallMapDataSaver::saveToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::setUpTestData
//
// Set up all variables with test data.
//

public void setUpTestData()
{

    //set up test data

    cfgFile.set("Config file - this is a test.", 128);
    WO.set("1234567890", 10);
    grade.set("Grade", 10);
    lotNum.set("Lot Number Entry", 10);
    heat.set("Heat", 34);
    customer.set("Customer", 32);
    operator.set("Operator", 10);
    busUnit.set("Business Unit", 32);
    comment.set("Comment", 80);
    version.set("Version", 16);
    verNum.value = 23;
    nominalWall = (float)0.534;
    OD = (float)11.75;
    location.set("Location", 40);
    wellName.set("Well Name", 40);
    date.set("Date", 10);
    range.set("Range", 8);
    wellFoot.set("Well Foot", 12);
    wallStatFlag = 0x00;
    rbNum.set("RB Number", 10);
    spare.set("", 75);
    fMotionPulseLen = (float)0.375;

    fChnlOffset[0] = (float)0;
    fChnlOffset[1] = (float)0.625;
    fChnlOffset[2] = (float)1.25;
    fChnlOffset[3] = (float)1.875;

    nHomeXOffset = 0x55aa;
    nAwayXOffset = 0x66bb;

    nStopXloc = 0x23456789;

    nJointNum.value = 0x12345678;
    fWall = (float)0.534;
    fOD = (float)11.75;
    nNumRev.value = 0x789;
    nMotionBus.value = 0x456;
    nMotionBusNotUsed.value = 0x02;

}//end of WallMapDataSaver::setUpTestData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::saveHeader
//
// Saves the header to the file.
//

public void saveHeader()throws IOException
{

    cfgFile.write(outFile);
    WO.write(outFile);
    grade.write(outFile);
    lotNum.write(outFile);
    heat.write(outFile);
    customer.write(outFile);
    operator.write(outFile);
    busUnit.write(outFile);
    comment.write(outFile);
    version.write(outFile);
    verNum.write(outFile);
    LittleEndianTool.writeFloat(nominalWall, outFile);
    LittleEndianTool.writeFloat(OD, outFile);
    location.write(outFile);
    wellName.write(outFile);
    date.write(outFile);
    range.write(outFile);
    wellFoot.write(outFile);
    outFile.writeByte(wallStatFlag);
    rbNum.write(outFile);
    spare.write(outFile);
    LittleEndianTool.writeFloat(fMotionPulseLen, outFile);
    LittleEndianTool.writeFloat(fChnlOffset[0], outFile);
    LittleEndianTool.writeFloat(fChnlOffset[1], outFile);
    LittleEndianTool.writeFloat(fChnlOffset[2], outFile);
    LittleEndianTool.writeFloat(fChnlOffset[3], outFile);
    outFile.writeShort(Short.reverseBytes(nHomeXOffset));
    outFile.writeShort(Short.reverseBytes(nAwayXOffset));
    outFile.writeInt(Integer.reverseBytes(nStopXloc));

    nJointNum.write(outFile);
    LittleEndianTool.writeFloat(fWall, outFile);
    LittleEndianTool.writeFloat(fOD, outFile);
    nNumRev.write(outFile);
    nMotionBus.write(outFile);

}//end of WallMapDataSaver::saveHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::saveRevolution
//
// Saves a single revolution to the file.
//

public void saveRevolution(
        int pNumSamples0, int pNumSamples1, int pNumSamples2, int pNumSamples3)
                                                            throws IOException
{

    nNumAscan[0].value = pNumSamples0;
    nNumAscan[1].value = pNumSamples1;
    nNumAscan[2].value = pNumSamples2;
    nNumAscan[3].value = pNumSamples3;

    // write the data

    nNumAscan[0].write(outFile);
    nNumAscan[1].write(outFile);
    nNumAscan[2].write(outFile);
    nNumAscan[3].write(outFile);

    outFile.writeShort(Short.reverseBytes(nXloc));
    nMotionBus.write(outFile);

    LittleEndianTool.writeFloat(fCrossArea[0], outFile);
    LittleEndianTool.writeFloat(fCrossArea[1], outFile);
    LittleEndianTool.writeFloat(fCrossArea[2], outFile);
    LittleEndianTool.writeFloat(fCrossArea[3], outFile);

    writeWallReadingsForRevolution(100);

}//end of WallMapDataSaver::saveRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::writeWallReadingsForRevolution
//
// Saves data from one revolution all boards to the file.
//

public void writeWallReadingsForRevolution(int pNumRevolutions)
                                                        throws IOException
{

    //debug mks

    wall[0].value = 530;
    wall[1].value = 530;
    wall[2].value = 530;
    wall[3].value = 530;

    //debug mks

    for(int i = 0; i < pNumRevolutions; i++){

        wall[0].write(outFile);
        wall[1].write(outFile);
        wall[2].write(outFile);
        wall[3].write(outFile);

    }

}//end of WallMapDataSaver::writeWallReadingsForRevolution
//-----------------------------------------------------------------------------

}//end of class WallMapDataSaver
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
