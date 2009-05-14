package com.avaje.ebean.server.lib.cache;

public interface DoubleMapCreateValue<K,V> {

	public V createValue(K k);
	
	public void postPut(V v);
}
