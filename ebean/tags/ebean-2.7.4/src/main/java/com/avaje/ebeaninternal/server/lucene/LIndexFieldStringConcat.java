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
import com.avaje.ebeaninternal.server.query.SplitName;
import com.avaje.ebeaninternal.server.type.ScalarType;

public final class LIndexFieldStringConcat extends LIndexFieldBase {

    private final ElPropertyValue[] properties;
    
    private final ScalarType<?>[] scalarTypes;
    
    private final Analyzer indexAnalyzer;
    
    public LIndexFieldStringConcat(Analyzer queryAnalyzer, String fieldName, FieldFactory fieldFactory, ElPropertyValue[] properties, Analyzer indexAnalyzer) {
        super(queryAnalyzer, fieldName, LLuceneTypes.STRING, null, fieldFactory);
        this.properties = properties;
        this.indexAnalyzer = indexAnalyzer;
        this.scalarTypes = new ScalarType[properties.length];
        for (int i = 0; i < scalarTypes.length; i++) {
            scalarTypes[i] = properties[i].getBeanProperty().getScalarType();
        }
    }
    
    @Override
    public void addIndexRequiredPropertyNames(Set<String> requiredPropertyNames) {
        for (int i = 0; i < properties.length; i++) {
            String prefix = properties[i].getElPrefix();
            String name = properties[i].getName();
            String fullPath = SplitName.add(prefix, name);
            requiredPropertyNames.add(fullPath);
        }
    }

    public void addIndexResolvePropertyNames(Set<String> resolvePropertyNames) {
        // can't be used to resolve a bean property expression
    }

    public void addIndexRestorePropertyNames(Set<String> restorePropertyNames) {
        // can't restore any specific property
    }
    
    public String getSortableProperty() {
        // Can't use in sort/OrderBy 
        return null;
    }

    public boolean isBeanProperty() {
        return false;
    }

    @Override
    public void readValue(Document doc, Object bean){
    }
    
    public DocFieldWriter createDocFieldWriter() {
        Field f = (Field)fieldFactory.createFieldable();
        return new Writer(f, properties, scalarTypes, indexAnalyzer);
    }

    private static class Writer implements DocFieldWriter {
        
        private final Field field;
        private final ElPropertyValue[] properties;
        private final ScalarType<?>[] scalarTypes;
        
        private final Analyzer indexAnalyzer;
        
        private Writer(Field field, ElPropertyValue[] properties, ScalarType<?>[] scalarTypes, Analyzer indexAnalyzer) {
            this.field = field;
            this.properties = properties;
            this.scalarTypes = scalarTypes;
            this.indexAnalyzer = indexAnalyzer;
        }

        public void writeValue(Object bean, Document document) {
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < properties.length; i++) {
                Object value = properties[i].elGetValue(bean);
                if (value != null){
                    String s = (String)scalarTypes[i].luceneToIndexValue(value);
                    sb.append(s);
                    sb.append(" ");
                }            
            }
            //System.out.println("- write "+field.name()+" "+sb.toString());
            
            String s = sb.toString();
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
