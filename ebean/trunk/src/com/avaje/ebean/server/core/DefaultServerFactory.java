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

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.DatabasePlatformFactory;
import com.avaje.ebean.internal.InternalEbeanServer;
import com.avaje.ebean.internal.InternalEbeanServerFactory;
import com.avaje.ebean.net.Constants;
import com.avaje.ebean.server.cache.DefaultServerCacheFactory;
import com.avaje.ebean.server.cache.DefaultServerCacheManager;
import com.avaje.ebean.server.cache.ServerCacheFactory;
import com.avaje.ebean.server.cache.ServerCacheManager;
import com.avaje.ebean.server.cache.ServerCacheOptions;
import com.avaje.ebean.server.lib.ShutdownManager;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.sql.DataSourceGlobalManager;
import com.avaje.ebean.server.lib.sql.TransactionIsolation;
import com.avaje.ebean.server.net.ClusterCommandSecurity;
import com.avaje.ebean.server.net.ClusterContextManager;
import com.avaje.ebean.server.net.CommandProcessor;

/**
 * Default Server side implementation of ServerFactory.
 */
public class DefaultServerFactory implements InternalEbeanServerFactory, Constants {

	private static final Logger logger = Logger.getLogger(DefaultServerFactory.class.getName());

	private final ClusterManager clusterManager;

	private final JndiDataSourceLookup jndiDataSourceFactory;

	private final BootupClassPathSearch bootupClassSearch;	

	
	public DefaultServerFactory() {

		this.clusterManager = createClusterManager();
		this.jndiDataSourceFactory = new JndiDataSourceLookup();
		this.bootupClassSearch = new BootupClassPathSearch(null, null);
		
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

		// determine database platform (Oracle etc)
		setDatabasePlatform(serverConfig);

		setDataSource(serverConfig);
		// check the autoCommit and Transaction Isolation
		checkDataSource(serverConfig);
		
		// inform the NamingConvention of the associated DatabasePlaform 
		serverConfig.getNamingConvention().setDatabasePlatform(serverConfig.getDatabasePlatform());

		ServerCacheManager cacheManager  = getCacheManager(serverConfig);

		InternalConfiguration c = new InternalConfiguration(clusterManager, cacheManager, serverConfig, bootupClasses);
		
		DefaultServer server = new DefaultServer(c, cacheManager);
		
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
	 * Create and return the CacheManager.
	 */
	private ServerCacheManager getCacheManager(ServerConfig serverConfig) {
		
		//TODO: External configuration of ServerCacheFactory + options.
		
		ServerCacheOptions beanOptions = null;//serverConfig.getDefaultBeanCacheOptions();
		if (beanOptions == null){
			// these settings are for a cache per bean type
			beanOptions = new ServerCacheOptions();
			beanOptions.setMaxSize(GlobalProperties.getInt("cache.maxSize", 1000));
			beanOptions.setMaxIdleTime(GlobalProperties.getInt("cache.maxIdleTime", 1000*60*10));//10 minutes
			beanOptions.setMaxTimeToLive(GlobalProperties.getInt("cache.maxTimeToLive", 1000*60*60*6));//6 hrs
			//beanOptions.setTrimFrequency(GlobalProperties.getInt("cache.trimFrequency", 1000*60));//1 minute
		}
		
		ServerCacheOptions queryOptions = null;//serverConfig.getDefaultQueryCacheOptions();
		if (queryOptions == null) {
			// these settings are for a cache per bean type
			queryOptions = new ServerCacheOptions();
			queryOptions.setMaxSize(GlobalProperties.getInt("querycache.maxSize", 100));
			queryOptions.setMaxIdleTime(GlobalProperties.getInt("querycache.maxIdleTime", 1000*60*10));//10 minutes
			queryOptions.setMaxTimeToLive(GlobalProperties.getInt("querycache.maxTimeToLive", 1000*60*60*6));//6 hrs
			//queryOptions.setTrimFrequency(GlobalProperties.getInt("querycache.trimFrequency", 1000*60));//1 minute
		}
		
		ServerCacheFactory cacheFactory = null;//serverConfig.getServerCacheFactory();
		if (cacheFactory == null) {
			cacheFactory = new DefaultServerCacheFactory();
		}
		
		return new DefaultServerCacheManager(cacheFactory, beanOptions, queryOptions);
	}

	/**
	 * Get the entities, scalarTypes, Listeners etc combining the
	 * class registered ones with the already created instances.
	 */
	private BootupClasses getBootupClasses(ServerConfig serverConfig) {
		
		BootupClasses bootupClasses = getBootupClasses1(serverConfig);
		bootupClasses.addPersistControllers(serverConfig.getPersistControllers());
		
		return bootupClasses;
	}
	
	/**
	 * Get the class based entities, scalarTypes, Listeners etc.
	 */
	private BootupClasses getBootupClasses1(ServerConfig serverConfig) {
		
		List<Class<?>> entityClasses = serverConfig.getClasses();
		if (entityClasses != null && entityClasses.size() > 0){
			// use classes we explicitly added via configuration
			return new BootupClasses(serverConfig.getClasses());		
		}

		List<String> packages = serverConfig.getPackages();
		if (packages != null && packages.size() > 0){
			// filter by package name
			BootupClassPathSearch search = new BootupClassPathSearch(null, packages);
			return search.getBootupClasses();
		}

		// just use classes we can find via class path search
		return bootupClassSearch.getBootupClasses().createCopy();
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

		
		DatabasePlatform dbPlatform = config.getDatabasePlatform();
		if (dbPlatform == null) {
			
			DatabasePlatformFactory factory = new DatabasePlatformFactory();
			
			DatabasePlatform db = factory.create(config);
			config.setDatabasePlatform(db);
			logger.info("DatabasePlatform "+config.getName()+" "+db.getName());
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
			
		if (dsConfig.getHeartbeatSql() == null){
			// use default heartbeatSql from the DatabasePlatform
			String heartbeatSql = config.getDatabasePlatform().getHeartbeatSql();
			dsConfig.setHeartbeatSql(heartbeatSql);
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
