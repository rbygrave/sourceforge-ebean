package com.avaje.ebean.server.deploy;

import java.util.ArrayList;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.InternalEbean;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.collection.BeanCollection;

/**
 * Helper functions for performing tasks on Lists Sets or Maps.
 */
public interface BeanCollectionHelp<T> {

	/**
	 * Set the EbeanServer that owns the configuration.
	 */
	public void setInternalEbean(InternalEbean internalEbean);
	
	public BeanCollection<T> createEmpty();

	/**
	 * Add a bean to the List Set or Map.
	 */
	public void add(BeanCollection<?> collection, Object bean);

	/**
	 * Create a lazy loading proxy for a List Set or Map.
	 */
	public BeanCollection<T> createReference(Object parentBean, String serverName,
			String propertyName, ObjectGraphNode profilePoint);

	/**
	 * Validate the List Set or Map.
	 */
	public ArrayList<InvalidValue> validate(Object manyValue);

	/**
	 * Refresh the List Set or Map.
	 */
	public void refresh(EbeanServer server, Query<?> query, Transaction t, Object parentBean);
}
