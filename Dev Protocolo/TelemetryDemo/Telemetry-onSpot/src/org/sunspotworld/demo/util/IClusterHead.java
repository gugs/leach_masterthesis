/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.sunspotworld.demo.util;

/**
 *
 * @author USUARIO
 */
public interface IClusterHead
{

    public void sendAdvice();

    public void sendTDMA();

    public void receiveJoinRequest();

    public void sendDataToBaseStation();

}
