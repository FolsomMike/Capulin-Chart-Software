/******************************************************************************
* Title: UTSimulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class simulates a TCP/IP connection between the host and UT boards.
*
* This is a subclass of Simulator which subclasses Socket and can be substituted
* for an instance of the Socket class when simulated data is needed.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

    // use this to make sure each method is cleaning up all bytes in the
    // received packet -- copy to method to be checked and use breakpoint
    //testing mks
    //int x = 0;
    //try {x = byteIn.available();} catch(IOException e){}
    //readBytes(x);
    //testing mks


//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.mksystems.inifile.IniFile;
import java.io.*;
import java.net.*;

//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class RamMemoryBlockChecksum
//
// This class holds a starting memory address, the number of bytes associated
// with the memory block which starts at that address, and the checksum for
// that block. Each block is also distinguished by its chip, core, and page.
//
// The host computer sends the DSP code to the remotes to be stored in memory.
// Afterwards, it requests the checksum for each block of code which was
// stored at a different memory location. For the simulation to return the
// correct checksum, those checksums are computed and stored when the code
// is received. There is no need to store the actual DSP code as the simulator
// cannot run it -- only the checksums for each addressed block are needed so
// the can be returned for the verification.
//

class RamMemoryBlockChecksum extends Object{

    public boolean empty = true;
    public byte chip;
    public byte core;
    public byte page;
    public int startAddress;
    public int numberBytes;
    public int checksum;

}//end of class RamMemoryBlockChecksum
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTSimulator
//
// This class simulates data from a TCP/IP connection between the host computer
// and UT boards.
//

public class UTSimulator extends Simulator{

public UTSimulator() throws SocketException{}; //default constructor - not used

    short rabbitControlFlags = 0;

    static int MAX_BOARD_CHANNELS = 4;
    BoardChannelSimulator[] boardChannels;
    int peakDataPktCounter = 0;

    double nominalWall, nSPerDataPoint, uSPerDataPoint, compressionVelocityNS;
    int numWallMultiples;
    double inchesPerChartPercentagePoint;
    
    int mapChannel, boardChannelForMapDataSource, headForMapDataSensor;
    int traceBufferSize, mapBufferSize;
    
    static int ASCAN_BUFFER_SIZE = 8 * 1024; //matches RAM sample buffer in FPGA
    int[] aScanBuffer;

    public static int utBoardCounter = 0;
    int utBoardNumber;

    int mainBangSineAngle;
    int reflectionSineAngle;

    int interfaceStartSim =1300;
    int farReflectionStartSim = 0;
    int nearReflectionStartSim = 0;

    int peakSpacing;

    int ifaceProfileSize;
    int ifaceProfileCounter;
    int[] ifaceProfile = {10,  3,  7, 15, 17, 14, 20, 21, 18, 25,
                          27, 30, 25, 30, 35, 40, 37, 43, 45, 50,
                          10,  3,  7, 15, 17, 14, 20, 21, 18, 25,
                          30, 33, 27, 33, 38, 40, 34, 48, 55, 60,
                          27, 30, 25, 30, 35, 40, 37, 43, 45, 50,
                          10,  3,  7, 15, 17, 14, 20, 21, 18, 25,
                          30, 33, 27, 33, 38, 40, 34, 48, 55, 80,
                          5,  16, 20, 25, 27, 34, 42, 38, 39, 20
                          };

    class ChannelPeakSet{
        int channel;
        int numberOfGates;
        int peak;
        short peakFlags;
        //test value - change this to zero after sim added
        short flightTime = 1234;
        short track = 5678; //test value - change this to zero after sim added
        boolean isWallChannel;
        }//end channelPeakSet


    static int NUMBER_OF_BOARD_CHANNELS = 4;
    ChannelPeakSet[] channelPeakSets;

    int peakPacketCount;

    int currentBlock;

    //this value is the number of DSP code blocks which can be handled by the
    //simulator -- each block is a contiguous block of code; a new block is
    //sent each time the starting address of a block is specified, resulting in
    //a new block in a non-contiguous location; usually, the entire DSP code is
    //sent as one block with the Interrupt Vectors sent as a second block, so
    //not many blocks are required to cover all data which is expected to be
    //uploaded to the 8 DSP cores and their memory pages
    static int NUMBER_OF_RAM_MEMORY_BLOCKS = 50;
    RamMemoryBlockChecksum[] ramMemoryBlockChecksums;

    public byte prevChip = -1;
    public byte prevCore = -1;
    public byte prevPage = -1;
    public int prevAddress = -2;

    int helixAdvanceTracker = 0;
    static final int SAMPLES_PER_REV = 500;
    static final int SAMPLES_PER_ADVANCE = 500;
    static final int SAMPLE_COUNT_VARIATION = 30;

    static final int MAP_BUFFER_SIZE = 2000;
    int mapDataBuffer[];
    int wallMapPacketSendTimer = 0;
    static final int WALL_MAP_PACKET_SEND_RELOAD = 20;

//-----------------------------------------------------------------------------
// UTSimulator::UTSimulator (constructor)
//

public UTSimulator(InetAddress pIPAddress, int pPort, String pMainFileFormat,
        String pSimulationDataSourceFilePath) throws SocketException
{

    //call the parent class constructor
    super(pIPAddress, pPort, pSimulationDataSourceFilePath);

    mainFileFormat = pMainFileFormat;

}//end of UTSimulator::UTSimulator (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

public void init()
{
    
    //give each board a unique number so it can load data from the
    //simulation files and such

    utBoardNumber = utBoardCounter++;

    super.init(utBoardNumber);
        
    status = UTBoard.FPGA_LOADED_FLAG;

    mapDataBuffer = new int[MAP_BUFFER_SIZE];

    wallMapPacketSendTimer = WALL_MAP_PACKET_SEND_RELOAD;

    channelPeakSets = new ChannelPeakSet[NUMBER_OF_BOARD_CHANNELS];
    for (int i=0; i < NUMBER_OF_BOARD_CHANNELS; i++) {
        channelPeakSets[i] = new ChannelPeakSet();
    }

    ramMemoryBlockChecksums =
                        new RamMemoryBlockChecksum[NUMBER_OF_RAM_MEMORY_BLOCKS];
    for (int i=0; i < NUMBER_OF_RAM_MEMORY_BLOCKS; i++) {
        ramMemoryBlockChecksums[i] = new RamMemoryBlockChecksum();
    }

    aScanBuffer = new int[ASCAN_BUFFER_SIZE]; //used to store simulated A/D data

    //create an out writer from this class - will be input for some other class
    //this writer is only used to send the greeting back to the host

    PrintWriter out = new PrintWriter(localOutStream, true);
    out.println("Hello from UT Board!");

}//end of UTSimulator::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::processDataPackets
//
// See processDataPacketsHelper notes for more info.
//

public int processDataPackets(boolean pWaitForPkt)
{

    int x = 0;

    //process packets until there is no more data available

    // if pWaitForPkt is true, only call once or an infinite loop will occur
    // because the subsequent call will still have the flag set but no data
    // will ever be coming because this same thread which is now blocked is
    // sometimes the one requesting data

    if (pWaitForPkt) {
        return processDataPacketsHelper(pWaitForPkt);
    }
    else {
        while ((x = processDataPacketsHelper(pWaitForPkt)) != -1){}
    }

    //the wall map data packet is not requested by the host, it is sent
    //asynchronously and continuously

    if (wallMapPacketSendTimer-- == 0){
        wallMapPacketSendTimer = WALL_MAP_PACKET_SEND_RELOAD;
        if (isWallMapPacketSendEnabled()){
            //System.out.println("Map Packet Sent - " + index);
            sendWallMapPacket();
        }
    }

    return x;

}//end of UTSimulator::processDataPackets
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::isWallMapPacketSendEnabled
//
// Returns true if the Rabbit is in wall mapping mode and async data sending
// is enabled, false otherwise.
//

private boolean isWallMapPacketSendEnabled()
{

    if(((rabbitControlFlags & UTBoard.RABBIT_WALL_MAP_MODE) != 0)
         && ((rabbitControlFlags & UTBoard.RABBIT_SEND_DATA_ASYNC) != 0)){

        return(true);
    }
    else{
        return(false);
    }


}//end of UTSimulator::isWallMapPacketSendEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::processDataPacketsHelper
//
// Drive the simulation functions.  This function is usually called from a
// thread.
//

public int processDataPacketsHelper(boolean pWaitForPkt)
{

    if (byteIn == null) {return(0);}  //do nothing if the port is closed

    try{

        int x;

        //wait until 5 bytes are available - this should be the 4 header bytes,
        //and the packet identifier/command
        if ((x = byteIn.available()) < 5) {return(-1);}

        //read the bytes in one at a time so that if an invalid byte is
        //encountered it won't corrupt the next valid sequence in the case
        //where it occurs within 3 bytes of the invalid byte

        //check each byte to see if the first four create a valid header
        //if not, jump to resync which deletes bytes until a valid first header
        //byte is reached

        //if the reSynced flag is true, the buffer has been resynced and an 0xaa
        //byte has already been read from buffer so it shouldn't be read again

        //after a resync, the function exits without processing any packets

        if (!reSynced){
            //look for the 0xaa byte unless buffer just resynced
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] != (byte)0xaa) {reSync(); return 0;}
        }
        else {
            reSynced = false;
        }

        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0x55) {reSync(); return 0;}
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0xbb) {reSync(); return 0;}
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0x66) {reSync(); return 0;}

        //read the packet ID
        byteIn.read(inBuffer, 0, 1);

        if (inBuffer[0] == UTBoard.GET_STATUS_CMD) {getStatus();}
        else
        if (inBuffer[0] == UTBoard.LOAD_FPGA_CMD) {loadFPGA();}
        else
        if (inBuffer[0] == UTBoard.WRITE_FPGA_CMD) {writeFPGA();}
        else
        if (inBuffer[0] == UTBoard.READ_FPGA_CMD) {readFPGA();}
        else
        if (inBuffer[0] == UTBoard.WRITE_DSP_CMD) {writeDSP();}
        else
        if (inBuffer[0] == UTBoard.WRITE_NEXT_DSP_CMD) {writeNextDSP();}
        else
        if (inBuffer[0] == UTBoard.READ_DSP_CMD) {readDSP();}
        else
        if (inBuffer[0] == UTBoard.GET_DSP_RAM_BLOCK_CHECKSUM)
        {   getDSPRamBlockChecksum();}
        else
        if (inBuffer[0] == UTBoard.GET_PEAK_DATA4_CMD) {getPeakData4();}
        else
        if (inBuffer[0] == UTBoard.GET_ASCAN_CMD) {getAScan();}
        else
        if (inBuffer[0] == UTBoard.MESSAGE_DSP_CMD) {processDSPMessage();}
        else
        if (inBuffer[0] == UTBoard.SET_CONTROL_FLAGS_CMD)
        {   setRabbitControlFlags(); }
        else
        if (inBuffer[0] == UTBoard.RESET_FOR_NEXT_RUN_CMD)
        {   resetForNextRun(); }
 
        return 0;

    }//try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 519");
    }

    return 0;

}//end of UTSimulator::processDataPacketsHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readBytes
//
// Reads pNumBytes from byteIn into inBuffer. Returns the number of bytes
// read or -1 on error.
//
// Waits until the specified number of bytes are available.
//

private int readBytes(int pNumBytes)
{

    try{
        while(byteIn.available() < pNumBytes){}
        byteIn.read(inBuffer, 0, pNumBytes);
        return(pNumBytes);
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 544");
        return(-1);
    }

}//end of UTBoard::readBytes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::processDSPMessage
//
// Processes a message to a DSP.
//

private int processDSPMessage()
{

    readBytes(3); //read in the rest of the packet

    int dspChip = inBuffer[0];
    int dspCore = inBuffer[1];
    int dspMsgID = inBuffer[2];

    if ( dspMsgID == UTBoard.DSP_GET_STATUS_CMD) {
        return (readDSPStatus(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_FLAGS1) {
        return (setDSPFlags1(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_GATE_SIG_PROC_TUNING) {
        return (setDSPGateSigProcTuningValues(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_GAIN_CMD) {
        return (setDSPSoftwareGain(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_HIT_MISS_COUNTS) {
        return (setDSPHitMissCounts(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_RECTIFICATION) {
        return (setDSPRectification(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_AD_SAMPLE_SIZE_CMD) {
        return (setDSPADSampleSize(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_DELAYS) {
        return (setDSPDelays(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_ASCAN_SCALE) {
        return (setDSPAScanScale(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_GATE) {
        return (setDSPGate(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_DAC) {
        return (setDSPDAC(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_GATE_FLAGS) {
        return (setDSPGateFlags(dspChip, dspCore));
    }

    if ( dspMsgID == UTBoard.DSP_SET_DAC_FLAGS) {
        return (setDSPDACFlags(dspChip, dspCore));
    }

    //clear out the remaining bytes of any unhandled DSP message
    tossDSPMessageRemainder();

    return(0);

}//end of UTBoard::processDSPMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setRabbitControlFlags
//
// Sets the rabbitControlFlags to the short integer in the packet.
//

private int setRabbitControlFlags()
{
    
    readBytes(3); //read in the rest of the packet including checksum

    rabbitControlFlags = (short) (((((short)inBuffer[0])<<8) & 0xff00)
                                                + ((short)inBuffer[1]) & 0xff);

    return(3);

}//end of UTBoard::setRabbitControlFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::resetForNextRun
//
// Prepares for the next run.
//

private int resetForNextRun()
{
    
    readBytes(3); //read in the rest of the packet including checksum
    
    if (simulationType == RANDOM){
        prepareNextSimulationDataSetFromRandom();
    }
    else
    if (simulationType == FROM_FILE){
        prepareNextSimulationDataSetFromFiles();
    }
    
    //enable map data collection and transmission to the host
    rabbitControlFlags |= UTBoard.RABBIT_SEND_DATA_ASYNC;
    
    return(3);

}//end of UTBoard::resetForNextRun
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::prepareNextSimulationDataSetFromRandom
//
// Set up values for random data generation for the next run.
//

public void prepareNextSimulationDataSetFromRandom()
{

    //no setup currently required
    
}//end of UTBoard::prepareNextSimulationDataSetFromRandom
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::prepareNextSimulationDataSetFromFiles
//
// Loads the next set of simulation data and opens files for reading any data
// which is not pre-loaded but done on the fly.
//
// Each time this method is called, currentDataSetIndex is incremented so that
// a new data set will be loaded to provide different data. When a file set
// is not found for the index, it starts over at 1.
//
// If a file set is not found for index 1, the simulation mode is changed
// to RANDOM and no files are opened or data loaded.
//
// The data for all Traces is stored in a single file such as 
//  "20 - 0000001 map.dat". Thus this one file is opened and passed to each
// BoardChannelSimulator for use.
//
// The data for Maps is stored individually in separate files, so the info to
// create the filename (such as the currentDataSetIndex) is passed to each
// BoardChannelSimulator so each can create the appropriate filename and open
// that specific file.
//

public void prepareNextSimulationDataSetFromFiles()
{
    
    String dataSetFilename;
    File file; Boolean fileExists;
    
    //check if data file exists for the current data set number
    do {
        
        dataSetFilename = createSimulationDataFilename("20 - ", ".dat");
        file = new File(dataSetFilename);
        fileExists = file.exists();
        
        if (!fileExists){
            //if already at 1, don't load data -- set simulation RANDOM mode
            if(currentDataSetIndex == 1){ 
                setSimulationType(RANDOM); 
                return; 
            }
            
            //if not already at 1, assume last set reached and start over at 1
            currentDataSetIndex = 1;
            
        }

    }while(!fileExists);
    
    FileInputStream fileInputStream = null;
    InputStreamReader inputStreamReader = null;
    BufferedReader traceSimData = null;

    try{

        fileInputStream = new FileInputStream(dataSetFilename);
        inputStreamReader = new InputStreamReader(fileInputStream);

        traceSimData = new BufferedReader(inputStreamReader);

        for (BoardChannelSimulator boardChannel : boardChannels) {
            boardChannel.prepareNextSimulationDataSetFromFiles(
             simulationDataSourceFilePath, currentDataSetIndex, traceSimData);
        }
                
    }        
    catch (FileNotFoundException e){
        return;
        }
    finally{
        try{if (traceSimData != null) {traceSimData.close();}}
        catch(IOException e){}
        try{if (inputStreamReader != null) {inputStreamReader.close();}}
        catch(IOException e){}
        try{if (fileInputStream != null) {fileInputStream.close();}}
        catch(IOException e){}
    }
        
    //move to the next data set
    currentDataSetIndex++;    
    
}//end of UTSimulator::prepareNextSimulationDataSetFromFiles
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setSimulationType
//
// Sets value of simulationType to pValue for this class and all
// the BoardChannelSimulator objects.
//

public void setSimulationType(int pValue)
{

    simulationType = pValue;
    
    for (BoardChannelSimulator boardChannel : boardChannels) {
        boardChannel.setSimulationType(pValue);
    }

}//end of UTSimulator::setSimulationType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::tossDSPMessageRemainder
//
// Reads and discards the remainder of a DSP message so that a resync error
// won't occur.  This is used for functions which don't use the remaining
// bytes.

public void tossDSPMessageRemainder()
{

    readBytes(12); //read in the rest of the packet

}//end of UTBoard::tossDSPMessageRemainder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readDSPStatus
//
// Simulates returning of the DSP status flags.
//
// Returns number of bytes read.
//

int readDSPStatus(int pDSPChip, int pDSPCore)
{
        
    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //send standard packet header
    sendPacketHeader(UTBoard.MESSAGE_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore);

    //send core and status flags back
    sendBytes((byte)UTBoard.DSP_GET_STATUS_CMD, (byte)pDSPCore,
                (byte)0, (byte)1);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::readDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::sendACK
//
// Simulates returning of the DSP acknowledge packet.
//
// The low byte of resync count is returned in the packet.
//
// wip mks -- currently returns 0xaa55 instead of the resync count
//

void sendACK(int pDSPChip, int pDSPCore)
{

    //send standard packet header
    sendPacketHeader(UTBoard.MESSAGE_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore);

    //send core and status flags back
    sendBytes((byte)UTBoard.DSP_ACKNOWLEDGE, (byte)pDSPCore, (byte)0xa5);

}//end of UTSimulator::sendACK
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPFlags1
//
// Sets control flags 1 for the DSP core.
//

int setDSPFlags1(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the word
    int flags1 = (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPGateSigProcTuningValues
//
// Sets DSP gate signal processing tuning values for specified core.
//

int setDSPGateSigProcTuningValues(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    int x = 0;
    
    //parse the words
    int value1 = (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);
    int value2 = (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);
    int value3 = (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);    
    
    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPGateSigProcTuningValues
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPSoftwareGain
//
// Simulates setting the software gain applied by the DSP core.
//

int setDSPSoftwareGain(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the word
    int gain = (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPHitMissCounts
//
// Sets Hit and Miss counts for the DSP core.
//

int setDSPHitMissCounts(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words

    int hitCount = (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    int missCount = (int)((inBuffer[2]<<8) & 0xff00) + ((inBuffer[3]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPHitMissCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPRectification
//
// Sets the signal rectification mode for the DSP core.
//

int setDSPRectification(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the byte

    int rectification = (inBuffer[0]);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPRectification
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPADSampleSize
//
// Sets AD sample size for the DSP core.
//

int setDSPADSampleSize(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the word
    int adSampleSize =
                      (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPADSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPDelays
//
// Sets the software delay and the hardware delay for the A/D sample set and
// the AScan dataset for the DSP core.
//

int setDSPDelays(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words

    int aScanDelay = (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    int hardwareDelay =
     (int)((inBuffer[2]<<24) & 0xff000000) + (int)((inBuffer[3]<<16) & 0xff0000)
            + (int)((inBuffer[4]<<8) & 0xff00) + ((inBuffer[5]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPGate
//
// Sets the parameters for a gate for a DSP core.
//

int setDSPGate(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words
    //wip mks -- not done yet because there a a lot of variables --
    // see "setGate" in "Capulin UT DSP.asm" for details

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPDAC
//
// Sets the parameters for a DAC gate for a DSP core.
//

int setDSPDAC(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words
    //wip mks -- not done yet because there a a lot of variables --
    // see "setDAC" in "Capulin UT DSP.asm" for details

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPGateFlags
//
// Sets the parameters for a gate for a DSP core.
//

int setDSPGateFlags(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words
    int gate = ((inBuffer[0]) & 0xff);
    int flags = (int)((inBuffer[1]<<8) & 0xff00) + ((inBuffer[2]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPDACFlags
//
// Sets the parameters for a DAC gate for a DSP core.
//

int setDSPDACFlags(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words
    int gate = ((inBuffer[0]) & 0xff);
    int flags = (int)((inBuffer[1]<<8) & 0xff00) + ((inBuffer[2]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPDACFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPAScanScale
//
// Sets AScan compression scale for the DSP core and the size of each batch
// to process at a time when using slow processing mode.
//

int setDSPAScanScale(int pDSPChip, int pDSPCore)
{

    //read return and receive packet size
    readBytes(2);

    int returnPktSize = inBuffer[0];
    int receivePktSize = inBuffer[1];

    //read remainder of packet plus checksum byte
    readBytes(receivePktSize + 1);

    //parse the words

    int scale = (int)((inBuffer[0]<<8) & 0xff00) + ((inBuffer[1]) & 0xff);

    int batchSize = (int)((inBuffer[2]<<8) & 0xff00) + ((inBuffer[3]) & 0xff);

    sendACK(pDSPChip, pDSPCore);

    //read packet + two size bytes + checksum byte
    return(receivePktSize + 3);

}//end of UTSimulator::setDSPAScanScale
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::writeFPGA
//
// Simulates writing to a register in the FPGA on a UT board.
//

void writeFPGA()
{

    readBytes(2); //read in the rest of the packet

    //set delay count and sample count registers if applicable

    for (int i=0; i<4; i++){
        boardChannels[i].setDelayCount(inBuffer[0], inBuffer[1]);
        boardChannels[i].setSampleCount(inBuffer[0], inBuffer[1]);
    }

}//end of UTSimulator::writeFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readFPGA
//
// Simulates reading of a register in the FPGA on a UT board.
//

void readFPGA()
{

    readBytes(1); //read in the rest of the packet

    if (inBuffer[0] == UTBoard.CHASSIS_SLOT_ADDRESS) {getChassisSlotAddress();}

}//end of UTSimulator::readFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::getStatus
//
// Simulates returning of the status byte.
//

void getStatus()
{

    //send standard packet header
    sendPacketHeader(UTBoard.GET_STATUS_CMD, (byte)0, (byte)0);

    sendBytes(status, (byte)0);

}//end of UTSimulator::getStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::getDSPRamBlockChecksum
//
// Simulates returning of the checksum of a DSP data block.
//
// The DSP data is not actually saved by the simulator -- checksums for each
// contiguous block will have been saved in an array so those checksums can
// be returned by this method.
//

void getDSPRamBlockChecksum()
{

    readBytes(8); //read in the rest of the packet

    int checksum = 0;
    int x = 0;
    byte chip = inBuffer[x++];
    byte core = inBuffer[x++];
    byte page = inBuffer[x++];
    int address =
            (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);
    // blockSize is not used in current version
    //int blockSize =
    //        (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);
    for (
      RamMemoryBlockChecksum ramMemoryBlockChecksum : ramMemoryBlockChecksums) {
        if ((!ramMemoryBlockChecksum.empty)
                && (ramMemoryBlockChecksum.chip == chip)
                && (ramMemoryBlockChecksum.core == core)
                && (ramMemoryBlockChecksum.page == page)
                && (ramMemoryBlockChecksum.startAddress == address)) {
            checksum = ramMemoryBlockChecksum.checksum;
            break;
        } //if ((!ramMemoryBlockChecksums[i].empty) &&...
    } // for (int i = 0; i < ramMemoryBlockChecksums.length; i++)


    //send standard packet header
    sendPacketHeader(UTBoard.GET_DSP_RAM_BLOCK_CHECKSUM, chip, core);

    //return the lower two bytes of the checksum
    sendBytes((byte)((checksum >> 8) & 0xff),(byte)(checksum & 0xff));

}//end of UTSimulator::getDSPRamBlockChecksum
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::loadFPGA
//
// Simulates loading of the FPGA configuration bitstream.
//

void loadFPGA()
{

//not used right now:
//the getStatus function always returns flag that says FPGA is already loaded
//so this function never gets called

}//end of UTSimulator::loadFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::writeDSP
//
// Simulates writing to DSP RAM on a UT board.
//
// For each block of data stored in contiguous memory locations, the data
// are summed. If the address jumps to a non-contiguous location, info for that
// block is stored separately as a new block. This sum is later used to return
// the checksum for each block when requested for code verification.
//
// The data itself is not actually saved, just the sum of the data for each
// contiguous block. This is a quick and dirty method to allow the checksum
// to be returned when the host requests it for verification.
//
// This workaround does not catch cases where the host adds data more to an
// existing section. Even though the block would be contiguous, this function
// only considers a section to be contiguous if the data was all placed at
// the same time -- without jumping to a different section in the midst. As
// the host generally does not do this, this issue is usally moot.
//
// NOTE: This workaround assumes that the host is pretty much using writeDSPRam
//  only to send DSP code. If the fillRAM function is used to write several
//  non-contiguous blocks, then checksums for those blocks may not be stored
//  as a limited number of blocks is accounted for. This is okay as long as the
//  code load occurs first and uses the available blocks first -- those will
//  not be destroyed by later blocks and they are the only checksums actually
//  needed for verification.
//

void writeDSP()
{

    readBytes(8); //read in the rest of the packet

    int x = 0;
    byte chip = inBuffer[x++];
    byte core = inBuffer[x++];
    byte page = inBuffer[x++];
    int address =
            (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);
    int data =
            (int)((inBuffer[x++]<<8) & 0xff00) + ((inBuffer[x++]) & 0xff);

    //if the code is updated in the future to actually store the data, this is
    // the place to do it -- it might also be necessary in the future to only
    // store bytes in certain sections if the host uses the writeDSP method to
    // install data such as FIR filter coefficents and those are to be simulated
    // -- note that it would be better if the host uses a specific command to do
    // such things rather than writing straight to the RAM using this writeDSP
    // method

    //every time the chip, core, or page are changed or the address is not
    //contiguous with the previous address, start recording info for a new
    //contiguous data block

    if (chip != prevChip || core!= prevCore || page != prevPage ||
                                                address != (prevAddress + 1) ){

        currentBlock = -1;
        //reset these so they will never match again unless empty block is found
        prevChip = -1; prevCore = -1; prevPage = -1; prevAddress = -2;

        //look for the next empty block in the array
        for (int i = 0; i < ramMemoryBlockChecksums.length; i++){

            //if empty block found, prepare for use
            if (ramMemoryBlockChecksums[i].empty){
                ramMemoryBlockChecksums[i].empty = false;
                ramMemoryBlockChecksums[i].chip = chip;
                ramMemoryBlockChecksums[i].core = core;
                ramMemoryBlockChecksums[i].page = page;
                ramMemoryBlockChecksums[i].startAddress = address;
                ramMemoryBlockChecksums[i].numberBytes = 0;
                ramMemoryBlockChecksums[i].checksum = 0;
                currentBlock = i;
                break;
            }// if (ramMemoryBlockChecksums[i].empty)
        }// for (int i = 0; i < ramMemoryBlockChecksums.length; i++)
    }// if (chip != prevChip ||..

    //if no empty block was found, don't store info to it -- this block cannot
    //be verified later by the host
    if (currentBlock == -1) {return;}

    prevChip = chip; prevCore = core; prevPage = page; prevAddress = address;

    ramMemoryBlockChecksums[currentBlock].numberBytes++;
    ramMemoryBlockChecksums[currentBlock].checksum += data;

}//end of UTSimulator::writeDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::writeNextDSP
//
// Simulates writing to the next DSP RAM location on a UT board.
//

void writeNextDSP()
{

    readBytes(4); //read in the rest of the packet

}//end of UTSimulator::writeNextDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readDSP
//
// Simulates reading from DSP RAM on a UT board.
//

void readDSP()
{

    readBytes(6); //read in the rest of the packet

    //send standard packet header
    sendPacketHeader(UTBoard.READ_DSP_CMD, (byte)0, (byte)0);

    int readWord = 0xaa55;

    //send the value back to the host, MSB followed by LSB
    sendBytes((byte)((readWord >> 8) & 0xff), (byte)(readWord & 0xff));

}//end of UTSimulator::readDSP
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// UTSimulator::sendPacketHeader
//
// Sends via the socket: 0xaa, 0x55, 0xaa, 0x55, packet identifier, DSP chip,
// and DSP core.
//
// This is the packet header for Rabbit to Host, not DSP to Rabbit. The code
// calling this function skips the send/receive to DSP step and creates the
// data as if it had been received from the DSP.
//
// Does not flush.
//

void sendPacketHeader(byte pPacketID, byte pDSPChip, byte pDSPCore)
{

    outBuffer[0] = (byte)0xaa; outBuffer[1] = (byte)0x55;
    outBuffer[2] = (byte)0xbb; outBuffer[3] = (byte)0x66;
    outBuffer[4] = (byte)pPacketID; outBuffer[5] = pDSPChip;
    outBuffer[6] = pDSPCore;

    //send packet to remote
    if (byteOut != null) {
        try{
            byteOut.write(outBuffer, 0 /*offset*/, 7);
        }
        catch (IOException e) {
            logSevere(e.getMessage() + " - Error: 925");
        }
    }

}//end of UTSimulator::sendPacketHeader
//----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::getPeakData4()
//
// Returns simulated peak data for four channels on the board.
//

public void getPeakData4()
{

    //retrieve which channels to be retrieved and the number of gates for each
    for (int ch=0; ch<NUMBER_OF_BOARD_CHANNELS; ch++){
        channelPeakSets[ch].channel = getByteFromSocket();
        channelPeakSets[ch].numberOfGates = getByteFromSocket();
    }

    //retrieve wall flags - specify which channel return wall data
    int wallFlags = getByteFromSocket();

    channelPeakSets[0].isWallChannel = (wallFlags & 1) != 0 ? true : false;
    channelPeakSets[1].isWallChannel = (wallFlags & 2) != 0 ? true : false;
    channelPeakSets[2].isWallChannel = (wallFlags & 4) != 0 ? true : false;
    channelPeakSets[3].isWallChannel = (wallFlags & 8) != 0 ? true : false;

    //send standard packet header
    //use 0 for DSP chip and core because the data is from multiple cores
    sendPacketHeader(UTBoard.GET_PEAK_DATA4_CMD, (byte)0, (byte)0);

    //count packets sent -- this is sent to the host to detect missed packets
    peakDataPktCounter++;
    if (peakDataPktCounter > 255){peakDataPktCounter = 0;}

    //send packet count and status
    sendBytes((byte)peakDataPktCounter, (byte)(0));

    //these can be incremented in the future - use test values for now
    int encoder1 = 1234; int encoder2 = 5678;

    //send the encoder values to the host
    sendInteger(encoder1); sendInteger(encoder2);

    //send data back for the four channels on the board
    for (int ch=0; ch<NUMBER_OF_BOARD_CHANNELS; ch++){

        //send channel number
        sendBytes((byte)channelPeakSets[ch].channel);
        //send number of gates
        sendBytes((byte)channelPeakSets[ch].numberOfGates);

        //send peak data back for each gate of the channel
        for (int gate=0; gate<channelPeakSets[ch].numberOfGates; gate++){

            //send peak flags - these need to be simulated in future code update
            sendShortInt((short)channelPeakSets[ch].peakFlags);

            //baseline noise
            channelPeakSets[ch].peak = (int)(Math.random()*5);

            //occasional peak
            if(((int)(Math.random()*200)) == 1) {
                channelPeakSets[ch].peak = (int)(Math.random()*100);
            }

            sendShortInt((short)channelPeakSets[ch].peak);

            //for debugging:
            //send the peak, offset by gate so traces will be separated
            //add in a little random value
            //sendShortInt((short)channelPeakSets[ch].peak + (gate * 6)
            //                                           + (int)(Math.random()*3));


            //add next line in to generate a sawtooth wave form
            //if (channelPeakSets[ch].peak++ > 100) channelPeakSets[ch].peak = 0;


            //send the flight time for the peak
            sendShortInt((short)channelPeakSets[ch].flightTime);
            //send the position track for the peak
            sendShortInt((short)channelPeakSets[ch].track);

        }// for (int gate=0; gate<channelPeakSets[ch].numberOfGates; gate++)

        //if the channel is a wall type, return wall data
        if (channelPeakSets[ch].isWallChannel){

            channelPeakSets[ch].flightTime = 1;

            //maximum wall values

            sendShortInt(boardChannels[ch].getNextMaxWallValue());
        
            sendShortInt((short)0);  //start fractional distance numerator

            sendShortInt((short)1);  //start fractional distance denominator

            sendShortInt((short)0);  //end fractional distance numerator

            sendShortInt((short)1);  //end fractional distance denominator

            sendShortInt((short)99);  //tracking location of data

            //minimum wall values

            sendShortInt(boardChannels[ch].getNextMinWallValue());            
            
            sendShortInt((short)0);  //start fractional distance numerator

            sendShortInt((short)1);  //start fractional distance denominator

            sendShortInt((short)0);  //end fractional distance numerator

            sendShortInt((short)1);  //end fractional distance denominator

            sendShortInt((short)99);  //tracking location of data

        }// if (channelPeakDatas[ch].isWallChannel)

    }// for (int ch=0; ch<3; ch++)

}//end of UTSimulator::getPeakData4
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::sendWallMapPacket
//
// Returns simulated wall mapping data. This packet type is not requested by
// the host but is sent asynchronously. The transmittal is triggered by a
// specified number of samples being read by the Rabbit from the DSP.
//

public void sendWallMapPacket()
{

    //send standard packet header
    //use1 for DSP chip and 0 for core because the data is from cores A & B
    sendPacketHeader(UTBoard.GET_WALL_MAP_CMD, (byte)1, (byte)0);

    //count packets sent -- this is sent to the host to detect missed packets
    peakDataPktCounter++;
    if (peakDataPktCounter > 255){peakDataPktCounter = 0;}

    //send packet count and status
    sendBytes((byte)peakDataPktCounter, (byte)(0));

    //simulate and send the wall data points
    for (int i=0; i < MAP_BUFFER_SIZE; i++){

        short wallDataPoint = 
                (short)boardChannels[boardChannelForMapDataSource].
                                                        getNextWallMapValue(2);

        sendShortInt(wallDataPoint);
        
    }

}//end of UTSimulator::sendWallMapPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::getAScan()
//

private void getAScan()
{

    //next byte is the DSP chip to be read from
    int chip = getByteFromSocket();

    //next byte is the core in the DSP chip to be read from
    int core = getByteFromSocket();

    //next byte is the channel
    int channel = getByteFromSocket();

    //discard the checksum byte
    int checksum = getByteFromSocket();

    //simulateAScan(channel); //create a simulated AScan
    simulateSineWaveAScan(channel); //create a sine wave for the AScan

    //send standard packet header
    sendPacketHeader(UTBoard.GET_ASCAN_CMD, (byte)chip, (byte)core);

    sendDataBlock(804, aScanBuffer);

}//end of UTSimulator::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::simulateSineWaveAScan
//
// Returns a sine wave in the AScan buffer.
// Scale is always returned as 1, interface crossing as 0.
//
// The variable set to fill is specified by pChannel.
//

public void simulateSineWaveAScan(int pChannel)
{

    int SAMPLE_FREQUENCY =  66000000;
    int FREQUENCY = 6000000;

    //sample rate is 66Mhz
    // 1 Mhz would fill 66 samples
    // 5 Mhz would fill 13.2 samples

    int angleStep = 360 / (SAMPLE_FREQUENCY / FREQUENCY);

    int []aScan = aScanBuffer;

    //read and save to quench "only read from" warning
    int j = aScan[0]; aScan[0] = j;

    j = 0;

    //first byte returned is the channel
    aScan[j++] = pChannel;
    //next byte returned is the compression range
    aScan[j++] = 1;
    //next two byes are the interface crossing position integer
    int iFaceCrossing = 0;
    aScan[j++] = (byte)((iFaceCrossing >> 8) & 0xff);
    aScan[j++] = (byte)((iFaceCrossing) & 0xff);

    int simData;

    mainBangSineAngle = 0;

    //fill the array with a sine wave
    for (int i=0; i<400; i++){

        simData = (int)
             ((Math.sin(Math.toRadians(mainBangSineAngle++ * angleStep)) * 50));

        //place the data into the aScan buffer as MSB/LSB
        aScan[j++] = (byte)((simData >> 8) & 0xff);
        aScan[j++] = (byte)((simData) & 0xff);

        }

}//end of UTSimulator::simulateSineWaveAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::simulateAScan
//
// Simulates an AScan dataset skipping delayCount number of samples and
// recording sampleCount number of samples.  The variable set to fill is
// specified by pChannel.
//
// Simulates the Ascan data from a transducer.  The signal level in the gate 1
// area is scaled by class variable fineGain.  A sin wave is generated at time 0
// to simulate the main bang.  Another sin wave is generated in the gate 1 time
// span to simulate reflection from the target.
//
// The main bang occurs at time zero and lasts about three cycles.  At twenty
// data points per cycle, the bang lasts for the first 60 data points.
//
// Round trip in water path 1.5" * 2 (round trip) = 50.93 uS
// Each data point is 50.93 uS / 0.01667 uS per point = 3055 data points before
// the reflection will be received.
//
// NOTE: this is just a quick, crappy simulation.  Needs to be improved.
//

public void simulateAScan(int pChannel)
{

    //int delayCount = boardChannels[pChannel].delayCount;
    //int lSampleCount = boardChannels[pChannel].sampleCount;
    //int gain = boardChannels[pChannel].dspGain;

    mainBangSineAngle = 1;
    reflectionSineAngle = 1;

    int []aScan = aScanBuffer;

    //read and save to quench "only read from" warning
    int j = aScan[0]; aScan[0] = j;

    j = 0;

    //first byte returned is the channel
    aScan[j++] = pChannel;
    //next byte returned is the compression range (wip mks - needs to adjust)
    aScan[j++] = 1;
    //next two byes are the interface crossing position integer
    int iFaceCrossing = 40 + (int)(Math.random()*20);
    aScan[j++] = (byte)((iFaceCrossing >> 8) & 0xff);
    aScan[j++] = (byte)((iFaceCrossing) & 0xff);

    int simData;

    //fill the array with data - generate a spike for the interface
    for (int i=0; i<400; i++){
        if (i==iFaceCrossing) {
            simData = 280;
        }
        else{
            simData = (int)(Math.random()*10);
            }
        aScan[j++] = (byte)((simData >> 8) & 0xff);
        aScan[j++] = (byte)((simData) & 0xff);
    }

    /*

    while (i < (delayCount + lSampleCount)){

        //main bang for first 60 data points
        if ((i) <= 60) simulateMainBang(aScan, j, gain);
        else
        //reflection for 60 data points at 50uS
        if ((i > 3055) && (i < 3300)) simulateReflection(aScan, j, gain);
        else
        aScan[j] = (int)(Math.random()*5); //small noise when no signal

        i++; j++;
        }

     */

}//end of UTSimulator::simulateAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::simulateMainBang
//
// Places a sine wave into the buffer to simulate the main bang.
//
// multiply i by 18 to give correct frequency:
// Ttransducer is simulated 3 mHz, sample rate is simulated 60 mHz which gives
// 20 data points per cycle.  One cycle is 360 degrees, so i*18 will give
// 360 degrees at i=20, one full cycle.
//

void simulateMainBang(int[] pBuffer, int pIndex, double pGain)
{

    //use mainBangSineAngle to index the sine wave so it can be independent of
    //the array index - allows sine wave to start at any data point

    //multiply by 10000 to give large main bang, divide by mainBangSineAngle to
    //give sin(x)/x decay function to show attenuation

    pBuffer[pIndex] =
       (int)((Math.sin(Math.toRadians(mainBangSineAngle++*18)) * 10000)
                                                           / mainBangSineAngle);

    //if the signal is attenuated down to zero, add some noise
    if (pBuffer[pIndex] == 0) {
        pBuffer[pIndex] = (int)(Math.random()*5);
    }

}//end of UTSimulator::simulateMainBang
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::simulateInterface
//
// Simulates some fuzzy stuff to look like the interface.
//
//

void simulateInterface(int[] pBuffer, int pIndex, double pGain)
{

    ifaceProfileSize = ifaceProfile.length;

    //interfaceProfileCounter;

    if (ifaceProfileCounter < ifaceProfileSize){
        pBuffer[pIndex] = ifaceProfile[ifaceProfileCounter];
        ifaceProfileCounter++;
    }
    else {
        pBuffer[pIndex] = 25;
    }

    //apply gain
    pBuffer[pIndex] *= pGain;

    //add some noise
    pBuffer[pIndex] += (int)(Math.random()*10);

}//end of UTSimulator::simulateInterface
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::simulateReflection
//
// Places a sine wave into the buffer to simulate the reflection from the
// target.
//
//
// multiply i by 18 to give correct frequency:
// Ttransducer is simulated 3 mHz, sample rate is simulated 60 mHz which gives
// 20 data points per cycle.  One cycle is 360 degrees, so i*18 will give
// 360 degrees at i=20, one full cycle.
//

void simulateReflection(int[] pBuffer, int pIndex, double pGain)
{

    //use reflectionSineAngle to index the sine wave so it can be independent of
    //the array index - allows sine wave to start at any data point

    //reflectionSineAngle to give sin(x)/x decay function to show attenuation

    int attenuation = (byte)(reflectionSineAngle/20.0);
    if (attenuation < 1) {attenuation = 1;}

    //multiplying the angle by 12 gives approx 2.25Mhz pulse when sampling
    //period is 15 ns

    pBuffer[pIndex] =
         (int)(((Math.sin(Math.toRadians(reflectionSineAngle++*12)) * pGain)
                                                         * 2) / attenuation );

    //if the signal is attenuated down to zero, add some noise
    if (pBuffer[pIndex] == 0) {pBuffer[pIndex] = (int)(Math.random()*5);}

}//end of UTSimulator::simulateReflection
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::configureMain
//
// Loads configuration settings from the "01 - Simulation Main Info.ini" file.
// The various child objects are then created as specified by the config data.
//
// This info handles all set up for use with all the file in the
// specified simulation source data folder. In addition, each group of
// simulation files also has a config file specific to that group. Each group
// generally provides data for a different run, so different sets of data can
// be simulated for subsequent runs.
//
// Each instance must open its own iniFile object because they are created
// simultaneously in different threads.  The iniFile object is not guaranteed
// to be thread safe.
//

@Override
public void configureMain(int pBoardNumber) throws IOException
{

    try {
        //open the config file and load common settings
        super.configureMain(pBoardNumber);
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 1629");
        return;
    }

    String section = "Simulated UT Board " + (utBoardNumber + 1);

    chassisAddr = (byte)configFile.readInt(section, "Chassis Number", 0);

    chassisAddr = (byte)(~chassisAddr); //UTBoard returns inverted values

    slotAddr = (byte)configFile.readInt(section, "Slot Number", 0);

    slotAddr = (byte)(~slotAddr); //UTBoard returns inverted values

    String value;
    
    enabled = configFile.readBoolean(section, "Enabled", true);

    value = configFile.readString(section, "Simulation Type", "Random");
    
    parseSimulationType(value);
    
    value = configFile.readString(section, "Type", "Basic Peak Collector");

    parseBoardType(value);

    traceBufferSize = configFile.readInt(section, 
                                             "Number Of Trace Data Points", 0);

    if(traceBufferSize > 10000){ traceBufferSize = 10000; }

    mapChannel =
      configFile.readInt(section, "This Board is Source for Map Channel", -1);

    boardChannelForMapDataSource =
         configFile.readInt(section, "Board Channel for Map Data Source", -1);

    headForMapDataSensor =
              configFile.readInt(section, "Head for Map Data Sensor", -1);

    distanceMapSensorToFrontEdgeOfHead = configFile.readDouble(section,
                    "Distance From Map Data Sensor to Front Edge of Head", 0);

    mapBufferSize = configFile.readInt(section, "Map Data Buffer Size", 0);

    if(mapBufferSize > 100000000){ mapBufferSize = 100000000; }

    //create an array of channel variables
    boardChannels = new BoardChannelSimulator[MAX_BOARD_CHANNELS];
    for (int i=0; i<boardChannels.length; i++) {
        
        int lMapChannel = -1;
        if (i == boardChannelForMapDataSource) { lMapChannel = mapChannel;}
    
        boardChannels[i] = new BoardChannelSimulator(i, traceBufferSize, type,
        simulationType, UTBoard.CH1_SAMPLE_DELAY_0, UTBoard.CH1_SAMPLE_COUNT_0,
        mapChannel);
        boardChannels[i].init();
        boardChannels[i].configure(configFile, section);
    }
        
}//end of UTSimulator::configureMain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::configureSimulationDataSet
//
// Loads configuration settings for the data set to be used for the current run.
// Each simulation data source folder may contain multiple data sets, each with
// a different identifying number. These different sets are used to provide a
// different simulation for each successive run.
//

@Override
public void configureSimulationDataSet()
{
    
    try {
        String fullPath =
                createSimulationDataFilename("20 - ", " Simulation Info.ini");
        configFile = new IniFile(fullPath, mainFileFormat);
        configFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 1629");
        return;
    }

    String section = "Simulated UT Board " + (utBoardNumber + 1);

    nominalWall = configFile.readDouble("Wall", "Nominal Wall", 0.250);

    nSPerDataPoint = configFile.readDouble("Wall", "nS per Data Point", 15.0);
    
    uSPerDataPoint = nSPerDataPoint / 1000;
    
    double velocityUS =
              configFile.readDouble("Wall", "Velocity (distance/uS)", 0.233);
    
    compressionVelocityNS = velocityUS / 1000;
    
    numWallMultiples = configFile.readInt(
                                    "Wall", "Number of Multiples for Wall", 1);
    
    inchesPerChartPercentagePoint = configFile.readDouble(
                        "Wall", "Inches per Chart Percentage Point", 0.002);
    
    for (BoardChannelSimulator boardChannel : boardChannels) {
        boardChannel.setWallParameters(nominalWall, nSPerDataPoint, 
          uSPerDataPoint, velocityUS, compressionVelocityNS, numWallMultiples,
          inchesPerChartPercentagePoint);
    }

}//end of UTSimulator::configureSimulationDataSet
//-----------------------------------------------------------------------------
                
}//end of class UTSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
