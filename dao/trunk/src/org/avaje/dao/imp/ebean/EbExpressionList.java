package org.avaje.dao.imp.ebean;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.avaje.dao.DaoExpressionList;
import org.avaje.dao.DaoQuery;

import com.avaje.ebean.expression.ExpressionList;

public class EbExpressionList<T> implements DaoExpressionList<T> {

	private static final long serialVersionUID = 6439196524908948860L;

	ExpressionList<T> list;
	
	DaoQuery<T> query;
	
	public EbExpressionList(DaoQuery<T> query, ExpressionList<T> list) {
		this.list = list;
		this.query = query;
	}
	
	public DaoExpressionList<T> allEq(Map<String, Object> propertyMap) {
		list.allEq(propertyMap);
		return this;
	}

	public DaoExpressionList<T> between(String propertyName, Object value1, Object value2) {
		list.between(propertyName, value1, value2);
		return this;
	}

	public DaoExpressionList<T> contains(String propertyName, String value) {
		list.contains(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> endsWith(String propertyName, String value) {
		list.endsWith(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> eq(String propertyName, Object value) {
		list.eq(propertyName, value);
		return this;
	}

	public List<T> findList() {
		return list.findList();
	}

	public Map<?, T> findMap() {
		return list.findMap();
	}

	public Set<T> findSet() {
		return list.findSet();
	}

	public T findUnique() {
		return list.findUnique();
	}

	public DaoExpressionList<T> ge(String propertyName, Object value) {
		list.ge(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> gt(String propertyName, Object value) {
		list.gt(propertyName, value);
		return this;
	}

//	public DaoExpressionList<T> having() {
//		list.having();
//		return this;
//	}

	public DaoExpressionList<T> icontains(String propertyName, String value) {
		list.icontains(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> idEq(Object value) {
		list.idEq(value);
		return this;
	}

	public DaoExpressionList<T> iendsWith(String propertyName, String value) {
		list.iendsWith(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> ieq(String propertyName, String value) {
		list.ieq(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> ilike(String propertyName, String value) {
		list.ilike(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> in(String propertyName, Collection<?> values) {
		list.in(propertyName, values);
		return this;
	}

	public DaoExpressionList<T> in(String propertyName, Object[] values) {
		list.in(propertyName, values);
		return this;
	}

	public DaoExpressionList<T> isNotNull(String propertyName) {
		list.isNotNull(propertyName);
		return this;
	}

	public DaoExpressionList<T> isNull(String propertyName) {
		list.isNull(propertyName);
		return this;
	}

	public DaoExpressionList<T> istartsWith(String propertyName, String value) {
		list.istartsWith(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> le(String propertyName, Object value) {
		list.le(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> like(String propertyName, String value) {
		list.like(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> lt(String propertyName, Object value) {
		list.lt(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> ne(String propertyName, Object value) {
		list.ne(propertyName, value);
		return this;
	}

	public DaoQuery<T> orderBy(String orderBy) {
		list.orderBy(orderBy);
		return query;
	}

	public DaoQuery<T> query() {
		return query;
	}

	public DaoQuery<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
		return query.setBackgroundFetchAfter(backgroundFetchAfter);
	}

	public DaoQuery<T> setFirstRow(int firstRow) {
		return query.setFirstRow(firstRow);
	}

//	public DaoQuery<T> setListener(QueryListener<T> queryListener) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public DaoQuery<T> setMapKey(String mapKey) {
		return query.setMapKey(mapKey);
	}

	public DaoQuery<T> setMaxRows(int maxRows) {
		return query.setMaxRows(maxRows);
	}

	public DaoQuery<T> setOrderBy(String orderBy) {
		return query.setOrderBy(orderBy);
	}

	public DaoQuery<T> setUseCache(boolean useCache) {
		return query.setUseCache(useCache);
	}

	public DaoExpressionList<T> startsWith(String propertyName, String value) {
		list.startsWith(propertyName, value);
		return this;
	}

	public DaoExpressionList<T> where() {
		return this;
	}

	
}
