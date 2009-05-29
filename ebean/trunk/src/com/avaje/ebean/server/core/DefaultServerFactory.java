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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.sql.DataSource;

import com.avaje.ebean.ServerConfiguration;
import com.avaje.ebean.net.Constants;
import com.avaje.ebean.server.deploy.parse.SqlReservedWords;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;
import com.avaje.ebean.server.lib.ShutdownManager;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.sql.DataSourceGlobalManager;
import com.avaje.ebean.server.lib.sql.TransactionIsolation;
import com.avaje.ebean.server.net.ClusterCommandSecurity;
import com.avaje.ebean.server.net.ClusterContextManager;
import com.avaje.ebean.server.net.CommandProcessor;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.plugin.PluginFactory;
import com.avaje.ebean.util.Message;

/**
 * Default Server side implementation of ServerFactory.
 * <p>
 * If ebean.datasource.factory=jndi then ebean will use JNDI lookup to find the
 * DataSource.
 * </p>
 */
public class DefaultServerFactory implements ServerFactory, Constants {

	private static final Logger logger = Logger.getLogger(DefaultServerFactory.class.getName());

	final PluginFactory pluginFactory;

	final ConfigProperties baseProperties;
	
	final ClusterManager clusterManager;
	
	DataSourceFactory dsFactory;

	public DefaultServerFactory() {

		baseProperties = GlobalProperties.getConfigProperties();
		
		clusterManager = createClusterManager(baseProperties);
		pluginFactory = new PluginFactory(baseProperties, clusterManager);
		
		
		String cn = baseProperties.getProperty("ebean.datasource.factory");

		if (cn != null) {
			if (cn.equalsIgnoreCase("default")) {
				// Use built in DataSourceManager

			} else if (cn.equalsIgnoreCase("jndi")) {
				// Use built in JNDI support
				dsFactory = new JndiDataSourceFactory();

			} else {
				try {
					// use a custom DataSourceFactory
					Class<?> cls = Class.forName(cn);
					dsFactory = (DataSourceFactory) cls.newInstance();
				} catch (Exception ex) {
					throw new RuntimeException("FATAL Error:", ex);
				}
			}
		}
		
		// register so that we can shutdown any Ebean wide
		// resources such as clustering
		ShutdownManager.registerServerFactory(this);
	}

	/**
	 * Register the cluster command processor with the ClusterManager.
	 */
	protected ClusterManager createClusterManager(ConfigProperties properties) {

		// register with the cluster manager
		// TransactionEvent commands are sent around the cluster
		ClusterContextManager cm = new ClusterContextManager();
		ClusterCommandSecurity sec = new ClusterCommandSecurity();

		CommandProcessor p = new CommandProcessor();
		p.setUseSessionId(false);
		p.setContextManager(cm);
		p.setCommandSecurity(sec);

		ClusterManager clusterManager = new ClusterManager(properties);
		clusterManager.register(PROCESS_KEY, p);
		return clusterManager;
	}

	
	public void shutdown() {
		clusterManager.shutdown();	
	}

	public InternalEbeanServer createServer(String name) {
		return createServer(new ServerConfiguration(name));
	}
	
	/**
	 * Create the implementation for a serverName. Note that the serverName
	 * typically matches the DataSource name.
	 */
	public InternalEbeanServer createServer(ServerConfiguration serverConfig) {

		String name = serverConfig.getName();

		// start with baseProperties and then override with
		// the server specific properties
		ConfigProperties configProps = new ConfigProperties(baseProperties, serverConfig.getProperties());
		
		addExtraSqlReservedWords(configProps);
		
		DataSource ds = serverConfig.getDataSource();
		if (ds == null){
			// Get the DataSource using the built in DataSourceFactory.
			ds = getDataSource(name, configProps);
		} 
		

		// check the autoCommit and Transaction Isolation
		checkDataSource(ds, name);

		Plugin plugin = pluginFactory.create(ds, serverConfig, configProps);
		
		DefaultServer server = new DefaultServer(plugin);
		
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
		
		String dbConfig = plugin.getDbConfig().getClass().getName();
		if (name == null) {
			name = "";
		}
		logger.info(Message.msg("plugin.startup", new Object[] { name, dbConfig }));

		executeDDL(server);
		
		return server;
	}

	protected void executeDDL(InternalEbeanServer server) {
		
		
		server.createDdlGenerator().execute();	
	}
	
	/**
	 * Add extra sql reserved words to the ones known by Ebean.
	 * <p>
	 * When Ebean generates sql table alias it checks to make sure that the
	 * alias is not a reserved word such as "as".
	 * </p>
	 */
	private void addExtraSqlReservedWords(ConfigProperties configProps){
		String extraKeywords = configProps.getProperty("ebean.sqlreservedwords");
		if (extraKeywords != null){
			String[] keywords = extraKeywords.split(",");
			for (int i = 0; i < keywords.length; i++) {
				SqlReservedWords.addKeyword(keywords[i]);
			}
		}
	}
	
	/**
	 * Return the DataSource given the name.
	 * <p>
	 * Note the Ebean "serverName" is the same as the "DataSource" name.
	 * </p>
	 */
	public DataSource getDataSource(String name, ConfigProperties configProps) {
		
		if (dsFactory != null) {
			return dsFactory.createDataSource(name, configProps);
		}

		return DataSourceGlobalManager.getDataSource(name, configProps);
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
	private void checkDataSource(DataSource dataSource, String name) {

		Connection c = null;
		try {
			c = dataSource.getConnection();

			if (c.getAutoCommit()) {
				String m = "DataSource [" + name + "] has autoCommit defaulting to true!";
				logger.warning(m);
			}

			int isolationLevel = c.getTransactionIsolation();
			if (isolationLevel != Connection.TRANSACTION_READ_COMMITTED) {
				
				String desc = TransactionIsolation.getLevelDescription(isolationLevel);
				String m = "DataSource [" + name + "] has Transaction Isolation [" + desc
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
}
