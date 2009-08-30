package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;


class RawExpression implements SpiExpression {

	private static final long serialVersionUID = 7973903141340334606L;
	
	private final String sql;

	private final Object[] values;
	
	RawExpression(String sql, Object[] values) {
		this.sql = sql;
		this.values = values;
	}
		
	/**
	 * Always returns false.
	 */
	public boolean containsMany(BeanDescriptor<?> desc) {
		return false;
	}
	
	public void addBindValues(SpiExpressionRequest request) {
		for (int i = 0; i < values.length; i++) {
			request.addBindValue(values[i]);
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
