package com.avaje.ebeaninternal.server.core;

/**
 * Options for building references such as useCache and readOnly.
 */
public class ReferenceOptions {
	
	private final boolean useCache;

	private final String warmingQuery;
	
	/**
	 * Construct with options.
	 */
	public ReferenceOptions(boolean useCache, String warmingQuery) {
		this.useCache = useCache;
		this.warmingQuery = warmingQuery;
	}

	/**
	 * Return true if this should use a cache for lazy loading.
	 */
	public boolean isUseCache() {
		return useCache;
	}

	/**
	 * Return the query used to warm the cache.
	 */
	public String getWarmingQuery() {
		return warmingQuery;
	}	

	
}
