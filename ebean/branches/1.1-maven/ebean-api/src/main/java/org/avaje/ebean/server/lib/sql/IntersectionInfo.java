/**
 *  Copyright (C) 2006  Robin Bygrave
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */
package org.avaje.ebean.server.lib.sql;

import java.io.Serializable;

/**
 * Holds information to describe the intersection between two tables.
 */
public class IntersectionInfo implements Serializable{

	static final long serialVersionUID = -2382305945112700974L;
	
	TableInfo source;

	TableInfo dest;

	TableInfo intersection;

	Fkey sourceExportedKey;

	Fkey intersectionImportedKey;

	/**
	 * Create with a source and destination.
	 */
	public IntersectionInfo(TableInfo source, TableInfo dest) {
		this.source = source;
		this.dest = dest;
	}
	
	public String toString() {
		return "Source["+source+"] Dest["+dest+"] Intersection["+intersection+"]";
	}
	
	/**
	 * Return the destination tableInfo.
	 */
	public TableInfo getDest() {
		return dest;
	}

	/**
	 * Return the intersection tableInfo.
	 */
	public TableInfo getIntersection() {
		return intersection;
	}

	/**
	 * Set the intersection tableInfo.
	 */
	public void setIntersection(TableInfo intersection) {
		this.intersection = intersection;
	}

	/**
	 * Return the source tableInfo.
	 */
	public TableInfo getSource() {
		return source;
	}

	/**
	 * return the Fkey from intersection to destination.
	 */
	public Fkey getIntersectionImportedKey() {
		return intersectionImportedKey;
	}

	/**
	 * Set the Fkey from intersection to destination.
	 */
	public void setIntersectionImportedKey(Fkey intersectionImportedKey) {
		this.intersectionImportedKey = intersectionImportedKey;
	}

	/**
	 * Return the Fkey from source to intersection.
	 */
	public Fkey getSourceExportedKey() {
		return sourceExportedKey;
	}

	/**
	 * Set the Fkey from source to intersection.
	 */
	public void setSourceExportedKey(Fkey sourceExportedKey) {
		this.sourceExportedKey = sourceExportedKey;
	}

}
