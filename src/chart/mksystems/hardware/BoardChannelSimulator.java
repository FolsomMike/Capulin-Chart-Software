/******************************************************************************
* Title: BoardChannelSimulator.java
* Author: Mike Schoonover
* Date: 5/3/14
*
* Purpose:
*
* This class handles a simulated channel for a simulated board.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import static chart.mksystems.hardware.UTSimulator.SAMPLES_PER_REV;
import static chart.mksystems.hardware.UTSimulator.SAMPLE_COUNT_VARIATION;
import chart.mksystems.inifile.IniFile;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;

//-----------------------------------------------------------------------------
// class BoardChannelSimulator
//
// Simulates a single channel on a UT Board.
//

public class BoardChannelSimulator extends Object{

    int index, bufferSize;
    
    int sourceAGroupIndex;
    int sourceAChartIndex;
    int sourceATraceIndex;
    int sourceADataSetIndex;
    int sourceBGroupIndex;
    int sourceBChartIndex;
    int sourceBTraceIndex;
    int sourceBDataSetIndex;
    
    int [] bufferA;
    int [] bufferB;
    
    int bufferAIndex, bufferBIndex;
    int bufferADataEnd, bufferBDataEnd;

    int type;
    int simulationType;

    int mapChannel = -1;

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader mapSimDataFile = null;
    boolean mapSimDataError = false;
    
    int prevMapSimDataValue = 0;

    int tdcTracker = 0;
    int revCount = 0;
    int mapSampleCount = 0;
    short trackWord = 0;

    double nominalWall;
    double nSPerDataPoint;
    double uSPerDataPoint;
    double velocityUS;
    double compressionVelocityNS;
    int numWallMultiples;
    double inchesPerChartPercentagePoint;
    
    int revAnomalyStart = -1;
    int revAnomalyWidth = -1;
    int sampleAnomalyStart = -1;
    int sampleAnomalyHeight = -1;
    short anomalyThickness = -1;
    
    int delayCount; //number of samples to skip for delay - set by Host
    byte delayCount0, delayCount1, delayCount2, delayCount3;

    int sampleCount; //number of samples to record - set by Host
    byte sampleCount0, sampleCount1, sampleCount2;

    int dspGain;

    byte sampleDelayReg0;
    byte sampleDelayReg1;
    byte sampleDelayReg2;
    byte sampleDelayReg3;

    byte sampleCountReg0;
    byte sampleCountReg1;
    byte sampleCountReg2;

    final DecimalFormat dataSetIndexFormat = new DecimalFormat("0000000");    
    
//-----------------------------------------------------------------------------
// BoardChannelSimulator::BoardChannelSimulator (constructor)
//

public BoardChannelSimulator(
                            int pIndex, int pBufferSize, int pType,
                            int pSimulationType,
                            byte pSampleDelayReg0, byte pSampleCountReg0,
                            int pMapChannel)
{

    index = pIndex;    
    bufferSize = pBufferSize;
    type = pType; simulationType = pSimulationType;
    mapChannel = pMapChannel;
    
    // the FPGA register addresses for all channels for sample delay and sample
    // count are contiguous addresses, so use a bit of math to calculate each
    // address from the first one

    sampleDelayReg0 = (byte) (pSampleDelayReg0 + (index * 4));
    sampleDelayReg1 = (byte) (pSampleDelayReg0 + 1 + (index * 4));
    sampleDelayReg2 = (byte) (pSampleDelayReg0 + 2 + (index * 4));
    sampleDelayReg3 = (byte) (pSampleDelayReg0 + 3 + (index * 4));

    sampleCountReg0 = (byte) (pSampleCountReg0 + (index * 3));
    sampleCountReg1 = (byte) (pSampleCountReg0 + 1 + (index * 3));
    sampleCountReg2 = (byte) (pSampleCountReg0 + 2 + (index * 3));

}//end of BoardChannelSimulator::BoardChannelSimulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

    if(bufferSize > 0) {
        bufferA = new int[bufferSize];
        bufferB = new int[bufferSize];
    }

    tdcTracker = calculateTimeToNextTDC(SAMPLES_PER_REV);    
    
}//end of BoardChannelSimulator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::setSimulationType
//
// Sets value of simulationType to pValue.
//

public void setSimulationType(int pValue)
{

    simulationType = pValue;
    
}//end of BoardChannelSimulator::setSimulationType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::prepareNextSimulationDataSetFromFiles
//
// Loads the next set of simulation data from pTraceSimSourceFile and opens
// files for data sets which are not preloaded but are loaded on the fly, such
// as Map data.
//

public void prepareNextSimulationDataSetFromFiles(
    String pPath, int pCurrentDataSetIndex, BufferedReader pTraceSimSourceFile)
{

    prepareNextTraceSimulationDataSetFromFiles(pTraceSimSourceFile);
    
    //open files, load data, etc for the map
    prepareNextMapSimulationDataSetFromFiles(pPath, pCurrentDataSetIndex);

}//end of BoardChannelSimulator::prepareNextSimulationDataSetFromFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::prepareNextTraceSimulationDataSetFromFiles
//
// Loads the next set of simulation data for a Trace from pSourceFile.
//
// bufferADataEnd/bufferBDataEnd will be set to index of the last data
// point stored in each buffer.
//

public void prepareNextTraceSimulationDataSetFromFiles(
                                                BufferedReader pSourceFile)
{

    bufferAIndex = 0; bufferBIndex = 0;
    bufferADataEnd = 0; bufferADataEnd = 0;
    
    bufferADataEnd = 
            loadDataSetFromFile(pSourceFile, bufferA, sourceAGroupIndex, 
                    sourceAChartIndex, sourceATraceIndex, sourceADataSetIndex);

    //set Group and Chart index to -2 so they won't be searched for as they
    //have already been found by the previous call
    
    bufferBDataEnd =
            loadDataSetFromFile(pSourceFile, bufferB, -2, 
                    -2, sourceBTraceIndex, sourceBDataSetIndex);
    
}//end of BoardChannelSimulator::prepareNextTraceSimulationDataSetFromFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::loadDataSetFromFile
//
// Loads the specified Data Set of simulation data int pBuffer from pSourceFile.
//
// If the associated index is set to -2, the search for a Group, Chart, or
// Trace section will be skipped. This is done when searching for the next
// Trace when the Chart has already been found by the previous call for another
// load.
//
// Returns the index of the last data stored in the buffer.
//

public int loadDataSetFromFile(BufferedReader pSourceFile, int []pBuffer,
        int pGroupIndex, int pChartIndex, int pTraceIndex, int pDataSetIndex)
{
    
    for (int i=0; i<pBuffer.length; i++){
        pBuffer[i] = 0;
    }

    int retCode;
    
    retCode = findSectionWithIndex(
         pSourceFile, "[Chart Group]", "Chart Group Index", pGroupIndex);

    if (retCode == -1){ return(0); }
    
    retCode = findSectionWithIndex(
                     pSourceFile, "[Chart]", "Chart Index", pChartIndex);
    
    if (retCode == -1){ return(0); }    
    
    retCode = findSectionWithIndex(
                     pSourceFile, "[Trace]", "Trace Index", pTraceIndex);

    if (retCode == -1){ return(0); }
    
    retCode = findSectionWithIndex(
                pSourceFile, "[Data Set " + pDataSetIndex + "]", "", -1);
    
    if (retCode == -1){ return(0); }
    
    //load the data from the data set and return index of last value
    return(loadDataSetFromFilePosition(pSourceFile, pBuffer));
    
}//end of BoardChannelSimulator::loadDataSetFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::loadDataSetFromFilePosition
//
// Loads the specified Data Set of simulation data int pBuffer from the
// current position in pSourceFile.
//
// Reads data from pSourceFile, converts to ints and stores in pBuffer until
// the next section tag is reached, a conversion error occurs, or 
// end-of-file reached.
//
// Normally the next section tag will be "[End of Set]".
//
// Returns the index of the last data stored in the buffer.
// Returns -1 if no data stored in buffer at all.
//

public int loadDataSetFromFilePosition(BufferedReader pSourceFile,
                                                                int []pBuffer)
{

    String line;
    int i = 0;
    
    try{

        while ((line = pSourceFile.readLine()) != null){

            //exit when next section tag reached
            if (line.toLowerCase().startsWith("[")){ return(i-1); }
        
            //convert and store in buffer
            pBuffer[i] = Integer.parseInt(line);
            
            i++; //inc here to end up with correct end point on format error

            //stop loading if end of buffer reached
            if (i == pBuffer.length) { return(i-1); }

        }
    }
    catch(IOException | NumberFormatException e){
        return(i-1);
    }

    return(i-1);
    
}//end of BoardChannelSimulator::loadDataSetFromFilePosition
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::prepareNextMapSimulationDataSetFromFiles
//
// Prepares the next map data set for use by opening files and/or reading
// data as required. Some files are left open so that data can be read on the
// fly as the simulation is progressing.
//
// NOTE: BufferedReader.mark should NOT be used to mark the start point of the
// data as the data block is quite large. The data block size must be specified
// to the mark method which will cause the input buffer to be resized to match
// this large number which would require excessive memory. To reset to the
// beginning of the data, the file must be closed, reopened and then searched
// again.
//

public void prepareNextMapSimulationDataSetFromFiles(
                                        String pPath, int pCurrentDataSetIndex)
{

    if (type != UTBoard.WALL_MAPPER){ return; }

    //if open, close the data file in current use
    closeMapSimDataFile();
        
    String mapSimDataFileName = 
                  createMapSimulationDataFilename(pPath, pCurrentDataSetIndex);    

    try{

        fileInputStream = new FileInputStream(mapSimDataFileName);
        inputStreamReader = new InputStreamReader(fileInputStream);

        mapSimDataFile = new BufferedReader(inputStreamReader);
                
    }        
    catch (FileNotFoundException e){
        //on error, force simulation type to RANDOM
        simulationType = Simulator.RANDOM;
        return;
        }

    //skip to the data section
    int retCode = 
                findSectionWithIndex(mapSimDataFile, "[Data Set 1]", "", -1);
    
    //on error or failure to find section, force type to RANDOM
    if (retCode == -1) {
        closeMapSimDataFile();
        simulationType = Simulator.RANDOM;
    }

}//end of BoardChannelSimulator::prepareNextMapSimulationDataSetFromFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::createMapSimulationDataFilename
//
// Creates a filename to load a simulation Map data set file using pPath,
// the current data set index pCurrentDataSetIndex, and the supplied prefix and
// suffix.
//

protected String createMapSimulationDataFilename(
                                        String pPath, int pCurrentDataSetIndex)
{
 
    return(pPath + "20 - "
          + dataSetIndexFormat.format(pCurrentDataSetIndex)
          + " map.dat ~ Wall Mapping Data ~ " + mapChannel + ".dat");
    
}//end of BoardChannelSimulator::createMapSimulationDataFilename
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::closeMapSimDataFile
//
// If open, closes the map simulation data file and sets all related references
// to null.
//

private void closeMapSimDataFile()
{
 
    try{
        if (mapSimDataFile != null) {
            mapSimDataFile.close(); mapSimDataFile = null;
            mapSimDataError = false;
        }
    }
    catch(IOException e){}
    try{
        if (inputStreamReader != null) {
            inputStreamReader.close(); inputStreamReader = null;
        }
    }
    catch(IOException e){}
    try{
        if (fileInputStream != null) {
            fileInputStream.close(); fileInputStream = null;
        }
    }
    catch(IOException e){}    

}//end of BoardChannelSimulator::closeMapSimDataFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::findSectionWithIndex
//
// Searches forward from the current location in pSourceFile for the section
// pSection (stored in the file as "[***]" where *** is the section name) with
// index number of pIndex. The key paired with the index number is passed via
// pIndexKey. If pIndexKey is empty, the next matching section will be found,
// ignoring its index number.
//
// The index number key/value pair is expected to be the next line after the
// section name, ignoring blank lines.
//
// If pIndex is -2, the function returns 1 as if the section was found. This
// is a convenience function for skipping sections already found.
//
// Returns:
// 1 if section is found with matching index
// -1 if error or end of file reached before section is found
//

public int findSectionWithIndex(BufferedReader pSourceFile, String pSection,
                                                  String pIndexKey, int pIndex)
{        

    //return as though section found if pIndex is -1
    if (pIndex == -2) return(1);
    
    String line;
    
    pSection = pSection.toLowerCase();
    
    try{

        //search for the section name
        while ((line = pSourceFile.readLine()) != null){

            //if line does not contain the section name, skip to next line
            if (!line.toLowerCase().startsWith(pSection)){ continue; }
        
            //ignore index if key is blank -- return on first matching section
            if (pIndexKey.equals("")){ return(1); }

            //next non-blank line should be the index number line
            line = readNextNonBlankLine(pSourceFile);

            if (line == null){ return(-1); } //end-of-file reached

            //return 1 if value of key/value pair matches index
            //otherwise, continue searching
            if(parseIntFromKeyValue(line) == pIndex){ return(1); }

        }
        
    }
    catch(IOException e){
        return(-1);
    }
    
    //end-of-file reached and section not found
    return(-1);
    
}//end of BoardChannelSimulator::findSectionWithIndex
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::readNextNonBlankLine
//
// Returns the next non-blank line in pSourceFile.
//
// Returns null if end of file reached or on error.
//

public String readNextNonBlankLine(BufferedReader pSourceFile)
                                                        throws IOException
{
 
    String line;
    
    while ((line = pSourceFile.readLine()) != null){
        if (!line.equals("")) {break;}
    }
    
    return(line);
    
}//end of BoardChannelSimulator::readNextNonBlankLine
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::parseIntFromKeyValue
//
// Extracts the value from the key/value pair in pKeyValue, converts it to an
// int and returns it.
//
// On error, returns Integer.MAX_VALUE.
//

public int parseIntFromKeyValue(String pKeyValue)
{
 
    int indexOfEqual;

    //look for '=' symbol, if not found then return error
    if ( (indexOfEqual = pKeyValue.indexOf("=")) == -1) {
        return(Integer.MAX_VALUE);
        }

    //convert text after the equal sign to an int and return it
    try{
        return(Integer.parseInt(pKeyValue.substring(indexOfEqual + 1)));
        }
    catch(NumberFormatException e){
        return(Integer.MAX_VALUE);
        }

}//end of BoardChannelSimulator::readNextNonBlankLine
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::readAndParseIntFromMapSimDataFile
//
// Reads the next line from mapSimDataFile, converts it to an int, and returns
// the value.
//
// On error, returns the previous value read at the time of the error and
// for every call thereafter...does not try to read any more lines after the
// first error.
//

public int readAndParseIntFromMapSimDataFile()
{
    
    if (mapSimDataError) { return(prevMapSimDataValue); }
    
    int value = 0;
    
    try{
        
        String line;
        
        if ((line = mapSimDataFile.readLine()) == null){
            mapSimDataError = true;
        }

        //convert text after the equal sign to an int and return it
        try{
            value = Integer.parseInt(line);
            }
        catch(NumberFormatException e){
            mapSimDataError = true;
            }
    }
    catch(IOException e){
        mapSimDataError = true;
    }
    finally{
        if (mapSimDataError) {
            value = prevMapSimDataValue;
        }                    
    }
    
    return(value);
    
}//end of BoardChannelSimulator::readAndParseIntFromMapSimDataFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMaxWallValue
//
// Returns the next simulated or file supplied maximum wall value.
//
// If simulationType is RANDOM, the value is generated randomly.
// If simulationType is FROM_FILE, value is from data loaded from a file.
//

public int getNextMaxWallValue()
{

    if(simulationType == UTSimulator.RANDOM){
        return(getNextMaxWallValueRandom());
    }
    else
    if(simulationType == UTSimulator.FROM_FILE){
        return(getNextMaxWallValueFromFile());
    }
    else{
        return(0);
    }
    
}//end of BoardChannelSimulator::getNextMaxWallValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMaxWallValueRandom
//
// Returns the next maximum wall value simulated via random number.
//

public int getNextMaxWallValueRandom()
{
 
    short wall;
    wall = (short)(convertInchesToSampleCounts(nominalWall * 1.05)
                                                        + (Math.random()*10));
    return(wall);
    
}//end of BoardChannelSimulator::getNextMaxWallValueRandom
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMaxWallValueFromFile
//
// Returns the next maximum wall value simulated with a value from buffer
// previously loaded from a file.
//
// When end of data reached, sequence starts over from beginning.
//

public int getNextMaxWallValueFromFile()
{

    int value = bufferA[bufferAIndex++];
    
    if (bufferAIndex >= bufferADataEnd) { bufferAIndex = 0; }
    
    return(convertChartPercentageToSampleCounts(value));
    
}//end of BoardChannelSimulator::getNextMaxWallValueFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMinWallValue
//
// Returns the next simulated or file supplied minimum wall value.
//
// If simulationType is RANDOM, the value is generated randomly.
// If simulationType is FROM_FILE, value is from data loaded from a file.
//

public int getNextMinWallValue()
{

    if(simulationType == UTSimulator.RANDOM){
        return(getNextMinWallValueRandom());
    }
    else
    if(simulationType == UTSimulator.FROM_FILE){
        return(getNextMinWallValueFromFile());
    }
    else{
        return(0);
    }

}//end of BoardChannelSimulator::getNextMinWallValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMinWallValueRandom
//
// Returns the next minimum wall value simulated via random number. An
// occasional down spike is added.
//

public int getNextMinWallValueRandom()
{
 
    int tof = convertInchesToSampleCounts(nominalWall * .95);
    
    short wall;
    wall = (short)(tof +(Math.random()*10));

    //occasional down spike
    if(((int)(Math.random()*200)) == 1) {
//debug mks         wall -= (int)(Math.random()* tof * .30);
    }
    
    return(wall);
    
}//end of BoardChannelSimulator::getNextMinWallValueRandom
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMinWallValueFromFile
//
// Returns the next minimum wall value simulated with a value from buffer
// previously loaded from a file.
//
// When end of data reached, sequence starts over from beginning.
//

public int getNextMinWallValueFromFile()
{

    int value = bufferB[bufferBIndex++];
    
    if (bufferBIndex >= bufferBDataEnd) { bufferBIndex = 0; }
    
    return(convertChartPercentageToSampleCounts(value));
    
}//end of BoardChannelSimulator::getNextMinWallValueFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextWallMapValue
//
// Returns the next simulated or file supplied wall map value.
//
// If simulationType is RANDOM, the value is generated randomly.
// If simulationType is FROM_FILE, value is from data loaded from a file.
//

public int getNextWallMapValue(int pWhichSimulation)
{

    if(simulationType == UTSimulator.RANDOM){
        return(getNextWallMapValueRandom(pWhichSimulation));
    }
    else
    if(simulationType == UTSimulator.FROM_FILE){
        return(getNextWallMapValueFromFile());
    }
    else{
        return(0);
    }
    
}//end of BoardChannelSimulator::getNextWallMapValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextWallMapValueRandom
//
// Returns the next wall map value simulated via random number.
//
// Uses one of multiple methods of generating simulated data. Pick one by the
// value of pWhichSimulation.
//

public short getNextWallMapValueRandom(int pWhichSimulation)
{
    
    boolean insertControlWord = false;
    
    //periodically, the tracking word gets incremented by the clock signal
    //and reset to zero by the TDC signal; at every reset, the value of
    //the word just before the reset is inserted into the data stream as
    //a control code to denote the start of a revolution; thus the tracking
    //value used in the control code is usually the last clock position
    //before a reset; the increment by clock signal is not simulated here,
    //just the reset by TDC

    if (tdcTracker-- == 0){
        tdcTracker = calculateTimeToNextTDC(SAMPLES_PER_REV);
        insertControlWord = true;
        revCount++; //count number of revolutions
        mapSampleCount = 0; //count starts over with each rev
        trackWord = (short)UTBoard.MAX_CLOCK_POSITION;
    }

        //send a control byte if needed or a data sample
        //control bytes have bit 15 set to distinguish from a data byte

        if (insertControlWord){
            //return a control word
            return((short)(trackWord | UTBoard.MAP_CONTROL_CODE_FLAG));
        }
        else {
            //return a data word
            
            mapSampleCount++; //count the samples sent            
            
            if (pWhichSimulation == 1){
                return(genSimdWMDPtShotGun());
            }
            else
            if (pWhichSimulation == 2){
                return(genSimdWMDPtCleanWithRectangles());
            }

        }
    
    return(0);

}//end of BoardChannelSimulator::getNextWallMapValueRandom
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::genSimdWMDPtShotGun
//
// Small random background variations, frequent spikes of higher variation.
//
// the data in the map is raw time-of-flight count (two way)
// speed of sound in steel = 0.233 inch/uS
// 15 nS per count
//

private short genSimdWMDPtShotGun()
{

    //convert wall in inches to number of samples
    //debug mks -- use value loaded from config file
    int tof = convertInchesToSampleCounts(.250);
    
    short wallDataPoint = (short)(tof - (tof * .05 * Math.random()));

    if ((int)(200 * Math.random()) == 1){
        wallDataPoint = (short)(tof - (tof *.20 * Math.random()));
    }

    return(wallDataPoint);

}//end of BoardChannelSimulator::genSimdWMDPtShotGun
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::genSimdWMDPtCleanWithRectangles
//
// Clean background with no variations, relatively few rectangular low spots.
//
// the data in the map is raw time-of-flight count (two way)
// speed of sound in steel = 0.233 inch/uS
// 15 nS per count
//

private short genSimdWMDPtCleanWithRectangles()
{

    //start with the typical background wall
    //debug mks -- use value loaded from config file
    int tof = convertInchesToSampleCounts(.250);
        
    short wallDataPoint = (short)(tof - (tof * .05 * Math.random()));

    //at random intervals, create a rectangular thin wall anomaly

    if (revAnomalyStart == -1 && ((int)(30000 * Math.random()) == 1)){

        //starts horizontally at the current revolution count
        revAnomalyStart = revCount;
        //spans a random width of revolutions
        revAnomalyWidth = 10 + (int)(10 * Math.random());
        //starts vertically at the current sample count
        sampleAnomalyStart = mapSampleCount;
        //spans a random number of samples
        sampleAnomalyHeight = 10 + (int)(10 * Math.random());
        //has a random thickness
        anomalyThickness =
                       (short)(tof - (tof * .20) - (tof *.10 * Math.random()));
    }

    //if anomaly generation is currently active, check to see if the revolution
    //and sample counts which define the boundaries are in valid range for
    //the anomaly's position -- if so, set value to anomaly thickness

    if (revAnomalyStart != -1){

        //if the revCount has passed the width of the anomaly, end generation
        if (revCount >= (revAnomalyStart + revAnomalyWidth)){
            revAnomalyStart = -1;
            return(wallDataPoint);
        }

        //check both rev count and sample count to see if anomaly data points
        //should be generated for that position

        if (revCount > revAnomalyStart
                &&
            revCount < (revAnomalyStart + revAnomalyWidth)
                &&
            mapSampleCount > sampleAnomalyStart
                &&
            mapSampleCount < (sampleAnomalyStart + sampleAnomalyHeight)){

            return(anomalyThickness);

        }
    }

    return(wallDataPoint);

}//end of BoardChannelSimulator::genSimdWMDPtCleanWithRectangles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextWallMapValueFromFile
//
// Returns the next wall map value simulated with a value from buffer
// previously loaded from a file.
//
// When end of data reached, sequence starts over from beginning.
//

public int getNextWallMapValueFromFile()
{

    return(readAndParseIntFromMapSimDataFile());
    
}//end of BoardChannelSimulator::getNextWallMapValueFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::setDelayCount
//
// Checks if pRegAddr matches one of the delayCount byte register addresses
// and stores pValue in that register if so.
//
// Concatenates all bytes set by the host computer to form the value for the
// delayCount.  Since the value might be illegal when only some of the bytes
// have been set, it is checked for out of bounds.
//

void setDelayCount(byte pRegAddr, byte pValue){

    //since the host computer writes to the variables one byte at a time in the
    //FPGA registers, the bytes are collected individually and then converted
    //to the full values - the values must be tested for out of bounds each time
    //as it could be illegal when only some of the bytes have been updated

    if (pRegAddr == sampleDelayReg0) {delayCount0 = pValue;}
    if (pRegAddr == sampleDelayReg1) {delayCount1 = pValue;}
    if (pRegAddr == sampleDelayReg2) {delayCount2 = pValue;}
    if (pRegAddr == sampleDelayReg3) {delayCount2 = pValue;}


    delayCount = (int)(
        ((delayCount3<<24) & 0xff000000) + ((delayCount2<<16) & 0xff0000)
         + ((delayCount1<<8) & 0xff00) + (delayCount0 & 0xff)
        );

    //the hardware uses a 4 byte unsigned integer - Java doesn't do unsigned
    //easily, so the max value is limited to the maximum positive value Java
    //allows for a signed integer - this limitation is also used for the
    //hardware even though it could handle a larger number

    if (delayCount < 0) {delayCount = 0;}
    if (delayCount > UTBoard.MAX_DELAY_COUNT) {
        delayCount = UTBoard.MAX_DELAY_COUNT;
    }

}//end of BoardChannelSimulator::setDelayCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::setSampleCount
//
// Checks if pRegAddr matches one of the sampleCount byte register addresses
// and stores pValue in that register if so.
//
// Concatenates all bytes set by the host computer to form the value for the
// sampleCount.  Since the value might be illegal when only some of the bytes
// have been set, it is checked for out of bounds.
//

void setSampleCount(byte pRegAddr, byte pValue){


    //since the host computer writes to the variables one byte at a time in the
    //FPGA registers, the bytes are collected individually and then converted
    //to the full values - the values must be tested for out of bounds each time
    //as it could be illegal when only some of the bytes have been updated

    if (pRegAddr == sampleCountReg0) {sampleCount0 = pValue;}
    if (pRegAddr == sampleCountReg1) {sampleCount1 = pValue;}
    if (pRegAddr == sampleCountReg2) {sampleCount2 = pValue;}


    sampleCount = (int)(
        ((sampleCount2<<16) & 0xff0000)
         + ((sampleCount1<<8) & 0xff00) + (sampleCount0 & 0xff)
        );

    //the hardware uses a 3 byte unsigned integer - Java doesn't do unsigned
    //easily, so the max value is limited to the maximum positive value Java
    //allows for a signed integer

    if (sampleCount < 0) {sampleCount = 0;}
    if (sampleCount > UTBoard.MAX_SAMPLE_COUNT) {
        sampleCount = UTBoard.MAX_SAMPLE_COUNT;
    }

}//end of BoardChannelSimulator::setSampleCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::calculateTimeToNextTDC
//
// Returns a number for use as a countdown timer to the next TDC signal.
//
// Returns a number for resetting tdcTracker or helixAdvanceTracker which is
// a small variation of pTypicalCount by a random amount determined by
// SAMPLE_COUNT_VARIATION.
//
// One out of 10 times, the time is set very short to mimic an erroneous
// double hit.
//

private int calculateTimeToNextTDC(int pTypicalCount)
{

    //periodic with slight randomness
    int value = pTypicalCount
        - (int)(SAMPLE_COUNT_VARIATION / 2)
        + (int)(SAMPLE_COUNT_VARIATION * Math.random());

    //occasional very short period to mimic erroneous double hit
    int doubleHit = (int)(10 * Math.random());
    if (doubleHit == 1) { value = (int)(40 * Math.random()); }

    return(value);

}//end of BoardChannelSimulator::calculateTimeToNextTDC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::convertInchesToSampleCounts
//
// Converts the distance of pInches to the corresponding number of samples
// of the UT signal in steel for a full round trip of the wall thickness.
//

private int convertInchesToSampleCounts(double pInches)
{

    return((int)(pInches / velocityUS / uSPerDataPoint) * 2);
    
}//end of BoardChannelSimulator::convertInchesToSampleCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::convertChartPercentageToSampleCounts
//
// Converts the percentage of chart height pPercentage to the corresponding
// number of samples of the UT signal in steel for a full round trip of the
// wall thickness.
//
// This is useful for converting data from files which containg chart height
// values back to the raw time-of-flight values for simulation.
//

private int convertChartPercentageToSampleCounts(double pPercentage)
{

    double wall = nominalWall + (pPercentage - 50) * 
                                                inchesPerChartPercentagePoint;

    return(convertInchesToSampleCounts(wall));
    
}//end of BoardChannelSimulator::convertChartPercentageToSampleCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::setWallParameters
//
// Sets the various wall parameters for use in simulation.
//

public void setWallParameters(double pNominalWall, double pNSPerDataPoint,
     double pUSPerDataPoint, double pVelocityUS, double pCompressionVelocityNS,
                  int pNumWallMultiples, double pInchesPerChartPercentagePoint)
{

    nominalWall = pNominalWall;
    nSPerDataPoint = pNSPerDataPoint;
    uSPerDataPoint = pUSPerDataPoint;
    velocityUS = pVelocityUS;
    compressionVelocityNS = pCompressionVelocityNS;
    numWallMultiples = pNumWallMultiples;
    inchesPerChartPercentagePoint = pInchesPerChartPercentagePoint;
    
}//end of BoardChannelSimulator::setWallParameters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::configure
//
// Loads configuration settings from the file pConfigFile from section
// pSection.
// The various child objects are then created as specified by the config data.
//

void configure(IniFile pConfigFile, String pSection)
{

    sourceAGroupIndex = 
         pConfigFile.readInt(pSection, "Source A Group in Simulation File", -1);
            
    sourceAChartIndex =
         pConfigFile.readInt(pSection, "Source A Chart in Simulation File", -1);

    sourceATraceIndex = 
         pConfigFile.readInt(pSection, "Source A Trace in Simulation File", -1);
    
    sourceADataSetIndex = 
     pConfigFile.readInt(pSection, "Source A Data Set in Simulation File", -1);

    sourceBGroupIndex = 
         pConfigFile.readInt(pSection, "Source B Group in Simulation File", -1);
            
    sourceBChartIndex =
         pConfigFile.readInt(pSection, "Source B Chart in Simulation File", -1);

    sourceBTraceIndex = 
         pConfigFile.readInt(pSection, "Source B Trace in Simulation File", -1);
    
    sourceBDataSetIndex = 
     pConfigFile.readInt(pSection, "Source B Data Set in Simulation File", -1);
        
}//end of BoardChannelSimulator::configure
//-----------------------------------------------------------------------------

}//end of class BoardChannelSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
