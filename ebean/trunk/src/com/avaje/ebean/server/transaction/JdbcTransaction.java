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
package com.avaje.ebean.server.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;

import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.core.TransactionContext;
import com.avaje.ebean.server.persist.BatchControl;
import com.avaje.ebean.server.transaction.TransactionManager.OnQueryOnly;

/**
 * JDBC Connection based transaction.
 */
public class JdbcTransaction implements ServerTransaction {

	private static final Logger logger = Logger.getLogger(JdbcTransaction.class.getName());
	
	private static final String illegalStateMessage = "Transaction is Inactive";

	private static final int STATUS_ACTIVE = 100;

	private static final int STATUS_INACTIVE = 101;


	/**
	 * The associated TransactionManager.
	 */
	final TransactionManager manager;
	
	/**
	 * The status of the transaction.
	 */
	int activeStatus = STATUS_INACTIVE;
	
	/**
	 * The transaction id.
	 */
	final String id;

	/**
	 * Flag to indicate if this was an explicitly created Transaction.
	 */
	final boolean explicit;
	
	/**
	 * The underlying Connection.
	 */
	Connection connection;

	/**
	 * Used to queue up persist requests for batch execution.
	 */
	BatchControl batchControl;
	
	/**
	 * The event which holds persisted beans.
	 */
	TransactionEvent event;
	
	/**
	 * Holder of the objects fetched to ensure unique objects are used.
	 */
	TransactionContext transactionContext;
	
	/**
	 * Used to give developers more control over the insert update and delete
	 * functionality.
	 */
	boolean persistCascade = true;

	/**
	 * Flag used for performance to skip commit or rollback of query only
	 * transactions in read committed transaction isolation.
	 */
	boolean queryOnly = true;
	
	boolean localReadOnly;

	/**
	 * Behaviour for ending query only transactions.
	 */
	final OnQueryOnly onQueryOnly;
	
	/**
	 * Flag to explicitly turn off transaction logging for this transaction.
	 */
	boolean loggingOn = true;

	/**
	 * Set to true if using batch processing.
	 */
	boolean batchMode;

	int batchSize = -1;
	
	boolean batchFlushOnQuery = true;
	
	Boolean batchGetGeneratedKeys;

	Boolean batchFlushOnMixed;
	
	/**
	 * The depth used by batch processing to help the ordering
	 * of statements.
	 */
	int depth = 0;

	/**
	 * Create a new JdbcTransaction.
	 */
	public JdbcTransaction(String id, boolean explicit, Connection connection, TransactionManager manager) {
		try {
			this.activeStatus = STATUS_ACTIVE;
			this.id = id;
			this.explicit = explicit;
			this.manager = manager;
			this.connection = connection;
			this.onQueryOnly = manager == null ? OnQueryOnly.ROLLBACK : manager.getOnQueryOnly();
			this.transactionContext = new TransContext();

		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	public String toString() {
		return "Trans["+id+"]";
	}
	
	/**
	 * Return the depth of the current persist request plus the diff.
	 * This has the effect of changing the current depth and returning
	 * the new value. Pass diff=0 to return the current depth.
	 * <p>
	 * The depth of 0 is for the initial persist request. It is 
	 * modified as the cascading of the save or delete traverses to
	 * the the associated Ones (-1) and associated Manys (+1).
	 * </p> 
	 * <p>
	 * The depth is used to help the ordering of batched statements.
	 * </p>
	 * 
	 * @param diff the amount to add or subtract from the depth.
	 * @return the current depth plus the diff
	 */
	public int depth(int diff) {
		depth += diff;
		return depth;
	}
	
	public boolean isReadOnly() {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		try {
			return connection.isReadOnly();
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	public void setReadOnly(boolean readOnly) {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		try {
			localReadOnly = readOnly;
			connection.setReadOnly(readOnly);
		} catch (SQLException e) {
			throw new PersistenceException(e);
		}
	}
	
	public void setBatchMode(boolean batchMode) {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		this.batchMode = batchMode;
	}
	
	public void setBatchGetGeneratedKeys(boolean getGeneratedKeys) {
		this.batchGetGeneratedKeys = getGeneratedKeys;
		if (batchControl != null){
			batchControl.setGetGeneratedKeys(getGeneratedKeys);
		}
	}
	
	public void setBatchFlushOnMixed(boolean batchFlushOnMixed) {
		this.batchFlushOnMixed = batchFlushOnMixed;
		if (batchControl != null){
			batchControl.setBatchFlushOnMixed(batchFlushOnMixed);
		}
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
		if (batchControl != null){
			batchControl.setBatchSize(batchSize);
		}
	}

	public boolean isBatchFlushOnQuery() {
		return batchFlushOnQuery;
	}

	public void setBatchFlushOnQuery(boolean batchFlushOnQuery) {
		this.batchFlushOnQuery = batchFlushOnQuery;
	}

	/**
	 * Return true if this request should be batched. Returning false
	 * means that this request should be executed immediately.
	 */
	public boolean isBatchThisRequest() {
		if (!explicit && depth <= 0){
			// implicit transaction ... no gain
			// by batching where depth <= 0
			return false;
		}
		return batchMode;
	}
	
    
	public BatchControl getBatchControl(){
		return batchControl;
	}
	
	/**
	 * Set the BatchQueue to the transaction.
	 * This is done once per transaction on the first persist request.  
	 */
	public void setBatchControl(BatchControl batchControl){
		queryOnly = false;
		this.batchControl = batchControl;
		// in case these parameters have already been set
		if (batchGetGeneratedKeys != null){
			batchControl.setGetGeneratedKeys(batchGetGeneratedKeys);
		}
		if (batchSize != -1) {
			batchControl.setBatchSize(batchSize);
		}
		if (batchFlushOnMixed != null){
			batchControl.setBatchFlushOnMixed(batchFlushOnMixed);
		}
	}


	/**
	 * Flush any queued persist requests.
	 * <p>
	 * This is general will result in a number of batched
	 * PreparedStatements executing.
	 * </p>
	 */
	public void batchFlush() {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		if (batchControl != null){
			batchControl.flush();
		}
	}

	/**
	 * Return the persistence context associated with this transaction.
	 */
	public TransactionContext getTransactionContext() {
		return transactionContext;
	}

	/**
	 * Set the persistence context to this transaction.
	 * <p>
	 * This could be considered similar to EJB3 Extended PersistanceContext. In
	 * that you get the PersistanceContext from a transaction, hold onto it, and
	 * then set it back later to a second transaction.
	 * </p>
	 */
	public void setTransactionContext(TransactionContext context) {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		this.transactionContext = context;
	}

	/**
	 * Return the underlying TransactionEvent.
	 */
	public TransactionEvent getEvent() {
		queryOnly = false;
		if (event == null){
			event = new TransactionEvent();
		}
		return event;
	}

	/**
	 * Is transaction logging on for this transaction.
	 */
	public boolean isLoggingOn() {
		return loggingOn;
	}

	/**
	 * Set whether transaction logging is on for this transaction.
	 */
	public void setLoggingOn(boolean loggingOn) {
		this.loggingOn = loggingOn;
	}

	/**
	 * Return true if this was an explicitly created transaction.
	 */
	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * Log a message to the transaction log.
	 */
	public void log(String msg) {
		if (loggingOn && manager != null) {
			manager.log(this, msg);
		}
	}

	/**
	 * Return the transaction id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Return the underlying connection for internal use.
	 */
	public Connection getInternalConnection() {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		return connection;
	}

	/**
	 * Return the underlying connection for public use.
	 */
	public Connection getConnection() {
		queryOnly = false;
		return getInternalConnection();
	}

	protected void deactivate() {
		try {
			if (localReadOnly){
				connection.setReadOnly(false);
			}
		} catch (SQLException e){
			logger.log(Level.SEVERE, "Error setting to readOnly?", e);
		}
		try {
			connection.close();
		} catch (Exception ex) {
			// the connection pool will automatically remove the
			// connection if it does not pass the test
			logger.log(Level.SEVERE, "Error closing connection", ex);
		}
		connection = null;
		activeStatus = STATUS_INACTIVE;
	}

	/**
	 * Notify the transaction manager.
	 */
	protected void notifyCommit() {
		if (manager == null){
			return;
		}
		if (queryOnly){
			manager.notifyOfQueryOnly(true, this, null);
		} else {
			manager.notifyOfCommit(this);
		}
	}

	/**
	 * Rollback, Commit or Close for query only transaction.
	 * <p>
	 * For a transaction that was used for queries only we can choose
	 * to either rollback or just close the connection for performance.
	 * </p>
	 */
	private void commitQueryOnly() {
		try {
			switch (onQueryOnly) {
			case ROLLBACK:
				connection.rollback();
				break;
			case COMMIT:
				connection.commit();
				break;
			case CLOSE_ON_READCOMMITTED:
				// Connection is closed via deactivate() which follows
				// This optimisation is only available at READ COMMITTED Isolation
				break;
			default:
				connection.rollback();
			}
		} catch (SQLException e) {
			String m = "Error when ending a query only transaction via " + onQueryOnly;
			logger.log(Level.SEVERE, m, e);
		}
	}

	/**
	 * Commit the transaction.
	 */
	public void commit() throws RollbackException {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		try {
			if (queryOnly) {
				// can rollback or just close for performance
				commitQueryOnly();
			} else {
				// commit
				if (batchControl != null && !batchControl.isEmpty()){
					batchControl.flush();
				}
				connection.commit();
			}
			// these will not throw an exception
			deactivate();
			notifyCommit();

		} catch (Exception e) {
			throw new RollbackException(e);
		}
	}

	/**
	 * Notify the transaction manager.
	 */
	protected void notifyRollback(Throwable cause) {
		if (manager == null){
			return;
		}
		if (queryOnly){
			manager.notifyOfQueryOnly(false, this, cause);
		} else {
			manager.notifyOfRollback(this, cause);
		}
	}
	
	/**
	 * Rollback the transaction.
	 */
	public void rollback() throws PersistenceException {
		rollback(null);
	}
	
	/**
	 * Rollback the transaction.
	 * If there is a throwable it is logged as the cause in the transaction log.
	 */
	public void rollback(Throwable cause) throws PersistenceException {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		try {
			connection.rollback();

			// these will not throw an exception
			deactivate();
			notifyRollback(cause);
			
		} catch (Exception ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * If the transaction is active then perform rollback.
	 */
	public void end() throws PersistenceException {
		if (isActive()) {
			rollback();
		}
	}

	/**
	 * Return true if the transaction is active.
	 */
	public boolean isActive() {
		return (activeStatus == STATUS_ACTIVE);
	}


	public boolean isPersistCascade() {
		return persistCascade;
	}

	public void setPersistCascade(boolean persistCascade) {
		this.persistCascade = persistCascade;
	}

	public void addModification(String tableName, boolean inserts, boolean updates, boolean deletes) {
		getEvent().add(tableName, inserts, updates, deletes);
		
	}

	
}
