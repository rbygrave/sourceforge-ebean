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
package com.avaje.ebean.bean;

import java.util.Set;

/**
 * A no operation implementation of BeanPersistController. Objects extending this need to
 * only override the methods they want to.
 * <p>
 * A BeanPersistAdapter is either found automatically via class path search
 * or can be added programmatically via ServerConfiguration.addEntity().
 * </p>
 */
public abstract class BeanPersistAdapter<T> implements BeanPersistController<T> {

	/**
     * Returns true indicating normal processing should continue.
     */
    public boolean preDelete(BeanPersistRequest<T> request) {
        return true;
    }

    /**
     * Returns true indicating normal processing should continue.
     */
    public boolean preInsert(BeanPersistRequest<T> request) {
        return true;
    }

    /**
     * Returns true indicating normal processing should continue.
     */
    public boolean preUpdate(BeanPersistRequest<T> request) {
        return true;
    }

    /**
     * Does nothing by default.
     */
    public void postDelete(BeanPersistRequest<T> request) {
    }

    /**
     * Does nothing by default.
     */
    public void postInsert(BeanPersistRequest<T> request) {
    }

    /**
     * Does nothing by default.
     */
    public void postUpdate(BeanPersistRequest<T> request) {
    }

    /**
     * Does nothing by default.
     */
    public void postLoad(T bean, Set<String> includedProperties){
	}

    
}
