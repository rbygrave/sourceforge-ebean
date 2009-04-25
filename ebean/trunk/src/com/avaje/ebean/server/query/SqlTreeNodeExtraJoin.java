package com.avaje.ebean.server.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;
import com.avaje.ebean.server.deploy.TableJoin;
import com.avaje.ebean.server.deploy.jointree.JoinNode;

/**
 * The purpose is to add an extra join to the query.
 * <p>
 * This is used to support the where clause or order by clause that refers
 * to properties that are NOT included in the select. To support the where clause
 * etc in this case we must add an extra join.
 * </p>
 */
public class SqlTreeNodeExtraJoin implements SqlTreeNode {

	
	final JoinNode node;
	
	final boolean manyJoin;
	
	List<SqlTreeNodeExtraJoin> children;
	
	public SqlTreeNodeExtraJoin(JoinNode node) {
		this.node = node;
		this.manyJoin = node.isManyJoin();
	}
	
	/**
	 * Return true if the extra join is a many join.
	 * <p>
	 * This means we need to add distinct to the sql query.
	 * </p>
	 */
	public boolean isManyJoin() {
		return manyJoin;
	}


	public String getName() {
		return node.getName();
	}
	
	public void addChild(SqlTreeNodeExtraJoin child){
		if (children == null){
			children = new ArrayList<SqlTreeNodeExtraJoin>();
		}
		children.add(child);
	}
	
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		
        if (node.isManyJoin()) {
            BeanPropertyAssocMany<?> manyProp = node.getManyProp();
            if (manyProp.isManyToMany()) {
            	// add ManyToMany join
                TableJoin manyToManyJoin = manyProp.getIntersectionTableJoin();
                manyToManyJoin.addJoin(forceOuterJoin, node, ctx);
            }
        }
        
        node.addJoin(forceOuterJoin, ctx);
        
        if (children != null){
        	
        	if (manyJoin){
        		// make sure all decendants use OUTER JOIN
        		forceOuterJoin = true;
        	}
        	
        	for (int i = 0; i < children.size(); i++) {
        		SqlTreeNodeExtraJoin child = children.get(i);
        		child.appendFrom(ctx, forceOuterJoin);
			}
        }
	}

	/**
	 * Does nothing.
	 */
	public void appendSelect(DbSqlContext ctx) {		
	}

	/**
	 * Does nothing.
	 */
	public void appendWhere(StringBuilder sb) {
	}

	/**
	 * Does nothing.
	 */
	public void load(DbReadContext ctx, EntityBean parentBean) throws SQLException {
	}

	
	
}
