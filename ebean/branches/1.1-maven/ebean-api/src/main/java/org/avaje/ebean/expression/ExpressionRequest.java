package org.avaje.ebean.expression;

import java.util.ArrayList;

import org.avaje.ebean.server.core.QueryRequest;

/**
 * Request object used for gathering expression sql and bind values.
 */
public interface ExpressionRequest {

	/**
	 * Return the associated QueryRequest.
	 */
	public QueryRequest getQueryRequest();
	
	/**
	 * Append to the expression sql.
	 */
	public ExpressionRequest append(String sql);
	
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
