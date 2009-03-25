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
import com.avaje.ebean.MapBean;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.Update;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.query.OrmUpdate;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.core.PersistRequestCallableSql;
import com.avaje.ebean.server.core.PersistRequestOrmUpdate;
import com.avaje.ebean.server.core.PersistRequestUpdateSql;
import com.avaje.ebean.server.core.Persister;
import com.avaje.ebean.server.core.ServerTransaction;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DeploymentManager;
import com.avaje.ebean.server.idgen.IdGeneratorManager;
import com.avaje.ebean.server.plugin.Plugin;
import com.avaje.ebean.server.plugin.PluginCore;
import com.avaje.ebean.server.plugin.PluginProperties;
import com.avaje.ebean.util.Message;
import com.avaje.lib.log.LogFactory;

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
public final class DefaultPersister implements Persister, ConcurrencyMode {

	private static final Logger logger = LogFactory.get(DefaultPersister.class);

	/**
	 * For Version columns based on int.
	 */
	private static Integer ZERO_INT = Integer.valueOf(0);

	/**
	 * Actually does the persisting work.
	 */
	private final PersistExecute persistExecute;

	private final Plugin plugin;

	private final boolean validation;

	private final InternalEbeanServer server;

	private final DeploymentManager deploymentManager;

	final IdGeneratorManager idGeneratorManager;
	
	public DefaultPersister(Plugin plugin, InternalEbeanServer server) {

		PluginCore pluginCore = plugin.getPluginCore();

		this.plugin = plugin;
		this.server = server;
		this.deploymentManager = pluginCore.getDeploymentManager();
		this.persistExecute = new DefaultPersistExecute(pluginCore.getDbConfig());

		this.idGeneratorManager = plugin.createIdGeneratorManager(server);
		PluginProperties props = pluginCore.getDbConfig().getProperties();

		validation = props.getPropertyBoolean("validation", true);
	}

	public Object nextId(BeanDescriptor desc) {
		return idGeneratorManager.nextId(desc);
	}

	/**
	 * Execute the CallableSql.
	 */
	public int executeCallable(CallableSql callSql, Transaction t) {

		PersistRequestCallableSql request = new PersistRequestCallableSql(server, callSql,
				(ServerTransaction) t, persistExecute);
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

		PersistRequestOrmUpdate request = new PersistRequestOrmUpdate(server,
				(OrmUpdate<?>) update, (ServerTransaction) t, persistExecute);
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
				(ServerTransaction) t, persistExecute);
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
		PersistRequest req = createPersistRequest(bean, t, parentBean);
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
	private void save(PersistRequest request) {

		EntityBeanIntercept intercept = request.getEntityBeanIntercept();
		if (intercept != null) {
			if (intercept.isReference()) {
				// its a reference...
				if (request.isPersistCascade()) {
					// save any associated List held beans
					intercept.setLoaded();
					saveAssocMany(request, false);
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
	private void saveVanilla(PersistRequest request) {

		// use the version property to determine insert or update
		// if it is available on this bean

		BeanDescriptor desc = request.getBeanDescriptor();
		Object bean = request.getBean();

		BeanProperty versProp = desc.firstVersionProperty();
		if (versProp == null) {
			// no direct version property
			insert(request);

		} else {
			Object value = versProp.getValue(bean);
			if (value == null || ZERO_INT.equals(value)) {
				insert(request);
			} else {
				update(request);
			}
		}
	}
	
	/**
	 * Insert the bean.
	 */
	private void insert(PersistRequest request) {

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
			saveAssocMany(request, true);
		}
	}

	/**
	 * Update the bean. Return NOT_SAVED if the bean values have not changed.
	 */
	private void update(PersistRequest request) {

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
			if (plugin.getDebugLevel() > 0) {
				logger.fine(Message.msg("persist.update.skipped", request.getBean()));
			}
		}

		if (request.isPersistCascade()) {
			// save all the beans in assocMany's after
			saveAssocMany(request, true);
		}
	}

	/**
	 * Delete the bean with the explicit transaction.
	 */
	public void delete(Object bean, Transaction t) {

		PersistRequest req = createPersistRequest(bean, t, null);

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
	private void delete(PersistRequest request) {

		if (request.isPersistCascade()) {
			// delete children first
			deleteAssocMany(request);
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
	private void saveAssocMany(PersistRequest request, boolean includeExportedOnes) {

		Object parentBean = request.getBean();
		BeanDescriptor desc = request.getBeanDescriptor();

		// exported ones with cascade save
		BeanPropertyAssocOne[] expOnes = desc.propertiesOneExportedSave();
		for (int i = 0; i < expOnes.length; i++) {
			BeanPropertyAssocOne prop = expOnes[i];

			// check for partial beans
			if (request.isIncludeProperty(prop)) {
				Object detailBean = prop.getValue(request.getBean());
				if (detailBean != null) {
					if (prop.isSaveRecurseSkippable(detailBean)) {
						// skip saving this bean
					} else {
						ServerTransaction t = request.getTransaction();
						t.depth(+1);
						saveRecurse(detailBean, t, parentBean);
						t.depth(-1);
					}
				}

			}
		}

		// many's with cascade save
		BeanPropertyAssocMany[] manys = desc.propertiesManySave();
		for (int i = 0; i < manys.length; i++) {
			if (manys[i].isManyToMany()) {
				// save the beans that are in the manyToMany
				saveAssocManyDetails(request, manys[i], false);
				// create inserts and deletes into the intersection table
				saveAssocManyIntersection(request, manys[i]);

			} else {
				saveAssocManyDetails(request, manys[i], true);
			}
		}
	}

	/**
	 * Save the details from a OneToMany collection.
	 */
	private void saveAssocManyDetails(PersistRequest request, BeanPropertyAssocMany prop,
			boolean oneToMany) {

		Object parentBean = request.getBean();
		Object details = prop.getValue(parentBean);

		// check that the list is not null and if it is a BeanCollection
		// check that is has been populated (don't trigger lazy loading)
		Iterator<?> detailIt = getDetailsIterator(details);

		if (detailIt != null) {

			// increase depth for batching order
			ServerTransaction t = request.getTransaction();
			t.depth(+1);

			// if a map, then we get the key value and
			// set it to the appropriate property on the
			// detail bean before we save it
			boolean isMap = prop.getManyType().isMap();
			Object mapKeyValue = null;
			boolean saveSkippable = prop.isSaveRecurseSkippable();
			boolean skipSavingThisBean = false;

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
						if (((EntityBean) detailBean)._ebean_getIntercept().isDirty()) {
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
	private void saveAssocManyIntersection(PersistRequest request, BeanPropertyAssocMany prop) {

		Object parentBean = request.getBean();
		Object value = prop.getValue(parentBean);
		if (value == null) {
			return;
		}
		if (value instanceof BeanCollection == false) {
			// we can't determine the additions and removals
			String m = "Save cascade on ManyToMany [" + prop.getName() + "].";
			m += " The collection [" + value.getClass().toString() + "]was not a BeanCollection.";
			m += " The additions and removals can not be determined and";
			m += " *NO* inserts or deletes to the intersection table occured.";
			request.getTransaction().log(m);
			logger.warning(m);
			return;
		}

		BeanCollection<?> manyValue = (BeanCollection<?>) value;
		Set<?> additions = manyValue.getModifyAdditions();
		Set<?> deletions = manyValue.getModifyRemovals();
		if (additions == null && deletions == null) {
			return;
		}

		ServerTransaction t = request.getTransaction();
		t.depth(+1);

		if (additions != null) {
			Iterator<?> it = additions.iterator();
			while (it.hasNext()) {
				// the object from the 'other' side of the ManyToMany
				Object otherBean = it.next();
				if (deletions != null && deletions.remove(otherBean)) {
					String m = "Inserting and Deleting same object? " + otherBean;
					t.log(m);
					logger.log(Level.SEVERE, m);

				} else {
					// build a MapBean for the intersection table and 'insert'
					Object insertBean = prop.buildManyToManyMapBean(parentBean, otherBean);
					saveRecurse(insertBean, t, parentBean);
				}
			}
		}
		if (deletions != null) {
			Iterator<?> it = deletions.iterator();
			while (it.hasNext()) {
				// the object from the 'other' side of the ManyToMany
				Object otherDelete = it.next();

				// build a MapBean for the intersection table and 'delete'
				Object deleteBean = prop.buildManyToManyMapBean(parentBean, otherDelete);
				deleteRecurse(deleteBean, t);
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
	private void deleteAssocMany(PersistRequest request) {

		BeanDescriptor desc = request.getBeanDescriptor();

		Object parentBean = request.getBean();

		// exported assoc ones with delete cascade
		BeanPropertyAssocOne[] expOnes = desc.propertiesOneExportedDelete();
		for (int i = 0; i < expOnes.length; i++) {
			BeanPropertyAssocOne prop = expOnes[i];

			// check for partial object
			if (request.isIncludeProperty(prop)) {
				Object detailBean = prop.getValue(parentBean);
				if (detailBean != null) {
					ServerTransaction t = request.getTransaction();
					t.depth(-1);
					deleteRecurse(detailBean, t);
					t.depth(+1);
				}
			}
		}

		// Many's with delete cascade
		BeanPropertyAssocMany[] manys = desc.propertiesManyDelete();
		for (int i = 0; i < manys.length; i++) {

			// no need to check partial object with assoc many's
			// as they are left null or lazy loading detected
			// in getDetailsIterator().

			if (manys[i].isManyToMany()) {
				// ManyToMany delete cascade - a bit dangerous?
				String m = "ManyToMany delete cascade not implemented!";
				throw new RuntimeException(m);

			} else {
				// cascade the delete
				Object details = manys[i].getValue(parentBean);

				Iterator<?> it = getDetailsIterator(details);
				if (it != null) {
					// decrease depth for batched processing
					// lowest depth executes first
					ServerTransaction t = request.getTransaction();
					t.depth(-1);

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
	private void saveAssocOne(PersistRequest request) {

		BeanDescriptor desc = request.getBeanDescriptor();

		// imported ones with save cascade
		BeanPropertyAssocOne[] ones = desc.propertiesOneImportedSave();

		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne prop = ones[i];

			// check for partial objects
			if (request.isIncludeProperty(prop)) {
				Object detailBean = prop.getValue(request.getBean());
				if (detailBean != null) {
					if (prop.isSaveRecurseSkippable(detailBean)) {
						// we can skip saving this bean
					} else {
						ServerTransaction t = request.getTransaction();
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
	private void deleteAssocOne(PersistRequest request) {

		BeanDescriptor desc = request.getBeanDescriptor();

		// imported assoc ones with cascade delete
		BeanPropertyAssocOne[] ones = desc.propertiesOneImportedDelete();
		for (int i = 0; i < ones.length; i++) {
			BeanPropertyAssocOne prop = ones[i];

			// check for partial object
			if (request.isIncludeProperty(prop)) {

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
	private void setIdGenValue(PersistRequest request) {

		BeanDescriptor desc = request.getBeanDescriptor();
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
			Object nextId = idGeneratorManager.nextId(desc);

			// cast the data type if required and set it
			desc.convertSetId(nextId, bean);
		}
	}

	/**
	 * Return the details of the collection or map taking care to avoid
	 * unnecessary fetching of the data.
	 */
	private Iterator<?> getDetailsIterator(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof BeanCollection) {
			BeanCollection<?> bc = (BeanCollection<?>) o;
			if (!bc.isPopulated()) {
				return null;
			}
			return bc.getActualDetails();
		}

		if (o instanceof Map) {
			// yes, we want the entrySet (to set the keys)
			return ((Map<?, ?>) o).entrySet().iterator();

		} else if (o instanceof Collection) {
			return ((Collection<?>) o).iterator();

		}
		String m = "expecting a Map or Collection but got [" + o.getClass().getName() + "]";
		throw new PersistenceException(m);
	}

	/**
	 * Create the BeanPersistRequest that wraps all the objects used to perform
	 * an insert, update or delete.
	 */
	private PersistRequestBean createPersistRequest(Object bean, Transaction t, Object parentBean) {

		BeanManager mgr = getPersistDescriptor(bean);
		if (mgr == null){
			String msg = "No BeanManager found for type ["+bean.getClass()+"]. Is it an entity?";
			throw new PersistenceException(msg);
		}

		return new PersistRequestBean(server, bean, parentBean, mgr, (ServerTransaction) t,persistExecute);
	}

	/**
	 * Return the BeanDescriptor for a bean that is being persisted.
	 * <p>
	 * Note that this checks to see if the bean is a MapBean with a tableName.
	 * If so it will return the table based BeanDescriptor.
	 * </p>
	 */
	private BeanManager getPersistDescriptor(Object bean) {
		if (bean instanceof MapBean) {
			MapBean mapBean = (MapBean) bean;
			String tableName = mapBean.getTableName();
			if (tableName != null) {
				BeanManager mgr = deploymentManager.getMapBeanManager(tableName);
				if (mgr == null) {
					String m = "Could not find MapBean descriptor for table [" + tableName + "]";
					throw new PersistenceException(m);
				} else {
					return mgr;
				}

			} else {
				// assuming this is a normal bean that extends MapBean
				if (bean.getClass().equals(MapBean.class)) {
					throw new NullPointerException(Message.msg("tablebean.notable"));
				}
			}
		}
		return deploymentManager.getBeanManager(bean.getClass());
	}
}
