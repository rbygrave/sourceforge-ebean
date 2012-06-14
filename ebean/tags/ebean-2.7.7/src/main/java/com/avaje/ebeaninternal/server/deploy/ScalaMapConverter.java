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
package com.avaje.ebeaninternal.server.deploy;

import scala.collection.JavaConversions;

/**
 * Converts between Java Map and Scala mutable Map.
 *
 * @author rbygrave
 */
public class ScalaMapConverter implements CollectionTypeConverter {

    @SuppressWarnings({ "rawtypes" })
    public Object toUnderlying(Object wrapped) {
        
        if (wrapped instanceof JavaConversions.JMapWrapper){
            return ((JavaConversions.JMapWrapper)wrapped).underlying();
        }
        return null;
    }
    
    public Object toWrapped(Object wrapped) {
        if (wrapped instanceof java.util.Map<?,?>){
            return  JavaConversions.mapAsScalaMap((java.util.Map<?,?>)wrapped);
        }
        return wrapped;
    }
    
}
