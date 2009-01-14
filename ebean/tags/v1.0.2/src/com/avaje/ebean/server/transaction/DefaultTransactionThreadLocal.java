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

import com.avaje.ebean.server.core.ServerTransaction;

/**
 * Used by EbeanMgr to store its Transactions in a ThreadLocal.
 * This way the transaction objects don't have to passed around.
 */
public final class DefaultTransactionThreadLocal {


    private static ThreadLocal<TransactionMap> local = new ThreadLocal<TransactionMap>() {
        protected synchronized TransactionMap initialValue() {
            return new TransactionMap();
        }
    };
    
    /**
     * Not allowed.
     */
    private DefaultTransactionThreadLocal() {
    }
    
    /**
     * Return the current TransactionState for a given serverName.
     * This is for the local thread of course.
     */
    private static TransactionMap.State getState(String serverName){
        return local.get().getState(serverName);
    }
    
    /**
     * Return the current Transaction for this serverName and Thread.
     */
    public static ServerTransaction get(String serverName) {
        return getState(serverName).transaction;
    }

    /**
     * Set a new Transaction for this serverName and Thread.
     */
    public static void set(String serverName, ServerTransaction trans) {
        getState(serverName).set(trans);
    }
    
    /**
     * Commit the current transaction.
     */
    public static void commit(String serverName) {
        getState(serverName).commit();
    }

    /**
     * Rollback the current transaction.
     */
    public static void rollback(String serverName) {
        getState(serverName).rollback();
    }

    /**
     * If the transaction has not been committed then roll it back.
     * <p>
     * Designed to be put in a finally block instead of a rollback()
     * in each catch block.
     * <pre><code>
     * EbeanMgr.startTransaction();
     * try {
     *     //... perform some actions in a single transaction
     *     
     *     EbeanMgr.commitTransaction();
     *     
     * } finally {
     *     // ensure transaction ended.  If some error occurred then rollback() 
     *     EbeanMgr.endTransaction();
     * }
     * </code></pre>
     * </p>
     */
    public static void end(String serverName) {
        getState(serverName).end();
    }
    
    /**
     * A mechanism to get the transaction out of the thread local by 
     * replacing it with a 'proxy'.
     * <p>
     * Used for background fetching. Replaces the current transaction with a
     * 'dummy' transaction. The current transaction is given to the background
     * thread so it can continue the fetch.
     * </p>
     */
    public static void replace(String serverName, ServerTransaction trans) {
        getState(serverName).replace(trans);
    }
}
