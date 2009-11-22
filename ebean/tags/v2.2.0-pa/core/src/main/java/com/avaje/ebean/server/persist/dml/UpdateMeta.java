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

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.persist.dmlbind.Bindable;

/**
 * Meta data for update handler. The meta data is for a particular bean type. It
 * is considered immutable and is thread safe.
 */
public final class UpdateMeta {

	
	private final String sqlVersion;
	
	private final String sqlNone;
	
	private final Bindable set;
	private final Bindable id;
	private final Bindable version;
	private final Bindable all;
	
	private final String tableName;
	
	public UpdateMeta(BeanDescriptor<?> desc, Bindable set, Bindable id, Bindable version, Bindable all) {
		this.tableName = desc.getBaseTable();
		this.set = set;
		this.id = id;
		this.version = version;
		this.all = all;
		
		sqlNone = genSql(ConcurrencyMode.NONE, null);
		sqlVersion = genSql(ConcurrencyMode.VERSION, null);
	}
	
	/**
	 * Return the base table name.
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Bind the request based on the concurrency mode.
	 */
	public void bind(PersistRequestBean<?> persist, DmlHandler bind) throws SQLException {

		Object bean = persist.getBean();
		
		bind.bindLogAppend(" set[");
		set.dmlBind(bind, true, bean, true);

		bind.bindLogAppend("] where[");
		id.dmlBind(bind, false, bean, true);

		switch (persist.getConcurrencyMode()) {
		case VERSION:
			version.dmlBind(bind, false, bean, true);			
			break;
		case ALL:
			Object oldBean = persist.getOldValues();
			all.dmlBind(bind, true, oldBean, false);
			break;

		default:
			break;
		}
	}
	
	/**
	 * get or generate the sql based on the concurrency mode.
	 */
	public String getSql(PersistRequestBean<?> request) {

		ConcurrencyMode mode = request.determineConcurrencyMode();
		if (request.isDynamicUpdateSql()){
			return genSql(mode, request);
		}
		
		// 'full bean' update...
		switch (mode) {
		case NONE:
			return sqlNone;
			
		case VERSION:
			return sqlVersion;
			
		case ALL:
			Object oldValues = request.getOldValues();
			if (oldValues == null) {
				throw new PersistenceException("OldValues are null?");
			}
			return genDynamicWhere(request.getLoadedProperties(), oldValues);

		default:
			throw new RuntimeException("Invalid mode "+mode);
		}
	}
	
	private String genSql(ConcurrencyMode conMode, PersistRequestBean<?> persistRequest) {

		// update  set col0=?, col1=?, col2=? where bcol=? and bc1=? and bc2=?

		GenerateDmlRequest request;
		if (persistRequest == null){
			// For generation of None and Version DML/SQL
			request = new GenerateDmlRequest();
		} else {
			if (persistRequest.isUpdateChangesOnly()){
				set.determineChangedProperties(persistRequest);
			}
			request = persistRequest.createGenerateDmlRequest();
		}
				
		request.append("update ").append(tableName).append(" set ");
		
		request.setUpdateSetMode();
		set.dmlAppend(request, true);
		
		request.append(" where ");
		
		request.setWhereIdMode();
		id.dmlAppend(request, false);
		
		if (ConcurrencyMode.VERSION.equals(conMode)) {
			if (version == null){
				return null;
			}
			//request.setWhereMode();
			version.dmlAppend(request, false);
			
		} else if (ConcurrencyMode.ALL.equals(conMode)) {
			
			//request.setWhereMode();
			all.dmlWhere(request, true, request.getOldValues());
		}
		
		return request.toString();
	}
	
	
	/**
	 * Generate the sql dynamically for where using IS NULL for binding null values.
	 */
	private String genDynamicWhere(Set<String> loadedProps, Object oldBean) {

		// always has a preceding id property(s) so the first
		// option is always ' and ' and not blank.
		
		GenerateDmlRequest request = new GenerateDmlRequest(loadedProps, oldBean);
		
		//request.setBean(oldBean);
		request.append(sqlNone);
		
		request.setWhereMode();
		all.dmlWhere(request, true, oldBean);
		
		return request.toString();
	}
	
}
