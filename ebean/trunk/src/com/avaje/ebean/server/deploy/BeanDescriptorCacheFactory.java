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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.enhance.subclass.SubClassUtil;
import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;
import com.avaje.ebean.server.deploy.parse.AnnotationBeanTable;
import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.lib.log.LogFactory;

/**
 * Both caches and creates BeanDescriptors.
 */
public class BeanDescriptorCacheFactory {

	private static final Logger logger = LogFactory.get(BeanDescriptorCacheFactory.class);

	private final PluginDbConfig dbConfig;

	private final DeployUtil deployUtil;
	
	private final BeanDescriptorFactory factory;

	private final BeanManagerFactory managerFactory;

	private final HashMap<String, BeanDescriptor> map = new HashMap<String, BeanDescriptor>();

	private final HashMap<String, BeanManager> managerMap = new HashMap<String, BeanManager>();

	private final HashMap<Class<?>, BeanTable> tableMap = new HashMap<Class<?>, BeanTable>();

	public BeanDescriptorCacheFactory(DeploymentManager dm, PluginDbConfig dbConfig, DeployUtil deployUtil) {
		this.dbConfig = dbConfig;
		this.deployUtil = deployUtil;
		this.factory = new BeanDescriptorFactory(dm, dbConfig);
		this.managerFactory = new BeanManagerFactory(dbConfig);
	}

	/**
	 * Return an Iterator of all the BeanDescriptors.
	 */
	public Iterator<BeanDescriptor> descriptors() {
		return map.values().iterator();
	}
	
	/**
	 * Get the BeanDescriptor for a bean.
	 */
	public BeanDescriptor get(Class<?> beanClz) {

		return get(beanClz.getName());
	}

	/**
	 * Get the BeanDescriptor given a bean class name.
	 */
	public BeanDescriptor get(String beanClassName) {

		beanClassName = SubClassUtil.getSuperClassName(beanClassName);
		return map.get(beanClassName);
	}

	public BeanManager getBeanManager(Class<?> beanClz) {

		return getBeanManager(beanClz.getName());
	}
	
	public BeanTable getBeanTable(Class<?> cls){
		return tableMap.get(cls);
	}
	
	public BeanManager getBeanManager(String beanClassName) {

		beanClassName = SubClassUtil.getSuperClassName(beanClassName);
		return managerMap.get(beanClassName);
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
		
		deployBeanTables();
		
		deployBeanDescriptors();
		
		// now that all the BeanDescriptors are in their map
		// we can initialise them which sorts out circular
		// dependencies for OneToMany and ManyToOne etc
		
		// PASS 1:
		// initialise the ID properties of all the beans
		// first (as they are needed to initialise the 
		// associated properties in the second pass).
		Iterator<BeanDescriptor> initId = map.values().iterator();
		while (initId.hasNext()) {
			BeanDescriptor d = initId.next();
			d.initialiseId();
		}
		
		// PASS 2:
		// now initialise all the associated properties
		Iterator<BeanDescriptor> otherBeans = map.values().iterator();
		while (otherBeans.hasNext()) {
			BeanDescriptor d = otherBeans.next();
			d.initialiseOther();
		}
		
		// create BeanManager for each non-embedded entity bean
		Iterator<BeanDescriptor> it = map.values().iterator();
		while (it.hasNext()) {
			BeanDescriptor d = it.next();
			if (!d.isEmbedded()){
				BeanManager m = managerFactory.create(d);
				managerMap.put(d.getFullName(), m);
			}
		}
	}

	/**
     * Create a BeanTable for a given type of bean.
     */
    public BeanTable createBeanTable(Class<?> beanClass) {
    	
        DeployBeanTable beanTable = new DeployBeanTable(beanClass);

        // get base table information
        AnnotationBeanTable annBt = new AnnotationBeanTable(deployUtil, beanTable);
        annBt.parse();
                
        //TODO: parse XML deployment for the beanTable...
        
        return new BeanTable(beanTable);
    }
    
	/**
	 * Create the BeanDescriptor for a given class.
	 */
	private void createEmbedded(Class<?> cls) {
		try {
			
			BeanDescriptor d = factory.createEmbedded(cls);
			map.put(cls.getName(), d);

		} catch (PersistenceException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	private void deployBeanTables() {
		PluginProperties props = dbConfig.getProperties();
		BootupClasses bootupClasses = props.getBootupClasses();
		
		List<Class<?>> entityClassList = bootupClasses.getEntities();
		for (int i = 0, y = entityClassList.size(); i < y; i++) {
			Class<?> cls = entityClassList.get(i);

			// need to check if table exists for this server
			BeanTable bt = createBeanTable(cls);
			tableMap.put(cls, bt);
		}
	}
	
	/**
	 * Find all deployment information for Embeddable and Entity types.
	 */
	private void deployBeanDescriptors() {

		PluginProperties props = dbConfig.getProperties();
		BootupClasses bootupClasses = props.getBootupClasses();

		// create the controllers, finders and listeners...
		factory.initialise();

		
		// initialise all the Embeddable types
		List<Class<?>> embClassList = bootupClasses.getEmbeddables();
		for (int i = 0; i < embClassList.size(); i++) {
			Class<?> cls = embClassList.get(i);
			if (logger.isLoggable(Level.FINER)) {
				String msg = "load deployinfo for embeddable:" + cls.getName();
				logger.finer(msg);
			}
			createEmbedded(cls);
		}

		// initialise all the Entity types
		List<Class<?>> entityClasses = bootupClasses.getEntities();
		
		// load deployment info for all the entity bean types
		List<BeanDescriptor> descriptors = factory.createDescriptor(entityClasses);
		
		// put them into our map
		for (BeanDescriptor desc : descriptors) {
			map.put(desc.getBeanType().getName(), desc);
		}
		
		factory.logStatus();
	}

}
