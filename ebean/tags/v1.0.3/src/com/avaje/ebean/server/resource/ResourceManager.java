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

import com.avaje.ebean.server.lib.resource.ResourceSource;

/**
 * Controls access to the required resources.
 */
public interface ResourceManager {

	/**
	 * Return the directory the database dictionary is serialized to.
	 * <p>
	 * This needs to be a directory with read/write permissions.
	 * </p>
	 */
	public File getDictionaryDirectory();

	/**
	 * Return the directory to put lucene indexes in.
	 * <p>
	 * This needs to be a directory with read/write permissions.
	 * </p>
	 */
	public File getIndexDirectory();

	/**
	 * Return external SQL.
	 */
	public String getSql(String fileName);
	
	/**
	 * Access to resources.
	 * <p>
	 * This provides access to deployment resources (xml, sql etc). This is
	 * provided via Url or File based access depending on the configuration.
	 * </p>
	 */
	public ResourceSource getResourceSource();

}
