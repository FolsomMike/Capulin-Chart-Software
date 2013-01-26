/******************************************************************************
* Title: UTSimulator.java
* Author: Mike Schoonover
* Date: 5/24/09
*
* Purpose:
*
* This class simulates a TCP/IP connection between the host and UT boards.
*
* This is a subclass of Socket and can be substituted for an instance
* of that class when simulated data is needed.
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
// class BoardChannel
//
// This class encapsulates data for a single channel on a UT board.
//

class BoardChannel extends Object{

    int index;

    int delayCount; //number of samples to skip for delay - set by Host
    byte delayCount0, delayCount1, delayCount2, delayCount3;

    int sampleCount; //number of samples to record - set by Host
    byte sampleCount0, sampleCount1, sampleCount2;

    int dspGain;

    byte sampleDelayReg0;
    byte sampleDelayReg1;
    byte sampleDelayReg2;
    byte sampleDelayReg3;

    byte sampleCountReg0;
    byte sampleCountReg1;
    byte sampleCountReg2;

//-----------------------------------------------------------------------------
// BoardChannel::BoardChannel (constructor)
//

public BoardChannel(int pIndex, byte pSampleDelayReg0, byte pSampleCountReg0)
{

    index = pIndex;

    // the FPGA register addresses for all channels for sample delay and sample
    // count are contiguous addresses, so use a bit of math to calculate each
    // address from the first one

    sampleDelayReg0 = (byte) (pSampleDelayReg0 + (index * 4));
    sampleDelayReg1 = (byte) (pSampleDelayReg0 + 1 + (index * 4));
    sampleDelayReg2 = (byte) (pSampleDelayReg0 + 2 + (index * 4));
    sampleDelayReg3 = (byte) (pSampleDelayReg0 + 3 + (index * 4));

    sampleCountReg0 = (byte) (pSampleCountReg0 + (index * 3));
    sampleCountReg1 = (byte) (pSampleCountReg0 + 1 + (index * 3));
    sampleCountReg2 = (byte) (pSampleCountReg0 + 2 + (index * 3));

}//end of BoardChannel::BoardChannel (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannel::setDelayCount
//
// Checks if pRegAddr matches one of the delayCount byte register addresses
// and stores pValue in that register if so.
//
// Concatenates all bytes set by the host computer to form the value for the
// delayCount.  Since the value might be illegal when only some of the bytes
// have been set, it is checked for out of bounds.
//

void setDelayCount(byte pRegAddr, byte pValue){

    //since the host computer writes to the variables one byte at a time in the
    //FPGA registers, the bytes are collected individually and then converted
    //to the full values - the values must be tested for out of bounds each time
    //as it could be illegal when only some of the bytes have been updated

    if (pRegAddr == sampleDelayReg0) {delayCount0 = pValue;}
    if (pRegAddr == sampleDelayReg1) {delayCount1 = pValue;}
    if (pRegAddr == sampleDelayReg2) {delayCount2 = pValue;}
    if (pRegAddr == sampleDelayReg3) {delayCount2 = pValue;}


    delayCount = (int)(
        ((delayCount3<<24) & 0xff000000) + ((delayCount2<<16) & 0xff0000)
         + ((delayCount1<<8) & 0xff00) + (delayCount0 & 0xff)
        );

    //the hardware uses a 4 byte unsigned integer - Java doesn't do unsigned
    //easily, so the max value is limited to the maximum positive value Java
    //allows for a signed integer - this limitation is also used for the
    //hardware even though it could handle a larger number

    if (delayCount < 0) {delayCount = 0;}
    if (delayCount > UTBoard.MAX_DELAY_COUNT) {
        delayCount = UTBoard.MAX_DELAY_COUNT;
    }

}//end of BoardChannel::setDelayCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// BoardChannel::setSampleCount
//
// Checks if pRegAddr matches one of the sampleCount byte register addresses
// and stores pValue in that register if so.
//
// Concatenates all bytes set by the host computer to form the value for the
// sampleCount.  Since the value might be illegal when only some of the bytes
// have been set, it is checked for out of bounds.
//

void setSampleCount(byte pRegAddr, byte pValue){


    //since the host computer writes to the variables one byte at a time in the
    //FPGA registers, the bytes are collected individually and then converted
    //to the full values - the values must be tested for out of bounds each time
    //as it could be illegal when only some of the bytes have been updated

    if (pRegAddr == sampleCountReg0) {sampleCount0 = pValue;}
    if (pRegAddr == sampleCountReg1) {sampleCount1 = pValue;}
    if (pRegAddr == sampleCountReg2) {sampleCount2 = pValue;}


    sampleCount = (int)(
        ((sampleCount2<<16) & 0xff0000)
         + ((sampleCount1<<8) & 0xff00) + (sampleCount0 & 0xff)
        );

    //the hardware uses a 3 byte unsigned integer - Java doesn't do unsigned
    //easily, so the max value is limited to the maximum positive value Java
    //allows for a signed integer

    if (sampleCount < 0) {sampleCount = 0;}
    if (sampleCount > UTBoard.MAX_SAMPLE_COUNT) {
        sampleCount = UTBoard.MAX_SAMPLE_COUNT;
    }

}//end of BoardChannel::setSampleCount
//-----------------------------------------------------------------------------

}//end of class BoardChannel
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

    static int MAX_BOARD_CHANNELS = 4;
    BoardChannel[] boardChannels;
    String mainFileFormat;
    int peakDataPktCounter = 0;

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

//-----------------------------------------------------------------------------
// UTSimulator::UTSimulator (constructor)
//

public UTSimulator(InetAddress pIPAddress, int pPort, String pMainFileFormat)
                                                        throws SocketException
{

    //call the parent class constructor
    super(pIPAddress, pPort);

    mainFileFormat = pMainFileFormat;

    //give each UT board a unique number so it can load data from the
    //simulation.ini file and such
    //as that number is distributed across all sub classes -- UT boards,
    //Control boards, etc.
    utBoardNumber = utBoardCounter++;

    status = UTBoard.FPGA_LOADED_FLAG;

    //create an array of channel variables
    boardChannels = new BoardChannel[MAX_BOARD_CHANNELS];
    for (int i=0; i<MAX_BOARD_CHANNELS; i++) {
        boardChannels[i] = new BoardChannel(
                    i, UTBoard.CH1_SAMPLE_DELAY_0, UTBoard.CH1_SAMPLE_COUNT_0);
    }

    channelPeakSets = new ChannelPeakSet[NUMBER_OF_BOARD_CHANNELS];
    for (int i=0; i < NUMBER_OF_BOARD_CHANNELS; i++) {
        channelPeakSets[i] = new ChannelPeakSet();
    }

    ramMemoryBlockChecksums =
                        new RamMemoryBlockChecksum[NUMBER_OF_RAM_MEMORY_BLOCKS];
    for (int i=0; i < NUMBER_OF_RAM_MEMORY_BLOCKS; i++) {
        ramMemoryBlockChecksums[i] = new RamMemoryBlockChecksum();
    }

    //load configuration data from file
    configure();

    aScanBuffer = new int[ASCAN_BUFFER_SIZE]; //used to store simulated A/D data

    //create an out writer from this class - will be input for some other class
    //this writer is only used to send the greeting back to the host

    PrintWriter out = new PrintWriter(localOutStream, true);
    out.println("Hello from UT Board!");

}//end of UTSimulator::UTSimulator (constructor)
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

    return x;

}//end of UTSimulator::processDataPackets
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

        return 0;

    }//try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 430");
    }

    return 0;

}//end of UTSimulator::processDataPacketsHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::processDSPMessage
//
// Processes a message to a DSP.
//

private int processDSPMessage()
{

    try{
        while(byteIn.available() < 3){}
        byteIn.read(inBuffer, 0, 3);
    }// try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 452");
    }

    int dspChip = inBuffer[0];
    int dspCore = inBuffer[1];
    int dspMsgID = inBuffer[2];

    if ( dspMsgID == UTBoard.DSP_GET_STATUS_CMD) {
        return (readDSPStatus(dspChip, dspCore));
    }

    //clear out the remaining bytes of any unhandled DSP message
    tossDSPMessageRemainder();

    return 0;

}//end of UTBoard::processDSPMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::tossDSPMessageRemainder
//
// Reads and discards the remainder of a DSP message so that a resync error
// won't occur.  This is used for functions which don't use the remaining
// bytes.

public void tossDSPMessageRemainder()
{

    try{
        while(byteIn.available() < 12){}
        byteIn.read(inBuffer, 0, 12);
    }// try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 485");
    }

}//end of UTBoard::tossDSPMessageRemainder
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readDSPStatus
//
// Simulates returning of the DSP status flags.
//

int readDSPStatus(int pDSPChip, int pDSPCore)
{

    try{
        while(byteIn.available() < 12){}
        byteIn.read(inBuffer, 0, 12);
    }// try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 505");
    }

    //send standard packet header
    sendPacketHeader(UTBoard.MESSAGE_DSP_CMD, (byte)0, (byte)0);

    //send core and status flags back
    sendBytes((byte)UTBoard.DSP_GET_STATUS_CMD, (byte)pDSPCore,
                (byte)0, (byte)1);

    return 12;

}//end of UTSimulator::readDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::setDSPGain
//
// Simulates setting the software gain applied by the DSP.
//

void setDSPGain()
{

    try{byteIn.read(inBuffer, 0, 3);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 531");
    }

    boardChannels[inBuffer[0]].dspGain = inBuffer[1];

}//end of UTSimulator::setDSPGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::writeFPGA
//
// Simulates writing to a register in the FPGA on a UT board.
//

void writeFPGA()
{

    try{byteIn.read(inBuffer, 0, 2);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 550");
    }

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

    try{byteIn.read(inBuffer, 0, 1);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 574");
    }

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

    try{byteIn.read(inBuffer, 0, 8);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 614");
    }

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

    //look for the block entry which matches the chip, core, page and address

    for (int i = 0; i < ramMemoryBlockChecksums.length; i++){
        if ((!ramMemoryBlockChecksums[i].empty) &&
            (ramMemoryBlockChecksums[i].chip == chip) &&
            (ramMemoryBlockChecksums[i].core == core) &&
            (ramMemoryBlockChecksums[i].page == page) &&
            (ramMemoryBlockChecksums[i].startAddress == address)){

            checksum = ramMemoryBlockChecksums[i].checksum;

            break;

        }//if ((!ramMemoryBlockChecksums[i].empty) &&...
    }// for (int i = 0; i < ramMemoryBlockChecksums.length; i++)


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

    try{byteIn.read(inBuffer, 0, 8);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 704");
    }

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

    try{byteIn.read(inBuffer, 0, 4);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 776");
    }

}//end of UTSimulator::writeNextDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTSimulator::readDSP
//
// Simulates reading from DSP RAM on a UT board.
//

void readDSP()
{

    try{byteIn.read(inBuffer, 0, 6);}
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 793");
    }

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
            System.err.println(getClass().getName() + " - Error: 830");
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

            short wall;

            wall = (short)(120 + (Math.random()*10));

            sendShortInt(wall);  //wall max peak uS distance

            sendShortInt((short)0);  //start fractional distance numerator

            sendShortInt((short)1);  //start fractional distance denominator

            sendShortInt((short)0);  //end fractional distance numerator

            sendShortInt((short)1);  //end fractional distance denominator

            sendShortInt((short)99);  //tracking location of data

            //minimum wall values

            wall = (short)(95 + (Math.random()*10));

            //occasional peak
            if(((int)(Math.random()*200)) == 1) {
                wall -= (int)(Math.random()*30);
            }

            sendShortInt(wall);  //wall min peak uS distance

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
// UTSimulator::getAScan()
//

public void getAScan()
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

    int j = 0;

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

    int delayCount = boardChannels[pChannel].delayCount;
    int sampleCount = boardChannels[pChannel].sampleCount;
    int gain = boardChannels[pChannel].dspGain;

    mainBangSineAngle = 1;
    reflectionSineAngle = 1;

    int []aScan = aScanBuffer;

    int i, j = 0;

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
    for (i=0; i<400; i++){
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

    while (i < (delayCount + sampleCount)){

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
// UTSimulator::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//
// Each instance must open its own iniFile object because they are created
// simultaneously in different threads.  The iniFile object is not guaranteed
// to be thread safe.
//

private void configure()
{

    IniFile configFile;

    //if the ini file cannot be opened and loaded, exit without action
    try {configFile = new IniFile("Simulation.ini", mainFileFormat);}
        catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 1236");
        return;
        }

    String section = "Simulated UT Board " + (utBoardNumber + 1);

    chassisAddr = (byte)configFile.readInt(section, "Chassis Number", 0);

    chassisAddr = (byte)(~chassisAddr); //the switches invert the value

    slotAddr = (byte)configFile.readInt(section, "Slot Number", 0);

    slotAddr = (byte)(~slotAddr); //the switches invert the value

}//end of UTSimulator::configure
//-----------------------------------------------------------------------------

}//end of class UTSimulator
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
