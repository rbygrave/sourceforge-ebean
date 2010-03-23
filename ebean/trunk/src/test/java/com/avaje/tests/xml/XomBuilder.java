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
package com.avaje.tests.xml;

import java.util.HashMap;
import java.util.Map;

import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

public class XomBuilder {
    
    private final SpiEbeanServer ebeanServer;

    private final XomNamingConvention namingConvention;
        
    String rootNodeName;
    
    Map<String,XbNode> rootNodes = new HashMap<String,XbNode>();


    public XomBuilder(SpiEbeanServer ebeanServer, XomNamingConvention namingConvention){
        this.ebeanServer = ebeanServer;
        this.namingConvention = namingConvention;
    }
    
    protected String getNamingConventionNodeName(String propertyName) {

        if (namingConvention != null){
            return namingConvention.getNodeName(propertyName);
        } else {
            return propertyName;
        }
    }
    
    public XbNode addRootElement(String rootElementName, Class<?> rootType) {
        
        BeanDescriptor<?> descriptor = ebeanServer.getBeanDescriptor(rootType);
        XbNode rootNode = new XbNode(rootElementName, descriptor);
        rootNodes.put(rootElementName, rootNode);
        
        return rootNode;
    }
    
}
