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
package com.avaje.ebeaninternal.server.transaction;

import com.avaje.ebean.LogLevel;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.config.lucene.IndexUpdateFuture;
import com.avaje.ebeaninternal.api.DerivedRelationshipData;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.server.lucene.LIndexUpdateFuture;
import com.avaje.ebeaninternal.server.lucene.PersistenceLuceneException;
import com.avaje.ebeaninternal.server.persist.BatchControl;
import com.avaje.ebeaninternal.server.transaction.TransactionManager.OnQueryOnly;

import javax.persistence.PersistenceException;
import javax.persistence.RollbackException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JDBC Connection based transaction.
 */
public class JdbcTransaction implements SpiTransaction {

	private static final Logger logger = Logger.getLogger(JdbcTransaction.class.getName());
	
	private static final String illegalStateMessage = "Transaction is Inactive";

	/**
	 * The associated TransactionManager.
	 */
	final protected TransactionManager manager;
	
	/**
	 * The transaction id.
	 */
	final String id;

	/**
	 * Flag to indicate if this was an explicitly created Transaction.
	 */
	final boolean explicit;
	
	/**
	 * Set to true if the connection has autoCommit=true initially.
	 */
	final boolean autoCommit;
  
	/**
	 * Behaviour for ending query only transactions.
	 */
	final OnQueryOnly onQueryOnly;
	
	/**
	 * The status of the transaction.
	 */
	boolean active;
	
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
	PersistenceContext persistenceContext;
	
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

	LogLevel logLevel;
	
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

	HashSet<Object> persistingBeans = new HashSet<Object>();
	HashSet<Integer> deletingBeansHash;
	
	TransactionLogBuffer logBuffer;
		
	List<LIndexUpdateFuture> indexUpdateFutures;
	
	HashMap<Integer,List<DerivedRelationshipData>> derivedRelMap;

  private final Map<String, Object> userObjects = new ConcurrentHashMap<String, Object>();

	/**
	 * Create a new JdbcTransaction.
	 */
	public JdbcTransaction(String id, boolean explicit, LogLevel logLevel, Connection connection, TransactionManager manager) {
		try {
			this.active = true;
			this.id = id;
			this.explicit = explicit;
			this.logLevel = logLevel;
			this.manager = manager;
			this.connection = connection;
			this.autoCommit = connection.getAutoCommit();
			if (this.autoCommit){
			  connection.setAutoCommit(false);
			}
			this.onQueryOnly = manager == null ? OnQueryOnly.ROLLBACK : manager.getOnQueryOnly();
			this.persistenceContext = new DefaultPersistenceContext();

			this.logBuffer = new TransactionLogBuffer(50, id);
			
		} catch (Exception e) {
			throw new PersistenceException(e);
		}
	}

	public String toString() {
		return "Trans["+id+"]";
	}

	
    public List<DerivedRelationshipData> getDerivedRelationship(Object bean) {
    	if (derivedRelMap == null){
    		return null;
    	}
    	Integer key = Integer.valueOf(System.identityHashCode(bean));
    	return derivedRelMap.get(key);
    }
    
    public void registerDerivedRelationship(DerivedRelationshipData derivedRelationship) {
	    if (derivedRelMap == null){
	    	derivedRelMap = new HashMap<Integer, List<DerivedRelationshipData>>();
	    }
	    Integer key = Integer.valueOf(System.identityHashCode(derivedRelationship.getAssocBean()));
	   
	    List<DerivedRelationshipData> list = derivedRelMap.get(key);
	    if (list == null){
	    	list = new ArrayList<DerivedRelationshipData>();
	    	derivedRelMap.put(key, list);
	    }
	    list.add(derivedRelationship);
    }

	public void addIndexUpdateFuture(LIndexUpdateFuture future) {
        if (indexUpdateFutures == null){
            indexUpdateFutures = new ArrayList<LIndexUpdateFuture>();
        }
        indexUpdateFutures.add(future);
    }
	
    public void waitForIndexUpdates() {
        if (indexUpdateFutures != null){
            try {
                for (IndexUpdateFuture f : indexUpdateFutures) {
                    f.get();
                }
            } catch (InterruptedException e) {
                throw new PersistenceLuceneException(e);
            } catch (ExecutionException e) {
                throw new PersistenceLuceneException(e);
            }
        }
    }

    /**
	 * Add a bean to the registed list.
	 * <p>
	 * This is to handle bi-directional relationships where
	 * both sides Cascade.
	 * </p>
	 */
	public void registerDeleteBean(Integer persistingBean) {
		if (deletingBeansHash == null){
			deletingBeansHash = new HashSet<Integer>();
		}
		deletingBeansHash.add(persistingBean);
	}

	/**
	 * Unregister the persisted bean.
	 */
	public void unregisterDeleteBean(Integer persistedBean) {
		if (deletingBeansHash != null){
			deletingBeansHash.remove(persistedBean);
		}
	}
	
	/**
	 * Return true if this is a bean that has already been saved/deleted.
	 */
	public boolean isRegisteredDeleteBean(Integer persistingBean) {
		if (deletingBeansHash == null){
			return false;
		} else {
			return deletingBeansHash.contains(persistingBean);
		}
	}

	/**
	 * Unregister the persisted bean.
	 */
	public void unregisterBean(Object bean) {
		persistingBeans.remove(bean);
	}
	
	/**
	 * Return true if this is a bean that has already been saved.
	 * This will register the bean if it is not already.
	 */
	public boolean isRegisteredBean(Object bean) {
		return !persistingBeans.add(bean);
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

	/**
	 * Return the batchSize specifically set for this transaction or 0.
	 * <p>
	 * Returning 0 implies to use the system wide default batch size.
	 * </p>
	 */
	public int getBatchSize() {
		return batchSize;
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
	 * Set the BatchControl to the transaction.
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
	public void flushBatch() {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		if (batchControl != null){
			batchControl.flush();
		}
	}
	
	public void batchFlush() {
	    flushBatch();
	}
	

	/**
	 * Return the persistence context associated with this transaction.
	 */
	public PersistenceContext getPersistenceContext() {
		return persistenceContext;
	}

	/**
	 * Set the persistence context to this transaction.
	 * <p>
	 * This could be considered similar to EJB3 Extended PersistanceContext. In
	 * that you get the PersistanceContext from a transaction, hold onto it, and
	 * then set it back later to a second transaction.
	 * </p>
	 */
	public void setPersistenceContext(PersistenceContext context) {
		if (!isActive()) {
			throw new IllegalStateException(illegalStateMessage);
		}
		this.persistenceContext = context;
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
	 * Set whether transaction logging is on for this transaction.
	 */
	public void setLoggingOn(boolean loggingOn) {
	    if (loggingOn){
	        logLevel = LogLevel.SQL;
	    } else {
	        logLevel = LogLevel.NONE;
	    }
	}

	/**
	 * Return true if this was an explicitly created transaction.
	 */
	public boolean isExplicit() {
		return explicit;
	}

	
	public boolean isLogSql() {
        return logLevel.ordinal() >= LogLevel.SQL.ordinal();
    }

    public boolean isLogSummary() {
        return logLevel.ordinal() >= LogLevel.SUMMARY.ordinal();
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Log a message to the transaction log - for PUBLIC use.
     */
    public void log(String msg) {
    	if (isLogSummary()){
    		logInternal(msg);
    	}
    }
    
    /**
	 * Log a message to the transaction log - for Ebean INTERNAL use.
	 * The LogLevel should be explicitly checked before calling this method.
	 */
	public void logInternal(String msg) {
		if (manager != null) {
		    if (logBuffer.add(msg)) {
		        // buffer full so flush it
	            manager.log(logBuffer);
	            logBuffer = logBuffer.newBuffer();
		    }
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
			  // reset readOnly status prior to returning to pool
				connection.setReadOnly(false);
			}
		} catch (SQLException e){
			logger.log(Level.SEVERE, "Error setting to readOnly?", e);
		}
		try {
  		if (this.autoCommit){
  		  // reset the autoCommit status prior to returning to pool
        connection.setAutoCommit(true);
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
		active = false;
	}
	
	public TransactionLogBuffer getLogBuffer() {
        return logBuffer;
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
		return active;
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

  public void putUserObject(String name, Object value) {
    userObjects.put(name, value);
  }

  public Object getUserObject(String name) {
    return userObjects.get(name);
  }

  public final TransactionManager getTransactionManger() {
    return manager;
  }
}
