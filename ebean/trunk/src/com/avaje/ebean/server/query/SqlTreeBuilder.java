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
package com.avaje.ebean.server.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.query.OrmQuery;
import com.avaje.ebean.query.OrmQueryDetail;
import com.avaje.ebean.query.OrmQueryProperties;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.deploy.jointree.JoinNode;
import com.avaje.ebean.server.deploy.jointree.JoinTree;
import com.avaje.ebean.server.deploy.jointree.JoinNode.Type;
import com.avaje.ebean.util.Message;
import com.avaje.lib.log.LogFactory;

/**
 * Factory for SqlSelectClause.
 */
public class SqlTreeBuilder {

	private static final Logger logger = LogFactory.get(SqlTreeBuilder.class);

	final OrmQuery<?> query;

	final OrmQueryDetail queryDetail;

	final JoinTree joinTree;

	final SqlTree clause = new SqlTree();

	final String tableAliasPlaceHolder;
	
	final String columnAliasPrefix;

	final StringBuilder summary = new StringBuilder();

	final CQueryPredicates predicates;

	/**
	 * Property if resultSet contains master and detail rows.
	 */
	BeanPropertyAssocMany manyProperty;


	/**
	 * The predicates are used to determine if 'extra' joins are required to
	 * support the where and/or order by clause. If so these extra joins are
	 * added to the root node.
	 */
	public SqlTreeBuilder(String tableAliasPlaceHolder, String columnAliasPrefix, OrmQuery<?> query, 
			JoinTree joinTree, CQueryPredicates predicates) {
		
		this.tableAliasPlaceHolder = tableAliasPlaceHolder;
		this.columnAliasPrefix = columnAliasPrefix;
		this.query = query;
		this.queryDetail = query.getDetail();
		this.joinTree = joinTree;
		this.predicates = predicates;
	}

	/**
	 * Build based on the includes and using the BeanJoinTree.
	 */
	public SqlTree build() {

		JoinNode root = joinTree.getRoot();
		BeanDescriptor desc = root.getBeanDescriptor();
		summary.append("[");
		if (desc.isTableGenerated()) {
			summary.append(desc.getBaseTable());

		} else {
			summary.append(desc.getBeanType().getName());
		}

		// build the appropriate chain of SelectAdapter's
		build(root);

		// build the actual String
		SqlTreeNode rootNode = clause.getRootNode();

		clause.setSelectSql(buildSelectClause(rootNode));
		clause.setFromSql(buildFromClause(rootNode));
		clause.setInheritanceWhereSql(buildWhereClause(rootNode));

		summary.append("]");
		if (query.isAutoFetchTuned()){
			summary.append(" autoFetchTuned[true]");
		}

		clause.setIncludes(queryDetail.getIncludes());
		clause.setSummary(summary.toString());
		clause.setManyProperty(manyProperty);
		return clause;
	}

	private String buildSelectClause(SqlTreeNode rootNode) {

		DefaultDbSqlContext ctx = new DefaultDbSqlContext(tableAliasPlaceHolder, columnAliasPrefix);

		rootNode.appendSelect(ctx);

		String selectSql = ctx.toString();

		// trim off the first comma
		if (selectSql.length() >= SqlTreeNode.COMMA.length()) {
			selectSql = selectSql.substring(SqlTreeNode.COMMA.length());
		}

		return selectSql;
	}

	private String buildWhereClause(SqlTreeNode rootNode) {

		StringBuilder sb = new StringBuilder();

		rootNode.appendWhere(sb);

		return sb.toString();
	}

	private String buildFromClause(SqlTreeNode rootNode) {

		DefaultDbSqlContext ctx = new DefaultDbSqlContext(tableAliasPlaceHolder, columnAliasPrefix);

		rootNode.appendFrom(ctx, false);

		return ctx.toString();
	}

	private void build(JoinNode root) {

		SqlTreeNode selectRoot = buildSelectChain(root, null);
		clause.setRootNode(selectRoot);
	}

	/**
	 * Recursively build the query tree depending on what leaves in the tree
	 * should be included.
	 */
	private SqlTreeNode buildSelectChain(JoinNode node, List<SqlTreeNode> joinList) {

		if (!isInclude(node)) {
			return null;
		}

		List<SqlTreeNode> myJoinList = new ArrayList<SqlTreeNode>();

		JoinNode[] children = node.children();
		for (int i = 0; i < children.length; i++) {
			buildSelectChain(children[i], myJoinList);
		}

		SqlTreeNode selectNode = buildNode(node, myJoinList);
		if (joinList != null) {
			joinList.add(selectNode);
		}
		return selectNode;
	}

	private SqlTreeNode buildNode(JoinNode node, List<SqlTreeNode> myList) {

		OrmQueryProperties queryProps = queryDetail.getChunk(node.getPropertyPrefix(), false);

		BeanDescriptor desc = node.getBeanDescriptor();

		SqlTreeProperties props = getBaseSelect(node, desc, queryProps);

		if (node.isRoot()) {
			buildExtraJoins(node, myList);
			return new SqlTreeNodeRoot(node, props, myList);

		} else if (node.isManyJoin()) {
			return new SqlTreeNodeManyRoot(node, props, myList);

		} else {
			return new SqlTreeNodeBean(node, props, myList, true);
		}
	}

	/**
	 * Build extra joins to support properties used in where clause but not
	 * already in select clause.
	 */
	private void buildExtraJoins(JoinNode root, List<SqlTreeNode> myList) {

		Set<String> predicateIncludes = predicates.getPredicateIncludes();
		if (predicateIncludes == null) {
			return;
		}
		IncludesDistiller extraJoinDistill = new IncludesDistiller(root, queryDetail.getIncludes(),
				predicateIncludes);

		Collection<SqlTreeNodeExtraJoin> extraJoins = extraJoinDistill.getExtraJoinRootNodes();
		if (extraJoins.isEmpty()) {

		} else {
			// add extra joins required to support predicates and/or order by
			// clause
			Iterator<SqlTreeNodeExtraJoin> it = extraJoins.iterator();
			while (it.hasNext()) {
				SqlTreeNodeExtraJoin extraJoin = it.next();
				myList.add(extraJoin);
				
				if (extraJoin.isManyJoin()){
					// as we are now going to join to the many then we need
					// to add the distinct to the sql query to stop duplicate
					// rows...
					query.setDistinct(true);
				}
				
			}
		}

	}

	private SqlTreeProperties getBaseSelectPartial(JoinNode node, BeanDescriptor desc,
			OrmQueryProperties queryProps) {

		SqlTreeProperties selectProps = new SqlTreeProperties();
		selectProps.setReadOnly(queryProps.isReadOnly());
		selectProps.setIncludedProperties(queryProps.getAllIncludedProperties());

		// add properties in the order in which they appear
		// in the query. Gives predictable sql/properties for
		// use with SqlSelect type queries.

		// Also note that this can include transient properties.
		// This makes sense for transient properties used to
		// hold sum() count() type values (with SqlSelect)
		Iterator<String> it = queryProps.getSelectProperties().iterator();
		while (it.hasNext()) {
			String propName = it.next();
			if (propName.length() > 0){
				BeanProperty p = desc.getBeanProperty(propName);
				if (p == null) {
					logger.log(Level.SEVERE, "property [" + propName + "] not found on " + desc
							+ " for query - excluding it.");
	
				} else if (p.isId()) {
					// do not include id
	
				} else if (p instanceof BeanPropertyAssoc) {
					// need to check if this property should be
					// excluded. This occurs when this property is
					// included as a bean join. With a bean join 
					// the property should be excluded as the bean
					// join has its own node in the SqlTree.
					if (!queryProps.isIncludedBeanJoin(p.getName())) {
						// include the property... which basically
						// means include the foreign key column(s)
						selectProps.add(p);
					}
				} else {
					selectProps.add(p);
				}
			}
		}

		return selectProps;
	}

	private SqlTreeProperties getBaseSelect(JoinNode node, BeanDescriptor desc,
			OrmQueryProperties queryProps) {

		boolean partial = queryProps != null && !queryProps.allProperties();
		if (partial) {
			return getBaseSelectPartial(node, desc, queryProps);
		}

		SqlTreeProperties selectProps = new SqlTreeProperties();

		// normal simple properties of the bean
		selectProps.add(desc.propertiesBaseScalar());
		selectProps.add(desc.propertiesEmbedded());

		BeanPropertyAssocOne[] propertiesOne = desc.propertiesOne();
		for (int i = 0; i < propertiesOne.length; i++) {
			if (queryProps != null && queryProps.isIncludedBeanJoin(propertiesOne[i].getName())) {
				// if it is a joined bean... then don't add the property
				// as it will have its own entire Node in the SqlTree
			} else {
				selectProps.add(propertiesOne[i]);
			}
		}

		selectProps.setTableJoins(desc.tableJoins());

		InheritInfo inheritInfo = desc.getInheritInfo();
		if (inheritInfo != null) {
			// add sub type properties
			inheritInfo.addChildrenProperties(selectProps);

		}
		return selectProps;
	}

	/**
	 * Return true if this many node should be included in the query.
	 */
	private boolean isIncludeMany(JoinNode node) {
		if (queryDetail.isFetchJoinsEmpty()) {
			return false;
		}

		String propName = node.getPropertyPrefix();

		if (queryDetail.includes(propName)) {
			// add the 'many' property to the baseProps list
			// as we are going to set/populate these many'ies
			// when reading the resultSet data
			// queryDetail.addBaseProperty(propName);

			if (node.getJoinDepth() != 1) {
				// many must be directly associated with root object
				String m = Message.msg("fetch.many.depth", node.getPropertyPrefix());
				logger.warning(m);
				return false;
			}
			if (manyProperty != null) {
				// only one many associated allowed to be included in fetch
				String m = Message.msg("fetch.many.one", node.getPropertyPrefix());
				logger.warning(m);
				return false;
			}

			manyProperty = node.getManyProp();
			summary.append("] +many[").append(node.getPropertyPrefix());
			return true;
		}
		return false;
	}

	/**
	 * Test to see if we are including this node into the query.
	 * <p>
	 * Return true if this node is FULLY included resulting in table join. If
	 * the node is not included but its parent has been included then a "bean
	 * proxy" is added and false is returned.
	 * </p>
	 */
	private boolean isIncludeBean(JoinNode node) {

		String prefix = node.getPropertyPrefix();
		String parentProp = node.getParent().getPropertyPrefix();
		String propertyName = node.getBeanProp().getName();
		if (queryDetail.includes(prefix)) {
			// explicitly included
			summary.append(", ").append(prefix);
			queryDetail.includeBeanJoin(parentProp, propertyName);
			return true;
		}

		return false;
	}

	/**
	 * Test to see if we are including this node into the query.
	 */
	private boolean isInclude(JoinNode node) {
		Type type = node.getType();
		switch (type) {
		case ROOT:
			return true;
			
		case EMBEDDED:
			return false;

		case BEAN:
			return isIncludeBean(node);

		case LIST:
			return isIncludeMany(node);

		default:
			throw new IllegalArgumentException("Unknown type " + type);
		}
	}

	/**
	 * Takes the select includes and the predicates includes and determines the
	 * extra joins required to support the predicates (that are not already
	 * supported by the select includes).
	 * <p>
	 * This returns ONLY the leaves. The joins for the leaves
	 * </p>
	 */
	private static class IncludesDistiller {

		final Set<String> selectIncludes;
		final Set<String> predicateIncludes;

		/**
		 * Contains the 'root' extra joins. We only return the roots back.
		 */
		final Map<String, SqlTreeNodeExtraJoin> joinRegister = new HashMap<String, SqlTreeNodeExtraJoin>();

		/**
		 * Register of all the extra join nodes.
		 */
		final Map<String, SqlTreeNodeExtraJoin> rootRegister = new HashMap<String, SqlTreeNodeExtraJoin>();

		final JoinNode root;

		IncludesDistiller(JoinNode root, Set<String> selectIncludes, Set<String> predicateIncludes) {
			this.root = root;
			this.selectIncludes = selectIncludes;
			this.predicateIncludes = predicateIncludes;
		}

		/**
		 * Build the collection of extra joins returning just the roots.
		 * <p>
		 * each root returned here could contain a little tree of joins. This
		 * follows the more natural pattern and allows for forcing outer joins
		 * from a join to a 'many' down through the rest of its tree.
		 * </p>
		 */
		Collection<SqlTreeNodeExtraJoin> getExtraJoinRootNodes() {

			String[] extras = findExtras();
			if (extras.length == 0) {
				return rootRegister.values();
			}

			// sort so we process only getting the leaves
			// excluding nodes between root and the leaf
			Arrays.sort(extras);

			// reverse order so get the leaves first...
			for (int i = 0; i < extras.length; i++) {
				createExtraJoin(extras[i]);
			}

			return rootRegister.values();
		}

		private void createExtraJoin(String includeProp) {

			SqlTreeNodeExtraJoin extraJoin = createJoinLeaf(includeProp);
			if (extraJoin != null){
				// add the extra join...
			
				// find root of this extra join... linking back to the
				// parents (creating the tree) as it goes.
				SqlTreeNodeExtraJoin root = findExtraJoinRoot(includeProp, extraJoin);

				// register the root because these are the only ones we
				// return back.
				rootRegister.put(root.getName(), root);
			}
		}

		/**
		 * Create a SqlTreeNodeExtraJoin, register and return it.
		 */
		private SqlTreeNodeExtraJoin createJoinLeaf(String propertyName) {
			JoinNode node = root.findChild(propertyName);
			if (node == null){
				// this can occur for master detail queries
				// with concatenated keys (so not an error now)
				return null;
			}
			if (node.isEmbeddedBean()){
				// no extra join required for embedded beans
				return null;
				
			} else {
				SqlTreeNodeExtraJoin extraJoin = new SqlTreeNodeExtraJoin(node);
				joinRegister.put(propertyName, extraJoin);
				return extraJoin;
			}
		}

		/**
		 * Find the root the this extra join tree.
		 * <p>
		 * This may need to create a parent join implicitly if a predicate join
		 * 'skips' a level. e.g. where details.user.id = 1 (maybe join to
		 * details is not specified and is implicitly created.
		 * </p>
		 */
		private SqlTreeNodeExtraJoin findExtraJoinRoot(String includeProp,
				SqlTreeNodeExtraJoin childJoin) {

			int dotPos = includeProp.lastIndexOf('.');
			if (dotPos == -1) {
				// no parent possible(parent is root)
				return childJoin;

			} else {
				// look in register ...
				String parentPropertyName = includeProp.substring(0, dotPos);
				if (selectIncludes.contains(parentPropertyName)) {
					// parent already handled by select
					return childJoin;
				}

				SqlTreeNodeExtraJoin parentJoin = joinRegister.get(parentPropertyName);
				if (parentJoin == null) {
					// we need to create this the parent implicitly...
					parentJoin = createJoinLeaf(parentPropertyName);
				}

				parentJoin.addChild(childJoin);
				return findExtraJoinRoot(parentPropertyName, parentJoin);
			}
		}

		/**
		 * Find the extra joins required by predicates and not already taken
		 * care of by the select.
		 */
		private String[] findExtras() {

			List<String> extras = new ArrayList<String>();

			for (String predProp : predicateIncludes) {
				if (!selectIncludes.contains(predProp)) {
					extras.add(predProp);
				}
			}
			return extras.toArray(new String[extras.size()]);
		}

	}
}
