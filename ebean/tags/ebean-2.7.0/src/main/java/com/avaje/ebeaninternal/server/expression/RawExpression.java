package com.avaje.ebeaninternal.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.api.SpiLuceneExpr;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;


class RawExpression implements SpiExpression {

	private static final long serialVersionUID = 7973903141340334606L;
	
	private final String sql;

	private final Object[] values;
	
	RawExpression(String sql, Object[] values) {
		this.sql = sql;
		this.values = values;
	}
		
	public boolean isLuceneResolvable(LuceneResolvableRequest req) {
        return false;
    }
	
    public SpiLuceneExpr createLuceneExpr(SpiExpressionRequest request) {
        return null;
    }

    public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
		
	}
	
	public void addBindValues(SpiExpressionRequest request) {
	    if (values != null){
    		for (int i = 0; i < values.length; i++) {
    			request.addBindValue(values[i]);
    		}
	    }
	}
	
	public void addSql(SpiExpressionRequest request) {
		request.append(sql);
	}
	
	/**
	 * Based on the sql.
	 */
	public int queryAutoFetchHash() {
		int hc = RawExpression.class.getName().hashCode();
		hc = hc * 31 + sql.hashCode();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		return sql.hashCode();
	}
}
