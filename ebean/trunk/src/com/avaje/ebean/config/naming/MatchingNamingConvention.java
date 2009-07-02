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

import java.lang.reflect.Field;

/**
 * The Class MatchingNamingConvention uses the JPA naming strategy
 *
 * <p>The JPA specification states that the in the case of no annotations the
 * name of the class will be take as the table name and the name of a property
 * will be taken as the name of the column.
 * </p>
 */
public class MatchingNamingConvention extends AbstractNamingConvention {

	/**
	 * Instantiates a new matching naming convention.
	 */
	public MatchingNamingConvention() {
		super();
	}

	/**
	 * Instantiates a new matching naming convention.
	 *
	 * @param sequenceFormat the sequence format
	 */
	public MatchingNamingConvention(String sequenceFormat) {
		super(sequenceFormat);
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.naming.DefaultNamingConvention#getColumnFromProperty(java.lang.reflect.Field)
	 */
	public String getColumnFromProperty(Field field) {
		String name = getColumnFromAnnotation(field);

		if (name == null){
			name = field.getName();
		}
		return name;
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.naming.DefaultNamingConvention#getTableNameFromClass(java.lang.Class)
	 */
	public TableName getTableNameFromClass(Class<?> beanClass) {
		TableName tableName = getTableNameFromAnnotation(beanClass);

		if (tableName == null){
			tableName = new TableName(getCatalog(), getSchema(), beanClass.getSimpleName());
		}

		return tableName;
	}

	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.naming.NamingConvention#getPropertyFromColumn(java.lang.Class, java.lang.String)
	 */
	public String getPropertyFromColumn(Class<?> beanClass, String dbColumnName) {
		return dbColumnName;
	}
}
