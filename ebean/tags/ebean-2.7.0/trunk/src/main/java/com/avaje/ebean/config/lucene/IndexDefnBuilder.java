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
package com.avaje.ebean.config.lucene;

import java.util.List;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import com.avaje.ebean.config.lucene.IndexFieldDefn.Sortable;

/**
 * A helper that can be used to build/define index fields on an index.
 * 
 * @author rbygrave
 */
public interface IndexDefnBuilder {

    /**
     * Add all the 
     */
    public void addAllFields();

    public IndexDefnBuilder assocOne(String propertyName);

    public IndexFieldDefn addField(IndexFieldDefn fieldDefn);

    public IndexFieldDefn addField(String propertyName);

    public IndexFieldDefn addField(String propertyName, Sortable sortable);

    public IndexFieldDefn addField(String propertyName, Store store, Index index, Sortable sortable);

    public IndexFieldDefn addFieldConcat(String fieldName, String... propertyNames);
    
    public IndexFieldDefn addFieldConcat(String fieldName, Store store, Index index, String... propertyNames);

    public IndexFieldDefn getField(String fieldName);

    public List<IndexFieldDefn> getFields();
    
}
