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
package org.avaje.ebean.server.core;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.avaje.ebean.server.lib.ConfigProperties;
import org.avaje.ebean.server.util.ClassPathSearch;
import org.avaje.ebean.server.util.ClassPathSearchFilter;
import org.avaje.ebean.server.util.ClassPathSearchMatcher;
import org.avaje.lib.log.LogFactory;

/**
 * Searches for interesting classes such as Entities, Embedded and ScalarTypes.
 */
public class BootupClassPathSearch implements ClassPathSearchMatcher {

	private static final Logger logger = LogFactory.get(BootupClassPathSearch.class);

	final String monitor = new String();

	final BootupClasses bootupClasses = new BootupClasses();

	final ClassLoader classLoader;

	final ConfigProperties baseProperties;

	boolean performedSearch;

	/**
	 * Construct and search for interesting classes.
	 */
	public BootupClassPathSearch(ClassLoader classLoader, ConfigProperties baseProperties) {
		this.classLoader = (classLoader == null) ? getClass().getClassLoader() : classLoader;
		this.baseProperties = baseProperties;
	}

	public BootupClasses getBootupClasses() {
		return bootupClasses;
	}

	public boolean isMatch(Class<?> cls) {
		return bootupClasses.isMatch(cls);
	}

	/**
	 * Search the classPath for the classes we are interested in.
	 * 
	 * @param classLoader
	 *            can be null in which case we use the classLoader of this
	 *            object.
	 */
	public void search() {
		synchronized (monitor) {
			try {
				if (performedSearch) {
					return;
				}

				long st = System.currentTimeMillis();

				ClassPathSearchFilter filter = createFilter();

				ClassPathSearch finder = new ClassPathSearch(classLoader, filter, this);

				finder.findClasses();
				Set<String> jars = finder.getJarHits();
				Set<String> pkgs = finder.getPackageHits();

				long searchTime = System.currentTimeMillis() - st;

				String msg = "Classpath search hits in jars" + jars + " pkgs" + pkgs + "  searchTime[" + searchTime
						+ "]";
				logger.info(msg);

				performedSearch = true;

			} catch (Exception ex) {
				logger.log(Level.SEVERE, "Error", ex);
			}
		}
	}

	private ClassPathSearchFilter createFilter() {

		ClassPathSearchFilter filter = new ClassPathSearchFilter();
		filter.addDefaultExcludePackages();

		String packages = baseProperties.getProperty("ebean.search.packages", null);
		if (packages != null) {
			String[] packageList = packages.split(",");
			for (int i = 0; i < packageList.length; i++) {
				filter.includePackage(packageList[i].trim());
			}
		}

		String searchJars = baseProperties.getProperty("ebean.search.jars", null);
		if (searchJars != null) {
			String[] jarList = searchJars.split(",");
			for (int i = 0; i < jarList.length; i++) {
				filter.includeJar(jarList[i].trim());
			}
		}

		return filter;
	}
}
