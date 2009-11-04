package com.avaje.ebean.config;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A map like structure of properties.
 */
final class PropertyMap {
	
	private static final long serialVersionUID = 1L;

	private LinkedHashMap<String,String> map = new LinkedHashMap<String,String>();
	
	public String toString() {
		return map.toString();
	}
	
	public synchronized boolean getBoolean(String key, boolean defaultValue){
		String value = get(key);
		if (value == null){
			return defaultValue;
		} else {
			return Boolean.parseBoolean(value);
		}
	}
	
	public synchronized int getInt(String key, int defaultValue){
		String value = get(key);
		if (value == null){
			return defaultValue;
		} else {
			return Integer.parseInt(value);
		}
	}
	
	public synchronized String get(String key, String defaultValue){
		String value = map.get(key.toLowerCase());
		return value == null ? defaultValue : value;
	}	
	
	public synchronized String get(String key){
		return map.get(key.toLowerCase());
	}

	synchronized void putAll(Map<String,String> keyValueMap){
		Iterator<Entry<String, String>> it = keyValueMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> entry = it.next();
			put(entry.getKey(), entry.getValue());
		}
	}

	synchronized String put(String key, String value){
		return map.put(key.toLowerCase(), value);
	}

	synchronized String remove(String key){
		return map.remove(key.toLowerCase());
	}

}
