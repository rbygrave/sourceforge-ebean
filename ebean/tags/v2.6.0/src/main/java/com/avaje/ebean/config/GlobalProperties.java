package com.avaje.ebean.config;

import java.util.Map;

import javax.servlet.ServletContext;

import com.avaje.ebeaninternal.api.ClassUtil;

/**
 * Provides access to properties loaded from the ebean.properties file.
 */
public final class GlobalProperties {
	
	private static PropertyMap globalMap;
	
	private static boolean skipPrimaryServer;
	
	/**
	 * Set whether to skip automatically creating the primary server.
	 */
	public static synchronized void setSkipPrimaryServer(boolean skip){
		skipPrimaryServer = skip;
	}
	
	/**
	 * Return true to skip automatically creating the primary server.
	 */
	public static synchronized boolean isSkipPrimaryServer(){
		return skipPrimaryServer;
	}
	
	/**
	 * Parse the string replacing any expressions like ${catalina.base}.
	 * <p>
	 * This will evaluate expressions using first environment variables, 
	 * than java system variables and lastly properties in ebean.properties 
	 * - in that order.
	 * </p>
	 * <p>
	 * Expressions start with "${" and end with "}".
	 * </p>
	 */
	public static String evaluateExpressions(String val) {
		return PropertyExpression.eval(val);
	}
	
	/**
	 * In a servlet container environment this will additionally look in WEB-INF 
	 * for the ebean.properties file.
	 */
	public static synchronized void setServletContext(ServletContext servletContext) {
	
		PropertyMapLoader.setServletContext(servletContext);
	}
	
	/**
	 * Return the ServletContext (if setup in a servlet container environment).
	 */
	public static synchronized ServletContext getServletContext() {
		
		return PropertyMapLoader.getServletContext();
	}
	
	private static void initPropertyMap() {
		
		String fileName = System.getenv("EBEAN_PROPS_FILE");
		if (fileName == null) {
			fileName = System.getProperty("ebean.props.file");
			if (fileName == null) {
				fileName = "ebean.properties";
			}
		}
		
		globalMap = PropertyMapLoader.load(null, fileName);
		if (globalMap == null){
			// ebean.properties file was not found... but that 
			// is ok because we are likely doing programmatic config
			globalMap = new PropertyMap();
		}
		
		String loaderCn = globalMap.get("ebean.properties.loader");
		if (loaderCn != null){
			// a Runnable that can be used to customise the initialisation
			// of the GlobalProperties
			try {
			    Runnable r = (Runnable)ClassUtil.newInstance(loaderCn);
				r.run();
			} catch (Exception e){
				String m = "Error creating or running properties loader "+loaderCn;
				throw new RuntimeException(m, e);
			}
		}
	}
	
	/**
	 * Return the property map loading it if required.
	 */
	private static synchronized PropertyMap getPropertyMap() {
		
		if (globalMap == null){
			initPropertyMap();
		}
		
		return globalMap;
	}
	
	/**
	 * Return a String property with a default value.
	 */
	public static synchronized String get(String key, String defaultValue) {
		return getPropertyMap().get(key, defaultValue);
	}
	
	/**
	 * Return a int property with a default value.
	 */
	public static synchronized int getInt(String key, int defaultValue) {
		return getPropertyMap().getInt(key, defaultValue);
	}
	
	/**
	 * Return a boolean property with a default value.
	 */
	public static synchronized boolean getBoolean(String key, boolean defaultValue) {
		return getPropertyMap().getBoolean(key, defaultValue);
	}
	
	/**
	 * Set a property return the previous value.
	 */
	public static synchronized String put(String key, String defaultValue) {
		return getPropertyMap().put(key, defaultValue);
	}

	/**
	 * Set a Map of key value properties.
	 */
	public static synchronized void putAll(Map<String,String> keyValueMap) {
		getPropertyMap().putAll(keyValueMap);
	}
}
