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
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.server.lib.cron.CronManager;

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

	private static final Logger logger = Logger.getLogger(DataSourcePool.class.getName());

	/**
	 * The name given to this dataSource.
	 */
	private final String name;

	/**
	 * Used to notify of changes to the DataSource status.
	 */
	private final DataSourceNotify notify;

	/**
	 * Optional listener that can be notified when connections
	 * are got from and put back into the pool.
	 */
	private final DataSourcePoolListener poolListener;
	
	/**
	 * Properties used to create a Connection.
	 */
	private final Properties connectionProps;

	/**
	 * The jdbc connection url.
	 */
	private final String databaseUrl;

	/**
	 * The jdbc driver.
	 */
	private final String databaseDriver;

	/**
	 * The sql used to test a connection.
	 */
	private final String heartbeatsql;
	
	/**
	 * The transaction isolation level as per java.sql.Connection.
	 */
	private final int transactionIsolation;

	/**
	 * The default autoCommit setting for Connections in this pool.
	 */
	private final boolean autoCommit;
	
	/**
	 * Flag set to true to capture stackTraces (can be expensive).
	 */
	private boolean captureStackTrace;
	
	/**
	 * flag to indicate we have sent an alert message.
	 */
	private boolean dataSourceDownAlertSent;

	/**
	 * The time the pool was last trimmed.
	 */
	private long lastTrimTime;

	/**
	 * Last time the pool was reset. Used to close busy connections as they are
	 * returned to the pool that where created prior to the lastResetTime.
	 */
	private long lastResetTime;

	/**
	 * Assume that the DataSource is up. heartBeat checking will discover when
	 * it goes down, and comes back up again.
	 */
	private boolean dataSourceUp = true;

	/**
	 * The current alert.
	 */
	private boolean inWarningMode;

	/**
	 * The number of connections to exceed before a warning Alert is fired.
	 */
	private int warningSize;
	
	/**
	 * The size of the preparedStatement cache;
	 */
	private int pstmtCacheSize;
	
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
	private int waitTimeoutMillis;

	/**
	 * Flag indicating that the pool is shutting down.
	 */
	private boolean doingShutdown;

	/**
	 * By default trim connections that are inactive for longer than this time.
	 */
	private int maxInactiveTimeSecs;

	/**
	 * The unique incrementing ID of a connection.
	 */
	private int uniqueConnectionID;

	/**
	 * list of the available connections.
	 */
	private final ArrayList<PooledConnection> freeList = new ArrayList<PooledConnection>();

	private final ArrayList<PooledConnection> busyList = new ArrayList<PooledConnection>();

	/**
	 * Used to find and close() leaked connections. Leaked connections are
	 * thought to be busy but have not been used for some time. Each time a
	 * connection is used it sets it's lastUsedTime.
	 */
	private long leakTimeMinutes;

	/**
	 * Create the pool.
	 */
	public DataSourcePool(DataSourceNotify notify, DataSourceParams params) {

		this.notify = notify;
		this.name = params.getName();
		this.poolListener = createPoolListener(params.getPoolListener());
		
		this.maxInactiveTimeSecs = params.getMaxInactiveTimeSecs();
		this.leakTimeMinutes = params.getLeakTimeMinutes();
		this.captureStackTrace = params.isCaptureStackTrace();
		this.autoCommit = params.isAutoCommit();
		this.databaseDriver = params.getDriver();
		this.databaseUrl = params.getUrl();
		this.pstmtCacheSize = params.getPstmtCacheSize();
		this.minConnections = params.getMinConnections();
		this.maxConnections = params.getMaxConnections();
		this.waitTimeoutMillis = params.getWaitTimeout();
		this.transactionIsolation = params.getIsolationLevel();
		this.heartbeatsql = params.getHeartBeatSql();
		
        connectionProps = new Properties();
        connectionProps.setProperty("user", params.getUsername());
        connectionProps.setProperty("password", params.getPassword());
        
		try {
			initialise();
		} catch (SQLException ex) {
			throw new DataSourceException(ex);
		}
	}
	
	public DataSourcePool(DataSourceNotify notify, String name, DataSourceConfig params) {

		this.notify = notify;
		this.name = name;
		this.poolListener = createPoolListener(params.getPoolListener());
		
		this.autoCommit = false;
		this.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED;

		this.maxInactiveTimeSecs = params.getMaxInactiveTimeSecs();
		this.leakTimeMinutes = params.getLeakTimeMinutes();
		this.captureStackTrace = params.isCaptureStackTrace();
		this.databaseDriver = params.getDriver();
		this.databaseUrl = params.getUrl();
		this.pstmtCacheSize = params.getPstmtCacheSize();
		this.minConnections = params.getMinConnections();
		this.maxConnections = params.getMaxConnections();
		this.waitTimeoutMillis = params.getWaitTimeoutMillis();
		this.heartbeatsql = params.getHeartbeatSql();
		
		String un = params.getUsername();
		String pw = params.getPassword();
		if (un == null){
			throw new RuntimeException("DataSource user is null?");
		}
		if (pw == null){
			throw new RuntimeException("DataSource password is null?");
		}
        connectionProps = new Properties();
        connectionProps.setProperty("user", un);
        connectionProps.setProperty("password", pw);
        
		try {
			initialise();
		} catch (SQLException ex) {
			throw new DataSourceException(ex);
		}
	}
	
	/**
	 * Create the DataSourcePoolListener if there is one.
	 */
	private DataSourcePoolListener createPoolListener(String cn) {
		if (cn == null){
			return null;
		} 
		try {
			Class<?> cls = Class.forName(cn);
			return (DataSourcePoolListener)cls.newInstance();
		} catch (Exception e){
			throw new DataSourceException(e);
		}
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
	 * Return the dataSource name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns false when the dataSource is down.
	 */
	public boolean isDataSourceUp() {
		return dataSourceUp;
	}

	private void notifyDataSourceIsDown(SQLException ex) {

		if (isExpectedToBeDownNow()) {
			if (dataSourceUp) {
				String msg = "DataSourcePool [" + name + "] is down but in downtime!";
				logger.log(Level.WARNING, msg, ex);
			}

		} else if (!dataSourceDownAlertSent) {

			String msg = "FATAL: DataSourcePool [" + name + "] is down!!!";
			logger.log(Level.SEVERE, msg, ex);
			if (notify != null) {
				notify.notifyDataSourceDown(name);
			}
			dataSourceDownAlertSent = true;

		}
		if (dataSourceUp) {
			reset();
		}
		dataSourceUp = false;
	}

	private void notifyDataSourceIsUp() {
		if (dataSourceDownAlertSent) {
			String msg = "RESOLVED FATAL: DataSourcePool [" + name + "] is back up!";
			logger.log(Level.SEVERE, msg);
			if (notify != null) {
				notify.notifyDataSourceUp(name);
			}
			dataSourceDownAlertSent = false;

		} else if (!dataSourceUp) {
			logger.log(Level.WARNING, "DataSourcePool [" + name + "] is back up!");
		}

		if (!dataSourceUp) {
			dataSourceUp = true;
			reset();
		}
	}

	/**
	 * Check the dataSource is up. Trim connections.
	 */
	protected void checkDataSource() {
		try {
			// test to see if we can create a new connection...
			Connection conn = createUnpooledConnection();
			testConnection(conn);
			conn.close();

			notifyDataSourceIsUp();

			if (System.currentTimeMillis() > (lastTrimTime + (maxInactiveTimeSecs*1000))) {
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
	public void setMaxInactiveTimeSecs(int maxInactiveTimeSecs) {
		this.maxInactiveTimeSecs = maxInactiveTimeSecs;
	}

	/**
	 * Return the time after which inactive connections are trimmed.
	 */
	public int getMaxInactiveTimeSecs() {
		return maxInactiveTimeSecs;
	}

	private void testConnection(Connection conn) throws SQLException {

		if (heartbeatsql == null) {
			return;
		}
		Statement stmt = null;
		ResultSet rset = null;
		try {
			// It should only error IF the DataSource is down ? (or a network issue?)
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

		if (poolListener != null){
			poolListener.onBeforeReturnConnection(pooledConnection);
		}
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
			StringBuilder sb = new StringBuilder();
			Iterator<PooledConnection> i = busyList.iterator();
			while (i.hasNext()) {
				PooledConnection pc = i.next();
				sb.append(pc.getDescription()).append("\r\n");
			}
			return sb.toString();
		}
	}
	
	/**
	 * Dumps the busy connection information to the logs.
	 * <p>
	 * This includes the stackTrace elements if they are being captured.
	 * This is useful when needing to look a potential connection pool leaks.
	 * </p>
	 */
	public void dumpBusyConnectionInformation() {

		synchronized (freeList) {
			
			logger.info("Dumping busy connections: (Use datasource.xxx.capturestacktrace=true  ... to get stackTraces)");
			
			Iterator<PooledConnection> i = busyList.iterator();
			while (i.hasNext()) {
				PooledConnection pc = (PooledConnection) i.next();
				StackTraceElement[] stackTrace = pc.getStackTrace();
				
				logger.info(pc.getDescription());
				if (stackTrace != null){
					logger.info("Connect["+pc.getName()+"] stackTrace: "+Arrays.toString(stackTrace));
				}
			}
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
	public void closeBusyConnections(long leakTimeMinutes) {

		synchronized (freeList) {

			long olderThanTime = System.currentTimeMillis() - (leakTimeMinutes*60000);

			// firstly find all the PooledConnection that should be closed
			ArrayList<PooledConnection> listToClose = new ArrayList<PooledConnection>();
			Iterator<PooledConnection> i = busyList.iterator();
			while (i.hasNext()) {
				PooledConnection pc = (PooledConnection) i.next();
				if (pc.isLongRunning() || pc.getLastUsedTime() > olderThanTime) {
					// PooledConnection has been used recently or
					// expected to be longRunning so not closing...

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
	 * <p>
	 * This method is protected by synchronisation in calling methods.
	 * </p>
	 */
	private PooledConnection createConnection() throws SQLException {

		PooledConnection connection = null;
		try {
			uniqueConnectionID++;
			Connection c = createUnpooledConnection();
			
			connection = new PooledConnection(this, uniqueConnectionID, c);
			connection.resetForUse();

			if (!dataSourceUp) {
				notifyDataSourceIsUp();
			}

		} catch (SQLException ex) {
			notifyDataSourceIsDown(ex);
			throw ex;
		}

		int busy = busyList.size();
		int size = busy + freeList.size();

		String msg = "DataSourcePool [" + name + "] grow pool; " + " busy[" + busy + "] size["+ size + "] max[" + maxConnections + "]";
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
	 * <li>Closes busy connections that have not been used for some time (aka leaks).
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
			closeBusyConnections(leakTimeMinutes);

			String busyMsg = "Busy Connections:\r\n" + getBusyConnectionInformation();
			logger.info(busyMsg);

			inWarningMode = false;
		}
	}

	private void closeFreeConnections(boolean logErrors) {
		synchronized (freeList) {
			while (!freeList.isEmpty()) {
				PooledConnection conn = (PooledConnection) freeList.remove(0);
				logger.info("PSTMT Statistics: "+conn.getStatistics());
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

	/**
	 * Get a connection from the pool.
	 * <p>
	 * This will grow the pool if all the current connections are busy. This
	 * will go into a wait if the pool has hit its maximum size.
	 * </p>
	 */
	public PooledConnection getPooledConnection() throws SQLException {

		PooledConnection c = _getPooledConnection();
		
		if (captureStackTrace){
			c.setStackTrace(Thread.currentThread().getStackTrace());			
		}
		
		if (poolListener != null){
			poolListener.onAfterBorrowConnection(c);
		}
		return c;
	}
	
	private PooledConnection _getPooledConnection() throws SQLException {

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
				freeList.wait(waitTimeoutMillis);
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

		if (notify != null) {
			notify.notifyWarning(subject, msg);
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

			closeBusyConnections(leakTimeMinutes);

			if (!inWarningMode) {
				// send an Error to the event log...
				inWarningMode = true;

				String subject = "DataSourcePool [" + name + "] warning";
				String msg = "DataSourcePool [" + name + "] is [" + availableGrowth+ "] connections from its maximum size.";
				logger.warning(msg);
				if (notify != null) {
					notify.notifyWarning(subject, msg);
				}
			}
		}
	}

	/**
	 * This will close all the free connections, and then go into a wait loop,
	 * waiting for the busy connections to be freed.
	 * 
	 * <p>
	 * The DataSources's should be shutdown AFTER thread pools. Leaked
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
				String msg = "A potential connection leak was detected.  Total connections: "+ getSize();
				logger.warning(msg);
				
				dumpBusyConnectionInformation();

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
			long usedSince = System.currentTimeMillis() - (maxInactiveTimeSecs*1000);

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
				String msg = "DataSourcePool [" + name + "] trimmed [" + trimedCount+ "] inactive connections size[" + getSize() + "]";
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
	 * Return the default autoCommit setting Connections in this pool will use.
	 * 
	 * @return true if the pool defaults autoCommit to true
	 */
	public boolean getAutoCommit() {
		return autoCommit;
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
	 * Return true if the connection pool is currently capturing the StackTrace
	 * when connections are 'got' from the pool.
	 * <p>
	 * This is set to true to help diagnose connection pool leaks.
	 * </p>
	 */
	public boolean isCaptureStackTrace() {
		return captureStackTrace;
	}

	/**
	 * Set this to true means that the StackElements are captured every time
	 * a connection is retrieved from the pool. This can be used to identify
	 * connection pool leaks.
	 */
	public void setCaptureStackTrace(boolean captureStackTrace) {
		this.captureStackTrace = captureStackTrace;
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
	 * For detecting and closing leaked connections. Connections that have been
	 * busy for more than leakTimeMinutes are considered leaks and will be
	 * closed on a reset().
	 * <p>
	 * If you want to use a connection for that longer then you should consider
	 * creating an unpooled connection or setting longRunning to true on that 
	 * connection.
	 * </p>
	 */
	public void setLeakTimeMinutes(long leakTimeMinutes) {
		this.leakTimeMinutes = leakTimeMinutes;
	}

	/**
	 * Return the number of minutes after which a busy connection could be considered 
	 * leaked from the connection pool.
	 */
	public long getLeakTimeMinutes() {
		return leakTimeMinutes;
	}

	/**
	 * Return the preparedStatement cache size.
	 */
	public int getPstmtCacheSize() {
		return pstmtCacheSize;
	}

	/**
	 * Set the preparedStatement cache size.
	 */
	public void setPstmtCacheSize(int pstmtCacheSize) {
		this.pstmtCacheSize = pstmtCacheSize;
	}
	
}
