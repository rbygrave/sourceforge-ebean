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
package com.avaje.ebean.server.el;

import com.avaje.ebean.server.deploy.BeanProperty;


/**
 * A ElGetValue based on a chain of properties.
 * <p>
 * Used to get the value for an compound expression like customer.name or
 * customer.shippingAddress.city etc.
 * </p>
 * <p>
 * Note that if any element in the chain returns null, then null is returned and
 * no further processing of the chain occurs.
 * </p>
 */
public class ElPropertyChain implements ElPropertyValue {

	private final String prefix;

	private final String placeHolder;
	
	private final String name;

	private final boolean containsMany;
	
	private final ElPropertyValue[] chain;

	public ElPropertyChain(boolean containsMany, boolean embedded, String expression, ElPropertyValue[] chain) {
		
		this.containsMany = containsMany;
		this.chain = chain;
		
		int dotPos = expression.lastIndexOf('.');
		if (dotPos > -1){
			name = expression.substring(dotPos+1);
			if (embedded){
				int embPos = expression.lastIndexOf('.',dotPos-1);				
				prefix = embPos == -1 ? null : expression.substring(0, embPos);
				
			} else {
				prefix = expression.substring(0, dotPos);
			}
		} else {
			prefix = null;
			name = expression;
		}		
		placeHolder = calcPlaceHolder(prefix,getDbColumn());

	}

	private String calcPlaceHolder(String prefix, String dbColumn){
		if (prefix != null){
			return "${"+prefix+"}"+dbColumn;
		} else {
			return ROOT_ELPREFIX+dbColumn;
		}
	}
	
	/**
	 * Full ElGetValue support.
	 */
	public boolean isDeployOnly() {
		return false;
	}

	
	public boolean containsMany() {
		return containsMany;
	}

	public String getElPrefix() {
		return prefix;
	}

	public String getName() {
		return name;
	}
	
	public String getElPlaceholder() {
		return placeHolder;
	}

	public String getDbColumn() {
		return chain[chain.length-1].getDbColumn();
	}
	
	public BeanProperty getBeanProperty() {
		return chain[chain.length-1].getBeanProperty();
	}

	public Object elConvertType(Object value){
		// just convert using the last one in the chain
		return chain[chain.length-1].elConvertType(value);
	}
	
	public Object elGetValue(Object bean) {

		for (int i = 0; i < chain.length; i++) {
			bean = chain[i].elGetValue(bean);
			if (bean == null) {
				return bean;
			}
		}

		return bean;
	}
}
