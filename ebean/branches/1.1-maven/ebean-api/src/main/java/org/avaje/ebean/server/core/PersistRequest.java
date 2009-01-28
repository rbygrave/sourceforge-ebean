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
package org.avaje.ebean.server.core;

import java.util.Set;

import org.avaje.ebean.InvalidValue;
import org.avaje.ebean.ValidationException;
import org.avaje.ebean.bean.BeanController;
import org.avaje.ebean.bean.EntityBean;
import org.avaje.ebean.bean.EntityBeanIntercept;
import org.avaje.ebean.server.deploy.BeanManager;
import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.persist.BatchControl;
import org.avaje.ebean.server.persist.BatchPostExecute;
import org.avaje.ebean.server.persist.PersistExecute;

/**
 * Wraps all the objects used to persist a bean.
 */
public abstract class PersistRequest extends BeanRequest implements ConcurrencyMode,
		BatchPostExecute {

	public enum Type {
		INSERT, UPDATE, DELETE, ORMUPDATE, UPDATESQL, CALLABLESQL
	};

	final boolean isDirty;

	/**
	 * The bean being persisted.
	 */
	final Object bean;

	/**
	 * The parent bean for unidirectional save.
	 */
	final Object parentBean;

	/**
	 * The associated intercept.
	 */
	final EntityBeanIntercept intercept;

	/**
	 * For per post insert update delete control.
	 */
	final BeanController controller;

	boolean persistCascade;

	/**
	 * Old values used for concurrency checking.
	 */
	Object oldValues;

	/**
	 * The concurrency mode used for update or delete.
	 */
	int concurrencyMode;

	/**
	 * One of INSERT, UPDATE, DELETE, UPDATESQL or CALLABLESQL.
	 */
	Type type;

	/**
	 * The log level of for this request. One of LOG_NONE LOG_SUMMARY LOG_BIND
	 * or LOG_SQL.
	 */
	int logLevel;

	final Set<String> loadedProps;

	final PersistExecute persistExecute;
	
	/**
	 * Construct for bean insert update or delete.
	 */
	public PersistRequest(InternalEbeanServer server, Object bean, Object parentBean,
			BeanManager mgr, ServerTransaction t, PersistExecute persistExecute) {
		
		super(server, mgr, t);
		this.persistExecute = persistExecute;
		this.bean = bean;
		this.parentBean = parentBean;

		controller = beanDescriptor.getBeanController();
		concurrencyMode = beanDescriptor.getConcurrencyMode();

		if (bean instanceof EntityBean) {
			intercept = ((EntityBean) bean)._ebean_getIntercept();
			if (intercept.isReference()) {
				// allowed to delete reference objects
				// with no concurrency checking
				concurrencyMode = NONE;
			}
			isDirty = beanDescriptor.isDirty(intercept);
			loadedProps = intercept.getLoadedProps();
			oldValues = intercept.getOldValues();
			
		} else {
			loadedProps = null;
			intercept = null;
			// have to assume the vanilla bean is dirty
			isDirty = true;

			// degrade concurrency checking to none for vanilla bean
			if (concurrencyMode == ALL) {
				concurrencyMode = NONE;
			}
		}
	}

	/**
	 * Used by CallableSqlRequest and UpdateSqlRequest.
	 */
	public PersistRequest(InternalEbeanServer server, ServerTransaction t, PersistExecute persistExecute) {
		super(server, null, t);
		this.persistExecute = persistExecute;
		isDirty = true;
		bean = null;
		parentBean = null;
		controller = null;
		intercept = null;
		loadedProps = null;
	}

	/**
	 * Execute a the request or queue/batch it for later execution.
	 */
	public abstract int executeOrQueue();

	/**
	 * Execute the request right now.
	 */
	public abstract int executeNow();

	
	/**
	 * Execute the Callable statement.
	 */
	public int executeStatement() {

		initTransIfRequired();
		
		//ServerTransaction t = request.getTransaction();
		boolean batch = transaction.isBatchThisRequest();

		int rows;
		BatchControl control = transaction.getBatchControl();
		if (control != null) {
			rows = control.executeStatementOrBatch(this, batch);
		
		} else if (batch) {
			// need to create the BatchControl
			control = persistExecute.createBatchControl(transaction);
			rows = control.executeStatementOrBatch(this, batch);
		} else {
			rows = executeNow();
		}
		
		commitTransIfRequired();
		
		return rows;
	}
	
	public void initTransIfRequired() {
		createImplicitTransIfRequired(false);
		persistCascade = transaction.isPersistCascade();
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
	 * Return true if this property should be included in the request.
	 * <p>
	 * Used for partially loaded beans.
	 * </p>
	 */
	public boolean isIncludeProperty(BeanProperty prop) {
		if (loadedProps == null) {
			return true;
		} else {
			return loadedProps.contains(prop.getName());
		}
	}

	/**
	 * Used to skip updates if we know the bean is not dirty. This is the case
	 * for EntityBeans that have not been modified.
	 */
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * Return the intercept if there is one.
	 */
	public EntityBeanIntercept getEntityBeanIntercept() {
		return intercept;
	}

	/**
	 * Return the concurrency mode used for this persist.
	 */
	public int getConcurrencyMode() {
		return concurrencyMode;
	}

	/**
	 * Set the concurrency mode if updating a paritally loaded bean.
	 * <p>
	 * The mode then depends on whether a version column has been included.
	 * </p>
	 */
	public void setConcurrencyMode(int concurrencyMode) {
		this.concurrencyMode = concurrencyMode;
	}

	/**
	 * Get the old values bean. This is used to perform optimistic concurrency
	 * checking on updates and deletes.
	 */
	public Object getOldValues() {
		return oldValues;
	}

	/**
	 * Return the type of this request. One of INSERT, UPDATE, DELETE, UPDATESQL
	 * or CALLABLESQL.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the type of this request. One of INSERT, UPDATE, DELETE, UPDATESQL or
	 * CALLABLESQL.
	 */
	public void setType(Type type) {
		this.type = type;
		if (type == Type.DELETE || type == Type.UPDATE) {
			if (oldValues == null) {
				oldValues = bean;
			}
		}
	}

	/**
	 * Set the logLevel for this request.
	 */
	public void setLogLevel(int logLevel) {
		this.logLevel = logLevel;
	}

	/**
	 * Return the logLevel for this request.
	 */
	public int getLogLevel() {
		return logLevel;
	}

	/**
	 * Set the Id value that was bound. Used for the purposes of logging summary
	 * information on this request.
	 */
	public void setBoundId(Object idValue) {
		// by default this does nothing
		// overridden by PersistRequestBean
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
	public Object getBean() {
		return bean;
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
	public BeanController getBeanController() {
		return controller;
	}

	/**
	 * Return true if save and delete should cascade.
	 */
	public boolean isPersistCascade() {
		return persistCascade;
	}

}
