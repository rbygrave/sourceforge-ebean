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
package com.avaje.ebeaninternal.server.resource;

import java.io.File;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import com.avaje.ebean.config.GlobalProperties;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.server.lib.resource.DirectoryFinder;
import com.avaje.ebeaninternal.server.lib.resource.FileResourceSource;
import com.avaje.ebeaninternal.server.lib.resource.ResourceSource;
import com.avaje.ebeaninternal.server.lib.resource.UrlResourceSource;
import com.avaje.ebeaninternal.server.lib.util.NotFoundException;

/**
 * Creates a ResourceManager for a server depending on the avaje.properties.
 * <p>
 * This can use URL based resource loading for web applications or file based
 * otherwise.
 * </p>
 */
public class ResourceManagerFactory {

	private static final Logger logger = Logger.getLogger(ResourceManagerFactory.class.getName());

	/**
	 * Construct with the properties for a server.
	 */
	public ResourceManagerFactory() {
	}

	/**
	 * Create the resource manager given the properties for this server.
	 */
	public static ResourceManager createResourceManager(ServerConfig serverConfig) {

		ResourceSource resourceSource = createResourceSource(serverConfig);
		File autofetchDir = getAutofetchDir(serverConfig, resourceSource);

		return new ResourceManager(resourceSource, autofetchDir);
	}


	/**
	 * Return the directory that autofetch file goes into.
	 */
	protected static File getAutofetchDir(ServerConfig serverConfig, ResourceSource resourceSource) {

		String dir = null;
		if (serverConfig.getAutofetchConfig() != null) {
			dir = serverConfig.getAutofetchConfig().getLogDirectoryWithEval();
		}
		if (dir != null) {
			return new File(dir);
		}

		String realPath = resourceSource.getRealPath();
		if (realPath != null) {
			return new File(realPath);

		} else {
			throw new RuntimeException("No autofetch directory set?");
		}
	}

	/**
	 * Return the resource loader for external sql files.
	 * <p>
	 * This can be url based (for webapps) or otherwise file based.
	 * </p>
	 */
	protected static ResourceSource createResourceSource(ServerConfig serverConfig) {

		// default for web application, override this for file system
		String defaultDir = serverConfig.getResourceDirectory();
		
		
		// the default... check if a webapp first...
		ServletContext sc = GlobalProperties.getServletContext();
		if (sc != null) {
			// servlet container so use ServletContext.getResource()
			if (defaultDir == null) {
				defaultDir = "WEB-INF/ebean";
			}
			return new UrlResourceSource(sc, defaultDir);

		}
		// use File System directory
		return createFileSource(defaultDir);
	}

	private static ResourceSource createFileSource(String fileDir) {

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
