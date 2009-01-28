package org.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import org.avaje.ebean.EbeanServer;
import org.avaje.ebean.InvalidValue;
import org.avaje.ebean.Query;
import org.avaje.ebean.Transaction;
import org.avaje.ebean.bean.ObjectGraphNode;
import org.avaje.ebean.collection.BeanCollection;
import org.avaje.ebean.collection.BeanSet;

/**
 * Helper specifically for dealing with Sets.
 */
public final class BeanSetHelp implements BeanCollectionHelp {
	
	public void add(BeanCollection<?> collection, Object bean, String mapKey) {
		collection.internalAdd(bean);
	}

	@SuppressWarnings("unchecked")
	public BeanCollection<?> createEmpty() {
		
		return new BeanSet();
	}

	@SuppressWarnings("unchecked")
	public BeanCollection<?> createReference(Object parentBean, String serverName,
			String propertyName, ObjectGraphNode profilePoint) {
		
		return new BeanSet(serverName, parentBean, propertyName, profilePoint);
	}
	
	public ArrayList<InvalidValue> validate(BeanDescriptor target, Object manyValue) {
		
		ArrayList<InvalidValue> errs = null;
		
		Set<?> set = (Set<?>)manyValue;
		Iterator<?> i = set.iterator();
		while (i.hasNext()) {
			Object detailBean = i.next();
			InvalidValue invalid = target.validate(true, detailBean);
			if (invalid != null){
				if (errs == null){
					errs = new ArrayList<InvalidValue>();
				}
				errs.add(invalid);
			}
		}
		return errs;
	}
	
	public void refresh(EbeanServer server, Query<?> query, Transaction t, BeanPropertyAssocMany many, Object parentBean) {
		
		BeanSet<?> newBeanSet = (BeanSet<?>)server.findSet(query, t);
		
		Set<?> current = (Set<?>)many.getValue(parentBean);
		
		if (many.isManyToMany()){
			newBeanSet.setModifyListening(true);
		}
		if (current == null){
			// the currentList is null?  Not really expecting this...
			many.setValue(parentBean,newBeanSet);
			
		} else if (current instanceof BeanSet) {
			// normally this case, replace just the underlying list
			BeanSet<?> currentBeanSet = (BeanSet<?>)current;
			currentBeanSet.setActualSet(newBeanSet.getActualSet());
			if (many.isManyToMany()){
				currentBeanSet.setModifyListening(true);
			}
		} else {
			// replace the entire set 
			many.setValue(parentBean, newBeanSet);
		}
	}
}
