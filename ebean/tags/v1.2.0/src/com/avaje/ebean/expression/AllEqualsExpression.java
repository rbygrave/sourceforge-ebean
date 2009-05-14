package com.avaje.ebean.expression;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.avaje.ebean.bean.BeanQueryRequest;

class AllEqualsExpression implements Expression {

	private static final long serialVersionUID = -8691773558205937025L;
	
	final Map<String, Object> propMap;

	AllEqualsExpression(Map<String, Object> propMap) {
		this.propMap = propMap;
	}
	
	public String getPropertyName() {
		return null;
	}

	public void addBindValues(ExpressionRequest request) {
		
		if (propMap.isEmpty()) {
			return;
		}
		Iterator<Object> it = propMap.values().iterator();
		while (it.hasNext()) {
			Object value = it.next();
			if (value != null) {
				request.addBindValue(value);
			} else {
				// null value uses is null clause
			}
		}
	}

	public void addSql(ExpressionRequest request) {
		
		if (propMap.isEmpty()) {
			return;
		}

		request.append("(");

		Set<Entry<String, Object>> entries = propMap.entrySet();
		Iterator<Entry<String, Object>> it = entries.iterator();

		int count = 0;
		while (it.hasNext()) {
			Map.Entry<java.lang.String, java.lang.Object> entry = it.next();
			Object value = entry.getValue();
			String propName = entry.getKey();

			if (count > 0) {
				request.append("and ");
			}

			request.append(propName);
			if (value == null) {
				request.append(" is null ");
			} else {
				request.append(" = ? ");
			}
			count++;
		}
		request.append(")");
	}

	/**
	 * Based on the properties and whether they are null.
	 * <p>
	 * The null check is required due to the "is null" sql being generated.
	 * </p>
	 */
	public int queryAutoFetchHash() {
	
		int hc = AllEqualsExpression.class.getName().hashCode();
		Set<Entry<String, Object>> entries = propMap.entrySet();
		Iterator<Entry<String, Object>> it = entries.iterator();

		while (it.hasNext()) {
			Map.Entry<java.lang.String, java.lang.Object> entry = it.next();
			Object value = entry.getValue();
			String propName = entry.getKey();

			hc = hc * 31 + propName.hashCode();
			hc = hc * 31 + (value == null ? 0 : 1);
		}

		return hc;
	}
	
	public int queryPlanHash(BeanQueryRequest<?> request) {
		return queryAutoFetchHash();
	}
	
	public int queryBindHash() {
		return queryAutoFetchHash();
	}
}
