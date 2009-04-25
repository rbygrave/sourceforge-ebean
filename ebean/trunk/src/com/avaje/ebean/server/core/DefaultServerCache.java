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
import com.avaje.ebean.server.lib.cache.CacheManager;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.server.transaction.TableState;
import com.avaje.ebean.server.transaction.TableStateManager;
import com.avaje.ebean.server.util.TableValidator;

/**
 * Default ServerCache implementation. Caching beans and lookups.
 */
public class DefaultServerCache implements ServerCache {

    /**
     * Aware of 'transactional' tableState.
     */
    TableStateManager tableStateManager;
    
    InternalEbeanServer server;
    
    /**
     * Used to name lookup caches for this plugin. 
     */
    final String lookupCacheName;
    
    /**
     * Used to name bean caches for this plugin.
     */
    final String keyPrefix;

    /**
     * Create the ServerCache.
     */
    public DefaultServerCache(Plugin plugin, InternalEbeanServer server) {
    	PluginProperties properties = plugin.getProperties();
        this.keyPrefix = "ebean:"+properties.getServerName()+":";
        this.lookupCacheName = keyPrefix+"lookup";

    	this.server = server;
        this.tableStateManager = plugin.getTransactionManager().getTableStateManager();
    }
    
    /**
     * Configure this cache.
     */
    public void setOwner(InternalEbeanServer server){
        
    }

    /**
     * Return the tableState for a given table name.
     */
    public TableState getTableState(String tableName) {
        return tableStateManager.getTableState(tableName);
    }

    /**
     * Returns true if the table has been modified after since.
     * 
     * @param tableName the name of the table
     * @param since the time after which the table is modified
     * @param includeInsert if false includes updates and deletes only
     */
    public boolean isTableModified(String tableName, long since, boolean includeInsert) {
        TableState tableState = getTableState(tableName);
        return tableState.isModified(since, includeInsert);
    }
    

    
    /**
     * Put a bean into the cache.
     */
    public void putBean(BeanDescriptor<?> desc, Object uid, Object bean) {
        Cache cache = getBeanCache(desc);
        cache.put(uid, bean);    
    }
    
    /**
     * Add all the entries from the map into the cache.
     */
    public void putBeanAll(BeanDescriptor<?> desc, Map<?,?> beanMap) {
        Cache cache = getBeanCache(desc);
        cache.putAll(beanMap);    
    }
    

    /**
     * Get a bean out of the cache.
     */
    public Object getBean(BeanDescriptor<?> desc, Object uid) {

        Cache cache = getBeanCache(desc);
        return cache.get(uid);
    }

    
    /**
     * Create the cache for a given type of bean.
     */
    public Cache getBeanCache(BeanDescriptor<?> desc) {

        
        String key = keyPrefix;

        Class<?> beanType = desc.getBeanType();
        if (beanType == null) {
            key += "Table:" + desc.getBaseTable();
        } else {
            key += beanType.getName();
        }

        Cache cache = CacheManager.get(key);
        if (!cache.hasValidator()) {
            // invalidate on dependent tables
            String[] depTables = desc.getDependantTables();
            
            TableValidator updDel = new TableValidator(depTables, this);
            updDel.setInvalidatesCache();
            updDel.setDependantOnInserts(false);

            TableValidator insUpdDel = new TableValidator(depTables, this);
            insUpdDel.setInvalidatesCache();

            cache.setValidator(updDel, insUpdDel);

            initTableState(depTables);
        }
        return cache;
    }

    /**
     * Make sure the tableState is initialised for the dependant tables used by
     * the lookups and bean caches.
     */
    private void initTableState(String[] tables) {
        if (tables == null) {
            return;
        }
        for (int i = 0; i < tables.length; i++) {
            getTableState(tables[i]);
        }
    }
}
