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
 * Defines the configuration options for a query join.
 * <p>
 * A join/association in Ebean can be loaded in 3 ways:
 * <ul>
 * <li>A "fetch join" ... aka a normal join</li>
 * <li>via "lazy loading" ... when no join defined or marked as lazy</li>
 * <li>via "query join" ... loaded via a second query (aka like lazy loading but loaded immediately)</li>
 * </ul>
 * </p>
 * <p>
 * You can use this to define it the join specified should instead use lazy loading
 * and or a "query join"/"second query".
 * </p>
 * <pre>
 * List&lt;Order&gt; list = Ebean.find(Order.class).join(&quot;customer&quot;,
 * 		new JoinConfig().query(3).lazy(10)).findList();
 * </pre>
 * <p>
 * Find Orders <br/>
 * .. then immediately Load the first 3 customers referenced by those orders<br/>
 * .. after that lazy load customers 10 at a time as needed<br/>
 * </p>
 * 
 * @authors mario rbygrave
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
	 * Specify that this join should instead be invoked as a secondary query
	 * (rather than a fetch join).
	 * <p>
	 * This will use the default size for secondary queries.
	 * </p>
	 */
	public JoinConfig query() {
		this.queryBatchSize = 0;
		return this;
	}

	/**
	 * Specify that this join should instead be invoked as a secondary query
	 * (rather than a fetch join).
	 * <p>
	 * The queryBatchSize is the number of parent id's that this secondary query
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
	 * Return the batch size for secondary query.
	 */
	public int getQueryBatchSize() {
		return queryBatchSize;
	}
}
