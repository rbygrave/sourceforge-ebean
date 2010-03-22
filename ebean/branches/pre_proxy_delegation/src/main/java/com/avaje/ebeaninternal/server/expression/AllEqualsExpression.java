package com.avaje.ebeaninternal.server.expression;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.avaje.ebean.event.BeanQueryRequest;
import com.avaje.ebeaninternal.api.ManyWhereJoins;
import com.avaje.ebeaninternal.api.SpiExpression;
import com.avaje.ebeaninternal.api.SpiExpressionRequest;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.el.ElPropertyDeploy;

class AllEqualsExpression implements SpiExpression {

	private static final long serialVersionUID = -8691773558205937025L;
	
	private final Map<String, Object> propMap;

	AllEqualsExpression(Map<String, Object> map, String propertyNamePrefix) {
	    if (propertyNamePrefix == null){
	        this.propMap = map;
	    } else {
	        this.propMap = new LinkedHashMap<String,Object>();
	        for (Entry<String,Object> entry : map.entrySet()) {
                String pn = propertyNamePrefix+"."+entry.getKey();
                propMap.put(pn, entry.getValue());
            }
	    }
	}
	
	public void containsMany(BeanDescriptor<?> desc, ManyWhereJoins manyWhereJoin) {
		if (propMap != null){
			Iterator<String> it = propMap.keySet().iterator();
			while (it.hasNext()) {
				String propertyName = it.next();
				ElPropertyDeploy elProp = desc.getElPropertyDeploy(propertyName);
				if (elProp != null && elProp.containsMany()){
				    manyWhereJoin.add(elProp);
				}
			}
		}
	}

	public String getPropertyName() {
		return null;
	}

	public void addBindValues(SpiExpressionRequest request) {
		
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

	public void addSql(SpiExpressionRequest request) {
		
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
