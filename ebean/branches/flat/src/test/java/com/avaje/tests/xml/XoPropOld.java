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
import org.w3c.dom.Node;

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XoPropOld implements XoAttribute, XoContent, XoNode {

    protected final ElPropertyValue prop;

    protected final StringFormatter stringFormatter;

    protected final XoContent content;
    protected final XoAttribute[] attributes;

    protected final String name;

    protected final boolean encode;
    protected final boolean hasAttributes;
    protected final boolean selfClosing;

    private final String beginTag;
    private final String beginTagEnd;
    private final String endTag;

    protected boolean decreaseDepth = false;
    /**
     * Create as a property based node.
     */
    public XoPropOld(String name, ElPropertyValue prop, StringFormatter stringFormatter,
            XoAttribute... attributes) {

        this(name, prop, stringFormatter, null, attributes, true);
    }

    /**
     * Create as an property based attribute.
     */
    public XoPropOld(String name, ElPropertyValue prop, StringFormatter stringFormatter) {

        this(name, prop, stringFormatter, null, null, true);
    }
    
    /**
     * Create as a container of content and attributes.
     */
    public XoPropOld(String name, XoContent content, XoAttribute[] attributes) {
        
        this(name, null, null, content, attributes, false);
    }


    private XoPropOld(String name, ElPropertyValue prop, StringFormatter stringFormatter, XoContent content,
            XoAttribute[] attributes, boolean selfContent) {
        
        this.name = name;
        this.prop = prop;
        this.stringFormatter = stringFormatter;
        this.encode = false;//FIXME encode = true...
        this.attributes = attributes;

        this.hasAttributes = (attributes != null && attributes.length > 0);

        this.content = selfContent ? this : content;
        this.selfClosing = (!selfContent && content == null);
        
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
        if (hasAttributes) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].writeAttribute(out, newChild, bean);
            } 
        } 
        
        if (content != null) {
            content.writeContent(out, newChild, bean, val);
        }
        
        node.appendChild(newChild);
        
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
        if (content != null) {
            content.writeContent(o, bean, val);
        }
        o.decreaseDepth(decreaseDepth);
        o.write(endTag);
    }

    
    public void writeAttribute(XmlDocumentOutput out, Element e, Object bean) throws IOException {

        Object v = getObjectValue(bean);
        String sv = getFormattedValue(v);
        Attr attr  = out.getDocument().createAttribute(name);
        attr.setValue(sv);
        
        e.setAttributeNode(attr);
    }
    
    public void writeAttribute(XmlWriterOutput o, Object bean) throws IOException {

        Object v = getObjectValue(bean);
        String sv = getFormattedValue(v);
        
        o.write(" ");
        o.write(name);
        o.write("=\"");
        o.write(sv);
        o.write("\"");
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

    protected Object getObjectValue(Object bean) {
        return prop.elGetValue(bean);
    }
    
    protected String getFormattedValue(Object value) {

        if (value == null) {
            return "";
        }
        String s;
        if (stringFormatter == null){
            s = value.toString();
        } else {
            s = stringFormatter.format(value);            
        }
        
        if (encode){
            //FIXME xml encode the string
        }
        
        return s;
    }
}
