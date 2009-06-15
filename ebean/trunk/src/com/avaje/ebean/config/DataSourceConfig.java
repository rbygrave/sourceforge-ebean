package com.avaje.ebean.config;


public class DataSourceConfig {
	
	String url;
	
	String username;
	
	String password;
	
	String driver;
	
	int minConnections = 2;
	
	int maxConnections = 20;
	
	String heartbeatSql;
	
	boolean captureStackTrace;

	int leakTimeMinutes = 30;
	
	int maxInactiveTimeSecs = 900;

	int pstmtCacheSize = 20;
	int cstmtCacheSize = 20;
	
	int waitTimeout = 1;
	
	String poolListener;
	


	public String getUrl() {
		return url;
	}



	public void setUrl(String url) {
		this.url = url;
	}



	public String getUsername() {
		return username;
	}



	public void setUsername(String username) {
		this.username = username;
	}



	public String getPassword() {
		return password;
	}



	public void setPassword(String password) {
		this.password = password;
	}



	public String getDriver() {
		return driver;
	}



	public void setDriver(String driver) {
		this.driver = driver;
	}



	public int getMinConnections() {
		return minConnections;
	}



	public void setMinConnections(int minConnections) {
		this.minConnections = minConnections;
	}



	public int getMaxConnections() {
		return maxConnections;
	}



	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}



	public String getHeartbeatSql() {
		return heartbeatSql;
	}



	public void setHeartbeatSql(String heartbeatSql) {
		this.heartbeatSql = heartbeatSql;
	}



	public boolean isCaptureStackTrace() {
		return captureStackTrace;
	}



	public void setCaptureStackTrace(boolean captureStackTrace) {
		this.captureStackTrace = captureStackTrace;
	}



	public int getLeakTimeMinutes() {
		return leakTimeMinutes;
	}



	public void setLeakTimeMinutes(int leakTimeMinutes) {
		this.leakTimeMinutes = leakTimeMinutes;
	}



	public int getPstmtCacheSize() {
		return pstmtCacheSize;
	}



	public void setPstmtCacheSize(int pstmtCacheSize) {
		this.pstmtCacheSize = pstmtCacheSize;
	}



	public int getCstmtCacheSize() {
		return cstmtCacheSize;
	}



	public void setCstmtCacheSize(int cstmtCacheSize) {
		this.cstmtCacheSize = cstmtCacheSize;
	}



	public int getWaitTimeout() {
		return waitTimeout;
	}



	public void setWaitTimeout(int waitTimeout) {
		this.waitTimeout = waitTimeout;
	}



	public int getMaxInactiveTimeSecs() {
		return maxInactiveTimeSecs;
	}



	public void setMaxInactiveTimeSecs(int maxInactiveTimeSecs) {
		this.maxInactiveTimeSecs = maxInactiveTimeSecs;
	}



	public String getPoolListener() {
		return poolListener;
	}



	public void setPoolListener(String poolListener) {
		this.poolListener = poolListener;
	}



	public void loadSettings(String serverName){
		
		String prefix = "datasource."+serverName+".";
		
		//autocommit false;
		// isolation level
		
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
		
		waitTimeout = GlobalProperties.getInt(prefix+"waitTimeout", 1);
		
		heartbeatSql = GlobalProperties.get(prefix+"heartbeatSql", null);
		poolListener = GlobalProperties.get(prefix+"poolListener", null);

	}
}
