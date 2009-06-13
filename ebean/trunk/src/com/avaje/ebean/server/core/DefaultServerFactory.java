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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.DatabasePlatformFactory;
import com.avaje.ebean.net.Constants;
import com.avaje.ebean.server.cache.BasicCacheManager;
import com.avaje.ebean.server.cache.CacheManager;
import com.avaje.ebean.server.lib.ShutdownManager;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.sql.DataSourceGlobalManager;
import com.avaje.ebean.server.lib.sql.TransactionIsolation;
import com.avaje.ebean.server.net.ClusterCommandSecurity;
import com.avaje.ebean.server.net.ClusterContextManager;
import com.avaje.ebean.server.net.CommandProcessor;

/**
 * Default Server side implementation of ServerFactory.
 * <p>
 * If ebean.datasource.factory=jndi then ebean will use JNDI lookup to find the
 * DataSource.
 * </p>
 */
public class DefaultServerFactory implements ServerFactory, Constants {

	private static final Logger logger = Logger.getLogger(DefaultServerFactory.class.getName());

	final ClusterManager clusterManager;

	final JndiDataSourceLookup jndiDataSourceFactory;

	final BootupClassPathSearch bootupClassSearch;	

	
	public DefaultServerFactory() {

		this.clusterManager = createClusterManager();
		this.jndiDataSourceFactory = new JndiDataSourceLookup();
		this.bootupClassSearch = new BootupClassPathSearch(null);
		
		// register so that we can shutdown any Ebean wide
		// resources such as clustering
		ShutdownManager.registerServerFactory(this);
	}


	public void shutdown() {
		clusterManager.shutdown();	
	}

	/**
	 * Create the server reading configuration information
	 * from ebean.properties.
	 */
	public InternalEbeanServer createServer(String name) {

		ConfigBuilder b = new ConfigBuilder();
		ServerConfig config = b.build(name);

		return createServer(config);
	}
	
	
	/**
	 * Create the implementation from the configuration.
	 */
	public InternalEbeanServer createServer(ServerConfig serverConfig) {

		setNamingConvention(serverConfig);
		
		BootupClasses bootupClasses = getBootupClasses(serverConfig);
		
		setDataSource(serverConfig);
		// check the autoCommit and Transaction Isolation
		checkDataSource(serverConfig);

		// determine database platform (Oracle etc)
		setDatabasePlatform(serverConfig);
		
		InternalConfiguration c = new InternalConfiguration(clusterManager, serverConfig, bootupClasses);
		
		CacheManager serverCache = new BasicCacheManager();
		DefaultServer server = new DefaultServer(c, serverCache);
		
		MBeanServer mbeanServer;
		ArrayList<?> list = MBeanServerFactory.findMBeanServer(null);
		if (list.size() == 0){
			// probably not running in a server
			mbeanServer = MBeanServerFactory.createMBeanServer();
		} else {
			// use the first MBeanServer
			mbeanServer = (MBeanServer)list.get(0);
		}

		server.registerMBeans(mbeanServer);
		
		executeDDL(server);
		
		return server;
	}

	/**
	 * Get the classes (entities, scalarTypes, Listeners etc).
	 */
	private BootupClasses getBootupClasses(ServerConfig serverConfig) {
		
		List<Class<?>> entityClasses = serverConfig.getClasses();
		if (entityClasses == null || entityClasses.size() == 0){
			// just use classes we can find via class path search
			return bootupClassSearch.getBootupClasses();
		}
		
		// use classes we explicitly added via configuration
		return new BootupClasses(serverConfig.getClasses());		
	}
	
	/**
	 * Execute the DDL if required.
	 */
	private void executeDDL(InternalEbeanServer server) {
		
		server.getDdlGenerator().execute();	
	}

	/**
	 * Set the naming convention to underscore if it has not already been set.
	 */
	private void setNamingConvention(ServerConfig config){
		if (config.getNamingConvention() == null){
			config.setNamingConvention(new UnderscoreNamingConvention());
		}
	}
	
	/**
	 * Set the DatabasePlatform if it has not already been set.
	 */
	private void setDatabasePlatform(ServerConfig config) {

		DatabasePlatformFactory factory = new DatabasePlatformFactory();
		
		DatabasePlatform dbPlatform = config.getDatabasePlatform();
		if (dbPlatform == null) {
			DatabasePlatform db = factory.create(config);
			config.setDatabasePlatform(db);
		}
	}
	
	/**
	 * Set the DataSource if it has not already been set.
	 */
	private void setDataSource(ServerConfig config) {
		if (config.getDataSource() == null){
			DataSource ds = getDataSourceFromConfig(config);
			config.setDataSource(ds);
		}
	}
	
	private DataSource getDataSourceFromConfig(ServerConfig config) {
		
		DataSource ds = null;
		
		if (config.getDataSourceJndiName() != null){
			ds = jndiDataSourceFactory.lookup(config.getDataSourceJndiName());
			if (ds == null){
				String m = "JNDI lookup for DataSource "+config.getDataSourceJndiName()+" returned null.";
				throw new PersistenceException(m);
			} else {
				return ds;
			}
		}
		
		DataSourceConfig dsConfig = config.getDataSourceConfig();
		if (dsConfig == null){
			String m = "No DataSourceConfig definded for "+config.getName();
			throw new PersistenceException(m);			
		}
			
		return DataSourceGlobalManager.getDataSource(config.getName(), dsConfig);
	}

	/**
	 * Check the autoCommit and Transaction Isolation levels of the DataSource.
	 * <p>
	 * If autoCommit is true this could be a real problem.
	 * </p>
	 * <p>
	 * If the Isolation level is not READ_COMMITED then optimistic concurrency
	 * checking may not work as expected.
	 * </p>
	 */
	private void checkDataSource(ServerConfig serverConfig) {

		if (serverConfig.getDataSource() == null){
			throw new RuntimeException("DataSource not set?");
		}
		
		Connection c = null;
		try {
			c = serverConfig.getDataSource().getConnection();

			if (c.getAutoCommit()) {
				String m = "DataSource [" + serverConfig.getName()+ "] has autoCommit defaulting to true!";
				logger.warning(m);
			}

			int isolationLevel = c.getTransactionIsolation();
			if (isolationLevel != Connection.TRANSACTION_READ_COMMITTED) {
				
				String desc = TransactionIsolation.getLevelDescription(isolationLevel);
				String m = "DataSource [" + serverConfig.getName() 
						+ "] has Transaction Isolation [" + desc
						+ "] rather than READ_COMMITTED!";
				logger.warning(m);
			}
		} catch (SQLException ex) {
			logger.log(Level.SEVERE, null, ex);

		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (SQLException ex) {
					logger.log(Level.SEVERE, null, ex);
				}
			}
		}
	}
	
	/**
	 * Register the cluster command processor with the ClusterManager.
	 */
	private ClusterManager createClusterManager() {

		// register with the cluster manager
		// TransactionEvent commands are sent around the cluster
		ClusterContextManager cm = new ClusterContextManager();
		ClusterCommandSecurity sec = new ClusterCommandSecurity();

		CommandProcessor p = new CommandProcessor();
		p.setUseSessionId(false);
		p.setContextManager(cm);
		p.setCommandSecurity(sec);

		ClusterManager clusterManager = new ClusterManager();
		clusterManager.register(PROCESS_KEY, p);
		return clusterManager;
	}
}
