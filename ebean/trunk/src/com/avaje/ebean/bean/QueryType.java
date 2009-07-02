package com.avaje.ebean.bean;

/**
 * The type of query result.
 */
public enum QueryType {

	/**
	 * Find rowCount.
	 */
	ROWCOUNT,

	/**
	 * Find by Id or unique returning a single bean.
	 */
	BEAN,
	
	/**
	 * Find returning a List.
	 */
	LIST,
	
	/**
	 * Find returning a Set.
	 */
	SET,
	
	/**
	 * Find returning a Map.
	 */
	MAP
}
