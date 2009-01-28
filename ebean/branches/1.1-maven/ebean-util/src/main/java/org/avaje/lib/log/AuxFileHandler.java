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
package org.avaje.lib.log;

/**
 * An Auxilary file handler.
 * <p>
 * You can use this if you want a second File Handler (AvajeFileHanlder being
 * the other one).
 * </p>
 * <p>
 * By default the pattern is <b>aux%d.log</b>
 * </p>
 */
public class AuxFileHandler extends FileHandler {

	public AuxFileHandler() {
		super("aux%d.log");
	}
}
