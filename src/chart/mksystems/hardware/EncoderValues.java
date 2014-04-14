/******************************************************************************
* Title: EncoderValues.java
* Author: Mike Schoonover
* Date: 4/13/14
*
* Purpose:
*
* This class handles variables related to a position encoder.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

package chart.mksystems.hardware;

//-----------------------------------------------------------------------------
//

public class EncoderValues extends Object{

    public int encoderPosAtOnPipeSignal = 0;
    public int encoderPosAtOffPipeSignal = 0;
    public int encoderPosAtHead1DownSignal = 0;
    public int encoderPosAtHead1UpSignal = 0;
    public int encoderPosAtHead2DownSignal = 0;
    public int encoderPosAtHead2UpSignal = 0;

//-----------------------------------------------------------------------------
// EncoderValues::EncoderValues (constructor)
//

public EncoderValues()
{


}//end of EncoderValues::EncoderValues (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of EncoderValues::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::setAllToMaxValue
//
// Sets all member variables to max value.
//

public void setAllToMaxValue()
{

    encoderPosAtOnPipeSignal = Integer.MAX_VALUE;
    encoderPosAtOffPipeSignal = Integer.MAX_VALUE;
    encoderPosAtHead1DownSignal = Integer.MAX_VALUE;
    encoderPosAtHead1UpSignal = Integer.MAX_VALUE;
    encoderPosAtHead2DownSignal = Integer.MAX_VALUE;
    encoderPosAtHead2UpSignal = Integer.MAX_VALUE;

}//end of EncoderValues::setAllToMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// EncoderValues::convertBytesToInt
//
// Converts four bytes to an int value and returns the value.
//

public int convertBytesToInt(byte byte3, byte byte2, byte byte1, byte byte0)
{
    
    int value;
    
    value = ((byte3 << 24));
    value |= (byte2 << 16)& 0x00ff0000;
    value |= (byte1 << 8) & 0x0000ff00;
    value |= (byte0)      & 0x000000ff;

    return(value);

}//end of EncoderValues::convertBytesToInt
//-----------------------------------------------------------------------------



}//end of class EncoderValues
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
