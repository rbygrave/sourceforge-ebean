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
import java.util.Arrays;
import java.util.List;

import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.persist.dml.GenerateDmlRequest;

/**
 * Bindable for a Embedded bean.
 */
public class BindableEmbedded implements Bindable {

	final Bindable[] items;

	private final BeanPropertyAssocOne embProp;

	public BindableEmbedded(BeanPropertyAssocOne embProp, List<Bindable> list) {
		this.embProp = embProp;
		this.items = list.toArray(new Bindable[list.size()]);
	}

	public String toString() {
		return "BindableEmbedded "+embProp+" items:"+Arrays.toString(items);
	}
	
	
	public void dmlAppend(GenerateDmlRequest request, boolean checkIncludes){
		if (checkIncludes && !request.isIncluded(embProp)){
			return;
		}
		
		for (int i = 0; i < items.length; i++) {
			items[i].dmlAppend(request, false);
		}
	}
	
	/**
	 * Used for dynamic where clause generation.
	 */
	public void dmlWhere(GenerateDmlRequest request, boolean checkIncludes, Object origBean){
		if (checkIncludes && !request.isIncluded(embProp)){
			return;
		}
		Object embBean = embProp.getValue(origBean);
		
		for (int i = 0; i < items.length; i++) {
			items[i].dmlWhere(request, false, embBean);
		}
	}
	
	public void dmlBind(BindableRequest bindRequest, boolean checkIncludes, Object bean, boolean bindNull) throws SQLException{
		if (checkIncludes && !bindRequest.isIncluded(embProp)){
			return;
		}
		// get the embedded bean
		bean = embProp.getValue(bean);
		
		for (int i = 0; i < items.length; i++) {
			items[i].dmlBind(bindRequest, false, bean, bindNull);
		}
	}
	
}
