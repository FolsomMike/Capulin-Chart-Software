/******************************************************************************
* Title: EndianTools.java
* Author: Mike Schoonover
* Date: 8/23/13
*
* Purpose:
*
* This class provides tools for working with variables:
*
* Reading and writing from files and sockets in Big Endian (Java style) or
* Little Endian (C/C++ style) order. Java provides reversing methods for
* some types such as Short and Integer, but not for Floats and Doubles.
*
* Usage:
* 
* If any non-static methods are to be used, each thread should create it's own
* object from this class. To save time, it may use class variables for the
* process of flipping bytes which would be corrupted if multiple threads shared
* the object.
*
* Reference:
*
* Part of this code is covered by General Public License:
*
* Utility class for doing byte swapping (i.e. conversion between
* little-endian and big-endian representations) of different data types.
* Byte swapping is typically used when data is read from a stream
* delivered by a system of different endian type as the present one.
*
* @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
*
* (C) 2004 - Geotechnical Software Services
*
* This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This code is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this program; if not, write to the Free
* Software Foundation, Inc., 59 Temple Place - Suite 330, Boston,
* MA  02111-1307, USA.
*
* package no.geosoft.cc.util;
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.tools;

//-----------------------------------------------------------------------------

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

//-----------------------------------------------------------------------------
// class EndianTools
//

public class EndianTools extends Object{


//-----------------------------------------------------------------------------
// EndianTools::EndianTools (constructor)
//

public EndianTools()
{

}//end of EndianTools::EndianTools (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::writeFloatLE
//
// Writes pValue to pOut in Little Endian order.
//

public static void writeFloatLE(float pValue, DataOutputStream pOut)
                                                            throws IOException
{

    //store the float in an int -- not the same as casting!
    int intValue = Float.floatToIntBits(pValue);

    //write in Little Endian order
    pOut.writeByte((byte) (intValue & 0xff));
    pOut.writeByte((byte) ((intValue >> 8) & 0xff));
    pOut.writeByte((byte) ((intValue >> 16) & 0xff));
    pOut.writeByte((byte) ((intValue >> 24) & 0xff));

}//end of EndianTools::writeFloatLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::writeFloatBE
//
// Writes pValue to pOut in Big Endian order.
//

public static void writeFloatBE(float pValue, DataOutputStream pOut)
                                                            throws IOException
{

    //store the float in an int -- not the same as casting!
    int intValue = Float.floatToIntBits(pValue);

    //write in Big Endian order
    pOut.writeByte((byte) ((intValue >> 24) & 0xff));
    pOut.writeByte((byte) ((intValue >> 16) & 0xff));
    pOut.writeByte((byte) ((intValue >> 8) & 0xff));    
    pOut.writeByte((byte) (intValue & 0xff));
    
}//end of EndianTools::writeFloatBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::readFloatLE
//
// Reads pValue from pIn in Little Endian order.
//

public static float readFloatLE(DataInputStream pIn) throws IOException
{

    //load four bytes from the stream as an int
    int intValue = pIn.readInt();

    //reverse the bytes, store as a float (not the same as casting, return
    return(Float.intBitsToFloat(Integer.reverseBytes(intValue)));

}//end of EndianTools::readFloatLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::readFloatBE
//
// Reads pValue from pIn in Big Endian order.
//

public static float readFloatBE(DataInputStream pIn) throws IOException
{

    //load four bytes from the stream as an int
    int intValue = pIn.readInt();

    //store as a float (not the same as casting, return
    return(Float.intBitsToFloat(intValue));

}//end of EndianTools::readFloatBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractSignedShort
//
// Converts the next two bytes (MSB first - Big Endian) in pArray to an integer,
// starting at position specified by pStartBufPos.
//
// The original value's sign will be preserved.
//
// Use this if the original value was signed.
//

public static int extractSignedShort(byte[] pArray, int pStartBufPos)
{

    return (short)((pArray[pStartBufPos++]<<8) & 0xff00)
                                        + (pArray[pStartBufPos++] & 0xff);
    
}//end of EndianTools::extractSignedShort
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractUnsignedShort
//
// Converts the next two bytes (MSB first - Big Endian) in pArray to an integer,
// starting at position specified by pStartBufPos.
//
// Use this if the original value was unsigned.
//

public static int extractUnsignedShort(byte[] pArray, int pStartBufPos)
{

    return (int)((pArray[pStartBufPos++]<<8) & 0xff00)
                                    + (pArray[pStartBufPos++] & 0xff);
    
}//end of EndianTools::extractUnsignedShort
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractIntBE
//
// Converts the next four bytes (MSB first - Big Endian) in pArray to an
// integer, starting at position specified by pStartBufPos.
//
// The original value's sign will be preserved.
//
// Use this if the original value was signed.
//

public static int extractIntBE(byte[] pArray, int pStartBufPos)
{

    return((pArray[pStartBufPos++]<<24) & 0xff000000) +
          ((pArray[pStartBufPos++]<<16) & 0x00ff0000) +
          ((pArray[pStartBufPos++]<<8) &  0x0000ff00) +
          (pArray[pStartBufPos++] & 0xff);

}//end of EndianTools::extractIntBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractIntLE
//
// Converts the next four bytes (LSB first - Little Endian) in pArray to an
// integer, starting at position specified by pStartBufPos.
//
// The original value's sign will be preserved.
//
// Use this if the original value was signed.
//

public static int extractIntLE(byte[] pArray, int pStartBufPos)
{

    return(  pArray[pStartBufPos++] & 0xff +
           ((pArray[pStartBufPos++]<<8) &  0xff00) +            
           ((pArray[pStartBufPos++]<<16) & 0xff0000) +
           ((pArray[pStartBufPos++]<<24) & 0xff000000)
            );

}//end of EndianTools::extractIntLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractFloatLE
//
// Converts the next four bytes (LSB first - Little Endian) in pArray to a
// float, starting at position specified by pStartBufPos.
//

public static float extractFloatLE(byte[] pArray, int pStartBufPos)
{

    //convert four bytes in array to an int as first step
    int intValue = extractIntLE(pArray, pStartBufPos);

    //store as a float (not the same as casting, return
    return(Float.intBitsToFloat(intValue));

}//end of EndianTools::extractFloatLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::extractFloatBE
//
// Converts the next four bytes (MSB first - Big Endian) in pArray to a
// float, starting at position specified by pStartBufPos.
//

public static float extractFloatBE(byte[] pArray, int pStartBufPos)
{

    //convert four bytes in array to an int as first step
    int intValue = extractIntBE(pArray, pStartBufPos);

    //store as a float (not the same as casting, return
    return(Float.intBitsToFloat(intValue));

}//end of EndianTools::extractFloatBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::unpackShortIntBE
//
// Unpacks pShortInt into pArray as two bytes, MSB first (Big Endian), at
// position specified by pStartBufPos.
//
// Use this to send the value as bytes in Big Endian order.
//

void unpackShortIntBE(int pShortInt, byte[] pArray, int pStartBufPos)
{

    pArray[pStartBufPos++] = (byte)((pShortInt >> 8) & 0xff);
    pArray[pStartBufPos++] = (byte)(pShortInt & 0xff);

}//end of EndianTools::sendShortIntBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::unpackShortIntLE
//
// Unpacks pShortInt into pArray as two bytes, LSB first (Little Endian), at
// position specified by pStartBufPos.
//
// Use this to send the value as bytes in Little Endian order.
//

void unpackShortIntLE(int pShortInt, byte[] pArray, int pStartBufPos)
{

    pArray[pStartBufPos++] = (byte)(pShortInt & 0xff);    
    pArray[pStartBufPos++] = (byte)((pShortInt >> 8) & 0xff);

}//end of EndianTools::sendShortIntLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::unpackIntBE
//
// Unpacks pInt into pArray as four bytes, MSB first (Big Endian), at position
// specified by pStartBufPos.
//
// Use this to send the value as bytes in Big Endian order.
//

void unpackIntBE(int pInt, byte[] pArray, int pStartBufPos)
{

    pArray[pStartBufPos++] = (byte)((pInt >> 24) & 0xff);
    pArray[pStartBufPos++] = (byte)((pInt >> 16) & 0xff);
    pArray[pStartBufPos++] = (byte)((pInt >> 8) & 0xff);
    pArray[pStartBufPos++] = (byte)(pInt & 0xff);

}//end of EndianTools::unpackIntBE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Simulator::unpackIntLE
//
// Unpacks pInt into pArray as four bytes, LSB first (Little Endian), at
// position specified by pStartBufPos.
//
// Use this to send the value as bytes in Little Endian order.
//

void unpackIntLE(int pInt, byte[] pArray, int pStartBufPos)
{

    pArray[pStartBufPos++] = (byte)(pInt & 0xff);    
    pArray[pStartBufPos++] = (byte)((pInt >> 8) & 0xff);
    pArray[pStartBufPos++] = (byte)((pInt >> 16) & 0xff);    
    pArray[pStartBufPos++] = (byte)((pInt >> 24) & 0xff);

}//end of EndianTools::unpackIntLE
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(short)
//
// Byte swap a single short value.
//
// @param value  Value to byte swap.
// @return       Byte swapped representation.
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static short swap(short value)
{

    int b1 = value & 0xff;
    int b2 = (value >> 8) & 0xff;

    return (short) (b1 << 8 | b2);

}//end of EndianTools::swap(short)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(int)
//
// Byte swap a single int value.
//
// @param value  Value to byte swap.
// @return       Byte swapped representation.
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static int swap(int value)
{

    int b1 = (value) & 0xff;
    int b2 = (value >>  8) & 0xff;
    int b3 = (value >> 16) & 0xff;
    int b4 = (value >> 24) & 0xff;

    return b1 << 24 | b2 << 16 | b3 << 8 | b4;

}//end of EndianTools::swap(int)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(long)
//
// Byte swap a single long value.
//
// @param value  Value to byte swap.
// @return       Byte swapped representation.
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static long swap(long value)
{
    long b1 = (value) & 0xff;
    long b2 = (value >>  8) & 0xff;
    long b3 = (value >> 16) & 0xff;
    long b4 = (value >> 24) & 0xff;
    long b5 = (value >> 32) & 0xff;
    long b6 = (value >> 40) & 0xff;
    long b7 = (value >> 48) & 0xff;
    long b8 = (value >> 56) & 0xff;

    return b1 << 56 | b2 << 48 | b3 << 40 | b4 << 32 |
           b5 << 24 | b6 << 16 | b7 <<  8 | b8;

}//end of EndianTools::swap(long)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(float)
//
// Byte swap a single float value.
//
// @param value  Value to byte swap.
// @return       Byte swapped representation.
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static float swap(float value)
{

    int intValue = Float.floatToIntBits(value);
    intValue = swap(intValue);
    return Float.intBitsToFloat(intValue);

}//end of EndianTools::swap(float)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(double)
//
// Byte swap a single double value.
//
// @param value  Value to byte swap.
// @return       Byte swapped representation.
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static double swap(double value)
{

    long longValue = Double.doubleToLongBits(value);
    longValue = swap(longValue);
    return Double.longBitsToDouble(longValue);

}//end of EndianTools::swap(double)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(short[])
//
// Byte swap an array of shorts.
// The result of the swapping is put back into the specified array.
//
// @param array  Array of values to swap
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static void swap(short[] array)
{

    for (int i = 0; i < array.length; i++) {
          array[i] = swap(array[i]);
    }

}//end of EndianTools::swap(short[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(int[])
//
// Byte swap an array of ints.
// The result of the swapping is put back into the specified array.
//
// @param array  Array of values to swap
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static void swap(int[] array)
{

    for (int i = 0; i < array.length; i++) {
          array[i] = swap (array[i]);
    }

}//end of EndianTools::swap(int[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(long[])
//
// Byte swap an array of longs.
// The result of the swapping is put back into the specified array.
//
// @param array  Array of values to swap
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static void swap(long[] array)
{

    for (int i = 0; i < array.length; i++) {
          array[i] = swap (array[i]);
    }

}//end of EndianTools::swap(long[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(float[])
//
// Byte swap an array of floats.
// The result of the swapping is put back into the specified array.
//
// @param array  Array of values to swap
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static void swap (float[] array)
{

    for (int i = 0; i < array.length; i++) {
          array[i] = swap (array[i]);
      }

}//end of EndianTools::swap(float[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EndianTools::swap(double[])
//
// Byte swap an array of doubles.
// The result of the swapping is put back into the specified array.
//
// @param array  Array of values to swap
//
// @author <a href="mailto:jacob.dreyer@geosoft.no">Jacob Dreyer</a>
// See GNU license at top of file.
//

public static void swap (double[] array)
{

    for (int i = 0; i < array.length; i++) {
          array[i] = swap (array[i]);
      }

}//end of EndianTools::swap(double[])
//-----------------------------------------------------------------------------

}//end of class EndianTools
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
