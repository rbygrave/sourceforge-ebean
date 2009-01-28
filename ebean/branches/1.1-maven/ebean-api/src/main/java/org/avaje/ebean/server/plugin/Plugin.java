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
package org.avaje.ebean.server.plugin;

import org.avaje.ebean.server.autofetch.AutoFetchManager;
import org.avaje.ebean.server.core.InternalEbeanServer;
import org.avaje.ebean.server.core.OrmQueryEngine;
import org.avaje.ebean.server.core.Persister;
import org.avaje.ebean.server.core.RelationalQueryEngine;
import org.avaje.ebean.server.core.ServerCache;
import org.avaje.ebean.server.deploy.DeploymentManager;
import org.avaje.ebean.server.idgen.IdGeneratorManager;
import org.avaje.ebean.server.jmx.MLogControl;
import org.avaje.ebean.server.transaction.TransactionManager;
import org.avaje.ebean.server.transaction.TransactionScopeManager;

/**
 * Defines the functionality that may be overridden depending on the database.
 */
public interface Plugin {


	/**
	 * Create the Persister implementation.
	 */
	public Persister createPersister(InternalEbeanServer server);
	
	/**
	 * Create the ServerCache implementation.
	 */
	public ServerCache createServerCache(InternalEbeanServer server);
	
	/**
	 * Create the OrmQueryEngine implementation.
	 */
	public OrmQueryEngine createOrmQueryEngine(InternalEbeanServer server);
	
	/**
	 * Create the RelationQueryEngine implementation.
	 */
	public RelationalQueryEngine createRelationalQueryEngine(InternalEbeanServer server);
	
	/**
	 * Create the AutoFetchManager implementation.
	 */
	public AutoFetchManager createAutoFetchManager(InternalEbeanServer server);
	
	/**
	 * Create the IdGeneratorManager implementation.
	 */
	public IdGeneratorManager createIdGeneratorManager(InternalEbeanServer server);
	
	/**
	 * Return the pluginCore.
	 */
	public PluginCore getPluginCore();
	
	/**
	 * Return the Db configuration.
	 */
	public PluginDbConfig getDbConfig();

	/**
	 * Return the logControl object.
	 */
	public MLogControl getLogControl();
	
	/**
	 * Return the sql from an external file.
	 */
	public String getSql(String fileName);

	/**
	 * Return the plugin name. Null for the 'default'.
	 */
	public String getServerName();

	/**
	 * Return the properties for this plugin/server.
	 */
	public PluginProperties getProperties();

	/**
	 * Return the deployment manager.
	 */
	public DeploymentManager getDeploymentManager();

	/**
	 * Return the TransactionManager.
	 */
	public TransactionManager getTransactionManager();

	/**
	 * Return the object that manages the transaction scoping.
	 * This means getting and setting the transaction into a thread local.
	 */
	public TransactionScopeManager getTransactionScopeManager();

	/**
	 * Return the debug level.
	 */
	public int getDebugLevel();

	/**
	 * Change the debug level.
	 */
	public void setDebugLevel(int debugLevel);

}
