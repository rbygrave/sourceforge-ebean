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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.avaje.ebean.enhance.subclass.SubClassUtil;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.plugin.PluginDbConfig;

/**
 * Both caches and creates BeanDescriptors.
 */
public class BeanDescriptorBuilder {
	
	private final BeanManagerFactory beanManagerFactory;

	private final Map<String, BeanManager<?>> beanManagerMap = new HashMap<String, BeanManager<?>>();

	private final Map<String, BeanDescriptor<?>> beanDescriptorMap;
	
	private final Map<Class<?>, BeanTable> beanTableMap;

	private final List<BeanDescriptor<?>> beanDescriptorList;

	public BeanDescriptorBuilder(DeploymentManager dm, PluginDbConfig dbConfig, DeployUtil deployUtil) {

		this.beanManagerFactory = new BeanManagerFactory(dbConfig);
		
		BeanDescriptorFactory factory = new BeanDescriptorFactory(dm, dbConfig);
		factory.process();
		beanTableMap = factory.getBeanTables();
		beanDescriptorMap = factory.getBeanDescriptors();
		
		List<BeanDescriptor<?>> list = new ArrayList<BeanDescriptor<?>>(beanDescriptorMap.values());
		Collections.sort(list);
		beanDescriptorList = Collections.unmodifiableList(list);
	}
	
	/**
	 * Return the BeanDescriptors as a List.
	 */
	public List<BeanDescriptor<?>> getBeanDescriptors() {
		return beanDescriptorList;
	}

	/**
	 * Get the BeanDescriptor for a bean.
	 */
	@SuppressWarnings("unchecked")
	public <T> BeanDescriptor<T> get(Class<T> entityType) {

		return (BeanDescriptor<T>)get(entityType.getName());
	}

	/**
	 * Get the BeanDescriptor given a bean class name.
	 */
	public BeanDescriptor<?> get(String beanClassName) {

		beanClassName = SubClassUtil.getSuperClassName(beanClassName);
		return beanDescriptorMap.get(beanClassName);
	}

	@SuppressWarnings("unchecked")
	public <T> BeanManager<T> getBeanManager(Class<T> entityType) {

		return (BeanManager<T>)getBeanManager(entityType.getName());
	}
	
	public BeanTable getBeanTable(Class<?> cls){
		return beanTableMap.get(cls);
	}
	
	public BeanManager<?> getBeanManager(String beanClassName) {

		beanClassName = SubClassUtil.getSuperClassName(beanClassName);
		return beanManagerMap.get(beanClassName);
	}
	
	/**
	 * Initialise all the BeanDescriptors.
	 * <p>
	 * This occurs after all the BeanDescriptors have been created. This
	 * resolves circular relationships between BeanDescriptors.
	 * </p>
	 * <p>
	 * Also responsible for creating all the BeanManagers which contain
	 * the persister, listener etc.
	 * </p>
	 */
	public void initialiseAll() {
		
		
		// now that all the BeanDescriptors are in their map
		// we can initialise them which sorts out circular
		// dependencies for OneToMany and ManyToOne etc
		
		// PASS 1:
		// initialise the ID properties of all the beans
		// first (as they are needed to initialise the 
		// associated properties in the second pass).
		Iterator<BeanDescriptor<?>> initId = beanDescriptorMap.values().iterator();
		while (initId.hasNext()) {
			BeanDescriptor<?> d = initId.next();
			d.initialiseId();
		}
		
		// PASS 2:
		// now initialise all the associated properties
		Iterator<BeanDescriptor<?>> otherBeans = beanDescriptorMap.values().iterator();
		while (otherBeans.hasNext()) {
			BeanDescriptor<?> d = otherBeans.next();
			d.initialiseOther();
		}
		
		// create BeanManager for each non-embedded entity bean
		Iterator<BeanDescriptor<?>> it = beanDescriptorMap.values().iterator();
		while (it.hasNext()) {
			BeanDescriptor<?> d = it.next();
			if (!d.isEmbedded()){
				BeanManager<?> m = beanManagerFactory.create(d);
				beanManagerMap.put(d.getFullName(), m);
			}
		}
	}
	
}
