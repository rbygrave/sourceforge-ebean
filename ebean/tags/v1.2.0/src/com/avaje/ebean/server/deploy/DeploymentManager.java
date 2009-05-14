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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.server.deploy.parse.DeployUtil;
import com.avaje.ebean.server.lib.resource.ResourceContent;
import com.avaje.ebean.server.lib.resource.ResourceSource;
import com.avaje.ebean.server.lib.util.Dnode;
import com.avaje.ebean.server.lib.util.DnodeReader;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.server.type.TypeManager;

/**
 * Controls the creation and caching of BeanManager's, BeanDescriptors,
 * BeanTable etc for both beans and tables(MapBeans).
 * <p>
 * Also supports some other deployment features such as type conversion.
 * </p>
 */
public class DeploymentManager implements BeanDescriptorOwner {

	private static final Logger logger = Logger.getLogger(DeploymentManager.class.getName());

	private final PluginDbConfig dbConfig;

	private final DeployUtil deployUtil;

	private final BeanDescriptorCacheFactory beanDescCache;

	private final MapBeanDescriptorCacheFactory mapBeanDescCache;

	private final HashMap<String, DNativeQuery> nativeQueryCache;

	private final ArrayList<Dnode> ormXmlList;

	public DeploymentManager(PluginDbConfig dbConfig) {

		this.nativeQueryCache = new HashMap<String, DNativeQuery>();
		this.dbConfig = dbConfig;
		this.deployUtil = new DeployUtil(this, dbConfig);

		this.beanDescCache = new BeanDescriptorCacheFactory(this, dbConfig, deployUtil);

		this.mapBeanDescCache = new MapBeanDescriptorCacheFactory(this, dbConfig);

		this.ormXmlList = findAllOrmXml();

		initialiseNativeQueries();

		// initialise all the Entity Bean deployment descriptors
		beanDescCache.initialiseAll();

		// initialise all the (MapBean) table descriptors
		mapBeanDescCache.initialiseAll();
	}

	/**
	 * Register all the native queries in ALL orm xml deployment.
	 */
	private void initialiseNativeQueries() {
		for (Dnode ormXml : ormXmlList) {
			initialiseNativeQueries(ormXml);
		}
	}

	/**
	 * Register the native queries in this particular orm xml deployment.
	 */
	private void initialiseNativeQueries(Dnode ormXml) {

		Dnode entityMappings = ormXml.find("entity-mappings");
		if (entityMappings != null) {
			List<Dnode> nq = entityMappings.findAll("named-native-query", 1);
			for (int i = 0; i < nq.size(); i++) {
				Dnode nqNode = nq.get(i);
				Dnode nqQueryNode = nqNode.find("query");
				if (nqQueryNode != null) {
					String queryContent = nqQueryNode.getNodeContent();
					String queryName = (String) nqNode.getAttribute("name");

					if (queryName != null && queryContent != null) {
						DNativeQuery query = new DNativeQuery(queryContent);
						nativeQueryCache.put(queryName, query);
					}
				}
			}
		}
	}

	/**
	 * Return an Iterator of the BeanDescriptors.
	 */
	public Iterator<BeanDescriptor<?>> descriptors() {
		return beanDescCache.descriptors();
	}

	/**
	 * Return an Iterator of the table descriptors.
	 */
	public Iterator<MapBeanDescriptor> tableDescriptors() {
		return mapBeanDescCache.tableDescriptors();
	}

	/**
	 * Return a native named query.
	 * <p>
	 * These are loaded from the orm.xml deployment file.
	 * </p>
	 */
	public DNativeQuery getNativeQuery(String name) {
		return nativeQueryCache.get(name);
	}

	private ArrayList<Dnode> findAllOrmXml() {

		ArrayList<Dnode> ormXmlList = new ArrayList<Dnode>();

		PluginProperties properties = dbConfig.getProperties();

		HashSet<String> readFiles = new HashSet<String>();
		for (int i = 0; i < 20; i++) {
			String ormXmlName = properties.getProperty("ormxml." + i, null);
			if (ormXmlName != null) {
				readFiles.add(ormXmlName);
				if (!readOrmXml(ormXmlName, ormXmlList)) {
					logger.warning("Deployment xml [" + ormXmlName + "] was not found or loaded!");
				}
			}
		}

		String defaultFile = "orm.xml";
		if (!readFiles.contains(defaultFile)) {
			// try to read orm.xml automatically
			readOrmXml(defaultFile, ormXmlList);
		}
		if (ormXmlList.size() == 0) {
			logger.info("No deployment xml (orm.xml etc) was loaded.");

		} else {
			StringBuilder sb = new StringBuilder();
			for (Dnode ox : ormXmlList) {
				sb.append(", ").append(ox.getAttribute("ebean.filename"));
			}
			String loadedFiles = sb.toString().substring(2);
			logger.info("Deployment xml [" + loadedFiles + "]  loaded.");
		}

		return ormXmlList;
	}

	private boolean readOrmXml(String ormXmlName, ArrayList<Dnode> ormXmlList) {
		ResourceSource resSource = dbConfig.getResourceSource();
		try {
			Dnode ormXml = null;
			ResourceContent content = resSource.getContent(ormXmlName);
			if (content != null) {
				// servlet resource or file system...
				ormXml = readOrmXml(content.getInputStream());

			} else {
				// try the classpath...
				ormXml = readOrmXmlFromClasspath(ormXmlName);
			}

			if (ormXml != null) {
				ormXml.setAttribute("ebean.filename", ormXmlName);
				ormXmlList.add(ormXml);
				return true;

			} else {
				return false;
			}
		} catch (IOException e) {
			logger.log(Level.SEVERE, "error reading orm xml deployment " + ormXmlName, e);
			return false;
		}
	}

	private Dnode readOrmXmlFromClasspath(String ormXmlName) throws IOException {
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(ormXmlName);
		if (is == null) {
			return null;
		} else {
			return readOrmXml(is);
		}
	}

	private Dnode readOrmXml(InputStream in) throws IOException {
		DnodeReader reader = new DnodeReader();
		Dnode ormXml = reader.parseXml(in);
		in.close();
		return ormXml;
	}

	/**
	 * Find the deployment xml for a given entity. This will return null if no
	 * matching deployment xml is found for this entity.
	 * <p>
	 * This searches all the ormXml files and returns the first match.
	 * </p>
	 */
	public Dnode findEntityDeploymentXml(String className) {

		for (Dnode ormXml : ormXmlList) {
			Dnode entityMappings = ormXml.find("entity-mappings");

			List<Dnode> entities = entityMappings.findAll("entity", "class", className, 1);
			if (entities.size() == 1) {
				return entities.get(0);
			}
		}

		return null;
	}

	/**
	 * Return DeployUtil which is a helper for deployment processing.
	 */
	public DeployUtil getDeployUtil() {
		return deployUtil;
	}

	/**
	 * Return the TypeConverter for this server.
	 */
	public TypeManager getTypeManager() {
		return dbConfig.getTypeManager();
	}

	/**
	 * Return the PluginDbConfig.
	 */
	public PluginDbConfig getDbConfig() {
		return dbConfig;
	}

	/**
	 * Convert a value.
	 */
	public Object convert(Object v, int type) {
		return dbConfig.getTypeManager().convert(v, type);
	}

	/**
	 * Return the server name.
	 */
	public String getServerName() {
		return dbConfig.getProperties().getServerName();
	}

	// /**
	// * Resolve the Class for the class name and methodInfo.
	// * <p>
	// * The methodInfo is used to determine the method interception on the
	// * generated class.
	// * </p>
	// * <p>
	// * If the class has already been generated then it is returned out of a
	// * cache.
	// * </p>
	// */
	// public Class<?> resolve(String name, MethodInfo methodInfo) {
	// return subClassManager.resolve(name, methodInfo);
	// }

	/**
	 * Get the BeanTable for a given bean class.
	 */
	public BeanTable getBeanTable(Class<?> beanClz) {
		return beanDescCache.getBeanTable(beanClz);
	}

	/**
	 * Return the MapBeanDescriptor for a given table.
	 */
	public MapBeanDescriptor getMapBeanDescriptor(String tableName) {
		if (tableName == null) {
			throw new NullPointerException("tableName is null?");
		}
		return mapBeanDescCache.get(tableName);
	}

	/**
	 * Return the BeanManager for a given table.
	 */
	public BeanManager<?> getMapBeanManager(String tableName) {
		if (tableName == null) {
			throw new NullPointerException("tableName is null?");
		}
		return mapBeanDescCache.getManager(tableName);
	}

	/**
	 * Get the BeanDescriptor for a bean.
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> entityType) {

		return beanDescCache.get(entityType);
	}

	/**
	 * Get the BeanDescriptor for a given bean class name.
	 */
	public BeanDescriptor<?> getBeanDescriptor(String beanClzName) {

		return beanDescCache.get(beanClzName);
	}

	/**
	 * Get the BeanManager for a bean.
	 */
	public <T> BeanManager<T> getBeanManager(Class<T> entityType) {

		return beanDescCache.getBeanManager(entityType);
		//return (BeanManager<T>)getBeanManager(beanClz.getName());
	}

	/**
	 * Get the BeanDescriptor given a bean class name.
	 */
	public BeanManager<?> getBeanManager(String beanClassName) {

		return beanDescCache.getBeanManager(beanClassName);
	}

}
