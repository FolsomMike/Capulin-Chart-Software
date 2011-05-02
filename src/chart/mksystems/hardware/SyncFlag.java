/******************************************************************************
* Title: SyncFlag.java
* Author: Mike Schoonover
* Date: 5/1/11
*
* Purpose:
*
* This class encapsulates a boolean for use in passing triggers between objects.
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
// class SyncFlag
//
// This class encapsulates a boolean for use in passing triggers between
// objects.
//

public class SyncFlag extends Object{

boolean flag = false;

//-----------------------------------------------------------------------------
// SyncFlag::SyncFlag (constructor)
//
//

public SyncFlag()
{

}//end of SyncFlag::SyncFlag (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncFlag::set
//
// Synchronized method to set the flag.
//

public synchronized void set(boolean pState)
{

flag = pState;
    
}//end of SyncFlag::set
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncFlag::getAndClear
//
// Synchronized method to read and clear the value of the flag.
//
// Returns true if the flag was true, false otherwise.  Always clears the flag.
//

public synchronized boolean getAndClear()
{

boolean flagNow = flag;

flag = false;

return(flagNow);

}//end of SyncFlag::getAndClear
//-----------------------------------------------------------------------------

}//end of class SyncFlag
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
