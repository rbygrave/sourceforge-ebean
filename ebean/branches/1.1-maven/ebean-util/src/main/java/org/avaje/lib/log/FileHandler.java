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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.avaje.lib.util.StringHelper;

/**
 * A handler that writes to a file and automatically handles daily log rotation if required.
 * <p>
 * In logging.properties you can specify a pattern property, which is used to determine where
 * the log file name, location and if it rotates daily.
 * </p>
 * <p>
 * The pattern can contain %t, %h, %d expressions (or avaje type expressions such as ${CATALINA_HOME}
 * which can be used in SystemProperties.
 * </p>
 * <p>
 * %t = ${java.io.temp} = the temporary directory <br>
 * %h = ${user.home} = the user home directory <br>
 * %d = the date for daily file rotation (in yyyyMMdd format)
 * </p>
 * <p>
 * Some examples are:
 * <code>
 * <pre>
 * #non rotating, in working directory
 * org.avaje.lib.log.AuxFileHandler.pattern=mylog.log
 * 
 * #daily rotating, in logs subdirectoy
 * org.avaje.lib.log.AuxFileHandler.pattern=logs/mylog%d.log
 * 
 * #daily rotating, in java.io.temp directory
 * org.avaje.lib.log.AuxFileHandler.pattern=%t/mylog%d.log
 * 
 * #daily rotating, in CATALINA_HOME/logs
 * #where CATALINA_HOME is an environment variable
 * org.avaje.lib.log.AuxFileHandler.pattern=${CATALINA_HOME}/logs/mylog%d.log
 * </pre>
 * </code>
 * </p>
 */
public class FileHandler extends BaseWriterHandler implements HandlerConfigurable {
	
	int bufferSize = 1024;
	
	/**
	 * Whether to append or replace.
	 */
	boolean doAppend = true;

	/**
	 * The current file path.
	 */
	String currentPath;

	/**
	 * Set to true if using daily file switching.
	 */
	boolean useFileRotation;

	/**
	 * Name of the log file pre date (for rotation)
	 */
	String namePreDate;
	
	/**
	 * Name of the log file post date (for rotation)
	 */
	String namePostDate;
	
	LogTime logTime = new LogTime();
	
	/**
	 * In case you want to use this FileHandler default its
	 * file name to rotate.
	 * <p>
	 * Generally AvajeFileHandler or AuxFileHandler would be used.
	 * </p>
	 */
	public FileHandler() {
		this("java%d.log");
	}
	
	/**
	 * Create a FileHandler with a given default file name.
	 */
	public FileHandler(String defaultFileName) {
		super();
		config(defaultFileName);
	}

	/**
	 * Read the configuration including pattern, formatter, filter etc.
	 */
	protected synchronized void config(String defaultPattern) {
		
		try {
			HandlerConfig config  = new HandlerConfig(this);
			
			// get the raw pattern for the log file
			String pattern = config.getProperty(true, false, "pattern", defaultPattern);
			
			// replace %t and %h as these are used by JULI FileHandler
			pattern = StringHelper.replaceString(pattern, "%t", "${java.io.tmpdir}");
			pattern = StringHelper.replaceString(pattern, "%h", "${user.home}");
			pattern = StringHelper.replaceString(pattern, "${date}", "%d");

			// eval expressions such as ${user.home} or ${CATALINA_HOME}
			pattern = config.eval(pattern);

			// break up pattern into pre and post %d parts
			int datePos = pattern.indexOf("%d");
			if (datePos > -1){
				// daily file rotation
				namePreDate = pattern.substring(0, datePos);
				namePostDate = pattern.substring(datePos+2, pattern.length());
				useFileRotation = true;
				
			} else {
				// no file rotation
				namePreDate = pattern;
				namePostDate = "";
				useFileRotation = false;
			}
			

			try {
				String bufSize = config.getProperty("buffersize", "1024");
				bufferSize = Integer.parseInt(bufSize);
			} catch (NumberFormatException e){
				// ignore this
			}
			
			config.setLevel(Level.ALL);
			config.setFormatter(new DefaultFileFormatter());
			config.setFilter(null);			
			config.setEncoding();

			switchFile(logTime);

		} catch (Exception ex) {
			String msg = "error reading config";
			reportError(msg, ex, ErrorManager.GENERIC_FAILURE);
		}
	}
	
	
	/**
	 * Publish the record to the file.
	 * <p>
	 * This will rotate the log files if required.
	 * </p>
	 */
	public synchronized void publish(LogRecord record) {

		if (!isLoggable(record)) {
			return;
		}

		// check to see if we need to switch file?
		if (useFileRotation){
			
			if (logTime.isNextDay()) {
				logTime = new LogTime();
				try {
					switchFile(logTime);
				} catch (Exception ex) {
					// This is a pretty serious error... 
					String msg = "error switching file";
					reportError(msg, ex, ErrorManager.WRITE_FAILURE);
				}
			}
		}

		try {
			String msg = getFormatter().format(record);
			publishMessage(msg);

		} catch (Exception ex) {
			reportError(null, ex, ErrorManager.FORMAT_FAILURE);
			return;
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

		String newFilePath;// = logFilePath + File.separator + logFileName;

		if (useFileRotation) {
			// For file switching include the date in the file name
			//newFilePath = newFilePath + logTime.getYMD() + logFileSuffix;
			newFilePath = namePreDate + logTime.getYMD() + namePostDate;

		} else {
			//newFilePath = newFilePath + logFileSuffix;
			newFilePath = namePreDate;
		}

		// Try to open an output stream to the file
		if (!newFilePath.equals(currentPath)) {
			currentPath = newFilePath;

			FileOutputStream fos = new FileOutputStream(newFilePath, doAppend);
			OutputStream out = new BufferedOutputStream(fos, bufferSize);

			setOutputStream(out);
		}
	}

}
