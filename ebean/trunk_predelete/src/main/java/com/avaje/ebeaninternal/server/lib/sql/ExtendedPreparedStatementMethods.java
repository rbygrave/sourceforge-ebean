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

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Methods for the ExtendedPreparedStatement.
 *
 * This interfaced is used to compose the ExtendedPreparedStatement and for the decorator to easily cache
 * these specific methods and fast {@link MethodHandler} lookup.
 */
public interface ExtendedPreparedStatementMethods {

	public PreparedStatement getDelegate();

	/**
	 * Return the key used to cache this on the Connection.
	 */
	public String getCacheKey();

	/**
	 * Return the SQL used to create this PreparedStatement.
	 */
	public String getSql();

	/**
	 * Fully close the underlying PreparedStatement. After this we can no longer
	 * reuse the PreparedStatement.
	 */
	public void closeDestroy() throws SQLException;
}