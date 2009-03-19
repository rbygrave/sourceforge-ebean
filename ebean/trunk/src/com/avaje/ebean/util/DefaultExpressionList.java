package com.avaje.ebean.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.Query;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.expression.Expr;
import com.avaje.ebean.expression.Expression;
import com.avaje.ebean.expression.ExpressionList;
import com.avaje.ebean.expression.ExpressionRequest;
import com.avaje.ebean.expression.InternalExpressionList;
import com.avaje.ebean.expression.Junction;
import com.avaje.ebean.server.core.QueryRequest;

/**
 * Default implementation of ExpressionList.
 */
public final class DefaultExpressionList<T> implements InternalExpressionList<T> {

	private static final long serialVersionUID = -6992345500247035947L;

	final ArrayList<Expression> list = new ArrayList<Expression>();
	
	final Query<T> query;
	
	public DefaultExpressionList(Query<T> query) {
		this.query = query;
	}
	
	/**
	 * Return a copy of the expression list.
	 * <p>
	 * Each of the expressions are expected to be immutable and safe to reference.
	 * </p>
	 */
	public DefaultExpressionList<T> copy(Query<T> query) {
		DefaultExpressionList<T> copy = new DefaultExpressionList<T>(query);
		copy.list.addAll(list);
		return copy;
	}
	
	public Query<T> query() {
		return query;
	}
	
	public ExpressionList<T> where() {
		return query.where();
	}
	
	public Query<T> orderBy(String orderBy) {
		return query.orderBy(orderBy);
	}

	public Query<T> setOrderBy(String orderBy) {
		return query.orderBy(orderBy);
	}
	
	public List<T> findList() {
		return query.findList();
	}

	public Set<T> findSet() {
		return query.findSet();
	}

	public Map<?,T> findMap() {
		return query.findMap();
	}

	public T findUnique() {
		return query.findUnique();
	}
	
	public Query<T> setFirstRow(int firstRow){
		return query.setFirstRow(firstRow);
	}
	
	public Query<T> setMaxRows(int maxRows){
		return query.setMaxRows(maxRows);
	}
	
	public Query<T> setBackgroundFetchAfter(int backgroundFetchAfter){
		return query.setBackgroundFetchAfter(backgroundFetchAfter);
	}
	
	public Query<T> setMapKey(String mapKey){
		return query.setMapKey(mapKey);
	}
	
	public Query<T> setListener(QueryListener<T> queryListener){
		return query.setListener(queryListener);
	}
	
	public Query<T> setUseCache(boolean useCache){
		return query.setUseCache(useCache);
	}
	
	public ExpressionList<T> having(){
		return query.having();
	}
	
	public DefaultExpressionList<T> add(Expression expr) {
		list.add(expr);
		return this;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}

	public String buildSql(ExpressionRequest request) {
		
		for (int i = 0, size=list.size(); i < size; i++) {
			Expression expression = list.get(i);
			if (i > 0){
				request.append(" and ");
			}
			expression.addSql(request);
		}
		return request.getSql();
	}
	

	public ArrayList<Object> buildBindValues(ExpressionRequest request) {
	
		for (int i = 0, size=list.size(); i < size; i++) {
			Expression expression = list.get(i);
			expression.addBindValues(request);
		}
		return request.getBindValues();
	}
	
	/**
	 * Calculate a hash based on the expressions but excluding the actual bind values.
	 */
	public int queryAutoFetchHash() {
		int hash = DefaultExpressionList.class.getName().hashCode();
		for (int i = 0, size=list.size(); i < size; i++) {
			Expression expression = list.get(i);
			hash = hash*31 + expression.queryAutoFetchHash();
		}
		return hash;
	}
	
	/**
	 * Calculate a hash based on the expressions but excluding the actual bind values.
	 */
	public int queryPlanHash(QueryRequest request) {
		int hash = DefaultExpressionList.class.getName().hashCode();
		for (int i = 0, size=list.size(); i < size; i++) {
			Expression expression = list.get(i);
			hash = hash*31 + expression.queryPlanHash(request);
		}
		return hash;
	}
	
	/**
	 * Calculate a hash based on the expressions.
	 */
	public int queryBindHash() {
		int hash = DefaultExpressionList.class.getName().hashCode();
		for (int i = 0, size=list.size(); i < size; i++) {
			Expression expression = list.get(i);
			hash = hash*31 + expression.queryBindHash();
		}
		return hash;
	}
	
	public ExpressionList<T> eq(String propertyName, Object value) {
		add(Expr.eq(propertyName, value));
		return this;
	}

	public ExpressionList<T> ieq(String propertyName, String value) {
		add(Expr.ieq(propertyName, value));
		return this;
	}

	public ExpressionList<T> ne(String propertyName, Object value) {
		add(Expr.ne(propertyName, value));
		return this;
	}

	public ExpressionList<T> allEq(Map<String, Object> propertyMap) {
		add(Expr.allEq(propertyMap));
		return this;
	}

	public ExpressionList<T> and(Expression expOne, Expression expTwo) {
		add(Expr.and(expOne, expTwo));
		return this;
	}

	public ExpressionList<T> between(String propertyName, Object value1, Object value2) {
		add(Expr.between(propertyName, value1, value2));
		return this;
	}

	public Junction conjunction() {
		Junction conjunction = Expr.conjunction();
		add(conjunction);
		return conjunction;
	}

	public ExpressionList<T> contains(String propertyName, String value) {
		add(Expr.contains(propertyName, value));
		return this;
	}

	public Junction disjunction() {
		Junction disjunction = Expr.disjunction();
		add(disjunction);
		return disjunction;
	}

	public ExpressionList<T> endsWith(String propertyName, String value) {
		add(Expr.endsWith(propertyName, value));
		return this;
	}

	public ExpressionList<T> ge(String propertyName, Object value) {
		add(Expr.ge(propertyName, value));
		return this;
	}
		
	public ExpressionList<T> gt(String propertyName, Object value) {
		add(Expr.gt(propertyName, value));
		return this;
	}

	public ExpressionList<T> icontains(String propertyName, String value) {
		add(Expr.icontains(propertyName, value));
		return this;
	}

	public ExpressionList<T> idEq(Object value) {
		add(Expr.idEq(value));
		return this;
	}

	public ExpressionList<T> iendsWith(String propertyName, String value) {
		add(Expr.iendsWith(propertyName, value));
		return this;
	}

	public ExpressionList<T> ilike(String propertyName, String value) {
		add(Expr.ilike(propertyName, value));
		return this;
	}

	public ExpressionList<T> in(String propertyName, Query<?> subQuery){
		add(Expr.in(propertyName, subQuery));
		return this;	
	}
	
	public ExpressionList<T> in(String propertyName, Collection<?> values) {
		add(Expr.in(propertyName, values));
		return this;
	}

	public ExpressionList<T> in(String propertyName, Object[] values) {
		add(Expr.in(propertyName, values));
		return this;
	}

	public ExpressionList<T> isNotNull(String propertyName) {
		add(Expr.isNotNull(propertyName));
		return this;
	}

	public ExpressionList<T> isNull(String propertyName) {
		add(Expr.isNull(propertyName));
		return this;
	}

	public ExpressionList<T> istartsWith(String propertyName, String value) {
		add(Expr.istartsWith(propertyName, value));
		return this;
	}

	public ExpressionList<T> le(String propertyName, Object value) {
		add(Expr.le(propertyName, value));
		return this;
	}

	public ExpressionList<T> exampleLike(Object example) {
		add(Expr.exampleLike(example));
		return this;
	}

	public ExpressionList<T> iexampleLike(Object example) {
		add(Expr.iexampleLike(example));
		return this;
	}

	public ExpressionList<T> like(String propertyName, String value) {
		add(Expr.like(propertyName, value));
		return this;
	}
		
	public ExpressionList<T> lt(String propertyName, Object value) {
		add(Expr.lt(propertyName, value));
		return this;
	}

	public ExpressionList<T> not(Expression exp) {
		add(Expr.not(exp));
		return this;
	}

	public ExpressionList<T> or(Expression expOne, Expression expTwo) {
		add(Expr.or(expOne, expTwo));
		return this;
	}

	public ExpressionList<T> raw(String raw, Object value) {
		add(Expr.raw(raw, value));
		return this;
	}

	public ExpressionList<T> raw(String raw, Object[] values) {
		add(Expr.raw(raw, values));
		return this;
	}

	public ExpressionList<T> raw(String raw) {
		add(Expr.raw(raw));
		return this;
	}

	public ExpressionList<T> startsWith(String propertyName, String value) {
		add(Expr.startsWith(propertyName, value));
		return this;
	}
	
}
