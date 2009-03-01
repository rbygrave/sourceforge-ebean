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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.sql.DictionaryInfo;
import com.avaje.ebean.server.lib.sql.TableInfo;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.lib.log.LogFactory;

/**
 * Creates and caches MapBeanDescriptor.
 * <p>
 * Provides a mechanisim to create and cache the MapBeanDescriptor's (one per
 * table).
 * </p>
 */
public class MapBeanDescriptorCacheFactory {

	private static final Logger logger = LogFactory.get(MapBeanDescriptorCacheFactory.class);

	/**
	 * Creates MapBeanDescriptor's.
	 */
	private final MapBeanDescriptorFactory factory;

	private final MapBeanManagerFactory managerFactory;
	
	/**
	 * Cache of MapBeanDescriptor using Concurrent get, single threaded
	 * get/create/put.
	 */
	private final HashMap<String, MapBeanDescriptor> map = new HashMap<String, MapBeanDescriptor>();

	private final HashMap<String, BeanManager> managerMap = new HashMap<String, BeanManager>();

	private final PluginDbConfig dbConfig;

	private final DeploymentManager deploymentManager;

	public MapBeanDescriptorCacheFactory(DeploymentManager deploymentManager,
			PluginDbConfig dbConfig) {
		
		this.deploymentManager = deploymentManager;
		this.dbConfig = dbConfig;

		factory = new MapBeanDescriptorFactory(deploymentManager, dbConfig);
		managerFactory = new MapBeanManagerFactory(dbConfig);
	}

	public void initialiseAll() {

		// get all the base tables from the deployed entity beans...
		Iterator<BeanDescriptor> it = deploymentManager.descriptors();
		while (it.hasNext()) {
			BeanDescriptor desc = it.next();
			if (!desc.isEmbedded() && desc.isMeta()){
				String baseTable = desc.getBaseTable();
				if (baseTable != null){
					registerTable(baseTable);
				}
			}
		}

		DictionaryInfo dict = dbConfig.getDictionaryInfo();
		PluginProperties properties = dbConfig.getProperties();
		for (int i = 0; i < 10; i++) {
			String propKey = "register.tables." + i;
			String pattern = properties.getProperty(propKey, null);
			if (pattern != null) {
				String[] catSchemaTable = pattern.split(",");
				if (catSchemaTable.length != 3) {
					String msg = "Length [" + catSchemaTable.length
							+ "] != 3 error reading catalog,schema,table patterns from property "
							+ propKey;
					logger.log(Level.SEVERE, msg);

				} else {
					String catalog = catSchemaTable[0];
					String schema = catSchemaTable[1];
					String table = catSchemaTable[2];
					// register any other tables of interest that where 
					// not used by the ORM mapping of entity beans
					dict.registerTables(catalog, schema, table, true);
				}
			}
		}

		// make sure we have all the tables of interest
		List<TableInfo> tableInfoList = dict.getTableInfoList();
		for (int i = 0; i < tableInfoList.size(); i++) {
			TableInfo tableInfo = tableInfoList.get(i);
			registerTable(tableInfo.getName());
		}
	}

	/**
	 * Return an Iterator of the table descriptors.
	 */
	public Iterator<MapBeanDescriptor> tableDescriptors() {
		return map.values().iterator();
	}
	
	/**
	 * Return the MapBeanDescriptor for this table.
	 */
	public MapBeanDescriptor get(String tableName) {
		String key = tableName.toLowerCase();
		return map.get(key);
	}

	/**
	 * Return the BeanManager for this table.
	 */
	public BeanManager getManager(String tableName) {
		String key = tableName.toLowerCase();
		return managerMap.get(key);
	}

	/**
	 * Register this table (we are interested in).
	 */
	private void registerTable(String tableName) {

		String key = tableName.toLowerCase();
		if (!map.containsKey(key)) {
			MapBeanDescriptor desc = factory.createBeanDescriptor(tableName);
			if (desc == null){
				// null for beans based on raw sql query (no base table found).
				
			} else {
				map.put(key, desc);
				BeanManager manager = managerFactory.create(desc);
				managerMap.put(key, manager);
			}
		}
	}

}
