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
package com.avaje.ebean.server.idgen;

import java.util.HashMap;
import java.util.Map;

import com.avaje.ebean.config.dbplatform.IdGenerator;

/**
 * Manages all the IdGenerators for a given EbeanServer.
 */
public class IdGeneratorMap {
    
    /**
     * A cache of the named IdGenerators.
     */
	private final Map<String, IdGenerator> map = new HashMap<String, IdGenerator>();
            
    
    /**
     * Create the IdGeneratorMap.
     */
    public IdGeneratorMap(){      	
    }
    
    public void put(String name, IdGenerator idGenerator) {
        
    	map.put(name, idGenerator);
    }

    public IdGenerator get(String name){
    	return map.get(name);
    }
    
}
