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
package com.avaje.ebean.server.util;

import com.avaje.ebean.server.deploy.ManyType;

/**
 * Parameters used to create the specific Map Set or List object.
 */
public class BeanCollectionParams {

	Boolean ordered;

	private final int initialCapacity;

	private final ManyType manyType;

	/**
	 * Construct without a specific capacity.
	 */
	public BeanCollectionParams(ManyType manyType) {
		this.manyType = manyType;
		this.initialCapacity = -1;
	}

	/**
	 * Construct with an explicit initialCapacity.
	 */
	public BeanCollectionParams(ManyType manyType, int initialCapacity, boolean modifyListening) {
		this.manyType = manyType;
		this.initialCapacity = initialCapacity;
	}

	/**
	 * Return the initial capacity for the object.
	 */
	public int getInitialCapacity() {
		return initialCapacity;
	}

	/**
	 * HashSet and HashMap do not provide obvious iteration order. Use this flag
	 * to indicate that you want HashSet over LinkedHashSet or HashMap over
	 * LinkedHashMap.
	 */
	public Boolean getOrdered() {
		return ordered;
	}

	/**
	 * Use this flag to indicate that you want HashSet over LinkedHashSet or
	 * HashMap over LinkedHashMap.
	 */
	public void setOrdered(Boolean ordered) {
		this.ordered = ordered;
	}

	/**
	 * Return the type Map Set or List.
	 */
	public ManyType getManyType() {
		return manyType;
	}

}
