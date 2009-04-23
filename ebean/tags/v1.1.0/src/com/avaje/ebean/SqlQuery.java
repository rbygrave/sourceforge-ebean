package com.avaje.ebean;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Query object for performing native SQL queries that return MapBeans.
 * <p>
 * Firstly note that you can use your own sql queries with <em>entity beans</em>
 * by using the SqlSelect annotation. This should be your first approach when
 * wanting to use your own SQL queries.
 * </p>
 * <p>
 * If ORM Mapping is too tight and constraining for your problem then SqlQuery
 * and MapBeans could be a good approach. The are generally more suited to more
 * utility type applications and could be described as a fairly relational "bare
 * metal" type approach. If your problem is relational in nature, has very
 * dynamic data requirements where ORM Mapping could be an impediment then
 * SqlQuery and MapBeans could be a good fit (or raw JDBC of course).
 * </p>
 * <p>
 * The SqlQuery is raw sql with tables and columns that returns MapBeans. A
 * MapBean is similar to a LinkedHashMap but with some smarts such as type
 * conversion and 'dirty' checking. You can modify a MapBean and save it back
 * via Ebean.save() and it can automatically use optimistic concurrency checking
 * as well as maintain generated columns such as last updated timestamp and
 * inserted timestamp.
 * </p>
 * <p>
 * You may wish to use SqlQuery and MapBeans when you have a problem that could
 * be better solved by the more dynamic/direct/relational approach (and ORM
 * Mapping is poorly suited).
 * </p>
 * 
 * <pre class="code">
 * // its typically a good idea to use a named query 
 * // and put the sql in the orm.xml instead of in your code
 * 
 * String sql = &quot;select id, name from customer where name like :name and status_code = :status&quot;;
 * 
 * SqlQuery sqlQuery = Ebean.createSqlQuery();
 * sqlQuery.setQuery(sql);
 * sqlQuery.setParameter(&quot;name&quot;, &quot;Acme%&quot;);
 * sqlQuery.setParameter(&quot;status&quot;, &quot;ACTIVE&quot;);
 * 
 * // execute the query returning a List of MapBean objects
 * List&lt;MapBean&gt; list = sqlQuery.findList();
 * </pre>
 * 
 * <p>
 * If you are looking to modify the MapBeans and save them back you need to
 * specify the "base table" for the query. This is the table that the MapBeans
 * with insert/update/delete. The properties related to the base table are
 * detected (others are ignored).
 * </p>
 * 
 * <pre class="code">
 * ...
 * sqlQuery.setQuery(sql);
 * sqlQuery.setBaseTable(&quot;customer&quot;);
 * ...
 * 
 * // execute the query returning a List of MapBean objects
 * List&lt;MapBean&gt; list = sqlQuery.findList();
 * ...
 * mapBean.set(&quot;name&quot;,&quot;I'm a Changed Man!&quot;);
 * 
 *  // saves with optimistic concurrency checking 
 *  // and maintains last updated timestamp type columns etc
 * Ebean.save(mapBean);
 * 
 * </pre>
 * 
 */
public interface SqlQuery extends Serializable {

	/**
	 * Set the sql query.
	 */
	public SqlQuery setQuery(String sql);

	/**
	 * Set the "base table" for the MapBeans.
	 * <p>
	 * If MapBeans are saved then this is the table that the MapBeans will save
	 * to.
	 * </p>
	 * <p>
	 * Your query could join various tables and have columns coming from several
	 * tables. When you save a MapBean only columns in the "base table" are used
	 * and any other columns are ignored.
	 * </p>
	 */
	public SqlQuery setBaseTable(String baseTable);

	/**
	 * Execute the query returning a list.
	 */
	public List<MapBean> findList();

	/**
	 * Execute the query returning a set.
	 */
	public Set<MapBean> findSet();

	/**
	 * Execute the query returning a map.
	 */
	public Map<?, MapBean> findMap();

	/**
	 * Execute the query returning a single row or null.
	 * <p>
	 * If this query finds 2 or more rows then it will throw a
	 * PersistenceException.
	 * </p>
	 */
	public MapBean findUnique();

	/**
	 * Set an ordered bind parameter according to its position. Note that the
	 * position starts at 1 to be consistent with JDBC PreparedStatement. You
	 * need to set a parameter value for each ? you have in the query.
	 */
	public SqlQuery set(int position, Object value);

	/**
	 * Set a named bind parameter. Named parameters have a colon to prefix the
	 * name.
	 */
	public SqlQuery set(String name, Object value);

	/**
	 * @deprecated use {@link #set(int, Object)} or
	 *             {@link #setParameter(String, Object)}
	 */
	public SqlQuery bind(int position, Object value);

	/**
	 * @deprecated use {@link #set(String, Object)} or
	 *             {@link #setParameter(String, Object)}.
	 */
	public SqlQuery bind(String name, Object value);

	/**
	 * The same as bind for named parameters.
	 */
	public SqlQuery setParameter(String name, Object value);

	/**
	 * The same as bind for positioned parameters.
	 */
	public SqlQuery setParameter(int position, Object value);

	/**
	 * Set a listener to process the query on a row by row basis.
	 * <p>
	 * It this case the rows are not loaded into the persistence context and
	 * instead can be processed by the query listener.
	 * </p>
	 * <p>
	 * Use this when you want to process a large query and do not want to hold
	 * the entire query result in memory.
	 * </p>
	 */
	public SqlQuery setListener(SqlQueryListener queryListener);

	/**
	 * Set the index of the first row of the results to return.
	 */
	public SqlQuery setFirstRow(int firstRow);

	/**
	 * Set the maximum number of query results to return.
	 */
	public SqlQuery setMaxRows(int maxRows);

	/**
	 * Set the index after which fetching continues in a background thread.
	 */
	public SqlQuery setBackgroundFetchAfter(int backgroundFetchAfter);

	/**
	 * Set the initial capacity of List Set or Map collections.
	 */
	public SqlQuery setInitialCapacity(int initialCapacity);

	/**
	 * Set the column to use to determine the keys for a Map.
	 */
	public SqlQuery setMapKey(String mapKey);

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
	public SqlQuery setTimeout(int secs);

}
