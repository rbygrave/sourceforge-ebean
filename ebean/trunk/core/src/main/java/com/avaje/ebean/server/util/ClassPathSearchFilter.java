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
package com.avaje.ebean.server.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Used to reduce the classes searched by excluding jars and packages.
 */
public class ClassPathSearchFilter {

	boolean defaultPackageMatch = true;

	boolean defaultJarMatch = false;

	String ebeanJarPrefix = "ebean";

	HashSet<String> includePackageSet = new HashSet<String>();

	HashSet<String> excludePackageSet = new HashSet<String>();

	HashSet<String> includeJarSet = new HashSet<String>();

	HashSet<String> excludeJarSet = new HashSet<String>();

	public ClassPathSearchFilter() {
		addDefaultExcludePackages();
	}

	/**
	 * Set the name of the ebean jar file. Used with class path search to find
	 * the "Meta" entity beans that are contained within the ebean jar.
	 * <p>
	 * You only need to set this if the ebean jar file name starts with
	 * something other than ebean.
	 * </p>
	 */
	public void setEbeanJarPrefix(String ebeanJarPrefix) {
		this.ebeanJarPrefix = ebeanJarPrefix;
	}

	/**
	 * Return the explicit packages that should be searched.
	 */
	public Set<String> getIncludePackages() {
		return includePackageSet;
	}

	/**
	 * Add some packages which by default will be excluded from a search.
	 * <p>
	 * This includes java, javax, etc.
	 * </p>
	 * <p>
	 * This is not used when the includePackages is set, but can speed a search
	 * when includePackages has not been set.
	 * </p>
	 */
	public void addDefaultExcludePackages() {
		excludePackage("sun");
		excludePackage("com.sun");
		excludePackage("java");
		excludePackage("javax");
		excludePackage("junit");
		excludePackage("org.w3c");
		excludePackage("org.xml");
		excludePackage("org.apache");
		excludePackage("com.mysql");
		excludePackage("oracle.jdbc");
		excludePackage("com.microsoft.sqlserver");
		excludePackage("com.avaje.ebean");
		excludePackage("com.avaje.lib");
	}

	/**
	 * Clear all entries from the exclude packages list.
	 * <p>
	 * This includes the entries added by addDefaultExcludePackages() which is
	 * done on construction.
	 * </p>
	 */
	public void clearExcludePackages() {
		excludePackageSet.clear();
	}

	/**
	 * Set the default for jar matching when a jar is neither explicitly
	 * included or excluded.
	 */
	public void setDefaultJarMatch(boolean defaultJarMatch) {
		this.defaultJarMatch = defaultJarMatch;
	}

	/**
	 * Set the default for package matching when a package is neither explicitly
	 * included or excluded.
	 */
	public void setDefaultPackageMatch(boolean defaultPackageMatch) {
		this.defaultPackageMatch = defaultPackageMatch;
	}

	/**
	 * Add a package to explicitly include in the search.
	 */
	public void includePackage(String pckgName) {
		includePackageSet.add(pckgName);
	}

	/**
	 * Add a package to explicitly exclude in the search.
	 */
	public void excludePackage(String pckgName) {
		excludePackageSet.add(pckgName);
	}

	/**
	 * Add a jar to explicitly exclude in the search.
	 */
	public void excludeJar(String jarName) {
		includeJarSet.add(jarName);
	}

	/**
	 * Add a jar to explicitly include in the search.
	 */
	public void includeJar(String jarName) {
		includeJarSet.add(jarName);

	}

	/**
	 * Return true if the package should be included in the search.
	 */
	public boolean isSearchPackage(String packageName) {
		// special case... "meta" entity beans.
		if ("com.avaje.ebean.meta".equals(packageName)) {
			return true;
		}
		// special case... BeanFinders etc for "meta" beans.
		if ("com.avaje.ebean.server.bean".equals(packageName)) {
			return true;
		}
		if (containedIn(includePackageSet, packageName)) {
			return true;
		}

		if (containedIn(excludePackageSet, packageName)) {
			return false;
		}
		return defaultPackageMatch;
	}

	/**
	 * Return true if the jar should be included in the search.
	 */
	public boolean isSearchJar(String jarName) {
		if (jarName.startsWith(ebeanJarPrefix)) {
			return true;
		}

		if (containedIn(includeJarSet, jarName)) {
			return true;
		}

		if (containedIn(excludeJarSet, jarName)) {
			return false;
		}
		return defaultJarMatch;
	}

	/**
	 * Helper method to determine is a match is contained in the set.
	 */
	protected boolean containedIn(HashSet<String> set, String match) {
		if (set.contains(match)) {
			return true;
		}
		Iterator<String> incIt = set.iterator();
		while (incIt.hasNext()) {
			String val = incIt.next();
			if (match.startsWith(val)) {
				return true;
			}
		}
		return false;
	}

}
