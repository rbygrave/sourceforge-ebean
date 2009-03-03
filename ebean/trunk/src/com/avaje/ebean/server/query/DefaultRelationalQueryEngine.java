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

import com.avaje.ebean.MapBean;
import com.avaje.ebean.NamingConvention;
import com.avaje.ebean.SqlQueryListener;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.query.RelationalQuery;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.RelationalQueryEngine;
import com.avaje.ebean.server.core.RelationalQueryRequest;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.jmx.MLogControlMBean;
import com.avaje.ebean.server.persist.Binder;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.util.BindParamsParser;
import com.avaje.ebean.util.BindParams;
import com.avaje.ebean.util.Message;
import com.avaje.lib.log.LogFactory;

/**
 * Perform native sql fetches.
 */
public class DefaultRelationalQueryEngine implements RelationalQueryEngine {

	private static final Logger logger = LogFactory.get(DefaultRelationalQueryEngine.class);

	private final int defaultMaxRows;

	private final NamingConvention namingConvention;

	private final Binder binder;

	private final MLogControlMBean logControl;

	public DefaultRelationalQueryEngine(Plugin plugin, InternalEbeanServer server) {
		PluginCore pluginCore = plugin.getPluginCore();
		PluginDbConfig dbConfig = pluginCore.getDbConfig();
		this.binder = dbConfig.getBinder();
		this.logControl = dbConfig.getLogControl();
		this.namingConvention = dbConfig.getNamingConvention();
		this.defaultMaxRows = dbConfig.getProperties().getPropertyInt("nativesql.defaultmaxrows",
			100000);
	}

	public Object findMany(RelationalQueryRequest request) {

		RelationalQuery query = request.getQuery();

		long startTime = System.currentTimeMillis();

		ServerTransaction t = request.getTransaction();
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

			pstmt = conn.prepareStatement(sql);

			if (query.getTimeout() > 0){
				pstmt.setQueryTimeout(query.getTimeout());
			}
			
			String bindLog = "";
			if (!bindParams.isEmpty()) {
				bindLog = binder.bind(bindParams, 0, pstmt);
			}

			if (logControl.getSqlQueryLevel() >= LogControl.LOG_SQL) {
				String sOut = sql.replace(Constants.NEW_LINE, ' ');
				sOut = sOut.replace(Constants.CARRIAGE_RETURN, ' ');
				t.log(sOut);
			}

			rset = pstmt.executeQuery();

			String[] propNames = getPropertyNames(rset);

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

			String baseTable = query.getBaseTable();

			while (rset.next()) {
				MapBean bean = readRow(request, rset, baseTable, propNames, estimateCapacity);
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

			if (logControl.getSqlQueryLevel() >= LogControl.LOG_SUMMARY) {

				long exeTime = System.currentTimeMillis() - startTime;

				String msg = "SqlQuery  rows[" + loadRowCount + "] time[" + exeTime + "] bind["
						+ bindLog + "] finished[" + beanColl.isFinishedFetch() + "]";

				t.log(msg);
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
			} else {
				// dp("left closing resources up to Background process...");
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
			// convert it just the same way as other MapBeans
			columnName = namingConvention.getMapBeanPropertyFromColumn(columnName);
			propNames.add(columnName);
		}

		return (String[]) propNames.toArray(new String[propNames.size()]);
	}

	/**
	 * Read the row from the ResultSet and return as a MapBean.
	 */
	protected MapBean readRow(RelationalQueryRequest request, ResultSet rset, String baseTable,
			String[] propNames, int initialCapacity) throws SQLException {

		// by default a map will rehash on the 12th entry
		// it will be pretty common to have 12 or more entries so
		// to reduce rehashing I am trying to estimate a good
		// initial capacity for the MapBean to use.
		MapBean bean = new MapBean(initialCapacity, 0.75f);
		bean.setBaseTable(baseTable);
		
		int index = 0;

		for (int i = 0; i < propNames.length; i++) {
			index++;
			Object value = rset.getObject(index);
			bean.set(propNames[i], value);
		}

		// from now on setter methods create oldValues
		bean._ebean_getIntercept().setLoaded();

		return bean;

	}

}
