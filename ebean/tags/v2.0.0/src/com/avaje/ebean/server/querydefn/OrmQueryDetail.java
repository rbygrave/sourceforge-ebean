package com.avaje.ebean.server.querydefn;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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

	OrmQueryProperties baseProps = new OrmQueryProperties();

	HashMap<String, OrmQueryProperties> fetchJoins = new HashMap<String, OrmQueryProperties>(8);

	HashSet<String> includes = new HashSet<String>(8);

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
		baseProps = new OrmQueryProperties(null, columns);
	}

	/**
	 * Set the base / root query properties.
	 */
	public void setBase(OrmQueryProperties baseProps) {
		this.baseProps = baseProps;
	}

	/**
	 * Matches a join() method of the query.
	 */
	public void addFetchJoin(OrmQueryProperties chunk) {
		String property = chunk.getEntity();//.toLowerCase();
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
	public OrmQueryProperties addFetchJoin(String property, String partialProps) {
		OrmQueryProperties chunk = new OrmQueryProperties(property, partialProps);
		addFetchJoin(chunk);
		return chunk;
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
//			if (create && baseProps == null) {
//				baseProps = new OrmQueryProperties();
//			}
			return baseProps;
		}
		OrmQueryProperties props = fetchJoins.get(propertyName);//.toLowerCase());
		if (create && props == null) {
			return addFetchJoin(propertyName, null);
		} else {
			return props;
		}
	}

	/**
	 * Return true if the property is included.
	 */
	public boolean includes(String propertyName) {
		return includes.contains(propertyName);//.toLowerCase());
	}

	/**
	 * Return the property includes for this detail.
	 */
	public HashSet<String> getIncludes() {
		return includes;
	}
}
