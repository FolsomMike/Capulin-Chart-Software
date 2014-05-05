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

import chart.mksystems.inifile.IniFile;
import java.io.BufferedReader;
import java.io.IOException;

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

    int simulationType;
    
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

//-----------------------------------------------------------------------------
// BoardChannelSimulator::BoardChannelSimulator (constructor)
//

public BoardChannelSimulator(
                            int pIndex, int pBufferSize, int pSimulationType,
                            byte pSampleDelayReg0, byte pSampleCountReg0)
{

    index = pIndex;    
    bufferSize = pBufferSize;
    simulationType = pSimulationType;
    
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
        
}//end of BoardChannelSimulator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::prepareNextSimulationDataSetFromFiles
//
// Loads the next set of simulation data from pSourceFile.
//
// bufferADataEnd/bufferBDataEnd will be set to index of the last data
// point stored in each buffer.
//

public void prepareNextSimulationDataSetFromFiles(BufferedReader pSourceFile)
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
    
}//end of BoardChannelSimulator::prepareNextSimulationDataSetFromFiles
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
// wip mks -- hard coded to 170+/-10 which is around 0.297" wall
//    change this to be a bit more than nominal wall value loaded from sim file
//

public int getNextMaxWallValueRandom()
{
 
    short wall;
    wall = (short)(170 + (Math.random()*10));
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
    
    return(value);
    
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

}//end of BoardChannelSimulator::getNextMaxWallValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannelSimulator::getNextMinWallValueRandom
//
// Returns the next minimum wall value simulated via random number. An
// occasional down spike is added.
//
// wip mks -- hard coded to 145+/-10 which is around 0.253" wall
//    change this to be a bit less than nominal wall value loaded from sim file
//

public int getNextMinWallValueRandom()
{
 
    short wall;
    
    wall = (short)(145 + (Math.random()*10));

    //occasional down spike
    if(((int)(Math.random()*200)) == 1) {
        wall -= (int)(Math.random()*30);
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
    
    return(value);
    
}//end of BoardChannelSimulator::getNextMinWallValueFromFile
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
