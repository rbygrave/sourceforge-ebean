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
package com.avaje.ebean.server.transaction;

import java.util.HashMap;

import com.avaje.ebean.common.EntityBean;
import com.avaje.ebean.enhance.subclass.SubClassUtil;
import com.avaje.ebean.server.core.PersistenceContext;

/**
 * Default implementation of PersistenceContext.
 * <p>
 * Ensures only one instance of a bean is used according to its type and unique
 * id.
 * </p>
 * <p>
 * PersistenceContext lives on a Transaction and as such is expected to only have
 * a single thread accessing it at a time. This is not expected to be used concurrently.
 * </p>
 * <p>
 * Duplicate beans are ones having the same type and unique id value. These are
 * considered duplicates and replaced by the bean instance that was already
 * loaded into the PersistanceContext.
 * </p>
 */
public final class DefaultPersistenceContext implements PersistenceContext {

   
    /**
     * Map used hold caches. One cache per bean type.
     */
    final HashMap<String,ClassContext> typeCache = new HashMap<String,ClassContext>();

    /**
     * Create a new PersistanceContext.
     */
    public DefaultPersistenceContext() {
    }

    /**
     * Add the bean to the PersistanceContext. If forceReplace is true then this
     * bean is added even if a matching bean is already loaded into the
     * PersistanceContext.
     * 
     * <p>
     * Returns true if the bean was added and false if a matching bean was
     * already loaded into the PersistanceContext.
     * </p>
     */
    public boolean add(EntityBean entityBean, Object id, boolean forceReplace) {

    	ClassContext classMap = getClassContext(entityBean.getClass());
        if (forceReplace || !classMap.containsKey(id)) {
            classMap.put(id, entityBean);
            return true;
        }
    	
        return false;
    }

    /**
     * Set an object into the PersistanceContext.
     */
    public void set(Class<?> beanType, Object id, EntityBean bean) {
    	getClassContext(beanType).put(id, bean);
    }

    /**
     * Return an object given its type and unique id.
     */
    public EntityBean get(Class<?> beanType, Object id) {

    	return getClassContext(beanType).get(id);
    }

    /**
     * Clear the PersistenceContext.
     */
    public void clear() {
        typeCache.clear();
    }

    public void clear(Class<?> beanType) {
    	ClassContext classMap = typeCache.get(beanType.getName());
        if (classMap != null) {
        	classMap.clear();	
        }
    }

    public void clear(Class<?> beanType, Object id) {
    	ClassContext classMap = typeCache.get(beanType.getName());
        if (classMap != null && id != null) {
            //id = getUid(beanType, id);
            classMap.remove(id);
        }
    }
   
    public ClassContext getClassContext(Class<?> beanType) {

    	// strip off $$EntityBean.. suffix...
    	String clsName = SubClassUtil.getSuperClassName(beanType.getName());
    	
    	ClassContext classMap = typeCache.get(clsName);
        if (classMap == null) {            
            classMap = new ClassContext();
            typeCache.put(clsName, classMap);
        }
        return classMap;
    }

    private static class ClassContext {
    	
    	HashMap<Object,EntityBean> map = new HashMap<Object, EntityBean>();
        
    	public EntityBean get(Object id){
    		return map.get(id);
    	}
    	public void put(Object id, EntityBean b){
    		map.put(id, b);
    	}
    	public boolean containsKey(Object id){
    		return map.containsKey(id);
    	}
    	public void clear(){
    		map.clear();
    	}
    	public EntityBean remove(Object id){
    		return map.remove(id);
    	}
    }
    
}
