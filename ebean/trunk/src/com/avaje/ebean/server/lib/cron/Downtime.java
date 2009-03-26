/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.server.lib.cron;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;


/**
 * Sets a downtime flag on the CronManager.
 * Requires two parameters in system.properties.
 * <p>
 * In this example at 23:25 every night downtime is set for
 * a duration of 10 seconds.
 * </p>
 * <p>
 * This is useful where say the database is down for backup etc
 * and you don't want to run various things like database heartbeat
 * tests and any regularly run jobs etc.
 * </p>
 * <p>
 * SystemProperties:<br />
 * <pre><code>
 * ## schedule for 11:25pm every night
 * system.downtime.schedule=25 23 * * *
 * 
 * ## downtime lasts for 10 seconds
 * system.downtime.duration=10
 * </code></pre>
 * </p>
 */
public class Downtime implements Runnable {

	private static final Logger logger = Logger.getLogger(Downtime.class.getName());
	
    private CronManager manager;
    
    /**
     * Create the Downtime.
     */
    public Downtime(CronManager manager) {
        this.manager = manager;
    }
    
    /**
     * Run the downtime.
     */
    public void run() {
        
    	ConfigProperties properties = GlobalProperties.getConfigProperties();
        String downtime = properties.getProperty("system.downtime.duration");
        if (downtime == null){
            logger.info("system.downtime not set");
            
        } else {
            int downTimeSecs = Integer.parseInt(downtime);
            
            int offsetSecs = 2;
            
            long offsetTime = System.currentTimeMillis() + offsetSecs*1000;
            long endTime = System.currentTimeMillis() + downTimeSecs*1000;
            
            try {
                boolean isFinished = false;
                if (offsetSecs > 0){
                    // wait a little bit first.  This is so that jobs
                    // that run exactly on the minute get run.
                    // We wait for 2 seconds or so before setting downtime
                    // to be true.
		            while(!isFinished) {
		                Thread.sleep(500);
		                if (System.currentTimeMillis() >= offsetTime){
		                    isFinished = true;
		                }
		            }
                }
	            
                // ok, set the downtime flag and start sleeping
                // until the downtime is finished
                manager.setDowntime(true);
	            isFinished = false;
            
	            while(!isFinished) {
	                Thread.sleep(500);
	                if (System.currentTimeMillis() >= endTime){
	                    isFinished = true;
	                }
	            }
            } catch (InterruptedException ex){
                logger.log(Level.SEVERE, "", ex);
            }
            
            // set the flag indicating downtime has finished
            manager.setDowntime(false);
            
        } 
    }
    
    public String toString() {
        return "System Downtime";
    }
}
