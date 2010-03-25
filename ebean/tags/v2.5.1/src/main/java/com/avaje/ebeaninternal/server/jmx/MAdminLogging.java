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
package com.avaje.ebeaninternal.server.jmx;

import com.avaje.ebean.AdminLogging;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.transaction.TransactionManager;

/**
 * Implementation of the LogControl.
 * <p>
 * This is accessible via {@link EbeanServer#getAdminLogging()} or via JMX MBean.
 * </p>
 */
public class MAdminLogging implements MAdminLoggingMBean, AdminLogging {

	public static final int NONE = LogLevelStmt.NONE.ordinal();
	public static final int SQL = LogLevelStmt.SQL.ordinal();
	public static final int BIND = LogLevelStmt.BINDING.ordinal();
	public static final int SUMMARY = LogLevelStmt.SUMMARY.ordinal();

	private final TransactionManager transactionManager;

	LogLevelStmt queryLevel;
	int ordinalQuery;
	
	LogLevelStmt sqlQueryLevel;
	int ordinalSqlQuery;
	
	LogLevelStmt iudLevel;
	int ordinalIud;
	
	boolean debugSql;

	boolean debugLazyLoad;
	
	
	/**
	 * Configure from plugin properties.
	 */
	public MAdminLogging(ServerConfig serverConfig, TransactionManager txManager) {

		this.transactionManager = txManager;
		
		debugSql = serverConfig.isDebugSql();
		debugLazyLoad = serverConfig.isDebugLazyLoad();
		
		sqlQueryLevel = serverConfig.getLoggingLevelSqlQuery();
		ordinalSqlQuery = sqlQueryLevel == null ? 0 : sqlQueryLevel.ordinal();
		
		queryLevel = serverConfig.getLoggingLevelQuery();
		ordinalQuery = queryLevel == null ? 0 : queryLevel.ordinal();
		
		iudLevel = serverConfig.getLoggingLevelIud();
		ordinalIud = iudLevel == null ? 0 : iudLevel.ordinal();
	}

	public void setLoggingLevel(LogLevel txLogLevel){
		transactionManager.setTransactionLogLevel(txLogLevel);
	}
	
	public LogLevel getLoggingLevel() {
		return transactionManager.getTransactionLogLevel();
	}
	
	public void setLogFileSharing(LogFileSharing txLogSharing){
		transactionManager.setTransactionLogSharing(txLogSharing);
	}
	
	public LogFileSharing getLogFileSharing() {
		return transactionManager.getTransactionLogSharing();
	}

	public boolean isLogQuery(int level){
		return ordinalQuery >= level;
	}
	
	public boolean isLogSqlQuery(int level){
		return ordinalSqlQuery >= level;
	}
	public boolean isLogIud(int level){
		return ordinalIud >= level;
	}
	
	
	public boolean isLogBind(LogLevelStmt l){
		return l.ordinal() >= LogLevelStmt.BINDING.ordinal();
	}

	public boolean isLogSummary(LogLevelStmt l){
		return l.ordinal() >= LogLevelStmt.SUMMARY.ordinal();
	}

	public LogLevelStmt getLoggingLevelQuery() {
		return queryLevel;
	}

	public void setLoggingLevelQuery(LogLevelStmt queryLevel) {
		this.queryLevel = queryLevel;
		this.ordinalQuery = queryLevel == null ? 0 : queryLevel.ordinal();

	}

	public LogLevelStmt getLoggingLevelSqlQuery() {
		return sqlQueryLevel;
	}

	public void setLoggingLevelSqlQuery(LogLevelStmt sqlQueryLevel) {
		this.sqlQueryLevel = sqlQueryLevel;
		this.ordinalSqlQuery = sqlQueryLevel == null ? 0 : sqlQueryLevel.ordinal();
	}

	public int getIudOrdinal(){
		return ordinalIud;
	}
	
	public LogLevelStmt getLoggingLevelIud() {
		return iudLevel;
	}

	public void setLoggingLevelIud(LogLevelStmt iudLevel) {
		this.iudLevel = iudLevel;
		this.ordinalIud = iudLevel == null ? 0 : iudLevel.ordinal();
	}

	public boolean isDebugGeneratedSql() {
		return debugSql;
	}

	public void setDebugGeneratedSql(boolean debugSql) {
		this.debugSql = debugSql;
	}

	public boolean isDebugLazyLoad() {
		return debugLazyLoad;
	}

	public void setDebugLazyLoad(boolean debugLazyLoad) {
		this.debugLazyLoad = debugLazyLoad;
	}

}
