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

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebean.internal.PersistenceContext;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.deploy.BeanPropertyAssoc;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.InheritInfo;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.id.IdBinder;
import com.avaje.ebean.server.lib.util.StringHelper;

/**
 * Normal bean included in the query.
 */
public class SqlTreeNodeBean implements SqlTreeNode {

	private static final SqlTreeNode[] NO_CHILDREN = new SqlTreeNode[0];

	final BeanDescriptor<?> desc;
		
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
	
	/**
	 * Extra where clause added by Where annotation on associated many.
	 */
	final String extraWhere;
	
	final BeanPropertyAssoc<?> nodeBeanProp;

	final TableJoin[] tableJoins;

	/**
	 * False if report bean and has no id property.
	 */
	final boolean readId;
	
	final InheritInfo inheritInfo;
	
	final String prefix;

	public SqlTreeNodeBean(String prefix, BeanPropertyAssoc<?> beanProp, 
			SqlTreeProperties props, List<SqlTreeNode> myChildren, boolean withId) {
		this(prefix, beanProp, beanProp.getTargetDescriptor(),props, myChildren, withId);
	}
	
	/**
	 * Create with the appropriate node.
	 */
	public SqlTreeNodeBean(String prefix, BeanPropertyAssoc<?> beanProp, BeanDescriptor<?> desc, 
			SqlTreeProperties props, List<SqlTreeNode> myChildren, boolean withId) {

		this.prefix = prefix;
		this.nodeBeanProp = beanProp;
		this.desc = desc;
		this.inheritInfo = desc.getInheritInfo();
		this.extraWhere = (beanProp == null) ? null : beanProp.getExtraWhere();
		
		this.idBinder = desc.getIdBinder();
		
		// the bean has an Id property and we want to use it
		this.readId = withId && (desc.propertiesId().length > 0);
		
		this.tableJoins = props.getTableJoins();
		
		this.partialObject = props.isPartialObject();
		this.partialProps = props.getIncludedProperties();
		this.partialHash = partialObject ? partialProps.hashCode() : 0;
		
		this.readOnly = props.isReadOnly();
		
		this.properties = props.getProps();
		
		if (myChildren == null) {
			children = NO_CHILDREN;
		} else {
			children = myChildren.toArray(new SqlTreeNode[myChildren.size()]);
		}
	}

	protected void postLoad(DbReadContext cquery, Object loadedBean, Object id) {
	}

	/**
	 * read the properties from the resultSet.
	 */
	public void load(DbReadContext ctx, EntityBean parentBean) throws SQLException {

		// bean already existing in the persistence context
		Object contextBean = null;
		
		Class<?> localType;
		BeanDescriptor<?> localDesc;
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
				PersistenceContext persistCtx = ctx.getPersistenceContext();

//				contextBean = classContext.getOrSet(id, localBean);
//				if (contextBean != null){
//					// bean already exists in transaction context
//					localBean = null;
//				} else {
//					contextBean = localBean;
//				}
				
				contextBean = persistCtx.get(localBean.getClass(), id);
				if (contextBean != null && !((EntityBean)contextBean)._ebean_getIntercept().isReference()) {
					// bean already exists in transaction context
					localBean = null;

				} else {
					// load the bean into the TransactionContext
					// it may be accessed by other beans in the same jdbc
					// resultSet row (detail beans in master/detail query).
					persistCtx.set(id, localBean);
					contextBean = localBean;
				}
			}
		} 

		// only required for profiling
		ctx.setCurrentPrefix(prefix);
		
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

			localDesc.postLoad(localBean, includedProps);

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
				ctx.profileBean(ebi, null, prefix);
			}
			
		}
		if (parentBean != null && contextBean != null) {
			// set this back to the parentBean
			nodeBeanProp.setValue(parentBean, contextBean);
		}

		if (!readId){
			// a bean with no Id (never found in context)
			postLoad(ctx, localBean, id);
			
		} else {
			// return the contextBean which is either the localBean
			// read from the resultSet and put into the context OR
			// the 'matching' bean that already existed in the context
			postLoad(ctx, contextBean, id);
		}
	}

	/**
	 * Create lazy loading proxies for the Many's except for the one that is
	 * included in the actual query.
	 */
	private void createListProxies(BeanDescriptor<?> localDesc, DbReadContext ctx, EntityBean localBean, 
			BeanPropertyAssocMany<?> fetchedMany) {

		// load the List/Set/Map proxy objects (deferred fetching of lists)
		BeanPropertyAssocMany<?>[] manys = localDesc.propertiesMany();
		for (int i = 0; i < manys.length; i++) {

			if (fetchedMany != null && fetchedMany.equals(manys[i])) {
				// this many property is included in the query...
				// it is being loaded with real row data (result[1])
			} else {
				// create a proxy for the many (deferred fetching)
				if (ctx.isAutoFetchProfiling()){
					manys[i].createReference(localBean, ctx.createAutoFetchNode(manys[i].getName(), prefix));					
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
		
		ctx.pushJoin(prefix);
		ctx.pushTableAlias(prefix);

		if (nodeBeanProp != null) {
			ctx.append(NEW_LINE).append("        ");
		}
				
		if (inheritInfo != null){
			ctx.append(COMMA);
			ctx.append(ctx.getTableAlias(prefix));
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
		ctx.popJoin();
	}

	private void appendSelectTableJoins(DbSqlContext ctx) {

		String baseAlias = ctx.getTableAlias(prefix);
		
		for (int i = 0; i < tableJoins.length; i++) {
			TableJoin join = tableJoins[i];

			String alias = baseAlias+i;

			ctx.pushSecondaryTableAlias(alias);
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
	
	
	public void appendWhere(DbSqlContext ctx) {

		if (inheritInfo != null) {
			if (inheritInfo.isRoot()) {
				// at root of hierarchy so don't bother
				// adding a where clause because we want
				// all the types...
			} else {
				// restrict to this type and 
				// sub types of this type.
				if (ctx.length() > 0){
					ctx.append(" and");
				} 
				ctx.append(" ").append(ctx.getTableAlias(prefix)).append(".");//tableAlias
				ctx.append(inheritInfo.getWhere()).append(" ");
			}
		}
		if (extraWhere != null){
			if (ctx.length() > 0){
				ctx.append(" and");
			} 
			String ta = ctx.getTableAlias(prefix);
			String ew = StringHelper.replaceString(extraWhere, "${ta}", ta);
			ctx.append(" ").append(ew).append(" ");
		}
		
		for (int i = 0; i < children.length; i++) {
			// recursively add to the where clause any
			// fixed predicates (extraWhere etc)
			children[i].appendWhere(ctx);
		}
	}
	
	/**
	 * Append to the FROM clause for this node.
	 */
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {

		ctx.pushJoin(prefix);
		ctx.pushTableAlias(prefix);

		appendFromBaseTable(ctx, forceOuterJoin);
		
//		// NB: properties based on secondary tables are not 
//		// included for 'partial' object queries...
//		String baseAlias = ctx.getTableAlias(prefix);
//		for (int i = 0, x = tableJoins.length; i < x; i++) {
//			String alias = baseAlias+i;
//		    tableJoins[i].addJoin(forceOuterJoin, alias, baseAlias, ctx);
//		}
		
		for (int i = 0; i < properties.length; i++) {
			// usually nothing... except for 1-1 Exported
			properties[i].appendFrom(ctx, forceOuterJoin);
		}
		
		for (int i = 0; i < children.length; i++) {
			children[i].appendFrom(ctx, forceOuterJoin);
		}
		
		ctx.popTableAlias();
		ctx.popJoin();
	}
	
	/**
	 * Join to base table for this node. This includes a join to
	 * the intersection table if this is a ManyToMany node.
	 */
	public void appendFromBaseTable(DbSqlContext ctx, boolean forceOuterJoin) {
		
		boolean manyToMany = false;
		
		if (nodeBeanProp instanceof BeanPropertyAssocMany<?>){
			BeanPropertyAssocMany<?> manyProp = (BeanPropertyAssocMany<?>)nodeBeanProp;
			if (manyProp.isManyToMany()){
				
				manyToMany = true;
				
				String alias = ctx.getTableAlias(prefix);
				String[] split = SplitName.split(prefix);
				String parentAlias = ctx.getTableAlias(split[0]);
				String alias2 = alias+"z_";
				
				TableJoin manyToManyJoin = manyProp.getIntersectionTableJoin();
				manyToManyJoin.addJoin(forceOuterJoin, parentAlias, alias2, ctx);
				
				nodeBeanProp.addJoin(forceOuterJoin, alias2, alias, ctx);
			}
		}
        
		if (!manyToMany){
			nodeBeanProp.addJoin(forceOuterJoin, prefix, ctx);
		}
	}

    
	/**
	 * Summary description.
	 */
	public String toString() {
		return "SqlTreeNodeBean: " + desc;
	}
}
