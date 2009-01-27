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
package com.avaje.ebean.server.core;

import java.sql.SQLException;

import com.avaje.ebean.query.OrmUpdate;
import com.avaje.ebean.query.OrmUpdate.OrmUpdateType;
import com.avaje.ebean.server.persist.PersistExecute;

/**
 * Persist request specifically for CallableSql.
 */
public final class PersistRequestOrmUpdate extends PersistRequest {

	
	OrmUpdate<?> ormUpdate;

	int rowCount;

	String bindLog;

	/**
	 * Create.
	 */
	public PersistRequestOrmUpdate(InternalEbeanServer server, OrmUpdate<?> ormUpdate, ServerTransaction t, PersistExecute persistExecute) {
		super(server, t, persistExecute);
		this.ormUpdate = ormUpdate;
	}
	
	@Override
	public int executeNow() {
		return persistExecute.executeOrmUpdate(this);
	}

	@Override
	public int executeOrQueue() {
		return executeStatement();
	}


	/**
	 * Return the UpdateSql.
	 */
	public OrmUpdate<?> getOrmUpdate() {
		return ormUpdate;
	}

	/**
	 * No concurrency checking so just note the rowCount.
	 */
	public void checkRowCount(int count) throws SQLException {
		this.rowCount = count;
	}

	/**
	 * Always false.
	 */
	public boolean useGeneratedKeys() {
		return false;
	}

	/**
	 * Not called for this type of request.
	 */
	public void setGeneratedKey(Object idValue) {
	}

	/**
	 * Set the bound values.
	 */
	public void setBindLog(String bindLog) {
		this.bindLog = bindLog;
	}

	/**
	 * Perform post execute processing.
	 */
	public void postExecute() throws SQLException {

		OrmUpdateType ormUpdateType = ormUpdate.getOrmUpdateType();
		String tableName = ormUpdate.getBaseTable();
		
		
		if (transaction.isLoggingOn()) {

			String name = ormUpdate.getName();

			String m = ormUpdateType + " table[" + tableName + "] name[" + name + "] rows["
					+ rowCount + "] bind[" + bindLog + "]";

			// log the summary of the sql to the transaction log
			transaction.log(m);

		}
		
		if (ormUpdate.isNotifyCache()) {
			
			
			// add the modification info to the TransactionEvent
			// this is used to invalidate cached objects etc
			switch (ormUpdateType) {
			case INSERT:
				transaction.getEvent().addInsert(tableName);
				break;
			case UPDATE:
				transaction.getEvent().addUpdate(tableName);
				break;
			case DELETE:
				transaction.getEvent().addDelete(tableName);
				break;
			default:
				break;
			}
		}
	}

}
