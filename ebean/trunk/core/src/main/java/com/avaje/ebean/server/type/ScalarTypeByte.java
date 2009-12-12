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
package com.avaje.ebean.server.type;

import java.sql.SQLException;
import java.sql.Types;

import com.avaje.ebean.server.core.BasicTypeConverter;
import com.avaje.ebean.text.TextException;

/**
 * ScalarType for Byte.
 */
public class ScalarTypeByte extends ScalarTypeBase<Byte> {

	public ScalarTypeByte() {
		super(Byte.class, true,Types.TINYINT );
	}
	
	public void bind(DataBind b, Byte value) throws SQLException {
		if (value == null){
			b.setNull(Types.TINYINT);
		} else {
			b.setByte(value);
		}
	}

	public Byte read(DataReader dataReader) throws SQLException {
		return dataReader.getByte();
	}

	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toByte(value);
	}

	public Byte toBeanType(Object value) {
		return BasicTypeConverter.toByte(value);
	}

	public Byte parse(String value) {
		throw new TextException("Not supported");
	}
	
	public Byte parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}
	
	public boolean isDateTimeCapable() {
		return false;
	}
}
