/******************************************************************************
* Title: CharBuf.java
* Author: Mike Schoonover
* Date: 8/23/13
*
* Purpose:
*
* This class handles Strings as if they were character arrays for the purpose
* of writing to files. When written, the output is null terminated and any
* unused characters are written as zeroes. This ensures that the output files
* have consistent lengths regardless of the length of the strings.
*
*
* This class accepts a string with a maximum length for writing to a file.
*
* The string can be any length, but only maximum number of characters will be
* written to file. If the string is less than the maximum, the extra
* characters will be filled with zeroes.
*
* Note that the actual number of characters allowed is maxWrite-1 as the
* null terminator takes up one character.
*
*/

package chart.mksystems.tools;

//-----------------------------------------------------------------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class CharBuf extends Object{

    public String string;
    public int maxWrite;

//-----------------------------------------------------------------------------
// CharBuf::CharBuf (constructor)
//

public CharBuf(String pString, int pMaxWrite)
{

    string = pString;
    maxWrite = pMaxWrite;

}//end of CharBuf::CharBuf (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CharBuf::set
//
// Sets the value and the maximum file write length.
//

public void set(String pString, int pMaxWrite)
{

    string = pString;
    maxWrite = pMaxWrite;

}//end of CharBuf::set
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CharBuf::write
//
// Writes string to pFile as a character buffer, adding a null terminator
// and writing missing characters as zeroes to pad up to maxWrite number of
// characters.
//

public void write(DataOutputStream pFile) throws IOException
{

    int i = 0;

    //write the characters in the string up to maxWrite-1 characters
    while (i < maxWrite-1 && i < string.length()){
        pFile.writeByte((byte)string.charAt(i));
        i++;
    }

    //write the null string terminator and fill left over bytes with same
    while (i++ < maxWrite){
        pFile.writeByte(0);
    }

}//end of CharBuf::write
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// CharBuf::read
//
// Reads maxWrite characters from pFile as a character buffer, and converts
// to a string.
//

public void read(DataInputStream pFile) throws IOException
{

    string = "";

    //read maxWrite characters
    for (int i = 0; i < maxWrite; i++){
        string = string + (char)pFile.readByte();

        //char c = (char)(((bytes[bpos]&0x00FF)<<8) + (bytes[bpos+1]&0x00FF));
    }

}//end of CharBuf::read
//-----------------------------------------------------------------------------


}//end of class CharBuf
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
