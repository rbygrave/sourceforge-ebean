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
package org.avaje.ebean.server.persist.mapbean;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;

import javax.persistence.PersistenceException;

import org.avaje.ebean.server.core.PersistRequest;
import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.deploy.generatedproperty.GeneratedProperty;
import org.avaje.ebean.server.persist.Binder;
import org.avaje.ebean.util.Message;

/**
 * Process insert of a MapBean.
 */
public class InsertMapBean extends BaseMapBean {

	boolean firstColumn = true;
	
	boolean usingSequence;

	private final String sequenceNextVal;
	
	private final boolean supportsGeneratedKeys;
		
	public InsertMapBean(Binder binder, PersistRequest request, boolean genKeys) {
		super(binder, request);
		this.sequenceNextVal = desc.getSequenceNextVal();
		this.supportsGeneratedKeys = genKeys;
		generate();
	}

	/**
	 * Add getGeneratedKeys support for insert.
	 */
	protected void executeStmt(PreparedStatement pstmt) throws SQLException {
		int rc = pstmt.executeUpdate();
		request.checkRowCount(rc);
		
		if (usingGeneratedKeys) {
			getGeneratedKeys(pstmt);
		}
		request.postExecute();
	}
	
	
	protected void generate() {
		genSql.append("insert into ").append(desc.getBaseTable());
		genSql.append(" (");
		
		bindLogAppend("Binding insert [");
		bindLogAppend(desc.getBaseTable());
		bindLogAppend("]  set[");
		
		idColumns();
		baseColumns();
		
		genSql.append(") values (");
		
		if (usingSequence){
			genSql.append(sequenceNextVal);
			genSql.append(", ");
		}
		
		int bindCount = bindValues.size();
		for (int i = 0; i < bindCount; i++) {
			if (i > 0){
				genSql.append(", ");
			}
			genSql.append("?");
		}
		genSql.append(")");
	}

	private void idColumns() {
		BeanProperty[] uids = desc.propertiesId();
		if (uids.length == 1){
			Object idValue = uids[0].getValue(mapBean);
			if (idValue == null){
				usingGeneratedKeys = supportsGeneratedKeys;
				
				if (sequenceNextVal == null){
					// Identity or autoincrement type column
				} else {
					// Include column but not value
					usingSequence = true;
					includeSql(uids[0].getDbColumn());
				}
			} else {
				include(uids[0]);
				
				// for summary logging purposes
				request.setBoundId(idValue);
			}
		} else {
			// assume concatinated keys will not used generated key
			LinkedHashMap<String,Object> mapId = new LinkedHashMap<String, Object>();
			
			for (int i = 0; i < uids.length; i++) {
				String name = uids[i].getName();
				String dbColumn = uids[i].getDbColumn();
				int dbType = uids[i].getDbType();
				Object value = mapBean.get(name);
				
				include(name, dbColumn, dbType, value);
				
				mapId.put(name, value);
			}
			
			// for summary logging purposes
			request.setBoundId(mapId);
		}
	}
	
	private void baseColumns() {
		
		BeanProperty[] base = desc.propertiesBaseScalar();
		for (int i = 0; i < base.length; i++) {
			
			GeneratedProperty genProp = base[i].getGeneratedProperty();
			boolean insertGen = genProp != null && genProp.includeInInsert();
			
			if (insertGen){
				include(base[i], genProp);
			} else {
				String propName = base[i].getName();
				if (mapBean.containsKey(propName)) {
					include(base[i]);
				}
			}
		}
	}
	
	private void include(BeanProperty prop, GeneratedProperty genProp) {
		
		String name = prop.getName();
		int dbType = prop.getDbType();

		Object value = genProp.getInsertValue(prop, mapBean);
		mapBean.set(name, value);
		
		bindValue(value, dbType, name);
		includeSql(prop.getDbColumn());
	}
	
	private void include(BeanProperty prop) {
		String name = prop.getName();
		String dbColumn = prop.getDbColumn();
		int dbType = prop.getDbType();
		Object value = mapBean.get(name);
		
		include(name, dbColumn, dbType, value);
	}
		
	private void include(String name, String dbColumn, int dbType, Object value) {
		
		bindValue(value, dbType, name);
		includeSql(dbColumn);
	}

	private void includeSql(String dbColumn) {
		if (firstColumn){
			firstColumn = false;
		} else {
			genSql.append(", ");
		}
		
		genSql.append(dbColumn);
	}

	
    /**
     * For non batch insert with generated keys.
     */
    private void getGeneratedKeys(PreparedStatement pstmt) throws SQLException {

        ResultSet rset = pstmt.getGeneratedKeys();
        if (rset.next()) {
            Object idValue = rset.getObject(1);
            if (idValue != null) {
            	request.setGeneratedKey(idValue);
            }

        } else {
            throw new PersistenceException(Message.msg("persist.autoinc.norows"));
        }
    }


}
