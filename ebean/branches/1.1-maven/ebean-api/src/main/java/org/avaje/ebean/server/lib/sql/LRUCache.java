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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.lib.log.LogFactory;

/**
 * A Least recently used cache with a given maximum size and cleanup helper.
 * Least recently used implying that the least recently used entries are the ones
 * that are removed first from the cache when it hits its maximum size.
 * <p>
 * The cleanup helper is an optional object that knows how to cleanup the entries
 * as they are removed from the cache.
 * </p>
 */
public class LRUCache<K,V> extends LinkedHashMap<K,V> {

    static final long serialVersionUID = -3096406924865550697L;

    private static final Logger logger = LogFactory.get(LRUCache.class);
    
    /**
	 * The traceLevel for the cache.
	 */
	int traceLevel = 0;

	/**
	 * The total number of entries removed from this cache.
	 */
	int removeCounter = 0;

	/**
	 * The number of get hits.
	 */
	int hitCounter = 0;

	/** 
	 * The number of get() misses.
	 */
	int missCounter = 0;

	/**
	 * The number of puts into this cache.
	 */
	int putCounter = 0;

	/**
	 * The maximum size of the cache.  When this is exceeded the oldest entry is removed.
	 */
	int maxSize;
	
	/**
	 * If this has been set, it is called to do any special cleanup of the entry.
	 * For example, in the PreparedStatement cache this closes the PreparedStatement properly.
	 */
	LRUCacheCleanup cacheCleanup;

	/**
	 * The name of the cache, for tracing purposes.
	 */
	String cacheName;

	/**
	 * Object that holds the synchronization lock for this cache.
	 */
	Object monitor = new Object();
	
	/** 
	 * Create a Least recently used cache with a given maximum size and cleanup helper.
	 * Least recently used implying that the least recently used entries are the ones
	 * that are removed first from the cache when it hits its maximum size.
	 * <p>
	 * maxCacheSize is the maximum size the cache will grow to before it starts to remove
	 * the oldest entries from the cache.
	 * <p>
	 * When an entry is removed it can be passed to the cacheCleanup helper which knows how
	 * to clean up that type of entry.  For example, cleanup PreparedStatements.  The cacheCleanup
	 * can be null for cached objects that don't require any cleanup.
	 *
	 * @param maxCacheSize the maximum size of this cache.
	 *
	 * @param cacheCleanup the helper object that knows how to cleanup the entries just before
	 *        they are removed.  Note that it doesn't actually remove the entries.  Also it can
	 *        be null.
	 */
	public LRUCache(String cacheName, int maxCacheSize, LRUCacheCleanup cacheCleanup) {
		this(cacheName, 128, 0.75f, maxCacheSize, cacheCleanup);
	}

	/**
	 * Additionally specify an initialSize and loadFactor for the cache.
	 */
	public LRUCache(String cacheName, int initialSize, float loadFactor, int maxCacheSize, LRUCacheCleanup cacheCleanup) {

		// note = access ordered list.  This is what gives it the LRU order
		super(initialSize, loadFactor, true);
		this.cacheName = cacheName;
		this.maxSize = maxCacheSize;
		this.cacheCleanup = cacheCleanup;
	}

	/**
	 * returns the current maximum size of the cache.
	 */
	public int getMaxSize() {
		return maxSize;
	}	

	/**
	 * sets the new maximum size of the cache.
	 */
	public void setMaxSize(int newMaxCacheSize) {
		if (newMaxCacheSize != maxSize) {
			this.maxSize = newMaxCacheSize;
			if (maxSize < size()) {
				trim(maxSize);
			}
		}
	}	

	/**
	 * Gets the hit ratio.  A number between 0 and 100 indicating the number of
	 * hits to misses.  A number approaching 100 is desirable.
	 */
	public int getHitRatio() {
		if (hitCounter == 0) {
			return 0;
		} else {
			return hitCounter*100/(hitCounter+missCounter);
		}
	}

	/**
	 * The total number of hits against this cache.
	 */
	public int getHitCounter() {
		return hitCounter;
	}

	/**
	 * The total number of misses against this cache.
	 */
	public int getMissCounter() {
		return missCounter;
	}

	/**
	 * The total number of puts against this cache.
	 */
	public int getPutCounter() {
		return putCounter;
	}

    /**
     * Set the trace level.
     */
	public void setTraceLevel(int traceLevel) {
		this.traceLevel = traceLevel;
	}

    /**
     * return the trace level.
     */
	public int getTraceLevel() {
		return traceLevel;
	}

	/**
	 * additionally maintains hit and miss statistics.
	 */
	public V get(Object key) {

		synchronized(monitor) {
			V o = super.get(key);
			if (o == null) {
				missCounter++;
			} else {
				hitCounter++;
			}
			return o;
		}
	}

	/**
	 * additionally maintains hit and miss statistics.
	 */
	public V remove(Object key) {

		synchronized(monitor) {
			V o = super.remove(key);
			if (o == null) {
				missCounter++;
			} else {
				hitCounter++;
			}
			return o;
		}
	}

	/**
	 * additionally maintains put counter statistics.
	 */
	public V put(K key, V value) {

		synchronized(monitor) {
			putCounter++;
			return super.put(key, value);
		}
	}

	/**
	 * Trim the number of entries in the cache to the specified number.
	 */
	public void trim(int numberOfEntries) {
		
		synchronized(monitor) {

			int removeCount = size() - numberOfEntries;
			if (removeCount > 0) {
				if (logger.isLoggable(Level.FINEST)){
					logger.finer(" trim ["+removeCount+"] from cache");
				}
				Iterator<?> iterator = entrySet().iterator();
				for (int i=0; i < removeCount ; i++){
					Object value = iterator.next();
					iterator.remove(); 

					if (logger.isLoggable(Level.FINEST)){
						logger.finer(" trim ["+value+"] from cache");
					}
					if (cacheCleanup != null) {
						cacheCleanup.cleanupEldest(value);
					}
				}
			} else {
				//dp("removeCount > 0 ; "+removeCount);
			}	
		}
	}


	/**
	 * will check to see if we need to remove entries and
	 * if so call the cacheCleanup.cleanupEldestLRUCacheEntry() if
	 * one has been set.
	 */
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		synchronized(monitor) {
			if (size() > maxSize) {
	
				if (cacheCleanup != null) {
					cacheCleanup.cleanupEldest(eldest.getValue());
				}
				removeCounter++;
				if (traceLevel > 0) {
					logger.fine(" removing "+eldest.getKey());
				}
				return true;
	
			} else {
				return false;
			}
		}
	}

}
 
