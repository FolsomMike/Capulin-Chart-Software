/******************************************************************************
* Title: TraceData.java
* Author: Mike Schoonover
* Date: 7/1/13
*
* Purpose:
*
* This class handles data for a trace. It has synchronized functions (or other
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
// class TraceData
//
// This class creates and controls a data set for a trace.
//

public class TraceData extends PlotterData{

    int dataBuffer1[] = null;
    int dataBuffer2[] = null;

//-----------------------------------------------------------------------------
// TraceData::DataTrace (constructor)
//
//

public TraceData(final int pBufferSize, final int pPlotStyle,
                                                        int pPeakDirection)
{

    sizeOfDataBuffer = pBufferSize; plotStyle = pPlotStyle;
    peakDirection = pPeakDirection;

}//end of TraceData::TraceData (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    super.init();

    dataBuffer1 = new int[sizeOfDataBuffer];

    //for span mode, a second array is necessary - min/max data is plotted
    if (plotStyle == SPAN) {
        dataBuffer2 = new int[sizeOfDataBuffer];
    }

}//end of TraceData::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::resetAll
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

@Override
synchronized public void resetAll()
{

    super.resetAll();

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

}//end of TraceData::resetAll
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::storeDataAtInsertionPoint
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

}//end of TraceData::storeDataAtInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::getNewData
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
// of new data is resumed. This ensures that plotting can be reversed as far
// as the data was erased.
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

        segmentLength--;

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

        segmentLength++;

        pDatum.newData1 = dataBuffer1[extractionPoint];

        if (dataBuffer2 != null){
            pDatum.newData2 = dataBuffer2[extractionPoint];
        }

        pDatum.flags = flagBuffer[extractionPoint];

        return(FORWARD);

    }

    return(NO_NEW_DATA);

}//end of TraceData::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::advanceInsertionPoint
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

    @Override
    synchronized public void advanceInsertionPoint()
{

    super.advanceInsertionPoint();

    //copy previous data to new buffer location -- see notes above
    dataBuffer1[insertionPoint] = dataBuffer1[prevInsertionPoint];

}//end of TraceData::advanceInsertionPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::getDataBuffer1
//
// Returns a reference to dataBuffer1.
//

public int[] getDataBuffer1()
{

    return(dataBuffer1);

}//end of TraceData::getDataBuffer1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::getDataBuffer2
//
// Returns a reference to dataBuffer2.
//

public int[] getDataBuffer2()
{

    return(dataBuffer2);

}//end of TraceData::getDataBuffer2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::findMinValue
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

}//end of TraceData::findMinValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::findMaxValue
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

}//end of TraceData::findMaxValue
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::saveSegment
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

}//end of TraceData::saveSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::loadSegment
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

}//end of TraceData::loadSegment
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::processDataSeries
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

}//end of TraceData::processDataSeries
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::prepareForRepaint
//
// Sets up variable(s) in preparation to extract data to repaint the trace.
//
// The repaintPoint is set to pStart.
//

public void prepareForRepaint(int pStart){

    repaintPoint = pStart;

}//end of TraceData::prepareForRepaint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::getDataAtRepaintPoint
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

}//end of TraceData::getDataAtRepaintPoint
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// TraceData::advanceRepaintPoint
//
// Moves the repaintPoint pointer forward one buffer position.
//

private void advanceRepaintPoint()
{

    //the buffer is circular - start over at beginning
    repaintPoint++;
    if (repaintPoint == sizeOfDataBuffer) {repaintPoint = 0;}

}//end of TraceData::advanceRepaintPoint
//-----------------------------------------------------------------------------


}//end of class TraceData
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------