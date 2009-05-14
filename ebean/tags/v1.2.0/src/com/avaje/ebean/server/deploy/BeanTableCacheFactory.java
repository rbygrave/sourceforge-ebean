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

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.lib.cache.DoubleMap;
import com.avaje.ebean.server.lib.cache.DoubleMapCreateValue;

/**
 * Both caches and creates BeanTables.
 */
public class BeanTableCacheFactory {

	private final BeanTableFactory factory;

	private final DoubleMap<String, BeanTable> map = new DoubleMap<String, BeanTable>(new Create());

	public BeanTableCacheFactory(DeployUtil deployUtil) {
		factory = new BeanTableFactory(deployUtil);
	}

	public BeanTable get(Class<?> beanClz) {
		// Find parent if instance of $$EntityBean
		//String descKey = SubClassUtil.getSuperClassName(beanClz.getName());
		return map.get(beanClz.getName());
	}

	private class Create implements DoubleMapCreateValue<String, BeanTable> {

		public BeanTable createValue(String descKey) {
			try {
				Class<?> cls = Class.forName(descKey);
				return factory.createBeanTable(cls);
			} catch (PersistenceException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new PersistenceException(ex);
			}
		}

		public void postPut(BeanTable v) {
			// Do nothing.
		}

	}
}
