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
import org.apache.lucene.document.Field.TermVector;

public class IndexFieldDefn {

    public enum Sortable {
    	YES,
    	DEFAULT
    }
    
    protected final String name;
    
    protected String propertyName;
    
    protected Index index;
    
    protected Store store;
    
    protected TermVector termVector;
    
    protected boolean omitTermFreqAndPositions;
    
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
        this(name, store, index, sortable, TermVector.NO, true);
    }
    
    public IndexFieldDefn(String name, Store store, Index index, Sortable sortable, TermVector termVector, boolean omitTermFreqAndPositions) {
        this(name);
        this.store = store;
        this.index = index;
        this.termVector = termVector;
        this.omitTermFreqAndPositions = omitTermFreqAndPositions;
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
        IndexFieldDefn copy = new IndexFieldDefn(name, store, index, sortable, termVector, omitTermFreqAndPositions);
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
    
    /**
     * Return the index field name.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Return the matching bean property name.
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * Set the associated bean property name.
     */
    public IndexFieldDefn setPropertyName(String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    /**
     * Return the index this field belongs to.
     */
    public Index getIndex() {
        return index;
    }

    /**
     * Set the index this field belongs to.
     */
    public IndexFieldDefn setIndex(Index index) {
        this.index = index;
        return this;
    }
    
    public TermVector getTermVector() {
    	return termVector;
    }

	public IndexFieldDefn setTermVector(TermVector termVector) {
    	this.termVector = termVector;
    	return this;
    }

	/**
     * Return the Store option for this field.
     */
    public Store getStore() {
        return store;
    }

    /**
     * Set the Store option for this field.
     */
    public IndexFieldDefn setStore(Store store) {
        this.store = store;
        return this;
    }

    /**
     * Return the Sortable option for this field.
     */
    public Sortable getSortable() {
        return sortable;
    }

    /**
     * Set the Sortable option for this field.
     */
    public IndexFieldDefn setSortable(Sortable sortable) {
        this.sortable = sortable;
        return this;
    }
    
    public boolean isOmitTermFreqAndPositions() {
    	return omitTermFreqAndPositions;
    }

	public IndexFieldDefn setOmitTermFreqAndPositions(boolean omitTermFreqAndPositions) {
    	this.omitTermFreqAndPositions = omitTermFreqAndPositions;
    	return this;
    }

	/**
     * Return the precision step for this field.
     */
    public int getPrecisionStep() {
        return precisionStep;
    }

    /**
     * Set the precision step for this field.
     */
    public IndexFieldDefn setPrecisionStep(int precisionStep) {
        this.precisionStep = precisionStep;
        return this;
    }

    /**
     * Return the boost for this field.
     */
    public float getBoost() {
        return boost;
    }

    /**
     * Set the boost for this field.
     */
    public void setBoost(float boost) {
        this.boost = boost;
    }

    /**
     * Return the Analyzer to use to query this field.
     */
    public Analyzer getQueryAnalyzer() {
        return queryAnalyzer;
    }

    /**
     * Return the Analyzer to use to index the values for this field.
     */
    public Analyzer getIndexAnalyzer() {
        return indexAnalyzer;
    }
    
    /**
     * Set the Analyzer to use to query this field.
     */
    public IndexFieldDefn setQueryAnalyzer(Analyzer queryAnalyzer) {
        this.queryAnalyzer = queryAnalyzer;
        return this;
    }

    /**
     * Set the Analyzer to use to index the values for this field.
     */
    public IndexFieldDefn setIndexAnalyzer(Analyzer indexAnalyzer) {
        this.indexAnalyzer = indexAnalyzer;
        return this;
    }

    /**
     * Set the Analyzer to use to both query and index this field.
     */
    public IndexFieldDefn setBothAnalyzers(Analyzer analyzer) {
        this.queryAnalyzer = analyzer;
        this.indexAnalyzer = analyzer;
        return this;
    }

    /**
     * Return the bean properties associated with this field.
     */
    public String[] getPropertyNames() {
        return properties;
    }

    /**
     * Set the bean properties associated with this field.
     */
    public void setPropertyNames(String[] properties) {
        this.properties = properties;
    } 
    
}
