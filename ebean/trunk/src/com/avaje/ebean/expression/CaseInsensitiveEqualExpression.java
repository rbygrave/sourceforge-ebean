package com.avaje.ebean.expression;

import com.avaje.ebean.bean.BeanQueryRequest;


class CaseInsensitiveEqualExpression implements Expression {

	private static final long serialVersionUID = -6406036750998971064L;

	final String propertyName;
	
	final String value;
	
	CaseInsensitiveEqualExpression(String propertyName, String value) {
		this.propertyName = propertyName;
		this.value = value.toLowerCase();
	}
	
	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(ExpressionRequest request) {
		request.addBindValue(value);
	}

	public void addSql(ExpressionRequest request) {
		
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
