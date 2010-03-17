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

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebean.text.StringParser;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public abstract class XoPropContent extends XoBaseProp implements XoContent {


    /**
     * Create as an property based attribute.
     */
    public XoPropContent(String name, ElPropertyValue prop, StringFormatter formatter, StringParser parser) {

        super(name, prop, formatter, parser);
    }

    public void writeContent(XmlOutputDocument out, Element e, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        //TODO encode string
        e.setTextContent(sv);

    }
    public void writeContent(XmlOutputWriter o, Object bean, Object value) throws IOException {

        String sv = getFormattedValue(value);
        o.writeEncoded(sv);
    }
}
