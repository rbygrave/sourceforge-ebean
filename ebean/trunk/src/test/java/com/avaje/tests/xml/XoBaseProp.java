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

import com.avaje.ebean.text.StringFormatter;
import com.avaje.ebeaninternal.server.el.ElPropertyValue;

public abstract class XoBaseProp {

    protected final ElPropertyValue prop;

    protected final StringFormatter stringFormatter;

    protected final String name;

    protected final boolean encode;

    protected boolean decreaseDepth = false;


    protected XoBaseProp(String name, ElPropertyValue prop, StringFormatter stringFormatter) {
        
        this.name = name;
        this.prop = prop;
        this.stringFormatter = stringFormatter;
        this.encode = false;//FIXME encode = true...

////        this.content = selfContent ? this : content;
//        this.selfClosing = (!selfContent && content == null);
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
