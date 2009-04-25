package com.avaje.ebean.expression;

import java.util.ArrayList;
import java.util.Iterator;

import com.avaje.ebean.bean.BeanQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;

/**
 * A "Query By Example" type of expression.
 * <p>
 * Pass in an example entity and for each non-null scalar properties an
 * expression is added.
 * </p>
 * <pre class="code"> 
 *  // create an example bean and set the properties
 *  // with the query parameters you want
 * Customer example = new Customer();
 * example.setName("Rob%"); 
 * example.setNotes("%something%");
 * 
 * List&lt;Customer&gt; list = 
 * 		Ebean.find(Customer.class)
 * 		.where()
 * 		// pass the bean into the where() clause
 * 		.exampleLike(example)
 * 		// you can add other expressions to the same query
 * 		.gt("id", 2)
 * 		.findList();
 *  
 * </pre>
 */
public class ExampleExpression implements Expression {

	private static final long serialVersionUID = 1L;

	/**
	 * The example bean containing the properties.
	 */
	final Object entity;

	/**
	 * Set to true to use case insensitive expressions.
	 */
	boolean caseInsensitive;

	/**
	 * The type of like (RAW, STARTS_WITH, ENDS_WITH etc)
	 */
	LikeType likeType;

	/**
	 * By default zeros are excluded.
	 */
	boolean includeZeros;

	/**
	 * The non null bean properties and found and together added as a list of
	 * expressions (like or equal to expressions).
	 */
	ArrayList<Expression> list;

	/**
	 * Construct the query by example expression.
	 * 
	 * @param entity
	 *            the example entity with non null property values
	 * @param caseInsensitive
	 *            if true use case insensitive expressions
	 * @param likeType
	 *            the type of Like wild card used
	 */
	public ExampleExpression(Object entity, boolean caseInsensitive, LikeType likeType) {
		this.entity = entity;
		this.caseInsensitive = caseInsensitive;
		this.likeType = likeType;
	}

	/**
	 * By calling this method zero value properties are going to be included in
	 * the expression.
	 * <p>
	 * By default numeric zero values are excluded as they can result from primitive
	 * int and long types.
	 * </p>
	 */
	public ExampleExpression includeZeros() {
		includeZeros = true;
		return this;
	}

	/**
	 * Set case insensitive to true.
	 */
	public ExampleExpression caseInsensitive() {
		caseInsensitive = true;
		return this;
	}
	
	/**
	 * Use startsWith expression for string properties.
	 */
	public ExampleExpression useStartsWith() {
		likeType = LikeType.STARTS_WITH;
		return this;
	}

	/**
	 * Use contains expression for string properties.
	 */
	public ExampleExpression useContains() {
		likeType = LikeType.CONTAINS;
		return this;
	}

	/**
	 * Use endsWith expression for string properties.
	 */
	public ExampleExpression useEndsWith() {
		likeType = LikeType.ENDS_WITH;
		return this;
	}

	/**
	 * Use equal to expression for string properties.
	 */
	public ExampleExpression useEqualTo() {
		likeType = LikeType.EQUAL_TO;
		return this;
	}

	/**
	 * This will return null for this example expression.
	 */
	public String getPropertyName() {
		return null;
	}

	/**
	 * Adds bind values to the request.
	 */
	public void addBindValues(ExpressionRequest request) {

		for (int i = 0; i < list.size(); i++) {
			Expression item = list.get(i);
			item.addBindValues(request);
		}
	}

	/**
	 * Generates and adds the sql to the request.
	 */
	public void addSql(ExpressionRequest request) {

		if (!list.isEmpty()) {
			request.append("(");

			for (int i = 0; i < list.size(); i++) {
				Expression item = list.get(i);
				if (i > 0) {
					request.append(" and ");
				}
				item.addSql(request);
			}

			request.append(") ");
		}
	}

	/**
	 * Return a hash for autoFetch query identification.
	 */
	public int queryAutoFetchHash() {
		// we have not yet built the list of expressions
		// so just based on the class name
		return ExampleExpression.class.getName().hashCode();
	}

	/**
	 * Return a hash for query plan identification.
	 */
	public int queryPlanHash(BeanQueryRequest<?> request) {

		// this is always called once, and always called before
		// addSql() and addBindValues() methods
		list = buildExpressions(request);

		int hc = ExampleExpression.class.getName().hashCode();

		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryPlanHash(request);
		}

		return hc;
	}

	/**
	 * Return a hash for the actual bind values used.
	 */
	public int queryBindHash() {
		int hc = ExampleExpression.class.getName().hashCode();
		for (int i = 0; i < list.size(); i++) {
			hc = hc * 31 + list.get(i).queryBindHash();
		}

		return hc;
	}

	/**
	 * Build the List of expressions.
	 */
	private ArrayList<Expression> buildExpressions(BeanQueryRequest<?> request) {

		ArrayList<Expression> list = new ArrayList<Expression>();

		BeanDescriptor<?> beanDescriptor = request.getBeanDescriptor();

		Iterator<BeanProperty> propIter = beanDescriptor.propertiesAll();

		while (propIter.hasNext()) {
			BeanProperty beanProperty = propIter.next();
			String propName = beanProperty.getName();
			Object value = beanProperty.getValue(entity);

			if (beanProperty.isScalar() && value != null) {
				if (value instanceof String) {
					list.add(new LikeExpression(propName, (String) value, caseInsensitive, likeType));
				} else {
					if (!includeZeros && isZero(value)) {
						// exclude the zero values typically to weed out
						// primitive int and long that initialise to 0
					} else {
						list.add(new SimpleExpression(propName, SimpleExpression.Op.EQ, value));
					}
				}
			}
		}

		return list;
	}

	/**
	 * Return true if the value is a numeric zero.
	 */
	private boolean isZero(Object value) {
		if (value instanceof Number) {
			Number num = (Number) value;
			double doubleValue = num.doubleValue();
			if (doubleValue == 0) {
				return true;
			}
		}
		return false;
	}
}
