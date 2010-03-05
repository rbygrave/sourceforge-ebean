package com.avaje.ebean.config;

/**
 * Used to config a DataSource when using the internal Ebean
 * DataSource implementation.
 * <p>
 * If a DataSource instance is already defined via {@link ServerConfig#setDataSource(javax.sql.DataSource)}
 * or defined as JNDI dataSource via {@link ServerConfig#setDataSourceJndiName(String)}
 * then those will used and not this DataSourceConfig.
 * </p>
 */
public class DataSourceConfig {
	
	private String url;
	
	private String username;
	
	private String password;
	
	private String driver;
	
	private int minConnections = 2;
	
	private int maxConnections = 20;
	
	private String heartbeatSql;
	
	private boolean captureStackTrace;

	private int leakTimeMinutes = 30;
	
	private int maxInactiveTimeSecs = 900;

	private int pstmtCacheSize = 20;
	private int cstmtCacheSize = 20;
	
	private int waitTimeoutMillis = 1000;
	
	private String poolListener;

	private boolean offline;

	/**
	 * Return the connection URL.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the connection URL.
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Return the database username.
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the database username.
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the database password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Set the database password.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the database driver.
	 */
	public String getDriver() {
		return driver;
	}

	/**
	 * Set the database driver.
	 */
	public void setDriver(String driver) {
		this.driver = driver;
	}

	/**
	 * Return the minimum number of connections the pool should maintain.
	 */
	public int getMinConnections() {
		return minConnections;
	}

	/**
	 * Set the minimum number of connections the pool should maintain.
	 */
	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}

	/**
	 * Return the maximum number of connections the pool can reach.
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Set the maximum number of connections the pool can reach.
	 */
	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * Return a SQL statement used to test the database is accessible.
	 * <p>
	 * Note that if this is not set then it can get defaulted from the DatabasePlatform.
	 * </p>
	 */
	public String getHeartbeatSql() {
		return heartbeatSql;
	}

	/**
	 * Set a SQL statement used to test the database is accessible.
	 * <p>
	 * Note that if this is not set then it can get defaulted from the DatabasePlatform.
	 * </p>
	 */
	public void setHeartbeatSql(String heartbeatSql) {
		this.heartbeatSql = heartbeatSql;
	}


	/**
	 * Return true if a stack trace should be captured when obtaining
	 * a connection from the pool.
	 * <p>
	 * This can be used to diagnose a suspected connection pool leak.
	 * </p>
	 * <p>
	 * Obviously this has a performance overhead.
	 * </p>
	 */
	public boolean isCaptureStackTrace() {
		return captureStackTrace;
	}

	/**
	 * Set to true if a stack trace should be captured when obtaining
	 * a connection from the pool.
	 * <p>
	 * This can be used to diagnose a suspected connection pool leak.
	 * </p>
	 * <p>
	 * Obviously this has a performance overhead.
	 * </p>
	 */
	public void setCaptureStackTrace(boolean captureStackTrace) {
		this.captureStackTrace = captureStackTrace;
	}

	/**
	 * Return the time in minutes after which a connection could
	 * be considered to have leaked.
	 */
	public int getLeakTimeMinutes() {
		return leakTimeMinutes;
	}

	/**
	 * Set the time in minutes after which a connection could
	 * be considered to have leaked.
	 */
	public void setLeakTimeMinutes(int leakTimeMinutes) {
		this.leakTimeMinutes = leakTimeMinutes;
	}

	/**
	 * Return the size of the PreparedStatement cache (per connection).
	 */
	public int getPstmtCacheSize() {
		return pstmtCacheSize;
	}

	/**
	 * Set the size of the PreparedStatement cache (per connection).
	 */
	public void setPstmtCacheSize(int pstmtCacheSize) {
		this.pstmtCacheSize = pstmtCacheSize;
	}

	/**
	 * Return the size of the CallableStatement cache (per connection).
	 */
	public int getCstmtCacheSize() {
		return cstmtCacheSize;
	}

	/**
	 * Set the size of the CallableStatement cache (per connection).
	 */
	public void setCstmtCacheSize(int cstmtCacheSize) {
		this.cstmtCacheSize = cstmtCacheSize;
	}

	/**
	 * Return the time in millis to wait for a connection 
	 * before timing out once the pool has reached its maximum size.
	 */
	public int getWaitTimeoutMillis() {
		return waitTimeoutMillis;
	}

	/**
	 * Set the time in millis to wait for a connection 
	 * before timing out once the pool has reached its maximum size.
	 */
	public void setWaitTimeoutMillis(int waitTimeoutMillis) {
		this.waitTimeoutMillis = waitTimeoutMillis;
	}

	/**
	 * Return the time in seconds a connection can be idle after
	 * which it can be trimmed from the pool.
	 * <p>
	 * This is so that the pool after a busy period can trend over time 
	 * back towards the minimum connections.
	 * </p>
	 */
	public int getMaxInactiveTimeSecs() {
		return maxInactiveTimeSecs;
	}

	/**
	 * Set the time in seconds a connection can be idle after
	 * which it can be trimmed from the pool.
	 * <p>
	 * This is so that the pool after a busy period can trend over time 
	 * back towards the minimum connections.
	 * </p>
	 */
	public void setMaxInactiveTimeSecs(int maxInactiveTimeSecs) {
		this.maxInactiveTimeSecs = maxInactiveTimeSecs;
	}

	/**
	 * Return the pool listener.
	 */
	public String getPoolListener() {
		return poolListener;
	}


	/**
	 * Set a pool listener.
	 */
	public void setPoolListener(String poolListener) {
		this.poolListener = poolListener;
	}

	
	/**
	 * Return true if the DataSource should be left offline.
	 * <p>
	 * This is to support DDL generation etc without having a real database.
	 * </p>
	 */
	public boolean isOffline() {
        return offline;
    }

    /**
     * Set to true if the DataSource should be left offline.
     * <p>
     * This is to support DDL generation etc without having a real database.
     * </p>
     * <p>
     * Note that you MUST specify the database platform name (oracle, postgres,
     * h2, mysql etc) using {@link ServerConfig#setDatabasePlatformName(String)}
     * when you do this.
     * </p>
     */
    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    /**
	 * Load the settings from ebean.properties.
	 */
	public void loadSettings(String serverName){
		
		String prefix = "datasource."+serverName+".";
		
		username = GlobalProperties.get(prefix+"username", null);
		password = GlobalProperties.get(prefix+"password", null);
		
		String v;
		
		v = GlobalProperties.get(prefix+"databaseDriver", null);
		driver = GlobalProperties.get(prefix+"driver", v);
		
		
		v = GlobalProperties.get(prefix+"databaseUrl", null);
		url = GlobalProperties.get(prefix+"url", v);
		
		captureStackTrace = GlobalProperties.getBoolean(prefix+"captureStackTrace", false);
		leakTimeMinutes = GlobalProperties.getInt(prefix+"leakTimeMinutes", 30);
		maxInactiveTimeSecs = GlobalProperties.getInt(prefix+"maxInactiveTimeSecs", 900);

		minConnections = GlobalProperties.getInt(prefix+"minConnections", 0);
		maxConnections = GlobalProperties.getInt(prefix+"maxConnections", 20);
		pstmtCacheSize = GlobalProperties.getInt(prefix+"pstmtCacheSize", 20);
		cstmtCacheSize = GlobalProperties.getInt(prefix+"cstmtCacheSize", 20);
		
		waitTimeoutMillis = GlobalProperties.getInt(prefix+"waitTimeout", 1);
		
		heartbeatSql = GlobalProperties.get(prefix+"heartbeatSql", null);
		poolListener = GlobalProperties.get(prefix+"poolListener", null);
        offline = GlobalProperties.getBoolean(prefix+"offline", false);

	}
}
