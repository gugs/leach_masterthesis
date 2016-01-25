/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld.demo;

/**
 *
 * @author USUARIO
 */
public interface PacketType
{

    public static final String BROADCAST_PORT = "42";

    public static final String CONNECTION_PORT = "43";

    public static final byte LOCATE_DISPLAY_SERVER_REQ = 1;
    
    public static final byte DISPLAY_SERVER_RESTART = 2;

    /** Host command to ping the remote SPOT and get the radio signal strength. */
    public static final byte PING_REQ = 9;

    public static final byte START_LEACH_ENGINE = 11;

    //public static final byte REPLAY_LEACH_ENGINE = 12;

    public static final byte DISPLAY_SERVER_AVAIL_REPLY = 101;

    public static final byte BLINK_LEDS_REQ = 10;

    /** Client reply to a ping includes the radio signal strength & battery level. */
    public static final byte PING_REPLY                 = 110;

}
