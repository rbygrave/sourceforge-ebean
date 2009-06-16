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

import java.sql.Types;

/**
 * Database platform specific settings.
 */
public class DatabasePlatform {

	/**
	 * The open quote used by quoted identifiers.
	 */
	protected String openQuote = "\"";

	/**
	 * The close quote used by quoted identifiers.
	 */
	protected String closeQuote = "\"";

	/**
	 * For limit/offset, row_number etc limiting of SQL queries.
	 */
	protected SqlLimiter sqlLimiter = new LimitOffsetSqlLimiter();
	
	/**
	 * Mapping of JDBC to Database types.
	 */
	protected DbTypeMap dbTypeMap = new DbTypeMap();
	
	/**
	 * DB specific DDL syntax.
	 */
	protected DbDdlSyntax dbDdlSyntax = new DbDdlSyntax();
	
	/**
	 * Defines DB identity/sequence features.
	 */
	protected DbIdentity dbIdentity = new DbIdentity();
	
	/**
	 * The JDBC type to map booleans to (by default). 
	 */
	protected int booleanDbType = Types.BOOLEAN;
	
	/**
	 * For Oracle treat empty strings as null.
	 */
	protected boolean treatEmptyStringsAsNull;
	
	/**
	 * By default support JDBC batching for bean insert update and delete.
	 */
	protected boolean defaultBatching;
	
	public DatabasePlatform() {

	}

	/**
	 * Return the mapping of JDBC to DB types.
	 */
	public DbTypeMap getDbTypeMap() {
		return dbTypeMap;
	}

	/**
	 * Return the DDL syntax for this platform.
	 */
	public DbDdlSyntax getDbDdlSyntax() {
		return dbDdlSyntax;
	}
	
	/**
	 * Return the close quote for quoted identifiers.
	 */
	public String getCloseQuote() {
		return closeQuote;
	}

	/**
	 * Return the open quote for quoted identifiers.
	 */
	public String getOpenQuote() {
		return openQuote;
	}
	
	/**
	 * Return true if by default JDBC batching should be used.
	 */
	public boolean isDefaultBatching() {
		return defaultBatching;
	}

	/**
	 * Return the JDBC type used to store booleans.
	 */
	public int getBooleanDbType() {
		return booleanDbType;
	}
	
	/**
	 * Return true if empty strings should be treated as null.
	 */
	public boolean isTreatEmptyStringsAsNull() {
		return treatEmptyStringsAsNull;
	}
	
	/**
	 * Return the DB identity/sequence features for this platform.
	 */
	public DbIdentity getDbIdentity() {
		return dbIdentity;
	}

	/**
	 * Return the SqlLimiter used to apply additional sql around 
	 * a query to limit its results.
	 * <p>
	 * Basically add the clauses for limit/offset, rownum, row_number().
	 * </p>
	 */
	public SqlLimiter getSqlLimiter() {
		return sqlLimiter;
	}
}
