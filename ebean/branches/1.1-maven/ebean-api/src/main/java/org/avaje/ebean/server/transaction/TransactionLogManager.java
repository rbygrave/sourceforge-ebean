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
package org.avaje.ebean.server.transaction;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.avaje.ebean.server.core.ServerTransaction;
import org.avaje.ebean.server.plugin.PluginProperties;
import org.avaje.ebean.server.transaction.log.DefaultTransactionLogger;
import org.avaje.ebean.server.transaction.log.LogTime;
import org.avaje.ebean.server.transaction.log.TransactionLogger;

/**
 * Manages the transaction logs.
 */
public class TransactionLogManager {

	static final Logger logger = Logger.getLogger(TransactionLogManager.class.getName());
	
	/**
	 * Turn off logging with this logLevel.
	 */
	public static final int LOG_NONE = 0;

	/**
	 * Log only explicitly created transactions with this logLevel.
	 */
	public static final int LOG_EXPLICIT = 1;

	/**
	 * Log all transactions with this logLevel.
	 */
	public static final int LOG_ALL = 2;

	/**
	 * Every transaction has its own separate log.
	 */
	public static final int NONE_SHARE_LOGGER = 0;

	/**
	 * All implicit transactions share a single log file.
	 */
	public static final int IMPLICIT_SHARE_LOGGER = 1;

	/**
	 * Every transaction shares the same log file.
	 */
	public static final int ALL_SHARE_LOGGER = 2;

	private final String[] logSep = { "", "_" };

	Map<String, TransactionLogger> loggerMap;

	String sharedLogFileName;

	String baseDir;

	int logLevel;

	int logSharing;

	TransactionLogger sharedLogger;

	PluginProperties props;

	/**
	 * Create the TransactionLogger.
	 * <p>
	 * DevNote: This registers a shutdown hook to flush and close the
	 * sharedLogger. Alternate option would be to flush() the log after each
	 * write to the log.
	 * </p>
	 */
	public TransactionLogManager(PluginProperties props) {
		this.props = props;

		sharedLogFileName = props.getProperty("log.filename", "trans");
		int dfltLogLevel = props.getPropertyInt("transactionlogging.level", LOG_ALL);
		logLevel = props.getPropertyInt("log.level", dfltLogLevel);

		int dfltSharing = props.getPropertyInt("transactionlogging.share", IMPLICIT_SHARE_LOGGER);
		logSharing = props.getPropertyInt("log.share", dfltSharing);

		String dftlBaseDir = props.getProperty("transactionlogging.directory", null);
		baseDir = props.getProperty("log.directory", dftlBaseDir);
		if (baseDir == null) {
			baseDir = createDefaultLogsDirectory();
		}

		if (logLevel == 0){
			String m = "Transaction logging is OFF  ... ebean.log.level=0";
			logger.info(m);
		} else {
			String m = "Transaction logs in: "+baseDir;
			logger.info(m);			
		}
		
		loggerMap = new ConcurrentHashMap<String, TransactionLogger>(200);
	}

	private String createDefaultLogsDirectory() {
		String dftlDir = "logs/trans";
		File f = new File(dftlDir);
		if (f.mkdirs()) {
			return dftlDir;
		}
		return "logs";
	}

	/**
	 * Set the logging level.
	 */
	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Set the log sharing to one of ALL_SHARE_LOGGER, NONE_SHARE_LOGGER or
	 * IMPLICIT_SHARE_LOGGER.
	 * <p>
	 * The default is IMPLICIT_SHARE_LOGGER where explicit transactions each
	 * have their own separate logs and implicit transactions all share a common
	 * log.
	 * </p>
	 */
	public void setLogSharing(int logSharing) {
		this.logSharing = logSharing;
	}

	/**
	 * Set the base directory where transaction logs are placed.
	 */
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * If this transaction has its own Logger close it.
	 */
	public void transactionEnded(ServerTransaction t, String msg) {
		TransactionLogger logger = removeLogger(t);
		if (logger != null) {
			if (msg != null) {
				msg = "Trans[" + t.getId() + "] " + msg;
				logger.log(t.getId(), msg, null);
			}
			logger.close();
		}
	}

	public void log(ServerTransaction t, String msg, Throwable error) {
		switch (logLevel) {
		case LOG_NONE:
			break;

		case LOG_ALL:
			logInfo(t, msg, error);
			break;

		case LOG_EXPLICIT:
			if (t.isExplicit()) {
				logInfo(t, msg, error);
			}
			break;
		default:
			break;
		}
	}

	private void logInfo(ServerTransaction t, String msg, Throwable error) {

		getLogger(t).log(t.getId(), msg, error);
	}

	private TransactionLogger removeLogger(ServerTransaction t) {
		String id = t.getId();
		if (id != null){
			return loggerMap.remove(id);
		} else {
			return null;
		}
	}

	/**
	 * Get the Logger for a given transaction.
	 */
	private TransactionLogger getLogger(ServerTransaction t) {
		if (logSharing == ALL_SHARE_LOGGER) {
			return getSharedLogger();
		}
		if (logSharing == IMPLICIT_SHARE_LOGGER && !t.isExplicit()) {
			return getSharedLogger();
		}

		TransactionLogger logger = loggerMap.get(t.getId());
		if (logger == null) {
			logger = createLogger(t);
			loggerMap.put(t.getId(), logger);
		}
		return logger;
	}

	private TransactionLogger createLogger(ServerTransaction t) {

		LogTime logTime = LogTime.getWithCheck();

		String logFileName = props.getServerName() + "_";
		logFileName += t.getId() + "_" + logTime.getYMD() + "_" + logTime.getNow(logSep);

		return new DefaultTransactionLogger(baseDir, logFileName, false);
	}

	/**
	 * Return the shared logger.
	 */
	private TransactionLogger getSharedLogger() {

		if (sharedLogger == null) {
			String logFileName = "";
			if (props.getServerName() != null) {
				logFileName += props.getServerName() + "_";
			}
			logFileName += sharedLogFileName;
			sharedLogger = new DefaultTransactionLogger(baseDir, logFileName, true);
		}
		return sharedLogger;
	}

}
