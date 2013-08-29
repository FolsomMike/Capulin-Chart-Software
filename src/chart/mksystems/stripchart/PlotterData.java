/******************************************************************************
* Title: PlotterData.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This class handles data for a Plotter object such as a Trace, Map2D, Map3D,
* etc. It has synchronized functions (or other protection mechanisms) to allow
* data to be inserted by one thread and read for display by a different thread.
*
* Only two threads should access this class -- a producer thread which adds
* or removes data and a consumer thread which reads data.
*
* All methods which alter the flagBuffer should be synchronized. Those that
* only read it do not have to be synchronized.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------


package chart.mksystems.stripchart;

//-----------------------------------------------------------------------------
// class PlotterData
//
// This is the parent class for classes which create and control a data set for
// a Plotter object.
//

public class PlotterData extends Object{

    //int bufferSize;
    int plotStyle;

    // insertionPoint is the buffer location where peak data is currently
    // being collected
    // extractionPoint is the buffer location where data should next be
    // retrieved
    // lookAhead is always positioned one location ahead of extractionPoint
    // while lookBehind is always one location behind -- this allow the next
    // and previous positions to be checked without having to constantly
    // recalculate the two pointers -- they only need to be repositioned when
    // extractionPoint is repositioned

    int insertionPoint, prevInsertionPoint;
    int extractionPoint, lookAhead, lookBehind;

    int repaintPoint;

    int segmentLength;
    int lastSegmentStartIndex;
    int lastSegmentEndIndex;

    public int sizeOfDataBuffer;
    public int widthOfDataBuffer;

    // flags and constants to store meta data for each datapoint

    public int flagBuffer[]; //stores various flags for plotting
                             //0000 0000 0000 0000 | 0000 000 | 0 0000 0000
                             //           ||| |||| | threshold| clock position
                             //           ||| |||> min or max was flagged
                             //           ||| ||> segment start separator
                             //           ||| |> segment end separator
                             //           ||| > end mask marks
                             //           ||> data is valid for use
                             //           |> data has been erased
                             //           > data in process

    static final int CLEAR_ALL_FLAGS = 0;
    static final int MIN_MAX_FLAGGED =         0x10000;
    static final int SEGMENT_START_SEPARATOR = 0x20000;
    static final int SEGMENT_END_SEPARATOR =   0x40000;
    static final int END_MASK_MARK =           0x80000;
    static final int DATA_VALID =             0x100000;
    static final int DATA_ERASED =            0x200000;
    static final int IN_PROCESS =             0x400000;

    static final int CLEAR_CLOCK_MASK = 0xfffffe00;
    static final int THRESHOLD_MASK = 0x0000fe00;
    static final int TRIM_CLOCK_MASK = 0x1ff;
    static final int CLEAR_THRESHOLD_MASK = 0xffff01ff;
    static final int TRIM_THRESHOLD_MASK = 0x7f;
    static final int CLEAR_DATA_ERASED = ~DATA_ERASED;

    // style of plot constants

    static final public int POINT_TO_POINT = 0;
    static final public int STICK = 1;
    static final public int SPAN = 2;


    // type of peak data stored -- minimums or maximums

    int peakDirection;

    static final public int MAX = 0;
    static final public int MIN = 1;

    // miscellaneous constants

    static final int DEFAULT_DATA = 0;
    static final int MAX_VALUE = Integer.MAX_VALUE;
    static final int MIN_VALUE = Integer.MIN_VALUE;

    // data return and direction

    public static final int NO_NEW_DATA = 0;
    public static final int FORWARD = 1;
    public static final int REVERSE = 2;

//-----------------------------------------------------------------------------
// PlotterData::PlotterData (constructor)
//
//

public PlotterData()
{


}//end of PlotterData::PlotterData (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{

    flagBuffer = new int[sizeOfDataBuffer];

}//end of PlotterData::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::resetAll
//
// Resets everything back to default values.
//
// Insertion and extraction related pointers all set to 0. Plotting will
// begin after insertionPoint moves twice, which will allow extractionPoint to
// move once -- it only moves and returns data if the next position shows
// DATA_VALID flag. This sequence allows good data to placed at index 0, so
// the first time data is extracted it will have valid data in index 0 and
// index 1.
//

synchronized public void resetAll()
{

    insertionPoint = 0;
    extractionPoint = 0;
    lookAhead = 0;
    lookBehind = 0;
    repaintPoint = 0;

    //reset segment end pointers
    lastSegmentStartIndex = -1; lastSegmentEndIndex = -1;

}//end of PlotterData::ResetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position.
//
// Should be overridden by subclasses which should first call this method
// and then prepare the data buffer at the new insertion point.
//
// The DATA_VALID flag is set for the previous data location to indicate that
// it may be used by the consumer thread -- even if it is nothing more than a
// copy of the previous data. Note that the IN_PROCESS flag is left set.
//
// This method should only be called by the producer thread.
//

synchronized public void advanceInsertionPoint()
{

    //lastValidPoint is always one spot behind the one being filled with data;
    //data up to and including that at lastValidPoint is ready for the
    //consumer thread

    prevInsertionPoint = insertionPoint;

    //consumer thread can now use data at the current insertion point
    flagBuffer[prevInsertionPoint] |= DATA_VALID;

    //the buffer is circular - start over at beginning
    insertionPoint++;
    if (insertionPoint == sizeOfDataBuffer) {insertionPoint = 0;}

    //clear all flags for the new position except DATA_ERASED so that the
    //consumer thread will see that the data was once erased even if the
    //producer thread adds new data before the consumer thread can respond to
    //the erasure

    flagBuffer[insertionPoint] &= DATA_ERASED;

}//end of PlotterData::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::advanceExtractionPoint
//
// Moves the extractionPoint, lookAhead, and lookBehind pointers forward one
// buffer position.
//
// The lookAhead and lookBehind pointers are always one ahead and one behind
// the position of the extraction point so that other methods can quickly
// check those positions without having to recalculate the pointers.
//

protected void advanceExtractionPoint()
{

    lookBehind = extractionPoint;
    extractionPoint = lookAhead;

    //the buffer is circular - start over at beginning
    lookAhead++;
    if (lookAhead == sizeOfDataBuffer) {lookAhead = 0;}

}//end of PlotterData::advanceExtractionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::retractExtractionPoint
//
// Moves the extractionPoint, lookAhead, and lookBehind pointers back one
// buffer position.
//
// The lookAhead and lookBehind pointers are always one ahead and one behind
// the position of the extraction point so that other methods can quickly
// check those positions without having to recalculate the pointers.
//

protected void retractExtractionPoint()
{

    lookAhead = extractionPoint;
    extractionPoint = lookBehind;

    //the buffer is circular - jump to end if past beginning
    lookBehind--;
    if (lookBehind < 0) {lookBehind = sizeOfDataBuffer - 1;}

}//end of PlotterData::retractExtractionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::eraseDataAtInsertionPoint
//
// This is the functional opposite of the advanceInsertionPoint method.
//
// Moves the insertion point backward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// Only the DATA_ERASED flag is set for the erased position.
//
// This method should only be called by the producer thread.
//

synchronized public void eraseDataAtInsertionPoint()
{

    //clear all flags for the current position which was in process and never
    //reached by the extractionPoint (and the consumer thread)
    flagBuffer[insertionPoint] = CLEAR_ALL_FLAGS;

    // back up the insertionPoint
    insertionPoint--;
    if (insertionPoint == -1) {insertionPoint = sizeOfDataBuffer - 1;}

    //set the DATA_ERASED flag while clearing all others
    //clearing the IN_PROCESS flag will force the old data to be overwritten
    flagBuffer[insertionPoint] = DATA_ERASED;

}//end of PlotterData::eraseDataAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::setFlags
//
// The value in flagBuffer at pPosition is ORed with pMask to set one or more
// flags.
//

synchronized void setFlags(int pPosition, int pMask)
{

    flagBuffer[pPosition] |= pMask;

}//end of PlotterData::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::clearFlags
//
// The value in flagBuffer at pPosition is ANDed with pMask to clear one or more
// flags.
//

synchronized void clearFlags(int pPosition, int pMask)
{

    flagBuffer[pPosition] &= pMask;

}//end of PlotterData::clearFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::newDataIsReady
//
// Checks to see if any new data is ready to be plotted or erased.
//
// Returns true if new data is ready, false if not.
//

public boolean newDataIsReady()
{

    if ( ((flagBuffer[extractionPoint] & DATA_ERASED) != 0)
            ||
         ((flagBuffer[lookAhead] & DATA_VALID) != 0)) {

        return (true);
    }
    else {
        return (false);
    }

}//end of PlotterData::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::storeClockAtInsertionPoint
//
// Stores the clock value in the flag buffer at the current insertion point.
// Only the lower 9 bits are stored.
//
// pClock usually refers to the rotary location to be associated with the
// data at the insertion point. It can be a reference to the O'clock position,
// degrees, or any other angular measurement.
//

synchronized public void storeClockAtInsertionPoint(int pClock)
{

    flagBuffer[insertionPoint] &= CLEAR_CLOCK_MASK; //erase old value
    //mask top bits to protect against invalid value
    flagBuffer[insertionPoint] += (pClock & TRIM_CLOCK_MASK);

}//end of PlotterData::storeClockAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::storeThresholdAtInsertionPoint
//
// Stores the number of the threshold violated by the datapoint at the current
// insertion point.
//
// The threshold number is increased by a value of 2 for storage -- a zero
// indicates no threshold was violated, a value of 1 indicates a manual user
// flag, any other value represents the threshold number plus 2.
//
// After adding 2, only the lower 7 bits are stored.
//

synchronized public void storeThresholdAtInsertionPoint(int pThreshold)
{

    flagBuffer[insertionPoint] &= CLEAR_THRESHOLD_MASK; //erase old value
    //shift up by value of 2 (see notes above)
    pThreshold += 2;
    //mask top bits to protect against invalid value
    pThreshold &= TRIM_THRESHOLD_MASK;
    flagBuffer[insertionPoint] += pThreshold << 9; //store new flag

}//end of PlotterData::storeThresholdAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::placeEndMaskMarker
//
// Causes a vertical bar to be drawn at the data location specified by
// beingFilledSlot.
//
// Places a vertical bar on the graph to show the start or end flag mask.  This
// indicates the areas in which no flags are registered even if the data
// breaks a threshold. This is used for the beginning and end of the piece to
// ignore false indications caused by piece entry or head settling.
//
// NOTE: End masks don't work well for UT units in which the head
//  moves across spinning pipe because when the head is dropped each transducer
//  is expected to start flagging at that time even though each is at a
//  different location.  Thus, each trace on a chart would have its own end
//  mask bar which would be confusing.  There is an option in the config file
//  to use trace suppression instead to mark the end mask area.
//
// NOTE: This function might be place the flag prematurely if the thread
//  drawing the trace gets behind data collection.  It is usually better for
//  the data collection / position tracking thread to set the flag bit.


synchronized public void placeEndMaskMarker()
{

    flagBuffer[insertionPoint] |= END_MASK_MARK;

}//end of PlotterData::placeEndMaskMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::markSegmentStart
//
// Resets the segmentLength variable and records the current buffer location.
//
// This function should be called whenever a new segment is to start - each
// segment could represent a piece being monitored, a time period, etc.
//

synchronized public void markSegmentStart()
{

    segmentLength = 0;

    //set flag to display a separator bar at the start of the segment
    flagBuffer[insertionPoint] |= SEGMENT_START_SEPARATOR;

    //record the buffer start position of the last segment
    lastSegmentStartIndex = insertionPoint;

}//end of PlotterData::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::markSegmentEnd
//
// Records the current buffer position as the point where the current segment
// ends.  If the segment is to be saved, the save should occur after this
// function is called and before markSegmentStart is called for the next
// segment so the endpoints of the segment to be saved will still be valid.
//
// A separator bar is drawn for cases where the data might be free running
// between segments, thus leaving a gap.  In that case, a bar at the start and
// end points is necessary to delineate between segment data and useless data
// in the gap.
//
// This function should be called whenever a new segment is to end - each
// segment could represent a piece being monitored, a time period, etc.
//

synchronized public void markSegmentEnd()
{

    //set flag to display a separator bar at the end of the segment
    flagBuffer[insertionPoint] |= SEGMENT_END_SEPARATOR;

    //record the buffer end position of the last segment
    lastSegmentEndIndex = insertionPoint;

}//end of PlotterData::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// PlotterData::segmentStarted
//
// Checks to see if a segment has been started.  If the insertion point has
// moved a predetermined amount after the current segment was initiated, it is
// assumed that a segment has been started.
//
// The insertion point must move more than a few counts to satisfy the start
// criteria. This is to ignore any small errors.
//

public boolean segmentStarted()
{

    if (segmentLength >= 10) {
        return true;
    } else {
        return false;
    }

}//end of PlotterData::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataWidth
//
// Returns the index of the last valid data point.
//
// Returns -1 if no data found.
//

public int getDataWidth()
{

    int endOfData = -1;

    //NOTE: Start at dataBuffer1.length - 2 as the last element seems to be
    // filled with zero -- why is this? -- fix?
    // Update -- now looks for the DATA_VALID flag instead of MAX_VALUE --
    // is the last element still a problem?

    for (int i = (sizeOfDataBuffer - 2); i > 0; i--){

        if ((flagBuffer[i] & DATA_VALID) != 0){
            endOfData = i;
            break;
        }
    }//for (int i = (pBuffer.length - 1); i <= 0; i--){

    return(endOfData);

}//end of Plotter::getDataWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Plotter::getDataBufferWidth
//
// Gets the width of the data buffer. This is the second size of a multi-
// dimensional array used for mapping.
//

public int getDataBufferWidth()
{

    return(widthOfDataBuffer);

}//end of Plotter::getDataBufferWidth
//-----------------------------------------------------------------------------

}//end of class PlotterData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
