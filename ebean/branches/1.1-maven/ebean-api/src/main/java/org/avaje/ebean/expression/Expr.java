package org.avaje.ebean.expression;

import java.util.Collection;
import java.util.Map;

import org.avaje.ebean.Query;

/**
 * Expression factory for creating standard expressions.
 * <p>
 * Creates standard common expressions for using in a Query Where or Having clause.
 * </p>
 * <p>
 * </p>
 * <pre class="code">
 *  // Example: fetch orders where status equals new and orderDate > lastWeek.
 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
 * query.addWhere()
 *     .add(Expr.eq(&quot;status&quot;, Order.NEW))
 *     .add(Expr.gt(&quot;orderDate&quot;, lastWeek));
 * List&lt;Order&gt; list = query.findList();
 * ...
 * </pre>
 * @see Query#where()
 */
public class Expr {

	private Expr() {
	}

	/**
	 * Equal To - property equal to the given value.
	 */
	public static Expression eq(String propertyName, Object value) {
		if (value == null) {
			return isNull(propertyName);
		}
		return new SimpleExpression(propertyName, SimpleExpression.Op.EQ, value);
	}

	/**
	 * Not Equal To - property not equal to the given value.
	 */
	public static Expression ne(String propertyName, Object value) {
		if (value == null) {
			return isNotNull(propertyName);
		}
		return new SimpleExpression(propertyName, SimpleExpression.Op.NOT_EQ, value);
	}

	/**
	 * Case Insensitive Equal To - property equal to the given value (typically
	 * using a lower() function to make it case insensitive).
	 */
	public static Expression ieq(String propertyName, String value) {
		if (value == null) {
			return isNull(propertyName);
		}
		return new CaseInsensitiveEqualExpression(propertyName, value);
	}

	/**
	 * Between - property between the two given values.
	 */
	public static Expression between(String propertyName, Object value1, Object value2) {
		
		return new BetweenExpression(propertyName, value1, value2);
	}
	
	/**
	 * Greater Than - property greater than the given value.
	 */
	public static Expression gt(String propertyName, Object value) {

		return new SimpleExpression(propertyName, SimpleExpression.Op.GT, value);
	}

	/**
	 * Greater Than or Equal to - property greater than or equal to the given
	 * value.
	 */
	public static Expression ge(String propertyName, Object value) {

		return new SimpleExpression(propertyName, SimpleExpression.Op.GT_EQ, value);
	}

	/**
	 * Less Than - property less than the given value.
	 */
	public static Expression lt(String propertyName, Object value) {

		return new SimpleExpression(propertyName, SimpleExpression.Op.LT, value);
	}

	/**
	 * Less Than or Equal to - property less than or equal to the given value.
	 */
	public static Expression le(String propertyName, Object value) {

		return new SimpleExpression(propertyName, SimpleExpression.Op.LT_EQ, value);
	}

	/**
	 * Is Null - property is null.
	 */
	public static Expression isNull(String propertyName) {

		return new NullExpression(propertyName, false);
	}

	/**
	 * Is Not Null - property is not null.
	 */
	public static Expression isNotNull(String propertyName) {

		return new NullExpression(propertyName, true);
	}

	/**
	 * Like - property like value where the value contains the SQL wild card
	 * characters % (percentage) and _ (underscore).
	 */
	public static Expression like(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.raw);
	}

	/**
	 * Case insensitive Like - property like value where the value contains the
	 * SQL wild card characters % (percentage) and _ (underscore). Typically
	 * uses a lower() function to make the expression case insensitive.
	 */
	public static Expression ilike(String propertyName, String value) {
		return new LikeExpression(propertyName, value, true, LikeExpression.Type.raw);
	}

	/**
	 * Starts With - property like value%.
	 */
	public static Expression startsWith(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.startsWith);
	}

	/**
	 * Case insensitive Starts With - property like value%. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public static Expression istartsWith(String propertyName, String value) {
		return new LikeExpression(propertyName, value, true, LikeExpression.Type.startsWith);
	}

	/**
	 * Ends With - property like %value.
	 */
	public static Expression endsWith(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.endsWith);
	}

	/**
	 * Case insensitive Ends With - property like %value. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public static Expression iendsWith(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.endsWith);
	}

	/**
	 * Contains - property like %value%.
	 */
	public static Expression contains(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.contains);
	}

	/**
	 * Case insensitive Contains - property like %value%. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public static Expression icontains(String propertyName, String value) {
		return new LikeExpression(propertyName, value, false, LikeExpression.Type.contains);
	}

	/**
	 * In - property has a value in the array of values.
	 */
	public static Expression in(String propertyName, Object[] values) {
		return new InExpression(propertyName, values);
	}

	/**
	 * In - property has a value in the collection of values.
	 */
	public static Expression in(String propertyName, Collection<?> values) {
		return new InExpression(propertyName, values);
	}

	/**
	 * Id Equal to - ID property is equal to the value.
	 */
	public static Expression idEq(Object value) {
		return new IdExpression(value);
	}

	/**
	 * All Equal - Map containing property names and their values.
	 * <p>
	 * Expression where all the property names in the map are equal to the
	 * corresponding value.
	 * </p>
	 * 
	 * @param propertyMap
	 *            a map keyed by property names.
	 */
	public static Expression allEq(Map<String, Object> propertyMap) {
		return new AllEqualsExpression(propertyMap);
	}

	/**
	 * Add raw expression with a single parameter.
	 * <p>
	 * The raw expression should contain a single ? at the location of the
	 * parameter.
	 * </p>
	 */
	public static Expression raw(String raw, Object value) {
		return new RawExpression(raw, new Object[] { value });
	}

	/**
	 * Add raw expression with an array of parameters.
	 * <p>
	 * The raw expression should contain the same number of ? as there are
	 * parameters.
	 * </p>
	 */
	public static Expression raw(String raw, Object[] values) {
		return new RawExpression(raw, values);
	}

	/**
	 * Add raw expression with no parameters.
	 */
	public static Expression raw(String raw) {
		return new RawExpression(raw, RawExpression.EMPTY_ARRAY);
	}

	/**
	 * And - join two expressions with a logical and.
	 */
	public static Expression and(Expression expOne, Expression expTwo) {

		return new LogicExpression.And(expOne, expTwo);
	}

	/**
	 * Or - join two expressions with a logical or.
	 */
	public static Expression or(Expression expOne, Expression expTwo) {

		return new LogicExpression.Or(expOne, expTwo);
	}

	/**
	 * Negate the expression (prefix it with NOT).
	 */
	public static Expression not(Expression exp) {

		return new NotExpression(exp);
	}

	/**
	 * Return a list of expressions that will be joined by AND's.
	 */
	public static Junction conjunction() {

		return new JunctionExpression.Conjunction();
	}

	/**
	 * Return a list of expressions that will be joined by OR's.
	 */
	public static Junction disjunction() {

		return new JunctionExpression.Disjunction();
	}
}
