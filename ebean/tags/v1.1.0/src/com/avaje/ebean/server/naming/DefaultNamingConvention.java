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
package com.avaje.ebean.server.naming;

import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.server.lib.util.FactoryHelper;
import com.avaje.ebean.server.lib.util.StringHelper;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Converts from database column names to bean property names etc.
 */
public class DefaultNamingConvention implements NamingConvention {

	/**
	 * refer to getForeignKeyColumn();
	 */
	protected final String foreignKeySuffix;

	/**
	 * CamelCase to underscore converter.
	 */
	protected final PropertyNamingConvention propertyNamingConvention;

	protected final PluginProperties properties;

	protected final boolean mapBeanLowerCase;

	/**
	 * Create the NamingConvention.
	 */
	public DefaultNamingConvention(PluginProperties properties) {
		this.properties = properties;
		this.propertyNamingConvention = createPropertyNamingConvention(properties);

		this.foreignKeySuffix = properties.getProperty("namingconvention.foreignkey.suffix", "Id");

		this.mapBeanLowerCase = properties.getPropertyBoolean("namingconvention.mapbean.lowercase", true);
	}

	/**
	 * Create the PropertyNamingConvention implementation.
	 */
	private PropertyNamingConvention createPropertyNamingConvention(PluginProperties properties) {

		String implName = properties.getProperty("namingconvention.property", null);
		if (implName != null) {
			return (PropertyNamingConvention) FactoryHelper.create(implName);
		}

		if (properties.getPropertyBoolean("namingconvention.property.matching", false)) {
			return new MatchingPropertyNaming();
		}

		// use the default UnderscorePropertyNaming implementation
		boolean forceUpperCase = properties.getPropertyBoolean("namingconvention.property.forceuppercase", false);

		return new UnderscorePropertyNaming(forceUpperCase);		
	}

	public String getTableNameFromClass(Class<?> beanClass) {

		String clsName = beanClass.getName();
		int dp = clsName.lastIndexOf('.');
		if (dp != -1) {
			clsName = clsName.substring(dp + 1);
		}

		return clsName;
	}

	public String getColumnFromProperty(Class<?> beanClass, String beanPropertyName) {
		return propertyNamingConvention.toColumnFromProperty(beanClass, beanPropertyName);
	}

	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		return propertyNamingConvention.toPropertyFromColumn(beanClass, dbColumnName);
	}

	public String getMapBeanPropertyFromColumn(String dbColumnName) {
		if (mapBeanLowerCase) {
			return dbColumnName.toLowerCase();
		}
		return dbColumnName;
	}

	public String getForeignKeyColumn(Class<?> beanClass, String propertyName) {
		String joinedProp = propertyName + foreignKeySuffix;

		return getColumnFromProperty(beanClass, joinedProp);
	}

	public String getForeignKeyProperty(Class<?> beanClass, String dbForeignKeyColumn) {
		
		if (dbForeignKeyColumn.endsWith(foreignKeySuffix)) {
			// trim of the foreignKeySuffix
			int endIndex = dbForeignKeyColumn.length() - foreignKeySuffix.length();
			dbForeignKeyColumn = dbForeignKeyColumn.substring(0, endIndex);

		} else {
			// trim off after last underscore
			int lastUnderscore = dbForeignKeyColumn.lastIndexOf('_');
			if (lastUnderscore > -1) {
				dbForeignKeyColumn = dbForeignKeyColumn.substring(0, lastUnderscore);
			}
		}

		return getPropertyFromColumn(beanClass, dbForeignKeyColumn);
	}

	public String getSequenceName(String tableName) {
		
		String temp = properties.getProperty("namingconvention.sequence.name", "{table}_seq");

		return StringHelper.replaceString(temp, "{table}", tableName);		
	}

	public String getSequenceNextVal(String sequenceName) {

		String temp = properties.getProperty("namingconvention.sequence.nextval", null);
		return StringHelper.replaceString(temp, "{sequence}", sequenceName);
	}

	public String getSelectLastInsertedId(String tableName) {

		String sqlSelect = properties.getProperty("namingconvention.selectLastInsertedId", null);

		return StringHelper.replaceString(sqlSelect, "{table}", tableName);
	}
}
