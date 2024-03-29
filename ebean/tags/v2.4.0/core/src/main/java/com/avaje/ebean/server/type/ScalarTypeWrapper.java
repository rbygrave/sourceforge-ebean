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

import java.sql.SQLException;

import com.avaje.ebean.config.ScalarTypeConverter;

/**
 * A ScalarType that uses a ScalarTypeConverter to convert to and from another
 * underlying ScalarType.
 * <p>
 * Enables the use of a simple interface to add additional scalarTypes.
 * </p>
 * 
 * @author rbygrave
 * 
 * @param <B>
 *            the logical type
 * @param <S>
 *            the underlying scalar type this is converted to
 */
public class ScalarTypeWrapper<B, S> implements ScalarType<B> {

    private final ScalarType<S> scalarType;
    private final ScalarTypeConverter<B, S> converter;
    private final Class<B> wrapperType;

    public ScalarTypeWrapper(Class<B> wrapperType, ScalarType<S> scalarType, ScalarTypeConverter<B, S> converter) {
        this.scalarType = scalarType;
        this.converter = converter;
        this.wrapperType = wrapperType;
    }

    public String toString() {
        return "ScalarTypeWrapper " + wrapperType + " to " + scalarType.getType();
    }

    public void bind(DataBind b, B value) throws SQLException {
        if (value == null) {
            scalarType.bind(b, null);
        } else {
            S sv = converter.unwrapValue(value);
            scalarType.bind(b, sv);
        }
    }

    public int getJdbcType() {
        return scalarType.getJdbcType();
    }

    public int getLength() {
        return scalarType.getLength();
    }

    public Class<B> getType() {
        return wrapperType;
    }

    public boolean isDateTimeCapable() {
        return scalarType.isDateTimeCapable();
    }

    public boolean isJdbcNative() {
        return false;
    }

    public String format(B v) {
        S sv = converter.unwrapValue(v);
        return scalarType.format(sv);
    }

    public B parse(String value) {
        S sv = scalarType.parse(value);
        if (sv == null) {
            return null;
        }
        return converter.wrapValue(sv);
    }

    public B parseDateTime(long systemTimeMillis) {
        S sv = scalarType.parseDateTime(systemTimeMillis);
        if (sv == null) {
            return null;
        }
        return converter.wrapValue(sv);
    }

    public void loadIgnore(DataReader dataReader) {
        dataReader.incrementPos(1);
    }

    public B read(DataReader dataReader) throws SQLException {

        S sv = scalarType.read(dataReader);
        if (sv == null) {
            return null;
        }
        return converter.wrapValue(sv);
    }

    @SuppressWarnings("unchecked")
    public B toBeanType(Object value) {
        if (value == null) {
            return null;
        }
        if (getType().isAssignableFrom(value.getClass())) {
            return (B) value;
        }
        if (value instanceof String) {
            return parse((String) value);
        }
        S sv = scalarType.toBeanType(value);
        return converter.wrapValue(sv);
    }

    @SuppressWarnings("unchecked")
    public Object toJdbcType(Object value) {

        Object sv = converter.unwrapValue((B) value);
        if (sv == null) {
            return null;
        }
        return scalarType.toJdbcType(sv);
    }

    public void accumulateScalarTypes(String propName, CtCompoundTypeScalarList list) {
        list.addScalarType(propName, this);
    }

    public ScalarType<?> getScalarType() {
        return this;
    }

}
