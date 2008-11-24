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
 * TransactionContext for internal use.
 */
public interface TransactionContext {

	/**
	 * Get the context specific to a bean type.
	 */
    public TransactionContextClass getClassContext(Class<?> beanType);

    /**
     * Add the bean to the TransactionContext. If forceReplace is true then this
     * bean is added even if a matching bean is already loaded in the
     * TransactionContext.
     * 
     * <p>
     * Returns true if the bean was added and false if a matching bean was
     * already loaded in the TransactionContext.
     * </p>
     */
    public boolean add(EntityBean entityBean, Object id, boolean forceReplace);

    /**
     * Set an object into the PersistanceContext.
     */
    public void set(Class<?> beanType, Object uid, EntityBean bean);

    /**
     * Return an object given its type and unique id.
     */
    public EntityBean get(Class<?> beanType, Object uid);
        
    /**
     * Clear all the references.
     */
    public void clear();

    /**
     * Clear all the references for a given type of entity bean.
     */
    public void clear(Class<?> beanType);

    /**
     * Clear the reference to a specific entity bean.
     */
    public void clear(Class<?> beanType, Object uid);

}
