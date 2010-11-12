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
package com.avaje.ebeaninternal.server.persist.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceException;

import com.avaje.ebeaninternal.api.SpiTransaction;
import com.avaje.ebeaninternal.server.core.Message;
import com.avaje.ebeaninternal.server.core.PersistRequestBean;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;
import com.avaje.ebeaninternal.server.persist.DmlUtil;
import com.avaje.ebeaninternal.server.type.DataBind;

/**
 * Insert bean handler.
 */
public class InsertHandler extends DmlHandler {

	/**
	 * The associated InsertMeta data.
	 */
	private final InsertMeta meta;

	/**
	 * Set to true when the key is concatenated.
	 */
	private final boolean concatinatedKey;

	/**
	 * Flag set when using getGeneratedKeys.
	 */
	private boolean useGeneratedKeys;

	/**
	 * A SQL Select used to fetch back the Id where generatedKeys is not
	 * supported.
	 */
	private String selectLastInsertedId;
	    
	/**
	 * Create to handle the insert execution.
	 */
	public InsertHandler(PersistRequestBean<?> persist, InsertMeta meta) {
		super(persist, meta.isEmptyStringToNull());
		this.meta = meta;
		this.concatinatedKey = meta.isConcatinatedKey();
	}
	
	
    @Override
    public boolean isIncluded(BeanProperty prop) {
        return prop.isDbInsertable() && (super.isIncluded(prop));
    }
	
	/**
	 * Generate and bind the insert statement.
	 */
	public void bind() throws SQLException {

		BeanDescriptor<?> desc = persistRequest.getBeanDescriptor();
		Object bean = persistRequest.getBean();

		Object idValue = desc.getId(bean);

		boolean withId = !DmlUtil.isNullOrZero(idValue);

		// check to see if we are going to use generated keys
		if (!withId) {
			if (concatinatedKey) {
				// expecting a concatenated key that can
				// be built from supplied AssocOne beans
				withId = meta.deriveConcatenatedId(persistRequest);

			} else if (meta.supportsGetGeneratedKeys()) {
				// Identity with getGeneratedKeys
				useGeneratedKeys = true;
			} else {
				// use a query to get the last inserted id
				selectLastInsertedId = meta.getSelectLastInsertedId();
			}
		}
		
		SpiTransaction t = persistRequest.getTransaction();
		boolean isBatch = t.isBatchThisRequest();

		// get the appropriate sql
		sql = meta.getSql(withId);

		PreparedStatement pstmt;
		if (isBatch) {
			pstmt = getPstmt(t, sql, persistRequest, useGeneratedKeys);

		} else {
			logSql(sql);
			pstmt = getPstmt(t, sql, useGeneratedKeys);
		}
		dataBind = new DataBind(pstmt);

		bindLogAppend("Binding Insert [");
		bindLogAppend(desc.getBaseTable());
		bindLogAppend("]  set[");

		// bind the bean property values
		meta.bind(this, bean, withId);

		bindLogAppend("]");
		logBinding();
	}

	/**
	 * Check with useGeneratedKeys to get appropriate PreparedStatement.
	 */
	@Override
	protected PreparedStatement getPstmt(SpiTransaction t, String sql, boolean useGeneratedKeys) throws SQLException {
		Connection conn = t.getInternalConnection();
		if (useGeneratedKeys) {
			return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

		} else {
			return conn.prepareStatement(sql);
		}
	}

	/**
	 * Execute the insert in a normal non batch fashion. Additionally using
	 * getGeneratedKeys if required.
	 */
	public void execute() throws SQLException, OptimisticLockException {
		int rc = dataBind.executeUpdate();
		if (useGeneratedKeys) {
			// get the auto-increment value back and set into the bean
			getGeneratedKeys();

		} else if (selectLastInsertedId != null) {
			// fetch back the Id using a query
			fetchGeneratedKeyUsingSelect();
		}

		checkRowCount(rc);
		setAdditionalProperties();
	}
	
	/**
	 * For non batch insert with generated keys.
	 */
	private void getGeneratedKeys() throws SQLException {

		ResultSet rset = dataBind.getPstmt().getGeneratedKeys();
		try {
			if (rset.next()) {
				Object idValue = rset.getObject(1);
				if (idValue != null) {
					persistRequest.setGeneratedKey(idValue);
				}

			} else {
				throw new PersistenceException(Message.msg("persist.autoinc.norows"));
			}
		} finally {
			try {
				rset.close();
			} catch (SQLException ex) {
				String msg = "Error closing rset for returning generatedKeys?";
				logger.log(Level.WARNING, msg, ex);
			}
		}
	}

	/**
	 * For non batch insert with DBs that do not support getGeneratedKeys.
	 * Use a SQL select to fetch back the Id value.
	 */
	private void fetchGeneratedKeyUsingSelect() throws SQLException {

		Connection conn = transaction.getConnection();

		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			stmt = conn.prepareStatement(selectLastInsertedId);
			rset = stmt.executeQuery();
			if (rset.next()) {
				Object idValue = rset.getObject(1);
				if (idValue != null) {
					persistRequest.setGeneratedKey(idValue);
				}
			} else {
				throw new PersistenceException(Message.msg("persist.autoinc.norows"));
			}
		} finally {
			try {
				if (rset != null) {
					rset.close();
				}
			} catch (SQLException ex) {
				String msg = "Error closing rset for fetchGeneratedKeyUsingSelect?";
				logger.log(Level.WARNING, msg, ex);
			}
			try {
				if (stmt != null) {
					stmt.close();
				}
			} catch (SQLException ex) {
				String msg = "Error closing stmt for fetchGeneratedKeyUsingSelect?";
				logger.log(Level.WARNING, msg, ex);
			}
		}
	}
}
