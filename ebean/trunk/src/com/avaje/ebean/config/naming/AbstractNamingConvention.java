/**
 * Copyright (C) 2009  Authors
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

package com.avaje.ebean.config.naming;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import javax.persistence.Column;
import javax.persistence.Table;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.BeanTable;

/**
 * The Class AbstractNamingConvention.
 *
 * @author emcgreal
 */
public abstract class AbstractNamingConvention implements NamingConvention {

	/** The Constant logger. */
	private static final Logger logger = Logger.getLogger(AbstractNamingConvention.class.getName());

	/** The Constant DEFAULT_SEQ_FORMAT. */
	public static final String DEFAULT_SEQ_FORMAT = "{table}_seq";

	/** The catalog. */
	private String catalog;

	/** The schema. */
	private String schema;

	/** The sequence format. */
	final String sequenceFormat;

	/** The database platform. */
	protected DatabasePlatform databasePlatform;

	/** The max fkey length. */
	protected int maxFkeyLength;


	// Constructors -------------------------------------------------
	/**
	 * Instantiates a new default naming convention.
	 *
	 * @param sequenceFormat the sequence format
	 */
	public AbstractNamingConvention(String sequenceFormat) {
		this.sequenceFormat = sequenceFormat;
	}

	/**
	 * Instantiates a new default naming convention.
	 */
	public AbstractNamingConvention() {
		this(DEFAULT_SEQ_FORMAT);
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getForeignKeyName(com.avaje.ebean.server.deploy.BeanPropertyAssocOne)
	 */
	public String getForeignKeyName(BeanPropertyAssocOne<?> p, int fkCount) {
		final StringBuffer buffer = new StringBuffer();
		buffer.append("fk_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, fkCount);

		return buffer.toString();
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#getIndexName(com.avaje.ebean.server.deploy.BeanPropertyAssocOne)
	 */
	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount){
		final StringBuffer buffer = new StringBuffer();
		buffer.append("ix_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, ixCount);

		return buffer.toString();
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.naming.NamingConvention#getM2MJoinTableName(com.avaje.ebean.server.deploy.BeanTable, com.avaje.ebean.server.deploy.BeanTable)
	 */
	public String getM2MJoinTableName(BeanTable lhsTable, BeanTable rhsTable){
		StringBuffer buffer = new StringBuffer();
		buffer.append(lhsTable.getBaseTable());
		buffer.append("_");
		buffer.append(rhsTable.getBaseTable());

		// FIXME - maxFKeyLength is used as the max table name length.
		if (buffer.length() > maxFkeyLength){
			buffer.setLength(maxFkeyLength);
		}

		return buffer.toString();
	}


	/**
	 * Adds the suffix.
	 *
	 * @param buffer the buffer
	 * @param count the count
	 */
	protected void addSuffix(StringBuffer buffer, int count){
		final String suffixNr = Integer.toString(count);
		final int suffixLen = suffixNr.length()+ 1;

		if (buffer.length() + suffixLen > maxFkeyLength){
			buffer.setLength(maxFkeyLength-suffixLen);
		}
		buffer.append("_");
		buffer.append(suffixNr);
	}

	// Names from annotations ---------------------------------------
	/**
	 * Gets the column from annotation.
	 *
	 * @param field the field
	 *
	 * @return the column from annotation
	 */
	protected String getColumnFromAnnotation(Field field){
		final Column c = field.getAnnotation(Column.class);

		// Check for annotation @Column(name="xyz")
		if (c != null){
			final String columnName = c.name();

			if (!isNullString(columnName)){
				// Only need to convert quoted identifiers from annotations
				return databasePlatform.convertQuotedIdentifiers(columnName);
			}
		}

		return null;	// No annotation
	}

	/**
	 * Gets the table name from annotation.
	 *
	 * @param beanClass the bean class
	 *
	 * @return the table name from annotation
	 */
	public TableName getTableNameFromAnnotation(Class<?> beanClass) {
		
		final Table t = findTableAnnotation(beanClass);

		// Take the annotation if defined
		if (t != null){
			if (!isNullString(t.name())){
				// Note: empty catalog and schema are converted to null
				// Only need to convert quoted identifiers from annotations
				return new TableName(quoteIdentifiers(t.catalog()),
					quoteIdentifiers(t.schema()),
					quoteIdentifiers(t.name()));
			}
		}

		return null;	// No annotation
	}
	
	/**
	 * Replace back ticks (if they are used) with database platform specific
	 * quoted identifiers.
	 */
	protected String quoteIdentifiers(String s) {
		return databasePlatform.convertQuotedIdentifiers(s);
	}

	/**
	 * Search for an @Table in the class hierarchy.
	 *
	 * @param cls - initial class to search
	 *
	 * @return the table
	 */
	private Table findTableAnnotation(Class<?> cls) {
		if (cls.equals(Object.class)){
			return null;
		}
		Table table = cls.getAnnotation(Table.class);
		if (table != null){
			return table;
		}
		return findTableAnnotation(cls.getSuperclass());
	}

	// Sequences ----------------------------------------------------
	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.naming.NamingConvention#getSequenceName(java.lang.String)
	 */
	public String getSequenceName(String tableName) {
		return sequenceFormat.replace("{table}", tableName);
	}


	// Getter and setters -------------------------------------------
	/**
	 * Gets the catalog.
	 *
	 * @return the catalog
	 */
	public String getCatalog() {
		return catalog;
	}


	/**
	 * Sets the catalog.
	 *
	 * @param catalog the catalog to set
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}


	/**
	 * Gets the schema.
	 *
	 * @return the schema
	 */
	public String getSchema() {
		return schema;
	}


	/**
	 * Sets the schema.
	 *
	 * @param schema the schema to set
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}


	/**
	 * Gets the sequence format.
	 *
	 * @return the sequenceFormat
	 */
	public String getSequenceFormat() {
		return sequenceFormat;
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.NamingConvention#setDatabasePlatform(com.avaje.ebean.config.dbplatform.DatabasePlatform)
	 */
	public void setDatabasePlatform(DatabasePlatform databasePlatform) {
		this.databasePlatform = databasePlatform;
		maxFkeyLength =  databasePlatform.getDbDdlSyntax().getMaxConstraintNameLength();

		logger.info("Setting maxFkeyLength to: " + maxFkeyLength);
	}

	/**
	 * Gets the database platform.
	 *
	 * @return the databasePlatform
	 */
	public DatabasePlatform getDatabasePlatform() {
		return databasePlatform;
	}

	// Utility methods ----------------------------------------------
	/**
	 * Checks string is null or empty .
	 *
	 * @param s the s
	 *
	 * @return true, if is null string
	 */
	protected boolean isNullString(String s) {
		if (s == null || s.trim().length() == 0) {
			return true;
		}
		return false;
	}
}
