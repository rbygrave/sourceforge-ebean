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
package org.avaje.ebean.server.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.avaje.ebean.TxIsolation;
import org.avaje.ebean.net.Constants;
import org.avaje.ebean.server.core.ServerTransaction;
import org.avaje.ebean.server.deploy.BeanDescriptorOwner;
import org.avaje.ebean.server.deploy.DeploymentManager;
import org.avaje.ebean.server.lib.cluster.ClusterManager;
import org.avaje.ebean.server.lib.thread.ThreadPool;
import org.avaje.ebean.server.lib.thread.ThreadPoolManager;
import org.avaje.ebean.server.net.CmdRemoteListenerEvent;
import org.avaje.ebean.server.net.CmdServerTransactionEvent;
import org.avaje.ebean.server.net.Headers;
import org.avaje.ebean.server.plugin.PluginCore;
import org.avaje.ebean.server.plugin.PluginProperties;
import org.avaje.lib.log.LogFactory;

/**
 * Manages transactions.
 * <p>
 * Keeps the Cache, Cluster and Lucene indexes in synch when transactions are
 * committed.
 * </p>
 */
public class TransactionManager implements Constants {

	private static final Logger logger = LogFactory.get(TransactionManager.class);
	
	/**
	 * Holds Table state.
	 */
	private final TableStateManager tableState = new TableStateManager();

	/**
	 * The logger.
	 */
	private final TransactionLogManager transLogger;

	/**
	 * Id's for transaction logging.
	 */
	private long transactionCounter = 1000;

	/**
	 * for synchronising.
	 */
	private final Object monitor = new Object();

	/**
	 * Prefix for transaction id's (logging).
	 */
	String prefix;

	/**
	 * The dataSource of connections.
	 */
	DataSource dataSource;

	boolean logAllCommits;

	/**
	 * Flag to indicate the default Isolation is READ COMMITTED. This enables us
	 * to close queryOnly transactions rather than commit or rollback them.
	 */
	boolean readCommittedIsolation;

	/**
	 * The default batchMode for transactions.
	 */
	boolean defaultBatchMode;

	/**
	 * Background threading of post commit processing.
	 */
	ThreadPool threadPool;

	/**
	 * Helper object to perform BeanListener notification.
	 */
	ListenerNotify listenerNotify;

	PluginProperties properties;
	
	DeploymentManager deploy;
	
	int debugLevel;
	
	private final boolean logCommitEvent;
	
	final ClusterManager clusterManager;
	
	/**
	 * Create the TransactionManager
	 */
	public TransactionManager(PluginCore pluginCore) {
		this.clusterManager = pluginCore.getClusterManager();
		this.properties = pluginCore.getDbConfig().getProperties();
		this.deploy = pluginCore.getDeploymentManager();
		
		this.transLogger = new TransactionLogManager(properties);
		this.threadPool = ThreadPoolManager.getThreadPool("TransactionManager");
		if (threadPool.getMinSize() == 0) {
			threadPool.setMinSize(1);
		}
		
		this.listenerNotify = new ListenerNotify(this, pluginCore);
		this.dataSource = pluginCore.getDbConfig().getDataSource();
		
		logCommitEvent = properties.getPropertyBoolean("log.commit", false);
		debugLevel = properties.getPropertyInt("debug.transaction", 0);
			
		this.defaultBatchMode = properties.getPropertyBoolean("batch.mode", false);
		this.prefix = properties.getProperty("transaction.prefix", "");
		String logAllCom = properties.getProperty("transaction.logallcommits", "false");
		this.logAllCommits = (logAllCom != null && logAllCom.equalsIgnoreCase("true"));

		determineIsolation();
	}
	
	public BeanDescriptorOwner getDeploy() {
		return deploy;
	}
	
	public TableStateManager getTableStateManager() {
		return tableState;
	}

	/**
	 * Check to see if read committed is the default isolation level.
	 * <p>
	 * If it is, then Connections used only for queries do not require commit or
	 * rollback but instead can just be put back into the pool via close().
	 * </p>
	 * <p>
	 * If the Isolation level is higher (say SERIALIZABLE) then Connections used
	 * just for queries do need to be committed or rollback after the query.
	 * </p>
	 * <p>
	 * Note that The DefaultServer now checks the DataSource for its autoCommit
	 * and transaction isolation levels and logs warnings if appropriate.
	 * </p>
	 */
	private void determineIsolation() {
		Connection c = null;
		try {
			c = dataSource.getConnection();

			int isolationLevel = c.getTransactionIsolation();
			readCommittedIsolation = (isolationLevel == Connection.TRANSACTION_READ_COMMITTED);

		} catch (SQLException ex) {
			String m = "Errored trying to determine the default Isolation Level";
			
			logger.log(Level.SEVERE, m, ex);
			

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

	/**
	 * Flag to indicate the default Isolation is READ COMMITTED.
	 * <p>
	 * This is used to get a performance improvement on query only transactions.
	 * Perhaps unnecessary depending on the JDBC driver.
	 * </p>
	 * <p>
	 * At read committed isolation level connections only used for queries can
	 * be closed without requiring a commit or rollback.
	 * </p>
	 */
	public boolean isReadCommittedIsolation() {
		return readCommittedIsolation;
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
	 * Create a new Transaction.
	 */
	public ServerTransaction createTransaction(boolean explicit, int isolationLevel) {
		try {
			long id = 0;
			// Perhaps could use JDK5 atomic Integer instead
			// of this full synchronisation
			synchronized (monitor) {
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
	public void notifyOfRollback(ServerTransaction transaction, Throwable e) {
		
		String msg = "Rollback";
		if (e != null){
			msg += " error: "+formatThrowable(e);
		}
		
		transLogger.transactionEnded(transaction, msg);
		if (debugLevel >= 1){
			logger.info("Transaction ["+transaction.getId()+"] "+msg);
		}
	}

	/**
	 * Query only transaction in read committed isolation.
	 */
	public void notifyOfQueryOnly(boolean onCommit, ServerTransaction transaction, Throwable e) {
		
		// close the logger if one was created for this transaction
		String msg;
		if (onCommit){
			msg = "Commit queryOnly";
		
		} else {
			msg = "Rollback queryOnly";
			if (e != null){
				msg += " error: "+formatThrowable(e);
			}		
		}
		transLogger.transactionEnded(transaction, msg);	
		
		if (debugLevel >= 3){
			logger.info("Transaction ["+transaction.getId()+"] "+msg);
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
