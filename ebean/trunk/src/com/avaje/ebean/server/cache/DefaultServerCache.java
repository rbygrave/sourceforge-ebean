package com.avaje.ebean.server.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The default cache implementation.
 * <p>
 * It is base on ConcurrentHashMap with periodic trimming using a TimerTask.
 * The periodic trimming means that an LRU list does not have to be maintained.
 * </p>
 */
public class DefaultServerCache implements ServerCache {

	private static final Logger logger = Logger.getLogger(DefaultServerCache.class.getName());

	private final ConcurrentHashMap<Object, CacheEntry> map = new ConcurrentHashMap<Object, CacheEntry>();

	private final Object monitor = new Object();

	private final String name;

	private Timer timer;

	private int maxSize;

	private long trimFrequency;

	private long maxIdleTime;

	private long maxTimeToLive;

	public DefaultServerCache(String name, ServerCacheOptions options) {
		this(name, options.getMaxSize(), options.getMaxIdleTime(), options.getMaxIdleTime());
	}

	public DefaultServerCache(String name, int maxSize, long maxIdleTime, long maxTimeToLive) {
		this.name = name;
		this.maxSize = maxSize;
		this.maxIdleTime = maxIdleTime;
		this.maxTimeToLive = maxTimeToLive;
		this.trimFrequency = 1000*60;

		resetTimer();
	}

	protected void finalize() throws Throwable {
		if (timer != null) {
			timer.cancel();
		}
	}

	private void resetTimer() {
		synchronized (monitor) {
			if (timer != null) {
				timer.cancel();
			}
			timer = new Timer(true);
			timer.schedule(new TrimTask(), trimFrequency, trimFrequency);
		}
	}

	public ServerCacheOptions getOptions() {
		synchronized (monitor) {
			ServerCacheOptions o = new ServerCacheOptions();
			o.setMaxIdleTime(maxIdleTime);
			o.setMaxSize(maxSize);
			o.setMaxTimeToLive(maxTimeToLive);
			return o;
		}
	}
	
	public void setOptions(ServerCacheOptions o) {
		synchronized (monitor) {
			maxIdleTime = o.getMaxIdleTime();
			maxSize = o.getMaxSize();
			maxTimeToLive = o.getMaxTimeToLive();
		}
	}
	
	
	/**
	 * Return the max cache size.
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * Set the max cache size.
	 */
	public void setMaxSize(int maxSize) {
		synchronized (monitor) {
			this.maxSize = maxSize;
		}
	}

	/**
	 * Return the max idle time.
	 */
	public long getMaxIdleTime() {
		return maxIdleTime;
	}

	/**
	 * Set the max idle time.
	 */
	public void setMaxIdleTime(long maxIdleTime) {
		synchronized (monitor) {
			this.maxIdleTime = maxIdleTime;
		}
	}

	/**
	 * Return the maximum time to live.
	 */
	public long getMaxTimeToLive() {
		return maxTimeToLive;
	}

	/**
	 * Set the maximum time to live.
	 */
	public void setMaxTimeToLive(long maxTimeToLive) {
		synchronized (monitor) {
			this.maxTimeToLive = maxTimeToLive;
		}
	}
	
	/**
	 * Return the frequency trimming occurs on the cache.
	 */
	public long getTrimFrequency() {
		return trimFrequency;
	}

	/**
	 * Set the frequency that trimming occurs on the cache.
	 */
	public void setTrimFrequency(long trimFrequency) {
		synchronized (monitor) {
			this.trimFrequency = trimFrequency;
			resetTimer();
		}
	}

	/**
	 * Return the name of the cache.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Clear the cache.
	 */
	public void clear() {
		map.clear();
	}

	/**
	 * Return a value from the cache.
	 */
	public Object get(Object key) {
		// get value and increment the last access time
		CacheEntry entry = map.get(key);
		return entry == null ? null : entry.getValue();
	}

	/**
	 * Put a value into the cache.
	 */
	public Object put(Object key, Object value) {
		// put new entry with create time
		CacheEntry entry = map.put(key, new CacheEntry(key, value));
		return entry == null ? null : entry.getValue();
	}

	/**
	 * Remove an entry from the cache.
	 */
	public Object remove(Object key) {
		CacheEntry entry = map.remove(key);
		return entry == null ? null : entry.getValue();
	}

	/**
	 * Return the number of elements in the cache.
	 */
	public int size() {
		return map.size();
	}

	private Iterator<CacheEntry> cacheEntries() {
		return map.values().iterator();
	}

	/**
	 * The task used to periodically trim the cache.
	 */
	private class TrimTask extends TimerTask {

		@Override
		public void run() {

			long startTime = System.currentTimeMillis();
			
			if (logger.isLoggable(Level.FINER)){
				logger.finer("trimming cache " + name);
			}
			
			int trimmedByIdle = 0;
			int trimmedByTTL = 0;
			int trimmedByLRU = 0;

			boolean trimMaxSize = maxSize > 0 && maxSize < size();

			ArrayList<CacheEntry> activeList = new ArrayList<CacheEntry>();

			long idleExpire = System.currentTimeMillis() - maxIdleTime;
			long ttlExpire = System.currentTimeMillis() - maxTimeToLive;

			Iterator<CacheEntry> it = cacheEntries();
			while (it.hasNext()) {
				CacheEntry cacheEntry = it.next();
				if (maxIdleTime > 0 && idleExpire > cacheEntry.getLastAccessTime()) {
					it.remove();
					trimmedByIdle++;

				} else if (maxTimeToLive > 0 && ttlExpire > cacheEntry.getCreateTime()) {
					it.remove();
					trimmedByTTL++;

				} else if (trimMaxSize) {
					activeList.add(cacheEntry);
				}
			}

			if (trimMaxSize) {
				trimmedByLRU = activeList.size() - maxSize;

				if (trimmedByLRU > 0) {
					// sort into last access time ascending
					Collections.sort(activeList);
					for (int i = maxSize; i < activeList.size(); i++) {
						// remove if still in the cache
						map.remove(activeList.get(i).getKey());
					}
				}
			}
			
			long exeTime = System.currentTimeMillis() - startTime;
			
			if (logger.isLoggable(Level.FINE)){
				logger.fine("Executed trim of cache " + name + " in ["+exeTime
					+"]millis  idle[" + trimmedByIdle + "] timeToLive[" 
					+ trimmedByTTL + "] accessTime["
					+ trimmedByLRU + "]");
			}

		}

	}

	/**
	 * Wraps the values to additionally hold createTime and lastAccessTime.
	 */
	private class CacheEntry implements Comparable<CacheEntry> {

		private final Object key;
		private final Object value;
		private final long createTime;
		private Long lastAccessTime;

		public CacheEntry(Object key, Object value) {
			this.key = key;
			this.value = value;
			this.createTime = System.currentTimeMillis();
			this.lastAccessTime = Long.valueOf(createTime);
		}

		public int compareTo(CacheEntry o) {
			return lastAccessTime.compareTo(o.getLastAccessLong());
		}

		public Object getKey() {
			return key;
		}

		public Object getValue() {
			// object assignment is atomic
			this.lastAccessTime = Long.valueOf(System.currentTimeMillis());
			return value;
		}

		public long getCreateTime() {
			return createTime;
		}

		public long getLastAccessTime() {
			return lastAccessTime.longValue();
		}

		public Long getLastAccessLong() {
			return lastAccessTime;
		}

	}
}
