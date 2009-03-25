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

import java.lang.reflect.Constructor;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.core.DefaultServerCache;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.OrmQueryEngine;
import com.avaje.ebean.server.core.Persister;
import com.avaje.ebean.server.core.RelationalQueryEngine;
import com.avaje.ebean.server.core.ServerCache;
import com.avaje.ebean.server.deploy.DeploymentManager;
import com.avaje.ebean.server.idgen.IdGeneratorManager;
import com.avaje.ebean.server.jmx.MLogControl;
import com.avaje.ebean.server.persist.DefaultPersister;
import com.avaje.ebean.server.query.DefaultOrmQueryEngine;
import com.avaje.ebean.server.query.DefaultRelationalQueryEngine;
import com.avaje.ebean.server.transaction.DefaultTransactionScopeManager;
import com.avaje.ebean.server.transaction.SpringAwareTransactionScopeManager;
import com.avaje.ebean.server.transaction.TransactionManager;
import com.avaje.ebean.server.transaction.TransactionScopeManager;

/**
 * The core implementation which can be extended.
 */
public class DefaultPlugin implements Plugin {

	/**
	 * The transaction manager.
	 */
	private final TransactionManager transactionManager;

	private final TransactionScopeManager transactionScopeManager;

	private final PluginProperties properties;

	private final PluginCore pluginCore;

	private final PluginDbConfig dbConfig;

	private int debugLevel;

	/**
	 * Create the DbPlugin.
	 */
	public DefaultPlugin(PluginCore pluginCore) {

		this.pluginCore = pluginCore;
		this.dbConfig = pluginCore.getDbConfig();
		this.properties = dbConfig.getProperties();

		this.transactionManager = new TransactionManager(pluginCore);
		this.transactionScopeManager = createScopeManager(properties, transactionManager);
	}
	
	/**
	 * Create a TransactionScopeManager. This could be aware of external transaction managers
	 * such as Spring etc.
	 */
	protected TransactionScopeManager createScopeManager(PluginProperties props, TransactionManager mgr) {
		String cn = props.getProperty("transactionScopeManager", null);
		if (cn != null){
			try {
				Class<?> cls = Class.forName(cn);
				Constructor<?> constructor = cls.getConstructor(TransactionManager.class);
				return (TransactionScopeManager)constructor.newInstance(mgr);
			} catch (Exception e){
				String m = "Error trying to create TransactionScopeManager with "+cn;
				throw new PersistenceException(m, e);
			}
		}
		if ( props.getPropertyBoolean("spring.transactions", false) ) {
			// built in spring aware transactions
			return new SpringAwareTransactionScopeManager(mgr);
			
		} else {
			// standard one
			return new DefaultTransactionScopeManager(mgr);
		}
		
	}

	public Persister createPersister(InternalEbeanServer server) {
		return new DefaultPersister(this, server);
	}

	public ServerCache createServerCache(InternalEbeanServer server) {
		return new DefaultServerCache(this, server);
	}

	public OrmQueryEngine createOrmQueryEngine(InternalEbeanServer server) {
		return new DefaultOrmQueryEngine(this, server);
	}

	public RelationalQueryEngine createRelationalQueryEngine(InternalEbeanServer server) {
		return new DefaultRelationalQueryEngine(this, server);
	}

	public IdGeneratorManager createIdGeneratorManager(InternalEbeanServer server) {
		return new IdGeneratorManager(dbConfig.getProperties(), server);
	}

	public AutoFetchManager createAutoFetchManager(InternalEbeanServer server) {
		return dbConfig.createAutoFetchManager(server);
	}

	public PluginDbConfig getDbConfig() {
		return dbConfig;
	}

	public PluginProperties getProperties() {
		return properties;
	}

	public PluginCore getPluginCore() {
		return pluginCore;
	}

	public MLogControl getLogControl() {
		return dbConfig.getLogControl();
	}

	/**
	 * Return the plugin name. Null for the 'default'.
	 */
	public String getServerName() {
		return dbConfig.getProperties().getServerName();
	}

	public DeploymentManager getDeploymentManager() {
		return pluginCore.getDeploymentManager();
	}

	/**
	 * Return the TransactionManager.
	 */
	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	/**
	 * Return the TransactionScopeManager. Either the Default Ebean Thread Local
	 * mechanism or one that works with external transaction managers such as
	 * Spring.
	 */
	public TransactionScopeManager getTransactionScopeManager() {
		return transactionScopeManager;
	}

	/**
	 * Return the sql from an external file.
	 */
	public String getSql(String fileName) {
		return dbConfig.getSql(fileName);
	}

	/**
	 * Return the debug level.
	 */
	public int getDebugLevel() {
		return debugLevel;
	}

	/**
	 * Change the debug level.
	 */
	public void setDebugLevel(int debugLevel) {
		this.debugLevel = debugLevel;
	}
}
