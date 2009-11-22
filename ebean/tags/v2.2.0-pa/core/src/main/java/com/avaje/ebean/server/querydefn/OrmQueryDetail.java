package com.avaje.ebean.server.querydefn;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.avaje.ebean.JoinConfig;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.el.ElPropertyDeploy;
import com.avaje.ebean.server.query.SplitName;

/**
 * Represents the internal structure of an Object Relational query.
 * <p>
 * Holds the select() and join() details of a ORM query.
 * </p>
 * <p>
 * It is worth noting that for autoFetch a "tuned fetch info" builds an instance
 * of OrmQueryDetail. Tuning a query is a matter of replacing an instance of
 * this class with one that has been tuned with select() and join() set.
 * </p>
 */
public class OrmQueryDetail implements Serializable {

	private static final long serialVersionUID = -2510486880141461806L;

	private OrmQueryProperties baseProps = new OrmQueryProperties();

	private HashMap<String, OrmQueryProperties> fetchJoins = new HashMap<String, OrmQueryProperties>(8);

	private HashSet<String> includes = new HashSet<String>(8);

	/**
	 * Return a deep copy of the OrmQueryDetail.
	 */
	public OrmQueryDetail copy() {
		OrmQueryDetail copy = new OrmQueryDetail();
		copy.baseProps = baseProps.copy();
		Iterator<Entry<String, OrmQueryProperties>> it = fetchJoins.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, OrmQueryProperties> entry = it.next();
			copy.fetchJoins.put(entry.getKey(), entry.getValue().copy());
		}
		copy.includes = new HashSet<String>(includes);
		return copy;
	}
	
	/**
	 * Calculate the hash for the query plan.
	 */
	public int queryPlanHash() {

		int hc = (baseProps == null ? 1 : baseProps.queryPlanHash());

		if (fetchJoins != null) {
			Iterator<OrmQueryProperties> it = fetchJoins.values().iterator();
			while (it.hasNext()) {
				OrmQueryProperties p = it.next();
				hc = hc * 31 + p.queryPlanHash();
			}
		}

		return hc;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (baseProps == null) {

		} else {
			sb.append("select ").append(baseProps);
		}
		for (OrmQueryProperties join : fetchJoins.values()) {
			sb.append(" join ").append(join);
		}
		return sb.toString();
	}

	public int hashCode() {
		throw new RuntimeException("should not use");
	}

	/**
	 * set the properties to include on the base / root entity.
	 */
	public void select(String columns) {
		baseProps = new OrmQueryProperties(null, columns, null);
	}
	
	public boolean containsProperty(String property){
		if (baseProps == null){
			return true;
		} else {
			return baseProps.isIncluded(property);
		}
	}

	/**
	 * Set the base / root query properties.
	 */
	public void setBase(OrmQueryProperties baseProps) {
		this.baseProps = baseProps;
	}

	public List<OrmQueryProperties> removeSecondaryQueries() {
		return removeSecondaryQueries(false);
	}
	
	public List<OrmQueryProperties> removeSecondaryLazyQueries() {
		return removeSecondaryQueries(true);
	}
	
	private List<OrmQueryProperties> removeSecondaryQueries(boolean lazyQuery) {
		
		ArrayList<String> matchingPaths = new ArrayList<String>(2);
		
		Iterator<OrmQueryProperties> it = fetchJoins.values().iterator();
		while (it.hasNext()) {
			OrmQueryProperties chunk = it.next();
			boolean match = lazyQuery ? chunk.isLazyJoin() : chunk.isQueryJoin();
			if (match){
				matchingPaths.add(chunk.getPath());
			}
		}
		
		if (matchingPaths.size() == 0){
			return null;
		}

		// sort into depth order to remove 
		Collections.sort(matchingPaths);

		// the list of secondary queries
		ArrayList<OrmQueryProperties> props = new ArrayList<OrmQueryProperties>(2);

		
		for (int i = 0; i < matchingPaths.size(); i++) {
			String path = matchingPaths.get(i);
			includes.remove(path);
			OrmQueryProperties secQuery = fetchJoins.remove(path);
			if (secQuery == null){
				// the path has already been removed by another
				// secondary query
				
			} else {
				props.add(secQuery);
				
				// remove any child properties for this path
				Iterator<OrmQueryProperties> pass2It = fetchJoins.values().iterator();
				while (pass2It.hasNext()) {
					OrmQueryProperties pass2Prop = pass2It.next();
					if (secQuery.isChild(pass2Prop)){
						// remove join to secondary query from the main query 
						// and add to this secondary query
						pass2It.remove();
						includes.remove(pass2Prop.getPath());
						secQuery.add(pass2Prop);
					}
				}
			}
		}
		
		// Add the secondary queries as select properties
		// to the parent chunk to ensure the foreign keys
		// are included in the query
		for (int i = 0; i < props.size(); i++) {
			String path = props.get(i).getPath();
			// split into parent and property
			String[] split = SplitName.split(path);
			// add property to parent chunk
			OrmQueryProperties chunk = getChunk(split[0], true);
			chunk.addSecondaryQueryJoin(split[1]);
		}
		
		return props;
	}
	
	/**
	 * Matches a join() method of the query.
	 */
	public void addFetchJoin(OrmQueryProperties chunk) {
		String property = chunk.getPath();//.toLowerCase();
		fetchJoins.put(property, chunk);
		includes.add(property);
	}
	
	/**
	 * Remove all joins and properties.
	 * <p>
	 * Typically for the row count query.
	 * </p>
	 */
	public void clear() {
		includes.clear();
		fetchJoins.clear();		
	}

	/**
	 * Matches the join() method of a query.
	 * @param property the property to join
	 * @param partialProps the properties on the join property to include
	 */
	public OrmQueryProperties addFetchJoin(String property, String partialProps, JoinConfig joinConfig) {
		OrmQueryProperties chunk = new OrmQueryProperties(property, partialProps, joinConfig);
		addFetchJoin(chunk);
		return chunk;
	}
	
	public void removeManyJoins(BeanDescriptor<?> beanDescriptor) {
		
		ArrayList<String> removeList = new ArrayList<String>();
		
		Iterator<String> it = fetchJoins.keySet().iterator();
		while (it.hasNext()) {
			String joinProp = (String) it.next();
			ElPropertyDeploy elProp = beanDescriptor.getElPropertyDeploy(joinProp);
			if (elProp.containsMany()){
				removeList.add(joinProp);
			}
		}
		
		for (int i = 0; i < removeList.size(); i++) {
			String manyJoinProp = removeList.get(i);
			includes.remove(manyJoinProp);
			fetchJoins.remove(manyJoinProp);
		}
		
	}

	/**
	 * Set any default select clauses for the main bean and any joins that have
	 * not explicitly defined a select clause.
	 * <p>
	 * That is this will use FetchType.LAZY to exclude some properties by default.
	 * </p>
	 */
	public void setDefaultSelectClause(BeanDescriptor<?> desc){
		
		if (desc.hasDefaultSelectClause() && !hasSelectClause()){
			if (baseProps == null){
				baseProps = new OrmQueryProperties();
			}
			baseProps.setProperties(desc.getDefaultSelectClause(), desc.getDefaultSelectClauseSet());
		}
		
		Iterator<OrmQueryProperties> it = fetchJoins.values().iterator();
		while (it.hasNext()) {
			OrmQueryProperties joinProps = it.next();
			if (!joinProps.hasSelectClause()){
				BeanDescriptor<?> assocDesc = desc.getBeanDescriptor(joinProps.getPath());
				if (assocDesc.hasDefaultSelectClause()){
					// use the default select clause 
					joinProps.setProperties(assocDesc.getDefaultSelectClause(), assocDesc.getDefaultSelectClauseSet());
				}
			}
		}
	}
	
	public boolean hasSelectClause() {
		return (baseProps != null && baseProps.hasSelectClause());
	}
	
	/**
	 * Return true if the query detail has neither select properties specified
	 * or any joins defined.
	 */
	public boolean isEmpty() {
		return fetchJoins.isEmpty() && (baseProps == null || !baseProps.hasProperties());
	}

	/**
	 * Return true if there are no fetch joins.
	 */
	public boolean isFetchJoinsEmpty() {
		return fetchJoins.isEmpty();
	}

	/**
	 * Add the explicit bean join.
	 * <p>
	 * This is also used to Exclude the matching property from the parent select
	 * (aka remove the foreign key) because it is now included in it's on node
	 * in the SqlTree.
	 * </p>
	 */
	public void includeBeanJoin(String parent, String propertyName) {
		OrmQueryProperties parentChunk = getChunk(parent, true);
		parentChunk.includeBeanJoin(propertyName);
	}

	public OrmQueryProperties getChunk(String propertyName, boolean create) {
		if (propertyName == null) {
			return baseProps;
		}
		OrmQueryProperties props = fetchJoins.get(propertyName);
		if (create && props == null) {
			return addFetchJoin(propertyName, null, null);
		} else {
			return props;
		}
	}

	/**
	 * Return true if the property is included.
	 */
	public boolean includes(String propertyName) {
		
		OrmQueryProperties chunk = fetchJoins.get(propertyName);
		
		// may not have fetch properties if just +cache etc
		return chunk != null && chunk.isFetchInclude();
	}

	/**
	 * Return the property includes for this detail.
	 */
	public HashSet<String> getIncludes() {
		return includes;
	}
}
