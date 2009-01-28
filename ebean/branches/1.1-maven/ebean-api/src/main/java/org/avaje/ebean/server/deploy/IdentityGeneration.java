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
package org.avaje.ebean.server.deploy;

/**
 * The types of Identity generation that can be defined.
 */
public final class IdentityGeneration {

	/**
	 * Try to determine the approach automatically depending on db features.
	 */
	public static final char AUTO = 'a';

	/**
	 * Use a Database Identity (autoincrement) to generate the identity.
	 */
	public static final char DB_IDENTITY = 'i';
	
	/**
	 * Use a Database sequence to generate the identity.
	 */
	public static final char DB_SEQUENCE = 's';
	
	/**
	 * Use a IdGenerator generate the identity.
	 */
	public static final char ID_GENERATOR = 'g';


	/**
	 * Parse the string returning the type of Identity generation.
	 */
	public static char parse(String value) {
		
		value = value.toLowerCase();
		if (value.indexOf("identity") != -1){
			return DB_IDENTITY;
		}
		if (value.indexOf("sequence") != -1){
			return DB_SEQUENCE;
		}
		if (value.indexOf("generator") != -1){
			return ID_GENERATOR;
		}
		if (value.indexOf("auto") != -1){
			return AUTO;
		}
		return AUTO;
	}


}
