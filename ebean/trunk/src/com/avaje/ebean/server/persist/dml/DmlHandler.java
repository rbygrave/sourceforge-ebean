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
package com.avaje.ebean.server.persist.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.internal.ServerTransaction;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.jmx.MAdminLogging;
import com.avaje.ebean.server.persist.BatchPostExecute;
import com.avaje.ebean.server.persist.BatchedPstmt;
import com.avaje.ebean.server.persist.BatchedPstmtHolder;
import com.avaje.ebean.server.persist.dmlbind.BindableRequest;


/**
 * Base class for Handler implementations.
 */
public abstract class DmlHandler implements PersistHandler, BindableRequest {

	static final Logger logger = Logger.getLogger(DmlHandler.class.getName());
	
	/**
	 * Position in the PreparedStatement.
	 */
	int index;

	/**
	 * The PreparedStatement used for the dml.
	 */
	PreparedStatement pstmt;
	
	ArrayList<UpdateGenValue> updateGenValues;
	
	/**
	 * The originating request.
	 */
	protected final PersistRequestBean<?> persistRequest;

	final int logLevel;

	final StringBuilder bindLog;

	final boolean loggingBind;

	final Set<String> loadedProps;

	final ServerTransaction transaction;

	protected DmlHandler(PersistRequestBean<?> persistRequest) {
		this.persistRequest = persistRequest;
		
		EntityBeanIntercept ebi = persistRequest.getEntityBeanIntercept();
		if (ebi == null){
			loadedProps = null;
		} else {
			loadedProps = ebi.getLoadedProps();
		}
		
		transaction = persistRequest.getTransaction();
		logLevel = persistRequest.getLogLevel();
		if (logLevel >= MAdminLogging.BIND) {
			loggingBind = true;
			bindLog = new StringBuilder();
		} else {
			loggingBind = false;
			bindLog = null;
		}
	}

	public PersistRequestBean<?> getPersistRequest() {
		return persistRequest;
	}
	
	/**
	 * Get the sql and bind the statement.
	 */
	public abstract void bind() throws SQLException;

	/**
	 * Execute now for non-batch execution.
	 */
	public abstract int execute() throws SQLException;

	/**
	 * Add this for batch execution.
	 */
	public void addBatch() throws SQLException {
		pstmt.addBatch();
	}

	/**
	 * Close the underlying statement.
	 */
	public void close() {
		try {
			if (pstmt != null){
				pstmt.close();
			}
		} catch (SQLException ex) {
        	logger.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Set the Id value that was bound. This value is used for logging summary
	 * level information.
	 */
	public void setIdValue(Object idValue) {
		persistRequest.setBoundId(idValue);
	}

	/**
	 * Log the bind information to the transaction log.
	 */
	protected void logBinding() {
		if (logLevel >= MAdminLogging.BIND) {
			if (transaction.isLoggingOn()) {
				transaction.log(bindLog.toString());
			}
		}
	}

	/**
	 * Log the sql to the transaction log.
	 */
	protected void logSql(String sql) {
		if (logLevel >= MAdminLogging.SQL) {
			if (transaction.isLoggingOn()) {
				transaction.log(sql);
			}
		}
	}
	
	
	public boolean isIncluded(BeanProperty prop) {
		return (loadedProps == null || loadedProps.contains(prop.getName()));
	}

	/**
	 * Bind a raw value. Used to bind the discriminator column.
	 */
	public Object bind(String propName, Object value, int sqlType) throws SQLException {
		if (loggingBind) {
			bindLog.append(propName).append("=");
			bindLog.append(value).append(", ");
		}
		pstmt.setObject(++index, value, sqlType);
		return value;
	}
	
	/**
	 * Bind the value to the preparedStatement.
	 */
	public Object bind(Object value, BeanProperty prop, String propName, boolean bindNull) throws SQLException {
				
		if (!bindNull){
			// support Oracle conversion of empty string to null 
			value = prop.getDbNullValue(value);
		}

		if (!bindNull && value == null) { 
			// where will have IS NULL clause so don't actually bind
			if (loggingBind) {
				bindLog.append(propName).append("=");
				bindLog.append("null, ");
			}
		} else {
			if (loggingBind) {
				bindLog.append(propName).append("=");
				if (prop.isLob()){
					bindLog.append("[LOB]");
				} else {
					String sv = String.valueOf(value);
					if (sv.length() > 50){
						sv = sv.substring(0,47)+"...";
					}
					bindLog.append(sv);
				}
				bindLog.append(", ");
			}
			// do the actual binding to PreparedStatement
			prop.bind(pstmt, ++index, value);
		}
		return value;
	}

	/**
	 * Add the comment to the bind information log.
	 */
	protected void bindLogAppend(String comment) {
		if (loggingBind) {
			bindLog.append(comment);
		}
	}

	/**
	 * Register a generated value on a update. This can not be set to the bean
	 * until after the where clause has been bound for concurrency checking.
	 * <p>
	 * GeneratedProperty values are likely going to be used for optimistic
	 * concurrency checking. This includes 'counter' and 'update timestamp'
	 * generation.
	 * </p>
	 */
	public void registerUpdateGenValue(BeanProperty prop, Object bean, Object value) {
		if (updateGenValues == null) {
			updateGenValues = new ArrayList<UpdateGenValue>();
		}
		updateGenValues.add(new UpdateGenValue(prop, bean, value));
	}



	/**
	 * Set any update generated values to the bean. Must be called after where
	 * clause has been bound.
	 */
	public void setUpdateGenValues() {
		if (updateGenValues != null) {
			for (int i = 0; i < updateGenValues.size(); i++) {
				UpdateGenValue updGenVal = updateGenValues.get(i);
				updGenVal.setValue();
			}
		}
	}

	
	/**
	 * Check with useGeneratedKeys to get appropriate PreparedStatement.
	 */
	protected PreparedStatement getPstmt(ServerTransaction t, String sql, boolean genKeys) throws SQLException {
		Connection conn = t.getInternalConnection();
		if (genKeys) {
			// the Id generated is always the first column
			// Required to stop Oracle10 giving us Oracle rowId??
			// Other jdbc drivers seem fine without this hint.
            int[] columns = {1};
            return conn.prepareStatement(sql, columns);
            
        } else {
            return conn.prepareStatement(sql);
        }
	}

	/**
	 * Return a prepared statement taking into account batch requirements.
	 */
	protected PreparedStatement getPstmt(ServerTransaction t, String sql, BatchPostExecute batchExe, boolean genKeys)
			throws SQLException {

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
	
	/**
	 * Hold the values from GeneratedValue that need to be set to the bean
	 * property after the where clause has been built.
	 */
	private static final class UpdateGenValue {

		private final BeanProperty property;

		private final Object bean;

		private final Object value;

		private UpdateGenValue(BeanProperty property, Object bean, Object value) {
			this.property = property;
			this.bean = bean;
			this.value = value;
		}

		/**
		 * Set the value to the bean property.
		 */
		private void setValue() {
			// support PropertyChangeSupport
			property.setValueIntercept(bean, value);
		}
	}
}
