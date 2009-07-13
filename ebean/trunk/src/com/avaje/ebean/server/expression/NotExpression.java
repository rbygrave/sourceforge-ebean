package com.avaje.ebean.server.expression;

import com.avaje.ebean.Expression;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.InternalExpression;
import com.avaje.ebean.internal.InternalExpressionRequest;


final class NotExpression implements InternalExpression {

	private static final long serialVersionUID = 5648926732402355781L;

	private static final String NOT = "not (";
	
	final InternalExpression exp;
	
	NotExpression(Expression exp){
		this.exp = (InternalExpression)exp;
	}
	
	public String getPropertyName() {
		return exp.getPropertyName();
	}

	public void addBindValues(InternalExpressionRequest request) {
		exp.addBindValues(request);
	}
	
	public void addSql(InternalExpressionRequest request) {
		request.append(NOT);
		exp.addSql(request);
		request.append(") ");
	}

	/**
	 * Based on the expression.
	 */
	public int queryAutoFetchHash() {
		int hc = NotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryAutoFetchHash();
		return hc;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		int hc = NotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryPlanHash(request);
		return hc;
	}
	
	public int queryBindHash() {
		return exp.queryBindHash();
	}
	
}
