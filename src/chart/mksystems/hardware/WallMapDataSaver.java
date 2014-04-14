/******************************************************************************
* Title: WallMapDataSaver.java
* Author: Mike Schoonover
* Date: 8/22/13
*
* Purpose:
*
* This is the parent class for those which save wall data collected by a
* UTBoard in a various file formats.
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

import chart.mksystems.inifile.IniFile;

// class WallMapDataSaver

public class WallMapDataSaver extends Object{

    static final int IRNDT_TEXT_FORMAT = 1;
    static final int IRNDT_BINARY_FORMAT = 2;
    static final int TUBO_BINARY_FORMAT = 3;

    IniFile jobInfoFile = null;

    EncoderValues encoderValues;
    
//-----------------------------------------------------------------------------
// WallMapDataSaver::WallMapDataSaver (constructor)
//

public WallMapDataSaver()
{



}//end of WallMapDataSaver::WallMapDataSaver (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init(MapSourceBoard pMapSourceBoards[],
                                                  EncoderValues pEncoderValues)
{

    encoderValues = pEncoderValues;
    
}//end of WallMapDataSaver::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::saveToFile
//
// Saves data to a file.
//
// Subclasses should override this method to provide custom functionality
//

public void saveToFile(String pFilename)
{


}//end of WallMapDataSaver::saveToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMapDataSaver::loadFromFile
//
// Loads data from a file.
//
// Subclasses should override this method to provide custom functionality
//

void loadFromFile(String pFilename)
{


}//end of WallMapDataSaver::loadFromFile
//-----------------------------------------------------------------------------


}//end of class WallMapDataSaver
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
