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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.AdminLogging.TxLogLevel;
import com.avaje.ebean.AdminLogging.TxLogSharing;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebean.internal.ServerTransaction;
import com.avaje.ebean.internal.TransactionEvent;
import com.avaje.ebean.internal.TransactionEventTable;
import com.avaje.ebean.internal.TransactionEventTable.TableIUD;
import com.avaje.ebean.net.Constants;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.thread.ThreadPool;
import com.avaje.ebean.server.lib.thread.ThreadPoolManager;

/**
 * Manages transactions.
 * <p>
 * Keeps the Cache, Cluster and Lucene indexes in synch when transactions are
 * committed.
 * </p>
 */
public class TransactionManager implements Constants {

	private static final Logger logger = Logger.getLogger(TransactionManager.class.getName());
	
	/**
	 * The behaviour desired when ending a query only transaction.
	 */
	public enum OnQueryOnly {
		
		/**
		 * Rollback the transaction.
		 */
		ROLLBACK,
		
		/**
		 * Just close the transaction.
		 */
		CLOSE_ON_READCOMMITTED,
		
		/**
		 * Commit the transaction
		 */
		COMMIT
	}
	
	private final BeanDescriptorManager beanDescriptorManager;
	
	/**
	 * The logger.
	 */
	private final TransactionLogManager transLogger;

	/**
	 * for synchronising.
	 */
	private final Object monitor = new Object();

	/**
	 * Prefix for transaction id's (logging).
	 */
	private final String prefix;

	private final String externalTransPrefix;

	/**
	 * The dataSource of connections.
	 */
	private final DataSource dataSource;

	private final boolean logAllCommits;

	/**
	 * Flag to indicate the default Isolation is READ COMMITTED. This enables us
	 * to close queryOnly transactions rather than commit or rollback them.
	 */
	private final OnQueryOnly onQueryOnly;

	/**
	 * The default batchMode for transactions.
	 */
	private final boolean defaultBatchMode;

	/**
	 * Background threading of post commit processing.
	 */
	private final ThreadPool threadPool;
			
	private final boolean logCommitEvent;
	
	private final ClusterManager clusterManager;
	
	private final int debugLevel;

	private final String serverName;
	
	/**
	 * Id's for transaction logging.
	 */
	private long transactionCounter = 1000;

	/**
	 * Create the TransactionManager
	 */
	public TransactionManager(ClusterManager clusterManager, ServerConfig config, 
			BeanDescriptorManager descMgr) {
		
		this.beanDescriptorManager = descMgr;
		this.clusterManager = clusterManager;
		this.serverName = config.getName();
		
		this.transLogger = new TransactionLogManager(config);
		this.threadPool = ThreadPoolManager.getThreadPool("TransactionManager");
		if (threadPool.getMinSize() == 0) {
			threadPool.setMinSize(1);
		}
		
		this.dataSource = config.getDataSource();
		
		this.debugLevel = config.getTransactionDebugLevel();	
		this.defaultBatchMode = config.getDatabasePlatform().isDefaultBatching();
		
		this.prefix = GlobalProperties.get("transaction.prefix", "");
		this.externalTransPrefix = GlobalProperties.get("transaction.prefix", "e");
		this.logCommitEvent = GlobalProperties.getBoolean("log.commit", false);
		this.logAllCommits = GlobalProperties.getBoolean("transaction.logallcommits", false);
		
		String value = GlobalProperties.get("transaction.onqueryonly", "ROLLBACK").toUpperCase().trim();

		this.onQueryOnly = getOnQueryOnly(value, dataSource);
	}
	
	/**
	 * Return the logging level for transactions.
	 */
	public TxLogLevel getTransactionLogLevel(){
		return transLogger.getLogLevel();
	}
	
	/**
	 * Set the log level for transactions.
	 */
	public void setTransactionLogLevel(TxLogLevel txLogLevel){
		transLogger.setLogLevel(txLogLevel);
	}

	/**
	 * Return the log sharing mode.
	 */
	public TxLogSharing getTransactionLogSharing(){
		return transLogger.getLogSharing();
	}
	
	/**
	 * Set the log sharing mode.
	 */
	public void setTransactionLogSharing(TxLogSharing txLogSharing){
		transLogger.setLogSharing(txLogSharing);
	}
	
	
	/**
	 * Return the behaviour to use when a query only transaction is committed.
	 * <p>
	 * There is a potential optimisation available when read committed is the default 
	 * isolation level. If it is, then Connections used only for queries do not require 
	 * commit or rollback but instead can just be put back into the pool via close().
	 * </p>
	 * <p>
	 * If the Isolation level is higher (say SERIALIZABLE) then Connections used
	 * just for queries do need to be committed or rollback after the query.
	 * </p>
	 */
	private OnQueryOnly getOnQueryOnly(String onQueryOnly, DataSource ds) {
		
		
		if (onQueryOnly.equals("COMMIT")){
			return OnQueryOnly.COMMIT;
		}
		if (onQueryOnly.startsWith("CLOSE")){
			if (!isReadCommitedIsolation(ds)){
				String m = "transaction.queryonlyclose is true but the transaction Isolation Level is not READ_COMMITTED";
				throw new PersistenceException(m);
			} else {
				return OnQueryOnly.CLOSE_ON_READCOMMITTED;				
			}
		}
		// default to rollback
		return OnQueryOnly.ROLLBACK;
	}		
	
	/**
	 * Return true if the isolation level is read committed.
	 */
	private boolean isReadCommitedIsolation(DataSource ds) {
		
		Connection c = null;
		try {
			c = ds.getConnection();

			int isolationLevel = c.getTransactionIsolation();
			return (isolationLevel == Connection.TRANSACTION_READ_COMMITTED);

		} catch (SQLException ex) {
			String m = "Errored trying to determine the default Isolation Level";
			throw new PersistenceException(m, ex);			

		} finally {
			try {
				if (c != null) {
					c.close();
				}
			} catch (SQLException ex) {
				logger.log(Level.SEVERE, "closing connection", ex);
			}
		}
	}
	
	public String getServerName() {
		return serverName;
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	/**
	 * Defines the type of behaviour to use when closing a transaction that was used to query data only.
	 */
	public OnQueryOnly getOnQueryOnly() {
		return onQueryOnly;
	}

	/**
	 * Return the TransactionLogger used by this TransactionManager.
	 */
	public TransactionLogManager getLogger() {
		return transLogger;
	}

	/**
	 * Log a message to the Transaction log.
	 */
	public void log(ServerTransaction t, String msg) {
		transLogger.log(t, msg, null);
	}

	/**
	 * Log an error to the transaction log.
	 */
	public void log(ServerTransaction t, String msg, Throwable error) {
		transLogger.log(t, msg, error);
	}

	/**
	 * Wrap the externally supplied Connection.
	 */
	public ServerTransaction wrapExternalConnection(Connection c) {

		return wrapExternalConnection(externalTransPrefix + c.hashCode(), c);
	}
	
	/**
	 * Wrap an externally supplied Connection with a known transaction id.
	 */
	public ServerTransaction wrapExternalConnection(String id, Connection c) {

		ExternalJdbcTransaction t = new ExternalJdbcTransaction(id, true, c, this);

		// set the default batch mode. This can be on for
		// jdbc drivers that support getGeneratedKeys
		if (defaultBatchMode){
			t.setBatchMode(true);
		}
				
		return t;
	}
	
	/**
	 * Create a new Transaction.
	 */
	public ServerTransaction createTransaction(boolean explicit, int isolationLevel) {
		try {
			long id;
			synchronized (monitor) {
				// Perhaps could use JDK5 atomic Integer instead
				id = ++transactionCounter;
			}
			Connection c = dataSource.getConnection();

			JdbcTransaction t = new JdbcTransaction(prefix + id, explicit, c, this);

			// set the default batch mode. This can be on for
			// jdbc drivers that support getGeneratedKeys
			if (defaultBatchMode){
				t.setBatchMode(true);
			}
			if (isolationLevel > -1) {
				c.setTransactionIsolation(isolationLevel);
			}

			if (debugLevel >= 2){
				String msg = "Transaction ["+t.getId()+"] begin";
				if (isolationLevel > -1){
					TxIsolation txi = TxIsolation.fromLevel(isolationLevel);
					msg += " isolationLevel["+txi+"]";
				}
				logger.info(msg);
			}
			
			return t;

		} catch (SQLException ex) {
			throw new PersistenceException(ex);
		}
	}
	
	public ServerTransaction createQueryTransaction() {
		try {
			long id;
			// Perhaps could use JDK5 atomic Integer instead
			// of this full synchronisation
			synchronized (monitor) {
				id = ++transactionCounter;
			}
			Connection c = dataSource.getConnection();

			JdbcTransaction t = new JdbcTransaction(prefix + id, false, c, this);
			
			// set the default batch mode. Can be true for
			// jdbc drivers that support getGeneratedKeys
			if (defaultBatchMode){
				t.setBatchMode(true);
			}

			if (debugLevel >= 2){
				logger.info("Transaction ["+t.getId()+"] begin - queryOnly");
			}
			
			return t;

		} catch (SQLException ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Process a local rolled back transaction.
	 */
	public void notifyOfRollback(ServerTransaction transaction, Throwable cause) {
		
		try {
			String msg = "Rollback";
			if (cause != null){
				msg += " error: "+formatThrowable(cause);
			}
			
			transLogger.transactionEnded(transaction, msg);
			if (debugLevel >= 1){
				logger.info("Transaction ["+transaction.getId()+"] "+msg);
			}
		} catch (Exception ex) {
			String m = "Potentially Transaction Log incomplete due to error:";
			logger.log(Level.SEVERE, m, ex);
		}
	}

	/**
	 * Query only transaction in read committed isolation.
	 */
	public void notifyOfQueryOnly(boolean onCommit, ServerTransaction transaction, Throwable cause) {
		
		try {
			// close the logger if one was created for this transaction
			String msg;
			if (onCommit){
				msg = "Commit queryOnly";
			
			} else {
				msg = "Rollback queryOnly";
				if (cause != null){
					msg += " error: "+formatThrowable(cause);
				}		
			}
			transLogger.transactionEnded(transaction, msg);	
			
			if (debugLevel >= 2){
				logger.info("Transaction ["+transaction.getId()+"] "+msg);
			}
		} catch (Exception ex) {
			String m = "Potentially Transaction Log incomplete due to error:";
			logger.log(Level.SEVERE, m, ex);
		}
	}
		
	private String formatThrowable(Throwable e){
		if (e == null){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		formatThrowable(e, sb);
		return sb.toString();
	}
	
	private void formatThrowable(Throwable e, StringBuilder sb){
		
		sb.append(e.toString());
		StackTraceElement[] stackTrace = e.getStackTrace();
		if (stackTrace.length > 0){
			sb.append(" stack0: ");
			sb.append(stackTrace[0]);
		}
		Throwable cause = e.getCause();
		if (cause != null){
			sb.append(" cause: ");
			formatThrowable(cause, sb);
		}
	}
	
	/**
	 * Process a local committed transaction.
	 */
	public void notifyOfCommit(ServerTransaction transaction) {

		try {
			// close the logger if required
			if (logAllCommits || transaction.isExplicit()) {
				transLogger.transactionEnded(transaction, "Commit");
	
			} else {
				transLogger.transactionEnded(transaction, null);
			}
	
			TransactionEvent event = transaction.getEvent();
			
			// notify cache with bean changes returning any table events
			TransactionEventTable tableEvents = event.notifyCache();
			processTableEvents(tableEvents);
			
	
			// cluster and text indexing
			localCommitBackgroundProcess(event);
			
			if (logCommitEvent || debugLevel >= 1){
				logger.info("Transaction ["+transaction.getId()+"] commit: "+event.toString());
			}
		} catch (Exception ex) {
			String m = "Potentially Transaction Log incomplete due to error:";
			logger.log(Level.SEVERE, m, ex);
		}
	}

	/**
	 * Process a Transaction that comes from another framework or local code.
	 * <p>
	 * For cases where raw SQL/JDBC or other frameworks are used this can
	 * invalidate the appropriate parts of the cache.
	 * </p>
	 */
	public void externalModification(TransactionEventTable tableEvents) {
		
		// invalidate parts of local cache 
		processTableEvents(tableEvents);
		
		TransactionEvent event = new TransactionEvent();
		event.add(tableEvents);
		
		// send to cluster
		localCommitBackgroundProcess(event);
	}
	
	/**
	 * Notify local BeanPersistListeners etc of events from another server in the cluster.
	 */
	public void remoteTransactionEvent(RemoteTransactionEvent remoteEvent) {

		List<RemoteBeanPersist> list = remoteEvent.getBeanPersistList();
		if (list != null){
			for (int i = 0; i < list.size(); i++) {
				remoteBeanPersist(list.get(i));
			}
		}
		
		processTableEvents(remoteEvent.getTableEvents());
	}
	
	/**
	 * Send a remote bean persist event to the local bean persist listeners.
	 */
	private void remoteBeanPersist(RemoteBeanPersist remoteBeanPersist) {

		BeanDescriptor<?> desc = beanDescriptorManager.getBeanDescriptor(remoteBeanPersist.getBeanType());
		if (desc == null){
			String msg = "Could not find BeanDescriptor for "+remoteBeanPersist.getBeanType();
			msg += "? Missing out remoteNotify of "+remoteBeanPersist;
			logger.severe(msg);
		} else {
			remoteBeanPersist.notifyListener(desc);			
		}
	}

	
	/**
	 * Run some of the post commit processing in a background thread. This can
	 * be relatively expensive/long running and includes notifying the cluster
	 * and BeanPersitListeners.
	 */
	private void localCommitBackgroundProcess(TransactionEvent event) {

		PostCommitProcessing postCommit = new PostCommitProcessing(clusterManager, this, event);
		threadPool.assign(postCommit, true);
	}

	/**
	 * Table events are where SQL or external tools are used. In this case
	 * the cache is notified based on the table name (rather than bean type).
	 */
	private void processTableEvents(TransactionEventTable tableEvents) {
		
		if (tableEvents != null && !tableEvents.isEmpty()){
			// notify cache with table based changes
			for (TableIUD tableIUD : tableEvents.values()) {
				beanDescriptorManager.cacheNotify(tableIUD);
			}
		}
	}
}
