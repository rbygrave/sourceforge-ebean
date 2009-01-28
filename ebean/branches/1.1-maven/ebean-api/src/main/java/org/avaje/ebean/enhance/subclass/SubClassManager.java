/**
 * Copyright (C) 2006  Robin Bygrave
 * 
 * This file is part of Ebean.
 * 
 * Ebean is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *  
 * Ebean is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Ebean; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA  
 */
package org.avaje.ebean.enhance.subclass;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.PersistenceException;

import org.avaje.ebean.Ebean;
import org.avaje.ebean.enhance.agent.EnhanceConstants;
import org.avaje.ebean.server.plugin.PluginProperties;

/**
 * Creates and caches the dynamically generated subclasses.
 * <p>
 * That is, the 'EntityBean' classes are dynamically generated subclasses of the
 * 'vanilla' classes.
 * </p>
 */
public class SubClassManager implements EnhanceConstants {

	final ConcurrentHashMap<String,Class<?>> clzMap;

	final SubClassFactory subclassFactory;

	final String serverName;

	/**
	 * The log level for debugging subclass generation/enhancement.
	 */
	final int logLevel;
	
	/**
	 * Construct with the ClassLoader used to load Ebean.class.
	 */
	@SuppressWarnings("unchecked")
	public SubClassManager(PluginProperties props) {
		
		this.serverName = props.getServerName();
		this.logLevel =  props.getPropertyInt("enhance.log.level", -1);
		this.clzMap = new ConcurrentHashMap<String, Class<?>>();
		
		try {
			subclassFactory = (SubClassFactory) AccessController
					.doPrivileged(new PrivilegedExceptionAction() {
						public Object run() {
							ClassLoader cl = Ebean.class.getClassLoader();
							return new SubClassFactory(cl, logLevel);
						}
					});
		} catch (PrivilegedActionException e) {
			throw new PersistenceException(e);
		}
	}

	/**
	 * Resolve the Class for the class name.
	 * <p>
	 * The methodInfo is used to determine the method interception on the
	 * generated class.
	 * </p>
	 * <p>
	 * If the class has already been generated then it is returned out of a
	 * cache.
	 * </p>
	 */
	public Class<?> resolve(String name) {

		String superName = SubClassUtil.getSuperClassName(name);

		Class<?> clz = (Class<?>) clzMap.get(superName);
		if (clz == null) {
			synchronized (this) {
				clz = (Class<?>) clzMap.get(superName);
				if (clz == null) {
					clz = createClass(superName);
					clzMap.put(superName, clz);
				}
			}
		}
		return clz;
	}

	private Class<?> createClass(String name) {

		try {

			Class<?> superClass = Class.forName(name);

			return subclassFactory.create(superClass, serverName);

		} catch (Exception ex) {
			String m = "Error creating subclass for [" + name + "]";
			throw new PersistenceException(m, ex);
		}
	}

}
