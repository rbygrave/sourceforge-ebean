/**
 * Copyright (C) 2009  Robin Bygrave
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
	 *
	 * @param databasePlatform the database platform
	 */
	public void setDatabasePlatform(DatabasePlatform databasePlatform);

	/**
	 * Returns the table name for a given Class.
	 * <p>
	 * This method is always called and should take into account @Table
	 * annotations etc. This means you can choose to override the settings
	 * defined by @Table if you wish.
	 * </p>
	 *
	 * @param beanClass the bean class
	 *
	 * @return the table name for the entity class
	 */
	public TableName getTableName(Class<?> beanClass);

	/**
	 * Return the column name given the property name.
	 *
	 * @return the column from property
	 */
	public String getColumnFromProperty(Class<?> beanClass, String propertyName);

	/**
	 * Return the property name from the column name.
	 * <p>
	 * This is used to help mapping of raw SQL queries onto bean properties.
	 * </p>
	 *
	 * @param beanClass the bean class
	 * @param dbColumnName the db column name
	 *
	 * @return the property from column
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName);


	/**
	 * Return the sequence name given the table name (for DB's that use sequences).
	 * <p>
	 * Typically you might append "_seq" to the table name as an example.
	 * </p>
	 *
	 * @param tableName the table name
	 *
	 * @return the sequence name
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
	 * Returns the index name for a given associated one property.
	 *
	 * @param p the associated bean property
	 * @param ixCount A unique count for the index
	 *
	 * @return the index name
	 */
	public String getIndexName(BeanPropertyAssocOne<?> p, int ixCount);

	/**
	 * Returns the ManyToMany join(intersection) table name.
	 *
	 * @param lhsTable the left hand side bean table
	 * @param rhsTable the right hand side bean table
	 *
	 * @return the many to many join(intersection) table name
	 */
	public String getM2MJoinTableName(BeanTable lhsTable, BeanTable rhsTable);
}