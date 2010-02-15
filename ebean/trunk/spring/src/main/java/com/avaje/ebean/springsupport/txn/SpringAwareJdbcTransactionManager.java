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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.sql.DataSource;

import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    private final static Logger logger = Logger.getLogger(SpringAwareJdbcTransactionManager.class.getName());

    /** 
     * The data source. 
     */
    private DataSource dataSource;

    /** 
     * The Ebean transaction manager. 
     */
    private TransactionManager transactionManager;

    /**
     *  The EbeanServer name. 
     */
    private String serverName;

    /**
     * The Spring TransactionSynchronisation that is registered with Spring so
     * that Ebean is notified of commit and rollback events.
     */
    private SpringTxnListener springTxnListener;

    /**
     * Instantiates a new spring aware transaction scope manager.
     */
    public SpringAwareJdbcTransactionManager() {
    }

    /**
     * Initialise this with the Ebean internal transaction manager.
     */
    public void setTransactionManager(Object txnMgr) {
        
        // RB: At this stage not exposing TransactionManager to 
        // the public API and hence the Object type and casting here
        
        this.transactionManager = (TransactionManager) txnMgr;
        this.dataSource = transactionManager.getDataSource();
        this.serverName = transactionManager.getServerName();
        
        // This is a Spring TransactionSynchronization that is used to
        // get Spring to notify us when it has committed or rolled back a 
        // transaction. Ebean can then manage its cache and notify
        // BeanPersistListeners etc
        this.springTxnListener = createSpringTxnListener(serverName, transactionManager);
    }

    /**
     * Looks for a current Spring managed transaction and wraps/returns that as a Ebean transaction.
     * <p>
     * Returns null if there is no current spring transaction (lazy loading outside a spring txn etc).
     * </p>
     */
    public Object getCurrentTransaction() {

        // Register the springTxnListener if not added already
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && !TransactionSynchronizationManager.getSynchronizations().contains(springTxnListener)) {
            // this must be registered here (rather than on initialisation) as it gets registered 
            // into a Spring ThreadLocal mechanism (Spring does not have a global txn listener at this point)
            TransactionSynchronizationManager.registerSynchronization(springTxnListener);
        }

        // Get the current Spring ConnectionHolder associated to the current spring managed transaction
        ConnectionHolder holder = (ConnectionHolder) TransactionSynchronizationManager.getResource(dataSource);

        if (holder == null || !holder.isSynchronizedWithTransaction()) {
            // no current Spring transaction
            SpiTransaction currentEbeanTransaction = DefaultTransactionThreadLocal.get(serverName);
            if (currentEbeanTransaction != null){
                // NOT expecting this so log WARNING
                String msg = "SpringTransaction - no current spring txn BUT using current Ebean one "+currentEbeanTransaction.getId();
                logger.log(Level.WARNING, msg);
                
            } else if (logger.isLoggable(Level.FINEST)) {
                logger.log(Level.FINEST, "SpringTransaction - no current transaction ");
            }
            return currentEbeanTransaction;
        }

        // get the current Ebean transaction so that we can check this against the current Spring transaction
        SpringJdbcTransaction currentTrans = (SpringJdbcTransaction) DefaultTransactionThreadLocal.get(serverName);

        if (currentTrans != null) {
            ConnectionHolder currentHolder = currentTrans.getConnectionHolder();
            if (holder.equals(currentHolder)) {
                // already been using this transaction (already "wrapped" by Ebean)
                if (logger.isLoggable(Level.FINEST)) {
                    logger.log(Level.FINEST, "SpringTransaction - using current transaction " + currentTrans.getId());
                }
                return currentTrans;
            }
        }


        // "wrap" the underlying JDBC Connection from the Spring transaction
        // as an Ebean Transaction (which Ebean is NOT allowed to commit or rollback)
        SpringJdbcTransaction newTrans = new SpringJdbcTransaction(holder, transactionManager);
        
        // put this into Ebean's ThreadLocal and Ebean will use this (on this thread) 
        // until it has been committed or rolled back by Spring / external code
        DefaultTransactionThreadLocal.set(serverName, newTrans);
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "SpringTransaction starting new transaction " + newTrans.getId());
        }
        return newTrans;
    }

    /**
     * Create a listener to register with Spring to enable Ebean to be 
     * notified when transactions commit and rollback.
     * <p>
     * This is used by Ebean to notify it's appropriate listeners and maintain it's server
     * cache etc.
     * </p>
     */
    private SpringTxnListener createSpringTxnListener(String serverName, TransactionManager transactionManager) {
        return new SpringTxnListener(serverName, transactionManager);
    }

    /**
     * A Spring TransactionSynchronization that we register with Spring to get
     * notified when a Spring managed transaction has been committed or rolled
     * back.
     * <p>
     * When Ebean is notified (of the commit/rollback) it can then manage its
     * cache, notify BeanPersistListeners etc.
     * </p>
     */
    private static class SpringTxnListener extends TransactionSynchronizationAdapter {

        private final String serverName;
        
        private final TransactionManager transactionManager;
        
        private SpringTxnListener(String serverName, TransactionManager transactionManager){
            this.serverName  = serverName;
            this.transactionManager = transactionManager;
        }
        
        public void afterCompletion(int status) {
            
            SpringJdbcTransaction t = (SpringJdbcTransaction) DefaultTransactionThreadLocal.get(serverName);

            if (t == null) {
                // A spring transaction was created and committed / rolled back ... but Ebean was
                // never used with that transaction (perhaps used a Spring jdbc template etc)
                if (logger.isLoggable(Level.FINE)){
                    logger.fine("Ebean was not used with this Spring transaction; EbeanServer name:" + serverName);
                }
                
            } else {
                // Ebean was used with this transaction so it needs to
                // manage its cache and notify BeanPersistListeners etc
                switch (status) {
                case STATUS_COMMITTED:
                    logger.info("Txn committed");
                    transactionManager.notifyOfCommit(t);
                    break;
                    
                case STATUS_ROLLED_BACK:
                    logger.info("Txn rolled back");
                    transactionManager.notifyOfRollback(t, null);
                    break;
                    
                default:
                    // this should never happen
                    String msg = "Invalid status "+status;
                    throw new PersistenceException(msg);
                }
            } 
        }
    }
}
