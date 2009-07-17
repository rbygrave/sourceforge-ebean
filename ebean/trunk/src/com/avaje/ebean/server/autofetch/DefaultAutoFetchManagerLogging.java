package com.avaje.ebean.server.autofetch;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.AutofetchConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.server.lib.BackgroundThread;
import com.avaje.ebean.server.querydefn.OrmQueryDetail;
import com.avaje.ebean.server.transaction.log.DefaultTransactionLogger;

/**
 * Handles the logging aspects for the DefaultAutoFetchListener.
 * <p>
 * Note that java util logging loggers generally should not be serialised and
 * that is one of the main reasons for pulling out the logging to this class.
 * </p>
 */
public class DefaultAutoFetchManagerLogging {

	private static final Logger logger = Logger.getLogger(DefaultAutoFetchManagerLogging.class.getName());

	final DefaultTransactionLogger fileLogger;

	final DefaultAutoFetchManager manager;

	final boolean useFileLogger;

	public DefaultAutoFetchManagerLogging(ServerConfig serverConfig, DefaultAutoFetchManager profileListener) {

		this.manager = profileListener;

		AutofetchConfig autofetchConfig = serverConfig.getAutofetchConfig();

		useFileLogger = autofetchConfig.isUseFileLogging();

		if (!useFileLogger) {
			fileLogger = null;

		} else {
			// a separate log file just like the transaction logging
			// for putting the profiling log messages. The benefit is that
			// this doesn't pollute the main log with heaps of messages.
			String baseDir = serverConfig.getTransactionLogDirectoryWithEval();
			fileLogger = new DefaultTransactionLogger(baseDir, "autofetch", true, "csv");
		}

		int updateFreqInSecs = autofetchConfig.getProfileUpdateFrequency();

		BackgroundThread.add(updateFreqInSecs, new UpdateProfile());
	}

	private final class UpdateProfile implements Runnable {
		public void run() {
			manager.updateTunedQueryInfo();
		}
	}

	public void logError(Level level, String msg, Throwable e) {
		if (useFileLogger) {
			String errMsg = e == null ? "" : e.getMessage();
			fileLogger.log("\"Error\",\"" + msg+" "+errMsg+"\",,,,");
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
