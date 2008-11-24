package com.avaje.ebean.server.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

public class ConfigProperties implements PropertyHolder {

	static final Logger logger = Logger.getLogger(ConfigProperties.class.getName());
	
	final PropertyEvaluator propertyEvaluator;
	
	final Map<String,String> map = new LinkedHashMap<String, String>();
	
	final HashSet<String> propKeys = new HashSet<String>();
	
	public ConfigProperties(Properties properties){
		this(null, properties);
	}
	
	public ConfigProperties(ConfigProperties parentProps, Properties properties){
		
		propertyEvaluator = new PropertyEvaluator(this);
		
		if (parentProps != null){
			map.putAll(parentProps.map);
			propKeys.addAll(parentProps.propKeys);
		}
		
		if (properties != null){
			add(properties, true);
		}
	}
	
	public ConfigProperties(InputStream in){
		
		propertyEvaluator = new PropertyEvaluator(this);
		
		Properties props = new Properties();
		try {
			props.load(in);
			in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		add(props, true);
	}

	
	/**
	 * Load the resource from the classpath.
	 */
	protected static InputStream getFromClasspath(String fileName) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
	}
	

	protected synchronized static InputStream findInputStream(String fileName) {

		if (fileName == null) {
			throw new NullPointerException("fileName is null?");
		}

		InputStream in = null;
		try {
			File f = new File(fileName);
			if (f.exists()) {
				in = new FileInputStream(f);
			} else {
				in = getFromClasspath(fileName);
			}

			return in;

		} catch (FileNotFoundException ex) {
			// already made the check so this 
			// should never be thrown
			throw new RuntimeException(ex);
		} 
	}
	
	private synchronized static Properties findProperties(String fileName) {
		
		Properties props = null;
		InputStream in = findInputStream(fileName);
		if (in != null){
			props = new Properties();
			try {
				props.load(in);
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
		return props;
	}
	
	/**
	 * load the properties from an InputStream.
	 */
	public static ConfigProperties create(String fileName) {
		
		Properties props = findProperties(fileName);
		if (props == null){
			return null;
		}

		ConfigProperties config = new ConfigProperties(props);
		

		return config;
	}
	
	private void add(Properties properties, boolean override){
		
		Iterator<?> i = properties.entrySet().iterator();
		while (i.hasNext()) {
			Map.Entry<?,?> entry = (Map.Entry<?,?>) i.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();

			// add a normal property
			put(key, value, override);
		}
		
		evaluateExpressions();
		addRecurse();
	}
	
	private void addRecurse() {
		
		String extraFile = removeProperty("load.properties");
		if (extraFile != null){
			logger.info("load.properties: "+extraFile);
			Properties extra  = findProperties(extraFile);
			if (extra != null){
				add(extra, false);
			} else {
				logger.warning("Extra load.properties not found? "+extraFile);
			}
		}

		// load more properties overriding existing ones
		extraFile = removeProperty("load.properties.override");
		if (extraFile != null) {
			logger.info("load.properties.override: "+extraFile);
			Properties extra  = findProperties(extraFile);
			if (extra != null){
				add(extra, true);
			} else {
				logger.warning("Extra load.properties.override not found? "+extraFile);
			}
		}
	}
	
	private void evaluateExpressions() {
		
		Iterator<Map.Entry<String,String>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,String> entry = it.next();
			//String key = entry.getKey();
			String value = entry.getValue();

			value = propertyEvaluator.eval(value);
			entry.setValue(value);
		}
	}
	
	
	/**
	 * Put the property taking evaluating any expressions as required.
	 * <p>
	 * Expressions such as ${CATALINA_HOME} can be included in val and these are
	 * evaluated (see evaluateExpression).
	 * </p>
	 */
	private void put(String key, String val, boolean override) {

		String lowerKey = key.toLowerCase();
		if (!override && map.containsKey(lowerKey)) {
			// already has an entry so not overriding
			return;
		}

		// evaluate expressions like ${CATALINA_HOME}
		// that may be contained in val
		//val = propertyEvaluator.eval(val);
		map.put(lowerKey, val);
		propKeys.add(key);
	}
	
	public synchronized Iterator<String> keys() {
		return propKeys.iterator();
	}
	
	public synchronized int getIntProperty(String key, int defaultValue) {
		String v = getProperty(key);
		if (v != null) {
			return Integer.parseInt(v.trim());
		}
		return defaultValue;
	}

	/**
	 * Return a boolean value which should be either "true" or "false".
	 */
	public synchronized boolean getBooleanProperty(String key, boolean defaultValue) {
		String v = getProperty(key);
		if (v != null) {
			return v.trim().equalsIgnoreCase("true");
		}
		return defaultValue;
	}
	
	/**
	 * Return a property with a default value.
	 */
	public synchronized String getProperty(String key, String defaultValue) {
		String v = getProperty(key);
		if (v == null) {
			v = defaultValue;
		}
		return v;
	}

	public Map<String,String> getMap(){
		return map;
	}
	
	/**
	 * Set a property.
	 */
	public synchronized void setProperty(String key, String value) {
		
		put(key, value, true);
	}

	/**
	 * Remove a property.
	 */
	public synchronized String removeProperty(String key) {
		
		propKeys.remove(key);
		return (String) map.remove(key.toLowerCase());
	}

	/**
	 * Get a property returning null if no value has been set.
	 */
	public synchronized String getProperty(String key) {
		if (key == null) {
			throw new NullPointerException("the key is null?");
		}
		
		return (String) map.get(key.toLowerCase());
	}
}
