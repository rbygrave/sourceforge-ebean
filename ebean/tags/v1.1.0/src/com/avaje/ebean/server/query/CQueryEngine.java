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

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.server.core.QueryRequest;
import com.avaje.ebean.server.jmx.MLogControlMBean;
import com.avaje.ebean.server.lib.thread.ThreadPool;
import com.avaje.ebean.server.lib.thread.ThreadPoolManager;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.util.Message;

/**
 * Handles the Object Relational fetching.
 */
public class CQueryEngine {

	/**
	 * Use JDBC row navigation to limit results.
	 */
	private final boolean useResultSetLimit;

	/**
	 * Thread pool used for background fetching.
	 */
	private final ThreadPool threadPool;

	private final CQueryBuilder queryBuilder;

	private final MLogControlMBean logControl;

	public CQueryEngine(PluginCore pluginCore) {
		this.logControl = pluginCore.getDbConfig().getLogControl();

		this.queryBuilder = new CQueryBuilder(pluginCore);
		this.threadPool = ThreadPoolManager.getThreadPool("BGFetch");

		this.useResultSetLimit = pluginCore.getDbConfig().getDbSpecific().useJdbcResultSetLimit();
	}

	public CQuery buildQuery(QueryRequest request) {
		return queryBuilder.buildQuery(request);
	}
	
	
	/**
	 * Find a list/map/set of beans.
	 */
	public BeanCollection<?> findMany(QueryRequest request) {

		// flag indicating whether we need to close the resources...
		boolean useBackgroundToContinueFetch = false;

		CQuery cquery = queryBuilder.buildQuery(request);
		try {

			if (logControl.isDebugGeneratedSql()){
				logSqlToConsole(cquery);
			}
			if (logControl.getQueryManyLevel() >= LogControl.LOG_SQL) {
				logSql(cquery);
			}

			cquery.prepareBindExecuteQuery();

			BeanCollection<?> beanCollection = cquery.readCollection(useResultSetLimit);

			if (cquery.useBackgroundToContinueFetch()) {
				// stop the request from putting connection back into pool
				// before background fetching is finished.
				request.setBackgroundFetching();
				useBackgroundToContinueFetch = true;
				BackgroundFetch fetch = new BackgroundFetch(cquery);

				threadPool.assign(fetch, true);
			}

			if (logControl.getQueryManyLevel() >= LogControl.LOG_SUMMARY) {
				logFindManySummary(cquery);
			}
			
			return beanCollection;

		} catch (SQLException e) {
			String sql = cquery.getGeneratedSql();
			String m = Message.msg("fetch.error", e.getMessage(), sql);
			throw new PersistenceException(m, e);

		} finally {
			if (!useBackgroundToContinueFetch) {
				if (cquery != null) {
					cquery.close();
				}
			} else {
				// left closing resources up to Background process...
			}
		}
	}

	/**
	 * Find and return a single bean using its unique id.
	 */
	public Object find(QueryRequest request) {

		EntityBean bean = null;

		CQuery cquery = queryBuilder.buildQuery(request);		
		
		try {
			if (logControl.isDebugGeneratedSql()) {
				logSqlToConsole(cquery);
			}
			if (logControl.getQueryByIdLevel() >= LogControl.LOG_SQL){
				logSql(cquery);
			}
			
			cquery.prepareBindExecuteQuery();

			if (cquery.readBean()) {
				bean = cquery.getLoadedBean();
			}

			if (logControl.getQueryByIdLevel() >= LogControl.LOG_SUMMARY) {
				logFindSummary(cquery);
			}
			
			return bean;

		} catch (SQLException e) {
			String sql = cquery.getGeneratedSql();
			String msg = Message.msg("fetch.error", e.getMessage(), sql);
			throw new PersistenceException(msg, e);

		} finally {
			cquery.close();
		}
	}

	/**
	 * Log the generated SQL to the console.
	 */
	private void logSqlToConsole(CQuery build) {

		String sql = build.getGeneratedSql();
		String summary = build.getSummary();

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<sql summary='").append(summary).append("'>");
		sb.append(Constants.NEW_LINE);
		sb.append(sql);
		sb.append(Constants.NEW_LINE).append("</sql>");

		System.out.println(sb.toString());
	}
	
	/**
	 * Log the generated SQL to the transaction log.
	 */
	private void logSql(CQuery query) {

		String sql = query.getGeneratedSql();
		sql = sql.replace(Constants.NEW_LINE, ' ');
		query.getTransaction().log(sql);
	}

	/**
	 * Log the FindById summary to the transaction log.
	 */
	private void logFindSummary(CQuery q) {
			
		StringBuilder msg = new StringBuilder(200);
		msg.append("FindById");
		msg.append(" exeMicros[").append("" + q.getQueryExecutionTimeMicros()).append("]");
		msg.append(" rows[").append(q.getLoadedRowDetail());
		msg.append("]");
		
		String beanType = q.getBeanType();
		msg.append(" type[").append(beanType).append("]");
		msg.append(" bind[").append(q.getBindLog()).append("]");

		q.getTransaction().log(msg.toString());
	}

	/**
	 * Log the FindMany to the transaction log.
	 */
	private void logFindManySummary(CQuery q) {

		StringBuilder msg = new StringBuilder(200);
		msg.append("FindMany");
		msg.append(" exeMicros[").append(q.getQueryExecutionTimeMicros()).append("]");
		msg.append(" rows[").append(q.getLoadedRowDetail());
		msg.append("] type[");
		String beanType = q.getBeanType();
		msg.append(beanType).append("]");
		msg.append(" name[").append(q.getName()).append("]");
		
		msg.append(" predicates[").append(q.getLogWhereSql()).append("]");
		msg.append(" bind[").append(q.getBindLog()).append("]");

		q.getTransaction().log(msg.toString());
	}
}
