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
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Queue;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Random;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import org.sunspotworld.demo.PacketTypes;
import org.sunspotworld.demo.TelemetryMain;

/**
 * LEACH Protocol implementation
 * 
 * Class responsable to perform coordinator tasks. Every exchanded message is 
 * performed between coordinator and non-coordinator nodes. Non-coordinator 
 * nodes is represented by main class (TelemetryMain.java)
 * This class was modified and adapted from demo app, developed by Ron Goldman,
 * aiming to reuse the abstraction.
 *    
 * @author Gustavo
 * 
 * @see TelemetryMain
 */


public class CoordinatorService implements PacketTypes
{
    
    private long ourMacAddress;
    private Random random;
    private ITriColorLED led = null;
    private Thread thread = null;
    private int ledColor = 0;
    private long now = 0L;
    private long timeOut = 500; //miliseconds
    private Vector addressNodes;
    

    public CoordinatorService()
    {
        init();
    }

    public void coordinatorReset()
    {
        ourMacAddress = 0L;
        random = null;
        led = null;
        thread = null;
        addressNodes.removeAllElements();
        now = 0L;
        timeOut = 500;
    }

    private void init()
    {
        //LEACH
        random = new Random();
        ledColor = (int)random.nextInt(4);
        ledColor++;
        ourMacAddress = Spot.getInstance().getRadioPolicyManager().
                getIEEEAddress();
        addressNodes = new Vector();
    }

    /**
     * Return color signed to CH
     *
     * @return value that indicates CH's color
     */
    public int getCHColor()
    {
        return ledColor;
    }

    public ITriColorLED getLed()
    {
        return led;
    }

    /**
     * Return status of thread
     *
     * @return boolean value specifying its status
     */
    public boolean getThreadStatus ()
    {
        if(thread == null)
            return false;
        return true;
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
    public void start()
    {

        
        thread = new Thread("Coordinator") {
            public void run() {
                coordinatorLoop();
            }
        };

        thread.start();

    }
    

    /**
     * Stop searching for a service
     */
    public void stop()
    {
        if (thread != null)
        {
            thread.interrupt();
            thread = null;
        }
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

    public synchronized void forwardResetPacket() throws IOException
    {
        RadiogramConnection txConn = null;
        Datagram newDg = null;
        
            System.out.println("Tamanho do vetor dos nós subordinados ao CH"
                    +": "+addressNodes.size());
            for(int i = 0; i < addressNodes.size(); i++)
            {
                try
                {
                    
                    txConn = (RadiogramConnection) Connector.open("radiogram://"
                            +(String)addressNodes.elementAt(i)+
                            ":"+CONNECTED_PORT);
                    newDg = txConn.newDatagram(txConn.getMaximumLength());
                    newDg.writeByte(RESET_LEACH_ENGINE);
                    txConn.send(newDg);
                    txConn.close();

                    System.out.println("Endereco de destino do Reset_Packet: "
                            + "\nradiogram://"+(String)addressNodes.elementAt(i)
                            +":"+CONNECTED_PORT+"\nReset da aplicação!");
                }
                catch (Exception e)
                {
                    System.out.println("Erro no envio do Reset_Packet enviado"
                            + " por: "+IEEEAddress.toDottedHex(ourMacAddress)
                            + "-:-"+e.getMessage());
                }   
            }
    }

    /**
     * Internal loop to locate a remote display service and report its
     * IEEE address back.
     */
    private void coordinatorLoop ()
    {
            System.out.println("Início dos passos a serem executados pelo CH");

            led.setOff();
            led.setRGB(ledColor,40,100);
            led.setOn();
            Utils.sleep(2000);

            RadiogramConnection txConn = null;
            RadiogramConnection rcvConn = null;

            now = 0L;

            Utils.sleep(200);

                try
                {
                   
                   txConn = (RadiogramConnection) Connector.
                           open("radiogram://broadcast:" + BROADCAST_PORT);
                   Datagram xdg = txConn.newDatagram(20);

                    led.setOff();
                    led.setRGB(255,255,100);
                    led.setOn();


                   System.out.println("Radiogram estabelecido em broadcast"
                           + " com sucesso!");

                   if (led != null)
                   {
                      led.setRGB(ledColor,40,0);
                      led.setOn();
                   }

                   xdg = writeCoordinatorAddressInHeader(ADV_PACKET, xdg,
                           ourMacAddress, ledColor);

                   led.setOff();
                   led.setRGB(0, 100, 100);
                   led.setOn();
                   
                   txConn.send(xdg);
                   Utils.sleep(250);
                   //txConn.close();
                   
                   rcvConn = (RadiogramConnection)Connector.open("radiogram://:"
                           + CONNECTED_PORT);

                   Radiogram rdg = (Radiogram)rcvConn.newDatagram(rcvConn.
                           getMaximumLength());

                   now = System.currentTimeMillis();

                   System.out.println("Radiogram estabelecido em server "
                           + "broadcast com sucesso!");

                   led.setOff();
                   led.setRGB(0, 255, 0);
                   led.setOn();
                   

                   while((System.currentTimeMillis() - now < timeOut) )
                   {     
                        try
                        {
                            rcvConn.setTimeout(500);
                            System.out.println("Recebimento dos JOINS "
                                    + "iniciado!");
                            rcvConn.receive(rdg);
                        }
                        catch(TimeoutException e)
                        {
                            System.out.println(e.getMessage()+"Primeiro");
                            break;
                        }

                        if(rdg.readByte() == JOIN_PACKET && !addressNodes.
                                contains(rdg.getAddress()))
                        {
                            addressNodes.addElement(rdg.getAddress());
                            led.setOff();
                            led.setRGB(255, 0, 0);
                            led.setOn();
                            timeOut += 100;
                            System.out.println("Join_Packet recebido de "
                                    +rdg.getAddress()+" com sucesso!");
                        }
                   }

                    System.out.println("Quantidade de nós subordinados: "
                            +addressNodes.size());

                    led.setOff();
                    led.setRGB(255, 255, 0);
                    led.setOn();

                   System.out.println("Início do envio do TDMA_Packet!");

                   Datagram newDg = null;

                   for(int i = 0; i < addressNodes.size(); i++)
                   {
                       System.out.println("Endereço destinatário do pacote "
                               + "TDMA: "+(String)addressNodes.elementAt(i));

                       if(txConn != null && thread == Thread.currentThread())
                       {
                           txConn = (RadiogramConnection) Connector.
                                   open("radiogram://"+(String)addressNodes.
                                   elementAt(i)+":"+CONNECTED_PORT);
                           newDg = txConn.newDatagram(txConn.
                                   getMaximumLength());
                           newDg.writeByte(TDMA_PACKET);
                           newDg.writeByte(addressNodes.indexOf(addressNodes.
                                   elementAt(i)));
                           newDg.writeByte(addressNodes.size());
                           txConn.send(newDg);
                           txConn.close();
                           System.out.println("TDMA enviado com sucesso para "
                                   + "o endereço: "+"radiogram://"+
                                   (String)addressNodes.elementAt(i)+":"+
                                   CONNECTED_PORT);
                       }
                       
                   }
                   
                   System.out.println("Envio do TDMA finalizado!");
                   rcvConn.close();
                   
                }
                catch (IOException ex)
                {
                    System.out.println(ex.getMessage()+"Excecao 2");
                }
                finally
                {
                    
                }
    }


}
