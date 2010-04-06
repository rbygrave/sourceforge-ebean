package com.avaje.ebean.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

/**
 * Helper used to load the PropertyMap.
 */
final class PropertyMapLoader {

	private static Logger logger = Logger.getLogger(PropertyMapLoader.class.getName());
	
	private static ServletContext servletContext;

	/**
	 * Return the servlet context when in a web environment.
	 */
	public static ServletContext getServletContext() {
		return servletContext;
	}
	
	/**
	 * Set the ServletContext for when ebean.properties is in WEB-INF
	 * in a web application environment.
	 */
	public static void setServletContext(ServletContext servletContext) {
		PropertyMapLoader.servletContext = servletContext;
	}

	/**
	 * Load the file returning the property map.
	 * @param p an existing property map to load into.
	 * @param fileName the name of the properties file to load.
	 */
	public static PropertyMap load(PropertyMap p, String fileName){

		ResourceFinder dataSource=new ResourceFinder(fileName,servletContext);
		if (dataSource.exists()){
			return load(p, dataSource.getInputSteram());
		} else {
			logger.severe(fileName+" not found");
			return p;
		}
	}
	
	/**
	 * Load the inputstream returning the property map.
	 * @param p an existing property map to load into.
	 * @param in the InputStream of the properties file to load.
	 */
	private static PropertyMap load(PropertyMap p, InputStream in){
		
		Properties props = new Properties();
		try {
			props.load(in);
			in.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		if (p == null){
			p = new PropertyMap();
		}
		
		Iterator<Entry<Object, Object>> it = props.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Object, Object> entry = it.next();
			String key = ((String)entry.getKey()).toLowerCase();
			String val = ((String)entry.getValue());
			val = PropertyExpression.eval(val);
			
			logger.finer("... loading "+key+" = "+val);
			p.put(key, val);
		}
		
		String otherProps = p.remove("load.properties");
		if (otherProps == null){
			otherProps = p.remove("load.properties.override");
		}
		if (otherProps != null){
			otherProps = otherProps.replace("\\", "/");
			ResourceFinder dataSource=new ResourceFinder(otherProps,servletContext);
			if (dataSource.exists()){
				logger.fine("loading properties from "+otherProps);
				load(p, dataSource.getInputSteram());
			} else {
				logger.severe("load.properties "+otherProps+" not found.");
			}
		}

		return p;
	}
	
}
