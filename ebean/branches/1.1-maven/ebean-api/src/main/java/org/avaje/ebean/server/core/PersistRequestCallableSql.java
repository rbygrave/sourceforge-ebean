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

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;

import org.avaje.ebean.CallableSql;
import org.avaje.ebean.server.persist.PersistExecute;
import org.avaje.ebean.server.transaction.TransactionEvent;
import org.avaje.ebean.util.BindParams;
import org.avaje.ebean.util.BindParams.Param;

/**
 * Persist request specifically for CallableSql.
 */
public final class PersistRequestCallableSql extends PersistRequest {

	CallableSql callableSql;

	int rowCount;

	String bindLog;

	CallableStatement cstmt;

	BindParams bindParam;

	/**
	 * Create.
	 */
	public PersistRequestCallableSql(InternalEbeanServer server,
			CallableSql cs, ServerTransaction t, PersistExecute persistExecute) {
		
		super(server, t, persistExecute);
		this.type = PersistRequest.Type.CALLABLESQL;
		this.callableSql = cs;
	}
	
	@Override
	public int executeOrQueue() {
		return executeStatement();
	}

	@Override
	public int executeNow() {
		return persistExecute.executeSqlCallable(this);
	}

	/**
	 * Return the CallableSql.
	 */
	public CallableSql getCallableSql() {
		return callableSql;
	}

	/**
	 * The the log of bind values.
	 */
	public void setBindLog(String bindLog) {
		this.bindLog = bindLog;
	}

	/**
	 * Note the rowCount of the execution.
	 */
	public void checkRowCount(int count) throws SQLException {
		this.rowCount = count;
	}

	/**
	 * Only called for insert with generated keys.
	 */
	public void setGeneratedKey(Object idValue) {
	}

	/**
	 * False for CallableSql.
	 */
	public boolean useGeneratedKeys() {
		return false;
	}

	/**
	 * Perform post execute processing for the CallableSql.
	 */
	public void postExecute() throws SQLException {

		if (transaction.isLoggingOn()) {

			String label = callableSql.getLabel();

			String m = "CallableSql label[" + label + "]" + " rows[" + rowCount
					+ "]" + " bind[" + bindLog + "]";

			// log the summary of the CallableSql
			transaction.log(m);
		}

		// register table modifications with the transaction event
		TransactionEvent cEvent = ProtectedMethod
				.getTransactionEvent(callableSql);
		if (cEvent != null && cEvent.hasModifications()) {
			TransactionEvent transEvent = transaction.getEvent();
			transEvent.add(cEvent);
		}

	}

	/**
	 * These need to be set for use with Non-batch execution. Specifically to
	 * read registered out parameters and potentially handle the
	 * executeOverride() method.
	 */
	public void setBound(BindParams bindParam, CallableStatement cstmt) {
		this.bindParam = bindParam;
		this.cstmt = cstmt;
	}

	/**
	 * Execute the statement in normal non batch mode.
	 */
	public int executeUpdate() throws SQLException {

		// check to see if the execution has been overridden
		// only works in non-batch mode
		if (callableSql.executeOverride(cstmt)) {
			return -1;
			// // been overridden so just return the rowCount
			// rowCount = callableSql.getRowCount();
			// return rowCount;
		}

		rowCount = cstmt.executeUpdate();

		// only read in non-batch mode
		readOutParams();

		return rowCount;
	}

	private void readOutParams() throws SQLException {

		List<Param> list = bindParam.positionedParameters();
		int pos = 0;

		for (int i = 0; i < list.size(); i++) {
			pos++;
			BindParams.Param param = (BindParams.Param) list.get(i);
			if (param.isOutParam()) {
				Object outValue = cstmt.getObject(pos);
				param.setOutValue(outValue);
			}
		}
	}

}
