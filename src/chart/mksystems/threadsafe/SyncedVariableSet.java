/******************************************************************************
* Title: SyncedVariableSet.java
* Author: Mike Schoonover
* Date: 8/25/12
*
* Purpose:
*
* This class handles a set of synchronized variables making them safe to
* access by different threads.
*
* The class creates an array to hold pointers to a collection of objects of
* the SyncedVariable class.  The owner creates each type of SyncedVariable it
* needs and passes a pointer to each of those objects into the addVariable
* method of this class.
*
* This class maintains a single dataChangedMaster variable that should be set
* by the SyncedVariables if any have their dataChanged flag set.  Thus, the
* owner can quickly tell if any data needs to be handled by looking only at the
* dataChangedMaster variable instead of scanning through each SyncedVariable
* checking for changes.
*
* All SyncedVariable objects managed by an object of this class should call
* this class notifyDataChanged method when their data has been modified.
* They should call notifyDataUnchanged when their changed data has been
* read and applied by the appropriate object.
*
* Any object needing to know if any SyncedVariable in the list has modified
* data can call getDataChangedMaster which will return true if any managed
* object has modified data.
*
* All methods in this class and the SyncedVariable classes should be
* synchronized to be sure there is no question about when any operation is
* thread safe.
*
* IMPORTANT:  This class cannot call any synchronized methods in any of the
*  SyncedVariable objects it controls.  This can cause a thread lock up if
*  another thread is accessing one of those objects simultaneously.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.threadsafe;

import java.util.ArrayList;
import java.util.ListIterator;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class SyncedVariableSet
//

public class SyncedVariableSet extends Object{


    private boolean dataChangedMaster = false;

    private ArrayList<SyncedVariable> variableList;

//-----------------------------------------------------------------------------
// SyncedVariableSet::SyncedVariableSet (constructor)
//
//

public SyncedVariableSet()
{

    //create an ArrayList for the SyncedVariable objects managed by this object
    variableList = new ArrayList<SyncedVariable>(100);

}//end of SyncedVariableSet::SyncedVariableSet (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariableSet::addVariable
//
// Adds a SyncedVariable object to the list tracked by this class.
//

public synchronized void addVariable(SyncedVariable pSyncedVariable)
{

    variableList.add(pSyncedVariable);

}//end of SyncedVariableSet::addVariable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariableSet::notifyDataChanged
//
// Sets the dataChangedMaster flag true.  Should be called by any SyncedVariable
// managed by this object when its data has been modified.
//

public synchronized void notifyDataChanged()
{

    dataChangedMaster = true;

}//end of SyncedVariableSet::notifyDataChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariableSet::getDataChangedMaster
//
// Returns the dataChangedMaster flag true.  Should be called by object which
// needs to know if any variable managed by this object has been modified.
//

public synchronized boolean getDataChangedMaster()
{

    return(dataChangedMaster);

}//end of SyncedVariableSet::getDataChangedMaster
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedVariableSet::notifyDataUnchanged
//
// All SyncedVariables managed by this object should call this method when
// their data becomes unmodified, i.e. any changes have been read and applied as
// necessary by some external object.
//
// This method will then check all SyncedVariable objects in its list.  If ALL
// are unmodifed, then it will set the dataChangedMaster flag to false to show
// that no modified data is present in any of the SyncedVariables.  If ANY
// are modified, the master flag is set true.
//
// Note that the unsynchronized version of getDataChanged is called in the
// SyncedVariable objects.  This is done to avoid thread lockup when another
// thread happens to be in the SyncedVariable::setValue method which will try
// to call notifyDataChanged in this class.  Thus, one thread is in this class
// trying to access getDataChanged method in the variable oject while the other
// thread is in the variable object trying to access notifyDataChanged in this
// class -- each thread blocks the other.
//
// The unsynchonized version works because the flag is atomic and is set
// by the calling object prior to calling.
//

public synchronized void notifyDataUnchanged()
{

    boolean someDataChanged = false;
    ListIterator i;

    SyncedVariable sv;

    int debug = 0;

    for (i = variableList.listIterator(); i.hasNext(); ){
        sv = (SyncedVariable)i.next();

        debug++;

        if ( sv.getDataChangedUnSynced()) {
            someDataChanged = true;
            break;
        }
    }

    //set the master flag true if ANY SyncedVariable flag is true, only set
    //false if NONE are true
    dataChangedMaster = someDataChanged;

}//end of SyncedVariableSet::notifyDataUnchanged
//-----------------------------------------------------------------------------

}//end of class SyncedVariableSet
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
