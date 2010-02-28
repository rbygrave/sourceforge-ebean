package com.avaje.ebean.server.ldap.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;


class LdRawExpression implements SpiExpression {

	private static final long serialVersionUID = 7973903141340334607L;
	
	private final String rawExpr;

	private final Object[] values;
	
	LdRawExpression(String rawExpr, Object[] values) {
		this.rawExpr = rawExpr;
		this.values = values;
	}
		
	/**
	 * Always returns false.
	 */
	public boolean containsMany(BeanDescriptor<?> desc) {
		return false;
	}
	
	public void addBindValues(SpiExpressionRequest request) {
	    if (values != null){
    		for (int i = 0; i < values.length; i++) {
    			request.addBindValue(values[i]);
    		}
	    }
	}
	
	public void addSql(SpiExpressionRequest request) {
		request.append(rawExpr);
	}
	
	/**
	 * Based on the raw expression.
	 */
	public int queryAutoFetchHash() {
		int hc = LdRawExpression.class.getName().hashCode();
		hc = hc * 31 + rawExpr.hashCode();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		return rawExpr.hashCode();
	}
}
