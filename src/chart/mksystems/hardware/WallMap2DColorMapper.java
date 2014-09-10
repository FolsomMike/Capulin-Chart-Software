/******************************************************************************
* Title: WallMap2DColorMapper.java
* Author: Mike Schoonover
* Date: 8/16/13
*
* Purpose:
*
* This class translates data values to color values for display on a 2D map.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.stripchart.ColorMapper;
import java.awt.Color;

//-----------------------------------------------------------------------------
// class WallMap2DColorMapper
//

public class WallMap2DColorMapper extends ColorMapper{

//-----------------------------------------------------------------------------
// WallMap2DColorMapper::WallMap2DColorMapper (constructor)
//

public WallMap2DColorMapper(int pValueBase, int pValueRange, float pHueBase,
        float pHueRange, float pHue, float pSaturation, float pBrightness,
        boolean pInvertHue)
{

    super(pValueBase, pValueRange, pHueBase, pHueRange,
                                   pHue, pSaturation, pBrightness, pInvertHue);

}//end of WallMap2DColorMapper::WallMap2DColorMapper (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMap2DColorMapper::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

}//end of WallMap2DColorMapper::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// WallMap2DColorMapper::mapIntegerToColor
//
// Translates pValue to a Color and returns that Color.
//
// For this class the hue is varied depending on the value while the
// saturation and brightness are held constant.
//
// Should be overridden by subclasses.
//

@Override
public Color mapIntegerToColor(int pValue)
{

    if (pValue < valueBase) {pValue = valueBase;}
    if (pValue > valueTop) {pValue = valueTop;}

    //calculate where the value falls on the scale based on the base
    //value and the value range as a percentage of full scale

    float ratio = (((float)pValue - (float)valueBase) / (float)valueRange);
    
    ratio = ratio / 2; //increase the span a bit to improve color separation

    //calculate a hue percentage of full scale based on the value's percentage

    float newHue;

    // when not inverting, lower values are reddish while higher values are
    // bluish/purplish; if inverting the opposite is true

    if (!invertHue){
        newHue= hueBase + (hueRange * ratio);
    }
    else {
        newHue= (hueTop) - (hueRange * ratio);
    }

    return(Color.getHSBColor(newHue, saturation, brightness));

}//end of WallMap2DColorMapper::mapIntegerToColor
//-----------------------------------------------------------------------------

}//end of class WallMap2DColorMapper
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
