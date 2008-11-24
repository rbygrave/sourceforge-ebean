package org.avaje.dao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.Query;

/**
 * List of Expressions that make up a where or having clause.
 * <p>
 * An ExpressionList is returned from {@link Query#where()}.
 * </p>
 * <p>
 * The ExpressionList has a list of convenience methods that create the standard
 * expressions and add them to this list.
 * </p>
 * <p>
 * The ExpressionList also duplicates methods that are found on the Query such
 * as findList() and orderBy(). The purpose of these methods is provide a fluid
 * API. The upside of this approach is that you can build and execute a query
 * via chained methods. The down side is that this ExpressionList object has
 * more methods than you would initially expect (the ones duplicated from
 * Query).
 * </p>
 * 
 * @see Query#where()
 */
public interface DaoExpressionList<T> extends Serializable {

	/**
	 * Return the query that owns this expression list.
	 * <p>
	 * This is a convenience method solely to support a fluid API where the
	 * methods are chained together. Adding expressions returns this expression
	 * list and this method can be used after that to return back the original
	 * query so that further things can be added to it.
	 * </p>
	 */
	public DaoQuery<T> query();

	/**
	 * Add an orderBy clause to the query.
	 * @see DaoQuery#orderBy(String)
	 */
	public DaoQuery<T> orderBy(String orderBy);

	/**
	 * Add an orderBy clause to the query.
	 * @see DaoQuery#orderBy(String)
	 */
	public DaoQuery<T> setOrderBy(String orderBy);

	/**
	 * Execute the query returning a list.
	 * @see DaoQuery#findList()
	 */
	public List<T> findList();

	/**
	 * Execute the query returning a set.
	 * @see DaoQuery#findSet()
	 */
	public Set<T> findSet();

	/**
	 * Execute the query returning a map.
	 * @see DaoQuery#findMap()
	 */
	public Map<?, T> findMap();

	/**
	 * Execute the query returning a single bean.
	 * @see DaoQuery#findUnique()
	 */
	public T findUnique();

	/**
	 * Set the first row to fetch.
	 * @see DaoQuery#setFirstRow(int)
	 */
	public DaoQuery<T> setFirstRow(int firstRow);

	/**
	 * Set the maximum number of rows to fetch.
	 * @see DaoQuery#setMaxRows(int)
	 */
	public DaoQuery<T> setMaxRows(int maxRows);

	/**
	 * Set the number of rows after which the fetching should continue in a background thread.
	 * @see DaoQuery#setBackgroundFetchAfter(int)
	 */
	public DaoQuery<T> setBackgroundFetchAfter(int backgroundFetchAfter);

	/**
	 * Set the name of the property which values become the key of a map.
	 * @see DaoQuery#setMapKey(String)
	 */
	public DaoQuery<T> setMapKey(String mapKey);

//	/**
//	 * Set a DaoQueryListener for bean by bean processing.
//	 * @see DaoQuery#setListener(QueryListener)
//	 */
//	public DaoQuery<T> setListener(QueryListener<T> queryListener);

	/**
	 * Set to true to use the query for executing this query.
	 * @see DaoQuery#setUseCache(boolean)
	 */
	public DaoQuery<T> setUseCache(boolean useCache);

//	/**
//	 * Add expressions to the having clause.
//	 * <p>
//	 * The having clause is only used for queries based on
//	 * raw sql (via SqlSelect annotation etc).
//	 * </p>
//	 */
//	public DaoExpressionList<T> having();

	/**
	 * Add another expression to the where clause.
	 */
	public DaoExpressionList<T> where();

	

	/**
	 * Equal To - property is equal to a given value.
	 */
	public DaoExpressionList<T> eq(String propertyName, Object value);

	/**
	 * Not Equal To - property not equal to the given value.
	 */
	public DaoExpressionList<T> ne(String propertyName, Object value);

	/**
	 * Case Insensitive Equal To - property equal to the given value (typically
	 * using a lower() function to make it case insensitive).
	 */
	public DaoExpressionList<T> ieq(String propertyName, String value);

	/**
	 * Between - property between the two given values.
	 */
	public DaoExpressionList<T> between(String propertyName, Object value1, Object value2);

	/**
	 * Greater Than - property greater than the given value.
	 */
	public DaoExpressionList<T> gt(String propertyName, Object value);

	/**
	 * Greater Than or Equal to - property greater than or equal to the given
	 * value.
	 */
	public DaoExpressionList<T> ge(String propertyName, Object value);

	/**
	 * Less Than - property less than the given value.
	 */
	public DaoExpressionList<T> lt(String propertyName, Object value);

	/**
	 * Less Than or Equal to - property less than or equal to the given value.
	 */
	public DaoExpressionList<T> le(String propertyName, Object value);

	/**
	 * Is Null - property is null.
	 */
	public DaoExpressionList<T> isNull(String propertyName);

	/**
	 * Is Not Null - property is not null.
	 */
	public DaoExpressionList<T> isNotNull(String propertyName);

	/**
	 * Like - property like value where the value contains the SQL wild card
	 * characters % (percentage) and _ (underscore).
	 */
	public DaoExpressionList<T> like(String propertyName, String value);

	/**
	 * Case insensitive Like - property like value where the value contains the
	 * SQL wild card characters % (percentage) and _ (underscore). Typically
	 * uses a lower() function to make the expression case insensitive.
	 */
	public DaoExpressionList<T> ilike(String propertyName, String value);

	/**
	 * Starts With - property like value%.
	 */
	public DaoExpressionList<T> startsWith(String propertyName, String value);

	/**
	 * Case insensitive Starts With - property like value%. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public DaoExpressionList<T> istartsWith(String propertyName, String value);

	/**
	 * Ends With - property like %value.
	 */
	public DaoExpressionList<T> endsWith(String propertyName, String value);

	/**
	 * Case insensitive Ends With - property like %value. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public DaoExpressionList<T> iendsWith(String propertyName, String value);

	/**
	 * Contains - property like %value%.
	 */
	public DaoExpressionList<T> contains(String propertyName, String value);

	/**
	 * Case insensitive Contains - property like %value%. Typically uses a
	 * lower() function to make the expression case insensitive.
	 */
	public DaoExpressionList<T> icontains(String propertyName, String value);

	/**
	 * In - property has a value in the array of values.
	 */
	public DaoExpressionList<T> in(String propertyName, Object[] values);

	/**
	 * In - property has a value in the collection of values.
	 */
	public DaoExpressionList<T> in(String propertyName, Collection<?> values);

	/**
	 * Id Equal to - ID property is equal to the value.
	 */
	public DaoExpressionList<T> idEq(Object value);

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
	public DaoExpressionList<T> allEq(Map<String, Object> propertyMap);

//	/**
//	 * Add raw expression with a single parameter.
//	 * <p>
//	 * The raw expression should contain a single ? at the location of the
//	 * parameter.
//	 * </p>
//	 */
//	public ExpressionList<T> raw(String raw, Object value);
//
//	/**
//	 * Add raw expression with an array of parameters.
//	 * <p>
//	 * The raw expression should contain the same number of ? as there are
//	 * parameters.
//	 * </p>
//	 */
//	public ExpressionList<T> raw(String raw, Object[] values);

//	/**
//	 * Add raw expression with no parameters.
//	 */
//	public ExpressionList<T> raw(String raw);
//
//	/**
//	 * And - join two expressions with a logical and.
//	 */
//	public ExpressionList<T> and(Expression expOne, Expression expTwo);
//
//	/**
//	 * Or - join two expressions with a logical or.
//	 */
//	public ExpressionList<T> or(Expression expOne, Expression expTwo);
//
//	/**
//	 * Negate the expression (prefix it with NOT).
//	 */
//	public ExpressionList<T> not(Expression exp);
//
//	/**
//	 * Return a list of expressions that will be joined by AND's.
//	 */
//	public Junction conjunction();
//
//	/**
//	 * Return a list of expressions that will be joined by OR's.
//	 */
//	public Junction disjunction();
}
