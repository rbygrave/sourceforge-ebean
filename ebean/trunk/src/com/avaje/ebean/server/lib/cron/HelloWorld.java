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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HelloWorld job used to for testing the CronManager.
 */
public class HelloWorld implements Runnable {

	private static final Logger logger = Logger.getLogger(HelloWorld.class.getName());

	public String toString() {
		return "Hello World";
	}

	public void run() {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS ");
			String now = sdf.format(new Date());
			logger.info("Hello World " + now + "  ... sleeping 20 secs");

			Thread.sleep(20000);
			logger.info("Hello World finished.");

		} catch (InterruptedException ex) {
			logger.log(Level.SEVERE, "", ex);
		}
	}

}
