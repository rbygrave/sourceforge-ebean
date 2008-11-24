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

import com.avaje.ebean.server.core.PersistRequest;

/**
 * A no operation implementation of BeanController. Objects extending this need to
 * only override the methods they want to.
 */
public abstract class BeanControllerAdapter implements BeanController {

	/**
	 * The types of entity bean this is the controller for.
	 */
    public abstract Class<?>[] registerFor();

	/**
     * Returns true indicating normal processing should continue.
     */
    public boolean preDelete(PersistRequest request) {
        return true;
    }

    /**
     * Returns true indicating normal processing should continue.
     */
    public boolean preInsert(PersistRequest request) {
        return true;
    }

    /**
     * Returns true indicating normal processing should continue.
     */
    public boolean preUpdate(PersistRequest request) {
        return true;
    }

    /**
     * Does nothing by default.
     */
    public void postDelete(PersistRequest request) {
    }

    /**
     * Does nothing by default.
     */
    public void postInsert(PersistRequest request) {
    }

    /**
     * Does nothing by default.
     */
    public void postUpdate(PersistRequest request) {
    }

    /**
     * Does nothing by default.
     */
    public void postLoad(Object bean, Set<String> includedProperties){
	}

    
}
