package com.avaje.ebean.server.cache;


/**
 * Default implementation of CacheFactory.
 */
public class DefaultServerCacheFactory implements ServerCacheFactory {

	public ServerCache createCache(Class<?> beanType, ServerCacheOptions cacheOptions) {
		
		return new DefaultServerCache("Bean:"+beanType, cacheOptions);	
	}
	
}
