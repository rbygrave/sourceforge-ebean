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

import java.util.List;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;

/**
 * Lucene support for JunctionExpression.
 * 
 * @author rbygrave
 */
public class JunctionExpressionLucene {

    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request, List<SpiExpression> list, boolean disjunction) {
  
        BooleanClause.Occur occur = disjunction ? Occur.SHOULD : Occur.MUST;
        
        StringBuilder sb = new StringBuilder();
        
        BooleanQuery bq = new BooleanQuery();
        for (int i = 0; i < list.size(); i++) {
            SpiLuceneExpr luceneExpr = list.get(i).createLuceneExpr(request);
            Query query = luceneExpr.mergeLuceneQuery();
            bq.add(query, occur);
            
            if (i > 0){
                sb.append(" ").append(occur).append(" ");
            }
            sb.append(luceneExpr.getDescription());
        }
        
        return new LuceneExprResponse(bq, sb.toString());
    }
    
}
