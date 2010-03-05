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
package com.avaje.ebeaninternal.server.type;

import java.sql.SQLException;

/**
 * Encrypted ScalarType that wraps a byte[] types.
 * 
 * @author rbygrave
 *
 */
public class ScalarTypeBytesEncrypted implements ScalarType<byte[]> {

    private final ScalarTypeBytesBase baseType;
    
    private final DataEncryptSupport dataEncryptSupport;
    
    
    public ScalarTypeBytesEncrypted(ScalarTypeBytesBase baseType, DataEncryptSupport dataEncryptSupport) {
        this.baseType = baseType;
        this.dataEncryptSupport = dataEncryptSupport;
    }
    
    public void bind(DataBind b, byte[] value) throws SQLException {
        value = dataEncryptSupport.encrypt(value);
        baseType.bind(b, value);
    }

    public int getJdbcType() {
        return baseType.getJdbcType();
    }

    public int getLength() {
        return baseType.getLength();
    }

    public Class<byte[]> getType() {
        return byte[].class;
    }

    public boolean isDateTimeCapable() {
        return baseType.isDateTimeCapable();
    }

    public boolean isJdbcNative() {
        return baseType.isJdbcNative();
    }

    public void loadIgnore(DataReader dataReader) {
        baseType.loadIgnore(dataReader);
    }

    public String format(byte[] v) {
        throw new RuntimeException("Not used");
    }

    public byte[] parse(String value) {
        return baseType.parse(value);
    }

    public byte[] parseDateTime(long systemTimeMillis) {
        return baseType.parseDateTime(systemTimeMillis);
    }

    public byte[] read(DataReader dataReader) throws SQLException {
        
        byte[] data = baseType.read(dataReader);
        data  = dataEncryptSupport.decrypt(data);
        return data;
    }

    public byte[] toBeanType(Object value) {
        return baseType.toBeanType(value);
    }

    public Object toJdbcType(Object value) {
        return baseType.toJdbcType(value);
    }

    public void accumulateScalarTypes(String propName, CtCompoundTypeScalarList list) {
        baseType.accumulateScalarTypes(propName, list);
    }

    
}
