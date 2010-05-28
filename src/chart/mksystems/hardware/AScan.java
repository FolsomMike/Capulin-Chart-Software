/******************************************************************************
* Title: AScan.java
* Author: Mike Schoonover
* Date: 7/31/09
*
* Purpose:
*
* This class handles the data set for an AScan.
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
//-----------------------------------------------------------------------------
// class AScan
//
//

public class AScan extends Object{

public int bufSize = 1;

public int range;
public int interfaceCrossingPosition;

public int[] buffer;

//-----------------------------------------------------------------------------
// AScan::AScan (constructor)
//
//

public AScan(int pBufSize)
{

bufSize = pBufSize;

buffer = new int[bufSize];

}//end of AScan::AScan (constructor)
//-----------------------------------------------------------------------------

}//end of class AScan
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

