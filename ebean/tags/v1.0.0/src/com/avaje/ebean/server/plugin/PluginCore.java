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

import com.avaje.ebean.server.deploy.DeploymentManager;
import com.avaje.ebean.server.lib.cluster.ClusterManager;

/**
 * The core properties of a Plugin.
 */
public class PluginCore {

	private final PluginDbConfig dbConfig;
	
	private final DeploymentManager deploymentManager;
	
	private final ClusterManager clusterManager;
	
	public PluginCore(PluginDbConfig dbConfig, ClusterManager clusterManager) {
		this.dbConfig = dbConfig;
		this.clusterManager = clusterManager;
		this.deploymentManager = new DeploymentManager(dbConfig);
	}

	public PluginDbConfig getDbConfig() {
		return dbConfig;
	}


	public DeploymentManager getDeploymentManager() {
		return deploymentManager;
	}

	public ClusterManager getClusterManager() {
		return clusterManager;
	}

}
