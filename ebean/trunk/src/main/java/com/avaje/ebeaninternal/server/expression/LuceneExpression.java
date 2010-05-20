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
import org.apache.lucene.queryParser.QueryParser.Operator;
import org.apache.lucene.search.Query;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;

class LuceneExpression extends AbstractExpression {

    private static final long serialVersionUID = 8959252357123977939L;

    private final String val;

    private final boolean andOperator;
    
    LuceneExpression(FilterExprPath pathPrefix, String propertyName, String value, boolean andOperator) {
        super(pathPrefix, propertyName);
        this.val = value;
        this.andOperator = andOperator;
    }
    
    public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return true;
    }
    
    public Query addLuceneQuery(SpiExpressionRequest request) throws ParseException{

        String propertyName = getPropertyName();

        QueryParser p = request.createQueryParser(propertyName);
        p.setDefaultOperator(Operator.OR);
        return p.parse(val);    
    }
    
    public void addBindValues(SpiExpressionRequest request) {
    }

    public void addSql(SpiExpressionRequest request) {
    }
     
    public int queryPlanHash(BeanQueryRequest<?> request) {
        return queryAutoFetchHash();
    }
    
    /**
     * Based on caseInsensitive and the property name.
     */
    public int queryAutoFetchHash() {
        int hc = LuceneExpression.class.getName().hashCode();
        hc = hc * 31 + (andOperator ? 0 : 1);
        hc = hc * 31 + propName.hashCode();
        return hc;
    }
    
    public int queryBindHash() {
        return val.hashCode();
    }
}
