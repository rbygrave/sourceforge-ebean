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
package com.avaje.ebean.server.core;

import java.util.Map;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.lib.cache.Cache;
import com.avaje.ebean.server.transaction.TableState;

/**
 * The cache service for server side caching of beans and lookups.
 */
public interface ServerCache {

    public void setOwner(InternalEbeanServer server);

    /**
     * Return the tableState for a given table name.
     */
    public TableState getTableState(String tableName);

    /**
     * Returns true if the table has been modified after since.
     * 
     * @param tableName the name of the table
     * @param since the time after which the table is modified
     * @param includeInsert if false includes updates and deletes only
     */
    public boolean isTableModified(String tableName, long since, boolean includeInsert);

    /**
     * Add all the entries from the map into the cache.
     */
    public void putBeanAll(BeanDescriptor<?> desc, Map<?,?> beanMap);
    
    /**
     * Put a bean into the cache.
     */
    public void putBean(BeanDescriptor<?> desc, Object uid, Object bean);

    /**
     * Get a bean out of the cache. If the bean is not in the cache return null.
     */
    public Object getBean(BeanDescriptor<?> desc, Object uid);
    
    /**
     * Get a cache for the type of bean.
     */
    public Cache getBeanCache(BeanDescriptor<?> desc);


}
