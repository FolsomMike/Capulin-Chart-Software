/******************************************************************************
* Title: WORD.java
* Author: Mike Schoonover
* Date: 8/23/13
*
* Purpose:
*
* This class mimics the Microsoft WORD programming type which is an unsigned
* 16 bit integer.
*
* Since the object takes up more space than the primitive types, if a large
* number (such as an array) are to be used it can be more efficient to keep
* the WORDs as ints and then run them through a WORD object just for printing.
*
* Java doesn't have unsigned short ints, so a 32 bit int is used to hold the
* value. As the WORD will never be greater than 65,535, it will never affect the
* sign bit of the 32 bit int.
*
* When saved to a file, only the lower two bytes are saved -- in Little Endian
* order as opposed to Java's Big Endian order for compatibility with Microsoft
* C, C++, C#.
*
* Reference:
*
*  WORD:
*      A 16-bit unsigned integer. The range is 0 through 65535 decimal.
*      This type is declared in WinDef.h as follows:
*      typedef unsigned short WORD;
*
*/


package MKSTools;

//-----------------------------------------------------------------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class WORD
//

public class WORD extends Object{

public int value;

//-----------------------------------------------------------------------------
// WORD::WORD (constructor)
//

public WORD(int pValue)
{

    value = pValue;

}//end of WORD::WORD (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WORD::write
//
// Writes the two least significant bytes to pFile, Little Endian order.
//

public void write(DataOutputStream pFile) throws IOException
{

    pFile.writeByte((byte) (value & 0xff));
    pFile.writeByte((byte) ((value >> 8) & 0xff));

}//end of WORD::write
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WORD::read
//
// Reads two bytes from pFile, Little Endian order, reversing the order to
// be Big Endian for Java compatibility.
//

public void read(DataInputStream pFile) throws IOException
{

    value =    (pFile.readByte() & 0xff)
            + ((pFile.readByte() << 8)   & 0xff00);
    
}//end of WORD::read
//-----------------------------------------------------------------------------


}//end of class WORD
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
