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
package com.avaje.ebean.server.deploy;

import java.util.Map;

import com.avaje.ebean.server.deploy.jointree.JoinTree;
import com.avaje.ebean.server.deploy.jointree.PropertyDeploy;
import com.avaje.ebean.server.persist.BeanPersister;

/**
 * Holds the BeanDescriptor and its associated BeanPersister and JoinTree.
 * <p>
 * The reason for this BeanManager to hold this information (rather than say
 * BeanDescriptor) is is mostly due to the desire to make the BeanDescriptor
 * immutable.
 * </p>
 * <p>
 * The JoinTree is constructed after the BeanDescriptor.
 * </p>
 */
public class BeanManager<T> {

	private final BeanPersister persister;

	private final BeanDescriptor<T> descriptor;

	private final JoinTree joinTree;

	private final Map<String,PropertyDeploy> propMap;

	private final boolean autoFetchTunable;
	
	public BeanManager(BeanDescriptor<T> descriptor, JoinTree joinTree, BeanPersister persister) {
		this.descriptor = descriptor;
		this.joinTree = joinTree;
		this.propMap = joinTree == null? null : joinTree.getDeployMap();
		this.persister = persister;
		this.autoFetchTunable = descriptor.isAutoFetchTunable();
		descriptor.initialiseWithJoinTree(joinTree);
	}


	/**
	 * Return true if queries for beans of this type are autoFetch tunable.
	 */
	public boolean isAutoFetchTunable() {
		return autoFetchTunable;
	}
	
	/**
	 * Create a parser for converting logical property names to deployment names.
	 */
	public DeployPropertyParser createParser() {
		return new DeployPropertyParser(propMap);
	}

	
	/**
	 * Return the associated JoinTree.
	 */
	public JoinTree getBeanJoinTree() {
		return joinTree;
	}

	/**
	 * Return the associated BeanPersister.
	 */
	public BeanPersister getBeanPersister() {
		return persister;
	}

	/**
	 * Return the BeanDescriptor.
	 */
	public BeanDescriptor<T> getBeanDescriptor() {
		return descriptor;
	}

}
