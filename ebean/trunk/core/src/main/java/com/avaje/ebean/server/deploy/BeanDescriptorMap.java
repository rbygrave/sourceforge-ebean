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
package com.avaje.ebean.server.deploy;

import com.avaje.ebean.cache.ServerCacheManager;


/**
 * Provides a method to find a BeanDescriptor.
 * <p>
 * Used during deployment of to resolve relationships between beans.
 * </p>
 */
public interface BeanDescriptorMap {

	/**
	 * Return the name of the server/database.
	 */
	public String getServerName();

	/**
	 * Return the Cache Manager.
	 */
	public ServerCacheManager getCacheManager();
	
	/**
	 * Return the BeanDescriptor for a given class.
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> entityType);

    /**
     * Return the DB decrypt SQL for a given column with its table alias.
     */
    public String getDecryptSql(String columnWithTableAlias);
    
    public String getEncryptSql(String column);
   
    /**
     * Return the Encrypt key given the table and column name.
     */
    public String getEncryptKey(String tableName, String columnName);

}
