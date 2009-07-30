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

import javax.persistence.CascadeType;

import com.avaje.ebean.server.deploy.BeanCascadeInfo;
import com.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;

/**
 * Base class for reading deployment annotations.
 */
public abstract class AnnotationParser extends AnnotationBase {

	protected final DeployBeanInfo<?> info;
    
    protected final DeployBeanDescriptor<?> descriptor;
        
    protected final Class<?> beanType;
    
    public AnnotationParser(DeployBeanInfo<?> info){
    	super(info.getUtil());
        this.info = info;
        this.beanType = info.getDescriptor().getBeanType();
        this.descriptor = info.getDescriptor();
    }
    
    /**
     * read the deployment annotations.
     */
    public abstract void parse();
    
    /**
     * Helper method to set cascade types to the CascadeInfo on BeanProperty.
     */
    protected void setCascadeTypes(CascadeType[] cascadeTypes, BeanCascadeInfo cascadeInfo) {
        if (cascadeTypes != null && cascadeTypes.length > 0) {
            cascadeInfo.setTypes(cascadeTypes);
        }
    }
}
