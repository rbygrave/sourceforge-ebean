package com.avaje.ebeaninternal.api;

import java.util.ArrayList;

import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;

import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.query.LuceneResolvableRequest;

/**
 * Internal extension of ExpressionList.
 */
public interface SpiExpressionList<T> extends ExpressionList<T> {

    public boolean isLuceneResolvable(LuceneResolvableRequest req);
    
    public Query createLuceneQuery(SpiExpressionRequest request, Occur occur) throws ParseException;

    /**
     * Trim the path for filterMany() expressions.
     */
    public void trimPath(int prefixTrim);
    
	/**
	 * Restore the ExpressionFactory after deserialisation.
	 */
	public void setExpressionFactory(ExpressionFactory expr);

    /**
     * Process "Many" properties populating ManyWhereJoins.
     * <p>
     * Predicates on Many properties require an extra independent
     * join clause.
     * </p>
     */
	public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoins);
	
	/**
	 * Return true if this list is empty.
	 */
	public boolean isEmpty();

	/**
	 * Concatenate the expression sql into a String.
	 * <p>
	 * The list of expressions are evaluated in order building a sql statement
	 * with bind parameters.
	 * </p>
	 */
	public String buildSql(SpiExpressionRequest request);

	/**
	 * Combine the expression bind values into a list.
	 * <p>
	 * Expressions are evaluated in order and all the resulting bind values are
	 * returned as a List.
	 * </p>
	 * 
	 * @return the list of all the bind values in order.
	 */
	public ArrayList<Object> buildBindValues(SpiExpressionRequest request);
	
    /**
     * Calculate a hash based on the expressions but excluding the actual bind
     * values.
     */
    public int queryPlanHash(BeanQueryRequest<?> request);

}
