package com.avaje.ebean.expression;

import com.avaje.ebean.server.core.QueryRequest;


final class NotExpression implements Expression {

	private static final long serialVersionUID = 5648926732402355781L;

	private static final String NOT = "not (";
	
	final Expression exp;
	
	NotExpression(Expression exp){
		this.exp = exp;
	}
	
	public String getPropertyName() {
		return exp.getPropertyName();
	}

	public void addBindValues(ExpressionRequest request) {
		exp.addBindValues(request);
	}
	
	public void addSql(ExpressionRequest request) {
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

	public int queryPlanHash(QueryRequest request) {
		int hc = NotExpression.class.getName().hashCode();
		hc = hc * 31 + exp.queryPlanHash(request);
		return hc;
	}
	
	public int queryBindHash() {
		return exp.queryBindHash();
	}
	
}
