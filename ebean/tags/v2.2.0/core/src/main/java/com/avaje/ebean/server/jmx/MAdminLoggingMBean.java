package com.avaje.ebean.server.jmx;

import com.avaje.ebean.AdminLogging.StmtLogLevel;

public interface MAdminLoggingMBean {

	/**
	 * The current log level for native sql queries.
	 */
	public StmtLogLevel getQueryLevel();

	/**
	 * Set the log level for native sql queries.
	 */
	public void setQueryLevel(StmtLogLevel sqlQueryLevel);

	/**
	 * The current log level for bean update.
	 */
	public StmtLogLevel getIudLevel();

	/**
	 * Set the log level for bean update.
	 */
	public void setIudLevel(StmtLogLevel updateLevel);

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