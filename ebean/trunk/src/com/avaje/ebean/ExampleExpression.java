package com.avaje.ebean;


public interface ExampleExpression extends Expression {

	/**
	 * By calling this method zero value properties are going to be included in
	 * the expression.
	 * <p>
	 * By default numeric zero values are excluded as they can result from primitive
	 * int and long types.
	 * </p>
	 */
	public ExampleExpression includeZeros();

	/**
	 * Set case insensitive to true.
	 */
	public ExampleExpression caseInsensitive();

	/**
	 * Use startsWith expression for string properties.
	 */
	public ExampleExpression useStartsWith();

	/**
	 * Use contains expression for string properties.
	 */
	public ExampleExpression useContains();

	/**
	 * Use endsWith expression for string properties.
	 */
	public ExampleExpression useEndsWith();

	/**
	 * Use equal to expression for string properties.
	 */
	public ExampleExpression useEqualTo();

}