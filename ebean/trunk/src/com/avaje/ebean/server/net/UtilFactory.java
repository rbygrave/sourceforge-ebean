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
package com.avaje.ebean.server.net;

import javax.persistence.PersistenceException;

import com.avaje.ebean.config.ConfigProperties;

/**
 * Creates various implementation objects for CommandProcessor.
 */
public class UtilFactory {

	ConfigProperties configProperties;
	
	UtilFactory(ConfigProperties configProperties){
		this.configProperties = configProperties;
	}
	
    /**
     * Create the Authenticate.
     */
    public Authenticate createAuthenticate(String dftl) {
        
        String cn = configProperties.getProperty("ebean.server.authenticate", dftl);
        if (cn == null){
            return null;
        }
        try {
            Class<?> cls = Class.forName(cn);
            Authenticate auth = (Authenticate)cls.newInstance();
            
            return auth;
            
        } catch (Exception ex){
            throw new PersistenceException(ex);
        }
    }
    
    /**
     * Create a CommandSecurity.
     */
    public CommandSecurity createCommandSecurity(String dftl) {
        
        String cn = configProperties.getProperty("ebean.server.commandsecurity", dftl);
        if (cn == null){
            return null;
        }
        
        try {
            Class<?> cls = Class.forName(cn);
            return (CommandSecurity)cls.newInstance();            
            
        } catch (Exception ex){
            throw new PersistenceException(ex);
        }
    }
    
    /**
     * Create a CommandContextManager.
     */
    public CommandContextManager createCommandContextManager(String dftl) {
               
        String cn = configProperties.getProperty("ebean.server.commandcontextmanager", dftl);
        if (cn == null){
            return null;
        }
        try {
            Class<?> cls = Class.forName(cn);
            CommandContextManager cm = (CommandContextManager)cls.newInstance();
            
            return cm;
            
        } catch (Exception ex){
            throw new PersistenceException(ex);
        }
    }
}
