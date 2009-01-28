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

import java.util.Map;

/**
 * Holds a tree structure of known possible joins for a given bean type.
 * <p>
 * Each node of the tree matches a join. The joins are for associated One Beans,
 * associated Many Beans, Secondary tables as well as joins required for
 * inheritance hierarchy.
 * </p>
 */
public final class JoinTree {

	private final int maxTreeDepth;

	private final JoinNode root;

	private final Map<String, PropertyDeploy> deployMap;

	/**
	 * Create the JoinTree.
	 */
	public JoinTree(JoinNode root, int maxDepth, Map<String, PropertyDeploy> deployMap) {
		this.root = root;
		this.maxTreeDepth = maxDepth;
		this.deployMap = deployMap;
	}

	/**
	 * Return the map of logical to physical deployment.
	 */
	public Map<String, PropertyDeploy> getDeployMap() {
		return deployMap;
	}

	/**
	 * Returns the maximum depth of the tree.
	 * <p>
	 * This max depth indicates the maximum number of beans that can be
	 * represented for a given row in a sql result set.
	 * </p>
	 */
	public int getMaxTreeDepth() {
		return maxTreeDepth;
	}

	/**
	 * Return the root node of the tree.
	 */
	public JoinNode getRoot() {
		return root;
	}
}
