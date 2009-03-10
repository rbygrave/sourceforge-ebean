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

import java.util.ArrayList;
import java.util.Iterator;

import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.jointree.DeployPropertyFactory.DeployPropertyRequest;
import com.avaje.ebean.server.lib.util.StringHelper;

/**
 * Holds the tree structure of associated beans.
 */
public abstract class JoinNode {
	
	public enum Type {
		/**
		 * The root node of the Join Tree.
		 */
		ROOT,
		
		/**
		 * A join to associated one Beans.
		 */
		BEAN,
		
		/**
		 * A join to associated many Beans.
		 */
		LIST,
		
		/**
		 * A place holder for Embedded Beans.
		 * Required for extra join checking.
		 */
		EMBEDDED
	}
	
	/**
	 * Set to true if this bean or ANY of its parents are outer joined.
	 */
	boolean outerJoin;

	/**
	 * The direct children beans.
	 */
	ArrayList<JoinNode> children = new ArrayList<JoinNode>();

	JoinNode[] childArray;
	
	/**
	 * The level, starts at 0.
	 */
	final int joinDepth;
	final int objectDepth;
	final int embeddedObjectCount;
	
	final Type type;

	/**
	 * The descriptor for this node.
	 */
	final BeanDescriptor desc;

	/**
	 * The property prefix built up by depth.
	 */
	final String propertyPrefix;

	final TableJoin tableJoin;
	
	/**
	 * the prefix used for on clause. (one char truncated from prefix).
	 */
	final String parentTableAlias;
	
	final String tableAlias;
	
	final String name;
	
	final JoinNode root;

	final JoinNode parent;

	final PropertyDeploy[] propertyDeployList;
	
	final PropertyDeploy[] fkeyDeployList;
	
	/**
	 * Create the tree.
	 */
	protected JoinNode(BeanDescriptor descriptor, DeployPropertyRequest deployPropertyRequest) {
		this.type = Type.ROOT;
		this.joinDepth = 0;
		this.objectDepth = 0;
		this.embeddedObjectCount = descriptor.propertiesEmbedded().length;
		this.root = this;
		this.name = null;
		this.parent = null;
		this.desc = descriptor;
		this.propertyDeployList = deployPropertyRequest.getProperties();
		this.fkeyDeployList = deployPropertyRequest.getForeignKeys();
		
		this.tableAlias = desc.getBaseTableAlias();
		
		this.parentTableAlias = null;
		this.propertyPrefix = null;
		this.tableJoin = null;
	}


	/**
	 * Used internally for Embedded Beans place holder.
	 */
	protected JoinNode(String name, JoinNode parent,  BeanDescriptor desc, String propertyPrefix) {

		this.type = Type.EMBEDDED;
		this.name = name;
		this.root = parent.getRoot();
		this.parent = parent;
		parent.addChild(this);
		
		this.propertyPrefix = propertyPrefix;
		this.desc = desc;
		this.embeddedObjectCount = 0;
		this.propertyDeployList = new PropertyDeploy[0];
		this.fkeyDeployList = new PropertyDeploy[0];

		this.joinDepth = parent.joinDepth;
		this.objectDepth = parent.objectDepth;
		this.tableJoin = null;
		this.parentTableAlias = null;
		this.tableAlias = null;
	}
	
	/**
	 * Used internally when building the tree.
	 */
	protected JoinNode(Type type, String name, JoinNode parent, TableJoin tableJoin, BeanDescriptor desc,
			String propertyPrefix, DeployPropertyRequest deployPropertyRequest) {

		this.type = type;
		this.name = name;
		this.desc = desc;

		this.root = parent.getRoot();
		this.parent = parent;
		parent.addChild(this);
		
		this.propertyDeployList = deployPropertyRequest.getProperties();
		this.fkeyDeployList = deployPropertyRequest.getForeignKeys();
		this.propertyPrefix = propertyPrefix;
		this.embeddedObjectCount = desc.propertiesEmbedded().length;
		
		this.joinDepth = parent.joinDepth + 1;
		this.objectDepth = parent.objectDepth + parent.embeddedObjectCount + 1;

		this.tableJoin = tableJoin;
		this.parentTableAlias = tableJoin.getLocalTableAlias();
		this.tableAlias = tableJoin.getForeignTableAlias();
	}
	
	/**
	 * Return the extra where clause for this join node or null.
	 */
	public abstract String getExtraWhere();
	
	String convertExtraWhere(String extraWhere){
		if (extraWhere == null){
			return null;
		} else {
			return StringHelper.replaceString(extraWhere, "${ta}", tableAlias);
		}
	}

	/**
	 * Make sure the nodes are initialised.
	 */
	protected void initChildren() {
		JoinNode[] childArr =  children();
		for (int i = 0; i < childArr.length; i++) {
			childArr[i].initChildren();
		}
	}
	
	/**
	 * Return the children of this node.
	 */
	public JoinNode[] children() {
		if (childArray == null) {
			childArray = (JoinNode[]) children.toArray(new JoinNode[children.size()]);
		}
		return childArray;
	}

	/**
	 * Find the child using propertyName including dot notation.
	 * <p>
	 * e.g. customer.shippingAddress.country
	 * </p>
	 */
	public JoinNode findChild(String propertyName) {
		
		if (propertyName == null){
			return root;
		}
		
		int dotPos = propertyName.indexOf('.');
		if (dotPos == -1){
			// a direct child of this node
			return findChildLocal(propertyName);
			
		} else {
			// find the child to ask...
			String childName = propertyName.substring(0,dotPos);
			JoinNode localParent = findChildLocal(childName);
			if (localParent == null){
				// not found
				return null;
			} else {
				String grandChildName = propertyName.substring(dotPos+1);
				return localParent.findChild(grandChildName);
			}
		}
	}
	
	public JoinNode findChildLocal(String propertyName) {
		for (int i = 0; i < childArray.length; i++) {		
			if (childArray[i].name.equalsIgnoreCase(propertyName)){
				return childArray[i];
			}
		}
		
		return null;
	}
	
	/**
	 * Return the name of this join.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Return the root node.
	 */
	public JoinNode getRoot() {
		return root;
	}

	/**
	 * Return the parent node.
	 */
	public JoinNode getParent() {
		return parent;
	}


	protected void addChild(JoinNode child){
		children.add(child);
	}
	
	/**
	 * Return a list of properties and their logical to physical name mapping.
	 */
	public PropertyDeploy[] getPropertyDeployList() {
		return propertyDeployList;
	}

	
	/**
	 * List of foreign key properties on this node.
	 */
	public PropertyDeploy[] getFkeyDeployList() {
		return fkeyDeployList;
	}

	/**
	 * Return the type of this join.
	 */
	public Type getType() {
		return type;
	}

	/**
	 * If true this is a join to a bean that has OneToMany Association.
	 */
	public boolean isRoot() {
		return type == Type.ROOT;
	}
	
	/**
	 * If true this is a join to a bean that has OneToMany Association.
	 */
	public boolean isEmbeddedBean() {
		return type == Type.EMBEDDED;
	}
	
	/**
	 * If true this is a join to a bean that has OneToMany Association.
	 */
	public boolean isManyJoin() {
		return type == Type.LIST;
	}

	/**
	 * If true represents a 'join' for a SubClass.
	 */
	public boolean isSubClassJoin() {
		return false;
	}

	/**
	 * If true this is a join to a single bean. ManyToOne or OneToOne.
	 */
	public boolean isBeanJoin() {
		return type == Type.BEAN;
	}

	/**
	 * Return the table alias.
	 */
	public String getTableAlias() {
		return tableAlias;
	}

	/**
	 * Return the property prefix for this node.
	 */
	public String getPropertyPrefix() {
		return propertyPrefix;
	}

	/**
	 * Return the associated BeanDescriptor for this node.
	 */
	public BeanDescriptor getBeanDescriptor() {
		return desc;
	}

	/**
	 * Return the level for this node in terms of table joins.
	 */
	public int getJoinDepth() {
		return joinDepth;
	}

	/**
	 * Return the 'starting depth' of this node in terms of Objects.
	 * Embedded objects will add to this depth.
	 */
	public int getObjectDepth() {
		return objectDepth;
	}
	
	public int getMaxObjectDepth() {
		return objectDepth + embeddedObjectCount;
	}
	
	public int getEmbeddedObjectCount() {
		return embeddedObjectCount;
	}	
	
	public final void addJoin(boolean forceOuterJoin, DbSqlContext ctx){
		tableJoin.addJoin(forceOuterJoin, this, ctx);
	}
	
	/**
	 * Return the AssocOne BeanProperty.
	 */
	public abstract BeanPropertyAssocOne getBeanProp();

	/**
	 * Return the AssocMany BeanProperty.
	 */
	public abstract BeanPropertyAssocMany getManyProp();
	
	/**
	 * Return the inheritance info if this node has it.
	 */
	public InheritInfo getInheritInfo() {
		throw new RuntimeException("Overridden");
	}
	
	/**
	 * Return true if this node uses a left outer join.
	 */
	public boolean isOuterJoin() {
		return outerJoin;
	}


	public String toString() {
		JoinNode parent = getParent();
		if (parent == null){
			// this is the root level
			return getBeanDescriptor().toString();
		} else {
			return getRoot().toString()+"."+propertyPrefix;
		}
	}
	
	/**
	 * A string description.
	 */
	public String getDescription() {
		StringBuffer sb = new StringBuffer();
		addToString(sb, this);
		return sb.toString();
	}

	/**
	 * Used to build tree iterator.
	 */
	private void addToString(StringBuffer sb, JoinNode node) {
		
		sb.append(node.getShortDescription()).append("\r\n");
		
		Iterator<JoinNode> it = node.children.iterator();
		while (it.hasNext()) {
			JoinNode child = (JoinNode) it.next();
			addToString(sb, child);
		}
	}

	protected abstract void appendDescription(StringBuffer sb);
	
	/**
	 * Get a description for debugging.
	 */
	private String getShortDescription() {
		StringBuffer sb = new StringBuffer();
		append(sb, String.valueOf(joinDepth), 4);
		append(sb, String.valueOf(objectDepth), 4);
		append(sb, getTableAlias(), 9);
		append(sb, propertyPrefix, 25);
		sb.append(" outerJoin: ");
		append(sb, String.valueOf(outerJoin), 6);
		
		appendDescription(sb);

		return sb.toString();
	}
	
	private void append(StringBuffer sb, String content, int minLength){
		if (content == null){
			content = "";
		}
		sb.append(content);
		int padding = minLength - content.length();
		if (padding > 0){
			for (int i = 0; i < padding; i++) {
				sb.append(" ");
			}
		}
	}
}
