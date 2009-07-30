package com.avaje.ebean.server.cache;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the construction of caches.
 */
public class DefaultCacheHolder {

	private final ConcurrentHashMap<Class<?>, ServerCache> concMap = new ConcurrentHashMap<Class<?>, ServerCache>();
	
	private final HashMap<Class<?>, ServerCache> synchMap = new HashMap<Class<?>, ServerCache>();
	
	private final Object monitor = new Object();
	
	private final ServerCacheFactory cacheFactory;
	
	private ServerCacheOptions defaultOptions;
	
	/**
	 * Create with a cache factory and default cache options.
	 */
	public DefaultCacheHolder(ServerCacheFactory cacheFactory, ServerCacheOptions defaultOptions){
		
		this.cacheFactory = cacheFactory;
		this.defaultOptions = defaultOptions;		
	}
		
	/**
	 * Return the default cache options.
	 */
	public ServerCacheOptions getDefaultOptions() {
		return defaultOptions;
	}

	/**
	 * Set the default cache options.
	 */
	public void setDefaultOptions(ServerCacheOptions defaultOptions) {
		this.defaultOptions = defaultOptions;
	}


	/**
	 * Return the cache for a given bean type.
	 */
	public ServerCache getCache(Class<?> beanType) {
		
		ServerCache cache = concMap.get(beanType);
		if (cache != null){
			return cache;
		}
		synchronized (monitor) {
			cache = synchMap.get(beanType);
			if (cache == null){
				ServerCacheOptions options = getCacheOptions(beanType);
				cache = cacheFactory.createCache(beanType, options);
				synchMap.put(beanType, cache);
				concMap.put(beanType, cache);
			}
			return cache;
		}
	}

	/**
	 * Return true if there is an active cache for this bean type.
	 */
	public boolean isCaching(Class<?> beanType){
		return concMap.containsKey(beanType);
	}

	/**
	 * Return the cache options for a given bean type.
	 */
	private ServerCacheOptions getCacheOptions(Class<?> beanType) {
		
		// read the deployment annotation
//		Cache cache = beanType.getAnnotation(Cache.class);
//		
//		if (cache == null){
			return defaultOptions.copy();
//		}
		
//		return new ServerCacheOptions(defaultOptions, cache);
	}
	
}
