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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import com.avaje.ebean.Ebean;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.Filter;
import com.avaje.ebean.FutureIds;
import com.avaje.ebean.FutureList;
import com.avaje.ebean.FutureRowCount;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.avaje.ebean.QueryIterator;
import com.avaje.ebean.QueryResultVisitor;
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
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.CallStack;
import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.bean.PersistenceContext;
import com.avaje.ebean.cache.ServerCacheManager;
import com.avaje.ebean.config.EncryptKeyManager;
import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.ldap.LdapConfig;
import com.avaje.ebean.config.lucene.LuceneIndex;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanQueryAdapter;
import com.avaje.ebean.text.csv.CsvReader;
import com.avaje.ebean.text.json.JsonContext;
import com.avaje.ebean.text.json.JsonElement;
import com.avaje.ebeaninternal.api.LoadBeanRequest;
import com.avaje.ebeaninternal.api.LoadManyRequest;
import com.avaje.ebeaninternal.api.ScopeTrans;
import com.avaje.ebeaninternal.api.SpiBackgroundExecutor;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.api.SpiQuery.Mode;
import com.avaje.ebeaninternal.api.SpiQuery.Type;
import com.avaje.ebeaninternal.api.SpiSqlQuery;
import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.api.TransactionEventTable;
import com.avaje.ebeaninternal.server.autofetch.AutoFetchManager;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptorManager;
import com.avaje.ebeaninternal.server.deploy.BeanManager;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.deploy.DNativeQuery;
import com.avaje.ebeaninternal.server.deploy.DeployNamedQuery;
import com.avaje.ebeaninternal.server.deploy.DeployNamedUpdate;
import com.avaje.ebeaninternal.server.deploy.InheritInfo;
import com.avaje.ebeaninternal.server.el.ElFilter;
import com.avaje.ebeaninternal.server.jmx.MAdminAutofetch;
import com.avaje.ebeaninternal.server.ldap.DefaultLdapOrmQuery;
import com.avaje.ebeaninternal.server.ldap.LdapOrmQueryEngine;
import com.avaje.ebeaninternal.server.ldap.LdapOrmQueryRequest;
import com.avaje.ebeaninternal.server.ldap.expression.LdapExpressionFactory;
import com.avaje.ebeaninternal.server.lib.ShutdownManager;
import com.avaje.ebeaninternal.server.loadcontext.DLoadContext;
import com.avaje.ebeaninternal.server.lucene.LuceneIndexManager;
import com.avaje.ebeaninternal.server.query.CQuery;
import com.avaje.ebeaninternal.server.query.CQueryEngine;
import com.avaje.ebeaninternal.server.query.CallableQueryIds;
import com.avaje.ebeaninternal.server.query.CallableQueryList;
import com.avaje.ebeaninternal.server.query.CallableQueryRowCount;
import com.avaje.ebeaninternal.server.query.CallableSqlQueryList;
import com.avaje.ebeaninternal.server.query.LimitOffsetPagingQuery;
import com.avaje.ebeaninternal.server.query.QueryFutureIds;
import com.avaje.ebeaninternal.server.query.QueryFutureList;
import com.avaje.ebeaninternal.server.query.QueryFutureRowCount;
import com.avaje.ebeaninternal.server.query.SqlQueryFutureList;
import com.avaje.ebeaninternal.server.querydefn.DefaultOrmQuery;
import com.avaje.ebeaninternal.server.querydefn.DefaultOrmUpdate;
import com.avaje.ebeaninternal.server.querydefn.DefaultRelationalQuery;
import com.avaje.ebeaninternal.server.querydefn.NaturalKeyBindParam;
import com.avaje.ebeaninternal.server.text.csv.TCsvReader;
import com.avaje.ebeaninternal.server.transaction.DefaultPersistenceContext;
import com.avaje.ebeaninternal.server.transaction.RemoteTransactionEvent;
import com.avaje.ebeaninternal.server.transaction.TransactionManager;
import com.avaje.ebeaninternal.server.transaction.TransactionScopeManager;
import com.avaje.ebeaninternal.util.ParamTypeHelper;
import com.avaje.ebeaninternal.util.ParamTypeHelper.TypeInfo;

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

  private final DatabasePlatform databasePlatform;

  private final AdminLogging adminLogging;

  private final AdminAutofetch adminAutofetch;

  private final TransactionManager transactionManager;

  private final TransactionScopeManager transactionScopeManager;

  private final int maxCallStack;

  /**
   * Ebean defaults this to true but for EJB compatible behaviour set this to
   * false;
   */
  private final boolean rollbackOnChecked;
  private final boolean defaultDeleteMissingChildren;
  private final boolean defaultUpdateNullProperties;

  /**
   * Set to true if vanilla objects should be returned by default from queries
   * (with dynamic subclassing).
   */
  private final boolean vanillaMode;
  private final boolean vanillaRefMode;

  private final LdapOrmQueryEngine ldapQueryEngine;

  /**
   * Handles the save, delete, updateSql CallableSql.
   */
  private final Persister persister;

  private final OrmQueryEngine queryEngine;

  private final RelationalQueryEngine relationalQueryEngine;

  private final ServerCacheManager serverCacheManager;

  private final BeanDescriptorManager beanDescriptorManager;

  private final DiffHelp diffHelp = new DiffHelp();

  private final AutoFetchManager autoFetchManager;

  private final CQueryEngine cqueryEngine;

  private final DdlGenerator ddlGenerator;

  private final ExpressionFactory ldapExpressionFactory = new LdapExpressionFactory();

  private final ExpressionFactory expressionFactory;

  private final SpiBackgroundExecutor backgroundExecutor;

  private final DefaultBeanLoader beanLoader;

  private final EncryptKeyManager encryptKeyManager;

  private final JsonContext jsonContext;

  private final LuceneIndexManager luceneIndexManager;

  /**
   * The MBean name used to register Ebean.
   */
  private String mbeanName;

  /**
   * The MBeanServer Ebean is registered with.
   */
  private MBeanServer mbeanServer;

  /**
   * The default batch size for lazy loading beans or collections.
   */
  private int lazyLoadBatchSize;

  /** The query batch size */
  private int queryBatchSize;
  /**
   * JDBC driver specific handling for JDBC batch execution.
   */
  private PstmtBatch pstmtBatch;

  /**
   * Create the DefaultServer.
   */
  public DefaultServer(InternalConfiguration config, ServerCacheManager cache) {

    this.vanillaMode = config.getServerConfig().isVanillaMode();
    this.vanillaRefMode = config.getServerConfig().isVanillaRefMode();

    this.serverCacheManager = cache;
    this.pstmtBatch = config.getPstmtBatch();
    this.databasePlatform = config.getDatabasePlatform();
    this.backgroundExecutor = config.getBackgroundExecutor();
    this.serverName = config.getServerConfig().getName();
    this.lazyLoadBatchSize = config.getServerConfig().getLazyLoadBatchSize();
    this.queryBatchSize = config.getServerConfig().getQueryBatchSize();
    this.cqueryEngine = config.getCQueryEngine();
    this.expressionFactory = config.getExpressionFactory();
    this.adminLogging = config.getLogControl();
    this.encryptKeyManager = config.getServerConfig().getEncryptKeyManager();

    this.beanDescriptorManager = config.getBeanDescriptorManager();
    beanDescriptorManager.setEbeanServer(this);

    this.maxCallStack = GlobalProperties.getInt("ebean.maxCallStack", 5);

    this.defaultUpdateNullProperties = "true"
        .equalsIgnoreCase(config.getServerConfig().getProperty("defaultUpdateNullProperties", "false"));
    this.defaultDeleteMissingChildren = "true".equalsIgnoreCase(config.getServerConfig()
        .getProperty("defaultDeleteMissingChildren", "true"));

    this.rollbackOnChecked = GlobalProperties.getBoolean("ebean.transaction.rollbackOnChecked", true);
    this.transactionManager = config.getTransactionManager();
    this.transactionScopeManager = config.getTransactionScopeManager();

    this.persister = config.createPersister(this);
    this.queryEngine = config.createOrmQueryEngine();
    this.relationalQueryEngine = config.createRelationalQueryEngine();

    this.autoFetchManager = config.createAutoFetchManager(this);
    this.adminAutofetch = new MAdminAutofetch(autoFetchManager);

    this.ddlGenerator = new DdlGenerator(this, config.getDatabasePlatform(), config.getServerConfig());
    this.beanLoader = new DefaultBeanLoader(this, config.getDebugLazyLoad());
    this.jsonContext = config.createJsonContext(this);

    LdapConfig ldapConfig = config.getServerConfig().getLdapConfig();
    if (ldapConfig == null) {
      this.ldapQueryEngine = null;
    } else {
      this.ldapQueryEngine = new LdapOrmQueryEngine(ldapConfig.isVanillaMode(), ldapConfig.getContextFactory());
    }

    this.luceneIndexManager = config.getLuceneIndexManager();
    luceneIndexManager.setServer(this);

    ShutdownManager.register(new Shutdown());
  }

  public LuceneIndexManager getLuceneIndexManager() {
    return luceneIndexManager;
  }

  public LuceneIndex getLuceneIndex(Class<?> beanType) {
    return luceneIndexManager.getIndex(beanType.getName());
  }

  public boolean isDefaultDeleteMissingChildren() {
    return defaultDeleteMissingChildren;
  }

  public boolean isDefaultUpdateNullProperties() {
    return defaultUpdateNullProperties;
  }

  public boolean isVanillaMode() {
    return vanillaMode;
  }

  public int getLazyLoadBatchSize() {
    return lazyLoadBatchSize;
  }

  public PstmtBatch getPstmtBatch() {
    return pstmtBatch;
  }

  public DatabasePlatform getDatabasePlatform() {
    return databasePlatform;
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

  /**
   * Run any initialisation required before registering with the ClusterManager.
   */
  public void initialise() {
    if (encryptKeyManager != null) {
      encryptKeyManager.initialise();
    }
    List<BeanDescriptor<?>> list = beanDescriptorManager.getBeanDescriptorList();
    for (int i = 0; i < list.size(); i++) {
      list.get(i).cacheInitialise();
    }

  }

  /**
   * Start any services after registering with the ClusterManager.
   */
  public void start() {
    luceneIndexManager.start();
  }

  public void registerMBeans(MBeanServer mbeanServer, int uniqueServerId) {

    this.mbeanServer = mbeanServer;
    this.mbeanName = "Ebean:server=" + serverName + uniqueServerId;

    ObjectName adminName;
    ObjectName autofethcName;
    try {
      adminName = new ObjectName(mbeanName + ",function=Logging");
      autofethcName = new ObjectName(mbeanName + ",key=AutoFetch");
    } catch (Exception e) {
      String msg = "Failed to register the JMX beans for Ebean server [" + serverName + "].";
      logger.log(Level.SEVERE, msg, e);
      return;
    }

    try {
      mbeanServer.registerMBean(adminLogging, adminName);
      mbeanServer.registerMBean(adminAutofetch, autofethcName);

    } catch (InstanceAlreadyExistsException e) {
      // tomcat webapp reloading
      String msg = "JMX beans for Ebean server [" + serverName + "] already registered. Will try unregister/register" + e.getMessage();
      logger.log(Level.WARNING, msg);
      try {
        mbeanServer.unregisterMBean(adminName);
        mbeanServer.unregisterMBean(autofethcName);
        // re-register
        mbeanServer.registerMBean(adminLogging, adminName);
        mbeanServer.registerMBean(adminAutofetch, autofethcName);

      } catch (Exception ae) {
        String amsg = "Unable to unregister/register the JMX beans for Ebean server [" + serverName + "].";
        logger.log(Level.SEVERE, amsg, ae);
      }
    } catch (Exception e) {
      String msg = "Error registering MBean[" + mbeanName + "]";
      logger.log(Level.SEVERE, msg, e);
    }
  }

  private final class Shutdown implements Runnable {
    public void run() {
      try {
        if (mbeanServer != null) {
          mbeanServer.unregisterMBean(new ObjectName(mbeanName + ",function=Logging"));
          mbeanServer.unregisterMBean(new ObjectName(mbeanName + ",key=AutoFetch"));
        }
      } catch (Exception e) {
        String msg = "Error unregistering Ebean " + mbeanName;
        logger.log(Level.SEVERE, msg, e);
      }

      // shutdown services
      transactionManager.shutdown();
      autoFetchManager.shutdown();
      backgroundExecutor.shutdown();
      // luceneIndexManager.shutdown();
    }
  }

  /**
   * Return the server name.
   */
  public String getName() {
    return serverName;
  }

  public BeanState getBeanState(Object bean) {
    if (bean instanceof EntityBean) {
      return new DefaultBeanState((EntityBean) bean);
    }
    // if using "subclassing" (not enhancement) this will
    // return null for 'vanilla' instances (not subclassed)
    return null;
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
    BeanDescriptor<?> desc = beanDescriptorManager.getBeanDescriptor(beanType);
    if (desc == null) {
      String msg = "Is " + beanType + " an entity? Could not find a BeanDescriptor";
      throw new PersistenceException(msg);
    } else {
      desc.runCacheWarming();
    }
  }

  /**
   * Compile a query. Only valid for ORM queries (not LDAP)
   */
  public <T> CQuery<T> compileQuery(Query<T> query, Transaction t) {
    SpiOrmQueryRequest<T> qr = createQueryRequest(Type.SUBQUERY, query, t);
    OrmQueryRequest<T> orm = (OrmQueryRequest<T>) qr;
    return cqueryEngine.buildQuery(orm);
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

  public void refreshMany(Object parentBean, String propertyName, Transaction t) {

    beanLoader.refreshMany(parentBean, propertyName, t);
  }

  public void refreshMany(Object parentBean, String propertyName) {

    beanLoader.refreshMany(parentBean, propertyName);
  }

  public void loadMany(LoadManyRequest loadRequest) {

    beanLoader.loadMany(loadRequest);
  }

  public void loadMany(BeanCollection<?> bc, boolean onlyIds) {

    beanLoader.loadMany(bc, null, onlyIds);
  }

  public void refresh(Object bean) {

    beanLoader.refresh(bean);
  }

  public void loadBean(LoadBeanRequest loadRequest) {

    beanLoader.loadBean(loadRequest);
  }

  public void loadBean(EntityBeanIntercept ebi) {

    beanLoader.loadBean(ebi);
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
   * This notifies this instance of the framework that beans have been committed
   * externally to it. Either by another framework or clustered server. It needs
   * to maintain its cache and lucene indexes appropriately.
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
    return (T) desc.createEntityBean();
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
   * If a current transaction is active then this will check the Context of that
   * transaction to see if the bean is already loaded. If it is already loaded
   * then it will returned that object.
   * </p>
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
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
        ref = desc.createReference(vanillaRefMode, null, id, null);
      }

      if (ctx != null && (ref instanceof EntityBean)) {
        // Not putting a vanilla reference in the persistence context
        ctx.put(id, ref);
      }
    }
    return (T) ref;
  }

  /**
   * Creates a new Transaction that is NOT stored in TransactionThreadLocal. Use
   * this when you want a thread to have a second independent transaction.
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
   * This will also potentially throw exceptions for MANDATORY and NEVER types.
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
      if (t == null) {
        throw new PersistenceException("Transaction missing when MANDATORY");
      }
      return true;

    case NEVER:
      if (t != null) {
        throw new PersistenceException("Transaction exists for Transactional NEVER");
      }
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
   * If the current transaction has already been committed do nothing otherwise
   * rollback the transaction.
   * <p>
   * Useful to put in a finally block to ensure the transaction is ended, rather
   * than a rollbackTransaction() in each catch block.
   * </p>
   * <p>
   * Code example:<br />
   * 
   * <pre>
   * &lt;code&gt;
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
   * &lt;/code&gt;
   * </pre>
   * 
   * </p>
   */
  public void endTransaction() {
    transactionScopeManager.end();
  }

  /**
   * return the next unique identity value.
   * <p>
   * Uses the BeanDescriptor deployment information to determine the sequence to
   * use.
   * </p>
   */
  public Object nextId(Class<?> beanType) {
    BeanDescriptor<?> desc = getBeanDescriptor(beanType);
    return desc.nextId(null);
  }

  @SuppressWarnings("unchecked")
  public <T> void sort(List<T> list, String sortByClause) {

    if (list == null) {
      throw new NullPointerException("list is null");
    }
    if (sortByClause == null) {
      throw new NullPointerException("sortByClause is null");
    }
    if (list.size() == 0) {
      // don't need to sort an empty list
      return;
    }
    // use first bean in the list as the correct type
    Class<T> beanType = (Class<T>) list.get(0).getClass();
    BeanDescriptor<T> beanDescriptor = getBeanDescriptor(beanType);
    if (beanDescriptor == null) {
      String m = "BeanDescriptor not found, is [" + beanType + "] an entity bean?";
      throw new PersistenceException(m);
    }
    beanDescriptor.sort(list, sortByClause);
  }

  public <T> Query<T> createQuery(Class<T> beanType) throws PersistenceException {
    return createQuery(beanType, null);
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
    return new DefaultOrmQuery<T>(beanType, this, expressionFactory, deployQuery);
  }

  public <T> Filter<T> filter(Class<T> beanType) {
    BeanDescriptor<T> desc = getBeanDescriptor(beanType);
    if (desc == null) {
      String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
      throw new PersistenceException(m);
    }
    return new ElFilter<T>(desc);
  }

  public <T> CsvReader<T> createCsvReader(Class<T> beanType) {
    BeanDescriptor<T> descriptor = getBeanDescriptor(beanType);
    if (descriptor == null) {
      throw new NullPointerException("BeanDescriptor for " + beanType.getName() + " not found");
    }
    return new TCsvReader<T>(this, descriptor);
  }

  public <T> Query<T> find(Class<T> beanType) {
    return createQuery(beanType);
  }

  public <T> Query<T> createQuery(Class<T> beanType, String query) {
    BeanDescriptor<?> desc = getBeanDescriptor(beanType);
    if (desc == null) {
      String m = beanType.getName() + " is NOT an Entity Bean registered with this server?";
      throw new PersistenceException(m);
    }
    switch (desc.getEntityType()) {
    case SQL:
      if (query != null) {
        throw new PersistenceException("You must used Named queries for this Entity " + desc.getFullName());
      }
      // use the "default" SqlSelect
      DeployNamedQuery defaultSqlSelect = desc.getNamedQuery("default");
      return new DefaultOrmQuery<T>(beanType, this, expressionFactory, defaultSqlSelect);

    case LDAP:
      return new DefaultLdapOrmQuery<T>(beanType, this, ldapExpressionFactory, query);

    default:
      return new DefaultOrmQuery<T>(beanType, this, expressionFactory, query);
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

  private <T> SpiOrmQueryRequest<T> createQueryRequest(Type type, Query<T> query, Transaction t) {

    SpiQuery<T> spiQuery = (SpiQuery<T>) query;
    spiQuery.setType(type);

    BeanDescriptor<T> desc = beanDescriptorManager.getBeanDescriptor(spiQuery.getBeanType());
    spiQuery.setBeanDescriptor(desc);

    return createQueryRequest(desc, spiQuery, t);
  }

  public <T> SpiOrmQueryRequest<T> createQueryRequest(BeanDescriptor<T> desc, SpiQuery<T> query, Transaction t) {

    if (desc.isLdapEntityType()) {
      return new LdapOrmQueryRequest<T>(query, desc, ldapQueryEngine);
    }

    if (desc.isAutoFetchTunable() && !query.isSqlSelect()) {
      // its a tunable query
      if (autoFetchManager.tuneQuery(query)) {
        // was automatically tuned by Autofetch
      } else {
        // use deployment FetchType.LAZY/EAGER annotations
        // to define the 'default' select clause
        query.setDefaultSelectClause();
      }
    }

    if (query.selectAllForLazyLoadProperty()) {
      // we need to select all properties to ensure the lazy load property
      // was included (was not included by default or via autofetch).
      if (logger.isLoggable(Level.FINE)) {
        logger.log(Level.FINE, "Using selectAllForLazyLoadProperty");
      }
    }

    if (true) {
      // if determine cost and no origin for Autofetch
      if (query.getParentNode() == null) {
        CallStack callStack = createCallStack();
        query.setOrigin(callStack);
      }
    }

    // determine extra joins required to support where clause
    // predicates on *ToMany properties
    if (query.initManyWhereJoins()) {
      // we need a sql distinct now
      query.setDistinct(true);
    }

    boolean allowOneManyFetch = true;
    if (Mode.LAZYLOAD_MANY.equals(query.getMode())) {
      allowOneManyFetch = false;

    } else if (query.hasMaxRowsOrFirstRow() && !query.isRawSql() && !query.isSqlSelect() && query.getBackgroundFetchAfter() == 0) {
      // convert ALL fetch joins to Many's to be query joins
      // so that limit offset type SQL clauses work
      allowOneManyFetch = false;
    }

    query.convertManyFetchJoinsToQueryJoins(allowOneManyFetch, queryBatchSize);

    SpiTransaction serverTrans = (SpiTransaction) t;
    OrmQueryRequest<T> request = new OrmQueryRequest<T>(this, queryEngine, query, desc, serverTrans);

    BeanQueryAdapter queryAdapter = desc.getQueryAdapter();
    if (queryAdapter != null) {
      // adaption of the query probably based on the
      // current user
      queryAdapter.preQuery(request);
    }

    // the query hash after any tuning
    request.calculateQueryPlanHash();

    return request;
  }

  /**
   * Try to get the object out of the persistence context.
   */
  @SuppressWarnings("unchecked")
  private <T> T findIdCheckPersistenceContextAndCache(Transaction transaction, BeanDescriptor<T> beanDescriptor, SpiQuery<T> query) {

    SpiTransaction t = (SpiTransaction) transaction;
    if (t == null) {
      t = getCurrentServerTransaction();
    }
    PersistenceContext context = null;
    if (t != null) {
      // first look in the persistence context
      context = t.getPersistenceContext();
      if (context != null) {
        Object o = context.get(beanDescriptor.getBeanType(), query.getId());
        if (o != null) {
          return (T) o;
        }
      }
    }

    if (!beanDescriptor.calculateUseCache(query.isUseBeanCache())) {
      // not using bean cache
      return null;
    }

    // boolean readOnly = beanDescriptor.calculateReadOnly(query.isReadOnly());
    boolean vanilla = query.isVanillaMode(vanillaMode);
    Object cachedBean = beanDescriptor.cacheGetBean(query.getId(), vanilla, query.isReadOnly());
    if (cachedBean != null) {
      if (context == null) {
        context = new DefaultPersistenceContext();

      }
      context.put(query.getId(), cachedBean);
      if (!vanilla) {

        DLoadContext loadContext = new DLoadContext(this, beanDescriptor, query.isReadOnly(), false, null, false);
        loadContext.setPersistenceContext(context);

        EntityBeanIntercept ebi = ((EntityBean) cachedBean)._ebean_getIntercept();
        ebi.setPersistenceContext(context);
        loadContext.register(null, ebi);

      }
    }

    return (T) cachedBean;
  }
    
  @SuppressWarnings("unchecked")
  private <T> T findId(Query<T> query, Transaction t) {

    SpiQuery<T> spiQuery = (SpiQuery<T>) query;
    spiQuery.setType(Type.BEAN);

    BeanDescriptor<T> desc = beanDescriptorManager.getBeanDescriptor(spiQuery.getBeanType());
    spiQuery.setBeanDescriptor(desc);

    if (SpiQuery.Mode.NORMAL.equals(spiQuery.getMode()) && !spiQuery.isLoadBeanCache()) {
      // See if we can skip doing the fetch completely by getting the bean from the
      // persistence context or the bean cache
      T bean = findIdCheckPersistenceContextAndCache(t, desc, spiQuery);
      if (bean != null) {
        return bean;
      }
    }

    SpiOrmQueryRequest<T> request = createQueryRequest(desc, spiQuery, t);

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
    SpiQuery<T> q = (SpiQuery<T>) query;
    Object id = q.getId();
    if (id != null) {
      return findId(query, t);
    }

    BeanDescriptor<T> desc = beanDescriptorManager.getBeanDescriptor(q.getBeanType());

    if (desc.calculateUseNaturalKeyCache(q.isUseBeanCache())) {
      // check if it is a find by unique id
      NaturalKeyBindParam keyBindParam = q.getNaturalKeyBindParam();
      if (keyBindParam != null && desc.cacheIsNaturalKey(keyBindParam.getName())) {
        Object id2 = desc.cacheGetNaturalKeyId(keyBindParam.getValue());
        if (id2 != null) {
          SpiQuery<T> copy = q.copy();
          copy.convertWhereNaturalKeyToId(id2);
          return findId(copy, t);
        }
      }
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

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> Set<T> findSet(Query<T> query, Transaction t) {

    SpiOrmQueryRequest request = createQueryRequest(Type.SET, query, t);

    Object result = request.getFromQueryCache();
    if (result != null) {
      return (Set<T>) result;
    }

    try {
      request.initTransIfRequired();
      Set<T> set = (Set<T>) request.findSet();
      request.endTransIfRequired();

      return set;

    } catch (RuntimeException ex) {
      // String stackTrace = throwablePrinter.print(ex);
      request.rollbackTransIfRequired();
      throw ex;
    }
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T> Map<?, T> findMap(Query<T> query, Transaction t) {

    SpiOrmQueryRequest request = createQueryRequest(Type.MAP, query, t);

    Object result = request.getFromQueryCache();
    if (result != null) {
      return (Map<?, T>) result;
    }

    try {
      request.initTransIfRequired();
      Map<?, T> map = (Map<?, T>) request.findMap();
      request.endTransIfRequired();

      return map;

    } catch (RuntimeException ex) {
      // String stackTrace = throwablePrinter.print(ex);
      request.rollbackTransIfRequired();
      throw ex;
    }
  }

  public <T> int findRowCount(Query<T> query, Transaction t) {

    SpiQuery<T> copy = ((SpiQuery<T>) query).copy();
    return findRowCountWithCopy(copy, t);
  }

  public <T> int findRowCountWithCopy(Query<T> query, Transaction t) {

    SpiOrmQueryRequest<T> request = createQueryRequest(Type.ROWCOUNT, query, t);
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

  public <T> List<Object> findIds(Query<T> query, Transaction t) {

    SpiQuery<T> copy = ((SpiQuery<T>) query).copy();

    return findIdsWithCopy(copy, t);
  }

  public <T> List<Object> findIdsWithCopy(Query<T> query, Transaction t) {

    SpiOrmQueryRequest<T> request = createQueryRequest(Type.ID_LIST, query, t);
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

  public <T> FutureRowCount<T> findFutureRowCount(Query<T> q, Transaction t) {

    SpiQuery<T> copy = ((SpiQuery<T>) q).copy();
    copy.setFutureFetch(true);

    Transaction newTxn = createTransaction();

    CallableQueryRowCount<T> call = new CallableQueryRowCount<T>(this, copy, newTxn);
    FutureTask<Integer> futureTask = new FutureTask<Integer>(call);

    QueryFutureRowCount<T> queryFuture = new QueryFutureRowCount<T>(copy, futureTask);
    backgroundExecutor.execute(futureTask);

    return queryFuture;
  }

  public <T> FutureIds<T> findFutureIds(Query<T> query, Transaction t) {

    SpiQuery<T> copy = ((SpiQuery<T>) query).copy();
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

    SpiQuery<T> spiQuery = (SpiQuery<T>) query;
    spiQuery.setFutureFetch(true);

    if (spiQuery.getPersistenceContext() == null) {
      if (t != null) {
        spiQuery.setPersistenceContext(((SpiTransaction) t).getPersistenceContext());
      } else {
        SpiTransaction st = getCurrentServerTransaction();
        if (st != null) {
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

    SpiQuery<T> spiQuery = (SpiQuery<T>) query;

    // we want to use a single PersistenceContext to be used
    // for all the paging queries so we make sure there is a
    // PersistenceContext on the query
    PersistenceContext pc = spiQuery.getPersistenceContext();
    if (pc == null) {
      SpiTransaction currentTransaction = getCurrentServerTransaction();
      if (currentTransaction != null) {
        pc = currentTransaction.getPersistenceContext();
      }
      if (pc == null) {
        pc = new DefaultPersistenceContext();
      }
      spiQuery.setPersistenceContext(pc);
    }

    return new LimitOffsetPagingQuery<T>(this, spiQuery, pageSize);
  }

  public <T> void findVisit(Query<T> query, QueryResultVisitor<T> visitor, Transaction t) {

    SpiOrmQueryRequest<T> request = createQueryRequest(Type.LIST, query, t);

    try {
      request.initTransIfRequired();
      request.findVisit(visitor);

    } catch (RuntimeException ex) {
      request.rollbackTransIfRequired();
      throw ex;
    }
  }

  public <T> QueryIterator<T> findIterate(Query<T> query, Transaction t) {

    SpiOrmQueryRequest<T> request = createQueryRequest(Type.LIST, query, t);

    try {
      request.initTransIfRequired();
      return request.findIterate();
      // request.endTransIfRequired();

    } catch (RuntimeException ex) {
      request.rollbackTransIfRequired();
      throw ex;
    }
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> findList(Query<T> query, Transaction t) {

    SpiOrmQueryRequest<T> request = createQueryRequest(Type.LIST, query, t);

    Object result = request.getFromQueryCache();
    if (result != null) {
      return (List<T>) result;
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

    SpiSqlQuery spiQuery = (SpiSqlQuery) query;
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
   * Force an update using the bean updating non-null properties.
   */
  public void update(Object bean) {
    update(bean, null, null);
  }

  /**
   * Force an update using the bean explicitly stating which properties to
   * include in the update.
   */
  public void update(Object bean, Set<String> updateProps) {
    update(bean, updateProps, null);
  }

  /**
   * Force an update using the bean updating non-null properties.
   */
  public void update(Object bean, Transaction t) {
    update(bean, null, t);
  }

  /**
   * Force an update using the bean explicitly stating which properties to
   * include in the update.
   */
  public void update(Object bean, Set<String> updateProps, Transaction t) {
    update(bean, updateProps, t, defaultDeleteMissingChildren, defaultUpdateNullProperties);
  }

  /**
   * Force an update using the bean explicitly stating which properties to
   * include in the update.
   */
  public void update(Object bean, Set<String> updateProps, Transaction t, boolean deleteMissingChildren, boolean updateNullProperties) {
    if (bean == null) {
      throw new NullPointerException(Message.msg("bean.isnull"));
    }
    persister.forceUpdate(bean, updateProps, t, deleteMissingChildren, updateNullProperties);
  }

  /**
   * Force the bean to be saved with an explicit insert.
   * <p>
   * Typically you would use save() and let Ebean determine if the bean should
   * be inserted or updated. This can be useful when you are transferring data
   * between databases and want to explicitly insert a bean into a different
   * database that it came from.
   * </p>
   */
  public void insert(Object bean) {
    insert(bean, null);
  }

  /**
   * Force the bean to be saved with an explicit insert.
   * <p>
   * Typically you would use save() and let Ebean determine if the bean should
   * be inserted or updated. This can be useful when you are transferring data
   * between databases and want to explicitly insert a bean into a different
   * database that it came from.
   * </p>
   */
  public void insert(Object bean, Transaction t) {
    if (bean == null) {
      throw new NullPointerException(Message.msg("bean.isnull"));
    }
    persister.forceInsert(bean, t);
  }

  /**
   * Delete the associations (from the intersection table) of a ManyToMany given
   * the owner bean and the propertyName of the ManyToMany collection.
   * <p>
   * This returns the number of associations deleted.
   * </p>
   */
  public int deleteManyToManyAssociations(Object ownerBean, String propertyName) {
    return deleteManyToManyAssociations(ownerBean, propertyName, null);
  }

  /**
   * Delete the associations (from the intersection table) of a ManyToMany given
   * the owner bean and the propertyName of the ManyToMany collection.
   * <p>
   * This returns the number of associations deleted.
   * </p>
   */
  public int deleteManyToManyAssociations(Object ownerBean, String propertyName, Transaction t) {

    TransWrapper wrap = initTransIfRequired(t);
    try {
      SpiTransaction trans = wrap.transaction;
      int rc = persister.deleteManyToManyAssociations(ownerBean, propertyName, trans);
      wrap.commitIfCreated();
      return rc;

    } catch (RuntimeException e) {
      wrap.rollbackIfCreated();
      throw e;
    }
  }

  /**
   * Save the associations of a ManyToMany given the owner bean and the
   * propertyName of the ManyToMany collection.
   */
  public void saveManyToManyAssociations(Object ownerBean, String propertyName) {
    saveManyToManyAssociations(ownerBean, propertyName, null);
  }

  /**
   * Save the associations of a ManyToMany given the owner bean and the
   * propertyName of the ManyToMany collection.
   */
  public void saveManyToManyAssociations(Object ownerBean, String propertyName, Transaction t) {

    TransWrapper wrap = initTransIfRequired(t);
    try {
      SpiTransaction trans = wrap.transaction;

      persister.saveManyToManyAssociations(ownerBean, propertyName, trans);

      wrap.commitIfCreated();

    } catch (RuntimeException e) {
      wrap.rollbackIfCreated();
      throw e;
    }
  }

  public void saveAssociation(Object ownerBean, String propertyName) {
    saveAssociation(ownerBean, propertyName, null);
  }

  public void saveAssociation(Object ownerBean, String propertyName, Transaction t) {

    if (ownerBean instanceof EntityBean) {
      Set<String> loadedProps = ((EntityBean) ownerBean)._ebean_getIntercept().getLoadedProps();
      if (loadedProps != null && !loadedProps.contains(propertyName)) {
        // skip as property is not actually loaded in this partially
        // loaded bean
        logger.fine("Skip saveAssociation as property " + propertyName + " is not loaded");
        return;
      }
    }

    TransWrapper wrap = initTransIfRequired(t);
    try {
      SpiTransaction trans = wrap.transaction;

      persister.saveAssociation(ownerBean, propertyName, trans);

      wrap.commitIfCreated();

    } catch (RuntimeException e) {
      wrap.rollbackIfCreated();
      throw e;
    }
  }

  /**
   * Perform an update or insert on each bean in the iterator. Returns the
   * number of beans that where saved.
   */
  public int save(Iterator<?> it) {
    return save(it, null);
  }

  /**
   * Perform an update or insert on each bean in the collection. Returns the
   * number of beans that where saved.
   */
  public int save(Collection<?> c) {
    return save(c.iterator(), null);
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

  public int delete(Class<?> beanType, Object id) {
    return delete(beanType, id, null);
  }

  public int delete(Class<?> beanType, Object id, Transaction t) {

    TransWrapper wrap = initTransIfRequired(t);
    try {
      SpiTransaction trans = wrap.transaction;
      int rowCount = persister.delete(beanType, id, trans);
      wrap.commitIfCreated();

      return rowCount;

    } catch (RuntimeException e) {
      wrap.rollbackIfCreated();
      throw e;
    }
  }

  public void delete(Class<?> beanType, Collection<?> ids) {
    delete(beanType, ids, null);
  }

  public void delete(Class<?> beanType, Collection<?> ids, Transaction t) {

    TransWrapper wrap = initTransIfRequired(t);
    try {
      SpiTransaction trans = wrap.transaction;
      persister.deleteMany(beanType, ids, trans);
      wrap.commitIfCreated();

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
   * Delete all the beans in the collection.
   */
  public int delete(Collection<?> c) {
    return delete(c.iterator(), null);
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
  public int execute(CallableSql callSql, Transaction t) {
    return persister.executeCallable(callSql, t);
  }

  /**
   * Execute the CallableSql.
   */
  public int execute(CallableSql callSql) {
    return execute(callSql, null);
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

  public boolean isSupportedType(java.lang.reflect.Type genericType) {

    TypeInfo typeInfo = ParamTypeHelper.getTypeInfo(genericType);
    if (typeInfo == null) {
      return false;
    }
    Class<?> beanType = typeInfo.getBeanType();
    if (JsonElement.class.isAssignableFrom(beanType)) {
      return true;
    }
    return getBeanDescriptor(typeInfo.getBeanType()) != null;
  }

  public Object getBeanId(Object bean) {
    BeanDescriptor<?> desc = getBeanDescriptor(bean.getClass());
    if (desc == null) {
      String m = bean.getClass().getName() + " is NOT an Entity Bean registered with this server?";
      throw new PersistenceException(m);
    }

    return desc.getId(bean);
  }

  /**
   * Return the BeanDescriptor for a given type of bean.
   */
  public <T> BeanDescriptor<T> getBeanDescriptor(Class<T> beanClass) {
    return beanDescriptorManager.getBeanDescriptor(beanClass);
  }

  /**
   * Return the BeanDescriptor's for a given table name.
   */
  public List<BeanDescriptor<?>> getBeanDescriptors(String tableName) {
    return beanDescriptorManager.getBeanDescriptors(tableName);
  }

  /**
   * Return the BeanDescriptor using its unique id.
   */
  public BeanDescriptor<?> getBeanDescriptorById(String descriptorId) {
    return beanDescriptorManager.getBeanDescriptorById(descriptorId);
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
   * Returns a TransWrapper which contains the wasCreated flag. If this is true
   * then the transaction was created for this request in which case it will
   * need to be committed after the request has been processed.
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

  private static final int IGNORE_LEADING_ELEMENTS = 5;
  private static final String AVAJE_EBEAN = Ebean.class.getName().substring(0, 15);

  /**
   * Create a CallStack object.
   * <p>
   * This trims off the avaje ebean part of the stack trace so that the first
   * element in the CallStack should be application code.
   * </p>
   */
  public CallStack createCallStack() {

    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    // ignore the first 6 as they are always avaje stack elements
    int startIndex = IGNORE_LEADING_ELEMENTS;

    // find the first non-avaje stackElement
    for (; startIndex < stackTrace.length; startIndex++) {
      if (!stackTrace[startIndex].getClassName().startsWith(AVAJE_EBEAN)) {
        break;
      }
    }

    int stackLength = stackTrace.length - startIndex;
    if (stackLength > maxCallStack) {
      // maximum of maxCallStack stackTrace elements
      stackLength = maxCallStack;
    }

    // create the 'interesting' part of the stackTrace
    StackTraceElement[] finalTrace = new StackTraceElement[stackLength];
    for (int i = 0; i < stackLength; i++) {
      finalTrace[i] = stackTrace[i + startIndex];
    }

    if (stackLength < 1) {
      // this should not really happen
      throw new RuntimeException("StackTraceElement size 0?  stack: " + Arrays.toString(stackTrace));
    }

    return new CallStack(finalTrace);
  }

  public JsonContext createJsonContext() {
    // immutable thread safe so return shared instance
    return jsonContext;
  }

}
