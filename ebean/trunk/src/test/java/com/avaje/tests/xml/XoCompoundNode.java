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

import org.w3c.dom.Node;

public class XoCompoundNode implements XoNode {

    private final XoNode[] nodes;

    private final String nodeName;
    private final String beginTag;
    private final String endTag;
    
    public XoCompoundNode(XoNode... nodes) {
        this(null, nodes);
    }
    public XoCompoundNode(String nodeName, XoNode... nodes) {
        this.nodeName = nodeName;
        this.nodes = nodes;
        this.beginTag = nodeName == null ? "" : "<"+nodeName+">";
        this.endTag = nodeName == null ? "" : "</"+nodeName+">";
    }
        
    public String getNodeName() {
        return nodeName;
    }
    
    public void writeNode(XmlOutputWriter o, Object bean) throws IOException {
        o.increaseDepth();
        o.write(beginTag);
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].writeNode(o, bean);
        }
        o.decreaseDepth(true);
        o.write(endTag);
    }
    
    public void writeNode(XmlOutputDocument out, Node node, Object bean) throws IOException {

        Node e;
        if (nodeName == null){
            e = node;
        } else {
            e = out.getDocument().createElement(nodeName);
            out.appendChild(node, e);
        } 
        
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].writeNode(out, e, bean);
        }

    }
    
    public void readNode(Node node, XoWriteContext ctx) {
        
//        String processNodeName = node.getNodeName();
//        if (!processNodeName.equals(nodeName)){
//            throw new RuntimeException(processNodeName+" <> "+nodeName);
//        }
        Node childNode = node.getFirstChild();        
        int pos = -1;
        
        do {
            for (; ++pos < nodes.length;) {
                if (childNode.getNodeName().equals(nodes[pos].getNodeName())){
                    nodes[pos].readNode(childNode, ctx);
                    break;
                } else {
                    System.out.println("no element for "+nodes[pos].getNodeName());
                }
            }
            
            childNode = childNode.getNextSibling();
            
        } while(childNode != null);
        
    }

    
}
