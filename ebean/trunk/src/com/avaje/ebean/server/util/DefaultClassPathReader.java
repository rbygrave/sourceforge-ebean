package com.avaje.ebean.server.util;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.logging.Logger;

/**
 * Default implementation for getting the classpath from the classLoader.
 * <p>
 * This class path is used to search for entity beans etc.
 * </p>
 */
public class DefaultClassPathReader implements ClassPathReader {

	private static final Logger logger = Logger.getLogger(DefaultClassPathReader.class.getName());

	public Object[] readPath(ClassLoader classLoader)  {
		
		if (classLoader instanceof URLClassLoader){
			// this is really what we are hoping for
			URLClassLoader ucl = (URLClassLoader)classLoader;
			return ucl.getURLs();
		}
		
		try {
			// search for a "getClassPath" method... resin2
			Method  method = classLoader.getClass().getMethod("getClassPath");
			if (method != null){
				logger.fine("Using getClassPath() method on classLoader["+classLoader.getClass()+"]");
				String s = method.invoke(classLoader).toString();
				return s.split(File.pathSeparator);
			}
		} catch (NoSuchMethodException e) {
			// Not really an error...
		
		} catch (Exception e) {
			throw new RuntimeException("Unexpected Error trying to read classpath from classloader", e);
		}

		String imsg = "Unsure how to read classpath from classLoader ["+classLoader.getClass()+"]";
		logger.info(imsg);
		
		String msg = "Using java.class.path system property to search for entity beans";
		logger.warning(msg);
		
		return System.getProperty("java.class.path", "").split(File.pathSeparator);
	}

	
}
