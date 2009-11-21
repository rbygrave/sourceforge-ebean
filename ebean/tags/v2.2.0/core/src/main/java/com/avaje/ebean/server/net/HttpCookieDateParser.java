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
package com.avaje.ebean.server.net;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Used to parse dates for HttpCookie.
 */
public class HttpCookieDateParser {

	static final String[] formatList = { "EEEE','dd-MMM-yy HH:mm:ss z",
			"EEE', 'dd-MMM-yyyy HH:mm:ss z", "EEE', 'dd MMM yyyy HH:mm:ss z",
			"EEEE', 'dd MMM yyyy HH:mm:ss z", "EEE', 'dd MMM yyyy hh:mm:ss z",
			"EEEE', 'dd MMM yyyy hh:mm:ss z", "EEE MMM dd HH:mm:ss yyyy",
			"EEE', 'dd-MMM-yyyy HH:mm:ss", "EEE MMM dd HH:mm:ss z yyyy" };

	SimpleDateFormat[] format;

	/**
	 * Create the Date Parser.
	 */
	public HttpCookieDateParser() {
		init();
	}

	private void init() {
		format = new SimpleDateFormat[formatList.length];
		for (int i = 0; i < format.length; i++) {
			format[i] = new SimpleDateFormat(formatList[i]);
		}
	}


	/**
	 * Parse the cookie date into a Date.
	 */
	public Date parse(String rfcDate) {

		for (int i = 0; i < format.length; i++) {
			try {
				//synchronized (this) {
					return format[i].parse(rfcDate);										
				//}
				
			} catch (ParseException e) {
			}
		}
		return null;
	}

}
