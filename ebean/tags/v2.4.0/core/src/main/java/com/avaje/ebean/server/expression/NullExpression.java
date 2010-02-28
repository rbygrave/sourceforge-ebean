package com.avaje.ebean.server.expression;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpressionRequest;


/**
 * Slightly redundant as Query.setId() ultimately also does the same job.
 */
class NullExpression extends AbstractExpression {

	private static final long serialVersionUID = 4246991057451128269L;
	
	private final boolean notNull;
	
	NullExpression(String propertyName, boolean notNull) {
		super(propertyName);
		this.notNull = notNull;
	}
	
	public void addBindValues(SpiExpressionRequest request) {
		
	}
	
	public void addSql(SpiExpressionRequest request) {
		
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
