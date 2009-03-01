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
package com.avaje.ebean.server.lib.sql;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.BackgroundRunnable;
import com.avaje.ebean.server.lib.BackgroundThread;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;


/**
 * Manages access to named DataSources.
 */
public class DataSourceManager implements DataSourceNotify {
	
	private static final Logger logger = Logger.getLogger(DataSourceManager.class.getName());
	
    /**
     * An alerter that notifies when the database has problems.
     */
    private final DataSourceAlertListener alertlistener;

    /** 
     * Cache of the named DataSources. 
     */
    private final Hashtable<String,DataSourcePool> dsMap = new Hashtable<String, DataSourcePool>();

    /**
     * Monitor for creating dataSources.
     */
    private final Object monitor = new Object();

    /**
     * The database checker registered with BackgroundThread.
     */
    private final BackgroundRunnable dbChecker;
    
    /**
     * The frequency to test db while it is up.
     */
    private final int dbUpFreqInSecs;
    
    /**
     * The frequency to test db while it is down.
     */
    private final int dbDownFreqInSecs;
    
    private final ConfigProperties defaultConfig;
    
    /** 
     * The default dataSource used (can be null). 
     */
    private final String defaultDataSource;

    /**
     * Set to true when shutting down.
     */
    private boolean shuttingDown;
    
    /**
     * Construct based on the GlobalProperties.
     */
	public DataSourceManager() {
		this(GlobalProperties.getConfigProperties());
	}
	
	/** 
	 * Construct with explicit ConfigProperties.
	 */
	public DataSourceManager(ConfigProperties defaultConfig) {
		this.defaultConfig = defaultConfig;
	    
		this.alertlistener = createAlertListener(defaultConfig);
		
		// perform heart beat every 30 seconds by default
        this.dbUpFreqInSecs = defaultConfig.getIntProperty("datasource.heartbeatfreq",30);
        this.dbDownFreqInSecs = defaultConfig.getIntProperty("datasource.deadbeatfreq",10);
        this.defaultDataSource = defaultConfig.getProperty("datasource.default");
        
        this.dbChecker = new BackgroundRunnable(new Checker(), dbUpFreqInSecs);
        
		try {
	        BackgroundThread.add(dbChecker);
            		    
		} catch (Exception e) {
			logger.log(Level.SEVERE, null, e);
		}
	}

	private DataSourceAlertListener createAlertListener(ConfigProperties configProperties) throws DataSourceException {
		
		String alertCN = configProperties.getProperty("datasource.alert.class");
		if (alertCN == null){
			return new SimpleAlerter(configProperties);
			
		} else {
		    try {
		        Class<?> claz = Class.forName(alertCN);
		        DataSourceAlertListener alert = (DataSourceAlertListener)claz.newInstance();
		        alert.initialise(configProperties);
		        return alert;
		        
		    } catch (Exception ex){
		    	throw new DataSourceException(ex);
		    }
		}
	}

    /**
     * Send an alert to say the dataSource is back up.
     */
	public void notifyDataSourceUp(String dataSourceName){

        dbChecker.setFreqInSecs(dbUpFreqInSecs);
		
		if (alertlistener != null){
		    alertlistener.dataSourceUp(dataSourceName);
		}
	}

    /**
     * Send an alert to say the dataSource is down.
     */
	public void notifyDataSourceDown(String dataSourceName){
		
        dbChecker.setFreqInSecs(dbDownFreqInSecs);
        
		if (alertlistener != null){
		    alertlistener.dataSourceDown(dataSourceName); 
		}
	}

    /**
     * Send an alert to say the dataSource is getting close to its max size.
     */
	public void notifyWarning(String subject, String msg){
		if (alertlistener != null){
		    alertlistener.warning(subject, msg);
		}
	}

    /**
     * Return true when the dataSource is shutting down.
     */
	public boolean isShuttingDown() {
		synchronized(monitor) {
			return shuttingDown;
		}
	}
	
    /**
     * Shutdown the dataSources.
     */
    public void shutdown() {
		
		synchronized(monitor) {
			
			this.shuttingDown = true;
			
			Iterator<DataSourcePool> i = dsMap.values().iterator();
			while (i.hasNext()) {
				try {					
					DataSourcePool ds = (DataSourcePool)i.next();
					ds.shutdown();

				} catch (DataSourceException e) {
					// should never be thrown as the DataSources are all created...
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

    /**
     * Return the DataSourcePool's.
     */
	public List<DataSourcePool> getPools() {
		synchronized(monitor) {
			// create a copy of the DataSourcePool's
			ArrayList<DataSourcePool> list = new ArrayList<DataSourcePool>();
			list.addAll(dsMap.values());
			return list;
		}
	}
    
	/**
	 * Get the dataSource using the default ConfigProperties.
	 */
	public DataSourcePool getDataSource(String name) {
		return getDataSource(name, defaultConfig);
	}
	
    /**
     * Get the dataSource using explicit ConfigProperties.
     */
    public DataSourcePool getDataSource(String name, ConfigProperties configProps){
        return get(name, configProps);
    }
    
	private DataSourcePool get(String name, ConfigProperties configProps){
	    if (name == null){
	        name = defaultDataSource;
	        if (defaultDataSource == null){
	            throw new DataSourceException("No default datasource [datasource.default] has been defined.");
	        }
	    }
	    
	    if (configProps == null){
	    	configProps = defaultConfig;
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
	 * Check that the database is up by performing a simple query. This should
	 * be done periodically. By default every 30 seconds.
	 */
	private void checkDataSource() {

		synchronized (monitor) {
			if (!isShuttingDown()) {
				Iterator<DataSourcePool> it = dsMap.values().iterator();
				while (it.hasNext()) {
					DataSourcePool ds = (DataSourcePool) it.next();
					ds.checkDataSource();
				}
			}
		}
	}
    
    /**
     * Runs every dbUpFreqInSecs to make sure dataSource is up.
     */
    private final class Checker implements Runnable {

        public void run() {
            checkDataSource();
        }
    }
}
