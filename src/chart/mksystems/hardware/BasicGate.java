/******************************************************************************
* Title: BasicGate.java
* Author: Mike Schoonover
* Date: 11/19/09
*
* Purpose:
*
* This is the root class for gates.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.threadsafe.*;
import java.text.DecimalFormat;


//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class BasicGate
//
// This is the base class for gates.
//

public class BasicGate extends Object{

    IniFile configFile; //remove this after DACGate changed so that owner object calls config method and passes configFile in

    DecimalFormat[] decimalFormats;

    SyncedVariableSet syncedVarMgr;

    int channelIndex, gateIndex;

    public String title, shortTitle;

    boolean selectedFlag = false;

    boolean newDataReady = false;

    // NOTE regarding "Adjusted" values:
    //  These are actually to be used for drawing the gates.  An external object
    //  must call adjustPositionsNoTracking or adjustPositionsWithTracking
    //  before each gate draw to set these values.  See notes in those
    //  functions.

    final SyncedDouble gateStart;
    double gateStartTrackingOn = 0;
    double gateStartTrackingOff = 0;
    public int gatePixStart = 0;
    public int gatePixStartAdjusted = 0;
    public int gatePixEnd = 0;
    public int gatePixEndAdjusted = 0;
    public int gatePixMidPoint = 0;
    public int gatePixMidPointAdjusted = 0;

    public boolean gateActive;
    final SyncedDouble gateWidth;
    final SyncedInteger gateLevel;
    public int gatePixLevel = 0;
    final SyncedInteger gateFlags;

    double previousGateStart;
    double previousGateWidth;

    public int interfaceCrossingPixAdjusted = 0;

    static int GATE_ACTIVE = 0x0001;
    static int GATE_REPORT_NOT_EXCEED = 0x0002;
    static int GATE_MAX_MIN = 0x0004;
    static int GATE_WALL_START = 0x0008;
    static int GATE_WALL_END = 0x0010;
    static int GATE_FIND_CROSSING = 0x0020;
    static int GATE_USES_TRACKING = 0x0040;
    static int GATE_FIND_PEAK = 0x0080;
    static int GATE_FOR_INTERFACE = 0x0100;
    static int GATE_INTEGRATE_ABOVE_PEAK = 0x0200;
    static int GATE_QUENCH_IF_OVERLIMIT = 0x0400;
    static int GATE_TRIGGER_ASCAN_SAVE = 0x0800;
    static int GATE_FIND_DUAL_PEAK_CENTER = 0x1000;
    static int GATE_APPLY_SIGNAL_AVERAGING = 0x2000;
    
    // references to point at the controls used to adjust the values - these
    // references are set up by the object which handles the adjusters and used
    // to access those controls

    public Object gateStartAdjuster;
    public Object gateWidthAdjuster;
    public Object gateLevelAdjuster;
    public Object aScanTriggerCheckBox;

//-----------------------------------------------------------------------------
// BasicGate::BasicGate (constructor)
//
// The constructing class should pass a pointer to a SyncedVariableSet for the
// values in this class which can be changed by the user and are sent to the
// remotes so that they will be managed in a threadsafe manner.
//

public BasicGate(SyncedVariableSet pSyncedVarMgr)
{

    syncedVarMgr = pSyncedVarMgr;

    gateStart = new SyncedDouble(syncedVarMgr); gateStart.init();
    gateWidth = new SyncedDouble(syncedVarMgr); gateWidth.init();
    gateLevel = new SyncedInteger(syncedVarMgr); gateLevel.init();

    gateFlags = new SyncedInteger(syncedVarMgr); gateFlags.init();

    //create various decimal formats
    decimalFormats = new DecimalFormat[4];
    decimalFormats[0] = new  DecimalFormat("0000000");
    decimalFormats[1] = new  DecimalFormat("0.0");
    decimalFormats[2] = new  DecimalFormat("0.00");
    decimalFormats[3] = new  DecimalFormat("0.000");

}//end of BasicGate::BasicGate (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{


}//end of BasicGate::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::calculateGatePixelLocation
//
// Calculates the pixel locations for a gate from its time locations and
// various related offsets.
//
// Returns true if any value was changed.  This return value can then be used
// to decide if the gate data needs to be sent to a remote.
//
// This function performs the opposite of calculateGateTimeLocation.
//

public boolean calculateGatePixelLocation(double pUSPerPixel, int pDelayPix,
                                           int pCanvasHeight, int pVertOffset)
{

    boolean valueChanged = false;

    int nGatePixStart =
                (int)Math.round(gateStart.getValue() / pUSPerPixel - pDelayPix);
    if (gatePixStart != nGatePixStart){
        gatePixStart = nGatePixStart; valueChanged = true;
    }

    int nGatePixEnd =
         gatePixStart + (int)Math.round(gateWidth.getValue() / pUSPerPixel) - 1;
    if (gatePixEnd != nGatePixEnd){
        gatePixEnd = nGatePixEnd; valueChanged = true;
    }

    //used for things which need to know the X midpoint of the gate
    gatePixMidPoint = (gatePixStart + gatePixEnd) / 2;

    //make first calculation from the percentage value of gateLevel
    int nGatePixLevel =
                (int)Math.round(gateLevel.getValue() * pCanvasHeight / 100);

    //add in the current vertical offset
    nGatePixLevel += pVertOffset;

    //invert level so 0,0 is at bottom left
    if (nGatePixLevel < 0) {nGatePixLevel = 0;}
    if (nGatePixLevel > pCanvasHeight) {nGatePixLevel = pCanvasHeight;}
    nGatePixLevel = pCanvasHeight - nGatePixLevel;

    if (gatePixLevel != nGatePixLevel){
        gatePixLevel = nGatePixLevel; valueChanged = true;
    }

    return(valueChanged);

}//end of BasicGate::calculateGatePixelLocations
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::calculateGateTimeLocation
//
// Calculates the time locations for a gate from its pixel location and
// various related offsets.
//
// This function performs the opposite of calculateGatePixelLocation.
//

public void calculateGateTimeLocation(double pUSPerPixel, int pDelayPix,
                                             int pCanvasHeight, int pVertOffset)
{

    double nGateStart =  (gatePixStart + pDelayPix) * pUSPerPixel;
    gateStart.setValue(nGateStart, false);

    double nGateWidth = (gatePixEnd - gatePixStart + 1) * pUSPerPixel;
    gateWidth.setValue(nGateWidth, false);

    int nPixLevel = gatePixLevel; //use a temp variable - don't modify original

    //invert level so 0,0 is back at top left
    if (nPixLevel < 0) {nPixLevel = 0;}
    if (nPixLevel > pCanvasHeight) {nPixLevel = pCanvasHeight;}
    nPixLevel = pCanvasHeight - nPixLevel;

    //remove the current vertical offset
    nPixLevel = nPixLevel - pVertOffset;

    int nGateLevel =
            (int)Math.round(((double)nPixLevel / (double)pCanvasHeight * 100));

    gateLevel.setValue(nGateLevel, false);

}//end of BasicGate::calculateGateTimeLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::adjustPositionsNoTracking
//
// Readies the gate's "adjusted" pixel locations for drawing the gate by
// copying their values from the "non-adjusted" versions.  For use when the
// gate's position is static and not adjusted for each draw.
//
// The "adjusted" positions are the values to be used for drawing the gates.
// If the gate positions are static and not tracking another event such as
// an interface crossing in a different gate, then the non-adjusted values are
// simply copied to the adjusted values -- call this method each time the
// gate is drawn for that case.
//
// If the gate position is constantly changing to track another event such
// as an interface crossing in another gate, then the adjusted values are
// tweaked to move the gates as the event moves -- call
// adjustPositionWithTracking each time the gate is drawn for that case.
//

public void adjustPositionsNoTracking()
{

    gatePixStartAdjusted = gatePixStart;
    gatePixEndAdjusted = gatePixEnd;
    gatePixMidPointAdjusted = gatePixMidPoint;

}//end of BasicGate::adjustPositionsNoTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::adjustPositionsWithTracking
//
// Readies the gate's "adjusted" pixel locations for drawing the gate by
// re-calculating their values based on the "non-adjusted" versions and timing
// for another event.  For use when the gate's position is moving to track
// another event.
//
// The adjusted values are calculated by adding pOffset to their non-adjusted
// values.
//
// See notes for adjustPositionsNoTracking for more details.
//

public void adjustPositionsWithTracking(int pOffset)
{

    gatePixStartAdjusted = gatePixStart + pOffset;
    gatePixEndAdjusted = gatePixEnd + pOffset;
    gatePixMidPointAdjusted = gatePixMidPoint + pOffset;

}//end of BasicGate::adjustPositionsWithTracking
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::isPositionChanged
//
// Returns true if the start, width, or level of the gate have been changed.
//

public boolean isPositionChanged()
{

    if (     gateStart.getDataChangedFlag()
          || gateWidth.getDataChangedFlag()
          || gateLevel.getDataChangedFlag()) {

        return(true);
    }
    else {
        return(false);
    }

}//end of BasicGate::isPositionChanged
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BasicGate::isFlagsChanged
//
// Returns true if the gateFlags variable has been changed.
//

public boolean isFlagsChanged()
{

    if ( gateFlags.getDataChangedFlag()) {
        return(true);
    }
    else {
        return(false);
    }

}//end of BasicGate::isFlagsChanged
//-----------------------------------------------------------------------------


}//end of class BasicGate
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
