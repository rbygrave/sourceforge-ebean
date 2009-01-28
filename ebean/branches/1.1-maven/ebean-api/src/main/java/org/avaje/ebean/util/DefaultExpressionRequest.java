package org.avaje.ebean.util;

import java.util.ArrayList;

import org.avaje.ebean.expression.ExpressionRequest;
import org.avaje.ebean.server.core.QueryRequest;

public class DefaultExpressionRequest implements ExpressionRequest {

	final QueryRequest queryRequest;
	
	StringBuilder sb = new StringBuilder();
	
	ArrayList<Object> bindValues = new ArrayList<Object>();
	
	public DefaultExpressionRequest(QueryRequest queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	public QueryRequest getQueryRequest() {
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
