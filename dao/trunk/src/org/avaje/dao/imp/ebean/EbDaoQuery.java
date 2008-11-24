package org.avaje.dao.imp.ebean;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.avaje.dao.DaoExpressionList;
import org.avaje.dao.DaoQuery;

import com.avaje.ebean.Query;

public class EbDaoQuery<T> implements DaoQuery<T> {

	private static final long serialVersionUID = 5480853800536176769L;

	final Query<T> query;

	final DaoExpressionList<T> where;

	public EbDaoQuery(Query<T> query){
		this.query = query;
		where = new EbExpressionList<T>(this, query.where());
	}
	
	public List<T> findList() {
		return query.findList();
	}

	public Map<?, T> findMap() {
		return query.findMap();
	}

	public Set<T> findSet() {
		return query.findSet();
	}

	public T findUnique() {
		return query.findUnique();
	}

	public String getGeneratedSql() {
		return query.getGeneratedSql();
	}

	public boolean isAutoFetchTuned() {
		return query.isAutoFetchTuned();
	}

	public DaoQuery<T> join(String assocProperty, String fetchProperties) {
		query.join(assocProperty, fetchProperties);
		return this;
	}

	public DaoQuery<T> join(String assocProperty) {
		query.join(assocProperty);
		return this;
	}

	public DaoQuery<T> orderBy(String orderBy) {
		query.orderBy(orderBy);
		return this;
	}

	public DaoQuery<T> select(String fetchProperties) {
		query.select(fetchProperties);
		return this;
	}

	public DaoQuery<T> set(int position, Object value) {
		query.set(position, value);
		return this;
	}

	public DaoQuery<T> set(String name, Object value) {
		query.set(name, value);
		return this;
	}

	public void setAutoFetch(boolean autoFetch) {
		query.setAutoFetch(autoFetch);
	}

	public DaoQuery<T> setBackgroundFetchAfter(int backgroundFetchAfter) {
		query.setBackgroundFetchAfter(backgroundFetchAfter);
		return this;
	}

	public DaoQuery<T> setDistinct(boolean isDistinct) {
		query.setDistinct(isDistinct);
		return this;
	}

	public DaoQuery<T> setFirstRow(int firstRow) {
		// TODO Auto-generated method stub
		return null;
	}

	public DaoQuery<T> setId(Object id) {
		query.setId(id);
		return this;
	}


	public DaoQuery<T> setMapKey(String mapKey) {
		query.setMapKey(mapKey);
		return this;
	}

	public DaoQuery<T> setMaxRows(int maxRows) {
		query.setMaxRows(maxRows);
		return this;
	}

	public DaoQuery<T> setOrderBy(String orderBy) {
		query.setOrderBy(orderBy);
		return this;
	}

	public DaoQuery<T> setParameter(int position, Object value) {
		query.setParameter(position, value);
		return this;
	}

	public DaoQuery<T> setParameter(String name, Object value) {
		query.setParameter(name, value);
		return this;
	}

	public DaoQuery<T> setQuery(String oql) {
		query.setQuery(oql);
		return this;
	}

	public DaoQuery<T> setTimeout(int secs) {
		query.setTimeout(secs);
		return this;
	}

	public DaoQuery<T> setUseCache(boolean useCache) {
		query.setUseCache(useCache);
		return this;
	}


	public DaoExpressionList<T> where() {
		
		return where;
	}

	
}
