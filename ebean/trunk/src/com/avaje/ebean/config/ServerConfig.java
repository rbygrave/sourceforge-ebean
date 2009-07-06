/**
 * Imilia Interactive Mobile Applications GmbH
 * Copyright (c) 2009 - all rights reserved
 *
 * Created on: Jul 2, 2009
 * Created by: emcgreal
 */
package com.avaje.ebean.config;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.config.naming.NamingConvention;

/**
 * The configuration used for creating a EbeanServer.
 */
public class ServerConfig {

	/** The name. */
	private String name;

	/** The resource directory. */
	private String resourceDirectory;

	/** The enhance log level. */
	private int enhanceLogLevel;

	/** The register. */
	boolean register = true;

	/** The default server. */
	boolean defaultServer;

	/** The validate on save. */
	boolean validateOnSave = true;

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
	private String transactionLogDirectory;

	/** The transaction logging. */
	private TransactionLogging transactionLogging = TransactionLogging.ALL;

	/** The transaction log sharing. */
	private TransactionLogSharing transactionLogSharing = TransactionLogSharing.EXPLICIT;

	/** The insert update delete log level. */
	private StatementLogLevel insertUpdateDeleteLogLevel = StatementLogLevel.SQL;

	/** The find id log level. */
	private StatementLogLevel findIdLogLevel = StatementLogLevel.SQL;

	/** The find many log level. */
	private StatementLogLevel findManyLogLevel = StatementLogLevel.SQL;


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
	public TransactionLogging getTransactionLogging() {
		return transactionLogging;
	}

	/**
	 * Set the amount (None, Explict, All) of transaction logging.
	 */
	public void setTransactionLogging(TransactionLogging logging) {
		this.transactionLogging = logging;
	}

	/**
	 * Return how transactions should share log files.
	 */
	public TransactionLogSharing getTransactionLogSharing() {
		if (externalTransactionManager != null){
			// with external transaction managers we need to share a
			// single transaction log file as we don't get notified
			// of commit/rollback events
			return TransactionLogSharing.ALL;
		}
		return transactionLogSharing;
	}

	/**
	 * Set how the transaction should share log files.
	 */
	public void setTransactionLogSharing(TransactionLogSharing logSharing) {
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
	 * Return the logging level on Insert Update and Delete statements.
	 */
	public StatementLogLevel getInsertUpdateDeleteLogLevel() {
		return insertUpdateDeleteLogLevel;
	}

	/**
	 * Set the logging level on Insert Update and Delete statements.
	 */
	public void setInsertUpdateDeleteLogLevel(StatementLogLevel iudLoglevel) {
		this.insertUpdateDeleteLogLevel = iudLoglevel;
	}

	/**
	 * Return the logging level on Find by Id (or find unique) statements.
	 */
	public StatementLogLevel getFindIdLogLevel() {
		return findIdLogLevel;
	}

	/**
	 * set the logging level on Find by Id (or find unique) statements.
	 */
	public void setFindIdLogLevel(StatementLogLevel findIdLogLevel) {
		this.findIdLogLevel = findIdLogLevel;
	}

	/**
	 * Return the logging level on FindMany statements.
	 */
	public StatementLogLevel getFindManyLogLevel() {
		return findManyLogLevel;
	}

	/**
	 * Set the logging level on FindMany statements.
	 */
	public void setFindManyLogLevel(StatementLogLevel findManyLogLevel) {
		this.findManyLogLevel = findManyLogLevel;
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
	 * Alternatively the classes can contain added via {@link #addClass(Class)}.
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

		ddlGenerate = p.getBoolean("ddl.generate", false);
		ddlRun = p.getBoolean("ddl.run", false);

		transactionLogging = p.getEnum(TransactionLogging.class, "logging", TransactionLogging.ALL);//"log.level"
		transactionLogSharing = p.getEnum(TransactionLogSharing.class, "logsharing", TransactionLogSharing.EXPLICIT);//"log.sharing"

		debugSql = p.getBoolean("debug.sql", false);
		debugLazyLoad = p.getBoolean("debug.lazyload", false);

		transactionDebugLevel= p.getInt("debug.transaction", 0);
		transactionLogDirectory = p.get("log.directory", "logs");


		insertUpdateDeleteLogLevel = p.getEnum(StatementLogLevel.class, "logging.iud", StatementLogLevel.SQL);
		findIdLogLevel = p.getEnum(StatementLogLevel.class, "logging.findId", StatementLogLevel.SQL);
		findManyLogLevel = p.getEnum(StatementLogLevel.class, "logging.findMany", StatementLogLevel.SQL);

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
