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
package com.avaje.ebean.server.resource;

import java.io.File;
import java.util.logging.Logger;

import javax.persistence.PersistenceException;
import javax.servlet.ServletContext;

import com.avaje.ebean.server.lib.GlobalProperties;
import com.avaje.ebean.server.lib.resource.DirectoryFinder;
import com.avaje.ebean.server.lib.resource.FileResourceSource;
import com.avaje.ebean.server.lib.resource.ResourceSource;
import com.avaje.ebean.server.lib.resource.UrlResourceSource;
import com.avaje.ebean.server.lib.util.NotFoundException;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Creates a ResourceManager for a server depending on the avaje.properties.
 * <p>
 * This can use URL based resource loading for web applications or file based
 * otherwise.
 * </p>
 */
public class DefaultResourceManagerFactory implements ResourceManagerFactory {

	private static final Logger logger = Logger.getLogger(DefaultResourceManagerFactory.class.getName());

	final PluginProperties properties;

	/**
	 * Construct with the properties for a server.
	 */
	public DefaultResourceManagerFactory(PluginProperties properties) {
		this.properties = properties;
	}

	/**
	 * Create the resource manager given the properties for this server.
	 */
	public ResourceManager createResourceManager() {

		ResourceSource resourceSource = createResourceSource();
		File dictDir = getDictionaryDir(resourceSource);
		File autofetchDir = getAutofetchDir(resourceSource);
		File idxDir = getIndexDir(dictDir);

		return new DefaultResourceManager(resourceSource, dictDir, autofetchDir, idxDir);
	}

	/**
	 * Return the directory that lucene indexes go into.
	 */
	protected File getIndexDir(File dictDir) {

		String dir = properties.getProperty("index.directory", null);
		if (dir != null) {
			return new File(dir);
		}

		// default to a subdirectory from the dictionary dir
		return new File(dictDir, "index");
	}

	/**
	 * Return the directory that the dictionary serialized file goes into.
	 */
	protected File getDictionaryDir(ResourceSource resourceSource) {

		String dir = properties.getProperty("dictionary.directory", null);
		if (dir != null) {
			return new File(dir);
		}

		String realPath = resourceSource.getRealPath();
		if (realPath != null) {
			// same location as the sql files
			return new File(realPath);

		} else {
			String msg = "Can not determine a directory for the dictionary file.";
			msg += " Please specify a ebean.dictionary.directory property.";
			throw new PersistenceException(msg);
		}
	}

	/**
	 * Return the directory that autofetch serialized file goes into.
	 */
	protected File getAutofetchDir(ResourceSource resourceSource) {

		String dir = properties.getProperty("autofetch.directory", null);
		if (dir != null) {
			return new File(dir);
		}

		String realPath = resourceSource.getRealPath();
		if (realPath != null) {
			// same location as the sql files
			return new File(realPath);

		} else {
			// be compatible to previous behaviour
			return getDictionaryDir(resourceSource);
		}
	}

	/**
	 * Return the resource loader for external sql files.
	 * <p>
	 * This can be url based (for webapps) or otherwise file based.
	 * </p>
	 */
	protected ResourceSource createResourceSource() {

		String source = properties.getProperty("resource.source", "default");

		// default for web application, override this for file system
		String defaultDir = properties.getProperty("resource.directory", null);

		if (source.equalsIgnoreCase("file")) {
			return createFileSource(defaultDir);
		}

		// the default... check if a webapp first...
		ServletContext sc = GlobalProperties.getServletContext();
		if (sc != null) {
			// servlet container so use ServletContext.getResource()
			if (defaultDir == null) {
				defaultDir = "WEB-INF/ebean";
			}
			String basePath = properties.getProperty("resource.url.directory", defaultDir);
			return new UrlResourceSource(sc, basePath);

		}
		// use File System directory
		return createFileSource(defaultDir);
	}

	private ResourceSource createFileSource(String defaultDir) {

		String fileDir = properties.getProperty("resource.file.directory", defaultDir);
		if (fileDir != null) {
			// explicitly stated so
			File dir = new File(fileDir);
			if (dir.exists()) {
				logger.info("ResourceManager initialised: type[file] [" + fileDir + "]");
				return new FileResourceSource(fileDir);
			} else {
				String msg = "ResourceManager could not find directory [" + fileDir + "]";
				throw new NotFoundException(msg);
			}
		}

		// try to guess the directory starting from the current working
		// directory, and searching to a maximum depth of 3 subdirectories
		File guessDir = DirectoryFinder.find(null, "WEB-INF", 3);
		if (guessDir != null) {
			// Typically this means we found the WEB-INF directory below the
			// current working directory
			logger.info("ResourceManager initialised: type[file] [" + guessDir.getPath() + "]");
			return new FileResourceSource(guessDir.getPath());
		}

		// default to the current working directory
		File workingDir = new File(".");
		return new FileResourceSource(workingDir);
	}

}
