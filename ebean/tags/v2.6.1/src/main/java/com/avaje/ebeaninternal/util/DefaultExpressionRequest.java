package com.avaje.ebeaninternal.util;

import java.util.ArrayList;

import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.core.SpiOrmQueryRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.DeployParser;

public class DefaultExpressionRequest implements SpiExpressionRequest {

	private final SpiOrmQueryRequest<?> queryRequest;
	
	private final StringBuilder sb = new StringBuilder();
	
	private final ArrayList<Object> bindValues = new ArrayList<Object>();
	
	private final DeployParser deployParser;
	
	private int paramIndex;
	
	public DefaultExpressionRequest(SpiOrmQueryRequest<?> queryRequest, DeployParser deployParser) {
		this.queryRequest = queryRequest;
		this.deployParser = deployParser;
	}
	
	public String parseDeploy(String logicalProp) {
        
        String s = deployParser.getDeployWord(logicalProp);
        return s == null ? logicalProp : s;
    }

    /**
	 * Increments the parameter index and returns that value.
	 */
    public int nextParameter() {
        return ++paramIndex;
    }

    public BeanDescriptor<?> getBeanDescriptor(){
		return queryRequest.getBeanDescriptor();
	}
	
	public SpiOrmQueryRequest<?> getQueryRequest() {
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
