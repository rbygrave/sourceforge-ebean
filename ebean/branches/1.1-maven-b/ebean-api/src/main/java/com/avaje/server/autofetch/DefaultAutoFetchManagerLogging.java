package com.avaje.ebean.server.autofetch;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.query.OrmQueryDetail;
import com.avaje.ebean.server.lib.BackgroundThread;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.server.transaction.log.DefaultTransactionLogger;

/**
 * Handles the logging aspects for the DefaultAutoFetchListener.
 * <p>
 * Note that java util logging loggers generally should not be serialised and
 * that is one of the main reasons for pulling out the logging to this class.
 * </p>
 */
public class DefaultAutoFetchManagerLogging {

	private static final Logger logger = Logger.getLogger(DefaultAutoFetchManagerLogging.class
			.getName());

	final DefaultTransactionLogger fileLogger;

	final PluginDbConfig dbConfig;

	final DefaultAutoFetchManager manager;

	final boolean useFileLogger;

	public DefaultAutoFetchManagerLogging(Plugin plugin, DefaultAutoFetchManager profileListener) {

		this.dbConfig = plugin.getDbConfig();
		this.manager = profileListener;

		PluginProperties props = plugin.getProperties();

		useFileLogger = props.getPropertyBoolean("autofetch.usefilelogging", true);

		if (!useFileLogger) {
			fileLogger = null;

		} else {
			// a separate log file just like the transaction logging
			// for putting the profiling log messages. The benefit is that
			// this doesn't pollute the main log with heaps of messages.
			String dftlBaseDir = props.getProperty("transactionlogging.directory", null);
			String baseDir = props.getProperty("log.directory", dftlBaseDir);
			fileLogger = new DefaultTransactionLogger(baseDir, "autofetch", true, "csv");
		}

		int updateFreqInSecs = props.getPropertyInt("autofetch.profiling.updatefrequency", 60);

		BackgroundThread.add(updateFreqInSecs, new UpdateProfile());
	}

	private final class UpdateProfile implements Runnable {
		public void run() {
			manager.updateTunedQueryInfo();
		}
	}

	public void logError(Level level, String msg, Throwable e) {
		if (useFileLogger) {
			fileLogger.log("\"Error\",\"" + msg+" "+e.getMessage()+"\",,,,");
		}
		logger.log(level, msg, e);
	}

	public void logToJavaLogger(String msg) {
		logger.info(msg);
	}

	public void logSummary(String summaryInfo) {
		
		String msg = "\"Summary\",\""+summaryInfo+"\",,,,";
		
		if (useFileLogger) {
			fileLogger.log(msg);
		}
		logger.fine(msg);
	}

	public void logChanged(TunedQueryInfo tunedFetch, OrmQueryDetail newQueryDetail) {
		
		String msg = tunedFetch.getLogOutput(newQueryDetail);
		
		if (useFileLogger) {
			fileLogger.log(msg);
		} else {
			logger.fine(msg);
		}
	}

	public void logNew(TunedQueryInfo tunedFetch) {

		String msg = tunedFetch.getLogOutput(null);

		if (useFileLogger) {
			fileLogger.log(msg);
		} else {
			logger.fine(msg);
		}
	}

}
