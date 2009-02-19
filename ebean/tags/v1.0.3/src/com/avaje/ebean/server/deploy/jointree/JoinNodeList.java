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
package com.avaje.ebean.server.deploy.jointree;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.jointree.DeployPropertyFactory.DeployPropertyRequest;

/**
 * A JoinNode for associated many beans (1-M and M-M).
 */
public class JoinNodeList extends JoinNode {

	final BeanPropertyAssocMany manyProperty;
	
	final String extraWhere;
	
	/**
	 * Create for a given assoc many property.
	 */
	public JoinNodeList(JoinNode parent, TableJoin tableJoin, BeanDescriptor desc,
			String propertyPrefix, DeployPropertyRequest deployPropertyRequest, BeanPropertyAssocMany listProp) {

		super(Type.LIST, listProp.getName(), parent, tableJoin, desc, propertyPrefix, deployPropertyRequest);
		this.manyProperty = listProp;
		
		// many properties always use outer joins
		this.outerJoin = true;
		this.extraWhere = convertExtraWhere(manyProperty.getExtraWhere());
	}
	
	/**
	 * If true this is a join to a bean that has OneToMany Association.
	 */
	public boolean isManyJoin() {
		return true;
	}
	
	public String getExtraWhere(){
		return extraWhere;
	}
	
	/**
	 * Return the AssocMany BeanProperty.
	 */
	@Override
	public BeanPropertyAssocMany getManyProp() {
		return manyProperty;
	}
	
	@Override
	public BeanPropertyAssocOne getBeanProp() {
		return null;
	}
	
	protected void appendDescription(StringBuffer sb) {

		sb.append("list: ");
		sb.append(manyProperty.getTargetType().getName());
	}

}
