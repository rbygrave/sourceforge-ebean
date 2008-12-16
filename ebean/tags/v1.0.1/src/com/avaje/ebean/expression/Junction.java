package com.avaje.ebean.expression;

/**
 * Represents a Conjunction or a Disjunction.
 * <p>
 * Basically with a Conjunction you join together many expressions with AND, and
 * with a Disjunction you join together many expressions with OR.
 * </p>
 */
public interface Junction extends Expression {

	/**
	 * Add an expression to the Conjunction/Disjunction.
	 * 
	 * @see com.avaje.ebean.expression.ExpressionList#conjunction()
	 * @see com.avaje.ebean.expression.ExpressionList#disjunction()
	 */
	public Junction add(Expression expression);

}
