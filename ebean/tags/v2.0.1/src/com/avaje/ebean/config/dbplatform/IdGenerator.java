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
package com.avaje.ebean.config.dbplatform;


/**
 * Generates unique ids for objects. This occurs prior to the actual insert.
 * <p>
 * Note that many databases have sequences or auto increment features. These
 * can be used rather than an IdGenerator and are different in that they 
 * occur during an insert. IdGenerator is used to generate an id <em>BEFORE</em>
 * the actual insert.
 * </p>
 */
public interface IdGenerator {

    /**
     * The name of the default UUID generator.
     */
    public static final String AUTO_UUID = "auto.uuid";
    
    /**
     * return the next unique identity value.
     */
    public Object nextId();


}
