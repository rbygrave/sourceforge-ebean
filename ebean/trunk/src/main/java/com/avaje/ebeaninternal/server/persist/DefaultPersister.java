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
package com.avaje.ebeaninternal.server.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.Update;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollection.ModifyListenMode;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.config.ldap.LdapContextFactory;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.api.SpiUpdate;
import com.avaje.ebeaninternal.server.core.ConcurrencyMode;
import com.avaje.ebeaninternal.server.core.Message;
import com.avaje.ebeaninternal.server.core.PersistRequest;
import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.core.PersistRequestCallableSql;
import com.avaje.ebeaninternal.server.core.PersistRequestOrmUpdate;
import com.avaje.ebeaninternal.server.core.PersistRequestUpdateSql;
import com.avaje.ebeaninternal.server.core.Persister;
import com.avaje.ebeaninternal.server.core.PstmtBatch;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;
import com.avaje.ebeaninternal.server.deploy.BeanManager;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebeaninternal.server.deploy.IntersectionRow;
import com.avaje.ebeaninternal.server.deploy.ManyType;
import com.avaje.ebeaninternal.server.ldap.DefaultLdapPersister;
import com.avaje.ebeaninternal.server.ldap.LdapPersistBeanRequest;

/**
 * Persister implementation using DML.
 * <p>
 * This object uses DmlPersistExecute to perform the actual persist execution.
 * </p>
 * <p>
 * This object:
 * <ul>
 * <li>Determines insert or update for saved beans</li>
 * <li>Determines the concurrency mode</li>
 * <li>Handles cascading of save and delete</li>
 * <li>Handles the batching and queueing</li>
 * </p>
 * 
 * @see com.avaje.ebeaninternal.server.persist.DefaultPersistExecute
 */
public final class DefaultPersister implements Persister {

	private static final Logger logger = Logger.getLogger(DefaultPersister.class.getName());

	/**
	 * Actually does the persisting work.
	 */
	private final PersistExecute persistExecute;

	private final DefaultLdapPersister ldapPersister;

	private final SpiEbeanServer server;

	private final BeanDescriptorManager beanDescriptorManager;

	private final boolean defaultUpdateNullProperties;
	private final boolean defaultDeleteMissingChildren;

	public DefaultPersister(SpiEbeanServer server, boolean validate,
	        Binder binder, BeanDescriptorManager descMgr, PstmtBatch pstmtBatch, LdapContextFactory contextFactory) {

		this.server = server;
		this.beanDescriptorManager = descMgr;

		this.persistExecute = new DefaultPersistExecute(validate, binder, pstmtBatch);
		this.ldapPersister = new DefaultLdapPersister(contextFactory);
		
        this.defaultUpdateNullProperties = server.isDefaultUpdateNullProperties();
        this.defaultDeleteMissingChildren = server.isDefaultDeleteMissingChildren();
	}

	/**
	 * Execute the CallableSql.
	 */
	public int executeCallable(CallableSql callSql, Transaction t) {

		PersistRequestCallableSql request = new PersistRequestCallableSql(server, callSql, (SpiTransaction) t, persistExecute);
		try {
			request.initTransIfRequired();
			int rc = request.executeOrQueue();
			request.commitTransIfRequired();
			return rc;

		} catch (RuntimeException e) {
			request.rollbackTransIfRequired();
			throw e;
		}
	}

	/**
	 * Execute the orm update.
	 */
	public int executeOrmUpdate(Update<?> update, Transaction t) {

		SpiUpdate<?> ormUpdate = (SpiUpdate<?>) update;

		BeanManager<?> mgr = beanDescriptorManager.getBeanManager(ormUpdate.getBeanType());

		if (mgr == null) {
			String msg = "No BeanManager found for type [" + ormUpdate.getBeanType() + "]. Is it an entity?";
			throw new PersistenceException(msg);
		}

		PersistRequestOrmUpdate request = new PersistRequestOrmUpdate(server, mgr, ormUpdate, (SpiTransaction) t, persistExecute);
		try {
			request.initTransIfRequired();
			int rc = request.executeOrQueue();
			request.commitTransIfRequired();
			return rc;

		} catch (RuntimeException e) {
			request.rollbackTransIfRequired();
			throw e;
		}
	}

	/**
	 * Execute the updateSql.
	 */
	public int executeSqlUpdate(SqlUpdate updSql, Transaction t) {

		PersistRequestUpdateSql request = new PersistRequestUpdateSql(server, updSql, (SpiTransaction) t, persistExecute);
		try {
			request.initTransIfRequired();
			int rc = request.executeOrQueue();
			request.commitTransIfRequired();
			return rc;

		} catch (RuntimeException e) {
			request.rollbackTransIfRequired();
			throw e;
		}
	}

	/**
	 * Recursively delete the bean. This calls back to the EbeanServer.
	 */
	private void deleteRecurse(Object detailBean, Transaction t) {
		// NB: a new PersistRequest is made
		server.delete(detailBean, t);
	}

	/**
	 * Force an Update using the given bean.
	 */
	public void forceUpdate(Object bean, Set<String> updateProps, Transaction t, boolean deleteMissingChildren, boolean updateNullProperties) {

		if (bean == null) {
			throw new NullPointerException(Message.msg("bean.isnull"));
		}

		if (updateProps == null) {
			// checking to see if this is just a 'normal' update
			if (bean instanceof EntityBean) {
				EntityBeanIntercept ebi = ((EntityBean) bean)._ebean_getIntercept();
				if (ebi.isDirty() || ebi.isLoaded()) {
					// a 'normal' update using 'dirty' properties from internal bean state.
					// if not dirty we still update in case any cascading save occurs
					PersistRequestBean<?> req = createRequest(bean, t, null);
					try {
						req.initTransIfRequired();
						update(req);
						req.commitTransIfRequired();
						// finished a 'normal' update
						return;

					} catch (RuntimeException ex) {
						req.rollbackTransIfRequired();
						throw ex;
					}
				} else if (ebi.isReference()) {
					// just return as no point in cascading (no modified beans/lists)
					return;
				}

				// loadedProps set by Ebean JSON / XML Marshalling
				updateProps = ebi.getLoadedProps();
			}
		}

		BeanManager<?> mgr = getBeanManager(bean);
		if (mgr == null) {
			throw new PersistenceException(errNotRegistered(bean.getClass()));
		}

		forceUpdateStateless(bean, t, null, mgr, updateProps, deleteMissingChildren, updateNullProperties);
	}

	/**
	 * Force a 'stateless' update determining which properties to update.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void forceUpdateStateless(Object bean, Transaction t, Object parentBean, BeanManager<?> mgr, Set<String> updateProps, 
			boolean deleteMissingChildren, boolean updateNullProperties) {

		BeanDescriptor<?> descriptor = mgr.getBeanDescriptor();

		// determine concurrency mode based on version property not null
		ConcurrencyMode mode = descriptor.determineConcurrencyMode(bean);

		if (updateProps == null) {
			// determine based on null treatment (all properties updated or just the non-null ones)
			updateProps = updateNullProperties ? null : descriptor.determineLoadedProperties(bean);

		} else if (updateProps.isEmpty()) {
			// in this case means we want to include all properties in the update
			updateProps = null;

		} else if (ConcurrencyMode.VERSION.equals(mode)) {
			// check that the version property is included
			String verName = descriptor.firstVersionProperty().getName();
			if (!updateProps.contains(verName)) {
				// defensively copy the updateProps and add the version property name
				updateProps = new HashSet<String>(updateProps);
				updateProps.add(verName);
			}
		}

		PersistRequestBean<?> req;
		if (descriptor.isLdapEntityType()) {
			req = new LdapPersistBeanRequest(server, bean, parentBean, mgr, ldapPersister, updateProps, mode);

		} else {
			// special constructor for force 'stateless' Update mode ...
			req = new PersistRequestBean(server, bean, parentBean, mgr, (SpiTransaction) t, persistExecute, updateProps, mode);
			req.setStatelessUpdate(true, deleteMissingChildren, updateNullProperties);
		}

		try {
			req.initTransIfRequired();
			update(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	public void save(Object bean, Transaction t) {
		saveRecurse(bean, t, null);
	}

	/**
	 * Explicitly specify to insert this bean.
	 */
	public void forceInsert(Object bean, Transaction t) {

		PersistRequestBean<?> req = createRequest(bean, t, null);
		try {
			req.initTransIfRequired();
			insert(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	private void saveRecurse(Object bean, Transaction t, Object parentBean) {
		if (bean == null) {
			throw new NullPointerException(Message.msg("bean.isnull"));
		}

		if (bean instanceof EntityBean == false) {
			saveVanillaRecurse(bean, t, parentBean);
			return;
		}

		PersistRequestBean<?> req = createRequest(bean, t, parentBean);
		try {
			req.initTransIfRequired();
			saveEnhanced(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	/**
	 * Insert or update the bean depending on PersistControl and the bean state.
	 */
	private void saveEnhanced(PersistRequestBean<?> request) {

		EntityBeanIntercept intercept = request.getEntityBeanIntercept();

		if (intercept.isReference()) {
			// its a reference...
			if (request.isPersistCascade()) {
				// save any associated List held beans
				intercept.setLoaded();
				saveAssocMany(false, request);
				intercept.setReference();
			}

		} else {
			if (intercept.isLoaded()) {
				// Need to call setLoaded(false) to simulate insert
				update(request);
			} else {
				insert(request);
			}
		}
	}

	/**
	 * Determine if this is an Insert or update for the 'vanilla' bean.
	 */
	private void saveVanillaRecurse(Object bean, Transaction t, Object parentBean) {

		BeanManager<?> mgr = getBeanManager(bean);
		if (mgr == null) {
			throw new RuntimeException("No Mgr found for " + bean + " " + bean.getClass());
		}
		// use the version property to determine insert or update
		if (mgr.getBeanDescriptor().isVanillaInsert(bean)) {
			saveVanillaInsert(bean, t, parentBean, mgr);

		} else {
			// update non-null properties (no partial object knowledge with vanilla bean)
			forceUpdateStateless(bean, t, parentBean, mgr, null, defaultDeleteMissingChildren, defaultUpdateNullProperties);
		}
	}

	/**
	 * Perform insert on non-enhanced bean (effectively same as enhanced bean).
	 */
	private void saveVanillaInsert(Object bean, Transaction t, Object parentBean, BeanManager<?> mgr) {

		PersistRequestBean<?> req = createRequest(bean, t, parentBean, mgr);
		try {
			req.initTransIfRequired();
			insert(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	/**
	 * Insert the bean.
	 */
	private void insert(PersistRequestBean<?> request) {

		if (request.isRegisteredBean()){
			// skip as already inserted/updated in this request (recursive cascading)
			return;
		}
		
		try {
			request.setType(PersistRequest.Type.INSERT);
	
			if (request.isPersistCascade()) {
				// save associated One beans recursively first
				saveAssocOne(request);
			}
	
			// set the IDGenerated value if required
			setIdGenValue(request);
			request.executeOrQueue();
			
			if (request.isPersistCascade()) {
				// save any associated List held beans
				saveAssocMany(true, request);
			}
		} finally {
			request.unRegisterBean();
		}
	}

	/**
	 * Update the bean. Return NOT_SAVED if the bean values have not changed.
	 */
	private void update(PersistRequestBean<?> request) {

		if (request.isRegisteredBean()){
			// skip as already inserted/updated in this request (recursive cascading)
			return;
		}
		
		try {
			// we have determined that it is an update
			request.setType(PersistRequest.Type.UPDATE);
			if (request.isPersistCascade()) {
				// save associated One beans recursively first
				saveAssocOne(request);
			}
	
			if (request.isDirty()) {
				request.executeOrQueue();
	
			} else {
				// skip validation on unchanged bean
				if (logger.isLoggable(Level.FINE)) {
					logger.fine(Message.msg("persist.update.skipped", request.getBean()));
				}
			}
	
			if (request.isPersistCascade()) {
				// save all the beans in assocMany's after
				saveAssocMany(false, request);
			}
		} finally {
			request.unRegisterBean();
		}
	}

	/**
	 * Delete the bean with the explicit transaction.
	 */
	public void delete(Object bean, Transaction t) {

		PersistRequestBean<?> req = createRequest(bean, t, null);
		if (req.isRegisteredForDeleteBean()) {
			// skip deleting bean. Used where cascade is on
			// both sides of a relationship
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("skipping delete on alreadyRegistered " + bean);
			}
			return;
		}
		req.setType(PersistRequest.Type.DELETE);
		try {
			req.initTransIfRequired();
			delete(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	private void deleteList(List<?> beanList, Transaction t) {
		for (int i = 0; i < beanList.size(); i++) {
			Object bean = beanList.get(i);
			delete(bean, t);
		}
	}

	/**
	 * Delete by a List of Id's.
	 */
	public void deleteMany(Class<?> beanType, Collection<?> ids, Transaction transaction) {

		if (ids == null || ids.size() == 0) {
			return;
		}

		BeanDescriptor<?> descriptor = beanDescriptorManager.getBeanDescriptor(beanType);

		ArrayList<Object> idList = new ArrayList<Object>(ids.size());
		for (Object id : ids) {
			// convert to appropriate type if required
			idList.add(descriptor.convertId(id));
		}

		delete(descriptor, null, idList, transaction);
	}

	/**
	 * Delete by Id.
	 */
	public int delete(Class<?> beanType, Object id, Transaction transaction) {

		BeanDescriptor<?> descriptor = beanDescriptorManager.getBeanDescriptor(beanType);

		// convert to appropriate type if required
		id = descriptor.convertId(id);
		return delete(descriptor, id, null, transaction);
	}

	/**
	 * Delete by Id or a List of Id's.
	 */
	private int delete(BeanDescriptor<?> descriptor, Object id, List<Object> idList, Transaction transaction) {

		SpiTransaction t = (SpiTransaction) transaction;
		if (t.isPersistCascade()) {
			BeanPropertyAssocOne<?>[] propImportDelete = descriptor.propertiesOneImportedDelete();
			if (propImportDelete.length > 0) {
				// We actually need to execute a query to get the foreign key values
				// as they are required for the delete cascade. Query back just the
				// Id and the appropriate foreign key values
				Query<?> q = deleteRequiresQuery(descriptor, propImportDelete);
				if (idList != null) {
					q.where().idIn(idList);
					if (t.isLogSummary()) {
						t.logInternal("-- DeleteById of " + descriptor.getName() + " ids[" + idList + "] requires fetch of foreign key values");
					}
					List<?> beanList = server.findList(q, t);
					deleteList(beanList, t);
					return beanList.size();

				} else {
					q.where().idEq(id);
					if (t.isLogSummary()) {
						t.logInternal("-- DeleteById of " + descriptor.getName() + " id[" + id + "] requires fetch of foreign key values");
					}
					Object bean = server.findUnique(q, t);
					if (bean == null) {
						return 0;
					} else {
						delete(bean, t);
						return 1;
					}
				}
			}
		}

		if (t.isPersistCascade()) {
			// OneToOne exported side with delete cascade
			BeanPropertyAssocOne<?>[] expOnes = descriptor.propertiesOneExportedDelete();
			for (int i = 0; i < expOnes.length; i++) {
				BeanDescriptor<?> targetDesc = expOnes[i].getTargetDescriptor();
				if (targetDesc.isDeleteRecurseSkippable() && !targetDesc.isUsingL2Cache()) {
					SqlUpdate sqlDelete = expOnes[i].deleteByParentId(id, idList);
					executeSqlUpdate(sqlDelete, t);
				} else {
					List<Object> childIds = expOnes[i].findIdsByParentId(id, idList, t);
					delete(targetDesc, null, childIds, t);
				}
			}

			// OneToMany's with delete cascade
			BeanPropertyAssocMany<?>[] manys = descriptor.propertiesManyDelete();
			for (int i = 0; i < manys.length; i++) {
				BeanDescriptor<?> targetDesc = manys[i].getTargetDescriptor();
				if (targetDesc.isDeleteRecurseSkippable() && !targetDesc.isUsingL2Cache()) {
					// we can just delete children with a single statement
					SqlUpdate sqlDelete = manys[i].deleteByParentId(id, idList);
					executeSqlUpdate(sqlDelete, t);
				} else {
					// we need to fetch the Id's to delete (recurse or notify L2 cache/lucene)
					List<Object> childIds = manys[i].findIdsByParentId(id, idList, t, null);
					delete(targetDesc, null, childIds, t);
				}
			}
		}

		// ManyToMany's ... delete from intersection table
		BeanPropertyAssocMany<?>[] manys = descriptor.propertiesManyToMany();
		for (int i = 0; i < manys.length; i++) {
			SqlUpdate sqlDelete = manys[i].deleteByParentId(id, idList);
			if (t.isLogSummary()) {
				t.logInternal("-- Deleting intersection table entries: " + manys[i].getFullBeanName());
			}
			executeSqlUpdate(sqlDelete, t);
		}

		// delete the bean(s)
		SqlUpdate deleteById = descriptor.deleteById(id, idList);
		if (t.isLogSummary()) {
			t.logInternal("-- Deleting " + descriptor.getName() + " Ids" + idList);
		}

		// use Id's to update L2 cache rather than Bulk table event
		deleteById.setAutoTableMod(false);
		if (idList != null) {
			t.getEvent().addDeleteByIdList(descriptor, idList);
		} else {
			t.getEvent().addDeleteById(descriptor, id);
		}
		return executeSqlUpdate(deleteById, t);
	}

	/**
	 * We need to create and execute a query to get the foreign key values as
	 * the delete cascades to them (foreign keys).
	 */
	private Query<?> deleteRequiresQuery(BeanDescriptor<?> desc, BeanPropertyAssocOne<?>[] propImportDelete) {

		Query<?> q = server.createQuery(desc.getBeanType());
		StringBuilder sb = new StringBuilder(30);
		for (int i = 0; i < propImportDelete.length; i++) {
			sb.append(propImportDelete[i].getName()).append(",");
		}
		q.setAutofetch(false);
		q.select(sb.toString());
		return q;
	}

	/**
	 * Delete the bean.
	 * <p>
	 * Note that preDelete fires before the deletion of children.
	 * </p>
	 */
	private void delete(PersistRequestBean<?> request) {

		DeleteUnloadedForeignKeys unloadedForeignKeys = null;

//		boolean pauseIndexInvalidate = request.getBeanDescriptor().isLuceneIndexed();
//		if (pauseIndexInvalidate) {
//			// Stop any deletions of child beans to Invalidate the Index as we
//			// will use the delete of this top level object to maintain the index
//			request.pauseIndexInvalidate();
//		}

		if (request.isPersistCascade()) {
			// delete children first ... register the
			// bean to handle bi-directional cascading
			request.registerDeleteBean();
			deleteAssocMany(request);
			request.unregisterDeleteBean();

			unloadedForeignKeys = getDeleteUnloadedForeignKeys(request);
			if (unloadedForeignKeys != null) {
				// there are foreign keys that we don't have on this partially
				// populated bean so we actually need to query them (to cascade delete)
				unloadedForeignKeys.queryForeignKeys();
			}
		}

		request.executeOrQueue();

		if (request.isPersistCascade()) {
			deleteAssocOne(request);

			if (unloadedForeignKeys != null) {
				unloadedForeignKeys.deleteCascade();
			}
		}
//		if (pauseIndexInvalidate) {
//			request.resumeIndexInvalidate();
//		}
	}

	/**
	 * Save the associated child beans contained in a List.
	 * <p>
	 * This will automatically copy over any join properties from the parent
	 * bean to the child beans.
	 * </p>
	 */
	private void saveAssocMany(boolean insertedParent, PersistRequestBean<?> request) {

		Object parentBean = request.getBean();
		BeanDescriptor<?> desc = request.getBeanDescriptor();
		SpiTransaction t = request.getTransaction();

		// exported ones with cascade save
		BeanPropertyAssocOne<?>[] expOnes = desc.propertiesOneExportedSave();
		for (int i = 0; i < expOnes.length; i++) {
			BeanPropertyAssocOne<?> prop = expOnes[i];

			// check for partial beans
			if (request.isLoadedProperty(prop)) {
				Object detailBean = prop.getValue(parentBean);
				if (detailBean != null) {
					if (prop.isSaveRecurseSkippable(detailBean)) {
						// skip saving this bean
					} else {
						t.depth(+1);
						saveRecurse(detailBean, t, parentBean);
						t.depth(-1);
					}
				}
			}
		}

		// many's with cascade save
		BeanPropertyAssocMany<?>[] manys = desc.propertiesManySave();
		for (int i = 0; i < manys.length; i++) {
			saveMany(new SaveManyPropRequest(insertedParent, manys[i], parentBean, request));
		}
	}

	/**
	 * Helper to wrap the details when saving a OneToMany or ManyToMany
	 * relationship.
	 */
	private static class SaveManyPropRequest {
		private final boolean insertedParent;
		private final BeanPropertyAssocMany<?> many;
		private final Object parentBean;
		private final SpiTransaction t;
		private final boolean cascade;
		private final boolean statelessUpdate;
		private final boolean deleteMissingChildren;
		private final boolean updateNullProperties;

		private SaveManyPropRequest(boolean insertedParent, BeanPropertyAssocMany<?> many, Object parentBean, PersistRequestBean<?> request) {
			this.insertedParent = insertedParent;
			this.many = many;
			this.cascade = many.getCascadeInfo().isSave();
			this.parentBean = parentBean;
			this.t = request.getTransaction();
			this.statelessUpdate = request.isStatelessUpdate();
			this.deleteMissingChildren = request.isDeleteMissingChildren();
			this.updateNullProperties = request.isUpdateNullProperties();
		}

		private SaveManyPropRequest(BeanPropertyAssocMany<?> many, Object parentBean, SpiTransaction t) {
			this.insertedParent = false;
			this.many = many;
			this.parentBean = parentBean;
			this.t = t;
			this.cascade = true;
			this.statelessUpdate = false;
			this.deleteMissingChildren = false;
			this.updateNullProperties = false;
		}

		private Object getValueUnderlying() {
			return many.getValueUnderlying(parentBean);
		}

		private boolean isModifyListenMode() {
			return ModifyListenMode.REMOVALS.equals(many.getModifyListenMode());
		}

		private boolean isStatelessUpdate() {
			return statelessUpdate;
		}

		private boolean isDeleteMissingChildren() {
			return deleteMissingChildren;
		}
		
		private boolean isUpdateNullProperties() {
        	return updateNullProperties;
        }

		private boolean isInsertedParent() {
			return insertedParent;
		}

		private BeanPropertyAssocMany<?> getMany() {
			return many;
		}

		private Object getParentBean() {
			return parentBean;
		}

		private SpiTransaction getTransaction() {
			return t;
		}

		private boolean isCascade() {
			return cascade;
		}
	}

	private void saveMany(SaveManyPropRequest saveMany) {

		if (saveMany.getMany().isManyToMany()) {
			// save the beans that are in the manyToMany
			if (saveMany.isCascade()) {
				// Need explicit Cascade to save the beans on other side
				saveAssocManyDetails(saveMany, false, saveMany.isUpdateNullProperties());
				// for ManyToMany save the 'relationship' via inserts/deletes
				// into/from the intersection table
				saveAssocManyIntersection(saveMany, saveMany.isDeleteMissingChildren());
			}

		} else {
			if (saveMany.isCascade()) {
				saveAssocManyDetails(saveMany, saveMany.isDeleteMissingChildren(), saveMany.isUpdateNullProperties());
			}
			if (saveMany.isModifyListenMode()) {
				removeAssocManyPrivateOwned(saveMany);
			}
		}
	}

	private void removeAssocManyPrivateOwned(SaveManyPropRequest saveMany) {

		Object details = saveMany.getValueUnderlying();

		// check that the list is not null and if it is a BeanCollection
		// check that is has been populated (don't trigger lazy loading)
		if (details instanceof BeanCollection<?>) {

			BeanCollection<?> c = (BeanCollection<?>) details;
			Set<?> modifyRemovals = c.getModifyRemovals();
			if (modifyRemovals != null && !modifyRemovals.isEmpty()) {

				SpiTransaction t = saveMany.getTransaction();
				// increase depth for batching order
				t.depth(+1);
				for (Object removedBean : modifyRemovals) {
					deleteRecurse(removedBean, t);
				}
				t.depth(-1);
			}
		}
	}

	/**
	 * Save the details from a OneToMany collection.
	 */
	private void saveAssocManyDetails(SaveManyPropRequest saveMany, boolean deleteMissingChildren, boolean updateNullProperties) {

		BeanPropertyAssocMany<?> prop = saveMany.getMany();

		Object details = saveMany.getValueUnderlying();

		// check that the list is not null and if it is a BeanCollection
		// check that is has been populated (don't trigger lazy loading)
		Collection<?> collection = getDetailsIterator(details);

		if (collection == null) {
			// nothing to do here
			return;
		}

		if (saveMany.isInsertedParent()) {
			// performance optimisation for large collections
			prop.getTargetDescriptor().preAllocateIds(collection.size());
		}

		BeanDescriptor<?> targetDescriptor = prop.getTargetDescriptor();
		ArrayList<Object> detailIds = null;
		if (deleteMissingChildren) {
			// collect the Id's (to exclude from deleteManyDetails)
			detailIds = new ArrayList<Object>();
		}

		// increase depth for batching order
		SpiTransaction t = saveMany.getTransaction();
		t.depth(+1);

		// if a map, then we get the key value and
		// set it to the appropriate property on the
		// detail bean before we save it
		boolean isMap = ManyType.JAVA_MAP.equals(prop.getManyType());
		Object parentBean = saveMany.getParentBean();
		Object mapKeyValue = null;

		boolean saveSkippable = prop.isSaveRecurseSkippable();
		boolean skipSavingThisBean = false;

		for (Object detailBean : collection) {
			if (isMap) {
				// its a map so need the key and value
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) detailBean;
				mapKeyValue = entry.getKey();
				detailBean = entry.getValue();
			}

			if (prop.isManyToMany()) {
				if (detailBean instanceof EntityBean) {
					skipSavingThisBean = ((EntityBean) detailBean)._ebean_getIntercept().isReference();
				}
			} else {
				// set the 'parent/master' bean to the detailBean as long
				// as we don't make it 'dirty' in doing so
				if (detailBean instanceof EntityBean) {
					EntityBeanIntercept ebi = ((EntityBean) detailBean)._ebean_getIntercept();
					if (ebi.isNewOrDirty()) {
						// set the parent bean to detailBean
						prop.setJoinValuesToChild(parentBean, detailBean, mapKeyValue);
					} else if (ebi.isReference()) {
						// we can skip this one
						skipSavingThisBean = true;

					} else {
						// unmodified so skip depending on prop.isSaveRecurseSkippable();
						skipSavingThisBean = saveSkippable;
					}
				} else {
					// set the parent bean to detailBean
					prop.setJoinValuesToChild(parentBean, detailBean, mapKeyValue);
				}
			}

			if (skipSavingThisBean) {
				// unmodified bean that does not recurse its save
				// so we can skip the save for this bean.
				// Reset skipSavingThisBean for the next detailBean
				skipSavingThisBean = false;

			} else if (!saveMany.isStatelessUpdate()) {
				// normal save recurse
				saveRecurse(detailBean, t, parentBean);

			} else {
				if (targetDescriptor.isStatelessUpdate(detailBean)) {
					// update based on the value of Version/Id properties
					// cascade update in stateless mode
					forceUpdate(detailBean, null, t, deleteMissingChildren, updateNullProperties);
				} else {
					// cascade insert
					forceInsert(detailBean, t);
				}
			}

			if (detailIds != null) {
				// remember the Id (other details not in the collection) will be removed
				Object id = targetDescriptor.getId(detailBean);
				if (!DmlUtil.isNullOrZero(id)) {
					detailIds.add(id);
				}
			}
		}

		if (detailIds != null) {
			deleteManyDetails(t, prop.getBeanDescriptor(), parentBean, prop, detailIds);
		}

		t.depth(-1);

	}

	public int deleteManyToManyAssociations(Object ownerBean, String propertyName, Transaction t) {

		BeanDescriptor<?> descriptor = beanDescriptorManager.getBeanDescriptor(ownerBean.getClass());
		BeanPropertyAssocMany<?> prop = (BeanPropertyAssocMany<?>) descriptor.getBeanProperty(propertyName);
		return deleteAssocManyIntersection(ownerBean, prop, t);
	}

	public void saveManyToManyAssociations(Object ownerBean, String propertyName, Transaction t) {

		BeanDescriptor<?> descriptor = beanDescriptorManager.getBeanDescriptor(ownerBean.getClass());
		BeanPropertyAssocMany<?> prop = (BeanPropertyAssocMany<?>) descriptor.getBeanProperty(propertyName);

		saveAssocManyIntersection(new SaveManyPropRequest(prop, ownerBean, (SpiTransaction) t), false);
	}

	public void saveAssociation(Object parentBean, String propertyName, Transaction t) {

		BeanDescriptor<?> descriptor = beanDescriptorManager.getBeanDescriptor(parentBean.getClass());
		SpiTransaction trans = (SpiTransaction) t;

		BeanProperty prop = descriptor.getBeanProperty(propertyName);
		if (prop == null) {
			String msg = "Could not find property [" + propertyName + "] on bean " + parentBean.getClass();
			throw new PersistenceException(msg);
		}

		if (prop instanceof BeanPropertyAssocMany<?>) {
			BeanPropertyAssocMany<?> manyProp = (BeanPropertyAssocMany<?>) prop;
			saveMany(new SaveManyPropRequest(manyProp, parentBean, (SpiTransaction) t));

		} else if (prop instanceof BeanPropertyAssocOne<?>) {
			BeanPropertyAssocOne<?> oneProp = (BeanPropertyAssocOne<?>) prop;
			Object assocBean = oneProp.getValue(parentBean);

			int depth = oneProp.isOneToOneExported() ? 1 : -1;
			int revertDepth = -1 * depth;

			trans.depth(depth);
			saveRecurse(assocBean, t, parentBean);
			trans.depth(revertDepth);

		} else {
			String msg = "Expecting [" + prop.getFullBeanName() + "] to be a OneToMany, OneToOne, ManyToOne or ManyToMany property?";
			throw new PersistenceException(msg);
		}

	}

	/**
	 * Save the additions and removals from a ManyToMany collection as inserts
	 * and deletes from the intersection table.
	 * <p>
	 * This is done via MapBeans.
	 * </p>
	 */
	private void saveAssocManyIntersection(SaveManyPropRequest saveManyPropRequest, boolean deleteMissingChildren) {

		BeanPropertyAssocMany<?> prop = saveManyPropRequest.getMany();
		Object value = prop.getValueUnderlying(saveManyPropRequest.getParentBean());
		if (value == null) {
			return;
		}

		SpiTransaction t = saveManyPropRequest.getTransaction();
		Collection<?> additions = null;
		Collection<?> deletions = null;

		boolean vanillaCollection = (value instanceof BeanCollection<?> == false);

		if (vanillaCollection || deleteMissingChildren) {
			// delete all intersection rows and then treat all
			// beans in the collection as additions
			deleteAssocManyIntersection(saveManyPropRequest.getParentBean(), prop, t);
		}

		if (saveManyPropRequest.isInsertedParent() || vanillaCollection || deleteMissingChildren) {
			// treat everything in the list/set/map as an intersection addition
			if (value instanceof Map<?, ?>) {
				additions = ((Map<?, ?>) value).values();
			} else if (value instanceof Collection<?>) {
				additions = (Collection<?>) value;
			} else {
				String msg = "Unhandled ManyToMany type " + value.getClass().getName() + " for " + prop.getFullBeanName();
				throw new PersistenceException(msg);
			}
			if (!vanillaCollection) {
				((BeanCollection<?>) value).modifyReset();
			}
		} else {
			// BeanCollection so get the additions/deletions
			BeanCollection<?> manyValue = (BeanCollection<?>) value;
			additions = manyValue.getModifyAdditions();
			deletions = manyValue.getModifyRemovals();
			// reset so the changes are only processed once
			manyValue.modifyReset();
		}

		t.depth(+1);

		if (additions != null && !additions.isEmpty()) {
			for (Object otherBean : additions) {
				// the object from the 'other' side of the ManyToMany
				if (deletions != null && deletions.remove(otherBean)) {
					String m = "Inserting and Deleting same object? " + otherBean;
					if (t.isLogSummary()) {
						t.logInternal(m);
					}
					logger.log(Level.WARNING, m);

				} else {
					if (!prop.hasImportedId(otherBean)) {
						String msg = "ManyToMany bean " + otherBean + " does not have an Id value.";
						throw new PersistenceException(msg);

					} else {
						// build a intersection row for 'insert'
						IntersectionRow intRow = prop.buildManyToManyMapBean(saveManyPropRequest.getParentBean(), otherBean);
						SqlUpdate sqlInsert = intRow.createInsert(server);
						executeSqlUpdate(sqlInsert, t);
					}
				}
			}
		}
		if (deletions != null && !deletions.isEmpty()) {
			for (Object otherDelete : deletions) {
				// the object from the 'other' side of the ManyToMany
				// build a intersection row for 'delete'
				IntersectionRow intRow = prop.buildManyToManyMapBean(saveManyPropRequest.getParentBean(), otherDelete);
				SqlUpdate sqlDelete = intRow.createDelete(server);
				executeSqlUpdate(sqlDelete, t);
			}
		}

		// decrease the depth back to what it was
		t.depth(-1);
	}

	private int deleteAssocManyIntersection(Object bean, BeanPropertyAssocMany<?> many, Transaction t) {

		// delete all intersection rows for this bean
		IntersectionRow intRow = many.buildManyToManyDeleteChildren(bean);
		SqlUpdate sqlDelete = intRow.createDeleteChildren(server);

		return executeSqlUpdate(sqlDelete, t);
	}

	/**
	 * Delete beans in any associated many.
	 * <p>
	 * This is called prior to deleting the parent bean.
	 * </p>
	 */
	private void deleteAssocMany(PersistRequestBean<?> request) {

		SpiTransaction t = request.getTransaction();
		t.depth(-1);

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		Object parentBean = request.getBean();

		BeanPropertyAssocOne<?>[] expOnes = desc.propertiesOneExportedDelete();
		if (expOnes.length > 0) {

			DeleteUnloadedForeignKeys unloaded = null;
			for (int i = 0; i < expOnes.length; i++) {
				BeanPropertyAssocOne<?> prop = expOnes[i];
				if (request.isLoadedProperty(prop)) {
					Object detailBean = prop.getValue(parentBean);
					if (detailBean != null) {
						deleteRecurse(detailBean, t);
					}
				} else {
					if (unloaded == null) {
						unloaded = new DeleteUnloadedForeignKeys(server, request);
					}
					unloaded.add(prop);
				}
			}
			if (unloaded != null) {
				unloaded.queryForeignKeys();
				unloaded.deleteCascade();
			}
		}

		// Many's with delete cascade
		BeanPropertyAssocMany<?>[] manys = desc.propertiesManyDelete();
		for (int i = 0; i < manys.length; i++) {
			if (manys[i].isManyToMany()) {
				// delete associated rows from intersection table
				deleteAssocManyIntersection(parentBean, manys[i], t);

			} else {

				if (ModifyListenMode.REMOVALS.equals(manys[i].getModifyListenMode())) {
					// PrivateOwned ...
					Object details = manys[i].getValueUnderlying(parentBean);
					if (details instanceof BeanCollection<?>) {
						Set<?> modifyRemovals = ((BeanCollection<?>) details).getModifyRemovals();
						if (modifyRemovals != null && !modifyRemovals.isEmpty()) {

							// delete the orphans that have been removed from the collection
							for (Object detailBean : modifyRemovals) {
								if (manys[i].hasId(detailBean)) {
									deleteRecurse(detailBean, t);
								}
							}
						}
					}
				}

				deleteManyDetails(t, desc, parentBean, manys[i], null);
			}
		}

		// restore the depth
		t.depth(+1);
	}

	/**
	 * Delete the 'many' detail beans for a given parent bean.
	 * <p>
	 * For stateless updates this deletes details beans that are no longer in
	 * the many - the excludeDetailIds holds the detail beans that are in the
	 * collection (and should not be deleted).
	 * </p>
	 */
	private void deleteManyDetails(SpiTransaction t, BeanDescriptor<?> desc, Object parentBean,
	        BeanPropertyAssocMany<?> many, ArrayList<Object> excludeDetailIds) {

		if (many.getCascadeInfo().isDelete()) {
			// cascade delete the beans in the collection
			BeanDescriptor<?> targetDesc = many.getTargetDescriptor();
			if (targetDesc.isDeleteRecurseSkippable() && !targetDesc.isUsingL2Cache()) {
				// Just delete all the children with one statement
				IntersectionRow intRow = many.buildManyDeleteChildren(parentBean, excludeDetailIds);
				SqlUpdate sqlDelete = intRow.createDelete(server);
				executeSqlUpdate(sqlDelete, t);

			} else {
				// Delete recurse using the Id values of the children
				Object parentId = desc.getId(parentBean);
				List<Object> idsByParentId = many.findIdsByParentId(parentId, null, t, excludeDetailIds);
				if (!idsByParentId.isEmpty()) {
					delete(targetDesc, null, idsByParentId, t);
				}
			}
		}
	}

	/**
	 * Save any associated one beans.
	 */
	private void saveAssocOne(PersistRequestBean<?> request) {

		BeanDescriptor<?> desc = request.getBeanDescriptor();

		// imported ones with save cascade
		BeanPropertyAssocOne<?>[] ones = desc.propertiesOneImportedSave();

		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne<?> prop = ones[i];

			// check for partial objects
			if (request.isLoadedProperty(prop)) {
				Object detailBean = prop.getValue(request.getBean());
				if (detailBean != null) {
					if (isReference(detailBean)) {
						// skip saving a reference
					} else if (request.isParent(detailBean)) {
						// skip saving the parent as already saved
					} else if (prop.isSaveRecurseSkippable(detailBean)) {
						// we can skip saving this bean

					} else {
						SpiTransaction t = request.getTransaction();
						t.depth(-1);
						saveRecurse(detailBean, t, null);
						t.depth(+1);
					}
				}
			}
		}
	}

	/**
	 * Return true if the bean is a reference.
	 */
	private boolean isReference(Object bean) {
		return (bean instanceof EntityBean) && ((EntityBean) bean)._ebean_getIntercept().isReference();
	}

	/**
	 * Support for loading any Imported Associated One properties that are not
	 * loaded but required for Delete cascade.
	 */
	private DeleteUnloadedForeignKeys getDeleteUnloadedForeignKeys(PersistRequestBean<?> request) {

		DeleteUnloadedForeignKeys fkeys = null;

		BeanPropertyAssocOne<?>[] ones = request.getBeanDescriptor().propertiesOneImportedDelete();
		for (int i = 0; i < ones.length; i++) {
			if (!request.isLoadedProperty(ones[i])) {
				// we have cascade Delete on a partially populated bean and
				// this property was not loaded (so we are going to have to fetch it)
				if (fkeys == null) {
					fkeys = new DeleteUnloadedForeignKeys(server, request);
				}
				fkeys.add(ones[i]);
			}
		}

		return fkeys;
	}

	/**
	 * Delete any associated one beans.
	 */
	private void deleteAssocOne(PersistRequestBean<?> request) {

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		BeanPropertyAssocOne<?>[] ones = desc.propertiesOneImportedDelete();

		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne<?> prop = ones[i];
			if (!request.isLoadedProperty(prop)) {
				// handled by DeleteUnloadedForeignKeys that was built
				// via getDeleteUnloadedForeignKeys();
			} else {
				Object detailBean = prop.getValue(request.getBean());
				if (detailBean != null && prop.hasId(detailBean)) {
					deleteRecurse(detailBean, request.getTransaction());
				}
			}
		}
	}

	/**
	 * Set Id Generated value for insert.
	 */
	private void setIdGenValue(PersistRequestBean<?> request) {

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		if (!desc.isUseIdGenerator()) {
			return;
		}

		BeanProperty idProp = desc.getSingleIdProperty();
		if (idProp == null || idProp.isEmbedded()) {
			// not supporting IdGeneration for concatenated or Embedded
			return;
		}

		Object bean = request.getBean();
		Object uid = idProp.getValue(bean);

		if (DmlUtil.isNullOrZero(uid)) {

			// generate the nextId and set it to the property
			Object nextId = desc.nextId(request.getTransaction());

			// cast the data type if required and set it
			desc.convertSetId(nextId, bean);
		}
	}

	/**
	 * Return the details of the collection or map taking care to avoid
	 * unnecessary fetching of the data.
	 */
	private Collection<?> getDetailsIterator(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BeanCollection<?>) {
			BeanCollection<?> bc = (BeanCollection<?>) o;
			if (!bc.isPopulated()) {
				return null;
			}
			return bc.getActualDetails();
		}

		if (o instanceof Map<?, ?>) {
			// yes, we want the entrySet (to set the keys)
			return ((Map<?, ?>) o).entrySet();

		} else if (o instanceof Collection<?>) {
			return ((Collection<?>) o);
		}
		String m = "expecting a Map or Collection but got [" + o.getClass().getName() + "]";
		throw new PersistenceException(m);
	}

	/**
	 * Create the Persist Request Object that wraps all the objects used to
	 * perform an insert, update or delete.
	 */
	@SuppressWarnings("unchecked")
	private <T> PersistRequestBean<T> createRequest(T bean, Transaction t, Object parentBean) {
		BeanManager<T> mgr = getBeanManager(bean);
		if (mgr == null) {
			throw new PersistenceException(errNotRegistered(bean.getClass()));
		}
		return (PersistRequestBean<T>) createRequest(bean, t, parentBean, mgr);
	}

	private String errNotRegistered(Class<?> beanClass) {
		String msg = "The type [" + beanClass + "] is not a registered entity?";
		msg += " If you don't explicitly list the entity classes to use Ebean will search for them in the classpath.";
		msg += " If the entity is in a Jar check the ebean.search.jars property in ebean.properties file or check ServerConfig.addJar().";
		return msg;
	}

	/**
	 * Create the Persist Request Object that wraps all the objects used to
	 * perform an insert, update or delete.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private PersistRequestBean<?> createRequest(Object bean, Transaction t, Object parentBean, BeanManager<?> mgr) {

		if (mgr.isLdapEntityType()) {
			return new LdapPersistBeanRequest(server, bean, parentBean, mgr, ldapPersister);
		}
		return new PersistRequestBean(server, bean, parentBean, mgr, (SpiTransaction) t, persistExecute);
	}

	/**
	 * Return the BeanDescriptor for a bean that is being persisted.
	 * <p>
	 * Note that this checks to see if the bean is a MapBean with a tableName.
	 * If so it will return the table based BeanDescriptor.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	private <T> BeanManager<T> getBeanManager(T bean) {

		return (BeanManager<T>) beanDescriptorManager.getBeanManager(bean.getClass());
	}
}
