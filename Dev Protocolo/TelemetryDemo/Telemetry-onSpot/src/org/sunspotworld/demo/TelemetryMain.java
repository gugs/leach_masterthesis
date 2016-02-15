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
import java.util.Random;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

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
        implements  PacketHandler, PacketTypes , ISwitchListener
{
    
    private static final long SERVICE_CHECK_INTERVAL = 10000;   // = 10 seconds
    private static final String VERSION = "2.0";
    
    private boolean connected = false;
    private String messages[] = new String[50];
    private int stringIndex = 0;
    
    private CoordinatorService locator;
    private PacketReceiver rcvr;
    private PacketReceiver rcvrBS;
    private PacketTransmitter xmit;
    private RadiogramConnection hostConn;
    private int maxRssiCoordinator = 0;
    private String clusterHeadAddress = "";
    private int clusterHeadColor = 0;
    private int rssi = 0;
    //private AccelMonitor accelMonitor = null;

    private ITriColorLEDArray leds = (ITriColorLEDArray) Resources.lookup(ITriColorLEDArray.class);
    private ITriColorLED led1 = leds.getLED(0);
    private ITriColorLED led2 = leds.getLED(1);
    private ITriColorLED led4 = leds.getLED(3);
    private ITriColorLED led6 = leds.getLED(5);
    private ITriColorLED led7 = leds.getLED(6);
    private ITriColorLED led8 = leds.getLED(7);

    //leach variables

    private double randomNumber = 0.0;
    private byte roundNumber = 0;
    private double percentage = 0.1;
    private double probability = 0;
    private boolean isCH = false;
    private boolean isCT = false;
    private Random r;
    
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

        startListeningBaseStation();

        // create a service locator using the correct port & connection commands
        locator = new CoordinatorService();

        //locator.stop();

        //locator.setStatusLed(led2);

        stringIndex = 0;
    }
    
    /**
     * Main application run loop.
     */
    public void run()
    {
        System.out.println("Spot acceleration telemetry recorder  (version " + VERSION + ")");
        led1.setRGB(50,0,0);     // Red = not active
        led1.setOn();

        locator.start();            // look for host app and call serviceLocated() when found
        while (true)
        {
            Utils.sleep(30000);     // real work is done elsewhere; allow deep sleep while trying to connect
        }
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
            hostConn = (RadiogramConnection) Connector.open("radiogram://:"+BROADCAST_PORT);
            rcvrBS = new PacketReceiver(hostConn);        // set up thread to receive & dispatch commands
            rcvrBS.setServiceName("Base Station Command Server");
            rcvrBS.registerHandler(this, START_APP);     // specify commands this class handles
            rcvrBS.registerHandler(this, RESET_LEACH_ENGINE);
            rcvrBS.registerHandler(this, START_LEACH_ENGINE);
            rcvrBS.registerHandler(this, STOP_LEACH_ENGINE);
            rcvrBS.registerHandler(this, JOIN_PACKET);
            rcvrBS.registerHandler(this, ADV_PACKET);
            rcvrBS.registerHandler(this, TDMA_PACKET);
            rcvrBS.registerHandler(this, DATA_PACKET);
            rcvrBS.start();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
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
        rcvr.stop();
        Utils.sleep(100);       // give things time to shut down
        try {
            if (hostConn != null) {
                hostConn.close();
                hostConn = null;
            }
        } catch (IOException ex) {
            System.out.println("Failed to close connection to host: " + ex);
        }
        locator.start();
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
                    isCH = false;
                    isCT = false;
                    roundNumber = 0;
                    blinkLEDs();
                    break;

                //LEACH
                case START_LEACH_ENGINE:

                    leds.setOff();

                    led4.setRGB(0, 100, 100);
                    led4.setOn();
                    Utils.sleep(250);
                    led4.setOff();

                    led7.setOff();
                    led8.setOff();

                    maxRssiCoordinator = 0;
                    led8.setOff();

                    if(roundNumber >= 1/percentage)
                    {
                        roundNumber = 0;
                        isCH = false;
                        isCT = false;
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
                    }
                    else
                    {
                        if(roundNumber >= (1/percentage - 1))
                        {
                            probability = 1;
                        }
                        else
                        {
                            probability = percentage/(1-percentage*
                                    (roundNumber % (int)(1/percentage)));
                        }

                    }
                    if(randomNumber<probability)
                    {
                        //ajustar timer
                        //enviar mensagem de ADV
                        isCH = true;
                        locator.setStatusLed(led6);
                        locator.start();
                        clusterHeadColor = locator.getCHColor();

                        leds.getLED(clusterHeadColor).setOff();
                        leds.getLED(clusterHeadColor).setRGB(255, 255, 255);
                        leds.getLED(clusterHeadColor).setOn();

                        //blinkLEDs();
                    }
                    else
                    {
                        locator.stop();
                    }
                    
                    //setTimer(START_ROUND, roundLenght);
                    roundNumber++;
                    led1.setRGB(0, 50, 0);
                    led1.setOn();
                    break;

                case ADV_PACKET:

                    if(!isCH)
                    {
                        if(maxRssiCoordinator < pkt.getRssi())
                        {
                            System.out.println("Aqui vc tem de entrar de todo jeito!");
                            for(int i = 1; i < 5; i++)
                            {
                                leds.getLED(i).setOff();
                            }
                            
                            pkt.resetRead();
                            byte header = pkt.readByte();
                            long address = pkt.readLong();
                            clusterHeadColor = pkt.readInt();
                            leds.getLED(clusterHeadColor).setOff();
                            leds.getLED(clusterHeadColor).setRGB(255, 255, 255);
                            leds.getLED(clusterHeadColor).setOn();
                            System.out.println("Endereco: radiogram://"+IEEEAddress.toDottedHex(address)+":"+CONNECTED_PORT);
                            
                            rcvrBS.stop();

                            if(hostConn != null)
                            {
                                hostConn.close();
                            }

                            hostConn = (RadiogramConnection) Connector.open("radiogram://"+IEEEAddress.toDottedHex(address)+":"+CONNECTED_PORT);
                            hostConn.setTimeout(-1);
                            
                            rcvrBS.start();

                            Datagram answerCH = hostConn.newDatagram(hostConn.getMaximumLength());
                            xmit = new PacketTransmitter(hostConn);
                            xmit.start();

                            // verificar pq o Packet Receiver está dando Null Point Ex...
                            // Aparentemente pode ser devido a mudanca do hostConn...

                            answerCH = (xmit.newDataPacket(JOIN_PACKET));     
                            Utils.sleep(300);
                            xmit.send(answerCH);
                            
                            
                            
                            rcvrBS.stop();

                            if(hostConn != null)
                            {
                                hostConn.close();
                            }
                            
                            hostConn = (RadiogramConnection) Connector.open("radiogram://:"+CONNECTED_PORT);
                            rcvrBS = new PacketReceiver(hostConn);
                            rcvrBS.registerHandler(this, TDMA_PACKET);
                            rcvrBS.start();
                            System.out.println("Aguardadndo recebimento de pacotes");
                            
                        }
                    }
                    break;

                case TDMA_PACKET:

                    blinkLEDs();

                    break;
                //END LEACH

                case DISPLAY_SERVER_QUITTING:
                    closeConnection();              // we need to reconnect to host
                    break;

                case PING_REQ:
                    led2.setRGB(40, 0, 0);       // Red = ping
                    led2.setOn();
                    sendPingReply(pkt);
                    led2.setOff();
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
                                   
                case REPLAY_LEACH_ENGINE:
                    String addressCandidate = pkt.readUTF();
                    float probabilityValue = pkt.readFloat();

                    if(probabilityValue < percentage)
                        blinkLEDs();
                    break;
            }
        } 
        catch (IOException ex)
        {
            closeConnection();
        }
    }
    
    // handle Ping command
    private void sendPingReply(Radiogram pkt) throws IOException {
        int linkQuality = pkt.getLinkQuality();
        int corr = pkt.getCorr();
        int rssi = pkt.getRssi();
        int battery = Spot.getInstance().getPowerController().getVbatt();
        Radiogram rdg = xmit.newDataPacket(PING_REPLY);
        rdg.writeInt(linkQuality);          // report how well we can hear server
        rdg.writeInt(corr);
        rdg.writeInt(rssi);
        rdg.writeInt(battery);
        xmit.send(rdg);
        for (int im = 0; im < stringIndex; im++) {
            Utils.sleep(30);                // give host time to process packet
            rdg = xmit.newDataPacket(MESSAGE_REPLY);
            rdg.writeUTF(messages[im]);
            xmit.send(rdg);
            // System.out.println("Sent message: " + messages[im]);
        }
        stringIndex = 0;
        Utils.sleep(200);
    }

    //Minha implementação
    // handle probability results
    private void sendProbabilityResult(Radiogram pkt) throws IOException
    {
        String myAddress = IEEEAddress.toDottedHex(pkt.getAddressAsLong());
        Radiogram rdg = xmit.newDataPacket(REPLAY_LEACH_ENGINE);
        rdg.writeUTF(myAddress);          // report how well we can hear server
        rdg.writeDouble(percentage);
        xmit.send(rdg);
        Utils.sleep(200);
    }

    
    // handle Blink LEDs command
    private void blinkLEDs() {
        for (int i = 0; i < 10; i++) {          // blink LEDs 10 times = 10 seconds
            leds.setColor(LEDColor.MAGENTA);
            leds.setOn();
            Utils.sleep(250);
            leds.setOff();
            Utils.sleep(750);
        }
    }

    /**
     * Add a message to the queue to be sent to the host at a later time. 
     * Messages will be sent after the next Ping request arrives.
     *
     * @param msg the String to be sent
     */
    public void queueMessage (String msg) {
        if (stringIndex < messages.length) {
            messages[stringIndex++] = (msg.length() < Radiogram.MAX_LENGTH) ? msg : msg.substring(0, Radiogram.MAX_LENGTH - 1);
            // System.out.println("Queuing message: " + msg);
        } else {
            // System.out.println("No room in queue for message: " + msg);
        }
    }

    private float calculateProbability()
    {
        led4.setRGB(10, 10, 10);
        led4.setOn();
        Utils.sleep(2000);
        led4.setOff();
        return (PERCENTAGE_CLUSTER_HEAD /1-PERCENTAGE_CLUSTER_HEAD*
                (roundNumber%(1/PERCENTAGE_CLUSTER_HEAD)));
    }


    //////////////////////////////////////////////////////////
    //
    // Standard MIDlet class methods
    //
    //////////////////////////////////////////////////////////

    /**
     * MIDlet call to start our application.
     */
    protected void startApp() throws MIDletStateChangeException {
        BootloaderListenerService.getInstance().start();       // Listen for downloads/commands over USB connection
        ISwitch sw1 = (ISwitch) Resources.lookup(ISwitch.class, "sw1");
        sw1.addISwitchListener(this);
        initialize();
        while(true)
        {
            Utils.sleep(30000);
        }
        //run();
    }

    /**
     * This will never be called by the Squawk VM.
     */
    protected void pauseApp() {
        // This will never be called by the Squawk VM
    }

    /**
     * Called if the MIDlet is terminated by the system.
     * @param unconditional If true the MIDlet must cleanup and release all resources.
     */
    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
    }

    public void switchPressed(SwitchEvent evt) {
    }

    public void switchReleased(SwitchEvent evt) {
        closeConnection();
    }

}

