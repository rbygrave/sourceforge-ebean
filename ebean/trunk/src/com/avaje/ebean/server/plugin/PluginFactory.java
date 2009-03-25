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
package com.avaje.ebean.server.plugin;

import java.util.ArrayList;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.ServerConfiguration;
import com.avaje.ebean.server.core.BootupClassPathSearch;
import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.core.ProtectedMethod;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.util.FactoryHelper;

/**
 * Create the plugin to use for a given dataSource.
 */
public class PluginFactory {

	private static final Class<?>[] CONS_TYPES = {PluginCore.class};
	
	private final DbSpecificFactory dbSpecificFactory = new DbSpecificFactory();
	
	/**
	 * Finds Entities, ScalarTypes etc in the class path.
	 */
	private final BootupClassPathSearch bootupClassSearch;
	
	private final ClusterManager clusterManager;
	
	public PluginFactory(ConfigProperties baseProperties, ClusterManager clusterManager) {
		this.clusterManager = clusterManager;
		this.bootupClassSearch = new BootupClassPathSearch(null, baseProperties);	
		
		boolean search = baseProperties.getBooleanProperty("ebean.classpath.search", true);
		if (search){
			bootupClassSearch.search();
		}
	}
	
    /**
     * Create the appropriate DbPlugin.
     */
    public Plugin create(DataSource ds, ServerConfiguration serverConfig, ConfigProperties props) {
        
    	BootupClasses bootupClasses = null;
    	
    	// get the list from the ServerConfiguration
    	ArrayList<Class<?>> classes = ProtectedMethod.getClasses(serverConfig);
    	if (classes == null || classes.size() == 0){
    		// empty list so use the ones found by classPath search
    		// make sure search has been run
    		bootupClassSearch.search();
    		// use the ones found by classPath search
    		bootupClasses = bootupClassSearch.getBootupClasses();
    	} else {
    		// only use the registered classes
    		bootupClasses = new BootupClasses(classes);
    	}
    	
    	PluginProperties properties = createProperties(serverConfig, ds, props, bootupClasses);
    	
    	PluginDbConfig dbConfig = createDbConfig(properties);
    	
    	PluginCore core = new PluginCore(dbConfig, clusterManager);
    	
    	Plugin plugin = createPlugin(core);
    	
    	return plugin;
    }

    
    private PluginProperties createProperties(ServerConfiguration serverConfig, DataSource ds, ConfigProperties props, BootupClasses bootupClasses) {
    	return new PluginProperties(serverConfig, ds, props, bootupClasses);
    }
    
    private PluginDbConfig createDbConfig(PluginProperties props) {
    	
    	DbSpecific dbSpecific = dbSpecificFactory.create(props);
    	return new PluginDbConfig(props, dbSpecific);
    }
    
    private Plugin createPlugin(PluginCore pluginCore) {
    	
    	PluginProperties props = pluginCore.getDbConfig().getProperties();
    	String key = props.getServerName()+".serverplugin";
        
    	try {
	        String classname = props.getProperty(key, null);
	        if (classname != null) {
	            
	            	Object[] args = {pluginCore};
	            	return (Plugin)FactoryHelper.create(classname, CONS_TYPES, args);	                
	            
	        } else {
	        	return new DefaultPlugin(pluginCore);
	        }
	        
    	} catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }
}
