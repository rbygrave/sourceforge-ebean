package org.avaje.ebean.expression;

import java.util.ArrayList;

/**
 * Internal extension of ExpressionList.
 */
public interface InternalExpressionList<T> extends ExpressionList<T> {

	/**
	 * Return true if this list is empty.
	 */
	public boolean isEmpty();

	/**
	 * Concatinate the expression sql into a String.
	 * <p>
	 * The list of expressions are evaluated in order building a sql statement
	 * with bind parameters.
	 * </p>
	 */
	public String buildSql(ExpressionRequest request);

	/**
	 * Combine the expression bind values into a list.
	 * <p>
	 * Expressions are evaluated in order and all the resulting bind values are
	 * returned as a List.
	 * </p>
	 * 
	 * @return the list of all the bind values in order.
	 */
	public ArrayList<Object> buildBindValues(ExpressionRequest request);
}
