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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class IndexFieldDefn {

    public enum Sortable {
        /**
         * Make sure the field is sortable.
         */
        YES,
        
        /**
         * Depends on the type and whether it is Analysed.
         * Text fields that are Analyzed/Tokenized are not sortable.
         */
        DEFAULT
    }
    
    protected final String name;
    
    protected String propertyName;
    
    protected Index index;
    
    protected Store store;
    
    protected Sortable sortable;
    
    protected int precisionStep = -1;
        
    protected float boost;
    
    protected Analyzer queryAnalyzer;
    
    protected Analyzer indexAnalyzer;

    protected String[] properties;
    
    public IndexFieldDefn(String name) {
        this.name = name;
        this.propertyName = name;
    }
    
    public IndexFieldDefn(String name, Store store, Index index, Sortable sortable) {
        this(name);
        this.store = store;
        this.index = index;
        this.sortable = sortable;
    }
    
    public String toString() {
        return name;
    }
    
    /**
     * Use this IndexFieldDefn as a template copying all the settings to a new
     * definition using the new name.
     * 
     * @param name
     *            the name of the new IndexFieldDefn
     */
    public IndexFieldDefn copyField(String name){
        IndexFieldDefn copy = new IndexFieldDefn(name, store, index, sortable);
        copy.setPropertyName(name);
        //copy.setPropertyNames(properties);
        copy.setIndexAnalyzer(indexAnalyzer);
        copy.setQueryAnalyzer(queryAnalyzer);
        copy.setPrecisionStep(precisionStep);
        copy.setBoost(boost);
        
        return copy;
    }

    /**
     * Use this IndexFieldDefn as a template for a CONCATINATED string field.
     */
    public IndexFieldDefn copyFieldConcat(String fieldName, String[] properties){
        IndexFieldDefn copy = copyField(fieldName);
        copy.setPropertyName(null);
        copy.setPropertyNames(properties);
        return copy;
    }
    
    public String getName() {
        return name;
    }
    
    public String getPropertyName() {
        return propertyName;
    }

    public IndexFieldDefn setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public Index getIndex() {
        return index;
    }

    public IndexFieldDefn setIndex(Index index) {
        this.index = index;
        return this;
    }

    public Store getStore() {
        return store;
    }

    public IndexFieldDefn setStore(Store store) {
        this.store = store;
        return this;
    }

    public Sortable getSortable() {
        return sortable;
    }

    public IndexFieldDefn setSortable(Sortable sortable) {
        this.sortable = sortable;
        return this;
    }
    
    public int getPrecisionStep() {
        return precisionStep;
    }

    public IndexFieldDefn setPrecisionStep(int precisionStep) {
        this.precisionStep = precisionStep;
        return this;
    }

    public float getBoost() {
        return boost;
    }

    public void setBoost(float boost) {
        this.boost = boost;
    }

    public Analyzer getQueryAnalyzer() {
        return queryAnalyzer;
    }

    public Analyzer getIndexAnalyzer() {
        return indexAnalyzer;
    }
    
    public IndexFieldDefn setQueryAnalyzer(Analyzer queryAnalyzer) {
        this.queryAnalyzer = queryAnalyzer;
        return this;
    }

    public IndexFieldDefn setIndexAnalyzer(Analyzer indexAnalyzer) {
        this.indexAnalyzer = indexAnalyzer;
        return this;
    }

    public IndexFieldDefn setBothAnalyzers(Analyzer analyzer) {
        this.queryAnalyzer = analyzer;
        this.indexAnalyzer = analyzer;
        return this;
    }

    public String[] getPropertyNames() {
        return properties;
    }

    public void setPropertyNames(String[] properties) {
        this.properties = properties;
    } 
    
}
