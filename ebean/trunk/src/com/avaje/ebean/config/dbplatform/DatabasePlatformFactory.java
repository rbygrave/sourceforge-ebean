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
package com.avaje.ebean.config.dbplatform;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.config.ServerConfig;

/**
 * Create a DatabasePlatform from the configuration.
 * <p>
 * Will used platform name or use the meta data from the
 * JDBC driver to determine the platform automatically.
 * </p>
 */
public class DatabasePlatformFactory {

	private static final Logger logger = Logger.getLogger(DatabasePlatformFactory.class.getName());

	/**
	 * Create the appropriate database specific platform.
	 */
	public DatabasePlatform create(ServerConfig serverConfig) {

		try {

			if (serverConfig.getDatabasePlatformName() != null){
				// choose based on dbName
				return byDatabaseName(serverConfig.getDatabasePlatformName());
				
			} else {
				// guess using meta data from driver
				return byDataSource(serverConfig.getDataSource());
			}

		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Lookup the platform by name.
	 */
	private DatabasePlatform byDatabaseName(String dbName) throws SQLException {

		dbName = dbName.toLowerCase();
		if (dbName.equals("postgres83")){
			return new Postgres83Platform();
		} 
		if (dbName.equals("oracle9")){
			return new Oracle9Platform();
		} 
		if (dbName.equals("oracle10")){
			return new Oracle10Platform();
		}
		if (dbName.equals("oracle")){
			return new Oracle10Platform();
		}
		if (dbName.equals("sqlserver2005")){
			return new MsSqlServer2005Platform();
		}
		if (dbName.equals("sqlserver2000")){
			return new MsSqlServer2000Platform();
		}
		if (dbName.equals("mysql")){
			return new MySqlPlatform();
		}
		
		throw new RuntimeException("database platform "+dbName+" is not known?");
	}


	/**
	 * Use JDBC DatabaseMetaData to determine the platform.
	 */
	private DatabasePlatform byDataSource(DataSource dataSource) {
		
		Connection conn = null;
		try {
			conn = dataSource.getConnection();
			DatabaseMetaData metaData = conn.getMetaData();

			return byDatabaseMeta(metaData);

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
	 * Find the platform by the metaData.getDatabaseProductName().
	 */
	private DatabasePlatform byDatabaseMeta(DatabaseMetaData metaData) throws SQLException {

		String dbProductName = metaData.getDatabaseProductName();
		dbProductName = dbProductName.toLowerCase();

		int majorVersion = metaData.getDatabaseMajorVersion();

		if (dbProductName.indexOf("oracle") > -1) {
			if (majorVersion > 9) {
				return new Oracle10Platform();
			} else {
				return new Oracle9Platform();
			}
		}
		if (dbProductName.indexOf("microsoft") > -1) {
			if (majorVersion > 8){
				return new MsSqlServer2005Platform();				
			} else {
				return new MsSqlServer2000Platform();
			}
		}

		if (dbProductName.indexOf("mysql") > -1) {
			return new MySqlPlatform();
		}
		if (dbProductName.indexOf("h2") > -1) {
			return new H2Platform();
		}
		if (dbProductName.indexOf("postgres") > -1) {
			return new Postgres83Platform();
		}
		// use the standard one
		return new DatabasePlatform();
	}
}
