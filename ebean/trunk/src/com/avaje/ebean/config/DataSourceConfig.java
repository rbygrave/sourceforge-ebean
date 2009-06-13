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



	public void loadSettings(ConfigPropertyMap p){
		
		String prefix = "datasource."+p.getServerName()+".";
		
		//autocommit false;
		// isolation level
		
		username = p.getRaw(prefix+"username", null);
		password = p.getRaw(prefix+"password", null);
		
		String v;
		
		v = p.getRaw(prefix+"databaseDriver", null);
		driver = p.getRaw(prefix+"driver", v);
		
		
		v = p.getRaw(prefix+"databaseUrl", null);
		url = p.getRaw(prefix+"url", v);
		
		captureStackTrace = p.getRawBoolean(prefix+"captureStackTrace", false);
		leakTimeMinutes = p.getRawInt(prefix+"leakTimeMinutes", 30);
		maxInactiveTimeSecs = p.getRawInt(prefix+"maxInactiveTimeSecs", 900);

		minConnections = p.getRawInt(prefix+"minConnections", 0);
		maxConnections = p.getRawInt(prefix+"maxConnections", 20);
		pstmtCacheSize = p.getRawInt(prefix+"pstmtCacheSize", 20);
		cstmtCacheSize = p.getRawInt(prefix+"cstmtCacheSize", 20);
		
		waitTimeout = p.getRawInt(prefix+"waitTimeout", 1);
		
		heartbeatSql = p.getRaw(prefix+"heartbeatSql", null);
		poolListener = p.getRaw(prefix+"poolListener", null);

	}
}
