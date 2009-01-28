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

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A handler for using with Console output (System.out and or System.err).
 * <p>
 * This has modes to either use System.out or System.err or both where warnings
 * and severe messages are sent to System.err and all the rest to System.out.
 * </p>
 * <p>
 * In combination with the SimpleFormatter this provides a resonable default
 * ConsoleHandler for use during development.
 * </p>
 */
public class ConsoleHandler extends Handler implements HandlerConfigurable {

	/**
	 * Warning and Severe messages goto System.err and rest goto System.out.
	 */
	private static final int MODE_BOTH = 0;

	/**
	 * All messages goto System.out
	 */
	private static final int MODE_OUT = 1;

	/**
	 * All messages goto System.err
	 */
	private static final int MODE_ERR = 2;

	/**
	 * The mode (default is BOTH).
	 */
	int mode = MODE_BOTH;

	/**
	 * A writer to System.out
	 */
	ConsoleStreamWriter outWriter;

	/**
	 * A writer to System.err
	 */
	ConsoleStreamWriter errWriter;

	/**
	 * Create a ConsoleHandler.
	 */
	public ConsoleHandler() {
		configure();
		configureWriter();
	}

	/**
	 * Configure the mode, level, formatter, filter and encoding.
	 */
	protected void configure() {
		try {
			HandlerConfig config = new HandlerConfig(this);

			String modeVal = config.getProperty("mode", "both");
			mode = determineMode(modeVal);

			config.setLevel(Level.ALL);
			config.setFormatter(new DefaultConsoleFormatter());
			config.setFilter(null);
			config.setEncoding();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Determine the mode of OUT, ERR or both.
	 */
	protected int determineMode(String modeVal) {

		if ("out".equalsIgnoreCase(modeVal)) {
			return MODE_OUT;
		}
		if ("err".equalsIgnoreCase(modeVal)) {
			return MODE_ERR;
		}
		return MODE_BOTH;
	}

	/**
	 * Configure the writers.
	 */
	protected void configureWriter() throws SecurityException {

		String encoding = getEncoding();

		switch (mode) {
		case MODE_OUT:
			outWriter = new ConsoleStreamWriter(this, System.out, encoding);
			break;
		case MODE_ERR:
			errWriter = new ConsoleStreamWriter(this, System.err, encoding);
			break;
		case MODE_BOTH:
			errWriter = new ConsoleStreamWriter(this, System.err, encoding);
			outWriter = new ConsoleStreamWriter(this, System.out, encoding);
			break;

		default:
			throw new RuntimeException("Invalid mode " + mode);
		}
	}

	/**
	 * Publish a message to the appropriate writer.
	 */
	public void publish(LogRecord record) {
		if (!isLoggable(record)) {
			return;
		}
		String msg;
		try {
			msg = getFormatter().format(record);

		} catch (Exception ex) {
			reportError(null, ex, ErrorManager.FORMAT_FAILURE);
			return;
		}

		switch (mode) {
		case MODE_OUT:
			outWriter.write(msg);
			break;

		case MODE_ERR:
			errWriter.write(msg);
			break;

		case MODE_BOTH:
			boolean isErr = record.getLevel().intValue() > Level.INFO.intValue();
			if (isErr) {
				errWriter.write(msg);
			} else {
				outWriter.write(msg);
			}
			break;

		default:
			throw new RuntimeException("Incorrect mode " + mode);
		}
	}

	/**
	 * Flush any buffered messages.
	 */
	public synchronized void flush() {
		if (outWriter != null) {
			outWriter.flush();
		}
		if (errWriter != null) {
			errWriter.flush();
		}
	}

	/**
	 * Close the current output stream.
	 */
	public synchronized void close() throws SecurityException {
		if (outWriter != null) {
			outWriter.close();
		}
		if (errWriter != null) {
			errWriter.close();
		}
	}
}
