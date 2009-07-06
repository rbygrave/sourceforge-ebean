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

import java.util.logging.Logger;

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

	/** The max length of constraint names. */
	protected int maxConstraintNameLength;


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

	public void setDatabasePlatform(DatabasePlatform databasePlatform) {
		this.databasePlatform = databasePlatform;
		this.maxConstraintNameLength = databasePlatform.getDbDdlSyntax().getMaxConstraintNameLength();
		
		logger.finer("Using maxConstraintNameLength of "+maxConstraintNameLength);
	}
	
	public String getForeignKeyName(BeanPropertyAssocOne<?> p, int fkCount) {
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("fk_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, fkCount);

		return buffer.toString();
	}

	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount){
		
		StringBuilder buffer = new StringBuilder();
		buffer.append("ix_");
		buffer.append(p.getBeanDescriptor().getBaseTable());
		buffer.append("_");
		buffer.append(p.getName());

		addSuffix(buffer, ixCount);

		return buffer.toString();
	}


	public String getM2MJoinTableName(BeanTable lhsTable, BeanTable rhsTable){
		
		StringBuilder buffer = new StringBuilder();
		buffer.append(lhsTable.getBaseTable());
		buffer.append("_");
		buffer.append(rhsTable.getBaseTable());

		// maxConstraintNameLength is used as the max table name length.
		if (buffer.length() > maxConstraintNameLength){
			buffer.setLength(maxConstraintNameLength);
		}

		return buffer.toString();
	}


	/**
	 * Adds the suffix.
	 *
	 * @param buffer the buffer
	 * @param count the count
	 */
	protected void addSuffix(StringBuilder buffer, int count){
		final String suffixNr = Integer.toString(count);
		final int suffixLen = suffixNr.length()+ 1;

		if (buffer.length() + suffixLen > maxConstraintNameLength){
			buffer.setLength(maxConstraintNameLength-suffixLen);
		}
		buffer.append("_");
		buffer.append(suffixNr);
	}


	// Sequences ----------------------------------------------------
	public String getSequenceName(String tableName) {
		return sequenceFormat.replace("{table}", tableName);
	}


	// Getter and setters -------------------------------------------
	/**
	 * Return the catalog.
	 */
	public String getCatalog() {
		return catalog;
	}


	/**
	 * Sets the catalog.
	 */
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}


	/**
	 * Return the schema.
	 */
	public String getSchema() {
		return schema;
	}


	/**
	 * Sets the schema.
	 */
	public void setSchema(String schema) {
		this.schema = schema;
	}


	/**
	 * Returns the sequence format.
	 */
	public String getSequenceFormat() {
		return sequenceFormat;
	}

	protected abstract TableName getTableNameByConvention(Class<?> beanClass);
	
	/**
	 * Returns the table name for a given entity bean.
	 * <p>
	 * This first checks for the @Table annotation and if not present
	 * uses the naming convention to define the table name.
	 * </p>
	 */
	public TableName getTableName(Class<?> beanClass) {

		TableName tableName = getTableNameFromAnnotation(beanClass);
		if (tableName == null){
			tableName = getTableNameByConvention(beanClass);
		}
		return tableName;
	}
	
	/**
	 * Gets the table name from annotation.
	 */
	protected TableName getTableNameFromAnnotation(Class<?> beanClass) {
		
		final Table t = findTableAnnotation(beanClass);

		// Take the annotation if defined
		if (t != null && !isEmpty(t.name())){
			// Note: empty catalog and schema are converted to null
			// Only need to convert quoted identifiers from annotations
			return new TableName(quoteIdentifiers(t.catalog()),
				quoteIdentifiers(t.schema()),
				quoteIdentifiers(t.name()));
		}

		// No annotation
		return null;	
	}
	
	/**
	 * Search recursively for an @Table in the class hierarchy.
	 */
	protected Table findTableAnnotation(Class<?> cls) {
		if (cls.equals(Object.class)){
			return null;
		}
		Table table = cls.getAnnotation(Table.class);
		if (table != null){
			return table;
		}
		return findTableAnnotation(cls.getSuperclass());
	}
	
	/**
	 * Replace back ticks (if they are used) with database platform specific
	 * quoted identifiers.
	 */
	protected String quoteIdentifiers(String s) {
		return databasePlatform.convertQuotedIdentifiers(s);
	}
	
	/**
	 * Checks string is null or empty .
	 */
	protected boolean isEmpty(String s) {
		if (s == null || s.trim().length() == 0) {
			return true;
		}
		return false;
	}
}
