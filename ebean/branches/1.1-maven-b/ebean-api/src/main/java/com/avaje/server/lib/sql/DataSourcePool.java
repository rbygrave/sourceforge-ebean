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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.avaje.ebean.server.lib.cron.CronManager;
import com.avaje.lib.log.LogFactory;

/**
 * A robust DataSource.
 * <p>
 * <ul>
 * <li>Manages the number of connections closing connections that have been
 * idle for some time.
 * <li>Notifies when the datasource goes down and comes back up.
 * <li>Checks for expected downtime which is useful for schedule db backups.
 * <li>Provides PreparedStatement caching
 * <li>Knows the busy connections
 * <li>Traces connections that have been leaked
 * </ul>
 * </p>
 */
public class DataSourcePool implements DataSource {

	private static final Logger logger = LogFactory.get(DataSourcePool.class);

	/**
	 * The deployment parameters.
	 */
	DataSourceParams params;

	/**
	 * The name given to this datasource.
	 */
	String name;

	/**
	 * The manager.
	 */
	DataSourceManager manager;

	/**
	 * flag to indicate we have sent an alert message.
	 */
	boolean isDataSourceDownAlertSent = false;

	/**
	 * The time the pool was last trimmed.
	 */
	long lastTrimTime = 0;

	/**
	 * The sql used to test a connection.
	 */
	String heartbeatsql = null;

	/**
	 * Last time the pool was reset. Used to close busy connections as they are
	 * returned to the pool that where created prior to the lastResetTime.
	 */
	private long lastResetTime = 0;

	/**
	 * Assume that the DataSource is up. heartBeat checking will discover when
	 * it goes down, and comes back up again.
	 */
	private boolean isDataSourceUp = true;

	/**
	 * The current alert.
	 */
	private boolean isWarningMode = false;

	/**
	 * The number of connections to exceed before a warning Alert is fired.
	 */
	private int warningSize;

	/**
	 * Properties used to create a Connection.
	 */
	protected Properties connectionProps;

	/**
	 * The jdbc connection url.
	 */
	protected String databaseUrl;

	/**
	 * The jdbc driver.
	 */
	protected String databaseDriver;

	/**
	 * The minimum number of connections this pool will maintain.
	 */
	private int minConnections;

	/**
	 * The maximum number of connections this pool will grow to.
	 */
	private int maxConnections;

	/**
	 * The time a thread will wait for a connection to become available.
	 */
	private int waitTimeout;

	/**
	 * Flag indicating that the pool is shutting down.
	 */
	private boolean doingShutdown = false;

	/**
	 * By default trim connections that are inactive for longer than this time.
	 */
	private long maxInactiveTime = 5 * 60 * 1000;

	/**
	 * The unique incrementing ID of a connection.
	 */
	private int uniqueConnectionID;

	/**
	 * list of the available connections.
	 */
	private ArrayList<PooledConnection> freeList = new ArrayList<PooledConnection>();

	private ArrayList<PooledConnection> busyList = new ArrayList<PooledConnection>();

	/**
	 * Holds Database metadata from this connection. Aka Tables, Columns,
	 * Primary keys etc.
	 */
	private DictionaryInfo dictionaryInfo;

	/**
	 * The transaction isolation level as per java.sql.Connection.
	 */
	private int transactionIsolation = -1;

	/**
	 * The default autoCommit setting for Connections in this pool.
	 */
	private boolean autoCommit = false;

	/**
	 * Used to find and close() leaked connections. Leaked connections are
	 * thought to be busy but have not been used for some time. Each time a
	 * connection is used it sets it's lastUsedTime.
	 */
	private long leakTime = 60 * 1000 * 5;

	/**
	 * Create the pool.
	 */
	public DataSourcePool(DataSourceManager manager, DataSourceParams params) {
		this.manager = manager;
		initParams(params);
		try {
			initialise();
		} catch (SQLException ex) {
			throw new DataSourceException(ex);
		}
	}

	/**
	 * Return the parameters used by this pool.
	 */
	public DataSourceParams getParams() {
		return params;
	}

	private void initParams(DataSourceParams params) {
		this.params = params;
		this.connectionProps = params.getConnectionProperties();
		this.name = params.getName();
		this.databaseDriver = params.getDriver();
		this.databaseUrl = params.getUrl();
		this.minConnections = params.getMinConnections();
		this.maxConnections = params.getMaxConnections();
		this.waitTimeout = params.getWaitTimeout();
		this.transactionIsolation = params.getIsolationLevel();
		this.heartbeatsql = params.getHeartBeatSql();
	}

	/**
	 * Returns false.
	 */
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		return false;
	}

	/**
	 * Not Implemented.
	 */
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		throw new SQLException("Not Implemented");
	}

	/**
	 * Return the dictionary associated with this pool.
	 */
	public DictionaryInfo getDictionaryInfo() {
		return dictionaryInfo;
	}

	private void initialise() throws SQLException {

		// Ensure database driver is loaded
		try {
			Class.forName(this.databaseDriver);
		} catch (ClassNotFoundException e) {
			throw new SQLException("Database Driver class not found: " + e.getMessage());
		}

		String transIsolation = TransactionIsolation.getLevelDescription(transactionIsolation);
		StringBuffer sb = new StringBuffer();
		sb.append("DataSourcePool [").append(name);
		sb.append("] autoCommit[").append(autoCommit);
		sb.append("] transIsolation[").append(transIsolation);
		sb.append("] min[").append(minConnections);
		sb.append("] max[").append(maxConnections).append("]");

		logger.info(sb.toString());

		ensureMinimumConnections();

		this.dictionaryInfo = new DictionaryInfo(this);
	}

	/**
	 * Return the datasource name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns false when the datasource is down.
	 */
	public boolean isDataSourceUp() {
		return isDataSourceUp;
	}

	private void notifyDataSourceIsDown(SQLException ex) {

		if (isExpectedToBeDownNow()) {
			if (isDataSourceUp) {
				String msg = "DataSourcePool [" + name + "] is down but in downtime!";
				logger.log(Level.WARNING, msg, ex);
			}

		} else if (!isDataSourceDownAlertSent) {

			String msg = "FATAL: DataSourcePool [" + name + "] is down!!!";
			logger.log(Level.SEVERE, msg, ex);
			if (manager != null) {
				manager.notifyDataSourceDown(name);
			}
			isDataSourceDownAlertSent = true;

		}
		if (isDataSourceUp) {
			reset();
		}
		isDataSourceUp = false;
	}

	private void notifyDataSourceIsUp() {
		if (isDataSourceDownAlertSent) {
			String msg = "RESOLVED FATAL: DataSourcePool [" + name + "] is back up!";
			logger.log(Level.SEVERE, msg);
			if (manager != null) {
				manager.notifyDataSourceUp(name);
			}
			isDataSourceDownAlertSent = false;

		} else if (!isDataSourceUp) {
			logger.log(Level.WARNING, "DataSourcePool [" + name + "] is back up!");
		}

		if (!isDataSourceUp) {
			isDataSourceUp = true;
			reset();
		}
	}

	/**
	 * Check the datasource is up. Trim connections.
	 */
	protected void checkDataSource() {
		try {
			// test to see if we can create a new connection...
			Connection conn = createUnpooledConnection();
			testConnection(conn);
			conn.close();

			notifyDataSourceIsUp();

			if (System.currentTimeMillis() > (lastTrimTime + maxInactiveTime)) {
				trimInactiveConnections();
				ensureMinimumConnections();
				lastTrimTime = System.currentTimeMillis();
			}

		} catch (SQLException ex) {
			notifyDataSourceIsDown(ex);
		}
	}

	private boolean isExpectedToBeDownNow() {
		// downtime as controlled as a scheduled task
		// for example, if the database is down as 23.50 every night
		// for 10 minutes schedule the ...lib.cron.Downtime to run
		// at that time for that duration.
		return CronManager.isDowntime();
	}

	/**
	 * Create a Connection that will not be part of the connection pool.
	 * 
	 * <p>
	 * When this connection is closed it will not go back into the pool.
	 * </p>
	 * 
	 * <p>
	 * If withDefaults is true then the Connection will have the autoCommit and
	 * transaction isolation set to the defaults for the pool.
	 * </p>
	 */
	public Connection createUnpooledConnection() throws SQLException {

		try {
			Connection conn = DriverManager.getConnection(databaseUrl, connectionProps);
			conn.setAutoCommit(autoCommit);
			conn.setTransactionIsolation(transactionIsolation);
			return conn;

		} catch (SQLException ex) {
			notifyDataSourceIsDown(null);
			throw ex;
		}
	}

	/**
	 * Set a new maximum size. The pool should respect this new maximum
	 * immediately and not require a restart. You may want to increase the
	 * maxConnections if the pool gets large and hits the warning and or alert
	 * levels.
	 */
	public void setMaxSize(int max) {
		this.maxConnections = max;
	}

	/**
	 * Return the max size this pool can grow to.
	 */
	public int getMaxSize() {
		return maxConnections;
	}

	/**
	 * Set the min size this pool should maintain.
	 */
	public void setMinSize(int min) {
		this.minConnections = min;
	}

	/**
	 * Return the min size this pool should maintain.
	 */
	public int getMinSize() {
		return minConnections;
	}

	/**
	 * Set the time after which inactive connections are trimmed.
	 */
	public void setMaxInactiveTime(long maxInactiveTime) {
		this.maxInactiveTime = maxInactiveTime;
	}

	/**
	 * Return the time after which inactive connections are trimmed.
	 */
	public long getMaxInactiveTime() {
		return maxInactiveTime;
	}

	private void testConnection(Connection conn) throws SQLException {

		if (heartbeatsql == null) {
			return;
		}
		Statement stmt = null;
		ResultSet rset = null;
		try {
			// It should only error IF the DataSource is down ? (or a network
			// issue?)
			stmt = conn.createStatement();
			rset = stmt.executeQuery(heartbeatsql);
			conn.commit();

		} finally {
			try {
				if (rset != null) {
					rset.close();
				}
			} catch (SQLException e) {
				logger.log(Level.SEVERE, null, e);
			}
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException e) {
				logger.log(Level.SEVERE, null, e);
			}
		}
	}

	/**
	 * Make sure the connection is still ok to use. If not then remove it from
	 * the pool.
	 */
	protected boolean validateConnection(PooledConnection conn) {
		try {
			if (heartbeatsql == null) {
				logger.info("Can not test connection as heartbeatsql is not set");
				// conn.closeConnectionFully(true);
				return false;
			}

			testConnection(conn);
			return true;

		} catch (Exception e) {
			String desc = "heartbeatsql test failed on connection[" + conn.getName() + "]";
			logger.warning(desc);
			return false;
		}
	}

	/**
	 * Called by the PooledConnection themselves, returning themselves to the
	 * pool when they have been finished with.
	 * <p>
	 * Note that connections may not be added back to the pool if returnToPool
	 * is false or if they where created before the recycleTime. In both of
	 * these cases the connection is fully closed and not pooled.
	 * </p>
	 * 
	 * @param pooledConnection
	 *            the returning connection
	 * 
	 */
	protected void returnConnection(PooledConnection pooledConnection) {

		if (pooledConnection.getCreationTime() <= lastResetTime) {
			pooledConnection.closeConnectionFully(false);

		} else {
			synchronized (freeList) {
				if (!busyList.remove(pooledConnection)) {
					logger.warning("Connection [" + pooledConnection + "] not found in BusyList? ");
				}

				// we are returning this connection back into the pool
				freeList.add(pooledConnection);
				freeList.notify();
			}
		}
	}

	/**
	 * Remove the connection from the pool.
	 */
	protected void removeConnection(PooledConnection pooledConnection) {
		synchronized (freeList) {
			busyList.remove(pooledConnection);
		}
	}

	/**
	 * Returns information describing connections that are currently being used.
	 */
	public String getBusyConnectionInformation() {

		synchronized (freeList) {
			StringBuffer sb = new StringBuffer();
			Iterator<PooledConnection> i = busyList.iterator();
			while (i.hasNext()) {
				PooledConnection pc = (PooledConnection) i.next();
				String methodLine = pc.getCreatedByMethod();

				sb.append("name[").append(pc.getName()).append("] startTime[").append(
						pc.getStartUseTime()).append("] stmt[").append(pc.getLastStatement())
						.append("] createdBy[").append(methodLine).append("]\r\n");

			}
			return sb.toString();
		}
	}

	/**
	 * Close any busy connections that have not been used for some time.
	 * <p>
	 * These connections are considered to have leaked from the connection pool.
	 * </p>
	 * <p>
	 * Connection leaks occur when code doesn't ensure that connections are
	 * closed() after they have been finished with. There should be an
	 * appropriate try catch finally block to ensure connections are always
	 * closed and put back into the pool.
	 * </p>
	 */
	public void closeBusyConnections(long unusedForMillis) {

		synchronized (freeList) {

			long olderThanTime = System.currentTimeMillis() - unusedForMillis;

			// firstly find all the PooledConnection that should be closed
			ArrayList<PooledConnection> listToClose = new ArrayList<PooledConnection>();
			Iterator<PooledConnection> i = busyList.iterator();
			while (i.hasNext()) {
				PooledConnection pc = (PooledConnection) i.next();
				long lastUsedTime = pc.getLastUsedTime();
				if (lastUsedTime > olderThanTime) {
					// Busy PooledConnection has been used recently so not
					// closing...

				} else {
					listToClose.add(pc);
				}
			}

			// now close them...
			Iterator<PooledConnection> closeIt = listToClose.iterator();
			while (closeIt.hasNext()) {
				PooledConnection pc = (PooledConnection) closeIt.next();

				try {
					String methodLine = pc.getCreatedByMethod();

					Date luDate = new Date();
					luDate.setTime(pc.getLastUsedTime());

					String msg = "DataSourcePool closing leaked connection? " + " name["
							+ pc.getName() + "] lastUsed[" + luDate + "] createdBy[" + methodLine
							+ "] lastStmt[" + pc.getLastStatement() + "]";

					logger.warning(msg);

					pc.close();

				} catch (SQLException ex) {
					// this should never actually happen
					logger.log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	/**
	 * Grow the pool by creating a new connection. The connection can either be
	 * added to the available list, or returned.
	 */
	private PooledConnection createConnection() throws SQLException {

		PooledConnection connection = null;
		try {
			uniqueConnectionID++;
			connection = new PooledConnection(this, uniqueConnectionID);
			connection.resetForUse();

			if (!isDataSourceUp) {
				notifyDataSourceIsUp();
			}

		} catch (SQLException ex) {
			notifyDataSourceIsDown(ex);
			throw ex;
		}

		int busy = busyList.size();
		int size = busy + freeList.size();

		String msg = "DataSourcePool [" + name + "] grow pool; " + " busy[" + busy + "] size["
				+ size + "] max[" + maxConnections + "]";

		logger.info(msg);

		checkForWarningSize();

		return connection;
	}

	/**
	 * Close all the connections in the pool.
	 * <p>
	 * <ul>
	 * <li>Checks that the database is up.
	 * <li>Resets the Alert level.
	 * <li>Closes busy connections that have not been used for some time (aka
	 * leaks).
	 * <li>This closes all the currently available connections.
	 * <li>Busy connections are closed when they are returned to the pool.
	 * </ul>
	 * </p>
	 */
	public void reset() {
		synchronized (freeList) {
			logger.info("Reseting DataSourcePool [" + name + "]");
			lastResetTime = System.currentTimeMillis();

			closeFreeConnections(false);
			closeBusyConnections(leakTime);

			String busyMsg = "Busy Connections:\r\n" + getBusyConnectionInformation();
			logger.info(busyMsg);
		}

		isWarningMode = false;
	}

	private void closeFreeConnections(boolean logErrors) {
		synchronized (freeList) {
			while (!freeList.isEmpty()) {
				PooledConnection conn = (PooledConnection) freeList.remove(0);
				conn.closeConnectionFully(logErrors);
			}
		}
	}

	/**
	 * Return a pooled connection.
	 */
	public Connection getConnection() throws SQLException {
		return getPooledConnection();
	}

	// -----------------------------------------------------------------
	// Pulled this method as getting the StackTrace is pretty expensive.
	// ------------------------------------------------------------------
	// public PooledConnection getPooledConnection() throws SQLException {
	//
	// PooledConnection conn = getPooledConnectionRaw();
	//
	// // StackTraceElement[] st = Thread.currentThread().getStackTrace();
	// // conn.setStackTrace(st);
	//        
	// // create a stack trace for the method that created the connection
	// // Do this so that we can find code that causes connection leaks
	// // try {
	// // throw new RuntimeException("Connect Leak trace");
	// //
	// // } catch (RuntimeException ex) {
	// // StackTraceElement[] st = ex.getStackTrace();
	// // conn.setStackTrace(st);
	// // }
	// return conn;
	// }

	/**
	 * Get a connection from the pool.
	 * <p>
	 * This will grow the pool if all the current connections are busy. This
	 * will go into a wait if the pool has hit its maximum size.
	 * </p>
	 */
	public PooledConnection getPooledConnection() throws SQLException {

		if (doingShutdown) {
			throw new SQLException("Trying to access the Connection Pool when it is shutting down");
		}

		PooledConnection connection = null;

		synchronized (freeList) {
			int freeSize = freeList.size();
			if (freeSize > 0) {
				// If idle connections are available, grab one
				connection = (PooledConnection) freeList.remove(freeSize - 1);
				// connection = (PooledConnection) freeList.remove(0);
				connection.resetForUse();

				busyList.add(connection);
				return connection;
			}

			int busySize = busyList.size();
			if ((busySize + freeSize) < maxConnections) {
				// If the pool size has not maxed out, create a new connection
				// and use it
				connection = createConnection();
				busyList.add(connection);
				return connection;
			}

			// the pool has grown to its max size... avoid this!!!!
			reset();

			try {
				freeList.wait(waitTimeout);
			} catch (InterruptedException e) {
			}

			if (!freeList.isEmpty()) {
				connection = (PooledConnection) freeList.remove(0);
				connection.resetForUse();
				busyList.add(connection);
				return connection;

			} else {

				String s = "Unsuccessfully waited for a connection to be returned."
						+ " No connections are free. You need to Increase the max connections"
						+ " or look for a connection pool leak.";

				throw new SQLException(s);
			}
		}
	}

	/**
	 * Send a message to the DataSourceAlertListener to test it. This is so that
	 * you can make sure the alerter is configured correctly etc.
	 */
	public void testAlert() {

		String subject = "Test DataSourcePool [" + name + "]";
		String msg = "Just testing if alert message is sent successfully.";

		if (manager != null) {
			manager.notifyWarning(subject, msg);
		}
	}

	/**
	 * As the pool grows it gets closer to the maxConnections limit. We can send
	 * an Alert (or warning) as we get close to this limit and hence an
	 * Administrator could increase the pool size if that is possible.
	 * <P>
	 * 
	 * This is called whenever the pool grows in size (towards the max limit).
	 */
	private void checkForWarningSize() {

		// the the total number of connections that we can add to the pool
		// before it hits the maximum
		int availableGrowth = (maxConnections - getSize());

		if (availableGrowth < warningSize) {

			closeBusyConnections(leakTime);

			if (!isWarningMode) {
				// send an Error to the event log...
				isWarningMode = true;

				String subject = "DataSourcePool [" + name + "] warning";
				String msg = "DataSourcePool [" + name + "] is [" + availableGrowth
						+ "] connections from its maximum size.";
				logger.warning(msg);
				if (manager != null) {
					manager.notifyWarning(subject, msg);
				}
			}
		}
	}

	/**
	 * This will close all the free connections, and then go into a wait loop,
	 * waiting for the busy connections to be freed.
	 * 
	 * <p>
	 * The DataSources's should be shutdown AFTER threadpools. Leaked
	 * Connections are not waited on, as that would hang the server.
	 * </p>
	 */
	public void shutdown() {
		synchronized (freeList) {
			String m = "DataSourcePool [" + name + "] shutdown";
			logger.info(m);

			doingShutdown = true;
			closeFreeConnections(true);

			if (getSize() > 0) {
				String msg = "A potential connection leak was detected.  Total connections: "
						+ getSize();
				logger.warning(msg);

				closeBusyConnections(0);
			}
		}
	}

	/**
	 * Trim connections that have been not used for some time. The inactive time
	 * is set by setTrimInactiveTime() and defaults to 5 minutes.
	 */
	private void trimInactiveConnections() {

		synchronized (freeList) {

			int maxTrim = freeList.size() - minConnections;
			if (maxTrim <= 0) {
				return;
			}
			int trimedCount = 0;
			long usedSince = System.currentTimeMillis() - maxInactiveTime;

			Iterator<PooledConnection> it = freeList.iterator();
			while (it.hasNext()) {
				PooledConnection conn = (PooledConnection) it.next();
				if (conn.getLastUsedTime() < usedSince) {
					trimedCount++;
					it.remove();
					conn.closeConnectionFully(true);
					if (trimedCount >= maxTrim) {
						break;
					}
				}
			}
			if (trimedCount > 0) {
				String msg = "DataSourcePool [" + name + "] trimmed [" + trimedCount
						+ "] inactive connections size[" + getSize() + "]";
				logger.info(msg);
			}
		}
	}

	private void ensureMinimumConnections() {

		synchronized (freeList) {
			if (getSize() < minConnections) {
				return;
			}
			int numToAdd = minConnections - getSize();
			if (numToAdd > 0) {
				try {
					for (int i = 0; i < numToAdd; i++) {
						PooledConnection conn = createConnection();
						freeList.add(conn);
					}
					freeList.notify();

				} catch (SQLException e) {
					logger.log(Level.SEVERE, null, e);
				}
			}
		}
	}

	/**
	 * The number of busy connections in the pool.
	 */
	public int getBusyCount() {
		synchronized (freeList) {
			return busyList.size();
		}
	}

	/**
	 * The total number of connections in the pool.
	 */
	public int getSize() {
		synchronized (freeList) {
			return freeList.size() + busyList.size();
		}
	}

	/**
	 * Not implemented and shouldn't be used.
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		throw new SQLException("Method not supported");
	}

	/**
	 * Not implemented and shouldn't be used.
	 */
	public int getLoginTimeout() throws SQLException {
		throw new SQLException("Method not supported");
	}

	/**
	 * Not implemented and shouldn't be used.
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
		throw new SQLException("Method not supported");
	}

	/**
	 * Returns null.
	 */
	public PrintWriter getLogWriter() {
		return null;
	}

	/**
	 * Not implemented.
	 */
	public void setLogWriter(PrintWriter writer) throws SQLException {
		throw new SQLException("Method not supported");
	}

	/**
	 * Set the default autoCommit setting used for all connections in this pool.
	 * All Connections in this pool will be created with this autoCommit
	 * setting, and will be reset back to this setting when they are returned to
	 * the pool.
	 */
	public void setAutoCommit(boolean autoCommit) {
		this.autoCommit = autoCommit;
	}

	/**
	 * Return the default autoCommit setting Connections in this pool will use.
	 * 
	 * @return true if the pool defaults autoCommit to true
	 */
	public boolean getAutoCommit() {
		return autoCommit;
	}

	/**
	 * Set the default transaction isolation level that all Connections in this
	 * pool should have. This gets set when Connections are created and when
	 * they are returned to the pool.
	 */
	public void setTransactionIsolation(int level) {
		this.transactionIsolation = level;
	}

	/**
	 * Return the default transaction isolation level connections in this pool
	 * should have.
	 * 
	 * @return the default transaction isolation level
	 */
	public int getTransactionIsolation() {
		return transactionIsolation;
	}

	/**
	 * For detecting and closing leaked connections. Connections that have been
	 * busy for more than (leakTime/5 minutes) are considered leaks and will be
	 * closed on a reset().
	 * <p>
	 * If you want to use a connection for that longer then you should consider
	 * creating an unpooled connection.
	 * </p>
	 */
	public void setLeakTime(long leakTimeMillis) {
		this.leakTime = leakTimeMillis;
	}

}
