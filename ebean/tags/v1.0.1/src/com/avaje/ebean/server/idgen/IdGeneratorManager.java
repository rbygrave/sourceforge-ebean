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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.PersistenceException;

import com.avaje.ebean.server.core.InternalEbeanServer;
import com.avaje.ebean.server.core.IdGenerator;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.plugin.PluginProperties;

/**
 * Manages all the IdGenerators for a given EbeanServer.
 */
public class IdGeneratorManager {

    /**
     * The name of the default UUID generator.
     */
    public static final String AUTO_UUID = "auto.uuid";

    
    /**
     * A cache of the named IdGenerators.
     */
	private final Map<String, IdGenerator> genCache;
    
    /**
     * The properties.
     */
    private final PluginProperties properties;
    
    /**
     * The default generator (for non-UUID types).
     */
    private final IdGenerator defaultGen;

    /**
     * The default generator for UUID types.
     */
    private final IdGenerator defaultUUIDGenerator;

    private final InternalEbeanServer server;
        
    
    /**
     * Create the IdGeneratorManager.
     */
    public IdGeneratorManager(PluginProperties properties, InternalEbeanServer server){  
    	
    	this.properties = properties;
    	this.server = server;
    	
    	// the default IdGenerator for UUID types
    	this.defaultUUIDGenerator = new UtilUUIDGen();
    	
    	genCache = new ConcurrentHashMap<String, IdGenerator>();
        
        
        String key = "idgen.default.class";
        String cn = properties.getProperty(key,null);
        if (cn != null){
            defaultGen = createGenerator("default");
            
        } else {
        	// no harm in setting up a default IdGenerator
        	// even if it is not used
        	defaultGen = new DbSequence();
        	//defaultGen.configure("default", properties);
        }
        defaultGen.configure("default",server);
    }
    
    public Object nextId(BeanDescriptor desc) {
        
        String name = desc.getIdGeneratorName();
        return getGenerator(name).nextId(desc);
    }
    
    /**
     * Return the IdGenerator for a given name.
     * <p>
     * In the system.properties file this uses the 
     * name to determine the actual implementation.
     * </p>
     */
    public IdGenerator getGenerator(String name){
        
    	if (AUTO_UUID.equalsIgnoreCase(name)){
    		// use the default UUID generator
    		return defaultUUIDGenerator;
    	}
    	
        if (name == null){
            return defaultGen;
        }
        name = name.toLowerCase();
        
        IdGenerator gen = (IdGenerator)genCache.get(name);
        if (gen == null){
            synchronized (this) {
                gen = (IdGenerator)genCache.get(name);
                if (gen != null){
                    return gen;
                }
                gen = createGenerator(name);
                genCache.put(name, gen);
            }
        }
        
        return gen;
    }

    public IdGenerator createGenerator(String name){
        
        String key = "idgen."+name+".class";
        String cn = properties.getProperty(key,null);
        if (cn == null){
            throw new NullPointerException("No Idgenerator cn for "+key);
        }
        try {
            Class<?> clz = Class.forName(cn);
            IdGenerator gen = (IdGenerator)clz.newInstance();
            gen.configure(name, server);
            
            return gen;
            
        } catch (Exception ex){
            throw new PersistenceException(ex);
        }
    }
}
