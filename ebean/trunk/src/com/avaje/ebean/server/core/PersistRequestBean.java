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

import java.io.Serializable;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.OptimisticLockException;

import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.ValidationException;
import com.avaje.ebean.bean.BeanPersistController;
import com.avaje.ebean.bean.BeanPersistRequest;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.control.LogControl;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.persist.BatchControl;
import com.avaje.ebean.server.persist.PersistExecute;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebean.server.transaction.RemoteListenerPayload;
import com.avaje.ebean.server.transaction.TransactionEvent;
import com.avaje.ebean.util.Message;

/**
 * PersistRequest for insert update or delete of a bean.
 */
public final class PersistRequestBean<T> extends PersistRequest implements BeanPersistRequest<T> {
	
	final BeanManager<T> beanManager;

	final BeanDescriptor<T> beanDescriptor;
	
	/**
	 * For per post insert update delete control.
	 */
	final BeanPersistController<T> controller;
	
	/**
	 * The associated intercept.
	 */
	final EntityBeanIntercept intercept;
	
	/**
	 * The parent bean for unidirectional save.
	 */
	final Object parentBean;

	final boolean isDirty;
	
	/**
	 * True if this is a vanilla bean.
	 */
	final boolean vanilla;
	
	/**
	 * The bean being persisted.
	 */
	final T bean;

	/**
	 * Old values used for concurrency checking.
	 */
	T oldValues;


	/**
	 * The concurrency mode used for update or delete.
	 */
	ConcurrencyMode concurrencyMode;

	final Set<String> loadedProps;
	
	/**
	 * The unique id used for logging summary.
	 */
	Object idValue;
	
	Set<String> changedProps;
	
    @SuppressWarnings("unchecked")
	public PersistRequestBean(InternalEbeanServer server, T bean, Object parentBean, BeanManager<T> mgr, ServerTransaction t, PersistExecute persistExecute) {
       super(server, t, persistExecute);
		this.beanManager = mgr;
		this.beanDescriptor = mgr.getBeanDescriptor();
		
		this.bean = bean;
		this.parentBean = parentBean;

		controller = beanDescriptor.getBeanController();
		concurrencyMode = beanDescriptor.getConcurrencyMode();

		if (bean instanceof EntityBean) {
			intercept = ((EntityBean) bean)._ebean_getIntercept();
			if (intercept.isReference()) {
				// allowed to delete reference objects
				// with no concurrency checking
				concurrencyMode = ConcurrencyMode.NONE;
			}
			// this is ok to not use isNewOrDirty() as used for updates only
			isDirty = intercept.isDirty();
			loadedProps = intercept.getLoadedProps();
			oldValues = (T)intercept.getOldValues();
			vanilla = false;
			
		} else {
			// have to assume the vanilla bean is dirty
			vanilla = true;
			isDirty = true;
			loadedProps = null;
			intercept = null;

			// degrade concurrency checking to none for vanilla bean
			if (concurrencyMode.equals(ConcurrencyMode.ALL)) {
				concurrencyMode = ConcurrencyMode.NONE;
			}
		}
    }
    
	/**
	 * Set the type of this request. One of INSERT, UPDATE, DELETE, UPDATESQL or
	 * CALLABLESQL.
	 */
    @Override
	public void setType(Type type) {
		this.type = type;
		if (type == Type.DELETE || type == Type.UPDATE) {
			if (oldValues == null) {
				oldValues = bean;
			}
		}
	}
	
	public BeanManager<T> getBeanManager() {
		return beanManager;
	}

	/**
	 * Return the BeanDescriptor for the associated bean.
	 */
	public BeanDescriptor<T> getBeanDescriptor() {
		return beanDescriptor;
	}
	
	/**
	 * Used to skip updates if we know the bean is not dirty. This is the case
	 * for EntityBeans that have not been modified.
	 */
	public boolean isDirty() {
		return isDirty;
	}
	
	/**
	 * Return the concurrency mode used for this persist.
	 */
	public ConcurrencyMode getConcurrencyMode() {
		return concurrencyMode;
	}

	public Set<String> getLoadedProperties() {
		return loadedProps;
	}
	
	/**
	 * Returns a description of the request. This is typically the bean class
	 * name or the base table for MapBeans.
	 * <p>
	 * Used to determine common persist requests for queueing and statement
	 * batching.
	 * </p>
	 */
	public String getFullName() {
		return beanDescriptor.getFullName();
	}

	/**
	 * Return the bean associated with this request.
	 */
	public T getBean() {
		return bean;
	}
	
	/**
	 * Get the old values bean. This is used to perform optimistic concurrency
	 * checking on updates and deletes.
	 */
	public T getOldValues() {
		return oldValues;
	}

	/**
	 * Return the parent bean for cascading save with unidirectional relationship.
	 */
	public Object getParentBean() {
		return parentBean;
	}

	/**
	 * Return the controller if there is one associated with this type of bean.
	 * This returns null if there is no controller associated.
	 */
	public BeanPersistController<T> getBeanController() {
		return controller;
	}
	/**
	 * Return the intercept if there is one.
	 */
	public EntityBeanIntercept getEntityBeanIntercept() {
		return intercept;
	}

	
	/**
	 * Validate the bean. This is not recursive and only runs the 'local'
	 * validation rules.
	 */
	public void validate() {
		InvalidValue errs = beanDescriptor.validate(false, bean);
		if (errs != null) {
			throw new ValidationException(errs);
		}
	}

	/**
	 * Return true if this property is loaded (full bean or included in partial bean).
	 */
	public boolean isLoadedProperty(BeanProperty prop) {
		if (loadedProps == null) {
			return true;
		} else {
			return loadedProps.contains(prop.getName());
		}
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
	        throw new OptimisticLockException(m, null, bean);
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
            event.add(this);
        }
    }

    /**
     * Determine the concurrency mode depending on fully/partially populated bean.
     * <p>
     * Specifically with version concurrency we want to check that the version 
     * property was one of the loaded properties.
     * </p>
     */
	public ConcurrencyMode determineConcurrencyMode() {    
		if (loadedProps != null){
			// 'partial bean' update/delete...
			if (concurrencyMode.equals(ConcurrencyMode.VERSION)){
				// check the version property was loaded
				BeanProperty prop = beanDescriptor.firstVersionProperty();
				if (prop != null && loadedProps.contains(prop.getName())){
					// OK to use version property
				} else {
					concurrencyMode = ConcurrencyMode.ALL;
				}
			}
		}	
		return concurrencyMode;
	}

	/**
	 * Return true if the update DML/SQL must be dynamically generated.
	 * <p>
	 * This is the case for updates/deletes of partially populated beans.
	 * </p>
	 */
	public boolean isDynamicUpdateSql() {
		return !vanilla && beanDescriptor.isUpdateChangesOnly() || (loadedProps != null);
	}
	
	/**
	 * Create a GenerateDmlRequest used to generate the DML.
	 * <p>
	 * Will used changed properties or loaded properties depending on
	 * the BeanDescriptor.isUpdateChangesOnly() value.
	 * </p>
	 */
	public GenerateDmlRequest createGenerateDmlRequest() {
		if (changedProps != null){
			return new GenerateDmlRequest(changedProps, oldValues);	
		} else {
			return new GenerateDmlRequest(loadedProps, oldValues);
		}
	}
	
	/**
	 * Return the updated properties. If this returns null then all
	 * the properties on the bean where updated.
	 */
	public Set<String> getUpdatedProperties() {
		if (changedProps != null){
			return changedProps;
		}
		return loadedProps;
	}
	
	/**
	 * Return true if we only want to include changed properties in an update.
	 */
	public boolean isUpdateChangesOnly() {
		if (beanDescriptor.isUpdateChangesOnly()){
			changedProps = new LinkedHashSet<String>();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Test if the property value has changed and if so include it in the update.
	 */
	public boolean hasChanged(BeanProperty prop) {
		if (isLoadedProperty(prop) && prop.hasChanged(bean, oldValues)) {
			changedProps.add(prop.getName());
			return true;
		} else {
			return false;
		}
	}
	
	public RemoteListenerPayload createRemoteListenerPayload() {
		
		Serializable sid = (Serializable)idValue;
		return new RemoteListenerPayload(beanDescriptor.getFullName(), type, sid);
	}
}
