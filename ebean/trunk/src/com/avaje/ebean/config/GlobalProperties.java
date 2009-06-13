package com.avaje.ebean.config;

import javax.servlet.ServletContext;

/**
 * Provides access to properties loaded from the ebean.properties file.
 */
public final class GlobalProperties {
	
	private static PropertyMap globalMap;
	
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
	
	/**
	 * Return the property map loading it if required.
	 */
	private static synchronized PropertyMap getPropertyMap() {
		
		if (globalMap == null){
			globalMap = PropertyMapLoader.load(null, "ebean.properties");
		}
		
		return globalMap;
	}
	
	public static synchronized String get(String key, String defaultValue) {
		return getPropertyMap().get(key, defaultValue);
	}
	
	public static synchronized int getInt(String key, int defaultValue) {
		return getPropertyMap().getInt(key, defaultValue);
	}
	
	public static synchronized boolean getBoolean(String key, boolean defaultValue) {
		return getPropertyMap().getBoolean(key, defaultValue);
	}
	
	public static synchronized String put(String key, String defaultValue) {
		return getPropertyMap().put(key, defaultValue);
	}
}
