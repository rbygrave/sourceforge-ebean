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

import java.util.List;
import java.util.concurrent.Future;

/**
 * Used to page through a query result rather than fetching all the results in a
 * single query.
 * <p>
 * Has the ability to use background threads to 'fetch ahead' the next page and
 * get the total row count. 
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <T>
 *            the entity bean type
 */
public interface PagingList<T> {

	/**
	 * Refresh will clear all the pages and row count forcing them
	 * to be re-fetched when next required. 
	 */
	public void refresh();
	
	//public void fetchAll();
	//public String? getOrderBy();
	//public void setOrderBy(String?);
	
	/**
	 * Set the number of pages to fetch ahead using background fetching.
	 * <p>
	 * If set to 0 no fetch ahead is used. If set to 1 then the next page is
	 * fetched in the background as soon as the list is accessed.
	 * </p>
	 */
	public PagingList<T> setFetchAhead(int fetchAhead);

	/**
	 * Return the Future for getting the total row count.
	 */
	public Future<Integer> getFutureRowCount();

	/**
	 * Return the data in the form of a List.
	 */
	public List<T> getAsList();

	/**
	 * Return the page size.
	 */
	public int getPageSize();

	/**
	 * Return the total row count.
	 * <p>
	 * This gets the result from getFutureRowCount and will wait until that
	 * query has completed.
	 * </p>
	 */
	public int getRowCount();

	/**
	 * Return the total page count.
	 * <p>
	 * This is based on the total row count. This will wait until the row count
	 * has returned if it has not already.
	 * </p>
	 */
	public int getPageCount();

	/**
	 * Return the page for a given page position (starting at 0).
	 */
	public Page<T> getPage(int i);

}
