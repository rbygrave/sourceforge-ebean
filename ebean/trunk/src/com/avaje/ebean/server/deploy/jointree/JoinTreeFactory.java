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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.MapBean;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.jointree.DeployPropertyFactory.DeployPropertyRequest;
import com.avaje.ebean.server.deploy.parse.SqlReservedWords;
import com.avaje.ebean.server.plugin.PluginDbConfig;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Creates JoinTree's for given BeanDescriptors.
 */
public class JoinTreeFactory {

	private final Logger logger = Logger.getLogger(JoinTreeFactory.class.getName());
	
	private final int maxDepth;
	
	private final boolean sysoutOnMaxDepth;
	
	private final boolean debugJoinTree;
	
	private final DeployPropertyFactory deployPropertyFactory;
	
	public JoinTreeFactory(PluginDbConfig dbConfig) {
		PluginProperties properties = dbConfig.getProperties();
		deployPropertyFactory = new DeployPropertyFactory(dbConfig);
		maxDepth = properties.getPropertyInt("jointree.maxdepth", 10);
		sysoutOnMaxDepth = properties.getPropertyBoolean("jointree.sysoutOnMaxDepth", true);
		
		// set this to true or increase logging level to FINE to get
		// the join tree output (for debugging purposes)
		debugJoinTree = properties.getPropertyBoolean("debug.jointree", false);
	}
	
	/**
	 * Create the JoinTree for the given descriptor.
	 */
	public JoinTree create(BeanDescriptor<?> descriptor) {

		JoinTreeRequest request = new JoinTreeRequest(descriptor);

		DeployPropertyRequest deployPropertyRequest = deployPropertyFactory.createDeployProperties(descriptor);

		JoinNodeRoot root = new JoinNodeRoot(descriptor, deployPropertyRequest);
		request.setMaxObjectDepth(root.getMaxObjectDepth());
		
		buildTree(request, root);
		buildListTree(request, root);

		// initialise the children 
		root.initChildren();
		
		boolean sentOutput = false;
		if (request.isHitMaxDepth()){
			
			String msg = "The 'join tree' for "+descriptor+" has hit the maxdepth. This is unexpected. ";
			msg += "The join tree will be sent to sysout. Please review it and perhaps ask for help.";
			logger.log(Level.WARNING, msg);
			
			if (sysoutOnMaxDepth){
				sentOutput = true;
				System.out.println(" ## You are getting this sysout message because the 'Join Tree' for this bean");
				System.out.println(" ## .. has hit a maximum depth of ["+maxDepth+"]. This is generally NOT EXPECTED??");
				System.out.println(" ## .. you can specify in ebean.properties ebean.jointree.maxdepth=...");
				System.out.println(" ## .. to increase the maxdepth or ask for help. The join tree output is...");
				System.out.println(" ## JOIN TREE for : "+descriptor);
				System.out.println(root.getDescription());
				System.out.println(" ## END OF JOIN TREE for : "+descriptor);
			}
		}
		
		if (!sentOutput && (debugJoinTree || logger.isLoggable(Level.FINE))){
			if (descriptor.getBaseTable() == null || descriptor.getBeanType().equals(MapBean.class)){
				// skipping embedded beans, report beans and MapBeans...
			} else {
				String msg = "JoinTree for "+descriptor+"\n"+root.getDescription();
				logger.log(Level.INFO, msg);
			}
		}
		
		
		Map<String, PropertyDeploy> deployMap = new HashMap<String, PropertyDeploy>();
		Map<String,String> fkeyMap = new HashMap<String, String>();
		
		addDeployMap(root, deployMap, fkeyMap);

		return new JoinTree(root, request.getMaxObjectDepth(), deployMap);
	}

	
	/**
	 * Recursively build the deployment map.
	 */
	private void addDeployMap(JoinNode node, Map<String, PropertyDeploy> deployMap, Map<String,String> fkeyMap){

		// put foreign keys into map
		PropertyDeploy[] fkeyDeployList = node.getFkeyDeployList();
		for (int i = 0; i < fkeyDeployList.length; i++) {
			fkeyMap.put(fkeyDeployList[i].getLogical(), fkeyDeployList[i].getDeploy());
		}
		
		PropertyDeploy[] list = node.getPropertyDeployList();
		for (int i = 0; i < list.length; i++) {
			
			//deployMap.put(list[i].getLogical(), list[i]);
			
			if (!list[i].isForeignKey()){
				deployMap.put(list[i].getLogical(), list[i]);
				
			} else {
				// try to convert an imported foreign key property
				// to use the imported foreign key column and 
				// different include so as avoid the extra join
				
				String logical = list[i].getLogical();
				int lastDot = logical.lastIndexOf('.');
				if (lastDot == -1){
					throw new RuntimeException("error?");
				} 
				
				// trim off id property name (whatever it is)
				String foreignKeyName = logical.substring(0,lastDot);
				
				// lookup the foreignKey deployment value...
				String dbFkeyDeploy = fkeyMap.get(foreignKeyName);
				if (dbFkeyDeploy == null){
					// OneToMany or not found... use default...
					dbFkeyDeploy = list[i].getDeploy();
				} 
				
				PropertyDeploy fkPropertyDeploy = new PropertyDeploy(true, logical, dbFkeyDeploy, foreignKeyName);
				deployMap.put(logical, fkPropertyDeploy);			
			} 			
		}

		// recurse
		JoinNode[] children = node.children();
		for (int i = 0; i < children.length; i++) {
			addDeployMap(children[i], deployMap, fkeyMap);
		}
	}
	
	/**
	 * Builds the tree recursively.
	 * <p>
	 * baseType is typically rootType but changes for each many. It is used to
	 * stop the tree from infinite recursion. </>
	 */
	private void buildTree(JoinTreeRequest request, JoinNode parent) {

		BeanDescriptor<?> treeDesc = parent.getBeanDescriptor();
		
		BeanPropertyAssocOne<?>[] embedded = treeDesc.propertiesEmbedded();
		for (int i = 0; i < embedded.length; i++) {
			addEmbeddedChild(request, parent, embedded[i]);
		}

		BeanPropertyAssocOne<?>[] ones = treeDesc.propertiesOne();
		for (int i = 0; i < ones.length; i++) {

			BeanPropertyAssocOne<?> beanProp = ones[i];

			JoinNode subTree = addChild(request, parent, beanProp);
			Class<?> targetType = beanProp.getTargetType();

			boolean hitMaxDepth = (subTree.getJoinDepth() >= maxDepth); 
			
			boolean circularRelationship = false;

			// Climb the ancestor path to see if we already handled this
			// bean type or we reach the root (null)
			JoinNode ancestor = subTree.getParent();
			while (ancestor != null && !circularRelationship){
				final Class<?> ancestorType = ancestor.getBeanDescriptor().getBeanType();
				circularRelationship = targetType.equals(ancestorType);
				if (!circularRelationship){
					ancestor = ancestor.getParent();
				}
			}			
			
			if (circularRelationship) {
				//normal way to stop the recursion...
				logger.fine("Circular path detected at: " + subTree + " back to:" + ancestor);

			} else if (hitMaxDepth){
				request.setHitMaxDepth(true);
				// don't recurse further...
				String msg = "Max Depth of "+maxDepth+" reached building join tree ";
				msg += "for "+request.getBeanDescriptor().getFullName();
				logger.warning(msg);
				
			} else {
				// if same type as root. Do not continue tree walk.
				request.addType(targetType);
				buildTree(request, subTree);				
			}
		}
	}


	/**
	 * Add a Node for a join to a many property.
	 */
	private void buildListTree(JoinTreeRequest request, JoinNode parent) {

		BeanDescriptor<?> desc = parent.getBeanDescriptor();
		BeanPropertyAssocMany<?>[] manys = desc.propertiesMany();
		for (int i = 0; i < manys.length; i++) {

			// start treeTypes again for each many
			request.restartTypes();
			request.addType(manys[i].getTargetType());

			JoinNode subTree = addChild(request, parent, manys[i]);
			buildTree(request, subTree);
		}
	}

	/**
	 * Create and add a child node for a OneToMany List join. These are only
	 * added at level 1, not at any lower levels.
	 */
	private JoinNode addChild(JoinTreeRequest request, JoinNode parent,
			BeanPropertyAssocMany<?> listProp) {

		BeanDescriptor<?> parentDesc = parent.getBeanDescriptor();
		BeanDescriptor<?> forDesc = parentDesc.getBeanDescriptor(listProp.getTargetType());

		String propName = getPropName(listProp, parent);

		String destAlias = listProp.getTableJoin().getForeignTableAlias();
		String foreignAlias = getChildAlias(parent, destAlias);

		DeployPropertyRequest deployProps = deployPropertyFactory.createDeployProperties(forDesc,
				propName, foreignAlias);

		String parentTableAlias = parent.getTableAlias();
		if (listProp.isManyToMany()){
			// join to intersection table (not parent)
			parentTableAlias = listProp.getTableJoin().getLocalTableAlias();
		}
		TableJoin tableJoin = listProp.getTableJoin().createWithAlias(parentTableAlias, foreignAlias);
		
		JoinNodeList child = new JoinNodeList(parent, tableJoin, forDesc, propName,deployProps, listProp);

		request.setMaxObjectDepth(child.getMaxObjectDepth());

		return child;
	}

	
	/**
	 * Add a child node. Depth first.
	 */
	private JoinNode addChild(JoinTreeRequest request, JoinNode parent,
			BeanPropertyAssocOne<?> beanProp) {

		BeanDescriptor<?> forDesc = beanProp.getTargetDescriptor();

		String propName = getPropName(beanProp, parent);

		String destAlias = beanProp.getTableJoin().getForeignTableAlias();
		String foreignAlias = getChildAlias(parent, destAlias);

		DeployPropertyRequest deployProps = deployPropertyFactory.createDeployProperties(forDesc,propName, foreignAlias);

		String parentTableAlias = parent.getTableAlias();
		TableJoin tableJoin = beanProp.getTableJoin().createWithAlias(parentTableAlias, foreignAlias);

		JoinNodeBean child = new JoinNodeBean(parent, tableJoin, forDesc, propName, deployProps, beanProp);
		
		request.setMaxObjectDepth(child.getMaxObjectDepth());

		return child;
	}

	/**
	 * Add a child node. Depth first.
	 */
	private JoinNode addEmbeddedChild(JoinTreeRequest request, JoinNode parent,
			BeanPropertyAssocOne<?> beanProp) {

		BeanDescriptor<?> forDesc = beanProp.getTargetDescriptor();

		String propName = getPropName(beanProp, parent);

		JoinNodeEmbeddedBean child = new JoinNodeEmbeddedBean(parent, forDesc, propName,beanProp);
		
		return child;
	}
	
	/**
	 * Prepend the parent table alias IF we are deeper than the first level.
	 * Make sure this derived alias is not a keyword
	 */
	private String getChildAlias(JoinNode parent, String tableAlias) {
		String alias = getChildAliasAttempt(parent, tableAlias);
		if (!SqlReservedWords.isKeyword(alias)){
			return alias;
		}
		// append a trailing underscore to ensure that the
		// table alias is not a reserved word (no SQL reserved 
		// words end in an underscore)
		return alias+"_";
	}
	
	/**
	 * Prepend the parent table alias IF we are deeper than the first level.
	 */
	private String getChildAliasAttempt(JoinNode parent, String tableAlias) {
		if (!parent.isRoot()){
			// prepend the parent table alias
			return parent.getTableAlias()+tableAlias;
			
		} else {
			// the tableAlias for all joins at the top level 
			// should be unique and already determined to be
			// safe (aka single character - not a keyword)
			return tableAlias;
		}
	}
	
	/**
	 * Return a property name prefix for the properties of a given assoc One or
	 * Many bean.
	 * <p>
	 * This is like an Expression Language (EL) type full dot notation property
	 * name
	 * </p>
	 */
	private String getPropName(BeanPropertyAssoc<?> prop, JoinNode parent) {
		String propName = prop.getName();
		if (parent.propertyPrefix != null && parent.propertyPrefix.length() > 0) {
			propName = parent.propertyPrefix + "." + propName;
		}
		return propName;
	}

}
