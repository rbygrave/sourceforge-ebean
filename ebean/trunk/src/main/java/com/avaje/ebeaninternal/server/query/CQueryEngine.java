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
package com.avaje.ebeaninternal.server.query;

import java.sql.SQLException;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionTouched;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebeaninternal.api.BeanIdList;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.Message;
import com.avaje.ebeaninternal.server.core.OrmQueryRequest;
import com.avaje.ebeaninternal.server.jmx.MAdminLogging;
import com.avaje.ebeaninternal.server.persist.Binder;

/**
 * Handles the Object Relational fetching.
 */
public class CQueryEngine {

	private static final Logger logger = Logger.getLogger(CQueryEngine.class.getName());
	
	private final CQueryBuilder queryBuilder;

	private final MAdminLogging logControl;

	private final BackgroundExecutor backgroundExecutor;

	private final int defaultSecondaryQueryBatchSize = 100;

	public CQueryEngine(DatabasePlatform dbPlatform, MAdminLogging logControl, 
			Binder binder, BackgroundExecutor backgroundExecutor) {

		this.logControl = logControl;
		this.backgroundExecutor = backgroundExecutor;
		this.queryBuilder = new CQueryBuilder(backgroundExecutor, dbPlatform, binder);
	}

	public <T> CQuery<T> buildQuery(OrmQueryRequest<T> request) {
		return queryBuilder.buildQuery(request);
	}

	/**
	 * Build and execute the row count query.
	 */
	public <T> BeanIdList findIds(OrmQueryRequest<T> request) {


		CQueryFetchIds rcQuery = queryBuilder.buildFetchIdsQuery(request);
		try {

			String sql = rcQuery.getGeneratedSql();
			sql = sql.replace(Constants.NEW_LINE, ' ');

			if (logControl.isDebugGeneratedSql()) {
				System.out.println(sql);
			}
			if (logControl.isLogQuery(MAdminLogging.SQL)) {
				request.getTransaction().log(sql);
			}

			BeanIdList list = rcQuery.findIds();

			if (logControl.isLogQuery(MAdminLogging.SUMMARY)) {
				request.getTransaction().log(rcQuery.getSummary());
			}

			if (!list.isFetchingInBackground() && request.getQuery().isFutureFetch()){
				// end the transaction for futureFindIds (it had it's own one)
				logger.fine("Future findIds completed!");
				request.getTransaction().end();
			}
			
			return list;

		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	/**
	 * Build and execute the row count query.
	 */
	public <T> int findRowCount(OrmQueryRequest<T> request) {


		CQueryRowCount rcQuery = queryBuilder.buildRowCountQuery(request);
		try {

			String sql = rcQuery.getGeneratedSql();
			sql = sql.replace(Constants.NEW_LINE, ' ');

			if (logControl.isDebugGeneratedSql()) {
				System.out.println(sql);
			}
			if (logControl.isLogQuery(MAdminLogging.SQL)) {
				request.getTransaction().log(sql);
			}

			int rowCount = rcQuery.findRowCount();

			if (logControl.isLogQuery(MAdminLogging.SUMMARY)) {
				request.getTransaction().log(rcQuery.getSummary());
			}
			
			if (request.getQuery().isFutureFetch()){
				logger.fine("Future findRowCount completed!");
				request.getTransaction().end();
			}
			return rowCount;

		} catch (SQLException e) {
			throw new PersistenceException(e);
		} 
	}

	/**
	 * Find a list/map/set of beans.
	 */
	public <T> BeanCollection<T> findMany(OrmQueryRequest<T> request) {

		// flag indicating whether we need to close the resources...
		boolean useBackgroundToContinueFetch = false;

		CQuery<T> cquery = queryBuilder.buildQuery(request);
		request.setCancelableQuery(cquery);
		
		try {

			if (logControl.isDebugGeneratedSql()) {
				logSqlToConsole(cquery);
			}
			if (logControl.isLogQuery(MAdminLogging.SQL)) {
				logSql(cquery);
			}

			if (!cquery.prepareBindExecuteQuery()) {
				// query has been cancelled already
				logger.finest("Future fetch already cancelled");
				return null;
			}

			BeanCollection<T> beanCollection = cquery.readCollection();
			if (request.getParentState() != EntityBeanIntercept.DEFAULT){
				// effectively making the collection immutable
				beanCollection.setSharedInstance();
			}
			
			BeanCollectionTouched collectionTouched = request.getQuery().getBeanCollectionTouched();
			if (collectionTouched != null){
				// register a listener that wants to be notified when the
				// bean collection is first used
				beanCollection.setBeanCollectionTouched(collectionTouched);
			}

			if (cquery.useBackgroundToContinueFetch()) {
				// stop the request from putting connection back into pool
				// before background fetching is finished.
				request.setBackgroundFetching();
				useBackgroundToContinueFetch = true;
				BackgroundFetch fetch = new BackgroundFetch(cquery);

				FutureTask<Integer> future = new FutureTask<Integer>(fetch);
				beanCollection.setBackgroundFetch(future);
				backgroundExecutor.execute(future);
			}

			if (logControl.isLogQuery(MAdminLogging.SUMMARY)) {
				logFindManySummary(cquery);
			}
			
			request.executeSecondaryQueries(defaultSecondaryQueryBatchSize);
	
			return beanCollection;

		} catch (SQLException e) {
			String sql = cquery.getGeneratedSql();
			String m = Message.msg("fetch.error", e.getMessage(), sql);
			throw new PersistenceException(m, e);

		} finally {
			if (useBackgroundToContinueFetch) {
				// left closing resources to BackgroundFetch...
			} else {
				if (cquery != null) {
					cquery.close();
				}
				if (request.getQuery().isFutureFetch()){
					// end the transaction for futureFindIds 
					// as it had it's own transaction
					logger.fine("Future fetch completed!");
					request.getTransaction().end();
				}
			}
		}
	}
	
	/**
	 * Find and return a single bean using its unique id.
	 */
	public <T> T find(OrmQueryRequest<T> request) {

		T bean = null;

		CQuery<T> cquery = queryBuilder.buildQuery(request);

		try {
			if (logControl.isDebugGeneratedSql()) {
				logSqlToConsole(cquery);
			}
			if (logControl.isLogQuery(MAdminLogging.SQL)) {
				logSql(cquery);
			}

			cquery.prepareBindExecuteQuery();

			if (cquery.readBean()) {
				bean = cquery.getLoadedBean();
			}

			if (logControl.isLogQuery(MAdminLogging.SUMMARY)) {
				logFindSummary(cquery);
			}

			request.executeSecondaryQueries(defaultSecondaryQueryBatchSize);
			
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
	private void logSqlToConsole(CQuery<?> cquery) {

		SpiQuery<?> query = cquery.getQueryRequest().getQuery();
		String loadMode = query.getLoadMode();
		String loadDesc = query.getLoadDescription();
		
		String sql = cquery.getGeneratedSql();
		String summary = cquery.getSummary();

		StringBuilder sb = new StringBuilder(1000);
		sb.append("<sql ");
		if (loadMode != null) {
			sb.append("mode='").append(loadMode).append("' ");
		}
		sb.append("summary='").append(summary);
		if (loadDesc != null){
			sb.append("' load='").append(loadDesc);			
		}
		sb.append("' >");
		sb.append(Constants.NEW_LINE);
		sb.append(sql);
		sb.append(Constants.NEW_LINE).append("</sql>");

		System.out.println(sb.toString());
	}

	/**
	 * Log the generated SQL to the transaction log.
	 */
	private void logSql(CQuery<?> query) {

		String sql = query.getGeneratedSql();
		sql = sql.replace(Constants.NEW_LINE, ' ');
		query.getTransaction().log(sql);
	}

	/**
	 * Log the FindById summary to the transaction log.
	 */
	private void logFindSummary(CQuery<?> q) {

		StringBuilder msg = new StringBuilder(200);
		msg.append("FindById exeMicros[").append("" + q.getQueryExecutionTimeMicros());
		msg.append("] rows[").append(q.getLoadedRowDetail());
		msg.append("] type[").append(q.getBeanName());
		msg.append("] bind[").append(q.getBindLog()).append("]");

		q.getTransaction().log(msg.toString());
	}

	/**
	 * Log the FindMany to the transaction log.
	 */
	private void logFindManySummary(CQuery<?> q) {

		StringBuilder msg = new StringBuilder(200);
		msg.append("FindMany exeMicros[").append(q.getQueryExecutionTimeMicros());
		msg.append("] rows[").append(q.getLoadedRowDetail());
		msg.append("] type[").append(q.getBeanName());
		msg.append("] name[").append(q.getName());
		msg.append("] predicates[").append(q.getLogWhereSql());
		msg.append("] bind[").append(q.getBindLog()).append("]");

		q.getTransaction().log(msg.toString());
	}
}
