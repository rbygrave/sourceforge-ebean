package com.avaje.ebean.config;

import com.avaje.ebean.server.transaction.TransactionManager;

/**
 * Provides awareness of externally managed transactions.
 */
public interface ExternalTransactionManager {

	/**
	 * Set the transaction manager.
	 */
	public void setTransactionManager(TransactionManager transactionManager);

	/**
	 * Return the current transaction or null if there is none.
	 */
	public Object getCurrentTransaction();
	
}
