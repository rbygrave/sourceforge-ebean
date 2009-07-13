package com.avaje.ebean.server.transaction;

import com.avaje.ebean.internal.ServerTransaction;

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

	public ServerTransaction get() {
		return DefaultTransactionThreadLocal.get(serverName);
	}

	public void replace(ServerTransaction trans) {
		DefaultTransactionThreadLocal.replace(serverName, trans);
	}

	public void rollback() {
		DefaultTransactionThreadLocal.rollback(serverName);
	}

	public void set(ServerTransaction trans) {
		DefaultTransactionThreadLocal.set(serverName, trans);
	}

	
}
