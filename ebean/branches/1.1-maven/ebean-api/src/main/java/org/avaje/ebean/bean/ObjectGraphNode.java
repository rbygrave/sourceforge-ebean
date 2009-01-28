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
package org.avaje.ebean.bean;

import java.io.Serializable;

/**
 * Identifies a unique node of an object graph. 
 * <p>
 * It represents a location relative to the root of an object graph and specific to a
 * query and call stack hash.
 * </p>
 */
public final class ObjectGraphNode implements Serializable {

	private static final long serialVersionUID = 2087081778650228995L;

	/**
	 * Identifies the origin.
	 */
	final ObjectGraphOrigin originQueryPoint;
	
	/**
	 * The relative bean index.
	 */
	final String beanIndex;
	
	/**
	 * The path relative to the root.
	 */
	final String path;
	
	/**
	 * Create at a sub level.
	 */
	public ObjectGraphNode(ObjectGraphNode parent, String beanIndex, String path) {
		this.originQueryPoint = parent.getOriginQueryPoint();
		this.beanIndex = parent.getBeanIndex()+"."+beanIndex;
		this.path = parent.getChildPath(path);
	}
	
	/**
	 * Create an the root level.
	 */
	public ObjectGraphNode(ObjectGraphOrigin originQueryPoint, String beanIndex, String path) {
		this.originQueryPoint = originQueryPoint;
		this.beanIndex = beanIndex;
		this.path = path;
	}
	
	/**
	 * Return the origin query point.
	 */
	public ObjectGraphOrigin getOriginQueryPoint() {
		return originQueryPoint;
	}

	/**
	 * Return the bean index.
	 */
	public String getBeanIndex() {
		return beanIndex;
	}

	private String getChildPath(String childPath) {
		if (path == null){
			return childPath;
		} else if (childPath == null) {
			return path;
		} else {
			return path+"."+childPath;
		}
	}

	/**
	 * Return the path relative to the root.
	 */
	public String getPath() {
		return path;
	}

	public String toString() {
		return "originQueryPoint:"+originQueryPoint+" "+":"+path+":"+beanIndex;
	}
}
