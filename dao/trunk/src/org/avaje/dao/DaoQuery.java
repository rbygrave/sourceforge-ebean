package org.avaje.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DaoQuery<T> extends Serializable {

	/**
	 * Returns true if this query was tuned by autoFetch.
	 */
	public boolean isAutoFetchTuned();

	/**
	 * Explicitly specify whether to use autoFetch for this query.
	 * <p>
	 * If you do not call this method on a query the "Implicit AutoFetch mode"
	 * is used to determine if autoFetch should be used for a given query.
	 * </p>
	 * <p>
	 * When using autoFetch the select() and join() information is controlled by
	 * autoFetch. Profiling is used to determine the joins and properties that
	 * are used and this information is then used to build a "optimal query
	 * plan". Before the query is executed it is modified by applying this
	 * "optimal query plan" which sets the select() and join() information.
	 * </p>
	 */
	public void setAutoFetch(boolean autoFetch);

	/**
	 * Set the query using the query language.
	 * <p>
	 * You are <em>NOT</em> allowed to use this method if the query is a named
	 * query. You are allowed to add additional clauses using where() as well
	 * as use join() and setOrderBy().
	 * </p>
	 * This can contain join where order by and limit clauses. The query can
	 * also contain named parameters.
	 * 
	 * <pre class="code">
	 * String oql = &quot;find order join customer where id = :orderId&quot;;
	 * 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * query.setQuery(oql);
	 * query.setParameter(&quot;orderId&quot;, 1);
	 * Order order = query.findUnique();
	 * </pre>
	 */
	public DaoQuery<T> setQuery(String oql);

	/**
	 * Explicitly set a comma delimited list of the properties to fetch on the
	 * 'main' entity bean. This defaults to '*' which means all properties.
	 * 
	 * <pre class="code">
	 * Query&lt;Customer&gt; query = Ebean.createQuery(Customer.class);
	 * 
	 * // Only fetch the customer id, name and status.
	 * // This is described as a &quot;Partial Object&quot;
	 * query.select(&quot;name, status&quot;);
	 * query.where(&quot;lower(name) like :custname&quot;).setParameter(&quot;custname&quot;, &quot;rob%&quot;);
	 * 
	 * List&lt;Customer&gt; customerList = query.findList();
	 * </pre>
	 * 
	 * @param fetchProperties
	 *            the properties to fetch for this bean (* = all properties).
	 */
	public DaoQuery<T> select(String fetchProperties);

	/**
	 * Specify a property (associated bean) to join and <em>fetch</em> with
	 * its specific properties to include.
	 * <p>
	 * Note that you do <em>NOT</em> need to specify a join just for the
	 * purposes of a where clause (predicate) or order by clause. Ebean will
	 * automatically add appropriate joins to satisfy conditions in where and
	 * order by clauses.
	 * </p>
	 * <p>
	 * When you specify a join this means that property (associated bean(s))
	 * will be fetched and populated. If you specify "*" then all the properties
	 * of the associated bean will be fetched and populated. You can specify a
	 * comma delimited list of the properties of that associated bean which
	 * means that only those properties are fetched and populated resulting in a
	 * "Partial Object" - a bean that only has some of its properties populated.
	 * </p>
	 * 
	 * <pre class="code">
	 * // query orders...
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * 
	 * // join fetch the customer... 
	 * // ... getting the customer's name and phone number
	 * query.join(&quot;customer&quot;, &quot;name, phNumber&quot;);
	 * 
	 * // ... also join fetch the customers billing address (* = all properties)
	 * query.join(&quot;customer.billingAddress&quot;, &quot;*&quot;);
	 * </pre>
	 * 
	 * <p>
	 * If columns is null or "*" then all columns/properties of the joined bean
	 * are fetched.
	 * </p>
	 * 
	 * <pre class="code">
	 * // fetch customers (their id, name and status)
	 * Query&lt;Customer&gt; query = Ebean.createQuery(Customer.class);
	 * 
	 * // only fetch some of the properties of the customers
	 * query.setProperties(&quot;name, status&quot;);
	 * List&lt;Customer&gt; list = query.findList();
	 * </pre>
	 * 
	 * @param assocProperty
	 *            the property of an associated (1-1,1-M,M-1,M-M) bean.
	 * @param fetchProperties
	 *            properties of the associated bean that you want to include in
	 *            the fetch (* means all properties, null also means all
	 *            properties).
	 */
	public DaoQuery<T> join(String assocProperty, String fetchProperties);

	/**
	 * Specify a property (associated bean) to join including all its
	 * properties.
	 * <p>
	 * The same as {@link #join(String, String)} with the fetchProperties as
	 * "*".
	 * </p>
	 * 
	 * @param assocProperty
	 *            the property of an associated (1-1,1-M,M-1,M-M) bean.
	 */
	public DaoQuery<T> join(String assocProperty);

	/**
	 * Execute the query returning the list of objects.
	 * <p>
	 * This query will execute against the EbeanServer that was used to create
	 * it.
	 * </p>
	 * 
	 * @see EbeanServer#findList(DaoQuery, Transaction)
	 */
	public List<T> findList();

	/**
	 * Execute the query returning the set of objects.
	 * <p>
	 * This query will execute against the EbeanServer that was used to create
	 * it.
	 * </p>
	 * 
	 * @see EbeanServer#findSet(DaoQuery, Transaction)
	 */
	public Set<T> findSet();

	/**
	 * Execute the query returning a map of the objects.
	 * <p>
	 * This query will execute against the EbeanServer that was used to create
	 * it.
	 * </p>
	 * <p>
	 * You can use setMapKey() so specify the property values to be used as keys
	 * on the map. If one is not specified then the id property is used.
	 * </p>
	 * 
	 * <pre class="code">
	 * Query&lt;Product&gt; query = Ebean.createQuery(Product.class);
	 * query.setMapKey(&quot;sku&quot;);
	 * Map&lt;?, Product&gt; map = query.findMap();
	 * </pre>
	 * 
	 * @see EbeanServer#findMap(DaoQuery, Transaction)
	 */
	public Map<?, T> findMap();

	/**
	 * Execute the query returning either a single bean or null (if no matching
	 * bean is found).
	 * <p>
	 * If more than 1 row is found for this query then a PersistenceException is
	 * thrown.
	 * </p>
	 * <p>
	 * This is useful when your predicates dictate that your query should only
	 * return 0 or 1 results.
	 * </p>
	 * 
	 * <pre class="code">
	 * // assuming the sku of products is unique...
	 * Query&lt;Product&gt; query = Ebean.createQuery(Product.class);
	 * query.where(&quot;sku = ?&quot;).set(1, &quot;aa113&quot;);
	 * Product product = query.findUnique();
	 * ...
	 * </pre>
	 * 
	 * <p>
	 * It is also useful with finding objects by their id when you want to
	 * specify further join information.
	 * </p>
	 * 
	 * <pre class="code">
	 * // Fetch order 1 and additionally fetch join its order details...
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * query.setId(1);
	 * query.join(&quot;details&quot;);
	 * Order order = query.findUnique();
	 * List&lt;OrderDetail&gt; details = order.getDetails();
	 * ...
	 * </pre>
	 */
	public T findUnique();

	/**
	 * The same as {@link #setParameter(int, Object)} for positioned parameters.
	 * <p>
	 * set() is just an alias for setParameter().
	 * </p>
	 */
	public DaoQuery<T> set(int position, Object value);

	/**
	 * Exactly the same as {@link #setParameter(String, Object)} for named
	 * parameters.
	 * <p>
	 * set() is just an alias for setParameter().
	 * </p>
	 */
	public DaoQuery<T> set(String name, Object value);

	/**
	 * Set a named bind parameter. Named parameters have a colon to prefix the
	 * name.
	 * 
	 * <pre class="code">
	 * // a query with a named parameter
	 * String oql = &quot;find order where status = :orderStatus&quot;;
	 * 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * 
	 * // bind the named parameter
	 * query.bind(&quot;orderStatus&quot;, OrderStatus.NEW);
	 * List&lt;Order&gt; list = query.findList();
	 * </pre>
	 * 
	 * @param name
	 *            the parameter name
	 * @param value
	 *            the parameter value
	 */
	public DaoQuery<T> setParameter(String name, Object value);

	/**
	 * Set an ordered bind parameter according to its position. Note that the
	 * position starts at 1 to be consistent with JDBC PreparedStatement. You
	 * need to set a parameter value for each ? you have in the query.
	 * 
	 * <pre class="code">
	 * // a query with a positioned parameter
	 * String oql = &quot;where status = ? order by id desc&quot;;
	 * 
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * 
	 * // bind the parameter
	 * query.set(1, OrderStatus.NEW);
	 * 
	 * List&lt;Order&gt; list = query.findList();
	 * </pre>
	 * 
	 * @param position
	 *            the parameter bind position starting from 1 (not 0)
	 * @param value
	 *            the parameter bind value.
	 */
	public DaoQuery<T> setParameter(int position, Object value);

//	/**
//	 * Set a listener to process the query on a row by row basis.
//	 * <p>
//	 * Use this when you want to process a large query and do not want to hold
//	 * the entire query result in memory.
//	 * </p>
//	 * <p>
//	 * It this case the rows are not loaded into the persistence context and
//	 * instead are processed by the query listener.
//	 * </p>
//	 * 
//	 * <pre class="code">
//	 * QueryListener&lt;Order&gt; listener = ...;
//	 *   
//	 * Query&lt;Order&gt; query  = Ebean.createQuery(Order.class);
//	 *   
//	 * // set the listener that will process each order one at a time
//	 * query.setListener(listener);
//	 *   
//	 * // execute the query. Note that the returned
//	 * // list (emptyList) will be empty ...
//	 * List&lt;Order&gt; emtyList = query.findList();
//	 * </pre>
//	 */
//	public Query<T> setListener(QueryListener<T> queryListener);

	/**
	 * Set the Id value to query. This is used with findUnique().
	 * <p>
	 * You can use this to have further control over the query. For example
	 * adding fetch joins.
	 * </p>
	 * 
	 * <pre class="code">
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class);
	 * Order order = query.setId(1).join(&quot;details&quot;).findUnique();
	 * List&lt;OrderDetail&gt; details = order.getDetails();
	 * ...
	 * </pre>
	 */
	public DaoQuery<T> setId(Object id);

//	/**
//	 * Add additional clause(s) to the where clause.
//	 * <p>
//	 * This typically contains named parameters which will need to be set via
//	 * {@link #setParameter(String, Object)}.
//	 * </p>
//	 * 
//	 * <pre class="code">
//	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class, &quot;top&quot;);
//	 * ...
//	 * if (...) {
//	 *   query.where(&quot;status = :status and lower(customer.name) like :custName&quot;);
//	 *   query.setParameter(&quot;status&quot;, Order.NEW);
//	 *   query.setParameter(&quot;custName&quot;, &quot;rob%&quot;);
//	 * }
//	 * </pre>
//	 * 
//	 * <p>
//	 * Internally the addToWhereClause string is processed by removing named
//	 * parameters (replacing them with ?) and by converting logical property
//	 * names to database column names (with table alias). The rest of the string
//	 * is left as is and it is completely acceptable and expected for the
//	 * addToWhereClause string to include sql functions and columns.
//	 * </p>
//	 * 
//	 * @param addToWhereClause
//	 *            the clause to append to the where clause which typically
//	 *            contains named parameters.
//	 * @return The query object
//	 */
//	public DaoQuery<T> where(String addToWhereClause);
//
//	/**
//	 * Add a single Expression to the where clause returning the query.
//	 * 
//	 * <pre class="code">
//	 * List&lt;Order&gt; newOrders = Ebean.createQuery(Order.class)
//	 * 		.where(Expr.eq(&quot;status&quot;, Order.NEW))
//	 * 		.findList();
//	 * ...
//	 * </pre>
//	 */
//	public DaoQuery<T> where(Expression expression);

	/**
	 * Add Expressions to the where clause with the ability to chain on the
	 * ExpressionList. You can use this for adding multiple expressions to the
	 * where clause.
	 * 
	 * <pre class="code">
	 * Query&lt;Order&gt; query = Ebean.createQuery(Order.class, &quot;top&quot;);
	 * ...
	 * if (...) {
	 *   query.where()
	 *     .eq(&quot;status&quot;, Order.NEW)
	 *     .ilike(&quot;customer.name&quot;,&quot;rob%&quot;);
	 * }
	 * </pre>
	 * 
	 * @see Expr
	 * @return The ExpressionList for adding expressions to.
	 */
	public DaoExpressionList<T> where();

//	/**
//	 * Add Expressions to the Having clause return the ExpressionList.
//	 * <p>
//	 * Currently only beans based on raw sql will use the having clause.
//	 * </p>
//	 * <p>
//	 * Note that this returns the ExpressionList (so you can add multiple
//	 * expressions to the query in a fluent API way).
//	 * </p>
//	 * 
//	 * @see Expr
//	 * @return The ExpressionList for adding more expressions to.
//	 */
//	public DaoExpressionList<T> having();
//
//	/**
//	 * Add additional clause(s) to the having clause.
//	 * <p>
//	 * This typically contains named parameters which will need to be set via
//	 * {@link #setParameter(String, Object)}.
//	 * </p>
//	 * 
//	 * <pre class="code">
//	 * Query&lt;ReportOrder&gt; query = Ebean.createQuery(ReportOrder.class);
//	 * ...
//	 * if (...) {
//	 *   query.having(&quot;score &gt; :min&quot;);
//	 *   query.setParameter(&quot;min&quot;, 1);
//	 * }
//	 * </pre>
//	 * 
//	 * @param addToHavingClause
//	 *            the clause to append to the having clause which typically
//	 *            contains named parameters.
//	 * @return The query object
//	 */
//	public DaoQuery<T> having(String addToHavingClause);
//
//	/**
//	 * Add an expression to the having clause returning the query.
//	 * <p>
//	 * Currently only beans based on raw sql will use the having clause.
//	 * </p>
//	 * <p>
//	 * This is similar to {@link #having()} except it returns the query rather
//	 * than the ExpressionList. This is useful when you want to further specify
//	 * something on the query.
//	 * </p>
//	 * 
//	 * @param addExpressionToHaving
//	 *            the expression to add to the having clause.
//	 * @return the Query object
//	 */
//	public DaoQuery<T> having(Expression addExpressionToHaving);

	/**
	 * Set the order by clause.
	 */
	public DaoQuery<T> setOrderBy(String orderBy);

	/**
	 * Set the order by clause.
	 */
	public DaoQuery<T> orderBy(String orderBy);

	/**
	 * Set whether this query uses DISTINCT.
	 */
	public DaoQuery<T> setDistinct(boolean isDistinct);

	/**
	 * Set the first row to return.
	 * 
	 * @param firstRow
	 */
	public DaoQuery<T> setFirstRow(int firstRow);

	/**
	 * Set the maximum number of rows to return in the query.
	 * 
	 * @param maxRows
	 *            the maximum number of rows to return in the query.
	 */
	public DaoQuery<T> setMaxRows(int maxRows);

	/**
	 * Set the rows after which fetching should continue in a background thread.
	 * 
	 * @param backgroundFetchAfter
	 */
	public DaoQuery<T> setBackgroundFetchAfter(int backgroundFetchAfter);

	/**
	 * Set the property to use as keys for a map.
	 * <p>
	 * If no property is set then the id property is used.
	 * </p>
	 * 
	 * <pre class="code">
	 * // Assuming sku is unique for products...
	 *    
	 * Query&lt;Product&gt; query = Ebean.createQuery(Product.class);
	 *   
	 * // use sku for keys...
	 * query.setMapKey(&quot;sku&quot;);
	 *   
	 * Map&lt;?,Product&gt; productMap = query.findMap();
	 * ...
	 * </pre>
	 * 
	 * @param mapKey
	 *            the property to use as keys for a map.
	 */
	public DaoQuery<T> setMapKey(String mapKey);

	/**
	 * Set this to true to use the cache.
	 * <p>
	 * If the query result is in cache then by default this same instance is
	 * returned. In this sense it should be treated as a read only object graph.
	 * </p>
	 */
	public DaoQuery<T> setUseCache(boolean useCache);

	/**
	 * Set a timeout on this query.
	 * <p>
	 * This will typically result in a call to setQueryTimeout() on a preparedStatement.
	 * If the timeout occurs an exception will be thrown - this will be a SQLException wrapped
	 * up in a PersistenceException.
	 * </p>
	 * 
	 * @param secs the query timeout limit in seconds. Zero means there is no limit.
	 */
	public DaoQuery<T> setTimeout(int secs);

	/**
	 * Return the sql that was generated for executing this query.
	 * <p>
	 * This is only available after the query has been executed and provided
	 * only for informational purposes.
	 * </p>
	 */
	public String getGeneratedSql();
}
