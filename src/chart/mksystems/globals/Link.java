/******************************************************************************
* Title: Link.java
* Author: Mike Schoonover
* Date: 11/24/03
*
* Purpose:
*
* This file contains the interface definition for Link.  This interface provides
* functions with which objects can communicate with each other.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.globals;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// interface Link
//
// Defines functions to allow different objects to call functions in other
// objects.
//

public interface Link {

void changeLanguage(String pLanguage);

}//end of interface Link
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

