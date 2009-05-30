package com.avaje.ebean.config;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.servlet.ServletContext;


/**
 * Finds and translates the properties used to configure Ebean.
 */
public class GlobalProperties {
	
	private static final Logger logger = Logger.getLogger(GlobalProperties.class.getName());

	private static final String propsFile0 = "ebean.properties";
	private static final String propsFile1 = "avaje.properties";
	
	private static ServletContext servletContext;
	
	private static ConfigProperties configProperties;
	
	public synchronized static ConfigProperties getConfigProperties() {
		
		if (configProperties == null) {
			configProperties = defaultInit();
		}
		
		return configProperties;
	}
	
	public synchronized static void setProperty(String key, String value) {
		getConfigProperties().setProperty(key, value);
	}
	
	public synchronized static String getProperty(String key) {
		return getConfigProperties().getProperty(key);
	}
	
	public synchronized static ServletContext getServletContext() {
		return servletContext;
	}
	
	/**
	 * Initialise when run in a Servlet Container. It looks for the
	 * <code>props.file</code> parameter which should contain the name of the
	 * file to load the properties from.
	 */
	public synchronized static void initWebapp(ServletContext sc) {

		servletContext = sc;

		// this will be null for unpacked war
		// If not null then the WEB-INF sub directories could be used
		// to write content to (as opposed to being read-only).
		//String realPath = servletContext.getRealPath("/");

		// try loading from WEB-INF
		InputStream in = null;
		in = servletContext.getResourceAsStream("/WEB-INF/" + propsFile0);
		if (in == null) {
			in = servletContext.getResourceAsStream("/WEB-INF/" + propsFile1);
		}
		if (in == null) {
			in = ConfigProperties.getFromClasspath(propsFile0);
		}
		if (in == null) {
			in = ConfigProperties.getFromClasspath(propsFile1);
		}

		// load the properties
		if (in != null) {			
			configProperties = new ConfigProperties(in);
			
		} else {
			// try environment variables and working directory
			configProperties = defaultInit();
		}
	}
	
	/**
	 * Load the properties assuming a avaje.properties file.
	 * <p>
	 * This will be used when run in 'Standalone' mode as opposed to when being
	 * run inside a Servlet Container.
	 * </p>
	 * <p>
	 * Firstly try the Environment variable EBEAN_PROPS_FILE, then try the
	 * System property "avaje.props.file" then try the default name of
	 * "avaje.properties".
	 * </p>
	 */
	private static ConfigProperties defaultInit() {
		String fileName = System.getenv("EBEAN_PROPS_FILE");
		if (fileName == null) {
			fileName = System.getProperty("ebean.props.file");
		}

		ConfigProperties configProperties = null;
		
		if (fileName != null){
			configProperties = ConfigProperties.create(fileName);
		}
		if (configProperties == null){
			// search for ebean.properties
			configProperties = ConfigProperties.create(propsFile0);
		}
		if (configProperties == null){
			// search for avaje.properties
			configProperties = ConfigProperties.create(propsFile1);
		}
		if (configProperties == null){
			logger.warning("ebean.properties file not found");
			return new ConfigProperties(new Properties());
		}
		//loggingInit(configProperties);
		return configProperties;
	}

	
//	private static void loggingInit(ConfigProperties configProperties) {
//		
//		String loggingProps = configProperties.getProperty("logging.properties.file");
//		if (loggingProps != null){
//			try {
//				InputStream is = ConfigProperties.findInputStream(loggingProps);
//				if (is != null){
//					LogManager logManager = LogManager.getLogManager();
//					logManager.readConfiguration(is);
//				} else {
//					String msg = "Logging Properties file ["+loggingProps+"] not found";
//					logger.warning(msg);
//				}
//			} catch (Exception e) {
//				String msg = "Error initialising LogManager";
//				logger.log(Level.SEVERE, msg, e);
//			}
//		}
//	}
	
}
