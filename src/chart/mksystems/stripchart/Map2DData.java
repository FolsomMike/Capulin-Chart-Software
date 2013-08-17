/******************************************************************************
* Title: Map2DData.java
* Author: Mike Schoonover
* Date: 8/15/13
*
* Purpose:
*
* This class handles data for a 2D map. It has synchronized functions (or other
* protection mechanisms) to allow data to be inserted by one thread and read
* for display by a different thread.
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

import chart.Viewer;
import chart.Xfer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

//-----------------------------------------------------------------------------
// class Map2DData
//
// This class creates and controls a data set for a 2D map.
//

public class Map2DData extends PlotterData{

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

    int insertionPoint;
    int extractionPoint, lookAhead, lookBehind;

    int repaintPoint;

    int segmentLength;
    int lastSegmentStartIndex;
    int lastSegmentEndIndex;

    public int sizeOfDataBuffer;
    int dataBuffer1[] = null;
    int dataBuffer2[] = null;

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

    // data return and direction

    static final int NO_NEW_DATA = 0;
    static final int FORWARD = 1;
    static final int REVERSE = 2;

//-----------------------------------------------------------------------------
// Map2DData::Map2DData (constructor)
//
//

public Map2DData(final int pBufferSize, final int pPlotStyle,
                                                        int pPeakDirection)
{

    sizeOfDataBuffer = pBufferSize; plotStyle = pPlotStyle;
    peakDirection = pPeakDirection;

}//end of Map2DData::Map2DData (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    dataBuffer1 = new int[sizeOfDataBuffer];
    flagBuffer = new int[sizeOfDataBuffer];

    //for span mode, a second array is necessary - min/max data is plotted
    if (plotStyle == SPAN) {
        dataBuffer2 = new int[sizeOfDataBuffer];
    }

}//end of Map2DData::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::totalReset
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

synchronized public void totalReset()
{

    insertionPoint = 0;
    extractionPoint = 0;
    lookAhead = 0;
    lookBehind = 0;
    repaintPoint = 0;

    //reset segment end pointers
    lastSegmentStartIndex = -1; lastSegmentEndIndex = -1;

    if (dataBuffer1 != null) {
        for (int i = 0; i < dataBuffer1.length; i++){
            dataBuffer1[i] = DEFAULT_DATA;
            flagBuffer[i] = CLEAR_ALL_FLAGS;
        }
    }

    //used in span mode
    if (dataBuffer2 != null) {
        for (int i = 0; i < dataBuffer1.length; i++) {
            dataBuffer2[i] = DEFAULT_DATA;
        }
    }

}//end of Map2DData::totalReset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::storeDataAtInsertionPoint
//
// Stores pData in dataBuffer1 at the current insertion point.
//
// If no data has already been stored at that point, pData is stored without
// comparison.
//
// If data has already been stored at that point, the old data is only
// overwritten if pData is greater than or less than the old data, depending
// on the value of peakDirection. Thus the dataset can collect the max or min
// peak of the incoming data depending on whether peakDirection is MAX or MIN.
//
// Returns true if the data point was stored, false if it was not stored due
// to not being greater or less than the existing data point.
//

public boolean storeDataAtInsertionPoint(int pData)
{

    boolean dataStored = false;

    //if no data has yet been stored, force store the first value; afterwards
    //value will be overwritten if new data is a peak

    if ((flagBuffer[insertionPoint] & IN_PROCESS) == 0){
        setFlags(insertionPoint, IN_PROCESS);
        dataBuffer1[insertionPoint] = pData;
        dataStored = true;
    }
    else{

        //only store if new data is greater than old data
        if (peakDirection == MAX){
            if (pData > dataBuffer1[insertionPoint]){
                dataBuffer1[insertionPoint] = pData;
                dataStored = true;
                }
        }
        else{
            //only store if new data is less than old data
            if (pData < dataBuffer1[insertionPoint]){
                dataBuffer1[insertionPoint] = pData;
                dataStored = true;
            }
        }
    }

    return(dataStored);

}//end of Map2DData::storeDataAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::setFlags
//
// The value in flagBuffer at pPosition is ORed with pMask to set one or more
// flags.
//

synchronized void setFlags(int pPosition, int pMask)
{

    flagBuffer[pPosition] |= pMask;

}//end of TraceData::setFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::clearFlags
//
// The value in flagBuffer at pPosition is ANDed with pMask to clear one or more
// flags.
//

synchronized void clearFlags(int pPosition, int pMask)
{

    flagBuffer[pPosition] &= pMask;

}//end of Map2DData::clearFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::newDataIsReady
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

}//end of Map2DData::newDataIsReady
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::getNewData
//
// If the DATA_ERASED flag for the current extractionPoint data position is
// set, the extractionPoint is decremented and the data at that position is
// returned as newData1/2 while the data at the previous position is
// returned as prevData1/2 in pDatum.
//
// If dataBuffer2 is not in use, newData2 and prevData2 will be returned as
// unknown values.
//
// NOTE: If the producer thread has added new data before the consumer thread
// can respond to any data erasures, the newData* and prevData* may
// not reflect the expected values.
//
// If the DATA_VALID flag is set on the next buffer position, the
// extractionPoint is incremented and the data at that position is returned
// as newData* while the data at the previous position is returned as prevData*
// in pDatum.
//
// The check for DATA_ERASED always overrides the check for DATA_VALID. If data
// is erased and then new data inserted before the consumer thread can react,
// the consumer thread will get all the erasure notices before forward reading
// of new data is resumed. This ensures that that traces can be reversed as
// far as the data was erased.
//
// Returns:
//
//  NO_NEW_DATA -- no data has been added or erased since the last call
//  FORWARD -- new data has been added
//  REVERSE -- data at current position has been erased
//
// The new data point, its clock position, and other information such as flags
// are returned in pDatum along with the data point for the previous position.
//

synchronized public int getNewData(TraceDatum pDatum)
{

    if ((flagBuffer[extractionPoint] & DATA_ERASED) != 0){

        clearFlags(extractionPoint, CLEAR_DATA_ERASED);

        pDatum.prevData1 = dataBuffer1[extractionPoint];

        if (dataBuffer2 != null){
            pDatum.prevData2 = dataBuffer2[extractionPoint];
        }

        retractExtractionPoint();

        segmentLength++;

        pDatum.newData1 = dataBuffer1[extractionPoint];

        if (dataBuffer2 != null){
            pDatum.newData2 = dataBuffer2[extractionPoint];
        }

        pDatum.flags = flagBuffer[extractionPoint];

        return(REVERSE);

    }

    if ((flagBuffer[lookAhead] & DATA_VALID) != 0){

        pDatum.prevData1 = dataBuffer1[extractionPoint];

        if (dataBuffer2 != null){
            pDatum.prevData2 = dataBuffer2[extractionPoint];
        }

        advanceExtractionPoint();

        segmentLength--;

        pDatum.newData1 = dataBuffer1[extractionPoint];

        if (dataBuffer2 != null){
            pDatum.newData2 = dataBuffer2[extractionPoint];
        }

        pDatum.flags = flagBuffer[extractionPoint];

        return(FORWARD);

    }

    return(NO_NEW_DATA);

}//end of Map2DData::Map2DData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::storeClockAtInsertionPoint
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

}//end of Map2DData::storeClockAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::storeThresholdAtInsertionPoint
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

}//end of Map2DData::storeThresholdAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::placeEndMaskMarker
//
// Causes a vertical bar to be drawn at the data location specified by
// beingFilledSlot.
//
// Places a vertical bar on the graph to show the start or end flag mask.  This
// indicates the areas in which no flags are registered even if the trace
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

}//end of Map2DData::placeEndMaskMarker
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::markSegmentStart
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

}//end of Map2DData::markSegmentStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::markSegmentEnd
//
// Records the current buffer position as the point where the current segment
// ends.  If the segment is to be saved, the save should occur after this
// function is called and before markSegmentStart is called for the next
// segment so the endpoints of the segment to be saved will still be valid.
//
// A separator bar is drawn for cases where the traces might be free running
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

}//end of Map2DData::markSegmentEnd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::segmentStarted
//
// Checks to see if a segment has been started.  If the trace has moved
// a predetermined amount after the current segment was initiated, it is assumed
// that a segment has been started.
//
// The trace must move more than a few counts to satisfy the start criteria.
// This is to ignore any small errors.
//

public boolean segmentStarted()
{

    if (segmentLength >= 10) {
        return true;
    } else {
        return false;
    }

}//end of Map2DData::segmentStarted
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::advanceInsertionPoint
//
// Moves the insertion point forward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// The data from the previous point is copied to the new location so that if
// the position is advanced multiple times before data is stored to the buffer,
// the skipped locations will have valid data. Since the IN_PROCESS flag is
// cleared, this copied value will be overwritten by the first new data added
// without checking to see if the new data is a peak
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

    int prevPoint = insertionPoint;

    //consumer thread can now use data at the current insertion point
    flagBuffer[prevPoint] |= DATA_VALID;

    //the buffer is circular - start over at beginning
    insertionPoint++;
    if (insertionPoint == sizeOfDataBuffer) {insertionPoint = 0;}

    //copy previous data to new buffer location -- see notes above
    dataBuffer1[insertionPoint] = dataBuffer1[prevPoint];

    //clear all flags except DATA_ERASED so that the consumer thread will
    //see that the data was once erased even if the producer thread adds new
    //data before the consumer thread can respond to the erasure

    flagBuffer[insertionPoint] &= DATA_ERASED;

}//end of Map2DData::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::advanceExtractionPoint
//
// Moves the extractionPoint, lookAhead, and lookBehind pointers forward one
// buffer position.
//
// The lookAhead and lookBehind pointers are always one ahead and one behind
// the position of the extraction point so that other methods can quickly
// check those positions without having to recalculate the pointers.
//

private void advanceExtractionPoint()
{

    lookBehind = extractionPoint;
    extractionPoint = lookAhead;

    //the buffer is circular - start over at beginning
    lookAhead++;
    if (lookAhead == sizeOfDataBuffer) {lookAhead = 0;}

}//end of Map2DData::advanceExtractionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::retractExtractionPoint
//
// Moves the extractionPoint, lookAhead, and lookBehind pointers back one
// buffer position.
//
// The lookAhead and lookBehind pointers are always one ahead and one behind
// the position of the extraction point so that other methods can quickly
// check those positions without having to recalculate the pointers.
//

private void retractExtractionPoint()
{

    lookAhead = extractionPoint;
    extractionPoint = lookBehind;

    //the buffer is circular - jump to end if past beginning
    lookBehind--;
    if (lookBehind < 0) {lookBehind = sizeOfDataBuffer - 1;}

}//end of Map2DData::retractExtractionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::eraseDataAtInsertionPoint
//
// This is the functional opposite of the advanceInsertionPoint method.
//
// Moves the insertion point backward one buffer position and makes the
// necessary preparations to the previous and new locations.
//
// The data for the erased position is set to 0.
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

}//end of Map2DData::eraseDataAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::getDataWidth
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

    for (int i = (dataBuffer1.length - 2); i > 0; i--){

        if ((flagBuffer[i] & DATA_VALID) != 0){
            endOfData = i;
            break;
        }
    }//for (int i = (pBuffer.length - 1); i <= 0; i--){

    return(endOfData);

}//end of Map2DData::getDataWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::getDataBuffer1
//
// Returns a reference to dataBuffer1.
//

public int[] getDataBuffer1()
{

    return(dataBuffer1);

}//end of Map2DData::getDataBuffer1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::getDataBuffer2
//
// Returns a reference to dataBuffer2.
//

public int[] getDataBuffer2()
{

    return(dataBuffer2);

}//end of Map2DData::getDataBuffer2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::findMinValue
//
// Finds the minimum value in dataBuffer1.
//
// Finds the minimum value in dataBuffer1.  Search begins at pStart position
// in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and dataBuffer1.length
// to avoid errors.
//

public int findMinValue(int pStart, int pEnd)
{

    if (pStart < 0) {pStart = 0;}
    if (pStart >= dataBuffer1.length) {pStart = dataBuffer1.length - 1;}

    if (pEnd < 0) {pEnd = 0;}
    if (pEnd >= dataBuffer1.length) {pEnd = dataBuffer1.length - 1;}

    int peak = Integer.MAX_VALUE;

    for (int i = pStart; i < pEnd; i++){

        if (dataBuffer1[i] < peak) {peak = dataBuffer1[i];}

    }

    return(peak);

}//end of Map2DData::findMinValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::findMaxValue
//
// Finds the maximum value in dataBuffer1.  Search begins at pStart position
// in the array and ends at pEnd.
//
// Values of pStart and pEnd will be forced between 0 and dataBuffer1.length
// to avoid errors.
//

public int findMaxValue(int pStart, int pEnd)
{

    if (pStart < 0) {pStart = 0;}
    if (pStart >= dataBuffer1.length) {pStart = dataBuffer1.length - 1;}

    if (pEnd < 0) {pEnd = 0;}
    if (pEnd >= dataBuffer1.length) {pEnd = dataBuffer1.length - 1;}

    int peak = Integer.MIN_VALUE;

    for (int i = pStart; i < pEnd; i++){
        if (dataBuffer1[i] > peak) {peak = dataBuffer1[i];}
    }

    return(peak);

}//end of Map2DData::findMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::saveSegment
//
// Saves the data for a segment to the open file pOut.
//
// This function should be called whenever a new segment is completed - each
// segment could represent a piece being monitored, a time period, etc.
//
// This function should be called after the segment end has been marked and
// before the next segment start has been marked so that the end points
// of the data to be saved are known.
//

public void saveSegment(BufferedWriter pOut) throws IOException
{

    //catch unexpected case where start/stop are invalid and bail
    if (lastSegmentStartIndex < 0 || lastSegmentEndIndex < 0){
        pOut.write("Segment start and/or start invalid - no data saved.");
        pOut.newLine(); pOut.newLine();
        return;
    }

    //save all the data and flags in the segment

    int i = lastSegmentStartIndex;

    pOut.write("[Data Set 1]"); pOut.newLine(); //save the first data set

    while (i != lastSegmentEndIndex){
        pOut.write(Integer.toString(dataBuffer1[i])); //save the data set 1
        pOut.newLine();
        //increment to next buffer slot, wrap around because buffer is circular
        if (++i == sizeOfDataBuffer) {i = 0;}
    }

    pOut.write("[End of Set]"); pOut.newLine();

    //save the second data set if it exists
    //the second data set is only used for certain styles of plotting
    if (dataBuffer2 != null){

        i = lastSegmentStartIndex;

        pOut.write("[Data Set 2]"); pOut.newLine();

        //save the second data set if it exists
        while (i != lastSegmentEndIndex){
            pOut.write(Integer.toString(dataBuffer2[i])); //save the data set 2
            pOut.newLine();
            //increment to next buffer slot, wrap around as buffer is circular
            if (++i == sizeOfDataBuffer) {i = 0;}
        }

        pOut.write("[End of Set]"); pOut.newLine();

    }//if (dataBuffer2 != null)

    i = lastSegmentStartIndex;

    pOut.write("[Flags]"); pOut.newLine(); //save the flags

    while (i != lastSegmentEndIndex){
        pOut.write(Integer.toString(flagBuffer[i])); //save the flags
        pOut.newLine();
        //increment to next buffer slot, wrap around because buffer is circular
        if (++i == sizeOfDataBuffer) {i = 0;}
    }

    pOut.write("[End of Set]"); pOut.newLine();

    pOut.newLine(); //blank line

}//end of Map2DData::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::loadSegment
//
// Loads the data points and flags for a segment from pIn.  It is expected that
// the Trace tag and meta data has already been read.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//

public String loadSegment(BufferedReader pIn, String pLastLine)
                                                            throws IOException
{

    //read in "Data Set 1"
    String line =
            processDataSeries(pIn, pLastLine, "[Data Set 1]", dataBuffer1);

    //if "Data Set 2" is in use, read it in
    if (dataBuffer2 != null) {
        line = processDataSeries(pIn, line, "[Data Set 2]", dataBuffer2);
    }

    //read in "Flags"
    line = processDataSeries(pIn, line, "[Flags]", flagBuffer);

    return(line);

}//end of Map2DData::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::processDataSeries
//
// Processes a data series from pIn.  The series could be "Data Set 1",
// "Data Set 2", or "Flags" depending on the parameters passed in.
//
// The pStartTag string specifies the section start tag for the type of data
// expected and could be: "[Data Set 1]", "[Data Set 2]", or "[Flags]".  The
// pBuffer pointer should be set to the buffer associated with the data type.
//
// Returns the last line read from the file so that it can be passed to the
// next process.
//
// For these sections, the [xxx] section start tag may or may not have already
// been read from the file by the code handling the previous section.  If it has
// been read, the line containing the tag should be passed in via pLastLine.
//

private String processDataSeries(BufferedReader pIn, String pLastLine,
                            String pStartTag, int[] pBuffer) throws IOException
{

    String line;
    boolean success = false;
    Xfer matchSet = new Xfer(); //for receiving data from function calls

    //if pLastLine contains the [xxx] tag, then skip ahead else read until
    // end of file reached or "[xxx]" section tag reached

    if (Viewer.matchAndParseString(pLastLine, pStartTag, "",  matchSet)) {
        success = true;  //tag already found
    }
    else {
        while ((line = pIn.readLine()) != null){  //search for tag
            if (Viewer.matchAndParseString(line, pStartTag, "",  matchSet)){
                success = true; break;
            }
        }//while
    }//else

    if (!success) {
        throw new IOException(
           "The file could not be read - section not found for " + pStartTag);
    }

    //scan the first part of the section and parse its entries
    //these entries apply to the chart group itself

    int i = 0;
    success = false;
    while ((line = pIn.readLine()) != null){

        //stop when next section end tag reached (will start with [)
        if (Viewer.matchAndParseString(line, "[", "",  matchSet)){
            success = true; break;
        }

        try{

            //convert the text to an integer and save in the buffer
            int data = Integer.parseInt(line);
            pBuffer[i++] = data;

            //catch buffer overflow
            if (i == pBuffer.length) {
                throw new IOException(
                 "The file could not be read - too much data for " + pStartTag
                                                       + " at data point " + i);
            }

        }
        catch(NumberFormatException e){
            //catch error translating the text to an integer
            throw new IOException(
             "The file could not be read - corrupt data for " + pStartTag
                                                       + " at data point " + i);
        }

    }//while ((line = pIn.readLine()) != null)

    if (!success) {
        throw new IOException(
         "The file could not be read - missing end of section for "
                                                                + pStartTag);
    }

    return(line); //should be "[xxxx]" tag on success, unknown value if not

}//end of Map2DData::processDataSeries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::prepareForRepaint
//
// Sets up variable(s) in preparation to extract data to repaint the trace.
//
// The repaintPoint is set to pStart.
//

public void prepareForRepaint(int pStart){

    repaintPoint = pStart;

}//end of Map2DData::prepareForRepaint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::getDataAtRepaintPoint
//
// Returns the data and flags at the buffer location of repaintPoint. Advances
// repaintPoint to the next position.
//

public int getDataAtRepaintPoint(TraceDatum pDatum){


    pDatum.prevData1 = dataBuffer1[repaintPoint];

    if (dataBuffer2 != null){
        pDatum.prevData2 = dataBuffer2[repaintPoint];
    }

    advanceRepaintPoint();

    pDatum.newData1 = dataBuffer1[repaintPoint];

    if (dataBuffer2 != null){
        pDatum.newData2 = dataBuffer2[repaintPoint];
    }

    pDatum.flags = flagBuffer[repaintPoint];

    return(FORWARD);

}//end of Map2DData::getDataAtRepaintPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Map2DData::advanceRepaintPoint
//
// Moves the repaintPoint pointer forward one buffer position.
//

private void advanceRepaintPoint()
{

    //the buffer is circular - start over at beginning
    repaintPoint++;
    if (repaintPoint == sizeOfDataBuffer) {repaintPoint = 0;}

}//end of Map2DData::advanceRepaintPoint
//-----------------------------------------------------------------------------


}//end of class Map2DData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------