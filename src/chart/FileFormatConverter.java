/******************************************************************************
* Title: UTF16LEToUTF8Converter.java
* Author: Mike Schoonover
* Date: 5/31/12
*
* Purpose:
*
* This class converts all ini, configuration, language, and other types of
* preference files from UTF-16LE to UTF-8.  Before May 2012, all of these files
* were saved as UTF-16LE (type UNICODE in Windows Notepad).  Since Git only
* handles UTF-8 (and this is the most commonly used format), the type was
* changed to UTF-8.
*
* All old jobs will have a configuration file which does not have a file
* format entry, so the file format will default to UTF-16LE for that job to
* all viewing of the old style files in the job folder.
*
* The settings files in the main program folder and the preset and config files
* need to be switched to UTF-8 to avoid confusion as these are independent of
* the job.  This class will convert all pertinent files.
*
* Currently converts the following files:
*
* all .ini files in the main program folder
* all *.config files in the "configurations" folder
* all *.preset files in the "presets" folder
*
* *.language files are not converted as none were ever stored as UTF-16LE
*
* --- File Format Details and Flags used by Windows for Identification ---
*
* Windows Notepad (and probably other Windows programs), adds a multi-byte
* flag to the start of all files in formats other than ANSI.
* 
* Windows Notepad loads Java files saved as UTF-8 files (which do not have the
* Windows flags) as ANSI files. If they are then saved without changing the
* file type, Notepad then saves them as Windows ANSI. Java does not utilize
* the flags which Windows uses to detect UTF-8.
*
* The two formats are similar, especially for the standard character set, so
* Java can load Windows ANSI files as if they were UTF-8, so the files can
* be loaded, edited, and saved in Notepad with no problems.
*
* Windows UTF-8 files have an extra 3 bytes at the beginning which is used to
* identify the format: 0xef 0xbb 0xbf
*
* The -16 variants have similar flags for Windows, and Notepad will load files
* of those types saved by Java as ANSI (incorrectly) if the flag is missing:
*
* UTF-16LE (Windows calls this Unicode): 0xff 0xfe
* UTF-16BE (Windows calls this Unicode big endian) : 0xfe 0xff 
*
* If a UTF-16LE file does not have the flag bytes, Notepad will still detect
* it as a Unicode/UTF-16LE file, probably by detecting the zero bytes which
* are part of the two byte basic characters. For some reason, it cannot do the
* same for UTF-16BE. As for UTF-8 files missing the flag bytes, UTF-16BE files
* are loaded as ANSI resulting in the zeroes of the upper byte of each
* character being displayed as spaces between each character.
*
* If the first line of the file is blank or a comment line or some line not
* used by the IniFile class, the flag bytes appear as trash at the beginning
* of the line which is ignored anyway. If, however, the first line is
* important, such as a section tag ([example section]), then the section tag
* will not be recognized because of the flag bytes read in at the beginning of
* the line.
*
* The problem could be solved by writing the flag when saving the file in Java.
* When this program used UTF-16LE, the problem was solved by saving the files
* in Notepad as UTF-16 to force insertion of the flag. A note was added to each
* file warning not to delete the first line. This class ignored the flag because
* the first line was not used so any unexpected flag bytes were ignored. When
* this class saves the files, it writes out every line read in so the flags
* were preserved. This unhandy workaround was used due to ignorance regarding
* the flags.
*
* Saving the flags may prove to be prudent in the future to allow for other
* languages which use the extended character set. ANSI and UTF-8 differ there,
* so manipulating the files in Notepad (which will think they are ANSI) will
* make them unreadable to Java.
*
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class FileFormatConverter
//
// See notes at top of page for details.
//

public class FileFormatConverter extends FileConverter{

    private static final int NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS = 3;

    private static final byte UTF8_FLAG1 = (byte)0xef;
    private static final byte UTF8_FLAG2 = (byte)0xbb;
    private static final byte UTF8_FLAG3 = (byte)0xbf;

    private static final byte UTF16LE_FLAG1 = (byte)0xff;
    private static final byte UTF16LE_FLAG2 = (byte)0xfe;

    private static final byte UTF16BE_FLAG1 = (byte)0xfe;
    private static final byte UTF16BE_FLAG2 = (byte)0xff;

    private static final byte[] UTF8_FLAG_BYTES = 
                                        {(byte)0xef, (byte)0xbb, (byte)0xbf};
    private static final byte[] UTF16LE_FLAG_BYTES = {(byte)0xff, (byte)0xfe};
    private static final byte[] UTF16BE_FLAG_BYTES = {(byte)0xfe, (byte)0xff};
    
//-----------------------------------------------------------------------------
// FileFormatConverter::FileFormatConverter (constructor)
//
// pNewFormat specifies the format to which all files will be converted.
//
// Method init() MUST be called after instantiation.
//

public FileFormatConverter(String pNewFormat)
{

    newFormat = pNewFormat;

}//end of FileFormatConverter::FileFormatConverter (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::init
//
// Initializes the object.  MUST be called after instantiation.
//
// This method also performs the conversion operation.
//
// If all files were converted, tested for identical content, old file deleted
// and new file renamed to old filename, then a flag file will be created so
// that the conversion is not attempted again.
//
// If an error occurs, then some files may have been converted while others
// have not.  In that case, no flag file is created and the conversion is
// attempted again.  The files converted and renamed on the previous pass will
// be ignored because they are not in the old UTF-16LE format.  Those converted
// but not deleted/renamed will be converted again.  This ability to make
// multiple conversion passes should cover most error cases.
//

@Override
public void init()
{

    pathList = new String[NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS];
    extList = new String[NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS];

    //add each path/extension combination to the lists
    //a path is listed repeatedly if multiple extensions targeted in that path
    //an extension is listed repeatedly if it is found in multiple paths

    //convert ini files in program root folder
    pathList[0] = "."; extList[0] = "ini"; //files in root folder
    pathList[1] = "presets"; extList[1] = "preset"; //files in preset folder
    pathList[2] = "configurations"; extList[2] = "config"; //in config folder

    //call the parent class to perform the conversion
    super.init();

}//end of FileFormatConverter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::init
//
// Initializes the object.  MUST be called after instantiation.
//
// This method also performs the conversion operation.
//
// This version of init allows the path and extension lists to be supplied.
// The paths specified in pPathList will be searched for files having the
// extension in pExtList at the same index in the array as the path is
// located in the pPathList.
//
// A log file documenting the conversion will be stored as pLogFilename.
//
// To process multiple extensions in a singe path, the path should be added
// to the path list multiple times with each different extension added to the
// extension list at the corresponding index. Exampe:
//
//    pathList[0] = "test"; extList[0] = "ini";  //ini files test folder
//    pathList[1] = "test2"; extList[1] = "ini"; //ini files in test2 folder
//    pathList[2] = "test2"; extList[2] = "txt"; //txt files in test2 folder
//
// See init() for more information.
//

public void init(String[] pPathList, String[] pExtList, String pLogFilePath)
{

    pathList = pPathList; extList = pExtList;
    
    logFilePath = pLogFilePath;

    //call the parent class to perform the conversion
    super.init();

}//end of FileFormatConverter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::convertFile
//
// If pOldFile is a normal file (not a directory) and is not of newFormat
// format, the file is converted to newFormat as a new file pTempFile.
//
// Returns true if no error, false if error.
//
// Per the FileConverter parent class, the old file is not deleted.
//

@Override
protected boolean convertFile(String pOldFile, String pTempFile)
{

    boolean convertGood = true;
    String oldFormat;
    
    File oldFile = new File(pOldFile), tempFile = new File(pTempFile);

    AtomicBoolean containsWindowsFlags = new AtomicBoolean(false);
    
    try{
        oldFormat = detectFileFormat(pOldFile, containsWindowsFlags);
    }
    catch(IOException e){
        return(false);
    }

    //exit if file already formatted as desired, unless it contains Windows
    //flags in which case the file is converted anyway to remove the flags
    //for consistency as all other formats will have the flags removed
    
    if(oldFormat.equals(newFormat) && !containsWindowsFlags.get()) {
        return(convertGood);
    }

    boolean addFormatTypeEntry = false;

    //for config files, add a line specifying the new format of UTF-8 when
    //the proper section is reached
    if (pOldFile.contains("configurations") && pOldFile.endsWith("config")) {
        addFormatTypeEntry = true;
    }

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader in = null;

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter out = null;

    try{

        //open the old format file for reading
        fileInputStream = new FileInputStream(pOldFile);
        inputStreamReader = new InputStreamReader(fileInputStream, oldFormat);
        in = new BufferedReader(inputStreamReader);

        //open the new format file for writing
        // (append " UTF-8" to the file name to differentiate from the old file)
        fileOutputStream = new FileOutputStream(pTempFile);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream,newFormat);
        out = new BufferedWriter(outputStreamWriter);

        String line;
        boolean firstLineDone = false;
        
        //read lines from the old UTF-16LE file until end reached

        while ((line = in.readLine()) != null){

            //if Windows format-type flags are present, they end up at the
            //beginning of the first line; they are removed here as they are
            //flags for the old format and Java does not use such flags anyways
            
            if(!firstLineDone){
                
                AtomicBoolean wasTrimmed = new AtomicBoolean(false);
                
                line = checkForByteMatchAndRemoveBytes(
                      line, oldFormat, UTF8_FLAG_BYTES, newFormat, wasTrimmed);
                
                if(!wasTrimmed.get()){
                    line = checkForByteMatchAndRemoveBytes(line, oldFormat,
                                    UTF16LE_FLAG_BYTES, newFormat, wasTrimmed);
                }

                if(!wasTrimmed.get()){                
                    line = checkForByteMatchAndRemoveBytes(line, oldFormat,
                                    UTF16BE_FLAG_BYTES, newFormat, wasTrimmed);
                }
                    
                firstLineDone = true;
            }
                
            //do not copy format explanation lines which are no longer used
            if (line.startsWith(";Do not erase")) {continue;}
            if (line.startsWith(";To make a new")) {continue;}

            //write each line to the new format file
            out.write(line); out.newLine();

            //for applicable files, add a line to specify new format
            if (addFormatTypeEntry && line.startsWith("[Main Configuration]")){
                out.write("Job File Format=" + newFormat);
                out.newLine();
            }


        }//while...

    }//try
    catch (FileNotFoundException e){
        logSevere(e.getMessage() + " - Error: 200");
        logFile.log("Conversion fail - old or new file not found.");
        convertGood = false;
    }//catch...
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 205");
        logFile.log("Conversion fail - IO error for old or new file.");
        convertGood = false;
    }//catch...
    finally{

        try{if (in != null) {in.close();}}
            catch(IOException e){convertGood = false;}
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
            catch(IOException e){convertGood = false;}
        try{if (fileInputStream != null) {fileInputStream.close();}}
            catch(IOException e){convertGood = false;}

        try{if (out != null) {out.close();}}
            catch(IOException e){convertGood = false;}
        try{if (outputStreamWriter != null) {outputStreamWriter.close();}}
            catch(IOException e){convertGood = false;}
        try{if (fileOutputStream != null) {fileOutputStream.close();}}
            catch(IOException e){convertGood = false;}

        if(convertGood) {
            logFile.log("File converted: " + oldFile);
        }
        else {
            logFile.log("Error during conversion: " + oldFile);
        }

    }//finally

    return(convertGood);

}//end of FileFormatConverter::convertFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::checkForByteMatchAndRemoveBytes
//
// pLine is converted to a byte array assuming character set pLineFormat.
// Then checks to see if the first bytes in array matches those in pMatchBytes.
// If so, the bytes are removed from the pBytes array and it is returned as a
// string using character set pOutputFormat.
//
// If the bytes do not match, pLine is returned untrimmed.
//
// If the string was trimmed, pWasTrimmed is set true else it is left unchanged;
// the value should be passed in as false.
//

private String checkForByteMatchAndRemoveBytes(String pLine, String pLineFormat,
          byte[] pMatchBytes, String pOutputFormat, AtomicBoolean pWasTrimmed)
{        

    byte[] bytes;
    
    try{
        bytes = pLine.getBytes(pLineFormat);
    }catch(UnsupportedEncodingException e){
        return(pLine);
    }
    
    boolean match = true;
    
    //check for matching bytes unless source is too short which is no match
    if (bytes.length >= pMatchBytes.length){    
        for (int i=0; i<pMatchBytes.length; i++){
            if(bytes[i] != pMatchBytes[i]) {match = false; break;}        
        }
    }else{
        return(pLine); //return line unchanged
    }

    if (!match){ return(pLine); } //return line changed
    
    byte[] trimmed;
        
    trimmed = new byte[bytes.length - pMatchBytes.length];

    System.arraycopy(bytes, pMatchBytes.length,
                         trimmed, 0, bytes.length - pMatchBytes.length);

    pWasTrimmed.set(true);
        
    //return the byte array as a string
    try{return(new String(trimmed, pLineFormat));}
    catch(UnsupportedEncodingException e){
        return("");
    }        

}//end of FileFormatConverter::checkForByteMatchAndRemoveBytes
//-----------------------------------------------------------------------------
    
//-----------------------------------------------------------------------------
// FileFormatConverter::removeBeginningCharacters
//
// Removes pCount number of characters from the beginning of pString and
// returns the result.
//

private String removeBeginningCharacters(String pString, int pCount)
{

    return(pString.substring(pCount));
    
}//end of FileFormatConverter::removeBeginningCharacters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::compareFileLastLines
//
// The last non-blank line of each file, pOldFile and pTempFile, are compared
// to determine if the latter is most likely a successfully converted version
// of the former.
//
// Since converted files have Windows flags and format specific identifier
// lines stripped out, a line-for-line comparison with the original file is not
// possible. In lieu of this, comparing only the last non-blank lines is used
// as a reasonable test.
//
// If both are normal files (not directories), the two are compared, taking
// into account that the old file and new files may have different formats.
//
// If the two contain identical information and there are no errors, the
// function returns true.  A failed comparison is thus considered to be an
// error.
//
// Returns false otherwise.
//

@Override
protected boolean compareFileLastLines(String pOldFile, String pTempFile)
{

    boolean compareGood = true;

    File oldFile = new File(pOldFile); File tempFile = new File(pTempFile);

    //if the old file is not present, then exit without error -- the temp
    //file may be orphaned so return without error so it will be renamed

    if (!oldFile.exists()){
        logFile.log("Compare ignored -- there is no old file: " + oldFile);
        return(compareGood);
    }

    //if a new temp file is not present, then exit without error -- the temp
    //file was probably already renamed for this set on a previous conversion
    //attempt

    if (!tempFile.exists()){
        logFile.log("Compare ignored -- there is no temp file: " + tempFile);
        return(compareGood);
    }

    //make sure old file is a normal file
    if (!oldFile.isFile()){
        logFile.log("Compare fail - old file is not a normal file.");
        compareGood = false; return(compareGood);
    }

    //make sure temp file is a normal file
    if (!tempFile.isFile()){
        logFile.log("Compare fail - temp file is not a normal file.");
        compareGood = false; return(compareGood);
    }

    String detectedFileFormat;
    AtomicBoolean containsWindowsFlags = new AtomicBoolean(false);
    
    try{
        detectedFileFormat = detectFileFormat(pOldFile, containsWindowsFlags);
    }
    catch(IOException e){
        detectedFileFormat = newFormat; //try default on error
    }

    FileInputStream oldInputStream = null;
    InputStreamReader oldInputStreamReader = null;
    BufferedReader oldIn = null;

    FileInputStream newInputStream = null;
    InputStreamReader newInputStreamReader = null;
    BufferedReader newIn = null;

    try{

        //open the old format file for reading
        oldInputStream = new FileInputStream(pOldFile);
        oldInputStreamReader = 
                    new InputStreamReader(oldInputStream, detectedFileFormat);
        oldIn = new BufferedReader(oldInputStreamReader);

        //open the new format file for reading
        newInputStream = new FileInputStream(pTempFile);
        newInputStreamReader = new InputStreamReader(newInputStream, newFormat);
        newIn = new BufferedReader(newInputStreamReader);

        String line, oldLine="old", newLine="new";

        //read all lines from each to obtain the last line in each file

        while ((line = oldIn.readLine()) != null){
                if(!line.isEmpty()){ oldLine = line; }
            }

        while ((line = newIn.readLine()) != null){
                if(!line.isEmpty()){ newLine = line; }
            }
                
        //if lines don't match, then exit with error
        if (!oldLine.equals(newLine)){
            logFile.log("Compare fail - lines don't match.");
            compareGood = false; return(compareGood);
        }

    }//try
    catch (FileNotFoundException e){
        logSevere(e.getMessage() + " - Error: 385");
        logFile.log("Compare fail - old or temp file not found.");
        compareGood = false;
    }//catch...
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 390");
        logFile.log("Compare fail - IO error for old or temp file.");
        compareGood = false;
    }//catch...
    finally{

        try{if (newIn != null) {newIn.close();}}
            catch(IOException e){compareGood = false;}
        try{if (newInputStreamReader != null) {newInputStreamReader.close();}}
            catch(IOException e){compareGood = false;}
        try{if (newInputStream != null) {newInputStream.close();}}
            catch(IOException e){compareGood = false;}

        try{if (oldIn != null) {oldIn.close();}}
            catch(IOException e){compareGood = false;}
        try{if (oldInputStreamReader != null) {oldInputStreamReader.close();}}
            catch(IOException e){compareGood = false;}
        try{if (oldInputStream != null) {oldInputStream.close();}}
            catch(IOException e){compareGood = false;}

        //if there was any error closing either file, fail the compare to be
        //on the safe side

        if(compareGood) {
            logFile.log("Files are identical: " + oldFile);
        }
        else {
            logFile.log("Compare fail: " + oldFile);
        }

    }//finally

    return(compareGood);

}//end of FileFormatConverter::compareFileLastLines
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::detectFileFormat
//
// Determines the file format of the specified file as ANSI, UTF-8, UTF-16LE,
// or UTF-16BE by checking the first few bytes for flags stored by Windows
// or by looking at the zero value arrangement if the file has no flags and
// was created by Java or similar.
//
// Returns the appropriate file format string.
//
// Returns newFormat if file not found.
//
// Regardless of the format detected, pContainsWindowsFlags is set true if
// the file contains Window's format identifier flags at the beginning of the
// file. The calling function can use this flag to determine if the flags
// need to be stripped even if the format is the preferred format.
//
// Throws IOException on I/O error.
//
// See top of this file for details on the different formats and the flags
// Window's uses for its identification purposes.
//

static public String detectFileFormat(String pFilename,
                        AtomicBoolean pContainsWindowsFlags) throws IOException
{

    byte [] data = new byte[10];
    for(int i=0; i<data.length; i++){ data[i] = (byte)0xff; }
    
    FileInputStream in = null;

    try {

        in = new FileInputStream(pFilename);

        //even though an int is used here, only one byte at a time is loaded
        int c, i=0;

        //read in up to 10 bytes
        while (((c = in.read()) != -1) && (i<data.length) ) {
            
            data[i++] = (byte) c;
            
        }

    }//try
    catch (FileNotFoundException e){
        return(newFormat);
    }//catch
    catch(IOException e){
        throw new IOException(e.getMessage() + " - Error: 1089");
    }
    finally {
        if (in != null) {
            try{
                in.close();
            }
            catch(IOException e){
                throw new IOException(e.getMessage() + " - Error: 1097");
            }
        }//if (in...
    }//finally
    
    //if format determined by detecting Window's flags, set flag true and
    //return
    
    if (data[0]==UTF8_FLAG1 && data[1]==UTF8_FLAG2 && data[2]==UTF8_FLAG3){
        pContainsWindowsFlags.set(true);
        return("UTF-8");
    }

    if (data[0]==UTF16LE_FLAG1 && data[1]==UTF16LE_FLAG2){
        pContainsWindowsFlags.set(true);        
        return("UTF-16LE");
    }
    
    if (data[0]==UTF16BE_FLAG1 && data[1]==UTF16BE_FLAG2){
        pContainsWindowsFlags.set(true);        
        return("UTF-16BE");
    }

    //look for zero as first or second byte of first character -- probably
    //UTF-16LE or UTF-16BE if found, but if first character is an extended
    //character then it might not have a zero so the test will fail
    //all files saved by this program would have had a non-extended character
    //at the beginning, so the test should be valid
    
    //if second byte is a zero, probably MSB of UTF-16LE character
    if (data[1]== 0){
        return("UTF-16LE");
    }
    
    //if first byte is a zero, probably MSB of UTF-16BE character
    if (data[0]== 0){
        return("UTF-16BE");
    }

    //files that fail the above tests could be ANSI or UTF-8
    //no ANSI files were ever used by this program, so must be UTF-8
    //empty files without format flags will end up here as well
    
    return("UTF-8");
    
}//end of FileFormatConverter::detectFileFormat
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of FileFormatConverter::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// FileFormatConverter::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of FileFormatConverter::logStackTrace
//-----------------------------------------------------------------------------

}//end of class FileFormatConverter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
