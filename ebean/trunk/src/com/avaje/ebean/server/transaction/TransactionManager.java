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
import javax.sql.DataSource;

import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.net.Constants;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.lib.cluster.ClusterManager;
import com.avaje.ebean.server.lib.thread.ThreadPool;
import com.avaje.ebean.server.lib.thread.ThreadPoolManager;
import com.avaje.ebean.server.net.CmdRemoteListenerEvent;
import com.avaje.ebean.server.net.CmdServerTransactionEvent;
import com.avaje.ebean.server.net.Headers;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.server.plugin.PluginProperties;

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
	
	/**
	 * Holds Table state.
	 */
	private final TableStateManager tableState = new TableStateManager();

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

	/**
	 * Helper object to perform BeanListener notification.
	 */
	private final ListenerNotify listenerNotify;

	private final PluginProperties properties;
			
	private final boolean logCommitEvent;
	
	private final ClusterManager clusterManager;
	
	private final int debugLevel;

	/**
	 * Id's for transaction logging.
	 */
	private long transactionCounter = 1000;
	
	/**
	 * Create the TransactionManager
	 */
	public TransactionManager(PluginCore pluginCore) {
		this.clusterManager = pluginCore.getClusterManager();
		this.properties = pluginCore.getDbConfig().getProperties();
		
		this.transLogger = new TransactionLogManager(properties);
		this.threadPool = ThreadPoolManager.getThreadPool("TransactionManager");
		if (threadPool.getMinSize() == 0) {
			threadPool.setMinSize(1);
		}
		
		this.listenerNotify = new ListenerNotify(this, pluginCore);
		this.dataSource = pluginCore.getDbConfig().getDataSource();
		
		this.logCommitEvent = properties.getPropertyBoolean("log.commit", false);
		this.debugLevel = properties.getPropertyInt("debug.transaction", 0);	
		this.defaultBatchMode = properties.getPropertyBoolean("batch.mode", false);
		
		this.prefix = properties.getProperty("transaction.prefix", "");
		this.logAllCommits = properties.getPropertyBoolean("transaction.logallcommits", false);
		this.onQueryOnly = getOnQueryOnly(properties, dataSource);
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
	private OnQueryOnly getOnQueryOnly(PluginProperties props, DataSource ds) {
		
		String value = props.getProperty("transaction.onqueryonly", "ROLLBACK");
		value = value.toUpperCase().trim();
		
		if (value.equals("COMMIT")){
			return OnQueryOnly.COMMIT;
		}
		if (value.startsWith("CLOSE")){
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
		return properties.getServerName();
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	public TableStateManager getTableStateManager() {
		return tableState;
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

		long id;
		synchronized (monitor) {
			// Perhaps could use JDK5 atomic Integer instead
			id = ++transactionCounter;
		}

		return wrapExternalConnection(prefix + id, c);
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

			if (debugLevel >= 3){
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
			
			// set the default batch mode. This can be on for
			// jdbc drivers that support getGeneratedKeys
			if (defaultBatchMode){
				t.setBatchMode(true);
			}

			if (debugLevel >= 9){
				logger.info("Transaction ["+t.getId()+"] begin - queryOnly");
			}
			
			return t;

		} catch (SQLException ex) {
			throw new PersistenceException(ex);
		}
	}

	/**
	 * Return the state of a given table. TableState is used to simplify
	 * invalidating cached objects.
	 */
	public TableState getTableState(String tableName) {
		return tableState.getTableState(tableName);
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
			
			if (debugLevel >= 3){
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
			if (!event.hasModifications()) {
				// ignore as it has no modifications
				if (debugLevel >= 3){
					logger.info("Transaction ["+transaction.getId()+"] commit with no changes");
				}
				return;
			}
	
			// maintain table state information for cache invalidation
			tableState.process(event);
	
			// cluster and Lucene indexing
			postProcess(event);
			
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
	 * Developers should use this method to inform the framework of commit
	 * events. The framework can then manage the cache, lucene indexes and
	 * inform other servers in the cluster.
	 * </p>
	 * <p>
	 * This method is also called when a remote cluster commit occurs.
	 * </p>
	 */
	public void externalModification(TransactionEvent event) {
		if (event.isInvalidateAll()) {
			// bypass normal table state modification
			tableState.setAllTablesModifiedNow();
		} else {
			tableState.process(event);
		}
		postProcess(event);
	}

	/**
	 * Another server in the cluster sent this event so that we can inform local
	 * BeanListeners of inserts updates and deletes that occurred remotely (on
	 * another server in the cluster).
	 */
	public void remoteListenerEvent(RemoteListenerEvent event) {
		listenerNotify.remoteNotify(event);
	}

	/**
	 * Run Post commit Processing. Clustering, Lucene and BeanListener
	 * notification.
	 */
	private void postProcess(TransactionEvent event) {

		PostCommitNotify postCommit = new PostCommitNotify(this, event);
		threadPool.assign(postCommit, true);
	}

//	/**
//	 * Notify LuceneManager of the changes so that it can update its indexes
//	 * appropriately.
//	 */
//	protected void notifyLucene(TransactionEvent event) {
//		if (!event.isInvalidateAll()) {
//			if (finder != null){
//				finder.notifyCommit(event);
//			}
//		}
//	}

	/**
	 * Send TransactionEvent information across the cluster to maintain cache
	 * and lucene indexes appropriately. Aka invalidate cached elements and
	 * notify Lucene of the changes.
	 */
	protected void notifyCluster(TransactionEvent event) {

		if (event.isLocal() && clusterManager.isClusteringOn()) {
			Headers h = new Headers();
			h.setProcesorId(PROCESS_KEY);
			h.set(SERVER_NAME_KEY, properties.getServerName());

			CmdServerTransactionEvent cmd = new CmdServerTransactionEvent(event);
			clusterManager.broadcast(h, cmd);
		}
	}

	/**
	 * notify BeanListeners of local transaction event.
	 */
	protected void notifyBeanListeners(TransactionEvent event) {

		TransactionEventBeans eventBeans = event.getEventBeans();
		if (eventBeans != null) {
			listenerNotify.localNotify(eventBeans);
		}
	}

	/**
	 * Send RemoteListenerEvent to all the servers in the cluster.
	 * <p>
	 * This is used to notify BeanListeners on the other servers of the insert
	 * update and delete events.
	 * </p>
	 */
	protected void notifyCluster(RemoteListenerEvent event) {

		if (clusterManager.isClusteringOn()) {
			Headers h = new Headers();
			h.setProcesorId(PROCESS_KEY);
			h.set(SERVER_NAME_KEY, properties.getServerName());

			CmdRemoteListenerEvent cmd = new CmdRemoteListenerEvent(event);

			clusterManager.broadcast(h, cmd);
		}
	}

}
