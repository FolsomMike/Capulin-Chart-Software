/******************************************************************************
* Title: TraceValueCalculator.java
* Author: Mike Schoonover
* Date: 3/18/08
*
* Purpose:
*
* This file contains the interface definition for functions useful in 
* converting the height of a trace to a real world value.
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
// interface TraceValueCalculator
//
// Defines functions to allow different objects to call functions in other
// objects.
//

public interface TraceValueCalculator {

public double calculateComputedValue1(int pCursorY);

}//end of interface TraceValueCalculator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
