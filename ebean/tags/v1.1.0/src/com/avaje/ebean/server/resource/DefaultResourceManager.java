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
import java.io.IOException;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.lib.resource.ResourceContent;
import com.avaje.ebean.server.lib.resource.ResourceSource;

/**
 * The default ResourceManager implementation.
 */
public class DefaultResourceManager implements ResourceManager {

	final ResourceSource resourceSource;
	
	final File dictionaryDir;
	
	final File autofetchDir;
	
	final File indexDirectory;
	
	public DefaultResourceManager(ResourceSource resourceSource, File dictionaryDir, File autofetchDir, File indexDirectory) {
		this.resourceSource = resourceSource;
		this.dictionaryDir = dictionaryDir;
		this.autofetchDir = autofetchDir;
		this.indexDirectory = indexDirectory;
	}
	
	public ResourceSource getResourceSource() {
		return resourceSource;
	}

	public File getDictionaryDirectory() {
		return dictionaryDir;
	}
	
	public File getAutofetchDirectory() {
		return autofetchDir;
	}

	public File getIndexDirectory() {
		return indexDirectory;
	}

	public String getSql(String fileName) {
		try {
			ResourceContent content = resourceSource.getContent("ebean/"+fileName+".sql");
			if (content != null){
				return resourceSource.readString(content, 1024);
			}
			return null;
			
		} catch (IOException ex){
			throw new PersistenceException(ex);
		}
	}


}
