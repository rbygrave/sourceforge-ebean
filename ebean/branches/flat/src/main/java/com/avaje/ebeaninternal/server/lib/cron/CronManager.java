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
package com.avaje.ebeaninternal.server.lib.cron;

import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.avaje.ebeaninternal.server.lib.thread.ThreadPool;
import com.avaje.ebeaninternal.server.lib.thread.ThreadPoolManager;

/**
 * Manages and Schedules Runnables.
 *
 * <p>The CronManager wakes up every minute, and fires off any scheduled jobs.
 * Note that the scheduled Jobs are run in their own background thread.</p>
 *
 * <p>CronSchedule describes a schedule it should executed on.</p>
 */
public final class CronManager {

	private static final Logger logger = Logger.getLogger(CronManager.class.getName());
	
    private static class CronManagerHolder {
    	private static CronManager me = new CronManager();
    }
    
    private boolean running = true;
    
    /**
     * The threadpool used to run the scheduled runnables.
     */
    private ThreadPool threadPool;
    
    /**
     * The list of CronRunnable.
     */
    private Vector<CronRunnable> runList;

    /**
     * The background thread that checks the schedules every minute.
     */
    private Thread backgroundThread;

    /** 
     * flag to indicate if downtime is currently on.
     */
    private boolean isDowntime = false;
    
    /**
     * Additional small delay past the minute.
     * This is to handle the Thread.sleep() waking some small time too early.
     * That is I'm aiming for the thread to wake up 10 millis after the minute has
     * passed.  This is because Thread.sleep() only approximates the sleep time.
     */
    private static final long SMALL_DELAY = 10;
	    
	/** 
	 * Singleton private constructor.
	 */
	private CronManager() {
	    runList = new Vector<CronRunnable>(); 
        threadPool = ThreadPoolManager.getThreadPool("CronManager");
        
        backgroundThread = new Thread(new Runner(),"CronManager Daemon");
        backgroundThread.setDaemon(true);
        backgroundThread.start();
        
    }
    
    private void init() {
		CronRunnable sr = new CronRunnable("* * * * *", new HelloWorld());
		sr.setEnabled(false);
		add(sr);
		
		// create assuming it is disabled
	    CronRunnable dt = new CronRunnable("25 23 * * *", new Downtime(this));
	    dt.setEnabled(false);
	    
		String downtimeSchedule = GlobalProperties.get("system.downtime.schedule", null);
		if (downtimeSchedule != null){
		    // there is acutally downtime
			dt.setSchedule(downtimeSchedule);
			dt.setEnabled(true);
		} 
		add(dt);
	}

	
	/**
	 * Return true if the CronManager is currently in downtime.
	 */
	public static boolean isDowntime() {
	    return getInstance().isDowntime;
	}
	
	/**
	 * Set the downtime to be on or off.
	 * This is called by Downtime which is scheduled if the property
	 * <code>system.downtime.schedule</code> is set.
	 */
	protected void setDowntime(boolean isDowntime){
	    this.isDowntime = isDowntime;
	    if (isDowntime){
	        String duration = GlobalProperties.get("system.downtime.duration", null);
	        logger.warning("System downtime has started for ["+duration+"] seconds");
	    } else {
	        logger.warning("System downtime has finished.");	        
	    }
	}

	/**
	 * Use this to temporarily turn off processing.
	 */
	public static void setRunning(boolean running){
		CronManagerHolder.me.running = running;
	}
	
	private void runScheduledJobs() {
		if (!running){
			// temporarily not going to try to run any scheduled
			// tasks
			return;
		}
		
		//synchronized(jobMonitor){
			// Get now time rounding it to the minute that has just ticked over.
			// Note that I add 5 secs to the current time in case we are in the 59th second
			// before I then round it down to the minute that has just ticked over.
			Date nowDate = new Date(((long)(System.currentTimeMillis()+5000)/60000)*60000);
			GregorianCalendar thisMinute = new GregorianCalendar();
			thisMinute.setTime(nowDate);
	
			Enumeration<CronRunnable> en = runList.elements();
			while (en.hasMoreElements()) {
				CronRunnable sr = (CronRunnable)en.nextElement();
				
				if (sr.isScheduledToRunNow(thisMinute)) {
					threadPool.assign(sr.getRunnable(),true);
					
				} 
			}
		//}
	}

	/**
	 *  Return the singleton instance.
	 */
	private static CronManager getInstance() {
		return CronManagerHolder.me;
	}

	/**
	 * Add a CronRunnable to the list of system jobs.
	 */
	public static void add(String schedule, Runnable runnable) {
		CronRunnable sr = new CronRunnable(schedule, runnable);
		add(sr);
	}
		
    /**
     * Add a CronRunnable to the list of system jobs.
     */
	public static void add(CronRunnable runnable) {
        getInstance().runList.add(runnable);
	}
	
	/**
	 * Returns an iterator of CronRunnable objects.
	 */
	public static Iterator<CronRunnable> iterator() {
	    return getInstance().runList.iterator();
	}

    private class Runner implements Runnable {
        
        public void run() {
            init();
                        
            while (true) {
                try {
                	
                    // synch up the next fire to the 0 second.
                    // calculate the time to the next minute... and wait.
                    long nextMinute = ((long)System.currentTimeMillis()/60000)*60000 + 60000;
                    long now = System.currentTimeMillis();
                    long nextSleepTime = nextMinute - now + SMALL_DELAY;
                    if (nextSleepTime > 0) {
                        // synch up to the 0 second of each minute that elaspes.
                        // NOTE: this is not EXACT  but fairly close depending on JVM and o/s.
                        Thread.sleep(nextSleepTime);
                    } else {
                        // this occurs when edit mode is used on a console window...
                    }
                    long additionalDelay = nextMinute - System.currentTimeMillis();
                    if ( additionalDelay > 0 ) {
                        // if we have woken up just before the minute ticks over...
                        // wait a little longer still.  This possibly will never occur.
                        Thread.sleep(additionalDelay + 20);
                    }

                    // Don't run if we are stopping
                    boolean stopping = ShutdownManager.isStopping();
                    if (!stopping){
                    	runScheduledJobs();
                    }                    

                    // if the runScheduledJobs executes in the 59th second then it
                    // can execute multiple times (very quickly) before the minute is up.
                    // This 5 second sleep makes sure the minute has ticked over... and so
                    // ensures only one runScheduledJobs() fires per minute.
                    // Another option would be to fire on the First second (rather than 0) perhaps?
                    Thread.sleep(5000);

                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "", e);
                }
            } 
        }
    }
	
}
