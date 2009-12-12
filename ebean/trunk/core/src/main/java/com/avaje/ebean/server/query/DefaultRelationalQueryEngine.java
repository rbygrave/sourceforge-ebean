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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.SqlQueryListener;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.internal.BindParams;
import com.avaje.ebean.internal.SpiSqlQuery;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.server.core.Message;
import com.avaje.ebean.server.core.RelationalQueryEngine;
import com.avaje.ebean.server.core.RelationalQueryRequest;
import com.avaje.ebean.server.jmx.MAdminLogging;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.type.DataBind;
import com.avaje.ebean.server.util.BindParamsParser;

/**
 * Perform native sql fetches.
 */
public class DefaultRelationalQueryEngine implements RelationalQueryEngine {

	private static final Logger logger = Logger.getLogger(DefaultRelationalQueryEngine.class.getName());

	private final int defaultMaxRows;

	private final Binder binder;

	private final MAdminLogging logControl;

	public DefaultRelationalQueryEngine(MAdminLogging logControl, Binder binder) {
		this.binder = binder;
		this.logControl = logControl;
		this.defaultMaxRows = GlobalProperties.getInt("nativesql.defaultmaxrows",100000);
	}

	public Object findMany(RelationalQueryRequest request) {

		SpiSqlQuery query = request.getQuery();

		long startTime = System.currentTimeMillis();

		SpiTransaction t = request.getTransaction();
		Connection conn = t.getInternalConnection();
		ResultSet rset = null;
		PreparedStatement pstmt = null;

		// flag indicating whether we need to close the resources...
		boolean useBackgroundToContinueFetch = false;

		String sql = query.getQuery();

		BindParams bindParams = query.getBindParams();

		if (!bindParams.isEmpty()) {
			// convert any named parameters if required
			sql = BindParamsParser.parse(bindParams, sql);
		}

		try {

			String bindLog = "";
			String[] propNames = null;
			
			synchronized (query) {
				if (query.isCancelled()){
					logger.finest("Query already cancelled");
					return null;
				}
				
				// synchronise for query.cancel() support		
				pstmt = conn.prepareStatement(sql);
	
				if (query.getTimeout() > 0){
					pstmt.setQueryTimeout(query.getTimeout());
				}
				if (query.getBufferFetchSizeHint() > 0){
					pstmt.setFetchSize(query.getBufferFetchSizeHint());
				}
				
				if (!bindParams.isEmpty()) {
					bindLog = binder.bind(bindParams, new DataBind(pstmt));
				}
	
				if (logControl.isLogSqlQuery(MAdminLogging.SQL)) {
					String sOut = sql.replace(Constants.NEW_LINE, ' ');
					sOut = sOut.replace(Constants.CARRIAGE_RETURN, ' ');
					t.log(sOut);
				}
	
				rset = pstmt.executeQuery();
	
				propNames = getPropertyNames(rset);
			}
			
			// calculate the initialCapacity of the Map to reduce
			// rehashing for queries with 12+ columns
			float initCap = (propNames.length) / 0.7f;
			int estimateCapacity = (int) initCap + 1;

			// determine the maxRows limit
			int maxRows = defaultMaxRows;
			if (query.getMaxRows() >= 1) {
				maxRows = query.getMaxRows();
			}

			boolean hasHitMaxRows = false;

			int loadRowCount = 0;

			SqlQueryListener listener = query.getListener();

			BeanCollectionWrapper wrapper = new BeanCollectionWrapper(request);
			boolean isMap = wrapper.isMap();
			String mapKey = query.getMapKey();
			
			SqlRow bean = null;
			
			while (rset.next()) {
				synchronized (query) {					
					// synchronise for query.cancel() support		
					if (!query.isCancelled()){
						bean = readRow(request, rset, propNames, estimateCapacity);
					}
				}
				if (bean != null){
					// bean can be null if query cancelled
					if (listener != null) {
						listener.process(bean);
	
					} else {
						if (isMap) {
							Object keyValue = bean.get(mapKey);
							wrapper.addToMap(bean, keyValue);
						} else {
							wrapper.addToCollection(bean);
						}
					}
	
					loadRowCount++;
	
					if (loadRowCount == maxRows) {
						// break, as we have hit the max rows to fetch...
						hasHitMaxRows = true;
						break;
					}
				}
			}

			BeanCollection<?> beanColl = wrapper.getBeanCollection();

			if (hasHitMaxRows) {
				if (rset.next()) {
					// there are more rows available after the maxRows limit
					beanColl.setHasMoreRows(true);
				}
			}

			if (!useBackgroundToContinueFetch) {
				beanColl.setFinishedFetch(true);
			}

			if (logControl.isLogSqlQuery(MAdminLogging.SUMMARY)) {

				long exeTime = System.currentTimeMillis() - startTime;

				String msg = "SqlQuery  rows[" + loadRowCount + "] time[" + exeTime + "] bind["
						+ bindLog + "] finished[" + beanColl.isFinishedFetch() + "]";

				t.log(msg);
			}
			
			if (query.isCancelled()){
				logger.fine("Query was cancelled during execution rows:"+loadRowCount);
			}
			
			return beanColl;

		} catch (Exception e) {
			String m = Message.msg("fetch.error", e.getMessage(), sql);
			throw new PersistenceException(m, e);

		} finally {
			if (!useBackgroundToContinueFetch) {
				try {
					if (rset != null) {
						rset.close();
					}
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
				try {
					if (pstmt != null) {
						pstmt.close();
					}
				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			} 
		}
	}

	/**
	 * Build the list of property names.
	 */
	protected String[] getPropertyNames(ResultSet rset) throws SQLException {

		ArrayList<String> propNames = new ArrayList<String>();

		ResultSetMetaData rsmd = rset.getMetaData();

		int columnsPlusOne = rsmd.getColumnCount()+1;

		
		for (int i = 1; i < columnsPlusOne; i++) {
			String columnName = rsmd.getColumnLabel(i);
			// will convert columnName to lower case
			propNames.add(columnName);
		}

		return (String[]) propNames.toArray(new String[propNames.size()]);
	}

	/**
	 * Read the row from the ResultSet and return as a MapBean.
	 */
	protected SqlRow readRow(RelationalQueryRequest request, ResultSet rset,
			String[] propNames, int initialCapacity) throws SQLException {

		// by default a map will rehash on the 12th entry
		// it will be pretty common to have 12 or more entries so
		// to reduce rehashing I am trying to estimate a good
		// initial capacity for the MapBean to use.
		SqlRow bean = new DefaultSqlRow(initialCapacity, 0.75f);
		
		int index = 0;

		for (int i = 0; i < propNames.length; i++) {
			index++;
			Object value = rset.getObject(index);
			bean.set(propNames[i], value);
		}

		return bean;

	}

}
