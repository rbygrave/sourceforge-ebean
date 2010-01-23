package com.avaje.ebean.util;

import java.util.ArrayList;

import com.avaje.ebean.internal.SpiExpressionRequest;
import com.avaje.ebean.server.core.OrmQueryRequest;
import com.avaje.ebean.server.deploy.BeanDescriptor;

public class DefaultExpressionRequest implements SpiExpressionRequest {

	private final OrmQueryRequest<?> queryRequest;
	
	private final StringBuilder sb = new StringBuilder();
	
	private final ArrayList<Object> bindValues = new ArrayList<Object>();
	
	public DefaultExpressionRequest(OrmQueryRequest<?> queryRequest) {
		this.queryRequest = queryRequest;
	}

    public BeanDescriptor<?> getBeanDescriptor(){
		return queryRequest.getBeanDescriptor();
	}
	
	public OrmQueryRequest<?> getQueryRequest() {
		return queryRequest;
	}


	public SpiExpressionRequest append(String sql) {
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
