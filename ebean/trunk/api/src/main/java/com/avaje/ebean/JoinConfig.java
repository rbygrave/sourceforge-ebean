/**
 * Copyright (C) 2009 Authors
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package com.avaje.ebean;

/**
 * Defines the configuration options for a "query join" or a "lazy loading join". 
 * This gives you the ability to use "query joins" as opposed to "fetch joins" 
 * and also the ability to control the lazy loading queries (batch size, selected properties and
 * joins).
 * <p>
 * We can use this to optimise the performance/behaviour of object graph loading for a given use case.
 * That is, there are cases where using multiple queries is more efficient that a single query and or
 * we want to optimise the lazy loading behaviour (avoid N+1 queries etc).
 * </p>
 * <p>
 * Any time you want to load multiple OneToMany associations it will likely be
 * more performant as multiple SQL queries. If a single SQL query was used that 
 * would result in a Cartesian product.
 * </p>
 * <p>
 * There can also be cases loading across a single OneToMany where 2 SQL queries 
 * (using Ebean "query join") can be more efficient than one SQL query (using Ebean "fetch join"). 
 * When the "One" side is wide (lots of columns) and the cardinality difference is high 
 * (a lot of "Many" beans per "One" bean) then this can be more efficient 
 * loaded as 2 SQL queries.
 * </p>
 * <p>
 * The reason you would want to control the lazy loading query is to
 * optimise performance for further lazy loading (avoid N+1 queries, 
 * load what is required and no more etc).
 * </p>
 * <p>
 * A join/association in Ebean can be loaded in 3 ways:
 * <ul>
 * <li>A "fetch join" ... aka a normal SQL join fetching the associated data in
 * a single query and defined via {@link Query#join(String)} or {@link Query#join(String, String)}</li>
 * 
 * <li>via "lazy loading" ... when no join is defined or the join is 
 * marked via {@link #lazy()} or {@link #lazy(int)}</li>
 * 
 * <li>via "query join" ... loaded via a second SQL query 
 * and marked via {@link #query()} or {@link #query(int)}</li>
 * </ul>
 * </p>
 * <p>
 * Note: "lazy loading" occurs on demand when the application navigates to part of the object
 * graph that is not yet loaded. Ebean will automatically fetch the data via a "lazy loading query".
 * </p>
 * <p>
 * You use JoinConfig to mark a join as a "query join" or a "lazy load join".
 * <ul>
 * <li>For "query join", a second SQL query is executed immediately after the original query</li>
 * <li>For a "lazy load join", it is not until the application navigates
 * to that part of the object graph (and it has not already been loaded) that the "lazy loading query"
 * is actually executed. If the application does not navigate to that part of the object graph the
 * lazy loading query is never executed. It could be described as load on demand.</li>
 * </ul>
 * </p>
 * 
 * <pre class="code">
 * // Normal fetch join results in a single SQL query 
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .join(&quot;details&quot;)
 *   .findList();
 * 
 * // Find Orders join details using a single SQL query
 * </pre>
 * <p>
 * Example: Using a "query join" instead of a "fetch join" we instead use 2 SQL
 * queries
 * </p>
 * 
 * <pre class="code">
 * // This will use 2 SQL queries to build this object graph
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .join(&quot;details&quot;,new JoinConfig().query())
 *   .findList();
 * 
 * // query 1)  find order 
 * // query 2)  find orderDetails where order.id in (?,?...) // first 100 order id's
 * </pre>
 * <p>
 * Example: Using 2 "query joins"
 * </p>
 * 
 * <pre class="code">
 * // This will use 3 SQL queries to build this object graph
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .join(&quot;details&quot;, new JoinConfig().query())
 *   .join(&quot;customer&quot;, new JoinConfig().query(5))
 *   .findList();
 * 
 * // query 1) find order 
 * // query 2) find orderDetails where order.id in (?,?...) // first 100 order id's
 * // query 3) find customer where id in (?,?,?,?,?) // first 5 customers
 * </pre>
 * <p>
 * Example: Using "query joins" and partial objects
 * </p>
 * 
 * <pre class="code">
 * // This will use 3 SQL queries to build this object graph
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .select(&quot;status, shipDate&quot;)
 *   
 *   .join(&quot;details&quot;, &quot;quantity, price&quot;, new JoinConfig().query())
 *   .join(&quot;details.product&quot;, &quot;sku, name&quot;)
 *   
 *   .join(&quot;customer&quot;, &quot;name&quot;, new JoinConfig().query(10))
 *   .join(&quot;customer.contacts&quot;)
 *   .join(&quot;customer.shippingAddress&quot;)
 *   .findList();
 * 
 * // query 1) find order (status, shipDate)
 * // query 2) find orderDetail (quantity, price) join product (sku, name) where order.id in (?,? ...)
 * // query 3) find customer (name) join contacts (*) join shippingAddress (*) where id in (?,?,?,?,?)
 * 
 * 
 * // Note: the join to "details.product" is automatically included into the 
 * //       "details" query join
 * //
 * // Note: the joins to "customer.contacts" and "customer.shippingAddress" 
 * //       are automatically included in the "customer" query join
 * </pre>
 * <p>
 * You can use query() and lazy together on a single join. The query is executed immediately and the lazy 
 * defines the batch size to use for further lazy loading (if lazy loading is invoked).
 * </p>
 * 
 * <pre class="code">
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .join(&quot;customer&quot;,new JoinConfig().query(3).lazy(10))
 *   .findList();
 * 
 * // query 1) find order 
 * // query 2) find customer where id in (?,?,?) // first 3 customers 
 * // .. then if lazy loading of customers is invoked 
 * // .. use a batch size of 10 to load the customers
 * 
 * </pre>
 * 
 * <p>
 * Example of controlling the lazy loading query:
 * </p>
 * <p>
 * This gives us the ability to optimise the lazy loading query
 * for a given use case.
 * </p>
 * 
 * <pre class="code">
 * List&lt;Order&gt; list = Ebean.find(Order.class)
 *   .join(&quot;customer&quot;,&quot;name&quot;, new JoinConfig().lazy(5))
 *   .join(&quot;customer.contacts&quot;,&quotcontactName, phone, email&quot)
 *   .join(&quot;customer.shippingAddress&quot;)
 *   .where().eq(&quot;status&quot;,Order.Status.NEW)
 *   .findList();
 * 
 * // query 1) find order where status = Order.Status.NEW
 * //  
 * // .. if lazy loading of customers is invoked 
 * // .. use a batch size of 5 to load the customers 
 *  
 *       find customer (name) 
 *       join contact (contactName, phone, email) 
 *       join shippingAddress (*) 
 *       where id in (?,?,?,?,?)
 * 
 * </pre> 
 * 
 * @author mario
 * @author rbygrave
 */
public class JoinConfig {

	private int lazyBatchSize = -1;
	
	private int queryBatchSize = -1;
	
	/**
	 * Construct the join configuration object.
	 */
	public JoinConfig() {
	}
	
	/**
	 * Specify that this join should be lazy loaded using 
	 * the default batch load size.
	 */
	public JoinConfig lazy() {
		this.lazyBatchSize = 0;
		return this;
	}

	/**
	 * Specify that this join should be lazy loaded with a specified batch size.
	 * 
	 * @param lazyBatchSize
	 *            the batch size for lazy loading
	 */
	public JoinConfig lazy(int lazyBatchSize) {
		this.lazyBatchSize = lazyBatchSize;
		return this;
	}

	/**
	 * Specify that this join should instead be invoked as a "query join"
	 * (rather than a fetch join).
	 * <p>
	 * This will use the default batch size for "query join" which is 100.
	 * </p>
	 */
	public JoinConfig query() {
		this.queryBatchSize = 0;
		return this;
	}

	/**
	 * Specify that this join should instead be invoked as a "query join"
	 * (rather than a fetch join).
	 * <p>
	 * The queryBatchSize is the number of parent id's that this "query join"
	 * will load.
	 * </p>
	 */
	public JoinConfig query(int queryBatchSize) {
		this.queryBatchSize = queryBatchSize;
		return this;
	}

	/**
	 * Return the batch size for lazy loading.
	 */
	public int getLazyBatchSize() {
		return lazyBatchSize;
	}

	/**
	 * Return the batch size for "query join".
	 */
	public int getQueryBatchSize() {
		return queryBatchSize;
	}
}
