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
package org.avaje.ebean.server.deploy.meta;

import java.util.List;

import org.avaje.ebean.MapBean;
import org.avaje.ebean.server.deploy.BeanDescriptorOwner;

/**
 * BeanDecriptor for MapBeans.
 */
public class DeployMapBeanDescriptor extends DeployBeanDescriptor {
    
    private float mapLoadFactor = 0.75f;
    
    private int mapInitialCapacity = 16;
    
    /**
     * Create for a given plugin.
     */
    public DeployMapBeanDescriptor(BeanDescriptorOwner owner) {
        super(owner, MapBean.class);
        factoryType = MapBean.class;
       
    }

    @Override
    public String getFullName(){
    	return "table["+getBaseTable()+"]";
    }

	/**
     * Return the loadFactor used for Maps of this type.
     */
    public float getMapLoadFactor() {
		return mapLoadFactor;
	}

    /**
     * Set the loadFactor used for Maps of this type.
     */
	public void setMapLoadFactor(float mapLoadFactor) {
		this.mapLoadFactor = mapLoadFactor;
	}

	/**
	 * Return the initialCapacity used for Maps of this type.
	 */
	public int getMapInitialCapacity() {
    	return mapInitialCapacity;
    }
    
	/**
	 * set the initialCapacity used for Maps of this type.
	 * <p>
	 * If initialCapacity is less than 2 then it will be
	 * estimated based on the number of properties and the
	 * loadFactor.
	 * </p>
	 */
    public int setMapInitialCapacity(int initialCapacity) {
    	if (initialCapacity > 2){
    		this.mapInitialCapacity = initialCapacity;
    	} else {
	    	int propertyCount = propMap.size();
	    	float initCap = (propertyCount)/mapLoadFactor;
	    	this.mapInitialCapacity = (int)initCap + 1;
    	}
    	return mapInitialCapacity;
    }
    
    public String getUidPropertyName() {
		List<DeployBeanProperty> uids = propertiesId();
        if (uids.size() == 1){
        	return uids.get(0).getName();
        } else {
        	return null;
        }
    }
    
    /**
     * Returns the base table name.
     */
    public String getTypeDescription() {
    	return baseTable;
    }
    
}
