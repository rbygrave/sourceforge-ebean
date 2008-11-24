package com.avaje.ebean.enhance.agent;

import java.io.PrintStream;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to hold meta data, arguments and log levels for the enhancement.
 */
public class EnhanceContext {

	static final Logger logger = Logger.getLogger(EnhanceContext.class
			.getName());

	final IgnoreClassHelper ignoreClassHelper;

	final boolean subclassing;

	final String agentArgs;

	final HashMap<String, String> agentArgsMap;

	final boolean readOnly;

	PrintStream logout;

	int logLevel;

	HashMap<String, ClassMeta> map = new HashMap<String, ClassMeta>();

	/**
	 * Additions to the class path used to process inheritance.
	 */
	URL[] extraClassPath;

	ClassMetaReader reader;

	public EnhanceContext(URL[] extraClassPath, String agentArgs) {
		this(extraClassPath, false, agentArgs);

	}

	public EnhanceContext(String agentArgs) {
		this(null, true, agentArgs);
	}

	/**
	 * Construct a context for enhancement or subclass generation.
	 * 
	 * @param subclassGeneration
	 *            true if generating subclasses (false for javaagent etc)
	 * @param agentArgs
	 *            parameters for enhancement such as log level
	 */
	private EnhanceContext(URL[] extraClassPath, boolean subclassing,
			String agentArgs) {

		this.ignoreClassHelper = new IgnoreClassHelper(agentArgs);
		this.extraClassPath = extraClassPath;
		this.subclassing = subclassing;
		this.agentArgs = agentArgs;
		this.agentArgsMap = ArgParser.parse(agentArgs);

		this.logout = System.out;

		this.reader = new ClassMetaReader(this, extraClassPath);

		String debugValue = agentArgsMap.get("debug");
		if (debugValue != null) {
			try {
				logLevel = Integer.parseInt(debugValue);
			} catch (NumberFormatException e) {
				String msg = "Agent debug argument [" + debugValue
						+ "] is not an int?";
				logger.log(Level.WARNING, msg);
			}
		}

		String readonlyValue = agentArgsMap.get("readonly");
		if (readonlyValue != null) {
			readOnly = readonlyValue.trim().equalsIgnoreCase("true");
		} else {
			readOnly = false;
		}
	}
	
	/**
	 * Return a value from the agent arguments using its key.
	 */
	public String getProperty(String key){
		return agentArgsMap.get(key);
	}

	public boolean getPropertyBoolean(String key, boolean dflt){
		String s = getProperty(key);
		if (s == null){
			return dflt;
		} else {
			return s.trim().equalsIgnoreCase("true");
		}
	}

	
	/**
	 * Return true if this class should be ignored. That is JDK classes and
	 * known libraries JDBC drivers etc can be skipped.
	 */
	public boolean isIgnoreClass(String className) {
		return ignoreClassHelper.isIgnoreClass(className);
	}

	/**
	 * Change the logout to something other than system out.
	 */
	public void setLogout(PrintStream logout) {
		this.logout = logout;
	}

	/**
	 * Create a new meta object for enhancing a class.
	 */
	public ClassMeta createClassMeta() {
		return new ClassMeta(subclassing, logLevel, logout);
	}

	/**
	 * Read the class meta data for a super class.
	 * <p>
	 * Typically used to read meta data for inheritance hierarchy.
	 * </p>
	 */
	public ClassMeta getSuperMeta(String superClassName, ClassLoader classLoader) {

		try {
			if (isIgnoreClass(superClassName)){
				return null;
			}
			return reader.get(false, superClassName, classLoader);
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Read the class meta data for an interface.
	 * <p>
	 * Typically used to check the interface to see if it is transactional.
	 * </p>
	 */
	public ClassMeta getInterfaceMeta(String interfaceClassName, ClassLoader classLoader) {

		try {
			if (isIgnoreClass(interfaceClassName)){
				return null;
			}
			return reader.get(true, interfaceClassName, classLoader);
			
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void addClassMeta(ClassMeta meta) {
		map.put(meta.getClassName(), meta);
	}

	public ClassMeta get(String className) {
		return map.get(className);
	}

	/**
	 * Log some debug output.
	 */
	public void log(int level, String msg) {
		if (logLevel >= level) {
			logout.println(msg);
		}
	}
	
	public void log(String className, String msg) {
		if (className != null) {
			msg = "cls: " + className + "  msg: " + msg;
		}
		logout.println("transform> " + msg);
	}
	
	public boolean isLog(int level){
		return logLevel >= level;
	}

	/**
	 * Log an error.
	 */
	public void log(Throwable e) {
		e.printStackTrace(logout);
	}

	/**
	 * Return the log level.
	 */
	public int getLogLevel() {
		return logLevel;
	}

	/**
	 * Return true if this should go through the enhancement process but not
	 * actually save the enhanced classes.
	 * <p>
	 * Set this to true to run through the enhancement process without actually
	 * doing the enhancement for debugging etc.
	 * </p>
	 */
	public boolean isReadOnly() {
		return readOnly;
	}

}
