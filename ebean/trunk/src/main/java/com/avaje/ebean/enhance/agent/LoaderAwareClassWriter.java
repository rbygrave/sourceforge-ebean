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

import java.util.logging.Level;
import java.util.logging.Logger;

import com.avaje.ebean.enhance.asm.ClassWriter;

/**
 * Extension of ClassWriter to enable find Classes for getCommonSuperClass().
 * 
 * @author rbygrave
 */
public class LoaderAwareClassWriter extends ClassWriter {

    private static final Logger logger = Logger.getLogger(LoaderAwareClassWriter.class.getName());
    
    private final ClassLoader classLoader;
    
    /**
     * Construct with flags and a ClassLoader to use for classForName().
     */
    public LoaderAwareClassWriter(int flags, ClassLoader classLoader) {
        super(flags);
        this.classLoader = classLoader;
    }
    
    @Override
    protected Class<?> classForName(String name) throws ClassNotFoundException {
        
        ClassNotFoundException notFound = null;
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            notFound = e;
            String m = "Error looking for classes for getCommonSuperClass() with "+classLoader.getClass().getName();
            logger.log(Level.FINE, m, e);
        }
        
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (contextClassLoader != null && (contextClassLoader != classLoader)) {
                return Class.forName(name, false, contextClassLoader);
            }
        } catch (ClassNotFoundException e) {
            // still not found
            String m = "2nd Error looking for classes for getCommonSuperClass() with Context ClassLoader "+contextClassLoader.getClass().getName();
            logger.log(Level.FINE, m, e);
        }
        
        ClassLoader localClassLoader = getClass().getClassLoader();
        try {
            if (localClassLoader != classLoader){
                return Class.forName(name, false, localClassLoader);
            }
        } catch (ClassNotFoundException e) {
            // still not found
            String m = "3nd Error looking for classes for getCommonSuperClass() with Local ClassLoader "+localClassLoader.getClass().getName();
            logger.log(Level.FINE, m, e);
        }

        String m = "Error looking for classes for getCommonSuperClass() with "+classLoader.getClass().getName();
        logger.log(Level.WARNING, m, notFound);

        throw notFound;
    }
}
