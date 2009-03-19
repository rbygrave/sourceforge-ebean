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

import javax.persistence.PersistenceException;

import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.server.plugin.ResultSetLimit;
import com.avaje.ebean.util.Message;

/**
 * Generates the SQL SELECT statements taking into account the physical
 * deployment properties.
 */
public class CQueryBuilder implements Constants {

	/**
	 * Set this to empty string for Oracle. Otherwise "as limitresult".
	 */
	private final String rowNumberWindowAlias;

	private final String tableAliasPlaceHolder;
	private final String columnAliasPrefix;
	private final boolean alwaysUseColumnAlias;
	
	/**
	 * Get one more than maxRows. This last one is not returned but if it exists
	 * is used to set a flag so the client knows there is more data to get if
	 * they want.
	 */
	private static final int maxOffset = 1;

	private final ResultSetLimit resultSetLimit;
	
	private final RawSqlSelectClauseBuilder rawSqlBuilder;
	
	private final Binder binder;
	
	/**
	 * Create the SqlGenSelect.
	 */
	public CQueryBuilder(PluginCore pluginCore) {
		this.binder = pluginCore.getDbConfig().getBinder();
		this.resultSetLimit = pluginCore.getDbConfig().getResultSetLimit();
		this.rowNumberWindowAlias = pluginCore.getDbConfig().getRowNumberWindowAlias();
		this.tableAliasPlaceHolder = pluginCore.getDbConfig().getTableAliasPlaceHolder();
		this.columnAliasPrefix = pluginCore.getDbConfig().getProperties().getProperty("columnAliasPrefix", "as c");
		this.alwaysUseColumnAlias = pluginCore.getDbConfig().getProperties().getPropertyBoolean("alwaysUseColumnAlias", false);
		this.rawSqlBuilder = new RawSqlSelectClauseBuilder(pluginCore);
	}

	/**
	 * Create the ROW_NUMBER() column used for firstRow maxRows limits. Returns
	 * null if the limitMode is not set to LIMIT_MODE_ROWNUMBER.
	 * <p>
	 * Only do this is there are detail rows joined. With detail rows joined
	 * this way of limiting the result set will not work.
	 * </p>
	 */
	protected String getRowNumberColumn(OrmQuery<?> find, String orderBy) {
		if (!resultSetLimit.equals(ResultSetLimit.RowNumber)) {
			return null;
		}

		if (find.getFirstRow() > 0 || find.getMaxRows() > 0) {
			if (orderBy == null || orderBy.trim().length() == 0) {
				throw new PersistenceException(Message.msg("fetch.limit.orderby"));
			}
			return ROW_NUMBER_OVER + orderBy + ROW_NUMBER_AS;
		}
		return null;
	}

	protected String getOrderBy(String orderBy, BeanPropertyAssocMany many, BeanDescriptor desc,
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
	public CQuery buildQuery(QueryRequest request) {

		if (request.isSqlSelect()){
			return rawSqlBuilder.build(request);
		}
		
		CQueryPredicates predicates = new CQueryPredicates(binder, request, null);
		
		
		CQueryPlan queryPlan = request.getQueryPlan();
		
		if (queryPlan != null){
			// Reuse the query plan so skip generating SqlTree and SQL.
			// We do prepare and bind the new parameters
			predicates.prepare(false);
			return new CQuery(request, predicates, queryPlan);
		}

		// Prepare the where, having and order by clauses. 
		// This also parses them from logical property names to
		// database columns and determines 'includes'. 
		
		// We need to check these 'includes' for extra joins 
		// that are not included via select
		predicates.prepare(true);

		// Build the tree structure that represents the query.
		SqlTree sqlTree = createSqlTree(request, predicates);
		
		String sql = buildSql(request, predicates, sqlTree);

		queryPlan = new CQueryPlan(request.getQueryPlanHash(), sql, sqlTree, false, predicates.isRowNumberIncluded(), predicates.getLogWhereSql());
		
		// cache the query plan because we can reuse it and also 
		// gather query performance statistics based on it.
		request.putQueryPlan(queryPlan);
		
		return new CQuery(request, predicates, queryPlan);
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
    private SqlTree createSqlTree(QueryRequest request, CQueryPredicates predicates) {

        return new SqlTreeBuilder(tableAliasPlaceHolder, columnAliasPrefix, alwaysUseColumnAlias, request, predicates).build();
    }
	
	private String buildSql(QueryRequest request, CQueryPredicates predicates, SqlTree select) {
				
		OrmQuery<?> query = request.getQuery();
		BeanPropertyAssocMany manyProp = select.getManyProperty();
		
		String dbOrderBy = predicates.getDbOrderBy();


		StringBuilder sb = new StringBuilder(500);

		sb.append("select ");
		if (query.isDistinct()) {
			sb.append("distinct ");
		}

		if (manyProp == null) {
			// setup ROW_NUMBER() column if required
			String rowNumberCol = getRowNumberColumn(query, dbOrderBy);
			if (rowNumberCol != null) {
				// column used to limit rows returned based on
				//firstRow and maxRows
				predicates.setRowNumberIncluded(true);
				sb.append(rowNumberCol);
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
			
			BeanDescriptor desc = request.getBeanDescriptor();
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

		String genSql = sb.toString();
		if (manyProp == null) {
			// finish wrapping ROW_NUMBER() or LIMIT clause
			genSql = wrapSql(query, genSql);
		}

		return genSql;
	}

	/**
	 * Wrap the sql to implement firstRow maxRows limits. Use the standard
	 * ROW_NUMBER() function or MySQL/Postgres LIMIT OFFSET clause.
	 */
	protected String wrapSql(OrmQuery<?> f, String sql) {
		if (!f.hasMaxRowsOrFirstRow()){
			return sql;
		}
		switch (resultSetLimit) {
		case RowNumber:
			return wrapRowNumberLimit(f, sql);
		case LimitOffset:
			return wrapLimitOffset(f, sql);
		case JdbcRowNavigation:
			return sql;

		default:
			return sql;
		}
	}

	/**
	 * Wrap the select statement to use firstRows maxRows limit features.
	 */
	protected String wrapRowNumberLimit(OrmQuery<?> f, String sql) {

		int firstRow = f.getFirstRow();

		int lastRow = f.getMaxRows();
		if (lastRow > 0) {
			lastRow = lastRow + firstRow + maxOffset;
		}

		StringBuilder sb = new StringBuilder(512);
		sb.append("select * from (").append(NEW_LINE);
		sb.append(sql);
		sb.append(NEW_LINE).append(") ");
		sb.append(rowNumberWindowAlias);
		sb.append(" where ");
		if (firstRow > 0) {
			sb.append(" rn > ").append(firstRow);
			if (lastRow > 0) {
				sb.append(" and ");
			}
		}
		if (lastRow > 0) {
			sb.append(" rn <= ").append(lastRow);
		}

		return sb.toString();
	}

	/**
	 * Wrap the sql with LIMIT OFFSET keywords. Used by MySql and Postgres.
	 */
	protected String wrapLimitOffset(OrmQuery<?> f, String sql) {

		int firstRow = f.getFirstRow();
		int lastRow = f.getMaxRows();
		if (lastRow > 0) {
			lastRow = lastRow + firstRow + maxOffset;
		}

		StringBuilder sb = new StringBuilder(512);
		sb.append(sql);
		sb.append(" ").append(NEW_LINE).append(LIMIT).append(" ");
		if (lastRow > 0) {
			sb.append(lastRow);
			if (firstRow > 0) {
				sb.append(" ").append(OFFSET).append(" ");
			}
		}
		if (firstRow > 0) {
			sb.append(firstRow);
		}

		return sb.toString();
	}

}
