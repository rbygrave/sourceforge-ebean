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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.DeployNamedQuery;
import com.avaje.ebean.server.deploy.DeployPropertyParser;
import com.avaje.ebean.server.deploy.DeploySqlSelect;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.server.plugin.ResultSetLimit;
import com.avaje.lib.log.LogFactory;

/**
 * Factory for SqlSelectClause based on raw sql.
 * <p>
 * Its job is to execute the sql, read the meta data to determine the columns to
 * bean property mapping.
 * </p>
 */
public class RawSqlSelectClauseBuilder {

	private static final Logger logger = LogFactory.get(RawSqlSelectClauseBuilder.class);

	private static final int MAX_OFFSET = 1;
	
	private final Binder binder;

	private final ResultSetLimit resultSetLimit;
	
	private final boolean useLimitOffset;
		
	public RawSqlSelectClauseBuilder(PluginCore pluginCore) {

		this.binder = pluginCore.getDbConfig().getBinder();
		this.resultSetLimit = getLimitMode(pluginCore.getDbConfig().getDbSpecific().getResultSetLimit());
		this.useLimitOffset = resultSetLimit.equals(ResultSetLimit.LimitOffset);
	}


	/**
	 * Build based on the includes and using the BeanJoinTree.
	 */
	public CQuery build(QueryRequest request) throws PersistenceException {

		OrmQuery<?> query = request.getQuery();
		BeanDescriptor desc = request.getBeanDescriptor();
		
		DeployNamedQuery namedQuery = desc.getNamedQuery(query.getName());
		DeploySqlSelect sqlSelect = namedQuery.getSqlSelect();

		
		// create a parser for this specific SqlSelect... has to be really
		// as each SqlSelect could have different table alias etc
		DeployPropertyParser parser = sqlSelect.createDeployPropertyParser();
		
		CQueryPredicates predicates = new CQueryPredicates(binder, request, parser);
		// prepare and convert logical property names to dbColumns etc 
		predicates.prepare(true);
		
		String sql = null;
		try {

			// build the actual sql String
			sql = sqlSelect.buildSql(predicates, request);
			if (useLimitOffset && query.hasMaxRowsOrFirstRow()) {
				// wrap with a limit offset clause
				// Not going to wrap with ROW_NUMBER() at this stage.
				sql = wrapLimitOffset(query, sql);
			}
			
			SqlTree sqlTree = sqlSelect.getSqlTree();
			
			CQueryPlan queryPlan = new CQueryPlan(0, sql, sqlTree, true, false, "");
			CQuery compiledQuery = new CQuery(request, predicates, queryPlan);

			return compiledQuery;

		} catch (Exception e) {
			
			String msg = "Error with "+desc.getFullName()+" query:\r" + sql;
			logger.log(Level.SEVERE, msg);
			throw new PersistenceException(e);
		}
	}

	/**
	 * Determine how row limits will be supported.
	 */
	private ResultSetLimit getLimitMode(ResultSetLimit limit) {
	
	    if (limit == null) {
	    	return ResultSetLimit.JdbcRowNavigation;
	
	    } else {
	    	return limit;
	    }
	}
	
	/**
	 * Wrap the sql with LIMIT OFFSET keywords. Used by MySql and Postgres.
	 */
	protected String wrapLimitOffset(OrmQuery<?> q, String sql) {

		int firstRow = q.getFirstRow();
		int lastRow = q.getMaxRows();
		if (lastRow > 0) {
			lastRow = lastRow + firstRow + MAX_OFFSET;
		}

		StringBuilder sb = new StringBuilder(512);
		sb.append(sql);
		sb.append(" ").append(Constants.NEW_LINE).append(Constants.LIMIT).append(" ");
		if (lastRow > 0) {
			sb.append(lastRow);
			if (firstRow > 0) {
				sb.append(" ").append(Constants.OFFSET).append(" ");
			}
		}
		if (firstRow > 0) {
			sb.append(firstRow);
		}

		return sb.toString();
	}
}
