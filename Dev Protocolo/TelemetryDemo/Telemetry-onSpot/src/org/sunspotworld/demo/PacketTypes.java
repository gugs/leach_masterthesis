/*
 * Copyright (c) 2007 Sun Microsystems, Inc.
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

/**
 * Packet types for TelemetryDemo
 * 
 * @author Ron Goldman
 * Date: January 15, 2007
 * Revised: August 1, 2007
 * Revised: August 1, 2010
 */
public interface PacketTypes {

    /** Port to use to locate the host application. */
    public static final String BROADCAST_PORT = "42";
    /** Port to use for sending commands and replies between the SPOT and the host application. */
    public static final String CONNECTED_PORT = "43";
          
    // Command & reply codes for data packets
    
    /** Client command to locate a display server. */
    public static final byte LOCATE_DISPLAY_SERVER_REQ  = 1;    // sent to display host (broadcast)
    /** Host command to indicate it is restarting. */
    public static final byte DISPLAY_SERVER_RESTART     = 2;    // sent to any clients (broadcast)
    /** Host command to indicate it is quitting. */
    public static final byte DISPLAY_SERVER_QUITTING    = 3;    // (direct p2p)
    /** Host command to request the current accelerometer scale & calibration. */

    public static final byte TDMA_PACKET = 4;

    public static final byte DATA_PACKET = 5;

    public static final byte ADV_PACKET = 6;

    public static final byte JOIN_PACKET = 7;

    public static final byte AGGREGATE_PACKET = 8;

    /** Host command to ping the remote SPOT and get the radio signal strength. */
    public static final byte PING_REQ                   = 9;
    /** Host command to blink the remote SPOT's LEDs. */
    public static final byte BLINK_LEDS_REQ             = 10;

    //Implementacao do LEACH

    /** Client command to start LEACH engine */
    public static final byte START_LEACH_ENGINE = 11;

    /** Reply command about START_LEACH_ENGINE*/
    public static final byte REPLAY_LEACH_ENGINE = 12;

    public static final byte STOP_LEACH_ENGINE = 13;

    public static final byte RESET_LEACH_ENGINE = 14;

    public static final byte START_APP = 15;

    /** Client reply to a ping includes the radio signal strength & battery level. */
    public static final byte PING_REPLY                 = 110;
    /** Client reply with any error message for the host to display. */
    public static final byte MESSAGE_REPLY              = 111;

    public static final byte DISPLAY_SERVER_AVAIL_REPLY = 101;
    
    /** Cluster Head Percentual*/
    public static final float PERCENTAGE_CLUSTER_HEAD = (float) 0.1;

    public static final int TIMEOUT_DATA_PACKET = 30000;

}