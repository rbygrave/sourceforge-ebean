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

import java.util.HashSet;

import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Request object for creating a JoinTree.
 */
public class JoinTreeRequest {

	BeanDescriptor descriptor;

	HashSet<Class<?>> treeTypes = new HashSet<Class<?>>();

	Class<?> rootType;

	int maxObjectDepth;

	boolean hitMaxDepth;

	/**
	 * Create a Request to create a JoinTree.
	 */
	public JoinTreeRequest(BeanDescriptor descriptor) {
		this.descriptor = descriptor;
		this.rootType = descriptor.getBeanType();
	}

	/**
	 * Return the BeanDescriptor for the root node.
	 */
	public BeanDescriptor getBeanDescriptor() {
		return descriptor;
	}

	/**
	 * Restart the set of types already defined in the tree.
	 * <p>
	 * This is reset for each base join to a many property.
	 * </p>
	 */
	public void restartTypes() {
		treeTypes = new HashSet<Class<?>>();
		addType(rootType);
	}

	/**
	 * Determine if the tree already contains node for the given type.
	 */
	public boolean containsType(Class<?> type) {
		return treeTypes.contains(type);
	}

	public void addType(Class<?> type) {
		treeTypes.add(type);
	}

	/**
	 * Set the maximum tree depth.
	 */
	public void setMaxObjectDepth(int max) {
		if (max > maxObjectDepth) {
			maxObjectDepth = max;
		}
	}

	/**
	 * Return the maximum tree depth.
	 * <p>
	 * This is later set to the JoinTree.
	 * </p>
	 */
	public int getMaxObjectDepth() {
		return maxObjectDepth;
	}

	/**
	 * Return true if the maxDepth limit was hit when building this join tree.
	 */
	public boolean isHitMaxDepth() {
		return hitMaxDepth;
	}

	/**
	 * In building the join tree the maxDepth limit was hit.
	 */
	public void setHitMaxDepth(boolean hitMaxDepth) {
		this.hitMaxDepth = hitMaxDepth;
	}

}
