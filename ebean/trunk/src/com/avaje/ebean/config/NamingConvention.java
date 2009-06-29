/**
 * Imilia Interactive Mobile Applications GmbH
 * Copyright (c) 2009 - all rights reserved
 *
 * Created on: Jun 29, 2009
 * Created by: emcgreal
 */
package com.avaje.ebean.config;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.BeanTable;

/**
 * Defines the naming convention for converting between logical property names
 * and physical DB column names etc.
 * <p>
 * The main goal of the naming convention is to reduce the amount of
 * configuration required in the mapping (especially when mapping between column
 * and property names).
 * </p>
 * <p>
 * Note that if you do not define a NamingConvention the default one will be used and you can
 * configure it's behaviour via properties.
 * </p>
 */
public interface NamingConvention {

	/**
	 * Set the associated DatabasePlaform.
	 * <p>
	 * This is set after the DatabasePlatform has been associated.
	 * </p>
	 * <p>
	 * The purpose of this is to enable NamingConvention to be able to support
	 * database platform specific configuration.
	 * </p>
	 */
	public void setDatabasePlatform(DatabasePlatform databasePlatform);

	/**
	 * Returns the table name for a given Class when the @Table annotation has
	 * not set the table name.
	 */
	public String getTableNameFromClass(Class<?> beanClass);

	/**
	 * Return the column name given the property name.
	 */
	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName);

	/**
	 * Return the property name from the column name.
	 * <p>
	 * This is used to help mapping of raw SQL queries onto bean properties.
	 * </p>
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName);


	/**
	 * Return the sequence name given the table name (for DB's that use sequences).
	 * <p>
	 * Typically you might append "_seq" to the table name as an example.
	 * </p>
	 */
	public String getSequenceName(String tableName);


	/**
	 * Gets the foreign key name.
	 *
	 * @param property the property
	 * @param fkCount the fk count
	 *
	 * @return the foreign key name
	 */
	public String getForeignKeyName(BeanPropertyAssocOne<?> property, int fkCount);


	/**
	 * Gets the index name.
	 *
	 * @param p the p
	 * @param ixCount the ix count
	 *
	 * @return the index name
	 */
	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount);

	/**
	 * Gets the ManyToMany join table name.
	 *
	 * @param lhsTable the lhs table
	 * @param rhsTable the rhs table
	 *
	 * @return the m2m join table name
	 */
	public String getM2MJoinTableName(BeanTable lhsTable, BeanTable rhsTable);
}