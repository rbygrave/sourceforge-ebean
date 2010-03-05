/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebeaninternal.server.deploy;

import com.avaje.ebeaninternal.server.deploy.meta.DeployBeanProperty;
import com.avaje.ebeaninternal.server.type.CtCompoundProperty;

/**
 * A BeanProperty owned by a Compound value object that maps to 
 * a real scalar type.
 * 
 * @author rbygrave
 */
public class BeanPropertyCompoundScalar extends BeanProperty {

    private final BeanPropertyCompoundRoot rootProperty;
    
    private final CtCompoundProperty ctProperty;
    
    public BeanPropertyCompoundScalar(BeanPropertyCompoundRoot rootProperty, DeployBeanProperty scalarDeploy, CtCompoundProperty ctProperty) {
        
        super(scalarDeploy);
        this.rootProperty = rootProperty;
        this.ctProperty = ctProperty;
    }

    @Override
    public Object getValue(Object valueObject) {
        return ctProperty.getValue(valueObject);
    }

    @Override
    public void setValue(Object bean, Object value) {
        setValueInCompound(bean, value, false);        
    }
    
    public void setValueInCompound(Object bean, Object value, boolean intercept) {
        
        Object compoundValue = ctProperty.setValue(bean, value);
        
        if (compoundValue != null){
            // we are at the top level and we have a compound value
            // that we can set using the root property
            if (intercept){
                rootProperty.setValueIntercept(bean, compoundValue);
            } else {
                rootProperty.setValue(bean, compoundValue);
            }
        }
    }

    /**
     * No interception on embedded scalar values inside a CVO.
     */
    @Override
    public void setValueIntercept(Object bean, Object value) {
        setValueInCompound(bean, value, true);
    }

    /**
     * No interception on embedded scalar values inside a CVO.
     */
    @Override
    public Object getValueIntercept(Object bean) {
        return getValue(bean);
    }

    @Override
    public Object elGetReference(Object bean) {
        return getValue(bean);
    }

    @Override
    public Object elGetValue(Object bean) {
        return getValue(bean);
    }

    @Override
    public void elSetReference(Object bean) {
        super.elSetReference(bean);
    }

    @Override
    public void elSetValue(Object bean, Object value, boolean populate, boolean reference) {
        super.elSetValue(bean, value, populate, reference);
    }

    
}
