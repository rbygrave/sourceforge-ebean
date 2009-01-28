/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package org.avaje.ebean.server.persist;

import org.avaje.ebean.server.core.PersistRequest;
import org.avaje.ebean.server.core.PersistRequestCallableSql;
import org.avaje.ebean.server.core.PersistRequestOrmUpdate;
import org.avaje.ebean.server.core.PersistRequestUpdateSql;
import org.avaje.ebean.server.core.ServerTransaction;

/**
 * The actual execution of persist requests.
 * <p>
 * A Persister 'front-ends' this object and handles the
 * batching, cascading, concurrency mode detection etc.
 * </p>
 *
 */
public interface PersistExecute {
	
	/**
	 * Create a BatchControl for the current transaction.
	 */
	public BatchControl createBatchControl(ServerTransaction t);
	
	/**
	 * Execute a Bean (or MapBean) insert.
	 */
	public void executeInsertBean(PersistRequest request);

	/**
	 * Execute a Bean (or MapBean) update.
	 */
	public void executeUpdateBean(PersistRequest request);

	/**
	 * Execute a Bean (or MapBean) delete.
	 */
	public void executeDeleteBean(PersistRequest request);

	/**
	 * Execute a Update.
	 */
	public int executeOrmUpdate(PersistRequestOrmUpdate request);
	
	/**
	 * Execute a CallableSql.
	 */
	public int executeSqlCallable(PersistRequestCallableSql request);

	/**
	 * Execute a UpdateSql.
	 */
	public int executeSqlUpdate(PersistRequestUpdateSql request);

}
