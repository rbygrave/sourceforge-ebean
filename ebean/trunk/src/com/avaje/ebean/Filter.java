package com.avaje.ebean;

import java.util.List;
import java.util.Set;

/**
 * Provides support filtering and sorting of lists of entities locally without
 * going back to the database.
 * <p>
 * That it uses local in-memory sorting and filtering of a list of entity beans.
 * It is not used in a Database query or invoke a Database query.
 * </p>
 * <p>
 * You can optionally specify a sortByClause and if so, the sort will always
 * execute prior to the filter expressions. You can specify any number of filter
 * expressions and they are effectively joined by logical "AND".
 * </p>
 * 
 * <pre class="code">
 * 
 * 	// get a list of entities (query execution statistics in this case)
 * 
 * 	List&lt;MetaQueryStatistic&gt; list =
 * 		Ebean.find(MetaQueryStatistic.class).findList();
 * 
 * 	long nowMinus24Hrs = System.currentTimeMillis() -24*(1000*60*60);
 * 
 * 	// sort and filter the list returning a filtered list...
 * 
 * 	List&lt;MetaQueryStatistic&gt; filteredList =
 * 	   Ebean.filter(MetaQueryStatistic.class) 
 * 			.sort("avgTimeMicros desc")
 * 			.gt("executionCount", 0) 
 * 			.gt("lastQueryTime", nowMinus24Hrs)
 * 			.eq("autofetchTuned", true) 
 * 			.maxRows(10) 
 * 			.filter(list);
 * 
 * </pre>
 * <p>
 * The propertyNames can traverse the object graph (e.g. customer.name)
 * by using dot notation. If any point during the object graph traversal to get
 * a property value is null then null is returned.
 * </p>
 * <pre>
 *		// examples of property names that 
 *		// ... will traverse the object graph
 *		// ... where customer is a property of our bean
 *
 * 		customer.name
 *      customer.shippingAddress.city
 * </pre>
 * </p>
 * <pre class="code">
 * 
 * 	// get a list of entities (query execution statistics)
 * 
 * 	List&lt;Order&gt; orders =
 * 			Ebean.find(Order.class).findList();
 * 
 * 
 * 	// Apply a filter...
 * 
 * 	List&lt;Order&gt; filteredOrders =
 * 	   	Ebean.filter(Order.class) 
 * 			.startsWith("customer.name", "Rob")
 * 			.eq("customer.shippingAddress.city", "Auckland")
 * 			.filter(orders);
 * 
 * </pre>
 * @param <T>
 *            the entity bean type
 */
public interface Filter<T> {

	/**
	 * Specify a sortByClause.
	 * <p>
	 * The sort (if specified) will always execute first followed by the filter
	 * expressions.
	 * </p>
	 * <p>
	 * Refer to {@link Ebean#sort(List, String)} for more detail.
	 * </p>
	 */
	public Filter<T> sort(String sortByClause);

	/**
	 * Specify the maximum number of rows/elements to return.
	 */
	public Filter<T> maxRows(int maxRows);

	/**
	 * Equal To - property equal to the given value.
	 */
	public Filter<T> eq(String prop, Object value);

	/**
	 * Not Equal To - property not equal to the given value.
	 */
	public Filter<T> ne(String propertyName, Object value);

	/**
	 * Case Insensitive Equal To.
	 */
	public Filter<T> ieq(String propertyName, String value);

	/**
	 * Between - property between the two given values.
	 */
	public Filter<T> between(String propertyName, Object value1, Object value2);

	/**
	 * Greater Than - property greater than the given value.
	 */
	public Filter<T> gt(String propertyName, Object value);

	/**
	 * Greater Than or Equal to - property greater than or equal to the given
	 * value.
	 */
	public Filter<T> ge(String propertyName, Object value);

	/**
	 * Less Than - property less than the given value.
	 */
	public Filter<T> lt(String propertyName, Object value);

	/**
	 * Less Than or Equal to - property less than or equal to the given value.
	 */
	public Filter<T> le(String propertyName, Object value);

	/**
	 * Is Null - property is null.
	 */
	public Filter<T> isNull(String propertyName);

	/**
	 * Is Not Null - property is not null.
	 */
	public Filter<T> isNotNull(String propertyName);

	/**
	 * Starts With.
	 */
	public Filter<T> startsWith(String propertyName, String value);

	/**
	 * Case insensitive Starts With.
	 */
	public Filter<T> istartsWith(String propertyName, String value);

	/**
	 * Ends With.
	 */
	public Filter<T> endsWith(String propertyName, String value);

	/**
	 * Case insensitive Ends With.
	 */
	public Filter<T> iendsWith(String propertyName, String value);

	/**
	 * Contains - property contains the string "value".
	 */
	public Filter<T> contains(String propertyName, String value);

	/**
	 * Case insensitive Contains.
	 */
	public Filter<T> icontains(String propertyName, String value);

	/**
	 * In - property has a value contained in the set of values.
	 */
	public Filter<T> in(String propertyName, Set<?> values);

	/**
	 * Apply the filter to the list returning a new list of the matching
	 * elements.
	 * 
	 * @param list
	 *            the source list the filter is applied to
	 * @return Returns a new list with the sorting and filters applied.
	 */
	public List<T> filter(List<T> list);

}