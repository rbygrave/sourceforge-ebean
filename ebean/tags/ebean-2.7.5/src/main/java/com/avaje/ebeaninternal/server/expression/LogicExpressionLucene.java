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

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;

public class LogicExpressionLucene {

    public SpiLuceneExpr addLuceneQuery(String joinType, SpiExpressionRequest request, SpiExpression expOne, SpiExpression expTwo) {
                
        SpiLuceneExpr e1 = expOne.createLuceneExpr(request);
        SpiLuceneExpr e2 = expTwo.createLuceneExpr(request);
        
        Query q1 = e1.mergeLuceneQuery();
        Query q2 = e2.mergeLuceneQuery();
        
        BooleanQuery q = new BooleanQuery();
        Occur occur = LogicExpression.OR.equals(joinType) ? Occur.SHOULD : Occur.MUST;
        
        q.add(q1, occur);
        q.add(q2, occur);
        
        String desc = e1.getDescription()+joinType+e2.getDescription();
        return new LuceneExprResponse(q, desc);
    }
}
