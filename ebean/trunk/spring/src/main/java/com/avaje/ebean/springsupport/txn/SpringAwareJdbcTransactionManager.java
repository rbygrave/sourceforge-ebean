/**
 * Copyright (C) 2009 the original author or authors
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
package com.avaje.ebean.springsupport.txn;


import javax.sql.DataSource;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.config.ExternalTransactionManager;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.server.transaction.DefaultTransactionThreadLocal;
import com.avaje.ebean.server.transaction.TransactionManager;


/**
 * A Spring aware TransactionScopeManager.
 * 
 * <p>
 * Will look for Spring transactions and use them if they exist.
 * </p>
 * 
 * @since 18.05.2009
 * @author E Mc Greal
 */
public class SpringAwareJdbcTransactionManager implements ExternalTransactionManager {

	/** The Constant logger. */
	private final static Logger logger = Logger.getLogger(SpringAwareJdbcTransactionManager.class.getName());

	/** The data source. */
	private DataSource dataSource;

	/** The transaction manager. */
	private TransactionManager transactionManager;

	/** The server name. */
	private String serverName;

	/** The transaction synchronization. */
	private TransactionSynchronization transactionSynchronization;
	
	/**
	 * Instantiates a new spring aware transaction scope manager.
	 */
	public SpringAwareJdbcTransactionManager() {
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.ExternalTransactionManager#setTransactionManager(java.lang.Object)
	 */
	public void setTransactionManager(Object txnMgr) {
		this.transactionManager = (TransactionManager)txnMgr;
		this.dataSource = transactionManager.getDataSource();
		this.serverName = transactionManager.getServerName();
		this.transactionSynchronization = getTransactionSynchronization();
	}


	/* (non-Javadoc)
	 * @see com.avaje.ebean.config.ExternalTransactionManager#getCurrentTransaction()
	 */
	public Object getCurrentTransaction() {
		
		ConnectionHolder holder = (ConnectionHolder)TransactionSynchronizationManager.getResource(dataSource);
		
		// Add the txn sync listener if not added already
		if (TransactionSynchronizationManager.isSynchronizationActive() 
			&& !TransactionSynchronizationManager.getSynchronizations().contains(transactionSynchronization)){
			TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
		}

		if (holder == null || !holder.isSynchronizedWithTransaction()){
			// no current Spring transaction
			DefaultTransactionThreadLocal.replace(serverName, null);
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "SpringTransaction - no current transaction ");
			}
			return null;
		}

		ConnectionHolder currentHolder = null;
		SpringJdbcTransaction currentTrans = (SpringJdbcTransaction)DefaultTransactionThreadLocal.get(serverName);
		if (currentTrans != null){
			currentHolder = currentTrans.getConnectionHolder();
		}

		if (holder.equals(currentHolder)){
			if (logger.isLoggable(Level.FINEST)) {
				logger.log(Level.FINEST, "SpringTransaction - using current transaction "+currentTrans.getId());
			}
			return currentTrans;
		}

		SpringJdbcTransaction newTrans = new SpringJdbcTransaction(holder, transactionManager);
		DefaultTransactionThreadLocal.set(serverName, newTrans);
		if (logger.isLoggable(Level.FINEST)){
			logger.log(Level.FINEST, "SpringTransaction starting new transaction "+newTrans.getId());
		}
		return newTrans;
	}

	/**
	 * Gets the transaction synchronization.
	 * 
	 * @return the transaction synchronization
	 */
	private TransactionSynchronization getTransactionSynchronization(){
		return new TransactionSynchronization(){

			public void afterCommit() {
			}

			public void afterCompletion(int status) {
				final SpringJdbcTransaction t = (SpringJdbcTransaction) DefaultTransactionThreadLocal.get(serverName);
				
				if (t != null){
					switch(status){
						case STATUS_COMMITTED:
							logger.info("Txn committed");
							t.getTransactionManger().notifyOfCommit(t);
							break;
						case STATUS_ROLLED_BACK:
							logger.info("Txn rolled back");
							t.getTransactionManger().notifyOfRollback(t, null);
							break;
					}
				}else{
					logger.fine("Failed to find Ebean transaction for server name:" + serverName);
				}
				
			}

			public void beforeCommit(boolean arg0) {
			}

			public void beforeCompletion() {
			}

			public void flush() {
			}

			public void resume() {
			}

			public void suspend() {
			}};
	}
}
