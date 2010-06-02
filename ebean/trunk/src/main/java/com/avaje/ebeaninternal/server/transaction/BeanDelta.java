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
package com.avaje.ebeaninternal.server.transaction;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;

public class BeanDelta {

    private final List<BeanDeltaProperty> properties;
    
    private final BeanDescriptor<?> beanDescriptor;
    
    private final Object id;
    
    public BeanDelta(BeanDescriptor<?> beanDescriptor, Object id) {
        this.beanDescriptor = beanDescriptor;
        this.id = id;
        this.properties = new ArrayList<BeanDeltaProperty>();
    }
    
    public BeanDescriptor<?> getBeanDescriptor() {
        return beanDescriptor;
    }

    public Object getId() {
        return id;
    }

    public void add(BeanProperty beanProperty, Object value) {
        this.properties.add(new BeanDeltaProperty(beanProperty, value));
    }
    
    public void add(BeanDeltaProperty propertyDelta) {
        this.properties.add(propertyDelta);
    }
    
    public void apply(Object bean) {
        
        for (int i = 0; i < properties.size(); i++) {
            properties.get(i).apply(bean);
        }
    }
}
