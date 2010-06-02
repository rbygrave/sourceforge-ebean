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
package com.avaje.ebeaninternal.server.query;

import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import com.avaje.ebean.OrderBy;
import com.avaje.ebeaninternal.api.BindParams;
import com.avaje.ebeaninternal.api.SpiExpressionList;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.LuceneOrmQueryRequest;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.expression.PersistenceLuceneParseException;
import com.avaje.ebeaninternal.server.lucene.LIndex;
import com.avaje.ebeaninternal.server.lucene.LLuceneSortResolve;
import com.avaje.ebeaninternal.util.DefaultExpressionRequest;

public class CQueryPredicatesLuceneResolve {

    private static final Logger logger = Logger.getLogger(CQueryPredicatesLuceneResolve.class.getName());

    private final OrmQueryRequest<?> request;

    private final SpiQuery<?> query;

    private final BindParams bindParams;

    // private final Object idValue;

    public CQueryPredicatesLuceneResolve(OrmQueryRequest<?> request) {

        this.request = request;
        this.query = request.getQuery();
        this.bindParams = query.getBindParams();
        // this.idValue = query.getId();
    }

    /**
     * Return true if the predicates in this query can be resolved via a lucene
     * index.
     */
    public boolean isLuceneResolvable() {

        // return false;

        LIndex luceneIndex = request.getLuceneIndex();
        if (luceneIndex == null) {
            // We don't have a Lucene Index on this bean type
            return false;
        }
        if (bindParams != null) {
            // no support for named or positioned parameters
            return false;
        }
        if (query.getHavingExpressions() != null) {
            // no support for having expressions
            return false;
        }

        LuceneResolvableRequest req = new LuceneResolvableRequest(request.getBeanDescriptor(), luceneIndex);

        LLuceneSortResolve lucenSortResolve = new LLuceneSortResolve(req, query.getOrderBy());

        if (!lucenSortResolve.isResolved()) {
            logger.info("Lucene Index can't support sort/orderBy of [" + lucenSortResolve.getUnsortableField() + "]");
            return false;
        }

        Sort luceneSort = lucenSortResolve.getSort();
        
        OrderBy<?> orderBy = query.getOrderBy();
        String sortDesc = orderBy == null ? "" : orderBy.toStringFormat();

        SpiExpressionList<?> whereExp = query.getWhereExpressions();
        if (whereExp == null) {
            // fetch all using index?
            MatchAllDocsQuery q = new MatchAllDocsQuery();
            request.setLuceneOrmQueryRequest(new LuceneOrmQueryRequest(q, luceneSort, "MatchAllDocs", sortDesc));
            return true;

        }
        if (!whereExp.isLuceneResolvable(req)) {
            // at least one expression was not resolvable via the lucene index
            return false;
        }

        try {
            // build the Lucene Query
            DefaultExpressionRequest whereReq = new DefaultExpressionRequest(request, luceneIndex);
            SpiLuceneExpr luceneExpr = whereExp.createLuceneExpr(whereReq, SpiLuceneExpr.ExprOccur.MUST);

            Query luceneQuery = luceneExpr.mergeLuceneQuery();
            String luceneDesc = luceneExpr.getDescription();
            request.setLuceneOrmQueryRequest(new LuceneOrmQueryRequest(luceneQuery, luceneSort, luceneDesc, sortDesc));
            return true;

        } catch (PersistenceLuceneParseException e) {
            // do we want to automatically fall back to SQL?
            String msg = "Failed to parse the Query using Lucene";
            throw new PersistenceException(msg, e);
        }
    }
}
