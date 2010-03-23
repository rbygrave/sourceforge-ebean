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
package com.avaje.ebeaninternal.server.lib.sql;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A decorator builder which uses java {@link Proxy} methods to wrap a Connection which provides
 * additional methods as described in {@link PooledConnection}.
 *
 * Technical description about how those Decorator thing works:<br />
 * Note: This description uses the ConnectionDecorator as example, but applies also to the PreparedStatementDecorator.
 * <p>
 * The goal is to add aspects to a standard interface which allow us to deal with special cases used by Ebean internally.
 * Those are e.g. Exception handling: Each exception thrown should be captured and stored in internal datastructures of the
 * decorated class.
 * </p>
 * <p>
 * As a start, we have to define which methods we need to add to the decorated interface. This has been done by
 * adding the {@link PooledConnectionMethods} interface. The {@link PooledConnection} then composes the final interface
 * with {@link Connection}.
 * </p>
 * <p>
 * The decorator itself implements the {@link PooledConnectionMethods} interface to ensure that the methods in question
 * do have the correct signature, else runtime exception could be easily produced.
 * </p>
 *
 * There are three different strategies to override a method
 * <ol>
 * <li>method defined in super class, e.g. {@link Object}</li>
 * <li>method defined in decorated Interface and decoration is handled by a {@link MethodHandler}</li>
 * <li>method defined in decorated Interface and decoration is handled by an "external" method in the decorator itself</li>
 * </ol>
 *
 * <p>
 * For performance reasons the proxy simply looks up a map of method handles and pass the method call to this handler or if no
 * handler is defined it passes the method call to the decorated object. <br />
 * This requires that even the normal Object methods are required to be registered.
 * </p>
 * <p>
 * The method handles map is created in a static constructor of this class and thus runs only once per JVM instance.
 * </p>
 * <p />
 * <p>
 * Case 1 is handled by a standard decorator. The methods in question here are basically the identity giving methods like equals(), hashCode() and
 * obviously toString().<br />
 * The using class deal only with the proxy instance, but the proxy itself simply passes each method call to the decorated class. This makes any
 * equals() not working any more. Thus we override these methods to make them work again.
 * </p>
 * <p />
 * <p>
 * Case 2 is handled by the {@link MethodHandler} by simply applying an "around-advice". This is useful for "mass" method pointcuts.
 * e.g. each and every method should be wrapped with a special exception handler.<br />
 * At the end of the static initializer you will see that every method, provided by the Connection class, which is not yet wrapped,
 * will be wrapped using the special exception handler.
 * </p>
 * <p />
 * <p>
 * Case 3 needs a little trick. The method call comes in using the {@link Method} instance of the combined interface, but we need to call
 * the method of the decorator. Thus, we put the "combined-interface-method" into the method handler map, but also lookup a method with
 * exactly the same signature on the decorator. On delegation then we are able call the decorator method.
 * </p>
 */
public class ConnectionDecorator implements PooledConnectionMethods
{

	private static final Logger logger = Logger.getLogger(ConnectionDecorator.class.getName());

	private static String IDLE_CONNECTION_ACCESSED_ERROR = "Pooled Connection has been accessed whilst idle in the pool, via method: ";

	private final static Class[] PROXY_INTERFACES = new Class[]{PooledConnection.class};

	protected final static Map<Method, MethodHandler<ConnectionDecorator, Connection>> METHOD_HANDLERS =
		new HashMap<Method, MethodHandler<ConnectionDecorator, Connection>>();

	/**
	 * Set when connection is idle in the pool. In general when in the pool the
	 * connection should not be modified.
	 */
	static final int STATUS_IDLE = 88;

	/**
	 * Set when connection given to client.
	 */
	static final int STATUS_ACTIVE = 89;

	/**
	 * Set when commit() or rollback() called.
	 */
	static final int STATUS_ENDED = 87;

	/**
	 * Name used to identify the PooledConnection for logging.
	 */
	final String name;

	/**
	 * The pool this connection belongs to.
	 */
	final DataSourcePool pool;

	/**
	 * The underlying connection.
	 */
	final Connection connection;

	/**
	 * The time this connection was created.
	 */
	final long creationTime;

	/**
	 * Cache of the PreparedStatements
	 */
	final PstmtCache pstmtCache;

	final Object pstmtMonitor = new Object();

	/**
	 * The status of the connection. IDLE, ACTIVE or ENDED.
	 */
	int status = STATUS_IDLE;

	/**
	 * Set this to true if the connection will be busy for a long time.
	 * <p>
	 * This means it should skip the suspected connection pool leak checking.
	 * </p>
	 */
	boolean longRunning;

	/**
	 * Flag to indicate that this connection had errors and should be checked to
	 * make sure it is okay.
	 */
	boolean hadErrors;

	/**
	 * The last start time. When the connection was given to a thread.
	 */
	long startUseTime;

	/**
	 * The last end time of this connection. This is to calculate the usage
	 * time.
	 */
	long lastUseTime;

	/**
	 * The last statement executed by this connection.
	 */
	String lastStatement;

	/**
	 * The number of hits against the preparedStatement cache.
	 */
	int pstmtHitCounter;

	/**
	 * The number of misses against the preparedStatement cache.
	 */
	int pstmtMissCounter;

	/**
	 * The non avaje method that created the connection.
	 */
	String createdByMethod;

	/**
	 * Used to find connection pool leaks.
	 */
	StackTraceElement[] stackTrace;

	int maxStackTrace;

	private PooledConnection decoratedConnection;

	static
	{
		final MethodHandler delegateToDecoratorHandler = new DelegateToDecoratorHandler();
		final MethodHandler<ConnectionDecorator, Connection> idleCheckMethodHandlerWithEx = new MethodHandler<ConnectionDecorator, Connection>()
		{
			public Object invoke(ConnectionDecorator decorator, Object proxy, Method method, Connection delegate, Object... args) throws Exception
			{
				if (decorator.status == STATUS_IDLE)
				{
					String m = IDLE_CONNECTION_ACCESSED_ERROR + method.getName();
					throw new SQLException(m);
				}

				try
				{
					return method.invoke(delegate, args);
				}
				catch (InvocationTargetException e)
				{
					return handleInvocationTargetException(decorator, e);
				}
			}
		};

		// this captures the sql and assuems it is the first argument
		final MethodHandler<ConnectionDecorator, Connection> idleCheckMethodHandlerWithExAndSql = new MethodHandler<ConnectionDecorator, Connection>()
		{
			public Object invoke(ConnectionDecorator decorator, Object proxy, Method method, Connection delegate, Object... args) throws Exception
			{
				if (decorator.status == STATUS_IDLE)
				{
					String m = IDLE_CONNECTION_ACCESSED_ERROR + method.getName();
					throw new SQLException(m);
				}

				decorator.lastStatement = (String) args[0];

				try
				{
					return method.invoke(delegate, args);
				}
				catch (InvocationTargetException e)
				{
					return handleInvocationTargetException(decorator, e);
				}
			}
		};

		try
		{
			// delegate the default object methods to the decorator
			METHOD_HANDLERS.put(Object.class.getMethod("hashCode"), delegateToDecoratorHandler);
			METHOD_HANDLERS.put(Object.class.getMethod("equals", Object.class), delegateToDecoratorHandler);
			METHOD_HANDLERS.put(Object.class.getMethod("toString"), delegateToDecoratorHandler);

			// delegate all the extended method to the decorator
			for (Method method : PooledConnectionMethods.class.getMethods())
			{
				METHOD_HANDLERS.put(method, delegateToDecoratorHandler);
			}

			// delegate all the more complicated overridden method to the decorator. In this case we have to lookup
			// the method from the Connection interface on the ConnectionDecorator.
			// This ensures that the method uses the correct signature and is required for delegateToDecorator to correctly
			// invoke the method.
			handleDecoratedMethod(Connection.class.getMethod("prepareStatement", String.class, int.class, int.class));
			handleDecoratedMethod(Connection.class.getMethod("prepareStatement", String.class, int.class, int.class));
			handleDecoratedMethod(Connection.class.getMethod("prepareStatement", String.class, int.class));
			handleDecoratedMethod(Connection.class.getMethod("prepareStatement", String.class));
			handleDecoratedMethod(Connection.class.getMethod("close"));
			handleDecoratedMethod(Connection.class.getMethod("setTransactionIsolation", int.class));
			handleDecoratedMethod(Connection.class.getMethod("setReadOnly", boolean.class));
			handleDecoratedMethod(Connection.class.getMethod("commit"));
			handleDecoratedMethod(Connection.class.getMethod("rollback"));

			// now install the simple method handlers
			METHOD_HANDLERS.put(Connection.class.getMethod("nativeSQL", String.class), idleCheckMethodHandlerWithExAndSql);
			METHOD_HANDLERS.put(Connection.class.getMethod("prepareCall", String.class), idleCheckMethodHandlerWithExAndSql);
			METHOD_HANDLERS.put(Connection.class.getMethod("prepareCall", String.class, int.class, int.class), idleCheckMethodHandlerWithExAndSql);
			METHOD_HANDLERS.put(Connection.class.getMethod("prepareCall", String.class, int.class, int.class, int.class), idleCheckMethodHandlerWithExAndSql);

			// now install the default handler (idle check guard) for all other methods
			for (Method method : Connection.class.getMethods())
			{
				if (!METHOD_HANDLERS.containsKey(method))
				{
					METHOD_HANDLERS.put(method, idleCheckMethodHandlerWithEx);
				}
			}
		}
		catch (NoSuchMethodException e)
		{
			// if this happens something is very wrong
			throw new RuntimeException(e);
		}
	}

	private static void handleDecoratedMethod(Method method)
	{
		METHOD_HANDLERS.put(method, new DelegateToDecoratedMethodHandler(findOnDecorator(method)));
	}

	private static Method findOnDecorator(Method method)
	{
		for (Method decoratedMethod : ConnectionDecorator.class.getMethods())
		{
			if (!method.getName().equals(decoratedMethod.getName())
				|| !method.getReturnType().equals(decoratedMethod.getReturnType())
				|| !Arrays.equals(method.getParameterTypes(), decoratedMethod.getParameterTypes()))
			{
				continue;
			}

			return decoratedMethod;
		}

		throw new IllegalArgumentException("method " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()) + ") not found on decorator.");
	}

	private static Void handleInvocationTargetException(ConnectionDecorator decorator, InvocationTargetException e) throws SQLException, InvocationTargetException
	{
		Throwable targetException = e.getTargetException();
		if (targetException instanceof SQLException)
		{
			decorator.addError(targetException);
			throw (SQLException) targetException;
		}

		throw e;
	}

	/**
	 * Construct the connection that can refer back to the pool it belongs to.
	 * <p>
	 * close() will return the connection back to the pool , while
	 * closeDestroy() will close() the underlining connection properly.
	 * </p>
	 */
	public ConnectionDecorator(DataSourcePool pool, int uniqueId, Connection connection) throws SQLException
	{

		this.pool = pool;
		this.connection = connection;
		this.name = pool.getName() + "." + uniqueId;
		this.pstmtCache = new PstmtCache(name, pool.getPstmtCacheSize());
		this.maxStackTrace = pool.getMaxStackTraceSize();
		this.creationTime = System.currentTimeMillis();
		this.lastUseTime = creationTime;
	}

	/**
	 * create the decorator which looks like a PreparedStatement which also implements the
	 * {@link ExtendedPreparedStatement} interface.<br />
	 * That way we are no longer bound to a specific JDK at compile time.
	 */
	public PooledConnection buildDecorator()
	{
		decoratedConnection = (PooledConnection) Proxy.newProxyInstance(PooledConnection.class.getClassLoader(),
			PROXY_INTERFACES,
			new InvocationHandler()
			{
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
				{
					MethodHandler<ConnectionDecorator, Connection> handler = METHOD_HANDLERS.get(method);
					if (handler == null)
					{
						return method.invoke(connection, args);
					}

					return handler.invoke(ConnectionDecorator.this, proxy, method, connection, args);
				}
			});

		return decoratedConnection;
	}

	@Override
	public boolean equals(Object obj)
	{
		return decoratedConnection == obj;
	}

	@Override
	public int hashCode()
	{
		return System.identityHashCode(decoratedConnection);
	}
	
	/**
	 * Return the DataSourcePool that this connection belongs to.
	 */
	public DataSourcePool getDataSourcePool()
	{
		return pool;
	}

	/**
	 * Return the time the connection was created.
	 */
	public long getCreationTime()
	{
		return creationTime;
	}

	/**
	 * Return a string to identify the connection.
	 */
	public String getName()
	{
		return name;
	}

	public String toString()
	{
		return name;
	}

	public String getDescription()
	{
		return "name[" + name + "] startTime[" + getStartUseTime() + "] stmt[" + getLastStatement() + "] createdBy[" + getCreatedByMethod() + "]";
	}

	public String getStatistics()
	{
		return "name[" + name + "] startTime[" + getStartUseTime() + "] pstmtHits[" + pstmtHitCounter + "] pstmtMiss[" + pstmtMissCounter + "] " + pstmtCache.getDescription();
	}

	/**
	 * Return true if the connection should be treated as long running (skip connection pool leak check).
	 */
	public boolean isLongRunning()
	{
		return longRunning;
	}

	/**
	 * Set this to true if the connection is a long running connection and should skip the
	 * 'suspected connection pool leak' checking.
	 */
	public void setLongRunning(boolean longRunning)
	{
		this.longRunning = longRunning;
	}

	/**
	 * Close the connection fully NOT putting in back into the pool.
	 * <p>
	 * The logErrors parameter exists so that expected errors are not logged
	 * such as when the database is known to be down.
	 * </p>
	 *
	 * @param logErrors if false then don't log errors when closing
	 */
	public void closeConnectionFully(boolean logErrors)
	{
		pool.removeConnection(decoratedConnection);

		String msg = "Closing Connection[" + getName() + "]" + " psReuse[" + pstmtHitCounter
			+ "] psCreate[" + pstmtMissCounter + "] psSize[" + pstmtCache.size() + "]";

		logger.info(msg);

		try
		{
			if (connection.isClosed())
			{
				msg = "Closing Connection[" + getName() + "] that is already closed?";
				logger.log(Level.SEVERE, msg);
				return;
			}
		}
		catch (SQLException ex)
		{
			if (logErrors)
			{
				msg = "Error when fully closing connection [" + getName() + "]";
				logger.log(Level.SEVERE, msg, ex);
			}
		}

		try
		{
			Iterator<ExtendedPreparedStatement> psi = pstmtCache.values().iterator();
			while (psi.hasNext())
			{
				ExtendedPreparedStatement ps = (ExtendedPreparedStatement) psi.next();
				ps.closeDestroy();
			}

		}
		catch (SQLException ex)
		{
			if (logErrors)
			{
				logger.log(Level.WARNING, "Error when closing connection Statements", ex);
			}
		}

		try
		{
			connection.close();

		}
		catch (SQLException ex)
		{
			if (logErrors)
			{
				msg = "Error when fully closing connection [" + getName() + "]";
				logger.log(Level.SEVERE, msg, ex);
			}
		}
	}

	/**
	 * A Least Recently used cache of PreparedStatements.
	 */
	public PstmtCache getPstmtCache()
	{
		return pstmtCache;
	}

	/**
	 * Creates a wrapper ExtendedStatement so that I can get the executed sql. I
	 * want to do this so that I can get the slowest query statments etc, and
	 * log that information.
	 */
	public Statement createStatement() throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "createStatement()");
		}
		try
		{
			return connection.createStatement();
		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	public Statement createStatement(int resultSetType, int resultSetConcurreny)
		throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "createStatement()");
		}
		try
		{
			return connection.createStatement(resultSetType, resultSetConcurreny);

		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	/**
	 * Return a PreparedStatement back into the cache.
	 */
	public void returnPreparedStatement(ExtendedPreparedStatement pstmt)
	{

		synchronized (pstmtMonitor)
		{
			ExtendedPreparedStatement alreadyInCache = pstmtCache.get(pstmt.getCacheKey());

			if (alreadyInCache == null)
			{
				// add the returning prepared statement to the cache.
				// Note that the LRUCache will automatically close fully old unused
				// PStmts when the cache has hit its maximum size.
				pstmtCache.put(pstmt.getCacheKey(), pstmt);

			}
			else
			{
				try
				{
					// if a entry in the cache exists for the exact same SQL...
					// then remove it from the cache and close it fully.
					// Only having one PreparedStatement per unique SQL
					// statement
					pstmt.closeDestroy();

				}
				catch (SQLException e)
				{
					logger.log(Level.SEVERE, "Error closing Pstmt", e);
				}
			}
		}
	}

	/**
	 * This will try to use a cache of PreparedStatements.
	 */
	public PreparedStatement prepareStatement(String sql, int returnKeysFlag) throws SQLException
	{
		String cacheKey = sql + returnKeysFlag;
		return prepareStatement(sql, true, returnKeysFlag, cacheKey);
	}

	/**
	 * This will try to use a cache of PreparedStatements.
	 */
	public PreparedStatement prepareStatement(String sql) throws SQLException
	{
		return prepareStatement(sql, false, 0, sql);
	}

	/**
	 * This will try to use a cache of PreparedStatements.
	 */
	private PreparedStatement prepareStatement(String sql, boolean useFlag, int flag, String cacheKey) throws SQLException
	{

		if (status == STATUS_IDLE)
		{
			String m = IDLE_CONNECTION_ACCESSED_ERROR + "prepareStatement()";
			throw new SQLException(m);
		}
		try
		{
			synchronized (pstmtMonitor)
			{
				lastStatement = sql;

				// try to get a matching cached PStmt from the cache.
				PreparedStatement pstmt = pstmtCache.remove(cacheKey);

				if (pstmt != null)
				{
					pstmtHitCounter++;
					return pstmt;
				}

				// create a new PreparedStatement
				pstmtMissCounter++;
				PreparedStatement actualPstmt;
				if (useFlag)
				{
					actualPstmt = connection.prepareStatement(sql, flag);
				}
				else
				{
					actualPstmt = connection.prepareStatement(sql);
				}

				PreparedStatementDecorator decorator = new PreparedStatementDecorator(decoratedConnection, actualPstmt, sql, cacheKey);
				return decorator.buildDecorator();
			}

		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurreny)
		throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "prepareStatement()");
		}
		try
		{
			// no caching when creating PreparedStatements this way
			pstmtMissCounter++;
			lastStatement = sql;
			return connection.prepareStatement(sql, resultSetType, resultSetConcurreny);
		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	/**
	 * Reset the connection for returning to the client. Resets the status,
	 * startUseTime and hadErrors.
	 */
	public void resetForUse()
	{
		this.status = STATUS_ACTIVE;
		this.startUseTime = System.currentTimeMillis();
		this.createdByMethod = null;
		this.lastStatement = null;
		this.hadErrors = false;
		this.longRunning = false;
	}

	/**
	 * When an error occurs during use add it the connection.
	 * <p>
	 * Any PooledConnection that has an error is checked to make sure it works
	 * before it is placed back into the connection pool.
	 * </p>
	 */
	public void addError(Throwable e)
	{
		hadErrors = true;
	}

	/**
	 * Returns true if the connect threw any errors during use.
	 * <p>
	 * Connections with errors are testing to make sure they are still good
	 * before putting them back into the pool.
	 * </p>
	 */
	public boolean hadErrors()
	{
		return hadErrors;
	}

	/**
	 * close the connection putting it back into the connection pool.
	 * <p>
	 * Note that to ensure that the next transaction starts at the correct time
	 * a commit() or rollback() should be called. If neither has occured at this
	 * time then a rollback() is used (to end the transaction).
	 * </p>
	 * <p>
	 * To close the connection fully use closeConnectionFully().
	 * </p>
	 */
	public void close() throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "close()");
		}

		if (hadErrors)
		{
			if (!pool.validateConnection(decoratedConnection))
			{
				// the connection is BAD, close it and test the pool
				closeConnectionFully(false);
				pool.checkDataSource();
				return;
			}
		}

		try
		{
			// reset the autoCommit back if client code changed it
			if (connection.getAutoCommit() != pool.getAutoCommit())
			{
				connection.setAutoCommit(pool.getAutoCommit());
			}
			// Generally resetting Isolation level seems expensive.
			// Hence using resetIsolationReadOnlyRequired flag
			// performance reasons.
			if (resetIsolationReadOnlyRequired)
			{
				resetIsolationReadOnly();
				resetIsolationReadOnlyRequired = false;
			}

			// the connection is assumed GOOD so put it back in the pool
			lastUseTime = System.currentTimeMillis();
			// connection.clearWarnings();
			status = STATUS_IDLE;
			pool.returnConnection(decoratedConnection);

		}
		catch (Exception ex)
		{
			// the connection is BAD, close it and test the pool
			closeConnectionFully(false);
			pool.checkDataSource();
		}
	}

	private void resetIsolationReadOnly() throws SQLException
	{
		// reset the transaction isolation if the client code changed it
		if (connection.getTransactionIsolation() != pool.getTransactionIsolation())
		{
			connection.setTransactionIsolation(pool.getTransactionIsolation());
		}
		// reset readonly to false
		if (connection.isReadOnly())
		{
			connection.setReadOnly(false);
		}
	}

	protected void finalize() throws Throwable
	{
		try
		{
			if (connection != null && !connection.isClosed())
			{
				// connect leak?
				String msg = "Closing Connection[" + getName() + "] on finalize().";
				logger.warning(msg);
				closeConnectionFully(false);
			}
		}
		catch (Exception e)
		{
			logger.log(Level.SEVERE, null, e);
		}
		super.finalize();
	}

	/**
	 * Return the time the connection was passed to the client code.
	 * <p>
	 * Used to detect busy connections that could be leaks.
	 * </p>
	 */
	public long getStartUseTime()
	{
		return startUseTime;
	}

	/**
	 * Returns the time the connection was last used.
	 * <p>
	 * Used to close connections that have been idle for some time. Typically 5
	 * minutes.
	 * </p>
	 */
	public long getLastUsedTime()
	{
		return lastUseTime;
	}

	/**
	 * Returns the last sql statement executed.
	 */
	public String getLastStatement()
	{
		return lastStatement;
	}

	/**
	 * Called by ExtendedStatement to trace the sql being executed.
	 * <p>
	 * Note with addBatch() this will not really work.
	 * </p>
	 */
	public void setLastStatement(String lastStatement)
	{
		this.lastStatement = lastStatement;
		if (logger.isLoggable(Level.FINER))
		{
			logger.finer(".setLastStatement[" + lastStatement + "]");
		}
	}

	boolean resetIsolationReadOnlyRequired = false;

	/**
	 * Also note the read only status needs to be reset when put back into the
	 * pool.
	 */
	public void setReadOnly(boolean readOnly) throws SQLException
	{
		// A bit loose not checking for STATUS_IDLE
		// if (status == STATUS_IDLE) {
		// throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR +
		// "setReadOnly()");
		// }
		resetIsolationReadOnlyRequired = true;
		connection.setReadOnly(readOnly);
	}

	/**
	 * Also note the Isolation level needs to be reset when put back into the
	 * pool.
	 */
	public void setTransactionIsolation(int level) throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "setTransactionIsolation()");
		}
		try
		{
			resetIsolationReadOnlyRequired = true;
			connection.setTransactionIsolation(level);
		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	public void commit() throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "commit()");
		}
		try
		{
			status = STATUS_ENDED;
			connection.commit();
		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	public void rollback() throws SQLException
	{
		if (status == STATUS_IDLE)
		{
			throw new SQLException(IDLE_CONNECTION_ACCESSED_ERROR + "rollback()");
		}
		try
		{
			status = STATUS_ENDED;
			connection.rollback();
		}
		catch (SQLException ex)
		{
			addError(ex);
			throw ex;
		}
	}

	/**
	 * Returns the method that created the connection.
	 * <p>
	 * Used to help finding connection pool leaks.
	 * </p>
	 */
	public String getCreatedByMethod()
	{
		if (createdByMethod != null)
		{
			return createdByMethod;
		}
		if (stackTrace == null)
		{
			return null;
		}

		for (int j = 0; j < stackTrace.length; j++)
		{
			String methodLine = stackTrace[j].toString();
			if (skipElement(methodLine))
			{
				// ignore these methods...
			}
			else
			{
				createdByMethod = methodLine;
				return createdByMethod;
			}
		}

		return null;
	}

	private boolean skipElement(String methodLine)
	{
		if (methodLine.startsWith("java.lang."))
		{
			return true;
		}
		else if (methodLine.startsWith("java.util."))
		{
			return true;
		}
		else if (methodLine.startsWith("com.avaje.ebeaninternal.server.query.CallableQuery.<init>"))
		{
			// creating connection on future...
			return true;
		}
		else if (methodLine.startsWith("com.avaje.ebeaninternal.server.query.Callable"))
		{
			// it is a future task being executed...
			return false;
		}
		else if (methodLine.startsWith("com.avaje.ebeaninternal"))
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Set the stack trace to help find connection pool leaks.
	 */
	public void setStackTrace(StackTraceElement[] stackTrace)
	{
		this.stackTrace = stackTrace;
	}

	/**
	 * Return the full stack trace that got the connection from the pool. You
	 * could use this if getCreatedByMethod() doesn't work for you.
	 */
	public StackTraceElement[] getStackTrace()
	{

		if (stackTrace == null)
		{
			return null;
		}

		// filter off the top of the stack that we are not interested in
		ArrayList<StackTraceElement> filteredList = new ArrayList<StackTraceElement>();
		boolean include = false;
		for (int i = 0; i < stackTrace.length; i++)
		{
			if (!include && !skipElement(stackTrace[i].toString()))
			{
				include = true;
			}
			if (include && filteredList.size() < maxStackTrace)
			{
				filteredList.add(stackTrace[i]);
			}
		}
		return filteredList.toArray(new StackTraceElement[filteredList.size()]);
	}
}