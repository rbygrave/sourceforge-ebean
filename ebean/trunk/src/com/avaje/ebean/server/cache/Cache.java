package com.avaje.ebean.server.cache;

public interface Cache {

	public Object get(Object id);
	
	public Object put(Object id, Object value);
	
	public Object remove(Object id);
	
	public void clear();
	
	public int size();
}
