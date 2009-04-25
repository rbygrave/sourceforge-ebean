package com.avaje.ebean.expression;

import com.avaje.ebean.bean.BeanQueryRequest;


class SimpleExpression implements Expression {

	private static final long serialVersionUID = -382881395755603790L;

	enum Op { 
		EQ {
			public String toString() {
				return "=";
			}
		},
		NOT_EQ {
			public String toString() {
				return "<>";
			}
		},
		LT {
			public String toString() {
				return "<";
			}
		},
		LT_EQ {
			public String toString() {
				return "<=";
			}
		},
		GT {
			public String toString() {
				return ">";
			}
		},
		GT_EQ {
			public String toString() {
				return ">=";
			}
		}
	}
	
	final String propertyName;
	
	final Op type;
	
	final Object value;
	
	SimpleExpression(String propertyName, Op type, Object value) {
		this.propertyName = propertyName;
		this.type = type;
		this.value = value;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
	public void addBindValues(ExpressionRequest request) {
		request.addBindValue(value);
	}
	
	public void addSql(ExpressionRequest request) {
		request.append(propertyName).append(" ").append(type.toString()).append(" ? ");
	}
	
	
	/**
	 * Based on the type and propertyName.
	 */
	public int queryAutoFetchHash() {
		int hc = SimpleExpression.class.getName().hashCode();
		hc = hc * 31 + propertyName.hashCode();
		hc = hc * 31 + type.name().hashCode();
		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}

	public int queryBindHash() {
		return value.hashCode();
	}
	
	
}
