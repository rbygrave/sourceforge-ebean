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

/**
 * A JoinNode for embedded beans.
 * <p>
 * These do not require joins as they use their parent's joins.
 * The reason JoinNodeEmbeddedBean exists is to safely find extra
 * joins for embedded properties and safely ignore those extra joins.
 * </p>
 */
public class JoinNodeEmbeddedBean extends JoinNode {

	/**
	 * The BeanPropertyAssocOne for this node.
	 */
	final BeanPropertyAssocOne beanProperty;
	
	/**
	 * Create for a given assoc many property.
	 */
	public JoinNodeEmbeddedBean(JoinNode parent, BeanDescriptor desc,
			String propertyPrefix, BeanPropertyAssocOne beanProp) {

		super(beanProp.getName(), parent, desc, propertyPrefix);
		this.beanProperty = beanProp;
	}

	/**
	 * If true this is a join to a single bean. ManyToOne or OneToOne.
	 */
	public boolean isBeanJoin() {
		return false;
	}

	
	@Override
	public String getExtraWhere() {
		return null;
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
	}
}
