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

import com.avaje.ebean.server.deploy.meta.DeployBeanTable;
import com.avaje.ebean.server.deploy.parse.AnnotationBeanTable;
import com.avaje.ebean.server.deploy.parse.DeployUtil;

/**
 * Create a BeanTable. This essentially reads the base table and alias for a
 * given type of bean. You can almost consider it a placeholder for a
 * BeanDescriptor.
 * <p>
 * This is done to get around the bi-directional nature of bean
 * associations. Aka, avoiding an infinite loop situation when defining
 * beans by following their associations and looping back to themselves.
 * </p>
 */
public class BeanTableFactory {

	private final DeployUtil deployUtil;
	
	public BeanTableFactory(DeployUtil deployUtil) {
		this.deployUtil = deployUtil;
	}
	
	/**
     * Create a BeanTable for a given type of bean.
     */
    public BeanTable createBeanTable(Class<?> beanClass) {
    	
        DeployBeanTable beanTable = new DeployBeanTable(beanClass);

        // get base table information
        AnnotationBeanTable annBt = new AnnotationBeanTable(deployUtil, beanTable);
        annBt.parse();
                
        //TODO: parse XML deployment for the beanTable...
        
        return new BeanTable(beanTable);
    }
}
