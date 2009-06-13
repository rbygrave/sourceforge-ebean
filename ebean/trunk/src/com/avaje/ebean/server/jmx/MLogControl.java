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
package com.avaje.ebean.server.jmx;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.control.ServerControl;

/**
 * Implementation of the LogControl.
 * <p>
 * This is accessible via {@link EbeanServer#getServerControl()} and
 * {@link ServerControl#getLogControl()} or via JMX MBean.
 * </p>
 */
public class MLogControl implements MLogControlMBean, LogControl {

	int queryByIdLevel;

	int queryManyLevel;
	
	int sqlQueryLevel;
	
	int insertLevel;

	int updateLevel;

	int deleteLevel;
	
	int ormUpdateLevel;

	int sqlUpdateLevel;

	int callableSqlLevel;

	boolean debugSql;

	boolean debugLazyLoad;
	
	/**
	 * Configure from plugin properties.
	 */
	public MLogControl(ServerConfig serverConfig) {

		debugSql = serverConfig.isDebugSql();
		debugLazyLoad = serverConfig.isDebugLazyLoad();
		
		queryByIdLevel = serverConfig.getFindIdLogLevel().ordinal();
		queryManyLevel = serverConfig.getFindManyLogLevel().ordinal();
		sqlQueryLevel =  GlobalProperties.getInt("log.nativesql", 0);
			
		int iudLevel = serverConfig.getInsertUpdateDeleteLogLevel().ordinal();

		insertLevel = GlobalProperties.getInt("log.insert", iudLevel);
		updateLevel = GlobalProperties.getInt("log.update", iudLevel);
		deleteLevel = GlobalProperties.getInt("log.delete", iudLevel);
		ormUpdateLevel = GlobalProperties.getInt("log.ormupdate", iudLevel);

		sqlUpdateLevel = GlobalProperties.getInt("log.updatablesql", iudLevel);
		callableSqlLevel = GlobalProperties.getInt("log.callablesql", iudLevel);

	}

	public int getSqlQueryLevel() {
		return sqlQueryLevel;
	}

	public void setSqlQueryLevel(int sqlQueryLevel) {
		this.sqlQueryLevel = sqlQueryLevel;
	}

	public int getQueryByIdLevel() {
		return queryByIdLevel;
	}

	public int getQueryManyLevel() {
		return queryManyLevel;
	}

	public int getOrmUpdateLevel() {
		return ormUpdateLevel;
	}

	public void setOrmUpdateLevel(int ormUpdateLevel) {
		this.ormUpdateLevel = ormUpdateLevel;
	}

	public int getDeleteLevel() {
		return deleteLevel;
	}

	public void setDeleteLevel(int deleteLevel) {
		this.deleteLevel = deleteLevel;
	}

	public int getInsertLevel() {
		return insertLevel;
	}

	public void setInsertLevel(int insertLevel) {
		this.insertLevel = insertLevel;
	}

	public int getUpdateLevel() {
		return updateLevel;
	}

	public void setUpdateLevel(int updateLevel) {
		this.updateLevel = updateLevel;
	}

	public int getCallableSqlLevel() {
		return callableSqlLevel;
	}

	public void setCallableSqlLevel(int callableSqlLevel) {
		this.callableSqlLevel = callableSqlLevel;
	}

	public int getSqlUpdateLevel() {
		return sqlUpdateLevel;
	}

	public void setSqlUpdateLevel(int sqlUpdateLevel) {
		this.sqlUpdateLevel = sqlUpdateLevel;
	}

	public void setQueryByIdLevel(int queryByIdLevel) {
		this.queryByIdLevel = queryByIdLevel;
	}

	public void setQueryManyLevel(int queryManyLevel) {
		this.queryManyLevel = queryManyLevel;
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
