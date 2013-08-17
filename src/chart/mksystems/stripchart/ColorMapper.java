/******************************************************************************
* Title: ColorMapper.java
* Author: Mike Schoonover
* Date: 8/16/13
*
* Purpose:
*
* This is the parent class for classes which translate data values to color
* values. Each subclass provides different rules and mappings for the
* translation.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.stripchart;

import java.awt.Color;

//-----------------------------------------------------------------------------
// class ColorMapper
//
// This is the parent class for classes which translate data values to color
// values.
//

public class ColorMapper extends Object{

    protected int valueBase, valueTop, valueRange;
    protected float hueBase, hueTop, hueRange;
    protected float hue, saturation, brightness;
    protected boolean invertHue;

//-----------------------------------------------------------------------------
// ColorMapper::ColorMapper (constructor)
//
// This constructor is not normally used. Each subclass should provide a
// custom constructor.
//
// The pValueBase is the lowest value expected to be mapped while pValueRange
// sets the range for the values.
//
// The pHueBase is the lowest hue value to be used -- this will be mapped to
// pValueBase. The range is 0.0 to 1.0. The application of this value is
// specific to each subclass.
//
// The pHueRange specifies the range of hue from pHueBase upward to which the
// values are to be mapped. The range is 0.0 to 1.0.  The application of this
// value is specific to each subclass.
//
// As an example, some subclasses might use the following mapping:
//
//  pValueBase values will be mapped to hue pHueBase
//  pValueBase + pValueRange will be mapped to (pHueBase + pHueRange)
//  all values between will be mapped linearly
//  values below the base or above base + range will be clipped
//
// The parameters pHue, pSaturation, and pBrightness set the default values
// for those elements and are used when one or more of the elements are to
// be varied while the rest are left at default.
//
// If pInvertHue is false, lower hues (red/yellow) will map to lower values
// while higher hues (blue/purple) will map to higher values. If true, the
// opposite mapping will be used.
//

public ColorMapper(int pValueBase, int pValueRange, float pHueBase,
        float pHueRange, float pHue, float pSaturation, float pBrightness,
        boolean pInvertHue)
{

    valueBase = pValueBase; valueRange = pValueRange;
    hueBase = pHueBase; hueRange = pHueRange;
    hue = pHue; saturation = pSaturation; brightness = pBrightness;
    invertHue = pInvertHue;

    //calculate the high end values to save processing time later

    valueTop = valueBase + valueRange;
    hueTop = hueBase + hueRange;

}//end of ColorMapper::ColorMapper (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ColorMapper::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

}//end of ColorMapper::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// ColorMapper::mapIntegerToColor
//
// Translates pValue to a Color and returns that Color.
//
// Should be overridden by subclasses.
//

public Color mapIntegerToColor(int pValue)
{

    return(Color.WHITE);

}//end of ColorMapper::mapIntegerToColor
//-----------------------------------------------------------------------------

}//end of class ColorMapper
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
