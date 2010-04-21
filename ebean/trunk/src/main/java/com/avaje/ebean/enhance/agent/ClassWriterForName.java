/**
 * Copyright (C) 2009 Authors
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
package com.avaje.ebean.enhance.agent;

/**
 * Provides ability to control the Class.forName(..) used in
 * LoaderAwareClassWriter.
 * <p>
 * Introduced to support other frameworks like Play that programmatically invoke
 * Ebean's enhancement process.
 * </p>
 * 
 * @author rbygrave
 * 
 */
public interface ClassWriterForName {

    /**
     * Return the Class used by the underlying ClassWriter in
     * getCommonSuperClass().
     * 
     * @param name
     *            the name of the class to find
     * @param classLoader
     *            the classLoader used by the LoaderAwareClassWriter
     */
    public Class<?> classForName(String name, ClassLoader classLoader) throws ClassNotFoundException;

}
