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

import com.sun.spot.peripheral.Spot;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.ISleepManager;
import com.sun.spot.service.BootloaderListenerService;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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
public class ExtendedSensorSampler extends MIDlet
        implements  PacketHandler, PacketType
{

    private PacketReceiver rcvrBS;
    private RadiogramConnection hostConn = null;
    private RadiogramConnection txConn = null;                        
    private PersistenceUnit persistence;
    private ISleepManager sleepManager = Spot.getInstance().getSleepManager();
    private int timeMainLoopApplication = 70;
    private long initialTime = -1;
    boolean condition = true;
    boolean first = true;
    

    //counters
    private int overAllCount = 0;

    
    public void initialize() 
    {
             if(persistence == null)
             {
                 persistence = new PersistenceUnit();
                 System.out.println("[System Information] Instantiating "
                            + "Persistence Unit");
             }
        startListeningBaseStation();
    }

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
        rcvrBS.registerHandler(this, START_ENGINE);
        rcvrBS.registerHandler(this, BLINK_LEDS_REQ);
        rcvrBS.registerHandler(this, PERSISTENCE);
        rcvrBS.registerHandler(this, ERASE_PERSISTENCE);
        rcvrBS.start();
    }


    /**
     * Called to any open connections to the host.
     */
    public void closeConnection()
    {
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
    
    
    /**
     * Callback from PacketReceiver when a new command is received
     * from the host.
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
//                        led8.setRGB(0, 0, 100);
//                        led8.setOn();
                        Utils.sleep(200);
//                        led8.setOff();
                    break;


                case ERASE_PERSISTENCE:
                    
                    if(!persistence.isNull())
                    {
                        persistence.deleteRecStoreOfTimeElapsed();
                        persistence.deleteRecStoreNonCoordinator();
                    }
                    persistence = new PersistenceUnit();
                    System.out.println("All data has been deleted!");

                    break;

                case PERSISTENCE:

                    String teste = "TIME ELAPSED: \n";

                    txConn = (RadiogramConnection) Connector.
                            open("radiogram://broadcast:"+
                            BROADCAST_PORT);
                    Datagram dgtx = txConn.
                            newDatagram(txConn.getMaximumLength());
                   
                    try
                    {
                        persistence.openRecordStoreForTimeElapsed();
                        teste += persistence.readRecord();
                        persistence.closeRecStore();
                        persistence.openRecordStoreForNonCoordinatorMetric();
                        teste += "\nPACKETS OF NON-COORDINATOR: \n";
                        teste += persistence.readRecord();
                        persistence.closeRecStore();
                        teste += "\nPACKETS OF COORDINATOR: \n";
                        teste += persistence.readRecord();
                        persistence.closeRecStore();
                    }
                    catch (RecordStoreException ex)
                    {
                        ex.printStackTrace();
                    }

                    dgtx.reset();
                    dgtx.writeByte(PERSISTENCE);
                    dgtx.writeInt(teste.length());
                    dgtx.writeUTF(teste);
                    txConn.send(dgtx);
                    txConn.close();

                    System.out.println("Tamanho da string: "+teste.length());
                    System.out.println("Conteudo: "+teste);

                    break;

                
                case START_ENGINE:

                    initialTime = System.currentTimeMillis();

                    txConn = (RadiogramConnection)Connector.
                            open("radiogram://0014.4F01.0000.7A7C:"
                            +BROADCAST_PORT);

                    txConn.setMaxBroadcastHops(0);

                    Datagram rdg = txConn.newDatagram(100);

                    String address = IEEEAddress.toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress());

                    //rcvrBS.stop();
                    try
                    {
                        persistence.updateTimeElapsed((byte) 1,
                                getTime(System.currentTimeMillis()));
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }

                    while(condition)
                    {
                        rdg.reset();
                        rdg.writeByte(DATA);
                        rdg.writeInt(overAllCount+1);
                        rdg.writeUTF(address);
                        Utils.sleep(100);
                        txConn.send(rdg);
                        overAllCount++;
                        System.out.print("Packet has been sent successfully "
                                + "by node: "+address+";\n");

                        if(overAllCount%10 == 0)
                        {
                            try
                            {
                                    persistence.updateTimeElapsed((byte) 2,
                                            getTime(System.currentTimeMillis()));
                                    persistence.updatePacketsCounterNonCoordinator(
                                            PersistenceUnit.OVERALL,
                                            String.valueOf(overAllCount));
                            }
                            catch (Exception ex)
                            {
                                    ex.printStackTrace();
                            }
                        }

                        Utils.sleep(SEND_RATE);
                    }

                break;
                

                case RESET_ENGINE:

                    System.out.println("Sensor "+IEEEAddress.
                            toDottedHex(Spot.getInstance().
                            getRadioPolicyManager().getIEEEAddress())
                            +" has been stopped the process!");
                    condition = false;
                    first =true;
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
                ((day < 10) ? "0" : "") + day + "; " + hour + ":" +
                ((min < 10) ? "0" : "") + min + ":" +
                ((sec < 10) ? "0" : "") + sec;

        return result;
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
        boolean control = false;
        sleepManager.enableDeepSleep(true);
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        while(true)
        {
            Utils.sleep(timeMainLoopApplication);

            if(initialTime != -1)
            {
                if(!control)
                {
                    rcvrBS.stop();
                    timeMainLoopApplication = Integer.MAX_VALUE;
                    control = true;
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

}

