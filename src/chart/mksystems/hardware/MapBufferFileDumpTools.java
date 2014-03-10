/******************************************************************************
* Title: MapBufferFileDumpTools.java
* Author: Mike Schoonover
* Date: 9/1/13
*
* Purpose:
*
* This class contains tools for saving and loading data buffers to file in
* various formats. They are mainly useful for debugging as the files are not
* formatted and are raw data from the buffers.
*
* One common use is to dump the map dataBuffer for mapping UTBoards. Inserting
* a line into WallMapDataSaverTuboBinary.saveToFile can call a dump function
* to save or reload a data set for offline debugging.
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

//-----------------------------------------------------------------------------

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
// class MapBufferFileDumpTools
//

public class MapBufferFileDumpTools extends Object{

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::experimental
//
// Method for testing experimental ideas.
//

static public void experimental()
{

    Map<String, Object> m = new HashMap<>();

    Integer x = 5;
    m.put("Integer", x);
    Double y = 10.2;
    m.put("Double", y);
    String z = "Z string";
    m.put("String", z);

    Object obj1 = m.get("Integer");
    if (obj1 instanceof Integer) {
        System.out.println("Integer: " + obj1);
    }

}//end of MapBufferFileDumpTools::experimental
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::getStaticClassName
//
// Returns the class name. Static methods cannot access getClass().getName().
// Instead, this method can be used.
//
// Returns: the simple class name
//

static private String getStaticClassName()
{

    return(MapBufferFileDumpTools.class.getSimpleName());

}//end of MapBufferFileDumpTools::getStaticClassName
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

static private void logSevere(String pMessage)
{

    Logger.getLogger(getStaticClassName()).log(Level.SEVERE, pMessage);

}//end of MapBufferFileDumpTools::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

static private void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getStaticClassName()).log(Level.SEVERE, pMessage, pE);

}//end of MapBufferFileDumpTools::logStackTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::writeHeaderToTextFile
//
// Saves all key/value pairs in pHeaderInfo to file pOutFile.
//

static private void writeHeaderToTextFile(
                    Map<String, Object> pHeaderInfo, BufferedWriter  pOutFile)
                                                            throws IOException
{

    //write the header information - this portion can be read by the iniFile
    //class which will only read up to the "[Header End]" tag - this allows
    //simple parsing of the header information while ignoring the data
    //stream which  follows the header

    pOutFile.write("[Header Start]"); pOutFile.newLine();
    pOutFile.newLine();

    if (pHeaderInfo != null){
        for (Map.Entry<String, Object> keyValuePair :
                                                    pHeaderInfo.entrySet()){
            pOutFile.write(keyValuePair.getKey()
                      + "=" + keyValuePair.getValue()); pOutFile.newLine();
        }
    }

    pOutFile.newLine();

    pOutFile.write("[Header End]"); pOutFile.newLine(); pOutFile.newLine();

}//end of MapBufferFileDumpTools::writeHeaderToTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::saveAllDataBuffersToTextFiles
//
// Saves an array of data buffers pBuffers to text files. The base filename is
// pFilename, the index for each buffer in the array is appended to distinguish
// each file.
//
// Any key/value pairs in pHeaderInfo are stored in the header.
//
// Saves data from pStart to pEnd-1 in each array.
//

static public void saveAllDataBuffersToTextFiles(
        String pFilename, short pBuffer[][], Integer pStart[], Integer pEnd[],
        Map<String, Object> pHeaderInfo)
{

    for (int i = 0; i < pBuffer.length; i++){
        saveDataBufferToTextFile(i, pFilename, pBuffer[i], pStart[i],
                                                     pEnd[i], pHeaderInfo);
    }

}//end of MapBufferFileDumpTools::saveAllDataBuffersToTextFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::saveDataBufferToTextFile
//
// Saves the data in pBuffer to a text file.
//
// Any key/value pairs in pHeaderInfo are stored in the header.
//
// Saves data from pStart to pEnd-1 in the array.
//

static private void saveDataBufferToTextFile(
        int pIndex, String pFilename, short pBuffer[], Integer pStart,
        Integer pEnd, Map<String, Object> pHeaderInfo)
{

    short dataBuffer[] = pBuffer;
    if (dataBuffer == null) {return;}

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pIndex + ".dat";

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter lOutFile = null;

    try{

        fileOutputStream = new FileOutputStream(pFilename);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        lOutFile = new BufferedWriter(outputStreamWriter);

        writeHeaderToTextFile(pHeaderInfo, lOutFile);

        lOutFile.write("[Data Set 1]"); lOutFile.newLine();

        //save all data stored in the buffer
        for(int i = pStart; i < pEnd; i++){
            lOutFile.write(Integer.toString(dataBuffer[i]));
            lOutFile.newLine();
        }

        lOutFile.write("[End of Set]"); lOutFile.newLine();

    }
    catch(IOException e){ logSevere(e.getMessage() + " - Error: 218"); }
    finally{
        try{if (lOutFile != null) {lOutFile.close();}}
        catch(IOException e){ }
        try{if (outputStreamWriter != null) {outputStreamWriter.close();}}
        catch(IOException e){ }
        try{if (fileOutputStream != null) {fileOutputStream.close();}}
        catch(IOException e){ }
    }

}//end of MapBufferFileDumpTools::saveDataBufferToTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::convertTextValueToNumber
//
// Converts the text in pString to the type of object of which pObject is a
// member...Integer, Short, Double, String, etc.
//
// pObject is unchanged -- its only purpose is to specify the desired type.
//
// Returns the converted value as an Object of the appropriate type.
//

static private Object convertTextValueToNumber(String pString, Object pObject)
{

    if (pObject instanceof Integer) {
        return(Integer.valueOf(pString));
    }
    else
    if (pObject instanceof Short) {
        return(Short.valueOf(pString));
    }
    else
    if (pObject instanceof Double) {
        return(Double.valueOf(pString));
    }
    else
    if (pObject instanceof String) {
        return(pString);
    }

    return(null);

}//end of MapBufferFileDumpTools::convertTextValueToNumber
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::parseKeyValueFromString
//
// Parses pString for key=value data and splits into a key and a value. If
// the key is found in pHeaderInfo, the parsed value is converted to the
// appropriate type of object for the associated value in pHeaderInfo and
// stored with the matching key.
//
// NOTE: A key/value entry should already exist in the map for every key which
// is to be parsed. All other keys will be ignored. The value in the map is an
// Object reference and can thus hold Integers, Shorts, Strings, etc.
//

static private void parseKeyValueFromString(
                            String pString, Map<String, Object> pHeaderInfo)
{

    int sepIndex;

    if((sepIndex = pString.indexOf('=')) == -1) { return; }

    pString = pString.trim();

    //extract key before the separator, value after
    String key = pString.substring(0, sepIndex);
    String valueText = pString.substring(sepIndex + 1);

    //check if key exists in the map -- exit if no
    Object value = pHeaderInfo.get(key);
    if (value == null) { return; }

    //convert the text into the same type as Object and store it in the map
    Object object = convertTextValueToNumber(valueText, value);
    if(object != null) { pHeaderInfo.put(key, object); }

}//end of MapBufferFileDumpTools::parseKeyValueFromString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::loadHeaderFromTextFile
//
// Loads all key/value pairs from file pInFile and stores in pHeaderInfo.
//

static private void loadHeaderFromTextFile(
  Map<String, Object> pHeaderInfo, BufferedReader  pInFile) throws IOException
{

    String line;
    boolean success = false;
    String tag = "[Header Start]";
    String tagUC = tag.toUpperCase();


    //search for the segment tag
    while ((line = pInFile.readLine()) != null){  //search for tag
        if (line.trim().toUpperCase().startsWith(tagUC)){
            success = true; break;
        }
    }

    if (success == false) {
        throw(new IOException(
                    "The file could not be read - tag " + tag + " not found."));
    }

    //parse entries in the header

    tag = "[Header End]";
    tagUC = tag.toUpperCase();

    while ((line = pInFile.readLine()) != null){  //search for tag

        //halt when end of header tag reached
        if (line.trim().toUpperCase().startsWith(tagUC)){
            success = true; break;
        }

        //attempt to convert and store in map
        parseKeyValueFromString(line, pHeaderInfo);

    }

    if (success == false) {
        throw(new IOException(
                    "The file could not be read - tag " + tag + " not found."));
    }

}//end of MapBufferFileDumpTools::loadHeaderFromTextFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::loadAllDataBuffersFromTextFiles
//
// Loads an array of data buffers pBuffers from text files. The base filename is
// pFilename, the index for each buffer in the array is appended to distinguish
// each file.
//
// Any key/value pairs in pHeaderInfo are retrieved from the header if they
// are found and each value is updated in pHeaderInfo with the value from the
// file.
//
// Saves data beginning at pStart in each array.
//

static public void loadAllDataBuffersFromTextFiles(
        String pFilename, short pBuffer[][], Integer pStart[], Integer pEnd[],
        Map<String, Object> pHeaderInfo)

{

    for (int i = 0; i < pBuffer.length; i++){
        loadDataBufferFromTextFile(
                    i, pFilename, pBuffer[i], pStart[i], pEnd[i], pHeaderInfo);
    }

}//end of MapBufferFileDumpTools::loadAllDataBuffersFromTextFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::loadDataBufferFromTextFile
//
// Loads data buffer pBuffers from text files. The base filename is pFilename,
// the index for each buffer in the array is appended to distinguish each file.
//
// Any key/value pairs in pHeaderInfo are retrieved from the header if they
// are found and each value is updated in pHeaderInfo with the value from the
// file.
//
// Saves data beginning at pStart in the array.
//

static private void loadDataBufferFromTextFile(
   int pIndex, String pFilename, short pBuffer[], Integer pStart, Integer pEnd,
                            Map<String, Object> pHeaderInfo)
{

    String status = "";

    short dataBuffer[] = pBuffer;

    if (dataBuffer == null) {
        logSevere("No data buffer for index: " + pIndex);
        return;
    }

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pIndex + ".dat";

    int i = 0;

    String tag = "";
    String tagUC;

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader lInFile = null;

    try{

        fileInputStream = new FileInputStream(pFilename);
        inputStreamReader = new InputStreamReader(fileInputStream);
        lInFile = new BufferedReader(inputStreamReader);

        //read key/value pairs from header and store in the map
        loadHeaderFromTextFile(pHeaderInfo, lInFile);

        String line;
        boolean success = false;

        tag = "[Data Set 1]";
        tagUC = tag.toUpperCase();

        //search for the data segment tag
        while ((line = lInFile.readLine()) != null){  //search for tag
            if (line.trim().toUpperCase().startsWith(tagUC)){
                success = true; break;
            }
        }

        if (success == false) {
             logSevere(
                    "The file could not be read - tag " + tag + " not found.");
             return;
        }

        //read data values

        i = pStart;

        while ((line = lInFile.readLine()) != null){

            //stop when next section end tag reached (will start with [)
            if (line.trim().startsWith("[")){
                break;
            }

            //convert the text to an integer and save in the buffer
            short data = Short.parseShort(line);
            dataBuffer[i++] = data;

            //catch buffer overflow
            if (i == dataBuffer.length) {
                logSevere(
                "The file could not be read entirely - too much data for "
                                                + tag + " at data point " + i);
                break;
                }

        }//while ((line = pIn.readLine()) != null)

        //return the last data position + 1 in pBuffer
        pEnd = i;

    }
    catch(NumberFormatException e){
        //catch error translating the text to an integer
        logSevere("The file could not be read - corrupt data for " + tag
                                                   + " at data point " + i);
        return;
    }
    catch (FileNotFoundException e){
        logSevere("Could not find the requested text file for reading.");
        return;
        }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 492");
        return;
        }
    finally{
        try{if (lInFile != null) {lInFile.close();}}
        catch(IOException e){ }
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){ }
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){ }

    }

}//end of MapBufferFileDumpTools::loadDataBufferFromTextFile
//-----------------------------------------------------------------------------

/*

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::saveAllDataBuffersToBinaryFiles
//
// Saves an array of data buffers pBuffers to binary files. The base filename is
// pFilename, the index for each buffer in the array is appended to distinguish
// each file.
//
// pHeader1, pHeader2, pHeader 3 are saved at the beginning of each file.
//

private void saveAllDataBuffersToBinaryFiles(String pFilename,
        Integer pHeader1, Integer pHeader2, Integer pHeader3)

{

    for (int i = 0; i < mapSourceBoards.length; i++){
        saveDataBufferToBinaryFile(i,
           pFilename, settings.jobFileFormat, pInspectionDirectionDescription);
    }

}//end of MapBufferFileDumpTools::saveAllDataBuffersToBinaryFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::saveDataBufferToBinaryFile
//
// Saves the data in pBuffer to a binary file.
//
// pHeader1, pHeader2, pHeader 3 are saved at the beginning of each file.
// Saves data from pStart to pEnd-1 in the array.
//

private void saveAllDataBufferToBinaryFile(String pFilename,
        Integer pHeader1, Integer pHeader2, Integer pHeader3)

{

    short dataBuffer[] = mapSourceBoards[pBoard].dataBuffer;
    if (dataBuffer == null) { return; }

    DataOutputStream lOutFile = null;

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pBoard;

    //create a buffered writer stream

    try{
        lOutFile =
              new DataOutputStream(new BufferedOutputStream(
              new FileOutputStream(pFilename)));

        //save the linear locations of the start/stop inspection positions
        lOutFile.writeDouble(
                    mapSourceBoards[pBoard].utBoard.inspectionStartLocation);
        lOutFile.writeDouble(
                    mapSourceBoards[pBoard].utBoard.inspectionStopLocation);

        //the board will probably still be adding data packets as the system
        //will probably still be in inspect mode at this time, so lock in
        //the current position and save up to that point

        int endOfData =
          mapSourceBoards[pBoard].utBoard.getIndexOfLastDataPointInDataBuffer();

        //save all data stored in the buffer
        for(int i = 0; i < endOfData; i++){
            lOutFile.writeShort(dataBuffer[i]);
        }

    }
    catch(IOException e){
        throw(e);
    }
    finally{
        try{if (lOutFile != null) {lOutFile.close();}}
        catch(IOException e){ throw(e); }
    }

}//end of MapBufferFileDumpTools::saveDataBufferToBinaryFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::loadAllDataBuffersFromBinaryFiles
//
// Loads an array of data buffers pBuffers from binary files. The base filename
// is pFilename, the index for each buffer in the array is appended to
// distinguish each file.
//
// pHeader1, pHeader2, pHeader 3 are loaded from the beginning of each file.
//

private void loadAllDataBuffersFromBinaryFiles(String pFilename,
        Integer pHeader1, Integer pHeader2, Integer pHeader3)
{

    for (int i = 0; i < mapSourceBoards.length; i++){
        saveDataBufferToBinaryFile(i,
           pFilename, settings.jobFileFormat, pInspectionDirectionDescription);
    }

}//end of MapBufferFileDumpTools::loadAllDataBuffersFromBinaryFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapBufferFileDumpTools::loadDataBufferFromBinaryFile
//
// Loads data into pBuffer from a binary file.
//
// pHeader1, pHeader2, pHeader 3 are loaded from the beginning of each file.
// Stores data from pStart to pEnd-1 in the array.
//

private void loadDataBufferFromBinaryFile(String pFilename,
                        Integer pHeader1, Integer pHeader2, Integer pHeader3)
{

    short dataBuffer[] = mapSourceBoards[pBoard].dataBuffer;
    if (dataBuffer == null) { return; }

    pFilename = pFilename + " ~ Wall Mapping Data ~ " + pBoard;

    DataInputStream lInFile = null;


    try{
        lInFile =
              new DataInputStream(new BufferedInputStream(
              new FileInputStream(pFilename)));

        //save the linear locations of the start/stop inspection positions
        mapSourceBoards[pBoard].utBoard.inspectionStartLocation =
                                                        lInFile.readDouble();
        mapSourceBoards[pBoard].utBoard.inspectionStopLocation =
                                                        lInFile.readDouble();

        //the board will probably still be adding data packets as the system
        //will probably still be in inspect mode at this time, so lock in
        //the current position and save up to that point

        int endOfData =
          mapSourceBoards[pBoard].utBoard.getIndexOfLastDataPointInDataBuffer();

        //save all data stored in the buffer
        for(int i = 0; i < endOfData; i++){
            mapSourceBoards[pBoard].dataBuffer[i] = lInFile.readShort();
        }


    //let the board know the position of the last data in the buffer
    mapSourceBoards[pBoard].utBoard.setIndexOfLastDataPointInDataBuffer(i);


    }
    catch(IOException e){
        { throw(e); }
    }
    finally{
        try{if (lInFile != null) {lInFile.close();}}
        catch(IOException e){ throw(e); }
    }

}//end of MapBufferFileDumpTools::loadDataBufferFromBinaryFile
//-----------------------------------------------------------------------------

*
*/
}//end of class MapBufferFileDumpTools
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
