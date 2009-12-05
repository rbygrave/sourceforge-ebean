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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ScalarTypeWrapper implements ScalarType {

    final ScalarType scalarType;
    final WrapperConverter converter;
    final Class<?> wrapperType;
    
    ScalarTypeWrapper(Class<?> wrapperType, ScalarType scalarType, WrapperConverter converter){
        this.scalarType = scalarType;
        this.converter = converter;
        this.wrapperType = wrapperType;
    }
    
    public void bind(PreparedStatement pstmt, int index, Object value) throws SQLException {
        Object sv = converter.toScalarType(value);
        scalarType.bind(pstmt, index, sv);
    }

    public Object getDbNullValue(Object value) {
        Object sv = converter.toScalarType(value);
        return scalarType.getDbNullValue(sv);
    }

    public int getJdbcType() {
        return scalarType.getJdbcType();
    }

    public int getLength() {
        return scalarType.getLength();
    }

    public Class<?> getType() {
        return wrapperType;
    }

    public boolean isDateTimeCapable() {
        return scalarType.isDateTimeCapable();
    }

    public boolean isDbNull(Object value) {
        Object sv = converter.toScalarType(value);
        return scalarType.isDbNull(sv);
    }

    public boolean isJdbcNative() {
        return false;
    }

    public Object parse(String value) {
        Object sv  = scalarType.parse(value);
        return converter.toBeanType(sv);
    }

    public Object parseDateTime(long systemTimeMillis) {
        return scalarType.parseDateTime(systemTimeMillis);
    }

    public Object read(ResultSet rset, int index) throws SQLException {
        
        Object sv = scalarType.read(rset, index);
        return converter.toBeanType(sv);
    }

    public Object toBeanType(Object value) {
        
        Object sv = scalarType.toBeanType(value);
        return converter.toBeanType(sv);
    }

    public Object toJdbcType(Object value) {
        Object sv = converter.toScalarType(value);
        return scalarType.toJdbcType(sv);
    }

}
