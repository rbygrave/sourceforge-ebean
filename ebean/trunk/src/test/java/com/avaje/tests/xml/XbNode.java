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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XbNode extends XbBase {

    private Map<String,XbAttribute> attributes = new LinkedHashMap<String, XbAttribute>();

    private List<XbNode> childNodes = new ArrayList<XbNode>();
    
    public XbNode(String propertyName, String nodeName, XomBuilder builder) {
        super(propertyName, nodeName, builder);
    }

    public void addElement(String propertyName){
        String elementName = getNamingConventionNodeName(propertyName);
        addElement(propertyName, elementName);
    }
    
    public void addElement(String propertyName, String elementName){
        XbNode n = new XbNode(propertyName, elementName, builder);
        childNodes.add(n);
    }
    
    public XbNode addAttribute(String propertyName){
        String attrName = getNamingConventionNodeName(propertyName);
        return addAttribute(propertyName, attrName);
    }
    
    public XbNode addAttribute(String propertyName, String attrName){
        XbAttribute xbAttribute = attributes.get(attrName);
        if (xbAttribute != null){
            throw new RuntimeException("Attribute with this name ["+attrName+"]already exists");
        } 
        xbAttribute = new XbAttribute(attrName, propertyName, builder);
        return this;
    }
    
    public XoiNode createNode(BeanDescriptor<?> descriptor) {
    
        XoiAttribute[] attrs = new XoiAttribute[attributes.size()];
        
        int i = 0;
        for (XbAttribute attrBuilder : attributes.values()) {
            XoiAttribute xoAttr = attrBuilder.create(descriptor);
            attrs[i++] = xoAttr;
        }
        
        ElPropertyValue prop = descriptor.getElGetValue(propertyName);
        
        XoiNode[] children = null;
        if (!childNodes.isEmpty()){
            children = new XoiNode[childNodes.size()];
            for (int j = 0; j < children.length; j++) {
                XbNode xbNode = childNodes.get(j);
                children[j] = xbNode.createNode(descriptor);
            }
        }
        
        return new XopNode(nodeName, prop, formatter, parser, children, attrs);
    }
}
