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
package org.avaje.ebean.server.resource;

import org.avaje.lib.utilr;
import org.avaje.ebean.server.plugin.PluginProperties;

/**
 * Helper to create the specific ResourceManager.
 */
public class ResourceControl {

	private static Class<?>[] factoryTypes = {PluginProperties.class};
	
	/**
	 * Create the ResourceManager given the deployment properties.
	 */
	public static ResourceManager createResourceManager(PluginProperties properties) {
		
		ResourceManagerFactory factory = getFactory(properties);
		
		return factory.createResourceManager();
	}
	
	private static ResourceManagerFactory getFactory(PluginProperties properties) {
		
		String resMgrFactCn = properties.getProperty("resourcemanagerfactory", null);
		if (resMgrFactCn != null){
			
			Object[] args = {properties};
			return (ResourceManagerFactory)FactoryHelper.create(resMgrFactCn, factoryTypes, args);
		}
		
		return new DefaultResourceManagerFactory(properties);
	}
}
