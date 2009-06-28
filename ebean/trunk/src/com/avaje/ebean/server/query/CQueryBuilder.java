/**
 * Copyright (C) 2006  Robin Bygrave
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
package com.avaje.ebean.server.query;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.dbplatform.SqlLimitRequest;
import com.avaje.ebean.config.dbplatform.SqlLimitResponse;
import com.avaje.ebean.config.dbplatform.SqlLimiter;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.OrmQueryLimitRequest;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.persist.Binder;

/**
 * Generates the SQL SELECT statements taking into account the physical
 * deployment properties.
 */
public class CQueryBuilder implements Constants {


	private final String tableAliasPlaceHolder;
	private final String columnAliasPrefix;
	
	private final SqlLimiter dbQueryLimiter;
	
	private final RawSqlSelectClauseBuilder rawSqlBuilder;
	
	private final Binder binder;
	
	/**
	 * Create the SqlGenSelect.
	 */
	public CQueryBuilder(DatabasePlatform dbPlatform, Binder binder) {
		this.binder = binder;
		this.tableAliasPlaceHolder = GlobalProperties.get("ebean.tableAliasPlaceHolder","${ta}");
		this.columnAliasPrefix = GlobalProperties.get("ebean.columnAliasPrefix", "c");
		this.rawSqlBuilder = new RawSqlSelectClauseBuilder(dbPlatform, binder);

		this.dbQueryLimiter = dbPlatform.getSqlLimiter();
	}

	protected String getOrderBy(String orderBy, BeanPropertyAssocMany<?> many, BeanDescriptor<?> desc,
			boolean hasListener) {

		String manyOrderBy = null;

		if (many != null) {
			manyOrderBy = many.getFetchOrderBy();
			if (manyOrderBy != null) {
				// FIXME: Bug: assuming only one column in manyOrderBy
				// Need to prefix many.getName() to all the column names
				// in the order by but not the ASC DESC keywords
				manyOrderBy = many.getName() + "." + manyOrderBy;
			}
		}
		if (orderBy == null && (hasListener || manyOrderBy != null)) {
			// build orderBy to be the list of primary key columns
			StringBuffer sb = new StringBuffer();
			
			BeanProperty[] uids = desc.propertiesId();
			for (int i = 0; i < uids.length; i++) {				
				if (i > 0) {
					sb.append(", ");
				}
				sb.append(uids[i].getName());
			}
			orderBy = sb.toString();
		}
		if (manyOrderBy != null) {
			// add first orderBy to manyOrderby
			orderBy = orderBy + " , " + manyOrderBy;
		}
		return orderBy;
	}
	
	/**
	 * Return the SQL Select statement as a String. Converts logical property
	 * names to physical deployment column names.
	 */
	public <T> CQuery<T> buildQuery(OrmQueryRequest<T> request) {

		if (request.isSqlSelect()){
			return rawSqlBuilder.build(request);
		}
		
		CQueryPredicates predicates = new CQueryPredicates(binder, request, null);
		
		
		CQueryPlan queryPlan = request.getQueryPlan();
		
		if (queryPlan != null){
			// Reuse the query plan so skip generating SqlTree and SQL.
			// We do prepare and bind the new parameters
			predicates.prepare(false, true);
			return new CQuery<T>(request, predicates, queryPlan);
		}

		// Prepare the where, having and order by clauses. 
		// This also parses them from logical property names to
		// database columns and determines 'includes'. 
		
		// We need to check these 'includes' for extra joins 
		// that are not included via select
		predicates.prepare(true, true);

		// Build the tree structure that represents the query.
		SqlTree sqlTree = createSqlTree(request, predicates);
		
		SqlLimitResponse s = buildSql(request, predicates, sqlTree);

		queryPlan = new CQueryPlan(request, s.getSql(), sqlTree, false, s.isIncludesRowNumberColumn(), predicates.getLogWhereSql());
		
		// cache the query plan because we can reuse it and also 
		// gather query performance statistics based on it.
		request.putQueryPlan(queryPlan);
		
		return new CQuery<T>(request, predicates, queryPlan);
	}
	
    /**
     * Build the SqlTree.
     * <p>
     * The SqlTree is immutable after construction and
     * so is safe to use by concurrent threads.
     * </p>
     * <p>
     * The predicates is used to add additional joins that come from
     * the where or order by clauses that are not already included for
     * the select clause.
     * </p>
     */
    private SqlTree createSqlTree(OrmQueryRequest<?> request, CQueryPredicates predicates) {

        return new SqlTreeBuilder(tableAliasPlaceHolder, columnAliasPrefix, request, predicates).build();
    }
	
	private SqlLimitResponse buildSql(OrmQueryRequest<?> request, CQueryPredicates predicates, SqlTree select) {
				
		OrmQuery<?> query = request.getQuery();
		BeanPropertyAssocMany<?> manyProp = select.getManyProperty();
		
		String dbOrderBy = predicates.getDbOrderBy();


		boolean useSqlLimiter = (query.getMaxRows() > 0 || query.getFirstRow() > 0 && manyProp != null);

		StringBuilder sb = new StringBuilder(500);

		if (!useSqlLimiter){
			sb.append("select ");
			if (query.isDistinct()) {
				sb.append("distinct ");
			}
		}

		sb.append(select.getSelectSql());

		sb.append(" ").append(NEW_LINE);
		sb.append("from ");

		// build the from clause potentially with joins
		// required only for the predicates
		sb.append(select.getFromSql());

		String inheritanceWhere = select.getInheritanceWhereSql();
		
		boolean hasWhere = false;
		if (inheritanceWhere.length() > 0) {
			sb.append(" ").append(NEW_LINE).append("where");
			sb.append(inheritanceWhere);
			hasWhere = true;
		}
		
		if (request.isFindById() || query.getId() != null){
			if (hasWhere){
				sb.append(" and ");
			} else {
				sb.append(NEW_LINE).append("where ");
			}
			
			BeanDescriptor<?> desc = request.getBeanDescriptor();
			String idSql = desc.getBindIdSql();
			sb.append(idSql).append(" ");
			hasWhere = true;
		}

		String dbWhere = predicates.getDbWhere();
		if (dbWhere != null) {
			if (dbWhere.length() > 0) {
				if (!hasWhere) {
					sb.append(" ").append(NEW_LINE).append("where ");
				} else {
					sb.append("and ");
				}
				sb.append(dbWhere);
			}
		}

		if (dbOrderBy != null) {
			sb.append(" ").append(NEW_LINE);
			sb.append("order by ").append(dbOrderBy);
		}

//		String genSql = sb.toString();
//		if (manyProp == null) {
//			// finish wrapping ROW_NUMBER() or LIMIT clause
//			genSql = wrapSql(query, genSql);
//		}

		if (useSqlLimiter){
			// use LIMIT/OFFSET, ROW_NUMBER() or rownum type SQL query limitation
			SqlLimitRequest r = new OrmQueryLimitRequest(sb.toString(), dbOrderBy, query);
			return dbQueryLimiter.limit(r);
			
		} else {

			return new SqlLimitResponse(sb.toString(), false);
		}
		
	}

//	/**
//	 * Wrap the sql to implement firstRow maxRows limits. Use the standard
//	 * ROW_NUMBER() function or MySQL/Postgres LIMIT OFFSET clause.
//	 */
//	protected String wrapSql(OrmQuery<?> f, String sql) {
//		if (!f.hasMaxRowsOrFirstRow()){
//			return sql;
//		}
//		switch (resultSetLimit) {
//		case RowNumber:
//			return wrapRowNumberLimit(f, sql);
//		case LimitOffset:
//			return wrapLimitOffset(f, sql);
//		case JdbcRowNavigation:
//			return sql;
//
//		default:
//			return sql;
//		}
//	}
//
//	/**
//	 * Wrap the select statement to use firstRows maxRows limit features.
//	 */
//	protected String wrapRowNumberLimit(OrmQuery<?> f, String sql) {
//
//		int firstRow = f.getFirstRow();
//
//		int lastRow = f.getMaxRows();
//		if (lastRow > 0) {
//			lastRow = lastRow + firstRow + maxOffset;
//		}
//
//		StringBuilder sb = new StringBuilder(512);
//		sb.append("select * from (").append(NEW_LINE);
//		sb.append(sql);
//		sb.append(NEW_LINE).append(") ");
//		sb.append(rowNumberWindowAlias);
//		sb.append(" where ");
//		if (firstRow > 0) {
//			sb.append(" rn > ").append(firstRow);
//			if (lastRow > 0) {
//				sb.append(" and ");
//			}
//		}
//		if (lastRow > 0) {
//			sb.append(" rn <= ").append(lastRow);
//		}
//
//		return sb.toString();
//	}
//
//	/**
//	 * Wrap the sql with LIMIT OFFSET keywords. Used by MySql and Postgres.
//	 */
//	protected String wrapLimitOffset(OrmQuery<?> f, String sql) {
//
//		int firstRow = f.getFirstRow();
//		int lastRow = f.getMaxRows();
//		if (lastRow > 0) {
//			lastRow = lastRow + firstRow + maxOffset;
//		}
//
//		StringBuilder sb = new StringBuilder(512);
//		sb.append(sql);
//		sb.append(" ").append(NEW_LINE).append(LIMIT).append(" ");
//		if (lastRow > 0) {
//			sb.append(lastRow);
//			if (firstRow > 0) {
//				sb.append(" ").append(OFFSET).append(" ");
//			}
//		}
//		if (firstRow > 0) {
//			sb.append(firstRow);
//		}
//
//		return sb.toString();
//	}

}
