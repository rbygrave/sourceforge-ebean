package com.avaje.ebean.server.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import com.avaje.ebean.annotation.CacheTuning;
import com.avaje.ebean.cache.ServerCache;
import com.avaje.ebean.cache.ServerCacheFactory;
import com.avaje.ebean.cache.ServerCacheOptions;

/**
 * Manages the construction of caches.
 */
public class DefaultCacheHolder {

	private final ConcurrentHashMap<Class<?>, ServerCache> concMap = new ConcurrentHashMap<Class<?>, ServerCache>();

	private final HashMap<Class<?>, ServerCache> synchMap = new HashMap<Class<?>, ServerCache>();

	private final Object monitor = new Object();

	private final ServerCacheFactory cacheFactory;

	private final ServerCacheOptions defaultOptions;

	private final boolean useBeanTuning;

	/**
	 * Create with a cache factory and default cache options.
	 * 
	 * @param cacheFactory
	 *            the factory for creating the cache
	 * @param defaultOptions
	 *            the default options for tuning the cache
	 * @param useBeanTuning
	 *            if true then use the bean class specific tuning. This is
	 *            generally false for the query cache.
	 */
	public DefaultCacheHolder(ServerCacheFactory cacheFactory,
			ServerCacheOptions defaultOptions, boolean useBeanTuning) {

		this.cacheFactory = cacheFactory;
		this.defaultOptions = defaultOptions;
		this.useBeanTuning = useBeanTuning;
	}

	/**
	 * Return the default cache options.
	 */
	public ServerCacheOptions getDefaultOptions() {
		return defaultOptions;
	}

	/**
	 * Return the cache for a given bean type.
	 */
	public ServerCache getCache(Class<?> beanType) {

		ServerCache cache = concMap.get(beanType);
		if (cache != null) {
			return cache;
		}
		synchronized (monitor) {
			cache = synchMap.get(beanType);
			if (cache == null) {
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
	public boolean isCaching(Class<?> beanType) {
		return concMap.containsKey(beanType);
	}

	public void clearAll() {
		Iterator<ServerCache> it = concMap.values().iterator();
		while (it.hasNext()) {
			ServerCache serverCache = it.next();
			serverCache.clear();
		}
	}

	/**
	 * Return the cache options for a given bean type.
	 */
	private ServerCacheOptions getCacheOptions(Class<?> beanType) {

		if (useBeanTuning) {
			// read the deployment annotation
			CacheTuning cacheTuning = beanType.getAnnotation(CacheTuning.class);
			if (cacheTuning != null) {
				ServerCacheOptions o = new ServerCacheOptions(cacheTuning);
				o.applyDefaults(defaultOptions);
				return o;
			}
		}

		return defaultOptions.copy();

	}

}
