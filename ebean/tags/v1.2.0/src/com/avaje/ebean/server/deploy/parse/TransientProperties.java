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

import java.util.List;

import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import com.avaje.ebean.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.meta.DeployBeanPropertyAssocOne;

/**
 * Mark transient properties.
 */
public class TransientProperties {
		
	public TransientProperties() {
	}
	
    /**
     * Mark any additional properties as transient.
     */
    public void process(DeployBeanDescriptor<?> desc) {
        
        List<DeployBeanProperty> props = desc.propertiesBase();
        for (int i = 0; i < props.size(); i++) {
        	DeployBeanProperty prop = props.get(i);
            if (!prop.isDbRead() && !prop.isDbWrite()) {
            	// non-transient...
            	prop.setTransient(true);
            }
		}

        List<DeployBeanPropertyAssocOne<?>> ones = desc.propertiesAssocOne();
        for (int i = 0; i < ones.size(); i++) {
        	DeployBeanPropertyAssocOne<?> prop = ones.get(i);
            if (prop.getBeanTable() == null) {
                if (!prop.isEmbedded()) {
                	prop.setTransient(true);
                }
            }
        }

        List<DeployBeanPropertyAssocMany<?>> manys = desc.propertiesAssocMany();
        for (int i = 0; i < manys.size(); i++) {
        	DeployBeanPropertyAssocMany<?> prop = manys.get(i);
        	if (prop.getBeanTable() == null) {
            	prop.setTransient(true);
            }
        }
                
    }
}
