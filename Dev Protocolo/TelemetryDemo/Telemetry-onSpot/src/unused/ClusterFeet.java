/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package unused;

import com.sun.spot.io.j2me.radiogram.Radiogram;
import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.peripheral.TimeoutException;
import com.sun.spot.peripheral.radio.RadioPolicy;
import com.sun.spot.resources.transducers.ITriColorLED;
import com.sun.spot.util.IEEEAddress;
import com.sun.spot.util.Utils;
import java.io.IOException;
import javax.microedition.io.Connector;
import org.sunspotworld.demo.PacketTypes;

/**
 *
 * @author USUARIO
 */
public class ClusterFeet implements PacketTypes{

    private boolean checking = true;
    private ITriColorLED led = null;
    private long ourMacAddress;
    private long serviceAddress;

    public void setStatusLed(ITriColorLED led) {
        
    }

    public ClusterFeet(ITriColorLED newled)
    {

        this.led = newled;
        RadiogramConnection rcvConn = null;
        Radiogram rdg  = null;
        byte color = 0;
         while (checking ) {
                try {

                    rcvConn = (RadiogramConnection)Connector.open("radiogram://:" + BROADCAST_PORT );
                    rcvConn.setTimeout(300);    // timeout in 300 msec - so receive() will not deep sleep
                    rcvConn.setRadioPolicy(RadioPolicy.AUTOMATIC);  // but allow deep sleep other times
                    rdg = (Radiogram)rcvConn.newDatagram(20);
                    } catch (Exception ex) {
                    System.out.println("Error trying to locate remote display server: " + ex.toString());
                    ex.printStackTrace();
                     // loop to locate a remote print server
                        while (checking) {
                            int tries = 0;
                            boolean found = false;
                            if (led != null) {
                                led.setRGB(60,40,0);                // Yellow = looking for display server
                                led.setOn();
                            }
                            do {
                                try {
                                    rdg.reset();
                                    rcvConn.receive(rdg);              // wait until we receive a request
                                    if (rdg.readByte() == ADV_PACKET) {  // type of packet
                                        long replyAddress = rdg.readLong();
                                        //if (replyAddress == ourMacAddress) {
                                            String addr = rdg.getAddress();
                                            IEEEAddress ieeeAddr = new IEEEAddress(addr);
                                            serviceAddress = ieeeAddr.asLong();
                                            found = true;
                                            color = rdg.readByte();
                                        //}
                                    }
                                   } catch (IOException ex2) { /* ignore - just return false */ }
                                Utils.sleep(20);  // wait 20 msecs
                                ++tries;
                            } while (checking && !found && tries < 5);
                            if (led != null) {
                                led.setOff();
                            }
                            if (found) {
                                checking = false;
                                led.setRGB((int) color, (int) color, (int) color);
                                //listener.serviceLocated(serviceAddress);    //report success
                                break;
                            } else {
                                if (led != null) {
                                    led.setRGB(80,0,0);             // Red = still looking for service
                                    led.setOn();
                                }
                                if (checking) {
                                    Utils.sleep(1000);     // wait a while before looking again
                                }
                            }
                        }
                } finally {
                    try {
                        if (led != null) {
                            led.setOff();
                        }

                        if (rcvConn != null) {
                            rcvConn.close();
                            rcvConn = null;
                        }
                    } catch (IOException ex) { /* ignore */ }
                }
            }


       }



}
