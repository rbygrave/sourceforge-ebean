package com.avaje.ebean.bean;

public enum QueryType {

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
