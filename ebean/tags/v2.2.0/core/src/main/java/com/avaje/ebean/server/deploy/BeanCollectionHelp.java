package com.avaje.ebean.server.deploy;

import java.util.ArrayList;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionAdd;
import com.avaje.ebean.bean.BeanCollectionLoader;
import com.avaje.ebean.bean.EntityBean;

/**
 * Helper functions for performing tasks on Lists Sets or Maps.
 */
public interface BeanCollectionHelp<T> {

	/**
	 * Set the EbeanServer that owns the configuration.
	 */
	public void setLoader(BeanCollectionLoader loader);
	
	/**
	 * Return the mechanism to add beans to the underlying collection.
	 * <p>
	 * For Map's this needs to take the mapKey.
	 * </p>
	 */
	public BeanCollectionAdd getBeanCollectionAdd(BeanCollection<?> bc, String mapKey);

	/**
	 * Create an empty collection of the correct type.
	 */
	public BeanCollection<T> createEmpty();

	/**
	 * Add a bean to the List Set or Map.
	 */
	public void add(BeanCollection<?> collection, Object bean);

	/**
	 * Create a lazy loading proxy for a List Set or Map.
	 */
	public BeanCollection<T> createReference(EntityBean parentBean,String propertyName);

	/**
	 * Validate the List Set or Map.
	 */
	public ArrayList<InvalidValue> validate(Object manyValue);

	/**
	 * Refresh the List Set or Map.
	 */
	public void refresh(EbeanServer server, Query<?> query, Transaction t, Object parentBean);
	
	/**
	 * Apply the new refreshed BeanCollection to the appropriate property of the parent bean.
	 */
	public void refresh(BeanCollection<?> bc, Object parentBean);

}
