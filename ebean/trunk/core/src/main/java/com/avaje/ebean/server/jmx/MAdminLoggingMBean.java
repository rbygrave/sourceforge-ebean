package com.avaje.ebean.server.jmx;

import com.avaje.ebean.AdminLogging.LogLevelStmt;

public interface MAdminLoggingMBean {

	/**
	 * The current log level for native sql queries.
	 */
	public LogLevelStmt getLoggingLevelQuery();

	/**
	 * Set the log level for native sql queries.
	 */
	public void setLoggingLevelQuery(LogLevelStmt sqlQueryLevel);

	/**
	 * The current log level for bean update.
	 */
	public LogLevelStmt getLoggingLevelIud();

	/**
	 * Set the log level for bean update.
	 */
	public void setLoggingLevelIud(LogLevelStmt updateLevel);

	/**
	 * If true Log generated sql to the console.
	 */
	public boolean isDebugGeneratedSql();

	/**
	 * Set to true to Log generated sql to the console.
	 */
	public void setDebugGeneratedSql(boolean debugSql);

	/**
	 * Return true if lazy loading should be debugged.
	 */
	public boolean isDebugLazyLoad();

	/**
	 * Set the debugging on lazy loading.
	 */
	public void setDebugLazyLoad(boolean debugLazyLoad);

}