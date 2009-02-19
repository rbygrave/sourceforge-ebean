package com.avaje.ebean.server.core;

/**
 * Options for controlling how references are built.
 */
public class ReferenceOptions {

	boolean readOnly;
	
	boolean cache;

	boolean noCache;
	
	/**
	 * Return true if the cache should be used otherwise false.
	 */
	public boolean isUseCache(boolean beanDefault){
		if (noCache){
			return false;
		}
		if (cache){
			return true;
		}
		noCache = !beanDefault;
		cache = beanDefault;
		return beanDefault;
	}

	public boolean isCache() {
		return cache;
	}

	public void setCache(boolean cache) {
		this.cache = cache;
	}

	public boolean isNoCache() {
		return noCache;
	}

	public void setNoCache(boolean noCache) {
		this.noCache = noCache;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	
}
