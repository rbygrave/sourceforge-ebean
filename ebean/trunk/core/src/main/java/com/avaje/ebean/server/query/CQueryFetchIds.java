/**
 * Copyright (C) 2009  Robin Bygrave
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.core.ReferenceOptions;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbReadContext;

/**
 * Executes the select row count query.
 */
public class CQueryFetchIds {

	private static final Logger logger = Logger.getLogger(CQueryFetchIds.class.getName());

	/**
	 * The overall find request wrapper object.
	 */
	private final OrmQueryRequest<?> request;

	private final BeanDescriptor<?> desc;

	private final SpiQuery<?> query;

	/**
	 * Where clause predicates.
	 */
	private final CQueryPredicates predicates;

	/**
	 * The final sql that is generated.
	 */
	private final String sql;

	/**
	 * The resultSet that is read and converted to objects.
	 */
	private ResultSet rset;

	private int rsetIndex;
	
	/**
	 * The statement used to create the resultSet.
	 */
	private PreparedStatement pstmt;

	private String bindLog;

	private long startNano;
	
	private int executionTimeMicros;

	private int rowCount;
	
	/**
	 * Create the Sql select based on the request.
	 */
	public CQueryFetchIds(OrmQueryRequest<?> request, CQueryPredicates predicates, String sql) {
		this.request = request;
		this.query = request.getQuery();
		this.sql = sql;

		query.setGeneratedSql(sql);

		this.desc = request.getBeanDescriptor();
		this.predicates = predicates;

	}
	
	/**
	 * Return a summary description of this query.
	 */
	public String getSummary() {
		StringBuilder sb = new StringBuilder();
			sb.append("FindIds exeMicros[").append(executionTimeMicros)
			.append("] rows[").append(rowCount)
			.append("] type[").append(desc.getFullName())
			.append("] predicates[").append(predicates.getLogWhereSql())
			.append("] bind[").append(bindLog).append("]");
		
		return sb.toString();		
	}

	/**
	 * Return the generated sql.
	 */
	public String getGeneratedSql() {
		return sql;
	}
	
	public OrmQueryRequest<?> getQueryRequest() {
		return request;
	}

	/**
	 * Execute the query returning the row count.
	 */
	public List<Object> findIds() throws SQLException {

		startNano = System.nanoTime();
		try {
			// get the list that we are going to put the id's into.
			// This was already set so that it is available to be 
			// read by other threads (it is a synchronised list)
			List<Object> idList = query.getPartialIds();
			if (idList == null){
				// running in foreground thread (not FutureIds query)
				idList = Collections.synchronizedList(new ArrayList<Object>());
				query.setPartialIds(idList);
			}
			
			SpiTransaction t = request.getTransaction();
			Connection conn = t.getInternalConnection();
			pstmt = conn.prepareStatement(sql);
			
			if (query.getBufferFetchSizeHint() > 0){
				pstmt.setFetchSize(query.getBufferFetchSizeHint());
			}
	
			if (query.getTimeout() > 0){
				pstmt.setQueryTimeout(query.getTimeout());
			}
	
			bindLog = predicates.bind(pstmt);
	
			rset = pstmt.executeQuery();
	
			DbReadContext ctx = new DbContext();
			
			while (rset.next()){
				Object idValue = desc.getIdBinder().read(ctx);
				
				idList.add(idValue);
				
				// reset back to 0
				rsetIndex = 0;
			}
			
			long exeNano = System.nanoTime() - startNano;
			executionTimeMicros = (int)exeNano/1000;
		
			return idList;
			
		} finally {
			close();
		}
	}

	/**
	 * Close the resources.
	 * <p>
	 * The jdbc resultSet and statement need to be closed. Its important that
	 * this method is called.
	 * </p>
	 */
	private void close() {
		try {
			if (rset != null) {
				rset.close();
				rset = null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
		}
		try {
			if (pstmt != null) {
				pstmt.close();
				pstmt = null;
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
		}
	}

	private class DbContext implements DbReadContext {

		public ResultSet getRset() {
			return rset;
		}
		
		public int nextRsetIndex() {
			return ++rsetIndex;
		}
		
		public boolean isSharedInstance() {
			return false;
		}

		public boolean isReadOnly() {
			return false;
		}

		public ReferenceOptions getReferenceOptionsFor(BeanPropertyAssocOne<?> beanProp) {
			// always null
			return null;
		}

		public ObjectGraphNode createAutoFetchNode(String extraPath,
				String prefix) {
			// always null
			return null;
		}

		public BeanPropertyAssocMany<?> getManyProperty() {
			// always null
			return null;
		}

		public PersistenceContext getPersistenceContext() {
			// always null
			return null;
		}

		public boolean isAutoFetchProfiling() {
			return false;
		}

		public void profileBean(EntityBeanIntercept ebi, String extraPath, String prefix) {
			// no-op			
		}

		public void profileReference(EntityBeanIntercept ebi, String extraPath) {
			// no-op
		}

		public void setCurrentPrefix(String currentPrefix) {
			// no-op
		}

		public void setLoadedBean(Object loadedBean, Object id) {
			// no-op
		}

		public void setLoadedManyBean(Object loadedBean) {
			// no-op
		}
		
	}
	
}
