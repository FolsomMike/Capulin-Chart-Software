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
* Tubo Wall Map Format Notes
*
* The Tubo document specifies:
*
* RAW_WALL_HEAD.JobRec.Version = “TruscanWD200501”
*
* If any other value is used, the locations will be different between the map
* and the CVS file and the Motion Pulse length will not be accurate.
*
* The fChnlOffset value for each channel CANNOT BE ALL ZEROES or the Tubo
* Map Viewer program will crash at the 51st revolution. Even using values of
* 0, .1, .2, .3 will cause a crash. Thus, if the revolutions are already
* aligned, zero offsets cannot be used so some other arrangement must be used.
*
* The Tubo document also specifies that fMotionPulseLen must be 0.5".
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
import chart.mksystems.settings.Settings;
import chart.mksystems.tools.CharBuf;
import chart.mksystems.tools.DWORD;
import chart.mksystems.tools.EndianTools;
import chart.mksystems.tools.WORD;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


//-----------------------------------------------------------------------------
// class WallMapDataSaverTuboBinary
//

public class WallMapDataSaverTuboBinary extends WallMapDataSaver{

    static final int SAVE_TO_TEXT = 1;
    static final int LOAD_FROM_TEXT = 2;
    static final int SAVE_TO_BINARY = 3;
    static final int LOAD_FROM_BINARY = 4;

    //each slice contains a sample from each channel
    //thus there are NUMBER_SLICES_PER_REV * NUMBER CHANNELS entries
    //this number of samples is always written to the file; as 
    
    static final int NUMBER_SLICES_PER_REV = 2000;

    static final int FROM_HOME_DIRECTION_FLAG = 0x4000;
    static final int TOWARD_HOME_DIRECTION_FLAG = 0x0000;

    // in the "going away" inspection direction, the Tubo Map Viewer as of
    // 8/30/13 has a built in offset  -- the following can be used to correct
    // for that
    // units is inches
    // NOTE: due to backwards nomenclature in the Tubo map viewer, the
    //  "home" offset variable is used with "AWAY" offset correction

    static final double GOING_AWAY_TUBO_OFFSET_CORRECTION = -28;

    //in the "going home" inspection direction, the Tubo Map Viewer as of
    // 8/30/13 has a built in offset -- the following can be used to correct
    // for that
    // units is inches
    // NOTE: due to backwards nomenclature in the Tubo map viewer, the
    //  "away" offset variable is used with "HOME" offset correction
    
    static final double GOING_HOME_TUBO_OFFSET_CORRECTION = 30.5;

    //the PLC and/or photo-eye have built-in delay to prevent false triggering
    //this value is used to correct for the distance missed during delay
    
    static final double GOING_HOME_PLC_OFFSET_CORRECTION = -1.5;
    
    double avgCalculatedHelix;

    Settings settings;

    double distanceInspected;
    
    MapSourceBoard mapSourceBoards[];
    int numberOfMapSourceBoards;
    int numberOfHardwareChannels;
    int numberOfChannelsToStoreInFile;
    boolean copyChannelsToFillMissingChannels;

    WORD wallWord;
    WORD countWord;
    WORD fillerWord;
    
    static int PADDING_VALUE;

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
public void init(MapSourceBoard pMapSourceBoards[],
                                                  EncoderValues pEncoderValues)
{

    super.init(pMapSourceBoards, pEncoderValues);
    
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
    fillerWord = new WORD(0);

    wall = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < wall.length; i++){wall[i] = new WORD(0);}

    nNumAscan = new WORD[numberOfChannelsToStoreInFile];
    for(int i = 0; i < nNumAscan.length; i++){nNumAscan[i] = new WORD(0);}

    fCrossArea = new float[numberOfChannelsToStoreInFile];

    nMotionBus = new WORD(0);
    nMotionBusNotUsed = new WORD(0);
    nJointNum = new DWORD(0);

    //setUpTestData(); //use this to create test data for job info entries

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
// WallMapDataSaverTuboBinary::dumpOrLoadAllDataBuffersToFiles
//
// Saves or loads data buffers from all map source boards to files.
//
// The choice of saving or loading to text or binary files is specified by
// pLoadSaveFormatChoice:
//
// SAVE_TO_TEXT, LOAD_FROM_TEXT, SAVE_TO_BINARY, LOAD_FROM_BINARY
//
// Note 1: The same start and stop inspection locations are stored for all
// mapping boards. Each mapping board only has one mapping channel tied to
// a single transducer. Using the known head and offset from photo eye for that
// transducer allows the actual position of the transducer to be calculated
// when needed.
//

public void dumpOrLoadAllDataBuffersToFiles(String pFilename,
                                int pLoadSaveFormatChoice) throws IOException
{

    //create array to hold reference to data buffers for all map source boards
    short dataBuffers[][];
    dataBuffers = new short[mapSourceBoards.length][];
    for (int i = 0; i < dataBuffers.length; i++){
        dataBuffers[i] = mapSourceBoards[i].dataBuffer;
    }

    Integer startIndices[] = {0, 0, 0, 0};

    //get index of the last data point from each board
    Integer endIndices[];
    endIndices = new Integer[mapSourceBoards.length];
    copyLastDataPointIndicesFromMappingBoards(endIndices);

    //set up info to be saved in header

    Map<String, Object> headerInfo = new LinkedHashMap<>();

    //start/stop positions same for all mapping boards -- see Note 1 in header
    headerInfo.put("Start Inspection Location",
                           mapSourceBoards[0].utBoard.inspectionStartLocation);
    headerInfo.put("Stop Inspection Location",
                           mapSourceBoards[0].utBoard.inspectionStopLocation);

    headerInfo.put("Inspection Direction",
            settings.inspectionDirectionDescription);

    switch (pLoadSaveFormatChoice) {

        case SAVE_TO_TEXT:
            encoderValues.writeEncoderValuesToFile(pFilename, 
                                    settings.inspectionDirectionDescription);
            MapBufferFileDumpTools.saveAllDataBuffersToTextFiles(pFilename,
                            dataBuffers, startIndices, endIndices, headerInfo);
            break;

        case LOAD_FROM_TEXT:
            encoderValues.readEncoderValuesFromFile(pFilename);
            settings.inspectionDirectionDescription = 
                                            encoderValues.inspectionDirection;
            MapBufferFileDumpTools.loadAllDataBuffersFromTextFiles(pFilename,
                            dataBuffers, startIndices, endIndices, headerInfo);
            //new end indices determined by number of data read from file
            copyLastDataPointIndicesToMappingBoards(endIndices);
            break;

        case SAVE_TO_BINARY:

            break;

        case LOAD_FROM_BINARY:

            break;

        default:

            break;

    }

}//end of WallMapDataSaverTuboBinary::dumpAllDataBuffersToFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::copyLastDataPointIndicesFromMappingBoards
//
// Copies the index of the last data point in the data buffer from each
// mapping board to the pEndIndices array.
//
// NOTE: The end index actually points to the location after the last value.
//

private void copyLastDataPointIndicesFromMappingBoards(Integer pEndIndices[])
{

    for (int i = 0; i < pEndIndices.length; i++){
        pEndIndices[i] =
           mapSourceBoards[i].utBoard.getIndexOfLastDataPointInDataBuffer();
    }

}//end of WallMapDataSaverTuboBinary::copyLastDataPointIndicesFromMappingBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::copyLastDataPointIndicesToMappingBoards
//
// Copies the index of the last data point in the data buffer from the
// pEndIndices array to each mapping board.
//
// NOTE: The end index actually points to the location after the last value.
//

private void copyLastDataPointIndicesToMappingBoards(Integer pEndIndices[])
{

    for (int i = 0; i < pEndIndices.length; i++){
        mapSourceBoards[i].utBoard.setIndexOfLastDataPointInDataBuffer(
                                                               pEndIndices[i]);
    }

}//end of WallMapDataSaverTuboBinary::copyLastDataPointIndicesToMappingBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveToFile
//
// Writes all header data and the data in the UTBoards to file pFilename.
//

@Override
public void saveToFile(String pFilename)
{

    try{

        //collect job info into local variables
        setUpJobInfo();
        
        for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
            mapSourceBoard.setUpForSavingData();
        }

        //testing mks -- use this to save or load raw map data
        //if loading, set next tube number to same number used to save
        //and then run a short inspection (can be in simulation mode) and
        //the saved run will be loaded and used as if it came from a run

        //dumpOrLoadAllDataBuffersToFiles(pFilename, SAVE_TO_TEXT);        
        
        //dumpOrLoadAllDataBuffersToFiles(pFilename, LOAD_FROM_TEXT);

        //the above options save the encoders as well as the buffers, use this
        //line instead to just save the encoders
        //encoderValues.writeEncoderValuesToFile(pFilename, 
        //                        settings.inspectionDirectionDescription);

        //testing mks end

        outFile =
              new DataOutputStream(new BufferedOutputStream(
              new FileOutputStream(pFilename)));

        //find start/stop, count revolutions, repair missing control codes, etc.
        //must be done before saveHeader as the header needs some of that info

        analyzeAndRepairData();
        
        calculateDistanceInspectedAndAvgHelix();
                
        //save all header information
        saveHeader();

        //save all data from all revolutions
        saveRevolutions();

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 448");
    }
    finally{
        try{if (outFile != null) {outFile.close();}}
        catch(IOException e){}
    }

}//end of WallMapDataSaverTuboBinary::saveToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateDistanceInspectedAndAvgHelix
//
// Calculates the distance inspected from the head down signal to the head
// up signal.
//

private void calculateDistanceInspectedAndAvgHelix()
{
    
    int headUpPosition, headDownPosition;
    
    //choose encoder position and distances based on which head holds
    //leading sensor in "Go Home" direction
    
    if (mapSourceBoards[0].utBoard.headForMapDataSensor == 1){

        headUpPosition = encoderValues.encoderPosAtHead1UpSignal;
        headDownPosition = encoderValues.encoderPosAtHead1DownSignal;
        
    }else if (mapSourceBoards[0].utBoard.headForMapDataSensor == 2){

        headUpPosition = encoderValues.encoderPosAtHead2UpSignal;
        headDownPosition = encoderValues.encoderPosAtHead2DownSignal;
        
    }else{
        headUpPosition = encoderValues.encoderPosAtHead3UpSignal;
        headDownPosition = encoderValues.encoderPosAtHead3DownSignal;        
    }
    
    distanceInspected = 
        encoderValues.convertEncoder2CountsToInches(
                    Math.abs(headUpPosition) - Math.abs(headDownPosition));

    avgCalculatedHelix = distanceInspected / leastNumberOfRevs;
    
}//end of WallMapDataSaverTuboBinary::calculateDistanceInspectedAndAvgHelix
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::setDataBufferIsEnabled
//
// Sets the dataBufferedIsEnabled flag for each mapping board to pState
//

private void setDataBufferIsEnabled(boolean pState)
{
    
    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
        mapSourceBoard.utBoard.setDataBufferIsEnabled(pState);
    }

}//end of WallMapDataSaverTuboBinary::setDataBufferIsEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::setUpJobInfo
//
// Loads the job information and transfers it to local variables.
//

private void setUpJobInfo()
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

private void loadJobInfo()
{

    //if the ini file cannot be opened and loaded, exit without action and
    //default values will be used

    try {
        jobInfoFile = new IniFile(
            settings.currentJobPrimaryPath + "03 - " + settings.currentJobName
            + " Job Info.ini", settings.jobFileFormat);
        jobInfoFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 512");
        return;
    }

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

private void copyJobInfoToLocalVariables()
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

    fMotionPulseLen = (float)0.5; //need to read this from config file

    //debug mks -- needs to be extracted from config info or such

    fChnlOffset[0] = (float)  0;
    fChnlOffset[1] = (float)  .75;
    fChnlOffset[2] = (float) 1.5;
    fChnlOffset[3] = (float) 2.25;

    //these are calculated later
    nHomeXOffset = 0;
    nAwayXOffset = 0;
    nStopXloc = 0;

    nJointNum.value = settings.pieceNumberToBeSaved;
    
    fWall = (float)settings.nominalWall;
    fOD = (float)parseDiameterFromJobInfoString();

    nNumRev.value = 0; //set later after revolutions are counted

    nMotionBus.value = 0; //set later when header is saved

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

private boolean isNumerical(char pInput)
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
    EndianTools.writeFloatLE(nominalWall, outFile);
    EndianTools.writeFloatLE(OD, outFile);
    location.write(outFile);
    wellName.write(outFile);
    date.write(outFile);
    range.write(outFile);
    wellFoot.write(outFile);
    outFile.writeByte(wallStatFlag);
    rbNum.write(outFile);
    spare.write(outFile);
    EndianTools.writeFloatLE(fMotionPulseLen, outFile);
    for(int i = 0; i < fChnlOffset.length; i++){
        EndianTools.writeFloatLE(fChnlOffset[i], outFile);
    }
    
    //calculate nHomeXOffset, nAwayXOffset, nStopXloc
    calculateEndOfInspectionDistances();

    outFile.writeShort(Short.reverseBytes(nHomeXOffset));
    outFile.writeShort(Short.reverseBytes(nAwayXOffset));
    outFile.writeInt(Integer.reverseBytes(nStopXloc));

    //set the inspection direction flag    
    nMotionBus.value = 0;
    nMotionBus.value |= parseInspectionDirectionFlagFromJobInfoString();
    
    nJointNum.write(outFile);
    EndianTools.writeFloatLE(fWall, outFile);
    EndianTools.writeFloatLE(fOD, outFile);
    nNumRev.value = leastNumberOfRevs;
    nNumRev.write(outFile);
    nMotionBus.write(outFile);

}//end of WallMapDataSaverTuboBinary::saveHeader
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateEndOfInspectionDistances
//
// Calculates the various distances for the transducers using the encoder
// position values at the end of inspection as required for the Tubo binary
// file.
//
// All values are calculated for both directions, even though not all of the
// values are used by the viewer program for both directions. The unused
// values will be ignored.
//
// For the "Go Away From Home" direction:
//      on pipe occurs when eye1 reaches tube
//      off pipe occurs when eye2 leaves tube
//
// For the "Go Home" direction:
//      on pipe occurs when eye2 reaches tube
//      off pipe occurs when eye1 leaves tube
//
// nHomeXOffset: distance from the end of the pipe to Probe 4 where inspection
//               starts for the "Go Away from Home" direction
//
// nAwayXOffset: distance from the end of the pipe to Probe 1 where inspection
//              ends for the "Go Toward Home" direction
//
// nStopXloc: for direction "Go Toward Home" is location of the head at the 
//            moment the head is picked up from the pipe to stop inspection.
//            nStopXloc generally is equal to the longitudinal location of the
//            last revolution if the last revolution is not truncated.
//              (this would be probe 1?)
//            Per Yan Ming, this value can be nearly anything. The program
//            uses nAwayXOffset and the position recorded for each revolution
//            to calculate the location of any point.
//

private void calculateEndOfInspectionDistances()
{

    //--- calculate nHomeXOffset -- used for "Go Away From Home" direction
    // photo eye 1 detects on-pipe for this direction so use it for distances
    
    int encoderPosAtHeadDownSignal;
    double photoEye1DistanceFrontOfHead;
    
    //choose encoder position and distances based on which head holds
    //leading sensor in "Go Away From Home" direction
    if (mapSourceBoards[3].utBoard.headForMapDataSensor == 1){
        encoderPosAtHeadDownSignal = encoderValues.encoderPosAtHead1DownSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead1;
    }else if (mapSourceBoards[3].utBoard.headForMapDataSensor == 2){
        encoderPosAtHeadDownSignal = encoderValues.encoderPosAtHead2DownSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead2;        
    }else {
        encoderPosAtHeadDownSignal = encoderValues.encoderPosAtHead3DownSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead3;        
    }    
    
        
    //calculate position in inches of the leading mapping transducer from the
    //end of the tube when its head begins inspection for "Go Away From Home"
    //direction
    
    double homeXOffset = 
              encoderValues.convertEncoder2CountsToInches(
                                                    encoderPosAtHeadDownSignal)
            - encoderValues.convertEncoder2CountsToInches(
                                        encoderValues.encoderPosAtOnPipeSignal)
            - photoEye1DistanceFrontOfHead
            - mapSourceBoards[3].utBoard.distanceMapSensorToFrontEdgeOfHead;
    
    //correct for offset built into the viewer program
    //(yes, "home" offset variable is used with "AWAY" offset correction)
    homeXOffset += GOING_AWAY_TUBO_OFFSET_CORRECTION;

    //convert to motion pulses
    nHomeXOffset = (short)(homeXOffset / fMotionPulseLen);
        
    //--- calculate nAwayXOffset -- used for "Go Home" direction
    // encoder counts are negative for this direction so use absolute value
    // photo-eye 1 detects off-pipe for this direction so use it for distances
    
    int encoderPosAtHeadUpSignal;
    
    //choose encoder position and distances based on which head holds
    //leading sensor in "Go Home" direction
    if (mapSourceBoards[0].utBoard.headForMapDataSensor == 1){
        encoderPosAtHeadUpSignal = encoderValues.encoderPosAtHead1UpSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead1;
    }else if (mapSourceBoards[0].utBoard.headForMapDataSensor == 2) {
        encoderPosAtHeadUpSignal = encoderValues.encoderPosAtHead2UpSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead2;        
    }else {
        encoderPosAtHeadUpSignal = encoderValues.encoderPosAtHead3UpSignal;
        photoEye1DistanceFrontOfHead = 
                                    encoderValues.photoEye1DistanceFrontOfHead3;        
    }
    
    double awayXOffset =
              encoderValues.convertEncoder2CountsToInches(
                   Math.abs(encoderValues.encoderPosAtOffPipeSignal))
            - encoderValues.convertEncoder2CountsToInches(
                                            Math.abs(encoderPosAtHeadUpSignal))
            - photoEye1DistanceFrontOfHead
            - mapSourceBoards[0].utBoard.distanceMapSensorToFrontEdgeOfHead;

    //correct for distance added during PLC/eye deglitch delay
    awayXOffset += GOING_HOME_PLC_OFFSET_CORRECTION;
    
    //correct for offset built into the viewer program
    //(yes, "away" offset variable is used with "HOME" offset correction)        
    awayXOffset += GOING_HOME_TUBO_OFFSET_CORRECTION;
        
    //convert to motion pulses
    nAwayXOffset = (short)(awayXOffset / fMotionPulseLen);
    
    //--- calculate nStopXloc -- used for "Go Home" direction
    
    double stopXloc = distanceInspected;
    
    //convert to motion pulses
    nStopXloc = (short)(stopXloc / fMotionPulseLen);
    
}//end of WallMapDataSaverTuboBinary::calculateEndOfInspectionDistances
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::saveRevolutions
//
// Saves all revolutions to the file.
//

private void saveRevolutions() throws IOException
{

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

    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
        //the number of samples will vary between the boards, the max number
        //from any board is used instead with missing samples in the other
        //boards filled at write time

        countWord.value = mostNumberOfSamplesPerRev;

        countWord.write(outFile);
    }

    //this is the location of the current revolution measured in number of
    // "motion pulses" where each pulse equals so many inches

    nXloc = (short)(pRevolutionNumber * avgCalculatedHelix / fMotionPulseLen);
    outFile.writeShort(Short.reverseBytes(nXloc));

    //not used
    nMotionBusNotUsed.write(outFile);

    //not used
    for (int i = 0; i < fCrossArea.length; i++){
        EndianTools.writeFloatLE(fCrossArea[i], outFile);
    }

    writeWallReadingsForRevolution(pRevolutionNumber);

}//end of WallMapDataSaverTuboBinary::saveRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeWallReadingsForRevolution
//
// Saves data from one revolution from all boards to the file.
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

    //write entire block -- TUBO_BINARY_FORMAT always stores the entire size of
    //the maximum allowable -- REVOLUTION_SAMPLES_BLOCK_SIZE, padding with
    //zeroes if the number of samples is actually less than the maximum
    
    WORD sampleSlice[]; //holds one sample from each channel
    sampleSlice = new WORD[mapSourceBoards.length];
    for (int i = 0; i<sampleSlice.length; i++){
        sampleSlice[i] = new WORD(0);
    }
        
    fillerWord.value = 0;

    for(int i =0; i < NUMBER_SLICES_PER_REV; i++){

        writeSliceToOutFile(i, sampleSlice);
        
    }        

}//end of WallMapDataSaverTuboBinary::writeWallReadingsForRevolution
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeSliceToOutFile
//
// Writes one real data or padding sample slice to outFile. A slice contains
// one sample from each channel.
//
// If pSlice is less than the number of samples in the channel with the greatest
// number of sample in the revolution, then real sample data is written.
//
// After pSlice passes the largest number of samples, padding values are
// written to fill out the data block.
//

private void writeSliceToOutFile(int pSlice, WORD[] pValues) throws IOException
{
    
    if(pSlice < mostNumberOfSamplesPerRev){
        writeSampleSliceToOutFile(pSlice, pValues);
    }
    else{
        writePaddingSliceToOutFile(pValues);
    }

}//end of WallMapDataSaverTuboBinary::writeSliceToOutFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeSampleSliceToOutFile
//
// Writes one real data sample slice to outFile. A slice contains one sample
// from each channel.
//
// Every other slice is actually written as a tweaked duplicate. See note
// below:
//
// Sample Double Simulation Note 1:
//
// Currently, the program duplicates (with a tweak) each value to simulate
// double samples. When this feature is removed, also remove doubling of the
// sample count value (search for "sample count doubling to be removed").
//
// This can be done when Rabbit code is changed to use data from both cores
// See Git Gui commit tag Double_Sample_Rate_Emulation for all changes which
// need to be undone to remove the doubling functionality.
//

private void writeSampleSliceToOutFile(int pSplice, WORD[] pValues)
                                                            throws IOException
{

    for (int j=0; j<mapSourceBoards.length; j++) {
        
        if (mapSourceBoards[j].sampleIndex 
                                    < mapSourceBoards[j].revEndIndex) {

            //alternate real samples with tweaked copies
            //sample count doubling to be removed at later time
            
            if ((pSplice % 2) == 0){
                translateSample(j, pValues);
            }
            else{
                createTweakedSample(j, pValues);
            }
            
        }
        else{            
            //after end of data reached in a channel, leave data point as
            //the last valid point to pad so that each channel has the same
            //number of samples per revolution            
        }
        
    }//for (int j=0...
        
    //write one slice to file (one sample value for each channel)
    writeArrayOfWordsToOutFile(pValues);
    
}//end of WallMapDataSaverTuboBinary::writeSampleSliceToOutFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::translateSample
//
// Extracts the next sample from the mapSourceBoard denoted by pIndex,
// converts it to the appropriate format, and stores it in the matching slot
// of pValues.
// 

private void translateSample(int pIndex, WORD[] pValues)
{        

    int TOF = mapSourceBoards[pIndex].
            dataBuffer[mapSourceBoards[pIndex].sampleIndex++];
    double lWall = (TOF * 0.015 * .233) / 2;
    pValues[pIndex].value = (int)(lWall * 1000);    
        
}//end of WallMapDataSaverTuboBinary::translateSample
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::createTweakedSample
//
// Uses the sample already in the pIndex slot of pValues to create a slightly
// tweaked copy which is stored back in the same slot.
//
// Search for "Sample Double Simulation Note 1:" for more info.
//

private void createTweakedSample(int pIndex, WORD[] pValues)
{        

    if ((pValues[pIndex].value & 0x02) != 0 ){
        pValues[pIndex].value ^= 0x01;
    }
    
}//end of WallMapDataSaverTuboBinary::createTweakedSample
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writePaddingSliceToOutFile
//
// Writes one padding value slice to outFile. A slice contains one padding
// sample for each channel.
//

private void writePaddingSliceToOutFile(WORD[] pValues)
                                                             throws IOException
{

    for (WORD slice : pValues){
        slice.value = PADDING_VALUE;
    }
    
    //write one slice to file (one padding value for each channel)
    writeArrayOfWordsToOutFile(pValues);

}//end of WallMapDataSaverTuboBinary::writePaddingSliceToOutFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeArrayOfWordsToOutFile
//
// Writes all the words in pWords to outFile
//

private void writeArrayOfWordsToOutFile(WORD[] pValues) throws IOException
{
    
    for (WORD value : pValues) {
        value.write(outFile);
    }
    
}//end of WallMapDataSaverTuboBinary::writeArrayOfWordsToOutFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeArrayOfMassagedWordsToOutFile
//
// Writes all the words in pWords to outFile after tweaking them.
//
// NOTE: This method to be obsoleted some day. See note below.
//
// Sample Double Simulation Note 1:
//
// Currently, the program writes each value twice to simulate double samples.
// When this feature is removed, also remove doubling of the the sample count
// value (search for "sample count doubling to be removed").
//
// This can be done when Rabbit code changed to use data from both cores
// See Git Gui commit tag Double_Sample_Rate_Emulation for all changes which
// need to be undone to remove the doubling functionality.
//

private void writeArrayOfMassagedWordsToOutFile(WORD[] pValues)
                                                        throws IOException
{
    
    for (WORD value : pValues) {
        value.write(outFile);
    }
    
}//end of WallMapDataSaverTuboBinary::writeArrayOfMassagedWordsToOutFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::writeWORDToOutFile
//
// Writes one WORD to outFile.
//
// Sample Double Simulation Note 1:
//
// Currently, the program writes each value twice to simulate double samples.
// When this feature is removed, also remove doubling of the the sample count
// value (search for "sample count doubling to be removed").
//
// This will be done when Rabbit code changed to use data from both cores
// See Git Gui commit tag Double_Sample_Rate_Emulation for all changes which
// need to be undone to remove the doubling functionality.
//

private void writeWORDToOutFile(WORD pValue) throws IOException
{

    pValue.write(outFile);

    //remove this when Rabbit code changed to use data from both cores
    //see "Sample Double Simulation Notes" 1,2,3 in this file
    //write value again -- flipping bit 0 if bit 1 is set
    //note that zero filler values will be unchanged

    if ((pValue.value & 0x02) != 0 ){
        pValue.value ^= 0x01;
    }

    pValue.write(outFile);

    //end of remove this

}//end of WallMapDataSaverTuboBinary::writeWORDToOutFile
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

    //split any extra large revolutions in two with a TDC code
    repairMissingTDCCodesForAllBoards();
    
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
// Since the tracking markers are disabled from the Control Board until the
// inspection starts, the first control code in the map data will be the first
// TDC signal code stored. Thus, searching for the first control code of any
// type should find the start of the data.
//

private void findStartStopInspectionIndices()
{
    
    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {

        int startIndex, stopIndex;
        startIndex = findControlCode(mapSourceBoard, UTBoard.MAP_ANY_FLAG, 0,
                                                            Integer.MAX_VALUE);
        mapSourceBoard.inspectionStartIndex = startIndex;
        //this is also the start location for the first revolution
        mapSourceBoard.revStartIndex = startIndex;
        stopIndex = findControlCode(mapSourceBoard, UTBoard.MAP_STOP_CODE_FLAG,
                                            startIndex + 1, Integer.MAX_VALUE);
        mapSourceBoard.inspectionStopIndex = stopIndex;
    }

}//end of WallMapDataSaverTuboBinary::findStartStopInspectionIndices
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::repairMissingTDCCodesForAllBoards
//
// Fills in missing TDC codes for all mapping boards. See note in
// repairMissingTDCCodes for more info.
//

private void repairMissingTDCCodesForAllBoards()
{

    //do a preliminary count before repair to calculate a rough average number
    //of samples per revolution; a new count will be required after TDC code
    //repair as that adds more revolutions
    
    calculateRevCount(); //counts revs for all boards

    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
        calculateAvgNumSamplesPerRev(mapSourceBoard);
        repairMissingTDCCodes(mapSourceBoard);
    }
    
}//end of WallMapDataSaverTuboBinary::repairMissingTDCCodesForAllBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateAvgNumSamplesPerRev
//
// Calculates the average number of samples in each revolution for 
// pMapSourceBoard.
//
// NOTE: Method calculateRevCount should already have been called.
//

private void calculateAvgNumSamplesPerRev(MapSourceBoard pMapSourceBoard)
{
    
    if (pMapSourceBoard.numRevs <= 0){
        pMapSourceBoard.avgNumSamplesPerRev = 0;
        return;        
    }
    
    //get total number of samples in the board's buffer; this will also
    //include the control codes, but they are few enough to be ignored for the
    //purpose of obtaining a rough average
    
    int totalNumSamples = pMapSourceBoard.inspectionStopIndex
                                    -  pMapSourceBoard.inspectionStartIndex;
    
    if (totalNumSamples <= 0){
        pMapSourceBoard.avgNumSamplesPerRev = 0;
        return;
    }
        
    pMapSourceBoard.avgNumSamplesPerRev = 
                                totalNumSamples / pMapSourceBoard.numRevs;

}//end of WallMapDataSaverTuboBinary::calculateAvgNumSamplesPerRev
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::repairMissingTDCCodes
//
// Scan the data file for pMapSourceBoard for revolutions which are
// approximately twice the average size and insert a TDC code halfway to split
// the revolution into two. It is assumed that for those oversized revolutions
// that a TDC signal was missed by the UT board.
//
// NOTE: Method calculateAvgNumSamplesPerRev should already have been called.
//

private void repairMissingTDCCodes(MapSourceBoard pMapSourceBoard)
{

    if (pMapSourceBoard.avgNumSamplesPerRev <= 0){
        return;        
    }
    
    int sampleIndex;
    int prevStart;
    int start = pMapSourceBoard.inspectionStartIndex;
    int stop = pMapSourceBoard.inspectionStopIndex;
    int numSamples;
    
    //if a revolution has signicantly more than average number of samples,
    //it will be split -- calculate the trigger level using the average
    int triggerLevel = 
                    (int)(pMapSourceBoard.avgNumSamplesPerRev * 1.75);
        
    //check the sample count for each rev, using findControlCode to find the
    //end of each rev

    while((sampleIndex = findControlCode(pMapSourceBoard,
                                UTBoard.MAP_ANY_FLAG, start, stop)) != -1) {

        numSamples = sampleIndex - start;

        if (numSamples > triggerLevel){                
            splitRevWithTDCCode(pMapSourceBoard, start, sampleIndex);
        }

        start = sampleIndex + 1;

    }//while

}//end of WallMapDataSaverTuboBinary::repairMissingTDCCodes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::splitRevWithTDCCode
//
// In the map buffer for pMapSourceBoard, overwrites the sample in the center
// of the revolution bounded by pStart and pStop with a TDC code. The
// overwritten sample is compared with the following sample and that sample
// position is overwritten with the smaller of the two in order to preserve the
// worst case signal.
//

private void splitRevWithTDCCode(MapSourceBoard pMapSourceBoard, 
                                                        int pStart, int pStop)
{

    //ignore small revolutions
    if ((pStop - pStart) < 10) {return;}
    
    //find the sample index in the middle of the revolution
    int midPoint = (pStart + pStop) / 2;
    
    //keep the smaller of the midpoint/midpoint+1 values so no worst case
    //data point is lost
    
    if (pMapSourceBoard.dataBuffer[midPoint] <
                                    pMapSourceBoard.dataBuffer[midPoint+1]){
        
        pMapSourceBoard.dataBuffer[midPoint+1] = 
                                        pMapSourceBoard.dataBuffer[midPoint];
    }

    //insert the TDC code over the midpoint sample
    pMapSourceBoard.dataBuffer[midPoint] = (short)UTBoard.MAP_CONTROL_CODE_FLAG;
    
}//end of WallMapDataSaverTuboBinary::splitRevWithTDCCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::calculateRevCount
//
// Calculates the number of revolutions for each board. Stores the count in
// each board, stores the least number of revs of any of the boards in
// leastNumberOfRevs and the average number of revs in avgNumberOfRevs.
//
// Count drops one revolution -- don't want to use last rev as it may be corrupt
// due to head liftoff. First rev should be good as its already delayed after
// head drop.
//

private void calculateRevCount()
{

    leastNumberOfRevs = Integer.MAX_VALUE;
    avgNumberOfRevs = 0;
    int index;
        
    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
        
        int start = mapSourceBoard.inspectionStartIndex;
        int stop = mapSourceBoard.inspectionStopIndex;
        int revCount = 0;
        
        //look for all control codes between the start and stop points
        while ((index = findControlCode(mapSourceBoard, UTBoard.MAP_ANY_FLAG,
                                                        start, stop)) != -1) {
            revCount++;
            start = index + 1;
        }
        //drop one from the count to ignore the last rev
        if (revCount > 0) { revCount--; }
        mapSourceBoard.numRevs = revCount;
        //trap value of the smallest rev count of all the boards
        if (revCount < leastNumberOfRevs) { leastNumberOfRevs = revCount; }
        avgNumberOfRevs += revCount;
    }

    //calculate the average number of revolutions across all the channels
    avgNumberOfRevs = avgNumberOfRevs / mapSourceBoards.length;

}//end of WallMapDataSaverTuboBinary::calculateRevCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::findControlCode
//
// Finds the next occurrance of the control code with pFlag set for 
// pMapSourceBoard. Only control codes (those values with MAP_CONTROL_CODE_FLAG
// bit set) will be checked.
//
// Searching will begin at pStart and end at pEnd - 1. If pEnd is set to
// Integer.MAX_VALUE, the search will end at buffer end - 1.
//
// Returns index of first occurrance of pFlag between pStart and pEnd - 1.
// Returns -1 if the code is not found.
//

private int findControlCode(MapSourceBoard pMapSourceBoard, int pFlag,
                                                        int pStart, int pEnd)
{

    short []dataBuffer = pMapSourceBoard.dataBuffer;

    //bail out for invalid pStart
    if (pStart < 0 || pStart > dataBuffer.length) { return(-1); }

    //catch MAX_VALUE -- special signal to search to end of buffer
    if (pEnd == Integer.MAX_VALUE) { pEnd = dataBuffer.length; }

    //only look at values with the MAP_CONTROL_CODE_FLAG bit set as well
    int target = UTBoard.MAP_CONTROL_CODE_FLAG | pFlag;

    for (int i = pStart; i < pEnd; i++){
        if((dataBuffer[i] & target) == target) {
            return(i); }
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
    
    for (MapSourceBoard mapSourceBoard : mapSourceBoards) {
        
        //revIndex points at the control code, skip past to first value
        int start = mapSourceBoard.revStartIndex + 1;
        int stop = Integer.MAX_VALUE;
        //the first sample in rev will be first value after control code
        mapSourceBoard.sampleIndex = start;
        //look for the next control code -- that's the end of the revolution
        index = findControlCode(mapSourceBoard,
                                        UTBoard.MAP_ANY_FLAG, start, stop);
        //store end of current revolution
        mapSourceBoard.revEndIndex = index;
        //preset to end of this revolution (start of next) for next time
        mapSourceBoard.revStartIndex = index;
        //calculate number of samples in the revolution
        sampleCount = index - start - 1;
        mapSourceBoard.numSamplesInRev = sampleCount;
        //trap value of the largest sample count of all the boards for this
        //revolution
        if (sampleCount > mostNumberOfSamplesPerRev) {
            mostNumberOfSamplesPerRev = sampleCount;
        }   avgNumberOfSamplesPerRev += sampleCount;
    }

    //calculate the average
    avgNumberOfSamplesPerRev = avgNumberOfSamplesPerRev/mapSourceBoards.length;

    //remove this when Rabbit code modified to use both cores
    //Sample Double Simulation Note 2
    // The value is doubled as the method writing samples writes a
    // dummy sample for every actual sample to simulate double sampling
    // rate -- search for "Sample Double Simulation Note 1" and remove
    // double writing when this part is removed.

    mostNumberOfSamplesPerRev *= 2;

    // end of remove this

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
        logSevere(e.getMessage() + " - Error: 1146");
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
    nominalWall = EndianTools.readFloatLE(inFile);
    OD = EndianTools.readFloatLE(inFile);
    location.read(inFile);
    wellName.read(inFile);
    date.read(inFile);
    range.read(inFile);
    wellFoot.read(inFile);
    wallStatFlag = inFile.readByte();
    rbNum.read(inFile);
    spare.read(inFile);
    fMotionPulseLen = EndianTools.readFloatLE(inFile);
    for(int i = 0; i < fChnlOffset.length; i++){
        fChnlOffset[i] = EndianTools.readFloatLE(inFile);
    }
    nHomeXOffset = Short.reverseBytes(inFile.readShort());
    nAwayXOffset = Short.reverseBytes(inFile.readShort());
    nStopXloc = Integer.reverseBytes(inFile.readInt());

    nJointNum.read(inFile);
    fWall = EndianTools.readFloatLE(inFile);
    fOD = EndianTools.readFloatLE(inFile);
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

    for (WORD nNumAscan1 : nNumAscan) {
            nNumAscan1.read(inFile);
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
        fCrossArea[i] = EndianTools.readFloatLE(inFile);
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
            for (WORD wall1 : wall) {
                wall1.read(inFile);
            }
        }
        else{
            for (WORD wall1 : wall) {
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

private void setUpTestData()
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

    fMotionPulseLen = (float)0.5;

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
// WallMapDataSaverTuboBinary::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of WallMapDataSaverTuboBinary::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaverTuboBinary::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of WallMapDataSaverTuboBinary::logStackTrace
//-----------------------------------------------------------------------------

}//end of class WallMapDataSaverTuboBinary
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
