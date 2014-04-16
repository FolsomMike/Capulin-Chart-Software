/******************************************************************************
* Title: MapSourceBoard.java
* Author: Mike Schoonover
* Date: 8/26/13
*
* Purpose:
*
* This class handles boards which are configured to provide data for mapping.
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
// class MapSourceBoard
//
// This class handles variables related to a source board -- a board designated
// to provide data for mapping.
//

public class MapSourceBoard extends Object{

    UTBoard utBoard;
    short dataBuffer[];

    int revolutionStartIndex;
    int revolutionEndIndex;

    int inspectionStartIndex = -1;
    int inspectionStopIndex = -1;

    int revStartIndex = -1;
    int revEndIndex = -1;
    int numRevs = 0;
    int numSamplesInRev = 0;
    int avgNumSamplesPerRev = 0;

    int sampleIndex = 0;

//-----------------------------------------------------------------------------
// MapSourceBoard::MapSourceBoard (constructor)
//

public void Sourceboard()
{

}//end of MapSourceBoard::MapSourceBoard (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapSourceBoard::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//
// debug mks -- this crashes on startup sometimes with utBoard null
//

public void init(UTBoard pUTBoard)
{

    utBoard = pUTBoard;

    dataBuffer = utBoard.getDataBuffer();

}//end of MapSourceBoard::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MapSourceBoard::setUpForSavingData
//
// Prepares variables for finding revolutions and anything require for
// manipulating the data for saving.
//

public void setUpForSavingData()
{

    revolutionStartIndex = -1;
    revolutionEndIndex = -1;

}//end of MapSourceBoard::MapsetUpForSavingData
//-----------------------------------------------------------------------------

}//end of class MapSourceBoard
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
