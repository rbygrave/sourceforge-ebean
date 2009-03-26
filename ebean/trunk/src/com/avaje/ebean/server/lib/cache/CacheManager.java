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
package com.avaje.ebean.server.lib.cache;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.lib.BackgroundThread;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.GlobalProperties;

/**
 * Provides access and manages the caches.
 * <p>
 * SystemProperties:<br>
 * <pre><code>
 * ## elements lastAccessed more than 300 secs ago will be trimmed from the cache
 * cache.timeoutsecs=300
 * 
 * ## trim the caches every 100 seconds
 * cache.trimfreq=100
 * </code></pre>
 * </p>
 */
public class CacheManager {

	private static final Logger logger = Logger.getLogger(CacheManager.class.getName());
	
	private static class Single {
		private static CacheManager me = new CacheManager();
	}
	
	
    /**
     * The map of caches.
     */
	private final DoubleMap<String, Cache> cacheHolder = new DoubleMap<String, Cache>(new CacheManager.CacheCreate());
	
    private int trimFreqInSecs;
        
    final ConfigProperties configProperties;
    
    /**
     * Singleton.
     */
    private CacheManager() {        
    	configProperties = GlobalProperties.getConfigProperties();
        trimFreqInSecs = configProperties.getIntProperty("cache.trimfreq", 100);

        BackgroundThread.add(trimFreqInSecs, new Trimmer());
    }

    /**
     * Return the singleton instance.
     */
    private static CacheManager getInstance() {
        return Single.me;
    }
    
    /**
     * Returns the cache for a given name.
     * This will create a cache if it does not exist.
     * <p>
     * If you want to set a cache level validator you can 
     * do something like
     * <pre><code>
     * Cache cache = CacheManager.get("mycache");
     * if (!cache.hasValidator()) {
     *     Validator v = ...;
     *     cache.setValidator(v);
     * }
     * </code></pre>
     * </p>
     */
    public static Cache get(String cacheName){
        return getInstance().getCache(cacheName);
    }
    
    private Cache getCache(String cacheName){
    	return cacheHolder.get(cacheName);
    }
    
    
    
    /**
     * Returns an Iterator of caches.
     */
    public static Iterator<Cache> caches() {
        return getInstance().cacheIterator();
    }

    private Iterator<Cache> cacheIterator() {
    	return cacheHolder.values().iterator();
    }

    private static class Trimmer implements Runnable {

        /**
         * Trim all the caches.
         */
        public void run() {

            try {

                Iterator<Cache> it = CacheManager.caches();

                while (it.hasNext()) {
                    Cache cache = (Cache) it.next();
                    trimCache(cache);
                }

            } catch (Exception e) {
            	String msg = "error trimming cache";
            	logger.log(Level.SEVERE, msg, e);
            }
        }

        private void trimCache(Cache cache) {

        	ConfigProperties properties = GlobalProperties.getConfigProperties();
        	int defaultTimeoutSecs = properties.getIntProperty("cache.timeoutsecs", 300);
        	
            int timeoutSecs = cache.getTimeoutSeconds();
            if (timeoutSecs < 5) {
                timeoutSecs = defaultTimeoutSecs;
            }
            long removeTime = System.currentTimeMillis() - defaultTimeoutSecs * 1000;

            int trimCount = 0;

            Iterator<Element> it = cache.elements();
            while (it.hasNext()) {
                Element element = (Element) it.next();
                if (removeTime > element.lastAccess()) {
                    it.remove();
                    trimCount++;
                }
            }

            if (trimCount > 0) {
            	if (logger.isLoggable(Level.FINE)) {
	                String m = "Cache[" + cache.getName() + "] trimmed by [" + trimCount + "] to ["
	                        + cache.size() + "]";
	                logger.fine(m);
            	}
            }

        }
    }
    
    private static class CacheCreate implements DoubleMapCreateValue<String, Cache> {

    	
    	
		public void postPut(Cache v) {
			// Do nothing
		}



		public Cache createValue(String name) {
			    
			ConfigProperties properties = GlobalProperties.getConfigProperties();
	        try {
	            //synchronized(monitor) {
	                String cacheClassName = properties.getProperty("cache."+name+".implementation");
	                if (cacheClassName == null){
	                    cacheClassName = properties.getProperty("cache.implementation");
	                }
	                
	                Cache cache = null;
	                if (cacheClassName != null){
	                    Class<?> cacheClass = Class.forName(cacheClassName);
	                    cache = (Cache)cacheClass.newInstance();
	                    
	                } else {
	                    cache = new MapCache();
	                }
	                 
	                cache.setName(name);
	                
	                String val = properties.getProperty("cache."+name+".validator");
	                if (val != null){
	                    Class<?> c = Class.forName(val);
	                    Validator validator = (Validator)c.newInstance();
	                    cache.setValidator(validator, null);
	                }
	                
	                String timeout = properties.getProperty("cache."+name+".timeout");
	                if (timeout != null){
	                	try {
	                		int secs = Integer.parseInt(timeout);
	                    	cache.setTimeoutSeconds(secs);
	                	} catch (Exception ex){
	                		logger.log(Level.SEVERE,null,ex);
	                	}
	                }
	                return cache;
	            //}
	        } catch (Exception e) {
	            String m = "Error creating cache ["+name+"]";
	            throw new RuntimeException(m, e);
	        }
		}
	}
}
