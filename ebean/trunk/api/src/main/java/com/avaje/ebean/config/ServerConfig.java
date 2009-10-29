/**
 * Copyright (C) 2006  Authors
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
package com.avaje.ebean.config;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.AdminLogging.StmtLogLevel;
import com.avaje.ebean.AdminLogging.TxLogLevel;
import com.avaje.ebean.AdminLogging.TxLogSharing;
import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.event.BeanPersistController;
import com.avaje.ebean.event.BeanPersistListener;

/**
 * The configuration used for creating a EbeanServer.
 * <p>
 * Used to programmatically construct an EbeanServer and optionally register
 * it with the Ebean singleton.
 * </p>
 * <p>
 * If you just use Ebean without this programmatic configuration Ebean will read
 * the ebean.properties file and take the configuration from there. This usually
 * includes searching the class path and automatically registering any entity 
 * classes and listeners etc.
 * </p>
 * 
 * <pre class="code">
 * ServerConfig c = new ServerConfig();
 * c.setName("ordh2");
 * 
 * // read the ebean.properties and load 
 * // those settings into this serverConfig object 
 * c.loadFromProperties();
 * 
 * // generate DDL and run it
 * c.setDdlGenerate(true);
 * c.setDdlRun(true);
 * 
 * // add any classes found in the app.data package
 * c.addPackage("app.data");
 * 
 * // register as the 'Default' server
 * c.setDefaultServer(true);
 * 
 * EbeanServer server = EbeanServerFactory.create(c);
 * 
 * </pre>
 * 
 * @see EbeanServerFactory
 * 
 * @author emcgreal
 * @author rbygrave 
 */
public class ServerConfig {

	/** The name. */
	private String name;

	/** The resource directory. */
	private String resourceDirectory;

	/** The enhance log level. */
	private int enhanceLogLevel;

	/** true to register this EbeanServer with the Ebean singleton. */
	private boolean register = true;

	/** true if this is the default/primary server. */
	private boolean defaultServer;

	/** The validate on save. */
	private boolean validateOnSave = true;
	
	private boolean useJuliTransactionLogger;

	/** List of interesting classes such as entities, embedded, ScalarTypes, Listeners, Finders, Controllers etc. */
	private List<Class<?>> classes = new ArrayList<Class<?>>();

	/** The packages. */
	private List<String> packages = new ArrayList<String>();

	/** The autofetch config. */
	private AutofetchConfig autofetchConfig = new AutofetchConfig();

	/** The database platform name. */
	private String databasePlatformName;

	/** The database platform. */
	private DatabasePlatform databasePlatform;

	/** For DB's using sequences this is the number of sequence values prefetched */
	private int databaseSequenceBatchSize = 20;
	
	private boolean usePersistBatching;
	
	private int persistBatchSize = 20;
	
	/** The default batch size for lazy loading */
	private int loadBatchSize = 1;
	
	/** The ddl generate. */
	private boolean ddlGenerate;

	/** The ddl run. */
	private boolean ddlRun;

	/** The debug sql. */
	private boolean debugSql;

	/** The debug lazy load. */
	private boolean debugLazyLoad;

	/** The external transaction manager. */
	private ExternalTransactionManager externalTransactionManager;

	/** The transaction debug level. */
	private int transactionDebugLevel;

	/** The transaction log directory. */
	private String transactionLogDirectory = "logs";

	/** The transaction logging. */
	private TxLogLevel transactionLogging = TxLogLevel.ALL;

	/** The transaction log sharing. */
	private TxLogSharing transactionLogSharing = TxLogSharing.EXPLICIT;

	/** The insert update delete log level. */
	private StmtLogLevel iudLogLevel = StmtLogLevel.SQL;

	/** The find id log level. */
	private StmtLogLevel queryLogLevel = StmtLogLevel.SQL;

	/** The find many log level. */
	private StmtLogLevel sqlQueryLogLevel = StmtLogLevel.SQL;

	/** Used to unwrap PreparedStatements to perform JDBC Driver specific functions */
	private PstmtDelegate pstmtDelegate;
	
	/** The data source. */
	private DataSource dataSource;

	/** The data source config. */
	private DataSourceConfig dataSourceConfig = new DataSourceConfig();

	/** The data source jndi name. */
	private String dataSourceJndiName;

	/** The database boolean true. */
	private String databaseBooleanTrue;

	/** The database boolean false. */
	private String databaseBooleanFalse;

	/** The naming convention. */
	private NamingConvention namingConvention;

	/** The update changes only. */
	private boolean updateChangesOnly = true;
	
	private List<BeanPersistController> persistControllers = new ArrayList<BeanPersistController>();
	private List<BeanPersistListener<?>> persistListeners = new ArrayList<BeanPersistListener<?>>();
	
	/**
	 * Return the name of the EbeanServer.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the EbeanServer.
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Return true if this server should be registered with the
	 * Ebean singleton when it is created.
	 * <p>
	 * By default this is set to true.
	 * </p>
	 */
	public boolean isRegister() {
		return register;
	}

	/**
	 * Set to false if you do not want this server to be registered
	 * with the Ebean singleton when it is created.
	 * <p>
	 * By default this is set to true.
	 * </p>
	 */
	public void setRegister(boolean register) {
		this.register = register;
	}

	/**
	 * Return true if this server should be registered as the
	 * "default" server with the Ebean singleton.
	 * <p>
	 * This is only used when {@link #setRegister(boolean)} is also
	 * true.
	 * </p>
	 */
	public boolean isDefaultServer() {
		return defaultServer;
	}

	/**
	 * Set true if this EbeanServer should be registered as the
	 * "default" server with the Ebean singleton.
	 * <p>
	 * This is only used when {@link #setRegister(boolean)} is also
	 * true.
	 * </p>
	 */
	public void setDefaultServer(boolean defaultServer) {
		this.defaultServer = defaultServer;
	}

	
	/**
	 * Returns true if by default JDBC batching is used for persisting or deleting beans.
	 * <p>
	 * With this Ebean will batch up persist requests and use the JDBC batch api. This is
	 * a performance optimisation designed to reduce the network chatter. 
	 * </p>
	 */
	public boolean isUsePersistBatching() {
		return usePersistBatching;
	}

	/**
	 * Set to true if you what to use JDBC batching for persisting and deleting beans.
	 * <p>
	 * With this Ebean will batch up persist requests and use the JDBC batch api. This is
	 * a performance optimisation designed to reduce the network chatter. 
	 * </p>
	 */
	public void setUsePersistBatching(boolean usePersistBatching) {
		this.usePersistBatching = usePersistBatching;
	}

	/**
	 * Return the batch size used for JDBC batching.
	 * This defaults to 20.
	 */
	public int getPersistBatchSize() {
		return persistBatchSize;
	}

	/**
	 * Set the batch size used for JDBC batching. If unset this defaults to 20.
	 */
	public void setPersistBatchSize(int persistBatchSize) {
		this.persistBatchSize = persistBatchSize;
	}
	
	/**
	 * Return the default batch size for lazy loading of beans and collections.
	 */
	public int getLoadBatchSize() {
		return loadBatchSize;
	}

	/**
	 * Set the default batch size for lazy loading.
	 * <p>
	 * This is the number of beans or collections loaded when
	 * lazy loading is invoked by default.
	 * </p>
	 * <p>
	 * The default value is for this is 1 (load 1 bean or collection).
	 * </p>
	 * <p>
	 * You can explicitly control the lazy loading batch size for a 
	 * given join on a query using +lazy(batchSize).
	 * </p>
	 */
	public void setLoadBatchSize(int loadBatchSize) {
		this.loadBatchSize = loadBatchSize;
	}

	/**
	 * Set the number of sequences to fetch/preallocate when using DB sequences.
	 * <p>
	 * This is a performance optimisation to reduce the number times Ebean requests
	 * a sequence to be used as an Id for a bean (aka reduce network chatter).
	 * </p>
	 */
	public void setDatabaseSequenceBatchSize(int databaseSequenceBatchSize) {
		this.databaseSequenceBatchSize = databaseSequenceBatchSize;
	}

	/**
	 * Return the external transaction manager.
	 */
	public ExternalTransactionManager getExternalTransactionManager() {
		return externalTransactionManager;
	}

	/**
	 * Set the external transaction manager.
	 */
	public void setExternalTransactionManager(ExternalTransactionManager externalTransactionManager) {
		this.externalTransactionManager = externalTransactionManager;
	}

	/**
	 * Return true if a bean should be validated when it is saved.
	 */
	public boolean isValidateOnSave() {
		return validateOnSave;
	}

	/**
	 * Set whether validation should run when a bean is saved.
	 */
	public void setValidateOnSave(boolean validateOnSave) {
		this.validateOnSave = validateOnSave;
	}

	/**
	 * Return the log level used for "subclassing" enhancement.
	 */
	public int getEnhanceLogLevel() {
		return enhanceLogLevel;
	}

	/**
	 * Set the log level used for "subclassing" enhancement.
	 */
	public void setEnhanceLogLevel(int enhanceLogLevel) {
		this.enhanceLogLevel = enhanceLogLevel;
	}

	/**
	 * Return the NamingConvention.
	 * <p>
	 * If none has been set the default UnderscoreNamingConvention is used.
	 * </p>
	 */
	public NamingConvention getNamingConvention() {
		return namingConvention;
	}

	/**
	 * Set the NamingConvention.
	 * <p>
	 * If none is set the default UnderscoreNamingConvention is used.
	 * </p>
	 */
	public void setNamingConvention(NamingConvention namingConvention) {
		this.namingConvention = namingConvention;
	}

	/**
	 * Return the configuration for the Autofetch feature.
	 */
	public AutofetchConfig getAutofetchConfig() {
		return autofetchConfig;
	}

	/**
	 * Set the configuration for the Autofetch feature.
	 */
	public void setAutofetchConfig(AutofetchConfig autofetchConfig) {
		this.autofetchConfig = autofetchConfig;
	}

	
	/**
	 * Return the PreparedStatementDelegate.
	 */
	public PstmtDelegate getPstmtDelegate() {
		return pstmtDelegate;
	}

	/**
	 * Set the PstmtDelegate which can be used to support JDBC driver specific features.
	 * <p>
	 * Typically this means Oracle JDBC driver specific workarounds.
	 * </p>
	 */
	public void setPstmtDelegate(PstmtDelegate pstmtDelegate) {
		this.pstmtDelegate = pstmtDelegate;
	}

	/**
	 * Return the DataSource.
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Set a DataSource.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Return the configuration to build a DataSource using
	 * Ebean's own DataSource implementation.
	 */
	public DataSourceConfig getDataSourceConfig() {
		return dataSourceConfig;
	}

	/**
	 * Set the configuration required to build a DataSource using
	 * Ebean's own DataSource implementation.
	 */
	public void setDataSourceConfig(DataSourceConfig dataSourceConfig) {
		this.dataSourceConfig = dataSourceConfig;
	}

	/**
	 * Return the JNDI name of the DataSource to use.
	 */
	public String getDataSourceJndiName() {
		return dataSourceJndiName;
	}

	/**
	 * Set the JNDI name of the DataSource to use.
	 * <p>
	 * By default a prefix of "java:comp/env/jdbc/" is used to
	 * lookup the DataSource. This prefix is not used if
	 * dataSourceJndiName starts with "java:".
	 * </p>
	 */
	public void setDataSourceJndiName(String dataSourceJndiName) {
		this.dataSourceJndiName = dataSourceJndiName;
	}

	/**
	 * Return a value used to represent TRUE in the database.
	 * <p>
	 * This is used for databases that do not support boolean natively.
	 * </p>
	 * <p>
	 * The value returned is either a Integer or a String (e.g. "1", or "T").
	 * </p>
	 */
	public String getDatabaseBooleanTrue() {
		return databaseBooleanTrue;
	}

	/**
	 * Set the value to represent TRUE in the database.
	 * <p>
	 * This is used for databases that do not support boolean natively.
	 * </p>
	 * <p>
	 * The value set is either a Integer or a String (e.g. "1", or "T").
	 * </p>
	 */
	public void setDatabaseBooleanTrue(String databaseTrue) {
		this.databaseBooleanTrue = databaseTrue;
	}

	/**
	 * Return a value used to represent FALSE in the database.
	 * <p>
	 * This is used for databases that do not support boolean natively.
	 * </p>
	 * <p>
	 * The value returned is either a Integer or a String (e.g. "0", or "F").
	 * </p>
	 */
	public String getDatabaseBooleanFalse() {
		return databaseBooleanFalse;
	}

	/**
	 * Set the value to represent FALSE in the database.
	 * <p>
	 * This is used for databases that do not support boolean natively.
	 * </p>
	 * <p>
	 * The value set is either a Integer or a String (e.g. "0", or "F").
	 * </p>
	 */
	public void setDatabaseBooleanFalse(String databaseFalse) {
		this.databaseBooleanFalse = databaseFalse;
	}

	
	/**
	 * Return the number of DB sequence values that should be preallocated.
	 */
	public int getDatabaseSequenceBatchSize() {
		return databaseSequenceBatchSize;
	}

	/**
	 * Set the number of DB sequence values that should be preallocated
	 * and cached by Ebean.
	 * <p>
	 * This is only used for DB's that use sequences and is a performance
	 * optimisation. This reduces the number of times Ebean needs to get
	 * a sequence value from the Database reducing network chatter.
	 * </p>
	 * <p>
	 * By default this value is 10 so when we need another Id (and don't have
	 * one in our cache) Ebean will fetch 10 id's from the database. Note that
	 * when the cache drops to have full (which is 5 by default) Ebean will fetch 
	 * another batch of Id's in a background thread.
	 * </p>
	 */
	public void setDatabaseSequenceBatch(int databaseSequenceBatchSize) {
		this.databaseSequenceBatchSize = databaseSequenceBatchSize;
	}

	/**
	 * Return the database platform name (can be null).
	 * <p>
	 * If null then the platform is determined automatically
	 * via the JDBC driver information.
	 * </p>
	 */
	public String getDatabasePlatformName() {
		return databasePlatformName;
	}

	/**
	 * Explicitly set the database platform name
	 * <p>
	 * If none is set then the platform is determined automatically
	 * via the JDBC driver information.
	 * </p>
	 */
	public void setDatabasePlatformName(String databasePlatformName) {
		this.databasePlatformName = databasePlatformName;
	}

	/**
	 * Return the database platform to use for this server.
	 */
	public DatabasePlatform getDatabasePlatform() {
		return databasePlatform;
	}

	/**
	 * Explicitly set the database platform to use.
	 * <p>
	 * If none is set then the platform is determined via the
	 * databasePlatformName or automatically via the JDBC driver
	 * information.
	 * </p>
	 */
	public void setDatabasePlatform(DatabasePlatform databasePlatform) {
		this.databasePlatform = databasePlatform;
	}

	/**
	 * Return the amount of transaction logging.
	 */
	public TxLogLevel getTransactionLogging() {
		return transactionLogging;
	}

	/**
	 * Set the amount (None, Explict, All) of transaction logging.
	 */
	public void setTransactionLogging(TxLogLevel logging) {
		this.transactionLogging = logging;
	}

	/**
	 * Return how transactions should share log files.
	 */
	public TxLogSharing getTransactionLogSharing() {
		if (externalTransactionManager != null){
			// with external transaction managers we need to share a
			// single transaction log file as we don't get notified
			// of commit/rollback events
			return TxLogSharing.ALL;
		}
		return transactionLogSharing;
	}

	/**
	 * Set how the transaction should share log files.
	 */
	public void setTransactionLogSharing(TxLogSharing logSharing) {
		this.transactionLogSharing = logSharing;
	}

	/**
	 * Return true to get the generated SQL queries output to the console.
	 * <p>
	 * To get the SQL and bind variables for insert update delete statements
	 * you should use transaction logging.
	 * </p>
	 */
	public boolean isDebugSql() {
		return debugSql;
	}

	/**
	 * Set to true to get the generated SQL queries output to the console.
	 * <p>
	 * To get the SQL and bind variables for insert update delete statements
	 * you should use transaction logging.
	 * </p>
	 */
	public void setDebugSql(boolean debugSql) {
		this.debugSql = debugSql;
	}

	/**
	 * Return true if there is debug logging on lazy loading events.
	 */
	public boolean isDebugLazyLoad() {
		return debugLazyLoad;
	}

	/**
	 * Set to true to get debug logging on lazy loading events.
	 */
	public void setDebugLazyLoad(boolean debugLazyLoad) {
		this.debugLazyLoad = debugLazyLoad;
	}

	/**
	 * Return the debug level for transaction begin, commit and rollback events.
	 * <p>
	 * <ul>
	 * <li>0 = No Logging</li>
	 * <li>1 = Log Rollbacks</li>
	 * <li>2 = Log all begin, commit and rollback events</li>
	 * </p>
	 *
	 * @return the transaction debug level
	 */
	public int getTransactionDebugLevel() {
		return transactionDebugLevel;
	}

	/**
	 * Set a debug level for transaction begin, commit and rollback events.
	 * <p>
	 * <ul>
	 * <li>0 = No Logging</li>
	 * <li>1 = Log Rollbacks</li>
	 * <li>2 = Log all begin, commit and rollback events</li>
	 * </p>
	 *
	 * @param transactionDebugLevel the transaction debug level
	 */
	public void setTransactionDebugLevel(int transactionDebugLevel) {
		this.transactionDebugLevel = transactionDebugLevel;
	}

	/**
	 * Return the directory where transaction logs go.
	 */
	public String getTransactionLogDirectory() {
		return transactionLogDirectory;
	}

	/**
	 * Return the transaction log directory substituting any expressions
	 * such as ${catalina.base} etc.
	 */
	public String getTransactionLogDirectoryWithEval() {
		return PropertyExpression.eval(transactionLogDirectory);
	}

	/**
	 * Set the directory that the transaction logs go in.
	 * <p>
	 * This can contain expressions like ${catalina.base} with
	 * environment variables, java system properties and entries
	 * in ebean.properties.
	 * </p>
	 * <p>
	 * e.g. ${catalina.base}/logs/trans
	 * </p>
	 *
	 * @param transactionLogDirectory the transaction log directory
	 */
	public void setTransactionLogDirectory(String transactionLogDirectory) {
		this.transactionLogDirectory = transactionLogDirectory;
	}

	
	/**
	 * Return true if you want to use a java.util.logging.Logger to 
	 * log transaction statements, bind values etc. 
	 * <p>
	 * If this is false then the default transaction logger is used
	 * which logs the transaction details to separate transaction log
	 * files.
	 * </p>
	 */
	public boolean isUseJuliTransactionLogger() {
		return useJuliTransactionLogger;
	}

	/**
	 * Set this to true if you want transaction logging to use a
	 * java.util.logging.Logger to log the statements and bind variables
	 * etc rather than the default one which creates separate transaction
	 * log files.
	 */
	public void setUseJuliTransactionLogger(boolean useJuliTransactionLogger) {
		this.useJuliTransactionLogger = useJuliTransactionLogger;
	}

	/**
	 * Return the logging level on Insert Update and Delete statements.
	 */
	public StmtLogLevel getIudLogLevel() {
		return iudLogLevel;
	}

	/**
	 * Set the logging level on Insert Update and Delete statements.
	 */
	public void setIudLogLevel(StmtLogLevel iudLoglevel) {
		this.iudLogLevel = iudLoglevel;
	}

	/**
	 * Return the logging level on Find by Id (or find unique) statements.
	 */
	public StmtLogLevel getQueryLogLevel() {
		return queryLogLevel;
	}

	/**
	 * set the logging level on Find by Id (or find unique) statements.
	 */
	public void setQueryLogLevel(StmtLogLevel queryLogLevel) {
		this.queryLogLevel = queryLogLevel;
	}

	/**
	 * Return the logging level on FindMany statements.
	 */
	public StmtLogLevel getSqlQueryLogLevel() {
		return sqlQueryLogLevel;
	}

	/**
	 * Set the logging level on FindMany statements.
	 */
	public void setSqlQueryLogLevel(StmtLogLevel sqlQueryLogLevel) {
		this.sqlQueryLogLevel = sqlQueryLogLevel;
	}

	/**
	 * Set to true to run the DDL generation on startup.
	 */
	public void setDdlGenerate(boolean ddlGenerate) {
		this.ddlGenerate = ddlGenerate;
	}

	/**
	 * Set to true to run the generated DDL on startup.
	 */
	public void setDdlRun(boolean ddlRun) {
		this.ddlRun = ddlRun;
	}

	/**
	 * Return true if the DDL should be generated.
	 */
	public boolean isDdlGenerate() {
		return ddlGenerate;
	}

	/**
	 * Return true if the DDL should be run.
	 */
	public boolean isDdlRun() {
		return ddlRun;
	}

	/**
	 * Programmatically add classes (typically entities) that this server should use.
	 * <p>
	 * The class can be an Entity, Embedded type, ScalarType, BeanPersistListener,
	 * BeanFinder or BeanPersistController.
	 * </p>
	 * <p>
	 * If no classes are specified then the classes are found
	 * automatically via searching the class path.
	 * </p>
	 * <p>
	 * Alternatively the classes can be added via {@link #setClasses(List)}.
	 * </p>
	 *
	 * @param cls the entity type (or other type) that should be registered by
	 * this server.
	 */
	public void addClass(Class<?> cls) {
		if (classes == null){
			classes = new ArrayList<Class<?>>();
		}
		classes.add(cls);
	}

	/**
	 * Add a package to search for entities via class path search.
	 * <p>
	 * This is only used if classes have not been explicitly specified.
	 * </p>
	 */
	public void addPackage(String packageName){
		if (packages == null){
			packages = new ArrayList<String>();
		}
		packages.add(packageName);
	}

	/**
	 * Return packages to search for entities via class path search.
	 * <p>
	 * This is only used if classes have not been explicitly specified.
	 * </p>
	 */
	public List<String> getPackages() {
		return packages;
	}

	/**
	 * Set packages to search for entities via class path search.
	 * <p>
	 * This is only used if classes have not been explicitly specified.
	 * </p>
	 */
	public void setPackages(List<String> packages) {
		this.packages = packages;
	}

	/**
	 * Set the list of classes (entities, listeners, scalarTypes etc)
	 * that should be used for this server.
	 * <p>
	 * If no classes are specified then the classes are found
	 * automatically via searching the class path.
	 * </p>
	 * <p>
	 * Alternatively the classes can contain added via {@link #addClass(Class)}.
	 * </p>
	 */
	public void setClasses(List<Class<?>> classes) {
		this.classes = classes;
	}

	/**
	 * Return the classes registered for this server.
	 * Typically this includes entities and perhaps listeners.
	 */
	public List<Class<?>> getClasses() {
		return classes;
	}

	/**
	 * Return true to only update changed properties.
	 */
	public boolean isUpdateChangesOnly() {
		return updateChangesOnly;
	}

	/**
	 * Set to true to only update changed properties.
	 */
	public void setUpdateChangesOnly(boolean updateChangesOnly) {
		this.updateChangesOnly = updateChangesOnly;
	}

	/**
	 * Returns the resource directory.
	 */
	public String getResourceDirectory() {
		return resourceDirectory;
	}

	/**
	 * Sets the resource directory.
	 */
	public void setResourceDirectory(String resourceDirectory) {
		this.resourceDirectory = resourceDirectory;
	}

	/**
	 * Register a BeanPersistController instance.
	 * <p>
	 * Note alternatively you can use {@link #setPersistControllers(List)} to 
	 * set all the BeanPersistController instances.
	 * </p>
	 */
	public void add(BeanPersistController beanPersistController){
		persistControllers.add(beanPersistController);
	}
	
	/**
	 * Return the BeanPersistController instances.
	 */
	public List<BeanPersistController> getPersistControllers() {
		return persistControllers;
	}

	/**
	 * Register all the BeanPersistController instances.
	 * <p>
	 * Note alternatively you can use {@link #add(BeanPersistController)} to 
	 * add BeanPersistController instances one at a time.
	 * </p>
	 */
	public void setPersistControllers(List<BeanPersistController> persistControllers) {
		this.persistControllers = persistControllers;
	}

	/**
	 * Register a BeanPersistListener instance.
	 * <p>
	 * Note alternatively you can use {@link #setPersistListeners(List)} to 
	 * set all the BeanPersistListener instances.
	 * </p>
	 */
	public void add(BeanPersistListener<?> beanPersistListener){
		persistListeners.add(beanPersistListener);
	}
	
	/**
	 * Return the BeanPersistListener instances.
	 */
	public List<BeanPersistListener<?>> getPersistListeners() {
		return persistListeners;
	}

	/**
	 * Register all the BeanPersistListener instances.
	 * <p>
	 * Note alternatively you can use {@link #add(BeanPersistListener)} to 
	 * add BeanPersistListener instances one at a time.
	 * </p>
	 */
	public void setPersistListeners(List<BeanPersistListener<?>> persistListeners) {
		this.persistListeners = persistListeners;
	}

	/**
	 * Load the settings from the ebean.properties file.
	 */
	public void loadFromProperties() {
		ConfigPropertyMap p = new ConfigPropertyMap(name);
		loadSettings(p);
	}

	/**
	 * Load the configuration settings from the properties file.
	 */
	private void loadSettings(ConfigPropertyMap p){

		if (autofetchConfig == null){
			autofetchConfig = new AutofetchConfig();
		}
		autofetchConfig.loadSettings(p);
		if (dataSourceConfig == null){
			dataSourceConfig = new DataSourceConfig();
		}
		dataSourceConfig.loadSettings(p.getServerName());

		String nc = p.get("namingconvention", null);
		if (nc != null){
			try {
				Class<?> cls = Class.forName(nc);
				namingConvention = (NamingConvention)cls.newInstance();
			} catch (Exception e){
				throw new RuntimeException(e);
			}
		}
		String dbp = p.get("databasePlatform", null);
		if (dbp != null){
			try {
				Class<?> cls = Class.forName(dbp);
				databasePlatform = (DatabasePlatform)cls.newInstance();
			} catch (Exception e){
				throw new RuntimeException(e);
			}
		}
		
		
		usePersistBatching = p.getBoolean("batch.mode", false);
		persistBatchSize = p.getInt("batch.size", 20);
		
		databaseSequenceBatchSize = p.getInt("databaseSequenceBatchSize", 20);
			
		databaseBooleanTrue = p.get("databaseBooleanTrue", null);
		databaseBooleanFalse = p.get("databaseBooleanFalse", null);
		
		ddlGenerate = p.getBoolean("ddl.generate", false);
		ddlRun = p.getBoolean("ddl.run", false);

		transactionLogging = p.getEnum(TxLogLevel.class, "logging", TxLogLevel.ALL);//"log.level"
		transactionLogSharing = p.getEnum(TxLogSharing.class, "logsharing", TxLogSharing.EXPLICIT);//"log.sharing"

		useJuliTransactionLogger = p.getBoolean("useJuliTransactionLogger", false);
		
		debugSql = p.getBoolean("debug.sql", false);
		debugLazyLoad = p.getBoolean("debug.lazyload", false);

		transactionDebugLevel= p.getInt("debug.transaction", 0);
		transactionLogDirectory = p.get("log.directory", "logs");


		iudLogLevel = p.getEnum(StmtLogLevel.class, "logging.iud", StmtLogLevel.SQL);
		sqlQueryLogLevel = p.getEnum(StmtLogLevel.class, "logging.sqlquery", StmtLogLevel.SQL);
		queryLogLevel = p.getEnum(StmtLogLevel.class, "logging.query", StmtLogLevel.SQL);

		classes = getClasses(p);
	}

	/**
	 * Build the list of classes from the comma delimited string.
	 *
	 * @param p the p
	 *
	 * @return the classes
	 */
	private ArrayList<Class<?>> getClasses(ConfigPropertyMap p) {


		String classNames = p.get("classes", null);
		if (classNames == null){

			return null;
		}

		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();

		String[] split = classNames.split("[,;]");
		for (int i = 0; i < split.length; i++) {
			String cn = split[i].trim();
			if (cn.length() > 0){
				try {
					classes.add(Class.forName(cn));
				} catch (ClassNotFoundException e) {
					String msg = "Error registering class ["+cn+"] from ["+classNames+"]";
					throw new RuntimeException(msg, e);
				}
			}
		}
		return classes;
	}
}
