/******************************************************************************
* Title: MFloatSpinner.java
* Author: Mike Schoonover
* Date: 12/05/03
*
* Purpose:
*
* This class creates a number editing spinner with a customizable display
* format most useful for floating point numbers.  The standard JSpinner
* created with the SpinnerNumberModel will create a JSpinner.NumberEditor with
* a default formatting pattern.  This subclass (MFloatSpinner) allows the
* pattern to be specified at instantiation.
*
* This spinner can also be used for integers for consistency.
*
* How to Use MFloatSpinner
*
*   Creating the Objects
* 
* For double values:
* 
*    //set this spinner up for use with doubles
*    //the format string "##0" has decimal places
*    //use intSpinner1.getIntValue() to retrieve the value as an integer
*    
*    MFloatSpinner doubleSpinner1 = 
*            new MFloatSpinner(5.5, 1.1, 9.9, 0.1, "##0.0", 60, 20);
*    doubleSpinner1.setName("Double Spinner 1 -- used for doubles");
*    doubleSpinner1.addChangeListener(this);
*    doubleSpinner1.setToolTipText("This is float spinner #1!");
*    mainPanel.add(doubleSpinner1);
*
* For integer values:
* 
*    MFloatSpinner doubleSpinner1 = 
*            new MFloatSpinner(5.5, 1.1, 9.9, 0.1, "##0.0", 60, 20);
*    doubleSpinner1.setName("Double Spinner 1 -- used for doubles");
*    doubleSpinner1.addChangeListener(this);
*    doubleSpinner1.setToolTipText("This is float spinner #1!");
*    mainPanel.add(doubleSpinner1);
*
* Responding to Changes
* 
* When the value is changed, MFloatSpinners call their ChangeListener's
* stateChanged method. So, in the above case, the class creating the objects
* should implement ChangeListener since it passes "this" to the spinners as
* their ChangeListener objects.
* 
* As such, the class should also contain the stateChanged method which can
* handle spinner value changes as shown below:
*
* @Override
* public void stateChanged(ChangeEvent ce)
*
*{
*    
*    //if for some reason the object which changed state is not a subclass of
*   //of Component, do nothing as this code only handles Components
*   
*   if (!(ce.getSource() instanceof Component)) {
*       return;
*   }    
*   
*   //cast the object to a Component so it's methods can be accessed
*   Component c = (Component)ce.getSource();
*   
*   String name = ((Component)ce.getSource()).getName();
*       
*   if (name.startsWith("Double Spinner 1")){
*   
*       //Since we know that the Component with the name starting with
*       //"Double Spinner 1" is an MFloatSpinner (because we created it and
*       // used that name for it), it can safely be cast to an MFloatSpinner.
*       //Since the values in that spinner are meant to be doubles, the
*       //getDoubleValue method is used to retrieve the value.
*       
*      double value = ((MFloatSpinner)c).getDoubleValue();
*   
*       view.setTextForDataTArea1("" + value);
*       
*   }
*   
*   if (name.startsWith("Integer Spinner 1")){
*   
*       //Since we know that the Component with the name starting with
*       //"Integer Spinner 1" is an MFloatSpinner (because we created it and
*       // used that name for it), it can safely be cast to an MFloatSpinner.
*       //Since the values in that spinner are meant to be integers, the
*       //getIntValue method is used to retrieve the value.
*        
*       int value = ((MFloatSpinner)c).getIntValue();
*    
*       view.setTextForDataTArea2("" + value);
*        
*    }
* 
* }//end of stateChanged
* 
* Details of Implementation
*  (only needed for deeper understanding)
* 
* While the pattern can be changed after creating a standard JSpinner, the
* size of the edit window also has to be adjusted because its size will already
* have been determined using the default pattern.
*
* Because the NumberEditor tends to oversize the edit box for formats with
* many digits after the decimal place (presumably because it expects there to
* be many digits before the decimal place, which is not always the case), the
* user can specify a custom width via pWidth.  If the default is preferred,
* pWidth should be set to -1.
*
* The height can be specified as well via pHeight. If the default is preferred,
* pHeight should be set to -1.
*
* To change the pattern of a standard JSpinner, use the following (substitute
*  the desired pattern in place of "0.0000"):
*
*  ((JSpinner.NumberEditor)mySpinner.getEditor()).
*                                           getFormat().applyPattern("0.0000");
*
*  This will require the size of the edit box to be adjusted similar to:
*
*   mySpinner.setMinimumSize(new Dimension(60,22));
*   mySpinner.setPreferredSize(new Dimension(60,22));
*
* To display the pattern being used by a JSpinner:
*
*  DecimalFormat decimalFormat =
*             ((JSpinner.NumberEditor)velocitySpinner.getEditor()).getFormat();
*
*  System.out.println("Pattern = " + decimalFormat.toPattern());
*
* By using MFloatSpinner with the SpinnerNumberModel, the pattern can be set on
* instantiation, negating the need to adjust the pattern and size afterwards.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.mswing;

import java.awt.Dimension;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class MFloatSpinner
//
// See header at top of the page for info.
//

public class MFloatSpinner extends JSpinner
{

    String formatPattern;
    public SpinnerNumberModel model;

//-----------------------------------------------------------------------------
// MFloatSpinner:MFloatSpinner (constructor)
//
// Constructs a JSpinner with custom formatting for the float value.
//
// pValue is the initial value to be displayed
// pMin is the minimum allowable value
// pMax is the maximum allowable value
// pIncrement is the amount the value is adjusted by use of the up/down arrows
// pFormatPattern is a format string specifiying how the float value is to be
//  displayed - number of digits before and after the decimal point, forced
//  forced zeroes, etc.  See help from Sun for class DecimalFormat.
//  Example formats: "#,##0.00" and "0.0000"
//
// pWidth is the user specified width for the edit box, set to -1 to use the
// default.
// pHeight is the user specified height for the edit box, set to -1 to use the
// default.
//
// See header notes at the top of the page for more info.
//

public MFloatSpinner(double pValue, double pMin, double pMax,
             double pIncrement, String pFormatPattern, int pWidth, int pHeight)
{

    super(new SpinnerNumberModel(pValue, pMin, pMax, pIncrement));

    model = (SpinnerNumberModel)getModel();

    //save the pattern for use by createEditor function
    formatPattern = pFormatPattern;

    setEditor(new JSpinner.NumberEditor(this, formatPattern));

    //adjust the width as specified if pWidth is not -1

    if(pWidth != -1){
        //force the editor window to the user specified dimensions
        setMinimumSize(new Dimension(pWidth, getMinimumSize().height));
        setMaximumSize(new Dimension(pWidth, getMaximumSize().height));
        setPreferredSize(new Dimension(pWidth, getPreferredSize().height));
    }

    if(pHeight != -1){
        //force the editor window to the user specified dimensions
        setMinimumSize(new Dimension(getMinimumSize().width, pHeight));
        setMaximumSize(new Dimension(getMaximumSize().width, pHeight));
        setPreferredSize(new Dimension(getPreferredSize().width, pHeight));
    }

}//end of MFloatSpinner::MFloatSpinner (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MFloatSpinner::getDoubleValue
//
// Returns the value in the spinner as a double.
//

public double getDoubleValue()

{

    Double dValue = (Double)getValue();

    return dValue.doubleValue();

}//end of MFloatSpinner::getDoubleValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MFloatSpinner::getInt
//
// Returns the value in the spinner as an integer.
//

public int getIntValue()

{

    Double dValue = (Double)getValue();

    return dValue.intValue();

}//end of MFloatSpinner::getInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// MFloatSpinner::createEditor
//
// Override parent class function JSpinner::createEditor
//
// -- NOTE --
// This override does nothing but call the overriding function in the parent
// class - this function is only here for reference purposes.
//
// The original intent was to create a JSpinner.NumberEditor object using the
// format string passed into the constructor of MFloatSpinner.  But the super
// class must be constructed before anything else is done, and the super class'
// constructor calls this function.  Therefore it is impossible to save the
// pFormatPattern parameter in the class variable formatPattern before this
// function is called - this function would need that variable.
//
// The solution is to allow createEditor to function as normal, creating the
// default JSpinner.NumberEditor.  The MFloatSpinner constructor then uses
// setEditor to create a new NumberEditor with a custom format string.
//

@Override
protected JComponent createEditor(SpinnerModel model)
{

    return super.createEditor(model);

}//end of MFloatSpinner::createEditor
//-----------------------------------------------------------------------------

}//end of class MFloatSpinner
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
