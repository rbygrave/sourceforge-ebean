/**
 * Copyright (C) 2009  Robin Bygrave
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
package com.avaje.ebean.bean;

import com.avaje.ebean.Query;



/**
 * Loads a entity bean collection.
 * <p>
 * Typically invokes lazy loading for a single or batch of collections. 
 * </p>
 */
public interface BeanCollectionLoader {

	/**
	 * Return the name of the associated EbeanServer.
	 */
	public String getName();
	
	/**
	 * Invoke the lazy loading for this bean collection.
	 */
	public void loadMany(BeanCollection<?> collection, boolean onlyIds);

	/**
	 * Configure a filter if one is specified for this path.
	 */
	public void configureFilter(Query<?> query);
}
