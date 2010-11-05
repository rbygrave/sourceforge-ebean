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
package com.avaje.ebeaninternal.server.query;

import java.util.Set;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LIndexField;

public class LuceneResolvableRequest {

    private final BeanDescriptor<?> beanDescriptor;
    
    private final LIndex luceneIndex;
    
    private final Set<String> resolvePropertyNames;
 
    public LuceneResolvableRequest(BeanDescriptor<?> beanDescriptor, LIndex luceneIndex) {
        this.beanDescriptor = beanDescriptor;
        this.luceneIndex = luceneIndex;
        this.resolvePropertyNames = luceneIndex.getResolvePropertyNames();
    }
    
    public boolean indexContains(String propertyName) {
        return resolvePropertyNames.contains(propertyName);
    }
    
    public LIndexField getSortableProperty(String propertyName){
        return luceneIndex.getIndexFieldDefn().getSortableField(propertyName);
    }

    public BeanDescriptor<?> getBeanDescriptor() {
        return beanDescriptor;
    }
    
}
