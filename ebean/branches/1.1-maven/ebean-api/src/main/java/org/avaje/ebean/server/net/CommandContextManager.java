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
package org.avaje.ebean.server.net;

import org.avaje.ebean.net.CommandContext;

/**
 * Create a Context to execute the command.
 */
public interface CommandContextManager {

	/**
	 * Create the appropriate context.
	 * <p>
	 * This will have the appropriate EbeanServer and may have session
	 * support for user transactions.
	 * </p>
	 */
    public CommandContext getContext(Headers headers);
}
