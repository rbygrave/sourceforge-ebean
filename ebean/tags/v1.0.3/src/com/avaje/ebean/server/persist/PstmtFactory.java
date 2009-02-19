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
package com.avaje.ebean.server.persist;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.avaje.ebean.server.core.ServerTransaction;

/**
 * Factory for creating Statements.
 * <p>
 * This is only used by CallableSql and UpdateSql requests and does not support
 * getGeneratedKeys.
 * </p>
 */
public class PstmtFactory {

	/**
	 * Get a callable statement without any batching.
	 */
	public CallableStatement getCstmt(ServerTransaction t, String sql) throws SQLException {
		Connection conn = t.getInternalConnection();
		return conn.prepareCall(sql);
	}

	/**
	 * Get a prepared statement without any batching.
	 */
	public PreparedStatement getPstmt(ServerTransaction t, String sql) throws SQLException {
		Connection conn = t.getInternalConnection();
		return conn.prepareStatement(sql);
	}

	/**
	 * Return a prepared statement taking into account batch requirements.
	 */
	public PreparedStatement getPstmt(ServerTransaction t, String sql, BatchPostExecute batchExe)
			throws SQLException {

		BatchedPstmtHolder batch = t.getBatchControl().getPstmtHolder();
		PreparedStatement stmt = batch.getStmt(sql, batchExe);

		if (stmt != null) {
			return stmt;
		}

		Connection conn = t.getInternalConnection();
		stmt = conn.prepareStatement(sql);

		BatchedPstmt bs = new BatchedPstmt(stmt, false, sql);
		batch.addStmt(bs, batchExe);
		return stmt;
	}

	/**
	 * Return a callable statement taking into account batch requirements.
	 */
	public CallableStatement getCstmt(ServerTransaction t, String sql, BatchPostExecute batchExe)
			throws SQLException {

		BatchedPstmtHolder batch = t.getBatchControl().getPstmtHolder();
		CallableStatement stmt = (CallableStatement) batch.getStmt(sql, batchExe);

		if (stmt != null) {
			return stmt;
		}

		Connection conn = t.getInternalConnection();
		stmt = conn.prepareCall(sql);

		BatchedPstmt bs = new BatchedPstmt(stmt, false, sql);
		batch.addStmt(bs, batchExe);
		return stmt;
	}
}
