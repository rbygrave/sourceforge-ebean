package com.avaje.ebean.expression;

import com.avaje.ebean.bean.BeanQueryRequest;


/**
 * Slightly redundant as Query.setId() ultimately also does the same job.
 */
class NullExpression implements Expression {

	private static final long serialVersionUID = 4246991057451128269L;

	final String propertyName;
	
	final boolean notNull;
	
	NullExpression(String propertyName, boolean notNull) {
		this.propertyName = propertyName;
		this.notNull = notNull;
	}

	public String getPropertyName() {
		return propertyName;
	}
	
	public void addBindValues(ExpressionRequest request) {
		
	}
	
	public void addSql(ExpressionRequest request) {
		
		request.append(propertyName).append(" ");
		if (notNull){
			request.append(" is not null ");
		} else {
			request.append(" is null ");			
		}
	}
	
	/**
	 * Based on notNull flag and the propertyName.
	 */
	public int queryAutoFetchHash() {
		int hc = NullExpression.class.getName().hashCode();
		hc = hc * 31 + (notNull ? 1 : 0);
		hc = hc * 31 + propertyName.hashCode();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		return (notNull ? 1 : 0);
	}
}
