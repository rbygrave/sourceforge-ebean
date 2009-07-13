package com.avaje.ebean.util;

import java.util.ArrayList;

import com.avaje.ebean.internal.InternalExpressionRequest;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;

public class DefaultExpressionRequest implements InternalExpressionRequest {

	final OrmQueryRequest<?> queryRequest;
	
	StringBuilder sb = new StringBuilder();
	
	ArrayList<Object> bindValues = new ArrayList<Object>();
	
	public DefaultExpressionRequest(OrmQueryRequest<?> queryRequest) {
		this.queryRequest = queryRequest;
	}
	
	public BeanDescriptor<?> getBeanDescriptor(){
		return queryRequest.getBeanDescriptor();
	}
	
	public OrmQueryRequest<?> getQueryRequest() {
		return queryRequest;
	}


	public InternalExpressionRequest append(String sql) {
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
