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

import java.util.HashMap;

import javax.persistence.PersistenceException;

import com.avaje.ebean.internal.SpiTransaction;


/**
 * Current transactions mapped by server name. 
 */
public class TransactionMap {
    
    /**
     * Map of State by serverName. 
     */
    HashMap<String,State> map = new HashMap<String, State>();
    
    /**
     * Return the State for a given serverName.
     */
    public State getState(String serverName) {
        
        State state = (State)map.get(serverName);
        if (state == null){
        	state = new State();
            map.put(serverName, state);
        }
        return state;
    }
    
    /**
     * The transaction and whether it is active.
     */
    public static class State {

        SpiTransaction transaction;
        
        public SpiTransaction get() {
            return transaction;
        }
        
        /**
         * Set the transaction. This will now be the current transaction.
         */
        public void set(SpiTransaction trans) {
            
        	if (transaction != null && transaction.isActive()){
        		String m = "The existing transaction is still active?";
                throw new PersistenceException(m);
            }
            transaction = trans;
        }


        /**
         * Commit the transaction.
         */
        public void commit() {
        	transaction.commit();
        	transaction = null;
        }

        /**
         * Rollback the transaction.
         */
        public void rollback() {
        	transaction.rollback();
        	transaction = null;
        }
        
        /**
         * End the transaction.
         */
        public void end() {
        	if (transaction != null){
        		transaction.end();
            	transaction = null;
        	}
        }
        
        /**
         * Used to replace transaction with a proxy.
         */
        public void replace(SpiTransaction trans) {
            transaction = trans;
        }

    }
}
