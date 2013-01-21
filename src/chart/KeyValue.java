/******************************************************************************
* Title: KeyValue.java
* Author: Mike Schoonover
* Date: 9/7/11
*
* Purpose:
*
* This class contains a set of generic variable key/value pairs.  This is
* useful since Java won't allow functions to return values via reference.
*
* More variables can be added as necessary to handle all required types of
* keys and values.
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
// class KeyValue
//
// See notes at the top of the page.
//

public class KeyValue extends Object{

    public String keyString;
    public String valueString;

    public int keyInt;
    public int valueInt;

}//end of class KeyValue
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
