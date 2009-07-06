/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean.server.cache;

/**
 * Options for controlling a cache.
 * <p>
 * Note that there is one cache per bean type for both bean caches and query result caches.
 * So the maxSize for example means maximum number of beans for that type in the cache.
 * </p>
 */
public class ServerCacheOptions {

	private int maxSize;
	private long maxIdleTime;
	private long maxTimeToLive;

	public ServerCacheOptions() {
		
	}
	
	/**
	 * Create merging default options with the deployment specified ones.
	 */
	public ServerCacheOptions(ServerCacheOptions d, com.avaje.ebean.annotation.Cache c){
		this.maxSize = c.maxSize() != 0 ? c.maxSize() : d.getMaxSize();
		this.maxIdleTime = c.maxIdleTime() != 0 ? c.maxIdleTime() : d.getMaxIdleTime();
		this.maxTimeToLive = c.maxTimeToLive() != 0 ? c.maxTimeToLive() : d.getMaxIdleTime();
	}
	
	/**
	 * Return a copy of this object.
	 */
	public ServerCacheOptions copy() {
		
		ServerCacheOptions copy = new ServerCacheOptions();
		copy.maxSize = maxSize;
		copy.maxIdleTime = maxIdleTime;
		copy.maxTimeToLive = maxTimeToLive;
		
		return copy;
	}
	
	/**
	 * Return the maximum cache size.
	 */
	public int getMaxSize() {
		return maxSize;
	}
	
	/**
	 * Set the maximum cache size.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
	
	/**
	 * Return the maximum idle time.
	 */
	public long getMaxIdleTime() {
		return maxIdleTime;
	}
	
	/**
	 * Set the maximum idle time.
	 */
	public void setMaxIdleTime(long maxIdleTime) {
		this.maxIdleTime = maxIdleTime;
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
		this.maxTimeToLive = maxTimeToLive;
	}
	
}
