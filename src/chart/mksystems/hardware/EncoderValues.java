/******************************************************************************
* Title: EncoderValues.java
* Author: Mike Schoonover
* Date: 4/13/14
*
* Purpose:
*
* This class handles variables related to a position encoder.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
// class Encoder Values
//
// Various encoder values returned by Control board as recorded at various
// stages in the inspection process.
//
// Value photoEyeToPhotoEyeDistance is the distance between the two spots
// on the tube of the vertical photo eyes.
//

public class EncoderValues extends Object{

    public int encoderPosAtOnPipeSignal = 0;
    public int encoderPosAtOffPipeSignal = 0;
    public int encoderPosAtHead1DownSignal = 0;
    public int encoderPosAtHead1UpSignal = 0;
    public int encoderPosAtHead2DownSignal = 0;
    public int encoderPosAtHead2UpSignal = 0;
    
    public double photoEyeToPhotoEyeDistance = 0;
    public double encoder1InchesPerCount = 0;
    public double encoder2InchesPerCount = 0;
    
//-----------------------------------------------------------------------------
// EncoderValues::EncoderValues (constructor)
//

public EncoderValues()
{


}//end of EncoderValues::EncoderValues (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of EncoderValues::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::setAllToMaxValue
//
// Sets all member variables to max value.
//

public void setAllToMaxValue()
{

    encoderPosAtOnPipeSignal = Integer.MAX_VALUE;
    encoderPosAtOffPipeSignal = Integer.MAX_VALUE;
    encoderPosAtHead1DownSignal = Integer.MAX_VALUE;
    encoderPosAtHead1UpSignal = Integer.MAX_VALUE;
    encoderPosAtHead2DownSignal = Integer.MAX_VALUE;
    encoderPosAtHead2UpSignal = Integer.MAX_VALUE;

}//end of EncoderValues::setAllToMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::convertBytesToInt
//
// Converts four bytes to an int value and returns the value.
//

public int convertBytesToInt(byte byte3, byte byte2, byte byte1, byte byte0)
{
    
    int value;
    
    value = ((byte3 << 24));
    value |= (byte2 << 16)& 0x00ff0000;
    value |= (byte1 << 8) & 0x0000ff00;
    value |= (byte0)      & 0x000000ff;

    return(value);

}//end of EncoderValues::convertBytesToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::convertEncoder1CountsToFeet
//
// Converts pCounts number of encoder 1 counts to decimal feet and returns that
// value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder1CountsToFeet(int pCounts)
{
    
    return(pCounts * encoder1InchesPerCount / 12);
    
}//end of EncoderValues::convertEncoder1CountsToFeet
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::convertEncoder2CountsToFeet
//
// Converts pCounts number of encoder 2 counts to decimal feet and returns that
// value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder2CountsToFeet(int pCounts)
{
    
    return(pCounts * encoder2InchesPerCount / 12);
    
}//end of EncoderValues::convertEncoder2CountsToFeet
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::writeEncoderValuesToFile
//
// Writes all encoder values to a text file with name pFilename as the filename
// prefix.
//

public void writeEncoderValuesToFile(String pFilename)
{

    pFilename = pFilename + " ~ Wall Mapping Data ~ Encoder Values.dat";

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter lOutFile = null;

    try{

        fileOutputStream = new FileOutputStream(pFilename);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        lOutFile = new BufferedWriter(outputStreamWriter);


        lOutFile.write("[Encoder Values]"); lOutFile.newLine();

        writeEncoderValuesToOpenFile(lOutFile);
        
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

}//end of EncoderValues::writeEncoderValuesToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::writeEncoderValuesToOpenFile
//
// Writes all encoder values to already opened file pOutFile.
//

private void writeEncoderValuesToOpenFile(BufferedWriter pOutFile)
                                                        throws IOException
{
    
    pOutFile.write("Encoder Position at On Pipe Signal=" 
                                                + encoderPosAtOnPipeSignal);
    pOutFile.newLine();
    pOutFile.write("Encoder Position at Off Pipe Signal=" 
                                                +  encoderPosAtOffPipeSignal);
    pOutFile.newLine();
    pOutFile.write("Encoder Position at Head 1 Down Signal=" 
                                                +  encoderPosAtHead1DownSignal);
    pOutFile.newLine();
    pOutFile.write("Encoder Position at Head 1 Up Signal=" 
                                                +  encoderPosAtHead1UpSignal);
    pOutFile.newLine();
    pOutFile.write("Encoder Position at Head 2 Down Signal=" 
                                                +  encoderPosAtHead2DownSignal);
    pOutFile.newLine();
    pOutFile.write("Encoder Position at Head 2 Up Signal=" 
                                                +  encoderPosAtHead2UpSignal);
    pOutFile.newLine();

    pOutFile.write("Vertical Photo Eye to Vertical Photo Eye Distance=" 
                                                +  photoEyeToPhotoEyeDistance);
    pOutFile.newLine();
    pOutFile.write("Encoder 1 Inches per Count=" 
                                                +  encoder1InchesPerCount);
    pOutFile.newLine();
    pOutFile.write("Encoder 2 Inches per Count=" 
                                                +  encoder2InchesPerCount);
    pOutFile.newLine();
    
}//end of EncoderValues::writeEncoderValuesToOpenFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::readEncoderValuesFromFile
//
// Reads all encoder values from a text file with name pFilename as the filename
// prefix.
//

public void readEncoderValuesFromFile(String pFilename)
{

    pFilename = pFilename + " ~ Wall Mapping Data ~ Encoder Values.dat";

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader lInFile = null;

    try{

        fileInputStream = new FileInputStream(pFilename);
        inputStreamReader = new InputStreamReader(fileInputStream);
        lInFile = new BufferedReader(inputStreamReader);

        readEncoderValuesFromOpenFile(lInFile);
                
    }
    catch(IOException e){ logSevere(e.getMessage() + " - Error: 218"); }
    finally{
        try{if (lInFile != null) {lInFile.close();}}
        catch(IOException e){ }
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){ }
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){ }
    }

}//end of EncoderValues::readEncoderValuesFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::readNextValue
//
// Reads and returns the next value in pInFile from the next line containing
// a key/value pair (key=value). Any blank lines or lines starting with a
// '[', such as section tags, are skipped. Any lines which do not contain '='
// are also skipped.
//
// Returns null when end of file reached.
//

private String  readNextValue(BufferedReader pInFile) throws IOException
{        

    String line;
    
    while((line = pInFile.readLine()) != null){
    
        //skip all blank lines and section header lines
        if ("".equals(line.trim())) {continue;}
        if (line.trim().startsWith("[")) {continue;}
        
        //find '=' symbol
        int pos = line.lastIndexOf('=');
        if (pos == -1) {continue;} //skip line if '=' not present
        
        //return value after the '=' symbol
        line = line.substring(pos+1);
        if ("".equals(line.trim())) {continue;}
        
        return(line);
        
    }
    
    return(line);
    
}//end of EncoderValues::readNextValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::readEncoderValuesFromOpenFile
//
// Reads all encoder values from already opened file pInFile.
//

public void readEncoderValuesFromOpenFile(BufferedReader pInFile)
                                                        throws IOException
{

    String value;

    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtOnPipeSignal = Integer.parseInt(value);

    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtOffPipeSignal = Integer.parseInt(value);
    
    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtHead1DownSignal = Integer.parseInt(value);

    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtHead1UpSignal = Integer.parseInt(value);

    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtHead2DownSignal = Integer.parseInt(value);

    value = readNextValue(pInFile); if (value == null){return;}
    encoderPosAtHead2UpSignal = Integer.parseInt(value);
    
    value = readNextValue(pInFile); if (value == null){return;}
    photoEyeToPhotoEyeDistance = Double.parseDouble(value);

    value = readNextValue(pInFile); if (value == null){return;}
    encoder1InchesPerCount = Double.parseDouble(value);
    
    value = readNextValue(pInFile); if (value == null){return;}
    encoder2InchesPerCount = Double.parseDouble(value);            
            
}//end of EncoderValues::readEncoderValuesFromOpenFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of EncoderValues::logSevere
//-----------------------------------------------------------------------------

}//end of class EncoderValues
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
