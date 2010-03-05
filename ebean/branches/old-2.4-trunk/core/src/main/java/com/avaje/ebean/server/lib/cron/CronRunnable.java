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

import java.util.Calendar;

/**
 * Wraps a Runnable with a cron like schedule.
 */
public class CronRunnable {

	boolean isEnabled = true;
	
	CronSchedule schedule;
	
	Runnable runnable;
	
    /**
     * Create with a schedule to parse.
     */
	public CronRunnable(String schedule, Runnable runnable){
		this(new CronSchedule(schedule), runnable);
	}
	
    /**
     * Create with a preparsed schedule.
     */
	public CronRunnable(CronSchedule schedule, Runnable runnable){
		this.schedule = schedule;
		this.runnable = runnable;
	}
	
    public boolean equals(Object obj) {
        if (obj == null){
            return false;
        }
        if (obj instanceof CronRunnable){
            return hashCode() == obj.hashCode();
        } 
        return false;
    }
    
    public int hashCode() {
        int hc = CronRunnable.class.getName().hashCode();
        hc += 31*hc + schedule.hashCode();
        hc += 31*hc + runnable.hashCode(); 
        return hc;
    }
    	
	


	/**
     * Returns true if it should be run this minute.
	 */
	public boolean isScheduledToRunNow(Calendar thisMinute) {
		return isEnabled && schedule.isScheduledToRunNow(thisMinute);
	}

	/**
	 * Sets the cron like schedule definition string.
	 * <p>Example '59 23 * * * ' == fire every day at 23:59.</p>
	 */
	public void setSchedule(String scheduleLine) {
		schedule.setSchedule(scheduleLine);
	}
	
    /**
     * Returns the schedule.
     */
	public String getSchedule() {
		return schedule.getSchedule();
	}

    /**
     * Return the underlying Runnable. 
     */
	public Runnable getRunnable() {
		return runnable;
	}
    
    /**
     * Set the runnable to use.
     */
    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    /**
     * Return true if this in enabled to run.
     */
    public boolean isEnabled() {
		return isEnabled;
	}

    /**
     * Set the to false to disable the Runnable.
     * With this false the job will not be run.
     */
    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public String toString() {
		return "CronRunnable"
			+": isEnabled["+isEnabled
			+"] sch["+schedule.getSchedule()
			+"] ["+runnable.toString()+"]";
	}
}
