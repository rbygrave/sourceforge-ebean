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
package com.avaje.ebean.server.core;

import java.sql.SQLException;

import javax.persistence.OptimisticLockException;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.persist.BatchControl;
import com.avaje.ebean.server.persist.PersistExecute;
import com.avaje.ebean.server.transaction.TransactionEvent;
import com.avaje.ebean.util.Message;

/**
 * PersistRequest for insert update or delete of a bean.
 */
public final class PersistRequestBean extends PersistRequest {
	
	/**
	 * The unique id used for logging summary.
	 */
	Object idValue;
	
    public PersistRequestBean(InternalEbeanServer server, Object bean, Object parentBean, BeanManager mgr, ServerTransaction t, PersistExecute persistExecute) {
       super(server, bean, parentBean, mgr, t, persistExecute);
    }

    
    
	@Override
	public int executeNow() {
		switch (type) {
		case INSERT:
			persistExecute.executeInsertBean(this);
			return -1;
			
		case UPDATE:
			persistExecute.executeUpdateBean(this);
			return -1;
			
		case DELETE:
			persistExecute.executeDeleteBean(this);
			return -1;
			
		default:
			throw new RuntimeException("Invalid type " + type);
		}
	}

	@Override
	public int executeOrQueue() {

		boolean batch = transaction.isBatchThisRequest();

		BatchControl control = transaction.getBatchControl();
		if (control != null) {
			return control.executeOrQueue(this, batch);
		}
		if (batch) {
			control = persistExecute.createBatchControl(transaction);
			return control.executeOrQueue(this, batch);
		
		} else {
			return executeNow();
		}
	}



	/**
	 * Set the generated key back to the bean.
	 * Only used for inserts with getGeneratedKeys.
	 */
	public void setGeneratedKey(Object idValue) {
        if (idValue != null) {
        	
        	// set back to the bean so that we can use the same bean later
        	// for update [refer ebeanIntercept.setLoaded(true)].
        	idValue = beanDescriptor.convertSetId(idValue, bean);
        	            
            // remember it for logging summary
            this.idValue = idValue;
        }
	}
	
	/**
	 * Set the Id value that was bound. Used for the purposes of logging summary
	 * information on this request.
	 */
	public void setBoundId(Object idValue) {
		this.idValue = idValue;
	}
	
	/**
	 * Check for optimistic concurrency exception.
	 */
	public void checkRowCount(int rowCount) throws SQLException {
		if (rowCount != 1) {
	        String m = Message.msg("persist.conc2", "" + rowCount);
	        throw new OptimisticLockException(m);
	    }
	}

    /**
     * Post processing.
     */
	public void postExecute() throws SQLException {

		if (controller != null) {
			controllerPost();
		}

        if (bean instanceof EntityBean) {
            // if bean persisted again then should result in an update
            EntityBean entityBean = (EntityBean) bean;
            entityBean._ebean_getIntercept().setLoaded();
        }

        addEvent();

        if (transaction.isLoggingOn()) {
        	if (logLevel >= LogControl.LOG_SUMMARY){
        		logSummary();
        	}
        }
    }

	private void controllerPost() {
		switch (type) {
		case INSERT:
			controller.postInsert(this);
			break;
		case UPDATE:
			controller.postUpdate(this);
			break;
		case DELETE:
			controller.postDelete(this);
			break;
		default:
			break;
		}
	}
	
	private void logSummary() {
        // log to the transaction log
        String typeDesc = beanDescriptor.getFullName();
    	switch (type) {
		case INSERT:
			String im = "Inserted ["+typeDesc+"] ["+idValue+"]";
			transaction.log(im);
			break;
		case UPDATE:
			String um = "Updated ["+typeDesc+"] ["+idValue+"]";
			transaction.log(um);
			break;
		case DELETE:
			String dm = "Deleted ["+typeDesc+"] ["+idValue+"]";
			transaction.log(dm);
			break;
		default:
			break;
		}
	}
	
    /**
     * Add the bean to the TransactionEvent. This will be used by
     * TransactionManager to synch Cache, Cluster and Lucene.
     */
    private void addEvent() {

        TransactionEvent event = transaction.getEvent();
        if (event != null) {
            event.add(this, type);
        }
    }

	
}
