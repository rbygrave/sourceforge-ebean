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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.Ebean;

/**
 * Database platform specific settings.
 */
public class GenericDatabasePlatform implements DatabasePlatform {

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(GenericDatabasePlatform.class.getName());

	/** The open quote used by quoted identifiers. */
	protected String openQuote = "\"";

	/** The close quote used by quoted identifiers. */
	protected String closeQuote = "\"";

	/** For limit/offset, row_number etc limiting of SQL queries. */
	protected SqlLimiter sqlLimiter = new LimitOffsetSqlLimiter();

	/** Mapping of JDBC to Database types. */
	protected DbTypeMap dbTypeMap = new DbTypeMap();

	/** DB specific DDL syntax. */
	protected DbDdlSyntax dbDdlSyntax = new DbDdlSyntax();

	/** Defines DB identity/sequence features. */
	protected DbIdentity dbIdentity = new DbIdentity();

	/** The JDBC type to map booleans to (by default). */
	protected int booleanDbType = Types.BOOLEAN;

	/** The JDBC type to map Blob to. */
	protected int blobDbType = Types.BLOB;

	/** For Oracle treat empty strings as null. */
	protected boolean treatEmptyStringsAsNull;

	/** The name. */
	protected String name = "generic";

	/**
	 * Use a BackTick ` at the beginning and end of table or column names that
	 * you want to use quoted identifiers for. The backticks get converted to
	 * the appropriate characters in convertQuotedIdentifiers
	 */
	private static final char BACK_TICK = '`';

	protected DbEncrypt dbEncrypt;

	/**
	 * Instantiates a new database platform.
	 */
	public GenericDatabasePlatform() {
	}

	/**
	 * Realization of {@link DatabasePlatform#getName()}
	 * <p>
	 * "generic" is returned when no specific database platform has been set or
	 * found.
	 * </p>
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return a DB Sequence based IdGenerator.
	 * 
	 * @param be
	 *            the BackgroundExecutor that can be used to load the sequence
	 *            if desired
	 * @param ds
	 *            the DataSource
	 * @param seqName
	 *            the name of the sequence
	 * @param batchSize
	 *            the number of sequences that should be loaded
	 */
	public IdGenerator createSequenceIdGenerator(BackgroundExecutor be, DataSource ds, String seqName,
			int batchSize) {

		return null;
	}

	/**
	 * 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getDbEncrypt()
	 */
	public DbEncrypt getDbEncrypt() {
		return dbEncrypt;
	}

	/** 
	 * @see
	 * com.avaje.ebean.config.dbplatform.DatabasePlatform#setDbEncrypt(com.
	 * avaje.ebean.config.dbplatform.DbEncrypt)
	 */
	public void setDbEncrypt(DbEncrypt dbEncrypt) {
		this.dbEncrypt = dbEncrypt;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getDbTypeMap()
	 */
	public DbTypeMap getDbTypeMap() {
		return dbTypeMap;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getDbDdlSyntax()
	 */
	public DbDdlSyntax getDbDdlSyntax() {
		return dbDdlSyntax;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getCloseQuote()
	 */
	public String getCloseQuote() {
		return closeQuote;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getOpenQuote()
	 */
	public String getOpenQuote() {
		return openQuote;
	}

	/** 
	 * @see
	 * com.avaje.ebean.config.dbplatform.DatabasePlatform#getBooleanDbType()
	 */
	public int getBooleanDbType() {
		return booleanDbType;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getBlobDbType()
	 */
	public int getBlobDbType() {
		return blobDbType;
	}

	/** 
	 * @see
	 * com.avaje.ebean.config.dbplatform.DatabasePlatform#isTreatEmptyStringsAsNull
	 * ()
	 */
	public boolean isTreatEmptyStringsAsNull() {
		return treatEmptyStringsAsNull;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getDbIdentity()
	 */
	public DbIdentity getDbIdentity() {
		return dbIdentity;
	}

	/** 
	 * @see com.avaje.ebean.config.dbplatform.DatabasePlatform#getSqlLimiter()
	 */
	public SqlLimiter getSqlLimiter() {
		return sqlLimiter;
	}

	/** 
	 * @see
	 * com.avaje.ebean.config.dbplatform.DatabasePlatform#convertQuotedIdentifiers
	 * (java.lang.String)
	 */
	public String convertQuotedIdentifiers(String dbName) {
		// Ignore null values e.g. schema name or catalog
		if (dbName != null && dbName.length() > 0) {
			if (dbName.charAt(0) == BACK_TICK) {
				if (dbName.charAt(dbName.length() - 1) == BACK_TICK) {

					String quotedName = getOpenQuote();
					quotedName += dbName.substring(1, dbName.length() - 1);
					quotedName += getCloseQuote();

					return quotedName;

				} else {
					logger.log(Level.SEVERE, "Missing backquote on [" + dbName + "]");
				}
			}
		}
		return dbName;
	}
}
