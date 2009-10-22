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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.server.core.InternString;
import com.avaje.ebean.server.deploy.meta.DeployBeanTable;
import com.avaje.ebean.server.deploy.meta.DeployTableJoin;
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

	private static final Logger logger = Logger.getLogger(BeanTable.class.getName());
	
    private final Class<?> beanType;

    /**
     * The base table.
     */
    private final String baseTable;

    private final BeanProperty[] idProperties;
    
    /**
     * Create the BeanTable.
     */
    public BeanTable(DeployBeanTable mutable, BeanDescriptorMap owner) {
        this.beanType = mutable.getBeanType();
        this.baseTable = InternString.intern(mutable.getBaseTable());
        this.idProperties = mutable.createIdProperties(owner);
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
    
	public void createJoinColumn(String foreignKeyPrefix, DeployTableJoin join, boolean reverse) {
		
		boolean complexKey = false;
		BeanProperty[] props = idProperties;
		
		if (idProperties.length == 1){
			if (idProperties[0] instanceof BeanPropertyAssocOne<?>) {
				BeanPropertyAssocOne<?> assocOne = (BeanPropertyAssocOne<?>)idProperties[0];
				props = assocOne.getProperties();
				complexKey = true;
			}
		}
		
		for (int i = 0; i < props.length; i++) {
				
    		String fk = foreignKeyPrefix+"_"+props[i].getDbColumn();
    		String lc = props[i].getDbColumn();
    		
    		if (complexKey){
    			// check to see if we want prefixes by default with complex keys
    			boolean usePrefix = GlobalProperties.getBoolean("ebean.prefixComplexKeys", false);
    			if (!usePrefix){
	    			// just to copy the column name rather than prefix with the foreignKeyPrefix. 
    				// I think that with complex keys this is the more common approach.
	    			String msg = "On table["+baseTable+"] foreign key column ["+lc+"]";
	    			logger.log(Level.FINE, msg);
	    			fk = lc;
    			}
    		} 
    		
    		DeployTableJoinColumn joinCol = new DeployTableJoinColumn(lc, fk);
    		if (reverse){
    			joinCol = joinCol.reverse();
    		}
    		join.addJoinColumn(joinCol);
		}
		
	}
    
}
