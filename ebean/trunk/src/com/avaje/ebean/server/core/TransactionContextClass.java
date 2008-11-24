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
package com.avaje.ebean.server.core;

import com.avaje.ebean.bean.EntityBean;

/**
 * Part of the TransactionContext for a single bean type.
 */
public interface TransactionContextClass {

	/**
	 * Get a bean from the context.
	 */
	public EntityBean get(Object id);
	
	/**
	 * Put a bean into the context.
	 */
	public void put(Object id, EntityBean b);
	
	/**
	 * Return true if a bean is already in the context for this id.
	 */
	public boolean containsKey(Object id);
	
	/**
	 * Clear the context for this bean type.
	 */
	public void clear();
	
	/**
	 * Remove a specific bean from the context.
	 * @param id
	 * @return
	 */
	public EntityBean remove(Object id);
}
