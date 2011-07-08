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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import com.avaje.ebean.config.lucene.IndexDefn;
import com.avaje.ebean.config.lucene.IndexFieldDefn;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.id.IdBinder;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.type.ScalarType;

public class LIndexFactory {

    private final DefaultLuceneIndexManager manager;

    public LIndexFactory(DefaultLuceneIndexManager manager) {
        this.manager = manager;
    }

    public LIndex create(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor) throws IOException {
        
        Analyzer defaultAnalyzer = manager.getDefaultAnalyzer();
        return new Factory(indexDefn, descriptor, manager, defaultAnalyzer).create();
    }

    static class Factory {

        private final Analyzer defaultAnalyzer;
        private final DefaultLuceneIndexManager manager;
        private final IndexDefn<?> indexDefn;
        private final BeanDescriptor<?> descriptor;

        private Factory(IndexDefn<?> indexDefn, BeanDescriptor<?> descriptor, DefaultLuceneIndexManager manager,
                Analyzer defaultAnalyzer) {
            
            this.indexDefn = indexDefn;
            this.descriptor = descriptor;
            this.defaultAnalyzer = defaultAnalyzer;
            this.manager = manager;
        }

        public LIndex create() throws IOException {

            LIndexFieldsBuilder helper = new LIndexFieldsBuilder(descriptor);

            indexDefn.initialise(helper);

            List<LIndexField> definedFields = new ArrayList<LIndexField>();

            List<IndexFieldDefn> fields = indexDefn.getFields();

            for (int i = 0; i < fields.size(); i++) {
                IndexFieldDefn fieldDefn = fields.get(i);
                LIndexField field = creatField(fieldDefn);
                definedFields.add(field);
            }

            String defaultField = indexDefn.getDefaultField();
            LIndexFields fieldGroup = new LIndexFields(definedFields, descriptor, defaultField);

            Analyzer analyzer = indexDefn.getAnalyzer();
            if (analyzer == null){
                analyzer = defaultAnalyzer;
            }
            
            String indexName = indexDefn.getClass().getName();
            String indexDir = manager.getIndexDirectory(indexName);
            
            return new LIndex(manager, indexName, indexDir, analyzer, descriptor, fieldGroup, indexDefn.getUpdateSinceProperties());
        }

        private ElPropertyValue getProperty(String name) {
            ElPropertyValue prop = descriptor.getElGetValue(name);
            if (prop == null) {
                String msg = "Property [" + name + "] not found on " + descriptor.getFullName();
                throw new NullPointerException(msg);
            }
            return prop;
        }
        
        private LIndexField creatField(IndexFieldDefn fieldDefn) {

            String fieldName = fieldDefn.getName();
            
            Analyzer queryAnalyzer = getQueryAnalyzer(fieldDefn);
            
            Store store = fieldDefn.getStore();
            Index index = fieldDefn.getIndex();
            
            int luceneType = LLuceneTypes.STRING;
            
            Analyzer indexAnalyzer = fieldDefn.getIndexAnalyzer();
            float boost = fieldDefn.getBoost();
            
            String[] propertyNames = fieldDefn.getPropertyNames();
            if (propertyNames != null && propertyNames.length > 0){
                // This is a concatenation of multiple properties 
                ElPropertyValue[] props = new ElPropertyValue[propertyNames.length];
                for (int i = 0; i < props.length; i++) {
                    props[i] = getProperty(propertyNames[i]);
                }
                
                FieldFactory fieldFactory = FieldFactory.normal(fieldName, store, index, boost);
                return new LIndexFieldStringConcat(queryAnalyzer, fieldName, fieldFactory, props, indexAnalyzer);
            } 

            ElPropertyValue prop = getProperty(fieldDefn.getPropertyName());
            BeanProperty beanProperty = prop.getBeanProperty();
            ScalarType<?> scalarType = beanProperty.getScalarType();
            luceneType = scalarType.getLuceneType();
            
            if (beanProperty.isId()){
                IdBinder idBinder = beanProperty.getBeanDescriptor().getIdBinder();
                FieldFactory fieldFactory = FieldFactory.normal(fieldName, store, index, boost);
                return new LIndexFieldId(queryAnalyzer, fieldName, fieldFactory, prop, idBinder);
            }

            if (luceneType == LLuceneTypes.BINARY) {
                FieldFactory fieldFactory = FieldFactory.normal(fieldName, store, index, boost);
                return new LIndexFieldBinary(queryAnalyzer, fieldName, fieldFactory, prop);

            } else if (luceneType == LLuceneTypes.STRING) {
                FieldFactory fieldFactory = FieldFactory.normal(fieldName, store, index, boost);
                return new LIndexFieldString(queryAnalyzer, fieldName, fieldFactory, prop, indexAnalyzer);

            } else {
                // Numeric types including Date and Timestamp
                int precisionStep = fieldDefn.getPrecisionStep();
                if (precisionStep < 0){
                    precisionStep = 8;
                }
                
                FieldFactory fieldFactory = FieldFactory.numeric(fieldName, store, index, boost, precisionStep);
                return new LIndexFieldNumeric(queryAnalyzer, fieldName, fieldFactory, luceneType, prop);
            }
        }

        private Analyzer getQueryAnalyzer(IndexFieldDefn fieldDefn) {
            Analyzer analyzer = fieldDefn.getQueryAnalyzer();
            if (analyzer == null) {
                analyzer = indexDefn.getAnalyzer();
            }
            return analyzer == null ? defaultAnalyzer : analyzer;
        }

    }

}
