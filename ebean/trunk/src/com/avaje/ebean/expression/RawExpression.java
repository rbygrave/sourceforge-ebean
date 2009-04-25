package com.avaje.ebean.expression;

import com.avaje.ebean.bean.BeanQueryRequest;


class RawExpression implements Expression {

	private static final long serialVersionUID = 7973903141340334606L;

	static final Object[] EMPTY_ARRAY = new Object[]{};
	
	final String sql;

	final Object[] values;
	
	RawExpression(String sql, Object[] values) {
		this.sql = sql;
		this.values = values;
	}
	
	public String getPropertyName() {
		return null;
	}
	
	public void addBindValues(ExpressionRequest request) {
		for (int i = 0; i < values.length; i++) {
			request.addBindValue(values[i]);
		}
	}
	
	public void addSql(ExpressionRequest request) {
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
