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

import com.avaje.ebean.server.el.ElPropertyValue;
import com.avaje.ebean.text.StringFormatter;

public class XoPropAttribute extends XoBaseProp implements XoAttribute {

    /**
     * Create as an property based attribute.
     */
    public XoPropAttribute(String name, ElPropertyValue prop, StringFormatter stringFormatter) {

        super(name, prop, stringFormatter);
    }

    public void writeAttribute(XmlDocumentOutput out, Element e, Object bean) throws IOException {

        Object v = getObjectValue(bean);
        String sv = getFormattedValue(v);
        Attr attr = out.getDocument().createAttribute(name);
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

}
