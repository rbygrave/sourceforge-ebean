/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.lib.sql;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.server.lib.BackgroundRunnable;
import org.avaje.ebean.server.lib.BackgroundThread;
import org.avaje.ebean.server.lib.ConfigProperties;
import org.avaje.ebean.server.lib.GlobalProperties;
import org.avaje.lib.log.LogFactory;


/**
 * Manages access to named DataSources.
 */
public class DataSourceManager {
	
	private static final Logger logger = LogFactory.get(DataSourceManager.class);
	
	private static class DataSourceManagerHolder {
	    private static DataSourceManager me = new DataSourceManager();
	}
	
    /**
     * An alerter that notifies when the database has problems.
     */
    private DataSourceAlertListener alertlistener;
 
    /** 
     * The default datasource used (can be null). 
     */
    private String defaultDataSource;


    /**
     * Set to true when shutting down.
     */
    private boolean isShuttingDown = false;

    /** 
     * Cache of the named DataSources. 
     */
    private Hashtable<String,DataSourcePool> dsMap = new Hashtable<String, DataSourcePool>();

    /**
     * Monitor for creating datasources.
     */
    private Object monitor = new Object();

    /**
     * The database checker registered with BackgroundThread.
     */
    final BackgroundRunnable dbChecker;
    
    /**
     * The frequency to test db while it is up.
     */
    final int dbUpFreqInSecs;
    
    /**
     * The frequency to test db while it is down.
     */
    final int dbDownFreqInSecs = 10;
    
    final ConfigProperties configProperties;
    
	/** 
	 * Singleton private constructor.
	 */
	private DataSourceManager() {
		configProperties = GlobalProperties.getConfigProperties();
	    // perform heart beat every 30 seconds by default
        dbUpFreqInSecs = configProperties.getIntProperty("datasource.heartbeatfreq",30);
        dbChecker = new BackgroundRunnable(new Checker(),dbUpFreqInSecs);

		try {
            
		    BackgroundThread.add(dbChecker);
		    
			initialise();
			
		} catch (DataSourceException e) {
			logger.log(Level.SEVERE, null, e);
		}
	}

	private void initialise() throws DataSourceException {
		
		this.alertlistener = new SimpleAlerter();

		this.defaultDataSource = configProperties.getProperty("datasource.default");
		
		String alertCN = configProperties.getProperty("datasource.alert.class");
		if (alertCN != null){
		    try {
		        Class<?> claz = Class.forName(alertCN);
		        this.alertlistener = (DataSourceAlertListener)claz.newInstance();
		    } catch (Exception ex){
				logger.log(Level.SEVERE, null, ex);
		    }
		}
	}

    /**
     * Send an alert to say the datasource is back up.
     */
	protected void notifyDataSourceUp(String dataSourceName){

        dbChecker.setFreqInSecs(dbUpFreqInSecs);
		
		if (alertlistener != null){
		    alertlistener.dataSourceUp(dataSourceName);
		}
	}

    /**
     * Send an alert to say the datasource is down.
     */
	protected void notifyDataSourceDown(String dataSourceName){
		
        dbChecker.setFreqInSecs(dbDownFreqInSecs);
        
		if (alertlistener != null){
		    alertlistener.dataSourceDown(dataSourceName); 
		}
	}

    /**
     * Send an alert to say the datasource is getting close to its max size.
     */
	protected void notifyWarning(String subject, String msg){
		if (alertlistener != null){
		    alertlistener.warning(subject, msg);
		}
	}

	/**
	 *  Return the singleton instance.
	 */
	private static DataSourceManager getInstance() {
		return DataSourceManagerHolder.me;
	}

    /**
     * Return true when the datasource is shutting down.
     */
	public static boolean isShuttingDown() {
	    return getInstance().isShuttingDown;
	}
	
    /**
     * Shutdown the datasources.
     */
    public static void shutdown() {
        getInstance().shutdownPools();
    }
    
	/**
	 *  Called on Server Shutdown (via the ShutdownHook).
	 *  This will go through all the dataSources and shut them
	 *  down, closing their connections explicitly.
	 */
	private void shutdownPools() {
		this.isShuttingDown = true;
		synchronized(monitor) {
			Iterator<DataSourcePool> i = iterator();
			while (i.hasNext()) {
				try {					
					DataSourcePool ds = (DataSourcePool)i.next();
					ds.shutdown();

				} catch (DataSourceException e) {
					// should never be thrown as the Datasources are all created...
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

    /**
     * Return an iterator of DataSourcePool's.
     */
	public static Iterator<DataSourcePool> iterator() {
	    return getInstance().dsMap.values().iterator();
	}
    
	public static DataSourcePool getDataSource(String name) {
		return getDataSource(name, GlobalProperties.getConfigProperties());
	}
	
    /**
     * Return the named DataSourcePool.
     */
    public static DataSourcePool getDataSource(String name, ConfigProperties configProps){
        return getInstance().get(name, configProps);
    }
    
	private DataSourcePool get(String name, ConfigProperties configProps){
	    if (name == null){
	        name = defaultDataSource;
	        if (defaultDataSource == null){
	            throw new DataSourceException("No default datasource [datasource.default] has been defined.");
	        }
	    }
	    
	    if (configProps == null){
	    	configProps = GlobalProperties.getConfigProperties();
	    }
	    
	    synchronized(monitor){
		    DataSourcePool pool = (DataSourcePool)dsMap.get(name);
		    if (pool == null){
		        Map<String,String> systemProps = configProps.getMap();
                DataSourceParams params = new DataSourceParams(systemProps, "datasource", name);
		        pool = new DataSourcePool(this, params);
		        dsMap.put(name, pool); 
		    }
		    return pool;
		}
	}
	
	/**
	 * Check that the database is up by performing a simple query.
	 * This should be done periodically.  By default every 30 seconds.
	 */
	private void checkDataSource() {

        if (!isShuttingDown()) {

            synchronized(monitor){
	            Iterator<DataSourcePool> it = iterator();
	            while (it.hasNext()) {
	                DataSourcePool ds = (DataSourcePool) it.next();	
	                ds.checkDataSource();
	            }
            }
        }
    }
    
    /**
     * Runs every dbUpFreqInSecs secs to make sure datasource is up.
     */
    class Checker implements Runnable {

        public void run() {
            checkDataSource();
        }
    }
}
