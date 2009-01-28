package org.avaje.ebean.server.deploy;

import java.util.ArrayList;

import org.avaje.ebean.EbeanServer;
import org.avaje.ebean.InvalidValue;
import org.avaje.ebean.Query;
import org.avaje.ebean.Transaction;
import org.avaje.ebean.bean.ObjectGraphNode;
import org.avaje.ebean.collection.BeanCollection;

/**
 * Helper functions for performing tasks on Lists Sets or Maps.
 */
public interface BeanCollectionHelp {

	public BeanCollection<?> createEmpty();

	/**
	 * Create a lazy loading proxy for a List Set or Map.
	 */
	public BeanCollection<?> createReference(Object parentBean, String serverName,
			String propertyName, ObjectGraphNode profilePoint);

	/**
	 * Add a bean to the List Set or Map.
	 */
	public void add(BeanCollection<?> collection, Object bean, String mapKey);

	/**
	 * Validate the List Set or Map.
	 */
	public ArrayList<InvalidValue> validate(BeanDescriptor target, Object manyValue);

	/**
	 * Refresh the List Set or Map.
	 */
	public void refresh(EbeanServer server, Query<?> query, Transaction t,
			BeanPropertyAssocMany many, Object parentBean);
}
