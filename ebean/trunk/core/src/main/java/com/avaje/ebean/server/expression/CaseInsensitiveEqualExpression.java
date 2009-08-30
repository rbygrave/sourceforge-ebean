package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpressionRequest;


class CaseInsensitiveEqualExpression extends AbstractExpression {

	private static final long serialVersionUID = -6406036750998971064L;
	
	private final String value;
	
	CaseInsensitiveEqualExpression(String propertyName, String value) {
		super(propertyName);
		this.value = value.toLowerCase();
	}
	
	public void addBindValues(SpiExpressionRequest request) {
		request.addBindValue(value);
	}

	public void addSql(SpiExpressionRequest request) {
		
		request.append("lower(").append(propertyName).append(") =? ");
	}

	public int queryAutoFetchHash() {
		int hc = CaseInsensitiveEqualExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		return value.hashCode();
	}
}
