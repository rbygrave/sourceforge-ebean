package com.avaje.ebeaninternal.server.cache;

import java.util.HashSet;
import java.util.Set;

import com.avaje.ebean.bean.EntityBean;
import com.avaje.ebean.bean.EntityBeanIntercept;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanProperty;

public class CachedBeanDataExtract {

    private final BeanDescriptor<?> desc;
    private final Object bean;
    private final EntityBeanIntercept ebi;
    
    private final Set<String> loadedProps;
    private final Set<String> extractProps;

    public static CachedBeanData extract(BeanDescriptor<?> desc, Object bean){
    	if (bean instanceof EntityBean){
        	return new CachedBeanDataExtract(desc, bean, ((EntityBean)bean)._ebean_getIntercept()).extract();    		
    		
    	} else {
        	return new CachedBeanDataExtract(desc, bean, null).extract();    		
    	}
    }
    
    public static CachedBeanData extract(BeanDescriptor<?> desc, Object bean, EntityBeanIntercept ebi){
    	return new CachedBeanDataExtract(desc, bean, ebi).extract();
    }
    
    private CachedBeanDataExtract(BeanDescriptor<?> desc, Object bean, EntityBeanIntercept ebi) {
        this.desc = desc;
        this.bean = bean;
        this.ebi = ebi;        
        if (ebi != null){
        	this.loadedProps = ebi.getLoadedProps(); 
        	this.extractProps = (loadedProps == null) ? null : new HashSet<String>();
        } else {
        	this.extractProps = new HashSet<String>();
        	this.loadedProps = null;
        }
    }
    
    private CachedBeanData extract(){

        BeanProperty[] props = desc.propertiesNonMany();

    	Object[] data = new Object[props.length];
    	
        for (int i = 0; i < props.length; i++) {
        	BeanProperty prop  = props[i];
            if (includeNonManyProperty(prop.getName())){
            	Object val = prop.getCacheDataValue(bean);
            	data[i] = val;
            	
            	if (ebi != null){
            		if (extractProps != null){
            			extractProps.add(prop.getName());
            		}
            	} else if (val != null){
            		if (extractProps != null){
            			extractProps.add(prop.getName());
            		}
            	}
            }
        }
        
        
        return new CachedBeanData(extractProps,data);
    }
    
    
    private boolean includeNonManyProperty(String name) {
        
    	return loadedProps == null || loadedProps.contains(name);
    }
    
}