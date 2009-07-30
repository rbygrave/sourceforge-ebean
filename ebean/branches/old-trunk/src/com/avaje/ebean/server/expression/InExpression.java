package com.avaje.ebean.server.expression;

import java.util.Collection;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionRequest;

class InExpression implements SpiExpression {

	private static final long serialVersionUID = 3150665801693551260L;

	private final String propertyName;
	
	private final Object[] values;
	
	InExpression(String propertyName, Collection<?> coll){
		this.propertyName = propertyName;
		values = coll.toArray(new Object[coll.size()]);
	}
	
	InExpression(String propertyName, Object[] array){
		this.propertyName = propertyName;
		this.values = array;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(SpiExpressionRequest request) {

		for (int i = 0; i < values.length; i++) {
			request.addBindValue(values[i]);
		}
	}

	public void addSql(SpiExpressionRequest request) {
		request.append(propertyName).append(" in ( ?");
		for (int i = 1; i < values.length; i++) {
			
			request.append(", ?");
		}
		
		request.append(" ) ");
	}

	/**
	 * Based on the number of values in the in clause.
	 */
	public int queryAutoFetchHash() {
		return InExpression.class.getName().hashCode() + 31 * values.length;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		int hc = 0;
		for (int i = 1; i < values.length; i++) {
			hc = 31*hc + values[i].hashCode();
		}
		return hc;
	}
	
	
}
