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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * A decorator builder which uses java {@link Proxy} methods to wrap a PreparedStatement which provides
 * additional methods as described in {@link ExtendedPreparedStatement}.
 */
public class PreparedStatementDecorator implements ExtendedPreparedStatementMethods {

	/**
	 * The pooled connection this Statement belongs to.
	 */
	private final PooledConnection pooledConnection;

	/**
	 * The underlying Statement that this object wraps.
	 */
	private final PreparedStatement pstmt;

	/**
	 * The SQL used to create the underlying PreparedStatement.
	 */
	private String sql;

	/**
	 * The key used to cache this in the connection.
	 */
	private String cacheKey;

	private PreparedStatement decoratedStatement;

	private final static Class[] PROXY_INTERFACES = new Class[] {ExtendedPreparedStatement.class};

	protected final static Map<Method, MethodHandler<PreparedStatementDecorator, PreparedStatement>> METHOD_HANDLERS =
		new HashMap<Method, MethodHandler<PreparedStatementDecorator, PreparedStatement>>();

	// create datastructure for fast lookup of overridden methods.
	// since this is as static block the cost for building the structure is only once per JVM start.
	static
	{
		final MethodHandler<PreparedStatementDecorator, PreparedStatement> addErrorHandler = new MethodHandler<PreparedStatementDecorator, PreparedStatement>()
		{
			public Object invoke(PreparedStatementDecorator decorator, Object proxy, Method method, PreparedStatement delegate, Object... args) throws Exception
			{
				try {
					return method.invoke(delegate, args);
				} catch (InvocationTargetException e) {
					return handleInvocationTargetException(decorator, e);
				}
			}
		};

		final MethodHandler delegateToDecoratorHandler = new DelegateToDecoratorHandler();

		final MethodHandler<PreparedStatementDecorator, PreparedStatement> addStatementAndErrorHandler = new MethodHandler<PreparedStatementDecorator, PreparedStatement>()
		{
			public Object invoke(PreparedStatementDecorator decorator, Object proxy, Method method, PreparedStatement delegate, Object... args) throws Exception
			{
				try {
					decorator.pooledConnection.setLastStatement((String) args[0]);
					return method.invoke(delegate, args);
				} catch (InvocationTargetException e) {
					return handleInvocationTargetException(decorator, e);
				}
			}
		};

		final MethodHandler<PreparedStatementDecorator, PreparedStatement> closeHandler = new MethodHandler<PreparedStatementDecorator, PreparedStatement>()
		{
			public Object invoke(PreparedStatementDecorator decorator, Object proxy, Method method, PreparedStatement delegate, Object... args) throws Exception
			{
				decorator.pooledConnection.returnPreparedStatement((ExtendedPreparedStatement) proxy);
				return null;
			}
		};

		try
		{
			METHOD_HANDLERS.put(Object.class.getMethod("hashCode"), delegateToDecoratorHandler);
			METHOD_HANDLERS.put(Object.class.getMethod("equals", Object.class), delegateToDecoratorHandler);
			METHOD_HANDLERS.put(Object.class.getMethod("toString"), delegateToDecoratorHandler);

			METHOD_HANDLERS.put(Statement.class.getMethod("getConnection"), addErrorHandler);
			METHOD_HANDLERS.put(Statement.class.getMethod("addBatch", String.class), addStatementAndErrorHandler);
			METHOD_HANDLERS.put(Statement.class.getMethod("execute", String.class), addStatementAndErrorHandler);
			METHOD_HANDLERS.put(Statement.class.getMethod("executeQuery", String.class), addStatementAndErrorHandler);
			METHOD_HANDLERS.put(Statement.class.getMethod("executeUpdate", String.class), addStatementAndErrorHandler);

			for (Method method : ExtendedPreparedStatementMethods.class.getMethods())
			{
				METHOD_HANDLERS.put(method, delegateToDecoratorHandler);
			}

			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("close"), closeHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("addBatch"), addErrorHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("clearParameters"), addErrorHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("execute"), addErrorHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("executeQuery"), addErrorHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("executeUpdate"), addErrorHandler);
			METHOD_HANDLERS.put(PreparedStatement.class.getMethod("getMetaData"), addErrorHandler);
		}
		catch (NoSuchMethodException e)
		{
			// if this happens something is very wrong
			throw new RuntimeException(e);
		}
	}

	private static Void handleInvocationTargetException(PreparedStatementDecorator decorator, InvocationTargetException e) throws SQLException, InvocationTargetException
	{
		Throwable targetException = e.getTargetException();
		if (targetException instanceof SQLException)
		{
			decorator.pooledConnection.addError(targetException);
			throw (SQLException) targetException;
		}

		throw e;
	}

	/**
	 * Create a wrapped PreparedStatement that can be cached.
	 */
	public PreparedStatementDecorator(PooledConnection pooledConnection, PreparedStatement pstmt,
			String sql, String cacheKey) {

		this.pooledConnection = pooledConnection;
		this.pstmt = pstmt;
		this.sql = sql;
		this.cacheKey = cacheKey;
	}

	/**
	 * create the decorator which looks like a PreparedStatement which also implements the
	 * {@link ExtendedPreparedStatement} interface.<br />
	 * That way we are no longer bound to a specific JDK at compile time.
	 */
	public PreparedStatement buildDecorator()
	{
		decoratedStatement = (PreparedStatement) Proxy.newProxyInstance(PreparedStatementDecorator.class.getClassLoader(),
			PROXY_INTERFACES,
			new InvocationHandler()
			{
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
				{
					MethodHandler<PreparedStatementDecorator, PreparedStatement> handler = METHOD_HANDLERS.get(method);
					if (handler == null)
					{
						return method.invoke(pstmt, args);
					}

					return handler.invoke(PreparedStatementDecorator.this, proxy, method, pstmt, args);
				}
			});

		return decoratedStatement;
	}

	@Override
	public boolean equals(Object obj)
	{
		return decoratedStatement == obj;
	}

	@Override
	public int hashCode()
	{
		return System.identityHashCode(decoratedStatement);
	}

	public PreparedStatement getDelegate() {
		return pstmt;
	}

	/**
	 * Return the key used to cache this on the Connection.
	 */
	public String getCacheKey() {
		return cacheKey;
	}

	/**
	 * Return the SQL used to create this PreparedStatement.
	 */
	public String getSql() {
		return sql;
	}

	/**
	 * Fully close the underlying PreparedStatement. After this we can no longer
	 * reuse the PreparedStatement.
	 */
	public void closeDestroy() throws SQLException {
		pstmt.close();
	}

}