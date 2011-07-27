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

import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

import com.avaje.ebean.config.lucene.IndexDefnBuilder;
import com.avaje.ebean.config.lucene.IndexFieldDefn;
import com.avaje.ebean.config.lucene.IndexFieldDefn.Sortable;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.type.ScalarType;

public class LIndexFieldsBuilder implements SpiIndexDefnHelper {

    private final BeanDescriptor<?> desc;

    private final LinkedHashMap<String, IndexFieldDefn> fieldDefnMap = new LinkedHashMap<String, IndexFieldDefn>();

    public LIndexFieldsBuilder(BeanDescriptor<?> desc) {
        this.desc = desc;
    }

    public Nested assocOne(String propertyName) {

        BeanProperty beanProperty = desc.getBeanProperty(propertyName);
        if (beanProperty instanceof BeanPropertyAssocOne<?>) {
            BeanPropertyAssocOne<?> assocOne = (BeanPropertyAssocOne<?>) beanProperty;
            BeanDescriptor<?> targetDescriptor = assocOne.getTargetDescriptor();
            return new Nested(this, propertyName, targetDescriptor);
        }

        throw new IllegalArgumentException("Expecing " + propertyName + " to be an AssocOne property of "
                + desc.getFullName());
    }

    public void addAllFields() {
        Iterator<BeanProperty> it = desc.propertiesAll();
        while (it.hasNext()) {
            BeanProperty beanProperty = it.next();
            if (beanProperty instanceof BeanPropertyAssocMany<?>) {

            } else if (beanProperty instanceof BeanPropertyAssocOne<?>) {

            } else {
                addField(beanProperty.getName());
            }
        }
    }
    
    public IndexFieldDefn addField(String propertyName) {
        return addField(propertyName, null);
    }

    public IndexFieldDefn addField(String propertyName, Sortable sortable) {
        return addField(propertyName, null, null, sortable);
    }
    
    public IndexFieldDefn addFieldConcat(String fieldName, String... propertyNames) {
        return addFieldConcat(fieldName, Store.NO, Index.ANALYZED, propertyNames);
    }
    
    public IndexFieldDefn addFieldConcat(String fieldName, Store store, Index index, String... propertyNames) {
        return addPrefixFieldConcat(null, fieldName, store, index, propertyNames);
    }

    public IndexFieldDefn addPrefixField(String prefix, String propertyName, Store store, Index index, Sortable sortable) {
        String fullPath = prefix + "." + propertyName;
        return addField(fullPath, store, index, sortable);
    }
    
    public IndexFieldDefn addPrefixFieldConcat(String prefix, String fieldName, Store store, Index index, String[] propertyNames) {

        if (prefix != null){
            for (int i = 0; i < propertyNames.length; i++) {
                propertyNames[i] = prefix+"."+propertyNames;
            }
        }
        
        IndexFieldDefn fieldDefn = new IndexFieldDefn(fieldName, store, index, Sortable.YES);
        fieldDefn.setPropertyNames(propertyNames);
        
        fieldDefnMap.put(fieldName, fieldDefn);
        return fieldDefn;
    }
    
    public IndexFieldDefn addField(IndexFieldDefn fieldDefn) {
        fieldDefnMap.put(fieldDefn.getName(), fieldDefn);
        return fieldDefn;
    }

    public IndexFieldDefn addField(String propertyName, Store store, Index index, Sortable sortable) {

        ElPropertyValue prop = desc.getElGetValue(propertyName);
        if (prop == null) {
            String msg = "Property [" + propertyName + "] not found on " + desc.getFullName();
            throw new NullPointerException(msg);
        }
        BeanProperty beanProperty = prop.getBeanProperty();
        ScalarType<?> scalarType = beanProperty.getScalarType();
        
        if (store == null) {
            store = isLob(scalarType) ? Store.NO : Store.YES;
        }

        boolean luceneStringType = beanProperty.isId() || isLuceneString(scalarType.getLuceneType());
        if (index == null) {            
            if (beanProperty.isId() || !luceneStringType) {
                index = Index.NOT_ANALYZED;
            } else {
                index = Index.ANALYZED;
            }
        }

        IndexFieldDefn fieldDefn = new IndexFieldDefn(propertyName, store, index, sortable);
        fieldDefnMap.put(propertyName, fieldDefn);
        
        if (luceneStringType && index.isAnalyzed() && Sortable.YES.equals(sortable)) {
            IndexFieldDefn extraFieldDefn = new IndexFieldDefn(propertyName+"_sortonly", Store.NO, Index.NOT_ANALYZED, Sortable.YES);
            extraFieldDefn.setPropertyName(propertyName);
            fieldDefnMap.put(extraFieldDefn.getName(), extraFieldDefn);
        }
        
        return fieldDefn;
    }
    
    public IndexFieldDefn getField(String fieldName) {
        return fieldDefnMap.get(fieldName);
    }

    private boolean isLuceneString(int luceneType) {
        return LLuceneTypes.STRING == luceneType;
    }
    
    private boolean isLob(ScalarType<?> scalarType) {
        int jdbcType = scalarType.getJdbcType();
        switch (jdbcType) {
        case Types.CLOB:
            return true;
        case Types.BLOB:
            return true;
        case Types.LONGVARCHAR:
            return true;
        case Types.LONGVARBINARY:
            return true;
            
        default:
            return false;
        }
    }
    
    public List<IndexFieldDefn> getFields() {

        ArrayList<IndexFieldDefn> fields = new ArrayList<IndexFieldDefn>();
        fields.addAll(fieldDefnMap.values());
        return fields;
    }

    private static class Nested implements SpiIndexDefnHelper {

        private final String path;
        private final BeanDescriptor<?> targetDescriptor;
        private final SpiIndexDefnHelper parent;

        Nested(SpiIndexDefnHelper parent, String path, BeanDescriptor<?> targetDescriptor) {
            this.parent = parent;
            this.path = path;
            this.targetDescriptor = targetDescriptor;
        }

        public IndexDefnBuilder assocOne(String propertyName) {

            BeanProperty beanProperty = targetDescriptor.getBeanProperty(propertyName);
            if (beanProperty instanceof BeanPropertyAssocOne<?>) {
                BeanPropertyAssocOne<?> assocOne = (BeanPropertyAssocOne<?>) beanProperty;
                BeanDescriptor<?> targetDescriptor = assocOne.getTargetDescriptor();
                return new Nested(this, propertyName, targetDescriptor);
            }
            throw new IllegalArgumentException("Expecing " + propertyName + " to be an AssocOne property of "
                    + targetDescriptor.getFullName());
        }

        public void addAllFields() {
            Iterator<BeanProperty> it = targetDescriptor.propertiesAll();
            while (it.hasNext()) {
                BeanProperty beanProperty = it.next();
                if (beanProperty instanceof BeanPropertyAssocMany<?>) {

                } else if (beanProperty instanceof BeanPropertyAssocOne<?>) {

                } else {
                    if (!beanProperty.isTransient()) {
                        addField(beanProperty.getName());
                    }
                }
            }
        }

        public IndexFieldDefn addField(IndexFieldDefn fieldDefn) {
            parent.addField(fieldDefn);
            return fieldDefn;
        }
        
        
        public IndexFieldDefn addField(String propertyName) {
            return addField(propertyName, null);
        }
        
        public IndexFieldDefn addField(String propertyName, Sortable sortable) {
            return addField(propertyName, null, null, sortable);
        }

        public IndexFieldDefn addField(String propertyName, Store store, Index index, Sortable sortable) {
            return parent.addPrefixField(path, propertyName, store, index, sortable);
        }

        public IndexFieldDefn addFieldConcat(String fieldName, String... propertyNames) {
            return addFieldConcat(fieldName, Store.NO, Index.ANALYZED, propertyNames);
        }
        
        public IndexFieldDefn addFieldConcat(String fieldName, Store store, Index index, String... propertyNames) {
            return parent.addPrefixFieldConcat(path, fieldName, store, index, propertyNames);
        }

        public IndexFieldDefn addPrefixFieldConcat(String prefix, String fieldName, Store store, Index index, String[] propertyNames) {
            
            String nestedPath = path+"."+prefix;
            return parent.addPrefixFieldConcat(nestedPath, fieldName, store, index, propertyNames);
        }

        public IndexFieldDefn addPrefixField(String prefix, String propertyName, Store store, Index index, Sortable sortable) {
            String nestedPath = prefix + "." + propertyName;
            return addField(nestedPath, store, index, sortable);
        }
        
        public IndexFieldDefn getField(String fieldName) {
            return parent.getField(fieldName);
        }

        public List<IndexFieldDefn> getFields() {
            return parent.getFields();
        }

    }
}
