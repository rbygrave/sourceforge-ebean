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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XopNode extends XopBase implements XoiNode {

    protected final XoiAttribute[] attributes;
    protected final boolean hasAttributes;

    protected final String beginTag;
    protected final String beginTagEnd;
    protected final String endTag;
    
    protected final XoiNode[] childNodes;
    protected final boolean hasChildNodes;

    public XopNode(String nodeName, XoiNode... nodes) {
        this(nodeName, null, null, null, nodes, null);
    }
    
    public XopNode(String nodeName, ElPropertyValue prop) {
        this(nodeName, prop, null, null, null, null);
    }
    
    /**
     * Create as a property based node.
     */
    public XopNode(String nodeName, ElPropertyValue prop, StringFormatter formatter,
            StringParser parser, XoiNode[] childNodes, XoiAttribute[] attributes) {

        super(nodeName, prop, formatter, parser);
        
        this.attributes = attributes;
        this.hasAttributes = (attributes != null && attributes.length > 0);

        this.childNodes = childNodes;
        this.hasChildNodes = (childNodes!= null && childNodes.length > 0);

        boolean selfClosing = false;
        
        this.beginTagEnd = selfClosing ? "/>" : ">";
        this.beginTag = "<" + nodeName;
        this.endTag = selfClosing ? "" : "</" + nodeName + ">";
    }
    
    public String toString() {
        return nodeName;
    }
    
    public String getNodeName() {
        return nodeName;
    }

    public void readNode(Node node, XoWriteContext ctx) {

        if (prop != null){
            String c = node.getTextContent();
            Object v = getParsedValue(c);
            setObjectValue(ctx.getBean(), v);
        }
        
        if (hasAttributes){
            NamedNodeMap attrs = node.getAttributes();
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].readNode(node, attrs, ctx);
            } 
        }
        
        if (hasChildNodes){
            readChildNodes(node, ctx);
        }
        
    }

    private void readChildNodes(Node node, XoWriteContext ctx) {

        Node childNode = node.getFirstChild();
        int pos = -1;

        do {
            for (; ++pos < childNodes.length;) {
                if (childNode.getNodeName().equals(childNodes[pos].getNodeName())) {
                    childNodes[pos].readNode(childNode, ctx);
                    break;
                } else {
                    System.out.println("no element for " + childNodes[pos].getNodeName());
                }
            }

            childNode = childNode.getNextSibling();

        } while (childNode != null);

    }


    public void writeNode(XmlOutputDocument out, Node node, Object bean) throws IOException {

        
        Node childNode;
        if (nodeName == null){
            childNode = node;
            
        } else {
            Element element = out.getDocument().createElement(nodeName);
            childNode = element;
            
            out.appendChild(node, element);
            if (hasAttributes) {
                for (int i = 0; i < attributes.length; i++) {
                    attributes[i].writeAttribute(out, element, bean);
                } 
            } 
            if (prop != null){
                Object val = getObjectValue(bean);
                if (val != null) {
                    writeContent(out, childNode, bean, val);
                }
            }        
        } 
        
        if (hasChildNodes) {
            for (int i = 0; i < childNodes.length; i++) {
                childNodes[i].writeNode(out, childNode, bean);
            }
        }
        
    }
    
    public void writeNode(XmlOutputWriter o, Object bean) throws IOException {
        
        Object val = null;
        if (prop != null){
            val = getObjectValue(bean);
            if (val == null && !hasAttributes && !hasChildNodes){
                return;
            }
        }
        
        o.increaseDepth();
        o.write(beginTag);
        if (hasAttributes) {
            for (int i = 0; i < attributes.length; i++) {
                attributes[i].writeAttribute(o, bean);
            }
        }
//            o.write(beginTagEnd);
//        } else {
//            o.write(beginTagEnd);            
//        }
        o.write(beginTagEnd);            
        
        if (val != null){
            writeContent(o, bean, val);
        }
        
        if (hasChildNodes){
            for (int i = 0; i < childNodes.length; i++) {
                childNodes[i].writeNode(o, bean);
            }
            o.decreaseDepth(true);
        } else {
            o.decreaseDepth(decreaseDepth);
        }
        
        o.write(endTag);
    }

    public void writeContent(XmlOutputDocument out, Node e, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        //TODO encode string
        e.setTextContent(sv);

    }
    
    public void writeContent(XmlOutputWriter o, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        o.writeEncoded(sv);
    }

}
