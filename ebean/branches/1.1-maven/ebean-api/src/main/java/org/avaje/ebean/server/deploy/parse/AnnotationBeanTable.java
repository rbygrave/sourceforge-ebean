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
package org.avaje.ebean.server.deploy.parse;

import java.lang.annotation.Annotation;

import javax.persistence.Table;

import org.avaje.ebean.server.deploy.meta.DeployBeanTable;

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
        read(beanTable.getBeanType());
        
    	String baseTable = beanTable.getBaseTable();
    	if (baseTable == null){
    		// default the tableName using NamingConvention.
    		// JPA Spec defines this as the class name
    		String tableName = util.getTableNameFromClass(beanTable.getBeanType());

    		setTable(tableName);
    	}
    }


    private void read(Class<?> cls) {
        Annotation[] anns = cls.getAnnotations();
        for (int i = 0; i < anns.length; i++) {
            if (anns[i] instanceof Table) {
                Table tableAnn = (Table) anns[i];
                setTable(tableAnn.name());
            }
        }
    }
    
    private void setTable(String table) {
        if (table != null && table.trim().length() > 0) {
            
        	// the first alias set so will be good
            String alias = util.getPotentialAlias(table);

            table = util.convertQuotedIdentifiers(table);
    
            beanTable.setBaseTable(table);
            beanTable.setBaseTableAlias(alias);            
        }
    }
   
}
