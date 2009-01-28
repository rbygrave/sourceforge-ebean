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
package org.avaje.lib.log;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Utility object used in logging with daily file rotation.
 */
public class LogTime {

	private static LogTime current = new LogTime();
	
	/**
	 * Always returns a LogTime that is current.
	 */
	public static LogTime getCurrent() {
		if (!current.isNextDay()){
			return current;
		} else {
			LogTime newTime = new LogTime();
			current = newTime;
			return newTime;
		}
	}
	
	private static final String[] sep = { ":", "." };

	/**
	 * The day in yyyyMMdd format.
	 */
	private final String ymd;

	/**
	 * The day start time in System millis.
	 */
	private final long startMidnight;

	/**
	 * Tomorrow start time in System millis.
	 */
	private final long startTomorrow;

	/**
	 * Create a LogTime representing today.
	 */
	public LogTime() {

		//Because every variable is private final 
		//the constructor should be thread
		//safe (In JDK5+).
		 
		GregorianCalendar now = new GregorianCalendar();
		// dstOffset = cal.get(Calendar.DST_OFFSET);

		now.set(Calendar.HOUR_OF_DAY, 0);
		now.set(Calendar.MINUTE, 0);
		now.set(Calendar.SECOND, 0);
		now.set(Calendar.MILLISECOND, 0);

		this.startMidnight = now.getTime().getTime();
		this.ymd = getDayDerived(now);
		
		now.add(Calendar.DATE, 1);
		this.startTomorrow = now.getTime().getTime();
	}

	/**
	 * Return true if we have moved into tomorrow. This is used to trigger log
	 * switching if required.
	 */
	public boolean isNextDay() {
		return (System.currentTimeMillis() >= startTomorrow);
	}

	/**
	 * Return the Year Month Day for today.
	 */
	public String getYMD() {
		return ymd;
	}

	/**
	 * Return the current time specify the separators.
	 * <p>
	 * The separators is a String[2] with the first string separating hours
	 * minutes and seconds and the second separating seconds from millis.
	 * </p>
	 * <p>
	 * The default is {":","."}
	 * </p>
	 */
	public String getNow(String[] sep, boolean withMillis) {

		StringBuilder sb = new StringBuilder();
		getTime(sb, System.currentTimeMillis(), startMidnight, sep, withMillis);
		return sb.toString();
	}

	/**
	 * Returns the current time.
	 * <p>
	 * Format used is hours:minutes:seconds.millis
	 * </p>
	 */
	public String getNow() {
		return getNow(sep, true);
	}

	/**
	 * Return the current time including millis.
	 */
	public String getNow(boolean withMillis) {
		return getNow(sep, withMillis);
	}

	/**
	 * Set the derived day information.
	 */
	private String getDayDerived(Calendar now) {

		int year = now.get(Calendar.YEAR);
		int month = now.get(Calendar.MONTH);
		int day = now.get(Calendar.DAY_OF_MONTH);

		month++;

		StringBuilder sb = new StringBuilder();

		format(sb, year, 4);
		format(sb, month, 2);
		format(sb, day, 2);

		return sb.toString();
	}

	private void getTime(StringBuilder sb, long time, long midnight, String[] sep, boolean withMillis) {

		long rem = time - midnight;// startMidnight;

		long millis = rem % 1000;
		rem = rem / 1000;
		long secs = rem % 60;
		rem = rem / 60;
		long mins = rem % 60;
		rem = rem / 60;
		long hrs = rem;

		format(sb, hrs, 2);
		sb.append(sep[0]);
		format(sb, mins, 2);
		sb.append(sep[0]);
		format(sb, secs, 2);
		if (withMillis){
			sb.append(sep[1]);
			format(sb, millis, 3);
		}
	}

	private void format(StringBuilder sb, long value, int places) {
		String format = Long.toString(value);

		int pad = places - format.length();
		for (int i = 0; i < pad; i++) {
			sb.append("0");
		}
		sb.append(format);
	}

}
