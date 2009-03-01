/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package com.avaje.ebean.server.lib.sql;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * Deployment parameters for a DataSourcePool.
 */
public class DataSourceParams {

    Map<String,String> params;

    String prefix;

    String name;

    Properties connectionProps;

    /**
     * Create the parameters based on a Map.
     * 
     * @param name the name of the dataSource
     * @param prefix the prefix used to determine the full parameter key
     * @param params the Map that contains the key value pairs
     */
    public DataSourceParams(Map<String,String> params, String prefix, String name) {
        this.params = params;
        this.prefix = prefix;
        this.name = name;

        parse();
    }

    /**
     * Return the datasource name.
     */
    public String getName() {
        return name;
    }

    /**
     * Return the Properties used to create new Connections.
     */
    public Properties getConnectionProperties() {
        return connectionProps;
    }

    /**
     * Return the driver class name.
     */
    public boolean isAutoCommit() {
        String s = getParam("autocommit", false);
        return "true".equalsIgnoreCase(s);
    }
    

    /**
     * Return the driver class name.
     */
    public boolean isCaptureStackTrace() {
        String s = getParam("capturestacktrace", false);
        return "true".equalsIgnoreCase(s);
    }
    
    /**
     * Return the driver class name.
     */
    public String getDriver() {
        return getParam("databasedriver", true);
    }

    /**
     * Return the connection url.
     */
    public String getUrl() {
        return getParam("databaseurl", true);
    }

    /**
     * Return the time after which a connection is considered 'leaked' from the pool in minutes.
     */
    public int getLeakTimeMinutes() {
        return getIntParam("leaktimeminutes", 30);
    }
    
    /**
     * Max time a connection is allowed to be inactive.
     */
    public int getMaxInactiveTimeSecs() {
        return getIntParam("maxinactivetimesecs", 900);
    }
    
    /**
     * Return the minimum number of connections the pool should maintain.
     */
    public int getMinConnections() {
        return getIntParam("minconnections", 0);
    }

    /**
     * Return the maximum size the pool can grow to.
     */
    public int getMaxConnections() {
        return getIntParam("maxconnections", 20);
    }

    /**
     * Return the maximum size of the prepared statement cache.
     */
    public int getPstmtCacheSize() {
        return getIntParam("pstmtcachesize", 20);
    }

    /**
     * Return the maximum size of the callable statement cache.
     */
    public int getCstmtCacheSize() {
        return getIntParam("cstmtcachesize", 20);
    }

    /**
     * Return the maximum time a thread will wait when the pool can't grow.
     */
    public int getWaitTimeout() {
        return 1000 * getIntParam("waittimeout", 1);
    }

    /**
     * Return a simple query used to test connections when they error. For
     * example "select * from dual".
     */
    public String getHeartBeatSql() {
        return getParam("heartbeatsql", false);
    }

    /**
     * Return the transaction isolation level set on the connections.
     * <p>
     * This defaults to TRANSACTION_READ_COMMITTED if a "isolationlevel"
     * parameter is not explicitly set.
     * </p>
     */
    public int getIsolationLevel() {
        String isoLevel = getParam("isolationlevel", false);
        if (isoLevel == null) {
            isoLevel = TransactionIsolation.getLevelDescription(Connection.TRANSACTION_READ_COMMITTED);
        }
        return TransactionIsolation.getLevel(isoLevel);
    }

	/**
	 * Return the class name of a DataSourcePoolListener or null.
	 */
	public String getPoolListener() {
		return getParam("poolListener", false);
	}
    
    /**
	 * Return a parameter value.
	 */
    public String getParam(String key) {
        return getParam(key, false);
    }

    private void parse() {
        String un = getParam("username", false);
        if (un == null) {
            un = getParam("eusername", false);
            if (un != null) {
                un = Prefix.getProp(un);
                //parseThrowError("username", null);
            }
        }
        String pw = getParam("password", false);
        if (pw == null) {
            pw = getParam("epassword", false);
            if (pw != null) {
                pw = Prefix.getProp(pw);
                //parseThrowError("password", null);
            }
        }

        connectionProps = new Properties();
        if (un != null){
        	connectionProps.setProperty("user", un);
        }
        if (pw != null){
        	connectionProps.setProperty("password", pw);
        }
    }

    private int getIntParam(String key, int defaultVal) {
        String val = getParam(key, false);
        if (val == null) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception ex) {
            String msg = "[" + val + "] is not an Integer";
            parseThrowError(key, msg);
            return 0;
        }
    }

    private String getParam(String key, boolean throwError) {
        String val = (String) params.get(getParameterKey(key));
        if (val == null && throwError) {
            parseThrowError(key, null);
        }
        return val;
    }

    private String getParameterKey(String key) {
        return (prefix + "." + name + "." + key).toLowerCase();
    }

    private void parseThrowError(String key, String msg) {
        String fullName = getParameterKey(key);
        if (msg == null) {
            throw new DataSourceException("DataSource parameter [" + fullName + "] is not defined.");
        }
        throw new DataSourceException("DataSource parameter [" + fullName + "]" + msg);
    }
}
