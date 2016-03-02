/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld.demo.util;

import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.resources.Resources;
import com.sun.spot.resources.transducers.ILed;
import com.sun.spot.resources.transducers.ITemperatureInput;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.Datagram;
import org.sunspotworld.demo.PacketType;

/**
 *
 * @author USUARIO
 */
public class DataCollectorAndSender extends PeriodicTask implements PacketType
{
    private PacketTransmitter xmit;
    private long startTime;
    private ITemperatureInput tempSensor = (ITemperatureInput) Resources.lookup(ITemperatureInput.class);
    private int temperature;
    private ITriColorLED led = null;
    

    public DataCollectorAndSender(int timmer, ITriColorLED led)
    {
        //Construtor do PeriodicTask
        super(timmer);
        this.led = led;
        //this.xmit = xmit;
    }

    public void starting()
    {

    }

    public void doTask() 
    {
        int tries = 0;
        while(tries < 10)
        {
            led.setOff();
            led.setRGB(255, 0, 255);
            led.setOn();
            Utils.sleep(250);
            tries++;
        }
//        Datagram dg = null;
//        try
//        {
//            if(xmit != null)
//            {
//                xmit.setRadioPolicy(RadioPolicy.ON);
//            }
//            temperature = (int) tempSensor.getCelsius();
//            dg = xmit.newDataPacket(DATA_PACKET);
//            dg.writeInt(temperature);
//            xmit.send(dg);
//            xmit.setRadioPolicy(RadioPolicy.OFF);
//        }
//        catch (IOException ex)
//        {
//            ex.printStackTrace();
//        }
    }


}
