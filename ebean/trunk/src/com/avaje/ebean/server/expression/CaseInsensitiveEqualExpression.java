package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.InternalExpression;
import com.avaje.ebean.internal.InternalExpressionRequest;


class CaseInsensitiveEqualExpression implements InternalExpression {

	private static final long serialVersionUID = -6406036750998971064L;

	private final String propertyName;
	
	private final String value;
	
	CaseInsensitiveEqualExpression(String propertyName, String value) {
		this.propertyName = propertyName;
		this.value = value.toLowerCase();
	}
	
	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(InternalExpressionRequest request) {
		request.addBindValue(value);
	}

	public void addSql(InternalExpressionRequest request) {
		
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
