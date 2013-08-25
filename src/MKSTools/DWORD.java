/******************************************************************************
* Title: DWORD.java
* Author: Mike Schoonover
* Date: 8/23/13
*
* Purpose:
*
* This class mimics the Microsoft DWORD programming type which is an unsigned
* 32 bit integer.
*
* Since the object takes up more space than the primitive types, if a large
* number (such as an array) are to be used it can be more efficient to keep
* the DWORDs as long ints and then run them through a DWORD object just for
* printing.
*
* Java doesn't have unsigned ints, so a 64 bit long int is used to hold the
* value. As the WORD will never be greater than 4,294,967,295, it will never
* affect the sign bit of the 64 bit long int.
*
* When saved to a file, only the lower four bytes are saved -- in Little Endian
* order as opposed to Java's Big Endian order for compatibility with Microsoft
* C, C++, C#.
*
* Reference:
*
* DWORD:
*      A 32-bit unsigned integer. The range is 0 through 4294967295 decimal.
*      This type is declared in IntSafe.h as follows:
*      typedef unsigned long DWORD;
*
*/


package MKSTools;

//-----------------------------------------------------------------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class DWORD
//

public class DWORD extends Object{

public long value;

//-----------------------------------------------------------------------------
// DWORD::DWORD (constructor)
//

public DWORD(long pValue)
{

    value = pValue;

}//end of DWORD::DWORD (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DWORD::write
//
// Writes the four least significant bytes to pFile, Little Endian order.
//

public void write(DataOutputStream pFile) throws IOException
{

    pFile.writeByte((byte) (value & 0xff));
    pFile.writeByte((byte) ((value >> 8) & 0xff));
    pFile.writeByte((byte) ((value >> 16) & 0xff));
    pFile.writeByte((byte) ((value >> 24) & 0xff));

}//end of DWORD::write
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// DWORD::read
//
// Reads four bytes from pFile, Little Endian order, reversing the order to
// be Big Endian for Java compatibility.
//

public void read(DataInputStream pFile) throws IOException
{

    value =    (pFile.readByte() & 0xff)
            + ((pFile.readByte() << 8)   & 0xff00)
            + ((pFile.readByte() << 16)  & 0xff0000)
            + ((pFile.readByte() << 24)  & 0xff000000);

}//end of DWORD::read
//-----------------------------------------------------------------------------

}//end of class DWORD
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
