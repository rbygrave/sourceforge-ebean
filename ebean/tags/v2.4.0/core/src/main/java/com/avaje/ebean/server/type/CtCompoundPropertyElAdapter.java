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
package com.avaje.ebean.server.type;

import com.avaje.ebean.server.deploy.BeanProperty;
import com.avaje.ebean.server.el.ElPropertyValue;
import com.avaje.ebean.text.StringParser;

/**
 * Adapter for CtCompoundProperty to ElPropertyValue.
 * <p>
 * This is used for non-scalar properties of a Compound Value Object. These only
 * occur in nested compound types.
 * </p>
 * 
 * @author rbygrave
s */
public class CtCompoundPropertyElAdapter implements ElPropertyValue {

    private final CtCompoundProperty prop;

    public CtCompoundPropertyElAdapter(CtCompoundProperty prop) {
        this.prop = prop;
    }

    public Object elConvertType(Object value) {
        return value;
    }

    public Object elGetReference(Object bean) {
        return bean;
    }

    public Object elGetValue(Object bean) {
        return prop.getValue(bean);
    }

    public void elSetReference(Object bean) {
        // prop.setValue(bean, value)
    }

    public void elSetValue(Object bean, Object value, boolean populate, boolean reference) {
        prop.setValue(bean, value);
    }

    public String getAssocOneIdExpr(String prefix, String operator) {
        throw new RuntimeException("Not Supported or Expected");
    }

    public Object[] getAssocOneIdValues(Object bean) {
        throw new RuntimeException("Not Supported or Expected");
    }

    public BeanProperty getBeanProperty() {
        return null;
    }

    public StringParser getStringParser() {
        return null;
    }

    public boolean isDbEncrypted() {
        return false;
    }

    public boolean isLocalEncrypted() {
        return false;
    }

    public boolean isAssocOneId() {
        return false;
    }

    public boolean isDateTimeCapable() {
        return false;
    }

    public Object parseDateTime(long systemTimeMillis) {
        throw new RuntimeException("Not Supported or Expected");
    }

    public boolean containsMany() {
        return false;
    }

    public String getDbColumn() {
        return null;
    }

    public String getElPlaceholder(boolean encrypted) {
        return null;
    }

    public String getElPrefix() {
        return null;
    }

    public String getName() {
        return prop.getPropertyName();
    }

}
