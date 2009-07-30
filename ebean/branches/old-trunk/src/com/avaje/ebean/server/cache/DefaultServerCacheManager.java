package com.avaje.ebean.server.cache;


/**
 * Manages the bean and query caches. 
 */
public class DefaultServerCacheManager implements ServerCacheManager {

	private final DefaultCacheHolder beanCache;

	private final DefaultCacheHolder queryCache;

	
	/**
	 * Create with a cache factory and default cache options.
	 */
	public DefaultServerCacheManager(ServerCacheFactory cacheFactory, ServerCacheOptions defaultBeanOptions, ServerCacheOptions defaultQueryOptions) {
		this.beanCache = new DefaultCacheHolder(cacheFactory, defaultBeanOptions);
		this.queryCache = new DefaultCacheHolder(cacheFactory, defaultQueryOptions);
	}
	
	/**
	 * Return the query cache for a given bean type.
	 */
	public ServerCache getQueryCache(Class<?> beanType) {
		
		return queryCache.getCache(beanType);
	}
	
	/**
	 * Return the bean cache for a given bean type.
	 */
	public ServerCache getBeanCache(Class<?> beanType) {
		return beanCache.getCache(beanType);
	}

	/**
	 * Return true if there is an active cache for the given bean type.
	 */
	public boolean isBeanCaching(Class<?> beanType) {
		
		return beanCache.isCaching(beanType);
	}

	
}
