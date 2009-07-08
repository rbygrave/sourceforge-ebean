package com.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanMap;
import com.avaje.ebean.common.InternalEbean;
import com.avaje.ebean.common.ObjectGraphNode;

/**
 * Helper specifically for dealing with Maps.
 */
public class BeanMapHelp<T> implements BeanCollectionHelp<T> {

	final BeanPropertyAssocMany<T> many;
	final BeanDescriptor<T> targetDescriptor;
	final String mapKey;
	final BeanProperty beanProperty;
	InternalEbean internalEbean;
	
	/**
	 * When created for a given query that will return a map.
	 */
	public BeanMapHelp(BeanDescriptor<T> targetDescriptor, String mapKey) {
		this(null, targetDescriptor, mapKey);
	}

	public BeanMapHelp(BeanPropertyAssocMany<T> many){
		this(many, many.getTargetDescriptor(), many.getMapKey());
	}
	
	/**
	 * When help is attached to a specific many property.
	 */
	private BeanMapHelp(BeanPropertyAssocMany<T> many, BeanDescriptor<T> targetDescriptor, String mapKey){
		this.many = many;
		this.targetDescriptor = targetDescriptor;
		this.mapKey = mapKey;
		this.beanProperty = targetDescriptor.getBeanProperty(mapKey);
	}
	
	
	public void setInternalEbean(InternalEbean internalEbean){
		this.internalEbean = internalEbean;
	}
	
	@SuppressWarnings("unchecked")
	public BeanCollection<T> createEmpty() {
		return new BeanMap();
	}
	
	@SuppressWarnings("unchecked")
	public void add(BeanCollection<?> collection, Object bean) {

		Object keyValue = beanProperty.getValue(bean);

		Map<Object, Object> map = (Map<Object, Object>) collection;
		map.put(keyValue, bean);
	}

	@SuppressWarnings("unchecked")
	public BeanCollection<T> createReference(Object parentBean, String serverName,
			String propertyName, ObjectGraphNode profilePoint) {

		return new BeanMap(internalEbean, parentBean, propertyName, profilePoint);
	}

	public ArrayList<InvalidValue> validate(Object manyValue) {

		ArrayList<InvalidValue> errs = null;

		Map<?, ?> m = (Map<?, ?>) manyValue;
		Iterator<?> it = m.values().iterator();
		while (it.hasNext()) {
			Object detailBean = (Object) it.next();
			InvalidValue invalid = targetDescriptor.validate(true, detailBean);
			if (invalid != null) {
				if (errs == null) {
					errs = new ArrayList<InvalidValue>();
				}
				errs.add(invalid);
			}
		}

		return errs;
	}

	public void refresh(EbeanServer server, Query<?> query, Transaction t, Object parentBean) {

		BeanMap<?, ?> newBeanMap = (BeanMap<?, ?>) server.findMap(query, t);

		Map<?, ?> current = (Map<?, ?>) many.getValue(parentBean);

		if (many.isManyToMany()) {
			newBeanMap.setModifyListening(true);
		}
		if (current == null) {
			// the currentMap is null? Not really expecting this...
			many.setValue(parentBean, newBeanMap);

		} else if (current instanceof BeanMap<?,?>) {
			// normally this case, replace just the underlying list
			BeanMap<?, ?> currentBeanMap = (BeanMap<?, ?>) current;
			currentBeanMap.setActualMap(newBeanMap.getActualMap());
			if (many.isManyToMany()) {
				currentBeanMap.setModifyListening(true);
			}
		} else {
			// replace the entire set
			many.setValue(parentBean, newBeanMap);
		}
	}

}
