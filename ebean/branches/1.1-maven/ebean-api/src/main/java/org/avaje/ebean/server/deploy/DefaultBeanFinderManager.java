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
package org.avaje.ebean.server.deploy;

import java.util.HashMap;
import java.util.List;

import javax.persistence.PersistenceException;

import org.avaje.ebean.bean.BeanFinder;

/**
 * Default implementation for BeanFinderFactory.
 */
public class DefaultBeanFinderManager implements BeanFinderManager {

	HashMap<Class<?>, BeanFinder> registerFor = new HashMap<Class<?>, BeanFinder>();

	public int createBeanFinders(List<Class<?>> finderClassList) {

		for (Class<?> cls : finderClassList) {
			try {
				BeanFinder beanFinder = (BeanFinder) cls.newInstance();
				
				Class<?>[] entityTypes = beanFinder.registerFor();
				if (entityTypes != null){
					for (Class<?> entityType : entityTypes) {
						registerFor.put(entityType, beanFinder);
					}
				}

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
	 * Return the BeanFinder for a given entity type.
	 */
	public BeanFinder getBeanFinder(Class<?> entityType) {
		return registerFor.get(entityType);
	}
}
