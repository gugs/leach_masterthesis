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
import javax.microedition.rms.RecordStoreNotOpenException;
import org.sunspotworld.demo.util.CoordinatorService;
import org.sunspotworld.demo.util.LocateServiceListener;
import org.sunspotworld.demo.util.PacketHandler;
import org.sunspotworld.demo.util.PacketReceiver;
import org.sunspotworld.demo.util.PacketTransmitter;
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

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import org.sunspotworld.demo.util.Aggregator;
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
        implements  PacketHandler, PacketType , ISwitchListener
{
    private boolean connected = false;
    private Random r;
    private CoordinatorService locator;
    private PacketReceiver rcvrBS;
    private PacketTransmitter xmit;
    private RadiogramConnection hostConn;
    private Aggregator aggregator;
    private PersistenceUnit persistence;

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
        r = new Random();
        led1.setRGB(50,0,0);     // Red = not active
        led1.setOn();
        persistence = new PersistenceUnit();
        persistence.openRecordStoreForLog();
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
        locator = new CoordinatorService();

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
            else
            {
                
                rcvrBS.stop();
                System.out.println(getTime(System.currentTimeMillis())+
                        ", node: "+IEEEAddress.toDottedHex(Spot.getInstance().
                        getRadioPolicyManager().getIEEEAddress())+
                        "Status: An instance from PacketReceived is "
                        + "already created!");
                hostConn = (RadiogramConnection) Connector.open("radiogram://:"
                        +BROADCAST_PORT);
                rcvrBS.setRadiogramConnection(hostConn);
            }
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
        xmit.stop();
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
        locator.stop();
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
                            locator.forwardResetPacket();
                            locator.stop();
                            locator = null;
                            rcvrBS = null;
                            startListeningBaseStation();
                            
                        }
                        else
                        {
                            startListeningBaseStation();
                            locator.stop();
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
                    led8.setOff();

                    if(roundNumber >= 1/PERCENTAGE_CLUSTER_HEAD)
                    {
                        roundNumber = 0;
                        isCH = false;
                        isCT = false;
                    }

                    randomNumber = r.nextDouble();

                    if(isCH)
                    {
                        locator.stop();
                        isCH = false;
                        isCT = true;
                    }
                    if(isCT)
                    {
                        probability = 0;
                        locator.stop();
                    }
                    else
                    {
                        if(roundNumber >= (1/PERCENTAGE_CLUSTER_HEAD - 1))
                        {
                            probability = 1;
                        }
                        else
                        {
                            probability = PERCENTAGE_CLUSTER_HEAD
                                    /(1-PERCENTAGE_CLUSTER_HEAD*
                                    (roundNumber % 
                                    (int)(1/PERCENTAGE_CLUSTER_HEAD)));
                        }

                    }

                    if(randomNumber<probability)
                    {
                        isCH = true;
                        locator.setStatusLed(led6);
                        locator.start();
                        clusterHeadColor = locator.getCHColor();
                        leds.getLED(clusterHeadColor).setOff();
                        leds.getLED(clusterHeadColor).setRGB(255, 255, 255);
                        leds.getLED(clusterHeadColor).setOn();
                        
                    }

                    System.out.println(getTime(System.currentTimeMillis())+
                        ", node: "+IEEEAddress.toDottedHex(Spot.getInstance().
                        getRadioPolicyManager().getIEEEAddress())+
                        "Is this a CH? "+isCH+", Is PacketReceive service"
                        + " running? "
                            +rcvrBS.isRunning());

                    roundNumber++;
                    led1.setRGB(0, 50, 0);
                    led1.setOn();
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
                            
                            rcvrBS.stop();

                            if(hostConn != null)
                            {
                                hostConn.close();
                                hostConn = null;
                            }

                            hostConn = (RadiogramConnection) Connector.
                                    open("radiogram://"+clusterHeadAddress+":"
                                    +CONNECTED_PORT);
                            hostConn.setTimeout(-1);
                            rcvrBS.setRadiogramConnection(hostConn);
                            rcvrBS.start();

                            Datagram answerCH = hostConn.
                                    newDatagram(JOIN_PACKET_SIZE);
                            xmit = new PacketTransmitter(hostConn);
                            xmit.start();

                            answerCH = (xmit.newDataPacket(JOIN_PACKET,
                                    JOIN_PACKET_SIZE));
                            Utils.sleep(300);
                            xmit.send(answerCH);
                                
                            rcvrBS.stop();

                            if(hostConn != null)
                            {
                                hostConn.close();
                                hostConn = null;
                            }
                            
                            hostConn = (RadiogramConnection) Connector.
                                    open("radiogram://:"+CONNECTED_PORT);
                            rcvrBS.setRadiogramConnection(hostConn);
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
                    else
                    {
//                            rcvrBS.registerHandler(this, TDMA_PACKET);
//                            rcvrBS.registerHandler(this, DATA_PACKET);
//                            rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
                    }
                    break;

                case TDMA_PACKET:

                    pkt.resetRead();
                    pkt.readByte();
                    long now = 0L;
                    double fator = 0;
                    boolean alreadySent = false;

                    indexSlotTimmer = (pkt.readByte());
                    indexSlotTimmer++;
                    clusterLength = pkt.readByte();
                    fator = ((double)indexSlotTimmer/(double)clusterLength);
                    int time = (int)(tdmaSlotTime*fator);


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
                           rcvrBS.stop();
                           xmit.stop();

                           if (hostConn != null)
                           {
                                hostConn.close();
                           }

                           hostConn = (RadiogramConnection) Connector.
                                   open("radiogram://"+clusterHeadAddress+":"
                                   +BROADCAST_PORT);
                           hostConn.setTimeout(-1);
                           rcvrBS.setRadiogramConnection(hostConn);
                           xmit.setRadiogramConnection(hostConn);

                           // ADD HANDLERS AFTER RESET PACKETRECEIVER

                           rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
                           rcvrBS.registerHandler(this, START_LEACH_ENGINE);
                           rcvrBS.registerHandler(this, DATA_PACKET);
                           rcvrBS.registerHandler(this, ADV_PACKET);
                           rcvrBS.registerHandler(this, TDMA_PACKET);
                           rcvrBS.registerHandler(this, JOIN_PACKET);
                           rcvrBS.start();
                           xmit.start();

                           dataPacket = hostConn.
                                   newDatagram(DATA_PACKET_SIZE);
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
                               xmit.send(dataPacket);
                               alreadySent = true;
                           }
                    }

                break;

                case DATA_PACKET:

                    long nowDataPacket = 0L;

                    if(dataPacketPhase)
                    {
                        nowDataPacket = System.currentTimeMillis();
                        System.out.println(getTime(System.
                                    currentTimeMillis())+", node: "+IEEEAddress.
                                    toDottedHex(Spot.getInstance().
                                    getRadioPolicyManager().getIEEEAddress())+
                                    "First DATA_PACKET received "
                                    + "from non-CH to start the Aggregator");
                        aggregator = new Aggregator(nowDataPacket);
                        aggregator.start();
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
                           aggregator.
                                   addDataPacketInQueue(Integer.valueOf(temp));

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
        BootloaderListenerService.getInstance().start();       // Listen for downloads/commands over USB connection
        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "sw1");
        sw1.addISwitchListener(this);
        initialize();

        while(true)
        {
            Utils.sleep(100);

            if ( initialTime!= -1){
                if((System.currentTimeMillis()-initialTime) > 25000 )
                {
                    System.out.println("Iniciou as coisas");
                    blinkLEDs();
                    //startListeningBaseStation();
                    try {
                        rcvrBS.stop();

                        if(hostConn != null)
                        {
                            hostConn.close();
                            hostConn = null;
                        }

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
                    } catch (Exception e) {
                    }
                    

                    handlePacket(START_LEACH_ENGINE, dgController);
                }
            }

            
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

}

