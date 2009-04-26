package com.avaje.ebean.util;

import java.util.ArrayList;

import com.avaje.ebean.expression.ExpressionRequest;
import com.avaje.ebean.server.core.OrmQueryRequest;

public class DefaultExpressionRequest implements ExpressionRequest {

	final OrmQueryRequest<?> queryRequest;
	
	StringBuilder sb = new StringBuilder();
	
	ArrayList<Object> bindValues = new ArrayList<Object>();
	
	public DefaultExpressionRequest(OrmQueryRequest<?> queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	public OrmQueryRequest<?> getQueryRequest() {
		return queryRequest;
	}


	public ExpressionRequest append(String sql) {
		sb.append(sql);
		return this;
	}

	public void addBindValue(Object bindValue) {
		bindValues.add(bindValue);
	}

	public boolean includeProperty(String propertyName) {
		return true;
	}

	public String getSql() {
		return sb.toString();
	}

	public ArrayList<Object> getBindValues() {
		return bindValues;
	}
	
	
}
