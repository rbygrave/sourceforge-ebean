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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;

class DelegateToDecoratedMethodHandler implements MethodHandler
{
	private final Method method;

	public DelegateToDecoratedMethodHandler(Method method)
	{
		this.method = method;
	}

	public Object invoke(Object decorator, Object proxy, Method method, Object delegate, Object... args) throws Exception
	{
		try
		{
			return this.method.invoke(decorator, args);
		}
		catch (InvocationTargetException e)
		{
			Throwable er = e.getTargetException();
			if (er instanceof SQLException)
			{
				throw (SQLException) er;
			}

			throw e;
		}
	}
}