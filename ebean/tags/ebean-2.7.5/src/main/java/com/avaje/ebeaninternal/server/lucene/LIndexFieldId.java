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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;

import com.avaje.ebeaninternal.server.deploy.id.IdBinder;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public final class LIndexFieldId extends LIndexFieldBase {
        
    private final IdBinder idBinder;
    
    public LIndexFieldId(Analyzer queryAnalyzer, String fieldName, FieldFactory fieldFactory, ElPropertyValue property, IdBinder idBinder) {
        super(queryAnalyzer, fieldName, LLuceneTypes.STRING, property, fieldFactory);
        this.idBinder = idBinder;
    }
    
    public Term createTerm(Object id) {
        String termVal = idBinder.writeTerm(id);
        return new Term(fieldName, termVal);
    }
    
    public void addIndexResolvePropertyNames(Set<String> resolvePropertyNames) {
        if (propertyName != null && isIndexed()){
            resolvePropertyNames.add(propertyName);
        }
    }

    public void addIndexRestorePropertyNames(Set<String> restorePropertyNames) {
        if (propertyName != null && isStored()){
            restorePropertyNames.add(propertyName);
        }
    }
    
    public String getSortableProperty() {
        if (isIndexed() && !isTokenized()) {
            // must be indexed and not tokenized to be
            // able to use this in a sort expression
            return propertyName;
        }
        return null;
    }

    @Override
    public void readValue(Document doc, Object bean){

        String v = doc.get(fieldName);
        if (v != null){
            Object id = idBinder.readTerm(v);
            property.elSetValue(bean, id, true, false);
        }
    }
    
    
    public DocFieldWriter createDocFieldWriter() {    
        Field f = (Field)fieldFactory.createFieldable();
        return new Writer(property, f, idBinder);
    }

    private static class Writer implements DocFieldWriter {

        private final IdBinder idBinder;
        private final ElPropertyValue property;
        private final Field field;
        
        Writer(ElPropertyValue property, Field field, IdBinder idBinder) {
            this.property = property;
            this.field = field;
            this.idBinder = idBinder;
        }
        
        public void writeValue(Object bean, Document document) {
        
            Object value = property.elGetValue(bean);
    
            if (value == null){
                
            } else {
                String writeTerm = idBinder.writeTerm(value);
                field.setValue(writeTerm);
                document.add(field);
            }
        }
    }
    
}
