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
package com.avaje.ebean.spring;


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
public class NotUsedSpringAwareTransactionScope {//extends TransactionScopeManager {

//	private final static Logger logger = Logger.getLogger(SpringAwareTransactionScope.class.getName());
//
//	/** The data source. */
//	final DataSource dataSource;
//
//	/**
//	 * Instantiates a new spring aware transaction scope manager.
//	 *
//	 * @param transactionManager the transaction manager
//	 */
//	public SpringAwareTransactionScope(TransactionManager transactionManager) {
//		super(transactionManager);
//		this.dataSource = transactionManager.getDataSource();
//	}
//
//	public void commit() {
//		DefaultTransactionThreadLocal.commit(serverName);
//	}
//
//
//	public void end() {
//		DefaultTransactionThreadLocal.end(serverName);
//	}
//
//	public ServerTransaction get() {
//		
//		ConnectionHolder holder = (ConnectionHolder)TransactionSynchronizationManager.getResource(dataSource);
//		if (holder == null || !holder.isSynchronizedWithTransaction()){
//			// no current Spring transaction
//			DefaultTransactionThreadLocal.replace(serverName, null);
//			logger.info("SpringTransaction - no current transaction ");
//			return null;
//		}
//		
//		ConnectionHolder currentHolder = null;
//		SpringJdbcTransaction currentTrans = (SpringJdbcTransaction)DefaultTransactionThreadLocal.get(serverName);
//		if (currentTrans != null){
//			currentHolder = currentTrans.getConnectionHolder();
//		}
//		
//		if (holder.equals(currentHolder)){
//			logger.info("SpringTransaction - using current transaction "+currentTrans.getId());
//			return currentTrans;
//		}
//		
//		SpringJdbcTransaction newTrans = new SpringJdbcTransaction(holder, transactionManager);
//		DefaultTransactionThreadLocal.set(serverName, newTrans);
//		logger.info("SpringTransaction starting new transaction "+newTrans.getId());
//		
//		return newTrans;
//	}
//
//	public void replace(ServerTransaction trans) {
//		DefaultTransactionThreadLocal.replace(serverName, trans);
//	}
//
//	public void rollback() {
//		DefaultTransactionThreadLocal.rollback(serverName);
//	}
//
//	public void set(ServerTransaction trans) {
//		DefaultTransactionThreadLocal.set(serverName, trans);
//	}
}
