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


/**
 * Controls the creation and caching of BeanManager's, BeanDescriptors,
 * BeanTable etc for both beans and tables(MapBeans).
 * <p>
 * Also supports some other deployment features such as type conversion.
 * </p>
 */
public class DeploymentManager {

//	private static final Logger logger = Logger.getLogger(DeploymentManager.class.getName());
//
//
//	private final DeployUtil deployUtil;
//
//	private final BeanDescriptorBuilder beanDescCache;
//
//	private final DeployOrmXml deploymentOrmXml;
//	
//	public DeploymentManager(InternalConfiguration dbConfig) {
//
//		this.deployUtil = new DeployUtil(dbConfig.getDatabasePlatform(), dbConfig.getTypeManager(), dbConfig.getServerConfig().getNamingConvention());
//
//		this.beanDescCache = new BeanDescriptorBuilder(this, dbConfig, deployUtil);
//
//		// initialise all the Entity Bean deployment descriptors
//		beanDescCache.initialiseAll();
//
//		this.deploymentOrmXml = dbConfig.getDeployOrmXml();
//	}

//	
//
//	public List<BeanDescriptor<?>> getBeanDescriptors() {
//		return beanDescCache.getBeanDescriptors();
//	}
//	
//	/**
//	 * Return a native named query.
//	 * <p>
//	 * These are loaded from the orm.xml deployment file.
//	 * </p>
//	 */
//	public DNativeQuery getNativeQuery(String name) {
//		return deploymentOrmXml.getNativeQuery(name);
//	}
//
//	/**
//	 * Find the deployment xml for a given entity. This will return null if no
//	 * matching deployment xml is found for this entity.
//	 * <p>
//	 * This searches all the ormXml files and returns the first match.
//	 * </p>
//	 */
//	public Dnode findEntityDeploymentXml(String className) {
//
//		return deploymentOrmXml.findEntityDeploymentXml(className);
//	}
//
//	/**
//	 * Return DeployUtil which is a helper for deployment processing.
//	 */
//	public DeployUtil getDeployUtil() {
//		return deployUtil;
//	}
//
////	/**
////	 * Return the TypeConverter for this server.
////	 */
////	public TypeManager getTypeManager() {
////		return dbConfig.getTypeManager();
////	}
//
////	/**
////	 * Return the PluginDbConfig.
////	 */
////	public PluginDbConfig getDbConfig() {
////		return dbConfig;
////	}
//
////	/**
////	 * Convert a value.
////	 */
////	public Object convert(Object v, int type) {
////		return dbConfig.getTypeManager().convert(v, type);
////	}
//
////	/**
////	 * Return the server name.
////	 */
////	public String getServerName() {
////		return dbConfig.getProperties().getServerName();
////	}
//
//	/**
//	 * Get the BeanDescriptor for a bean.
//	 */
//	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> entityType) {
//
//		return beanDescCache.get(entityType);
//	}
//
//	/**
//	 * Get the BeanDescriptor for a given bean class name.
//	 */
//	public BeanDescriptor<?> getBeanDescriptor(String beanClzName) {
//
//		return beanDescCache.get(beanClzName);
//	}
//
//	/**
//	 * Get the BeanManager for a bean.
//	 */
//	public <T> BeanManager<T> getBeanManager(Class<T> entityType) {
//
//		return beanDescCache.getBeanManager(entityType);
//	}
//
//	/**
//	 * Get the BeanDescriptor given a bean class name.
//	 */
//	public BeanManager<?> getBeanManager(String beanClassName) {
//
//		return beanDescCache.getBeanManager(beanClassName);
//	}

}
