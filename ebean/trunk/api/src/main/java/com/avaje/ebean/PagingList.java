package com.avaje.ebean;

import java.util.List;
import java.util.concurrent.Future;

public interface PagingList<T> {

	/**
	 * Set the number of pages to fetch ahead using background fetching.
	 * <p>
	 * If set to 0 no fetch ahead is used. If set to 1 then the next page
	 * is fetched in the background as soon as the list is accessed.
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
     * Return the total row count waiting if it has not already been 
     * fetched.
     */
    public int getRowCount();
   
    public int getPageCount();
   
    public Page<T> getPage(int i);

}
