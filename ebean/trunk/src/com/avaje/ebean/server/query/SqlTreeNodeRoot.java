package com.avaje.ebean.server.query;

import java.util.List;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.jointree.JoinNode;

/**
 * Represents the root node of the Sql Tree.
 */
public final class SqlTreeNodeRoot extends SqlTreeNodeBean {

	final TableJoin includeJoin;
	
	/**
	 * Specify for SqlSelect to include an Id property or not.
	 */
	public SqlTreeNodeRoot(JoinNode node, SqlTreeProperties props, List<SqlTreeNode> myList, boolean withId, TableJoin includeJoin) {
		super(node, props, myList, withId);
		this.includeJoin = includeJoin;
	}

	/**
	 * Normal constructor.
	 */
	public SqlTreeNodeRoot(JoinNode node, SqlTreeProperties props, List<SqlTreeNode> myList, TableJoin includeJoin) {
		super(node, props, myList, true);
		this.includeJoin = includeJoin;
	}
	
	@Override
	protected void postLoad(DbReadContext cquery, EntityBean loadedBean, Object id) {
		
		// set the current bean with id...
		cquery.setLoadedBean(loadedBean, id);
	}
	
	/**
	 * For the root node there is no join type or on clause etc.
	 */
	@Override
	public void appendFromBaseTable(DbSqlContext ctx, boolean forceOuterJoin) {
		ctx.append(desc.getBaseTable());
        if (desc.getBaseTableAlias() != null) {
        	ctx.append(" ").append(desc.getBaseTableAlias());
        }
        if (includeJoin != null){
        	includeJoin.addJoin(forceOuterJoin, node, ctx);
        }
	}
	
}
