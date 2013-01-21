/******************************************************************************
* Title: RemoteParam.java
* Author: Mike Schoonover
* Date: 5/1/11
*
* Purpose:
*
* This class encapsulates a value which can be transmitted to a remote device.
* It provides flags to allow one thread to modify the value and signal a
* second thread (the owner of the communication link) to send the value.
*
* WARNING: This class can handle various data types such as int, double,
* boolean -- more can be added as needed.  Be sure to always use the matching
* setInt/getInt, setDouble/getDouble etc. for the data type in used
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
// class RemoteParam
//
// This class encapsulates a value which can be transmitted to a remote device.
// It provides flags to allow one thread to modify the value and signal a
// second thread (the owner of the communication link) to send the value.
//
// All methods which modify the values or the signalling flags are synchronized
// to protect against thread collision.
//

public class RemoteParam extends Object{

    double doubleValue = 0;
    int intValue = 0;
    boolean booleanValue = false;

    boolean dataChanged;

    SyncFlag dataChanged1, dataChanged2, dataChanged3, dataChanged4;

//-----------------------------------------------------------------------------
// RemoteParam::RemoteParam (constructor)
//
// The SyncFlag objects are flags which must be set true when the data in this
// object is changed and cleared when it is retrieved via the xmt methods.
//
// These flags are supplied by the creator and are those which cause an external
// object to process changes to values in this object.
//
// A null value may be passed in as a SyncFlag, in which case it will be
// ignored.
//

public RemoteParam(SyncFlag pDataChanged1, SyncFlag pDataChanged2,
                                SyncFlag pDataChanged3, SyncFlag pDataChanged4)
{

    dataChanged1 = pDataChanged1;
    dataChanged2 = pDataChanged2;
    dataChanged3 = pDataChanged3;
    dataChanged4 = pDataChanged4;

}//end of RemoteParam::RemoteParam (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::setDouble
//
// Sets the double value.  Use getDouble to retrieve it (not synchronized and
// no data changed flags cleared), or xmtDouble to retrieve it for sending to
// remote device (clears the data changed flags to signal that data change has
// been processed)
//

public synchronized void setDouble(double pDouble)
{

    doubleValue = pDouble;

    setDataChangedFlags(true);

}//end of RemoteParam::setDouble
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::getDouble
//
// Returns the double value without clearing the dataChanged flags.  Use this
// for simply retrieving the value for uses other than transmitting or
// or processing it.
//

public double getDouble()
{

    return(doubleValue);

}//end of RemoteParam::getDouble
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::xmtDouble
//
// Returns the double value and clears the dataChanged flags.  Use this
// for retrieving the value for transmitting or processing it.
//

public synchronized double xmtDouble()
{

    setDataChangedFlags(false);

    return(doubleValue);

}//end of RemoteParam::xmtDouble
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::setInt
//
// Sets the int value.  Use getInt to retrieve it (not synchronized and
// no data changed flags cleared), or xmtInt to retrieve it for sending to
// remote device (clears the data changed flags to signal that data change has
// been processed)
//

public synchronized void setInt(int pInt)
{

    intValue = pInt;

    setDataChangedFlags(true);

}//end of RemoteParam::setInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::getInt
//
// Returns the int value without clearing the dataChanged flags.  Use this
// for simply retrieving the value for uses other than transmitting or
// or processing it.
//

public int getInt()
{

    return(intValue);

}//end of RemoteParam::getInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::xmtInt
//
// Returns the int value and clears the dataChanged flags.  Use this
// for retrieving the value for transmitting or processing it.
//

public synchronized int xmtInt()
{

    setDataChangedFlags(false);

    return(intValue);

}//end of RemoteParam::xmtInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::setBoolean
//
// Sets the boolean value.  Use getBoolean to retrieve it (not synchronized and
// no data changed flags cleared), or xmtBoolean to retrieve it for sending to
// remote device (clears the data changed flags to signal that data change has
// been processed)
//

public synchronized void setBoolean(boolean pBoolean)
{

    booleanValue = pBoolean;

    setDataChangedFlags(true);

}//end of RemoteParam::setBoolean
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::getBoolean
//
// Returns the boolean value without clearing the dataChanged flags.  Use this
// for simply retrieving the value for uses other than transmitting or
// or processing it.
//

public boolean getBoolean()
{

    return(booleanValue);

}//end of RemoteParam::getBoolean
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::xmtBoolean
//
// Returns the boolean value and clears the dataChanged flags.  Use this
// for retrieving the value for transmitting or processing it.
//

public synchronized boolean xmtBoolean()
{

    setDataChangedFlags(false);

    return(booleanValue);

}//end of RemoteParam::xmtBoolean
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::isDataChanged
//
// Returns true if the data has been changed, false otherwise.
//

public boolean isDataChanged()
{

    return(dataChanged);

}//end of RemoteParam::isDataChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// RemoteParam::setDataChangedFlags
//
// Sets true or false all the data changed flags passed to this object on
// instantiation.
//
// These flags should be those which cause the appropriate external object to
// process any changes to this object's values.
//
// Some of the flag objects may have been passed in as NULL, so ignore those.
//

public synchronized void setDataChangedFlags(boolean pState)
{

    dataChanged = pState; //set this object's flag

    //set all other flags which tell outside objects that data has changed

    if (dataChanged1 != null) dataChanged1.flag = pState;
    if (dataChanged2 != null) dataChanged2.flag = pState;
    if (dataChanged3 != null) dataChanged3.flag = pState;
    if (dataChanged4 != null) dataChanged4.flag = pState;

}//end of RemoteParam::setChangedFlags
//-----------------------------------------------------------------------------

}//end of class RemoteParam
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
