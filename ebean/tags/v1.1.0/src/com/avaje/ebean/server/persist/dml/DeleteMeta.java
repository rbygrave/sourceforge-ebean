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

import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.persist.dmlbind.Bindable;

/**
 * Meta data for delete handler. The meta data is for a particular bean type. It
 * is considered immutable and is thread safe.
 */
public final class DeleteMeta {

	private final String sqlVersion;

	private final String sqlNone;

	private final Bindable id;

	private final Bindable version;

	private final Bindable all;

	private final String tableName;

	private final BeanDescriptor desc;

	public DeleteMeta(BeanDescriptor desc, Bindable id, Bindable version, Bindable all) {
		this.desc = desc;
		this.tableName = desc.getBaseTable();
		this.id = id;
		this.version = version;
		this.all = all;

		sqlNone = genSql(ConcurrencyMode.NONE, null, null);
		sqlVersion = genSql(ConcurrencyMode.VERSION, null, null);
	}

	/**
	 * Return the table name.
	 */
	public String getTableName() {
		return tableName;
	}

	/**
	 * Bind the request based on the concurrency mode.
	 */
	public void bind(PersistRequest persist, DmlHandler bind)
			throws SQLException {

		Object bean = persist.getBean();

		id.dmlBind(bind, false, bean, true);

		int mode = persist.getConcurrencyMode();
		if (mode == ConcurrencyMode.VERSION) {
			version.dmlBind(bind, false, bean, true);

		} else if (mode == ConcurrencyMode.ALL) {
			
			Object oldBean = persist.getOldValues();
			all.dmlBind(bind, true, oldBean, false);
		}
	}

	/**
	 * get or generate the sql based on the concurrency mode.
	 */
	public String getSql(PersistRequest request) throws SQLException {

		EntityBeanIntercept ebi = request.getEntityBeanIntercept();
		if (ebi != null) {
			Set<String> loadedProps = ebi.getLoadedProps();
			if (loadedProps != null){
				// 'partial bean' deletion...
				// determine the concurrency mode
				int mode = ConcurrencyMode.ALL;
				BeanProperty prop = desc.firstVersionProperty();
				if (prop != null && loadedProps.contains(prop.getName())){
					mode = ConcurrencyMode.VERSION;
				}
				// generate SQL dynamically depending on the
				// loadedProps
				request.setConcurrencyMode(mode);
				return genSql(mode, loadedProps, request.getOldValues());
			}
		}
		
		// 'full bean' deletion
		int mode = request.getConcurrencyMode();
		switch (mode) {
		case ConcurrencyMode.NONE:
			return sqlNone;
		
		case ConcurrencyMode.VERSION:
			return sqlVersion;
			
		case ConcurrencyMode.ALL:
			Object oldValues = request.getOldValues();
			return genDynamicWhere(oldValues, null);

		default:
			throw new RuntimeException("Invalid mode " + mode);
		}
	}

	private String genSql(int conMode, Set<String> loadedProps, Object oldBean) {

		// delete ... where bcol=? and bc1=? and bc2 is null and ...

		GenerateDmlRequest request = new GenerateDmlRequest(loadedProps);
				
		request.append("delete from ").append(tableName);
		request.append(" where ");

		request.setWhereIdMode();
		id.dmlAppend(request, false);

		if (conMode == ConcurrencyMode.VERSION) {
			if (version == null) {
				return null;
			}
			version.dmlAppend(request, false);

		} else if (conMode == ConcurrencyMode.ALL) {
			//request.setWhereMode();
			all.dmlWhere(request, true, oldBean);
		}

		return request.toString();
	}

	/**
	 * Generate the sql dynamically for where using IS NULL for binding null
	 * values.
	 */
	private String genDynamicWhere(Object oldBean, Set<String> loadedProps) throws SQLException {

		// always has a preceding id property(s) so the first
		// option is always ' and ' and not blank.
		
		GenerateDmlRequest request = new GenerateDmlRequest(loadedProps);

		
		request.append(sqlNone);

		request.setWhereMode();
		all.dmlWhere(request, true, oldBean);

		return request.toString();
	}

}
