/*
 * Copyright (c) 2007-2010 Sun Microsystems, Inc.
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
 **/

package org.sunspotworld.demo.util;

import com.sun.spot.peripheral.Spot;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import org.sunspotworld.demo.PacketTypes;


/**
 * Helper class to handle locating a remote service. 
 *<p>
 * Broadcasts a service request periodically and listens for a response.
 * When found calls back to report the IEEE address where the service can be found.
 *
 * @author Ron Goldman<br>
 * Date: July 31, 2007
 *
 * @see LocateServiceListener
 */
public class CoordinatorService implements PacketTypes{
    
    //private static final int DEFAULT_HOPS = 2;
    private long ourMacAddress;
    //private LocateServiceListener listener = null;
    private String port;
    private int numHops;
    private Random random;
    private ITriColorLED led = null;
    private Thread thread = null;
    private boolean checking = false;
    private int ledColor = 0;

    private Vector addressNodes;
    
    /**
     * Creates a new instance of LocateService.
     *
     * @param listener class to callback when the service is found
     * @param port the port to broadcast & listen on
     * @param checkInterval how long to wait between checking
     * @param requestCmd the command requesting a connection with the service
     * @param replyCmd the command the service replys with to indicate it is available
     */
//    public CoordinatorService(LocateServiceListener listener, String port, long checkInterval,
//                         byte requestCmd, byte replyCmd) {
//        init(listener, port, checkInterval, requestCmd, replyCmd, DEFAULT_HOPS);
//    }
    
    /**
     * Creates a new instance of LocateService.
     *
     * @param listener class to callback when the service is found
     * @param port the port to broadcast & listen on
     * @param checkInterval how long to wait between checking
     * @param requestCmd the command requesting a connection with the service
     * @param replyCmd the command the service replys with to indicate it is available
     * @param numHops the number of hops the broadcast command should traverse
     */
//    public CoordinatorService(LocateServiceListener listener, String port, long checkInterval,
//                         byte requestCmd, byte replyCmd, int numHops) {
//        init(listener, port, checkInterval, requestCmd, replyCmd, numHops);
//    }

    public CoordinatorService()
    {
    
    }

    private void init()
    {
        //LEACH
        ledColor = (int)(random.nextInt()*255);
        ourMacAddress = Spot.getInstance().getRadioPolicyManager().getIEEEAddress();
        addressNodes = new Vector();

    }
 
    /**
     * Specify an LED to use to display status of search.
     *
     * @param led the LED to use to show the search status
     */
    public void setStatusLed(ITriColorLED led) {
        this.led = led;
    }

    /**
     * Start searching for a service
     */
    public void start() {
        checking = true;
        thread = new Thread() {
            public void run() {
                coordinatorLoop();
            }
        };
        thread.start();
    }
    

    /**
     * Stop searching for a service
     */
    public void stop() {
        checking = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }
    
    


    private Datagram writeCoordinatorAddressInHeader (byte cmd, Datagram xdg, long address, int color)
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
    
    private Datagram writeTDMASchedule (byte cmd, Datagram xdg, long address, double time)
    {
        try
        {
            xdg.reset();
            xdg.writeByte(cmd);
            xdg.writeLong(address);
            xdg.writeDouble(time);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return xdg;
    }



    /**
     * Internal loop to locate a remote display service and report its IEEE address back.
     */
    private void coordinatorLoop ()
    {
        
            RadiogramConnection txConn = null;
            RadiogramConnection rcvConn = null;
            Utils.sleep(200);  // wait a bit to give any previously running instance a chance to exit
            // this outer loop is for retrying if there is an exception
            int tries = 2;

                try
                {
                   txConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + port);
                   txConn.setMaxBroadcastHops(numHops);
                   Datagram xdg = txConn.newDatagram(20);

                   if (led != null)
                   {
                      led.setRGB(ledColor,40,0);                // Yellow = looking for display server
                      led.setOn();
                   }

                   xdg = writeCoordinatorAddressInHeader(ADV_PACKET, xdg, ourMacAddress, ledColor);

                   while(tries < 2)
                   {
                       txConn.send(xdg);
                       Utils.sleep(100);
                       tries++;
                   }

                   rcvConn = (RadiogramConnection)Connector.open("radiogram://:" + port);
                   rcvConn.setTimeout(300);    // timeout in 300 msec - so receive() will not deep sleep
                   rcvConn.setRadioPolicy(RadioPolicy.AUTOMATIC);  // but allow deep sleep other times
                   Radiogram rdg = (Radiogram)rcvConn.newDatagram(20);

                   while(checking == true && thread == Thread.currentThread())
                   {
                        rcvConn.receive(rdg);

                        if(rdg.readByte() == JOIN_PACKET)
                        {
                            addressNodes.addElement(rdg.getAddress());
                        }

                        //qual a melhor condicao de sair do laco? timeout
                   }

                   long endereco = 0;

                   for(int i = 0; i < addressNodes.size(); i++)
                   {
                       xdg.reset();
                       endereco = ((Long)addressNodes.elementAt(i)).longValue();
                       txConn.send(writeTDMASchedule(TDMA_PACKET, xdg, endereco, (double)0.10));
                   }

                   //problema de sincronismo de tempo
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                }
                        

    }

}
