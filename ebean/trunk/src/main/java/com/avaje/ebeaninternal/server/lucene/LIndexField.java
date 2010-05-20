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

import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;

import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public interface LIndexField {

    public String getName();

    public DocFieldWriter createDocFieldWriter();
    
    /**
     * Add property names that can resolve WHERE clause expressions.
     */
    public void addIndexResolvePropertyNames(Set<String> resolvePropertyNames);
    
    /**
     * Add property names that can be restored/populated from the index.
     */
    public void addIndexRestorePropertyNames(Set<String> restorePropertyNames);
    
    /**
     * Add property names that are required for building the index.
     */
    public void addIndexRequiredPropertyNames(Set<String> requiredPropertyNames);
    
    public String getSortableProperty();
    
    public int getSortType();

    public boolean isIndexed();
    
    public boolean isStored();

    public boolean isBeanProperty();

    public ElPropertyValue getElBeanProperty();
        
    public void readValue(Document doc, Object bean);

    public QueryParser createQueryParser();

}
