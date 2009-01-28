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
package org.avaje.ebean.server.deploy.jointree;

import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import org.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import org.avaje.ebean.server.deploy.TableJoin;
import org.avaje.ebean.server.deploy.jointree.DeployPropertyFactory.DeployPropertyRequest;

/**
 * A JoinNode for associated one beans (1-1 and M-1).
 */
public class JoinNodeBean extends JoinNode {

	/**
	 * The BeanPropertyAssocOne for this node.
	 */
	final BeanPropertyAssocOne beanProperty;
	
	final String extraWhere;
	
	/**
	 * Create for a given assoc many property.
	 */
	public JoinNodeBean(JoinNode parent, TableJoin tableJoin, BeanDescriptor desc,
			String propertyPrefix,DeployPropertyRequest deployPropertyRequest, BeanPropertyAssocOne beanProp) {

		super(Type.BEAN, beanProp.getName(), parent, tableJoin, desc, propertyPrefix, deployPropertyRequest);

		this.beanProperty = beanProp;
		this.extraWhere = convertExtraWhere(beanProp.getExtraWhere());

		if (parent.outerJoin || beanProp.isOuterJoin()) {
			this.outerJoin = true;
		} else {
			this.outerJoin = false;
		}
	}

	/**
	 * If true this is a join to a single bean. ManyToOne or OneToOne.
	 */
	public boolean isBeanJoin() {
		return true;
	}
	
	public String getExtraWhere(){
		return extraWhere;
	}

	/**
	 * Return the AssocOne BeanProperty.
	 */
	@Override
	public BeanPropertyAssocOne getBeanProp() {
		return beanProperty;
	}
	
	@Override
	public BeanPropertyAssocMany getManyProp() {
		throw new RuntimeException("Error?");
	}

	protected void appendDescription(StringBuffer sb) {
		
		sb.append("bean: ");
		sb.append(desc.getBeanType().getName());
	}
}
