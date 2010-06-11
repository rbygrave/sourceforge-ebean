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
package com.avaje.ebeaninternal.server.transaction;

import java.io.File;
import java.util.logging.Logger;

import com.avaje.ebean.AdminLogging.LogLevel;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.transaction.log.FileTransactionLogger;
import com.avaje.ebeaninternal.server.transaction.log.JuliTransactionLogger;

/**
 * Manages the transaction logs.
 */
public class TransactionLogManager {

	private static final Logger logger = Logger.getLogger(TransactionLogManager.class.getName());

	private final String serverName;

	private LogLevel logLevel;

	private final TransactionLogWriter logWriter;

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
		this.logLevel = serverConfig.getLoggingLevel();
		
		boolean logToJavaLogger = serverConfig.isLoggingToJavaLogger();
		if (logToJavaLogger){
		    logWriter = new JuliTransactionLogger();
		    
		} else {
		
    		String dir = serverConfig.getLoggingDirectoryWithEval();
    		if (dir == null) {
    			dir = createDefaultLogsDirectory();
    		}
    		String m = "Transaction logs in: "+dir;
            logger.info(m);       
            
            String middleName = GlobalProperties.get("ebean.logging.filename", "_txn_");
            int maxFileSize = GlobalProperties.getInt("ebean.logging.maxFileSize", 100*1024*1024);
            
            String logPrefix = serverName + middleName;
            String threadName = "Ebean-"+serverName+"-TxnLogWriter";
            this.logWriter = new FileTransactionLogger(threadName, dir, logPrefix, maxFileSize);
		}
		
        if (logLevel == LogLevel.NONE){
            String m = "Transaction logging is OFF  ... ebean.log.level=0";
            logger.info(m);
        }
        
        logWriter.start();
	}

	public void shutdown() {
	    logWriter.shutdown();
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
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Return the log level.
	 */
	public LogLevel getLogLevel() {
		return logLevel;
	}
	
    public void log(TransactionLogBuffer logBuffer) {
        logWriter.log(logBuffer);
    }
    
}
