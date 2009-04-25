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
import java.util.ArrayList;

import com.avaje.ebean.server.core.ConcurrencyMode;
import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.generatedproperty.GeneratedProperty;
import com.avaje.ebean.server.persist.Binder;

/**
 * Process Update of a MapBean.
 */
public class UpdateMapBean extends DeleteMapBean {

	private ArrayList<UpdateGenValue> updateGenValues;
	
	public UpdateMapBean(Binder binder, PersistRequestBean<?> request) {
		super(binder, request);
	}
	
	/**
	 * Additionally need to set generated values back to the bean.
	 * This needs to occur prior to postExecute().
	 */
	protected void executeStmt(PreparedStatement pstmt) throws SQLException {
		int rc = pstmt.executeUpdate();
		request.checkRowCount(rc);
		
		// set generated values back to the mapBean
		// need to do this before postExecute()
		setUpdateGenValues();

		request.postExecute();
	}


	protected void generate() {
		genSql.append("update ").append(desc.getBaseTable());
		genSql.append(" set ");
		
		bindLogAppend("Binding update [");
		bindLogAppend(desc.getBaseTable());
		bindLogAppend("]  set[");
		
		setColumns();
		
		bindValues.addComment("]  where[");
		
		genSql.append(" where ");
		firstColumn = true;
		whereIdColumns();
		
		int conMode = request.getConcurrencyMode();
		if (conMode == ConcurrencyMode.VERSION){
			whereVersionColumns();

		} else if (conMode == ConcurrencyMode.ALL) {
			whereBaseColumns();			
		}

	}

	private void setColumns() {
		
		BeanProperty[] base = desc.propertiesBaseScalar();
		for (int i = 0; i < base.length; i++) {
			
			GeneratedProperty genProp = base[i].getGeneratedProperty();
			if (genProp != null){
				if (genProp.includeInUpdate()){
					includeSet(base[i], genProp);
				} else {
					// Not including 'Insert Timestamp' as never updated
				}
				
			} else {
				// included if contained in the map
				String propName = base[i].getName();
				if (mapBean.containsKey(propName)) {
					includeSet(base[i]);
				}
			}
		}
	}

	private void includeSet(BeanProperty prop) {
		
		String name = prop.getName();
		int dbType = prop.getDbType();
		Object value = mapBean.get(name);

		bindValue(value, dbType, name);
		includeSetSql(prop);
	}
	
	private void includeSet(BeanProperty prop, GeneratedProperty genProp) {
		
		String name = prop.getName();
		int dbType = prop.getDbType();

		Object value = genProp.getUpdateValue(prop, mapBean);

		bindValue(value, dbType, name);
		includeSetSql(prop);
		
		// we will set this back to the bean later
		// after the where clause has been bound
		registerUpdateGenValue(prop, mapBean, value);
	}
	
	private void includeSetSql(BeanProperty prop) {
		if (firstColumn){
			firstColumn = false;
		} else {
			genSql.append(", ");
		}
		genSql.append(prop.getDbColumn());
		genSql.append("=?");
	}
	

	
	/**
	 * Register a generated value on a update. This can not be set to the bean
	 * until after the where clause has been bound for concurrency checking.
	 * <p>
	 * GeneratedProperty values are likely going to be used for optimistic
	 * concurrency checking. This includes 'counter' and 'update timestamp'
	 * generation.
	 * </p>
	 */
	private void registerUpdateGenValue(BeanProperty prop, Object bean, Object value) {
		if (updateGenValues == null) {
			updateGenValues = new ArrayList<UpdateGenValue>();
		}
		updateGenValues.add(new UpdateGenValue(prop, bean, value));
	}

	/**
	 * Set any update generated values to the bean. Must be called after where
	 * clause has been bound.
	 */
	private void setUpdateGenValues() {
		if (updateGenValues != null) {
			for (int i = 0; i < updateGenValues.size(); i++) {
				UpdateGenValue updGenVal = (UpdateGenValue) updateGenValues.get(i);
				updGenVal.setValue();
			}
		}
	}
	
	/**
	 * Hold the values from GeneratedValue that need to be set to the bean
	 * property after the where clause has been built.
	 */
	private static class UpdateGenValue {

		private final BeanProperty property;

		private final Object bean;

		private final Object value;

		private UpdateGenValue(BeanProperty property, Object bean, Object value) {
			this.property = property;
			this.bean = bean;
			this.value = value;
		}

		/**
		 * Set the value to the bean property.
		 */
		private void setValue() {
			property.setValue(bean, value);
		}
	}
}
