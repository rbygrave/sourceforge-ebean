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
package org.avaje.ebean.server.core;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.avaje.ebean.server.lib.ConfigProperties;


/**
 * DataSource retrieved from JNDI lookup.
 *
 */
public class JndiDataSourceFactory implements DataSourceFactory {

	private static final String DEFAULT_PREFIX = "java:comp/env/jdbc/";

	
	public JndiDataSourceFactory() {
	}

	private String getJndiPrefix(ConfigProperties config) {
		return config.getProperty("ebean.datasource.jndi.prefix", DEFAULT_PREFIX);
	}
	
	/**
	 * Return the DataSource by JNDI lookup.
	 * <p>
	 * If name is null the 'default' dataSource is returned.
	 * </p>
	 */
	public DataSource createDataSource(String name, ConfigProperties config) {

		if (name == null) {
			// get the default dataSource name from avaje.properties
			String dflt = config.getProperty("ebean.datasource.default");
			name = config.getProperty("datasource.default", dflt);
			if (name == null) {
				String msg = "datasource.default has not be defined in system.properties";
				throw new PersistenceException(msg);
			}
		}

		try {
			
			Context ctx = new InitialContext();
			String lookupName = getJndiPrefix(config) + name;
			DataSource ds = (DataSource) ctx.lookup(lookupName);
			if (ds == null) {
				throw new PersistenceException("JNDI DataSource [" + lookupName + "] not found?");
			}
			return ds;

		} catch (NamingException ex) {
			throw new PersistenceException(ex);
		}
	}
}
