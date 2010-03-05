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

public class XoCompoundNode implements XoNode {

    private final XoNode[] nodes;

    private final String name;
    private final String beginTag;
    private final String endTag;
    
    public XoCompoundNode(XoNode... nodes) {
        this(null, nodes);
    }
    public XoCompoundNode(String name, XoNode... nodes) {
        this.name = name;
        this.nodes = nodes;
        this.beginTag = name == null ? "" : "<"+name+">";
        this.endTag = name == null ? "" : "</"+name+">";
    }
    
    public void writeNode(XmlWriterOutput o, Object bean) throws IOException {
        o.increaseDepth();
        o.write(beginTag);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].writeNode(o, bean);
        }
        o.decreaseDepth(true);
        o.write(endTag);
    }
    
    public void writeNode(XmlDocumentOutput out, Node node, Object bean) throws IOException {

        Element e = null;
        if (name != null){
            e = out.getDocument().createElement(name);
        }
        out.appendChild(node, e);
        
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].writeNode(out, e, bean);
        }

    }

    
}
