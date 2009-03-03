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

import javax.persistence.PersistenceException;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.server.deploy.meta.DeployMapBeanDescriptor;
import com.avaje.ebean.server.type.TypeManager;

/**
 * BeanDecriptor for MapBeans.
 */
public class MapBeanDescriptor extends BeanDescriptor {
    
    private final String derivedIdPropName;
    
    private final float mapLoadFactor;// = 0.75f;
    
    private final int mapInitialCapacity;// = 16;
    
    /**
     * Create for a given plugin.
     */
    public MapBeanDescriptor(TypeManager typeManager, DeployMapBeanDescriptor deploy) {
        super(typeManager, deploy);
        this.derivedIdPropName = deploy.getIdPropertyName();
        this.mapLoadFactor = deploy.getMapLoadFactor();
        this.mapInitialCapacity = deploy.getMapInitialCapacity();
    }

    /**
     * Same as createEntityBean() for MapBeans.
     */
    public Object createVanillaBean() {
    	return createEntityBean();
    }
    
    /**
     * Create a MapBean with the appropiate settings.
     */
    public EntityBean createEntityBean() {
        try {
            MapBean mb = new MapBean(mapInitialCapacity, mapLoadFactor);
            EntityBeanIntercept in = mb._ebean_getIntercept();
            in.setServerName(serverName);
            mb.setTableName(baseTable);
            
            mb.setIdPropertyName(derivedIdPropName);
            
            return mb;

        } catch (Exception ex) {
            throw new PersistenceException(ex);
        }
    }
    
}
