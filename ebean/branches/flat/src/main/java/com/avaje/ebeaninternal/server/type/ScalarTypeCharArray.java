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
import java.sql.Types;

import com.avaje.ebean.text.TextException;
import com.avaje.ebeaninternal.server.core.BasicTypeConverter;

/**
 * ScalarType for char[].
 */
public class ScalarTypeCharArray extends ScalarTypeBase<char[]>{

	public ScalarTypeCharArray() {
		super(char[].class, false, Types.VARCHAR);
	}
		
	public void bind(DataBind b, char[] value) throws SQLException {
		if (value == null){
			b.setNull(Types.VARCHAR);
		} else {
			String s = BasicTypeConverter.toString(value);
			b.setString(s);
		}
	}

	public char[] read(DataReader dataReader) throws SQLException {
		String string = dataReader.getString();
		if (string == null){
			return null;
		} else {
			return string.toCharArray();
		}
	}
	
	
	public Object toJdbcType(Object value) {
		return BasicTypeConverter.toString(value);
	}

	public char[] toBeanType(Object value) {
		String s = BasicTypeConverter.toString(value);
		return s.toCharArray();
	}

	
	public String format(char[] t) {
        return String.valueOf(t);
    }

    public char[] parse(String value) {
		return value.toCharArray();
	}

	public char[] parseDateTime(long systemTimeMillis) {
		throw new TextException("Not Supported");
	}

	public boolean isDateTimeCapable() {
		return false;
	}
	
}
