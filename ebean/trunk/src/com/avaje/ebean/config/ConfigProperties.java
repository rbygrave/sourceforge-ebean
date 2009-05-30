package com.avaje.ebean.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Configuration properties that includes evaluation of expressions using
 * JNDI, environment variables and other properties (e.g. ${CATALINA_HOME}).
 * <p>
 * This will also use a properties "load.properties" and "load.properties.override"
 * to recursively load another properties file. This is typically used to load
 * environment specific properties (DEV,UAT,PRD etc) from a known location such
 * as ${catalina.base}/conf/myapp.properties for tomcat.
 * </p>
 * <p>
 * Note that keys are case insensitive (set to lowercase for setting and getting).
 * </p>
 */
public final class ConfigProperties {

	private static final Logger logger = Logger.getLogger(ConfigProperties.class.getName());
	
	private final PropertyEvaluator propertyEvaluator;
	
	private final Map<String,String> map = new LinkedHashMap<String, String>();
		
	/**
	 * Create based on Properties.
	 */
	public ConfigProperties(Properties properties){
		this(null, properties);
	}

	/**
	 * Create when there is a parent configuration.
	 * <p>
	 * Properties are loaded from the parent configuration.
	 * </p>
	 */
	public ConfigProperties(ConfigProperties parentProps, Properties properties){
		
		propertyEvaluator = new PropertyEvaluator(this);
		
		if (parentProps != null){
			map.putAll(parentProps.map);
		}
		
		if (properties != null){
			add(properties, true);
		}
	}
	
	/**
	 * Create from an properties Inputstream.
	 */
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
	static InputStream getFromClasspath(String fileName) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
	}
	
	synchronized static InputStream findInputStream(String fileName) {

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
	 * Return the the properties by loading a file.
	 */
	public static ConfigProperties create(String fileName) {
		
		Properties props = findProperties(fileName);
		if (props == null){
			return null;
		}

		return new ConfigProperties(props);
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
		map.put(lowerKey, val);
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

	/**
	 * Return an unmodifiable Map view of the properties.
	 */
	public synchronized  Map<String,String> getMap(){
		return Collections.unmodifiableMap(map);
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
	
	/**
	 * Helper used to evaluate expressions such as ${CATALINA_HOME}.
	 * <p>
	 * The expressions can contain environment variables, JNDI properties or other
	 * properties. JNDI expressions take the form ${jndi:propertyName} where you
	 * substitute propertyName with the name of the jndi property you wish to
	 * evaluate.
	 * </p>
	 */
	private static final class PropertyEvaluator {

		private static final Logger logger = Logger.getLogger(PropertyEvaluator.class.getName());

		/**
		 * Prefix for looking up JNDI Environment variable.
		 */
		private static final String JAVA_COMP_ENV = "java:comp/env/";

		/**
		 * Used to detect the start of an expression.
		 */
		private static String START = "${";

		/**
		 * Used to detect the end of an expression.
		 */
		private static String END = "}";

		final ConfigProperties configProperties;

		/**
		 * Specify the PropertyHolder.
		 */
		PropertyEvaluator(ConfigProperties configProperties) {
			this.configProperties = configProperties;
		}

		/**
		 * Convert the expression using JNDI, Environment variables, System
		 * Properties or existing an property in SystemProperties itself.
		 */
		private String evaluateExpression(String exp) {

			if (isJndiExpression(exp)) {
				// JNDI property lookup...
				String val = getJndiProperty(exp);
				if (val != null) {
					return val;
				}
			}

			// check Environment Variables first
			String val = System.getenv(exp);
			if (val == null) {
				// Properties from command line etc
				val = System.getProperty(exp);
				if (val == null) {
					// Already existing properties
					val = configProperties.getProperty(exp);
				}
				if (val == null) {
					// this is probably an error
					String msg = "SystemProperties unable to evaluate expression [" + exp + "]";
					logger.warning(msg);
					val = exp;
				}
			}
			return val;
		}

		/**
		 * Return the property value evaluating and replacing any expressions such
		 * as ${CATALINA_HOME}.
		 * <p>
		 * Note that the actual evaluation occurs in
		 * SystemProperties.evaluateExpression().
		 * </p>
		 */
		String eval(String val) {
			if (val == null){
				return null;
			}
			int sp = val.indexOf(START);
			if (sp > -1) {
				int ep = val.indexOf(END, sp + 1);
				if (ep > -1) {
					return eval(val, sp, ep);
				}
			}
			return val;
		}

		/**
		 * Return true if the value contains an expression.
		 */
		boolean hasExpression(String val) {
			int sp = val.indexOf(START);
			if (sp > -1) {
				int ep = val.indexOf(END, sp + 1);
				if (ep > -1) {
					return true;
				}
			}
			return false;
		}

		private String eval(String val, int sp, int ep) {

			StringBuilder sb = new StringBuilder();
			sb.append(val.substring(0, sp));

			String cal = evalExpression(val, sp, ep);
			sb.append(cal);

			eval(val, ep + 1, sb);

			return sb.toString();
		}

		private void eval(String val, int startPos, StringBuilder sb) {
			if (startPos < val.length()) {
				int sp = val.indexOf(START, startPos);
				if (sp > -1) {
					// append what is between the last token and the new one (if startPos == sp nothing gets added)
					sb.append(val.substring(startPos, sp));
					int ep = val.indexOf(END, sp + 1);
					if (ep > -1) {
						String cal = evalExpression(val, sp, ep);
						sb.append(cal);
						eval(val, ep + 1, sb);
						return;
					}
				}
			}
			// append what is left...
			sb.append(val.substring(startPos));
		}

		private String evalExpression(String val, int sp, int ep) {
			// trim off start and end ${ and }
			String exp = val.substring(sp + START.length(), ep);

			// evaluate the variable
			return evaluateExpression(exp);
		}

		private static boolean isJndiExpression(String exp) {
			if (exp.startsWith("JNDI:")) {
				return true;
			}
			if (exp.startsWith("jndi:")) {
				return true;
			}
			return false;
		}

		/**
		 * Returns null if JNDI is not setup or if the property is not found.
		 * 
		 * @param key
		 *            the key of the JNDI Environment property including a JNDI:
		 *            prefix.
		 */
		private static String getJndiProperty(String key) {

			try {
				// remove the JNDI: prefix
				key = key.substring(5);

				return (String) getJndiObject(key);

			} catch (NamingException ex) {
				return null;
			}
		}

		/**
		 * Similar to getProperty but throws NamingException if JNDI is not setup or
		 * if the property is not found.
		 */
		private static Object getJndiObject(String key) throws NamingException {

			InitialContext ctx = new InitialContext();
			return ctx.lookup(JAVA_COMP_ENV + key);
		}

	}

}
