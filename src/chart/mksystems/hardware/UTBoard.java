/******************************************************************************
* Title: UTBoard.java
* Author: Mike Schoonover
* Date: 5/7/09
*
* Purpose:
*
* This class interfaces with a Capulin UT board.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import java.io.*;
import java.net.*;
import javax.swing.*;
import java.lang.Math.*;

import chart.mksystems.inifile.IniFile;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class UTBoard
//
// This class creates and handles an interface to a UT board.
//

public class UTBoard extends Board{

    int debug = 0; //debug mks

boolean fpgaLoaded = false;
String fpgaCodeFilename;
String dspCodeFilename;
double nSPerDataPoint, uSPerDataPoint;
int repRateInHertz, repRate;
int triggerWidth;
int syncWidth;
int pulseDelay;
int numberOfBanks;
int reSyncCount = 0, reSyncDSPChip, reSyncDSPCore, reSyncPktID, reSyncDSPMsgID;
boolean syncSource;
HardwareVars hdwVs;
String jobFileFormat, mainFileFormat;

//ASCAN_MAX_HEIGHT and SIGNAL_SCALE should be changed to variables
//which can be initialized and or adjusted
static int ASCAN_MAX_HEIGHT = 350;

static int MAX_CLOCK_POSITION = 11; //0 to 11 positions
//offset gets added to clock position to account for various sensor positions
//and errors
static int CLOCK_OFFSET = 6;

//WARNING:  In general it is a bad idea to use any value other than 1 for
//        SIGNAL_SCALE.  If greater than 1, the result will look choppy.  Less
//        than 1 and the input device will require needless addition of gain to
//        counterract the scale down.  Let the input device provide the scaling
//        unless it absolutely cannot be done.
//        To make the AScan signal levels match the chart, it may be necessary
//        to scale the AScan up.  This is better than scaling the chart down
//        which needlessly wastes gain.  In general, it is more important to
//        have a good chart presentation than an AScale presentation as the
//        chart is often the permanent record.

static double SIGNAL_SCALE = 1;
static double ASCAN_SCALE = 3.5;

public static int ASCAN_SAMPLE_SIZE = 400;
public static int ASCAN_BUFFER_SIZE = 400;

AScan aScan, aScanBuffer;

AScan [] aScanFIFO;
static int ASCAN_FIFO_SIZE = 25; //fifo is used for smoothing - larger number
                                //allows for more smoothing
int aScanFIFOIndex = 0;

boolean udpResponseFlag = false;

int hardwareDelay;

int aScanCoreID;
int pktDSPChipID, pktDSPCoreID, pktID, dspMsgID;

byte[] readDSPResult;
boolean readDSPDone;

byte[] getDSPRamChecksumResult;
boolean getDSPRamChecksumDone;

int aScanCoreSelector = 1;
//set aScanRcvd flag true so first request for peakData packet will succeed
boolean aScanRcvd = true;
boolean aScanDataPacketProcessed = false;
//set peakDataRcvd flag true so first request for peakData packet will succeed
boolean peakDataRcvd = true;
boolean peakDataPacketProcessed = false;

boolean dspStatusMessageRcvd = false;

int dbug = 0; //debug mks - remove this

int timeOutRead = 0; //use this one in the read functions
int timeOutWFP = 0; //used by processDataPackets

int getAScanTimeOut = 0, GET_ASCAN_TIMEOUT = 50;
int getPeakDataTimeOut = 0, GET_PEAK_DATA_TIMEOUT = 50;


double prevMinThickness, prevMaxThickness;

/*
// 5Mhz center pass, 6 order, 31 tap 4Mhz & 6 Mhz cutoff
int[] firCoef = {
         7757,
        12219,
        14341,
        13208,
         8662,
         1441,
        -6914,
        -14407,
        -19073,
        -19522,
        -15352,
        -7327,
         2761,
        12516,
        19550,
        22108,
        19550,
        12516,
         2761,
        -7327,
        -15352,
        -19522,
        -19073,
        -14407,
        -6914,
         1441,
         8662,
        13208,
        14341,
        12219,
         7757
    };

*/
/*
//filter 2
int[] firCoef = {
         -333,
         -486,
        -1021,
        -1939,
        -3133,
        -4372,
        -5303,
        -5496,
        -4515,
        -2024,
         2093,
         7617,
        13912,
        19932,
        24352,
        25991,
        24352,
        19932,
        13912,
         7617,
         2093,
        -2024,
        -4515,
        -5496,
        -5303,
        -4372,
        -3133,
        -1939,
        -1021,
         -486,
         -333
         };
 *
 */

//filter 5
int[] firCoef = {
          374,
          478,
          540,
          537,
          451,
          267,
          -20,
         -406,
         -877,
        -1406,
        -1957,
        -2491,
        -2963,
        -3335,
        -3572,
        29428,
        -3572,
        -3335,
        -2963,
        -2491,
        -1957,
        -1406,
         -877,
         -406,
          -20,
          267,
          451,
          537,
          540,
          478,
          374
         };

int firBuf[];

//this class holds information for a channel on the board
class BoardChannel{

byte dspChip;
byte dspCore1;
byte dspCore2;

byte delayReg0, delayReg1, delayReg2, delayReg3;
byte ducerSetupReg;
byte countReg0, countReg1, countReg2;
byte bufStart0, bufStart1, bufStart2;

int numberOfGates = 0;
Gate[] gates;

int aScanSmoothing = 1;
int rejectLevel;
boolean isWallChannel=false;

}

BoardChannel bdChs[];
static int NUMBER_OF_BOARD_CHANNELS = 4;

static int FALSE = 0;
static int TRUE = 1;

//this is the memory location in the DSP where the FPGA stuff the raw A/D data
static int AD_RAW_DATA_BUFFER_ADDRESS = 0x4000;

static int RUNTIME_PACKET_SIZE = 2048;

//the hardware uses a 4 byte unsigned integer for MAX_DELAY_COUNT - Java
//doesn't do unsigned easily, so the max value is limited to the maximum
//positive value Java allows for a signed integer just to make the Java side
//easier

static int MAX_DELAY_COUNT = Integer.MAX_VALUE - 10;
static int MAX_SAMPLE_COUNT = 8 * 1024; //matches RAM sample buffer in FPGA

//Message IDs for the setState function
static byte ENABLE_SAMPLING = 0;
static byte ENABLE_DSP_RUN = 1;
static byte ENABLE_TEST_DATA = 2;
static byte ENABLE_FPGA_INTERNALS = 3;

int PEAK_DATA_BYTES_PER_GATE = 8;
int PEAK_DATA_BYTES_FOR_WALL = 24;

public static byte POSITIVE_HALF = 0;
public static byte NEGATIVE_HALF = 1;
public static byte FULL_WAVE = 2;
public static byte RF_WAVE = 3;
public static byte CHANNEL_OFF = 4;

// bits for flag1 variable in DSP's
//The TRANSMITTER_ACTIVE bit should usually not be modified by the host.
//The GATES_ENABLED, DAC_ENABLED, and ASCAN_ENABLED flags can be cleared before
//requesting a processing time calculation to use as a baseline as this results
//in the least amount of processing.

static byte TRANSMITTER_ACTIVE = 0x0001; //transmitter active flag (set by DSP)
static byte GATES_ENABLED = 0x0002;      //gates enabled flag
static byte DAC_ENABLED = 0x0004;        //DAC enabled flag
static byte ASCAN_FAST_ENABLED = 0x0008; //fast AScan enabled flag
static byte ASCAN_SLOW_ENABLED = 0x0010; //slow AScan enabled flag

//Messages for DSPs
//These should match the values in the code for those DSPs

static byte DSP_NULL_MSG_CMD = 0;
static byte DSP_GET_STATUS_CMD = 1;
static byte DSP_SET_GAIN_CMD = 2;
static byte DSP_GET_ASCAN_BLOCK_CMD = 3;
static byte DSP_GET_ASCAN_NEXT_BLOCK_CMD = 4;
static byte DSP_SET_AD_SAMPLE_SIZE_CMD = 5;
static byte DSP_SET_DELAYS	= 6;
static byte DSP_SET_ASCAN_SCALE	= 7;
static byte DSP_SET_GATE = 8;
static byte DSP_SET_GATE_FLAGS = 9;
static byte DSP_SET_DAC	= 10;
static byte DSP_SET_DAC_FLAGS = 11;
static byte DSP_SET_HIT_MISS_COUNTS = 12;
static byte DSP_GET_PEAK_DATA = 13;
static byte DSP_SET_RECTIFICATION = 14;
static byte DSP_SET_FLAGS1 = 15;
static byte DSP_CLEAR_FLAGS1 = 16;
static byte DSP_SET_GATE_SIG_PROC_THRESHOLD = 17;

static byte DSP_ACKNOWLEDGE = 127;

//Commands for UT boards
//These should match the values in the code for those boards.

static byte NO_ACTION = 0;
static byte MONITOR_CMD = 1;
static byte ZERO_ENCODERS_CMD = 2;
static byte REFRESH_CMD = 3;
static byte LOAD_FPGA_CMD = 4;
static byte SEND_DATA_CMD = 5;
static byte DATA_CMD = 6;
static byte WRITE_FPGA_CMD = 7;
static byte READ_FPGA_CMD = 8;
static byte GET_STATUS_CMD = 9;
static byte SET_HDW_GAIN_CMD = 10;
static byte WRITE_DSP_CMD = 11;
static byte WRITE_NEXT_DSP_CMD = 12;
static byte READ_DSP_CMD = 13;
static byte READ_NEXT_DSP_CMD = 14;
static byte READ_DSP_BLOCK_CMD = 15;
static byte ZERO_DSP_CMD = 16;
static byte GET_ASCAN_CMD = 17;
static byte MESSAGE_DSP_CMD = 18;
static byte GET_PEAK_DATA_CMD = 19;     //differs from DSP_GET_PEAK_DATA
static byte GET_PEAK_DATA4_CMD = 20;	// four channel version
static byte GET_DSP_RAM_BLOCK_CHECKSUM = 21;
static byte LOAD_FIRMWARE_CMD = 22;     //loads new Rabbit software

static byte ERROR = 125;
static byte DEBUG_CMD = 126;
static byte EXIT_CMD = 127;

//Status Codes for UT boards
//These should match the values in the code for those boards.

static byte NO_STATUS = 0;
static byte FPGA_INITB_ERROR = 1;
static byte FPGA_DONE_ERROR = 2;
static byte FPGA_CONFIG_CRC_ERROR = 3;
static byte FPGA_CONFIG_GOOD = 4;

//FPGA Register Addresses for the UT Board

static byte MASTER_CONTROL_REG = 0x00;
static byte RESET_REG = 0x01;

static byte REP_RATE_0_REG = 0x03;
static byte REP_RATE_1_REG = 0x04;
static byte REP_RATE_2_REG = 0x05;
static byte REP_RATE_3_REG = 0x06;

static byte CHASSIS_SLOT_ADDRESS = 0x08;

static byte CH1_SAMPLE_DELAY_0 = 0x1b;
static byte CH1_SAMPLE_DELAY_1 = 0x1c;
static byte CH1_SAMPLE_DELAY_2 = 0x1d;
static byte CH1_SAMPLE_DELAY_3 = 0x1e;

static byte CH2_SAMPLE_DELAY_0 = 0x1f;
static byte CH2_SAMPLE_DELAY_1 = 0x20;
static byte CH2_SAMPLE_DELAY_2 = 0x21;
static byte CH2_SAMPLE_DELAY_3 = 0x22;

static byte CH3_SAMPLE_DELAY_0 = 0x23;
static byte CH3_SAMPLE_DELAY_1 = 0x24;
static byte CH3_SAMPLE_DELAY_2 = 0x25;
static byte CH3_SAMPLE_DELAY_3 = 0x26;

static byte CH4_SAMPLE_DELAY_0 = 0x27;
static byte CH4_SAMPLE_DELAY_1 = 0x28;
static byte CH4_SAMPLE_DELAY_2 = 0x29;
static byte CH4_SAMPLE_DELAY_3 = 0x2a;

static byte CH1_SAMPLE_COUNT_0 = 0x2b;
static byte CH1_SAMPLE_COUNT_1 = 0x2c;
static byte CH1_SAMPLE_COUNT_2 = 0x2d;

static byte CH2_SAMPLE_COUNT_0 = 0x2e;
static byte CH2_SAMPLE_COUNT_1 = 0x2f;
static byte CH2_SAMPLE_COUNT_2 = 0x30;

static byte CH3_SAMPLE_COUNT_0 = 0x31;
static byte CH3_SAMPLE_COUNT_1 = 0x32;
static byte CH3_SAMPLE_COUNT_2 = 0x33;

static byte CH4_SAMPLE_COUNT_0 = 0x34;
static byte CH4_SAMPLE_COUNT_1 = 0x35;
static byte CH4_SAMPLE_COUNT_2 = 0x36;

static byte CH1_SAMPLE_BUFSTART_0 = 0x37;
static byte CH1_SAMPLE_BUFSTART_1 = 0x38;
static byte CH1_SAMPLE_BUFSTART_2 = 0x39;

static byte CH2_SAMPLE_BUFSTART_0 = 0x3a;
static byte CH2_SAMPLE_BUFSTART_1 = 0x3b;
static byte CH2_SAMPLE_BUFSTART_2 = 0x3c;

static byte CH3_SAMPLE_BUFSTART_0 = 0x3d;
static byte CH3_SAMPLE_BUFSTART_1 = 0x3e;
static byte CH3_SAMPLE_BUFSTART_2 = 0x3f;

static byte CH4_SAMPLE_BUFSTART_0 = 0x40;
static byte CH4_SAMPLE_BUFSTART_1 = 0x41;
static byte CH4_SAMPLE_BUFSTART_2 = 0x42;

static byte DUCER_SETUP_1_REG = 0x43;
static byte DUCER_SETUP_2_REG = 0x44;
static byte DUCER_SETUP_3_REG = 0x45;
static byte DUCER_SETUP_4_REG = 0x46;

static byte NUMBER_BANKS_REG = 0x47;

static byte TRIG_WIDTH_0_REG = 0x48;
static byte TRIG_WIDTH_1_REG = 0x49;
static byte TRIG_WIDTH_2_REG = 0x4a;
static byte TRIG_WIDTH_3_REG = 0x4b;

static byte PULSE_DELAY_0_REG = 0x50;
static byte PULSE_DELAY_1_REG = 0x51;

static byte SYNC_WIDTH_0_REG = 0x6e;
static byte SYNC_WIDTH_1_REG = 0x6f;

static byte PROGRAMMABLE_GAIN_CH1_CH2 = 0x4c;
static byte PROGRAMMABLE_GAIN_CH3_CH4 = 0x4d;

static byte STATUS1_REG = 0x19; //HPI1 ready, Pulse Sync, Pulse Sync Reset
static byte STATUS2_REG = 0x1a; //HPI2 ready, Track Sync, Track Sync Reset

//FPGA Master Control Register Bit Masks for the UT Board

static byte SETUP_RUN_MODE = 0x01;
static byte SYNC_SOURCE = 0x02;
static byte SIM_DATA = 0x04;

//FPGA Reset Register Bit Masks for the UT Board

static byte FPGA_INTERNALS_RST = 0x01;
static byte GLOBAL_DSP_RST = 0x02;
static byte DSPA_RST = 0x04;
static byte DSPB_RST = 0x08;
static byte DSPC_RST = 0x10;
static byte DSPD_RST = 0x20;

//bit masks for gate peak data flag

static byte HIT_COUNT_MET = 0x0001;
static byte MISS_COUNT_MET = 0x0002;

//shadow registers for FPGA registers which do not already have an associated
//variable

public byte masterControlShadow;
byte resetShadow;
byte progGain12Shadow;
byte progGain34Shadow;

// UT Board status flag bit masks

static byte FPGA_LOADED_FLAG = 0x01;

//number of loops to wait for response before timeout
static int FPGA_LOAD_TIMEOUT = 999999;

boolean reSynced;

//-----------------------------------------------------------------------------
// UTBoard::UTBoard (constructor)
//
// The parameter configFilename is used to load configuration data.  Each
// UTBoard object MUST create its own iniFile object because they are accessed
// in the UTBoard's thread and there may be more than one such thead operaing
// simultaneously.  This slows down startup but cannot be avoided as the
// iniFile object is not guaranteed to be thread safe.
//

public UTBoard(String pConfigFilename, String pBoardName, int pBoardIndex,
     boolean pSimulate, JTextArea pLog, HardwareVars pHdwVs,
     String pJobFileFormat, String pMainFileFormat)
{

super(pLog);

configFilename = pConfigFilename;
hdwVs = pHdwVs;
jobFileFormat = pJobFileFormat; mainFileFormat = pMainFileFormat;

//if the ini file cannot be opened and loaded, continue on - values will default
try {configFile = new IniFile(configFilename, jobFileFormat);}
    catch(IOException e){}

boardName = pBoardName;
boardIndex = pBoardIndex;
simulate = pSimulate;

//FIR filter buffer -- same length as number of filter taps
firBuf = new int[firCoef.length];

//aScan holds an aScan data set for transfer to the display object
aScan = new AScan(ASCAN_BUFFER_SIZE);
//aScanBuffer holds data while it is being processed
aScanBuffer = new AScan(ASCAN_BUFFER_SIZE);

//aScanFIFO holds multiple data sets which can then be averaged to create the
//data for aScan - this allows for smoothing
aScanFIFO = new AScan[ASCAN_FIFO_SIZE];
for (int i = 0; i < ASCAN_FIFO_SIZE; i++)
    aScanFIFO[i] = new AScan(ASCAN_BUFFER_SIZE);

readDSPResult = new byte[512];

getDSPRamChecksumResult = new byte [2];

//setup information for each channel on the board
setupBoardChannels();

//read the configuration file and create/setup the charting/control elements
configure(configFile);

}//end of UTBoard::UTBoard (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setupBoardChannels
//
// Creates and sets up an array to hold channel specific values such as
// FPGA register addresses, DSP chip and core numbers, etc.
//

private void setupBoardChannels() {

bdChs = new BoardChannel[NUMBER_OF_BOARD_CHANNELS];

for (int i=0; i<4; i++) bdChs[i] = new BoardChannel();

//Each channel is handled by two DSP cores.
//select the proper dsp chip and cores for each channel as follows
// channel 0 : chip 1, cores A & B
// channel 1 : chip 1, cores C & D
// channel 2 : chip 2, cores A & B
// channel 3 : chip 2, cores C & D

bdChs[0].dspChip = 1; bdChs[0].dspCore1 = 1; bdChs[0].dspCore2 = 2;
bdChs[1].dspChip = 1; bdChs[1].dspCore1 = 3; bdChs[1].dspCore2 = 4;
bdChs[2].dspChip = 2; bdChs[2].dspCore1 = 1; bdChs[2].dspCore2 = 2;
bdChs[3].dspChip = 2; bdChs[3].dspCore1 = 3; bdChs[3].dspCore2 = 4;

//assign the appropriate FPGA register addresses to each channel

bdChs[0].delayReg0 = CH1_SAMPLE_DELAY_0;
bdChs[0].delayReg1 = CH1_SAMPLE_DELAY_1;
bdChs[0].delayReg2 = CH1_SAMPLE_DELAY_2;
bdChs[0].delayReg3 = CH1_SAMPLE_DELAY_3;
bdChs[0].countReg0 = CH1_SAMPLE_COUNT_0;
bdChs[0].countReg1 = CH1_SAMPLE_COUNT_1;
bdChs[0].countReg2 = CH1_SAMPLE_COUNT_2;
bdChs[0].bufStart0 = CH1_SAMPLE_BUFSTART_0;
bdChs[0].bufStart1 = CH1_SAMPLE_BUFSTART_1;
bdChs[0].bufStart2 = CH1_SAMPLE_BUFSTART_2;

bdChs[1].delayReg0 = CH2_SAMPLE_DELAY_0;
bdChs[1].delayReg1 = CH2_SAMPLE_DELAY_1;
bdChs[1].delayReg2 = CH2_SAMPLE_DELAY_2;
bdChs[1].delayReg3 = CH2_SAMPLE_DELAY_3;
bdChs[1].countReg0 = CH2_SAMPLE_COUNT_0;
bdChs[1].countReg1 = CH2_SAMPLE_COUNT_1;
bdChs[1].countReg2 = CH2_SAMPLE_COUNT_2;
bdChs[1].bufStart0 = CH2_SAMPLE_BUFSTART_0;
bdChs[1].bufStart1 = CH2_SAMPLE_BUFSTART_1;
bdChs[1].bufStart2 = CH2_SAMPLE_BUFSTART_2;

bdChs[2].delayReg0 = CH3_SAMPLE_DELAY_0;
bdChs[2].delayReg1 = CH3_SAMPLE_DELAY_1;
bdChs[2].delayReg2 = CH3_SAMPLE_DELAY_2;
bdChs[2].delayReg3 = CH3_SAMPLE_DELAY_3;
bdChs[2].countReg0 = CH3_SAMPLE_COUNT_0;
bdChs[2].countReg1 = CH3_SAMPLE_COUNT_1;
bdChs[2].countReg2 = CH3_SAMPLE_COUNT_2;
bdChs[2].bufStart0 = CH3_SAMPLE_BUFSTART_0;
bdChs[2].bufStart1 = CH3_SAMPLE_BUFSTART_1;
bdChs[2].bufStart2 = CH3_SAMPLE_BUFSTART_2;

bdChs[3].delayReg0 = CH4_SAMPLE_DELAY_0;
bdChs[3].delayReg1 = CH4_SAMPLE_DELAY_1;
bdChs[3].delayReg2 = CH4_SAMPLE_DELAY_2;
bdChs[3].delayReg3 = CH4_SAMPLE_DELAY_3;
bdChs[3].countReg0 = CH4_SAMPLE_COUNT_0;
bdChs[3].countReg1 = CH4_SAMPLE_COUNT_1;
bdChs[3].countReg2 = CH4_SAMPLE_COUNT_2;
bdChs[3].bufStart0 = CH4_SAMPLE_BUFSTART_0;
bdChs[3].bufStart1 = CH4_SAMPLE_BUFSTART_1;
bdChs[3].bufStart2 = CH4_SAMPLE_BUFSTART_2;

bdChs[0].ducerSetupReg = DUCER_SETUP_1_REG;
bdChs[1].ducerSetupReg = DUCER_SETUP_2_REG;
bdChs[2].ducerSetupReg = DUCER_SETUP_3_REG;
bdChs[3].ducerSetupReg = DUCER_SETUP_4_REG;

}//end of UTBoard::setupBoardChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::run
//
// This thread loads the board with FPGA and DSP code.  Using a thread allows
// multiple boards to be loaded simultaneously.
//

@Override
public void run() {

//link with all the remotes
connect();

//Since the sockets and associated streams were created by this
//thread, it cannot be closed without disrupting the connections. If
//other threads try to read from the socket after the thread which
//created the socket finishes, an exception will be thrown.  This
//thread just waits() after performing the connect function.  The
//alternative is to close the socket and allow another thread to
//reopen it, but this results in a lot of overhead.

waitForever();

}//end of UTBoard::run
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::waitForever
//
// Puts the thread in wait mode forever.
//

public synchronized void waitForever()
{

while (true){
    try{wait();}
    catch (InterruptedException e) { }
    }

}//end of UTBoard::waitForever
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::connect
//
// Opens a TCP/IP connection with the board.
//

public synchronized void connect()
{

//see notes above regarding IP Addresses

try {

    //displays message on bottom panel of IDE
    logger.logMessage("Connecting to UT board " + ipAddrS + "...\n");

    if (!simulate) socket = new Socket(ipAddr, 23);
    else socket = new UTSimulator(ipAddr, 23, mainFileFormat);

    //set amount of time in milliseconds that a read from the socket will
    //wait for data - this prevents program lock up when no data is ready
    socket.setSoTimeout(250);

    // the buffer size is not changed here as the default ends up being
    // large enough - use this code if it needs to be increased
    //socket.setReceiveBufferSize(10240 or as needed);

    //allow verification that the hinted size is actually used
    logger.logMessage("UT " + ipAddrS + " receive buffer size: " +
                                    socket.getReceiveBufferSize() + "...\n");

    //allow verification that the hinted size is actually used
    logger.logMessage("UT " + ipAddrS + " send buffer size: " +
                                    socket.getSendBufferSize() + "...\n");

    out = new PrintWriter(socket.getOutputStream(), true);

    in = new BufferedReader(new InputStreamReader(
                                        socket.getInputStream()));

    byteOut = new DataOutputStream(socket.getOutputStream());
    byteIn = new DataInputStream(socket.getInputStream());

    }
catch (UnknownHostException e) {
    logger.logMessage("Unknown host: UT " + ipAddrS + ".\n");
    return;
    }
catch (IOException e) {
    logger.logMessage("Couldn't get I/O for UT " + ipAddrS + "\n");
    logger.logMessage("--" + e.getMessage() + "--\n");
    return;
    }

try {
    //display the greeting message sent by the remote
    logger.logMessage("UT " + ipAddrS + " says " + in.readLine() + "\n");
    }
catch(IOException e){}

loadFPGA(); //send configuration file for the board's FPGA

initFPGA(); //setup the registers in the UT board FPGA

//ask the board for its chassis and board address switch settngs
getChassisSlotAddress();

//check for an address override entry and use that if it exists
getChassisSlotAddressOverride();

//NOTE: now that the chassis and slot addresses are known, display messages
// using those to identify the board instead of the IP address so it is easier
// to discern which board is which.

chassisSlotAddr = chassisAddr + ":" + slotAddr;

loadDSPCode(1, 1); //send the code to DSP 1, Core A (this also loads Core B)
verifyDSPCode(1, 1); //verify the checksum of the code in the DSP
loadDSPCode(1, 3); //send the code to DSP 1, Core C (this also loads Core D)
verifyDSPCode(1, 3); //verify the checksum of the code in the DSP
loadDSPCode(2, 1); //send the code to DSP 2, Core A (this also loads Core B)
verifyDSPCode(2, 1); //verify the checksum of the code in the DSP
loadDSPCode(2, 3); //send the code to DSP 2, Core C (this also loads Core D)
verifyDSPCode(2, 3); //verify the checksum of the code in the DSP

//release FPGA internals from reset (low = no reset)
//release DSP Global reset so HPI bus can be used (high = no reset)
//release DSPs A,B,C,D resets (low = reset)
resetShadow = writeFPGAReg(RESET_REG, (byte)0x3e);

//sleep for a bit to allow DSPs to start up
waitSleep(1000);

/* debug mks
logDSPStatus(1, 1, true); logDSPStatus(1, 2, true);
logDSPStatus(1, 3, true); logDSPStatus(1, 4, true);

logDSPStatus(2, 1, true); logDSPStatus(2, 2, true);
logDSPStatus(2, 3, true); logDSPStatus(2, 4, true);
*/

//enable sampling - FPGA has control of the HPI bus to transfer A/D data
setState(0, 1);

//flag that board setup has been completed - whether it failed or not
setupComplete = true;

//flag that setup was successful and board is ready for use
ready = true;

logger.logMessage(
              "UT " + chassisSlotAddr + " ~ " + ipAddrS + " is ready." + "\n");

notifyAll(); //wake up all threads that are waiting for this to complete

/*
//wip mks remove this - can be used in a monitor function?
// monitors the Pulse Sync, Pulse Sync Reset, Track Sync, Track Sync Reset

byte status1, status2;
byte status1p = 0, status2p = 0;


while (true){

    //read the status inputs
    status1 = getRemoteAddressedData(READ_FPGA_CMD, STATUS1_REG);
    status2 = getRemoteAddressedData(READ_FPGA_CMD, STATUS2_REG);

    if (status1 != status1p || status2 != status2p)
        logger.logMessage("Status 1: " + status1 + " Status 2: " + status2 + "\n");

    status1p = status1; status2p = status2;

    }

//wip mks end remove this
*/

}//end of UTBoard::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::verifyAllDSPCode2
//
// Verifies that the code in each DSP core matches the file.  Used to check for
// transmission or corruption errors.
//
// This function checks byte by byte and is VERY slow.
//

public void verifyAllDSPCode2()
{

//disable sampling on the UT boards so the DSP rams can be accessed
setState(0, 0);

//read the code back from each DSP and compare it to the file

verifyDSPCode2(1, 1);
verifyDSPCode2(1, 3);
verifyDSPCode2(2, 1);
verifyDSPCode2(2, 3);

//re-enable sampling on the UT boards so the DSP rams can be accessed
setState(0, 1);

}//end of UTBoard::verifyAllDSPCode2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::loadFPGA
//
// Transmits the FPGA code to the specified UT board for loading into the
// chip.  Note that the MainThread object in Main does not start calling
// processDataPackets until after the FPGA code is loaded so there are no
// conflicts created by this function reading from the socket.
//
// This function uses TCP/IP and transmits to the single board handled by
// this UTBoard object.  If multiple boards are being loaded simultaneously,
// the load time increases significantly.  The Capulin1 class has a loader
// which uses UDP to simultaneously broadcast the FPGA code to multiple boards
// at once -- this is a much faster method.  See Capulin1::loadFPGAViaUPD.
//
// Before sending, the status byte of the remote is retrieved.  If the FPGA
// has already been loaded, then this function exits immediately.  This saves
// time when the host PC software is restarted but the remotes are not.
//
// This function uses the "Binary Configuration File" (*.bin) produced by
// the Xilinx ISE.
//
// The config file is 5,214,784 bits (651848 bytes) for the Xilinx
// Xc3s1500-4fg456C. The file is transmitted to the remotes in 637 blocks of
// 1024 bytes each, with the last block being a partial one.
//
// The file should be sent by the remote to the FPGA starting with the most
// significant bit of each byte transmitted first.  The remote should send
// the command SEND_DATA when it is ready for each block, including the first
// one.
//
// The remote should send FPGA_INITB_ERROR if the INIT_B line is not high
// after PROG_B is taken high.
// The remote should send FPGA_DONE_ERROR if the DONE line is not low
// after PROG_B is taken high or does not go high after all is loaded.
// The remote should send FPGA_CONFIG_CRC_ERROR if the INIT_B line goes low
// while or after code is loaded.
// The remote should send FPGA_CONFIG_GOOD if DONE line goes high and the
// INIT_B line stays high after code has been loaded.
//

public void loadFPGA()
{

// don't attempt to load the FPGA if no contact made with remote
if (byteOut == null) return;

// check to see if the FPGA has already been loaded
if ((getRemoteData(GET_STATUS_CMD, true) & FPGA_LOADED_FLAG) != 0) {
    logger.logMessage("UT " + ipAddrS + " FPGA already loaded..." + "\n");

    return;
    }

fpgaLoaded = true;

int CODE_BUFFER_SIZE = 1025; //transfer command word and 1024 data bytes
byte[] codeBuffer;
codeBuffer = new byte[CODE_BUFFER_SIZE];

int bufPtr;

boolean fileDone = false;

FileInputStream inFile = null;

try {

    sendByte(LOAD_FPGA_CMD); //send command to initiate loading

    logger.logMessage("UT " + ipAddrS + " loading FPGA..." + "\n");

    timeOutRead = 0;
    inFile = new FileInputStream("fpga\\" + fpgaCodeFilename);
    int c, inCount;

    while(timeOutRead < FPGA_LOAD_TIMEOUT){


        inBuffer[0] = NO_ACTION; //clear request byte from host
        inBuffer[1] = NO_STATUS; //clear status byte from host

        //check for a request from the remote if connected
        if (byteIn != null){
            inCount = byteIn.available();
            //0 = buffer offset, 2 = number of bytes to read
            if (inCount >= 2) byteIn.read(inBuffer, 0, 2);
             }

        //trap error and finished status messages, second byte in buffer

        if (inBuffer[1] == FPGA_INITB_ERROR){
            logger.logMessage(
                      "UT " + ipAddrS + " error loading FPGA - INIT_B" + "\n");
            return;
            }

        if (inBuffer[1] == FPGA_DONE_ERROR){
            logger.logMessage(
                      "UT " + ipAddrS + " error loading FPGA - DONE" + "\n");
            return;
            }

        if (inBuffer[1] == FPGA_CONFIG_CRC_ERROR){
            logger.logMessage(
                        "UT " + ipAddrS + " error loading FPGA - CRC" + "\n");
            return;
            }

        if (inBuffer[1] == FPGA_CONFIG_GOOD){
            logger.logMessage("UT " + ipAddrS + " FPGA Loaded." + "\n");
            return;
            }

        //send data packet when requested by remote
        if (inBuffer[0] == SEND_DATA_CMD && !fileDone){

            bufPtr = 0; c = 0;
            codeBuffer[bufPtr++] = DATA_CMD; // command byte = data packet

            //be sure to check bufPtr on left side or a byte will get read
            //and ignored every time bufPtr test fails
            while (bufPtr < CODE_BUFFER_SIZE && (c = inFile.read()) != -1 ) {

                //stuff the bytes into the buffer after the command byte
                codeBuffer[bufPtr++] = (byte)c;

                //reset timer in this loop so it only gets reset when
                //a request has been received AND not at end of file
                timeOutRead = 0;

                }

            if (c == -1) fileDone = true; //send no more packets

            //send packet to remote
            byteOut.write(codeBuffer, 0 /*offset*/, CODE_BUFFER_SIZE);

            }//if (inBuffer[0] == SEND_DATA)

        //count loops - will exit when max reached
        //this is reset whenever a packet request is received and the end of
        //file not reached - when end of file reached, loop will wait until
        //timeout reached again before exiting in order to catch success/error
        //messages from the remote

        timeOutRead++;

        }// while(timeOutGet <...

    //remote has not responded if this part reached
    logger.logMessage(
                "UT " + ipAddrS + " error loading FPGA - contact lost." + "\n");

    }//try
catch(IOException e){}
finally {
    if (inFile != null) try {inFile.close();}catch(IOException e){}
    }//finally

}//end of UTBoard::loadFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::writeFPGAReg
//
// Writes pByte to FPGA register specified by pAddress
//
// Returns pByte so a shadow register can be set to match.
//

public byte writeFPGAReg(byte pAddress, byte pByte)
{

sendBytes3(WRITE_FPGA_CMD, pAddress, pByte);

return pByte;

}//end of UTBoard::writeFPGAReg
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::initFPGA
//
// Initializes the registers in the UT board FPGA
//

void initFPGA()
{

//place FPGA internals in reset (active high), DSPs in reset (active low)
resetShadow = writeFPGAReg(RESET_REG, (byte)0x01);

//place FPGA in setup mode - Rabbit controls HPI
masterControlShadow = writeFPGAReg(MASTER_CONTROL_REG, (byte)0x00);

//tell the fpga where to store data samples in the DSPs
sendSampleBufferStart(0, UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);
sendSampleBufferStart(1, UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);
sendSampleBufferStart(2, UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);
sendSampleBufferStart(3, UTBoard.AD_RAW_DATA_BUFFER_ADDRESS);

//release FPGA internals from reset (low = no reset)
//release DSP Global reset so HPI bus can be used (high = no reset)
//DSPs A,B,C,D still in individual reset (low = reset)
resetShadow = writeFPGAReg(RESET_REG, (byte)0x02);

}//end of UTBoard::initFPGA
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard:initialize
//
// Sets up various settings on the board, most of which can only be loaded
// from the config file after the boards FPGA has been loaded and the chassis
// and slot addresses can be read.
//

public void initialize()
{

// load values from the config file which can only be loaded after the board's
// chassis and slot addresses are known

configureExtended(configFile);

//destroy the configFile object to release resources
configFile = null;

//place the FPGA internals into reset to prevent them from starting
//in an unknown condition after changing the sampling registers

resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x01));

//turn off all tranducers
sendTransducer(0, (byte)0, (byte)1, (byte)0);
sendTransducer(1, (byte)0, (byte)1, (byte)1);
sendTransducer(2, (byte)0, (byte)1, (byte)2);
sendTransducer(3, (byte)0, (byte)1, (byte)3);

sendRepRate(repRate);

sendTriggerWidth(triggerWidth);

sendSyncWidth(syncWidth);
sendPulseDelay(pulseDelay);

//number of banks to fire (use desired value - 1, 0 = 1 bank)
writeFPGAReg(NUMBER_BANKS_REG, (byte)numberOfBanks);

//place FPGA in run mode using A/D test data sequence
//use 0x07 for board to be the source of the pulser sync
//use 0x05 for board to be a receiver of the pulser sync

//set the board up as the pulse sync source if specified
if (syncSource) masterControlShadow |= SYNC_SOURCE;

//set the board up to use real data instead of simulation data
masterControlShadow &= (~SIM_DATA);

//apply the settings to the FPGA register
writeFPGAReg(MASTER_CONTROL_REG, (byte)masterControlShadow);

//release the FPGA internals from reset
resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x01)));

}//end of UTBoard::initialize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getChassisSlotAddress
//
// Retrieves the board's chassis and slot address settings.  This function
// can only be called after the board's FPGA has been loaded because the
// switches are read through the FPGA.
//
// The switches are located on the motherboard.
//

void getChassisSlotAddress()
{

//read the address from FPGA register connected to the switches
byte address = getRemoteAddressedData(READ_FPGA_CMD, CHASSIS_SLOT_ADDRESS);

//the address from the switches is inverted with the chassis address in the
//upper nibble and the board address in the lower

chassisAddr =  (~address>>4 & 0xf);
slotAddr = ~address & 0xf;

logger.logMessage("UT " + ipAddrS + " chassis & slot address: "
                                        + chassisAddr + "-" + slotAddr + "\n");

}//end of UTBoard::getChassisSlotAddress
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getChassisSlotAddressOverride
//
// Checks the "Board Slot Overrides.ini" file to see if there is a chassis
// and slot address override for the board with this IP.
//
// The override is used to force a particular board to use a specific chassis
// and slot address.
//
// Each UTBoard object must open its own iniFile object because they are created
// simultaneously in different threads.  The iniFile object is not guaranteed
// to be thread safe.
//

void getChassisSlotAddressOverride()
{

//use integers with zeroing of upper bits to represent the bytes as unsigned
//values - without zeroing, sign extension during transfer to the int makes
//values above 127 negative

int byte2 = (int)(ipAddr.getAddress()[2] & 0xff);
int byte3 = (int)(ipAddr.getAddress()[3] & 0xff);

IniFile configFileL;

//if the ini file cannot be opened and loaded, exit without action
try {configFileL = new IniFile("Board Slot Overrides.ini", mainFileFormat);}
    catch(IOException e){
    return;
    }

String section = byte2 + "." + byte3;

byte chassisAddrL = (byte)configFileL.readInt(section, "Chassis", -1);

byte slotAddrL = (byte)configFileL.readInt(section, "Slot", -1);

//if a chassis and board address were found for this board's IP address,
//set the board's addresses to match

if (chassisAddrL != -1) chassisAddr = chassisAddrL;
if (slotAddrL != -1) slotAddr = slotAddrL;

if (chassisAddrL != -1 || slotAddrL != -1)
    logger.logMessage("UT " + ipAddrS + " chassis & slot override: "
                                        + chassisAddr + "-" + slotAddr + "\n");

}//end of UTBoard::getChassisSlotAddressOverride
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendRepRate
//
// Sends the Pulser Rep Rate to the UT board.
//

void sendRepRate(int pValue)
{

//write the bytes of the integer to the appropriate registers
writeFPGAReg(REP_RATE_0_REG, (byte) (pValue & 0xff));
writeFPGAReg(REP_RATE_1_REG, (byte) ((pValue >> 8) & 0xff));
writeFPGAReg(REP_RATE_2_REG, (byte) ((pValue >> 16) & 0xff));
writeFPGAReg(REP_RATE_3_REG, (byte) ((pValue >> 24) & 0xff));

}//end of UTBoard::sendRepRate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getRepRate
//
// Returns the UT board's pulser rep rate.
//

public int getRepRate()
{

return (repRateInHertz);

}//end of UTBoard::getRepRate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendTransducer
//
// Sends the transducer's pulse on/off, firing bank, and specifies which
// channel to fire for the transducer.  Note that the transucer can be on
// one channel while another is fired for pitch/catch configurations.
//
// pBank is the pulser bank to which the transducer is to be assigned.
// pPulsedChannel is the pulser fired for the specified channel.
// pOnOff is 0 if the channel is to be pulsed and 1 if not.
//

void sendTransducer(int pChannel, byte pOnOff, byte pPulseBank,
                                                           byte pPulseChannel)
{

int setup = pOnOff + (pPulseBank << 1) + (pPulseChannel << 4);

//    *  Bit 0 : 0 = transducer is inactive, 1 = transducer is active
//    * Bits 1:3 : time slot for the transducer see Number of Time Slots
//    * Bits 4:7 :specifies channel of pulser to be fired for this transducer

writeFPGAReg(bdChs[pChannel].ducerSetupReg, (byte)setup);

}//end of UTBoard::sendTransducer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendTriggerWidth
//
// Sends the Pulser Trigger Width to the UT board.
//

void sendTriggerWidth(int pValue)
{

//write the bytes of the integer to the appropriate registers
writeFPGAReg(TRIG_WIDTH_0_REG, (byte) (pValue & 0xff));
writeFPGAReg(TRIG_WIDTH_1_REG, (byte) ((pValue >> 8) & 0xff));
writeFPGAReg(TRIG_WIDTH_2_REG, (byte) ((pValue >> 16) & 0xff));
writeFPGAReg(TRIG_WIDTH_3_REG, (byte) ((pValue >> 24) & 0xff));

}//end of UTBoard::sendTriggerWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendSyncWidth
//
// Sends the Sync Width to the UT board.  This is used by the board which is
// designated as the sync source.  The value is adjusted to make sure it is
// wide enough to trigger the optoisolators.
//

void sendSyncWidth(int pValue)
{

//write the bytes of the integer to the appropriate registers
writeFPGAReg(SYNC_WIDTH_0_REG, (byte) (pValue & 0xff));
writeFPGAReg(SYNC_WIDTH_1_REG, (byte) ((pValue >> 8) & 0xff));

}//end of UTBoard::sendSyncWidth
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendPulseDelay
//
// Sends the Pulse Delay to the UT board.  All boards use this to delay the
// actual firing of the Initial Pulse after the sync source has been sent (in
// the case of the source board) or received.  This allows all boards to be
// adjusted to fire precisely at the same time regardless of the slew of their
// optoisolators.
//
// Normally, the source board will have the largest delay so that its pulse
// triggers at the same time as the sync receiving boards which have an inherent
// delay through their sync optoisolators.
//

void sendPulseDelay(int pValue)
{

//write the bytes of the integer to the appropriate registers
writeFPGAReg(PULSE_DELAY_0_REG, (byte) (pValue & 0xff));
writeFPGAReg(PULSE_DELAY_1_REG, (byte) ((pValue >> 8) & 0xff));

}//end of UTBoard::sendPulseDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendHardwareGain
//
// Sennds the programmable hardware gain for channel pChannel.  The gain is
// controlled by two amplifiers in series.  The gain for the first is set to
// pGain1 and the second to pGain2.
//
// Each channel has two amplifiers whose serial data lines are connected in
// series.  The data is shifted through the first chip to the second chip.  To
// program either chip, both must be programmed.
//
// The format for the programming byte is:
// MSB  C1   - bandwidth compensation
//      C0   - bandwidth compensation
//      Zero - forces the amplifier input to zero
//      PD   - powers down the amplifier
//      G3   - gain
//      G2   - gain
//      G1   - gain
// LSB  G0   - gain
//
// For gain, 0000 is gain of 1 while 1111 is gain of 16.
// Lower gain settings must use bandwidth compensation to avoid instability.
// The function automatically selects the correct compensation per the data
// sheet recommendations.
//
// The calling function should set pGainx to 1 for a gain of 1 and to 16 for
// a gain of 16.
//

void sendHardwareGain(int pChannel, int pGain1, int pGain2)
{

byte gain1 = 0, gain2 = 0;

// gain goes in lower nibble
gain1 = (byte)((pGain1-1) & 0x0f); gain2 = (byte)((pGain2-1) & 0x0f);

//leave bits 4 & 5 as zero - Zero and Power Down functions inactive

//determine the appropriate bandwidth compensation value for each gain per
//the data sheet recommendations

// gain 16 (value 15), use comp of 00b, 01b, 10b, or 11b
// gain 11-15 (value 10-14), use comp of 00b, 01b, or 10b
// gain 6-10 (value 5-9), use comp of 00b or 01b
// gain 1-5 (values 0-4), use max comp of 00b
// in each case, the highest comp is the lowest value (00 is highest comp)
//   so choosing the highest value for each case gives the lowest comp and
//   therefore the highest possible frequency response for that range

if (gain1 == 15) gain1 |= 0xc0;
else
if (gain1 >= 10) gain1 |= 0x80;
else
if (gain1 >= 5)  gain1 |= 0x40;
// gains less than 6 (value of 5) have 00b compensation

if (gain2 == 15) gain2 |= 0xc0;
else
if (gain2 >= 10) gain2 |= 0x80;
else
if (gain2 >= 5)  gain2 |= 0x40;
// gains less than 6 (value of 5) have 00b compensation

sendBytes4((byte)SET_HDW_GAIN_CMD, (byte)pChannel, gain1, gain2);

}//end of UTBoard::sendHardwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setAScanSmoothing
//
// Sets the amount of smoothing to be applied to the AScan data.  A larger
// number results in more smoothing (averaging).  The smoothing number
// specifies how many samples to average.
//

void setAScanSmoothing(int pChannel, int pAScanSmoothing)
{

if (pAScanSmoothing < 1) pAScanSmoothing = 1;
if (pAScanSmoothing > ASCAN_FIFO_SIZE) pAScanSmoothing = ASCAN_FIFO_SIZE;

bdChs[pChannel].aScanSmoothing = pAScanSmoothing;

}//end of UTBoard::setAScanSmoothing
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setRejectLevel
//
// Sets the reject level.  Any signal below this level will be squeezed down
// to 10% of chart/scope height.
//
// This is done in the Java code and nothing is sent to the remotes.
//

void setRejectLevel(int pChannel, int pRejectLevel)
{

bdChs[pChannel].rejectLevel =
        (int)((pRejectLevel * ASCAN_MAX_HEIGHT) / 100 / ASCAN_SCALE);

}//end of UTBoard::setRejectLevel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDCOffset
//
// Sends the DC offset for the signal.
//

void sendDCOffset(int pChannel, int pDCOffset)
{

//wip mks - add code to send DC offset to the FPGA or the DSP

}//end of UTBoard::sendDCOffset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::writeDSPRam
//
// Writes pValue to the RAM at pAddress on the specified DSP chip, DSP core,
// shared or local memory, and page.
//
// If pRAMType is 0, the value will be written to local data memory.  If it is
// 1, the value will be written to shared program memory.
//

void writeDSPRam(int pDSPChip, int pDSPCore, int pRAMType,
                   int pPage, int pAddress, int pValue)
{

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)
// byte3 = address byte 2 (the page)
// byte4 = address byte 1
// byte5 = address byte 0
// byte6 = high byte of value to be written
// byte7 = low byte of value to be written


//if shared memory is selected, set bit 20 of the HPIA register - this is
//bit 4 of the upper address byte 2

sendBytes8(WRITE_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
            (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
            (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff),
            (byte)((pValue >> 8) & 0xff), (byte)(pValue & 0xff));

}//end of UTBoard::writeDSPRam
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::writeNextDSPRam
//
// Writes the next word of the contents of the RAM on the specified DSP chip
// and DSP core.  The function modifyRAMDSP should already have been called to
// write the first word in the block and set up the address.  This function
// is then used to write subsequent words, the address will be incremented
// after each word by the remote device.
//

void writeNextDSPRam(int pDSPChip, int pDSPCore, int pValue)
{

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)
// byte3 = high byte of value to be written
// byte4 = low byte of value to be written

sendBytes5(WRITE_NEXT_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
            (byte)((pValue >> 8) & 0xff), (byte)(pValue & 0xff));

}//end of UTBoard::writeNextDSPRam
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::fillRAM
//
// Fills a block of memory with the specified address, size, and value.
//

public void fillRAM(int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pBlockSize, int pValue)
{

int i = 0;

while (i < pBlockSize){

    //modifyRAMDSP sets up the address and modifies the first word
    //subsequent words are modified with modifyNextRAMDSP
    if (i++ == 0)
        writeDSPRam(pDSPChip, pDSPCore, pRAMType, pPage, pAddress, pValue);
    else
        writeNextDSPRam(pDSPChip, pDSPCore, pValue);

    }//while (i < pBlockSize)

}//end of Capulin1::fillRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::readRAM
//
// Fills array with the contents of the RAM on the specified board, DSP chip,
// DSP core, shared or local memory, page, block length, and starting address.
//
// pCount is the data block size in words, pCount * 2 bytes are returned.
//
// A maximum of 127 words can be retrieved.
//

public void readRAM(int pDSPChip, int pDSPCore, int pRAMType,
                   int pPage, int pAddress, int pCount, byte[] dataBlock)
{

//limit number of words to 127 because the number of bytes is twice that
//and the remote device must be able to specify the number of data bytes
//returned in the single size byte which can express a maximum of 255

if (pCount > 127) pCount = 127;

//limit bytes retrieved to size of array - pCount is in words so multiply by 2
//for the number of bytes
if ((pCount*2) > readDSPResult.length) pCount = readDSPResult.length / 2;

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)
// byte3 = address byte 2 (the page number)
// byte4 = address byte 1
// byte5 = address byte 0
// byte6 = size of data block to read (max is 255)

//clear flag - will be set true when processDataPackets encounters the
//return packet
readDSPDone = false;

//if shared memory is selected, set bit 20 of the HPIA register - this is
//bit 4 of the upper address byte 2

sendBytes7(READ_DSP_BLOCK_CMD, (byte)pDSPChip, (byte)pDSPCore,
            (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
            (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff),
            (byte)(pCount & 0xff));

// wait until processDSPPackets reaches the answer packet from the remote
// and processes it

timeOutRead = 0;
while(!readDSPDone && timeOutRead++ < TIMEOUT ){waitSleep(10);}

System.arraycopy(readDSPResult, 0, dataBlock, 0, pCount * 2);

}//end of UTBoard::readRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::readRAMDSP
//
// Returns one word of the contents of the RAM on the specified DSP chip,
// DSP core, shared or local memory, page, and address.
//
// The word is returned as two bytes via the array pRetBuffer, MSB first.
//
// If pForceProcessDataPackets is true, then the ForceProcessDataPackets will
// be called to wait for a packet to be returned.  This is necessary for cases
// when another thread is not already calling pForceProcessDataPackets, such
// as during startup.
//

void readRAMDSP(int pDSPChip, int pDSPCore, int pRAMType,
                   int pPage, int pAddress, byte[] pRetBuffer,
                   boolean pForceProcessDataPackets)
{

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)
// byte3 = address byte 2 (the page number)
// byte4 = address byte 1
// byte5 = address byte 0

//clear flag - will be set true when processDataPackets encounters the
//return packet
readDSPDone = false;

//if shared memory is selected, set bit 20 of the HPIA register - this is
//bit 4 of the upper address byte 2

sendBytes6(READ_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
            (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
            (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff));

// wait until processDSPPackets reaches the answer packet from the remote
// and processes it

if (pForceProcessDataPackets)
    //force waiting for and processing of receive packets
    processDataPackets(true, TIMEOUT);
else {
    timeOutRead = 0;
    while(!readDSPDone && timeOutRead++ < TIMEOUT){waitSleep(10);}
    }

//transfer the stored result and return to caller
pRetBuffer[0] = readDSPResult[0]; pRetBuffer[1] = readDSPResult[1];

}//end of UTBoard::readRAMDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getDSPRamChecksum
//
// Returns a one word checksum of the contents of the RAM on the specified
// DSP chip, DSP core, shared or local memory, page, address, and block size.
//
// The maximum block size is 65 kilobytes.
//
// If pForceProcessDataPackets is true, then the ForceProcessDataPackets will
// be called to wait for a packet to be returned.  This is necessary for cases
// when another thread is not already calling pForceProcessDataPackets, such
// as during startup.
//

int getDSPRamChecksum(int pDSPChip, int pDSPCore, int pRAMType,
                                    int pPage, int pAddress, int pBlockSize,
                                    boolean pForceProcessDataPackets)
{

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)
// byte3 = address byte 2 (the page number)
// byte4 = address byte 1
// byte5 = address byte 0
// byte6 = block size byte 1
// byte7 = block size byte 0

//clear flag - will be set true when processDataPackets encounters the
//return packet
getDSPRamChecksumDone = false;

//if shared memory is selected, set bit 20 of the HPIA register - this is
//bit 4 of the upper address byte 2

sendBytes8(GET_DSP_RAM_BLOCK_CHECKSUM, (byte)pDSPChip, (byte)pDSPCore,
            (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
            (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff),
            (byte)((pBlockSize >> 8) & 0xff),(byte)(pBlockSize & 0xff)
            );

// wait until processDSPPackets reaches the answer packet from the remote
// and processes it

if (pForceProcessDataPackets)
    //force waiting for and processing of receive packets
    //use a time out number large enough to give the remote time to scan the
    //block of DSP ram
    processDataPackets(true, 500);
else {
    timeOutRead = 0;
    while(!getDSPRamChecksumDone && timeOutRead++ < TIMEOUT){waitSleep(10);}
    }

//transfer the stored result and return to caller
return((int)((getDSPRamChecksumResult[0]<<8) & 0xff00)
                                + (int)(getDSPRamChecksumResult[1] & 0xff));

}//end of UTBoard::getDSPRamChecksum
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::readNextRAMDSP
//
// Returns the next word of the contents of the RAM on the specified DSP chip
// and DSP core.  The function readRAMDSP should already have been called to
// retrieve the first word in the block and set up the address.  This function
// is then used to retrieve subsequent words, the address will be incremented
// after each word by the remote device.
//
// The word is returned as two bytes via the array pRetBuffer, MSB first.
//

void readNextRAMDSP(int pDSPChip, int pDSPCore, byte[] pRetBuffer)
{

//transfer the values to the command packet
// byte0 = command
// byte1 = DSP chip number (1 or 2)
// byte2 = DSP core number (1,2,3, or 4)

//clear flag - will be set true when processDataPackets encounters the
//return packet
readDSPDone = false;

sendBytes3(READ_NEXT_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore);

// wait until processDSPPackets reaches the answer packet from the remote
// and processes it

timeOutRead = 0;
while(!readDSPDone && timeOutRead++ < TIMEOUT){waitSleep(10);}

//transfer the stored result and return to caller
pRetBuffer[0] = readDSPResult[0]; pRetBuffer[1] = readDSPResult[1];

}//end of UTBoard::readNextRAMDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::loadDSPCode
//
// Transmits an ASCII hex file to the selected DSP and core.
// Note that shared memory page 0 for core A & B is only accessible via core
// A HPI bus while shared memory page 0 for core C & D is only accessible via
// core C HPI bus.  Loading into cores A & B makes the code also available to
// cores C & D since cores A & B / C & D share the same program memory.
//
// The writeDPSRam function is used to write each code word.  This is not
// very efficient and could be improved by use of a block write function.  Since
// the buffer in the remote device can be overrun, the code loading is pause
// every 100 words while a status byte is retreived from the remote device.
// Since the loading is not resumed until the remote device processes and
// responds to the status request, it serves as a handshake.
//
// The format of the hex file is that produced by the Texas Instruments
// hex500.exe program using the following command line (or similar):
//
// hex500.exe -a "Capulin UT DSP.out" -o "CapulinUTDSP.hex" -romwidth 16
//
// The file format is as follows, with a shift to a new address being
// specified by $Axxxx unless the byte stream starts at 0000h in which case the
// first address shift will be absent.
//
// ctrl-B
// $A8000,
// 00 00 00 01
// EA 00 10 61 80 62 F0 73 80 05
// $Aff80,
// F0 73 80 02
// ctrl-C
//

void loadDSPCode(int pDSPChip, int pDSPCore)
{

FileInputStream inputStream = null;
int packetCnt = 0;

String core;
if (pDSPCore == 1) core = "A & B";
else
if (pDSPCore == 3) core = "C & D";
else core = "";

logger.logMessage("UT " + chassisSlotAddr + " loading DSP code for" + "\n"
        + "    Chip " + pDSPChip + " Cores " + core + "\n");

try {
    inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

    int c;
    int n3, n2, n1, n0;
    int address = 0, value = 0, place = 3;

    while ((c = inputStream.read()) != -1) {

        if (c == 2) continue; //catch the ctrl-B file start marker - skip it

        if (c == 3) break; //catch the ctrl-C file end marker - exit

        if (c == '\r' || c == '\n' || c == ',' || c == ' ') continue;

        // catch new address flag
        if (c == '$') {

            c = inputStream.read(); //read and ignore the 'A'

            //read next four characters to create new address value
            n3 = fromHex(inputStream.read());
            n2 = fromHex(inputStream.read());
            n1 = fromHex(inputStream.read());
            n0 = fromHex(inputStream.read());

           address =
                (int)((n3<<12) & 0xf000) +
                (int)((n2<<8) & 0xf00) +
                (int)((n1<<4) & 0xf0) +
                (int)(n0 & 0xf);

            continue;

            }
        else{

            //if this part reached, must be a digit for a character so add
            //it into value, using the place count to specify the shift

            c = fromHex(c);

            if (place == 3) value += (int)((c<<12) & 0xf000);
            else
            if (place == 2) value += (int)((c<<8)  & 0xf00);
            else
            if (place == 1) value += (int)((c<<4) & 0xf0);
            else{

                //fourth character converted, add it in and write word

                value += (int)(c & 0xf);

                //write to shared program memory page 0
                writeDSPRam(pDSPChip, pDSPCore, 1, 0, address++, value);

                //request and wait for a status flag ever so often to make sure
                //the remote's buffer is not overrun
                if(packetCnt++ >= 100){
                    getRemoteData(GET_STATUS_CMD, true);
                    packetCnt = 0;
                    }

                place = 3; value = 0; //start over for next word

                continue; //skip the decrement below
                }

            //can't decrement in the if statements because they will do the
            //dec even if the test fails
            place--;

            continue;

            }

        }//while ((c =
    }// try
catch(IOException e){

    logger.logMessage("Error opening DSP code file " + dspCodeFilename + "\n");

    }
finally {
    if (inputStream != null) {
        try{inputStream.close();}catch(IOException e){}
        }
    }// finally

}//end of UTBoard::loadDSPCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::verifyDSPCode
//
// Verifies that the DSP code has been loaded into the DSP.  Returns true if
// no errors, false otherwise.
//
// See the loadDSPCode function for more details.
//
// This function verifies the code by calculating checksums for contiguous
// blocks and then comparing with the checksum reported by the remote device
// for those specified blocks.
//

boolean verifyDSPCode(int pDSPChip, int pDSPCore)
{

boolean success = true;

FileInputStream inputStream = null;
int packetCnt = 0;

int blockCount = 0; //count the number of contiguous code blocks
int byteCount = 0; //count the number of bytes in the block
int checksum = 0;

String core;
if (pDSPCore == 1) core = "A & B";
else
if (pDSPCore == 3) core = "C & D";
else core = "";

logger.logMessage("UT " + chassisSlotAddr + " verifying DSP code for" + "\n"
        + "    Chip " + pDSPChip + " Cores " + core + "\n");

byte[] buffer = new byte[2];

try {
    inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

    int c;
    int n3, n2, n1, n0;
    int address = 0, value = 0, place = 3;

    while ((c = inputStream.read()) != -1) {

        if (c == 2) continue; //catch the ctrl-B file start marker - skip it

        if (c == 3) break; //catch the ctrl-C file end marker - exit

        if (c == '\r' || c == '\n' || c == ',' || c == ' ') continue;

        // catch new address flag
        if (c == '$') {

            c = inputStream.read(); //read and ignore the 'A'

            //read next four characters to create new address value
            n3 = fromHex(inputStream.read());
            n2 = fromHex(inputStream.read());
            n1 = fromHex(inputStream.read());
            n0 = fromHex(inputStream.read());

            //each time the address is changed, verify the checksum for the
            //previous block by comparing with the checksum from the remote

            //request the checksum for the current block as stored in the DSP
            //skip if the block size is 0
            if (byteCount > 0){

                checksum &= 0xffff; //only use lower word of checksum

                int remoteChecksum = getDSPRamChecksum(pDSPChip, pDSPCore, 1, 0,
                                                      address, byteCount, true);

                //compare the local and remote checksums
                if (checksum != remoteChecksum){
                    logger.logMessage("UT " + chassisSlotAddr + " DSP code error"
                     + "\n" + "    Chip " + pDSPChip + " Cores " + core
                     + "  Block: " + blockCount + "\n");
                    success = false;
                    }

                }// if (byteCount > 0)

            blockCount++; checksum = 0; byteCount = 0; //new block

            //point to new address
            address =
                (int)((n3<<12) & 0xf000) +
                (int)((n2<<8) & 0xf00) +
                (int)((n1<<4) & 0xf0) +
                (int)(n0 & 0xf);

            continue;

            }
        else{

            //if this part reached, must be a digit for a character so add
            //it into value, using the place count to specify the shift

            c = fromHex(c);

            if (place == 3) value += (int)((c<<12) & 0xf000);
            else
            if (place == 2) value += (int)((c<<8)  & 0xf00);
            else
            if (place == 1) value += (int)((c<<4) & 0xf0);
            else{

                //fourth character converted, add it in and write word
                value += (int)(c & 0xf);
                //track the number of bytes and the checksum
                byteCount++; checksum += value;

                place = 3; value = 0; //start over for next word

                continue; //skip the decrement below
                }

            //can't decrement in the if statements because they will do the
            //dec even if the test fails
            place--;

            continue;

            }

        }//while ((c =

    //validate the last block
    //request the checksum for the current block as stored in the DSP
    //skip if the block size is 0
    if (byteCount > 0){

        checksum &= 0xffff; //only use lower word of checksum

        int remoteChecksum = getDSPRamChecksum(pDSPChip, pDSPCore, 1, 0,
                                                      address, byteCount, true);

        //compare the local and remote checksums
        if (checksum != remoteChecksum){
            logger.logMessage("UT " + chassisSlotAddr + " DSP code error"
                     + "\n" + "    Chip " + pDSPChip + " Cores " + core
                     + "  Block: " + blockCount + "\n");
                    success = false;
                    }

        }// if (byteCount > 0)

    }// try
catch(IOException e){

    logger.logMessage("Error opening DSP code file " + dspCodeFilename + "\n");
    success = false;

    }
finally {
    if (inputStream != null) {
        try{inputStream.close();}catch(IOException e){}
        }
    }// finally

return(success);

}//end of UTBoard::verifyDSPCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::verifyDSPCode2
//
// Verifies that the DSP code has been loaded into the DSP.
// See the loadDSPCode function for more details.
//
// This function checks byte by byte and is VERY slow.  It is useful because
// it reports the address of any incorrect values.
//

void verifyDSPCode2(int pDSPChip, int pDSPCore)
{

FileInputStream inputStream = null;
int packetCnt = 0;

String core;
if (pDSPCore == 1) core = "A & B";
else
if (pDSPCore == 3) core = "C & D";
else core = "";

logger.logMessage("UT " + chassisSlotAddr + " verifying DSP code for" + "\n"
        + "    Chip " + pDSPChip + " Cores " + core + "\n");

byte[] buffer = new byte[2];

try {
    inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

    int c;
    int n3, n2, n1, n0;
    int address = 0, value = 0, place = 3;

    while ((c = inputStream.read()) != -1) {

        if (c == 2) continue; //catch the ctrl-B file start marker - skip it

        if (c == 3) break; //catch the ctrl-C file end marker - exit

        if (c == '\r' || c == '\n' || c == ',' || c == ' ') continue;

        // catch new address flag
        if (c == '$') {

            c = inputStream.read(); //read and ignore the 'A'

            //read next four characters to create new address value
            n3 = fromHex(inputStream.read());
            n2 = fromHex(inputStream.read());
            n1 = fromHex(inputStream.read());
            n0 = fromHex(inputStream.read());

           address =
                (int)((n3<<12) & 0xf000) +
                (int)((n2<<8) & 0xf00) +
                (int)((n1<<4) & 0xf0) +
                (int)(n0 & 0xf);

            continue;

            }
        else{

            //if this part reached, must be a digit for a character so add
            //it into value, using the place count to specify the shift

            c = fromHex(c);

            if (place == 3) value += (int)((c<<12) & 0xf000);
            else
            if (place == 2) value += (int)((c<<8)  & 0xf00);
            else
            if (place == 1) value += (int)((c<<4) & 0xf0);
            else{

                //fourth character converted, add it in and write word

                value += (int)(c & 0xf);

                //read from shared program memory page 0
                readRAMDSP(pDSPChip, pDSPCore, 1, 0, address++, buffer, true);

                int memValue = (int)((buffer[0]<<8) & 0xff00)
                                                    + (int)(buffer[1] & 0xff);

                //compare the value read from memory with the code from file
                if (value != memValue)
                    logger.logMessage(
                     "UT " + chassisSlotAddr + " DSP code error"
                     + "\n" + "    Chip " + pDSPChip + " Cores " + core
                     + "  Address: " + (address-1) + "\n");

                //request and wait for a status flag ever so often to make sure
                //the remote's buffer is not overrun
                if(packetCnt++ >= 100){
                    getRemoteData(GET_STATUS_CMD, true);
                    packetCnt = 0;
                    }

                place = 3; value = 0; //start over for next word

                continue; //skip the decrement below
                }

            //can't decrement in the if statements because they will do the
            //dec even if the test fails
            place--;

            continue;

            }

        }//while ((c =
    }// try
catch(IOException e){

    logger.logMessage("Error opening DSP code file " + dspCodeFilename + "\n");

    }
finally {
    if (inputStream != null) {
        try{inputStream.close();}catch(IOException e){}
        }
    }// finally

}//end of UTBoard::verifyDSPCode2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::fromHex
//
// Converts a hex character to decimal.
//

int fromHex(int pChar)
{

if (pChar == '0') return 0;
else
if (pChar == '1') return 1;
else
if (pChar == '2') return 2;
else
if (pChar == '3') return 3;
else
if (pChar == '4') return 4;
else
if (pChar == '5') return 5;
else
if (pChar == '6') return 6;
else
if (pChar == '7') return 7;
else
if (pChar == '8') return 8;
else
if (pChar == '9') return 9;
else
if (pChar == 'a' || pChar == 'A') return 10;
else
if (pChar == 'b' || pChar == 'B') return 11;
else
if (pChar == 'c' || pChar == 'C') return 12;
else
if (pChar == 'd' || pChar == 'D') return 13;
else
if (pChar == 'e' || pChar == 'E') return 14;
else
if (pChar == 'f' || pChar == 'F') return 15;
else
return 0;

}//end of UTBoard::fromHex
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendHardwareDelay
//
// Sends the number of samples to be ignored after the initial pulse by the
// FPGA before data collection begins.
//

void sendHardwareDelay(int pChannel, int pDelay)
{

writeFPGAReg(bdChs[pChannel].delayReg0, (byte) (pDelay & 0xff));
writeFPGAReg(bdChs[pChannel].delayReg1, (byte) ((pDelay >> 8) & 0xff));
writeFPGAReg(bdChs[pChannel].delayReg2, (byte) ((pDelay >> 16) & 0xff));
writeFPGAReg(bdChs[pChannel].delayReg3, (byte) ((pDelay >> 24) & 0xff));

}//end of UTBoard::sendHardwareDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendSoftwareDelay
//
// Sends the number of samples to be skipped by the DSP software before the
// AScan data set begins (pSoftwareDelay) and transmits to the DSP the number
// of samples being skipped by the FPGA after the initial pulse before the
// start of recording (pHardwareDelay).
//

void sendSoftwareDelay(int pChannel, int pSoftwareDelay, int pHardwareDelay)
{

sendChannelParam(pChannel, (byte) DSP_SET_DELAYS,
               (byte)((pSoftwareDelay >> 8) & 0xff),
               (byte)(pSoftwareDelay & 0xff),
               (byte)((pHardwareDelay >> 24) & 0xff),
               (byte)((pHardwareDelay >> 16) & 0xff),
               (byte)((pHardwareDelay >> 8) & 0xff),
               (byte)(pHardwareDelay & 0xff),
               (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendSoftwareDelay
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendSampleBufferStart
//
// Sends value to the registers in the FPGA which hold the starting address in
// the DSP for the sample buffer.
//

void sendSampleBufferStart(int pChannel, int pBufStart)
{

writeFPGAReg(bdChs[pChannel].bufStart0, (byte) (pBufStart & 0xff));
writeFPGAReg(bdChs[pChannel].bufStart1, (byte) ((pBufStart >> 8) & 0xff));
writeFPGAReg(bdChs[pChannel].bufStart2, (byte) ((pBufStart >> 16) & 0xff));

}//end of UTBoard::sendSampleBufferStart
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendHardwareRange
//
// Sends value to the registers in the FPGA which hold the number of samples to
// be recorded after each initial pulse.
//

void sendHardwareRange(int pChannel, int pCount)
{

//force pCount to be even - FPGA transfers two samples at a time to the DSP
//RAM via the HPI port - odd number will cause transfer lock up in FPGA
if (pCount % 2 != 0) pCount++;

writeFPGAReg(bdChs[pChannel].countReg0, (byte) (pCount & 0xff));
writeFPGAReg(bdChs[pChannel].countReg1, (byte) ((pCount >> 8) & 0xff));
writeFPGAReg(bdChs[pChannel].countReg2, (byte) ((pCount >> 16) & 0xff));

}//end of UTBoard::sendHardwareRange
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendAScanScale
//
// Sends the compression ratio to be used by the DSP when collecting data
// samples for an AScan data set.  For example, if pScale = 3, the DSP should
// squeeze the samples by 3.  For pScale = 1, no compression is applied.  The
// DSP is expected to perform peak capture for the samples to ensure that the
// peak data is preserved in the resulting AScan data set.
//
// The batch size for slow AScan processing is also sent.  This is
// the number of compressed output AScan data points the DSP processes after
// each shot. If too many are processed, the next shot may be missed resulting
// in a possible loss of data.  For each compressed data point, the number of
// input points processed will be batchSize * pScale as pScale tells the DSP
// how many input points to compress for each output point.  The number of
// input points is really what needs to be limited as it is often the larger
// of the two.  Since the number of inputs points processed varies according
// to pScale, pBatchSize is computed each time to give a reasonable number of
// input points to process during each cycle.
//
// The DSP actually processes twice as many input points for a given scale
// to provide the min/max peak as it would for just a min peak.  Thus, the
// scale must be divided in half to get the actual number of input samples
// that will be processed.
//
// Since the batchSize is rounded off, the number of raw bytes processed in
// each batch could vary quite a bit.
//
// As of 3/23/11, approximately 50 input points per cycle is used
//

void sendAScanScale(int pChannel, int pScale)
{

//protect against divide by zero
if (pScale < 1) pScale = 1;

// calculate number of output samples to process for a desired number of
// input samples to be processed -- rounding off is fine
// for example, if pScale is two then 25 output samples should be processed to
// process 50 input samples -- then divide by two again because the DSP
// processes twice as many input samples to accommodate the min and max peaks
// recorded instead of just one peak
// since the value is used as a loop counter, adjust down by subtracting 1
int batchSize = (50 / pScale / 2) - 1;

if (batchSize < 1) batchSize = 1;

sendChannelParam(pChannel, (byte) DSP_SET_ASCAN_SCALE,
               (byte)((pScale >> 8) & 0xff), (byte)(pScale & 0xff),
               (byte)((batchSize >> 8) & 0xff), (byte)(batchSize & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendAScanScale
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendGate
//
// Sends the gate start position, width, and level in the DSP.  To send the
// gates function flags, see sendGateFlags.
//
// The level should be relative to the signal values in the DSP, not a
// percentage.
//
// The DSP expects the width of the gate to be in sample time units and to be
// divided by three.  The DSP scans one of every three samples in each gate
// and uses the width as a loop counter.
//

void sendGate(int pChannel, int pGate, int pStart, int pWidth, int pLevel)
{

pWidth /= 3; // divide by three, see notes above

pLevel = (int)((pLevel * ASCAN_MAX_HEIGHT) / 100 / ASCAN_SCALE);

sendChannelParam(pChannel, (byte) DSP_SET_GATE,
                (byte)pGate,
                (byte)((pStart >> 24) & 0xff), (byte)((pStart >> 16) & 0xff),
                (byte)((pStart >> 8) & 0xff),  (byte)(pStart & 0xff),
                (byte)((pWidth >> 8) & 0xff),  (byte)(pWidth & 0xff),
                (byte)((pLevel >> 8) & 0xff),  (byte)(pLevel & 0xff)
                );

}//end of UTBoard::sendGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendGateFlags
//
// Sends the flags for pGate of pChannel.
//

public void sendGateFlags(int pChannel, int pGate, int pFlags)
{

sendChannelParam(pChannel, (byte) DSP_SET_GATE_FLAGS,
               (byte)pGate,
               (byte)((pFlags >> 8) & 0xff),
               (byte)(pFlags & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendGateSigProcThreshold
//
// Sends the signal processing thresholds for pGate of pChannel.
//

public void sendGateSigProcThreshold(int pChannel, int pGate, int pThreshold)
{

sendChannelParam(pChannel, (byte) DSP_SET_GATE_SIG_PROC_THRESHOLD,
               (byte)pGate,
               (byte)((pThreshold >> 8) & 0xff),
               (byte)(pThreshold & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendGateSigProcThreshold
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDAC
//
// Sends the DAC gate start position, width, and level in the DSP.  To send the
// gates function flags, see sendDACGateFlags.
//
// The level for a DAC gate is the gain multiplier to be used for that section.
//
// Unlike a normal gate, DAC gate widths are not divided by three.
//

void sendDAC(int pChannel, int pGate, int pStart, int pWidth, int pLevel)
{

//remotes will crash if gate width is less than 1
if (pWidth < 1) pWidth = 1;

sendChannelParam(pChannel, (byte) DSP_SET_DAC,
                (byte)pGate,
                (byte)((pStart >> 24) & 0xff), (byte)((pStart >> 16) & 0xff),
                (byte)((pStart >> 8) & 0xff),  (byte)(pStart & 0xff),
                (byte)((pWidth >> 8) & 0xff),  (byte)(pWidth & 0xff),
                (byte)((pLevel >> 8) & 0xff),  (byte)(pLevel & 0xff)
                );

}//end of UTBoard::sendDAC
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDACGateFlags
//
// Sends the flags for the DAC gate of pChannel to pFlags.
//

public void sendDACGateFlags(int pChannel, int pGate, int pFlags)
{

sendChannelParam(pChannel, (byte) DSP_SET_DAC_FLAGS,
               (byte)pGate,
               (byte)((pFlags >> 8) & 0xff),
               (byte)(pFlags & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendDACGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendWallChannelFlag
//
// Sends the flags which specifies the channel as being used for wall
// measurement.  Extra data will then be expected to be appended to peak data
// packets sent by the remote devices.
//

public void sendWallChannelFlag(int pChannel, boolean pIsWallChannel)
{

bdChs[pChannel].isWallChannel = pIsWallChannel;

}//end of Channel::sendWallChannelFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendHitMissCounts
//
// Sends the hit and miss count values for a gate.  The hit count is the
// number of consecutive times the signal must exceed the gate level before it
// will cause an alarm while the miss count is the consecutive number of times
// the signal must not reach the gate level before it causes and alarm.
//

void sendHitMissCounts(int pChannel, int pGate, int pHitCount, int pMissCount)
{

sendChannelParam(pChannel, (byte) DSP_SET_HIT_MISS_COUNTS,
               (byte)pGate,
               (byte)((pHitCount >> 8) & 0xff),
               (byte)(pHitCount & 0xff),
               (byte)((pMissCount >> 8) & 0xff),
               (byte)(pMissCount & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendHitMissCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::linkGates
//
// Sets a link to the gates for the channel pChannel.
//

public void linkGates(int pChannel, Gate[] pGates, int pNumberOfGates)
{

bdChs[pChannel].numberOfGates = pNumberOfGates;

bdChs[pChannel].gates = pGates;

}//end of Channel::linkGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendMode
//
// Sends the signal mode to one of the following:
//
// 0 = Positive half and RF (host computer shifts by half screen for RF)
// 1 = Negative half
// 2 = Full
// 3 = Off channel is not pulsed and is not displayed.
//

void sendMode(int pChannel, int pMode)
{

sendChannelParam(pChannel, (byte) DSP_SET_RECTIFICATION,
                (byte)(pMode & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
               (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getState
//
// Returns the state of various flags or values as selected by pWhich.
// If a flag is being requested, returns 0 for false and not 0 for true.
// If a value is being requested, returns the value.
//

public int getState(int pWhich)
{

if (pWhich == 0){

    //return the state of the Sampling Enabled flag - this is a flag in the
    //FPGA also called the Setup flag which chooses between Rabbit and host PC
    //control of the HPI interfaces or FPGA control which enables samples
    //to be stored in the DSP

    return(masterControlShadow & SETUP_RUN_MODE);

    }

//return the DSP's enabled and running flag
if (pWhich == 1) return(resetShadow & 0x3c);

//return the test/actual data flag
if (pWhich == 2) return(masterControlShadow & SIM_DATA);

return 0;

}//end of UTBoard::getState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setState
//
// Sets the state of various flags or values as selected by pWhich.
// If a flag is being specified, pValue 0 for false and not 0 for true.
// If a value is being specified, it will be set to pValue.
//

public void setState(int pWhich, int pValue)
{

if (pWhich == ENABLE_SAMPLING){

    //set the state of the Sampling Enabled flag - this is a flag in the
    //FPGA also called the Setup flag which chooses between Rabbit and host PC
    //control of the HPI interfaces or FPGA control which enables samples
    //to be stored in the DSP

    if (pValue == FALSE)
        masterControlShadow &= (~SETUP_RUN_MODE); //clear the flag (setup mode)
    else{

        //If entering run mode, always read a word from the DSP RAM page where
        //the A/D values are to be stored by the FPGA.  The FPGA does not set
        //the upper bits of the HPIA register - reading from the desired page
        //here first will set the bits properly.  This must be done for each
        //DSP core.

        byte[] buffer = new byte[2];

        //read from local data memory page 0 for all cores

        readRAMDSP(1, 1, 0, 0, 0000, buffer, false); //DSP1, Core A
        readRAMDSP(1, 2, 0, 0, 0000, buffer, false); //DSP1, Core B
        readRAMDSP(1, 3, 0, 0, 0000, buffer, false); //DSP1, Core C
        readRAMDSP(1, 4, 0, 0, 0000, buffer, false); //DSP1, Core D

        readRAMDSP(2, 1, 0, 0, 0000, buffer, false); //DSP2, Core A
        readRAMDSP(2, 2, 0, 0, 0000, buffer, false); //DSP2, Core B
        readRAMDSP(2, 3, 0, 0, 0000, buffer, false); //DSP2, Core C
        readRAMDSP(2, 4, 0, 0, 0000, buffer, false); //DSP2, Core D

        masterControlShadow |= SETUP_RUN_MODE;    //set the flag (run mode)
        }

    //place the FPGA internals into reset to prevent them from starting
    //in an unknown condition after changing the sampling registers

    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x01));

    writeFPGAReg(MASTER_CONTROL_REG, masterControlShadow);

    //release the FPGA internals from reset
    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x01)));
    }

if (pWhich == ENABLE_DSP_RUN){
    if (pValue == FALSE)
        //false, DSP run not enabled, set all core resets low (in reset)
        resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x3c)));
    else
        //true, DSP run enabled, set all core resets high (not in reset)
        resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x3c));
    }

if (pWhich == ENABLE_TEST_DATA){
    if (pValue == FALSE)
        //false, use A/D data instead of simulated data
        masterControlShadow = writeFPGAReg(
                MASTER_CONTROL_REG, (byte)(masterControlShadow & (~SIM_DATA)));
    else
        //true, use simulated data
        masterControlShadow = writeFPGAReg(
                   MASTER_CONTROL_REG, (byte)(masterControlShadow | SIM_DATA));
    }

if (pWhich == ENABLE_FPGA_INTERNALS){
    if (pValue == FALSE)
        //false, put all internal functions into reset
        resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x01));
    else
        //true, enable all internal functions
        resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x01)));
    }


}//end of UTBoard::setState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendSoftwareGain
//
// Sends the software gain applied by the DSP software for pChannel.  The
// gain is in decibels, which is arbitrarily scaled with a multiplier so that
// 50 dB puts a typical defect reflection at 80 percent of screen height.
//

public void sendSoftwareGain(int pChannel, double pSoftwareGain)
{

//convert decibels to linear gain: dB = 20 * log10(gain)
double gain = Math.pow(10, pSoftwareGain/20);

// a gain of 4 in the DSP code is realized by a multiplier of 2048
// this gain of 4 compares reasonably with other types of UT scopes at 50 dB
// 50 dB = 316.227766 linear gain     2048 / 316.227766 = 6.476
// thus the user set gain is multiplied by 6.476 so that 50db = gain of 4
// 50 dB = 316.227766  becomes 2048 by multiplying by 6.476
//

gain *= 6.476;

long roundedGain = Math.round(gain);

sendChannelParam(pChannel, (byte) DSP_SET_GAIN_CMD,
               (byte)((roundedGain >> 8) & 0xff),
               (byte)(roundedGain & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendSetFlags1
//
// Sends mask word to set one or more bits in the DSP's flags1 variable.
// To set a particular bit in the flag, the corresponding bit in pSetMask
// should be set to a 1.  Any bit in pSetMask which is a 0 is ignored.
//

public void sendSetFlags1(int pChannel, int pSetMask)
{

sendChannelParam(pChannel, (byte) DSP_SET_FLAGS1,
               (byte)((pSetMask >> 8) & 0xff),
               (byte)(pSetMask & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendSetFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendClearFlags1
//
// Sends mask word to clear one or more bits in the DSP's flags1 variable.
// To clear a particular bit in the flag, the corresponding bit in pSetMask
// should be set to a 0.  Any bit in pSetMask which is a 1 is ignored.
//

public void sendClearFlags1(int pChannel, int pClearMask)
{

sendChannelParam(pChannel, (byte) DSP_CLEAR_FLAGS1,
               (byte)((pClearMask >> 8) & 0xff),
               (byte)(pClearMask & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendClearFlags1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logDSPStatus
//
// Calls logDSPStatusHelper one or more times to check the status of the
// specified core.  See the notes for logDSPStatusHelper for more info.
//

public void logDSPStatus(int pDSPChip, int pDSPCore,
                                              boolean pForceProcessDataPackets)
{

//request status - try three times
if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) return;

if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) return;

if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) return;

}//end of Channel::logDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logDSPStatusHelper
//
// Requests and writes to the log window the status word from the specified
// DSP chip and Core.  The returned packet will be handled by
// processDataPackets.
//
// If pForceProcessDataPackets is true, the processDataPackets function will
// be called.  This is for use when that function is not already being called
// by another thread.
//
// Returns true if the proper response is received, false otherwise.
//

public boolean logDSPStatusHelper(int pDSPChip, int pDSPCore,
                                              boolean pForceProcessDataPackets)
{

readDSPStatus(pDSPChip, pDSPCore);

int bytesRead = 0;

int wait = 0;

//force waiting for and processing of receive packets
//loop until a dsp status message received
if (pForceProcessDataPackets){
    dspStatusMessageRcvd = false;
    while (!dspStatusMessageRcvd && wait++ < 10)
        bytesRead = processDataPackets(true, TIMEOUT);
    }

//if bytesRead is > 0, a packet was processed and the data will be in inBuffer
if (bytesRead > 0){

    int dspCoreFromPkt = (int)inBuffer[0];

    int status = (int)((inBuffer[1]<<8) & 0xff00) + (int)(inBuffer[2] & 0xff);

    //displays status code in log window
    logger.logMessage("UT " + chassisSlotAddr + " Chip: " + pDSPChip + " Core: "
              + pDSPCore + " Status: " + dspCoreFromPkt + "-" + status + "\n");
    return(true);

    }
else{

    logger.logMessage("UT " + chassisSlotAddr + " Chip: " + pDSPChip + " Core: "
            + pDSPCore + " Status Packet Error" + "\n");
    return(false);

    }

}//end of Channel::logDSPStatusHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::readDSPStatus
//
// Requests the status word from the specified DSP chip and Core.  The
// returned packet will be handled by processDataPackets.
//

public void readDSPStatus(int pDSPChip, int pDSPCore)
{

sendBytes15(MESSAGE_DSP_CMD,
           (byte)pDSPChip, (byte)pDSPCore,
           (byte) DSP_GET_STATUS_CMD, // DSP message type id
           (byte) 2, //return packet size expected
           (byte) 9, //data packet size sent (DSP always expects 9)
           (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 , (byte) 0,
           (byte) 0, (byte)0, (byte)0
           );

}//end of Channel::readDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendChannelParam
//
// Sends a message to both cores assigned to handle the channel specified by
// pChannel.  The message type is specified by pMsgID, the data bytes by
// pByte1 - pByte9.
//
// This function is for use in setting values and cannot be used to read them.
// The return packet size is always set to one as ALL functions called in the
// DSP by this method should return an ACK packet.
//
// Each channel is handled by two cores, so the message is sent to each
// appropriate core.
//

public void sendChannelParam(int pChannel, byte pMsgID,
                        byte pByte1, byte pByte2, byte pByte3, byte pByte4,
                        byte pByte5, byte pByte6, byte pByte7,
                        byte pByte8, byte pByte9)
{

// 1st Core handling the channel

sendBytes15(MESSAGE_DSP_CMD, bdChs[pChannel].dspChip, bdChs[pChannel].dspCore1,
           pMsgID, // DSP message type id
           (byte) 1, //return packet size expected (expects an ACK packet)
           (byte) 9, //data packet size sent
           pByte1, pByte2, pByte3, pByte4,
           pByte5, pByte6, pByte7, pByte8, pByte9
           );

// 2nd Core handling the channel

sendBytes15(MESSAGE_DSP_CMD, bdChs[pChannel].dspChip, bdChs[pChannel].dspCore2,
           pMsgID, // DSP message type id
           (byte) 1, //return packet size expected (expects an ACK packet)
           (byte) 9, //data packet size sent
           pByte1, pByte2, pByte3, pByte4,
           pByte5, pByte6, pByte7, pByte8, pByte9
           );

}//end of Channel::sendChannelParam
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDSPSampleSize
//
// Sends the sample collection size in the DSPs for the specified channel.  The
// FPGA record this many samples but will transfer half this number of words
// because it stuffs two byte samples into each transfer.
//
// Each channel is handled by two cores, so the message is sent to each
// appropriate core
//

public void sendDSPSampleSize(int pChannel, int pSampleSize)
{

sendChannelParam(pChannel, (byte)DSP_SET_AD_SAMPLE_SIZE_CMD,
               (byte)((pSampleSize >> 8) & 0xff), (byte)(pSampleSize & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of Channel::sendDSPSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::requestAScan
//
// Requests an aScan packet for the board analog channel specified by
// pChannel.
//
// The parameter pHardwareDelay is stored for use by the function which
// processes the returned packet.
//

public void requestAScan(int pChannel, int pHardwareDelay)
{

//store the hardware delay so that processAScanPacket can use it later when
//the return packet is processed
hardwareDelay = pHardwareDelay;

//debug mks
//about every 15 seconds, an AScan request or return packet is lost - about
//every 320 packets - the dbug variable is being used to track the number
//of packets transmitted before failure and can be removed after fix
//Update: on 10/10/09, this had improved greatly to one error for every
//10-50 thousand packets
// UPdate:  This may have been caused by accessing the com link by two
// different threads.  Fixed now?
//debug mks end

//if a request is not still pending, send a request for a new aScan data packet
if (aScanRcvd){

    if (aScanCoreSelector == 1) aScanCoreSelector = 2;
    else aScanCoreSelector = 1;

    sendBytes4(GET_ASCAN_CMD, bdChs[pChannel].dspChip,
    aScanCoreSelector == 1 ? bdChs[pChannel].dspCore1 : bdChs[pChannel].dspCore2,
    (byte) pChannel);

    // block further AScan requests until processDSPPackets processes the answer
    // packet from the previous request
    aScanRcvd = false;

    getAScanTimeOut = 0; //restart timeout

    dbug++; //debug mks - remove this
    }
else {
    // if the packet does not get an answer, after about .25 seconds reset
    // the flag to force a new call on the next time through
    if (getAScanTimeOut++ == GET_ASCAN_TIMEOUT) {
        getAScanTimeOut = 0; aScanRcvd = true;
        dbug=0; //debug mks - remove this
        return;
        }
    }

}//end of UTBoard::requestAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getAScan
//
// Returns a pointer to a previously received AScan dataset or returns null
// if a new dataset has not been received since the last call to this function.
//

public AScan getAScan()
{

//return a pointer to the array holding the last received aScan data
//if new data has not been received, then return null

if (aScanDataPacketProcessed){
    aScanDataPacketProcessed = false;
    return (aScan);
    }
else
    return(null);

}//end of UTBoard::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::requestPeakData
//
// Requests a peak data packet for the board analog channel specified by
// pChannel.  The Rabbit processor collects the peak data from the two DSP
// cores which are assigned to handle the channel.
//
// Each channel is handled by two DSP cores on alternate pulses. The Rabbit
// will read the peak data from both cores handling the specified channel and
// return the worst case peak from the two.
//

public void requestPeakData(int pChannel)
{

//if a request is not still pending, send a request for a new peak data packet
if (peakDataRcvd){

    // if the channel is setup for wall data, last byte sent is set to 1

    sendBytes4(GET_PEAK_DATA_CMD,
            (byte)pChannel, (byte)bdChs[pChannel].numberOfGates,
            (byte)(bdChs[pChannel].isWallChannel ? 1:0));

    getPeakDataTimeOut = 0; //restart timeout

    }
else {
    // if the packet does not get an answer, after about .25 seconds reset
    // the flag to force a new call on the next time through
    if (getPeakDataTimeOut++ == GET_PEAK_DATA_TIMEOUT) {
        getPeakDataTimeOut = 0; peakDataRcvd = true;
        return;
        }
    }

// block further requests until processDSPPackets processes the answer
//packet from the previous request
peakDataRcvd = false;

}//end of Channel::requestPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::requestPeakData4
//
// Requests a peak data packet for four channels specified by pChannelx.  The
// Rabbit processor collects the peak data from the two DSP cores which are
// assigned to handle each channel.
//
// Each channel is handled by two DSP cores on alernate pulses. The Rabbit
// will read the peak data from both cores handling the specified channel and
// return the worst case peak from the two.
//

public void requestPeakData4(int pChannel0, int pChannel1, int pChannel2,
                                                                int pChannel3)
{

//if a request is not still pending, send a request for a new peak data packet
if (peakDataRcvd){

    // for any channel setup for wall data, set corresponding bit to 1 - this
    // byte is then sent with the request

    byte wallFlags = 0;

    if(bdChs[pChannel0].isWallChannel) wallFlags |= 1;
    if(bdChs[pChannel1].isWallChannel) wallFlags |= 2;
    if(bdChs[pChannel2].isWallChannel) wallFlags |= 4;
    if(bdChs[pChannel3].isWallChannel) wallFlags |= 8;

    sendBytes10(GET_PEAK_DATA4_CMD,
            (byte)pChannel0, (byte)bdChs[pChannel0].numberOfGates,
            (byte)pChannel1, (byte)bdChs[pChannel1].numberOfGates,
            (byte)pChannel2, (byte)bdChs[pChannel2].numberOfGates,
            (byte)pChannel3, (byte)bdChs[pChannel3].numberOfGates,
            wallFlags
            );

    getPeakDataTimeOut = 0; //restart timeout

    }
else {
    // if the packet does not get an answer, after about .25 seconds reset
    // the flag to force a new call on the next time through
    if (getPeakDataTimeOut++ == GET_PEAK_DATA_TIMEOUT) {
        getPeakDataTimeOut = 0; peakDataRcvd = true;
        return;
        }
    }

// block further requests until processDSPPackets processes the answer
//packet from the previous request
peakDataRcvd = false;

}//end of Channel::requestPeakData4
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getPeakDataFromDSP
//
// Requests a peak data packet from a single DSP core - DSP chip 1 or 2 and
// cores A and C from each.
//
// NOTE: Each channel is handled by two DSP cores on alernate pulses.  This
//  function only reads the peak data from a SINGLE core, ignoring the second
//  core for the channel.  It retrieves the data directly from the DSP using
//  the MESSAGE_DSP_CMD which the Rabbit passes on to the DSP.
//
// THIS FUNCTION IS ONLY FOR DEBUGGING PURPOSES
//  Use requestPeakData or requestPeakData4 for proper results.  That function
//  requests the Rabbit to collect the peak data from each DSP core handling
//  the specified channel and send back the worst peak data from the two.
//

public void getPeakDataFromDSP(int pChannel)
{

//the number of data bytes expected in the return packet is determined by the
//number of gates for the channel
int numberReturnBytes =
        bdChs[pChannel].numberOfGates * PEAK_DATA_BYTES_PER_GATE;

sendBytes15(MESSAGE_DSP_CMD,
           (byte)bdChs[pChannel].dspChip,
           (byte)bdChs[pChannel].dspCore1,
           (byte) DSP_GET_PEAK_DATA, // DSP message type id
           (byte) numberReturnBytes, //return packet size expected
           (byte) 9, //data packet size sent (DSP always expects 9)
           (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 , (byte) 0,
           (byte) 0, (byte)0, (byte)0
           );

}//end of Channel::getPeakDataFromDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::driveSimulation
//
// Drive any simulation functions if they are active.  This function is usually
// called from a thread.
//

public void driveSimulation()
{

if (simulate && socket != null) ((UTSimulator)socket).processDataPackets(false);

}//end of UTBoard::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//

private void configure(IniFile pConfigFile)
{

inBuffer = new byte[RUNTIME_PACKET_SIZE];
outBuffer = new byte[RUNTIME_PACKET_SIZE];

fpgaCodeFilename =
  pConfigFile.readString("Hardware", "UT FPGA Code Filename", "not specified");

dspCodeFilename =
  pConfigFile.readString("Hardware", "UT DSP Code Filename", "not specified");

}//end of UTBoard::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::configureExtended
//
// Loads further configuration settings from the configuration.ini file.
// These settings are stored using the boards chassis and slot addresses, so
// they cannot be loaded until after the host has uploaded the FPGA code to the
// board.
//
//

private void configureExtended(IniFile pConfigFile)
{

String section = "UT Board in Chassis " + chassisAddr + " Slot " + slotAddr;

nSPerDataPoint = pConfigFile.readDouble(section, "nS per Data Point", 15.0);
uSPerDataPoint = nSPerDataPoint / 1000;

numberOfBanks = pConfigFile.readInt(section, "Number Of Banks", 1) - 1;
//check for validity - set to one bank (value of zero)
if (numberOfBanks < 0 || numberOfBanks > 3) numberOfBanks = 0;

repRateInHertz = pConfigFile.readInt(section, "Pulse Rep Rate in Hertz", 2000);

//rep rate is for each channel
//multipy the rep rate by the number of banks and multiply by the clock period
//to get the number of clock counts per pulse
//to get counts: (2000 * number of banks) * 0.000000015
// 0.000000015 = 15 ns
// add one to numberOfBanks because it is zero based

repRate = (int)(1/(repRateInHertz * (numberOfBanks+1) * 0.000000015));

//limit to a safe value - if the rep rate is too high and the pulse width too
//wide, the pulser circuitry will have an excessive duty cycle and burn up
//4166 is twice 2Khz for 4 channels - a reasonable maximum
// (a smaller value is a higher rep rate)
if (repRate < 4166 || repRate > 65535 ) repRate = 33333;

//each count is 15 ns
triggerWidth = pConfigFile.readInt(section, "Pulse Width", 15);

//limit to a safe value - see notes above for repRate
if (triggerWidth < 0 || triggerWidth > 50) triggerWidth = 15;

//each count is 15 ns
syncWidth = pConfigFile.readInt(section, "Sync Width", 200);

//each count is 15 ns
pulseDelay = pConfigFile.readInt(section, "Pulse Delay", 2);

//only one board in the system can be the sync source
syncSource = pConfigFile.readBoolean(
                                section, "Board is Pulse Sync Source", false);

}//end of UTBoard::configureExtended
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processOneDataPacket
//
// This function processes a single data packet if it is available.  If
// pWaitForPkt is true, the function will wait until data is available.
//
// The amount of time the function is to wait for a packet is specified by
// pTimeOut.  Each count of pTimeOut equals 10 ms.
//
// This function should be called often to allow processing of data packets
// received from the remotes and stored in the socket buffer.
//
// All packets received from the remote devices should begin with
// 0xaa, 0x55, 0xbb, 0x66, followed by the packet identifier, the DSP chip
// identifier, and the DSP core identifier.
//
// Returns number of bytes retrieved from the socket, not including the
// 4 header bytes, the packet ID, the DSP chip ID, and the DSP core ID.
// Thus, if a non-zero value is returned, a packet was processed.  If zero
// is returned, some bytes may have been read but a packet was not successfully
// processed due to missing bytes or header corruption.
// A return value of -1 means that the buffer does not contain a packet.
//

@Override
public int processOneDataPacket(boolean pWaitForPkt, int pTimeOut)
{

if (byteIn == null) return -1;  //do nothing if the port is closed

try{

    //wait a while for a packet if parameter is true
    if (pWaitForPkt){
        timeOutWFP = 0;
        while(byteIn.available() < 7 && timeOutWFP++ < pTimeOut){waitSleep(10);}
        }

    //wait until 7 bytes are available - this should be the 4 header bytes, the
    //packet identifier, the DSP chip identifier, and the DSP core identifier
    if (byteIn.available() < 7) return -1;

    //read the bytes in one at a time so that if an invalid byte is encountered
    //it won't corrupt the next valid sequence in the case where it occurs
    //within 3 bytes of the invalid byte

    //check each byte to see if the first four create a valid header
    //if not, jump to resync which deletes bytes until a valid first header
    //byte is reached

    //if the reSynced flag is true, the buffer has been resynced and an 0xaa
    //byte has already been read from the buffer so it shouldn't be read again

    //after a resync, the function exits without processing any packets

    if (!reSynced){
        //look for the 0xaa byte unless buffer just resynced
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] != (byte)0xaa) {reSync(); return 0;}
        }
    else reSynced = false;

    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0x55) {reSync(); return 0;}
    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0xbb) {reSync(); return 0;}
    byteIn.read(inBuffer, 0, 1);
    if (inBuffer[0] != (byte)0x66) {reSync(); return 0;}

    //read in the packet identifier and the dsp chip and core identifiers
    byteIn.read(inBuffer, 0, 3);

    //store the ID of the packet (the packet type)
    pktID = inBuffer[0];

    //store the ID of the chip associated with this packet
    pktDSPChipID = inBuffer[1];

    //store the ID of the core associated with this packet
    pktDSPCoreID = inBuffer[2];

    //reset the DSP message ID to null because it no longer is associated
    //with the pktID which now matches this packet - dspMsgID is updated
    //by the processDSPMessage function

    dspMsgID = DSP_NULL_MSG_CMD;

    if ( pktID == GET_STATUS_CMD) return process2BytePacket();

    if ( pktID == GET_ASCAN_CMD) return processAScanPacket();

    if ( pktID == READ_DSP_CMD || pktID == READ_NEXT_DSP_CMD)
        return processReadDSPPacket();

    if ( pktID == READ_DSP_BLOCK_CMD) return processReadDSPBlockPacket();

    if ( pktID == MESSAGE_DSP_CMD) return processDSPMessage();

    if ( pktID == GET_PEAK_DATA_CMD) return processPeakDataPacket(1);

    if ( pktID == GET_PEAK_DATA4_CMD) return processPeakDataPacket(4);

    if ( pktID == GET_DSP_RAM_BLOCK_CHECKSUM)
        return processGetDSPRamChecksumPacket();

    }
catch(IOException e){}

return 0;

}//end of UTBoard::processOneDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processDataPacketsUntilPeakPacket
//
// Processes incoming data packets until aPeak Data packet has been processed.
//
// Returns 1 if an Encoder data packet has been processed, -1 if all available
// packets have been processed but no peak data packet was present.
//
// See processOneDataPacket notes for more info.
//

public int processDataPacketsUntilPeakPacket()
{

int x = 0;

//this flag will be set true if a Peak Data packet is processed
peakDataPacketProcessed = false;

//process packets until there is no more data available or until a Peak Data
//packet has been processed

while ((x = processOneDataPacket(false, TIMEOUT)) > 0
                                          && peakDataPacketProcessed == false){}


if (peakDataPacketProcessed == true) return 1;
else return -1;

}//end of UTBoard::processDataPacketsUntilPeakPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processDSPMessage
//
// Processes a message from a DSP.  The message identifier whould be the first
// byte in the socket.
//
// The message is expected to already be available in the buffer - this
// function will not wait for data.
//
// Returns number of bytes retrieved from the socket.
//

private int processDSPMessage()
{

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 1) break;
        waitSleep(10);
        }
    if (byteIn.available() < 1) return 0;
    byteIn.read(inBuffer, 0, 1);
    }// try
catch(IOException e){}

dspMsgID = inBuffer[0];

if ( dspMsgID == DSP_GET_STATUS_CMD) return processDSPStatusMessage();

if ( dspMsgID == DSP_ACKNOWLEDGE) return processDSPAckMessage();

return 0;

}//end of UTBoard::processDSPMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::reSync
//
// Clears bytes from the socket buffer until 0xaa byte reached which signals
// the *possible* start of a new valid packet header or until the buffer is
// empty.
//
// If an 0xaa byte is found, the flag reSynced is set true to that other
// functions will know that an 0xaa byte has already been removed from the
// stream, signalling the possible start of a new packet header.
//
// There is a special case where a 0xaa is found just before the valid 0xaa
// which starts a new packet - the first 0xaa is the last byte of the previous
// packet (usually the checksum).  In this case, the next packet will be lost
// as well.  This should happen rarely.
//

public void reSync()
{

reSynced = false;

//track the number of times this function is called, even if a resync is not
//successful - this will track the number of sync errors
reSyncCount++;

//store info pertaining to what preceded the reSync - these values will be
//overwritten by the next reSync, so they only reflect the last error
//NOTE: when a reSync occurs, these values are left over from the PREVIOUS good
// packet, so they indicate what PRECEDED the sync error.

reSyncDSPChip = pktDSPChipID; reSyncDSPCore = pktDSPCoreID;
reSyncPktID = pktID; reSyncDSPMsgID = dspMsgID;

try{
    while (byteIn.available() > 0) {
        byteIn.read(inBuffer, 0, 1);
        if (inBuffer[0] == (byte)0xaa) {reSynced = true; break;}
        }
    }
catch(IOException e){}

}//end of UTBoard::reSync
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processReadDSPPacket
//
// Retrieves the word read from DSP RAM, stores it as bytes in readDSPResult,
// and sets the readDSPDone flag true.  The readDSPDone flag will be set true
// even if the wait times out.
//
// Returns number of bytes retrieved from the socket.
//

private int processReadDSPPacket()
{

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 2) break;
        waitSleep(10);
        }
    if (timeOutProcess < TIMEOUT && byteIn.available() >= 2){
        int c = byteIn.read(readDSPResult, 0, 2);
        readDSPDone = true; //must be set AFTER buffer read
        return(c);
        }
    else {
        readDSPResult[0] = 0; readDSPResult[1] = 0; readDSPDone = true;
        }
    }// try
catch(IOException e){}

return 0;

}//end of UTBoard::processReadDSPPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processReadDSPBlockPacket
//
// Retrieves the block read from DSP RAM, stores it as bytes in readDSPResult,
// and sets the readDSPDone flag true.  The readDSPDone flag will be set true
// even if the wait times out.
//
// Returns number of bytes retrieved from the socket.
//
// The first byte in the packet should be the number of data bytes to follow
// up to a maximum of 255.
//

private int processReadDSPBlockPacket()
{

int c = 0;

try{

    //read the first byte which tells the number of data bytes to follow

    timeOutProcess = 0;

    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 1) break;
        waitSleep(10);
        }

    if (timeOutProcess < TIMEOUT && byteIn.available() >= 1){
        c = byteIn.read(readDSPResult, 0, 1);
        }
    else {
        for (int i=0; i < readDSPResult.length; i++) readDSPResult[i] = 0;
        readDSPDone = true;
        return(c);
        }

    // get the number of data bytes and validate - cast to  int and AND with
    // 0xff to ignore two's complement and give 0-255

    int count;
    count = (int)readDSPResult[0] & 0xff;
    if (count > readDSPResult.length) count = readDSPResult.length;

    // read in the data bytes

    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= count) break;
        waitSleep(10);
        }

    if (timeOutProcess < TIMEOUT && byteIn.available() >= count){
        c += byteIn.read(readDSPResult, 0, count);
        readDSPDone = true;
        return(c);
        }
    else {
        for (int i=0; i < readDSPResult.length; i++) readDSPResult[i] = 0;
        readDSPDone = true;
        return(c);
        }

}// try
catch(IOException e){}

return(c);

}//end of UTBoard::processReadDSPBlockPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processDSPStatusMessage
//
// Retrieves the status value from the packet and stores it in inBuffer.
//
// Returns number of bytes retrieved from the socket.
//

private int processDSPStatusMessage()
{

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 3) break;
        waitSleep(10);}
    if (byteIn.available() >= 3){
        dspStatusMessageRcvd = true;
        return byteIn.read(inBuffer, 0, 3);
        }
    }// try
catch(IOException e){}

return 0; // failure - no bytes read

}//end of UTBoard::processDSPStatusMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processGetDSPRamChecksumPacket
//
// Retrieves the checksum for the DSP RAM block, stores it as bytes in
// getDSPRamChecksumResult, and sets the getDSPRamChecksumDone flag true.  The
// getDSPRamChecksumDone flag will be set true even if the wait times out.
//
// Returns number of bytes retrieved from the socket.
//

private int processGetDSPRamChecksumPacket()
{

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 2) break;
        waitSleep(10);
        }
    if (timeOutProcess < TIMEOUT && byteIn.available() >= 2){
        int c = byteIn.read(getDSPRamChecksumResult, 0, 2);
        getDSPRamChecksumDone = true; //must be set AFTER buffer read
        return(c);
        }
    else {
        getDSPRamChecksumResult[0] = 0;
        getDSPRamChecksumResult[1] = 0;
        getDSPRamChecksumDone = true;
        }
    }// try
catch(IOException e){}

return 0;

}//end of UTBoard::processGetDSPRamChecksumPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processDSPAckMessage
//
// Receives an acknowledgement packet from the DSP.
//
// Various functions return an acknowledgment package just to control the
// rate of data transmission into the DSP.  The host computer can send all
// the data at one time and ignore the ACK packets sent back, but the ACK
// packet will force the Rabbit controller to wait between each packet it
// sends to the DSP.  This prevents the serial port buffer allocated in the
// DSP code from being overrun.
//
// If the host needs verification of the type and data of the packet received
// by the DSP, the DSP should send back a packet with the same packet ID as
// transmitted by the host.  Another function should then be added to this
// module to catch those packets and perform the verification.
//
// Returns number of bytes retrieved from the socket.
//

private int processDSPAckMessage()
{

// the ack packet has two data bytes
// the DSP core returned by the core itself is the first byte
// for the ack packet, the second byte is undefined

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 2) break;
        waitSleep(10);
        }
    if (byteIn.available() >= 2) return byteIn.read(inBuffer, 0, 2);
    }// try
catch(IOException e){}

return 0; // failure - no bytes read

}//end of UTBoard::processDSPAckMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processAScanPacket
//
// Transfers data from an AScan data packet to a class array.  The data is
// first stored in a FIFO so multiple samples can be averaged for smoothing.
//
// Returns number of bytes retrieved from the socket.
//

public int processAScanPacket()
{

int x;

//allow another request packet to be transmitted now that the return
//packet for the previous request has been received
aScanRcvd = true;

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 804) break;
        waitSleep(10);
        }
    if ((x = byteIn.available()) >= 804) byteIn.read(inBuffer, 0, 804);
    else
        return 0;
    }// try
catch(IOException e){}

//get the board channel from the packet for the aScan data set
int channel = inBuffer[0];
if (channel < 0) channel = 0; if (channel > 3) channel = 3;

int aScanSmoothing = bdChs[channel].aScanSmoothing;

//move to the next position of the filtering FIFO each time
aScanFIFOIndex++;
if (aScanFIFOIndex >= aScanSmoothing) aScanFIFOIndex = 0;

//get the aScan range associated with this data set - this should be used for
//the display because when the range is being changed, the value the host
//currently has may not be what was used by the DSP due to pipeline latency
//in the connection

aScanFIFO[aScanFIFOIndex].range = inBuffer[1];

//get the location where the interface crossed the interface gate

aScanFIFO[aScanFIFOIndex].interfaceCrossingPosition =
        (int)((inBuffer[2]<<8) & 0xff00) + (int)(inBuffer[3] & 0xff);

//the interface crossing position returned by the DSP is relative to the
//start of the sample buffer stored by the FPGA - the FPGA delays by
//hardwareDelay number of samples from the initial pulse before recording,
//add this back in to make the crossing value relative to the initial pulse
aScanFIFO[aScanFIFOIndex].interfaceCrossingPosition += hardwareDelay;

for (int i = 0; i < firBuf.length; i++) firBuf[i] = 0;

//transfer the bytes to the int array - allow for sign extension
//400 words from 800 bytes, MSB first
//the +4 shifts past the leading info bytes
//the +5 points to the LSB (would be +1 without the shift)

for (int i=0; i<ASCAN_SAMPLE_SIZE; i++){

    int raw = 0, filtered = 0;

     raw =
       (int)((int)(inBuffer[i*2+4]<<8) + (inBuffer[(i*2)+5] & 0xff));

    if (raw > 0 && raw < bdChs[channel].rejectLevel) raw = raw % 10;
    else
    if (raw < 0 && raw > -bdChs[channel].rejectLevel) raw = raw % 10;

    raw *= ASCAN_SCALE;

    boolean filterActive = false;

    if (filterActive){

        //apply FIR filtering

        //shift the old samples and insert the newest
        for(int n = firBuf.length-1; n>0; n--) firBuf[n] = firBuf[n-1];
        firBuf[0] = raw;

        //calculate the new filtered output value
        for(int n=0; n<firCoef.length; n++) filtered += firCoef[n] * firBuf[n];

        filtered /= 290000;

        }
    else{
        filtered = raw; //no filtering applied
        }

    aScanFIFO[aScanFIFOIndex].buffer[i] = filtered;

    }// for (int i=0; i<ASCAN_SAMPLE_SIZE; i++)


// the display thread can be accessing the aScan buffer at any time, so
// prepare all the data in the aScanBuffer first - transferring from
// the aScanBuffer to aScan is not visible because the data is nearly identical
// each time when the transfer occurs

//transfer the range to the aScan object as is
aScanBuffer.range = aScanFIFO[aScanFIFOIndex].range;
//the interfaceCrossingPosition gets averaged
aScanBuffer.interfaceCrossingPosition = 0;
//the data samples get averaged
for (int i=0; i<ASCAN_SAMPLE_SIZE; i++) aScanBuffer.buffer[i] = 0;

for (int i=0; i<aScanSmoothing; i++){

    //sum the interface crossing position from each dataset
    aScanBuffer.interfaceCrossingPosition +=
                                        aScanFIFO[i].interfaceCrossingPosition;

    //sum all datasets in the fifo
    for (int j=0; j<ASCAN_SAMPLE_SIZE; j++)
        aScanBuffer.buffer[j] += aScanFIFO[i].buffer[j];

    }//for (int i=1; i< aScanSmoothing; i++)


// transfer from aScanBuffer to aScan now that most calculations are done
//divide values by the number of samples summed from the FIFO to get average

//transfer the range to the aScan object as is
aScan.range = aScanBuffer.range;

int t;

aScan.interfaceCrossingPosition =
                    aScanBuffer.interfaceCrossingPosition / aScanSmoothing;
for (int i=0; i<ASCAN_SAMPLE_SIZE; i++) {
    t = aScanBuffer.buffer[i] / aScanSmoothing;
    //t = aScanBuffer.buffer[i]; //debug mks see line above --  -- don't scale the average as DSP does not
    aScan.buffer[i] = t;
    }

//signal that new aScan data is available
aScanDataPacketProcessed = true;

return(804); //number of bytes read from packet

}//end of UTBoard::processAScanPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processPeakData
//
// Extracts the peak data from a packet.
//
// Parameter pNumberOfChannels is the number of channels expected in the
// packet.
//
// Parameter pBufPtr points to the next byte to be read from the packet.
//
// Returns a number greater than zero if a packet successfully extracted.
//

public int processPeakData(int pNumberOfChannels, int pEncoder1, int pEncoder2)
{

int x = 0;

//process number of channels specified - each channel data section in the
//packet has the number of gates for that channel

for (int h=0; h < pNumberOfChannels; h++){

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= 2) break;
            waitSleep(10);
            }
        if ((byteIn.available()) >= 2)
            byteIn.read(inBuffer, 0, 2);
        else
            return 0;
        }// try
    catch(IOException e){}

     x = 0;

    //retrieve the board channel 0-3
    //this channel number refers to the analog channels on the board
    int channel = inBuffer[x++];

    // if the channel number is illegal, bail out - the code will resync to
    // toss the unused bytes still in the socket

    if (channel < 0 || channel > NUMBER_OF_BOARD_CHANNELS-1) return x;

    int numberOfGates = inBuffer[x++]; //number of gates for the channel

    // if the gate count is illegal, bail out - the code will resync to
    // toss the unused bytes still in the socket

    if (numberOfGates < 0 || numberOfGates > 9) return x;

    // calculate the number of data bytes
    int numberDataBytes = numberOfGates * PEAK_DATA_BYTES_PER_GATE;

    //add extra for wall data if the specified channel has such
    if (bdChs[channel].isWallChannel)
        numberDataBytes += PEAK_DATA_BYTES_FOR_WALL;

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= numberDataBytes) break;
            waitSleep(10);
            }
        if ((byteIn.available()) >= numberDataBytes)
            byteIn.read(inBuffer, 0, numberDataBytes);
        else
            return 0;
        }// try
    catch(IOException e){}

    int peakFlags;
    int peak;
    int peakFlightTime;
    int peakTrack;

    x = 0;

    for (int i=0; i < numberOfGates; i++){

        peakFlags =
            (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

        //did the gate receive the host specified number of consecutive hits?
        boolean hitCountMet = true;
        hitCountMet = (peakFlags & HIT_COUNT_MET) == 0 ? false : true;

        //cast to short used to force sign extension for signed values
        peak = (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        //if the signal is below the reject level, squash it down to 10%
        if (peak < bdChs[channel].rejectLevel) peak %= 10;

        //if the hit count for the gate is greater than zero and the signal did
        //not exceed the gate the specified number of times, squash it down
        //to 10%
        //at first glance, it would seem that a hitCount of zero would always
        //trigger the flag in the DSP, but if the signal never exceeds the
        //gate, the hitCountMet flag never gets set even if the hitCount is
        //zero, so have to catch that special case here

        if (bdChs[channel].gates[i].gateHitCount > 0 && !hitCountMet)
                                                                    peak %= 10;

        peakFlightTime =
            (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

        //the flight time value is the memory address of the peak in the DSP
        //the data buffer starts at 0x8000 in memory, so subtracting 0x8000
        //from the flight time value gives the time position in relation to the
        //time the FPGA began recording data after the hardware delay
        //NOTE: the FPGA should really subtract the 0x8000 instead of doing
        //it here!

        peakFlightTime -= 0x8000;

        peakTrack =
            (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

        //debug mks

        //debug code for testing tdc
        //if reset pulse is missed, peakTrack will be greater than 11

        debug++;

        if (peakTrack > 14)
            debug = 0;

        if (peakTrack > 22)
            debug = 0;

        //catch a particular slot and board channel
        if (slotAddr == 6 && channel == 1)
            debug = 26;

        //end debug mks

        //Add in the clock position adjustment to account for offset sensor
        //positions and timing errors.  Wraparound past the max clock position
        //gets taken care of with modulo in the next bit of code.
        peakTrack += CLOCK_OFFSET;

        //If the TDC input is missed, the counter will not be reset and will
        //continue counting past the maximum clock position.  It should still be
        //accurate, so the actual clock position can be determined by computing
        //the wrap around.

        if (peakTrack > MAX_CLOCK_POSITION)
            peakTrack = peakTrack % (MAX_CLOCK_POSITION + 1);

        bdChs[channel].gates[i].storeNewAScanPeak((int)(peak * ASCAN_SCALE),
                                                                peakFlightTime);

        peak *= SIGNAL_SCALE; //scale signal up or down

        //the peakTrack variable denotes the clock position, replace position
        //0 with 12 before saving

        int clockPos = peakTrack;
        if (clockPos == 0) clockPos = 12;

        bdChs[channel].gates[i].storeNewData(peak, 0, 0, 0,
                peakFlags, peakFlightTime, peakTrack, clockPos,
                pEncoder1, pEncoder2);

        //if the channel has been configured to modify the wall, then save
        //the data so that it can be used to modify the wall elsewhere
        if (bdChs[channel].gates[i].modifyWall){
            //store the value if it is greater than the stored peak
            if (peak > hdwVs.wallMinModifier)
                hdwVs.wallMinModifier = peak;
            }

        }// for (int i=0; i < numberOfGates; i++)

    if (bdChs[channel].isWallChannel){

        //cast to short used to force sign extension for signed values
        //some of these values may never be negative, but they are handled
        //as signed for the sake of consistency

        // Note that StartNum, StartDen, EndNum, and EndDen are no longer
        // used as the fractional math has been abandonded.
        // See Git commit tag VersionWithFractionalMathForThickness in the
        // Java and DSP code archives for version which used fractional math.

        int wallMaxPeak =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMaxStartNum =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMaxStartDen =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMaxEndNum =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMaxEndDen =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMaxTrack =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        double maxThickness = (double)wallMaxPeak;

        //if the max value returns as minimum int, then the wall reading is
        //invalid (missed interface or echo, etc.)  use the previous reading
        //instead -- if value is good then save as the previous value

        if (maxThickness == -32768)
            maxThickness = prevMaxThickness;
        else
            prevMaxThickness = maxThickness;

        //store the max peak - overwrites info saved for this gate above
        //debug mks - gates[1] should use the wallStartGate specified by user
        bdChs[channel].gates[1].storeNewDataD(maxThickness, wallMaxTrack);

        // Note that StartNum, StartDen, EndNum, and EndDen are no longer
        // used as the fractional math has been abandonded.
        // See Git commit tag VersionWithFractionalMathForThickness in the
        // Java and DSP code archives for version which used fractional math.

        int wallMinPeak =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMinStartNum =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMinStartDen =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMinEndNum =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMinEndDen =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        int wallMinTrack =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

        double minThickness = (double)wallMinPeak;

        //if the min value returns as max int, then the wall reading is
        //invalid (missed interface or echo, etc.)  use the previous reading
        //instead -- if value is good then save as the previous value

        if (minThickness == 32767)
            minThickness = prevMinThickness;
        else {
            //if the modifier value is over the threshold, then modify the
            //wall trace with it
            //NOTE: need to add the threshold to the config file so it is
            // programmable.
            if ((hdwVs.nearStartOfPiece || hdwVs.nearEndOfPiece)
                                            && hdwVs.wallMinModifier > 30) {
                minThickness -= hdwVs.wallMinModifier;
                hdwVs.wallMinModifier = Integer.MIN_VALUE;
                }
            prevMinThickness = minThickness;
            }

        //store the min peak - overwrites info saved for this gate above
        //debug mks - gates[2] should use the wallEndGate specified by user
        bdChs[channel].gates[2].storeNewDataD(minThickness, wallMinTrack);

        }// if (bdChs[channel].isWallChannel)

    }// for (int h=0; h < pNumberOfChannels; h++)

return 1;

}//end of UTBoard::processPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processPeakDataPacket
//
// Handles data from a peak data packet.  This type of packet is used to
// transfer the peak inspection data from the remote devices.
//
// Parameter pNumberOfChannels is the number of channels expected in the
// packet.
//
// Returns a number greater than zero if a packet successfully extracted.
//

public int processPeakDataPacket(int pNumberOfChannels)
{

//allow another request packet to be transmitted now that the return
//packet for the previous request has been received
peakDataRcvd = true;

int x;

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= 8) break;
        waitSleep(10);
        }
    if ((x = byteIn.available()) >= 8) byteIn.read(inBuffer, 0, 8);
    else
        return 0;
    }// try
catch(IOException e){}

x = 0;

//get the position of encoder 1
//this is the entry encoder or the carriage encoder depending on unit type
int encoder1 =
     ((inBuffer[x++]<<24) & 0xff000000) +  ((inBuffer[x++]<<16) & 0xff0000) +
     ((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

//get the position of encoder 2
//this is the entry encoder or the carriage encoder depending on unit type
int encoder2 =
      ((inBuffer[x++]<<24) & 0xff000000) + ((inBuffer[x++]<<16) & 0xff0000) +
      ((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

//extract the peak info for each gate of each channel
x = processPeakData(pNumberOfChannels, encoder1, encoder2);

//flag that a Peak Data packet has been processed and the data is ready
peakDataPacketProcessed = true;

//return value greater than zero if bytes read from socket

return(x);

}//end of UTBoard::processPeakDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processPeakDataPacketX
//
// Handles data from a peak data packet.  This type of packet is used to
// transfer the peak inspection data from the remote devices.
//
// Returns number of bytes retrieved from the socket.
//

// WIP MKS -- this function is not used -- DELETE

public int processPeakDataPacketX()
{

int x;

//the number of data bytes expected in the return packet is determined by the
//number of gates for the channel
int numberReturnBytes = bdChs[0].numberOfGates * PEAK_DATA_BYTES_PER_GATE;

//add one for the DSP core identifier at the beginning of the data packet
numberReturnBytes++;

//above uses channel 0 - need to match channel being returned
//Rabbit will return all channels in one packet - need to compute packet size
//using all channels when this is fixed.

try{
    timeOutProcess = 0;
    while(timeOutProcess++ < TIMEOUT){
        if (byteIn.available() >= numberReturnBytes) break;
        waitSleep(10);
        }
    if ((x = byteIn.available()) >= numberReturnBytes)
        byteIn.read(inBuffer, 0, numberReturnBytes);
    else
        return 0;
    }// try
catch(IOException e){}

x = 0;

int dspCoreFromPkt = (int)inBuffer[x++];

int flags1 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peak1 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakLoc1 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakTrk1 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int flags2 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peak2 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakLoc2 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakTrk2 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int flags3 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peak3 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakLoc3 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

int peakTrk3 = (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

return(numberReturnBytes); //number of bytes read from the socket

}//end of UTBoard::processPeakDataPacketX
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::installNewRabbitFirmware
//
// Transmits the Rabbit firmware image to the UT board to replace the existing
// code.
//
// See corresponding function in the parent class Board.
//

public void installNewRabbitFirmware()
{

//create an object to hold codes specific to the UT board for use by the
//firmware installer method

InstallFirmwareSettings settings = new InstallFirmwareSettings();
settings.loadFirmwareCmd = LOAD_FIRMWARE_CMD;
settings.noAction = NO_ACTION;
settings.error = ERROR;
settings.sendDataCmd = SEND_DATA_CMD;
settings.dataCmd = DATA_CMD;
settings.exitCmd = EXIT_CMD;

super.installNewRabbitFirmware("UT", "Rabbit\\CAPULIN UT BOARD.bin", settings);

}//end of Capulin1::installNewRabbitFirmware
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logStatus
//
// Writes various status messages to the log window.
//

@Override
public void logStatus(JTextArea pTextArea)
{

// if there have been socket sync errors, display message and clear count

if (reSyncCount > 0){
    log.append("----------------------------------------------\n");
    log.append("Number of reSync errors since last report: " + reSyncCount
      + "\nInfo for packet processed prior to sync error: \n"
      + "DSP Chip: " + reSyncDSPChip + " DSP Core: " + reSyncDSPCore
      + "\nPacket ID: " + reSyncPktID + " DSP Message ID: " + reSyncDSPMsgID);
    log.append("\n----------------------------------------------\n");
    }

reSyncCount = 0;

}//end of UTBoard::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

public void shutDown()
{

//shut down pulser operations by placing everything into reset
//place FPGA internals in reset (active high), DSPs in reset (active low)
resetShadow = writeFPGAReg(RESET_REG, (byte)0x01);

//close everything - the order of closing may be important

try{
    if (byteOut != null) byteOut.close();
    if (byteIn != null) byteIn.close();
    if (out != null) out.close();
    if (in != null) in.close();
    if (socket != null) socket.close();
    }
catch(IOException e){}

}//end of UTBoard::shutDown
//-----------------------------------------------------------------------------

}//end of class UTBoard
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
