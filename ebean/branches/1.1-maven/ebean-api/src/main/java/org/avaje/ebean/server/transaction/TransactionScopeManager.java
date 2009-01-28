package org.avaje.ebean.server.transaction;

import org.avaje.ebean.server.core.ServerTransaction;

/**
 * Manages the Transactions typically held in a ThreadLocal.
 */
public interface TransactionScopeManager {

	 /**
     * Return the current Transaction for this serverName and Thread.
     */
    public ServerTransaction get();

    /**
     * Set a new Transaction for this serverName and Thread.
     */
    public void set(ServerTransaction trans);
    
    /**
     * Commit the current transaction.
     */
    public void commit();

    /**
     * Rollback the current transaction.
     */
    public void rollback();


    /**
     * Rollback if required.
     */
    public void end();

    /**
     * Replace the current transaction with this one.
     * <p>
     * Used for Background fetching and Nested transaction scopes.
     * </p>
     * <p>
     * Used for background fetching. Replaces the current transaction with a
     * 'dummy' transaction. The current transaction is given to the background
     * thread so it can continue the fetch.
     * </p>
     */
    public void replace(ServerTransaction trans);
}
