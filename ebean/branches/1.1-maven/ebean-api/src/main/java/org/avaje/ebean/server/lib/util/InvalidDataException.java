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
package org.avaje.ebean.server.lib.util;


/**
 * A general exception for invalid data.
 */
public class InvalidDataException extends RuntimeException
{
    static final long serialVersionUID = 7061559938704539846L;
    
	public InvalidDataException(Exception cause) {
		super(cause);
	}
	
    public InvalidDataException(String s, Exception cause) {
		super(s, cause);
	}

	public InvalidDataException(String s) {
		super(s);
	}

}
