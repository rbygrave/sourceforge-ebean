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
package com.avaje.ebean;

import java.util.ArrayList;
import java.util.Properties;

import javax.sql.DataSource;

/**
 * Describes information for programmatically creating a EbeanServer.
 * <p>
 * Typically you will create a new ServerConfiguration, set some of its
 * properties and then pass it to
 * {@linkplain Ebean#registerServer(ServerConfiguration)}.
 * </p>
 * <p>
 * Note that when Ebean starts it automatically checks to see if there is a
 * 'default/primary' server and if there is creates it. It is likely that you do
 * <b>NOT</b> want this to happen as so you should make sure that
 * avaje.properties does not contain a "datasource.default" property.
 * </p>
 * 
 * <pre class="code">
 * // create a configuration
 * ServerConfiguration config = new ServerConfiguration(&quot;hdb&quot;);
 * config.setDataSourceUsername(&quot;sa&quot;);
 * config.setDataSourceDriver(&quot;org.h2.Driver&quot;);
 * config.setDataSourceUrl(&quot;jdbc:h2:database/test;SCHEMA=TEST&quot;);
 * config.setDataSourceMaxConnections(20);
 * 
 * // optional but recommended
 * config.setDataSourceHeartbeatSql(&quot;select count(*) from my_small_table&quot;);
 * 
 * // its going to be my 'default/primary' EbeanServer
 * config.setDefaultServer(true);
 * 
 * // register... returning the newly created EbeanServer
 * EbeanServer ebeanServer = Ebean.registerServer(config);
 * 
 * // use it...
 * 
 * // later we can get the same instance of EbeanServer
 * EbeanServer ebeanServer3 = Ebean.getServer(&quot;hdb&quot;);
 * 
 * // we can use null for the name because its the 'default' server
 * EbeanServer ebeanServer2 = Ebean.getServer(null);
 * 
 * // ebeanServer == ebeanServer2 == ebeanServer3.
 * 
 * </pre>
 */
public class ServerConfiguration {

	final String name;

	boolean defaultServer;

	DataSource dataSource;

	Properties properties;

	/**
	 * List of interesting classes such as entities, embedded, ScalarTypes,
	 * Listeners, Finders, Controllers etc.
	 */
	ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

	/**
	 * Create a ServerConfiguration with a given name.
	 * <p>
	 * This is the name of the EbeanServer (and typically the same as the
	 * DataSource name). This is used with {@link Ebean#getServer(String)}.
	 * </p>
	 * 
	 * @param name
	 *            the EbeanServer name.
	 */
	public ServerConfiguration(String name) {
		this.name = name;
	}

	/**
	 * Return true if this is the 'default/primary' EbeanServer.
	 */
	public boolean isDefaultServer() {
		return defaultServer;
	}

	/**
	 * Set this to true if this is the 'default/primary' EbeanServer.
	 */
	public void setDefaultServer(boolean defaultServer) {
		this.defaultServer = defaultServer;
	}

	/**
	 * Return the DataSource.
	 * <p>
	 * If this is null, then Ebean will create a DataSource based on the
	 * properties set via setDataSourceXXX() or properties already existing in
	 * avaje.properties.
	 * </p>
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * If you already have a DataSource you can use it.
	 * <p>
	 * You will either use this method setting an already existing DataSource or
	 * a DataSource will be created by Ebean using the setDataSourceXXX()
	 * settings or settings that already exist in the avaje.properties file.
	 * </p>
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the properties for this configuration.
	 */
	public Properties getProperties() {
		return properties;
	}

	/**
	 * Set all the properties. If you are going to do this you should do it
	 * BEFORE using any of the setDataSourceXXX() methods as they set values to
	 * the underlying properties file.
	 * <p>
	 * There will generally be a avaje.properties file and these properties
	 * effectively override/extend the ones from avaje.properties.
	 * </p>
	 */
	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	/**
	 * Return the name of the EbeanServer. Typically the same as the DataSource
	 * name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set any property that you would otherwise find in avaje.properties.
	 * <p>
	 * This property will effectively
	 * </p>
	 * 
	 * @param property
	 * @param value
	 */
	public void setProperty(String property, String value) {
		if (properties == null) {
			properties = new Properties();
		}
		properties.setProperty(property, value);
	}

	/**
	 * Set the DataSource user name.
	 */
	public void setDataSourceUsername(String username) {
		setProperty("datasource." + name + ".username", username);
	}

	/**
	 * Set the DataSource password.
	 */
	public void setDataSourcePassword(String password) {
		setProperty("datasource." + name + ".password", password);
	}

	/**
	 * Set the DataSource connection url.
	 */
	public void setDataSourceUrl(String databaseUrl) {
		setProperty("datasource." + name + ".databaseUrl", databaseUrl);
	}

	/**
	 * Set the DataSource driver.
	 */
	public void setDataSourceDriver(String databaseDriver) {
		setProperty("datasource." + name + ".databaseDriver", databaseDriver);
	}

	/**
	 * Set the DataSource heart beat sql.
	 * <p>
	 * This SQL is used to test a DataSource connection to make sure it is still
	 * functioning. The test occurs whenever an unexpected SQLException occurs.
	 * </p>
	 * <p>
	 * You do not have to set this property but it is recommended. The SQL
	 * should be a simple query that is cheap to execute.
	 * </p>
	 * 
	 * <pre>
	 * // the query should be cheap to execute
	 * setDataSourceHeartbeatSql(&quot;select id from my_very_small_table&quot;);
	 * 
	 * // commonly on Oracle you would use the dual table
	 * setDataSourceHeartbeatSql(&quot;select * from dual&quot;);
	 * </pre>
	 */
	public void setDataSourceHeartbeatSql(String heartbeatsql) {
		setProperty("datasource." + name + ".heartbeatsql", heartbeatsql);
	}

	/**
	 * Set the minimum number of connections this pool should maintain.
	 */
	public void setDataSourceMinConnections(int minConnections) {
		setProperty("datasource." + name + ".minConnections", String.valueOf(minConnections));
	}

	/**
	 * Set the maximum number of connections this pool can obtain.
	 */
	public void setDataSourceMaxConnections(int maxConnections) {
		setProperty("datasource." + name + ".maxConnections", String.valueOf(maxConnections));
	}

	/**
	 * Programmatically add classes that this server should use.
	 * <p>
	 * The class can be an Entity, Embedded type, ScalarType, BeanListener,
	 * BeanFinder or BeanController.
	 * </p>
	 * <p>
	 * If no classes are added using this method then the classes are found
	 * automatically via searching the classpath.
	 * </p>
	 * 
	 * @param cls
	 *            the entity type (or other type) that should be registered by
	 *            this server.
	 */
	public void addEntity(Class<?> cls) {
		classes.add(cls);
	}

	ArrayList<Class<?>> getClasses() {
		return classes;
	}
}
