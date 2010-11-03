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
package com.avaje.ebeaninternal.server.lucene;

import java.util.List;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;

import com.avaje.ebean.OrderBy;
import com.avaje.ebean.OrderBy.Property;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;

/**
 * Convert OrderBy to a Lucene Sort if possible.
 * 
 * @author rbygrave
 */
public class LLuceneSortResolve {

    private final LuceneResolvableRequest req;
    
    private final OrderBy<?> orderBy;
    
    private Sort sort;
    
    private boolean isResolved;
    
    private String unsortableField;
    
    public LLuceneSortResolve(LuceneResolvableRequest req, OrderBy<?> orderBy) {
        this.req = req;
        this.orderBy = orderBy;
        this.isResolved = resolve();
    }
    
    public boolean isResolved() {
        return isResolved;
    }
    
    public Sort getSort() {
        return sort;
    }

    public String getUnsortableField() {
        return unsortableField;
    }

    private boolean resolve() {
        
        BeanDescriptor<?> beanDescriptor = req.getBeanDescriptor();
        
        if (orderBy != null){
            
            List<Property> properties = orderBy.getProperties();
    
            SortField[] sortFields = new SortField[properties.size()];
    
            for (int i = 0; i < properties.size(); i++) {
                Property property = properties.get(i);
                SortField sf = createSortField(property, beanDescriptor);
                if (sf == null){
                    // field not supported by the index
                    unsortableField = property.getProperty();
                    return false;
                }
                sortFields[i] = sf;
            }
            
            sort = new Sort(sortFields);
        }
        return true;
    }
    
    private SortField createSortField(Property property, BeanDescriptor<?> beanDescriptor) {
                
        String propName = property.getProperty();
        
        LIndexField sortField = req.getSortableProperty(propName);
        if (sortField == null){
            // order by property not supported by index
            return null;
        }
        
        int sortType = sortField.getSortType();
        return sortType == -1 ? null : new SortField(sortField.getName(), sortType, !property.isAscending());
    }
    
    
}
