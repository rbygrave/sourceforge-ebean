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

import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.persist.dmlbind.Bindable;
import com.avaje.ebean.server.persist.dmlbind.BindableDiscriminator;
import com.avaje.ebean.server.persist.dmlbind.BindableId;
import com.avaje.ebean.server.plugin.PluginDbConfig;

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

	private final String sequenceNextVal;

	private final boolean supportsGetGeneratedKeys;

	private final boolean concatinatedKey;

	private final String tableName;

	/**
	 * Used for DB that do not support getGeneratedKeys.
	 */
	private final String selectLastInsertedId;

	private final Bindable shadowFKey;
	
	public InsertMeta(PluginDbConfig dbConfig, BeanDescriptor desc, Bindable shadowFKey, BindableId id, Bindable all) {

		this.sequenceNextVal = desc.getSequenceNextVal();
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
			this.sqlNullId = null;
			this.supportsGetGeneratedKeys = false;
			this.selectLastInsertedId = null;

		} else {
			// insert sql for db identity or sequence insert
			this.concatinatedKey = false;
			this.sqlNullId = genSql(true, null);
			this.supportsGetGeneratedKeys = dbConfig.isSupportsGetGeneratedKeys();
			this.selectLastInsertedId = desc.getSelectLastInsertedId();
		}
	}

	private static Bindable getDiscriminator(BeanDescriptor desc){
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

	public boolean deriveConcatenatedId(PersistRequest persist) {
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

	/**
	 * For DB Sequence add the db column. For Identity leave the column out all
	 * together.
	 */
	protected void addNullUidColumn(GenerateDmlRequest request) {
		// Using Identity or Sequence
		if (sequenceNextVal != null) {
			id.dmlAppend(request, false);
		}
	}

	/**
	 * For DB Sequence add the sequence name. For Identity leave the column out
	 * all together.
	 */
	protected void addNullUidValue(GenerateDmlRequest request) {
		// Using Identity or Sequence
		if (sequenceNextVal != null) {
			request.appendRaw(sequenceNextVal);
		}
	}

	private String genSql(boolean nullId, Set<String> loadedProps) {

		GenerateDmlRequest request = new GenerateDmlRequest(loadedProps);
		request.setInsertSetMode();
		
		request.append("insert into ").append(tableName);
		request.append(" (");

		if (nullId) {
			addNullUidColumn(request);
		} else {
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

		if (nullId) {
			addNullUidValue(request);
		}

		int bindStart = (!nullId || sequenceNextVal==null) ? 0 : 1;
		
		for (int i = bindStart; i < bindCount; i++) {
			if (i > 0) {
				request.append(", ");
			}
			request.append("?");
		}
		request.append(")");

		return request.toString();
	}

}
