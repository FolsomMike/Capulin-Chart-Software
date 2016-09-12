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
*
* Notes Regarding Collection of Map Data
*
* Signal is given for remotes to begin sending map data and for UTBoard object
* to save it.
*
* Data from the same revolution may not be stored at the same index in the
* different UTBoard object buffers as each remote may receive the command to
* begin sending at a slightly different time -- thus the data is misaligned in
* the buffers.
*
* Distance tagging and saving to the 2D Map buffer does not begin until each
* ducer reaches the edge of the test piece -- thus the data in the 2D Map is
* aligned distance wise.
*
* The Tubo program does not want the data distance aligned -- it wants the data
* sets for each ducer in a revolution group to have come from the same physical
* revolution. It aligns the data based on value saved with the data specifying
* the physical distance between each transducer.
*
* The data stored to the 2D map may be compressed or stretched and is distance
* aligned, so it cannot be used for saving the file for use by the Tubo map
* display program.
*
* When the start inspection signal is received, a flag is set for the last
* received revolution in each raw data buffer. *IF* the Ethernet transfer is
* always fast enough, all buffers may have the latest revolution -- in which
* case this signal would serve to provide an alignment signal in the unaligned
* raw data buffers.
*
* The only problem would be the rare case when the start inspection signal is
* received exactly between the time when a TDC code has been received by one or
* more UTBoard objects but not yet received for the remaining objects.
*
* Solution:
*
*  This problem could be alleviated by looking at the number of samples since
*  the last TDC for all objects -- if some have just received a TDC code, but
*  others have not, skip back past the recent ones -- this should end up marking
*  the last full revolution for all boards.
*
* The TDC codes do arrive with an incremental counter, but it cannot be trusted
* that they are not out of sync as the boards will receive the command to start
* recording data at slightly different times.
*
* Could force synchronization of counters by having Control board send a reset
* pulse once or periodically. Any missed pulses would have to be detected as
* the counters would be mismatched for each. NOTE: This could be confusing as
* the counters currently overrun to zero from the TDC pulses -- how can the
* two be differentiated?
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.Log;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.stripchart.Map2D;
import chart.mksystems.tools.SwissArmyKnife;
import java.io.*;
import java.net.*;
import javax.swing.*;

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

    public boolean recordMapDataEnabled;

    double inspectionStartLocation;
    double inspectionStopLocation;


    int packetCount = Integer.MAX_VALUE;
    int dataBufferIndex = 0;
    int dataBufferSize;
    short dataBuffer[] = null;
    int prevCtrlCodeIndex = -1;
    int mapTDCCodeIgnoreTimer = 0;
    boolean dataBufferIsEnabled = false;

    static final int MAP_TDC_IGNORE_TIMER_RESET = 50;

        //on startup, the UT boards each load a default rep rate from the
        //configuration file, but that is often overriden by the owner class
        //all boards must have the same rep rate

    int repRateInHertz, repRate;
    int triggerWidth;
    int syncWidth;
    int pulseDelay;
    int numberOfBanks;
    int reSyncCount = 0, reSyncDSPChip, reSyncDSPCore;
    int reSyncPktID, reSyncDSPMsgID;
    boolean syncSource;
    HardwareVars hdwVs;
    String jobFileFormat, mainFileFormat;

    int dspMessageSentCounter = 0, dspMessageAckCounter = 0;

    //ASCAN_MAX_HEIGHT and SIGNAL_SCALE should be changed to variables
    //which can be initialized and or adjusted
    static int ASCAN_MAX_HEIGHT = 350;

    static int MAX_CLOCK_POSITION = 11; //0 to 11 positions
    //offset gets added to clock position to account for various sensor positions
    //and errors
    static int CLOCK_OFFSET = 6;

    //WARNING:  In general it is a bad idea to use any value other than 1 for
    //        SIGNAL_SCALE.  If greater than 1, the result will look choppy.
    //        Less than 1 and the input device will require needless addition
    //        of gain to counterract the scale down.  Let the input device
    //        provide the scaling unless it absolutely cannot be done.
    //        To make the AScan signal levels match the chart, it may be
    //        necessary to scale the AScan up.  This is better than scaling the
    //        chart down which needlessly wastes gain.  In general, it is more
    //        important to have a good chart presentation than an AScale
    //        presentation as the chart is often the permanent record.

    static double SIGNAL_SCALE = 1;
    static double ASCAN_SCALE = 3.5;

    public static int ASCAN_SAMPLE_SIZE = 400;
    public static int ASCAN_BUFFER_SIZE = 400;

    AScan aScan, aScanBuffer;

    AScan [] aScanFIFO;
    static int ASCAN_FIFO_SIZE = 25; //fifo is used for smoothing - larger
                                     //number allows for more smoothing
    int aScanFIFOIndex = 0;

    boolean udpResponseFlag = false;

    int hardwareDelay;

    int aScanCoreID;
    int pktDSPChipID, pktDSPCoreID, pktID, dspMsgID, dspMsgCoreID;

    byte[] readDSPResult;
    boolean readDSPDone;

    byte[] getDSPRamChecksumResult;
    boolean getDSPRamChecksumDone;

    int aScanCoreSelector = 1;
    //set aScanRcvd flag true so first request for peakData packet will succeed
    boolean aScanRcvd = true;
    boolean aScanDataPacketProcessed = false;
    //set peakDataRcvd flag true so first request for peakData packet
    //will succeed
    boolean peakDataRcvd = true;
    boolean peakDataPacketProcessed = false;

    boolean dspStatusMessageRcvd = false;

    int dbug = 0; //debug mks - remove this

    int timeOutRead = 0; //use this one in the read functions
    int timeOutWFP = 0; //used by processDataPackets

    int getAScanTimeOut = 0, GET_ASCAN_TIMEOUT = 50;
    int getPeakDataTimeOut = 0, GET_PEAK_DATA_TIMEOUT = 50;


    double prevMinThickness, prevMaxThickness;
    double minThickTossThreshold = Double.MIN_VALUE;
    int minThickTossCount = 0;

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

        boolean active = false;
        
        byte dspChip;
        byte dspCore1;
        byte dspCore2;

        byte delayReg0, delayReg1, delayReg2, delayReg3;
        byte ducerSetupReg;
        byte countReg0, countReg1, countReg2;
        byte bufStart0, bufStart1, bufStart2;

        int numberOfGates = 0;
        UTGate[] gates;

        int aScanSmoothing = 1;
        int rejectLevel;
        boolean isWallChannel=false;

        AnalogOutputController analogOutputController;
        int analogOutputControllerChannel = -1;

        void applyWallOnAnalogOutput(double pAnalogOutput){
            if (analogOutputControllerChannel != -1){
                analogOutputController.setOutputWithMinMaxPeakHold(
                                analogOutputControllerChannel, pAnalogOutput);                
            }
        }
    }

    BoardChannel bdChs[];
    static int NUMBER_OF_BOARD_CHANNELS = 4;

    static int FALSE = 0;
    static int TRUE = 1;

    //this is the memory location in the DSP where the FPGA stuffs the raw
    //A/D data
    static int AD_RAW_DATA_BUFFER_ADDRESS = 0x4000;

    static final int MAP_ANY_FLAG =             0x0000;
    static final int MAP_CONTROL_CODE_FLAG =    0x8000;
    static final int MAP_LINEAR_ADVANCE_FLAG =  0x4000;
    static final int MAP_IGNORE_CODE_FLAG =     0x2000;
    static final int MAP_START_CODE_FLAG =      0x1000;
    static final int MAP_STOP_CODE_FLAG =       0x0800;

    static final int MAP_IGNORE_DETECTION =
                                MAP_CONTROL_CODE_FLAG | MAP_IGNORE_CODE_FLAG;

    static final int RUNTIME_PACKET_SIZE = 2048;
    static final int WALL_MAP_PACKET_DATA_SIZE = 4002;
    static final int WALL_MAP_PACKET_DATA_SIZE_INTS = 2001;

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

    // values for the board's type variable

    static int BASIC_PEAK_COLLECTOR = 1;
    static int WALL_MAPPER = 2;

    // values for the Rabbits control flag - only lower 16 bits are used
    // as the corresponding variable in the Rabbit is an unsigned int

    static final int RABBIT_FLAW_WALL_MODE = 0x0001;	//board is a basic flaw or wall module
    static final int RABBIT_WALL_MAP_MODE = 0x0002;	//board is a wall mapping module
    static final int RABBIT_SEND_DATA_ASYNC = 0x0004;    //send data packets without being requested

    // bits for flag1 variable in DSP's
    //The GATES_ENABLED, DAC_ENABLED, and ASCAN_ENABLED flags can be cleared
    //before requesting a processing time calculation to use as a baseline as
    //this results in the least amount of processing.

    static int PROCESSING_ENABLED = 0x0001; //signal sample processing enabled
    static int GATES_ENABLED = 0x0002;      //gates enabled flag
    static int DAC_ENABLED = 0x0004;        //DAC enabled flag
    static int ASCAN_FAST_ENABLED = 0x0008; //fast AScan enabled flag
    static int ASCAN_SLOW_ENABLED = 0x0010; //slow AScan enabled flag
    static int ASCAN_FREE_RUN = 0x0020;    //AScan free run, not triggered by gate
    static int DSP_FLAW_WALL_MODE = 0x0040;    //DSP is a basic flaw/wall peak processor
    static int DSP_WALL_MAP_MODE = 0x0080;     //DSP is a wall mapping processor

    //Messages for DSPs
    //These should match the values in the code for those DSPs

    static byte DSP_NULL_MSG_CMD = 0;
    static byte DSP_GET_STATUS_CMD = 1;
    static byte DSP_SET_GAIN_CMD = 2;
    static byte DSP_GET_ASCAN_BLOCK_CMD = 3;
    static byte DSP_GET_ASCAN_NEXT_BLOCK_CMD = 4;
    static byte DSP_SET_AD_SAMPLE_SIZE_CMD = 5;
    static byte DSP_SET_DELAYS	= 6;
    static byte DSP_SET_ASCAN_SCALE = 7;
    static byte DSP_SET_GATE = 8;
    static byte DSP_SET_GATE_FLAGS = 9;
    static byte DSP_SET_DAC	= 10;
    static byte DSP_SET_DAC_FLAGS = 11;
    static byte DSP_SET_HIT_MISS_COUNTS = 12;
    static byte DSP_GET_PEAK_DATA = 13;
    static byte DSP_SET_RECTIFICATION = 14;
    static byte DSP_SET_FLAGS1 = 15;
    static byte DSP_UNUSED1 = 16;
    static byte DSP_SET_GATE_SIG_PROC_TUNING = 17;
    static byte DSP_GET_MAP_BLOCK_CMD = 18;
    static byte DSP_GET_MAP_COUNT_CMD = 19;
    static byte DSP_RESET_MAPPING_CMD = 20;
    static byte DSP_SET_FILTER = 21;
    static byte DSP_SET_FILTER_ABS_PREPROCESSING = 22;
    
    static byte DSP_ACKNOWLEDGE = 127;

    static final byte DSP_NULL_CORE = 0;

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
    static byte SET_REP_RATE_CMD = 23;
    static byte SET_CONTROL_FLAGS_CMD = 24;
    static byte GET_WALL_MAP_CMD = 25;
    static byte RESET_FOR_NEXT_RUN_CMD = 26;
    static byte SET_MAPPING_CHANNEL_CMD = 27;

    static byte ERROR = 125;
    static byte DEBUG_CMD = 126;
    static byte EXIT_CMD = 127;

    //Error Codes for UT boards

    static final byte NO_ERROR = 0;
    static final byte DSP_RETURN_PKT_ILLEGAL_SIZE_ERROR = 1;
    static final byte DSP_RETURN_PKT_TIMEOUT_ERROR = 2;
    static final byte DSP_RETURN_PKT_INVALID_HEADER_ERROR = 3;

    static final int BOARD_ERROR_FLAG_BIT = 0x80;

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
    static byte GATE_EXCEEDED = 0x0004;

    //shadow registers for FPGA registers which do not already have an
    // associated variable

    public byte masterControlShadow;
    byte resetShadow;
    byte progGain12Shadow;
    byte progGain34Shadow;

    // UT Board status flag bit masks

    static byte FPGA_LOADED_FLAG = 0x01;

    //number of loops to wait for response before timeout
    static int FPGA_LOAD_TIMEOUT = 200;

    boolean reSynced;

    //Error Counters ----------------------

    //track number of "No Response to Peak Data Request" occurrances
    int noResponseToPeakDataRequestCount = 0;
    //track number of "No Response Time Out for Peak Data Request" occurrances
    int noResponseToPeakDataRequestTimeOutCount = 0;

    //track various errors
    int dspReturnPktIllegalSizeErrorCount = 0;
    int dspReturnPktTimeoutErrorCount = 0;
    int dspReturnPktInvalidHeaderErrorCount = 0;

    // track info for the last reported error

    int lastErrorCode = NO_ERROR;
    int lastErrorDSPChip = -1;
    int lastErrorCore = -1;
    int lastErrorPktID = -1;
    int lastErrorDSPMsgID = -1;

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
    boardName = pBoardName;
    boardIndex = pBoardIndex;
    simulate = pSimulate;

}//end of UTBoard::UTBoard (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::init
//
// Initializes new objects. Should be called immediately after instantiation.
//

public void init()
{

    //if the ini file cannot be loaded, continue on - values will default
    try {
        configFile = new IniFile(configFilename, jobFileFormat);
        configFile.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 531");
    }

    //FIR filter buffer -- same length as number of filter taps
    firBuf = new int[firCoef.length];

    //aScan holds an aScan data set for transfer to the display object
    aScan = new AScan(ASCAN_BUFFER_SIZE);
    //aScanBuffer holds data while it is being processed
    aScanBuffer = new AScan(ASCAN_BUFFER_SIZE);

    //aScanFIFO holds multiple data sets which can then be averaged to create
    //the data for aScan - this allows for smoothing
    aScanFIFO = new AScan[ASCAN_FIFO_SIZE];
    for (int i = 0; i < ASCAN_FIFO_SIZE; i++) {
        aScanFIFO[i] = new AScan(ASCAN_BUFFER_SIZE);
    }

    readDSPResult = new byte[512];

    getDSPRamChecksumResult = new byte [2];

    //setup information for each channel on the board
    setupBoardChannels();

    //read the configuration file and create/setup the charting/control elements
    configure(configFile);

}//end of UTBoard::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setupBoardChannels
//
// Creates and sets up an array to hold channel specific values such as
// FPGA register addresses, DSP chip and core numbers, etc.
//

private void setupBoardChannels() {

    bdChs = new BoardChannel[NUMBER_OF_BOARD_CHANNELS];

    for (int i=0; i<4; i++) {bdChs[i] = new BoardChannel();}

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

    //make connection with all the remotes
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

    if (ipAddrS == null || ipAddr == null){
        logger.logMessage("UT board #" + boardIndex + " never responded to "
                + "roll call and cannot be contacted.\n");
        return;
    }

    //see notes above regarding IP Addresses

    try {

        //displays message on bottom panel of IDE
        logger.logMessage("Connecting to UT board " + ipAddrS + "...\n");

        if (!simulate) {socket = new Socket(ipAddr, 23);}
        else {
            UTSimulator utSimulator = new UTSimulator(
                     ipAddr, 23, mainFileFormat, simulationDataSourceFilePath);
            utSimulator.init();

            socket = utSimulator;
        }



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
        logSevere(e.getMessage() + " - Error: 732");
        logger.logMessage("Unknown host: UT " + ipAddrS + ".\n");
        return;
    }
    catch (IOException e) {
        logSevere(e.getMessage() + " - Error: 737");
        logger.logMessage("Couldn't get I/O for UT " + ipAddrS + "\n");
        logger.logMessage("--" + e.getMessage() + "--\n");
        return;
    }

    try {
        //display the greeting message sent by the remote
        logger.logMessage("UT " + ipAddrS + " says " + in.readLine() + "\n");
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 748");
    }

    loadFPGA(); //send configuration file for the board's FPGA

    initFPGA(); //setup the registers in the UT board FPGA

    //ask the board for its chassis and board address switch settngs
    getChassisSlotAddressFromRemote();

    //check for an address override entry and use that if it exists
    getChassisSlotAddressOverrideFromFile();

    //NOTE: now that the chassis and slot addresses are known, display messages
    // using those to identify the board instead of the IP address so it is
    // easier to discern which board is which.

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

    //give another reset pulse to the four DSP cores -- without this, random
    //DSP cores won't start after a cold boot
    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x3c)));
    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x3c));

    //sleep for a bit to allow DSPs to start up
    waitSleep(1000);

    logAllDSPStatus();

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
// UTBoard::enableWallMapPackets
//
// If the board's type is WALL_MAPPER, the RABBIT_SEND_DATA_ASYNC flag is
// set/unset in rabbitControlFlags and the flags are sent to the remote.
//
// If set, the remote will then begin sending wall mapping packets back
// asynchronously without request from the host.
//
// If unset, the remote will not send packets.
//
// If the board's type is not WALL_MAPPER, nothing is done.
//

public void enableWallMapPackets(boolean pState)
{

    if(type != WALL_MAPPER) {return;}

    if (pState){
        rabbitControlFlags |= RABBIT_SEND_DATA_ASYNC;
    }
    else{
        rabbitControlFlags &= (~RABBIT_SEND_DATA_ASYNC);
    }

    sendRabbitControlFlags();

}//end of UTBoard::enableWallMapPackets
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::resetForNextRun
//
// Resets all buffer pointers and such in preparation for the next run.
// Sends a reset code to the remote so it can prepare as well.
//
// The remote is enabled to collect map data from the DSPs and transmit back
// to the host asynchonously -- this is done automatically in the Rabbit by the
// call to sendResetForNextRunCmd().
//
// Should be called from "Main Thread" and not the GUI thread to avoid
// collisions in accessing the socket.
//

public void resetForNextRun()
{

    recordMapDataEnabled = false;
    inspectionStartLocation = 0;
    inspectionStopLocation = 0;

    dataBufferIndex = 0;
    prevCtrlCodeIndex = -1;
    if(map2D != null) { map2D.resetAll(); }

    //no map data is stored in the buffer until enabled later
    setDataBufferIsEnabled(false);
        
    //send reset command to the remotes; also resets DSP mapping and enables
    //Rabbit to collect map data from DSP and transmit to host
    sendResetForNextRunCmd();
    
}//end of UTBoard::resetForNextRun
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

    //re-enable sampling on the UT boards so A/D data is processed
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

    int pktCounter = 0;

    // don't attempt to load the FPGA if no contact made with remote
    if (byteOut == null) {return;}

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

        sendBytes(LOAD_FPGA_CMD); //send command to initiate loading

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
                if (inCount >= 2) {
                    byteIn.read(inBuffer, 0, 2);
                }
                else{
                    waitSleep(10);
                }

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

                pktCounter++; //count packets sent

                //be sure to check bufPtr on left side or a byte will get read
                //and ignored every time bufPtr test fails
                while (bufPtr < CODE_BUFFER_SIZE && (c = inFile.read()) != -1 ){

                    //stuff the bytes into the buffer after the command byte
                    codeBuffer[bufPtr++] = (byte)c;

                    //reset timer in this loop so it only gets reset when
                    //a request has been received AND not at end of file
                    timeOutRead = 0;

                }

                if (c == -1) {fileDone = true;} //send no more packets

                //send packet to remote
                byteOut.write(codeBuffer, 0 /*offset*/, CODE_BUFFER_SIZE);

            }//if (inBuffer[0] == SEND_DATA)

            //count loops - will exit when max reached
            //this is reset whenever a packet request is received and the end of
            //file not reached - when end of file reached, loop will wait until
            //timeout reached again before exiting in order to catch
            //success/error messages from the remote

            timeOutRead++;

        }// while(timeOutGet <...

        //remote has not responded if this part reached
        logger.logMessage(
              "UT " + ipAddrS + " error loading FPGA - contact lost after " +
                pktCounter + " packets." + "\n");
    }//try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 1014");
    }
    finally {
        if (inFile != null) {try {inFile.close();} catch(IOException e){}}
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

    sendBytes(WRITE_FPGA_CMD, pAddress, pByte);

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

    // load values from the config file which can only be loaded after the
    // board's chassis and slot addresses are known

    configureExtended(configFile);

    sendRabbitControlFlags();

    sendMappingChannel();

    //destroy the configFile object to release resources
    //debug -- mks -- this was removed because the warmStart needs to reload resources
    // probably doesn't need to reload them as the originals are probably still good?
    // on warmStart check if null and use old values
    //configFile = null;

    //place the FPGA internals into reset to prevent them from starting
    //in an unknown condition after changing the sampling registers

    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x01));

    //turn off all tranducers
    sendTransducer(0, (byte)0, (byte)1, (byte)0);
    sendTransducer(1, (byte)0, (byte)1, (byte)1);
    sendTransducer(2, (byte)0, (byte)1, (byte)2);
    sendTransducer(3, (byte)0, (byte)1, (byte)3);

    sendRepRate();

    sendTriggerWidth(triggerWidth);

    sendSyncWidth(syncWidth);
    sendPulseDelay(pulseDelay);

    //number of banks to fire (use desired value - 1, 0 = 1 bank)
    writeFPGAReg(NUMBER_BANKS_REG, (byte)numberOfBanks);

    //place FPGA in run mode using A/D test data sequence
    //use 0x07 for board to be the source of the pulser sync
    //use 0x05 for board to be a receiver of the pulser sync

    //set the board up as the pulse sync source if specified
    if (syncSource) {masterControlShadow |= SYNC_SOURCE;}

    //set the board up to use real data instead of simulation data
    masterControlShadow &= (~SIM_DATA);

    //apply the settings to the FPGA register
    writeFPGAReg(MASTER_CONTROL_REG, (byte)masterControlShadow);

    //release the FPGA internals from reset
    resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x01)));

}//end of UTBoard::initialize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::warmReset
//
// Resets various board and channel settings, including FPGA register values.
//

public void warmReset()
{

    initFPGA();

    //release FPGA internals from reset (low = no reset)
    //release DSP Global reset so HPI bus can be used (high = no reset)
    //release DSPs A,B,C,D resets (low = reset)
    resetShadow = writeFPGAReg(RESET_REG, (byte)0x3e);

    //sleep for a bit to allow DSPs to start up
    waitSleep(1000);

    logDSPStatus(1, 1, true); logDSPStatus(1, 2, true);
    logDSPStatus(1, 3, true); logDSPStatus(1, 4, true);

    logDSPStatus(2, 1, true); logDSPStatus(2, 2, true);
    logDSPStatus(2, 3, true); logDSPStatus(2, 4, true);

    //enable sampling - FPGA has control of the HPI bus to transfer A/D data
    setState(0, 1);

    initialize();

}//end of UTBoard::warmReset
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getChassisSlotAddressFromRemote
//
// Retrieves the board's chassis and slot address settings.  This function
// can only be called after the board's FPGA has been loaded because the
// switches are read through the FPGA.
//
// The switches are located on the motherboard.
//

void getChassisSlotAddressFromRemote()
{

    //read the address from FPGA register connected to the switches
    byte address = getRemoteAddressedData(READ_FPGA_CMD, CHASSIS_SLOT_ADDRESS);

    //the address from the switches is inverted with the chassis address in the
    //upper nibble and the board address in the lower

    chassisAddr =  (~address>>4 & 0xf);
    slotAddr = ~address & 0xf;

    logger.logMessage("UT " + ipAddrS + " chassis & slot address: "
                                        + chassisAddr + "-" + slotAddr + "\n");

}//end of UTBoard::getChassisSlotAddressFromRemote
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getChassisSlotAddressOverrideFromFile
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

void getChassisSlotAddressOverrideFromFile()
{

    //use integers with zeroing of upper bits to represent the bytes as unsigned
    //values - without zeroing, sign extension during transfer to the int makes
    //values above 127 negative

    int byte2 = (int)(ipAddr.getAddress()[2] & 0xff);
    int byte3 = (int)(ipAddr.getAddress()[3] & 0xff);

    IniFile configFileL;

    //if the ini file cannot be opened and loaded, exit without action
    try {
        configFileL = new IniFile("Board Slot Overrides.ini", mainFileFormat);
        configFileL.init();
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 1222");
        return;
    }

    String section = byte2 + "." + byte3;

    byte chassisAddrL = (byte)configFileL.readInt(section, "Chassis", -1);

    byte slotAddrL = (byte)configFileL.readInt(section, "Slot", -1);

    //if a chassis and board address were found for this board's IP address,
    //set the board's addresses to match

    if (chassisAddrL != -1) {chassisAddr = chassisAddrL;}
    if (slotAddrL != -1) {slotAddr = slotAddrL;}

    if (chassisAddrL != -1 || slotAddrL != -1) {
        logger.logMessage("UT " + ipAddrS + " chassis & slot override: "
                                        + chassisAddr + "-" + slotAddr + "\n");
    }

}//end of UTBoard::getChassisSlotAddressOverrideFromFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getMapChannel
//
// Returns mapChannel.
//

int getMapChannel()
{

    return(mapChannel);

}//end of UTBoard::getmapChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getDataBuffer
//
// Returns a reference to dataBuffer.
//

short[] getDataBuffer()
{

    return(dataBuffer);

}//end of UTBoard::getDataBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getIndexOfLastDataPointInDataBuffer
//
// Returns the index of the last value stored in dataBuffer + 1.
//

public int getIndexOfLastDataPointInDataBuffer()
{

    return(dataBufferIndex);

}//end of UTBoard::getIndexOfLastDataPointInDataBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setIndexOfLastDataPointInDataBuffer
//
// Sets the index of the last value stored in dataBuffer + 1.
//
// pIndex should actually point to the next location after the last value. It
// will be used to store the next data point.
//

public void setIndexOfLastDataPointInDataBuffer(int pIndex)
{

    if (pIndex < 0) { pIndex = 0; }
    if (pIndex >= dataBuffer.length) { pIndex = dataBuffer.length - 1; }

    dataBufferIndex = pIndex;

}//end of UTBoard::setIndexOfLastDataPointInDataBuffer
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setDataBufferIsEnabled
//
// Sets dataBufferIsEnabled to pState.
//
// If enabled, data from received map packets is stored in the buffer.
// If disabled, the map packets are still collected from the socket, but no
// data is stored.
//

public void setDataBufferIsEnabled(boolean pState)
{

    dataBufferIsEnabled = pState;

}//end of UTBoard::setDataBufferIsEnabled
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendRabbitControlFlags
//
// Sends the rabbitControlFlags value to the remotes. These flags control
// the functionality of the remotes.
//
// Note that the value of the CMD may different for each Board subclass which
// is why each subclass calls the super method with its particular command.
//

public void sendRabbitControlFlags()
{

    super.sendRabbitControlFlags(SET_CONTROL_FLAGS_CMD);

}//end of Board::sendRabbitControlFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Board::sendMappingChannel
//
// Sends the mapping channel value to the remotes. Each remote board can have
// one mapping channel which collects extensive data and transmits it to the
// host for mapping purposes.
//

public void sendMappingChannel()
{

    sendBytes(SET_MAPPING_CHANNEL_CMD,
            (byte) ((boardChannelForMapDataSource >> 8) & 0xff),
            (byte) (boardChannelForMapDataSource & 0xff)
            );

}//end of Board::sendMappingChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendResetForNextRunCmd
//
// Sends to the remote the command to reset for the next run.
//

public void sendResetForNextRunCmd()
{

    sendBytes(RESET_FOR_NEXT_RUN_CMD, (byte) (0), (byte) (0));

}//end of UTBoard::sendResetForNextRunCmd
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendRepRate
//
// Sends the Pulser Rep Rate to the UT board.
//

void sendRepRate()
{

    //the rep rate registers should NEVER be set directly by the host
    //using the SET_REP_RATE_CMD as done here is safer as the remote can
    //check that the received value is within limits; also the chances of
    //missing a packet write for any one of the bytes is greatly reduced

    sendBytes(SET_REP_RATE_CMD,
                (byte) ((repRate >> 24) & 0xff),
                (byte) ((repRate >> 16) & 0xff),
                (byte) ((repRate >> 8) & 0xff),
                (byte) (repRate & 0xff)
                );

}//end of UTBoard::sendRepRate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setRepRateInHertz
//
// Accepts a rep rate value in Hertz, calculates related values, then stores
// them.
//
// The value is the rep rate per channel, if value is 2000, then each channel
// will be running at 2K rep rate -- it is not an overall rep rate divided by
// the number of channels.
//

void setRepRateInHertz(int pValue)
{

    //rep rate is per channel
    //multipy the rep rate by the number of banks and multiply by the clock
    //period to get the number of clock counts per pulse
    //to get counts: (2000 * number of banks) * 0.000000015
    // 0.000000015 = 15 ns
    // add one to numberOfBanks because it is zero based

    repRate = (int)(1/(repRateInHertz * (numberOfBanks+1) * 0.000000015));

    //limit to a safe value - if the rep rate is too high and the pulse width
    //too wide, the pulser circuitry will have an excessive duty cycle and burn
    //up; 4166 is twice 2Khz for 4 banks - a reasonable maximum
    // (a smaller value is a higher rep rate)
    if (repRate < 4166 || repRate > 65535 ) {repRate = 33333;}

}//end of UTBoard::setRepRateInHertz
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getRepRateInHertz
//
// Returns the UT board's pulser rep rate.
//

public int getRepRateInHertz()
{

    return (repRateInHertz);

}//end of UTBoard::getRepRateInHertz
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

    //   *  Bit 0 : 0 = transducer is inactive, 1 = transducer is active
    //   * Bits 1:3 : time slot for the transducer see Number of Time Slots
    //   * Bits 4:7 :specifies channel of pulser to be fired for this transducer

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
// For the sync and sync reset pulses, the actual width is 100 uS when
// pValue is 200, which is 200 cycles of the FPGA clock. The actual 100uS
// equates to 6,666 counts of the FPGA CLK (15nS) -- the difference is due to
// capacitance in the transistor driver circuit which widens the signal.
// This was verified with two UT boards and a Control board in the system.
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

    byte gain1, gain2;

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

    if (gain1 == 15) {gain1 |= 0xc0;}
    else if (gain1 >= 10) {gain1 |= 0x80;}
    else if (gain1 >= 5)  {gain1 |= 0x40;}
    // gains less than 6 (value of 5) have 00b compensation

    if (gain2 == 15) { gain2 |= 0xc0; }
    else if (gain2 >= 10) { gain2 |= 0x80; }
    else if (gain2 >= 5) { gain2 |= 0x40; }
    // gains less than 6 (value of 5) have 00b compensation

    sendBytes((byte)SET_HDW_GAIN_CMD, (byte)pChannel, gain1, gain2);

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

    if (pAScanSmoothing < 1) {pAScanSmoothing = 1;}
    if (pAScanSmoothing > ASCAN_FIFO_SIZE) {pAScanSmoothing = ASCAN_FIFO_SIZE;}

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

    sendBytes(WRITE_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
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

    sendBytes(WRITE_NEXT_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
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
        if (i++ == 0) {
            writeDSPRam(pDSPChip, pDSPCore, pRAMType, pPage, pAddress, pValue);
        }
        else {
            writeNextDSPRam(pDSPChip, pDSPCore, pValue);
        }

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

    if (pCount > 127) {pCount = 127;}

    //limit bytes retrieved to size of array
    //pCount is in words so multiply by 2 for the number of bytes
    if ((pCount*2) > readDSPResult.length) {pCount = readDSPResult.length / 2;}

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

    sendBytes(READ_DSP_BLOCK_CMD, (byte)pDSPChip, (byte)pDSPCore,
                (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
                (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff),
                (byte)(pCount & 0xff));

    // wait until processDSPMessage reaches the answer packet from the remote
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
// If pForceProcessDataPackets is true, then processDataPackets will be called
// to wait for a packet to be returned.  This is necessary for cases when
// another thread is not already calling processDataPackets, such as
// during startup.
//
// If pForceProcessDataPackets is false, it is assume some other thread is
// calling processDataPackets. In that case, this method will wait a bit of
// time until readDSPDone flag is set true by processDataPackets upon receipt
// of the return packet (with processDataPackets being called by some other
// thread).
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

    sendBytes(READ_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore,
                (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
                (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff));

    // wait until processDSPMessage reaches the answer packet from the remote
    // and processes it

    if (pForceProcessDataPackets){
        //force waiting for and processing of receive packets
        processDataPackets(true, TIMEOUT);
    }
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

    sendBytes(GET_DSP_RAM_BLOCK_CHECKSUM, (byte)pDSPChip, (byte)pDSPCore,
                (byte)(pPage |= ((pRAMType == 1) ? 0x10 : 0x00)),
                (byte)((pAddress >> 8) & 0xff),(byte)(pAddress & 0xff),
                (byte)((pBlockSize >> 8) & 0xff),(byte)(pBlockSize & 0xff)
                );

    // wait until processDSPMessage reaches the answer packet from the remote
    // and processes it

    if (pForceProcessDataPackets){
        //force waiting for and processing of receive packets
        //use a time out number large enough to give the remote time to scan the
        //block of DSP ram
        processDataPackets(true, 500);
    }
    else {
        timeOutRead = 0;
        while(!getDSPRamChecksumDone && timeOutRead++ < TIMEOUT){waitSleep(10);}
    }

    //transfer the stored result and return to caller
    //this is not a signed value, so the sign will not be extended in the result
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

    sendBytes(READ_NEXT_DSP_CMD, (byte)pDSPChip, (byte)pDSPCore);

    // wait until processDSPMessage reaches the answer packet from the remote
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
// core C HPI bus.  Loading into cores A & C makes the code also available to
// cores B & D since cores A & B / C & D share the same program memory.
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
    if (pDSPCore == 1) {core = "A & B";}
    else if (pDSPCore == 3) {core = "C & D";}
    else {core = "";}

    logger.logMessage("UT " + chassisSlotAddr + " loading DSP code for" + "\n"
            + "    Chip " + pDSPChip + " Cores " + core + "\n");

    try {
        inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

        int c;
        int n3, n2, n1, n0;
        int address = 0, value = 0, place = 3;

        while ((c = inputStream.read()) != -1) {

            if (c == 2) {continue;} //catch ctrl-B file start marker - skip it

            if (c == 3) {break;} //catch the ctrl-C file end marker - exit

            if (c == '\r' || c == '\n' || c == ',' || c == ' ') {continue;}

            // catch new address flag
            if (c == '$') {

                inputStream.read(); //read and ignore the 'A'

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

            }
            else{

                //if this part reached, must be a digit for a character so add
                //it into value, using the place count to specify the shift

                c = fromHex(c);

                if (place == 3) {value += (int)((c<<12) & 0xf000);}
                else if (place == 2) {value += (int)((c<<8)  & 0xf00);}
                else if (place == 1) {value += (int)((c<<4) & 0xf0);}
                else{

                    //fourth character converted, add it in and write word

                    value += (int)(c & 0xf);

                    //write to shared program memory page 0
                    writeDSPRam(pDSPChip, pDSPCore, 1, 0, address++, value);

                    //request and wait for a status flag ever so often to make
                    //sure the remote's buffer is not overrun
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

                }

        }//while ((c =
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 1960");
        logger.logMessage(
                    "Error opening DSP code file " + dspCodeFilename + "\n");

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
    if (pDSPCore == 1) {
        core = "A & B";
    }else if (pDSPCore == 3) {
        core = "C & D";
    }else {
        core = "";
    }

    logger.logMessage("UT " + chassisSlotAddr + " verifying DSP code for" + "\n"
    + "    Chip " + pDSPChip + " Cores " + core + "\n");

    byte[] buffer = new byte[2];

    try {
        inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

        int c;
        int n3, n2, n1, n0;
        int address = 0, value = 0, place = 3;

        while ((c = inputStream.read()) != -1) {

            if (c == 2) {continue;} //catch ctrl-B file start marker - skip it

            if (c == 3) {break;} //catch the ctrl-C file end marker - exit

            if (c == '\r' || c == '\n' || c == ',' || c == ' ') {continue;}

            // catch new address flag
            if (c == '$') {

                inputStream.read(); //read and ignore the 'A'

                //read next four characters to create new address value
                n3 = fromHex(inputStream.read());
                n2 = fromHex(inputStream.read());
                n1 = fromHex(inputStream.read());
                n0 = fromHex(inputStream.read());

                //each time the address is changed, verify the checksum for the
                //previous block by comparing with the checksum from the remote

                //request the checksum for the current block as stored in the
                //DSP skip if the block size is 0
                if (byteCount > 0){

                    checksum &= 0xffff; //only use lower word of checksum

                    int remoteChecksum = getDSPRamChecksum(pDSPChip, pDSPCore,
                                                1, 0, address, byteCount, true);

                    //compare the local and remote checksums
                    if (checksum != remoteChecksum){
                        logger.logMessage(
                            "UT " + chassisSlotAddr + " DSP code error"
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

            }
            else{

                //if this part reached, must be a digit for a character so add
                //it into value, using the place count to specify the shift

                c = fromHex(c);

                if (place == 3) {value += (int)((c<<12) & 0xf000);}
                else if (place == 2) {value += (int)((c<<8)  & 0xf00);}
                else if (place == 1) {value += (int)((c<<4) & 0xf0);}
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

            }
        }//while ((c =

        //validate the last block -- when the last byte is read from the code
        //file, the above loop will exit with a possible partial block -- check
        //that last block here
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
        logSevere(e.getMessage() + " - Error: 2126");
        logger.logMessage(
                    "Error opening DSP code file " + dspCodeFilename + "\n");
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
    if (pDSPCore == 1) {core = "A & B";}
    else if (pDSPCore == 3) {core = "C & D";}
    else {core = "";}

    logger.logMessage("UT " + chassisSlotAddr + " verifying DSP code for" + "\n"
            + "    Chip " + pDSPChip + " Cores " + core + "\n");

    byte[] buffer = new byte[2];

    try {
        inputStream = new FileInputStream("DSP\\" + dspCodeFilename);

        int c;
        int n3, n2, n1, n0;
        int address = 0, value = 0, place = 3;

        while ((c = inputStream.read()) != -1) {

            if (c == 2) {continue;} //catch ctrl-B file start marker - skip it

            if (c == 3) {break;} //catch the ctrl-C file end marker - exit

            if (c == '\r' || c == '\n' || c == ',' || c == ' ') {continue;}

            // catch new address flag
            if (c == '$') {

                inputStream.read(); //read and ignore the 'A'

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

            }
            else{

                //if this part reached, must be a digit for a character so add
                //it into value, using the place count to specify the shift

                c = fromHex(c);

                if (place == 3) {value += (int)((c<<12) & 0xf000);}
                else if (place == 2) {value += (int)((c<<8)  & 0xf00);}
                else if (place == 1) {value += (int)((c<<4) & 0xf0);}
                else{

                    //fourth character converted, add it in and write word

                    value += (int)(c & 0xf);

                    //read from shared program memory page 0
                    readRAMDSP(
                            pDSPChip, pDSPCore, 1, 0, address++, buffer, true);

                    int memValue = (int)((buffer[0]<<8) & 0xff00)
                                                     + (int)(buffer[1] & 0xff);

                    //compare the value read from memory with the code from file
                    if (value != memValue) {
                        logger.logMessage(
                         "UT " + chassisSlotAddr + " DSP code error"
                         + "\n" + "    Chip " + pDSPChip + " Cores " + core
                         + "  Address: " + (address-1) + "\n");
                    }

                    //request and wait for a status flag ever so often to make
                    //sure the remote's buffer is not overrun
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

            }
        }//while ((c =
    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 2259");
        logger.logMessage(
                    "Error opening DSP code file " + dspCodeFilename + "\n");

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

    if (pChar == '0') {return 0;}
    else
    if (pChar == '1') {return 1;}
    else
    if (pChar == '2') {return 2;}
    else
    if (pChar == '3') {return 3;}
    else
    if (pChar == '4') {return 4;}
    else
    if (pChar == '5') {return 5;}
    else
    if (pChar == '6') {return 6;}
    else
    if (pChar == '7') {return 7;}
    else
    if (pChar == '8') {return 8;}
    else
    if (pChar == '9') {return 9;}
    else
    if (pChar == 'a' || pChar == 'A') {return 10;}
    else
    if (pChar == 'b' || pChar == 'B') {return 11;}
    else
    if (pChar == 'c' || pChar == 'C') {return 12;}
    else
    if (pChar == 'd' || pChar == 'D') {return 13;}
    else
    if (pChar == 'e' || pChar == 'E') {return 14;}
    else
    if (pChar == 'f' || pChar == 'F') {return 15;}
    else
        {return 0;}

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
    if (pCount % 2 != 0) {pCount++;}

    writeFPGAReg(bdChs[pChannel].countReg2, (byte) ((pCount >> 16) & 0xff));
    writeFPGAReg(bdChs[pChannel].countReg1, (byte) ((pCount >> 8) & 0xff));
    writeFPGAReg(bdChs[pChannel].countReg0, (byte) (pCount & 0xff));

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
    if (pScale < 1) {pScale = 1;}

    // calculate number of output samples to process for a desired number of
    // input samples to be processed -- rounding off is fine
    // for example, if pScale is two then 25 output samples should be processed
    // to process 50 input samples -- then divide by two again because the DSP
    // processes twice as many input samples to accommodate the min and max
    // peask recorded instead of just one peak
    // since the value is used as a loop counter, adjust down by subtracting 1
    int batchSize = (50 / pScale / 2) - 1;

    if (batchSize < 1) {batchSize = 1;}

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

}//end of UTBoard::sendGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendGateSigProcTuningValues
//
// Sends the signal processing tuning values for pGate of pChannel.
//

public void sendGateSigProcTuningValues(int pChannel, int pGate,
                                        int pValue1, int pValue2, int pValue3)
{

    sendChannelParam(pChannel, (byte) DSP_SET_GATE_SIG_PROC_TUNING,
               (byte)pGate,
               (byte)((pValue1 >> 8) & 0xff),
               (byte)(pValue1 & 0xff),
               (byte)((pValue2 >> 8) & 0xff),
               (byte)(pValue2 & 0xff),
               (byte)((pValue3 >> 8) & 0xff),
               (byte)(pValue3 & 0xff),
               (byte)0, (byte)0);

}//end of UTBoard::sendGateSigProcTuningValues
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
    if (pWidth < 1) {pWidth = 1;}

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

}//end of UTBoard::sendDACGateFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDSPMapResetCommand
//
// Sends the command to reset the mapping variables to the DSP cores
// associated with the mapping board channel.
//

void sendDSPMapResetCommand()
{

    sendChannelParam(boardChannelForMapDataSource, (byte) DSP_RESET_MAPPING_CMD,
               (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
               (byte) 0, (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendDSPMapResetCommand
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setAnalogOutputController
//
// Sets the analog output controller and channel for the board channel
// pChannel.
//
// The values reported for this channel will be output on the device.
//

public void setAnalogOutputController(int pChannel,
                                AnalogOutputController pAnalogOutputController,
                                int pOutputChannel)
{

    bdChs[pChannel].analogOutputController = pAnalogOutputController;
    
    bdChs[pChannel].analogOutputControllerChannel = pOutputChannel;
    
}//end of UTBoard::setAnalogOutputController
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setBoardChannelActive
//
// Sets flag in a board channel which activates it for use.
//

public void setBoardChannelActive(int pChannel, boolean pValue)
{

    bdChs[pChannel].active = pValue;

}//end of UTBoard::setBoardChannelActive
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setWallChannelFlag
//
// Sets the flag which specifies the channel as being used for wall
// measurement.  Extra data will then be expected to be appended to peak data
// packets sent by the remote devices.
//

public void setWallChannelFlag(int pChannel, boolean pIsWallChannel)
{

    bdChs[pChannel].isWallChannel = pIsWallChannel;

}//end of UTBoard::setWallChannelFlag
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

public void linkGates(int pChannel, UTGate[] pGates, int pNumberOfGates)
{

    bdChs[pChannel].numberOfGates = pNumberOfGates;

    bdChs[pChannel].gates = pGates;

}//end of UTBoard::linkGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::setMap2D
//
// Sets pointer to a Map2D object in which the board can send mapping data.
//

@Override
public void setMap2D(Map2D pMap2D)
{

    super.setMap2D(pMap2D);

    // hue base of 0.0 is Red
    // hue base of .6666666667 is Blue

    //convert nominal wall to sample periods as the round trip sample period
    //value is stored in the buffer
    double nominalWallTOF =
                hdwVs.nominalWall / (hdwVs.nSPerDataPoint * hdwVs.velocityNS) *
                (hdwVs.numberOfMultiples * 2);

    double baseWall = nominalWallTOF - (nominalWallTOF * .20);
    double topWall = nominalWallTOF + (nominalWallTOF * .20);
    double rangeWall = topWall - baseWall;

    map2D.setColorMapper(new WallMap2DColorMapper(
                (int)baseWall, (int)rangeWall,
                (float)0.0, (float)0.5,
                (float)1.0, (float)1.0, (float)1.0, false));

}//end of UTBoard::setMap2D
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendMode
//
// Sends the signal mode as one of the following:
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
// UTBoard::sendFilterABSPreProcessingMode
//
// Sends the on/off state for absolute-value processing of raw samples before
// being digitally filtered:
//
// 0 = no processing performed
// 1 = absolute value performed
//
// If no filter is active, this setting has no effect.
//
// Note: this setting is also sent with the filter data using the sendFilter
// function, so sendFilterABSPreProcessingMode may actually never be called.
//

void sendFilterABSPreProcessingMode(int pChannel, int pMode)
{

    sendChannelParam(pChannel, (byte) DSP_SET_FILTER_ABS_PREPROCESSING,
                (byte)(pMode & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
               (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendFilterABSPreProcessingMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendFilter
//
// Sends the signal filter values to the DSP. The filter parameters and the
// coefficients are sent using several packets.
//
// The first byte after the command byte describes the data contained in the
// packet (iterated by groupNum variable):
//
//  00: the packet contains the number of filter coefficients, the bit shift
//       amount for convolution, and the preprocessing mode
//  01: the following 8 bytes contain the first set of four coefficient words
//  02: the following 8 bytes contain the second set of four coefficient words
//  03: the following 8 bytes contain the third set of four coefficient words
//  04: the following 8 bytes contain the fourth set of four coefficient words
//  05: the following 8 bytes contain the fifth set of four coefficient words
//  06: the following 8 bytes contain the sixth set of four coefficient words
//  07: the following 8 bytes contain the seventh set of four coefficient words
//  08: the following 8 bytes contain the eighth set of four coefficient words
//
// The number of FIR coefficients is always odd, so one or more of the words
// in the last set sent will be ignored. The DSP uses the number of coefficients
// specified using the 00 descriptor. It is not necessary to send 8 sets if
// there are fewer coefficients.
//
// During the FIR filter convolution performed in the DSP, it is often necessary
// to right shift after each pass to reduce the gain.
//
// The number of coefficients should not be more than 31.
// The number of bits to shift should be in the range –16 <= ASM <= 15.
//
// If the filter array is empty (a single element set to zero), the array size
// zero will be sent with a zero bit shift value. No coefficients will be sent.
//

void sendFilter(int pChannel, int[]pFilter)
{

    byte groupNum = 0; int numBitsShift = 0; int preProcessMode = 0;
  
    //only extract from filter if it is not length of 1 (empty filter)
    
    if (pFilter.length > 1){
        numBitsShift = pFilter[1];
        preProcessMode = pFilter[2];
    }
    
    //send the number of coefficients (first value in array), the convolution
    //bit shift amount (second value in array), and the preprocessing mode
    //(third value in array)
    
    sendChannelParam(pChannel, (byte) DSP_SET_FILTER,
               (byte)groupNum, (byte)pFilter[0], (byte)numBitsShift,
               (byte)preProcessMode, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);
    
    groupNum++;
    
    //send the coefficients four at a time using multiple calls
    
    int i = 2; int length = pFilter.length;
    int val1, val2, val3, val4;
    
    while(i < length){
    
        val1=val2=val3=val4=0;
        
        if(i<length){ val1 = pFilter[i++];}
        if(i<length){ val2 = pFilter[i++];}
        if(i<length){ val3 = pFilter[i++];}
        if(i<length){ val4 = pFilter[i++];}
        
        sendChannelParam(pChannel, (byte) DSP_SET_FILTER,
            (byte)groupNum,
            (byte)((val1 >> 8) & 0xff), (byte)(val1 & 0xff),
            (byte)((val2 >> 8) & 0xff), (byte)(val2 & 0xff),
            (byte)((val3 >> 8) & 0xff), (byte)(val3 & 0xff),
            (byte)((val4 >> 8) & 0xff), (byte)(val4 & 0xff)
            );
    
        groupNum++;
    
    }

}//end of UTBoard::sendFilter
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
        //FPGA also called the Setup flag which chooses between Rabbit and host
        //PC control of the HPI interfaces or FPGA control which enables samples
        //to be stored in the DSP

        return(masterControlShadow & SETUP_RUN_MODE);

    }

    //return the DSP's enabled and running flag
    if (pWhich == 1) {return(resetShadow & 0x3c);}

    //return the test/actual data flag
    if (pWhich == 2) {return(masterControlShadow & SIM_DATA);}

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
        //FPGA also called the Setup flag which chooses between Rabbit and host
        //PC control of the HPI interfaces or FPGA control which enables samples
        //to be stored in the DSP

        if (pValue == FALSE){
            //clear the flag (setup mode)
            masterControlShadow &= (~SETUP_RUN_MODE);
        }
        else{

            prepareHPIARegistersForFPGAToDSPCommunications();

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
        if (pValue == FALSE) {
            resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x3c)));
        }
        else {
            resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x3c));
        }
    }

    if (pWhich == ENABLE_TEST_DATA){
        if (pValue == FALSE) {
            masterControlShadow = writeFPGAReg(
                MASTER_CONTROL_REG, (byte)(masterControlShadow & (~SIM_DATA)));
        }
        else {
            masterControlShadow = writeFPGAReg(
                   MASTER_CONTROL_REG, (byte)(masterControlShadow | SIM_DATA));
        }
    }

    if (pWhich == ENABLE_FPGA_INTERNALS){
        if (pValue == FALSE) {
            resetShadow = writeFPGAReg(RESET_REG, (byte)(resetShadow | 0x01));
        }
        else {
            resetShadow =
                        writeFPGAReg(RESET_REG, (byte)(resetShadow & (~0x01)));
        }
    }

}//end of UTBoard::setState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::prepareHPIARegistersForFPGAToDSPCommunications
//
// If entering run mode in which the FPGA has control of the DSP HPIA bus,
// always read a word from the DSP RAM page where the A/D values are to be
// stored by the FPGA.  The FPGA does not set the upper bits of the HPIA
// register - reading from the desired page here first will set the bits
// properly.  This must be done for each DSP core.
//
// All calls to readRAMDSP are made with the pForceProcessDataPackets parameter
// set true so the method will wait for the return packet and clear it from
// the buffer even though it is not used.
//

public void prepareHPIARegistersForFPGAToDSPCommunications()
{

    byte[] buffer = new byte[2];

    //read from local data memory page 0 for all cores

    readRAMDSP(1, 1, 0, 0, 0000, buffer, true); //DSP1, Core A
    readRAMDSP(1, 2, 0, 0, 0000, buffer, true); //DSP1, Core B
    readRAMDSP(1, 3, 0, 0, 0000, buffer, true); //DSP1, Core C
    readRAMDSP(1, 4, 0, 0, 0000, buffer, true); //DSP1, Core D

    readRAMDSP(2, 1, 0, 0, 0000, buffer, true); //DSP2, Core A
    readRAMDSP(2, 2, 0, 0, 0000, buffer, true); //DSP2, Core B
    readRAMDSP(2, 3, 0, 0, 0000, buffer, true); //DSP2, Core C
    readRAMDSP(2, 4, 0, 0, 0000, buffer, true); //DSP2, Core D

}//end of UTBoard::prepareHPIARegistersForFPGAToDSPCommunications
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

}//end of UTBoard::sendSoftwareGain
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendDSPControlFlags
//
// Sends the dspControlFlags to the DSP.
//

public void sendDSPControlFlags(int pChannel, int pFlags)
{

    sendChannelParam(pChannel, (byte) DSP_SET_FLAGS1,
               (byte)((pFlags >> 8) & 0xff),
               (byte)(pFlags & 0xff),
               (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0);

}//end of UTBoard::sendDSPControlFlags
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logAllDSPStatus
//
// Retrieves and displays the status for all DSP cores on the board.
//

public void logAllDSPStatus()
{

    logDSPStatus(1, 1, true); logDSPStatus(1, 2, true);
    logDSPStatus(1, 3, true); logDSPStatus(1, 4, true);

    logDSPStatus(2, 1, true); logDSPStatus(2, 2, true);
    logDSPStatus(2, 3, true); //logDSPStatus(2, 4, true);

}//end of UTBoard::logAllDSPStatus
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
    if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) {
        return;
    }

    if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) {
        return;
    }

    if (logDSPStatusHelper(pDSPChip, pDSPCore, pForceProcessDataPackets)) {
        return;
    }

}//end of UTBoard::logDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logDSPStatusHelper
//
// Requests and writes to the log window the status word from the specified
// DSP chip and Core.
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

    return (waitForPacketThenLogMessage(pDSPChip, pDSPCore,
                                        "Status", pForceProcessDataPackets));

}//end of UTBoard::logDSPStatusHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logDSPMapBufferHelper
//
// Requests and writes to the log window the number of bytes available for
// extraction from the specified DSP chip and Core..
//
// If pForceProcessDataPackets is true, the processDataPackets function will
// be called.  This is for use when that function is not already being called
// by another thread.
//
// Returns true if the proper response is received, false otherwise.
//

public boolean logDSPMapBufferHelper(int pDSPChip, int pDSPCore,
                                              boolean pForceProcessDataPackets)
{

    getMapBufferWordsAvailableCount(pDSPChip, pDSPCore);

    return (waitForPacketThenLogMessage(pDSPChip, pDSPCore,
                            "Bytes in Map Buffer", pForceProcessDataPackets));

}//end of UTBoard::logDSPMapBufferHelper
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::waitForPacketThenLogMessage
//
// Waits for the next packet to be received from the remotes and then prints
// a message with the phrase pMessagePhrase and the value in the returned
// packet. It is assumed that the only packet expected to be returned matches
// the last request, so this method is only useful when a specific packet has
// been requested and no other packets are being returned.
//
// If pForceProcessDataPackets is true, the processDataPackets function will
// be called.  This is for use when that function is not already being called
// by another thread.
//
// Returns true if the proper response is received, false otherwise.
//

public boolean waitForPacketThenLogMessage(int pDSPChip, int pDSPCore,
                    String pMessagePhrase, boolean pForceProcessDataPackets)
{

    int bytesRead = 0;

    int wait = 0;

    //force waiting for and processing of receive packets
    //loop until a dsp status message received
    if (pForceProcessDataPackets){
        while (bytesRead == 0 && wait++ < 100) { //debug  mks - was 10
            bytesRead = processDataPackets(true, TIMEOUT);
        }
    }

    //if bytesRead is > 0, a packet was processed and data will be in inBuffer
    if (bytesRead > 0){

        int value =
                  (int)((inBuffer[0]<<8) & 0xff00) + (int)(inBuffer[1] & 0xff);

        //displays status code in log window
        logger.logMessage("UT " + chassisSlotAddr + " Chip: " + pDSPChip
          + " Core: " + pDSPCore + " " + pMessagePhrase + " "
          + dspMsgID + "-" + toHexString(value) + "\n");
        return(true);

    }
    else{

        logger.logMessage("UT " + chassisSlotAddr + " Chip: " + pDSPChip
                    + " Core: " + pDSPCore + " Packet Error" + "\n");
        return(false);

    }

}//end of UTBoard::waitForPacketThenLogMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::sendMessageToDSP
//
// Sends a message to a DSP core via the Rabbit on the board. Counts the number
// of messages sent to be compared later with the number of DSP acknowledments
// received.
//
// Can actually send anything to the Rabbit, but specifically meant to send
// a DSP message as those are counted here for later comparison with the
// number of acknowledgments received.
//

public void sendMessageToDSP(byte... pBytes)
{

    sendBytes(pBytes);

    dspMessageSentCounter++;

}//end of UTBoard::sendMessageToDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::clearDSPMessageAndAckCounters
//
// Zeroes the counters used to track messages sent to DSPs and the ACK
// packets received back.
//

public void clearDSPMessageAndAckCounters()
{

    dspMessageSentCounter = 0; dspMessageAckCounter = 0;

}//end of UTBoard::clearDSPMessageAndAckCounters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::checkDSPAckCountVsMessageCount
//
// Compares the number of DSP messages sent to the number of DSP ack packets
// received and returns true if they match and false if not.
//

public boolean checkDSPAckCountVsMessageCount()
{

    return(dspMessageSentCounter == dspMessageAckCounter);

}//end of UTBoard::checkDSPAckCountVsMessageCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::compareDSPAckCountToMessageCount
//
// Compares the number of DSP messages to the number of DSP ack packets
// received and logs an error if they differ. Resets both counts.
//

public void compareDSPAckCountToMessageCount()
{

    if(dspMessageSentCounter != dspMessageAckCounter) {
        logger.logMessage(
              "UT " + chassisSlotAddr + " ~ " + ipAddrS + " has sent "
            + dspMessageSentCounter + " DSP messages but has received " +
              dspMessageAckCounter + " ACK packets.\n");
    }

    dspMessageSentCounter = 0; dspMessageAckCounter = 0;

}//end of UTBoard::compareDSPAckCountToMessageCount
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::readDSPStatus
//
// Requests the status word from the specified DSP chip and Core.  The
// returned packet will be handled by processDataPackets.
//

public void readDSPStatus(int pDSPChip, int pDSPCore)
{

    sendMessageToDSP(MESSAGE_DSP_CMD,
           (byte)pDSPChip, (byte)pDSPCore,
           (byte) DSP_GET_STATUS_CMD, // DSP message type id
           (byte) 2, //return packet size expected
           (byte) 9, //data packet size sent (DSP always expects 9)
           (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 , (byte) 0,
           (byte) 0, (byte)0, (byte)0
           );

}//end of UTBoard::readDSPStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getMapBufferWordsAvailableCount
//
// Requests the number of words available for extraction in the map buffer
// from the specified DSP chip and Core.  The returned packet will be handled
// by processDataPackets.
//

public void getMapBufferWordsAvailableCount(int pDSPChip, int pDSPCore)
{

    sendMessageToDSP(MESSAGE_DSP_CMD,
           (byte)pDSPChip, (byte)pDSPCore,
           (byte) DSP_GET_MAP_COUNT_CMD, // DSP message type id
           (byte) 2, //return packet size expected
           (byte) 9, //data packet size sent (DSP always expects 9)
           (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 , (byte) 0,
           (byte) 0, (byte)0, (byte)0
           );

}//end of UTBoard::getMapBufferWordsAvailableCount
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

    sendMessageToDSP(MESSAGE_DSP_CMD, bdChs[pChannel].dspChip,
           bdChs[pChannel].dspCore1,
           pMsgID, // DSP message type id
           (byte) 1, //return packet size expected (expects an ACK packet)
           (byte) 9, //data packet size sent
           pByte1, pByte2, pByte3, pByte4,
           pByte5, pByte6, pByte7, pByte8, pByte9
           );

    // 2nd Core handling the channel

    sendMessageToDSP(MESSAGE_DSP_CMD, bdChs[pChannel].dspChip,
               bdChs[pChannel].dspCore2,
               pMsgID, // DSP message type id
               (byte) 1, //return packet size expected (expects an ACK packet)
               (byte) 9, //data packet size sent
               pByte1, pByte2, pByte3, pByte4,
               pByte5, pByte6, pByte7, pByte8, pByte9
               );

}//end of UTBoard::sendChannelParam
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

}//end of UTBoard::sendDSPSampleSize
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::requestAScan
//
// Requests an aScan packet for the board analog channel specified by
// pChannel.  When the packet is returned, it will be processed by a different
// section of code.
//
// The DSP's have multiple AScan modes -- Free Run and Triggered.
//
// Free Run Mode:
//
// Upon receiving a request for an AScan, the DSP immediately returns the
// AScan data set created after the last request.  The DSP then creates a new
// data set from whatever data is in the sample buffer to be ready for the next
// AScan request.  Thus the display always reflects the data stored after the
// previous request, but this is not obvious to the user.
//
// When viewing brief signal indications, the signal will not be very clear as
// the indications will only occasionally flash into view when they happen to
// coincide with the time of request.
//
// Triggered:
//
// The DSP will only create and store an AScan packet when any gate flagged as
// a trigger gate is exceeded by the signal.  An AScan request is answered with
// the latest packet created.  This allows the user to adjust the gate such that
// only the signal of interest triggers a save, thus making sure that that
// signal is clearly captured and displayed.
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

    //if a request is not still pending, send request for new aScan data packet
    if (aScanRcvd){

        //each channel is handled by two cores processing alternating shots
        //alternate between the two cores to verify that both are working
        if (aScanCoreSelector == 1) {aScanCoreSelector = 2;}
        else {aScanCoreSelector = 1;}

        sendBytes(GET_ASCAN_CMD, bdChs[pChannel].dspChip,
            aScanCoreSelector == 1
                    ? bdChs[pChannel].dspCore1 : bdChs[pChannel].dspCore2,
            (byte) pChannel);

        // block further AScan requests until processDSPMessage processes the
        // answer packet from the previous request
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
    else {
        return(null);
    }

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

    //if a request is not still pending, send request for new peak data packet
    if (peakDataRcvd){

        // if the channel is setup for wall data, last byte sent is set to 1

        sendBytes(GET_PEAK_DATA_CMD,
                (byte)pChannel, (byte)bdChs[pChannel].numberOfGates,
                (byte)(bdChs[pChannel].isWallChannel ? 1:0));

        getPeakDataTimeOut = 0; //restart timeout

    }
    else {
        noResponseToPeakDataRequestCount++; //track non-response count
        // if the packet does not get an answer, after repeated calls to this
        // method set the flag to allow a new call on the next time through
        if (getPeakDataTimeOut++ == GET_PEAK_DATA_TIMEOUT) {
            getPeakDataTimeOut = 0; peakDataRcvd = true;
            noResponseToPeakDataRequestTimeOutCount++; //track timeouts
            return;
        }
    }

    // block further requests until processDSPMessage processes the answer
    //packet from the previous request
    peakDataRcvd = false;

}//end of UTBoard::requestPeakData
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
// Any board channel which is inactive will be sent as channel number 255
// (-1) so that the Rabbits will not attempt to retrieve data from them.
//

public void requestPeakData4(int pChannel0, int pChannel1, int pChannel2,
                                                                int pChannel3)
{

    //if a request is not still pending, send request for new peak data packet
    if (peakDataRcvd){
        
        int ch0, ch1, ch2, ch3;
        
        //set any inactive channels to -1
        if (bdChs[pChannel0].active){ ch0 = pChannel0;} else {ch0 = 255; }
        if (bdChs[pChannel1].active){ ch1 = pChannel1;} else {ch1 = 255; }
        if (bdChs[pChannel2].active){ ch2 = pChannel2;} else {ch2 = 255; }
        if (bdChs[pChannel3].active){ ch3 = pChannel3;} else {ch3 = 255; }

        // for any channel setup for wall data, set corresponding bit to 1
        // this byte is then sent with the request

        byte wallFlags = 0;

        if(bdChs[pChannel0].isWallChannel) {wallFlags |= 1;}
        if(bdChs[pChannel1].isWallChannel) {wallFlags |= 2;}
        if(bdChs[pChannel2].isWallChannel) {wallFlags |= 4;}
        if(bdChs[pChannel3].isWallChannel) {wallFlags |= 8;}

        sendBytes(GET_PEAK_DATA4_CMD,
                (byte)ch0, (byte)bdChs[pChannel0].numberOfGates,
                (byte)ch1, (byte)bdChs[pChannel1].numberOfGates,
                (byte)ch2, (byte)bdChs[pChannel2].numberOfGates,
                (byte)ch3, (byte)bdChs[pChannel3].numberOfGates,
                wallFlags
                );

        getPeakDataTimeOut = 0; //restart timeout

    }
    else {
        noResponseToPeakDataRequestCount++; //track non-response count
        // if the packet does not get an answer, after repeated calls to this
        // method set the flag to allow a new call on the next time through
        if (getPeakDataTimeOut++ == GET_PEAK_DATA_TIMEOUT) {
            getPeakDataTimeOut = 0; peakDataRcvd = true;
            noResponseToPeakDataRequestTimeOutCount++; //track timeouts
            return;
        }
    }

    // block further requests until processDSPMessage processes the answer
    //packet from the previous request
    peakDataRcvd = false;

}//end of UTBoard::requestPeakData4
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

    //the number of data bytes expected in the return packet is determined by
    //the number of gates for the channel
    int numberReturnBytes =
        bdChs[pChannel].numberOfGates * PEAK_DATA_BYTES_PER_GATE;

    sendMessageToDSP(MESSAGE_DSP_CMD,
           (byte)bdChs[pChannel].dspChip,
           (byte)bdChs[pChannel].dspCore1,
           (byte) DSP_GET_PEAK_DATA, // DSP message type id
           (byte) numberReturnBytes, //return packet size expected
           (byte) 9, //data packet size sent (DSP always expects 9)
           (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 , (byte) 0,
           (byte) 0, (byte)0, (byte)0
           );

}//end of UTBoard::getPeakDataFromDSP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::driveSimulation
//
// Drive any simulation functions if they are active.  This function is usually
// called from a thread.
//

public void driveSimulation()
{

    if (simulate && socket != null) {
        ((UTSimulator)socket).processDataPackets(false);
    }

}//end of UTBoard::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::configure
//
// Loads configuration settings from the configuration.ini file.
// The various child objects are then created as specified by the config data.
//

@Override
void configure(IniFile pConfigFile)
{

    super.configure(pConfigFile);

    simulationDataSourceFilePath = SwissArmyKnife.formatPath(
       pConfigFile.readString("Hardware", 
         "Simulation Data Source File Path", 
         "Simulation Data Source Files" + File.separator + 
         "Random Simulation for Basic 1-10 Board System Without Wall Mapping"));
    
    fpgaCodeFilename = pConfigFile.readString(
                        "Hardware", "UT FPGA Code Filename", "not specified");

    dspCodeFilename = pConfigFile.readString(
                        "Hardware", "UT DSP Code Filename", "not specified");

    inBuffer = new byte[RUNTIME_PACKET_SIZE];
    outBuffer = new byte[RUNTIME_PACKET_SIZE];

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

@Override
void configureExtended(IniFile pConfigFile)
{

    super.configureExtended(pConfigFile);

    String value;

    String section = "UT Board in Chassis " + chassisAddr + " Slot " + slotAddr;

    value = pConfigFile.readString(section, "Type", "Basic Peak Collector");

    parseBoardType(value);

    mapChannel =
      pConfigFile.readInt(section, "This Board is Source for Map Channel", -1);

    boardChannelForMapDataSource =
         pConfigFile.readInt(section, "Board Channel for Map Data Source", 1);

    headForMapDataSensor =
              pConfigFile.readInt(section, "Head for Map Data Sensor", -1);

    distanceMapSensorToFrontEdgeOfHead = pConfigFile.readDouble(section,
                    "Distance From Map Data Sensor to Front Edge of Head", 0);

    dataBufferSize = pConfigFile.readInt(section, "Data Buffer Size", 0);

    if(dataBufferSize > 100000000){ dataBufferSize = 100000000; }

    if(dataBufferSize > 0){ dataBuffer = new short[dataBufferSize]; }

    nSPerDataPoint = pConfigFile.readDouble(section, "nS per Data Point", 15.0);
    uSPerDataPoint = nSPerDataPoint / 1000;

    numberOfBanks = pConfigFile.readInt(section, "Number Of Banks", 1) - 1;
    //check for validity - set to one bank (value of zero)
    if (numberOfBanks < 0 || numberOfBanks > 3) {numberOfBanks = 0;}

    repRateInHertz = pConfigFile.readInt(
                                     section, "Pulse Rep Rate in Hertz", 2000);

    //store the value and calculate related values
    setRepRateInHertz(repRateInHertz);

    //each count is 15 ns
    triggerWidth = pConfigFile.readInt(section, "Pulse Width", 15);

    //limit to a safe value - see notes above for repRate
    if (triggerWidth < 0 || triggerWidth > 50) {triggerWidth = 15;}

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
// UTBoard::parseBoardType
//
// Sets various flags and variables appropriate to the type of board specified
// by pValue.
//

void parseBoardType(String pValue)
{

    if (pValue.equalsIgnoreCase("Basic Peak Collector")) {
        type = BASIC_PEAK_COLLECTOR;
        rabbitControlFlags |= RABBIT_FLAW_WALL_MODE;
    }
    else
    if (pValue.equalsIgnoreCase("Wall Mapper")) {
        type = WALL_MAPPER;
        rabbitControlFlags |= RABBIT_WALL_MAP_MODE;
    }

}//end of UTBoard::parseBoardType
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::getType
//
// Returns the board type -- i.e. the type of data processor the board is
// set up to do: peak detection, wall mapping, etc.
//

public int getType()
{

    return(type);

}//end of UTBoard::getType
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
// Currently, packets from the Rabbit and DSP do not have checksums. Data
// integrity is left to the TCP/IP protocol.
//

@Override
public int processOneDataPacket(boolean pWaitForPkt, int pTimeOut)
{

    if (byteIn == null) {return -1;}  //do nothing if the port is closed

    try{

        //wait a while for a packet if parameter is true
        if (pWaitForPkt){
            timeOutWFP = 0;
            while(byteIn.available() < 7 && timeOutWFP++ < pTimeOut){
                waitSleep(10);
            }
        }

        //wait until 7 bytes are available - this should be the 4 header bytes,
        //the packet identifier, the DSP chip identifier, and the DSP core
        //identifier
        if (byteIn.available() < 7) {return -1;}

        //read the bytes in one at a time so that if an invalid byte is
        //encountered it won't corrupt the next valid sequence in the case
        //where it occurs within 3 bytes of the invalid byte

        //check each byte to see if the first four create a valid header
        //if not, jump to resync which deletes bytes until a valid first header
        //byte is reached

        //if the reSynced flag is true, the buffer has been resynced and an 0xaa
        //byte has already been read from the buffer so it shouldn't be read
        //again

        //after a resync, the function exits without processing any packets

        if (!reSynced){
            //look for the 0xaa byte unless buffer just resynced
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] != (byte)0xaa) {reSync(); return 0;}
        }
        else {reSynced = false;}

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

        //reset the DSP message details -- will get set by processDSPMessage
        //if the packet is a DSP message response

        dspMsgID = DSP_NULL_MSG_CMD; dspMsgCoreID = DSP_NULL_CORE;

        if ( pktID == GET_STATUS_CMD) {return processRabbitStatusPacket();}

        if ( pktID == GET_ASCAN_CMD) {return processAScanPacket();}

        if ( pktID == READ_DSP_CMD || pktID == READ_NEXT_DSP_CMD) {
            return processReadDSPPacket();
        }

        if ( pktID == READ_DSP_BLOCK_CMD) {return processReadDSPBlockPacket();}

        if ( pktID == MESSAGE_DSP_CMD) {return processDSPMessage();}

        if ( pktID == GET_PEAK_DATA_CMD) {return processPeakDataPacket(1);}

        if ( pktID == GET_PEAK_DATA4_CMD) {return processPeakDataPacket(4);}

        if ( pktID == GET_WALL_MAP_CMD) {return processWallMapPacket();}

        if ( pktID == GET_DSP_RAM_BLOCK_CHECKSUM) {
            return processGetDSPRamChecksumPacket();
        }

    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3453");
    }

    return 0;

}//end of UTBoard::processOneDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processDataPacketsUntilPeakPacket
//
// Processes incoming data packets until a Peak Data packet has been processed.
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


    if (peakDataPacketProcessed == true) {return 1;}
    else {return -1;}

}//end of UTBoard::processDataPacketsUntilPeakPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processAllAvailableDataPackets
//
// Processes all available data packets in the receive buffer.
//
// if pWaitForPkt is true, then the method will wait a bit for each packet
// if necessary. This can slow the method quite a bit if a lot of packets are
// expected.
//
// See processOneDataPacket notes for more info.
//
// wip mks -- This has has a problem! If an error occurs and a resync is
// required, processOneDataPacket returns 0 even though the remaining packets
// could be processed after the resync correction. Maybe return -1 on resync
// and only bail out here if processOneDataPacket returns 0? Would need to
// update all functions which call processOneDataPacket in that case!
//

public void processAllAvailableDataPackets(boolean pWaitForPkt)
{

    //process packets until there is no more data available

    while ((processOneDataPacket(pWaitForPkt, TIMEOUT)) > 0){}

}//end of UTBoard::processAllAvailableDataPackets
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
            if (byteIn.available() >= 2) {break;}
            waitSleep(10);
            }
        if (byteIn.available() < 2) {return 0;}
        byteIn.read(inBuffer, 0, 2);
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3518");
    }

    //first two bytes are message type and the responding DSP core number
    dspMsgID = inBuffer[0]; dspMsgCoreID = inBuffer[1];

    //catch error
    if ((dspMsgCoreID & BOARD_ERROR_FLAG_BIT) != 0){
        handleDSPMessageErrors();
        return(2);
    }

    if ( dspMsgID == DSP_GET_STATUS_CMD) {return processDSPStatusMessage();}

    if ( dspMsgID == DSP_ACKNOWLEDGE) {return processDSPAckMessage();}

    if ( dspMsgID == DSP_GET_MAP_COUNT_CMD) {
        return processMapBufferWordsAvailableCount();
    }

    return 0;

}//end of UTBoard::processDSPMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::handleDSPMessageErrors
//
// Tracks error messages returned by the DSPs.
//

private void handleDSPMessageErrors()
{

    //store chip/core/packet ID/error message of the last error

    //for DSP errors, the core ID and message type are not valid as these are
    //returned by the DSP core which may not have responded in error cases
    //instead, use the packet info which can generally be parsed to deduce
    //the message details

    //the error code is returned from the remove via dspMsgID

    lastErrorDSPChip = pktDSPChipID; lastErrorCore = pktDSPCoreID;
    lastErrorPktID =  pktID;  lastErrorDSPMsgID = dspMsgID;

    if(dspMsgID == DSP_RETURN_PKT_ILLEGAL_SIZE_ERROR) {
        dspReturnPktIllegalSizeErrorCount++;
    }

    if(dspMsgID == DSP_RETURN_PKT_TIMEOUT_ERROR) {
        dspReturnPktTimeoutErrorCount++;
    }

    if(dspMsgID == DSP_RETURN_PKT_INVALID_HEADER_ERROR) {
        dspReturnPktInvalidHeaderErrorCount++;
    }

}//end of UTBoard::handleDSPMessageErrors
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
// packet.  In this case, the next packet will be lost as well.  This should
// happen rarely.
//

public void reSync()
{

    reSynced = false;

    //track the number of times this function is called, even if a resync is not
    //successful - this will track the number of sync errors
    reSyncCount++;

    //store info pertaining to what preceded the reSync - these values will be
    //overwritten by the next reSync, so they only reflect the last error
    //NOTE: when a reSync occurs, these values are left over from the PREVIOUS
    // good packet, so they indicate what PRECEDED the sync error.

    reSyncDSPChip = pktDSPChipID; reSyncDSPCore = pktDSPCoreID;
    reSyncPktID = pktID; reSyncDSPMsgID = dspMsgID;

    try{
        while (byteIn.available() > 0) {
            byteIn.read(inBuffer, 0, 1);
            if (inBuffer[0] == (byte)0xaa) {reSynced = true; break;}
            }
        }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3573");
    }

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
            if (byteIn.available() >= 2) {break;}
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
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3608");
    }

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
            if (byteIn.available() >= 1) {break;}
            waitSleep(10);
            }

        if (timeOutProcess < TIMEOUT && byteIn.available() >= 1){
            c = byteIn.read(readDSPResult, 0, 1);
            }
        else {
            for (int i=0; i < readDSPResult.length; i++) {readDSPResult[i] = 0;}
            readDSPDone = true;
            return(c);
            }

        // get the number of data bytes and validate - cast to  int and AND with
        // 0xff to ignore two's complement and give 0-255

        int count;
        count = (int)readDSPResult[0] & 0xff;
        if (count > readDSPResult.length) {count = readDSPResult.length;}

        // read in the data bytes

        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= count) {break;}
            waitSleep(10);
            }

        if (timeOutProcess < TIMEOUT && byteIn.available() >= count){
            c += byteIn.read(readDSPResult, 0, count);
            readDSPDone = true;
            return(c);
            }
        else {
            for (int i=0; i < readDSPResult.length; i++) {readDSPResult[i] = 0;}
            readDSPDone = true;
            return(c);
            }

    }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3682");
    }

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
            if (byteIn.available() >= 2) {break;}
            waitSleep(10);}
        if (byteIn.available() >= 2){
            dspStatusMessageRcvd = true;
            return byteIn.read(inBuffer, 0, 2);
            }
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3712");
    }

    return 0; // failure - no bytes read

}//end of UTBoard::processDSPStatusMessage
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processMapBufferWordsAvailableCount
//
// Retrieves the number of words stored in the DSPs map buffer from the packet
// and stores it in inBuffer.
//
// Returns number of bytes retrieved from the socket.
//

private int processMapBufferWordsAvailableCount()
{

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= 2) {break;}
            waitSleep(10);}
        if (byteIn.available() >= 2){
            dspStatusMessageRcvd = true;
            return byteIn.read(inBuffer, 0, 2);
            }
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3712");
    }

    return 0; // failure - no bytes read

}//end of UTBoard::processMapBufferWordsAvailableCount
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
            if (byteIn.available() >= 2) {break;}
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
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3751");
    }

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
// Increments dspMessageAckCounter for each ACK packet received.
//
// Returns number of bytes retrieved from the socket.
//

private int processDSPAckMessage()
{

    try{
        timeOutProcess = 0;
        while(timeOutProcess++ < TIMEOUT){
            if (byteIn.available() >= 1) {break;}
            waitSleep(10);
            }
        if (byteIn.available() >= 1) {
            dspMessageAckCounter++;
            return byteIn.read(inBuffer, 0, 1);
        }
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3795");
    }

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
            if (byteIn.available() >= 804) {break;}
            waitSleep(10);
            }
        if ((x = byteIn.available()) >= 804) {byteIn.read(inBuffer, 0, 804);}
        else
            {return 0;}
        }// try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 3832");
    }

    //get the board channel from the packet for the aScan data set
    int channel = inBuffer[0];
    if (channel < 0) {channel = 0;} if (channel > 3) {channel = 3;}

    int aScanSmoothing = bdChs[channel].aScanSmoothing;

    //move to the next position of the filtering FIFO each time
    aScanFIFOIndex++;
    if (aScanFIFOIndex >= aScanSmoothing) {aScanFIFOIndex = 0;}

    //get the aScan range associated with this data set - this should be used
    //for the display because when the range is being changed, the value the
    //host currently has may not be what was used by the DSP due to pipeline
    //latency in the connection

    aScanFIFO[aScanFIFOIndex].range = inBuffer[1];

    //get the location where the interface crossed the interface gate

    aScanFIFO[aScanFIFOIndex].interfaceCrossingPosition =
            (int)((inBuffer[2]<<8) & 0xff00) + (int)(inBuffer[3] & 0xff);

    //the interface crossing position returned by the DSP is relative to the
    //start of the sample buffer stored by the FPGA - the FPGA delays by
    //hardwareDelay number of samples from the initial pulse before recording,
    //add this back in to make the crossing value relative to the initial pulse
    aScanFIFO[aScanFIFOIndex].interfaceCrossingPosition += hardwareDelay;

    for (int i = 0; i < firBuf.length; i++) {firBuf[i] = 0;}

    //transfer the bytes to the int array - allow for sign extension
    //400 words from 800 bytes, MSB first
    //the +4 shifts past the leading info bytes
    //the +5 points to the LSB (would be +1 without the shift)

    for (int i=0; i<ASCAN_SAMPLE_SIZE; i++){

        int raw, filtered = 0;

         raw =
           (int)((int)(inBuffer[i*2+4]<<8) + (inBuffer[(i*2)+5] & 0xff));

        if (raw > 0 && raw < bdChs[channel].rejectLevel) {raw = raw % 10;}
        else if (raw < 0 && raw > -bdChs[channel].rejectLevel) {raw = raw % 10;}

        raw *= ASCAN_SCALE;

        boolean filterActive = false;

        if (filterActive){

            //apply FIR filtering

            //shift the old samples and insert the newest
            for(int n = firBuf.length-1; n>0; n--) {firBuf[n] = firBuf[n-1];}
            firBuf[0] = raw;

            //calculate the new filtered output value
            for(int n=0; n<firCoef.length; n++){
                filtered += firCoef[n] * firBuf[n];
            }

            filtered /= 290000;

            }
        else{
            filtered = raw; //no filtering applied
            }

        aScanFIFO[aScanFIFOIndex].buffer[i] = filtered;

        }// for (int i=0; i<ASCAN_SAMPLE_SIZE; i++)

    // the display thread can be accessing the aScan buffer at any time, so
    // prepare all the data in the aScanBuffer first - transferring from
    // the aScanBuffer to aScan is not visible because the data is nearly
    // identical each time when the transfer occurs

    //transfer the range to the aScan object as is
    aScanBuffer.range = aScanFIFO[aScanFIFOIndex].range;
    //the interfaceCrossingPosition gets averaged
    aScanBuffer.interfaceCrossingPosition = 0;
    //the data samples get averaged
    for (int i=0; i<ASCAN_SAMPLE_SIZE; i++) {aScanBuffer.buffer[i] = 0;}

    for (int i=0; i<aScanSmoothing; i++){

        //sum the interface crossing position from each dataset
        aScanBuffer.interfaceCrossingPosition +=
                                        aScanFIFO[i].interfaceCrossingPosition;

        //sum all datasets in the fifo
        for (int j=0; j<ASCAN_SAMPLE_SIZE; j++){
            aScanBuffer.buffer[j] += aScanFIFO[i].buffer[j];
        }

    }//for (int i=0; i< aScanSmoothing; i++)


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
// packet. Any board channel which is not active have an empty data group
// returned by the Rabbit.
//
// Parameter pBufPtr points to the next byte to be read from the packet.
//
// Returns a number greater than zero if a packet successfully extracted.
//

public int processPeakData(int pNumberOfChannels, int pEncoder1, int pEncoder2)
{

    int x;

    //process number of channels specified - each channel data section in the
    //packet has the number of gates for that channel

    for (int h=0; h < pNumberOfChannels; h++){

        //get next two bytes -- channel number and number of gates for section
        if (readBytes(2) == 0) {return(0);}

         x = 0;

        //retrieve the board channel 0-3
        //this channel number refers to the analog channels on the board
        int channel = inBuffer[x++];

        // if the channel number is illegal, bail out - the code will resync to
        // toss the unused bytes still in the socket -- note that -1 designates
        // an inactive channel and will have no data in the packet

        if ((channel < 0 || channel > NUMBER_OF_BOARD_CHANNELS-1)
             && channel != -1) {return x;}

        int numberOfGates = inBuffer[x++]; //number of gates for the channel

        if (channel == -1) continue; //skip inactive channel    
        
        // if the gate count is illegal, bail out - the code will resync to
        // toss the unused bytes still in the socket

        if (numberOfGates < 0 || numberOfGates > 9) {return x;}

        // calculate the number of data bytes for the channel
        int numberDataBytes = numberOfGates * PEAK_DATA_BYTES_PER_GATE;

        //add extra for wall data if the specified channel has such
        if (bdChs[channel].isWallChannel){
            numberDataBytes += PEAK_DATA_BYTES_FOR_WALL;
        }

        //read bytes for the channel
        if (readBytes(numberDataBytes) == 0) {return(0);}

        int peakFlags;
        int peak;
        int peakFlightTime;
        int peakTrack;

        x = 0;

        for (int i=0; i < numberOfGates; i++){

            peakFlags =
               (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

            //did gate receive the host specified number of consecutive hits?
            boolean hitCountMet;
            hitCountMet = (peakFlags & HIT_COUNT_MET) != 0;

            //cast to short used to force sign extension for signed values
            peak =
                 (short)((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

            //if the signal is below the reject level, squash it down to 10%
            if (peak < bdChs[channel].rejectLevel) {peak %= 10;}

            //if the hit count for the gate is greater than zero and the signal
            //did not exceed the gate the specified number of times, squash it
            //down to 10%
            //at first glance, it would seem that a hitCount of zero would
            //always trigger the flag in the DSP, but if the signal never
            //exceeds the gate, the hitCountMet flag never gets set even if the
            //hitCount is zero, so have to catch that special case here

            if (bdChs[channel].gates[i].gateHitCount.getValue() > 0
                                                             && !hitCountMet) {
                peak %= 10;
            }

            //the flight time value is the memory address of the peak in the DSP
            //the data buffer starts at 0x8000 in memory, so subtracting 0x8000
            //from the flight time value gives the time position in relation to
            //the time the FPGA began recording data after the hardware delay
            //NOTE: the FPGA should really subtract the 0x8000 instead of doing
            //it here!

            peakFlightTime =
               (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

            peakFlightTime -= 0x8000;

            //peakTrack is the angular and/or linear position of the indication
            //on the test piece -- most systems use peakTrack to track angular
            //position while linear position is read from the control board
            //encoder data

            peakTrack =
               (int)((inBuffer[x++]<<8) & 0xff00) + (int)(inBuffer[x++] & 0xff);

            //debug mks

            //debug code for testing tdc
            //if reset pulse is missed, peakTrack will be greater than 11

            debug++;

            if (peakTrack > 14)
                {debug = 0;}

            if (peakTrack > 22)
                {debug = 0;}

            //catch a particular slot and board channel
            if (slotAddr == 6 && channel == 1)
                {debug = 26;}

            //end debug mks

            //protect against corrupt values
            if(peakTrack < 0) {peakTrack = 0;}

            //Add in the clock position adjustment to account for offset sensor
            //positions and timing errors.  Wraparound past the max clock
            //position gets taken care of with modulo in the next bit of code.
            peakTrack += CLOCK_OFFSET;

            //If the TDC input is missed, the counter will not be reset and will
            //continue counting past the maximum clock position.  It should
            //still be accurate, so the actual clock position can be determined
            //by computing the wrap around.

            if (peakTrack > MAX_CLOCK_POSITION) {
                peakTrack = peakTrack % (MAX_CLOCK_POSITION + 1);
            }

            //the peakTrack variable denotes the clock position, replace
            //position 0 with 12 before saving

            int clockPos = peakTrack;
            if (clockPos == 0) {clockPos = 12;}

            //NOTE: clockPos and peakTrack are the same now -- should really
            //store the original peakTrack value along with the clockPos.

            bdChs[channel].gates[i].storeNewAScanPeak((int)(peak * ASCAN_SCALE),
                                                                peakFlightTime);

            peak *= SIGNAL_SCALE; //scale signal up or down

            bdChs[channel].gates[i].storeNewData(peak, 0, 0, 0,
                    peakFlags, peakFlightTime, peakTrack, clockPos,
                    pEncoder1, pEncoder2);

            //if the channel has been configured to modify the wall, then save
            //the data so that it can be used to modify the wall elsewhere
            if (bdChs[channel].gates[i].modifyWall){
                //store the value if it is greater than the stored peak
                if (peak > hdwVs.wallMinModifier) {
                    hdwVs.wallMinModifier = peak;
                }
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

            if (maxThickness == -32768) {
                maxThickness = prevMaxThickness;
            }
            else {
                prevMaxThickness = maxThickness;
            }

            //see notes above for peakTrack -- make this into a function?
            if(wallMaxTrack < 0) {wallMaxTrack = 0;}
            wallMaxTrack += CLOCK_OFFSET;
            if (wallMaxTrack > MAX_CLOCK_POSITION) {
                wallMaxTrack = wallMaxTrack % (MAX_CLOCK_POSITION + 1);
            }
            int clockPos = wallMaxTrack;
            if (clockPos == 0) {clockPos = 12;}

            //store the max peak - overwrites info saved for this gate above
            //debug mks - gates[1] should use wallStartGate specified by user
            bdChs[channel].gates[1].storeNewDataD(
                                         maxThickness, wallMaxTrack, clockPos);
            
            //output as analog signal if enabled
            if (bdChs[channel].analogOutputControllerChannel != -1){
                outputWallThicknessOnAnalogOutput(channel, maxThickness);
            }
                        
            // Note that StartNum, StartDen, EndNum, and EndDen are no longer
            // used as the fractional math has been abandonded.
            // See Git commit tag VersionWithFractionalMathForThickness in the
            // Java and DSP code archives for version which used fractional
            // math.

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
            
            if (minThickness == 32767 || 
               (minThickness<minThickTossThreshold && minThickTossCount < 2)){
                minThickness = prevMinThickness;
                minThickTossCount++;
            }
            else {
                //if the modifier value is over the threshold, then modify the
                //wall trace with it
                //NOTE: need to add the threshold to the config file so it is
                // programmable.
                if (hdwVs.wallMinModifier > 50) {
                    minThickness -= hdwVs.wallMinModifier;
                    hdwVs.wallMinModifier = Integer.MIN_VALUE;
                    }
                
                prevMinThickness = minThickness;
                minThickTossThreshold = minThickness - (minThickness * .25);
                minThickTossCount = 0;
                }

            //see notes above for peakTrack -- make this into a function?
            if(wallMinTrack < 0) {wallMinTrack = 0;}
            wallMinTrack += CLOCK_OFFSET;
            if (wallMinTrack > MAX_CLOCK_POSITION) {
                wallMinTrack = wallMinTrack % (MAX_CLOCK_POSITION + 1);
            }
            clockPos = wallMinTrack;
            if (clockPos == 0) {clockPos = 12;}

            //store the min peak - overwrites info saved for this gate above
            //debug mks - gates[2] should use the wallEndGate specified by user
            bdChs[channel].gates[2].storeNewDataD(
                                         minThickness, wallMinTrack, clockPos);

            //output as analog signal if enabled
            if (bdChs[channel].analogOutputControllerChannel != -1){
                outputWallThicknessOnAnalogOutput(channel, minThickness);
            }

            
            //debug mks -- sets max wall to min wall since max wall is so
            //screwed up at Tejas
            //store the max peak - overwrites info saved for this gate above
            //debug mks - gates[1] should use wallStartGate specified by user
            bdChs[channel].gates[1].storeNewDataD(
                                         minThickness, wallMinTrack, clockPos);
            //debug mks end
            
            
        }// if (bdChs[channel].isWallChannel)

    }// for (int h=0; h < pNumberOfChannels; h++)

    return 1;

}//end of UTBoard::processPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::outputWallThicknessOnAnalogOutput
//
// If an IO Module output has been linked to board channel pChannel, the
// wall thickness represented by pValue is applied to the output.
//
// Both min and max values can be passed to this function for the same
// channe...the analog controller object will track the min and max values and
// ensure that they are transmitted.
//

public void outputWallThicknessOnAnalogOutput(int pChannel, double pValue)
{

    //convert nanosecond time span to distance
    double thickness = pValue * hdwVs.nSPerDataPoint * hdwVs.velocityNS /
                                                (hdwVs.numberOfMultiples * 2);

//    thickness = .260; //debug mks remove this
    
    //wip mks -- need to load nominal current offset and scale from config file
    //           also need to load correction factor
    
    
    //compute 4-20mA current loop output for the wall value where
    //12mA is the nominalWall and each 0.001 of wall change is
    //0.2 mA current change
    
    double OFFSET = 0.223; //debug mks -- allow user to adjust this
    
    double analogOutput = 12.0 + 
                    (thickness - OFFSET) * 48.780487804878048780487804878049;

    double correction = 1.0174418604651162790697674418605;
    
    analogOutput *= correction;
    
    bdChs[pChannel].applyWallOnAnalogOutput(analogOutput); 
    
}//end of UTBoard::outputWallThicknessOnAnalogOutput
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

    int pktNumber, status;

    //allow another request packet to be transmitted now that the return
    //packet for the previous request has been received
    peakDataRcvd = true;

    int x;

    //read packet number, status, encoder 1, encoder 2
    if (readBytes(10) == 0) {return(0);}

    x = 0;

    pktNumber = (int)inBuffer[x++];
    status = (int)inBuffer[x++];

    //get the position of encoder 1
    //this is the entry encoder or the carriage encoder depending on unit type
    //note that not all systems use the encoder 1 value in this packet -- in
    //many configurations the UT boards do not know the encoder values

    int encoder1 =
         ((inBuffer[x++]<<24) & 0xff000000) +  ((inBuffer[x++]<<16) & 0xff0000)
          + ((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

    //get the position of encoder 2
    //this is the entry encoder or the carriage encoder depending on unit type
    //note that not all systems use the encoder 2 value in this packet -- in
    //many configurations the UT boards do not know the encoder values

    int encoder2 =
          ((inBuffer[x++]<<24) & 0xff000000) + ((inBuffer[x++]<<16) & 0xff0000)
           + ((inBuffer[x++]<<8) & 0xff00) + (inBuffer[x++] & 0xff);

    //extract the peak info for each gate of each channel
    x = processPeakData(pNumberOfChannels, encoder1, encoder2);

    //flag that a Peak Data packet has been processed and the data is ready
    peakDataPacketProcessed = true;

    //return value greater than zero if bytes read from socket

    return(x);

}//end of UTBoard::processPeakDataPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processRabbitStatusPacket
//
// Handles data from a Rabbit status packet. Reads two bytes from socket into
// buffer.
//
// Returns number of bytes retrieved from the socket.
//

public int processRabbitStatusPacket()
{

    return (readBytes(2));

}//end of UTBoard::processRabbitStatusPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::processWallMapPacket
//
// Handles data from a wall map packet.  This type of packet is used to
// transfer the wall mapping data from the remote devices.
//
// Returns number of bytes retrieved from the socket.
//

public int processWallMapPacket()
{

    try{

        //wait a bit for the full packet, bail out if it times out
        //that will cause a resync and a tossed packet
        if (!waitForNumberOfBytes(WALL_MAP_PACKET_DATA_SIZE)){
            return(0);
        }

        //read in the packet count and the status byte
        byteIn.read(inBuffer, 0, 2);

        packetCount = inBuffer[0];

        for (int i = 0; i < (WALL_MAP_PACKET_DATA_SIZE_INTS - 1); i++){

            byteIn.read(inBuffer, 0, 2);

            int value =
              (int)((inBuffer[0]<<8) & 0xff00) + (int)(inBuffer[1] & 0xff);

            //if control code flag set, handle code embedded in the data
            if ((value & MAP_CONTROL_CODE_FLAG) != 0) {
                value = handleMapDataControlCode(value);
            }

            //only store if data buffer is enabled

            if(dataBufferIsEnabled){

                //only store if value is not an ignored control code
                if ((value & MAP_IGNORE_DETECTION) != MAP_IGNORE_DETECTION){
                    dataBuffer[dataBufferIndex++] = (short)value;
                }

                if (dataBufferIndex == dataBuffer.length){
                    dataBufferIndex = dataBuffer.length-1;
                }
            }

            mapTDCCodeIgnoreTimer--; //don't care if goes negative

        }
    }//try
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 4674");
    }

    return(WALL_MAP_PACKET_DATA_SIZE); //number of bytes read from the socket

}//end of UTBoard::processWallMapPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::handleMapDataControlCode
//
// Handles control codes embedded in the map data from the remotes.
//
// Examples are top-dead-center markers for rotary position and linear
// advance markers for linear position.
//
// The code will be incremented each time the test piece or heads rotate to
// the top-dead-center position.
//
// In some modes, the count will be reset to zero when the Control board
// signals a linear advance. In other modes, the Control board will not
// reset the count and another software object will trigger the advance based
// on encoder counts or some other mechanism. See Note 1 below.
//
// NOTE: The 14 lsb of control codes are cleared in the buffer so the
// lower bits can be used as various control flags later in the processing
// chain. As lower bits of the control code generally gets incremented with
// each occurrance as received from the remote, the incremented part must be
// removed before the bits can be used later as flags.
//
// Note 1:
//
// If mapAdvanceMode == ADVANCE_ON_TRIGGER, the map advance is handled by
// another Java object. In that case, it is assumed that all control codes
// from the remotes are meant to be TDC codes. Since the Control board is not
// resetting the TDC code count in the lower bits, it will roll over to zero
// and appear to be a linear advance code -- in this mode it must be handled as
// a TDC rather than a linear advance.
//
// Returns:
//
// If a TDC code, code returned with only MAP_CONTROL_CODE_FLAG flag bit set.
//
// If a TDC code received too soon after the previous TDC code, the code is
// ignored and code is returned with MAP_CONTROL_CODE_FLAG and
// MAP_IGNORE_CODE_FLAG set which signals that the code should not be stored in
// the buffer.
//
// If Linear Advance code, previous control code in the buffer will have its
// MAP_LINEAR_ADVANCE_FLAG bit set and the code code is returned with
// MAP_CONTROL_CODE_FLAG and MAP_IGNORE_CODE_FLAG set which signals that the
// code should not be stored in the buffer.
//
// Thus the only control codes stored in the final data will have the
// MAP_CONTROL_CODE_FLAG flag bit set and will represent TDC codes; some of
// those also will have the linear advance flag set.
//

private int handleMapDataControlCode(int pCode)
{

    //zero the top control bit to use value in lower bits
    int code = pCode & ~MAP_CONTROL_CODE_FLAG;

    //since there is currently no Rabbit track reset pulse, the track will
    //roll over periodically when incremented due to TDC trigger -- thus zero
    //is a TDC code so treat it the same way unless Rabbit code changes

    if (code > 0 ) {
        code = handleMapDataTDCCode(code);
        return(code);
    }

    //see Note 1 in method header
    if (code == 0) {
        if (mapAdvanceMode == ADVANCE_ON_TRIGGER){
            code = handleMapDataTDCCode(code);
        }
        else{
            code = handleMapDataLinearAdvanceCode(code);
        }

        return(code);

    }

    return(code);

}//end of UTBoard::handleMapDataControlCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::handleMapDataTDCCode
//
// Handles top-dead-center marker control code for map data.
//
// This code specifies that the top-dead-center signal has been detected. That
// signal occurs once per revolution and is used to align the data in its
// angular position around the test piece.
//
// Each time this code is detected, the data since the last TDC code is
// considered to be one helical revolution of data. It is compressed/expanded
// to fit a single column of the 2D map.
//
// Returns:
//
// If valid TDC code, code returned with only MAP_CONTROL_CODE_FLAG flag bit set
// so the other bits can be used as flags by later processes.
//
// If a TDC code received too soon after the previous TDC code, the code is
// considered to be invalid and ignored and code is returned with
// MAP_CONTROL_CODE_FLAG and MAP_IGNORE_CODE_FLAG set which signals that the
// code should not be stored in the buffer.
//

private int handleMapDataTDCCode(int pCode)
{

    int defaultValue;
    boolean maxMode;

    //set only the bit designating value as a control flag
    int code = MAP_CONTROL_CODE_FLAG;

    //if the code is encountered too soon after the last code, assume that it
    //is an erroneous multiple hit, ignore it and return last good value

    if(mapTDCCodeIgnoreTimer > 0){
        //restart timer to catch a possible next multiple hit
        mapTDCCodeIgnoreTimer = MAP_TDC_IGNORE_TIMER_RESET;
        return(code |= MAP_IGNORE_CODE_FLAG);
    }

    //ignore possible erroneous multiple hit
    mapTDCCodeIgnoreTimer = MAP_TDC_IGNORE_TIMER_RESET;

    //if this board has no map or it has not been set yet, bail out
    if (map2D == null) {
        //keep current code position for next time
        prevCtrlCodeIndex = dataBufferIndex;
        return(code);
    }

    //prepare variables based on whether maximum or minimum peaks are to be
    //captured for mapping

    if (map2D.isMaxCaptureMode()){
        maxMode = true; defaultValue = Integer.MIN_VALUE;
    }else{
        maxMode = false; defaultValue = Integer.MAX_VALUE;
    }

    //initialize the array with default value so comparisons work first time
    for(int i = 0; i < map2DDataColumn.length; i++){
        map2DDataColumn[i] = defaultValue;
    }

    //get number of samples in this revolution
    //this code also works fine first time through when prevCodePostion is -1,
    //but that first revolution will be partial and unusable

    int numSamplesInRev = dataBufferIndex - prevCtrlCodeIndex - 1;

    //first data point for the revolution in dataBuffer
    int xfrSourceIndex = prevCtrlCodeIndex + 1;

    //keep current code position for next time
    prevCtrlCodeIndex = dataBufferIndex;

    //determine the scale to shrink/stretch the samples to fit the map column
    double scale = (double)map2DDataColumn.length / (double)numSamplesInRev;

    int scaledPosition;

    for (int i = 0; i < numSamplesInRev; i++){

        int value = dataBuffer[xfrSourceIndex++];

        //adjust the position of each sample so the range fits the column
        scaledPosition = (int)(i * scale);

        //protect against round off error going past end of array
        if (scaledPosition >= map2DDataColumn.length)
        { scaledPosition = map2DDataColumn.length - 1; }

        //store the max or min peak depending on the mode
        if(maxMode) {
            if (value > map2DDataColumn[scaledPosition])
            { map2DDataColumn[scaledPosition] = value; }
        }else{
            if (value < map2DDataColumn[scaledPosition])
            { map2DDataColumn[scaledPosition] = value; }
        }
    }//for (int i = 0; i < numSamplesInRev; i++)

    //check for any elements skipped from stretching data to fit and fill
    //with the data next to it -- probably faster to do it like this (as a
    //separate loop) than to add code to the loop above -- note that element
    //0 will never have been skipped due to scaling so it is bypassed here
    for(int i = 1; i < map2DDataColumn.length; i++){
        if (map2DDataColumn[i] == defaultValue){
            map2DDataColumn[i] = map2DDataColumn[i-1];
        }
    }

    //store the revolution of data points in the map
    map2DData.storeDataAtInsertionPoint(map2DDataColumn);

    //if mode is appropriate, advance the map for each revolution
    //(typically used for Scan mode)
    //in this case, ignore the altered code returned as this is actually a TDC
    //code and it must be stored in the buffer
    if (mapAdvanceMode == Board.ADVANCE_ON_TDC_CODE){
        handleMapDataLinearAdvanceCode(code);
    }

    return(code);

}//end of UTBoard::handleMapDataTDCCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::handleMapDataLinearAdvanceCode
//
// Handles linear-advance marker control code for map data.
//
// This code specifies that the test piece has moved an incremental amount
// through the system (or the test heads have moved). It is used to determine
// if the map should also be advanced.
//
// Currently, the map is advanced one position each time the control code is
// received. It is up to the remotes to ensure that the increments are
// accurate over the full length of the test.
//
// When called by handleMapDataTDCCode due to ADVANCE_ON_TDC_CODE mode, the
// MAP_LINEAR_ADVANCE_FLAG bit will be ovewritten by that method with
// only the MAP_CONTROL_CODE_FLAG set. Currently, the bit is not important
// in those modes anyway.
//
// Returns:
//
// If Linear Advance code, previous control code in the buffer will have its
// MAP_LINEAR_ADVANCE_FLAG bit set and the code is returned with
// MAP_CONTROL_CODE_FLAG and MAP_IGNORE_CODE_FLAG set which signals that the
// code should not be stored in the buffer.
//
// NOTE for FUTURE:
//
// Currently the TDC code handler catches code which occur to closely and
// ignores them -- assumes it was a double hit on the photo eye or an electrical
// bounce on the pulse line.
//
// However, this is not currently in place for the Linear Advance codes -- a
// quick double hit might be used in the future by the Control board to signal
// reverse direction so system can back up when mapping. In that case, if there
// are glitches on the Control board pulse line, they will have to be solved
// some other way -- perhaps filtering by the FPGA.
//
// prevCtrlCodeIndex is not updated because these codes are never kept in
// the data buffer -- they merely set a flag in the previous control code.
//
// If the data buffer is not enabled, no action will be taken.
//

private int handleMapDataLinearAdvanceCode(int pCode)
{

    //set only the bit designating value as a control flag
    int code = MAP_CONTROL_CODE_FLAG;

    if(!dataBufferIsEnabled) { return(code |= MAP_IGNORE_CODE_FLAG); }

    if (prevCtrlCodeIndex > 0 && prevCtrlCodeIndex < dataBuffer.length){
        dataBuffer[prevCtrlCodeIndex] |= MAP_LINEAR_ADVANCE_FLAG;
    }

    //if this board has no map or it has not been set yet, bail out
    if (map2D == null) { return(code |= MAP_IGNORE_CODE_FLAG); }

    map2D.advanceInsertionPoint();

    return(code |= MAP_IGNORE_CODE_FLAG);

}//end of UTBoard::handleMapDataLinearAdvanceCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::triggerMapAdvance
//
// If the board is a mapping type and the current mode allows for external
// control of map advancement, a flag bit is placed in the last control code
// stored in the data to reflect that the linear position has advanced and
// if the board controls a map, the map is advanced one position.
//
// Currently, the map packet handling code and the code which calls this
// function should be the same thread, so collisions with
// dataBuffer[prevCtrlCodeIndex] are not a problem.
//
// If the data buffer is not enabled, no action will be taken.
//

@Override
public void triggerMapAdvance()
{

    if(!dataBufferIsEnabled) { return; }

    if(type == WALL_MAPPER && mapAdvanceMode == ADVANCE_ON_TRIGGER){

        if (map2D != null) {
            //move to next buffer slot for storing data
            map2D.advanceInsertionPoint();
        }

        //set flag in the last code stored in the buffer to signify that the
        //slice will be stored in the next map column -- this is done so that
        //the raw data will have position information in the saved file

        if (prevCtrlCodeIndex > 0 && prevCtrlCodeIndex < dataBuffer.length){

            dataBuffer[prevCtrlCodeIndex] |= MAP_LINEAR_ADVANCE_FLAG;

        }
    }

}//end of UTBoard::triggerMapAdvance
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::recordStartLocationForMapping
//
// If the board is a mapping type and the mapping transducer is in pHead, a
// flag bit is placed in the last control code stored in the data to reflect
// that the inspection has started.
//
// Currently, the map packet handling code and the code which calls this
// function should be the same thread, so collisions with
// dataBuffer[prevCtrlCodeIndex] are not a problem.
//

public void recordStartLocationForMapping(int pHead, double pPosition)
{

    if(type == WALL_MAPPER && headForMapDataSensor == pHead){

        inspectionStartLocation = pPosition;

        if (prevCtrlCodeIndex > 0 && prevCtrlCodeIndex < dataBuffer.length){

            dataBuffer[prevCtrlCodeIndex] |= MAP_START_CODE_FLAG;

        }
    }

}//end of UTBoard::recordStartLocationForMapping
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::recordStopLocationForMapping
//
// If the board is a mapping type and the mapping transducer is in pHead, a
// flag bit is placed in the last control code stored in the data to reflect
// that the inspection has started.
//
// Currently, the map packet handling code and the code which calls this
// function should be the same thread, so collisions with
// dataBuffer[prevCtrlCodeIndex] are not a problem.
//

public void recordStopLocationForMapping(int pHead, double pPosition)
{

    if(type == WALL_MAPPER && headForMapDataSensor == pHead){

        inspectionStopLocation = pPosition;

        if (prevCtrlCodeIndex > 0 && prevCtrlCodeIndex < dataBuffer.length){

            dataBuffer[prevCtrlCodeIndex] |= MAP_STOP_CODE_FLAG;

        }
    }

}//end of UTBoard::recordStopLocationForMapping
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::installNewRabbitFirmware
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

    super.installNewRabbitFirmware(
                               "UT", "Rabbit\\CAPULIN UT BOARD.bin", settings);

}//end of UTBoard::installNewRabbitFirmware
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::signed2BytesToInt
//
// Converts the two bytes of a signed short int to an integer.
//
// Use this if the original value was signed.
//

private int signed2BytesToInt(byte pByte1, byte pByte0)
{

    return (short)((pByte1<<8) & 0xff00) + (pByte0 & 0xff);
    
}//end of UTBoard::signed2BytesToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::unSigned2BytesToInt
//
// Converts the two bytes of an unsigned short to an integer.
//
// Use this if the original value was unsigned.
//

private int unSigned2BytesToInt(byte pByte1, byte pByte0)
{

    return (int)((pByte1<<8) & 0xff00) + (pByte0 & 0xff);
    
}//end of UTBoard::unSigned2BytesToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::signed4BytesToInt
//
// Converts the four bytes of a signed integer to an integer.
//
// Use this if the original value was signed.
//

private int signed4BytesToInt(
                        byte pByte3, byte pByte2, byte pByte1, byte pByte0)
{
            
    return
         ((pByte3<<24) & 0xff000000) +  ((pByte2<<16) & 0xff0000)
          + ((pByte1<<8) & 0xff00) + (pByte0 & 0xff);

}//end of UTBoard::signed4BytesToInt
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logStatus
//
// Writes various status and error messages to the log window.
//

@Override
public void logStatus(Log pLogWindow)
{

    Log lLog = pLogWindow; //use shorter name

    boolean displayErrorDetail = false;

    //display a header to identify the board

    lLog.append("----------------------------------------------\n");
    lLog.appendLine("Chassis #: " + chassisAddr + "   Slot #: " + slotAddr);

    // if there have been socket sync errors, display message and clear count

    if (reSyncCount > 0){
        lLog.appendLine("");
        lLog.appendLine("Number of reSync errors since last report: "
        + reSyncCount
        + "\nInfo for packet processed prior to sync error: \n"
        + "DSP Chip: " + reSyncDSPChip + " DSP Core: " + reSyncDSPCore
        + "\nPacket ID: " + reSyncPktID + " DSP Message ID: " + reSyncDSPMsgID);
        lLog.appendLine("");
        reSyncCount = 0;
    }

    //display number of peak data requests which had no response
    if (noResponseToPeakDataRequestCount > 0){
        lLog.append("Number of Peak Data Packet Requests with no response:");
        lLog.appendLine(" " + noResponseToPeakDataRequestCount);
        noResponseToPeakDataRequestCount = 0;
    }

    //display number of peak data requests which time out before response
    if (noResponseToPeakDataRequestTimeOutCount > 0){
        lLog.append("Number of Peak Data Packet Requests which timed out:");
        lLog.appendLine(" " + noResponseToPeakDataRequestTimeOutCount);
        noResponseToPeakDataRequestTimeOutCount = 0;
        displayErrorDetail = true;
    }

    if (dspReturnPktIllegalSizeErrorCount > 0){
        lLog.append("Number of DSP return packet illegal size errors:");
        lLog.appendLine(" " + dspReturnPktIllegalSizeErrorCount);
        dspReturnPktIllegalSizeErrorCount = 0;
        displayErrorDetail = true;
    }

    if (dspReturnPktTimeoutErrorCount > 0){
        lLog.append("Number of DSP return packet timeout errors:");
        lLog.appendLine(" " + dspReturnPktTimeoutErrorCount);
        dspReturnPktTimeoutErrorCount = 0;
        displayErrorDetail = true;
    }

    if (dspReturnPktInvalidHeaderErrorCount > 0){
        lLog.append("Number of DSP return packet invalid header errors:");
        lLog.appendLine(" " + dspReturnPktInvalidHeaderErrorCount);
        dspReturnPktInvalidHeaderErrorCount = 0;
        displayErrorDetail = true;
    }

    //display the last DSP core which reported an error and details
    if (displayErrorDetail){
        lLog.appendLine("Last error code reported:" + lastErrorCode);
        lLog.appendLine("Details:");
        lLog.appendLine(
        "DSP Chip: " + lastErrorDSPChip + " DSP Core: " + lastErrorCore
        + "\nPacket ID: " + lastErrorPktID
        + " DSP Message ID: " + lastErrorDSPMsgID);

        lastErrorCode = NO_ERROR;


    }

}//end of UTBoard::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::logStatistics
//
// Displays various statistics in the log window.
//

public void logStatistics()
{


    try{
    int bCount = byteIn.available();
    logger.logMessage(
              "UT " + chassisSlotAddr + " ~ " + ipAddrS + " has "
            + bCount + " bytes in the receive buffer." + "\n");

    //debug mks if (bCount > 0) {byteIn.read(inBuffer, 0, inBuffer.length);}
    }
    catch(IOException e){}

}//end of UTBoard::logStatistics
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// UTBoard::toHexString
//
// Converts an integer to a 4 character hex string.
//

String toHexString(int pValue)
{

    String s = Integer.toString(pValue & 0xffff, 16);

    //force length to be four characters
    if (s.length() == 0) {return("0000" + s);}
    else
    if (s.length() == 1) {return("000" + s);}
    else
    if (s.length() == 2) {return("00" + s);}
    else
    if (s.length() == 3) {return("0" + s);}
    else
    {return (s);}

}//end of UTBoard::toHexString
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
        if (byteOut != null) {byteOut.close();}
        if (byteIn != null) {byteIn.close();}
        if (out != null) {out.close();}
        if (in != null) {in.close();}
        if (socket != null) {socket.close();}
    }
    catch(IOException e){
        logSevere(e.getMessage() + " - Error: 4509");
    }

}//end of UTBoard::shutDown
//-----------------------------------------------------------------------------

}//end of class UTBoard
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
