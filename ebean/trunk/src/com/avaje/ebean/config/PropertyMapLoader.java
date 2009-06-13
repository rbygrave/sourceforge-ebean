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

final class PropertyMapLoader {

	private static Logger logger = Logger.getLogger(PropertyMapLoader.class.getName());
	
	private static ServletContext servletContext;

	public static ServletContext getServletContext() {
		return servletContext;
	}
	
	public static void setServletContext(ServletContext servletContext) {
		PropertyMapLoader.servletContext = servletContext;
	}

	public static PropertyMap load(PropertyMap p, String fileName){

		InputStream is = findInputStream(fileName);
		if (is == null){
			return null;
		} else {
			return load(p, is);
		}
	}
	
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
			
			p.put(key, val);
		}
		
		String otherProps = (String)p.remove("load.properties");
		if (otherProps != null){
			InputStream is = findInputStream(otherProps);
			if (is != null){
				load(p, in);
			} else {
				logger.severe("load.properties "+otherProps+" not found.");
			}
		}
		
		return p;
	}

	private static InputStream findInputStream(String fileName) {

		if (fileName == null) {
			throw new NullPointerException("fileName is null?");
		}

		if (servletContext != null){
			// first look in WEB-INF ...
			InputStream in = servletContext.getResourceAsStream("/WEB-INF/" + fileName);
			if (in != null){
				return in;
			}
		}

		try {
			File f = new File(fileName);

			if (f.exists()) {
				return new FileInputStream(f);
			} else {
				return findInClassPath(fileName);
			}

		} catch (FileNotFoundException ex) {
			// already made the check so this 
			// should never be thrown
			throw new RuntimeException(ex);
		} 
	}

	private static InputStream findInClassPath(String fileName) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
	}
	
}
