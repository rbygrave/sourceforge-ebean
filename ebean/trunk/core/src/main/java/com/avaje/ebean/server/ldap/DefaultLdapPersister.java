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
package com.avaje.ebean.server.ldap;

import java.util.Iterator;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import com.avaje.ebean.config.ldap.LdapContextFactory;
import com.avaje.ebean.server.deploy.BeanDescriptor;
import com.avaje.ebean.server.deploy.BeanProperty;

public class DefaultLdapPersister {

    private final LdapContextFactory contextFactory;
    
    public DefaultLdapPersister(LdapContextFactory dirContextFactory) {
        this.contextFactory = dirContextFactory;
    }
    
    public int persist(LdapPersistBeanRequest<?> request){
        
        switch (request.getType()) {
        case INSERT:
            return insert(request);
        case UPDATE:
            return update(request);
        case DELETE:
            return delete(request);

        default:
            throw new LdapPersistenceException("Invalid type "+request.getType());
        }
    }
    
    private int insert(LdapPersistBeanRequest<?> request) {
        
        DirContext dc = contextFactory.createContext();
        
        Name name = request.createLdapName();
        Attributes attrs  = createAttributes(request);
        
        System.out.println("Name:"+name);
        System.out.println("Attributes:"+attrs);
        
        try {
            dc.bind(name, null, attrs);            
            return 1;
            
        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }

    private int delete(LdapPersistBeanRequest<?> request) {

        DirContext dc = contextFactory.createContext();        
        Name name = request.createLdapName();
        try {
            dc.unbind(name);            
            return 1;
            
        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }
    
    private int update(LdapPersistBeanRequest<?> request) {
        
        
        DirContext dc = contextFactory.createContext();        
        Name name = request.createLdapName();
        Attributes attrs = createAttributes(request);
        try {
            dc.modifyAttributes(name, DirContext.REPLACE_ATTRIBUTE, attrs);
            return 1;
            
        } catch (NamingException e) {
            throw new LdapPersistenceException(e);
        }
    }
    
    private Attributes createAttributes(LdapPersistBeanRequest<?> request) {
        
        BeanDescriptor<?> desc = request.getBeanDescriptor();
        
        Attributes attrs  = desc.createAttributes();

        Object bean = request.getBean();
        
        Set<String> loadedProperties = request.getLoadedProperties();
        if (loadedProperties != null){
            for (String propName : loadedProperties) {
                BeanProperty p = desc.getBeanPropertyFromPath(propName);
                Attribute attr = p.createAttribute(bean);
                if (attr != null){
                    attrs.put(attr);
                }
            }
        } else {
            Iterator<BeanProperty> it = desc.propertiesAll();
            while (it.hasNext()) {
                BeanProperty p = it.next();
                //if (!p.isId()){
                    Attribute attr = p.createAttribute(bean);
                    if (attr != null){
                        attrs.put(attr);
                    }
                //}
            }
        }

        return attrs;
    }
}
