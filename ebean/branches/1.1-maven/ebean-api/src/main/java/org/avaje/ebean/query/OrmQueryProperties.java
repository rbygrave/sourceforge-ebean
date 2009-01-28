package org.avaje.ebean.query;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.avaje.lib.util.StringHelper;

/**
 * Represents the Properties of an Object Relational query.
 */
public class OrmQueryProperties implements Serializable {

	private static final long serialVersionUID = -8785582703966455657L;

	String entity;

	String queryPlanProperties;
	
	boolean cache;
	
	boolean readOnly;

	boolean allProperties;

	/**
	 * Note this SHOULD be a LinkedHashSet to preserve order of the 
	 * properties. This is to make using SqlSelect easier with 
	 * predictable property/column ordering.
	 */
	Set<String> included;
	
	/**
	 * Included bean joins.
	 */
	Set<String> includedBeanJoin;
	
	public OrmQueryProperties() {
		this(null,null);
	}
	
	public OrmQueryProperties(String entity, String properties) {
		this.entity = entity;
		this.queryPlanProperties = properties;
		String parsedProps = parseProperties(properties);
		
		this.allProperties = parsedProps == null || "*".equals(parsedProps);
		if (!allProperties){
			this.included = parseIncluded(parsedProps);
		} else {
			this.included = null;
		}
	}
	
	public String toString() {
		String s = "";
		if (entity != null){
			s += entity+" ";
		}
		if (queryPlanProperties != null){
			s += "("+queryPlanProperties+") ";
		}
		return s;
	}
	
	/**
	 * Calculate the query plan hash.
	 */
	public int queryPlanHash() {
		int hc = (entity != null ? entity.hashCode() : 1);
		hc = hc * 31 + (queryPlanProperties != null ? queryPlanProperties.hashCode() : 1);
		return hc;
	}
	
	/**
	 * Return true if this has properties.
	 */
	public boolean hasProperties() {
		return queryPlanProperties != null;
	}
	
	/**
	 * Return true if this property is included as a bean join.
	 * <p>
	 * If a property is included as a bean join then it should
	 * not be included as a reference/proxy to avoid duplication.
	 * </p>
	 */
	public boolean isIncludedBeanJoin(String propertyName) {
		if (includedBeanJoin == null){
			return false;
		} else {
			return includedBeanJoin.contains(propertyName);
		}
	}
	
	/**
	 * Add a bean join property.
	 */
	public void includeBeanJoin(String propertyName){
		if (includedBeanJoin == null){
			includedBeanJoin = new HashSet<String>();
		}
		includedBeanJoin.add(propertyName);
	}
	
	public boolean allProperties() {
		return allProperties;
	}
	
	/**
	 * This excludes the bean joined properties.
	 * <p>
	 * This is because bean joins will have there own node in the SqlTree.
	 * </p>
	 */
	public Set<String> getSelectProperties() {
		return included;
	}

	/**
	 * Return all the properties including the bean joins. This is the
	 * set that will be used by EntityBeanIntercept to determine if a 
	 * property needs to be lazy loaded.
	 */
	public Set<String> getAllIncludedProperties() {
		
		if (includedBeanJoin == null){
			return included;
		} 
		if (included == null){
			return null;
		}
		LinkedHashSet<String> s = new LinkedHashSet<String>(2*(included.size()+includedBeanJoin.size()));
		s.addAll(included);
		s.addAll(includedBeanJoin);
		return s;
	}
	
	public boolean isIncluded(String propName) {

		if (includedBeanJoin != null && includedBeanJoin.contains(propName)){
			return false;
		}
		if (allProperties){
			return true;
		}
		return included.contains(propName);
	}

    
	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isCache() {
		return cache;
	}

	public String getEntity() {
		return entity;
	}
	
	private String parseProperties(String props){
		if (props == null){
			return null;
		}
		if (props.indexOf("+readonly") >-1){
			props = StringHelper.replaceString(props, "+readonly","");
			this.readOnly = true;
		}
		if (props.indexOf("+cache") >-1){
			props = StringHelper.replaceString(props, "+cache","");
			this.cache = true;
		}
		return props.trim();
	}
	
    /**
     * Parse the include separating by comma or semicolon.
     */
    private static Set<String> parseIncluded(String rawList) {
            	
        String[] res = rawList.split(",");
    	
        LinkedHashSet<String> set = new LinkedHashSet<String>(res.length+3);

        String temp = null;
        for (int i = 0; i < res.length; i++) {
            temp = res[i].trim();
            if (temp.length() > 0){
            	set.add(temp);
            }
        }
        return Collections.unmodifiableSet(set);
    }
}
