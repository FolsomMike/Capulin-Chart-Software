/******************************************************************************
* Title: SyncedIntArray.java
* Author: Mike Schoonover
* Date: 9/24/15
*
* Purpose:
*
* This class manages an integer array by providing thread safe methods to
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
// class SyncedIntArray
//

public class SyncedIntArray extends SyncedVariable{


    private int[] values;

//-----------------------------------------------------------------------------
// SyncedIntArray::SyncedIntArray (constructor)
//
// If the object is to be managed by a SyncedVariableSet object, a pointer to
// that manager should be passed via pSyncedVariableSet.  If no manager, then
// pass that parameter as null.
//

public SyncedIntArray(SyncedVariableSet pSyncedVariableSet)
{

    super(pSyncedVariableSet);

}//end of SyncedIntArray::SyncedIntArray (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::init
//

@Override
public void init()
{

    super.init();
   
    values = new int[1]; values[0] = Integer.MAX_VALUE; //create a default array

}//end of SyncedIntArray::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::setValues
//
// Sets the values to pValues, sets the dataChangedFlag, and notifies the
// manager of the change.
//
// If pForceUpdate is false, the values will only be updated if the new values
// are different than the current values. If true, then the values will be
// updated regardless of the current values -- this allows the dataChangedFlag
// to be forcibly set.
//

public synchronized void setValues(int[] pValues, boolean pForceUpdate)
{

    if (pForceUpdate || !valuesEqual(pValues)){
        
        updateValues(pValues);    
        setDataChangedTrue();

    }

}//end of SyncedIntArray::setValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::setValuesFromString
//
// Sets the values to series extracted from comma delimited list in
// pValuesString, sets the dataChangedFlag, and notifies the manager of the
// change.
//
// Entries in the list which cannot be converted to an integer will be replaced
// in values with Integer.MAX_VALUE.
//
// If no valid values are found in the string, values is set to an array of
// one element equal to Integer.MAX_VALUE.
//
// If pForceUpdate is false, the values will only be updated if the new values
// are different than the current values. If true, then the values will be
// updated regardless of the current values -- this allows the dataChangedFlag
// to be forcibly set.
//

public synchronized void setValuesFromString(String pValuesString,
                                                          boolean pForceUpdate)
{

    String[] valuesSplit = pValuesString.split(",");
    
    int[] newValues;
    
    if(valuesSplit.length > 0){
    
        newValues = new int[valuesSplit.length];
        
        for (int i=0; i<newValues.length; i++){
            try{
                newValues[i] = Integer.parseInt(valuesSplit[i].trim());
            }
            catch(NumberFormatException e){
                newValues[i] = Integer.MAX_VALUE;
            }
        }
        
    }else{
        newValues = new int[1]; newValues[0] = Integer.MAX_VALUE;
    }

    if (pForceUpdate || !valuesEqual(newValues)){
        
        updateValues(newValues);    
        setDataChangedTrue();

    }

}//end of SyncedIntArray::setValuesFromString
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::valuesEqual
//
// Compares the values in the values array with pValues. If any value is
// different or the lengths of the arrays differs, returns false. Returns true
// if the arrays are identical.
//

private boolean valuesEqual(int[] pValues)
{

    if (values.length != pValues.length){ return(false); }
    
    for(int i=0; i<values.length; i++){        
        if (values[i] != pValues[i]){ return(false); }    
    }
    
    return(true);
    
}//end of SyncedIntArray::valuesEqual
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::updateValues
//
// Creates a new values array and copies pValues into it. A new array is created
// in case pValues is a different length.
//

private boolean updateValues(int[] pValues)
{

    values = new int[pValues.length];
    
    System.arraycopy(pValues, 0, values, 0, values.length);
    
    return(true);
    
}//end of SyncedIntArray::updateValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::applyValues
//
// Returns the valuse, clears the dataChanged flag, and notifies the manager
// object.
//
// This method should be called by the receiver object when it is ready to
// apply the modified values.  To read the values without affecting the
// dataChanged flag, use getValues instead.
//

public synchronized int[] applyValues()
{

    setDataChangedFalse();
    return(values);

}//end of SyncedIntArray::applyValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::getValues
//
// Returns the values BUT DOES NOT clear the dataChanged flag or notify the
// manager object.  If the receive object wishes to get the values and apply
// them then it should call applyValues.
//
// This function allows a non-receiver object to check on the value while still
// ensuring that the dataChanged flag stays set until the designated receiver
// object obtains the value.
//

public synchronized int[] getValues()
{

    return(values);

}//end of SyncedIntArray::getValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// SyncedIntArray::toString
//
// Returns the values as a comma delimited string of values BUT DOES NOT clear
// the dataChanged flag or notify the manager object.  If the receive object
// wishes to get the values and apply them then it should call applyValues.
//
// This function allows a non-receiver object to check on the value while still
// ensuring that the dataChanged flag stays set until the designated receiver
// object obtains the value.
//

@Override
public String toString()
{

    String valueString = "";
    
    for(int i=0; i<values.length; i++){
        valueString += values[i];
        if(i<values.length-1){ valueString += ","; }
    }
    
    return(valueString);

}//end of SyncedIntArray::toString
//-----------------------------------------------------------------------------

}//end of class SyncedIntArray
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
