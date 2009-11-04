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
package com.avaje.ebean.server.persist;

import java.util.Collection;
import java.util.Iterator;
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
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.BeanCollection.ModifyListenMode;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.internal.SpiUpdate;
import com.avaje.ebean.server.core.Message;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.core.PersistRequestCallableSql;
import com.avaje.ebean.server.core.PersistRequestOrmUpdate;
import com.avaje.ebean.server.core.PersistRequestUpdateSql;
import com.avaje.ebean.server.core.Persister;
import com.avaje.ebean.server.core.PstmtBatch;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.IntersectionRow;
import com.avaje.ebean.server.jmx.MAdminLogging;

/**
 * Persister implementation using DML.
 * <p>
 * This object uses DmlPersistExecute to perform the actual persist execution.
 * </p>
 * <p>
 * This object:
 * <ul>
 * <li>Determines insert or update for saved beans </li>
 * <li>Determines the concurrency mode</li>
 * <li>Handles cascading of save and delete</li>
 * <li>Handles the batching and queueing</li>
 * </p>
 * 
 * @see com.avaje.ebean.server.persist.DefaultPersistExecute
 */
public final class DefaultPersister implements Persister {

	private static final Logger logger = Logger.getLogger(DefaultPersister.class.getName());

	/**
	 * Actually does the persisting work.
	 */
	private final PersistExecute persistExecute;

	private final boolean validation;

	private final SpiEbeanServer server;

	private final BeanDescriptorManager beanDescriptorManager;
	
	public DefaultPersister(SpiEbeanServer server, boolean validate, MAdminLogging logControl, 
			Binder binder, BeanDescriptorManager descMgr, PstmtBatch pstmtBatch) {

		this.server = server;
		this.beanDescriptorManager = descMgr;
		
		this.persistExecute = new DefaultPersistExecute(logControl, binder, pstmtBatch);
		this.validation = validate;
	}

	/**
	 * Execute the CallableSql.
	 */
	public int executeCallable(CallableSql callSql, Transaction t) {

		PersistRequestCallableSql request = new PersistRequestCallableSql(server, callSql,
				(SpiTransaction) t, persistExecute);
		try {
			return request.executeOrQueue();

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
		
		if (mgr == null){
			String msg = "No BeanManager found for type ["+ormUpdate.getBeanType()+"]. Is it an entity?";
			throw new PersistenceException(msg);
		}

		PersistRequestOrmUpdate request = new PersistRequestOrmUpdate(server, mgr, ormUpdate, (SpiTransaction) t, persistExecute);
		try {
			return request.executeOrQueue();

		} catch (RuntimeException e) {
			request.rollbackTransIfRequired();
			throw e;
		}
	}

	/**
	 * Execute the updateSql.
	 */
	public int executeSqlUpdate(SqlUpdate updSql, Transaction t) {

		PersistRequestUpdateSql request = new PersistRequestUpdateSql(server, updSql,
				(SpiTransaction) t, persistExecute);
		try {
			return request.executeOrQueue();

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

	public void save(Object bean, Transaction t) {
		saveRecurse(bean, t, null);
	}

	private void saveRecurse(Object bean, Transaction t, Object parentBean) {
		if (bean == null) {
			throw new NullPointerException(Message.msg("bean.isnull"));
		}
	
		PersistRequestBean<?> req = createPersistRequest(bean, t, parentBean);
		try {
			req.initTransIfRequired();
			save(req);
			req.commitTransIfRequired();

		} catch (RuntimeException ex) {
			req.rollbackTransIfRequired();
			throw ex;
		}
	}

	/**
	 * Insert or update the bean depending on PersistControl and the bean state.
	 */
	private void save(PersistRequestBean<?> request) {

		EntityBeanIntercept intercept = request.getEntityBeanIntercept();
		if (intercept != null) {
			if (intercept.isReference()) {
				// its a reference...
				if (request.isPersistCascade()) {
					// save any associated List held beans
					intercept.setLoaded();
					saveAssocMany(false, request, false);
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

		} else {
			saveVanilla(request);
		}
	}

	/**
	 * Determine if this is an Insert or update for the 'vanilla' bean.
	 */
	private void saveVanilla(PersistRequestBean<?> request) {

		// use the version property to determine insert or update
		// if it is available on this bean

		BeanDescriptor<?> desc = request.getBeanDescriptor();
		Object bean = request.getBean();

		BeanProperty versProp = desc.firstVersionProperty();
		if (versProp == null) {
			// no version property - assume insert 
			insert(request);

		} else {
			// use version property to determine insert or update
			Object value = versProp.getValue(bean);
			if (DmlUtil.isNullOrZero(value)) {
				insert(request);
			} else {
				update(request);
			}
		}
	}
	
	/**
	 * Insert the bean.
	 */
	private void insert(PersistRequestBean<?> request) {

		request.setType(PersistRequest.Type.INSERT);

		if (request.isPersistCascade()) {
			// save associated One beans recursively first
			saveAssocOne(request);
		}

		// set the IDGenerated value if required
		setIdGenValue(request);
		if (validation) {
			// run the local validation rules
			request.validate();
		}
		request.executeOrQueue();

		if (request.isPersistCascade()) {
			// save any associated List held beans
			saveAssocMany(true, request, true);
		}
	}

	/**
	 * Update the bean. Return NOT_SAVED if the bean values have not changed.
	 */
	private void update(PersistRequestBean<?> request) {

		// we have determined that it is an update
		request.setType(PersistRequest.Type.UPDATE);

		if (request.isPersistCascade()) {
			// save associated One beans recursively first
			saveAssocOne(request);
		}

		if (request.isDirty()) {
			// run the local validation rules
			if (validation) {
				request.validate();
			}
			request.executeOrQueue();

		} else {
			// skip validation on unchanged bean
			if (logger.isLoggable(Level.FINE)) {
				logger.fine(Message.msg("persist.update.skipped", request.getBean()));
			}
		}

		if (request.isPersistCascade()) {
			// save all the beans in assocMany's after
			saveAssocMany(false, request, true);
		}
	}

	/**
	 * Delete the bean with the explicit transaction.
	 */
	public void delete(Object bean, Transaction t) {
		
		PersistRequestBean<?> req = createPersistRequest(bean, t, null);
		if (req.isRegistered()){
			// skip deleting bean. Used where cascade is on
			// both sides of a relationship
			if (logger.isLoggable(Level.FINE)){
				logger.fine("skipping delete on alreadyRegistered "+bean);
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

	/**
	 * Delete the bean.
	 * <p>
	 * Note that preDelete fires before the deletion of children.
	 * </p>
	 */
	private void delete(PersistRequestBean<?> request) {

		if (request.isPersistCascade()) {
			// delete children first ... register the
			// bean to handle bi-directional cascading
			request.registerBean();
			deleteAssocMany(request);
			request.unregisterBean();
		}

		request.executeOrQueue();

		if (request.isPersistCascade()) {
			deleteAssocOne(request);
		}
	}

	/**
	 * Save the associated child beans contained in a List.
	 * <p>
	 * This will automatically copy over any join properties from the parent
	 * bean to the child beans.
	 * </p>
	 */
	private void saveAssocMany(boolean insertedParent, PersistRequestBean<?> request, boolean includeExportedOnes) {

		Object parentBean = request.getBean();
		BeanDescriptor<?> desc = request.getBeanDescriptor();

		// exported ones with cascade save
		BeanPropertyAssocOne<?>[] expOnes = desc.propertiesOneExportedSave();
		for (int i = 0; i < expOnes.length; i++) {
			BeanPropertyAssocOne<?> prop = expOnes[i];

			// check for partial beans
			if (request.isLoadedProperty(prop)) {
				Object detailBean = prop.getValue(request.getBean());
				if (detailBean != null) {
					if (prop.isSaveRecurseSkippable(detailBean)) {
						// skip saving this bean
					} else {
						SpiTransaction t = request.getTransaction();
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
			if (manys[i].isManyToMany()) {
				// save the beans that are in the manyToMany
				if (manys[i].getCascadeInfo().isSave()){
					// Need explicit Cascade to save the beans on other side 
					saveAssocManyDetails(insertedParent, request, manys[i], false);
				}
				// for ManyToMany always save the 'relationship' via inserts/deletes
				// into/from the intersection table
				saveAssocManyIntersection(insertedParent, request, manys[i]);

			} else {
				if (manys[i].getCascadeInfo().isSave()){
					saveAssocManyDetails(insertedParent, request, manys[i], true);
				}
				if (ModifyListenMode.REMOVALS.equals(manys[i].getModifyListenMode())) {
					removeAssocManyPrivateOwned(request, manys[i]);
				}
			}
		}
	}

	private void removeAssocManyPrivateOwned(PersistRequestBean<?> request, BeanPropertyAssocMany<?> prop) {

		Object parentBean = request.getBean();
		Object details = prop.getValue(parentBean);

		// check that the list is not null and if it is a BeanCollection
		// check that is has been populated (don't trigger lazy loading)
		if (details instanceof BeanCollection<?>) {
			BeanCollection<?> c = (BeanCollection<?>)details;
			Set<?> modifyRemovals = c.getModifyRemovals();
			if (modifyRemovals != null && !modifyRemovals.isEmpty()) {
				// increase depth for batching order
				SpiTransaction t = request.getTransaction();
				t.depth(+1);
				
				Iterator<?> it = modifyRemovals.iterator();
				while (it.hasNext()) {
					Object removedBean = it.next();
					deleteRecurse(removedBean, t);
				}
				
				t.depth(-1);
			}
		}
	}
	
	/**
	 * Save the details from a OneToMany collection.
	 */
	private void saveAssocManyDetails(boolean insertedParent, PersistRequestBean<?> request, 
			BeanPropertyAssocMany<?> prop, boolean oneToMany) {

		Object parentBean = request.getBean();
		Object details = prop.getValue(parentBean);

		// check that the list is not null and if it is a BeanCollection
		// check that is has been populated (don't trigger lazy loading)
		Collection<?> collection = getDetailsIterator(details);

		if (collection != null) {

			if (insertedParent){
				// performance optimisation for large collections
				prop.getTargetDescriptor().preAllocateIds(collection.size());
			}
			
			// increase depth for batching order
			SpiTransaction t = request.getTransaction();
			t.depth(+1);

			// if a map, then we get the key value and
			// set it to the appropriate property on the
			// detail bean before we save it
			boolean isMap = Query.Type.MAP.equals(prop.getManyType());
			Object mapKeyValue = null;
			boolean saveSkippable = prop.isSaveRecurseSkippable();
			boolean skipSavingThisBean = false;

			Iterator<?> detailIt = collection.iterator();
			while (detailIt.hasNext()) {
				Object detailBean = detailIt.next();
				if (isMap) {
					// its a map so need the key and value
					Map.Entry<?, ?> entry = (Map.Entry<?, ?>) detailBean;
					mapKeyValue = entry.getKey();
					detailBean = entry.getValue();
				}
				if (oneToMany) {
					// set the 'parent/master' bean to the detailBean as long
					// as we don't make it 'dirty' in doing so
					if (detailBean instanceof EntityBean) {
						if (((EntityBean) detailBean)._ebean_getIntercept().isNewOrDirty()) {
							// set the parent bean to detailBean
							prop.setJoinValuesToChild(request, parentBean, detailBean, mapKeyValue);
						} else {
							// unmodified so potentially can skip
							// depending on prop.isSaveRecurseSkippable();
							skipSavingThisBean = saveSkippable;
						}
					} else {
						// set the parent bean to detailBean
						prop.setJoinValuesToChild(request, parentBean, detailBean, mapKeyValue);
					}
				}

				if (skipSavingThisBean) {
					// 1. unmodified bean that does not recurse its save
					// so we can skip the save for this bean.
					// 2. Reset skipSavingThisBean for the next detailBean
					skipSavingThisBean = false;
				} else {
					saveRecurse(detailBean, t, parentBean);
				}
			}
			t.depth(-1);
		}
	}

	/**
	 * Save the additions and removals from a ManyToMany collection as inserts
	 * and deletes from the intersection table.
	 * <p>
	 * This is done via MapBeans.
	 * </p>
	 */
	private void saveAssocManyIntersection(boolean insertedParent, PersistRequestBean<?> request, BeanPropertyAssocMany<?> prop) {

		Object parentBean = request.getBean();
		Object value = prop.getValue(parentBean);
		if (value == null) {
			return;
		}
		
		Collection<?> additions = null;
		Collection<?> deletions = null;
		
		boolean vanillaCollection = (value instanceof BeanCollection<?> == false);
		
		if (vanillaCollection || insertedParent){
			// treat everything in the list/set/map as an intersection addition  
			if (value instanceof Map<?,?>){
				additions = ((Map<?,?>)value).values();
			} else if (value instanceof Collection<?>) {
				additions = (Collection<?>)value;
			} else {
				String msg = "Unhandled ManyToMany type "+value.getClass().getName()+" for "+prop.getFullBeanName();
				throw new PersistenceException(msg);
			}
		} else {
			// BeanCollection so get the additions/deletions
			BeanCollection<?> manyValue = (BeanCollection<?>) value;
			additions = manyValue.getModifyAdditions();
			deletions = manyValue.getModifyRemovals();
			// reset so the changes are only processed once
			manyValue.modifyReset();
		}

		SpiTransaction t = request.getTransaction();
		t.depth(+1);

		if (additions != null && !additions.isEmpty()) {
			Iterator<?> it = additions.iterator();
			while (it.hasNext()) {
				// the object from the 'other' side of the ManyToMany
				Object otherBean = it.next();
				if (deletions != null && deletions.remove(otherBean)) {
					String m = "Inserting and Deleting same object? " + otherBean;
					t.log(m);
					logger.log(Level.SEVERE, m);

				} else {
					// build a intersection row for 'insert'
					IntersectionRow intRow = prop.buildManyToManyMapBean(parentBean, otherBean);
					SqlUpdate sqlInsert = intRow.createInsert(server);
					t.log(sqlInsert.getSql());
					sqlInsert.execute();
				}
			}
		}
		if (deletions != null && !deletions.isEmpty()) {
			Iterator<?> it = deletions.iterator();
			while (it.hasNext()) {
				// the object from the 'other' side of the ManyToMany
				Object otherDelete = it.next();

				// build a intersection row for 'delete'
				IntersectionRow intRow = prop.buildManyToManyMapBean(parentBean, otherDelete);
				SqlUpdate sqlDelete = intRow.createDelete(server);
				t.log(sqlDelete.getSql());
				sqlDelete.execute();
			}
		}

		// decrease the depth back to what it was
		t.depth(-1);
	}

	/**
	 * Delete beans in any associated many.
	 * <p>
	 * This is called prior to deleting the parent bean.
	 * </p>
	 */
	private void deleteAssocMany(PersistRequestBean<?> request) {

		SpiTransaction t = request.getTransaction();
		BeanDescriptor<?> desc = request.getBeanDescriptor();

		Object parentBean = request.getBean();

		// exported assoc ones with delete cascade
		BeanPropertyAssocOne<?>[] expOnes = desc.propertiesOneExportedDelete();
		for (int i = 0; i < expOnes.length; i++) {
			BeanPropertyAssocOne<?> prop = expOnes[i];

			// check for partial object
			if (request.isLoadedProperty(prop)) {
				Object detailBean = prop.getValue(parentBean);
				if (detailBean != null) {
					t.depth(-1);
					deleteRecurse(detailBean, t);
					t.depth(+1);
				}
			}
		}

		// Many's with delete cascade
		BeanPropertyAssocMany<?>[] manys = desc.propertiesManyDelete();
		for (int i = 0; i < manys.length; i++) {

			// no need to check partial object with assoc many's
			// as they are left null or lazy loading detected
			// in getDetailsIterator().

			if (manys[i].isManyToMany()) {
				// ManyToMany delete cascade - a bit dangerous?
				String m = "ManyToMany delete cascade not implemented!";
				throw new RuntimeException(m);

			} else {
				Object details = manys[i].getValue(parentBean);
				
				if (ModifyListenMode.REMOVALS.equals(manys[i].getModifyListenMode())) {
					// PrivateOwned ...
					if (details instanceof BeanCollection<?>){
						BeanCollection<?> bc = (BeanCollection<?>)details;
						Set<?> modifyRemovals = bc.getModifyRemovals();
						if (modifyRemovals != null && !modifyRemovals.isEmpty()){
							// delete the orphans that have been removed from the collection
							t.depth(-1);

							Iterator<?> it = modifyRemovals.iterator();
							while (it.hasNext()) {
								Object detailBean = it.next();
								if (manys[i].hasId(detailBean)) {
									deleteRecurse(detailBean, t);
								} else {
									// bean that had not been inserted
								}
							}
							t.depth(+1);
						}
					}
				}
				
				// cascade delete the beans in the collection
				Collection<?> collection = getDetailsIterator(details);
				 
				if (collection != null) {
					// decrease depth for batched processing
					// lowest depth executes first
					t.depth(-1);

					Iterator<?> it = collection.iterator();
					while (it.hasNext()) {
						Object detailBean = it.next();
						if (manys[i].hasId(detailBean)) {
							deleteRecurse(detailBean, t);
						} else {
							// bean that had not been inserted
						}
					}
					t.depth(+1);
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
					if (request.isParent(detailBean)) {
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
	 * Delete any associated one beans.
	 */
	private void deleteAssocOne(PersistRequestBean<?> request) {

		BeanDescriptor<?> desc = request.getBeanDescriptor();

		// imported assoc ones with cascade delete
		BeanPropertyAssocOne<?>[] ones = desc.propertiesOneImportedDelete();
		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne<?> prop = ones[i];

			// TODO: Review this treatment for references and partial objects
			// as a cascade delete can potentially get skipped here.
			
			// check for partial object
			if (request.isLoadedProperty(prop)) {

				Object detailBean = prop.getValue(request.getBean());

				// if bean exists with a unique id then delete it
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

		// assumed that the Id is set to the unique property and
		// there is only one in this case
		BeanProperty idProp = desc.getSingleIdProperty();

		if (idProp == null || idProp.isEmbedded()) {
			// not supporting IdGeneration for concatenated or Embedded
			return;
		}

		Object bean = request.getBean();
		Object uid = idProp.getValue(bean);

		if (uid != null) {
			// the value is not null, ignore

		} else {
			// generate the nextId and set it to the property
			Object nextId = desc.nextId();

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

		if (o instanceof Map<?,?>) {
			// yes, we want the entrySet (to set the keys)
			return ((Map<?, ?>) o).entrySet();

		} else if (o instanceof Collection<?>) {
			return ((Collection<?>) o);
		}
		String m = "expecting a Map or Collection but got [" + o.getClass().getName() + "]";
		throw new PersistenceException(m);
	}

	/**
	 * Create the BeanPersistRequest that wraps all the objects used to perform
	 * an insert, update or delete.
	 */
	private <T> PersistRequestBean<T> createPersistRequest(T bean, Transaction t, Object parentBean) {

		BeanManager<T> mgr = getPersistDescriptor(bean);
		if (mgr == null){
			String msg = "No BeanManager found for type ["+bean.getClass()+"]. Is it a registered entity?";
			throw new PersistenceException(msg);
		}

		return new PersistRequestBean<T>(server, bean, parentBean, mgr, (SpiTransaction) t,persistExecute);
	}

	/**
	 * Return the BeanDescriptor for a bean that is being persisted.
	 * <p>
	 * Note that this checks to see if the bean is a MapBean with a tableName.
	 * If so it will return the table based BeanDescriptor.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	private <T> BeanManager<T> getPersistDescriptor(T bean) {
		
		return (BeanManager<T>) beanDescriptorManager.getBeanManager(bean.getClass());
	}
}
