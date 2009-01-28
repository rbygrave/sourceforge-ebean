/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package org.avaje.ebean.server.transaction.log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

/**
 * Default transaction logger implementation.
 * <p>
 * File based logger that can switch daily. It will include the date in the
 * files name.
 * </p>
 * <p>
 * Administration Note: If log file switching fails it will send the error to
 * standard out and standard err print streams. This is assumed to be rather
 * unlikely but possible.
 * </p>
 */
public class DefaultTransactionLogger implements TransactionLogger {

	static final Logger logger = Logger.getLogger(DefaultTransactionLogger.class.getName());
	
	/**
	 * Used to print stack trace.
	 */
	private static final String atString = "        at ";
	
	/**
	 * The output stream.
	 */
	private PrintStream out;

	/**
	 * Whether to append or replace.
	 */
	private boolean doAppend = true;

	/**
	 * Flag to indicate the logger if the logger has been closed.
	 */
	private boolean open = true;
	
	/**
	 * The current file path.
	 */
	private String currentPath;
	
	/**
	 * The path of the log file.
	 */
	private final String filepath;

	/**
	 * Set to true if using daily file switching.
	 */
	private final boolean useFileSwitching;

	/**
	 * The maximum number of stack lines to output.
	 * This is just for transaction logging so 5 is fine.
	 */
	private final int maxStackTraceLines = 5;

	/**
	 * The delimiter to use.
	 */
	private final String deliminator;

	/**
	 * Object used to synch the file switching.
	 */
	private final Object fileMonitor = new Object();

	/**
	 * The prefix of the log file name.
	 */
	private final String logFileName;

	/**
	 * The file suffix for the logs.
	 */
	private final String logFileSuffix;

	/**
	 * The newLineChar used instead of NL or CRNL.
	 */
	private final String newLineChar = "\\r\\n";

	private final boolean csv;
	
	/**
	 * Create a logger with a logFileName and useFileSwitching flag.
	 * 
	 * @param dir
	 *            the sub directory to put the log. Can be null.
	 * @param logFileName
	 *            the prefix log file name.
	 * @param useFileSwitching
	 *            if true then use daily file switching.
	 */
	public DefaultTransactionLogger(String dir, String logFileName, boolean useFileSwitching, String suffix) {
		this.logFileName = logFileName;
		this.useFileSwitching = useFileSwitching;
		this.logFileSuffix = "."+suffix;
		csv = "csv".equalsIgnoreCase(suffix);
		deliminator = csv ? "," : ", ";
		try {

			// get the directory where the log files are going to go
			filepath = makeDirIfRequired(dir);

			switchFile(LogTime.nextDay());

		} catch (Exception e) {
			// Not going to use logger to show logger exceptions...
			// Using standard out and standard err instead.
			System.out.println("FATAL ERROR: init of FileLogger: " + e.getMessage());
			System.err.println("FATAL ERROR: init of FileLogger: " + e.getMessage());
			throw new RuntimeException(e);
		}
	}

	public DefaultTransactionLogger(String dir, String logFileName, boolean useFileSwitching) {
		this(dir, logFileName, useFileSwitching, "log");
	}

	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/**
	 * Close the file output stream being used for logging.
	 */
	public void close() {
		if (open) {
			out.flush();
			out.close();
			open = false;
		}
	}
	
	public void log(String msg) {
		log(null, msg, null);
	}
	
	public void log(String msg, Throwable e) {
		log(null, msg, e);
	}
	
	/**
	 * Log the event to the file.
	 */
	public void log(String transId, String msg, Throwable e) {

		// check to see if we need to switch file?
		LogTime logTime = LogTime.get();
		if (logTime.isNextDay()) {
			logTime = LogTime.nextDay();
			try {
				switchFile(logTime);
			} catch (Exception ex) {
				// This is a pretty serious error... not logging it though as
				// this could be recursive so just going to send to std err
				ex.printStackTrace();
			}
		}

		// prefix of transID and timestamp ~ 40 chars
		int roughSize = 40;
		if (msg != null) {
			roughSize += msg.length();
		}
		if (e != null) {
			roughSize += 200;
		}

		StringBuilder line = new StringBuilder(roughSize);
		if (transId != null){
			line.append("trans[").append(transId).append("]").append(deliminator);
		}
		
		if (csv){
			line.append("\"'");
		}
		line.append(logTime.getNow());
		if (csv){
			line.append("'\"");
		}
		line.append(deliminator);

		if (msg != null) {
			line.append(msg).append(" ");
		}

		printThrowable(line, e, false);

		String lineString = line.toString();

		synchronized (fileMonitor) {
			out.println(lineString);
			// without flush automatic close() *MUST* be called
			out.flush();
		}
	}

	/**
	 * Recursively output the Throwable stack trace to the log.
	 * 
	 * @param sb
	 *            the buffer to write the stack trace to
	 * @param e
	 *            the source throwable
	 * @param isCause
	 *            flag to indicate if this is the top level throwable or a cause
	 */
	protected void printThrowable(StringBuilder sb, Throwable e, boolean isCause) {
		if (e != null) {
			if (isCause) {
				sb.append("Caused by: ");
			}
			sb.append(e.getClass().getName());
			sb.append(":");
			sb.append(e.getMessage()).append(newLineChar);

			StackTraceElement[] ste = e.getStackTrace();
			int outputStackLines = ste.length;
			int notShownCount = 0;
			if (ste.length > maxStackTraceLines) {
				outputStackLines = maxStackTraceLines;
				notShownCount = ste.length - outputStackLines;
			}
			for (int i = 0; i < outputStackLines; i++) {
				sb.append(atString);
				sb.append(ste[i].toString()).append(newLineChar);
			}
			if (notShownCount > 0) {
				sb.append("        ... ");
				sb.append(notShownCount);
				sb.append(" more").append(newLineChar);
			}
			Throwable cause = e.getCause();
			if (cause != null) {
				printThrowable(sb, cause, true);
			}
		}
	}

	/**
	 * Creates a new file and sets the file logging output to be directed to the
	 * new file.
	 * 
	 * @exception Exception
	 *                indicates a problem writing to the new log file.
	 */
	protected void switchFile(LogTime logTime) throws Exception {

		String newFilePath = filepath + File.separator + logFileName;

		if (useFileSwitching) {
			// For file switching include the date in the file name
			newFilePath = newFilePath + logTime.getYMD() + logFileSuffix;

		} else {
			newFilePath = newFilePath + logFileSuffix;
		}

		// Try to open an output stream to the file
		synchronized (fileMonitor) {

			if (!newFilePath.equals(currentPath)) {
				currentPath = newFilePath;

				out = new PrintStream(new BufferedOutputStream(new FileOutputStream(newFilePath,
						doAppend)));
			}
		}
	}

	/**
	 * Returns the directory path of the log file.
	 */
	protected String makeDirIfRequired(String dir) {

		File f = new File(dir);
		if (f.exists()){
			if (!f.isDirectory()){
				String msg = "Transaction logs directory is a file? "+dir;
				throw new PersistenceException(msg);			
			}
		} else {
			if (!f.mkdirs()) {
				String msg = "Failed to create transaction logs directory "+dir;
				logger.log(Level.SEVERE, msg);
			} 
		}
		return dir;
	}

}
