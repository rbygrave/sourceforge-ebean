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

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.PersistenceException;

import com.avaje.ebean.CallableSql;
import com.avaje.ebean.Filter;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;
import com.avaje.ebean.SqlUpdate;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.TxCallable;
import com.avaje.ebean.TxIsolation;
import com.avaje.ebean.TxRunnable;
import com.avaje.ebean.TxScope;
import com.avaje.ebean.TxType;
import com.avaje.ebean.Update;
import com.avaje.ebean.ValuePair;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.NodeUsageCollector;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.bean.ScopeTrans;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.control.AutoFetchControl;
import com.avaje.ebean.control.ServerControl;
import com.avaje.ebean.el.ElFilter;
import com.avaje.ebean.query.DefaultOrmQuery;
import com.avaje.ebean.query.DefaultOrmUpdate;
import com.avaje.ebean.query.DefaultRelationalQuery;
import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
import com.avaje.ebean.server.cache.CacheManager;
import com.avaje.ebean.server.ddl.DdlGenerator;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanDescriptorManager;
import com.avaje.ebean.server.deploy.BeanManager;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DNativeQuery;
import com.avaje.ebean.server.deploy.DeployNamedQuery;
import com.avaje.ebean.server.deploy.DeployNamedUpdate;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.jmx.MAutoFetchControl;
import com.avaje.ebean.server.jmx.MLogControl;
import com.avaje.ebean.server.jmx.MServerControl;
import com.avaje.ebean.server.lib.ShutdownManager;
import com.avaje.ebean.server.query.CQuery;
import com.avaje.ebean.server.query.CQueryEngine;
import com.avaje.ebean.server.transaction.RemoteListenerEvent;
import com.avaje.ebean.server.transaction.TransContext;
import com.avaje.ebean.server.transaction.TransactionEvent;
import com.avaje.ebean.server.transaction.TransactionManager;
import com.avaje.ebean.server.transaction.TransactionScopeManager;
import com.avaje.ebean.util.Message;

/**
 * The default server side implementation of EbeanServer.
 */
public final class DefaultServer implements InternalEbeanServer {

	private static final Logger logger = Logger.getLogger(DefaultServer.class.getName());

	/**
	 * Used when no errors are found validating a property.
	 */
	private static final InvalidValue[] EMPTY_INVALID_VALUES = new InvalidValue[0];


	/**
	 * The name, null for the 'primary' server.
	 */
	private final String serverName;

	private final ServerControl serverControl;

	/**
	 * Manages the transaction.
	 */
	private final TransactionManager transactionManager;

	private final TransactionScopeManager transactionScopeManager;

	/**
	 * Ebean defaults this to true but for EJB compatible behaviour 
	 * set this to false;
	 */
	private final boolean rollbackOnChecked;

	/**
	 * Handles the save, delete, updateSql CallableSql.
	 */
	private final Persister persister;

	private final OrmQueryEngine queryEngine;

	private final RelationalQueryEngine relationalQueryEngine;

	/**
	 * The cache implementation.
	 */
	private final CacheManager serverCache;

	private final BeanDescriptorManager beanDescriptorManager;

	private final DiffHelp diffHelp = new DiffHelp();

	private final MLogControl logControl;

	private final AutoFetchControl autoFetchControl;

	private final RefreshHelp refreshHelp;

	private final AutoFetchManager autoFetchManager;

	private final DebugLazyLoad debugLazyHelper;

	private final CQueryEngine cqueryEngine;
	 
	private final DdlGenerator ddlGenerator;
	
	/**
	 * Create the DefaultServer.
	 */
	public DefaultServer(InternalConfiguration config, CacheManager serverCache) {
		
		this.serverCache = serverCache;
		this.serverName = config.getServerConfig().getName();
		this.cqueryEngine = config.getCQueryEngine();
		this.logControl = config.getLogControl();
		this.refreshHelp = config.getRefreshHelp();
		this.debugLazyHelper = config.getDebugLazyLoad();
		this.beanDescriptorManager = config.getBeanDescriptorManager();
		this.rollbackOnChecked = GlobalProperties.getBoolean("ebean.transaction.rollbackOnChecked", true);

		this.transactionManager = config.getTransactionManager();
		this.transactionScopeManager = config.getTransactionScopeManager();

		this.persister = config.createPersister(this);
		this.queryEngine = config.createOrmQueryEngine(serverCache);
		this.relationalQueryEngine = config.createRelationalQueryEngine();

		autoFetchManager = config.createAutoFetchManager(this);
		autoFetchControl = new MAutoFetchControl(autoFetchManager);

		serverControl = new MServerControl(logControl, autoFetchControl);

		this.ddlGenerator = new DdlGenerator(this, config.getDatabasePlatform(), config.getServerConfig());
		ShutdownManager.register(new Shutdown());
	}
	
	public DdlGenerator getDdlGenerator() {
		return ddlGenerator;
	}

	public ServerControl getServerControl() {
		return serverControl;
	}

	public AutoFetchManager getAutoFetchManager() {
		return autoFetchManager;
	}

	public void registerMBeans(MBeanServer mbeanServer) {
		
		String mbeanName = "Ebean:server=" + serverName;

		try {
			mbeanServer.registerMBean(logControl, new ObjectName(mbeanName + ",function=Logging"));
			mbeanServer.registerMBean(autoFetchControl, new ObjectName(mbeanName + ",key=AutoFetch"));

		} catch (InstanceAlreadyExistsException e){
			String msg = "Error registering the JMX beans for Ebean server ["+serverName
				+"]. It seems that the Ebean server name ["+serverName
				+"] is not unique for this JVM. ";
			
			logger.log(Level.SEVERE, msg, e);
			
		} catch (Exception e) {
			String msg = "Error registering MBean["+mbeanName+"]";
			logger.log(Level.SEVERE, msg, e);
		}
	}

	private final class Shutdown implements Runnable {
		public void run() {
			// collect usage statistics
			autoFetchManager.shutdown();
		}
	}

	/**
	 * Return the server name.
	 */
	public String getName() {
		return serverName;
	}

	/**
	 * Compile a query.
	 */
	public <T> CQuery<T> compileQuery(Query<T> query, Transaction t) {
		OrmQueryRequest<T> qr = createQueryRequest(query, t);
	
		return cqueryEngine.buildQuery(qr);
	}
	
	public CQueryEngine getQueryEngine() {
		return cqueryEngine;
	}

	public CacheManager getServerCache() {
		return serverCache;
	}

	/**
	 * Return the Profile Listener.
	 */
	public AutoFetchManager getProfileListener() {
		return autoFetchManager;
	}

	/**
	 * Return the Relational query engine.
	 */
	public RelationalQueryEngine getRelationalQueryEngine() {
		return relationalQueryEngine;
	}

	public void refreshMany(Object parentBean, String propertyName) {
		refreshMany(parentBean, propertyName, null);
	}

	public void lazyLoadMany(Object parentBean, String propertyName, ObjectGraphNode profileNode) {

		refreshManyInternal(parentBean, propertyName, null, profileNode);

		if (profileNode != null || logControl.isDebugLazyLoad()) {

			Class<?> cls = parentBean.getClass();
			BeanDescriptor<?> desc = getBeanDescriptor(cls);
			BeanPropertyAssocMany<?> many = (BeanPropertyAssocMany<?>) desc.getBeanProperty(propertyName);

			StackTraceElement cause = debugLazyHelper.getStackTraceElement(cls);

			if (logControl.isDebugLazyLoad()) {
				String msg = "debug.lazyLoad " + many.getManyType() + " [" + desc + "][" + propertyName + "]";
				if (cause != null) {
					msg += " at: " + cause;
				}
				System.err.println(msg);
			}
		}
	}

	public void refreshMany(Object parentBean, String propertyName, Transaction t) {
		refreshManyInternal(parentBean, propertyName, t, null);
	}

	public void refreshManyInternal(Object parentBean, String propertyName, Transaction t, ObjectGraphNode profilePoint) {

		if (parentBean instanceof EntityBean) {
			EntityBean parent = (EntityBean) parentBean;
			Class<?> cls = parent.getClass();
			BeanDescriptor<?> desc = getBeanDescriptor(cls);
			BeanPropertyAssocMany<?> many = (BeanPropertyAssocMany<?>) desc.getBeanProperty(propertyName);

			Class<?> manyTypeCls = many.getTargetType();
			OrmQuery<?> query = (OrmQuery<?>) createQuery(manyTypeCls);

			// add parentBean to context for bidirectional relationships
			query.contextAdd(parent);
			if (profilePoint != null) {
				// so we can hook back to the root query
				query.setParentNode(profilePoint);
			}

			// build appropriate predicates for the query...
			many.setPredicates(query, parent);

			many.refresh(this, query, t, parent);

		} else {
			throw new PersistenceException("Can only refresh a previously queried bean");
		}
	}

	public void refresh(Object bean) {
		refreshBeanInternal(bean, null, false);
	}

	public void lazyLoadBean(Object bean, NodeUsageCollector collector) {

		refreshBeanInternal(bean, collector, true);
	}

	private void refreshBeanInternal(Object bean, NodeUsageCollector collector, boolean isLazyLoad) {

		if (bean instanceof EntityBean) {

			EntityBean eb = (EntityBean) bean;
			Class<?> beanType = bean.getClass();

			BeanDescriptor<?> desc = getBeanDescriptor(beanType);
			// get the real POJO type (no $$EntityBean stuff)
			beanType = desc.getBeanType();

			Object id = desc.getId(bean);

			EntityBeanIntercept ebi = eb._ebean_getIntercept();

			Object parentBean = ebi.getParentBean();

			OrmQuery<?> query = (OrmQuery<?>) createQuery(beanType);
			
			// don't collect autoFetch usage profiling information
			// as we just copy the data out of these fetched beans
			// and put the data into the original bean
			query.setUsageProfiling(false);
			
			if (!isLazyLoad){
				// for refresh we want to run in our own
				// context (not an existing transaction scoped one)
				query.setTransactionContext(new TransContext());
			}

			if (parentBean != null) {
				query.contextAdd(eb);
			}

			if (collector != null) {
				query.setParentNode(collector.getNode());
			}

			query.setId(id);
			Object dbBean = query.findUnique();
			if (dbBean == null) {
				String msg = "Bean not found during lazy load or refresh." + " id[" + id + "] type[" + beanType + "]";
				throw new PersistenceException(msg);
			}

			// merge the existing and new dbBean bean
			refreshHelp.refresh(bean, dbBean, desc, ebi, id, isLazyLoad);

		} else {
			throw new PersistenceException("Can only refresh a previously fetched bean.");
		}
	}

	public InvalidValue validate(Object bean) {
		if (bean == null) {
			return null;
		}
		BeanDescriptor<?> beanDescriptor = getBeanDescriptor(bean.getClass());
		return beanDescriptor.validate(true, bean);
	}

	public InvalidValue[] validate(Object bean, String propertyName, Object value) {
		if (bean == null) {
			return null;
		}
		BeanDescriptor<?> beanDescriptor = getBeanDescriptor(bean.getClass());
		BeanProperty prop = beanDescriptor.getBeanProperty(propertyName);
		if (prop == null) {
			String msg = "property " + propertyName + " was not found?";
			throw new PersistenceException(msg);
		}
		if (value == null) {
			value = prop.getValue(bean);
		}
		List<InvalidValue> errors = prop.validate(true, value);
		if (errors == null) {
			return EMPTY_INVALID_VALUES;
		} else {
			return InvalidValue.toArray(errors);
		}
	}

	public Map<String, ValuePair> diff(Object a, Object b) {
		if (a == null) {
			return null;
		}

		Class<?> cls = a.getClass();
		BeanDescriptor<?> desc = getBeanDescriptor(cls);
		return diffHelp.diff(a, b, desc);
	}

	/**
	 * Process committed beans from another framework or server in another
	 * cluster.
	 * <p>
	 * This notifies this instance of the framework that beans have been
	 * committed externally to it. Either by another framework or clustered
	 * server. It needs to maintain its cache and lucene indexes appropriately.
	 * </p>
	 */
	public void externalModification(TransactionEvent event) {
		ServerTransaction t = transactionScopeManager.get();
		if (t != null) {
			t.getEvent().add(event);
		} else {
			transactionManager.externalModification(event);
		}
	}

	/**
	 * Developer informing eBean that tables where modified outside of eBean.
	 * Invalidate the cache etc as required.
	 */
	public void externalModification(String tableName, boolean inserts, boolean updates, boolean deletes) {
		TransactionEvent evt = new TransactionEvent();
		if (inserts) {
			evt.addInsert(tableName);
		}
		if (updates) {
			evt.addUpdate(tableName);
		}
		if (deletes) {
			evt.addDelete(tableName);
		}
		externalModification(evt);
	}

	/**
	 * Clear the query execution statistics.
	 */
	public void clearQueryStatistics() {
		for (BeanDescriptor<?> desc : getBeanDescriptors()) {
			desc.clearQueryStatistics();			
		}
	}

	/**
	 * Create a new EntityBean bean.
	 * <p>
	 * This will generally return a subclass of the parameter 'type' which
	 * additionally implements the EntityBean interface. That is, the returned
	 * bean is typically an instance of a dynamically generated class.
	 * </p>
	 */
	public EntityBean createEntityBean(Class<?> type) {
		BeanDescriptor<?> desc = getBeanDescriptor(type);
		return desc.createEntityBean();
	}

	/**
	 * Return a Reference bean.
	 * <p>
	 * If a current transaction is active then this will check the Context of
	 * that transaction to see if the bean is already loaded. If it is already
	 * loaded then it will returned that object.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	public <T> T getReference(Class<T> type, Object id) {

		if (id == null) {
			throw new NullPointerException("The id is null");
		}

		EntityBean ref = null;
		TransactionContext ctx = null;

		ServerTransaction t = transactionScopeManager.get();
		if (t != null) {
			ctx = t.getTransactionContext();
			ref = ctx.get(type, id);
		}
		if (ref == null) {
			BeanDescriptor desc = getBeanDescriptor(type);

			// convert the id type if necessary
			id = desc.convertId(id);

			InheritInfo inheritInfo = desc.getInheritInfo();
			if (inheritInfo != null) {
				// we actually need to do a query because
				// we don't know the type without the
				// discriminator value
				BeanProperty[] idProps = desc.propertiesId();
				String idNames;
				switch (idProps.length) {
				case 0:
					throw new PersistenceException("No ID properties for this type? " + desc);
				case 1:
					idNames = idProps[0].getName();
					break;
				default:
					idNames = Arrays.toString(idProps);
					idNames = idNames.substring(1, idNames.length() - 1);
				}

				// just select the id properties and
				// the discriminator column (auto added)
				Query query = createQuery(type);
				query.select(idNames).setId(id);

				ref = (EntityBean) query.findUnique();

			} else {
				ref = desc.createReference(id, null, null);
			}

			if (ctx != null) {
				ctx.set(type, id, ref);
			}
		}
		return (T) ref;
	}

	/**
	 * Creates a new Transaction that is NOT stored in TransactionThreadLocal.
	 * Use this when you want a thread to have a second independent transaction.
	 */
	public Transaction createTransaction() {

		return transactionManager.createTransaction(true, -1);
	}

	/**
	 * Create a transaction additionally specify the Isolation level.
	 * <p>
	 * Note that this transaction is not stored in a thread local.
	 * </p>
	 */
	public Transaction createTransaction(TxIsolation isolation) {

		return transactionManager.createTransaction(true, isolation.getLevel());
	}

	/**
	 * Log a comment to the transaction log (of the current transaction).
	 */
	public void logComment(String msg) {
		Transaction t = transactionScopeManager.get();
		if (t != null) {
			t.log(msg);
		}
	}

	public <T> T execute(TxCallable<T> c) {
		return execute(null, c);
	}

	public <T> T execute(TxScope scope, TxCallable<T> c) {
		ScopeTrans scopeTrans = createScopeTrans(scope);
		try {
			return c.call();

		} catch (Error e) {
			throw scopeTrans.caughtError(e);

		} catch (RuntimeException e) {
			throw scopeTrans.caughtThrowable(e);

		} finally {
			scopeTrans.onFinally();
		}
	}

	public void execute(TxRunnable r) {
		execute(null, r);
	}

	public void execute(TxScope scope, TxRunnable r) {
		ScopeTrans scopeTrans = createScopeTrans(scope);
		try {
			r.run();

		} catch (Error e) {
			throw scopeTrans.caughtError(e);

		} catch (RuntimeException e) {
			throw scopeTrans.caughtThrowable(e);

		} finally {
			scopeTrans.onFinally();
		}
	}

	/**
	 * Determine whether to create a new transaction or not.
	 * <p>
	 * This will also potentially throw exceptions for MANDATORY and NEVER
	 * types.
	 * </p>
	 */
	private boolean createNewTransaction(ServerTransaction t, TxScope scope) {

		TxType type = scope.getType();
		switch (type) {
		case REQUIRED:
			return t == null;

		case REQUIRES_NEW:
			return true;

		case MANDATORY:
			if (t == null)
				throw new PersistenceException("Transaction missing when MANDATORY");
			return true;

		case NEVER:
			if (t != null)
				throw new PersistenceException("Transaction exists for Transactional NEVER");
			return false;

		case SUPPORTS:
			return false;

		case NOT_SUPPORTED:
			throw new RuntimeException("NOT_SUPPORTED should already be handled?");

		default:
			throw new RuntimeException("Should never get here?");
		}
	}

	public ScopeTrans createScopeTrans(TxScope txScope) {

		if (txScope == null) {
			// create a TxScope with default settings
			txScope = new TxScope();
		}

		ServerTransaction suspended = null;

		// get current transaction from ThreadLocal or equivalent
		ServerTransaction t = transactionScopeManager.get();

		boolean newTransaction;
		if (txScope.getType().equals(TxType.NOT_SUPPORTED)) {
			// Suspend existing transaction and 
			// run without a transaction in scope
			newTransaction = false;
			suspended = t;
			t = null;

		} else {
			// create a new Transaction based on TxType and t
			newTransaction = createNewTransaction(t, txScope);

			if (newTransaction) {
				// suspend existing transaction (if there is one)
				suspended = t;

				// create a new transaction 
				int isoLevel = -1;
				TxIsolation isolation = txScope.getIsolation();
				if (isolation != null) {
					isoLevel = isolation.getLevel();
				}
				t = transactionManager.createTransaction(true, isoLevel);
			}
		}
		
		// replace the current transaction ... ScopeTrans.onFinally()
		// has the job of restoring the suspended transaction 
		transactionScopeManager.replace(t);

		return new ScopeTrans(rollbackOnChecked, newTransaction, t, txScope, suspended, transactionScopeManager);
	}

	/**
	 * Returns the current transaction (or null) from the scope.
	 */
	public ServerTransaction getCurrentServerTransaction() {
		return transactionScopeManager.get();
	}

	/**
	 * Start a transaction.
	 * <p>
	 * Note that the transaction is stored in a ThreadLocal variable.
	 * </p>
	 */
	public Transaction beginTransaction() {
		// start an explicit transaction
		ServerTransaction t = transactionManager.createTransaction(true, -1);
		transactionScopeManager.set(t);
		return t;
	}

	/**
	 * Start a transaction with a specific Isolation Level.
	 * <p>
	 * Note that the transaction is stored in a ThreadLocal variable.
	 * </p>
	 */
	public Transaction beginTransaction(TxIsolation isolation) {
		// start an explicit transaction
		ServerTransaction t = transactionManager.createTransaction(true, isolation.getLevel());
		transactionScopeManager.set(t);
		return t;
	}

	/**
	 * Return the current transaction or null if there is not one currently in
	 * scope.
	 */
	public Transaction currentTransaction() {
		return transactionScopeManager.get();
	}

	/**
	 * Commit the current transaction.
	 */
	public void commitTransaction() {
		transactionScopeManager.commit();
	}

	/**
	 * Rollback the current transaction.
	 */
	public void rollbackTransaction() {
		transactionScopeManager.rollback();
	}

	/**
	 * If the current transaction has already been committed do nothing
	 * otherwise rollback the transaction.
	 * <p>
	 * Useful to put in a finally block to ensure the transaction is ended,
	 * rather than a rollbackTransaction() in each catch block.
	 * </p>
	 * <p>
	 * Code example:<br />
	 * 
	 * <pre><code>
	 * Ebean.startTransaction();
	 * try {
	 * 	// do some fetching and or persisting
	 * 
	 * 	// commit at the end
	 * 	Ebean.commitTransaction();
	 * 
	 * } finally {
	 * 	// if commit didn't occur then rollback the transaction
	 * 	Ebean.endTransaction();
	 * }
	 * </code></pre>
	 * 
	 * </p>
	 */
	public void endTransaction() {
		transactionScopeManager.end();
	}

	/**
	 * return the next unique identity value.
	 * <p>
	 * Uses the BeanDescriptor deployment information to determine the sequence
	 * to use.
	 * </p>
	 */
	public Object nextId(Class<?> beanType) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		return persister.nextId(desc);
	}
	
	@SuppressWarnings("unchecked")
	public <T> void sort(List<T> list, String sortByClause){
		
		if (list == null){
			throw new NullPointerException("list is null");
		}
		if (sortByClause == null){
			throw new NullPointerException("sortByClause is null");
		}
		if (list.size() == 0){
			// don't need to sort an empty list
			return;
		}
		// use first bean in the list as the correct type
		Class<T> beanType = (Class<T>)list.get(0).getClass();
		BeanDescriptor<T> beanDescriptor = getBeanDescriptor(beanType);
		if (beanDescriptor == null){
			String m = "BeanDescriptor not found, is ["+beanType+"] an entity bean?";
			throw new PersistenceException(m);
		}
		beanDescriptor.sort(list, sortByClause);
	}


	public <T> Query<T> createQuery(Class<T> beanType, String namedQuery) throws PersistenceException {

		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			throw new PersistenceException("Is " + beanType.getName() + " an Entity Bean? BeanDescriptor not found?");
		}
		DeployNamedQuery deployQuery = desc.getNamedQuery(namedQuery);
		if (deployQuery == null) {
			throw new PersistenceException("named query " + namedQuery + " was not found for " + desc.getFullName());
		}

		// this will parse the query
		return new DefaultOrmQuery<T>(beanType, this, deployQuery);
	}
	
	public <T> Filter<T> filter(Class<T> beanType) {
		BeanDescriptor<T> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			throw new PersistenceException(beanType.getName() + " is NOT an Entity Bean?");
		}
		return new ElFilter<T>(desc);
	}

	public <T> Query<T> find(Class<T> beanType) {
		return createQuery(beanType);
	}

	public <T> Query<T> createQuery(Class<T> beanType) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			throw new PersistenceException(beanType.getName() + " is NOT an Entity Bean?");
		}
		if (desc.isSqlSelectBased()) {
			// use the "default" SqlSelect
			DeployNamedQuery defaultSqlSelect = desc.getNamedQuery("default");
			return new DefaultOrmQuery<T>(beanType, this, defaultSqlSelect);

		} else {
			return new DefaultOrmQuery<T>(beanType, this);
		}
	}

	public <T> Update<T> createUpdate(Class<T> beanType, String namedUpdate) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			throw new PersistenceException(beanType.getName() + " is NOT an Entity Bean?");
		}

		DeployNamedUpdate deployUpdate = desc.getNamedUpdate(namedUpdate);
		if (deployUpdate == null) {
			throw new PersistenceException("named update " + namedUpdate + " was not found for " + desc.getFullName());
		}

		return new DefaultOrmUpdate<T>(beanType, this, desc.getBaseTable(), deployUpdate);
	}

	public <T> Update<T> createUpdate(Class<T> beanType) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			throw new PersistenceException(beanType.getName() + " is NOT an Entity Bean?");
		}

		return new DefaultOrmUpdate<T>(beanType, this, desc.getBaseTable());
	}

	public SqlQuery createSqlQuery() {
		return new DefaultRelationalQuery(this, null);
	}

	public SqlQuery createSqlQuery(String namedQuery) {
		DNativeQuery nq = beanDescriptorManager.getNativeQuery(namedQuery);
		if (nq == null) {
			throw new PersistenceException("SqlQuery " + namedQuery + " not found.");
		}
		return new DefaultRelationalQuery(this, nq.getQuery());
	}

	public SqlUpdate createSqlUpdate() {
		return new SqlUpdate(this, null);
	}

	public SqlUpdate createSqlUpdate(String namedQuery) {
		DNativeQuery nq = beanDescriptorManager.getNativeQuery(namedQuery);
		if (nq == null) {
			throw new PersistenceException("SqlUpdate " + namedQuery + " not found.");
		}
		return new SqlUpdate(this, nq.getQuery());
	}

	public <T> T find(Class<T> beanType, Object uid) {

		return find(beanType, uid, null);
	}

	/**
	 * Find a bean using its unique id.
	 */
	public <T> T find(Class<T> beanType, Object id, Transaction t) {

		if (id == null) {
			throw new NullPointerException("The id is null");
		}

		Query<T> query = createQuery(beanType).setId(id);
		return findId(query, t);
	}

	public <T> OrmQueryRequest<T> createQueryRequest(Query<T> q, Transaction t) {

		OrmQuery<T> query = (OrmQuery<T>) q;
		BeanManager<T> mgr = beanDescriptorManager.getBeanManager(query.getBeanType());

		if (mgr.isAutoFetchTunable() && !query.isSqlSelect()) {
			// its a tunable query
			autoFetchManager.tuneQuery(query);
		}
		ServerTransaction serverTrans = (ServerTransaction)t;
		OrmQueryRequest<T> request = new OrmQueryRequest<T>(this, queryEngine, query, mgr, serverTrans);
		// the query hash after an AutoFetch tuning
		request.calculateQueryPlanHash();
		return request;
	}

	@SuppressWarnings("unchecked")
	public <T> T findId(Query<T> query, Transaction t) {
		OrmQueryRequest request = createQueryRequest(query, t);
		try {
			request.initTransIfRequired();

			T bean = (T) request.findId();
			request.endTransIfRequired();

			return bean;

		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public <T> T findUnique(Query<T> query, Transaction t) {

		// actually a find by Id type of query...
		// ... perhaps with joins and cache hints?
		OrmQuery<T> q = (OrmQuery<T>) query;
		Object id = q.getId();
		if (id != null) {
			return findId(query, t);
		}

		// a query that is expected to return either 0 or 1 rows
		List<T> list = findList(query, t);

		if (list.size() == 0) {
			return null;
		} else if (list.size() > 1) {
			String m = "Unique expecting 0 or 1 rows but got [" + list.size() + "]";
			throw new PersistenceException(m);
		} else {
			return list.get(0);
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Set<T> findSet(Query<T> query, Transaction t) {

		OrmQueryRequest request = createQueryRequest(query, t);
		try {
			request.initTransIfRequired();
			Set<T> set = (Set<T>) request.findSet();
			request.endTransIfRequired();

			return set;

		} catch (RuntimeException ex) {
			//String stackTrace = throwablePrinter.print(ex);
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Map<?, T> findMap(Query<T> query, Transaction t) {
		OrmQueryRequest request = createQueryRequest(query, t);
		try {
			request.initTransIfRequired();
			Map<?, T> map = (Map<?, T>) request.findMap();
			request.endTransIfRequired();

			return map;

		} catch (RuntimeException ex) {
			//String stackTrace = throwablePrinter.print(ex);
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> findList(Query<T> query, Transaction t) {

		OrmQueryRequest request = createQueryRequest(query, t);
		try {
			request.initTransIfRequired();
			List<T> list = (List<T>) request.findList();
			request.endTransIfRequired();

			return list;

		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public SqlRow findUnique(SqlQuery query, Transaction t) {

		// no findId() method for SqlQuery...
		// a query that is expected to return either 0 or 1 rows
		List<SqlRow> list = findList(query, t);

		if (list.size() == 0) {
			return null;

		} else if (list.size() > 1) {
			String m = "Unique expecting 0 or 1 rows but got [" + list.size() + "]";
			throw new PersistenceException(m);

		} else {
			return list.get(0);
		}
	}

	public List<SqlRow> findList(SqlQuery query, Transaction t) {

		RelationalQueryRequest request = new RelationalQueryRequest(this, relationalQueryEngine, query, t);

		try {
			request.initTransIfRequired();
			List<SqlRow> list = request.findList();
			request.endTransIfRequired();

			return list;

		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public Set<SqlRow> findSet(SqlQuery query, Transaction t) {

		RelationalQueryRequest request = new RelationalQueryRequest(this, relationalQueryEngine, query, t);

		try {
			request.initTransIfRequired();
			Set<SqlRow> set = request.findSet();
			request.endTransIfRequired();

			return set;

		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public Map<?, SqlRow> findMap(SqlQuery query, Transaction t) {

		RelationalQueryRequest request = new RelationalQueryRequest(this, relationalQueryEngine, query, t);
		try {
			request.initTransIfRequired();
			Map<?, SqlRow> map = request.findMap();
			request.endTransIfRequired();

			return map;

		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	/**
	 * Persist the bean by either performing an insert or update.
	 */
	public void save(Object bean) {
		save(bean, null);
	}

	/**
	 * Save the bean with an explicit transaction.
	 */
	public void save(Object bean, Transaction t) {
		if (bean == null) {
			throw new NullPointerException(Message.msg("bean.isnull"));
		}
		persister.save(bean, t);
	}

	/**
	 * Perform an update or insert on each bean in the iterator. Returns the
	 * number of beans that where saved.
	 */
	public int save(Iterator<?> it) {
		return save(it, null);
	}

	/**
	 * Save all beans in the iterator with an explicit transaction.
	 */
	public int save(Iterator<?> it, Transaction t) {

		TransWrapper wrap = initTransIfRequired(t);
		try {
			ServerTransaction trans = wrap.transaction;
			int saveCount = 0;
			while (it.hasNext()) {
				Object bean = it.next();
				persister.save(bean, trans);
				saveCount++;
			}

			wrap.commitIfCreated();

			return saveCount;

		} catch (RuntimeException e) {
			wrap.rollbackIfCreated();
			throw e;
		}
	}

	/**
	 * Delete the bean.
	 */
	public void delete(Object bean) {
		delete(bean, null);
	}

	/**
	 * Delete the bean with the explicit transaction.
	 */
	public void delete(Object bean, Transaction t) {
		if (bean == null) {
			throw new NullPointerException(Message.msg("bean.isnull"));
		}
		persister.delete(bean, t);
	}

	/**
	 * Delete all the beans in the iterator.
	 */
	public int delete(Iterator<?> it) {
		return delete(it, null);
	}

	/**
	 * Delete all the beans in the iterator with an explicit transaction.
	 */
	public int delete(Iterator<?> it, Transaction t) {

		TransWrapper wrap = initTransIfRequired(t);

		try {
			ServerTransaction trans = wrap.transaction;
			int deleteCount = 0;
			while (it.hasNext()) {
				Object bean = it.next();
				persister.delete(bean, trans);
				deleteCount++;
			}

			wrap.commitIfCreated();

			return deleteCount;

		} catch (RuntimeException e) {
			wrap.rollbackIfCreated();
			throw e;
		}
	}

	/**
	 * Execute the CallableSql with an explicit transaction.
	 */
	public void execute(CallableSql callSql, Transaction t) {
		persister.executeCallable(callSql, t);
	}

	/**
	 * Execute the CallableSql.
	 */
	public void execute(CallableSql callSql) {
		execute(callSql, null);
	}

	/**
	 * Execute the updateSql with an explicit transaction.
	 */
	public int execute(SqlUpdate updSql, Transaction t) {
		return persister.executeSqlUpdate(updSql, t);
	}

	/**
	 * Execute the updateSql.
	 */
	public int execute(SqlUpdate updSql) {
		return execute(updSql, null);
	}

	/**
	 * Execute the updateSql with an explicit transaction.
	 */
	public int execute(Update<?> update, Transaction t) {
		return persister.executeOrmUpdate(update, t);
	}

	/**
	 * Execute the orm update.
	 */
	public int execute(Update<?> update) {
		return execute(update, null);
	}

	public <T> BeanManager<T> getBeanManager(Class<T> beanClass) {
		return beanDescriptorManager.getBeanManager(beanClass);
	}

	/**
	 * Return all the BeanDescriptors.
	 */
	public List<BeanDescriptor<?>> getBeanDescriptors() {
		return beanDescriptorManager.getBeanDescriptorList();
	}
	
	/**
	 * Return the BeanDescriptor for a given type of bean.
	 */
	public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> beanClass) {
		return beanDescriptorManager.getBeanDescriptor(beanClass);
	}


	/**
	 * Another server in the cluster sent this event so that we can inform local
	 * BeanListeners of inserts updates and deletes that occurred remotely (on
	 * another server in the cluster).
	 */
	public void remoteListenerEvent(RemoteListenerEvent event) {
		transactionManager.remoteListenerEvent(event);
	}

	/**
	 * Create a transaction if one is not currently active in the
	 * TransactionThreadLocal.
	 * <p>
	 * Returns a TransWrapper which contains the wasCreated flag. If this is
	 * true then the transaction was created for this request in which case it
	 * will need to be committed after the request has been processed.
	 * </p>
	 */
	TransWrapper initTransIfRequired(Transaction t) {

		if (t != null) {
			return new TransWrapper((ServerTransaction) t, false);
		}

		boolean wasCreated = false;
		ServerTransaction trans = transactionScopeManager.get();
		if (trans == null || !trans.isActive()) {
			// create a transaction
			trans = transactionManager.createTransaction(false, -1);
			wasCreated = true;
		}
		return new TransWrapper(trans, wasCreated);
	}

	public ServerTransaction createServerTransaction(boolean isExplicit, int isolationLevel) {
		return transactionManager.createTransaction(isExplicit, isolationLevel);
	}

	public ServerTransaction createQueryTransaction() {
		return transactionManager.createQueryTransaction();
	}
}
