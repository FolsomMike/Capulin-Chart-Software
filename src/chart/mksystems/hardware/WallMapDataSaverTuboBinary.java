/******************************************************************************
* Title: WallMapDataSaverTuboBinary.java
* Author: Mike Schoonover
* Date: 8/22/13
*
* Purpose:
*
* This class saves wall data collected by a UTBoard in a format compatible
* with Tuboscope's wall  map viewer.
*
* Many of the variable names here non-standard to Java -- they have been named
* to mimic the names used in the Tubo Map Viewer file format documentation.
*
* Reference
*
* Description of Tuboscope Wall Data Mapping File
* Author: Yanming Guo
* Date: Dec. 12, 2011
*
* NOTE1
*
* According to the Tubo document listed above, fMotionPulseLen "inches per
* motion pulse, needs to be 0.5". However, the program seems to force the value
* to 1.0", regardless of the value stored in the data file. However the Tubo
* data file seems to be based on a 0.5" fMotionPulseLen -- is the value set
* by some other variable?
*
* Per the documentation, nXloc is "linear location for this revolution, in
* motion pulses"
*
* For the IRNDT code to achieve a helix of
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
import chart.mksystems.settings.Settings;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;


//-----------------------------------------------------------------------------
// class SourceBoard
//
// This class handles variables related to a source board -- a board designated
// to provide data for mapping.
//

class SourceBoard extends Object{

    UTBoard utBoard;
    short dataBuffer[];

    int revolutionStartIndex;
    int revolutionEndIndex;

//-----------------------------------------------------------------------------
// SourceBoard::SourceBoard (constructor)
//

public void Sourceboard()
{

}//end of SourceBoard::SourceBoard (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SourceBoard::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init(UTBoard pUTBoard)
{

    utBoard = pUTBoard;

    dataBuffer = utBoard.getDataBuffer();

}//end of SourceBoard::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SourceBoard::setUpForSavingData
//
// Prepares variables for finding revolutions and anything require for
// manipulating the data for saving.
//

public void setUpForSavingData()
{

    revolutionStartIndex = -1;
    revolutionEndIndex = -1;

}//end of SourceBoard::setUpForSavingData
//-----------------------------------------------------------------------------

}//end of class SourceBoard
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// class WallMapDataSaverTuboBinary
//

public class WallMapDataSaverTuboBinary extends WallMapDataSaver{


    static final int FROM_HOME_DIRECTION_FLAG = 0x4000;
    static final int TOWARD_HOME_DIRECTION_FLAG = 0x0000;

    Settings settings;

    SourceBoard sourceBoards[];
    int numberOfSourceBoards;
    int numberOfHardwareChannels;
    int numberOfChannelsToStoreInFile;
    boolean copyChannelsToFillMissingChannels;

    int fileFormat;
    double measuredLength;

    DataOutputStream outFile;
    DataInputStream inFile;

    //NOTE: These variables are named oddly to match those used by Tuboscope's
    //Wall Map Viewer program code.

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
    short  nXloc;               // linear location for this revolution, in motion pulses
    WORD   nMotionBusNotUsed;   // not used
    float   fCrossArea[];       // not used

    // WALL_ASCAN    WallAscan[NUM_MAX_ASCAN];	// wall readings for this revolution all wall channels
                                                // (can't use structs in Java

    //--- end of variables contained in the Tubo struct WALL_REVOLUTION ---

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::WallMapDataSaverTuboBinary (constructor)
//

public WallMapDataSaverTuboBinary(Settings pSettings, int pFileFormat,
        int pNumberOfSourceBoards, int pNumberOfHardwareChannels,
        boolean pCopyChannelsToFillMissingChannels)
{

    settings = pSettings;
    fileFormat = pFileFormat;
    numberOfSourceBoards = pNumberOfSourceBoards;
    numberOfHardwareChannels = pNumberOfHardwareChannels;
    copyChannelsToFillMissingChannels = pCopyChannelsToFillMissingChannels;

}//end of WallMapDataSaverTuboBinary::WallMapDataSaverTuboBinary (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init(UTBoard pBoard0, UTBoard pBoard1, UTBoard pBoard2,
                                                            UTBoard pBoard3)
{

    //number of channels in file depends on fileFormat
    determineNumberOfChannelToStoreInFile();

    sourceBoards = new SourceBoard[numberOfSourceBoards];

    for (int i = 0; i < sourceBoards.length; i++){
        sourceBoards[i] = new SourceBoard();
    }

    sourceBoards[0].init(pBoard0); sourceBoards[1].init(pBoard1);
    sourceBoards[2].init(pBoard2); sourceBoards[3].init(pBoard3);

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
    fChnlOffset = new float[numberOfChannelsToStoreInFile];

    nNumRev = new WORD(0);

    wall = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < wall.length; i++){wall[i] = new WORD(0);}

    nNumAscan = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < nNumAscan.length; i++){nNumAscan[i] = new WORD(0);}

    fCrossArea = new float[numberOfChannelsToStoreInFile];

    nMotionBus = new WORD(0);
    nMotionBusNotUsed = new WORD(0);
    nJointNum = new DWORD(0);

    setUpTestData(); //use this to create test data

    //saveToFile("test1.dat", 31.2, "unknown" ); //debug mks -- remove this
    //loadFromFile("OVALYTEST_10_2 - Copy.dat"); //debug mks -- remove this

}//end of WallMapDataSaverTuboBinary::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::determineNumberOfChannelToStoreInFile
//
// Different formats require different numbers of channels in the file.
//
// This object is meant specifically to save in TUBO_BINARY_FORMAT.
//
// TUBO_BINARY_FORMAT always expects 4 channels in the output file. If fewer
// hardware channels are used, the data must be duplicated to fill the mising
// channels.
//

private void determineNumberOfChannelToStoreInFile()
{

    //always force file channels to 4 as this object always saves in
    //TUBO_BINARY_FORMAT

    if (fileFormat == TUBO_BINARY_FORMAT) {
        numberOfChannelsToStoreInFile = 4;
    }else{
        numberOfChannelsToStoreInFile = 4;
    }

}//end of WallMapDataSaverTuboBinary::determineNumberOfChannelToStoreInFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveToFile
//
// Writes all header data and the data in the UTBoards to file pFilename.
//

@Override
public void saveToFile(String pFilename, double pMeasuredLength,
                                    String pInspectionDirectionDescription)
{

    //this is the measured length of the test piece
    measuredLength = pMeasuredLength;

    //prepare to find revolutions and prepare data for saving
    for (int i = 0; i < sourceBoards.length; i++){
        sourceBoards[i].setUpForSavingData();
    }

    //debug mks -- remove this

    //load the buffers from a text file to provide debugging data
    loadAllDataBuffersFromTextFile(pFilename, settings.jobFileFormat);

    //save the buffers to a text file for debugging
    //saveAllDataBuffersToTextFile(pFilename,
    //             settings.jobFileFormat, pInspectionDirectionDescription);

    //debug mks end

    try{
        outFile =
              new DataOutputStream(new BufferedOutputStream(
              new FileOutputStream(pFilename)));

        saveHeader();

        //save all data from all revolutions
        saveRevolutions();

    }
    catch(IOException e){

    }
    finally{
        try{if (outFile != null) {outFile.close();}}
        catch(IOException e){}
    }

}//end of WallMapDataSaverTuboBinary::saveToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::setUpTestData
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
    wallStatFlag = 0x01;
    rbNum.set("RB Number", 10);
    spare.set("", 75);

    fMotionPulseLen = (float)1.0; //Tubo Map Viewer ignores this for display?
                                  //See Note 1 at top of file.

    fChnlOffset[0] = (float)0;
    fChnlOffset[1] = (float)0.625;
    fChnlOffset[2] = (float)1.25;
    fChnlOffset[3] = (float)1.875;

    nHomeXOffset = 0;
    nAwayXOffset = 0;

    nStopXloc = 372;

    nJointNum.value = 1001;
    fWall = (float)0.534;
    fOD = (float)11.75;

    nNumRev.value = 1000;

    //note if TOWARD_HOME is used, nStopLoc must be set to offset the entire
    //tube or all locations will be negative numbers
    //in that case, zero feet will be on the right of the screen

    nMotionBus.value = TOWARD_HOME_DIRECTION_FLAG; // FROM_HOME_DIRECTION_FLAG;
    nMotionBusNotUsed.value = 0;

}//end of WallMapDataSaverTuboBinary::setUpTestData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveHeader
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
    for(int i = 0; i < fChnlOffset.length; i++){
        LittleEndianTool.writeFloat(fChnlOffset[i], outFile);
    }
    outFile.writeShort(Short.reverseBytes(nHomeXOffset));
    outFile.writeShort(Short.reverseBytes(nAwayXOffset));
    outFile.writeInt(Integer.reverseBytes(nStopXloc));

    nJointNum.write(outFile);
    LittleEndianTool.writeFloat(fWall, outFile);
    LittleEndianTool.writeFloat(fOD, outFile);
    nNumRev.write(outFile);
    nMotionBus.write(outFile);

}//end of WallMapDataSaverTuboBinary::saveHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveRevolutions
//
// Saves all revolutions to the file.
//

public void saveRevolutions() throws IOException
{

    for (int i = 0; i < nNumRev.value; i++){
        saveRevolution(i, 500, 500, 500, 500);
    }

}//end of WallMapDataSaverTuboBinary::saveRevolutions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveRevolution
//
// Saves a single revolution to the file.
//
// pNumSamplesx is the number of samples taken by each channel during the
// revolution.
//

public void saveRevolution(int pRevolutionNumber,
        int pNumSamples0, int pNumSamples1, int pNumSamples2, int pNumSamples3)
                                                            throws IOException
{

    //find endpoints of the next revolution in the databuffer, etc.
    prepareToExtractNextRevolutionFromDataBuffer();

    // write the data

    //number of samples for each channel in the revolution
    for (int i = 0; i < nNumAscan.length; i++){
        nNumAscan[i].write(outFile);
    }

    //this is the location of the current revolution measured in number of
    // "motion pulses" where each pulse equals so many inches

    // NOTE: the Tubo Wall Viewer program seems to ignore the value for
    // fMotionPulseLen and defaults to 1.0 inch per pulse, thus the correct
    // operation seems to be multiplying the helix by 1.0 to get nXloc
    // see Note 1 at the top of this file.

    nXloc = (short)(pRevolutionNumber * 0.375);

    outFile.writeShort(Short.reverseBytes(nXloc));

    //not used
    nMotionBusNotUsed.write(outFile);
    //not used
    for (int i = 0; i < fCrossArea.length; i++){
        LittleEndianTool.writeFloat(fCrossArea[i], outFile);
    }

    writeWallReadingsForRevolution(pRevolutionNumber);

}//end of WallMapDataSaverTuboBinary::saveRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeWallReadingsForRevolution
//
// Saves data from one revolution all boards to the file.
//

public void writeWallReadingsForRevolution(int pRevolutionNumber)
                                                            throws IOException
{

    //debug mks

    wall[0].value = 530;
    wall[1].value = 531;
    wall[2].value = 532;
    wall[3].value = 533;

    //debug mks

    //write all data -- TUBO_BINARY_FORMAT always stores the entire size of the
    //maximum allowable, padding with zeroes if the number of samples is
    //actually less than the maximum

    int i = 0;

    while(i < NUM_MAX_ASCAN){

        if(i < nNumAscan[0].value){
            for(int j = 0; j < wall.length; j++){

                //debug mks -- this section needs to read data from UTBoards

                if(i > 99 && i < 110
                   && pRevolutionNumber > 99
                        && pRevolutionNumber < 110){

                        outFile.writeShort(0);

                }
                else{
                    wall[j].write(outFile);
                }

                //debug mks end

            }
        }
        else{
            for(int j = 0; j < wall.length; j++){
                outFile.writeByte(0); outFile.writeByte(0);
            }
        }

        i++;
    }//while

}//end of WallMapDataSaverTuboBinary::writeWallReadingsForRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::prepareToExtractNextRevolutionFromDataBuffer
//
// Finds the endpoints of the next revolution in the data buffers by looking
// for control codes.
//
// Control codes are deglitched -- if two occur too closely, the second is
// assumed to be a false signal (water drops on the reflector, etc.) and is
// ignored. It is not removed from the dataset -- all following functions
// should ignore any control codes between endpoints.
//

public void prepareToExtractNextRevolutionFromDataBuffer()
{



//    nNumAscan[0].value = pNumSamples0;
//    nNumAscan[1].value = pNumSamples1;
//    nNumAscan[2].value = pNumSamples2;
//    nNumAscan[3].value = pNumSamples3;



}//end of WallMapDataSaverTuboBinary::prepareToExtractNextRevolutionFromDataBuff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::loadFromFile
//
// Loads all header data and sample data from file pFilename.
//

@Override
public void loadFromFile(String pFilename)
{

    try{
        inFile =
              new DataInputStream(new BufferedInputStream(
              new FileInputStream(pFilename)));

        readHeader();

        //save all data from all revolutions
        readRevolutions();

    }
    catch(IOException e){

    }
    finally{
        try{if (inFile != null) {inFile.close();}}
        catch(IOException e){}
    }

}//end of WallMapDataSaverTuboBinary::loadFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::readHeader
//
// Reads the header from the file.
//

public void readHeader()throws IOException
{

    cfgFile.read(inFile);
    WO.read(inFile);
    grade.read(inFile);
    lotNum.read(inFile);
    heat.read(inFile);
    customer.read(inFile);
    operator.read(inFile);
    busUnit.read(inFile);
    comment.read(inFile);
    version.read(inFile);
    verNum.read(inFile);
    nominalWall = LittleEndianTool.readFloat(inFile);
    OD = LittleEndianTool.readFloat(inFile);
    location.read(inFile);
    wellName.read(inFile);
    date.read(inFile);
    range.read(inFile);
    wellFoot.read(inFile);
    wallStatFlag = inFile.readByte();
    rbNum.read(inFile);
    spare.read(inFile);
    fMotionPulseLen = LittleEndianTool.readFloat(inFile);
    for(int i = 0; i < fChnlOffset.length; i++){
        fChnlOffset[i] = LittleEndianTool.readFloat(inFile);
    }
    nHomeXOffset = Short.reverseBytes(inFile.readShort());
    nAwayXOffset = Short.reverseBytes(inFile.readShort());
    nStopXloc = Integer.reverseBytes(inFile.readInt());

    nJointNum.read(inFile);
    fWall = LittleEndianTool.readFloat(inFile);
    fOD = LittleEndianTool.readFloat(inFile);
    nNumRev.read(inFile);
    nMotionBus.read(inFile);

}//end of WallMapDataSaverTuboBinary::readHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::readRevolutions
//
// Reads all revolutions from the file.
//

public void readRevolutions() throws IOException
{

    for (int i = 0; i < nNumRev.value; i++){
        readRevolution(i, 500, 500, 500, 500);
    }

}//end of WallMapDataSaverTuboBinary::readRevolutions
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::readRevolution
//
// Reads a single revolution from the file.
//
// pNumSamplesx is the number of samples taken by each channel during the
// revolution.
//

public void readRevolution(int pRevolutionNumber,
        int pNumSamples0, int pNumSamples1, int pNumSamples2, int pNumSamples3)
                                                            throws IOException
{

    //number of samples for each channel in the revolution
    for (int i = 0; i < nNumAscan.length; i++){
        nNumAscan[i].read(inFile);
    }

    //this is the location of the current revolution measured in number of
    // "motion pulses" where each pulse equals so many inches
    // if the "motion pulse" is equal to the helix, then each revolution will
    // advance one "motion pulse"

    nXloc = Short.reverseBytes(inFile.readShort());

    //not used
    nMotionBusNotUsed.read(inFile);
    //not used
    for (int i = 0; i < fCrossArea.length; i++){
        fCrossArea[i] = LittleEndianTool.readFloat(inFile);
    }

    readWallReadingsForRevolution();

}//end of WallMapDataSaverTuboBinary::readRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::readWallReadingsForRevolution
//
// Reads data from one revolution from the file.
//

public void readWallReadingsForRevolution() throws IOException
{

   //read all data -- TUBO_BINARY_FORMAT always stores the entire size of the
    //maximum allowable, padding with zeroes if the number of samples is
    //actually less than the maximum

    int i = 0;

    byte skipPadding;

    while(i < NUM_MAX_ASCAN){

        if(i < nNumAscan[0].value){
            for(int j = 0; j < wall.length; j++){
                wall[j].read(inFile);
            }
        }
        else{
            for(int j = 0; j < wall.length; j++){
                skipPadding = inFile.readByte();
                skipPadding = inFile.readByte();
            }
        }
        i++;
    }//while

}//end of WallMapDataSaverTuboBinary::readWallReadingsForRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debug mks -- move this to WallMapDataSaverIRNDTText.java::saveAllDataBuffersToTextFile
//
// Saves data buffers for all source boards to text files.
//

public void saveAllDataBuffersToTextFile(String pFilename,
                 String pJobFileFormat, String pInspectionDirectionDescription)
{

    for (int i = 0; i < sourceBoards.length; i++){
        saveDataBufferToTextFile(i,
           pFilename, settings.jobFileFormat, pInspectionDirectionDescription);
    }

}//end of Debug mks -- move this to WallMapDataSaverIRNDTText.java::saveAllDataBuffersToTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debug mks -- move this to WallMapDataSaverIRNDTText.java::saveDataBufferToTextFile
//
// Saves the data in dataBuffer of pBoard to a text file.
//
// NOTE: This saves from position zero to the end of data -- it does not
//  understand segments like the trace saving function does which is necessary
//  when the index restarts at 0 as required for end to end inspection. Fix
//  this some day.
//

public void saveDataBufferToTextFile(int pBoard,
                    String pFilename, String pJobFileFormat,
                                         String pInspectionDirectionDescription)
{

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pBoard;

    //create a buffered writer stream

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter bwOut = null;

    try{

        fileOutputStream = new FileOutputStream(pFilename);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream,
                                                                pJobFileFormat);
        bwOut = new BufferedWriter(outputStreamWriter);

        //write the header information - this portion can be read by the iniFile
        //class which will only read up to the "[Header End]" tag - this allows
        //simple parsing of the header information while ignoring the data
        //stream which  follows the header

        bwOut.write("[Header Start]"); bwOut.newLine();
        bwOut.newLine();
        bwOut.write("Segment Data Version=" + Settings.SEGMENT_DATA_VERSION);
        bwOut.newLine();
        bwOut.write("Measured Length=" + measuredLength);
        bwOut.newLine();
        bwOut.write("Inspection Direction="
                                     + pInspectionDirectionDescription);
        bwOut.newLine();
        bwOut.write("[Header End]"); bwOut.newLine(); bwOut.newLine();

        bwOut.write("[Data Set 1]"); bwOut.newLine(); //save the first data set

        int endOfData =
             sourceBoards[pBoard].utBoard.getIndexOfLastDataPointinDataBuffer();

        //save all data stored in the buffer
        for(int i = 0; i < endOfData; i++){
            bwOut.write(Integer.toString(sourceBoards[pBoard].dataBuffer[i]));
            bwOut.newLine();
        }

        bwOut.write("[End of Set]"); bwOut.newLine();

    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 766");
    }
    finally{
        try{if (bwOut != null) {bwOut.close();}}
        catch(IOException e){}
        try{if (outputStreamWriter != null) {outputStreamWriter.close();}}
        catch(IOException e){}
        try{if (fileOutputStream != null) {fileOutputStream.close();}}
        catch(IOException e){}
    }

}//end of Debug mks -- move this to WallMapDataSaverIRNDTText.java::saveDataBufferToTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debug mks -- move this to WallMapDataSaverIRNDTText.java::loadAllDataBuffersFromTextFile
//
// Loads data buffers for all source boards from text files.
//

public void loadAllDataBuffersFromTextFile(String pFilename,
                                                        String pJobFileFormat)
{

    for (int i = 0; i < sourceBoards.length; i++){
        loadDataBufferFromTextFile(i, pFilename, settings.jobFileFormat);
    }

}//end of Debug mks -- move this to WallMapDataSaverIRNDTText.java::loadAllDataBuffersFromTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debug mks -- move this to WallMapDataSaverIRNDTText.java::loadDataBufferFromTextFile
//
// Loads the data in dataBuffer of pBoard from a text file. If board is not set
// up for mapping or no data buffer has been created, nothing is done.
//
// Returns error messages on error, empty string on no error.
//

public String loadDataBufferFromTextFile(int pBoard,
                                    String pFilename, String pJobFileFormat)
{

    String status = "";

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pBoard;

    int i = 0;

    //create a buffered writer stream

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader brIn = null;

    short dataBuffer[] = sourceBoards[pBoard].dataBuffer;

    String startTag = "[Data Set 1]";
    String startTagUC = startTag.toUpperCase();

    try{

        fileInputStream = new FileInputStream(pFilename);
        inputStreamReader = new InputStreamReader(fileInputStream,
                                                                pJobFileFormat);
        brIn = new BufferedReader(inputStreamReader);

        String line;
        boolean success = false;

        //search for the data segment tag
        while ((line = brIn.readLine()) != null){  //search for tag
            if (line.trim().toUpperCase().startsWith(startTagUC)){
                success = true; break;
            }
        }

        if (success == false) {
            throw new IOException(
             "The file could not be read - tag " + startTag
                                                          + " not found.");
        }

        i = 0;

        while ((line = brIn.readLine()) != null){

            //stop when next section end tag reached (will start with [)
            if (line.trim().startsWith("[")){
                break;
            }

            //convert the text to an integer and save in the buffer
            short data = Short.parseShort(line);
            dataBuffer[i++] = data;

            //catch buffer overflow
            if (i == dataBuffer.length) {
                throw new IOException(
                 "The file could not be read - too much data for " + startTag
                                                       + " at data point " + i);
                }

        }//while ((line = pIn.readLine()) != null)

        //let the board know the position of the last data in the buffer
        sourceBoards[pBoard].utBoard.setIndexOfLastDataPointinDataBuffer(i);

    }
    catch(NumberFormatException e){
        //catch error translating the text to an integer
        return("The file could not be read - corrupt data for " + startTag
                                                   + " at data point " + i);
    }
    catch (FileNotFoundException e){
        return("Could not find the requested file.");
        }
    catch(IOException e){
        return(e.getMessage() + " " + getClass().getName() + " - Error: 970");
        }
    finally{
        try{if (brIn != null) {brIn.close();}}
        catch(IOException e){}
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){}
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){}

        return(status);
    }

}//end of Debug mks -- move this to WallMapDataSaverIRNDTText.java::loadDataBufferFromTextFile
//-----------------------------------------------------------------------------

}//end of class WallMapDataSaverTuboBinary
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
