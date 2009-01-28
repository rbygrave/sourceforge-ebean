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
package org.avaje.ebean.server.core;

import java.sql.SQLException;

import org.avaje.ebean.SqlUpdate;
import org.avaje.ebean.server.persist.PersistExecute;

/**
 * Persist request specifically for CallableSql.
 */
public final class PersistRequestUpdateSql extends PersistRequest {

	public enum SqlType {
		SQL_UPDATE, SQL_DELETE, SQL_INSERT, SQL_UNKNOWN
	};

	SqlUpdate updateSql;

	int rowCount;

	String bindLog;

	SqlType sqlType;

	String tableName;

	String description;

	/**
	 * Create.
	 */
	public PersistRequestUpdateSql(InternalEbeanServer server, SqlUpdate updateSql,
			ServerTransaction t, PersistExecute persistExecute) {
		super(server, t, persistExecute);
		this.type = Type.UPDATESQL;
		this.updateSql = updateSql;
	}

	@Override
	public int executeNow() {
		return persistExecute.executeSqlUpdate(this);
	}

	@Override
	public int executeOrQueue() {
		return executeStatement();
	}

	/**
	 * Return the UpdateSql.
	 */
	public SqlUpdate getUpdateSql() {
		return updateSql;
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
	 * Specify the type of statement executed. Used to automatically register
	 * with the transaction event.
	 */
	public void setType(SqlType sqlType, String tableName, String description) {
		this.sqlType = sqlType;
		this.tableName = tableName;
		this.description = description;
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

		if (transaction.isLoggingOn()) {

			String label = updateSql.getLabel();

			String m = description + " label[" + label + "] table[" + tableName + "] rows["
					+ rowCount + "] bind[" + bindLog + "]";

			// log the summary of the sql to the transaction log
			transaction.log(m);

		}

		if (updateSql.isAutoTableMod()) {
			// add the modification info to the TransactionEvent
			// this is used to invalidate cached objects etc
			switch (sqlType) {
			case SQL_INSERT:
				transaction.getEvent().addInsert(tableName);
				break;
			case SQL_UPDATE:
				transaction.getEvent().addUpdate(tableName);
				break;
			case SQL_DELETE:
				transaction.getEvent().addDelete(tableName);
				break;
			default:
				break;
			}
		}
	}

}
