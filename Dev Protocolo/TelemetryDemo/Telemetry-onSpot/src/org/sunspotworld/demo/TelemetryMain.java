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

import com.sun.spot.resources.transducers.SwitchEvent;
import org.sunspotworld.demo.util.PacketHandler;
import org.sunspotworld.demo.util.PacketReceiver;
import org.sunspotworld.demo.util.PeriodicTask;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.resources.transducers.ITriColorLEDArray;
import com.sun.spot.resources.transducers.LEDColor;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.resources.transducers.ISwitch;
import com.sun.spot.resources.transducers.ISwitchListener;
import com.sun.spot.service.BootloaderListenerService;
import com.sun.spot.util.DummyApp;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.demo.util.Aggregator;
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
        implements  PacketHandler, PacketType , IPeriodicTask, ISwitchListener
{
    private boolean connected = false;
    private Random r;
    //private CoordinatorService locator;
    private PacketReceiver rcvrBS;
    //private PacketTransmitter xmit;
    private RadiogramConnection hostConn = null;
    private RadiogramConnection txConn = null;
    //private Aggregator aggregator;
    //private PersistenceUnit persistence;

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.
            lookup(ITriColorLEDArray.class);
    private ITriColorLED led1 = leds.getLED(0);
    private ITriColorLED led2 = leds.getLED(1);
    private ITriColorLED led4 = leds.getLED(3);
    private ITriColorLED led6 = leds.getLED(5);
    private ITriColorLED led7 = leds.getLED(6);
    private ITriColorLED led8 = leds.getLED(7);

    //LEACH Variables

    private long initialTime = -1;
    private double randomNumber = 0.0;
    private byte roundNumber = 0;
    private double probability = 0;
    private boolean isCH = false;
    private boolean isCT = false;
    private int maxRssiCoordinator = 0;
    private String clusterHeadAddress = "";
    private int clusterHeadColor = 0;
    private byte indexSlotTimmer = 0;
    private byte clusterLength = 0;
    private final int tdmaSlotTime = 20000;
    private Datagram dataPacket;
    private boolean resetStatus = false;
    private boolean flagTDMA = false;
    private boolean dataPacketPhase = true;
    private Radiogram dgController;


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
        addressNodes = new Vector();
        r = new Random();
        led1.setRGB(50,0,0);     // Red = not active
        led1.setOn();
        startListeningBaseStation();
        // create a service locator using the correct port & connection commands
        //locator = new CoordinatorService();

    }

    ////////////////////////////////////////////////////////////////////
    //
    // Service connection management - called from LocateService class
    //
    ////////////////////////////////////////////////////////////////////

    private void startListeningBaseStation()
    {
        //locator = new CoordinatorService();

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
//            else
//            {
//
//                rcvrBS.stop();
//                System.out.println(getTime(System.currentTimeMillis())+
//                        ", node: "+IEEEAddress.toDottedHex(Spot.getInstance().
//                        getRadioPolicyManager().getIEEEAddress())+
//                        "Status: An instance from PacketReceived is "
//                        + "already created!");
//                hostConn = (RadiogramConnection) Connector.open("radiogram://:"
//                        +BROADCAST_PORT);
//                rcvrBS = null;
//                rcvrBS = new PacketReceiver(hostConn);
//            }
        }
        catch(IOException ex)
        {
            ex.printStackTrace();
        }

        rcvrBS.setServiceName("Base Station Command Server");
        rcvrBS.registerHandler(this, START_APP);
        rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
        rcvrBS.registerHandler(this, START_LEACH_ENGINE);
        rcvrBS.registerHandler(this, STOP_LEACH_ENGINE);
        rcvrBS.registerHandler(this, JOIN_PACKET);
        rcvrBS.registerHandler(this, ADV_PACKET);
        rcvrBS.registerHandler(this, TDMA_PACKET);
        rcvrBS.registerHandler(this, DATA_PACKET);
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
        led1.setRGB(50,0,0);     // Red = not active
        connected = false;
        //accelMonitor.stop();
        rcvrBS.stop();
        Utils.sleep(100);       // give things time to shut down
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
        //locator.stop();
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
                        blinkLEDs();
                        if(isCH)
                        {
                            //locator.forwardResetPacket();
                            //locator.stop();
                            //locator = null;
                            rcvrBS = null;
                            startListeningBaseStation();
                            
                        }
                        else
                        {
                            startListeningBaseStation();
                            //locator.stop();
                            //System.out.println("Deveria resetar a conf 2!");
                        }
                            
                        isCH = false;
                        isCT = false;
                        flagTDMA = false;
                        clusterHeadAddress = "";
                        clusterHeadColor = 0;
                        indexSlotTimmer = 0;
                        clusterLength = 0;
                    }
                    resetStatus = true;
                    
                    break;

                //LEACH
                case START_LEACH_ENGINE:

                    dgController = pkt;

                    System.out.println("Iniciou START em "
                            +getTime(System.currentTimeMillis()));
                    
                    initialTime = System.currentTimeMillis();
                    
                    resetStatus = false;

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

                        System.out.println("Iam coordinator! Process started "
                                + "in: "+getTime(System.currentTimeMillis()));

                        clusterHeadColor = ledColor;
                        leds.getLED(clusterHeadColor).setOff();
                        leds.getLED(clusterHeadColor).setRGB(255, 255, 255);
                        leds.getLED(clusterHeadColor).setOn();

                        // Das modificacoes
                        System.out.println(getTime(System.
                        currentTimeMillis())+", node: "+IEEEAddress.
                        toDottedHex(Spot.getInstance().
                        getRadioPolicyManager().getIEEEAddress())+
                        " Starting steps to be performed by CH.");

                        led6.setOff();
                        led6.setRGB(clusterHeadColor,40,100);
                        led6.setOn();

                        txConn = null;

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


                           System.out.println(getTime(System.
                                   currentTimeMillis())+", node: "+IEEEAddress.
                                   toDottedHex(Spot.getInstance().
                                   getRadioPolicyManager().getIEEEAddress())+
                                   " ADV-Radiogram broadcast send connection"
                                   + "stablished with success!");

                           if (led6 != null)
                           {
                              led6.setRGB(ledColor,40,0);
                              led6.setOn();
                           }

                           xdg = writeCoordinatorAddressInHeader(ADV_PACKET, xdg,
                                   ourMacAddress, ledColor);

                           led6.setOff();
                           led6.setColor(LEDColor.YELLOW);
                           led6.setOn();

                           txConn.send(xdg);

                           System.out.println(getTime(System.currentTimeMillis())+
                                ", node: "+IEEEAddress.toDottedHex(Spot.getInstance().
                                getRadioPolicyManager().getIEEEAddress())+
                                "Is this a CH? "+isCH+", Is PacketReceive service"
                                + " running? "
                                    +rcvrBS.isRunning());

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
                        roundNumber++;
                        led1.setRGB(0, 50, 0);
                        led1.setOn();
                        init();
                    }
                    break;

                case ADV_PACKET:



                    flagTDMA = false;

                    if(!isCH)
                    {
                        if(maxRssiCoordinator < pkt.getRssi())
                        {
                            maxRssiCoordinator = pkt.getRssi();
                            
                            System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "WARNING: Receiving ADV Packet sent from"
                                    + " CH: "+IEEEAddress.toDottedHex(Spot.
                                    getInstance().getRadioPolicyManager().
                                    getIEEEAddress()));

                            for(int i = 1; i < 5; i++)
                            {
                                leds.getLED(i).setOff();
                            }
                            
                            pkt.resetRead();
                            pkt.readByte();
                            long address = pkt.readLong();
                            clusterHeadColor = pkt.readInt();
                            leds.getLED(clusterHeadColor).setOff();
                            leds.getLED(clusterHeadColor).setRGB(255, 255, 255);
                            leds.getLED(clusterHeadColor).setOn();
                            clusterHeadAddress = IEEEAddress.
                                    toDottedHex(address);


                            System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "CH Address elected: radiogram"
                                    + "://"+clusterHeadAddress+":"
                                    +CONNECTED_PORT+", by node: "+IEEEAddress.
                                    toDottedHex(Spot.
                                    getInstance().getRadioPolicyManager().
                                    getIEEEAddress()));
                            

                            txConn = (RadiogramConnection) Connector.
                                    open("radiogram://"+clusterHeadAddress+":"
                                    +CONNECTED_PORT);

                            txConn.setTimeout(-1);
                            
                            Datagram answerCH = txConn.
                                    newDatagram(JOIN_PACKET_SIZE);

                            answerCH.writeByte(JOIN_PACKET);

                            Utils.sleep(300);

                            txConn.send(answerCH);
                            
                            txConn.close();

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

                            System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    " Waiting to receive packets!");
                            
                        }
                    }

                    break;

                case JOIN_PACKET:

                    long nowJoin = System.currentTimeMillis();

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

                            thread.start();
                    }

                    led6.setOff();
                    led6.setRGB(0, 255, 0);
                    led6.setOn();

                    if(!addressNodes.contains(pkt.getAddress()))
                    {
                        addressNodes.addElement(pkt.getAddress());
                        System.out.println(getTime(System.
                                currentTimeMillis())+", node: "+IEEEAddress.
                                toDottedHex(Spot.getInstance().
                                getRadioPolicyManager().getIEEEAddress())+
                                " Join_Packet received "
                                +pkt.getAddress()+" with success!");
                    }
                    
                    System.out.println(getTime(System.
                            currentTimeMillis())+", node: "+IEEEAddress.
                            toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())+
                            " Quantity of non-CH joined to this CH: "
                            +addressNodes.size());

                    led6.setOff();
                    led6.setColor(LEDColor.CYAN);
                    led6.setOn();

                    System.out.println(getTime(System.
                           currentTimeMillis())+", node: "+IEEEAddress.
                           toDottedHex(Spot.getInstance().
                           getRadioPolicyManager().getIEEEAddress())+
                           " Starting TDMA_Packet sending!");
                   break;

                case TDMA_PACKET:

                    System.out.println("TDMA RECEIVED - "+getTime(System.
                            currentTimeMillis())+", node: "+IEEEAddress.
                            toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())+
                            "Non CH Node received from "+
                            pkt.getAddress()+", ");

                    pkt.resetRead();
                    pkt.readByte();
                    double fator = 0;
                    boolean alreadySent = false;

                    txConn = null;

                    indexSlotTimmer = (pkt.readByte());
                    indexSlotTimmer++;
                    clusterLength = pkt.readByte();
                    fator = ((double)indexSlotTimmer/(double)clusterLength);
                    int time = (int)(tdmaSlotTime*fator);

                    // Trying keep time balance due packets coming order
                    // plus time took by reply Method
                    Utils.sleep(150+(indexSlotTimmer*50));


                    System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "Non CH Node received from "+
                                    pkt.getAddress()+", "
                                    + "a TDMA Packet with the follow "
                                    + "information:\n "
                                    + "-IndexSlotTimmer: "
                                    +indexSlotTimmer+"\n -ClusterLength: "+
                                    clusterLength+"\nisCH: "+isCH+"\nFlagTMDA: "
                                    +flagTDMA+"\nTDMA slot time"+tdmaSlotTime
                                    +"\n"+ "Factor: "+fator);

                    if(!isCH && !flagTDMA)
                    {

                           flagTDMA = true;
                           
                           txConn = (RadiogramConnection) Connector.
                                   open("radiogram://"+clusterHeadAddress+":"
                                   +CONNECTED_PORT);
                           txConn.setTimeout(-1);

                           dataPacket = txConn.newDatagram(DATA_PACKET_SIZE);

                           now = System.currentTimeMillis();

                           while((System.currentTimeMillis()-now)<tdmaSlotTime
                                   && !alreadySent)
                           {
                               System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())
                                       +" will take "+
                                       getTime(System.currentTimeMillis())+
                                       "in stand-by mode");
                    
                               synchronized(this)
                               {
                                   Utils.sleep(time);
                                   System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    " took "+time+" ms in stand-by mode before "
                                    + "send data_packet.");
                                   
                                   dataPacket.reset();
                                   dataPacket.writeByte(DATA_PACKET);

                                   //supposed value collected from transductor
                                   dataPacket.writeInt(10);

                                   txConn.send(dataPacket);
                                   alreadySent = true;
                                   Utils.sleep(tdmaSlotTime - time);
                                   
                                   if (txConn != null)
                                   {
                                       txConn.close();
                                       txConn = null;
                                   }
                               
                               };

                           }
                    }

                break;

                case DATA_PACKET:

                    //long nowDataPacket = 0L;

                    if(dataPacketPhase)
                    {
                        thread = null;
                        //nowDataPacket = System.currentTimeMillis();
                        System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "First DATA_PACKET received "
                                    + "from non-CH to start the Aggregator");
                        //aggregator = new Aggregator(nowDataPacket);
                        //aggregator.start();
                        dataPacketPhase = false;
                    }
                    
                    System.out.println(getTime(System.
                            currentTimeMillis())+", node: "+IEEEAddress.
                            toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())+
                            "Is it CH: "+isCH+", and the CH receive"
                            + "Data_Packet from: "
                            +pkt.getAddress());

                    if((System.currentTimeMillis()-pkt.getTimestamp()) <
                            TIMEOUT_DATA_PACKET)
                    {
                           pkt.resetRead();
                           pkt.readByte();
                           int temp = pkt.readInt();
                          // aggregator.
                          //        addDataPacketInQueue(Integer.valueOf(temp));

                           System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "Radiogram added in aggregator!");
                    }
                    

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

    private void replyJoins() throws IOException
    {

        Utils.sleep(150); // waiting for vector fillment

        txConn = null;
        Datagram newDg = null;
        int i = 0;

        for(i = 0; i < addressNodes.size(); i++)
        {
            System.out.println(getTime(System.
            currentTimeMillis())+", node: "+IEEEAddress.
            toDottedHex(Spot.getInstance().
            getRadioPolicyManager().getIEEEAddress())+
            " Destination address TDMA packet: "+
            (String)addressNodes.elementAt(i));

            txConn = (RadiogramConnection) Connector.
                    open("radiogram://"+(String)addressNodes.
                    elementAt(i)+":"+CONNECTED_PORT);
            System.out.println("Iam in the METHOD Replay - radiogram://"+
                    (String)addressNodes.elementAt(i)+
                    ":"+CONNECTED_PORT);
            
            newDg = txConn.newDatagram(TDMA_PACKET_SIZE);
            newDg.writeByte(TDMA_PACKET);
            newDg.writeByte(addressNodes.indexOf(addressNodes.elementAt(i)));
            newDg.writeByte(addressNodes.size());
            txConn.send(newDg);

            System.out.println(getTime(System.
            currentTimeMillis())+", node: "+IEEEAddress.
            toDottedHex(Spot.getInstance().
            getRadioPolicyManager().getIEEEAddress())+
            " TDMA sent with success to"
            + "address node: "+"radiogram://"+
            (String)addressNodes.elementAt(i)+":"+
            CONNECTED_PORT);

            txConn.close();
            txConn = null;
        }
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
        // Listen for downloads/commands over USB connection
        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "sw1");
        sw1.addISwitchListener(this);
        initialize();
        PeriodicTask p = null;
        boolean control = false;

        while(true)
        {
            Utils.sleep(100);

            if(initialTime != -1)
            {
                if(!control)
                    p = new PeriodicTask(this, 0, 25000);

                    if(!control)
                    {
                        p.start();
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

    public void switchPressed(SwitchEvent evt) 
    {

    }

    public void switchReleased(SwitchEvent evt)
    {
        closeConnection();
    }


    private Datagram writeCoordinatorAddressInHeader (byte cmd, Datagram xdg,
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
        addressNodes.removeAllElements();
    }

    public void doTask()
    {
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

        assignListeners();
        handlePacket(START_LEACH_ENGINE, dgController);
    }

}

