package com.avaje.ebean.expression;

import java.io.Serializable;

import com.avaje.ebean.server.core.QueryRequest;


/**
 * An expression that becomes part of a Where clause or Having clause.
 */
public interface Expression extends Serializable {

	/**
	 * Return the name of the property this expression is for.
	 * <p>
	 * If there are multiple properties or non null can be returned.
	 * </p>
	 */
	public String getPropertyName();
	
	/**
	 * Calculate a hash value used to identify a query for AutoFetch tuning.
	 * <p>
	 * That is, if the hash changes then the query will be considered different
	 * from an AutoFetch perspective and get different tuning.
	 * </p>
	 */
	public int queryAutoFetchHash();
	
	/**
	 * Calculate a hash value for the expression.
	 * This includes the expression type and property but should exclude
	 * the bind values.
	 * <p>
	 * This is used where queries are the same except for the bind values, in which
	 * case the query execution plan can be reused.
	 * </p>
	 */
	public int queryPlanHash(QueryRequest request);
	
	/**
	 * Return the hash value for the values that will be bound.
	 */
	public int queryBindHash();
	
	/**
	 * Add some sql to the query.
	 * <p>
	 * This will contain ? as a place holder for each associated bind values.
	 * </p>
	 * <p>
	 * The 'sql' added to the query can contain object property names rather
	 * than db tables and columns. This 'sql' is later parsed converting the
	 * logical property names to their full database column names.
	 * </p>
	 * @param request
	 *            the associated request.
	 */
	public void addSql(ExpressionRequest request);

	/**
	 * Add the parameter values to be set against query. For each ? place holder
	 * there should be a corresponding value that is added to the bindList.
	 * 
	 * @param request
	 *            the associated request.
	 */
	public void addBindValues(ExpressionRequest request);
}
