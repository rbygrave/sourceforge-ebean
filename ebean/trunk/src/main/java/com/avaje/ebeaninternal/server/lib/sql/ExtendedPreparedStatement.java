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

/**
 * Extended PreparedStatement that supports caching.
 * <p>
 * Designed so that it can be cached by the PooledConnection. It additionally
 * notes any Exceptions that occur and this is used to ensure bad connections
 * are removed from the connection pool.
 * </p>
 * Ebean uses this interface to access the PreparedStatement proxy to access either the default
 * PreparedStatement methods and also the ExtendedPreparedStatementMethods. 
 */
public interface ExtendedPreparedStatement extends PreparedStatement, ExtendedPreparedStatementMethods {
}
