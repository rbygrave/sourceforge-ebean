package com.avaje.ebean.server.cache;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class BasicCacheManager  implements CacheManager {

	private final ConcurrentHashMap<String, Cache> concMap = new ConcurrentHashMap<String, Cache>();
	private final HashMap<String, Cache> synchMap = new HashMap<String, Cache>();
	
	private final String monitor = new String();
	
	private Cache getCache(Class<?> beanType, String cacheType) {
		
		String key = beanType.getCanonicalName()+cacheType;
		Cache cache = concMap.get(key);
		if (cache != null){
			return cache;
		}
		synchronized (monitor) {
			cache = synchMap.get(key);
			if (cache == null){
				cache = new BasicCache();
				synchMap.put(key, cache);
				concMap.put(key, cache);
			}
			return cache;
		}
	}

	public Cache getBeanCache(Class<?> beanType) {
		return getCache(beanType, ":bean");
	}
	
	public Cache getQueryCache(Class<?> beanType) {
		return getCache(beanType, ":query");
	}

	
}
