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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryParser.QueryParser;

import com.avaje.ebean.text.PathProperties;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.query.SplitName;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryDetail;

public class LIndexFields {

    private final String defaultFieldName;
    
    private final LIndexField[] fields;
    
    private final LIndexField[] readFields;
    
    private final PathProperties pathProperties;
    
    private final OrmQueryDetail ormQueryDetail;
    
    private final HashMap<String,LIndexField> fieldMap = new HashMap<String,LIndexField>();

    private final HashMap<String,LIndexField> sortableExpressionMap = new HashMap<String,LIndexField>();

    private final LinkedHashSet<String> requiredPropertyNames = new LinkedHashSet<String>();
    private final LinkedHashSet<String> resolvePropertyNames = new LinkedHashSet<String>();
    private final LinkedHashSet<String> restorePropertyNames = new LinkedHashSet<String>();

    public LIndexFields(List<LIndexField> definedFields, BeanDescriptor<?> descriptor, String defaultFieldName) {
        
        this.defaultFieldName = defaultFieldName;
        this.fields = definedFields.toArray(new LIndexField[definedFields.size()]);
        for (LIndexField field : fields) {
            fieldMap.put(field.getName(), field);
            field.addIndexRequiredPropertyNames(requiredPropertyNames);
            field.addIndexResolvePropertyNames(resolvePropertyNames);
            field.addIndexRestorePropertyNames(restorePropertyNames);
            
            String sortableProperty = field.getSortableProperty();
            if (sortableProperty != null){
                sortableExpressionMap.put(sortableProperty, field);
            }
        }
        
        this.readFields = createReadFields();
        this.pathProperties = createPathProperties();
        this.ormQueryDetail = createOrmQueryDetail();
    }
    
    public LIndexField getSortableField(String propertyName) {
        return sortableExpressionMap.get(propertyName);
    }
        
    public QueryParser createQueryParser(String fieldName) {
        if (fieldName == null){
            fieldName = defaultFieldName;
        }
        LIndexField fld = fieldMap.get(fieldName);
        if (fld == null){
            throw new NullPointerException("fieldName ["+fieldName+"] not in index?");
        }
        return fld.createQueryParser();
    }
    
    public OrmQueryDetail getOrmQueryDetail() {
        return ormQueryDetail;
    }
    
    public Set<String> getResolvePropertyNames() {
        return resolvePropertyNames;
    }

    public LinkedHashSet<String> getRequiredPropertyNames() {
        return requiredPropertyNames;
    }

    public DocFieldWriter createDocFieldWriter() {
     
        DocFieldWriter[] dw = new DocFieldWriter[fields.length];
        for (int j = 0; j < dw.length; j++) {
            dw[j] = fields[j].createDocFieldWriter();
        }
        return new Writer(dw);
    }
    
    public void readDocument(Document doc, Object bean){
        for (LIndexField indexField : fields) {
            indexField.readValue(doc, bean);
        }
    }
    
    public LIndexField[] getReadFields() {
        return readFields;
    }

    private LIndexField[] createReadFields() {
        
        ArrayList<LIndexField> readFieldList = new ArrayList<LIndexField>();

        for (int i = 0; i < fields.length; i++) {
            LIndexField field = fields[i];
            if (field.isStored() && field.isBeanProperty()) {
                // this is an index field that maps to a
                // bean property and can be read
                readFieldList.add(field);
            }
        }
        return readFieldList.toArray(new LIndexField[readFieldList.size()]);
    }
    
    private PathProperties createPathProperties() {
        
        PathProperties pathProps = new PathProperties();
        
        for (int i = 0; i < readFields.length; i++) {
            LIndexField field = readFields[i];
            String propertyName = field.getName();
               
            // this is an index field that maps to a 
            // bean property and can be read
            
            ElPropertyValue el = field.getElBeanProperty();
            if (el.getBeanProperty().isId()){
                // For @Id properties we chop off the last part of the path
                propertyName = SplitName.parent(propertyName);
            }
            if (propertyName != null){
                String[] pathProp = SplitName.split(propertyName);
                pathProps.addToPath(pathProp[0], pathProp[1]);
            }            
        }
        
        return pathProps;
    }        
    
    private OrmQueryDetail createOrmQueryDetail() {
        
        OrmQueryDetail detail = new OrmQueryDetail();
        
        // transfer PathProperties into OrmQueryDetail
        Iterator<String> pathIt = pathProperties.getPaths().iterator();
        while (pathIt.hasNext()) {
            String path = pathIt.next();
            Set<String> props = pathProperties.get(path);
            detail.getChunk(path, true).setDefaultProperties(null, props);
        }
        
        return detail;
    }

    static class Writer implements DocFieldWriter {
        
        private final DocFieldWriter[] dw;
        
        private Writer(DocFieldWriter[] dw){
            this.dw = dw;
        }

        public void writeValue(Object bean, Document document) {
            for (DocFieldWriter w : dw) {
                w.writeValue(bean, document);
            }
        }
    }
}
