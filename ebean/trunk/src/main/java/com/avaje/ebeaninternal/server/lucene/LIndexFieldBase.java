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
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.SortField;
import org.apache.lucene.util.Version;

import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.query.SplitName;
import com.avaje.ebeaninternal.server.type.ScalarType;

public abstract class LIndexFieldBase implements LIndexField {

    protected final Analyzer queryAnalyzer;
    
    protected final String fieldName;
    
    protected final String propertyName;
    
    protected final int luceneType;
    
    protected final int sortType;
    
    protected final ElPropertyValue property;
    
    protected final ScalarType<?> scalarType;
    
    protected final FieldFactory fieldFactory;
    
    protected final boolean indexed;
    protected final boolean stored;
    protected final boolean tokenized;
    
    public LIndexFieldBase(Analyzer queryAnalyzer, String fieldName, int luceneType, ElPropertyValue property, FieldFactory fieldFactory) {
        this.queryAnalyzer = queryAnalyzer;
        this.fieldName = fieldName;
        this.luceneType = luceneType;
        this.sortType = getSortType(luceneType);
        this.property = property;
        this.fieldFactory = fieldFactory;
        
        Fieldable fieldPrototype = fieldFactory.createFieldable();
        this.indexed = fieldPrototype.isIndexed();
        this.stored = fieldPrototype.isStored();
        this.tokenized = fieldPrototype.isTokenized();
        
        if (property == null){
            this.scalarType = null;
            this.propertyName = null;
        } else {
            this.scalarType = property.getBeanProperty().getScalarType();
            this.propertyName = SplitName.add(property.getElPrefix(), property.getName());
        }
    }
    
    public String toString() {
        return propertyName;
    }

    public void addIndexRequiredPropertyNames(Set<String> requiredPropertyNames) {
        if (propertyName != null){
            requiredPropertyNames.add(propertyName);
        }
    }

    public int getSortType() {
        return sortType;
    }

    public QueryParser createQueryParser() {
        return new QueryParser(Version.LUCENE_30, fieldName, queryAnalyzer);
    }

    public String getName() {
        return fieldName;
    }
    
    public boolean isIndexed() {
        return indexed;
    }
    
    public boolean isStored() {
        return stored;
    }
    
    public boolean isTokenized() {
        return tokenized;
    }
    
    public boolean isBeanProperty() {
        return property != null;
    }
    
    public int getPropertyOrder() {
        return property == null ? 0 : property.getDeployOrder();
    }

    public ElPropertyValue getElBeanProperty() {
        return property;
    }

    public void readValue(Document doc, Object bean){

        Object v = readIndexValue(doc);
        if (v != null){
            v = scalarType.luceneFromIndexValue(v);
        }
        property.elSetValue(bean, v, true, false);
    }
    
    protected Object readIndexValue(Document doc){
        
        String s = doc.get(fieldName);
        if (s == null){
            return null;
        }
        
        switch (luceneType) {
        case LLuceneTypes.INT:
            return Integer.parseInt(s);

        case LLuceneTypes.LONG:
            return Long.parseLong(s);

        case LLuceneTypes.DATE:
            return Long.parseLong(s);
            
        case LLuceneTypes.TIMESTAMP:
            return Long.parseLong(s);

        case LLuceneTypes.DOUBLE:
            return Double.parseDouble(s);

        case LLuceneTypes.FLOAT:
            return Float.parseFloat(s);

        default:
            throw new RuntimeException("Unhandled type "+luceneType);
        }
    }
    
    private int getSortType(int luceneType){
        switch (luceneType) {
        case LLuceneTypes.INT:
            return SortField.INT;
        case LLuceneTypes.LONG:
            return SortField.LONG;
        case LLuceneTypes.DATE:
            return SortField.LONG;
        case LLuceneTypes.TIMESTAMP:
            return SortField.LONG;
        case LLuceneTypes.DOUBLE:
            return SortField.DOUBLE;
        case LLuceneTypes.FLOAT:
            return SortField.FLOAT;
        case LLuceneTypes.STRING:
            return SortField.STRING;

        default:
            return -1;
        }
    }
}
