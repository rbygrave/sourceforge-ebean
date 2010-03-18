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
package com.avaje.ebeaninternal.server.jdbc;

import com.avaje.ebean.config.PstmtDelegate;
import com.avaje.ebeaninternal.server.core.PstmtBatch;

import javax.persistence.OptimisticLockException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * Oracle specific handling of JDBC batching.
 * <p>
 * I guess some people don't need to follow the jdbc specification.
 * </p>
 * 
 * @author rbygrave
 */
public class OraclePstmtBatch implements PstmtBatch {

	private final PstmtDelegate pstmtDelegate;

	private static final Method METHOD_SET_EXECUTE_BATCH;
	private static final Method METHOD_SEND_BATCH;

	private static final RuntimeException INIT_EXCEPTION;

	static
	{
		RuntimeException initException = null;
		Method mSetExecuteBatch = null;
		Method mSendBatch = null;

		try {
			Class ops = Class.forName("oracle.jdbc.OraclePreparedStatement");

			mSetExecuteBatch = ops.getMethod("setExecuteBatch", new Class[] { int.class });
			mSendBatch = ops.getMethod("sendBatch");
		}
		catch (NoSuchMethodException e) {
			initException = new RuntimeException("problems initializing oracle reflection", e);
			initException.fillInStackTrace();
		}
		catch (ClassNotFoundException e) {
			initException = new RuntimeException("problems initializing oracle reflection", e);
			initException.fillInStackTrace();
		}

		INIT_EXCEPTION = initException;
		METHOD_SET_EXECUTE_BATCH = mSetExecuteBatch;
		METHOD_SEND_BATCH = mSendBatch;
	}

	public OraclePstmtBatch(PstmtDelegate pstmtDelegate) {
		this.pstmtDelegate = pstmtDelegate;
	}

	public void setBatchSize(PreparedStatement pstmt, int batchSize) {
		if (INIT_EXCEPTION != null) {
			throw INIT_EXCEPTION;
		}

		try {
			METHOD_SET_EXECUTE_BATCH.invoke(pstmtDelegate.unwrap(pstmt), batchSize+1);
		}
		catch (IllegalAccessException e) {
			String m = "Error with Oracle setExecuteBatch "+(batchSize+1);
			throw new RuntimeException(m, e);
		}
		catch (InvocationTargetException e) {
			String m = "Error with Oracle setExecuteBatch "+(batchSize+1);
			throw new RuntimeException(m, e);
		}
		/*
		try {
			getOraStmt(pstmt).setExecuteBatch(batchSize+1);
		} catch (SQLException e){
			String m = "Error with Oracle setExecuteBatch "+(batchSize+1);
			throw new RuntimeException(m, e);
		}
		*/
	}
	
	/**
	 * Simply calls standard pstmt.addBatch().
	 */
	public void addBatch(PreparedStatement pstmt) throws SQLException {
		pstmt.executeUpdate();
	}
	
	public int executeBatch(PreparedStatement pstmt, int expectedRows, String sql, boolean occCheck) throws SQLException {
		
		if (INIT_EXCEPTION != null) {
			throw INIT_EXCEPTION;
		}

		// OraclePreparedStatement oStmt = getOraStmt(pstmt);
		// int rows = oStmt.sendBatch();

		int rows;
		try {
			rows = ((Integer) METHOD_SEND_BATCH.invoke(pstmtDelegate.unwrap(pstmt))).intValue();
		}
		catch (IllegalAccessException e) {
			throw new SQLException(e);
		}
		catch (InvocationTargetException e) {
			throw new SQLException(e);
		}
		if (occCheck && rows != expectedRows) {
			throw new OptimisticLockException("Batch execution expected "+expectedRows+" but got "+rows+"  sql:"+sql);
		}
		
		return rows;
	}

	/*
	protected OraclePreparedStatement getOraStmt(PreparedStatement pstmt) {
		
		return (OraclePreparedStatement)pstmtDelegate.unwrap(pstmt);		
	}
	*/
}
