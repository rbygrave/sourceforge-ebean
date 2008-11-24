package com.avaje.ebean.server.deploy;

import java.util.ArrayList;
import java.util.List;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.ObjectGraphNode;
import com.avaje.ebean.collection.BeanCollection;
import com.avaje.ebean.collection.BeanList;

/**
 * Helper object for dealing with Lists.
 */
public final class BeanListHelp implements BeanCollectionHelp {
	
	public void add(BeanCollection<?> collection, Object bean, String mapKey) {
		collection.internalAdd(bean);
	}

	@SuppressWarnings("unchecked")
	public BeanCollection<?> createEmpty() {
		
		return new BeanList();
	}

	@SuppressWarnings("unchecked")
	public BeanCollection<?> createReference(Object parentBean, String serverName,
			String propertyName, ObjectGraphNode profilePoint) {
		
		return new BeanList(serverName, parentBean, propertyName, profilePoint);
	}
	
	public ArrayList<InvalidValue> validate(BeanDescriptor target, Object manyValue) {
		
		ArrayList<InvalidValue> errs = null;
		
		List<?> l = (List<?>)manyValue;
		for (int i = 0; i < l.size(); i++) {
			Object detailBean = l.get(i);
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
		
		BeanList<?> newBeanList = (BeanList<?>)server.findList(query, t);
		
		List<?> currentList = (List<?>)many.getValue(parentBean);
		
		if (many.isManyToMany()){
			newBeanList.setModifyListening(true);
		}
		if (currentList == null){
			// the currentList is null?  Not really expecting this...
			many.setValue(parentBean,newBeanList);
			
		} else if (currentList instanceof BeanList) {
			// normally this case, replace just the underlying list
			BeanList<?> currentBeanList = (BeanList<?>)currentList;
			currentBeanList.setActualList(newBeanList.getActualList());
			if (many.isManyToMany()){
				currentBeanList.setModifyListening(true);
			}
		} else {
			// replace the entire list with the BeanList
			many.setValue(parentBean, newBeanList);
		}
	}
}
