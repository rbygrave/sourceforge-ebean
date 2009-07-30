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
package com.avaje.ebean.server.core;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.server.util.ClassPathSearch;
import com.avaje.ebean.server.util.ClassPathSearchFilter;

/**
 * Searches for interesting classes such as Entities, Embedded and ScalarTypes.
 */
public class BootupClassPathSearch {

	private static final Logger logger = Logger.getLogger(BootupClassPathSearch.class.getName());

	private final Object monitor = new Object();

	private final ClassLoader classLoader;

	private final List<String> packages;
	
	private BootupClasses bootupClasses;

	/**
	 * Construct and search for interesting classes.
	 */
	public BootupClassPathSearch(ClassLoader classLoader, List<String> packages) {
		this.classLoader = (classLoader == null) ? getClass().getClassLoader() : classLoader;
		this.packages = packages;
	}

	public BootupClasses getBootupClasses() {
		synchronized (monitor) {
			
			if (bootupClasses == null){
				bootupClasses = search();
			}
			
			return bootupClasses;
		}
	}

	/**
	 * Search the classPath for the classes we are interested in.
	 */
	private BootupClasses search() {
		synchronized (monitor) {
			try {
				
				BootupClasses bc = new BootupClasses();

				long st = System.currentTimeMillis();

				ClassPathSearchFilter filter = createFilter();

				ClassPathSearch finder = new ClassPathSearch(classLoader, filter, bc);

				finder.findClasses();
				Set<String> jars = finder.getJarHits();
				Set<String> pkgs = finder.getPackageHits();

				long searchTime = System.currentTimeMillis() - st;

				String msg = "Classpath search hits in jars" + jars + " pkgs" + pkgs + "  searchTime[" + searchTime+ "]";
				logger.info(msg);

				return bc;

			} catch (Exception ex) {
				String msg = "Error in classpath search (looking for entities etc)";
				throw new RuntimeException(msg, ex);
			}
		}
	}

	private ClassPathSearchFilter createFilter() {

		ClassPathSearchFilter filter = new ClassPathSearchFilter();
		filter.addDefaultExcludePackages();

		String searchPackages = GlobalProperties.get("ebean.search.packages", null);
		if (searchPackages != null) {
			String[] packageList = searchPackages.split(",");
			for (int i = 0; i < packageList.length; i++) {
				filter.includePackage(packageList[i].trim());
			}
		}
		
		if (packages != null){
			for (String packageName : packages) {
				filter.includePackage(packageName);
			}
		}

		String searchJars = GlobalProperties.get("ebean.search.jars", null);
		if (searchJars != null) {
			String[] jarList = searchJars.split(",");
			for (int i = 0; i < jarList.length; i++) {
				filter.includeJar(jarList[i].trim());
			}
		}

		return filter;
	}
}
