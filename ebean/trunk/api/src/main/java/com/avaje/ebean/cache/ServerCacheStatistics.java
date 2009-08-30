/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebean.cache;

public class ServerCacheStatistics {

	protected String cacheName;

	protected int maxSize;

	protected int size;
	
	protected int hitCount;
	
	protected int missCount;

	public String getCacheName() {
		return cacheName;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public int getHitCount() {
		return hitCount;
	}

	public void setHitCount(int hitCount) {
		this.hitCount = hitCount;
	}

	public int getMissCount() {
		return missCount;
	}

	public void setMissCount(int missCount) {
		this.missCount = missCount;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}

	/**
	 * Returns an int from 0 to 100 (percentage) for the hit ratio.
	 * <p>
	 * A hit ratio of 100 means every get request against the cache
	 * hits an entry.
	 * </p>
	 */
	public int getHitRatio() {
		int totalCount = hitCount + missCount;
		if (totalCount == 0){
			return 0;
		} else {
			return hitCount * 100 / totalCount;
		}
	}
	
}
