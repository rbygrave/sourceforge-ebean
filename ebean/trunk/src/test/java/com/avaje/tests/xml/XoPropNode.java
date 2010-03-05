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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XoPropNode extends XoBaseProp implements XoNode, XoContent {

    protected final XoAttribute[] attributes;
    protected final boolean hasAttributes;

    protected final String beginTag;
    protected final String beginTagEnd;
    protected final String endTag;
    
    /**
     * Create as a property based node.
     */
    public XoPropNode(String name, ElPropertyValue prop, StringFormatter stringFormatter,
            XoAttribute... attributes) {

        super(name, prop, stringFormatter);
        
        this.attributes = attributes;
        this.hasAttributes = (attributes != null && attributes.length > 0);

        boolean selfClosing = false;
        
        this.beginTagEnd = selfClosing ? "/>" : ">";
        this.beginTag = "<" + name;
        this.endTag = selfClosing ? "" : "</" + name + ">";
    }

    public void writeNode(XmlDocumentOutput out, Node node, Object bean) throws IOException {

        Object val = getObjectValue(bean);
        if (val == null && !hasAttributes){
            return;
        }
        
        Element newChild = out.getDocument().createElement(name);

        out.appendChild(node, newChild);

        if (hasAttributes) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].writeAttribute(out, newChild, bean);
            } 
        } 
        
        writeContent(out, newChild, bean, val);
        
    }
    
    public void writeNode(XmlWriterOutput o, Object bean) throws IOException {
        
        Object val = getObjectValue(bean);
        if (val == null && !hasAttributes){
            return;
        }
        
        o.increaseDepth();
        o.write(beginTag);
        if (hasAttributes) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].writeAttribute(o, bean);
            }
            o.write(beginTagEnd);
        } else {
            o.write(beginTagEnd);            
        }
        
        writeContent(o, bean, val);
        
        o.decreaseDepth(decreaseDepth);
        o.write(endTag);
    }

    public void writeContent(XmlDocumentOutput out, Element e, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        //TODO encode string
        e.setTextContent(sv);

    }
    public void writeContent(XmlWriterOutput o, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        o.writeEncoded(sv);
    }

}
