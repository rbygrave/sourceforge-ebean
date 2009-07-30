package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionRequest;


class BetweenExpression implements SpiExpression {

	private static final long serialVersionUID = 2078918165221454910L;

	private static final String BETWEEN = " between ";
	
	private final String propertyName;
	
	private final Object valueHigh;
	
	private final Object valueLow;
	
	BetweenExpression(String propertyName, Object valLo, Object valHigh) {
		this.propertyName = propertyName;
		this.valueLow = valLo;
		this.valueHigh = valHigh;
	}
	
	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(SpiExpressionRequest request) {
		request.addBindValue(valueLow);
		request.addBindValue(valueHigh);
	}

	public void addSql(SpiExpressionRequest request) {
		
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
