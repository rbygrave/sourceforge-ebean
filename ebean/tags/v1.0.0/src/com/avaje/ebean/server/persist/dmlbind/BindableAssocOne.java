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

import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.id.ImportedId;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;

/**
 * Bindable for an ManyToOne or OneToOne associated bean.
 */
public class BindableAssocOne implements Bindable {

	private final BeanPropertyAssocOne assocOne;

	private final ImportedId importedId;
	
	public BindableAssocOne(BeanPropertyAssocOne assocOne) {
		this.assocOne = assocOne;
		this.importedId = assocOne.getImportedId();
	}

	public String toString() {
		return "BindableAssocOne "+assocOne;
	}
	
	public void dmlAppend(GenerateDmlRequest request, boolean checkIncludes) {
		if (checkIncludes && !request.isIncluded(assocOne)){
			return;
		}
		importedId.dmlAppend(request);
	}

	/**
	 * Used for dynamic where clause generation.
	 */
	public void dmlWhere(GenerateDmlRequest request, boolean checkIncludes, Object bean){
		if (checkIncludes && !request.isIncluded(assocOne)){
			return;
		}
		Object assocBean = assocOne.getValue(bean);
		importedId.dmlWhere(request, assocBean);
	}
	
	public void dmlBind(BindableRequest request, boolean checkIncludes, Object bean, boolean bindNull) throws SQLException {
		if (checkIncludes && !request.isIncluded(assocOne)){
			return;
		}
		Object assocBean = assocOne.getValue(bean);
		importedId.bind(request, assocBean, bindNull);
	}

}
