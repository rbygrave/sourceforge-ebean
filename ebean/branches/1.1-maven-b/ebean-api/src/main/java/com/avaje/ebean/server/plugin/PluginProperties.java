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
package com.avaje.ebean.server.plugin;

import java.util.Iterator;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.util.Message;

/**
 * The deployment properties for a given server instance.
 */
public class PluginProperties implements com.avaje.ebean.server.plugin.spi.PluginProperties {

	private final String name;

	private final ConfigProperties configProperties;
	
	private final BootupClasses bootupClasses;
	
	private final DataSource dataSource;
	
	public PluginProperties(String name, DataSource ds, ConfigProperties configProps, BootupClasses bootupClasses) {
		this.name = name;
		this.dataSource = ds;
		this.configProperties = configProps;
		this.bootupClasses = bootupClasses;
	}
	
	/**
	 * Return the classes such as entities, scalar types etc.
	 */
	public BootupClasses getBootupClasses() {
		return bootupClasses;
	}
	
	/**
	 * Return the associated DataSource.
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	public ConfigProperties getConfigProperties() {
		return configProperties;
	}

	/**
	 * Return the name of this server.
	 */
	public String getServerName() {
		return name;
	}

	/**
	 * Return a int deployment property.
	 */
	public int getPropertyInt(String key, int defaultValue) {
		String val = getProperty(key, "" + defaultValue);
		if (val.length() == 0) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(val);
		} catch (Exception ex) {
			throw new PersistenceException(Message.msg("plugin.deploy.integer", val));
		}
	}

	/**
	 * Return a boolean deployment property.
	 */
	public boolean getPropertyBoolean(String key, boolean defaultValue) {
		String val = getProperty(key, "" + defaultValue);
		return val.equalsIgnoreCase("true");
	}

	/**
	 * Return all the keys in the property map.
	 * <p>
	 * This includes ALL keys and not just ones for Ebean.
	 * </p>
	 */
	public Iterator<String> keys() {
		return configProperties.keys();
	}

	/**
	 * Return a deployment property for this DbPlugin. If the DbPlugin is the
	 * primary database then the key just has 'ebean.' as the prefix. Otherwise
	 * it tries to used the DbPlugin name to read the deployment information.
	 */
	public String getProperty(String key, String defaultValue) {

		String primaryKey = "ebean." + key;
		if (name == null) {
			return configProperties.getProperty(primaryKey, defaultValue);

		} else {
			String fullKey = "ebean." + name + "." + key;
			String val = configProperties.getProperty(fullKey, null);
			if (val != null) {
				return val;
			} else {
				return configProperties.getProperty(primaryKey, defaultValue);
			}
		}
	}

	/**
	 * Set a default deployment property. Only set if a value for that key has
	 * not been explicitly set already.
	 */
	public void setPropertyDefault(String key, String value) {
		String existingValue = getProperty(key, null);
		if (existingValue == null) {
			key = "ebean." + key;
			configProperties.setProperty(key, value);
		}
	}
}
