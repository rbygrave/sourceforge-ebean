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
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Executes the select row count query.
 */
public class CQueryRowCount {

	private static final Logger logger = Logger.getLogger(CQueryRowCount.class.getName());

	/**
	 * The overall find request wrapper object.
	 */
	final OrmQueryRequest<?> request;

	final BeanDescriptor<?> desc;

	final OrmQuery<?> query;

	/**
	 * Where clause predicates.
	 */
	final CQueryPredicates predicates;

	/**
	 * The final sql that is generated.
	 */
	final String sql;

	/**
	 * The resultSet that is read and converted to objects.
	 */
	ResultSet rset;

	/**
	 * The statement used to create the resultSet.
	 */
	PreparedStatement pstmt;

	String bindLog;

	long startNano;
	
	int executionTimeMicros;

	int rowCount;
	
	/**
	 * Create the Sql select based on the request.
	 */
	public CQueryRowCount(OrmQueryRequest<?> request, CQueryPredicates predicates, String sql) {
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
			sb.append("FindRowCount exeMicros[").append(executionTimeMicros)
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
	public int findRowCount() throws SQLException {

		startNano = System.nanoTime();
		try {
			
			ServerTransaction t = request.getTransaction();
			Connection conn = t.getInternalConnection();
			pstmt = conn.prepareStatement(sql);
	
			if (query.getTimeout() > 0){
				pstmt.setQueryTimeout(query.getTimeout());
			}
	
			bindLog = predicates.bind(pstmt);
	
			rset = pstmt.executeQuery();
			
			if (!rset.next()){
				throw new PersistenceException("Expecting 1 row but got none?");
			} 

			rowCount = rset.getInt(1);
			
			long exeNano = System.nanoTime() - startNano;
			executionTimeMicros = (int)exeNano/1000;
		
			return rowCount;
			
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

	
}
