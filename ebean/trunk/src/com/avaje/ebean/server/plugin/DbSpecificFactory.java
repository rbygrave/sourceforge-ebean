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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.server.lib.ConfigProperties;
import com.avaje.ebean.server.lib.util.FactoryHelper;

public class DbSpecificFactory {

	private static final Logger logger = Logger.getLogger(DbSpecificFactory.class.getName());

	private static final Class<?>[] CONS_TYPES = { PluginProperties.class };

	/**
	 * Create the appropriate DbSpecific.
	 */
	public DbSpecific create(PluginProperties properties) {

		String serverName = properties.getServerName();
		
		// see if we have explicitly specified in deployment 
		// the db specific plugin to use for this server
		ConfigProperties configProperties = properties.getConfigProperties();
		String plugin = configProperties.getProperty("datasource."+serverName+".plugin", null);
		String classname = configProperties.getProperty(serverName+".dbconfig", plugin);
		
		String dbName = configProperties.getProperty("datasource."+serverName+".database", null);
		
		try {
			if (classname != null) {

				Object[] args = { properties };
				return (DbSpecific) FactoryHelper.create(classname, CONS_TYPES, args);

			} else if (dbName != null){
				// choose based on dbName
				return byDatabaseName(properties, dbName);
				
			} else {
				// guess using meta data from driver
				return byDataSource(properties);
			}

		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Try to guess the correct plugin.
	 */
	private DbSpecific byDatabaseName(PluginProperties properties, String dbName) throws SQLException {

		dbName = dbName.toLowerCase();
		if (dbName.equals("postgres83")){
			return new Postgres83Plugin(properties);
		} 
		if (dbName.equals("oracle9")){
			return new Oracle9Plugin(properties);
		} 
		if (dbName.equals("oracle10")){
			return new Oracle10Plugin(properties);
		}
		if (dbName.equals("sqlserver2005")){
			return new MsSqlServer2005(properties);
		}
		if (dbName.equals("sqlserver2000")){
			return new MsSqlServer2000(properties);
		}
		if (dbName.equals("mysql")){
			return new MySqlPlugin(properties);
		}
		if (dbName.equals("mckoi")){
			return new MckoiPlugin(properties);
		}
		
		// use the standard one
		return new DbSpecific(properties);
	}


	/**
	 * Try to guess the correct plugin.
	 */
	private DbSpecific byDataSource(PluginProperties properties) {
		
		DataSource dataSource = properties.getDataSource();
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();

			return byDatabaseMeta(properties, metaData);

		} catch (SQLException ex) {
			throw new PersistenceException(ex);

		} finally {
			try {
				if (conn != null) {
					conn.close();
				}
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		}
	}
	
	/**
	 * Try to guess the correct plugin.
	 */
	private DbSpecific byDatabaseMeta(PluginProperties properties,
			DatabaseMetaData metaData) throws SQLException {

		String dbProductName = metaData.getDatabaseProductName();
		dbProductName = dbProductName.toLowerCase();

		int majorVersion = metaData.getDatabaseMajorVersion();

		if (dbProductName.indexOf("oracle") > -1) {
			if (majorVersion > 9) {
				return new Oracle10Plugin(properties);
			} else {
				return new Oracle9Plugin(properties);
			}
		}
		if (dbProductName.indexOf("microsoft") > -1) {
			if (majorVersion > 8){
				return new MsSqlServer2005(properties);				
			} else {
				return new MsSqlServer2000(properties);
			}
		}
		if (dbProductName.indexOf("mckoi") > -1) {
			return new MckoiPlugin(properties);
		}
		if (dbProductName.indexOf("mysql") > -1) {
			return new MySqlPlugin(properties);
		}
		if (dbProductName.indexOf("h2") > -1) {
			return new H2Plugin(properties);
		}
		if (dbProductName.indexOf("postgres") > -1) {
			return new Postgres83Plugin(properties);
		}
		// use the standard one
		return new DbSpecific(properties);
	}
}
