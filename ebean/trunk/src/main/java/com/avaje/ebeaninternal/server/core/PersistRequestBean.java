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
package com.avaje.ebeaninternal.server.core;

import java.sql.SQLException;
import java.util.Set;

import javax.persistence.OptimisticLockException;

import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.ValidationException;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistListener;
import com.avaje.ebean.event.BeanPersistRequest;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.api.TransactionEvent;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanManager;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.jmx.MAdminLogging;
import com.avaje.ebeaninternal.server.persist.BatchControl;
import com.avaje.ebeaninternal.server.persist.PersistExecute;
import com.avaje.ebeaninternal.server.persist.dml.GenerateDmlRequest;
import com.avaje.ebeaninternal.server.transaction.RemoteBeanPersistMap;

/**
 * PersistRequest for insert update or delete of a bean.
 */
public class PersistRequestBean<T> extends PersistRequest implements BeanPersistRequest<T> {

    protected final BeanManager<T> beanManager;

    protected final BeanDescriptor<T> beanDescriptor;

    protected final BeanPersistListener<T> beanPersistListener;

    /**
     * For per post insert update delete control.
     */
    protected final BeanPersistController controller;

    /**
     * The associated intercept.
     */
    protected final EntityBeanIntercept intercept;

    /**
     * The parent bean for unidirectional save.
     */
    protected final Object parentBean;

    protected final boolean isDirty;

    /**
     * True if this is a vanilla bean.
     */
    protected final boolean vanilla;

    /**
     * The bean being persisted.
     */
    protected final T bean;

    /**
     * Old values used for concurrency checking.
     */
    protected T oldValues;

    /**
     * The concurrency mode used for update or delete.
     */
    protected ConcurrencyMode concurrencyMode;

    protected final Set<String> loadedProps;

    /**
     * The unique id used for logging summary.
     */
    protected Object idValue;

    /**
     * Hash value used to handle cascade delete both ways in a relationship.
     */
    protected Integer beanHash;

    protected final Set<String> changedProps;

    protected boolean notifyCache;

    /**
     * Used for forced update of a bean.
     */
    public PersistRequestBean(SpiEbeanServer server, T bean, Object parentBean, BeanManager<T> mgr, SpiTransaction t,
            PersistExecute persistExecute, Set<String> updateProps, ConcurrencyMode concurrencyMode) {

        super(server, t, persistExecute);
        this.beanManager = mgr;
        this.beanDescriptor = mgr.getBeanDescriptor();
        this.beanPersistListener = beanDescriptor.getPersistListener();
        this.bean = bean;
        this.parentBean = parentBean;

        this.controller = beanDescriptor.getPersistController();
        this.concurrencyMode = beanDescriptor.getConcurrencyMode();

        this.concurrencyMode = concurrencyMode;
        this.loadedProps = updateProps;
        this.changedProps = updateProps;

        this.vanilla = true;
        this.isDirty = true;
        this.oldValues = bean;
        if (bean instanceof EntityBean) {
            this.intercept = ((EntityBean)bean)._ebean_getIntercept();
        } else {
            this.intercept = null;
        }
    }

    @SuppressWarnings("unchecked")
    public PersistRequestBean(SpiEbeanServer server, T bean, Object parentBean, BeanManager<T> mgr, SpiTransaction t,
            PersistExecute persistExecute) {
        super(server, t, persistExecute);
        this.beanManager = mgr;
        this.beanDescriptor = mgr.getBeanDescriptor();
        this.beanPersistListener = beanDescriptor.getPersistListener();
        this.bean = bean;
        this.parentBean = parentBean;

        this.controller = beanDescriptor.getPersistController();
        this.concurrencyMode = beanDescriptor.getConcurrencyMode();

        if (bean instanceof EntityBean) {
            this.intercept = ((EntityBean) bean)._ebean_getIntercept();
            if (intercept.isReference()) {
                // allowed to delete reference objects
                // with no concurrency checking
                this.concurrencyMode = ConcurrencyMode.NONE;
            }
            // this is ok to not use isNewOrDirty() as used for updates only
            this.isDirty = intercept.isDirty();
            this.changedProps = intercept.getChangedProps();
            this.loadedProps = intercept.getLoadedProps();
            this.oldValues = (T) intercept.getOldValues();
            this.vanilla = false;

        } else {
            // have to assume the vanilla bean is dirty
            this.vanilla = true;
            this.isDirty = true;
            this.loadedProps = null;
            this.changedProps = null;
            this.intercept = null;

            // degrade concurrency checking to none for vanilla bean
            if (concurrencyMode.equals(ConcurrencyMode.ALL)) {
                this.concurrencyMode = ConcurrencyMode.NONE;
            }
        }
    }

    public boolean isNotify() {
        return notifyCache || isNotifyPersistListener();
    }

    public boolean isNotifyCache() {
        return notifyCache;
    }

    public boolean isNotifyPersistListener() {
        return beanPersistListener != null;
    }

    public void notifyCache() {
        if (notifyCache) {
            beanDescriptor.cacheRemove(idValue);
        }
    }

    public void notifyLocalPersistListener(RemoteBeanPersistMap beanPersistMap) {
        
        localNotifyPersistListener();
        
        beanPersistMap.add(beanDescriptor, type, idValue);
    }
    

    private boolean localNotifyPersistListener() {
        if (beanPersistListener == null) {
            return false;

        } else {
            switch (type) {
            case INSERT:
                return beanPersistListener.inserted(bean);

            case UPDATE:
                return beanPersistListener.updated(bean, getUpdatedProperties());

            case DELETE:
                return beanPersistListener.deleted(bean);

            default:
                return false;
            }
        }
    }

    public boolean isParent(Object o) {
        return o == parentBean;
    }
    
    /**
     * The hash used to register the bean with the transaction.
     * <p>
     * Takes into account the class type and id value.
     * </p>
     */
    private Integer getBeanHash() {
        if (beanHash == null){
            Object id = beanDescriptor.getId(bean);
            int hc = bean.getClass().getName().hashCode();
            beanHash = Integer.valueOf(hc * 31 + id.hashCode());
        }
        return beanHash;
    }

    /**
     * Register this bean with the transaction. Used to detect Cascade delete on
     * both sides of a relationship.
     */
    public void registerBean() {
        transaction.registerBean(getBeanHash());
    }

    /**
     * Unregister the bean from the transaction.
     */
    public void unregisterBean() {
        transaction.unregisterBean(getBeanHash());
    }

    /**
     * Return true if this bean has been registered with the transaction.
     */
    public boolean isRegistered() {
        if (transaction == null) {
            return false;
        }
        return transaction.isRegisteredBean(getBeanHash());
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
            if (beanDescriptor.isCaching()) {
                notifyCache = true;
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

    /**
     * Set loaded properties when generated values has added properties
     * such as created and updated timestamps.
     */
    public void setLoadedProps(Set<String> additionalProps){
        if (intercept != null){
            intercept.setLoadedProps(additionalProps);
        }
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
     * Return the Id value for the bean.
     */
    public Object getBeanId() {
        return beanDescriptor.getId(bean);
    }
    
    /**
     * Get the old values bean. This is used to perform optimistic concurrency
     * checking on updates and deletes.
     */
    public T getOldValues() {
        return oldValues;
    }

    /**
     * Return the parent bean for cascading save with unidirectional
     * relationship.
     */
    public Object getParentBean() {
        return parentBean;
    }

    /**
     * Return the controller if there is one associated with this type of bean.
     * This returns null if there is no controller associated.
     */
    public BeanPersistController getBeanController() {
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
     * Return true if this property is loaded (full bean or included in partial
     * bean).
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
     * Set the generated key back to the bean. Only used for inserts with
     * getGeneratedKeys.
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

        if (intercept != null){
            // if bean persisted again then should result in an update
            intercept.setLoaded();
        }

        addEvent();

        if (transaction.isLoggingOn()) {
            if (logLevel >= MAdminLogging.SUMMARY) {
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

        String name = beanDescriptor.getName();
        switch (type) {
        case INSERT:
            transaction.log("Inserted [" + name + "] [" + idValue + "]");
            break;
        case UPDATE:
            transaction.log("Updated [" + name + "] [" + idValue + "]");
            break;
        case DELETE:
            transaction.log("Deleted [" + name + "] [" + idValue + "]");
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
     * Determine the concurrency mode depending on fully/partially populated
     * bean.
     * <p>
     * Specifically with version concurrency we want to check that the version
     * property was one of the loaded properties.
     * </p>
     */
    public ConcurrencyMode determineConcurrencyMode() {
        if (loadedProps != null) {
            // 'partial bean' update/delete...
            if (concurrencyMode.equals(ConcurrencyMode.VERSION)) {
                // check the version property was loaded
                BeanProperty prop = beanDescriptor.firstVersionProperty();
                if (prop != null && loadedProps.contains(prop.getName())) {
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
     * Will used changed properties or loaded properties depending on the
     * BeanDescriptor.isUpdateChangesOnly() value.
     * </p>
     */
    public GenerateDmlRequest createGenerateDmlRequest(boolean emptyStringAsNull) {
        if (beanDescriptor.isUpdateChangesOnly()) {
            return new GenerateDmlRequest(emptyStringAsNull, changedProps, loadedProps, oldValues);
        } else {
            return new GenerateDmlRequest(emptyStringAsNull, loadedProps, loadedProps, oldValues);
        }
    }

    /**
     * Return the updated properties. If this returns null then all the
     * properties on the bean where updated.
     */
    public Set<String> getUpdatedProperties() {
        if (changedProps != null) {
            return changedProps;
        }
        return loadedProps;
    }

    /**
     * Test if the property value has changed and if so include it in the
     * update.
     */
    public boolean hasChanged(BeanProperty prop) {

        return changedProps.contains(prop.getName());
    }

}
