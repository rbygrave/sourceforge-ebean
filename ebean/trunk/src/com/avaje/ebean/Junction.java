package com.avaje.ebean;

/**
 * Represents a Conjunction or a Disjunction.
 * <p>
 * Basically with a Conjunction you join together many expressions with AND, and
 * with a Disjunction you join together many expressions with OR.
 * </p>
 * <pre class="code">
 *  Query q = Ebean.createQuery(Person.class);
 *  q.where().disjunction()
 *  	.add(Expr.like(&quot;name&quot;,&quot;Rob%&quot;))
 *  	.add(Expr.eq(&quot;status&quot;,Status.NEW))
 * </pre>
 * @see Query#where()
 * @see ExpressionList#conjunction()
 * @see ExpressionList#disjunction()
 */
public interface Junction extends Expression {

	/**
	 * Add an expression to the Conjunction/Disjunction.
	 * 
	 * @see ExpressionList#conjunction()
	 * @see ExpressionList#disjunction()
	 */
	public Junction add(Expression expression);

}
