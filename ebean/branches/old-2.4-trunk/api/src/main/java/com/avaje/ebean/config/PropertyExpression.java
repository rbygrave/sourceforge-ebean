package com.avaje.ebean.config;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Helper used to evaluate expressions such as ${CATALINA_HOME}.
 * <p>
 * The expressions can contain environment variables, system properties
 * or JNDI properties. JNDI expressions take the form ${jndi:propertyName} 
 * where you substitute propertyName with the name of the jndi property 
 * you wish to evaluate.
 * </p>
 */
final class PropertyExpression {

	private static final Logger logger = Logger.getLogger(PropertyExpression.class.getName());

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

	/**
	 * Specify the PropertyHolder.
	 */
	private PropertyExpression() {
	}

	/**
	 * Return the property value evaluating and replacing any expressions such
	 * as ${CATALINA_HOME}.
	 */
	static String eval(String val) {
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
	 * Convert the expression using JNDI, Environment variables, System
	 * Properties or existing an property in SystemProperties itself.
	 */
	private static String evaluateExpression(String exp) {

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
			// then check system properties
			val = System.getProperty(exp);
		}
		
		if (val != null) {
			return val;

		} else {
			// this is probably an error
			String msg = "Unable to evaluate expression [" + exp + "]";
			logger.warning(msg);
			return exp;
		}
	}

	private static String eval(String val, int sp, int ep) {

		StringBuilder sb = new StringBuilder();
		sb.append(val.substring(0, sp));

		String cal = evalExpression(val, sp, ep);
		sb.append(cal);

		eval(val, ep + 1, sb);

		return sb.toString();
	}

	private static void eval(String val, int startPos, StringBuilder sb) {
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

	private static String evalExpression(String val, int sp, int ep) {
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