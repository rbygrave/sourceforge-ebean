package com.avaje.ebeaninternal.server.query;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssoc;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.deploy.DbReadContext;
import com.avaje.ebeaninternal.server.deploy.DbSqlContext;
import com.avaje.ebeaninternal.server.deploy.TableJoin;

/**
 * The purpose is to add an extra join to the query.
 * <p>
 * This is used to support the where clause or order by clause that refers
 * to properties that are NOT included in the select. To support the where clause
 * etc in this case we must add an extra join.
 * </p>
 */
public class SqlTreeNodeExtraJoin implements SqlTreeNode {

	
	final BeanPropertyAssoc<?> assocBeanProperty;
	
	final String prefix;
	
	final boolean manyJoin;
	
	List<SqlTreeNodeExtraJoin> children;
	
	public SqlTreeNodeExtraJoin(String prefix, BeanPropertyAssoc<?> assocBeanProperty) {
		this.prefix = prefix;
		this.assocBeanProperty = assocBeanProperty;
		this.manyJoin = assocBeanProperty instanceof BeanPropertyAssocMany<?>;
	}
	
	
	
	public void buildSelectExpressionChain(List<String> selectChain) {
	    // nothing to add
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
		return prefix;
	}
	
	public void addChild(SqlTreeNodeExtraJoin child){
		if (children == null){
			children = new ArrayList<SqlTreeNodeExtraJoin>();
		}
		children.add(child);
	}
	
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		
		boolean manyToMany = false;
		
		if (assocBeanProperty instanceof BeanPropertyAssocMany<?>){
			BeanPropertyAssocMany<?> manyProp = (BeanPropertyAssocMany<?>)assocBeanProperty;
			if (manyProp.isManyToMany()){
				
				manyToMany = true;
				
				String alias = ctx.getTableAlias(prefix);
				String[] split = SplitName.split(prefix);
				String parentAlias = ctx.getTableAlias(split[0]);
				String alias2 = alias+"z_";
				
				TableJoin manyToManyJoin = manyProp.getIntersectionTableJoin();
				manyToManyJoin.addJoin(forceOuterJoin, parentAlias, alias2, ctx);
				
				assocBeanProperty.addJoin(forceOuterJoin, alias2, alias, ctx);
			}
		}
        
		if (!manyToMany){
			assocBeanProperty.addJoin(forceOuterJoin, prefix, ctx);
		}
        
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
	public void appendWhere(DbSqlContext ctx) {
	}

	/**
	 * Does nothing.
	 */
	public void load(DbReadContext ctx, Object parentBean, int parentState) throws SQLException {
	}

	
	
}
