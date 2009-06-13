package com.avaje.ebean.server.cache;

import java.util.concurrent.ConcurrentHashMap;


public class BasicCache implements Cache {

	ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<Object, Object>();
	
	public void clear() {
		map.clear();
	}

	public Object get(Object key) {
		return map.get(key);
	}

	public Object put(Object key, Object value) {
		return map.put(key, value);
	}

	public Object remove(Object key) {
		return map.remove(key);
	}

	public int size() {
		return map.size();
	}

	
	
}
