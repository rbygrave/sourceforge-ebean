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
package com.avaje.ebean.server.persist.mapbean;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.LinkedHashMap;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.PersistRequest;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.persist.Binder;

/**
 * Process Delete of a MapBean.
 */
public class DeleteMapBean extends BaseMapBean {

	MapBean oldBean;
	
	boolean firstColumn = true;
	
	//boolean noVersionColumn;

	
	@SuppressWarnings("unchecked")
	public DeleteMapBean(Binder binder, PersistRequest request) {
		super(binder, request);
		this.oldBean = (MapBean)request.getOldValues();
		if (oldBean == null){
			// deleting it without ever modifying it
			oldBean = mapBean;
		}
		
		generate();
	}

	/**
	 * execute the delete and perform checkRowCount() and postExecute().
	 */
	protected void executeStmt(PreparedStatement pstmt) throws SQLException {
		int rc = pstmt.executeUpdate();
		request.checkRowCount(rc);
		request.postExecute();
	}

	protected void generate() {
		genSql.append("delete from ").append(desc.getBaseTable());
		genSql.append(" where ");
		
		bindLogAppend("Binding delete [");
		bindLogAppend(desc.getBaseTable());
		bindLogAppend("]  where[");
		
		whereIdColumns();
		
		int conMode = request.getConcurrencyMode();
		if (conMode == ConcurrencyMode.VERSION){
			whereVersionColumns();

		} else if (conMode == ConcurrencyMode.ALL) {
			whereBaseColumns();			
		}
	}


	protected void whereIdColumns() {
		BeanProperty[] uids = desc.propertiesId();
		if (uids.length == 1){
			BeanProperty uid = uids[0];
			String propName = uid.getName();
			String dbColumn = uid.getDbColumn();
			int dbType = uid.getDbType();
			
			Object value = mapBean.get(propName);
			includeWhere(propName, dbColumn, value, dbType);
			
			// for summary logging purposes
			request.setBoundId(value);
			
		} else {
			LinkedHashMap<String,Object> mapId = new LinkedHashMap<String, Object>();
			for (int i = 0; i < uids.length; i++) {
				String propName = uids[i].getName();
				String dbColumn = uids[i].getDbColumn();
				int dbType = uids[0].getDbType();
				
				Object value = mapBean.get(propName);
				includeWhere(propName, dbColumn, value, dbType);
				
				mapId.put(propName, value);
			}
			
			// for summary logging purposes
			request.setBoundId(mapId);
		}

	}
	
	protected void whereVersionColumns() {
		BeanProperty[] props = desc.propertiesVersion();
		for (int i = 0; i < props.length; i++) {
			String propName = props[i].getName();
			if (mapBean.containsKey(propName)) {
				includeWhere(props[i], mapBean);
			} else {
				//Hmmm, ends up with No Concurrency checking
				// but the developer explicitly choose NOT
				// to include that column so letting it go
				//noVersionColumn = true;
			}
		}
	}
	
	/**
	 * For ALL concurrency mode bind using the OldValues.
	 */
	protected void whereBaseColumns() {
		
		BeanProperty[] props = desc.propertiesBaseScalar();
		for (int i = 0; i < props.length; i++) {
			String propName = props[i].getName();
			if (oldBean.containsKey(propName)) {
				includeWhere(props[i], oldBean);
			}
		}
	}
	
	
	private void includeWhere(BeanProperty prop, MapBean bean) {
		
		String propName = prop.getName();
		int dbType = prop.getDbType();
		
		if (!isLob(dbType)){
			Object value = bean.get(propName);
			String dbColumn = prop.getDbColumn();
			includeWhere(propName, dbColumn, value, dbType);
		}
	}

	private boolean isLob(int type){
		switch (type) {
		case Types.LONGVARCHAR:
			return true;
		case Types.CLOB:
			return true;
		case Types.LONGVARBINARY:
			return true;
		case Types.BLOB:
			return true;

		default:
			return false;
		}
	}
	
	private void includeWhere(String propName, String dbColumn, Object value, int dbType) {
		
		if (firstColumn){
			firstColumn = false;
		} else {
			genSql.append(" and ");
		}
		genSql.append(dbColumn);
		if (value == null){
			// exclude nulls from binding in where clause
			genSql.append(" is null");
			bindValues.addComment(", "+propName+"=NULL");
			
		} else {
			genSql.append("=?");
			bindValue(value, dbType, propName);
		}
	}
	
}
