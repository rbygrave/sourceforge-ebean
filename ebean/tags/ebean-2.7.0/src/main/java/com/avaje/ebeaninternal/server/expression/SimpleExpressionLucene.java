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

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;

import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;
import com.avaje.ebeaninternal.server.expression.SimpleExpression.Op;
import com.avaje.ebeaninternal.server.lucene.LLuceneTypes;
import com.avaje.ebeaninternal.server.type.ScalarType;

public class SimpleExpressionLucene {

    public SpiLuceneExpr addLuceneQuery(SpiExpressionRequest request, SimpleExpression.Op type, String propertyName, Object value, ElPropertyValue prop) {

        try {
            if (prop == null){
                throw new RuntimeException("Property not found? "+propertyName);
            } 
            BeanProperty beanProperty = prop.getBeanProperty();
            ScalarType<?> scalarType = beanProperty.getScalarType();
            
            int luceneType = scalarType.getLuceneType();
            if (LLuceneTypes.STRING == luceneType){
                
                Object lucVal = (String)scalarType.luceneToIndexValue(value);
    
                if (Op.EQ.equals(type)){
                    String desc = propertyName+" = "+lucVal.toString();
                    QueryParser queryParser = request.getLuceneIndex().createQueryParser(propertyName);
                    return new LuceneExprResponse(queryParser.parse(lucVal.toString()), desc);
                }
                if (Op.NOT_EQ.equals(type)){
                    String desc = propertyName+" != "+lucVal.toString();
                    QueryParser queryParser = request.getLuceneIndex().createQueryParser(propertyName);
                    return new LuceneExprResponse(queryParser.parse("-"+propertyName+"("+lucVal.toString()+")"), desc);
                } 
                throw new RuntimeException("String type only supports EQ and NOT_EQ - "+type);
            }
            
            // Must be a number range expression
            LLuceneRangeExpression exp = new LLuceneRangeExpression(type, value, propertyName, luceneType);
            return new LuceneExprResponse(exp.buildQuery(), exp.getDescription());
            
        } catch (ParseException e){
            throw new PersistenceLuceneParseException(e);
        }
    }
}
