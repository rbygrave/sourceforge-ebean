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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.LogLevel;
import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.api.TransactionEventTable.TableIUD;
import com.avaje.ebeaninternal.server.cluster.ClusterManager;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;

/**
 * Manages transactions.
 * <p>
 * Keeps the Cache, Cluster and Lucene indexes in synch when transactions are
 * committed.
 * </p>
 */
public class TransactionManager {

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
	
	private final LuceneIndexManager luceneIndexManager;
    
	private final BeanDescriptorManager beanDescriptorManager;
	
	private LogLevel logLevel;
	
	/**
	 * The logger.
	 */
	private final TransactionLogManager transLogger;
	
	/**
	 * Prefix for transaction id's (logging).
	 */
	private final String prefix;

	private final String externalTransPrefix;

	/**
	 * The dataSource of connections.
	 */
	private final DataSource dataSource;

	/**
	 * Flag to indicate the default Isolation is READ COMMITTED. This enables us
	 * to close queryOnly transactions rather than commit or rollback them.
	 */
	private final OnQueryOnly onQueryOnly;

	/**
	 * The default batchMode for transactions.
	 */
	private final boolean defaultBatchMode;

	private final BackgroundExecutor backgroundExecutor;
			
	private final ClusterManager clusterManager;
	
	private final int commitDebugLevel;

	private final String serverName;
	
	/**
	 * Id's for transaction logging.
	 */
	private AtomicLong transactionCounter = new AtomicLong(1000);
	
	private int clusterDebugLevel;
	
	/**
	 * Create the TransactionManager
	 */
	public TransactionManager(ClusterManager clusterManager, LuceneIndexManager luceneIndexManager, 
	        BackgroundExecutor backgroundExecutor, ServerConfig config, BeanDescriptorManager descMgr) {
		
		this.beanDescriptorManager = descMgr;
		this.clusterManager = clusterManager;
		this.luceneIndexManager = luceneIndexManager;
		this.serverName = config.getName();
		
		this.logLevel = config.getLoggingLevel();
		this.transLogger = new TransactionLogManager(config);
		this.backgroundExecutor = backgroundExecutor;		
		this.dataSource = config.getDataSource();
		
		// log some transaction events using a java util logger          
        this.commitDebugLevel = GlobalProperties.getInt("ebean.commit.debuglevel", 0);
		this.clusterDebugLevel = GlobalProperties.getInt("ebean.cluster.debuglevel", 0);
		
		this.defaultBatchMode = config.isPersistBatching();
		
		this.prefix = GlobalProperties.get("transaction.prefix", "");
		this.externalTransPrefix = GlobalProperties.get("transaction.prefix", "e");
		
		String value = GlobalProperties.get("transaction.onqueryonly", "ROLLBACK").toUpperCase().trim();
		this.onQueryOnly = getOnQueryOnly(value, dataSource);
	}
	
	public void shutdown() {
	    transLogger.shutdown();
	}
	
	public BeanDescriptorManager getBeanDescriptorManager() {
        return beanDescriptorManager;
    }

    /**
	 * Return the logging level for transactions.
	 */
	public LogLevel getTransactionLogLevel(){
		return logLevel;
	}
	
	/**
	 * Set the log level for transactions.
	 */
	public void setTransactionLogLevel(LogLevel logLevel){
		this.logLevel = logLevel;
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
	 * Return the cluster debug level.
	 */
	public int getClusterDebugLevel() {
        return clusterDebugLevel;
    }

    /**
     * Set the cluster debug level. 
     */
    public void setClusterDebugLevel(int clusterDebugLevel) {
        this.clusterDebugLevel = clusterDebugLevel;
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

	public void log(TransactionLogBuffer logBuffer){
	    if (!logBuffer.isEmpty()){
	        transLogger.log(logBuffer);
	    }
	}

	/**
	 * Wrap the externally supplied Connection.
	 */
	public SpiTransaction wrapExternalConnection(Connection c) {

		return wrapExternalConnection(externalTransPrefix + c.hashCode(), c);
	}
	
	/**
	 * Wrap an externally supplied Connection with a known transaction id.
	 */
	public SpiTransaction wrapExternalConnection(String id, Connection c) {

		ExternalJdbcTransaction t = new ExternalJdbcTransaction(id, true, logLevel, c, this);

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
	public SpiTransaction createTransaction(boolean explicit, int isolationLevel) {
		try {
			long id = transactionCounter.incrementAndGet();
			
			Connection c = dataSource.getConnection();

			JdbcTransaction t = new JdbcTransaction(prefix + id, explicit, logLevel, c, this);

			// set the default batch mode. This can be on for
			// jdbc drivers that support getGeneratedKeys
			if (defaultBatchMode){
				t.setBatchMode(true);
			}
			if (isolationLevel > -1) {
				c.setTransactionIsolation(isolationLevel);
			}

			if (commitDebugLevel >= 3){
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
	
	public SpiTransaction createQueryTransaction() {
		try {
			long id = transactionCounter.incrementAndGet();
			Connection c = dataSource.getConnection();

			JdbcTransaction t = new JdbcTransaction(prefix + id, false, logLevel, c, this);
			
			// set the default batch mode. Can be true for
			// jdbc drivers that support getGeneratedKeys
			if (defaultBatchMode){
				t.setBatchMode(true);
			}

			if (commitDebugLevel >= 3){
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
	public void notifyOfRollback(SpiTransaction transaction, Throwable cause) {
		
		try {
			String msg = "Rollback";
			if (cause != null){
				msg += " error: "+formatThrowable(cause);
			}
			transaction.log(msg);
			
			if (commitDebugLevel >= 1){
				logger.info("Transaction ["+transaction.getId()+"] "+msg);
			}
			
			log(transaction.getLogBuffer());
			
		} catch (Exception ex) {
			String m = "Potentially Transaction Log incomplete due to error:";
			logger.log(Level.SEVERE, m, ex);
		}
	}

	/**
	 * Query only transaction in read committed isolation.
	 */
	public void notifyOfQueryOnly(boolean onCommit, SpiTransaction transaction, Throwable cause) {
		
		try {
            if (commitDebugLevel >= 2){
    			String msg;
    			if (onCommit){
    				msg = "Commit queryOnly";
    			
    			} else {
    				msg = "Rollback queryOnly";
    				if (cause != null){
    					msg += " error: "+formatThrowable(cause);
    				}		
    			}
    			transaction.log(msg);
                logger.info("Transaction ["+transaction.getId()+"] "+msg);
            }
            
			log(transaction.getLogBuffer());
			
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
	public void notifyOfCommit(SpiTransaction transaction) {

		try {
		    
		    log(transaction.getLogBuffer());
	
		    PostCommitProcessing postCommit = new PostCommitProcessing(clusterManager, luceneIndexManager, this, transaction, transaction.getEvent());
	    
		    postCommit.notifyLocalCacheIndex();
		    postCommit.notifyCluster();
			
			// cluster and text indexing
	        backgroundExecutor.execute(postCommit.notifyPersistListeners());
			
			if (commitDebugLevel >= 1){
				logger.info("Transaction ["+transaction.getId()+"] commit");
			}
		} catch (Exception ex) {
			String m = "NotifyOfCommit failed. Cache/Lucene potentially not notified.";
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
				
		TransactionEvent event = new TransactionEvent();
		event.add(tableEvents);
		
		PostCommitProcessing postCommit = new PostCommitProcessing(clusterManager, luceneIndexManager, this, null, event);
        
		// invalidate parts of local cache and index
		postCommit.notifyLocalCacheIndex();
		
		backgroundExecutor.execute(postCommit.notifyPersistListeners());
	}
	
	
    /**
     * Notify local BeanPersistListeners etc of events from another server in the cluster.
     */
	public void remoteTransactionEvent(RemoteTransactionEvent remoteEvent) {
        
        if (clusterDebugLevel > 0 || logger.isLoggable(Level.FINE)){
            logger.info("Cluster Received: "+remoteEvent.toString());
        }

        luceneIndexManager.processEvent(remoteEvent, null);
        
        List<TableIUD> tableIUDList = remoteEvent.getTableIUDList();
        if (tableIUDList != null){
            for (int i = 0; i < tableIUDList.size(); i++) {
                TableIUD tableIUD = tableIUDList.get(i);
                beanDescriptorManager.cacheNotify(tableIUD);
            }
        }
        
        List<BeanPersistIds> beanPersistList = remoteEvent.getBeanPersistList();
        if (beanPersistList != null){
            for (int i = 0; i < beanPersistList.size(); i++) {
                BeanPersistIds beanPersist = beanPersistList.get(i);
                beanPersist.notifyCacheAndListener();
            }
        }
        
        List<IndexEvent> indexEventList = remoteEvent.getIndexEventList();
        if (indexEventList != null){
            for (int i = 0; i < indexEventList.size(); i++) {
                IndexEvent indexEvent = indexEventList.get(i);
                luceneIndexManager.processEvent(indexEvent);
            }
        }
        
    }
	

}
