/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.lib;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.avaje.lib.log.LogFactory;

/**
 * Helper used by SystemProperties to evaluate expressions such as
 * ${CATALINA_HOME}.
 * <p>
 * The expressions can contain environment variables, JNDI properties or other
 * properties. JNDI expressions take the form ${jndi:propertyName} where you
 * substitue propertyName with the name of the jndi property you wish to
 * evaluate.
 * </p>
 */
public class PropertyEvaluator {

	private static final Logger logger = LogFactory.get(PropertyEvaluator.class);

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

	final PropertyHolder propertyHolder;

	/**
	 * Use LogManager as the propertyHolder.
	 */
	public PropertyEvaluator(LogManager logManager) {
		this(new LogPropHolder());
	}

	/**
	 * Specify the PropertyHolder.
	 */
	public PropertyEvaluator(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
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
				val = propertyHolder.getProperty(exp);
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
	public String eval(String val) {
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
	public static boolean hasExpression(String val) {
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


	private static class LogPropHolder implements PropertyHolder {

		public String getProperty(String name) {
			return LogManager.getLogManager().getProperty(name);
		}
	}
}
