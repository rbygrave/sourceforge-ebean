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
package com.avaje.ebeaninternal.util;

import java.util.ArrayList;

import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

import com.avaje.ebeaninternal.api.SpiLuceneExpr;

public class LuceneQueryList implements SpiLuceneExpr {

    private final SpiLuceneExpr.ExprOccur localOccur;

    private final ArrayList<SpiLuceneExpr> list = new ArrayList<SpiLuceneExpr>();

    private String description;
    
    public LuceneQueryList(SpiLuceneExpr.ExprOccur loccur) {
        this.localOccur = loccur;
    }

    public void add(SpiLuceneExpr q) {
        list.add(q);
    }

    public ArrayList<SpiLuceneExpr> getList() {
        return list;
    }

    
    public String getDescription() {
        return description;
    }

    public Object mergeLuceneQuery() {

        Occur luceneOccur = getLuceneOccur();

        StringBuilder sb = new StringBuilder();
        
        BooleanQuery bq = new BooleanQuery();
        for (int i = 0; i < list.size(); i++) {
            SpiLuceneExpr luceneExpr = list.get(i);
            Query lucQuery = (Query) luceneExpr.mergeLuceneQuery();
            bq.add(lucQuery, luceneOccur);
            
            if (i > 0){
                sb.append(" ").append(luceneOccur).append(" ");
            }
            sb.append(luceneExpr.getDescription());
        }
        
        description = sb.toString();
        return bq;
    }

    private Occur getLuceneOccur() {
        switch (localOccur) {
        case MUST:
            return BooleanClause.Occur.MUST;
        case MUST_NOT:
            return BooleanClause.Occur.MUST_NOT;
        case SHOULD:
            return BooleanClause.Occur.SHOULD;

        default:
            throw new RuntimeException("Invalid type " + localOccur);
        }
    }

}
