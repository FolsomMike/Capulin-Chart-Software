/******************************************************************************
* Title: SyncedInteger.java
* Author: Mike Schoonover
* Date: 8/25/12
*
* Purpose:
*
* This class manages a single variable by providing thread safe methods to
* set and read the value.
*
* Via its superclass SyncedVariable, it sets a flag when the value has been
* modified (usually by a sender object) and read (usually by a receive object).
* The object can also be managed by a SyncedVariableSet so that multiple
* variables can be quickly checked for modified data.
*
* See the superclass SyncedVariable and the manager class SyncedVariableSet
* for more details.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.threadsafe;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SyncedInteger
//

public class SyncedInteger extends SyncedVariable{


    private int value;

//-----------------------------------------------------------------------------
// SyncedInteger::SyncedInteger (constructor)
//
// If the object is to be managed by a SyncedVariableSet object, a pointer to
// that manager should be passed via pSyncedVariableSet.  If no manager, then
// pass that parameter as null.
//

public SyncedInteger(SyncedVariableSet pSyncedVariableSet)
{

    super(pSyncedVariableSet);

}//end of SyncedInteger::SyncedInteger (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::setValue
//
// Sets the value to pValue, sets the dataChangedFlag, and notifies the
// manager of the change.
//

public synchronized void setValue(Integer pValue)
{

    value = pValue;
    setDataChangedTrue();

}//end of SyncedInteger::setValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::applyValue
//
// Returns the value, clears the dataChanged flag, and notifies the manager
// object.
//
// This method should be called by the receiver object when it is ready to
// apply the modified value.  To read the value without affecting the
// dataChanged flag, use getValue instead.
//

public synchronized Integer applyValue()
{

    setDataChangedFalse();
    return(value);

}//end of SyncedInteger::applyValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::getValue
//
// Returns the value BUT DOES NOT clear the dataChanged flag or notify the
// manager object.  If the receive object wishes to get the value and apply it
// then it should call applyValue.
//
// This function allows a non-receiver object to check on the value while still
// ensuring that the dataChanged flag stays set until the designated receiver
// object obtains the value.
//

public synchronized Integer getValue()
{

    return(value);

}//end of SyncedInteger::getValue
//-----------------------------------------------------------------------------


}//end of class SyncedInteger
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
