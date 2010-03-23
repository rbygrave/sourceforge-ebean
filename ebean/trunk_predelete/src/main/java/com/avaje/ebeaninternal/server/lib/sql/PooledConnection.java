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

import java.sql.Connection;

/**
 * Is a connection that belongs to a DataSourcePool.
 * 
 * <p>
 * It is designed to be part of DataSourcePool. Closing the connection puts it
 * back into the pool.
 * </p>
 * 
 * <p>
 * It defaults autoCommit and Transaction Isolation to the defaults of the
 * DataSourcePool.
 * </p>
 * 
 * <p>
 * It has caching of Statements and PreparedStatements. Remembers the last
 * statement that was executed. Keeps statistics on how long it is in use.
 * </p>
 */
public interface PooledConnection extends Connection, PooledConnectionMethods {
}
