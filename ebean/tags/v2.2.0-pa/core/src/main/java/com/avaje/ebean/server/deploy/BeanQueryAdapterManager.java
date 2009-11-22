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
package com.avaje.ebean.server.deploy;

import java.util.List;
import java.util.logging.Logger;

import com.avaje.ebean.event.BeanQueryAdapter;
import com.avaje.ebean.server.core.BootupClasses;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;

/**
 * Default implementation for creating BeanControllers.
 */
public class BeanQueryAdapterManager {

	private static final Logger logger = Logger.getLogger(BeanQueryAdapterManager.class.getName());

    private final List<BeanQueryAdapter> list;
    
    public BeanQueryAdapterManager(BootupClasses bootupClasses){
    	
    	list = bootupClasses.getBeanQueryAdapters();
    }
	
    public int getRegisterCount() {
		return list.size();
	}
	
    /**
     * Return the BeanPersistController for a given entity type.
     */
	public void addQueryAdapter(DeployBeanDescriptor<?> deployDesc){
		
		for (int i = 0; i < list.size(); i++) {
			BeanQueryAdapter c = list.get(i);
			if (c.isRegisterFor(deployDesc.getBeanType())){
				logger.fine("BeanQueryAdapter on[" + deployDesc.getFullName() + "] " + c.getClass().getName());
				deployDesc.addQueryAdapter(c);
			}
		}		
    }
    
}
