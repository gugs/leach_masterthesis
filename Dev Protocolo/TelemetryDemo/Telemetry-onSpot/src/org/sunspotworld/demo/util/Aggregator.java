/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld.demo.util;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.service.IService;
import java.io.IOException;
import java.util.Vector;
import javax.microedition.io.Connector;
import org.sunspotworld.demo.PacketTypes;

/**
 *
 * @author USUARIO
 */
public class Aggregator implements IService, PacketTypes
{
    private long now = 0L;
    private double mean;
    private Radiogram dg;
    private int statusHere = STOPPED;
    private Thread thread = null;
    private String name = "Data Dispatcher";
    private RadiogramConnection txConn = null;
    private Vector dataValues;

    public Aggregator(long nowTime)
    {
        this.now = nowTime;
        dataValues = new Vector();
    }

    public synchronized void addDataPacketInQueue(Integer value)
    {
        dataValues.addElement(value);
    }

    public double getMean()
    {
        return mean;
    }


    public void calculateDataValues()
    {
        statusHere = RUNNING;

            while(statusHere == RUNNING && thread == Thread.currentThread())
            {       
                if((System.currentTimeMillis()-now) > (30000))
                {
                    System.out.println("Timeout do aggregator"
                            + "finalizou!! Tamanho: "+dataValues.size());

                        for(int i = 0; i < dataValues.size(); i++)
                        {
                            System.out.println("TAMANHO VETOR - DATA_PACKETS: "+
                                dataValues.size());
                            
                            mean += ((Integer)dataValues.
                                    elementAt(i)).doubleValue();
                        }

                        mean = (double)mean/(double)dataValues.size();
                    
                    System.out.println("Aggregator calculou a "
                            + "seguinte media: "+mean);
                    sendAggregatedData();
                    stop();
                }
            }
    }

    public void sendAggregatedData()
    {
        System.out.println("Iniciando o processo de envio!");

        try
        {
            txConn = (RadiogramConnection) Connector.
                    open("radiogram://broadcast"+":"+CONNECTED_PORT);
            dg = (Radiogram) txConn.newDatagram(txConn.getMaximumLength());
            dg.writeByte(AGGREGATE_PACKET);
            dg.writeDouble(mean);
            txConn.send(dg);
            txConn.close();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }

        
    }

    public boolean start()
    {
        if (statusHere == STOPPED || statusHere == STOPPING)
        {
            statusHere = STARTING;

            thread = new Thread("Thread Aggregator") {
                public void run()
                {
                    calculateDataValues();
                }
            };
            thread.setPriority(Thread.MIN_PRIORITY + 3);
            thread.start();
            System.out.println("Starting Aggregator: " + name);
        }
        return true;
    }

    public boolean stop()
    {
        if (statusHere != STOPPED)
        {
            statusHere = STOPPING;
        }
        System.out.println("Stopping Aggregator: " + name);
        return true;
    }

    public boolean pause() 
    {
        return stop();
    }

    public boolean resume() 
    {
        return start();
    }

    public int getStatus() 
    {
        return statusHere;
    }

    public boolean isRunning() 
    {
        return statusHere == RUNNING;
    }

    public String getServiceName() 
    {
        return name;
    }

    public void setServiceName(String who)
    {
        if(who != null)
            this.name = who;
    }

    public boolean getEnabled() 
    {
        return false;
    }

    public void setEnabled(boolean bln) {
    }

    public void addTag(String string) 
    {

    }

    public void removeTag(String string) 
    {

    }

    public String[] getTags()
    {
        return null;
    }

    public boolean hasTag(String string)
    {
        return false;
    }

    public String getTagValue(String string)
    {
        return "";
    }


}
