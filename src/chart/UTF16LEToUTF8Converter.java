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
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

import java.io.*;

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTF16LEToUTF8Converter
//
// See notes at top of page for details.
//

public class UTF16LEToUTF8Converter extends FileConverter{

static int NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS = 3;

//-----------------------------------------------------------------------------
// UTF16LEToUTF8Converter::UTF16LEToUTF8Converter (constructor)
//
// Method init() MUST be called after instantiation.
//

public UTF16LEToUTF8Converter()
{


}//end of UTF16LEToUTF8Converter::UTF16LEToUTF8Converter (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTF16LEToUTF8Converter::init
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

    tempFileSuffix = " UTF-8";
    logFileName = "Config file UTF-16LE to UTF-8 Conversion Log.txt";
    conversionCompletedFlagFileName = "Configs Have Been Converted To UTF-8";

    pathList = new String[NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS];
    extList = new String[NUMBER_OF_FILE_PATH_EXTENSION_COMBINATIONS];

    //add each path/extension combination to the lists
    //a path is listed repeatedly if multiple extensions targeted in that path
    //an extension is listed repeatedly if it is found in multiple paths

    //convert ini files in program root folder
    pathList[0] = "."; extList[0] = "ini";
    pathList[1] = "presets"; extList[1] = "preset";
    pathList[2] = "configurations"; extList[2] = "config";

    //call the parent class to perform the conversion
    super.init();

}//end of UTF16LEToUTF8Converter::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTF16LEToUTF8Converter::convertFile
//
// If pOldFile is a normal file (not a directory) and contains a phrase used in
// all UTF-16LE files created by this program in the past, the file is converted
// to UTF-8 as a new file pTempFile.
//
// Returns true if no error, false if error.
//
// Per the FileConverter parent class, the old file is not deleted.
//

@Override
protected boolean convertFile(String pOldFile, String pTempFile)
{

    boolean convertGood = true;

    File oldFile = new File(pOldFile), tempFile = new File(pTempFile);

    //if the file is not a UTF-16LE file saved by this program, skip it
    //this is not an error, so return true
    if(!IniFile.detectUTF16LEFormat(pOldFile)) return(convertGood);

    boolean addFormatTypeEntry = false;

    //for config files, add a line specifying the new format of UTF-8 when
    //the proper section is reached
    if (pOldFile.contains("configurations") && pOldFile.endsWith("config"))
        addFormatTypeEntry = true;

    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader in = null;

    FileOutputStream fileOutputStream = null;
    OutputStreamWriter outputStreamWriter = null;
    BufferedWriter out = null;

    boolean firstNonBlankLineReached = false;

    try{

        //open the old UTF-16LE format file for reading
        fileInputStream = new FileInputStream(pOldFile);
        inputStreamReader = new InputStreamReader(fileInputStream, "UTF-16LE");
        in = new BufferedReader(inputStreamReader);

        //open the new UTF-8 format file for writing
        // (append " UTF-8" to the file name to differentiate from the old file)
        fileOutputStream = new FileOutputStream(pTempFile);
        outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
        out = new BufferedWriter(outputStreamWriter);

        String line;

        //read lines from the old UTF-16LE file until end reached

        while ((line = in.readLine()) != null){

            //set flag when first non-blank line reached (the first blank line
            //actually contains a code byte left over from the UTF-16LE format
            //so look for a line with length > 1)
            if(line.length() > 1) firstNonBlankLineReached = true;

            //toss all blank lines and the explanation lines at the beginning
            //the old UTF-16LE files had an explanation header -- not needed now

            if (!firstNonBlankLineReached) continue;
            if (line.startsWith(";Do not erase")) continue;
            if (line.startsWith(";To make a new")) continue;

            //write each line to the new UTF-8 file
            out.write(line); out.newLine();

            //for applicable files, add a line to specify new format of UTF-8
            if (addFormatTypeEntry && line.startsWith("[Main Configuration]")){
                out.write("Job File Format=UTF-8");
                out.newLine();
            }


        }//while...

    }//try
    catch (FileNotFoundException e){
        System.err.println(getClass().getName() + " - Error: 200");
        logFile.log("Conversion fail - old or new file not found.");
        convertGood = false;
    }//catch...
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 205");
        logFile.log("Conversion fail - IO error for old or new file.");
        convertGood = false;
    }//catch...
    finally{

        try{if (in != null) in.close();}
            catch(IOException e){convertGood = false;}
        try{if (inputStreamReader != null) inputStreamReader.close();}
            catch(IOException e){convertGood = false;}
        try{if (fileInputStream != null) fileInputStream.close();}
            catch(IOException e){convertGood = false;}

        try{if (out != null) out.close();}
            catch(IOException e){convertGood = false;}
        try{if (outputStreamWriter != null) outputStreamWriter.close();}
            catch(IOException e){convertGood = false;}
        try{if (fileOutputStream != null) fileOutputStream.close();}
            catch(IOException e){convertGood = false;}

        if(convertGood)
            logFile.log("File converted: " + oldFile);
        else
            logFile.log("Error during conversion: " + oldFile);

        return(convertGood);

    }//finally

}//end of UTF16LEToUTF8Converter::convertFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTF16LEToUTF8Converter::compareFile
//
// The old UTF-16LE pOldFile and the new UTF-8 pTempFile are compared.
//
// If both are normal files (not directories), the two are compared, taking
// into account that the old file is in format UTF-16LE with header info and
// the new file is in format UTF-8 without the header info.
//
// The files are compared line for line, ignoring header lines in the old file.
// The old file must have the header lines and the new file cannot have extra
// lines.
//
// If the two contain identical information and there are no errors, the
// function returns true.  A failed comparison is thus considered to be an
// error.
//
// Returns false otherwise.
//

@Override
protected boolean compareFile(String pOldFile, String pTempFile)
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

    //if the old file is not of the old style format, fail compare
    if(!IniFile.detectUTF16LEFormat(pOldFile)){
        logFile.log("Compare fail - old file is not in UTF-16LE format.");
        compareGood = false; return(compareGood);
    }

    boolean ignoreFormatTypeEntry = false;

    //for config files, add a line specifying the new format of UTF-8 when
    //the proper section is reached
    if (pOldFile.contains("configurations") && pOldFile.endsWith("config"))
        ignoreFormatTypeEntry = true;

    FileInputStream oldInputStream = null;
    InputStreamReader oldInputStreamReader = null;
    BufferedReader oldIn = null;

    FileInputStream newInputStream = null;
    InputStreamReader newInputStreamReader = null;
    BufferedReader newIn = null;

    boolean firstNonBlankLineReached = false;

    try{

        //open the old UTF-16LE format file for reading
        oldInputStream = new FileInputStream(pOldFile);
        oldInputStreamReader = new InputStreamReader(oldInputStream, "UTF-16LE");
        oldIn = new BufferedReader(oldInputStreamReader);

        //open the new UTF-8 format file for reading
        newInputStream = new FileInputStream(pTempFile);
        newInputStreamReader = new InputStreamReader(newInputStream, "UTF-8");
        newIn = new BufferedReader(newInputStreamReader);

        String oldLine, newLine;

        //read lines from the old UTF-16LE file until end reached

        while ((oldLine = oldIn.readLine()) != null){

            //set flag when first non-blank line reached (the first blank line
            //actually contains a code byte left over from the UTF-16LE format
            //so look for a line with length > 1)
            if(oldLine.length() > 1) firstNonBlankLineReached = true;

            //toss all blank lines and the explanation lines at the beginning
            //the old UTF-16LE files had an explanation header -- these will
            //not be present in the new UTF-8 file

            if (!firstNonBlankLineReached) continue;
            if (oldLine.startsWith(";Do not erase")) continue;
            if (oldLine.startsWith(";To make a new")) continue;

            //read line from the new version to compare
            newLine = newIn.readLine();

            //for applicable files, ignore the format entry which was added
            //to the new file by the convertFile method
            if (ignoreFormatTypeEntry &&
                            newLine.startsWith("Job File Format=UTF-8")){
                newLine = newIn.readLine();
            }

            //if end of new file reached too soon, fail compare
            if (newLine == null){
                logFile.log("Compare fail - temp file is shorter.");
                compareGood = false; return(compareGood);
            }

            //if lines don't match, then exit with error
            if (!oldLine.equals(newLine)){
                logFile.log("Compare fail - lines don't match.");
                compareGood = false; return(compareGood);
            }

        }//while...

        //try to read one more line from new file -- this should fail unless
        //the new file has extra lines
        newLine = newIn.readLine();

        //if end of new file not reached, fail compare
        if (newLine != null){
            logFile.log("Compare fail - temp file is longer.");
            compareGood = false;
        }

    }//try
    catch (FileNotFoundException e){
        System.err.println(getClass().getName() + " - Error: 385");
        logFile.log("Compare fail - old or temp file not found.");
        compareGood = false;
    }//catch...
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 390");
        logFile.log("Compare fail - IO error for old or temp file.");
        compareGood = false;
    }//catch...
    finally{

        try{if (newIn != null) newIn.close();}
            catch(IOException e){compareGood = false;}
        try{if (newInputStreamReader != null) newInputStreamReader.close();}
            catch(IOException e){compareGood = false;}
        try{if (newInputStream != null) newInputStream.close();}
            catch(IOException e){compareGood = false;}

        try{if (oldIn != null) oldIn.close();}
            catch(IOException e){compareGood = false;}
        try{if (oldInputStreamReader != null) oldInputStreamReader.close();}
            catch(IOException e){compareGood = false;}
        try{if (oldInputStream != null) oldInputStream.close();}
            catch(IOException e){compareGood = false;}

        //if there was any error closing either file, fail the compare to be
        //on the safe side

        if(compareGood)
            logFile.log("Files are identical: " + oldFile);
        else
            logFile.log("Compare fail: " + oldFile);

        return(compareGood);

    }//finally

}//end of UTF16LEToUTF8Converter::compareFile
//-----------------------------------------------------------------------------

}//end of class UTF16LEToUTF8Converter
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
