/******************************************************************************
* Title: Capulin1.java
* Author: Mike Schoonover
* Date: 4/23/09
*
* Purpose:
*
* This class handles the Capulin1 functions.
*
* Open Source Policy:
*
* This source code is Public Domain and free to any interested party.  Any
* person, company, or organization may do with it as they please.
*
*/

//-----------------------------------------------------------------------------

package chart.mksystems.hardware;

import chart.Log;
import chart.MessageLink;
import chart.ThreadSafeLogger;
import chart.mksystems.inifile.IniFile;
import chart.mksystems.settings.Settings;
import chart.mksystems.stripchart.ChartGroup;
import chart.mksystems.stripchart.Map2D;
import chart.mksystems.stripchart.Plotter;
import chart.mksystems.stripchart.StripChart;
import chart.mksystems.stripchart.Threshold;
import chart.mksystems.stripchart.Trace;
import chart.mksystems.stripchart.TraceData;
import chart.mksystems.threadsafe.SyncedBoolean;
import chart.mksystems.threadsafe.SyncedInteger;
import chart.mksystems.threadsafe.SyncedVariableSet;
import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
// class Capulin1
//
// This class creates and handles the hardware interface.
//

public class Capulin1 extends Object implements HardwareLink, MessageLink{

    Settings settings;
    ChartGroup chartGroups[];

    //debug mks - this is only for demo - delete later
    static int MONITOR_PACKET_SIZE = 20;
    byte[] monitorBuffer;
    boolean tdcFlagCaught = false;
    int showCount1 = 0;
    int showCount2 = 0;
    int reflectionTimer = 0;
    //debug mks end  - this is only for demo - delete later

    MapSourceBoard mapSourceBoards[] = null;
    int numberOfMapSourceBoards = 0;
    WallMapDataSaver wallMapDataSaver = null;

    int wallMapFileFormat;

    ThreadSafeLogger logger;

    String jobFileFormat, mainFileFormat;

    boolean utBoardsReady = false;
    boolean controlBoardsReady = false;

    boolean fpgaLoaded = false;

    SyncFlag dataChangedFlag;

    boolean simulate, simulateControlBoards, simulateUTBoards;

    ControlBoard[] controlBoards;
    int numberOfControlBoards = 1; //debug mks - read from config
    String controlBoardIP; //debug mks - get rid of this?

    String fpgaCodeFilename;

    boolean logEnabled = true;

    IniFile configFile;
    HardwareVars hdwVs;
    boolean simulationMode = false;
    int numberOfAnalogChannels;

    Channel[] channels;
    public int numberOfChannels;

    UTBoard[] utBoards;
    int numberOfUTBoards;

    JTextArea log;

    static int RUNTIME_PACKET_SIZE = 50;

    byte[] pktBuffer;

    int opMode = Hardware.STOPPED;
    boolean controlBoardInspectMode = false;

    //flags to signal "Main Thread" from the GUI thread to perform an action
    //involving the socket -- used to prevent thread collisions

    boolean performResetUTBoardsForNextRun = false;


    SyncedVariableSet syncedVarMgr;

    SyncedBoolean wallMapPacketsEnabled;

    //all boards (and thus all channels) must have the same rep rate
    //on startup, the UT boards each load a default rep rate from the
    //configuration file, but that can be overridden by the value in this
    //class if options set to do that
    SyncedInteger repRateInHertz;

//-----------------------------------------------------------------------------
// Capulin1::Capulin1 (constructor)
//
// The parameter configFile is used to load configuration data.  The IniFile
// should already be opened and ready to access.
//

Capulin1(IniFile pConfigFile, Settings pSettings, boolean pSimulationMode,
               int pNumberOfAnalogChannels, HardwareVars pHdwVs, JTextArea pLog,
               String pJobFileFormat, String pMainFileFormat)

{

    configFile = pConfigFile; settings = pSettings;
    simulationMode = pSimulationMode;
    numberOfAnalogChannels = pNumberOfAnalogChannels;
    hdwVs = pHdwVs;
    log = pLog;
    jobFileFormat = pJobFileFormat;
    mainFileFormat = pMainFileFormat;

}//end of Capulin1::Capulin1 (constructor)
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::init
//
// Initializes the object.  MUST be called by sub classes after instantiation.
//

@Override
public void init()
{

    syncedVarMgr = new SyncedVariableSet();
    wallMapPacketsEnabled = new SyncedBoolean(syncedVarMgr);
    wallMapPacketsEnabled.init();
    repRateInHertz = new SyncedInteger(syncedVarMgr);
    repRateInHertz.init();

    logger = new ThreadSafeLogger(log);

    pktBuffer = new byte[RUNTIME_PACKET_SIZE];

    dataChangedFlag = new SyncFlag();

    //load configuration settings
    configure(configFile);

}//end of Capulin1::init
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::configure
//
// Loads configuration settings from the configuration.ini file.  These set
// the number and style of channels, gates, etc.
// The various child objects are then created as specified by the config data.
//

private void configure(IniFile pConfigFile)
{

    simulateControlBoards =
       pConfigFile.readBoolean("Hardware", "Simulate Control Boards", false);

    simulateUTBoards =
            pConfigFile.readBoolean("Hardware", "Simulate UT Boards", false);

    //if any simulation is active, set the simulate flag true
    if (simulateControlBoards || simulateUTBoards) {simulate = true;}

    numberOfUTBoards =
                     pConfigFile.readInt("Hardware", "Number of UT Boards", 1);

    if (numberOfUTBoards > 255) {numberOfUTBoards = 255;}

    numberOfChannels =
                pConfigFile.readInt("Hardware", "Number of Analog Channels", 1);

    controlBoardIP = pConfigFile.readString(
                      "Hardware", "Control Board IP Address", "169.254.56.11");

    fpgaCodeFilename = pConfigFile.readString(
                        "Hardware", "UT FPGA Code Filename", "not specified");

    if (numberOfChannels > 1500) {numberOfUTBoards = 1500;}

    String value = pConfigFile.readString(
                         "Data Output", "Wall Map File Format", "IRNDT Text");

    parseWallMapFileFormat(value);

    //create and setup the Control boards
    configureControlBoards();

    //create and setup the UT boards
    configureUTBoards();

    //create and setup the channels
    configureChannels();

}//end of Capulin1::configure
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::parseWallMapFileFormat
//
// Parses the string read from the config file to set the map file format.
//

void parseWallMapFileFormat(String pValue)
{

    if (pValue.equalsIgnoreCase("IRNDT Binary")) {
        wallMapFileFormat = WallMapDataSaver.IRNDT_BINARY_FORMAT;
    }
    else
    if (pValue.equalsIgnoreCase("IRNDT Text")) {
        wallMapFileFormat = WallMapDataSaver.IRNDT_TEXT_FORMAT;
    }
    else
    if (pValue.equalsIgnoreCase("Tuboscope Binary")) {
        wallMapFileFormat = WallMapDataSaver.TUBO_BINARY_FORMAT;
    }

}//end of Capulin1::parseWallMapFileFormat
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::createWallMapDataSaver
//
// Creates and initializes an object to save map data from Board objects.
//

public void createWallMapDataSaver()
{

    numberOfMapSourceBoards = countMapSourceBoards();

    //if no boards are map sources, ignore map operations
    if (numberOfMapSourceBoards == 0){ return; }

    mapSourceBoards = new MapSourceBoard[numberOfMapSourceBoards];

    for (int i = 0; i < mapSourceBoards.length; i++){
        mapSourceBoards[i] = new MapSourceBoard();
    }

    //init map source objects with the boards assigned to each map channel;
    //they are in random order in the array so use findBoard to locate each;
    //they will then be in mapSoureBoards array in order of their map channel

    for (int i = 0; i < mapSourceBoards.length; i++){
        mapSourceBoards[i].init(findBoardByTargetMapChannel(i));
    }

    //currently, the system only handles a single mapping channel per board
    //so the number of source boards is also the number of source channels

    int numberOfMapSourceHardwareChannels = numberOfMapSourceBoards;

    //if a dataBuffer has been created, create a file saver for it
    wallMapDataSaver = new WallMapDataSaverTuboBinary(settings,
                wallMapFileFormat, numberOfMapSourceHardwareChannels , true);

    //pass a reference to the source boards to the data saver
    wallMapDataSaver.init(mapSourceBoards);

}//end of Capulin1::createWallMapDataSaver
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::connect
//
// Establishes a connection with each board.
//
// Notes about the IP Address and Subnet Mask
// When a Windows computer is connected to the local network with only the
// Rabbit modules, it will assign itself an IP Address such as 169.254.56.136
// and a Mask Subnet of 255.255.0.0 because there is no DHCP server to assign
// these values to the hosts on the network.
//
// Each host (Windows computer and Rabbits) uses the Subnet Mask to determine
// if the computer it is connecting to is on the same subnet.  If it is on the
// same subnet, the data is sent directly.  If not, the computer sends it
// through a router (default gateway).  The part of the mask with ones is the
// part which specifies the local subnet - this part should match in all hosts
// on the subnet.  The Subnet Mask should also be the same in all hosts so they
// all understand which computers are on the same subnet.
//
// To use a Windows computer to talk to the Rabbits, you can either manually
// set the IP Address and Subnet Mask to match the Rabbits or set the Rabbits
// to match the Windows computer.  Since the Windows computer may also be used
// on other networks, it is inconvenient to switch back and forth; thus the
// Rabbits in this system use values which match the typical Windows computer.
//
// When the Windows computer is connected without manually setting the
// IP Address and Subnet Mask, a yellow warning sign will be displayed by the
// network icon and the warning "Limited or no connectivity" will be shown.
// This does not affect communication with the Rabbits and the warning may be
// ignored.

@Override
public void connect() throws InterruptedException
{

    connectControlBoard();

    connectUTBoards();

}//end of Capulin1::connect
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::connectControlBoards
//
// Opens a TCP/IP connection with the Control Board.
//

public void connectControlBoard() throws InterruptedException
{

    //displays message on bottom panel of IDE
    logger.logMessage("Broadcasting greeting to all Control boards...\n");

    DatagramSocket socket;

    //debug mks - need separate variable for simulated control boards ~ UTSimulator.instanceCounter = 0; //reset simulated board counter

    try{
        if (!simulateControlBoards) {socket = new DatagramSocket(4445);}
        else {socket = new UDPSimulator(4445, "Control Board present...");}

    }
    catch (IOException e) {
        System.err.println(getClass().getName() + " - Error: 204");
        logger.logMessage("Couldn't create Control broadcast socket.\n");
        return;
    }

    int loopCount = 0;
    String castMsg = "Control Board Roll Call";
    byte[] outBuf;
    outBuf = castMsg.getBytes();
    InetAddress group;
    DatagramPacket outPacket;
    byte[] inBuf = new byte[256];
    DatagramPacket inPacket;
    inPacket = new DatagramPacket(inBuf, inBuf.length);
    int responseCount = 0;
    String response;

    try{
        group = InetAddress.getByName("230.0.0.1");
    }
    catch (UnknownHostException e){
        System.err.println(getClass().getName() + " - Error: 224");
        socket.close();
        return;
    }

    outPacket = new DatagramPacket(outBuf, outBuf.length, group, 4446);

    //force socket.receive to return if no packet available within 1 millisec
    try{
        socket.setSoTimeout(1000);
    }
    catch(SocketException e){
        System.err.println(getClass().getName() + " - Error: 236");
    }

    //broadcast the roll call greeting several times - bail out when expected
    //number of different Control boards have responded
    while(loopCount++ < 5 && responseCount < numberOfControlBoards){

        try {socket.send(outPacket);}
        catch(IOException e) {
            System.err.println(getClass().getName() + " - Error: 245");
            socket.close();
            return;
        }

        waitSleep(1000); //sleep to delay between broadcasts

        //check for response packets from the remotes
        try{
            //read response packets until a timeout error exception occurs or
            //until expected number of different Control boards have responded
            while(responseCount < numberOfControlBoards){

                socket.receive(inPacket);

                //store each new ip address in a Control board object
                for (int i = 0; i < numberOfControlBoards; i++){

                    //if a ut board already has the same ip, don't save it
                    //this might occur if a board responds more than once as the
                    //host repeatedly broadcasts the greeting
                    //since the first utBoard objects in the array are filled
                    //first -- this will catch duplicates

                    if (controlBoards[i].ipAddr != null &&
                            controlBoards[i].ipAddr == inPacket.getAddress()){
                        break;
                    }

                    //only boards which haven't been already seen make it here

                    //if an unused board reached, store ip there
                    if (controlBoards[i].ipAddr == null){

                        //store the ip address in the unused object
                        controlBoards[i].setIPAddr(inPacket.getAddress());

                        //count unique IP address responses
                        responseCount++;

                        //convert the response packet to a string
                        response = new String(
                                inPacket.getData(), 0, inPacket.getLength());

                        //display the greeting string from the remote
                        logger.logMessage(
                             controlBoards[i].ipAddrS + "  " + response + "\n");

                        break;
                    }
                }//for (int i = 0; i < numberOfControlBoards; i++)
            }//while(true)
        }//try
        catch(IOException e){
            //this reached if receive times out -- take no action
        }
    }// while(loopCount...

    socket.close();

    //bail out if no boards responded
    if (responseCount == 0) {return;}

    //start the run method of each ControlBoard thread class - the run method
    //makes the TCP/IP connections and uploads FPGA and DSP code simultaneously
    //to shorten start up time

    if (responseCount > 0){
        for (int i = 0; i < numberOfControlBoards; i++){
            //pass the Runnable interfaced controlBoard object to a thread and
            //start the run function of the controlBoard will peform the
            //connection tasks
            Thread thread = new Thread(controlBoards[i], "Control Board " + i);
            thread.start();
        }
    }//if (responseCount > 0)

    //call each board and wait for it to complete its connection & initial setup
    //note that this object cannot even enter the synchronized method
    //waitForconnectCompletion until controlBoard.connect completes because
    //connect is also synchronized

    for (int i = 0; i < numberOfControlBoards; i++) {
        controlBoards[i].waitForConnectCompletion();
    }

    logger.logMessage("All Control boards ready.\n");

    //initialize each Control board
    initializeControlBoards();

}//end of Capulin1::connectControlBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::connectUTBoards
//
// Connects with and sets up all UT boards.
//

public synchronized void connectUTBoards() throws InterruptedException
{

    //displays message on bottom panel of IDE
    logger.logMessage("Broadcasting greeting to all UT boards...\n");

    DatagramSocket socket;

    UTSimulator.instanceCounter = 0; //reset simulated board counter

    // because the UTSimulator class does not currently simulate the FPGA load,
    // the response string is set to show that the FPGA is already loaded so
    // that no attempt is made to perform the load -- the string is passed to
    // the UDPSimulator here so that simulator will return it to simulate wake
    // up response string expected

    try{
        if (!simulateUTBoards) {
            socket = new DatagramSocket(4445);
        }
        else {
            socket = new UDPSimulator(4445, "UT board present, FPGA loaded...");
        }
    }
    catch (IOException e) {
        System.err.println(getClass().getName() + " - Error: 351");
        logger.logMessage("Couldn't create UT broadcast socket.\n");
        return;
    }


    int loopCount = 0;
    String castMsg = "UT Board Roll Call";
    byte[] outBuf;
    outBuf = castMsg.getBytes();
    InetAddress group;
    DatagramPacket outPacket;
    byte[] inBuf = new byte[256];
    DatagramPacket inPacket;
    inPacket = new DatagramPacket(inBuf, inBuf.length);
    int responseCount = 0, fpgaLoadedCount = 0;
    String response;

    try{
        group = InetAddress.getByName("230.0.0.1");
    }
    catch (UnknownHostException e) {
        System.err.println(getClass().getName() + " - Error: 373");
        socket.close();
        return;
    }

    outPacket = new DatagramPacket(outBuf, outBuf.length, group, 4446);

    //force socket.receive to return if no packet available quickly
    try{
        socket.setSoTimeout(1000);
    }
    catch(SocketException e){
        System.err.println(getClass().getName() + " - Error: 385");
    }

    //broadcast the roll call greeting repeatedly - bail out when the expected
    //number of different UT boards have responded
    while(loopCount++ < 5 && responseCount < numberOfUTBoards){

        try {socket.send(outPacket);}
        catch(IOException e) {
            System.err.println(getClass().getName() + " - Error: 394");
            socket.close();
            return;
        }

        waitSleep(1000); //sleep to delay between broadcasts

        //check for response packets from the remotes
        try{
            //read response packets until a timeout error exception occurs or
            //or until the expected number of different UT boards have responded
            while(responseCount < numberOfUTBoards){

                socket.receive(inPacket);

                //store each new ip address in a UT board object
                for (int i = 0; i < numberOfUTBoards; i++){

                    //if a ut board already has the same ip, don't save it
                    //this might occur if a board responds more than once as the
                    //host repeatedly broadcasts the greeting
                    //since the first utBoard objects in the array are filled
                    //first -- this will catch duplicates

                    if (utBoards[i].ipAddr != null &&
                            utBoards[i].ipAddr.equals(inPacket.getAddress())){
                        break;
                    }

                    //only boards which haven't been already seen make it here

                    //if an unused board reached, store ip there
                    if (utBoards[i].ipAddr == null){

                        //store the ip address in the unused object
                        utBoards[i].setIPAddr(inPacket.getAddress());

                        //count unique IP address responses
                        responseCount++;

                        //convert the response packet to a string
                        response = new String(
                                inPacket.getData(), 0, inPacket.getLength());

                        //count number of boards already having loaded FPGA's
                        if (response.contains("FPGA loaded")){
                            fpgaLoadedCount++;
                        }

                        //display the greeting string from the remote
                        logger.logMessage(
                                utBoards[i].ipAddrS + "  " + response + "\n");

                        //stop scanning the boards now that ip saved
                        break;
                    }
                }//for (int i = 0; i < numberOfUTBoards; i++)

            }//while(true)
        }//try
        catch(IOException e){
            //this reached if receive times out -- take no action
        }
    }// while(loopCount...

    //if not all boards reported that their FPGAs were already loaded, load them
    //all now

    /*
    if (fpgaLoadedCount < numberOfUTBoards)
        loadFPGAViaUDP(socket);
    else
        sendRunTriggers(socket, outPacket);

     */

    sendRunTriggers(socket, outPacket); //debug mks -- remove this put code above back in

    socket.close();

    //bail out if no boards responded
    if (responseCount == 0) {return;}

    //start the run method of each UTBoard thread class - the run method makes
    //the TCP/IP connections and uploads FPGA and DSP code simultaneously to the
    //different boards to shorten start up time

    if (responseCount > 0){
        for (int i = 0; i < numberOfUTBoards; i++){
            //pass the Runnable interfaced utBoard object to a thread and start
            //it -- the run function of the utBoard will peform the connection
            //tasks
            Thread thread = new Thread(utBoards[i], "UT Board " + i);
            thread.start();
        }
    }//if (responseCount > 0)

    //call each board and wait for it to complete its connection & initial setup
    //note that this object cannot even enter the synchronized method
    //waitForconnectCompletion until utBoard.connect completes because connect
    //is also synchronized

    for (int i = 0; i < numberOfUTBoards; i++) {
        utBoards[i].waitForConnectCompletion();
    }

    logger.logMessage("All UT boards connected...\n");

    //Connect the UT boards to their software channels.
    //Set the utBoard pointer for each channel to the utBoard object which has a
    //matching chassis and board addresses.
    //The channel gets its chassis and board addresses from the
    //"configuration.ini" file while the utBoard object gets its addresses from
    //the switches on the motherboard into which the board is plugged.
    //The address switches may be overridden by an entry in the file
    //"Board Slot Overrides.config" which will force a board with a particular
    //IP address to a specific chassis and board address.  This allows boards
    //with damaged FPGA address inputs or motherboards with bad address switches
    //to be utilized.

    //use the ready flag for each UT board - don't attach to a board which was
    //not successfully setup because it's addresses aren't trusted in that case

    for (int i = 0; i < numberOfUTBoards; i++){
        if (utBoards[i].ready){
            for (int j = 0; j < numberOfChannels; j++) {
                if (channels[j].chassisAddr == utBoards[i].chassisAddr
                        && channels[j].slotAddr == utBoards[i].slotAddr) {
                    channels[j].utBoard = utBoards[i];
                }
            }
        }
        else {
            System.out.println("UT Board " + i
                                 + " not ready at time of channel assignment");
        }

    }//for (int i = 0;...

    //initialize each UT board
    initializeUTBoards();

    waitSleep(3000); //sleep for a bit to allow DSPs to start up

    logger.logMessage("All UT boards initialized...\n");

    //set up each channel
    // CAUTION: the next should only be called after each channel's loadCalFile
    // function has been called because it uses those values.  The main file
    // calls the loadCalFile function soon after it creates the Hardware object
    // while this part of the code isn't reached until all the UT boards have
    // been initialized.  Thus the call to loadCalFile SHOULD get completed
    // before initializeChannels - always true?

    initializeChannels();

    logger.logMessage("All channels initialized...\n");
    logger.logMessage("All UT boards ready.\n");

}//end of Capulin1::connectUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1:initializeUTBoards
//
// Sets up each UT board with various settings.
//

public void initializeUTBoards()
{

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i] != null) { utBoards[i].initialize();}
    }

    linkPlottersToBoards();

    //disable async sending of wall map data packets by the UTBoards
    wallMapPacketsEnabled.setValue(false, true);

    //create object to save map data collected by boards
    createWallMapDataSaver();

}//end of Capulin1::initializeUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::linkPlottersToBoards
//
// Connect plotters to boards.
//
// Some plotters (Trace, Map2D, etc.) are connected to a board independent of
// any channel connection. The board may use multiple channels to create a
// data set for display.
//
// The plotter is connected to a board by specifying the board's slot address
// in the plotter's config file section. The boards are randomly placed in the
// board array based on which board answers roll call first, so the array must
// be scanned looking for which boards have which slot numbers.
//
// wip mks -- move the loop stuff to ChartGroups then we don't need imports
// for StripChart, Trace, etc. Make a function to iterate through all
// plotters in ChartGroups.
//

private void linkPlottersToBoards()
{

    for (int i = 0; i < chartGroups.length; i++){

        ChartGroup cg = chartGroups[i];

        for (int j = 0; j < cg.getNumberOfStripCharts(); j++){

            StripChart sc = cg.getStripChart(j);

            for (int k = 0; k < sc.getNumberOfPlotters(); k++){

                Plotter p = sc.getPlotter(k);

               for (int l = 0; l < numberOfUTBoards; l++) {
                   UTBoard utb = utBoards[l];
                   if(isPlotterConnectedToBoard(p, utb)){
                       utb.setMap2D((Map2D)p);
                    }
                }
            }
        }
    }



}//end of Capulin1::linkPlottersToBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::isPlotterConnectedToBoard
//
// Returns true if the pPlotter data source board chassis and slot match
// those of pBoard.
//

public boolean isPlotterConnectedToBoard(Plotter pPlotter, Board pBoard)
{

    if (pBoard.compareChassisAndSlot(
        pPlotter.getDataSourceBoardChassis(),
        pPlotter.getDataSourceBoardSlot())){

        return(true);
    }
    else{
        return(false);
    }

}//end of Capulin1::isPlotterConnectedToBoard
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::initializeControlBoards
//
// Sets up each Control board with various settings.
//

public void initializeControlBoards()
{

    for (int j = 0; j < numberOfControlBoards; j++) {
        if (controlBoards[j] != null) {
            controlBoards[j].initialize();
        }
    }

}//end of Capulin1::initializeControlBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1:verifyAllDSPCode2
//
// Verifies that the code in each DSP matches the file.  Used to check for
// transmission or corruption errors.
//
// This function checks byte by byte and is VERY slow.
//

@Override
public void verifyAllDSPCode2()
{

    for (int j = 0; j < numberOfUTBoards; j++) {
        if (utBoards[j] != null) {
            utBoards[j].verifyAllDSPCode2();
        }
    }

}//end of Capulin1::verifyAllDSPCode2
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1:initializeChannels
//
// Sets up each channel with various settings.
//

public void initializeChannels()
{

    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].initialize();
    }

}//end of Capulin1::initializeChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1:sendDataChangesToRemotes
//
// If any synced data or command flag has been changed, sends the changes or
// commands to the remotes.
//
// These values are often changed by the user clicking on controls which are
// handled in the main GUI thread. Synced variables are used to avoid
// collisions with the thread handling the UTBoards which also calls this
// function.
//
// This method should only be invoked by the "Main Thread".
//

@Override
public void sendDataChangesToRemotes()
{

    //perform any pending actions required by the "Main Thread"
    handleSyncedCommands();

    //handle data changes in all the channels

    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].sendDataChangesToRemotes();
    }

    //handle data changes in this class

    if (!syncedVarMgr.getDataChangedMaster()) {return;}

    if (wallMapPacketsEnabled.getDataChangedFlag()){enableWallMapPackets();}

    if (repRateInHertz.getDataChangedFlag()) {sendRepRate();}

}//end of Capulin1::sendDataChangesToRemotes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::handleSyncedCommands
//
// Perform any pending actions which are required for the "Main Thread". This
// method should only be invoked by the "Main Thread".
//
// The GUI thread should not directly invoke methods which access the sockets
// to the remotes. Instead, the GUI thread sets signal flags to trigger the
// "Main Thread" to perform the action instead.
//

public void handleSyncedCommands()
{

    if(performResetUTBoardsForNextRun){
        performResetUTBoardsForNextRun = false;
        //prepare the UTBoards for a new run
        resetUTBoardsForNextRun();
    }

}//end of Capulin1::handleSyncedCommands
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::driveSimulation
//
// Drive any simulation functions if they are active.  This function is usually
// called from a thread.
//

@Override
public void driveSimulation()
{

    if (simulateControlBoards) {
        for (int i = 0; i < numberOfControlBoards; i++) {
            controlBoards[i].driveSimulation();
        }
    }

    if (simulateUTBoards) {
        for (int i = 0; i < numberOfUTBoards; i++) {
            utBoards[i].driveSimulation();
        }
    }

}//end of Capulin1::driveSimulation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::logStatus
//
// Writes various status and error messages to the log window.
//

@Override
public void logStatus(Log pLogWindow)
{

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i]!= null) {
        utBoards[i].logStatus(pLogWindow);
        }
    }

}//end of Capulin1::logStatus
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::shutDown
//
// This function should be called before exiting the program.  Overriding the
// "finalize" method does not work as it does not get called reliably upon
// program exit.
//

@Override
public void shutDown()
{

    for (int i = 0; i < numberOfControlBoards; i++) {
        if (controlBoards[i]!= null) {
            controlBoards[i].shutDown();
        }
    }

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i]!= null) {
            utBoards[i].shutDown();
        }
    }

}//end of Capulin1::shutDown
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::loadCalFile
//
// This loads the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may load their
// own data.
//

@Override
public void loadCalFile(IniFile pCalFile)
{

    repRateInHertz.setValue(
          pCalFile.readInt("Hardware", "Pulse Rep Rate in Hertz", 2000), true);

    // call each channel to load its data
    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].loadCalFile(pCalFile);
    }

}//end of Capulin1::loadCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::saveCalFile
//
// This saves the file used for storing calibration information pertinent to a
// job, such as gains, offsets, thresholds, etc.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

@Override
public void saveCalFile(IniFile pCalFile)
{

    pCalFile.writeInt(
            "Hardware", "Pulse Rep Rate in Hertz", repRateInHertz.getValue());

    // call each channel to save its data
    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].saveCalFile(pCalFile);
    }

}//end of Capulin1::saveCalFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::saveCalFileHumanReadable
//
// This saves a subset of the calibration data, the values of which affect
// the inspection process.
//
// The data is saved in a human readable format.
//
// Each object is passed a pointer to the file so that they may save their
// own data.
//

@Override
public void saveCalFileHumanReadable(BufferedWriter pOut) throws IOException
{

    //print a header

    pOut.write("----- Channel Settings -----");
    pOut.newLine(); pOut.newLine();

    // call each channel to save its data
    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].saveCalFileHumanReadable(pOut);
    }

}//end of Capulin1::saveCalFileHumanReadable
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::displayMessages
//
// Displays any messages received from the remote.
//
// NOTE: If a message needs to be displayed by a thread other than the main
// Java thread, use threadSafeLog instead.
//

@Override
public void displayMessages()
{

    //if another function is using the socket, don't read messages from it
    if (!logEnabled) {return;}

}//end of Capulin1::displayMessages
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::updateRabbitCode
//
// Installs new firmware on the Rabbit micro-controllers.  Which boards are
// updated is selected by pWhichRabbits -- all, UT, or Control boards.
//
// wip mks -- need to be able to load an individual Rabbit so if one out of
// ten fails, don't need to reload all ten to fix the one
//

@Override
public void updateRabbitCode(int pWhichRabbits)
{

    logger.logMessage("\n");
    logger.logMessage("-- Updating Rabbit Firmware --" + "\n\n");

    if (pWhichRabbits == Hardware.UT_RABBITS
            || pWhichRabbits == Hardware.ALL_RABBITS){
        for (int j = 0; j < numberOfUTBoards; j++) {
            if (utBoards[j] != null) {utBoards[j].installNewRabbitFirmware();}
        }
    }

    if (pWhichRabbits == Hardware.CONTROL_RABBITS
                                || pWhichRabbits == Hardware.ALL_RABBITS){
        for (int j = 0; j < numberOfControlBoards; j++) {
            if (controlBoards[j] != null) {
                controlBoards[j].installNewRabbitFirmware();
            }
        }
    }

    logger.logMessage("\n");
    logger.logMessage("-- Rabbit Firmware Update Complete --");


}//end of Capulin1::updateRabbitCode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::doTasks
//
// Should be called by a timer so that various tasks can be performed as
// necessary.  Since Java doesn't update the screen during calls to the user
// software, it is necessary to execute tasks in a segmented fashion if it
// is necessary to display status messages along the way.
//

@Override
public void doTasks()
{

    displayMessages();

}//end of Capulin1::doTasks
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::setMode
//
// Sets the mode to INSPECT, SCAN, STOPPED, etc.
//
// In scan mode, the data is transferred to the trace buffers in "free run"
// mode, regardless of the movement of the test piece.  This mode is used for
// calibration, troubleshooting, etc. and utilizes the freeRun method.
//
// Note that this not the same as the "free run" used when simulating encoders
// for systems that don't have any.  That is done in the "run" mode with
// data simulating encoder movement.  In that mode, test pieces are tracked,
// segregated, and data is saved.
//

@Override
public void setMode(int pOpMode)
{

    opMode = pOpMode;

    if (opMode == Hardware.SCAN){

        //set mode for Boards to advance map plotters they control -- map
        //plotters will advance across screen with each revolution
        setMapAdvanceModes(Board.ADVANCE_ON_TDC_CODE);

        //signal "Main Thread" to reset
        performResetUTBoardsForNextRun = true;

        //enable async sending of wall map data packets by the UTBoards
        wallMapPacketsEnabled.setValue(true, false);

    }

    //for INSPECT mode, which uses hardware encoder and control signals, perform
    //initialization
    if (opMode == Hardware.INSPECT){

        //system waits until it receives flag that head is off the pipe or no
        //pipe is in the system
        hdwVs.waitForOffPipe = true;

        //track from photo eye clear to end of pipe
        hdwVs.trackToEndOfPiece = false;

        //use a flag and a tracking counter to indicate when head is still near
        //the beginning of the piece
        hdwVs.nearStartOfPiece = true;
        hdwVs.nearStartOfPieceTracker = hdwVs.nearStartOfPiecePosition;

        //flags set true later when end of pipe is near
        hdwVs.trackToNearEndofPiece = false;
        hdwVs.nearEndOfPiece = false;

        //ignore the Inspect status flags until a new packet is received
        controlBoards[0].setNewInspectPacketReady(false);

        //force send of an Inspect packet so all the flags will be up to date
        controlBoards[0].requestInspectPacket();

        controlBoards[0].startInspect();
        controlBoardInspectMode = true; //flag that board is in inspect mode

        //set mode for Boards to advance map plotters they control
        //map plotter advance controlled by this object monitoring encoder
        setMapAdvanceModes(Board.ADVANCE_BY_CONTROLLER);

        //signal "Main Thread" to reset
        performResetUTBoardsForNextRun = true;

        //enable async sending of wall map data packets by the UTBoards
        wallMapPacketsEnabled.setValue(true, false);

    }

    if (opMode == Hardware.STOPPED){

        //set mode for Boards to advance map plotters they control
        setMapAdvanceModes(Board.ADVANCE_NEVER);

        //disable async sending of wall map data packets by the UTBoards
        wallMapPacketsEnabled.setValue(false, false);

        //deactivate inspect mode in the control board(s)
        //wip mks -- should this be a synced variable or use a flag to
        // signal "Main Thread" to do this? see performResetUTBoardsForNextRun
        // for an example

        if (controlBoardInspectMode) {controlBoards[0].stopInspect();}

    }

}//end of Capulin1::setMode
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::startMonitor
//
// Places the Control board in Monitor status and displays the status of
// various I/O as sent back from the Control board.
//

@Override
public void startMonitor()
{

    controlBoards[0].startMonitor();

}//end of Capulin1::startMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::stopMonitor
//
// Takes the Control board out of monitor mode.
//

@Override
public void stopMonitor()
{

    controlBoards[0].stopMonitor();

}//end of Capulin1::stopMonitor
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getMonitorPacket
//
// Stuffs I/O status received from the remote into an array.
// If pRequestPacket is true, then a packet is requested every so often.
// If false, then packets are only received when the remote computer sends
// them.
//

@Override
public byte[] getMonitorPacket(boolean pRequestPacket)
{

    return controlBoards[0].getMonitorPacket(pRequestPacket);

}//end of Capulin1::getMonitorPacket
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::zeroEncoderCounts
//
// Sends command to zero the encoder counts.
//

@Override
public void zeroEncoderCounts()
{

    controlBoards[0].zeroEncoderCounts();

}//end of Capulin1::zeroEncoderCounts
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::pulseOutput1
//
// Pulses output 1.
//

@Override
public void pulseOutput1()
{

    controlBoards[0].pulseOutput();

}//end of Capulin1::pulseOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::turnOnOutput1
//
// Turn on output 1.
//

@Override
public void turnOnOutput1()
{

    controlBoards[0].turnOnOutput();

}//end of Capulin1::turnOnOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::turnOffOutput1
//
// Turn off output 1.
//

@Override
public void turnOffOutput1()
{

    controlBoards[0].turnOffOutput();

}//end of Capulin1::turnOffOutput1
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::requestAScan
//
// Requests an AScan dataset for the specified channel from the appropriate
// remote device.
//

@Override
public void requestAScan(int pChannel)
{

    channels[pChannel].requestAScan();

}//end of Capulin1::requestAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getAScan
//
// Retrieves an AScan dataset for the specified channel.
//

@Override
public AScan getAScan(int pChannel)
{

    return channels[pChannel].getAScan();

}//end of Capulin1::getAScan
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::requestPeakData
//
// Sends a request to the remote device for a peak data packet for the
// specified channel.
//

@Override
public void requestPeakData(int pChannel)
{

    channels[pChannel].requestPeakData();

}//end of Capulin1::requestPeakData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::requestPeakDataForAllBoards
//
// Requests peak data for all channels on all enabled UT boards.
//
// The channel numbers sent to requestPeakData4 refer to the four analog
// channels on each board.  The utBoard objects have links back to the logical
// channels for each analog channel.
//

@Override
public void requestPeakDataForAllBoards()
{

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i] != null && utBoards[i].isEnabled()) {
            utBoards[i].requestPeakData4(0, 1, 2, 3);
        }
    }

}//end of Capulin1::requestPeakDataForAllBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getChannelData
//
// Returns data for channel specified by pChannel.
//
//

@Override
public int getChannelData(int pChannel, int pSimDataType)
{

    //if in simulation mode, return simulated data
    //if(simulationMode) return(simulateChannelData(pChannel, pSimDataType));

    return(50);

}//end of Capulin1::getChannelData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::prepareAnalogData
//
// Allows each enabled UT board to process any available data packets until it
// processes its first Peak Data packet.
//
// If any UT board encounters and processes a Peak Data packet, this function
// returns true to signal that some data is available.  It is up to the
// calling function to verify which channels have new data.
//
// If no Peak Data packet is processed, function returns false.
//
// Returns true if new data is available, false if not.
//
// debug mks - This function processes data until the first peak data packet
//  for each board is encountered.  If the data is being pushed faster than
//  this functions is called, the incoming data packets will accumulate.  This
//  function needs to be changed to process data until all waiting packets are
//  processed -- but that will require peaks to be stored.
//

@Override
public boolean prepareAnalogData()
{

    boolean atLeastOnePeakDataPacketProcessed = false;

    //give each UT board a chance to process data packets from the remotes
    for (int i = 0; i < numberOfUTBoards; i++){

        //if data packet(s) are available, process them for each UT board until
        //a peak data packet for each board is encountered -
        //process no more than one peak data packet for each UT board so the
        //peak won't be overwritten by a new packet before it can be transferred
        //to the traces - if a peak data packet is processed for any UT board,
        //return true to signal that some data is ready to be processed and the
        //peak data should be transferred

        //calling processDataPacketsUntilPeakPacket will also result in other
        //types of packets being processed as well, such as A-Scan packets -
        //thus calling this (prepareData) function serves to handle all packet
        //types - calling it repeatedly will serve to keep incoming data packets
        //processed, the only catch being that processing will be stopped
        //briefly when a peak data packet is encountered so that the peak may
        //be handled

        //NOTE: The processDataPacketsUntilPeakPacket function returns
        //immediately if no packets are available, so the name is not entirely
        //accurate - it will not wait until the first packet is encountered if
        //there are no packets waiting.  Also, any other packet types waiting
        //will be processed even if there is no peak data packet in the queue.

        if (utBoards[i].isEnabled() &&
                    utBoards[i].processDataPacketsUntilPeakPacket() == 1) {
            atLeastOnePeakDataPacketProcessed = true;
        }

    }

    return atLeastOnePeakDataPacketProcessed;

}//end of Capulin1::prepareAnalogData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::prepareControlData
//
// Allows each Control board to process any available data packets until it
// processes its first Encoder packet.
//
// If any Control board encounters and processes an Encoder packet, this
// function returns true to signal that some data is available.  It is up to
// the calling function to verify which channels have new data.
//
// If no Encoder packet is processed, function returns false.
//
// Returns true if new data is available, false if not.
//
// debug mks - This function processes data until the first Encoder packet
//  for each board is encountered.  If the data is being pushed faster than
//  this functions is called, the incoming data packets will accumulate.  This
//  function needs to be changed to process data until all waiting packets are
//  processed.
//

@Override
public boolean prepareControlData()
{

    boolean atLeastOneEncoderPacketProcessed = false;

    //give each Control board a chance to process data packets from the remotes
    for (int i = 0; i < numberOfControlBoards; i++){

        //this function was copied from prepareAnalogData function which expects
        //a peak packet from each board -- each control board may not send
        //encoder packets, but may not be a problem as that means all the other
        //packet types will be processed anyway -- most systems only have one
        //control board so the point is generally moot

        //if data packet(s) are available, process them for each board until an
        //encoder packet for each board is encountered -
        //process no more than one encoder packet for each Control board so the
        //value won't be overwritten by a new packet before it can be
        //transferred to the traces - if an encoder packet is processed for any
        //board, return true to signal that some data is ready to be processed
        //and the control data should be transferred

        //calling processDataPacketsUntilEncoderPacket will also result in
        //other types of packets being processed as well, such as flag packets -
        //thus calling this (prepareData) function serves to handle all packet
        //types - calling it repeatedly will serve to keep incoming data packets
        // processed, the only catch being that processing will be stopped
        //briefly when an encoder packet is encountered so that the encoder data
        //may be handled

        //NOTE: The processDataPacketsUntilEncoderPacket function returns
        //immediately if no packets are available, so the name is not entirely
        //accurate - it will not wait until the first packet is encountered if
        //there are no packets waiting.  Also, any other packet types waiting
        //will be processed even if there is no encoder packet in the queue.

        if (controlBoards[i].processDataPacketsUntilEncoderPacket() == 1) {
            atLeastOneEncoderPacketProcessed = true;
        }

    }

    return atLeastOneEncoderPacketProcessed;

}//end of Capulin1::prepareControlData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getRepRateInHertz
//
// Returns the rep rate in Hertz used for all boards/channels.
//

@Override
public int getRepRateInHertz()
{

    return(repRateInHertz.getValue());

}//end of Capulin1::getRepRateInHertz
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getNumberOfChannels
//
// Returns the number of channels.
//

@Override
public int getNumberOfChannels()
{

    return numberOfChannels;

}//end of Capulin1::getNumberOfChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getChannels
//
// Returns a reference to the array of channels.
//

@Override
public Channel[] getChannels()
{

    return channels;

}//end of Capulin1::getChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getGate
//
// Returns a reference to the specified gate.
//

@Override
public UTGate getGate(int pChannel, int pGate)
{

    return channels[pChannel].getGate(pGate);

}//end of Capulin1::getGate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getNumberOfGates
//
// Returns the number of gates for the specified channel.

@Override
public int getNumberOfGates(int pChannel)
{

    return channels[pChannel].getNumberOfGates();

}//end of Capulin1::getNumberOfGates
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getNumberOfUTBoards
//
// Returns the number of UTBoards.
//

@Override
public int getNumberOfUTBoards()
{

    return(numberOfUTBoards);

}//end of Capulin1::getNumberOfUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getNewData
//
// Calls the getNewData function for the specified channel and gate.  See the
// channel and gate classes for more info.
//

@Override
public boolean getNewData(int pChannel, int pGate, HardwareVars hdwVs)
{

    return channels[pChannel].getNewData(pGate, hdwVs);

}//end of Capulin1::getNewData
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getTrace
//
// Calls the getTrace function for the specified channel and gate.  See the
// channel and gate classes for more info.
//

@Override
public Trace getTrace(int pChannel, int pGate)
{

    return channels[pChannel].getTrace(pGate);

}//end of Capulin1::getTrace
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::linkPlotters
//
// This function is called by Plotters (Traces, etc.) to link their buffers to
// specific hardware channels/gates and give a link back to variables in the
// Plotter object.
//

@Override
public void linkPlotters(int pChartGroup, int pChart, int pTrace,
            TraceData pTraceData, Threshold[] pThresholds, int pPlotStyle,
                                                               Trace pTracePtr)
{

    for (int i = 0; i < numberOfChannels; i++) {
        channels[i].linkPlotters(pChartGroup, pChart, pTrace, pTraceData,
                                        pThresholds, pPlotStyle, pTracePtr);
    }

}//end of Capulin1::linkPlotters
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::setChartGroups
//
// Sets the chartGroups variable.
//

@Override
public void setChartGroups(ChartGroup pChartGroups [])
{

    chartGroups = pChartGroups;

}//end of Capulin1::setChartGroups
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::configureControlBoards
//
// Loads configuration settings from the configuration.ini file relating to
// the boards and creates/sets them up.
//

private void configureControlBoards()
{

    //create an array of boards per the config file setting
    if (numberOfControlBoards > 0){

        controlBoards = new ControlBoard[numberOfControlBoards];

        //pass the config filename instead of the configFile already opened
        //because the UTBoards have to create their own iniFile objects to read
        //the config file because they each have threads and iniFile is not
        //threadsafe

        for (int i = 0; i < numberOfControlBoards; i++) {
            controlBoards[i] = new ControlBoard(configFile, "Control " + (i+1),
                    i, RUNTIME_PACKET_SIZE, simulateControlBoards, log,
                                                                mainFileFormat);
            controlBoards[i].init();
        }

    }//if (numberOfControlBoards > 0)

}//end of Capulin1::configureControlBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::configureUTBoards
//
// Loads configuration settings from the configuration.ini file relating to
// the boards and creates/sets them up.
//

private void configureUTBoards()
{

    //create an array of boards per the config file setting
    if (numberOfUTBoards > 0){

        utBoards = new UTBoard[numberOfUTBoards];

        //pass the config filename instead of the configFile already opened
        //because the UTBoards have to create their own iniFile objects to read
        //the config file because they each have threads and iniFile is not
        //threadsafe

        for (int i = 0; i < numberOfUTBoards; i++) {
            utBoards[i] = new UTBoard(configFile.filename,
                  "UT "+ (i+1), i, simulateUTBoards, log, hdwVs, jobFileFormat,
                                                               mainFileFormat);
            utBoards[i].init();
        }

    }//if (numberOfUTBoards > 0)

}//end of Capulin1::configureUTBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::configureChannels
//
// Loads configuration settings from the configuration.ini file relating to
// the channels and creates/sets them up.
//

private void configureChannels()
{

    //create an array of channels per the config file setting
    if (numberOfChannels > 0){

        channels = new Channel[numberOfChannels];

        for (int i = 0; i < numberOfChannels; i++) {
            channels[i] = new Channel(configFile, settings, i, null);
        }

    }//if (numberOfChannels > 0)

}//end of Capulin1::configureChannels
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::findBoardIndexByChassisAndSlotAddr
//
// Finds the UTBoard object in the array which has been assigned to the board
// in pChassis and PSlot.
//
// Returns the board's index in the array or -1 if no board found.
//

int findBoardIndexByChassisAndSlotAddr(int pChassis, int pSlot)
{

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i] != null && utBoards[i].ready) {
            if (pChassis == utBoards[i].chassisAddr
                                            && pSlot == utBoards[i].slotAddr) {
                return (i);
            }
        }
    }

    return(-1);

}//end of Capulin1::findBoardIndexByChassisAndSlotAddr
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::findBoardIndexByTargetMapChannel
//
// Finds the UTBoard object in the array which has been designated as a
// source for map channel pTargetMapChannel.
//
// Returns the board's index in the array or -1 if no board found.
//

int findBoardIndexByTargetMapChannel(int pTargetMapChannel)
{

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i] != null && utBoards[i].ready) {
            if (pTargetMapChannel == utBoards[i].getTargetMapChannel()) {
                return (i);
            }
        }
    }

    return(-1);

}//end of Capulin1::findBoardIndexByTargetMapChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::findBoardByTargetMapChannel
//
// Finds the UTBoard object in the array which has been designated as a
// source for map channel pTargetMapChannel.
//
// Returns reference to the board or null if no board found.
//

UTBoard findBoardByTargetMapChannel(int pTargetMapChannel)
{

    int target = findBoardIndexByTargetMapChannel(pTargetMapChannel);

    if (target != -1){
        return(utBoards[target]);
    }
    else{
        return(null);
    }

}//end of Capulin1::findBoardByTargetMapChannel
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::countMapSourceBoards
//
// Returns the number of boards configured to be map data sources.
//

int countMapSourceBoards()
{

    int count = 0;

    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i] != null && utBoards[i].ready) {
            if (utBoards[i].getTargetMapChannel() != -1) {
                count++;
            }
        }
    }

    return(count);

}//end of Capulin1::countMapSourceBoards
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getRAM
//
// Fills array with the contents of the RAM on the specified chassis, slot,
// DSP chip, DSP core, shared or local memory, page, and starting address.
//
// pCount bytes are returned.
//

@Override
public void readRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pCount, byte[] dataBlock)
{

    int board = findBoardIndexByChassisAndSlotAddr(pChassis, pSlot);

    if (board != -1) {
        utBoards[board].readRAM(pDSPChip, pDSPCore, pRAMType,
                                           pPage, pAddress, pCount, dataBlock);
    }

}//end of Capulin1::getRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::writeRAM
//
// Writes pValue to the RAM at pAddress on the specified chassis, slot,
// DSP chip, DSP core, shared or local memory, and page.
//

@Override
public void writeRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pValue)
{

    int board = findBoardIndexByChassisAndSlotAddr(pChassis, pSlot);

    if (board != -1) {
        utBoards[board].writeDSPRam(pDSPChip, pDSPCore, pRAMType,
                                                     pPage, pAddress, pValue);
    }

}//end of Capulin1::writeRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::fillRAM
//
// Fills a block of memory with the specified address, size, and value.
//

@Override
public void fillRAM(int pChassis, int pSlot, int pDSPChip, int pDSPCore,
           int pRAMType, int pPage, int pAddress, int pBlockSize, int pValue)
{

    int board = findBoardIndexByChassisAndSlotAddr(pChassis, pSlot);

    if (board != -1) {
        utBoards[board].fillRAM(pDSPChip, pDSPCore,
                                pRAMType, pPage, pAddress, pBlockSize, pValue);
    }

}//end of Capulin1::fillRAM
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getState
//
// Returns the state of various flags or values as selected by pWhich.
// If a flag is being requested, returns 0 for false and not 0 for true.
// If a value is being requested, returns the value.
//
//

@Override
public int getState(int pChassis, int pSlot, int pWhich)
{

    int board = findBoardIndexByChassisAndSlotAddr(pChassis, pSlot);

    if (board != -1) {
        return utBoards[board].getState(pWhich);
    }
    else {
        return(0);
    } //board not found

}//end of Capulin1::getState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::setState
//
// Sets the state of various flags or values as selected by pWhich.
// If a flag is being specified, pValue 0 for false and not 0 for true.
// If a value is being specified, it will be set to pValue.
//

@Override
public void setState(int pChassis, int pSlot, int pWhich, int pValue)
{

    int board = findBoardIndexByChassisAndSlotAddr(pChassis, pSlot);

    if (board != -1) {
        utBoards[board].setState(pWhich, pValue);
    }

}//end of Capulin1::setState
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getSimulate
//
// Returns the simulate flag.  This flag is set if any simulation is being
// performed so that outside classes can adjust accordingly, such as by
// starting a thread to drive the simulation functions.
//

@Override
public boolean getSimulate()
{

    return (simulate);

}//end of Capulin1::getSimulate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getInspectControlVars
//
// Transfers local variables related to inspection control signals and encoder
// counts.
//

@Override
public void getInspectControlVars(InspectControlVars pICVars)
{

    controlBoards[0].getInspectControlVars(pICVars);

}//end of Capulin1::getInspectControlVars
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::waitSleep
//
// Sleeps for pTime milliseconds.
//

void waitSleep(int pTime) throws InterruptedException
{

    Thread.sleep(pTime);

}//end of Capulin1::waitSleep
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::sendRunTriggers
//
// Sends a command to cause the remote to skip past the UDP FPGA loading.
//
//

void sendRunTriggers(DatagramSocket pSocket, DatagramPacket pOutPacket)
{

    //send NO_ACTION to skip past the UDP FPGA loading

    sendByteUDP(pSocket, pOutPacket, UTBoard.NO_ACTION);

}//end of Capulin1::sendRunTriggers
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::loadFPGAViaUDP
//
// Transmits the FPGA code to the specified UT board for loading into the
// chip.  Note that the MainThread object in Main does not start calling
// processDataPackets until after the FPGA code is loaded so there are no
// conflicts created by this function reading from the socket.
//
// This function uses uses UDP to simultaneously broadcast the FPGA code to
// multiple boards at once -- this is a much faster method letting all the
// UTBoard objects load FPGA code to their attached boards simultaneously
// via TCP/IP.  See UTBoard::loadFPGA if the TCP/IP method is needed.
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

public void loadFPGAViaUDP(DatagramSocket pSocket)
{

    // don't attempt to load the FPGA if UDP socket is not open
    if (pSocket == null) {return;}

    fpgaLoaded = false;

    InetAddress group;
    int CODE_BUFFER_SIZE = 1025; //transfer command word and 1024 data bytes
    byte[] codeBuffer;
    codeBuffer = new byte[CODE_BUFFER_SIZE];
    byte[] inBuffer;
    byte[] outBuffer;
    DatagramPacket outPacket;
    DatagramPacket inPacket;
    DatagramPacket codePacket;

    inBuffer = new byte[RUNTIME_PACKET_SIZE];
    outBuffer = new byte[RUNTIME_PACKET_SIZE];

    try{
        group = InetAddress.getByName("230.0.0.1");
    }
    catch (UnknownHostException e) {
        System.err.println(getClass().getName() + " - Error: 1750");
        return;
    }

    inPacket = new DatagramPacket(inBuffer, inBuffer.length);
    outPacket = new DatagramPacket(outBuffer, outBuffer.length, group, 4446);
    codePacket = new DatagramPacket(codeBuffer, codeBuffer.length, group, 4446);

    int bufPtr;

    boolean fileDone = false;

    FileInputStream inFile;

    try {

        //send command to initiate FPGA loading
        sendByteUDP(pSocket, outPacket, UTBoard.LOAD_FPGA_CMD);

        logger.logMessage("Loading all UT board FPGAs..." + "\n");

        inFile = new FileInputStream("fpga\\" + fpgaCodeFilename);
        int c;
        int dataRequested = 0;

        //loop until error occurs -- on successful load, the function will exit
        //note that the looping continues after the fileDone flag is set so that
        //the final success/fail messages from the remote can be caught

        while(dataRequested != -1){

            dataRequested = getFPGALoadResponse(pSocket, inPacket);

            //if all FPGAs loaded successfully, exit the function
            if (dataRequested == 2){
                fpgaLoaded = true;
                return;
            }

            //send data packet when requested by remote
            if ((dataRequested == 1) && !fileDone){

                bufPtr = 0; c = 0;
                // command byte = data packet
                codeBuffer[bufPtr++] = UTBoard.DATA_CMD;

                //be sure to check bufPtr on left side or a byte will get read
                //and ignored every time bufPtr test fails
                while (bufPtr < CODE_BUFFER_SIZE && (c = inFile.read()) != -1 ){

                    //stuff the bytes into the buffer after the command byte
                    codeBuffer[bufPtr++] = (byte)c;

                }//while (bufPtr...

                if (c == -1) {fileDone = true;}//send no more packets after this

                //send packet to remote
                pSocket.send(codePacket);

            }//if ((dataRequested == 1) && !fileDone)

        }//while(dataRequested != -1)

    }//try
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 1816");
    }
    finally {

    }//finally

}//end of Capulin1::loadFPGAViaUDP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::getFPGALoadResponse
//
// Waits for a response from all UTBoards between sending packets of the FPGA
// code.
//
// All boards in the system must send the SEND_DATA_CMD code before this
// function will exit with a request for a new packet.
//
// All boards must send the FPGA_CONFIG_GOOD code before this function will
// exit with a good code.
//
// If any one board sends an error code, this function will return an error.
//
// If all boards respond with a request for the next packet, returns 1.
// If all boards respond with FPGA_CONFIG_GOOD, returns 2.
// If a board responds with an error, returns -1.
// If timeout occurs before all boards have responded, returns -1.
// If a socket error occurs, returns -1.
//

int getFPGALoadResponse(DatagramSocket pSocket, DatagramPacket pInPacket)
{

    byte[] inBuffer = pInPacket.getData();
    String ipAddrS = "unknown";
    boolean timeOut = false;
    int sendDataCmdCount = 0;
    int fpgaConfigGoodCount = 0;

    //clear all the response flags
    for (int i = 0; i < numberOfUTBoards; i++) {
        utBoards[i].udpResponseFlag = false;
    }

    while(!timeOut){

        inBuffer[0] = UTBoard.NO_ACTION; //clear request byte
        inBuffer[1] = UTBoard.NO_STATUS; //clear status byte

        //check for data requests from the remotes if socket is good
        if (pSocket != null){

            //will read two bytes or timeout -- each board will send two bytes
            //to form its data request
            pInPacket.setLength(2);

            try {pSocket.receive(pInPacket);}
            catch(SocketTimeoutException ste){
                timeOut = true;
            }
            catch(IOException e){
                return(-1);
            }
        }//if (pSocket != null)
        else {
            return(-1);
        }

        //if a packet was received, get the packet's sending IP address
        if (pInPacket.getAddress() != null){
            ipAddrS = pInPacket.getAddress().toString();
            //set flag in matching utBoard
            setUDPResponseFlag(ipAddrS);
        }

        //trap error and finished status messages, second byte in buffer

        if (inBuffer[1] == UTBoard.FPGA_INITB_ERROR){
            logger.logMessage(
                      "UT " + ipAddrS + " error loading FPGA - INIT_B" + "\n");
            return(-1);
        }

        if (inBuffer[1] == UTBoard.FPGA_DONE_ERROR){
            logger.logMessage(
                      "UT " + ipAddrS + " error loading FPGA - DONE" + "\n");
            return(-1);
        }

        if (inBuffer[1] == UTBoard.FPGA_CONFIG_CRC_ERROR){
            logger.logMessage(
                        "UT " + ipAddrS + " error loading FPGA - CRC" + "\n");
            return(-1);
        }

        if (inBuffer[1] == UTBoard.FPGA_CONFIG_GOOD){
            logger.logMessage("UT " + ipAddrS + " FPGA Loaded." + "\n");
            //count boards which return good code
            fpgaConfigGoodCount++;
            //exit when all boards return good
            if (fpgaConfigGoodCount == numberOfUTBoards) {return(2);}
        }

        //send data packet when requested by remote
        if (inBuffer[0] == UTBoard.SEND_DATA_CMD){
            //count boards which return data request code
            sendDataCmdCount++;
            //exit when all boards return data request code
            if (sendDataCmdCount == numberOfUTBoards) {return(1);}
        }//if (inBuffer[0] == SEND_DATA)

    }// while(!timeOut)

    //remote has not responded -- timeout if this part reached
    logger.logMessage("Error loading FPGA(s) - contact lost with:" + "\n");

    //log the list of IP addresses which did not response
    displayUnresponsiveIPAddresses();

    return(-1);

}//end of Capulin1::getFPGALoadResponse
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::setUDPResponseFlag
//
// Sets the udpResponseFlag true in the UTBoard which has an ipAddress matching
// pIPAddress.
//
// This is useful for determining which board did not respond.
//

void setUDPResponseFlag(String pIPAddress)
{

    //set the flag in the utBoard with the matching IP address
    for (int i = 0; i < numberOfUTBoards; i++) {
        if (utBoards[i].ipAddrS.equals(pIPAddress)) {
            utBoards[i].udpResponseFlag = true;
        }
    }

}//end of Capulin1::setUDPResponseFlag
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::displayUnresponsiveIPAddresses
//
// Displays a list of utBoards which do not have their udp
//
// This is useful for determining which board did not respond.
//

void displayUnresponsiveIPAddresses()
{

    //set the flag in the utBoard with the matching IP address
    for (int i = 0; i < numberOfUTBoards; i++) {
        if (!utBoards[i].udpResponseFlag) {
            logger.logMessage("UT " + utBoards[i].ipAddrS + "\n");
        }
    }

}//end of Capulin1::displayUnresponsiveIPAddresses
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::sendByteUDP
//
// Sends pByte via the UDP socket pSocket using pOutPacket.
//

void sendByteUDP(DatagramSocket pSocket, DatagramPacket pOutPacket, byte pByte)
{

    pOutPacket.getData()[0] = pByte; //store the byte in the buffer

    pOutPacket.setLength(1); //send one byte

    try {
        pSocket.send(pOutPacket);
    }
    catch(IOException e){
        System.err.println(getClass().getName() + " - Error: 1995");
    }

}//end of Capulin1::sendByteUDP
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::resetUTBoardsForNextRun
//
// Resets all buffer pointers and such in each UTBoard in preparation for the
// next run.
//

void resetUTBoardsForNextRun()
{

    for (int i = 0; i < numberOfUTBoards; i++){
        if (utBoards[i] != null){ utBoards[i].resetForNextRun(); }
    }

}//end of Capulin1::resetUTBoardsForNextRun
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::enableWallMapPackets
//
// Enables or disables the asynchronous sending of wall map data packets by
// UTBoards of type WALL_MAPPING.
//
// Any boards of other type will ignore the call.
//

void enableWallMapPackets()
{

    if (mapSourceBoards == null) { return; }

    //lock in the synced value since it is used multiple times here
    boolean enable = wallMapPacketsEnabled.applyValue();

    for (int i = 0; i < mapSourceBoards.length; i++){
        if (mapSourceBoards[i].utBoard != null){
            mapSourceBoards[i].utBoard.enableWallMapPackets(enable);
        }
    }

}//end of Capulin1::enableWallMapPackets
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::setMapAdvanceModes
//
// Sets the map advance mode for all UTBoards.
//
// Any boards without mapping will set the mode, but it will be ignored.
//

void setMapAdvanceModes(int pMode)
{

    if (mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){
        if (mapSourceBoards[i].utBoard != null){
            mapSourceBoards[i].utBoard.setMapAdvanceMode(pMode);
        }
    }

}//end of Capulin1::setMapAdvanceModes
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::triggerMapAdvance
//
// Advance the maps for boards with map plotters. Parameter pPosition is the
// encoder position. It must be greater than the plotter's delayDistance for
// the plotter to advance.
//
// Any board without mapping or not in a mode allowing external control of
// advance will ignore the request.
//
// Parameter pPosition is the position of the head or inspection piece as
// measured from the point where the photo eye was blocked.
//

@Override
public void triggerMapAdvance(double pPosition)
{

    if (mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){

        UTBoard utBoard = mapSourceBoards[i].utBoard;

        if (utBoard != null){
            if (utBoard.mapSensorDelayDistance > pPosition ) {
                continue;
            }
            else{
                utBoard.triggerMapAdvance();
            }
        }
    }

}//end of Capulin1::triggerMapAdvance
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::recordStartLocation
//
// Records the linear position of the head/test piece when the start
// inspection signal is received for pHead.
//

@Override
public void recordStartLocation(int pHead, double pPosition)
{

    recordStartLocationForMapping(pHead, pPosition);

}//end of Capulin1::recordStartLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::recordStopLocation
//
// Records the linear position of the head/test piece when the stop
// inspection signal is received for pHead.
//

@Override
public void recordStopLocation(int pHead, double pPosition)
{

    recordStopLocationForMapping(pHead, pPosition);

}//end of Capulin1::recordStopLocation
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::recordStartLocationForMapping
//
// Records the linear position of the head/test piece when the start
// inspection signal is received for pHead for all boards recording map data.
//
// The position saved is the distance the leading vertical photo eye beam spot
// has traveled past the starting end of the piece. The head and transducer
// offsets can be subtracted from this value to give the locaton of any given
// transducer.
//

private void recordStartLocationForMapping(int pHead, double pPosition)
{

    if (mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){

        UTBoard utBoard = mapSourceBoards[i].utBoard;

        if (utBoard != null){

            utBoard.recordStartLocationForMapping(pHead, pPosition);

        }
    }

}//end of Capulin1::recordStartLocationForMapping
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::recordStopLocationForMapping
//
// Records the linear position of the head/test piece when the stop
// inspection signal is received for pHead for all boards recording map data.
//
// The position saved is the distance the leading vertical photo eye beam spot
// has traveled past the starting end of the piece when the trailing vertical
// beam spot clears the far end of the piece. The leading vertical beam spot
// will actually be beyond the far end of the piece at that point. The head and
// transducer offsets can be subtracted from this value to give the location of
// any given transducer.
//

private void recordStopLocationForMapping(int pHead, double pPosition)
{

    if (mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){

        UTBoard utBoard = mapSourceBoards[i].utBoard;

        if (utBoard != null){

            utBoard.recordStopLocationForMapping(pHead, pPosition);

        }
    }

}//end of Capulin1::recordStopLocationForMapping
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::saveAllMapDataSetsToFile
//
// Stores the map data stored in Boards set up for mapping to file(s).
//

@Override
public void saveAllMapDataSetsToFile(
        String pFilename, String pJobFileFormat,
        String pInspectionDirectionDescription)
{


    //update values in Settings object for others to access
    settings.nominalWall = hdwVs.nominalWall;
    settings.measuredPieceLength = hdwVs.measuredLength;
    settings.inspectionDirectionDescription = pInspectionDirectionDescription;

    if (wallMapDataSaver != null) {wallMapDataSaver.saveToFile(pFilename);}

}//end of Capulin1::saveAllMapDataSetsToFile
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::calculateMapOffsetDelays
//
// Adds the appropriate photo eye distance to the front of each head to each
// map data sensor's distance from the front edge of its head.
//
// These offsets are used to delay the data storace after the photo eye detects
// the pipe until the sensor(s) associated to that plotter reach the pipe.
//
// NOTE: Map offsets are actually deduced from the source sensor offset.
// Even though the maps have offsets (as all Plotter objects), these are
// ignored and the sensor offsets are used instead.
//

@Override
public void calculateMapOffsetDelays(
        double pPhotoEye1DistanceFrontOfHead1,
        double pPhotoEye1DistanceFrontOfHead2,
        double pPhotoEye2DistanceFrontOfHead1,
        double pPhotoEye2DistanceFrontOfHead2
        )
{

    if(mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){

        UTBoard utBoard = mapSourceBoards[i].utBoard;

        if (utBoard != null && utBoard.headForMapDataSensor == 1){

            utBoard.startFwdDelayDistance = pPhotoEye1DistanceFrontOfHead1
                                  + utBoard.distanceMapSensorToFrontEdgeOfHead;

            utBoard.startRevDelayDistance = pPhotoEye2DistanceFrontOfHead1 -
                                    utBoard.distanceMapSensorToFrontEdgeOfHead;

        }//if (utBoard != null && utBoard.headForMapDataSensor == 1)
        else
        if (utBoard != null && utBoard.headForMapDataSensor == 1){

            utBoard.startFwdDelayDistance = pPhotoEye1DistanceFrontOfHead2
                                + utBoard.distanceMapSensorToFrontEdgeOfHead;

            utBoard.startRevDelayDistance = pPhotoEye2DistanceFrontOfHead2
                                  - utBoard.distanceMapSensorToFrontEdgeOfHead;

        }//if (utBoard != null && utBoard.headForMapDataSensor == 1)
    }//for (int i = 0; i < mapSourceBoards.length; i++)

}//end of Capulin1::calculateMapOffsetDelays();
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::initializeMapOffsetDelays
//
// Sets the map start delays so the maps don't start until their associated
// sensors reach the pipe.
//
// NOTE: Map offsets are actually deduced from the source sensor offset.
// Even though the maps have offsets (as all Plotter objects), these are
// ignored and the sensor offsets are used instead.
//
// The distance is set depending on the direction of inspection.  Some systems
// have different photo eye to sensor distances depending on the direction
// of travel.
//
// The delay is necessary because each sensor may be a different distance from
// the photo-eye which detects the start of the pipe.
//
// Two sets of values are stored:
//
// The distance of each sensor from the front edge of its head.
// The front edge of the head is the edge which reaches the inspection piece
// first when the carriage is moving away from the operator's station
// (the "forward" direction).
//
// The distances of Photo Eye 1 and Photo Eye 2 to the front edge of each
// head.
//
// Photo Eye 1 is the photo eye which reaches the inspection piece first when
// the carriage is moving away from the operator's station (the "forward"
// direction).
//

@Override
public void initializeMapOffsetDelays(int pDirection, int pAwayDirection)
{

    if (mapSourceBoards == null) { return; }

    for (int i = 0; i < mapSourceBoards.length; i++){

        UTBoard utBoard = mapSourceBoards[i].utBoard;

        if (utBoard != null){

            //set all map plotters as the lead plotter -- there should only
            //be one per chart, so it will be the lead by default
            Plotter plotterPtr = utBoard.getMap2D();
            if (plotterPtr != null) { plotterPtr.leadPlotter = true; }

            //if the current direction is the "Away" direction, then set
            //the offsets properly for the carriage moving away from the
            //operator otherwise set them for the carriage moving towards
            //the operator see more notes in this method's header

            if (pDirection == pAwayDirection) {
                utBoard.mapSensorDelayDistance = utBoard.startFwdDelayDistance;
            }
            else {
                utBoard.mapSensorDelayDistance = utBoard.startRevDelayDistance;
            }
        }//if (utBoard != null){
    }//for (int i = 0; i < mapSourceBoards.length; i++)

}//end of Hardware::initializeMapOffsetDelays
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::sendRepRate
//
// Sends the rep rate to all UT boards.
//
// All UT boards (and thus all channels) must have the same rep rate. Initially,
// each UT board loads its rep rate from the configuration file -- they should
// all be set to the same value there.
//
// The value in this class, which can be modified by the user, overrides the
// initial value used by each UT board.
//

void sendRepRate()
{

    //lock in the synced value since it is used multiple times here
    int lRepRate = repRateInHertz.applyValue();

    for (int i = 0; i < numberOfUTBoards; i++){
        utBoards[i].setRepRateInHertz(lRepRate);
//debug mks        utBoards[i].sendRepRate();
    }

}//end of Capulin1::sendRepRate
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::various get/set functions
//

@Override
public boolean getOnPipeFlag(){return controlBoards[0].getOnPipeFlag();}
@Override
public boolean getInspectFlag(){return controlBoards[0].getInspectFlag();}
@Override
public boolean getNewInspectPacketReady()
    {return controlBoards[0].getNewInspectPacketReady();}
@Override
public void setNewInspectPacketReady(boolean pValue)
    {controlBoards[0].setNewInspectPacketReady(pValue);}

//end of Capulin1Board::various get/set functions
//-----------------------------------------------------------------------------

//----------------------------------------------------------------------------
// Capulin1::xmtMessage
//
// This method allows an outside class to send a message and a value to this
// class and receive a status value back.
//
// In this class, this is mainly used to pass messages on to the simulator
// object(s) so that they can be controlled via messages.
//

@Override
public int xmtMessage(int pMessage, int pValue)
{

    //pass the message on to the first Control Board -- in the future could use
    //the pMessage value to specify which board if necessary
    return controlBoards[0].xmtMessage(pMessage, pValue);

}//end of Capulin1::xmtMessage
//----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::logSevere
//
// Logs pMessage with level SEVERE using the Java logger.
//

void logSevere(String pMessage)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage);

}//end of Capulin1::logSevere
//-----------------------------------------------------------------------------

//-----------------------------------------------------------------------------
// Capulin1::logStackTrace
//
// Logs stack trace info for exception pE with pMessage at level SEVERE using
// the Java logger.
//

void logStackTrace(String pMessage, Exception pE)
{

    Logger.getLogger(getClass().getName()).log(Level.SEVERE, pMessage, pE);

}//end of Capulin1::logStackTrace
//-----------------------------------------------------------------------------

}//end of class Capulin1
//-----------------------------------------------------------------------------
//-----------------------------------------------------------------------------
