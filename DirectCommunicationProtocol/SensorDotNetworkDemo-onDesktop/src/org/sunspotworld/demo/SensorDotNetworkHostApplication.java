/*
 * SensorDotNetworkHostApplication.java
 *
 * Created on Sep 25, 2009 5:41:04 PM;
 */

package org.sunspotworld.demo;
import com.sun.spot.io.j2me.radiogram.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import javax.microedition.io.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;


/**
 * Sample Sun SPOT host application
 */
public class SensorDotNetworkHostApplication extends Thread
        implements PacketType

{

    private static RadiogramConnection rcvConn = null;
    private static DatagramConnection txConn = null;
    private static Datagram txDg;
    private static Datagram rxDg;
    private JFrame mainFrame;
    private JPanel panelContent;
    private JButton button;
    private JButton button2;
    private JButton button3;
    private JButton button4;

    @Override
    public void run()
    {
        init();
        startGUI();
        listenForSpots();
    }

    public void startGUI()
    {
        mainFrame = new JFrame("Example Application of Broadcast "
                + "Message Receiver");
        panelContent = new JPanel();
        button  = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        button4 = new JButton();
        button.setText("Reset");
        button2.setText("Start");
        button3.setText("Download Log");
        button4.setText("Apagar Log");

        mainFrame.setSize(200, 160);
        button.setSize(35, 35);
        button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(RESET_ENGINE);
            }
        });

        button2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(START_ENGINE);
            }
        });


        button3.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(PERSISTENCE);
            }
        });

        button4.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                broadcastCmd(ERASE_PERSISTENCE);
            }
        });

        mainFrame.add(panelContent);

        panelContent.add(button);
        panelContent.add(button2);
        panelContent.add(button3);
        panelContent.add(button4);

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
            rcvConn = (RadiogramConnection) Connector.open("radiogram://:"
                    +BROADCAST_PORT);
            txConn = (DatagramConnection) Connector.
                    open("radiogram://broadcast:" + BROADCAST_PORT);
            //((RadiogramConnection) txConn).setMaxBroadcastHops(4);
            txDg = txConn.newDatagram(10);
            rxDg = rcvConn.newDatagram(rcvConn.getMaximumLength());
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

    private void listenForSpots ()
    {
        while (true)
        {
            try
            {
                rcvConn.receive(rxDg);

                byte result = rxDg.readByte();

                switch (result)
                {
                    case PERSISTENCE:

                        int antes;
                        int data = 0;
                        String temp = "";
                        antes = rxDg.readInt();
                        data = rxDg.readInt();
                        temp = rxDg.readUTF();

                        System.out.println("Endereco Sensor: "+rxDg.getAddress());
                        System.out.println("quantidade registros: "+antes+"\n"
                                + "tamanho string: "+data+"\n"
                                + "conteudo: "+temp);

                    break;

                    case DATA:

                        int idPkt = rxDg.readInt();
                        String address = rxDg.readUTF();
                        System.out.println(getTime(System.currentTimeMillis())+"; Packet ID: "+idPkt+
                                "; Sensors Address: "
                                +address+";");

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
    public static void main(String[] args) throws Exception
    {
        SensorDotNetworkHostApplication app =
                new SensorDotNetworkHostApplication();
        app.run();
        //System.exit(0);
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
}

