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
package com.avaje.ebean.server.deploy;

import com.avaje.ebean.server.core.InternString;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;
import com.avaje.ebean.server.deploy.meta.DeployTableJoinColumn;


/**
 * Used for associated beans in place of a BeanDescriptor. This is done to avoid
 * recursion issues due to the potentially bi-directional and circular
 * relationships between beans.
 * <p>
 * It holds the main deployment information and not all the detail that is held
 * in a BeanDescriptor.
 * </p>
 */
public class BeanTable {

    private final Class<?> beanType;

    /**
     * The base table.
     */
    private final String baseTable;

    private final BeanProperty[] idProperties;
    /**
     * Create the BeanTable.
     */
    public BeanTable(DeployBeanTable mutable) {
        this.beanType = mutable.getBeanType();
        this.baseTable = InternString.intern(mutable.getBaseTable());
        this.idProperties = mutable.getIdProperties();
    }
    
    public String toString(){
    	return baseTable; 
    }
    
    /**
     * Return the base table for this BeanTable.
     * This is used to determine the join information
     * for associations.
     */
    public String getBaseTable() {
        return baseTable;
    }
    
    /**
     * Return the Id properties.
     */
    public BeanProperty[] getIdProperties() {
		return idProperties;
	}

	/**
     * Return the class for this beanTable.
     */
    public Class<?> getBeanType() {
        return beanType;
    }
    
	public DeployTableJoinColumn createJoinColumn(String foreignKeyPrefix) {
    	if (idProperties.length == 1){
    		String fk = foreignKeyPrefix+"_"+idProperties[0].getDbColumn();
    		String lc = idProperties[0].getDbColumn();
    		return new DeployTableJoinColumn(lc, fk);
    	}
    	return null;
	}
    
}
