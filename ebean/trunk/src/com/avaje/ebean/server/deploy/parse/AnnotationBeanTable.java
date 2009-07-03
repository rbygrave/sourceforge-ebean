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
package com.avaje.ebean.server.deploy.parse;

import javax.persistence.Table;

import com.avaje.ebean.config.naming.TableName;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;

/**
 * Read the annotations for BeanTable.
 * <p>
 * Refer to BeanTable but basically determining base table, table alias
 * and the unique id properties.
 * </p>
 */
public class AnnotationBeanTable {

	DeployBeanTable beanTable;

	DeployUtil util;


    public AnnotationBeanTable(DeployUtil util, DeployBeanTable beanTable){
    	this.util = util;
        this.beanTable = beanTable;
    }

    /**
     * Parse the annotations.
     */
    public void parse() {
    	
    	// default the TableName using NamingConvention.
		TableName tableName = util.getTableNameFromClass(beanTable.getBeanType());
		
		// read and apply Table annotation settings if present
		applyTableAnnotation(tableName, beanTable.getBeanType());

		beanTable.setBaseTable(tableName.getQualifiedName());
    }
    
    public static void applyTableAnnotation(TableName tableName, Class<?> beanType) {

    	Table tableAnnotaton = findTable(beanType);
		if (tableAnnotaton != null){
			// override with Table annotation settings
			tableName.apply(tableAnnotaton);
		}
    }
    
    /**
	 * Search the inheritance hierarchy for Table annotation.
	 */
	private static Table findTable(Class<?> cls) {
		if (cls.equals(Object.class)){
			return null;
		} 
		Table table = cls.getAnnotation(Table.class);
		if (table != null){
			return table;
		}
		return findTable(cls.getSuperclass());
	}
}
