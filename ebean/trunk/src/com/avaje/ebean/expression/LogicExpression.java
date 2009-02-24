package com.avaje.ebean.expression;


/**
 * A logical And or Or for joining two expressions.
 */
abstract class LogicExpression implements Expression {

	static final String AND = " and ";
	static final String OR = " or ";

	static class And extends LogicExpression {
		
		private static final long serialVersionUID = -3832889676798526444L;

		And(Expression expOne, Expression expTwo) {
			super(AND, expOne, expTwo);
		}
	}

	static class Or extends LogicExpression {

		private static final long serialVersionUID = -6871993143194094819L;

		Or(Expression expOne, Expression expTwo) {
			super(OR, expOne, expTwo);
		}
	}

	final Expression expOne;

	final Expression expTwo;

	final String joinType;

	LogicExpression(String joinType, Expression expOne, Expression expTwo) {
		this.joinType = joinType;
		this.expOne = expOne;
		this.expTwo = expTwo;
	}

	public String getPropertyName() {
		return null;
	}
	
	public void addBindValues(ExpressionRequest request) {
		expOne.addBindValues(request);
		expTwo.addBindValues(request);
	}

	public void addSql(ExpressionRequest request) {

		request.append("(");
		expOne.addSql(request);
		request.append(joinType);
		expTwo.addSql(request);
		request.append(") ");
	}

	/**
	 * Based on the joinType plus the two expressions.
	 */
	public int queryPlanHash() {
		int hc = LogicExpression.class.getName().hashCode() + joinType.hashCode();
		hc = hc * 31 + expOne.queryPlanHash();
		hc = hc * 31 + expTwo.queryPlanHash();
		return hc;
	}

	public int queryBindHash() {
		int hc = expOne.queryBindHash();
		hc = hc * 31 + expTwo.queryBindHash();
		return hc;
	}

}
