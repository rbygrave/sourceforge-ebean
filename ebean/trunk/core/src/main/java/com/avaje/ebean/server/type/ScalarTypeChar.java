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
 * ScalarType for char.
 */
public class ScalarTypeChar extends ScalarTypeBase<Character> {
	
	public ScalarTypeChar() {
		super(char.class, false, Types.VARCHAR);
	}
	
	public void bind(DataBind b, Character value) throws SQLException {
		if (value == null){
			b.setNull(Types.VARCHAR);
		} else {
			String s = BasicTypeConverter.toString(value);
			b.setString(s);
		}
	}

	public Character read(DataReader dataReader) throws SQLException {
		String string = dataReader.getString();
		if (string == null || string.length()==0){
			return null;
		} else {
			return string.charAt(0);
		}
	}
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toString(value);
	}

	public Character toBeanType(Object value) {
		String s = BasicTypeConverter.toString(value);
		return s.charAt(0);
	}
	
	public Character parse(String value) {
		return value.charAt(0);
	}

	public Character parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}
}
