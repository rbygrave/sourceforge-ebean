package com.avaje.ebean.expression;

import com.avaje.ebean.bean.BeanQueryRequest;


class BetweenExpression implements Expression {

	private static final long serialVersionUID = 2078918165221454910L;

	private static final String BETWEEN = " between ";
	
	final String propertyName;
	
	final Object valueHigh;
	
	final Object valueLow;
	
	BetweenExpression(String propertyName, Object valLo, Object valHigh) {
		this.propertyName = propertyName;
		this.valueLow = valLo;
		this.valueHigh = valHigh;
	}
	
	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(ExpressionRequest request) {
		request.addBindValue(valueLow);
		request.addBindValue(valueHigh);
	}

	public void addSql(ExpressionRequest request) {
		
		request.append(propertyName).append(BETWEEN).append(" ? and ? ");
	}

	public int queryAutoFetchHash() {
		int hc = BetweenExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		int hc = valueLow.hashCode();
		hc = hc * 31 + valueHigh.hashCode();
		return hc;
	}
}
