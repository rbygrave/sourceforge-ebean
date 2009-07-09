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
package com.avaje.ebean.server.deploy;

import java.util.HashMap;
import java.util.List;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.BeanPersistAdapter;
import com.avaje.ebean.bean.BeanPersistController;

/**
 * Default implementation for creating BeanControllers.
 */
public class DefaultPersistControllerManager implements PersistControllerManager {
 
    HashMap<Class<?>,BeanPersistController<?>> registerFor = new HashMap<Class<?>,BeanPersistController<?>>();
    
    public int createControllers(List<Class<?>> controllerList){
    	
    	for (Class<?> cls : controllerList) {
			Class<?> entityType = getEntityClass(cls);
    		try {
	    		BeanPersistController<?> controller = (BeanPersistController<?>)cls.newInstance();
    			registerFor.put(entityType, controller);	    		
	        } catch (Exception ex){
	            throw new PersistenceException(ex);
	        }
		}
    	
    	return registerFor.size();
    }
	
    public int getRegisterCount() {
		return registerFor.size();
	}
	
    /**
     * Return the BeanPersistController for a given entity type.
     */
    @SuppressWarnings("unchecked")
	public <T> BeanPersistController<T> getController(Class<T> entityType){
    	return (BeanPersistController<T>)registerFor.get(entityType);
    }
    
	/**
	 * Find the entity class given the controller class.
	 * <p>
	 * This uses reflection to find the generics parameter type. 
	 * </p>
	 */	
    private Class<?> getEntityClass(Class<?> controller){
    	
		Class<?> cls = ParamTypeUtil.findParamType(controller, BeanPersistController.class);
		if (cls == null){
			cls = ParamTypeUtil.findParamType(controller, BeanPersistAdapter.class);
		}
		if (cls == null){
			String msg = "Could not determine the entity class (generics parameter type) from "+controller+" using reflection.";
			throw new PersistenceException(msg);
		}
		return cls;
    }
}
