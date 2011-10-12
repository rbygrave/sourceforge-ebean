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
package com.avaje.ebeaninternal.server.expression;

import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;

import com.avaje.ebeaninternal.server.core.BasicTypeConverter;
import com.avaje.ebeaninternal.server.expression.SimpleExpression.Op;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;

public class LLuceneRangeExpression {

    private final Op op;
    
    private final Object value;

    private final String propertyName;

    private final int luceneType;    
    
    String description;
    
    boolean minInclusive;
    boolean maxInclusive;
    
    public LLuceneRangeExpression(Op op, Object value, String propertyName, int luceneType) {
        this.op = op;
        this.value = value;
        this.propertyName = propertyName;
        this.luceneType = luceneType;
        
        this.minInclusive = Op.EQ.equals(op) || Op.GT_EQ.equals(op);
        this.maxInclusive = Op.EQ.equals(op) || Op.LT_EQ.equals(op);
        
        description = propertyName+op.shortDesc()+value;
    }
    
    public String getDescription() {
        return description;
    }

    public Query buildQuery() {
                
        switch (luceneType) {
        case LLuceneTypes.INT:
            return createIntRange(); 
        case LLuceneTypes.LONG:
            return createLongRange(); 
        case LLuceneTypes.DOUBLE:
            return createDoubleRange(); 
        case LLuceneTypes.FLOAT:
            return createFloatRange(); 

        case LLuceneTypes.DATE:
            return createLongRange(); 
        case LLuceneTypes.TIMESTAMP:
            return createLongRange(); 

        default:
            throw new RuntimeException("Unhandled type "+luceneType);
        }
    }
    
    private Query createIntRange() {
        
        Integer intVal = BasicTypeConverter.toInteger(value);;
        
        Integer min = intVal;
        Integer max = intVal;
        
        if (Op.EQ.equals(op)){
            
        } else if (Op.GT.equals(op) || Op.GT_EQ.equals(op)) {
            max = Integer.MAX_VALUE;
        } else if (Op.LT.equals(op) || Op.LT_EQ.equals(op)) {
            min = Integer.MIN_VALUE;
        }
        
        return NumericRangeQuery.newIntRange(propertyName, min, max, minInclusive, maxInclusive);
    }

    private Query createLongRange() {
        
        Long longVal = BasicTypeConverter.toLong(value);
        Long min = longVal;
        Long max = longVal;
        
        if (Op.EQ.equals(op)){
            
        } else if (Op.GT.equals(op) || Op.GT_EQ.equals(op)) {
            max = Long.MAX_VALUE;
        } else if (Op.LT.equals(op) || Op.LT_EQ.equals(op)) {
            min = Long.MIN_VALUE;
        }
        
        return NumericRangeQuery.newLongRange(propertyName, min, max, minInclusive, maxInclusive);
    }
    
    private Query createFloatRange() {
        
        Float floatVal = BasicTypeConverter.toFloat(value);
        Float min = floatVal;
        Float max = floatVal;
        
        if (Op.EQ.equals(op)){
            
        } else if (Op.GT.equals(op) || Op.GT_EQ.equals(op)) {
            max = Float.MAX_VALUE;
        } else if (Op.LT.equals(op) || Op.LT_EQ.equals(op)) {
            min = Float.MIN_VALUE;
        }
        
        return NumericRangeQuery.newFloatRange(propertyName, min, max, minInclusive, maxInclusive);
    }

    private Query createDoubleRange() {
        
        Double doubleVal = BasicTypeConverter.toDouble(value);
        Double min = doubleVal;
        Double max = doubleVal;
        
        if (Op.EQ.equals(op)){
            
        } else if (Op.GT.equals(op) || Op.GT_EQ.equals(op)) {
            max = Double.MAX_VALUE;
        } else if (Op.LT.equals(op) || Op.LT_EQ.equals(op)) {
           min = Double.MIN_VALUE;
        }
        
        return NumericRangeQuery.newDoubleRange(propertyName, min, max, minInclusive, maxInclusive);
    }
}
