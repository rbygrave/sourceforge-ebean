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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.PersistenceException;

import com.avaje.ebean.AdminAutofetch;
import com.avaje.ebean.AdminLogging;
import com.avaje.ebean.BackgroundExecutor;
import com.avaje.ebean.BeanState;
import com.avaje.ebean.CallableSql;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.Filter;
import com.avaje.ebean.FutureIds;
import com.avaje.ebean.FutureList;
import com.avaje.ebean.FutureRowCount;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.avaje.ebean.SqlFutureList;
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
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.cache.ServerCacheManager;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.internal.ScopeTrans;
import com.avaje.ebean.internal.SpiEbeanServer;
import com.avaje.ebean.internal.SpiQuery;
import com.avaje.ebean.internal.SpiSqlQuery;
import com.avaje.ebean.internal.SpiTransaction;
import com.avaje.ebean.internal.TransactionEventTable;
import com.avaje.ebean.server.autofetch.AutoFetchManager;
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
import com.avaje.ebean.server.el.ElFilter;
import com.avaje.ebean.server.jmx.MAdminAutofetch;
import com.avaje.ebean.server.lib.ShutdownManager;
import com.avaje.ebean.server.query.CQuery;
import com.avaje.ebean.server.query.CQueryEngine;
import com.avaje.ebean.server.query.CallableQueryIds;
import com.avaje.ebean.server.query.CallableQueryList;
import com.avaje.ebean.server.query.CallableQueryRowCount;
import com.avaje.ebean.server.query.CallableSqlQueryList;
import com.avaje.ebean.server.query.LimitOffsetPagingQuery;
import com.avaje.ebean.server.query.QueryFutureIds;
import com.avaje.ebean.server.query.QueryFutureList;
import com.avaje.ebean.server.query.QueryFutureRowCount;
import com.avaje.ebean.server.query.SqlQueryFutureList;
import com.avaje.ebean.server.querydefn.DefaultOrmQuery;
import com.avaje.ebean.server.querydefn.DefaultOrmUpdate;
import com.avaje.ebean.server.querydefn.DefaultRelationalQuery;
import com.avaje.ebean.server.transaction.DefaultPersistenceContext;
import com.avaje.ebean.server.transaction.RemoteTransactionEvent;
import com.avaje.ebean.server.transaction.TransactionManager;
import com.avaje.ebean.server.transaction.TransactionScopeManager;

/**
 * The default server side implementation of EbeanServer.
 */
public final class DefaultServer implements SpiEbeanServer {

	private static final Logger logger = Logger.getLogger(DefaultServer.class.getName());

	/**
	 * Used when no errors are found validating a property.
	 */
	private static final InvalidValue[] EMPTY_INVALID_VALUES = new InvalidValue[0];

	private final String serverName;

	private final AdminLogging adminLogging;
	private final AdminAutofetch adminAutofetch;
	
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

	private final ServerCacheManager serverCacheManager;

	private final BeanDescriptorManager beanDescriptorManager;

	private final DiffHelp diffHelp = new DiffHelp();

	private final RefreshHelp refreshHelp;

	private final AutoFetchManager autoFetchManager;

	private final DebugLazyLoad debugLazyHelper;

	private final CQueryEngine cqueryEngine;
	 
	private final DdlGenerator ddlGenerator;
	
	private final ExpressionFactory expressionFactory;
	
	private final BackgroundExecutor backgroundExecutor;
	
	/**
	 * Create the DefaultServer.
	 */
	public DefaultServer(InternalConfiguration config, ServerCacheManager cache) {
		
		this.serverCacheManager = cache;
		this.backgroundExecutor = config.getBackgroundExecutor();
		this.serverName = config.getServerConfig().getName();
		this.cqueryEngine = config.getCQueryEngine();
		this.expressionFactory = config.getExpressionFactory();
		this.adminLogging = config.getLogControl();
		this.refreshHelp = config.getRefreshHelp();
		this.debugLazyHelper = config.getDebugLazyLoad();
		this.beanDescriptorManager = config.getBeanDescriptorManager();
		beanDescriptorManager.setEbeanServer(this);
		this.rollbackOnChecked = GlobalProperties.getBoolean("ebean.transaction.rollbackOnChecked", true);

		this.transactionManager = config.getTransactionManager();
		this.transactionScopeManager = config.getTransactionScopeManager();

		this.persister = config.createPersister(this);
		this.queryEngine = config.createOrmQueryEngine();
		this.relationalQueryEngine = config.createRelationalQueryEngine();

		autoFetchManager = config.createAutoFetchManager(this);
		adminAutofetch = new MAdminAutofetch(autoFetchManager);

		this.ddlGenerator = new DdlGenerator(this, config.getDatabasePlatform(), config.getServerConfig());
		ShutdownManager.register(new Shutdown());
	}

	public BackgroundExecutor getBackgroundExecutor() {
		return backgroundExecutor;
	}

	public ExpressionFactory getExpressionFactory() {
		return expressionFactory;
	}

	public DdlGenerator getDdlGenerator() {
		return ddlGenerator;
	}
	
	public AdminLogging getAdminLogging() {
		return adminLogging;
	}


	public AdminAutofetch getAdminAutofetch() {
		return adminAutofetch;
	}


	public AutoFetchManager getAutoFetchManager() {
		return autoFetchManager;
	}

	public void registerMBeans(MBeanServer mbeanServer, int uniqueServerId) {
		
		String mbeanName = "Ebean:server=" + serverName + uniqueServerId;

		try {
			mbeanServer.registerMBean(adminLogging, new ObjectName(mbeanName + ",function=Logging"));
			mbeanServer.registerMBean(adminAutofetch, new ObjectName(mbeanName + ",key=AutoFetch"));

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

	public BeanState getBeanState(Object bean){
		if (bean instanceof EntityBean) {
			return new DefaultBeanState((EntityBean) bean);
		}
		// if using "subclassing" (not enhancement) this will
		// return null for 'vanilla' instances (not subclassed)
		return null;
		// throw new PersistenceException("The bean is not an EntityBean");
	}
	
	/**
	 * Run the cache warming queries on all beans that have them defined.
	 */
	public void runCacheWarming() {
		List<BeanDescriptor<?>> descList = beanDescriptorManager.getBeanDescriptorList();
		for (int i = 0; i < descList.size(); i++) {
			descList.get(i).runCacheWarming();
		}
	}

	public void runCacheWarming(Class<?> beanType) {
		BeanDescriptor<?> desc  = beanDescriptorManager.getBeanDescriptor(beanType);
		if (desc == null){
			String msg = "Is "+beanType+" an entity? Could not find a BeanDescriptor";
			throw new PersistenceException(msg);
		} else {
			desc.runCacheWarming();
		}
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

	public ServerCacheManager getServerCacheManager() {
		return serverCacheManager;
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

		if (profileNode != null || adminLogging.isDebugLazyLoad()) {

			Class<?> cls = parentBean.getClass();
			BeanDescriptor<?> desc = getBeanDescriptor(cls);
			BeanPropertyAssocMany<?> many = (BeanPropertyAssocMany<?>) desc.getBeanProperty(propertyName);

			StackTraceElement cause = debugLazyHelper.getStackTraceElement(cls);

			if (adminLogging.isDebugLazyLoad()) {
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
			SpiQuery<?> query = (SpiQuery<?>) createQuery(manyTypeCls);

			PersistenceContext persistenceContext = parent._ebean_getIntercept().getPersistenceContext();
			if (persistenceContext == null){
				persistenceContext = new DefaultPersistenceContext();
				Object parentId = desc.getId(parent);
				persistenceContext.put(parentId, parent);
			}
			query.setPersistenceContext(persistenceContext);
			
//			// add parentBean to context for bidirectional relationships
//			query.contextAdd(parent);
			if (profilePoint != null) {
				// so we can hook back to the root query
				query.setParentNode(profilePoint);
			}

			// build appropriate predicates for the query...
			many.setPredicates(query, parent);
			
			if (parent._ebean_getIntercept().isSharedInstance()){
				// lazy loading for a sharedInstance (bean in the cache)
				query.setSharedInstance();
			}

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

		if (!(bean instanceof EntityBean)) {
			throw new PersistenceException("Can only refresh a previously fetched bean.");
		}
		EntityBean eb = (EntityBean) bean;
		Class<?> beanType = bean.getClass();

		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		// get the real POJO type (no $$EntityBean stuff)
		beanType = desc.getBeanType();

		Object id = desc.getId(bean);

		EntityBeanIntercept ebi = eb._ebean_getIntercept();

		PersistenceContext persistenceContext = null;
		if (!isLazyLoad){
			// for refresh we want to run in a new persistenceContext
			persistenceContext = new DefaultPersistenceContext();
			
		} else {
			persistenceContext = eb._ebean_getIntercept().getPersistenceContext();
			if (persistenceContext == null){
				// a reference with no existing persistenceContext
				persistenceContext = new DefaultPersistenceContext();
				eb._ebean_getIntercept().setPersistenceContext(persistenceContext);
			} else {
				// remove the bean from the persistence context temporarily 
				// so that we load a fresh bean from the database
				persistenceContext.clear(eb.getClass(), id);
			}
		}
		
		
		Object sourceBean =  null;
		if (ebi.isUseCache() && ebi.isReference()) {
			// try to use a bean from the cache to load from
			sourceBean = desc.cacheGet(id);
		}
		
		if (sourceBean == null){
			// query the database 
			Object parentBean = ebi.getParentBean();
	
			SpiQuery<?> query = (SpiQuery<?>) createQuery(beanType);
			
			// don't collect autoFetch usage profiling information
			// as we just copy the data out of these fetched beans
			// and put the data into the original bean
			query.setUsageProfiling(false);

			if (parentBean != null) {
				// Special case for OneToOne 
				BeanDescriptor<?> parentDesc = getBeanDescriptor(parentBean.getClass());
				Object parentId = parentDesc.getId(parentBean);
				persistenceContext.put(parentId, parentBean);
			}
			
			query.setPersistenceContext(persistenceContext);
	
			if (collector != null) {
				query.setParentNode(collector.getNode());
			}
	
			// make sure the query doesn't use the cache and
			// use readOnly in case we put the bean in the cache
			sourceBean = query.setId(id)
				.setUseCache(false)
				.setReadOnly(true)
				.findUnique();
			
			if (sourceBean == null) {
				String msg = "Bean not found during lazy load or refresh." + " id[" + id + "] type[" + beanType + "]";
				throw new PersistenceException(msg);
			}
			
			if (ebi.isUseCache()){
				// put the fresh source bean into the cache
				// it will be readOnly and a sharedInstance
				desc.cachePutObject(sourceBean);
			}
		}

		// put the refreshed entity into the persistenceContext
		// after the dbBean (fresh one) has been loaded
		persistenceContext.put(id, eb);

		// merge the existing and new dbBean bean
		refreshHelp.refresh(bean, sourceBean, desc, ebi, id, isLazyLoad);
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

		BeanDescriptor<?> desc = getBeanDescriptor(a.getClass());
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
	public void externalModification(TransactionEventTable tableEvent) {
		SpiTransaction t = transactionScopeManager.get();
		if (t != null) {
			t.getEvent().add(tableEvent);
		} else {
			transactionManager.externalModification(tableEvent);
		}
	}

	/**
	 * Developer informing eBean that tables where modified outside of eBean.
	 * Invalidate the cache etc as required.
	 */
	public void externalModification(String tableName, boolean inserts, boolean updates, boolean deletes) {
		
		TransactionEventTable evt = new TransactionEventTable();
		evt.add(tableName, inserts, updates, deletes);
		
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
	@SuppressWarnings("unchecked")
	public <T> T createEntityBean(Class<T> type) {
		BeanDescriptor<T> desc = getBeanDescriptor(type);
		return (T)desc.createEntityBean();
	}
	
	

	public ObjectInputStream createProxyObjectInputStream(InputStream is) {
		
		try {
			return new ProxyBeanObjectInputStream(is, this);
		} catch (IOException e) {
			throw new PersistenceException(e);
		}
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

		BeanDescriptor desc = getBeanDescriptor(type);
		// convert the id type if necessary
		id = desc.convertId(id);

		Object ref = null;
		PersistenceContext ctx = null;

		SpiTransaction t = transactionScopeManager.get();
		if (t != null) {
			// first try the persistence context
			ctx = t.getPersistenceContext();
			ref = ctx.get(type, id);
		}
		if (ref == null) {
			// check to see if the bean cache should be used
			ReferenceOptions opts = desc.getReferenceOptions();
			if (opts != null && opts.isUseCache()){
				ref = desc.cacheGet(id);
				if (ref != null && !opts.isReadOnly()){
					ref = desc.createCopy(ref);
				}
			}
		}
		
		if (ref == null) {
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
				Query<T> query = createQuery(type);
				query.select(idNames).setId(id);

				ref = query.findUnique();

			} else {
				// use the default reference options
				ref = desc.createReference(id, null, desc.getReferenceOptions());
			}

			if (ctx != null) {
				ctx.put(id, ref);
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
	private boolean createNewTransaction(SpiTransaction t, TxScope scope) {

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

		SpiTransaction suspended = null;

		// get current transaction from ThreadLocal or equivalent
		SpiTransaction t = transactionScopeManager.get();

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
	public SpiTransaction getCurrentServerTransaction() {
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
		SpiTransaction t = transactionManager.createTransaction(true, -1);
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
		SpiTransaction t = transactionManager.createTransaction(true, isolation.getLevel());
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
		return desc.nextId();
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
		return createNamedQuery(beanType, namedQuery);
	}
	
	public <T> Query<T> createNamedQuery(Class<T> beanType, String namedQuery) throws PersistenceException {

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
			String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
			throw new PersistenceException(m);
		}
		return new ElFilter<T>(desc);
	}

	public <T> Query<T> find(Class<T> beanType) {
		return createQuery(beanType);
	}

	public <T> Query<T> createQuery(Class<T> beanType) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
			throw new PersistenceException(m);
		}
		if (desc.isSqlSelectBased()) {
			// use the "default" SqlSelect
			DeployNamedQuery defaultSqlSelect = desc.getNamedQuery("default");
			return new DefaultOrmQuery<T>(beanType, this, defaultSqlSelect);

		} else {
			return new DefaultOrmQuery<T>(beanType, this);
		}
	}

	public <T> Update<T> createNamedUpdate(Class<T> beanType, String namedUpdate) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
			throw new PersistenceException(m);
		}

		DeployNamedUpdate deployUpdate = desc.getNamedUpdate(namedUpdate);
		if (deployUpdate == null) {
			throw new PersistenceException("named update " + namedUpdate + " was not found for " + desc.getFullName());
		}

		return new DefaultOrmUpdate<T>(beanType, this, desc.getBaseTable(), deployUpdate);
	}

	public <T> Update<T> createUpdate(Class<T> beanType, String ormUpdate) {
		BeanDescriptor<?> desc = getBeanDescriptor(beanType);
		if (desc == null) {
			String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
			throw new PersistenceException(m);
		}

		return new DefaultOrmUpdate<T>(beanType, this, desc.getBaseTable(), ormUpdate);
	}

	public SqlQuery createSqlQuery(String sql) {
		return new DefaultRelationalQuery(this, sql);
	}

	public SqlQuery createNamedSqlQuery(String namedQuery) {
		DNativeQuery nq = beanDescriptorManager.getNativeQuery(namedQuery);
		if (nq == null) {
			throw new PersistenceException("SqlQuery " + namedQuery + " not found.");
		}
		return new DefaultRelationalQuery(this, nq.getQuery());
	}

	public SqlUpdate createSqlUpdate(String sql) {
		return new DefaultSqlUpdate(this, sql);
	}

	public CallableSql createCallableSql(String sql) {
		return new DefaultCallableSql(this, sql);
	}

	
	public SqlUpdate createNamedSqlUpdate(String namedQuery) {
		DNativeQuery nq = beanDescriptorManager.getNativeQuery(namedQuery);
		if (nq == null) {
			throw new PersistenceException("SqlUpdate " + namedQuery + " not found.");
		}
		return new DefaultSqlUpdate(this, nq.getQuery());
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

		SpiQuery<T> query = (SpiQuery<T>) q;
		BeanDescriptor<T> desc = beanDescriptorManager.getBeanDescriptor(query.getBeanType());

		if (desc.isAutoFetchTunable() && !query.isSqlSelect()) {
			// its a tunable query
			autoFetchManager.tuneQuery(query);
		}
		SpiTransaction serverTrans = (SpiTransaction)t;
		OrmQueryRequest<T> request = new OrmQueryRequest<T>(this, queryEngine, query, desc, serverTrans);
		
		
		if (query.hasMaxRowsOrFirstRow()
				&& !request.isSqlSelect() 
				&& query.getBackgroundFetchAfter() == 0) {
			// ensure there are no joins to Many's so that  
			// limit offset type SQL clauses work etc
			query.removeManyJoins();
			
			if (query.isManyInWhere()){
				query.setDistinct(true);
			}
		}
		
		// the query hash after an AutoFetch tuning
		request.calculateQueryPlanHash();
		return request;
	}

	@SuppressWarnings("unchecked")
	private <T> T findId(Query<T> query, Transaction t) {
		
		OrmQueryRequest<T> request = createQueryRequest(query, t);
		
		// First have a look in the persistence context and then
		// the bean cache (if we are using caching)
		T bean = request.getFromPersistenceContextOrCache();
		if (bean != null){
			return bean;
		}
				
		try {
			request.initTransIfRequired();

			bean = (T) request.findId();
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
		SpiQuery<T> q = (SpiQuery<T>) query;
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
		    
    	Object result = request.getFromQueryCache();
    	if (result != null){
    		return (Set<T>)result;
    	}
		
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
		    
    	Object result = request.getFromQueryCache();
    	if (result != null){
    		return (Map<?, T>)result;
    	}
		
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

	public <T> int findRowCount(Query<T> query, Transaction t){
		
		SpiQuery<T> copy = ((SpiQuery<T>)query).copy();
		
		OrmQueryRequest<T> request = createQueryRequest(copy, t);
		try {
			request.initTransIfRequired();
			int rowCount = request.findRowCount();
			request.endTransIfRequired();

			return rowCount;
			
		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public <T> List<Object> findIds(Query<T> query, Transaction t){
		
		SpiQuery<T> copy = ((SpiQuery<T>)query).copy();

		return findIdsWithCopy(copy, t);
	}
	
	public <T> List<Object> findIdsWithCopy(Query<T> query, Transaction t){
			
		OrmQueryRequest<T> request = createQueryRequest(query, t);
		try {
			request.initTransIfRequired();
			List<Object> list = request.findIds();
			request.endTransIfRequired();

			return list;
			
		} catch (RuntimeException ex) {
			request.rollbackTransIfRequired();
			throw ex;
		}
	}

	public <T> FutureRowCount<T> findFutureRowCount(Query<T> query, Transaction t) {

		SpiQuery<T> spiQuery = (SpiQuery<T>)query;
		spiQuery.setFutureFetch(true);
				
		Transaction newTxn = createTransaction();
		CallableQueryRowCount<T> call = new CallableQueryRowCount<T>(this, query, newTxn);
		
		FutureTask<Integer> futureTask = new FutureTask<Integer>(call);
		
		QueryFutureRowCount<T> queryFuture = new QueryFutureRowCount<T>(query, futureTask);
		backgroundExecutor.execute(futureTask);
		
		return queryFuture;
	}
	
	public <T> FutureIds<T> findFutureIds(Query<T> query, Transaction t) {

		SpiQuery<T> copy = ((SpiQuery<T>)query).copy();
		copy.setFutureFetch(true);
		
		// this is the list we will put the id's in ... create it now so
		// it is available for other threads to read while the id query
		// is still executing (we don't need to wait for it to finish)
		List<Object> idList = Collections.synchronizedList(new ArrayList<Object>());
		copy.setIdList(idList);
		
		
		Transaction newTxn = createTransaction();
		
		CallableQueryIds<T> call = new CallableQueryIds<T>(this, copy, newTxn);
		FutureTask<List<Object>> futureTask = new FutureTask<List<Object>>(call);
		
		QueryFutureIds<T> queryFuture = new QueryFutureIds<T>(copy, futureTask);
		
		backgroundExecutor.execute(futureTask);
		
		return queryFuture;
	}
	
	public <T> FutureList<T> findFutureList(Query<T> query, Transaction t) {

		SpiQuery<T> spiQuery = (SpiQuery<T>)query;
		spiQuery.setFutureFetch(true);
		
		if (spiQuery.getPersistenceContext() == null){
			if (t != null){
				spiQuery.setPersistenceContext(((SpiTransaction)t).getPersistenceContext());
			} else {
				SpiTransaction st = getCurrentServerTransaction();
				if (st != null){
					spiQuery.setPersistenceContext(st.getPersistenceContext());					
				}
			}
		}
		
		Transaction newTxn = createTransaction();
		CallableQueryList<T> call = new CallableQueryList<T>(this, query, newTxn);
		
		FutureTask<List<T>> futureTask = new FutureTask<List<T>>(call);
		
		backgroundExecutor.execute(futureTask);
		
		return new QueryFutureList<T>(query, futureTask);
	}

	public <T> PagingList<T> findPagingList(Query<T> query, Transaction t, int pageSize) {

		SpiQuery<T> spiQuery = (SpiQuery<T>)query;
		
		// we want to use a single PersistenceContext to be used
		// for all the paging queries so we make sure there is a 
		// PersistenceContext on the query
		PersistenceContext pc = spiQuery.getPersistenceContext();
		if (pc == null){
			SpiTransaction currentTransaction = getCurrentServerTransaction();
			if (currentTransaction != null){
				pc = currentTransaction.getPersistenceContext();
			}
			if (pc == null){
				pc = new DefaultPersistenceContext();
			}
			spiQuery.setPersistenceContext(pc);
		}
		
		return new LimitOffsetPagingQuery<T>(this, spiQuery, pageSize);
	}
	
	@SuppressWarnings("unchecked")
	public <T> List<T> findList(Query<T> query, Transaction t) {

		OrmQueryRequest<T> request = createQueryRequest(query, t);
		    
    	Object result = request.getFromQueryCache();
    	if (result != null){
    		return (List<T>)result;
    	}
    
		try {
			request.initTransIfRequired();
			List<T> list = request.findList();
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

	public SqlFutureList findFutureList(SqlQuery query, Transaction t) {

		SpiSqlQuery spiQuery = (SpiSqlQuery)query;
		spiQuery.setFutureFetch(true);
				
		Transaction newTxn = createTransaction();
		CallableSqlQueryList call = new CallableSqlQueryList(this, query, newTxn);
		
		FutureTask<List<SqlRow>> futureTask = new FutureTask<List<SqlRow>>(call);
		
		backgroundExecutor.execute(futureTask);
		
		return new SqlQueryFutureList(query, futureTask);
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
			SpiTransaction trans = wrap.transaction;
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
			SpiTransaction trans = wrap.transaction;
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
	
	public void register(BeanPersistController c) {
		List<BeanDescriptor<?>> list = beanDescriptorManager.getBeanDescriptorList();
		for (int i = 0; i < list.size(); i++) {
			list.get(i).register(c);
		}
	}
	
	public void deregister(BeanPersistController c) {
		List<BeanDescriptor<?>> list = beanDescriptorManager.getBeanDescriptorList();
		for (int i = 0; i < list.size(); i++) {
			list.get(i).deregister(c);
		}
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
	public void remoteTransactionEvent(RemoteTransactionEvent event) {
		transactionManager.remoteTransactionEvent(event);
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
			return new TransWrapper((SpiTransaction) t, false);
		}

		boolean wasCreated = false;
		SpiTransaction trans = transactionScopeManager.get();
		if (trans == null) {
			// create a transaction
			trans = transactionManager.createTransaction(false, -1);
			wasCreated = true;
		}
		return new TransWrapper(trans, wasCreated);
	}

	public SpiTransaction createServerTransaction(boolean isExplicit, int isolationLevel) {
		return transactionManager.createTransaction(isExplicit, isolationLevel);
	}

	public SpiTransaction createQueryTransaction() {
		return transactionManager.createQueryTransaction();
	}
}
