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
package com.avaje.ebean.server.transaction;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.avaje.ebean.AdminLogging.TxLogSharing;
import com.avaje.ebean.AdminLogging.TxLogLevel;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.internal.ServerTransaction;
import com.avaje.ebean.server.transaction.log.DefaultTransactionLogger;
import com.avaje.ebean.server.transaction.log.LogTime;
import com.avaje.ebean.server.transaction.log.TransactionLogger;

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

	private final Map<String, TransactionLogger> loggerMap = new ConcurrentHashMap<String, TransactionLogger>(200);

	private final String sharedLogFileName;

	private final String baseDir;
	
	private final String serverName;

	private TxLogLevel logLevel;

	private TxLogSharing logSharing;

	private final TransactionLogger sharedLogger;


	/**
	 * Create the TransactionLogger.
	 * <p>
	 * DevNote: This registers a shutdown hook to flush and close the
	 * sharedLogger. Alternate option would be to flush() the log after each
	 * write to the log.
	 * </p>
	 */
	public TransactionLogManager(ServerConfig serverConfig) {

		this.serverName = serverConfig.getName();
		this.sharedLogFileName = GlobalProperties.get("log.filename", "trans");
		
		this.logLevel = serverConfig.getTransactionLogging();
		this.logSharing = serverConfig.getTransactionLogSharing();
		
		String dir = serverConfig.getTransactionLogDirectoryWithEval();
		if (dir == null) {
			dir = createDefaultLogsDirectory();
		}
		this.baseDir = dir;

		if (logLevel == TxLogLevel.NONE){
			String m = "Transaction logging is OFF  ... ebean.log.level=0";
			logger.info(m);
		} else {
			String m = "Transaction logs in: "+baseDir;
			logger.info(m);			
		}
		
		this.sharedLogger = createSharedLogger();
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
	public void setLogLevel(TxLogLevel logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Return the log level.
	 */
	public TxLogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * Return the log sharing mode.
	 */
	public TxLogSharing getLogSharing() {
		return logSharing;
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
	public void setLogSharing(TxLogSharing logSharing) {
		this.logSharing = logSharing;
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
		case NONE:
			break;

		case ALL:
			logInfo(t, msg, error);
			break;

		case EXPLICIT:
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
		
		if (logSharing == TxLogSharing.ALL) {
			return sharedLogger;
		}
		if (logSharing == TxLogSharing.EXPLICIT && !t.isExplicit()) {
			return sharedLogger;
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

		String logFileName = serverName+ "_"+ t.getId() + "_" 
			+ logTime.getYMD() + "_" + logTime.getNow(logSep);

		return new DefaultTransactionLogger(baseDir, logFileName, false);
	}

	/**
	 * Return the shared logger.
	 */
	private TransactionLogger createSharedLogger() {

		String logFileName = serverName+"_" + sharedLogFileName;
		return new DefaultTransactionLogger(baseDir, logFileName, true);
	}

}
