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
package org.avaje.ebean.server.query;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.avaje.ebean.bean.BeanController;
import org.avaje.ebean.bean.EntityBean;
import org.avaje.ebean.bean.EntityBeanIntercept;
import org.avaje.ebean.server.core.TransactionContextClass;
import org.avaje.ebean.server.deploy.BeanDescriptor;
import org.avaje.ebean.server.deploy.BeanProperty;
import org.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import org.avaje.ebean.server.deploy.BeanPropertyAssocOne;
import org.avaje.ebean.server.deploy.DbReadContext;
import org.avaje.ebean.server.deploy.DbSqlContext;
import org.avaje.ebean.server.deploy.InheritInfo;
import org.avaje.ebean.server.deploy.TableJoin;
import org.avaje.ebean.server.deploy.id.IdBinder;
import org.avaje.ebean.server.deploy.jointree.JoinNode;

/**
 * Normal bean included in the query.
 */
public class SqlTreeNodeBean implements SqlTreeNode {

	private static final SqlTreeNode[] NO_CHILDREN = new SqlTreeNode[0];

	final BeanDescriptor desc;
		
	final IdBinder idBinder;

	/**
	 * The children which will be other SelectBean or SelectProxyBean.
	 */
	final SqlTreeNode[] children;

	final boolean readOnly;

	/**
	 * Set to true if this is a partial object fetch.
	 */
	final boolean partialObject;
	
	/**
	 * The set of properties explicitly included in the query.
	 * We actually add the manyProp names to this as they are
	 * references/proxies we add via createListProxies().
	 */
	final Set<String> partialProps;
	
	/**
	 * The hash of the partialProps (calculate once).
	 */
	final int partialHash;
	
	final BeanProperty[] properties;
	
	final JoinNode node;
	
	/**
	 * Extra where clause added by Where annotation on associated many.
	 */
	final String extraWhere;
	
	final String tableAlias;

	final BeanPropertyAssocOne nodeBeanProp;

	final TableJoin[] tableJoins;

	/**
	 * False if report bean and has no id property.
	 */
	final boolean readId;
	
	final InheritInfo inheritInfo;
	
	/**
	 * Create with the appropriate node.
	 */
	public SqlTreeNodeBean(JoinNode joinNode, SqlTreeProperties props, List<SqlTreeNode> myChildren, boolean withId) {

		node = joinNode;
		nodeBeanProp = node.getBeanProp();
		tableAlias = node.getTableAlias();
		desc = node.getBeanDescriptor();
		inheritInfo = desc.getInheritInfo();
		extraWhere = joinNode.getExtraWhere();
		
		idBinder = desc.getIdBinder();
		
		// the bean has an Id property and we want to use it
		readId = withId && (desc.propertiesId().length > 0);
		
		tableJoins = props.getTableJoins();
		
		partialObject = props.isPartialObject();
		partialProps = props.getIncludedProperties();
		partialHash = partialObject ? partialProps.hashCode() : 0;
		
		readOnly = props.isReadOnly();
		
		properties = props.getProps();
		
		if (myChildren == null) {
			children = NO_CHILDREN;
		} else {
			children = myChildren.toArray(new SqlTreeNode[myChildren.size()]);
		}
	}

	protected void postLoad(DbReadContext cquery, EntityBean loadedBean, Object id) {
	}

	/**
	 * read the properties from the resultSet.
	 */
	public void load(DbReadContext ctx, EntityBean parentBean) throws SQLException {

		// bean already existing in the persistence context
		EntityBean contextBean = null;
		
		Class<?> localType;
		BeanDescriptor localDesc;
		IdBinder localIdBinder;
		EntityBean localBean;
		if (inheritInfo != null){
			InheritInfo localInfo = inheritInfo.readType(ctx);
			localBean = localInfo.createEntityBean();
			localType = localInfo.getType();
			localIdBinder = localInfo.getIdBinder();
			localDesc = localInfo.getBeanDescriptor();
	        
		} else {
			localType = null;
			localDesc = desc;
			localBean = desc.createEntityBean();
			localIdBinder = idBinder;
		}



		Object id = null;
		if (!readId){
			// report type bean... or perhaps excluding the id for SqlSelect?
			
		} else {
			id = localIdBinder.readSet(ctx, localBean);
			if (id == null){
				// bean must be null...
				localBean = null;
			} else {
				// check the TransactionContext to see if the bean already exists
				TransactionContextClass classContext = ctx.getClassContext(localBean.getClass());

				contextBean = classContext.get(id);
				if (contextBean != null && !contextBean._ebean_getIntercept().isReference()) {
					// bean already exists in transaction context
					localBean = null;

				} else {
					// load the bean into the TransactionContext
					// it may be accessed by other beans in the same jdbc
					// resultSet row (detail beans in master/detail query).
					classContext.put(id, localBean);
					contextBean = localBean;
				}
			}
		} 

		// only required for profiling
		ctx.setCurrentJoinNode(node);
		
		if (inheritInfo == null){
			// normal behaviour with no inheritance
			for (int i = 0, x = properties.length; i < x; i++) {
				properties[i].readSet(ctx, localBean, localType);
			}
			
		} else {
			// take account of inheritance and due to subclassing approach
			// need to get a 'local' version of the property
			for (int i = 0, x = properties.length; i < x; i++) {
				// get a local version of the BeanProperty
				String propName = properties[i].getName();
				BeanProperty p = localDesc.getBeanProperty(propName);
				if (p != null){
					p.readSet(ctx, localBean, localType);
				} else {
					properties[i].read(ctx);
				}
			}	
		} 
		
		for (int i = 0, x = tableJoins.length; i < x; i++) {
			tableJoins[i].readSet(ctx, localBean, localType);
		}

		// recursively continue reading...
		for (int i = 0; i < children.length; i++) {
			// read each child... and let them set their
			// values back to this localBean
			children[i].load(ctx, localBean);
		}

		if (localBean != null) {

			Set<String> includedProps = null;
			if (partialObject){
				// merge the explicit partialProps with the implicitly added
				// list proxies (that are added by createListProxies()) to get
				// the full set of 'loaded' properties for this bean.
				includedProps = LoadedPropertiesCache.get(partialHash, partialProps, desc);
			}
			
			createListProxies(localDesc, ctx, localBean, ctx.getManyProperty());

			BeanController controller = localDesc.getBeanController();
			if (controller != null){
				controller.postLoad(localBean, includedProps);
			}

			EntityBeanIntercept ebi = localBean._ebean_getIntercept();
			ebi.setLoaded();
			if (includedProps != null) {
				ebi.setLoadedProps(includedProps);
			}
			if (readOnly) {
				ebi.setReadOnly(true);
			}
			
			if (ctx.isAutoFetchProfiling()){
				// collect profile info for this node...
				// after children so explicit node
				ctx.profileBean(ebi, null, node);
			}
			
		}
		if (parentBean != null && contextBean != null) {
			// set this back to the parentBean
			nodeBeanProp.setValue(parentBean, contextBean);
		}

		// return the contextBean which is either the localBean
		// read from the resultSet and put into the context OR
		// the 'matching' bean that already existed in the context
		postLoad(ctx, contextBean, id);
	}

	/**
	 * Create lazy loading proxies for the Many's except for the one that is
	 * included in the actual query.
	 */
	private void createListProxies(BeanDescriptor localDesc, DbReadContext ctx, EntityBean localBean, 
			BeanPropertyAssocMany fetchedMany) {

		// load the List/Set/Map proxy objects (deferred fetching of lists)
		BeanPropertyAssocMany[] manys = localDesc.propertiesMany();
		for (int i = 0; i < manys.length; i++) {

			if (fetchedMany != null && fetchedMany.equals(manys[i])) {
				// this many property is included in the query...
				// it is being loaded with real row data (result[1])
			} else {
				// create a proxy for the many (deferred fetching)
				if (ctx.isAutoFetchProfiling()){
					manys[i].createReference(localBean, ctx.createAutoFetchNode(manys[i].getName(), node));					
				} else {
					manys[i].createReference(localBean, null);
				}
			}
		}
	}

	/**
	 * Append the property columns to the buffer.
	 */
	public void appendSelect(DbSqlContext ctx) {
		
		ctx.pushJoinNode(node);
		ctx.pushTableAlias(tableAlias);
		
		if (!node.isRoot()) {
			ctx.append(NEW_LINE).append("        ");
		}
				
		if (inheritInfo != null){
			ctx.append(COMMA);
			ctx.append(tableAlias);
			ctx.append(PERIOD);
			ctx.append(inheritInfo.getDiscriminatorColumn());
		}
		
		if (readId) {
			appendSelect(ctx, idBinder.getProperties());
		}
		appendSelect(ctx, properties);
		appendSelectTableJoins(ctx);

		for (int i = 0; i < children.length; i++) {
			// read each child... and let them set their
			// values back to this localBean
			children[i].appendSelect(ctx);
		}
		
		ctx.popTableAlias();
		ctx.popJoinNode();
	}

	private void appendSelectTableJoins(DbSqlContext ctx) {

		for (int i = 0; i < tableJoins.length; i++) {
			TableJoin join = tableJoins[i];

			String alias = join.getForeignTableAlias();

			ctx.pushTableAlias(alias);
			join.appendSelect(ctx);
			ctx.popTableAlias();
		}
	}

	/**
	 * Append the properties to the buffer.
	 */
	private void appendSelect(DbSqlContext ctx, BeanProperty[] props) {

		for (int i = 0; i < props.length; i++) {
			props[i].appendSelect(ctx);
		}
	}
	
	
	public void appendWhere(StringBuilder sb) {

		if (inheritInfo != null) {
			if (inheritInfo.isRoot()) {
				// at root of hierarchy so don't bother
				// adding a where clause because we want
				// all the types...
			} else {
				// restrict to this type and 
				// sub types of this type.
				if (sb.length() > 0){
					sb.append(" and");
				} 
				sb.append(" ").append(tableAlias).append(".");
				sb.append(inheritInfo.getWhere()).append(" ");
			}
		}
		if (extraWhere != null){
			if (sb.length() > 0){
				sb.append(" and");
			} 
			sb.append(" ").append(extraWhere).append(" ");
		}
		
		for (int i = 0; i < children.length; i++) {
			// recursively add to the where clause any
			// fixed predicates (extraWhere etc)
			children[i].appendWhere(sb);
		}
	}
	
	/**
	 * Append to the FROM clause for this node.
	 */
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {

		ctx.pushJoinNode(node);
		ctx.pushTableAlias(node.getTableAlias());
		
		appendFromBaseTable(ctx, forceOuterJoin);
		
		// NB: properties based on secondary tables are not 
		// included for 'partial' object queries...
		for (int i = 0, x = tableJoins.length; i < x; i++) {
		    tableJoins[i].addJoin(forceOuterJoin, node, ctx);
		}
		
		for (int i = 0; i < properties.length; i++) {
			// usually nothing... except for 1-1 Exported
			properties[i].appendFrom(ctx, forceOuterJoin);
		}
		
		for (int i = 0; i < children.length; i++) {
			children[i].appendFrom(ctx, forceOuterJoin);
		}
		
		ctx.popTableAlias();
		ctx.popJoinNode();
	}
	
	/**
	 * Join to base table for this node. This includes a join to
	 * the intersection table if this is a ManyToMany node.
	 */
	public void appendFromBaseTable(DbSqlContext ctx, boolean forceOuterJoin) {
		
        if (node.isManyJoin()) {
            BeanPropertyAssocMany manyProp = node.getManyProp();
            if (manyProp.isManyToMany()) {
            	// add ManyToMany join
                TableJoin manyToManyJoin = manyProp.getIntersectionTableJoin();
                manyToManyJoin.addJoin(forceOuterJoin, node, ctx);
            }
        }
        
        node.addJoin(forceOuterJoin, ctx);
	}

    
	/**
	 * Summary description.
	 */
	public String toString() {
		return "BeanItem: " + desc;
	}
}
