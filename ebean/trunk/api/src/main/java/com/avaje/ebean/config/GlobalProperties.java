package com.avaje.ebean.config;

import javax.servlet.ServletContext;

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
	
	/**
	 * Return the property map loading it if required.
	 */
	private static synchronized PropertyMap getPropertyMap() {
		
		if (globalMap == null){
			
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
}
