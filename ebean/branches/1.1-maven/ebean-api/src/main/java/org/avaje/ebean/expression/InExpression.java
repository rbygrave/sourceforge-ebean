package org.avaje.ebean.expression;

import java.util.Collection;

class InExpression implements Expression {

	private static final long serialVersionUID = 3150665801693551260L;

	String propertyName;
	
	Object[] values;
	
	InExpression(String propertyName, Collection<?> coll){
		this.propertyName = propertyName;
		values = coll.toArray(new Object[coll.size()]);
	}
	
	InExpression(String propertyName, Object[] array){
		this.propertyName = propertyName;
		this.values = array;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void addBindValues(ExpressionRequest request) {

		for (int i = 0; i < values.length; i++) {
			request.addBindValue(values[i]);
		}
	}

	public void addSql(ExpressionRequest request) {
		request.append(propertyName).append(" in ( ?");
		for (int i = 1; i < values.length; i++) {
			
			request.append(", ?");
		}
		
		request.append(" ) ");
	}

	/**
	 * Based on the number of values in the in clause.
	 */
	public int queryPlanHash() {
		return InExpression.class.hashCode() + 31 * values.length;
	}

	public int queryBindHash() {
		int hc = 0;
		for (int i = 1; i < values.length; i++) {
			hc = 31*hc + values[i].hashCode();
		}
		return hc;
	}
	
	
}
