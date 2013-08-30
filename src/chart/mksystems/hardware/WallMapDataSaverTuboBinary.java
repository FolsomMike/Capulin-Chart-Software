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
import chart.mksystems.inifile.IniFile;
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
// class WallMapDataSaverTuboBinary
//

public class WallMapDataSaverTuboBinary extends WallMapDataSaver{


    //there must be this many values saved for each channel -- left over space
    //should be zero filled
    static final int REVOLUTION_SAMPLES_BLOCK_SIZE = 2000;

    static final int FROM_HOME_DIRECTION_FLAG = 0x4000;
    static final int TOWARD_HOME_DIRECTION_FLAG = 0x0000;

    //in the "going away" inspection direction, the Tubo Map Viewer as of
    // 8/30/13 has a built in offset of 56 inches -- the following can be used
    // to correct for that as it's not compatible with the Chart program

    static final int GOING_AWAY_OFFSET_CORRECTION = -56;

    Settings settings;

    MapSourceBoard mapSourceBoards[];
    int numberOfMapSourceBoards;
    int numberOfHardwareChannels;
    int numberOfChannelsToStoreInFile;
    boolean copyChannelsToFillMissingChannels;

    WORD wallWord;
    WORD countWord;

    //the average values are used to detect values which are outside the norm

    int leastNumberOfRevs = 0;
    int avgNumberOfRevs = 0;
    int mostNumberOfSamplesPerRev = 0;
    int avgNumberOfSamplesPerRev = 0;

    int fileFormat;

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
     int pNumberOfHardwareChannels, boolean pCopyChannelsToFillMissingChannels)
{

    settings = pSettings;
    fileFormat = pFileFormat;
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
public void init(MapSourceBoard pMapSourceBoards[])
{

    mapSourceBoards = pMapSourceBoards;
    numberOfMapSourceBoards = mapSourceBoards.length;

    //number of channels in file depends on fileFormat
    determineNumberOfChannelToStoreInFile();

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

    wallWord = new WORD(0);
    countWord = new WORD(0);

    wall = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < wall.length; i++){wall[i] = new WORD(0);}

    nNumAscan = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < nNumAscan.length; i++){nNumAscan[i] = new WORD(0);}

    fCrossArea = new float[numberOfChannelsToStoreInFile];

    nMotionBus = new WORD(0);
    nMotionBusNotUsed = new WORD(0);
    nJointNum = new DWORD(0);

    //setUpTestData(); //use this to create test data for job info entries

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
public void saveToFile(String pFilename)
{


    setUpJobInfo();

    //prepare to find revolutions and prepare data for saving
    for (int i = 0; i < mapSourceBoards.length; i++){
        mapSourceBoards[i].setUpForSavingData();
    }

    //debug mks -- remove this

    //load the buffers from a text file to provide debugging data
    //loadAllDataBuffersFromTextFile(pFilename, settings.jobFileFormat);

    //save the buffers to a text file for debugging
    //saveAllDataBuffersToTextFile(pFilename,
    //             settings.jobFileFormat, pInspectionDirectionDescription);

    //debug mks end

    try{
        outFile =
              new DataOutputStream(new BufferedOutputStream(
              new FileOutputStream(pFilename)));

        //find start/stop, count revolutions, repair missing control codes, etc.
        //must be done before saveHeader as the header needs some of that info

        analyzeAndRepairData();

        //save all header information
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
// WallMapDataSaverTuboBinary::setUpJobInfo
//
// Loads the job information and transfers it to local variables.
//

public void setUpJobInfo()
{

    loadJobInfo();

    copyJobInfoToLocalVariables();

}//end of WallMapDataSaverTuboBinary::setUpJobInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::loadJobInfo
//
// Loads the job information into an iniFile object.  No values are transferred
// out -- they are extracted by other functions as needed.
//

public void loadJobInfo()
{

    //if the ini file cannot be opened and loaded, exit without action and
    //default values will be used

    try {
        jobInfoFile = new IniFile(
            settings.currentJobPrimaryPath + "03 - " + settings.currentJobName
            + " Job Info.ini", settings.jobFileFormat);
    }
    catch(IOException e){return;}

}//end of WallMapDataSaverTuboBinary::loadJobInfo
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::copyJobInfoToLocalVariables
//
// Copies information from jobInfo object to local variables used to save
// that info to the data file
//
// Note 1:
//
// Photo eye to front edge of head: 7.5"
// Front edge of head to ducer: 7"
// Delay from inspection start to Start Inspection flag for ducer 1:  6 x 3/8"
//                                                                     (2.25")
// Location of photo eye from end of pipe at start inspection: 30.8"
//
//
// nHomeXOffset required by Tubo Wall Map viewer to properly: 21
//
// Distance from end of pipe to ducer 1 at start inspection =
//          30.8 - 7.5 - 7 + 2.25 = 18.55  -- 2.45" missing from the 21"
//




public void copyJobInfoToLocalVariables()
{

    cfgFile.set("01 - " + settings.currentJobName + " Configuration.ini", 128);
    WO.set(retrieveJobInfoString("Work Order"), 10);
    grade.set(retrieveJobInfoString("Grade"), 10);
    lotNum.set(retrieveJobInfoString("Lot"), 10);
    heat.set(retrieveJobInfoString("Heat"), 34);
    customer.set(retrieveJobInfoString("Customer Name"), 32);
    operator.set(retrieveJobInfoString("Unit Operator"), 10);
    busUnit.set(retrieveJobInfoString("Business Unit"), 32);
    comment.set(retrieveJobInfoString("Comment"), 80);
    version.set(Settings.MAP_TUBO_BINARY_DATA_VERSION, 16);
    verNum.value = (int)(Settings.MAP_TUBO_BINARY_DATA_VERSION_NUMBER * 100);
    nominalWall = (float)settings.nominalWall;
    OD = (float)parseDiameterFromJobInfoString();
    location.set(retrieveJobInfoString("Job Location"), 40);
    wellName.set(retrieveJobInfoString("Well Name"), 40);
    date.set(retrieveJobInfoString("Date Job Started"), 10);
    range.set(retrieveJobInfoString("Pipe Range"), 8);
    wellFoot.set(retrieveJobInfoString("Well Footage"), 12);
    wallStatFlag = 0x01;
    rbNum.set(retrieveJobInfoString("RB Number"), 10);
    spare.set("", 75);

    fMotionPulseLen = (float).375; //need to read this from config file

    // sensor position is already offset by the map source sensor delays
    // so all values are zero here
    fChnlOffset[0] = (float)0;
    fChnlOffset[1] = (float)0;
    fChnlOffset[2] = (float)0;
    fChnlOffset[3] = (float)0;

    nHomeXOffset = 21; //debug mks -- read actual value here see Note 1 above

    nHomeXOffset += GOING_AWAY_OFFSET_CORRECTION;

    nStopXloc = 0;

    nJointNum.value = 1;
    fWall = (float)settings.nominalWall;
    fOD = (float)parseDiameterFromJobInfoString();

    nNumRev.value = 0; //set later after revolutions are counted

    nMotionBus.value = 0;

    //set the inspection direction flag
    nMotionBus.value |= parseInspectionDirectionFlagFromJobInfoString();

    nMotionBusNotUsed.value = 0; //this value is stored with each revolution

}//end of WallMapDataSaverTuboBinary::copyJobInfoToLocalVariables
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::retrieveJobInfoString
//
// Retrieves and returns the value from the jobInfoFile associated with pKey.
// Returns empty string if pKey is not found.
//

private String retrieveJobInfoString(String pKey)
{

    return(jobInfoFile.readString("Job Info", pKey, ""));

}//end of WallMapDataSaverTuboBinary::retrieveJobInfoString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::parseDiameterFromJobInfoString
//
// Retrieves, parses, and returns the value from the jobInfoFile associated
// with pKey.
//
// The diameter is stored as a string an can take many variations such as:
//
//  14.750, 14-3/4, 14 3/4
//
// This method attempts to convert the various formats to a double.
//
// Returns 0 if not found or format not supported.
//
// NOTE: currently on converts decimal format...13.75 and such -- need to add
// ability to convert xx-y/z format
//

private double parseDiameterFromJobInfoString()
{

    String string = jobInfoFile.readString("Job Info", "Pipe Diameter", "");

    String culled = "";

    //strip out all characters except numbers and decimal points

    //NOTE: when code for parsing of xx-y/z added, leave in dashes and slashes

    for(int i=0; i < string.length(); i++){

        if (isNumerical(string.charAt(i))) {
            culled = culled + string.charAt(i);
        }

    }


    double value;

    try{

        value = Double.valueOf(culled);

    }
    catch(NumberFormatException e){

        value = 0;
    }

    return(value);

}//end of WallMapDataSaverTuboBinary::parseDiameterFromJobInfoString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::isNumerical
//
// Returns true if pInput is a number or a decimal point.
//

public boolean isNumerical(char pInput)
{

    if(    (pInput == '0')
        || (pInput == '1')
        || (pInput == '2')
        || (pInput == '3')
        || (pInput == '4')
        || (pInput == '5')
        || (pInput == '6')
        || (pInput == '7')
        || (pInput == '8')
        || (pInput == '9')
        || (pInput == '.') ) {return(true);}

    return(false); //not a numerical type character

}//end of WallMapDataSaverTuboBinary::isNumerical
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::parseInspectionDirectionFlagFromString
//
// Parses the string description from the jobInfo object of the inspection
// direction into a value compatible with the flag in the Tubo binary file
// format.
//

private int parseInspectionDirectionFlagFromJobInfoString()
{

    if (settings.inspectionDirectionDescription.equals(settings.awayFromHome)){

        return(FROM_HOME_DIRECTION_FLAG);

    }
    else {

        return(TOWARD_HOME_DIRECTION_FLAG);

    }

}//end of WallMapDataSaverTuboBinary::parseInspectionDirectionFlagFromString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveHeader
//
// Saves the header to the file.
//

private void saveHeader()throws IOException
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
    nNumRev.value = leastNumberOfRevs;
    nNumRev.write(outFile);
    nMotionBus.write(outFile);

}//end of WallMapDataSaverTuboBinary::saveHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveRevolutions
//
// Saves all revolutions to the file.
//

private void saveRevolutions() throws IOException
{

    //debug mks -- remove this
    if (leastNumberOfRevs < 5){
        leastNumberOfRevs = leastNumberOfRevs;
    }
    //debug mks -- remove this

    for (int i = 0; i < leastNumberOfRevs; i++){
        saveRevolution(i);
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

private void saveRevolution(int pRevolutionNumber) throws IOException
{

    //find endpoints of the next revolution in the databuffer, etc.
    prepareToExtractNextRevolutionFromDataBuffer();

    // write the data

    // convert the number of samples to a WORD and then write to the file
    for (int i = 0; i < mapSourceBoards.length; i++){

        //the number of samples will vary between the boards, the max number
        //from any board is used instead with missing samples in the other
        //boards filled at write time

        countWord.value = mostNumberOfSamplesPerRev;
        countWord.write(outFile);
    }

    //this is the location of the current revolution measured in number of
    // "motion pulses" where each pulse equals so many inches

    nXloc = (short)(pRevolutionNumber * .375);
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
// Since the different channels will usually have a slightly different number
// of samples in any given revolution, the one with the highest sample count
// is used as the count. The others use the last value reported by the last
// board to have valid data -- doesn't matter which one so long as a valid
// data point is used as filler. This way, no samples are actually thrown
// away, instead some are duplicated.
//

private void writeWallReadingsForRevolution(int pRevolutionNumber)
                                                            throws IOException
{

    //write all data -- TUBO_BINARY_FORMAT always stores the entire size of the
    //maximum allowable -- REVOLUTION_SAMPLES_BLOCK_SIZE, padding with zeroes
    //if the number of samples is actually less than the maximum

    int i = 0;

    while(i < REVOLUTION_SAMPLES_BLOCK_SIZE){

        //write data until number of samples reached for the board which had
        //the most samples in the current revolution

        if(i < mostNumberOfSamplesPerRev){

            for(int j = 0; j < mapSourceBoards.length; j++){

                //get samples from each board until end of revolution reached
                //(different for each board as each will have a slightly
                // different number of samples/rev), then use filler data

                if (mapSourceBoards[j].sampleIndex
                                        < mapSourceBoards[j].revEndIndex) {

                    int TOF = mapSourceBoards[j].
                               dataBuffer[mapSourceBoards[j].sampleIndex++];

                    double lWall = (TOF * 0.015 * .233) / 2;

                    wallWord.value = (int)(lWall * 1000);

                if (wallWord.value < 0)   //debug mks
                     wallWord.value = wallWord.value; //debug mks

                }
                else{
                    //leave wallWord at its last value -- doesn't matter what
                    //board it came from, just need valid data for filler
                    int debug = 0; //debug mks remove this
                }

                wallWord.write(outFile);

                if (wallWord.value < 0)   //debug mks
                     wallWord.value = wallWord.value; //debug mks


            }//for(int j = 0; j < mapSourceBoards.length; j++)
        }//if(i < mostNumberOfSamplesPerRev)
        else{
            //write zeroes to fill out the block length
            for(int j = 0; j < mapSourceBoards.length; j++){
                outFile.writeByte(0); outFile.writeByte(0);
            }
        }//else

        i++;

    }//while

}//end of WallMapDataSaverTuboBinary::writeWallReadingsForRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::analyzeAndRepairData
//
// Analyzes the data in preparation for saving to file.
//
// Finds the start/stop inspection points, counts the revolutions, repairs
// missing control codes, etc.
//

private void analyzeAndRepairData()
{

    //find start/stop indices for all map source boards
    findStartStopInspectionIndices();

    //compute rev count for each board and find the one with the least
    calculateRevCount();

}//end of WallMapDataSaverTuboBinary::analyzeAndRepairData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::findStartStopInspectionIndices
//
// Finds the start/stop inspection points of all map source boards. The
// results are stored in the mapSourceBoard objects.
//

private void findStartStopInspectionIndices()
{

    for (int i = 0; i < mapSourceBoards.length; i++){

        int startIndex, stopIndex;

        startIndex = findControlCode(i, UTBoard.MAP_START_CODE_FLAG,
                                                        0, Integer.MAX_VALUE);

        mapSourceBoards[i].inspectionStartIndex = startIndex;

        //this is also the start location for the first revolution
        mapSourceBoards[i].revEndIndex = startIndex;

        stopIndex = findControlCode(i, UTBoard.MAP_STOP_CODE_FLAG,
                                            startIndex + 1, Integer.MAX_VALUE);

        mapSourceBoards[i].inspectionStopIndex = stopIndex;

    }

}//end of WallMapDataSaverTuboBinary::findStartStopInspectionIndices
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateRevCount
//
// Calculates the number of revolutions for each board. Stores the value of
// the least number of revs of any of the boards in leastNumberOfRevs and
// the average number of revs in avgNumberOfRevs.
//
// Count drops one revolution -- don't want to use last rev as it may be corrupt
// due to head liftoff. First rev should be good as its already delayed after
// head drop
//

private void calculateRevCount()
{

    leastNumberOfRevs = Integer.MAX_VALUE;
    avgNumberOfRevs = 0;
    int index;

    for (int i = 0; i < mapSourceBoards.length; i++){

        int start = mapSourceBoards[i].inspectionStartIndex;
        int stop = mapSourceBoards[i].inspectionStopIndex;
        int revCount = 0;

        //look for all control codes between the start and stop points

        while((index = findControlCode(i, 0, start, stop)) != -1) {
            revCount++;
            start = index + 1;
        }

        //drop one from the count to ignore the last rev
        if (revCount < 0) { revCount--; }

         mapSourceBoards[i].numberOfRevs = revCount;

        //trap value of the smallest rev count of all the boards
        if (revCount < leastNumberOfRevs) { leastNumberOfRevs = revCount; }

        avgNumberOfRevs += revCount;

    }

    //calculate the average
    avgNumberOfRevs = avgNumberOfRevs / mapSourceBoards.length;

}//end of WallMapDataSaverTuboBinary::calculateRevCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::findControlCode
//
// Finds the next occurrance of the control code with pFlag set for board
// pBoardIndex. Only control codes (those values with MAP_CONTROL_CODE_FLAG bit
// set) will be checked.
//
// Searching will begin at pStart and end at pEnd - 1. If pEnd is set to
// Integer.MAX_VALUE, the search will end at buffer end - 1.
//
// Returns index of first occurrance of pFlag between pStart and pEnd - 1.
// Returns -1 if the code is not found.
//

private int findControlCode(int pBoardIndex, int pFlag, int pStart, int pEnd)
{

    short []dataBuffer = mapSourceBoards[pBoardIndex].dataBuffer;

    //bail out for invalid pStart
    if (pStart < 0 || pStart > dataBuffer.length) { return(-1); }

    //catch MAX_VALUE -- special signal to search to end of buffer
    if (pEnd == Integer.MAX_VALUE) { pEnd = dataBuffer.length; }

    //only look at values with the MAP_CONTROL_CODE_FLAG bit set as well
    int target = UTBoard.MAP_CONTROL_CODE_FLAG | pFlag;

    for (int i = pStart; i < pEnd; i++){
        if((dataBuffer[i] & target) == target) { return(i); }
    }

    //code not found
    return(-1);

}//end of WallMapDataSaverTuboBinary::findControlCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::prepareToExtractNextRevolutionFromDataBuffer
//
// Finds the endpoint of the next revolution in the data buffers by looking
// for the next control code. Determines the number of samples in the
// revolution, sets up pointers, prepares for the writing of the data to
// the file.
//

private void prepareToExtractNextRevolutionFromDataBuffer()
{

    calculateNumberSamplesInRev();

}//end of WallMapDataSaverTuboBinary::prepareToExtractNextRevolutionFromDataBuff
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateNumberSamplesInRev
//
// Determines the position of and the number of samples in the next revolution
// in each map source board and records the value of the highest revolution
// count of any boards.
//

private void calculateNumberSamplesInRev()
{

    mostNumberOfSamplesPerRev = Integer.MIN_VALUE;
    avgNumberOfSamplesPerRev = 0;
    int sampleCount;

    int index;

    for (int i = 0; i < mapSourceBoards.length; i++){

        //revIndex points at the control code, skip past to first value
        int start = mapSourceBoards[i].revStartIndex + 1;
        int stop = Integer.MAX_VALUE;

        //the first sample in rev will be first value after control code
        mapSourceBoards[i].sampleIndex = start;

        //look for the next control code -- that's the end of the revolution
        index = findControlCode(i, 0, start, stop);

        //store end of current revolution
        mapSourceBoards[i].revEndIndex = index;

        //preset to end of this revolution (start of next) for next time
        mapSourceBoards[i].revStartIndex = index;

        //calculate number of samples in the revolution
        sampleCount = index - start - 1;

        mapSourceBoards[i].numSamplesInRev = sampleCount;

        //trap value of the largest sample count of all the boards for this
        //revolution
        if (sampleCount > mostNumberOfSamplesPerRev) {
            mostNumberOfSamplesPerRev = sampleCount;
        }

        avgNumberOfSamplesPerRev += sampleCount;

    }

    //calculate the average
    avgNumberOfSamplesPerRev = avgNumberOfSamplesPerRev/mapSourceBoards.length;

}//end of WallMapDataSaverTuboBinary::calculateNumberSamplesInRev
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::loadFromFile
//
// Loads all header data and sample data from file pFilename.
//

@Override
void loadFromFile(String pFilename)
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

private void readHeader()throws IOException
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

private void  readRevolutions() throws IOException
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

private void readRevolution(int pRevolutionNumber,
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

private void readWallReadingsForRevolution() throws IOException
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
// WallMapDataSaverTuboBinary::setUpTestData
//
// Set up all variables with test data for debugging purposes.
//

public void setUpTestData()
{

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

    //fChnlOffset[0] = (float)0;
    //fChnlOffset[1] = (float)0.625;
    //fChnlOffset[2] = (float)1.25;
    //fChnlOffset[3] = (float)1.875;

    // sensor position is already offset by the map source sensor delays
    fChnlOffset[0] = (float)0;
    fChnlOffset[1] = (float)0;
    fChnlOffset[2] = (float)0;
    fChnlOffset[3] = (float)0;


    nHomeXOffset = 0;
    nAwayXOffset = 0;

    nStopXloc = 372;

    nJointNum.value = 1001;
    fWall = (float)0.534;
    fOD = (float)11.75;

    nNumRev.value = 1000;

    nMotionBus.value = TOWARD_HOME_DIRECTION_FLAG; // FROM_HOME_DIRECTION_FLAG;
    nMotionBusNotUsed.value = 0;

}//end of WallMapDataSaverTuboBinary::setUpTestData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Debug mks -- move this to WallMapDataSaverIRNDTText.java::saveAllDataBuffersToTextFile
//
// Saves data buffers for all source boards to text files.
//

private void saveAllDataBuffersToTextFile(String pFilename,
                 String pJobFileFormat, String pInspectionDirectionDescription)
{

    for (int i = 0; i < mapSourceBoards.length; i++){
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

private void saveDataBufferToTextFile(int pBoard,
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
        bwOut.write("Measured Length=" + settings.measuredPieceLength);
        bwOut.newLine();
        bwOut.write("Inspection Direction="
                                     + pInspectionDirectionDescription);
        bwOut.newLine();
        bwOut.write("[Header End]"); bwOut.newLine(); bwOut.newLine();

        bwOut.write("[Data Set 1]"); bwOut.newLine(); //save the first data set

        int endOfData =
          mapSourceBoards[pBoard].utBoard.getIndexOfLastDataPointInDataBuffer();

        //save all data stored in the buffer
        for(int i = 0; i < endOfData; i++){
            bwOut.write(
                    Integer.toString(mapSourceBoards[pBoard].dataBuffer[i]));
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

private void loadAllDataBuffersFromTextFile(String pFilename,
                                                        String pJobFileFormat)
{

    for (int i = 0; i < mapSourceBoards.length; i++){
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

private String loadDataBufferFromTextFile(int pBoard,
                                    String pFilename, String pJobFileFormat)
{

    String status = "";

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pBoard;

    int i = 0;

    //create a buffered writer stream

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader brIn = null;

    short dataBuffer[] = mapSourceBoards[pBoard].dataBuffer;

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
        mapSourceBoards[pBoard].utBoard.setIndexOfLastDataPointInDataBuffer(i);

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
