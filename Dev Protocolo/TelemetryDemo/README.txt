LEACH Protocol Implementation Over Oracle SunSPOT Platform

Author: Gustavo Nobrega Martins
Email: gustavonobrega@gmail.com
Github: https://github.com/gugs/leach_masterthesis/

Advisors: Prof. D.Sc. Reinaldo César Gomes de Morais; 
	  Prof. Ph.D. Marcelo Sampaio de Alencar 

Purpose: Master Thesis Application
June 1, 2016,
  
This code has third part software (Receive Packet skeleton) copied from Telemetry Demo Project - sdk code examples.

Details about third's part softawre author:

 * author Ron Goldman<br>
 * Date: May 8, 2006,
 * revised: August 1, 2007
 * revised: July 25, 2008
 * revised: August 1, 2010 

--------------------
TERMS AND CONDITION
--------------------

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



--------
OVERVIEW
--------

This is a LEACH (Low Energy Adaptative Clustering Hierarchy) protocol[1] implementation developed in Java2ME with deployment target on Oracle SUnSPOT platform. LEACH protocol was developed by Heinzelman et al concerning energy aware routing protocol. LEACH protocol comprises hierarchical routing protocols applied to WSN (Wireless Sensor Network). More informations about LEACH protocol, please check out external reference in the end of this file.

--------------
HOW TO INSTALL
--------------

To deploy this implementation on SunSPOT motes, you can adopt three differents ways to do that: ant file script, solarium application and Netbeans IDE:

1 - ant file script: After sdk installation and environment settings done, you can run the follow command at LEACH's folder: ant deploy . Be sure SunSPOT is connected in USB port and properly installed on operating system.

2 - solarium: After plugging USB cable into SunSPOT mote, with solarium open, right click over node target, choose deploy option and open up xml file located in LEACH's folder.  

3 - Netbeans: Right click over project's folder and select "build and deploy" option with USB cable plugged into SunSPOT mote.

OBS: To perform these procedures above, motes in power on is required.

--------------------------
HOW TO LAUNCH SOLARIUM APP
--------------------------

Starting in the directory containing this README file, execute

            % cd Telemetry-onSpot
            % ant solarium

        to start up the Solarium tool which also includes the SPOT
        emulator.
 
          i.  Create a virtual SPOT by choosing Emulator -> New Virtual
              SPOT.
         ii.  Right click on the virtual SPOT, choose "Deploy a MIDlet 
              bundle ..." from the menu. A file chooser window will pop 
              up. Navigate to the Telemetry-onSPOT subdirectory and 
              select the "build.xml" file. This will automatically 
              cause the application to be compiled and loaded into the
              virtual SPOT.
        iii.  Right click on the virtual SPOT, choose "Display application
              output" -> "in Internal Frame" to open a window where
              output from the application will be displayed.
         iv.  Right click on the virtual SPOT, choose "Display sensor
              panel" to open up a multi-tabbed panel that allows you
              to manipulate various sensor readings experienced by the
              virtual SPOT. Select the tab marked "Accel".
          v.  Start the application by right clicking on the virtual 
              SPOT and selecting "Run MIDlet" -> "TelemetryMain".

-------------------------
Sensors' LEDs Indication 
-------------------------

LEDS: [1][2][3][4][5][6][7][8]

[1] Red: Application is running but not started. Waiting packet from basestation to start LEACH's engine;
    GREEN: Protocol is running and engine was started;

[2..5] WHITE: The LED position is a manner to identify the clusters among CH and non-CH;

[6] ORANGE: Coordinator is waiting JOIN requests from non-coordinator nodes. It is only possible observe this state 	    when all JOIN requests didn't reach the coordinator node. 
    CYAN: Coordinator received JOIN request from non-coordinator nodes and step-in TDMA phase.

[8] RED: If red is on, it means nodes was coordinator already, otherwise node will become coordinator (1/PERCENTAGE) 	 rounds;

[1..8] WHITE_FLASHES 4 times: Erasing storage

[1..8] WHITE_FLASHES 5 times: Test communication between basestation and motes

[1..8] WHITE_FLASHES Incremental: It means current round of LEACH's cycle   


 
















EXTERNAL REFERENCE:

[1] https://pdos.csail.mit.edu/archive/decouto/papers/heinzelman00.pdf