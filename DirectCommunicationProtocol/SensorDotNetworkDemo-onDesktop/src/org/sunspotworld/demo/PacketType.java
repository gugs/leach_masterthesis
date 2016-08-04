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
 * Packet types for LEACH Protocol on SunSPOT
 * 
 * @author Gustavo Nobrega
 * Date: January 5, 2016
 */
public interface PacketType
{
    /** Port to use to locate the host application. */
    public static final String BROADCAST_PORT = "44";
    /** Port to use to locate the host application. */
    public static final String BROADCAST_PORT_II = "45";
    /** Port to use for sending commands and replies between the SPOT and the host application. */
    public static final String CONNECTED_PORT = "43";

    public static final int SEND_RATE = 1000;
    // Commands & reply codes for packets'header

    public static final byte LOCATE_DISPLAY_SERVER_REQ  = 1;

    public static final byte DISPLAY_SERVER_QUITTING    = 3;

    public static final byte BLINK_LEDS_REQ             = 10;
    /** Client command to start LEACH engine */
    public static final byte START_ENGINE = 11;
    /** STOP LEACH's Engine     */
    public static final byte STOP_LEACH_ENGINE = 13;
    /** RESET LEACH'S Engine     */
    public static final byte RESET_ENGINE = 14;
    /** START LEACH's Engine II     */
    public static final byte START_APP = 15;

    public static final byte PERSISTENCE = 16;

    public static final byte ERASE_PERSISTENCE = 17;

    public static final byte DATA = 18;

    /** LEACH parameters */

    public static final byte TDMA_PACKET_SIZE = 100;
    public static final byte DATA_PACKET_SIZE = 100;
    public static final byte JOIN_PACKET_SIZE = 100;
    public static final byte ADV_PACKET_SIZE = 100;
    public static final byte AGGREGATE_PACKET_SIZE = 100;
    public static final byte OPERATING_LEACH_PACKET_SIZE = 100;
    public static final byte QTDDATAPKT = 5;
    public static final float CLUSTER_HEAD_QUANTITY = (float) 0.1;
    public static final int TIMEOUT_DATA_PACKET = 2000;

}