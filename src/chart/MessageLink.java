/******************************************************************************
* Title: MessageLink.java
* Author: Mike Schoonover
* Date: 12/24/11
*
* Purpose:
*
* This file contains the interface definition for MessageLink.  This interface
* provides functions with which objects can communicate with each other, mainly
* by sending messages or retrieving messages.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// interface MessageLink
//
// Defines functions to allow different objects to call functions in other
// objects.
//

public interface MessageLink {

    //modes

    static int FORWARD = 0;
    static int REVERSE = 1;
    static int STOP = 2;
    static int RESET = 3;

    //commands
    static int SET_MODE = 1;

    //status flags

    static int NULL = 0;

    public int xmtMessage(int pMessage, int pValue);

}//end of interface MessageLink
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
