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
package com.avaje.ebean.query;

import java.io.Serializable;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.bean.BindParams;
import com.avaje.ebean.server.deploy.DeployNamedUpdate;

/**
 * Default implementation of OrmUpdate.
 */
public final class DefaultOrmUpdate<T> implements OrmUpdate<T>, Serializable {

	private static final long serialVersionUID = -8791423602246515438L;


	private transient final EbeanServer server;
	
	private final Class<?> beanType;
	
	/**
	 * The name of the update.
	 */
	private final String name;

	/**
	 * The parameters used to bind to the sql.
	 */
	private final BindParams bindParams = new BindParams();

	/**
	 * The sql update or delete statement.
	 */
	private String updateStatement;

	/**
	 * Automatically detect the table being modified by this sql. This will
	 * register this information so that eBean invalidates cached objects if
	 * required.
	 */
	private boolean notifyCache = true;

	private int timeout;
	
	private String generatedSql;
	
	private final String baseTable;
	
	private OrmUpdateType type;
	
	/**
	 * Create with a specific server. This means you can use the
	 * UpdateSql.execute() method.
	 */
	public DefaultOrmUpdate(Class<?> beanType, EbeanServer server, String baseTable) {
		this.beanType = beanType;
		this.server = server;
		this.baseTable = baseTable;
		this.name =  "";
	}
	
	public DefaultOrmUpdate(Class<?> beanType, EbeanServer server, String baseTable, DeployNamedUpdate namedUpdate) {
		
		this.beanType = beanType;
		this.server = server;
		this.baseTable = baseTable;
		this.name =  namedUpdate.getName();
		this.notifyCache = namedUpdate.isNotifyCache();
		
		// named updates are always converted to sql as part
		// of the initialisation
		setUpdate(namedUpdate.getSqlUpdateStatement());
	}
	
	public DefaultOrmUpdate<T> setUpdate(String updateStatement){
		this.updateStatement = updateStatement;
		this.type = deriveType(updateStatement);
		return this;
	}
	
	public DefaultOrmUpdate<T> setTimeout(int secs){
		this.timeout = secs;
		return this;
	}

	public Class<?> getBeanType() {
		return beanType;
	}

	/**
	 * Return the timeout in seconds.
	 */
	public int getTimeout() {
		return timeout;
	}

	private OrmUpdate.OrmUpdateType deriveType(String updateStatement) {
		
		updateStatement = updateStatement.trim();
		int spacepos = updateStatement.indexOf(' ');
		if (spacepos == -1){
			return OrmUpdate.OrmUpdateType.UNKNOWN;
		
		} else {
			String firstWord = updateStatement.substring(0, spacepos);
			if (firstWord.equalsIgnoreCase("update")){
				return OrmUpdate.OrmUpdateType.UPDATE;
				
			} else if (firstWord.equalsIgnoreCase("insert")) {
				return OrmUpdate.OrmUpdateType.INSERT;
				
			} else if (firstWord.equalsIgnoreCase("delete")) {
				return OrmUpdate.OrmUpdateType.DELETE;
			} else {
				return OrmUpdate.OrmUpdateType.UNKNOWN;
			}
		}
	}
	
	public int execute() {
		return server.execute(this);
	}

	/**
	 * Set this to false if you don't want eBean to automatically deduce the
	 * table modification information and process it.
	 * <p>
	 * Set this to false if you don't want any cache invalidation or text index
	 * management to occur. You may do this when say you update only one column
	 * and you know that it is not important for cached objects or text indexes.
	 * </p>
	 */
	public DefaultOrmUpdate<T> setNotifyCache(boolean notifyCache) {
		this.notifyCache = notifyCache;
		return this;
	}

	/**
	 * Return true if the cache should be notified so that invalidates
	 * appropriate objects.
	 */
	public boolean isNotifyCache() {
		return notifyCache;
	}

	public String getName() {
		return name;
	}

	public String getUpdateStatement() {
		return updateStatement;
	}

	public DefaultOrmUpdate<T> setQuery(String updateStatement) {
		this.updateStatement = updateStatement;
		return this;
	}

	public DefaultOrmUpdate<T> set(int position, Object value) {
		bindParams.setParameter(position, value);
		return this;
	}

	public DefaultOrmUpdate<T> setParameter(int position, Object value) {
		bindParams.setParameter(position, value);
		return this;
	}

	public DefaultOrmUpdate<T> setNull(int position, int jdbcType) {
		bindParams.setNullParameter(position, jdbcType);
		return this;
	}

	public DefaultOrmUpdate<T> setNullParameter(int position, int jdbcType) {
		bindParams.setNullParameter(position, jdbcType);
		return this;
	}

	public DefaultOrmUpdate<T> set(String name, Object value) {
		bindParams.setParameter(name, value);
		return this;
	}

	public DefaultOrmUpdate<T> setParameter(String name, Object param) {
		bindParams.setParameter(name, param);
		return this;
	}

	public DefaultOrmUpdate<T> setNull(String name, int jdbcType) {
		bindParams.setNullParameter(name, jdbcType);
		return this;
	}

	public DefaultOrmUpdate<T> setNullParameter(String name, int jdbcType) {
		bindParams.setNullParameter(name, jdbcType);
		return this;
	}

	/**
	 * Return the bind parameters.
	 */
	public BindParams getBindParams() {
		return bindParams;
	}

	public String getGeneratedSql() {
		return generatedSql;
	}

	public void setGeneratedSql(String generatedSql) {
		this.generatedSql = generatedSql;
	}

	public String getBaseTable() {
		return baseTable;
	}

	public OrmUpdateType getOrmUpdateType() {
		return type;
	}
	
}
