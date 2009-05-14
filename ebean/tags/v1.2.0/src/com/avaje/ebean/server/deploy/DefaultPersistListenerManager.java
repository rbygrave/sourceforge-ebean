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

import com.avaje.ebean.bean.BeanPersistListener;
import com.avaje.ebean.util.ParamTypeUtil;

/**
 * Default implementation for BeanFinderFactory.
 */
public class DefaultPersistListenerManager implements PersistListenerManager {

	HashMap<Class<?>, BeanPersistListener<?>> registerFor = new HashMap<Class<?>, BeanPersistListener<?>>();

	public int createListeners(List<Class<?>> finderClassList) {

		for (Class<?> cls : finderClassList) {
			Class<?> entityType = getEntityClass(cls);
			try {
				BeanPersistListener<?> beanListener = (BeanPersistListener<?>) cls.newInstance();
				registerFor.put(entityType, beanListener);
			} catch (Exception ex) {
				throw new PersistenceException(ex);
			}
		}
		
		return registerFor.size();
	}

	public int getRegisterCount() {
		return registerFor.size();
	}
	
	/**
	 * Return the BeanListener for a given entity type.
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanPersistListener<T> getListener(Class<T> entityType) {
		return (BeanPersistListener<T>)registerFor.get(entityType);
	}
	
    
	/**
	 * Find the entity class given the controller class.
	 * <p>
	 * This uses reflection to find the generics parameter type. 
	 * </p>
	 */	
    private Class<?> getEntityClass(Class<?> controller){
    	
		Class<?> cls = ParamTypeUtil.findParamType(controller, BeanPersistListener.class);
		if (cls == null){
			String msg = "Could not determine the entity class (generics parameter type) from "+controller+" using reflection.";
			throw new PersistenceException(msg);
		}
		return cls;
    }
}
