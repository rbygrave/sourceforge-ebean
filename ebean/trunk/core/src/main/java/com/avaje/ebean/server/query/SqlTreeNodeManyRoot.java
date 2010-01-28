package com.avaje.ebean.server.query;

import java.sql.SQLException;
import java.util.List;

import com.avaje.ebean.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebean.server.deploy.DbReadContext;
import com.avaje.ebean.server.deploy.DbSqlContext;

public final class SqlTreeNodeManyRoot extends SqlTreeNodeBean {

	public SqlTreeNodeManyRoot(String prefix, BeanPropertyAssocMany<?> prop, SqlTreeProperties props, List<SqlTreeNode> myList) {
		super(prefix, prop, prop.getTargetDescriptor(), props, myList, true);
	}

    @Override
	protected void postLoad(DbReadContext cquery, Object loadedBean, Object id) {
    
    	// put the localBean into the manyValue so that it
    	// is added to the collection/map
    	cquery.setLoadedManyBean(loadedBean);
	}

    @Override
	public void load(DbReadContext cquery, Object parentBean, int parentState) throws SQLException {
		// pass in null for parentBean because the localBean
    	// that is built is added to a collection rather than
    	// being set to the parentBean directly
    	super.load(cquery, null, parentState);
    }

    /**
     * Force outer join for everything after the many property.
     */
	@Override
	public void appendFrom(DbSqlContext ctx, boolean forceOuterJoin) {
		super.appendFrom(ctx, true);
	}
    
    
}
