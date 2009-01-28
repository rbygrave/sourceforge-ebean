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
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.persistence.CascadeType;

import org.avaje.ebean.server.deploy.BeanCascadeInfo;
import org.avaje.ebean.server.deploy.meta.DeployBeanDescriptor;
import org.avaje.ebean.server.deploy.meta.DeployBeanProperty;

/**
 * Base class for reading deployment annotations.
 */
public abstract class AnnotationParser {

    final DeployBeanInfo info;
    
    final DeployBeanDescriptor descriptor;
    
    final DeployUtil util;
    
    public AnnotationParser(DeployBeanInfo info){
        this.info = info;
        descriptor = info.getDescriptor();
        util = info.getUtil();
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
    
    /**
     * Return the annotation for the property.
     * <p>
     * Looks first at the field and then at the getter method.
     * </p>
     */
    @SuppressWarnings("unchecked")
	protected Annotation get(DeployBeanProperty prop, Class annClass) {
        Annotation a = null;
        Field field = prop.getField();
        if (field != null){
        	a = field.getAnnotation(annClass);
        }
        if (a == null) {
            Method m = prop.getReadMethod();
            if (m != null) {
                a = m.getAnnotation(annClass);
            }
        }
        return a;
    }
}
