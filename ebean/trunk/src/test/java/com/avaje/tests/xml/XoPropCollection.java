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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import javax.persistence.PersistenceException;

import org.w3c.dom.Node;

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebean.bean.BeanCollectionAdd;
import com.avaje.ebeaninternal.server.deploy.BeanDescriptor;
import com.avaje.ebeaninternal.server.deploy.BeanPropertyAssocMany;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XoPropCollection extends XoPropNode implements XoNode {

    private final XoNode childNode;
    
    private final boolean invokeFetch;
    
    private final BeanPropertyAssocMany<?> manyProp;

    private final String mapKey;

    private final BeanDescriptor<?> targetDescriptor;
    
    /**
     * Construct with no mapKey and no attributes.
     */
    public XoPropCollection(String name, ElPropertyValue prop, XoNode childNode, boolean invokeFetch) {
        this(name, prop, childNode, invokeFetch, null, null);
    }

    /**
     * Construct with no attributes.
     */
    public XoPropCollection(String name, ElPropertyValue prop, XoNode childNode, boolean invokeFetch, String mapKey) {
        this(name, prop, childNode, invokeFetch, mapKey, null);
    }
    
    /**
     * Construct with all options.
     */
    public XoPropCollection(String name, ElPropertyValue prop, XoNode childNode, boolean invokeFetch, String mapKey, XoAttribute[] attributes) {

        super(name, prop, null, null, null, attributes);
        this.childNode = childNode;
        this.invokeFetch = invokeFetch;
        this.decreaseDepth = true;
        this.mapKey = mapKey;
        this.manyProp = (BeanPropertyAssocMany<?>)prop.getBeanProperty();
        this.targetDescriptor = manyProp.getTargetDescriptor();
    }

    
    @Override
    public void readNode(Node node, XoWriteContext ctx) {
        
        Object parentBean = ctx.getBean();
        
        // create a List/Set/Map to hold the details
        Object details = manyProp.createEmpty(ctx.isVanillaMode());
        
        // Wrapper used to add to the collection
        BeanCollectionAdd bcAdd = manyProp.getBeanCollectionAdd(details, mapKey);
        
        Node detailNode = nextElement(node.getFirstChild());
        do {
            Object detailBean = targetDescriptor.createBean(ctx.isVanillaMode());
            ctx.setBean(detailBean);
            
            childNode.readNode(detailNode, ctx);
            
            bcAdd.addBean(detailBean);
            
            detailNode = nextElement(detailNode.getNextSibling());
            
        } while(detailNode != null);
        
        setObjectValue(parentBean, details);
        ctx.setBean(parentBean);
    }

    private Node nextElement(Node node) {
        
        while (node != null) {
            int type = node.getNodeType();
            if (type == Node.ELEMENT_NODE){
                return node;
            }
            node = node.getNextSibling();   
        }
        return null;
    }
    
    
    @Override
    public void writeContent(XmlOutputWriter o, Object bean, Object val) throws IOException {
        
        Collection<?> collection = (Collection<?>)val;
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            Object childBean = it.next();
            childNode.writeNode(o, childBean);
        }
    }

    @Override
    public void writeContent(XmlOutputDocument out, Node e, Object bean, Object value) throws IOException {
        
        Collection<?> collection = (Collection<?>)value;
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            Object childBean = it.next();
            childNode.writeNode(out, e, childBean);
        }
    }

    @Override
    public void writeNode(XmlOutputDocument out, Node node, Object bean) throws IOException {
        super.writeNode(out, node, bean);
    }

    @Override
    protected Object getObjectValue(Object bean) {
        
        Object o = prop.elGetValue(bean);
        Collection<?> collection = getDetailsIterator(o);
        return collection;
    }


    /**
     * Return the details of the collection or map taking care to avoid
     * unnecessary fetching of the data.
     */
    private Collection<?> getDetailsIterator(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof BeanCollection<?>) {
            BeanCollection<?> bc = (BeanCollection<?>) o;
            if (!invokeFetch && !bc.isPopulated()) {
                return null;
            }
            bc.size();
            return bc.getActualDetails();
        }

        if (o instanceof Map<?,?>) {
            // yes, we want the entrySet (to set the keys)
            return ((Map<?, ?>) o).entrySet();

        } else if (o instanceof Collection<?>) {
            return ((Collection<?>) o);
        }
        String m = "expecting a Map or Collection but got [" + o.getClass().getName() + "]";
        throw new PersistenceException(m);
    }

}
