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

import java.util.ArrayList;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;

public class XomBuilder {
    
    //private final BeanDescriptor<?> descriptor;

    private final XomNamingConvention namingConvention;
        
    String rootNodeName;
    
    ArrayList<XbNode> nodes = new ArrayList<XbNode>();


    public XomBuilder(BeanDescriptor<?> descriptor, XomNamingConvention namingConvention){
        //this.descriptor = descriptor;
        this.namingConvention = namingConvention;
    }
    
    protected String getNamingConventionNodeName(String propertyName) {

        if (namingConvention != null){
            return namingConvention.getNodeName(propertyName);
        } else {
            return propertyName;
        }
    }
    
    public XbNode addElement(String propertyName){

        String nodeName = getNamingConventionNodeName(propertyName);
        return addElement(propertyName, nodeName);
    }
    
    public XbNode addElement(String propertyName, String nodeName){
        XbNode node = new XbNode(nodeName, propertyName, this);
        nodes.add(node);
        return node;
    }
}
