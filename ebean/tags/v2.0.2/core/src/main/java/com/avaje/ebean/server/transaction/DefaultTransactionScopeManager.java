package com.avaje.ebean.server.transaction;

import com.avaje.ebean.internal.SpiTransaction;

/**
 * Manages the transaction scoping using a Ebean thread local.
 */
public class DefaultTransactionScopeManager extends TransactionScopeManager {

	
	public DefaultTransactionScopeManager(TransactionManager transactionManager){
		super(transactionManager);
	}

	public void commit() {
		DefaultTransactionThreadLocal.commit(serverName);
	}

	public void end() {
		DefaultTransactionThreadLocal.end(serverName);
	}

	public SpiTransaction get() {
		return DefaultTransactionThreadLocal.get(serverName);
	}

	public void replace(SpiTransaction trans) {
		DefaultTransactionThreadLocal.replace(serverName, trans);
	}

	public void rollback() {
		DefaultTransactionThreadLocal.rollback(serverName);
	}

	public void set(SpiTransaction trans) {
		DefaultTransactionThreadLocal.set(serverName, trans);
	}

	
}
