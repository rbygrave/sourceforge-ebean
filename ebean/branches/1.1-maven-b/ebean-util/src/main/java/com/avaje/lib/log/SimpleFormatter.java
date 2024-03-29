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
package com.avaje.lib.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Print a brief summary of the LogRecord in a human readable format. Designed
 * to be used during development with the idea of making the logging easier to
 * read (by outputting less).
 * <p>
 * This is useful for single line output logging with or without time.
 * </p>
 * <p>
 * The point of this is that I generally find that multi line logging (like the
 * JULI default) is harder to read. This seems like a reasonable format to use
 * during development.
 * </p>
 */
public class SimpleFormatter extends Formatter {

	private static final String lineSeparator = "\r\n";

	boolean withMillis;

	boolean withTime;

	public SimpleFormatter() {
		configure();
	}

	/**
	 * Create additionally specifying a logtime format of null, none, millis or sec.
	 * <p>
	 * Refer to setLogTimeFormats().
	 * </p>
	 */
	public SimpleFormatter(String logTime) {
		setLogTimeFormat(logTime);
		configure();
	}
	
	/**
	 * Configure the Formatter by reading properties if they have been set.
	 */
	protected void configure() {
		String cn = this.getClass().getName();
		String format = LogManager.getLogManager().getProperty(cn+".format");
		setLogTimeFormat(format);
	}
	
	/**
	 * Set whether logTime should be included in the output.
	 * <p>
	 * <ul>
	 * <li>millis = include logTime with milliseconds precision</li>
	 * <li>sec = include logTime with seconds precision</li>
	 * <li>none = no logTime included</li>
	 * </ul>
	 * </p>
	 * @param format
	 */
	public void setLogTimeFormat(String format){
		if (format == null){
			return;
		}
		format = format.toLowerCase();
		if (format.indexOf("millis") > -1){
			withTime = true;
			withMillis = true;
		} else if (format.indexOf("sec") > -1) {
			withTime = true;
			withMillis = false;
		} else if (format.indexOf("time") > -1) {
			withTime = true;
			withMillis = false;
		} else {
			// default to not show logTime...
			withTime = false;
			withMillis = false;	
		}
	}
	
	private String getTime() {

		return LogTime.getCurrent().getNow(withMillis);
	}

	/**
	 * Format the given LogRecord.
	 */
	public String format(LogRecord record) {

		StringBuilder sb = new StringBuilder();

		String message = formatMessage(record);

		sb.append(record.getLevel().getLocalizedName());
		sb.append(": ");
		if (withTime) {
			sb.append(getTime());
			sb.append(" ");
		}

		sb.append(message);
		sb.append(lineSeparator);
		if (record.getThrown() != null) {
			try {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		return sb.toString();
	}
}
