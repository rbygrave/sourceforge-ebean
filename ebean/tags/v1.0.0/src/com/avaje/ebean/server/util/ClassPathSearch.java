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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.lib.log.LogFactory;

/**
 * Can search the class path for classes using a ClassPathSearchMatcher. A
 * ClassPathSearch should only be used once in a single threaded manor. It is
 * not safe for multithreaded use.
 * <p>
 * For example, used to find all the Entity beans and ScalarTypes for Ebean.
 * </p>
 */
public class ClassPathSearch {

	private static final Logger logger = LogFactory.get(ClassPathSearch.class);

	ClassLoader classLoader;

	Object[] classPaths;

	ClassPathSearchFilter filter;

	ClassPathSearchMatcher matcher;

	ArrayList<Class<?>> matchList = new ArrayList<Class<?>>();

	HashSet<String> jarHits = new HashSet<String>();

	HashSet<String> packageHits = new HashSet<String>();

	public ClassPathSearch(ClassLoader classLoader, ClassPathSearchFilter filter,
			ClassPathSearchMatcher matcher) {
		this.classLoader = classLoader;
		this.filter = filter;
		this.matcher = matcher;
		initClassPaths();
	}

	private void initClassPaths() {
		try {
			classPaths = ((java.net.URLClassLoader) classLoader).getURLs();
		} catch (ClassCastException e) {
			classPaths = System.getProperty("java.class.path", "").split(File.pathSeparator);
		}
		if (logger.isLoggable(Level.FINER)) {
			String msg = "Classpath " + Arrays.toString(classPaths);
			logger.finer(msg);
		}
	}

	/**
	 * Return the set of jars that contained classes that matched.
	 */
	public Set<String> getJarHits() {
		return jarHits;
	}

	/**
	 * Return the set of packages that contained classes that matched.
	 */
	public Set<String> getPackageHits() {
		return packageHits;
	}

	/**
	 * Register where matching classes where found.
	 * <p>
	 * Could use this info to speed up future searches.
	 * </p>
	 */
	private void registerHit(String jarFileName, Class<?> cls) {
		if (jarFileName != null) {
			jarHits.add(jarFileName);
		}
		Package pkg = cls.getPackage();
		if (pkg != null){
			packageHits.add(pkg.getName());			
		} else {
			packageHits.add("");
		}
	}

	/**
	 * Searches the class path for all matching classes.
	 */
	public List<Class<?>> findClasses() throws ClassNotFoundException {

		for (int h = 0; h < classPaths.length; h++) {
			String jarFileName = null;
			Enumeration<?> files = null;
			JarFile module = null;

			// for each class path ...
			File classPath = new File((URL.class).isInstance(classPaths[h]) ? ((URL) classPaths[h])
					.getFile() : classPaths[h].toString());

			if (classPath.isDirectory()) {
				files = getDirectoryEnumeration(classPath);

			} else if (classPath.getName().endsWith(".jar")) {
				jarFileName = classPath.getName();
				if (!filter.isSearchJar(jarFileName)) {
					// skip any jars not list in the filter
					continue;
				}
				try {
					// if our resource is a jar, instantiate a jarFile using the
					// full path to resource
					module = new JarFile(classPath);
					files = module.entries();

				} catch (MalformedURLException ex) {
					throw new ClassNotFoundException("Bad classpath. Error: ", ex);

				} catch (IOException ex) {
					String msg = "jar file '" + classPath.getName()
							+ "' could not be instantiate from file path. Error: ";
					throw new ClassNotFoundException(msg, ex);
				}
			}

			searchFiles(files, jarFileName);

			if (module != null) {
				try {
					// close the jar if it was used
					module.close();
				} catch (IOException e) {
					String msg = "Error closing jar";
					throw new ClassNotFoundException(msg, e);
				}
			}
		}

		return matchList;
	}

	private Enumeration<?> getDirectoryEnumeration(File classPath) {

		// list of file names (latter checked as Classes)
		ArrayList<String> fileNameList = new ArrayList<String>();

		Set<String> includePkgs = filter.getIncludePackages();
		if (includePkgs.size() > 0) {
			// just search the relevant directories based on the
			// list of included packages
			Iterator<String> it = includePkgs.iterator();
			while (it.hasNext()) {
				String pkg = it.next();
				String relPath = pkg.replace('.', '/');
				File dir = new File(classPath, relPath);
				if (dir.exists()) {
					recursivelyListDir(fileNameList, dir, new StringBuilder(relPath));
				}
			}

		} else {
			// get a recursive listing of this classpath
			recursivelyListDir(fileNameList, classPath, new StringBuilder());
		}

		return Collections.enumeration(fileNameList);
	}

	private void searchFiles(Enumeration<?> files, String jarFileName) {

		while (files != null && files.hasMoreElements()) {

			String fileName = files.nextElement().toString();

			// we only want the class files
			if (fileName.endsWith(".class")) {

				String className = fileName.replace('/', '.').substring(0, fileName.length() - 6);
				int lastPeriod = className.lastIndexOf(".");
				
				String pckgName;
				if (lastPeriod > 0){
					pckgName = className.substring(0, lastPeriod);
				} else {
					pckgName = "";
				}

				if (!filter.isSearchPackage(pckgName)) {
					continue;
				}

				// get the class for our class name
				Class<?> theClass = null;
				try {
					theClass = Class.forName(className, false, classLoader);

					if (matcher.isMatch(theClass)) {
						matchList.add(theClass);
						registerHit(jarFileName, theClass);
					}

				} catch (ClassNotFoundException e) {
					// expected to get this hence finer
					logger.finer("Error searching classpath" + e.getMessage());
					continue;

				} catch (NoClassDefFoundError e) {
					// expected to get this hence finer
					logger.finer("Error searching classpath: " + e.getMessage());
					continue;
				}
			}
		}
	}

	private void recursivelyListDir(List<String> fileNameList, File dir, StringBuilder relativePath) {

		int prevLen;

		if (dir.isDirectory()) {
			File[] files = dir.listFiles();

			for (int i = 0; i < files.length; i++) {
				// store our original relative path string length
				prevLen = relativePath.length();
				relativePath.append(prevLen == 0 ? "" : "/").append(files[i].getName());

				recursivelyListDir(fileNameList, files[i], relativePath);

				// delete sub directory from our relative path
				relativePath.delete(prevLen, relativePath.length());
			}
		} else {
			// add class fileName to the list
			fileNameList.add(relativePath.toString());
		}
	}
}
