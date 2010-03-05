package com.avaje.ebeaninternal.server.expression;

import java.util.Collection;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;

class InExpression extends AbstractExpression {

	private static final long serialVersionUID = 3150665801693551260L;
	
	private final Object[] values;
	
	InExpression(String propertyName, Collection<?> coll){
		super(propertyName);
		values = coll.toArray(new Object[coll.size()]);
	}
	
	InExpression(String propertyName, Object[] array){
		super(propertyName);
		this.values = array;
	}

	public void addBindValues(SpiExpressionRequest request) {

		for (int i = 0; i < values.length; i++) {
			request.addBindValue(values[i]);
		}
	}

	public void addSql(SpiExpressionRequest request) {
	    
	    if (values.length == 0){
	        // 'no match' for in empty collection
            request.append("1=0");
            return;
	    }
	    
		request.append(propertyName).append(" in ( ?");
		for (int i = 1; i < values.length; i++) {
			
			request.append(", ?");
		}
		
		request.append(" ) ");
	}

	/**
	 * Based on the number of values in the in clause.
	 */
	public int queryAutoFetchHash() {
		return InExpression.class.getName().hashCode() + 31 * values.length;
	}

	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		int hc = 0;
		for (int i = 1; i < values.length; i++) {
			hc = 31*hc + values[i].hashCode();
		}
		return hc;
	}
	
	
}
