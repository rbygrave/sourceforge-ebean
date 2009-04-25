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
package com.avaje.ebean.server.persist.dmlbind;

import java.sql.SQLException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.PersistRequestBean;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;

/**
 * Bindable for a single scalar id property.
 */
public class BindableIdScalar implements BindableId {

	private final BeanProperty uidProp;
	
	public BindableIdScalar(BeanProperty uidProp) {
		this.uidProp = uidProp;
	}	
	public boolean isConcatenated() {
		return false;
	}

	@Override
	public String toString() {
		return uidProp.toString();
	}
	
	/**
	 * Does nothing for BindableId. 
	 */
	public void determineChangedProperties(PersistRequestBean<?> request) {
		// do nothing (id not changing)
	}
	
	/**
	 * Should not be called as this is really only for concatenated keys.
	 */
	public boolean deriveConcatenatedId(PersistRequestBean<?> persist) {
		throw new PersistenceException("Should not be called? only for concatinated keys");
	}
	
	/**
	 * Id values are never null in where clause.
	 */
	public void dmlWhere(GenerateDmlRequest request, boolean checkIncludes, Object bean){
		// id values are never null in where clause
		request.appendColumn(uidProp.getDbColumn());
	}
	
	public void dmlAppend(GenerateDmlRequest request, boolean checkIncludes) {
		
		request.appendColumn(uidProp.getDbColumn());
	}
	
	public void dmlBind(BindableRequest bindRequest, boolean checkIncludes, Object bean, boolean bindNull) throws SQLException {
		
		Object value = uidProp.getValue(bean);
		
		bindRequest.bind(value, uidProp, uidProp.getName(), bindNull);
		
		// used for summary logging
		bindRequest.setIdValue(value);
	}

}
