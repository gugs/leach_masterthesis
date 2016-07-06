/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * Copyright (c) 2010 Oracle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.sunspotworld.demo;


import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotOpenException;
import org.sunspotworld.demo.util.PacketHandler;
import org.sunspotworld.demo.util.PacketReceiver;
import org.sunspotworld.demo.util.PeriodicTask;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.peripheral.UnableToDeepSleepException;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.service.BootloaderListenerService;


import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.demo.util.IPeriodicTask;
import org.sunspotworld.demo.util.PersistenceUnit;

/**
 * Sample application that sends a stream of accelerometer telemetry
 * information back from the SPOT to a host application. 
 * <p>
 * This class establishes a radio connection with the host application.
 * The AccelMonitor class takes care of all accelerometer-related commands,
 * and sends the host a telemetry stream of accelerometer readings.
 * <p>
 * To simplify our work we make use of a number of utility helper classes: 
 * <ul>
 * <li>{@link LocateService} to locate a remote service (on a host)
 * <li>{@link PacketReceiver} to receive commands from the host application and 
 *     dispatch them to whatever classes registered to handle the command
 * <li>{@link PacketTransmitter} handles sending reply packets back to the host
 * <li>{@link PeriodicTask} provides for running a task, such as taking samples, 
       at a regular interval using the timer/counter hardware
 * </ul>
 * The host commands and replies are defined in the {@link PacketTypes} class.
 *
 *<p>
 * The SPOT uses the LEDs to display its status as follows:
 *<p>
 * LED 0:
 *<ul>
 *<li> Red = running, but not connected to host
 *<li> Green = connected to host display server
 *</ul>
 * LED 1:
 *<ul>
 *<li> Yellow = looking for host display server
 *<li> Blue = calibrating accelerometer
 *<li> Red blink = responding to a ping request
 *<li> Green = sending accelerometer values using 2G scale
 *<li> Blue-green = sending accelerometer values using 4G or 6G scale
 *<li> White = sending accelerometer values using 8G scale
 *</ul>
 * <p>
 * Note: pushing switch 1 will close the current connection.
 * <p>
 * 
 * @author Ron Goldman<br>
 * Date: May 8, 2006,
 * revised: August 1, 2007
 * revised: July 25, 2008
 * revised: August 1, 2010
 *
 * @see LocateService
 * @see LocateServiceListener
 * @see PacketHandler
 * @see PacketReceiver
 * @see PacketTransmitter
 * @see PacketTypes
 */
public class TelemetryMain extends MIDlet
        implements  PacketHandler, PacketType , IPeriodicTask
{
    private boolean connected = false;

    private Random r;
    private PacketReceiver rcvrBS;
    private RadiogramConnection hostConn = null;
    private RadiogramConnection txConn = null;
    //private Aggregator aggregator;
                            
    //private PersistenceUnit persistence;
    private PeriodicTask periodicTask;
    private ISleepManager sleepManager = Spot.getInstance().getSleepManager();

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.
            lookup(ITriColorLEDArray.class);

    private ITriColorLED led1 = leds.getLED(0);
    private ITriColorLED led2 = leds.getLED(1);
    private ITriColorLED led4 = leds.getLED(3);
    private ITriColorLED led6 = leds.getLED(5);
    private ITriColorLED led7 = leds.getLED(6);
    private ITriColorLED led8 = leds.getLED(7);

    //LEACH Variables

    private int timeMainLoopApplication = 80;
    private long initialTime = -1;
    private double randomNumber = 0.0;
    private byte roundNumber = 0;
    private double probability = 0;
    private boolean isCH = false;
    private boolean isCT = false;
    private int maxRssiCoordinator = 0;
    private String clusterHeadAddress = "";
    private int clusterHeadLed = 0;
    private byte indexSlotTimmer = 0;
    private byte clusterLength = 0;
    private final int tdmaSlotTime = 20000;
    private Datagram dataPacket;
    private boolean resetStatus = false;
    private boolean flagTDMA = false;
    private boolean dataPacketPhase = true;
    private Radiogram dgController;
    private Radiogram dgController2;
    private boolean persistenceControl = true;

    //packets' counters for non-CH
    private int advCountNode = 0;
    private int joinCountNode = 0;
    private int tdmaCountNode = 0;
    private int dataCountNode = 0;

    //packets' counters for nCH
    private int advCountCH = 0;
    private int joinCountCH = 0;
    private int tdmaCountCH = 0;
    private int dataCountCH = 0;

    //moving coordinator
    private long ourMacAddress;
    private Thread thread = null;
    private int ledColor = 0;
    private long now = 0L;
    private Vector addressNodes;
    private boolean joinStartTimeControl = false;
    private int joinTimeOut = 100;
    
    /////////////////////////////////////////////////////
    //
    // Lifecycle management - called from startApp() method
    //
    /////////////////////////////////////////////////////
    
    /**
     * Initialize any needed variables.
     */
    public void initialize() 
    {
//                if(persistence == null)
//                {
//                    persistence = new PersistenceUnit();
//                    System.out.println("[System Information] Instantiating "
//                            + "Persistence Unit");
//                }
            
        addressNodes = new Vector();
        r = new Random();
                                led1.setRGB(50,0,0);     // Red = not active
                                led1.setOn();
        startListeningBaseStation();
    }

    ////////////////////////////////////////////////////////////////////
    //
    // Service connection management - called from LocateService class
    //
    ////////////////////////////////////////////////////////////////////

    private void startListeningBaseStation()
    {

        try
        {           
            if(rcvrBS == null)
            {

                System.out.println(getTime(System.currentTimeMillis())+
                        ", node: "+IEEEAddress.toDottedHex(Spot.getInstance().
                        getRadioPolicyManager().getIEEEAddress())+
                        "Status: New instance from PacketReceived"
                        + "was created!");
                
                hostConn = (RadiogramConnection) Connector.open("radiogram://:"
                        +BROADCAST_PORT);
                rcvrBS = new PacketReceiver(hostConn);
            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }

        rcvrBS.setServiceName("Base Station Command Server");
        rcvrBS.registerHandler(this, START_APP);
        rcvrBS.registerHandler(this, START_LEACH_ENGINE);
        rcvrBS.registerHandler(this, BLINK_LEDS_REQ);
        rcvrBS.registerHandler(this, PERSISTENCE);
        rcvrBS.registerHandler(this, ERASE_PERSISTENCE);
        rcvrBS.start();
    }


    /**
     * Called to declare that the connection to the host is no longer present.
     */
    public void closeConnection()
    {
        if (!connected)
        {
            return;
        }
//                          led1.setRGB(50,0,0);     // Red = not active
        connected = false;
        rcvrBS.stop();
        Utils.sleep(100);
        try {
            if (hostConn != null) {
                hostConn.close();
                hostConn = null;
            }
        } 
        catch (IOException ex)
        {
            System.out.println("Failed to close connection to host: " + ex);
        }
    }
    

    //////////////////////////////////////////////////////////
    //
    // Command processing - called from PacketReceiver class
    //
    //////////////////////////////////////////////////////////
    
    /**
     * Callback from PacketReceiver when a new command is received from the host.
     * Note that commands associated with the accelerometer are dispatched directly
     * to the AccelMonitor class's handlePacket() method.
     * 
     * @param type the command
     * @param pkt the radiogram with any other required information
     */
    public void handlePacket(byte type, Radiogram pkt)
    {

        try
        {
            switch (type)
            {
                //Base station commands
                case START_APP:
                        led8.setRGB(0, 0, 100);
                        led8.setOn();
                        Utils.sleep(250);
                        led8.setOff();
                    break;

                case RESET_LEACH_ENGINE:
                    
                    if(!resetStatus)
                    {
                        roundNumber = 0;
        //                        blinkLEDs();
                        if(isCH)
                        {
                            rcvrBS = null;
                            startListeningBaseStation();
                            
                        }
                        else
                        {
                            startListeningBaseStation();
                        }
                            
                        isCH = false;
                        isCT = false;
                        flagTDMA = false;
                        clusterHeadAddress = "";
                        clusterHeadLed = 0;
                        indexSlotTimmer = 0;
                        clusterLength = 0;
                    }
                    resetStatus = true;
                    
                    break;

                case ERASE_PERSISTENCE:
                    
//                    if(!persistence.isNull())
//                    {
//                        persistence.deleteRecStoreOfTimeElapsed();
//                        persistence.deleteRecStoreNonCoordinator();
//                        persistence.deleteRecStoreCoordinator();
//                    }
//
//                    persistence = new PersistenceUnit();
                                                
                        blinkLEDs();
                        led1.setRGB(50,0,0);     // Red = not active
                        led1.setOn();

                    break;

                case PERSISTENCE:

                    txConn = (RadiogramConnection) Connector.
                                   open("radiogram://broadcast:"+
                                   BROADCAST_PORT);

                    String teste = "TIME ELAPSED: \n";

                    Datagram dgtx = txConn.
                            newDatagram(txConn.getMaximumLength());

                    

//                                try
//                                {
//                                    persistence.openRecordStoreForTimeElapsed();
//                                    teste += persistence.readRecord();
//                                    persistence.closeRecStore();
//                                    persistence.openRecordStoreForNonCoordinatorMetric();
//                                    teste += "\nPACKETS OF NON-COORDINATOR: \n";
//                                    teste += persistence.readRecord();
//                                    persistence.closeRecStore();
//                                    persistence.openRecordStoreForCoordinatorMetric();
//                                    teste += "\nPACKETS OF COORDINATOR: \n";
//                                    teste += persistence.readRecord();
//                                    persistence.closeRecStore();
//                                }
//                                catch (RecordStoreException ex)
//                                {
//                                    ex.printStackTrace();
//                                }

                    dgtx.reset();
                    dgtx.writeByte(PERSISTENCE);
                    dgtx.writeInt(teste.length());
                    dgtx.writeUTF(teste);
                    txConn.send(dgtx);
                    txConn.close();

                    System.out.println("Tamanho da string: "+teste.length());
                    System.out.println("Conteudo: "+teste);

                    blinkLEDs();
                    
                    led1.setRGB(50,0,0);     // Red = not active
                    led1.setOn();

                    break;

                //LEACH
                case START_LEACH_ENGINE:

                    dgController = pkt;

//                    try
//                    {
//                        if(persistenceControl)
//                        {
//                            persistence.updateTimeElapsed( (byte)1,
//                                    "Starting cycle: "
//                                    +getTime(System.currentTimeMillis()));
//                                persistenceControl = false;
//                        }
//                        else
//                        {
//                            persistence.updateTimeElapsed( (byte)2,
//                                    "Ending cycle: "
//                                    +getTime(System.currentTimeMillis()));
//                        }
//                    }
//                    catch(Exception e)
//                    {
//                        e.printStackTrace();
//                    }

                    System.out.println("[System Information],"+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", LEACH STARTED in, "
                            +getTime(System.currentTimeMillis()));
                    
                    initialTime = System.currentTimeMillis();
                    
                    resetStatus = false;
                    joinStartTimeControl = false;

                    leds.setOff();
                    led4.setRGB(0, 100, 100);
                    led4.setOn();
                    Utils.sleep(250);
                    led4.setOff();
                    led7.setOff();
                    led8.setOff();
                    maxRssiCoordinator = 0;

                    if(roundNumber >= 1/CLUSTER_HEAD_QUANTITY)
                    {
                        roundNumber = 0;
                        isCH = false;
                        isCT = false;
                        addressNodes.removeAllElements();
                    }

                    randomNumber = r.nextDouble();

                    if(isCH)
                    {
                        isCH = false;
                        isCT = true;
                        addressNodes.removeAllElements();
                    }
                    if(isCT)
                    {
                        probability = 0;
                        led8.setColor(LEDColor.RED);
                        led8.setOn();
                    }
                    else
                    {
                        if(roundNumber >= (1/ CLUSTER_HEAD_QUANTITY - 1))
                            probability = 1;
                        else
                        {
                            probability = CLUSTER_HEAD_QUANTITY
                                    /(1 - CLUSTER_HEAD_QUANTITY*
                                    (roundNumber % 
                                    (int)(1/CLUSTER_HEAD_QUANTITY)));
                        }
                    }

                    if(randomNumber<probability)
                    {
                        roundNumber++;
                        led1.setRGB(0, 50, 0);
                        led1.setOn();
                        
                        init();

                        isCH = true;

                        System.out.println("[System Information],"+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", was elected CH in, "
                            +getTime(System.currentTimeMillis()));

                        clusterHeadLed = ledColor;
                        leds.getLED(clusterHeadLed).setOff();
                        leds.getLED(clusterHeadLed).setRGB(255, 255, 255);
                        leds.getLED(clusterHeadLed).setOn();


                        led6.setOff();
                        led6.setRGB(clusterHeadLed,40,100);
                        led6.setOn();

                        if(txConn != null)
                        {
                            txConn.close();
                            txConn = null;
                        }
                        
                        now = 0L;

                        Utils.sleep(50);

                        try
                        {

                           txConn = (RadiogramConnection) Connector.
                                   open("radiogram://broadcast:" +
                                   BROADCAST_PORT);
                           Datagram xdg = txConn.newDatagram(ADV_PACKET_SIZE);


                                               led6.setOff();
                                               led6.setRGB(255,255,100);
                                               led6.setOn();

                           System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", seding ADV_PACKET in, "
                            +getTime(System.currentTimeMillis()));

                        if (led6 != null)
                        {
                            led6.setRGB(ledColor,40,0);
                            led6.setOn();
                        }

                        xdg = mountAdvPacket(ADV_PACKET, xdg,
                                   ourMacAddress, ledColor);

                        led6.setOff();
                        led6.setColor(LEDColor.YELLOW);
                        led6.setOn();

                        Utils.sleep(50);
                        txConn.send(xdg);
                        Utils.sleep(25);
                        txConn.send(xdg);
                        advCountCH++;                           

                        rcvrBS.stop();

                        hostConn.close();

                        hostConn = (RadiogramConnection) Connector.
                                    open("radiogram://:"+CONNECTED_PORT);

                        rcvrBS = null;
                        rcvrBS = new PacketReceiver(hostConn);
                        rcvrBS.registerHandler(this, TDMA_PACKET);
                        rcvrBS.registerHandler(this, DATA_PACKET);
                        rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
                        rcvrBS.registerHandler(this, JOIN_PACKET);
                        rcvrBS.registerHandler(this, ADV_PACKET);
                        rcvrBS.registerHandler(this, START_LEACH_ENGINE);
                        rcvrBS.start();
                           
                        System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", checking is PacketReceiver is alive: "+
                            rcvrBS.isRunning()+" in,"
                            +getTime(System.currentTimeMillis()));

                        }
                        catch (IOException ex)
                        {
                            System.out.println(ex.getMessage());
                        }
                        finally
                        {
                           if(txConn != null)
                           {
                               txConn.close();
                               txConn = null;
                           }
                        }
                    }
                    else
                    {
                        isCH = false;
                        roundNumber++;
                        led1.setRGB(0, 50, 0);
                        led1.setOn();
                        init();
                    }
                    break;

                case ADV_PACKET:

                    dgController2 = pkt;
                    flagTDMA = false;
                    boolean controlADV = false;

                    if(!isCH)
                    {
                        if(maxRssiCoordinator < pkt.getRssi() && last)
                        {
                            maxRssiCoordinator = pkt.getRssi();
                            System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", is CH"+isCH+" receiving ADV_Packet from: "+
                            pkt.getAddress()+" in,"
                            +getTime(System.currentTimeMillis()));

                            cleanCHLeds(); //set off ch's leds indicators

                            pkt.resetRead();
                            pkt.readByte();
                            long address = pkt.readLong();
                            clusterHeadLed = pkt.readInt();
                            leds.getLED(clusterHeadLed).setOff();
                            leds.getLED(clusterHeadLed).setRGB(255, 255, 255);
                            leds.getLED(clusterHeadLed).setOn();
                            clusterHeadAddress = IEEEAddress.
                                    toDottedHex(address);
                        }

                        if(!controlADV)
                        {
                            thread = new Thread(new Runnable()
                            {
                                public void run()
                                {
                                    try
                                    {
                                        waitForAdvPackets();
                                    }
                                    catch (IOException ex)
                                    {
                                        ex.printStackTrace();
                                    }
                                }
                            });
                            thread.setPriority(Thread.MAX_PRIORITY);
                            thread.start();
                            controlADV = true;
                        }
                    }

                    break;

                case JOIN_PACKET:

                   if(!joinStartTimeControl)
                   {
                       joinStartTimeControl = true;

                       thread = new Thread(new Runnable()
                       {
                           public void run()
                           {
                                try
                                {
                                    replyJoins();
                                }
                                catch (IOException ex)
                                {
                                    ex.printStackTrace();
                                }
                           }
                       }
                    );
                        thread.setPriority(Thread.MAX_PRIORITY);
                        thread.start();
                    }

                   led6.setOff();
                   led6.setRGB(0, 255, 0);
                   led6.setOn();

                    if(!addressNodes.contains(pkt.getAddress()))
                    {
                        addressNodes.addElement(pkt.getAddress());
                        System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", received Join Request from node "+
                            pkt.getAddress()+" in,"
                            +getTime(System.currentTimeMillis()));
                    }

                   led6.setOff();
                   led6.setColor(LEDColor.CYAN);
                   led6.setOn();

                   break;

                case TDMA_PACKET:

                    tdmaCountNode++;
                    System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", isCH: "+isCH+" received a TDMA_Packet from "+
                            pkt.getAddress()+" in,"
                            +getTime(System.currentTimeMillis()));

                    pkt.resetRead();
                    pkt.readByte();
                    double fator = 0;

                    txConn = null;

                    indexSlotTimmer = (pkt.readByte());
                    indexSlotTimmer++;
                    clusterLength = pkt.readByte();
                    fator = ((double)indexSlotTimmer/(double)clusterLength);
                    int time = (int)(tdmaSlotTime*fator);


                    System.out.println("[System Information], "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    ", TDMA Summary: \n"
                                    + "-IndexSlotTimmer: "
                                    +indexSlotTimmer+"\n -ClusterLength: "+
                                    clusterLength+"\nisCH: "+isCH+"\nFlagTMDA: "
                                    +flagTDMA+"\nTDMA slot time"+tdmaSlotTime
                                    +"\n"+ "Factor: "+fator+" in,"+
                                    getTime(System.currentTimeMillis()));

                   txConn = (RadiogramConnection) Connector.
                           open("radiogram://"+clusterHeadAddress+":"
                           +CONNECTED_PORT);

                   txConn.setTimeout(-1);

                   dataPacket = txConn.newDatagram(DATA_PACKET_SIZE);

                   now = System.currentTimeMillis();

                   txConn.setRadioPolicy(RadioPolicy.OFF);
                   rcvrBS.stop();
                   thread = null;

                   Utils.sleep(time);

                   txConn.setRadioPolicy(RadioPolicy.ON);
                   dataPacket.reset();
                   dataPacket.writeByte(DATA_PACKET);
                   dataPacket.writeInt(10); //supposed value collected from transductor
                   txConn.send(dataPacket);
                   Utils.sleep(20);
                   if (txConn != null)
                   {
                       txConn.close();
                       txConn = null;
                   }
                   Utils.sleep((tdmaSlotTime - time));
                   rcvrBS.start();
                   dataCountNode++;
                  
                break;

                case DATA_PACKET:

                    //long nowDataPacket = 0L;
                    dataCountCH++;
                    if(dataPacketPhase)
                    {
                        thread = null;
                        //nowDataPacket = System.currentTimeMillis();
                        System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +",isCH: "+isCH+
                            " received first Data_Packet from CH: "+
                            pkt.getAddress()+" in,"+
                            getTime(System.currentTimeMillis()));
                        //aggregator = new Aggregator(nowDataPacket);
                        //aggregator.start();
                        dataPacketPhase = false;
                    }
                    
                    System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +",isCH: "+isCH+
                            " received first Data_Packet from CH: "+
                            pkt.getAddress()+" in,"+
                            getTime(System.currentTimeMillis()));

//                    if((System.currentTimeMillis()-pkt.getTimestamp()) <
//                            TIMEOUT_DATA_PACKET)
//                    {
//                           pkt.resetRead();
//                           pkt.readByte();
//                           int temp = pkt.readInt();
//                          // aggregator.
//                          //        addDataPacketInQueue(Integer.valueOf(temp));
//
//                    }
                    
                break;
                //END LEACH

                case DISPLAY_SERVER_QUITTING:
                    closeConnection();      // we need to reconnect to host
                    break;

                case BLINK_LEDS_REQ:
                                blinkLEDs();
                                if (connected)
                                {
                                    led1.setRGB(0, 30, 0);   // Green = connected
                                }
                                else
                                {
                                    led1.setRGB(50, 0, 0);   // Red = not active
                                }
                                led1.setOn();
                break;
                                   
            }
        } 
        catch (IOException ex)
        {
            closeConnection();
        }
    }


    //Print long time format to display
    public static String getTime(long millis)
    {
        String result = "";
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(millis));
        int year = cal.get(Calendar.YEAR);
        int month = 1 + cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min = cal.get(Calendar.MINUTE);
        int sec = cal.get(Calendar.SECOND);

        result = year + "-" + ((month < 10)? "0": "") + month + "-" +
                ((day < 10) ? "0" : "") + day + " " + hour + ":" +
                ((min < 10) ? "0" : "") + min + ":" +
                ((sec < 10) ? "0" : "") + sec;

        return result;
    }

    
    // handle Blink LEDs command
    private void blinkLEDs()
    {
        for (int i = 0; i < 4; i++)
        {          // blink LEDs 10 times = 10 seconds
                        leds.setColor(LEDColor.MAGENTA);
                        leds.setOn();
                        Utils.sleep(250);
                        leds.setOff();
                        Utils.sleep(250);
        }
    }

    private void waitForAdvPackets() throws IOException
    {
        advCountNode++;
        Utils.sleep(200);
        if(thread == Thread.currentThread())
        {
            txConn = (RadiogramConnection) Connector.
                open("radiogram://"+clusterHeadAddress+":"
                +CONNECTED_PORT);
        txConn.setTimeout(-1);
        Datagram answerCH = txConn.
                newDatagram(JOIN_PACKET_SIZE);
        answerCH.writeByte(JOIN_PACKET);
        //Utils.sleep(150);
        txConn.send(answerCH);
        txConn.close();
        joinCountNode++;
        txConn = null;
        rcvrBS.stop();
        hostConn.close();
        hostConn = (RadiogramConnection) Connector.
                open("radiogram://:"+CONNECTED_PORT);
        rcvrBS = null;
        rcvrBS = new PacketReceiver(hostConn);
        rcvrBS.registerHandler(this, TDMA_PACKET);
        rcvrBS.registerHandler(this, DATA_PACKET);
        rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
        rcvrBS.registerHandler(this, JOIN_PACKET);
        rcvrBS.registerHandler(this, ADV_PACKET);
        rcvrBS.registerHandler(this, START_LEACH_ENGINE);
        rcvrBS.start();

        System.out.println("[System Information], "+
        IEEEAddress.toDottedHex(Spot.getInstance().
        getRadioPolicyManager().getIEEEAddress())
        +", is waiting for TDMA_Packet in,"
        +getTime(System.currentTimeMillis()));
        }

    }

    private void replyJoins() throws IOException
    {
        Utils.sleep(300); // waiting for vector fillment

        System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", isCH: "+isCH+" registered "+addressNodes+
                            " Joins requests in,"
                            +getTime(System.currentTimeMillis()));
        txConn = null;
        Datagram newDg = null;
        int i = 0;

        for(i = 0; i < addressNodes.size(); i++)
        {
            joinCountCH++;
            txConn = (RadiogramConnection) Connector.
                    open("radiogram://"+(String)addressNodes.
                    elementAt(i)+":"+CONNECTED_PORT);
            System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", isCH: "+isCH+" replying with TDMA_Packet node: "
                            +(String)addressNodes.elementAt(i)+":"+
                            CONNECTED_PORT+" in,"+
                            getTime(System.currentTimeMillis()));
            
            newDg = txConn.newDatagram(TDMA_PACKET_SIZE);
            newDg.writeByte(TDMA_PACKET);
            newDg.writeByte(addressNodes.indexOf(addressNodes.elementAt(i)));
            newDg.writeByte(addressNodes.size());
            txConn.send(newDg);

            txConn.close();
            txConn = null;
            tdmaCountCH++;
        }

        System.out.println("[System Information], "+
                            IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +", isCH: "+isCH+
                            " finished TDMA_Packet send process in,"
                            +getTime(System.currentTimeMillis()));
    }


    //////////////////////////////////////////////////////////
    //
    // Standard MIDlet class methods
    //
    //////////////////////////////////////////////////////////

    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException
    {
        BootloaderListenerService.getInstance().start();
        initialize();
        //modified to global variable
        periodicTask = null;
        boolean control = false;
        sleepManager.enableDeepSleep(true);

        while(true)
        {
            Utils.sleep(timeMainLoopApplication);

            if(initialTime != -1)
            {
                if(!control)
                {
                    rcvrBS.stop();
                    rcvrBS.registerHandler(this, START_LEACH_ENGINE);
                    rcvrBS.registerHandler(this, JOIN_PACKET);
                    rcvrBS.registerHandler(this, ADV_PACKET);
                    rcvrBS.registerHandler(this, TDMA_PACKET);
                    rcvrBS.registerHandler(this, DATA_PACKET);
                    rcvrBS.start();

                    timeMainLoopApplication = 30000;
                    periodicTask = new PeriodicTask(this, 0, tdmaSlotTime+6000);
                    periodicTask.start();
                    control = true;
                }
            }
        }
    }

    protected void assignListeners()
    {
        try
        {

            hostConn = (RadiogramConnection) Connector.
                            open("radiogram://:"+BROADCAST_PORT);
            rcvrBS.setRadiogramConnection(hostConn);
            rcvrBS.registerHandler(this, TDMA_PACKET);
            rcvrBS.registerHandler(this, DATA_PACKET);
            rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
            rcvrBS.registerHandler(this, JOIN_PACKET);
            rcvrBS.registerHandler(this, ADV_PACKET);
            rcvrBS.registerHandler(this, START_LEACH_ENGINE);
            rcvrBS.start();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * This will never be called by the Squawk VM.
     */
    protected void pauseApp()
    {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
    protected void destroyApp(boolean unconditional)
            throws MIDletStateChangeException
    {
        
    }


    private Datagram mountAdvPacket (byte cmd, Datagram xdg,
            long address, int color)
    {
        try
        {
            xdg.reset();
            xdg.writeByte(cmd);
            xdg.writeLong(address);
            xdg.writeInt(color);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return xdg;
    }

    /**
     * Method from LEACH scope
     */
    private void init()
    {
        ledColor = (int)r.nextInt(4);
        ledColor++;
        ourMacAddress = Spot.getInstance().getRadioPolicyManager().
                getIEEEAddress();
    }

    public void doTask()
    {
        System.gc();
        System.out.println("Round: "
                +roundNumber+", acomplished!");

        for (int i = 0; i <= (roundNumber-1); i++)
        {
            leds.setColor(LEDColor.MAGENTA);
            leds.setOn();
            Utils.sleep(200);
            leds.setOff();
            Utils.sleep(200);
        }

        thread = null;
        rcvrBS.stop();

        try
        {
            if(hostConn != null)
            {
                hostConn.close();
                hostConn = null;
            }
            if(txConn != null)
            {
                txConn.close();
                txConn = null;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
//        try
//        {
//            // VERIFICAR O PQ DO RESULTADOS DOS CONTADORES
//            persistence.updatePacketsCounterCoordinator
//                    (PersistenceUnit.ADV_PACKET, String.valueOf(advCountCH));
//            persistence.updatePacketsCounterCoordinator
//                    (PersistenceUnit.JOIN_PACKET, String.valueOf(joinCountCH));
//            persistence.updatePacketsCounterCoordinator
//                    (PersistenceUnit.TDMA_PACKET, String.valueOf(tdmaCountCH));
//            persistence.updatePacketsCounterCoordinator
//                    (PersistenceUnit.DATA_PACKET, String.valueOf(dataCountCH));
//            persistence.updatePacketsCounterNonCoordinator
//                    (PersistenceUnit.ADV_PACKET, String.valueOf(advCountNode));
//            persistence.updatePacketsCounterNonCoordinator
//                    (PersistenceUnit.JOIN_PACKET, String.valueOf(joinCountNode));
//            persistence.updatePacketsCounterNonCoordinator
//                    (PersistenceUnit.TDMA_PACKET, String.valueOf(tdmaCountNode));
//            persistence.updatePacketsCounterNonCoordinator
//                    (PersistenceUnit.DATA_PACKET, String.valueOf(dataCountNode));
//
//            System.out.println("Contadores armazenados com sucesso.");
//        }
//        catch (Exception ex)
//        {
//            ex.printStackTrace();
//        }
        assignListeners();
        handlePacket(START_LEACH_ENGINE, dgController);
    }

    private void cleanCHLeds()
    {
        for(int i = 1; i < 5; i++)
        {
            leds.getLED(i).setOff();
        }
    }

}

