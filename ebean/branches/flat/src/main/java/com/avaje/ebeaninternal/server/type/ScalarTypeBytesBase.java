/**
 * Copyright (C) 2006  Robin Bygrave
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

import com.avaje.ebean.text.TextException;

/**
 * ScalarType for BLOB.
 */
public abstract class ScalarTypeBytesBase extends ScalarTypeBase<byte[]> {
	
	protected ScalarTypeBytesBase(boolean jdbcNative, int jdbcType) {
		super(byte[].class, jdbcNative, jdbcType);
	}

	public void bind(DataBind b, byte[] value) throws SQLException {
		if (value == null) {
			b.setNull(jdbcType);
		} else {
			b.setBytes(value);
		}
	}

	public Object toJdbcType(Object value) {
		return value;
	}

	public byte[] toBeanType(Object value) {
		return (byte[])value;
	}

	
	public String format(byte[] t) {
        throw new TextException("Not supported");
    }

    public byte[] parse(String value) {
		throw new TextException("Not supported");
	}
	
	public byte[] parseDateTime(long systemTimeMillis) {
		throw new TextException("Not supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}
	
	public abstract byte[] read(DataReader dataReader) throws SQLException;
}
