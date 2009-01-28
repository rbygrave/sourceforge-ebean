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
package com.avaje.lib.log;

import java.util.logging.Logger;

/**
 * Utility object used throughout to create Loggers.
 */
public class LogFactory {

	/**
	 * Return the Logger for a given Class.
	 */
	public static Logger get(Class<?> cls){
		return Logger.getLogger(cls.getName());
	}

	/**
	 * Return a logger with an associated resource bundle.
	 */
	public static Logger getWithResource(Class<?> cls){
		
		final String pck = cls.getPackage().getName();
		final String bundle = pck+ "." + "messages.properties";
		
		return Logger.getLogger(cls.getName(), bundle);
	}
	
}
