package com.avaje.ebeaninternal.server.querydefn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.avaje.ebean.JoinConfig;
import com.avaje.ebeaninternal.api.SpiQuery;
import com.avaje.ebeaninternal.server.core.ReferenceOptions;
import com.avaje.ebeaninternal.server.lib.util.StringHelper;

/**
 * Represents the Properties of an Object Relational query.
 */
public class OrmQueryProperties implements Serializable {

	private static final long serialVersionUID = -8785582703966455658L;

	private String path;

	private String properties;
	
	private String trimmedProperties;
	
	/**
	 * NB: -1 means no +query, 0 means use the default batch size.
	 */
	private int queryJoinBatch = -1;

	/**
	 * NB: -1 means no +lazy, 0 means use the default batch size.
	 */
	private int lazyJoinBatch = -1;

	private boolean cache;
	
	private boolean readOnly;

	private boolean allProperties;

	/**
	 * Note this SHOULD be a LinkedHashSet to preserve order of the 
	 * properties. This is to make using SqlSelect easier with 
	 * predictable property/column ordering.
	 */
	private Set<String> included;
	
	/**
	 * Included bean joins.
	 */
	private Set<String> includedBeanJoin;
	
	/**
	 * Add these properties to the select so that the foreign key columns
	 * are included in the query.
	 */
	private Set<String> secondaryQueryJoins;

	private List<OrmQueryProperties> secondaryChildren;
	
	public OrmQueryProperties() {
		this(null, null, null);
	}
	
	public OrmQueryProperties(String path, String properties, JoinConfig joinConfig) {
		this.path = path;
		this.properties = properties;
		
		this.trimmedProperties = properties;
		parseProperties();
		
		if (joinConfig != null){
			lazyJoinBatch = joinConfig.getLazyBatchSize();
			queryJoinBatch = joinConfig.getQueryBatchSize();
		}
		
		this.allProperties = trimmedProperties == null || "*".equals(trimmedProperties);
		if (!allProperties){
			this.included = parseIncluded(trimmedProperties);
		} else {
			this.included = null;
		}
	}
	
	/**
	 * Set the properties from deployment default FetchTypes.
	 */
	public void setProperties(String properties, Set<String> included) {
		this.properties = properties;
		this.trimmedProperties = properties;
		this.included = included;
		this.allProperties = false;
	}
	
	/**
	 * Define the select and joins for this query.
	 */
	public void configureBeanQuery(SpiQuery<?> query){
		
		if (trimmedProperties != null && trimmedProperties.length() > 0){
			query.select(trimmedProperties);
		}
		
		if (secondaryChildren != null){
			int trimPath = path.length()+1;
			for (int i = 0; i < secondaryChildren.size(); i++) {
				OrmQueryProperties p = secondaryChildren.get(i);
				String path = p.getPath();
				path = path.substring(trimPath);
				query.join(path, p.getProperties());
			}
		}
	}

	/**
	 * Define the select and joins for this query.
	 */
	public void configureManyQuery(SpiQuery<?> query){
		
		if (trimmedProperties != null && trimmedProperties.length() > 0){
			query.join(path, trimmedProperties);
		}
		
		if (secondaryChildren != null){
			
			for (int i = 0; i < secondaryChildren.size(); i++) {
				OrmQueryProperties p = secondaryChildren.get(i);
				String path = p.getPath();
				query.join(path, p.getProperties());
			}
		}
	}
	
	/**
	 * Creates a copy of the OrmQueryProperties.
	 */
	public OrmQueryProperties copy() {
		OrmQueryProperties copy = new OrmQueryProperties();
		copy.path = path;
		copy.properties = properties;
		copy.cache = cache;
		copy.readOnly = readOnly;
		copy.queryJoinBatch = queryJoinBatch;
		copy.lazyJoinBatch = lazyJoinBatch;
		copy.allProperties = allProperties;
		if (included != null){
			copy.included = new HashSet<String>(included);			
		}
		if (includedBeanJoin != null){
			copy.includedBeanJoin = new HashSet<String>(includedBeanJoin);	
		}
		return copy;
	}
	
	public boolean hasSelectClause() {
		if ("*".equals(trimmedProperties)) {
			// explicitly selected all properties
			return true;
		}
		// explicitly selected some properties
		return included != null;
	}
	
	public String toString() {
		String s = "";
		if (path != null){
			s += path+" ";
		}
		if (properties != null){
			s += "("+properties+") ";
		}
		return s;
	}
	
	public boolean isChild(OrmQueryProperties possibleChild){
		return possibleChild.getPath().startsWith(path+".");			
	}
	
	/**
	 * For secondary queries add a child element.
	 */
	public void add(OrmQueryProperties child){
		if (secondaryChildren == null){
			secondaryChildren = new ArrayList<OrmQueryProperties>();
		}
		secondaryChildren.add(child);
	}
	
	/**
	 * Calculate the query plan hash.
	 */
	public int queryPlanHash() {
		int hc = (path != null ? path.hashCode() : 1);
		hc = hc * 31 + (properties != null ? properties.hashCode() : 1);
		return hc;
	}
	
	public String getProperties() {
		return properties;
	}

	public ReferenceOptions getReferenceOptions(){
		if (cache || readOnly){
			return new ReferenceOptions(cache, readOnly, null); 
		} else {
			return null;
		}
	}
	
	public boolean isFetchInclude() {
		if (cache){
			return false;
		} else {
			return allProperties || included != null && !included.isEmpty();
		}
	}
	
	/**
	 * Return true if this has properties.
	 */
	public boolean hasProperties() {
		return properties != null;
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
	public Iterator<String> getSelectProperties() {
		
		if (secondaryQueryJoins == null){
			return included.iterator();			
		}
		
		LinkedHashSet<String> temp = new LinkedHashSet<String>(secondaryQueryJoins.size()+ included.size());
		temp.addAll(included);
		temp.addAll(secondaryQueryJoins);
		return temp.iterator();
	}
	
	public void addSecondaryQueryJoin(String property){
		if (secondaryQueryJoins == null){
			secondaryQueryJoins = new HashSet<String>(4);
		}
		secondaryQueryJoins.add(property);
	}
	
	/**
	 * Return all the properties including the bean joins. This is the
	 * set that will be used by EntityBeanIntercept to determine if a 
	 * property needs to be lazy loaded.
	 */
	public Set<String> getAllIncludedProperties() {

		if (included == null){
			return null;
		}

		if (includedBeanJoin == null && secondaryQueryJoins == null){
			return new LinkedHashSet<String>(included);
		} 
		
		LinkedHashSet<String> s = new LinkedHashSet<String>(2*(included.size()+5));
		if (included != null){
			s.addAll(included);
		}
		if (includedBeanJoin != null){
			s.addAll(includedBeanJoin);
		}
		if (secondaryQueryJoins != null){
			s.addAll(secondaryQueryJoins);
		}
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

	public boolean isQueryJoin() {
		return queryJoinBatch > -1;
	}

	public int getQueryJoinBatch() {
		return queryJoinBatch;
	}

	public boolean isLazyJoin() {
		return lazyJoinBatch > -1;
	}

	public int getLazyJoinBatch() {
		return lazyJoinBatch;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public boolean isCache() {
		return cache;
	}

	public String getPath() {
		return path;
	}
	
	private void parseProperties(){
		if (trimmedProperties == null){
			return;
		}
		int pos = trimmedProperties.indexOf("+readonly");
		if (pos > -1){
			trimmedProperties = StringHelper.replaceString(trimmedProperties, "+readonly","");
			this.readOnly = true;
		}
		pos = trimmedProperties.indexOf("+cache");
		if (pos > -1){
			trimmedProperties = StringHelper.replaceString(trimmedProperties, "+cache","");
			this.cache = true;
		}
		pos = trimmedProperties.indexOf("+query");
		if (pos > -1){
			queryJoinBatch = parseBatchHint(pos,"+query");		
		} 
		pos = trimmedProperties.indexOf("+lazy");
		if (pos > -1){
			lazyJoinBatch = parseBatchHint(pos,"+lazy");		
		}
	
		trimmedProperties = trimmedProperties.trim();
	}
		
	private int parseBatchHint(int pos, String option) {
		
		int startPos = pos+option.length();
		
		int endPos = findEndPos(startPos, trimmedProperties);
		if (endPos == -1){
			trimmedProperties = StringHelper.replaceString(trimmedProperties, option, "");
			return 0;
			
		} else {
			
			String batchParam = trimmedProperties.substring(startPos+1, endPos);
			
			if (endPos+1 >= trimmedProperties.length()){
				trimmedProperties = trimmedProperties.substring(0, pos);
			} else {
				trimmedProperties = trimmedProperties.substring(0, pos) + trimmedProperties.substring(endPos+1);
			}
			return Integer.parseInt(batchParam);
		}
	}
	
	private int findEndPos(int pos, String props) {
		
		if (pos < props.length()){
			if (props.charAt(pos) == '('){
				int endPara = props.indexOf(')', pos+1);
				if (endPara == -1){
					String m = "Error could not find ')' in "+props+" after position "+pos;
					throw new RuntimeException(m);
				}
				return endPara;
			}
		}
		return -1;
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
