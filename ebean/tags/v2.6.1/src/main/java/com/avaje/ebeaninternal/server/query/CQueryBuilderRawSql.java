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

import com.avaje.ebean.RawSql;
import com.avaje.ebean.config.dbplatform.SqlLimitResponse;
import com.avaje.ebean.config.dbplatform.SqlLimiter;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.querydefn.OrmQueryLimitRequest;

public class CQueryBuilderRawSql implements Constants {

    private final SqlLimiter sqlLimiter;

    CQueryBuilderRawSql(SqlLimiter sqlLimiter) {
        this.sqlLimiter = sqlLimiter;
    }
    
    /**
     * Build the full SQL Select statement for the request.
     */
    public SqlLimitResponse buildSql(OrmQueryRequest<?> request, CQueryPredicates predicates, RawSql.Sql rsql) {

        if (!rsql.isParsed()){
            return new SqlLimitResponse(rsql.getUnparsedSql(), false);
        }

        String orderBy = getOrderBy(predicates, rsql);
    
        // build the actual sql String
        String sql = buildMainQuery(orderBy, request, predicates, rsql);
        
        SpiQuery<?> query = request.getQuery();
        if (query.hasMaxRowsOrFirstRow() && sqlLimiter != null) {
            // wrap with a limit offset or ROW_NUMBER() etc
            return sqlLimiter.limit(new OrmQueryLimitRequest(sql, orderBy, query));
            
        } else {
            // add back select keyword (it was removed to support sqlQueryLimiter)
            sql = "select " + sql;
            return new SqlLimitResponse(sql, false);
        }
    }
    
    private String buildMainQuery(String orderBy, OrmQueryRequest<?> request, CQueryPredicates predicates, RawSql.Sql sql) {
        
        StringBuilder sb = new StringBuilder();
        sb.append(sql.getPreFrom());
        sb.append(" ");
        sb.append(NEW_LINE);
        sb.append(sql.getPreWhere());
        sb.append(" ");

        String dynamicWhere = null;
        if (request.getQuery().getId() != null) {
            // need to convert this as well. This avoids the
            // assumption that id has its proper dbColumn assigned
            // which may change if using multiple raw sql statements
            // against the same bean.
            BeanDescriptor<?> descriptor = request.getBeanDescriptor();
            //FIXME: I think this is broken... needs to be logical 
            // and then parsed for RawSqlSelect...
            dynamicWhere = descriptor.getIdBinderIdSql();
        }

        String dbWhere = predicates.getDbWhere();
        if (dbWhere != null && dbWhere.length() > 0) {
            if (dynamicWhere == null) {
                dynamicWhere = dbWhere;
            } else {
                dynamicWhere += " and " + dbWhere;
            }
        }

        if (dynamicWhere != null) {
            sb.append(NEW_LINE);            
            if (sql.isAndWhereExpr()) {
                sb.append("and ");
            } else {
                sb.append("where ");
            }
            sb.append(dynamicWhere);
            sb.append(" ");
        }

        String preHaving = sql.getPreHaving();
        if (preHaving != null) {
            sb.append(NEW_LINE);
            sb.append(preHaving);
            sb.append(" ");
        }

        String dbHaving = predicates.getDbHaving();
        
        if (dbHaving != null && dbHaving.length() > 0) {
            sb.append(" ");
            sb.append(NEW_LINE);
            if (sql.isAndHavingExpr()) {
                sb.append("and ");
            } else {
                sb.append("having ");
            }
            sb.append(dbHaving);
            sb.append(" ");
        }

        //String orderBy = getOrderBy(predicates, sql);
        if (orderBy != null) {
            sb.append(NEW_LINE);
            sb.append(" order by ").append(orderBy);
        }

        return sb.toString();
    }

    private String getOrderBy(CQueryPredicates predicates, RawSql.Sql sql) {
        String orderBy = predicates.getDbOrderBy();
        if (orderBy != null) {
            return orderBy;
        } else {
            return sql.getOrderBy();          
        }
    }
}
