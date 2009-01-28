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
package org.avaje.ebean.server.persist.mapbean;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import org.avaje.ebean.MapBean;
import org.avaje.ebean.control.LogControl;
import org.avaje.ebean.server.core.PersistRequest;
import org.avaje.ebean.server.core.ServerTransaction;
import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.persist.BatchPostExecute;
import org.avaje.ebean.server.persist.BatchedPstmt;
import org.avaje.ebean.server.persist.BatchedPstmtHolder;
import org.avaje.ebean.server.persist.BindValues;
import org.avaje.ebean.server.persist.Binder;
import org.avaje.lib.log.LogFactory;

/**
 * Base class for Insert Update and Delete of MapBeans.
 */
public abstract class BaseMapBean {

	private static final Logger logger = LogFactory.get(BaseMapBean.class);
	
	/**
	 * Set to true if this is an insert that is going to use getGeneratedKeys.
	 */
	boolean usingGeneratedKeys;

	final PersistRequest request;

	final ServerTransaction transaction;
	
	final Binder binder;

	final MapBean mapBean;

	final BeanDescriptor desc;
	
	/**
	 * The log of binding information.
	 */
	final StringBuilder bindLog;
	
	/**
	 * The generated sql.
	 */
	final StringBuilder genSql = new StringBuilder();

	/**
	 * The bind values.
	 */
	final BindValues bindValues;

	/**
	 * Set to true if we are going to log the binding information.
	 */
	private final boolean loggingBind;
	
	/**
	 * The log level for this request.
	 */
	private final int logLevel;
	
	@SuppressWarnings("unchecked")
	public BaseMapBean(Binder binder, PersistRequest request) {
		this.binder = binder;
		this.bindValues = new BindValues();
		this.request = request;
		this.logLevel = request.getLogLevel();
		if (logLevel >= LogControl.LOG_BIND){
			loggingBind = true;
			bindLog = new StringBuilder();
		} else {
			loggingBind = false;
			bindLog = null;
		}
		desc = request.getBeanDescriptor();
		mapBean = (MapBean) request.getBean();
		transaction = request.getTransaction();
	}

	protected void bindValue(Object value, int dbType, String name){
		value = binder.convertType(value, dbType);
		bindValues.add(value, dbType, name);
	}
	
	/**
	 * Execute the statement for non-batched execution.
	 */
	protected abstract void executeStmt(PreparedStatement pstmt) throws SQLException;

	/**
	 * Execute the insert update or delete. This takes into account batch or
	 * non-batch execution.
	 */
	public void execute() {

		String dml = genSql.toString();
		logSql(dml);
		
		boolean isBatch = transaction.isBatchThisRequest();

		PreparedStatement pstmt = null;

		try {
			if (isBatch) {
				pstmt = getPstmt(transaction, dml, request, usingGeneratedKeys);
			} else {
				pstmt = getPstmt(transaction, dml, usingGeneratedKeys);
			}

			binder.bind(bindValues, pstmt, bindLog);

			logBinding();
			
			if (isBatch) {
				pstmt.addBatch();
			} else {
				executeStmt(pstmt);
			}
			
		} catch (SQLException ex) {
			throw new PersistenceException(ex);

		} finally {
			if (!isBatch && pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException ex) {
		        	logger.log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Append a comment to the bindLog.
	 */
	protected void bindLogAppend(String comment) {
		if (loggingBind) {
			bindLog.append(comment);
		}
	}
	
	/**
	 * Log the binding to the transaction log.
	 */
	private void logBinding() {
		if (logLevel >= LogControl.LOG_BIND) {
			if (transaction.isLoggingOn()) {
				bindLog.append("]");
				transaction.log(bindLog.toString());
			}
		}
	}

	/**
	 * Log the sql to the transaction log.
	 */
	private void logSql(String s) {
		if (logLevel >= LogControl.LOG_SQL) {
			if (transaction.isLoggingOn()) {
				transaction.log(s);
			}
		}
	}
	
	/**
	 * Check with useGeneratedKeys to get appropriate PreparedStatement.
	 */
	protected PreparedStatement getPstmt(ServerTransaction t, String sql, boolean genKeys)
			throws SQLException {
		Connection conn = t.getInternalConnection();
		if (genKeys) {
			// the Id generated is always the first column
			// Required to stop Oracle10 giving us Oracle rowId??
			// Other jdbc drivers seem fine without this hint.
			int[] columns = { 1 };
			return conn.prepareStatement(sql, columns);

		} else {
			return conn.prepareStatement(sql);
		}
	}

	/**
	 * Return a prepared statement taking into account batch requirements.
	 */
	protected PreparedStatement getPstmt(ServerTransaction t, String sql,
			BatchPostExecute batchExe, boolean genKeys) throws SQLException {

		BatchedPstmtHolder batch = t.getBatchControl().getPstmtHolder();
		PreparedStatement stmt = batch.getStmt(sql, batchExe);

		if (stmt != null) {
			return stmt;
		}

		stmt = getPstmt(t, sql, genKeys);

		BatchedPstmt bs = new BatchedPstmt(stmt, genKeys, sql);
		batch.addStmt(bs, batchExe);
		return stmt;
	}

}
