package com.avaje.ebean.internal;

import java.util.ArrayList;

import com.avaje.ebean.event.BeanQueryRequest;

/**
 * Request object used for gathering expression sql and bind values.
 */
public interface InternalExpressionRequest {

	/**
	 * Return the associated QueryRequest.
	 */
	public BeanQueryRequest<?> getQueryRequest();
	
	/**
	 * Append to the expression sql.
	 */
	public InternalExpressionRequest append(String sql);
	
	/**
	 * Add a bind value to this request.
	 */
	public void addBindValue(Object bindValue);
	
	/**
	 * Return the accumulated expression sql for all expressions in this request.
	 */
	public String getSql();
	
	/**
	 * Return the ordered list of bind values for all expressions in this request.
	 */
	public ArrayList<Object> getBindValues();
}
