/******************************************************************************
* Title: HardwareVars.java
* Author: Mike Schoonover
* Date: 4/26/08
*
* Purpose:
*
* This class encapsulates variables related to the Hardware class.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Plotter;
import chart.mksystems.stripchart.StripChart;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.PlotterHdwVars;

//-----------------------------------------------------------------------------
// class HardwareVars
//
//

public class HardwareVars extends Object{

    int nextIndex;
    PlotterHdwVars hdwVs;
    Threshold[] thresholds;

    UTGate gatePtr;

    double nSPerDataPoint, uSPerDataPoint;

    public double velocityUS, velocityNS, nominalWall, wallChartScale;
    public int nominalWallChartPosition, numberOfMultiples;

    public int repRate = 2000;

    public String measuredLengthText;
    public double measuredLength;

    public double pixelsPerInch;
    public double decimalFeetPerPixel;

    public double velocityShearUS, velocityShearNS;

    //the following are used to pass values between objects
    public double minWall;
    public double maxWall;
    public ChartGroup chartGroup;
    public StripChart chart;
    public Plotter plotter;

    boolean waitForOffPipe = false;
    boolean waitForOnPipe = false;
    boolean waitForInspectStart = false;
    boolean watchForOffPipe = false;
    boolean head1Down = false;
    boolean head2Down = false;
    boolean head3Down = false;
    
    //used to track count from photo eye clear to end of piece

    //Usually, the end of pipe signal comes from a photo eye which reaches the
    //end of the pipe before the inspection heads, so the system must compute
    //the length of the piece at that time and then continue tracking until all
    //sensors have also reached the end of the tube.  When the eye detects the
    //end of the piece, these variables are used to determine how much longer
    //each trace should run before signaling that the piece has been completed.

    //debug mks NOTE
    //The associated code for this needs some work -- each trace should have its
    //own set of tracking variables as they may reach the end at different
    //times. Move these to the Trace class.

    public int endOfPieceTracker;
    public boolean trackToEndOfPiece;
    public int endOfPiecePosition = 0; //NOTE: load this from config file

    //Sometimes, special processing is applied at the beginning of the
    //inspection piece.  The following variables are used to signal when the
    //inspection heads are within the specified distance from the start of the
    //piece, during which time the processing is applied.  At the start of the
    //inspection, these variables are setup and count down until the special
    //zone is passed.

    public boolean nearStartOfPiece;
    public int nearStartOfPieceTracker;
    //position is distance from start of piece for which modifier is to be
    //applied NOTE: load this from config file
    public int nearStartOfPiecePosition = 350;

    //Sometimes, special processing is applied at the end of the inspection
    //piece.  The following variables are used to signal when the inspection
    //heads are within the specified distance from the end of the piece, during
    //which time the processing is applied.  As the end of pipe signal usually
    //occurs before reaching the zone, these variables are setup at that time
    //and count down until the special ending zone is reached.  The zone is
    //then active from that time until the end of the piece is reached.

    public boolean nearEndOfPiece;
    public int nearEndOfPieceTracker;
    //position is distance from photo eye clear signal to location where
    //modifier is to be applied until the end of the pipe
    public int nearEndOfPiecePosition = 1; //NOTE: load this from config file
    public boolean trackToNearEndofPiece;

    //these are used to transfer values from gates specified in the
    //configuration file -- the values are used to modify the wall traces so
    //that a flaw gate can create a spike on the wall trace(s)
    public int wallMaxModifier;
    public int wallMinModifier =  Integer.MIN_VALUE;

    double encoder1InchesPerCount;
    double encoder2InchesPerCount;

    double photoEye1DistanceToEncoder1;
    double photoEye1DistanceToEncoder2;
    double photoEye1DistanceToMarker;
    double distanceAfterEncoder2ToSwitchEncoders;

    //distance between the laser spots of the two perpendicular eyes used to
    //trigger on-pipe and off-pipe signals
    public double photoEyeToPhotoEyeDistance;

    //length of the end stop at the "toward" end of the unit
    //if the away laser triggers on the start of this instead of the start of
    //the tube, specify the length in the config file so it can be accounted
    //for; if the away laser ignores the end stop and starts at the end of the
    //tube, set this value to 0.0 in the config file
    
    double endStopLength;
    
    double photoEye1DistanceFrontOfHead1;
    double photoEye1DistanceFrontOfHead2;
    double photoEye1DistanceFrontOfHead3;
    
    double photoEye2DistanceFrontOfHead1;
    double photoEye2DistanceFrontOfHead2;
    double photoEye2DistanceFrontOfHead3;

//-----------------------------------------------------------------------------
// HardwareVars::convertEncoder1CountsToFeet
//
// Converts pCounts number of encoder 1 counts to decimal feet and returns that
// value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder1CountsToFeet(int pCounts)
{
    
    return(pCounts * encoder1InchesPerCount / 12);
    
}//end of HardwareVars::convertEncoder1CountsToFeet
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// HardwareVars::convertEncoder1CountsToInches
//
// Converts pCounts number of encoder 1 counts to decimal inches and returns
// that value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder1CountsToInches(int pCounts)
{
    
    return(pCounts * encoder1InchesPerCount);
    
}//end of HardwareVars::convertEncoder1CountsToInches
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// HardwareVars::convertEncoder2CountsToFeet
//
// Converts pCounts number of encoder 2 counts to decimal feet and returns that
// value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder2CountsToFeet(int pCounts)
{
    
    return(pCounts * encoder2InchesPerCount / 12);
    
}//end of HardwareVars::convertEncoder2CountsToFeet
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// HardwareVars::convertEncoder2CountsToInches
//
// Converts pCounts number of encoder 2 counts to decimal inches and returns
// that value by scaling by the counts-per-inch value for that encoder.
//

public double convertEncoder2CountsToInches(int pCounts)
{
    
    return(pCounts * encoder2InchesPerCount);
    
}//end of HardwareVars::convertEncoder2CountsToInches
//-----------------------------------------------------------------------------
    
    
}//end of class HardwareVars
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
