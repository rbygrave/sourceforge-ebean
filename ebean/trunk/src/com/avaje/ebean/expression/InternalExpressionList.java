package com.avaje.ebean.expression;

import java.util.ArrayList;

import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Internal extension of ExpressionList.
 */
public interface InternalExpressionList<T> extends ExpressionList<T> {

	/**
	 * Returns true if the expression list contains a many property.
	 */
	public boolean containsMany(BeanDescriptor<T> desc);
	
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
