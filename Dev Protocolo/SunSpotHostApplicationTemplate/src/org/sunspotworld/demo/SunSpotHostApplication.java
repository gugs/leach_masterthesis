/*
 * Copyright (c) 2006-2010 Sun Microsystems, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 * BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.sunspotworld.demo;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.IEEEAddress;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.dgc.DGC;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Sample Sun SPOT host application
 */
public class SunSpotHostApplication extends Thread implements PacketType
{

    private static RadiogramConnection rcvConn = null;
    private static DatagramConnection txConn = null;
    private static Datagram txDg;
    private JFrame mainFrame;
    private JPanel panelContent;
    private JButton button;
    private JButton button2;
    private JButton button3;

    @Override
    public void run()
    {
        init();
        startGUI();
        broadcastCmd(DISPLAY_SERVER_RESTART);
        listenForSpots();
    }

    public void startGUI()
    {
        mainFrame = new JFrame("LEACH Protocol on SunSPOT Platform");
        panelContent = new JPanel();
        button  = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        button.setText("Send");
        button2.setText("Start");
        button3.setText("Ping Request");

        mainFrame.setSize(200, 160);
        button.setSize(35, 35);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(BLINK_LEDS_REQ);
            }
        });
        button2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(START_LEACH_ENGINE);
            }
        });

        button3.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(PING_REQ);
            }
        });

        mainFrame.add(panelContent);
        panelContent.add(button);
        panelContent.add(button2);
        panelContent.add(button3);
        mainFrame.validate();
        mainFrame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        mainFrame.setTitle("LEACH Protocol on SunSPOT Platform");
        mainFrame.setName("leachProtocol");
        mainFrame.setVisible(true);
    }

   
    public void init()
    {
        try
        {
            rcvConn = (RadiogramConnection) Connector.open("radiogram://:" + BROADCAST_PORT);
            txConn = (DatagramConnection) Connector.open("radiogram://broadcast:" + CONNECTION_PORT);
            ((RadiogramConnection) txConn).setMaxBroadcastHops(4);
            txDg = txConn.newDatagram(10);
        } 
        catch (Exception ex)
        {
      
        }       
    }

    private static void broadcastCmd (byte cmd)
    {
            try
            {
                txDg.reset();
                txDg.writeByte(cmd);
                txConn.send(txDg);
            }
            catch (IOException ex)
            {

            }
    }

    private void listenForSpots () {
        while (true) {
            System.out.println("teste eb");
            try {
                Datagram dg = rcvConn.newDatagram(10);
                rcvConn.receive(dg);            // wait until we receive a request
                byte result = dg.readByte();
                System.out.println(result);
                
                switch (result)
                {
                    case LOCATE_DISPLAY_SERVER_REQ:       // type of packet
                    String addr = dg.getAddress();
                    IEEEAddress ieeeAddr = new IEEEAddress(addr);
                    long macAddress = ieeeAddr.asLong();
                    System.out.println("Received request from: " + ieeeAddr.asDottedHex());
                    Datagram rdg = rcvConn.newDatagram(10);
                    rdg.reset();
                    rdg.setAddress(dg);
                    rdg.writeByte(DISPLAY_SERVER_AVAIL_REPLY);        // packet type
                    rdg.writeLong(macAddress);                        // requestor's ID
                    rcvConn.send(rdg);                                // send it
                    System.out.println("Bla");
                    break;
                    
                    case PING_REPLY:
                    System.out.println("Teste");
                    int linkQuality = dg.readInt();          // report how well we can hear server
                    int corr = dg.readInt();
                    int rssi = dg.readInt();
                    int battery = dg.readInt();
                    String addrPing = dg.getAddress();
                    IEEEAddress ieeeAddrPing = new IEEEAddress(addrPing);
                    System.out.println("Ping Request from: "+ieeeAddrPing.asDottedHex()+", Link Quality: "+linkQuality);
                    break;
                }
            } 
            catch (IOException ex)
            {
                System.out.println("Error waiting for remote Spot: " + ex.toString());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Start up the host application.
     *
     * @param args any command line arguments
     */
    public static void main(String[] args) throws Exception {
        SunSpotHostApplication app = new SunSpotHostApplication();
        app.run();
        //System.exit(0);
    }

}
