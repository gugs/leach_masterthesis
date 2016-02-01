package unused;

import com.sun.spot.io.j2me.radiogram.RadiogramConnection;
import com.sun.spot.util.Utils;
import java.io.IOException;
import java.util.Random;
import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import org.sunspotworld.demo.PacketTypes;
import org.sunspotworld.demo.util.IClusterHead;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author USUARIO
 */
public class ClusterHead implements PacketTypes, IClusterHead {

    private byte color = 0;

    public byte getColor(){
        return color;
    }

    public void sendAdvice(){
        Utils.sleep(200);
        Datagram txDG = null;
        RadiogramConnection txConn = null;
        try 
        {
            txConn = (RadiogramConnection) Connector.open("radiogram://broadcast:" + BROADCAST_PORT);
            txDG.writeByte(ADV_PACKET);
            Random  r = new Random();

            color = (byte) (r.nextInt()*255);
            txDG.writeByte(color);
            txConn.send(txDG);
            
            //txDG.writeByte(ADV_PACKET);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    public void sendTDMA() {
    }

    public void receiveJoinRequest() {
    }

    public void sendDataToBaseStation() {
    }




}



