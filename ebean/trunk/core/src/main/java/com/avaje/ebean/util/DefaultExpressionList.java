package com.avaje.ebean.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaje.ebean.Expression;
import com.avaje.ebean.ExpressionFactory;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.FutureList;
import com.avaje.ebean.Junction;
import com.avaje.ebean.OrderBy;
import com.avaje.ebean.PagingList;
import com.avaje.ebean.Query;
import com.avaje.ebean.QueryListener;
import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebean.internal.SpiExpression;
import com.avaje.ebean.internal.SpiExpressionList;
import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;

/**
 * Default implementation of ExpressionList.
 */
public class DefaultExpressionList<T> implements SpiExpressionList<T> {

    private static final long serialVersionUID = -6992345500247035947L;

    private final ArrayList<SpiExpression> list = new ArrayList<SpiExpression>();

    private final Query<T> query;

    private transient ExpressionFactory expr;

    private final String exprLang;
    private final String listAndStart;
    private final String listAndEnd;
    private final String listAndJoin;

    public DefaultExpressionList(Query<T> query, String exprLang) {
        this.query = query;
        this.expr = query.getExpressionFactory();
        this.exprLang = exprLang;
        
        if ("ldap".equals(exprLang)){
            // Language is LDAP
            listAndStart = "(&";
            listAndEnd = ")";
            listAndJoin = "";
            
        } else {
            listAndStart = "";
            listAndEnd = "";
            listAndJoin = " and ";
        }
    }

    /**
     * Set the ExpressionFactory.
     * <p>
     * After deserialisation so that it can be further modified.
     * </p>
     */
    public void setExpressionFactory(ExpressionFactory expr) {
        this.expr = expr;
    }

    /**
     * Return a copy of the expression list.
     * <p>
     * Each of the expressions are expected to be immutable and safe to
     * reference.
     * </p>
     */
    public DefaultExpressionList<T> copy(Query<T> query) {
        DefaultExpressionList<T> copy = new DefaultExpressionList<T>(query, exprLang);
        copy.list.addAll(list);
        return copy;
    }

    /**
     * Return true if one of the expressions is related to a Many property.
     */
    public boolean containsMany(BeanDescriptor<?> desc) {

        for (int i = 0; i < list.size(); i++) {

            if (list.get(i).containsMany(desc)) {
                return true;
            }
        }
        return false;
    }

    public Query<T> query() {
        return query;
    }

    public ExpressionList<T> where() {
        return query.where();
    }

    public OrderBy<T> order() {
        return query.order();
    }

    public Query<T> order(String orderByClause) {
        return query.order(orderByClause);
    }

    public Query<T> orderBy(String orderBy) {
        return query.order(orderBy);
    }

    public Query<T> setOrderBy(String orderBy) {
        return query.order(orderBy);
    }

    public FutureList<T> findFutureList() {
        return query.findFutureList();
    }

    public PagingList<T> findPagingList(int pageSize) {
        return query.findPagingList(pageSize);
    }

    public List<T> findList() {
        return query.findList();
    }

    public Set<T> findSet() {
        return query.findSet();
    }

    public Map<?, T> findMap() {
        return query.findMap();
    }

    public T findUnique() {
        return query.findUnique();
    }

    public Query<T> select(String fetchProperties) {
        return query.select(fetchProperties);
    }

    public Query<T> join(String assocProperties) {
        return query.join(assocProperties);
    }

    public Query<T> join(String assocProperty, String assocProperties) {
        return query.join(assocProperty, assocProperties);
    }

    public Query<T> setFirstRow(int firstRow) {
        return query.setFirstRow(firstRow);
    }

    public Query<T> setMaxRows(int maxRows) {
        return query.setMaxRows(maxRows);
    }

    public Query<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
        return query.setBackgroundFetchAfter(backgroundFetchAfter);
    }

    public Query<T> setMapKey(String mapKey) {
        return query.setMapKey(mapKey);
    }

    public Query<T> setListener(QueryListener<T> queryListener) {
        return query.setListener(queryListener);
    }

    public Query<T> setUseCache(boolean useCache) {
        return query.setUseCache(useCache);
    }

    public ExpressionList<T> having() {
        return query.having();
    }

    public DefaultExpressionList<T> add(Expression expr) {
        list.add((SpiExpression) expr);
        return this;
    }

    public boolean isEmpty() {
        return list.isEmpty();
    }
    
    public String buildSql(SpiExpressionRequest request) {

        request.append(listAndStart);
        for (int i = 0, size = list.size(); i < size; i++) {
            SpiExpression expression = list.get(i);
            if (i > 0) {
                request.append(listAndJoin);
            }
            expression.addSql(request);
        }
        request.append(listAndEnd);
        return request.getSql();
    }

    public ArrayList<Object> buildBindValues(SpiExpressionRequest request) {

        for (int i = 0, size = list.size(); i < size; i++) {
            SpiExpression expression = list.get(i);
            expression.addBindValues(request);
        }
        return request.getBindValues();
    }

    /**
     * Calculate a hash based on the expressions but excluding the actual bind
     * values.
     */
    public int queryAutoFetchHash() {
        int hash = DefaultExpressionList.class.getName().hashCode();
        for (int i = 0, size = list.size(); i < size; i++) {
            SpiExpression expression = list.get(i);
            hash = hash * 31 + expression.queryAutoFetchHash();
        }
        return hash;
    }

    /**
     * Calculate a hash based on the expressions but excluding the actual bind
     * values.
     */
    public int queryPlanHash(BeanQueryRequest<?> request) {
        int hash = DefaultExpressionList.class.getName().hashCode();
        for (int i = 0, size = list.size(); i < size; i++) {
            SpiExpression expression = list.get(i);
            hash = hash * 31 + expression.queryPlanHash(request);
        }
        return hash;
    }

    /**
     * Calculate a hash based on the expressions.
     */
    public int queryBindHash() {
        int hash = DefaultExpressionList.class.getName().hashCode();
        for (int i = 0, size = list.size(); i < size; i++) {
            SpiExpression expression = list.get(i);
            hash = hash * 31 + expression.queryBindHash();
        }
        return hash;
    }

    public ExpressionList<T> eq(String propertyName, Object value) {
        add(expr.eq(propertyName, value));
        return this;
    }

    public ExpressionList<T> ieq(String propertyName, String value) {
        add(expr.ieq(propertyName, value));
        return this;
    }

    public ExpressionList<T> ne(String propertyName, Object value) {
        add(expr.ne(propertyName, value));
        return this;
    }

    public ExpressionList<T> allEq(Map<String, Object> propertyMap) {
        add(expr.allEq(propertyMap));
        return this;
    }

    public ExpressionList<T> and(Expression expOne, Expression expTwo) {
        add(expr.and(expOne, expTwo));
        return this;
    }

    public ExpressionList<T> between(String propertyName, Object value1, Object value2) {
        add(expr.between(propertyName, value1, value2));
        return this;
    }

    public ExpressionList<T> betweenProperties(String lowProperty, String highProperty, Object value) {
        add(expr.betweenProperties(lowProperty, highProperty, value));
        return this;
    }

    public Junction conjunction() {
        Junction conjunction = expr.conjunction();
        add(conjunction);
        return conjunction;
    }

    public ExpressionList<T> contains(String propertyName, String value) {
        add(expr.contains(propertyName, value));
        return this;
    }

    public Junction disjunction() {
        Junction disjunction = expr.disjunction();
        add(disjunction);
        return disjunction;
    }

    public ExpressionList<T> endsWith(String propertyName, String value) {
        add(expr.endsWith(propertyName, value));
        return this;
    }

    public ExpressionList<T> ge(String propertyName, Object value) {
        add(expr.ge(propertyName, value));
        return this;
    }

    public ExpressionList<T> gt(String propertyName, Object value) {
        add(expr.gt(propertyName, value));
        return this;
    }

    public ExpressionList<T> icontains(String propertyName, String value) {
        add(expr.icontains(propertyName, value));
        return this;
    }

    public ExpressionList<T> idIn(List<?> idList) {
        add(expr.idIn(idList));
        return this;
    }

    public ExpressionList<T> idEq(Object value) {
        add(expr.idEq(value));
        return this;
    }

    public ExpressionList<T> iendsWith(String propertyName, String value) {
        add(expr.iendsWith(propertyName, value));
        return this;
    }

    public ExpressionList<T> ilike(String propertyName, String value) {
        add(expr.ilike(propertyName, value));
        return this;
    }

    public ExpressionList<T> in(String propertyName, Query<?> subQuery) {
        add(expr.in(propertyName, subQuery));
        return this;
    }

    public ExpressionList<T> in(String propertyName, Collection<?> values) {
        add(expr.in(propertyName, values));
        return this;
    }

    public ExpressionList<T> in(String propertyName, Object[] values) {
        add(expr.in(propertyName, values));
        return this;
    }

    public ExpressionList<T> isNotNull(String propertyName) {
        add(expr.isNotNull(propertyName));
        return this;
    }

    public ExpressionList<T> isNull(String propertyName) {
        add(expr.isNull(propertyName));
        return this;
    }

    public ExpressionList<T> istartsWith(String propertyName, String value) {
        add(expr.istartsWith(propertyName, value));
        return this;
    }

    public ExpressionList<T> le(String propertyName, Object value) {
        add(expr.le(propertyName, value));
        return this;
    }

    public ExpressionList<T> exampleLike(Object example) {
        add(expr.exampleLike(example));
        return this;
    }

    public ExpressionList<T> iexampleLike(Object example) {
        add(expr.iexampleLike(example));
        return this;
    }

    public ExpressionList<T> like(String propertyName, String value) {
        add(expr.like(propertyName, value));
        return this;
    }

    public ExpressionList<T> lt(String propertyName, Object value) {
        add(expr.lt(propertyName, value));
        return this;
    }

    public ExpressionList<T> not(Expression exp) {
        add(expr.not(exp));
        return this;
    }

    public ExpressionList<T> or(Expression expOne, Expression expTwo) {
        add(expr.or(expOne, expTwo));
        return this;
    }

    public ExpressionList<T> raw(String raw, Object value) {
        add(expr.raw(raw, value));
        return this;
    }

    public ExpressionList<T> raw(String raw, Object[] values) {
        add(expr.raw(raw, values));
        return this;
    }

    public ExpressionList<T> raw(String raw) {
        add(expr.raw(raw));
        return this;
    }

    public ExpressionList<T> startsWith(String propertyName, String value) {
        add(expr.startsWith(propertyName, value));
        return this;
    }

}
