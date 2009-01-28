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
package org.avaje.lib.log;

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.avaje.lib.PropertyEvaluator;
import org.avaje.lib.util.FactoryHelper;

/**
 * A Helper object used to configure handlers.
 * <p>
 * Used to handle common configuration tasks. Typically reading configuration
 * properties and setting Formatter, Filter, encoding etc as required.
 * </p>
 */
public class HandlerConfig {

	boolean withSuper = true;

	HandlerConfigurable handler;

	LogManager logManager;

	PropertyEvaluator propertyEvaluator;

	/**
	 * Create with a given Handler.
	 */
	public HandlerConfig(HandlerConfigurable handler) {
		this.handler = handler;
		this.logManager = LogManager.getLogManager();
		this.propertyEvaluator = new PropertyEvaluator(logManager);
	}

	/**
	 * Set the level with a default level if none is explicitly specified.
	 */
	public void setLevel(Level dfltLevel) {
		String levelName = getProperty("level", null);
		if (levelName != null) {
			Level level = Level.parse(levelName);
			handler.setLevel(level);

		} else if (dfltLevel != null) {
			handler.setLevel(dfltLevel);
		}
	}

	/**
	 * Set the formatter with a default if none is specified.
	 */
	public void setFormatter(Formatter dfltFormatter) {
		String formatterCn = getProperty("formatter", null);
		if (formatterCn != null) {
			Formatter formatter = (Formatter) FactoryHelper.create(formatterCn);
			handler.setFormatter(formatter);

		} else if (dfltFormatter != null) {
			handler.setFormatter(dfltFormatter);
		}
	}

	/**
	 * Set the filter with a default if none is specified.
	 * @param dfltFilter
	 */
	public void setFilter(Filter dfltFilter) {
		String filterCn = getProperty("filter", null);
		if (filterCn != null) {
			Filter filter = (Filter) FactoryHelper.create(filterCn);
			handler.setFilter(filter);

		} else if (dfltFilter != null) {
			handler.setFilter(dfltFilter);
		}
	}

	/**
	 * Set the encoding.
	 */
	public void setEncoding() throws SecurityException, java.io.UnsupportedEncodingException {
		String encoding = getProperty("encoding", null);
		if (encoding != null) {
			handler.setEncoding(encoding);
		}
	}

	/**
	 * Return a property for this handler.
	 */
	public String getProperty(String propertyName, String dflt) {
		return getProperty(withSuper, true, propertyName, dflt);
	}

	/**
	 * Return a property specifying whether to use superClass property inheritance and expression evaluation.
	 * <p>
	 * Expression evaluation is used to convert expressions like ${user.home}.
	 * </p>
	 */
	public String getProperty(boolean withSuper, boolean withEval, String propertyName, String dflt) {
		String val = getProperty(withSuper, withEval, handler.getClass(), propertyName);
		if (val == null) {
			return dflt;
		} else {
			return val;
		}
	}

	/**
	 * Return a property for a specific handler class.
	 */
	public String getProperty(boolean withSuper, boolean withEval, Class<?> cls, String propertyName) {
		String key = cls.getName() + "." + propertyName;

		String val = logManager.getProperty(key);
		if (val == null && withSuper) {
			Class<?> superCls = cls.getSuperclass();
			if (superCls != null) {
				return getProperty(withSuper, withEval, superCls, propertyName);
			}
		}
		if (withEval) {
			val = propertyEvaluator.eval(val);
		}
		return val;
	}

	/**
	 * Evaluate a property value that can contain expressions like
	 * ${CATALINA_HOME} or ${user.home}.
	 */
	public String eval(String propertyValue) {
		return propertyEvaluator.eval(propertyValue);
	}
}
