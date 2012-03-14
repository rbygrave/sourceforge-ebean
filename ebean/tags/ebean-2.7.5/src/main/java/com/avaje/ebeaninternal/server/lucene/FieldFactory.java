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

import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.TermVector;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class FieldFactory {

    private final boolean numericField;
    private final String fieldName;

    private final Store store;
    private final Index index;
    private final TermVector termVector;
    private final boolean omitTermFreqAndPositions;
    private final float boost;
    
    private final int precisionStep;

    public static FieldFactory numeric(String fieldName, Store store, Index index, float boost, int precisionStep) {
        return new FieldFactory(true, fieldName, store, index, TermVector.NO, true, boost, precisionStep);
    }
    
    public static FieldFactory normal(String fieldName, Store store, Index index, TermVector termVector, boolean omitTermFreqAndPositions, float boost) {
        return new FieldFactory(false, fieldName, store, index, termVector, omitTermFreqAndPositions, boost, 0);
    }
    
    private FieldFactory(boolean numericField, String fieldName, Store store, Index index, TermVector termVector, boolean omitTermFreqAndPositions, float boost, int precisionStep) {
        this.numericField = numericField;
        this.fieldName = fieldName;
        this.store = store;
        this.index = index;
        this.termVector = termVector;
        this.omitTermFreqAndPositions = omitTermFreqAndPositions;
        this.boost = boost;
        this.precisionStep = precisionStep;
    }

    public Fieldable createFieldable() {
        return numericField ? createNumericField() : createNormalField();
    }
    
    private Fieldable createNormalField() {
        Field f = new Field(fieldName, "", store, index, termVector);
        if (boost > 0){
            f.setBoost(boost);
        }
        if (omitTermFreqAndPositions){
        	f.setOmitTermFreqAndPositions(omitTermFreqAndPositions);
        }
        return f;
    }
    
    private Fieldable createNumericField() {
        boolean indexed = index.isIndexed();
        NumericField f = new NumericField(fieldName, precisionStep, store, indexed);
        if (boost > 0){
            f.setBoost(boost);
        }
        return f;
    }
}
