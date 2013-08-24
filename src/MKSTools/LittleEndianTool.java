/******************************************************************************
* Title: LittleEndianTool.java
* Author: Mike Schoonover
* Date: 8/23/13
*
* Purpose:
*
* If any non-static methods are to be used, each thread should create it's own
* object from this class. To save time, it may use class variables for the
* process of flipping bytes which would be corrupted if multiple threads shared
* the object.
*
* This class provides tools for working with variables:
*
* Reading and writing from files and sockets in Little Endian order as opposed
* to Big Endian Order as done by Java. Java provides reversing methods for
* some types such as Short and Integer, but not for Floats and Doubles.
*
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

package MKSTools;

//-----------------------------------------------------------------------------

import java.io.DataOutputStream;
import java.io.IOException;

//-----------------------------------------------------------------------------
// class LittleEndianTool
//

public class LittleEndianTool extends Object{


//-----------------------------------------------------------------------------
// LittleEndianTool::LittleEndianTool (constructor)
//

public LittleEndianTool()
{

}//end of LittleEndianTool::LittleEndianTool (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::writeFloat
//
// Writes pValue to pFile in Little Endian order.
//

public static void writeFloat(float pValue, DataOutputStream pFile)
                                                            throws IOException
{

    //store the float in an int -- not the same as casting!
    int intValue = Float.floatToIntBits(pValue);

    //write in Little Endian order
    pFile.writeByte((byte) (intValue & 0xff));
    pFile.writeByte((byte) ((intValue >> 8) & 0xff));
    pFile.writeByte((byte) ((intValue >> 16) & 0xff));
    pFile.writeByte((byte) ((intValue >> 24) & 0xff));

}//end of LittleEndianTool::writeFloat
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(short)
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

}//end of LittleEndianTool::swap(short)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(int)
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

}//end of LittleEndianTool::swap(int)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(long)
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

}//end of LittleEndianTool::swap(long)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(float)
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

}//end of LittleEndianTool::swap(float)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(double)
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

}//end of LittleEndianTool::swap(double)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(short[])
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

}//end of LittleEndianTool::swap(short[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(int[])
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

}//end of LittleEndianTool::swap(int[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(long[])
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

}//end of LittleEndianTool::swap(long[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(float[])
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

}//end of LittleEndianTool::swap(float[])
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// LittleEndianTool::swap(double[])
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

}//end of LittleEndianTool::swap(double[])
//-----------------------------------------------------------------------------

}//end of class LittleEndianTool
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
