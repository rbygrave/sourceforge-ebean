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

import com.avaje.ebean.bean.BeanCollection;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public class XoPropCollection extends XoPropNode implements XoNode {

    private final XoNode childNode;
    
    private final boolean invokeFetch;

    public XoPropCollection(String name, ElPropertyValue prop, XoNode childNode, boolean invokeFetch) {
        this(name, prop, childNode, invokeFetch, null);
    }
    
    /**
     * Create as a property based node.
     */
    public XoPropCollection(String name, ElPropertyValue prop, XoNode childNode, boolean invokeFetch, XoAttribute[] attributes) {

        super(name, prop, null, attributes);
        this.childNode = childNode;
        this.invokeFetch = invokeFetch;
        this.decreaseDepth = true;
    }

    @Override
    public void writeContent(XmlWriterOutput o, Object bean, Object val) throws IOException {
        
        Collection<?> collection = (Collection<?>)val;
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            Object childBean = it.next();
            childNode.writeNode(o, childBean);
        }
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
