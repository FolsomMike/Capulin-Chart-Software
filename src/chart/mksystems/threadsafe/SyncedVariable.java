/******************************************************************************
* Title: SyncedVariable.java
* Author: Mike Schoonover
* Date: 8/25/12
*
* Purpose:
*
* This class manages a single variable by providing thread safe methods to
* set and read the value.
*
* Objects of this class can be added to a SyncedVariableSet object so that the
* changed value status of multiple variables can be quickly detected by receiver
* objects.
*
* See the SyncedVariableSet class header notes for more details.
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
// class SyncedVariable
//

public class SyncedVariable extends Object{


    SyncedVariableSet syncedVariableSet;

    private boolean dataChanged = false;

//-----------------------------------------------------------------------------
// SyncedVariableSet::SyncedVariable (constructor)
//
// If the object is to be managed by a SyncedVariableSet object, a pointer to
// that manager should be passed via pSyncedVariableSet.  If no manager, then
// pass that parameter as null.
//

public SyncedVariable(SyncedVariableSet pSyncedVariableSet)
{

    syncedVariableSet = pSyncedVariableSet;

}//end of SyncedVariable::SyncedVariable (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariableSet::init
//

//

public void init()
{

    if(syncedVariableSet != null) syncedVariableSet.addVariable(this);

}//end of SyncedVariable::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::setDataChangedTrue
//
// Sets the dataChanged flag to true and notifies the manager object of the
// change.
//

public synchronized void setDataChangedTrue()
{

    dataChanged = true;
    if (syncedVariableSet != null) syncedVariableSet.notifyDataChanged();

}//end of SyncedVariableSet::setDataChangedTrue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::setDataChangedFalse
//
// Sets the dataChanged flag to false and notifies the manager object.
//

public synchronized void setDataChangedFalse()
{
    dataChanged = false;
    if (syncedVariableSet != null) syncedVariableSet.notifyDataUnchanged();

}//end of SyncedVariableSet::setDataChangedFalse
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::getDataChanged
//
// Returns the dataChanged flag.
//

public synchronized boolean getDataChanged()
{

    return(dataChanged);

}//end of SyncedVariable::getDataChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariable::getDataChangedUnSynced
//
// Returns the dataChanged flag.
//
// IMPORTANT: This unsynchronized version is meant for use only by a
// SyncedVariableSet object to avoid thread blocking.
//

public boolean getDataChangedUnSynced()
{

    return(dataChanged);

}//end of SyncedVariable::getDataChangedUnSynced
//-----------------------------------------------------------------------------


}//end of class SyncedVariable
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
