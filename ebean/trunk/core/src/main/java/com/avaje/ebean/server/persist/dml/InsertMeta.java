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
package com.avaje.ebean.server.persist.dml;

import java.sql.SQLException;
import java.util.Set;

import com.avaje.ebean.config.dbplatform.DatabasePlatform;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.persist.dmlbind.Bindable;
import com.avaje.ebean.server.persist.dmlbind.BindableDiscriminator;
import com.avaje.ebean.server.persist.dmlbind.BindableId;

/**
 * Meta data for insert handler. The meta data is for a particular bean type. It
 * is considered immutable and is thread safe.
 */
public final class InsertMeta {

	private final String sqlNullId;

	private final String sqlWithId;

	private final BindableId id;

	private final Bindable discriminator;

	private final Bindable all;

	private final boolean supportsGetGeneratedKeys;

	private final boolean concatinatedKey;

	private final String tableName;

	/**
	 * Used for DB that do not support getGeneratedKeys.
	 */
	private final String selectLastInsertedId;

	private final Bindable shadowFKey;
	
	private final String[] identityDbColumns;
	
	public InsertMeta(DatabasePlatform dbPlatform, BeanDescriptor<?> desc, Bindable shadowFKey, BindableId id, Bindable all) {

		this.tableName = desc.getBaseTable();
		this.discriminator = getDiscriminator(desc);
		this.id = id;
		this.all = all;
		this.shadowFKey = shadowFKey;

		this.sqlWithId = genSql(false, null);

		// only available for single Id property
		if (id.isConcatenated()) {
			// concatenated key
			this.concatinatedKey = true;
			this.identityDbColumns = null;
			this.sqlNullId = null;
			this.supportsGetGeneratedKeys = false;
			this.selectLastInsertedId = null;

		} else {
			// insert sql for db identity or sequence insert
			this.concatinatedKey = false;
			this.identityDbColumns = new String[]{id.getIdentityColumn()};
			this.sqlNullId = genSql(true, null);
			this.supportsGetGeneratedKeys = dbPlatform.getDbIdentity().isSupportsGetGeneratedKeys();
			this.selectLastInsertedId = desc.getSelectLastInsertedId();
		}
	}

	private static Bindable getDiscriminator(BeanDescriptor<?> desc){
		InheritInfo inheritInfo = desc.getInheritInfo();
		if (inheritInfo != null){
			return new BindableDiscriminator(inheritInfo);
		} else {
			return null;
		}
	}
	
	/**
	 * Return true if this is a concatenated key.
	 */
	public boolean isConcatinatedKey() {
		return concatinatedKey;
	}

	public String[] getIdentityDbColumns() {
		return identityDbColumns;
	}

	/**
	 * Returns sql that is used to fetch back the last inserted id. This will
	 * return null if it should not be used.
	 * <p>
	 * This is only for DB's that do not support getGeneratedKeys. For MS
	 * SQLServer 2000 this could return "SELECT (at)(at)IDENTITY as id".
	 * </p>
	 */
	public String getSelectLastInsertedId() {
		return selectLastInsertedId;
	}

	/**
	 * Return true if getGeneratedKeys is supported by the underlying jdbc
	 * driver and database.
	 */
	public boolean supportsGetGeneratedKeys() {
		return supportsGetGeneratedKeys;
	}

	/**
	 * Return true if the Id can be derived from other property values.
	 */
	public boolean deriveConcatenatedId(PersistRequestBean<?> persist) {
		return id.deriveConcatenatedId(persist);
	}

	/**
	 * Bind the request based on whether the id value(s) are null.
	 */
	public void bind(DmlHandler request, Object bean, boolean withId) throws SQLException {

		if (withId) {
			id.dmlBind(request, false, bean, true);
		}
		if (shadowFKey != null){
			shadowFKey.dmlBind(request, false, bean, true);
		}
		if (discriminator != null){
			discriminator.dmlBind(request, false, bean, true);			
		}
		all.dmlBind(request, false, bean, true);
	}

	/**
	 * get the sql based whether the id value(s) are null.
	 */
	public String getSql(boolean withId) {

		if (withId) {
			return sqlWithId;
		} else {
			return sqlNullId;
		}
	}

	private String genSql(boolean nullId, Set<String> loadedProps) {

		GenerateDmlRequest request = new GenerateDmlRequest(loadedProps, null);
		request.setInsertSetMode();
		
		request.append("insert into ").append(tableName);
		request.append(" (");

		if (!nullId) {
			id.dmlAppend(request, false);
		} 
		
		if (shadowFKey != null){
			shadowFKey.dmlAppend(request, false);
		}
		
		if (discriminator != null){
			discriminator.dmlAppend(request, false);			
		}
		
		all.dmlAppend(request, false);

		request.append(") values (");

		// ensure prefixes are reset for the value list
		request.setInsertSetMode();
				
		// the number of scalar properties being bound
		int bindCount = request.getBindCount();

		for (int i = 0; i < bindCount; i++) {
			if (i > 0) {
				request.append(", ");
			}
			request.append("?");
		}
		request.append(")");

		return request.toString();
	}

}
