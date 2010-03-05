package com.avaje.ebeaninternal.server.deploy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.InvalidValue;
import com.avaje.ebean.Query;
import com.avaje.ebean.Transaction;
import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionAdd;
import com.avaje.ebean.bean.BeanCollectionLoader;
import com.avaje.ebean.common.BeanSet;

/**
 * Helper specifically for dealing with Sets.
 */
public final class BeanSetHelp<T> implements BeanCollectionHelp<T> {
	
	private final BeanPropertyAssocMany<T> many;
	private final BeanDescriptor<T> targetDescriptor;
	private BeanCollectionLoader loader;
	
	/**
	 * When attached to a specific many property.
	 */
	public BeanSetHelp(BeanPropertyAssocMany<T> many){
		this.many = many;
		this.targetDescriptor = many.getTargetDescriptor();
	}
	
	/**
	 * For a query that returns a set.
	 */
	public BeanSetHelp(){
		this.many = null;
		this.targetDescriptor = null;
	}
	
	public void setLoader(BeanCollectionLoader loader){
		this.loader = loader;
	}
	
    public Iterator<?> getIterator(Object collection) {
        return ((Set<?>) collection).iterator();
    }
	
	public BeanCollectionAdd getBeanCollectionAdd(Object bc,String mapKey) {
	    if (bc instanceof BeanSet<?>){
    		BeanSet<?> beanSet = (BeanSet<?>)bc;
    		if (beanSet.getActualSet() == null){
    			beanSet.setActualSet(new LinkedHashSet<Object>());
    		}
    		return beanSet;
	    } else if (bc instanceof Set<?>) {
	        return new VanillaAdd((Set<?>)bc);
	        
	    } else {
	        throw new RuntimeException("Unhandled type "+bc);
	    }
	}
	

    @SuppressWarnings("unchecked")
    static class VanillaAdd implements BeanCollectionAdd {

        private final Set set;

        private VanillaAdd(Set<?> set) {
            this.set = set;
        }

        public void addBean(Object bean) {
            set.add(bean);
        }
    }
	
	public void add(BeanCollection<?> collection, Object bean) {
		collection.internalAdd(bean);
	}

    @SuppressWarnings("unchecked")
    public Object copyShallow(Object source, boolean vanilla) {
        if (source instanceof Set<?> == false){
            return null;
        }
        Set<T> s = vanilla ? new LinkedHashSet<T>() : new BeanSet<T>();
        s.addAll((Set<T>)source);
	    return s;
    }

    public Object createEmpty(boolean vanilla) {
	    return vanilla ? new LinkedHashSet<T>() : new BeanSet<T>();
	}

	public BeanCollection<T> createReference(Object parentBean, String propertyName) {
		
		return new BeanSet<T>(loader, parentBean, propertyName);
	}
	
	public ArrayList<InvalidValue> validate(Object manyValue) {
		
		ArrayList<InvalidValue> errs = null;
		
		Set<?> set = (Set<?>)manyValue;
		Iterator<?> i = set.iterator();
		while (i.hasNext()) {
			Object detailBean = i.next();
			InvalidValue invalid = targetDescriptor.validate(true, detailBean);
			if (invalid != null){
				if (errs == null){
					errs = new ArrayList<InvalidValue>();
				}
				errs.add(invalid);
			}
		}
		return errs;
	}

	public void refresh(EbeanServer server, Query<?> query, Transaction t, Object parentBean) {
		
		BeanSet<?> newBeanSet = (BeanSet<?>)server.findSet(query, t);
		refresh(newBeanSet, parentBean);
	}
	
	public void refresh(BeanCollection<?> bc, Object parentBean) {
		
		BeanSet<?> newBeanSet = (BeanSet<?>)bc;
		
		Set<?> current = (Set<?>)many.getValue(parentBean);
		
		newBeanSet.setModifyListening(many.getModifyListenMode());
		//if (many.isManyToMany()){
		//	newBeanSet.setModifyListening(true);
		//}
		if (current == null){
			// the currentList is null?  Not really expecting this...
			many.setValue(parentBean,newBeanSet);
			
		} else if (current instanceof BeanSet<?>) {
			// normally this case, replace just the underlying list
			BeanSet<?> currentBeanSet = (BeanSet<?>)current;
			currentBeanSet.setActualSet(newBeanSet.getActualSet());
			currentBeanSet.setModifyListening(many.getModifyListenMode());
			//if (many.isManyToMany()){
			//	currentBeanSet.setModifyListening(true);
			//}
		} else {
			// replace the entire set 
			many.setValue(parentBean, newBeanSet);
		}
	}
}
