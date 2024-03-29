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
package com.avaje.ebeaninternal.server.core;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.cache.ServerCacheFactory;
import com.avaje.ebean.cache.ServerCacheManager;
import com.avaje.ebean.cache.ServerCacheOptions;
import com.avaje.ebean.common.BootupEbeanManager;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.PstmtDelegate;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.config.UnderscoreNamingConvention;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebeaninternal.api.SpiBackgroundExecutor;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.cache.DefaultServerCacheFactory;
import com.avaje.ebeaninternal.server.cache.DefaultServerCacheManager;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.jdbc.OraclePstmtBatch;
import com.avaje.ebeaninternal.server.jdbc.StandardPstmtDelegate;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.avaje.ebeaninternal.server.lib.sql.DataSourceGlobalManager;
import com.avaje.ebeaninternal.server.lib.sql.DataSourcePool;
import com.avaje.ebeaninternal.server.lib.thread.ThreadPool;
import com.avaje.ebeaninternal.server.lib.thread.ThreadPoolManager;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default Server side implementation of ServerFactory.
 */
public class DefaultServerFactory implements BootupEbeanManager {

	private static final Logger logger = Logger.getLogger(DefaultServerFactory.class.getName());

	private final ClusterManager clusterManager;

	private final JndiDataSourceLookup jndiDataSourceFactory;

	private final BootupClassPathSearch bootupClassSearch;	

	private final AtomicInteger serverId = new AtomicInteger(1);
	
	private final XmlConfigLoader xmlConfigLoader;
	
	private final XmlConfig xmlConfig;
	
	public DefaultServerFactory() {

		this.clusterManager = new ClusterManager();
		this.jndiDataSourceFactory = new JndiDataSourceLookup();
		
	    List<String> packages = getSearchJarsPackages(GlobalProperties.get("ebean.search.packages", null));
	    List<String> jars = getSearchJarsPackages(GlobalProperties.get("ebean.search.jars", null));
        
		this.bootupClassSearch = new BootupClassPathSearch(null, packages, jars);
		this.xmlConfigLoader = new XmlConfigLoader(null);
		
        this.xmlConfig = xmlConfigLoader.load();
		
		// register so that we can shutdown any Ebean wide
		// resources such as clustering
		ShutdownManager.registerServerFactory(this);
	}

    private List<String> getSearchJarsPackages(String searchPackages) {

        List<String> hitList = new ArrayList<String>();

        if (searchPackages != null) {

            String[] entries = searchPackages.split("[ ,;]");
            for (int i = 0; i < entries.length; i++) {
                hitList.add(entries[i].trim());
            }
        }
        return hitList;
    }

	public void shutdown() {
		clusterManager.shutdown();	
	}

	/**
	 * Create the server reading configuration information
	 * from ebean.properties.
	 */
	public SpiEbeanServer createServer(String name) {

		ConfigBuilder b = new ConfigBuilder();
		ServerConfig config = b.build(name);

		return createServer(config);
	}
	
	private SpiBackgroundExecutor createBackgroundExecutor(ServerConfig serverConfig, int uniqueServerId) {
		
		String namePrefix = "Ebean-"+serverConfig.getName();
		
		// the size of the pool for executing periodic tasks (such as cache flushing)
		int schedulePoolSize = GlobalProperties.getInt("backgroundExecutor.schedulePoolsize", 1);
		
		// the side of the main pool for immediate background task execution
		int minPoolSize = GlobalProperties.getInt("backgroundExecutor.minPoolSize", 1);
        int poolSize = GlobalProperties.getInt("backgroundExecutor.poolsize", 20);
        int maxPoolSize = GlobalProperties.getInt("backgroundExecutor.maxPoolSize", poolSize);
        
        int idleSecs = GlobalProperties.getInt("backgroundExecutor.idlesecs", 60);
		int shutdownSecs = GlobalProperties.getInt("backgroundExecutor.shutdownSecs", 30);
		
		boolean useTrad = GlobalProperties.getBoolean("backgroundExecutor.traditional", true);
        
		if (useTrad){
		    // this pool will use Idle seconds between min and max so I think it is better
		    // as it will let the thread count float between the min and max
    		ThreadPool pool = ThreadPoolManager.getThreadPool(namePrefix);
    		pool.setMinSize(minPoolSize);
    		pool.setMaxSize(maxPoolSize);
    		pool.setMaxIdleTime(idleSecs*1000);
    		return new TraditionalBackgroundExecutor(pool, schedulePoolSize, shutdownSecs, namePrefix);
		} else {
		    return new DefaultBackgroundExecutor(poolSize, schedulePoolSize, idleSecs, shutdownSecs, namePrefix);
		}
	}
	
	/**
	 * Create the implementation from the configuration.
	 */
	public SpiEbeanServer createServer(ServerConfig serverConfig) {

	    synchronized (this) {
    		setNamingConvention(serverConfig);
    		
    		BootupClasses bootupClasses = getBootupClasses(serverConfig);
    
    		setDataSource(serverConfig);
    		// check the autoCommit and Transaction Isolation
    		boolean online = checkDataSource(serverConfig);
    		
    		// determine database platform (Oracle etc)
    		setDatabasePlatform(serverConfig);
    		if (serverConfig.getDbEncrypt() != null){
    		    // use a configured DbEncrypt rather than the platform default 
    		    serverConfig.getDatabasePlatform().setDbEncrypt(serverConfig.getDbEncrypt());
    		}
    		
    		DatabasePlatform dbPlatform = serverConfig.getDatabasePlatform();
    		
    		PstmtBatch pstmtBatch = null;
    		
    		if (dbPlatform.getName().startsWith("oracle")){
    			PstmtDelegate pstmtDelegate = serverConfig.getPstmtDelegate();
    			if (pstmtDelegate == null){
    				// try to provide the 
    				pstmtDelegate = getOraclePstmtDelegate(serverConfig.getDataSource());
    			}
    			if (pstmtDelegate != null){
    				// We can support JDBC batching with Oracle
    				// via OraclePreparedStatement  
    				pstmtBatch = new OraclePstmtBatch(pstmtDelegate);
    			} 
    			if (pstmtBatch == null){
    				// We can not support JDBC batching with Oracle
    				logger.warning("Can not support JDBC batching with Oracle without a PstmtDelegate");
    				serverConfig.setPersistBatching(false);
    			}
    		}
    		
    		// inform the NamingConvention of the associated DatabasePlaform 
    		serverConfig.getNamingConvention().setDatabasePlatform(serverConfig.getDatabasePlatform());
    
    		ServerCacheManager cacheManager  = getCacheManager(serverConfig);
    
    		int uniqueServerId = serverId.incrementAndGet();
    		SpiBackgroundExecutor bgExecutor = createBackgroundExecutor(serverConfig, uniqueServerId);
    			
    		InternalConfiguration c = new InternalConfiguration(xmlConfig, clusterManager, cacheManager, 
    		        bgExecutor, serverConfig, bootupClasses, pstmtBatch);
    		
    		DefaultServer server = new DefaultServer(c, cacheManager);
    		
    		cacheManager.init(server);
    		
    		MBeanServer mbeanServer;
    		ArrayList<?> list = MBeanServerFactory.findMBeanServer(null);
    		if (list.size() == 0){
    			// probably not running in a server
    			mbeanServer = MBeanServerFactory.createMBeanServer();
    		} else {
    			// use the first MBeanServer
    			mbeanServer = (MBeanServer)list.get(0);
    		}
    
    		server.registerMBeans(mbeanServer, uniqueServerId);
    		
    		// generate and run DDL if required
    		executeDDL(server, online);
    		
    		// initialise prior to registering with clusterManager
    		server.initialise();
    		
    		if (online){
                if (clusterManager.isClustering()) {
                    // register the server once it has been created
                    clusterManager.registerServer(server);
        		}
    		    
        		// warm the cache in 30 seconds 
        		int delaySecs = GlobalProperties.getInt("ebean.cacheWarmingDelay", 30);
        		long sleepMillis = 1000 * delaySecs;
        
        		if (sleepMillis > 0){
        			Timer t = new Timer("EbeanCacheWarmer", true);
        			t.schedule(new CacheWarmer(sleepMillis, server), sleepMillis);
        		}
    		}
    		
    		// start any services after registering with clusterManager
    		server.start();
    		return server;
	    }
	}

	private PstmtDelegate getOraclePstmtDelegate(DataSource ds) {
		
		if (ds instanceof DataSourcePool){
			// Using Ebean's own DataSource implementation
			return new StandardPstmtDelegate();
		}
		
		return null;
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
			beanOptions.setMaxIdleSecs(GlobalProperties.getInt("cache.maxIdleTime", 60*10));//10 minutes
			beanOptions.setMaxSecsToLive(GlobalProperties.getInt("cache.maxTimeToLive", 60*60*6));//6 hrs
			//beanOptions.setTrimFrequency(GlobalProperties.getInt("cache.trimFrequency", 1000*60));//1 minute
		}
		
		ServerCacheOptions queryOptions = null;//serverConfig.getDefaultQueryCacheOptions();
		if (queryOptions == null) {
			// these settings are for a cache per bean type
			queryOptions = new ServerCacheOptions();
			queryOptions.setMaxSize(GlobalProperties.getInt("querycache.maxSize", 100));
			queryOptions.setMaxIdleSecs(GlobalProperties.getInt("querycache.maxIdleTime", 60*10));//10 minutes
			queryOptions.setMaxSecsToLive(GlobalProperties.getInt("querycache.maxTimeToLive", 60*60*6));//6 hrs
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
        bootupClasses.addTransactionEventListeners(serverConfig.getTransactionEventListeners());
		bootupClasses.addPersistListeners(serverConfig.getPersistListeners());
		bootupClasses.addQueryAdapters(serverConfig.getQueryAdapters());
		bootupClasses.addServerConfigStartup(serverConfig.getServerConfigStartupListeners());
		
		// run any ServerConfigStartup instances
		bootupClasses.runServerConfigStartup(serverConfig);
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

        List<String> jars = serverConfig.getJars();
		List<String> packages = serverConfig.getPackages();
		
		if ((packages != null && !packages.isEmpty()) || (jars != null && !jars.isEmpty())){
			// filter by package name
			BootupClassPathSearch search = new BootupClassPathSearch(null, packages, jars);
			return search.getBootupClasses();
		}

		// just use classes we can find via class path search
		return bootupClassSearch.getBootupClasses().createCopy();
	}
	
	/**
	 * Execute the DDL if required.
	 */
	private void executeDDL(SpiEbeanServer server, boolean online) {
		
		server.getDdlGenerator().execute(online);	
	}

	/**
	 * Set the naming convention to underscore if it has not already been set.
	 */
	private void setNamingConvention(ServerConfig config){
		if (config.getNamingConvention() == null){
		    UnderscoreNamingConvention nc = new UnderscoreNamingConvention();
			config.setNamingConvention(nc);
			
			String v = config.getProperty("namingConvention.useForeignKeyPrefix");
			if (v != null){
			    boolean useForeignKeyPrefix = Boolean.valueOf(v);
			    nc.setUseForeignKeyPrefix(useForeignKeyPrefix);
			}

	        String sequenceFormat = config.getProperty("namingConvention.sequenceFormat");
            if (sequenceFormat != null){
                nc.setSequenceFormat(sequenceFormat);
            }
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
			logger.info("DatabasePlatform name:"+config.getName()+" platform:"+db.getName());
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
		
		if (dsConfig.isOffline()){
		    if (config.getDatabasePlatformName() == null){
	            String m = "You MUST specify a DatabasePlatformName on ServerConfig when offline";
	            throw new PersistenceException(m);          
		    }
		    return null;
		}
		
		if (dsConfig.getHeartbeatSql() == null){
			// use default heartbeatSql from the DatabasePlatform
			String heartbeatSql = getHeartbeatSql(dsConfig.getDriver());
			dsConfig.setHeartbeatSql(heartbeatSql);
		}
		
		return DataSourceGlobalManager.getDataSource(config.getName(), dsConfig);
	}
	
	/**
	 * Return a heartbeatSql depending on the jdbc driver name.
	 */
	private String getHeartbeatSql(String driver) {
	    if (driver != null){
    		String d = driver.toLowerCase();
    		if (d.contains("oracle")){
    			return "select 'x' from dual";
    		}
    		if (d.contains(".h2.") || d.contains(".mysql.") || d.contains("postgre")){
    			return "select 1";
    		}
	    }
		return null;
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
	private boolean checkDataSource(ServerConfig serverConfig) {

		if (serverConfig.getDataSource() == null){
		    if (serverConfig.getDataSourceConfig().isOffline()){
		        // this is ok - offline DDL generation etc
	            return false;
		    }
            throw new RuntimeException("DataSource not set?");
		}
		
		Connection c = null;
		try {
			c = serverConfig.getDataSource().getConnection();

			if (c.getAutoCommit()) {
				String m = "DataSource [" + serverConfig.getName()+ "] has autoCommit defaulting to true!";
				logger.warning(m);
			}
			
			return true;
			
		} catch (SQLException ex) {
			throw new PersistenceException(ex);
			
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
	
    
	private static class CacheWarmer extends TimerTask {

		private static final Logger log = Logger.getLogger(CacheWarmer.class.getName());
		
		private final long sleepMillis;
		private final EbeanServer server;
		
		CacheWarmer(long sleepMillis, EbeanServer server){
			this.sleepMillis = sleepMillis;
			this.server = server;
		}
		
		public void run() {
			try {
				Thread.sleep(sleepMillis);
			} catch (InterruptedException e) {
				String msg = "Error while sleeping prior to cache warming";
				log.log(Level.SEVERE, msg, e);
			}
			server.runCacheWarming();
		}
		
		
	}
}
