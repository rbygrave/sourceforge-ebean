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
import org.avaje.ebean.server.deploy.jointree.DeployPropertyFactory.DeployPropertyRequest;

/**
 * A JoinNode for the Root node.
 */
public class JoinNodeRoot extends JoinNode {

	/**
	 * Create the root node.
	 */
	public JoinNodeRoot(BeanDescriptor descriptor, DeployPropertyRequest deployPropertyRequest) {

		super(descriptor, deployPropertyRequest);
	}

	protected void appendDescription(StringBuffer sb) {
		
		sb.append("root: ");
		sb.append(desc.getBeanType().getName());
	}

	@Override
	public BeanPropertyAssocOne getBeanProp() {
		return null;
	}

	public String getExtraWhere(){
		return null;
	}

	@Override
	public BeanPropertyAssocMany getManyProp() {
		throw new RuntimeException("Error - should not be called");
	}
	
	
}
