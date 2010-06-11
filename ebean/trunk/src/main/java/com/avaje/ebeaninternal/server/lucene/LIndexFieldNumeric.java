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
import org.apache.lucene.document.NumericField;

import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.type.ScalarType;

public final class LIndexFieldNumeric extends LIndexFieldBase {

    public LIndexFieldNumeric(Analyzer queryAnalyzer, String fieldName, FieldFactory fieldFactory, int luceneType, ElPropertyValue property) {
        super(queryAnalyzer, fieldName,luceneType, property, fieldFactory);
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
        return isIndexed() ? propertyName : null;
    }
    
    public DocFieldWriter createDocFieldWriter() {    
        NumericField f = (NumericField)fieldFactory.createFieldable();
        return new Writer(luceneType, property, scalarType, f);
    }

    private static class Writer implements DocFieldWriter {

        private final int luceneType;
        private final ElPropertyValue property;
        private final ScalarType<?> scalarType;
        private final NumericField field;
        
        Writer(int luceneType, ElPropertyValue property, ScalarType<?> scalarType, NumericField field) {
            this.luceneType = luceneType;
            this.property = property;
            this.scalarType = scalarType;
            this.field = field;
        }
        
        public void writeValue(Object bean, Document document) {
    
            Object value = property.elGetValue(bean);
            if (value == null){
                
            } else {
                
                //System.out.println("- write "+field.name()+" "+value);
                
                value = scalarType.luceneToIndexValue(value);
                setValueToField(value);
                document.add(field);
            }
        }
    
        protected void setValueToField(Object value) {
            
            switch (luceneType) {            
            case LLuceneTypes.INT:
                field.setIntValue((Integer)value);
                break;
                
            case LLuceneTypes.LONG:
                field.setLongValue((Long)value);
                break;
    
            case LLuceneTypes.DATE:
                field.setLongValue((Long)value);
                break;
                
            case LLuceneTypes.TIMESTAMP:
                field.setLongValue((Long)value);
                break;
    
            case LLuceneTypes.DOUBLE:
                field.setDoubleValue((Double)value);
                break;
    
            case LLuceneTypes.FLOAT:
                field.setFloatValue((Float)value);
                break;
    
            default:
                throw new RuntimeException("Unhandled type "+luceneType);
            }
            
        }
    }
    
}
