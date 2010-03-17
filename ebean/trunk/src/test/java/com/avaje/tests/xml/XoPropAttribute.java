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

import java.io.IOException;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XoPropAttribute extends XoBaseProp implements XoAttribute {

    /**
     * Create as an property based attribute.
     */
    public XoPropAttribute(String attrName, ElPropertyValue prop, StringFormatter formatter, StringParser parser) {
        super(attrName, prop, formatter, parser);
    }

    public XoPropAttribute(String attrName, ElPropertyValue prop) {
        this(attrName, prop, null, null);
    }
    
    public void writeAttribute(XmlOutputDocument out, Element e, Object bean) throws IOException {

        Object v = getObjectValue(bean);
        String sv = getFormattedValue(v);
        Attr attr = out.getDocument().createAttribute(nodeName);
        attr.setValue(sv);

        e.setAttributeNode(attr);
    }

    public void writeAttribute(XmlOutputWriter o, Object bean) throws IOException {

        Object v = getObjectValue(bean);
        String sv = getFormattedValue(v);

        o.write(" ");
        o.write(nodeName);
        o.write("=\"");
        o.write(sv);
        o.write("\"");
    }

    public void readNode(Node node, NamedNodeMap attributes, XoWriteContext ctx) {
        
        Node namedItem = attributes.getNamedItem(nodeName);
        if (namedItem != null){
            String c = namedItem.getTextContent();
            Object v = getParsedValue(c);
            setObjectValue(ctx.getBean(), v);
        }
    }

    
}
