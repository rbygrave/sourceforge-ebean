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

import java.io.StringReader;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.type.ScalarType;

public final class LIndexFieldString extends LIndexFieldBase {
        
    private final Analyzer indexAnalyzer;
    
    public LIndexFieldString(Analyzer queryAnalyzer, String fieldName, FieldFactory fieldFactory, ElPropertyValue property, Analyzer indexAnalyzer) {
        super(queryAnalyzer, fieldName, LLuceneTypes.STRING, property, fieldFactory);
        this.indexAnalyzer = indexAnalyzer;
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

        Object v = doc.get(fieldName);
        if (v != null){
            v = scalarType.luceneFromIndexValue(v);
        }
        property.elSetValue(bean, v, true, false);
    }
    
    
    public DocFieldWriter createDocFieldWriter() {    
        Field f = (Field)fieldFactory.createFieldable();
        return new Writer(property, scalarType, f, indexAnalyzer);
    }

    private static class Writer implements DocFieldWriter {

        private final ElPropertyValue property;
        private final ScalarType<?> scalarType;
        private final Field field;
        private final Analyzer indexAnalyzer;
        
        Writer(ElPropertyValue property, ScalarType<?> scalarType, Field field, Analyzer indexAnalyzer) {
            this.property = property;
            this.scalarType = scalarType;
            this.field = field;
            this.indexAnalyzer = indexAnalyzer;
        }
        
        public void writeValue(Object bean, Document document) {
        
            Object value = property.elGetValue(bean);
    
            if (value == null){
                
            } else {
                String s = (String)scalarType.luceneToIndexValue(value);
                if (indexAnalyzer == null){
                    field.setValue(s);
                } else {
                    // using TokenStream for indexing
                    StringReader sr = new StringReader(s);
                    TokenStream tokenStream = indexAnalyzer.tokenStream(field.name(), sr);
                    field.setTokenStream(tokenStream);
                } 
                document.add(field);
            }
        }
    }
    
}
