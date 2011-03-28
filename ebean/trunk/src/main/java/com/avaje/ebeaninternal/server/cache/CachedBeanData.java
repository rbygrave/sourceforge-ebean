package com.avaje.ebeaninternal.server.cache;

import java.util.Set;

public class CachedBeanData {

    private final Set<String> loadedProperties;
    private final Object[] data;
    
    public CachedBeanData(Set<String> loadedProperties, Object[] data) {
        this.loadedProperties= loadedProperties;
        this.data = data;
    }
    
    public boolean containsProperty(String propName) {
        return loadedProperties == null || loadedProperties.contains(propName);
    }
    
    public Object getData(int i){
        return data[i];
    }
    
	public Set<String> getLoadedProperties() {
    	return loadedProperties;
    }
    
	public Object[] copyData() {
		Object[] dest = new Object[data.length];
		System.arraycopy(data, 0, dest, 0, data.length);
		return dest;
	}
	
}

